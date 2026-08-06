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
import org.brailleblaster.utd.config.DocumentUTDConfig;
import org.brailleblaster.utd.config.UTDConfig;
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
