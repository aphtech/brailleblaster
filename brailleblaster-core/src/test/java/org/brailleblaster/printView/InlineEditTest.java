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
package org.brailleblaster.printView;

import org.brailleblaster.GuiTest;
import static org.testng.Assert.assertEquals;

import java.io.File;

import org.brailleblaster.bbx.BBX;
import org.brailleblaster.testrunners.BBTestRunner;
import org.eclipse.swt.SWT;
import org.testng.annotations.Test;

@GuiTest
public class InlineEditTest {
    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/printView/InlineEditTests.xml");

    @Test
    public void inlineDelete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "First pargraph on page 1";
        String expectedBraille = "  ,f/ p>~1graph on page #a";

        bbTest.navigateTextView(9);
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(0));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(0));
    }

    @Test
    public void inlineDelete_before_end() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Secondaragraph on Page 2";
        String expectedBraille = "  ,second~1>agraph on ,page #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 6);
        bbTest.textViewTools.pressKey(SWT.DEL, 2);
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(3));
    }

    @Test
    public void inlineBackspace() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "SecondParagraph on Page 2";
        String expectedBraille = "  ,second~1,p>agraph on ,page #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 7);
        bbTest.textViewTools.pressKey(SWT.BS, 1);
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(3));
    }

    @Test
    public void inlineBackspace_before_start() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Secondaragraph on Page 2";
        String expectedBraille = "  ,second~1>agraph on ,page #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3) + 8);
        bbTest.textViewTools.pressKey(SWT.BS, 2);
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(3));
    }

    @Test
    public void fullElementDelete() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "3rd paragraph on Page 2";
        String expectedBraille = "  #crd p>agraph on ,page #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.textViewTools.selectToEndOfLine();
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(3));

        //Assert that node was deleted
        bbTest.assertRootSection_NoBrlCopy()
                .nextChildIs(child -> child.isSection(BBX.SECTION.OTHER))
                .nextChildIs(bodyMatterSection -> bodyMatterSection.isSection(BBX.SECTION.OTHER)
                        .nextChildIs(level1Section -> level1Section.isSection(BBX.SECTION.OTHER)
                                .nextChildIs(child -> child.isBlockWithStyle("Body Text"))
                                .nextChildIs(child -> child.isBlock(BBX.BLOCK.PAGE_NUM))
                                .nextChildIs(child -> child.isBlockWithStyle("Body Text"))
                                .nextChildIs(child -> child.isBlockWithStyle("Body Text"))
                                .nextChildIs(child -> child.isBlock(BBX.BLOCK.PAGE_NUM))
                        )
                );

        expectedText = "--------------------------------------2";
        expectedBraille = "--------------------------------------#b";

        bbTest.textViewTools.selectToEndOfLine();
        bbTest.textViewTools.pressKey(SWT.DEL, 1);
        bbTest.updateTextView();

        assertEquals(expectedText, bbTest.textViewBot.getTextOnLine(3));
        assertEquals(expectedBraille, bbTest.brailleViewBot.getTextOnLine(3));

        bbTest.assertRootSection_NoBrlCopy()
                .nextChildIs(child -> child.isSection(BBX.SECTION.OTHER))
                .nextChildIs(bodyMatterSection -> bodyMatterSection.isSection(BBX.SECTION.OTHER)
                        .nextChildIs(level1Section -> level1Section.isSection(BBX.SECTION.OTHER)
                                .nextChildIs(child -> child.isBlockWithStyle("Body Text"))
                                .nextChildIs(child -> child.isBlock(BBX.BLOCK.PAGE_NUM))
                                .nextChildIs(child -> child.isBlockWithStyle("Body Text"))
                                .nextChildIs(child -> child.isBlock(BBX.BLOCK.PAGE_NUM))
                        )
                );
    }
}
