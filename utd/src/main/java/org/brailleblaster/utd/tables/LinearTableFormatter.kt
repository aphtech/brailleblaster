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
package org.brailleblaster.utd.tables

import nu.xom.Attribute
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.FormatSelector
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.IStyleMap
import org.brailleblaster.utd.PageBuilder
import org.brailleblaster.utd.formatters.Formatter
import org.brailleblaster.utd.internal.elements.TableDivider
import org.brailleblaster.utd.properties.BrailleTableType
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.TableUtils
import org.brailleblaster.utd.utils.TextTranslator
import org.brailleblaster.utd.utils.getDescendantBrlFast
import kotlin.math.max

class LinearTableFormatter : Formatter() {
    override fun format(
        node: Node,
        style: IStyle,
        pageBuilders: Set<PageBuilder>,
        formatSelector: FormatSelector
    ): MutableSet<PageBuilder> {
        val mutPageBuilders = pageBuilders.toMutableSet()
        if (node !is Element) {
            return mutPageBuilders
        }
        var pageBuilder = mutPageBuilders.last()
        TableUtils.deleteExistingTable(node)
        val originalTable = TableUtils.copyTable(node)
        TableUtils.removeBrlBetweenCells(node, formatSelector.styleMap)
        TableUtils.createSignsOfOmission(formatSelector.engine, node, formatSelector.styleMap, symbol = "\u2810\u2810\u2810")
        val caption = TableUtils.findCaption(node, formatSelector.styleMap)
        for (brl in caption) {
            pageBuilder.setFirstLineIndent(6)
                .setLeftIndent(4)
                .addAtLeastLinesBefore(1)
            mutPageBuilders.addAll(pageBuilder.addBrl(brl))
            pageBuilder = mutPageBuilders.last()
        }
        val cells = fillCells(node, formatSelector.styleMap)
        val numOfCols = calculateNumOfCols(cells)
        if (cells.isEmpty()) return mutPageBuilders
        for (row in cells) {
            for (cell in row) {
                val col = row.indexOf(cell)
                cell.getDescendantBrlFast().lastOrNull()?.let { lastBrl ->
                    val newSpan = TableDivider()
                    val newBrl = UTDElements.BRL.create()
                    val dividerType = if (col == 0) {
                        TableDivider.DividerTypes.LINEAR_COLON
                    } else if (col != numOfCols - 1) {
                        TableDivider.DividerTypes.LINEAR_SEMICOLON
                    } else {
                        continue
                    }
                    newSpan.setType(dividerType)
                    val (idx, transText) = TextTranslator.translateIndexedText(newSpan.value, formatSelector.engine, tableType = BrailleTableType.UNCONTRACTED)
                    newBrl.appendChild(transText)
                    newBrl.addAttribute(Attribute("index", idx.joinToString(separator = " ")))
                    newSpan.appendChild(newBrl)
                    var parent = lastBrl.parent as Element
                    val type = parent.getAttributeValue("type", parent.getNamespaceURI("bb"))
                    if (type != null && type == "MATHML") {
                        //Rebecca : I don't know if this should be fixed here or in BB.  It didn't seem correct to 
                        //have the linear separator under an Inline MathML parent.
                        parent = parent.parent as Element
                    }
                    parent.appendChild(newSpan)
                }
            }
        }
        pageBuilder.setStartOfBlock(true)
        val linesBefore = style.getLinesBefore(node, formatSelector.styleMap.namespaces)
        if (linesBefore != null) {
            pageBuilder.setMaxLines(linesBefore)
        } else {
            pageBuilder.addAtLeastLinesBefore(style.linesBefore)
        }
        pageBuilder.addAtLeastPages(style.newPagesBefore)
        pageBuilder.setFirstLineIndent(0)
        pageBuilder.setLeftIndent(2)
        var beginningOfPage = pageBuilder.y == 0 || pageBuilder.y == 1 && pageBuilder.runningHead.isNotEmpty()
        var i = 0
        outerLoop@ while (i < cells.size) {
            val row = cells[i]
            for (cell in row) {
                val query = cell.getDescendantBrlFast()
                for (brl in query) {
                    val curPages = mutPageBuilders.size
                    mutPageBuilders.addAll(pageBuilder.addBrl(brl))
                    pageBuilder = mutPageBuilders.last()
                    if (mutPageBuilders.size > curPages && !beginningOfPage) {
                        removeRowFromPageBuilder(row, mutPageBuilders)
                        pageBuilder.x = 0
                        //If KeepWithNext brought something to the next page, the cursor will
                        //already be in the correct position
                        if (pageBuilder.isBlankPageWithPageNumbers) {
                            pageBuilder.y = 0
                        }
                        pageBuilder.setStartOfBlock(true)
                        i--
                        beginningOfPage = true
                        i++
                        continue@outerLoop
                    }
                }
            }
            pageBuilder.setStartOfBlock(true)
            pageBuilder.addAtLeastLinesBefore(1)
            beginningOfPage = false
            i++
        }
        node.addAttribute(Attribute("class", "utd:tableLinear"))
        node.parent.insertChild(originalTable, node.parent.indexOf(node))
        val newPos = pageBuilder.findLastBlankLine()
        pageBuilder.y = if (newPos >= pageBuilder.linesPerPage) pageBuilder.linesPerPage - 1 else newPos
        val linesAfter = style.getLinesAfter(node, formatSelector.styleMap.namespaces)
        if (linesAfter != null) {
            pageBuilder.setMaxLines(linesAfter)
        } else {
            pageBuilder.addAtLeastLinesAfter(max(style.linesAfter, 1))
        }
        pageBuilder.addAtLeastPages(style.newPagesAfter)
        return mutPageBuilders
    }

    private fun removeRowFromPageBuilder(row: List<Element>, pageBuilder: Set<PageBuilder>) {
        for (pb in pageBuilder) {
            for (element in row) {
                val brls = element.getDescendantBrlFast()
                for (i in brls) {
                    pb.removeBrl((i))
                }
            }
        }
    }

    private fun calculateNumOfCols(cells: List<List<Element>>): Int {
        return cells.maxOfOrNull { it.size } ?: 0
    }

    private fun fillCells(element: Element, iStyleMap: IStyleMap): List<List<Element>> {
        return TableUtils.findRows(element, iStyleMap).map { TableUtils.findCols(it, iStyleMap).toList() }
    }
}
