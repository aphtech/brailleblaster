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
package org.brailleblaster.ebraille

import org.brailleblaster.Main
import org.brailleblaster.spi.Exporter
import picocli.CommandLine
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

private const val CMD_NAME = "ebraille"
private const val DESCRIPTION = "Create an eBraille"

@CommandLine.Command(name = CMD_NAME, description = [DESCRIPTION])
class EBrailleCommand : Exporter {
    override val id = CMD_NAME
    override val description = DESCRIPTION

    @CommandLine.Parameters(paramLabel = "<input-file>", index = "0", description = ["The input file"])
    lateinit var inputFile: Path

    @CommandLine.Parameters(paramLabel = "<output-file>", index = "1", description = ["The output file to create"])
    lateinit var outputFile: Path
    override fun call(): Int {
        return Main.start(inputFile) {
            try {
                val manager = it.currentManager
                if (manager != null && !manager.isDefaultFile) {
                    manager.checkForUpdatedViews()
                    manager.waitForFormatting(true)
                    val doc = manager.doc
                    val engine = manager.document.engine
                    createEbraille(outputFile, listOf(doc), outputFile.nameWithoutExtension, engine)
                    manager.isDocumentEdited = false
                    0
                } else {
                    println("Unable to open file")
                    1
                }
            } catch (_: Throwable) {
                println("Unable to convert file")
                1
            } finally {
                if (!it.isClosed()) {
                    it.close()
                }
            }
        }
    }
}