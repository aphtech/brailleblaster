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
import org.brailleblaster.utils.localization.LocaleHandler;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.ViewTestRunner;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@GuiTest
public class ImagePlaceholderTest {
    private static LocaleHandler localeHandler;

    @BeforeClass
    public static void classSetup() {
        localeHandler = LocaleHandler.getDefault();
    }

    // Disabled: the dialog now requires an image path, which can only be chosen in a native file dialog a test can't drive (test_fix.md Phase 7a)
    @Test(enabled = false)
    public void insertImagePlaceholder_issue4961_issue5014() {
        BBTestRunner bbTest = new BBTestRunner("", "<h2>this is</h2><h1>testing</h1>");

        bbTest.textViewTools.navigateToText("testing");
        bbTest.openMenuItem(TopMenu.INSERT, localeHandler.get("&ImagePlaceholder"));

        SWTBot placeholderBot = bbTest.bot.activeShell().bot();
        placeholderBot.text(0).setText("10");
        ViewTestRunner.doPendingSWTWork();
        // By label: button 0 is now "Select Image", which opens a native file dialog that hangs the test
        placeholderBot.button("Insert").click();
        ViewTestRunner.doPendingSWTWork();

        //Issue 4961: Should not get an exception at this point

        //Issue #5014: Should appear in style pane
        SWTBotStyledText stylePane = bbTest.bot.styledText(0);
        int offset = stylePane.getText().indexOf("Image Placeholder");
        Assert.assertNotEquals(offset, -1);

        int stylePaneStyleLine = stylePane.widget.getLineAtOffset(offset);
        Assert.assertEquals(stylePaneStyleLine, 1);
        Assert.assertEquals(bbTest.textViewWidget.getLine(stylePaneStyleLine), "");

        //Shold also appear correctly in breadcrumbs
        bbTest.textViewTools.navigateToLine(stylePaneStyleLine);
        SWTBotButton breadcrumbsAncestor = bbTest.getBreadcrumbsAncestor(0);
        Assert.assertEquals(breadcrumbsAncestor.getText(), "BLOCK Image Placeholder Skip Lines = 10");
    }
}
