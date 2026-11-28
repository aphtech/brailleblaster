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
package org.brailleblaster.archiver2

import org.brailleblaster.bbx.legacy.LegacyImportReport
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Collections
import java.util.WeakHashMap
import kotlin.io.path.nameWithoutExtension

/**
 * Bookkeeping for documents that were converted from an older BrailleBlaster release when opened.
 *
 * A converted archiver is marked as imported from the original file, so the first save always
 * goes through Save As and the original is only replaced if the user picks its name.
 */
object LegacyImports {
    private val reports: MutableMap<Archiver2, LegacyImportReport> =
        Collections.synchronizedMap(WeakHashMap())

    /** Suggested Save As name for a converted file: `<name> (converted).bbz`. */
    fun convertedName(original: Path): Path = Paths.get("${original.nameWithoutExtension} (converted).bbz")

    /** Mark [archiver] as converted from [original] and remember [report] for the open notice. */
    fun markConverted(archiver: Archiver2, original: Path, report: LegacyImportReport) {
        archiver.importedFrom = original
        archiver.newPath = convertedName(original)
        reports[archiver] = report
    }

    /** The conversion report for [archiver], removed so it is only shown once. */
    fun takeReport(archiver: Archiver2): LegacyImportReport? = reports.remove(archiver)
}
