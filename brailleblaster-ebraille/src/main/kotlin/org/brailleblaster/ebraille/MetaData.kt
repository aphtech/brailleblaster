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
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * The eBraille-specific metadata that BrailleBlaster emits into `package.opf` in addition to the
 * canonical bibliographic metadata already owned by [org.brailleblaster.archiver2.ImportedSourceMetadata].
 */
data class EBrailleManifest(
    val format: String,
    val languages: List<String>,
    val modified: String,
    val dateCopyrighted: String,
    val brailleCellType: String,
    val brailleSystem: String,
    val completeTranscription: String,
    val producers: List<String>,
    val tactileGraphics: String
) {
    companion object {
        private val UTC_EPOCH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun defaults(
            languages: List<String> = listOf("en-Brai"),
            clock: Clock = Clock.systemUTC()
        ): EBrailleManifest {
            val now: Instant = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS)
            return EBrailleManifest(
                format = "eBraille 1.0",
                languages = languages.ifEmpty { listOf("en-Brai") },
                modified = DateTimeFormatter.ISO_INSTANT.format(now),
                dateCopyrighted = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC).format(UTC_EPOCH_FORMATTER),
                brailleCellType = "6",
                brailleSystem = "UEB",
                completeTranscription = "true",
                producers = listOf("-"),
                tactileGraphics = "none"
            )
        }
    }
}

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

internal fun EBrailleManifest.withVolatileExportValues(
    docs: List<JsoupDocument>,
    packageItems: List<PackageItem>,
    clock: Clock = Clock.systemUTC()
): EBrailleManifest = copy(
    modified = currentModifiedTime(clock),
    brailleCellType = deriveBrailleCellType(docs),
    tactileGraphics = deriveTactileGraphics(packageItems)
)