package org.brailleblaster.utils.xml

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