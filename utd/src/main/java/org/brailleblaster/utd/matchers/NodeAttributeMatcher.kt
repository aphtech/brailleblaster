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
import jakarta.xml.bind.annotation.XmlAttribute
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.NamespaceMap
import java.util.*

@XmlAccessorType(XmlAccessType.PROPERTY)
class NodeAttributeMatcher : NodeNameMatcher {
    @XmlAttribute
    val selfAttribName: String?
    private var _selfAttribValue: List<String>? = null

    @XmlAttribute
    val selfAttribNamespace: String?

    constructor() : super() {
        selfAttribName = null
        _selfAttribValue = null
        selfAttribNamespace = null //Default per element.getAttribute(String)
    }

    @JvmOverloads
    constructor(
        selfName: String?,
        selfAttribName: String?,
        selfAttribValue: String?,
        selfAttribNamespace: String? = null,
        namespace: String? = null
    ) : super(selfName, namespace) {
        this.selfAttribName = selfAttribName
        this.selfAttribValue = selfAttribValue
        this.selfAttribNamespace = selfAttribNamespace
    }

    override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
        if (node !is Element) {
            return false
        }

        if (super.nodeName != null && !super.isMatch(node, namespaces)) return false

        if (selfAttribName == null) {
            //No attribute given to check
            return true
        }

        //Check attributes
        val namespace: String?
        if (selfAttribNamespace != null) {
            namespace = namespaces.getNamespace(selfAttribNamespace)
            if (namespace == null) {
                throw NullPointerException("Searching for namespace $selfAttribNamespace but does not exist in namespace map")
            }
        } else {
            namespace = ""
        }

        val attrib = node.getAttribute(selfAttribName, namespace) ?: return false

        if (_selfAttribValue == null) {
            //Key exists but not value to check
            return true
        }

        for (curValue in _selfAttribValue) {
            if (curValue == attrib.value) {
                return true
            }
        }
        return false
    }

    @get:XmlAttribute
    var selfAttribValue: String?
        get() = _selfAttribValue?.joinToString(separator = "|")
        set(value) {
            _selfAttribValue = value?.split('|')
        }


    val selfAttribValueList: List<String>
        get() = _selfAttribValue ?: emptyList()

    override fun hashCode(): Int {
        var hash = 7
        hash = 83 * hash + super.hashCode()
        hash = 83 * hash + Objects.hashCode(this.selfAttribName)
        //Convert to string as different arrays are never equal
        hash = 83 * hash + Objects.hashCode(this.selfAttribValue)
        hash = 83 * hash + Objects.hashCode(this.selfAttribNamespace)
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        val obj = other as NodeAttributeMatcher
        if (!super.equals(other)) return false
        if (this.selfAttribName != obj.selfAttribName) return false
        //Convert to string as different arrays are never equal
        if (this.selfAttribValue != obj.selfAttribValue) return false
        return this.selfAttribNamespace == obj.selfAttribNamespace
    }

    override fun toString(): String {
        return "NodeAttributeMatcher{" + "selfAttribName=" + selfAttribName + ", selfAttribValue=" + this.selfAttribValue + ", selfAttribNamespace=" + selfAttribNamespace + ", super=" + super.toString() + '}'
    }
}
