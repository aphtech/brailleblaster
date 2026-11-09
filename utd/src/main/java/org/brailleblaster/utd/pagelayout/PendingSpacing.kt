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
package org.brailleblaster.utd.pagelayout

data class PendingSpacing(
    var linesBefore: Int = 0,
    var linesAfter: Int = 0,
    var newLinesOverride: Int = 0, // When 0 no override
    var keptNewLines: Int = 0, // How many of newLinesOverride survive a forced page break
    var spaces: Int = 0,
    var pages: Int = 0,
    var explicitPages: Int = 0
) {

    fun addExplicitPages(pages: Int) {
        explicitPages += pages
        addAtLeastPages(explicitPages)
    }

    fun addAtLeastPages(numOfNewPages: Int) {
        if (numOfNewPages > pages) pages = numOfNewPages
    }

    fun addAtLeastLinesBefore(numOfNewLines: Int) {
        if (numOfNewLines > linesBefore) linesBefore = numOfNewLines
    }

    fun addAtLeastLinesAfter(numOfNewLines: Int) {
        if (numOfNewLines > linesAfter) linesAfter = numOfNewLines
    }

    fun removePage(i: Int) {
        pages -= i
    }

    fun removeSpace(i: Int) {
        spaces -= i
    }

    fun updateSpaces(requestedSpaces: Int) {
        if (requestedSpaces > spaces) {
            spaces = requestedSpaces
        }
    }
}