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
import nu.xom.Nodes
import nu.xom.XPathException
import org.brailleblaster.utd.NamespaceMap

/**
 * Match nodes using XPath.
 *
 *
 * This node matcher implementation uses XPath expressions to determine whether a node is a
 * match. When writing the XPath expressions to be used by this matcher, you should write it as if
 * it were to be run on the document's root node and that the node being checked should be found in
 * the resulting nodeset.
 */
class XPathMatcher : INodeMatcher {
    @XmlAttribute(name = "expression")
    val expression: String?

    // Needed for JAXB
    @Suppress("UNUSED")
    private constructor() {
        expression = null
    }

    constructor(expression: String?) {
        requireNotNull(expression) { "The expression cannot be null" }
        this.expression = expression
    }

    override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
        val resultNodes: Nodes
        if (expression == null) {
            return false
        }
        try {
            resultNodes = node.query(expression, namespaces.xPathContext)
            if (resultNodes.size() > 1) throw RuntimeException(
                "XPath found too many results! Query "
                        + expression
                        + " returned "
                        + resultNodes.size()
                        + " nodes"
            )
        } catch (e: XPathException) {
            throw RuntimeException("Invalid XPath expression $expression", e)
        }
        return resultNodes.contains(node)
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + expression.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val obj = other as XPathMatcher
        return if (expression == null) {
            obj.expression == null
        } else expression == obj.expression
    }

    override fun toString(): String {
        return "XPathMatcher{expression=$expression}"
    }
}