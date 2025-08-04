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
package org.brailleblaster.perspectives.braille.mapping.elements

import nu.xom.Document
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.bbx.BBX
import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.interfaces.Uneditable
import org.brailleblaster.perspectives.mvc.modules.misc.TableSelectionModule.Companion.displayInvalidTableMessage
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.properties.UTDElements
import kotlin.math.max

open class TableCellTextMapElement : TextMapElement, ITableCell, Uneditable {
    override lateinit var lines: LinkedHashMap<Double, Any>
    var brlLines: LinkedHashMap<Double, String>? = null
    var guideDots: MutableList<GuideDot> = ArrayList()
    var prev: TableCellTextMapElement? = null
    var startingVPos: Double = 0.0
        protected set
    var endingVPos: Double = 0.0
        protected set
    var endingMoveTo: Element? = null
        protected set
    var hPos: Double = 0.0 //hPos for rendering on print side
    var actualHPos: Double = 0.0 //hPos for accurate rendering on braille side
        protected set
    var width: Int = 0
        protected set
    lateinit var parentTableMapElement: TableTextMapElement

    constructor(start: Int, end: Int, n: Node?) : super(start, end, n)

    constructor(n: Node?) : super(n)

    override fun splitIntoLines(manager: Manager, maxWidth: Int, isLastColumn: Boolean) {
        lines = LinkedHashMap()
        brlLines = LinkedHashMap()
        guideDots = ArrayList()
        if (brailleList.isEmpty()) return
        var brlNode = brailleList[0].node.parent as Element
        while (brlNode.getAttribute("index") == null) {
            brlNode = brlNode.parent as Element
        }
        val indexes = getIndexes(brlNode)
        var totalLength = 0
        var start = 0
        for (bme in brailleList) {
            if (bme is NewPageBrlMapElement) continue
            val bmeIndex = bme.node.parent.indexOf(bme.node)
            var moveTo: Element? = null
            if (bmeIndex > 0) {
                if (UTDElements.MOVE_TO.isA(bme.node.parent.getChild(bmeIndex - 1))) {
                    moveTo = bme.node.parent.getChild(bmeIndex - 1) as Element
                    endingMoveTo = moveTo
                }
            }
            if (moveTo == null && prev != null && prev!!.endingMoveTo != null) {
                moveTo = prev!!.endingMoveTo
                endingMoveTo = moveTo
            } else if (moveTo == null) continue
            var spacing = ""
            val vPos = moveTo!!.getAttributeValue("vPos").toDouble()
            if (start == 0) {
                startingVPos = vPos
                if (prev != null && prev!!.col == col && prev!!.row == row) {
                    hPos = prev!!.hPos
                    if (hPos < moveTo.getAttributeValue("hPos").toDouble()) {
                        spacing = "  "
                    }
                } else {
                    hPos = moveTo.getAttributeValue("hPos").toDouble()
                }
                actualHPos = hPos
            } else {
                if (moveTo.getAttributeValue("hPos").toDouble() > hPos) {
                    spacing = "  "
                }
            }
            val brlValue = bme.node.value
            var printValue = ""

            if (bme is BraillePageBrlMapElement || bme is PrintPageBrlMapElement || bme is BrlOnlyBrlMapElement) {
                continue
            } else {
                totalLength += brlValue.length
                if (totalLength == indexes.size) {
                    printValue += spacing + node.value.substring(indexes[start])
                    endingVPos = vPos
                } else {
                    if (ancestorIsMath(node)) {
                        val brailleLength = MathModule.getMathText(node)
                        printValue += brailleLength
                        endingVPos = vPos
                    } else {
                        printValue += spacing + node.value.substring(indexes[start], indexes[totalLength])
                    }
                }
            }
            width = max(width.toDouble(), printValue.length.toDouble()).toInt()
            if (printValue.isNotEmpty()) lines[vPos] = printValue
            brlLines!![vPos] = spacing + brlValue
            start = totalLength
        }
        //Add guide dots
        for (bme in brailleList) {
            if (bme is BrlOnlyBrlMapElement) {
                if (bme !is RunningHeadBrlMapElement) {
                    val newGuideDots = GuideDot(bme.hPos, bme.vPos, bme.node)
                    if (bme.vPos < startingVPos && prev != null) {
                        //Edge case where the guide dots in this element are for the previous row
                        while (prev != null && prev!!.row == row && prev!!.col == col) {
                            prev = prev!!.prev
                        }
                        if (prev != null) {
                            prev!!.guideDots.add(newGuideDots)
                        } else {
                            guideDots.add(newGuideDots)
                        }
                    } else {
                        guideDots.add(newGuideDots)
                    }
                }
            }
        }
    }

    protected fun getIndexes(brl: Element): IntArray {
        try {
            val indexes = brl.getAttributeValue("index")
            //Workaround for MathML inside tables which is missing the indexes
            //TODO: Not sure if this is going to break anything
            if (indexes.isEmpty()) {
                return IntArray(0)
            }
            val indexArray = indexes.split(" ")
            val returnArray = IntArray(indexArray.size)
            for (i in indexArray.indices) {
                returnArray[i] = indexArray[i].toInt()
            }
            return returnArray
        } catch (e: Exception) {
            throw NodeException("brl index is ok?", brl, e)
        }
    }

    val row: Int
        get() {
            val td = tDElement
            val rowCol = td.getAttributeValue("row-col")
            return rowCol.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].toInt()
        }

    val col: Int
        get() {
            val td = tDElement
            val rowCol = td.getAttributeValue("row-col")
            return rowCol.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].toInt()
        }

    private val tDElement: Element
        get() {
            if (BBX.BLOCK.TABLE_CELL.isA(node)) return node as Element
            var parent = node.parent as Element
            while (!(BBX.BLOCK.TABLE_CELL.isA(parent)) && parent.getAttribute("row-col") == null) {
                if (parent.parent is Document) {
                    throw NodeException("Can't find TABLE_CELL block ancestor with rol-col attrib", node)
                }
                parent = parent.parent as Element
            }
            return parent
        }

    class GuideDot(val hPos: Double, val vPos: Double, val node: Node)

    override fun isImage(lines: LinkedHashMap<Double, Any>): Boolean {
        return false
    }

    override fun blockEdit(m: Manager) {
        displayInvalidTableMessage(m.wpManager.shell)
    }
}
