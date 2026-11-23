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
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotEquals;


@GuiTest
public class FindReplaceNonBreakingSpaceTest {
    private static final Logger log = LoggerFactory.getLogger(FindReplaceNonBreakingSpaceTest.class);
    private final String regularSpace = "Unit 1";
    private final String nbSpace = "Unit\u00a01";// non-breaking space
    private final String multiLineRegular = "Testing across multiple words.";
    private final String multiLineNbs = "Testing\u00a0across\u00a0multiple\u00a0words.";// non breaking space
    private final Document rs = TestXMLUtils.generateBookDoc("",
            "<p>" + nbSpace + "</p>"
    );
    private final Document nbs = TestXMLUtils.generateBookDoc("",
            "<p>" + regularSpace + "</p>"
    );
    private final Document mlrs = TestXMLUtils.generateBookDoc("",
            "<p>" + multiLineNbs + "</p>"
    );
    private final Document mlnbs = TestXMLUtils.generateBookDoc("",
            "<p>" + multiLineRegular + "</p>"
    );

    @Test(enabled = false)
    public void breakToNonOneWord() {
        BBTestRunner bb = new BBTestRunner(rs);
        bb.textViewTools.pressShortcut(SWT.CTRL, 'f');
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        /*
         * Test non breaking to regular
         */
        bot.comboBox(0).setText(nbSpace);
        bot.comboBox(1).setText(regularSpace);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);

        String text = bb.textViewTools.getSelectionStripped();
        log.debug("Log Replaced Text " + text);

        TestUtils.getFirstSectionChild(bb)
                .nextChildIsText(regularSpace);

    }

    @Test(enabled = false)
    public void nonToBreakOneWord() {
        BBTestRunner bb = new BBTestRunner(nbs);
        bb.textViewTools.pressShortcut(SWT.CTRL, 'f');
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        /*
         * Test regular to non breaking
         */

        bot.comboBox(0).setText(regularSpace);
        bot.comboBox(1).setText(nbSpace);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String text = bb.textViewTools.getSelectionStripped();
        log.debug("Log Replaced Text " + text);
        TestUtils.getFirstSectionChild(bb)
                .nextChildIsText(nbSpace);
    }

    @Test(enabled = false)
    public void nonToBreakPhrase() {
        BBTestRunner bb = new BBTestRunner(mlnbs);
        bb.textViewTools.pressShortcut(SWT.CTRL, 'f');
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        /*
         * Test regular to non breaking
         */
        bot.comboBox(0).setText(multiLineRegular);
        bot.comboBox(1).setText(multiLineNbs);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String text = bb.textViewTools.getSelectionStripped();
        log.debug("Log Replaced Text " + text);
        TestUtils.getFirstSectionChild(bb)
                .nextChildIsText(multiLineNbs);
    }

    @Test(enabled = false)
    public void breakToNonPhrase() {
        BBTestRunner bb = new BBTestRunner(mlrs);
        bb.textViewTools.pressShortcut(SWT.CTRL, 'f');
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        /*
         * Test non breaking to regular
         */
        bot.comboBox(0).setText(multiLineNbs);
        bot.comboBox(1).setText(multiLineRegular);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String text = bb.textViewTools.getSelectionStripped();
        log.debug("Log Replaced Text " + text);
        TestUtils.getFirstSectionChild(bb)
                .nextChildIsText(multiLineRegular);
    }

    @Test(enabled = false)
    public void testChar() {
        /*
         * make sure that testng can differentiate between the characters
         */
        char nbs = ' ';
        char space = ' ';
        String nbs2 = "\u00a0";
        String space2 = " ";
        assertNotEquals(space, nbs);
        assertNotEquals(space2, nbs2);
    }

}
