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

import org.brailleblaster.TestGroups;

import org.brailleblaster.testrunners.BBTestRunner;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.testng.annotations.Test;

@Test(groups = TestGroups.BROKEN_TESTS)
@GuiTest
public class UndoStylesTest {
    private final static String BLANK_LINE = "";

    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/undoRedo/UndoStylesTests.xml");

    protected SWTBotTree treeBot;

    @Test
    public void undo_redo_heading() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Second Paragraph on Page 2";
        String expectedBraille = "  ,second ,p>agraph on ,page #b";
        String expectedBrailleHeading = "     ,second ,p>agraph on ,page #b";

        String line2 = "Page 2 paragraph";
        String brailleLine2 = "  ,page #b p>agraph";

        String line4 = "3rd paragraph on Page 2";
        String brailleLine4 = "  #crd p>agraph on ,page #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.selectToolbarOption("Centered Heading");

        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(BLANK_LINE, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(expectedBrailleHeading, bbTest.brailleViewBot.getTextOnLine(4));

        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnLine(5));
        assertEquals(BLANK_LINE, bbTest.brailleViewBot.getTextOnLine(5));

        undo(bbTest);
        assertEquals(line2, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(brailleLine2, bbTest.brailleViewBot.getTextOnLine(2));

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(line4, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(brailleLine4, bbTest.brailleViewBot.getTextOnLine(4));

        redo(bbTest);
        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(BLANK_LINE, bbTest.brailleViewBot.getTextOnLine(3));

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(4));
        assertEquals(expectedBrailleHeading, bbTest.brailleViewBot.getTextOnLine(4));

        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnLine(5));
        assertEquals(BLANK_LINE, bbTest.brailleViewBot.getTextOnLine(5));
    }

    @Test
    public void undo_redo_heading_afterPage() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String page = "--------------------------------------2";
        String braillePage = "--------------------------------------#b";
        String expectedText = "A paragraph on page 3";
        String expectedBrailleHeading = "         ,a p>agraph on page #c";
        String expectedBraille = "  ,a p>agraph on page #c";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(7));
        bbTest.selectToolbarOption("Centered Heading");

        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnLine(7));
        assertEquals(BLANK_LINE, bbTest.brailleViewBot.getTextOnLine(7));

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(8));
        assertEquals(expectedBrailleHeading, bbTest.brailleViewBot.getTextOnLine(8));

        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnLine(9));
        assertEquals(BLANK_LINE, bbTest.brailleViewBot.getTextOnLine(9));

        undo(bbTest);
        assertEquals(page, bbTest.textViewBot.getTextOnLine(6));
        assertEquals(braillePage, bbTest.brailleViewBot.getTextOnLine(6));

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(7));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(7));

        redo(bbTest);
        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnLine(7));
        assertEquals(BLANK_LINE, bbTest.brailleViewBot.getTextOnLine(7));

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(8));
        assertEquals(expectedBrailleHeading, bbTest.brailleViewBot.getTextOnLine(8));

        assertEquals(BLANK_LINE, bbTest.textViewBot.getTextOnLine(9));
        assertEquals(BLANK_LINE, bbTest.brailleViewBot.getTextOnLine(9));
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
