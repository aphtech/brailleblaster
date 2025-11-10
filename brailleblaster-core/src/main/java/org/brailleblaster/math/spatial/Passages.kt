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

import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.mathml.NemethIndicators
import org.brailleblaster.math.mathml.NumericPassage

object Passages {
    fun addNemethMultipleRows(lines: ArrayList<Line>, end: Boolean) {
        val startingLines = lines.size
        for (i in 0 until startingLines) {
            val line = lines[i]
            if (i == 0 && !end) {
                line.elements.add(0, line.getTextSegment(NemethIndicators.INLINE_BEGINNING_INDICATOR))
            } else if (i == startingLines - 1 && end) {
                line.elements.add(
                    line.elements.size,
                    line.getTextSegment(NemethIndicators.INLINE_END_INDICATOR)
                )
            } else if (!end) {
                line.elements.add(0, line.getWhitespaceSegment(NemethIndicators.INLINE_END_INDICATOR.length))
            }
        }
    }

    fun addNemethOneRow(lines: ArrayList<Line>) {
        for (i in lines.indices) {
            val line = lines[i]
            when (i) {
                0 -> {
                    line.elements.add(0, line.getTextSegment(NemethIndicators.INLINE_BEGINNING_INDICATOR))
                }
                lines.size - 1 -> {
                    line.elements.add(0, line.getWhitespaceSegment(NemethIndicators.INLINE_END_INDICATOR.length))
                    line.elements.add(line.getTextSegment(NemethIndicators.INLINE_END_INDICATOR))
                }
                else -> {
                    line.elements.add(0, line.getWhitespaceSegment(NemethIndicators.INLINE_END_INDICATOR.length))
                }
            }
        }
    }

    fun addNumericOneRow(lines: ArrayList<Line>, indicator: Boolean) {
        if (!indicator) {
            for (l in lines) {
                l.elements.add(0, l.getWhitespaceSegment(NumericPassage.BLOCK_BEGINNING_INDICATOR.length))
            }
        }
        val startLine = Line()
        startLine.isPassageLine = true
        startLine.elements.add(startLine.getTextSegment(NumericPassage.BLOCK_BEGINNING_INDICATOR))
        lines.add(0, startLine)
        val endLine = Line()
        endLine.isPassageLine = true
        endLine.elements.add(endLine.getTextSegment(NumericPassage.BLOCK_END_INDICATOR))
        lines.add(endLine)
    }

    fun addNumericMultipleRows(lines: ArrayList<Line>, indicator: Boolean, end: Boolean) {
        if (!indicator) {
            for (l in lines) {
                l.elements.add(0, l.getWhitespaceSegment(NumericPassage.BLOCK_BEGINNING_INDICATOR.length))
            }
        }
        if (end) {
            val endLine = Line()
            endLine.isPassageLine = true
            endLine.elements.add(endLine.getTextSegment(NumericPassage.BLOCK_END_INDICATOR))
            lines.add(endLine)
        } else {
            val startLine = Line()
            startLine.isPassageLine = true
            startLine.elements.add(startLine.getTextSegment(NumericPassage.BLOCK_BEGINNING_INDICATOR))
            lines.add(0, startLine)
        }
    }

    fun addGrade1OneRow(lines: ArrayList<Line>) {
        val startLine = Line()
        startLine.isPassageLine = true
        startLine.elements.add(startLine.getTextSegment(MathModuleUtils.GRADE_1_PASSAGE_START))
        lines.add(0, startLine)
        val endLine = Line()
        endLine.isPassageLine = true
        endLine.elements.add(endLine.getTextSegment(MathModuleUtils.GRADE_1_PASSAGE_END))
        lines.add(endLine)
    }

    fun addGrade1MultipleRows(lines: ArrayList<Line>, end: Boolean) {
        if (end) {
            val endLine = Line()
            endLine.isPassageLine = true
            endLine.elements.add(endLine.getTextSegment(MathModuleUtils.GRADE_1_PASSAGE_END))
            lines.add(endLine)
        } else {
            val startLine = Line()
            startLine.isPassageLine = true
            startLine.elements.add(startLine.getTextSegment(MathModuleUtils.GRADE_1_PASSAGE_START))
            lines.add(0, startLine)
        }
    }
}
