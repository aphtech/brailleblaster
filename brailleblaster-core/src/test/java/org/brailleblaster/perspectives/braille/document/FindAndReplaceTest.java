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
import org.brailleblaster.testrunners.ViewTestRunner;
import org.brailleblaster.util.Notify;
import org.brailleblaster.util.Notify.DebugException;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import static org.testng.Assert.assertEquals;

@GuiTest
public class FindAndReplaceTest {
    private static final Logger log = LoggerFactory.getLogger(FindAndReplaceTest.class);
    private final Document oddSpacing = TestXMLUtils.generateBookDoc("",
            "<p>" + "Test with" + "</p>" + "<p>" + "odd spacing" + "</p>");
    private final Document basic = TestXMLUtils.generateBookDoc("", "<p>" + "Test with normal spacing" + "</p>");
    private final Document multiples = TestXMLUtils.generateBookDoc("", "<p>" + "word " + "word " + "word " + "word"
            + "</p>" + "<p>" + "word " + "word " + "word " + "word" + "</p>");
    private final Document repeat = TestXMLUtils.generateBookDoc("",
            "<p>" + "word " + "word " + "word " + "word" + "</p>");
    private final Document emptyText = TestXMLUtils.generateBookDoc("",
            "<p>" + "good " + "bad " + "good " + "bad" + "</p>");
    private final Document dollarSign = TestXMLUtils.generateBookDoc("", "<p>" + "More $ more problems" + "</p>");
    private final Document periodTest = TestXMLUtils.generateBookDoc("", "<p>" + "word," + "</p>");
    private final String odd = "Test with odd spacing";
    private final String normal = "Test with normal spacing";
    private final String replaced = "Replaced text";
    private final String question = "?";
    private final Document questionMark = TestXMLUtils.generateBookDoc("",
            "<p>" + "My dad told me to " + question + " everything." + "</p>");

    public enum REPLACE_OUTCOME {
        SUCCESS, END_DOC, EXCEPTION, WRONG_SHELL
    }

    public enum FIND_OUTCOME {
        SUCCESS, END_DOC, WRONG_SHELL
    }

    /*
     * RT#4533 can't find dollar sign
     */
    //@test
    public void dollarSign() {
        BBTestRunner bb = new BBTestRunner(dollarSign);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();
        bot = cleanSettings(bot, bb);
        String dollar = "$";
        bot.comboBox(0).setText(dollar);
        FindReplaceBotHelper.clickFind(bb);
        String text = bb.textViewTools.getSelectionStripped();
        assertEquals((text), (dollar));
    }

    /*
     * RT#4533 can't find question mark
     */
    //@test
    public void questionMark() {
        BBTestRunner bb = new BBTestRunner(questionMark);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();
        bot = cleanSettings(bot, bb);

        bot.comboBox(0).setText(question);
        FindReplaceBotHelper.clickFind(bb);
        String text = bb.textViewTools.getSelectionStripped();
        assertEquals((text), (question));
    }

    //@test
    public void basicFind() {
        BBTestRunner bb = new BBTestRunner(basic);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();

        bot.comboBox(0).setText(normal);
        FindReplaceBotHelper.clickFind(bb);
        String text = bb.textViewTools.getSelectionStripped();
        assertEquals((text), (normal));
    }

    //@test
    public void basicReplace() {
        BBTestRunner bb = new BBTestRunner(basic);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();

        bot.comboBox(0).setText(normal);
        bot.comboBox(1).setText(replaced);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String text = bb.textViewTools.getTextStripped();
        log.debug("Log Replaced Text " + text);
        assertEquals((text), (replaced));
    }

    //@test
    /*
     * Ticket 4436
     */
    public void findMultiples() {
        BBTestRunner bb = new BBTestRunner(multiples);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = TestUtils.refreshReturnActiveBot(bb);

        bot.comboBox(0).setText("word");
        FindReplaceBotHelper.clickFind(bb);
        int wordCount = 0;
        bot = TestUtils.refreshReturnActiveBot(bb);
        while (bot.getDisplay().getActiveShell().getText().equals(SearchConstants.FIND_REPLACE_SHELL)) {
            wordCount++;
            bot.comboBox(0).setText("word");
            bot = TestUtils.refreshReturnActiveBot(bb);
            FindReplaceBotHelper.clickFind(bb);
            if (wordCount > 8)
                Assert.fail("Too many words");
        }

        assertEquals((wordCount), 8);
    }

    //@test
    public void findOddSpacing() {

        /*
         * The file will force the phrase to go across a line break by making it
         * two text nodes.
         */
        BBTestRunner bb = new BBTestRunner(oddSpacing);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = TestUtils.refreshReturnActiveBot(bb);

        bb.textViewTools.navigate(0);
        TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText(odd);
        FindReplaceBotHelper.clickFind(bb);
        String text = bb.textViewBot.getSelection().replaceAll("\n", " ").replaceAll("\r", " ").replaceAll("\\s+", " ");
        assertEquals((text), (odd));
    }

    //@test
    public void replaceOddSpacing() {

        /*
         * The file will force the phrase to go across a line break by making it
         * two text nodes.
         */
        BBTestRunner bb = new BBTestRunner(oddSpacing);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();
        bot = cleanSettings(bot, bb);

        bot.comboBox(0).setText(odd);
        bot.comboBox(1).setText(replaced);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String text = bb.textViewTools.getTextStripped();
        log.debug("Log Replaced Text " + text);
        assertEquals((text), (replaced));
    }

    /*
     * RT 4318 using F3 for repeat last search
     */

    //@test
    public void repeatLastSearch() {
        BBTestRunner bb = new BBTestRunner(repeat);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();
        bot = cleanSettings(bot, bb);
        bot.comboBox(0).setText("word");
        FindReplaceBotHelper.clickFind(bb);
        FindReplaceBotHelper.clickClose(bb);
        bb.textViewTools.pressKey(SWT.ARROW_RIGHT, 1);
        bb.textViewTools.pressKey(SWT.F3, 1);

        ViewTestRunner.doPendingSWTWork();
        int x = bb.textViewTools.widget.getCaretOffset();
        log.debug("Log Replaced Text " + x);
        assertEquals((x), (9));
    }

    /*
     * RT4447 replace with empty text confusion, part 1 -- empty text with no
     * formatting
     */
    //@test
    public void replaceWithEmptyText() {
        BBTestRunner bb = new BBTestRunner(emptyText);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();

        bot.comboBox(0).setText("bad");
        bot.comboBox(1).setText("");
        FindReplaceBotHelper.clickReplace(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);

        bot.comboBox(0).setText("bad");
        bot.comboBox(1).setText("");
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String text = bb.textViewTools.getTextStripped();
        log.debug("Log Replaced Text " + text);

        TestUtils.getInnerSection(bb).textChildCount(1).child(0).nextChildIsText("good  good ");
    }

    /*
     * RT4578 Find Replace confused about periods, matching a search for test.
     * with test, test$, etc.
     */
    //@test
    public void periodMatchingAllWords() {
        BBTestRunner bb = new BBTestRunner(periodTest);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();

        bot.comboBox(0).setText("word.");
        FindReplaceBotHelper.clickFind(bb);
        String text = bb.textViewTools.getSelectionStripped();
        assertEquals((text), (""));
    }

    // Utils

    public static SWTBotCombo findBox(BBTestRunner bb) {
        return bb.activeShell().bot().comboBox(0);
    }

    public static SWTBotCombo replaceBox(BBTestRunner bb) {
        return bb.activeShell().bot().comboBox(1);
    }

    public static SWTBot cleanSettings(SWTBot bot, BBTestRunner bb) {
        FindReplaceBotHelper.clickReset(bb);
        return TestUtils.refreshReturnActiveBot(bb);
    }

    public static long startTimer() {
        return System.currentTimeMillis();
    }

    public static long getTotalTime(long startTime) {
        return (System.currentTimeMillis() - startTime) / 1000 / 60;// gives
        // minutes
    }

    public static boolean isFNRDialog(BBTestRunner bb) {
        return TestUtils.refreshReturnActiveBot(bb).getDisplay().getActiveShell().getText().equals(SearchConstants.FIND_REPLACE_SHELL);
    }

    public static boolean isNotifyDialog(BBTestRunner bb) {
        return TestUtils.refreshReturnActiveBot(bb).getDisplay().getActiveShell().getText()
                .equals(Notify.ALERT_SHELL_NAME)
                || TestUtils.refreshReturnActiveBot(bb).getDisplay().getActiveShell().getText()
                .equals(Notify.EXCEPTION_SHELL_NAME);
    }

    private static boolean isEndDoc(BBTestRunner bb) {
        return TestUtils.refreshReturnActiveBot(bb).getDisplay().getActiveShell().getText()
                .equals(SearchConstants.END_DOC_SHELL);
    }

    /**
     * @param bb
     * @return replace outcome
     */
    public static REPLACE_OUTCOME clickReplace(BBTestRunner bb) {
        if (!isFNRDialog(bb)) {
            log.error("Find replace with wrong shell, shell is " + bb.activeShell().getText());
            if (isEndDoc(bb)) {
                return REPLACE_OUTCOME.END_DOC;
            }
            return REPLACE_OUTCOME.WRONG_SHELL;
        }
        try {
            TestUtils.refreshReturnActiveBot(bb).button(SearchConstants.REPLACE_FIND).click();
        } catch (DebugException e) {
            bb.textViewTools.pressKey(SWT.ESC, 1);
            FindReplaceBotHelper.openDialog(bb);
            return REPLACE_OUTCOME.EXCEPTION;
        }
        if (isFNRDialog(bb)) {
            return REPLACE_OUTCOME.SUCCESS;
        } else if (isEndDoc(bb)) {
            return REPLACE_OUTCOME.END_DOC;
        } else {
            throw new RuntimeException("Find and Replace is triggering an unexpected dialog");
        }
    }

    /**
     * @param bb
     * @return find outcome
     */
    public static FIND_OUTCOME clickFind(BBTestRunner bb) {
        if (!isFNRDialog(bb)) {
            log.error("Find replace with wrong shell, shell is " + bb.activeShell().getText());
            if (isEndDoc(bb)) {
                return FIND_OUTCOME.END_DOC;
            }
            return FIND_OUTCOME.WRONG_SHELL;
        }
        try {
            FindReplaceBotHelper.clickFind(bb);
        } catch (DebugException e) {
            throw new RuntimeException("Find and Replace is triggering an unexpected dialog");
        }
        if (isFNRDialog(bb)) {
            return FIND_OUTCOME.SUCCESS;
        } else if (isEndDoc(bb)) {
            return FIND_OUTCOME.END_DOC;
        } else {
            throw new RuntimeException("Find and Replace is triggering an unexpected dialog");
        }
    }

}
