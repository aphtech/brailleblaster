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
package org.brailleblaster.perspectives.mvc;

import org.brailleblaster.GuiTest;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.perspectives.mvc.modules.misc.ToggleViewsModule;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.ViewTestRunner;
import org.testng.Assert;
import org.testng.annotations.Test;

@GuiTest
public class ViewManagerTest {

    @Test(enabled = false)
    public void visibleViewHideShowWindow() {
        BBTestRunner test = new BBTestRunner("", "<p>test</p>");

        assertWindowedViewClosed(test);

        test.openMenuItem(TopMenu.WINDOW, ToggleViewsModule.WINDOWIZE_TITLE, "Print");

        test.wpManager.getShell().setFocus();
        ViewTestRunner.doPendingSWTWork();

        assertWindowedViewOpen(test);

        test.openMenuItem(TopMenu.WINDOW, ToggleViewsModule.WINDOWIZE_TITLE, "Print");

        assertWindowedViewClosed(test);
    }

    @Test(enabled = false)
    public void visibleViewShowHideWindow() {
        BBTestRunner test = new BBTestRunner("", "<p>test</p>", () -> ViewManager.setWindowedView(ToggleViewsModule.Views.PRINT));

        focusMainShell(test);
        assertWindowedViewOpen(test);

        test.openMenuItem(TopMenu.WINDOW, ToggleViewsModule.WINDOWIZE_TITLE, "Print");

        focusMainShell(test);
        assertWindowedViewClosed(test);

        test.openMenuItem(TopMenu.WINDOW, ToggleViewsModule.WINDOWIZE_TITLE, "Print");

        assertWindowedViewOpen(test);
    }

    @Test(enabled = false)
    public void invisibleViewShowHideWindow() {
        BBTestRunner test = new BBTestRunner("", "<p>test</p>");
        focusMainShell(test);
        assertWindowedViewClosed(test);
        _doInvisible(test);
    }

    @Test(enabled = false)
    public void invisibleViewHideShowWindow() {
        BBTestRunner test = new BBTestRunner("", "<p>test</p>", () -> ViewManager.setWindowedView(ToggleViewsModule.Views.PRINT));
        focusMainShell(test);
        assertWindowedViewOpen(test);
        focusMainShell(test);
        _doInvisible(test);
    }

    private void _doInvisible(BBTestRunner test) {
        test.openMenuItem(TopMenu.WINDOW, ToggleViewsModule.TOGGLE_SUBMENU_TITLE, "Print");

        test.openMenuItem(TopMenu.WINDOW, ToggleViewsModule.WINDOWIZE_TITLE, "Print");

        focusMainShell(test);
        assertWindowedViewClosed(test);

        test.openMenuItem(TopMenu.WINDOW, ToggleViewsModule.WINDOWIZE_TITLE, "Print");

        assertWindowedViewClosed(test);
    }

    @Test(enabled = false)
    public void showWindowInvisibleVisible() {
        BBTestRunner test = new BBTestRunner("", "<p>test</p>");

        assertWindowedViewClosed(test);

        test.openMenuItem(TopMenu.WINDOW, ToggleViewsModule.WINDOWIZE_TITLE, "Print");

        focusMainShell(test);
        assertWindowedViewOpen(test);

        test.openMenuItem(TopMenu.WINDOW, ToggleViewsModule.TOGGLE_SUBMENU_TITLE, "Print");

        assertWindowedViewClosed(test);

        test.openMenuItem(TopMenu.WINDOW, ToggleViewsModule.WINDOWIZE_TITLE, "Print");

        assertWindowedViewClosed(test);
    }

    @Test(enabled = false)
    public void showWindowSwitchToInvisible() {
        BBTestRunner test = new BBTestRunner("", "<p>test</p>");

        test.openMenuItem(TopMenu.WINDOW, ToggleViewsModule.WINDOWIZE_TITLE, "Print");

        focusMainShell(test);
        assertWindowedViewOpen(test);

        test.openMenuItem(TopMenu.WINDOW, ToggleViewsModule.TOGGLE_SUBMENU_TITLE, "Braille");

        test.openMenuItem(TopMenu.WINDOW, ToggleViewsModule.WINDOWIZE_TITLE, "Braille");

        assertWindowedViewClosed(test);
    }

    @Test(enabled = false)
    public void invisibleHideWindowSwitchToVisible() {
        BBTestRunner test = new BBTestRunner("", "<p>test</p>");

        test.openMenuItem(TopMenu.WINDOW, ToggleViewsModule.WINDOWIZE_TITLE, "Print");

        focusMainShell(test);
        assertWindowedViewOpen(test);

        test.openMenuItem(TopMenu.WINDOW, ToggleViewsModule.TOGGLE_SUBMENU_TITLE, "Print");

        focusMainShell(test);
        assertWindowedViewClosed(test);

        test.openMenuItem(TopMenu.WINDOW, ToggleViewsModule.WINDOWIZE_TITLE, "Braille");

        assertWindowedViewOpen(test);
    }

    private static void assertWindowedViewOpen(BBTestRunner test) {
        Assert.assertEquals(test.manager.getViewManager().mainShellViewCount(), 2, "unexpected number of views in main shell");
        Assert.assertEquals(test.bot.shells().length, 2, "unexpected number of shells");
    }

    private static void assertWindowedViewClosed(BBTestRunner test) {
        Assert.assertEquals(test.manager.getViewManager().mainShellViewCount(), 3, "unexpected number of views in main shell");
        Assert.assertEquals(test.bot.shells().length, 1, "unexpected number of shells");
    }

    private static void focusMainShell(BBTestRunner test) {
        test.wpManager.getShell().setFocus();
        ViewTestRunner.doPendingSWTWork();
    }
}
