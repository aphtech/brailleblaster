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
package org.brailleblaster.perspectives.braille.ui;

import org.brailleblaster.GuiTest;
import org.brailleblaster.TestUtils;
import org.brailleblaster.settings.TableExceptions;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.testrunners.ViewTestRunner;
import org.brailleblaster.util.Notify;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import nu.xom.Document;

@Test(enabled = false)
@GuiTest
public class CorrectTranslationTest {

    private final String dollarSign = "\u0024";
    private final String blackTriangle = "\u25b2";
    private final String equalsSign = "=";
    private final String dotOne = "a";

    private final Document defined = TestXMLUtils.generateBookDoc("", "<p>" + dollarSign + "</p>");

    private final Document undefined = TestXMLUtils.generateBookDoc("", "<p>" + blackTriangle + "</p>");

    @Test(enabled = false)
    public void correctWordInLiblouisAscii() {
        setup();
        BBTestRunner bb = new BBTestRunner(defined);
        openCorrectTranslation(bb);
        bb.bot.activeShell().bot().text(0).setText(dollarSign);
        bb.bot.activeShell().bot().text(1).setText(equalsSign);
        bb.bot.activeShell().bot().button("Save Translation Locally").click();
        bb.manager.refresh();
        Assert.assertEquals(getBraille(bb), equalsSign);
        breakDown();
    }

    @Test(enabled = false)
    public void correctWordNotInLiblouisAscii() {
        setup();
        BBTestRunner bb = new BBTestRunner(undefined);
        openCorrectTranslation(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot.text(0).setText(blackTriangle);
        bot.text(1).setText(equalsSign);
        bot.button("Save Translation Locally").click();
        TestUtils.closeNotifyDialog(bot, "Success");
        openCorrectTranslation(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.radio("Define New Character").click();
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.text(0).setText(blackTriangle);
        bot.text(1).setText(equalsSign);
        bot.button("Save Translation Locally").click();
        Assert.assertEquals(getBraille(bb), equalsSign);
        breakDown();
    }

    @Test(enabled = false)
    public void defineCharacterAscii() {
        setup();
        BBTestRunner bb = new BBTestRunner(undefined);
        openCorrectTranslation(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot.radio("Define New Character").click();
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.text(0).setText(blackTriangle);
        bot.text(1).setText(equalsSign);
        bot.button("Save Translation Locally").click();
        Assert.assertEquals(getBraille(bb), equalsSign);
        breakDown();
    }

    @Test(enabled = false)
    public void correctDefinedCharacterAscii() {
        setup();
        BBTestRunner bb = new BBTestRunner(undefined);
        openCorrectTranslation(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot.text(0).setText(blackTriangle);
        bot.text(1).setText(equalsSign);
        bot.radio("Define New Character").click();
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.button("Save Translation Locally").click();
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.button("OK").click();

        openCorrectTranslation(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.text(0).setText(blackTriangle);
        String percentSign = "%";
        bot.text(1).setText(percentSign);
        bot.button("Save Translation Locally").click();
        Assert.assertEquals(getBraille(bb), percentSign);
        breakDown();
    }

    @Test(enabled = false)
    public void correctWordInLiblouisSixKey() {
        setup();
        BBTestRunner bb = new BBTestRunner(defined);
        openCorrectTranslation(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot.radio("Six Key").click();
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.text(0).setText(dollarSign);
        bot.text(1).setText(dotOne);
        bot.button("Save Translation Locally").click();
        Assert.assertEquals(getBraille(bb), dotOne);
        breakDown();
    }

    @Test(enabled = false)
    public void correctWordNotInLiblouisSixKey() {
        setup();
        BBTestRunner bb = new BBTestRunner(undefined);
        openCorrectTranslation(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot.radio("Six Key").click();
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.text(0).setText(blackTriangle);
        bot.text(1).setText(dotOne);
        bot.button("Save Translation Locally").click();
        TestUtils.closeNotifyDialog(bot, "Success");
        openCorrectTranslation(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.radio("Define New Character").click();
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.radio("Six Key").click();
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.text(0).setText(blackTriangle);
        bot.text(1).setText(dotOne);
        bot.button("Save Translation Locally").click();
        Assert.assertEquals(getBraille(bb), dotOne);
        breakDown();
    }

    @Test(enabled = false)
    public void defineCharacterSixKey() {
        setup();
        BBTestRunner bb = new BBTestRunner(undefined);
        openCorrectTranslation(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot.radio("Six Key").click();
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.radio("Define New Character").click();
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.text(0).setText(blackTriangle);
        bot.text(1).setText(dotOne);
        bot.button("Save Translation Locally").click();
        Assert.assertEquals(getBraille(bb), dotOne);
        breakDown();
    }

    @Test(enabled = false)
    public void correctDefinedCharacterSixKey() {
        setup();
        BBTestRunner bb = new BBTestRunner(defined);
        openCorrectTranslation(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot.radio("Define New Character").click();
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.radio("Six Key").click();
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.text(0).setText(dollarSign);
        bot.text(1).setText(dotOne);
        bot.button("Save Translation Locally").click();
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.button("OK").click();

        openCorrectTranslation(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.radio("Six Key").click();
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.text(0).setText(dollarSign);
        String dotOneTwo = "b";
        bot.text(1).setText(dotOneTwo);
        bot.button("Save Translation Locally").click();
        Assert.assertEquals(getBraille(bb), dotOneTwo);
        breakDown();
    }

    private String getBraille(BBTestRunner bb) {
        nu.xom.Node braille = bb.getDoc().getRootElement().getChild(1).getChild(0).getChild(1).getChild(2);
        return braille.getValue();
    }

    private void setup() {
        Notify.DEBUG_EXCEPTION = false;
    }

    //must reset DEBUG_EXCEPTION
    @AfterMethod(alwaysRun = true)
    private void breakDown() {
        Notify.DEBUG_EXCEPTION = true;
        TableExceptions.KEEP_TEST_DATA = false;
    }

    private void openCorrectTranslation(BBTestRunner bb) {
        TableExceptions.KEEP_TEST_DATA = true;
        ViewTestRunner.doPendingSWTWork();
        //refresh makes an entirely new styledText
        bb.updateViewReferences();
        bb.textViewTools.pressShortcut(SWT.CTRL, 't');
    }
}
