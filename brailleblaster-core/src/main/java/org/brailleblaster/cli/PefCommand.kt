/*
 * Copyright (C) 2026 American Printing House for the Blind
 * Copyright (C) 2026 Michael Whapples
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
package org.brailleblaster.cli

import org.brailleblaster.Main
import org.brailleblaster.spi.Exporter
import org.brailleblaster.utd.utils.ALL_VOLUMES
import org.brailleblaster.utd.utils.convertBBX2PEF
import picocli.CommandLine
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.outputStream

private const val CMD_NAME = "pef"
private const val DESCRIPTION = "Create a PEF"

@CommandLine.Command(name = CMD_NAME, description = [DESCRIPTION])
class PefCommand : BaseBrailleExportCommand(), Exporter {
    override val id = CMD_NAME
    override val description = DESCRIPTION

    @CommandLine.Parameters(paramLabel = "<input-file>", index = "0", description = ["The input file to convert"])
    lateinit var inputFile: Path

    @CommandLine.Parameters(paramLabel = "<output-file>", index = "1", description = ["The output file"])
    lateinit var outputFile: Path
    override fun call(): Int {
        return Main.start(inputFile, brailleProfile = brailleProfile) {
            try {
                val manager = it.currentManager
                if (manager != null && !manager.isDefaultFile) {
                    manager.checkForUpdatedViews()
                    manager.waitForFormatting(true)
                    val doc = manager.doc
                    val engine = manager.document.engine
                    manager.isDocumentEdited = false
                    outputFile.outputStream().use { outStream ->
                        convertBBX2PEF(doc, outputFile.nameWithoutExtension, engine, ALL_VOLUMES, outStream)
                        0
                    }
                } else {
                    println("Unable to open file")
                    1
                }
            } catch (_: Throwable) {
                println("Unable to convert to PEF")
                1
            } finally {
                if (!it.isClosed()) {
                    it.close()
                }
            }
        }
    }
}