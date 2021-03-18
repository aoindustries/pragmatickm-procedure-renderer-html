/*
 * pragmatickm-procedure-renderer-html - Procedures rendered as HTML in a Servlet environment.
 * Copyright (C) 2014, 2015, 2016, 2017, 2019, 2020, 2021  AO Industries, Inc.
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

import com.aoindustries.html.any.AnyListContent;
import com.aoindustries.html.any.AnyPalpableContent;
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
		AnyListContent<?, ?> content,
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

		if(content != null) {
			HtmlRenderer htmlRenderer = HtmlRenderer.getInstance(servletContext);
			Integer index = pageIndex==null ? null : pageIndex.getPageIndex(pageRef);
			BookRef bookRef = pageRef.getBookRef();
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
			content.li__any(li -> {
				li.a()
					.clazz(mainLinkToProcedure ? htmlRenderer.getLinkCssClass(procedures.get(0)) : null)
					.href(response.encodeURL(href.toString()))
				.__(a -> {
					a.text(PageUtils.getShortTitle(parentPageRef, page));
					if(index != null) {
						a.sup__any(sup -> sup
							.text('[').text(index + 1).text(']')
						);
					}
				});
				if(!mainLinkToProcedure) {
					if(!procedures.isEmpty()) {
						for(Procedure procedure : procedures) {
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
							li.div__any(div -> div
								.a()
									.clazz(htmlRenderer.getLinkCssClass(procedure))
									.href(response.encodeURL(href.toString()))
								.__(a -> {
									a.text(procedure);
									if(index != null) {
										a.sup__any(sup -> sup
											.text('[').text(index + 1).text(']')
										);
									}
								})
							);
						}
					}
				}
				List<ChildRef> childRefs = NavigationTreeRenderer.filterPages(
					page.getChildRefs(),
					pagesWithProcedures
				);
				if(!childRefs.isEmpty()) {
					li.ul__any(ul -> {
						// TODO: traversal
						for(ChildRef childRef : childRefs) {
							PageRef childPageRef = childRef.getPageRef();
							assert SemanticCMS.getInstance(servletContext).getBook(childPageRef.getBookRef()).isAccessible()
								: "pagesWithProcedures does not contain anything from missing books";
							Page child = CapturePage.capturePage(servletContext, request, response, childPageRef, CaptureLevel.META);
							writePage(servletContext, request, response, currentNode, pagesWithProcedures, pageIndex, ul, pageRef, child);
						}
					});
				}
			});
		} else {
			List<ChildRef> childRefs = NavigationTreeRenderer.filterPages(
				page.getChildRefs(),
				pagesWithProcedures
			);
			if(!childRefs.isEmpty()) {
				// TODO: traversal
				for(ChildRef childRef : childRefs) {
					PageRef childPageRef = childRef.getPageRef();
					assert SemanticCMS.getInstance(servletContext).getBook(childPageRef.getBookRef()).isAccessible()
						: "pagesWithProcedures does not contain anything from missing books";
					Page child = CapturePage.capturePage(servletContext, request, response, childPageRef, CaptureLevel.META);
					writePage(servletContext, request, response, currentNode, pagesWithProcedures, pageIndex, null, pageRef, child);
				}
			}
		}
	}

	/**
	 * @param content  optional, null if no output needs to be written
	 */
	public static void writeProcedureTree(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		AnyPalpableContent<?, ?> content,
		Page root
	) throws ServletException, IOException {
		writeProcedureTree(
			servletContext,
			null,
			request,
			response,
			content,
			root
		);
	}

	/**
	 * @param content  optional, null if no output needs to be written
	 * @param root  either Page of ValueExpression that returns Page
	 */
	public static void writeProcedureTree(
		ServletContext servletContext,
		ELContext elContext,
		HttpServletRequest request,
		HttpServletResponse response,
		AnyPalpableContent<?, ?> content,
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

			Node currentNode = CurrentNode.getCurrentNode(request);
			PageIndex pageIndex = PageIndex.getCurrentPageIndex(request);

			if(content != null) {
				content.ul__any(ul -> writePage(
					servletContext,
					request,
					response,
					currentNode,
					pagesWithProcedures,
					pageIndex,
					ul,
					null,
					rootPage
				));
			} else {
				writePage(
					servletContext,
					request,
					response,
					currentNode,
					pagesWithProcedures,
					pageIndex,
					null,
					null,
					rootPage
				);
			}
		}
	}

	/**
	 * Make no instances.
	 */
	private ProcedureTreeHtmlRenderer() {
	}
}
