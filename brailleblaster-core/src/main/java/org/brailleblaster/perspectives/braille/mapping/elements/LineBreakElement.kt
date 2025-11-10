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
package org.brailleblaster.perspectives.braille.mapping.elements

import nu.xom.Element
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.util.LINE_BREAK

class LineBreakElement(lineBreak: Element) : WhiteSpaceElement(lineBreak) {
    /**
     * @return true if this map element occurs at the end of a line
     */
    var isEndOfLine: Boolean

    init {
        require(UTDElements.NEW_LINE.isA(lineBreak)) { "lineBreak cannot be null and must be a line break" }
        isEndOfLine = false
    }

    override fun getNodeParent(): Element {
        return node as Element
    }

    override fun getText(): String {
        return ""
    }

    override fun getStart(list: MapList): Int {
        val index = list.indexOf(this)
        if (index <= 0) {
            return 0
        }
        return if (isEndOfLine || !isFullyVisible) {
            list[index - 1].getEnd(list)
        } else {
            list[index - 1].getEnd(list) + LINE_BREAK.length
        }
    }

    override fun getEnd(list: MapList): Int {
        return getStart(list)
    }
}
