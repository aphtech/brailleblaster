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
package org.brailleblaster.math.template

import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.math.spatial.NemethTranslations
import org.brailleblaster.math.spatial.SpatialMathEnum.Passage
import org.brailleblaster.math.spatial.UebTranslations

class TemplateNumber private constructor(
  val leftDec: String,
  val rightDec: String,
  private val isDecimal: Boolean,
  var isMinus: Boolean
) {
  var isColumnHasDecimal: Boolean = false
  var isColumnHasMinus: Boolean = false
  var leftPadding: Int = 0
  var rightPadding: Int = 0

  //Is this the source of unclear symbols (bug 52355?)
  fun toStringPart(): String {
    return if (MathModule.isNemeth) {
      (" ".repeat(leftPadding) + leftDec
          + (if (isDecimal) NemethTranslations.NEMETH_DECIMAL
      else if (isColumnHasDecimal) " " else "") + rightDec
          + " ".repeat(rightPadding))
    } else {
      (" ".repeat(leftPadding) + leftDec
          + (if (isDecimal) UebTranslations.UEB_DECIMAL
      else if (isColumnHasDecimal) " " else "") + rightDec
          + " ".repeat(rightPadding))
    }
  }

  fun toStringWhole(template: Template): String {
    return if (MathModule.isNemeth) {
      ((if (isMinus) NemethTranslations.MINUS
      else if (isColumnHasMinus) " ".repeat(NemethTranslations.MINUS.length)
      else "")
          + " ".repeat(leftPadding) + leftDec
          + (if (isDecimal) NemethTranslations.NEMETH_DECIMAL else if (isColumnHasDecimal) " " else "") + rightDec
          + " ".repeat(rightPadding))
    } else {
      if (template.settings.passage == Passage.NUMERIC) {
        ((if (isMinus) UebTranslations.MINUS
        else if (isColumnHasMinus)
          " ".repeat(UebTranslations.MINUS.length) else "")
            + " ".repeat(leftPadding) + leftDec
            + (if (isDecimal) UebTranslations.UEB_DECIMAL else if (isColumnHasDecimal) " " else "") + rightDec
            + " ".repeat(rightPadding))
      } else {
        ((if (isMinus) UebTranslations.MINUS
        else if (isColumnHasMinus) " ".repeat(
          UebTranslations.MINUS.length
        ) else "")
            + UebTranslations.NUMBER_CHAR
            + " ".repeat(leftPadding) + leftDec
            + (if (isDecimal) UebTranslations.UEB_DECIMAL else if (isColumnHasDecimal) " " else "") + rightDec
            + " ".repeat(rightPadding))
      }
    }
  }

  class TemplateNumberBuilder {
    private var leftDec = ""
    private var rightDec = ""
    private var decimal = false
    private var minus = false

    fun leftDec(leftDec: String): TemplateNumberBuilder {
      this.leftDec = leftDec
      return this
    }

    fun rightDec(rightDec: String): TemplateNumberBuilder {
      this.rightDec = rightDec
      return this
    }

    fun build(): TemplateNumber {
      return TemplateNumber(this.leftDec, this.rightDec, this.decimal, this.minus)
    }

    fun wholeNum(string: String): TemplateNumberBuilder {
      var wholeString = string
      wholeString = wholeString.replace(UebTranslations.NUMBER_CHAR.toRegex(), "")
      val decimal = if (MathModule.isNemeth) NemethTranslations.NEMETH_DECIMAL else UebTranslations.UEB_DECIMAL
      val split = wholeString.indexOf(decimal)
      if (split != -1) {
        this.leftDec = wholeString.take(split)
        this.rightDec = wholeString.drop(split + 1)
        this.decimal = true
        val minusSplit = if (MathModule.isNemeth) leftDec.indexOf(NemethTranslations.MINUS)
        else leftDec.indexOf(UebTranslations.PRINT_MINUS)
        if (minusSplit != -1) {
          this.minus = true
          if (!MathModule.isNemeth) {
            this.leftDec = leftDec.substring(minusSplit + 1)
          }
        }
      } else {
        this.leftDec = wholeString
        val minusSplit = if (MathModule.isNemeth) leftDec.indexOf(NemethTranslations.MINUS)
        else leftDec.indexOf(UebTranslations.PRINT_MINUS)
        if (minusSplit != -1) {
          this.minus = true
          if (!MathModule.isNemeth) {
            this.leftDec = leftDec.substring(minusSplit + 1)
          }
        }
      }
      return this
    }
  }

  companion object {
    fun makeColumnsEqualWidth(array: List<TemplateNumber>) {
      var widestLeft = 0
      var widestRight = 0
      var columnHasDecimal = false
      var columnHasMinus = false
      for (number in array) {
        if (number.isDecimal) {
          columnHasDecimal = true
        }
        if (number.isMinus) {
          columnHasMinus = true
        }
        if (number.leftDec.length > widestLeft) {
          widestLeft = number.leftDec.length
        }
        if (number.rightDec.length > widestRight) {
          widestRight = number.rightDec.length
        }
      }

      for (number in array) {
        number.isColumnHasDecimal = columnHasDecimal
        number.isColumnHasMinus = columnHasMinus
        number.leftPadding = widestLeft - number.leftDec.length
        number.rightPadding = widestRight - number.rightDec.length
      }
    }

    fun equalsWithNullCheck(tn1: TemplateNumber?, tn2: TemplateNumber?): Boolean {
      if (tn1 == null || tn2 == null) {
        return false
      }
      return tn1 == tn2
    }
  }
}
