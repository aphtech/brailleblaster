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
package org.brailleblaster.ebraille

import org.brailleblaster.ebraille.bbx2html.BBX2HTML
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.spi.ExportProvider
import java.nio.file.Path
import kotlin.io.path.name

class EBrailleExportProvider : ExportProvider {
    override val id: String = "ebrl"
    override val displayName: String = "eBraille"
    override val cliOption: String = "ebrl"

    override fun export(manager: Manager, outputPath: Path) {
        val html = BBX2HTML.convertBbxToHtml(manager.doc)
        EBraillePackager.createEbraillePackage(
            outputPath,
            listOf(html),
            outputPath.name,
            manager.simpleManager.utdManager.engine
        )
    }
}