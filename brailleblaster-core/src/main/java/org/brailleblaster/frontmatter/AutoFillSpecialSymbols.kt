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
package org.brailleblaster.frontmatter

import nu.xom.Document
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.apache.commons.lang3.StringUtils
import org.brailleblaster.bbx.BBX
import org.brailleblaster.frontmatter.SpecialSymbols.SymbolComparator
import org.brailleblaster.settings.UTDManager
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyleRange
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*

class AutoFillSpecialSymbols(
    val doc: Document, val m: UTDManager,
    /**
     * List of special symbols by volume
     */
    val callback: (List<List<SpecialSymbols.Symbol>>) -> Unit
) {
    fun openDialog(parent: Shell, curVolume: Int) {
        val dialog = Shell(parent, SWT.APPLICATION_MODAL or SWT.DIALOG_TRIM)
        dialog.layout = GridLayout(1, false)
        val container = Composite(dialog, SWT.NONE)
        container.layout = GridLayout(1, false)
        dialog.text = "Special Symbol Locator"
        val warningLabel = EasySWT.makeLabel(
            container,
            "Warning: Any symbols manually added to T-Pages before running this process will be overwritten when this process is ran.",
            1
        )
        EasySWT.setLabelSizeToFit(warningLabel, TEXT_WIDTH)
        val radioContainer = Composite(container, SWT.NONE)
        radioContainer.layout = GridLayout(1, false)
        val radio1 = Button(radioContainer, SWT.RADIO)
        radio1.text = "Volume " + (curVolume + 1) + " only"
        radio1.selection = false
        val radio2 = Button(radioContainer, SWT.RADIO)
        radio2.text = "All volumes"
        radio2.selection = true

        val buttonPanel = Composite(container, SWT.NONE)
        buttonPanel.layout = GridLayout(2, false)
        val beginButton = EasySWT.makePushButton(buttonPanel, "Begin", BUTTON_WIDTH, 1, null)
        val cancelButton =
            EasySWT.makePushButton(buttonPanel, "Cancel", BUTTON_WIDTH, 1) { dialog.close() }

        val statusText = StyledText(container, SWT.BORDER or SWT.V_SCROLL)
        statusText.editable = false
        EasySWT.buildGridData().setHint(TEXT_WIDTH, TEXT_HEIGHT).setGrabSpace(horizontally = true, vertically = true).applyTo(statusText)
        val simBraille = Font(Display.getCurrent(), "SimBraille", statusText.font.fontData[0].getHeight(), SWT.NORMAL)

        val onFind = { symbol: String, location: String ->
            var curLength = statusText.text.length
            statusText.append(FOUND_SYMBOL_MESSAGE_1 + symbol + FOUND_SYMBOL_MESSAGE_2 + location + "\n")
            curLength += FOUND_SYMBOL_MESSAGE_1.length

            statusText.setStyleRange(createFontRange(curLength, symbol.length, simBraille))
            curLength += symbol.length + FOUND_SYMBOL_MESSAGE_2.length
            statusText.setStyleRange(createFontRange(curLength, location.length, simBraille))

            statusText.setSelection(statusText.text.length - 1)
            val display = Display.getCurrent()
            while (display.readAndDispatch()) {
                display.sleep()
            }
        }

        val onMessage = { s: String ->
            statusText.append(s + "\n")
            statusText.setSelection(statusText.text.length - 1)
            val display = Display.getCurrent()
            while (display.readAndDispatch()) {
                display.sleep()
            }
        }

        beginButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                @Suppress("UNCHECKED_CAST")
                val data = beginButton.data as? List<List<SpecialSymbols.Symbol>>
                if (data == null) { //This is janky
                    cancelButton.isEnabled = false
                    beginButton.isEnabled = false
                    beginButton.data = beginAutoFill(onFind, onMessage, if (radio1.selection) curVolume else -1)
                    beginButton.isEnabled = true
                    cancelButton.isEnabled = true
                    beginButton.text = "Continue"
                } else {
                    verifyResults(parent, data)
                    dialog.close()
                }
            }
        })
        dialog.open()
        dialog.pack()
    }

    private fun verifyResults(parent: Shell, results: List<List<SpecialSymbols.Symbol>>) {
        val dialog = Shell(parent, SWT.APPLICATION_MODAL or SWT.DIALOG_TRIM)
        dialog.layout = GridLayout(1, false)
        val container = EasySWT.makeComposite(dialog, 1)

        val description = Label(container, SWT.NONE)
        val tableCont = EasySWT.makeComposite(dialog, 1)
        val resultsTable = Table(tableCont, SWT.V_SCROLL or SWT.BORDER or SWT.FULL_SELECTION)
        resultsTable.linesVisible = true
        resultsTable.headerVisible = true
        EasySWT.buildGridData().setHint(TABLE_WIDTH, TABLE_HEIGHT).applyTo(resultsTable)

        val buttonPanel = EasySWT.makeComposite(tableCont, 1)
        EasySWT.buildGridData().setGrabSpace(horizontally = true, vertically = false).applyTo(buttonPanel)
        description.text = "The following special symbols were found:"
        if (results.isEmpty()) {
            val emptyColumn = TableColumn(resultsTable, SWT.NONE)
            emptyColumn.text = "No results found"
            emptyColumn.width = TABLE_WIDTH
        } else {
            val symbolColumn = TableColumn(resultsTable, SWT.NONE)
            symbolColumn.text = "Symbol"
            symbolColumn.width = TABLE_WIDTH / 2 - 100
            val descColumn = TableColumn(resultsTable, SWT.NONE)
            descColumn.text = "Description"
            descColumn.width = TABLE_WIDTH / 2 + 100
            var vol = 1
            val symbolDatabase = SpecialSymbols.getSymbols()
            for (volume in results) {
                if (volume.isNotEmpty()) {
                    val headingItem = TableItem(resultsTable, SWT.NONE)
                    headingItem.setText(arrayOf("Volume $vol", ""))
                    for (symbol in volume) {
                        val storedSymbol = symbolDatabase[symbolDatabase.indexOf(symbol)]
                        val newItem = TableItem(resultsTable, SWT.NONE)
                        val symbolName = storedSymbol.symbol
                        val symbolDesc = storedSymbol.desc ?: ""
                        newItem.setText(
                            arrayOf(
                                symbolName,
                                symbolDesc,
                                SpecialSymbols.rulesToString(storedSymbol.rules)
                            )
                        )
                    }
                }
                vol++
            }
        }

        val okCancelPanel = EasySWT.buildComposite(buttonPanel)
            .addButton("Finish", BUTTON_WIDTH, 1) { finish(dialog, results) }
            .addButton("Cancel", BUTTON_WIDTH, 1) { dialog.close() }
            .build()
        EasySWT.buildGridData().setAlign(SWT.RIGHT, null).setGrabSpace(horizontally = true, vertically = false).applyTo(okCancelPanel)
        dialog.open()
        dialog.pack(true)
    }

    private fun beginAutoFill(
        onFind: (String, String) -> Unit,
        onMessage: (String) -> Unit,
        volume: Int
    ): List<List<SpecialSymbols.Symbol>> {
        onMessage("Loading local symbol definitions")
        val symbols = SpecialSymbols.getSymbols()

        onMessage("Begin special symbol search")
        val found = searchSymbols(onFind, onMessage, volume, symbols)
        for (symbolList in found) {
            symbolList.sortWith(SymbolComparator())
        }
        return found
    }

    private class VolumeTracker {
        var volume: Int = 0
        var allVolumes: Boolean = true

        fun increaseVolume() {
            volume++
        }
    }

    private fun searchSymbols(
        onFind: (String, String) -> Unit,
        onMessage: (String) -> Unit,
        volume: Int,
        symbols: List<SpecialSymbols.Symbol>
    ): List<MutableList<SpecialSymbols.Symbol>> {
        val volumes: MutableList<MutableList<SpecialSymbols.Symbol>> = ArrayList()
        val foundSymbols: MutableList<SpecialSymbols.Symbol> = ArrayList()
        val localSymbols: MutableList<SpecialSymbols.Symbol> = ArrayList(symbols)
        val vt = VolumeTracker()
        vt.allVolumes = volume == -1

        val onBrl = { brlElement: Element ->
            for (i in 0 until brlElement.childCount) {
                if (!vt.allVolumes && vt.volume != volume) {
                    break
                }
                if (brlElement.getChild(i) !is Text) continue
                val brl = brlElement.getChild(i).value
                val iter = localSymbols.iterator()
                while (iter.hasNext()) {
                    val symbol = iter.next()
                    val symbolPermutations = SpecialSymbols.getSymbolPermutations(symbol.symbol)
                    symbolPermutations.add(symbol.symbol)
                    var symbolIter = 0
                    permLoop@ for (symbolPerm in symbolPermutations) {
                        var iteration = 0
                        innerLoop@ //This loop is to catch multiple instances of a symbol in one brl
                        while (symbolIter < brl.length && brl.indexOf(symbolPerm, symbolIter) != -1) {
                            var optionalTests = 0
                            var optionalTestsPassed = 0
                            for (rule in symbol.rules) {
                                if (!rule.always) optionalTests++
                                val result = rule.test(brlElement, i, symbol.symbol, iteration, m)
                                if (!result && rule.always) {
                                    symbolIter = brl.indexOf(symbolPerm, symbolIter) + 1
                                    iteration++
                                    continue@innerLoop
                                }
                                if (result && !rule.always) optionalTestsPassed++
                            }
                            if (optionalTests > 0 && optionalTestsPassed == 0) {
                                symbolIter = brl.indexOf(symbolPerm, symbolIter) + 1
                                iteration++
                                continue
                            }
                            onFind(symbolPerm, StringUtils.abbreviate(brl, brl.indexOf(symbolPerm), 30))
                            foundSymbols.add(symbol)
                            iter.remove()
                            break@permLoop
                        }
                    }
                }
            }
            localSymbols
        }

        val onVolume = {
            vt.increaseVolume()
            volumes.add(ArrayList(foundSymbols))
            if (vt.allVolumes) onMessage("Finished volume " + volumes.size)
            foundSymbols.clear()
            localSymbols.clear()
            localSymbols.addAll(symbols)
            localSymbols
        }

        iterateThruXML(onBrl, onVolume, localSymbols, doc)

        if (volumes.isEmpty()) {
            //If book has no volumes, onVolume never runs
            onVolume()
        }

        onMessage("Completed special symbol search")

        return volumes
    }

    private fun iterateThruXML(
        onBrl: (Element) -> List<SpecialSymbols.Symbol>,
        onVolume: () -> List<SpecialSymbols.Symbol>,
        listOfSymbols: List<SpecialSymbols.Symbol>,
        node: Node
    ) {
        var symbols = listOfSymbols
        var inVolumeEndBlock = false
        if (node is Element) {
            if (UTDElements.BRL.isA(node)) {
                symbols = onBrl(node)
            } else if (BBX.BLOCK.VOLUME_END.isA(node)) {
                inVolumeEndBlock = true
            }
        }
        for (i in 0 until node.childCount) {
            iterateThruXML(onBrl, onVolume, symbols, node.getChild(i))
        }
        if (inVolumeEndBlock) //Finish processing the end of volume block before marking the end of a volume
            onVolume()
    }

    private fun finish(dialog: Shell, data: List<List<SpecialSymbols.Symbol>>) {
        callback(data)
        dialog.close()
    }

    private fun createFontRange(start: Int, length: Int, font: Font): StyleRange {
        val newSR = StyleRange()
        newSR.font = font
        newSR.start = start
        newSR.length = length
        return newSR
    }

    companion object {
        const val BUTTON_WIDTH: Int = 100
        const val TEXT_WIDTH: Int = 400
        const val TEXT_HEIGHT: Int = 300
        const val TABLE_WIDTH: Int = 500
        const val TABLE_HEIGHT: Int = 500

        private const val FOUND_SYMBOL_MESSAGE_1 = "Found symbol: "
        private const val FOUND_SYMBOL_MESSAGE_2 = " in "
    }
}
