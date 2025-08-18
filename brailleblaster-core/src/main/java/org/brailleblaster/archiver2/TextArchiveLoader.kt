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
import nu.xom.IllegalCharacterDataException
import nu.xom.Text
import org.brailleblaster.BBIni
import org.brailleblaster.bbx.BBX
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.readLines

open class TextArchiveLoader : ArchiverFactory.FileLoader {
    @Throws(Exception::class)
    override fun tryLoad(file: Path, fileData: ArchiverFactory.ParseData): Archiver2? {
        val bbxDoc = BBX.newDocument()
        val root = BBX.SECTION.ROOT.create()
        bbxDoc.rootElement.appendChild(root)
        file.readLines(BBIni.charset).flatMap { it.split('\u000c') }.filter { it.isNotBlank() }.mapNotNull { getUsableText(it) }.fold(root) { r, v ->
            r.also {
                it.appendChild(createBlock(v))
            }
        }
        val archiver: Archiver2 = BBZArchiver.createImportedBBZ(file, bbxDoc)
        var fileStr = file.toString()
        fileStr = (if (fileStr.lowercase(Locale.getDefault()).endsWith(".txt")) fileStr.dropLast(4) else fileStr) + ".bbz"
        archiver.newPath = Paths.get(fileStr)
        return archiver
    }

    /**
     * Overridable callback
     *
     */
    protected open fun createBlock(usableText: Text?): Element {
        val block = BBX.BLOCK.DEFAULT.create()
        block.appendChild(usableText)
        return block
    }

    override val extensionsAndDescription: Map<String, String> = mapOf("*.txt" to "Text (*.txt)")

    companion object {
        private val log = LoggerFactory.getLogger(TextArchiveLoader::class.java)
        val INSTANCE = TextArchiveLoader()
        const val FORM_FEED = 0x0c.toChar()

        /**
         * Get string usable for XML 1.0
         *
         * @param line
         * @return
         */
		@JvmStatic
		fun getUsableText(line: String): Text? {
            return try {
                Text(line)
            } catch (_: IllegalCharacterDataException) {
                /*
			Try to identify weird character and remove it.
			There is little we can do with them anyway and they may break upstream code like SWT.
			TODO: This is probably not multi-byte character friendly
			 */
                var text: Text? = null
                val lineBuilder = StringBuilder()
                for (c in line.toCharArray()) {
                    try {
                        text = Text(lineBuilder.toString() + c)
                        lineBuilder.append(c)
                    } catch (ex: IllegalCharacterDataException) {
                        log.error(
                            "Character 0x" + Integer.toHexString(c.code) + " " + Character.getName(c.code) + " is not valid in XML, skipping",
                            ex
                        )
                    }
                }
                text
            }
        }
    }
}