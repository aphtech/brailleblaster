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

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.ComparableStyle
import org.brailleblaster.utd.NamespaceMap
import org.brailleblaster.utd.internal.ComparableStyleAdapter
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.debug
import org.slf4j.LoggerFactory

@XmlAccessorType(XmlAccessType.FIELD)
open class ActionAndStyleMatcher @JvmOverloads constructor(
    @field:XmlElement(name = "style") @field:XmlJavaTypeAdapter(
        ComparableStyleAdapter::class
    ) var styles: List<ComparableStyle> = mutableListOf(),
    @field:XmlElement(name = "action") var actions: List<String> = mutableListOf()
) : INodeMatcher {

    private fun checkAttributeValue(
            e: Element, attributeName: String?, possibleValues: List<String?>?): Boolean {
        val value = e.getAttributeValue(attributeName)
        return possibleValues!!.isEmpty() || value != null && checkString(value, possibleValues)
    }

    private fun checkString(value: String, possibleValues: List<String?>?): Boolean {
        for (other in possibleValues!!) {
            log.debug("Checking if {} contains {}", value, other)
            if (value.contains(other!!)) {
                return true
            }
        }
        return false
    }

    override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
        log.debug { String.format("For matcher %s checking actions and styles of %s", this, node.toXML()) }
        log.debug { String.format("Styles are %s", styles) }
        if (node !is Element) {
            log.debug { String.format("Node %s is not an element", node) }
            return false
        }
        if (!checkAttributeValue(node, UTDElements.UTD_ACTION_ATTRIB, actions)) {
            log.debug { String.format("Node %s action does not match", node) }
            return false
        }
        // if (!checkAttributeValue(e, UTDElements.UTD_STYLE_ATTRIB, styles)) {
        // 	log.debug("Node {} action does not match", e);
        // 	return false;
        // }
        if (styles.isNotEmpty()) {
            val nodeStyleName = node.getAttributeValue(UTDElements.UTD_STYLE_ATTRIB)
            // Deal with node not having style
            if (nodeStyleName == null) {
                log.debug { String.format("Node %s does not match", node) }
                return false
            }
            // See if style names match
            for (compStyle in styles) {
                if (compStyle.isInstanceOfStyle(nodeStyleName)) {
                    return true
                }
            }
            return false
        }
        log.debug("Node {} is a match", node)
        return true
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + actions.hashCode()
        result = prime * result + styles.hashCode()
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
        val o = other as ActionAndStyleMatcher
        return actions == o.actions && styles == o.styles
    }

    companion object {
        private val log = LoggerFactory.getLogger(ActionAndStyleMatcher::class.java)
    }
}