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

import nu.xom.Element
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.utd.properties.EmphasisType
import java.nio.file.Path

/**
 * BRL files are text files with braille but no formatting. Essentially the
 * braille version of a text file of paragraphs
 */
class BRLArchiverLoader : TextArchiveLoader() {
    @Throws(Exception::class)
    override fun tryLoad(file: Path, fileData: ArchiverFactory.ParseData): Archiver2? {
        // Like BRFs, file content cannot really identify brl, only can use file extension
        return if (file.fileName.toString().endsWith(".brl", true)) {
            super.tryLoad(file, fileData)
        } else {
            null
        }
    }

    override fun createBlock(usableText: Text?): Element {
        val block = BBX.BLOCK.DEFAULT.create()
        val direct = BBX.INLINE.EMPHASIS.create(EmphasisType.NO_TRANSLATE)
        direct.appendChild(usableText)
        block.appendChild(direct)
        return block
    }

    override val extensionsAndDescription: Map<String, String> = mapOf("*.brl" to "Braille Text (*.brl)")

    companion object {
        val INSTANCE = BRLArchiverLoader()
    }
}