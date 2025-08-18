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
package org.brailleblaster.perspectives.braille.views.wp.tableEditor

import nu.xom.Element
import nu.xom.Node
import kotlin.math.max

internal class InternalFacingTable(
    override val nodes: MutableList<MutableList<Node>>,
    @JvmField var split: Int,
    override val captions: List<Element>,
    override var type: TableType
) : ITable {
    val leftNodes: List<List<Node>>
        get() {
            val returnList: MutableList<List<Node>> = ArrayList()
            for (row in nodes) {
                val cellList = row.take(split)
                returnList.add(cellList)
            }
            return returnList
        }

    val rightNodes: List<List<Node>>
        get() {
            val returnList: MutableList<List<Node>> = ArrayList()
            for (row in nodes) {
                val cellList: MutableList<Node> = ArrayList()
                for (j in split until row.size) {
                    cellList.add(row[j])
                }
                returnList.add(cellList)
            }
            return returnList
        }

    override var rows: Int
        get() = nodes.size
        set(rows) {
            super.rows = rows
        }

    override var cols: Int
        get() {
            if (nodes.isEmpty()) return 0
            var maxCol = 0
            for (row in nodes) {
                maxCol = max(row.size.toDouble(), maxCol.toDouble()).toInt()
            }
            return maxCol
        }
        set(cols) {
            super.cols = cols
        }

    override var displayedRows: Int
        get() = rows
        set(displayedRows) {
            if (displayedRows > rows) rows = displayedRows
        }

    override var displayedCols: Int
        get() = cols
        set(displayedCols) {
            if (displayedCols > cols) cols = displayedCols
        }

    override val displayedNodes: List<List<Node>>
        get() = ArrayList(nodes)

    override var tNContainer: Element?
        get() = null
        set(_) {
            throw UnsupportedOperationException("TN Container not supported with facing tables yet")
        }
}
