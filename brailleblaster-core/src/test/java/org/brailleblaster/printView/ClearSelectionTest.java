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
import static org.testng.Assert.*;

import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.testrunners.BBTestRunner;
import org.eclipse.swt.SWT;
import org.testng.annotations.Test;

/**
 * Tests whether after a selection is made and a style is changed that the selection is clear and
 * events after occur correctly
 */
@GuiTest
public class ClearSelectionTest {

    @Test
    public void editAfterStyleChange() {
        BBTestRunner bbTest = new BBTestRunner("", "<h1>Heading</h1><p>Line 1</p><p>Line 2</p><p>Line 3</p>");
        String text = "test Line 1";
        String braille = "te/ ,l9e #a";

        bbTest.textViewBot.navigateTo(2, 0);
        bbTest.textViewTools.selectRight(20);
        bbTest.openMenuItem(TopMenu.STYLES, "Lists", "List 1 Level", "L1-3");
        bbTest.textViewTools.typeText("test ");
        bbTest.updateTextView();

        assertEquals(text, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(braille, bbTest.brailleViewBot.getTextOnLine(2));
    }

    @Test
    public void deleteAfterLineChange() {
        BBTestRunner bbTest = new BBTestRunner("", "<h1>Heading</h1><p>Line 1</p><p>Line 2</p><p>Line 3</p>");
        String text = "ine 1";
        String braille = "9e #a";

        bbTest.textViewBot.navigateTo(2, 0);
        bbTest.textViewTools.selectRight(20);
        bbTest.openMenuItem(TopMenu.STYLES, "Lists", "List 1 Level", "L1-3");
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        assertEquals(text, bbTest.textViewBot.getTextOnLine(2));
        assertEquals(braille, bbTest.brailleViewBot.getTextOnLine(2));
    }

    @Test
    public void backspaceAfterLineChange() {
        BBTestRunner bbTest = new BBTestRunner("", "<h1>Heading</h1><p>Line 1</p><p>Line 2</p><p>Line 3</p>");
        String text = "Line 1";
        String braille = ",l9e #a";

        bbTest.textViewBot.navigateTo(2, 0);
        bbTest.textViewTools.selectRight(20);
        bbTest.openMenuItem(TopMenu.STYLES, "Lists", "List 1 Level", "L1-3");
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        assertEquals(text, bbTest.textViewBot.getTextOnLine(1));
        assertEquals(braille, bbTest.brailleViewBot.getTextOnLine(1));
    }
}
