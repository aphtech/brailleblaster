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
import org.brailleblaster.utils.UnitConverter.Companion.newDecimalFormatUS
import java.math.BigDecimal

class MoveTo @JvmOverloads constructor(hPos: BigDecimal = BigDecimal.ZERO, vPos: BigDecimal = BigDecimal.ZERO) : Element(PROTOTYPE) {
    private lateinit var _hPos: BigDecimal
    private lateinit var _vPos: BigDecimal
    var hPos: BigDecimal
    get() = _hPos
        set(hPos) {
            _hPos = hPos
            super.addAttribute(Attribute("hPos", posFormatter.format(hPos)))
        }
    var vPos: BigDecimal
    get() = _vPos
        set(vPos) {
            _vPos = vPos
            super.addAttribute(Attribute("vPos", posFormatter.format(vPos)))
        }

    override fun addAttribute(attribute: Attribute) {
        when (attribute.localName) {
            "hPos" -> hPos = newBigDecimal(attribute.value)
            "vPos" -> vPos = newBigDecimal(attribute.value)
            else -> super.addAttribute(attribute)
        }
    }

    override fun removeAttribute(attribute: Attribute): Attribute {
        val attrName = attribute.localName
        require(attrName != "hPos") { "Cannot remove hPos from a moveTo element" }
        require(attrName != "vPos") { "Cannot remove vPos from a moveTo element" }
        return super.removeAttribute(attribute)
    }

    companion object {
        private val PROTOTYPE = Element(UTDElements.MOVE_TO.qName, UTD_NS)
        private val posFormatter = newDecimalFormatUS("0.##")
        private fun newBigDecimal(text: String): BigDecimal {
            return try {
                BigDecimal(text)
            } catch (e: NumberFormatException) {
                throw RuntimeException("Failed to parse number '$text'", e)
            }
        }
    }
    init {
        this.hPos = hPos
        this.vPos = vPos
    }
}