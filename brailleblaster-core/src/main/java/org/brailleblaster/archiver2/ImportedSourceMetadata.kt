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
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Required bibliographic metadata for a book, read from its original source markup - a NIMAS/EPUB
 * OPF package's `dc-metadata`, or a NIMAS dtbook `<head>` - before BBX conversion, so it can be
 * reused later (e.g. by the eBraille exporter) without re-parsing the source.
 *
 * Every field is required, so any field the source doesn't provide is filled with a default
 * constant (see [defaults]) rather than left blank or absent - a BBX document always has a
 * complete, well-formed record of this metadata after import, regardless of what the source
 * format provided.
 *
 * Persisted into the BBX `<head>` as the same OPF-style `<metadata>` block (Dublin Core elements)
 * that a source OPF or an eBraille package uses, so [fromOpf] can read both without a separate
 * BBX-only parser.
 */
data class ImportedSourceMetadata(
    val title: String,
    val creators: List<String>,
    val identifier: String,
    val date: String
) {
    fun saveTo(doc: Document) {
        val headElem = DocumentUTDConfig.NIMAS.getOrCreateHeadElement(doc)
        for (existing in headElem.getChildElements(METADATA_ELEMENT, OPF_NS)) {
            existing.detach()
        }
        headElem.appendChild(toMetadataElement())
    }

    private fun toMetadataElement(): Element = Element(METADATA_ELEMENT, OPF_NS).apply {
        appendChild(dcElement("title", title))
        creators.forEach { appendChild(dcElement("creator", it)) }
        appendChild(dcElement("identifier", identifier))
        appendChild(dcElement("date", date))
    }

    private fun dcElement(localName: String, value: String): Element = Element("dc:$localName", DC_NS).apply {
        appendChild(value)
    }

    companion object {
        private const val METADATA_ELEMENT = "metadata"
        private const val DEFAULT_TITLE = "-"
        private const val DEFAULT_CREATOR = "-"

        /** The default constants used to fill in any field the source doesn't provide. */
        fun defaults(clock: Clock = Clock.systemUTC(), uuidProvider: () -> String = { UUID.randomUUID().toString() }): ImportedSourceMetadata =
            ImportedSourceMetadata(
                title = DEFAULT_TITLE,
                creators = listOf(DEFAULT_CREATOR),
                identifier = "urn:uuid:${uuidProvider()}",
                date = LocalDate.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE)
            )

        fun load(doc: Document, clock: Clock = Clock.systemUTC(), uuidProvider: () -> String = { UUID.randomUUID().toString() }): ImportedSourceMetadata {
            val metadataElem = DocumentUTDConfig.NIMAS.getHeadElement(doc)
                ?.getFirstChildElement(METADATA_ELEMENT, OPF_NS)
                ?: return defaults(clock, uuidProvider)
            return fromOpf(metadataElem, clock, uuidProvider)
        }

        /** Extracts dc:title/creator/identifier/date from an OPF-style metadata block - shared by NIMAS/EPUB source OPFs and the BBX head. */
        fun fromOpf(
            opfSource: Node,
            clock: Clock = Clock.systemUTC(),
            uuidProvider: () -> String = { UUID.randomUUID().toString() }
        ): ImportedSourceMetadata {
            val defaults = defaults(clock, uuidProvider)
            return ImportedSourceMetadata(
                title = OPFUtils.getDCElementValuesCaseInsensitive(opfSource, "title").firstOrNull { it.isNotBlank() } ?: defaults.title,
                creators = OPFUtils.getDCElementValuesCaseInsensitive(opfSource, "creator").filter { it.isNotBlank() }.ifEmpty { defaults.creators },
                identifier = OPFUtils.getDCElementValuesCaseInsensitive(opfSource, "identifier").firstOrNull { it.isNotBlank() } ?: defaults.identifier,
                date = OPFUtils.getDCElementValuesCaseInsensitive(opfSource, "date").firstOrNull { it.isNotBlank() } ?: defaults.date
            )
        }

        /** Extracts dc:Title/Creator/Identifier/Date from a NIMAS dtbook's `<head><meta name="dc:X" content="Y"/></head>` block. */
        fun fromDtbookHead(
            dtbookDocument: Document,
            clock: Clock = Clock.systemUTC(),
            uuidProvider: () -> String = { UUID.randomUUID().toString() }
        ): ImportedSourceMetadata {
            fun metaValues(name: String): List<String> = FastXPath.descendant(dtbookDocument)
                .filterIsInstance<Element>()
                .filter { it.localName == "meta" && (it.parent as? Element)?.localName == "head" }
                .filter { it.getAttributeValue("name")?.equals(name, ignoreCase = true) == true }
                .mapNotNull { it.getAttributeValue("content") }
                .filter { it.isNotBlank() }
                .toList()

            val defaults = defaults(clock, uuidProvider)
            return ImportedSourceMetadata(
                title = metaValues("dc:Title").firstOrNull() ?: defaults.title,
                creators = metaValues("dc:Creator").ifEmpty { defaults.creators },
                identifier = metaValues("dc:Identifier").firstOrNull() ?: defaults.identifier,
                date = metaValues("dc:Date").firstOrNull() ?: defaults.date
            )
        }
    }
}
