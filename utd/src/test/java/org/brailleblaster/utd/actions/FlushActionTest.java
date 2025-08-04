/*
 * Copyright (C) 2025 American Printing House for the Blind
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.brailleblaster.utd.actions;

import java.util.ArrayList;
import java.util.List;

import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;

import org.brailleblaster.utd.ITranslationEngine;
import org.brailleblaster.utd.UTDTranslationEngine;
import org.brailleblaster.utd.TextSpan;
import org.brailleblaster.utd.properties.UTDElements;
import org.brailleblaster.utils.NamespacesKt;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class FlushActionTest {
	List<TextSpan> result = new ArrayList<>();
	
	/*
	* <p>The start of<br/>a line.</p>
	*/
	private Node nodeBuilder() {
		Element p = new Element("p");
		Node text = new Text("Some text");
		Node br = new Element("br");
		Node text2 = new Text("a bold phrase");
		
		p.appendChild(text);
		p.appendChild(br);
		p.appendChild(text2);
		
		return p;
	}

	@Test
	public void testApplyTo() {
		Node p = nodeBuilder();
		String originalXML = "<p>Some text<br />a bold phrase</p>";
		assertEquals(p.toXML(), originalXML);
		
		Node flush = p.getChild(1);
		FlushAction action = new FlushAction();
		ITranslationEngine context = new UTDTranslationEngine();
		result = action.applyTo(flush, context);
		
		String newXML = String.format("<p>Some text<br /><utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" />a bold phrase</p>", NamespacesKt.UTD_NS);
		assertEquals(p.toXML(), newXML);
		assertTrue(result.get(0).isTranslated());
	}
	
	@Test
	public void testInsertBrl() {
		Node p = nodeBuilder();
		Node flush = p.getChild(1);
		FlushAction action = new FlushAction();
		ITranslationEngine context = new UTDTranslationEngine();
		action.applyTo(flush, context);
		
		String newXML = String.format("<p>Some text<br /><utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" />a bold phrase</p>", NamespacesKt.UTD_NS);
		assertEquals(p.toXML(), newXML);
	}
}