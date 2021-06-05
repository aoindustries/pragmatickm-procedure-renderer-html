/*
 * pragmatickm-procedure-renderer-html - Procedures rendered as HTML in a Servlet environment.
 * Copyright (C) 2014, 2015, 2016, 2017, 2018, 2020, 2021  AO Industries, Inc.
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

import com.aoapps.html.any.AnyPalpableContent;
import com.aoapps.io.buffer.BufferResult;
import com.pragmatickm.procedure.model.Procedure;
import com.semanticcms.core.model.ElementContext;
import com.semanticcms.core.model.NodeBodyWriter;
import com.semanticcms.core.renderer.html.PageIndex;
import java.io.IOException;

final public class ProcedureHtmlRenderer {

	public static void writeProcedureTable(
		PageIndex pageIndex,
		AnyPalpableContent<?, ?> content,
		ElementContext context,
		Object style,
		Procedure procedure
	) throws IOException {
		content.table()
			.id(idAttr -> PageIndex.appendIdInPage(
				pageIndex,
				procedure.getPage(),
				procedure.getId(),
				idAttr
			))
			.clazz("ao-grid", "pragmatickm-procedure")
			.style(style)
		.__(table -> table
			.thead__any(thead -> thead
				.tr__any(tr -> tr
					.th__any(th -> th
						.div__(procedure)
					)
				)
			)
			.tbody__any(tbody -> {
				BufferResult body = procedure.getBody();
				if(body.getLength() > 0) {
					tbody.tr__any(tr -> tr
						.td__any(td ->
							body.writeTo(new NodeBodyWriter(procedure, td.getUnsafe(), context))
						)
					);
				}
			})
		);
	}

	/**
	 * Make no instances.
	 */
	private ProcedureHtmlRenderer() {
	}
}
