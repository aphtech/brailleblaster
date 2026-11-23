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
import static org.testng.Assert.*;

import java.io.File;

import org.brailleblaster.TestGroups;

import org.brailleblaster.perspectives.braille.Manager;
import org.brailleblaster.perspectives.braille.mapping.elements.BoxLineTextMapElement;
import org.brailleblaster.perspectives.braille.mapping.elements.PageIndicatorTextMapElement;
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement;
import org.brailleblaster.testrunners.BBTestRunner;
//import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.swt.SWT;
//import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
//import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.testng.annotations.Test;

@Test(groups = TestGroups.BROKEN_TESTS, enabled = false)
@GuiTest
public class CursorAndOffsetTest {
    //private final String XMLTREE = "XML";

    private static final File TEST_FILE = new File("src/test/resources/org/brailleblaster/printView/ReadOnlyCursorTests.bbx");

    //protected SWTBotTree treeBot;


    //Tests pressing down arrow into a pagenum element and correct update in xml tree
    @Test(enabled = false)
    public void basicArrowDownTest() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "pagenum";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateTextView(bbTest.textViewBot.widget.getOffsetAtLine(4));

        assertTrue(manager.getMapList().getCurrent() instanceof PageIndicatorTextMapElement);
        assertEquals(manager.getTextView().getCaretOffset(), manager.getMapList().getCurrent().getStart(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    public void basicArrowDownTestBraille() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "pagenum";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateBrailleView(bbTest.brailleViewBot.widget.getOffsetAtLine(4));

        assertTrue(manager.getMapList().getCurrent() instanceof PageIndicatorTextMapElement);
        assertEquals(manager.getBrailleView().getCaretOffset(), manager.getMapList().getCurrent().brailleList.getFirst().getStart(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    //tests pr4essing a right key at the end of a line into a pagenum element and correct update in xml tree
    @Test(enabled = false)
    public void basicArrowRightTest() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "pagenum";

        Manager manager = bbTest.manager;
        //treeBot = bbTest.bot.tree(0);
        //bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateTextView(bbTest.textViewBot.widget.getOffsetAtLine(3) + bbTest.textViewBot.widget.getLine(3).length() + 1);

        assertTrue(manager.getMapList().getCurrent() instanceof PageIndicatorTextMapElement);
        assertEquals(manager.getTextView().getCaretOffset(), manager.getMapList().getCurrent().getStart(manager.getMapList()));
        //assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    public void basicArrowRightTestBraille() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "pagenum";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateBrailleView(bbTest.brailleViewBot.widget.getOffsetAtLine(3) + bbTest.brailleViewBot.widget.getLine(3).length() + 1);

        assertTrue(manager.getMapList().getCurrent() instanceof PageIndicatorTextMapElement);
        assertEquals(manager.getBrailleView().getCaretOffset(), manager.getMapList().getCurrent().brailleList.getFirst().getStart(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    //tests pressing up arrow into a pagenum element and corect update in xml tree
    @Test(enabled = false)
    public void basicArrowUpTest() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "pagenum";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateTextView(bbTest.textViewBot.widget.getOffsetAtLine(5));
        bbTest.textViewTools.pressKey(SWT.ARROW_UP, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof PageIndicatorTextMapElement);
        //+2 to account for indent, cursor moves up from indentto cell equivalent to indent
        assertEquals(manager.getTextView().getCaretOffset(), manager.getMapList().getCurrent().getStart(manager.getMapList()) + 2);
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    public void basicArrowUpTestBraille() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "pagenum";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateBrailleView(bbTest.brailleViewBot.widget.getOffsetAtLine(5));
        bbTest.brailleViewTools.pressKey(SWT.ARROW_UP, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof PageIndicatorTextMapElement);
        assertEquals(manager.getBrailleView().getCaretOffset(), manager.getMapList().getCurrent().brailleList.getFirst().getStart(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    //tests pressing an arrow left into a pagenum element and correct update in xml tree
    public void basicArrowLeftTest() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "pagenum";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateTextView(bbTest.textViewBot.widget.getOffsetAtLine(5));
        bbTest.textViewTools.pressKey(SWT.ARROW_LEFT, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof PageIndicatorTextMapElement);
        assertEquals(manager.getTextView().getCaretOffset(), manager.getMapList().getCurrent().getEnd(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    public void basicArrowLeftTestBraille() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //String expectedTreeItem = "pagenum";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateBrailleView(bbTest.brailleViewBot.widget.getOffsetAtLine(5));
        bbTest.brailleViewTools.pressKey(SWT.ARROW_LEFT, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof PageIndicatorTextMapElement);
        assertEquals(manager.getBrailleView().getCaretOffset(), manager.getMapList().getCurrent().brailleList.getLast().getEnd(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    public void boxline_basicArrowDown_Test() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "sidebar";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateTextView(bbTest.textViewBot.widget.getOffsetAtLine(15));
        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof BoxLineTextMapElement);
        assertEquals(manager.getTextView().getCaretOffset(), manager.getMapList().getCurrent().getStart(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    public void boxline_basicArrowDown_Braille() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "sidebar";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateBrailleView(bbTest.brailleViewBot.widget.getOffsetAtLine(15));
        bbTest.brailleViewTools.pressKey(SWT.ARROW_DOWN, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof BoxLineTextMapElement);
        assertEquals(manager.getBrailleView().getCaretOffset(), manager.getMapList().getCurrent().brailleList.getFirst().getStart(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    //test pressing arrow right into a sidebar and correct update im xml tree
    public void boxline_basicArrowRightTest() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "sidebar";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateTextView(bbTest.textViewBot.widget.getOffsetAtLine(15));
        bbTest.textViewTools.pressKey(SWT.ARROW_RIGHT, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof BoxLineTextMapElement);
        assertEquals(manager.getTextView().getCaretOffset(), manager.getMapList().getCurrent().getStart(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    public void boxline_basicArrowRightBraille() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "sidebar";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateBrailleView(bbTest.brailleViewBot.widget.getOffsetAtLine(15));
        bbTest.brailleViewTools.pressKey(SWT.ARROW_RIGHT, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof BoxLineTextMapElement);
        assertEquals(manager.getBrailleView().getCaretOffset(), manager.getMapList().getCurrent().brailleList.getFirst().getStart(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    //test pressing arrow up into a sidebar and correct update im xml tree
    public void boxline_basicArrowUpTest() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "sidebar";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateTextView(bbTest.textViewBot.widget.getOffsetAtLine(17));
        bbTest.textViewTools.pressKey(SWT.ARROW_UP, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof BoxLineTextMapElement);
        //account for paragraph indent with +2
        assertEquals(manager.getTextView().getCaretOffset(), manager.getMapList().getCurrent().getStart(manager.getMapList()) + 2);
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    public void boxline_basicArrowUpBraille() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "sidebar";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateBrailleView(bbTest.brailleViewBot.widget.getOffsetAtLine(17));
        bbTest.brailleViewTools.pressKey(SWT.ARROW_UP, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof BoxLineTextMapElement);
        assertEquals(manager.getBrailleView().getCaretOffset(), manager.getMapList().getCurrent().brailleList.getFirst().getStart(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    //test pressing arrow left into a sidebar and correct update im xml tree
    public void boxline_basicArrowLeft() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "sidebar";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateTextView(bbTest.textViewBot.widget.getOffsetAtLine(17));
        bbTest.textViewTools.pressKey(SWT.ARROW_LEFT, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof BoxLineTextMapElement);
        assertEquals(manager.getTextView().getCaretOffset(), manager.getMapList().getCurrent().getEnd(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    public void boxline_basicArrowLeftBraille() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "sidebar";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateBrailleView(bbTest.brailleViewBot.widget.getOffsetAtLine(17));
        bbTest.brailleViewTools.pressKey(SWT.ARROW_LEFT, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof BoxLineTextMapElement);
        assertEquals(manager.getBrailleView().getCaretOffset(), manager.getMapList().getCurrent().brailleList.getLast().getEnd(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    public void insideBoxline_basicArrowDown() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "p";
        int offset = 5;

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateTextView(bbTest.textViewBot.widget.getOffsetAtLine(16) + offset);
        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof TextMapElement);
        //-2 to account for indent
        assertEquals(manager.getTextView().getCaretOffset(), manager.getMapList().getCurrent().getStart(manager.getMapList()) + offset - 2);
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    public void insideBoxline_basicArrowDownBraille() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "p";
        int offset = 5;

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateBrailleView(bbTest.brailleViewBot.widget.getOffsetAtLine(16) + offset);
        bbTest.brailleViewTools.pressKey(SWT.ARROW_DOWN, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof TextMapElement);
        //-2 to account for indent
        assertEquals(manager.getBrailleView().getCaretOffset(), manager.getMapList().getCurrent().brailleList.getFirst().getStart(manager.getMapList()) + offset - 2);
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    public void insideBoxline_basicArrowRight() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "p";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateTextView(bbTest.textViewBot.widget.getOffsetAtLine(17) - 1);
        bbTest.textViewTools.pressKey(SWT.ARROW_RIGHT, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof TextMapElement);
        assertEquals(manager.getTextView().getCaretOffset(), manager.getMapList().getCurrent().getStart(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    public void insideBoxline_basicArrowRightBraille() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "p";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateBrailleView(bbTest.brailleViewBot.widget.getOffsetAtLine(17) - 1);
        //+3 to account for spaces before body text
        bbTest.brailleViewTools.pressKey(SWT.ARROW_RIGHT, 3);

        assertTrue(manager.getMapList().getCurrent() instanceof TextMapElement);
        assertEquals(manager.getBrailleView().getCaretOffset(), manager.getMapList().getCurrent().brailleList.getFirst().getStart(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    public void bottomBoxline_basicArrowRight() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "sidebar";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateTextView(bbTest.textViewBot.widget.getOffsetAtLine(18) - 1);
        bbTest.textViewTools.pressKey(SWT.ARROW_RIGHT, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof BoxLineTextMapElement);
        assertEquals(manager.getTextView().getCaretOffset(), manager.getMapList().getCurrent().getStart(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    public void bottomBoxline_basicArrowRightBraille() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //String expectedTreeItem = "sidebar";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateBrailleView(bbTest.brailleViewBot.widget.getOffsetAtLine(18) - 1);
        bbTest.brailleViewTools.pressKey(SWT.ARROW_RIGHT, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof BoxLineTextMapElement);
        assertEquals(manager.getBrailleView().getCaretOffset(), manager.getMapList().getCurrent().brailleList.getFirst().getStart(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    public void bottomBoxline_basicArrowDown() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //	String expectedTreeItem = "sidebar";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateTextView(bbTest.textViewBot.widget.getOffsetAtLine(17));
        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof BoxLineTextMapElement);
        //+2 to account for body text indent
        assertEquals(manager.getTextView().getCaretOffset(), manager.getMapList().getCurrent().getStart(manager.getMapList()) + 2);
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }

    @Test(enabled = false)
    public void bottomBoxline_basicArrowBraille() {
        BBTestRunner bbTest = new BBTestRunner(TEST_FILE);
        //String expectedTreeItem = "sidebar";

        Manager manager = bbTest.manager;
        //	treeBot = bbTest.bot.tree(0);
        //	bbTest.selectTree(bbTest.bot, XMLTREE);

        bbTest.navigateBrailleView(bbTest.brailleViewBot.widget.getOffsetAtLine(17));
        bbTest.brailleViewTools.pressKey(SWT.ARROW_DOWN, 1);

        assertTrue(manager.getMapList().getCurrent() instanceof BoxLineTextMapElement);
        assertEquals(manager.getBrailleView().getCaretOffset(), manager.getMapList().getCurrent().brailleList.getFirst().getStart(manager.getMapList()));
        //	assertEquals(expectedTreeItem, getTreeItem(treeBot));
    }
	
	/*
	private String getTreeItem(SWTBotTree tBot){
		return tBot.selection().get(0, 0).toString();
	}
	
	
	private void pressKey(SWTBotStyledText bot, int key, int times){
		for(int i = 0; i < times; i++)
			bot.pressShortcut(KeyStroke.getInstance(key));
	}
	*/
}
