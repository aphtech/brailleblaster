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

import com.google.common.io.Files
import com.google.common.io.LineProcessor
import nu.xom.Element
import nu.xom.IllegalCharacterDataException
import nu.xom.Text
import org.brailleblaster.BBIni
import org.brailleblaster.bbx.BBX
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

open class TextArchiveLoader : ArchiverFactory.FileLoader {
    @Throws(Exception::class)
    override fun tryLoad(file: Path, fileData: ArchiverFactory.ParseData): Archiver2? {
        val bbxDoc = BBX.newDocument()
        val root = BBX.SECTION.ROOT.create()
        bbxDoc.rootElement.appendChild(root)
        Files.asCharSource(file.toFile(), BBIni.charset).readLines(object : LineProcessor<Void?> {
            override fun processLine(line: String): Boolean {
                if (line.contains(FORM_FEED)) {
                    if (line.trim { it <= ' ' }.length == 1) {
                        //skip, essentially is an empty line
                    } else {
                        for (curPart in line.split(FORM_FEED)) {
                            processLine(curPart)
                        }
                    }
                } else if (line.isNotEmpty()) {
                    val usableText = getUsableText(line.trim())
                    if (usableText != null) {
                        val block = createBlock(usableText)
                        root.appendChild(block)
                    }
                }
                return true
            }

            override fun getResult(): Void? {
                return null
            }
        })
        val archiver: Archiver2 = BBZArchiver.createImportedBBZ(file, bbxDoc)
        var fileStr = file.toString()
        fileStr = (if (fileStr.lowercase(Locale.getDefault()).endsWith(".txt")) fileStr.substring(
            0,
            fileStr.length - 4
        ) else fileStr) + ".bbz"
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