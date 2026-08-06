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
package org.brailleblaster.mathml;

import org.brailleblaster.TestFiles;
import org.brailleblaster.math.mathml.NemethIndicators;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import nu.xom.Document;

public class NemethIndicatorsTest {
    private static final Logger log = LoggerFactory.getLogger(NemethIndicatorsTest.class);
    final String wizard1 = "We're off to see the wizard.";
    final String wizard2 = "The wonderful wizard of Oz.";
    final String wizard3 = "We hear he is a whiz of a wiz";
    final int halfway = 13;
    private final Document one_paragraph = TestXMLUtils.generateBookDoc("", "<p>" + wizard1 + "</p>");
    private final Document three_paragraph = TestXMLUtils.generateBookDoc("",
            "<p>" + wizard1 + "</p>" + "<p>" + wizard2 + "</p>" + "<p>" + wizard3 + "</p>");

    public void clickInline(BBTestRunner bb) {
        NemethIndicators.INSTANCE.inline(bb.manager);
//		bb.openMenuItem(MenuManager.TopMenu.MATH, MathModule.NEMETH_TOGGLE);
    }

    public void clickBlock(BBTestRunner bb) {
        NemethIndicators.INSTANCE.block(bb.manager);
//		bb.openMenuItem(MenuManager.TopMenu.MATH, MathModule.NEMETH_TOGGLE);
    }

    @Test(enabled = false)
    public void inline_beg_block_one_line() {
        log.debug("inline beginning of block");
        BBTestRunner bb = new BBTestRunner(one_paragraph);
        bb.textViewTools.selectRight(halfway);
        String w1 = wizard1.substring(0, halfway);
        String w2 = wizard1.substring(halfway);
        clickInline(bb);
        String s = bb.textViewTools.getTextStripped();
        Assert.assertEquals(s, NemethIndicators.INLINE_BEGINNING_INDICATOR + w1 + NemethIndicators.INLINE_END_INDICATOR + w2);
    }

    @Test(enabled = false)
    public void inline_end_block_one_line() {
        log.debug("inline end of block");
        BBTestRunner bb = new BBTestRunner(one_paragraph);
        bb.textViewTools.navigate(halfway);
        bb.textViewTools.selectToEndOfLine();
        String w1 = wizard1.substring(0, halfway);
        String w2 = wizard1.substring(halfway);
        clickInline(bb);
        String s = bb.textViewTools.getTextStripped();
        Assert.assertEquals(s, w1 + NemethIndicators.INLINE_BEGINNING_INDICATOR + w2 + NemethIndicators.INLINE_END_INDICATOR);
    }

    @Test(enabled = false)
    public void inline_all_block_one_line() {
        log.debug("inline all of block");
        BBTestRunner bb = new BBTestRunner(one_paragraph);
        bb.textViewTools.selectToEndOfLine();
        clickInline(bb);
        String s = bb.textViewTools.getTextStripped();
        Assert.assertEquals(s, NemethIndicators.INLINE_BEGINNING_INDICATOR + wizard1 + NemethIndicators.INLINE_END_INDICATOR);
    }

    @Test(enabled = false)
    public void inline_beg_block_mult_lines() {
        log.debug("inline beginning of block multi");
        BBTestRunner bb = new BBTestRunner(three_paragraph);
        bb.textViewTools.selectToEndOfLine();
        clickInline(bb);
        String s = bb.textViewTools.getTextStripped();
        Assert.assertEquals(s,
                NemethIndicators.INLINE_BEGINNING_INDICATOR + wizard1 + NemethIndicators.INLINE_END_INDICATOR + wizard2 + wizard3);
    }

    @Test(enabled = false)
    public void inline_end_block_mult_lines() {
        log.debug("inline end of block multi");
        BBTestRunner bb = new BBTestRunner(three_paragraph);
        bb.textViewTools.navigateToLine(2);
        bb.textViewTools.selectToEndOfLine();
        clickInline(bb);
        String s = bb.textViewTools.getTextStripped();
        Assert.assertEquals(s,
                wizard1 + wizard2 + NemethIndicators.INLINE_BEGINNING_INDICATOR + wizard3 + NemethIndicators.INLINE_END_INDICATOR);
    }

    @Test(enabled = false)
    public void inline_all_block_mult_lines() {
        log.debug("inline beginning of block multi");
        BBTestRunner bb = new BBTestRunner(three_paragraph);
        bb.textViewTools.selectToEndOf(wizard3);
        clickInline(bb);
        String s = bb.textViewTools.getTextStripped();
        Assert.assertEquals(s,
                NemethIndicators.INLINE_BEGINNING_INDICATOR + wizard1 + wizard2 + wizard3 + NemethIndicators.INLINE_END_INDICATOR);
    }

    @Test(enabled = false)
    public void block_beg_block_one_line() {
        log.debug("block beginning of block");
        BBTestRunner bb = new BBTestRunner(one_paragraph);
        bb.textViewTools.selectRight(halfway);
        clickBlock(bb);
        String s1 = bb.textViewBot.getTextOnLine(0);
        Assert.assertEquals(s1, NemethIndicators.BEGINNING_INDICATOR.trim());
        String s2 = bb.textViewBot.getTextOnLine(1);
        Assert.assertEquals(s2, wizard1);
        String s3 = bb.textViewBot.getTextOnLine(2);
        Assert.assertEquals(s3, NemethIndicators.END_INDICATOR.trim());
    }

    @Test(enabled = false)
    public void block_end_block_one_line() {
        log.debug("block end of block");
        BBTestRunner bb = new BBTestRunner(one_paragraph);
        bb.textViewTools.navigate(halfway);
        bb.textViewTools.selectToEndOfLine();
        clickBlock(bb);
        String s1 = bb.textViewBot.getTextOnLine(0);
        Assert.assertEquals(s1, NemethIndicators.BEGINNING_INDICATOR.trim());
        String s2 = bb.textViewBot.getTextOnLine(1);
        Assert.assertEquals(s2, wizard1);
        String s3 = bb.textViewBot.getTextOnLine(2);
        Assert.assertEquals(s3, NemethIndicators.END_INDICATOR.trim());
    }

    @Test(enabled = false)
    public void block_all_block_one_line() {
        log.debug("block all of block");
        BBTestRunner bb = new BBTestRunner(one_paragraph);
        bb.textViewTools.selectToEndOf(wizard1);
        clickBlock(bb);
        String s1 = bb.textViewBot.getTextOnLine(0);
        Assert.assertEquals(s1, NemethIndicators.BEGINNING_INDICATOR.trim());
        String s2 = bb.textViewBot.getTextOnLine(1);
        Assert.assertEquals(s2, wizard1);
        String s3 = bb.textViewBot.getTextOnLine(2);
        Assert.assertEquals(s3, NemethIndicators.END_INDICATOR.trim());
    }

    @Test(enabled = false)
    public void block_beg_block_mult_lines() {
        log.debug("block beginning of block multi");
        BBTestRunner bb = new BBTestRunner(three_paragraph);
        bb.textViewTools.selectToEndOf(wizard1);
        clickBlock(bb);
        String s1 = bb.textViewBot.getTextOnLine(0);
        Assert.assertEquals(s1, NemethIndicators.BEGINNING_INDICATOR.trim());
        String s2 = bb.textViewBot.getTextOnLine(1);
        Assert.assertEquals(s2, wizard1);
        String s3 = bb.textViewBot.getTextOnLine(2);
        Assert.assertEquals(s3, NemethIndicators.END_INDICATOR.trim());
        String s4 = bb.textViewBot.getTextOnLine(3);
        Assert.assertEquals(s4, wizard2);
        String s5 = bb.textViewBot.getTextOnLine(4);
        Assert.assertEquals(s5, wizard3);
    }

    @Test(enabled = false)
    public void block_end_block_mult_lines() {
        log.debug("block end of block multi");
        BBTestRunner bb = new BBTestRunner(three_paragraph);
        bb.textViewTools.navigateToLine(2);
        bb.textViewTools.selectToEndOf(wizard3);
        clickBlock(bb);
        String s1 = bb.textViewBot.getTextOnLine(0);
        Assert.assertEquals(s1, wizard1);
        String s2 = bb.textViewBot.getTextOnLine(1);
        Assert.assertEquals(s2, wizard2);
        String s3 = bb.textViewBot.getTextOnLine(2);
        Assert.assertEquals(s3, NemethIndicators.BEGINNING_INDICATOR.trim());
        String s4 = bb.textViewBot.getTextOnLine(3);
        Assert.assertEquals(s4, wizard3);
        String s5 = bb.textViewBot.getTextOnLine(4);
        Assert.assertEquals(s5, NemethIndicators.END_INDICATOR.trim());
    }

    @Test(enabled = false)
    public void block_all_block_mult_lines() {
        log.debug("block all of block multi");
        BBTestRunner bb = new BBTestRunner(three_paragraph);
        bb.textViewTools.selectToEndOf(wizard3);
        clickBlock(bb);
        String s1 = bb.textViewBot.getTextOnLine(0);
        Assert.assertEquals(s1, NemethIndicators.BEGINNING_INDICATOR.trim());
        String s2 = bb.textViewBot.getTextOnLine(1);
        Assert.assertEquals(s2, wizard1);
        String s3 = bb.textViewBot.getTextOnLine(2);
        Assert.assertEquals(s3, wizard2);
        String s4 = bb.textViewBot.getTextOnLine(3);
        Assert.assertEquals(s4, wizard3);
        String s5 = bb.textViewBot.getTextOnLine(4);
        Assert.assertEquals(s5, NemethIndicators.END_INDICATOR.trim());
    }

    public void clickAddIndicators(BBTestRunner bb) {
        NemethIndicators.INSTANCE.inline(bb.manager);
    }

    @Test(enabled = false)
    public void list() {
        BBTestRunner bb = new BBTestRunner(TestFiles.lists);
        bb.textViewTools.selectToEndOf(TestFiles.wizard3);
        clickAddIndicators(bb);
        String s1 = bb.textViewBot.getTextOnLine(0).substring(0, 2);
        Assert.assertEquals(s1, NemethIndicators.BEGINNING_INDICATOR.trim());
        String s3 = bb.textViewBot.getTextOnLine(2);
        s3 = s3.substring(s3.length() - 2);
        Assert.assertEquals(s3, NemethIndicators.END_INDICATOR.trim());
    }

    @Test(enabled = false)
    public void math() {
        BBTestRunner bb = new BBTestRunner(TestFiles.simpleMath);
        bb.textViewTools.selectToEndOfLine();
        clickAddIndicators(bb);
        String s1 = bb.textViewBot.getTextOnLine(0);
        Assert.assertEquals(s1,
                NemethIndicators.BEGINNING_INDICATOR + TestFiles.mathPrintView + NemethIndicators.END_INDICATOR);
    }

    @Test(enabled = false)
    public void text() {
        BBTestRunner bb = new BBTestRunner(TestFiles.text);
        bb.textViewTools.selectToEndOf(TestFiles.wizard1);
        clickAddIndicators(bb);
        String s1 = bb.textViewBot.getTextOnLine(0);
        Assert.assertEquals(s1,
                NemethIndicators.BEGINNING_INDICATOR + TestFiles.wizard1 + NemethIndicators.END_INDICATOR);
    }

    @Test(enabled = false)
    public void mathText() {
        BBTestRunner bb = new BBTestRunner(TestFiles.textMath);
        bb.textViewTools.navigateToLine(2);
        bb.textViewTools.selectRight(TestFiles.wizard3.length() + 2);
        clickAddIndicators(bb);
        String s3 = bb.textViewBot.getTextOnLine(2);
        Assert.assertEquals(s3, NemethIndicators.BEGINNING_INDICATOR + TestFiles.wizard3);
        String s4 = bb.textViewBot.getTextOnLine(3);
        Assert.assertEquals(s4, "x" + NemethIndicators.END_INDICATOR + " + 2");
    }

    @Test(enabled = false)
    public void textList() {
        BBTestRunner bb = new BBTestRunner(TestFiles.listText);
        bb.textViewTools.navigateToLine(2);
        bb.textViewTools
                .selectRight(TestFiles.wizard3.length() + TestFiles.wizard1.length() + System.lineSeparator().length());
        clickAddIndicators(bb);
        String s3 = bb.textViewBot.getTextOnLine(2);
        Assert.assertEquals(s3, NemethIndicators.BEGINNING_INDICATOR + TestFiles.wizard3);
        String s4 = bb.textViewBot.getTextOnLine(4);
        Assert.assertEquals(s4, TestFiles.wizard1 + NemethIndicators.END_INDICATOR);
    }

    @Test(enabled = false)
    public void listMath() {
        BBTestRunner bb = new BBTestRunner(TestFiles.listMath);
        bb.textViewTools.navigateToLine(1);
        bb.textViewTools.selectToEndOf(TestFiles.mathPrintView);
        clickAddIndicators(bb);
        String s2 = bb.textViewBot.getTextOnLine(1);
        Assert.assertEquals(s2, NemethIndicators.BEGINNING_INDICATOR + TestFiles.wizard2);
        String s4 = bb.textViewBot.getTextOnLine(4);
        Assert.assertEquals(s4, TestFiles.mathPrintView + NemethIndicators.END_INDICATOR);
    }
}
