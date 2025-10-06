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

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Text;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.bbx.utd.BBXDynamicOptionStyleMap;
import org.brailleblaster.perspectives.braille.Manager;
import org.brailleblaster.perspectives.braille.views.wp.tableEditor.TableEditor;
import org.brailleblaster.perspectives.braille.views.wp.TextRenderer;
import org.brailleblaster.perspectives.mvc.menu.EmphasisItem;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.perspectives.mvc.modules.misc.ContextMenuModule;
import org.brailleblaster.perspectives.mvc.modules.misc.TableSelectionModule;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.testrunners.ViewTestRunner;
import org.brailleblaster.testrunners.XMLElementAssert;
import org.brailleblaster.utd.internal.xml.FastXPath;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.brailleblaster.utd.properties.UTDElements;
import org.brailleblaster.utils.NamespacesKt;
import org.brailleblaster.utils.gui.PickerDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotSpinner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.testng.annotations.Test;

import java.io.File;
import java.util.stream.StreamSupport;

import static org.brailleblaster.TestUtils.getFirstSectionChild;
import static org.brailleblaster.TestUtils.getInnerSection;
import static org.brailleblaster.testrunners.ViewTestRunner.doPendingSWTWork;
import static org.testng.Assert.*;


@Test(enabled = false)
public class TableTest {
    private static final String TEST_TEXT = "Test";
    private static final String TEST_HEADING = "Heading";
    private static final String TEST_COLUMN = "Column";
    private final int DEFAULT_ROWS = 3;
    private final int DEFAULT_COLS = 3;
    private final Document basicBlock = TestXMLUtils.generateBookDoc("", "<p>" + TEST_TEXT + "</p>");
    private final Document basicSimpleTable = TestXMLUtils.generateBookDoc("", constructTable(DEFAULT_COLS, DEFAULT_ROWS));
    private final Document simpleTableBetweenBlocks = TestXMLUtils.generateBookDoc("", "<p>" + TEST_TEXT
            + "</p>" + constructTable(DEFAULT_COLS, DEFAULT_ROWS) + "<p>" + TEST_TEXT + "</p>");

    /* TEST OPEN DIALOG */
    @Test(enabled = false)
    public void openDialogAndCancel() {
        BBTestRunner test = new BBTestRunner(basicBlock);

        SWTBotShell dialog = openInsertDialog(test);
        clickCancel(dialog);

        assertNotSame(test.bot.activeShell(), dialog);
    }

    @Test(enabled = false)
    public void editTableThroughMenu() {
        BBTestRunner test = new BBTestRunner(basicSimpleTable);

        test.textViewTools.navigateToText(TEST_COLUMN);

        SWTBotShell dialog = openEditDialog(test);

        verifyDialog(dialog);
    }

    @Test(enabled = false)
    public void editTableThroughContextMenu() {
        BBTestRunner test = new BBTestRunner(basicSimpleTable);

        test.textViewTools.navigateToText(TEST_COLUMN);
        test.textViewBot.contextMenu(ContextMenuModule.Items.EDIT_TABLE.getLabel()).click();
        doPendingSWTWork();
        verifyDialog(test.bot.activeShell());
    }

    @Test(enabled = false)
    public void cancelInsertTableDoesNotAffectXML() {
        BBTestRunner test = new BBTestRunner(basicBlock);

        SWTBotShell dialog = openInsertDialog(test);
        clickCancel(dialog);

        getFirstSectionChild(test).nextChildIsText(TEST_TEXT).noNextChild();
    }

    /* TEST TEXT VIEW EDIT BLOCKING*/

    @Test(enabled = false)
    public void cannotEditInTextView() {
        BBTestRunner test = new BBTestRunner(basicSimpleTable);

        test.textViewTools.navigateToText(TEST_COLUMN);
        test.textViewTools.pressKey(Keystrokes.SPACE.getNaturalKey(), 1);

        verifyBlockedEditMessage(test);
    }

    @Test(enabled = false)
    public void cannotEmphasizeTable() {
        BBTestRunner test = new BBTestRunner(basicSimpleTable);

        test.textViewTools.navigateToText(TEST_COLUMN);
        test.textViewTools.selectRight(TEST_COLUMN.length());
        test.textViewTools.pressShortcut(SWT.CTRL, 'b');

        verifyBlockedEditMessage(test);
    }

    @Test(enabled = false)
    public void rt6249_tnSymbolsInTable() {
        BBTestRunner test = new BBTestRunner(basicSimpleTable);

        test.textViewTools.navigateToText(TEST_COLUMN);
        test.textViewTools.selectRight(TEST_COLUMN.length());
        test.openMenuItem(TopMenu.EMPHASIS, EmphasisItem.TNSYMBOLS.longName);

        verifyBlockedEditMessage(test);
    }

    @Test(enabled = false)
    public void cannotEditTableWithinSelection() {
        BBTestRunner test = new BBTestRunner(simpleTableBetweenBlocks);

        for (int i = 0; i <= 4 + DEFAULT_ROWS; i++) {
            test.textViewBot.pressShortcut(Keystrokes.SHIFT, Keystrokes.DOWN);
            doPendingSWTWork();
        }
        test.textViewTools.pressKey(Keystrokes.SPACE.getNaturalKey(), 1);

        verifyBlockedEditMessage(test);
    }

    @Test(enabled = false)
    public void cannotInsertTableInsideTable() {
        BBTestRunner test = new BBTestRunner(basicSimpleTable);

        test.textViewTools.navigateToText(TEST_COLUMN);

        openInsertDialog(test);

        assertEquals(test.bot.activeShell().getText(), TableEditor.ERROR_DIALOG_TITLE);
    }

    @Test(enabled = false)
    public void cannotChangeStyle() {
        BBTestRunner test = new BBTestRunner(basicSimpleTable);

        test.textViewTools.navigateToText(TEST_COLUMN);
        test.clickStyleMenuItem("Basic", "Body Text");

        verifyBlockedEditMessage(test);
    }

    /* TEST STYLE VIEW EDIT BLOCKING */
    @Test(enabled = false)
    public void cannotEditSelectedTableBreadcrumbs() {
        BBTestRunner test = new BBTestRunner(simpleTableBetweenBlocks);

        test.textViewTools.pressKey(Keystrokes.DOWN.getNaturalKey(), 2);
        test.selectBreadcrumbsAncestor(0, BBX.CONTAINER.TABLE::assertIsA);
        test.textViewTools.pressKey(Keystrokes.DELETE.getNaturalKey(), 1);

        verifyBlockedEditMessage(test);
    }

    @Test(enabled = false)
    public void cannotChangeStyleInBreadcrumbs() {
        BBTestRunner test = new BBTestRunner(basicSimpleTable);

        test.textViewTools.navigateToText(TEST_COLUMN);
        test.selectBreadcrumbsAncestor(0, BBX.CONTAINER.TABLE::assertIsA);

        test.clickStyleMenuItem("Basic", "Body Text");

        verifyBlockedEditMessage(test);
    }

    @Test(enabled = false)
    public void tableEditBlocked() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>This here</p><table>"
                + "<tr><td>First TD</td><td>cell 2</td></tr>"
                + "<tr><td>This is a test</td><td>cell 2</td></tr></table>");

        String xmlBefore = bbTest.getDoc().toXML();
        String textBefore = bbTest.textViewBot.getText();

        bbTest.textViewTools.navigateToText("irst TD");
        bbTest.textViewTools.pressShortcut(Keystrokes.DELETE);
        bbTest.textViewTools.pressShortcut(Keystrokes.BS);

        assertEquals(bbTest.getDoc().toXML(), xmlBefore);
        assertEquals(bbTest.textViewBot.getText(), textBefore);
    }

    /* INSERT TABLE TESTS */

    @Test(enabled = false)
    public void insertSimpleTable() {
        BBTestRunner test = new BBTestRunner(basicBlock);

        test.textViewTools.navigate(0);
        SWTBotShell dialog = openInsertDialog(test);
        insertTextIntoDialog(dialog);
        clickSave(dialog);

        verifyTable(DEFAULT_ROWS, DEFAULT_COLS, getInnerSection(test));
    }

    /* TABLE EDITING */

    @Test(enabled = false)
    public void changeSimpleTable() {
        BBTestRunner test = new BBTestRunner(basicSimpleTable);

        test.textViewTools.navigateToText(TEST_COLUMN);
        SWTBotShell dialog = openEditDialog(test);

        dialog.bot().comboBox("Simple").setSelection("Listed");
        doPendingSWTWork();

        dialog.bot().button(TableEditor.SAVE_BUTTON).click();
        doPendingSWTWork();

        verifyTable(DEFAULT_ROWS, DEFAULT_COLS, getInnerSection(test));
        getInnerSection(test).child(0).hasAttribute("format", "listed");
    }

    @Test(enabled = false)
    public void nonbreakingSpace_issue5744() {
        BBTestRunner test = new BBTestRunner(basicSimpleTable);

        test.textViewTools.navigateToText(TEST_COLUMN);
        SWTBotShell dialog = openEditDialog(test);

        BBTestRunner.ViewTools dialogTools = new BBTestRunner.ViewTools(dialog.bot().styledText(0));
        dialogTools.navigate(4);
        dialogTools.pressShortcut(SWT.MOD1, ' ');
        dialogTools = new BBTestRunner.ViewTools(dialog.bot().styledText(DEFAULT_COLS));
        dialogTools.navigate(4);
        dialogTools.pressShortcut(SWT.MOD1, ' ');

        dialog.bot().button(TableEditor.SAVE_BUTTON).click();
        doPendingSWTWork();

        Element[] rows = StreamSupport.stream(FastXPath.descendant(test.getDoc()).spliterator(), false)
                .filter(BBX.BLOCK.TABLE_CELL::isA)
                .map(node -> (Element) node)
                .toArray(Element[]::new);
        test.assertElement(rows[0]).onlyChildIsText("Head" + TextRenderer.NON_BREAKING_SPACE + "ing");
        test.assertElement(rows[3]).onlyChildIsText("Colu" + TextRenderer.NON_BREAKING_SPACE + "mn");
    }

    /* DESTROY TABLE */

    @Test(enabled = false)
    public void destroyTableToList() {
        BBTestRunner test = new BBTestRunner(basicBlock);

        test.textViewTools.navigate(0);
        SWTBotShell dialog = openInsertDialog(test);
        insertTextIntoDialog(dialog);
        clickSave(dialog);

        test.textViewTools.navigateToText(TEST_HEADING);
        test.textViewBot.contextMenu(ContextMenuModule.Items.EDIT_TABLE.getLabel()).click();

        SWTBot tableEditorBot = test.bot.activeShell().bot();
        tableEditorBot.button(TableEditor.REFORMAT_BUTTON).click();
        doPendingSWTWork();

        handlePicker(test, "L3-5");

        XMLElementAssert asserter = getInnerSection(test)
                .childCount(2)
                .child(0)
                .isContainerListType(BBX.ListType.NORMAL);
        for (int i = 0; i < DEFAULT_ROWS * DEFAULT_COLS; i++) {
            boolean heading = i < DEFAULT_COLS;
            asserter.nextChildIs(c -> c.isBlockWithStyle("L3-5").nextChildIsText(heading ? TEST_HEADING : TEST_COLUMN));
        }
    }

    @Test(enabled = false)
    public void destroyHeading() {
        BBTestRunner test = new BBTestRunner(basicSimpleTable);

        test.textViewTools.navigateToText(TEST_COLUMN);
        SWTBotShell dialog = openEditDialog(test);

        dialog.bot().button("X").click();
        doPendingSWTWork();

        SWTBotShell placementDialog = test.bot.activeShell();
        placementDialog.bot().button(TableEditor.OK_BUTTON).click();
        doPendingSWTWork();

        handlePicker(test, "Body Text");

        Document doc = test.getDoc();
        Element rootSection = (Element) doc.getRootElement().getChild(1);

        assertTrue(BBX.BLOCK.isA(rootSection.getChild(0)), rootSection.getChild(0).toXML() + " not a block");
        assertEquals(rootSection.getChild(0).getChild(0).getValue(), TEST_HEADING);
        //RT 4837: Make sure the heading got translated
        assertTrue(UTDElements.BRL.isA(rootSection.getChild(0).getChild(1)));
        assertEquals(rootSection.getChild(1).getChild(0).getValue(), TEST_HEADING);
        assertTrue(UTDElements.BRL.isA(rootSection.getChild(1).getChild(1)));
        assertEquals(rootSection.getChild(2).getChild(0).getValue(), TEST_HEADING);
        assertTrue(UTDElements.BRL.isA(rootSection.getChild(2).getChild(1)));
        assertTrue(BBX.CONTAINER.TABLE.isA(rootSection.getChild(3)));
    }

    @Test(enabled = false)
    public void reformatTable() {
        BBTestRunner test = new BBTestRunner(basicSimpleTable);

        test.textViewTools.navigateToText(TEST_COLUMN);
        SWTBotShell dialog = openEditDialog(test);

        dialog.bot().button(TableEditor.REFORMAT_BUTTON).click();
        doPendingSWTWork();
        ViewTestRunner.forceActiveShellHack();
        doPendingSWTWork();
        handlePicker(test, "Body Text");

        XMLElementAssert asserter = getInnerSection(test);
        for (int i = 0; i < DEFAULT_ROWS * DEFAULT_COLS; i++) {
            boolean heading = i < DEFAULT_COLS;
            asserter.nextChildIs(c -> c.isBlock(BBX.BLOCK.STYLE).nextChildIsText(heading ? TEST_HEADING : TEST_COLUMN));
        }
    }

    @Test(enabled = false)
    public void table_actualTableTest() {
        Document doc = new BBTestRunner("", "<table testid='table'>"
                + "<tr><td testid='first'>Testing 1</td><td>testing 2</td></tr>"
                + "<tr><td testid='second'>Testing 1</td><td>testing 2</td></tr>"
                + "</table>").getDoc();

        //Can't use getTestIdElement since table formatter copies the table
        Text text = (Text) TestXMLUtils.getTestIdElements(doc, "first").get(0).getChild(0);
        assertEquals(
                Manager.getTableParent(text),
                TestXMLUtils.getTestIdElements(doc, "table").get(0),
                XMLHandler.toXMLPrettyPrint(doc)
        );

        text = (Text) TestXMLUtils.getTestIdElements(doc, "first").get(1).getChild(0);
        assertEquals(
                Manager.getTableParent(text),
                TestXMLUtils.getTestIdElements(doc, "table").get(0),
                XMLHandler.toXMLPrettyPrint(doc)
        );
    }

    @Test(enabled = false)
    public void rt6131_tableTestWithEmptyCell() {
        BBTestRunner bbTest = new BBTestRunner("", "<table><tr><td>Head 1</td><td>Head 2</td></tr>"
                + "<tr><td>Cell 1</td><td>Cell 2</td></tr></table>");

        bbTest.textViewTools.navigateToText("Head 1");
        SWTBotShell dialog = openEditDialog(bbTest);

        SWTBotStyledText stBot = dialog.bot().styledText(2);
        assertEquals(stBot.getText(), "Cell 1");

        stBot.selectLine(0);
        ViewTestRunner.doPendingSWTWork();
        stBot.pressShortcut(Keystrokes.BS);
        ViewTestRunner.doPendingSWTWork();

        clickSave(dialog);
        ViewTestRunner.doPendingSWTWork();

        getInnerSection(bbTest)
                .nextChildIs(tableParent -> tableParent.isContainer(BBX.CONTAINER.TABLE)
                        .nextChildIs(row1 -> row1.isContainerTableRowType(BBX.TableRowType.NORMAL)
                                .nextChildIs(row1cell1 -> row1cell1.hasText("Head 1"))
                                .nextChildIs(row1cell2 -> row1cell2.hasText("Head 2")))
                        .nextChildIs(row2 -> row2.isContainerTableRowType(BBX.TableRowType.NORMAL)
                                .nextChildIs(row2cell1 -> row2cell1.childCount(0))
                                .nextChildIs(row2cell2 -> row2cell2.hasText("Cell 2"))));
    }

    @Test(enabled = false)
    public void rt5342_pageBreakOnLineBreakPrecedingTable() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>Test</p><table><tr><td>Head 1</td><td>Head 2</td></tr>"
                + "<tr><td>Cell 1</td><td>Cell 2</td></tr></table>");

        bbTest.textViewTools.navigateToLine(1);
        bbTest.textViewTools.pressShortcut(Keystrokes.CR);

        bbTest.textViewTools.navigateToLine(2);
        bbTest.textViewTools.pressShortcut(Keystrokes.CTRL, Keystrokes.CR);

        getInnerSection(bbTest)
                .nextChildIs(child -> child.isBlockDefaultStyle("Body Text"))
                .nextChildIs(child -> assertTrue(UTDElements.NEW_LINE.isA(child.element())))
                .nextChildIs(child -> assertTrue(UTDElements.NEW_LINE.isA(child.element())))
                .nextChildIs(child -> assertTrue(UTDElements.NEW_LINE.isA(child.element())))
                .nextChildIs(tableParent -> assertEquals(tableParent.isContainer(BBX.CONTAINER.TABLE).element()
                        .getAttributeValue(BBXDynamicOptionStyleMap.OPTION_ATTRIB_PREFIX + "newPagesBefore", NamespacesKt.BB_NS), "1"));
    }

    /* TEST CONVERT TEXT TO TABLE */
    @Test(enabled = false)
    public void convertTextToTable_basicTest() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>Head 1</p><p>Head 2</p><p>Cell 1</p><p>Cell 2</p>");

        bbTest.textViewTools.navigateToLine(0);
        bbTest.textViewTools.selectToEndOf("Cell 2");
        SWTBotShell dialog = convertToColumns(bbTest, 2);

        clickSave(dialog);

        getInnerSection(bbTest)
                .nextChildIs(tableParent -> tableParent.isContainer(BBX.CONTAINER.TABLE)
                        .nextChildIs(row1 -> row1.isContainer(BBX.CONTAINER.TABLE_ROW)
                                .nextChildIs(cell1 -> cell1.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Head 1"))
                                .nextChildIs(cell2 -> cell2.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Head 2")))
                        .nextChildIs(row2 -> row2.isContainer(BBX.CONTAINER.TABLE_ROW)
                                .nextChildIs(cell1 -> cell1.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Cell 1"))
                                .nextChildIs(cell2 -> cell2.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Cell 2"))))
                .nextChildIs(tableCopy -> tableCopy.isContainer(BBX.CONTAINER.TABLE))
                .noNextChild();
    }

    @Test(enabled = false)
    public void convertTextToTable_threeCols() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>Head 1</p><p>Head 2</p><p>Head 3</p><p>Cell 1</p><p>Cell 2</p><p>Cell 3</p>");

        bbTest.textViewTools.navigateToLine(0);
        bbTest.textViewTools.selectToEndOf("Cell 3");
        SWTBotShell dialog = convertToColumns(bbTest, 3);

        clickSave(dialog);

        getInnerSection(bbTest)
                .nextChildIs(tableParent -> tableParent.isContainer(BBX.CONTAINER.TABLE)
                        .nextChildIs(row1 -> row1.isContainer(BBX.CONTAINER.TABLE_ROW)
                                .nextChildIs(cell1 -> cell1.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Head 1"))
                                .nextChildIs(cell2 -> cell2.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Head 2"))
                                .nextChildIs(cell3 -> cell3.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Head 3")))
                        .nextChildIs(row2 -> row2.isContainer(BBX.CONTAINER.TABLE_ROW)
                                .nextChildIs(cell1 -> cell1.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Cell 1"))
                                .nextChildIs(cell2 -> cell2.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Cell 2"))
                                .nextChildIs(cell3 -> cell3.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Cell 3"))))
                .nextChildIs(tableCopy -> tableCopy.isContainer(BBX.CONTAINER.TABLE))
                .noNextChild();
    }

    @Test(enabled = false)
    public void convertTextToTable_box() {
        BBTestRunner bbTest = new BBTestRunner(new File("src/test/resources/org/brailleblaster/printView/BoxLineSelectionTests.bbx"));

        bbTest.textViewTools.navigateToLine(0);
        bbTest.textViewTools.selectToEndOf("Paragraph 4");
        SWTBotShell dialog = convertToColumns(bbTest, 2);

        clickSave(dialog);

        getInnerSection(bbTest)
                .nextChildIs(tableParent -> tableParent.isContainer(BBX.CONTAINER.TABLE)
                        .nextChildIs(row1 -> row1.isContainer(BBX.CONTAINER.TABLE_ROW)
                                .nextChildIs(cell1 -> cell1.isBlock(BBX.BLOCK.TABLE_CELL))
                                .nextChildIs(cell2 -> cell2.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Paragraph 2")))
                        .nextChildIs(row2 -> row2.isContainer(BBX.CONTAINER.TABLE_ROW)
                                .nextChildIs(cell1 -> cell1.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Paragraph 3"))
                                .nextChildIs(cell2 -> cell2.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Paragraph 4"))))
                .nextChildIs(tableCopy -> tableCopy.isContainer(BBX.CONTAINER.TABLE))
                .nextChildIs(child -> child.isContainer(BBX.CONTAINER.BOX))
                .nextChildIs(child -> child.isBlock(BBX.BLOCK.DEFAULT).hasText("Paragraph 6"))
                .noNextChild();
    }

    @Test(enabled = false)
    public void convertTextToTable_manyColumns() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>Head 1</p><p>Head 2</p><p>Head 3</p><p>Head 4</p><p>Cell 1</p>");

        bbTest.textViewTools.navigateToLine(0);
        bbTest.textViewTools.selectToEndOf("Cell 1");
        SWTBotShell dialog = convertToColumns(bbTest, 4);

        clickSave(dialog);

        getInnerSection(bbTest)
                .nextChildIs(tableParent -> tableParent.isContainer(BBX.CONTAINER.TABLE)
                        .nextChildIs(row1 -> row1.isContainer(BBX.CONTAINER.TABLE_ROW)
                                .nextChildIs(cell1 -> cell1.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Head 1"))
                                .nextChildIs(cell2 -> cell2.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Head 2"))
                                .nextChildIs(cell3 -> cell3.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Head 3"))
                                .nextChildIs(cell4 -> cell4.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Head 4")))
                        .nextChildIs(row2 -> row2.isContainer(BBX.CONTAINER.TABLE_ROW)
                                .nextChildIs(cell1 -> cell1.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .hasText("Cell 1"))
                                .nextChildIs(cell2 -> cell2.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .childCount(0))
                                .nextChildIs(cell3 -> cell3.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .childCount(0))
                                .nextChildIs(cell4 -> cell4.isBlock(BBX.BLOCK.TABLE_CELL)
                                        .childCount(0))))
                .nextChildIs(tableCopy -> tableCopy.isContainer(BBX.CONTAINER.TABLE));
    }

    /* UTILS */

    private SWTBotShell openInsertDialog(BBTestRunner test) {
        test.openMenuItem(TopMenu.INSERT, TableEditor.INSERT_MENUITEM);
        return test.bot.activeShell();
    }

    private SWTBotShell openEditDialog(BBTestRunner test) {
        test.openMenuItem(TopMenu.TOOLS, TableEditor.EDIT_MENUITEM);
        return test.bot.activeShell();
    }

    private SWTBotShell openConvertDialog(BBTestRunner test) {
        test.openMenuItem(TopMenu.TOOLS, TableEditor.CONVERT_MENU_ITEM);
        return test.bot.activeShell();
    }

    private SWTBotShell convertToColumns(BBTestRunner test, int columns) {
        SWTBotShell curShell = openConvertDialog(test);
        SWTBotSpinner spinner = curShell.bot().spinner();
        spinner.setSelection(columns);
        doPendingSWTWork();
        curShell.bot().button(TableEditor.OK_BUTTON).click();
        doPendingSWTWork();
        return test.bot.activeShell();
    }

    private void clickSave(SWTBotShell dialog) {
        clickButton(dialog, TableEditor.SAVE_BUTTON);
    }

    private void clickCancel(SWTBotShell dialog) {
        clickButton(dialog, TableEditor.CANCEL_BUTTON);
    }

    private void clickButton(SWTBotShell dialog, String button) {
        new SWTBotButton(dialog.bot().button(button).widget).click();
    }

    private void verifyBlockedEditMessage(BBTestRunner test) {
        assertEquals(test.bot.activeShell().widget.getText(), TableSelectionModule.TABLE_WARNING_DIALOG_TITLE);
    }

    private void verifyDialog(SWTBotShell dialog) {
        verifyDialog(dialog, DEFAULT_ROWS - 1, ((DEFAULT_COLS * DEFAULT_ROWS) - DEFAULT_ROWS) - 1);
    }

    private void verifyDialog(SWTBotShell dialog, int headings, int entries) {
        try {
            dialog.bot().styledText(TEST_HEADING, headings);
            dialog.bot().styledText(TEST_COLUMN, entries);
        } catch (IndexOutOfBoundsException e) {
            fail("Expected " + headings + " headings and " + entries + " entries");
        }
    }

    private void insertTextIntoDialog(SWTBotShell dialog) {
        for (int i = 0; i < 9; i++) {
            SWTBotStyledText text = dialog.bot().styledText(i);
            text.typeText(i < 3 ? TEST_HEADING : TEST_COLUMN);
            doPendingSWTWork();
        }
    }

    private void handlePicker(BBTestRunner test, String styleName) {
        SWTBotShell stylePicker = test.bot.activeShell();
        assertEquals(stylePicker.widget.getText(), PickerDialog.SHELL_TITLE);
        stylePicker.bot().text(0).widget.forceFocus();
        doPendingSWTWork();
        stylePicker.bot().text(0).typeText(styleName + System.lineSeparator());
        doPendingSWTWork();
    }

    private void verifyTable(int rows, int cols, XMLElementAssert element) {
        element.nextChildIs(c1 -> {
            c1.isContainer(BBX.CONTAINER.TABLE);
            for (int row = 0; row < rows; row++) {
                boolean heading = row == 0;
                c1.childIs(row, c2 -> {
                    c2.isContainer(BBX.CONTAINER.TABLE_ROW);
                    for (int col = 0; col < cols; col++) {
                        c2.nextChildIs(c3 -> c3.isBlock(BBX.BLOCK.TABLE_CELL).nextChildIsText(heading ? TEST_HEADING : TEST_COLUMN));
                    }
                });
            }
        });
    }

    private static String constructTable(int columns, int rows) {
        StringBuilder sb = new StringBuilder();

        sb.append("<table>");
        for (int row = 0; row < rows; row++) {
            sb.append("<tr>");
            sb.append(("<td>" + (row == 0 ? TEST_HEADING : TEST_COLUMN) + "</td>").repeat(Math.max(0, columns)));
            sb.append("</tr>");
        }
        sb.append("</table>");

        return sb.toString();
    }
}
