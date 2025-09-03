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
package org.brailleblaster.ebraille

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.jsoup.nodes.Document
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object EBraillePackager {
    private const val MIMETYPE_DATA = "application/epub+zip"
    fun packageDocument(outPath: Path, html: Document) {
        ZipArchiveOutputStream(FileChannel.open(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)).use { zos ->
            zos.putArchiveEntry(ZipArchiveEntry("mimetype").apply {
                method = ZipArchiveEntry.STORED
            })
            zos.writeUsAscii(MIMETYPE_DATA)
            zos.putArchiveEntry(ZipArchiveEntry("ebraille/document.html"))
            zos.writer(Charsets.UTF_8).let {
                html.html(it)
                it.flush()
            }
            zos.closeArchiveEntry()
        }
    }
}