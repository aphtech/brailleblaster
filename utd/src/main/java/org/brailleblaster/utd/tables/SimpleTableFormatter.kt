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
import org.brailleblaster.utd.exceptions.BadSimpleTableException
import org.brailleblaster.utd.formatters.Formatter
import org.brailleblaster.utd.internal.elements.TableDivider
import org.brailleblaster.utd.properties.Align
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.PageBuilderHelper
import org.brailleblaster.utd.utils.TableUtils
import org.brailleblaster.utd.utils.UTDHelper
import org.brailleblaster.utils.UTD_NS
import org.brailleblaster.utils.xom.detachAll
import org.slf4j.LoggerFactory
import kotlin.math.max

open class SimpleTableFormatter : Formatter() {
    protected class TableCell internal constructor(var td: Element, var row: Int, var col: Int) {
        var guideDots: Element? = null

        @JvmField
        var x: Int? = null

        @JvmField
        var y: Int? = null

        @JvmField
        var height: Int? = null

        fun removeFromPageBuilders(pageBuilders: Set<PageBuilder>) {
            for (pb in pageBuilders) {
                removeFromPageBuilder(pb)
            }
        }

        fun removeFromPageBuilder(pageBuilder: PageBuilder) {
            val brls = brlElements
            for (brl in brls) {
                pageBuilder.removeBrl(brl)
            }
            if (guideDots != null) pageBuilder.removeFormattingElement(guideDots!!)
        }

        val text: String
            get() = td.value
        val brlElements: List<Element>
            get() = UTDHelper.getDescendantBrlFast(td)
        val textLength: Int
            get() {
                val brls = brlElements
                return brls.sumOf { e: Element -> e.value.length }
            }
    }

    private var cellsBetweenCols = 2
    private var guideDotsEnabled = true
    private val log = LoggerFactory.getLogger(javaClass)
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
        TableUtils.deleteExistingTable(node)
        val startingPBSize = mutPageBuilders.size
        val originalTable = TableUtils.copyTable(node)
        cellsBetweenCols = if (TableUtils.hasSimpleTableOption(
                TableUtils.SimpleTableOptions.ONE_CELL_BETWEEN_COLUMNS,
                node
            )
        ) 1 else 2
        guideDotsEnabled = !TableUtils.hasSimpleTableOption(TableUtils.SimpleTableOptions.GUIDE_DOTS_DISABLED, node)
        val rowHeadingEnabled =
            !TableUtils.hasSimpleTableOption(TableUtils.SimpleTableOptions.ROW_HEADING_DISABLED, node)
        prepareTable(node, formatSelector)
        mutPageBuilders.addAll(preFormat(node, style, mutPageBuilders, formatSelector))
        var pageBuilder = mutPageBuilders.last()
        pageBuilder.clearKeepWithNext() //TODO: Fix
        val cellType = formatSelector.engine.brailleSettings.cellType
        val cells = findCells(node, formatSelector.styleMap)
        // Check if all rows are empty to determine if the table is empty
        // Calculating the number of columns will do this.
        val numOfCols = calculateNumOfCols(cells)
        if (numOfCols == 0) {
            log.debug("Skipping empty table")
            return mutPageBuilders
        }

        if (!calculateSmallestFits(
                cells,
                cellType.getCellsForWidth(formatSelector.engine.pageSettings.drawableWidth.toBigDecimal()),
                numOfCols,
                false
            )
        ) {
            return handleBadSimpleTable(node, originalTable, style, mutPageBuilders, formatSelector, cells, startingPBSize)
        }

        val widths = TableUtils.getCustomSimpleTableWidths(node) ?: calculateColumnWidths(
            cells,
            cellType.getCellsForWidth(formatSelector.engine.pageSettings.drawableWidth.toBigDecimal()),
            numOfCols,
            false
        )
        if (!TableUtils.hasSimpleTableOption(TableUtils.SimpleTableOptions.COLUMN_HEADING_DISABLED, node)) {
            addSeparationLines(cells[0], formatSelector, widths)
        }
        mutPageBuilders.addAll(pageBuilder.processSpacing())
        pageBuilder = mutPageBuilders.last()
        var startOfPage = 0
        if (pageBuilder.runningHead.isNotEmpty()) {
            startOfPage = 2
        } else if (shouldSkipTopLine(pageBuilder, formatSelector)) {
            startOfPage = 1
        }
        if (pageBuilder.y < startOfPage) pageBuilder.y = startOfPage
        if (pageBuilder.hasLeftPage()) pageBuilder.setSkipNumberLineBottom(true)
        pageBuilder.alignment = Align.LEFT
        val startingY = pageBuilder.y
        try {
            mutPageBuilders.addAll(addHeadings(mutPageBuilders, pageBuilder, cells[0], widths, startOfPage, startingY))
        } catch (_: BadSimpleTableException) {
            return handleBadSimpleTable(
                node,
                originalTable, style,
                mutPageBuilders,
                formatSelector,
                cells,
                startingPBSize
            )
        }

        pageBuilder = mutPageBuilders.last()
        pageBuilder.addAtLeastLinesBefore(1)
        //Skip last line so there's no interference with page numbers
        if (pageBuilder.y + pageBuilder.pendingLinesBefore == pageBuilder.linesPerPage - 1) {
            pageBuilder.addAtLeastPages(1)
        }
        val curResults = mutPageBuilders.size
        mutPageBuilders.addAll(pageBuilder.processSpacing())
        if (mutPageBuilders.size > curResults) {
            pageBuilder = mutPageBuilders.last()
            pageBuilder.y = startOfPage
        }
        for (rowNum in 1 until cells.size) {
            val lastRow = rowNum + 1 == cells.size
            try {
                mutPageBuilders.addAll(
                    addRow(
                        mutPageBuilders,
                        pageBuilder,
                        formatSelector,
                        cells[rowNum],
                        startOfPage,
                        numOfCols,
                        widths,
                        rowHeadingEnabled,
                        false,
                        lastRow
                    )
                )
            } catch (_: BadSimpleTableException) {
                return handleBadSimpleTable(
                    node,
                    originalTable, style,
                    mutPageBuilders,
                    formatSelector,
                    cells,
                    startingPBSize
                )
            }
            pageBuilder = mutPageBuilders.last()
        }
        node.addAttribute(Attribute("class", "utd:tableSimple"))
        node.parent.insertChild(originalTable, node.parent.indexOf(node))
        val linesAfter = style.getLinesAfter(node, formatSelector.styleMap.namespaces)
        if (linesAfter != null) {
            pageBuilder.setMaxLines(linesAfter)
        } else {
            pageBuilder.addAtLeastLinesAfter(style.linesAfter.coerceAtLeast(1))
        }
        pageBuilder.addAtLeastPages(style.newPagesAfter)
        return mutPageBuilders
    }

    private fun handleBadSimpleTable(
        node: Element,
        originalTable: Element,
        style: IStyle,
        pageBuilders: MutableSet<PageBuilder>,
        formatSelector: FormatSelector,
        cells: List<List<TableCell>>,
        startingPBSize: Int
    ): MutableSet<PageBuilder> {
        node.parent.replaceChild(node, originalTable)
        pageBuilders.forEach { pb ->
            UTDHelper.getDescendantBrlFast(node).forEach { brl ->
                pb.removeBrl(brl)
            }
        }
        return handleBadSimpleTable(
            originalTable, style,
            pageBuilders,
            formatSelector,
            cells,
            startingPBSize
        )
    }

    protected fun prepareTable(table: Element, formatSelector: FormatSelector) {
        TableUtils.removeBrlBetweenCells(table, formatSelector.styleMap)
        TableUtils.createSignsOfOmission(formatSelector.engine, table, formatSelector.styleMap, "")
    }

    protected fun preFormat(
        element: Element,
        style: IStyle,
        pageBuilders: MutableSet<PageBuilder>,
        formatSelector: FormatSelector
    ): MutableSet<PageBuilder> {
        var pageBuilder = pageBuilders.last()
        val keepWithNext = pageBuilder.hasKeepWithNextQueued()
        val linesBefore = style.getLinesBefore(
            element,
            formatSelector.styleMap.namespaces
        )
        if (linesBefore != null) {
            pageBuilder.setMaxLines(linesBefore)
        } else {
            pageBuilder.addAtLeastLinesBefore(style.linesBefore)
        }
        pageBuilder.addAtLeastPages(style.newPagesBefore)
        val pbSize = pageBuilders.size
        pageBuilders.addAll(pageBuilder.processSpacing())
        if (pageBuilders.size > pbSize) {
            pageBuilder = pageBuilders.last()
            if (keepWithNext) {
                pageBuilder.addAtLeastLinesBefore(style.linesBefore.coerceAtLeast(1))
                pageBuilders.addAll(pageBuilder.processSpacing())
                pageBuilder = pageBuilders.last()
            }
        }
        val caption = TableUtils.findCaptionBrl(element, formatSelector.styleMap)
        for (brl in caption) {
            pageBuilder.apply {
                setFirstLineIndent(6)
                setLeftIndent(4)
                setRightIndent(0)
                addAtLeastLinesBefore(1)
                setStartOfBlock(true)
            }
            pageBuilders.addAll(pageBuilder.addBrl(brl))
            pageBuilder = pageBuilders.last()
        }
        if (caption.isNotEmpty()) pageBuilder.addAtLeastLinesBefore(2)
        return pageBuilders
    }

    @Throws(BadSimpleTableException::class)
    protected fun addHeadings(
        pageBuilders: MutableSet<PageBuilder>,
        pageBuilder: PageBuilder,
        headings: List<TableCell>,
        widths: IntArray,
        startOfPage: Int,
        startLine: Int
    ): Set<PageBuilder> {
        var curPageBuilder = pageBuilder
        var startingLine = startLine
        var finalLine: Int? = null
        var beginningOfPage = curPageBuilder.y == 0 || curPageBuilder.y == 1 && curPageBuilder.runningHead.isNotEmpty()
        var i = 0
        while (i < headings.size) {

            //For each heading...
            val curHeading = headings[i]
            if (curHeading.x == null) curHeading.x = calculateXPos(i, widths)
            if (curHeading.y == null) curHeading.y = startingLine
            curPageBuilder.x = curHeading.x!!
            curPageBuilder.y = curHeading.y!!
            val brlElements = curHeading.brlElements
            for (brl in brlElements) {
                //For each brl element in this heading...
                if (brl.getAttributeValue("class") != null && brl.getAttributeValue("class") == "sepline") {
                    //If this is a separation line, it needs to be on its own line
                    curPageBuilder.addAtLeastLinesBefore(1)
                    //Skip last line so there's no interference with page numbers
                    if (curPageBuilder.y + curPageBuilder.pendingLinesBefore == curPageBuilder.linesPerPage - 1) curPageBuilder.addAtLeastPages(
                        1
                    )
                }
                curPageBuilder.setFirstLineIndent(curHeading.x)
                    .setLeftIndent(curHeading.x!!)
                    .setRightIndent(widths[i])
                    .setStartOfBlock(true)
                val curPages = pageBuilders.size
                pageBuilders.addAll(curPageBuilder.addBrl(brl))
                if (pageBuilders.size > curPages) {
                    if (beginningOfPage) {
                        log.error("A simple table heading failed to be created")
                        throw BadSimpleTableException()
                    }
                    for (heading in headings) {
                        heading.x = null
                        heading.y = null
                        heading.removeFromPageBuilders(pageBuilders)
                    }
                    curPageBuilder = pageBuilders.last()
                    finalLine = null
                    curPageBuilder.x = 0
                    curPageBuilder.y = startOfPage
                    startingLine = startOfPage
                    i = -1
                    beginningOfPage = true
                    break
                }
            }
            curHeading.height = curPageBuilder.y - startingLine
            finalLine = if (finalLine == null) {
                //This is the first heading
                curPageBuilder.y
            } else if (finalLine < curPageBuilder.y) {
                //This heading ended farther down than previous headings
                headings.take(i).forEach { prevHeading ->
                    //Move each previous heading down until they end on the same line
                    prevHeading.removeFromPageBuilders(pageBuilders)
                    prevHeading.y = prevHeading.y!! + curHeading.height!! - prevHeading.height!!
                }
                curHeading.removeFromPageBuilders(pageBuilders)
                i = -1 //Restart loop
                curPageBuilder.y
            } else if (finalLine > curPageBuilder.y) {
                //This heading ended further up than previous headings
                //Move this heading down until it ends on the same line as previous headings
                curHeading.y = curHeading.y!! + finalLine - curPageBuilder.y
                curHeading.removeFromPageBuilders(pageBuilders)
                i-- //Redo this element
                finalLine
            } else finalLine
            i++
        }
        return pageBuilders
    }

    @Throws(BadSimpleTableException::class)
    protected fun addRow(
        pageBuilders: MutableSet<PageBuilder>,
        pageBuilder: PageBuilder,
        formatSelector: FormatSelector,
        row: List<TableCell>,
        startOfPage: Int,
        numOfCols: Int,
        widths: IntArray,
        hasHeading: Boolean,
        hasEndingGuideDots: Boolean,
        lastRow: Boolean
    ): Set<PageBuilder> {
        // Thought to be unused
        // List<Element> guideDots = new ArrayList<Element>();
        // Check the row actually has something
        var curPageBuilder = pageBuilder
/*        if (row.isEmpty()) {
            return pageBuilders
        }
        */
        var endingY = 0
        val atBeginningOfPage = curPageBuilder.y <= startOfPage
        for (colNum in row.indices) {
            val col = row[colNum]
            val startPos = calculateXPos(col.col, widths)
            curPageBuilder.setFirstLineIndent(startPos)
                .setLeftIndent(startPos + 2)
                .setRightIndent(widths[col.col] - 2)
                .setStartOfBlock(true)
            curPageBuilder.x = startPos
            val startingY = curPageBuilder.y
            val brls = UTDHelper.getDescendantBrlFastNodes(col.td)
            for (i in 0 until brls.size()) {
                val curBrl = brls[i] as Element
                val curPages = pageBuilders.size
                pageBuilders.addAll(curPageBuilder.addBrl(curBrl))
                if (pageBuilders.size > curPages) {
                    return if (!atBeginningOfPage) {
                        for (k in 0 until colNum) {
                            val prevCol = row[k]
                            prevCol.removeFromPageBuilders(pageBuilders)
                        }
                        col.removeFromPageBuilders(pageBuilders)
                        curPageBuilder = pageBuilders.last()
                        curPageBuilder.x = 0
                        curPageBuilder.y = startOfPage
                        addRow(
                            pageBuilders,
                            curPageBuilder,
                            formatSelector,
                            row,
                            startOfPage,
                            numOfCols,
                            widths,
                            hasHeading,
                            hasEndingGuideDots,
                            lastRow
                        )
                    } else {
                        //This table is impossible to render as simple.
                        log.error("A simple table failed to be created")
                        throw BadSimpleTableException()
                    }
                }
                curPageBuilder.setStartOfBlock(false)
            }
            endingY = endingY.coerceAtLeast(curPageBuilder.y)
            col.y = startingY
            col.height = endingY - startingY
            if (guideDotsEnabled) {
                if (colNum != 0 || !hasHeading) {
                    curPageBuilder.y = startingY
                }
                if (colNum != numOfCols - 1 && numOfCols > 1 || colNum == numOfCols - 1 && hasEndingGuideDots) {
                    val startOfCol = calculateXPos(colNum, widths) + col.textLength.coerceAtMost(2)
                    if (startOfCol < curPageBuilder.cellsPerLine) {
                        curPageBuilder.x = startOfCol
                        val endOfCol =
                            if (colNum == numOfCols - 1) startOfCol + widths[colNum] - cellsBetweenCols else calculateXPos(
                                colNum + 1,
                                widths
                            ) - cellsBetweenCols
                        val guideDot = if (formatSelector.engine.brailleSettings.isUseAsciiBraille) '"' else '\u2810'
                        //If cell is empty, fill column with guide dots
                        val dotsElement = if (col.text.isEmpty()) curPageBuilder.fillSpace(
                            guideDot,
                            endOfCol - startOfCol,
                            0,
                            4
                        ) else curPageBuilder.fillSpace(guideDot, endOfCol - startOfCol + 1, 1, 4)
                        if (dotsElement != null) {
                            dotsElement.addAttribute(Attribute("type", "guideDots"))
                            dotsElement.addAttribute(Attribute(GUIDE_DOTS_ATTRIB_NAME, GUIDE_DOTS_ATTRIB_VALUE))
                        }
                        col.guideDots = dotsElement
                    }
                }
            }
        }
        curPageBuilder.y = endingY
        curPageBuilder.setForceSpacing(true)
        curPageBuilder.addAtLeastLinesBefore(1)
        //Skip last line so there's no interference with page numbers
        //and only when not last row (IE. still within the table)
        if (!lastRow && curPageBuilder.y + curPageBuilder.pendingLinesBefore == curPageBuilder.linesPerPage - 1 && shouldSkipBottomLine(
                curPageBuilder,
                formatSelector
            )
        ) {
            curPageBuilder.addAtLeastPages(1)
        }
        val processNewLine: Set<PageBuilder> = curPageBuilder.processSpacing()
        if (processNewLine.size > 1) {
            curPageBuilder = processNewLine.last()
            // When within the table, table rows should not be on number line.
            if (!lastRow) {
                curPageBuilder.y = if (shouldSkipTopLine(curPageBuilder, formatSelector)) {
                    1
                } else {
                    0
                }
            }
        }
        pageBuilders.addAll(processNewLine)
        return pageBuilders
    }

    private fun changeTable(element: Element) =
        element.query("descendant::*[local-name()='span'][@class='sepline']", UTDElements.UTD_XPATH_CONTEXT).detachAll()

    protected fun calculateColumnWidths(
        cells: List<List<TableCell>>,
        totalWidth: Int,
        numOfCols: Int,
        fullWidth: Boolean
    ): IntArray {
        val lengths = IntArray(numOfCols) //Stores total length of each element
        val totalSpaceBetweenCols = (numOfCols - 1) * cellsBetweenCols
        val startingWidth = (totalWidth - totalSpaceBetweenCols) / numOfCols
        var leftoverSpace = 0
        val widths = IntArray(numOfCols) { startingWidth } //Stores final calculated widths
        for (i in 0 until numOfCols) { //For each column...
            val curCol = getCellsInCol(cells, i)
            var longestWidth = 0
            for (curCell in curCol) {
                //Calculate how much text is in each element of this column and determine whether less space is needed
                longestWidth = max(TableUtils.getDescendantBrlNoFormatting(curCell.td).stream()
                    .mapToInt { s: Element -> s.value.length }
                    .sum(), longestWidth)
                longestWidth = max(longestWidth, 1) //Columns have to be at least 1 cell wide
                lengths[i] = longestWidth
            }
            if (longestWidth < startingWidth) {
                //This column needs less space. Shorten this column
                widths[i] = longestWidth
                leftoverSpace += startingWidth - longestWidth
            }
        }
        var changed = false
        var i = 0
        while (i < widths.size) {

            //Now find each column that needs to be bigger and use the leftover space to fill it
            if (leftoverSpace > 0 && (widths[i] >= startingWidth && widths[i] != lengths[i] || fullWidth)) { //Don't make the column any bigger than it needs to be
                widths[i]++
                leftoverSpace--
                changed = true
            }
            if (i == widths.size - 1) {
                if (!changed || leftoverSpace == 0) {
                    break //Get out of the loop if no changes need to be made
                } else {
                    i = -1 //Restart the loop until there is no more leftover space
                    changed = false
                }
            }
            i++
        }
        return widths
    }


    private fun calculateSmallestFits(
        cells: List<List<TableCell>>,
        totalWidth: Int,
        numOfCols: Int,
        fullWidth: Boolean
    ): Boolean {
        val totalSpaceBetweenCols = (numOfCols - 1) * cellsBetweenCols
        val startingWidth = (totalWidth - totalSpaceBetweenCols) / numOfCols
        val widths = IntArray(numOfCols) { startingWidth} //Stores final calculated widths
        for (i in 0 until numOfCols) { //For each column...
            val curCol = getCellsInCol(cells, i)
            var longestWidth = 0
            for (curCell in curCol) {
//calculate the smallest column width that fits the content of the column in two rows
                longestWidth = max(
                    smallestColumnWidthForTwoRows(TableUtils.getDescendantBrlNoFormatting(curCell.td)
                        .map { it.value ?: "" }
                        .joinToString { it }
                    ), longestWidth)
                longestWidth = max(longestWidth, 1) //Columns have to be at least 1 cell wide
            }

            widths[i] = longestWidth
        }

        return (widths.sum() + totalSpaceBetweenCols) <= totalWidth
    }


    private fun smallestColumnWidthForTwoRows(input: String): Int {
        // Function to determine if the input string can fit in exactly two rows of a given column width
        fun canFitInTwoRows(words: List<String>, width: Int): Boolean {
            var currentWidth = 0
            var rowCount = 1

            for (word in words) {
                val wordLength = word.length
                if (wordLength > width) {
                    return false // A single word is too long to fit in the column width
                }
                if (currentWidth + wordLength > width) {
                    rowCount++
                    currentWidth = wordLength // start new row with the current word
                    if (rowCount > 2) {
                        return false // More than two rows needed
                    }
                } else {
                    currentWidth += wordLength
                }
                currentWidth++ // Account for the space after the word
            }

            return rowCount <= 2
        }

        // Split the input string by whitespace
        val words = input.split("\\s+".toRegex())

        // Binary search to find the smallest possible column width
        var left = 1
        var right = input.length
        var result = right

        while (left <= right) {
            val mid = (left + right) / 2
            if (canFitInTwoRows(words, mid)) {
                result = mid
                right = mid - 1
            } else {
                left = mid + 1
            }
        }

        return result
    }


    protected fun calculateNumOfCols(cells: List<List<TableCell>>): Int = cells.maxOfOrNull { it.size } ?: 0

    private fun calculateXPos(col: Int, widths: IntArray): Int =
        widths.map { it + cellsBetweenCols }.take(col.coerceAtLeast(0)).sum()

    protected fun findCells(element: Element, iStyleMap: IStyleMap): List<List<TableCell>> {
        return TableUtils.findRows(element, iStyleMap).mapIndexed { rowIndex, row ->
            TableUtils.findCols(row, iStyleMap).mapIndexed { colIndex, col ->
                col.addAttribute(Attribute("row-col", "$rowIndex-$colIndex"))
                TableCell(col, rowIndex, colIndex).apply {
                    if (rowIndex == 0) brlElements.forEach { brl: Element ->
                        brl.addAttribute(
                            Attribute(
                                "tableHeading",
                                if (colIndex == 0) "true" else "false"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun getCellsInCol(cells: List<List<TableCell>>, col: Int): List<TableCell> =
        cells.flatten().filter { it.col == col }

    protected fun addSeparationLines(headings: List<TableCell>, formatSelector: FormatSelector, widths: IntArray) {
        for (heading in headings) {
            if (heading.brlElements.isEmpty()) {
                continue
            }
            val width = widths[heading.col]
            val span: Element = TableDivider(TableDivider.DividerTypes.SIMPLE_SEP_LINE)
            span.appendChild("-".repeat(width))
            val brl = UTDElements.BRL.create()
            if (formatSelector.engine.brailleSettings.isUseAsciiBraille) brl.appendChild(
                "\"" +
                        "3".repeat(width - 1)
            ) else brl.appendChild("\u2810" + "\u2812".repeat(width - 1))
            brl.addAttribute(Attribute("class", "sepline"))
            span.appendChild(brl)
            val index = StringBuilder()
            for (i in 0 until width) {
                index.append(i).append(" ")
            }
            brl.addAttribute(Attribute("index", index.toString().trim { it <= ' ' }))
            brl.addAttribute(Attribute("tableHeading", if (heading.col == 0) "true" else "false"))
            heading.td.appendChild(span)
        }
    }

    protected fun handleBadSimpleTable(
        element: Element,
        style: IStyle,
        pageBuilders: MutableSet<PageBuilder>,
        formatSelector: FormatSelector,
        cells: List<List<TableCell>>,
        startingPBSize: Int
    ): MutableSet<PageBuilder> {
        if (element.getAttribute("class") != null) element.removeAttribute(element.getAttribute("class"))
        //Remove all added table cells
        for (row in cells) {
            for (cell in row) {
                cell.removeFromPageBuilders(pageBuilders)
            }
        }

        //Delete any blank pages that were made
        var i = pageBuilders.size - 1
        while (i >= 0 && pageBuilders.size >= startingPBSize && pageBuilders.size > 1) {
            val pb = pageBuilders.elementAt(i)
            if (pb.isBlankPage) {
                pageBuilders.remove(pb)
            }
            i--
        }
        val pb = pageBuilders.last()
        //Reset the cursor
        pb.y = pb.findLastBlankLine()
        changeTable(element)
        element.addAttribute(Attribute("format", "listed"))
        element.addAttribute(Attribute("utd-style", "Listed Table"))
        element.addAttribute(
            Attribute(
                UTDElements.UTD_PREFIX + ":" + TableUtils.FALLBACK_ATTRIB_NAME,
                UTD_NS,
                TableUtils.FALLBACK_ATTRIB_VALUE
            )
        )
        element.getAttribute(TableUtils.ATTRIB_TABLE_COPY, UTD_NS)?.detach()
        return ListedTableFormatter().format(element, style, pageBuilders, formatSelector)
    }

    private fun shouldSkipTopLine(pageBuilder: PageBuilder, formatSelector: FormatSelector): Boolean {
        val braillePageNumber = pageBuilder.braillePageNumber
        return ((PageBuilderHelper.getBraillePageNumberAt(
            formatSelector.engine.pageSettings,
            braillePageNumber.pageNumber
        ).isTop
            && pageBuilder.braillePageNum.isNotEmpty())
            || (PageBuilderHelper.getPrintPageNumberAt(
            formatSelector.engine.pageSettings,
            braillePageNumber.pageNumber
        ).isTop
            && pageBuilder.printPageNumber.isNotEmpty()))
    }

    private fun shouldSkipBottomLine(pageBuilder: PageBuilder, formatSelector: FormatSelector): Boolean {
        val braillePageNumber = pageBuilder.braillePageNumber
        return ((PageBuilderHelper.getBraillePageNumberAt(
            formatSelector.engine.pageSettings,
            braillePageNumber.pageNumber
        ).isBottom
            && pageBuilder.braillePageNum.isNotEmpty())
            || (PageBuilderHelper.getPrintPageNumberAt(
            formatSelector.engine.pageSettings,
            braillePageNumber.pageNumber
        ).isBottom
            && pageBuilder.printPageNumber.isNotEmpty()))
    }

    companion object {
        const val GUIDE_DOTS_ATTRIB_NAME = "tableDots"
        const val GUIDE_DOTS_ATTRIB_VALUE = "true"
    }
}
