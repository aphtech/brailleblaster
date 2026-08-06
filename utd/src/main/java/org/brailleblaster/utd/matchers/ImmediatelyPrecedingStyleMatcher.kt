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

import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter
import nu.xom.Node
import org.brailleblaster.utd.ComparableStyle
import org.brailleblaster.utd.NamespaceMap
import org.brailleblaster.utd.internal.ComparableStyleAdapter
import org.brailleblaster.utd.properties.UTDElements

@Suppress("UNUSED")
class ImmediatelyPrecedingStyleMatcher : INodeMatcher {
    private val delegateMatcher: ImmediatelyPrecedingDelegatingMatcher = ImmediatelyPrecedingDelegatingMatcher()
    private val styleMatcher: ActionAndStyleMatcher?

    init {
        val attrMatcher = NotMatcher()
        attrMatcher.matcher = NodeAttributeMatcher(null, UTDElements.UTD_STYLE_ATTRIB, null)
        delegateMatcher.ignoreMatcher = attrMatcher
        styleMatcher = ActionAndStyleMatcher()
        delegateMatcher.matcher = styleMatcher
    }

    @get:XmlJavaTypeAdapter(ComparableStyleAdapter::class)
    @get:XmlElement(name = "style")
    var styles: List<ComparableStyle>
        get() = styleMatcher!!.styles
        set(styles) {
            styleMatcher!!.styles = styles
        }

    override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
        return delegateMatcher.isMatch(node, namespaces)
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + styleMatcher.hashCode()
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
        val o = other as ImmediatelyPrecedingStyleMatcher
        return styleMatcher?.equals(o.styleMatcher) ?: (o.styleMatcher == null)
    }
}
