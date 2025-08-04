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

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import jakarta.xml.bind.DatatypeConverter
import nu.xom.Document
import org.brailleblaster.bbx.BookToBBXConverter
import org.brailleblaster.exceptions.BBNotifyException
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Locale
import javax.xml.stream.XMLInputFactory

/**
 * Handles opening book files and creating wrapping Archivers, converting to BBX
 * if necessary
 */
object ArchiverFactory {
    private val loaders: Multimap<Types, FileLoader> = ImmutableMultimap.builder<Types, FileLoader>().apply {
        put(Types.XML, BBXArchiverLoader.INSTANCE)
        // put BBZ archiver first so it can find the bbx_location file
        put(Types.ZIP, BBZArchiver.Loader.INSTANCE)
        put(Types.XML, NimasFileArchiverLoader)
        put(Types.ZIP, NimasZipArchiverLoader.INSTANCE)

        // these are the pandoc file types
        put(Types.DOCX, PandocArchiverLoader)
        put(Types.EPUB, PandocArchiverLoader)
        put(Types.MD, PandocArchiverLoader)
        put(Types.HTML, PandocArchiverLoader)
        put(Types.ODT, PandocArchiverLoader)
        put(Types.TEX, PandocArchiverLoader)
        put(Types.OTHER, BRLArchiverLoader.INSTANCE)
        put(Types.OTHER, BRFArchiverLoader.INSTANCE)
        put(Types.OTHER, TextArchiveLoader.INSTANCE)
    }.build()
    private val supportedExtensionAndDesc: Map<String, String> = buildMap {
        var alreadyLoaded = false // used to skip double loading pandoc loader
        for (loader in loaders.values()) {
            // hack to avoid multiple listing of file types for pandoc
            if (loader is PandocArchiverLoader) {
                alreadyLoaded = if (alreadyLoaded) {
                    continue
                } else {
                    true
                }
            }
            for ((key, value) in loader.extensionsAndDescription) {
                merge(key, value) { existing: String, current: String -> "$existing $current" }
            }
        }
    }.toSortedMap()
    private val pandocExts =
        listOf("*.docx", "*.epub", "*.htm", "*.html", "*.xhtml;*.xhtm;*.xht", "*.md", "*.odt", "*tex")

    fun load(path: Path): Archiver2 {
        log.info("Loading file $path")
        val parseData: ParseData
        try {
            parseData = detectFileType(path)
            log.info("Parsed file {} as {}", path, parseData)
            val archiver = loaders[parseData.type].firstNotNullOfOrNull { curLoader ->
                log.info("Attempting to load {} with loader {}", path, curLoader)
                curLoader.tryLoad(path, parseData)
            }
            if (archiver != null) {
                return archiver
            }
        } catch (e: Exception) {
            throw BBNotifyException("Failed when opening file $path", e)
        }

        log.info("Failed to load the file {}", path)
        when (parseData.type) {
            Types.XML -> {
                throw BBNotifyException("$path is not a valid BBX or NIMAS XML file")
            }

            Types.ZIP -> {
                throw BBNotifyException("$path is not a valid BBZ or NIMAS ZIP file")
            }

            Types.DOCX, Types.EPUB, Types.HTML, Types.MD, Types.ODT, Types.TEX -> {
                throw BBNotifyException("$path is not a valid DOCX, EPUB, HTML, TEX, MD. or ODT that can be opened by BrailleBlaster")
            }

            else -> {
                throw BBNotifyException("$path cannot be opened by BrailleBlaster")
            }
        }
    }

    /**
     * Guess the format of the document so only relevant loaders try, otherwise
     * mangled XML may cause the TextArchiveLoader to run
     *
     * @param path
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun detectFileType(path: Path): ParseData {

        // try to load with pandoc types
        val testpath = path.toString().lowercase(Locale.getDefault())
        if (testpath.endsWith(".docx")) {
            return ParseData(Types.DOCX, null)
        } else if (testpath.endsWith(".epub")) {
            return ParseData(Types.EPUB, null)
        } else if (testpath.endsWith(".html")
            || testpath.endsWith(".htm")
            || testpath.endsWith(".xhtml")
            || testpath.endsWith(".xhtm")
            || testpath.endsWith(".xht")
        ) {
            return ParseData(Types.HTML, null)
        } else if (testpath.endsWith(".odt")) {
            return ParseData(Types.ODT, null)
        } else if (testpath.endsWith(".md")) {
            return ParseData(Types.MD, null)
        } else if (testpath.endsWith(".tex")) {
            return ParseData(Types.TEX, null)
        } else {
            log.warn("File {} not of pandoc type", path)
        }
        // might be already opened zip (remember )
        /*
     * NOTE: ZipFileSystem isn't flushed/sync to disk until zipFS.close() Therefore
     * new ZIPs (eg saving bbz to a new location) are empty ZIPs on disk, even
     * though the ZipFileSystem has data.
     */
        if (ZipHandles.has(path)) {
            return ParseData(Types.ZIP, null)
        }
        Files.newInputStream(path, StandardOpenOption.READ).use { rawInput ->
            // ZIP format header signature
            // See: https://en.wikipedia.org/wiki/Zip_%28file_format%29#File_headers
            val header = ByteArray(4)
            rawInput.read(header, 0, 4)
            if (header[0].toInt() == 0x50 && header[1].toInt() == 0x4b) {
                log.trace("Detected zip")
                if (header[2].toInt() == 0x03 && header[3].toInt() == 0x04) {
                    return ParseData(Types.ZIP, null)
                } else if (header[2].toInt() == 0x05 && header[3].toInt() == 0x06) {
                    throw BBNotifyException("Cannot open empty ZIP file")
                } else if (header[2].toInt() == 0x07 && header[3].toInt() == 0x08) {
                    throw BBNotifyException("Spanned/Multi-part ZIP is not supported")
                } else {
                    log.error("Potential zip file has unknown signature {}", DatatypeConverter.printHexBinary(header))
                }
            }
        }

        // not zip, try XML
        /*
         * NOTE: Do not use XMLStreamReader due to JDK-8153781 and exceptions loading
         * some files that XOM can load
         */
        try {
            return ParseData(Types.XML, FileLoader.loadXML(path))
        } catch (e: Exception) {
            log.warn("Failed to load {} as an XML file", path, e)
        }

        // not XML or zip or pandoc type, presumably regular text
        return ParseData(Types.OTHER, null)
    }

    private val supportedExtensions: Collection<String>
        get() {
            val pandoc = System.getProperty("PANDOC")
            return supportedExtensionAndDesc.keys.filter { null != pandoc || it !in pandocExts }
        }
    private val supportedDescriptions: Collection<String>
        get() {
            val pandoc = System.getProperty("PANDOC")
            return supportedExtensionAndDesc.filterKeys { pandoc != null || it !in pandocExts }.values
        }
    val supportedExtensionsWithCombinedEntry: Collection<String>
        get() {
            val supported = supportedExtensions
            return listOf(supported.joinToString(";")) + supported
        }

    // This format seems to be the standard way to represent multiple extensions
    val supportedDescriptionsWithCombinedEntry: Collection<String>
        get() {
            val supported = supportedDescriptions
            // This format seems to be the standard way to represent multiple extensions
            return listOf("All BB Documents (${supportedExtensions.joinToString(";")})") + supported
        }

    fun getSupportedExtensions(archiver: Archiver2): Collection<String> {
        return archiver.extensionsAndDescription.keys
    }

    fun getSupportedDescriptions(archiver: Archiver2): Collection<String> {
        return archiver.extensionsAndDescription.values
    }

    interface ExtensionSupport {
        val extensionsAndDescription: Map<String, String>
    }

    interface FileLoader : ExtensionSupport {
        @Throws(Exception::class)
        fun tryLoad(file: Path, fileData: ParseData): Archiver2?

        companion object {
            fun loadXML(file: Path?): Document {
                return XMLHandler().load(file)
            }

            fun convert(file: Path, standard: String): Document = convert(file, null, standard)

            fun convert(file: Path, book: Document?, standard: String): Document {
                log.info("Converting nimas {} to bbx", file.toUri())
                val converter = BookToBBXConverter.fromConfig(standard)
                return converter.convert(book ?: loadXML(file))
            }
        }
    }

    enum class Types {
        ZIP, XML, DOCX, EPUB, HTML, MD, ODT, TEX, OTHER
    }

    class ParseData(val type: Types, val doc: Document?) {

        override fun toString(): String {
            return "ParseData{type=$type, doc=$doc}"
        }
    }

        private val log = LoggerFactory.getLogger(ArchiverFactory::class.java)
        private val inputFactory = XMLInputFactory.newInstance().apply {
            setProperty(XMLInputFactory.IS_VALIDATING, false)
            setProperty(XMLInputFactory.SUPPORT_DTD, false)
        }

        init {
            System.setProperty("PANDOC", "true")
        }
}