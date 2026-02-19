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
import org.brailleblaster.exceptions.BBNotifyException
import org.brailleblaster.utd.BRFWriter
import org.brailleblaster.utd.UTDTranslationEngine
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.utils.ALL_VOLUMES
import org.brailleblaster.utd.utils.convertBBX2PEF
import org.brailleblaster.utd.utils.stripUTDRecursive
import org.brailleblaster.util.Notify
import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.nameWithoutExtension

/**
 * BrailleBlaster ZIP (BBZ) file
 */
class BBZArchiver(
    override var path: Path,
    private var zipFS: FileSystem,
    private var bbxPath: Path,
    override val bbxDocument: Document
) : BaseArchiver(null, null), Archiver2 {
    enum class BBZSaveOptions : SaveOptions {
        IncludeBRF,
        IncludePEF
    }

    init {
        isZipValid(zipFS)
    }

    override fun resolveSibling(descendant: Path): Path {
        // Remap to Zip Path as Paths must be from same filesystem
        val itr: Iterator<Path> = descendant.iterator()
        val zipDescendant = zipFS.getPath(
            itr.next().toString(),
            *(itr.asSequence().map { it.toString() }.toList().toTypedArray())
        )
        return try {
            bbxPath.resolveSibling(zipDescendant)
        } catch (e: Exception) {
            throw RuntimeException("Failed to resolve '$zipDescendant' as sibling of $bbxPath", e)
        }
    }

    override fun save(destPath: Path, doc: Document, engine: UTDTranslationEngine, options: Set<SaveOptions>) {
        if (destPath == path) {
            saveExisting(doc, zipFS, path, engine, options)
        } else {
            saveNew(doc, destPath, engine, options)
        }
        try {
            ZipHandles.close(path)
            zipFS = ZipHandles.open(path, false)
            isZipValid(zipFS)
        }
        catch (e: Exception) {
            Notify.showException("Failed to save to existing file at $destPath.\n" +
                "The file location may not be available, or the file may have been opened by another program." +
                " Try closing the file and opening it again.", e)
        }
    }

    override fun saveAs(destPath: Path, doc: Document, engine: UTDTranslationEngine, options: Set<SaveOptions>) {
        if (destPath == path) {
            saveExisting(doc, zipFS, path, engine, options)
        } else {
            saveNew(doc, destPath, engine, options)
        }

        // switching FS, cleanup old one
        ZipHandles.close(path)
        path = destPath
        zipFS = ZipHandles.open(path, false)
        bbxPath = zipFS.getPath(bbxPath.toString())
        isZipValid(zipFS)
    }

    private fun saveExisting(
        doc: Document,
        destZipFS: FileSystem,
        destZipPath: Path,
        engine: UTDTranslationEngine,
        options: Set<SaveOptions?>
    ) {
        val relativeBBXPath = bbxPath.toString()
        try {
            log.debug("Saving to existing zip {}", bbxPath)
            val newBBXPath = destZipFS.getPath(relativeBBXPath)
            saveBBX(newBBXPath, doc)

            //write location file so bbz archiver can find this in the future
            val docPath = destZipFS.getPath(LOCATION_FILE)
            log.debug("Writing {} to {}", relativeBBXPath, docPath.toUri())
            Files.write(
                docPath,
                listOf(relativeBBXPath),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
        } catch (e: Exception) {
            Notify.showException("Failed to save to existing ZIP at $destZipPath", e)
        }
        if (options.contains(BBZSaveOptions.IncludePEF)) {
            // Create the PEF and save in the BBZ
            val pefPath = destZipFS.getPath("/document.pef")
            try {
                BufferedOutputStream(
                    Files.newOutputStream(
                        pefPath,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
                    )
                ).use { pefOut ->
                    convertBBX2PEF(
                        doc,
                        relativeBBXPath,
                        engine,
                        ALL_VOLUMES,
                        pefOut
                    )
                }
            } catch (e: IOException) {
                throw RuntimeException("Failed to create and insert the PEF into the BBZ.", e)
            }
        }
        if (options.contains(BBZSaveOptions.IncludeBRF)) {
            val brfPath = destZipFS.getPath("/document.brf")
            try {
                Files.newBufferedWriter(
                    brfPath, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
                ).use { writer -> engine.toBRF(doc, writer, BRFWriter.OPTS_DEFAULT, BRFWriter.EMPTY_PAGE_LISTENER) }
            } catch (e: IOException) {
                throw RuntimeException("Failed to create and insert the BRF into the BBZ.", e)
            }
        }
        isZipValid(zipFS)
    }

    private fun saveNew(doc: Document, destZipPath: Path, engine: UTDTranslationEngine, options: Set<SaveOptions?>) {
        try {
            log.debug("Saving to new zip {}", destZipPath)
            if (ZipHandles.has(destZipPath)) {
                throw BBNotifyException("Cannot save over other open file $destZipPath")
            }
            if (Files.exists(destZipPath)) {
                try {
                    Files.delete(destZipPath)
                } catch (e: Throwable) {
                    throw RuntimeException("Failed to delete existing file at $destZipPath", e)
                }
            }
            isZipValid(zipFS)
            val newZipFS = ZipHandles.open(destZipPath, true)
            //Copy over old zip contents
            Files.walkFileTree(zipFS.getPath("/"), object : FileVisitor<Path> {
                @Throws(IOException::class)
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (dir.toString() == "/") {
                        //Can't remake the root
                        return FileVisitResult.CONTINUE
                    }
                    Files.copy(
                        dir,
                        newZipFS.getPath(dir.toString()),
                        StandardCopyOption.COPY_ATTRIBUTES
                    )
                    return FileVisitResult.CONTINUE
                }

                @Throws(IOException::class)
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    Files.copy(
                        file,
                        newZipFS.getPath(file.toString()),
                        StandardCopyOption.COPY_ATTRIBUTES
                    )
                    return FileVisitResult.CONTINUE
                }

                @Throws(IOException::class)
                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                    throw UnsupportedOperationException("Not supported yet.", exc)
                }

                @Throws(IOException::class)
                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    if (exc != null) {
                        throw UnsupportedOperationException("Not supported yet.")
                    }
                    return FileVisitResult.CONTINUE
                }
            })
            saveExisting(doc, newZipFS, destZipPath, engine, options)
            ZipHandles.close(destZipPath)
        } catch (e: BBNotifyException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException("Unable to save to $destZipPath", e)
        }
    }

    private fun isZipValid(zip: FileSystem?) {
        check(zip!!.isOpen) { "zipFS at $path is not open: $zip" }
    }
    override val extensionsAndDescription: Map<String, String>
        get() = Loader.INSTANCE.extensionsAndDescription

    @Throws(IOException::class)
    override fun close() {
        ZipHandles.close(path)
    }

    class Loader : ArchiverFactory.FileLoader {
        @Throws(Exception::class)
        override fun tryLoad(file: Path, fileData: ArchiverFactory.ParseData): Archiver2? {
            val zipFS = ZipHandles.open(file, false)
            val locationFile = loadLocationFile(file, zipFS)
            return if (locationFile == null) {
                log.info("No BBX location file found")
                null
            } else {
                log.debug("Loading " + locationFile.toUri())
                BBZArchiver(
                    file,
                    zipFS,
                    locationFile,
                    XMLHandler().load(locationFile, "")
                )
            }
        }

        override val extensionsAndDescription: Map<String, String> = mapOf(
            "*.bbz" to "BB Archive (*.bbz)"
        )

        companion object {
            private val log = LoggerFactory.getLogger(Loader::class.java)
            val INSTANCE = Loader()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(BBZArchiver::class.java)

        /**
         * File in ZIP archive that says where the BBX is stored
         */
        const val LOCATION_FILE = "/bbx_location"
        fun loadLocationFile(zipPath: Path?, zipFS: FileSystem): Path? {
            //Verify this is a valid BBZ archive that can be opened later
            val locationPath = zipFS.getPath(LOCATION_FILE)
            return if (!Files.exists(locationPath)) {
                //throw new RuntimeException(LOCATION_FILE + " not found in zip " + zipPath);
                null
            } else try {
                val locationDataRaw = Files.readAllLines(locationPath, StandardCharsets.UTF_8)
                if (locationDataRaw.size != 1) {
                    throw RuntimeException("Location data is in unexpected format")
                }
                val locationData = locationDataRaw[0]
                zipFS.getPath(locationData)
            } catch (e: Exception) {
                throw RuntimeException("Unable to load $LOCATION_FILE in ZIP", e)
            }
        }

        @Throws(IOException::class)
        fun saveBBX(destPath: Path, doc: Document) {
            log.debug("Writing BBX to {}", destPath.toUri())
            val newDoc = doc.copy()
            newDoc.stripUTDRecursive()
            Files.newOutputStream(
                destPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            ).use { out ->
                XMLHandler().save(
                    newDoc,
                    out
                )
            }
        }

        @Throws(IOException::class)
        fun createImportedBBZ(importedFrom: Path, bbxDocument: Document): BBZArchiver {
            val docBaseName = importedFrom.nameWithoutExtension
            val bbzFile = File.createTempFile("$docBaseName-", ".bbz")
            bbzFile.deleteOnExit()
            return createImportedBBZ(importedFrom, bbzFile.toPath(), bbxDocument)
        }

        @Throws(IOException::class)
        fun createImportedBBZ(importedFrom: Path?, bbzPath: Path, bbxDocument: Document): BBZArchiver {
            val archiver = createNewBBZ(bbzPath, bbxDocument)
            archiver.importedFrom = importedFrom
            return archiver
        }

        @Throws(IOException::class)
        fun createNewBBZ(bbzPath: Path, bbxDocument: Document): BBZArchiver {
            val zipFS = ZipHandles.open(bbzPath, true)
            // Create the location file, believe it may be necessary for BBZ to be valid.
            val locationPath = zipFS.getPath(LOCATION_FILE)
            val bbxPathStr = "/document.bbx"
            Files.write(
                locationPath,
                listOf(bbxPathStr),
                Charsets.UTF_8,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
            // Now create the archiver.
            val bbxPath = zipFS.getPath(bbxPathStr)
            return BBZArchiver(bbzPath, zipFS, bbxPath, bbxDocument)
        }
    }
}
