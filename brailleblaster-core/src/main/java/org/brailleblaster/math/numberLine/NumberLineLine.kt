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

import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.spatial.NemethTranslations
import org.brailleblaster.math.spatial.UebTranslations

class NumberLineLine {
    var type: LineChar
    var length = 1

    enum class LineChar {
        ARROW_END,
        ARROW_START,
        INTERVAL,
        LINE_FILL,
        LINE_EMPTY,
        FILL_CIRCLE,
        EMPTY_CIRCLE,
        HORIZONTAL_MODE,
        DOUBLE_ARROW_END,
        DOUBLE_ARROW_START;

        companion object {
            fun getString(c: LineChar?): String {
                var s = ""
                when (c) {
                    ARROW_END -> s =
                        if (MathModuleUtils.isNemeth) NemethTranslations.END_ARROW else UebTranslations.END_ARROW

                    ARROW_START -> s =
                        if (MathModuleUtils.isNemeth) NemethTranslations.BEGIN_ARROW else UebTranslations.BEGIN_ARROW

                    DOUBLE_ARROW_END -> s =
                        if (MathModuleUtils.isNemeth) NemethTranslations.DOUBLE_END_ARROW else UebTranslations.END_ARROW

                    DOUBLE_ARROW_START -> s =
                        if (MathModuleUtils.isNemeth) NemethTranslations.DOUBLE_BEGIN_ARROW else UebTranslations.BEGIN_ARROW

                    EMPTY_CIRCLE -> s =
                        if (MathModuleUtils.isNemeth) NemethTranslations.EMPTY_CIRCLE_ASCII else UebTranslations.EMPTY_ASCII

                    FILL_CIRCLE -> s =
                        if (MathModuleUtils.isNemeth) NemethTranslations.FILL_CIRCLE_ASCII else UebTranslations.FILL_ASCII

                    HORIZONTAL_MODE -> s =
                        if (MathModuleUtils.isNemeth) NemethTranslations.LINE_ASCII else UebTranslations.HORIZONTAL_MODE

                    INTERVAL -> s =
                        if (MathModuleUtils.isNemeth) NemethTranslations.INTERVAL_ASCII else UebTranslations.INTERVAL_ASCII

                    LINE_EMPTY -> s =
                        if (MathModuleUtils.isNemeth) NemethTranslations.LINE_ASCII else UebTranslations.LINE_ASCII

                    LINE_FILL -> s =
                        if (MathModuleUtils.isNemeth) NemethTranslations.LINE_FILLED_ASCII else UebTranslations.LINE_ASCII

                    else -> {}
                }
                return s
            }

            fun isArrow(c: LineChar): Boolean {
                return c == DOUBLE_ARROW_END || c == DOUBLE_ARROW_START || c == ARROW_END || c == ARROW_START
            }
        }
    }

    constructor(type: LineChar) {
        this.type = type
    }

    constructor(type: LineChar, length: Int) {
        this.type = type
        this.length = length
    }

    val totalLength: Int
        get() = length * LineChar.getString(type).length

    companion object {
        fun getLineLength(array: ArrayList<NumberLineLine>): Int {
            var length = 0
            for (numberLineLine in array) {
                length += numberLineLine.totalLength
            }
            return length
        }
    }
}
