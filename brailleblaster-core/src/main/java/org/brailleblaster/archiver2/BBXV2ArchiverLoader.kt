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

import org.brailleblaster.bbx.legacy.V2ToBBXConverter
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * Opens `.bbx` files written by BrailleBlaster 2.0 by converting them into a new BBZ.
 * Returns null for every other file, so current and 1.x BBX files go to [BBXArchiverLoader].
 */
object BBXV2ArchiverLoader : ArchiverFactory.FileLoader {
    private val log = LoggerFactory.getLogger(BBXV2ArchiverLoader::class.java)

    /** Empty: `*.bbx` is already listed by [BBXArchiverLoader]; listing it again would duplicate it in the Open dialog. */
    override val extensionsAndDescription: Map<String, String> = emptyMap()

    override fun tryLoad(file: Path, fileData: ArchiverFactory.ParseData): Archiver2? {
        val source = fileData.doc ?: return null
        if (!V2ToBBXConverter.isV2Document(source)) {
            return null
        }
        log.info("Detected BrailleBlaster 2.0 BBX {}", file)
        val (converted, report) = V2ToBBXConverter.convert(source)
        return BBZArchiver.createImportedBBZ(file, converted).also {
            LegacyImports.markConverted(it, file, report)
        }
    }
}
