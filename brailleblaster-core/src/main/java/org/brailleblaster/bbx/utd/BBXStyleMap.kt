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
package org.brailleblaster.bbx.utd

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBX.ListType
import org.brailleblaster.bbx.BBXUtils
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.IStyleMap
import org.brailleblaster.utd.NamespaceMap
import org.brailleblaster.utd.OverrideMap.AbstractNonMatcherMap
import org.brailleblaster.utd.Style
import org.brailleblaster.utd.config.StyleDefinitions
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utils.BB_NS
import org.brailleblaster.utils.UTD_NS

/**
 * Auto-generated style map for
 *
 *  * Margin and List styles to replace repetitive copy-pasted matches per style
 *  * Styles with style options
 */
class BBXStyleMap(private val styleDefs: StyleDefinitions) : AbstractNonMatcherMap<IStyle>(), IStyleMap {
    override val defaultValue: IStyle
        get() = Style()
    @Throws(NoSuchElementException::class)
    override fun findValue(node: Node): IStyle {
        if (node !is Element) {
            throw NoSuchElementException("No value found")
        }

        val prefix: String
        val indent: Int
        val runover: Int
        if (BBX.BLOCK.LIST_ITEM.isA(node)) {
            var listWrapper = node
            // get parent without using ancestorVisitor which requires node to be attached to document
            while (!BBX.CONTAINER.LIST.isA(listWrapper)) {
                val parent = listWrapper.parent as? Element
                    ?: //TODO: sometimes list items might be put in random places in the document
                    throw NoSuchElementException("No value found")
                listWrapper = parent
            }

            when (val listType = BBX.CONTAINER.LIST.ATTRIB_LIST_TYPE[listWrapper]) {
                ListType.POEM_LINE_GROUP -> {
                    //may be wrapped in a poem container which will have the list level
                    val poemWrapper =
                        FastXPath.ancestor(listWrapper).lastOrNull { element -> BBX.CONTAINER.LIST.isA(element) }
                    if (poemWrapper != null) {
//						if (BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.has(listWrapper)) {
                        //Do you really need to throw an exception here every time you have a listLevel on a linegroup with a poem parent?
//							throw new NodeException("POEM_LINE_GROUP is wrapped by poem but has listLevel attrib", node);

//						}

                        listWrapper = poemWrapper
                    }
                    prefix = listType.styleNamePrefix
                }

                ListType.NORMAL, ListType.DEFINITION, ListType.POEM -> prefix = listType.styleNamePrefix
                else -> throw NodeException("Unhandled list type", node)
            }
            indent = BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL[node]
            runover = BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL[listWrapper]
        } else if (BBX.BLOCK.MARGIN.isA(node)) {
            prefix = BBX.BLOCK.MARGIN.ATTRIB_MARGIN_TYPE[node].styleNamePrefix
            indent = BBX.BLOCK.MARGIN.ATTRIB_INDENT[node]
            runover = BBX.BLOCK.MARGIN.ATTRIB_RUNOVER[node]
        } else {
            throw NoSuchElementException("No value found")
        }

        val styleName = (prefix
                + BBXUtils.indentFromLevel(indent)
                + "-" //Stored runover is maximum indent, increase for extra braille indent
                //EG max indent is 1 cell, so must use 1-3 style
                + BBXUtils.runoverFromLevel(runover))
        val style = styleDefs.getStyleByName(styleName) ?: throw NodeException("Style is null for $styleName", node)
        return style

        //Map poems to GD
    }

    override var namespaces: NamespaceMap
        get() = BASIC_NAMESPACE_MAP
        set(value) { super.namespaces = value }

    companion object {
        val BASIC_NAMESPACE_MAP: NamespaceMap = NamespaceMap()

        init {
            BASIC_NAMESPACE_MAP.addNamespace("bb", BB_NS)
            BASIC_NAMESPACE_MAP.addNamespace("utd", UTD_NS)
        }
    }
}
