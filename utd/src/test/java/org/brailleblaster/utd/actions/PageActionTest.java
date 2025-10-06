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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;

import nu.xom.Attribute;
import nu.xom.Element;

import org.brailleblaster.utd.ActionMap;
import org.brailleblaster.utd.BrailleSettings;
import org.brailleblaster.utd.ITranslationEngine;
import org.brailleblaster.utd.TextSpan;
import org.brailleblaster.utd.UTDTranslationEngine;
import org.brailleblaster.utd.properties.UTDElements;
import org.mwhapples.jlouis.Louis;
import org.mwhapples.jlouis.TranslationException;
import org.mwhapples.jlouis.TranslationResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PageActionTest {
	private ITranslationEngine contextMock;
	private BrailleSettings brailleSettings;
	@BeforeMethod
	private void setupMethod() {
		contextMock = mock(UTDTranslationEngine.class);
		brailleSettings = new BrailleSettings();
		when(contextMock.getBrailleSettings()).thenReturn(brailleSettings);
	}
	
	private TranslationResult createMockTranslationResult(String braille, String indexes) {
		String[] indexArray = indexes.split(" ");
		int[] inPosArray = new int[indexArray.length];
		for (int i = 0; i < indexArray.length; i++) {
			inPosArray[i] = Integer.parseInt(indexArray[i]);
		}
		TranslationResult translationResultMock = mock(TranslationResult.class);
		when(translationResultMock.getTranslation()).thenReturn(braille);
		when(translationResultMock.getInputPos()).thenReturn(inPosArray);
		return translationResultMock;
	}

	@Test
	public void processPageNodeTest() throws TranslationException {
		Element p = new Element("p");
		Element pageNum = new Element("pagenum");
		pageNum.appendChild("5");
		p.appendChild(pageNum);

		Louis louisMock = mock(Louis.class);
		brailleSettings.setUseAsciiBraille(true);
		TranslationResult transResultMock = createMockTranslationResult("#e", "0");
		//TODO: You need a representation of a short[] for the TypeForms in the louisMock
		when(louisMock.translate(anyString(), eq("5"), any(short[].class), eq(0), eq(0))).thenReturn(transResultMock);
		when(contextMock.getBrailleTranslator()).thenReturn(louisMock);
		when(contextMock.getActionMap()).thenReturn(new ActionMap());
		
		PageAction action = new PageAction();
		List<TextSpan> result = action.applyTo(pageNum, contextMock);
		assertTrue(result.isEmpty());
		String expectedXML = "<p><pagenum>5<utd:brl xmlns:utd=\"http://brailleblaster.org/ns/utd\" xml:space=\"preserve\" printPage=\"5\" printPageBrl=\"#e\" pageType=\"NORMAL\" /></pagenum></p>";
		assertEquals(p.toXML(), expectedXML);
	}
	
	@Test
	public void checkPresentBrl() {
		Element p = new Element("p");
		Element pageNum = new Element("pagenum");
		pageNum.appendChild("5");
		p.appendChild(pageNum);
		Element brl = UTDElements.BRL.create();
		brl.addAttribute(new Attribute("printPage", "5"));
		pageNum.appendChild(brl);
		
		String original = "<p><pagenum>5<utd:brl xmlns:utd=\"http://brailleblaster.org/ns/utd\" xml:space=\"preserve\" printPage=\"5\" /></pagenum></p>";
		assertEquals(p.toXML(), original);
		PageAction action = new PageAction();
		List<TextSpan> result = action.applyTo(pageNum, contextMock);
		assertTrue(result.isEmpty());
		assertEquals(p.toXML(), original);
	}
}
