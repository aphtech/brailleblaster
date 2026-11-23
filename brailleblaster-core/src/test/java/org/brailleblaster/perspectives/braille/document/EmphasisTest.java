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
import static org.brailleblaster.TestUtils.getFirstSectionChild;
import static org.brailleblaster.TestUtils.getInnerSection;
import static org.brailleblaster.testrunners.ViewTestRunner.doPendingSWTWork;
import static org.testng.Assert.assertEquals;

import org.brailleblaster.bbx.BBX;
import org.brailleblaster.perspectives.mvc.modules.views.EmphasisModule;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.BBTestRunner.ViewTools;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.utd.properties.EmphasisType;
import org.eclipse.swt.SWT;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import nu.xom.Document;

@Test(enabled = false)
@GuiTest
public class EmphasisTest {
    private final String lorem = "lorem ipsum";
    private final String dolor = "dolor sit amet";

    private final Document basicBlock = TestXMLUtils.generateBookDoc("", "<p>" + lorem + "</p>");
    private final Document boldedBlock = TestXMLUtils.generateBookDoc("", "<p><strong>" + lorem + "</strong></p>");
    private final Document beginningBoldedBlock = TestXMLUtils.generateBookDoc("", "<p><strong>" + lorem + "</strong> " + dolor + "</p>");
    private final Document middleBoldedBlock = TestXMLUtils.generateBookDoc("", "<p>" + lorem + " <strong>" + dolor + "</strong> " + lorem + "</p>");
    private final Document endBoldedBlock = TestXMLUtils.generateBookDoc("", "<p>" + lorem + " <strong>" + dolor + "</strong></p>");
    private final Document complexBlock = TestXMLUtils.generateBookDoc("", "<p><strong>" + lorem + "</strong><em>" + lorem + "</em><span class=\"underline\">" + lorem + "</span></p>");
    private final Document multiEmphasisComplexBlock = TestXMLUtils.generateBookDoc("", "<p><strong><em><span class=\"underline\">" + lorem + "</span>" + dolor + "</em></strong></p>");
    private final Document multiEmphasis = TestXMLUtils.generateBookDoc("", "<p><strong>" + lorem + "</strong><em>" + dolor + "</em></p>");

    private final Document multiBlock = TestXMLUtils.generateBookDoc("", "<level1><p>" + lorem + "</p><p>" + lorem + "</p><p>" + lorem + "</p></level1>");
    private final Document multiBlockMultiEmphasis = TestXMLUtils.generateBookDoc("", "<level1><p><strong>" + lorem + "</strong><em>" + dolor + "</em></p><p><span class=\"underline\">" + lorem + "</span><strong><em>" + dolor + "</em></strong></p></level1>");
    private final Document blocksWithPageNum = TestXMLUtils.generateBookDoc("", "<p>" + lorem + "</p><pagenum>1</pagenum><p>" + dolor + "</p>");

    private final Document bugRT4380 = TestXMLUtils.generateBookDoc("", "<p>" + lorem + "<img/>" + dolor + "</p>");
    private final String triangle = "\u25b2";
    private final Document directTranslateUnicode = TestXMLUtils.generateBookDoc("", "<p>" + lorem + triangle + dolor + "</p>");

    /* TEXT VIEW TESTS*/

    //@Test
    public void rt_5896_direct_translate() {
        BBTestRunner test = new BBTestRunner(directTranslateUnicode);
        test.textViewTools.navigate(lorem.length());
        test.textViewTools.selectRight(1);
        test.textViewBot.contextMenu("Change Translation").contextMenu("Direct").click();
    }

    //	@Test(dataProvider="allEmphasis")
    public void basicEmphasis(Em em) {
        BBTestRunner test = new BBTestRunner(basicBlock);

        textViewEmphasizeLine(test, em);

        getFirstSectionChild(test)
                .nextChildIs((c) -> c.isInlineEmphasis(em.emType))
                .noNextChild();
    }

    //	@Test
    public void basicUnemphasis() {
        BBTestRunner test = new BBTestRunner(boldedBlock);

        textViewEmphasizeLine(test, Em.BOLD);

        getFirstSectionChild(test)
                .nextChildIsText(lorem)
                .noNextChild();
    }

    //	@Test
    public void unemphasizeBeginningNormalizesTextNode() {
        BBTestRunner test = new BBTestRunner(beginningBoldedBlock);

        test.textViewTools.selectRight(lorem.length(), true);
        doPendingSWTWork();
        textViewEmphasize(test, Em.BOLD);

        getFirstSectionChild(test)
                .nextChildIsText(lorem + " " + dolor)
                .noNextChild();
    }

    //	@Test
    public void unemphasizeMiddleNormalizesTextNode() {
        BBTestRunner test = new BBTestRunner(middleBoldedBlock);

        test.textViewTools.navigateToText(dolor);
        test.textViewTools.selectRight(dolor.length(), true);
        textViewEmphasize(test, Em.BOLD);

        getFirstSectionChild(test)
                .nextChildIsText(lorem + " " + dolor + " " + lorem)
                .noNextChild();
    }

    //	@Test
    public void unemphasizeEndNormalizesTextNode() {
        BBTestRunner test = new BBTestRunner(endBoldedBlock);

        test.textViewTools.navigateToText(dolor);
        test.textViewTools.selectRight(dolor.length(), true);
        textViewEmphasize(test, Em.BOLD);

        getFirstSectionChild(test)
                .nextChildIsText(lorem + " " + dolor)
                .noNextChild();
    }

    //	@Test
    public void unboldIfAllTextNodesAreBold() {
        BBTestRunner test = new BBTestRunner(endBoldedBlock);

        test.textViewTools.selectRight(lorem.length() + 1, true);
        textViewEmphasize(test, Em.BOLD);

        test.textViewTools.navigate(0);
        test.textViewTools.selectRight(lorem.length() + 1 + dolor.length(), true);
        textViewEmphasize(test, Em.BOLD);

        getFirstSectionChild(test)
                .nextChildIsText(lorem + " " + dolor)
                .noNextChild();
    }

    //	@Test
    public void boldIfNotAllTextNodesAreBold() {
        BBTestRunner test = new BBTestRunner(endBoldedBlock);

        test.textViewTools.selectRight(lorem.length() + 1 + dolor.length(), true);
        textViewEmphasize(test, Em.BOLD);

        getFirstSectionChild(test)
                .nextChildIs(c -> c.isInlineEmphasis(EmphasisType.BOLD).nextChildIsText(lorem + " " + dolor))
                .noNextChild();
    }

    /// /	@Test
    public void boldAndUnboldMultipleBlocks() {
        BBTestRunner test = new BBTestRunner(multiBlock);
        test.textViewTools.selectRight(lorem.length() * 3, true);
        textViewEmphasize(test, Em.BOLD);

        test.assertInnerSection_NoBrlCopy()
                .nextChildIs(c -> c.isBlock(BBX.BLOCK.DEFAULT)
                        .nextChildIs(c2 -> c2.isInlineEmphasis(EmphasisType.BOLD).nextChildIsText(lorem)))
                .nextChildIs(c -> c.isBlock(BBX.BLOCK.DEFAULT)
                        .nextChildIs(c2 -> c2.isInlineEmphasis(EmphasisType.BOLD).nextChildIsText(lorem)))
                .nextChildIs(c -> c.isBlock(BBX.BLOCK.DEFAULT)
                        .nextChildIs(c2 -> c2.isInlineEmphasis(EmphasisType.BOLD).nextChildIsText(lorem)))
                .noNextChild();

        test.textViewTools.navigate(0);
        test.textViewTools.selectRight(lorem.length() * 3, true);
        textViewEmphasize(test, Em.BOLD);

        test.assertInnerSection_NoBrlCopy()
                .nextChildIs(c -> c.isBlock(BBX.BLOCK.DEFAULT).nextChildIsText(lorem))
                .nextChildIs(c -> c.isBlock(BBX.BLOCK.DEFAULT).nextChildIsText(lorem))
                .nextChildIs(c -> c.isBlock(BBX.BLOCK.DEFAULT).nextChildIsText(lorem))
                .noNextChild();
    }

    //	@Test
    public void disableBoldOnPageNum() {
        BBTestRunner test = new BBTestRunner(blocksWithPageNum);

        test.textViewTools.navigateToLine(1);
        textViewEmphasizeLine(test, Em.BOLD);

        getInnerSection(test)
                .nextChildIs(c -> c.isBlock(BBX.BLOCK.DEFAULT).nextChildIsText(lorem))
                .nextChildIs(c -> c.isBlock(BBX.BLOCK.PAGE_NUM))
                .nextChildIs(c -> c.isBlock(BBX.BLOCK.DEFAULT).nextChildIsText(dolor))
                .noNextChild();
    }

    //	@Test
    public void boldBeginning() {
        BBTestRunner test = new BBTestRunner(basicBlock);

        test.textViewTools.selectRight(lorem.length() / 2, true);
        textViewEmphasize(test, Em.BOLD);

        getFirstSectionChild(test)
                .nextChildIs((c) -> c.isInlineEmphasis(EmphasisType.BOLD))
                .nextChildIsText(lorem.substring(lorem.length() / 2))
                .noNextChild();
    }

    //	@Test
    public void boldMiddle() {
        BBTestRunner test = new BBTestRunner(basicBlock);

        doPendingSWTWork();
        test.textViewTools.navigate(lorem.length() / 3);
        doPendingSWTWork();
        test.textViewTools.selectRight(lorem.length() / 3, true);
        textViewEmphasize(test, Em.BOLD);

        getFirstSectionChild(test)
                .nextChildIsText(lorem.substring(0, lorem.length() / 3))
                .nextChildIs(c -> c.isInlineEmphasis(EmphasisType.BOLD))
                .nextChildIsText(lorem.substring((lorem.length() / 3) * 2))
                .noNextChild();
    }

    //	@Test
    public void boldEnd() {
        BBTestRunner test = new BBTestRunner(basicBlock);

        doPendingSWTWork();
        test.textViewTools.navigate(lorem.length() / 2);
        doPendingSWTWork();
        test.textViewTools.selectToEndOfLine();
        textViewEmphasize(test, Em.BOLD);

        getFirstSectionChild(test)
                .nextChildIsText(lorem.substring(0, lorem.length() / 2))
                .nextChildIs(c -> c.isInlineEmphasis(EmphasisType.BOLD))
                .noNextChild();
    }

    //	@Test
    public void boldOnOtherEmphasis() {
        BBTestRunner test = new BBTestRunner(complexBlock);

        test.textViewTools.selectRight(lorem.length() * 3, true);
        test.textViewTools.pressShortcut(Em.BOLD.mod, Em.BOLD.keyCode);

        getFirstSectionChild(test)
                .nextChildIs(childAssert -> childAssert
                        .isInlineEmphasis(EmphasisType.BOLD)
                        .hasText(lorem)
                )
                .nextChildIs(child ->
                        child.isInlineEmphasis(EmphasisType.BOLD, EmphasisType.ITALICS)
                                .hasText(lorem)
                )
                .nextChildIs(child -> child.isInlineEmphasis(EmphasisType.BOLD, EmphasisType.UNDERLINE).hasText(lorem))
                .noNextChild();
    }

    //	@Test
    public void mergeEmphasisTogether() {
        BBTestRunner test = new BBTestRunner(basicBlock);

        test.textViewTools.navigateToText(lorem);
        test.textViewTools.selectRight(lorem.length() / 2, true);
        textViewEmphasize(test, Em.BOLD);

        test.textViewTools.navigateToText(lorem.substring(lorem.length() / 2));
        test.textViewTools.selectRight((int) Math.ceil(lorem.length() / 2.0), true);
        textViewEmphasize(test, Em.BOLD);

        getFirstSectionChild(test)
                .nextChildIs(c -> c.isInlineEmphasis(EmphasisType.BOLD).nextChildIsText(lorem)).noNextChild();

    }

    /* STYLE VIEW TESTS */

    //	@Test
    public void styleViewUnbold() {
        BBTestRunner test = new BBTestRunner(boldedBlock);

        test.textViewTools.navigateToText(lorem);
        test.selectBreadcrumbsAncestor(0, BBX.INLINE.EMPHASIS::assertIsA);
        test.textViewTools.pressShortcut(SWT.MOD1, 'b');

        getFirstSectionChild(test)
                .nextChildIsText(lorem)
                .noNextChild();
    }

    //	@Test
    public void styleViewEmphasizeElement() {
        BBTestRunner test = new BBTestRunner(basicBlock);
        test.textViewTools.navigateToText(lorem);
        test.selectBreadcrumbsAncestor(0, BBX.BLOCK::assertIsA);
        test.textViewTools.pressShortcut(SWT.MOD1, 'b');

        getFirstSectionChild(test)
                .nextChildIs(c -> c.isInlineEmphasis(EmphasisType.BOLD).hasText(lorem))
                .noNextChild();
    }

    /**
     * This used to select the two paragraph elements itself, now just selects the text
     */
//	@Test
    public void styleViewEmphasizeMultiElements() {
        BBTestRunner test = new BBTestRunner(multiBlock);
        test.textViewTools.navigateToText(lorem);
        test.textViewTools.selectRight(lorem.length(), true);
        test.textViewTools.pressShortcut(SWT.SHIFT, SWT.ARROW_DOWN, '\0');
        test.textViewTools.pressShortcut(SWT.MOD1, 'b');

        test.assertInnerSection_NoBrlCopy()
                .nextChildIs(c -> c.isBlock(BBX.BLOCK.DEFAULT)
                        .nextChild().isInlineEmphasis(EmphasisType.BOLD).hasText(lorem))
                .nextChildIs(c -> c.isBlock(BBX.BLOCK.DEFAULT)
                        .nextChild().isInlineEmphasis(EmphasisType.BOLD).hasText(lorem))
                .nextChildIs(c -> c.isBlock(BBX.BLOCK.DEFAULT)
                        .nextChildIsText(lorem))
                .noNextChild();
    }

    /// /	@Test
    public void styleViewEmphasizeSection() {
        BBTestRunner test = new BBTestRunner(multiBlock);
        test.textViewTools.navigateToText(lorem);
        test.selectBreadcrumbsAncestor(1, BBX.SECTION::assertIsA);
        test.textViewTools.pressShortcut(SWT.MOD1, 'b');

        test.assertInnerSection_NoBrlCopy()
                .nextChildIs(c -> c.isBlock(BBX.BLOCK.DEFAULT)
                        .nextChild().isInlineEmphasis(EmphasisType.BOLD).hasText(lorem))
                .nextChildIs(c -> c.isBlock(BBX.BLOCK.DEFAULT)
                        .nextChild().isInlineEmphasis(EmphasisType.BOLD).hasText(lorem))
                .nextChildIs(c -> c.isBlock(BBX.BLOCK.DEFAULT)
                        .nextChild().isInlineEmphasis(EmphasisType.BOLD).hasText(lorem))
                .noNextChild();
    }

    /**
     * This test selected "lorem ipsum[img][/img]" which can't happen anymore in the text view.
     * Next best thing is to select the wrapping block
     */
//	@Test
    public void rt4380_1() {
        BBTestRunner test = new BBTestRunner(bugRT4380);

        test.textViewTools.navigateToText(lorem);
        test.selectBreadcrumbsAncestor(0, BBX.BLOCK::assertIsA);
        test.textViewTools.pressShortcut(SWT.MOD1, 'b');

        getFirstSectionChild(test)
                .nextChildIs(c -> c.isInlineEmphasis(EmphasisType.BOLD).nextChildIsText(lorem))
                .nextChildIs(c -> c.isSpan(BBX.SPAN.IMAGE))
                .nextChildIs(c -> c.isInlineEmphasis(EmphasisType.BOLD).nextChildIsText(dolor))
                .noNextChild();
    }

    /* TEXT VIEW REMOVE ALL EMPHASIS TESTS */
//	@Test
    public void basicRemoveAllEmphasis() {
        BBTestRunner test = new BBTestRunner(complexBlock);

        test.textViewTools.selectRight(lorem.length() * 3, true);
        removeEmphasis(test.textViewTools);

        getFirstSectionChild(test).nextChildIsText(lorem + lorem + lorem).noNextChild();
    }

    //	@Test
    public void removeAllEmphasisNormalizesText() {
        BBTestRunner test = new BBTestRunner(middleBoldedBlock);

        test.textViewTools.selectRight((lorem.length() * 2) + dolor.length(), true);
        removeEmphasis(test.textViewTools);

        getFirstSectionChild(test).nextChildIsText(lorem + " " + dolor + " " + lorem).noNextChild();
    }

    //	@Test
    public void removeAllEmphasisMultipleEmphasis() {
        BBTestRunner test = new BBTestRunner(multiEmphasisComplexBlock);

        test.textViewTools.selectRight(lorem.length() + dolor.length(), true);
        removeEmphasis(test.textViewTools);

        getFirstSectionChild(test).nextChildIsText(lorem + dolor).noNextChild();
    }

    //	@Test
    public void removeAllEmphasisMultiBlock() {
        BBTestRunner test = new BBTestRunner(multiBlockMultiEmphasis);

        test.textViewTools.selectRight((lorem.length() + dolor.length()) * 2, true);
        removeEmphasis(test.textViewTools);

        test.assertInnerSection_NoBrlCopy()
                .nextChildIs(c -> c.isBlock(BBX.BLOCK.DEFAULT).nextChildIsText(lorem + dolor))
                .nextChildIs(c -> c.isBlock(BBX.BLOCK.DEFAULT).nextChildIsText(lorem + dolor))
                .noNextChild();
    }

    //	@Test
    public void removeAllEmphasisPartial() {
        BBTestRunner test = new BBTestRunner(multiEmphasis);

        test.textViewTools.navigateToText("ipsumdolor");
        test.textViewTools.selectRight("ipsumdolor".length());
        assertEquals(test.textViewTools.getSelectionStripped(), "ipsumdolor");
        removeEmphasis(test.textViewTools);

        test.assertRootSectionFirst_NoBrlCopy()
                .nextChildIs(childAssert -> childAssert
                        .isInlineEmphasis(EmphasisType.BOLD)
                        .hasText("lorem ")
                ).nextChildIsText("ipsumdolor")
                .nextChildIs(childAssert -> childAssert
                        .isInlineEmphasis(EmphasisType.ITALICS)
                        .hasText(" sit amet")
                );
    }

    //	@Test
    public void removeAllEmphasisPartialWithStartUnemphasisedText() {
        BBTestRunner test = new BBTestRunner("", "<p>" + lorem + "<em>" + dolor + "</em></p>");

        test.textViewTools.navigateToText("ipsumdolor");
        test.textViewTools.selectRight("ipsumdolor".length());
        assertEquals(test.textViewTools.getSelectionStripped(), "ipsumdolor");
        removeEmphasis(test.textViewTools);

        test.assertRootSectionFirst_NoBrlCopy()
                .nextChildIsText(lorem + "dolor")
                .nextChildIs(childAssert -> childAssert
                        .isInlineEmphasis(EmphasisType.ITALICS)
                        .hasText(" sit amet")
                ).noNextChild();
    }

    //@Test
    public void removeAllEmphasisPartialWithEndUnemphasisedText() {
        BBTestRunner test = new BBTestRunner("", "<p><em>" + lorem + "</em>" + dolor + "</p>");

        test.textViewTools.navigateToText("ipsumdolor");
        test.textViewTools.selectRight("ipsumdolor".length());
        assertEquals(test.textViewTools.getSelectionStripped(), "ipsumdolor");
        removeEmphasis(test.textViewTools);

        test.assertRootSectionFirst_NoBrlCopy()
                .nextChildIs(childAssert -> childAssert
                        .isInlineEmphasis(EmphasisType.ITALICS)
                        .hasText("lorem ")
                )
                .nextChildIsText("ipsum" + dolor)
                .noNextChild();
    }

    /* STYLE VIEW REMOVE ALL EMPHASIS TESTS */

    //@Test
    public void removeAllEmphasisBlockElement() {
        BBTestRunner test = new BBTestRunner(boldedBlock);

        test.textViewTools.navigateToText(lorem);
        test.textViewTools.selectRight(lorem.length(), true);
        removeEmphasis(test.textViewTools);

        getFirstSectionChild(test).nextChildIsText(lorem);
    }

    //@Test
    public void removeAllEmphasisSection() {
        BBTestRunner test = new BBTestRunner(multiBlockMultiEmphasis);

        test.selectBreadcrumbsAncestor(2, BBX.SECTION::isA);
        removeEmphasis(test.textViewTools);

        test.assertInnerSection_NoBrlCopy()
                .nextChildIs(c -> c.nextChildIsText(lorem + dolor))
                .nextChildIs(c -> c.nextChildIsText(lorem + dolor))
                .noNextChild();
    }

    //@Test
    public void removeAllEmphasisFromSubtextStyleView() {
        BBTestRunner test = new BBTestRunner(multiEmphasis);

        test.textViewTools.navigateToText("dolor");
        test.textViewTools.selectRight(2);
        removeEmphasis(test.textViewTools);

        test.assertRootSectionFirst_NoBrlCopy()
                .nextChildIs(childAssert -> childAssert
                        .isInlineEmphasis(EmphasisType.BOLD)
                        .hasText(lorem)
                ).nextChildIsText(dolor.substring(0, 2))
                .nextChildIs(childAssert -> childAssert
                        .isInlineEmphasis(EmphasisType.ITALICS)
                        .hasText(dolor.substring(2))
                ).noNextChild();
    }

    //	@Test
    public void removeAllEmphasisInlineElement() {
        BBTestRunner test = new BBTestRunner(multiEmphasis);

        test.textViewTools.navigateToText("lorem");
        test.selectBreadcrumbsAncestor(0, BBX.INLINE.EMPHASIS::assertIsA);
        removeEmphasis(test.textViewTools);

        test.assertRootSectionFirst_NoBrlCopy()
                .nextChildIsText(lorem)
                .nextChildIs(childAssert -> childAssert
                        .isInlineEmphasis(EmphasisType.ITALICS)
                        .hasText(dolor)
                ).noNextChild();
    }


    @DataProvider(name = "allEmphasis")
    public Object[][] allEmphasis() {
        Object[][] returnArray = new Object[Em.values().length][1];
        for (int i = 0; i < Em.values().length; i++) {
            returnArray[i][0] = Em.values()[i];
        }
        return returnArray;
    }

    public enum Em {
        BOLD(SWT.MOD1, 'b', EmphasisType.BOLD),
        ITALIC(SWT.MOD1, 'i', EmphasisType.ITALICS),
        UNDERLINE(SWT.MOD1, 'u', EmphasisType.UNDERLINE);
        final int mod;
        final char keyCode;
        final EmphasisType emType;

        Em(int mod, char keyCode, EmphasisType emType) {
            this.mod = mod;
            this.keyCode = keyCode;
            this.emType = emType;
        }
    }

    private void textViewEmphasizeLine(BBTestRunner test, Em emphasis) {
        test.textViewTools.pressShortcut(SWT.HOME, '\0');
        test.textViewTools.selectToEndOfLine();
        textViewEmphasize(test, emphasis);
    }

    private void textViewEmphasize(BBTestRunner test, Em emphasis) {
        test.textViewTools.pressShortcut(emphasis.mod, emphasis.keyCode);
        test.updateTextView();
    }

    private void removeEmphasis(ViewTools tools) {
        tools.pressShortcut(EmphasisModule.REMOVE_ALL_EMPHASIS_MODIFIER, EmphasisModule.REMOVE_ALL_EMPHASIS_ACCELERATOR);
    }

    //	@Test
    public void removeBold_nestedSpan_noLingeringEmphasis_first() {
        // In this example the unemphasised text was wrapped with [I-EMPHASIS] with no bits
        BBTestRunner bbTest = new BBTestRunner(
                "",
                "<p><span><strong>William L. McBride</strong></span> Curriculum</p>"
        );

        bbTest.textViewTools.navigateToText("L. McBride");
        bbTest.textViewTools.selectRight("L. McBride".length());
        bbTest.textViewTools.pressShortcut(SWT.CTRL, 'b');

        bbTest.assertRootSectionFirst_NoBrlCopy()
                .isBlockDefaultStyle("Body Text")
                .nextChildIs(childAssert -> childAssert
                        .isSpan(BBX.SPAN.OTHER)
                        .nextChildIs(childAssert2 -> childAssert2
                                .isInlineEmphasis(EmphasisType.BOLD)
                                .hasText("William ")
                        ).nextChildIsText("L. McBride")
                        .noNextChild()
                ).nextChildIsText(" Curriculum")
                .noNextChild();
    }

    //	@Test
    public void removeBold_nestedSpan_noLingeringEmphasis_second() {
        // In this example the Bride text was wrapped with [I-EMPHASIS] with no bits
        BBTestRunner bbTest = new BBTestRunner(
                "",
                "<p><span><strong>William L. McBride</strong></span> Curriculum</p>"
        );

        bbTest.textViewTools.navigateToText("L. Mc");
        bbTest.textViewTools.selectRight("L. Mc".length());
        bbTest.textViewTools.pressShortcut(SWT.CTRL, 'b');

        bbTest.textViewTools.navigateToTextRelative("Bride");
        bbTest.textViewTools.selectRight("Bride".length());
        bbTest.textViewTools.pressShortcut(SWT.CTRL, 'b');

        bbTest.assertRootSectionFirst_NoBrlCopy()
                .isBlockDefaultStyle("Body Text")
                .nextChildIs(childAssert -> childAssert
                        .isSpan(BBX.SPAN.OTHER)
                        .nextChildIs(childAssert2 -> childAssert2
                                .isInlineEmphasis(EmphasisType.BOLD)
                                .hasText("William ")
                        ).nextChildIsText("L. McBride")
                        .noNextChild()
                ).nextChildIsText(" Curriculum")
                .noNextChild();
    }

    //	@Test
    public void rt6239_mergeTextNodesOnUnbold() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>were</p>");

        bbTest.textViewTools.navigateToText("were");
        bbTest.textViewTools.selectToEndOfLine();
        bbTest.textViewTools.pressShortcut(SWT.CTRL, 'b');

        bbTest.textViewTools.navigateToText("were");
        bbTest.textViewTools.selectRight(3);
        bbTest.textViewTools.pressShortcut(SWT.CTRL, 'b');

        bbTest.assertRootSectionFirst_NoBrlCopy()
                .isBlockDefaultStyle("Body Text")
                .nextChildIsText("wer")
                .nextChildIs(child -> child.isInlineEmphasis(EmphasisType.BOLD).hasText("e"))
                .noNextChild();

        bbTest.textViewTools.navigate(3);
        bbTest.textViewTools.selectRight(1);
        bbTest.textViewTools.pressShortcut(SWT.CTRL, 'b');

        bbTest.assertRootSectionFirst_NoBrlCopy()
                .isBlockDefaultStyle("Body Text")
                .nextChildIsText("were")
                .noNextChild();
    }
}
