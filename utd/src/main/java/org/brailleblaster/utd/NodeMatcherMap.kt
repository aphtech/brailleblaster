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
package org.brailleblaster.utd

import nu.xom.Node
import org.apache.commons.collections4.map.ListOrderedMap
import org.brailleblaster.utd.matchers.INodeMatcher

abstract class NodeMatcherMap<T> : ListOrderedMap<INodeMatcher, T>, INodeMatcherMap<T> {
    final override val defaultValue: T
    final override var namespaces: NamespaceMap

    protected constructor(defaultValue: T) : super() {
        this.defaultValue = defaultValue
        this.namespaces = NamespaceMap()
    }

    constructor(map: NodeMatcherMap<T>) : super(map) {
        this.defaultValue = map.defaultValue
        this.namespaces = map.namespaces
    }

    override fun put(key: INodeMatcher, value: T): T? {
        return put(0, key, value)
    }

    override fun putAll(
        from: Map<out INodeMatcher, T>
    ) {
        putAll(0, from)
    }

    @Throws(NoSuchElementException::class)
    override fun findValue(node: Node): T {
        val it = this.mapIterator()
        while (it.hasNext()) {
            val matcher = it.next()
            if (matcher!!.isMatch(node, namespaces)) {
                return it.value
            }
        }
        throw NoSuchElementException("No value found")
    }
}
