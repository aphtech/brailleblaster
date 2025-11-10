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
import org.brailleblaster.bbx.BBX
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.numberLine.*
import org.brailleblaster.math.numberLine.NumberLine.Companion.getContainerFromElement
import org.brailleblaster.math.spatial.MatrixSettings.Companion.combineCellComponents
import org.brailleblaster.math.spatial.SpatialMathEnum.IntervalType
import org.brailleblaster.math.spatial.SpatialMathEnum.NumberLineType
import org.brailleblaster.math.spatial.SpatialMathEnum.Passage
import org.brailleblaster.math.template.Template
import org.brailleblaster.math.template.Template.Companion.getTemplateFromElement
import org.brailleblaster.math.template.Template.Companion.translateIdentifier
import org.brailleblaster.math.template.TemplateSettings

object VersionConverter {
    fun convertNumberLine(ele: Element): Element {
        val numberLine = NumberLine()
        val passage = BBX.CONTAINER.MATRIX.NUMERIC_PASSAGE[ele]
        numberLine.settings.passage = passage
        val arrow = ele.getAttributeValue(NumberLineConstants.ATTRIB_ARROW, ele.namespaceURI).toBoolean()
        numberLine.settings.isArrow = arrow
        val isStretch = ele.getAttributeValue(NumberLineConstants.ATTRIB_STRETCH, ele.namespaceURI).toBoolean()
        numberLine.settings.isStretch = isStretch
        val reduce = ele.getAttributeValue(NumberLineConstants.ATTRIB_REDUCE_FRACTION, ele.namespaceURI).toBoolean()
        numberLine.settings.isReduceFraction = reduce
        val numberLineType = BBX.CONTAINER.NUMBER_LINE.NUMBER_LINE_TYPE[ele]
        numberLine.settings.type = numberLineType
        if (numberLineType == NumberLineType.USER_DEFINED) {
            val translation = BBX.CONTAINER.NUMBER_LINE.USER_DEFINED_TRANSLATION[ele]
            numberLine.settings.translationUserDefined = translation
            val userDefinedArray = BBX.CONTAINER.NUMBER_LINE.USER_DEFINED_SEGMENTS[ele]
            numberLine.settings.printArray = userDefinedArray
        } else {
            val type = IntervalType
                .valueOf(ele.getAttributeValue(NumberLineConstants.ATTRIB_INTERVAL_TYPE, ele.namespaceURI))
            numberLine.settings.intervalType = type
            when (type) {
                IntervalType.DECIMAL -> {
                    val endLineWhole = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_LINE_END_WHOLE,
                        ele.namespaceURI
                    ))
                    val startLineWhole = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_LINE_START_WHOLE,
                        ele.namespaceURI
                    ))
                    val endSegmentWhole = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_SEGMENT_END_WHOLE,
                        ele.namespaceURI
                    ))
                    val startSegmentWhole = ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_SEGMENT_START_WHOLE,
                        ele.namespaceURI
                    )
                    val intervalWhole = ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_INTERVAL_WHOLE,
                        ele.namespaceURI
                    )

                    val endLineDecimal = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_LINE_END_DECIMAL,
                        ele.namespaceURI
                    ))
                    val startLineDecimal = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_LINE_START_DECIMAL,
                        ele.namespaceURI
                    ))
                    val endSegmentDecimal = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_SEGMENT_END_DECIMAL,
                        ele.namespaceURI
                    ))
                    val startSegmentDecimal = ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_SEGMENT_START_DECIMAL,
                        ele.namespaceURI
                    )
                    val intervalDecimal = ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_INTERVAL_DECIMAL,
                        ele.namespaceURI
                    )
                    val interval = NumberLineComponent(whole=intervalWhole,
                        decimal=intervalDecimal)
                    val startSegment = NumberLineComponent(
                        decimal=startSegmentDecimal, whole=startSegmentWhole)
                    val endSegment = NumberLineComponent(
                        whole=endSegmentWhole, decimal=endSegmentDecimal)
                    val startLine = NumberLineComponent(
                        whole=startLineWhole,decimal=startLineDecimal)
                    val endLine = NumberLineComponent(whole=endLineWhole,
                        decimal=endLineDecimal)
                    val text = NumberLineText(interval=interval,lineStart=startLine,
                        lineEnd=endLine,
                        segment=
                            NumberLineSegment(segmentStart=startSegment,
                                segmentEnd=endSegment, startSegmentCircle = numberLine.settings.startLineCircle, endSegmentCircle = numberLine.settings.endSegmentCircle)
                        )
                    numberLine.numberLineText = text
                }

                IntervalType.IMPROPER -> {
                    val endLineNumerator = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_LINE_END_NUMERATOR,
                        ele.namespaceURI
                    ))
                    val startLineNumerator = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_LINE_START_NUMERATOR,
                        ele.namespaceURI
                    ))
                    val endSegmentNumerator = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_SEGMENT_END_NUMERATOR,
                        ele.namespaceURI
                    ))
                    val startSegmentNumerator = ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_SEGMENT_START_NUMERATOR,
                        ele.namespaceURI
                    )
                    val intervalNumerator = ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_INTERVAL_NUMERATOR,
                        ele.namespaceURI
                    )

                    val endLineDenominator = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_LINE_END_DENOMINATOR,
                        ele.namespaceURI
                    ))
                    val startLineDenominator = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_LINE_START_DENOMINATOR,
                        ele.namespaceURI
                    ))
                    val endSegmentDenominator = (ele
                        .getAttributeValue(NumberLineConstants.ATTRIB_SEGMENT_END_DENOMINATOR, ele.namespaceURI))
                    val startSegmentDenominator = ele
                        .getAttributeValue(NumberLineConstants.ATTRIB_SEGMENT_START_DENOMINATOR, ele.namespaceURI)
                    val intervalDenominator = ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_INTERVAL_DENOMINATOR,
                        ele.namespaceURI
                    )

                    val interval = NumberLineComponent(
                        numerator=intervalNumerator, denominator=intervalDenominator)
                    val startSegment = NumberLineComponent(
                        denominator=startSegmentDenominator, numerator=startSegmentNumerator)
                    val endSegment = NumberLineComponent(
                        numerator=endSegmentNumerator, denominator=endSegmentDenominator)
                    val startLine = NumberLineComponent(
                        numerator=startLineNumerator, denominator=startLineDenominator)
                    val endLine = NumberLineComponent(
                        numerator=endLineNumerator, denominator=endLineDenominator)
                    val text = NumberLineText(interval=interval, lineStart=startLine,
                        lineEnd=endLine, segment=
                            NumberLineSegment(segmentStart=startSegment, segmentEnd=endSegment)
                        )
                    numberLine.numberLineText = text
                }

                IntervalType.MIXED -> {
                    val endLineWhole = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_LINE_END_WHOLE,
                        ele.namespaceURI
                    ))
                    val startLineWhole = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_LINE_START_WHOLE,
                        ele.namespaceURI
                    ))
                    val endSegmentWhole = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_SEGMENT_END_WHOLE,
                        ele.namespaceURI
                    ))
                    val startSegmentWhole = ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_SEGMENT_START_WHOLE,
                        ele.namespaceURI
                    )
                    val intervalWhole = ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_INTERVAL_WHOLE,
                        ele.namespaceURI
                    )

                    val endLineNumerator = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_LINE_END_NUMERATOR,
                        ele.namespaceURI
                    ))
                    val startLineNumerator = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_LINE_START_NUMERATOR,
                        ele.namespaceURI
                    ))
                    val endSegmentNumerator = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_SEGMENT_END_NUMERATOR,
                        ele.namespaceURI
                    ))
                    val startSegmentNumerator = ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_SEGMENT_START_NUMERATOR,
                        ele.namespaceURI
                    )
                    val intervalNumerator = ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_INTERVAL_NUMERATOR,
                        ele.namespaceURI
                    )

                    val endLineDenominator = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_LINE_END_DENOMINATOR,
                        ele.namespaceURI
                    ))
                    val startLineDenominator = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_LINE_START_DENOMINATOR,
                        ele.namespaceURI
                    ))
                    val endSegmentDenominator = (ele
                        .getAttributeValue(NumberLineConstants.ATTRIB_SEGMENT_END_DENOMINATOR, ele.namespaceURI))
                    val startSegmentDenominator = ele
                        .getAttributeValue(NumberLineConstants.ATTRIB_SEGMENT_START_DENOMINATOR, ele.namespaceURI)
                    val intervalDenominator = ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_INTERVAL_DENOMINATOR,
                        ele.namespaceURI
                    )

                    val interval = NumberLineComponent(whole=intervalWhole,
                        numerator=intervalNumerator, denominator=intervalDenominator)
                    val startSegment = NumberLineComponent(
                        denominator=startSegmentDenominator, numerator=startSegmentNumerator, whole=startSegmentWhole)
                    val endSegment = NumberLineComponent(
                        whole=endSegmentWhole, numerator=endSegmentNumerator, denominator=endSegmentDenominator)
                    val startLine = NumberLineComponent(
                        whole=startLineWhole, numerator=startLineNumerator, denominator=startLineDenominator)
                    val endLine = NumberLineComponent(whole=endLineWhole, numerator=endLineNumerator, denominator=endLineDenominator)
                    val text = NumberLineText(interval=interval, lineStart=startLine,
                        lineEnd=endLine, segment=
                            NumberLineSegment(segmentStart=startSegment, segmentEnd=endSegment)
                        )
                    numberLine.numberLineText = text
                }

                else -> {
                    val endLineWhole = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_LINE_END_WHOLE,
                        ele.namespaceURI
                    ))
                    val startLineWhole = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_LINE_START_WHOLE,
                        ele.namespaceURI
                    ))
                    val endSegmentWhole = (ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_SEGMENT_END_WHOLE,
                        ele.namespaceURI
                    ))
                    val startSegmentWhole = ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_SEGMENT_START_WHOLE,
                        ele.namespaceURI
                    )
                    val intervalWhole = ele.getAttributeValue(
                        NumberLineConstants.ATTRIB_INTERVAL_WHOLE,
                        ele.namespaceURI
                    )
                    val interval = NumberLineComponent(whole=intervalWhole)
                    val startSegment = NumberLineComponent(
                        whole=startSegmentWhole)
                    val endSegment = NumberLineComponent(
                        whole=endSegmentWhole)
                    val startLine = NumberLineComponent(
                        whole=startLineWhole)
                    val endLine = NumberLineComponent(whole=endLineWhole)
                    val text = NumberLineText(interval=interval, lineStart=startLine,
                        lineEnd=endLine, segment=
                            NumberLineSegment(segmentStart=startSegment, segmentEnd=endSegment)
                        )
                    numberLine.numberLineText = text
                }
            }
        }
        return BBX.CONTAINER.NUMBER_LINE.create(numberLine)
    }

    fun convertTemplate(templateElement: Element?): Element {
        val template = Template()
        template.settings.operator = BBX.CONTAINER.TEMPLATE.OPERATOR[templateElement]
        template.settings.type = BBX.CONTAINER.TEMPLATE.TYPE[templateElement]
        val printNumbers = BBX.CONTAINER.TEMPLATE.OPERANDS[templateElement]
        template.printOperands = printNumbers
        val solutionNumbers = BBX.CONTAINER.TEMPLATE.SOLUTIONS[templateElement]
        template.printSolutions = solutionNumbers
        var p = BBX.CONTAINER.TEMPLATE.PASSAGE_MODE[templateElement]
        if (MathModuleUtils.isNemeth && p == Passage.NUMERIC) {
            p = Passage.NONE
        }
        if (!MathModuleUtils.isNemeth && p == Passage.NEMETH) {
            p = Passage.NONE
        }
        template.settings.passage = p
        template.settings.operands = template.operands.size
        template.settings.solutions = template.solutions.size
        template.settings.isStraightRadicalSymbol = BBX.CONTAINER.TEMPLATE.STRAIGHT_RADICAL[templateElement]
        var identifier = (BBX.CONTAINER.TEMPLATE.IDENTIFER_AS_MATH[templateElement])
        if (identifier && !MathModuleUtils.isNemeth) {
            identifier = false
        }
        template.settings.isTranslateIdentifierAsMath = identifier
        template.identifier = MathText(
            print=BBX.CONTAINER.TEMPLATE.IDENTIFIER[templateElement],
            braille=translateIdentifier(BBX.CONTAINER.TEMPLATE.IDENTIFIER[templateElement], template))
        template.settings.isLinear = BBX.CONTAINER.TEMPLATE.LINEAR[templateElement]
        return BBX.CONTAINER.TEMPLATE.create(template)
    }

    @JvmStatic
	fun convertMatrix(current: Element?): Element {
        val matrix = Matrix()
        if (BBX.CONTAINER.MATRIX.isA(current)) {
            val array = BBX.CONTAINER.MATRIX.ASCII_MATH[current]
            val type = BBX.CONTAINER.MATRIX.BRACKET[current]
            val cols = BBX.CONTAINER.MATRIX.COLS[current]
            val rows = BBX.CONTAINER.MATRIX.ROWS[current]
            val wide = BBX.CONTAINER.MATRIX.WIDE_TYPE[current]
            val ellipses = BBX.CONTAINER.MATRIX.ELLIPSES_ARRAY[current]
            val passage = BBX.CONTAINER.MATRIX.NUMERIC_PASSAGE[current]
            val matrixCells = combineCellComponents(array, ellipses)
            val translation = BBX.CONTAINER.MATRIX.MATRIX_TRANSLATION[current]
            matrix.settings.translation = translation
            matrix.settings.passage = passage
            matrix.settings.bracketType = type
            matrix.settings.cols = cols
            matrix.settings.rows = rows
            matrix.settings.wideType = wide
            matrix.setPrintCells(array)
            matrix.settings.setModelFromArray(matrixCells)
        }
        return BBX.CONTAINER.MATRIX.create(matrix)
    }

    @JvmStatic
	fun convertConnectingContainer(node: Element?): Element {
        val container = ConnectingContainer()
        if (BBX.CONTAINER.CONNECTING_CONTAINER.isA(node)) {
            val isMath = BBX.CONTAINER.CONNECTING_CONTAINER.IS_MATH[node]
            val vertical = BBX.CONTAINER.CONNECTING_CONTAINER.VERTICAL[node]
            val horizontal = BBX.CONTAINER.CONNECTING_CONTAINER.HORIZONTAL[node]
            val text = BBX.CONTAINER.CONNECTING_CONTAINER.TEXT[node]
            container.settings.horizontal = horizontal
            container.settings.vertical = vertical
            container.settings.isTranslateAsMath = isMath
            container.text = MathText(print=text,
                braille=if (isMath) MathModuleUtils.translateMathPrint(text) else MathModuleUtils.translateMainPrint(text))
        }
        return BBX.CONTAINER.CONNECTING_CONTAINER.create(container)
    }

    @JvmStatic
	fun convertGrid(current: Element?): Element {
        val page = Grid()
        val rows = BBX.CONTAINER.SPATIAL_GRID.ROWS[current]
        val cols = BBX.CONTAINER.SPATIAL_GRID.COLS[current]
        page.settings.rows = rows
        page.settings.cols = cols
        val parent = BBX.CONTAINER.SPATIAL_GRID.GRID[current]
        if ((parent is Element)) {
            val containers = ArrayList<ISpatialMathContainer>()
            for (k in 0 until parent.getChildCount()) {
                val node = parent.getChild(k) as? Element ?: continue
                if (BBX.CONTAINER.NUMBER_LINE.isA(node)) {
                    val numberLine = getContainerFromElement(node)
                    containers.add(numberLine)
                } else if (BBX.CONTAINER.TEMPLATE.isA(node)) {
                    val template = getTemplateFromElement(node)
                    containers.add(template)
                } else if (BBX.CONTAINER.MATRIX.isA(node)) {
                    val matrix = Matrix.getContainerFromElement(node)
                    containers.add(matrix)
                } else {
                    val connectingContainer = ConnectingContainer
                        .getContainerFromElement(node)
                    containers.add(connectingContainer)
                }
            }
            val elements = ArrayList<ArrayList<ISpatialMathContainer>>()
            for (i in 0 until rows) {
                if (i < rows) {
                    val array = ArrayList<ISpatialMathContainer>()
                    elements.add(array)
                }
            }
            var passage = false
            val passageType = if (MathModuleUtils.isNemeth) {
                Passage.NEMETH
            } else {
                Passage.NUMERIC
            }
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    val index = i * cols + j
                    if (index < containers.size) {
                        if (containers[index].settings.passage == passageType) {
                            passage = true
                        }
                    }
                }
            }
            page.settings.passage = if (passage) passageType else Passage.NONE
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    val index = i * cols + j
                    if (index < containers.size) {
                        elements[i].add(containers[index])
                        if (containers[index] is Template) {
                            page.settings.isTranslateIdentifierAsMath = (containers[index].settings as TemplateSettings)
                                .isTranslateIdentifierAsMath
                        }
                    } else {
                        elements[i].add(ConnectingContainer())
                    }
                }
            }
            page.array.clear()
            page.array.addAll(elements)
        }
        return BBX.CONTAINER.SPATIAL_GRID.create(page)
    }
}
