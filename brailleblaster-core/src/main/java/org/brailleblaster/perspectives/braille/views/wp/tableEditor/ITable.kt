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
import org.brailleblaster.utd.internal.xml.XMLHandler
import java.util.function.Consumer
import kotlin.math.max

internal interface ITable {
    val nodes: MutableList<MutableList<Node>>

    val captions: List<Element>

    var tNContainer: Element?

    var type: TableType

    var displayedRows: Int

    var displayedCols: Int

    var rows: Int
        get() {
        return nodes.size
    }
        set(rows) {
        while (rows > nodes.size) {
            nodes.add(ArrayList())
        }
        while (rows < nodes.size) {
            nodes.removeAt(nodes.size - 1)
        }
    }

    var cols: Int
    get() {
        if (nodes.isEmpty()) return 0
        var maxCol = 0
        for (row in nodes) {
            maxCol = max(row.size.toDouble(), maxCol.toDouble()).toInt()
        }
        return maxCol
    }
        set(cols) {
        for (row in nodes) {
            while (cols > row.size) {
                row.add(
                    XMLHandler.newElement(
                        nodes[0][0] as Element,
                        "td"
                    )
                )
            }
            while (cols < row.size) {
                row.removeAt(row.size - 1)
            }
        }
    }

    val displayedNodes: List<List<Node>>


    //toString() cannot be overriden in a default method
    fun toPrintableState(): String {
        val sb = StringBuilder()
        sb.append(this.javaClass.simpleName).append(": Type: ")
            .append(type).append("\nTN Heading:\n")
            .append(if (tNContainer == null) "null" else tNContainer!!.toXML())
            .append("\nRows (Displayed): ")
            .append(rows)
            .append("(")
            .append(displayedRows)
            .append(") Cols (Displayed): ")
            .append(cols)
            .append("(")
            .append(displayedCols)
            .append(")")
        sb.append("\nNodes:")
        nodes.forEach(Consumer { r: List<Node> ->
            sb.append("\nRow:")
            r.forEach(Consumer { col: Node -> sb.append("\n").append(col.value) })
        })
        sb.append("\nCaptions:")
        captions.forEach(Consumer { c: Element -> sb.append("\n").append(c.value) })
        return sb.toString()
    }
}
