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
package org.brailleblaster.utd.matchers

import jakarta.xml.bind.annotation.XmlAttribute
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.utd.NamespaceMap

/**
 * This class is meant to match a list unless is follows a cell 5 or cell 7
 * heading, top box line, directions, or is the child of another list.
 */
class MultipleExclusionsMatcher : NodeNameMatcher() {
    @set:XmlAttribute
    override var nodeName: String? = null
    @set:XmlAttribute
    var sometimesParent: String? = null
    @set:XmlAttribute
    var repeatOnce: String? = null
    private var neverParentParts: Array<String>? = null
    @set:XmlAttribute
    var neverParent: String?
        get() = neverParentParts?.joinToString(separator = "|")
        set(value) {
            neverParentParts = value?.split("|")?.toTypedArray()
        }
    override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
        if (node is Element) {
            if (node.localName != nodeName) {
                return false
            } else {
                // The list cannot have an ancestor that is a list
                val ancestorMatcher = NodeAncestorMatcher()
                ancestorMatcher.setSelfName(nodeName)
                ancestorMatcher.setParentName(nodeName)
                if (ancestorMatcher.isMatch(node, namespaces)) return false
            }
        } else {
            // Not an element, don't format as list
            return false
        }
        val doc = node.document
        var currentParent: Node? = node.parent
        if (currentParent !== doc) {
            var parentElement = currentParent as Element

            if (parentElement.localName == repeatOnce) {
                // Ancestor cannot be side bar unless that side bar has
                // a side bar parent.
                val ancestorMatcher = NodeAncestorMatcher()
                ancestorMatcher.setSelfName(repeatOnce)
                ancestorMatcher.setParentName(repeatOnce)
                if (ancestorMatcher.isMatch(currentParent, namespaces)) return true
                else {
                    for (i in 0..<currentParent.getChildCount()) {
                        if (currentParent.getChild(i) !== node && currentParent.getChild(i) is Text) {
                            return true
                        }
                    }
                }
            }
            var currentGrandparent: Node? = currentParent.parent
            if (currentGrandparent is Element) {
                var grandparentElement: Element = currentGrandparent

                while (currentParent != null && currentGrandparent != null && currentParent !== doc) {
                    for (neverParentPart in neverParentParts ?: arrayOf()) {
                        // Parent can never be h2|h3|h4|h5|h6
                        if (parentElement.localName == neverParentPart) {
                            return false
                        }
                        // Parent can be p unless its parent is
                        // h2|h3|h4|h5|h6
                        if (parentElement.localName == sometimesParent
                            && (grandparentElement.localName
                                    == neverParentPart)
                        ) {
                            return false
                        }
                    }
                    currentParent = currentParent.parent
                    if (currentParent is Element) parentElement = currentParent
                    currentGrandparent = currentGrandparent.parent
                    if (currentGrandparent is Element) grandparentElement = currentGrandparent
                }
            }
        }
        return true
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
