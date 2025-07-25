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
import org.brailleblaster.utils.UTD_NS

class Tab : Element(PROTOTYPE) {
    var tabValue: String?
        get() {
            var type = getAttributeValue("tabValue")
            if (type == null) {
                type = "0"
            }
            return type
        }
        set(type) {
            addAttribute(Attribute("tabValue", type))
        }

    companion object {
        @JvmStatic
        private val PROTOTYPE = Element(UTDElements.TAB.qName, UTD_NS).apply { addAttribute(Attribute("xml:space", "http://www.w3.org/XML/1998/namespace", "preserve")) }
    }

}