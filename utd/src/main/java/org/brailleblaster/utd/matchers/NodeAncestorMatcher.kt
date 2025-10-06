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
import java.util.*

/**
 * Matches nodes that match the given self* parameters and has a parent, grandparent, or ancestor
 * that matches the given parent* parameters
 */
open class NodeAncestorMatcher : INodeMatcher {
    private var _selfName: String? = null
    private var _selfAttribName: String? = null
    private var _selfAttribValue: String? = null
    private var _selfAttribNamespace: String? = null
    private var _selfNamespace: String? = null
    private var _parentName: String? = null
    private var _parentAttribName: String? = null
    private var _parentAttribValue: String? = null
    private var _parentAttribNamespace: String? = null
    private var _parentNamespace: String? = null
    var selfMatcher: NodeAttributeMatcher? = null
        protected set
    var parentMatcher: NodeAttributeMatcher? = null
        protected set

    constructor()

    constructor(
        selfName: String?,
        selfAttribName: String?,
        selfAttribValue: String?,
        parentName: String?,
        parentAttribName: String?,
        parentAttribValue: String?
    ) {
        this._selfName = selfName
        this._selfAttribName = selfAttribName
        this._selfAttribValue = selfAttribValue
        this._parentName = parentName
        this._parentAttribName = parentAttribName
        this._parentAttribValue = parentAttribValue
    }

    override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
        val doc = node.document
        if (selfMatcher != null && selfMatcher!!.isMatch(node, namespaces)) {
            if (parentMatcher == null) {
                return true
            }
            var parent: Node? = node.parent
            while (parent != null && parent !== doc) {
                if (parentMatcher!!.isMatch(parent, namespaces)) return true
                parent = parent.parent
            }
        }
        return false
    }

    @set:XmlAttribute
    var selfName: String?
        get() = _selfName
        set(selfName) {
            this._selfName = selfName
            updateSelf()
        }

    @set:XmlAttribute
    var selfAttribName: String?
        get() = _selfAttribName
        set(selfAttribName) {
            this._selfAttribName = selfAttribName
            updateSelf()
        }

    @set:XmlAttribute
    var selfAttribValue: String?
        get() = _selfAttribValue
        set(selfAttribValue) {
            this._selfAttribValue = selfAttribValue
            updateSelf()
        }

    @set:XmlAttribute
    var selfAttribNamespace: String?
        get() = _selfAttribNamespace
        set(selfAttribNamespace) {
            this._selfAttribNamespace = selfAttribNamespace
            updateSelf()
        }

    @set:XmlAttribute
    var selfNamespace: String?
        get() = _selfNamespace
        set(selfNamespace) {
            this._selfNamespace = selfNamespace
            updateSelf()
        }

    @set:XmlAttribute
    var parentName: String?
        get() = _parentName
        set(parentName) {
            this._parentName = parentName
            updateParent()
        }

    @set:XmlAttribute
    var parentAttribName: String?
        get() = _parentAttribName
        set(parentAttribName) {
            this._parentAttribName = parentAttribName
            updateParent()
        }

    @set:XmlAttribute
    var parentAttribValue: String?
        get() = _parentAttribValue
        set(parentAttribValue) {
            this._parentAttribValue = parentAttribValue
            updateParent()
        }

    @set:XmlAttribute
    var parentAttribNamespace: String?
        get() = _parentAttribNamespace
        set(parentAttribNamespace) {
            this._parentAttribNamespace = parentAttribNamespace
            updateParent()
        }

    @set:XmlAttribute
    var parentNamespace: String?
        get() = _parentNamespace
        set(parentNamespace) {
            this._parentNamespace = parentNamespace
            updateParent()
        }

    private fun updateSelf() {
        selfMatcher =
            NodeAttributeMatcher(_selfName, _selfAttribName, _selfAttribValue, _selfAttribNamespace, _selfNamespace)
    }

    private fun updateParent() {
        parentMatcher = NodeAttributeMatcher(
            _parentName,
            _parentAttribName,
            _parentAttribValue,
            _parentAttribNamespace,
            _parentNamespace
        )
    }

    override fun toString(): String {
        return "NodeAncestorMatcher{selfMatcher=$selfMatcher, parentMatcher=$parentMatcher}"
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 89 * hash + super.hashCode()
        hash = 89 * hash + Objects.hashCode(this.selfMatcher)
        hash = 89 * hash + Objects.hashCode(this.parentMatcher)
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        val obj = other as NodeAncestorMatcher
        if (!super.equals(other)) return false
        if (this.selfMatcher != obj.selfMatcher) return false
        return this.parentMatcher == obj.parentMatcher
    }
}
