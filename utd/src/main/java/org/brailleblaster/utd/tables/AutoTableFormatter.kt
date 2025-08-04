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
package org.brailleblaster.utd.tables

import nu.xom.Attribute
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.FormatSelector
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.PageBuilder
import org.brailleblaster.utd.formatters.Formatter
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.TableUtils
import org.brailleblaster.utd.utils.TableUtils.TableTypes

class AutoTableFormatter : Formatter() {
    private val simpleTable
        get() = TableFormat.SIMPLE.formatter
    private val listedTable
        get() = TableFormat.LISTED.formatter
    private val linearTable
        get() = TableFormat.LINEAR.formatter
    private val stairstepTable
        get() = TableFormat.STAIRSTEP.formatter
    override fun format(
        node: Node,
        style: IStyle,
        pageBuilders: Set<PageBuilder>,
        formatSelector: FormatSelector
    ): MutableSet<PageBuilder> {
        val mutPageBuilders = pageBuilders.toMutableSet()
        if (node !is Element) {
            return mutPageBuilders
        }
        val engine = formatSelector.engine
        when (TableUtils.detectType(node, engine.styleMap, engine.brailleSettings, engine.pageSettings)) {
            TableTypes.SIMPLE -> {
                node.addAttribute(Attribute("format", "simple"))
                node.addAttribute(Attribute(UTDElements.UTD_STYLE_ATTRIB,
                    engine.styleDefinitions.getStyleByName("Simple Table")!!.name))
                mutPageBuilders.addAll(simpleTable.format(node, style, mutPageBuilders, formatSelector))
            }
            TableTypes.LINEAR -> {
                node.addAttribute(Attribute("format", "simple"))
                node.addAttribute(Attribute(UTDElements.UTD_STYLE_ATTRIB,
                    engine.styleDefinitions.getStyleByName("Linear Table")!!.name))
                mutPageBuilders.addAll(linearTable.format(node, style, mutPageBuilders, formatSelector))
            }
            TableTypes.STAIRSTEP -> {
                node.addAttribute(Attribute("format", "stairstep"))
                node.addAttribute(Attribute(UTDElements.UTD_STYLE_ATTRIB,
                    engine.styleDefinitions.getStyleByName("Stairstep Table")!!.name))
                mutPageBuilders.addAll(stairstepTable.format(node, style, mutPageBuilders, formatSelector))
            }
            TableTypes.LISTED, TableTypes.NONTABLE -> {
                node.addAttribute(Attribute("format", "listed"))
                node.addAttribute(Attribute(UTDElements.UTD_STYLE_ATTRIB,
                    engine.styleDefinitions.getStyleByName("Listed Table")!!.name))
                mutPageBuilders.addAll(listedTable.format(node, style, mutPageBuilders, formatSelector))
            }
        }
        return mutPageBuilders
    }
}
