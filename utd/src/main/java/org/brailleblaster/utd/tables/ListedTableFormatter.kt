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
import org.brailleblaster.utd.formatters.LiteraryFormatter
import org.brailleblaster.utd.internal.elements.TableDivider
import org.brailleblaster.utd.properties.Align
import org.brailleblaster.utd.properties.BrailleTableType
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.PageBuilderHelper
import org.brailleblaster.utd.utils.TableUtils
import org.brailleblaster.utd.utils.TextTranslator
import org.brailleblaster.utils.xml.UTD_NS
import org.brailleblaster.utils.xom.childNodes
import java.util.*
import kotlin.math.max

class ListedTableFormatter : Formatter() {
    data class TableCell internal constructor(var td: Element, var row: Int, var col: Int)

    private val xpathContext = UTDElements.UTD_XPATH_CONTEXT
    override fun format(
        node: Node, style: IStyle, pageBuilders: Set<PageBuilder>,
        formatSelector: FormatSelector
    ): MutableSet<PageBuilder> {
        val mutPageBuilders = pageBuilders.toMutableSet()
        if (node !is Element) {
            return mutPageBuilders
        }
        TableUtils.deleteExistingTable(node)
        val originalTable = TableUtils.copyTable(node)
        TableUtils.removeBrlBetweenCells(node, formatSelector.styleMap)
        TableUtils.createSignsOfOmission(formatSelector.engine, node, formatSelector.styleMap, symbol = "\u2810\u2810\u2810")
        val cells = fillCells(node, formatSelector.styleMap)
        var pageBuilder = mutPageBuilders.last()
        pageBuilder.alignment = Align.LEFT
        val headings = cells.firstOrNull() ?: emptyList()
        if (headings.isNotEmpty()) {
            // only insert the caption when actually can be laid out as listed table
            // Otherwise the caption is inserted as part of the literary formatting.
            val caption = TableUtils.findCaptionBrl(node, formatSelector.styleMap)
            for (brl in caption) {
                pageBuilder.setFirstLineIndent(6)
                    .setLeftIndent(4)
                    .addAtLeastLinesBefore(1)
                mutPageBuilders.addAll(pageBuilder.addBrl(brl))
                pageBuilder = mutPageBuilders.last()
            }
        }
        val pageNumbers = formatSelector.engine.pageSettings
        val page = pageBuilder.braillePageNumber.pageNumber
        val skipTop = ((PageBuilderHelper.getPrintPageNumberAt(pageNumbers, page).isTop
                && PageBuilderHelper.getPrintPageNumberAt(pageNumbers, page).isLeft)
                || (PageBuilderHelper.getBraillePageNumberAt(pageNumbers, page).isTop
                && PageBuilderHelper.getBraillePageNumberAt(pageNumbers, page).isLeft))
        if (pageBuilder.y == 0 && skipTop) {
            pageBuilder.y = 1
        } else {
            val linesBefore = style.getLinesBefore(
                node,
                formatSelector.styleMap.namespaces
            )
            if (linesBefore != null) {
                pageBuilder.setMaxLines(linesBefore)
            } else {
                pageBuilder.addAtLeastLinesBefore(style.linesBefore)
            }
        }
        pageBuilder.addAtLeastPages(style.newPagesBefore)

        // Single-row tables don't make sense as listed tables
        if (cells.size <= 1 || headings.isEmpty()) {
            return LiteraryFormatter().format(node, style, mutPageBuilders, formatSelector)
        }
        for (cell in headings) {
            val colon: Element = TableDivider(TableDivider.DividerTypes.LISTED_COLON)
            if (cell.td.value.isEmpty()) {
                colon.removeChildren()
            } else {
                val newBrl = UTDElements.BRL.create()
                val (idx, sepBrl) = TextTranslator.translateIndexedText(": ", formatSelector.engine, tableType = BrailleTableType.UNCONTRACTED)
                newBrl.addAttribute(Attribute("index", idx.joinToString(separator = " ")))
                newBrl.appendChild(sepBrl)
                colon.appendChild(newBrl)
            }
            cell.td.appendChild(colon)
            cell.td.detach()
        }
        TableUtils.findRows(node, formatSelector.styleMap).first().detach()
        attachHeadings(headings, cells)
        var rowStart: Int
        for (row in cells.drop(1)) {
            val addedCells: MutableList<Element> = LinkedList()
            rowStart = pageBuilder.y
            pageBuilder.addAtLeastLinesBefore(2)
            var j = 0
            columnLoop@ while (j < row.size) {
                val curCell = row[j]
                val curCol: Int = curCell.col
                if (curCol == 0) {
                    pageBuilder.setLeftIndent(4)
                        .setFirstLineIndent(4)
                } else {
                    pageBuilder.setFirstLineIndent(0)
                        .setLeftIndent(2)
                }
                pageBuilder.setStartOfBlock(true)
                pageBuilder.addAtLeastLinesBefore(1)
                pageBuilder.setRightIndent(0)
                pageBuilder.x = pageBuilder.firstLineIndent
                val brls = curCell.td.query("descendant::utd:brl", xpathContext)
                for (brl in brls.filter { it.value.isNotEmpty() }) {
                    val curPages = mutPageBuilders.size
                    mutPageBuilders.addAll(pageBuilder.addBrl((brl as Element)))
                    pageBuilder = mutPageBuilders.last()
                    addedCells.add(brl)
                    if (mutPageBuilders.size == curPages + 1 && rowStart != 0) {
                        for (addedCell in addedCells) {
                            for (pb in mutPageBuilders) {
                                pb.removeBrl(addedCell)
                            }
                        }
                        pageBuilder = mutPageBuilders.last()
                        pageBuilder.y = pageBuilder.findLastBlankLine()
                        if (skipTop && pageBuilder.y == 0) pageBuilder.y = 1
                        rowStart = 0
                        j = 0 //Restart the loop
                        continue@columnLoop
                    } else if (mutPageBuilders.size > curPages + 1) rowStart = 0
                }
                j++
            }
        }
        node.addAttribute(Attribute(CLASS_ATTRIB_NAME, CLASS_ATTRIB_VALUE))
        if (TableUtils.FALLBACK_ATTRIB_VALUE == node.getAttributeValue(
                TableUtils.FALLBACK_ATTRIB_NAME,
                UTD_NS
            )
        ) {
            node.addAttribute(
                Attribute(
                    UTDElements.UTD_PREFIX + ":" + TableUtils.FALLBACK_ATTRIB_NAME,
                    UTD_NS,
                    TableUtils.FALLBACK_ATTRIB_VALUE
                )
            )
        }
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

    private fun attachHeadings(headings: List<TableCell>, cells: List<List<TableCell>>) {
        cells.drop(1).flatten().forEach { curCell ->
            val heading = headings[curCell.col.coerceIn(headings.indices)].td.copy()
            insertChildren(heading, curCell.td)
        }
    }

    private fun insertChildren(oldParent: Element, newParent: Element, index: Int = 0) {
        oldParent.childNodes.reversed().forEach { child ->
            child.detach()
            newParent.insertChild(child, index)
        }
    }

    companion object {
        private const val CLASS_ATTRIB_NAME = "class"
        private const val CLASS_ATTRIB_VALUE = "utd:tableListed"

        private fun fillCells(element: Element, iStyleMap: IStyleMap): List<List<TableCell>> = TableUtils.findRows(element, iStyleMap).mapIndexed { rowIndex, row ->
                TableUtils.findCols(row, iStyleMap).mapIndexed { colIndex, col ->
                    TableCell(col, rowIndex, colIndex)
                }
            }

    }
}
