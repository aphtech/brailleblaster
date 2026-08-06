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
import org.brailleblaster.utd.NamespaceMap

open class NodeNameMatcher @JvmOverloads constructor(nodeName: String? = null,
                                                     @set:XmlAttribute var namespace: String? = null
) : INodeMatcher {
    @set:XmlAttribute
    open var nodeName: String? = nodeName
        set(value) {
             field = value
            nodeNameParts = (value ?: "").split("|").toTypedArray()
        }
    private var nodeNameParts: Array<String> = (nodeName ?: "").split("|").toTypedArray()

    override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
        if (node is Element) {
            if (namespace != null) {
                val uri = namespaces.getNamespace(namespace)
                if (node.namespaceURI != uri) {
                    return false
                }
            }

            if (nodeName == null) if (javaClass == NodeNameMatcher::class.java) throw RuntimeException("No nodeName given")
            else  //Assume it matches, subclasses might not always require a node name
                return true

            //Check all names
            for (curName in nodeNameParts) if (node.localName == curName) return true
        }
        return false
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = (prime * result
                + namespace.hashCode())
        result = (prime * result
                + nodeName.hashCode())
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
        val o = other as NodeNameMatcher
        if (namespace == null) {
            if (o.namespace != null) {
                return false
            }
        } else if (namespace != o.namespace) {
            return false
        }
        return if (nodeName == null) {
            o.nodeName == null
        } else nodeName == o.nodeName
    }

    override fun toString(): String {
        return "NodeNameMatcher{nodeName=$nodeName}"
    }
}
