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

import nu.xom.*;
import org.brailleblaster.BBIni;
import org.brailleblaster.TestFiles;
import org.brailleblaster.TestUtils;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.frontmatter.TOCBuilderBBX;
import org.brailleblaster.math.ascii.ASCIIMathEditorDialog;
import org.brailleblaster.math.ascii.MathDialogSettings.CATEGORIES;
import org.brailleblaster.math.mathml.MathModuleUtils;
import org.brailleblaster.math.mathml.NumericSeries;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.settings.ui.TranslationSettingsTab;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.util.Notify;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.utils.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

public class MathMLASCIIEditTest {

    private static final Logger log = LoggerFactory.getLogger(MathMLASCIIEditTest.class);
    private final Document plainText = TestXMLUtils.generateBookDoc("", "<p>x+2</p>");
    private final Document abc = TestXMLUtils.generateBookDoc("",
            "<p><m:math ><m:mrow><m:mfrac><m:mrow><m:mi>a</m:mi></m:mrow><m:mrow>"
                    + "<m:mi>b</m:mi></m:mrow></m:mfrac><m:mo>=</m:mo><m:mi>c</m:mi></m:mrow></m:math></p>");
    private final Document nonMath = TestXMLUtils.generateBookDoc("", "<p>x + 2</p>");
    final String abcs = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";
    private final Document longString = TestXMLUtils.generateBookDoc("", "<p>" + abcs + "</p>");
    private final Document blank = TestXMLUtils.generateBookDoc("", "<p></p>");
    private final Document dollar_fifty = TestXMLUtils.generateBookDoc("",
            "<p>In December of 2003, the average price for a gallon of regular gas in the United States was $1.50.</p>");
    private final Document two_lines = TestXMLUtils.generateBookDoc("", "<p>line_one</p>" + "<p>line_two</p>");
    private final Document pre_calc = TestXMLUtils.generateBookDoc("", "<p>" + "<em>w</em>" + "<sub>1</sub>"
            + "<em>x</em>" + "<sub>1</sub>" + "=" + "<em>w</em>" + "<sub>2</sub>" + "<em>x</em>" + "<sub>2</sub>"
            + "</p>" + "<list><li><strong>Dimensions of a Can</strong>A cylindrical can has a volume of 40π cm"
            + "<sup>3</sup> and is 10 cm tall.  What is its diameter? ["
            + "<em>Hint: </em> Use the volume formula listed on the inside cover of this book.]" + "</li></list>");
    private final String numeric_spaces = "1" + NumericSeries.BRAILLE_DOT_5 + "2" + NumericSeries.BRAILLE_DOT_5 + "3"
            + NumericSeries.BRAILLE_DOT_5 + "4" + NumericSeries.BRAILLE_DOT_5 + "5" + NumericSeries.BRAILLE_DOT_5 + "6"
            + NumericSeries.BRAILLE_DOT_5 + "7";
    private final String no_numeric_spaces = "1 2 3 4 5 6 7";
    private final Document no_num = TestXMLUtils.generateBookDoc("", "<p>" + no_numeric_spaces + "</p>");
    private final Document num = TestXMLUtils.generateBookDoc("", "<p>" + numeric_spaces + "</p>");

    @Test(enabled = false)
    public void makeNumeric() {
        BBTestRunner bb = new BBTestRunner(no_num);
        bb.textViewTools.selectToEndOfLine();
        clickToggleNumericSeries(bb);
        String s = bb.textViewTools.getTextStripped();
        Assert.assertEquals(s, numeric_spaces);
        s = bb.brailleViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s, "#a\"b\"c\"d\"e\"f\"g");
    }

    @Test(enabled = false)
    public void removeNumeric() {
        BBTestRunner bb = new BBTestRunner(num);
        bb.textViewTools.selectToEndOfLine();
        clickToggleNumericSeries(bb);
        String s = bb.textViewTools.getTextStripped();
        Assert.assertEquals(s, no_numeric_spaces);
        s = bb.brailleViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s, "#a #b #c #d #e #f #g");
    }

    public void clickMakeMath(BBTestRunner bb) {
        bb.openMenuItem(TopMenu.MATH, MathModuleUtils.MATH_TOGGLE);
    }

    public void clickOpenAsciiEditor(BBTestRunner bb) {
        bb.openMenuItem(TopMenu.MATH, MathModuleUtils.ASCII_EDITOR);
    }

    public void clickToggleNumericSeries(BBTestRunner bb) {
        bb.openMenuItem(TopMenu.MATH, MathModuleUtils.NUMERIC_SERIES);
    }

    public void clickNemethIndicators(BBTestRunner bb) {
        bb.openMenuItem(TopMenu.MATH, MathModuleUtils.NEMETH_TOGGLE);
    }

    @Test(enabled = false)
    public void rt_5917_ueb_to_ueb_plus_nemeth() {
        BBTestRunner bb = new BBTestRunner(two_lines);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);// ueb
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);

        bb.updateViewReferences();
        bb.textViewTools.navigate(5);
        bb.textViewTools.selectRight(2);
        clickMakeMath(bb);
        String s = bb.textViewTools.getTextStripped();
        Assert.assertEquals(s, "line_oneline_two");
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        s = bb.textViewTools.getTextStripped();
        Assert.assertEquals(s, "line_oneline_two");

        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        s = bb.textViewTools.getTextStripped();
        Assert.assertEquals(s, "line_oneline_two");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void rt5762_pre_calc_book_sup_and_subs() {
        BBTestRunner bb = new BBTestRunner(pre_calc);
        bb.textViewTools.navigateToText("w1");
        bb.textViewTools.selectRight(9);

        clickMakeMath(bb);
        String s1 = bb.textViewBot.getTextOnLine(0);
        Assert.assertEquals(s1, "w1x1=w2x2");

        bb.textViewBot.navigateTo(3, 16);
        bb.textViewTools.selectRight(7);

        clickMakeMath(bb);
        String s2 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s2, "has a volume of 40π cm3 and is 10 cm");
    }

    @Test(enabled = false)
    public void rt5762_pre_calc_book_subs() {
        BBTestRunner bb = new BBTestRunner(pre_calc);
        bb.textViewTools.navigateToText("w1");
        bb.textViewTools.selectRight(9);

        clickMakeMath(bb);
        String s1 = bb.textViewBot.getTextOnLine(0);
        Assert.assertEquals(s1, "w1x1=w2x2");
    }

    @Test(enabled = false)
    public void rt5762_pre_calc_book_sups() {
        BBTestRunner bb = new BBTestRunner(pre_calc);

        bb.textViewBot.navigateTo(3, 16);
        bb.textViewTools.selectRight(7);

        clickMakeMath(bb);
        String s2 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s2, "has a volume of 40π cm3 and is 10 cm");
    }

    @Test(enabled = false)
    public void rt6005_lim_alpha_close_bracket_single_char() {
        BBTestRunner bb = new BBTestRunner(blank);

        bb.textViewTools.typeText("lim_(");
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.GREEK);
        clickButtonWithTooltop(bb, "alpha");
        clickInsert(bb);
        bb.textViewTools.pressKey(SWT.ESC, 1);
        bb.textViewTools.typeText(")");

        bb.textViewTools.pressKey(SWT.CR, 1);
        String s1 = bb.textViewBot.getTextOnLine(0);
        Assert.assertEquals(s1, "lim_(alpha)");

        bb.textViewTools.navigate(0);
        bb.textViewTools.selectToEndOfLine();

        clickMakeMath(bb);
        String s = bb.textViewTools.getTextStripped();
        Assert.assertEquals(s, "lim_(alpha)");
    }

    @Test(enabled = false)
    public void rt6005_lim_alpha_close_bracket_double_char() {
        BBTestRunner bb = new BBTestRunner(blank);

        bb.textViewTools.typeText("lim_(");
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.GREEK);
        clickButtonWithTooltop(bb, "alpha");
        clickInsert(bb);
        bb.textViewTools.pressKey(SWT.ESC, 1);
        bb.textViewTools.typeText("))");

        bb.textViewTools.pressKey(SWT.CR, 1);
        String s1 = bb.textViewBot.getTextOnLine(0);
        Assert.assertEquals(s1, "lim_(alpha))");

        bb.textViewTools.navigate(0);
        bb.textViewTools.selectToEndOfLine();

        clickMakeMath(bb);
        String s = bb.textViewTools.getTextStripped();
        Assert.assertEquals(s, "lim_(alpha))");
    }

    @Test(enabled = false)
    public void rt5765_math_and_grave_accent() {
        BBTestRunner bb = new BBTestRunner(blank);
        BBIni.setDebuggingEnabled();
        bb.textViewTools.typeText("`");
        bb.textViewTools.navigate(0);
        bb.textViewTools.selectToEndOfLine();
        clickMakeMath(bb);
        String s = bb.textViewTools.getTextStripped();
        Assert.assertEquals(s, "`");
    }

    @Test(enabled = false)
    public void rt5675_math_and_toc() {
        BBTestRunner bb = new BBTestRunner(blank);
        BBIni.setDebuggingEnabled();
        if (!TOCBuilderBBX.isEnabled(bb.manager)) {
            bb.openMenuItem(TopMenu.TOOLS, "TOC Builder");
        }
        if (!bb.bot.checkBox("Find Page | Page Prefix").isChecked()) {
            bb.bot.checkBox("Find Page | Page Prefix");
        }
        bb.textViewTools.typeText("beta");
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.GREEK);
        clickButtonWithTooltop(bb, "alpha");
        // Greek Letter alpha
        clickInsert(bb);
        String text = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(text, "betaalpha");
        bb.textViewTools.navigate(1);
        Notify.DEBUG_EXCEPTION = false;
        bb.textViewTools.pressKey(SWT.F4, 1);
        Notify.DEBUG_EXCEPTION = true;
        String s = bb.bot.activeShell().bot().activeShell().getText();
        Assert.assertEquals(s, "Error");
    }

    @Test(enabled = false)
    public void rt5679_math_direct_translate_and_delete() {
        BBTestRunner bb = new BBTestRunner(blank);
        BBIni.setDebuggingEnabled();
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.GREEK);
        clickButtonWithTooltop(bb, "alpha");
        // Greek Letter alpha

        bb.textViewTools.navigate(0);
        bb.textViewTools.selectToEndOfLine();
        bb.bot.menu("Tools").menu("Change Translation").menu("Direct").click();

        bb.textViewTools.navigate(0);
        bb.textViewTools.selectToEndOfLine();
        bb.textViewTools.pressKey(SWT.DEL, 1);

        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "");
    }

    @Test(enabled = false)
    public void rt5736_math_and_image_placeholder() {
        BBTestRunner bb = new BBTestRunner(two_lines);
        BBIni.setDebuggingEnabled();
        bb.textViewTools.selectRight(18);
        clickMakeMath(bb);
        bb.textViewTools.navigateToLine(1);
        bb.openMenuItem(TopMenu.INSERT, "Image Placeholder");

        TestUtils.refreshReturnActiveBot(bb).text(0).setText(String.valueOf(10));
        TestUtils.refreshReturnActiveBot(bb).button("Submit").click();

        String s = bb.textViewBot.getTextOnLine(11);
        Assert.assertEquals(s, "line_two");
    }

    @Test(enabled = false)
    public void rt5674_math_and_prose() {
        BBTestRunner bb = new BBTestRunner(two_lines);
        BBIni.setDebuggingEnabled();
        bb.textViewTools.selectRight(20);
        bb.openMenuItem(TopMenu.TOOLS, "Line Number Tools");
        bb.clickButton("Wrap Prose (CTRL + F2)");

        bb.textViewTools.navigate(0);
        bb.textViewTools.selectToEndOfLine();
        clickMakeMath(bb);

        bb.textViewTools.pressKey(SWT.F2, 1);
    }

    @Test(enabled = false)
    public void rt5764insertComplexFractions() {
        BBTestRunner bb = new BBTestRunner(blank);
        BBIni.setDebuggingEnabled();
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.EXAMPLES);
        clickButtonWithTooltop(bb, "complex fraction");
        clickInsert(bb);
        Position pos = bb.textViewBot.cursorPosition();
        Assert.assertEquals(pos.column, 12);
    }

    public void rt5766_math_last_node_delete_all() {
        BBTestRunner bb = new BBTestRunner(two_lines);
        BBIni.setDebuggingEnabled();

        bb.textViewTools.navigateToLine(1);
        bb.textViewTools.selectToEndOfLine();
        clickMakeMath(bb);
        bb.textViewTools.navigate(0);
        bb.textViewTools.selectRight(25);
        bb.textViewTools.pressKey(SWT.DEL, 1);
    }

    public void rt5766_math_first_node_delete_all() {
        BBTestRunner bb = new BBTestRunner(two_lines);
        BBIni.setDebuggingEnabled();

        bb.textViewTools.selectToEndOfLine();
        clickMakeMath(bb);
        bb.textViewTools.navigate(0);
        bb.textViewTools.selectRight(25);
        bb.textViewTools.pressKey(SWT.DEL, 1);
    }

    public void rt5766_math_half_last_node_delete_all() {
        BBTestRunner bb = new BBTestRunner(two_lines);
        BBIni.setDebuggingEnabled();

        bb.textViewTools.navigate(12);
        bb.textViewTools.selectToEndOfLine();
        clickMakeMath(bb);
        bb.textViewTools.navigate(0);
        bb.textViewTools.selectRight(25);
        bb.textViewTools.pressKey(SWT.DEL, 1);
    }

    public void rt5766_math_half_first_node_delete_all() {
        BBTestRunner bb = new BBTestRunner(two_lines);
        BBIni.setDebuggingEnabled();

        bb.textViewTools.selectRight(3);
        clickMakeMath(bb);
        bb.textViewTools.navigate(0);
        bb.textViewTools.selectRight(20);
        bb.textViewTools.pressKey(SWT.DEL, 1);
    }

    @Test(enabled = false)
    public void rt5825typeInsertMathTypePressEnterInReverseOrder() {
        BBTestRunner bb = new BBTestRunner(blank);

        bb.textViewTools.typeText(")");
        bb.textViewTools.navigate(0);
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.GREEK);
        clickButtonWithTooltop(bb, "alpha");
        clickInsert(bb);
        bb.textViewTools.navigate(0);
        bb.textViewTools.typeText("lim_(");
        bb.textViewTools.navigateToEndOfLine();
        bb.textViewTools.pressKey(SWT.CR, 1);
        String s1 = bb.textViewBot.getTextOnLine(0);
        Assert.assertEquals(s1, "lim_(alpha)");
    }

    @Test(enabled = false)
    public void rt5825typeInsertMathTypePressEnter() {
        BBTestRunner bb = new BBTestRunner(blank);

        bb.textViewTools.typeText("lim_(");
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.GREEK);
        clickButtonWithTooltop(bb, "alpha");
        clickInsert(bb);
        bb.textViewTools.pressKey(SWT.ESC, 1);

        bb.textViewTools.pressKey(SWT.CR, 1);
        String s1 = bb.textViewBot.getTextOnLine(0);
        Assert.assertEquals(s1, "lim_(alpha");
    }

    @Test(enabled = false)
    /*
     * Will fail on Windows
     */
    public void rt5822makeMathEndOfNodeWithLineBreaks() {
        BBTestRunner bb = new BBTestRunner(dollar_fifty);
        BBIni.setDebuggingEnabled();
        bb.textViewTools.navigate(94);
        bb.textViewTools.selectToEndOfLine();
        clickMakeMath(bb);
        String s1 = bb.textViewBot.getTextOnLine(0);
        String s2 = bb.textViewBot.getTextOnLine(1);
        String s3 = bb.textViewBot.getTextOnLine(2);
        Assert.assertEquals(s1, "In December of 2003, the average price for ");
        Assert.assertEquals(s2, "a gallon of regular gas in the United States ");
        Assert.assertEquals(s3, "was $1.50.");
    }

    @Test(enabled = false)
    public void makeAllMath() {
        BBTestRunner bb = new BBTestRunner(plainText);
        BBIni.setDebuggingEnabled();
        String text = bb.textViewTools.getTextStripped();
        Assert.assertEquals(text, "x+2");
        bb.textViewTools.selectToEndOfLine();
        clickMakeMath(bb);
        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "x+2");
    }

    @Test(enabled = false)
    public void removeMath() {
        BBTestRunner bb = new BBTestRunner(TestFiles.simpleMath);
        bb.textViewTools.selectToEndOfLine();
        clickMakeMath(bb);
        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "x + 2");
    }

    @Test(enabled = false)
    public void makeBeginningMath() {
        BBTestRunner bb = new BBTestRunner(plainText);
        BBIni.setDebuggingEnabled();
        String text = bb.textViewTools.getTextStripped();
        Assert.assertEquals(text, "x+2");
        bb.textViewTools.selectRight(1);
        clickMakeMath(bb);
        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "x+2");
    }

    @Test(enabled = false)
    public void makeEndMath() {
        BBTestRunner bb = new BBTestRunner(plainText);
        BBIni.setDebuggingEnabled();
        String text = bb.textViewTools.getTextStripped();
        Assert.assertEquals(text, "x+2");
        bb.textViewTools.navigate(2);
        bb.textViewTools.selectRight(1);
        clickMakeMath(bb);
        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "x+2");
    }

    @Test(enabled = false)
    public void makeMiddleMath() {
        BBTestRunner bb = new BBTestRunner(plainText);
        BBIni.setDebuggingEnabled();
        String text = bb.textViewTools.getTextStripped();
        Assert.assertEquals(text, "x+2");
        bb.textViewTools.navigate(1);
        bb.textViewTools.selectRight(1);
        clickMakeMath(bb);
        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "x+2");

        Nodes nodes = bb.getDoc().getRootElement().query("descendant::*[local-name()='INLINE']");
        Assert.assertEquals(nodes.size(), 1);
    }

    @Test(enabled = false)
    public void makeMathAlreadyHalfMath() {
        BBTestRunner bb = new BBTestRunner(plainText);
        BBIni.setDebuggingEnabled();
        String text = bb.textViewTools.getTextStripped();
        Assert.assertEquals(text, "x+2");
        bb.textViewTools.selectRight(1);
        clickMakeMath(bb);
        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "x+2");

        bb.textViewTools.navigate(0);
        bb.textViewTools.selectToEndOfLine();
        clickMakeMath(bb);
        s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "x+2");
    }

    @Test(enabled = false)
    public void insertMathWhiteSpace() {
        BBTestRunner bb = new BBTestRunner(blank);
        BBIni.setDebuggingEnabled();
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.GREEK);
        clickButtonWithTooltop(bb, "alpha");
        // Greek Letter alpha
        clickInsert(bb);
        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "alpha");
    }

    @Test(enabled = false)
    public void rt5764CursorAfter3MathInserts() {
        BBTestRunner bb = new BBTestRunner(blank);
        BBIni.setDebuggingEnabled();
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.GREEK);
        for (int i = 0; i < 3; i++) {
            bb.bot.activeShell().bot().button(i).click();
        }
        clickInsert(bb);
        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "alphabetaDelta");
    }

    @Test(enabled = false)
    public void rt5764CursorAfter16MathInserts() {
        BBTestRunner bb = new BBTestRunner(blank);
        BBIni.setDebuggingEnabled();
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.GREEK);
        for (int i = 0; i < 16; i++) {
            bb.bot.activeShell().bot().button(i).click();
        }

        clickInsert(bb);
        String s = bb.textViewTools.getTextStripped();
        Assert.assertEquals(s, "alphabetaDeltaGammaLambdaOmegaPhiPiPsiSigmaThetaXichideltaepsiloneta");
    }

    @Test(enabled = false)
    public void insertMathAfterText() {
        BBTestRunner bb = new BBTestRunner(plainText);
        BBIni.setDebuggingEnabled();
        bb.textViewTools.navigateToEndOfLine();
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.GREEK);
        clickButtonWithTooltop(bb, "alpha");
        // Greek Letter alpha
        clickInsert(bb);
        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "x+2alpha");
    }

    private void clickInsert(BBTestRunner bb) {
        bb.bot.activeShell().bot().button(ASCIIMathEditorDialog.INSERT_MATH).click();
    }

    @Test(enabled = false)
    public void insertMathBeforeText() {
        BBTestRunner bb = new BBTestRunner(plainText);
        BBIni.setDebuggingEnabled();
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.GREEK);
        clickButtonWithTooltop(bb, "alpha");
        // Greek Letter alpha
        clickInsert(bb);
        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "alphax+2");
    }

    @Test(enabled = false)
    public void insertMathMiddleText() {
        BBTestRunner bb = new BBTestRunner(plainText);
        BBIni.setDebuggingEnabled();
        bb.textViewTools.navigate(1);
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.GREEK);
        clickButtonWithTooltop(bb, "alpha");
        // Greek Letter alpha
        clickInsert(bb);
        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "xalpha+2");
    }

    @Test(enabled = false)
    public void typeIntoMath() {
        BBTestRunner bb = new BBTestRunner(TestFiles.simpleMath);
        BBIni.setDebuggingEnabled();
        bb.textViewTools.navigate(2);
        bb.textViewTools.typeText("y");
        String s1 = bb.textViewBot.getTextOnLine(0);
        Assert.assertEquals(s1, "x y+ 2");
    }

    @Test(enabled = false)
    public void typeBeforeMath() {
        BBTestRunner bb = new BBTestRunner(TestFiles.simpleMath);
        BBIni.setDebuggingEnabled();
        bb.textViewTools.typeText("y");
        String s1 = bb.textViewBot.getTextOnLine(0);
        Assert.assertEquals(s1, "yx + 2");
    }

    @Test(enabled = false)
    public void typeAfterMath() {
        BBTestRunner bb = new BBTestRunner(TestFiles.simpleMath);
        BBIni.setDebuggingEnabled();
        bb.textViewTools.navigateToEndOfLine();
        bb.textViewTools.typeText("y");
        String s1 = bb.textViewBot.getTextOnLine(0);
        Assert.assertEquals(s1, "x + 2y");
    }

    @Test(enabled = false)
    public void typeWhitespaceAfterMath() {
        BBTestRunner bb = new BBTestRunner(TestFiles.simpleMath);
        BBIni.setDebuggingEnabled();
        bb.textViewTools.navigateToEndOfLine();
        bb.textViewTools.pressKey(SWT.CR, 1);
        bb.textViewTools.typeText("y");

        String s1 = bb.textViewBot.getTextOnLine(0);
        String s2 = bb.textViewBot.getTextOnLine(1);
        Assert.assertEquals(s1, "x + 2");
        Assert.assertEquals(s2, "y");
    }

    @Test(enabled = false)
    public void typeWhitespaceBefore() {
        BBTestRunner bb = new BBTestRunner(TestFiles.simpleMath);
        BBIni.setDebuggingEnabled();
        bb.textViewTools.pressKey(SWT.CR, 1);
        bb.textViewTools.pressKey(SWT.ARROW_UP, 1);
        bb.textViewTools.typeText("y");

        String s1 = bb.textViewBot.getTextOnLine(0);
        String s2 = bb.textViewBot.getTextOnLine(1);
        Assert.assertEquals(s1, "y");
        Assert.assertEquals(s2, "x + 2");
    }

    @Test(enabled = false)
    public void addAlpha() {
        log.debug("Add alpha");
        BBTestRunner bb = new BBTestRunner(TestFiles.simpleMath);
        BBIni.setDebuggingEnabled();
        bb.textViewTools.navigate(4);
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.GREEK);
        bb.bot.activeShell().bot().styledText(0).navigateTo(0, bb.bot.styledText(0).getTextOnLine(0).length());
        clickButtonWithTooltop(bb, "alpha");
        // Greek Letter alpha
        clickReplace(bb);
        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "x + 2alpha");
    }

    private void setCategory(BBTestRunner bb, CATEGORIES category) {
        bb.bot.activeShell().bot().comboBox().setSelection(category.getPrettyName());
    }

    private void clickButtonWithTooltop(BBTestRunner bb, String tooltip) {
        bb.bot.activeShell().bot().buttonWithTooltip(tooltip).click();
    }

    private void clickReplace(BBTestRunner bb) {
        bb.bot.activeShell().bot().button(ASCIIMathEditorDialog.REPLACE_CURRENT_MATH).click();
    }

    @Test(enabled = false)
    public void rt5635MathNonMathToMath() {
        BBTestRunner bb = new BBTestRunner(nonMath);
        bb.textViewTools.navigate(5);
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.GREEK);
        clickButtonWithTooltop(bb, "alpha");
        // Greek Letter alpha
        clickInsert(bb);
        bb.textViewTools.navigate(0);
        bb.textViewTools.selectToEndOf("alpha");
        String highlighted = (bb.textViewBot.getTextOnCurrentLine());
        Assert.assertEquals(highlighted, "x + 2alpha");
        clickMakeMath(bb);
        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "x + 2alpha");
    }

    @Test(enabled = false)
    public void makeMathFromPlainText() {
        log.debug("From plain text");
        BBTestRunner bb = new BBTestRunner(plainText);
        String text = bb.textViewTools.getTextStripped();
        Assert.assertEquals(text, "x+2");
        bb.textViewTools.selectToEndOfLine();
        clickMakeMath(bb);
        TestUtils.getFirstSectionChild(bb).child(0).isInline(BBX.INLINE.MATHML);
    }

    @Test(enabled = false)
    public void rt5703MathEnterMath() {
        BBTestRunner bb = new BBTestRunner(TestFiles.simpleMath);
        bb.textViewTools.navigateToEndOfLine();
        bb.textViewTools.pressKey(SWT.CR, 1);
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.FUNCTIONS);
        clickButtonWithTooltop(bb, "sine");
        clickInsert(bb);
        String s1 = bb.textViewBot.getTextOnLine(0);
        String s2 = bb.textViewBot.getTextOnLine(1);
        Assert.assertEquals(s1, "x + 2");
        Assert.assertEquals(s2, "sin");
    }

    @Test(enabled = false)
    public void rt5665MakingALongStringIntoMath() {
        BBTestRunner bb = new BBTestRunner(longString);
        bb.textViewTools.selectRight(54);
        clickMakeMath(bb);

        String s = bb.textViewTools.getTextStripped();
        Assert.assertEquals(s, "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz");
    }

    @Test(enabled = false)
    public void sinBeforeA() {
        BBTestRunner bb = new BBTestRunner(abc);
        bb.textViewTools.navigate(2);
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.FUNCTIONS);
        clickButtonWithTooltop(bb, "sine");
        clickReplace(bb);
        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "sin(a)/(b) = c");
    }

    @Test(enabled = false)
    public void sinAfterEquation() {
        BBTestRunner bb = new BBTestRunner(abc);
        bb.textViewTools.navigateToEndOfLine();
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.FUNCTIONS);
        clickButtonWithTooltop(bb, "sine");
        clickReplace(bb);
        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "(a)/(b) = c sin");
    }

    @Test(enabled = false)
    public void sinAfterMath() {
        BBTestRunner bb = new BBTestRunner(abc);
        bb.textViewTools.navigateToEndOfLine();
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.FUNCTIONS);
        clickButtonWithTooltop(bb, "sine");
        clickReplace(bb);
        bb.textViewTools.navigate(0);
        bb.textViewTools.selectToEndOf("sin");
        clickMakeMath(bb);
        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "(a)/(b) = c sin");
    }

    @Test(enabled = false)
    public void sinBeforeParenthesis() {
        BBTestRunner bb = new BBTestRunner(abc);
        bb.textViewTools.navigate(1);
        clickOpenAsciiEditor(bb);
        setCategory(bb, CATEGORIES.FUNCTIONS);
        clickButtonWithTooltop(bb, "sine");
        clickReplace(bb);
        String s = bb.textViewBot.getTextOnCurrentLine();
        Assert.assertEquals(s, "sin(a)/(b) = c");
    }

    @DataProvider(name = "complex")
    public String[][] createComplex(Method m) {
        String s = m.getName();
        int i = Integer.parseInt(s.substring(s.length() - 1));
        final String[] complexArray = {"taylor series", "quadratic", "integrate natural log", "pythagorean identity",
                "column vectors", "script order", "complex subscripts", "matrices"};
        final String[] complexAnswerArray = {"f(x)=sum_(n=0)^oo((f^na)/(n!))(x-a)^n", "x=(-b+- sqrt(b^2-4ac))/(2a)",
                "int 1/x dx = ln |x| + C", "sin^2(theta)+cos^2(theta) = 1", "((a,b),(c,d))", "int_0^1f(x)dx",
                "lim_(xrarroo)", "[[a,b],[c,d]]"};
        return new String[][]{{complexArray[i], complexAnswerArray[i]}};
    }

    @Test(dataProvider = "complex", enabled = false)
    public void complex0(String[] s) {
        File file = new File(BBTestRunner.BB_PATH, "programData//settings//MathMLExamples.xml");
        try {
            Document doc = new Builder().build(file);
            BBTestRunner bb = new BBTestRunner(doc);
            Nodes nodes = bb.getDoc().query("//*[local-name()='math' and @example='" + s[0] + "']");
            if (nodes.size() > 0) {
                String ascii = ((Element) nodes.get(0)).getAttributeValue("alttext");
                if (ascii != null) {
                    ascii = ascii.replaceAll(" ", "").replaceAll("`", "");
                    Assert.assertEquals(ascii, s[1].replaceAll(" ", ""));
                } else {
                    Assert.fail("Failed to translate ascii text");
                }
            } else {
                Assert.fail("Cannot find math example in file");
            }
        } catch (ParsingException | IOException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test(dataProvider = "complex", enabled = false)
    public void complex1(String[] s) {
        File file = new File(BBTestRunner.BB_PATH, "programData//settings//MathMLExamples.xml");
        try {
            Document doc = new Builder().build(file);
            BBTestRunner bb = new BBTestRunner(doc);
            Nodes nodes = bb.getDoc().query("//*[local-name()='math' and @example='" + s[0] + "']");
            if (nodes.size() > 0) {
                String ascii = ((Element) nodes.get(0)).getAttributeValue("alttext");
                if (ascii != null) {
                    ascii = ascii.replaceAll(" ", "").replaceAll("`", "");
                    Assert.assertEquals(ascii, s[1].replaceAll(" ", ""));
                } else {
                    Assert.fail("Failed to translate ascii text");
                }
            } else {
                Assert.fail("Cannot find math example in file");
            }
        } catch (ParsingException | IOException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test(dataProvider = "complex", enabled = false)
    public void complex2(String[] s) {
        File file = new File(BBTestRunner.BB_PATH, "programData//settings//MathMLExamples.xml");
        try {
            Document doc = new Builder().build(file);
            BBTestRunner bb = new BBTestRunner(doc);
            Nodes nodes = bb.getDoc().query("//*[local-name()='math' and @example='" + s[0] + "']");
            if (nodes.size() > 0) {
                String ascii = ((Element) nodes.get(0)).getAttributeValue("alttext");
                if (ascii != null) {
                    ascii = ascii.replaceAll(" ", "").replaceAll("`", "");
                    Assert.assertEquals(ascii, s[1].replaceAll(" ", ""));
                } else {
                    Assert.fail("Failed to translate ascii text");
                }
            } else {
                Assert.fail("Cannot find math example in file");
            }
        } catch (ParsingException | IOException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test(dataProvider = "complex", enabled = false)
    public void complex3(String[] s) {
        File file = new File(BBTestRunner.BB_PATH, "programData//settings//MathMLExamples.xml");
        try {
            Document doc = new Builder().build(file);
            BBTestRunner bb = new BBTestRunner(doc);
            Nodes nodes = bb.getDoc().query("//*[local-name()='math' and @example='" + s[0] + "']");
            if (nodes.size() > 0) {
                String ascii = ((Element) nodes.get(0)).getAttributeValue("alttext");
                if (ascii != null) {
                    ascii = ascii.replaceAll(" ", "").replaceAll("`", "");
                    Assert.assertEquals(ascii, s[1].replaceAll(" ", ""));
                } else {
                    Assert.fail("Failed to translate ascii text");
                }
            } else {
                Assert.fail("Cannot find math example in file");
            }
        } catch (ParsingException | IOException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test(dataProvider = "complex", enabled = false)
    public void complex4(String[] s) {
        File file = new File(BBTestRunner.BB_PATH, "programData//settings//MathMLExamples.xml");
        try {
            Document doc = new Builder().build(file);
            BBTestRunner bb = new BBTestRunner(doc);
            Nodes nodes = bb.getDoc().query("//*[local-name()='math' and @example='" + s[0] + "']");
            if (nodes.size() > 0) {
                String ascii = ((Element) nodes.get(0)).getAttributeValue("alttext");
                if (ascii != null) {
                    ascii = ascii.replaceAll(" ", "").replaceAll("`", "");
                    Assert.assertEquals(ascii, s[1].replaceAll(" ", ""));
                } else {
                    Assert.fail("Failed to translate ascii text");
                }
            } else {
                Assert.fail("Cannot find math example in file");
            }
        } catch (ParsingException | IOException e) {

            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test(dataProvider = "complex", enabled = false)
    public void complex5(String[] s) {
        File file = new File(BBTestRunner.BB_PATH, "programData//settings//MathMLExamples.xml");
        try {
            Document doc = new Builder().build(file);
            BBTestRunner bb = new BBTestRunner(doc);
            Nodes nodes = bb.getDoc().query("//*[local-name()='math' and @example='" + s[0] + "']");
            if (nodes.size() > 0) {
                String ascii = ((Element) nodes.get(0)).getAttributeValue("alttext");
                if (ascii != null) {
                    ascii = ascii.replaceAll(" ", "").replaceAll("`", "");
                    Assert.assertEquals(ascii, s[1].replaceAll(" ", ""));
                } else {
                    Assert.fail("Failed to translate ascii text");
                }
            } else {
                Assert.fail("Cannot find math example in file");
            }
        } catch (ParsingException | IOException e) {

            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test(dataProvider = "complex", enabled = false)
    public void complex6(String[] s) {
        File file = new File(BBTestRunner.BB_PATH, "programData//settings//MathMLExamples.xml");
        try {
            Document doc = new Builder().build(file);
            BBTestRunner bb = new BBTestRunner(doc);
            Nodes nodes = bb.getDoc().query("//*[local-name()='math' and @example='" + s[0] + "']");
            if (nodes.size() > 0) {
                String ascii = ((Element) nodes.get(0)).getAttributeValue("alttext");
                if (ascii != null) {
                    ascii = ascii.replaceAll(" ", "").replaceAll("`", "");
                    Assert.assertEquals(ascii, s[1].replaceAll(" ", ""));
                } else {
                    Assert.fail("Failed to translate ascii text");
                }
            } else {
                Assert.fail("Cannot find math example in file");
            }
        } catch (ParsingException | IOException e) {

            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test(dataProvider = "complex", enabled = false)
    public void complex7(String[] s) {
        File file = new File(BBTestRunner.BB_PATH, "programData//settings//MathMLExamples.xml");
        try {
            Document doc = new Builder().build(file);
            BBTestRunner bb = new BBTestRunner(doc);
            Nodes nodes = bb.getDoc().query("//*[local-name()='math' and @example='" + s[0] + "']");
            if (nodes.size() > 0) {
                String ascii = ((Element) nodes.get(0)).getAttributeValue("alttext");
                if (ascii != null) {
                    ascii = ascii.replaceAll(" ", "").replaceAll("`", "");
                    Assert.assertEquals(ascii, s[1].replaceAll(" ", ""));
                } else {
                    Assert.fail("Failed to translate ascii text");
                }
            } else {
                Assert.fail("Cannot find math example in file");
            }
        } catch (ParsingException | IOException e) {

            e.printStackTrace();
            Assert.fail();
        }
    }

    @DataProvider(name = "ueb")
    public String[][] createUeb(Method m) {
        String s = m.getName();
        int i = Integer.parseInt(s.substring(s.length() - 1));
        final String[] basicArray = {"<p><m:math><m:mi>x</m:mi><m:mo>+</m:mo><m:mn>2</m:mn></m:math></p>",
                "<p><m:math><m:mn>77</m:mn><m:mo>-</m:mo><m:mn>3</m:mn></m:math></p>",
                "<p><m:math><m:mn>5</m:mn><m:mo>*</m:mo><m:mn>4</m:mn></m:math></p>"};
        final String[] basicAnswerUebArray = {"x\"6#b", "#gg\"-#c", "#e\"9#d"};
        return new String[][]{{basicArray[i], basicAnswerUebArray[i]}};
    }

    @Test(dataProvider = "ueb", enabled = false)
    public void ueb0(String[] s) {
        Document test = TestXMLUtils.generateBookDoc("", s[0]);
        BBTestRunner bb = new BBTestRunner(test);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);// ueb
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        String braille = bb.bot.activeShell().bot().styledText(2).getTextOnCurrentLine().replaceAll(" ", "");
        Assert.assertEquals(braille, s[1]);
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(dataProvider = "ueb", enabled = false)
    public void ueb1(String[] s) {
        Document test = TestXMLUtils.generateBookDoc("", s[0]);
        BBTestRunner bb = new BBTestRunner(test);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);// ueb
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        String braille = bb.bot.activeShell().bot().styledText(2).getTextOnCurrentLine().replaceAll(" ", "");
        Assert.assertEquals(braille, s[1]);
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(dataProvider = "ueb", enabled = false)
    public void ueb2(String[] s) {
        Document test = TestXMLUtils.generateBookDoc("", s[0]);
        BBTestRunner bb = new BBTestRunner(test);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);// ueb
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        String braille = bb.bot.activeShell().bot().styledText(2).getTextOnCurrentLine().replaceAll(" ", "");
        Assert.assertEquals(braille, s[1]);
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @DataProvider(name = "nemeth")
    public String[][] createNemeth(Method m) {
        String s = m.getName();
        int i = Integer.parseInt(s.substring(s.length() - 1));
        final String[] basicArray = {"<p><m:math><m:mi>x</m:mi><m:mo>+</m:mo><m:mn>2</m:mn></m:math></p>",
                "<p><m:math><m:mn>77</m:mn><m:mo>-</m:mo><m:mn>3</m:mn></m:math></p>",
                "<p><m:math><m:mn>5</m:mn><m:mo>^</m:mo><m:mn>4</m:mn></m:math></p>"};
        final String[] basicAnswerNemethArray = {"x+2", "#77-3", "#5^4"};
        return new String[][]{{basicArray[i], basicAnswerNemethArray[i]}};
    }

    @Test(dataProvider = "nemeth", enabled = false)
    public void nemeth0(String[] s) {
        Document test = TestXMLUtils.generateBookDoc("", s[0]);
        BBTestRunner bb = new BBTestRunner(test);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// Nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        String braille = bb.bot.activeShell().bot().styledText(2).getTextOnCurrentLine().replaceAll(" ", "");
        Assert.assertEquals(braille, s[1]);
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(dataProvider = "nemeth", enabled = false)
    public void nemeth1(String[] s) {
        Document test = TestXMLUtils.generateBookDoc("", s[0]);
        BBTestRunner bb = new BBTestRunner(test);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// Nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        String braille = bb.bot.activeShell().bot().styledText(2).getTextOnCurrentLine().replaceAll(" ", "");
        Assert.assertEquals(braille, s[1]);
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(dataProvider = "nemeth", enabled = false)
    public void nemeth2(String[] s) {
        Document test = TestXMLUtils.generateBookDoc("", s[0]);
        BBTestRunner bb = new BBTestRunner(test);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// Nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        String braille = bb.bot.activeShell().bot().styledText(2).getTextOnCurrentLine().replaceAll(" ", "");
        Assert.assertEquals(braille, s[1]);
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @DataProvider(name = "basic")
    public String[][] createData(Method m) {
        String s = m.getName();
        int i = Integer.parseInt(s.substring(s.length() - 1));
        final String[] basicArray = {"<p><m:math><m:mi>x</m:mi><m:mo>+</m:mo><m:mn>2</m:mn></m:math></p>",
                "<p><m:math><m:mn>77</m:mn><m:mo>-</m:mo><m:mn>3</m:mn></m:math></p>",
                "<p><m:math><m:mn>5</m:mn><m:mo>*</m:mo><m:mn>4</m:mn></m:math></p>"};
        final String[] basicAnswerArray = {"x+2", "77-3", "5*4"};
        return new String[][]{{basicArray[i], basicAnswerArray[i]}};
    }

    @Test(dataProvider = "basic", enabled = false)
    public void basic0(String[] s) {
        Document test = TestXMLUtils.generateBookDoc("", s[0]);
        BBTestRunner bb = new BBTestRunner(test);
        Nodes nodes = bb.getDoc().query("//*[local-name()='math']");
        if (nodes.size() > 0) {
            String ascii = ((Element) nodes.get(0)).getAttributeValue("alttext");
            if (ascii != null) {
                ascii = ascii.replaceAll(" ", "").replaceAll("`", "");
                Assert.assertEquals(ascii, s[1]);
            } else {
                Assert.fail("Failed to translate ascii text");
            }
        } else {
            Assert.fail("Failed to make math ml");
        }
    }

    @Test(dataProvider = "basic", enabled = false)
    public void basic1(String[] s) {
        Document test = TestXMLUtils.generateBookDoc("", s[0]);
        BBTestRunner bb = new BBTestRunner(test);
        Nodes nodes = bb.getDoc().query("//*[local-name()='math']");
        if (nodes.size() > 0) {
            String ascii = ((Element) nodes.get(0)).getAttributeValue("alttext");
            if (ascii != null) {
                ascii = ascii.replaceAll(" ", "").replaceAll("`", "");
                Assert.assertEquals(ascii, s[1]);
            } else {
                Assert.fail("Failed to translate ascii text");
            }
        } else {
            Assert.fail("Failed to make math ml");
        }
    }

    @Test(dataProvider = "basic", enabled = false)
    public void basic2(String[] s) {
        Document test = TestXMLUtils.generateBookDoc("", s[0]);
        BBTestRunner bb = new BBTestRunner(test);
        Nodes nodes = bb.getDoc().query("//*[local-name()='math']");
        if (nodes.size() > 0) {
            String ascii = ((Element) nodes.get(0)).getAttributeValue("alttext");
            if (ascii != null) {
                ascii = ascii.replaceAll(" ", "").replaceAll("`", "");
                Assert.assertEquals(ascii, s[1]);
            } else {
                Assert.fail("Failed to translate ascii text");
            }
        } else {
            Assert.fail("Failed to make math ml");
        }
    }

}
