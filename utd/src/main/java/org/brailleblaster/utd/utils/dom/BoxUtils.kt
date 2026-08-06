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
package org.brailleblaster.utd.utils.dom

import nu.xom.Element
import org.brailleblaster.utd.properties.UTDElements

/**
 * Class to work on boxline elements (eg. sidebar).
 */
object BoxUtils {
    /**
     * Strip the brl elements representing the boxlines from the element.
     *
     * This will remove the brl elements which represent the separator lines of the element. The element and its content still stay attached to the document, however the element's Braille will become unformatted and thus will require reformatting.
     *
     * @param element The element which represents the boxed item (eg. the sidebar element).
     */
    fun stripBoxBrl(element: Element?) {
        if (element == null) {
            return
        }
        var separator = getStartSeparator(element)
        separator?.detach()
        separator = getEndSeparator(element)
        separator?.detach()
    }

    /**
     * Get the start separator brl of the boxed element.
     *
     * @param element The boxed element (eg. sidebar)
     * @return The start separator brl.
     */
    fun getStartSeparator(element: Element): Element? {
        // Strip the start separator
        if (element.childCount > 0) {
            val firstChild = element.getChild(0)
            if (UTDElements.BRL.isA(firstChild)) {
                val childBrl = firstChild as Element
                if ("formatting" == childBrl.getAttributeValue("type") && "start" == childBrl.getAttributeValue("separator")) {
                    return childBrl
                }
            }
        }
        return null
    }

    /**
     * Get the end separator of a boxed element.
     *
     * @param element The boxed element (eg. a sidebar).
     * @return The brl of the end separator.
     */
    fun getEndSeparator(element: Element): Element? {
        // Strip the end separator brl
        val parent = element.parent
        if (parent != null) {
            var index = parent.indexOf(element) + 1
            if (index < parent.childCount) {
                var nextSibling = parent.getChild(index)
                if (nextSibling is Element && UTDElements.BRL.isA(nextSibling)) {
                    var brl: Element? = null
                    if ("formatting" != nextSibling.getAttributeValue("type")) {
                        // The boxed element has associated Braille content rather than its children.
                        index++
                        if (index < parent.childCount) {
                            nextSibling = parent.getChild(index)
                            if (UTDElements.BRL.isA(nextSibling)) {
                                brl = nextSibling as Element
                            }
                        }
                    } else {
                        brl = nextSibling
                    }
                    if (brl != null && "formatting" == brl.getAttributeValue("type") && "end" == brl.getAttributeValue("separator")) {
                        return brl
                    }
                }
            }
        }
        return null
    }

    /**
     * Remove the box element and place its content in its place.
     *
     * @param element The box element (eg. sidebar).
     */
    fun unbox(element: Element) {
        // Check there is a parent, cannot do otherwise
        val parent = element.parent ?: throw IllegalArgumentException("The element is not attached to a parent.")
        stripBoxBrl(element)
        val index = parent.indexOf(element)
        element.detach()
        val childCount = element.childCount
        for (i in 0 until childCount) {
            val child = element.removeChild(0)
            parent.insertChild(child, index + i)
        }
    }
}
