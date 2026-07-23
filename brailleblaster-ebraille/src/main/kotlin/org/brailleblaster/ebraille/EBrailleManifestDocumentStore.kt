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

import nu.xom.Document
import org.brailleblaster.utd.config.DocumentUTDConfig
import java.time.Clock

internal object EBrailleManifestDocumentStore {
    private const val ROOT_KEY = "ebrailleManifest"
    private const val VERSION_KEY = "$ROOT_KEY.version"

    fun load(doc: Document, clock: Clock = Clock.systemUTC()): EBrailleManifest? {
        if (!hasAnyPersistedManifestData(doc)) {
            return null
        }

        val defaults = EBrailleManifest.defaults(clock)
        return defaults.copy(
            title = readSingleValue(doc, "title", defaults.title),
            creators = readListValues(doc, "creators", defaults.creators),
            identifier = readSingleValue(doc, "identifier", defaults.identifier),
            languages = readListValues(doc, "languages", defaults.languages),
            date = readSingleValue(doc, "date", defaults.date),
            dateCopyrighted = readSingleValue(doc, "dateCopyrighted", defaults.dateCopyrighted),
            brailleCellType = readSingleValue(doc, "brailleCellType", defaults.brailleCellType),
            brailleSystem = readSingleValue(doc, "brailleSystem", defaults.brailleSystem),
            completeTranscription = readSingleValue(doc, "completeTranscription", defaults.completeTranscription),
            producers = readListValues(doc, "producers", defaults.producers)
        )
    }

    fun save(doc: Document, manifest: EBrailleManifest) {
        writeSingleValue(doc, "title", manifest.title)
        writeListValues(doc, "creators", manifest.creators)
        writeSingleValue(doc, "identifier", manifest.identifier)
        writeListValues(doc, "languages", manifest.languages)
        writeSingleValue(doc, "date", manifest.date)
        writeSingleValue(doc, "dateCopyrighted", manifest.dateCopyrighted)
        writeSingleValue(doc, "brailleCellType", manifest.brailleCellType)
        writeSingleValue(doc, "brailleSystem", manifest.brailleSystem)
        writeSingleValue(doc, "completeTranscription", manifest.completeTranscription)
        writeListValues(doc, "producers", manifest.producers)
        DocumentUTDConfig.NIMAS.setSetting(doc, VERSION_KEY, "1")
    }

    private fun hasAnyPersistedManifestData(doc: Document): Boolean {
        val directKeys = listOf(
            VERSION_KEY,
            key("title", "value"),
            key("identifier", "value"),
            key("date", "value"),
            key("dateCopyrighted", "value"),
            key("brailleCellType", "value"),
            key("brailleSystem", "value"),
            key("completeTranscription", "value"),
            key("creators", "count"),
            key("languages", "count"),
            key("producers", "count")
        )
        return directKeys.any { DocumentUTDConfig.NIMAS.getSetting(doc, it) != null }
    }

    private fun writeSingleValue(doc: Document, field: String, value: ManifestValue) {
        DocumentUTDConfig.NIMAS.setSetting(doc, key(field, "value"), value.value)
        DocumentUTDConfig.NIMAS.setSetting(doc, key(field, "source"), value.source.name)
        DocumentUTDConfig.NIMAS.setSetting(doc, key(field, "defaulted"), value.defaulted.toString())
    }

    private fun writeListValues(doc: Document, field: String, values: List<ManifestValue>) {
        DocumentUTDConfig.NIMAS.setSetting(doc, key(field, "count"), values.size.toString())
        values.forEachIndexed { i, value ->
            writeSingleValue(doc, "$field.$i", value)
        }
    }

    private fun readSingleValue(doc: Document, field: String, fallback: ManifestValue): ManifestValue {
        val value = DocumentUTDConfig.NIMAS.getSetting(doc, key(field, "value")) ?: return fallback
        val source = DocumentUTDConfig.NIMAS.getSetting(doc, key(field, "source"))
            ?.let { runCatching { ManifestValueSource.valueOf(it) }.getOrNull() }
            ?: ManifestValueSource.AUTHORED
        val defaulted = DocumentUTDConfig.NIMAS.getSetting(doc, key(field, "defaulted"))?.toBoolean() ?: false
        return ManifestValue(value = value, source = source, defaulted = defaulted)
    }

    private fun readListValues(doc: Document, field: String, fallback: List<ManifestValue>): List<ManifestValue> {
        val count = DocumentUTDConfig.NIMAS.getSetting(doc, key(field, "count"))?.toIntOrNull() ?: return fallback
        val values = (0 until count)
            .map { i -> readSingleValue(doc, "$field.$i", ManifestValue("", ManifestValueSource.AUTHORED)) }
            .filter { it.value.isNotBlank() }
        return if (values.isEmpty()) fallback else values
    }

    private fun key(field: String, suffix: String): String = "$ROOT_KEY.$field.$suffix"
}