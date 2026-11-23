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
import org.brailleblaster.TestUtils;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.utd.properties.EmphasisType;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import nu.xom.Document;

@GuiTest
public class FindReplaceEmphasisTest {
    private static final Logger log = LoggerFactory.getLogger(FindReplaceEmphasisTest.class);
    private final String bold = "bold";
    private final String unbold = "unbold";
    private final Document boldMe = TestXMLUtils.generateBookDoc("", "<p>" + unbold + "</p>");
    private final Document unboldMe = TestXMLUtils.generateBookDoc("", "<strong>" + bold + "</strong>");
    private final Document alreadyBold = TestXMLUtils.generateBookDoc("",
            "<strong>" + bold + "</strong>" + "<strong>" + bold + "</strong>");
    private final Document mixedBoldUnbold = TestXMLUtils.generateBookDoc("", "<strong>" + bold + "</strong>" + unbold);

    @Test(enabled = false)
    public void makeBold() {
        BBTestRunner bb = new BBTestRunner(boldMe);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.modifyReplace(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.done(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText(unbold);
        bot.comboBox(1).setText(bold);
        FindReplaceBotHelper.clickReplace(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.clickReplace(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);

        String afterXml = TestXMLUtils.toXMLnoNS(bb.getDoc().getRootElement());
        log.debug("Log Replaced Text for Emphasis" + afterXml);

        TestUtils.getFirstSectionChild(bb).child(0).isInlineEmphasis(EmphasisType.BOLD);
    }

    @Test(enabled = false)
    public void makeUnbold() {
        BBTestRunner bb = new BBTestRunner(unboldMe);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.removeTextAttributes(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.modifyReplace(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.done(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText(bold);
        bot.comboBox(1).setText(unbold);
        FindReplaceBotHelper.clickReplace(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.clickReplace(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);

        String afterXml = TestXMLUtils.toXMLnoNS(bb.getDoc().getRootElement());
        log.debug("Log Replaced Text " + afterXml);

        TestUtils.getFirstSectionChild(bb).nextChildIsText(unbold);
    }

    /*
     * RT4447 replace with empty text confusion, part 2 -- empty text with bold
     * formatting
     */
    @Test(enabled = false)
    public void boldEmptyTextCombo() {
        BBTestRunner bb = new BBTestRunner(boldMe);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.modifyReplace(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.done(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText(unbold);
        bot.comboBox(1).setText("");
        FindReplaceBotHelper.clickReplace(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.clickReplace(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);

        String afterXml = TestXMLUtils.toXMLnoNS(bb.getDoc().getRootElement());
        log.debug("Log Replaced Text for Emphasis" + afterXml);

        TestUtils.getFirstSectionChild(bb).child(0).isInlineEmphasis(EmphasisType.BOLD);
    }

    @Test(enabled = false)
    public void findNoTextThenReplace() {

        BBTestRunner bb = new BBTestRunner(alreadyBold);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.modifyFind(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.done(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText("");
        String success = "success";
        bot.comboBox(1).setText(success);
        FindReplaceBotHelper.clickReplace(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.clickReplace(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.clickReplace(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);

        String afterXml = TestXMLUtils.toXMLnoNS(bb.getDoc().getRootElement());
        log.debug("Log Replaced Text " + afterXml);

        TestUtils.getInnerSection(bb, true).child(0).child(0).isInlineEmphasis(EmphasisType.BOLD)
                .nextChildIsText(success);

        TestUtils.getInnerSection(bb, true).child(0).child(1).isInlineEmphasis(EmphasisType.BOLD)
                .nextChildIsText(success);
    }

    @Test(enabled = false)
    public void makeBoldIntoBoldAndItalics() {
        BBTestRunner bb = new BBTestRunner(alreadyBold);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("ITALICS");

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.modifyReplace(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.done(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText("bold");
        bot.comboBox(1).setText("");
        FindReplaceBotHelper.clickReplace(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.clickReplace(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);

        String afterXml = TestXMLUtils.toXMLnoNS(bb.getDoc().getRootElement());
        log.debug("Log Replaced Text for Emphasis" + afterXml);

        TestUtils.getFirstSectionChild(bb).child(0).isInlineEmphasis(EmphasisType.ITALICS, EmphasisType.BOLD);
    }

    @Test(enabled = false)
    public void RT5279makeBoldIntoItalicsAndRemoveBold() {
        BBTestRunner bb = new BBTestRunner(alreadyBold);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.modifyFind(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("ITALICS");

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.addTextAttributes(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.modifyReplace(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.removeTextAttributes(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.modifyReplace(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.done(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText("");
        bot.comboBox(1).setText("");
        FindReplaceBotHelper.clickReplace(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.clickReplace(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);

        String afterXml = TestXMLUtils.toXMLnoNS(bb.getDoc().getRootElement());
        log.debug("Log Replaced Text for Emphasis" + afterXml);

        bb.assertRootSectionFirst_NoBrlCopy().isBlock(BBX.BLOCK.DEFAULT).child(0).isInlineEmphasis(EmphasisType.ITALICS);
    }

    @Test(enabled = false)
    public void RT5280makeBoldIntoItalicsAndRemoveBoldInBlock() {
        BBTestRunner bb = new BBTestRunner(mixedBoldUnbold);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.modifyFind(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("ITALICS");

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.addTextAttributes(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.modifyReplace(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        FindReplaceBotHelper.removeTextAttributes(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");
        FindReplaceBotHelper.modifyReplace(bb);
        FindReplaceBotHelper.done(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText("");
        bot.comboBox(1).setText("");
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);

        String afterXml = TestXMLUtils.toXMLnoNS(bb.getDoc().getRootElement());
        log.debug("Log Replaced Text for Emphasis" + afterXml);

        TestUtils.getFirstSectionChild(bb).child(0).isInlineEmphasis(EmphasisType.ITALICS);
    }
}
