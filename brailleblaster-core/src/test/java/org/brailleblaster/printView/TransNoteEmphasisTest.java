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

import org.brailleblaster.TestFiles;
import org.brailleblaster.perspectives.mvc.menu.EmphasisItem;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.testrunners.BBTestRunner;
import org.testng.annotations.Test;

@GuiTest
public class TransNoteEmphasisTest {
    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/printView/TransNoteEmphasisTest.bbx");

    // Disabled: transcriber note emphasis: next paragraph runs onto the same braille line and the indent is gone (R11) (test_fix.md Phase 7d, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void partialElement() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBraille = "  ,hey `.<diddle`.> diddle";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2) + 4);
        bbTest.textViewTools.selectRight(6);
        bbTest.openMenuItem(TopMenu.EMPHASIS, EmphasisItem.TNSYMBOLS.longName);

        assertEquals(bbTest.brailleViewBot.getTextOnLine(2), expectedBraille);
    }

    // Disabled: transcriber note emphasis: next paragraph runs onto the same braille line and the indent is gone (R11) (test_fix.md Phase 7d, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void wholeElement() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBraille = "  `.<,hey diddle diddle`.>";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(17);
        bbTest.openMenuItem(TopMenu.EMPHASIS, EmphasisItem.TNSYMBOLS.longName);

        assertEquals(bbTest.brailleViewBot.getTextOnLine(2), expectedBraille);
    }

    // Disabled: transcriber note emphasis: next paragraph runs onto the same braille line and the indent is gone (R11) (test_fix.md Phase 7d, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void multipleElements() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBraille1 = "  `.<,hey diddle diddle";
        String expectedBraille2 = "  ,! cat & ! fiddle`.>";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        bbTest.textViewTools.selectRight(40);
        bbTest.openMenuItem(TopMenu.EMPHASIS, EmphasisItem.TNSYMBOLS.longName);

        assertEquals(bbTest.brailleViewBot.getTextOnLine(2), expectedBraille1);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(3), expectedBraille2);
    }

    // Disabled: transcriber note emphasis: next paragraph runs onto the same braille line and the indent is gone (R11) (test_fix.md Phase 7d, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void multipleSplitElements() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedBraille1 = "  ,he`.<y diddle diddle";
        String expectedBraille2 = "  ,! cat & ! fidd`.>le";

        bbTest.textViewTools.selectFromTo("y", "fidd");
        bbTest.openMenuItem(TopMenu.EMPHASIS, EmphasisItem.TNSYMBOLS.longName);

        assertEquals(bbTest.brailleViewBot.getTextOnLine(2), expectedBraille1);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(3), expectedBraille2);
    }

}
