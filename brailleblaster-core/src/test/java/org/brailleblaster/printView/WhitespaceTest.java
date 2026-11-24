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
import static org.testng.Assert.assertEquals;

import java.io.File;

import org.brailleblaster.testrunners.BBTestRunner;
import org.eclipse.swt.SWT;
//import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.testng.annotations.Test;

@GuiTest
public class WhitespaceTest {
    //	private final String XMLTREE = "XML";
    private static final String BLANK_LINE = "";

    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/printView/WhitespaceTests.xml");

//	protected SWTBotTree treeBot;

    //Corey: Commenting this test out because we don't really want this behavior anyway.
    //The most likely situation is that the user wanted to add an additional paragraph
    //before the heading, in which case removing the blank line would not be ideal.
    // Disabled: text typed on the blank line after a heading now becomes its own heading-styled block (te/ indented, blank lines around), not body text; product decision (test_fix.md Phase 7b)
    @Test(enabled = false)
    //inserts text after a heading, no blank should exist after reformat
    public void whitespace_after_heading() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "              ,p>agraph #a";

        String typedText = "test";
        String typedBraille = "  te/";

        String textAfter = "Paragraph 2";
        String brailleAfter = "  ,p>agraph #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1));
        bbTest.textViewTools.typeText("test");
        bbTest.updateTextView();

        assertEquals(textBefore, bbTest.textViewBot.getTextOnLine(0));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnLine(0));

        assertEquals(typedText, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(typedBraille, bbTest.brailleViewBot.getTextOnLine(1));

        assertEquals(textAfter, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(2));
    }

    @Test
    //presses delete after heading, no blank line should exist after reformat
    public void whitespace_deleteBlank_after_heading() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "              ,p>agraph #a";

        String textAfter = "Paragraph 2";
        String brailleAfter = "  ,p>agraph #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1));
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        assertEquals(textBefore, bbTest.textViewBot.getTextOnLine(0));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnLine(0));

        assertEquals(textAfter, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(1));
    }

    @Test
    //presses delete after heading, no blank line should exist after reformat
    public void whitespace_deleteBlank_backspace_after_heading() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "              ,p>agraph #a";

        String textAfter = "Paragraph 2";
        String brailleAfter = "  ,p>agraph #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1));
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        assertEquals(textBefore, bbTest.textViewBot.getTextOnLine(0));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnLine(0));

        assertEquals(textAfter, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(1));
    }

    @Test
    public void enter_after_heading() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "              ,p>agraph #a";

        String textAfter = "Paragraph 2";
        String brailleAfter = "  ,p>agraph #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1));
        bbTest.textViewTools.pressKey(SWT.LF, 1);
        bbTest.updateTextView();

        assertEquals(textBefore, bbTest.textViewBot.getTextOnLine(0));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnLine(0));

        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(BLANK_LINE, bbTest.brailleViewBot.getTextOnLine(1));

        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(BLANK_LINE, bbTest.brailleViewBot.getTextOnLine(2));

        assertEquals(textAfter, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));
    }

    @Test
    public void enter_after_paragraph() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 2";
        String brailleBefore = "  ,p>agraph #b";

        String textAfter = "Paragraph 3";
        String brailleAfter = "  ,p>agraph #c";

        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(2));
        bbTest.textViewTools.pressKey(SWT.LF, 1);
        bbTest.updateTextView();

        assertEquals(textBefore, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnLine(2));

        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(BLANK_LINE, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(textAfter, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test
    public void add_line_after_then_backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);

        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(2));
        bbTest.textViewTools.pressKey(SWT.LF, 1);
        bbTest.updateTextView();
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        assertEquals("Paragraph 2", bbTest.textViewBot.getTextOnLine(2));
        assertEquals("Paragraph 3", bbTest.textViewBot.getTextOnLine(3));
    }

    @Test
    public void add_line_before_then_backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.pressKey(SWT.LF, 1);
        bbTest.updateTextView();
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.pressKey(SWT.BS, 1);

        assertEquals("Paragraph 2", bbTest.textViewBot.getTextOnLine(2));
        assertEquals("Paragraph 3", bbTest.textViewBot.getTextOnLine(3));
    }

    @Test
    public void add_line_atDocumentEnd() {
        BBTestRunner test = new BBTestRunner(TEST_FILE);

        test.textViewTools.navigate(test.textViewWidget.getCharCount());
//		test.textViewTools.pressKey(SWT.CR, 1);
        test.textViewTools.typeText(System.lineSeparator() + "silicon" + System.lineSeparator());
//		test.textViewTools.pressKey(SWT.CR, 1);

        test.assertRootSectionFirst_NoBrlCopy()
                .nextChildIs(block -> block
                        .onlyChildIsText("Paragraph 1")
                ).nextChildIs(block -> block
                        .onlyChildIsText("Paragraph 2")
                ).nextChildIs(block -> block
                        .onlyChildIsText("Paragraph 3")
                ).nextChildIs(block -> block
                        .onlyChildIsText("Paragraph 4")
                ).nextChildIs(block -> block
                        .onlyChildIsText("Paragraph 5")
                ).nextChildIs(block -> block
                        .onlyChildIsText("Paragraph 6")
                ).nextChildIs(block -> block
                        .onlyChildIsText("silicon")
                ).noNextChild();
    }
}
