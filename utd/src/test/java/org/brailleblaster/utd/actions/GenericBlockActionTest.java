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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;

import org.brailleblaster.utd.BrailleSettings;
import org.brailleblaster.utd.IActionMap;
import org.brailleblaster.utd.ITranslationEngine;
import org.brailleblaster.utd.PageSettings;
import org.brailleblaster.utd.TextSpan;
import org.brailleblaster.utd.matchers.XPathMatcher;
import org.brailleblaster.utd.properties.BrailleTableType;
import org.brailleblaster.utd.properties.EmphasisType;
import org.brailleblaster.utd.properties.UTDElements;
import org.brailleblaster.utd.testutils.MockTranslatorFactory;
import org.brailleblaster.utils.NamespacesKt;
import org.mockito.Mockito;
import org.mwhapples.jlouis.Louis;
import org.mwhapples.jlouis.Louis.TypeForms;
import org.mwhapples.jlouis.TranslationException;
import org.mwhapples.jlouis.TranslationResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class GenericBlockActionTest {
	List<TextSpan> result;
	
	@DataProvider(name="nodesProvider")
	public Iterator<Object[]> nodesprovider() throws TranslationException {
		List<Object[]> dataList = new ArrayList<>();
		// Test translation and assignment to correct blocks, including removing existing brl elements
		Element p = new Element("p");
		Node text = new Text("Some text");
		p.appendChild(text);
		Node bold = new Text("a bold phrase");
		p.appendChild(new Element("br"));
		p.appendChild(bold);
		
		short[] emphasisArr = new short[22];
		Arrays.fill(emphasisArr, TypeForms.PLAIN_TEXT);
		
		String str = "Some texta bold phrase";
		String brlStr = ",\"s texta bold phrase";
		String index = "0 0 0 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21";
		String indexOut = "0 0 0 0 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20";
		ITranslationEngine engineMock = MockTranslatorFactory.createTranslationEngine(str, brlStr, index, indexOut, emphasisArr);
		String expectedXML = String.format("<p>Some text<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"0 0 0 4 5 6 7 8\">,\"s text</utd:brl><br />a bold phrase<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"0 1 2 3 4 5 6 7 8 9 10 11 12\">a bold phrase</utd:brl></p>", NamespacesKt.UTD_NS, NamespacesKt.UTD_NS);
		dataList.add(new Object[] {p, expectedXML, engineMock});
		
		// Test index handling where contraction occurs at the end of a brl element
		short[] emphasisArr2 = new short[10];
		Arrays.fill(emphasisArr2, TypeForms.PLAIN_TEXT);
		
		str = "ActionTest";
		brlStr = ",ac;n,te/";
		index = "0 0 1 2 2 6 6 7 8";
		indexOut = "0 2 3 3 3 3 5 7 8 8";
		engineMock = MockTranslatorFactory.createTranslationEngine(str, brlStr, index, indexOut, emphasisArr2);
		p = new Element("p");
		p.appendChild("Action");
		p.appendChild("Test");
		expectedXML = String.format("<p>Action<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"0 0 1 2 2\">,ac;n</utd:brl>Test<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"0 0 1 2\">,te/</utd:brl></p>", NamespacesKt.UTD_NS, NamespacesKt.UTD_NS);
		dataList.add(new Object[] {p, expectedXML, engineMock});
		return dataList.iterator();
	}
	
	@Test(dataProvider="nodesProvider")
	public void applyToBlock(Node node, String expectedXML, ITranslationEngine contextMock) {
		GenericBlockAction action = new GenericBlockAction();

		result = action.applyTo(node, contextMock);
		assertTrue(result.isEmpty());
		assertEquals(node.toXML(), expectedXML);
	}
	
	@Test
	public void testTranslateString() throws TranslationException {
		String str = "Some text";
		String brlStr = ",\"s text";
		String index = "0 0 0 4 5 6 7 8";
		String indexOut = "0 0 0 0 3 4 5 6 7";
		short[] typeForms = new short[9];
		Arrays.fill(typeForms, TypeForms.BOLD);
		ITranslationEngine contextMock = MockTranslatorFactory.createTranslationEngine(str, brlStr, index, indexOut, typeForms);
		List<TextSpan> textList = new ArrayList<>();
		Element p = new Element("p");
		Node text = new Text("Some text");
		p.appendChild(text);
		p.appendChild(UTDElements.BRL.create());
		
		TextSpan span = new TextSpan(text, "Some text");
		span.setEmphasis(EnumSet.of(EmphasisType.BOLD));
		textList.add(span);
		
		GenericBlockAction action = new GenericBlockAction();
		action.translateString(textList, BrailleTableType.LITERARY, contextMock);
		
		//Secondary test for multiple typeforms
		short[] multipleType = new short[9];
		//byte value = (byte) (TypeForms.BOLD & TypeForms.ITALIC);
		byte value = TypeForms.BOLD + TypeForms.ITALIC;
		Arrays.fill(multipleType, value);
		contextMock = MockTranslatorFactory.createTranslationEngine(str, brlStr, index, indexOut, multipleType);
		
		span.addEmphasis(EmphasisType.ITALICS);
		action.translateString(textList, BrailleTableType.LITERARY, contextMock);
		
		String xmlResult = String.format("<p>Some text<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"0 0 0 4 5 6 7 8\">,\"s text</utd:brl></p>", NamespacesKt.UTD_NS);
		assertEquals(p.toXML(), xmlResult);
	}

	@Test
	public void testMultipleTypeforms() throws TranslationException {
		String str = "Some texta bold phrase";
		String brlStr = ",\"s texta bold phrase";
		String index = "0 0 0 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21";
		String indexOut = "0 0 0 0 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20";
		short[] typeForms = new short[22];
		Arrays.fill(typeForms, 0, 8, TypeForms.PLAIN_TEXT);
		Arrays.fill(typeForms, 9, 22, TypeForms.BOLD);
		ITranslationEngine contextMock = MockTranslatorFactory.createTranslationEngine(str, brlStr, index, indexOut, typeForms);
		List<TextSpan> textList = new ArrayList<>();
		Element p = new Element("p");
		Node text = new Text("Some text");
		p.appendChild(text);
		Node bold = new Text("a bold phrase");
		p.appendChild(UTDElements.BRL.create());
		p.appendChild(bold);
		
		TextSpan span = new TextSpan(text, "Some text");
		span.setEmphasis(EnumSet.noneOf(EmphasisType.class));
		textList.add(span);
		
		TextSpan span2 = new TextSpan(bold, "a bold phrase");
		span2.setEmphasis(EnumSet.of(EmphasisType.BOLD));
		textList.add(span2);		
		
		GenericBlockAction action = new GenericBlockAction();
		action.translateString(textList, BrailleTableType.LITERARY, contextMock);

		String xmlResult = String.format("<p>Some text<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"0 0 0 4 5 6 7 8\">,\"s text</utd:brl>a bold phrase<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"0 1 2 3 4 5 6 7 8 9 10 11 12\">a bold phrase</utd:brl></p>", NamespacesKt.UTD_NS, NamespacesKt.UTD_NS);
		assertEquals(p.toXML(), xmlResult);
	}

	@DataProvider(name="contractionsProvider")
	public Iterator<Object[]> contractionsProvider() throws TranslationException {
		List<Object[]> dataList = new ArrayList<>();
		
		Element p = new Element("p");
		Element strong = new Element("strong");
		Node text = new Text("to");
		strong.appendChild(text);
		p.appendChild(strong);
		Node text2 = new Text(" their");
		p.appendChild(text2);
		
		short[] typeForms = new short[8];
		Arrays.fill(typeForms, TypeForms.PLAIN_TEXT);
		
		String str = "to their";
		String brlStr = "6_!";
		String index = "0 3 3";
		String indexOut = "0 0 0 1 1 1 1 1";
		
		ITranslationEngine engineMock = MockTranslatorFactory.createTranslationEngine(str, brlStr, index, indexOut, typeForms);
		String expectedXML = String.format("<p><strong>to<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"0\">6</utd:brl></strong> their<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"1 1\">_!</utd:brl></p>", NamespacesKt.UTD_NS, NamespacesKt.UTD_NS);
		dataList.add(new Object[] {p, expectedXML, engineMock});
		
		// Ensure multi-cell translation at the beginning does not lead to index errors
		// when oversized inputPos (JLouis bug, fixed in latest snapshot).
		p = new Element("p");
		Element sup = new Element("sup");
		sup.appendChild("*");
		p.appendChild(sup);
		p.appendChild("Advanced");
		
		typeForms = new short[9];
		Arrays.fill(typeForms, TypeForms.PLAIN_TEXT);
		
		str = "*Advanced";
		brlStr = "99,adv.ed";
		// Pretend bad output from LibLouis
		index = "0 0 1 1 2 3 4 4 5 0 0 0";
		indexOut = "0 3 4 5 6 6 6 6 8";
		ITranslationEngine contextMock = MockTranslatorFactory.createTranslationEngine(str, brlStr, index, indexOut, typeForms);
		
		expectedXML = String.format("<p><sup>*<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"0 0\">99</utd:brl></sup>Advanced<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"0 0 1 2 3 3 4\">,adv.ed</utd:brl></p>", NamespacesKt.UTD_NS, NamespacesKt.UTD_NS);
		dataList.add(new Object[] {p, expectedXML, contextMock});
		
		p = new Element("p");
		p.appendChild("a data");
		typeForms = new short[6];
		Arrays.fill(typeForms, TypeForms.PLAIN_TEXT);
		
		str = "a data";
		brlStr = "a data";
		index = "0 1 2 3 4 5";
		indexOut = "0 8 9 10 11 12";
		contextMock = MockTranslatorFactory.createTranslationEngine(str, brlStr, index, indexOut, typeForms);
		expectedXML = String.format("<p>a data<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"%s\">a data</utd:brl></p>", NamespacesKt.UTD_NS, index);
		dataList.add(new Object[] {p, expectedXML, contextMock});
		
		return dataList.iterator();
	}
	
	
	@Test(dataProvider="contractionsProvider")
	public void testContraction(Node node, String expectedXML, ITranslationEngine contextMock) {
		GenericBlockAction action = new GenericBlockAction();		
		action.applyTo(node, contextMock);
		
		assertEquals(node.toXML(), expectedXML);
	}
	
	@DataProvider(name="brNodesProvider")
	public Iterator<Object[]> brNodesProvider() throws TranslationException {
		BrailleSettings brailleSettings = new BrailleSettings();
		brailleSettings.setUseAsciiBraille(true);
		brailleSettings.setMainTranslationTable("en-us-g2.ctb");
		List<Object[]> dataList = new ArrayList<>();
		Element p = new Element("p");
		Node text = new Text("became");
		p.appendChild(text);
		Node text2 = new Text("dry");
		Element br = new Element("br");
		p.appendChild(br);
		p.appendChild(text2);
		
		short[] typeForms = new short[6];
		Arrays.fill(typeForms, TypeForms.PLAIN_TEXT);
		
		Louis louisMock = mock(Louis.class);
		TranslationResult transResult = MockTranslatorFactory.createMockTranslationResult("2came", "0 2 3 4 5");
		doReturn(transResult).when(louisMock).translate("en-us-g2.ctb", "became", typeForms, 0, 0);
		typeForms = new short[3];
		Arrays.fill(typeForms, TypeForms.PLAIN_TEXT);
		transResult = MockTranslatorFactory.createMockTranslationResult("dry", "0 1 2");
		doReturn(transResult).when(louisMock).translate("en-us-g2.ctb", "dry", typeForms, 0, 0);
		ITranslationEngine contextMock = MockTranslatorFactory.createTranslationEngine(louisMock, brailleSettings, new PageSettings());
		String expectedXML = String.format("<p>became<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"0 2 3 4 5\">2came</utd:brl><br /><utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" utd-action=\"SkipAction\" />dry<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"0 1 2\">dry</utd:brl></p>", NamespacesKt.UTD_NS, NamespacesKt.UTD_NS, NamespacesKt.UTD_NS);
		dataList.add(new Object[] {p, expectedXML, contextMock});
		
//		short[] typeForms = new short[9];
//		Arrays.fill(typeForms, TypeForms.PLAIN_TEXT);
//		
//		String str = "became";
//		String brlStr = "2came";
//		String index = "0 2 3 4 5";
//		String indexOut = "0 0 1 2 3 4";
//		ITranslationEngine contextMock = createTranslationEngine(str, brlStr, index, indexOut, typeForms);
//
//		String expectedXML = "<p>became<brl index=\"0 2 3 4 5\">2came</brl><br /></p>";
//		dataList.add(new Object[] {p, expectedXML, contextMock});
		
		// Test index handling where contraction occurs at the end of a brl element
		typeForms = new short[9];
		Arrays.fill(typeForms, TypeForms.PLAIN_TEXT);
		louisMock = mock(Louis.class);
		transResult = MockTranslatorFactory.createMockTranslationResult("my ?roat", "0 1 2 3 5 6 7 8");
		doReturn(transResult).when(louisMock).translate("en-us-g2.ctb", "my throat", typeForms, 0, 0);
		typeForms = new short[6];
		transResult = MockTranslatorFactory.createMockTranslationResult("2came", "0 2 3 4 5");
		doReturn(transResult).when(louisMock).translate("en-us-g2.ctb", "became", typeForms, 0, 0);
		typeForms = new short[3];
		transResult = MockTranslatorFactory.createMockTranslationResult("dry", "0 1 2");
		doReturn(transResult).when(louisMock).translate("en-us-g2.ctb", "dry", typeForms, 0, 0);
		contextMock = MockTranslatorFactory.createTranslationEngine(louisMock, brailleSettings, new PageSettings());
		
		p = new Element("p");
		p.appendChild("my throat");
		Element br1 = new Element("br");
		p.appendChild(br1);
		p.appendChild("became");
		Element br2 = new Element("br");
		p.appendChild(br2);
		p.appendChild("dry");

		expectedXML = String.format("<p>my throat<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"0 1 2 3 5 6 7 8\">my ?roat</utd:brl><br /><utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" utd-action=\"SkipAction\" />became<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"0 2 3 4 5\">2came</utd:brl><br /><utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" utd-action=\"SkipAction\" />dry<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\" index=\"0 1 2\">dry</utd:brl></p>", NamespacesKt.UTD_NS, NamespacesKt.UTD_NS, NamespacesKt.UTD_NS, NamespacesKt.UTD_NS, NamespacesKt.UTD_NS);

		dataList.add(new Object[] {p, expectedXML, contextMock});
		
		return dataList.iterator();
	}

	@Test( dataProvider = "brNodesProvider")
	public void testTranslate(Node node, String expectedXML, ITranslationEngine contextMock) {
		GenericBlockAction action = Mockito.spy(new GenericBlockAction());
		
		IActionMap actionMap = contextMock.getActionMap();
		FlushAction fAction = new FlushAction();		
		actionMap.put(new XPathMatcher("self::br"), fAction);

		result = action.applyTo(node, contextMock);
		

		assertTrue(result.isEmpty());
		/*
		 * 	This shows that flush was applied.
		 * 	The translation did not go through - may need to add something
		 * 	in order to let the passed sublist to get translated
		 * 	properly
		 */
		assertEquals(node.toXML(), expectedXML);
	}
	
	@Test
	public void unknownCharacterTest() {
		testTranslate(
			"\u2345 test",
			"'\\x2345' te\"",
			"0 0 0 0 0 0 0 0 1 2 3 4",
			"<p>⍅ test<utd:brl xmlns:utd=\"http://brailleblaster.org/ns/utd\" xml:space=\"preserve\" index=\"0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 2 3 4\">'\\xf000',,apl ,,func;nal ,,symbol ,,leftw&gt;ds ,,vane'\\xf001' te/</utd:brl></p>"
		);
	}
	
	void testTranslate(String inputText, String outputBraille, String inputPos, String expectedXML) {
		BrailleSettings brailleSettings = new BrailleSettings();
		brailleSettings.setUseAsciiBraille(true);
		brailleSettings.setMainTranslationTable("en-us-g2.ctb");
		
//		short[] typeForms = new short[9];
//		Arrays.fill(typeForms, TypeForms.PLAIN_TEXT);
//		Louis louisMock = mock(Louis.class);
//		TranslationResult transResult = MockTranslatorFactory.createMockTranslationResult(outputBraille, inputPos);
//		doReturn(transResult).when(louisMock).translate("en-us-g2.ctb", "dry", typeForms, 0, 0);
		
		ITranslationEngine contextMock = MockTranslatorFactory.createTranslationEngine(new Louis(), brailleSettings, new PageSettings());
		
		GenericBlockAction action = new GenericBlockAction();		
		Element p = new Element("p");
		p.appendChild(inputText);
		
		action.applyTo(p, contextMock);
		assertEquals(p.toXML(), expectedXML);
	}
}
