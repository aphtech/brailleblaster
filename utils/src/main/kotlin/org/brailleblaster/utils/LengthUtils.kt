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
package org.brailleblaster.utils

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

object LengthUtils {
    fun inchesToMM(inches: Double): Double {
        val denominator = 0.039370
        return (inches / denominator * 10.0).roundToInt() / 10.0
    }

    fun mmToInches(mm: Double): Double {
        val multiplier = 0.039370
        return (mm * multiplier * 10000.0).roundToInt() / 10000.0
    }

    fun toLengthBigDecimal(length: Double): BigDecimal {
        return BigDecimal(length).setScale(2, RoundingMode.HALF_UP)
    }

    fun toLengthBigDecimal(length: String?): BigDecimal {
        return BigDecimal(length).setScale(2, RoundingMode.HALF_UP)
    }

    enum class Units(private val multiplier: Int) {
        MILLIMETRES(1000), INCHES(25400);

        fun asUnits(micrometres: Int): Double {
            return micrometres.toDouble() / multiplier.toDouble()
        }

        fun fromUnits(measure: Double): Int {
            return (measure * multiplier).toInt()
        }
    }
}