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
import static org.brailleblaster.TestUtils.getInnerSection;
import static org.testng.Assert.assertEquals;

import java.io.File;

import org.brailleblaster.bbx.BBX;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.utd.properties.EmphasisType;
import org.eclipse.swt.SWT;
import org.testng.annotations.Test;

import nu.xom.Document;
import org.brailleblaster.TestGroups;
import org.brailleblaster.TestUtils;
import org.brailleblaster.frontmatter.VolumeTest;
import org.brailleblaster.util.Notify;

@GuiTest
public class BlankDocumentTest {
    private final Document blankDoc = TestXMLUtils.generateBookDoc("", "<p></p>");
    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/printView/blankTemplate.bbx");
    private static final String BLANK_LINE = "";

    @Test
    public void basicParagraph() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>agraph #a";

        bbTest.navigateTextView(0);
        bbTest.textViewTools.typeText(expectedText);
        bbTest.textViewTools.pressKey(SWT.CR, 1);

        assertEquals(bbTest.textViewBot.getTextOnLine(0), expectedText);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(0), expectedBraille);

        assertEquals(bbTest.textViewBot.getTextOnLine(1), BLANK_LINE);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(1), BLANK_LINE);
    }

    @Test
    public void multipleParagraph() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Paragraph 1";
        String expectedBraille = "  ,p>agraph #a";
        String expectedText2 = "Paragraph 2";
        String expectedBraille2 = "  ,p>agraph #b";
        String expectedText3 = "Paragraph 3";
        String expectedBraille3 = "  ,p>agraph #c";

        bbTest.navigateTextView(0);
        bbTest.textViewTools.typeText(expectedText);
        bbTest.textViewTools.pressKey(SWT.CR, 1);

        bbTest.textViewTools.typeText(expectedText2);
        bbTest.textViewTools.pressKey(SWT.CR, 1);

        bbTest.textViewTools.typeText(expectedText3);
        bbTest.textViewTools.pressKey(SWT.CR, 1);

        assertEquals(bbTest.textViewBot.getTextOnLine(0), expectedText);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(0), expectedBraille);

        assertEquals(bbTest.textViewBot.getTextOnLine(1), expectedText2);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(1), expectedBraille2);

        assertEquals(bbTest.textViewBot.getTextOnLine(2), expectedText3);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(2), expectedBraille3);

        assertEquals(bbTest.textViewBot.getTextOnLine(3), BLANK_LINE);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(3), BLANK_LINE);
    }

    //New volume cannot be inserted in an empty document
    @Test
    public void insertVolume() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        TestUtils.assertException(Notify.DebugException.class,
                () -> VolumeTest.openInsertVolume(bbTest, BBX.VolumeType.VOLUME)
        );

        assertEquals(bbTest.textViewBot.getTextOnLine(0), BLANK_LINE);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(0), BLANK_LINE);
    }

    //note separation line cannot be inserted in an empty document
    @Test
    public void noteSeperationLine() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        TestUtils.assertException(
                Notify.DebugException.class,
                () -> bbTest.selectToolbarOption("Note Separation Line")
        );

        assertEquals(bbTest.textViewBot.getTextOnLine(0), BLANK_LINE);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(0), BLANK_LINE);
    }

    //image placeholder cannot be inserted in an empty document
    // Disabled: Image Manager no longer shows the empty-document warning on a blank document; product decision (test_fix.md Phase 7b)
    @Test(enabled = false)
    public void imagePlaceHolder() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        TestUtils.assertException(
                Notify.DebugException.class,
                () -> bbTest.selectToolbarOption(org.brailleblaster.utils.localization.LocaleHandler.getDefault().get("&ImagePlaceholder"))
        );

        assertEquals(bbTest.textViewBot.getTextOnLine(0), BLANK_LINE);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(0), BLANK_LINE);
    }

    @Test(groups = TestGroups.TODO_FIX_LATER) // Issue #6405: We don't do anything on empty documents
    public void blankDocumentEmphasisTest() {

        BBTestRunner test = new BBTestRunner(blankDoc);
        test.textViewTools.navigateToLine(0);
        test.selectBreadcrumbsAncestor(0);
        test.textViewTools.pressShortcut(SWT.MOD1, 'b');
        getInnerSection(test).child(1).isBlock(BBX.BLOCK.DEFAULT)
                .nextChild().isInlineEmphasis(EmphasisType.BOLD);


        undo(test);
        test.textViewTools.navigateToLine(0);
        test.selectBreadcrumbsAncestor(0);
        test.textViewTools.pressShortcut(SWT.MOD1, 'i');
        getInnerSection(test).child(1).isBlock(BBX.BLOCK.DEFAULT)
                .nextChild().isInlineEmphasis(EmphasisType.ITALICS);

        undo(test);
        test.textViewTools.navigateToLine(0);
        test.selectBreadcrumbsAncestor(0);
        test.textViewTools.pressShortcut(SWT.MOD1, 'u');
        getInnerSection(test).child(1).isBlock(BBX.BLOCK.DEFAULT)
                .nextChild().isInlineEmphasis(EmphasisType.UNDERLINE);

        undo(test);
        test.textViewTools.navigateToLine(0);
        test.selectBreadcrumbsAncestor(0);
        test.textViewTools.pressShortcut(SWT.SHIFT + SWT.ALT, 's');
        getInnerSection(test).child(1).isBlock(BBX.BLOCK.DEFAULT)
                .nextChild().isInlineEmphasis(EmphasisType.SCRIPT);

        undo(test);
        test.textViewTools.navigateToLine(0);
        test.selectBreadcrumbsAncestor(0);
        test.textViewTools.pressShortcut(SWT.SHIFT + SWT.ALT, '1');
        getInnerSection(test).child(1).isBlock(BBX.BLOCK.DEFAULT)
                .nextChild().isInlineEmphasis(EmphasisType.TRANS_1);

        undo(test);
        test.textViewTools.navigateToLine(0);
        test.selectBreadcrumbsAncestor(0);
        test.textViewTools.pressShortcut(SWT.SHIFT + SWT.ALT, '2');
        getInnerSection(test).child(1).isBlock(BBX.BLOCK.DEFAULT)
                .nextChild().isInlineEmphasis(EmphasisType.TRANS_2);

        undo(test);
        test.textViewTools.navigateToLine(0);
        test.selectBreadcrumbsAncestor(0);
        test.textViewTools.pressShortcut(SWT.SHIFT + SWT.ALT, '3');
        getInnerSection(test).child(1).isBlock(BBX.BLOCK.DEFAULT)
                .nextChild().isInlineEmphasis(EmphasisType.TRANS_3);

        undo(test);
        test.textViewTools.navigateToLine(0);
        test.selectBreadcrumbsAncestor(0);
        test.textViewTools.pressShortcut(SWT.SHIFT + SWT.ALT, '4');
        getInnerSection(test).child(1).isBlock(BBX.BLOCK.DEFAULT)
                .nextChild().isInlineEmphasis(EmphasisType.TRANS_4);

        undo(test);
        test.textViewTools.navigateToLine(0);
        test.selectBreadcrumbsAncestor(0);
        test.textViewTools.pressShortcut(SWT.SHIFT + SWT.ALT, '5');
        getInnerSection(test).child(1).isBlock(BBX.BLOCK.DEFAULT)
                .nextChild().isInlineEmphasis(EmphasisType.TRANS_5);
    }

    private void undo(BBTestRunner bbTest) {
        bbTest.textViewTools.pressShortcut(SWT.MOD1, 'z');
    }
}
