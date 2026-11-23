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
public class UndoUpdateTest {

    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/undoRedo/UndoUpdateTests.xml");

    @Test
    public void basic_Undo_Redo_Update() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Second Paragraph on Page 22";
        String expectedBraille = "  ,second ,p>agraph on ,page #bb";
        String expectedText2 = "Second Paragraph on Page 2";
        String expectedBraille2 = "  ,second ,p>agraph on ,page #b";

        // End of line 3; offsetAtLine(4) - 1 assumed a one-character line break
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + bbTest.textViewWidget.getLine(3).length());
        bbTest.textViewTools.typeText("2");
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(3));

        undo(bbTest);
        assertEquals(expectedText2, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille2, bbTest.brailleViewBot.getTextOnLine(3));

        redo(bbTest);
        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(3));
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS)
    public void basic_Undo_Redo__Selection_Update() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Second Paragraph on Paragraph";
        String expectedBraille = "  ,second ,p>agraph on ,p>agraph";
        String expectedText2 = "Second Paragraph on Page 2";
        String expectedBraille2 = "  ,second ,p>agraph on ,page #b";

        //copy
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 7);
        bbTest.textViewTools.selectRight(9);
        bbTest.copy();

        //paste
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 20);
        bbTest.textViewTools.selectRight(7);
        bbTest.paste();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(3));

        undo(bbTest);
        assertEquals(expectedText2, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille2, bbTest.brailleViewBot.getTextOnLine(3));

        redo(bbTest);
        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(3));
    }

    @Test
    public void multiple_undo_redo_update() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "Page 2 paragraph";
        String brailleBefore = "  ,page #b p>agraph";
        String textAfter = "Page 21 paragraph";
        String brailleAfter = "  ,page #ba p>agraph";
        String textBefore2 = "Second Paragraph on Page 2";
        String brailleBefore2 = "  ,second ,p>agraph on ,page #b";
        String textAfter2 = "Second 1Paragraph on Page 2";
        String brailleAfter2 = "  ,second #a,paragraph on ,page #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2) + 6);
        bbTest.textViewTools.typeText("1");
        bbTest.updateTextView();

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnCurrentLine());

        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        bbTest.textViewTools.typeText("1");
        bbTest.updateTextView();

        assertEquals(textAfter2, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnCurrentLine());

        undo(bbTest);
        assertEquals(textBefore2, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleBefore2, bbTest.brailleViewBot.getTextOnLine(3));

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnLine(2));

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(2));

        redo(bbTest);
        assertEquals(textAfter2, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(3));
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
