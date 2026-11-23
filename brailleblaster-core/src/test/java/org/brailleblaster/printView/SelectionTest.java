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
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.testrunners.BBTestRunner;
import org.testng.Assert;
import org.testng.annotations.Test;

@GuiTest
public class SelectionTest {
    @Test(enabled = false)
    public void selectEntireList() {
        BBTestRunner test = new BBTestRunner("", "<list><li>first</li><li>second</li></list>");
        test.textViewTools.navigateToText("first");
        test.selectBreadcrumbsAncestor(1, BBX.CONTAINER::isA);

        Assert.assertEquals(test.textViewTools.getSelectionStripped(), "firstsecond");
    }
}
