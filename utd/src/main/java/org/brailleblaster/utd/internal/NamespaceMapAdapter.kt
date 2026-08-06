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
package org.brailleblaster.utd.internal

import org.brailleblaster.utd.NamespaceMap
import jakarta.xml.bind.annotation.adapters.XmlAdapter

class NamespaceMapAdapter : XmlAdapter<AdaptedNamespaceMap?, NamespaceMap?>() {
    override fun marshal(namespaces: NamespaceMap?): AdaptedNamespaceMap? {
        return if (namespaces == null) {
            null
        } else {
            val nsList = namespaces.namespaceMappings.filter { (k, _) -> k != "xml" }.map { (k,v) -> NamespaceDefinition(k, v) }.toMutableList()
            AdaptedNamespaceMap(nsList)
        }
    }

    override fun unmarshal(namespaces: AdaptedNamespaceMap?): NamespaceMap? {
        return if (namespaces == null) {
            null
        } else {
            val result = NamespaceMap()
            for ((prefix, uri) in namespaces.namespaces) {
                result.addNamespace(prefix, uri)
            }
            result
        }
    }
}