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

import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.template.TemplateConstants

private val localeHandler = getDefault()

object GridConstants {
  @JvmField
  val PASSAGE_TYPE = localeHandler["passageType"]
}

object MatrixConstants {
  val ENABLED = localeHandler["enabled"]

  @JvmField
  val ELLIPSIS_LABEL = localeHandler["ellipsisLabel"]
  const val WIDE_LABEL_KEY = "wideLabel"
  const val BIG_KEY = "big"
  const val BLOCK_BLANK_KEY = "blockBlank"
  const val INDENT_COLUMN_KEY = "indentColumn"
  const val COL_KEY = "col"
  const val ROW_KEY = "row"
  const val SQUARE_KEY = "square"
  const val ROUND_KEY = "round"
  const val CURLY_KEY = "curly"
  const val STRAIGHT_KEY = "straight"
  const val BRACKET_TYPE_KEY = "bracketType"
  const val MATRIX_DIALOG_KEY = "matrixDialogTitle"
  const val IS_WIDE_KEY = "isWide"

  val MATRIX_DIALOG_LABEL = localeHandler[MATRIX_DIALOG_KEY]
  val BRACKET_TYPE_LABEL = localeHandler[BRACKET_TYPE_KEY]
  val CURLY_LABEL = localeHandler[CURLY_KEY]
  val ROUND_LABEL = localeHandler[ROUND_KEY]
  val SQUARE_LABEL = localeHandler[SQUARE_KEY]
  val ROW_LABEL = localeHandler[ROW_KEY]
  val COL_LABEL = localeHandler[COL_KEY]
  val INDENT_COLUMN_LABEL = localeHandler[INDENT_COLUMN_KEY]
  val BLOCK_BLANK_LABEL = localeHandler[BLOCK_BLANK_KEY]
  val STRAIGHT_LABEL = localeHandler[STRAIGHT_KEY]

  @JvmField
  val WIDE_LABEL = localeHandler[WIDE_LABEL_KEY]
  const val BIG_SQUARE_KEY = BIG_KEY + SQUARE_KEY
  val BIG_SQUARE_LABEL = localeHandler[BIG_SQUARE_KEY]
  const val BIG_ROUND_KEY = BIG_KEY + ROUND_KEY
  val BIG_ROUND_LABEL = localeHandler[BIG_ROUND_KEY]
  const val BIG_CURLY_KEY = BIG_KEY + CURLY_KEY
  val BIG_CURLY_LABEL = localeHandler[BIG_CURLY_KEY]
  const val BIG_STRAIGHT_KEY = BIG_KEY + STRAIGHT_KEY
  val BIG_STRAIGHT_LABEL = localeHandler[BIG_STRAIGHT_KEY]
  val USE_EDITOR_WARNING = localeHandler["readOnlyWarningMatrix"]

  @JvmField
  val FORMAT_INDENT_TOO_WIDE_WARNING = localeHandler["formatIndentTooWideWarning"]
  val DELETE_MATRIX_LABEL = localeHandler["deleteMatrix"]

  @JvmField
  val MENU_SETTINGS = localeHandler["menuSettings"]
  val MATRIX_TRANSLATION = localeHandler["matrixTranslation"]

  enum class Wide(val label: String) {
    BLOCK_BLANK(BLOCK_BLANK_LABEL), INDENT_COLUMN(INDENT_COLUMN_LABEL);

  }

  enum class BracketType(
    val label: String,
    val nemethLeft: String,
    val nemethRight: String,
    val uebLeft: String,
    val uebRight: String
  ) {
    SQUARE(
      SQUARE_LABEL, NemethTranslations.NEMETH_LEFT_SQUARE,
      NemethTranslations.NEMETH_RIGHT_SQUARE,
      UebTranslations.UEB_LEFT_SQUARE,
      UebTranslations.UEB_RIGHT_SQUARE
    ),
    ROUND(
      ROUND_LABEL,
      NemethTranslations.NEMETH_LEFT_ROUND,
      NemethTranslations.NEMETH_RIGHT_ROUND,
      UebTranslations.UEB_LEFT_ROUND,
      UebTranslations.UEB_RIGHT_ROUND
    ),
    CURLY(
      CURLY_LABEL,
      NemethTranslations.NEMETH_LEFT_CURLY,
      NemethTranslations.NEMETH_RIGHT_CURLY,
      UebTranslations.UEB_LEFT_CURLY,
      UebTranslations.UEB_RIGHT_CURLY
    ),
    STRAIGHT(
      STRAIGHT_LABEL,
      NemethTranslations.NEMETH_LEFT_STRAIGHT,
      NemethTranslations.NEMETH_RIGHT_STRAIGHT,
      UebTranslations.UEB_LEFT_STRAIGHT,
      UebTranslations.UEB_RIGHT_STRAIGHT
    ),
    BIG_SQUARE(
      BIG_SQUARE_LABEL,
      NemethTranslations.BIG_NEMETH_LEFT_SQUARE,
      NemethTranslations.BIG_NEMETH_RIGHT_SQUARE,
      UebTranslations.BIG_UEB_LEFT_SQUARE,
      UebTranslations.BIG_UEB_RIGHT_SQUARE
    ),
    BIG_ROUND(
      BIG_ROUND_LABEL,
      NemethTranslations.BIG_NEMETH_LEFT_ROUND,
      NemethTranslations.BIG_NEMETH_RIGHT_ROUND,
      UebTranslations.BIG_UEB_LEFT_ROUND,
      UebTranslations.BIG_UEB_RIGHT_ROUND
    ),
    BIG_CURLY(
      BIG_CURLY_LABEL,
      NemethTranslations.BIG_NEMETH_LEFT_CURLY,
      NemethTranslations.BIG_NEMETH_RIGHT_CURLY,
      UebTranslations.BIG_UEB_LEFT_CURLY,
      UebTranslations.BIG_UEB_RIGHT_CURLY
    ),
    BIG_STRAIGHT(
      BIG_STRAIGHT_LABEL,
      NemethTranslations.BIG_NEMETH_LEFT_STRAIGHT,
      NemethTranslations.BIG_NEMETH_RIGHT_STRAIGHT,
      UebTranslations.BIG_UEB_LEFT_STRAIGHT,
      UebTranslations.BIG_UEB_RIGHT_STRAIGHT
    );

    companion object {
      fun getLeft(b: BracketType): String {
        return if (MathModuleUtils.isNemeth) {
          b.nemethLeft
        } else {
          b.uebLeft
        }
      }

      fun getRight(b: BracketType): String {
        return if (MathModuleUtils.isNemeth) {
          b.nemethRight
        } else {
          b.uebRight
        }
      }
    }
  }
}

object SpatialMathEnum {
  enum class LabelPosition(s: String) {
    TOP("top"), BOTTOM("bottom");

    val prettyName: String = localeHandler[s]
  }

  enum class SpatialMathContainers(s: String, val id: Int) {
    NUMBER_LINE(MathModuleUtils.NUMBER_LINE_KEY, 0),
    MATRIX(MathModuleUtils.MATRIX_KEY, 1),
    TEMPLATE(MathModuleUtils.TEMPLATES_KEY, 2),
    CONNECTING(ConnectingContainer.KEY, 3),
    GRID(Grid.KEY, 4);

    @JvmField
    val prettyName: String = localeHandler[s]

    companion object {
      val array: Array<String> = entries.map { it.prettyName }.toTypedArray()
    }
  }

  enum class BlankOptions(s: String) {
    BLANK("blank"),
    OMISSION("omission"),
    NONE("nle.noChange");

    val prettyName: String = localeHandler[s]
  }

  enum class Passage(s: String) {
    NUMERIC("numericPassage"),
    NEMETH("nemethPassage"),
    NONE("none"),
    GRADE1("grade1");

    val prettyName: String = localeHandler[s]
  }

  enum class NumberLineSection(s: String) {
    POINTS("points"),
    SEGMENT("segment"),
    NONE("none");

    val prettyName: String = localeHandler[s]
  }

  enum class NumberLineType(s: String) {
    AUTOMATIC_MATH("automaticMath"),
    USER_DEFINED("userDefined");

    val prettyName: String = localeHandler[s]
  }

  enum class NumberLineViews(s: String) {
    AUTOMATIC_MATH("automaticMath"),
    USER_DEFINED("userDefined"),
    BLANKS("blankOmissionOverride"),
    LABELS("labels");

    val prettyName: String = localeHandler[s]
  }

  enum class NumberLineOptions(s: String) {
    BLANKS("blankOmissionOverride"),
    LABELS("labels");

    val prettyName: String = localeHandler[s]
  }

  enum class Translation(s: String) {
    UNCONTRACTED("uncontractedTranslation"),
    DIRECT("directTranslation"),
    ASCII_MATH("asciiMathTranslation"),
    LITERARY("literaryTranslation");

    val prettyName: String = localeHandler[s]
  }

  enum class OPERATOR(key: String, @JvmField val symbol: String) {
    PLUS_ENUM(TemplateConstants.PLUS_KEY, TemplateConstants.PLUS_SYMBOL),
    MINUS_ENUM(TemplateConstants.MINUS_KEY, TemplateConstants.MINUS_SYMBOL),
    MULTIPLY_ENUM(TemplateConstants.MULTIPLY_KEY, TemplateConstants.MULTIPLY_CROSS_SYMBOL);

    @JvmField
    val prettyName: String = localeHandler[key]

    companion object {
      fun stringArray(): Array<String?> {
        val values = entries.toTypedArray()
        val array = arrayOfNulls<String>(values.size)
        for (i in values.indices) {
          array[i] = values[i].symbol
        }
        return array
      }
    }
  }

  enum class Fill(s: String) {
    EMPTY("emptyCircle"),
    FULL("fullCircle"),
    NONE("none");

    val prettyName: String = localeHandler[s]
  }

  enum class IntervalType {
    MIXED, IMPROPER, DECIMAL, WHOLE
  }

  enum class Component {
    INTERVAL, START_LINE, END_LINE, START_SEGMENT, END_SEGMENT, POINT
  }

  enum class HorizontalJustify {
    RIGHT, CENTER, LEFT, TRIM;
  }

  enum class VerticalJustify {
    TOP, CENTER, BOTTOM;
  }

  enum class TemplateType(
    val key: String,
    numComponents: Int,
    operandComponents: Array<String>,
    solutionComponents: Array<String>
  ) {
    SIMPLE_ENUM(
      TemplateConstants.SIMPLE_KEY, 1,
      TemplateConstants.SIMPLE_COMPONENT_OPERAND_NAMES,
      TemplateConstants.SIMPLE_COMPONENT_SOLUTION_NAMES
    ),
    FRACTION_ENUM(
      TemplateConstants.FRACTION_KEY, 3,
      TemplateConstants.FRACTION_COMPONENT_OPERAND_NAMES,
      TemplateConstants.FRACTION_COMPONENT_SOLUTION_NAMES
    ),
    RADICAL_ENUM(
      TemplateConstants.DIVISION_KEY, 1,
      TemplateConstants.RADICAL_OPERAND_NAMES,
      TemplateConstants.RADICAL_SOLUTION_NAMES
    );

    @JvmField
    val prettyName: String = localeHandler[key]
    val numberComponents: Int = numComponents
    val operandComponents: List<String> = operandComponents.map { localeHandler[it] }
    val solutionComponents: List<String> = solutionComponents.map { localeHandler[it] }
  }
}