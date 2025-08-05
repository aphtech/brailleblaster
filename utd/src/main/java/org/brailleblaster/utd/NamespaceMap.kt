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

import nu.xom.XPathContext
import org.brailleblaster.utd.exceptions.UTDException
import org.brailleblaster.utd.internal.NamespaceMapAdapter
import java.lang.reflect.Field
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter

/**
 * Mapping of namespace prefixes to namespace URI.
 */
@XmlJavaTypeAdapter(NamespaceMapAdapter::class)
class NamespaceMap() {

    /**
     * Get the XPathContext object representing this NamespaceMap.
     *
     * @return The XPathContext object representing this NamespaceMap.
     */
    val xPathContext: XPathContext = XPathContext()

    constructor(prefix: String, uri: String?) : this() {
        addNamespace(prefix, uri)
    }

    /**
     * Add a namespace prefix definition.
     *
     *
     * Use this method to define a mapping from a prefix to a namespace URI. Should you want to
     * remove an existing mapping then map the prefix to be removed to a null URI value.
     *
     * @param prefix The prefix to be defined.
     * @param uri The URI of the namespace. Set this to null if you wish to remove the namespace
     * prefix definition.
     */
    fun addNamespace(prefix: String, uri: String?) {
        xPathContext.addNamespace(prefix, uri)
    }

    /**
     * Get a set of the defined prefixes.
     *
     * @return A set containing the defined prefixes.
     */
    val prefixes: Set<String>
        get() = namespaceMappings.keys

    @Suppress("UNCHECKED_CAST")
    val namespaceMappings: Map<String, String>
        get() {
            try {
                return XPC_NAMESPACES[xPathContext] as Map<String, String>
            } catch (_: Exception) {
                throw UTDException("Problem in finding namespaces from XPathContext object")
            }
        }

    /**
     * Get the namespace URI defined for the prefix.
     *
     * @param prefix The prefix to find a URI for.
     * @return The namespace URI defined for the prefix. If the prefix is not found null is returned.
     */
    fun getNamespace(prefix: String?): String? {
        return xPathContext.lookup(prefix)
    }

    companion object {
        private val XPC_NAMESPACES: Field = XPathContext::class.java.getDeclaredField("namespaces")

        init {
            XPC_NAMESPACES.trySetAccessible()
        }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun plus(other: NamespaceMap): NamespaceMap {
        val result = NamespaceMap()
        val namespaces = XPC_NAMESPACES[result.xPathContext] as MutableMap<String, String>
        namespaces.putAll(XPC_NAMESPACES[this.xPathContext] as Map<out String, String>)
        namespaces.putAll(XPC_NAMESPACES[other.xPathContext] as Map<out String, String>)
        return result
    }

    @Suppress("UNCHECKED_CAST")
    operator fun plusAssign(other: NamespaceMap) {
        val namespaces = XPC_NAMESPACES[this.xPathContext] as MutableMap<String, String>
        namespaces.putAll(XPC_NAMESPACES[other.xPathContext] as Map<out String, String>)
    }
}