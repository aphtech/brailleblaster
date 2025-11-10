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

import org.brailleblaster.TestUtils;
import org.brailleblaster.math.mathml.MathModuleUtils;
import org.brailleblaster.math.numberLine.*;
import org.brailleblaster.math.spatial.SpatialMathEnum.BlankOptions;
import org.brailleblaster.math.spatial.SpatialMathEnum.NumberLineOptions;
import org.brailleblaster.math.spatial.SpatialMathEnum.NumberLineViews;
import org.brailleblaster.math.spatial.SpatialMathEnum.Passage;
import org.brailleblaster.math.spatial.SpatialMathEnum.Translation;
import org.brailleblaster.math.template.TemplateConstants;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.settings.ui.TranslationSettingsTab;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import nu.xom.Document;

import java.util.ArrayList;

public class NumberLineTest {

    private final Document twoParagraphs =
            TestXMLUtils.generateBookDoc("", "<p>Para1</p><p>Para2</p>");
    int interval = 0;
    final int startSegment = 3;
    final int endSegment = 4;
    final int startLine = 1;
    final int endLine = 2;

    @DataProvider(name = "parseWeirdThings")
    public Object[][] parseWeirdThingsData() {
        final String[] noLeadingZero = {".2", ".2", ".6"};
        final NumberLineText textNoLeadingZero =
                new NumberLineText(

                        new NumberLineComponent.NumberLineComponentBuilder().whole("").decimal("2").build(), NumberLineSegment.Companion.createDefaultNumberLineSegment(),
                        new NumberLineComponent.NumberLineComponentBuilder().whole("").decimal("2").build(),
                        new NumberLineComponent.NumberLineComponentBuilder().whole("").decimal("6").build(), new ArrayList<>());
        final String[] extraSpacesDecimal = {"1.  2", "1  .2", " 3.6"};
        final NumberLineText textExtraSpacesDecimal =
                new NumberLineText(
                        new NumberLineComponent.NumberLineComponentBuilder()
                                .whole("1")
                                .decimal("2")
                                .build(), NumberLineSegment.Companion.createDefaultNumberLineSegment(),
                        new NumberLineComponent.NumberLineComponentBuilder()
                                .whole("1")
                                .decimal("2")
                                .build(),
                        new NumberLineComponent.NumberLineComponentBuilder()
                                .whole("3")
                                .decimal("6")
                                .build(), new ArrayList<>());
        final String[] extraSpacesImproper = {"2/ 5", " 2/5", "6 /5 "};
        final NumberLineText textExtraSpacesImproper =
                new NumberLineText(
                        new NumberLineComponent.NumberLineComponentBuilder()
                                .numerator("2")
                                .denominator("5")
                                .build(), NumberLineSegment.Companion.createDefaultNumberLineSegment(),
                        new NumberLineComponent.NumberLineComponentBuilder()
                                .numerator("2")
                                .denominator("5")
                                .build(),
                        new NumberLineComponent.NumberLineComponentBuilder()
                                .numerator("6")
                                .denominator("5")
                                .build(), new ArrayList<>());
        final String[] extraSpacesMixed = {"1    1/2", "1 1 / 2", " 4 1/2 "};
        final NumberLineText textExtraSpacesMixed =
                new NumberLineText(
                        new NumberLineComponent.NumberLineComponentBuilder()
                                .whole("1")
                                .numerator("1")
                                .denominator("2")
                                .build(), NumberLineSegment.Companion.createDefaultNumberLineSegment(),
                        new NumberLineComponent.NumberLineComponentBuilder()
                                .whole("1")
                                .numerator("1")
                                .denominator("2")
                                .build(),
                        new NumberLineComponent.NumberLineComponentBuilder()
                                .whole("4")
                                .numerator("1")
                                .denominator("2")
                                .build(), new ArrayList<>());
        final String[] mixedWithImproper = {"1/2", "1 1/2", "2"};
        final NumberLineText textMixedWithImproper =
                new NumberLineText(
                        new NumberLineComponent.NumberLineComponentBuilder()
                                .whole("")
                                .numerator("1")
                                .denominator("2")
                                .build(), NumberLineSegment.Companion.createDefaultNumberLineSegment(),
                        new NumberLineComponent.NumberLineComponentBuilder()
                                .whole("1")
                                .numerator("1")
                                .denominator("2")
                                .build(),
                        new NumberLineComponent.NumberLineComponentBuilder().whole("2").build(), new ArrayList<>());
        final String[] extraSpacesWhole = {"1 ", "77", " 79"};
        final NumberLineText textExtraSpacesWhole =
                new NumberLineText(
                        new NumberLineComponent.NumberLineComponentBuilder().whole("1").build(), NumberLineSegment.Companion.createDefaultNumberLineSegment(),
                        new NumberLineComponent.NumberLineComponentBuilder().whole("77").build(),
                        new NumberLineComponent.NumberLineComponentBuilder().whole("79").build(), new ArrayList<>());
        return new Object[][]{
                {extraSpacesDecimal, textExtraSpacesDecimal},
                {noLeadingZero, textNoLeadingZero},
                {extraSpacesImproper, textExtraSpacesImproper},
                {extraSpacesMixed, textExtraSpacesMixed},
                {extraSpacesWhole, textExtraSpacesWhole},
                {mixedWithImproper, textMixedWithImproper}
        };
    }

    @Test(dataProvider = "parseWeirdThings")
    public void parseWeirdThings(String[] weirdStrings, NumberLineText numberLineText) {
        NumberLineStringParser parser = new NumberLineStringParser();
        parser.setIntervalString(weirdStrings[0]);
        parser.setLineStart(weirdStrings[1]);
        parser.setLineEnd(weirdStrings[2]);
        NumberLineText newNumberLineText = null;
        try {
            newNumberLineText = parser.parse();
        } catch (MathFormattingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for (int i = 0; i < weirdStrings.length; i++) {
            try {
                Assert.assertEquals(newNumberLineText.getInterval().compareTo(numberLineText.getInterval()), 0);
                Assert.assertEquals(newNumberLineText.getLineStart().compareTo(numberLineText.getLineStart()), 0);
                Assert.assertEquals(newNumberLineText.getLineEnd().compareTo(numberLineText.getLineEnd()), 0);
            } catch (MathFormattingException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    public void openNumberLineEditor(BBTestRunner bb) {
        bb.openMenuItem(TopMenu.MATH, MathModuleUtils.SPATIAL_COMBO);
        bb.bot
                .activeShell()
                .bot()
                .menu(GridEditor.CONTAINER_TYPE_LABEL)
                .menu(SpatialMathEnum.SpatialMathContainers.NUMBER_LINE.prettyName)
                .click();
        setPassage(bb);
    }

    public void setPassage(BBTestRunner bb) {
        if (!MathModuleUtils.isNemeth()) {
            bb.bot
                    .activeShell()
                    .bot()
                    .menu(TemplateConstants.SETTINGS)
                    .menu(GridConstants.PASSAGE_TYPE)
                    .menu(Passage.NONE.getPrettyName())
                    .click();
        }
    }

    private void setStartFullCircle(BBTestRunner bb) {
        bb.bot
                .activeShell()
                .bot()
                .menu(NumberLineConstants.START_SEGMENT_SYMBOL_LABEL)
                .menu(NumberLineConstants.FULL_CIRCLE)
                .click();
    }

    private void setBeveledFraction(BBTestRunner bb) {
        bb.bot
                .activeShell()
                .bot()
                .menu(NumberLineConstants.MENU_SETTINGS)
                .menu(NumberLineConstants.BEVELED_FRACTION)
                .click();
    }

    private void setStretch(BBTestRunner bb) {
        bb.bot
                .activeShell()
                .bot()
                .menu(NumberLineConstants.MENU_SETTINGS)
                .menu(NumberLineConstants.STRETCH_LABEL)
                .click();
    }

    public void setSegment(String start, String end, BBTestRunner bb) {
        bb.bot.activeShell().bot().text(startSegment).setText(String.valueOf(start));
        bb.bot.activeShell().bot().text(endSegment).setText(String.valueOf(end));
    }

    public void setLine(String start, String end, BBTestRunner bb) {
        bb.bot.activeShell().bot().text(startLine).setText(String.valueOf(start));
        bb.bot.activeShell().bot().text(endLine).setText(String.valueOf(end));
    }

    public void setNotReduced(BBTestRunner bb) {
        bb.bot
                .activeShell()
                .bot()
                .menu(NumberLineConstants.MENU_SETTINGS)
                .menu(NumberLineConstants.REDUCE_FRACTION)
                .click();
    }

    public void setInterval(BBTestRunner bb, String interval) {
        bb.bot.activeShell().bot().text(0).setText(String.valueOf(interval));
    }

    public void pressOk(BBTestRunner bb) {
        bb.bot.activeShell().bot().button(SpatialMathUtils.OK_LABEL).click();
    }

    public void addArrowsToLine(BBTestRunner bb) {
        if (!bb.bot
                .activeShell()
                .bot()
                .menu(NumberLineConstants.MENU_SETTINGS)
                .menu(NumberLineConstants.ARROW_LABEL)
                .isChecked()) {
            bb.bot
                    .activeShell()
                    .bot()
                    .menu(NumberLineConstants.MENU_SETTINGS)
                    .menu(NumberLineConstants.ARROW_LABEL)
                    .click();
        }
    }

    private void checkRemoveLeadingZeros(BBTestRunner bb) {
        bb.bot
                .activeShell()
                .bot()
                .menu(NumberLineConstants.MENU_SETTINGS)
                .menu(NumberLineConstants.REMOVE_LEADING_ZEROS_LABEL)
                .click();
    }

    @DataProvider(name = "decimalWithBlank")
    public Object[][] decimalWithBlankData() {
        final String[] parameters = {"1.2", "3.6", "1.2"};
        final String[] uebAnswers = {
                "Para1", "", "=    =    =", "|{\"3w3333w3333w333|o", "#a4b +    #c4f", "", "Para2"
        };
        final String[] nemethAnswers = {
                "Para1", "", "=   =   =", "{3r333r333r333o", "1.2 =   3.6", "", "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "decimalWithBlank", enabled = false)
    public void decimalWithBlank(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setLine(parameters[0], parameters[1], bb);
        setInterval(bb, parameters[2]);
        setPoints(bb);
        setNumPoints(3, bb);
        addPoint(0, bb);
        addPoint(1, bb);
        addPoint(2, bb);
        addArrowsToLine(bb);
        setBlankView(bb);
        setRadioButton(bb, 1, BlankOptions.OMISSION);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    private void setRadioButton(BBTestRunner bb, int i, BlankOptions type) {
        bb.bot.activeShell().bot().radio(type.getPrettyName(), i).click();
    }

    private void setBlankView(BBTestRunner bb) {
        bb.bot.activeShell().bot().button(NumberLineOptions.BLANKS.getPrettyName()).click();
    }

    private void setView(BBTestRunner bb, NumberLineViews blanks) {
        bb.bot
                .activeShell()
                .bot()
                .menu(NumberLineConstants.NUMBER_LINE_TYPE)
                .menu(blanks.getPrettyName())
                .click();
    }

    @DataProvider(name = "userConvertedFromDecimalData")
    public Object[][] userConvertedFromDecimalData() {
        final String[] parameters = {"1.2", "3.6", "1.2"};
        final String[] uebAnswers = {
                "Para1", "", "=    =    =", "|{\"3w3333w3333w33|o", "1.2  test 3.6", "", "Para2"
        };
        final String[] nemethAnswers = {
                "Para1", "", "=    =    =", "{3r3333r3333r333o", "1.2  test 3.6", "", "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "userConvertedFromDecimalData", enabled = false)
    public void userConvertedFromDecimalData(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setLine(parameters[0], parameters[1], bb);
        setInterval(bb, parameters[2]);
        setView(bb, NumberLineViews.USER_DEFINED);
        setPoints(bb);
        setNumPoints(3, bb);
        addPoint(0, bb);
        addPoint(1, bb);
        addPoint(2, bb);
        addArrowsToLine(bb);
        setUserText(bb, "test", 1);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    private void setUserText(BBTestRunner bb, String string, int i) {
        bb.bot.activeShell().bot().text(i).setText(string);
    }

    @DataProvider(name = "userEnteredData")
    public Object[][] userEnteredData() {
        final String[] parameters = {"1.2", "3.6", "1.2"};
        final String[] uebAnswers = {
                "Para1", "", "=", "|{\"3w33333w33333w3333|o", "test1 test2 test3", "", "Para2"
        };
        final String[] nemethAnswers = {
                "Para1", "", "=", "{3r33333r33333r33333o", "test1 test2 test3", "", "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "userEnteredData", enabled = false)
    public void userEnteredData(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setView(bb, NumberLineViews.USER_DEFINED);
        setUserIntervals(bb, 3);
        setPoints(bb);
        setNumPoints(1, bb);
        setUserText(bb, "test1", 0);
        setUserText(bb, "test2", 1);
        setUserText(bb, "test3", 2);
        addArrowsToLine(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    private void setUserIntervals(BBTestRunner bb, int i) {
        bb.bot
                .activeShell()
                .bot()
                .menu(NumberLineConstants.MARKER_LABEL)
                .menu(String.valueOf(i))
                .click();
    }

    @DataProvider(name = "decimalMultiplePoints")
    public Object[][] decimalMultiplePointsData() {
        final String[] parameters = {"1.2", "3.6", "1.2"};
        final String[] uebAnswers = {
                "Para1", "", "=    =    =", "|{\"3w3333w3333w333|o", "#a4b #b4d #c4f", "", "Para2"
        };
        final String[] nemethAnswers = {
                "Para1", "", "=   =   =", "{3r333r333r333o", "1.2 2.4 3.6", "", "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "decimalMultiplePoints", enabled = false)
    public void decimalMultiplePoints(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setLine(parameters[0], parameters[1], bb);
        setInterval(bb, parameters[2]);
        setPoints(bb);
        setNumPoints(3, bb);
        addPoint(0, bb);
        addPoint(1, bb);
        addPoint(2, bb);
        addArrowsToLine(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    private void setPoints(BBTestRunner bb) {
        bb.bot
                .activeShell()
                .bot()
                .menu(NumberLineConstants.SECTION_TYPE)
                .menu(SpatialMathEnum.NumberLineSection.POINTS.getPrettyName())
                .click();
    }

    private void setNoneSectionType(BBTestRunner bb) {
        bb.bot
                .activeShell()
                .bot()
                .menu(NumberLineConstants.SECTION_TYPE)
                .menu(SpatialMathEnum.NumberLineSection.NONE.getPrettyName())
                .click();
    }

    private void setNumPoints(int i, BBTestRunner bb) {
        bb.bot
                .activeShell()
                .bot()
                .menu(NumberLineConstants.NUMBER_POINTS)
                .menu(String.valueOf(i))
                .click();
    }

    private void addPoint(int index, BBTestRunner bb) {
        bb.bot.activeShell().bot().comboBox(index).setSelection(index);
    }

    @DataProvider(name = "decimalSinglePoint")
    public Object[][] decimalSinglePointData() {
        final String[] parameters = {"2.4", "2.4", "1.2", "3.6", "1.2"};
        final String[] uebAnswers = {
                "Para1", "", "=", "|{\"3w3333w3333w333|o", "#a4b #b4d #c4f", "", "Para2"
        };
        final String[] nemethAnswers = {
                "Para1", "", "=", "{3r333r333r333o", "1.2 2.4 3.6", "", "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "decimalSinglePoint", enabled = false)
    public void decimalSinglePoint(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setStartFullCircle(bb);
        setSegment(parameters[0], parameters[1], bb);
        setLine(parameters[2], parameters[3], bb);
        setInterval(bb, parameters[4]);
        addArrowsToLine(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    @DataProvider(name = "wholeStretch")
    public Object[][] wholeStretchData() {
        final String[] parameters = {"1", "1", "3", "1", "4"};
        final String[] uebAnswers = {
                "Para1",
                "",
                "\"33333333333333333333",
                "|{\"3w333333333w333333333w333333333w3|o",
                "#a        #b        #c        #d",
                "",
                "Para2"
        };
        final String[] nemethAnswers = {
                "Para1",
                "",
                "3                     3",
                "{3r7777777777r7777777777r3333333333r3o",
                "1          2          3          4",
                "",
                "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "wholeStretch", enabled = false)
    public void wholeStretch(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setInterval(bb, parameters[0]);
        setStretch(bb);
        setSegment(parameters[1], parameters[2], bb);
        setLine(parameters[3], parameters[4], bb);
        addArrowsToLine(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    @DataProvider(name = "decimalLead")
    public Object[][] decimalLeadData() {
        final String[] parameters = {".2", "1", ".2"};
        final String[] uebAnswers = {
                "Para1", "", "|{\"3w3333w3333w3333w3333w333|o", "#4b  #4d  #4f  #4h  #a4j", "", "Para2"
        };
        final String[] nemethAnswers = {
                "Para1", "", "{3r333r333r333r333r333o", ".2  .4  .6  .8  1.0", "", "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "decimalLead", enabled = false)
    public void decimalLead(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setLine(parameters[0], parameters[1], bb);
        setInterval(bb, parameters[2]);
        addArrowsToLine(bb);
        checkRemoveLeadingZeros(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    @DataProvider(name = "lineOnly")
    public Object[][] lineOnlyData() {
        final String[] parameters = {"1", "1", "4"};
        final String[] uebAnswers = {"Para1", "", "|{\"3w33w33w33w3|o", "#a #b #c #d", "", "Para2"};
        final String[] nemethAnswers = {"Para1", "", "{3r3r3r3r3o", "1 2 3 4", "", "Para2"};
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "lineOnly", enabled = false)
    public void lineOnly(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setInterval(bb, parameters[0]);
        setLine(parameters[1], parameters[2], bb);
        addArrowsToLine(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    @DataProvider(name = "negativeLine")
    public Object[][] negativeLineData() {
        final String[] nemethParameters = {"1", "-3", "5"};
        final String[] uebParameters = {"1", "-3", "3"};
        final String[] uebAnswers = {
                "Para1",
                "",
                "|{\"33w3333w3333w3333w3333w3333w3333w3|o",
                "\"-#c \"-#b \"-#a   #j   #a   #b   #c",
                "",
                "Para2"
        };
        final String[] nemethAnswers = {
                "Para1", "", "{3r33r33r33r33r33r33r33r33r3o", "-3 -2 -1  0  1  2  3  4  5", "", "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, uebParameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, nemethParameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "negativeLine", enabled = false)
    public void negativeLine(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setInterval(bb, parameters[0]);
        setLine(parameters[1], parameters[2], bb);
        addArrowsToLine(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    @DataProvider(name = "lineAndSegment")
    public Object[][] lineAndSegmentData() {
        final String[] parameters = {"1", "1", "2", "1", "4"};
        final String[] uebAnswers = {
                "Para1", "", "\"333", "|{\"3w33w33w33w3|o", "#a #b #c #d", "", "Para2"
        };
        final String[] nemethAnswers = {"Para1", "", "3 3", "{3r7r3r3r3o", "1 2 3 4", "", "Para2"};
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "lineAndSegment", enabled = false)
    public void lineAndSegment(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setInterval(bb, parameters[0]);
        setSegment(parameters[1], parameters[2], bb);
        setLine(parameters[3], parameters[4], bb);
        addArrowsToLine(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    @DataProvider(name = "lineTooLong")
    public Object[][] lineTooLongData() {
        final String[] parameters = {"1", "1", "50"};
        final String[] uebAnswers = {"Para1", "Para2"};
        final String[] nemethAnswers = {"Para1", "Para2"};
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "lineTooLong", enabled = false)
    public void lineTooLong(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        try {
            bb.textViewTools.navigateToEndOfLine();
            openNumberLineEditor(bb);
            setInterval(bb, parameters[0]);
            setLine(parameters[1], parameters[2], bb);
            pressOk(bb);
            Assert.fail(); // should have thrown an exception
        } catch (Exception e) {
            String dialog = e.getMessage();
            Assert.assertTrue(dialog.contains(MathModuleUtils.LONG_LINE_WARNING));
            for (int i = 0; i < answers.length; i++) {
                String st = bb.textViewBot.getTextOnLine(i).trim();
                Assert.assertEquals(st, answers[i]);
            }
        }
    }

    @DataProvider(name = "altIntervalLineOnly")
    public Object[][] altIntervalLineOnlyData() {
        final String[] parameters = {"2", "0", "4"};
        final String[] uebAnswers = {"Para1", "", "|{\"3w33w33w3|o", "#j #b #d", "", "Para2"};
        final String[] nemethAnswers = {"Para1", "", "{3r3r3r3o", "0 2 4", "", "Para2"};
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "altIntervalLineOnly", enabled = false)
    public void altIntervalLineOnly(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setInterval(bb, parameters[0]);
        setLine(parameters[1], parameters[2], bb);
        addArrowsToLine(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    @DataProvider(name = "altIntervalLineAndSegment")
    public Object[][] altIntervalLineAndSegmentData() {
        final String[] parameters = {"10", "20", "0", "25", "5"};
        final String[] uebAnswers = {
                "Para1",
                "",
                "\"33333333",
                "|{\"3w333w333w333w333w333w33|o",
                "#j  #e  #aj #ae #bj #be",
                "",
                "Para2"
        };
        final String[] nemethAnswers = {
                "Para1", "", "3     3", "{3r33r33r77r77r33r33o", "0  5  10 15 20 25", "", "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "altIntervalLineAndSegment", enabled = false)
    public void altIntervalLineAndSegment(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setSegment(parameters[0], parameters[1], bb);
        setLine(parameters[2], parameters[3], bb);
        addArrowsToLine(bb);
        setInterval(bb, parameters[4]);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    @DataProvider(name = "decimal")
    public Object[][] decimalData() {
        final String[] parameters = {"1.2", "2.4", "1.2", "3.6", "1.2"};
        final String[] negativeParameters = {"-2.4", "-1.2", "-3.6", "-1.2", "1.2"};
        final String[] uebAnswers = {
                "Para1", "", "\"33333", "|{\"3w3333w3333w333|o", "#a4b #b4d #c4f", "", "Para2"
        };
        final String[] nemethAnswers = {
                "Para1", "", "3   3", "{3r777r333r333o", "1.2 2.4 3.6", "", "Para2"
        };
        final String[] uebAnswersNegative = {
                "Para1", "", "\"3333333", "|{\"33w333333w333333w333|o", "\"-#c4f \"-#b4d \"-#a4b", "", "Para2"
        };
        final String[] nemethAnswersNegative = {
                "Para1", "", "3    3", "{3r3333r7777r333o", "-3.6 -2.4 -1.2", "", "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers},
                {TranslationSettingsTab.UEB, negativeParameters, uebAnswersNegative},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, negativeParameters, nemethAnswersNegative}
        };
    }

    @Test(dataProvider = "decimal", enabled = false)
    public void decimal(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setSegment(parameters[0], parameters[1], bb);
        setLine(parameters[2], parameters[3], bb);
        setInterval(bb, parameters[4]);
        addArrowsToLine(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    @DataProvider(name = "decimalRT7360")
    public Object[][] decimalRT7360Data() {
        final String[] parameters = {"0.5", "3.5", "0.5", "5.0", "1.5"};
        final String[] uebAnswers = {
                "Para1", "", "\"3333333333", "|{\"3w3333w3333w3333w333|o", "#j4e #b4j #c4e #e4j", "", "Para2"
        };
        final String[] nemethAnswers = {
                "Para1", "", "3       3", "{3r777r777r333r333o", "0.5 2.0 3.5 5.0", "", "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "decimalRT7360", enabled = false)
    public void decimalRT7360(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setSegment(parameters[0], parameters[1], bb);
        setLine(parameters[2], parameters[3], bb);
        setInterval(bb, parameters[4]);
        addArrowsToLine(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    @DataProvider(name = "improperFraction")
    public Object[][] improperFractionData() {
        final String[] parameters = {"2/5", "4/5", "2/5", "6/5", "2/5"};
        final String[] negativeParameters = {"-4/5", "-2/5", "-6/5", "-2/5", "2/5"};
        final String[] uebAnswers = {
                "Para1", "", "\"33333", "|{\"3w3333w3333w333|o", "#b/e #d/e #f/e", "", "Para2"
        };
        final String[] nemethAnswers = {
                "Para1", "", "3     3", "{3r77777r33333r33333o", "?2/5# ?4/5# ?6/5#", "", "Para2"
        };
        final String[] uebNegativeAnswers = {
                "Para1", "", "\"3333333", "|{\"33w333333w333333w333|o", "\"-#f/e \"-#d/e \"-#b/e", "", "Para2"
        };
        final String[] nemethNegativeAnswers = {
                "Para1", "", "3      3", "{3r333333r777777r33333o", "-?6/5# -?4/5# -?2/5#", "", "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers},
                {TranslationSettingsTab.UEB, negativeParameters, uebNegativeAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, negativeParameters, nemethNegativeAnswers}
        };
    }

    @Test(dataProvider = "improperFraction", enabled = false)
    public void improperFraction(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setSegment(parameters[0], parameters[1], bb);
        setLine(parameters[2], parameters[3], bb);
        setInterval(bb, parameters[4]);
        addArrowsToLine(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    @DataProvider(name = "improperFractionRT7275")
    public Object[][] improperFractionRT7275Data() {
        final String[] parameters = {"5/4", "11/8", "8/8", "12/8", "1/8"};
        final String[] uebAnswers = {
                "Para1",
                "",
                "\"333333",
                "|{\"3w33333w33333w33333w33333w3333|o",
                "#h/h  #i/h  #aj/h #aa/h #ab/h",
                "",
                "Para2"
        };
        final String[] nemethAnswers = {
                "Para1",
                "",
                "3      3",
                "{3r333333r333333r777777r333333r333333o",
                "?8/8#  ?9/8#  ?10/8# ?11/8# ?12/8#",
                "",
                "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "improperFractionRT7275", enabled = false)
    public void improperFractionRT7275(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setSegment(parameters[0], parameters[1], bb);
        setLine(parameters[2], parameters[3], bb);
        setInterval(bb, parameters[4]);
        addArrowsToLine(bb);
        setNotReduced(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    @DataProvider(name = "improperFractionRT7367")
    public Object[][] improperFractionRT7367Data() {
        final String[] parameters = {"1/4", "1/2", "0/4", "4/4", "1/4"};
        final String[] uebAnswers = {
                "Para1",
                "",
                "\"33333",
                "|{\"3w3333w3333w3333w3333w333|o",
                "#j/d #a/d #b/d #c/d #d/d",
                "",
                "Para2"
        };
        final String[] nemethAnswers = {
                "Para1",
                "",
                "3     3",
                "{3r33333r77777r33333r33333r33333o",
                "?0/4# ?1/4# ?2/4# ?3/4# ?4/4#",
                "",
                "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "improperFractionRT7367", enabled = false)
    public void improperFractionRT7367(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setSegment(parameters[0], parameters[1], bb);
        setLine(parameters[2], parameters[3], bb);
        setInterval(bb, parameters[4]);
        addArrowsToLine(bb);
        setNotReduced(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    @DataProvider(name = "notReducedFraction")
    public Object[][] notReducedFractionData() {
        final String[] parameters = {"2/2", "3/2", "2/2", "4/2", "1/2"};
        final String[] uebAnswers = {
                "Para1", "", "\"33333", "|{\"3w3333w3333w333|o", "#b/b #c/b #d/b", "", "Para2"
        };
        final String[] nemethAnswers = {
                "Para1", "", "3     3", "{3r77777r33333r33333o", "?2/2# ?3/2# ?4/2#", "", "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "notReducedFraction", enabled = false)
    public void notReducedFraction(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setNotReduced(bb);
        setSegment(parameters[0], parameters[1], bb);
        setLine(parameters[2], parameters[3], bb);
        setInterval(bb, parameters[4]);
        addArrowsToLine(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    @DataProvider(name = "mixedFractionRT7267")
    public Object[][] mixedFractionRT7267Data() {
        final String[] parameters = {"1 1/2", "2", "0", "2", "1/2"};
        final String[] nemethParameters = {"1 1/2", "2", "1/2", "2", "1/2"};
        /*
         * same parameters for Nemeth are too long by one
         */
        final String[] uebAnswers = {
                "Para1",
                "",
                "\"3333333",
                "|{\"3w333333w333333w333333w333333w3|o",
                "#j     #a/b   #a     #a#a/b #b",
                "",
                "Para2"
        };
        final String[] nemethAnswers = {
                "Para1",
                "",
                "3        3",
                "{3r33333333r33333333r77777777r3o",
                "?1/2#    1        1_?1/2_# 2",
                "",
                "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, nemethParameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "mixedFractionRT7267", enabled = false)
    public void mixedFractionRT7267(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setSegment(parameters[0], parameters[1], bb);
        setLine(parameters[2], parameters[3], bb);
        setInterval(bb, parameters[4]);
        addArrowsToLine(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    @DataProvider(name = "mixedFraction")
    public Object[][] mixedFractionData() {
        final String[] parameters = {"1 1/2", "3", "1 1/2", "4 1/2", "1 1/2"};
        final String[] negativeParameters = {"-3", "-1 1/2", "-4 1/2", "-1 1/2", "1 1/2"};
        final String[] uebAnswers = {
                "Para1", "", "\"3333333", "|{\"3w333333w333333w33333|o", "#a#a/b #c     #d#a/b", "", "Para2"
        };
        final String[] nemethAnswers = {
                "Para1",
                "",
                "3        3",
                "{3r77777777r33333333r33333333o",
                "1_?1/2_# 3        4_?1/2_#",
                "",
                "Para2"
        };
        final String[] uebNegativeAnswers = {
                "Para1",
                "",
                "\"333333333",
                "|{\"33w33333333w33333333w33333|o",
                "\"-#d#a/b \"-#c     \"-#a#a/b",
                "",
                "Para2"
        };
        final String[] nemethNegativeAnswers = {
                "Para1",
                "",
                "3         3",
                "{3r333333333r777777777r33333333o",
                "-4_?1/2_# -3        -1_?1/2_#",
                "",
                "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers},
                {TranslationSettingsTab.UEB, negativeParameters, uebNegativeAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, negativeParameters, nemethNegativeAnswers}
        };
    }

    @Test(dataProvider = "mixedFraction", enabled = false)
    public void mixedFraction(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setSegment(parameters[0], parameters[1], bb);
        setLine(parameters[2], parameters[3], bb);
        setInterval(bb, parameters[4]);
        addArrowsToLine(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    @DataProvider(name = "mixedFractionWithLabels")
    public Object[][] mixedFractionWithLabelsData() {
        final String[] parameters = {"0", "1", "1/2"};
        final String[] uebAnswers = {
                "Para1", "", "|{\"3w3333w3333w3|o", "#j   #a/b #a", "low     okay     high", "", "Para2"
        };
        final String[] nemethAnswers = {
                "Para1", "", "{3r33333r33333r3o", "0     ?1/2# 1", "low     okay     high", "", "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    // @Test(dataProvider = "userTextWithLabels",enabled = false)
    public void userTextWithLabels(
            String s,
            String[] parameters,
            Translation userTranslation,
            Translation labelTranslation,
            String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setView(bb, NumberLineViews.USER_DEFINED);
        setUserIntervals(bb, 2);
        setUserText(bb, parameters[0], 0);
        setUserText(bb, parameters[1], 1);
        setLabelOption(bb);
        addLabels(bb, parameters);
        setLabelTranslation(bb, labelTranslation);
        setUserDefinedTranslation(bb, userTranslation);
        setNoneSectionType(bb);
        addArrowsToLine(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    @DataProvider(name = "userTextWithLabels")
    public Object[][] userTextWithLabelsData() {
        final String[] parameters = {"one", "two"};
        final Translation uncontracted = Translation.UNCONTRACTED;
        final Translation literary = Translation.LITERARY;
        final String[] uebAnswersUncontractedLabel = {
                "Para1", "", "|{\"3w3333w3333w3|o", "#j   #a/b #a", "low     okay     high", "", "Para2"
        };
        final String[] uebAnswersLiteraryLabel = {
                "Para1", "", "|{\"3w3333w3333w3|o", "#j   #a/b #a", "low     okay     high", "", "Para2"
        };
        final String[] nemethAnswersUncontractedLabel = {
                "Para1", "", "{3r33333r33333r3o", "0     ?1/2# 1", "low     okay     high", "", "Para2"
        };
        final String[] nemethAnswersLiteraryLabel = {
                "Para1", "", "{3r33333r33333r3o", "0     ?1/2# 1", "low     okay     high", "", "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uncontracted, literary, uebAnswersLiteraryLabel},
                {
                        TranslationSettingsTab.UEB_PLUS_NEMETH,
                        parameters,
                        uncontracted,
                        literary,
                        nemethAnswersLiteraryLabel
                },
                {TranslationSettingsTab.UEB, parameters, literary, uncontracted, uebAnswersUncontractedLabel},
                {
                        TranslationSettingsTab.UEB_PLUS_NEMETH,
                        parameters,
                        literary,
                        uncontracted,
                        nemethAnswersUncontractedLabel
                }
        };
    }

    private void setLabelTranslation(BBTestRunner bb, Translation literary) {
        bb.bot
                .activeShell()
                .bot()
                .menu(NumberLineConstants.TRANSLATION_LABEL)
                .menu(literary.getPrettyName())
                .click();
    }

    private void setUserDefinedTranslation(BBTestRunner bb, Translation literary) {
        bb.bot
                .activeShell()
                .bot()
                .menu(NumberLineConstants.TRANSLATION_TYPE)
                .menu(literary.getPrettyName())
                .click();
    }

    // @Test(dataProvider = "mixedFractionWithLabels")
    public void mixedFractionWithLabels(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setLine(parameters[0], parameters[1], bb);
        setInterval(bb, parameters[2]);
        setLabelOption(bb);
        String[] labels = {"low", "okay", "high"};
        addLabels(bb, labels);
        addArrowsToLine(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }

    private void setLabelOption(BBTestRunner bb) {
        bb.bot.activeShell().bot().button(NumberLineOptions.LABELS.getPrettyName()).click();
    }

    private void addLabels(BBTestRunner bb, String[] labels) {
        for (int i = 0; i < labels.length; i++) {
            bb.bot.activeShell().bot().text(i).setText(labels[i]);
        }
    }

    @DataProvider(name = "beveledFraction")
    public Object[][] beveledFractionData() {
        final String[] parameters = {"2/5", "4/5", "2/5", "6/5", "2/5"};
        final String[] uebAnswers = {
                "Para1", "", "\"333333", "|{\"3w33333w33333w3333|o", "#b_/e #d_/e #f_/e", "", "Para2"
        };
        final String[] nemethAnswers = {
                "Para1", "", "3      3", "{3r777777r333333r333333o", "?2_/5# ?4_/5# ?6_/5#", "", "Para2"
        };
        return new Object[][]{
                {TranslationSettingsTab.UEB, parameters, uebAnswers},
                {TranslationSettingsTab.UEB_PLUS_NEMETH, parameters, nemethAnswers}
        };
    }

    @Test(dataProvider = "beveledFraction", enabled = false)
    public void beveledFraction(String s, String[] parameters, String[] answers) {
        BBTestRunner bb = new BBTestRunner(twoParagraphs);
        TestUtils.changeSettings(bb, s);
        bb.textViewTools.navigateToEndOfLine();
        openNumberLineEditor(bb);
        setBeveledFraction(bb);
        setSegment(parameters[0], parameters[1], bb);
        setLine(parameters[2], parameters[3], bb);
        setInterval(bb, parameters[4]);
        addArrowsToLine(bb);
        pressOk(bb);
        for (int i = 0; i < answers.length; i++) {
            String st = bb.textViewBot.getTextOnLine(i).trim();
            Assert.assertEquals(st, answers[i]);
        }
    }
}
