/*
 * Copyright (C) 2026 Michael Whapples
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
package org.brailleblaster.perspectives.braille.document;

import org.brailleblaster.perspectives.braille.views.wp.tableEditor.TableEditor;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.utils.localization.LocaleHandler;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.testng.annotations.Test;

import static org.brailleblaster.testrunners.ViewTestRunner.doPendingSWTWork;
import static org.testng.Assert.assertEquals;

public class TableEditorSixKeyTest {
    private static final String TABLE = "<table>"
            + "<tr><td>Cell</td><td>Cell</td><td>Cell</td></tr>"
            + "<tr><td>Cell</td><td>Cell</td><td>Cell</td></tr>"
            + "<tr><td>Cell</td><td>Cell</td><td>Cell</td></tr>"
            + "</table>";
    private static final String SIX_KEY_MODE_LABEL = LocaleHandler.getDefault().get("SixKeyMode.menuItem");

    @Test
    public void sixKeyMenuAndCellInputTrackSharedMode() {
        BBTestRunner test = new BBTestRunner(TestXMLUtils.generateBookDoc("", TABLE));
        test.manager.getText().setSixKeyMode(false);
        test.textViewTools.navigateToText("Cell");

        SWTBotShell dialog = openTableEditor(test);
        SWTBotMenu sixKeyMode = getSixKeyMenu(dialog);
        assertEquals(sixKeyMode.isChecked(), false);

        SWTBotStyledText cell = dialog.bot().styledText(0);
        assertCellInput(cell, false);

        sixKeyMode.click();
        doPendingSWTWork();
        assertEquals(test.manager.getText().getSixKeyMode(), true);
        assertEquals(sixKeyMode.isChecked(), true);
        assertCellInput(cell, true);

        dialog.bot().button(TableEditor.CANCEL_BUTTON).click();
        doPendingSWTWork();

        test.manager.getText().setSixKeyMode(true);
        dialog = openTableEditor(test);
        sixKeyMode = getSixKeyMenu(dialog);
        assertEquals(sixKeyMode.isChecked(), true);

        cell = dialog.bot().styledText(0);
        assertCellInput(cell, true);

        sixKeyMode.click();
        doPendingSWTWork();
        assertEquals(test.manager.getText().getSixKeyMode(), false);
        assertEquals(sixKeyMode.isChecked(), false);
        assertCellInput(cell, false);
    }


    private SWTBotShell openTableEditor(BBTestRunner test) {
        test.openMenuItem(TopMenu.TOOLS, TableEditor.EDIT_MENUITEM);
        return test.bot.activeShell();
    }

    private SWTBotMenu getSixKeyMenu(SWTBotShell dialog) {
        return dialog.bot().menu("Translation").menu(SIX_KEY_MODE_LABEL);
    }

    private void assertCellInput(SWTBotStyledText cell, boolean sixKeyMode) {
        cell.setText("");
        cell.setFocus();
        cell.typeText("f");
        doPendingSWTWork();
        assertEquals(cell.getText(), sixKeyMode ? "\u2801" : "f");
    }
}