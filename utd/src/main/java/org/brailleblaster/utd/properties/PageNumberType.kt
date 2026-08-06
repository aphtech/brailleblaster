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
package org.brailleblaster.utd.properties

enum class PageNumberType {
    NORMAL, T_PAGE, P_PAGE;

    fun getFormattedPageNumber(number: String): String {
        return getFormattedPageNumber(number.toInt())
    }

    fun getFormattedPageNumber(number: Int): String {
        val numberString: String = when (this) {
            T_PAGE -> String.format("t%d", number)
            P_PAGE -> String.format("p%d", number)
            NORMAL -> number.toString()
        }
        return numberString
    }

    companion object {
        fun equivalentPage(pageType: String?): PageNumberType = when (pageType) {
            "T_PAGE" -> T_PAGE
            "P_PAGE" -> P_PAGE
            else -> NORMAL
        }
    }
}