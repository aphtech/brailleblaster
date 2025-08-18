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

import com.google.common.base.Preconditions
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.internal.elements.PageNumber
import org.brailleblaster.utd.properties.UTDElements
import java.util.*
import java.util.stream.Stream

/**
 * A representation of the cells of a page.
 */
class PageGrid(val width: Int, val height: Int) {
    private val grid: Array<Cell?> = arrayOfNulls(height * width)

    constructor(other: PageGrid) : this(other.width, other.height) {
        other.grid.copyInto(grid)
    }

    /**
     * Get the cell at page location.
     *
     * @param x The horizontal cell position.
     * @param y The line position.
     * @return The value of the cell at that location.
     */
    fun getCell(x: Int, y: Int): Cell? {
        Preconditions.checkElementIndex(x, width)
        Preconditions.checkElementIndex(y, height)
        return grid[y * width + x]
    }

    /**
     * Set the page location to the cell if there is no content.
     *
     * @param x The horizontal cell position.
     * @param y The line position.
     * @param cell The value to set the cell to.
     * @return Whether the cell is occupied, true if the cell has content already, otherwise false is
     * returned.
     */
    fun setCell(x: Int, y: Int, cell: Cell?): Boolean {
        return setCell(x, y, cell, false)
    }

    /**
     * Set the page location to the cell.
     *
     *
     * This allows setting the page location and optionally specifying whether any previous cell
     * content should be overwritten.
     *
     * @param x The horizontal cell position.
     * @param y The line position.
     * @param cell The value to set the cell to.
     * @param overWrite Whether any previous cell content should be overwritten.
     * @return Whether the location was previously occupied, true if there was previously content
     * otherwise false.
     */
    fun setCell(x: Int, y: Int, cell: Cell?, overWrite: Boolean): Boolean {
        Preconditions.checkElementIndex(y, height)
        Preconditions.checkElementIndex(x, width)
        val index = y * width + x
        var cellEmpty = true
        if (grid[index] != null) {
            cellEmpty = false
        }
        if (overWrite || cellEmpty) {
            grid[index] = cell
        }
        return !cellEmpty
    }

    /**
     * Report if the line is empty, ignoring page numbers on number lines.
     *
     * @param line The index of the line to be checked.
     * @return True if the line is empty or only contains page numbers, otherwise returns false.
     */
    fun isEmptyNumberLine(line: Int): Boolean {
        Preconditions.checkElementIndex(line, height)
        var result = true
        for (i in line * width until (line + 1) * width) {
            if (grid[i] != null
                    && (grid[i]!!.node is PageNumber
                            || grid[i]!!.node == null && grid[i]!!.index == -1)) { // NOPMD
                continue
            } else if (grid[i] != null) {
                result = false
                break
            }
        }
        return result
    }

    /**
     * Report if the line is empty.
     *
     * @param line The index of the line to be checked.
     * @return Returns true if there is no content on the line. Returns false for all other
     * situations.
     */
    fun isEmptyLine(line: Int): Boolean {
        Preconditions.checkElementIndex(line, height)
        var result = true
        for (i in line * width until (line + 1) * width) {
            if (grid[i] != null) {
                result = false
                break
            }
        }
        return result
    }

    /**
     * Sets every cell on the line to null.
     *
     * @param line The index of the line to clear.
     */
    fun clearLine(line: Int) {
        Preconditions.checkElementIndex(line, height)
        Arrays.fill(grid, line * width, (line + 1) * width, null)
    }

    /**
     * Update a number of cell locations in one go.
     *
     * @param x The horizontal cell position where insertion will start.
     * @param y The line on which insertion will happen.
     * @param cells An array of the cells to insert. If the array is too long then an exception will
     * be raised.
     */
    fun setCells(x: Int, y: Int, cells: Array<Cell?>) {
        val newX = x.coerceAtLeast(0)
        Preconditions.checkElementIndex(y, height)
        if (cells.isEmpty()) {
            return
        }
        require(newX + cells.size <= width) {
            String.format(
                    "%d is too many cells to insert starting at position %d on line, line length is only %d",
                    cells.size, newX, width)
        }
        System.arraycopy(cells, 0, grid, y * width + newX, cells.size)
    }

    /**
     * Update a number of cell locations in one go using a list of cells.
     *
     * @param x The horizontal cell position where insertion will start.
     * @param y The line on which insertion will happen.
     * @param cells A list of the cells to insert. If the list is too long then an exception will be
     * raised.
     */
    fun setCells(x: Int, y: Int, cells: List<Cell?>) {
        setCells(x, y, cells.toTypedArray())
    }

    /**
     * Check if there is any content on the page, ignoring page numbers.
     *
     * @return Returns true if the page has no content other than page numbers, otherwise returns
     * false.
     */
    val isBlankPage: Boolean
        get() = isEmptyLines(0, height)

    /**
     * Check if multiple lines are empty, ignoring page numbers.
     *
     *
     * This method will check a range of lines to test whether they are empty. This method will
     * ignore page numbers when determining whether a line is empty.
     *
     * @param start The first line to check.
     * @param end The last line to check.
     * @return Returns true if all lines are empty or only contain a page number, otherwise returns
     * false.
     */
    fun isEmptyLines(start: Int, end: Int): Boolean {
        for (i in start until end) {
            if (!isEmptyNumberLine(i)) return false
        }
        return true
    }

    val cells: Stream<Cell>
        get() = Arrays.stream(grid)
    val brlElementsOnPage: Iterable<Element>
        get() = grid
                .mapNotNull { it?.node }
                .distinct()
                .mapNotNull { it.parent}
            .filterIsInstance<Element>()
                .filter { UTDElements.BRL.isA(it) }

    fun getLine(i: Int): Stream<Cell?> {
        Preconditions.checkElementIndex(i, height)
        return Arrays.stream(grid, i * width, (i + 1) * width)
    }
fun getCellsForNode(node: Node): Iterable<Cell> {
    return grid.filterNotNull().filter { it.node == node }
}
}