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
import static org.testng.Assert.assertTrue;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.brailleblaster.TestGroups;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.utd.actions.TransNoteAction;
import org.eclipse.swt.SWT;
//import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.testng.annotations.Test;

@GuiTest
public class BoxlineSelectionTest {
    private final String BOXLINE_TEXT = "----------------------------------------";
    private final String FULL_BOX = "========================================";
    private final String TOP_BOXLINE = "7777777777777777777777777777777777777777";
    private final String BOTTOM_BOXLINE = "gggggggggggggggggggggggggggggggggggggggg";
    private final String TABLE_DOC = "<p>Test 1</p><table><tr><td>Cell 1</td><td>Cell 2</td></tr><tr><td>Cell 3</td><td>Cell 4</td></tr></table><p>Test 2</p>";

    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/printView/BoxLineSelectionTests.bbx");

    //positions cursor at the beginning of a boxline, press a letter key, no change should occur
    @Test(enabled = false)
    public void BoxlineStart_Keypress() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.typeText("F");
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
    }

    //positions cursor at the beginning of a boxline, attempts to paste text, no change should occur
    @Test(enabled = false)
    public void BoxlineStart_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.paste();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
    }

    @Test(enabled = false)
    //positions cursor at the start of a boxline, attempts to delete text, no change should occur
    public void BoxlineStart_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
    }

    @Test(enabled = false)
    //positions cursor at the sart of a boxline, attempts to delete textusing backspace, boxlines linesBefore should be removed
    public void BoxlineStart_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";
        String afterEndBox = "Paragraph 3";
        String afterEndBoxBraille = "  ,p>agraph #c";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnLine(0);
        String resultBraille = bbTest.brailleViewBot.getTextOnLine(0);

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(1);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(1);

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(5);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(5);

        assertEquals(afterEndBox, resultText);
        assertEquals(afterEndBoxBraille, resultBraille);
    }

    @Test(enabled = false)
    //positions cursor at the middle of a boxline, press a letter key, no change should occur
    public void BoxlineMiddle_Keypress() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2) + (bbTest.textViewBot.getTextOnLine(2).length() / 2));
        bbTest.textViewTools.typeText("F");
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 2);
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
    }

    @Test(enabled = false)
    //positions cursor at the middle of a boxline, attempts to paste text, no change should occur
    public void BoxlineMiddle_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight((bbTest.textViewBot.getTextOnLine(2).length() / 2));
        bbTest.paste();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
    }

    @Test(enabled = false)
    //positions cursor at the middle of a boxline, attempts to paste text, no change should occur
    public void BoxlineMiddle_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2) + (bbTest.textViewBot.getTextOnLine(2).length() / 2));
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
    }

    @Test(enabled = false)
    //positions cursor at the middle of a boxline, attempts to delete text using backspace, no change should occur
    public void BoxlineMiddle_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2) + (bbTest.textViewBot.getTextOnLine(2).length() / 2));
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
    }

    @Test(enabled = false)
    //positions cursor at the end of a boxline, press a letter key, no change should occur
    public void BoxlineEnd_Keypress() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) - 1);
        bbTest.textViewTools.typeText("F");
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
    }

    @Test(enabled = false)
    //positions cursor at the end of a boxline, attempts to paste text, no change should occur
    public void BoxlineEnd_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) - 1);
        bbTest.paste();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 2);
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //positions cursor at the end of a boxline, presses delete, linesAfter reduced by 1
    public void BoxlineEnd_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";
        String afterEndBox = "Paragraph 3";
        String afterEndBoxBraille = "  ,p>agraph #c";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(5) - 1);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);

        resultText = bbTest.textViewBot.getTextOnLine(5);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(5);
        assertEquals(afterEndBox, resultText);
        assertEquals(afterEndBoxBraille, resultBraille);
    }

    @Test(enabled = false)
    //positions cursor at the end of a boxline, attempts to delete text using backspace, no change should occur
    public void BoxlineEnd_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";
        String afterEndBox = "Paragraph 3";
        String afterEndBoxBraille = "  ,p>agraph #c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) - 1);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(6);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(6);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(afterEndBox, resultText);
        assertEquals(afterEndBoxBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //selects a boxline, attempts to delete text using delete, no change should occur
    public void Boxline_CompleteSelection_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";
        String afterEndBox = "Paragraph 3";
        String afterEndBoxBraille = "  ,p>agraph #c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnLine(2).length());
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
        //	resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(6);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(6);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(afterEndBox, resultText);
        assertEquals(afterEndBoxBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //selects a boxline, attempts to delete text using delete, no change should occur
    public void Boxline_CompleteSelection_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
        //String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";
        String afterEndBox = "Paragraph 3";
        String afterEndBoxBraille = "  ,p>agraph #c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnLine(2).length());
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
        //expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
        //expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
        //expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
        //expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(6);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(6);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(afterEndBox, resultText);
        assertEquals(afterEndBoxBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //selects a boxline, attempts to delete text using cut, no change should occur
    public void Boxline_CompleteSelection_Cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";
        String afterEndBox = "Paragraph 3";
        String afterEndBoxBraille = "  ,p>agraph #c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnLine(2).length());
        bbTest.cut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
        //	expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(6);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(6);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(afterEndBox, resultText);
        assertEquals(afterEndBoxBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //selects a boxline, attempts to delete text using cut shortcut, no change should occur
    public void Boxline_CompleteSelection_CutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";
        String afterEndBox = "Paragraph 3";
        String afterEndBoxBraille = "  ,p>agraph #c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnLine(2).length());
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(6);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(6);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(afterEndBox, resultText);
        assertEquals(afterEndBoxBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //positions cursor before a boxline, attempts to delete text using delete, line is removed
    public void BeforeBoxline_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";
        String afterEndBox = "Paragraph 3";
        String afterEndBoxBraille = "  ,p>agraph #c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1));
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(1);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(1);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(5);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(5);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(afterEndBox, resultText);
        assertEquals(afterEndBoxBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //positions cursor before a boxline, attempts to delete text using backspace, no change should occur
    public void AfterBoxline_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";
        String afterEndBox = "Paragraph 3";
        String afterEndBoxBraille = "  ,p>agraph #c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(5));
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(5);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(5);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(afterEndBox, resultText);
        assertEquals(afterEndBoxBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //selects from element starty to boxline end, boxline should remain unchanged
    public void StartofElement_Boxline_Edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "P";
        String expectedBraille = "  ;,p";
        //	String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0));
        bbTest.textViewTools.selectRight(53);
        bbTest.textViewTools.typeText("P");
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(groups = {TestGroups.CLIPBOARD_TESTS, TestGroups.TODO_FIX_LATER}, enabled = false)
    // overwriting start boxline issue
    //selects from element start to boxline end, pastes text, boxline should remain unchanged
    public void StartofElement_Boxline_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "P";
        String expectedBraille = "  ;,p";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0));
        bbTest.textViewTools.selectRight(53);
        bbTest.paste();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
        //expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
        //expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
        //expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //deletes first element, top boxline should be unchanged, current formatting has it shorter
    public void StartofElement_Boxline_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String SHORT_BOXLINE_TEXT = "----------------------------------------";
        String SHORT_TOP_BOXLINE = "7777777777777777777777777777777777777777";
//		String expectedTreeItem = "sidebar";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0));
        bbTest.textViewTools.selectRight(53);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        resultText = bbTest.textViewBot.getTextOnCurrentLine();
        resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(SHORT_BOXLINE_TEXT, resultText);
        assertEquals(SHORT_TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(1);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(1);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //deletes first element, top boxline should be unchanged, current formatting has it shorter
    public void StartofElement_Boxline_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String SHORT_BOXLINE_TEXT = "----------------------------------------";
        String SHORT_TOP_BOXLINE = "7777777777777777777777777777777777777777";
//		String expectedTreeItem = "sidebar";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0));
        bbTest.textViewTools.selectRight(53);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        resultText = bbTest.textViewBot.getTextOnCurrentLine();
        resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(SHORT_BOXLINE_TEXT, resultText);
        assertEquals(SHORT_TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(1);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(1);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //deletes first element, top boxline should be unchanged, current formatting has it shorter
    public void StartofElement_Boxline_Cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String SHORT_BOXLINE_TEXT = "----------------------------------------";
        String SHORT_TOP_BOXLINE = "7777777777777777777777777777777777777777";
//		String expectedTreeItem = "sidebar";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0));
        bbTest.textViewTools.selectRight(53);
        bbTest.cut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        resultText = bbTest.textViewBot.getTextOnCurrentLine();
        resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(SHORT_BOXLINE_TEXT, resultText);
        assertEquals(SHORT_TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(1);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(1);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    public void StartofElement_Boxline_CutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String SHORT_BOXLINE_TEXT = "----------------------------------------";
        String SHORT_TOP_BOXLINE = "7777777777777777777777777777777777777777";
//		String expectedTreeItem = "sidebar";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0));
        bbTest.textViewTools.selectRight(53);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        resultText = bbTest.textViewBot.getTextOnCurrentLine();
        resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(SHORT_BOXLINE_TEXT, resultText);
        assertEquals(SHORT_TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(1);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(1);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //selects from inside first element to middle of boxline, boxline should not change
    public void InsideofElement_Boxline_Edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "ParaP";
        String expectedBraille = "  ,p>a~2,p";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 4);
        bbTest.textViewTools.selectRight(49);
        bbTest.textViewTools.typeText("P");
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    //selects from inside first element to middle of boxline, boxline should not change
    public void InsideofElement_Boxline_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "ParaP";
        String expectedBraille = "  ,p>a,p";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 4);
        bbTest.textViewTools.selectRight(49);
        bbTest.paste();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //selects from first element to end of boxline and deletes text, boxline should not change
    public void InsideofElement_Boxline_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Para";
        String expectedBraille = "  ,p>a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 4);
        bbTest.textViewTools.selectRight(49);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //selects from first element to middle of boxline and deletes text, boxline should not change
    public void InsideofElement_Boxline_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Para";
        String expectedBraille = "  ,p>a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 4);
        bbTest.textViewTools.selectRight(49);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //positions cursor before a boxline, attempts to delete text using cut, no change should occur
    public void InsideofElement_Boxline_Cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Para";
        String expectedBraille = "  ,p>a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 4);
        bbTest.textViewTools.selectRight(49);
        bbTest.cut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //positions cursor before a boxline, attempts to delete text using cut shortcut, no change should occur
    public void InsideofElement_Boxline_CutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Para";
        String expectedBraille = "  ,p>a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 4);
        bbTest.textViewTools.selectRight(49);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);	
    }

    @Test(enabled = false)
    public void EndofElement_Boxline_Edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1P";
        String expectedBraille = "  ,p>a~2graph #a,p";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + bbTest.textViewBot.getTextOnLine(0).length());
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnLine(1).length() + 1);
        bbTest.textViewTools.typeText("P");
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(groups = {TestGroups.CLIPBOARD_TESTS, TestGroups.TODO_FIX_LATER}, enabled = false)
    // paste at end of line before a boxline doesn't work
    //positions cursor before a boxline, attempts to delete text using backspace, no change should occur
    public void EndofElement_Boxline_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1P";
        String expectedBraille = "  ,p>a~2graph #a,p";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + bbTest.textViewBot.getTextOnLine(0).length());
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnLine(1).length() + 1);
        bbTest.paste();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //positions cursor before a boxline, attempts to delete text using backspace, no change should occur
    public void EndofElement_Boxline_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + bbTest.textViewBot.getTextOnLine(0).length());
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnLine(1).length() + 2);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //positions cursor before a boxline, attempts to delete text using backspace, no change should occur
    public void EndofElement_Boxline_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + bbTest.textViewBot.getTextOnLine(0).length());
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnLine(1).length() + 2);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //positions cursor before a boxline, attempts to delete text using backspace, no change should occur
    public void EndofElement_Boxline_Cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + bbTest.textViewBot.getTextOnLine(0).length());
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnLine(1).length() + 2);
        bbTest.cut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //positions cursor before a boxline, attempts to delete text using backspace, no change should occur
    public void EndofElement_Boxline_CutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + bbTest.textViewBot.getTextOnLine(0).length());
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnLine(1).length() + 2);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(4);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(4);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(BOTTOM_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void ELementStart_DoubleBoxline_Edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "P";
        String expectedBraille = "  ;,p";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - bbTest.textViewWidget.getOffsetAtLine(6);
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.typeText("P");
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //	bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(9);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(9);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(10);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(10);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void ELementStart_DoubleBoxline_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "P";
        String expectedBraille = "  ;,p";
        //String expectedTreeItem = "p";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

//		treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - bbTest.textViewWidget.getOffsetAtLine(6);
        bbTest.textViewTools.selectRight(length);
        bbTest.paste();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
        //expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(9);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(9);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(10);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(10);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void ELementStart_DoubleBoxline_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
//		String expectedTreeItem = "sidebar";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - bbTest.textViewWidget.getOffsetAtLine(6);
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        //	bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnLine(6);
        String resultBraille = bbTest.brailleViewBot.getTextOnLine(6);
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //	bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
        //	expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(7);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(7);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
        //	expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void ELementStart_DoubleBoxline_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
//		String expectedTreeItem = "sidebar";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - bbTest.textViewWidget.getOffsetAtLine(6);
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

//		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnLine(6);
        String resultBraille = bbTest.brailleViewBot.getTextOnLine(6);
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(7);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(7);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void ELementStart_DoubleBoxline_Cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
//		String expectedTreeItem = "sidebar";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - bbTest.textViewWidget.getOffsetAtLine(6);
        bbTest.textViewTools.selectRight(length);
        bbTest.cut();
        bbTest.updateTextView();

        //	bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnLine(6);
        String resultBraille = bbTest.brailleViewBot.getTextOnLine(6);
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(7);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(7);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void ELementStart_DoubleBoxline_CutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
//		String expectedTreeItem = "sidebar";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - bbTest.textViewWidget.getOffsetAtLine(6);
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

//		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnLine(6);
        String resultBraille = bbTest.brailleViewBot.getTextOnLine(6);
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(7);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(7);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void InsideElement_DoubleBoxline_Edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "ParaP";
        String expectedBraille = "  ,p>a,p";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6) + 4);
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - (bbTest.textViewWidget.getOffsetAtLine(6) + 4);
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.typeText("P");
        bbTest.updateTextView();

//		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnLine(6);
        String resultBraille = bbTest.brailleViewBot.getTextOnLine(6);
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(9);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(9);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(10);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(10);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void InsideELement_DoubleBoxline_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "ParaP";
        String expectedBraille = "  ,p>a,p";
        //	String expectedTreeItem = "p";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

//		treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6) + 4);
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - (bbTest.textViewWidget.getOffsetAtLine(6) + 4);
        bbTest.textViewTools.selectRight(length);
        bbTest.paste();
        bbTest.updateTextView();

//		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnLine(6);
        String resultBraille = bbTest.brailleViewBot.getTextOnLine(6);
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(9);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(9);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(10);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(10);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void InsideElement_DoubleBoxline_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Para";
        String expectedBraille = "  ,p>a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6) + 4);
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - (bbTest.textViewWidget.getOffsetAtLine(6) + 4);
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

//		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnLine(6);
        String resultBraille = bbTest.brailleViewBot.getTextOnLine(6);
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
        //	assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(9);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(9);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(10);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(10);
        //	resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void InsideElement_DoubleBoxline_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Para";
        String expectedBraille = "  ,p>a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6) + 4);
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - (bbTest.textViewWidget.getOffsetAtLine(6) + 4);
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

//		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnLine(6);
        String resultBraille = bbTest.brailleViewBot.getTextOnLine(6);
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(9);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(9);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(10);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(10);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void InsideElement_DoubleBoxline_Cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Para";
        String expectedBraille = "  ,p>a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6) + 4);
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - (bbTest.textViewWidget.getOffsetAtLine(6) + 4);
        bbTest.textViewTools.selectRight(length);
        bbTest.cut();
        bbTest.updateTextView();

//		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnLine(6);
        String resultBraille = bbTest.brailleViewBot.getTextOnLine(6);
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(9);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(9);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(10);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(10);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void InsideElement_DoubleBoxline_CutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Para";
        String expectedBraille = "  ,p>a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6) + 4);
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - (bbTest.textViewWidget.getOffsetAtLine(6) + 4);
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

//		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnLine(6);
        String resultBraille = bbTest.brailleViewBot.getTextOnLine(6);
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(9);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(9);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(10);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(10);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void ElementEnd_DoubleBoxline_Edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 3P";
        String expectedBraille = "  ,p>agraph #c,p";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6) + bbTest.textViewBot.getTextOnLine(6).length());
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - (bbTest.textViewWidget.getOffsetAtLine(6) + bbTest.textViewBot.getTextOnLine(6).length());
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.typeText("P");
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(9);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(9);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(10);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(10);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void ELementEnd_DoubleBoxline_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 3P";
        String expectedBraille = "  ,p>agraph #c,p";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

//		treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6) + bbTest.textViewBot.getTextOnLine(6).length());
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - (bbTest.textViewWidget.getOffsetAtLine(6) + bbTest.textViewBot.getTextOnLine(6).length());
        bbTest.textViewTools.selectRight(length);
        bbTest.paste();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(9);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(9);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(10);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(10);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void ElementEnd_DoubleBoxline_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 3";
        String expectedBraille = "  ,p>agraph #c";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6) + bbTest.textViewBot.getTextOnLine(6).length());
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - (bbTest.textViewWidget.getOffsetAtLine(6) + bbTest.textViewBot.getTextOnLine(6).length());
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(9);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(9);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(10);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(10);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void ELementEnd_DoubleBoxline_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 3";
        String expectedBraille = "  ,p>agraph #c";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6) + bbTest.textViewBot.getTextOnLine(6).length());
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - (bbTest.textViewWidget.getOffsetAtLine(6) + bbTest.textViewBot.getTextOnLine(6).length());
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(9);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(9);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(10);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(10);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void ELementEnd_DoubleBoxline_Cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 3";
        String expectedBraille = "  ,p>agraph #c";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6) + bbTest.textViewBot.getTextOnLine(6).length());
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - (bbTest.textViewWidget.getOffsetAtLine(6) + bbTest.textViewBot.getTextOnLine(6).length());
        bbTest.textViewTools.selectRight(length);
        bbTest.cut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(9);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(9);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(10);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(10);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void ELementEnd_DoubleBoxline_CutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 3";
        String expectedBraille = "  ,p>agraph #c";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "graph 4";
        String brailleAfterBoxline = "  graph #d";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6) + bbTest.textViewBot.getTextOnLine(6).length());
        int length = (bbTest.textViewWidget.getOffsetAtLine(10) + 4) - (bbTest.textViewWidget.getOffsetAtLine(6) + bbTest.textViewBot.getTextOnLine(6).length());
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6));
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(8);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(8);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(FULL_BOX, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(9);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(9);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(10);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(10);
        //	resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void BoxlineStart__Selection_Edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        int length = (bbTest.textViewWidget.getOffsetAtLine(3) - bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.typeText("P");
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void BoxlineStart_Selection_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        int length = (bbTest.textViewWidget.getOffsetAtLine(3) - bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(length);
        bbTest.paste();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void BoxlineStart_Selection_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        int length = (bbTest.textViewWidget.getOffsetAtLine(3) - bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //	bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
        //	resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    public void BoxlineStart_Selection_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        int length = (bbTest.textViewWidget.getOffsetAtLine(3) - bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
        //	expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    public void BoxlineStart_Selection_Cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        int length = (bbTest.textViewWidget.getOffsetAtLine(3) - bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(length);
        bbTest.cut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    public void BoxlineStart_Selection_CutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        int length = (bbTest.textViewWidget.getOffsetAtLine(3) - bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    public void InsideBoxline__Selection_Edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2) + (bbTest.textViewBot.getTextOnLine(2).length() / 2));
        int length = (bbTest.textViewWidget.getOffsetAtLine(3) - (bbTest.textViewWidget.getOffsetAtLine(2) + bbTest.textViewBot.getTextOnLine(2).length() / 2));
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.typeText("P");
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void InsideBoxline_Selection_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2) + (bbTest.textViewBot.getTextOnLine(2).length() / 2));
        int length = (bbTest.textViewWidget.getOffsetAtLine(3) - (bbTest.textViewWidget.getOffsetAtLine(2) + bbTest.textViewBot.getTextOnLine(2).length() / 2));
        bbTest.textViewTools.selectRight(length);
        bbTest.paste();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    public void InsideBoxline_Selection_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2) + (bbTest.textViewBot.getTextOnLine(2).length() / 2));
        int length = (bbTest.textViewWidget.getOffsetAtLine(3) - (bbTest.textViewWidget.getOffsetAtLine(2) + bbTest.textViewBot.getTextOnLine(2).length() / 2));
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    public void InsideBoxline_Selection_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2) + (bbTest.textViewBot.getTextOnLine(2).length() / 2));
        int length = (bbTest.textViewWidget.getOffsetAtLine(3) - (bbTest.textViewWidget.getOffsetAtLine(2) + bbTest.textViewBot.getTextOnLine(2).length() / 2));
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    public void InsideBoxline_Selection_Cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2) + (bbTest.textViewBot.getTextOnLine(2).length() / 2));
        int length = (bbTest.textViewWidget.getOffsetAtLine(3) - (bbTest.textViewWidget.getOffsetAtLine(2) + bbTest.textViewBot.getTextOnLine(2).length() / 2));
        bbTest.textViewTools.selectRight(length);
        bbTest.cut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
///        expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void InsideBoxline_Selection_CutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2) + (bbTest.textViewBot.getTextOnLine(2).length() / 2));
        int length = (bbTest.textViewWidget.getOffsetAtLine(3) - (bbTest.textViewWidget.getOffsetAtLine(2) + bbTest.textViewBot.getTextOnLine(2).length() / 2));
        bbTest.textViewTools.selectRight(length);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void BoxlineEnd__Selection_Edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(1);
        bbTest.textViewTools.typeText("P");
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void BoxlineEnd_Selection_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(1);
        bbTest.paste();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    public void BoxlineEnd_Selection_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(1);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

//		bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    public void BoxlineEnd_Selection_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(1);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    public void BoxlineEnd_Selection_Cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(1);
        bbTest.cut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    //begins in a paragraph and tests that consecutive boxlines are not altered
    public void BoxlineEnd_Selection_CutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>a~2graph #a";
//		String expectedTreeItem = "p";
        String textAfterBoxline = "Paragraph 2";
        String brailleAfterBoxline = "  ,p>agraph #b";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(1);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0) + 1);
        String resultText = bbTest.textViewBot.getTextOnCurrentLine();
        String resultBraille = bbTest.brailleViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();

        assertEquals(expectedText, resultText);
        assertEquals(expectedBraille, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 2);
//		expectedTreeItem = "sidebar";
        resultText = bbTest.textViewBot.getTextOnLine(2);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(2);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(BOXLINE_TEXT, resultText);
        assertEquals(TOP_BOXLINE, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);

        //bbTest.textViewTools.pressKey( SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
        resultText = bbTest.textViewBot.getTextOnLine(3);
        resultBraille = bbTest.brailleViewBot.getTextOnLine(3);
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        assertEquals(textAfterBoxline, resultText);
        assertEquals(brailleAfterBoxline, resultBraille);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    @Test(enabled = false)
    public void color_box() {
        BBTestRunner bb = new BBTestRunner(TestXMLUtils.generateBookDoc("",
                "<p>" + "one" + "</p>" +
                        "<p>" + "two" + "</p>" +
                        "<p>" + "three" + "</p>"));
        bb.textViewTools.navigateToLine(1);
        bb.findMenu("Styles", true).menu("Miscellaneous").menu("Boxes").menu("Color Box").click();
        bb.activeShell().bot().comboBox(0).setText("blue");
        bb.activeShell().bot().button("OK").click();
        String s1 = bb.textViewBot.getTextOnLine(2);
        String s2 = bb.textViewBot.getTextOnLine(3);
        String s3 = bb.textViewBot.getTextOnLine(4);
        assertEquals(s1, TransNoteAction.START + "blue" + TransNoteAction.END + " " + StringUtils.repeat('-', TOP_BOXLINE.substring(7).length()));
        assertEquals(s2, "two");
        assertEquals(s3, StringUtils.repeat('-', BOTTOM_BOXLINE.length()));
    }

    @Test(enabled = false)
    public void deleteBox() {
        BBTestRunner bbTest = new BBTestRunner(TestXMLUtils.generateBookDoc("", "<p>Box here</p>"));

        bbTest.textViewTools.navigate(0);
        bbTest.openMenuItem(TopMenu.STYLES, "Miscellaneous", "Boxes", "Box");

        bbTest.textViewTools.navigateToText("Box here");
        bbTest.textViewTools.navigateToEndOfLine();
        bbTest.textViewTools.pressKey(SWT.BS, "Box here".length() + 1);
        bbTest.updateTextView();

        assertTrue(bbTest.textViewWidget.getText().isEmpty());
    }

    @Test(enabled = false)
    public void rt6553_applyToTextAndTable() {
        BBTestRunner bbTest = new BBTestRunner("", TABLE_DOC);

        bbTest.textViewTools.selectToEndOf("Cell 4");
        bbTest.openMenuItem(TopMenu.STYLES, "Miscellaneous", "Boxes", "Box");

        bbTest.assertRootSection_NoBrlCopy()
                .nextChildIs(child ->
                        child.isContainer(BBX.CONTAINER.BOX)
                                .nextChildIs(child2 -> child2.isBlock(BBX.BLOCK.DEFAULT))
                                .nextChildIs(child2 -> child2.isContainer(BBX.CONTAINER.TABLE))
                                .nextChildIs(child2 -> child2.isContainer(BBX.CONTAINER.TABLE)))
                .nextChildIs(child -> child.isBlock(BBX.BLOCK.DEFAULT))
                .noNextChild();
    }

    @Test(enabled = false)
    public void rt6553_applyToTableAndText() {
        BBTestRunner bbTest = new BBTestRunner("", TABLE_DOC);

        bbTest.textViewTools.navigateToText("Cell 1");
        bbTest.textViewTools.selectToEndOf("Test 2");
        bbTest.openMenuItem(TopMenu.STYLES, "Miscellaneous", "Boxes", "Box");

        bbTest.assertRootSection_NoBrlCopy()
                .nextChildIs(child -> child.isBlock(BBX.BLOCK.DEFAULT))
                .nextChildIs(child ->
                        child.isContainer(BBX.CONTAINER.BOX)
                                .nextChildIs(child2 -> child2.isContainer(BBX.CONTAINER.TABLE))
                                .nextChildIs(child2 -> child2.isContainer(BBX.CONTAINER.TABLE))
                                .nextChildIs(child2 -> child2.isBlock(BBX.BLOCK.DEFAULT)))
                .noNextChild();
    }

    @Test(enabled = false)
    public void rt6553_applyToTextAndTableAndText() {
        BBTestRunner bbTest = new BBTestRunner("", TABLE_DOC);

        bbTest.textViewTools.selectToEndOf("Test 2");
        bbTest.openMenuItem(TopMenu.STYLES, "Miscellaneous", "Boxes", "Box");

        bbTest.assertRootSection_NoBrlCopy()
                .nextChildIs(child ->
                        child.isContainer(BBX.CONTAINER.BOX)
                                .nextChildIs(child2 -> child2.isBlock(BBX.BLOCK.DEFAULT))
                                .nextChildIs(child2 -> child2.isContainer(BBX.CONTAINER.TABLE))
                                .nextChildIs(child2 -> child2.isContainer(BBX.CONTAINER.TABLE))
                                .nextChildIs(child2 -> child2.isBlock(BBX.BLOCK.DEFAULT)))
                .noNextChild();
    }
}
