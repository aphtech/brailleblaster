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
import static org.testng.Assert.assertEquals;

import java.io.File;

import org.brailleblaster.testrunners.BBTestRunner;
import org.eclipse.swt.SWT;
import org.testng.annotations.Test;

@GuiTest
public class UndoRedoMathTest {

    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/undoRedo/UndoRedoMath.xml");

    @Test(enabled = false)
    public void basicBackspaceTest() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String textBefore = "sqrt x";
        String brailleBefore = "  >x}";
        String textAfter = "srt x";

        bbTest.navigateTextView(2);
        bbTest.textViewTools.pressKey(SWT.BS, 1);

        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        UndoEditTest.undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());

        UndoEditTest.redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnCurrentLine());

        UndoEditTest.undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnCurrentLine());
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnCurrentLine());
    }
}
