/*
 * Copyright (C) 2025 Michael Whapples
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
package org.brailleblaster.perspectives.braille

import org.brailleblaster.perspectives.braille.ui.CellTabTool
import org.brailleblaster.perspectives.braille.ui.CorrectTranslationDialog
import org.brailleblaster.perspectives.braille.ui.contractionRelaxer.ContractionRelaxer
import org.brailleblaster.perspectives.braille.views.style.BreadcrumbsToolbar
import org.brailleblaster.perspectives.braille.views.wp.PageNumberDialog
import org.brailleblaster.perspectives.mvc.BBSimpleManager
import org.brailleblaster.perspectives.mvc.modules.misc.*
import org.brailleblaster.perspectives.mvc.modules.views.*
import org.brailleblaster.search.SearchDialog
import org.brailleblaster.settings.ResetBB
import org.brailleblaster.settings.ui.PrivacyTool
import org.brailleblaster.spi.ModuleFactory
import org.brailleblaster.tools.BlankPrintPageIndicatorTool
import org.brailleblaster.tools.InsertBookmarkTool
import org.brailleblaster.tools.InsertLinkTool
import org.brailleblaster.tools.LineBreakTool
import org.brailleblaster.tools.PageBreakTool
import org.brailleblaster.userHelp.AboutTool
import org.brailleblaster.userHelp.GoToWebsiteTool
import org.brailleblaster.userHelp.UserGuideTool
import org.eclipse.swt.SWT

class CoreModulesFactory : ModuleFactory {
    override val priority: Int
        get() = Int.MAX_VALUE
    override fun createModules(manager: Manager): Iterable<BBSimpleManager.SimpleListener> = buildList {
        add(TextViewModule(manager))
        add(DebugModule(manager))
        add(ToggleViewsModule(manager))
        add(ToolBarModule(manager.wpManager))
        add(BrailleViewModule(manager))
        add(FileModule())
        add(ClipboardModule(manager.simpleManager))
        add(UndoRedoModule(manager))
        add(RefreshModule(manager))
        add(FontSizeModule)
        add(PageNumberDialog(manager.wpManager.shell))
        add(SearchDialog(manager.wpManager.shell, 0))
        addAll(NavigateModule.tools)
        add(CorrectTranslationDialog(manager.wpManager.shell, SWT.APPLICATION_MODAL))
        add(SixKeyModeModule(manager))
        addAll(SettingsModule.tools)
        // add(ProseBuilder()) // Line Number tools
        add(NoteSeparationLineModule)
        add(ImagePlaceholderTool)
        addAll(TableEditorTools.tools)
        add(EmphasisModule)
        add(GoToWebsiteTool)
        add(UserGuideTool)
        add(AboutTool)
        add(PrivacyTool)
        add(LogTool)
        add(ResetBB)
        add(StylesMenuModule(manager))
        add(MenuModule())
        add(RunningHeadTool())
        add(ConvertPrintPageNumber)
        add(ChangeTranslationModule())
        add(ContextMenuModule(manager))
        // add(UnwrapElementTool)
        add(TableSelectionModule(manager))
        // add(SplitElementModule())
        add(PageBreakTool)
        add(LineBreakTool)
        add(BreadcrumbsToolbar(manager))
        add(CellTabTool)
        add(BlankPrintPageIndicatorTool)
        addAll(ElementNavigationModule.tools)
        add(PageDownUpModule(manager))
        add(InsertUnicode(manager.wpManager.shell))
        add(ContractionRelaxer(manager.wpManager.shell))
        add(InsertLinkTool(manager))
        add(InsertBookmarkTool(manager))
    }
}