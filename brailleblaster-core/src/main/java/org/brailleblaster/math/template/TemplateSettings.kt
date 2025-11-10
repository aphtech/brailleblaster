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

import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.spatial.ISpatialMathSettings
import org.brailleblaster.math.spatial.SpatialMathEnum
import org.brailleblaster.math.spatial.SpatialMathEnum.OPERATOR
import org.brailleblaster.math.spatial.SpatialMathEnum.Passage
import org.slf4j.LoggerFactory

class TemplateSettings : ISpatialMathSettings {
  override var passage = Passage.NONE
  var isLongSolution = false
  var isStraightRadicalSymbol = false
  var operands = 2
  var solutions = 0
  var isTranslateIdentifierAsMath = false
  var operator: OPERATOR = defaultOperator
  var type: SpatialMathEnum.TemplateType = defaultType
  var isLinear = true

  companion object {
    private val log = LoggerFactory.getLogger(TemplateSettings::class.java)
    var defaultType = SpatialMathEnum.TemplateType.SIMPLE_ENUM
    var defaultOperator = OPERATOR.PLUS_ENUM

    @JvmStatic
    fun shouldFormatLinear(template: Template): Boolean {
      return MathModuleUtils.isNemeth
          && template.settings.isLinear
          && template.settings.type == SpatialMathEnum.TemplateType.RADICAL_ENUM
          && (template.brailleSolutions.isEmpty()
          || template.brailleSolutions[0].isBlank())
    }

    fun enumifyOperator(s: String): OPERATOR {
      for (w in OPERATOR.entries.toTypedArray()) {
        if (w.symbol == s) {
          return w
        }
      }
      log.error("Combo box does not match operator enum options, using default")
      return defaultOperator
    }

    fun enumifyType(s: String): SpatialMathEnum.TemplateType {
      for (w in SpatialMathEnum.TemplateType.entries.toTypedArray()) {
        if (w.prettyName == s) {
          return w
        }
      }
      log.error("Combo box does not match type enum options, using default")
      return defaultType
    }
  }
}
