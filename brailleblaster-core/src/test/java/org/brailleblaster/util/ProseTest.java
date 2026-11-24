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
import java.io.File;

import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.testrunners.BBTestRunner;
import org.eclipse.swt.SWT;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import nu.xom.Node;
import org.brailleblaster.bbx.BBX;
import org.testng.Assert;

import static org.testng.Assert.*;

@GuiTest
public class ProseTest {
    private static final File shortProse = new File("src/test/resources/org/brailleblaster/utd/Prose.xml");
    private static final File proseWithLines = new File("src/test/resources/org/brailleblaster/utd/ProseWithLineNumbers.bbx");
    public ProseBuilder proseBuilder;

    @BeforeClass
    public void classSetup() {
        proseBuilder = new ProseBuilder();
    }

    // Disabled: Tools > Line Number Tools no longer exists (only the i18n string is left) (D14) (test_fix.md Phase 7f, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void testOpenProseTools() {
        BBTestRunner bb = new BBTestRunner(shortProse);
        int origHeight = bb.wpManager.getCurrentPerspective().getToolBar().getHeight();
        bb.openMenuItem(TopMenu.TOOLS, "Line Number Tools");

        assertNotEquals(bb.wpManager.getCurrentPerspective().getToolBar().getHeight(), origHeight);
        origHeight = bb.wpManager.getCurrentPerspective().getToolBar().getHeight();
        //close line number tools
        bb.clickButton("X");
        assertTrue(bb.wpManager.getCurrentPerspective().getToolBar().getHeight() < origHeight);
    }

    // Disabled: Tools > Line Number Tools no longer exists (only the i18n string is left) (D14) (test_fix.md Phase 7f, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void applyProseTagToBlock() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>testing</p>");

        bbTest.textViewTools.navigateToText("testing");
        bbTest.selectBreadcrumbsAncestor(0, BBX.BLOCK::assertIsA);

        bbTest.openMenuItem(TopMenu.TOOLS, "Line Number Tools");
        bbTest.clickButton("Wrap Prose (CTRL + F2)");

        bbTest.textViewTools.navigateToText("testing");
        Assert.assertEquals(bbTest.getStyleCursorAncestor(1).getName(), "Prose");

        int origHeight = bbTest.wpManager.getCurrentPerspective().getToolBar().getHeight();
        //close line number tools
        bbTest.clickButton("X");
        assertTrue(bbTest.wpManager.getCurrentPerspective().getToolBar().getHeight() < origHeight);
    }

    // Disabled: Tools > Line Number Tools no longer exists (only the i18n string is left) (D14) (test_fix.md Phase 7f, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void testInsertLineNumberShortcut() {
        BBTestRunner bb = new BBTestRunner(shortProse);
        bb.openMenuItem(TopMenu.TOOLS, "Line Number Tools");

        Node block = bb.manager.getDoc().query("descendant::text()[contains(., 'A killer wave')]").get(0).getParent();
        bb.textViewTools.navigateToText("A killer wave");
        bb.textViewTools.pressKey(SWT.F2, 1);

        assertTrue(block.toXML().contains("Line Number"));

        int origHeight = bb.wpManager.getCurrentPerspective().getToolBar().getHeight();
        //close line number tools
        bb.clickButton("X");
        assertTrue(bb.wpManager.getCurrentPerspective().getToolBar().getHeight() < origHeight);
    }

    // Disabled: Tools > Line Number Tools no longer exists (only the i18n string is left) (D14) (test_fix.md Phase 7f, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void testInsertLineNumberButton() {
        BBTestRunner bb = new BBTestRunner(shortProse);
        bb.openMenuItem(TopMenu.TOOLS, "Line Number Tools");

        Node block = bb.manager.getDoc().query("descendant::text()[contains(., 'A killer wave')]").get(0).getParent();
        bb.textViewTools.navigateToText("A killer wave");
        bb.clickButton("Insert (F2)");

        assertTrue(block.toXML().contains("Line Number"));

        int origHeight = bb.wpManager.getCurrentPerspective().getToolBar().getHeight();
        //close line number tools
        bb.clickButton("X");
        assertTrue(bb.wpManager.getCurrentPerspective().getToolBar().getHeight() < origHeight);
    }

    // Disabled: Tools > Line Number Tools no longer exists (only the i18n string is left) (D14) (test_fix.md Phase 7f, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void testSpaceBetween() {
        BBTestRunner bb = new BBTestRunner(shortProse);
        bb.openMenuItem(TopMenu.TOOLS, "Line Number Tools");

        bb.textViewTools.navigateToText("A killer wave");
        bb.selectBreadcrumbsAncestor(1, BBX.BLOCK::assertIsA);
        bb.clickButton("Wrap Prose (CTRL + F2)");

        bb.textViewTools.navigateToText("A killer wave");
        bb.textViewTools.pressKey(SWT.F2, 1);
        bb.textViewTools.navigateToText("approaching");
        bb.textViewTools.pressKey(SWT.F2, 1);

        bb.brailleViewTools.navigateToText("approa*+");
        bb.brailleViewTools.selectLeft(3);
        String line = bb.brailleViewBot.getSelection();
        assertEquals(line, "   ");

        int origHeight = bb.wpManager.getCurrentPerspective().getToolBar().getHeight();
        //close line number tools
        bb.clickButton("X");
        assertTrue(bb.wpManager.getCurrentPerspective().getToolBar().getHeight() < origHeight);
    }

    //TODO:Test multiple lines
//	@Test
    public void testMultipleSpaceBetween() {
        BBTestRunner bb = new BBTestRunner(proseWithLines);

        for (int i = 0; i < 75; i++) {
            String line = bb.brailleViewBot.getTextOnLine(i);
            String[] linePieces = line.split("#");
            StringBuilder lineCut = new StringBuilder();
            for (int j = 0; j < linePieces.length - 1; j++) {
                lineCut.append(linePieces[j]);
            }
            line = lineCut.toString();
            line.trim();
            assertFalse(line.contains("   "));
        }
    }
}
