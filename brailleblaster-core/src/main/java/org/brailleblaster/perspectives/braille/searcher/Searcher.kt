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
package org.brailleblaster.perspectives.braille.searcher

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.BrailleMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.PageIndicatorTextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.PrintPageBrlMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.UTD_NS

/**
 * Common MapList and Element search util methods
 */
object Searcher {

    @JvmStatic
	fun streamCurrentBufferFromCurrentCursor(m: Manager): Iterable<TextMapElement> {
        return streamCurrentBufferFrom(m, m.mapList.current)
    }

    fun streamCurrentBufferFrom(m: Manager, tme: TextMapElement): Iterable<TextMapElement> {
        val tmeIndex = m.mapList.indexOf(tme)
        require(tmeIndex != -1) { "tme not found in current buffer $tme" }
        return m.mapList.subList(tmeIndex, m.mapList.size)
    }

    @JvmStatic
	fun streamCurrentBufferReverseFromCurrentCursor(m: Manager): Iterable<TextMapElement> =
        streamCurrentBufferReverseFrom(m, m.mapList.current)

    fun streamCurrentBufferReverseFrom(m: Manager, tme: TextMapElement): Iterable<TextMapElement> {
        val reverseList = m.mapList.reversed()
        val tmeIndex = reverseList.indexOf(tme)
        require(tmeIndex != -1) { "tme not found in current buffer $tme" }
        return reverseList.subList(tmeIndex, reverseList.size)
    }

    //	private static Element getCurrent() {
    //		Node node = list.getCurrent().n != null ? list.getCurrent().n
    //				: (list.getNext(true) != null ? list.getNext(true).n : list.getPrevious(true).n);
    //	}
    object Filters {
        fun braillePrintPageIndicator(tme: TextMapElement): Boolean {
            return tme.brailleList.any { brlTme: BrailleMapElement? -> brlTme is PrintPageBrlMapElement }
        }

        @JvmStatic
		fun noUTDAncestor(node: Node?): Boolean {
            return XMLHandler.ancestorElementNot(node) { curElem: Element -> curElem.namespaceURI == UTD_NS }
        }

        @JvmStatic
		fun isElement(node: Node?): Boolean {
            return node is Element
        }

    }

    object Mappers {
        fun printPageNum(tme: TextMapElement?): String {
            return when (tme) {
                null -> {
                    throw NullPointerException("tme")
                }
                is PageIndicatorTextMapElement -> {
                    // PageMapElement's brailleList should be the brlOnly tag
                    var brl = tme.brailleList.first().node
                    while (!UTDElements.BRL.isA(brl)) {
                        brl = brl.parent
                    }
                    val attributeValue = (brl as Element).getAttributeValue("printPage")
                    if (attributeValue.isNullOrBlank()) {
                        throw NodeException("Missing printPage attribute", brl)
                    }
                    attributeValue
                }

                else -> {
                    for (curBrailleElement in tme.brailleList) {
                        if (curBrailleElement is PrintPageBrlMapElement) {
                            return curBrailleElement.node.value
                        }
                    }
                    throw IllegalArgumentException("Not a print page num $tme")
                }
            }
        }

        @JvmStatic
		fun toElement(node: Node): Element {
            return node as Element
        }
    }
}