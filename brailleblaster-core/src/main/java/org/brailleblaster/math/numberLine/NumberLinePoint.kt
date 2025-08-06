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
package org.brailleblaster.math.numberLine

import org.apache.commons.lang3.math.Fraction
import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.math.spatial.MathText
import org.brailleblaster.math.spatial.NemethTranslations
import org.brailleblaster.math.spatial.UebTranslations

class NumberLinePoint(
    var isMinus: Boolean = false,
    var fraction: Fraction? = null,
    var mathText: MathText = MathText(),
    private val format: Boolean = true
) {
    val leftDec: String
    val rightDec: String
    var leftPadding: Int = 0
    var rightPadding: Int = 0

    override fun toString(): String {
        return if (MathModule.isNemeth) {
            ""
        } else if (rightDec.isEmpty() && leftDec.isEmpty()) {
            ""
        } else if (!format) {
            " ".repeat(leftPadding) + rightDec + " ".repeat(rightPadding)
        } else {
            " ".repeat(leftPadding) + leftDec +
                UebTranslations.NUMBER_CHAR +
                rightDec + " ".repeat(rightPadding)
        }
    }

    init {
        val wholeString = mathText.braille
        //TODO better, setting agnostic parser
        if (wholeString.isEmpty()) {
            this.leftDec = ""
            this.rightDec = ""
        } else if (format) {
            val split = if (MathModule.isNemeth) wholeString.indexOf(NemethTranslations.MINUS)
            else wholeString.indexOf(UebTranslations.PRINT_MINUS)
            if (split != -1) {
                if (!MathModule.isNemeth) {
                    this.leftDec = wholeString.take(split + 1)
                    this.rightDec = wholeString.drop(split + 2)
                } else {
                    this.leftDec = wholeString.take(split + 1)
                    this.rightDec = wholeString.drop(split + 1)
                }
            } else {
                this.leftDec = ""
                if (MathModule.isNemeth) {
                    this.rightDec = wholeString
                } else {
                    this.rightDec = wholeString.substring(1)
                }
            }
        } else {
            this.leftDec = ""
            this.rightDec = wholeString
        }
    }
    companion object {
        fun makeColumnsEqualWidth(array: ArrayList<NumberLinePoint>) {
            val widestLeft = array.maxOf { it.leftDec.length }
            val widestRight = array.maxOf { it.rightDec.length }
            array.forEach { point ->
                point.leftPadding = widestLeft - point.leftDec.length
                point.rightPadding = widestRight - point.rightDec.length
            }
        }
    }
}
