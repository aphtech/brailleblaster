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
import org.brailleblaster.utd.properties.Align
import org.brailleblaster.utd.utils.PageBuilderHelper
import org.brailleblaster.utd.utils.TableUtils
import org.brailleblaster.utd.utils.getDescendantBrlFast

class StairstepTableFormatter : Formatter() {
    override fun format(
        node: Node,
        style: IStyle,
        pageBuilders: Set<PageBuilder>,
        formatSelector: FormatSelector
    ): MutableSet<PageBuilder> {
        val mutPageBuilders = pageBuilders.toMutableSet()
        if (node !is Element) return mutPageBuilders
        TableUtils.deleteExistingTable(node)
        val originalTable = TableUtils.copyTable(node)
        TableUtils.removeBrlBetweenCells(node, formatSelector.styleMap)
        TableUtils.createSignsOfOmission(formatSelector.engine, node, formatSelector.styleMap, symbol = "\u2810\u2810\u2810")
        var pageBuilder = mutPageBuilders.last()
        pageBuilder.alignment = Align.LEFT

        val caption = TableUtils.findCaption(node, formatSelector.styleMap)
        for (brl in caption) {
            pageBuilder.setFirstLineIndent(6)
                .setLeftIndent(4)
                .addAtLeastLinesBefore(1)
            mutPageBuilders.addAll(pageBuilder.addBrl(brl))
            pageBuilder = mutPageBuilders.last()
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
            val linesBefore = style.getLinesBefore(node, formatSelector.styleMap.namespaces)
            if (linesBefore != null) {
                pageBuilder.setMaxLines(linesBefore)
            } else {
                pageBuilder.addAtLeastLinesBefore(style.linesBefore)
            }
        }
        pageBuilder.addAtLeastPages(style.newPagesBefore)
        pageBuilder.setStartOfBlock(true)
        val cells = findCells(node, formatSelector.styleMap)
        var beginningOfPage = pageBuilder.y == 0 || pageBuilder.y == 1 && pageBuilder.runningHead.isNotEmpty()
        var i = 0
        outerLoop@ while (i < cells.size) {
            val row = cells[i]
            var indent = 0
            for (cell in row) {
                pageBuilder.addAtLeastLinesBefore(1)
                pageBuilder.setLeftIndent(indent)
                pageBuilder.setFirstLineIndent(indent)
                val brls = cell.getDescendantBrlFast()
                for (brl in brls) {
                    val size = mutPageBuilders.size
                    mutPageBuilders.addAll(pageBuilder.addBrl(brl))
                    if (mutPageBuilders.size > size && !beginningOfPage) {
                        removeRowFromPageBuilders(row, mutPageBuilders)
                        pageBuilder = mutPageBuilders.last()
                        pageBuilder.y = 0
                        pageBuilder.setStartOfBlock(true)
                        beginningOfPage = true
                        i--
                        i++
                        continue@outerLoop
                    }
                    pageBuilder = mutPageBuilders.last()
                }
                indent += 2
            }
            beginningOfPage = false
            i++
        }
        node.addAttribute(Attribute("class", "utd:tableStairstep"))
        node.parent.insertChild(originalTable, node.parent.indexOf(node))
        val newPos = pageBuilder.findLastBlankLine()
        pageBuilder.y = if (newPos >= pageBuilder.linesPerPage) pageBuilder.linesPerPage - 1 else newPos
        val linesAfter = style.getLinesAfter(node, formatSelector.styleMap.namespaces)
        if (linesAfter != null) {
            pageBuilder.setMaxLines(linesAfter)
        } else {
            pageBuilder.addAtLeastLinesAfter(style.linesAfter.coerceAtLeast(1))
        }
        pageBuilder.addAtLeastPages(style.newPagesAfter)
        return mutPageBuilders
    }

    private fun findCells(table: Element, styleMap: IStyleMap): List<List<Element>> = TableUtils.findRows(table, styleMap).map { TableUtils.findCols(it, styleMap) }

    private fun removeRowFromPageBuilders(row: List<Element>, pageBuilder: Set<PageBuilder>) {
        for (pb in pageBuilder) {
            for (element in row) {
                val brls = element.getDescendantBrlFast()
                for (i in brls) {
                    pb.removeBrl((i))
                }
            }
        }
    }
}
