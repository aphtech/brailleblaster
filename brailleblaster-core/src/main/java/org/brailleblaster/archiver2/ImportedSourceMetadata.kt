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
package org.brailleblaster.archiver2

import nu.xom.Document
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utils.xml.DC_NS
import org.brailleblaster.utils.xml.OPF_NS

/**
 * Bibliographic metadata read directly from a book's original source markup - a NIMAS/EPUB OPF
 * package's `dc-metadata`, or a NIMAS dtbook `<head>` - before BBX conversion, so it can be
 * reused later (e.g. by the eBraille exporter) without re-parsing the source.
 *
 * Persisted into the BBX `<head>` as the same OPF-style `<metadata>` block (Dublin Core elements)
 * that a source OPF or an eBraille package uses, so [fromOpf] can read both without a separate
 * BBX-only parser.
 *
 * Only fields that map 1:1 onto the source's own Dublin Core elements are captured here. Fields
 * that need interpretation to be spec-correct, such as deriving a braille-script language tag,
 * are left to dedicated default/derivation logic instead.
 */
data class ImportedSourceMetadata(
    val title: String? = null,
    val creators: List<String> = emptyList(),
    val identifier: String? = null,
    val date: String? = null
) {
    val isEmpty: Boolean
        get() = title == null && creators.isEmpty() && identifier == null && date == null

    fun saveTo(doc: Document) {
        val headElem = DocumentUTDConfig.NIMAS.getOrCreateHeadElement(doc)
        for (existing in headElem.getChildElements(METADATA_ELEMENT, OPF_NS)) {
            existing.detach()
        }
        if (isEmpty) return
        headElem.appendChild(toMetadataElement())
    }

    private fun toMetadataElement(): Element = Element(METADATA_ELEMENT, OPF_NS).apply {
        title?.let { appendChild(dcElement("title", it)) }
        creators.forEach { appendChild(dcElement("creator", it)) }
        identifier?.let { appendChild(dcElement("identifier", it)) }
        date?.let { appendChild(dcElement("date", it)) }
    }

    private fun dcElement(localName: String, value: String): Element = Element("dc:$localName", DC_NS).apply {
        appendChild(value)
    }

    companion object {
        private const val METADATA_ELEMENT = "metadata"

        fun load(doc: Document): ImportedSourceMetadata {
            val metadataElem = DocumentUTDConfig.NIMAS.getHeadElement(doc)
                ?.getFirstChildElement(METADATA_ELEMENT, OPF_NS)
                ?: return ImportedSourceMetadata()
            return fromOpf(metadataElem)
        }

        /** Extracts dc:title/creator/identifier/date from an OPF-style metadata block - shared by NIMAS/EPUB source OPFs and the BBX head. */
        fun fromOpf(opfSource: Node): ImportedSourceMetadata = ImportedSourceMetadata(
            title = OPFUtils.getDCElementValuesCaseInsensitive(opfSource, "title").firstOrNull { it.isNotBlank() },
            creators = OPFUtils.getDCElementValuesCaseInsensitive(opfSource, "creator").filter { it.isNotBlank() },
            identifier = OPFUtils.getDCElementValuesCaseInsensitive(opfSource, "identifier").firstOrNull { it.isNotBlank() },
            date = OPFUtils.getDCElementValuesCaseInsensitive(opfSource, "date").firstOrNull { it.isNotBlank() }
        )

        /** Extracts dc:Title/Creator/Identifier/Date from a NIMAS dtbook's `<head><meta name="dc:X" content="Y"/></head>` block. */
        fun fromDtbookHead(dtbookDocument: Document): ImportedSourceMetadata {
            fun metaValues(name: String): List<String> = FastXPath.descendant(dtbookDocument)
                .filterIsInstance<Element>()
                .filter { it.localName == "meta" && (it.parent as? Element)?.localName == "head" }
                .filter { it.getAttributeValue("name")?.equals(name, ignoreCase = true) == true }
                .mapNotNull { it.getAttributeValue("content") }
                .filter { it.isNotBlank() }
                .toList()

            return ImportedSourceMetadata(
                title = metaValues("dc:Title").firstOrNull(),
                creators = metaValues("dc:Creator"),
                identifier = metaValues("dc:Identifier").firstOrNull(),
                date = metaValues("dc:Date").firstOrNull()
            )
        }
    }
}
