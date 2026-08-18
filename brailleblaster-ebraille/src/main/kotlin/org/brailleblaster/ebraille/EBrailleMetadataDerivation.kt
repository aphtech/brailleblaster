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
import org.jsoup.nodes.Document as JsoupDocument
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Helpers that derive the eBraille-only `package.opf` metadata values - braille cell type,
 * tactile graphics format summary, export timestamp, and default braille-table language - from
 * the actual content being packaged. These are always recomputed at export time rather than
 * stored, since they describe the specific export rather than the source document's canonical
 * bibliographic metadata (owned by [org.brailleblaster.archiver2.OpfMetadata]).
 */
private const val BRAILLE_PATTERN_BASE = 0x2800
private const val DOT_7_8_MASK = 0xC0
private val TABLE_LANGUAGE_PREFIX = Regex("^([a-z]{2,3})-")

internal fun deriveBrailleCellType(docs: List<JsoupDocument>): String {
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

    return if (dotCounts.isEmpty()) "6" else dotCounts.joinToString(" ")
}

internal fun deriveTactileGraphics(packageItems: List<PackageItem>): String {
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

    return if (formats.isEmpty()) "none" else formats.joinToString(" ")
}

internal fun currentModifiedTime(clock: Clock = Clock.systemUTC()): String {
    val now = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS)
    return DateTimeFormatter.ISO_INSTANT.format(now)
}

internal fun defaultEbrailleLanguage(engine: ITranslationEngine?): String {
    val languageCode = engine?.brailleSettings?.mainTranslationTable
        ?.let { TABLE_LANGUAGE_PREFIX.find(it.lowercase())?.groupValues?.get(1) }

    return if (languageCode != null) "$languageCode-Brai" else "en-Brai"
}