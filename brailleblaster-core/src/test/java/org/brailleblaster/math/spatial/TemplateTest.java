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

import org.brailleblaster.math.mathml.MathModuleUtils;
import org.brailleblaster.math.spatial.SpatialMathEnum.Passage;
import org.brailleblaster.math.template.TemplateConstants;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.settings.ui.TranslationSettingsTab;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import nu.xom.Document;

public class TemplateTest {
    private final Document twoParagraphs = TestXMLUtils.generateBookDoc("", "<p>Para1</p><p>Para2</p>");

    private void addIdentifier(BBTestRunner bb, String identifier) {
        bb.bot.activeShell().bot().textWithLabel(TemplateConstants.IDENTIFIER_LABEL).setText(identifier);
    }

    private void pressOk(BBTestRunner bb) {
        bb.bot.activeShell().bot().button(SpatialMathUtils.OK_LABEL).click();
    }

    private void setOperator(BBTestRunner bb, SpatialMathEnum.OPERATOR operatorEnum) {
        bb.bot.activeShell().bot().menu(TemplateConstants.OPERATOR_LABEL).menu(operatorEnum.prettyName).click();
    }

    private void setType(BBTestRunner bb, SpatialMathEnum.TemplateType type) {
        bb.bot.activeShell().bot().menu(TemplateConstants.TEMPLATE_TYPE_LABEL).menu(type.prettyName).click();
    }

    private void setDivisor(int i, BBTestRunner bb) {
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.OPERAND_GROUP, 0).setText(String.valueOf(i));
    }

    private void setDividend(int i, BBTestRunner bb) {
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.OPERAND_GROUP, 1).setText(String.valueOf(i));
    }

    private void setQuotientWithRemainder(int i, int j, BBTestRunner bb) {
        bb.bot.activeShell().bot().menu(TemplateConstants.SOLUTION_GROUP).menu(TemplateConstants.TRUE).click();
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.SOLUTION_GROUP, 0).setText(String.valueOf(i));
        bb.bot.activeShell().bot().textWithLabel(TemplateConstants.REMAINDER).setText(String.valueOf(j));
    }

    private void setDivisor(double i, BBTestRunner bb) {
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.OPERAND_GROUP, 0).setText(String.valueOf(i));
    }

    private void setDividend(double i, BBTestRunner bb) {
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.OPERAND_GROUP, 1).setText(String.valueOf(i));
    }

    private void setQuotient(double i, BBTestRunner bb) {
        bb.bot.activeShell().bot().menu(TemplateConstants.SOLUTION_GROUP).menu(TemplateConstants.TRUE).click();
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.SOLUTION_GROUP, 0).setText(String.valueOf(i));
    }

    private void setQuotient(int i, BBTestRunner bb) {
        bb.bot.activeShell().bot().menu(TemplateConstants.SOLUTION_GROUP).menu(TemplateConstants.TRUE).click();
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.SOLUTION_GROUP, 0).setText(String.valueOf(i));
    }

    private void setOperand2(int i, BBTestRunner bb) {
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.OPERAND_GROUP, 1).setText(String.valueOf(i));
    }

    private void setOperand1(int i, BBTestRunner bb) {
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.OPERAND_GROUP, 0).setText(String.valueOf(i));
    }

    private void setOperand2(BBTestRunner bb, double d) {
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.OPERAND_GROUP, 1).setText(String.valueOf(d));
    }

    private void setOperand1(BBTestRunner bb, double d) {
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.OPERAND_GROUP, 0).setText(String.valueOf(d));
    }

    private void setOperands(int[] operands, BBTestRunner bb) {
        for (int i = 0; i < operands.length; i++) {
            bb.bot.activeShell().bot().textInGroup(TemplateConstants.OPERAND_GROUP, i)
                    .setText(String.valueOf(operands[i]));
        }
    }

    private void setOperands(String[] operands, BBTestRunner bb) {
        for (int i = 0; i < operands.length; i++) {
            bb.bot.activeShell().bot().textInGroup(TemplateConstants.OPERAND_GROUP, i).setText((operands[i]));
        }
    }

    private void setSolution(int i, BBTestRunner bb) {
        bb.bot.activeShell().bot().menu(TemplateConstants.SOLUTION_GROUP).menu(TemplateConstants.TRUE).click();
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.SOLUTION_GROUP, 0).setText(String.valueOf(i));
    }

    private void openTemplateDialog(BBTestRunner bb) {
        bb.openMenuItem(TopMenu.MATH, MathModuleUtils.SPATIAL_COMBO);
        bb.bot.activeShell().bot().menu(GridEditor.CONTAINER_TYPE_LABEL)
                .menu(SpatialMathEnum.SpatialMathContainers.TEMPLATE.prettyName).click();
    }

    private void setNotUebPassage(BBTestRunner bb) {
        bb.bot.activeShell().bot().menu(TemplateConstants.SETTINGS).menu(GridConstants.PASSAGE_TYPE)
                .menu(Passage.NONE.getPrettyName()).click();
    }

    private void setNumericPassage(BBTestRunner bb) {
        bb.bot.activeShell().bot().menu(TemplateConstants.SETTINGS).menu(GridConstants.PASSAGE_TYPE)
                .menu(Passage.NUMERIC.getPrettyName()).click();
    }

    @Test(enabled = false)
    public void addIdentifierNemethUebContext() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        addIdentifier(bb, "1.");
        setOperand1(1, bb);
        setOperand2(2, bb);
        setOperator(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "#a4");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "1");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "+2");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "3333");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "");
        String s7 = bb.textViewBot.getTextOnLine(7).trim();
        Assert.assertEquals(s7, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void nemethRadicalLongQuotient() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setType(bb, SpatialMathEnum.TemplateType.RADICAL_ENUM);
        setDivisor(12.22, bb);
        setDividend(1.2, bb);
        setQuotient(12.22678, bb);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "12.22678");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "3333333333");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "12.22o 1.2");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void nemethRadicalRemainder() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setType(bb, SpatialMathEnum.TemplateType.RADICAL_ENUM);
        setDivisor(3, bb);
        setDividend(13, bb);
        setQuotientWithRemainder(4, 1, bb);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "4 R\"1");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "33333333");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "3o13");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void nemethRadicalLongDividend() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setType(bb, SpatialMathEnum.TemplateType.RADICAL_ENUM);
        setDivisor(12.22, bb);
        setDividend(12.22678, bb);
        setQuotient(1.2, bb);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "1.2");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "3333333333");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "12.22o12.22678");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void uebRadicalPassage() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setType(bb, SpatialMathEnum.TemplateType.RADICAL_ENUM);
        setDivisor(5, bb);
        setDividend(465, bb);
        setQuotient(93, bb);
        setNumericPassage(bb);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "##");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "ic");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "\"3333");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "e o dfe");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "#'");
        String s7 = bb.textViewBot.getTextOnLine(7).trim();
        Assert.assertEquals(s7, "");
        String s8 = bb.textViewBot.getTextOnLine(8).trim();
        Assert.assertEquals(s8, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void uebRadicalRemainder() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setType(bb, SpatialMathEnum.TemplateType.RADICAL_ENUM);
        setDivisor(5, bb);
        setDividend(465, bb);
        setQuotientWithRemainder(93, 1, bb);
        setNotUebPassage(bb);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "# ic r#a");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "\"333333333");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "#e o #dfe");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void uebRadicalNoPassage() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setType(bb, SpatialMathEnum.TemplateType.RADICAL_ENUM);
        setDivisor(5, bb);
        setDividend(465, bb);
        setQuotient(93, bb);
        setNotUebPassage(bb);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "# ic");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "\"33333");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "#e o #dfe");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void wholeNumNemeth() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setOperand1(1, bb);
        setOperand2(2, bb);
        setOperator(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "1");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "+2");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "3333");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void wholeNumUebPassage() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setOperand1(1, bb);
        setOperand2(2, bb);
        setOperator(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM);
        setNumericPassage(bb);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "##");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "a");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "\"6b");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "\"3");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "#'");
        String s7 = bb.textViewBot.getTextOnLine(7).trim();
        Assert.assertEquals(s7, "");
        String s8 = bb.textViewBot.getTextOnLine(8).trim();
        Assert.assertEquals(s8, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void wholeNumUebNoPassage() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setOperand1(1, bb);
        setOperand2(2, bb);
        setOperator(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM);
        setNotUebPassage(bb);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "#a");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "\"6#b");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "\"3");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void wholeNumUebNoPassageWithSolution() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setOperand1(111, bb);
        setOperand2(2, bb);
        setSolution(222, bb);
        setOperator(bb, SpatialMathEnum.OPERATOR.MULTIPLY_ENUM);
        setNotUebPassage(bb);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "#aaa");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "\"8#  b");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "\"333");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "#bbb");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "");
        String s7 = bb.textViewBot.getTextOnLine(7).trim();
        Assert.assertEquals(s7, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void wholeNumUebPassageWithSolution() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setOperand1(30, bb);
        setOperand2(50, bb);
        setSolution(1500, bb);
        setOperator(bb, SpatialMathEnum.OPERATOR.MULTIPLY_ENUM);
        setNumericPassage(bb);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "##");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "cj");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "\"8  ej");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "\"333");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "aejj");
        String s7 = bb.textViewBot.getTextOnLine(7).trim();
        Assert.assertEquals(s7, "#'");
        String s8 = bb.textViewBot.getTextOnLine(8).trim();
        Assert.assertEquals(s8, "");
        String s9 = bb.textViewBot.getTextOnLine(9).trim();
        Assert.assertEquals(s9, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void wholeNumNemethWithSolution() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setSolution(3, bb);
        setOperand1(1, bb);
        setOperand2(2, bb);
        setOperator(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "1");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "+2");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "3333");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "3");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "");
        String s7 = bb.textViewBot.getTextOnLine(7).trim();
        Assert.assertEquals(s7, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void nemethFraction() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setType(bb, SpatialMathEnum.TemplateType.FRACTION_ENUM);
        int[] operands = {122, 24, 366, 48, 51010, 612};
        setOperands(operands, bb);
        setOperator(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "122_?   24/366_#");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "+ 48_?51010/612_#");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "3333333333333333333");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void nemethSimpleFraction() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setType(bb, SpatialMathEnum.TemplateType.FRACTION_ENUM);
        String[] operands = {"", "24", "366", "", "51010", "612"};
        setOperands(operands, bb);
        setOperator(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "?   24/366#");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "+?51010/612#");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "33333333333333");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void nemethSimpleAndMixedFraction() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setType(bb, SpatialMathEnum.TemplateType.FRACTION_ENUM);
        String[] operands = {"122", "24", "366", "", "51010", "612"};
        setOperands(operands, bb);
        setOperator(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "122_?   24/366_#");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "+    ?51010/612 #");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "3333333333333333333");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void uebPassageFraction() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setType(bb, SpatialMathEnum.TemplateType.FRACTION_ENUM);
        int[] operands = {122, 24, 366, 48, 51010, 612};
        setOperands(operands, bb);
        setOperator(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM);
        setNumericPassage(bb);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "##");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "abb#   bd/cff");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "\"6 dh#eajaj/fab");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "\"333333333333");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "#'");
        String s7 = bb.textViewBot.getTextOnLine(7).trim();
        Assert.assertEquals(s7, "");
        String s8 = bb.textViewBot.getTextOnLine(8).trim();
        Assert.assertEquals(s8, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void uebNoPassageFraction() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setType(bb, SpatialMathEnum.TemplateType.FRACTION_ENUM);
        int[] operands = {122, 24, 366, 48, 51010, 612};
        setOperands(operands, bb);
        setOperator(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM);
        setNotUebPassage(bb);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "#abb#   bd/cff");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "\"6# dh#eajaj/fab");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "\"3333333333333");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void nemethDecimal() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setType(bb, SpatialMathEnum.TemplateType.SIMPLE_ENUM);
        setOperand1(bb, 12.22);
        setOperand2(bb, 2.4);
        setOperator(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "12.22");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "+ 2.4");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "33333333");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void uebPassageDecimal() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setType(bb, SpatialMathEnum.TemplateType.SIMPLE_ENUM);
        setOperand1(bb, 12.22);
        setOperand2(bb, 2.4);
        setOperator(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM);
        setNumericPassage(bb);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "##");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "ab4bb");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "\"6 b4d");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "\"3333");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "#'");
        String s7 = bb.textViewBot.getTextOnLine(7).trim();
        Assert.assertEquals(s7, "");
        String s8 = bb.textViewBot.getTextOnLine(8).trim();
        Assert.assertEquals(s8, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void uebNoPassageDecimal() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        setType(bb, SpatialMathEnum.TemplateType.SIMPLE_ENUM);
        setOperand1(bb, 12.22);
        setOperand2(bb, 2.4);
        setOperator(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM);
        setNotUebPassage(bb);
        pressOk(bb);
        String s0 = bb.textViewBot.getTextOnLine(0).trim();
        Assert.assertEquals(s0, "Para1");
        String s1 = bb.textViewBot.getTextOnLine(1).trim();
        Assert.assertEquals(s1, "");
        String s2 = bb.textViewBot.getTextOnLine(2).trim();
        Assert.assertEquals(s2, "#ab4bb");
        String s3 = bb.textViewBot.getTextOnLine(3).trim();
        Assert.assertEquals(s3, "\"6# b4d");
        String s4 = bb.textViewBot.getTextOnLine(4).trim();
        Assert.assertEquals(s4, "\"33333");
        String s5 = bb.textViewBot.getTextOnLine(5).trim();
        Assert.assertEquals(s5, "");
        String s6 = bb.textViewBot.getTextOnLine(6).trim();
        Assert.assertEquals(s6, "Para2");
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }
}
