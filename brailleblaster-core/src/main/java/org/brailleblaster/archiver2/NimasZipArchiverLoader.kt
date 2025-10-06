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

import nu.xom.Document
import org.brailleblaster.archiver2.ArchiverFactory.FileLoader.Companion.convert
import org.brailleblaster.archiver2.OPFUtils.ManifestEntry
import org.brailleblaster.archiver2.OPFUtils.findOPFFilesInFolder
import org.brailleblaster.archiver2.OPFUtils.getDCElementValueCaseInsensitive
import org.brailleblaster.archiver2.OPFUtils.getManifestItems
import org.brailleblaster.archiver2.ZipHandles.open
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Supplier

/**
 * Open and convert nimas books to BBX
 */
class NimasZipArchiverLoader : ArchiverFactory.FileLoader {
    override val extensionsAndDescription: Map<String, String> = mapOf(
        "*.zip" to "Nimas ZIP (*.zip)"
    )

    @Throws(Exception::class)
    override fun tryLoad(file: Path, fileData: ArchiverFactory.ParseData): Archiver2? {
        val zipFS = open(file, false)
        //Try to find opf
        val zipRoot = zipFS.getPath("/")

        var bookPath: Path? = null
        val opfFiles = findOPFFilesInFolder(zipRoot)
        if (!opfFiles.isEmpty()) {
            if (opfFiles.size > 1) {
                log.warn("Detected multiple OPF files, picking first: {}", opfFiles)
            }

            val opfFile = opfFiles.first()
            log.info("Detected nimas OPF file at {}", opfFile)

            val opfDocument: Document? = try {
                XMLHandler().load(opfFile)
            } catch (e: Exception) {
                log.error("Failed to open OPF file", e)
                null
            }

            if (opfDocument != null) {
                val bookManifest: ManifestEntry = guessNimasLocation(opfDocument)

                bookPath = opfFile.resolveSibling(bookManifest.href)

                //Issue #4693: Workaround for opf files that specify book with the wrong case
                if (!Files.exists(bookPath)) {
                    Files.list(bookPath.parent).use { filesStream ->
                        bookPath = filesStream
                            .filter { curPath: Path? ->
                                curPath!!.fileName.toString().equals(bookManifest.href, ignoreCase = true)
                            }
                            .findFirst()
                            .orElseThrow(Supplier {
                                NodeException(
                                    "Unable to find file ${bookManifest.href} in zip root ${opfFile.parent}", bookManifest.elem
                                )
                            }
                            )
                    }
                }
            }
        }

        if (bookPath == null) {
            log.warn("No opf files detected, brute forcing")
            Files.walk(zipRoot).use { filesStream ->
                bookPath = filesStream
                    .filter { path: Path? -> OPFUtils.pathNotHiddenOrInHiddenDirectory(path!!) }
                    .filter { curPath: Path? -> curPath!!.fileName.toString().endsWith(".xml") }
                    .filter { curPath: Path ->
                        try {
                            return@filter XMLHandler().load(curPath).rootElement.localName == "dtbook"
                        } catch (e: Exception) {
                            log.warn("Failed to detect xml root element of {}", curPath, e)
                            return@filter false
                        }
                    }.findFirst()
                    .orElse(null)
            }
            if (bookPath == null) {
                log.warn("File is not a nimas zip: {}", file)
                return null
            }
        }

        log.debug("book Path {}", bookPath)
        val convertedDoc = convert(bookPath, "nimas")
        return BBZArchiver(
            file,
            zipFS,
            bookPath.resolveSibling(bookPath.fileName.toString() + ".bbx"),
            convertedDoc
        )
    }

    companion object {
        val INSTANCE: NimasZipArchiverLoader = NimasZipArchiverLoader()
        private val log: Logger = LoggerFactory.getLogger(NimasZipArchiverLoader::class.java)

        /**
         * The reality of the OPF "standard"
         * - Every book has a different id
         * - Every mimeType you can think of for XML is used
         * - application/dtbook+html
         * - application/x-dtbook+xml
         * - application/dtbook+xml
         * - text/xml
         * - xml/document
         * - And presumably others
         * - The mimeType's are the same for the OPF and XML book
         *
         *
         * So we must guess and hope it's correct
         */
        fun guessNimasLocation(opfDocument: Document): ManifestEntry {
            val opfFormat = getDCElementValueCaseInsensitive(opfDocument, "format") ?: ""
            //TODO: When epub support is added this should be more explicit
            if (!opfFormat.contains("nimas", ignoreCase=true) //Standard name of DTD 2002
                || opfFormat != "ANSI/NISO Z39.86-2002"
            ) {
                log.warn(
                    "OPF at {} does not explicitly state nimas, says {}, assuming",
                    opfDocument.baseURI,
                    opfFormat
                )
            }

            //Find first XML file, most likely the nimas book
            for (curManifestEntry in getManifestItems(opfDocument)) {
                try {
                    if (curManifestEntry.href.endsWith(".xml")) {
                        return curManifestEntry
                    }
                } catch (e: Exception) {
                    throw NodeException("Mangled manifestEntry", curManifestEntry.elem, e)
                }
            }

            throw NodeException("No usable book entry found", opfDocument)
        }
    }
}
