/*
 * pragmatickm-procedure-renderer-html - Procedures rendered as HTML in a Servlet environment.
 * Copyright (C) 2014, 2015, 2016, 2017  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of pragmatickm-procedure-renderer-html.
 *
 * pragmatickm-procedure-renderer-html is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * pragmatickm-procedure-renderer-html is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with pragmatickm-procedure-renderer-html.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pragmatickm.procedure.renderer.html;

import com.aoindustries.encoding.MediaWriter;
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.encodeTextInXhtmlAttribute;
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.textInXhtmlAttributeEncoder;
import static com.aoindustries.encoding.TextInXhtmlEncoder.encodeTextInXhtml;
import com.aoindustries.net.UrlUtils;
import static com.aoindustries.taglib.AttributeUtils.resolveValue;
import com.pragmatickm.procedure.model.Procedure;
import com.semanticcms.core.controller.CapturePage;
import com.semanticcms.core.controller.PageUtils;
import com.semanticcms.core.controller.SemanticCMS;
import com.semanticcms.core.model.BookRef;
import com.semanticcms.core.model.ChildRef;
import com.semanticcms.core.model.Element;
import com.semanticcms.core.model.Node;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.model.PageRef;
import com.semanticcms.core.pages.CaptureLevel;
import com.semanticcms.core.pages.local.CurrentCaptureLevel;
import com.semanticcms.core.pages.local.CurrentNode;
import com.semanticcms.core.renderer.html.HtmlRenderer;
import com.semanticcms.core.renderer.html.NavigationTreeRenderer;
import com.semanticcms.core.renderer.html.PageIndex;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.el.ELContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

final public class ProcedureTreeHtmlRenderer {

	// TODO: as traversal
	private static boolean findProcedures(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Set<PageRef> pagesWithProcedures,
		Page page
	) throws ServletException, IOException {
		boolean hasProcedure = false;
		for(Element element : page.getElements()) {
			if(element instanceof Procedure) {
				hasProcedure = true;
				break;
			}
		}
		Set<ChildRef> childRefs = page.getChildRefs();
		if(!childRefs.isEmpty()) {
			SemanticCMS semanticCMS = SemanticCMS.getInstance(servletContext);
			for(ChildRef childRef : childRefs) {
				PageRef childPageRef = childRef.getPageRef();
				// Child is in an accessible book
				if(semanticCMS.getBook(childPageRef.getBookRef()).isAccessible()) {
					Page child = CapturePage.capturePage(servletContext, request, response, childPageRef, CaptureLevel.META);
					if(
						findProcedures(
							servletContext,
							request,
							response,
							pagesWithProcedures,
							child
						)
					) {
						hasProcedure = true;
					}
				}
			}
		}
		if(hasProcedure) {
			pagesWithProcedures.add(page.getPageRef());
		}
		return hasProcedure;
	}

	private static void writePage(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Node currentNode,
		Set<PageRef> pagesWithProcedures,
		PageIndex pageIndex,
		Writer out,
		PageRef parentPageRef,
		Page page
	) throws IOException, ServletException {
		final PageRef pageRef = page.getPageRef();
		if(currentNode != null) {
			// Add page links
			currentNode.addPageLink(pageRef);
		}
		List<Procedure> procedures = new ArrayList<Procedure>();
		for(Element element : page.getElements()) {
			if(element instanceof Procedure) procedures.add((Procedure)element);
		}

		// Make the main link point to the procedure when:
		//   1) Page has only one procedure
		//   2) The one procedure has same "label" as page "shortTitle"
		boolean mainLinkToProcedure =
			procedures.size()==1
			&& procedures.get(0).getLabel().equals(page.getShortTitle())
		;

		if(out!=null) {
			HtmlRenderer htmlRenderer = HtmlRenderer.getInstance(servletContext);
			final String responseEncoding = response.getCharacterEncoding();
			out.write("<li><a");
			if(mainLinkToProcedure) {
				String linkCssClass = htmlRenderer.getLinkCssClass(procedures.get(0));
				if(linkCssClass != null) {
					out.write(" class=\"");
					encodeTextInXhtmlAttribute(linkCssClass, out);
					out.write('"');
				}
			}
			out.write(" href=\"");
			BookRef bookRef = pageRef.getBookRef();
			Integer index = pageIndex==null ? null : pageIndex.getPageIndex(pageRef);
			if(index != null) {
				out.write('#');
				PageIndex.appendIdInPage(
					index,
					mainLinkToProcedure ? procedures.get(0).getId() : null,
					new MediaWriter(textInXhtmlAttributeEncoder, out)
				);
			} else {
				encodeTextInXhtmlAttribute(
					response.encodeURL(
						UrlUtils.encodeUrlPath(
							request.getContextPath() + bookRef.getPrefix() + pageRef.getPath(),
							responseEncoding
						)
					),
					out
				);
				if(mainLinkToProcedure) {
					encodeTextInXhtmlAttribute('#', out);
					encodeTextInXhtmlAttribute(procedures.get(0).getId(), out);
				}
			}
			out.write("\">");
			encodeTextInXhtml(PageUtils.getShortTitle(parentPageRef, page), out);
			if(index != null) {
				out.write("<sup>[");
				encodeTextInXhtml(Integer.toString(index+1), out);
				out.write("]</sup>");
			}
			out.write("</a>");
			if(!mainLinkToProcedure) {
				if(!procedures.isEmpty()) {
					for(Procedure procedure : procedures) {
						out.write("\n<div><a");
						String linkCssClass = htmlRenderer.getLinkCssClass(procedure);
						if(linkCssClass != null) {
							out.write(" class=\"");
							encodeTextInXhtmlAttribute(linkCssClass, out);
							out.write('"');
						}
						out.write(" href=\"");
						if(index != null) {
							out.write('#');
							PageIndex.appendIdInPage(
								index,
								procedure.getId(),
								new MediaWriter(textInXhtmlAttributeEncoder, out)
							);
						} else {
							encodeTextInXhtmlAttribute(
								response.encodeURL(
									UrlUtils.encodeUrlPath(
										request.getContextPath() + bookRef.getPrefix() + pageRef.getPath(),
										responseEncoding
									)
								),
								out
							);
							encodeTextInXhtmlAttribute('#', out);
							encodeTextInXhtmlAttribute(procedure.getId(), out);
						}
						out.write("\">");
						encodeTextInXhtml(procedure.getLabel(), out);
						if(index != null) {
							out.write("<sup>[");
							encodeTextInXhtml(Integer.toString(index+1), out);
							out.write("]</sup>");
						}
						out.write("</a></div>");
					}
				}
			}
		}
		List<ChildRef> childRefs = NavigationTreeRenderer.filterPages(
			page.getChildRefs(),
			pagesWithProcedures
		);
		if(!childRefs.isEmpty()) {
			if(out!=null) {
				out.write('\n');
				out.write("<ul>\n");
			}
			// TODO: traversal
			for(ChildRef childRef : childRefs) {
				PageRef childPageRef = childRef.getPageRef();
				assert
					SemanticCMS.getInstance(servletContext).getBook(childPageRef.getBookRef()).isAccessible()
					: "pagesWithProcedures does not contain anything from missing books"
				;
				Page child = CapturePage.capturePage(servletContext, request, response, childPageRef, CaptureLevel.META);
				writePage(servletContext, request, response, currentNode, pagesWithProcedures, pageIndex, out, pageRef, child);
			}
			if(out!=null) out.write("</ul>\n");
		}
		if(out!=null) out.write("</li>\n");
	}

	/**
	 * @param out  optional, null if no output needs to be written
	 */
	public static void writeProcedureTree(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Writer out,
		Page root
	) throws ServletException, IOException {
		writeProcedureTree(
			servletContext,
			null,
			request,
			response,
			out,
			root
		);
	}

	/**
	 * @param out  optional, null if no output needs to be written
	 * @param root  either Page of ValueExpression that returns Page
	 */
	public static void writeProcedureTree(
		ServletContext servletContext,
		ELContext elContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Writer out,
		Object root
	) throws ServletException, IOException {
		// Get the current capture state
		final CaptureLevel captureLevel = CurrentCaptureLevel.getCaptureLevel(request);
		if(captureLevel.compareTo(CaptureLevel.META) >= 0) {
			// Evaluate expressions
			Page rootPage = resolveValue(root, Page.class, elContext);

			// Filter by has procedures
			final Set<PageRef> pagesWithProcedures = new HashSet<PageRef>();
			findProcedures(servletContext, request, response, pagesWithProcedures, rootPage);

			if(out != null) out.write("<ul>\n");
			writePage(
				servletContext,
				request,
				response,
				CurrentNode.getCurrentNode(request),
				pagesWithProcedures,
				PageIndex.getCurrentPageIndex(request),
				out,
				null,
				rootPage
			);
			if(out != null) out.write("</ul>\n");
		}
	}

	/**
	 * Make no instances.
	 */
	private ProcedureTreeHtmlRenderer() {
	}
}
