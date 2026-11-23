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
import java.io.File;

import org.brailleblaster.TestFiles;
import org.brailleblaster.TestUtils;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import nu.xom.Document;

@GuiTest
public class FindReplaceStylesTest {
    private static final Logger log = LoggerFactory.getLogger(FindReplaceStylesTest.class);
    private final String regP = "This is a regular paragraph.";
    private final String cHead = "We made this a centered heading.";
    private final Document TEST_FILE = TestXMLUtils.generateBookDoc("", "<p>" + regP + "</p>");
    private final Document alreadyHeading = TestXMLUtils.generateBookDoc("",
            "<h1>" + regP + "</h1>" + "<h1>" + regP + "</h1>");

    @Test(enabled = false)
    public void rt6100_replace_split_text_node_and_make_style() {

        BBTestRunner bb = new BBTestRunner(new File(TestFiles.litBook));
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        FindReplaceBotHelper.clickFormatting(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(0).select("Centered Text");
        FindReplaceBotHelper.modifyReplace(bb);
        FindReplaceBotHelper.done(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText("States");
        bot.comboBox(1).setText("steak");
        FindReplaceBotHelper.clickFind(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String afterXml = TestXMLUtils.toXMLnoNS(bb.getDoc().getRootElement());
        log.debug("Log Replaced Text " + afterXml);
        String textNode = bb.getDoc().getChild(0).getChild(1).getChild(0).getChild(3).getChild(3).getChild(3).getValue();
        TestUtils.getInnerSection(bb, true).child(0).child(3).child(3).hasOverrideStyle("Centered Text");
        Assert.assertTrue(textNode.contains("steak"));
    }

    @Test(enabled = false)
    public void makeHeading() {

        BBTestRunner bb = new BBTestRunner(TEST_FILE);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(0).select("Centered Heading");
        FindReplaceBotHelper.modifyReplace(bb);
        FindReplaceBotHelper.done(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText(regP);
        bot.comboBox(1).setText(cHead);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);

        String afterXml = TestXMLUtils.toXMLnoNS(bb.getDoc().getRootElement());
        log.debug("Log Replaced Text " + afterXml);

        TestUtils.getInnerSection(bb, true).child(0).hasOverrideStyle("Centered Heading").nextChildIsText(cHead);

    }

    @Test(enabled = false)
    public void makeList() {
        BBTestRunner bb = new BBTestRunner(TEST_FILE);
        bb.textViewTools.pressShortcut(SWT.CTRL, 'f');
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(0).select("Centered Heading");
        FindReplaceBotHelper.modifyReplace(bb);
        FindReplaceBotHelper.done(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText(regP);
        String list = "We made this a list item.";
        bot.comboBox(1).setText(list);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);

        String afterXml = TestXMLUtils.toXMLnoNS(bb.getDoc().getRootElement());
        log.debug("Log Replaced Text " + afterXml);

        bb.assertRootSectionFirst_NoBrlCopy().hasOverrideStyle("Centered Heading").nextChildIsText(list);
    }

    @Test(enabled = false)
    public void makeBox() {
        BBTestRunner bb = new BBTestRunner(TEST_FILE);
        bb.textViewTools.pressShortcut(SWT.CTRL, 'f');
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(1).select("Box");
        FindReplaceBotHelper.modifyReplace(bb);
        FindReplaceBotHelper.done(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText(regP);
        bot.comboBox(1).setText("");
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);

        String afterXml = TestXMLUtils.toXMLnoNS(bb.getDoc().getRootElement());
        log.debug("Log Replaced Text for Box" + afterXml);

        TestUtils.getInnerSection(bb, true).child(0).hasOverrideStyle("Box").child(0).nextChildIsText(regP);
    }

    @Test(enabled = false)
    public void findNoTextThenReplace() {

        BBTestRunner bb = new BBTestRunner(alreadyHeading);
        bb.textViewTools.pressShortcut(SWT.CTRL, 'f');
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(0).select("Centered Heading");
        FindReplaceBotHelper.modifyFind(bb);
        FindReplaceBotHelper.done(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText("");
        bot.comboBox(1).setText(cHead);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);

        String afterXml = TestXMLUtils.toXMLnoNS(bb.getDoc().getRootElement());
        log.debug("Log Replaced Text " + afterXml);

        TestUtils.getInnerSection(bb, true).child(0).hasStyle("Centered Heading").nextChildIsText(cHead);

        TestUtils.getInnerSection(bb, true).child(1).hasStyle("Centered Heading").nextChildIsText(cHead);
    }

}
