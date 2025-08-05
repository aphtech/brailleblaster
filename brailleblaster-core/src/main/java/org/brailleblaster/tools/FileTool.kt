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
package org.brailleblaster.tools

import org.brailleblaster.BBIni
import org.brailleblaster.archiver2.ArchiverFactory
import org.brailleblaster.frontmatter.VolumeSaveDialog
import org.brailleblaster.utils.localization.LocaleHandler
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.IBBMenu
import org.brailleblaster.perspectives.mvc.menu.SharedItem
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.perspectives.mvc.modules.misc.FileModule.Companion.fileSave
import org.brailleblaster.perspectives.mvc.modules.misc.FileModule.Companion.saveAs
import org.brailleblaster.wordprocessor.BBFileDialog
import org.eclipse.swt.SWT
import java.nio.file.Paths

private val localeHandler = LocaleHandler.getDefault()

object NewFileTool : MenuTool {
    override val topMenu = TopMenu.FILE
    override val title = localeHandler["&New"]
    override val accelerator = SWT.MOD1 or 'N'.code
    override fun onRun(bbData: BBSelectionData) {
        bbData.wpManager.addDocumentManager(null)
    }
}

object OpenFileTool : MenuTool {
    override val title = localeHandler["&Open"]
    override val topMenu = TopMenu.FILE
    override val accelerator = SWT.MOD1 or 'O'.code
    override val sharedItem = SharedItem.OPEN
    override fun onRun(bbData: BBSelectionData) {
        val wpManager = bbData.wpManager
        if (BBIni.debugging) {
            throw RuntimeException("Debug init is handled in WPManager constructor")
        }
        val dialog = BBFileDialog(
            wpManager.shell,
            SWT.OPEN,
            null,
            ArchiverFactory.supportedDescriptionsWithCombinedEntry.toTypedArray(),
            ArchiverFactory.supportedExtensionsWithCombinedEntry.toTypedArray()
        )
        val result = dialog.open()
        if (result != null) {
            wpManager.addDocumentManager(Paths.get(result))
        }
    }
}

object RecentAutoSavesTool : MenuTool {
    override val title: String = localeHandler["&RecentAutoSaves"]
    override val topMenu: TopMenu = TopMenu.FILE
    override val enabled: Boolean = false
    override fun onRun(bbData: BBSelectionData) {}
}

object SaveTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.FILE
    override val title: String = localeHandler["&Save"]
    override val accelerator: Int = SWT.MOD1 or 'S'.code
    override val sharedItem = SharedItem.SAVE
    override fun onRun(bbData: BBSelectionData) {
        fileSave(bbData.manager)
    }
}

object SaveAsTool : MenuTool {
    override val title: String = localeHandler["&SaveAs"]
    override val topMenu: TopMenu = TopMenu.FILE
    override val accelerator: Int = SWT.MOD1 or SWT.MOD2 or 'S'.code
    override fun onRun(bbData: BBSelectionData) {
        saveAs(bbData.manager)
    }
}

object SaveVolumeBrfPefTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.FILE
    override val title: String = localeHandler["&SaveVolumeBRFPEF"]
    override fun onRun(bbData: BBSelectionData) {
        VolumeSaveDialog(bbData.wpManager.shell, bbData.manager.archiver, bbData.manager.document.settingsManager, bbData.manager.doc, bbData.manager)
    }
}

object ExportMenuTool : SubMenuModule {
    override val topMenu = TopMenu.FILE
    override val text = "Export"
    override val subMenuItems: List<IBBMenu> = listOf(SaveVolumeBrfPefTool)
}
object PrintTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.FILE
    override val title: String = localeHandler["&Print"]
    override val accelerator: Int = SWT.MOD1 or 'P'.code
    override val sharedItem: SharedItem = SharedItem.PRINT
    override fun onRun(bbData: BBSelectionData) {
        bbData.manager.textPrint()
    }
}

object EmbossTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.FILE
    override val title: String = localeHandler["E&mboss"]
    override val accelerator: Int = SWT.MOD1 or 'E'.code
    override val sharedItem: SharedItem = SharedItem.EMBOSS
    override fun onRun(bbData: BBSelectionData) {
        bbData.manager.fileEmbossNow()
    }
}

object BraillePreviewTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.FILE
    override val title: String = localeHandler["&BraillePreview"]
    override val accelerator: Int = SWT.MOD3 or SWT.HOME
    override val sharedItem: SharedItem = SharedItem.BRAILLE_PREVIEW
    override fun onRun(bbData: BBSelectionData) {
        bbData.manager.printPreview()
    }
}

object CloseTool : MenuTool {
    override val title: String = localeHandler["&Close"]
    override val topMenu: TopMenu = TopMenu.FILE
    override val accelerator: Int = SWT.MOD1 or 'W'.code
    override fun onRun(bbData: BBSelectionData) {
        bbData.wpManager.closeCurrentManager()
    }
}

object ExitTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.FILE
    override val title: String = "Exit"
    override val accelerator: Int = SWT.MOD3 or SWT.F4
    override fun onRun(bbData: BBSelectionData) {
        bbData.wpManager.close()
    }
}