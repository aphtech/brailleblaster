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
package org.brailleblaster.perspectives.braille.document;

import org.brailleblaster.GuiTest;
import nu.xom.Document;
import org.brailleblaster.TestUtils;
import org.brailleblaster.search.SearchConstants;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


@GuiTest
public class FindReplaceCaseSensitiveTest {
    private static final Logger log = LoggerFactory.getLogger(FindReplaceCaseSensitiveTest.class);
    private final String phraseInitial = "Test a whole phrase";
    private final String phraseSmall = "test a whole phrase";
    private final String phraseCaps = "TEST A WHOLE PHRASE";
    private final String wordInitial = "Testword";
    private final String wordSmall = "testword";
    private final String wordCaps = "TESTWORD";

    private final String controlPhrase = "RepLaced tExt";
    private final String controlWord = "nEwWord";

    private final Document pInitial = TestXMLUtils.generateBookDoc("",
            "<p>" + phraseInitial + "</p>"
    );
    private final Document pSmall = TestXMLUtils.generateBookDoc("",
            "<p>" + phraseSmall + "</p>"
    );
    private final Document pCaps = TestXMLUtils.generateBookDoc("",
            "<p>" + phraseCaps + "</p>"
    );
    private final Document wInitial = TestXMLUtils.generateBookDoc("",
            "<p>" + wordInitial + "</p>"
    );
    private final Document wSmall = TestXMLUtils.generateBookDoc("",
            "<p>" + wordSmall + "</p>"
    );
    private final Document wCaps = TestXMLUtils.generateBookDoc("",
            "<p>" + wordCaps + "</p>"
    );


    @Test(enabled = false)
    public void phraseInitial() {

        /*
         * Test a phrase initial caps
         */
        BBTestRunner bb = setUp(pInitial);

        SWTBot bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText(phraseInitial);
        bot.comboBox(1).setText(controlPhrase);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String text = bb.textViewTools.getSelectionStripped();
        log.debug("Log Replaced Text " + text);
        String replacePhraseInitial = "Replaced text";
        TestUtils.getFirstSectionChild(bb)
                .nextChildIsText(replacePhraseInitial);
        breakDown(bb);
    }

    @Test(enabled = false)
    public void phraseSmall() {
        BBTestRunner bb = setUp(pSmall);
        /*
         * Test a phrase no caps
         */

        SWTBot bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText(phraseSmall);
        bot.comboBox(1).setText(controlPhrase);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String text = bb.textViewTools.getSelectionStripped();
        log.debug("Log Replaced Text " + text);
        String replacePhraseSmall = "replaced text";
        TestUtils.getFirstSectionChild(bb)
                .nextChildIsText(replacePhraseSmall);
        breakDown(bb);
    }

    @Test(enabled = false)
    public void phraseCaps() {
        BBTestRunner bb = setUp(pCaps);
        /*
         * Test a phrase all caps
         */
        SWTBot bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText(phraseCaps);
        bot.comboBox(1).setText(controlPhrase);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String text = bb.textViewTools.getSelectionStripped();
        log.debug("Log Replaced Text " + text);
        String replacePhraseCaps = "REPLACED TEXT";
        TestUtils.getFirstSectionChild(bb)
                .nextChildIsText(replacePhraseCaps);
        breakDown(bb);
    }

    @Test(enabled = false)
    public void wordInitial() {
        BBTestRunner bb = setUp(wInitial);

        /*
         * Test a word to initialCaps
         */
        SWTBot bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText(wordInitial);
        bot.comboBox(1).setText(controlWord);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String text = bb.textViewTools.getSelectionStripped();
        log.debug("Log Replaced Text " + text);
        String replaceWordInitial = "Newword";
        TestUtils.getFirstSectionChild(bb)
                .nextChildIsText(replaceWordInitial);
        breakDown(bb);
    }

    @Test(enabled = false)
    public void wordCaps() {
        BBTestRunner bb = setUp(wCaps);

        /*
         * Test word to all caps
         */
        SWTBot bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText(wordCaps);
        bot.comboBox(1).setText(controlWord);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String text = bb.textViewTools.getSelectionStripped();
        log.debug("Log Replaced Text " + text);
        String replaceWordCaps = "NEWWORD";
        TestUtils.getFirstSectionChild(bb)
                .nextChildIsText(replaceWordCaps);
        breakDown(bb);
    }

    @Test(enabled = false)
    public void wordSmall() {
        BBTestRunner bb = setUp(wSmall);
        /*
         * Test word to no caps
         */
        SWTBot bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText(wordSmall);
        bot.comboBox(1).setText(controlWord);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String text = bb.textViewTools.getSelectionStripped();
        log.debug("Log Replaced Text " + text);
        String replaceWordSmall = "newword";
        TestUtils.getFirstSectionChild(bb)
                .nextChildIsText(replaceWordSmall);
        breakDown(bb);
    }

    private BBTestRunner setUp(Document TEST_FILE) {
        BBTestRunner bb = new BBTestRunner(TEST_FILE);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        SWTBotCheckBox matchCase = bot.checkBox(SearchConstants.CASE_SENSITIVE);
        if (!matchCase.isChecked()) {
            matchCase.click();
        }
        assertTrue(matchCase.isChecked());

        bot = bb.bot.activeShell().bot();
        matchCase = bot.checkBox(SearchConstants.MATCH_CASE);
        if (!matchCase.isChecked()) {
            matchCase.click();
        }
        assertTrue(matchCase.isChecked());
        return bb;
    }

    private void breakDown(BBTestRunner bb) {
        FindReplaceBotHelper.clickClose(bb);
        SWTBotCheckBox matchCase = TestUtils.refreshReturnActiveBot(bb)
                .checkBox(SearchConstants.CASE_SENSITIVE);
        if (matchCase.isChecked()) {
            matchCase.click();
        }
        assertFalse(matchCase.isChecked());

        matchCase = TestUtils.refreshReturnActiveBot(bb)
                .checkBox(SearchConstants.MATCH_CASE);
        if (matchCase.isChecked()) {
            matchCase.click();
        }
        assertFalse(matchCase.isChecked());
    }
}
