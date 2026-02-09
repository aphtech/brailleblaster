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
package org.brailleblaster.perspectives.braille.views.wp.tableEditor;

import kotlin.Pair;
import net.miginfocom.swt.MigLayout;
import nu.xom.*;
import org.apache.commons.lang3.StringUtils;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.bbx.BBX.TableRowType;
import org.brailleblaster.bbx.BBXUtils;
import org.brailleblaster.exceptions.EditingException;
import org.brailleblaster.perspectives.braille.mapping.maps.MapList;
import org.brailleblaster.utd.exceptions.NodeException;
import org.brailleblaster.util.Notify;
import org.brailleblaster.utils.gui.PickerDialog;
import org.brailleblaster.utils.localization.LocaleHandler;
import org.brailleblaster.math.mathml.MathModuleUtils;
import org.brailleblaster.perspectives.braille.Manager;
import org.brailleblaster.perspectives.braille.document.BrailleDocument;
import org.brailleblaster.perspectives.braille.mapping.interfaces.Uneditable;
import org.brailleblaster.perspectives.braille.messages.Sender;
import org.brailleblaster.perspectives.braille.ui.BBStyleableText;
import org.brailleblaster.perspectives.braille.ui.BBStyleableText.EmphasisTags;
import org.brailleblaster.perspectives.braille.ui.BBStyleableText.MathTags;
import org.brailleblaster.perspectives.mvc.BBSimpleManager;
import org.brailleblaster.perspectives.mvc.XMLNodeCaret;
import org.brailleblaster.perspectives.mvc.XMLTextCaret;
import org.brailleblaster.settings.UTDManager;
import org.brailleblaster.utd.IStyle;
import org.brailleblaster.utd.Style;
import org.brailleblaster.utd.UTDTranslationEngine;
import org.brailleblaster.utd.internal.xml.FastXPath;
import org.brailleblaster.utd.properties.UTDElements;
import org.brailleblaster.utd.utils.TableUtils;
import org.brailleblaster.utd.utils.UTDHelper;
import org.brailleblaster.utils.swt.AccessibilityUtils;
import org.brailleblaster.utils.swt.ButtonBuilder1;
import org.brailleblaster.utils.swt.EasyListeners;
import org.brailleblaster.utils.swt.EasySWT;
import org.brailleblaster.utils.swt.MenuBuilder;
import org.brailleblaster.utils.swt.SubMenuBuilder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.*;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class TableEditor extends Dialog {
    private static final LocaleHandler localeHandler = LocaleHandler.getDefault();
    private static final LocaleHandler banaStyles = LocaleHandler.getBanaStyles();
    //Public for use by tests
    public static final String OK_BUTTON = "Ok";
    public static final String SAVE_BUTTON = "Save";
    public static final String CANCEL_BUTTON = "Cancel";
    public static final String REFORMAT_BUTTON = "Reformat Table";
    public static final String ADVANCED_REFORMAT_BUTTON = "Advanced Reformat";
    public static final String CHANGE_DIMENSIONS_BUTTON = "Change Dimensions";

    public static final String INSERT_MENUITEM = "Table";
    public static final String EDIT_MENUITEM = "Table Editor";
    public static final String CONVERT_MENU_ITEM = "Convert Text to Table";

    public static final String ERROR_DIALOG_TITLE = "Error";
    private static final String ERROR_DIALOG_INSERT = "Table cannot be inserted here";
    private static final String ERROR_DIALOG_EDIT = "Cursor is not on table";
    private static final String ERROR_CONVERT_TEXT_UNSELECTED = "At least two items need to be selected to convert to a table";
    private static final String ERROR_CONVERT_TEXT_ALREADY_IN_TABLE = "Selection already contains table";

    private static final String LINEAR_TN_HEADING = "Columns follow one another in this order:";
    private static final String STAIRSTEP_TN_HEADING = "Table changed as follows:";
    private static final String LISTED_TN_HEADING = "Print format is changed."
            + " Row headings are blocked in cell 5; column headings begin in cell 1."
            + " All headings are repeated for clarity."
            + " A colon separates headings from table entries.";

    // Names used by accessibility devices
    private static final String ACCESSIBLE_DELETE_ROW_NAME = "Delete Row";
    public static final String ACCESSIBLE_COLUMN_NAME = "Column";
    public static final String ACCESSIBLE_ROW_NAME = "Row";
    private static final String ACCESSIBLE_CAPTION_NAME = "Caption";
    private static final String ACCESSIBLE_DELETE_CAPTION_NAME = "Delete Caption";
    public static final String ACCESSIBLE_TN_HEADING_NAME = "TN Heading";

    private UTDManager m;
    private Shell shell;
    private Element tableNode;
    private Combo tableTypeCombo;
    private Text rowText, colText;
    private Consumer<Node[]> callback;
    private @NonNull final List<TableUtils.SimpleTableOptions> options = new ArrayList<>();
    private int[] widths;
    private ITable state;
    static final int MAX_CELLS = 300; //Prevents user from crashing SWT
    private static boolean debug = false;
    private Mode mode;

    private final TableType[] tableTypes = new TableType[]{
            TableType.AUTO,
            TableType.SIMPLE,
//			TableType.SIMPLE_FACING, 
            TableType.LISTED,
            TableType.STAIRSTEP,
            TableType.LINEAR
    };

    private class SimpleTableOptionDialog {
        private List<Text> widthTexts;

        public void open(Shell parent, Element table, int columns, List<TableUtils.SimpleTableOptions> options, UTDManager m) {
            int pageWidth =
                    m.getEngine().getBrailleSettings().getCellType().getCellsForWidth(
                            BigDecimal.valueOf(m.getEngine().getPageSettings().getDrawableWidth()));

            Shell dialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
            dialog.setText("Simple Table Options");
            dialog.setLayout(new GridLayout(1, false));
            Composite container = EasySWT.makeComposite(dialog, 2);
            EasySWT.makeLabel(container, "Cells between columns:", 1);
            Combo cellsBwColCombo = new Combo(container, SWT.READ_ONLY);
            cellsBwColCombo.setItems("1", "2");
            cellsBwColCombo.select(options.contains(TableUtils.SimpleTableOptions.ONE_CELL_BETWEEN_COLUMNS) || TableUtils.hasSimpleTableOption(TableUtils.SimpleTableOptions.ONE_CELL_BETWEEN_COLUMNS, table) ? 0 : 1);

            EasySWT.makeLabel(container, "Guide dots:", 1);
            Combo guideDotsCombo = new Combo(container, SWT.READ_ONLY);
            guideDotsCombo.setItems("Enabled", "Disabled");
            guideDotsCombo.select(options.contains(TableUtils.SimpleTableOptions.GUIDE_DOTS_DISABLED) || TableUtils.hasSimpleTableOption(TableUtils.SimpleTableOptions.GUIDE_DOTS_DISABLED, table) ? 1 : 0);

            EasySWT.makeLabel(container, "Row headings:", 1);
            Combo rowHeadCombo = new Combo(container, SWT.READ_ONLY);
            rowHeadCombo.setItems("Enabled", "Disabled");
            rowHeadCombo.select(options.contains(TableUtils.SimpleTableOptions.ROW_HEADING_DISABLED) || TableUtils.hasSimpleTableOption(TableUtils.SimpleTableOptions.ROW_HEADING_DISABLED, table) ? 1 : 0);
            EasySWT.makeLabel(container, "Column headings:", 1);
            Combo colHeadCombo = new Combo(container, SWT.READ_ONLY);
            colHeadCombo.setItems("Enabled", "Disabled");
            colHeadCombo.select(options.contains(TableUtils.SimpleTableOptions.COLUMN_HEADING_DISABLED) || TableUtils.hasSimpleTableOption(TableUtils.SimpleTableOptions.COLUMN_HEADING_DISABLED, table) ? 1 : 0);

            EasySWT.makeLabel(container, "Column widths:", 1);
            Combo colWidthCombo = new Combo(container, SWT.READ_ONLY);
            colWidthCombo.setItems("Default", "Custom");
            colWidthCombo.select(options.contains(TableUtils.SimpleTableOptions.CUSTOM_WIDTHS) || TableUtils.hasSimpleTableOption(TableUtils.SimpleTableOptions.CUSTOM_WIDTHS, table) ? 1 : 0);

            Composite widthPanel = EasySWT.makeComposite(dialog, 1);
            if (colWidthCombo.getSelectionIndex() == 1)
                makeColWidthPanel(dialog, widthPanel, table, columns, pageWidth, cellsBwColCombo.getSelectionIndex() == 1);

            EasyListeners.selection(colWidthCombo, e -> {
                if (colWidthCombo.getSelectionIndex() == 1) {
                    if (widthPanel.getChildren().length == 0) {
                        makeColWidthPanel(dialog, widthPanel, table, columns, pageWidth, cellsBwColCombo.getSelectionIndex() == 1);
                    }
                } else
                    destroyColWidthPanel(dialog, widthPanel);
            });

            EasyListeners.selection(cellsBwColCombo, (e) -> {
                if (widthPanel.getChildren().length > 0) {
                    destroyColWidthPanel(dialog, widthPanel);
                    makeColWidthPanel(dialog, widthPanel, table, columns, pageWidth, cellsBwColCombo.getSelectionIndex() == 1);
                }
            });

            Composite buttonPanel = EasySWT.makeComposite(dialog, 2);
            EasySWT.makePushButton(buttonPanel, OK_BUTTON, 1, e -> {
                if (widthTexts != null) {
                    int totalWidth = 0;
                    for (Text text : widthTexts) {
                        if (text.getText().isEmpty()) {
                            EasySWT.makeEasyOkDialog("Error", "Width cannot be blank", dialog);
                            return;
                        }
                        int number = Integer.parseInt(text.getText());
                        if (number < 2) {
                            EasySWT.makeEasyOkDialog("Error", "Each column must be at least two cells width", dialog);
                            return;
                        }
                        if (number > pageWidth) {
                            EasySWT.makeEasyOkDialog("Error", "A column cannot be wider than the page", dialog);
                            return;
                        }
                        totalWidth += number;
                    }
                    if (totalWidth > pageWidth) {
                        EasySWT.makeEasyOkDialog("Error", "The total width of the columns cannot be wider than the page", dialog);
                        return;
                    }
                    widths = new int[widthTexts.size()];
                    for (int i = 0; i < widthTexts.size(); i++) {
                        widths[i] = Integer.parseInt(widthTexts.get(i).getText());
                    }
                } else {
                    widths = null;
                }
                if (cellsBwColCombo.getSelectionIndex() == 0) {
                    if (!options.contains(TableUtils.SimpleTableOptions.ONE_CELL_BETWEEN_COLUMNS))
                        options.add(TableUtils.SimpleTableOptions.ONE_CELL_BETWEEN_COLUMNS);
                } else options.remove(TableUtils.SimpleTableOptions.ONE_CELL_BETWEEN_COLUMNS);
                if (guideDotsCombo.getSelectionIndex() == 1) {
                    if (!options.contains(TableUtils.SimpleTableOptions.GUIDE_DOTS_DISABLED))
                        options.add(TableUtils.SimpleTableOptions.GUIDE_DOTS_DISABLED);
                } else options.remove(TableUtils.SimpleTableOptions.GUIDE_DOTS_DISABLED);
                if (rowHeadCombo.getSelectionIndex() == 1) {
                    if (!options.contains(TableUtils.SimpleTableOptions.ROW_HEADING_DISABLED))
                        options.add(TableUtils.SimpleTableOptions.ROW_HEADING_DISABLED);
                } else options.remove(TableUtils.SimpleTableOptions.ROW_HEADING_DISABLED);
                if (colHeadCombo.getSelectionIndex() == 1) {
                    if (!options.contains(TableUtils.SimpleTableOptions.COLUMN_HEADING_DISABLED))
                        options.add(TableUtils.SimpleTableOptions.COLUMN_HEADING_DISABLED);
                } else options.remove(TableUtils.SimpleTableOptions.COLUMN_HEADING_DISABLED);

                if (colWidthCombo.getSelectionIndex() == 1) {
                    if (!options.contains(TableUtils.SimpleTableOptions.CUSTOM_WIDTHS))
                        options.add(TableUtils.SimpleTableOptions.CUSTOM_WIDTHS);
                } else options.remove(TableUtils.SimpleTableOptions.CUSTOM_WIDTHS);

                dialog.close();
            });
            EasySWT.makePushButton(buttonPanel, CANCEL_BUTTON, 1, e -> dialog.close());

            dialog.pack();
            dialog.open();

            while (!dialog.isDisposed()) {
                Display.getCurrent().readAndDispatch();
            }
        }

        private void makeColWidthPanel(Shell dialog, Composite panel, Element table, int columns, int pageWidth, boolean twoCellsBwCols) {
            int truePageWidth = pageWidth - ((columns - 1) * (twoCellsBwCols ? 2 : 1));
            int defaultWidth = truePageWidth / columns;
            int remainder = truePageWidth % columns;
            final int[] colWidths;
            if (widths != null) {
                colWidths = widths;
            } else if (TableUtils.hasSimpleTableOption(TableUtils.SimpleTableOptions.CUSTOM_WIDTHS, table)) {
                colWidths = Objects.requireNonNull(TableUtils.getCustomSimpleTableWidths(table));
            } else {
                colWidths = new int[columns];
                for (int i = 0; i < colWidths.length; i++) {
                    colWidths[i] = defaultWidth + (remainder > 0 ? 1 : 0);
                    if (remainder > 0)
                        remainder--;
                }
            }
            widthTexts = new ArrayList<>();
            Label columnLabel = EasySWT.makeLabel(panel, "Column Widths:", 1);
            EasySWT.buildGridData().setAlign(SWT.CENTER, SWT.CENTER).applyTo(columnLabel);
            Composite textContainer = EasySWT.makeComposite(panel, (columns * 2) - 1);
            for (int i = 0; i < columns; i++) {
                Label newLabel = EasySWT.makeLabel(textContainer, String.valueOf(i + 1), 1);
                EasySWT.buildGridData().setAlign(SWT.CENTER, SWT.CENTER).applyTo(newLabel);
                if (i != columns - 1) {
                    EasySWT.makeLabel(textContainer, "", 1);
                }
            }
            for (int i = 0; i < columns; i++) {
                Text newText = EasySWT.makeText(textContainer, 1);
                EasySWT.buildGridData().setHint(EasySWT.getWidthOfText("999"), SWT.DEFAULT).applyTo(newText);
                EasyListeners.verifyNumbersOnly(newText);
                newText.setText(String.valueOf(colWidths[i]));
                newText.setTextLimit(3);
                widthTexts.add(newText);
                if (i != columns - 1) {
                    EasySWT.buildGridData().setAlign(SWT.CENTER, SWT.CENTER).applyTo(EasySWT.makeLabel(textContainer, twoCellsBwCols ? "(2)" : "(1)", 1));
                }
            }
            int totalWidth = (columns - 1) * (twoCellsBwCols ? 2 : 1);
            for (int width : colWidths) {
                totalWidth += width;
            }
            Label pageWidthLabel = EasySWT.makeLabel(textContainer, "Total Width: " + totalWidth, 1);
            widthTexts.forEach((wt) -> wt.addModifyListener(colTextModify(pageWidthLabel, pageWidth, truePageWidth)));
            EasySWT.buildGridData().setAlign(SWT.CENTER, SWT.CENTER).setColumns((columns * 2) - 1).applyTo(pageWidthLabel);
            panel.layout(true);
            dialog.pack();
        }

        private void destroyColWidthPanel(Shell dialog, Composite panel) {
            widthTexts = null;
            Control[] children = panel.getChildren();
            for (Control child : children) {
                child.dispose();
            }
            panel.layout(true);
            dialog.pack();
        }

        private ModifyListener colTextModify(Label pageWidthLabel, int pageWidth, int truePageWidth) {
            return e -> {
                Text text = (Text) e.widget;
                if (text.getText().isEmpty())
                    return;
                if (Integer.parseInt(text.getText()) < 2 || Integer.parseInt(text.getText()) > pageWidth) {
                    text.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
                } else {
                    text.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
                }
                int totalWidth = 0;
                for (Text colText : widthTexts) {
                    if (colText.getText().isEmpty())
                        return;
                    totalWidth += Integer.parseInt(colText.getText());
                }
                totalWidth += (pageWidth - truePageWidth);
                pageWidthLabel.setText("Total Width: " + totalWidth);
                if (totalWidth > pageWidth) {
                    pageWidthLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
                } else {
                    pageWidthLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
                }
                pageWidthLabel.pack(true);
            };
        }
    }

    public TableEditor(Shell parent) {
        super(parent, SWT.NONE);
    }

    public void initTable(Manager manager, boolean insert) {
        this.m = manager.getDocument().getSettingsManager();
        mode = insert ? Mode.INSERT : Mode.EDIT;
        Node selectedNode = manager.getSimpleManager().getCurrentCaret().getNode();
        if (BBX.CONTAINER.TABLETN.isA(selectedNode)) {
            ParentNode parent = selectedNode.getParent();
            int index = parent.indexOf(selectedNode);
            if (index != parent.getChildCount() - 1 && BBX.CONTAINER.TABLE.isA(parent.getChild(index + 1))) {
                selectedNode = parent.getChild(index + 1);
            } else {
                throw new IllegalStateException("TableTN container has no table");
            }
        }
        if (validateNode(selectedNode, manager, insert)) {
            if (insert) {
                tableNode = createEmptyTable(manager.getSimpleManager());
            } else {
                tableNode = Manager.getTableParent(selectedNode);
            }
            open((Node[] nodeList) -> {
                        if (manager.isEmptyDocument()) {
                            //Get rid of placeholder node
                            StreamSupport.stream(((Iterable<Node>)FastXPath.descendant(manager.getDoc().getRootElement())::iterator).spliterator(), false)
                                    .filter(rootDesc -> BBX.BLOCK.isA(rootDesc) && BrailleDocument.isEmptyPlaceholder((Element) rootDesc))
                                    .findFirst().ifPresent(Node::detach);
                        }
                        manager.getSimpleManager().dispatchEvent(new org.brailleblaster.perspectives.mvc.events.ModifyEvent(Sender.NO_SENDER, true, nodeList));
                    },
                    (n) -> {
                        if (insert) {
                            tableNode.detach();
                        }
                    });
        } else {
            if (insert) {
                EasySWT.makeEasyOkDialog(ERROR_DIALOG_TITLE, ERROR_DIALOG_INSERT, getParent());
            } else {
                EasySWT.makeEasyOkDialog(ERROR_DIALOG_TITLE, ERROR_DIALOG_EDIT, getParent());
            }
        }
    }

    private Element createEmptyTable(BBSimpleManager m) {
        XMLNodeCaret caret = m.getCurrentCaret();
        boolean before;
        if (caret instanceof XMLTextCaret text) {
            Node block = text.getNode();
            while (!BBX.BLOCK.isA(block)) {
                block = block.getParent();
            }
            //Calculate the halfway point of the text of the block
            //This is relevant if part of a block is emphasized, for example
            int actualOffset = text.getOffset();
            int blockTextLength = 0;
            boolean pastOriginalText = false;
            Iterator<Node> iter = StreamSupport.stream(((Iterable<Node>)FastXPath.descendant(block)::iterator).spliterator(), false)
                    .filter(e -> e instanceof nu.xom.Text && !UTDElements.BRL.isA(e.getParent()))
                    .iterator();
            while (iter.hasNext()) {
                Node next = iter.next();
                if (next == text.getNode())
                    pastOriginalText = true;
                if (!pastOriginalText)
                    actualOffset += next.getValue().length();
                blockTextLength += next.getValue().length();
            }
            int blockHalfPoint = blockTextLength / 2;
            before = actualOffset <= blockHalfPoint;
        } else {
            MapList mapList = m.getManager().getMapList();
            before = mapList.findNextNonWhitespace(mapList.indexOf(mapList.getCurrent())) != null;
        }

        Element table = BBX.CONTAINER.TABLE.create();
        int DEFAULT_ROWS = 3;
        for (int i = 0; i < DEFAULT_ROWS; i++) {
            Element newRow = BBX.CONTAINER.TABLE_ROW.create(TableRowType.NORMAL);
            table.appendChild(newRow);
            int DEFAULT_COLS = 3;
            for (int j = 0; j < DEFAULT_COLS; j++) {
                newRow.appendChild(BBX.BLOCK.TABLE_CELL.create());
            }
        }

        Node sibling = caret.getNode();
        while (!BBX.BLOCK.isA(sibling) && !BBX.CONTAINER.isA(sibling)) {
            sibling = sibling.getParent();
            if (sibling == null) {
                throw new EditingException("Block not found");
            }
        }

        ParentNode parent = sibling.getParent();
        int index = parent.indexOf(sibling);
        parent.insertChild(table, before ? index : index + 1);
        return table;
    }

    private boolean validateNode(Node node, Manager manager, boolean insert) {
        if (node == null || node instanceof Document || node.getDocument() == null || node.getDocument().getRootElement() == node) {
            return false;
        }
        if (insert && manager.getMapList().getCurrent().isReadOnly()) {
            return false;
        }
        if (insert && manager.getMapList().getCurrent() instanceof Uneditable) {
            ((Uneditable) manager.getMapList().getCurrent()).blockEdit(manager);
            return false;
        }
        return insert || Manager.getTableParent(node) != null;
    }

    /*
     * Logic for opening Table Editor through "Convert Text to Table" menu item
     */
    public void convertTextToTable(Manager manager) {
        this.m = manager.getDocument().getSettingsManager();
        List<Element> blocks = manager.getSimpleManager().getCurrentSelection().getSelectedBlocks();

        if (blocks.size() < 2) {
            Notify.INSTANCE.notify(ERROR_CONVERT_TEXT_UNSELECTED, ERROR_DIALOG_TITLE);
            return;
        }

        for (Element block : blocks) {
            if (Manager.getTableParent(block) != null) {
                Notify.INSTANCE.notify(ERROR_CONVERT_TEXT_ALREADY_IN_TABLE, ERROR_DIALOG_TITLE);
                return;
            }
        }

        new ConversionColumnSelectorDialog().open(i -> {
            if (i != null) {
                //Create the table and set tableNode to it
                tableNode = createConvertedTable(blocks, i);

                //insert table before selected blocks
                blocks.getFirst().getParent().insertChild(tableNode, blocks.getFirst().getParent().indexOf(blocks.getFirst()));
                open(nodes -> {
                    blocks.forEach(e -> {
                        //If they press ok, delete all existing blocks
                        //Detach each child so that we can run the block through BBXUtils.cleanupBlock
                        while (e.getChildCount() > 0) {
                            e.getChild(0).detach();
                        }
                        //Delete any parent boxes/lists that are now empty
                        BBXUtils.cleanupBlock(e);
                    });
                    manager.getSimpleManager().dispatchEvent(new org.brailleblaster.perspectives.mvc.events.ModifyEvent(Sender.NO_SENDER, true, nodes));
                }, n -> {
                    //remove the table that we inserted if they cancel
                    tableNode.detach();
                });
            }
        }, manager.getWpManager().getShell(), blocks.size());

    }

    private Element createConvertedTable(List<Element> blocks, int columns) {
        Element parent = BBX.CONTAINER.TABLE.create();
        Element curRow = null;
        int columnNum = 0;
        for (Element block : blocks) {
            if (columnNum == 0) {
                curRow = BBX.CONTAINER.TABLE_ROW.create(TableRowType.NORMAL);
                parent.appendChild(curRow);
            }
            Element tableCell = BBX.BLOCK.TABLE_CELL.create();
            block = block.copy();
            while (block.getChildCount() > 0) {
                Node child = block.getChild(0);
                child.detach();
                tableCell.appendChild(child);
            }
            curRow.appendChild(tableCell);
            columnNum++;
            if (columnNum == columns) {
                columnNum = 0;
            }
        }

        //fill out the rest of the row with empty table blocks
        while (columnNum < columns && columnNum != 0) {
            curRow.appendChild(BBX.BLOCK.TABLE_CELL.create());
            columnNum++;
        }
        return parent;
    }

    /**
     * Open the dialog
     */
    private void open(Consumer<Node[]> callback, Consumer<Node> onCancel) {
        shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
        shell.setText("Table Editor");
        shell.setLayout(new MigLayout("fill"));
        if (onCancel != null) {
            shell.addDisposeListener(e -> onCancel.accept(null));
        }
        options.clear();
        if (!isFacingTable(tableNode) && !isFacingTable((Element) tableNode.getParent())) {
            options.addAll(findOptions(tableNode));
            List<Element> captions = TableUtils.findCaption(tableNode, m.getEngine().getStyleMap());
            List<List<Node>> rows = getTableCells(tableNode);
            Element tnContainer = copyTNContainer(getTranscribersNote(tableNode));
            state = new InternalTable(rows, captions, tnContainer, getTableType(tableNode));
            createContents();
        } else {
            if (!"facingTable".equals(tableNode.getAttributeValue("class")))
                tableNode = (Element) tableNode.getParent();

            Element[] tables = getFacingTableParents(tableNode);
            int split = getTableCells(tables[0]).getFirst().size();
            List<List<Node>> combinedTables = combineFacingTables(tableNode);
            state = new InternalFacingTable(combinedTables, split, new ArrayList<>(), TableType.SIMPLE_FACING);
            createFacingContents();
        }
        //Prefer setLargeDialogSize for automatic sizing without scrollbars and weird look on Linux
        EasySWT.INSTANCE.setLargeDialogSize(shell);
        shell.open();
        shell.layout();
        this.callback = callback;
    }

    private void createContents() {
        List<List<Node>> rows = state.getNodes();
        TableType type = state.getType();

        ScrolledComposite sc = new ScrolledComposite(shell, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        sc.setExpandHorizontal(true);
        sc.setExpandVertical(true);
        sc.setShowFocusedControl(true);
        sc.setLayoutData("wmin 0, hmin 0, grow");

        int totalRows = rows.size();
        int totalColumns = state.getCols();
        List<CellText> textBoxes = createTexts(sc, true);
        Composite buttonPanel = new Composite(shell, SWT.NONE);
        buttonPanel.setLayout(new GridLayout(8, false));
        buttonPanel.setLayoutData("dock south, grow");

        Label tableTypeLabel = EasySWT.makeLabel(buttonPanel, "Table type:", 1);
        EasySWT.buildGridData().setHint(60, null).setAlign(SWT.RIGHT, SWT.CENTER).applyTo(tableTypeLabel);

        tableTypeCombo = new Combo(buttonPanel, SWT.READ_ONLY);
        EasySWT.buildGridData().setHint(150, null).setAlign(SWT.LEFT, SWT.BEGINNING).applyTo(tableTypeCombo);
        String[] tableTypeNames = new String[tableTypes.length];
        for (int i = 0; i < tableTypes.length; i++) {
            tableTypeNames[i] = tableTypes[i].displayName;
        }
        tableTypeCombo.setItems(tableTypeNames);
        tableTypeCombo.select(0);
        String tableType = type.equals(TableType.UNSET) ? tableNode.getAttributeValue("format") : type.displayName.toLowerCase();
        if ("simple".equals(tableType))
            tableTypeCombo.select(tableTypeCombo.indexOf(TableType.SIMPLE.displayName));
        else if ("listed".equals(tableType))
            tableTypeCombo.select(tableTypeCombo.indexOf(TableType.LISTED.displayName));
        else if ("stairstep".equals(tableType))
            tableTypeCombo.select(tableTypeCombo.indexOf(TableType.STAIRSTEP.displayName));
        else if ("linear".equals(tableType))
            tableTypeCombo.select(tableTypeCombo.indexOf(TableType.LINEAR.displayName));

        Button formatSpecificOption = EasySWT.makePushButton(buttonPanel, "Format Specific Option", 1, null);
        formatSpecificOption.setVisible(false);

        Label spacingLabel = EasySWT.makeLabel(buttonPanel, "", 1);
        EasySWT.buildGridData().setColumns(1).setGrabSpace(true, false).applyTo(spacingLabel);

        EasySWT.makeLabel(buttonPanel, "Rows:", 1);

        rowText = EasySWT.makeText(buttonPanel, 40, 1);
        EasyListeners.verifyNumbersOnly(rowText);
        rowText.setText(Integer.toString(totalRows));
        rowText.setTextLimit(2);

        EasySWT.makeLabel(buttonPanel, "Columns:", 1);

        colText = EasySWT.makeText(buttonPanel, 1);
        EasySWT.buildGridData().setHint(40, null).applyTo(colText);
        EasyListeners.verifyNumbersOnly(colText);
        colText.setText(Integer.toString(totalColumns));
        colText.setTextLimit(2);
        EasyListeners.modify(rowText, (e) -> {
            if (rowText.getText().isEmpty() || colText.getText().isEmpty())
                return;
            if (Integer.parseInt(colText.getText()) * Integer.parseInt(rowText.getText()) > MAX_CELLS) {
                EasySWT.makeEasyOkDialog("Error", "Table cannot exceed " + MAX_CELLS + " cells", shell);
                return;
            }
            updateState(textBoxes);
            deleteTexts(sc, textBoxes);
            state.setDisplayedRows(Integer.parseInt(rowText.getText()));
            recreateTexts(sc, textBoxes, formatSpecificOption);
            checkFormatSpecificOption(sc, formatSpecificOption, textBoxes);
        });
        EasyListeners.modify(colText, (e) -> {
            if (rowText.getText().isEmpty() || colText.getText().isEmpty())
                return;
            if (Integer.parseInt(colText.getText()) * Integer.parseInt(rowText.getText()) > MAX_CELLS) {
                EasySWT.makeEasyOkDialog("Error", "Table cannot exceed " + MAX_CELLS + " cells", shell);
                return;
            }
            updateState(textBoxes);
            deleteTexts(sc, textBoxes);
            state.setDisplayedCols(Integer.parseInt(colText.getText()));
            recreateTexts(sc, textBoxes, formatSpecificOption);
            checkFormatSpecificOption(sc, formatSpecificOption, textBoxes);
            if (tableTypes[tableTypeCombo.getSelectionIndex()] == TableType.SIMPLE && options.contains(TableUtils.SimpleTableOptions.CUSTOM_WIDTHS)) {
                options.remove(TableUtils.SimpleTableOptions.CUSTOM_WIDTHS);
                widths = null;
                EasySWT.makeEasyOkDialog("Custom widths reset", "Custom column widths have been reset", shell);
                if (options.isEmpty()) {
                    formatSpecificOption.setText("Simple Table Options");
                }
            }
        });

        tableTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (state.getTNContainer() != null) {
                    removeTNContainerFromState(sc, textBoxes, formatSpecificOption);
                    updateRowColTexts();
                }
                if (tableTypes[tableTypeCombo.getSelectionIndex()] == TableType.SIMPLE_FACING) {
                    //Prompt user on how to split facing table
                    int result = new ChangeToFacingDialog().open(shell, state, false)[0];
                    if (result == -1) {
                        //Dialog was canceled, switch back
                        tableTypeCombo.select(tableTypeCombo.indexOf(TableType.SIMPLE.displayName));
                    } else {
                        updateState(textBoxes);
                        List<List<Node>> tempRows = state.getDisplayedNodes();
                        state = new InternalFacingTable(tempRows, result, state.getCaptions(), TableType.SIMPLE_FACING);
                        rebuildView();
                    }
                } else {
                    checkFormatSpecificOption(sc, formatSpecificOption, textBoxes);
                }
            }
        });
        makeButtonPanel(buttonPanel, textBoxes, null);
        checkFormatSpecificOption(sc, formatSpecificOption, textBoxes);
        createMenu(textBoxes, null, debug);
    }

    /**
     * Create double-width scrolled composite for facing tables
     */
    private void createFacingContents() {
        List<List<Node>> leftRows = ((InternalFacingTable) state).getLeftNodes();
        List<List<Node>> rightRows = ((InternalFacingTable) state).getRightNodes();
        ScrolledComposite doubleSC = new ScrolledComposite(shell, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        doubleSC.setExpandHorizontal(true);
        doubleSC.setExpandVertical(true);
        doubleSC.setLayoutData("wmin 0, hmin 0");
        doubleSC.setLayout(new MigLayout("fill"));
        Composite tableContainer = new Composite(doubleSC, SWT.NONE);
        tableContainer.setLayoutData("wmin 0, hmin 0");
        tableContainer.setLayout(new MigLayout("fill"));
        Composite leftSC = new Composite(tableContainer, SWT.NONE);
        leftSC.setLayoutData("wmin 0, hmin 0, grow, dock west");
        leftSC.setLayout(new GridLayout(1, false));
        Label leftTableLabel = EasySWT.makeLabel(leftSC, "Left Page", 1);
        EasySWT.buildGridData().setAlign(SWT.CENTER, SWT.BEGINNING).applyTo(leftTableLabel);
        //Use the left-page table to create text boxes
        List<CellText> leftBoxes = createTexts(leftSC, leftRows, ((InternalFacingTable) state).split, state.getRows(), new ArrayList<>(), null, false);
        if (leftBoxes == null) {
            return;
        }

        Label border = new Label(tableContainer, SWT.BORDER);
        border.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
        border.setLayoutData("growy, wmax 4");
        Composite rightSC = new Composite(tableContainer, SWT.NONE);
        Label rightTableLabel = EasySWT.makeLabel(rightSC, "Right Page", 1);
        EasySWT.buildGridData().setAlign(SWT.CENTER, SWT.BEGINNING).applyTo(rightTableLabel);
        rightSC.setLayoutData("wmin 0, hmin 0, grow, dock east");
        rightSC.setLayout(new GridLayout(1, false));
        //Use the right page table to create text boxes
        List<CellText> rightBoxes = createTexts(rightSC, rightRows, state.getCols() - ((InternalFacingTable) state).split, state.getRows(), new ArrayList<>(), null, false);
        if (rightBoxes == null) {
            return;
        }

        rightBoxes.forEach((ct) -> ct.col = ct.col + ((InternalFacingTable) state).split);

        doubleSC.setContent(tableContainer);
        doubleSC.setMinSize(tableContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        Composite buttonPanel = new Composite(shell, SWT.NONE);
        buttonPanel.setLayout(new GridLayout(8, false));
        buttonPanel.setLayoutData("wmin 0, growx, dock south");

        Label tableTypeLabel = EasySWT.makeLabel(buttonPanel, "Table type:", 1);
        EasySWT.buildGridData().setHint(60, null).setAlign(SWT.RIGHT, SWT.CENTER).applyTo(tableTypeLabel);
        tableTypeCombo = new Combo(buttonPanel, SWT.READ_ONLY);
        EasySWT.buildGridData().setHint(150, null).setAlign(SWT.LEFT, SWT.BEGINNING).applyTo(tableTypeCombo);
        String[] tableTypeNames = new String[tableTypes.length];
        for (int i = 0; i < tableTypes.length; i++) {
            tableTypeNames[i] = tableTypes[i].displayName;
        }
        tableTypeCombo.setItems(tableTypeNames);
        tableTypeCombo.select(tableTypeCombo.indexOf(TableType.SIMPLE_FACING.displayName));
        tableTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (tableTypes[tableTypeCombo.getSelectionIndex()] != TableType.SIMPLE_FACING) {
                    //User wants to switch from facing to a non-facing table.
                    int newSelection = tableTypeCombo.getSelectionIndex();
                    updateState(leftBoxes);
                    updateState(rightBoxes);
                    state = new InternalTable(state.getNodes(), state.getCaptions(), state.getTNContainer(), tableTypes[newSelection]);
                    //Delete facing table view
                    rebuildView();
                    tableTypeCombo.select(newSelection);
                }
            }
        });

        EasySWT.makePushButton(buttonPanel, CHANGE_DIMENSIONS_BUTTON, 2, e -> {
            int[] results = new ChangeToFacingDialog().open(shell, state, true);
            if (results[0] > 0) {
                state.setRows(results[1]);
                state.setCols(results[2]);
                ((InternalFacingTable) state).split = results[0];

                //Delete the facing table view
                rebuildView();
            }
        });

        Label spacingLabel = EasySWT.makeLabel(buttonPanel, "", 1);
        EasySWT.buildGridData().setColumns(4).setGrabSpace(true, false).applyTo(spacingLabel);

        makeButtonPanel(buttonPanel, leftBoxes, rightBoxes);
        createMenu(leftBoxes, rightBoxes, debug);
    }

    private void createMenu(List<CellText> texts, List<CellText> rightTexts, boolean debug) {
        deleteMenu();
        MenuBuilder mb = new MenuBuilder(shell);
        boolean edit = mode == Mode.EDIT;
        mb.addToMenu("Actions")
                .addPushItem("Reformat Table", SWT.NONE, edit, e -> selectStyle((style) -> {
                    destroyTable(style, texts, rightTexts);
                    closeShell();
                }))
                .addPushItem("Advanced Reformat", SWT.NONE, edit, e -> {
                    if (advancedDestroy(texts, rightTexts)) {
                        closeShell();
                    }
                })
                .addPushItem("Delete Table", SWT.NONE, edit, e -> deleteTable())
                .addToMenu("Tools")
                .addPushItem("Swap columns and rows", -1, e -> {
                    state.setRows(state.getDisplayedRows());
                    state.setCols(state.getDisplayedCols());
                    updateState(texts);
                    if (rightTexts != null) {
                        updateState(rightTexts);
                    }
                    swapNodes();
                    rebuildView();
                })
                .addSubMenu("Add Emphasis To All", createEmphasisSubMenu(texts, rightTexts))
                .addPushItem(localeHandler.get("tableEditor.mathToAll"), SWT.MOD3 | MathModuleUtils.MATH_ACCELERATOR, e -> {
                    texts.forEach(t -> t.text.applyEmphasisToAll(MathTags.MATH));
                    if (rightTexts != null)
                        rightTexts.forEach(t -> t.text.applyEmphasisToAll(MathTags.MATH));
                })
                .addPushItem("Remove All Emphasis", SWT.MOD1 | SWT.MOD3 | 'E', e -> {
                    texts.forEach(t -> t.text.removeAllEmphasis());
                    if (rightTexts != null)
                        rightTexts.forEach(t -> t.text.removeAllEmphasis());
                })
                .addToMenu("Emphasis");
        createEmphasisMenu(mb, tag -> {
            BBStyleableText curText = getCurrentText(texts, rightTexts);
            if (curText != null)
                curText.toggleTag(tag);
        });
        mb.addToMenu("Translation")
                .addPushItem("Direct Translation", EmphasisTags.CTEXACT.getDefaultAccelerator(), e -> {
                    BBStyleableText curText = getCurrentText(texts, rightTexts);
                    if (curText != null)
                        curText.toggleTag(EmphasisTags.CTEXACT);
                })
                .addPushItem("Uncontracted Translation", EmphasisTags.CTUNCONTRACTED.getDefaultAccelerator(), e -> {
                    BBStyleableText curText = getCurrentText(texts, rightTexts);
                    if (curText != null)
                        curText.toggleTag(EmphasisTags.CTUNCONTRACTED);
                })
                .addPushItem("Math Translation", MathModuleUtils.MATH_ACCELERATOR, e -> {
                    BBStyleableText curText = getCurrentText(texts, rightTexts);
                    if (curText != null)
                        curText.toggleTag(MathTags.MATH);
                });
        if (debug) {
            mb.addToMenu("Debug")
                    .addPushItem("Print state", 0, e -> {
                        updateState(texts);
                        System.out.println(state.toPrintableState());
                    })
                    .addPushItem("Fill table", 0, e -> {
                        List<List<Node>> newList = new ArrayList<>();
                        for (int i = 0; i < state.getRows(); i++) {
                            newList.add(new ArrayList<>());
                            for (int j = 0; j < state.getCols(); j++) {
                                Element newElement = new Element("td");
                                newElement.appendChild("Test " + i + ", " + j);
                                newList.get(i).add(newElement);
                            }
                        }
                        state = state instanceof InternalFacingTable
                                ? new InternalFacingTable(newList, ((InternalFacingTable) state).split, state.getCaptions(), state.getType())
                                : new InternalTable(newList, state.getCaptions(), state.getTNContainer(), state.getType());
                        rebuildView();
                    });
        }
        mb.build();
    }

    private void rebuildView() {
        Control[] children = shell.getChildren();
        for (Control child : children) {
            child.dispose();
        }
        if (state instanceof InternalFacingTable) {
            createFacingContents();
        } else {
            createContents();
        }
        shell.layout(true);
    }

    private void deleteMenu() {
        if (shell.getMenuBar() != null) {
            shell.getMenuBar().dispose();
        }
    }

    private void createEmphasisMenu(MenuBuilder mb, Consumer<EmphasisTags> onClick) {
        for (EmphasisTags tag : EmphasisTags.getEntries()) {
            if (tag == EmphasisTags.ITALICBOLD || tag == EmphasisTags.CTEXACT || tag == EmphasisTags.CTUNCONTRACTED)
                continue;
            mb.addPushItem(tag.getTagName(), tag.getDefaultAccelerator(), e -> onClick.accept(tag));
        }
    }

    private SubMenuBuilder createEmphasisSubMenu(List<CellText> texts, List<CellText> rightTexts) {
        SubMenuBuilder smb = new SubMenuBuilder(shell);
        for (EmphasisTags tag : EmphasisTags.getEntries()) {
            if (tag == EmphasisTags.ITALICBOLD)
                continue;
            smb.addPushItem(tag.getTagName(), SWT.MOD3 | tag.getDefaultAccelerator(), e -> {
                texts.forEach(t -> t.text.applyEmphasisToAll(tag));
                if (rightTexts != null)
                    rightTexts.forEach(t -> t.text.applyEmphasisToAll(tag));
            });
        }
        return smb;
    }

    /**
     * Makes reformat, save, and cancel buttons
     */
    private void makeButtonPanel(Composite buttonPanel, List<CellText> textBoxes, List<CellText> rightBoxes) {
        Composite spacingComp = EasySWT.makeComposite(buttonPanel, 1);
        EasySWT.buildGridData().setColumns(3).applyTo(spacingComp);

        Composite saveCancelComp = EasySWT.makeComposite(buttonPanel, 2);
        EasySWT.buildGridData().setAlign(SWT.END, SWT.CENTER).setColumns(5).applyTo(saveCancelComp);

        Button saveButton = EasySWT.makePushButton(saveCancelComp, SAVE_BUTTON, 2, (e) -> {
            updateState(textBoxes);
            Node[] newNodes = rightBoxes != null ? generateNewFacingTable(textBoxes, rightBoxes) : generateNewTable(state.getNodes(), state.getDisplayedCols(), state.getDisplayedRows(), tableNode);
            if (newNodes != null) {
                callback.accept(newNodes);
            }
            closeShell();
        });
        EasySWT.buildGridData().setHint(80, 30).setColumns(1).applyTo(saveButton);

        Button cancelButton = EasySWT.makePushButton(saveCancelComp, CANCEL_BUTTON, 2, (e) -> shell.close());
        EasySWT.buildGridData().setHint(80, 30).setColumns(1).applyTo(cancelButton);
    }

    private List<List<Node>> combineFacingTables(Element parent) {
        Element[] parents = getFacingTableParents(parent);
        return combineNodeLists(getTableCells(parents[0]), getTableCells(parents[1]));
    }

    private List<List<Node>> combineNodeLists(List<List<Node>> leftTable, List<List<Node>> rightTable) {
        List<List<Node>> returnList = new ArrayList<>();
        for (int row = 0; row < Math.max(leftTable.size(), rightTable.size()); row++) {
            returnList.add(new ArrayList<>());
            if (row < leftTable.size()) {
                for (int col = 0; col < leftTable.get(row).size(); col++) {
                    returnList.getLast().add(leftTable.get(row).get(col));
                }
            }
            if (row < rightTable.size()) {
                for (int col = 0; col < rightTable.get(row).size(); col++) {
                    returnList.getLast().add(rightTable.get(row).get(col));
                }
            }
        }
        return returnList;
    }

    private void checkFormatSpecificOption(Composite parent, Button button, List<CellText> textBoxes) {
        if (button.getListeners(SWT.Selection).length == 0) {
            button.addSelectionListener(getFormatSpecificButtonListener(parent, textBoxes, button));
        }

        TableType type = tableTypes[tableTypeCombo.getSelectionIndex()];
        if (type == TableType.SIMPLE) {
            button.setText("Simple Table Options" + (!options.isEmpty() ? "*" : ""));
        } else if (type == TableType.STAIRSTEP || type == TableType.LINEAR || type == TableType.LISTED) {
            if (state.getTNContainer() == null) {
                button.setText("Create Transcriber Note Heading");
            } else {
                button.setText("Remove Transcriber Note Heading");
            }
        } else {
            button.setText("");
        }

        if (button.getText().isEmpty()) {
            button.pack(true);
            button.setVisible(false);
        } else {
            button.getParent().layout(true);
            button.setSize(EasySWT.getWidthOfText(button.getText() + "    "), button.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
            EasySWT.buildGridData().applyTo(button);
            button.setVisible(true);
        }
    }

    private SelectionAdapter getFormatSpecificButtonListener(Composite parent, List<CellText> textBoxes, Button formatSpecificButton) {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableType type = tableTypes[tableTypeCombo.getSelectionIndex()];
                if (type == TableType.SIMPLE) {
                    new SimpleTableOptionDialog().open(shell, tableNode, state.getDisplayedCols(), options, m);
                } else if (type == TableType.STAIRSTEP || type == TableType.LINEAR || type == TableType.LISTED) {
                    if (state.getTNContainer() == null) {
                        Element tNote = BBX.BLOCK.STYLE.create("7-5");
                        if (type == TableType.LISTED) {
                            tNote.appendChild(LISTED_TN_HEADING);
                        } else if (type == TableType.STAIRSTEP) {
                            tNote.appendChild(STAIRSTEP_TN_HEADING);
                        } else {
                            tNote.appendChild(LINEAR_TN_HEADING);
                        }
                        //Save current changes
                        updateState(textBoxes);
                        List<CellText> firstRow = textBoxes.stream().filter((t) -> t.row == 0).collect(Collectors.toList());
                        if (type != TableType.LISTED) {
                            //Remove first row from text boxes
                            for (CellText text : firstRow) {
                                textBoxes.remove(text);
                            }
                            //Set each remaining text box to be one row higher
                            for (CellText text : textBoxes) {
                                text.row--;
                            }
                            state.getNodes().removeFirst();
                            //Set table to be one less row
                            //Note: state.setRows does not need to be called because it is
                            //based on the size of state's nodes list, and the row was deleted
                            //Displayed Rows, however, are a field that needs to be updated
                            //manually
                            state.setDisplayedRows(state.getDisplayedRows() - 1);
                            //Create new state without first row
                            updateState(textBoxes);
                        }
                        Element tnContainer = createTNContainer(tNote, firstRow);
                        state.setTNContainer(tnContainer);
                        if (!shell.isDisposed()) {
                            //Possibility for the shell to be closed if the table is too big in createTNContainer.
                            updateRowColTexts();
                            deleteTexts(parent, textBoxes);
                            recreateTexts(parent, textBoxes, formatSpecificButton);
                            checkFormatSpecificOption(parent, formatSpecificButton, textBoxes);
                        }
                    } else {
                        removeTNContainerFromState(parent, textBoxes, formatSpecificButton);
                    }
                }
            }
        };
    }

    private void closeShell() {
        if (shell.getListeners(SWT.Dispose).length > 0) {
            shell.removeListener(SWT.Dispose, shell.getListeners(SWT.Dispose)[0]);
        }
        shell.close();
    }

    private List<CellText> createTexts(Composite sc, boolean destroyable) {
        return createTexts(sc, state.getNodes(), state.getDisplayedCols(), state.getDisplayedRows(), state.getCaptions(), state.getTNContainer(), destroyable);
    }

    /**
     * Given a list of table cells, create text boxes and put them into the appropriate composite
     *
     * @param destroyable If true, includes "Destroy Row" buttons next to each row.
     */
    private List<CellText> createTexts(
            Composite sc,
            List<List<Node>> rows,
            int displayCols,
            int displayRows,
            List<Element> captions,
            Element tnContainer,
            boolean destroyable) {

        List<CellText> returnList = new ArrayList<>();
        Composite tableComp = new Composite(sc, SWT.NONE);
        tableComp.setLayout(new GridLayout(displayCols + 1, false));
        EasySWT.INSTANCE.setGridData(tableComp);

        //Copy rows list into temporary list
        List<List<Node>> tempRows = new ArrayList<>(rows);

        if (!captions.isEmpty()) {
            //If table has a caption, include caption text box and store it in the first position of the return list.
            Element captionParent = captions.getFirst();
            IStyle parentStyle = m.getEngine().getStyle(captionParent);
            //Find caption element
            while (parentStyle == null || !parentStyle.getName().toLowerCase().contains("caption")) {
                captionParent = (Element) captionParent.getParent();
                if (captionParent == tableNode.getDocument().getRootElement()) {
                    captionParent = captions.getFirst();
                    break;
                }
                parentStyle = m.getEngine().getStyle(captionParent);
            }
            //Construct composite to hold caption text
            Composite captionRow = EasySWT.makeComposite(tableComp, displayCols + 1);
            EasySWT.buildGridData().setColumns(displayCols + 1).applyTo(captionRow);

            //Add destroy button if destroyable
            Button destroyButton = null;
            if (destroyable) {
                destroyButton = EasySWT.makePushButton(captionRow, "X", 1, null);
                EasySWT.buildGridData().setAlign(SWT.CENTER, SWT.CENTER).applyTo(destroyButton);
                AccessibilityUtils.setName(destroyButton, ACCESSIBLE_DELETE_CAPTION_NAME);
            }

            //Second composite contains Caption label and text box
            Composite labelAndTextContainer = EasySWT.makeComposite(captionRow, 2);
            Label captionLabel = EasySWT.makeLabel(labelAndTextContainer, "Caption", 1);
            EasySWT.buildGridData().setAlign(SWT.CENTER, SWT.CENTER).applyTo(captionLabel);
            Composite textHolder = EasySWT.makeComposite(labelAndTextContainer, 1);
            CellText captionText = new CellText(textHolder, -1, -1);
            captionText.setCaption(true);
            EasySWT.buildGridData().setHint(400, 100).setAlign(SWT.CENTER, SWT.CENTER).applyTo(captionText.getWidget());

            //Add caption element to text box
            captionText.text.setXML(captionParent);
            captionText.getWidget().setData(new Element("caption"));

            //Screen readers should read caption label
            AccessibilityUtils.setName(captionText.getWidget(), ACCESSIBLE_CAPTION_NAME);
            returnList.add(captionText);
            if (destroyable) {
                EasyListeners.selection(destroyButton, e -> destroyCaption(captionText));
            }
        }

        if (tnContainer != null) {
            //Note: validity of tnContainer was already checked in getTranscribersNote, so we can safely make
            //assumptions about the BBX without checking types

            Element containerCopy = copyTNContainer(tnContainer, state.getType());

            Composite tnRow = EasySWT.makeComposite(tableComp, displayCols + 1);
            EasySWT.buildGridData().setColumns(displayCols + 1).applyTo(tnRow);

            Label tnLabel = EasySWT.makeLabel(tnRow, "TN Heading", 1);
            EasySWT.buildGridData().setAlign(SWT.CENTER, SWT.CENTER).applyTo(tnLabel);

            CellText tnHeadingText = new CellText(tnRow, -1, -1);
            EasySWT.buildGridData().setColumns(displayCols).setHint(400, 50).applyTo(tnHeadingText.getWidget());
            tnHeadingText.setTN(true);
            tnHeadingText.text.setXML((Element) containerCopy.getChild(0));
            returnList.add(tnHeadingText);

            if (state.getType() != TableType.LISTED) {
                if (state.getType() == TableType.STAIRSTEP) {
                    Label spacingLabel = EasySWT.makeLabel(tnRow, "", 1);
                    EasySWT.buildGridData().setAlign(SWT.CENTER, SWT.CENTER).applyTo(spacingLabel);
                    for (int i = 0; i < displayCols; i++) {
                        int margin = (i * 2) + 1;
                        Label colLabel = EasySWT.makeLabel(tnRow, margin + "-" + margin, 1);
                        EasySWT.buildGridData().setAlign(SWT.CENTER, SWT.CENTER).applyTo(colLabel);
                    }
                }

                Label tnLabel2 = EasySWT.makeLabel(tnRow, state.getType() == TableType.LINEAR ? "7-5" : "", 1);
                EasySWT.buildGridData().setAlign(SWT.CENTER, SWT.CENTER).applyTo(tnLabel2);

                for (int i = 0; i < displayCols; i++) {
                    Composite textComp = EasySWT.makeComposite(tnRow, 1);
                    CellText newText = new CellText(textComp, -1, i);
                    EasySWT.buildGridData().setHint(200, 150).applyTo(newText.getWidget());
                    if (i + 1 < containerCopy.getChildCount()) {
                        try {
                            newText.text.setXML((Element) containerCopy.getChild(i + 1));
                        } catch (NodeException ne) {
                            Notify.showMessage("Error setting TN text. Table may be too long:\n" + ne.getMessage());
                            closeShell();
                            return null;
                        }
                    }
                    newText.setTN(true);
                    returnList.add(newText);
                }
            }
        }

        //First column has no text boxes so skip it
        EasySWT.makeLabel(tableComp, "", 1);

        //Make column labels
        for (int i = 0; i < displayCols; i++) {
            Label newLabel = EasySWT.makeLabel(tableComp, "Column " + (i + 1), 1);
            EasySWT.buildGridData().setAlign(SWT.CENTER, SWT.BEGINNING).applyTo(newLabel);
        }

        //If more rows are needed than are currently in the state, add empty rows to our temporary list
        while (tempRows.size() < displayRows) {
            tempRows.add(new ArrayList<>());
        }
        int iterator = 1;
        for (List<Node> cellList : tempRows) {
            final int iteration = iterator - 1; //Final for lambda
            //Make "Row _" label (and destroy button if applicable)
            Composite rowLabel = EasySWT.makeComposite(tableComp, 2);
            if (destroyable) {
                Button destroyButton = EasySWT.makePushButton(rowLabel, "X", 1, e -> destroyRow(returnList, iteration));
                EasySWT.buildGridData().setAlign(SWT.RIGHT, SWT.RIGHT).applyTo(destroyButton);
                AccessibilityUtils.setName(destroyButton, ACCESSIBLE_DELETE_ROW_NAME + " " + iterator);
            }
            Label newLabel = EasySWT.makeLabel(rowLabel, "", 1);
            //Don't add "heading" label when transcriber's notes are present because the tn is technically the heading
            if (iterator == 1 && tnContainer == null) {
                newLabel.setText("Heading");
            } else {
                newLabel.setText("Row " + iterator);
            }
            EasySWT.buildGridData().setAlign(SWT.RIGHT, SWT.CENTER).setColumns(destroyable ? 1 : 2).applyTo(newLabel);

            //If the state row has less columns than displayCols, add empty td elements to fill out the rest of the row
            while (cellList.size() < displayCols) {
                Element dummyElement = new Element("td");
                cellList.add(dummyElement);
            }

            //Create text boxes and add table cell elements to them
            int totalTexts = 0;
            for (Node node : cellList) {
                Composite textPanel = EasySWT.makeComposite(tableComp, 1);
                CellText newText = new CellText(textPanel, tempRows.indexOf(cellList), cellList.indexOf(node));
                EasySWT.buildGridData().setHint(200, 150).applyTo(newText.getWidget());
                newText.text.setXML((Element) node);
                returnList.add(newText);
                totalTexts++;
                //Listen for Ctrl+Shift+~ key press for debug menu
                newText.getWidget().addKeyListener(debugListener());
                if (totalTexts == displayCols)
                    break;
            }
            if (iterator == displayRows)
                break;
            iterator++;
        }

        if (sc instanceof ScrolledComposite) {
            ((ScrolledComposite) sc).setContent(tableComp);
            ((ScrolledComposite) sc).setMinSize(tableComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        }

        //Always set focus to the first text box on creation
        if (!returnList.isEmpty()) {
            returnList.getFirst().getWidget().setFocus();
        }

        //Detect the Ctl-Alt-Arrow keypresses to navigate around the textBoxes, which should amount to traversing a list
        for (CellText cell : returnList) {
            cell.getWidget().addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    //System.out.println("KeyEvent " + e.keyCode + " at " + cell.getRow() + "," + cell.getCol());
                    int textIndex = textFocusedWhere(returnList);
                    if (textIndex != -1) {
                        if (((e.stateMask & SWT.CTRL) == SWT.CTRL) && (e.stateMask & SWT.ALT) == SWT.ALT) {
                            if (e.keyCode == SWT.ARROW_UP) {
                                if (textIndex - state.getDisplayedCols() >= 0) {
                                    returnList.get(textIndex - state.getDisplayedCols()).getWidget().setFocus();
                                } else {
                                    //Send to start of table. Might modify to go to start of row instead.
                                    returnList.getFirst().getWidget().setFocus();
                                }
                            } else if (e.keyCode == SWT.ARROW_DOWN) {
                                if (textIndex + state.getDisplayedCols() < returnList.size()) {
                                    returnList.get(textIndex + state.getDisplayedCols()).getWidget().setFocus();
                                } else {
                                    //Send to the end of the table. Might modify to go to end of row instead.
                                    returnList.getLast().getWidget().setFocus();
                                }
                            } else if (e.keyCode == SWT.ARROW_LEFT) {
                                if (textIndex - 1 >= 0) {
                                    returnList.get(textIndex - 1).getWidget().setFocus();
                                }
                            } else if (e.keyCode == SWT.ARROW_RIGHT) {
                                if (textIndex + 1 < returnList.size()) {
                                    returnList.get(textIndex + 1).getWidget().setFocus();
                                }
                            } else if (e.keyCode == SWT.HOME) {
                                returnList.getFirst().getWidget().setFocus();
                            } else if (e.keyCode == SWT.END) {
                                returnList.getLast().getWidget().setFocus();
                            }
                        }
                    }
                }
            });
        }

        return returnList;
    }

    /*
  Helper function that signifies if any of the table text boxes are in focus.
  To be used for navigation with Ctrl-arrow keys - don't want anything weird to happen with
  the other items on the menu.
  Returns the index where the current focus is, or -1 if the focus is not in the table.
   */
    private int textFocusedWhere(List<CellText> myText) {
        for (int i = 0; i < myText.size(); i++) {
            if (myText.get(i).getWidget().isFocusControl()) {
                return i;
            }
        }
        return -1;
    }

    private void recreateTexts(Composite sc, List<CellText> textBoxes, Button formatSpecificOptionButton) {
        textBoxes.clear();
        textBoxes.addAll(createTexts(sc, true));
        shell.layout();
    }

    /**
     * Remove a single row from the table
     */
    private void destroyRow(List<CellText> textBoxes, int rowNum) {
        if (state.getRows() == 2) {
            MessageBox question = new MessageBox(shell, SWT.YES | SWT.NO);
            question.setMessage("Destroying this row will eliminate the entire table. Would you like to continue?");
            question.setText("Destroy Row");
            if (question.open() == SWT.YES)
                selectStyle((style) -> {
                    destroyTable(style, textBoxes, null);
                    shell.close();
                });
            return;
        }
        Shell destroyRowPopup = new Shell(shell, SWT.DIALOG_TRIM);
        destroyRowPopup.setLayout(new GridLayout(1, false));
        destroyRowPopup.setText("Place Destroyed Row");
        Composite container = EasySWT.makeComposite(destroyRowPopup, 1);
        ButtonBuilder1 buttonBuilder = EasySWT.buildPushButton(container);
        buttonBuilder.text = "Place row before table";
        buttonBuilder.setSwtOptions(SWT.RADIO);
        Button radio1 = buttonBuilder.build();
        radio1.setSelection(true);
        buttonBuilder = EasySWT.buildPushButton(container);
        buttonBuilder.text = "Place row after table";
        buttonBuilder.setSwtOptions(SWT.RADIO);
        buttonBuilder.build();
        buttonBuilder = EasySWT.buildPushButton(container);
        buttonBuilder.text = "Delete row entirely";
        buttonBuilder.setSwtOptions(SWT.RADIO);
        Button radio3 = buttonBuilder.build();
        EasySWT.buildComposite(container)
                .addButton(OK_BUTTON, 1, (i) -> {
                    boolean before = radio1.getSelection();
                    if (radio3.getSelection()) {
                        destroyRowPopup.close();
                        destroyRow(null, textBoxes, before, true, rowNum);
                    } else {
                        selectStyle((style) -> destroyRow(style, textBoxes, before, false, rowNum));
                        destroyRowPopup.close();
                    }
                })
                .addButton(CANCEL_BUTTON, 1, (i) -> destroyRowPopup.close())
                .build();

        destroyRowPopup.open();
        destroyRowPopup.pack();
    }

    /**
     * Remove a caption from the table element
     */
    private void destroyCaption(CellText captionText) {
        if (state.getCaptions().isEmpty())
            return;
        Element parent = (Element) tableNode.getParent();
        int index = parent.indexOf(tableNode);
        state.getCaptions().forEach(Node::detach);
        Element caption = captionText.text.getXML(BBX.BLOCK.STYLE.create("Caption"));
        parent.insertChild(caption, index);
        callback.accept(new Node[]{caption, parent});
        shell.close();
    }

    /**
     * Remove a row from the table element and give it a style
     */
    private void destroyRow(Style replacementStyle, List<CellText> textBoxes, boolean before, boolean delete, int rowNum) {
        updateState(textBoxes);
        List<Node> nodes = state.getNodes().get(rowNum);
        ParentNode tableParent = tableNode.getParent();
        int index = tableParent.indexOf(tableNode);
        if (!before) {
            index++;
            if (index < tableParent.getChildCount() && tableParent.getChild(index) instanceof Element
                    && ((Element) tableParent.getChild(index)).getAttribute("class") != null
                    && ((Element) tableParent.getChild(index)).getAttributeValue("class").contains("utd:table")) {
                tableParent.getChild(index).detach();
            }
        }
        List<Node> nodesToTranslate = new ArrayList<>();
        if (replacementStyle != null) {
            for (Node node : nodes) {
                if (node instanceof Element && !node.getValue().isEmpty()) {
                    Element el = BBX.BLOCK.STYLE.create(replacementStyle.getName());
                    if (!delete) {
                        while (node.getChildCount() > 0) {
                            Node child = node.getChild(node.getChildCount() - 1);
                            child.detach();
                            el.insertChild(child, 0);
                        }
                        tableParent.insertChild(el, index);
                        nodesToTranslate.add(el);
                        index++;
                    }
                }
            }
        }
        state.getNodes().remove(rowNum);
        Node[] newNodes = generateNewTable(state.getNodes(), state.getDisplayedCols(), state.getDisplayedRows() - 1, tableNode);
        if (newNodes != null) {
            nodesToTranslate.addAll(Arrays.asList(newNodes));
            callback.accept(nodesToTranslate.toArray(new Node[0]));
        }
        shell.close();
    }

    /**
     * Delete CellTexts in a composite
     */
    private void deleteTexts(Composite parent, List<CellText> textBoxes) {
        textBoxes.forEach(CellText::dispose);
        textBoxes.clear();
        for (Control control : parent.getChildren()) {
            if (control instanceof Composite) {
                deleteTexts((Composite) control, textBoxes);
            }
            control.dispose();
        }
    }

    private void updateState(List<CellText> textBoxes) {
        state.setType(getTableType(tableTypeCombo.getItem(tableTypeCombo.getSelectionIndex())));
        List<CellText> tnTexts = textBoxes.stream().filter(CellText::isTN).collect(Collectors.toList());
        state.setTNContainer(createTNContainer(tnTexts));
        for (CellText text : textBoxes) {
            if (text.isTN()) {
                continue;
            }
            if (text.isCaption()) {
                state.getCaptions().clear();
                if (!text.getWidget().getText().isEmpty())
                    state.getCaptions().add(text.text.getXML(BBX.BLOCK.STYLE.create("Caption")));
                continue;
            }
            int row = text.row;
            int col = text.col;
            state.getNodes().get(row).remove(col);
            state.getNodes().get(row).add(col, text.text.getXML(BBX.BLOCK.TABLE_CELL.create()));
        }
    }

    private void removeTNContainerFromState(Composite parent, List<CellText> textBoxes, Button formatSpecificButton) {
        updateState(textBoxes);
        //Re-add tn heading cells to first row of table
        List<CellText> tnTexts = textBoxes.stream().filter(CellText::isTN).toList();
        if (tnTexts.size() > 1) {
            List<Node> firstRow = new ArrayList<>();
            for (int i = 1; i < tnTexts.size(); i++) {
                firstRow.add(tnTexts.get(i).text.getXML(BBX.BLOCK.STYLE.create("1-3")));
            }
            state.getNodes().addFirst(firstRow);
            state.setDisplayedRows(state.getDisplayedRows() + 1);
            updateRowColTexts();
        }
        state.setTNContainer(null);
        deleteTexts(parent, textBoxes);
        recreateTexts(parent, textBoxes, formatSpecificButton);
        checkFormatSpecificOption(parent, formatSpecificButton, textBoxes);
    }

    private void updateRowColTexts() {
        EasyListeners.pauseListener(rowText, SWT.Modify, w -> ((Text) w).setText(String.valueOf(state.getDisplayedRows())));
        EasyListeners.pauseListener(colText, SWT.Modify, w -> ((Text) w).setText(String.valueOf(state.getDisplayedCols())));
    }

    private Element createTNContainer(List<CellText> tnTexts) {
        if (tnTexts.isEmpty()) {
            return null;
        }
        if (state.getType().equals(TableType.UNSET)) {
            throw new IllegalStateException("state type");
        }

        Element firstBlock = tnTexts.getFirst().text.getXML(BBX.BLOCK.STYLE.create("7-5"));
        List<CellText> tnTextsCopy = new ArrayList<>(tnTexts);
        tnTextsCopy.removeFirst();
        return createTNContainer(firstBlock, tnTextsCopy);
    }

    private Element createTNContainer(Element note, List<CellText> firstRow) {
        Element container = BBX.CONTAINER.TABLETN.create();
        container.appendChild(BBXUtils.wrapAsTransNote(note));
        switch (state.getType()) {
            case STAIRSTEP:
                for (int i = 0; i < firstRow.size(); i++) {
                    int margin = (i * 2) + 1;
                    //TODO: This can create styles that go out of range if they get big enough; max allowed is 11-11; indent level 10
                    if (margin > 10) {
                        Notify.showMessage("Cannot add transcriber notes to a stairstep table of this size.");
                        closeShell();
                        return null;
                    }
                    Element block = firstRow.get(i).text.getXML(BBX.BLOCK.STYLE.create(margin + "-" + margin));
                    container.appendChild(block);
                }
                break;
            case LINEAR:
                Element secondBlock = BBX.BLOCK.STYLE.create("1-3");
                for (int i = 0; i < firstRow.size(); i++) {
                    Element span = firstRow.get(i).text.getXML(BBX.SPAN.OTHER.create());
                    secondBlock.appendChild(span);
                    if (i == 0) {
                        secondBlock.appendChild(": ");
                    } else if (i == firstRow.size() - 1) {
                        secondBlock.appendChild(".");
                    } else {
                        secondBlock.appendChild("; ");
                    }
                }
                container.appendChild(secondBlock);
                break;
            case LISTED:
                break;
            case SIMPLE:
                //Do nothing
            default:
                return null;
        }

        return container;
    }

    /**
     * Create a new table from the textBoxes
     */
    private Node[] generateNewTable(List<List<Node>> nodes, int cols, int rows, Element existingTable) {
        trimTable(nodes);
        //If entire table was trimmed, return a null table
        //TODO: Currently breaks (the already broken) facing tables
        if (nodes.isEmpty()) {
            return null;
        }
        Element newTable = BBX.CONTAINER.TABLE.create();
        int rowNum = 0;
        for (List<Node> row : nodes) {
            Element newRow = BBX.CONTAINER.TABLE_ROW.create(TableRowType.NORMAL);
            int colNum = 0;
            for (Node col : row) {
                newRow.appendChild(col);
                colNum++;
                if (colNum >= cols)
                    break;
            }
            newTable.appendChild(newRow);
            rowNum++;
            if (rowNum >= rows)
                break;
        }

        if (!state.getCaptions().isEmpty() && !state.getCaptions().getFirst().getValue().isEmpty()) {
            Element captionContainer = BBX.CONTAINER.CAPTION.create();
            captionContainer.insertChild(state.getCaptions().getFirst(), 0);
            newTable.insertChild(captionContainer, 0);
        }

        String tableType = tableTypeCombo.getItem(tableTypeCombo.getSelectionIndex()).toLowerCase();
        if (!tableType.equals(TableType.AUTO.displayName.toLowerCase()))
            newTable.addAttribute(new Attribute("format", tableType));
        Element parent = (Element) existingTable.getParent();
        parent.replaceChild(existingTable, newTable);

        if (!options.isEmpty() && tableTypes[tableTypeCombo.getSelectionIndex()] == TableType.SIMPLE) {
            options.forEach((o) -> {
                if (o == TableUtils.SimpleTableOptions.CUSTOM_WIDTHS) {
                    if (widths != null) {
                        TableUtils.applyCustomSimpleTableWidths(newTable, widths);
                    } else {
                        int[] existingWidths = TableUtils.getCustomSimpleTableWidths(existingTable);
                        if (existingWidths != null) {
                            TableUtils.applyCustomSimpleTableWidths(newTable, existingWidths);
                        }
                    }
                } else {
                    TableUtils.applySimpleTableOption(newTable, o);
                }
            });
        }

        if (state.getTNContainer() != null) {
            Element tnContainer = state.getTNContainer().copy();
            final Element transcribersNote = getTranscribersNote(newTable);
            if (transcribersNote != null) {
                transcribersNote.detach();
            }
            parent.insertChild(tnContainer, parent.indexOf(newTable));
            tnContainer.addAttribute(new Attribute("format", state.getType().toString().toLowerCase()));
            return new Node[]{tnContainer, newTable};
        } else {
            int index = parent.indexOf(newTable);
            if (index > 0 && BBX.CONTAINER.TABLETN.isA(parent.getChild(index - 1))) {
                parent.removeChild(index - 1);
            }
        }
        return new Node[]{newTable};
    }

    /*
     * If the final row/columns are empty, remove them from the table
     */
    private void trimTable(List<List<Node>> nodes) {
        //Trim rows
        List<Node> finalRow = nodes.getLast();
        boolean emptyRow = true;
        for (Node node : finalRow) {
            Node nodeCopy = node.copy();
            UTDHelper.stripUTDRecursive((Element) nodeCopy);
            if (!nodeCopy.getValue().isEmpty()) {
                emptyRow = false;
                break;
            }
        }
        if (emptyRow) {
            nodes.removeLast();
            if (nodes.isEmpty()) {
                //If that was the rest of the table, stop recursion
                return;
            }
            trimTable(nodes);
            return;
        }

        //Trim columns
        boolean emptyCol = true;
        for (List<Node> row : nodes) {
            Node finalNode = row.getLast().copy();
            UTDHelper.stripUTDRecursive((Element) finalNode);
            if (!finalNode.getValue().isEmpty()) {
                emptyCol = false;
                break;
            }
        }
        if (emptyCol) {
            for (List<Node> row : nodes) {
                row.removeLast();
            }
            if (!nodes.isEmpty()) {
                trimTable(nodes);
            }
        }
    }

    /**
     * Create a new facing table from two lists of text boxes
     */
    private Node[] generateNewFacingTable(List<CellText> leftBoxes, List<CellText> rightBoxes) {
        updateState(leftBoxes);
        updateState(rightBoxes);
        InternalFacingTable ftState = (InternalFacingTable) state;
        if (isFacingTable(tableNode)) {
            Element[] tables = getFacingTableParents(tableNode);
            generateNewTable(ftState.getLeftNodes(), ftState.split, ftState.getRows(), tables[0]);
            generateNewTable(ftState.getRightNodes(), ftState.getCols() - ftState.split, ftState.getRows(), tables[1]);
            return new Node[]{tableNode};
        } else {
            deleteExistingTable(tableNode);
            Element newParent = new Element("span");
            newParent.addAttribute(new Attribute("class", "facingTable"));
            newParent.appendChild(new Element("table"));
            newParent.appendChild(new Element("table"));
            tableNode.getParent().replaceChild(tableNode, newParent);
            tableNode = newParent;
            generateNewTable(ftState.getLeftNodes(), ftState.split, ftState.getRows(), (Element) newParent.getChild(0));
            generateNewTable(ftState.getRightNodes(), ftState.getCols() - ftState.split, ftState.getRows(), (Element) newParent.getChild(1));
            return new Node[]{newParent};
        }
    }

    /**
     * Check if a translated table is nearby, and if so delete it
     */
    private void deleteExistingTable(Element table) {
        Element parent = (Element) table.getParent();
        int index = parent.indexOf(table);
        if (index + 1 < parent.getChildCount() - 1) {
            Node neighbor = parent.getChild(index + 1);
            if (neighbor instanceof Element
                    && BBX.CONTAINER.TABLE.isA(neighbor)
                    && ((Element) neighbor).getAttribute("class") != null
                    && ((Element) neighbor).getAttributeValue("class").contains("utd:table")) {
                neighbor.detach();
            }
        }
    }

    /**
     * Destroy a table and give each element the given style
     *
     * @param rightBoxes If not a facing table, should be null
     */
    private void destroyTable(Style replacementStyle, List<CellText> textBoxes, List<CellText> rightBoxes) {
        List<Style> newList = new ArrayList<>();
        newList.add(replacementStyle);
        destroyTable(newList, textBoxes);
    }

    /**
     * Destroy a table and give each element the corresponding style in the list
     */
    private void destroyTable(List<Style> replacementStyle, List<CellText> textBoxes) {
        Element parent = (Element) tableNode.getParent();
        int index = parent.indexOf(tableNode);
        String namespace = parent.getNamespaceURI();
        if (index + 1 < parent.getChildCount() && parent.getChild(index + 1) instanceof Element adjElem) {
            if (adjElem.getAttribute("class") != null && adjElem.getAttributeValue("class").contains("utd:table")) {
                parent.removeChild(index + 1);
            }
        }
        parent.removeChild(index);
        int iterator = 0;
        CellText captionText = null;
        if (!textBoxes.isEmpty() && textBoxes.getFirst().isCaption())
            captionText = textBoxes.removeFirst();
        Map<Element, Style> newElements = new LinkedHashMap<>();
        for (CellText textBox : textBoxes) {
            Element newSpan;
            try {
                //TODO: Needs rewriting (Leon 3/9/17: Switched to applyStyle, is that what you wanted?
                newSpan = textBox.text.getXML(BBX.BLOCK.STYLE.create("DEFAULT"));
            } catch (Exception e) {
                Notify.showMessage("Exception encountered when pulling from text box: {}. Styles removed from text.\nException: {}", textBox.getWidget().getText(), e);
                newSpan = new Element("span", namespace);
                newSpan.appendChild(textBox.getWidget().getText());
            }
            parent.insertChild(newSpan, index + iterator);
            Style newStyle = replacementStyle.get(iterator >= replacementStyle.size() ? replacementStyle.size() - 1 : iterator);
            newElements.put(newSpan, newStyle);
            iterator++;
        }
        if (captionText != null) {
            Element caption = captionText.text.getXML(BBX.BLOCK.STYLE.create("Caption"));
            parent.insertChild(caption, index);
        }

        //Post process styles as applyStyle may move elements which breaks above indexing
        for (Map.Entry<Element, Style> entry : newElements.entrySet()) {
            Element elem = entry.getKey();
            Style style = entry.getValue();
            m.applyStyle(style, elem);
        }

        Shell loadingBox = new Shell(shell, SWT.NONE);
        loadingBox.setText("Loading");
        loadingBox.setLayout(new RowLayout(SWT.VERTICAL));
        Label loadingLabel = new Label(loadingBox, SWT.NONE);
        loadingLabel.setText("Please wait, reloading document");
        loadingBox.setLocation(shell.getLocation().x + (shell.getSize().x / 2), shell.getLocation().y + (shell.getSize().y / 2));
        loadingBox.pack();
        loadingBox.open();
        callback.accept(new Node[]{parent});
        loadingBox.close();
    }

    /**
     * Destroy a table, giving each cell its own style
     *
     * @param rightBoxes If not a facing table, should be null
     */
    private boolean advancedDestroy(List<CellText> textBoxes, List<CellText> rightBoxes) {
        UTDTranslationEngine engine = m.getEngine();
        List<Style> styles = engine.getStyleDefinitions().getStyles().stream()
                .filter((s) -> !s.getId().contains("internal")).toList();
        List<String[]> styleNames =
                styles.stream().map(s -> new String[]{banaStyles.get(s.getName())}).toList();
        //Really wish they were sorted. Comparators aren't cooperating though.
        List<Style> newStyles = new ArrayList<>();
        Pair<Integer, Integer> shellLoc = null;

        for (CellText text : textBoxes) {
            PickerDialog pd = new PickerDialog();
            pd.setHeadings(new String[]{"Style"});
            pd.setContents(styleNames);
            pd.setMessage("Select the style for cell:\n\n" + StringUtils.abbreviate(text.getWidget().getText(), 100));
            final Shell pdShell = pd.open(shell, (i) -> {
                if (i != -1) {
                    newStyles.add(styles.get(i));
                } else {
                    newStyles.add(null);
                }
            });
            if (shellLoc == null) {
                shellLoc = new Pair<>(pdShell.getLocation().x, pdShell.getLocation().y);
            } else {
                pdShell.setLocation(shellLoc.getFirst(), shellLoc.getSecond());
            }

            while (!pdShell.isDisposed()) {
                Display.getCurrent().readAndDispatch();
            }
            if (newStyles.isEmpty() || newStyles.getLast() == null) { //Cancel was pressed
                return false;
            }
        }
        destroyTable(newStyles, textBoxes);
        return true;
    }

    private void deleteTable() {
        final Element transcribersNote = getTranscribersNote(tableNode);
        if (transcribersNote != null) {
            transcribersNote.detach();
        }
        ParentNode tableParent = tableNode.getParent();
        int index = tableParent.indexOf(tableNode);
        if (tableParent.getChildCount() > index + 1
                && BBX.CONTAINER.TABLE.isA(tableParent.getChild(index + 1))
                && TableUtils.isTableCopy((Element) tableParent.getChild(index + 1))) {
            tableParent.removeChild(index + 1);
        }
        tableParent.removeChild(index);
        callback.accept(new Node[]{tableParent});
        closeShell();
    }

    private void swapNodes() {
        if (state.getNodes().isEmpty())
            return;
        List<List<Node>> newList = new ArrayList<>();
        for (int i = 0; i < state.getNodes().getFirst().size(); i++) {
            newList.add(new ArrayList<>());
        }
        int col = 0;
        while (col < state.getNodes().getFirst().size()) {
            for (int row = 0; row < state.getNodes().size(); row++) {
                newList.get(col).add(state.getNodes().get(row).get(col));
            }
            col++;
        }
        if (state instanceof InternalTable) {
            state = new InternalTable(newList, state.getCaptions(), state.getTNContainer(), state.getType());
        } else {
            state = new InternalFacingTable(newList, newList.getFirst().size() / 2, new ArrayList<>(), state.getType());
        }
    }

    /**
     * Open the picker dialog to select a style
     */
    private void selectStyle(Consumer<Style> onFinished) {
        UTDTranslationEngine engine = m.getEngine();
        List<Style> styles = engine.getStyleDefinitions().getStyles().stream()
                .filter((s) -> !s.getId().contains("internal")).toList();
        List<String[]> styleNames =
                styles.stream().map(s -> new String[]{banaStyles.get(s.getName())}).toList();

        PickerDialog pd = new PickerDialog();
        pd.setHeadings(new String[]{"Style"});
        pd.setContents(styleNames);
        pd.setMessage("Select the style that should be applied to each table cell:");
        pd.open(shell, (i) -> {
            if (i != -1) {
                onFinished.accept(styles.get(i));
            }
        });
    }

    private List<List<Node>> getTableCells(Element tableNode) {
        return _getTableCells(tableNode, new ArrayList<>());
    }

    private List<List<Node>> _getTableCells(Element tableNode, List<List<Node>> rows) {
        for (int i = 0; i < tableNode.getChildElements().size(); i++) {
            Element child = tableNode.getChildElements().get(i);
            if (BBX.BLOCK.TABLE_CELL.isA(child)) {
                rows.getLast().add(child);
            } else if (BBX.CONTAINER.TABLE_ROW.isA(child)) {
                rows.add(new ArrayList<>());
            }
            _getTableCells(child, rows);
        }
        return rows;
    }

    private Element getTranscribersNote(Element tableNode) {
        ParentNode parent = tableNode.getParent();
        int index = parent.indexOf(tableNode);
        if (index > 0) {
            Node sibling = parent.getChild(index - 1);
            if (BBX.CONTAINER.TABLETN.isA(sibling) && checkTNElement((Element) sibling)) {
                return (Element) sibling;
            }
        }
        return null;
    }

    private Element copyTNContainer(Element tnContainer) {
        if (tnContainer == null) {
            return null;
        }
        return copyTNContainer(tnContainer, getTableType(tnContainer.getAttributeValue("format")));
    }

    private Element copyTNContainer(Element tnContainer, TableType type) {
        if (tnContainer == null) {
            return null;
        }
        //Copy TN container
        Element containerCopy = tnContainer.copy();
        //Strip UTD
        UTDHelper.stripUTDRecursive(containerCopy);

        for (Node child : (Iterable<Node>)FastXPath.descendant(containerCopy)::iterator) {
            if (BBX.SPAN.OTHER.isA(child)) {
                if (type == TableType.LINEAR) {
                    //If linear, re-attach span tags as children of the container
                    child.detach();
                    containerCopy.appendChild(child);
                }
            }
        }

        if (type == TableType.LINEAR && containerCopy.getChildCount() > 1 && BBX.BLOCK.isA(containerCopy.getChild(1))) {
            //Linear tables should now have a (basically) empty block
            containerCopy.removeChild(1);
        }

        return containerCopy;
    }

    private @NonNull TableType getTableType(Element tableNode) {
        String attrValue = tableNode.getAttributeValue("format");
        return getTableType(attrValue);
    }

    private @NonNull TableType getTableType(String tableType) {
        if (tableType == null) {
            return TableType.UNSET;
        }
        tableType = tableType.toLowerCase();
        return switch (tableType) {
            case "simple" -> TableType.SIMPLE;
            case "listed" -> TableType.LISTED;
            case "linear" -> TableType.LINEAR;
            case "stairstep" -> TableType.STAIRSTEP;
            default -> TableType.UNSET;
        };
    }

    private boolean checkTNElement(Element tnElement) {
        if (tnElement.getChildCount() == 0) {
            return false;
        }
        for (int i = 0; i < tnElement.getChildCount(); i++) {
            if (!BBX.BLOCK.isA(tnElement.getChild(i))) {
                return false;
            }
        }
        return true;
    }

    private BBStyleableText getCurrentText(List<CellText> texts, List<CellText> rightTexts) {
        for (CellText text : texts) {
            if (text.getWidget().isFocusControl())
                return text.text;
        }
        if (rightTexts != null) {
            for (CellText text : rightTexts) {
                if (text.getWidget().isFocusControl())
                    return text.text;
            }
        }
        return null;
    }

    private boolean isFacingTable(Element parent) {
        return "facingTable".equals(parent.getAttributeValue("class"));
    }

    /**
     * Given a span tag, return the two tables that represent a facing table
     *
     * @param parent Span tag with a class of "facingTable"
     * @return [0] = Left page table
     * [1] = Right page table
     */
    private Element[] getFacingTableParents(Element parent) {
        Nodes query = parent.query("descendant::*[local-name()='table']");
        for (int i = query.size() - 1; i >= 0; i--) {
            if ("utd:tableSimple".equals(((Element) query.get(i)).getAttributeValue("class")))
                query.remove(i);
        }
        if (query.size() == 2) {
            return new Element[]{(Element) query.get(0), (Element) query.get(1)};
        }
        throw new IllegalArgumentException("Table is not facing");
    }

    private List<TableUtils.SimpleTableOptions> findOptions(Element table) {
        List<TableUtils.SimpleTableOptions> options = new ArrayList<>();
        if ("simple".equals(table.getAttributeValue("format"))) {
            if (TableUtils.hasSimpleTableOption(TableUtils.SimpleTableOptions.ONE_CELL_BETWEEN_COLUMNS, table))
                options.add(TableUtils.SimpleTableOptions.ONE_CELL_BETWEEN_COLUMNS);
            if (TableUtils.hasSimpleTableOption(TableUtils.SimpleTableOptions.ROW_HEADING_DISABLED, table))
                options.add(TableUtils.SimpleTableOptions.ROW_HEADING_DISABLED);
            if (TableUtils.hasSimpleTableOption(TableUtils.SimpleTableOptions.COLUMN_HEADING_DISABLED, table))
                options.add(TableUtils.SimpleTableOptions.COLUMN_HEADING_DISABLED);
            if (TableUtils.hasSimpleTableOption(TableUtils.SimpleTableOptions.GUIDE_DOTS_DISABLED, table))
                options.add(TableUtils.SimpleTableOptions.GUIDE_DOTS_DISABLED);
            if (TableUtils.hasSimpleTableOption(TableUtils.SimpleTableOptions.CUSTOM_WIDTHS, table))
                options.add(TableUtils.SimpleTableOptions.CUSTOM_WIDTHS);
        }
        return options;
    }

    private KeyAdapter debugListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if ((e.stateMask & SWT.MOD1) != 0 && (e.stateMask & SWT.MOD2) != 0 && e.keyCode == '`') {
                    debug = true;
                    rebuildView();
                }
            }
        };
    }

}
