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
package org.brailleblaster.util;

import org.brailleblaster.GuiTest;
import static org.testng.Assert.assertTrue;

import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.testng.annotations.Test;

import nu.xom.Document;

@GuiTest
public class CustomToolbarTest {
    private final Document basicFile = TestXMLUtils.generateBookDoc("", "<p>test</p>");

    // Disabled: Tools > Line Number Tools no longer exists (only the i18n string is left) (D14) (test_fix.md Phase 7f, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void addLineTools() {
        BBTestRunner bb = new BBTestRunner(basicFile);
        int origHeight = bb.wpManager.getCurrentPerspective().getToolBar().getHeight();
        bb.openMenuItem(TopMenu.TOOLS, "Line Number Tools");
        assertTrue(bb.wpManager.getCurrentPerspective().getToolBar().getHeight() > origHeight);
    }

    @Test
    public void addTOCTools() {
        BBTestRunner bb = new BBTestRunner(basicFile);
        int origHeight = bb.wpManager.getCurrentPerspective().getToolBar().getHeight();
        bb.openMenuItem(TopMenu.TOOLS, "TOC Builder");
        assertTrue(bb.wpManager.getCurrentPerspective().getToolBar().getHeight() > origHeight);
    }

    // Disabled: Tools > Line Number Tools no longer exists (only the i18n string is left) (D14) (test_fix.md Phase 7f, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void addAndRemoveLineThenTOCTools() {
        BBTestRunner bb = new BBTestRunner(basicFile);
        int origHeight = bb.wpManager.getCurrentPerspective().getToolBar().getHeight();
        bb.openMenuItem(TopMenu.TOOLS, "Line Number Tools");
        assertTrue(bb.wpManager.getCurrentPerspective().getToolBar().getHeight() > origHeight);
        origHeight = bb.wpManager.getCurrentPerspective().getToolBar().getHeight();
        bb.openMenuItem(TopMenu.TOOLS, "TOC Builder");
        assertTrue(bb.wpManager.getCurrentPerspective().getToolBar().getHeight() > origHeight);
        origHeight = bb.wpManager.getCurrentPerspective().getToolBar().getHeight();
        //close line number tools
        bb.clickButton("X");
        assertTrue(bb.wpManager.getCurrentPerspective().getToolBar().getHeight() < origHeight);
        origHeight = bb.wpManager.getCurrentPerspective().getToolBar().getHeight();
        //close toc tools
        bb.clickButton("X");
        assertTrue(bb.wpManager.getCurrentPerspective().getToolBar().getHeight() < origHeight);
    }

    // Disabled: Tools > Line Number Tools no longer exists (only the i18n string is left) (D14) (test_fix.md Phase 7f, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void addAndRemoveTOCThenLineTools() {
        BBTestRunner bb = new BBTestRunner(basicFile);
        int origHeight = bb.wpManager.getCurrentPerspective().getToolBar().getHeight();
        bb.openMenuItem(TopMenu.TOOLS, "TOC Builder");
        assertTrue(bb.wpManager.getCurrentPerspective().getToolBar().getHeight() > origHeight);
        origHeight = bb.wpManager.getCurrentPerspective().getToolBar().getHeight();
        bb.openMenuItem(TopMenu.TOOLS, "Line Number Tools");
        assertTrue(bb.wpManager.getCurrentPerspective().getToolBar().getHeight() > origHeight);
        origHeight = bb.wpManager.getCurrentPerspective().getToolBar().getHeight();
        //close line number tools
        bb.clickButton("X");
        assertTrue(bb.wpManager.getCurrentPerspective().getToolBar().getHeight() < origHeight);
        origHeight = bb.wpManager.getCurrentPerspective().getToolBar().getHeight();
        //close toc tools
        bb.clickButton("X");
        assertTrue(bb.wpManager.getCurrentPerspective().getToolBar().getHeight() < origHeight);
    }
}
