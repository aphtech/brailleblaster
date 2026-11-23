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
package org.brailleblaster.easierxml;

import org.brailleblaster.GuiTest;
import static org.testng.Assert.assertFalse;

import java.io.File;

import org.brailleblaster.TestGroups;
import org.brailleblaster.testrunners.BBTestRunner;
import org.testng.annotations.Test;

/**
 * StyleView selection and applying styles and actions
 */
@GuiTest
public class StyleViewActionsStylesTest {
    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/easierxml/nimas-test.xml");


    @Test(groups = TestGroups.BROKEN_TESTS) //BBX will destroy small tables
    public void tableNotEnabledOnSimpleTables_issue3702() {
        BBTestRunner bbTest = new BBTestRunner("", "<table><tr><td>Testing 1</td><td>testing 2</td></tr></table>");

        bbTest.navigateTextView(10);

        bbTest.styleViewTools.navigateToText("ting 2");
        assertFalse(bbTest.findMenu("Edit Table at Cursor", true).isEnabled());
    }


}
