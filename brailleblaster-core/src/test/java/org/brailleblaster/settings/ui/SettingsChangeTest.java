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
package org.brailleblaster.settings.ui;

import org.brailleblaster.BBIni;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.settings.TableExceptions;
import org.brailleblaster.settings.UTDManager;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.ViewTestRunner;
import org.brailleblaster.utd.PageSettings;
import org.brailleblaster.utd.properties.PageNumberPosition;
import org.brailleblaster.utd.utils.Page;
import org.brailleblaster.utd.config.DocumentUTDConfig;
import org.brailleblaster.utd.config.UTDConfig;
import org.brailleblaster.utils.LengthUtils;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SettingsChangeTest {
    @Test(enabled = false)
    public void changeTranslationTest() {
        BBTestRunner test = new BBTestRunner("", "<p>test</p>");

        // default everything should be empty
        Assert.assertNull(BBIni.getPropertyFileManager().getProperty(UTDManager.USER_SETTINGS_BRAILLE_STANDARD));
        Assert.assertNull(
                DocumentUTDConfig.NIMAS.getSetting(test.getDoc(), UTDManager.USER_SETTINGS_BRAILLE_STANDARD)
        );

        changeBrailleStandard(test, "UEB", "EBAE", BrailleSettingsDialog.SWTBOT_OK_BUTTON);

        // non-apply as default should only apply to document
        Assert.assertNull(BBIni.getPropertyFileManager().getProperty(UTDManager.USER_SETTINGS_BRAILLE_STANDARD));
        Assert.assertEquals(
                DocumentUTDConfig.NIMAS.getSetting(test.getDoc(), UTDManager.USER_SETTINGS_BRAILLE_STANDARD),
                "EBAE"
        );

        // ensure UTD was actually updated, removing exceptions table
        String curTrans = test.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        String curTransNoExceptions = curTrans.substring(0, curTrans.lastIndexOf(','));
        Assert.assertEquals(
                UTDConfig.INSTANCE.loadBrailleSettings(
                        BBIni.loadAutoProgramDataFile(UTDManager.UTD_FOLDER, "EBAE" + UTDManager.BRAILLE_SETTINGS_NAME)
                ).getMainTranslationTable(),
                curTransNoExceptions
        );
        Assert.assertTrue(curTrans.endsWith("EBAE" + TableExceptions.EXCEPTIONS_TABLE_EXTENSION), "Have: " + curTrans);

        // try again with default
        changeBrailleStandard(test, "EBAE", "UEB-UNCONTRACTED", BrailleSettingsDialog.SWTBOT_OK_DEFAULT_BUTTON);

        // non-apply as default should only apply to document
        Assert.assertEquals(
                BBIni.getPropertyFileManager().getProperty(UTDManager.USER_SETTINGS_BRAILLE_STANDARD),
                "UEB-UNCONTRACTED"
        );
        Assert.assertEquals(
                DocumentUTDConfig.NIMAS.getSetting(test.getDoc(), UTDManager.USER_SETTINGS_BRAILLE_STANDARD),
                "UEB-UNCONTRACTED"
        );

        // ensure UTD was actually updated, removing exceptions table
        curTrans = test.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        curTransNoExceptions = curTrans.substring(0, curTrans.lastIndexOf(','));
        Assert.assertEquals(
                UTDConfig.INSTANCE.loadBrailleSettings(
                        BBIni.loadAutoProgramDataFile(UTDManager.UTD_FOLDER, "UEB-UNCONTRACTED" + UTDManager.BRAILLE_SETTINGS_NAME)
                ).getMainTranslationTable(),
                curTransNoExceptions
        );
        Assert.assertTrue(curTrans.endsWith("UEB-UNCONTRACTED" + TableExceptions.EXCEPTIONS_TABLE_EXTENSION), "Have: " + curTrans);
    }

    @Test
    public void changePageSettingsTest() {
        BBTestRunner test = new BBTestRunner("", "<p>test</p>");
        int pageIndex = 2;
        Page expected = Page.STANDARD_PAGES.get(pageIndex);

        test.openMenuItem(TopMenu.SETTINGS, "Page Properties");
        SWTBot settingsBot = test.bot.activeShell().bot();
        settingsBot.comboBoxWithLabel("Page Size").setSelection(pageIndex);
        settingsBot.comboBoxWithLabel("Interpoint").setSelection("Yes");
        ViewTestRunner.doPendingSWTWork();
        settingsBot.buttonWithId(BrailleSettingsDialog.SWTBOT_OK_BUTTON).click();
        ViewTestRunner.doPendingSWTWork();

        PageSettings actual = test.manager.getDocument().getEngine().getPageSettings();
        Assert.assertEquals(actual.getPaperHeight(), expected.getHeight(LengthUtils.Units.MILLIMETRES), 0.0);
        Assert.assertEquals(actual.getPaperWidth(), expected.getWidth(LengthUtils.Units.MILLIMETRES), 0.0);
        Assert.assertEquals(actual.getTopMargin(), expected.getTopMargin(LengthUtils.Units.MILLIMETRES), 0.0);
        Assert.assertEquals(actual.getLeftMargin(), expected.getLeftMargin(LengthUtils.Units.MILLIMETRES), 0.0);
        Assert.assertTrue(actual.getInterpoint());

        PageSettings documentSettings = DocumentUTDConfig.NIMAS.loadPageSettings(test.getDoc());
        Assert.assertEquals(documentSettings.getPaperWidth(), expected.getWidth(LengthUtils.Units.MILLIMETRES), 0.0);
        Assert.assertTrue(documentSettings.getInterpoint());
        ViewTestRunner.forceActiveShellHack();
        ViewTestRunner.doPendingSWTWork();

        test.openMenuItem(TopMenu.SETTINGS, "Page Numbers");
        settingsBot = test.bot.activeShell().bot();
        settingsBot.comboBoxWithLabel("Even Print Page Number").setSelection(PageNumberPosition.TOP_LEFT.name());
        settingsBot.comboBoxWithLabel("Lettered Continuation Pages").setSelection("No");
        settingsBot.comboBoxWithLabel("Continue Braille Pages Across Volumes").setSelection("Yes");
        settingsBot.comboBoxWithLabel("Continuation Indicator For Print Pages").setSelection("Yes");
        settingsBot.comboBoxWithLabel("Guide Words").setSelection("No");
        ViewTestRunner.doPendingSWTWork();
        settingsBot.buttonWithId(BrailleSettingsDialog.SWTBOT_OK_BUTTON).click();
        ViewTestRunner.doPendingSWTWork();

        actual = test.manager.getDocument().getEngine().getPageSettings();
        Assert.assertEquals(actual.getEvenPrintPageNumberAt(), PageNumberPosition.TOP_LEFT);
        Assert.assertFalse(actual.isPrintPageNumberRange());
        Assert.assertTrue(actual.isContinuePages());
        Assert.assertTrue(actual.isPrintPageLetterIndicator());
        Assert.assertFalse(actual.isGuideWords());

        documentSettings = DocumentUTDConfig.NIMAS.loadPageSettings(test.getDoc());
        Assert.assertEquals(documentSettings.getEvenPrintPageNumberAt(), PageNumberPosition.TOP_LEFT);
        Assert.assertFalse(documentSettings.isPrintPageNumberRange());
        Assert.assertFalse(documentSettings.isGuideWords());
    }

    private static void changeBrailleStandard(BBTestRunner test, String currentStd, String newStd, String swtbotButton) {
        test.openMenuItem(TopMenu.SETTINGS, "Translation Settings");

        SWTBot settingsBot = test.bot.activeShell().bot();
        SWTBotCombo standardCombo = settingsBot.comboBoxWithId(TranslationSettingsTab.SWTBOT_STANDARD_COMBO);
        Assert.assertEquals(standardCombo.getText(), currentStd);
        standardCombo.setSelection(newStd);
        ViewTestRunner.doPendingSWTWork();
        settingsBot.buttonWithId(swtbotButton).click();
        ViewTestRunner.doPendingSWTWork();
    }
}
