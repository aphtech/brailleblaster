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
package org.brailleblaster.utd.formatters

import nu.xom.Node
import org.brailleblaster.utd.FormatSelector
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.InsertionPatternEntry.Companion.listToMap
import org.brailleblaster.utd.PageBuilder
import org.brailleblaster.utd.RegexLineWrapper
import org.brailleblaster.utd.utils.UTDHelper

class MathFormatter : Formatter() {
    override fun format(
        node: Node, style: IStyle, pageBuilders: Set<PageBuilder>,
        formatSelector: FormatSelector
    ): MutableSet<PageBuilder> {
        val mutPageBuilders = pageBuilders.toMutableSet()
        if (mutPageBuilders.isEmpty()) {
            return mutPageBuilders
        }
        val brl = UTDHelper.getAssociatedBrlElement(node)
            ?: // This math seems to have no Braille
            return mutPageBuilders
        val styleMap = formatSelector.styleMap
        var pb = mutPageBuilders.last()
        mutPageBuilders.addAll(preFormat(node, pb, style, styleMap))
        val brailleSettings = formatSelector.engine.brailleSettings
        val mathLineWrapping = brailleSettings.mathBrailleCode.lineWrapping
        val mathStartLines = brailleSettings.mathBrailleCode.startLines
        val mathLineWrap = RegexLineWrapper(mathLineWrapping)
        val numSignPatterns = listToMap(mathStartLines)
        mathLineWrap.setLineStartInsertions(numSignPatterns)
        pb = mutPageBuilders.last()
        mutPageBuilders.addAll(pb.addBrl(brl, mathLineWrap))
        return mutPageBuilders
    }
}