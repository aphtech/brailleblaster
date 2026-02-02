/*
 * Copyright (C) 2025-2026 American Printing House for the Blind
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

import nu.xom.Serializer
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.jsoup.nodes.Document
import java.io.OutputStream
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

private const val OPF_PATH = "package.opf"

object EBraillePackager {
    private val RESOURCE_ITEMS = buildList {
        javaClass.getResource("/org/brailleblaster/ebraille/css/default.css")?.let {
            add(ResourceItem("ebraille/css/default.css", it, "text/css"))
        }
    }
    fun createEbraillePackage(outPath: Path, docs: List<Document>) {
        packageDocument(outPath, docs.mapIndexed { i, doc -> HtmlItem("ebraille/document${i}.html", doc) } + RESOURCE_ITEMS)
    }
    private fun packageDocument(outPath: Path, packageItems: List<PackageItem>) {
        ZipArchiveOutputStream(FileChannel.open(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)).use { zos ->
            zos.writeMimetype()
            zos.writeItems(packageItems)
            zos.writeOpf(packageItems)
            zos.writeContainer()
            zos.closeArchiveEntry()
        }
    }
}

private const val MIMETYPE_DATA = "application/epub+zip"

private fun createXomSerializer(output: OutputStream, encoding: String = "UTF-8", indent: Int = 4): Serializer = Serializer(output, encoding).apply { this.indent = indent }

private fun ZipArchiveOutputStream.writeMimetype() {
    putArchiveEntry(ZipArchiveEntry("mimetype").apply { method = ZipArchiveEntry.STORED })
    writeUsAscii(MIMETYPE_DATA)
}

private fun ZipArchiveOutputStream.writeItems(items: List<PackageItem>) {
    for (item in items) {
        putArchiveEntry(ZipArchiveEntry(item.path))
        item.write(this)
    }
}

private fun ZipArchiveOutputStream.writeOpf(docItems: List<PackageItem>) {
    putArchiveEntry(ZipArchiveEntry(OPF_PATH))
    createXomSerializer(this).write(createOpf(docItems))
}

private fun ZipArchiveOutputStream.writeContainer(opfPath: String = OPF_PATH) {
    putArchiveEntry(ZipArchiveEntry("META-INF/container.xml"))
    createXomSerializer(this).write(createContainerXml(opfPath))
}