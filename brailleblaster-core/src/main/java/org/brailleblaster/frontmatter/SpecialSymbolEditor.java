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
package org.brailleblaster.frontmatter;

import org.brailleblaster.frontmatter.SpecialSymbols.AdvancedDetectionRule;
import org.brailleblaster.frontmatter.SpecialSymbols.DetectionRule;
import org.brailleblaster.frontmatter.SpecialSymbols.Symbol;
import org.brailleblaster.settings.UTDManager;
import org.brailleblaster.utils.swt.ButtonBuilder1;
import org.brailleblaster.utils.swt.CompositeBuilder;
import org.brailleblaster.utils.swt.EasyListeners;
import org.brailleblaster.utils.swt.EasySWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SpecialSymbolEditor {
    private final int BUTTON_WIDTH = 100;
    private final int TEXT_WIDTH = 400;
    private final int MAX_SHELL_WIDTH = 700;
    private final int MAX_PADDING = 100;
    private final int SHELL_WIDTH = Math.min(Display.getCurrent().getBounds().width - MAX_PADDING, MAX_SHELL_WIDTH);

    public SpecialSymbolEditor() {
    }

    /**
     * Read symbols from files and open the dialog
     */
    public void open(Shell parent, UTDManager m) {
        List<Symbol> symbols = SpecialSymbols.getSymbols();
        createDialog(parent, symbols);
    }

    private void createDialog(Shell parent, List<Symbol> map) {
        Shell dialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        dialog.setLayout(new GridLayout(1, false));
        dialog.setText("Special Symbols");
        EasySWT.makeLabel(dialog, "", SHELL_WIDTH, 1);
        Composite tableCont = EasySWT.makeComposite(dialog, 1);
        Table symbolsTable = new Table(tableCont, SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        symbolsTable.setLinesVisible(true);
        symbolsTable.setHeaderVisible(true);
        EasySWT.buildGridData().setHint(SHELL_WIDTH, 400).applyTo(symbolsTable);
        TableColumn symbolColumn = new TableColumn(symbolsTable, SWT.NONE);
        symbolColumn.setWidth(100);
        symbolColumn.setText("Symbol");
        TableColumn descColumn = new TableColumn(symbolsTable, SWT.NONE);
        descColumn.setWidth((SHELL_WIDTH - 100) / 2);
        descColumn.setText("Description");
        TableColumn rulesColumn = new TableColumn(symbolsTable, SWT.NONE);
        rulesColumn.setText("Rules");

        CompositeBuilder compositeBuilder = EasySWT.buildComposite(dialog);
        compositeBuilder.setColumns(3);
        compositeBuilder.setEqualColumnWidth(true);
        compositeBuilder.addButton("Add...", BUTTON_WIDTH, 1, (i) -> new SymbolDialog(dialog, symbolsTable).open(null));
        compositeBuilder.addButton("Edit...", BUTTON_WIDTH, 1, (i) -> {
            if (symbolsTable.getSelection().length > 0) {
                new SymbolDialog(dialog, symbolsTable).open(symbolsTable.getSelection()[0]);
            }
        });
        compositeBuilder.addButton("Delete", BUTTON_WIDTH, 1, (i) -> {
            if (symbolsTable.getSelection().length > 0) {
                symbolsTable.remove(symbolsTable.getSelectionIndex());
            }
        });
        compositeBuilder.build();
        compositeBuilder = EasySWT.buildComposite(dialog);
        compositeBuilder.setColumns(3);
        compositeBuilder.setEqualColumnWidth(true);
        compositeBuilder.addButton("Save", BUTTON_WIDTH, 1, (i) -> {
            SpecialSymbols.saveChanges(pullListFromTable(symbolsTable));
            dialog.close();
        });
        compositeBuilder.addButton("Cancel", BUTTON_WIDTH, 1, (i) -> dialog.close());
        compositeBuilder.addButton("Restore Default", BUTTON_WIDTH, 1, (i) -> {
            if (EasySWT.makeEasyYesNoDialog("Confirmation", "This will delete all custom special symbols. Are you sure?", dialog)) {
                SpecialSymbols.restoreDefault();
                symbolsTable.removeAll();
                fillTable(symbolsTable, SpecialSymbols.getSymbols());
            }

        });
        compositeBuilder.build();

        fillTable(symbolsTable, map);

        dialog.pack();
        dialog.open();
    }

    private void resizeRulesColumn(@NonNull TableColumn rulesColumn) {
        Table symbolsTable = rulesColumn.getParent();
        int longestRulesCol = (SHELL_WIDTH - 100) / 2;
        GC gc = new GC(symbolsTable);
        for (int i = 0; i < symbolsTable.getItemCount(); i++) {
            TableItem item = symbolsTable.getItem(i);
            longestRulesCol = Math.max(gc.textExtent(item.getText(2)).x + 15, longestRulesCol);
        }
        rulesColumn.setWidth(longestRulesCol);
    }

    /**
     * Read symbols from map and insert them into table
     */
private void fillTable(@NonNull Table symbolsTable, @NonNull List<@NonNull Symbol> symbols) {
        for (Symbol symbol : symbols) {
            String desc = symbol.getDesc();
            if (desc == null)
                continue;
            TableItem newItem = new TableItem(symbolsTable, SWT.NONE);
            newItem.setText(new String[]{symbol.getSymbol(), symbol.getDesc(), SpecialSymbols.rulesToString(symbol.getRules())});
        }
        resizeRulesColumn(symbolsTable.getColumn(2));
    }

    private class SymbolDialog {
        final Shell parent;
        final Table symbolsTable;

        public SymbolDialog(Shell parent, Table symbolsTable) {
            this.parent = parent;
            this.symbolsTable = symbolsTable;
        }

        void open(TableItem existingItem) {
            boolean editing = existingItem != null;

            Shell addDialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
            addDialog.setLayout(new RowLayout(SWT.VERTICAL));
            Composite container = EasySWT.makeComposite(addDialog, 2);

            EasySWT.makeLabel(container, "Symbol:", 1);
            Text symbolText = EasySWT.makeText(container, TEXT_WIDTH, 1);
            if (editing)
                symbolText.setText(existingItem.getText(0));

            EasySWT.makeLabel(container, "Description:", 1);
            Text descText = EasySWT.makeText(container, TEXT_WIDTH, 1);
            if (editing)
                descText.setText(existingItem.getText(1));
            Button editRulesButton = EasySWT.makePushButton(container, "Edit rules", BUTTON_WIDTH, 1, null);
            Label rulesLabel = EasySWT.makeLabel(container, "Rules: " + (existingItem == null || existingItem.getText(2).isEmpty() ? "None" : existingItem.getText(2)), 1);
            EasyListeners.selection(editRulesButton, (e) -> new EditRulesDialog().open(rulesLabel.getText().replaceAll("Rules: ", "").replaceAll("None", "").replaceAll(System.lineSeparator(), ""), s -> {
                rulesLabel.setText("Rules: " + (s.isEmpty() ? "None" : s));
                rulesLabel.pack();

                EasySWT.setLabelSizeToFit(rulesLabel, TEXT_WIDTH);
                addDialog.layout(true);
                addDialog.pack(true);
            }));
            EasySWT.buildGridData().setAlign(SWT.DEFAULT, SWT.CENTER).applyTo(rulesLabel);

            EasySWT.makePushButton(container, editing ? "Save" : "Add", BUTTON_WIDTH, 1, (e) -> {
                addSymbol(existingItem, symbolText.getText(), descText.getText(), rulesLabel.getText().replace("Rules: ", "").replace("None", "").replaceAll(System.lineSeparator(), ""));
                addDialog.close();
            });
            EasyListeners.keyPress(descText, (e) -> {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                    addSymbol(existingItem, symbolText.getText(), descText.getText(), rulesLabel.getText().replace("Rules: ", "").replace("None", "").replaceAll(System.lineSeparator(), ""));
                    addDialog.close();
                }
            });
            EasySWT.makePushButton(container, "Cancel", BUTTON_WIDTH, 1, (e) -> addDialog.close());

            addDialog.pack();
            addDialog.open();
        }

        private void addSymbol(TableItem existingItem, String symbol, String desc, String rules) {

            TableItem newItem = existingItem == null ? new TableItem(symbolsTable, SWT.NONE) : existingItem;
            newItem.setText(new String[]{symbol, desc, rules});
            resizeRulesColumn(symbolsTable.getColumn(2));
        }
    }

    private class EditRulesDialog {
        Shell parent;
        Shell dialog;

        public EditRulesDialog() {
        }

        void open(String existingRules, Consumer<String> callback) {
            List<DetectionRule> ruleList = existingRules == null || existingRules.isEmpty() ? new ArrayList<>() : SpecialSymbols.stringToRules(existingRules);
            dialog = new Shell(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
            dialog.setText("Edit Special Symbols Rules");
            dialog.setLayout(new GridLayout(1, false));


            Composite container = EasySWT.makeComposite(dialog, 3);
            Composite buttonPanel = EasySWT.makeComposite(dialog, 2); //The button panel and rules container are created out of order so that the okButton can be passed around
            Button okButton = EasySWT.makePushButton(buttonPanel, "Ok", BUTTON_WIDTH, 1, e -> {
                callback.accept(SpecialSymbols.rulesToString(ruleList));
                dialog.close();
            });
            EasySWT.makePushButton(buttonPanel, "Cancel", BUTTON_WIDTH, 1, e -> dialog.close());
            buildRuleView(container, ruleList, okButton);


            dialog.pack();
            dialog.open();
        }

        private void buildRuleView(Composite container, List<DetectionRule> ruleList, Button okButton) {
            for (int i = container.getChildren().length - 1; i >= 0; i--) {
                container.getChildren()[i].dispose();
            }
            for (int i = 0; i < ruleList.size(); i++) {
                displayRule(container, ruleList, i, okButton);
            }
            addRuleButton(container, ruleList, okButton);
            container.pack();
            dialog.pack();
        }

        private void displayRule(Composite container, List<DetectionRule> ruleList, int index, Button okButton) {
            DetectionRule rule = ruleList.get(index);

            Button deleteButton = EasySWT.makePushButton(container, "Delete", 1, e -> {
                ruleList.remove(index);
                buildRuleView(container, ruleList, okButton);
            });
            EasySWT.buildGridData().setAlign(SWT.LEFT, SWT.CENTER).applyTo(deleteButton);
            Label nameLabel = EasySWT.makeLabel(container, rule.getRuleName() + (rule instanceof AdvancedDetectionRule ? " " + ((AdvancedDetectionRule) rule).getOption() : ""), 1);
            EasySWT.buildGridData().setAlign(SWT.LEFT, SWT.CENTER).applyTo(nameLabel);
            Label alwaysLabel = EasySWT.makeLabel(container, "   Always? " + (rule.getAlways() ? "Yes" : "No"), 1);
            EasySWT.buildGridData().setAlign(SWT.LEFT, SWT.CENTER).applyTo(alwaysLabel);
        }

        private void addRuleButton(Composite container, List<DetectionRule> ruleList, Button okButton) {
            Composite newRuleContainer = EasySWT.makeComposite(container, 5);
            EasySWT.buildGridData().setColumns(3).applyTo(newRuleContainer);
            EasySWT.makePushButton(newRuleContainer, "Add new rule", 1, e -> {
                e.widget.dispose();
                Combo newCombo = new Combo(newRuleContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
                newCombo.setItems(SpecialSymbols.RULES);
                newCombo.select(0);
                EasySWT.makeLabel(newRuleContainer, "Option:", 1);
                Text optionText = EasySWT.makeText(newRuleContainer, 1);
                optionText.setEnabled(false);
                EasyListeners.selection(newCombo, f -> optionText.setEnabled(isAdvancedRule(newCombo)));
                ButtonBuilder1 buttonBuilder = EasySWT.buildPushButton(newRuleContainer);
                buttonBuilder.text = "Always";
                buttonBuilder.setSwtOptions(SWT.CHECK);
                Button alwaysCheck = buttonBuilder.build();
                EasySWT.makePushButton(newRuleContainer, "Add", 1, v -> {
                    if (newCombo.getSelectionIndex() != 0) {
                        if (isAdvancedRule(newCombo) && optionText.getText().isEmpty()) {
                            optionText.setFocus();
                            return;
                        }
                        DetectionRule newRule = SpecialSymbols.getEquivalentRule(newCombo.getText(), alwaysCheck.getSelection());
                        if (newRule instanceof AdvancedDetectionRule) {
                            ((AdvancedDetectionRule) newRule).setOption(optionText.getText());
                        }
                        ruleList.add(newRule);
                    }
                    okButton.setEnabled(true);
                    buildRuleView(container, ruleList, okButton);
                });
                okButton.setEnabled(false);
                dialog.pack(true);
                container.layout(true);
                newRuleContainer.layout(true);
            });
        }

        private boolean isAdvancedRule(Combo combo) {
            return combo.getSelectionIndex() >= combo.getItemCount() - 4;
        }
    }

    /**
     * Read symbols from table and store them in a map
     */
    private List<Symbol> pullListFromTable(Table symbolsTable) {
        List<Symbol> list = new ArrayList<>();
        for (int i = 0; i < symbolsTable.getItemCount(); i++) {
            TableItem curItem = symbolsTable.getItem(i);
            list.add(new SpecialSymbols.Symbol(curItem.getText(0), curItem.getText(1), SpecialSymbols.stringToRules(curItem.getText(2))));
        }
        return list;
    }
}
