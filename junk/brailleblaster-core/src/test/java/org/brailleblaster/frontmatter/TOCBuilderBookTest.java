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
package org.brailleblaster.frontmatter;
/*
import static org.brailleblaster.testrunners.ViewTestRunner.doPendingSWTWork;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.File;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.brailleblaster.easierxml.StyleEditor;
import org.brailleblaster.easierxml.StyleEditorMapList;
import org.brailleblaster.easierxml.StyleEditorMapList.Mapping;
import org.brailleblaster.testrunners.TOCViewTestRunner;
import org.brailleblaster.testrunners.ViewTestRunner;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import org.brailleblaster.BBIni;
import org.brailleblaster.TestGroups;
*/
//@Test(groups = TestGroups.BROKEN_TESTS)
public class TOCBuilderBookTest {
	/**
	TODO: Port to TOCBuilderBBX and modules... eventually
	
	private static final File TOC_RAW_FILE = Paths.get("src", "test", "resources", "org", "brailleblaster", "easierxml", "toc.raw.xml").toFile();
	TOCViewTestRunner tocTest;
	SWTBot bot;
	SWTBotStyledText textViewBot;
	TOCBuilder tocBuilder;

	@BeforeClass
	public void setupSettings() {
		//Reset user settings as they will break tests if changed
		BBIni.getPropertyFileManager().save(StyleEditor.SETTINGS_KEY_SIMPLE_NAMES, "true");
		BBIni.getPropertyFileManager().save(StyleEditor.SETTINGS_KEY_SIMPLE_NESTED, "true");
		BBIni.getPropertyFileManager().save(StyleEditor.SETTINGS_KEY_XML_NAMES, "false");
	}

	public void setupTOCBuilderForTocRawTestFile() {
		Document doc = new XMLHandler().load(TOC_RAW_FILE);
		Element root = (Element) XMLHandler.query(doc, "descendant::book:level2[@class='contents']").get(0);

		tocTest = new TOCViewTestRunner(doc, root);
		tocBuilder = tocTest.builder;
		bot = tocTest.bot;
		textViewBot = tocTest.textViewBot;
	}

	@Test(enabled = false)
	//Uses toc.raw.gauntlet.xml
	public void tocGauntlet() {
		setupTOCBuilderForTocRawTestFile();

		assertEquals(textViewBot.widget.getCaretOffset(), tocBuilder.editor.mapList.getMappings().get(0).getLength());

		//skip head
		bot.button("Next").click();
		doPendingSWTWork();

		tocGauntlet_next(tocTest, //"heading-start",
				"Next", "[Cell 5 Heading][img][/img]The Test TOC[/Cell 5 Heading]",
				"Cntr Heading", "[Centered Heading][img][/img]The Test TOC[/Centered Heading]");
		tocGauntlet_next(tocTest, //"heading-ch1",
				"Next", "[Centered Heading]Chapter 1[/Centered Heading]",
				"Cntr Heading NB", "[Centered Heading No Blank]Chapter 1[/Centered Heading No Blank]");
		tocGauntlet_next(tocTest, //"toc-text-emphasis",
				"Next", "[...]▪ [B]Strong with page and list[/B] i[/...]",
				"Ok", "[TOC Title]▪ [B]Strong with page and list[/B][TOC Page]i[/TOC Page][/TOC Title]");
		tocGauntlet_next(tocTest, //"toc-list-emphasis",
				"Next", "[...][...][...][B]Strong with page and list[/B] vi[/...][/...][/...]",
				"Ok", "[TOC Title][...][...][B]Strong with page and list[/B][TOC Page]vi[/TOC Page][/...][/...][/...]");
		tocGauntlet_next(tocTest, //"toc-list-normal",
				"Next", "[...][...][...][B]Strong with page and list[/B] vi[/...][/...][/...]",
				"Ok", "[TOC Title][...][...][B]Strong with page and list[/B][TOC Page]vi[/TOC Page][/...][/...][/...]");

		assertEquals(textViewBot.getSelection(), "");
	}

	public static void tocGauntlet_next(TOCViewTestRunner tocTest,
			String nextButton, String expectedSelection, String okButton, String expectedOutput) {
		if (nextButton != null) {
			tocTest.bot.button(nextButton).click();
			doPendingSWTWork();
		}

		assertFalse(StringUtils.isBlank(tocTest.textViewBot.getSelection()), "Nothing is selected");

		assertEquals(ViewTestRunner.stripNewlines(tocTest.textViewBot.getSelection()), expectedSelection);

		int startPos = tocTest.textViewWidget.getSelectionRange().x;

//		Element testElem = (Element) XMLHandler.query(tocBuilder.editor.getCurrentRoot(), "descendant::*[@testid='{}']", testid).get(0);
//		StartElementMapping testMapping = (StartElementMapping) tocBuilder.editor.mapList.getMappingFromNode(testElem);
//		List<Mapping> testAllMappings = tocBuilder.editor.mapList.getAllElementMappings(testMapping);
//		assertEquals(tocBuilder.editor.getSelectedMappings(), testAllMappings);
//		StartElementMapping testMapping = (StartElementMapping) tocBuilder.editor.getSelectedMappings().get(0);
		//Execute and verify changes
		tocTest.bot.button(okButton).click();
		doPendingSWTWork();

		//Refind start element since it might of changed
//		testElem = (Element) XMLHandler.query(tocBuilder.editor.getCurrentRoot(), "descendant::*[@testid='{}']", testid).get(0);
//		testMapping = (StartElementMapping) tocBuilder.editor.mapList.getMappingFromNode(testElem);
//		assertEquals(ViewTestRunner.stripNewlines(getAllElementText(testMapping)), expectedOutput);
		assertThat(ViewTestRunner.stripNewlines(tocTest.textViewBot.getText().substring(startPos)), startsWith(expectedOutput));
	}

	@Test
	public void nextSimpleTitleTest() throws ParseException {
		setupTOCBuilderForTocRawTestFile();

		Node testNode = tocBuilder.editor.getCurrentRoot().query("descendant::*[@testid='simpleTOC']").get(0);
		Mapping testMapping = tocBuilder.editor.mapList.getMappingFromNode(testNode);
		int testMappingIndex = tocBuilder.editor.mapList.getMappings().indexOf(testMapping);
		System.out.println("Test mapping index: " + testMappingIndex);

		tocBuilder.editor.view.setCaretOffset(testMapping.getPosStart());
		doPendingSWTWork();

		//Incriment title depth
		textViewBot.pressShortcut(SWT.ALT, '=');
		textViewBot.pressShortcut(0, '=');
		doPendingSWTWork();

		tocGauntlet_next(tocTest,
				"Next", "[L1-3][Span]▪ [B]Then you just have to[/B] l [/Span][/L1-3]",
				"List Entry/Title", "[T3-5][...]▪ [B]Then you just have to[/B][TOC Page]l[/TOC Page][/...][/T3-5]");

		testMapping = tocBuilder.editor.mapList.getMappings().get(testMappingIndex);
		assertEquals(testMapping.getNode().toXML(),
				"<li xmlns:utd=\"http://brailleblaster.org/ns/utd\" testid=\"simpleTOC\" utd:toc-depth=\"3\" utd:toc-maxdepth=\"5\" utd:toc-type=\"title\">"
				+ "<p>▪ <strong>Then you just have to</strong><span utd:toc-type=\"page\">l</span></p>"
				+ "</li>");

		//Check if next title was automatically selected
		assertEquals(ViewTestRunner.stripNewlines(textViewBot.getSelection()), "[L1-3]Get approval lix [/L1-3]");
	}

	/**
	 * When cursor is on a EndElementMapping, pressing next should go to the next adjacent element
	
	@Test
	public void nextNestedTitleOnEndElement() {
		setupTOCBuilderForTocRawTestFile();

		Mapping testMapping = tocBuilder.editor.mapList.getMappings().get(22);
		tocBuilder.editor.view.setCaretOffset(testMapping.getPosStart());
		doPendingSWTWork();

		bot.button("Next").click();
		doPendingSWTWork();

		assertEquals(ViewTestRunner.stripNewlines(textViewBot.getSelection()), "[L1-3][Span][B]Write Brailleblaster[/B] vi[/Span][/L1-3]");
	}

	@Test
	public void nestedListTest() {
		setupTOCBuilderForTocRawTestFile();

		Mapping testMapping = tocBuilder.editor.mapList.getMappings().get(43);
		tocBuilder.editor.view.setCaretOffset(tocBuilder.editor.mapList.getMappings().get(38).getPosStart());
		doPendingSWTWork();

		bot.button("Next").click();
		doPendingSWTWork();
		assertEquals(textViewBot.getSelection(), "▪ [B]Then you can[/B] xxi");

		bot.button("List Entry/Title").click();
		doPendingSWTWork();

		assertThat(textViewBot.getText().substring(testMapping.getPosStart()),
				startsWith("[T1-3]▪ [B]Then you can[/B][TOC Page]xxi[/TOC Page][/T1-3]"));

		assertEquals(ViewTestRunner.stripNewlines(textViewBot.getSelection()), "[L1-3][Span][B]Do bugfixes[/B] xxxii[/Span][/L1-3]");
	}

	@Test
	public void nextTitleOnPageNum() {
		setupTOCBuilderForTocRawTestFile();

		Mapping testMapping = tocBuilder.editor.mapList.getMappings().get(68);
		assertEquals(testMapping.getOutput(), "[Page]");
		assertEquals(testMapping.getNode().getValue(), "vii");

		tocBuilder.editor.view.setCaretOffset(testMapping.getPosStart());
		doPendingSWTWork();

		bot.button("Next").click();
		doPendingSWTWork();

		assertThat(textViewBot.getSelection(), not(containsString("Page")));
		assertThat(textViewBot.getSelection(), not(containsString("vii")));
	}

	@Test
	public void nextTitleOnPartialElement() {
		setupTOCBuilderForTocRawTestFile();

		Mapping testMapping = tocBuilder.editor.mapList.getMappings().get(43);
		assertEquals(testMapping.getOutput(), "▪ ");

		tocBuilder.editor.view.setCaretOffset(testMapping.getPosStart());
		doPendingSWTWork();

		bot.button("Next").click();
		doPendingSWTWork();

		assertEquals(textViewBot.getSelection(), "▪ [B]Then you can[/B] xxi");
	}

	@Test
	public void nextTitleNoEmphasis() {
		setupTOCBuilderForTocRawTestFile();

		Node testNode = tocBuilder.editor.getCurrentRoot().query("descendant::*[@testid='noEmphasis']").get(0);
		Mapping testMapping = tocBuilder.editor.mapList.getMappingFromNode(testNode);

		textViewBot.widget.setCaretOffset(testMapping.getPosStart());
		doPendingSWTWork();

		bot.button("Next").click();
		doPendingSWTWork();
		assertEquals(textViewBot.getSelection().trim(), "[L1-3]Get approval lix [/L1-3]");

		bot.button("List Entry/Title").click();
		doPendingSWTWork();

		assertEquals(tocBuilder.editor.getText((StyleEditorMapList.StartElementMapping) tocBuilder.editor.mapList.getMappings().get(87)),
				"[T1-3]Get approval[TOC Page]lix[/TOC Page][/T1-3]");
	}
 */
}
