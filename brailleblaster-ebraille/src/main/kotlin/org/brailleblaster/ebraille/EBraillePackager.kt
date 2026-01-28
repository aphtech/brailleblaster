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
    fun packageDocument(outPath: Path, docs: List<Document>) {
        ZipArchiveOutputStream(FileChannel.open(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)).use { zos ->
            zos.writeMimetype()
            zos.writeVolumes(docs)
            zos.closeArchiveEntry()
        }
    }
}

private const val MIMETYPE_DATA = "application/epub+zip"

private fun ZipArchiveOutputStream.writeMimetype() {
    putArchiveEntry(ZipArchiveEntry("mimetype").apply { method = ZipArchiveEntry.STORED })
    writeUsAscii(MIMETYPE_DATA)
}

private fun ZipArchiveOutputStream.writeVolumes(docs: List<Document>) {
    for ((index, doc) in docs.withIndex()) {
        putArchiveEntry(ZipArchiveEntry("ebraille/document${index}.html"))
        writer(Charsets.UTF_8).let {
            doc.html(it)
            it.flush()
        }
    }
}
