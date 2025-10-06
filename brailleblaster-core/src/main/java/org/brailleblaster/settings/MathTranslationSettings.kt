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
package org.brailleblaster.settings

import org.brailleblaster.utd.BrailleSettings

object MathTranslationSettings {
    private const val NEMETH = "nemeth.ctb"
    private const val UEB = "en-ueb-math.ctb"
    private const val UEB_PLUS_NEMETH = "nemeth-ueb.ctb"
    fun getMathTable(settings: BrailleSettings): String {
        val mathTable = settings.mathExpressionTable.split(",".toRegex()).dropLastWhile { it.isEmpty() }.first()
        return when (mathTable) {
            NEMETH -> MathTable.MATH_NEMETH.name
            UEB -> MathTable.MATH_UEB.name
            UEB_PLUS_NEMETH -> MathTable.MATH_UEB_PLUS_NEMETH.name
                else -> MathTable.MATH_NEMETH.name
        }
    }

    enum class MathTable {
        MATH_NEMETH,
        MATH_UEB,
        MATH_UEB_PLUS_NEMETH,
        NONE
    }
}
