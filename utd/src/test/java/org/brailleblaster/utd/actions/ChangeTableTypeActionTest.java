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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.brailleblaster.utd.ActionMap;
import org.brailleblaster.utd.BrailleSettings;
import org.brailleblaster.utd.ITranslationEngine;
import org.brailleblaster.utd.TextSpan;
import org.brailleblaster.utd.properties.BrailleTableType;
import org.brailleblaster.utd.testutils.MockTranslatorFactory;
import org.brailleblaster.utils.xml.NamespacesKt;
import org.mwhapples.jlouis.Louis;
import org.mwhapples.jlouis.TranslationException;
import org.mwhapples.jlouis.TranslationResult;
import org.testng.annotations.Test;

import nu.xom.Element;

public class ChangeTableTypeActionTest {
	@Test
	public void construct() {
		ChangeTableTypeAction action = new ChangeTableTypeAction(BrailleTableType.COMPUTER_BRAILLE);
		assertEquals(action.getTable(), BrailleTableType.COMPUTER_BRAILLE);
	}
	@Test
	public void applyToBlock() throws TranslationException {
		ITranslationEngine mockEngine = mock(ITranslationEngine.class);
		BrailleSettings brailleSettings = new BrailleSettings();
		brailleSettings.setUseAsciiBraille(true);
		brailleSettings.setComputerBrailleTable("en-us-comp6.ctb");
		when(mockEngine.getBrailleSettings()).thenReturn(brailleSettings);
		when(mockEngine.getActionMap()).thenReturn(new ActionMap());
		Louis mockLouis = mock(Louis.class);
		TranslationResult transResult = MockTranslatorFactory.createMockTranslationResult(",\"s text", "0 0 0 4 5 6 7 8", "0 0 0 0 3 4 5 6 7");
		when(mockLouis.translate(eq("en-us-comp6.ctb"), eq("Some text"), any(), eq(0), eq(0))).thenReturn(transResult);
		when(mockEngine.getBrailleTranslator()).thenReturn(mockLouis);
		ChangeTableTypeAction action = new ChangeTableTypeAction(BrailleTableType.COMPUTER_BRAILLE);
		Element node = new Element("p");
		node.appendChild("Some text");
		
		List<TextSpan> result = action.applyTo(node, mockEngine);
		
		assertTrue(result.isEmpty());
		String expectedXML = String.format("<p>Some text<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"0 0 0 4 5 6 7 8\">,\"s text</utd:brl></p>", NamespacesKt.UTD_NS);
		assertEquals(expectedXML, node.toXML());
	}
}
