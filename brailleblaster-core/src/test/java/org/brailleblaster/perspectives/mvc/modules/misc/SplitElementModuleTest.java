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
package org.brailleblaster.perspectives.mvc.modules.misc;

import org.brailleblaster.perspectives.braille.messages.AdjustLocalStyleMessage;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.BBTestRunner.ViewTools;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.utd.Style;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.brailleblaster.utd.properties.EmphasisType;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import static org.brailleblaster.TestUtils.getInnerSection;

import org.brailleblaster.bbx.BBX;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParentNode;

public class SplitElementModuleTest {
    private final String TEST_TEXT = "Testing text";
    private final String DUMMY_TEXT = "Dummy";
    private final int HALFWAY = TEST_TEXT.length() / 2;
    private final Document basicBlock = TestXMLUtils.generateBookDoc("", "<p>" + TEST_TEXT + "</p>");
    private final Document boldedBlock = TestXMLUtils.generateBookDoc("", "<p><strong>" + TEST_TEXT + "</strong></p>");
    private final Document complexBlock = TestXMLUtils.generateBookDoc("",
            "<p>" + DUMMY_TEXT + "<a>" + DUMMY_TEXT + "<a>" + TEST_TEXT + "</a>" + DUMMY_TEXT + "</a>" + DUMMY_TEXT + "</p>");
    private final String P_STYLE = "Body Text";

    @Test(enabled = false)
    public void textViewBasicSplit() {
        BBTestRunner test = new BBTestRunner(basicBlock);
        testBasicSplit(test, test.textViewTools);
    }

    private void testBasicSplit(BBTestRunner test, ViewTools view) {
        splitTestTextInHalf(test, view);

        getInnerSection(test)
                .nextChildIs(c -> c.isBlockWithStyle(P_STYLE)
                        .nextChildIsText(TEST_TEXT.substring(0, HALFWAY)))
                .nextChildIs(c -> c.isBlockWithStyle(P_STYLE)
                        .nextChildIsText(TEST_TEXT.substring(HALFWAY)))
                .noNextChild();
    }

    @Test(enabled = false)
    public void textViewBoldSplit() {
        BBTestRunner test = new BBTestRunner(boldedBlock);
        testBoldSplit(test, test.textViewTools);
    }

    private void testBoldSplit(BBTestRunner test, ViewTools view) {
        splitTestTextInHalf(test, view);

        getInnerSection(test)
                .nextChildIs(c -> c.isBlockWithStyle(P_STYLE)
                        .nextChildIs(c2 -> c2.isInlineEmphasis(EmphasisType.BOLD)
                                .nextChildIsText(TEST_TEXT.substring(0, HALFWAY))))
                .nextChildIs(c -> c.isBlockWithStyle(P_STYLE)
                        .nextChildIs(c2 -> c2.isInlineEmphasis(EmphasisType.BOLD)
                                .nextChildIsText(TEST_TEXT.substring(HALFWAY))))
                .noNextChild();
    }

    @Test(enabled = false)
    public void textViewComplexSplit() {
        BBTestRunner test = new BBTestRunner(complexBlock);

        test.textViewTools.pressKey(Keystrokes.RIGHT.getNaturalKey(), (DUMMY_TEXT.length() * 2) + HALFWAY);
        test.manager.getSimpleManager().getModule(SplitElementModule.class);
        SplitElementModule.splitElement(test.manager.getSimpleManager());

        testComplexSplit(test, test.textViewTools);
    }

    private void testComplexSplit(BBTestRunner test, ViewTools view) {
        getInnerSection(test)
                .nextChildIs(c -> c.isBlockWithStyle(P_STYLE)
                        .nextChildIsText(DUMMY_TEXT)
                        .nextChildIs(c2 -> c2.isSpan(BBX.SPAN.OTHER)
                                .nextChildIsText(DUMMY_TEXT)
                                .nextChildIs(c3 -> c3.isSpan(BBX.SPAN.OTHER)
                                        .nextChildIsText(TEST_TEXT.substring(0, HALFWAY)))))
                .nextChildIs(c -> c.isBlockWithStyle(P_STYLE)
                        .nextChildIs(c2 -> c2.isSpan(BBX.SPAN.OTHER)
                                .nextChildIs(c3 -> c3.isSpan(BBX.SPAN.OTHER)
                                        .nextChildIsText(TEST_TEXT.substring(HALFWAY)))
                                .nextChildIsText(DUMMY_TEXT))
                        .nextChildIsText(DUMMY_TEXT))
                .noNextChild();
    }

    @Test(enabled = false)
    public void textViewSplitDoesNotCopyStyleOptions() {
        BBTestRunner test = new BBTestRunner(basicBlock);
        testSplitDoesNotCopyStyleOptions(test, test.textViewTools);
    }

    private void testSplitDoesNotCopyStyleOptions(BBTestRunner test, ViewTools view) {
        Element block = XMLHandler.Companion.childrenRecursiveVisitor(test.getDoc().getRootElement(), BBX.BLOCK::isA);
        assertNotNull(block);
        test.manager.dispatch(new AdjustLocalStyleMessage.AdjustLinesMessage(block, false, 2));

        splitTestTextInHalf(test, view);

        ParentNode parent = block.getParent();
        assertEquals(parent.getChildCount(), 2);
        assertTrue(BBX.BLOCK.isA(parent.getChild(0)), "First child is not block");
        assertEquals(((Style) test.manager.getStyle(parent.getChild(0))).getBaseStyleName(), P_STYLE, "First child style did not derive from " + P_STYLE);
        assertEquals(test.manager.getStyle(parent.getChild(1)).getName(), P_STYLE, "Second child style is not " + P_STYLE);
    }

    private void splitTestTextInHalf(BBTestRunner test, ViewTools view) {
        view.navigateToText(TEST_TEXT);
        view.pressKey(Keystrokes.RIGHT.getNaturalKey(), HALFWAY);
        test.manager.getSimpleManager().getModule(SplitElementModule.class);
        SplitElementModule.splitElement(test.manager.getSimpleManager());
    }

    @Test(enabled = false)
    public void splitNextToSpan_issue6508() {
        BBTestRunner test = new BBTestRunner("", "<p><span>special</span>text</p>");

        test.textViewTools.navigateToText("text");
        test.textViewTools.typeNewline();

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(p -> p
                        .isBlock(BBX.BLOCK.DEFAULT)
                        .nextChildIs(span -> span
                                .isSpan(BBX.SPAN.OTHER)
                                .onlyChildIsText("special")
                        ).noNextChild()
                ).nextChildIs(p -> p
                        .isBlock(BBX.BLOCK.DEFAULT)
                        // TODO: Bug, text view still thinks it's at the end of special but before text
                        .nextChildIs(span -> span
                                .childCount(0)
                        ).nextChildIsText("text")
                ).noNextChild();
    }
}
