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
package org.brailleblaster.utd.formatters;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;

import org.brailleblaster.utd.BrailleSettings;
import org.brailleblaster.utd.Cursor;
import org.brailleblaster.utd.FormatSelector;
import org.brailleblaster.utd.PageBuilder;
import org.brailleblaster.utd.Style;
import org.brailleblaster.utd.StyleMap;
import org.brailleblaster.utd.StyleStack;
import org.brailleblaster.utd.UTDTranslationEngine;
import org.brailleblaster.utd.matchers.NodeNameMatcher;
import org.brailleblaster.utd.properties.UTDElements;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.Iterables;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LiteraryFormatterTest {
	final static int ITERATIONS = 20; //Number of <p> tags to be generated
	@Test(dataProvider="mapAndDocumentProvider")
	public void testFormatting(StyleMap map, Document doc){
		UTDTranslationEngine engine = new UTDTranslationEngine();
		FormatSelector formatter = new FormatSelector(map, new StyleStack(), engine);
		Set<PageBuilder> results = new LinkedHashSet<>();
		PageBuilder pageBuilder = new PageBuilder(engine, new Cursor());
		results.add(pageBuilder);
		Set<PageBuilder> pbs = new LinkedHashSet<>();
		pbs.add(pageBuilder);
		results.addAll(new LiteraryFormatter().format(doc.getChild(0), new StyleStack(), pbs, formatter));
		pageBuilder = Iterables.getLast(results);
		int linesOnPage = engine.getBrailleSettings().getCellType().getLinesForHeight(BigDecimal.valueOf(engine.getPageSettings().getDrawableHeight()));
		//ITERATIONS % linesOnPage - 1 is the ending line of a page
		assertEquals(pageBuilder.getY(), ITERATIONS % linesOnPage - 1);
	}
	
	//This is disabled due to a possibly temporary change in how new pages are maintained.
	//A set in FormatSelector will maintain the page builders, LiteraryFormatter will only return
	//the pages it worked on. 
//	@Test(dataProvider="mapAndDocumentProvider")
	public void testNewPageBefore(StyleMap map, Document doc){
		UTDTranslationEngine engine = new UTDTranslationEngine();
		StyleStack mockStyleStack = mock(StyleStack.class);
		when(mockStyleStack.getNewPagesBefore()).thenReturn(1);
		FormatSelector formatter = new FormatSelector(map, mockStyleStack, engine);
		Set<PageBuilder> results = new LinkedHashSet<>();
		PageBuilder pageBuilder = new PageBuilder(engine, new Cursor());
		results.add(pageBuilder);
		Set<PageBuilder> pbs = new LinkedHashSet<>();
		results.addAll(new LiteraryFormatter().format(doc.getChild(0), mockStyleStack, pbs, formatter));
		pageBuilder = Iterables.getLast(results);
		assertEquals(pageBuilder.getBraillePageNumber().getBraillePageNumber(engine), Integer.toString(ITERATIONS + 1));
	}
	
	@DataProvider(name="mapAndDocumentProvider")
	public Object[][] mapAndDocumentProvider(){
		Element root = new Element("html");
		Document doc = new Document(root);
		Element curParent = root;
		for(int i = 0; i < ITERATIONS; i++){
			Element p = new Element("p");
			p.appendChild("Some text in a paragraph");
			Element brl = UTDElements.BRL.create();
			brl.appendChild(",s text 9 p>agraph");
			p.appendChild(brl);
			curParent.appendChild(p);
			curParent = p;
		}
		
		return new Object[][]{
				new Object[]{getStyleMap(), doc}
		};
	}
	
	private StyleMap getStyleMap(){
		StyleMap map = new StyleMap();
		map.getNamespaces().addNamespace("xhtml", "http://www.w3.org/555555/xhtml");
		Style style = new Style();
		style.setName("test");
		style.setLinesBefore(1);
		style.setFirstLineIndent(2);
		style.setIndent(0);
		style.setDontSplit(true);
		map.put(new NodeNameMatcher("p"), style);
		return map;
	}
	
	@DataProvider(name="elementProvider")
	public Object[][] brlProvider() {
		Element parent = new Element("p");
		Element child1 = new Element("Child1");
		child1.appendChild("First child");
		
		Element brl1 = UTDElements.BRL.create();
		brl1.addAttribute(new Attribute("printPage", "5"));
		brl1.addAttribute(new Attribute("printPageBrl", "#e"));
		brl1.addAttribute(new Attribute("pageType", "P_PAGE"));
		brl1.appendChild("This is the first brl element.");
		child1.appendChild(brl1);
		parent.appendChild(child1);
		
		Element child2 = new Element("Child2");
		child2.appendChild("Second child");
		
		Element brl2 = UTDElements.BRL.create();
		brl2.appendChild(" This is the second brl element.");
		child2.appendChild(brl2);
		parent.appendChild(child2);
		
		String result1 = "THIS IS THE FIRST BRL ELEMENT. THIS   #E\r\nIS THE SECOND BRL ELEMENT.\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n   1\f";
//		String result1 = "THIS IS THE FIRST BRL ELEMENT. THIS   #E\nIS THE SECOND BRL ELEMENT.\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n1?";
		
		//////////////////////////////////////////////////////////////////////////
		
		Element parent2 = new Element("p");
		Element child3 = new Element("p");
		child3.appendChild("First child");
		
		Element brl3 = UTDElements.BRL.create();
		brl3.addAttribute(new Attribute("printPage", "20"));
		brl3.addAttribute(new Attribute("printPageBrl", "#bj"));
		brl3.addAttribute(new Attribute("pageType", "T_PAGE"));
		brl3.appendChild("This is the first long line of the page.");
		child3.appendChild(brl3);
		parent2.appendChild(child3);
		
		Element child4 = new Element("p");
		child4.appendChild("Second child");
		
		Element brl4 = UTDElements.BRL.create();
		brl4.addAttribute(new Attribute("printPage", "36"));
		brl4.addAttribute(new Attribute("printPageBrl", "#cf"));
		brl4.addAttribute(new Attribute("pageType", "SPECIAL"));
		brl4.appendChild("This is the fourth brl element.");
		child4.appendChild(brl4);
		parent2.appendChild(child4);
		
		String result2 = "THIS IS THE FIRST LONG LINE OF THE   #BJ\r\nPAGE.\r\n-------------------------------------#CF\r\nTHIS IS THE FOURTH BRL ELEMENT.\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n   1\f";
		
		return new Object[][] {				
				{parent, new Cursor(), result1}, {parent2, new Cursor(), result2}
		};
	}
	
	@Test(dataProvider="elementProvider")
	public void insertPrintPageIndicator (Element parent, Cursor cursor, String result) {
		
		UTDTranslationEngine engine = new UTDTranslationEngine();
		StyleStack mockStyleStack = mock(StyleStack.class);
		when(mockStyleStack.getLineSpacing()).thenReturn(1);
		
		FormatSelector formatSelector = new FormatSelector(getStyleMap(), mockStyleStack, engine);
		PageBuilder pageBuilder = new PageBuilder(engine, cursor);
		
		Formatter formatter = new LiteraryFormatter();
		Set<PageBuilder> pbs = new LinkedHashSet<>();
		pbs.add(pageBuilder);
		formatter.format(parent, mockStyleStack, pbs, formatSelector);
		
	}
	
	static final String PAGE_INDICATOR = "-------------------------------------";
	
	@Test
	public void insertPrintPageIndicatorAtTop(){
		Element root = new Element("book");
		Element p = new Element("p");
		p.appendChild("test");
		Element brl = UTDElements.BRL.create();
		brl.appendChild("te/");
		p.appendChild(brl);
		Element pagenum = new Element("pagenum");
		pagenum.appendChild("1");
		Element pagenumBrl = UTDElements.BRL.create();
		pagenumBrl.addAttribute(new Attribute("printPage", "1"));
		pagenumBrl.addAttribute(new Attribute("printPageBrl", "#a"));
		pagenumBrl.addAttribute(new Attribute("pageType", "NORMAL"));
		pagenum.appendChild(pagenumBrl);
		
		root.appendChild(p);
		root.appendChild(pagenum);
		
		//Test that page indicator is inserted on line 1 when preceded by text
		UTDTranslationEngine engine = generateDefaultEngine();
		StyleStack mockSS = mock(StyleStack.class);
		when(mockSS.getLinesBefore()).thenReturn(1);
		when(mockSS.getLineSpacing()).thenReturn(1);
		PageBuilder pageBuilder = new PageBuilder(engine, new Cursor());
		Set<PageBuilder> pbs = new LinkedHashSet<>();
		pbs.add(pageBuilder);
		FormatSelector formatter = new FormatSelector(getStyleMap(), mockSS, engine);
		pageBuilder = Iterables.getLast(new LiteraryFormatter().format(root, mockSS, pbs, formatter));
		assertTrue(pageBuilder.toBRF().contains(PAGE_INDICATOR), "Expected page indicator on line 2\n" + pageBuilder);
		
		//Test that page indicator is not inserted on line 0
		Element root2 = new Element("book");
		root2.appendChild(pagenum.copy());
		root2.appendChild(p.copy());
		pageBuilder = new PageBuilder(engine, new Cursor());
		pbs.clear();
		pbs.add(pageBuilder);
		formatter = new FormatSelector(getStyleMap(), mockSS, engine);
		pageBuilder = Iterables.getLast(new LiteraryFormatter().format(root2, mockSS, pbs, formatter));
		assertFalse(pageBuilder.toBRF().contains(PAGE_INDICATOR), "Expected no page indicator\n" + pageBuilder);
	}
	
	@Test
	public void insertPrintPageIndicatorAtBottom(){
		Element root = new Element("book");
		Element p = new Element("p");
		for(int i = 0; i < 23; i++){
			p = new Element("p");
			p.appendChild("test");
			Element brl = UTDElements.BRL.create();
			brl.appendChild("te/");
			p.appendChild(brl);
			root.appendChild(p);
		}
		Element pagenum = new Element("pagenum");
		pagenum.appendChild("1");
		Element pagenumBrl = UTDElements.BRL.create();
		pagenumBrl.addAttribute(new Attribute("printPage", "1"));
		pagenumBrl.addAttribute(new Attribute("printPageBrl", "#a"));
		pagenumBrl.addAttribute(new Attribute("pageType", "NORMAL"));
		pagenum.appendChild(pagenumBrl);
		root.appendChild(pagenum);
		root.appendChild(p.copy());
		
		//Test that page indicator stays on second-to-last line of page
		UTDTranslationEngine engine = generateDefaultEngine();
		PageBuilder pb = new PageBuilder(engine, new Cursor());
		Set<PageBuilder> pbs = new LinkedHashSet<>();
		pbs.add(pb);
		StyleStack ss = new StyleStack();
		FormatSelector formatter = new FormatSelector(getStyleMap(), ss, engine);
		pb = Iterables.getLast(new LiteraryFormatter().format(root, ss, pbs, formatter));
		assertTrue(pb.toBRF().contains(PAGE_INDICATOR), "Expected page indicator on line 23\n" + pb);
		
		//Test that page indicator disappears on final line of page
		root.insertChild(p.copy(), 0);
		pb = new PageBuilder(engine, new Cursor());
		pbs.clear();
		pbs.add(pb);
		pb = Iterables.get(new LiteraryFormatter().format(root, ss, pbs, formatter), 0);
		assertFalse(pb.toBRF().contains(PAGE_INDICATOR), "Unexpected page indicator on line 24\n" + pb);
		
		//Test that page indicator stays on second-to-last line of page when there is no preceding text
		root = new Element("book");
		Element h1 = new Element("h1");
		h1.appendChild("test");
		Element h1Brl = UTDElements.BRL.create();
		h1Brl.appendChild("te/");
		h1.appendChild(h1Brl);
		root.appendChild(h1);
		root.appendChild(pagenum.copy());
		root.appendChild(p.copy());
		StyleMap newSM = getStyleMap();
		Style style = new Style();
		style.setName("test");
		style.setLinesBefore(1);
		style.setLinesAfter(23);
		newSM.put(new NodeNameMatcher("h1"), style);
		
		pb = new PageBuilder(engine, new Cursor());
		pbs.clear();
		pbs.add(pb);
		formatter = new FormatSelector(newSM, ss, engine);
		pb = Iterables.get(new LiteraryFormatter().format(root, ss, pbs, formatter), 0);
		assertTrue(pb.toBRF().contains(PAGE_INDICATOR), "Expected page indicator on line 23\n" + pb);

		//Test that page indicator is not inserted on last line of page and also starts a new page when there is no preceding text
		root.insertChild(p.copy(), 0);
		pb = new PageBuilder(engine, new Cursor());
		pbs.clear();
		pbs.add(pb);
		Set<PageBuilder> results = new LiteraryFormatter().format(root, ss, pbs, formatter);
		pb = Iterables.get(results, 0);
		assertFalse(pb.toBRF().contains(PAGE_INDICATOR), "Unexpected page indicatorp\n" + pb);
		assertTrue(results.size() > 1 || pb.getBraillePageNumber().getPageNumber() == 2, "Page indicator on final line should create a new page\n" + pb);
	}
	
	@Test
	public void insertBlankPrintPageIndicator(){
		Element root = new Element("book");
		Element p = new Element("p");
		p.appendChild("test");
		Element brl = UTDElements.BRL.create();
		brl.appendChild("te/");
		p.appendChild(brl);
		Element pagenum = new Element("pagenum");
		Element pagenumBrl = UTDElements.BRL.create();
		pagenumBrl.addAttribute(new Attribute("printPage", ""));
		pagenumBrl.addAttribute(new Attribute("printPageBrl", ""));
		pagenumBrl.addAttribute(new Attribute("pageType", "NORMAL"));
		pagenum.appendChild(pagenumBrl);
		
		root.appendChild(p);
		root.appendChild(pagenum);
		
		//Test that page indicator is inserted on line 1 when preceded by text
		UTDTranslationEngine engine = generateDefaultEngine();
		StyleStack mockSS = mock(StyleStack.class);
		when(mockSS.getLinesBefore()).thenReturn(1);
		when(mockSS.getLineSpacing()).thenReturn(1);
		PageBuilder pageBuilder = new PageBuilder(engine, new Cursor());
		Set<PageBuilder> pbs = new LinkedHashSet<>();
		pbs.add(pageBuilder);
		FormatSelector formatter = new FormatSelector(getStyleMap(), mockSS, engine);
		pageBuilder = Iterables.getLast(new LiteraryFormatter().format(root, mockSS, pbs, formatter));
		assertTrue(pageBuilder.toBRF().contains(PAGE_INDICATOR), "Expected page indicator on line 2\n" + pageBuilder);
		
		//Test that page indicator is not inserted on line 0
		Element root2 = new Element("book");
		root2.appendChild(pagenum.copy());
		root2.appendChild(p.copy());
		pageBuilder = new PageBuilder(engine, new Cursor());
		pbs.clear();
		pbs.add(pageBuilder);
		formatter = new FormatSelector(getStyleMap(), mockSS, engine);
		pageBuilder = Iterables.getLast(new LiteraryFormatter().format(root2, mockSS, pbs, formatter));
		assertTrue(pageBuilder.toBRF().contains(PAGE_INDICATOR), "Expected print page indicator on line 1\n" + pageBuilder);
	}
	
//	@Test
	public void insertSeparatorLines (){
		StyleMap map = getStyleMap();
		
		Element root = new Element("html");
		Document doc = new Document(root);
		Element curParent = root;
		for(int i = 0; i < 2; i++){
			Element p = new Element("p");
			p.appendChild("Some text in a paragraph");
			Element brl = UTDElements.BRL.create();
			brl.addAttribute(new Attribute("printPage", "1"));
			brl.addAttribute(new Attribute("printPageBrl", "#a"));
			brl.addAttribute(new Attribute("pageType", "NORMAL"));
			brl.appendChild(",s text 9 p>agraph");
			p.appendChild(brl);
			curParent.appendChild(p);
			curParent = p;
		}
		
		UTDTranslationEngine engine = new UTDTranslationEngine();
		StyleStack mockStyleStack = mock(StyleStack.class);
		when(mockStyleStack.getStartSeparator()).thenReturn("7");
		when(mockStyleStack.getEndSeparator()).thenReturn("g");
		
		FormatSelector formatter = new FormatSelector(map, mockStyleStack, engine);
		Set<PageBuilder> results = new LinkedHashSet<>();
		PageBuilder pageBuilder = new PageBuilder(engine, new Cursor());
		results.add(pageBuilder);
		Set<PageBuilder> pbs = new LinkedHashSet<>();
		pbs.add(pageBuilder);
		results.addAll(new LiteraryFormatter().format(doc.getChild(0), mockStyleStack, pbs, formatter));
		pageBuilder = Iterables.getLast(results);

		String result = """
				77777777777777777777777777777777777   #A\r
				7777777777777777777777777777777777777777\r
				--------------------------------------#A\r
				,S TEXT 9 P>AGRAPH\r
				7777777777777777777777777777777777777777\r
				--------------------------------------#A\r
				,S TEXT 9 P>AGRAPH\r
				GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG\r
				GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG\r
				GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG\r
				\r
				\r
				\r
				\r
				\r
				\r
				\r
				\r
				\r
				\r
				\r
				\r
				\r
				\f""";
		
		assertEquals(pageBuilder.toBRF(), result);
	}
	
	private UTDTranslationEngine generateDefaultEngine(){
		BrailleSettings brailleSettings = new BrailleSettings();
		brailleSettings.setUseAsciiBraille(true);
		UTDTranslationEngine newEngine = new UTDTranslationEngine();
		newEngine.setBrailleSettings(brailleSettings);
		return newEngine;
	}

}
