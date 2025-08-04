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
package org.brailleblaster.utd.utils

import nu.xom.Attribute
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.apache.commons.text.WordUtils
import org.brailleblaster.utd.BrailleSettings
import org.brailleblaster.utd.IStyleMap
import org.brailleblaster.utd.ITranslationEngine
import org.brailleblaster.utd.PageSettings
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.UTD_NS
import org.brailleblaster.utils.xom.detachAll

object TableUtils {
    const val ATTRIB_TABLE_COPY = "tableCopy"
    const val FALLBACK_ATTRIB_NAME = "fallback"
    const val FALLBACK_ATTRIB_VALUE = "true"

    /**
     * Copies the given table and extracts all brl elements
     * @param table
     * @return
     */
    @JvmStatic
    fun copyTable(table: Element): Element {
        val newTable = table.copy()
        findTableBrls(table).forEach { it.localName = "brl" }
        //Table formatters use this table as the "original" table, so mark the given table as the copy
        table.addAttribute(
            Attribute(
                UTDElements.UTD_PREFIX + ":" + ATTRIB_TABLE_COPY,
                UTD_NS,
                "true"
            )
        )
        return newTable
    }

    /**
     * Find elements with the TableRow style option
     * @param element
     * @param iStyleMap
     * @return
     */
    fun findRows(element: Element, iStyleMap: IStyleMap): List<Element> =
        element.childElements.flatMap {
            val style = iStyleMap.findValueOrDefault(it)
            if (style.isTableRow) listOf(it) else findRows(it, iStyleMap)
    }

    /**
     * Find elements with the TableCell style option
     * @param element
     * @param iStyleMap
     * @return
     */
    fun findCols(element: Element, iStyleMap: IStyleMap): List<Element> =
        element.childElements.flatMap {
            val style = iStyleMap.findValueOrDefault(it)
            if (style.isTableCell) listOf(it) else findCols(it, iStyleMap)
    }

    @JvmStatic
	  fun findCaption(element: Element, styleMap: IStyleMap): List<Element> {
        val foundCaption: MutableList<Element> = ArrayList()
        for (i in 0 until element.childCount) {
            val child = element.getChild(i)
            if (child is Element) {
                if (styleMap.findValueOrDefault(child).isTableRow) {
                    break
                }
                foundCaption.addAll(findCaption(child, styleMap))
            } else if (child is Text) {
                foundCaption.add(child.parent as Element)
                break
            }
        }
        return foundCaption
    }

    /**
     * Find any brl elements that occur before the first table row
     * @param element
     * @param styleMap
     * @return
     */
    fun findCaptionBrl(element: Element, styleMap: IStyleMap): List<Element> {
        return if (UTDHelper.containsBrl(element)) {
            element.childElements.map { it to styleMap.findValueOrDefault(it) }.takeWhile { (_, style) -> !style.isTableRow }
                .flatMap { (child, _) ->
                    if (UTDElements.BRL.isA(child)) {
                        listOf(child)
                    } else if (UTDHelper.containsBrl(child)) {
                        findCaptionBrl(child, styleMap)
                    } else {
                        emptyList()
                    }
                }
        } else {
            emptyList()
        }
    }

    const val SIGN_OF_OMISSION = "\u2013"
    @JvmOverloads
    fun createSignsOfOmission(
        engine: ITranslationEngine,
        tableParent: Element,
        styleMap: IStyleMap,
        symbol: String = SIGN_OF_OMISSION
    ) {
        val (idx, translatedSign) = if (symbol.isNotEmpty()) TextTranslator.translateIndexedText(symbol, engine) else (intArrayOf() to "")
        val rows = findRows(tableParent, styleMap)
        var totalCols = -1
        var i = 1
        while (i < rows.size) {
            val row = rows[i]
            val cols = findCols(row, styleMap)
            if (cols.size > totalCols) {
                totalCols = cols.size
                i = 0
            } else if (cols.size < totalCols) {
                if (cols.isEmpty()) {
                    row.detach()
                } else if (symbol.isNotEmpty()) {
                    val rowCopy = cols[0].copy()
                    rowCopy.removeChildren()
                    row.appendChild(rowCopy)
                    addSignOfOmission(rowCopy, symbol, translatedSign, idx)
                    i = 0
                }
            } else if (symbol.isNotEmpty()) {
                for (col in cols) {
                    val brls = col.query("descendant::*[local-name()='brl']")
                    if (brls.joinToString(separator = "") { it.value }.replace(" ".toRegex(), "")
                            .replace("\u2800".toRegex(), "").isEmpty()) {
                        brls.detachAll()
                        addSignOfOmission(col, symbol, translatedSign, idx)
                    }
                }
            }
            i++
        }
    }

    private fun addSignOfOmission(parent: Element, sign: String, translatedSign: String, indexArray: IntArray) {
        parent.appendChild(sign)
        val rowBrl = UTDElements.BRL.create()
        rowBrl.appendChild(translatedSign)
        if (translatedSign.isNotEmpty()) {
            rowBrl.addAttribute(Attribute(
                "index",
                indexArray.joinToString(separator = " ") { it.toString() }
            ))
        }
        parent.appendChild(rowBrl)
    }

    fun deleteExistingTable(node: Node) {
        if (node is Element) {
            val parent = node.parent as Element
            val index = parent.indexOf(node)
            if (index + 1 < parent.childCount && parent.getChild(index + 1) is Element) {
                val secondElement = parent.getChild(index + 1) as Element
                if (secondElement.localName == node.localName && secondElement.getAttribute("class") != null && secondElement.getAttributeValue(
                        "class"
                    ).contains("utd:table")
                ) {
                    secondElement.detach()
                }
            }
        }
    }

    fun getDescendantBrlNoFormatting(element: Element?): List<Element> {
        return UTDHelper.getDescendantBrlFast(element).map { brl ->
            brl.copy().apply {
                childElements.detachAll()
            }
        }
    }

    fun removeBrlBetweenCells(element: Element, styleMap: IStyleMap) {
        val allBrls = UTDHelper.getDescendantBrlFast(element)
        val rows = findRows(element, styleMap)
        val properBrl = rows.flatMap { tr -> findCols(tr, styleMap) }.flatMap { td -> UTDHelper.getDescendantBrlFast(td) } + findCaptionBrl(element, styleMap)
        allBrls.filter { brl -> !properBrl.contains(brl) && brl.getAttribute("printPage") == null }.detachAll()
    }

    private const val MAX_CHARS = 150
    fun detectType(
        table: Element,
        styleMap: IStyleMap,
        brailleSettings: BrailleSettings,
        pageSettings: PageSettings
    ): TableTypes {
        val columnElements = ArrayList<Element>()
        var parent = table.parent
        while (parent != null && parent !== table.document.rootElement) {
            val style = styleMap.findValueOrDefault(parent)
            if (style.isTable) return TableTypes.NONTABLE
            parent = parent.parent
        }
        //Calculate number of rows and columns
        var rowNum = 0
        var columnNum = 0
        var totalCols = 0
        val rows = findRows(table, styleMap)
        var longestCol = 0
        var longestColBrl = ""
        for (row in rows) {
            rowNum++
            val cols = findCols(row, styleMap)
            for (col in cols) {
                columnNum++
                columnElements.add(col)
                val colBrl: String = try {
                    col.getChild(1).getChild(0).value
                }
                catch (_: Exception) {
                    // If the column doesn't have a braille element, skip it
                    continue
                }
                longestCol = maxOf(colBrl.length, longestCol)
                //Get the longest braille string
                //We want the braille string to split it by spaces to figure out line wrapping.
                // More than 2 lines means it should be a listed table.
                if (colBrl.length >= longestCol) {
                    longestColBrl = colBrl
                }
            }
            totalCols = totalCols.coerceAtLeast(columnNum)
            columnNum = 0
        }
        columnNum = totalCols
        if (columnNum <= 1 || rowNum <= 1) {
            return TableTypes.NONTABLE
        }
        var defaultWidth = brailleSettings.cellType.getCellsForWidth(pageSettings.drawableWidth.toBigDecimal())
        defaultWidth /= columnNum

        //totalSizes doesn't compute correctly. Seems to always be 0.
        val totalSizes =
            columnElements.map { elem -> UTDHelper.getDescendantBrlFastNodes(elem).sumOf { it.value.length } }

        //Simple enough way to estimate the number of lines in the longest column
        val estimatedWrap = WordUtils.wrap(longestColBrl, defaultWidth)
        val estimatedLines = estimatedWrap.split("\n").size + 1

        return if (columnNum <= 4 && estimatedLines <= 2) {
            TableTypes.SIMPLE
        } else if (totalSizes.none { it > MAX_CHARS } || estimatedLines > 2) {
            TableTypes.LISTED
        } else {
            TableTypes.NONTABLE
        }
    }

    @JvmStatic
	  fun hasSimpleTableOption(option: SimpleTableOptions, table: Element): Boolean {
        return option.value == table.getAttributeValue(option.id)
    }

    @JvmStatic
	  fun applySimpleTableOption(table: Element, option: SimpleTableOptions) {
        table.addAttribute(Attribute(option.id, option.value))
    }

    @JvmStatic
	  fun getCustomSimpleTableWidths(table: Element): IntArray? {
        if (hasSimpleTableOption(SimpleTableOptions.CUSTOM_WIDTHS, table)) {
            val attr = table.getAttributeValue("widths") ?: return null
            return attr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.map { it.toInt() }.toIntArray()
        }
        return null
    }

    @JvmStatic
	  fun applyCustomSimpleTableWidths(table: Element, widths: IntArray) {
        applySimpleTableOption(table, SimpleTableOptions.CUSTOM_WIDTHS)
        table.addAttribute(Attribute("widths", widths.joinToString(separator = ",") { it.toString() }))
    }

    /**
     * Checks if given element is a <table> copy
     * @param table
     * @return
    </table> */
	  @JvmStatic
	  fun isTableCopy(table: Element): Boolean {
        return table.getAttribute(
            ATTRIB_TABLE_COPY,
            UTD_NS
        ) != null
    }

    fun findTableBrls(element: Element): List<Element> = element.childElements.flatMap { e ->
        if (e.namespaceURI == UTD_NS && e.localName == "tablebrl") listOf(e)
        else findTableBrls(e)
    }

    enum class SimpleTableOptions(val id: String, val value: String) {
        GUIDE_DOTS_DISABLED("guideDots", "false"),
        ROW_HEADING_DISABLED("rowHeading", "false"),
        COLUMN_HEADING_DISABLED("columnHeading", "false"),
        ONE_CELL_BETWEEN_COLUMNS("cellsBwColumns", "1"),
        CUSTOM_WIDTHS("customWidths", "true")
    }

    enum class TableTypes {
        SIMPLE,
        LISTED,
        LINEAR,
        STAIRSTEP,
        NONTABLE
    }
}
