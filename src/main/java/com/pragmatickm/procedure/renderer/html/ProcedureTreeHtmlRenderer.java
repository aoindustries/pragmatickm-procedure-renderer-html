/*
 * pragmatickm-procedure-renderer-html - Procedures rendered as HTML in a Servlet environment.
 * Copyright (C) 2014, 2015, 2016, 2017, 2019, 2020  AO Industries, Inc.
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

import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.encodeTextInXhtmlAttribute;
import com.aoindustries.html.Html;
import com.aoindustries.net.URIEncoder;
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
		Html html,
		PageRef parentPageRef,
		Page page
	) throws IOException, ServletException {
		final PageRef pageRef = page.getPageRef();
		if(currentNode != null) {
			// Add page links
			currentNode.addPageLink(pageRef);
		}
		List<Procedure> procedures = new ArrayList<>();
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

		if(html != null) {
			HtmlRenderer htmlRenderer = HtmlRenderer.getInstance(servletContext);
			html.out.write("<li><a");
			if(mainLinkToProcedure) {
				String linkCssClass = htmlRenderer.getLinkCssClass(procedures.get(0));
				if(linkCssClass != null) {
					html.out.write(" class=\"");
					encodeTextInXhtmlAttribute(linkCssClass, html.out);
					html.out.write('"');
				}
			}
			BookRef bookRef = pageRef.getBookRef();
			Integer index = pageIndex==null ? null : pageIndex.getPageIndex(pageRef);
			html.out.write(" href=\"");
			StringBuilder href = new StringBuilder();
			if(index != null) {
				href.append('#');
				URIEncoder.encodeURIComponent(
					PageIndex.getRefId(
						index,
						mainLinkToProcedure ? procedures.get(0).getId() : null
					),
					href
				);
			} else {
				URIEncoder.encodeURI(request.getContextPath(), href);
				URIEncoder.encodeURI(bookRef.getPrefix(), href);
				URIEncoder.encodeURI(pageRef.getPath().toString(), href);
				if(mainLinkToProcedure) {
					href.append('#');
					URIEncoder.encodeURIComponent(procedures.get(0).getId(), href);
				}
			}
			encodeTextInXhtmlAttribute(
				response.encodeURL(
					href.toString()
				),
				html.out
			);
			html.out.write("\">");
			html.text(PageUtils.getShortTitle(parentPageRef, page));
			if(index != null) {
				html.out.write("<sup>[");
				html.text(index + 1);
				html.out.write("]</sup>");
			}
			html.out.write("</a>");
			if(!mainLinkToProcedure) {
				if(!procedures.isEmpty()) {
					for(Procedure procedure : procedures) {
						html.out.write("\n<div><a");
						String linkCssClass = htmlRenderer.getLinkCssClass(procedure);
						if(linkCssClass != null) {
							html.out.write(" class=\"");
							encodeTextInXhtmlAttribute(linkCssClass, html.out);
							html.out.write('"');
						}
						html.out.write(" href=\"");
						href.setLength(0);
						if(index != null) {
							href.append('#');
							URIEncoder.encodeURIComponent(
								PageIndex.getRefId(
									index,
									procedure.getId()
								),
								href
							);
						} else {
							URIEncoder.encodeURI(request.getContextPath(), href);
							URIEncoder.encodeURI(bookRef.getPrefix(), href);
							URIEncoder.encodeURI(pageRef.getPath().toString(), href);
							href.append('#');
							URIEncoder.encodeURIComponent(procedure.getId(), href);
						}
						encodeTextInXhtmlAttribute(
							response.encodeURL(
								href.toString()
							),
							html.out
						);
						html.out.write("\">");
						html.text(procedure.getLabel());
						if(index != null) {
							html.out.write("<sup>[");
							html.text(index + 1);
							html.out.write("]</sup>");
						}
						html.out.write("</a></div>");
					}
				}
			}
		}
		List<ChildRef> childRefs = NavigationTreeRenderer.filterPages(
			page.getChildRefs(),
			pagesWithProcedures
		);
		if(!childRefs.isEmpty()) {
			if(html != null) {
				html.out.write("\n"
					+ "<ul>\n");
			}
			// TODO: traversal
			for(ChildRef childRef : childRefs) {
				PageRef childPageRef = childRef.getPageRef();
				assert
					SemanticCMS.getInstance(servletContext).getBook(childPageRef.getBookRef()).isAccessible()
					: "pagesWithProcedures does not contain anything from missing books"
				;
				Page child = CapturePage.capturePage(servletContext, request, response, childPageRef, CaptureLevel.META);
				writePage(servletContext, request, response, currentNode, pagesWithProcedures, pageIndex, html, pageRef, child);
			}
			if(html != null) html.out.write("</ul>\n");
		}
		if(html != null) html.out.write("</li>\n");
	}

	/**
	 * @param html  optional, null if no output needs to be written
	 */
	public static void writeProcedureTree(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Html html,
		Page root
	) throws ServletException, IOException {
		writeProcedureTree(
			servletContext,
			null,
			request,
			response,
			html,
			root
		);
	}

	/**
	 * @param html  optional, null if no output needs to be written
	 * @param root  either Page of ValueExpression that returns Page
	 */
	public static void writeProcedureTree(
		ServletContext servletContext,
		ELContext elContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Html html,
		Object root
	) throws ServletException, IOException {
		// Get the current capture state
		final CaptureLevel captureLevel = CurrentCaptureLevel.getCaptureLevel(request);
		if(captureLevel.compareTo(CaptureLevel.META) >= 0) {
			// Evaluate expressions
			Page rootPage = resolveValue(root, Page.class, elContext);

			// Filter by has procedures
			final Set<PageRef> pagesWithProcedures = new HashSet<>();
			findProcedures(servletContext, request, response, pagesWithProcedures, rootPage);

			if(html != null) html.out.write("<ul>\n");
			writePage(
				servletContext,
				request,
				response,
				CurrentNode.getCurrentNode(request),
				pagesWithProcedures,
				PageIndex.getCurrentPageIndex(request),
				html,
				null,
				rootPage
			);
			if(html != null) html.out.write("</ul>\n");
		}
	}

	/**
	 * Make no instances.
	 */
	private ProcedureTreeHtmlRenderer() {
	}
}
