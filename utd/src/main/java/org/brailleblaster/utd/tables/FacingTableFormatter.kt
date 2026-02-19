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
import org.brailleblaster.utd.PageBuilder
import org.brailleblaster.utd.exceptions.BadSimpleTableException
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.PageBuilderHelper
import org.brailleblaster.utd.utils.TableUtils.copyTable
import org.brailleblaster.utd.utils.TableUtils.deleteExistingTable
import org.brailleblaster.utd.utils.containsBrl
import java.util.*
import java.util.function.Consumer
import kotlin.math.max

class FacingTableFormatter : SimpleTableFormatter() {
    override fun format(
        node: Node,
        style: IStyle,
        pageBuilders: Set<PageBuilder>,
        formatSelector: FormatSelector
    ): MutableSet<PageBuilder> {
        val mutPageBuilders = pageBuilders.toMutableSet()
        if (node !is Element || node.getChildCount() == 0) return mutPageBuilders
        deleteExistingTable(node)
        val startingPBSize = mutPageBuilders.size
        val cellType = formatSelector.engine.brailleSettings.cellType
        if (node.childElements.size() == 0) return mutPageBuilders
        var leftTable: Element? = null
        var rightTable: Element? = null
        for (i in 0 until node.childElements.size()) {
            val child: Element = node.childElements[i]
            if ("utd:tableSimple" == child.getAttributeValue("class")) {
                child.addAttribute(Attribute("class", "utd:reformat"))
            }
            if (child.containsBrl()) {
                if (leftTable == null) leftTable = child
                else rightTable = child
            }
        }
        //Make preparations for both left and right page tables
        prepareTable(requireNotNull(leftTable), formatSelector)
        if (node.childElements.size() == 1 || rightTable == null) return super.format(
            leftTable,
            style,
            mutPageBuilders,
            formatSelector
        )
        prepareTable(rightTable, formatSelector)
        leftTable.addAttribute(Attribute("format", "simple"))
        rightTable.addAttribute(Attribute("format", "simple"))
        val leftOriginalTable = copyTable(leftTable)
        val rightOriginalTable = copyTable(rightTable)
        node.insertChild(leftOriginalTable, node.indexOf(leftTable))
        node.insertChild(rightOriginalTable, node.indexOf(rightTable))
        leftTable.addAttribute(Attribute("class", "utd:tableSimple"))
        rightTable.addAttribute(Attribute("class", "utd:tableSimple"))

        //TODO: Sidebars

        //Set up a second PageBuilder and make sure they are aligned with each other
        mutPageBuilders.addAll(preFormat(leftTable, style, mutPageBuilders, formatSelector))
        val pageBuilder = mutPageBuilders.last()
        PageBuilderHelper.verifyPageSide(pageBuilder, "left")
        mutPageBuilders.addAll(pageBuilder.processSpacing())
        var leftPage = mutPageBuilders.last()
        val startingY = leftPage.y
        leftPage.setNonsequentialPages(true)

        var rightPage = addNewPage(leftPage, mutPageBuilders)
        rightPage.setNonsequentialPages(true)
        leftPage.y = startingY
        leftPage.x = 0
        rightPage.y = startingY
        rightPage.x = 0

        //Find table cells and determine the size of the table
        val leftCells = findCells(leftTable, formatSelector.styleMap)
        val leftCols = calculateNumOfCols(leftCells)
        val leftWidths = calculateColumnWidths(
            leftCells,
            cellType.getCellsForWidth(formatSelector.engine.pageSettings.drawableWidth.toBigDecimal()),
            leftCols,
            true
        )
        addSeparationLines(leftCells[0], formatSelector, leftWidths)

        val rightCells = findCells(rightTable, formatSelector.styleMap)
        val rightCols = calculateNumOfCols(rightCells)
        val rightWidths = calculateColumnWidths(
            rightCells,
            cellType.getCellsForWidth(formatSelector.engine.pageSettings.drawableWidth.toBigDecimal()),
            rightCols,
            false
        )
        addSeparationLines(rightCells[0], formatSelector, rightWidths)

        //Add headings for both tables
        var leftHeadingY = startingY //-1 when properly aligned
        var rightHeadingY = startingY //-1 when properly aligned
        var beginningOfPage = leftPage.y == 0 || (leftPage.y == 1 && leftPage.runningHead.isNotEmpty())
        while (true) {
            val startingSize = mutPageBuilders.size
            try {
                if (leftHeadingY != -1) addHeadings(mutPageBuilders, leftPage, leftCells[0], leftWidths, 1, leftHeadingY)
            } catch (_: BadSimpleTableException) {
                return handleBadFacingTable(
                    leftTable,
                    rightTable,
                    style,
                    mutPageBuilders,
                    formatSelector,
                    leftCells,
                    rightCells,
                    startingPBSize,
                    leftOriginalTable,
                    rightOriginalTable
                )
            }
            if (leftPage != mutPageBuilders.elementAt(mutPageBuilders.size - 2)) {
                if (beginningOfPage) {
                    return handleBadFacingTable(
                        leftTable,
                        rightTable,
                        style,
                        mutPageBuilders,
                        formatSelector,
                        leftCells,
                        rightCells,
                        startingPBSize,
                        leftOriginalTable,
                        rightOriginalTable
                    )
                }
                while (mutPageBuilders.size > startingSize) {
                    mutPageBuilders.remove(mutPageBuilders.last())
                }
                removeCellsFromPageBuilders(leftCells[0], rightCells[0], mutPageBuilders)

                leftPage = addNewPage(rightPage, mutPageBuilders)
                rightPage = addNewPage(leftPage, mutPageBuilders)

                leftHeadingY = 0
                rightHeadingY = 0
                leftPage.y = 0
                rightPage.y = 0
                beginningOfPage = true
                continue
            }
            try {
                if (rightHeadingY != -1) addHeadings(
                    mutPageBuilders,
                    rightPage,
                    rightCells[0],
                    rightWidths,
                    1,
                    rightHeadingY
                )
            } catch (_: BadSimpleTableException) {
                return handleBadFacingTable(
                    leftTable,
                    rightTable,
                    style,
                    mutPageBuilders,
                    formatSelector,
                    leftCells,
                    rightCells,
                    startingPBSize,
                    leftOriginalTable,
                    rightOriginalTable
                )
            }
            if (rightPage != mutPageBuilders.last()) {
                if (beginningOfPage) {
                    return handleBadFacingTable(
                        leftTable,
                        rightTable,
                        style,
                        mutPageBuilders,
                        formatSelector,
                        leftCells,
                        rightCells,
                        startingPBSize,
                        leftOriginalTable,
                        rightOriginalTable
                    )
                }
                while (mutPageBuilders.size > startingSize) {
                    mutPageBuilders.remove(mutPageBuilders.last())
                }
                removeCellsFromPageBuilders(leftCells[0], rightCells[0], mutPageBuilders)
                leftPage = addNewPage(rightPage, mutPageBuilders)
                rightPage = addNewPage(leftPage, mutPageBuilders)

                leftHeadingY = 0
                rightHeadingY = 0
                leftPage.y = 0
                rightPage.y = 0
                beginningOfPage = true
                continue
            }
            if (leftPage.y == rightPage.y) { //Headings are in alignment
                break
            } else if (leftPage.y < rightPage.y) { // Left table needs to be pushed down
                rightHeadingY = -1
                for (cell in leftCells[0]) {
                    cell.y = null
                    cell.removeFromPageBuilder(leftPage)
                }
                leftHeadingY++
                leftPage.y = leftHeadingY
            } else { // Right table needs to be pushed down
                leftHeadingY = -1
                for (cell in rightCells[0]) {
                    cell.y = null
                    cell.removeFromPageBuilder(rightPage)
                }
                rightHeadingY++
                rightPage.y = rightHeadingY
            }
        }

        leftPage.addAtLeastLinesBefore(1)
        leftPage.processSpacing()
        rightPage.addAtLeastLinesBefore(1)
        rightPage.processSpacing()

        //Add rows
        for (curRow in 1 until leftCells.size) {
            val lastRow = curRow + 1 == leftCells.size
            val leftRow = leftCells[curRow]
            val rightRow = (if (curRow < rightCells.size) rightCells[curRow] else ArrayList())
            try {
                mutPageBuilders.addAll(
                    addRow(
                        mutPageBuilders,
                        leftPage,
                        formatSelector,
                        leftRow,
                        1,
                        leftCols,
                        leftWidths,
                        hasHeading = true,
                        hasEndingGuideDots = true,
                        lastRow = lastRow
                    )
                )
            } catch (_: BadSimpleTableException) {
                return handleBadFacingTable(
                    leftTable,
                    rightTable,
                    style,
                    mutPageBuilders,
                    formatSelector,
                    leftCells,
                    rightCells,
                    startingPBSize,
                    leftOriginalTable,
                    rightOriginalTable
                )
            }
            //Check for new page
            if (mutPageBuilders.last() != rightPage) {
                //Delete the new page that was created. It was based on the incorrect page numbers
                mutPageBuilders.remove(mutPageBuilders.last())
                leftRow.forEach(Consumer { tc: TableCell -> tc.removeFromPageBuilders(mutPageBuilders) })
                rightRow.forEach(Consumer { tc: TableCell -> tc.removeFromPageBuilders(mutPageBuilders) })
                rightPage.addAtLeastPages(1)
                mutPageBuilders.addAll(rightPage.processSpacing())
                leftPage = mutPageBuilders.last()
                leftPage.setNonsequentialPages(true)
                leftPage.x = 0
                leftPage.y = 1
                try {
                    mutPageBuilders.addAll(
                        addRow(
                            mutPageBuilders,
                            leftPage,
                            formatSelector,
                            leftRow,
                            1,
                            leftCols,
                            leftWidths,
                            hasHeading = true,
                            hasEndingGuideDots = true,
                            lastRow = lastRow
                        )
                    )
                } catch (_: BadSimpleTableException) {
                    return handleBadFacingTable(
                        leftTable,
                        rightTable,
                        style,
                        mutPageBuilders,
                        formatSelector,
                        leftCells,
                        rightCells,
                        startingPBSize,
                        leftOriginalTable,
                        rightOriginalTable
                    )
                }
                leftPage.addAtLeastPages(1)
                mutPageBuilders.addAll(leftPage.processSpacing())
                rightPage = mutPageBuilders.last()
                rightPage.setNonsequentialPages(true)
            }
            rightPage.y = (leftRow[0].y ?: 0) + (leftRow[0].height ?: 0)
            try {
                mutPageBuilders.addAll(
                    addRow(
                        mutPageBuilders,
                        rightPage,
                        formatSelector,
                        rightRow,
                        1,
                        rightCols,
                        rightWidths,
                        hasHeading = false,
                        hasEndingGuideDots = false,
                        lastRow = lastRow
                    )
                )
            } catch (_: BadSimpleTableException) {
                return handleBadFacingTable(
                    leftTable,
                    rightTable,
                    style,
                    mutPageBuilders,
                    formatSelector,
                    leftCells,
                    rightCells,
                    startingPBSize,
                    leftOriginalTable,
                    rightOriginalTable
                )
            }
            leftPage.y = max(leftPage.y.toDouble(), rightPage.y.toDouble()).toInt()
            rightPage.y = max(rightPage.y.toDouble(), leftPage.y.toDouble()).toInt()
        }

        rightPage.addAtLeastLinesBefore(2)

        return mutPageBuilders
    }

    private fun addNewPage(curPB: PageBuilder, pageBuilders: MutableSet<PageBuilder>): PageBuilder {
        //Page numbers will not cooperate unless there is something on the page grid
        //when a new page is made, so add a temporary fake brl element
        val fakeBrl = UTDElements.BRL.create()
        fakeBrl.appendChild("test")
        curPB.addBrl(fakeBrl)
        curPB.addAtLeastPages(1)
        pageBuilders.addAll(curPB.processSpacing())
        curPB.removeBrl(fakeBrl)
        pageBuilders.last().setNonsequentialPages(true)
        return pageBuilders.last()
    }

    private fun removeCellsFromPageBuilders(
        leftCells: List<TableCell>,
        rightCells: List<TableCell>,
        pageBuilders: Set<PageBuilder>
    ) {
        leftCells.forEach(Consumer { c: TableCell ->
            c.removeFromPageBuilders(pageBuilders)
            c.x = null
            c.y = null
        })
        rightCells.forEach(Consumer { c: TableCell ->
            c.removeFromPageBuilders(pageBuilders)
            c.x = null
            c.y = null
        })
    }

    private fun handleBadFacingTable(
        leftTable: Element?,
        rightTable: Element,
        style: IStyle,
        pbs: Set<PageBuilder>,
        formatSelector: FormatSelector,
        leftCells: List<List<TableCell>>,
        rightCells: List<List<TableCell>>,
        startingPBSize: Int,
        leftOriginalTable: Element,
        rightOriginalTable: Element
    ): MutableSet<PageBuilder> {
        var pageBuilders = pbs.toMutableSet()
        val span = leftTable!!.parent as Element
        for (i in span.attributeCount - 1 downTo 0) {
            span.removeAttribute(span.getAttribute(i))
        }
        pageBuilders = handleBadSimpleTable(leftTable, style, pageBuilders, formatSelector, leftCells, startingPBSize)
        return handleBadSimpleTable(rightTable, style, pageBuilders, formatSelector, rightCells, pageBuilders.size)
    }
}
