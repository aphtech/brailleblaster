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
package org.brailleblaster.utd.actions

import nu.xom.Attribute
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.utd.ITranslationEngine
import org.brailleblaster.utd.TextSpan
import org.brailleblaster.utd.exceptions.UTDTranslateException
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.xml.UTD_NS
import org.brailleblaster.utils.xom.NodeContext
import org.brailleblaster.utils.xom.nodeCache
import java.util.*

/**
 * This action will continue to walk through the child nodes, whilst not linking the node with a brl
 * node. This action is normally used for container nodes which are not used in Braille translation.
 */
open class GenericAction : IAction {
    private fun processTextNode(node: Text, context: ITranslationEngine): List<TextSpan> {
        return listOf(TextSpan(node, node.value))
    }

    protected open fun processElement(
        node: Element, context: ITranslationEngine
    ): List<TextSpan> {
        val processedInput: MutableList<TextSpan> = ArrayList()

        //   if node has insertions, insert preInsert
        val nodeAction = context.actionMap.findValueOrDefault(node)
        if (nodeAction is InsertAction) {
            var attribute = node.getAttribute("inserted")
            if (attribute == null) {
                attribute = Attribute("inserted", "false")
                node.addAttribute(attribute)
            }
            if (attribute.value == "false") {
                attribute.value = "true"

                //   do pre and post insertions now
                val preInsert = nodeAction.preInsert
                // String midInsert = ((InsertAction)nodeAction).getMidInsert();//TODO
                val postInsert = nodeAction.postInsert
                if (preInsert != null) {
                    val span = Element("span", UTD_NS)
                    span.addAttribute(Attribute("table", "DIRECT"))
                    span.appendChild(preInsert)
                    node.insertChild(span, 0)
                }
                if (postInsert != null) {
                    val span = Element("span", UTD_NS)
                    span.addAttribute(Attribute("table", "DIRECT"))
                    span.appendChild(postInsert)
                    node.appendChild(span)
                }
            }
        }
        val parentContext = nodeCache.getUnchecked(node)
        var i = 0
        while (i < node.childCount) {
            val child = node.getChild(i)
            val nodeContext = NodeContext(child, parentContext, i)
            nodeCache.put(child, nodeContext)
            val action = context.actionMap.findValueOrDefault(child)
            val childTextSpans = action.applyTo(child, context)
            addActionAttributes(child, action)
            processedInput.addAll(childTextSpans)
            i++
        }
        return processedInput
    }

    override fun applyTo(node: Node, context: ITranslationEngine): List<TextSpan> {
        try {
            if (node is Text) {
                return processTextNode(node, context)
            } else if (node is Element) {
                return processElement(node, context)
            }
        } catch (e: UTDTranslateException) {
            // Don't keep wrapping the exception otherwise it will bubble up to the root level
            throw e
        } catch (e: Exception) {
            throw UTDTranslateException("Failed at processing node " + node.toXML(), e)
        }
        return emptyList()
    }

    fun addActionAttributes(n: Node?, action: IAction?) {
        if (action != null
            && action.javaClass.simpleName in IGNORE_ACTION_NAMES
        ) {
            return
        }
        if (n is Element) {
            if (action != null) {
                val actionAttribute = Attribute(UTDElements.UTD_ACTION_ATTRIB, action.javaClass.simpleName)
                n.addAttribute(actionAttribute)
            }
        }
    }

    override fun hashCode(): Int {
        var hash = 7
        // Simple match on class name, gives sane .hashCode() while not breaking subclasses
        hash = 89 * hash + Objects.hashCode(this.javaClass)
        return hash
    }

    override fun equals(other: Any?): Boolean {
        return if (other == null) false else javaClass == other.javaClass
        // Simple match on class name, gives sane .hashCode() while not breaking subclasses
    }

    companion object {
        private val IGNORE_ACTION_NAMES = listOf("GenericAction", "FlushAction", "GenericBlockAction")
    }
}