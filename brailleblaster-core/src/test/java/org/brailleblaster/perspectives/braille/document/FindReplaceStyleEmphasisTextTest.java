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
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.testrunners.ViewTestRunner;
import org.brailleblaster.utd.properties.EmphasisType;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import nu.xom.Document;

@GuiTest
public class FindReplaceStyleEmphasisTextTest {
    private static final Logger log = LoggerFactory.getLogger(FindReplaceStyleEmphasisTextTest.class);
    private final String cHead = "Bold Centered Heading";
    private final String uList = "Unbolded List";
    private final Document TEST_FILE = TestXMLUtils.generateBookDoc("",
            "<h1>" + "<strong>" + cHead + "</strong>" + "</h1>");
    private final Document acrossNodesDoc = TestXMLUtils.generateBookDoc("",
            "<p>" + "<strong>" + "Bold " + "</strong>" + "Centered " + "<strong>" + "Heading" + "</strong>" + "</p>");

    @Test(enabled = false)
    public void findBodyTextMakeBold() {

        BBTestRunner bb = new BBTestRunner(acrossNodesDoc);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();
        FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);
        FindReplaceBotHelper.addTextAttributes(bb);
        FindReplaceBotHelper.addStylesAndContainers(bb);
        TestUtils.refreshReturnActiveBot(bb).list(0).select("Body Text");
        FindReplaceBotHelper.modifyFind(bb);
        FindReplaceBotHelper.resetReplace(bb);
        FindReplaceBotHelper.addTextAttributes(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");
        FindReplaceBotHelper.modifyReplace(bb);
        FindReplaceBotHelper.done(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String highlighted = bb.textViewTools.getSelectionStripped();
        Assert.assertEquals(highlighted, "Bold Centered Heading");
        FindReplaceBotHelper.clickReplace(bb);
        String afterXml = TestXMLUtils.toXMLnoNS(bb.getDoc());
        log.debug("Log Replaced Text " + afterXml);
        ViewTestRunner.doPendingSWTWork();
        TestUtils.getInnerSection(bb, true).child(0).child(0).isInlineEmphasis(EmphasisType.BOLD);
    }

    @Test(enabled = false)
    public void boldCenteredHeadingToUnboldedList() {

        BBTestRunner bb = new BBTestRunner(TEST_FILE);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();
        FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);
        FindReplaceBotHelper.addTextAttributes(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");
        FindReplaceBotHelper.addStylesAndContainers(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(0).select("Centered Heading");
        FindReplaceBotHelper.modifyFind(bb);
        FindReplaceBotHelper.removeTextAttributes(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");
        FindReplaceBotHelper.addStylesAndContainers(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(0).select("Centered Heading");
        FindReplaceBotHelper.modifyReplace(bb);
        FindReplaceBotHelper.done(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        FindAndReplaceTest.findBox(bb).setText(cHead);
        FindAndReplaceTest.replaceBox(bb).setText(uList);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);

        String afterXml = TestXMLUtils.toXMLnoNS(bb.getDoc());
        log.debug("Log Replaced Text " + afterXml);
        ViewTestRunner.doPendingSWTWork();

        bb.assertRootSectionFirst_NoBrlCopy().hasOverrideStyle("Centered Heading").nextChildIsText(uList);
    }

    @Test(enabled = false)
    public void acrossNodes() {

        BBTestRunner bb = new BBTestRunner(acrossNodesDoc);
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.activeShell().bot();
        FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);
        FindReplaceBotHelper.removeTextAttributes(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");
        FindReplaceBotHelper.addStylesAndContainers(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(0).select("Centered Heading");
        FindReplaceBotHelper.modifyReplace(bb);
        FindReplaceBotHelper.done(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        FindAndReplaceTest.findBox(bb).setText(cHead);
        FindAndReplaceTest.replaceBox(bb).setText(uList);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String afterXml = TestXMLUtils.toXMLnoNS(bb.getDoc());
        log.debug("Log Replaced Text " + afterXml);
        ViewTestRunner.doPendingSWTWork();
        bb.assertRootSection_NoBrlCopy().childIs(0, n -> n.hasOverrideStyle("Centered Heading").hasText(uList));
    }

}
