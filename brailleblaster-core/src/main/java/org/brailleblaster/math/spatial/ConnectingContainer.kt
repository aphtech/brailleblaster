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
import org.brailleblaster.math.spatial.SpatialMathBlock.format
import org.brailleblaster.math.spatial.SpatialMathEnum.HorizontalJustify
import org.brailleblaster.math.spatial.SpatialMathEnum.SpatialMathContainers
import org.brailleblaster.math.spatial.SpatialMathEnum.VerticalJustify
import org.brailleblaster.math.spatial.VersionConverter.convertConnectingContainer
import org.brailleblaster.util.LINE_BREAK
import java.util.*

class ConnectingContainer : ISpatialMathContainer {

    var text: MathText = MathText("", "", false)
    override val lines: MutableList<Line> = ArrayList()
    override val settings = ConnectingContainerSettings()
    override val widget = ConnectingContainerWidget()
    override var widestLine = 0

    override var blank = false

    fun fillPageInfo(spatialMathContainer: ISpatialMathContainer?, page: Grid, row: Int, col: Int) {
        lines.clear()
        val text = brailleText
        val textLines: Array<String> = text.split(LINE_BREAK).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val totalLines = page.getLinesForRow(row)
        val linesWithNewLine = addNewLines(textLines, this, page)
        var topLines = 0
        var bottomLines = 0
        when (settings.vertical) {
            VerticalJustify.TOP -> {
                bottomLines = totalLines - linesWithNewLine.size
            }

            VerticalJustify.CENTER -> {
                val linesLeft = totalLines - linesWithNewLine.size
                bottomLines = linesLeft / 2
                topLines = linesLeft / 2
                if (linesLeft % 2 != 0) {
                    bottomLines++
                }
            }

            VerticalJustify.BOTTOM -> {
                topLines = totalLines - linesWithNewLine.size
            }

        }
        for (i in 0 until topLines) {
            val topLineBreakLine = Line()
            topLineBreakLine.elements.add(topLineBreakLine.lineBreakSegment)
            lines.add(topLineBreakLine)
        }

        val width = page.getWidestColumn(row)
        for (s in linesWithNewLine) {
            val line = Line()
            var leftChars = 0
            var rightChars = 0
            when (settings.horizontal) {
                HorizontalJustify.LEFT -> {
                    rightChars = width - s.length
                }

                HorizontalJustify.CENTER -> {
                    val linesLeft = width - s.length
                    leftChars = linesLeft / 2
                    rightChars = linesLeft / 2
                    if (linesLeft % 2 != 0) {
                        rightChars++
                    }
                }

                HorizontalJustify.RIGHT -> {
                    leftChars = width - s.length
                }

                HorizontalJustify.TRIM -> {}
            }
            if (leftChars > 0) {
                line.elements.add(line.getWhitespaceSegment(leftChars))
            }
            line.elements.add(line.getTextSegment(s))
            if (rightChars > 0) {
                line.elements.add(line.getWhitespaceSegment(rightChars))
            }
            lines.add(line)
        }
        for (i in 0 until bottomLines) {
            val bottomLineBreakLine = Line()
            bottomLineBreakLine.elements.add(bottomLineBreakLine.lineBreakSegment)
            lines.add(bottomLineBreakLine)
        }
    }

    override fun format() {
        val text = brailleText
        val bottomLineBreakLine = Line()
        bottomLineBreakLine.elements.add(bottomLineBreakLine.getTextSegment(text))
        lines.add(bottomLineBreakLine)
    }

    val printText: String
        get() = text.print

    private val brailleText: String
        get() = text.braille

    override fun saveSettings() {
        BBIni.propertyFileManager.save(USER_SETTINGS_VERTICAL, settings.vertical.name)
        BBIni.propertyFileManager.save(USER_SETTINGS_HORIZONTAL, settings.horizontal.name)
        BBIni.propertyFileManager.saveAsBoolean(
            USER_SETTINGS_TRANSLATE_AS_MATH,
            settings.isTranslateAsMath
        )
    }

    /*
	 * Vertical, horizontal, translate as math
	 */
    override fun loadSettingsFromFile() {
        val verticalString = BBIni.propertyFileManager.getProperty(
            USER_SETTINGS_VERTICAL,
            ConnectingContainerSettings.DEFAULT_VERTICAL.name
        )
        val vertical = VerticalJustify.valueOf(verticalString)
        settings.vertical = vertical
        val horizontalString = BBIni.propertyFileManager.getProperty(
            USER_SETTINGS_HORIZONTAL,
            ConnectingContainerSettings.DEFAULT_HORIZONTAL.name
        )
        val horizontal = HorizontalJustify.valueOf(horizontalString)
        settings.horizontal = horizontal
        val mathString = BBIni.propertyFileManager.getProperty(USER_SETTINGS_TRANSLATE_AS_MATH, "false")
        settings.isTranslateAsMath = mathString.toBoolean()
    }

    override val typeEnum: SpatialMathContainers = SpatialMathContainers.CONNECTING

    override val json: ISpatialMathContainerJson
        get() = ConnectingContainerJson().containerToJson(this)

    override fun preFormatChecks(): Boolean {
        // TODO Auto-generated method stub
        return true
    }

    companion object {
        const val KEY: String = "connectingContainer"

        private const val USER_SETTINGS_VERTICAL = "cc.userSettingsVertical"

        private const val USER_SETTINGS_HORIZONTAL = "cc.userSettingsHorizontal"

        private const val USER_SETTINGS_TRANSLATE_AS_MATH = "cc.userSettingsMath"

        fun addNewLines(textLines: Array<String>, container: ConnectingContainer?, page: Grid?): List<String> {
            return listOf(*textLines)
        }

        fun getContainerFromElement(origNode: Element?): ConnectingContainer {
            var node = origNode
            var container = ConnectingContainer()
            if (BBX.CONTAINER.CONNECTING_CONTAINER.isA(node)) {
                if (!BBX.CONTAINER.CONNECTING_CONTAINER.ATTRIB_VERSION.has(node)) {
                    node = convertConnectingContainer(node)
                }
                val json = BBX.CONTAINER.CONNECTING_CONTAINER.JSON_CONNECTING_CONTAINER[node] as ConnectingContainerJson
                container = json.jsonToContainer() as ConnectingContainer
            }
            return container
        }

        @JvmStatic
		fun initialize(n: Node): Element {
            val e = n as Element
            val t = getContainerFromElement(e)
            t.format()
            val newElement = BBX.CONTAINER.CONNECTING_CONTAINER.create(t)
            try {
                format(newElement, t.lines)
            } catch (ignored: MathFormattingException) {
            }
            e.parent.replaceChild(e, newElement)
            return newElement
        }
    }
}
