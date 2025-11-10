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
package org.brailleblaster.math.template

import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.spatial.Line
import org.brailleblaster.math.spatial.NemethTranslations
import org.brailleblaster.math.spatial.SpatialMathEnum.Passage
import org.brailleblaster.math.spatial.UebTranslations

class TemplateFraction(
    val whole: TemplateNumber? = null,
    val num: TemplateNumber? = null,
    val den: TemplateNumber? = null,
    private var fractionString: String = NemethTranslations.NEMETH_FRAC,
    private var passage: Passage = Passage.NONE,
) {

    var wholePadding = 0
    var numPadding = 0
    var denPadding = 0

    var isMixed = whole?.toStringPart()?.isNotBlank() == true
    var isColumnHasMixed = false

    fun addToLine(line: Line): Line {
        val whole = whole
        val numerator = num
        val denominator = den
        if (wholeStart.isNotEmpty()) {
            line.elements.add(line.getTextSegment(wholeStart))
        }
        line.elements.add(line.getWhitespaceSegment(wholePadding + whole!!.leftPadding))
        line.elements.add(line.getTextSegment(whole.toStringPart().trim { it <= ' ' }))
        line.elements.add(line.getWhitespaceSegment(whole.rightPadding))
        if (fractionStart.isNotEmpty()) {
            line.elements.add(line.getTextSegment(fractionStart))
        }
        line.elements.add(line.getWhitespaceSegment(numPadding + numerator!!.leftPadding))
        line.elements.add(line.getTextSegment(numerator.toStringPart().trim { it <= ' ' }))
        line.elements.add(line.getWhitespaceSegment(numerator.rightPadding))
        if (fractionSymbol.isNotEmpty()) {
            line.elements.add(line.getTextSegment(fractionSymbol))
        }
        line.elements.add(line.getWhitespaceSegment(denPadding + denominator!!.leftPadding))
        line.elements.add(line.getTextSegment(denominator.toStringPart().trim { it <= ' ' }))
        line.elements.add(line.getWhitespaceSegment(denominator.rightPadding))
        if (fractionEnd.isNotEmpty()) {
            line.elements.add(line.getTextSegment(fractionEnd))
        }
        return line
    }

    val wholeStart: String
        get() = if (!MathModuleUtils.isNemeth && passage != Passage.NUMERIC && whole!!.toStringPart().isNotEmpty()) {
            UebTranslations.NUMBER_CHAR
        } else ""
    val fractionStart: String
        get() {
            if (MathModuleUtils.isNemeth) {
                if (num!!.toStringPart().isNotBlank()) {
                    return if (isMixed) {
                        NemethTranslations.NEMETH_START_MIXED_FRAC
                    } else {
                        if (isColumnHasMixed) {
                            (" ".repeat(NemethTranslations.NEMETH_START_MIXED_FRAC.length
                                - NemethTranslations.NEMETH_START_SIMPLE_FRAC.length)
                            + NemethTranslations.NEMETH_START_SIMPLE_FRAC)
                        } else {
                            NemethTranslations.NEMETH_START_SIMPLE_FRAC
                        }
                    }
                }
            } else {
                if (passage != Passage.NUMERIC) {
                    return if (num!!.toStringPart().isNotEmpty()) (if (whole!!.toStringPart().isNotEmpty()
                    ) UebTranslations.NUMBER_CHAR else "") else ""
                } else {
                    if (num!!.toStringPart().isNotBlank()) {
                        return if (whole!!.toStringPart().isBlank()) {
                            " "
                        } else {
                            UebTranslations.NUMBER_CHAR
                        }
                    }
                }
            }
            return ""
        }
    val fractionSymbol: String
        get() = if (num!!.toStringPart().isNotEmpty()) fractionString else ""
    val fractionEnd: String
        get() {
            if (MathModuleUtils.isNemeth) {
                if (num!!.toStringPart().isNotBlank()) {
                    return if (isMixed) {
                        NemethTranslations.NEMETH_END_MIXED_FRAC
                    } else {
                        if (isColumnHasMixed) {
                            (" ".repeat(
                            NemethTranslations.NEMETH_END_MIXED_FRAC.length
                                - NemethTranslations.NEMETH_END_SIMPLE_FRAC.length
                            )
                            + NemethTranslations.NEMETH_END_SIMPLE_FRAC)
                        } else {
                            NemethTranslations.NEMETH_END_SIMPLE_FRAC
                        }
                    }
                }
            }
            return ""
        }

    override fun toString(): String {
        return if (MathModuleUtils.isNemeth) {
            val fractionStart = fractionStart
            val fractionSymbol = if (num!!.toStringPart().isNotEmpty()) fractionString else ""
            val fractionEnd = fractionEnd
            (" ".repeat(wholePadding) + whole!!.toStringPart() + fractionStart
                    + " ".repeat(numPadding) + num.toStringPart() + fractionSymbol
                    + " ".repeat(denPadding) + den!!.toStringPart() + fractionEnd)
        } else {
            if (passage == Passage.NUMERIC) {
                val fractionSymbol = if (num!!.toStringPart().isNotEmpty()) fractionString else ""
                (" ".repeat(wholePadding) + whole!!.toStringPart()
                    + " ".repeat(numPadding) + num.toStringPart() + fractionSymbol
                    + " ".repeat(denPadding) + den!!.toStringPart())
            } else {
                val wholeStart = wholeStart
                val fractionStart = fractionStart
                val fractionSymbol = if (num!!.toStringPart().isNotEmpty()) fractionString else ""
                (wholeStart + " ".repeat(wholePadding)
                    + whole!!.toStringPart() + fractionStart
                    + " ".repeat(numPadding) + num.toStringPart() + fractionSymbol
                    + " ".repeat(denPadding) + den!!.toStringPart())
            }
        }
    }

    companion object {
        fun makeColumnsEqualWidth(array: ArrayList<TemplateFraction>) {
            var widestWhole = 0
            var widestNum = 0
            var widestDen = 0
            var columnHasMixed = false
            for (fraction in array) {
                if (fraction.isMixed) {
                    columnHasMixed = true
                }
                if (fraction.whole!!.toStringPart().length > widestWhole) {
                    widestWhole = fraction.whole.toStringPart().length
                }
                if (fraction.num!!.toStringPart().length > widestNum) {
                    widestNum = fraction.num.toStringPart().length
                }
                if (fraction.den!!.toStringPart().length > widestDen) {
                    widestDen = fraction.den.toStringPart().length
                }
            }
            for (fraction in array) {
                fraction.isColumnHasMixed = columnHasMixed
                fraction.wholePadding = widestWhole - fraction.whole!!.toStringPart().length
                fraction.numPadding = widestNum - fraction.num!!.toStringPart().length
                fraction.denPadding = widestDen - fraction.den!!.toStringPart().length
            }
        }

        fun getNumerators(templateLine: List<TemplateFraction>): List<TemplateNumber> {
            return templateLine.mapNotNull { obj: TemplateFraction -> obj.num }.toList()
        }

        fun getWhole(templateLine: List<TemplateFraction>): List<TemplateNumber> {
            return templateLine.mapNotNull { t: TemplateFraction -> t.whole }.toList()
        }

        fun getDenominators(templateLine: List<TemplateFraction>): List<TemplateNumber> {
            return templateLine.mapNotNull { obj: TemplateFraction -> obj.den }.toList()
        }
    }
}
