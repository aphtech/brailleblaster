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
package org.brailleblaster.ebraille

import nu.xom.Document
import nu.xom.Element
import org.brailleblaster.bbx.BBX
import org.brailleblaster.libembosser.utils.BrailleMapper
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.UTDHelper
import org.brailleblaster.utils.xml.BB_NS
import org.brailleblaster.utils.xml.UTD_NS
import org.brailleblaster.utils.xom.attributes

fun convertBbxToHtml(document: Document): org.jsoup.nodes.Document {
    val bbxRoot = requireNotNull(document.rootElement) { "BBX document must have a root element" }
    require(bbxRoot.namespaceURI == BB_NS && bbxRoot.localName == "bbdoc") { "Document must be a BBX document." }
    val htmlDoc = org.jsoup.nodes.Document.createShell("").also {
        it.insertChildren(0, org.jsoup.nodes.DocumentType("html", "", ""))
    }
    htmlDoc.head().appendChildren(bbxRoot.getFirstChildElement("head", BB_NS)?.processHead() ?: listOf<org.jsoup.nodes.Node>())
    htmlDoc.body().appendChildren(bbxRoot.childElements.filter { BBX.SECTION.ROOT.isA(it) }.flatMap { it.processRoot() })
    return htmlDoc
}

private fun Element.processHead(): Collection<org.jsoup.nodes.Node> {
    return listOf()
}

private fun Element.processRoot(): Iterable<org.jsoup.nodes.Node> {
    return childElements.flatMap {
        when {
            BBX.BLOCK.STYLE.isA(it) -> it.processStyle()
            BBX.BLOCK.DEFAULT.isA(it) -> listOf(it.processParagraph())
            else -> it.childElements.flatMap { e -> e.processRoot() }
        }
    }
}

private fun Element.processStyle(): Iterable<org.jsoup.nodes.Element> {
    val style = getAttributeValue(UTDElements.UTD_STYLE_ATTRIB)
    return when(style) {
        "Centered Heading" -> listOf(processParagraph(tag = "h1"))
        else -> listOf()
    }
}
private fun Element.processParagraph(tag: String = "p"): org.jsoup.nodes.Element {
    return org.jsoup.nodes.Element(tag).appendText(UTDHelper.getDescendantBrlFast(this).joinToString { BrailleMapper.ASCII_TO_UNICODE_FAST.map(it.value) })
}