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

import org.brailleblaster.math.mathml.MathModuleUtils;
import org.brailleblaster.math.mathml.NemethIndicators;
import org.brailleblaster.math.spatial.SpatialMathEnum.Passage;
import org.brailleblaster.math.template.TemplateConstants;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.settings.ui.TranslationSettingsTab;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import nu.xom.Document;

public class TemplateGridTest {
    private final Document twoParagraphs = TestXMLUtils.generateBookDoc("", "<p>Para1</p><p>Para2</p>");
    private final File OLD_TEMPLATE_FORMAT = (new File(
            "src/test/resources/org/brailleblaster/math/templateOldBBX.bbx"));

    private void addIdentifier(BBTestRunner bb, String identifier) {
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.IDENTIFIER_GROUP).setText(identifier);
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
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.OPERAND_GROUP).setText(String.valueOf(i));
    }

    private void setDividend(int i, BBTestRunner bb) {
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.OPERAND_GROUP, 1).setText(String.valueOf(i));
    }

    private void setQuotient(int i, BBTestRunner bb) {
        bb.bot.activeShell().bot().menu(TemplateConstants.SOLUTION_GROUP).menu(TemplateConstants.TRUE).click();
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.SOLUTION_GROUP).setText(String.valueOf(i));
    }

    private void setOperand2(int i, BBTestRunner bb) {
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.OPERAND_GROUP, 1).setText(String.valueOf(i));
    }

    private void setOperand1(int i, BBTestRunner bb) {
        bb.bot.activeShell().bot().textInGroup(TemplateConstants.OPERAND_GROUP).setText(String.valueOf(i));
    }

    private void setOperands(int[] operands, BBTestRunner bb) {
        for (int i = 0; i < operands.length; i++) {
            bb.bot.activeShell().bot().textInGroup(TemplateConstants.OPERAND_GROUP, i)
                    .setText(String.valueOf(operands[i]));
        }
    }

    private void openTemplateDialog(BBTestRunner bb) {
        bb.openMenuItem(TopMenu.MATH, MathModuleUtils.SPATIAL_COMBO);
        bb.bot.activeShell().bot().menu(GridEditor.CONTAINER_TYPE_LABEL)
                .menu(SpatialMathEnum.SpatialMathContainers.TEMPLATE.prettyName).click();
    }

    private void setNotUebPassage(BBTestRunner bb) {
        bb.bot.activeShell().bot().menu(TemplateConstants.SETTINGS).menu(GridConstants.PASSAGE_TYPE).menu(Passage.NONE.getPrettyName())
                .click();
    }

    private void setNemethPassage(BBTestRunner bb) {
        if (!bb.bot.activeShell().bot().menu(TemplateConstants.SETTINGS).menu(Passage.NEMETH.getPrettyName())
                .isChecked()) {
            bb.bot.activeShell().bot().menu(TemplateConstants.SETTINGS).menu(Passage.NEMETH.getPrettyName()).click();
        }
    }

    private void setNumericPassage(BBTestRunner bb) {
        bb.bot.activeShell().bot().menu(TemplateConstants.SETTINGS).menu(GridConstants.PASSAGE_TYPE).menu(Passage.NUMERIC.getPrettyName()).click();
    }

    private void clickNextRow(BBTestRunner bb) {
        bb.bot.activeShell().bot().toggleButtonInGroup(SpatialMathUtils.NEXT_LABEL, SpatialMathUtils.ROW_GROUP).click();
    }

    private void clickNextCol(BBTestRunner bb) {
        bb.bot.activeShell().bot().toggleButtonInGroup(SpatialMathUtils.NEXT_LABEL, SpatialMathUtils.COL_LABEL).click();
    }

    private void addFractionTemplate(BBTestRunner bb, SpatialMathEnum.OPERATOR op, String identifier, String row,
                                     String col) {
        bb.bot.activeShell().bot().toggleButtonInGroup(col, SpatialMathUtils.COL_LABEL).click();
        bb.bot.activeShell().bot().toggleButtonInGroup(row, SpatialMathUtils.ROW_GROUP).click();
        setType(bb, SpatialMathEnum.TemplateType.FRACTION_ENUM);
        int[] array = {1, 1, 1, 2, 2, 2};
        setOperands(array, bb);
        setOperator(bb, op);
        addIdentifier(bb, identifier);
    }

    private void addWholeTemplate(BBTestRunner bb, SpatialMathEnum.OPERATOR op, String identifier, String row,
                                  String col) {
        bb.bot.activeShell().bot().toggleButtonInGroup(col, SpatialMathUtils.COL_LABEL).click();
        bb.bot.activeShell().bot().toggleButtonInGroup(row, SpatialMathUtils.ROW_GROUP).click();
        setOperand1(122, bb);
        setOperand2(24, bb);
        setOperator(bb, op);
        addIdentifier(bb, identifier);
    }

    private void addRadicalTemplate(BBTestRunner bb, String identifier, String row, String col) {
        bb.bot.activeShell().bot().toggleButtonInGroup(col, SpatialMathUtils.COL_LABEL).click();
        bb.bot.activeShell().bot().toggleButtonInGroup(row, SpatialMathUtils.ROW_GROUP).click();
        setType(bb, SpatialMathEnum.TemplateType.RADICAL_ENUM);
        setDivisor(5, bb);
        setDividend(465, bb);
        setQuotient(93, bb);
        addIdentifier(bb, identifier);
    }

    // @Test
    public void openOldBBX() {
        BBTestRunner bb = new BBTestRunner(OLD_TEMPLATE_FORMAT);
        openTemplateDialog(bb);
    }

    @DataProvider(name = "simpleData")
    public Object[][] simpleData() {
        SpatialMathEnum.TemplateType type = SpatialMathEnum.TemplateType.SIMPLE_ENUM;
        SpatialMathEnum.OPERATOR op = SpatialMathEnum.OPERATOR.PLUS_ENUM;
        String identifier = "";
        boolean passage = false;
        return new Object[][]{{type, op, identifier, passage}};
    }

    @DataProvider(name = "radical")
    public Object[][] radical() {
        String identifier = "";
        boolean passage = false;
        return new Object[][]{{identifier, passage}};
    }

    @Test(dataProvider = "simpleData", enabled = false)
    public void threeColumns(SpatialMathEnum.TemplateType type, SpatialMathEnum.OPERATOR op, String identifier,
                             boolean passage) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextCol(bb);
        clickNextCol(bb);
        for (int i = 0; i < 3; i++) {
            addWholeTemplate(bb, op, identifier, "1", String.valueOf(i + 1));
        }
        pressOk(bb);
        String[] array = {"Para1", "", "122    122    122", "+ 24   + 24   + 24", "333333 333333 333333", "",
                "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(dataProvider = "simpleData", enabled = false)
    public void addNemethIdentifiers(SpatialMathEnum.TemplateType type, SpatialMathEnum.OPERATOR op, String identifier,
                                     boolean passage) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextCol(bb);
        clickNextCol(bb);
        for (int i = 0; i < 3; i++) {
            addWholeTemplate(bb, op, identifier, "1", String.valueOf(i + 1));
        }
        setNemethPassage(bb);
        pressOk(bb);
        String[] array = {"Para1", "", NemethIndicators.INLINE_BEGINNING_INDICATOR + "  122    122    122",
                "+ 24   + 24   + 24", "333333 333333 333333" + NemethIndicators.INLINE_END_INDICATOR, "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(dataProvider = "radical", enabled = false)
    public void templateGridRadicalNemeth(String identifier, boolean passage) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addRadicalTemplate(bb, identifier, String.valueOf(j + 1), String.valueOf(i + 1));
            }
        }
        pressOk(bb);
        String[] array = {"Para1", "", "93     93", "33333  33333", "5o465  5o465", "", "93     93", "33333  33333",
                "5o465  5o465", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridFractionNemeth() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addFractionTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "", String.valueOf(i + 1),
                        String.valueOf(j + 1));
            }
        }
        pressOk(bb);
        String[] array = {"Para1", "", "1_?1/1_#    1_?1/1_#", "+2_?2/2_#   +2_?2/2_#", "33333333333 33333333333", "",
                "1_?1/1_#    1_?1/1_#", "+2_?2/2_#   +2_?2/2_#", "33333333333 33333333333", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridMixedNemeth() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        addWholeTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "", "1", "1");
        addRadicalTemplate(bb, "", "1", "2");
        addFractionTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "", "2", "2");
        addRadicalTemplate(bb, "", "2", "1");
        pressOk(bb);
        String[] array = {"Para1", "", "122     93", "+ 24   33333", "333333 5o465", "", "93    1_?1/1_#",
                "33333  +2_?2/2_#", "5o465  33333333333", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridWholeNemeth() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addWholeTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "", String.valueOf(i + 1),
                        String.valueOf(j + 1));
            }
        }
        pressOk(bb);
        String[] array = {"Para1", "", "122    122", "+ 24   + 24", "333333 333333", "", "122    122", "+ 24   + 24",
                "333333 333333", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridRadicalUebNotPassage() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        setNotUebPassage(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addRadicalTemplate(bb, "", String.valueOf(i + 1), String.valueOf(j + 1));

            }
        }
        pressOk(bb);
        String[] array = {"Para1", "", "# ic      # ic", "\"33333    \"33333", "#e o #dfe #e o #dfe", "",
                "# ic      # ic", "\"33333    \"33333", "#e o #dfe #e o #dfe", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridFractionUebNotPassage() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        setNotUebPassage(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addFractionTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "", String.valueOf(i + 1),
                        String.valueOf(j + 1));
            }
        }
        pressOk(bb);
        String[] array = {"Para1", "", "#a#a/a   #a#a/a", "\"6#b#b/b \"6#b#b/b", "\"33333   \"33333", "",
                "#a#a/a   #a#a/a", "\"6#b#b/b \"6#b#b/b", "\"33333   \"33333", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridMixedUebNotPassage() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        setNotUebPassage(bb);
        addWholeTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "", "1", "1");
        addRadicalTemplate(bb, "", "1", "2");
        addRadicalTemplate(bb, "", "2", "1");
        addFractionTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "", "2", "2");
        pressOk(bb);
        String[] array = {"Para1", "", "#abb      # ic", "\"6# bd    \"33333", "\"333 #e o #dfe", "", "# ic   #a#a/a",
                "\"33333 \"6#b#b/b", "#e o #dfe   \"33333", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridWholeUebNotPassage() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        setNotUebPassage(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addWholeTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "", String.valueOf(i + 1),
                        String.valueOf(j + 1));

            }
        }
        pressOk(bb);
        String[] array = {"Para1", "", "#abb   #abb", "\"6# bd \"6# bd", "\"333   \"333", "", "#abb   #abb",
                "\"6# bd \"6# bd", "\"333   \"333", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridWholeNemethPassageMathIdentifiers() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addWholeTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, i + String.valueOf(j),
                        String.valueOf(i + 1), String.valueOf(j + 1));

            }
        }
        setIdentifiersAsMath(bb);
        pressOk(bb);
        String[] array = {"Para1", "", "#00          #01", "122          122", "+ 24         + 24", "333333       333333",
                "", "#10          #11", "122          122", "+ 24         + 24", "333333       333333", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    private void setIdentifiersAsMath(BBTestRunner bb) {
        bb.bot.activeShell().bot().menu(TemplateConstants.SETTINGS).menu(TemplateConstants.IDENTIFIER_TRANSLATION)
                .menu(TemplateConstants.MATH_TRANSLATION).click();
    }

    @Test(enabled = false)
    public void templateGridRadicalNemethWithIndicators() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addRadicalTemplate(bb, "1", String.valueOf(i + 1), String.valueOf(j + 1));
            }
        }
        pressOk(bb);
        String[] array = {"Para1", "", "#a          #a", "93          93", "33333       33333", "5o465       5o465",
                "", "#a          #a", "93          93", "33333       33333", "5o465       5o465", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridFractionNemethWithIndicators() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addFractionTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "1", String.valueOf(i + 1),
                        String.valueOf(j + 1));
            }
        }
        pressOk(bb);
        String[] array = {"Para1", "", "#a               #a", "1_?1/1_#         1_?1/1_#",
                "+2_?2/2_#        +2_?2/2_#", "33333333333      33333333333", "", "#a               #a",
                "1_?1/1_#         1_?1/1_#", "+2_?2/2_#        +2_?2/2_#", "33333333333      33333333333", "",
                "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridMixedNemethWithIndicators() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        addWholeTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, String.valueOf(1), "1", "1");
        addRadicalTemplate(bb, String.valueOf(2), "1", "2");
        addRadicalTemplate(bb, String.valueOf(3), "2", "1");
        addFractionTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, String.valueOf(4), "2", "2");
        pressOk(bb);
        String[] array = {"Para1", "", "#a          #b", "122          93", "+ 24        33333", "333333      5o465",
                "", "#c          #d", "93         1_?1/1_#", "33333       +2_?2/2_#", "5o465       33333333333", "",
                "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridWholeNemethWithIndicators() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addWholeTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "1", String.valueOf(i + 1),
                        String.valueOf(j + 1));
            }
        }
        pressOk(bb);
        String[] array = {"Para1", "", "#a          #a", "122         122", "+ 24        + 24", "333333      333333",
                "", "#a          #a", "122         122", "+ 24        + 24", "333333      333333", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridRadicalUebNotPassageWithIndicators() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        setNotUebPassage(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addRadicalTemplate(bb, "1", String.valueOf(i + 1), String.valueOf(j + 1));

            }
        }
        pressOk(bb);
        String[] array = {"Para1", "", "#a             #a", "# ic           # ic", "\"33333         \"33333",
                "#e o #dfe      #e o #dfe", "", "#a             #a", "# ic           # ic", "\"33333         \"33333",
                "#e o #dfe      #e o #dfe", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridFractionUebNotPassageWithIndicators() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        setNotUebPassage(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addFractionTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "1", String.valueOf(i + 1),
                        String.valueOf(j + 1));

            }
        }
        pressOk(bb);
        String[] array = {"Para1", "", "#a            #a", "#a#a/a        #a#a/a", "\"6#b#b/b      \"6#b#b/b",
                "\"33333        \"33333", "", "#a            #a", "#a#a/a        #a#a/a", "\"6#b#b/b      \"6#b#b/b",
                "\"33333        \"33333", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridMixedUebNotPassageWithIndicators() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        setNotUebPassage(bb);
        addWholeTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, String.valueOf(1), "1", "1");
        addRadicalTemplate(bb, String.valueOf(2), "1", "2");
        addRadicalTemplate(bb, "3", "2", "1");
        addFractionTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "4", "2", "2");
        pressOk(bb);
        String[] array = {"Para1", "", "#a          #b", "#abb           # ic", "\"6# bd         \"33333",
                "\"333      #e o #dfe", "", "#c             #d", "# ic        #a#a/a", "\"33333      \"6#b#b/b",
                "#e o #dfe        \"33333", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridWholeUebNotPassageWithIndicators() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        setNotUebPassage(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addWholeTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "1", String.valueOf(i + 1),
                        String.valueOf(j + 1));

            }
        }
        pressOk(bb);
        String[] array = {"Para1", "", "#a          #a", "#abb        #abb", "\"6# bd      \"6# bd",
                "\"333        \"333", "", "#a          #a", "#abb        #abb", "\"6# bd      \"6# bd",
                "\"333        \"333", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void threeColumnsWithIndicators() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB_PLUS_NEMETH);// nemeth
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextCol(bb);
        clickNextCol(bb);
        for (int i = 0; i < 3; i++) {
            addWholeTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, String.valueOf(i + 1), "1", String.valueOf(i + 1));
        }
        pressOk(bb);
        String[] array = {"Para1", "", "#a          #b          #c", "122         122         122",
                "+ 24        + 24        + 24", "333333      333333      333333", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridRadicalUebPassageWithIndicators() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addRadicalTemplate(bb, "1", String.valueOf(i + 1), String.valueOf(j + 1));
            }
        }
        setNumericPassage(bb);
        pressOk(bb);
        String[] array = {"Para1", "", "##", "#a           #a", "ic           ic", "\"3333        \"3333",
                "e o dfe      e o dfe", "", "#a           #a", "ic           ic", "\"3333        \"3333",
                "e o dfe      e o dfe", "#'", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridFractionUebPassageWithIndicators() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addFractionTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "1", String.valueOf(i + 1),
                        String.valueOf(j + 1));
            }
        }
        setNumericPassage(bb);
        pressOk(bb);
        String[] array = {"Para1", "", "##", "#a           #a", "a#a/a        a#a/a", "\"6b#b/b      \"6b#b/b",
                "\"3333        \"3333", "", "#a           #a", "a#a/a        a#a/a", "\"6b#b/b      \"6b#b/b",
                "\"3333        \"3333", "#'", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridMixedUebPassageWithIndicators() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        addWholeTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, String.valueOf(1), "1", "1");
        addRadicalTemplate(bb, String.valueOf(2), "1", "2");
        addRadicalTemplate(bb, String.valueOf(3), "2", "1");
        addFractionTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, String.valueOf(4), "2", "2");
        setNumericPassage(bb);
        pressOk(bb);
        String[] array = {"Para1", "", "##", "#a         #b", "abb           ic", "\"6 bd        \"3333",
                "\"33      e o dfe", "", "#c           #d", "ic        a#a/a", "\"3333      \"6b#b/b",
                "e o dfe        \"3333", "#'", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridWholeUebPassageWithIndicators() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addWholeTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "1", String.valueOf(i + 1),
                        String.valueOf(j + 1));
            }
        }
        setNumericPassage(bb);
        pressOk(bb);
        String[] array = {"Para1", "", "##", "#a         #a", "abb        abb", "\"6 bd      \"6 bd",
                "\"33        \"33", "", "#a         #a", "abb        abb", "\"6 bd      \"6 bd", "\"33        \"33",
                "#'", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridRadicalUebPassage() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addRadicalTemplate(bb, "", String.valueOf(i + 1), String.valueOf(j + 1));
            }
        }
        setNumericPassage(bb);
        pressOk(bb);
        String[] array = {"Para1", "", "##", "ic      ic", "\"3333   \"3333", "e o dfe e o dfe", "", "ic      ic",
                "\"3333   \"3333", "e o dfe e o dfe", "#'", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridFractionUebPassage() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addFractionTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "", String.valueOf(i + 1),
                        String.valueOf(j + 1));
            }
        }
        setNumericPassage(bb);
        pressOk(bb);
        String[] array = {"Para1", "", "##", "a#a/a   a#a/a", "\"6b#b/b \"6b#b/b", "\"3333   \"3333", "",
                "a#a/a   a#a/a", "\"6b#b/b \"6b#b/b", "\"3333   \"3333", "#'", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridMixedUebPassage() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        addWholeTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "", "1", "1");
        addRadicalTemplate(bb, "", "1", "2");
        addRadicalTemplate(bb, "", "2", "1");
        addFractionTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "", "2", "2");
        setNumericPassage(bb);
        pressOk(bb);
        String[] array = {"Para1", "", "##", "abb      ic", "\"6 bd   \"3333", "\"33 e o dfe", "", "ic   a#a/a",
                "\"3333 \"6b#b/b", "e o dfe   \"3333", "#'", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }

    @Test(enabled = false)
    public void templateGridWholeUebPassage() {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        String oldTable = bb.manager.getDocument().getEngine().getBrailleSettings().getMainTranslationTable();
        bb.openMenuItem(TopMenu.SETTINGS, "Translation Settings");
        bb.bot.activeShell().bot().comboBox(0).setSelection(TranslationSettingsTab.UEB);
        bb.bot.activeShell().bot().button("OK").click();
        bb.manager.waitForFormatting(true);
        bb.updateViewReferences();
        bb.textViewTools.navigateToEndOfLine();
        openTemplateDialog(bb);
        clickNextRow(bb);
        clickNextCol(bb);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addWholeTemplate(bb, SpatialMathEnum.OPERATOR.PLUS_ENUM, "", String.valueOf(i + 1),
                        String.valueOf(j + 1));
            }
        }
        setNumericPassage(bb);
        pressOk(bb);
        String[] array = {"Para1", "", "##", "abb   abb", "\"6 bd \"6 bd", "\"33   \"33", "", "abb   abb",
                "\"6 bd \"6 bd", "\"33   \"33", "#'", "", "Para2"};
        for (int i = 0; i < array.length; i++) {
            String s = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(s, array[i]);
        }
        bb.manager.getDocument().getEngine().getBrailleSettings().setMainTranslationTable(oldTable);
    }
}
