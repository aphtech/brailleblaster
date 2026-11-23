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
package org.brailleblaster.undoRedo;

import org.brailleblaster.GuiTest;
import java.io.File;

import org.brailleblaster.TestGroups;
import org.brailleblaster.testrunners.BBTestRunner;
import org.eclipse.swt.SWT;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@GuiTest
public class UndoSelectionTest {


    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/undoRedo/UndoSelectionTests.xml");

    String line1 = "Paragraph 1";
    String line2 = "Paragraph 2";
    String line3 = "Paragraph 3";
    final String line4 = "Paragraph 4";
    final String line5 = "Paragraph 5";
    final String line6 = "Paragraph 6";
    final String line7 = "Paragraph 7";
    String line8 = "Paragraph 8";
    String line9 = "Paragraph 9";
    String line10 = "Paragraph 10";
    String line11 = "Paragraph 11";

    String braille_line1 = "  ,p>agraph #a";
    String braille_line2 = "  ,p>agraph #b";
    String braille_line3 = "  ,p>a~1graph #c";
    final String braille_line4 = "  ,p>agraph #d";
    final String braille_line5 = "  ,p>agraph #e";
    final String braille_line6 = "  ,p>agraph ~2#f";
    final String braille_line7 = "  ,p>agraph #g";
    String braille_line8 = "  _.,p>agraph #h";
    String braille_line9 = "  ,p>agraph #i";
    String braille_line10 = "  ,p>agraph _.#aj";
    String braille_line11 = "  _.,p>agraph #aa";

    @Test(enabled = false)
    public void two_elements_beginning_to_beginning_edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "t";
        String brailleAfter = "  ;t";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(12);
        bbTest.textViewTools.typeText("t");
        bbTest.updateTextView();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void two_elements_beginning_to_beginning_paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "P";
        String brailleAfter = "  ;,p";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(12);
        bbTest.paste();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void two_elements_beginning_to_beginning_pasteShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "P";
        String brailleAfter = "  ;,p";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(12);
        bbTest.textViewTools.pasteShortcut();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_elements_beginning_to_beginning_delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Paragraph 5";
        String brailleAfter = "  ,p>agraph #e";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(12);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));
    }

    @Test(enabled = false)
    public void two_elements_beginning_to_beginning_backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Paragraph 5";
        String brailleAfter = "  ,p>agraph #e";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(12);
        bbTest.textViewTools.pressKey(SWT.BS, 1);

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));
    }

    @Test(enabled = false)
    public void two_elements_beginning_to_beginning_cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Paragraph 5";
        String brailleAfter = "  ,p>agraph #e";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(12);
        bbTest.cut();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));
    }

    @Test(enabled = false)
    public void two_elements_beginning_to_beginning_cutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Paragraph 5";
        String brailleAfter = "  ,p>agraph #e";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(12);
        bbTest.textViewTools.cutShortCut();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));
    }

    @Test(enabled = false)
    public void two_elements_beginning_to_middle_edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "t";
        String brailleAfter = "  ;t";

        String after2 = "graph 5";
        String brailleAfter2 = "  graph #e";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(16);
        bbTest.textViewTools.typeText("t");

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void two_elements_beginning_to_middle_paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "P";
        String brailleAfter = "  ;,p";

        String after2 = "graph 5";
        String brailleAfter2 = "  graph #e";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(16);
        bbTest.paste();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void two_elements_beginning_to_middle_pasteShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "P";
        String brailleAfter = "  ;,p";

        String after2 = "graph 5";
        String brailleAfter2 = "  graph #e";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(16);
        bbTest.textViewTools.pasteShortcut();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_elements_beginning_to_middle_delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "graph 5";
        String brailleAfter = "  graph #e";
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(16);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));
    }

    @Test(enabled = false)
    public void two_elements_beginning_to_middle_backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "graph 5";
        String brailleAfter = "  graph #e";
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(16);
        bbTest.textViewTools.pressKey(SWT.BS, 1);

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));
    }

    @Test(enabled = false)
    public void two_elements_beginning_to_middle_cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "graph 5";
        String brailleAfter = "  graph #e";
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(16);
        bbTest.cut();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));
    }

    @Test(enabled = false)
    public void two_elements_beginning_to_middle_cutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "graph 5";
        String brailleAfter = "  graph #e";
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(16);
        bbTest.textViewTools.cutShortCut();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));
    }

    @Test(enabled = false)
    public void two_elements_beginning_to_end_edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "t";
        String brailleAfter = "  ;t";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(24);
        bbTest.textViewTools.typeText("t");

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void two_elements_beginning_to_end_paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "P";
        String brailleAfter = "  ;,p";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(23);
        bbTest.paste();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));
    }


    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void two_elements_beginning_to_end_pasteShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "P";
        String brailleAfter = "  ;,p";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(23);
        bbTest.textViewTools.pasteShortcut();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_elements_beginning_to_end_delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(24);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line7, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line7, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(line6, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line7, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line7, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_elements_beginning_to_end_backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(24);
        bbTest.textViewTools.pressKey(SWT.BS, 1);

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line7, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line7, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(line6, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line7, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line7, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_elements_beginning_to_end_cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(24);
        bbTest.cut();

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line7, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line7, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(line6, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line7, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line7, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_elements_beginning_to_end_cutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(24);
        bbTest.textViewTools.cutShortCut();

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line7, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line7, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(line6, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line7, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line7, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_elements_middle_to_start_edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Parat";
        String brailleAfter = "  ,p>at";

        String after2 = "Paragraph 5";
        String brailleAfter2 = "  ,p>agraph #e";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(8);
        bbTest.textViewTools.typeText("t");

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void two_elements_middle_to_start_paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "ParaP";
        String brailleAfter = "  ,p>a,p";

        String after2 = "Paragraph 5";
        String brailleAfter2 = "  ,p>agraph #e";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(8);
        bbTest.paste();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void two_elements_middle_to_start_pasteShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "ParaP";
        String brailleAfter = "  ,p>a,p";

        String after2 = "Paragraph 5";
        String brailleAfter2 = "  ,p>agraph #e";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(8);
        bbTest.textViewTools.pasteShortcut();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_elements_middle_to_start_delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Para";
        String brailleAfter = "  ,p>a";

        String after2 = "Paragraph 5";
        String brailleAfter2 = "  ,p>agraph #e";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(8);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_elements_middle_to_start_backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Para";
        String brailleAfter = "  ,p>a";

        String after2 = "Paragraph 5";
        String brailleAfter2 = "  ,p>agraph #e";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(8);
        bbTest.textViewTools.pressKey(SWT.BS, 1);

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_elements_middle_to_start_cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Para";
        String brailleAfter = "  ,p>a";

        String after2 = "Paragraph 5";
        String brailleAfter2 = "  ,p>agraph #e";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(8);
        bbTest.cut();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_elements_middle_to_start_cutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Para";
        String brailleAfter = "  ,p>a";

        String after2 = "Paragraph 5";
        String brailleAfter2 = "  ,p>agraph #e";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(8);
        bbTest.textViewTools.cutShortCut();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_paragraphs_middle_to_middle_edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Parat";
        String brailleAfter = "  ,p>at";

        String after2 = "graph 5";
        String brailleAfter2 = "  graph #e";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(12);
        bbTest.textViewTools.typeText("t");

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void two_paragraphs_middle_to_middle_paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "ParaP";
        String brailleAfter = "  ,p>a,p";

        String after2 = "graph 5";
        String brailleAfter2 = "  graph #e";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(12);
        bbTest.paste();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void two_paragraphs_middle_to_middle_pasteShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "ParaP";
        String brailleAfter = "  ,p>a,p";

        String after2 = "graph 5";
        String brailleAfter2 = "  graph #e";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(12);
        bbTest.textViewTools.pasteShortcut();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_paragraphs_middle_to_middle_delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Para";
        String brailleAfter = "  ,p>a";

        String after2 = "graph 5";
        String brailleAfter2 = "  graph #e";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(12);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_paragraphs_middle_to_middle_backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Para";
        String brailleAfter = "  ,p>a";

        String after2 = "graph 5";
        String brailleAfter2 = "  graph #e";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(12);
        bbTest.textViewTools.pressKey(SWT.BS, 1);

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_paragraphs_middle_to_middle_cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Para";
        String brailleAfter = "  ,p>a";

        String after2 = "graph 5";
        String brailleAfter2 = "  graph #e";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(12);
        bbTest.cut();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_paragraphs_middle_to_middle_cut_shortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Para";
        String brailleAfter = "  ,p>a";

        String after2 = "graph 5";
        String brailleAfter2 = "  graph #e";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(12);
        bbTest.textViewTools.cutShortCut();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(after2, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_paragraphs_middle_toEnd_edit() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Parat";
        String brailleAfter = "  ,p>at";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(20);
        bbTest.textViewTools.typeText("t");

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void two_paragraphs_middle_toEnd_paste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "ParaP";
        String brailleAfter = "  ,p>a,p";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(20);
        bbTest.paste();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void two_paragraphs_middle_toEnd_pasteShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "ParaP";
        String brailleAfter = "  ,p>a,p";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(20);
        bbTest.textViewTools.pasteShortcut();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_paragraphs_middle_toEnd_delete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Para";
        String brailleAfter = "  ,p>a";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(20);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_paragraphs_middle_toEnd_backspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Para";
        String brailleAfter = "  ,p>a";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(20);
        bbTest.textViewTools.pressKey(SWT.BS, 1);

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_paragraphs_middle_toEnd_cut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Para";
        String brailleAfter = "  ,p>a";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(20);
        bbTest.cut();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));
    }

    @Test(enabled = false)
    public void two_paragraphs_middle_toEnd_cutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String after = "Para";
        String brailleAfter = "  ,p>a";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 4);
        bbTest.textViewTools.selectRight(20);
        bbTest.textViewTools.cutShortCut();

        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));

        undo(bbTest);
        assertEquals(line4, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(braille_line4, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line5, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line5, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(after, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line6, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(braille_line6, bbTest.brailleViewBot.getTextOnLine(4));
    }

    private void undo(BBTestRunner bbTest) {
        bbTest.textViewTools.pressShortcut(SWT.MOD1, 'z');
        //bbTest.doPendingSWTWork();
    }

    private void redo(BBTestRunner bbTest) {
        bbTest.textViewTools.pressShortcut(SWT.MOD1, 'y');
        //bbTest.doPendingSWTWork();
    }
}
