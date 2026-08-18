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

enum class PageNumberPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, NONE;

    val isTop: Boolean
        get() = this == TOP_LEFT || this == TOP_RIGHT
    val isBottom: Boolean
        get() = this == BOTTOM_LEFT || this == BOTTOM_RIGHT
    val isLeft: Boolean
        get() = this == BOTTOM_LEFT || this == TOP_LEFT
    val isRight: Boolean
        get() = this == TOP_RIGHT || this == BOTTOM_RIGHT

    fun differentLineAs(position: PageNumberPosition): Boolean {
        return isTop && position.isBottom || isBottom && position.isTop
    }
}