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
import jakarta.xml.bind.annotation.XmlElement
import nu.xom.Comment
import nu.xom.Node
import nu.xom.ProcessingInstruction
import nu.xom.Text
import org.brailleblaster.utd.NamespaceMap
import org.brailleblaster.utd.properties.UTDElements
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NodeAncestorDelegatingMatcher : DelegatingMatcher() {
    enum class Position {
        ANY, START, END
    }

	@get:XmlElement
    var position: Position? = Position.ANY

    @get:XmlAttribute
    var distance: Int = 0
    override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
        if (matcher == null) {
            return true
        }
        val doc = node.document
        var curNode = node
        var loopLevel = this.distance
        if (loopLevel < 1) {
            loopLevel = Int.MAX_VALUE
        }
        var i = 0
        while (curNode !== doc && i < loopLevel) {
            val parent = curNode.parent ?: break
            if (Position.ANY != position) {
                val index = parent.indexOf(curNode)
                if (Position.START == position && index > 0) {
                    // Make sure it is not just brl elements which come in front.
                    // brl elements may come from formatting like box lines.
                    for (j in 0 until index) {
                        val tempNode = parent.getChild(j)
                        if (!isIgnorableNode(tempNode)) {
                            log.debug("Node {} is not at the start of the ancestor", curNode)
                            return false
                        }
                    }
                } else if (Position.END == position && index + 1 < parent.childCount) {
                    // Similar check for brl elements after.
                    for (j in index + 1 until parent.childCount) {
                        val tempNode = parent.getChild(j)
                        if (!isIgnorableNode(tempNode)) {
                            log.debug("Node {} is not at the end of the ancestor", curNode)
                            return false
                        }
                    }
                }
            }
            if (matcher!!.isMatch(parent, namespaces)) {
                return true
            }
            curNode = parent
            i++
        }
        return false
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = super.hashCode()
        result = prime * result + position.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || !super.equals(other)) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val o = other as NodeAncestorDelegatingMatcher
        return position == o.position
    }

    private fun isIgnorableNode(node: Node): Boolean {
        if (node is Comment || node is ProcessingInstruction) {
            return true
        }
        if (node is Text && node.value.isEmpty()) {
            return true
        }
        return UTDElements.BRL.isA(node)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(NodeAncestorDelegatingMatcher::class.java)
    }
}
