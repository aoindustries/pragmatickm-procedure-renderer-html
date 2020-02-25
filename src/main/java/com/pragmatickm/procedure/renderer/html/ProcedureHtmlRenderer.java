/*
 * pragmatickm-procedure-renderer-html - Procedures rendered as HTML in a Servlet environment.
 * Copyright (C) 2014, 2015, 2016, 2017, 2018, 2020  AO Industries, Inc.
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

import com.aoindustries.encoding.Coercion;
import com.aoindustries.encoding.MediaWriter;
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.textInXhtmlAttributeEncoder;
import com.aoindustries.html.Html;
import com.aoindustries.io.buffer.BufferResult;
import com.pragmatickm.procedure.model.Procedure;
import com.semanticcms.core.model.ElementContext;
import com.semanticcms.core.model.NodeBodyWriter;
import com.semanticcms.core.renderer.html.PageIndex;
import java.io.IOException;

final public class ProcedureHtmlRenderer {

	public static void writeProcedureTable(
		PageIndex pageIndex,
		Html html,
		ElementContext context,
		Object style,
		Procedure procedure
	) throws IOException {
		html.out.write("<table id=\"");
		PageIndex.appendIdInPage(
			pageIndex,
			procedure.getPage(),
			procedure.getId(),
			new MediaWriter(textInXhtmlAttributeEncoder, html.out)
		);
		// TODO: SemanticCMS 2.0: All CSS classes properly project prefixed:
		// TODO: pragmatickm-procedure-renderer-html-table here, for example
		// TODO: Or may be just pragmatickm-procedure-table since this CSS used by renderer-html only?
		html.out.write("\" class=\"ao-grid procedureTable\"");
		if(style != null) {
			html.out.write(" style=\"");
			Coercion.write(style, textInXhtmlAttributeEncoder, html.out);
			html.out.write('"');
		}
		html.out.write(">\n"
				+ "<thead><tr><th class=\"procedureTableHeader\"><div>");
		html.text(procedure.getLabel());
		html.out.write("</div></th></tr></thead>\n"
				+ "<tbody>\n");
		BufferResult body = procedure.getBody();
		if(body.getLength() > 0) {
			html.out.write("<tr><td>\n");
			body.writeTo(new NodeBodyWriter(procedure, html.out, context));
			html.out.write("\n</td></tr>\n");
		}
		html.out.write("</tbody>\n"
				+ "</table>");
	}

	/**
	 * Make no instances.
	 */
	private ProcedureHtmlRenderer() {
	}
}
