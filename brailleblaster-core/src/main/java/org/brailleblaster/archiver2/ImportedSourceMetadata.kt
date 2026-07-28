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
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.brailleblaster.utd.internal.xml.FastXPath

/**
 * Bibliographic metadata read directly from a book's original source markup - a NIMAS/EPUB OPF
 * package's `dc-metadata`, or a NIMAS dtbook `<head>` - before BBX conversion, so it can be
 * reused later (e.g. by the eBraille exporter) without re-parsing the source.
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
        if (isEmpty) return
        val store = DocumentUTDConfig.NIMAS
        title?.let { store.setSetting(doc, key("title"), it) }
        identifier?.let { store.setSetting(doc, key("identifier"), it) }
        date?.let { store.setSetting(doc, key("date"), it) }
        if (creators.isNotEmpty()) {
            store.setSetting(doc, key("creators.count"), creators.size.toString())
            creators.forEachIndexed { i, value -> store.setSetting(doc, key("creators.$i"), value) }
        }
    }

    companion object {
        private const val ROOT_KEY = "importedSourceMetadata"
        private fun key(field: String) = "$ROOT_KEY.$field"

        fun load(doc: Document): ImportedSourceMetadata {
            val store = DocumentUTDConfig.NIMAS
            val creatorCount = store.getSetting(doc, key("creators.count"))?.toIntOrNull() ?: 0
            val creators = (0 until creatorCount).mapNotNull { store.getSetting(doc, key("creators.$it")) }
            return ImportedSourceMetadata(
                title = store.getSetting(doc, key("title")),
                creators = creators,
                identifier = store.getSetting(doc, key("identifier")),
                date = store.getSetting(doc, key("date"))
            )
        }

        /** Extracts dc:title/creator/identifier/date from an OPF package's metadata, shared by NIMAS and EPUB books. */
        fun fromOpf(opfDocument: Document): ImportedSourceMetadata = ImportedSourceMetadata(
            title = OPFUtils.getDCElementValuesCaseInsensitive(opfDocument, "title").firstOrNull { it.isNotBlank() },
            creators = OPFUtils.getDCElementValuesCaseInsensitive(opfDocument, "creator").filter { it.isNotBlank() },
            identifier = OPFUtils.getDCElementValuesCaseInsensitive(opfDocument, "identifier").firstOrNull { it.isNotBlank() },
            date = OPFUtils.getDCElementValuesCaseInsensitive(opfDocument, "date").firstOrNull { it.isNotBlank() }
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
