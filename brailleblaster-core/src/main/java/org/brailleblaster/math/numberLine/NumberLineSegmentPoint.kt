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
import org.brailleblaster.math.spatial.MathFormattingException
import org.brailleblaster.math.spatial.SpatialMathEnum.Fill
import org.brailleblaster.math.spatial.SpatialMathEnum.IntervalType
import org.slf4j.LoggerFactory

class NumberLineSegmentPoint(
  var circle: Fill = Fill.FULL,
  var point: NumberLineComponent = NumberLineComponent(),
  var interval: Int = 1
) :
  Comparable<NumberLineSegmentPoint> {

  override fun compareTo(other: NumberLineSegmentPoint): Int {
    return if (interval < other.interval) {
      -1
    } else if (interval == other.interval) {
      0
    } else {
      1
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(NumberLineSegmentPoint::class.java)
    @JvmStatic
		fun hasPoint(numberLine: NumberLine, interval: Int): Boolean {
      for (i in numberLine.segmentPoints.indices) {
        if (numberLine.segmentPoints[i].interval == interval) {
          return true
        }
      }
      return false
    }

    @JvmStatic
		@Throws(MathFormattingException::class)
    fun getPotentialPoints(numberLine: NumberLine): ArrayList<NumberLineSegmentPoint> {
      val array = ArrayList<NumberLineSegmentPoint>()
      if (numberLine.mathFormattingChecks(true)) {
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
          val component: NumberLineComponent = when (numberLine.settings.intervalType) {
            IntervalType.DECIMAL -> {
              val doubleValue = fraction.toDouble().toString()
              val parts = doubleValue.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
              NumberLineComponent(whole=parts[0],
                decimal=parts[1])
            }

            IntervalType.IMPROPER -> NumberLineComponent(
              numerator=fraction.numerator.toString(),
              denominator=fraction.denominator.toString())

            IntervalType.MIXED -> NumberLineComponent(
              whole=fraction.properWhole.toString(),
              numerator=fraction.properNumerator.toString(),
              denominator=fraction.denominator.toString())

            IntervalType.WHOLE -> NumberLineComponent(
              whole=fraction.toInt().toString())

          }
          array.add(
            NumberLineSegmentPoint(point=component, interval=i + 1)
          )
        }
      } else {
        log.error("Get points called but math formatting checks failed")
      }
      return array
    }

    @JvmStatic
		@Throws(MathFormattingException::class)
    fun getPotentialPointsStringArray(numberLine: NumberLine): Array<String> {
        return getPotentialPoints(numberLine).map {
            val fraction = it.point.fraction
            NumberLineMathUtils.getFractionString(numberLine, fraction)
        }.toTypedArray()
    }

    @JvmStatic
		fun getPointFromIndex(numberLine: NumberLine, interval: Int): NumberLineSegmentPoint? {
      for (i in numberLine.segmentPoints.indices) {
        if (numberLine.segmentPoints[i].interval == interval) {
          return numberLine.segmentPoints[i]
        }
      }
      return null
    }

    @JvmStatic
		@Throws(MathFormattingException::class)
    fun getPrettyString(numberLine: NumberLine, component: NumberLineComponent): String {
      val fraction = component.fraction
      return NumberLineMathUtils.getFractionString(numberLine, fraction)
    }
  }
}