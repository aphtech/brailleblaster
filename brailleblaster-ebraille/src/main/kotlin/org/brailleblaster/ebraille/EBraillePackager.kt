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
import org.brailleblaster.utd.ITranslationEngine
import org.brailleblaster.utd.UTDTranslationEngine
import org.jsoup.nodes.Document
import java.io.OutputStream
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Clock

private const val OPF_PATH = "package.opf"

object EBraillePackager {
    private val RESOURCE_ITEMS = buildList {
        javaClass.getResource("/org/brailleblaster/ebraille/css/default.css")?.let {
            add(ResourceItem("ebraille/css/default.css", it, "text/css"))
        }
    }
    fun createEbraillePackage(
        outPath: Path,
        docs: List<Document>,
        title: String = "-",
        translationEngine: ITranslationEngine = UTDTranslationEngine(),
        manifest: EBrailleManifest? = null
    ) {
        val docItems = docs.mapIndexed { i, doc -> XHtmlItem("ebraille/document${i}.html", doc) }
        val navDoc = XHtmlItem("index.html", NavigationHtml.createNavigationHtml(docItems, title = title, translationEngine = translationEngine), properties = "nav")
        val packageItems = docItems + RESOURCE_ITEMS + navDoc
        val exportManifest = manifest?.withVolatileExportValues(docs, packageItems) ?: createManifestForExport(title, docs, packageItems)
        packageDocument(outPath, packageItems, exportManifest)
    }

    private fun packageDocument(outPath: Path, packageItems: List<PackageItem>, manifest: EBrailleManifest) {
        ZipArchiveOutputStream(FileChannel.open(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)).use { zos ->
            zos.writeMimetype()
            zos.writeItems(packageItems)
            zos.writeOpf(packageItems, manifest)
            zos.writeContainer()
            zos.closeArchiveEntry()
        }
    }

    private fun createManifestForExport(title: String, docs: List<Document>, packageItems: List<PackageItem>): EBrailleManifest {
        val titleValue = title.ifBlank { "-" }
        return EBrailleManifest.defaults().copy(
            title = ManifestValue(titleValue, ManifestValueSource.DERIVED, defaulted = titleValue == "-"),
            brailleCellType = EBrailleManifestDefaults.brailleCellType(docs),
            tactileGraphics = EBrailleManifestDefaults.tactileGraphics(packageItems)
        )
    }
}

private fun EBrailleManifest.withVolatileExportValues(docs: List<Document>, packageItems: List<PackageItem>, clock: Clock = Clock.systemUTC()): EBrailleManifest {
    val derivedCellType = EBrailleManifestDefaults.brailleCellType(docs)
    return copy(
        modified = EBrailleManifestDefaults.modified(clock),
        brailleCellType = ManifestValuePrecedence.choose(brailleCellType, derivedCellType) ?: derivedCellType,
        tactileGraphics = EBrailleManifestDefaults.tactileGraphics(packageItems)
    )
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

private fun ZipArchiveOutputStream.writeOpf(docItems: List<PackageItem>, manifest: EBrailleManifest) {
    putArchiveEntry(ZipArchiveEntry(OPF_PATH))
    createXomSerializer(this).write(createOpf(docItems, manifest))
}

private fun ZipArchiveOutputStream.writeContainer(opfPath: String = OPF_PATH) {
    putArchiveEntry(ZipArchiveEntry("META-INF/container.xml"))
    createXomSerializer(this).write(createContainerXml(opfPath))
}