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
package org.brailleblaster.perspectives.braille.views.wp

import nu.xom.Element
import org.brailleblaster.BBIni
import org.brailleblaster.utd.UTDTranslationEngine
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.libembosser.spi.BrlCell
import org.brailleblaster.util.LINE_BREAK
import java.util.*
import java.util.function.Consumer
import kotlin.math.max

class RendererState(engine: UTDTranslationEngine) {
    private val lines: MutableList<RenderObject> = ArrayList()
    private val cursor: Cursor
    private val cell: BrlCell
    private val linesPerPage: Int
    private var pastFirstNewPage = false //Skip the first new page
    private var _charCount = 0
    private var lineCount = 0
    private var tableMode = false
    private var lineIndentMode = true
    private var emphasisNode: Element? = null
    private var lineNumber = false

    init {
        cursor = Cursor()
        cell = engine.brailleSettings.cellType
        linesPerPage = cell.getLinesForHeight(engine.pageSettings.drawableHeight.toBigDecimal())
    }

    fun moveTo(hPos: Double, vPos: Double) {
        moveTo(hPosToCell(hPos), vPosToCell(vPos))
    }

    fun moveTo(x: Int, y: Int) {
        if (y < cursor.vPos && y != NO_CURSOR_POS) {
            if (!tableMode) {
                val ex =
                    IllegalArgumentException("Moved backwards without a newPage element. RendererState's Cursor vPos: " + cursor.vPos + ", moveTo vPos: " + y)
                if (BBIni.debugging) throw ex
                else ex.printStackTrace()
            }
        }

        while (cursor.vPos < y) {
            if (getLineAtY(cursor.vPos) == null) {
                createBlankLine(0, cursor.vPos)
            }
            cursor.vPos++
        }
        var curLine = getLineAtY(y)
        if (curLine == null) {
            curLine = createBlankLine(if (lineIndentMode) x else 0, y)
        } else if (curLine.startingHPos == NO_CURSOR_POS) {
            curLine.startingHPos = x
        }

        if (!lineNumber) {
            cursor.hPos = curLine.startingHPos + curLine.text.length
            if (cursor.hPos != NO_CURSOR_POS && x > 0) curLine.setSpaces(x - cursor.hPos) //Do not add the spaces to the line yet in case no more text is being added (happens with line numbers)

            cursor.hPos = x
            cursor.setPos(x, y)
        } else {
            setLineNumber(false)
        }

        pastFirstNewPage = true
    }

    fun addToLine(line: String) {
        if (lines.isEmpty()) {
            lines.add(Line(0, 0))
            lineCount++
        }

        val curLine = (if (tableMode) getLineAtY(cursor.vPos) else previousLine)!!
        if (curLine.pendingHorizontalSpaces > 0) {
            curLine.addSpacesToLine()
        }
        val emphasized = emphasisNode != null
        if (emphasized) {
            val newEmphasis = Emphasis(emphasisNode)
            newEmphasis.start = curLine.text.length
            curLine.emphasisList.add(newEmphasis)
        }
        curLine.text += line
        if (emphasized) {
            curLine.emphasisList.last().end = curLine.text.length
            emphasisNode = null
        }
        cursor.hPos += line.length
        _charCount += line.length
    }

    fun newPage(newPage: Element?) {
        require(!(newPage == null || !UTDElements.NEW_PAGE.isA(newPage))) { "newPage must be a newPage element" }
        if (pastFirstNewPage) {
            while (cursor.vPos + 1 < linesPerPage) {
                moveTo(0, cursor.vPos + 1)
            }
        }
        lines.add(NewPage(newPage))
        cursor.reset()
        pastFirstNewPage = true
    }

    val text: String
        get() {
            val lastLine = previousLine
            val text = StringBuilder()
            for (line in lines) {
                if (line is Line) {
                    line.startOffset = text.length
                    val lineText = if (lastLine === line) line.text else "${line.text}$LINE_BREAK"
                    text.append(lineText)
                }
            }
            return text.toString()
        }

    val emphasis: List<Emphasis>
        get() {
            val emphasisList: MutableList<Emphasis> = ArrayList()
            for (`object` in lines) {
                if (`object` is Line) {
                    for (emphasis in `object`.emphasisList) {
                        emphasis.start += `object`.startOffset
                        emphasis.end += `object`.startOffset
                        emphasisList.add(emphasis)
                    }
                }
            }
            return emphasisList
        }

    val charCount: Int
        get() {
            return (_charCount + (LINE_BREAK.length * max(
                (lineCount - 1).toDouble(),
                0.0
            ))).toInt()
        }

    val charCountWithPendingSpaces: Int
        get() = charCount + previousLine!!.pendingHorizontalSpaces

    /**
     * Used for tables to get a correct character count as they move backwards.
     * Much less efficient than using getCharCount()
     */
    fun getCharCountToVPos(vPos: Int): Int {
        var foundLineAtVPos = false
        var count = 0
        for (curLine in lines.reversed()) {
            if (curLine is Line) {
                if (!foundLineAtVPos) {
                    if (curLine.vPos == vPos) {
                        foundLineAtVPos = true
                    }
                    continue
                }
                count += curLine.text.length + LINE_BREAK.length
            }
        }
        return count
    }

    val y: Int
        get() = cursor.vPos

    val x: Int
        get() = cursor.hPos

    val lineIndents: List<Int>
        get() {
            val returnList: MutableList<Int> = ArrayList()
            lines.forEach(Consumer { l: RenderObject? ->
                if (l is Line) {
                    returnList.add(l.startingHPos)
                }
            })
            return returnList
        }

    val newPages: List<Pair<Int, Element>>
        get() {
            val newPages: MutableList<Pair<Int, Element>> = ArrayList()
            lines.filterIsInstance<NewPage>()
                .forEach { n: RenderObject -> newPages.add(((n as NewPage).offset) to (n.newPageElement)) }
            return newPages
        }

    fun finishPage() {
        val prevLine = previousLine
        if (prevLine != null) {
            var lineNum = prevLine.vPos
            while (lineNum < linesPerPage - 1) {
                createBlankLine(0, lineNum)
                lineNum++
            }
        }
    }

    fun applyEmphasis(emphasisNode: Element?) {
        this.emphasisNode = emphasisNode
    }

    /*
     * Table mode allows the state to move backwards without requiring a newPage element
     */
    fun setTableMode(tableMode: Boolean) {
        this.tableMode = tableMode
    }

    fun setLineIndentMode(lineIndentMode: Boolean) {
        this.lineIndentMode = lineIndentMode
    }

    private val previousLine: Line?
        get() {
            for (i in lines.indices.reversed()) {
                if (lines[i] is Line) return lines[i] as Line
            }
            return null
        }

    private fun getLineAtY(y: Int): Line? {
        for (i in lines.indices.reversed()) {
            if (lines[i] is Line && (lines[i] as Line).vPos == y) return lines[i] as Line
            if (lines[i] is NewPage) return null
        }
        return null
    }

    private fun createBlankLine(x: Int, y: Int): Line {
        val newLine = Line(x, y)
        lines.add(newLine)
        lineCount++
        return newLine
    }

    private fun hPosToCell(hPos: Double): Int {
        return cell.getCellsForWidth(hPos.toBigDecimal())
    }

    private fun vPosToCell(vPos: Double): Int {
        return cell.getLinesForHeight(vPos.toBigDecimal())
    }

    private open class RenderObject

    private inner class Line(var startingHPos: Int, val vPos: Int) : RenderObject() {
        var pendingHorizontalSpaces: Int = 0
            private set
        var text: String = ""
        val emphasisList: LinkedList<Emphasis> = LinkedList()
        var startOffset: Int = 0 //Used for tracking emphasis when added to widget

        fun setSpaces(spaces: Int) {
            pendingHorizontalSpaces = spaces
        }

        fun addSpacesToLine() {
            text += " ".repeat(pendingHorizontalSpaces)
            _charCount +=pendingHorizontalSpaces
            resetSpaces()
        }

        private fun resetSpaces() {
            pendingHorizontalSpaces = 0
        }
    }

    private inner class NewPage(val newPageElement: Element) : RenderObject() {
        val offset: Int = charCount
    }

    class Emphasis internal constructor(val inlineNode: Element?) {
        var start: Int = 0
        var end: Int = 0
    }

    private class Cursor {
        var vPos: Int = 0
        var hPos: Int

        init {
            hPos = NO_CURSOR_POS
        }

        fun setPos(hPos: Int, vPos: Int) {
            this.hPos = hPos
            this.vPos = vPos
        }

        fun reset() {
            setPos(NO_CURSOR_POS, 0)
        }
    }

    fun setLineNumber(lineNumber: Boolean) {
        this.lineNumber = lineNumber
    }

    companion object {
        private const val NO_CURSOR_POS = -1
    }
}
