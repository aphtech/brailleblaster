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

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.bbx.BookToBBXConverter;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.BBXDocFactory;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.utd.actions.TransNoteAction;
import org.brailleblaster.utd.exceptions.NodeException;
import org.brailleblaster.utd.properties.EmphasisType;
import org.brailleblaster.utd.toc.TOCAttributes;
import org.brailleblaster.exceptions.BBNotifyException;
import org.brailleblaster.utd.utils.UTDHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.brailleblaster.testrunners.ViewTestRunner.doPendingSWTWork;
import static org.testng.Assert.assertEquals;

public class TOCBuilderTest {

	public static void openTocTools(BBTestRunner test) {
		if (!TOCBuilderBBX.isEnabled(test.manager)) {
			test.openMenuItem(TopMenu.TOOLS, "TOC Builder");
		}
	}

	@Test(enabled = false)
	public void pageNumButton_pageInSameText() {
		BBTestRunner test = new BBTestRunner("", "<p>Stuff R1</p><p>More things</p>");
		openTocTools(test);

		//TODO: TOC selection?
//		test.bot.button("Next").click();
//		assertEquals(test.styleViewTools.getSelectionStripped(), "[Body Text]Stuff R1[/Body Text]");
//		int start = test.styleViewWidget.getSelectionRange().x;
		test.textViewTools.navigateToText("Stuff");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);

		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.onlyChildIsText("Stuff R1")
						.isBlockWithStyle("T1-3")
				).nextChildIs(childAssert -> childAssert
						.onlyChildIsText("More things")
						.isBlockWithStyle("Body Text")
				).noNextChild();

		test.textViewTools.navigateToText("R1");
		test.textViewTools.pressShortcut(SWT.SHIFT, SWT.ARROW_RIGHT, '\0');
		test.textViewTools.pressShortcut(SWT.SHIFT, SWT.ARROW_RIGHT, '\0');
		assertEquals(test.textViewBot.getSelection(), "R1");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);

		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.onlyChildIsText("R1")
								.hasStyle("TOC Page")
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.onlyChildIsText("More things")
						.isBlockWithStyle("Body Text")
				).noNextChild();
	}

	@Test(enabled = false)
	public void pageNumButton_pageInWrapperElement() {
		BBTestRunner test = new BBTestRunner("", "<p>Stuff <strong>R1</strong></p><p>More things</p>");
		openTocTools(test);

		//TODO: toc selection?
//		tocTest.bot.button("Next").click();
//		assertEquals(tocTest.getTextViewSelectionStripped(), "[Body Text]Stuff [B]R1[/B][/Body Text]");
//		int start = tocTest.textViewWidget.getSelectionRange().x;
		test.textViewTools.navigateToText("Stuff");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff ")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("R1")
								.isInlineEmphasis(EmphasisType.BOLD)
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.hasText("More things")
						.isBlockWithStyle("Body Text")
				).noNextChild();

		test.textViewTools.navigateToText("1");
		test.selectBreadcrumbsAncestor(0, BBX.INLINE.EMPHASIS::assertIsA);
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasStyle("TOC Page")
								.nextChildIs(childAssert3 -> childAssert3
										.isInlineEmphasis(EmphasisType.BOLD)
										.hasText("R1")
								)
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.hasText("More things")
						.isBlockWithStyle("Body Text")
				).noNextChild();
	}

	@Test(enabled = false)
	public void pageNumButton_pageInWrapperElement_noSelection() {
		BBTestRunner test = new BBTestRunner("", "<p>Stuff <strong>R1</strong></p><p>More things</p>");
		openTocTools(test);

		//TODO: toc selection?
//		test.bot.button("Next").click();
//		assertEquals(test.getTextViewSelectionStripped(), "[Body Text]Stuff [B]R1[/B][/Body Text]");
//		int start = test.textViewWidget.getSelectionRange().x;
		test.textViewTools.navigateToText("Stuff");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff ")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("R1")
								.isInlineEmphasis(EmphasisType.BOLD)
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.hasText("More things")
						.isBlockWithStyle("Body Text")
				).noNextChild();

		test.textViewTools.navigateToText("1");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasStyle("TOC Page")
								.nextChildIs(childAssert3 -> childAssert3
										.isInlineEmphasis(EmphasisType.BOLD)
										.hasText("R1")
								).noNextChild()
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.hasText("More things")
						.isBlockWithStyle("Body Text")
				).noNextChild();
	}

	// broken due to #5586
	@Test(expectedExceptions = BBNotifyException.class,enabled = false)
	public void pageNumButton_Text_OnlyTextInBlock_issue4702_issue5586() {
		BBTestRunner test = new BBTestRunner("", "<p>3</p>");
		openTocTools(test);

		test.textViewTools.navigateToText("3");
		// should throw exception
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);
//		test.assertRootSection_NoBrlCopy()
//				.nextChildIs(childAssert -> childAssert
//						.hasStyle("Body Text")
//						.nextChildIs(childAssert2 -> childAssert2
//								.hasStyle("TOC Page")
//								.hasText("3")
//						).noNextChild()
//				).noNextChild();
	}

	@Test(enabled = false)
	public void pageNumButton_pageInSiblingBlock_rt4702_page() {
		BBTestRunner test = new BBTestRunner("", "<p>stuff</p><p>3</p>");
		openTocTools(test);

		test.textViewTools.navigateToText("stuff");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasStyle("TOC Page")
								.hasText("3")
						)
				).noNextChild();
	}

	@Test(enabled = false)
	public void pageNumButton_pageInSiblingBlock_rt4702_unrelated() {
		BBTestRunner test = new BBTestRunner("", "<p>stuff</p><p>things 3</p>");
		openTocTools(test);

		test.textViewTools.navigateToText("stuff");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("stuff")
				).nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("Body Text")
						.hasText("things 3")
				).noNextChild();
	}

	@Test(enabled = false)
	public void pageNumButton_pageInSiblingBlock_rt4702_printPageNum() {
		BBTestRunner test = new BBTestRunner("", "<p>stuff</p><pagenum>3</pagenum>");
		openTocTools(test);

		test.textViewTools.navigateToText("stuff");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("stuff")
				).nextChildIs(childAssert -> childAssert
						.isBlock(BBX.BLOCK.PAGE_NUM)
						.hasText("3")
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void pageNumButton_pageInSiblingBlock_rt4702_none() {
		BBTestRunner test = new BBTestRunner("", "<p>stuff</p>");
		openTocTools(test);

		test.textViewTools.navigateToText("stuff");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("stuff")
				).noNextChild();
	}
	
	@Test(expectedExceptions = BBNotifyException.class,enabled = false)
	public void pageNumButton_pageInSameWord_singleEntry_issue6025_issue5908_issue6252() {
		BBTestRunner test = new BBTestRunner("", 
				"<p>the Meaning of Division..........33</p>");
		openTocTools(test);
		
		test.textViewTools.navigateToText("the");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		
		test.textViewTools.navigateToText("3", 1);
		// should throw exception
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);
		
//		test.assertRootSectionFirst_NoBrlCopy()
//				.isBlockWithStyle("T1-3")
//				.onlyChildIs()
//				.isSpan(BBX.SPAN.OTHER)
//				.hasText("the Meaning of Division..........33");
	}
	
	@Test(expectedExceptions = BBNotifyException.class,enabled = false)
	public void pageNumButton_pageInSameWord_multipleEntries_firstWithPage_issue6025_issue5908() {
		BBTestRunner test = new BBTestRunner("", 
				"<p>more than you bargined for 36</p>"
				+ "<p>the Meaning of Division..........33</p>");
		openTocTools(test);
		
		test.textViewTools.selectFromTo("more", "the");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		
		test.textViewTools.navigateToText("3", 1);
		// should throw exception
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);
	}
	
	@Test(expectedExceptions = BBNotifyException.class,enabled = false)
	public void pageNumButton_pageInSameWord_multipleEntries_firstNoPage_issue6025_issue5908() {
		BBTestRunner test = new BBTestRunner("", 
				"<p>more than you bargined for</p>"
				+ "<p>the Meaning of Division..........33</p>");
		openTocTools(test);
		
		test.textViewTools.selectFromTo("more", "the");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		
		test.textViewTools.navigateToText("3", 1);
		// should throw exception
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);
	}

	@Test(expectedExceptions = BBNotifyException.class,enabled = false)
	public void pageNumButton_pageInPagenumElement() {
		BBTestRunner test = new BBTestRunner("", "<p>stuff</p><pagenum>3</pagenum>");
		openTocTools(test);

		test.textViewTools.navigateToText("3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);
	}
	
	@Test(enabled = false)
	public void pageNumButton_seperateFromTitle_issue6251() {
		BBTestRunner test = new BBTestRunner("", "<p>stuff</p><p>3</p>");
		openTocTools(test);
		
		test.clickCheckboxWithId(TOCBuilderBBX.SWTBOT_PAGE_PREFIX_CHECK, false);
		
		test.textViewTools.navigateToText("stuff");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.hasText("stuff")
						.isBlockWithStyle("T1-3")
				).nextChildIs(childAssert -> childAssert
						.hasText("3")
						.isBlockWithStyle("Body Text")
				).noNextChild();
		
		test.textViewTools.navigateToText("3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);
		
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasStyle("TOC Page")
								.hasText("3")
						)
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void pageNumButton_unapply_issue6160() {
		BBTestRunner test = new BBTestRunner("", "<p><strong>stuff 3</strong></p>");
		openTocTools(test);
		
		test.textViewTools.navigateToText("stuff");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		
		test.textViewTools.selectFromTo("3", "3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);
		
		test.assertRootSectionFirst_NoBrlCopy()
				.isBlockWithStyle("T1-3")
				.childCount(1)
				.child(0)
				.isInlineEmphasis(EmphasisType.BOLD)
				.nextChildIsText("stuff")
				.nextChildIs(page -> page
						.onlyChildIsText("3")
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void pageNumButton_unapply_reapply_unapply_issue6160() {
		BBTestRunner test = new BBTestRunner("", "<p><strong>stuff 3</strong></p>");
		openTocTools(test);
		
		test.textViewTools.navigateToText("stuff");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		
		test.textViewTools.selectFromTo("3", "3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);
		
		test.textViewTools.selectFromTo("3", "3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);
		
		test.textViewTools.selectFromTo("3", "3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);
		
		test.textViewTools.selectFromTo("3", "3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);
		
		test.textViewTools.selectFromTo("3", "3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);
		
		test.assertRootSectionFirst_NoBrlCopy()
				.isBlockWithStyle("T1-3")
				.childCount(1)
				.child(0)
				.isInlineEmphasis(EmphasisType.BOLD)
				.nextChildIsText("stuff")
				.nextChildIs(page -> page
						.onlyChildIsText("3")
				).noNextChild();
	}

	@Test(enabled = false)
	public void applyTOC_pageInPagenumElement() {
		BBTestRunner test = new BBTestRunner("", "<p>stuff</p><pagenum>3</pagenum>");
		openTocTools(test);

		test.textViewTools.navigateToText("3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		//should do nothing
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("Body Text")
						.nextChildIsText("stuff")
				).nextChildIs(childAssert -> childAssert
						.isBlock(BBX.BLOCK.PAGE_NUM)
						.hasText("3")
				).noNextChild();
	}

	@Test(enabled = false)
	public void applyTOC_pagePrefix_text() {
		BBTestRunner test = new BBTestRunner("", "<p>Stuff R1</p><p>More things</p>");
		openTocTools(test);

		test.bot.textWithId(TOCBuilderBBX.SWTBOT_PAGE_PREFIX_TEXT).setText("R");
		doPendingSWTWork();

		//TODO: toc selection?
//		test.bot.button("Next").click();
//		assertEquals(test.getTextViewSelectionStripped(), "[Body Text]Stuff R1[/Body Text]");
//		int start = test.textViewWidget.getSelectionRange().x;
		test.textViewTools.navigateToText("1");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasStyle("TOC Page")
								.hasText("R1")
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("Body Text")
						.hasText("More things")
				).noNextChild();
	}

	@Test(enabled = false)
	public void applyTOC_pagePrefix_element() {
		BBTestRunner test = new BBTestRunner("", "<p>Stuff <strong>R1</strong></p><p>More things</p>");
		openTocTools(test);

		test.bot.textWithId(TOCBuilderBBX.SWTBOT_PAGE_PREFIX_TEXT).setText("R");
		doPendingSWTWork();

		//TODO: toc selection?
//		test.bot.button("Next").click();
//		assertEquals(test.getTextViewSelectionStripped(), "[Body Text]Stuff [B]R1[/B][/Body Text]");
//		int start = test.textViewWidget.getSelectionRange().x;
		test.textViewTools.navigateToText("R1");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasStyle("TOC Page")
								.nextChildIs(childAssert3 -> childAssert3
										.isInlineEmphasis(EmphasisType.BOLD)
										.hasText("R1")
								).noNextChild()
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("Body Text")
						.hasText("More things")
				).noNextChild();
	}

	@Test(enabled = false)
	public void applyTOC_pagePrefix_NoPrefix_rt3745() {
		BBTestRunner test = new BBTestRunner("", "<p>Stuff <strong>1</strong></p><p>More things</p>");
		openTocTools(test);

		test.bot.textWithId(TOCBuilderBBX.SWTBOT_PAGE_PREFIX_TEXT).setText("R");
		doPendingSWTWork();

		//TODO: toc selection?
//		tocTest.bot.button("Next").click();
//		assertEquals(tocTest.getTextViewSelectionStripped(), "[Body Text]Stuff [B]1[/B][/Body Text]");
//		int start = tocTest.textViewWidget.getSelectionRange().x;
		test.textViewTools.navigateToText("1");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasStyle("TOC Page")
								.nextChildIs(childAssert3 -> childAssert3
										.isInlineEmphasis(EmphasisType.BOLD)
										.hasText("1")
								).noNextChild()
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("Body Text")
						.hasText("More things")
				).noNextChild();
	}
	
	@Test(enabled =false)
	public void applyTOC_pagePrefix_UnrelatedPrefix_rt5902() {
		BBTestRunner test = new BBTestRunner("", "<p>Ticket Rage</p>");
		openTocTools(test);
		
		test.bot.textWithId(TOCBuilderBBX.SWTBOT_PAGE_PREFIX_TEXT).setText("R");
		doPendingSWTWork();
		
		test.textViewTools.navigateToText("Rage");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Ticket Rage")
						.noNextChild()
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void applyTOC_pageNextBlock_text_rt4702() {
		BBTestRunner test = new BBTestRunner("", "<p>Stuff</p><p>1</p>");
		openTocTools(test);

		//TODO: toc selection?
//		tocTest.bot.button("Next").click();
//		assertEquals(tocTest.getTextViewSelectionStripped(), "[Body Text]Stuff [B]1[/B][/Body Text]");
//		int start = tocTest.textViewWidget.getSelectionRange().x;
		test.textViewTools.navigateToText("Stuff");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasStyle("TOC Page")
								.hasText("1")
						).noNextChild()
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void applyTOC_pageNextBlock_pagePrefix_text_rt4702() {
		BBTestRunner test = new BBTestRunner("", "<p>Stuff</p><p>R1</p>");
		openTocTools(test);

		test.bot.textWithId(TOCBuilderBBX.SWTBOT_PAGE_PREFIX_TEXT).setText("R");
		doPendingSWTWork();

		//TODO: toc selection?
//		tocTest.bot.button("Next").click();
//		assertEquals(tocTest.getTextViewSelectionStripped(), "[Body Text]Stuff [B]1[/B][/Body Text]");
//		int start = tocTest.textViewWidget.getSelectionRange().x;
		test.textViewTools.navigateToText("Stuff");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasStyle("TOC Page")
								.hasText("R1")
						).noNextChild()
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void applyTOC_pageNextBlock_unrelated_rt4702() {
		BBTestRunner test = new BBTestRunner("", "<p>Stuff</p><p>1 two</p>");
		openTocTools(test);

		//TODO: toc selection?
//		tocTest.bot.button("Next").click();
//		assertEquals(tocTest.getTextViewSelectionStripped(), "[Body Text]Stuff [B]1[/B][/Body Text]");
//		int start = tocTest.textViewWidget.getSelectionRange().x;
		test.textViewTools.navigateToText("Stuff");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff")
						.noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("Body Text")
						.nextChildIsText("1 two")
						.noNextChild()
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void applyTOC_pageNextBlock_span_bothSelected_rt4702() {
		BBTestRunner test = new BBTestRunner("", "<list><li><span>Stuff</span></li><li><span>1</span></li></list>");
		openTocTools(test);

		//TODO: toc selection?
//		tocTest.bot.button("Next").click();
//		assertEquals(tocTest.getTextViewSelectionStripped(), "[Body Text]Stuff [B]1[/B][/Body Text]");
//		int start = tocTest.textViewWidget.getSelectionRange().x;
		test.textViewTools.navigateToText("Stuff");
		test.textViewTools.selectRight(7);
		assertEquals(test.textViewBot.getSelection(), "Stuff\n1");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.childCount(1)
				.child(0)
				.isContainerListType(BBX.ListType.NORMAL)
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIs(childAssert2 -> childAssert2
								.isSpan(BBX.SPAN.OTHER)
								.nextChildIsText("Stuff")
								.noNextChild()
						).nextChildIs(childAssert2 -> childAssert2
								.isSpan(BBX.SPAN.OTHER)
								.hasAttributeUTD(TOCAttributes.TYPE.origName, "page")
								.nextChildIsText("1")
								.noNextChild()
						).noNextChild()
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void applyTOC_pageNextBlock_none_rt4702() {
		BBTestRunner test = new BBTestRunner("", "<p>Stuff</p>");
		openTocTools(test);

		//TODO: toc selection?
//		tocTest.bot.button("Next").click();
//		assertEquals(tocTest.getTextViewSelectionStripped(), "[Body Text]Stuff [B]1[/B][/Body Text]");
//		int start = tocTest.textViewWidget.getSelectionRange().x;
		test.textViewTools.navigateToText("Stuff");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff")
						.noNextChild()
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void applyTOC_unPageify_issue6160() {
		BBTestRunner test = new BBTestRunner("", "<p>test 3</p>");
		openTocTools(test);
		
		test.textViewTools.navigateToText("test");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSectionFirst_NoBrlCopy()
				.isBlockWithStyle("T1-3")
				.childCount(2)
				.nextChildIsText("test")
				.nextChild()
				.isSpan(BBX.SPAN.OTHER)
				.hasAttributeUTD(TOCAttributes.TYPE.origName, "page")
				.onlyChildIsText("3");
		
		
		test.textViewTools.navigateToText("3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);
		
		test.assertRootSectionFirst_NoBrlCopy()
				.isBlockWithStyle("T1-3")
				.nextChildIsText("test")
				.nextChildIs(span -> span
						.hasNoAttribute(TOCAttributes.TYPE.origName)
						.onlyChildIsText("3")
				).noNextChild();
	}
	
	@Test(enabled =false)
	public void applyTOC_unPageify_cursorAfter_issue6160() {
		BBTestRunner test = new BBTestRunner("", "<p>test 3</p>");
		openTocTools(test);
		
		test.textViewTools.navigateToText("test");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSectionFirst_NoBrlCopy()
				.isBlockWithStyle("T1-3")
				.childCount(2)
				.nextChildIsText("test")
				.nextChild()
				.isSpan(BBX.SPAN.OTHER)
				.hasAttributeUTD(TOCAttributes.TYPE.origName, "page")
				.onlyChildIsText("3");
		
		
		test.textViewTools.navigateToText("3");
		// go to end of character so cursor gets set to text node
		test.textViewTools.pressShortcut(Keystrokes.RIGHT);
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);
		
		test.assertRootSectionFirst_NoBrlCopy()
				.isBlockWithStyle("T1-3")
				.nextChildIsText("test")
				.nextChildIs(span -> span
						.hasNoAttribute(TOCAttributes.TYPE.origName)
						.onlyChildIsText("3")
				).noNextChild();
	}
	
	@Test(enabled =false)
	public void applyTOC_unPageify_selection_issue6160() {
		BBTestRunner test = new BBTestRunner("", "<p>test 3</p>");
		openTocTools(test);
		
		test.textViewTools.navigateToText("test");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSectionFirst_NoBrlCopy()
				.isBlockWithStyle("T1-3")
				.childCount(2)
				.nextChildIsText("test")
				.nextChild()
				.isSpan(BBX.SPAN.OTHER)
				.hasAttributeUTD(TOCAttributes.TYPE.origName, "page")
				.onlyChildIsText("3");
		
		test.textViewTools.selectFromTo("3", "3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_PAGE_NUM_BUTTON);
		
		test.assertRootSectionFirst_NoBrlCopy()
				.isBlockWithStyle("T1-3")
				.nextChildIsText("test")
				.nextChildIs(span -> span
						.hasNoAttribute(TOCAttributes.TYPE.origName)
						.onlyChildIsText("3")
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void applyTOC_pageInNextBlock_currentBlockEmphasis_rt5806() {
		BBTestRunner test = new BBTestRunner("", 
				"<p>stupid<em>x</em></p>" +
				"<p>57</p>"
		);
		openTocTools(test);
		
		test.textViewTools.selectFromTo("stupid", "57");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(p -> p
						.isBlockWithStyle("T1-3")
						.nextChildIsText("stupid")
						.nextChildIs(page -> page
								.isSpan(BBX.SPAN.OTHER)
								.hasAttributeUTD(TOCAttributes.TYPE.origName, "page")
								.nextChildIs(em -> em
										.isInlineEmphasis(EmphasisType.ITALICS)
										.hasText("x")
								).noNextChild()
						).noNextChild()
				).nextChildIs(p -> p
						.isBlockWithStyle("T1-3")
						.onlyChildIs()
						.isSpan(BBX.SPAN.OTHER)
						.hasAttributeUTD(TOCAttributes.TYPE.origName, "page")
						.hasText("57")
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void applyTOC_pageInNextBlock_cursorInNextBlock_rt5806() {
		BBTestRunner test = new BBTestRunner("", 
				"<p>57</p>"
		);
		openTocTools(test);
		
		test.textViewTools.navigateToText("57");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		
		test.assertRootSectionFirst_NoBrlCopy()
				.isBlockWithStyle("T1-3")
				.onlyChildIs()
				.isSpan(BBX.SPAN.OTHER)
				.hasAttributeUTD(TOCAttributes.TYPE.origName, "page")
				.hasText("57");
	}

	@Test(enabled =false)
	public void applyTOC_overrideLevel_nestedList_rt4701() {
		BBTestRunner test = new BBTestRunner("", "<list><li><list><li>nes</li><li><list><li>nested1</li><li>after</li></list></li><li>nested2</li></list></li><li>normal</li></list>");
		openTocTools(test);

		test.textViewTools.navigateToText("nested1");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.childCount(1)
				.child(0)
				.isContainerListType(BBX.ListType.NORMAL)
				.nextChildIs(childAssert -> childAssert
						.hasText("nes")
				).nextChildIs(childAssert -> childAssert
						.hasText("nested1")
						.isBlockWithStyle("T1-3")
				).nextChildIs(childAssert -> childAssert
						.hasText("after")
				).nextChildIs(childAssert -> childAssert
						.hasText("nested2")
				).nextChildIs(childAssert -> childAssert
						.hasText("normal")
				).noNextChild();
	}

	/*
	Port to TOCBuilderBBX and modules... eventually

	@Test(groups = TestGroups.BROKEN_TESTS) //depends on selection and more complicated TOCBuilderBookTest
	public void headerWithNestedPage() throws IOException {
		TOCViewTestRunner tocTest = new TOCViewTestRunner("", "<li><p>A header with page</p><p>14</p></li><li>unrelated</li>");

		TOCBuilderBookTest.tocGauntlet_next(tocTest,
				"Next", "[L1-3][Span]A header with page[/Span][Span]14[/Span][/L1-3]",
				"Centered Heading", "[TOC Centered Heading][Span]A header with page[/Span][TOC Page]14[/TOC Page][/TOC Centered Heading]");

		new BRFTestRunner().compareGeneratedXMLtoBraille(tocTest.getDoc(),
				"\n"
				+ "              ,a h1d} ) page \"\"\"\"\"\"\" #ad\n"
				+ "\n"
				+ "unrelat$",
				BRFTestRunner.OPTS_STRIP_ENDING_PAGE);

		assertEquals(stripNewlines(tocTest.textViewBot.getSelection()), "[L1-3]unrelated[/L1-3]", "Invalid next selection after apply");
	}

	@Test(groups = TestGroups.BROKEN_TESTS) //depends on selection and more complicated TOCBuilderBookTest
	public void titleWithNestedPage() throws IOException {
		TOCViewTestRunner tocTest = new TOCViewTestRunner("", "<li><p>The Test TOC</p><p>14</p></li><li>unrelated</li>");

		TOCBuilderBookTest.tocGauntlet_next(tocTest,
				"Next", "[L1-3][Span]The Test TOC[/Span][Span]14[/Span][/L1-3]",
				"List Entry/Title", "[T1-3][...]The Test TOC[/...][TOC Page]14[/TOC Page][/T1-3]");

		new BRFTestRunner().compareGeneratedXMLtoBraille(tocTest.getDoc(),
				"\n"
				+ ",! ,te/ ,,toc \"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\" #ad\n"
				+ "\n"
				+ "unrelat$",
				BRFTestRunner.OPTS_STRIP_ENDING_PAGE);

		assertEquals(stripNewlines(tocTest.textViewBot.getSelection()), "[L1-3]unrelated[/L1-3]", "Invalid next selection after apply");
	}

	@Test(groups = TestGroups.BROKEN_TESTS) //depends on selection and more complicated TOCBuilderBookTest
	public void titleNoPage() throws IOException {
		TOCViewTestRunner tocTest = new TOCViewTestRunner("", "<li><p>The Test TOC</p></li><li>unrelated</li>");

		TOCBuilderBookTest.tocGauntlet_next(tocTest,
				"Next", "[L1-3][Span]The Test TOC[/Span][/L1-3]",
				"List Entry/Title", "[T1-3][...]The Test TOC[/...][/T1-3]");

		new BRFTestRunner().compareGeneratedXMLtoBraille(tocTest.getDoc(),
				"\n"
				+ ",! ,te/ ,,toc\n"
				+ "\n"
				+ "unrelat$",
				BRFTestRunner.OPTS_STRIP_ENDING_PAGE);

		assertEquals(stripNewlines(tocTest.textViewBot.getSelection()), "[L1-3]unrelated[/L1-3]", "Invalid next selection after apply");
	}

	@Test(groups = TestGroups.BROKEN_TESTS) //depends on selection and more complicated TOCBuilderBookTest
	public void titleWithPageNoEmphasis() throws IOException {
		TOCViewTestRunner tocTest = new TOCViewTestRunner("", "<li><p>The Test TOC 20</p></li><li>unrelated</li>");

		TOCBuilderBookTest.tocGauntlet_next(tocTest,
				"Next", "[L1-3][Span]The Test TOC 20[/Span][/L1-3]",
				"List Entry/Title", "[T1-3][...]The Test TOC[TOC Page]20[/TOC Page][/...][/T1-3]");

		new BRFTestRunner().compareGeneratedXMLtoBraille(tocTest.getDoc(),
				"\n"
				+ ",! ,te/ ,,toc \"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\" #bj\n"
				+ "\n"
				+ "unrelat$",
				BRFTestRunner.OPTS_STRIP_ENDING_PAGE);

		assertEquals(stripNewlines(tocTest.textViewBot.getSelection()), "[L1-3]unrelated[/L1-3]", "Invalid next selection after apply");
	}

	@Test(groups = TestGroups.BROKEN_TESTS) //depends on selection and more complicated TOCBuilderBookTest
	public void titleWithPageNoEmphasisNestedList() throws IOException {
		TOCViewTestRunner tocTest = new TOCViewTestRunner(
				"",
				"<li><p>The Test TOC 20<list><li><p>next item 25</p></li><li>unrelated</li></list></p></li>"
		);

		TOCBuilderBookTest.tocGauntlet_next(tocTest,
				"Next", "The Test TOC 20",
				"List Entry/Title", "[T1-3]The Test TOC[TOC Page]20[/TOC Page][/T1-3]");

		TOCBuilderBookTest.tocGauntlet_next(tocTest,
				null, "[L1-3][Span]next item 25[/Span][/L1-3]",
				"List Entry/Title", "[T1-3][...]next item[TOC Page]25[/TOC Page][/...][/T1-3]");

		new BRFTestRunner().compareGeneratedXMLtoBraille(tocTest.getDoc(),
				"\n"
				+ ",! ,te/ ,,toc \"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\" #bj\n"
				+ "\n"
				+ "next item \"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\" #be\n"
				+ "\n"
				+ "unrelat$",
				BRFTestRunner.OPTS_STRIP_ENDING_PAGE);
		assertEquals(stripNewlines(tocTest.textViewBot.getSelection()), "[L1-3]unrelated[/L1-3]", "Invalid next selection after apply");
	}
	 */
	@Test(enabled = false)
	public void applyTOC_Multiple_Entry() {
		BBTestRunner test = new BBTestRunner("", "<p>Stuff 1</p><p>Things 2</p>");
		openTocTools(test);

		test.textViewTools.navigateToText("Stuff");
		test.textViewTools.selectToEndOfLine();
		test.textViewTools.pressShortcut(SWT.SHIFT, SWT.ARROW_DOWN, '\0');

		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("1")
								.hasStyle("TOC Page")
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Things")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("2")
								.hasStyle("TOC Page")
						).noNextChild()
				).noNextChild();
	}

	@Test(enabled = false)
	public void applyTOC_Multiple_Heading() {
		BBTestRunner test = new BBTestRunner("", "<p>Stuff 1</p><p>Things 2</p>");
		openTocTools(test);

		test.textViewTools.navigateToText("Stuff");
		test.textViewTools.selectToEndOfLine();
		test.textViewTools.pressShortcut(SWT.SHIFT, SWT.ARROW_DOWN, '\0');

		test.clickComboIndexWithId(TOCBuilderBBX.SWTBOT_HEADING_COMBO, 1)  ;
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("TOC Centered Heading")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("1")
								.hasStyle("TOC Page")
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("TOC Centered Heading")
						.nextChildIsText("Things")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("2")
								.hasStyle("TOC Page")
						).noNextChild()
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void applyTOC_MultipleHeading_PrintPageNumInBreadcrumbsSelection_rt5368() {
		BBTestRunner test = new BBTestRunner("", 
				"<list>"
				+ "<li>Stuff 1</li>"
				+ "<pagenum page='front'>ii</pagenum>"
				+ "<li>Stuff 2</li>"
				+ "</list>"
				);
		openTocTools(test);

		test.textViewTools.navigateToText("Stuff");
		test.selectBreadcrumbsAncestor(1, BBX.CONTAINER::isA);

		test.clickComboIndexWithId(TOCBuilderBBX.SWTBOT_HEADING_COMBO, 1)  ;
		test.assertRootSectionFirst_NoBrlCopy()
				.isContainerListType(BBX.ListType.NORMAL)
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("TOC Centered Heading")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("1")
								.hasStyle("TOC Page")
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.hasText("ii")
						.isBlock(BBX.BLOCK.PAGE_NUM)
				).nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("TOC Centered Heading")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("2")
								.hasStyle("TOC Page")
						).noNextChild()
				).noNextChild();
	}
	
	@Test(enabled =false)
	public void applyTOC_MultipleHeading_PrintPageNumInBreadcrumbsSelection_weirdWrap_rt5368() {
		BBXDocFactory docFactory = new BBXDocFactory()
				.append(BBX.CONTAINER.LIST.create(BBX.ListType.NORMAL), list -> {
					BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.set(list.root, 1);
					
					list.append(BBX.BLOCK.LIST_ITEM.create(), item -> {
						BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL.set(item.root, 1);
						item.root.appendChild("Test 1");
					}).append(BBX.BLOCK.LIST_ITEM.create(), item -> {
						BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL.set(item.root, 1);
						
						Element pageNum = BBX.SPAN.PAGE_NUM.create();
						pageNum.appendChild("ii");
						item.root.appendChild(pageNum);
					}).append(BBX.BLOCK.LIST_ITEM.create(), item -> {
						BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL.set(item.root, 1);
						item.root.appendChild("Test 2");
					});
				});
		
		BBTestRunner test = new BBTestRunner(docFactory);
		openTocTools(test);

		test.textViewTools.navigateToText("Test");
		test.selectBreadcrumbsAncestor(1, BBX.CONTAINER::isA);

		test.clickComboIndexWithId(TOCBuilderBBX.SWTBOT_HEADING_COMBO, 1)  ;
		test.assertRootSectionFirst_NoBrlCopy()
				.isContainerListType(BBX.ListType.NORMAL)
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("TOC Centered Heading")
						.nextChildIsText("Test")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("1")
								.hasStyle("TOC Page")
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						//should it be ignored?
						.isBlockWithStyle("TOC Centered Heading")
						.childCount(1)
						.child(0)
						.hasText("ii")
						.isSpan(BBX.SPAN.PAGE_NUM)
				).nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("TOC Centered Heading")
						.nextChildIsText("Test")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("2")
								.hasStyle("TOC Page")
						).noNextChild()
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void applyTOC_MultipleHeading_PrintPageNumInBreadcrumbsSelection_weirdWrapNoPage_rt5368() {
		BBXDocFactory docFactory = new BBXDocFactory()
				.append(BBX.CONTAINER.LIST.create(BBX.ListType.NORMAL), list -> {
					BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.set(list.root, 1);
					
					list.append(BBX.BLOCK.LIST_ITEM.create(), item -> {
						BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL.set(item.root, 1);
						item.root.appendChild("Test");
					}).append(BBX.BLOCK.LIST_ITEM.create(), item -> {
						BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL.set(item.root, 1);
						
						Element pageNum = BBX.SPAN.PAGE_NUM.create();
						pageNum.appendChild("ii");
						item.root.appendChild(pageNum);
					}).append(BBX.BLOCK.LIST_ITEM.create(), item -> {
						BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL.set(item.root, 1);
						item.root.appendChild("Test 2");
					});
				});
		
		BBTestRunner test = new BBTestRunner(docFactory);
		openTocTools(test);

		test.textViewTools.navigateToText("Test");
		test.selectBreadcrumbsAncestor(1, BBX.CONTAINER::isA);

		test.clickComboIndexWithId(TOCBuilderBBX.SWTBOT_HEADING_COMBO, 1)  ;
		test.assertRootSectionFirst_NoBrlCopy()
				.isContainerListType(BBX.ListType.NORMAL)
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("TOC Centered Heading")
						.nextChildIsText("Test")
						.noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("L3-5")
						.childCount(1)
						.child(0)
						.hasText("ii")
						.isSpan(BBX.SPAN.PAGE_NUM)
				).nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("TOC Centered Heading")
						.nextChildIsText("Test")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("2")
								.hasStyle("TOC Page")
						).noNextChild()
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void applyTOC_MultipleEntry_PrintPageNumInBreadcrumbsSelection_rt5368() {
		BBTestRunner test = new BBTestRunner("", 
				"<list>"
				+ "<li>Stuff 1</li>"
				+ "<pagenum page='front'>ii</pagenum>"
				+ "<li>Stuff 2</li>"
				+ "</list>"
				);
		openTocTools(test);

		test.textViewTools.navigateToText("Stuff");
		test.selectBreadcrumbsAncestor(1, BBX.CONTAINER::isA);

		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSectionFirst_NoBrlCopy()
				.isContainerListType(BBX.ListType.NORMAL)
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("1")
								.hasStyle("TOC Page")
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.hasText("ii")
						.isBlock(BBX.BLOCK.PAGE_NUM)
				).nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("2")
								.hasStyle("TOC Page")
						).noNextChild()
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void applyTOC_MultipleEntry_PrintPageNumInBreadcrumbsSelection_listItemChild_rt5380() {
		BBTestRunner test = new BBTestRunner("", 
				"<list>"
				+ "<li>Stuff 1</li>"
				+ "<li><pagenum page='front'>ii</pagenum></li>"
				+ "<li>Stuff 2</li>"
				+ "</list>"
				);
		openTocTools(test);

		test.textViewTools.navigateToText("Stuff");
		test.selectBreadcrumbsAncestor(1, BBX.CONTAINER::isA);

		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSectionFirst_NoBrlCopy()
				.isContainerListType(BBX.ListType.NORMAL)
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("1")
								.hasStyle("TOC Page")
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlock(BBX.BLOCK.LIST_ITEM)
						.childCount(1)
						.child(0)
						.isSpan(BBX.SPAN.PAGE_NUM)
						.hasText("ii")
				).nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("2")
								.hasStyle("TOC Page")
						).noNextChild()
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void applyTOC_MultipleEntry_PrintPageNumInBreadcrumbsSelection_weirdWrap_rt5368() {
		BBXDocFactory docFactory = new BBXDocFactory()
				.append(BBX.CONTAINER.LIST.create(BBX.ListType.NORMAL), list -> {
					BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.set(list.root, 1);
					
					list.append(BBX.BLOCK.LIST_ITEM.create(), item -> {
						BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL.set(item.root, 1);
						item.root.appendChild("Test 1");
					}).append(BBX.BLOCK.LIST_ITEM.create(), item -> {
						BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL.set(item.root, 1);
						
						Element pageNum = BBX.SPAN.PAGE_NUM.create();
						pageNum.appendChild("ii");
						item.root.appendChild(pageNum);
					}).append(BBX.BLOCK.LIST_ITEM.create(), item -> {
						BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL.set(item.root, 1);
						item.root.appendChild("Test 2");
					});
					
				});
		
		BBTestRunner test = new BBTestRunner(docFactory);
		openTocTools(test);
		
		test.textViewTools.navigateToText("Test");
		test.selectBreadcrumbsAncestor(1, BBX.CONTAINER::isA);
		
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSectionFirst_NoBrlCopy()
				.isContainerListType(BBX.ListType.NORMAL)
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Test")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("1")
								.hasStyle("TOC Page")
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						//should it be ignored?
						.isBlockWithStyle("L3-5")
						.childCount(1)
						.child(0)
						.hasText("ii")
						.isSpan(BBX.SPAN.PAGE_NUM)
				).nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Test")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("2")
								.hasStyle("TOC Page")
						).noNextChild()
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void applyTOC_MultipleEntry_PrintPageNumInBreadcrumbsSelection_weirdWrapNoTocPage_rt5368() {
		BBXDocFactory docFactory = new BBXDocFactory()
				.append(BBX.CONTAINER.LIST.create(BBX.ListType.NORMAL), list -> {
					BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.set(list.root, 1);
					
					list.append(BBX.BLOCK.LIST_ITEM.create(), item -> {
						BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL.set(item.root, 1);
						item.root.appendChild("Test 1");
					}).append(BBX.BLOCK.LIST_ITEM.create(), item -> {
						BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL.set(item.root, 1);
						
						Element pageNum = BBX.SPAN.PAGE_NUM.create();
						pageNum.appendChild("ii");
						item.root.appendChild(pageNum);
					}).append(BBX.BLOCK.LIST_ITEM.create(), item -> {
						BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL.set(item.root, 1);
						item.root.appendChild("Test 2");
					});
					
				});
		
		BBTestRunner test = new BBTestRunner(docFactory);
		openTocTools(test);
		
		test.textViewTools.navigateToText("Test");
		test.selectBreadcrumbsAncestor(1, BBX.CONTAINER::isA);

		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSectionFirst_NoBrlCopy()
				.isContainerListType(BBX.ListType.NORMAL)
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Test")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("1")
								.hasStyle("TOC Page")
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						//should it be ignored?
						.isBlockWithStyle("L3-5")
						.childCount(1)
						.child(0)
						.hasText("ii")
						.isSpan(BBX.SPAN.PAGE_NUM)
				).nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Test")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("2")
								.hasStyle("TOC Page")
						).noNextChild()
				).noNextChild();
	}
	

	@Test(enabled = false)
	public void applyTOC_Heading_indentedText_rt4721() {
		BBTestRunner test = new BBTestRunner("", "<level1><h1>Something</h1><list><li><p>Stuff 1</p></li><li><p>Things 2</p></li></list></level1>");
		openTocTools(test);

		test.clickCheckboxWithId(TOCBuilderBBX.SWTBOT_OVERRIDE_LEVEL, false);

		test.textViewTools.navigateToText("Stuff");
		test.openMenuItem(TopMenu.STYLES, "Lists", "List 3 Levels", "L5-7");

		test.selectBreadcrumbsAncestor(2, BBX.SECTION::assertIsA);
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertInnerSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.hasText("Something")
						.hasStyle("TOC Centered Heading")
				).nextChildIs(childAssert -> childAssert
						.isContainerListType(BBX.ListType.NORMAL)
						.nextChildIs(childAssert2 -> childAssert2
								.hasStyle("T5-7")
								.nextChildIsText("Stuff")
								.nextChildIs(childAssert3 -> childAssert3
										.hasText("1")
										.hasStyle("TOC Page")
								).noNextChild()
						).nextChildIs(childAssert2 -> childAssert2
								.hasStyle("T1-7")
								.nextChildIsText("Things")
								.nextChildIs(childAssert3 -> childAssert3
										.hasText("2")
										.hasStyle("TOC Page")
								).noNextChild()
						).noNextChild()
				).noNextChild();
	}

	@Test(enabled = false)
	public void applyTOC_Again_rt4173() {
		BBTestRunner test = new BBTestRunner("", "<p>Stuff 1</p><p>Things 2</p>");
		openTocTools(test);

		test.textViewTools.navigateToText("Stuff");
		test.selectBreadcrumbsAncestor(0, BBX.BLOCK::assertIsA);

		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("1")
								.hasStyle("TOC Page")
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("Body Text")
						.nextChildIsText("Things 2")
						.noNextChild()
				).noNextChild();

		//TODO BUG: Cannot select when cursor didn't move (cursor is at [T1-3)
		test.selectBreadcrumbsAncestor(0, BBX.BLOCK::assertIsA);
		//Should not get an exception
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("T1-3")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("1")
								.hasStyle("TOC Page")
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockWithStyle("Body Text")
						.nextChildIsText("Things 2")
						.noNextChild()
				).noNextChild();
	}

	@Test(enabled = false)
	public void applyTOC_Heading_autoTitle() {
		BBTestRunner test = new BBTestRunner("", "<level1><h1>Stuff 1</h1><h2>More 2</h2><p>Things 3</p></level1>");
		openTocTools(test);

		test.bot.checkBoxWithId(TOCBuilderBBX.SWTBOT_OVERRIDE_LEVEL).deselect();
		doPendingSWTWork();

		test.textViewTools.navigateToText("Stuff 1");
		test.selectBreadcrumbsAncestor(1, BBX.SECTION::assertIsA);

		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.assertInnerSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.hasStyle("TOC Centered Heading")
						.nextChildIsText("Stuff")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("1")
								.hasStyle("TOC Page")
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.hasStyle("TOC Cell 5 Heading")
						.nextChildIsText("More")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("2")
								.hasStyle("TOC Page")
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.hasStyle("T1-3")
						.nextChildIsText("Things")
						.nextChildIs(childAssert2 -> childAssert2
								.hasText("3")
								.hasStyle("TOC Page")
						).noNextChild()
				).noNextChild();
	}

	//What was this test supposed to do?
	@Test(enabled = false)
	public void applyTOC_thenDelete_1_rt4719() {
		BBTestRunner test = new BBTestRunner("", "<list><li><em>Stuff</em> <img src='test.jpg'/> <img src='test.jpg'/> <img src='test.jpg'/></li><li><em>Things</em> 2</li></list>");
		openTocTools(test);

//		test.textViewTools.navigateToText("Stuff");
//		test.styleViewTools.selectRight(2);
//		test.textViewTools.pressShortcut(SWT.SHIFT, SWT.ARROW_DOWN, '\0');
//		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.textViewTools.navigateToTextRelative("Stuff");
		test.textViewTools.navigateToEndOfLine();
		test.textViewTools.pressKey(SWT.ARROW_RIGHT, 1);
		test.textViewTools.pressKey(SWT.DEL, 2);
//		test.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
	}
	
	@Test(enabled = false)
	public void applyTOC_deleteTOC_emphasisAndText_issue6438() {
		BBTestRunner test = new BBTestRunner("", "<list type='pl'>"
				+ "<li>before</li>"
				+ "<li>▪ <strong>Building Academic Vocabulary</strong>xlvi</li>"
				+ "</list>"
		);
		openTocTools(test);
		
		test.textViewTools.navigateToText("Building");
		test.selectBreadcrumbsAncestor(1, BBX.CONTAINER.LIST::assertIsA);
		
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		
		test.assertRootSectionFirst_NoBrlCopy()
				.isContainerListType(BBX.ListType.NORMAL)
				.nextChildIs(item -> item
						.isBlock(BBX.BLOCK.MARGIN)
						.onlyChildIsText("before")
				).nextChildIs(item -> item
						.isBlock(BBX.BLOCK.MARGIN)
						.nextChildIsText("▪ ")
						.nextChildIs(strong -> strong
								.isInlineEmphasis(EmphasisType.BOLD)
								.onlyChildIsText("Building Academic Vocabulary")
						).nextChildIs(page -> page
							.nextChildIsText("xlvi")
						).noNextChild()
				).noNextChild();

		test.textViewTools.selectFromTo("▪", "xlvi");
		test.textViewTools.pressKey(SWT.DEL, 1);
		
		test.assertRootSectionFirst_NoBrlCopy()
				.isContainerListType(BBX.ListType.NORMAL)
				.nextChildIs(item -> item
						.isBlock(BBX.BLOCK.MARGIN)
						.onlyChildIsText("before")
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void applyTOC_deleteTOC_emphasis_issue6438() {
		BBTestRunner test = new BBTestRunner("", "<list type='pl'>"
				+ "<li><strong>str</strong>12</li>"
				+ "<li>next</li>"
				+ "</list>"
		);
		openTocTools(test);
		
		test.textViewTools.navigateToText("str");
		test.selectBreadcrumbsAncestor(2, BBX.CONTAINER.LIST::assertIsA);
		
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		
		test.assertRootSectionFirst_NoBrlCopy()
				.isContainerListType(BBX.ListType.NORMAL)
				.nextChildIs(item -> item
						.isBlock(BBX.BLOCK.MARGIN)
						.nextChildIs(strong -> strong
								.isInlineEmphasis(EmphasisType.BOLD)
								.onlyChildIsText("str")
						).nextChildIs(page -> page
							.nextChildIsText("12")
						).noNextChild()
				).nextChildIs(item -> item
						.isBlock(BBX.BLOCK.MARGIN)
						.onlyChildIsText("next")
				).noNextChild();
		
		test.textViewTools.selectFromTo("str", "12");
		test.textViewTools.pressKey(SWT.DEL, 1);
		
		test.assertRootSectionFirst_NoBrlCopy()
				.isContainerListType(BBX.ListType.NORMAL)
				.nextChildIs(item -> item
						.isBlock(BBX.BLOCK.MARGIN)
						.onlyChildIsText("next")
				).noNextChild();
	}	

	@Test(enabled = false)
	public void applyTOC_separatelyEmphasis_rt6134() {
		BBTestRunner test = new BBTestRunner(
				"", 
				"<p><strong>title1</strong> <strong>54</strong></p>"
		);
		openTocTools(test);
		
		test.assertRootSectionFirst_NoBrlCopy()
				.isBlockWithStyle("Body Text")
				.nextChildIs(strong -> strong
						.isInlineEmphasis(EmphasisType.BOLD)
						.hasText("title1")
				).nextChildIsText(" ")
				.nextChildIs(strong -> strong
						.isInlineEmphasis(EmphasisType.BOLD)
						.hasText("54")
				).noNextChild();
		
		test.textViewTools.navigateToText("title1");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		
		test.assertRootSectionFirst_NoBrlCopy()
				.isBlockWithStyle("T1-3")
				.nextChildIs(strong -> strong
						.isInlineEmphasis(EmphasisType.BOLD)
						.hasText("title1")
				).nextChildIs(page -> page
						.hasStyle("TOC Page")
						.nextChildIs(strong -> strong
							.isInlineEmphasis(EmphasisType.BOLD)
							.hasText("54")
						).noNextChild()
				).noNextChild();
		
		test.textViewTools.selectFromTo("54", "54");
		
		test.openMenuItem(TopMenu.EMPHASIS, "Bold");
		
		test.assertRootSectionFirst_NoBrlCopy()
				.isBlockWithStyle("T1-3")
				.nextChildIs(strong -> strong
						.isInlineEmphasis(EmphasisType.BOLD)
						.onlyChildIsText("title1")
				).nextChildIs(page -> page
						.hasStyle("TOC Page")
						.onlyChildIsText("54")
				).noNextChild();
	}

	@Test(enabled = false) //TODO: An element's brf is disappearing
	public void autoRunover_list() {
		BBTestRunner test = new BBTestRunner("", "<level1>"
				+ "<list>"
				+ "<li>Stuff 1</li>"
				+ "<li>Things 2</li>"
				+ "<li>normal 3</li>"
				+ "</list><list>"
				+ "<li>normal 4</li>"
				+ "</list>"
				+ "</level1>");
		openTocTools(test);

		try {
			//Mark first entry as a T style
			test.textViewTools.navigateToText("Stuff 1");
			test.selectBreadcrumbsAncestor(0, BBX.BLOCK::isA);
			test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
			test.assertRootSectionFirst_NoBrlCopy()
					.nextChildIs(childAssert -> childAssert
							.isContainerListType(BBX.ListType.NORMAL)
							.nextChildIs(childAssert2 -> childAssert2
									.isBlockWithStyle("T1-3")
									.nextChildIsText("Stuff")
									.nextChildIs(childAssert3 -> childAssert3
											.hasStyle("TOC Page")
											.hasText("1")
									).noNextChild()
							).nextChildIs(childAssert2 -> childAssert2
									.isBlockWithStyle("L1-3")
									.nextChildIsText("Things 2")
									.noNextChild()
							).nextChildIs(childAssert2 -> childAssert2
									.isBlockWithStyle("L1-3")
									.nextChildIsText("normal 3")
									.noNextChild()
							).noNextChild()
					).nextChildIs(childAssert -> childAssert
							.isContainerListType(BBX.ListType.NORMAL)
							.nextChildIs(childAssert2 -> childAssert2
									.isBlockWithStyle("L1-3")
									.nextChildIsText("normal 4")
									.noNextChild()
							).noNextChild()
					).noNextChild();

			//Make sure runover gets updated when marking second entry
			test.textViewTools.navigateToText("Things 2");
			test.selectBreadcrumbsAncestor(0, BBX.BLOCK::isA);
			test.setTextWithId(TOCBuilderBBX.SWTBOT_INDENT_TEXT, "3");
			test.setTextWithId(TOCBuilderBBX.SWTBOT_RUNOVER_TEXT, "5");
			//TODO: Fix bug where the second list entry disappears for some reason
			test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
			test.assertRootSectionFirst_NoBrlCopy()
					.nextChildIs(childAssert -> childAssert
							.isContainerListType(BBX.ListType.NORMAL)
							.nextChildIs(childAssert2 -> childAssert2
									.isBlockWithStyle("T1-5")
									.nextChildIsText("Stuff")
									.nextChildIs(childAssert3 -> childAssert3
											.hasStyle("TOC Page")
											.hasText("1")
									).noNextChild()
							).nextChildIs(childAssert2 -> childAssert2
									.isBlockWithStyle("T3-5")
									.nextChildIsText("Things")
									.nextChildIs(childAssert3 -> childAssert3
											.hasStyle("TOC Page")
											.hasText("2")
									).noNextChild()
							).nextChildIs(childAssert2 -> childAssert2
									.isBlockWithStyle("L1-5")
									.nextChildIsText("normal 3")
									.noNextChild()
							).noNextChild()
					).nextChildIs(childAssert -> childAssert
							.isContainerListType(BBX.ListType.NORMAL)
							.nextChildIs(childAssert2 -> childAssert2
									.isBlockWithStyle("L1-3")
									.nextChildIsText("normal 4")
									.noNextChild()
							).noNextChild()
					).noNextChild();

			//Make sure another list isn't affected
			test.textViewTools.navigateToText("normal 4");
			test.textViewTools.selectRight(1);
			test.setTextWithId(TOCBuilderBBX.SWTBOT_INDENT_TEXT, "5");
			test.setTextWithId(TOCBuilderBBX.SWTBOT_RUNOVER_TEXT, "7");
			test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
			test.assertRootSectionFirst_NoBrlCopy()
					.nextChildIs(childAssert -> childAssert
							.isContainerListType(BBX.ListType.NORMAL)
							.nextChildIs(childAssert2 -> childAssert2
									.isBlockWithStyle("T1-5")
									.nextChildIsText("Stuff")
									.nextChildIs(childAssert3 -> childAssert3
											.hasStyle("TOC Page")
											.hasText("1")
									).noNextChild()
							).nextChildIs(childAssert2 -> childAssert2
									.isBlockWithStyle("T3-5")
									.nextChildIsText("Things")
									.nextChildIs(childAssert3 -> childAssert3
											.hasStyle("TOC Page")
											.hasText("2")
									).noNextChild()
							).nextChildIs(childAssert2 -> childAssert2
									.isBlockWithStyle("L1-5")
									.nextChildIsText("normal 3")
									.noNextChild()
							).noNextChild()
					).nextChildIs(childAssert -> childAssert
							.isContainerListType(BBX.ListType.NORMAL)
							.nextChildIs(childAssert2 -> childAssert2
									.isBlockWithStyle("T5-7")
									.nextChildIsText("normal")
									.nextChildIs(childAssert3 -> childAssert3
											.hasStyle("TOC Page")
											.hasText("4")
									).noNextChild()
							).noNextChild()
					).noNextChild();
		} catch (Throwable e) {
			UTDHelper.getDescendantBrlFast(test.getDoc(), Node::detach);
			throw new NodeException("asdf", test.getDoc(), e);
		}
	}

	@Test(enabled = false)
	public void tocVolumeSplit() {
		BBTestRunner test = new BBTestRunner("",
				"<list>"
				+ "<li>Stuff 1</li>"
				+ "<li>Things 2</li>"
				+ "<li>Lorem 3</li>"
				+ "<li>normal 1</li>"
				+ "</list>"
				+ "<p>some text</p>"
				+ "<p>more text</p>"
				+ "<p>extra text</p>"
		);
		openTocTools(test);

		//use text view due to sections
		test.textViewTools.navigateToText("more text");
		VolumeTest.openInsertVolume(test, BBX.VolumeType.VOLUME);

		test.textViewTools.navigateToText("Stuff 1");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);

		test.setTextWithId(TOCBuilderBBX.SWTBOT_INDENT_TEXT, "3");
		test.setTextWithId(TOCBuilderBBX.SWTBOT_RUNOVER_TEXT, "5");
		//todo: annoying to have to reset cursor
		test.textViewTools.navigateToText("Things 2");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_VOLUME_SPLIT_BUTTON);
		test.textViewTools.navigateToText("Things 2");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);

		test.textViewTools.navigateToText("Lorem 3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);

		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_DISPERSE_VOLUMES_BUTTON);
		
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isContainerListType(BBX.ListType.NORMAL)
						.nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.TOC_VOLUME_SPLIT)
								.hasText(TransNoteAction.START + "Volume 1" + TransNoteAction.END)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("Stuff")
								.hasStyle("T1-5")
								.childCount(2)
						).nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.TOC_VOLUME_SPLIT)
								.hasText(TransNoteAction.START + "Volume 2" + TransNoteAction.END)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("Things")
								.hasStyle("T3-5")
								.childCount(2)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("Lorem")
								.hasStyle("T3-5")
								.childCount(2)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("normal 1")
								.hasStyle("L1-5")
								.childCount(1)
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("some text")
				).nextChildIs(childAssert -> childAssert
						.isContainer(BBX.CONTAINER.VOLUME)
						.nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.VOLUME_END)
								.hasText("END OF VOLUME 1")
						).nextChildIs(childAssert2 -> childAssert2
								.isContainer(BBX.CONTAINER.VOLUME_TOC)
								.nextChildIs(childAssert3 -> childAssert3
										.hasText("Things")
										.hasStyle("T3-5")
										.childCount(2)
								).nextChildIs(childAssert3 -> childAssert3
										.hasText("Lorem")
										.hasStyle("T3-5")
										.childCount(2)
								).noNextChild()
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("more text")
				).nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("extra text")
				).nextChildIs(childAssert -> childAssert
						.isContainer(BBX.CONTAINER.VOLUME)
						.childCount(1)
						.child(0)
						.isBlock(BBX.BLOCK.VOLUME_END)
						.hasText("THE END")
				).noNextChild();

		test.textViewTools.navigateToText("extra text");
		VolumeTest.openInsertVolume(test, BBX.VolumeType.VOLUME);

		System.out.println("========================================================================");
		//make sure there is no exception
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_DISPERSE_VOLUMES_BUTTON);

		test.textViewTools.navigateToText("Lorem");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_VOLUME_SPLIT_BUTTON);

		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_DISPERSE_VOLUMES_BUTTON);

		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isContainerListType(BBX.ListType.NORMAL)
						.nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.TOC_VOLUME_SPLIT)
								.hasText(TransNoteAction.START + "Volume 1" + TransNoteAction.END)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("Stuff")
								.hasStyle("T1-5")
								.childCount(2)
						).nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.TOC_VOLUME_SPLIT)
								.hasText(TransNoteAction.START + "Volume 2" + TransNoteAction.END)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("Things")
								.hasStyle("T3-5")
								.childCount(2)
						).nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.TOC_VOLUME_SPLIT)
								.hasText(TransNoteAction.START + "Volume 3" + TransNoteAction.END)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("Lorem")
								.hasStyle("T3-5")
								.childCount(2)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("normal 1")
								.hasStyle("L1-5")
								.childCount(1)
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("some text")
				).nextChildIs(childAssert -> childAssert
						.isContainer(BBX.CONTAINER.VOLUME)
						.nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.VOLUME_END)
								.hasText("END OF VOLUME 1")
						).nextChildIs(childAssert2 -> childAssert2
								.isContainer(BBX.CONTAINER.VOLUME_TOC)
								.nextChildIs(childAssert3 -> childAssert3
										.hasText("Things")
										.hasStyle("T3-5")
										.childCount(2)
								).noNextChild()
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("more text")
				).nextChildIs(childAssert -> childAssert
						.isContainer(BBX.CONTAINER.VOLUME)
						.nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.VOLUME_END)
								.hasText("END OF VOLUME 2")
						).nextChildIs(childAssert2 -> childAssert2
								.isContainer(BBX.CONTAINER.VOLUME_TOC)
								.nextChildIs(childAssert3 -> childAssert3
										.hasText("Lorem")
										.hasStyle("T3-5")
										.childCount(2)
								).noNextChild()
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("extra text")
				).nextChildIs(childAssert -> childAssert
						.isContainer(BBX.CONTAINER.VOLUME)
						.childCount(1)
						.child(0)
						.isBlock(BBX.BLOCK.VOLUME_END)
						.hasText("THE END")
				).noNextChild();
	}

	@Test(enabled = false)
	public void tocPageNumMissingInVolumeSplit_issue4943() {
		BBTestRunner test = new BBTestRunner("",
				"<pagenum>1</pagenum>"
				+ "<list>"
				+ "<li>Stuff 1</li>"
				+ "<li>Things 2</li>"
				+ "<pagenum>2</pagenum>"
				+ "<li>Lorem 3</li>"
				+ "<li>normal 1</li>"
				+ "</list>"
				+ "<p>some text</p>"
				+ "<p>more text</p>"
				+ "<p>extra text</p>"
		);
		openTocTools(test);

		//use text view due to sections
		test.textViewTools.navigateToText("more text");
		VolumeTest.openInsertVolume(test, BBX.VolumeType.VOLUME);

		test.textViewTools.navigateToTextRelative("Stuff 1");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.textViewTools.navigateToTextRelative("Things 2");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_VOLUME_SPLIT_BUTTON);
		test.textViewTools.navigateToTextRelative("Things 2");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.textViewTools.navigateToTextRelative("Lorem 3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);

		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_DISPERSE_VOLUMES_BUTTON);

		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isBlock(BBX.BLOCK.PAGE_NUM)
						.hasText("1")
				).nextChildIs(childAssert -> childAssert
						.isContainerListType(BBX.ListType.NORMAL)
						.nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.TOC_VOLUME_SPLIT)
								.hasText(TransNoteAction.START + "Volume 1" + TransNoteAction.END)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("Stuff")
								.hasStyle("T1-3")
								.childCount(2)
						).nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.TOC_VOLUME_SPLIT)
								.hasText(TransNoteAction.START + "Volume 2" + TransNoteAction.END)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("Things")
								.hasStyle("T1-3")
								.childCount(2)
						).nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.PAGE_NUM)
								.hasText("2")
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("Lorem")
								.hasStyle("T1-3")
								.childCount(2)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("normal 1")
								.hasStyle("L1-3")
								.childCount(1)
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("some text")
				).nextChildIs(childAssert -> childAssert
						.isContainer(BBX.CONTAINER.VOLUME)
						.nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.VOLUME_END)
								.hasText("END OF VOLUME 1")
						).nextChildIs(childAssert2 -> childAssert2
								.isContainer(BBX.CONTAINER.VOLUME_TOC)
								.nextChildIs(childAssert3 -> childAssert3
										.isBlock(BBX.BLOCK.PAGE_NUM)
										.hasText("1")
								).nextChildIs(childAssert3 -> childAssert3
										.hasText("Things")
										.hasStyle("T1-3")
										.childCount(2)
								).nextChildIs(childAssert3 -> childAssert3
										.isBlock(BBX.BLOCK.PAGE_NUM)
										.hasText("2")
								).nextChildIs(childAssert3 -> childAssert3
										.hasText("Lorem")
										.hasStyle("T1-3")
										.childCount(2)
								).noNextChild()
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("more text")
				).nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("extra text")
				).nextChildIs(childAssert -> childAssert
						.isContainer(BBX.CONTAINER.VOLUME)
						.childCount(1)
						.child(0)
						.isBlock(BBX.BLOCK.VOLUME_END)
						.hasText("THE END")
				).noNextChild();
	}

	@Test(expectedExceptions = BBNotifyException.class,enabled = false)
	public void disperse_noVolumes_issue4924() {
		BBTestRunner test = new BBTestRunner("",
				"<pagenum>1</pagenum>"
				+ "<list>"
				+ "<li>Stuff 1</li>"
				+ "<li>Things 2</li>"
				+ "<pagenum>2</pagenum>"
				+ "<li>Lorem 3</li>"
				+ "<li>normal 1</li>"
				+ "</list>"
				+ "<p>some text</p>"
				+ "<pagenum>3</pagenum>"
				+ "<p>more text</p>"
				+ "<p>extra text</p>"
		);
		openTocTools(test);

		test.textViewTools.navigateToText("Stuff 1");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.textViewTools.navigateToText("Things 2");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_VOLUME_SPLIT_BUTTON);
		test.textViewTools.navigateToText("Things 2");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.textViewTools.navigateToText("Lorem 3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_VOLUME_SPLIT_BUTTON);
		test.textViewTools.navigateToText("Lorem 3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);

		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_DISPERSE_VOLUMES_BUTTON);
	}

	@Test(enabled = false)
	public void disperse_notEnoughVolumes_issue4924() {
		BBTestRunner test = new BBTestRunner("",
				"<list>"
				+ "<li>Stuff 1</li>"
				+ "<li>Things 2</li>"
				+ "<li>Lorem 3</li>"
				+ "<li>normal 1</li>"
				+ "</list>"
				+ "<p>some text</p>"
				+ "<p>more text</p>"
				+ "<p>extra text</p>"
		);
		openTocTools(test);

		test.textViewTools.navigateToText("more text");
		VolumeTest.openInsertVolume(test, BBX.VolumeType.VOLUME);

		test.textViewTools.navigateToTextRelative("Stuff 1");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.textViewTools.navigateToTextRelative("Things 2");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_VOLUME_SPLIT_BUTTON);
		test.textViewTools.navigateToTextRelative("Things 2");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.textViewTools.navigateToTextRelative("Lorem 3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_VOLUME_SPLIT_BUTTON);
		test.textViewTools.navigateToTextRelative("Lorem 3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_DISPERSE_VOLUMES_BUTTON);
		try {
			Assert.assertTrue(TOCBuilderBBX.test_diffTOCSplitsThanVolumes);
		} finally {
			TOCBuilderBBX.test_diffTOCSplitsThanVolumes = false;
		}
		
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isContainerListType(BBX.ListType.NORMAL)
						.nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.TOC_VOLUME_SPLIT)
								.hasText(TransNoteAction.START + "Volume 1" + TransNoteAction.END)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("Stuff")
								.hasStyle("T1-3")
								.childCount(2)
						).nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.TOC_VOLUME_SPLIT)
								.hasText(TransNoteAction.START + "Volume 2" + TransNoteAction.END)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("Things")
								.hasStyle("T1-3")
								.childCount(2)
						).nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.TOC_VOLUME_SPLIT)
								.hasText("TOC Volume split placeholder")
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("Lorem")
								.hasStyle("T1-3")
								.childCount(2)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("normal 1")
								.hasStyle("L1-3")
								.childCount(1)
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("some text")
				).nextChildIs(childAssert -> childAssert
						.isContainer(BBX.CONTAINER.VOLUME)
						.nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.VOLUME_END)
								.hasText("END OF VOLUME 1")
						).nextChildIs(childAssert2 -> childAssert2
								.isContainer(BBX.CONTAINER.VOLUME_TOC)
								.nextChildIs(childAssert3 -> childAssert3
										.hasText("Things")
										.hasStyle("T1-3")
										.childCount(2)
								).nextChildIs(childAssert3 -> childAssert3
										.hasText("Lorem")
										.hasStyle("T1-3")
										.childCount(2)
								).noNextChild()
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("more text")
				).nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("extra text")
				).nextChildIs(childAssert -> childAssert
						.isContainer(BBX.CONTAINER.VOLUME)
						.childCount(1)
						.child(0)
						.isBlock(BBX.BLOCK.VOLUME_END)
						.hasText("THE END")
				).noNextChild();
	}
	
	@Test(expectedExceptions = BBNotifyException.class,enabled =false)
	public void disperse_noVolumeSplits_issue4924() {
		BBTestRunner test = new BBTestRunner("",
				"<list>"
				+ "<li>Stuff 1</li>"
				+ "<li>Things 2</li>"
				+ "<li>Lorem 3</li>"
				+ "<li>normal 1</li>"
				+ "</list>"
				+ "<p>some text</p>"
				+ "<p>more text</p>"
				+ "<p>extra text</p>"
		);
		openTocTools(test);

		test.textViewTools.navigateToText("more text");
		VolumeTest.openInsertVolume(test, BBX.VolumeType.VOLUME);

		test.textViewTools.navigateToText("Stuff 1");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.textViewTools.navigateToText("Things 2");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.textViewTools.navigateToText("Lorem 3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_DISPERSE_VOLUMES_BUTTON);
	}
	
	@Test(enabled = false)
	public void disperse_notEnoughVolumeSplits_issue4924() {
		BBTestRunner test = new BBTestRunner("",
				"<list>"
				+ "<li>Stuff 1</li>"
				+ "<li>Things 2</li>"
				+ "<li>Lorem 3</li>"
				+ "<li>normal 1</li>"
				+ "</list>"
				+ "<p>some text</p>"
				+ "<p>more text</p>"
				+ "<p>extra text</p>"
		);
		openTocTools(test);

		test.textViewTools.navigateToTextRelative("more text");
		VolumeTest.openInsertVolume(test, BBX.VolumeType.VOLUME);
		  
		test.textViewTools.navigateToTextRelative("extra text");
		VolumeTest.openInsertVolume(test, BBX.VolumeType.VOLUME);

		test.textViewTools.navigateToText("Stuff 1");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.textViewTools.navigateToTextRelative("Things 2");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_VOLUME_SPLIT_BUTTON);
		test.textViewTools.navigateToTextRelative("Things 2");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		test.textViewTools.navigateToTextRelative("Lorem 3");
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		
		test.clickButtonWithId(TOCBuilderBBX.SWTBOT_DISPERSE_VOLUMES_BUTTON);
		try {
			Assert.assertTrue(TOCBuilderBBX.test_diffTOCSplitsThanVolumes);
		} finally {
			TOCBuilderBBX.test_diffTOCSplitsThanVolumes = false;
		}
		
		test.assertRootSection_NoBrlCopy()
				.nextChildIs(childAssert -> childAssert
						.isContainerListType(BBX.ListType.NORMAL)
						.nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.TOC_VOLUME_SPLIT)
								.hasText(TransNoteAction.START + "Volume 1" + TransNoteAction.END)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("Stuff")
								.hasStyle("T1-3")
								.childCount(2)
						).nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.TOC_VOLUME_SPLIT)
								.hasText(TransNoteAction.START + "Volume 2" + TransNoteAction.END)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("Things")
								.hasStyle("T1-3")
								.childCount(2)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("Lorem")
								.hasStyle("T1-3")
								.childCount(2)
						).nextChildIs(childAssert2 -> childAssert2
								.hasText("normal 1")
								.hasStyle("L1-3")
								.childCount(1)
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("some text")
				).nextChildIs(childAssert -> childAssert
						.isContainer(BBX.CONTAINER.VOLUME)
						.nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.VOLUME_END)
								.hasText("END OF VOLUME 1")
						).nextChildIs(childAssert2 -> childAssert2
								.isContainer(BBX.CONTAINER.VOLUME_TOC)
								.nextChildIs(childAssert3 -> childAssert3
										.hasText("Things")
										.hasStyle("T1-3")
										.childCount(2)
								).nextChildIs(childAssert3 -> childAssert3
										.hasText("Lorem")
										.hasStyle("T1-3")
										.childCount(2)
								).noNextChild()
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("more text")
				).nextChildIs(childAssert -> childAssert
						.isContainer(BBX.CONTAINER.VOLUME)
						.nextChildIs(childAssert2 -> childAssert2
								.isBlock(BBX.BLOCK.VOLUME_END)
								.hasText("END OF VOLUME 2")
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("extra text")
				).nextChildIs(childAssert -> childAssert
						.isContainer(BBX.CONTAINER.VOLUME)
						.childCount(1)
						.child(0)
						.isBlock(BBX.BLOCK.VOLUME_END)
						.hasText("THE END")
				).noNextChild();
	}
	
	@Test(enabled = false)
	public void rt5624_EditTOCPageNumberPrintView(){
		BBTestRunner bbTest = new BBTestRunner("", "<p>Test 1</p>");
		
		openTocTools(bbTest);
		bbTest.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		
		bbTest.textViewTools.navigateToText("1");
		bbTest.textViewTools.selectRight(1);
		bbTest.textViewTools.typeText("2");
		bbTest.updateTextView();
		
		bbTest.assertRootSection_NoBrlCopy()
			.nextChildIs(child -> child
					.isBlock(BBX.BLOCK.MARGIN)
					.nextChildIsText("Test")
					.nextChildIs(child2 -> child2
							.nextChildIsText("2")
							.noNextChild())
					.noNextChild())
			.noNextChild();
	}
	
	@Test(enabled = false)
	public void rt6134_EmphasizeTOCPageNumberPrintView(){
		BBTestRunner bbTest = new BBTestRunner("", "<p>Test 123</p>");
		
		openTocTools(bbTest);
		bbTest.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
		
		bbTest.textViewTools.navigateToText("123");
		bbTest.textViewTools.selectRight(3);
		bbTest.textViewTools.pressShortcut(SWT.CTRL, 'b');
		bbTest.updateTextView();
		
		bbTest.assertRootSection_NoBrlCopy()
			.nextChildIs(child -> child
					.isBlock(BBX.BLOCK.MARGIN)
					.nextChildIsText("Test")
					.nextChildIs(child2 -> child2
							.nextChildIs(child3 -> child3.isInlineEmphasis(EmphasisType.BOLD)
								.hasText("123"))
							.noNextChild())
					.noNextChild())
			.noNextChild();
	}
	
	@Test(enabled = false)
	public void pagePrefixDisabledDuringNextBoot_rt6018(){
		BBTestRunner bbTest = new BBTestRunner("", "<p>Test 1</p>");
		
		openTocTools(bbTest);
		
		SWTBotCheckBox pagePrefixCheck = bbTest.bot.checkBoxWithId(TOCBuilderBBX.SWTBOT_PAGE_PREFIX_CHECK);
		pagePrefixCheck.click();
		doPendingSWTWork();
		// close toc builder
		bbTest.openMenuItem(TopMenu.TOOLS, "TOC Builder");
		
		openTocTools(bbTest);
	}
	
	@Test
	public void isNodeMovableTest() {
		Document doc = TestXMLUtils.generateBookDoc("", "<p>who was phone</p><p testid='after'><span>test</span></p>");
		doc = BookToBBXConverter.fromConfig().convert(doc);
		
		Element after = TestXMLUtils.getTestIdElement(doc, "after");
		Element span = (Element) after.getChild(0);
		Text text = (Text) span.getChild(0);
		try {
			Assert.assertEquals(TOCBuilderBBX.isPageMovable(text), after);
		} catch (Throwable e) {
			throw new NodeException("failed", text, e);
		}
	}
}
