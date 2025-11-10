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

import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.util.LINE_BREAK

/*
 * Running heads and guide words are painted to the view and leave a newline 
 * character in the view. This newline character should not be considered in 
 * editing calculations.
 */
class PaintedWhiteSpaceElement : FormattingWhiteSpaceElement {
    constructor(start: Int, end: Int) : super(start, end)

    constructor() : super()

    override fun getStart(list: MapList): Int {
        if (list.indexOf(this) > 0) {
            return list[list.indexOf(this) - 1].getEnd(list) + LINE_BREAK.length
        }
        return 0
    }

    override fun getEnd(list: MapList): Int {
        return getStart(list)
    }
}
