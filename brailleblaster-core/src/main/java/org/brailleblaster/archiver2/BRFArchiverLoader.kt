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

import java.nio.file.Path

class BRFArchiverLoader : ArchiverFactory.FileLoader {
    override fun tryLoad(file: Path, fileData: ArchiverFactory.ParseData): Archiver2? {
        if (!isBRF(file)) {
            return null
        }
        throw UnsupportedOperationException("should of been handled in WPManager.addDocumentManager")
    }

    override val extensionsAndDescription: Map<String, String> = mapOf("*.brf" to "Braille Ready File (*.brf)")

    companion object {
        val INSTANCE = BRFArchiverLoader()
        @JvmStatic
		fun isBRF(file: Path?): Boolean {
            /*
		Due to being a text format, it's not really possible to reliably determine if a file content is brf
		Instead, use the only identifier available: file extension
		*/
            return file != null && file.fileName.toString().endsWith(".brf", true)
        }
    }
}