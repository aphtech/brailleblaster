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
package org.brailleblaster;

import org.brailleblaster.bbx.BBX;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.settings.DefaultNimasMaps;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.ViewTestRunner;
import org.brailleblaster.testrunners.XMLElementAssert;
import org.brailleblaster.utils.swt.EasySWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.testng.Assert;

public class TestUtils {

    private final static DefaultNimasMaps maps = new DefaultNimasMaps();

    public static XMLElementAssert getInnerSection(BBTestRunner test) {
        return new XMLElementAssert(test.getDoc().getRootElement(), maps.styleMultiMap())
                .stripUTDAndCopy()
                .child(1)
                .isSection(BBX.SECTION.ROOT);
    }

    public static XMLElementAssert getInnerSection(BBTestRunner test, boolean StyleMap) {
        //TODO: StyleMap attribute ignored?
        return new XMLElementAssert(test.getDoc().getRootElement(), maps.styleMultiMap())
                .stripUTDAndCopy()
                .child(1)
                .isSection(BBX.SECTION.ROOT);
    }

    public static XMLElementAssert getFirstSectionChild(BBTestRunner test) {
        return getInnerSection(test)
                .child(0);
    }

    public static SWTBot refreshReturnActiveBot(BBTestRunner test) {
        ViewTestRunner.doPendingSWTWork();
        ViewTestRunner.forceActiveShellHack();
        return test.activeShell().bot();
    }

    public static void forceShellByName(String s) {
        Shell[] shells = Display.getCurrent().getShells();
        for (Shell shell : shells) {
            if (shell.getText().equals(s)) {
                shell.forceActive();
                break;
            }
        }
    }

    public static void closeNotifyDialog(SWTBot bot) {
        closeNotifyDialog(bot, "Exception");
    }

    public static void closeNotifyDialog(SWTBot bot, String title) {
        ViewTestRunner.doPendingSWTWork();
        ViewTestRunner.forceActiveShellHack();
        SWTBotShell activeShell = bot.activeShell();
        Assert.assertEquals(activeShell.getText(), title);

        activeShell.bot().button(0).click();
        ViewTestRunner.doPendingSWTWork();
    }

    public static void changeSettings(BBTestRunner bb, String translationTable) {
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.activeShell().bot().comboBox(0).setSelection(translationTable);
        bb.activeShell().bot().button(EasySWT.OK_LABEL).click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
    }

    public static void assertException(Class<? extends Throwable> exceptionClazz, Runnable run) {
        try {
            run.run();
        } catch (Exception e) {
            if (!exceptionClazz.isAssignableFrom(e.getClass())) {
                throw new AssertionError("Expected " + exceptionClazz.getName() + " got ", e);
            }
            return;
        }
        throw new AssertionError("Expected " + exceptionClazz.getName() + " got no exception");
    }
}
