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
package org.brailleblaster.utd.utils

import org.brailleblaster.utils.DC_NS
import org.brailleblaster.utils.PEF_NS
import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext

class PEFNamespaceContext : NamespaceContext {
    override fun getNamespaceURI(prefix: String): String {
        return when (prefix) {
            XMLConstants.DEFAULT_NS_PREFIX, "pef" -> PEF_NS
            "dc" -> DC_NS
            XMLConstants.XMLNS_ATTRIBUTE -> XMLConstants.XMLNS_ATTRIBUTE_NS_URI
            XMLConstants.XML_NS_PREFIX -> XMLConstants.XML_NS_URI
            else -> XMLConstants.NULL_NS_URI
        }
    }

    override fun getPrefix(namespaceURI: String): String? {
        return when (namespaceURI) {
            PEF_NS -> XMLConstants.DEFAULT_NS_PREFIX
            DC_NS -> "dc"
            XMLConstants.XML_NS_PREFIX -> XMLConstants.XML_NS_URI
            XMLConstants.XMLNS_ATTRIBUTE_NS_URI -> XMLConstants.XMLNS_ATTRIBUTE
            else -> null
        }
    }

    override fun getPrefixes(namespaceURI: String): Iterator<String> {
        val prefixes: List<String> = when (namespaceURI) {
            PEF_NS -> listOf(XMLConstants.DEFAULT_NS_PREFIX, "pef")
            DC_NS -> listOf("dc")
            XMLConstants.XML_NS_URI -> listOf(XMLConstants.XML_NS_PREFIX)
            XMLConstants.XMLNS_ATTRIBUTE_NS_URI -> listOf(XMLConstants.XMLNS_ATTRIBUTE)
            else -> listOf()
        }
        return prefixes.iterator()
    }

}