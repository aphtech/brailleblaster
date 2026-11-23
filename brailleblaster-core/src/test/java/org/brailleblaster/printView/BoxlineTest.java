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

import org.brailleblaster.testrunners.BBTestRunner;
//import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.testng.annotations.Test;

@GuiTest
public class BoxlineTest {
    private final String BLANK_LINE = "";
    private final String BOXLINE_TEXT = "----------------------------------------";
    private final String TOP_BOXLINE = "7777777777777777777777777777777777777777";
    //	private final String FULL_BOX = "========================================";
    private final String BOTTOM_BOXLINE = "gggggggggggggggggggggggggggggggggggggggg";

    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/printView/BoxLineTests.xml");

    /*
     * Tests adding a boxline to a single paragraph surround by two other paragraphs with Book Tree
     */
    @Test(enabled = false)
    public void basicBoxline() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Second Paragraph on Page 2";
        String expectedBraille = "  ,second ,p>agraph on ,page #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.selectToolbarOption("Box");

        assertEquals(bbTest.textViewBot.getTextOnLine(3), BLANK_LINE);
        assertEquals(bbTest.textViewBot.getTextOnLine(4), BOXLINE_TEXT);
        assertEquals(bbTest.textViewBot.getTextOnLine(5), expectedText);
        assertEquals(bbTest.textViewBot.getTextOnLine(6), BOXLINE_TEXT);

        assertEquals(bbTest.brailleViewBot.getTextOnLine(3), BLANK_LINE);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(4), TOP_BOXLINE);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(5), expectedBraille);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(6), BOTTOM_BOXLINE);
    }

    /*
     * Adds a simple boxline to the first element at the start of a document
     */
    @Test(enabled = false)
    public void basicBoxline_startOfDocument() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String shortBox = "----------------------------------------";
        String brailleBox = "7777777777777777777777777777777777777777";
        String expectedText = "First paragraph on page 1";
        String expectedBraille = "  ,f/ p>~1agraph on page #a";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(0));
        bbTest.selectToolbarOption("Box");

        assertEquals(bbTest.textViewBot.getTextOnLine(0), shortBox);
        assertEquals(bbTest.textViewBot.getTextOnLine(1), expectedText);
        assertEquals(bbTest.textViewBot.getTextOnLine(2), BOXLINE_TEXT);

        assertEquals(bbTest.brailleViewBot.getTextOnLine(0), brailleBox);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(1), expectedBraille);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(2), BOTTOM_BOXLINE);
    }

    /*
     * Adds a basic boxline to an element at the end of a document
     */
    @Test(enabled = false)
    public void basicBoxline_endOfDocument() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "last element in view";
        String expectedBraille = "  la/ ele;t 9 view";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(17));
        bbTest.selectToolbarOption("Box");

        assertEquals(bbTest.textViewBot.getTextOnLine(17), BLANK_LINE);
        assertEquals(bbTest.textViewBot.getTextOnLine(18), BOXLINE_TEXT);
        assertEquals(bbTest.textViewBot.getTextOnLine(19), expectedText);
        assertEquals(bbTest.textViewBot.getTextOnLine(20), BOXLINE_TEXT);

        assertEquals(bbTest.brailleViewBot.getTextOnLine(17), BLANK_LINE);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(18), TOP_BOXLINE);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(19), expectedBraille);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(20), BOTTOM_BOXLINE);
    }

    /*
     * TODO: fix full box
     * TODO: Decipher what "fix full box" actually means
     * Adds a boxline, then tests adding another boxline within the boxline
     */
	/*
	@Test(enabled = false)
	public void boxlineInBoxline_BOOKTREE(){
		BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
		String expectedText = "Second Paragraph on Page 2";
		String expectedBraille = "  ,second ,p>agraph on ,page #b";
		
//		bbTest.selectTree(bbTest.bot, BOOKTREE);
		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
		bbTest.selectToolbarOption("Box");
		
		bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
		bbTest.selectToolbarOption("Full Box");
		
		assertEquals(bbTest.textViewBot.getTextOnLine(3), BLANK_LINE);
		assertEquals(bbTest.textViewBot.getTextOnLine(4), FULL_BOX);
		assertEquals(bbTest.textViewBot.getTextOnLine(5), BOXLINE_TEXT);
		assertEquals(bbTest.textViewBot.getTextOnLine(6), expectedText);
		assertEquals(bbTest.textViewBot.getTextOnLine(7), BOXLINE_TEXT);
		assertEquals(bbTest.textViewBot.getTextOnLine(8), FULL_BOX);
		
		assertEquals(bbTest.brailleViewBot.getTextOnLine(3), BLANK_LINE);
		assertEquals(bbTest.brailleViewBot.getTextOnLine(4), FULL_BOX);
		assertEquals(bbTest.brailleViewBot.getTextOnLine(5), TOP_BOXLINE);
		assertEquals(bbTest.brailleViewBot.getTextOnLine(6), expectedBraille);
		assertEquals(bbTest.brailleViewBot.getTextOnLine(7), BOTTOM_BOXLINE);
		assertEquals(bbTest.brailleViewBot.getTextOnLine(7), FULL_BOX);
	}
	*/

    /*
     * Selects three paragraphs, then adds a boxline around them as a group
     */
    @Test(enabled = false)
    public void multipleSelectionBoxline() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Page 2 paragraph";
        String expectedBraille = "  ,page #b p>agraph";
        String expectedText2 = "Second Paragraph on Page 2";
        String expectedBraille2 = "  ,second ,p>agraph on ,page #b";
        String expectedText3 = "3rd paragraph on Page 2";
        String expectedBraille3 = "  #crd p>agraph on ,page #b";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        int length = (bbTest.textViewWidget.getOffsetAtLine(4) - bbTest.textViewWidget.getOffsetAtLine(2)) + 1;
        bbTest.textViewTools.selectRight(length);
        bbTest.selectToolbarOption("Box");

        assertEquals(bbTest.textViewBot.getTextOnLine(2), BLANK_LINE);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(2), BLANK_LINE);

        assertEquals(bbTest.textViewBot.getTextOnLine(3), BOXLINE_TEXT);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(3), TOP_BOXLINE);

        assertEquals(bbTest.textViewBot.getTextOnLine(4), expectedText);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(4), expectedBraille);

        assertEquals(bbTest.textViewBot.getTextOnLine(5), expectedText2);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(5), expectedBraille2);

        assertEquals(bbTest.textViewBot.getTextOnLine(6), expectedText3);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(6), expectedBraille3);

        assertEquals(bbTest.textViewBot.getTextOnLine(7), BOXLINE_TEXT);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(7), BOTTOM_BOXLINE);
    }

    /**
     * Adds a boxline around a paragraph, then selects the paragraph before the boxline, the top boxline, and the paragraph inside the boxline.
     * This test checks that the second boxline is added.
     */
    @Test(enabled = false)
    public void invalidBoxLine() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText1 = "Page 2 paragraph";
        String expectedBraille1 = "  ,page #b p>agraph";
        String expectedText2 = "Second Paragraph on Page 2";
        String expectedBraille2 = "  ,second ,p>agraph on ,page #b";

        //bbTest.selectTree(bbTest.bot, BOOKTREE);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.selectToolbarOption("Box");

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(2));
        int length = (bbTest.textViewWidget.getOffsetAtLine(5) - bbTest.textViewWidget.getOffsetAtLine(2)) + 1;
        bbTest.textViewTools.selectRight(length);
        bbTest.selectToolbarOption("Box");

        assertEquals(bbTest.textViewBot.getTextOnLine(3), BOXLINE_TEXT);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(3), TOP_BOXLINE);

        assertEquals(bbTest.textViewBot.getTextOnLine(4), expectedText1);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(4), expectedBraille1);

        assertEquals(bbTest.textViewBot.getTextOnLine(6), BOXLINE_TEXT);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(6), TOP_BOXLINE);

        assertEquals(bbTest.textViewBot.getTextOnLine(7), expectedText2);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(7), expectedBraille2);

        assertEquals(bbTest.textViewBot.getTextOnLine(8), BOXLINE_TEXT);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(8), BOTTOM_BOXLINE);

        assertEquals(bbTest.textViewBot.getTextOnLine(9), BOXLINE_TEXT);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(9), BOTTOM_BOXLINE);
    }

    //places the cursor in a boxline and attempts to remove it, top and bottom lines should be removed
    @Test(enabled = false)
    public void boxLine_Remove_Selection() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        String expectedText = "Page 2 paragraph";
        String expectedBraille = "  ,page #b p>agraph";
        String expectedText2 = "Second Paragraph on Page 2";
        String expectedBraille2 = "  ,second ,p>agraph on ,page #b";
        String expectedText3 = "3rd paragraph on Page 2";
        String expectedBraille3 = "  #crd p>agraph on ,page #b";

        //bbTest.selectTree(bbTest.bot, XMLTREE);
        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(3));
        bbTest.selectToolbarOption("Box");

        assertEquals(bbTest.textViewBot.getTextOnLine(4), BOXLINE_TEXT);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(4), TOP_BOXLINE);

        assertEquals(bbTest.textViewBot.getTextOnLine(5), expectedText2);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(5), expectedBraille2);

        assertEquals(bbTest.textViewBot.getTextOnLine(6), BOXLINE_TEXT);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(6), BOTTOM_BOXLINE);

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(4));
        bbTest.selectToolbarOption("Box");

        assertEquals(bbTest.textViewBot.getTextOnLine(2), expectedText);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(2), expectedBraille);

        assertEquals(bbTest.textViewBot.getTextOnLine(3), expectedText2);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(3), expectedBraille2);

        assertEquals(bbTest.textViewBot.getTextOnLine(4), expectedText3);
        assertEquals(bbTest.brailleViewBot.getTextOnLine(4), expectedBraille3);
    }
}
