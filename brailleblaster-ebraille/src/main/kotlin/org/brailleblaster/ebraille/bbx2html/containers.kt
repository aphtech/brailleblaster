/*
 * Copyright (C) 2025-2026 American Printing House for the Blind
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
package org.brailleblaster.ebraille.bbx2html

import nu.xom.Element
import org.brailleblaster.bbx.BBX
import org.brailleblaster.ebraille.ListItem
import org.brailleblaster.ebraille.toHtml
import org.brailleblaster.utils.xml.BB_NS
import org.brailleblaster.utils.xml.UTD_NS
import org.brailleblaster.utils.xom.previousSibling
import org.jsoup.nodes.Node

internal fun Element.processContainer(): Collection<Node> = when (BBX.CONTAINER.getSubType(this)) {
    BBX.CONTAINER.BOX -> listOf(processBox())
    BBX.CONTAINER.LIST -> listOf(processList())
    BBX.CONTAINER.TABLETN -> listOf()
    BBX.CONTAINER.TABLE -> processTable()
    else -> processChildren()
}

private fun Element.processBox(): org.jsoup.nodes.Element {
    val (boxSymbol, cssClass) = when (style) {
        "Full Box" -> "\u283f" to "full-box"
        else -> "\u2836" to "box"
    }
    return org.jsoup.nodes.Element("div").addClass(cssClass).attr("type", boxSymbol).appendChildren(processChildren())
}

private fun Element.processList(): org.jsoup.nodes.Element =
    childElements.filter { BBX.BLOCK.LIST_ITEM.isA(it) }
        .map { ListItem(it, it.getAttributeValue("itemLevel", BB_NS)?.toIntOrNull() ?: 0) }.let { items ->
            when (BBX.CONTAINER.LIST.ATTRIB_LIST_TYPE.get(this)) {
                BBX.ListType.DEFINITION -> items.toHtml(
                    level = 0,
                    containerFactory = { org.jsoup.nodes.Element("dl") }
                ) {
                    it.element.processDefinitionListItem()
                }
                else -> items.toHtml(
                    level = 0,
                    containerFactory = { org.jsoup.nodes.Element("ul").attr("style", "list-style-type: none") }
                ) {
                    it.element.processBlock()
                }
            }
        }

private fun Element.processTable(): List<org.jsoup.nodes.Element> =
    if (getAttributeValue("tableCopy", UTD_NS) == "true") {
        listOf()
    } else {
        val tableFormat = getAttributeValue("format")
        listOf(
            org.jsoup.nodes.Element("table").also {
                if (tableFormat in listOf("listed", "stairstep", "linear")) {
                    it.attr("class", tableFormat)
                }
            }.appendChildren(
                childElements.filter { BBX.CONTAINER.TABLE_ROW.isA(it) }.take(1).let { firstRow ->
                    when (tableFormat) {
                        "simple" -> {
                            firstRow.processTableRows(if (getAttributeValue("columnHeading") == "false") "td" else "th")
                        }

                        "listed" -> {
                            firstRow.processTableRows("th")
                        }

                        "stairstep" -> {
                            headerRowFromStairStepTableTN(this) + (firstRow.processTableRows())
                        }

                        "linear" -> {
                            headerRowFromLinearTableTN(this) + (firstRow.processTableRows())
                        }

                        else -> {
                            firstRow.processTableRows()
                        }
                    }
                } + (childElements.filter { BBX.CONTAINER.TABLE_ROW.isA(it) }.drop(1).processTableRows())
            )
        )
    }

private fun Iterable<Element>.processTableRows(cellTag: String = "td"): List<org.jsoup.nodes.Element> = map { r ->
    org.jsoup.nodes.Element("tr").appendChildren(r.childElements.filter { BBX.BLOCK.TABLE_CELL.isA(it) }
        .map { c -> org.jsoup.nodes.Element(cellTag).appendChildren(c.processContent()) })
}

private fun headerRowFromStairStepTableTN(table: Element): List<org.jsoup.nodes.Element> =
    (table.previousSibling { it is Element } as? Element)?.let { e ->
        if (BBX.CONTAINER.TABLETN.isA(e)) {
            listOf(
                org.jsoup.nodes.Element("tr").appendChildren(
                    e.childElements.filter { BBX.BLOCK.isA(it) }.drop(1)
                        .map { org.jsoup.nodes.Element("th").appendChildren(it.processContent()) })
            )
        } else {
            listOf()
        }
    } ?: listOf()

private fun headerRowFromLinearTableTN(table: Element): List<org.jsoup.nodes.Element> =
    (table.previousSibling { it is Element } as? Element)?.let { e ->
        if (BBX.CONTAINER.TABLETN.isA(e)) {
            listOf(
                org.jsoup.nodes.Element("tr").appendChildren(
                    e.childElements.filter { BBX.BLOCK.isA(it) }.takeLast(1).flatMap { b ->
                        b.childElements.filter { BBX.SPAN.OTHER.isA(it) }
                            .map { org.jsoup.nodes.Element("th").appendChildren(it.processContent()) }
                    }
                )
            )
        } else {
            listOf()
        }
    } ?: listOf()