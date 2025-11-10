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
package org.brailleblaster.utd.internal.elements

import nu.xom.Attribute
import nu.xom.Element
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.xml.UTD_NS

class Brl : Element(PROTOTYPE) {
    var indexArray: List<Int>? = emptyList()
    set(value) {
        if (value == null) {
            val attr = getAttribute("index")
            if (attr != null) {
                super.removeAttribute(attr)
            }
        } else {
            val indexStr = value.joinToString(separator = " ") { it.toString() }
            super.addAttribute(Attribute("index", indexStr))
        }
        field = value
    }

    var type: String?
        get() {
            var type = getAttributeValue("type")
            if (type == null) {
                type = "text"
            }
            return type
        }
        set(type) {
            addAttribute(Attribute("type", type))
        }

    override fun addAttribute(attribute: Attribute) {
        if (attribute.namespaceURI.isEmpty() && "index" == attribute.localName) {
            val value = attribute.value
            indexArray = convertIndexStringToArray(value)
        }
        super.addAttribute(attribute)
    }

    override fun removeAttribute(attribute: Attribute): Attribute {
        if (attribute.namespaceURI.isEmpty() && "index" == attribute.localName) {
            indexArray = null
        }
        return super.removeAttribute(attribute)
    }

    companion object {
        private val PROTOTYPE = Element(UTDElements.BRL.qName, UTD_NS)
        fun convertIndexStringToArray(value: String): List<Int> {
            return value.split(' ').mapNotNull { it.toIntOrNull() }
        }
    }

    init {
        addAttribute(Attribute("xml:space", "http://www.w3.org/XML/1998/namespace", "preserve"))
    }
}