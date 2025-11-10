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

class TableDivider() : Element(PROTOTYPE) {
    enum class DividerTypes(var attrValue: String, var value: String?) {
        LISTED_COLON("ltColon", ": "), SIMPLE_SEP_LINE("sepline", null), LINEAR_COLON("linearPunctuation", ": "), LINEAR_SEMICOLON("linearPunctuation", "; "), LINEAR_PERIOD("linearPunctuation", ". ");
    }

    constructor(type: DividerTypes) : this() {
        setType(type)
    }

    fun setType(type: DividerTypes): Element {
        addAttribute(Attribute(ATTR_NAME, type.attrValue))
        if (type.value != null) {
            appendChild(type.value)
        }
        return this
    }

    companion object {
        private val PROTOTYPE = Element(UTDElements.TABLE_DIVIDER.qName, UTD_NS)
        private const val ATTR_NAME = "class"
    }
}