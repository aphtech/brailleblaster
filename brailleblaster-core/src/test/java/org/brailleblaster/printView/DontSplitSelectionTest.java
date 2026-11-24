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
import java.io.File;

import org.brailleblaster.bbx.BBX;
import org.brailleblaster.testrunners.BBTestRunner;
import org.eclipse.swt.SWT;
import org.testng.annotations.Test;

//Tests over bugs that resulted from style options which wrap in a container
//and issues that arose from wrapping not occurring correctly

@GuiTest
public class DontSplitSelectionTest {
    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/printView/dontSplitSelectionTest.bbx");

    @Test
    public void inlineElementSelection() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);

        bbTest.textViewTools.navigateToLine(2);
        int length = (bbTest.textViewWidget.getOffsetAtLine(4) + bbTest.textViewWidget.getLine(4).length()) - bbTest.textViewWidget.getCaretOffset();
        bbTest.textViewTools.selectRight(length);

        bbTest.selectToolbarOption("Don't Split");
        bbTest.assertRootSection_NoBrlCopy().child(0).child(1).isContainer(BBX.CONTAINER.DONT_SPLIT);
    }

    @Test
    public void singleElement() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);

        bbTest.textViewTools.navigateToLine(0);
        bbTest.textViewTools.pressKey(SWT.ARROW_RIGHT, 1);

        bbTest.selectToolbarOption("Don't Split");
        bbTest.assertRootSection_NoBrlCopy().child(0).child(0).isContainer(BBX.CONTAINER.DONT_SPLIT);
    }

    @Test
    public void singleWithInlineElement() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);

        bbTest.textViewTools.navigateToLine(2);
        bbTest.textViewTools.pressKey(SWT.ARROW_RIGHT, 1);

        bbTest.selectToolbarOption("Don't Split");
        bbTest.assertRootSection_NoBrlCopy().child(0).child(1).isContainer(BBX.CONTAINER.DONT_SPLIT);
    }
}
