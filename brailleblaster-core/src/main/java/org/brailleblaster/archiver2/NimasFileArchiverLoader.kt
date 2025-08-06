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

import org.brailleblaster.archiver2.ArchiverFactory.FileLoader.Companion.convert
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

object NimasFileArchiverLoader : ArchiverFactory.FileLoader {
    override val extensionsAndDescription: Map<String, String> = mapOf(
        "*.xml" to "Nimas XML (*.xml)"
    )

    @Throws(Exception::class)
    override fun tryLoad(file: Path, fileData: ArchiverFactory.ParseData): Archiver2? {
        val rootName = fileData.doc!!.rootElement.localName
        return if (rootName == "dtbook") {
            val archiver: Archiver2 = BBZArchiver.createImportedBBZ(
                file,
                convert(file, "nimas")
            )
            // Set the recommended save as new file name
            var fileStr = file.toString()
            fileStr = (if (fileStr.lowercase(Locale.getDefault()).endsWith(".xml")) fileStr.dropLast(4) else fileStr) + ".bbz"
            archiver.newPath = Paths.get(fileStr)
            archiver
        } else {
            log.error("File " + file.toUri() + " has root element '" + rootName + "', not nimas dtbook")
            null
        }
    }

    private val log = LoggerFactory.getLogger(NimasFileArchiverLoader::class.java)
}