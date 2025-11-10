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
package org.brailleblaster.math.spatial

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.BBIni
import org.brailleblaster.bbx.BBX
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.numberLine.NumberLine
import org.brailleblaster.math.spatial.Line.LineBreakSegment
import org.brailleblaster.math.spatial.Passages.addGrade1MultipleRows
import org.brailleblaster.math.spatial.Passages.addGrade1OneRow
import org.brailleblaster.math.spatial.Passages.addNemethMultipleRows
import org.brailleblaster.math.spatial.Passages.addNemethOneRow
import org.brailleblaster.math.spatial.Passages.addNumericMultipleRows
import org.brailleblaster.math.spatial.Passages.addNumericOneRow
import org.brailleblaster.math.spatial.SpatialMathBlock.format
import org.brailleblaster.math.spatial.SpatialMathEnum.Passage
import org.brailleblaster.math.spatial.SpatialMathEnum.SpatialMathContainers
import org.brailleblaster.math.spatial.SpatialMathUtils.print
import org.brailleblaster.math.spatial.VersionConverter.convertGrid
import org.brailleblaster.math.template.Template
import org.brailleblaster.perspectives.braille.stylers.StyleHandler.Companion.addStyle
import org.brailleblaster.wordprocessor.WPManager
import kotlin.math.max

class Grid : ISpatialMathContainer {
    override val lines: MutableList<Line> = ArrayList()
    override var settings: GridSettings = GridSettings()
    var array: MutableList<MutableList<ISpatialMathContainer>> = ArrayList()
    override var blank: Boolean = false

    init {
        loadSettingsFromFile()
        buildArray()
    }

    fun isEmpty(container: ISpatialMathContainer): Boolean {
        for (i in container.lines.indices) {
            if (container.lines[i].toString().isNotBlank()) {
                return false
            }
        }
        return true
    }

    fun buildArray() {
        loadSettingsFromFile()
        for (i in 0 until settings.rows) {
            if (array.size < i + 1) {
                val row = ArrayList<ISpatialMathContainer>()
                array.add(row)
            }
        }
        for (i in 0 until settings.rows) {
            for (j in 0 until settings.cols) {
                if (array[i].size < j + 1) {
                    var sm: ISpatialMathContainer = ConnectingContainer()
                    when (settings.defaultType) {
                        SpatialMathContainers.CONNECTING -> sm = ConnectingContainer()
                        SpatialMathContainers.MATRIX -> sm = Matrix()
                        SpatialMathContainers.NUMBER_LINE -> sm = NumberLine()
                        SpatialMathContainers.TEMPLATE -> sm = Template()
                        else -> {}
                    }
                    sm.loadSettingsFromFile()
                    array[i].add(sm)
                }
            }
        }
    }

    fun hasMultiples(): Boolean {
        return array.size > 1 || (array.size == 1 && array[0].size > 1)
    }

    override fun format() {
        if (!MathModuleUtils.isNemeth){
            //If translation mode is not nemeth, set passage settings to None, as this is not a relevant category anymore.
            //This should get rid of the nemeth indicators when the translation switches; fix for bug #6482
            settings.passage = Passage.NONE
        }
        for (i in 0 until settings.rows) {
            for (j in 0 until settings.cols) {
                val container = array[i][j]
                var widestLine = 0
                for (k in container.lines.indices) {
                    val lineLength = container.lines[k].toString().length
                    if (lineLength > widestLine) {
                        widestLine = lineLength
                    }
                }
                container.widestLine = widestLine
            }
        }
        for (i in 0 until settings.rows) {
            val row = ArrayList<ISpatialMathContainer>()
            val newLines = ArrayList<Line>()
            for (j in 0 until settings.cols) {
                val container = array[i][j]
                row.add(container)
            }
            if (settings.cols < 2) {
                newLines.addAll(row[0].lines)
            } else {
                for (j in 0 until settings.cols - 1) {
                    val left = row[j]
                    val right = row[j + 1]
                    var whitespace = 1
                    if (right is Template) {
                        val hasIdentifier = right.identifier.braille.isNotEmpty()
                        // combine lines with whitespace according to identifier
                        // rules
                        if (hasIdentifier) {
                            whitespace = 3
                        }
                    }
                    if (isEmpty(right) || isEmpty(left)) {
                        whitespace = 0
                    }
                    val totalLines = max(right.lines.size.toDouble(), left.lines.size.toDouble()).toInt()
                    for (k in 0 until totalLines) {
                        if (k < newLines.size + 1) {
                            val l = Line()
                            newLines.add(l)
                        }
                        val newLine = newLines[k]
                        val leftLine = if (left.lines.size > k) left.lines[k] else null
                        val rightLine = if (right.lines.size > k) right.lines[k] else null
                        if (j == 0) {
                            if (leftLine == null) {
                                newLine.elements.add(newLine.getWhitespaceSegment(left.widestLine))
                            } else if ((leftLine.elements[0] is LineBreakSegment) || left.blank) {
                                newLine.elements.add(newLine.getWhitespaceSegment(left.widestLine))
                            } else {
                                newLine.elements.addAll(leftLine.elements)
                            }
                        }
                        newLine.elements.add(newLine.getWhitespaceSegment(whitespace))
                        if (rightLine == null) {
                            newLine.elements.add(newLine.getWhitespaceSegment(right.widestLine))
                        } else if (rightLine.elements[0] is LineBreakSegment || right.blank) {
                            newLine.elements.add(newLine.getWhitespaceSegment(right.widestLine))
                        } else {
                            newLine.elements.addAll(rightLine.elements)
                        }
                    }
                }
            }
            if (i != settings.rows - 1) {
                val emptyLine = Line()
                emptyLine.elements.add(emptyLine.lineBreakSegment)
                newLines.add(emptyLine)
            }
            newLines.removeIf { line: Line? -> line!!.elements.isEmpty() }
            val indicators = hasIndicator(row[0])
            if (settings.rows == 1) {
                when (settings.passage) {
                    Passage.NEMETH -> {
                        addNemethOneRow(newLines)
                    }
                    Passage.NUMERIC -> {
                        addNumericOneRow(newLines, indicators)
                    }
                    Passage.GRADE1 -> {
                        addGrade1OneRow(newLines)
                    }

                    Passage.NONE -> {}
                }
            } else {
                if (i == 0) {
                    when (settings.passage) {
                        Passage.NEMETH -> {
                            addNemethMultipleRows(newLines, false)
                        }
                        Passage.NUMERIC -> {
                            addNumericMultipleRows(newLines, indicators, false)
                        }
                        Passage.GRADE1 -> {
                            addGrade1MultipleRows(newLines, false)
                        }

                        Passage.NONE -> {}
                    }
                }
                if (i == settings.rows - 1) {
                    when (settings.passage) {
                        Passage.NEMETH -> {
                            addNemethMultipleRows(newLines, true)
                        }
                        Passage.NUMERIC -> {
                            addNumericMultipleRows(newLines, indicators, true)
                        }
                        Passage.GRADE1 -> {
                            addGrade1MultipleRows(newLines, true)
                        }

                        Passage.NONE -> {}
                    }
                }
            }
            lines.addAll(newLines)
        }
        print(this)
    }

    private fun hasIndicator(spatialMathContainer: ISpatialMathContainer): Boolean {
        if (spatialMathContainer is Template) {
            return spatialMathContainer.identifier.braille.isNotBlank()
        }
        return false
    }

    fun getLinesForRow(row: Int): Int {
        var lines = 0
        for (i in array[row].indices) {
            val container = array[row][i]
            if (container.lines.size > lines) {
                lines = container.lines.size
            }
        }
        return lines
    }

    fun getWidestColumn(row: Int): Int {
        var lines = 0
        for (i in array[row].indices) {
            val container = array[row][i]
            if (container.widestLine > lines) {
                lines = container.widestLine
            }
        }
        return lines
    }

    fun addRow() {
        settings.rows += 1
        buildArray()
    }

    fun addCol() {
        settings.cols += 1
        buildArray()
    }

    fun deleteRow(row: Int) {
        settings.rows -= 1
        if (settings.rowIndex >= settings.rows) {
            settings.rowIndex -= 1
        }
        array.removeAt(row)
        buildArray()
    }

    fun deleteCol(col: Int) {
        settings.cols -= 1
        if (settings.colIndex >= settings.cols) {
            settings.colIndex -= 1
        }
        for (i in array.indices) {
            array[i].removeAt(col)
        }
        buildArray()
    }

    override fun saveSettings() {
        BBIni.propertyFileManager.save(
            USER_SETTINGS_DEFAULT_CONTAINER,
            array[settings.rowIndex][settings.colIndex].typeEnum.name
        )
    }

    override fun loadSettingsFromFile() {
        val type = BBIni.propertyFileManager.getProperty(
            USER_SETTINGS_DEFAULT_CONTAINER,
            SpatialMathContainers.CONNECTING.name
        )
        val container = SpatialMathContainers.valueOf(type)
        settings.defaultType = container
    }

    override val typeEnum: SpatialMathContainers
        get() = SpatialMathContainers.GRID

    override var widestLine: Int
        get() =// TODO Auto-generated method stub
            0
        set(widestLine) {
            // TODO Auto-generated method stub
        }

    override val widget: ISpatialMathWidget?
        get() =// TODO Auto-generated method stub
            null

    override val json: ISpatialMathContainerJson
        get() = GridJson().containerToJson(this)

    override fun preFormatChecks(): Boolean {
        // TODO Auto-generated method stub
        return true
    }

    fun hasTemplate(): Boolean {
        for (i in array.indices) {
            for (j in array[i].indices) {
                if (array[i][j] is Template) {
                    return true
                }
            }
        }
        return false
    }

    companion object {
        const val KEY: String = "Grid"
        private const val USER_SETTINGS_DEFAULT_CONTAINER = "spatialPageDefault"
        fun gridContains(grid: Grid, container: SpatialMathContainers): Boolean {
            for (i in grid.array.indices) {
                for (j in grid.array[i].indices) {
                    if (grid.array[i][j].typeEnum == container) {
                        return true
                    }
                }
            }
            return false
        }

        fun getPageFromElement(currentElement: Element?): Grid {
            var current = currentElement
            return if (BBX.CONTAINER.SPATIAL_GRID.isA(current)) {
                if (!BBX.CONTAINER.SPATIAL_GRID.ATTRIB_VERSION.has(current)) {
                    current = convertGrid(current)
                }
                val json = BBX.CONTAINER.SPATIAL_GRID.JSON_GRID[current] as GridJson
                json.jsonToContainer() as Grid
            } else {
                Grid()
            }
        }

        @JvmStatic
        fun initialize(n: Node): Element {
            val ele = n as Element
            val page = getPageFromElement(ele)
            for (i in 0 until page.settings.rows) {
                for (j in 0 until page.settings.cols) {
                    val t = page.array[i][j]
                    t.format()
                    val e = when (t) {
                        is Template -> {
                            BBX.CONTAINER.TEMPLATE.create(t)
                        }

                        is Matrix -> {
                            BBX.CONTAINER.MATRIX.create(t)
                        }

                        is NumberLine -> {
                            BBX.CONTAINER.NUMBER_LINE.create(t)
                        }

                        else -> {
                            BBX.CONTAINER.CONNECTING_CONTAINER.create(t as ConnectingContainer)
                        }
                    }
                    try {
                        format(e, t.lines)
                    } catch (ignored: MathFormattingException) {
                    }
                }
            }
            page.format()
            val newElement = BBX.CONTAINER.SPATIAL_GRID.create(page)
            try {
                format(newElement, page.lines)
            } catch (ignored: MathFormattingException) {
            }
            ele.parent.replaceChild(ele, newElement)
            addStylesToGridElement(newElement)
            return newElement
        }

        fun addStylesToGridElement(e: Element) {
            addStyle(e, MathModuleUtils.STYLE_DEF_SPATIAL_GRID, WPManager.getInstance().controller)
            for (i in 0 until e.childCount) {
                val child = e.getChild(i)
                if (child is Element) {
                    if (child.getAttributeValue(MathModuleUtils.STYLE_DEF_OPTION_LINES_AFTER) != null) {
                        addStyle(
                            child, MathModuleUtils.STYLE_DEF_SPATIAL_BLOCK_BLANK_AFTER,
                            WPManager.getInstance().controller
                        )
                    }
                }
            }
        }

        fun extractCell(grid: Grid, container: SpatialMathContainers): ISpatialMathContainer? {
            for (i in grid.array.indices) {
                for (j in grid.array[i].indices) {
                    if (grid.array[i][j].typeEnum == container) {
                        return grid.array[i][j]
                    }
                }
            }
            return null
        }

        fun replaceCell(grid: Grid, container: SpatialMathContainers, newContainer: ISpatialMathContainer) {
            for (i in grid.array.indices) {
                for (j in grid.array[i].indices) {
                    if (grid.array[i][j].typeEnum == container) {
                        grid.array[i][j] = newContainer
                        return
                    }
                }
            }
        }
    }
}
