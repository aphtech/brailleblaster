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

import org.brailleblaster.bbx.BBX
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class BBXArchiverLoader : ArchiverFactory.FileLoader {
    override val extensionsAndDescription: Map<String, String> = mapOf(
        "*.bbx" to "BB XML (*.bbx)"
    )

    @Throws(Exception::class)
    override fun tryLoad(file: Path, fileData: ArchiverFactory.ParseData): Archiver2? {
        val rootName = fileData.doc!!.rootElement.localName
        return if (rootName == BBX.DOCUMENT_ROOT_NAME) {
            log.debug("Detected BBX")
            val arch: Archiver2 = BBZArchiver.createImportedBBZ(
                file,
                fileData.doc
            )
            var fileStr = file.fileName.toString()
            if (fileStr.lowercase(Locale.getDefault()).endsWith(".bbx")) {
                fileStr = fileStr.substring(0, fileStr.length - 4) + ".bbz"
            }
            arch.newPath = Paths.get(fileStr)
            arch
        } else {
            log.error("File " + file.toUri() + " has root element '" + rootName + "', not bbx bbdoc")
            null
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(BBXArchiverLoader::class.java)
        @JvmField
		val INSTANCE = BBXArchiverLoader()
    }
}