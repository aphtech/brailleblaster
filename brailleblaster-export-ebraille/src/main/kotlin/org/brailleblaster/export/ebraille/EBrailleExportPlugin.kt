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
package org.brailleblaster.export.ebraille

import org.brailleblaster.ebraille.EBraillePackager
import org.brailleblaster.ebraille.bbx2html.BBX2HTML
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.mvc.BBSimpleManager
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.spi.ExportProvider
import org.brailleblaster.spi.ModuleFactory
import org.brailleblaster.tools.ExportMenuTool
import org.brailleblaster.tools.MenuTool
import org.brailleblaster.tools.SubMenuModule
import org.brailleblaster.wordprocessor.BBFileDialog
import org.eclipse.swt.SWT
import java.nio.file.Path
import kotlin.io.path.Path
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

object EBrailleExportTool : MenuTool {
    override val topMenu = TopMenu.FILE
    override val title = "Export to eBraille"

    override fun onRun(bbData: BBSelectionData) {
        BBFileDialog(
            bbData.wpManager.shell,
            SWT.SAVE,
            suggestedFileName = null,
            filterNames = arrayOf("eBraille files"),
            filterExtensions = arrayOf("*.ebrl")
        ).open()?.let { f ->
            val html = BBX2HTML.convertBbxToHtml(bbData.manager.doc)
            EBraillePackager.createEbraillePackage(
                Path(f),
                listOf(html),
                title = Path(f).name,
                bbData.manager.simpleManager.utdManager.engine
            )
        }
    }
}

object EBrailleExportSubMenu : SubMenuModule {
    override val topMenu = ExportMenuTool.topMenu
    override val text = ExportMenuTool.text
    override val subMenuItems = listOf(EBrailleExportTool)
}

class EBrailleExportModuleFactory : ModuleFactory {
    override fun createModules(manager: Manager): Iterable<BBSimpleManager.SimpleListener> = listOf(EBrailleExportSubMenu)
}