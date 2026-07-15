/*
 * Copyright (C) 2026 American Printing House for the Blind
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
import picocli.CommandLine
import java.io.File
import java.nio.file.Path

private const val DESCRIPTION = "create a BRF"

@CommandLine.Command(name = "brf", description = [DESCRIPTION])
class BrfCommand : Exporter {
    override val id: String = "brf"
    override val description: String = DESCRIPTION
    @CommandLine.Parameters(paramLabel = "<input-file>", description = ["Input file to convert"])
    lateinit var inputFile: Path
    @CommandLine.Parameters(paramLabel = "<output-file>", description = ["File name of the output BRF"])
    lateinit var outputFile: File
    override fun call(): Int {
        return Main.start(inputFile) {
            val manager = it.currentManager
            if (manager != null && !manager.isDefaultFile) {
                manager.checkForUpdatedViews()
                manager.waitForFormatting(true)
                val doc = manager.doc
                val engine = manager.document.engine
                engine.toBRF(doc, outputFile)
                0
            } else {
                println("Unable to open input file")
                1
            }
        }
    }
}