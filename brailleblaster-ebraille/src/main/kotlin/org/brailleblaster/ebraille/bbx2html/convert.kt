/*
 * Copyright (C) 2025-2026 American Printing House for the Blind
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
package org.brailleblaster.ebraille.bbx2html

import nu.xom.Document
import nu.xom.Element
import org.brailleblaster.bbx.BBX
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.xml.BB_NS
import org.jsoup.Jsoup
import org.jsoup.nodes.Node

object BBX2HTML {
    private val FALLBACK_TEMPLATE = """
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
            <head></head>
            <body></body>
            </html>
        """.trimIndent()
    fun convertBbxToHtml(document: Document): org.jsoup.nodes.Document {
        val bbxRoot = requireNotNull(document.rootElement) { "BBX document must have a root element" }
        require(bbxRoot.namespaceURI == BB_NS && bbxRoot.localName == "bbdoc") { "Document must be a BBX document." }
        val template = javaClass.getResourceAsStream("/org/brailleblaster/ebraille/document_template.html")?.bufferedReader(Charsets.UTF_8)?.readText() ?: FALLBACK_TEMPLATE
        val htmlDoc = Jsoup.parse(template)
        htmlDoc.head().appendChildren(bbxRoot.getFirstChildElement("head", BB_NS)?.processHead() ?: listOf())
        htmlDoc.body().appendChildren(bbxRoot.childElements.filter { BBX.SECTION.ROOT.isA(it) }.flatMap { it.processChildren() })
        return htmlDoc
    }
}

internal val Element.style: String?
    get() = getAttributeValue(UTDElements.UTD_STYLE_ATTRIB)

@Suppress("UnusedReceiverParameter")
private fun Element.processHead(): Collection<Node> = listOf()

internal fun Element.processChildren(): Collection<Node> = childElements.flatMap {
    when(BBX.getTypeOrNull(it)) {
        BBX.SECTION -> it.processSection()
        BBX.CONTAINER -> it.processContainer()
        BBX.BLOCK -> it.processBlock()
        else -> it.processChildren()
    }
}

private fun Element.processSection(): Collection<Node> = processChildren()

