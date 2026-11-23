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
import org.brailleblaster.testrunners.BBViewTestRunner;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.testng.annotations.Test;

@GuiTest
public class UndoSplitTest {

    protected static SWTBot bot;
    protected SWTBotStyledText textBot, brailleBot;
    protected SWTBotTree treeBot;

    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/undoRedo/UndoSplitTests.xml");

    @Test(enabled = false)
    public void splitBlockElement() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);

        String textBefore = "Second Paragraph on Page";
        String brailleBefore = "  ,second ,p>agraph on ,page";

        String textAfter = "Second Para";
        String brailleAfter = "  ,second ,p>a";

        String textAfter2 = "graph on Page";
        String brailleAfter2 = "  graph on ,page";


        bbTest.navigateTextView(37);
        bbTest.textViewTools.pressKey(SWT.LF, 1);
        //TextEditingTests.navigateTo(textBot, 37);
        //TextEditingTests.pressKey(textBot, SWT.LF, 1);

        assertEquals(textAfter, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(textAfter2, bbTest.textViewBot.getTextOnLine(2));

        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(1));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(2));

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnLine(1));

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(textAfter2, bbTest.textViewBot.getTextOnLine(2));

        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(1));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(2));
    }

    @Test(enabled = false)
    public void splitInlineElement() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);

        String textBefore = "First paragraph on page 1";
        String brailleBefore = "  ,f/ p>~1agraph on page #a";

        String textAfter = "First parag";
        String brailleAfter = "  ,f/ p>~1ag";

        String textAfter2 = "raph on page 1";
        String brailleAfter2 = "  ~1raph on page #a";

        bbTest.navigateTextView(11);
        bbTest.textViewTools.pressKey(SWT.LF, 1);

        assertEquals(textAfter, bbTest.textViewBot.getTextOnLine(0));
        assertEquals(textAfter2, bbTest.textViewBot.getTextOnLine(1));

        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(0));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(1));

        undo(bbTest);
        assertEquals(textBefore, bbTest.textViewBot.getTextOnLine(0));
        assertEquals(brailleBefore, bbTest.brailleViewBot.getTextOnLine(0));

        redo(bbTest);
        assertEquals(textAfter, bbTest.textViewBot.getTextOnLine(0));
        assertEquals(textAfter2, bbTest.textViewBot.getTextOnLine(1));

        assertEquals(brailleAfter, bbTest.brailleViewBot.getTextOnLine(0));
        assertEquals(brailleAfter2, bbTest.brailleViewBot.getTextOnLine(1));
    }

    private void undo(BBTestRunner bbTest) {
        bbTest.textViewBot.pressShortcut(SWT.MOD1, 'z');
        BBViewTestRunner.doPendingSWTWork();
    }

    private void redo(BBTestRunner bbTest) {
        bbTest.textViewBot.pressShortcut(SWT.MOD1, 'y');
        BBViewTestRunner.doPendingSWTWork();
    }
}
