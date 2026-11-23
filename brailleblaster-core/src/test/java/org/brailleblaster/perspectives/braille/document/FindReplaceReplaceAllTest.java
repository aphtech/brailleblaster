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
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.util.EnumSet;

import org.brailleblaster.TestGroups;
import org.brailleblaster.TestUtils;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.search.SearchController;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.utd.properties.EmphasisType;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import nu.xom.Document;
import nu.xom.Element;

@Test(groups = TestGroups.SLOW_TESTS, enabled = false)
@GuiTest
public class FindReplaceReplaceAllTest {
    private static final Logger log = LoggerFactory.getLogger(FindReplaceReplaceAllTest.class);
    private final Document multiples = TestXMLUtils.generateBookDoc("", "<p>" + "word " + "word " + "word " + "word"
            + "</p>" + "<p>" + "word " + "word " + "word " + "word" + "</p>");

    private final Document emphasis = TestXMLUtils.generateBookDoc("", "<p>" + "word " + "bold " + "word " + "bold"
            + "</p>" + "<p>" + "word " + "bold " + "word " + "bold" + "</p>");

    private final Document style = TestXMLUtils.generateBookDoc("", "<p>" + "word " + "Caption " + "word " + "Caption"
            + "</p>" + "<p>" + "word " + "Caption " + "word " + "Caption" + "</p>");

    private final Document caseSensitive = TestXMLUtils.generateBookDoc("",
            "<p>" + "word " + "WORD " + "Word" + "</p>");

    private final Document oneLetter = TestXMLUtils.generateBookDoc("", "<p>" + "aAaAaA" + "</p>");

    private final Document alreadyBold = TestXMLUtils.generateBookDoc("",
            "<strong>" + "bold" + "</strong>" + "<strong>" + "bold" + "</strong>");

    private final String collections = "/media/brailleblaster/nimas-books/9780544087507NIMAS_collections.xml";

    @Test(enabled = false)
    public void basicReplaceAll() {
        BBTestRunner bb = new BBTestRunner(multiples);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        bot.comboBox(0).setText("word");
        bot.comboBox(1).setText("success");
        FindReplaceBotHelper.clickReplaceAll(bb);
        String text = bb.textViewTools.getTextStripped();
        log.debug("Log Replaced Text " + text);
        TestUtils.getInnerSection(bb).textChildCount(2).child(0).nextChildIsText("success success success success");
    }

    @Test(enabled = false)
    public void replaceAllWithEmphasis() {
        BBTestRunner bb = new BBTestRunner(emphasis);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");
        FindReplaceBotHelper.modifyReplace(bb);
        FindReplaceBotHelper.done(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText("bold");
        bot.comboBox(1).setText("");
        FindReplaceBotHelper.clickReplaceAll(bb);
        String text = bb.textViewTools.getTextStripped();
        log.debug("Log Replaced Text " + text);

        TestUtils.getInnerSection(bb).textChildCount(8).child(1).child(1).isInlineEmphasis(EmphasisType.BOLD);
    }

    @Test(enabled = false)
    public void replaceAllWithStyles() {
        BBTestRunner bb = new BBTestRunner(style);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(0).select("Caption");
        FindReplaceBotHelper.modifyReplace(bb);
        FindReplaceBotHelper.done(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText("caption");
        bot.comboBox(1).setText("");
        FindReplaceBotHelper.clickReplaceAll(bb);
        String text = bb.textViewTools.getTextStripped();
        log.debug("Log Replaced Text " + text);

        TestUtils.getInnerSection(bb).textChildCount(2).child(0).hasOverrideStyle("Caption");
    }

    @Test(enabled = false)
    public void replaceAllStylesAndEmphasis() {
        BBTestRunner bb = new BBTestRunner(multiples);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(0).select("Caption");
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");
        FindReplaceBotHelper.modifyReplace(bb);
        FindReplaceBotHelper.done(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText("word");
        bot.comboBox(1).setText("");
        FindReplaceBotHelper.clickReplaceAll(bb);
        String text = bb.textViewTools.getTextStripped();
        log.debug("Log Replaced Text " + text);

        TestUtils.getInnerSection(bb).textChildCount(14).child(0).hasOverrideStyle("Caption").child(0)
                .isInlineEmphasis(EmphasisType.BOLD);
    }

    @Test(enabled = false)
    public void replaceAllCaseSensitive() {
        BBTestRunner bb = new BBTestRunner(caseSensitive);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.checkMatchCase(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText("word");
        bot.comboBox(1).setText("suCcess");
        FindReplaceBotHelper.clickReplaceAll(bb);
        FindReplaceBotHelper.clickOk(bb);
        FindReplaceBotHelper.checkMatchCase(bb);
        String text = bb.textViewTools.getTextStripped();
        log.debug("Log Replaced Text " + text);

        TestUtils.getInnerSection(bb).textChildCount(1).child(0).nextChildIsText("success SUCCESS Success");
    }

    /*
     * RT 4613 test
     */

    @Test(enabled = false)
    public void replaceAllCaseSensitiveOneLetter() {
        BBTestRunner bb = new BBTestRunner(oneLetter);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.checkMatchCase(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText("a");
        bot.comboBox(1).setText("b");
        FindReplaceBotHelper.clickReplaceAll(bb);
        FindReplaceBotHelper.clickOk(bb);
        FindReplaceBotHelper.checkMatchCase(bb);
        String text = bb.textViewTools.getTextStripped();
        log.debug("Log Replaced Text " + text);
        TestUtils.getInnerSection(bb).textChildCount(1).child(0).nextChildIsText("bBbBbB");
    }

    @Test(enabled = false)
    public void replaceAllAttributionsCollections() {
        BBTestRunner bb = new BBTestRunner(new File(collections));
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);
        FindReplaceBotHelper.addStylesAndContainers(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(0).select("Attribution");
        FindReplaceBotHelper.modifyFind(bb);
        FindReplaceBotHelper.addStylesAndContainers(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(0).select("Centered Heading");
        FindReplaceBotHelper.modifyReplace(bb);
        FindReplaceBotHelper.done(bb);
        long start = FindAndReplaceTest.startTimer();
        FindReplaceBotHelper.clickReplaceAll(bb);
        SearchController.logIt(bot.getDisplay().getActiveShell().getText() + " is the active shell.");
        String replaced = TestUtils.refreshReturnActiveBot(bb).label(1).getText();
        SearchController.logIt(replaced + " the label");
        String[] replacedMessage = replaced.split(" ");
        // This is the message "229 instances replaced"
        String stringNumber = replacedMessage[0];
        int numReplaced = Integer.parseInt(stringNumber);
        long minutes = FindAndReplaceTest.getTotalTime(start);
        SearchController.logIt("Total time: " + minutes + " minutes. ");
        assertEquals(numReplaced, 6);
    }

    @Test(enabled = false)
    public void replaceAllLargeDocumentLitBook() {
        // These paths exist on Jenkins and the testFast scripts
        String litBook = "/media/brailleblaster/nimas-books/9780133268195NIMAS_revised.xml";
        BBTestRunner bb = new BBTestRunner(new File(litBook));
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        bot.comboBox(0).setText("literature");
        bot.comboBox(1).setText("success");
        long start = FindAndReplaceTest.startTimer();
        FindReplaceBotHelper.clickReplaceAll(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        String replaced = bot.label(1).getText();
        String[] replacedMessage = replaced.split(" ");
        // This is the message "229 instances replaced"
        String stringNumber = replacedMessage[0];
        int numReplaced = Integer.parseInt(stringNumber);
        long minutes = FindAndReplaceTest.getTotalTime(start);
        SearchController.logIt("Total time: " + minutes + " minutes. ");
        assertEquals(numReplaced, 229);
    }

    @Test(enabled = false)
    public void replaceAllLargeDocumentCollections() {
        /*
         * 40 instances of 'teacher', 3 in tables
         */
        BBTestRunner bb = new BBTestRunner(new File(collections));

        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        bot.comboBox(0).setText("teacher");
        bot.comboBox(1).setText("success");
        long start = FindAndReplaceTest.startTimer();
        FindReplaceBotHelper.clickReplaceAll(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        String replaced = bot.label(1).getText();
        String[] replacedMessage = replaced.split(" ");
        // This is the message "229 instances replaced"
        String stringNumber = replacedMessage[0];
        int numReplaced = Integer.parseInt(stringNumber);
        long minutes = FindAndReplaceTest.getTotalTime(start);
        SearchController.logIt("Total time: " + minutes + " minutes. ");
        assertEquals(numReplaced, 37);
    }

    @Test(enabled = false)
    public void replaceAllBoldUnbold() {
        /*
         * The word opportunity in Collections book -- 1 bold, 10 unbold, 3 in
         * tables
         */
        BBTestRunner bb = new BBTestRunner(new File(collections));
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");
        FindReplaceBotHelper.modifyFind(bb);
        FindReplaceBotHelper.done(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText("opportunity");
        bot.comboBox(1).setText("success");
        long start = FindAndReplaceTest.startTimer();
        FindReplaceBotHelper.clickReplaceAll(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        String replaced = bot.label(1).getText();

        String[] replacedMessage = replaced.split(" ");
        // This is the message "229 instances replaced"
        String stringNumber = replacedMessage[0];
        int numReplaced = Integer.parseInt(stringNumber);

        long minutes = FindAndReplaceTest.getTotalTime(start);

        SearchController.logIt("Total time: " + minutes + " minutes. ");
        assertEquals(numReplaced, 1);
        FindReplaceBotHelper.clickOk(bb);

        // __________Replace all unbold______________

        FindReplaceBotHelper.clickReset(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText("opportunity");
        bot.comboBox(1).setText("success");
        start = FindAndReplaceTest.startTimer();
        FindReplaceBotHelper.clickReplaceAll(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        replaced = bot.label(1).getText();

        replacedMessage = replaced.split(" ");
        // This is the message "229 instances replaced"
        stringNumber = replacedMessage[0];
        numReplaced = Integer.parseInt(stringNumber);

        minutes = FindAndReplaceTest.getTotalTime(start);

        SearchController.logIt("Total time: " + minutes + " minutes. ");
        assertEquals(numReplaced, 10);
    }

    @Test(enabled = false)
    public void RT5279makeBoldIntoItalicsAndRemoveBold() {
        BBTestRunner bb = new BBTestRunner(alreadyBold);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");
        FindReplaceBotHelper.modifyFind(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("ITALICS");
        FindReplaceBotHelper.addTextAttributes(bb);
        FindReplaceBotHelper.modifyReplace(bb);
        FindReplaceBotHelper.removeTextAttributes(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");

        FindReplaceBotHelper.modifyReplace(bb);
        FindReplaceBotHelper.done(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText("");
        bot.comboBox(1).setText("");
        FindReplaceBotHelper.clickReplaceAll(bb);

        String afterXml = TestXMLUtils.toXMLnoNS(bb.getDoc().getRootElement());
        log.debug("Log Replaced Text for Emphasis" + afterXml);

        TestUtils.getFirstSectionChild(bb).child(0).isInlineEmphasis(EmphasisType.ITALICS);
        Assert.assertEquals(
                BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS
                        .get((Element) bb.getDoc().getChild(0).getChild(1).getChild(0).getChild(0)),
                EnumSet.of(EmphasisType.ITALICS));
        Assert.assertFalse(BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS
                .get((Element) bb.getDoc().getChild(0).getChild(1).getChild(0).getChild(0))
                .contains((EmphasisType.BOLD)));
    }

}
