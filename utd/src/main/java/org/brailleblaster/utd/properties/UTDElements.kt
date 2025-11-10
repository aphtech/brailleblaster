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
package org.brailleblaster.utd.properties

import nu.xom.Element
import nu.xom.Node
import nu.xom.XPathContext
import org.brailleblaster.utd.internal.elements.*
import org.brailleblaster.utils.xml.UTD_NS

enum class UTDElements(val elementName: String) {
    NEW_PAGE("newPage"),
    MOVE_TO("moveTo"),
    BRL("brl"),
    BRL_PAGE_NUM("brlPageNum"),
    PRINT_PAGE_NUM("printPageNum"),
    BRLONLY("brlonly"),
    META("meta"),
    TAB("tab"),
    TABLE_DIVIDER("span"),
    NEW_LINE("newLine");

    val qName: String
        get() = "$UTD_PREFIX:$elementName"

    fun create(): Element {
        return when (this) {
            BRL -> Brl()
            MOVE_TO -> MoveTo()
            BRLONLY -> BrlOnly()
            NEW_PAGE -> NewPage()
            BRL_PAGE_NUM -> BrlPageNumber()
            PRINT_PAGE_NUM -> PrintPageNumber()
            META -> Meta()
            TAB -> Tab()
            TABLE_DIVIDER -> TableDivider()
            else -> Element(String.format("utd:%s", elementName), UTD_NS)
        }
    }

    fun isA(node: Node?): Boolean {
        var result = false
        if (node is Element) {
            if (UTD_NS == node.namespaceURI && elementName == node.localName) {
                result = true
            }
        }
        return result
    }

    companion object {
        const val UTD_PREFIX = "utd"
        @JvmField
        val UTD_XPATH_CONTEXT = XPathContext("utd", UTD_NS)
        const val UTD_ACTION_ATTRIB = "utd-action"
        const val UTD_STYLE_ATTRIB = "utd-style"
        const val UTD_SKIP_LINES_ATTRIB = "skipLines"
        fun getByName(name: String): UTDElements? {
            for (element in entries) {
                if (element.elementName == name) {
                    return element
                }
            }
            return null
        }

        @JvmStatic
        fun findType(e: Element): UTDElements? {
            if (UTD_NS == e.namespaceURI) {
                for (curType in entries) {
                    if (curType.elementName == e.localName) {
                        return curType
                    }
                }
            }
            return null
        }
    }
}