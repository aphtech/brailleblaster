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

import org.apache.commons.lang3.math.Fraction
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.numberLine.NumberLineFormatter.Companion.addLabels
import org.brailleblaster.math.numberLine.NumberLineFormatter.Companion.addLine
import org.brailleblaster.math.numberLine.NumberLineFormatter.Companion.addPoints
import org.brailleblaster.math.numberLine.NumberLineFormatter.Companion.dealWithBlanks
import org.brailleblaster.math.numberLine.NumberLineFormatter.Companion.getSegmentBeginning
import org.brailleblaster.math.numberLine.NumberLineFormatter.Companion.getSegmentEnding
import org.brailleblaster.math.numberLine.NumberLineFormatter.Companion.verticallyAlignLabelsAndPoints
import org.brailleblaster.math.numberLine.NumberLineLine.LineChar
import org.brailleblaster.math.numberLine.NumberLineSegmentPoint.Companion.getPointFromIndex
import org.brailleblaster.math.numberLine.NumberLineSegmentPoint.Companion.hasPoint
import org.brailleblaster.math.spatial.*
import org.brailleblaster.math.spatial.SpatialMathEnum.NumberLineSection
import org.brailleblaster.math.spatial.SpatialMathEnum.Passage
import org.brailleblaster.settings.UTDManager
import org.brailleblaster.utd.utils.TextTranslator
import org.brailleblaster.wordprocessor.WPManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.floor

open class UebNumberLineFormatter {
  companion object {
    val log: Logger = LoggerFactory.getLogger(UebNumberLineFormatter::class.java)
		fun formatSegment(numberLine: NumberLine) {
      val startInterval = numberLine.segment.startInterval
      val endInterval = numberLine.segment.endInterval
      var countOfIntervals = 0
      var inSegment = false
      val line = Line()
      for (i in numberLine.line.indices) {
        val lineSegment = numberLine.line[i]
        val c = lineSegment.type
        if (c == LineChar.INTERVAL) {
          countOfIntervals++
          if (countOfIntervals == startInterval) {
            // add opening circle
            inSegment = true
            line.elements.add(line.getTextSegment(getSegmentBeginning(numberLine)))
          } else if (countOfIntervals == endInterval) {
            // add ending circle
            inSegment = false
            line.elements.add(line.getTextSegment(getSegmentEnding(numberLine)))
          } else {
            if (inSegment && !numberLine.segment.isPoint) {
              // add line segment char
              line.elements.add(line.getTextSegment(UebTranslations.LINE_ASCII))
            } else {
              // add whitespace
              line.elements.add(line.getWhitespaceSegment(1))
            }
          }
        } else {
          if (inSegment && !numberLine.segment.isPoint) {
            // add line segment char
            line.elements.add(
              line
                .getTextSegment(UebTranslations.LINE_ASCII.repeat(lineSegment.length))
            )
          } else {
            // add whitespace
            line.elements.add(line.getWhitespaceSegment(lineSegment.totalLength))
          }
        }
      }
      numberLine.lines.add(0, line)
    }

		fun formatLine(numberLine: NumberLine) {
      val array = numberLine.points
      val lineChars = ArrayList<NumberLineLine>()
      if (numberLine.settings.isArrow) {
        lineChars.add(NumberLineLine(LineChar.ARROW_START))
      }
      lineChars.add(NumberLineLine(LineChar.HORIZONTAL_MODE))
      for (i in array.indices) {
        val mathText = array[i]
        if (i != 0) {
          lineChars.add(
            NumberLineLine(
              LineChar.LINE_EMPTY,
              mathText.leftPadding + mathText.leftDec.length
            )
          )
        } else {
          val necessaryLeadingChars = if (mathText.isMinus) 2 else 1
          lineChars.add(NumberLineLine(LineChar.LINE_EMPTY, necessaryLeadingChars))
        }
        lineChars.add(NumberLineLine(LineChar.INTERVAL))
        if (i != array.size - 1) {
          lineChars.add(
            NumberLineLine(
              LineChar.LINE_EMPTY,
              mathText.rightPadding + mathText.rightDec.length + 1
            )
          )
        } else {
          lineChars.add(NumberLineLine(LineChar.LINE_EMPTY, numberLine.lastIntervalLength))
        }
      }
      if (numberLine.settings.isArrow) {
        lineChars.add(NumberLineLine(LineChar.ARROW_END))
      }
      numberLine.line = lineChars
    }

    @Throws(MathFormattingException::class)
    protected fun formatPoints(numberLine: NumberLine, cellsPerLine: Int) {
      val points = ArrayList<NumberLinePoint>()
      val interval = numberLine.numberLineText.interval.fraction
      val lineStart = numberLine.numberLineText.lineStart.fraction
      val lineEnd = numberLine.numberLineText.lineEnd.fraction
      val totalUnits = lineEnd.subtract(lineStart).divideBy(interval).toDouble().toInt() + 1
      for (i in 0 until totalUnits) {
        val fraction: Fraction = if (i != 0) {
          val additionalUnits = interval.multiplyBy(Fraction.getFraction(i.toDouble()))
          lineStart.add(additionalUnits)
        } else {
          lineStart
        }
        val string = getFractionString(numberLine, fraction)
        val braille = getBrailleFraction(numberLine, fraction)
        val mathText = MathText(print=string, braille=braille)
        points.add(
          NumberLinePoint(fraction=fraction, mathText=mathText,
            isMinus=fraction.toDouble() < 0)
        )
      }
      numberLine.points = points
      dealWithBlanks(numberLine)
      NumberLinePoint.makeColumnsEqualWidth(points)
      if (numberLine.settings.isStretch) {
        val addedCells = getStretchCells(numberLine, cellsPerLine)
        if (addedCells > 0) {
          for (i in numberLine.points.indices) {
            numberLine.points[i].rightPadding += addedCells
          }
        }
      }
    }

		@Throws(MathFormattingException::class)
    fun getStretchCells(numberLine: NumberLine, cellsPerLine: Int): Int {
      formatLine(numberLine)
      val necessaryCells = NumberLineLine.getLineLength(numberLine.line)
      val leftoverCells = cellsPerLine - necessaryCells
      val intervals = floor(
        numberLine.numberLineText.lineEnd.fraction
          .subtract(numberLine.numberLineText.lineStart.fraction)
          .toDouble()
      ).toInt()
      if (leftoverCells > intervals) {
        val addedCells = leftoverCells / intervals
        if (addedCells > 0) {
          return addedCells
        }
      }
      return 0
    }

    fun getFractionString(numberLine: NumberLine, fraction: Fraction): String {
      var fraction = fraction
      val string: String
        if (numberLine.settings.intervalType == SpatialMathEnum.IntervalType.WHOLE) {
        string = fraction.toInt().toString()
      } else if (numberLine.settings.intervalType == SpatialMathEnum.IntervalType.DECIMAL) {
        if (numberLine.settings.isRemoveLeadingZeros) {
          string = fraction.toDouble().toString()
          val array = string.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
          if (array.size < 2) {
            return string
          }
          if (array[0].toInt() == 0) {
            return "." + array[1]
          }
        } else {
          string = fraction.toDouble().toString()
        }
      } else if (numberLine.settings.intervalType == SpatialMathEnum.IntervalType.MIXED) {
        if (numberLine.settings.isReduceFraction) {
          string = if (fraction.properWhole == 0) {
            fraction.reduce().toString()
          } else {
            fraction.reduce().toProperString()
          }
        } else {
          fraction = NumberLineMathUtils.unReduce(numberLine, fraction)
          string = if (fraction.properWhole == 0) {
            fraction.toString()
          } else {
            fraction.toProperString()
          }
        }
      } else {
        if (numberLine.settings.isReduceFraction) {
          string = fraction.reduce().toString()
        } else {
          fraction = NumberLineMathUtils.unReduce(numberLine, fraction)
          string = fraction.toString()
        }
      }
      return string
    }

    private fun getBrailleFraction(numberLine: NumberLine, fraction: Fraction): String {
      // TODO account for passages
      var fraction = fraction
      var string: String
        return if (numberLine.settings.intervalType == SpatialMathEnum.IntervalType.WHOLE) {
        string = fraction.toInt().toString()
        val ascii = MathModuleUtils.translateAsciiMath(string)
        if (numberLine.settings.passage == Passage.NUMERIC) {
          ascii.replace(UebTranslations.NUMBER_CHAR.toRegex(), "")
        } else ascii
      } else if (numberLine.settings.intervalType == SpatialMathEnum.IntervalType.DECIMAL) {
        if (numberLine.settings.isRemoveLeadingZeros) {
          string = fraction.toDouble().toString()
          val array = string.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
          if (array.size > 1) {
            if (array[0].toInt() == 0) {
              string = "." + array[1]
            }
          }
        } else {
          string = fraction.toDouble().toString()
        }
        val ascii = MathModuleUtils.translateAsciiMath(string)
        if (numberLine.settings.passage == Passage.NUMERIC) {
          ascii.replace(UebTranslations.NUMBER_CHAR.toRegex(), "")
        } else ascii
      } else if (numberLine.settings.intervalType == SpatialMathEnum.IntervalType.MIXED) {
        fraction = if (numberLine.settings.isReduceFraction) {
          fraction.reduce()
        } else {
          NumberLineMathUtils.unReduce(numberLine, fraction)
        }
        makeMixedFractionString(numberLine, fraction)
      } else {
        fraction = if (numberLine.settings.isReduceFraction) {
          fraction.reduce()
        } else {
          NumberLineMathUtils.unReduce(numberLine, fraction)
        }
        makeImproperFractionString(numberLine, fraction)
      }
    }

    private fun makeImproperFractionString(numberLine: NumberLine, fraction: Fraction): String {
      val string: String
      val whole = TextTranslator.translateText(
        fraction.properWhole.toString(),
        WPManager.getInstance().controller.document.engine
      )
        .replace((UebTranslations.NUMBER_CHAR + "").toRegex(), "")
        .replace(UebTranslations.PRINT_MINUS.toRegex(), "")

      val numerator = TextTranslator.translateText(
        fraction.numerator.toString(),
        WPManager.getInstance().controller.document.engine
      )
        .replace((UebTranslations.NUMBER_CHAR + "").toRegex(), "")
        .replace(UebTranslations.PRINT_MINUS.toRegex(), "")

      val denominator = TextTranslator.translateText(
        fraction.denominator.toString(),
        WPManager.getInstance().controller.document.engine
      )
        .replace((UebTranslations.NUMBER_CHAR + "").toRegex(), "")
        .replace(UebTranslations.PRINT_MINUS.toRegex(), "")

      val fractionString: String = if (numberLine.settings.isBeveledFraction) {
        UebTranslations.BEVELED_FRAC
      } else {
        UebTranslations.FRAC
      }
      /*
			 * There is the possibility that we can reduce the fraction to a whole number.
			 * If we can, we should
			 */
      var canReduceToWhole = false
      val wholeFrac =
        Fraction.getFraction(fraction.properWhole.toDouble())
      val numDenFrac =
        Fraction.getFraction(fraction.numerator.toString() + UebTranslations.FRAC + fraction.denominator)

      if (wholeFrac.compareTo(numDenFrac) == 0) {
        canReduceToWhole = true
      }
      string = if (canReduceToWhole && numberLine.settings.isReduceFraction) {
        ((if (fraction.toDouble() < 0) UebTranslations.MINUS else "")
            + (if (numberLine.settings.passage == Passage.NUMERIC) "" else UebTranslations.NUMBER_CHAR)
            + whole)
      } else {
        ((if (fraction.toDouble() < 0) UebTranslations.MINUS else "")
            + (if (numberLine.settings.passage == Passage.NUMERIC) "" else UebTranslations.NUMBER_CHAR)
            + numerator + fractionString + denominator)
      }

      return string
    }

    @Throws(MathFormattingException::class)
    fun format(numberLine: NumberLine): Boolean {
      val cellsPerLine = UTDManager.getCellsPerLine(WPManager.getInstance().controller)
      formatPoints(numberLine, cellsPerLine)
      verticallyAlignLabelsAndPoints(numberLine)
      formatLine(numberLine)
      addLine(numberLine)
      if (!addPoints(numberLine)) {
        return false
      }
      if (numberLine.settings.sectionType == NumberLineSection.POINTS) {
        formatMultiplePoints(numberLine)
      } else if (numberLine.shouldFormatSegment()) {
        formatSegment(numberLine)
      }
      addLabels(numberLine)
      return true
    }

		fun formatMultiplePoints(numberLine: NumberLine) {
      var interval = 0
      val line = Line()
      for (i in numberLine.line.indices) {
        val lineSegment = numberLine.line[i]
        val c = lineSegment.type
        if (c == LineChar.INTERVAL) {
          interval++
          if (hasPoint(numberLine, interval)) {
            line.elements.add(line.getTextSegment(getSegmentBeginning(getPointFromIndex(numberLine, interval)!!)))
          } else {
            line.elements.add(line.getWhitespaceSegment(1))
          }
        } else {
          line.elements.add(line.getWhitespaceSegment(lineSegment.totalLength))
        }
      }
      numberLine.lines.add(0, line)
    }

    private fun makeMixedFractionString(numberLine: NumberLine, fraction: Fraction): String {
        val whole = TextTranslator.translateText(
        fraction.properWhole.toString(),
        WPManager.getInstance().controller.document.engine
      )
        .replace((UebTranslations.NUMBER_CHAR + "").toRegex(), "")
        .replace(UebTranslations.PRINT_MINUS.toRegex(), "")

      val numerator = TextTranslator.translateText(
        fraction.properNumerator.toString(),
        WPManager.getInstance().controller.document.engine
      )
        .replace((UebTranslations.NUMBER_CHAR + "").toRegex(), "")
        .replace(UebTranslations.PRINT_MINUS.toRegex(), "")
      val denominator = TextTranslator.translateText(
        fraction.denominator.toString(),
        WPManager.getInstance().controller.document.engine
      )
        .replace((UebTranslations.NUMBER_CHAR + "").toRegex(), "")
        .replace(UebTranslations.PRINT_MINUS.toRegex(), "")
      val fractionString: String = if (numberLine.settings.isBeveledFraction) {
        UebTranslations.BEVELED_FRAC
      } else {
        UebTranslations.FRAC
      }
      /*
			 * There is the possibility that we can reduce the fraction to a whole number.
			 * If we can, we should
			 */
      var canReduceToWhole = false
      val wholeFrac = Fraction.getFraction(fraction.properWhole.toDouble())
      val numDenFrac = Fraction
        .getFraction(fraction.numerator.toString() + UebTranslations.FRAC + fraction.denominator)
      if (wholeFrac.compareTo(numDenFrac) == 0) {
        canReduceToWhole = true
      }
      val string = if (canReduceToWhole && numberLine.settings.isReduceFraction) {
        ((if (fraction.toDouble() < 0) UebTranslations.MINUS else "")
            + (if (numberLine.settings.passage == Passage.NUMERIC) "" else UebTranslations.NUMBER_CHAR)
            + whole)
      } else if (wholeFrac.properWhole == 0) {
        ((if (fraction.toDouble() < 0) UebTranslations.MINUS else "")
            + (if (numberLine.settings.passage == Passage.NUMERIC) "" else UebTranslations.NUMBER_CHAR)
            + numerator + fractionString + denominator)
      } else {
        ((if (fraction.toDouble() < 0) UebTranslations.MINUS else "")
            + (if (numberLine.settings.passage == Passage.NUMERIC) "" else UebTranslations.NUMBER_CHAR)
            + whole + UebTranslations.NUMBER_CHAR + numerator + fractionString + denominator)
      }

      return string
    }
  }
}