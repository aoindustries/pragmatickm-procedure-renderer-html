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

import com.aoindustries.encoding.Coercion;
import com.aoindustries.encoding.MediaWriter;
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.textInXhtmlAttributeEncoder;
import static com.aoindustries.encoding.TextInXhtmlEncoder.encodeTextInXhtml;
import com.aoindustries.io.buffer.BufferResult;
import com.pragmatickm.procedure.model.Procedure;
import com.semanticcms.core.model.ElementContext;
import com.semanticcms.core.model.NodeBodyWriter;
import com.semanticcms.core.renderer.html.PageIndex;
import java.io.IOException;
import java.io.Writer;

final public class ProcedureHtmlRenderer {

	public static void writeProcedureTable(
		PageIndex pageIndex,
		Writer out,
		ElementContext context,
		Object style,
		Procedure procedure
	) throws IOException {
		out.write("<table id=\"");
		PageIndex.appendIdInPage(
			pageIndex,
			procedure.getPage(),
			procedure.getId(),
			new MediaWriter(textInXhtmlAttributeEncoder, out)
		);
		out.write("\" class=\"thinTable procedureTable\"");
		if(style != null) {
			out.write(" style=\"");
			Coercion.write(style, textInXhtmlAttributeEncoder, out);
			out.write('"');
		}
		out.write(">\n"
				+ "<thead><tr><th class=\"procedureTableHeader\"><div>");
		encodeTextInXhtml(procedure.getLabel(), out);
		out.write("</div></th></tr></thead>\n"
				+ "<tbody>\n");
		BufferResult body = procedure.getBody();
		if(body.getLength() > 0) {
			out.write("<tr><td>\n");
			body.writeTo(new NodeBodyWriter(procedure, out, context));
			out.write("\n</td></tr>\n");
		}
		out.write("</tbody>\n"
				+ "</table>");
	}

	/**
	 * Make no instances.
	 */
	private ProcedureHtmlRenderer() {
	}
}
