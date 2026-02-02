/*
 * Copyright (C) 2026 American Printing House for the Blind
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

import nu.xom.Attribute
import nu.xom.Document
import nu.xom.Element
import org.brailleblaster.utils.xml.DC_NS
import org.brailleblaster.utils.xml.OPF_NS
import java.io.OutputStream
import java.net.URL

fun createOpf(items: List<PackageItem>): Document = Document(Element("package", OPF_NS).apply {
    val itemMap = items.mapIndexed { i, item -> "file${i}" to item }.toMap()
    addNamespaceDeclaration("dc", DC_NS)
    addAttribute(Attribute("version", "3.0"))
    addAttribute(Attribute("unique-identifier", "bookid"))
    appendChild(Element("metadata", OPF_NS))
    appendChild(Element("manifest", OPF_NS).apply {
        for ((id, item) in itemMap) {
            appendChild(Element("item", OPF_NS).apply {
                addAttribute(Attribute("id", id))
                addAttribute(Attribute("href", item.path))
                addAttribute(Attribute("media-type", item.mediaType))
            })
        }
    })
    appendChild(Element("spine", OPF_NS).apply {
        for (id in itemMap.filterValues { it.includeInSpine }.keys) {
            appendChild(Element("item", OPF_NS).apply {
                addAttribute(Attribute("idref", id))
            })
        }
    })
})

interface PackageItem {
    val path: String
    val mediaType: String
    val includeInSpine: Boolean
    fun write(output: OutputStream)
}

data class HtmlItem(override val path: String, val document: org.jsoup.nodes.Document, override val includeInSpine: Boolean = true) : PackageItem {
    override val mediaType: String = "application/xhtml+xml"
    override fun write(output: OutputStream) {
        output.bufferedWriter(Charsets.UTF_8).also {
            document.html(it)
        }.flush()
    }
}

data class ResourceItem(override val path: String, val resourceUrl: URL, override val mediaType: String,
                        override val includeInSpine: Boolean = false) : PackageItem {
    override fun write(output: OutputStream) {
        resourceUrl.openStream().use { it.copyTo(output) }
    }
}