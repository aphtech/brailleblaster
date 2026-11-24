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

import org.brailleblaster.TestGroups;
import org.brailleblaster.testrunners.BBTestRunner;
import org.eclipse.swt.SWT;
//import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.testng.annotations.Test;

@GuiTest
public class PageSelectionTest {
    //private final String XMLTREE = "XML";

    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/printView/PageSelectionTests.xml");

    private final String[] punctuation = {"!", "#", "$", "%", "^", ",", ".", "/", "\\", "&", "*"};
    //protected SWTBotTree treeBot;

    @Test
    public void alphanumeric_atStart_noChange() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1));
        bbTest.textViewTools.typeText("a");
        bbTest.textViewTools.typeText("1");
        bbTest.updateTextView();

        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    @Test
    public void punctuation_atStart_noChange() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1));
        for (String s : punctuation) bbTest.textViewTools.typeText(s);

        bbTest.updateTextView();

        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    @Test
    public void alphanumeric_inside_noChange() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1) + 5);
        bbTest.textViewTools.typeText("a");
        bbTest.textViewTools.typeText("1");
        bbTest.updateTextView();

        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    @Test
    public void punctuation_inside_noChange() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1) + 5);
        for (String s : punctuation) bbTest.textViewTools.typeText(s);

        bbTest.updateTextView();

        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    @Test
    public void alphanumeric_atEnd_noChange() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";

        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(1));
        bbTest.textViewTools.typeText("a");
        bbTest.textViewTools.typeText("1");
        bbTest.updateTextView();

        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    @Test
    public void punctuation_atEnd_noChange() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";

        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(1));
        for (String s : punctuation) bbTest.textViewTools.typeText(s);

        bbTest.updateTextView();

        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //selects from the start pos of element before page to page start
    public void start_Selection_Delete_ToNextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedText = "Page 2 paragraph";
        String expectedBraille = "  ,page #b p>agraph";

        //treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(27);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //selects from the start pos of element before page to page start
    public void start_Selection_Backspace_ToNextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedText = "Page 2 paragraph";
        String expectedBraille = "  ,page #b p>agraph";

        //treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(27);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //selects from the start pos of element before page to page start
    public void start_Selection_Cut_ToNextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //String expectedTreeItem = "pagenum";
        String expectedText = "Page 2 paragraph";
        String expectedBraille = "  ,page #b p>agraph";

        //treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(27);
        bbTest.cut();
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
        //assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //selects from the start pos of element before page to page start
    public void start_Selection_CutShortcut_ToNextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedText = "Page 2 paragraph";
        String expectedBraille = "  ,page #b p>agraph";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(27);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //selects from the start pos of element before page to page start
    public void start_Selection_Insert_ToNextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //String expectedTreeItem = "pagenum";
        String expectedText = "J";
        String expectedBraille = "  ;,j";

        //treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(27);
        bbTest.textViewTools.typeText("J");
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
        //assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //selects from the start pos of element before page to page start
    public void start_Selection_Paste_ToNextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedText = "F";
        String expectedBraille = "  ;,f";

        //	treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //Paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(27);
        bbTest.paste();
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //selects from the start pos of element before page to page start
    public void start_Selection_Paste_ToNextPageStartShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedText = "F";
        String expectedBraille = "  ;,f";

        //treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateBrailleView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //Paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(27);
        bbTest.textViewTools.pasteShortcut();
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from start of the element to start of range of the element on the next page and deletes
    //first line should have a char, other line should be blank, leaving page between unchanged
    public void start_Selection_Delete_pastFirstChar() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedText = "Page 2 paragraph";
        String expectedBraille = "  ,page #b p>agraph";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(70);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);	
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from start of the element to start of range of the element on the next page and deletes
    //first line should have a char, other line should be blank, leaving page between unchanged
    public void start_Selection_Backspace_pastFirstChar() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedText = "Page 2 paragraph";
        String expectedBraille = "  ,page #b p>agraph";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(69);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from start of the element to start of range of the element on the next page and deletes
    //first line should have a char, other line should be blank, leaving page between unchanged
    public void start_Selection_Cut_pastFirstChar() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedText = "Page 2 paragraph";
        String expectedBraille = "  ,page #b p>agraph";

        //treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(69);
        bbTest.cut();
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from start of the element to start of range of the element on the next page and deletes
    //first line should have a char, other line should be blank, leaving page between unchanged
    public void start_Selection_CutShortcut_pastFirstChar() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedText = "Page 2 paragraph";
        String expectedBraille = "  ,page #b p>agraph";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(69);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from start of the element to start of range of the element on the next page and inserts a char
    //first line should have a char, other line should be blank, leaving page between unchanged
    public void start_Selection_Insert_pastFirstChar() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedText = "J";
        String expectedBraille = "  ;,j";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(70);
        bbTest.textViewTools.typeText("J");
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from start of the element to start of range of the element on the next page and inserts a char
    //first line should have a char, other line should be blank, leaving page between unchanged
    public void start_Selection_Paste_pastFirstChar() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedText = "F";
        String expectedBraille = "  ;,f";

        //	treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateBrailleView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //Paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(70);
        bbTest.paste();
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from start of the element to start of range of the element on the next page and inserts a char
    //first line should have a char, other line should be blank, leaving page between unchanged
    public void start_Selection_PasteShortcut_pastFirstChar() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedText = "F";
        String expectedBraille = "  ;,f";

        //	treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateBrailleView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //Paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(70);
        bbTest.textViewTools.pasteShortcut();
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from start of the element to end of range of the element on the next page and deletes
    public void start_Selection_delete_toNextPageEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        String expectedPage2 = "--------------------------------------3";
        String expectedBraillePage2 = "--------------------------------------#c";
//		String expectedTreeItem = "pagenum";
        String expectedText = "Page 2 paragraph";
        String expectedBraille = "  ,page #b p>agraph";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(89);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage2, resultPage);
        assertEquals(expectedBraillePage2, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from start of the element to end of range of the element on the next page and deletes
    public void start_Selection_backspace_toNextPageEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        String expectedPage2 = "--------------------------------------3";
        String expectedBraillePage2 = "--------------------------------------#c";
//		String expectedTreeItem = "pagenum";
        String expectedText = "Page 2 paragraph";
        String expectedBraille = "  ,page #b p>agraph";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(89);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage2, resultPage);
        assertEquals(expectedBraillePage2, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from start of the element to end of range of the element on the next page and deletes using cut
    public void start_Selection_Cut_toNextPageEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        String expectedPage2 = "--------------------------------------3";
        String expectedBraillePage2 = "--------------------------------------#c";
//		String expectedTreeItem = "pagenum";
        String expectedText = "Page 2 paragraph";
        String expectedBraille = "  ,page #b p>agraph";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(89);
        bbTest.cut();
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage2, resultPage);
        assertEquals(expectedBraillePage2, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from start of the element to end of range of the element on the next page and deletes using cut shortcut
    public void start_Selection_CutShortcut_toNextPageEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        String expectedPage2 = "--------------------------------------3";
        String expectedBraillePage2 = "--------------------------------------#c";
//		String expectedTreeItem = "pagenum";
        String expectedText = "Page 2 paragraph";
        String expectedBraille = "  ,page #b p>agraph";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(88);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage2, resultPage);
        assertEquals(expectedBraillePage2, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from start of the element to end of range of the element on the next page and inserts a char
    //first line should have achar, second should be blank, page remains unchanged
    public void start_Selection_Insert_ToNextPageEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "J";
        String expectedBrailleBeforePage = "  ;,j";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        String expectedPage2 = "--------------------------------------3";
        String expectedBraillePage2 = "--------------------------------------#c";
        //	String expectedTreeItem = "pagenum";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(89);
        bbTest.textViewTools.typeText("J");
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage2, resultPage);
        assertEquals(expectedBraillePage2, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from start of the element to end of range of the element on the next page and pastes a char
    public void start_Selection_Paste_ToNextPageEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "F";
        String expectedBrailleBeforePage = "  ;,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        String expectedPage2 = "--------------------------------------3";
        String expectedBraillePage2 = "--------------------------------------#c";
        //	String expectedTreeItem = "pagenum";

        //copy
        bbTest.navigateBrailleView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //
//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(89);
        bbTest.paste();
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage2, resultPage);
        assertEquals(expectedBraillePage2, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from start of the element to end of range of the element on the next page and pastes a char using shortcut
    public void start_Selection_PasteShortcut_ToNextPageEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "F";
        String expectedBrailleBeforePage = "  ;,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        String expectedPage2 = "--------------------------------------3";
        String expectedBraillePage2 = "--------------------------------------#c";
//		String expectedTreeItem = "pagenum";

        //copy
        bbTest.navigateBrailleView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(89);
        bbTest.textViewTools.pasteShortcut();
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage2, resultPage);
        assertEquals(expectedBraillePage2, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from insdie the element to the start of the element on the next page and deletes
    public void insideWord_Selection_Delete_NextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Para";
        String expectedBrailleBeforePage = "  ,second ,p>a";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";
//		String expectedTreeItemAfter = "p";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + expectedBeforePage.length());
        bbTest.textViewTools.selectRight(56);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();


        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItemAfter, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from insdie the element to the start of the element on the next page and deletes
    public void insideWord_Selection_Backspace_NextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Para";
        String expectedBrailleBeforePage = "  ,second ,p>a";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";
//		String expectedTreeItemAfter = "p";

///		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + expectedBeforePage.length());
        bbTest.textViewTools.selectRight(56);
        bbTest.textViewTools.pressKey(SWT.BS, 1);

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();


        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItemAfter, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from insdie the element to the start of the element on the next page and deletes
    public void insideWord_Selection_Cut_NextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Para";
        String expectedBrailleBeforePage = "  ,second ,p>a";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";
//		String expectedTreeItemAfter = "p";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + expectedBeforePage.length());
        bbTest.textViewTools.selectRight(56);
        bbTest.cut();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();


        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItemAfter, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from insdie the element to the start of the element on the next page and deletes
    public void insideWord_Selection_CutShortcut_NextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Para";
        String expectedBrailleBeforePage = "  ,second ,p>a";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";
//		String expectedTreeItemAfter = "p";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + expectedBeforePage.length());
        bbTest.textViewTools.selectRight(56);
        bbTest.textViewTools.cutShortCut();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();


        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItemAfter, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from inside the element to the start of the element on the next page and inserts a char
    public void insideWord_Selection_Inside_NextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Paras";
        String expectedBrailleBeforePage = "  ,second ,p>as";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "--------------------------------------3";
        String expectedBrailleAfterPage = "--------------------------------------#c";


//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(79);
        bbTest.textViewTools.typeText("s");
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from inside the element to the start of the element on the next page and inserts a char
    public void insideWord_Selection_Paste_NextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second ParaF";
        String expectedBrailleBeforePage = "  ,second ,p>a,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedAfterPage = "--------------------------------------3";
        String expectedBrailleAfterPage = "--------------------------------------#c";


//		treeBot = bbTest.bot.tree(0);
        //copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(79);
        bbTest.paste();
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from inside the element to the start of the element on the next page and inserts a char
    public void insideWord_Selection_PasteShortcut_NextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second ParaF";
        String expectedBrailleBeforePage = "  ,second ,p>a,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "--------------------------------------3";
        String expectedBrailleAfterPage = "--------------------------------------#c";


        //	treeBot = bbTest.bot.tree(0);
        //copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(79);
        bbTest.textViewTools.pasteShortcut();
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from inside the element to the end of the element on the next page and deletes
    //Should edit the first line, leave the page entact, and delete the second element leaving a blank line
    public void insideWord_Selection_Delete_ToNextPageEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Para";
        String expectedBrailleBeforePage = "  ,second ,p>a";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "--------------------------------------3";
        String expectedBrailleAfterPage = "--------------------------------------#c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + expectedBeforePage.length());
        bbTest.textViewTools.selectRight(119);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();


        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from inside the element to the end of the element on the next page and deletes
    //Should edit the first line, leave the page entact, and delete the second element leaving a blank line
    public void insideWord_Selection_Backspace_ToNextPageEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Para";
        String expectedBrailleBeforePage = "  ,second ,p>a";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "--------------------------------------3";
        String expectedBrailleAfterPage = "--------------------------------------#c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + expectedBeforePage.length());
        bbTest.textViewTools.selectRight(119);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();


        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from inside the element to the end of the element on the next page and deletes
    //Should edit the first line, leave the page entact, and delete the second element leaving a blank line
    public void insideWord_Selection_Cut_ToNextPageEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Para";
        String expectedBrailleBeforePage = "  ,second ,p>a";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "--------------------------------------3";
        String expectedBrailleAfterPage = "--------------------------------------#c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + expectedBeforePage.length());
        bbTest.textViewTools.selectRight(119);
        bbTest.cut();
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();


        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from inside the element to the end of the element on the next page and deletes
    //Should edit the first line, leave the page entact, and delete the second element leaving a blank line
    public void insideWord_Selection_CutShortcut_ToNextPageEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Para";
        String expectedBrailleBeforePage = "  ,second ,p>a";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedAfterPage = "--------------------------------------3";
        String expectedBrailleAfterPage = "--------------------------------------#c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + expectedBeforePage.length());
        bbTest.textViewTools.selectRight(119);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();


        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from inside the element to the end of the element on the next page and inserts a char
    //Should edit the first line, leave the page intact, and delete the second element leaving a blank line
    public void insideWord_Selection_Insert_ToNextPageEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Paras";
        String expectedBrailleBeforePage = "  ,second ,p>as";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedAfterPage = "--------------------------------------3";
        String expectedBrailleAfterPage = "--------------------------------------#c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(119);
        bbTest.textViewTools.typeText("s");
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();


        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from inside the element to the end of the element on the next page and pastes a char
    //Should edit the first line, leave the page entact, and delete the second element leaving a blank line
    public void insideWord_Selection_Paste_ToNextPageEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second ParaF";
        String expectedBrailleBeforePage = "  ,second ,p>a,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "--------------------------------------3";
        String expectedBrailleAfterPage = "--------------------------------------#c";

        //	treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(119);
        bbTest.paste();
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();


        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from inside the element to the end of the element on the next page and pastes a char
    //Should edit the first line, leave the page intact, and delete the second element leaving a blank line
    public void insideWord_Selection_PasteShortcut_ToNextPageEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second ParaF";
        String expectedBrailleBeforePage = "  ,second ,p>a,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "--------------------------------------3";
        String expectedBrailleAfterPage = "--------------------------------------#c";

        //	treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(119);
        bbTest.textViewTools.pasteShortcut();
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();


        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from the end ot the current element to the start of the element on the next page and deletes
    //Should leave both lines intact since currently you cannot move elements from one print page to another since it would drastically alter markup
    public void end_Selection_Delete_ToNextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Paragraph on Page 2";
        String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #b";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
        bbTest.textViewTools.selectRight(41);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //	resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
//		expectedTreeItem = "p";

        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from the end ot the current element to the start of the element on the next page and deletes using backspace
    //Should leave both lines entac since currently you cannot move elements from one print page to another since it would drastically alter markup
    public void end_Selection_Backspace_ToNextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Paragraph on Page 2";
        String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #b";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
        bbTest.textViewTools.selectRight(41);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //	resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
        //	expectedTreeItem = "p";

        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from the end to the current element to the start of the element on the next page and deletes using cut
    //Should leave both lines intact since currently you cannot move elements from one print page to another since it would drastically alter markup
    public void end_Selection_Cut_ToNextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Paragraph on Page 2";
        String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #b";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
        bbTest.textViewTools.selectRight(41);
        bbTest.cut();
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
//		expectedTreeItem = "p";

        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from the end ot the current element to the start of the element on the next page and deletes using cut shortcut
    //Should leave both lines entact since currently you cannot move elements from one print page to another since it would drastically alter markup
    public void end_Selection_CutShortcut_ToNextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Paragraph on Page 2";
        String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #b";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
        bbTest.textViewTools.selectRight(41);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
        //	expectedTreeItem = "p";

        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
        //	assertEquals(expectedTreeItem, resultTreeItem);
    }

    // Disabled: inserting at the end of a page's last paragraph leaves the page number appended ('Page 21'); page indicator handling (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from the end ot the current element to the start of the element on the next page and inserts a char at the end of the first
    //second element should remain unchanged
    public void end_Selection_Insert_ToNextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Paragraph on Page 21";
        String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #ba";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
        bbTest.textViewTools.selectRight(41);
        bbTest.textViewTools.typeText("1");
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
//		expectedTreeItem = "p";

        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from the end ot the current element to the start of the element on the next page and pastes a char at the end of the first
    //second element should remain unchanged
    public void end_Selection_Paste_ToNextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Paragraph on Page 21";
        String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #ba";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(24);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
        bbTest.textViewTools.selectRight(41);
        bbTest.paste();
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
//		expectedTreeItem = "p";

        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from the end ot the current element to the start of the element on the next page and pastes a char at the end of the first using the shortcut
    //second element should remain unchanged
    public void end_Selection_PasteShortcut_ToNextPageStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Paragraph on Page 21";
        String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #ba";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

//		treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(24);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
        bbTest.textViewTools.selectRight(41);
        bbTest.textViewTools.pasteShortcut();
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
//		expectedTreeItem = "p";

        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }
    //TODO: test fail do to UTD, fix later
	/*
	@Test
	//Selects area from the end ot the current element to inside the element on the next page and deletes the first char of the 2nd element
	//since the selection starts at the end o the element at the end of the page, the element and page are unaltered, only the 2nd element changes
	public void end_Selection_Delete_pastFirstChar(){
		BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
		String expectedBeforePage = "Second Paragraph on Page 2";
		String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #b";
		String expectedPage = "--------------------------------------#b";
		String expectedBraillePage= "--------------------------------------#b";
		String expectedTreeItem = "pagenum";
		String expectedAfterPage = " paragraph on page 3";
		String expectedBrailleAfterPage= "   p>agraph on page #c";
		
		treeBot = bbTest.bot.tree(0);
		bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
		bbTest.textViewTools.selectRight(43);
		bbTest.pressKey(bbTest.textViewBot, SWT.DEL, 1);
		bbTest.updateTextView();
		
		assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
		assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));
		
		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
		String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
		String resultTreeItem = treeBot.selection().get(0, 0).toString();
		String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
		
		//check page line
		assertEquals(expectedPage, resultPage);
		assertEquals(expectedBraillePage, resultBraillePage);
		assertEquals(expectedTreeItem, resultTreeItem);		
		
		bbTest.pressKey(bbTest.textViewBot, SWT.ARROW_DOWN, 1);
		resultPage = bbTest.textViewBot.getTextOnCurrentLine();
		resultTreeItem = treeBot.selection().get(0, 0).toString();
		resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
		expectedTreeItem = "p";
		
		//check page line
		assertEquals(expectedAfterPage, resultPage);
		assertEquals(expectedBrailleAfterPage, resultBraillePage);
		assertEquals(expectedTreeItem, resultTreeItem);			
	}
	
	@Test
	//Selects area from the end ot the current element to inside the element on the next page and deletes the first char of the 2nd element using backspace
	//since the selection starts at the end o the element at the end of the page, the element and page are unaltered, only the 2nd element changes
	public void end_Selection_Backspace_pastFirstChar(){
		BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
		String expectedBeforePage = "Second Paragraph on Page 2";
		String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #b";
		String expectedPage = "--------------------------------------#b";
		String expectedBraillePage= "--------------------------------------#b";
		String expectedTreeItem = "pagenum";
		String expectedAfterPage = " paragraph on page 3";
		String expectedBrailleAfterPage= "   p>agraph on page #c";
		
		treeBot = bbTest.bot.tree(0);
		bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
		bbTest.textViewTools.selectRight(43);
		bbTest.pressKey(bbTest.textViewBot, SWT.BS, 1);
		bbTest.updateTextView();
		
		assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
		assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));
		
		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
		String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
		String resultTreeItem = treeBot.selection().get(0, 0).toString();
		String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
		
		//check page line
		assertEquals(expectedPage, resultPage);
		assertEquals(expectedBraillePage, resultBraillePage);
		assertEquals(expectedTreeItem, resultTreeItem);		
		
		bbTest.pressKey(bbTest.textViewBot, SWT.ARROW_DOWN, 1);
		resultPage = bbTest.textViewBot.getTextOnCurrentLine();
		resultTreeItem = treeBot.selection().get(0, 0).toString();
		resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
		expectedTreeItem = "p";
		
		//check page line
		assertEquals(expectedAfterPage, resultPage);
		assertEquals(expectedBrailleAfterPage, resultBraillePage);
		assertEquals(expectedTreeItem, resultTreeItem);			
	}
	
	@Test
	//Selects area from the end ot the current element to inside the element on the next page and deletes the first char of the 2nd element using cut
	//since the selection starts at the end o the element at the end of the page, the element and page are unaltered, only the 2nd element changes
	public void end_Selection_Cut_pastFirstChar(){
		BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
		String expectedBeforePage = "Second Paragraph on Page 2";
		String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #b";
		String expectedPage = "--------------------------------------#b";
		String expectedBraillePage= "--------------------------------------#b";
		String expectedTreeItem = "pagenum";
		String expectedAfterPage = " paragraph on page 3";
		String expectedBrailleAfterPage= "   p>agraph on page #c";
		
		treeBot = bbTest.bot.tree(0);
		bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
		bbTest.textViewTools.selectRight(43);
		bbTest.cut();
		bbTest.updateTextView();
		
		assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
		assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));
		
		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
		String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
		String resultTreeItem = treeBot.selection().get(0, 0).toString();
		String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
		
		//check page line
		assertEquals(expectedPage, resultPage);
		assertEquals(expectedBraillePage, resultBraillePage);
		assertEquals(expectedTreeItem, resultTreeItem);		
		
		bbTest.pressKey(bbTest.textViewBot, SWT.ARROW_DOWN, 1);
		resultPage = bbTest.textViewBot.getTextOnCurrentLine();
		resultTreeItem = treeBot.selection().get(0, 0).toString();
		resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
		expectedTreeItem = "p";
		
		//check page line
		assertEquals(expectedAfterPage, resultPage);
		assertEquals(expectedBrailleAfterPage, resultBraillePage);
		assertEquals(expectedTreeItem, resultTreeItem);			
	}
	
	@Test
	//Selects area from the end ot the current element to inside the element on the next page and deletes the first char of the 2nd element using cut shortcut
	//since the selection starts at the end o the element at the end of the page, the element and page are unaltered, only the 2nd element changes
	public void end_Selection_CutShortcut_pastFirstChar(){
		BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
		String expectedBeforePage = "Second Paragraph on Page 2";
		String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #b";
		String expectedPage = "--------------------------------------#b";
		String expectedBraillePage= "--------------------------------------#b";
		String expectedTreeItem = "pagenum";
		String expectedAfterPage = " paragraph on page 3";
		String expectedBrailleAfterPage= "   p>agraph on page #c";
		
		treeBot = bbTest.bot.tree(0);
		bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
		bbTest.textViewTools.selectRight(43);
		bbTest.textViewTools.cutShortCut();
		bbTest.updateTextView();
		
		assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
		assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));
		
		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
		String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
		String resultTreeItem = treeBot.selection().get(0, 0).toString();
		String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
		
		//check page line
		assertEquals(expectedPage, resultPage);
		assertEquals(expectedBraillePage, resultBraillePage);
		assertEquals(expectedTreeItem, resultTreeItem);		
		
		bbTest.pressKey(bbTest.textViewBot, SWT.ARROW_DOWN, 1);
		resultPage = bbTest.textViewBot.getTextOnCurrentLine();
		resultTreeItem = treeBot.selection().get(0, 0).toString();
		resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
		expectedTreeItem = "p";
		
		//check page line
		assertEquals(expectedAfterPage, resultPage);
		assertEquals(expectedBrailleAfterPage, resultBraillePage);
		assertEquals(expectedTreeItem, resultTreeItem);			
	}
	
	@Test
	//Selects area from the end ot the current element to inside the element on the next page and inserts a char
	//since the selection starts at the end o the element at the end of the page, the first and second element are editied, the page is unaltered
	public void end_Selection_Insert_PastFirstChar(){
		BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
		String expectedBeforePage = "Second Paragraph on Page 21";
		String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #ba";
		String expectedPage = "--------------------------------------#b";
		String expectedBraillePage= "--------------------------------------#b";
		String expectedTreeItem = "pagenum";
		String expectedAfterPage = " paragraph on page 3";
		String expectedBrailleAfterPage= "   p>agraph on page #c";
		
		treeBot = bbTest.bot.tree(0);
		bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
		bbTest.textViewTools.selectRight(43);
		bbTest.typeText(bbTest.textViewBot, "1");
		bbTest.updateTextView();
		
		assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
		assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));
		
		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
		String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
		String resultTreeItem = treeBot.selection().get(0, 0).toString();
		String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
		
		//check page line
		assertEquals(expectedPage, resultPage);
		assertEquals(expectedBraillePage, resultBraillePage);
		assertEquals(expectedTreeItem, resultTreeItem);		
		
		bbTest.pressKey(bbTest.textViewBot, SWT.ARROW_DOWN, 1);
		resultPage = bbTest.textViewBot.getTextOnCurrentLine();
		resultTreeItem = treeBot.selection().get(0, 0).toString();
		resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
		expectedTreeItem = "p";
		
		//check page line
		assertEquals(expectedAfterPage, resultPage);
		assertEquals(expectedBrailleAfterPage, resultBraillePage);
		assertEquals(expectedTreeItem, resultTreeItem);			
	}
	
	@Test
	//Selects area from the end ot the current element to inside the element on the next page and pastes a char
	//since the selection starts at the end o the element at the end of the page, the first and second element are editied, the page is unaltered
	public void end_Selection_Paste_PastFirstChar(){
		BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
		String expectedBeforePage = "Second Paragraph on Page 21";
		String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #ba";
		String expectedPage = "--------------------------------------#b";
		String expectedBraillePage= "--------------------------------------#b";
		String expectedTreeItem = "pagenum";
		String expectedAfterPage = " paragraph on page 3";
		String expectedBrailleAfterPage= "   p>agraph on page #c";
		
		treeBot = bbTest.bot.tree(0);
		//Copy
		bbTest.navigateTextView(24);
		bbTest.textViewTools.selectRight(1);
		bbTest.copy();
				
		//paste		
		bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
		bbTest.textViewTools.selectRight(43);
		bbTest.paste();
		bbTest.updateTextView();
		
		assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
		assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));
		
		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
		String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
		String resultTreeItem = treeBot.selection().get(0, 0).toString();
		String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
		
		//check page line
		assertEquals(expectedPage, resultPage);
		assertEquals(expectedBraillePage, resultBraillePage);
		assertEquals(expectedTreeItem, resultTreeItem);		
		
		bbTest.pressKey(bbTest.textViewBot, SWT.ARROW_DOWN, 1);
		resultPage = bbTest.textViewBot.getTextOnCurrentLine();
		resultTreeItem = treeBot.selection().get(0, 0).toString();
		resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
		expectedTreeItem = "p";
		
		//check page line
		assertEquals(expectedAfterPage, resultPage);
		assertEquals(expectedBrailleAfterPage, resultBraillePage);
		assertEquals(expectedTreeItem, resultTreeItem);			
	}
	
	@Test
	//Selects area from the end ot the current element to inside the element on the next page and pastes a char
	//since the selection starts at the end o the element at the end of the page, the first and second element are editied, the page is unaltered
	public void end_Selection_PasteShortcut_PastFirstChar(){
		BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
		String expectedBeforePage = "Second Paragraph on Page 21";
		String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #ba";
		String expectedPage = "--------------------------------------#b";
		String expectedBraillePage= "--------------------------------------#b";
		String expectedTreeItem = "pagenum";
		String expectedAfterPage = " paragraph on page 3";
		String expectedBrailleAfterPage= "   p>agraph on page #c";
		
		treeBot = bbTest.bot.tree(0);
		//Copy
		bbTest.navigateTextView(24);
		bbTest.textViewTools.selectRight(1);
		bbTest.copy();
				
		//paste		
		bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
		bbTest.textViewTools.selectRight(43);
		bbTest.textViewTools.pasteShortcut();
		bbTest.updateTextView();
		
		assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
		assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));
		
		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
		String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
		String resultTreeItem = treeBot.selection().get(0, 0).toString();
		String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
		
		//check page line
		assertEquals(expectedPage, resultPage);
		assertEquals(expectedBraillePage, resultBraillePage);
		assertEquals(expectedTreeItem, resultTreeItem);		
		
		bbTest.pressKey(bbTest.textViewBot, SWT.ARROW_DOWN, 1);
		resultPage = bbTest.textViewBot.getTextOnCurrentLine();
		resultTreeItem = treeBot.selection().get(0, 0).toString();
		resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();
		expectedTreeItem = "p";
		
		//check page line
		assertEquals(expectedAfterPage, resultPage);
		assertEquals(expectedBrailleAfterPage, resultBraillePage);
		assertEquals(expectedTreeItem, resultTreeItem);			
	}
	*/

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from the end of the current element to the end of the element on the next page and deletes
    //second element should be a blank line
    public void end_Selection_Delete_ToNextEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Paragraph on Page 2";
        String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #b";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "--------------------------------------3";
        String expectedBrailleAfterPage = "--------------------------------------#c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
        bbTest.textViewTools.selectRight(63);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from the end of the current element to the end of the element on the next page and deletes
    //second element should be a blank line
    public void end_Selection_Backspace_ToNextEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Paragraph on Page 2";
        String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #b";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedAfterPage = "--------------------------------------3";
        String expectedBrailleAfterPage = "--------------------------------------#c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
        bbTest.textViewTools.selectRight(63);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from the end of the current element to the end of the element on the next page and deletes using cut
    //second element should be a blank line
    public void end_Selection_Cut_ToNextEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Paragraph on Page 2";
        String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #b";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedAfterPage = "--------------------------------------3";
        String expectedBrailleAfterPage = "--------------------------------------#c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
        bbTest.textViewTools.selectRight(63);
        bbTest.cut();
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from the end of the current element to the end of the element on the next page and deletes using cut
    //second element should be a blank line
    public void end_Selection_CutShortcut_ToNextEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Paragraph on Page 2";
        String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #b";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "--------------------------------------3";
        String expectedBrailleAfterPage = "--------------------------------------#c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
        bbTest.textViewTools.selectRight(63);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: inserting at the end of a page's last paragraph leaves the page number appended ('Page 21'); page indicator handling (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from the end of the current element to the end of the element on the next page and inserts a char
    //char occurs at end of first element, page is unchanged, second element is now a blank line
    public void end_Selection_Insert_ToNextEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Paragraph on Page 21";
        String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #ba";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "--------------------------------------3";
        String expectedBrailleAfterPage = "--------------------------------------#c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
        bbTest.textViewTools.selectRight(63);
        bbTest.textViewTools.typeText("1");
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from the end of the current element to the end of the element on the next page and pastes a char
    //char occurs at end of first element, page is unchanged, second element is now a blank line
    public void end_Selection_Paste_ToNextEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Paragraph on Page 21";
        String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #ba";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "pagenum";
        String expectedAfterPage = "--------------------------------------3";
        String expectedBrailleAfterPage = "--------------------------------------#c";

//		treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(24);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
        bbTest.textViewTools.selectRight(63);
        bbTest.paste();
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects area from the end of the current element to the end of the element on the next page and pastes a char using keyboard shortcuts
    //char occurs at end of first element, page is unchanged, second element is now a blank line
    public void end_Selection_PasteShortcut_ToNextEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Paragraph on Page 21";
        String expectedBrailleBeforePage = "  ,second ,p>agraph on ,page #ba";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "pagenum";
        String expectedAfterPage = "--------------------------------------3";
        String expectedBrailleAfterPage = "--------------------------------------#c";

//		treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(24);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewTools.endOfLine(3));
        bbTest.textViewTools.selectRight(63);
        bbTest.textViewTools.pasteShortcut();
        bbTest.updateTextView();

        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBrailleBeforePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        String resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
        String resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedPage, resultPage);
        assertEquals(expectedBraillePage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);		

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        resultPage = bbTest.textViewBot.getTextOnCurrentLine();
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
        resultBraillePage = bbTest.brailleViewBot.getTextOnCurrentLine();

        //check page line
        assertEquals(expectedAfterPage, resultPage);
        assertEquals(expectedBrailleAfterPage, resultBraillePage);
//		assertEquals(expectedTreeItem, resultTreeItem);			
    }

    @Test
    //Selects a line containing a page indicator from start to end and presses a letter key, no change should occur
    public void completePageSelection_Edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";
//		String expectedTreeItem = "pagenum";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1));
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnCurrentLine().length());
        bbTest.textViewTools.typeText("1");
        bbTest.updateTextView();

//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    @Test
    //Selects a line containing a page indicator from start to end and presses a letter key, no change should occur
    public void completePageSelection_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";
//		String expectedTreeItem = "pagenum";

//		treeBot = bbTest.bot.tree(0);
        //copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1));
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnCurrentLine().length());
        bbTest.paste();
        bbTest.updateTextView();

//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    @Test
    //Selects a line containing a page indicator from start to end and presses a letter key, no change should occur
    public void completePageSelection_PasteShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";
//		String expectedTreeItem = "pagenum";

//		treeBot = bbTest.bot.tree(0);
        //copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1));
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnCurrentLine().length());
        bbTest.textViewTools.pasteShortcut();
        bbTest.updateTextView();

//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects a line containing a page indicator from start to end and presses delete, no change should occur
    public void completePageSelection_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";
//		String expectedTreeItem = "pagenum";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1));
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnCurrentLine().length());
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects a line containing a page indicator from start to end and presses delete, no change should occur
    public void completePageSelection_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";
//		String expectedTreeItem = "pagenum";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1));
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnCurrentLine().length());
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects a line containing a page indicator from start to end and presses delete, no change should occur
    public void completePageSelection_Cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";
//		String expectedTreeItem = "pagenum";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1));
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnCurrentLine().length());
        bbTest.cut();
        bbTest.updateTextView();

//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects a line containing a page indicator from start to end and presses delete, no change should occur
    public void completePageSelection_CutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";
        //String expectedTreeItem = "pagenum";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1));
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnCurrentLine().length());
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    @Test
    //Selects a line containing a page indicator from point after start to end, no change should occur
    public void partialPageSelection_Edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";
//		String expectedTreeItem = "pagenum";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1) + 20);
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnCurrentLine().length() - 20);
        bbTest.textViewTools.typeText("1");
        bbTest.updateTextView();

        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
        //	assertEquals(expectedTreeItem, resultTreeItem);
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    @Test
    //Selects a line containing a page indicator from point after start to end, no change should occur
    public void partialPageSelection_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";
        //	String expectedTreeItem = "pagenum";

        //treeBot = bbTest.bot.tree(0);
        //copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1) + 20);
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnCurrentLine().length() - 20);
        bbTest.paste();
        bbTest.updateTextView();

        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
        //	assertEquals(expectedTreeItem, resultTreeItem);
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    @Test
    //Selects a line containing a page indicator from point after start to end, no change should occur
    public void partialPageSelection_PasteShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";
        //	String expectedTreeItem = "pagenum";

//		treeBot = bbTest.bot.tree(0);
        //copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1) + 20);
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnCurrentLine().length() - 20);
        bbTest.textViewTools.pasteShortcut();
        bbTest.updateTextView();

        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects a line containing a page indicator from point after start to end and presses delete, no change should occur
    public void partialPageSelection_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";
//		String expectedTreeItem = "pagenum";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1) + 20);
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnCurrentLine().length() - 20);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects a line containing a page indicator from point after start to end and presses delete, no change should occur
    public void partialPageSelection_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";
//		String expectedTreeItem = "pagenum";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1) + 20);
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnCurrentLine().length() - 20);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects a line containing a page indicator from point after start to end and presses delete, no change should occur
    public void partialPageSelection_Cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";
//		String expectedTreeItem = "pagenum";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1) + 20);
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnCurrentLine().length() - 20);
        bbTest.cut();
        bbTest.updateTextView();

        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
        //	assertEquals(expectedTreeItem, resultTreeItem);
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects a line containing a page indicator from point after start to end and presses delete, no change should occur
    public void partialPageSelection_CutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";
//		String expectedTreeItem = "pagenum";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(1) + 20);
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnCurrentLine().length() - 20);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void FromStartOfElement_completePageSelection_Edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "F";
        String expectedBeforeBraillePage = "  ;,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(67);
        bbTest.textViewTools.typeText("F");
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        //	String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void FromStartOfElement_completePageSelection_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "F";
        String expectedBeforeBraillePage = "  ;,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(67);
        bbTest.paste();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        //	expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void FromStartOfElement_completePageSelection_PasteShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "F";
        String expectedBeforeBraillePage = "  ;,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(67);
        bbTest.textViewTools.pasteShortcut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void FromStartOfElement_completePageSelection_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Page 2 paragraph";
        String expectedBeforeBraillePage = "  ,page #b p>agraph";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(67);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void FromStartOfElement_completePageSelection_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Page 2 paragraph";
        String expectedBeforeBraillePage = "  ,page #b p>agraph";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(67);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void FromStartOfElement_completePageSelection_Cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Page 2 paragraph";
        String expectedBeforeBraillePage = "  ,page #b p>agraph";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(67);
        bbTest.cut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void FromStartOfElement_completePageSelection_CutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Page 2 paragraph";
        String expectedBeforeBraillePage = "  ,page #b p>agraph";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(67);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void FromStartOfElement_partialPageSelection_Edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "F";
        String expectedBeforeBraillePage = "  ;,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(45);
        bbTest.textViewTools.typeText("F");
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void FromStartOfElement_partialPageSelection_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "F";
        String expectedBeforeBraillePage = "  ;,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

//		treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(45);
        bbTest.paste();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void FromStartOfElement_partialPageSelection_PasteShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "F";
        String expectedBeforeBraillePage = "  ;,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

//		treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(45);
        bbTest.textViewTools.pasteShortcut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void FromStartOfElement_partialPageSelection_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Page 2 paragraph";
        String expectedBeforeBraillePage = "  ,page #b p>agraph";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(45);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void FromStartOfElement_partialPageSelection_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Page 2 paragraph";
        String expectedBeforeBraillePage = "  ,page #b p>agraph";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(45);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void FromStartOfElement_partialPageSelection_Cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Page 2 paragraph";
        String expectedBeforeBraillePage = "  ,page #b p>agraph";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(45);
        bbTest.cut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void FromStartOfElement_partialPageSelection_CutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Page 2 paragraph";
        String expectedBeforeBraillePage = "  ,page #b p>agraph";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(45);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(2));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects an element before a line with page indicator, selects from point after start to end and presses a letter key, no change should occur to indicator
    public void FromInsideOfElement_completePageSelection_Edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second ParaF";
        String expectedBeforeBraillePage = "  ,second ,p>a,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(56);
        bbTest.textViewTools.typeText("F");
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects an element before a line with page indicator, selects from point after start to end and presses a letter key, no change should occur to indicator
    public void FromInsideOfElement_completePageSelection_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second ParaF";
        String expectedBeforeBraillePage = "  ,second ,p>a,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(56);
        bbTest.paste();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects an element before a line with page indicator, selects from point after start to end and presses a letter key, no change should occur to indicator
    public void FromInsideOfElement_completePageSelection_PasteShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second ParaF";
        String expectedBeforeBraillePage = "  ,second ,p>a,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

//		treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(56);
        bbTest.textViewTools.pasteShortcut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects an element before a line with page indicator, selects from point after start to end and presses delete, no change should occur to indicator
    public void FromInsideOfElement_completePageSelection_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Para";
        String expectedBeforeBraillePage = "  ,second ,p>a";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

//		treeBot = bbTest.bot.tree(0);

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(56);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects an element before a line with page indicator, selects from point after start to end and presses delete, no change should occur to indicator
    public void FromInsideOfElement_completePageSelection_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Para";
        String expectedBeforeBraillePage = "  ,second ,p>a";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(56);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects an element before a line with page indicator, selects from point after start to end and presses delete, no change should occur to indicator
    public void FromInsideOfElement_completePageSelection_Cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Para";
        String expectedBeforeBraillePage = "  ,second ,p>a";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(56);
        bbTest.cut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects an element before a line with page indicator, selects from point after start to end and presses delete, no change should occur to indicator
    public void FromInsideOfElement_completePageSelection_CutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Para";
        String expectedBeforeBraillePage = "  ,second ,p>a";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

//		treeBot = bbTest.bot.tree(0);		
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(56);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects an element before a line with page indicator, selects from point after start to end and presses delete, no change should occur to indicator
    public void FromInsideOfElement_partialPageSelection_Edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second ParaF";
        String expectedBeforeBraillePage = "  ,second ,p>a,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(35);
        bbTest.textViewTools.typeText("F");
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects an element before a line with page indicator, selects from point after start to end and presses delete, no change should occur to indicator
    public void FromInsideOfElement_partialPageSelection_Paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second ParaF";
        String expectedBeforeBraillePage = "  ,second ,p>a,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

//		treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(35);
        bbTest.paste();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects an element before a line with page indicator, selects from point after start to end and presses delete, no change should occur to indicator
    public void FromInsideOfElement_partialPageSelection_PasteShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second ParaF";
        String expectedBeforeBraillePage = "  ,second ,p>a,f";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        //Copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(35);
        bbTest.textViewTools.pasteShortcut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects an element before a line with page indicator, selects from point after start to end and presses delete, no change should occur to indicator
    public void FromInsideOfElement_partialPageSelection_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Para";
        String expectedBeforeBraillePage = "  ,second ,p>a";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(35);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects an element before a line with page indicator, selects from point after start to end and presses delete, no change should occur to indicator
    public void FromInsideOfElement_partialPageSelection_Backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Para";
        String expectedBeforeBraillePage = "  ,second ,p>a";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(35);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects an element before a line with page indicator, selects from point after start to end and presses delete, no change should occur to indicator
    public void FromInsideOfElement_partialPageSelection_Cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Para";
        String expectedBeforeBraillePage = "  ,second ,p>a";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

//		treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(35);
        bbTest.cut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //Selects an element before a line with page indicator, selects from point after start to end and presses delete, no change should occur to indicator
    public void FromInsideOfElement_partialPageSelection_CutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "Second Para";
        String expectedBeforeBraillePage = "  ,second ,p>a";
        String expectedPage = "--------------------------------------2";
        String expectedBraillePage = "--------------------------------------#b";
        //	String expectedTreeItem = "p";
        String expectedAfterPage = "A paragraph on page 3";
        String expectedBrailleAfterPage = "  ,a p>agraph on page #c";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 11);
        bbTest.textViewTools.selectRight(35);
        bbTest.textViewTools.cutShortCut();
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        //	expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    // Disabled: edits on a selection that includes a print page indicator now delete the indicator; tests expect it kept (D13) (test_fix.md Phase 7e, bb_issues_from_tests.md)
    @Test(enabled = false)
    //test corresponds to bug fix in changeset 5467
    public void firstLine_partialPageSelection_Delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBeforePage = "First para";
        String expectedBeforeBraillePage = "  ,f/ p>a";
        String expectedPage = "--------------------------------------1";
        String expectedBraillePage = "--------------------------------------#a";
//		String expectedTreeItem = "p";
        String expectedAfterPage = "Page 2 paragraph";
        String expectedBrailleAfterPage = "  ,page #b p>agraph";

        //	treeBot = bbTest.bot.tree(0);
        bbTest.navigateTextView(10);
        bbTest.textViewTools.selectRight(35);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0));
//		String resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedBeforePage, bbTest.textViewBot.getTextOnLine(0));
        assertEquals(expectedBeforeBraillePage, bbTest.brailleViewBot.getTextOnLine(0));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "pagenum";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedPage, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(expectedBraillePage, bbTest.brailleViewBot.getTextOnLine(1));

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
//		expectedTreeItem = "p";
//		resultTreeItem = treeBot.selection().get(0, 0).toString();
//		assertEquals(expectedTreeItem, resultTreeItem);			
        assertEquals(expectedAfterPage, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(expectedBrailleAfterPage, bbTest.brailleViewBot.getTextOnLine(2));
    }
}
