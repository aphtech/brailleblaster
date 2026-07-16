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

import nu.xom.Document
import org.brailleblaster.ebraille.bbx2html.BBX2HTML
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.perspectives.mvc.modules.views.DebugModule
import org.brailleblaster.tools.ExportMenuTool
import org.brailleblaster.tools.MenuTool
import org.brailleblaster.tools.SubMenuModule
import org.brailleblaster.utd.ITranslationEngine
import org.brailleblaster.wordprocessor.BBFileDialog
import org.eclipse.swt.SWT
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension

internal fun createEbraille(outputPath: Path, docs: List<Document>, title: String, engine: ITranslationEngine) {
    val html = docs.map { BBX2HTML.convertBbxToHtml(it) }
    EBraillePackager.createEbraillePackage(outputPath, html, title = title, engine)
}
object EBrailleExportTool : MenuTool {
    override val topMenu = TopMenu.FILE
    override val title = "Export to eBraille"
    override fun onRun(bbData: BBSelectionData) {
        BBFileDialog(bbData.wpManager.shell, SWT.SAVE,  suggestedFileName = null, filterNames = arrayOf("eBraille files"), filterExtensions = arrayOf("*.ebrl")).open()?.let { f ->
            createEbraille(Path(f), listOf(bbData.manager.doc), Path(f).nameWithoutExtension, bbData.manager.document.engine)
        }
    }
}

object ExportSubMenu : SubMenuModule {
    override val topMenu = ExportMenuTool.topMenu
    override val text = ExportMenuTool.text
    override val visible: Boolean
        get() = DebugModule.enabled
    override val subMenuItems = listOf(EBrailleExportTool)
}