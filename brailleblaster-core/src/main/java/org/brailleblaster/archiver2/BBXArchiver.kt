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
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BookToBBXConverter
import org.brailleblaster.utd.UTDTranslationEngine
import org.brailleblaster.utd.exceptions.NodeException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Path

/**
 * BrailleBlaster XML (BBX) file
 */
@Deprecated("")
class BBXArchiver(private var origPath: Path, override val bbxDocument: Document, loader: Class<*>?) : BaseArchiver(
    null, null
), Archiver2 {
    init {
        // imported = loader != Loader.class;
        //TODO: getformatVersion throws an exception if it can't be found
        if (BBX.getFormatVersion(bbxDocument) < 1) {
            throw NodeException("Failed to find format version", bbxDocument)
        }
        BookToBBXConverter.upgradeFormat(bbxDocument)
    }

    override fun resolveSibling(descendant: Path): Path {
        return origPath.resolveSibling(descendant)
    }

    override val path: Path
        get() = origPath

    override fun save(destPath: Path, doc: Document, engine: UTDTranslationEngine, options: Set<SaveOptions>) {
        try {
            BBZArchiver.saveBBX(destPath, doc)
        } catch (e: Exception) {
            throw RuntimeException("Unable to save to existing $destPath", e)
        }
    }

    override fun saveAs(destPath: Path, doc: Document, engine: UTDTranslationEngine, options: Set<SaveOptions>) {
        save(destPath, doc, engine, options)
        origPath = destPath
    }

    override val extensionsAndDescription: Map<String, String>
        get() = BBXArchiverLoader.INSTANCE.extensionsAndDescription

    @Throws(IOException::class)
    override fun close() {
        //nothing to close
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(BBXArchiver::class.java)
    }
}