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
import org.brailleblaster.testrunners.ViewTestRunner;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@GuiTest
public class UndoEditTest {
    private final static String BLANK_LINE = "";

    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/undoRedo/UndoEditTests.xml");

    @Test(enabled = false)
    public void basicEditsTest() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";
        String textAfter = "Pjaragraph 1";

        bbTest.navigateTextView(1);
        bbTest.textViewTools.typeText("j");

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(enabled = false)
    public void basicDeleteTest() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";
        String textAfter = "Pararaph 1";

        bbTest.navigateTextView(4);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(enabled = false)
    public void basicBackspaceTest() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";
        String textAfter = "Pararaph 1";

        bbTest.navigateTextView(5);
        bbTest.textViewTools.pressKey(SWT.BS, 1);

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(enabled = false)
    public void basicCutTest() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";
        String textAfter = "Pararaph 1";

        bbTest.navigateTextView(4);
        bbTest.textViewTools.selectRight(1);
        bbTest.cut();

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(enabled = false)
    public void basicCutShortcutTest() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";
        String textAfter = "Pararaph 1";

        bbTest.navigateTextView(4);
        bbTest.textViewTools.selectRight(1);
        bbTest.textViewTools.cutShortCut();

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(enabled = false)
    public void multipleCharReplaceTest() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";
        String textAfter = "Parajaph 1";

        bbTest.navigateTextView(4);
        bbTest.textViewTools.selectRight(2);
        bbTest.textViewTools.typeText("j");

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void multipleCharPasteTest() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";
        String textAfter = "Para1aph 1";

        //copy
        bbTest.navigateTextView(10);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(4);
        bbTest.textViewTools.selectRight(2);
        bbTest.paste();

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void multipleCharPasteShortcutTest() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";
        String textAfter = "Para1aph 1";

        //copy
        bbTest.navigateTextView(10);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(4);
        bbTest.textViewTools.selectRight(2);
        bbTest.textViewTools.pasteShortcut();

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(enabled = false)
    public void multipleCharBackspaceTest() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";
        String textAfter = "Paraaph 1";

        bbTest.navigateTextView(4);
        bbTest.textViewTools.selectRight(2);
        bbTest.textViewTools.pressKey(SWT.BS, 1);

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(enabled = false)
    public void multipleCharDeleteTest() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";
        String textAfter = "Paraaph 1";

        bbTest.navigateTextView(4);
        bbTest.textViewTools.selectRight(2);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(enabled = false)
    public void fullReplace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";
        String textAfter = "t";

        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(11);
        bbTest.textViewTools.typeText("t");

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void fullPaste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";
        String textAfter = "P";

        //copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(11);
        bbTest.paste();

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void fullPasteShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";
        String textAfter = "P";

        //copy
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(11);
        bbTest.textViewTools.pasteShortcut();

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(enabled = false)
    public void fullDelete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";

        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(11);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);

        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(enabled = false)
    public void fullBackspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";

        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(11);
        bbTest.textViewTools.pressKey(SWT.BS, 1);

        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(groups = TestGroups.TODO_FIX_LATER, enabled = false) // Cut removes entire line instead of leaving blank
    public void fullCut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";

        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(11);
        bbTest.cut();

        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(groups = TestGroups.TODO_FIX_LATER, enabled = false) // Cut removes entire line instead of leaving blank
    public void fullCutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";

        bbTest.navigateTextView(0);
        bbTest.textViewTools.selectRight(11);
        bbTest.textViewTools.cutShortCut();

        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(enabled = false)
    public void multiLineReplace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 5 contains a long string of text that \ncovers multiples lines in the document";
        String brailleBefore = "  ,p>agraph #e 3ta9s a l;g /r+ ( text t \ncov}s multiples l9es 9 ! docu;t";

        String textAfter = "Paragraph 5 contains t in the document";
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4) + 21);
        bbTest.textViewTools.selectRight(50);
        bbTest.textViewTools.typeText("t");

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine() + "\n" + bbTest.textViewBot.getTextOnLine(5));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine() + "\n" + bbTest.brailleViewBot.getTextOnLine(5));

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine() + "\n" + bbTest.textViewBot.getTextOnLine(5));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine() + "\n" + bbTest.brailleViewBot.getTextOnLine(5));
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void multiLinePaste() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 5 contains a long string of text that \ncovers multiples lines in the document";
        String brailleBefore = "  ,p>agraph #e 3ta9s a l;g /r+ ( text t \ncov}s multiples l9es 9 ! docu;t";

        String textAfter = "Paragraph 5 contains t in the document";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4) + 15);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4) + 21);
        bbTest.textViewTools.selectRight(50);
        bbTest.paste();

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine() + "\n" + bbTest.textViewBot.getTextOnLine(5));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine() + "\n" + bbTest.brailleViewBot.getTextOnLine(5));

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine() + "\n" + bbTest.textViewBot.getTextOnLine(5));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine() + "\n" + bbTest.brailleViewBot.getTextOnLine(5));
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void multiLinePasteShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 5 contains a long string of text that \ncovers multiples lines in the document";
        String brailleBefore = "  ,p>agraph #e 3ta9s a l;g /r+ ( text t \ncov}s multiples l9es 9 ! docu;t";

        String textAfter = "Paragraph 5 contains t in the document";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4) + 15);
        bbTest.textViewTools.selectRight(1);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4) + 21);
        bbTest.textViewTools.selectRight(50);
        bbTest.textViewTools.pasteShortcut();

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine() + "\n" + bbTest.textViewBot.getTextOnLine(5));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine() + "\n" + bbTest.brailleViewBot.getTextOnLine(5));

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine() + "\n" + bbTest.textViewBot.getTextOnLine(5));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine() + "\n" + bbTest.brailleViewBot.getTextOnLine(5));
    }

    @Test(enabled = false)
    public void multiLineDelete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 5 contains a long string of text that \ncovers multiples lines in the document";
        String brailleBefore = "  ,p>agraph #e 3ta9s a l;g /r+ ( text t \ncov}s multiples l9es 9 ! docu;t";

        String textAfter = "Paragraph 5 contains  in the document";
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4) + 21);
        bbTest.textViewTools.selectRight(50);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine() + "\n" + bbTest.textViewBot.getTextOnLine(5));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine() + "\n" + bbTest.brailleViewBot.getTextOnLine(5));

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine() + "\n" + bbTest.textViewBot.getTextOnLine(5));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine() + "\n" + bbTest.brailleViewBot.getTextOnLine(5));
    }

    @Test(enabled = false)
    public void multiLineBackspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 5 contains a long string of text that \ncovers multiples lines in the document";
        String brailleBefore = "  ,p>agraph #e 3ta9s a l;g /r+ ( text t \ncov}s multiples l9es 9 ! docu;t";

        String textAfter = "Paragraph 5 contains  in the document";
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4) + 21);
        bbTest.textViewTools.selectRight(50);
        bbTest.textViewTools.pressKey(SWT.BS, 1);

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine() + "\n" + bbTest.textViewBot.getTextOnLine(5));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine() + "\n" + bbTest.brailleViewBot.getTextOnLine(5));

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine() + "\n" + bbTest.textViewBot.getTextOnLine(5));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine() + "\n" + bbTest.brailleViewBot.getTextOnLine(5));
    }

    @Test(enabled = false)
    public void multiLineCut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 5 contains a long string of text that \ncovers multiples lines in the document";
        String brailleBefore = "  ,p>agraph #e 3ta9s a l;g /r+ ( text t \ncov}s multiples l9es 9 ! docu;t";

        String textAfter = "Paragraph 5 contains  in the document";
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4) + 21);
        bbTest.textViewTools.selectRight(50);
        bbTest.cut();

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine() + "\n" + bbTest.textViewBot.getTextOnLine(5));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine() + "\n" + bbTest.brailleViewBot.getTextOnLine(5));

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine() + "\n" + bbTest.textViewBot.getTextOnLine(5));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine() + "\n" + bbTest.brailleViewBot.getTextOnLine(5));
    }

    @Test(enabled = false)
    public void multiLineCutShortcut() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 5 contains a long string of text that \ncovers multiples lines in the document";
        String brailleBefore = "  ,p>agraph #e 3ta9s a l;g /r+ ( text t \ncov}s multiples l9es 9 ! docu;t";

        String textAfter = "Paragraph 5 contains  in the document";
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4) + 21);
        bbTest.textViewTools.selectRight(50);
        bbTest.textViewTools.cutShortCut();

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine() + "\n" + bbTest.textViewBot.getTextOnLine(5));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine() + "\n" + bbTest.brailleViewBot.getTextOnLine(5));

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine() + "\n" + bbTest.textViewBot.getTextOnLine(5));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine() + "\n" + bbTest.brailleViewBot.getTextOnLine(5));
    }

    @Test(enabled = false)
    public void addspace_undo_redo() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";
        String textAfter = "Paragraph 1 ";

        bbTest.navigateTextView(11);
        bbTest.textViewTools.typeText(" ");

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(enabled = false)
    public void addword_end_undo_redo() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";
        String textAfter = "Paragraph 1 here";

        bbTest.navigateTextView(11);
        bbTest.textViewTools.typeText(" here");

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(enabled = false)
    public void addword_beginning_undo_redo() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";
        String textAfter = "A Paragraph 1";

        bbTest.navigateTextView(0);
        bbTest.textViewTools.typeText("A ");

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    @Test(enabled = false)
    public void edit_plus_multiple_words_undo_redo() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String brailleBefore = "  ,p>agraph #a";
        String textAfter = "Paragraphs including 1";

        bbTest.navigateTextView(9);
        bbTest.textViewTools.typeText("s including");

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }

    //TODO: inspect undo delete
	/*
	@Test(enabled = false)
	public void delete_multiple_words(){
		BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
		String textBefore = "Paragraph 5 contains a long string of text that \ncovers multiples lines in the document";
		String brailleBefore = "  ,p>agraph #e 3ta9s a l;g /r+ ( text t \ncov}s multiples l9es 9 ! docu;t";
		
		String textAfter = "Paragraph 5 string of text that ";
		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4) + 12);
		bbTest.pressKey(bbTest.textViewBot, SWT.DEL, 16);
		assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

		for(int i = 0; i < 16; i++)
			undo(bbTest);
		
		assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine() + "\n" + bbTest.textViewBot.getTextOnLine(5));
		assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine() + "\n" + bbTest.brailleViewBot.getTextOnLine(5));
	}
	*/

    @Test(enabled = false)
    public void delete_selection_multiple_words() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 5 contains a long string of text that \ncovers multiples lines in the document";
        String brailleBefore = "  ,p>agraph #e 3ta9s a l;g /r+ ( text t \ncov}s multiples l9es 9 ! docu;t";
        String textAfter = "Paragraph 5 string of text that ";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4) + 12);
        bbTest.textViewTools.selectRight(16);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine() + "\n" + bbTest.textViewBot.getTextOnLine(5));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine() + "\n" + bbTest.brailleViewBot.getTextOnLine(5));

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine() + "\n" + bbTest.textViewBot.getTextOnLine(5));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine() + "\n" + bbTest.brailleViewBot.getTextOnLine(5));
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void paste_atEnd() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String textAfter = "Paragraph 1 contains a long string of text that";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4) + 11);
        bbTest.textViewTools.selectRight(37);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(11);
        bbTest.paste();

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void paste_atStart() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String textAfter = "contains a long string of text that Paragraph 1";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4) + 11);
        bbTest.textViewTools.selectRight(37);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(1);
        bbTest.textViewTools.pressKey(SWT.ARROW_LEFT, 1);
        bbTest.paste();

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void paste_Middle() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Paragraph 1";
        String textAfter = "Paragraph contains a long string of text that  1";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4) + 11);
        bbTest.textViewTools.selectRight(37);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(9);
        bbTest.paste();

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
    }

    //cuts a word then types a new word checks that a blank occurs when hitting undo, then old word when hitting undo again
    @Test(enabled = false)
    public void blank_between_edits() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String text = "Paragraph 1 simple test";
        String braille = "  ,p>agraph #a simple te/";

        String text_after_cut = "Paragraph 1 test";
        String text_after_edit = "Paragraph 1 basic test";

        bbTest.navigateTextView(11);
        bbTest.textViewTools.typeText(" simple test");
        bbTest.updateTextView();

        assertEquals(text, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(braille, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.navigateTextView(12);
        bbTest.textViewTools.selectRight(7);
        bbTest.cut();
        bbTest.textViewTools.typeText("basic ");

        assertEquals(text_after_edit, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(text_after_cut, bbTest.textViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(text, bbTest.textViewBot.getTextOnCurrentLine());
    }

    @Test(enabled = false)
    public void rt6245_undoTablePageBreak() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>Test</p><table><tr><td>Head 1</td><td>Head 2</td></tr><tr><td>Cell 1</td><td>Cell 2</td></tr></table>");

        bbTest.textViewTools.navigateToLine(1);
        bbTest.textViewTools.pressShortcut(Keystrokes.CTRL, Keystrokes.CR);
        bbTest.updateTextView();

        assertEquals("Test", bbTest.textViewWidget.getLine(0));
        assertEquals("", bbTest.textViewWidget.getLine(1));
        assertEquals("", bbTest.textViewWidget.getLine(2));

        undo(bbTest);

        bbTest.textViewTools.pressShortcut(Keystrokes.F5);
        ViewTestRunner.doPendingSWTWork();
        bbTest.updateViewReferences();

        assertEquals("Test", bbTest.textViewWidget.getLine(0));
        assertEquals("", bbTest.textViewWidget.getLine(1));
        assertFalse(bbTest.textViewWidget.getLine(2).isEmpty());
    }

    public static void undo(BBTestRunner bbTest) {
        bbTest.textViewTools.pressShortcut(SWT.MOD1, 'z');
    }

    public static void redo(BBTestRunner bbTest) {
        bbTest.textViewTools.pressShortcut(SWT.MOD1, 'y');
    }
}
