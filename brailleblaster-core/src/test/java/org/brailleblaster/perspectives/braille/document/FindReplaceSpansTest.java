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

import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.testrunners.ViewTestRunner;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import nu.xom.Document;

@GuiTest
public class FindReplaceSpansTest {
    private static final Logger log = LoggerFactory.getLogger(FindReplaceSpansTest.class);
    private final Document TEST_FILE = TestXMLUtils.generateBookDoc("",
            "<p>" + "<strong>" + "M" +
                    "<span class='smallcaps'>" + "R" + "</span>" +
                    ". F" + "<span class='smallcaps'>" + "RANK" + "</span>" +
                    "</strong>" + "</p>"
    );
    private final File MATH_SPANS = (new File("src/test/resources/org/brailleblaster/printView/weird_spans.xml"));

    @Test(enabled = false)
    public void findSmallCaps() {
        BBTestRunner bb = new BBTestRunner(TEST_FILE);
        bb.textViewTools.pressShortcut(SWT.CTRL, 'f');
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        String toFind = "Mr. Frank";
        bot.comboBox(0).setText(toFind);
        String toReplace = "Success";
        bot.comboBox(1).setText(toReplace);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String text = bb.textViewTools.getTextStripped();
        log.debug("Log Replaced Text " + text);
        ViewTestRunner.doPendingSWTWork();
        assertEquals(ViewTestRunner.stripNewlines(text).toLowerCase(),
                ViewTestRunner.stripNewlines(toReplace).toLowerCase());
    }

    @Test(enabled = false)
    public void findMathSpans() {
        BBTestRunner bb = new BBTestRunner(MATH_SPANS);
        bb.textViewTools.pressShortcut(SWT.CTRL, 'f');
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        String before = "?___";
        String after = "? ___";
        int charsBefore = bb.textViewBot.getText().length();

        bot.comboBox(0).setText(before);
        bot.comboBox(1).setText(after);
        FindReplaceBotHelper.clickReplace(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String text = bb.textViewTools.getTextStripped();
        log.debug("Log Replaced Text " + text);
        ViewTestRunner.doPendingSWTWork();
        int charsAfter = bb.textViewBot.getText().length();
        assertEquals(charsBefore + 1, charsAfter, text);
    }

}
