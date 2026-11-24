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
package org.brailleblaster.perspectives.mvc.modules.misc;

import org.brailleblaster.GuiTest;
import static org.testng.Assert.assertEquals;

import java.io.File;

import org.brailleblaster.TestFiles;
import org.brailleblaster.TestGroups;
import org.brailleblaster.search.GoToPageDialog.PageType;
import org.brailleblaster.search.GoToPageTest;
import org.brailleblaster.testrunners.BBTestRunner;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.testng.annotations.Test;

@GuiTest
public class ClipboardModuleTest {
    final String textBefore = "sqrt x";
    final String brailleBefore = "  >x}";
    final String textAfter = "rt x";
    final String brailleAfter = "  rtx";
    final String cutText = "sq";

    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/undoRedo/UndoRedoMath.xml");
    private static final File TEST_FILE_2 = new File("src/test/resources/org/brailleblaster/printView/oz.bbx");

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void rt_5842_copy_paste_multi_block() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE_2);
        bbTest.textViewTools.selectRight(bbTest.textViewBot.getTextOnLine(0).length() + System.lineSeparator().length()
                + bbTest.textViewBot.getTextOnLine(1).length());
        String selection = bbTest.textViewTools.getSelectionStripped();
        assertEquals(selection, "We're off to see the wizard.The wonderful wizard of Oz.");
        pressCopy(bbTest);
        bbTest.textViewTools.navigate(bbTest.textViewBot.getTextOnLine(0).length());
        bbTest.textViewTools.pressKey(SWT.CR, 1);
        pressPaste(bbTest);
        selection = bbTest.textViewTools.getTextStripped();
        assertEquals(selection,
                "We're off to see the wizard.We're off to see the wizard.The wonderful wizard of Oz.The wonderful wizard of Oz.We hear he is a whiz of a wiz.");
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void cutMath() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        bbTest.textViewTools.selectRight(2);
        String selection = bbTest.textViewTools.getSelectionStripped();
        assertEquals(selection, cutText);
        pressCut(bbTest);

        assertEquals(bbTest.textViewBot.getTextOnCurrentLine(), textAfter);
        assertEquals(bbTest.brailleViewBot.getTextOnCurrentLine(), brailleAfter);
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void copyMath() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);

        bbTest.textViewTools.selectRight(2);

        pressCopy(bbTest);
        assertEquals(bbTest.textViewBot.getTextOnCurrentLine(), textBefore);
        assertEquals(bbTest.brailleViewBot.getTextOnCurrentLine(), brailleBefore);
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void cutPasteMath() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        bbTest.textViewTools.selectRight(2);

        pressCut(bbTest);
        assertEquals(bbTest.textViewBot.getTextOnCurrentLine(), textAfter);
        assertEquals(bbTest.brailleViewBot.getTextOnCurrentLine(), brailleAfter);

        pressPaste(bbTest);
        assertEquals(bbTest.textViewBot.getTextOnCurrentLine(), textBefore);
        assertEquals(bbTest.brailleViewBot.getTextOnCurrentLine(), brailleBefore);
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void copyPasteMath() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);

        bbTest.textViewTools.selectRight(2);

        pressCopy(bbTest);
        assertEquals(bbTest.textViewBot.getTextOnCurrentLine(), textBefore);
        assertEquals(bbTest.brailleViewBot.getTextOnCurrentLine(), brailleBefore);

        pressPaste(bbTest);
        assertEquals(bbTest.textViewBot.getTextOnCurrentLine(), textBefore);
        assertEquals(bbTest.brailleViewBot.getTextOnCurrentLine(), brailleBefore);
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void copyPasteCutPasteMath() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);

        bbTest.textViewTools.selectRight(2);

        pressCopy(bbTest);
        assertEquals(bbTest.textViewBot.getTextOnCurrentLine(), textBefore);
        assertEquals(bbTest.brailleViewBot.getTextOnCurrentLine(), brailleBefore);

        pressPaste(bbTest);
        assertEquals(bbTest.textViewBot.getTextOnCurrentLine(), textBefore);
        assertEquals(bbTest.brailleViewBot.getTextOnCurrentLine(), brailleBefore);

        bbTest.textViewTools.selectRight(2);

        pressCut(bbTest);
        assertEquals(bbTest.textViewBot.getTextOnCurrentLine(), textAfter);
        assertEquals(bbTest.brailleViewBot.getTextOnCurrentLine(), brailleAfter);

        pressPaste(bbTest);
        assertEquals(bbTest.textViewBot.getTextOnCurrentLine(), textBefore);
        assertEquals(bbTest.brailleViewBot.getTextOnCurrentLine(), brailleBefore);
    }

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void rt6709_clipboard_invalidXmlChars() {
        // TODO: Will have to disable the test check inside ClipboardModule
        BBTestRunner bbTest = new BBTestRunner("", "<p>init</p>");

        String illegalText = "This is \u000C with \u0002 test";

        TextTransfer textTransfer = TextTransfer.getInstance();
        Clipboard clipboard = new Clipboard(Display.getCurrent());
        clipboard.clearContents();
        clipboard.setContents(new String[]{illegalText}, new Transfer[]{textTransfer});
        clipboard.dispose();

        bbTest.textViewTools.pasteShortcut();
    }

    public static void pressCut(BBTestRunner bbTest) {
        bbTest.textViewTools.pressShortcut(SWT.MOD1, 'x');
    }

    public static void pressCopy(BBTestRunner bbTest) {
        bbTest.textViewTools.pressShortcut(SWT.MOD1, 'c');
    }

    public static void pressPaste(BBTestRunner bbTest) {
        bbTest.textViewTools.pressShortcut(SWT.MOD1, 'v');
    }
}
