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
package org.brailleblaster.export.brf

import org.brailleblaster.frontmatter.VolumeSaveDialog
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.mvc.BBSimpleManager
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.spi.ExportProvider
import org.brailleblaster.spi.ModuleFactory
import org.brailleblaster.tools.ExportMenuTool
import org.brailleblaster.tools.MenuTool
import org.brailleblaster.tools.SubMenuModule
import java.nio.file.Path

class BrfExportProvider : ExportProvider {
    override val id: String = "brf"
    override val displayName: String = "BRF"
    override val cliOption: String = "brf"

    override fun export(manager: Manager, outputPath: Path) {
        manager.document.settingsManager.engine.toBRF(manager.doc, outputPath.toFile())
    }
}

object BrfExportTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.FILE
    override val title: String = "Export to BRF"

    override fun onRun(bbData: BBSelectionData) {
        VolumeSaveDialog(
            bbData.wpManager.shell,
            bbData.manager.archiver,
            bbData.manager.document.settingsManager,
            bbData.manager.doc,
            bbData.manager,
            initialFormat = VolumeSaveDialog.Format.BRF,
            allowedFormats = setOf(VolumeSaveDialog.Format.BRF)
        )
    }
}

object BrfExportSubMenu : SubMenuModule {
    override val topMenu = ExportMenuTool.topMenu
    override val text = ExportMenuTool.text
    override val subMenuItems = listOf(BrfExportTool)
}

class BrfExportModuleFactory : ModuleFactory {
    override fun createModules(manager: Manager): Iterable<BBSimpleManager.SimpleListener> = listOf(BrfExportSubMenu)
}