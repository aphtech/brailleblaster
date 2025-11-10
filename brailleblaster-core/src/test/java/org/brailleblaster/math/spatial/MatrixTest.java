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
package org.brailleblaster.math.spatial;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.brailleblaster.TestUtils;
import org.brailleblaster.math.mathml.MathModuleUtils;
import org.brailleblaster.math.spatial.MatrixConstants.Wide;
import org.brailleblaster.math.spatial.SpatialMathEnum.Passage;
import org.brailleblaster.math.template.TemplateConstants;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.settings.ui.TranslationSettingsTab;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.util.Notify.DebugException;
import org.testng.Assert;
import org.testng.annotations.Test;

import nu.xom.Document;

public class MatrixTest {

    private final Document twoParagraphs = TestXMLUtils.generateBookDoc("", "<p>Para1</p><p>Para2</p>");
    private final File MATRIX_DOC = (new File("src/test/resources/org/brailleblaster/printView/testMatrix.bbx"));
    final String[] skinnyNemeth = {"Para1", "", "`,(#0 #1 #2`,)", "`,(#3 #4 #5`,)", "`,(#6 #7 #8`,)", "", "Para2"};
    final String[] skinnyNemethEllipsis = {"Para1", "", "`,(#0 #1 '''`,)", "`,(#3 #4 #5`,)", "`,(#6 #7 #8`,)", "", "Para2"};
    final String[] blankBlockNemeth = {"Para1", "", "`,(#00000000 #11111111 #22222222`,)",
            "`,(0000      1111      2222     `,)", "`,(#33333333 #44444444 #55555555`,)",
            "`,(3333      4444      5555     `,)", "`,(#66666666 #77777777 #88888888`,)",
            "`,(6666      7777      8888     `,)", "", "Para2"};
    final String[] indentNemeth = {"Para1", "", "`,(#000000000000", "`,(#333333333333", "`,(#666666666666", "  #111111111111",
            "  #444444444444", "  #777777777777", "    #222222222222`,)", "    #555555555555`,)", "    #888888888888`,)", "",
            "Para2"};
    final String[] skinnyUeb = {"Para1", "", ",.<#j #a #b,.>", ",.<#c #d #e,.>", ",.<#f #g #h,.>", "", "Para2"};
    final String[] blankBlockUeb = {"Para1", "", ",.<#jjjjjjjj #aaaaaaaa #bbbbbbbb,.>", ",.<jjjj      aaaa      bbbb     ,.>",
            ",.<#cccccccc #dddddddd #eeeeeeee,.>", ",.<cccc      dddd      eeee     ,.>",
            ",.<#ffffffff #gggggggg #hhhhhhhh,.>", ",.<ffff      gggg      hhhh     ,.>", "", "Para2"};
    final String[] indentUeb = {"Para1", "", ",.<#jjjjjjjjjjjj", ",.<#cccccccccccc", ",.<#ffffffffffff", "  #aaaaaaaaaaaa", "  #dddddddddddd",
            "  #gggggggggggg", "    #bbbbbbbbbbbb,.>", "    #eeeeeeeeeeee,.>", "    #hhhhhhhhhhhh,.>", "", "Para2"};
    final String[] skinnyEntries = {"0", "1", "2", "3", "4", "5", "6", "7", "8"};
    final int wideTestChars = 12;
    final int tooWideForIndent = 40;

    public void openMatrixBuilder(BBTestRunner bb) {
        bb.openMenuItem(TopMenu.MATH, MathModuleUtils.SPATIAL_COMBO);
        bb.bot.activeShell().bot().menu(GridEditor.CONTAINER_TYPE_LABEL)
                .menu(SpatialMathEnum.SpatialMathContainers.MATRIX.prettyName).click();
        setNoPassage(bb);
    }

    public void setNoPassage(BBTestRunner bb) {
        if (!MathModuleUtils.isNemeth()) {
            bb.bot.activeShell().bot().menu(TemplateConstants.SETTINGS).menu(GridConstants.PASSAGE_TYPE).menu(Passage.NONE.getPrettyName())
                    .click();
        }
    }

    public void setWideType(BBTestRunner bb, Wide type) {
        bb.bot.activeShell().bot().menu(MatrixConstants.WIDE_LABEL).menu(type.getLabel()).click();
    }

    public void clickFillEllipsis(BBTestRunner bb) {
        if (!bb.bot.activeShell().bot().menu(MatrixConstants.MENU_SETTINGS).menu(MatrixConstants.ELLIPSIS_LABEL)
                .isChecked()) {
            bb.bot.activeShell().bot().menu(MatrixConstants.MENU_SETTINGS).menu(MatrixConstants.ELLIPSIS_LABEL).click();
        }
    }

    public void pressOk(BBTestRunner bb) {
        bb.bot.activeShell().bot().button(SpatialMathUtils.OK_LABEL).click();
    }

    public void fillSkinnyEntries(BBTestRunner bb) {
        for (int i = 0; i < skinnyEntries.length; i++) {
            bb.bot.activeShell().bot().styledText(i).setText(skinnyEntries[i]);
        }
    }

    public void fillSkinnyEllipsis(BBTestRunner bb) {
        for (int i = 0; i < skinnyEntries.length; i++) {
            if (i == 2) {
                bb.bot.activeShell().bot().styledText(i).setText("");
            } else {
                bb.bot.activeShell().bot().styledText(i).setText(skinnyEntries[i]);
            }
        }
    }

    public void fillWideEntries(BBTestRunner bb, int wideTestChars) {
        for (int i = 0; i < skinnyEntries.length; i++) {
            String cell = StringUtils.repeat(skinnyEntries[i].charAt(0), wideTestChars);
            bb.bot.activeShell().bot().styledText(i).setText(cell);
        }
    }

    @Test(enabled = false)
    public void nemethEllipsis() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        if (!MathModuleUtils.isNemeth()) {
            TestUtils.changeSettings(bb, TranslationSettingsTab.UEB_PLUS_NEMETH);
        }
        Assert.assertTrue(MathModuleUtils.isNemeth(), "Settings should be nemeth");
        bb.textViewTools.navigateToEndOfLine();
        openMatrixBuilder(bb);
        clickFillEllipsis(bb);
        fillSkinnyEllipsis(bb);
        pressOk(bb);
        for (int i = 0; i < skinnyNemethEllipsis.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, skinnyNemethEllipsis[i]);
        }
    }

    @Test(enabled = false)
    public void skinnyMatrixNemeth() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        if (!MathModuleUtils.isNemeth()) {
            TestUtils.changeSettings(bb, TranslationSettingsTab.UEB_PLUS_NEMETH);
        }
        Assert.assertTrue(MathModuleUtils.isNemeth(), "Settings should be nemeth");
        bb.textViewTools.navigateToEndOfLine();
        openMatrixBuilder(bb);
        fillSkinnyEntries(bb);
        pressOk(bb);
        for (int i = 0; i < skinnyNemeth.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, skinnyNemeth[i]);
        }
    }

    @Test(enabled = false)
    public void skinnyMatrixUeb() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        if (MathModuleUtils.isNemeth()) {
            TestUtils.changeSettings(bb, TranslationSettingsTab.UEB);
        }
        Assert.assertFalse(MathModuleUtils.isNemeth(), "Settings should be Ueb");
        bb.textViewTools.navigateToEndOfLine();
        openMatrixBuilder(bb);
        fillSkinnyEntries(bb);
        pressOk(bb);
        for (int i = 0; i < skinnyUeb.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, skinnyUeb[i]);
        }
    }

    @Test
    public void blankBlockMatrixNemeth() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        if (!MathModuleUtils.isNemeth()) {
            TestUtils.changeSettings(bb, TranslationSettingsTab.UEB_PLUS_NEMETH);
        }
        Assert.assertTrue(MathModuleUtils.isNemeth(), "Settings should be nemeth");
        bb.textViewTools.navigateToEndOfLine();
        openMatrixBuilder(bb);
        setWideType(bb, Wide.BLOCK_BLANK);
        fillWideEntries(bb, wideTestChars);
        pressOk(bb);
        for (int i = 0; i < blankBlockNemeth.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, blankBlockNemeth[i]);
        }
    }

    @Test(enabled = false)
    public void blankBlockMatrixUeb() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        if (MathModuleUtils.isNemeth()) {
            TestUtils.changeSettings(bb, TranslationSettingsTab.UEB);
        }
        Assert.assertFalse(MathModuleUtils.isNemeth(), "Settings should be ueb");
        bb.textViewTools.navigateToEndOfLine();
        openMatrixBuilder(bb);
        setWideType(bb, Wide.BLOCK_BLANK);
        fillWideEntries(bb, wideTestChars);
        pressOk(bb);
        for (int i = 0; i < blankBlockUeb.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, blankBlockUeb[i]);
        }
    }

    @Test(enabled = false)
    public void indentMatrixNemeth() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        if (!MathModuleUtils.isNemeth()) {
            TestUtils.changeSettings(bb, TranslationSettingsTab.UEB_PLUS_NEMETH);
        }
        Assert.assertTrue(MathModuleUtils.isNemeth(), "Settings should be nemeth");
        bb.textViewTools.navigateToEndOfLine();
        openMatrixBuilder(bb);
        setWideType(bb, Wide.INDENT_COLUMN);
        fillWideEntries(bb, wideTestChars);
        pressOk(bb);
        for (int i = 0; i < indentNemeth.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i);
            Assert.assertEquals(st, indentNemeth[i]);
        }
    }

    @Test(enabled = false)
    public void indentMatrixUeb() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        if (MathModuleUtils.isNemeth()) {
            TestUtils.changeSettings(bb, TranslationSettingsTab.UEB);
        }
        Assert.assertFalse(MathModuleUtils.isNemeth(), "Settings should be ueb");
        bb.textViewTools.navigateToEndOfLine();
        openMatrixBuilder(bb);
        setWideType(bb, Wide.INDENT_COLUMN);
        fillWideEntries(bb, wideTestChars);
        pressOk(bb);
        for (int i = 0; i < indentUeb.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i);
            Assert.assertEquals(st, indentUeb[i]);
        }
    }

    @Test(enabled = false)
    public void indentMatrixTooWide() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        openMatrixBuilder(bb);
        setWideType(bb, Wide.INDENT_COLUMN);
        fillWideEntries(bb, tooWideForIndent);
        try {
            pressOk(bb);
            Assert.fail("You have gone too far, an exception should have been thrown in debug mode");
        } catch (DebugException e) {
            Assert.assertTrue(e.getMessage().contains(MatrixConstants.FORMAT_INDENT_TOO_WIDE_WARNING),
                    "Actual message is " + e.getMessage());
        }
    }

    // @Test
    public void loadFromDoc() {
        BBTestRunner bb = new BBTestRunner(MATRIX_DOC);
        openMatrixBuilder(bb);
        for (int i = 0; i < skinnyEntries.length; i++) {
            Assert.assertEquals(bb.bot.activeShell().bot().styledText(i).getText(), skinnyEntries[i]);
        }
    }

}
