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
package org.brailleblaster.math.numberLine

import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.numberLine.NumberLineLine.LineChar
import org.brailleblaster.math.spatial.*
import org.brailleblaster.math.spatial.SpatialMathEnum.BlankOptions
import org.brailleblaster.math.spatial.SpatialMathEnum.Fill
import org.brailleblaster.math.spatial.SpatialMathEnum.LabelPosition
import org.brailleblaster.math.spatial.SpatialMathEnum.NumberLineSection
import org.brailleblaster.math.spatial.SpatialMathEnum.NumberLineType
import org.brailleblaster.settings.UTDManager
import org.brailleblaster.wordprocessor.WPManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NumberLineFormatter(private val numberLine: NumberLine) {
  @Throws(MathFormattingException::class)
  fun format(): Boolean {
    clearFormatting(numberLine)
    if (numberLine.settings.sectionType == NumberLineSection.SEGMENT &&
      numberLine.settings.type == NumberLineType.AUTOMATIC_MATH
    ) {
      numberLine.setSegmentIntervals()
    }
    return if (MathModuleUtils.isNemeth) {
      if (numberLine.settings.type == NumberLineType.USER_DEFINED) {
        NemethUserDefinedFormatter.format(numberLine)
      } else {
        NemethNumberLineFormatter.format(numberLine)
      }
    } else {
      if (numberLine.settings.type == NumberLineType.USER_DEFINED) {
        UebUserDefinedFormatter.format(numberLine)
      } else {
        UebNumberLineFormatter.format(numberLine)
      }
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(NumberLineFormatter::class.java)

    fun dealWithBlanks(numberLine: NumberLine) {
      val points = numberLine.points
      for (j in numberLine.settings.userDefinedArray.indices) {
        val intervalPoint = numberLine.settings.userDefinedArray[j]
        when (intervalPoint.blankType) {
          BlankOptions.BLANK -> if (points.size > j) {
            val newPoint = NumberLinePoint(
              mathText=MathText(braille=" ", print=" "))
            points[j] = newPoint
          }

          BlankOptions.NONE -> {}
          BlankOptions.OMISSION -> if (points.size > j) {
            val omission = if (MathModuleUtils.isNemeth) NemethTranslations.OMISSION else UebTranslations.OMISSION
            val newPoint = NumberLinePoint(
              mathText=MathText(braille=omission, print=omission))
            points[j] = newPoint
          }

        }
      }
    }

    fun clearFormatting(numberLine: NumberLine) {
      numberLine.lines.clear()
      numberLine.points.clear()
      numberLine.line.clear()
    }

    fun getSegmentBeginning(numberLine: NumberLine): String {
      return when (numberLine.segment.startSegmentCircle) {
        Fill.EMPTY -> if (MathModuleUtils.isNemeth) NemethTranslations.EMPTY_CIRCLE_ASCII else UebTranslations.EMPTY_ASCII
        Fill.FULL -> if (MathModuleUtils.isNemeth) NemethTranslations.FILL_CIRCLE_ASCII else UebTranslations.FILL_ASCII
        else -> if (MathModuleUtils.isNemeth) NemethTranslations.LINE_ASCII else UebTranslations.HORIZONTAL_MODE
      }
    }

    fun getSegmentEnding(numberLine: NumberLine): String {
      return when (numberLine.segment.endSegmentCircle) {
        Fill.EMPTY -> if (MathModuleUtils.isNemeth) NemethTranslations.EMPTY_CIRCLE_ASCII else UebTranslations.EMPTY_ASCII
        Fill.FULL -> if (MathModuleUtils.isNemeth) NemethTranslations.FILL_CIRCLE_ASCII else UebTranslations.FILL_ASCII
        else -> if (MathModuleUtils.isNemeth) NemethTranslations.LINE_ASCII else UebTranslations.LINE_ASCII
      }
    }

    fun getSegmentBeginning(point: NumberLineSegmentPoint): String {
      return when (point.circle) {
        Fill.EMPTY -> if (MathModuleUtils.isNemeth) NemethTranslations.EMPTY_CIRCLE_ASCII else UebTranslations.EMPTY_ASCII
        Fill.FULL -> if (MathModuleUtils.isNemeth) NemethTranslations.FILL_CIRCLE_ASCII else UebTranslations.FILL_ASCII
        else -> if (MathModuleUtils.isNemeth) NemethTranslations.LINE_ASCII else UebTranslations.HORIZONTAL_MODE
      }
    }

    fun getSegmentEnding(point: NumberLineSegmentPoint): String {
      return when (point.circle) {
        Fill.EMPTY -> if (MathModuleUtils.isNemeth) NemethTranslations.EMPTY_CIRCLE_ASCII else UebTranslations.EMPTY_ASCII
        Fill.FULL -> if (MathModuleUtils.isNemeth) NemethTranslations.FILL_CIRCLE_ASCII else UebTranslations.FILL_ASCII
        else -> if (MathModuleUtils.isNemeth) NemethTranslations.LINE_ASCII else UebTranslations.LINE_ASCII
      }
    }

    fun addLine(numberLine: NumberLine) {
      val line = Line()
      for (i in numberLine.line.indices) {
        val l = numberLine.line[i]
        line.elements
          .add(line.getTextSegment(LineChar.getString(l.type).repeat(l.length)))
      }
      numberLine.lines.add(line)
    }

    fun verticallyAlignLabelsAndPoints(numberLine: NumberLine) {
      var widestLeft = 0
      var widestRight = 0
      val numIntervals = numberLine.settings.userDefinedArray.size
      val numPoints = numberLine.points.size
      if (numIntervals != numPoints) {
        log.debug("Number of intervals and points do not match")
        return
      }
      val numberChar = if (MathModuleUtils.isNemeth) 0 else 1
      for (i in 0 until numIntervals) {
        val label = numberLine.settings.userDefinedArray[i].labelText
        val point = numberLine.points[i]
        val leftLabel = label.leftDec.length
        val rightLabel = label.rightDec.length
        val leftPoint = point.leftDec.length
        val rightPoint = point.rightDec.length + numberChar
        if (leftLabel > widestLeft) {
          widestLeft = leftLabel
        }
        if (rightLabel > widestRight) {
          widestRight = rightLabel
        }
        if (leftPoint > widestLeft) {
          widestLeft = leftPoint
        }
        if (rightPoint > widestRight) {
          widestRight = rightPoint
        }
      }
      for (i in 0 until numPoints) {
        val point = numberLine.points[i]
        val label = numberLine.settings.userDefinedArray[i].labelText
        val leftLabel = label.leftDec.length
        val rightLabel = label.rightDec.length
        val leftPoint = point.leftDec.length
        val rightPoint = point.rightDec.length + numberChar
        val leftPaddingPoint = widestLeft - leftPoint
        val rightPaddingPoint = widestRight - rightPoint
        point.leftPadding = leftPaddingPoint
        point.rightPadding = rightPaddingPoint
        val leftPaddingLabel = widestLeft - leftLabel
        val rightPaddingLabel = widestRight - rightLabel
        label.leftPadding = leftPaddingLabel
        label.rightPadding = rightPaddingLabel
      }
    }

    fun addLabels(numberLine: NumberLine) {
      val numIntervals = numberLine.settings.userDefinedArray.size
      val numPoints = numberLine.points.size
      if (numIntervals != numPoints) {
        log.debug("Number of intervals and points do not match")
        return
      }
      val line = Line()
      if (numberLine.settings.isStartOverflow) {
        line.elements.add(
          line.getWhitespaceSegment(if (MathModuleUtils.isNemeth) NemethTranslations.DOUBLE_BEGIN_ARROW.length else UebTranslations.BEGIN_ARROW.length)
        )
      } else if (numberLine.settings.isArrow) {
        line.elements
          .add(line.getWhitespaceSegment(if (MathModuleUtils.isNemeth) NemethTranslations.BEGIN_ARROW.length else UebTranslations.BEGIN_ARROW.length))
      }
      if (!MathModuleUtils.isNemeth) {
        line.elements.add(line.getWhitespaceSegment(UebTranslations.HORIZONTAL_MODE.length))
      }
      val points = numberLine.settings.userDefinedArray
      for (i in points.indices) {
        val mathText = points[i].labelText
        if (i != 0) {
          line.elements.add(line.getWhitespaceSegment(mathText.leftPadding))
        } else {
          if (!mathText.isMinus) {
            line.elements.add(line.getWhitespaceSegment(UebTranslations.LINE_ASCII.length))
          }
        }
        line.elements.add(line.getTextSegment(mathText.mathText.braille))
        if (i != points.size - 1) {
          line.elements.add(line.getWhitespaceSegment(mathText.rightPadding + 1))
        }
      }
      if (numberLine.settings.isEndOverflow) {
        line.elements
          .add(line.getWhitespaceSegment(if (MathModuleUtils.isNemeth) NemethTranslations.DOUBLE_END_ARROW.length else UebTranslations.END_ARROW.length))
      } else if (numberLine.settings.isArrow) {
        line.elements
          .add(line.getWhitespaceSegment(if (MathModuleUtils.isNemeth) NemethTranslations.END_ARROW.length else UebTranslations.END_ARROW.length))
      }
      if (numberLine.settings.labelPosition == LabelPosition.TOP) {
        numberLine.lines.add(0, line)
      } else {
        numberLine.lines.add(line)
      }
    }

    fun addPoints(numberLine: NumberLine): Boolean {
      val line = Line()
      val cellsPerLine = UTDManager.getCellsPerLine(WPManager.getInstance().controller)
      val points = numberLine.points
      if (numberLine.settings.isStartOverflow) {
        line.elements.add(
          line.getWhitespaceSegment(if (MathModuleUtils.isNemeth) NemethTranslations.DOUBLE_BEGIN_ARROW.length else UebTranslations.BEGIN_ARROW.length)
        )
      } else if (numberLine.settings.isArrow) {
        line.elements
          .add(line.getWhitespaceSegment(if (MathModuleUtils.isNemeth) NemethTranslations.BEGIN_ARROW.length else UebTranslations.BEGIN_ARROW.length))
      }
      if (!MathModuleUtils.isNemeth) {
        line.elements.add(line.getWhitespaceSegment(UebTranslations.HORIZONTAL_MODE.length))
      }
      for (i in points.indices) {
        val mathText = points[i]
        if (i != 0) {
          line.elements.add(line.getWhitespaceSegment(mathText.leftPadding))
        } else {
          if (!mathText.isMinus) {
            line.elements.add(line.getWhitespaceSegment(UebTranslations.LINE_ASCII.length))
          }
        }
        line.elements.add(line.getTextSegment(mathText.mathText.braille))
        if (i != points.size - 1) {
          line.elements.add(line.getWhitespaceSegment(mathText.rightPadding + 1))
        }
      }
      if (numberLine.settings.isEndOverflow) {
        line.elements
          .add(line.getWhitespaceSegment(if (MathModuleUtils.isNemeth) NemethTranslations.DOUBLE_END_ARROW.length else UebTranslations.END_ARROW.length))
      } else if (numberLine.settings.isArrow) {
        line.elements
          .add(line.getWhitespaceSegment(if (MathModuleUtils.isNemeth) NemethTranslations.END_ARROW.length else UebTranslations.END_ARROW.length))
      }
      if (line.toString().length > cellsPerLine) {
        log.error("Points are longer than cells per line, points are {}", line.toString().length)
        return false
      }
      numberLine.lines.add(line)
      return true
    }
  }
}