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
package org.brailleblaster.debug

import org.brailleblaster.BBIni
import org.brailleblaster.utils.localization.LocaleHandler
import org.brailleblaster.perspectives.mvc.ViewManager
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.modules.views.DebugModule.DebugFatalException
import org.brailleblaster.settings.TableExceptions
import org.brailleblaster.settings.ui.AdvancedSettingsDialog
import org.brailleblaster.tools.CheckMenuTool
import org.brailleblaster.tools.DebugMenuToolModule
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.utils.UTDHelper
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.FileDialog
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

object DebugTool {
    val tools = listOf(
        XMLViewerTool,
        MapListViewerTool,
        StyleViewerTool,
        PageNumberViewerTool,
        SaveWithBrlTool,
        SaveFormattedWithBrlTool,
        SaveFormattedWithoutBrlTool,
        SetLogTool,
        DisableFocusLostListenerTool,
        TriggerExceptionTool,
        TriggerFatalExceptionTool,
        DarkModeToggleTool,
        MathTableExceptionsTool,
        TogglePandocImportTool
    )
}

private val log: Logger = LoggerFactory.getLogger(DebugTool::class.java)
private val localeHandler = LocaleHandler.getDefault()

object XMLViewerTool : DebugMenuToolModule {
    override val title = "XML Viewer"
    override val accelerator = SWT.MOD1 or SWT.MOD2 or 'X'.code
    override fun onRun(bbData: BBSelectionData) {
        XMLDebugger(bbData.wpManager.shell, bbData.manager.simpleManager)
    }
}
object MapListViewerTool : DebugMenuToolModule {
    override val title = "Map List Viewer"
    override fun onRun(bbData: BBSelectionData) {
        val manager = bbData.manager
        val mapListViewer = MapListDebugger(manager)
        mapListViewer.open()
        mapListViewer.setMapText(manager.mapList.current, manager.mapList.currentIndex)
    }
}
object StyleViewerTool : DebugMenuToolModule {
    override val title = "Style Viewer"
    override fun onRun(bbData: BBSelectionData) {
        val manager = bbData.manager
        val styleViewer = StyleDebugger(manager.wpManager.shell, SWT.NONE, manager)
        styleViewer.open()
        styleViewer.setStyleText(manager.currentTextMapElement!!.node)
    }
}
object PageNumberViewerTool : DebugMenuToolModule {
    override val title = "Page Number Viewer"
    override fun onRun(bbData: BBSelectionData) {
        PageNumberDebugger(bbData.manager)
    }
}
object SaveWithBrlTool : DebugMenuToolModule {
    override val title = "Save Document w/ <brl>"
    override fun onRun(bbData: BBSelectionData) {
        val path = FileDialog(bbData.wpManager.shell, SWT.SAVE).run {
            filterExtensions = arrayOf("*.xml")
            open()
        }
        if (path == null) {
            log.debug("Cancelled save")
        } else {
            XMLHandler().save(bbData.manager.doc, File(path))
        }
    }
}
object SaveFormattedWithBrlTool : DebugMenuToolModule {
    override val title = "Save Document Formatted w/ <brl>"
    override fun onRun(bbData: BBSelectionData) {
        val path = FileDialog(bbData.wpManager.shell, SWT.SAVE).run {
            filterExtensions = arrayOf("*.xml")
            open()
        }
        if (path == null) {
            log.debug("Cancelled save")
        } else {
            XMLHandler.Formatted().save(bbData.manager.doc, File(path))
        }
    }
}
object SaveFormattedWithoutBrlTool : DebugMenuToolModule {
    override val title = "Save Document Formatted w/o <brl>"
    override fun onRun(bbData: BBSelectionData) {
        val path = FileDialog(bbData.wpManager.shell, SWT.SAVE).run {
            filterExtensions = arrayOf("*.xml")
            open()
        }
        if (path == null) {
            log.debug("Cancelled save")
        } else {
            val docCopy = bbData.manager.doc.copy().also { UTDHelper.stripUTDRecursive(it) }
            XMLHandler.Formatted().save(docCopy, File(path))
        }
    }
}
object SetLogTool : DebugMenuToolModule {
    override val title = localeHandler["setLog"]
    override fun onRun(bbData: BBSelectionData) {
        AdvancedSettingsDialog()
    }
}
object TriggerExceptionTool : DebugMenuToolModule {
    override val title = "Trigger Exception"
    override fun onRun(bbData: BBSelectionData) {
        throw RuntimeException("It broke again")
    }
}
object TriggerFatalExceptionTool : DebugMenuToolModule {
    override val title = "Trigger Fatal Exception"
    override fun onRun(bbData: BBSelectionData) {
        throw DebugFatalException("It SUPER broke again")
    }
}
object DarkModeToggleTool : DebugMenuToolModule {
    override val title = "Dark Mode Toggle (next restart)"
    override fun onRun(bbData: BBSelectionData) {
        BBIni.propertyFileManager.saveAsBooleanCompute(
            ViewManager.SETTING_DARK_THEME,
            true
        ) { !it }
    }
}
object MathTableExceptionsTool : DebugMenuToolModule {
    override val title = "Math Table Exceptions"
    override fun onRun(bbData: BBSelectionData) {
        TableExceptions.MATH_EXCEPTION_TABLES = true
    }
}
object DisableFocusLostListenerTool : CheckMenuTool, DebugMenuToolModule {
    override val title = "Disable FocusLost Listener"
    override val active = false
    override fun onRun(bbData: BBSelectionData) {
        bbData.manager.text.setFocusListenerLock(bbData.menuItem!!.selection)
    }
}
object TogglePandocImportTool : CheckMenuTool, DebugMenuToolModule {
    override val title = "Toggle Pandoc Import"
            override val accelerator = SWT.MOD1 or SWT.MOD3 or 'P'.code
    override val active = true
    override fun onRun(bbData: BBSelectionData) {
        val enabled = bbData.menuItem!!.selection
        if (enabled) {
            System.setProperty("PANDOC", "true")
        } else {
            System.clearProperty("PANDOC")
        }
    }
}