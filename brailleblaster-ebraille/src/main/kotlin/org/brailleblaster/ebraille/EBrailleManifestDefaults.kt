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

import org.brailleblaster.utd.ITranslationEngine
import org.jsoup.nodes.Document
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Resolves which of several candidate [ManifestValue]s should win for a field, using the
 * provenance policy: user-entered (AUTHORED) values beat imported values, which beat calculated
 * values (DERIVED or DEFAULT). Blank values are treated as absent and never chosen.
 */
object ManifestValuePrecedence {
    private val TIER = mapOf(
        ManifestValueSource.AUTHORED to 0,
        ManifestValueSource.IMPORTED to 1,
        ManifestValueSource.DERIVED to 2,
        ManifestValueSource.DEFAULT to 2
    )

    fun choose(vararg candidates: ManifestValue?): ManifestValue? = candidates.asSequence()
        .filterNotNull()
        .filter { it.value.isNotBlank() }
        .minByOrNull { TIER.getValue(it.source) }

    fun chooseList(vararg candidates: List<ManifestValue>?): List<ManifestValue> = candidates.asSequence()
        .filterNotNull()
        .map { list -> list.filter { it.value.isNotBlank() } }
        .filter { it.isNotEmpty() }
        .minByOrNull { list -> list.minOf { TIER.getValue(it.source) } }
        ?: emptyList()
}

/**
 * Default/derivation calculators for required eBraille manifest metadata that can be computed
 * from the document, the export package, or ambient state, rather than authored by a user.
 */
object EBrailleManifestDefaults {
    private const val BRAILLE_PATTERN_BASE = 0x2800
    private const val DOT_7_8_MASK = 0xC0
    private val TABLE_LANGUAGE_PREFIX = Regex("^([a-z]{2,3})-")

    /** Derives cell type ("6", "8", or "6 8" for mixed content) from Unicode braille patterns actually present in the exported documents. */
    fun brailleCellType(docs: List<Document>): ManifestValue {
        val dotCounts = docs.asSequence()
            .flatMap { it.text().asSequence() }
            .mapNotNull { ch ->
                val bits = ch.code - BRAILLE_PATTERN_BASE
                when {
                    bits !in 0..0xFF -> null
                    bits and DOT_7_8_MASK != 0 -> "8"
                    else -> "6"
                }
            }
            .toSortedSet()

        return if (dotCounts.isEmpty()) {
            ManifestValue("6", ManifestValueSource.DEFAULT, defaulted = true)
        } else {
            ManifestValue(dotCounts.joinToString(" "), ManifestValueSource.DERIVED)
        }
    }

    /** Derives the tactile-graphics format list from the image resources actually bundled in the export package, most-frequent first. */
    fun tactileGraphics(packageItems: List<PackageItem>): ManifestValue {
        val formats = packageItems.asSequence()
            .map { it.mediaType.substringBefore(';').trim().lowercase() }
            .filter { it.startsWith("image/") }
            .map { it.substringAfter('/') }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }

        return if (formats.isEmpty()) {
            ManifestValue("none", ManifestValueSource.DEFAULT, defaulted = true)
        } else {
            ManifestValue(formats.joinToString(" "), ManifestValueSource.DERIVED)
        }
    }

    /** The current UTC time, truncated to seconds, for the volatile dcterms:modified field. */
    fun modified(clock: Clock = Clock.systemUTC()): ManifestValue {
        val now = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS)
        return ManifestValue(DateTimeFormatter.ISO_INSTANT.format(now), ManifestValueSource.DERIVED)
    }

    /** Keeps an existing identifier if present, otherwise generates a new UUID-based one. */
    fun identifier(existing: ManifestValue?, uuidProvider: () -> String = { UUID.randomUUID().toString() }): ManifestValue =
        existing?.takeIf { it.value.isNotBlank() }
            ?: ManifestValue("urn:uuid:${uuidProvider()}", ManifestValueSource.DEFAULT, defaulted = true)

    /** Derives a language tag from the engine's configured literary translation table (e.g. "en-ueb-g2.ctb" -> "en-Brai"), falling back to "en-Brai". */
    fun language(engine: ITranslationEngine?): ManifestValue {
        val languageCode = engine?.brailleSettings?.mainTranslationTable
            ?.let { TABLE_LANGUAGE_PREFIX.find(it.lowercase())?.groupValues?.get(1) }

        return if (languageCode != null) {
            ManifestValue("$languageCode-Brai", ManifestValueSource.DERIVED)
        } else {
            ManifestValue("en-Brai", ManifestValueSource.DEFAULT, defaulted = true)
        }
    }
}
