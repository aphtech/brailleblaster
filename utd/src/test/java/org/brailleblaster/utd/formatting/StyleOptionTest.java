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
package org.brailleblaster.utd.formatting;

import org.brailleblaster.utd.UTDTranslationEngine;
import org.brailleblaster.utd.actions.TransNoteAction;
import org.brailleblaster.utd.properties.UTDElements;
import org.brailleblaster.utd.testutils.UTDDocumentAsserter;
import org.brailleblaster.utd.testutils.UTDTestUtils;
import org.testng.annotations.Test;

import nu.xom.Document;

public class StyleOptionTest {
	
	private final static String FORTY_CHAR_STRING = "123456789 123456789 123456789 123456789 ";
	
	@Test
	public void basicLinesBeforeTest(){
		Document doc = UTDTestUtils.generateBookDoc("",
				"<p linesBefore='1'>Test 1</p>" +
				"<p linesBefore='1'>Test 2</p>" + 
				"<p linesBefore='2'>Test 3</p>");
		UTDTranslationEngine engine = UTDTestUtils.translateAndFormat(doc);
		new UTDDocumentAsserter(doc, engine)
			.hasTextAt("Test 1", 0, 0)
			.hasTextAt("Test 2", 0, 1)
			.hasTextAt("Test 3", 0, 3)
			.hasNoOtherMoveTo();
	}
	
	@Test
	public void basicLinesAfterTest(){
		Document doc = UTDTestUtils.generateBookDoc("",
				"<p linesAfter='1'>Test 1</p>" +
				"<p linesAfter='2'>Test 2</p>" +
				"<p>Test 3</p>");
		UTDTranslationEngine engine = UTDTestUtils.translateAndFormat(doc);
		new UTDDocumentAsserter(doc, engine)
			.hasTextAt("Test 1", 0, 0)
			.hasTextAt("Test 2", 0, 1)
			.hasTextAt("Test 3", 0, 3)
			.hasNoOtherMoveTo();
	}
	
	@Test
	public void basicIndentTest(){
		final String THIRTY_EIGHT_STRING = FORTY_CHAR_STRING.substring(0, 37) + " ";
		final String THIRTY_SIX_STRING = FORTY_CHAR_STRING.substring(0, 35) + " ";
		Document doc = UTDTestUtils.generateBookDoc("",
				UTDTestUtils.elementToString(UTDElements.NEW_LINE, "") +
				"<p indent='2' linesBefore='1'>" + THIRTY_EIGHT_STRING + "Test</p>" +
				"<p indent='4' linesBefore='1'>" + THIRTY_SIX_STRING + "Test 2</p>");
		UTDTranslationEngine engine = UTDTestUtils.translateAndFormat(doc);
		new UTDDocumentAsserter(doc, engine)
			.hasTextAt(THIRTY_EIGHT_STRING, 2, 1)
			.hasTextAt("Test", 2, 2)
			.hasTextAt(THIRTY_SIX_STRING, 4, 3)
			.hasTextAt("Test 2", 4, 4)
			.hasNoOtherMoveTo();
	}
	
	@Test
	public void basicFirstLineIndentTest(){
		final String THIRTY_EIGHT_STRING = FORTY_CHAR_STRING.substring(0, 37) + " ";
		final String THIRTY_SIX_STRING = FORTY_CHAR_STRING.substring(0, 35) + " ";
		Document doc = UTDTestUtils.generateBookDoc("",
				UTDTestUtils.elementToString(UTDElements.NEW_LINE, "") +
				"<p firstLineIndent='2' linesBefore='1'>" + THIRTY_EIGHT_STRING + "Test</p>" +
				"<p firstLineIndent='4' linesBefore='1'>" + THIRTY_SIX_STRING + "Test 2</p>");
		UTDTranslationEngine engine = UTDTestUtils.translateAndFormat(doc);
		new UTDDocumentAsserter(doc, engine)
			.hasTextAt(THIRTY_EIGHT_STRING, 2, 1)
			.hasTextAt("Test", 0, 2)
			.hasTextAt(THIRTY_SIX_STRING, 4, 3)
			.hasTextAt("Test 2", 0, 4)
			.hasNoOtherMoveTo();
	}
	
	@Test
	public void basicAlignTest(){
		Document doc = UTDTestUtils.generateBookDoc("",
				"<p linesBefore='1' align='CENTERED'>Test</p>" +
				"<p linesBefore='1' align='CENTERED'>Test 2</p>");
		UTDTranslationEngine engine = UTDTestUtils.translateAndFormat(doc);
		new UTDDocumentAsserter(doc, engine)
			.hasTextAt("Test", 18, 0)
			.hasTextAt("Test 2", 17, 1)
			.hasNoOtherMoveTo();
	}
	
	@Test
	public void basicLineLengthTest(){
		Document doc = UTDTestUtils.generateBookDoc("",
				UTDTestUtils.elementToString(UTDElements.NEW_LINE, "") +
				"<p lineLength='20' linesBefore='1'>" + FORTY_CHAR_STRING + "</p>" +
				"<p lineLength='-10' linesBefore='1'>" + FORTY_CHAR_STRING + "</p>");
		UTDTranslationEngine engine = UTDTestUtils.translateAndFormat(doc);
		new UTDDocumentAsserter(doc, engine)
			.hasTextAt(FORTY_CHAR_STRING.substring(0, 20), 0, 1)
			.hasTextAt(FORTY_CHAR_STRING.substring(20), 0, 2)
			.hasTextAt(FORTY_CHAR_STRING.substring(0, 30), 0, 3)
			.hasTextAt(FORTY_CHAR_STRING.substring(30), 0, 4)
			.hasNoOtherMoveTo();
	}
	
	@Test
	public void basicBoxlineTest(){
		Document doc = UTDTestUtils.generateBookDoc("",
				UTDTestUtils.elementToString(UTDElements.NEW_LINE, "") +
				"<span startSeparator='7' endSeparator='g'>" +
					"<p linesBefore='1'>Test</p>" +
				"</span>");
		UTDTranslationEngine engine = UTDTestUtils.translateAndFormat(doc);
		new UTDDocumentAsserter(doc, engine)
			.hasTextAt("7777777777777777777777777777777777777777", 0, 1)
			.hasTextAt("Test", 0, 2)
			.hasTextAt("gggggggggggggggggggggggggggggggggggggggg", 0, 3)
			.hasNoOtherMoveTo();
	}
	
	@Test
	public void colorBoxlineTest(){
		Document doc = UTDTestUtils.generateBookDoc("",
				UTDTestUtils.elementToString(UTDElements.NEW_LINE, "") +
				"<span startSeparator='7' endSeparator='g' color='blue'>" +
					"<p linesBefore='1'>Test</p>" +
				"</span>");
		UTDTranslationEngine engine = UTDTestUtils.translateAndFormat(doc);
		new UTDDocumentAsserter(doc, engine)
			.hasTextAt(TransNoteAction.Companion.getStart("ueb")+"blue"+TransNoteAction.Companion.getEnd("ueb")+" 77777777777777777777777777777", 0, 1)
			.hasTextAt("Test", 0, 2)
			.hasTextAt("gggggggggggggggggggggggggggggggggggggggg", 0, 3)
			.hasNoOtherMoveTo();
	}
	
	@Test
	public void nestedBoxlineTest(){
		Document doc = UTDTestUtils.generateBookDoc("",
				UTDTestUtils.elementToString(UTDElements.NEW_LINE, "") +
				"<span startSeparator='1' endSeparator='2'>" +
					"<span startSeparator='3' endSeparator='4'>" +
						"<p linesBefore='1'>Test</p>" +
					"</span>" +
				"</span>");
		UTDTranslationEngine engine = UTDTestUtils.translateAndFormat(doc);
		new UTDDocumentAsserter(doc, engine)
			.hasTextAt("1111111111111111111111111111111111111111", 0, 1)
			.hasTextAt("3333333333333333333333333333333333333333", 0, 2)
			.hasTextAt("Test", 0, 3)
			.hasTextAt("4444444444444444444444444444444444444444", 0, 4)
			.hasTextAt("2222222222222222222222222222222222222222", 0, 5)
			.hasNoOtherMoveTo();
	}
	
	@Test
	public void basicNewPagesBeforeTest(){
		Document doc = UTDTestUtils.generateBookDoc("",
				"<p>Test</p>" +
				"<p newPagesBefore='1'>Test 2</p>");
		UTDTranslationEngine engine = UTDTestUtils.translateAndFormat(doc);
		new UTDDocumentAsserter(doc, engine)
			.hasTextAt("Test", 0 ,0)
			.nextPage()
			.hasTextAt("Test 2", 0, 0)
			.hasNoOtherMoveTo();
	}
	
	@Test
	public void multipleNewPagesBeforeTest(){
		Document doc = UTDTestUtils.generateBookDoc("",
				"<p linesBefore='1'>Test</p>" +
				"<p newPagesBefore='2' linesBefore='1'>Test 2</p>");
		UTDTranslationEngine engine = UTDTestUtils.translateAndFormat(doc);
		new UTDDocumentAsserter(doc, engine)
			.hasTextAt("Test", 0, 0)
			.nextPage()
			.nextPage()
			.hasTextAt("Test 2", 0, 0)
			.hasNoOtherMoveTo();
	}
	
	@Test
	public void basicNewPagesAfterTest(){
		Document doc = UTDTestUtils.generateBookDoc("",
				"<p newPagesAfter='1'>Test</p>" +
				"<p>Test 2</p>");
		UTDTranslationEngine engine = UTDTestUtils.translateAndFormat(doc);
		new UTDDocumentAsserter(doc, engine)
			.hasTextAt("Test", 0, 0)
			.nextPage()
			.hasTextAt("Test 2", 0, 0)
			.hasNoOtherMoveTo();
	}
	
	@Test
	public void basicDontSplitTest(){
		Document doc = UTDTestUtils.generateBookDoc("",
				"<p linesBefore='1'>Test</p>" + 
				"<p linesBefore='23'>Test 2</p>" +
				"<span dontSplit='true'>" +
					"<p linesBefore='1'>Test 3</p>" +
					"<p linesBefore='1'>Test 4</p>" +
				"</span>");
		UTDTranslationEngine engine = UTDTestUtils.translateAndFormat(doc);
		new UTDDocumentAsserter(doc, engine)
			.hasTextAt("Test", 0, 0)
			.hasTextAt("Test 2", 0, 23)
			.nextPage()
			.hasTextAt("Test 3", 0, 0)
			.hasTextAt("Test 4", 0, 1)
			.hasNoOtherMoveTo();
	}
	
	@Test
	public void basicKeepWithNextTest(){
		Document doc = UTDTestUtils.generateBookDoc("", 
				"<p linesBefore='1'>Test</p>" +
				"<p linesBefore='24' linesAfter='2' keepWithNext='true'>Test 2</p>" +
				"<p linesBefore='1'>Test 3</p>");
		UTDTranslationEngine engine = UTDTestUtils.translateAndFormat(doc);
		new UTDDocumentAsserter(doc, engine)
			.hasTextAt("Test", 0, 0)
			.nextPage()
			.hasTextAt("Test 2", 0, 0)
			.hasTextAt("Test 3", 0, 2)
			.hasNoOtherMoveTo();
	}
	
	@Test
	public void chainedKeepWithNextTest(){
		Document doc = UTDTestUtils.generateBookDoc("", 
				"<p linesBefore='1'>Test</p>" +
				"<p linesBefore='22' linesAfter='2' keepWithNext='true'>Test 2</p>" +
				"<p linesBefore='2' linesAfter='2' keepWithNext='true'>Test 3</p>" +
				"<p linesBefore='1'>Test 4</p>");
		UTDTranslationEngine engine = UTDTestUtils.translateAndFormat(doc);
		new UTDDocumentAsserter(doc, engine)
			.hasTextAt("Test", 0, 0)
			.nextPage()
			.hasTextAt("Test 2", 0, 0)
			.hasTextAt("Test 3", 0, 2)
			.hasTextAt("Test 4", 0, 4)
			.hasNoOtherMoveTo();
	}
}
