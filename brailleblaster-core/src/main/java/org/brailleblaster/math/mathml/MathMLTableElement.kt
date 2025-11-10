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
package org.brailleblaster.math.mathml

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.perspectives.braille.mapping.elements.ITableCell
import org.brailleblaster.perspectives.braille.mapping.elements.TableCellTextMapElement
import org.slf4j.LoggerFactory

class MathMLTableElement(var math: MathMLElement, n: Node?) : TableCellTextMapElement(n), ITableCell {
    init {
        brailleList = math.brailleList
    }

    override fun getText(): String {
        val attribute = (node as Element).getAttribute(MathModuleUtils.MATH_ATTRIBUTE)
        return if (attribute == null) {
            log.error("AsciiMath attribute is null on a math tag")
            ""
        } else {
            attribute.value
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(MathMLTableElement::class.java)
    }
}