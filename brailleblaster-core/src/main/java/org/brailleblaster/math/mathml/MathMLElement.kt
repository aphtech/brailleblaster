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

import nu.xom.Node
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.slf4j.LoggerFactory

class MathMLElement(math: Node?) : TextMapElement(math) {
    override fun getText(): String {
        val string = MathModuleUtils.getMathText(node)
        if (string.isEmpty()) {
            log.error("AsciiMath attribute is empty on a math tag")
        }
        return string
    }

    companion object {
        private val log = LoggerFactory.getLogger(MathMLElement::class.java)
    }
}