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

import org.brailleblaster.libembosser.spi.BrlCell
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

/**
 * Provide conversions to local units as well as conversions to "cell units"
 */
class UnitConverter @JvmOverloads constructor(val isMetric: Boolean = Locale.getDefault().country != NONMETRIC_COUNTRY) {
    fun calculateLinesFromHeight(height: Double, cell: BrlCell): Int {
        return cell.getLinesForHeight(localUnitsToMM(height).toBigDecimal())
    }

    fun calculateCellsFromWidth(width: Double, cell: BrlCell): Int {
        return cell.getCellsForWidth(localUnitsToMM(width).toBigDecimal())
    }

    fun calculateWidthFromCells(numberOfCells: Int, cell: BrlCell): Double {
        val width = cell.getWidthForCells(numberOfCells).toDouble()
        return mmToLocalUnits(width)
    }

    fun calculateHeightFromLines(numberOfLines: Int, cell: BrlCell): Double {
        val height = cell.getHeightForLines(numberOfLines).toDouble()
        return mmToLocalUnits(height)
    }

    fun localUnitsToMM(distance: Double): Double {
        return if (!isMetric) {
            LengthUtils.inchesToMM(distance)
        } else distance
    }

    fun mmToLocalUnits(distance: Double): Double {
        return if (!isMetric) {
            LengthUtils.mmToInches(distance)
        } else distance
    }

    companion object {
        const val NONMETRIC_COUNTRY = "US"

        /**
         * Create a DecimalFormat using US formatted pattern instead of locale-specific potentially with
         * commas instead of decimals
         *
         * @param usFormattedPattern
         * @return
         */
        fun newDecimalFormatUS(usFormattedPattern: String?): DecimalFormat {
            return DecimalFormat(usFormattedPattern, DecimalFormatSymbols(Locale.ENGLISH))
        }
    }
}