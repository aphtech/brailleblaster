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
import nu.xom.Node
import org.brailleblaster.utd.NamespaceMap
import org.brailleblaster.utils.xom.childNodes
import java.util.*

/**
 * Due to XPath being extremely slow for relatively simple functions, implement only a small subset
 * for basic counting operations
 */
@Suppress("UNUSED")
class CountingXPathLikeMatcher : NodeAncestorMatcher() {
    @XmlAttribute
    private var comparer: String? = null

    @XmlAttribute
    private var axes: String? = null

    @XmlAttribute
    private var count = 0
    override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
        if (selfMatcher?.isMatch(node, namespaces) == false) return false
        if (node.parent == null && axes != "child") {
            return false
        }

        // Match node across XPath axes
        val matchedNodes = when (axes) {
            "ancestor" -> {
                val doc = node.document
                generateSequence(node.parent) { it.parent }.takeWhile { it != doc }
            }
            "preceding-sibling" -> {
                val parent = node.parent
                parent.childNodes.asSequence().take(parent.indexOf(node).coerceAtLeast(0))
            }
            "following-sibling" -> {
                val parent = node.parent
                parent.childNodes.asSequence().drop(parent.indexOf(node).coerceAtLeast(0))
            }
            "child" -> node.childNodes.asSequence()
            else -> throw UnsupportedOperationException("Axes not implemented: $axes")
        }.count { parentMatcher?.isMatch(it, namespaces) != false }

        // Match count
        return when (comparer) {
            ">=" -> matchedNodes >= count
            ">" -> matchedNodes > count
            "=" -> matchedNodes == count
            else -> throw UnsupportedOperationException("Comparer not implemented: $comparer")
        }
    }

    override fun toString(): String {
        return ("CountingXPathLikeMatcher{"
                + "comparer="
                + comparer
                + ", axes="
                + axes
                + ", count="
                + count
                + ", "
                + super.toString()
                + '}')
    }

    override fun hashCode(): Int {
        var hash = 3
        hash = 31 * hash + super.hashCode()
        hash = 31 * hash + Objects.hashCode(comparer)
        hash = 31 * hash + Objects.hashCode(axes)
        hash = 31 * hash + count
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        val obj = other as CountingXPathLikeMatcher
        if (!super.equals(obj)) return false
        if (comparer != obj.comparer) return false
        return if (axes != obj.axes) false else count == obj.count
    }
}