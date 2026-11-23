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
package org.brailleblaster.printView;

import org.brailleblaster.GuiTest;
import static org.brailleblaster.testrunners.ViewTestRunner.doPendingSWTWork;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.brailleblaster.bbx.BBX;
import org.brailleblaster.perspectives.braille.views.wp.NumberInputDialog;
import org.brailleblaster.perspectives.mvc.menu.EmphasisItem;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.BBXDocFactory;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.testrunners.ViewTestRunner;
import org.brailleblaster.utd.Style;
import org.brailleblaster.utd.exceptions.NodeException;
import org.brailleblaster.utd.properties.EmphasisType;
import org.brailleblaster.utd.properties.NumberLinePosition;
import org.brailleblaster.util.Notify;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import nu.xom.Element;

@GuiTest
public class StyleChangeTest {
    private static final Logger log = LoggerFactory.getLogger(StyleChangeTest.class);
    private final String BLANK_LINE = "";

    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/printView/StyleChangeTest.xml");

    @Test(enabled = false)
    //Changes first element in view into centered sub heading
    public void heading1FirstElement() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "First paragraph on page 1";
        String expectedBraille = "       ,f/ p>~1agraph on page #a";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        bbTest.selectToolbarOption("Centered Heading");

        String result = bbTest.textViewBot.getTextOnLine(0);
        String brailleResult = bbTest.brailleViewBot.getTextOnLine(0);
        assertEquals(expectedText, result);
        assertEquals(expectedBraille, brailleResult);

        result = bbTest.textViewBot.getTextOnLine(1);
        brailleResult = bbTest.brailleViewBot.getTextOnLine(1);
        assertEquals(BLANK_LINE, result);
        assertEquals(BLANK_LINE, brailleResult);
    }

    @Test(enabled = false)
    //Sets inline element in first block element in view and changes it to centered sub heading
    public void heading1InlineElement() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "First paragraph on page 1";
        String expectedBraille = "       ,f/ p>~1agraph on page #a";

        bbTest.navigateTextView(12);
        bbTest.selectToolbarOption("Centered Heading");

        String result = bbTest.textViewBot.getTextOnLine(0);
        String brailleResult = bbTest.brailleViewBot.getTextOnLine(0);
        assertEquals(expectedText, result);
        assertEquals(expectedBraille, brailleResult);

        result = bbTest.textViewBot.getTextOnLine(1);
        brailleResult = bbTest.brailleViewBot.getTextOnLine(1);
        assertEquals(BLANK_LINE, result);
        assertEquals(BLANK_LINE, brailleResult);
    }

    @Test(enabled = false)
    //changes a block element inside the document to a centered sub heading
    public void heading1Middle() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Second Paragraph on Page 2";
        String expectedBraille = "     ,second ,p>agraph on ,page #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.selectToolbarOption("Centered Heading");

        String result = bbTest.textViewBot.getTextOnLine(3);
        String brailleResult = bbTest.brailleViewBot.getTextOnLine(3);
        assertEquals(BLANK_LINE, result);
        assertEquals(BLANK_LINE, brailleResult);

        result = bbTest.textViewBot.getTextOnLine(4);
        brailleResult = bbTest.brailleViewBot.getTextOnLine(4);
        assertEquals(expectedText, result);
        assertEquals(expectedBraille, brailleResult);

        result = bbTest.textViewBot.getTextOnLine(5);
        brailleResult = bbTest.brailleViewBot.getTextOnLine(5);
        assertEquals(BLANK_LINE, result);
        assertEquals(BLANK_LINE, brailleResult);
    }

    @Test(enabled = false)
    //turns last element before page to a centered sub heading
    public void heading1End() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	ViewTestRunner.DEBUG_MODE();
        String expectedText = "Final Paragraph";
        String expectedBraille = "            ,f9al ,p>agraph";

        String pageAfter = "--------------------------------------7";
        String pageAfterBrl = "--------------------------------------#g";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(13));
        bbTest.selectToolbarOption("Centered Heading");

        String result = bbTest.textViewBot.getTextOnLine(13);
        String brailleResult = bbTest.brailleViewBot.getTextOnLine(13);
        assertEquals(BLANK_LINE, result);
        assertEquals(BLANK_LINE, brailleResult);

        result = bbTest.textViewBot.getTextOnLine(14);
        brailleResult = bbTest.brailleViewBot.getTextOnLine(14);
        assertEquals(expectedText, result);
        assertEquals(expectedBraille, brailleResult);

        result = bbTest.textViewBot.getTextOnLine(15);
        brailleResult = bbTest.brailleViewBot.getTextOnLine(15);
        assertEquals(BLANK_LINE, result);
        assertEquals(BLANK_LINE, brailleResult);

        result = bbTest.textViewBot.getTextOnLine(16);
        brailleResult = bbTest.brailleViewBot.getTextOnLine(16);
        assertEquals(pageAfter, result);
        assertEquals(pageAfterBrl, brailleResult);
    }

    @Test(enabled = false)
    public void boldTestSimple() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>This is <span class='smallcaps'>a test</span></p>");

        bbTest.textViewTools.navigateToText("This");
        bbTest.textViewTools.selectRight(3);
        assertEquals(bbTest.textViewWidget.getSelectionText(), "Thi");

        bbTest.bot.menu(EmphasisItem.BOLD.longName).click();

        bbTest.assertRootSectionFirst_NoBrlCopy()
                .isBlockDefaultStyle("Body Text")
                .nextChildIs(childAssert -> childAssert
                        .isInlineEmphasis(EmphasisType.BOLD)
                        .hasText("Thi")
                ).nextChildIsText("s is ")
                .nextChildIs(childAssert -> childAssert
                        .isSpan(BBX.SPAN.OTHER)
                        .hasText("a test")
                ).noNextChild();
    }

    /**
     * This was a style view bug but is useful to test in the text view as well
     */
    @Test(enabled = false)
    public void boldTestTextToSpan() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>This is <span class='smallcaps'>a test</span></p>");

        //Select in style view for auto-expansion
        bbTest.textViewTools.navigateToText("his");
        bbTest.textViewTools.selectRight(7);

        bbTest.openMenuItem(TopMenu.EMPHASIS, EmphasisItem.BOLD.longName);

        bbTest.assertRootSectionFirst_NoBrlCopy()
                .isBlockDefaultStyle("Body Text")
                .nextChildIsText("T")
                .nextChildIs(childAssert -> childAssert
                        .isInlineEmphasis(EmphasisType.BOLD)
                        .hasText("his is ")
                ).nextChildIs(childAssert -> childAssert
                        .isSpan(BBX.SPAN.OTHER)
                        .hasText("a test")
                ).noNextChild();
    }

    /**
     * This was a style view bug but is useful to test in the text view as well
     */
    @Test(enabled = false)
    public void boldTestTextSomeAndSpan() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>This is <span class='smallcaps'>a test</span></p>");

        //Select in style view for auto-expansion
        bbTest.textViewTools.navigateToText("his");
        bbTest.textViewTools.selectToEndOfLine();

        bbTest.openMenuItem(TopMenu.EMPHASIS, EmphasisItem.BOLD.longName);

        bbTest.assertRootSectionFirst_NoBrlCopy()
                .isBlockDefaultStyle("Body Text")
                .nextChildIsText("T")
                .nextChildIs(childAssert -> childAssert
                        .isInlineEmphasis(EmphasisType.BOLD)
                        .hasText("his is ")
                ).nextChildIs(childAssert -> childAssert
                        .isSpan(BBX.SPAN.OTHER)
                        .nextChildIs(childAssert2 -> childAssert2
                                .isInlineEmphasis(EmphasisType.BOLD)
                                .hasText("a test")
                        ).noNextChild()
                ).noNextChild();
    }

    /**
     * This was a style view bug but is useful to test in the text view as well
     */
    @Test(enabled = false)
    public void boldTestTextAllAndSpan() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>This is <span class='smallcaps'>a test</span></p>");

        //Select in style view for auto-expansion
        bbTest.textViewTools.navigateToText("This");
        bbTest.textViewTools.selectToEndOfLine();

        bbTest.openMenuItem(TopMenu.EMPHASIS, EmphasisItem.BOLD.longName);

        bbTest.assertRootSectionFirst_NoBrlCopy()
                .isBlockDefaultStyle("Body Text")
                .nextChildIs(childAssert -> childAssert
                        .isInlineEmphasis(EmphasisType.BOLD)
                        .hasText("This is ")
                ).nextChildIs(childAssert -> childAssert
                        .isSpan(BBX.SPAN.OTHER)
                        .nextChildIs(childAssert2 -> childAssert2
                                .isInlineEmphasis(EmphasisType.BOLD)
                                .hasText("a test")
                        ).noNextChild()
                ).noNextChild();
    }

    @Test(enabled = false)

    public void applyStyleOverBBStyleTest() {
        BBTestRunner bbTest = new BBTestRunner("", "<h1>Heading</h1><p>Test</p>");

        bbTest.textViewTools.navigateToLine(0);
        assertEquals(bbTest.textViewBot.getTextOnCurrentLine().trim(), "Heading");

        CountDownLatch lock = ViewTestRunner.lockedShellHack(s -> {
            SWTBotShell linesAfterWindow = bbTest.bot.shell("Lines After");
            doPendingSWTWork();
            linesAfterWindow.bot().text(0).typeText("3");
            doPendingSWTWork();
            linesAfterWindow.bot().button("OK").click();
            doPendingSWTWork();
        }, "Lines After", bbTest.bot);

        bbTest.openMenuItem(TopMenu.STYLES, "Options", "Lines After");

        try {
            if (!lock.await(10, TimeUnit.SECONDS)) {
                throw new RuntimeException("Work was never completed inside Lines After dialog");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Await interrupted");
        }

        bbTest.assertRootSection_NoBrlCopy()
                .nextChildIs(childAssert -> childAssert
                        .hasText("Heading")
                        .hasBaseStyleWithOptions(
                                "Centered Heading",
                                Style.StyleOption.LINES_AFTER,
                                3
                        )
                ).nextChildIs(childAssert -> childAssert
                        .hasText("Test")
                );

        bbTest.bot.menu("Cell 5 Heading").click();
        doPendingSWTWork();

        bbTest.assertRootSection_NoBrlCopy()
                .nextChildIs(childAssert -> childAssert
                        .hasText("Heading")
                        .hasBaseStyleWithOptions(
                                "Cell 5 Heading",
                                Style.StyleOption.LINES_AFTER,
                                3
                        )
                ).nextChildIs(childAssert -> childAssert
                        .hasText("Test")
                );
    }

    /**
     * This was a style view bug but is useful to test in the text view as well
     */
    @Test(enabled = false)
    public void applyBoxStyleTest_RT3671() {
        BBTestRunner bbTest = new BBTestRunner("",
                "<p>This is a test</p><list><li><p>Testing 123</p></li></list>");

        bbTest.textViewTools.navigateToText("Testing 123");
        bbTest.textViewTools.selectRight("Testing 123".length());

        bbTest.bot.menu("Box").click();
        doPendingSWTWork();

        bbTest.assertRootSection_NoBrlCopy()
                .isSection(BBX.SECTION.ROOT)
                .nextChildIs(childAssert -> childAssert
                        .isBlock(BBX.BLOCK.DEFAULT)
                ).nextChildIs(childAssert -> childAssert
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(childAssert2 -> childAssert2
                                .isContainer(BBX.CONTAINER.BOX)
                                .hasStyle("Box")
                                .nextChildIs(childAssert3 -> childAssert3
                                        .isBlock(BBX.BLOCK.LIST_ITEM)
                                        .hasText("Testing 123")
                                ).noNextChild()
                        ).noNextChild()
                ).noNextChild();
    }

    /**
     * This was a style view bug but is useful to test in the text view as well
     */
    @Test(enabled = false)

    public void styleOptions_KeepWithNextTest() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>paragraph 1</p><p>paragraph 2</p>");

        bbTest.textViewTools.navigateToText("paragraph 1");
        bbTest.textViewBot.pressShortcut(SWT.SHIFT, SWT.ARROW_DOWN, '\0');
        doPendingSWTWork();
        bbTest.textViewTools.selectRight("paragraph 2".length());
        assertThat(
                bbTest.textViewTools.getSelectionStripped(),
                Matchers.containsString("paragraph 1")
        );
        assertThat(
                bbTest.textViewTools.getSelectionStripped(),
                Matchers.containsString("paragraph 2")
        );

        bbTest.bot.menu("Keep With Next").menu("Yes").click();
        doPendingSWTWork();

        bbTest.assertRootSection_NoBrlCopy()
                .isSection(BBX.SECTION.ROOT)
                .nextChildIs(childAssert -> childAssert
                        .hasText("paragraph 1")
                        .hasBaseStyleWithOptions("Body Text", Style.StyleOption.KEEP_WITH_NEXT, true)
                ).nextChildIs(childAssert -> childAssert
                        .hasText("paragraph 2")
                        .hasBaseStyleWithOptions("Body Text", Style.StyleOption.KEEP_WITH_NEXT, true)
                );
    }

    @Test(enabled = false)

    public void nestedEmphasisFont_issue3931() {
        BBTestRunner bbTest = new BBTestRunner(new BBXDocFactory()
                .append(BBX.BLOCK.STYLE.create("Body Text"), child -> child
                        .append(BBX.INLINE.EMPHASIS.create(EmphasisType.TRANS_1, EmphasisType.BOLD), "emphasis")
                        .append(BBX.INLINE.EMPHASIS.create(EmphasisType.TRANS_1, EmphasisType.BOLD, EmphasisType.ITALICS), "nested")
                )
        );

        StyleRange testRange = bbTest.textViewWidget.getStyleRangeAtOffset(
                bbTest.textViewWidget.getText().indexOf("ested")
        );
        assertEquals(testRange.fontStyle & SWT.BOLD, SWT.BOLD);
        assertEquals(testRange.fontStyle & SWT.ITALIC, SWT.ITALIC);
        assertNotNull(testRange.background);
    }

    @Test(enabled = false)

    public void applyStyleBlockInteriorSelect_issue3654() {
        BBTestRunner bbTest = new BBTestRunner("", "<p><span>Not</span> a paragraph</p>");
        bbTest.textViewTools.navigateToText("Not");
        bbTest.textViewTools.selectToEndOf("Not a par");

        bbTest.selectToolbarOption("Centered Heading");
        bbTest.assertRootSectionFirst_NoBrlCopy()
                .isBlock(BBX.BLOCK.STYLE)
                .hasOverrideStyle("Centered Heading")
                .nextChildIs(childAssert2 -> childAssert2
                        .isSpan(BBX.SPAN.OTHER)
                        .hasText("Not")
                ).nextChildIsText(" a paragraph");
    }


    @Test(enabled = false)

    public void tableNotEnabledOnSimpleTables_issue3702() {
        BBTestRunner bbTest = new BBTestRunner("", "<table><tr><td>Testing 1</td><td>testing 2</td></tr><tr><td>Testing 3</td><td>testing 4</td></tr></table>");

        bbTest.navigateTextView(10);

        bbTest.textViewTools.navigateToText("ting 2");
        assertTrue(bbTest.textViewBot.contextMenu("Edit Table").isEnabled(), "No table editor");
    }

    @Test(enabled = false)

    public void applyStyleBreadcrumbsWithPage_issue3806() {
        BBTestRunner bbTest = new BBTestRunner("", "<level1><p>paragraph 1</p><pagenum>5</pagenum><p>paragraph 2</p></level1>");

        bbTest.textViewTools.navigateToText("paragraph 1");
        bbTest.selectBreadcrumbsAncestor(1, BBX.SECTION::assertIsA);
        bbTest.bot.menu("Caption").click();
        doPendingSWTWork();

        bbTest.assertInnerSection_NoBrlCopy()
                .nextChildIs(childAssert -> childAssert
                        .isBlock(BBX.BLOCK.STYLE)
                        .hasOverrideStyle("Caption")
                        .hasText("paragraph 1")
                ).nextChildIs(childAssert -> childAssert
                        .isBlock(BBX.BLOCK.PAGE_NUM)
                        .hasText("5")
                ).nextChildIs(childAssert -> childAssert
                        .isBlock(BBX.BLOCK.STYLE)
                        .hasOverrideStyle("Caption")
                        .hasText("paragraph 2")
                ).noNextChild();
    }

    @Test(enabled = false)

    public void applyStyleBlockInteriorSelect_all_issue3654() {
        BBTestRunner bbTest = new BBTestRunner(new BBXDocFactory()
                .append(BBX.BLOCK.STYLE.create("Body Text"), child -> child
                        .append("Style ")
                        .append(BBX.INLINE.EMPHASIS.create(EmphasisType.BOLD), "bold")
                        .append(" paragraphs")
                ));

        bbTest.textViewTools.navigateToText("Style ");
        bbTest.textViewTools.selectRight(22);
        assertEquals(bbTest.textViewTools.getSelectionStripped(), "Style bold paragraphs");

        bbTest.selectToolbarOption("Centered Heading");

        bbTest.assertRootSectionFirst_NoBrlCopy()
                .isBlock(BBX.BLOCK.STYLE)
                .hasOverrideStyle("Centered Heading")
                .nextChildIsText("Style ")
                .nextChildIs(childAssert -> childAssert
                        .isInlineEmphasis(EmphasisType.BOLD)
                        .hasText("bold")
                ).nextChildIsText(" paragraphs")
                .noNextChild();
    }

    @Test(enabled = false)

    public void styleOptionsBreadcrumbs_DontSplitTest() {
        BBTestRunner bbTest = new BBTestRunner("", "<level1><p>paragraph 1</p><p>paragraph 2</p></level1>");

        bbTest.textViewTools.navigateToText("paragraph 1");
        bbTest.selectBreadcrumbsAncestor(1, BBX.SECTION::assertIsA);

        bbTest.bot.menu("Options").menu("Don't Split").click();
        doPendingSWTWork();

        bbTest.assertInnerSectionFirst_NoBrlCopy()
                .hasStyle("Dont Split")
                .nextChildIs(childAssert2 -> childAssert2
                        .hasText("paragraph 1")
                        .hasStyle("Body Text")
                ).nextChildIs(childAssert2 -> childAssert2
                        .hasText("paragraph 2")
                        .hasStyle("Body Text")
                ).noNextChild();
    }

    @Test(enabled = false)

    public void styleOptionsBreadcrumbs_SkipNumberLines() {
        BBTestRunner bbTest = new BBTestRunner("", "<level1><p>paragraph 1</p><p>paragraph 2</p></level1>");

        bbTest.textViewTools.navigateToText("paragraph 1");
        bbTest.selectBreadcrumbsAncestor(1, BBX.SECTION::assertIsA);

        bbTest.bot.menu("Skip Number Lines").menu("Top").click();
        doPendingSWTWork();

        bbTest.assertInnerSection_NoBrlCopy()
                .nextChildIs(childAssert2 -> childAssert2
                        .hasText("paragraph 1")
                        .hasBaseStyleWithOptions("Body Text", Style.StyleOption.SKIP_NUMBER_LINES, NumberLinePosition.TOP)
                ).nextChildIs(childAssert2 -> childAssert2
                        .hasText("paragraph 2")
                        .hasBaseStyleWithOptions("Body Text", Style.StyleOption.SKIP_NUMBER_LINES, NumberLinePosition.TOP)
                ).noNextChild();
    }

    @Test(enabled = false)

    public void styleOptions_MultipleAndChange() {
        BBTestRunner bbTest = new BBTestRunner("", "<level1><p>paragraph 1</p><p>paragraph 2</p></level1>");

        bbTest.textViewTools.navigateToText("paragraph 1");
        bbTest.selectBreadcrumbsAncestor(1, BBX.SECTION::assertIsA);

        bbTest.bot.menu("Skip Number Lines").menu("Top").click();
        doPendingSWTWork();

        bbTest.textViewTools.navigateToText("paragraph 1");
        bbTest.selectBreadcrumbsAncestor(1, BBX.SECTION::assertIsA);

        bbTest.bot.menu("Keep With Next").menu("Yes").click();
        doPendingSWTWork();

        bbTest.assertInnerSection_NoBrlCopy()
                .nextChildIs(childAssert2 -> childAssert2
                        .hasText("paragraph 1")
                        .hasBaseStyleWithOptions(
                                "Body Text",
                                Style.StyleOption.SKIP_NUMBER_LINES,
                                NumberLinePosition.TOP,
                                Style.StyleOption.KEEP_WITH_NEXT,
                                true
                        )
                ).nextChildIs(childAssert2 -> childAssert2
                        .hasText("paragraph 2")
                        .hasBaseStyleWithOptions(
                                "Body Text",
                                Style.StyleOption.SKIP_NUMBER_LINES,
                                NumberLinePosition.TOP,
                                Style.StyleOption.KEEP_WITH_NEXT,
                                true
                        )
                ).noNextChild();

        bbTest.textViewTools.navigateToText("paragraph 1");
        bbTest.selectBreadcrumbsAncestor(1, BBX.SECTION::assertIsA);

        bbTest.bot.menu("Cell 5 Heading").click();
        doPendingSWTWork();

        bbTest.assertInnerSection_NoBrlCopy()
                .nextChildIs(childAssert2 -> childAssert2
                        .hasText("paragraph 1")
                        .hasAttributeBB(BBX._ATTRIB_OVERRIDE_STYLE, "Cell 5 Heading")
                        .hasBaseStyleWithOptions(
                                "Cell 5 Heading",
                                Style.StyleOption.SKIP_NUMBER_LINES,
                                NumberLinePosition.TOP,
                                Style.StyleOption.KEEP_WITH_NEXT,
                                true
                        )
                ).nextChildIs(childAssert2 -> childAssert2
                        .hasText("paragraph 2")
                        .hasAttributeBB(BBX._ATTRIB_OVERRIDE_STYLE, "Cell 5 Heading")
                        .hasBaseStyleWithOptions(
                                "Cell 5 Heading",
                                Style.StyleOption.SKIP_NUMBER_LINES,
                                NumberLinePosition.TOP,
                                Style.StyleOption.KEEP_WITH_NEXT,
                                true
                        )
                ).noNextChild();
    }

    @Test(enabled = false)

    public void styleOptions_onMarginStyle() {
        BBTestRunner test = new BBTestRunner("",
                "<p>Test 1</p>"
                        + "<p>List Heading</p>"
                        + "<p>Test 2</p>"
        );

        test.textViewTools.navigateToText("List Heading");
        test.textViewTools.selectToEndOf("List Heading");
        test.openMenuItem(TopMenu.STYLES, "Numeric", "Indent 1", "1-1");

        test.textViewTools.navigateToText("List Heading");
        test.textViewTools.selectToEndOf("List Heading");
        test.openMenuItem(TopMenu.STYLES, "Options", "New Pages Before");

        SWTBotShell activeShell = test.bot.activeShell();
        activeShell.widget.forceActive();
        doPendingSWTWork();
        SWTBot bot = activeShell.bot();
        bot.text(0).setText("6");
        doPendingSWTWork();
        bot.buttonWithId(NumberInputDialog.SWTBOT_OK_BUTTON).click();
        doPendingSWTWork();

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(child2 -> child2
                        .hasText("Test 1")
                        .isBlockWithStyle("Body Text")
                ).nextChildIs(child2 -> child2
                        .hasText("List Heading")
                        .hasBaseStyleWithOptions("1-1", Style.StyleOption.NEW_PAGES_BEFORE, 6)
                ).nextChildIs(child2 -> child2
                        .hasText("Test 2")
                        .isBlockWithStyle("Body Text")
                ).noNextChild();
    }

    @Test(enabled = false)

    public void styleOptions_onListStyle() {
        BBTestRunner test = new BBTestRunner("",
                "<list>"
                        + "<li>Test 1</li>"
                        + "<li>List Heading</li>"
                        + "<li>Test 2</li>"
                        + "</list>"
        );

        test.textViewTools.navigateToText("List Heading");
        test.textViewTools.selectToEndOf("List Heading");
        test.openMenuItem(TopMenu.STYLES, "Lists", "List 2 Levels", "L3-5");

        test.textViewTools.navigateToText("List Heading");
        test.textViewTools.selectToEndOf("List Heading");
        test.openMenuItem(TopMenu.STYLES, "Options", "New Pages Before");

        SWTBotShell activeShell = test.bot.activeShell();
        activeShell.widget.forceActive();
        doPendingSWTWork();
        SWTBot bot = activeShell.bot();
        bot.text(0).setText("6");
        doPendingSWTWork();
        bot.buttonWithId(NumberInputDialog.SWTBOT_OK_BUTTON).click();
        doPendingSWTWork();

        test.assertRootSectionFirst_NoBrlCopy()
                .isContainerListType(BBX.ListType.NORMAL)
                .nextChildIs(child2 -> child2
                        .hasText("Test 1")
                        .isBlockWithStyle("L1-5")
                ).nextChildIs(child2 -> child2
                        .hasText("List Heading")
                        .hasBaseStyleWithOptions("L3-5", Style.StyleOption.NEW_PAGES_BEFORE, 6)
                ).nextChildIs(child2 -> child2
                        .hasText("Test 2")
                        .isBlockWithStyle("L1-5")
                ).noNextChild();
    }

    @Test(enabled = false)

    public void styleOptions_elementWithBBXStyle_issue6032() {
        BBTestRunner test = new BBTestRunner("",
                "<p testid='first'>para1</p>"
                        + "<p>para2</p>"
        );

        Element first = TestXMLUtils.getTestIdElement(test.getDoc(), "first");
        BBX.BLOCK.DEFAULT.assertIsA(first);
        test.openMenuItem(TopMenu.STYLES, "Options", "New Pages After");

        SWTBot newPagesAfterBot = test.bot.activeShell().bot();
        newPagesAfterBot.text(0).setText("6");
        doPendingSWTWork();
        newPagesAfterBot.button("OK").click();
        doPendingSWTWork();

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(p -> p
                        .hasText("para1")
                        .hasBaseStyleWithOptions("Body Text", Style.StyleOption.NEW_PAGES_AFTER, 6)
                        .inlineTest(thisAssert -> {
                            BBX.BLOCK.DEFAULT.assertIsA(thisAssert.element());
                            if (BBX._ATTRIB_OVERRIDE_STYLE.has(thisAssert.element())) {
                                throw new NodeException("Should work without overrideStyle", thisAssert.element());
                            }
                        })
                ).nextChildIs(p -> p
                        .hasText("para2")
                        .isBlockWithStyle("Body Text")
                ).noNextChild();
    }

    @Test(enabled = false)

    public void styleOptions_elementWithBBXStyle_changedToOtherStyle_issue6032() {
        BBTestRunner test = new BBTestRunner("",
                "<p testid='first'>para1</p>"
                        + "<p>para2</p>"
        );

        Element first = TestXMLUtils.getTestIdElement(test.getDoc(), "first");
        BBX.BLOCK.DEFAULT.assertIsA(first);

        test.openMenuItem(TopMenu.STYLES, "Options", "New Pages After");
        SWTBot newPagesAfterBot = test.bot.activeShell().bot();
        newPagesAfterBot.text(0).setText("6");
        doPendingSWTWork();
        newPagesAfterBot.button("OK").click();
        doPendingSWTWork();

        test.openMenuItem(TopMenu.STYLES, "Options", "New Pages Before");
        SWTBot newPagesBeforeBot = test.bot.activeShell().bot();
        newPagesBeforeBot.text(0).setText("8");
        doPendingSWTWork();
        newPagesBeforeBot.button("OK").click();
        doPendingSWTWork();

        test.openMenuItem(TopMenu.STYLES, "Heading", "Cell 5 Heading");

        test.openMenuItem(TopMenu.STYLES, "Options", "Lines Before");
        SWTBot linesBeforeBot = test.bot.activeShell().bot();
        linesBeforeBot.text(0).setText("4");
        doPendingSWTWork();
        linesBeforeBot.button("OK").click();
        doPendingSWTWork();

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(p -> p
                        .hasText("para1")
                        .hasBaseStyleWithOptions("Cell 5 Heading",
                                Style.StyleOption.NEW_PAGES_AFTER, 6,
                                Style.StyleOption.NEW_PAGES_BEFORE, 8,
                                Style.StyleOption.LINES_BEFORE, 4
                        )
                        .inlineTest(thisAssert -> {
                            BBX.BLOCK.STYLE.assertIsA(thisAssert.element());
                            if (!BBX._ATTRIB_OVERRIDE_STYLE.has(thisAssert.element())) {
                                throw new NodeException("Needs overrideStyle", thisAssert.element());
                            }
                        })
                ).nextChildIs(p -> p
                        .hasText("para2")
                        .isBlockWithStyle("Body Text")
                ).noNextChild();
    }

    @Test(expectedExceptions = Notify.DebugException.class, enabled = false)
    public void crossSelection_Box_Invalid() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>paragraph</p>"
                + "<list>"
                + "<li>list1</li>"
                + "<li>list2</li>"
                + "</list>");

        bbTest.textViewTools.navigateToText("paragraph");
        bbTest.textViewTools.selectToEndOf("list1");
        assertEquals(
                bbTest.textViewTools.getSelectionStripped(),
                "paragraphlist1"
        );

        bbTest.openMenuItem(TopMenu.STYLES, "Miscellaneous", "Boxes", "Box");
        doPendingSWTWork();
    }

    @Test(enabled = false)

    public void crossSelection_Box() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>paragraph</p>"
                + "<list>"
                + "<li>list1</li>"
                + "<li>list2</li>"
                + "</list>");

        bbTest.textViewTools.navigateToText("paragraph");
        bbTest.textViewTools.selectToEndOf("list2");

        bbTest.openMenuItem(TopMenu.STYLES, "Miscellaneous", "Boxes", "Box");
        doPendingSWTWork();

        bbTest.assertRootSectionFirst_NoBrlCopy()
                .hasStyle("Box")
                .nextChildIs(childAssert2 -> childAssert2
                        .hasText("paragraph")
                        .hasStyle("Body Text")
                ).nextChildIs(childAssert -> childAssert
                        .hasStyle("List Tag")
                        .nextChildIs(childAssert2 -> childAssert2
                                .hasText("list1")
                                .hasStyle("L1-3")
                        ).nextChildIs(childAssert2 -> childAssert2
                                .hasText("list2")
                                .hasStyle("L1-3")
                        ).noNextChild()
                ).noNextChild();
    }

    @Test(enabled = false)

    public void crossSelection_DontSplit_List() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>paragraph</p>"
                + "<list>"
                + "<li>list1</li>"
                + "<li>list2</li>"
                + "</list>");

        bbTest.textViewTools.navigateToText("paragraph");
        bbTest.textViewTools.selectToEndOf("list2");
        assertEquals(
                bbTest.textViewTools.getSelectionStripped(),
                "paragraphlist1list2"
        );

        bbTest.openMenuItem(TopMenu.STYLES, "Options", "Don't Split");
        doPendingSWTWork();

        bbTest.assertRootSectionFirst_NoBrlCopy()
                .hasStyle("Dont Split")
                .nextChildIs(childAssert -> childAssert
                        .hasText("paragraph")
                        .hasStyle("Body Text")
                ).nextChildIs(childAssert -> childAssert
                        .hasStyle("List Tag")
                        .nextChildIs(childAssert2 -> childAssert2
                                .hasText("list1")
                                .hasStyle("L1-3")
                        ).nextChildIs(childAssert2 -> childAssert2
                                .hasText("list2")
                                .hasStyle("L1-3")
                        ).noNextChild()
                ).noNextChild();
    }

    @Test(enabled = false)

    public void crossSelection_DontSplit_TableAtEnd() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>paragraph 1</p>"
                + "<table>"
                + "<tr><td>2</td><td>3</td></tr>"
                + "<tr><td>4</td><td>5</td></tr>"
                + "</table>"
                + "<p>paragraph 6</p>");

        bbTest.textViewTools.navigateToText("paragraph 1");
        bbTest.textViewTools.selectToEndOf("2");
        assertEquals(
                bbTest.textViewTools.getSelectionStripped(),
                "paragraph 12"
        );

        bbTest.openMenuItem(TopMenu.STYLES, "Options", "Don't Split");
        doPendingSWTWork();

        bbTest.assertRootSection_NoBrlCopy()
                .nextChildIs(childAssert -> childAssert
                        .hasStyle("Dont Split")
                        .nextChildIs(childAssert2 -> childAssert2
                                .hasText("paragraph 1")
                                .hasStyle("Body Text")
                        ).nextChildIs(childAssert2 -> childAssert2
                                .isContainer(BBX.CONTAINER.TABLE)
                        ).nextChildIs(childAssert2 -> childAssert2
                                .isContainer(BBX.CONTAINER.TABLE)
                        ).noNextChild()
                ).nextChildIs(childAssert2 -> childAssert2
                        .hasText("paragraph 6")
                        .hasStyle("Body Text")
                ).noNextChild();
    }

    @Test(enabled = false)

    public void crossSelection_DontSplit_TableAtStart() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>paragraph 1</p>"
                + "<table>"
                + "<tr><td>2</td><td>3</td></tr>"
                + "<tr><td>4</td><td>5</td></tr>"
                + "</table>"
                + "<p>paragraph 6</p>");

        bbTest.textViewTools.navigateToText("5");
        bbTest.textViewTools.selectToEndOf("6");

        bbTest.openMenuItem(TopMenu.STYLES, "Options", "Don't Split");
        doPendingSWTWork();

        bbTest.assertRootSection_NoBrlCopy()
                .nextChildIs(childAssert2 -> childAssert2
                        .hasText("paragraph 1")
                        .hasStyle("Body Text")
                ).nextChildIs(childAssert -> childAssert
                        .hasStyle("Dont Split")
                        .nextChildIs(childAssert2 -> childAssert2
                                .isContainer(BBX.CONTAINER.TABLE)
                        ).nextChildIs(childAssert2 -> childAssert2
                                .isContainer(BBX.CONTAINER.TABLE)
                        ).nextChildIs(childAssert2 -> childAssert2
                                .hasText("paragraph 6")
                                .hasStyle("Body Text")
                        ).noNextChild()
                ).noNextChild();
    }

    @Test(enabled = false)

    public void crossSelection_ListAtEnd() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>paragraph 1</p>"
                + "<div>"
                + "<p>paragraph 2</p>"
                + "<p>paragraph 3</p>"
                + "</div>"
                + "<p>paragraph 4</p>");

        bbTest.textViewTools.navigateToText("paragraph 1");
        bbTest.textViewTools.selectToEndOf("paragraph 3");

        bbTest.openMenuItem(TopMenu.STYLES, "Lists", "List Tag");
        doPendingSWTWork();

        bbTest.assertRootSection_NoBrlCopy()
                .nextChildIs(childAssert -> childAssert
                        .hasStyle("List Tag")
                        .nextChildIs(childAssert2 -> childAssert2
                                .hasText("paragraph 1")
                                .hasStyle("Body Text")
                        ).nextChildIs(childAssert2 -> childAssert2
                                .isContainer(BBX.CONTAINER.OTHER)
                                .nextChildIs(childAssert3 -> childAssert3
                                        .hasText("paragraph 2")
                                        .hasStyle("Body Text")
                                ).nextChildIs(childAssert3 -> childAssert3
                                        .hasText("paragraph 3")
                                        .hasStyle("Body Text")
                                ).noNextChild()
                        ).noNextChild()
                ).nextChildIs(childAssert2 -> childAssert2
                        .hasText("paragraph 4")
                        .hasStyle("Body Text")
                ).noNextChild();
    }

    @Test(enabled = false)

    public void crossSelection_ListAtStart() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>paragraph 1</p>"
                + "<div>"
                + "<p>paragraph 2</p>"
                + "<p>paragraph 3</p>"
                + "</div>"
                + "<p>paragraph 4</p>");

        bbTest.textViewTools.navigateToText("paragraph 2");
        bbTest.textViewTools.selectToEndOf("paragraph 4");

        bbTest.openMenuItem(TopMenu.STYLES, "Lists", "List Tag");
        doPendingSWTWork();

        bbTest.assertRootSection_NoBrlCopy()
                .nextChildIs(childAssert2 -> childAssert2
                        .hasText("paragraph 1")
                        .hasStyle("Body Text")
                ).nextChildIs(childAssert -> childAssert
                        .hasStyle("List Tag")
                        .nextChildIs(childAssert2 -> childAssert2
                                .isContainer(BBX.CONTAINER.OTHER)
                                .nextChildIs(childAssert3 -> childAssert3
                                        .hasText("paragraph 2")
                                        .hasStyle("Body Text")
                                ).nextChildIs(childAssert3 -> childAssert3
                                        .hasText("paragraph 3")
                                        .hasStyle("Body Text")
                                ).noNextChild()
                        ).nextChildIs(childAssert2 -> childAssert2
                                .hasText("paragraph 4")
                                .hasStyle("Body Text")
                        ).noNextChild()
                ).noNextChild();
    }

    @Test(enabled = false)

    public void autoListContainer_Paragraphs() {
        BBTestRunner test = new BBTestRunner("", "<p>paragraph 1</p>"
                + "<p>paragraph 2</p>"
                + "<p>paragraph 3</p>"
                + "<p>paragraph 4</p>"
        );

        test.textViewTools.navigateToText("paragraph 2");
        test.textViewTools.selectToEndOf("paragraph 2");
        test.openMenuItem(TopMenu.STYLES, "Lists", "List 1 Level", "L1-3");

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(child -> child
                        .hasText("paragraph 1")
                        .isBlockWithStyle("Body Text")
                ).nextChildIs(child -> child
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(child2 -> child2
                                .hasText("paragraph 2")
                                .isBlockWithStyle("L1-3")
                        ).noNextChild()
                ).nextChildIs(child -> child
                        .hasText("paragraph 3")
                        .isBlockWithStyle("Body Text")
                ).nextChildIs(child -> child
                        .hasText("paragraph 4")
                        .isBlockWithStyle("Body Text")
                ).noNextChild();

        test.textViewTools.navigateToTextRelative("paragraph 3");
        test.textViewTools.selectToEndOf("paragraph 3");
        test.openMenuItem(TopMenu.STYLES, "Lists", "List 2 Levels", "L3-5");

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(child -> child
                        .hasText("paragraph 1")
                        .isBlockWithStyle("Body Text")
                ).nextChildIs(child -> child
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(child2 -> child2
                                .hasText("paragraph 2")
                                .isBlockWithStyle("L1-5")
                        ).nextChildIs(child2 -> child2
                                .hasText("paragraph 3")
                                .isBlockWithStyle("L3-5")
                        ).noNextChild()
                ).nextChildIs(child -> child
                        .hasText("paragraph 4")
                        .isBlockWithStyle("Body Text")
                ).noNextChild();

        test.textViewTools.navigateToText("paragraph 1");
        test.textViewTools.selectToEndOf("paragraph 1");
        test.openMenuItem(TopMenu.STYLES, "Lists", "List 3 Levels", "L5-7");

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(child -> child
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(child2 -> child2
                                .hasText("paragraph 1")
                                .isBlockWithStyle("L5-7")
                        ).nextChildIs(child2 -> child2
                                .hasText("paragraph 2")
                                .isBlockWithStyle("L1-7")
                        ).nextChildIs(child2 -> child2
                                .hasText("paragraph 3")
                                .isBlockWithStyle("L3-7")
                        ).noNextChild()
                ).nextChildIs(child -> child
                        .hasText("paragraph 4")
                        .isBlockWithStyle("Body Text")
                ).noNextChild();
    }

    @Test(enabled = false)

    public void autoListContainer_NestedList() {
        BBTestRunner test = new BBTestRunner("",
                "<list>"
                        + "<h1>List Heading</h1>"
                        + "<li>item1</li>"
                        + "<li>item2</li>"
                        + "</list>"
        );

        test.textViewTools.navigateToText("List Heading");
        test.textViewTools.selectToEndOf("List Heading");
        test.openMenuItem(TopMenu.STYLES, "Lists", "List 2 Levels", "L3-5");

        test.assertRootSectionFirst_NoBrlCopy()
                .isContainerListType(BBX.ListType.NORMAL)
                .nextChildIs(child2 -> child2
                        .hasText("List Heading")
                        .isBlockWithStyle("L3-5")
                ).nextChildIs(child2 -> child2
                        .hasText("item1")
                        .isBlockWithStyle("L1-5")
                ).nextChildIs(child2 -> child2
                        .hasText("item2")
                        .isBlockWithStyle("L1-5")
                ).noNextChild();
    }

    @Test(enabled = false)

    public void autoRemoveListItemFromContainer_issue5354() {
        BBTestRunner test = new BBTestRunner("",
                "<list>"
                        + "<li>item1</li>"
                        + "<li>item2</li>"
                        + "<li>item3</li>"
                        + "</list>");

        test.textViewTools.navigateToText("item1");
        test.openMenuItem(TopMenu.STYLES, "Basic", "Body Text");

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(child -> child
                        .isBlockWithStyle("Body Text")
                        .hasText("item1")
                ).nextChildIs(child -> child
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(child2 -> child2
                                .isBlockWithStyle("L1-3")
                                .hasText("item2"))
                        .nextChildIs(child2 -> child2
                                .isBlockWithStyle("L1-3")
                                .hasText("item3"))
                ).noNextChild();
    }

    @Test(enabled = false)

    public void autoRemoveListItemFromContainer_Middle_issue5354() {
        BBTestRunner test = new BBTestRunner("",
                "<list>"
                        + "<li>item1</li>"
                        + "<li>item2</li>"
                        + "<li>item3</li>"
                        + "</list>");

        test.textViewTools.navigateToText("item2");
        test.openMenuItem(TopMenu.STYLES, "Basic", "Body Text");

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(child -> child
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(child2 -> child2
                                .isBlockWithStyle("L1-3")
                                .hasText("item1")
                        )
                ).nextChildIs(child -> child
                        .isBlockWithStyle("Body Text")
                        .hasText("item2")
                ).nextChildIs(child -> child
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(child2 -> child2
                                .isBlockWithStyle("L1-3")
                                .hasText("item3")
                        )
                ).noNextChild();
    }

    @Test(enabled = false)

    public void autoRemoveListItemFromContainer_End_issue5354() {
        BBTestRunner test = new BBTestRunner("",
                "<list>"
                        + "<li>item1</li>"
                        + "<li>item2</li>"
                        + "<li>item3</li>"
                        + "</list>");

        test.textViewTools.navigateToText("item3");
        test.openMenuItem(TopMenu.STYLES, "Basic", "Body Text");

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(child -> child
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(child2 -> child2
                                .isBlockWithStyle("L1-3")
                                .hasText("item1")
                        ).nextChildIs(child2 -> child2
                                .isBlockWithStyle("L1-3")
                                .hasText("item2")
                        ).noNextChild()
                ).nextChildIs(child -> child
                        .isBlockWithStyle("Body Text")
                        .hasText("item3")
                ).noNextChild();
    }

    @Test(enabled = false)

    public void autoRemoveListItemFromContainer_Box_issue5354() {
        BBTestRunner test = new BBTestRunner("",
                "<list>"
                        + "<li>item1</li>"
                        + "<li>item2</li>"
                        + "<li>item3</li>"
                        + "</list>");

        test.textViewTools.navigateToText("item2");
        test.openMenuItem(TopMenu.STYLES, "Miscellaneous", "Boxes", "Box");

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(child -> child
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(child2 -> child2
                                .isBlockWithStyle("L1-3")
                                .hasText("item1")
                        ).nextChildIs(child2 -> child2
                                .isContainer(BBX.CONTAINER.BOX)
                                .childCount(1)
                                .child(0)
                                .isBlockWithStyle("L1-3")
                                .hasText("item2")
                        ).nextChildIs(child2 -> child2
                                .isBlockWithStyle("L1-3")
                                .hasText("item3")
                        ).noNextChild()
                ).noNextChild();
    }

    @Test(enabled = false)

    public void autoRunoverListItem_DecreaseListItemRunover_Start_issue5889() {
        BBTestRunner test = new BBTestRunner("",
                "<list>"
                        + "<list>"
                        + "<li>nested 1</li>"
                        + "<li>nested 2</li>"
                        + "<li>nested 3</li>"
                        + "</list>"
                        + "</list>"
        );

        test.textViewTools.navigateToText("nested 1");
        test.textViewTools.selectToEndOf("nested 1");
        test.openMenuItem(TopMenu.STYLES, "Lists", "List 1 Level", "L1-3");

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(listAssert -> listAssert
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(itemAssert -> itemAssert
                                .hasText("nested 1")
                                .isBlockWithStyle("L1-3")
                        ).noNextChild()
                ).nextChildIs(listAssert -> listAssert
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(itemAssert -> itemAssert
                                .hasText("nested 2")
                                .isBlockWithStyle("L3-5")
                        ).nextChildIs(itemAssert -> itemAssert
                                .hasText("nested 3")
                                .isBlockWithStyle("L3-5")
                        ).noNextChild()
                ).noNextChild();
    }

    @Test(enabled = false)

    public void autoRunoverListItem_DecreaseListItemRunover_Start_Outside_issue5889() {
        BBTestRunner test = new BBTestRunner("",
                "<p>paragraph</p>"
                        + "<list>"
                        + "<list>"
                        + "<li>nested 1</li>"
                        + "<li>nested 2</li>"
                        + "</list>"
                        + "</list>"
        );

        test.textViewTools.navigateToText("paragraph");
        test.textViewTools.selectToEndOf("paragraph");
        test.openMenuItem(TopMenu.STYLES, "Lists", "List 1 Level", "L1-3");

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(listAssert -> listAssert
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(itemAssert -> itemAssert
                                .hasText("paragraph")
                                .isBlockWithStyle("L1-3")
                        ).noNextChild()
                ).nextChildIs(listAssert -> listAssert
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(itemAssert -> itemAssert
                                .hasText("nested 1")
                                .isBlockWithStyle("L3-5")
                        ).nextChildIs(itemAssert -> itemAssert
                                .hasText("nested 2")
                                .isBlockWithStyle("L3-5")
                        ).noNextChild()
                ).noNextChild();
    }

    @Test(enabled = false)

    public void autoRunoverListItem_DecreaseListItemRunover_Middle_issue5889() {
        BBTestRunner test = new BBTestRunner("",
                "<list>"
                        + "<list>"
                        + "<li>nested 1</li>"
                        + "<li>nested 2</li>"
                        + "<li>nested 3</li>"
                        + "</list>"
                        + "</list>"
        );

        test.textViewTools.navigateToText("nested 2");
        test.textViewTools.selectToEndOf("nested 2");
        test.openMenuItem(TopMenu.STYLES, "Lists", "List 1 Level", "L1-3");

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(listAssert -> listAssert
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(itemAssert -> itemAssert
                                .hasText("nested 1")
                                .isBlockWithStyle("L3-5")
                        ).noNextChild()
                ).nextChildIs(listAssert -> listAssert
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(itemAssert -> itemAssert
                                .hasText("nested 2")
                                .isBlockWithStyle("L1-3")
                        ).noNextChild()
                ).nextChildIs(listAssert -> listAssert
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(itemAssert -> itemAssert
                                .hasText("nested 3")
                                .isBlockWithStyle("L3-5")
                        ).noNextChild()
                ).noNextChild();
    }

    @Test(enabled = false)

    public void autoRunoverListItem_DecreaseListItemRunover_End_issue5889() {
        BBTestRunner test = new BBTestRunner("",
                "<list>"
                        + "<list>"
                        + "<li>nested 1</li>"
                        + "<li>nested 2</li>"
                        + "<li>nested 3</li>"
                        + "</list>"
                        + "</list>"
        );

        test.textViewTools.navigateToText("nested 3");
        test.textViewTools.selectToEndOf("nested 3");
        test.openMenuItem(TopMenu.STYLES, "Lists", "List 1 Level", "L1-3");

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(listAssert -> listAssert
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(itemAssert -> itemAssert
                                .hasText("nested 1")
                                .isBlockWithStyle("L3-5")
                        ).nextChildIs(itemAssert -> itemAssert
                                .hasText("nested 2")
                                .isBlockWithStyle("L3-5")
                        ).noNextChild()
                ).nextChildIs(listAssert -> listAssert
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(itemAssert -> itemAssert
                                .hasText("nested 3")
                                .isBlockWithStyle("L1-3")
                        ).noNextChild()
                ).noNextChild();
    }

    @Test(enabled = false)

    public void autoRunoverListItem_DecreaseListItemRunover_End_Outside_issue5889() {
        BBTestRunner test = new BBTestRunner("",
                "<list>"
                        + "<list>"
                        + "<li>nested 1</li>"
                        + "<li>nested 2</li>"
                        + "</list>"
                        + "</list>"
                        + "<p>paragraph</p>"
        );

        test.textViewTools.navigateToText("paragraph");
        test.textViewTools.selectToEndOf("paragraph");
        test.openMenuItem(TopMenu.STYLES, "Lists", "List 1 Level", "L1-3");

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(listAssert -> listAssert
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(itemAssert -> itemAssert
                                .hasText("nested 1")
                                .isBlockWithStyle("L3-5")
                        ).nextChildIs(itemAssert -> itemAssert
                                .hasText("nested 2")
                                .isBlockWithStyle("L3-5")
                        ).noNextChild()
                ).nextChildIs(listAssert -> listAssert
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(itemAssert -> itemAssert
                                .hasText("paragraph")
                                .isBlockWithStyle("L1-3")
                        ).noNextChild()
                ).noNextChild();
    }

    @Test(description = "give same list styles to an entire nested list", enabled = false)
    public void autoRunoverListItem_WholeList_Reapply() {
        BBTestRunner test = new BBTestRunner("",
                "<list>"
                        + "  <li>level 2.1</li>"
                        + "  <li>level 2.2</li>"
                        + "</list>"
        );

        test.textViewTools.navigateToText("level 2.1");
        test.textViewTools.selectToEndOf("level 2.2");

        test.openMenuItem(TopMenu.STYLES, "Lists", "List 1 Level", "L1-3");

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(listAssert -> listAssert
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(itemAssert -> itemAssert
                                .hasText("level 2.1")
                                .isBlockWithStyle("L1-3")
                        ).nextChildIs(itemAssert -> itemAssert
                                .hasText("level 2.2")
                                .isBlockWithStyle("L1-3")
                        ).noNextChild()
                ).noNextChild();
    }

    @Test(enabled = false)

    public void autoRunoverListItem_DecreaseListItemRunover_WholeList_Nested() {
        BBTestRunner test = new BBTestRunner("",
                "<list>"
                        + "<li>"
                        + "  <list>"
                        + "    <li>level 2.1</li>"
                        + "    <li>level 2.2</li>"
                        + "  </list>"
                        + "</li>"
                        + "</list>"
        );

        test.textViewTools.navigateToText("level 2.1");
        test.textViewTools.selectToEndOf("level 2.2");

        test.openMenuItem(TopMenu.STYLES, "Lists", "List 1 Level", "L1-3");

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(listAssert -> listAssert
                        .isContainerListType(BBX.ListType.NORMAL)
                        .nextChildIs(itemAssert -> itemAssert
                                .hasText("level 2.1")
                                .isBlockWithStyle("L1-3")

                        ).nextChildIs(itemAssert -> itemAssert
                                .hasText("level 2.2")
                                .isBlockWithStyle("L1-3")
                        ).inlineTest(thisAssert -> {
                            if (BBX.PreFormatterMarker.ATTRIB_PRE_FORMATTER_MARKER.has(thisAssert.element())) {
                                throw new NodeException("attrib not removed", thisAssert.element());
                            }
                        }).noNextChild()
                ).noNextChild();
    }

    @Test(enabled = false)

    public void poemIndentChange_issue5577() {
        BBTestRunner test = new BBTestRunner("", "<poem><linegroup><line>test1</line><line>test2</line></linegroup></poem>");

        test.textViewTools.navigateToText("test1");
        test.clickStyleMenuItem("Poetry", "P3-5");

        test.assertRootSectionFirst_NoBrlCopy()
                .isContainerListType(BBX.ListType.POEM)
                .childCount(1)
                .child(0)
                .nextChildIs(block -> block
                        .onlyChildIsText("test1")
                        .hasStyle("P3-5")
                ).nextChildIs(block -> block
                        .onlyChildIsText("test2")
                        .hasStyle("P1-5")
                );
    }

    @Test(enabled = false)

    public BBTestRunner poetry_fromParagraphs_issue5994() {
        BBTestRunner test = new BBTestRunner("",
                "<p>para1</p>"
                        + "<p>para2</p>"
        );

        test.textViewTools.selectFromTo("para1", "para2");
        test.openMenuItem(TopMenu.STYLES, "Poetry", "Poetic Stanza");

        test.textViewTools.selectFromTo("para1", "para2");
        test.openMenuItem(TopMenu.STYLES, "Poetry", "Poetry 1 Level", "P1-3");

        test.assertRootSectionFirst_NoBrlCopy()
                .isContainerListType(BBX.ListType.POEM)
                .nextChildIs(block -> block
                        .isBlock(BBX.BLOCK.LIST_ITEM)
                        .onlyChildIsText("para1")
                        .hasStyle("P1-3")
                ).nextChildIs(block -> block
                        .isBlock(BBX.BLOCK.LIST_ITEM)
                        .onlyChildIsText("para2")
                        .hasStyle("P1-3")
                ).noNextChild();

        return test;
    }

    @Test(enabled = false)

    public void poetry_changeListItemToNonPoetry_issue5994() {
        BBTestRunner test = poetry_fromParagraphs_issue5994();

        test.textViewTools.selectFromTo("para2", "para2");
        test.openMenuItem(TopMenu.STYLES, "Glossary", "Glossary 1 Level", "G1-3");

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(list -> list
                        .isContainerListType(BBX.ListType.POEM)
                        .nextChildIs(block -> block
                                .isBlock(BBX.BLOCK.LIST_ITEM)
                                .onlyChildIsText("para1")
                                .hasStyle("P1-3")
                        ).noNextChild()
                ).nextChildIs(list -> list
                        .isContainerListType(BBX.ListType.DEFINITION)
                        .nextChildIs(block -> block
                                .isBlock(BBX.BLOCK.LIST_ITEM)
                                .onlyChildIsText("para2")
                                .hasStyle("G1-3")
                        ).noNextChild()
                );
    }

    @Test(enabled = false)

    public void poetry_reapply_issue5994() {
        BBTestRunner test = poetry_fromParagraphs_issue5994();

        test.textViewTools.selectFromTo("para2", "para2");
        test.openMenuItem(TopMenu.STYLES, "Poetry", "Poetic Stanza");

        Runnable run = () -> test.assertRootSectionFirst_NoBrlCopy()
                .isContainerListType(BBX.ListType.POEM)
                .nextChildIs(block -> block
                        .isBlock(BBX.BLOCK.LIST_ITEM)
                        .onlyChildIsText("para1")
                        .hasStyle("P1-3")
                ).nextChildIs(wrapper -> wrapper
                        .isContainerListType(BBX.ListType.POEM_LINE_GROUP)
                        .nextChildIs(block -> block
                                .isBlock(BBX.BLOCK.LIST_ITEM)
                                .onlyChildIsText("para2")
                                .hasStyle("P1-3")
                        ).noNextChild()
                ).noNextChild();

        run.run();

        // re-apply to break again
        try {
            test.textViewTools.selectFromTo("para2", "para2");
            test.openMenuItem(TopMenu.STYLES, "Poetry", "Poetic Stanza");
        } catch (RuntimeException e) {
            if (!e.getMessage().equals("Cannot have a list inside a list.")) {
                throw e;
            }
        }

        run.run();
    }

    @Test(enabled = false)

    public void pageMakesBlock_dedicated_issue5975() {
        BBTestRunner test = new BBTestRunner("", "<p>test1</p><p>test2</p>");

        test.textViewTools.navigateToText("test2");
        test.textViewTools.typeLine("p5");

        test.textViewTools.selectFromTo("p5", "p5");
        test.openMenuItem(TopMenu.STYLES, "Miscellaneous", "Page");


        test.assertRootSection_NoBrlCopy()
                .nextChildIs(p -> p
                        .hasText("test1")
                        .isBlockDefaultStyle("Body Text")
                ).nextChildIs(p -> p
                        .hasText("p5")
                        .isBlock(BBX.BLOCK.PAGE_NUM)
                ).nextChildIs(p -> p
                        .hasText("test2")
                        .isBlockDefaultStyle("Body Text")
                ).noNextChild();
    }

    @Test(enabled = false)

    public void pageMakesBlock_inBlock_issue5975() {
        BBTestRunner test = new BBTestRunner("", "<p>test1</p><p>test2</p>");

        test.textViewTools.selectFromTo("1", "1");
        test.openMenuItem(TopMenu.STYLES, "Miscellaneous", "Page");

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(p -> p
                        .nextChildIsText("test")
                        .nextChildIs(span -> span
                                .hasText("1")
                                .isSpan(BBX.SPAN.PAGE_NUM)
                        ).noNextChild()
                        .isBlockDefaultStyle("Body Text")
                ).nextChildIs(p -> p
                        .hasText("test2")
                        .isBlockDefaultStyle("Body Text")
                ).noNextChild();
    }
}
