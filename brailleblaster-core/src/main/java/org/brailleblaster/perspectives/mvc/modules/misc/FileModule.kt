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
package org.brailleblaster.perspectives.mvc.modules.misc

import org.brailleblaster.BBIni
import org.brailleblaster.archiver2.Archiver2
import org.brailleblaster.archiver2.ArchiverFactory
import org.brailleblaster.archiver2.ArchiverRecoverThread
import org.brailleblaster.archiver2.BBZArchiver.BBZSaveOptions
import org.brailleblaster.archiver2.SaveOptions
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.menu.MenuManager
import org.brailleblaster.perspectives.mvc.menu.SubMenuBuilder
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.tools.*
import org.brailleblaster.wordprocessor.BBFileDialog
import org.brailleblaster.wordprocessor.RecentDocs
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension

class FileModule : SimpleListener {
    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent) {
            MenuManager.add(NewFileTool)
            MenuManager.add(OpenFileTool)
            MenuManager.add(buildSubMenu().build())
            MenuManager.add(buildSubMenuAutoSave().build())
            //addMenuItem(RecentAutoSavesTool)
            MenuManager.add(SaveTool)
            MenuManager.add(SaveAsTool)
            MenuManager.add(ExportMenuTool)
            MenuManager.add(PrintTool)
            MenuManager.add(EmbossTool)
            MenuManager.add(BraillePreviewTool)
            // Disable split and merge as not being used.
//			MenuManager.addMenuItem(
//					MenuManager.TopMenu.FILE,
//					"Split",
//					0,
//					e -> {
//						e.manager.splitBook();
//					},
//					null);
//
//			MenuManager.addMenuItem(
//					MenuManager.TopMenu.FILE,
//					"Merge",
//					0,
//					e -> {
//						e.manager.mergeBook();
//					},
//					null);
            MenuManager.add(CloseTool)
            MenuManager.add(ExitTool)
        }
    }

    private fun buildSubMenu(): SubMenuBuilder {
        val smb = SubMenuBuilder(
            TopMenu.FILE,
            "Recent Document"
        )
        val recentDocs: List<Path> = RecentDocs.defaultRecentDocs.recentDocs
        recentDocs.forEach { curPath: Path ->
            val itemText = "${curPath.fileName}  [$curPath]"
            smb.addItem(itemText, 0) { WPManager.getInstance().addDocumentManager(curPath) }
        }
        return smb
    }

    private fun buildSubMenuAutoSave(): SubMenuBuilder {
        val smb = SubMenuBuilder(
            TopMenu.FILE,
            "Recent Auto Saves"
        )
        val recentSaves = ArchiverRecoverThread.recentSaves
        recentSaves.forEach { curPath: Path ->
            val fileName = curPath.fileName.toString()
            val itemText = "$fileName  [$curPath]"
            if (curPath.exists() && !curPath.isDirectory()) {
                smb.addItem(itemText, 0) { WPManager.getInstance().addDocumentManager(curPath) }
            }
        }
        return smb
    }

    companion object {
        private val log = LoggerFactory.getLogger(FileModule::class.java)

        fun fileSave(m: Manager): Boolean {
            log.debug("Saving file")
            m.checkForUpdatedViews()
            m.waitForFormatting(true)
            val arch = m.archiver
            val pathToRemove = arch.path.fileName.toString()
            val isNewFile = arch.path == Manager.DEFAULT_FILE
            val isImported = arch.isImported
            val isRecoveryFile = arch.path.toString().contains(BBIni.autoSavePath.toString())
            val isNotWritable = !arch.path.toFile().canWrite()
            log.debug(
                "isNewFile {} isImported {} isRecoveryFile {} isNotWritable {}",
                isNewFile,
                isImported,
                isRecoveryFile,
                isNotWritable
            )
            return if (isNewFile || isImported || isRecoveryFile || isNotWritable) {
                val success = saveAs(m)
                if (success) {
                    log.error("Setting as not imported")
                    arch.setNotImported()
                }
                log.debug("Saved file")
                success
            } else {
                save(m, arch, arch.path)
                ArchiverRecoverThread.removeFile(Path(pathToRemove))
                log.debug("Saved file")
                true
            }
        }

        /**
         * Open the Save As dialog
         * @return false if Cancel was pressed
         */
        fun saveAs(m: Manager): Boolean {
            log.debug("Saving as")
            m.waitForFormatting(true)
            val arch = m.archiver
            val pathToRemove = arch.path.fileName.toString()
            val filePath: String?
            if (!BBIni.debugging) {
                val fileName: String = (arch.newPath ?: arch.path).fileName.nameWithoutExtension
                val dialog = BBFileDialog(
                    m.wpManager.shell,
                    SWT.SAVE,
                    fileName,
                    ArchiverFactory.getSupportedDescriptions(arch).toTypedArray(),
                    ArchiverFactory.getSupportedExtensions(arch).toTypedArray()
                )
                filePath = dialog.open()
            } else {
                filePath = BBIni.debugSavePath.toString()
            }
            if (filePath == null) {
                //user pressed cancel
                log.debug("User cancelled save as")
                return false
            }
            save(m, arch, Path(filePath))
            ArchiverRecoverThread.removeFile(Path(pathToRemove))
            arch.setNotImported()
            log.debug("File saved")
            return true
        }

        private fun save(m: Manager, arch: Archiver2, filePath: Path) {
            m.checkForUpdatedViews()
            val engine = m.document.engine
            if (filePath == arch.path) {
                arch.save(
                    filePath,
                    arch.bbxDocument,
                    engine,
                    setOf<SaveOptions>(BBZSaveOptions.IncludeBRF, BBZSaveOptions.IncludePEF)
                )
            } else {
                arch.saveAs(
                    filePath,
                    arch.bbxDocument,
                    engine,
                    setOf<SaveOptions>(BBZSaveOptions.IncludeBRF, BBZSaveOptions.IncludePEF)
                )
                m.setTabTitle(filePath.fileName.toString())
                RecentDocs.defaultRecentDocs.addRecentDoc(arch.path)
            }
            m.text.hasChanged = false
            m.braille.hasChanged = false
            m.isDocumentEdited = false
        }
    }
}