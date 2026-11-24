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
package org.brailleblaster.perspectives.braille.ui;

import org.brailleblaster.GuiTest;
import org.brailleblaster.testrunners.BBTestRunner;
import org.testng.annotations.Test;

@GuiTest
public class MathMLTest {
    /**
     * This was a style view bug but should be tested in the text view
     */
    @Test(enabled = false)
    public void arrowIntoMathTextView_issue4430() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>Math: <m:math><m:mn>1</m:mn></m:math></p>");
        bbTest.textViewTools.navigate(bbTest.textViewWidget.getCharCount());
    }
}
