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
package org.brailleblaster.perspectives.braille.mapping.elements

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.bbx.BBX
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.math.numberLine.NumberLineConstants
import org.brailleblaster.math.spatial.MatrixConstants
import org.brailleblaster.math.spatial.SpatialMathUtils
import org.brailleblaster.math.template.TemplateConstants
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.interfaces.Uneditable
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.util.Notify

class ReadOnlyTextMapElement(n: Node) : TextMapElement(n), Uneditable {
  var invalidMessage = localeHandler["readOnlyWarning"]

  init {
    val individualMessage = getMessage(n)
    if (individualMessage != null) {
      invalidMessage = individualMessage
    }
  }

  override fun blockEdit(m: Manager) {
    Notify.notify(invalidMessage, Notify.ALERT_SHELL_NAME)
  }

  companion object {
    private val localeHandler = getDefault()
    @JvmStatic
		fun getMessage(n: Node): String? {
      val isNumberLine: Node? =
        XMLHandler.ancestorVisitorElement(n) { node: Element -> BBX.CONTAINER.NUMBER_LINE.isA(node) }
      if (isNumberLine != null) {
        return NumberLineConstants.USE_EDITOR_WARNING
      }
      val isMatrix: Node? = XMLHandler.ancestorVisitorElement(n) { node: Element? -> BBX.CONTAINER.MATRIX.isA(node) }
      if (isMatrix != null) {
        return MatrixConstants.USE_EDITOR_WARNING
      }
      val isTemplate: Node? =
        XMLHandler.ancestorVisitorElement(n) { node: Element? -> BBX.CONTAINER.TEMPLATE.isA(node) }
      if (isTemplate != null) {
        return TemplateConstants.USE_EDITOR_WARNING
      }
      val isSpatialGrid: Node? =
        XMLHandler.ancestorVisitorElement(n) { node: Element? -> BBX.CONTAINER.SPATIAL_GRID.isA(node) }
      return if (isSpatialGrid != null) {
        SpatialMathUtils.USE_EDITOR_WARNING
      } else null
    }
  }
}