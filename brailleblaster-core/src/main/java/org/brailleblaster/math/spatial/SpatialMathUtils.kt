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
import org.brailleblaster.bbx.BBX
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.math.spatial.SpatialMathEnum.Translation
import org.brailleblaster.perspectives.braille.mapping.elements.LineBreakElement
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.WhiteSpaceElement
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.wordprocessor.WPManager

object SpatialMathUtils {
    private val localeHandler = getDefault()

    @JvmStatic
    fun translate(translation: Translation, input: String): String {
        return when (translation) {
            Translation.ASCII_MATH -> MathModule.translateAsciiMath(input)
            Translation.DIRECT -> input
            Translation.LITERARY -> MathModule.translateMainPrint(input)
            Translation.UNCONTRACTED -> MathModule.translateUncontracted(input)
        }
    }

    @JvmStatic
    fun currentIsSpatialGrid(): Boolean {
        val current: Node? = XMLHandler.ancestorVisitorElement(
            WPManager.getInstance().controller
                .simpleManager.currentCaret.node
        ) { node: Element? -> BBX.CONTAINER.SPATIAL_GRID.isA(node) }
        val isWhitespace = WPManager.getInstance().controller.mapList
            .current is WhiteSpaceElement
        return !isWhitespace && current != null
    }

    @JvmStatic
    fun print(grid: Grid) {
        for (i in grid.lines.indices) {
            println(grid.lines[i].toString())
        }
    }

    @JvmStatic
    fun middleSpatialMathPage(currentElement: TextMapElement): Boolean {
        return currentElement.node != null && currentElement !is LineBreakElement &&
                XMLHandler.ancestorVisitorElement(currentElement.node) { node: Element? ->
                    BBX.CONTAINER.SPATIAL_GRID.isA(
                        node
                    )
                } != null
    }

    @JvmStatic
    fun isSpatialMathPage(node: Node?): Boolean {
        return node != null && node.document != null && XMLHandler.ancestorElementIs(node) { e: Element ->
            BBX.CONTAINER.SPATIAL_GRID.isA(
                e
            )
        }
    }

    @JvmStatic
    fun getSpatialPageParent(node: Node): Element? {
        return XMLHandler.ancestorVisitorElement(node) { e: Element -> BBX.CONTAINER.SPATIAL_GRID.isA(e) }
    }

    @JvmField
    val USE_EDITOR_WARNING = localeHandler["spatialPageUseEditorWarning"]

    @JvmField
    val ROW_GROUP = localeHandler["rowGroup"]

    @JvmField
    val COL_LABEL = localeHandler["col"]

    @JvmField
    val CANCEL_LABEL = localeHandler["cancelLabel"]

    @JvmField
    val DELETE_CONTAINER = localeHandler["deleteContainer"]

    @JvmField
    val PREVIOUS_LABEL = localeHandler["previous"]

    @JvmField
    val NEXT_LABEL = localeHandler["next"]

    @JvmField
    val DELETE = localeHandler["delete"]

    @JvmField
    val OK_LABEL = localeHandler["insertContainer"]

}