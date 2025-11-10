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
package org.brailleblaster.frontmatter

import nu.xom.Document
import nu.xom.Element
import org.brailleblaster.BBIni.debugSavePath
import org.brailleblaster.BBIni.debugging
import org.brailleblaster.BBIni.propertyFileManager
import org.brailleblaster.archiver2.Archiver2
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBX.VolumeType
import org.brailleblaster.frontmatter.VolumeUtils.VolumeData
import org.brailleblaster.frontmatter.VolumeUtils.getVolumeElements
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.settings.UTDManager
import org.brailleblaster.utd.BRFWriter
import org.brailleblaster.utd.BRFWriter.OutputCharStream
import org.brailleblaster.utd.BRFWriter.PageListener
import org.brailleblaster.utd.UTDTranslationEngine
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.ALL_VOLUMES
import org.brailleblaster.utd.utils.convertBBX2PEF
import org.brailleblaster.util.LINE_BREAK
import org.brailleblaster.util.Notify.showMessage
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.utils.swt.EasySWT.addSwtBotKey
import org.brailleblaster.utils.swt.EasySWT.buildGridData
import org.brailleblaster.utils.swt.EasySWT.makeComposite
import org.brailleblaster.wordprocessor.BBFileDialog
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.nameWithoutExtension

/**
 * Save volume to brf dialog
 */
class VolumeSaveDialog(
    parent: Shell,
    private val arch: Archiver2,
    private val utdManager: UTDManager,
    private val doc: Document,
    private val m: Manager
) {
    private var saveExec = false // To know if the user try to complete the save process
    private val shell: Shell
    private val volumesTable: Table?
    private var selectedFormat = Format.valueOf(
        propertyFileManager.getProperty(SETTINGS_FORMAT, Format.BRF.toString())
    )

    init {
        val volumes = getVolumeElements(doc)

        shell = EasySWT.makeDialog(parent)
        shell.text = localeHandler["&SaveVolumeBRFPEF"]
        shell.setLayout(GridLayout(2, false))

        if (volumes.isEmpty()) {
            clickSaveSingle()
            volumesTable = null
        } else {
            //Table must be wrapped in a composite for some reason
            val tableWrapper = makeComposite(shell, 1)
            buildGridData()
                .setGrabSpace(horizontally = true, vertically = true)
                .setAlign(GridData.FILL, GridData.FILL)
                .verticalSpan(3)
                .applyTo(tableWrapper)

            volumesTable = Table(tableWrapper, SWT.VIRTUAL or SWT.BORDER or SWT.FULL_SELECTION or SWT.MULTI)
            buildGridData()
                .setGrabSpace(horizontally = true, vertically = true)
                .setAlign(GridData.FILL, GridData.FILL)
                .applyTo(volumesTable)

            val name = TableColumn(volumesTable, SWT.NONE)
            name.setText("Volume")
            name.width = 100

            val saveSingle = Button(shell, SWT.NONE)
            addSwtBotKey(saveSingle, SWTBOT_SAVE_SINGLE)
            saveSingle.setText("Save All to Single File")
            EasySWT.setGridData(saveSingle)

            val saveFolder = Button(shell, SWT.NONE)
            addSwtBotKey(saveFolder, SWTBOT_SAVE_FOLDER)
            saveFolder.setText("Save Selected to Folder")
            EasySWT.setGridData(saveFolder)

            val saveFolderAll = Button(shell, SWT.NONE)
            addSwtBotKey(saveFolderAll, SWTBOT_SAVE_FOLDER_ALL)
            saveFolderAll.setText("Save All to Folder")
            EasySWT.setGridData(saveFolderAll)

            // ----------------- Listeners --------------------
            EasySWT.addSelectionListener(saveSingle) { _: SelectionEvent? -> clickSaveSingle() }
            EasySWT.addSelectionListener(saveFolder) { _: SelectionEvent? -> clickSaveFolder(false) }
            EasySWT.addSelectionListener(saveFolderAll) { it: SelectionEvent -> clickSaveFolder(true) }

            // -------------------- Data ---------------------
            for (curVolume in VolumeUtils.getVolumeNames(volumes)) {
                val entry = TableItem(volumesTable, SWT.NONE)
                entry.setText(arrayOf<String>(curVolume.nameLong))
                entry.setData(KEY_VOLUME_DATA, curVolume)
            }

            EasySWT.setLargeDialogSize(shell)
            shell.open()
        }
    }

    private fun clickSaveSingle() {
        log.trace("saving volumneless brf")

        val save = generateSaveDialog()
        if (save == null) {
            log.debug("canceled save")
            return
        }
        val format = save.first
        val path = save.second
        try {
            when (format) {
                Format.BRF -> {
                    utdManager.engine.toBRF(doc, File(path))
                }
                Format.PEF -> {
                    val os: OutputStream = FileOutputStream(path)
                    val defaultIdentifier = identifierFromFileName(File(path))
                    convertBBX2PEF(doc, defaultIdentifier, utdManager.engine, ALL_VOLUMES, os)
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Unable to write BRF to $path", e)
        }
    }

    private fun clickSaveFolder(all: Boolean) {
        log.trace("saving BRF to folder")
        val selectedItems = if (all)
            volumesTable!!.getItems()
        else
            volumesTable!!.selection
        if (selectedItems.size == 0) {
            showMessage("Must select volume")
            return
        }

        val path: String?
        if (selectedItems.size == 1) {
            val save = generateSaveDialog()
            if (save == null) {
                log.debug("canceled save")
                return
            }
            val format = save.first
            path = save.second

            val volumeIndex = volumesTable.getItems().indexOf(selectedItems[0])
            try {
                when (format) {
                    Format.BRF -> {
                        val volumeBRF: String = volumeToBRF(utdManager.engine, doc, volumeIndex, true)
                        File(path).writeText(volumeBRF, Charsets.UTF_8)
                        log.info("Wrote {} characters to {}", volumeBRF.length, path)
                    }
                    Format.PEF -> {
                        val defaultIdentifier = identifierFromFileName(File(path))
                        convertBBX2PEF(
                            doc,
                            defaultIdentifier,
                            utdManager.engine,
                            { x: Int -> x == volumeIndex },
                            FileOutputStream(path)
                        )
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException("Unable to save ordinal volume $volumeIndex", e)
            }
            shell.close()
        } else {
            val debugSavePath = debugSavePath
            if (debugging && debugSavePath != null) {
                path = debugSavePath.parent.toAbsolutePath().toString()
            } else {
                val dialog = DirectoryDialog(shell, SWT.SAVE)
                path = dialog.open()
            }

            if (path == null) {
                log.debug("canceled save")
                return
            }

            //Always prompt for file type to save
            val formatSelector = Shell(shell, SWT.APPLICATION_MODAL or SWT.DIALOG_TRIM)
            formatSelector.setLayout(GridLayout(1, false))
            formatSelector.text = "Format selector"

            val formatBrf = Button(formatSelector, SWT.RADIO)
            formatBrf.setText("Braille Ready File (BRF)")

            val formatPef = Button(formatSelector, SWT.RADIO)
            formatPef.setText("Portable Embosser Format (PEF)")

            val submit = Button(formatSelector, SWT.NONE)
            submit.setText("Submit")

            formatSelector.open()
            formatSelector.pack()

            // listeners
            submit.addListener(SWT.Selection) {
                selectedFormat = if (formatBrf.selection) {
                    Format.BRF
                } else if (formatPef.selection) {
                    Format.PEF
                } else {
                    throw UnsupportedOperationException("missing selection")
                }
                doSaveFolder(selectedItems, path)
                formatSelector.close()
                if (saveExec)  // if the user press No, shell will remain open
                    shell.close()
            }

            // data
            when (selectedFormat) {
                Format.BRF -> {
                    formatBrf.selection = true
                }
                Format.PEF -> {
                    formatPef.selection = true
                }
            }
        }
    }

    private fun doSaveFolder(selectedItems: Array<TableItem>, path: String) {
        val checkvalue = checkDoSaveFolder(selectedItems, path)
        if (checkvalue == 0) {  //the user press Yes
            saveExec = true
            for (selectedItem in selectedItems) {
                val volumeData = selectedItem.getData(KEY_VOLUME_DATA) as VolumeData
                val volumeIndex = volumesTable!!.getItems().indexOf(selectedItem)
                try {
                    val outputPath: File = getBRFPath(
                        Paths.get(path),
                        arch.path,
                        volumeData.type,
                        volumeData.volumeTypeIndex,
                        selectedFormat
                    )

                    when (selectedFormat) {
                        Format.BRF -> {
                            val volumeBRF: String = volumeToBRF(utdManager.engine, doc, volumeIndex, true)
                            outputPath.writeText(
                                volumeBRF,
                                Charsets.UTF_8
                            )
                            log.info("Wrote {} characters to {}", volumeBRF.length, outputPath)
                        }
                        Format.PEF -> {
                            val defaultIdentifier = identifierFromFileName(outputPath)
                            convertBBX2PEF(
                                doc,
                                defaultIdentifier,
                                utdManager.engine,
                                { x: Int -> x == volumeIndex },
                                FileOutputStream(outputPath)
                            )
                            log.info("Wrote volume {} to PEF", volumeIndex)
                        }
                    }
                } catch (e: Exception) {
                    throw RuntimeException("Unable to save ordinal volume $volumeIndex", e)
                }
            }
        }
    }

    private fun identifierFromFileName(path: File): String = path.name.substringBeforeLast(".")

    private fun checkDoSaveFolder(selectedItems: Array<TableItem>, path: String): Int {
        var existsFile = false
        for (selectedItem in selectedItems) {
            val volumeData = selectedItem.getData(KEY_VOLUME_DATA) as VolumeData

            val outputPath: File = getBRFPath(
                Paths.get(path),
                arch.path,
                volumeData.type,
                volumeData.volumeTypeIndex,
                selectedFormat
            )
            if (Files.exists(outputPath.toPath())) {
                existsFile = true
                break
            }
        }
        if (existsFile) {
            val dialog = MessageDialog(
                shell,
                "Confirm Save",
                null,
                "This operation will overwrite one or more files in this directory. Are you sure you want to continue?",
                MessageDialog.WARNING,
                arrayOf(
                    "Yes",
                    "No"
                ),
                0
            )
            return dialog.open() //return 0 Yes, 1 No, -1 close dialog
        } else {
            return 0
        }
    }

    private fun generateSaveDialog(): Pair<Format, String>? {
        val debugSavePath = debugSavePath
        if (debugging && debugSavePath != null) {
            // TODO: PEF testing
            return Format.BRF to debugSavePath.toString()
        } else {
            val fileName = m.archiver.path.fileName.nameWithoutExtension

            val dialog = BBFileDialog(
                m.wpManager.shell,
                SWT.SAVE,
                fileName,
                Format.fileDialogNames(),
                Format.fileDialogExtensions(),
                Format.entries.indexOf(selectedFormat)
            )

            return dialog.open()?.let { filename ->
                val format: Format = Format.matchExtension(filename, dialog.widget.getFilterIndex())
                format to filename
            }
        }
    }

    enum class State {
        NOT_RUNNING,
        START_TRIGGER,
        STARTED,
        END_TRIGGER,
        FINISHED,
    }

    enum class Format(private val extension: String, private val displayName: String) {
        BRF("brf", "Braille Ready File"),
        PEF("pef", "Portable Embosser Format");

        companion object {
            fun fileDialogExtensions(): Array<String> = entries.map { value -> "*.${value.extension}" }.toTypedArray()

            fun fileDialogNames(): Array<String> =
                entries.map { value -> "${value.displayName} (*.${value.extension})" }.toTypedArray()

            fun matchExtension(filename: String, fallbackEnumIndex: Int): Format {
                val result = entries[fallbackEnumIndex]
                val periodIndex = filename.lastIndexOf('.')
                if (periodIndex == -1) {
                    log.warn("missing extension {}", filename)
                    log.warn("falling back to value {}", result)
                } else {
                    val fileExtension = filename.substring(periodIndex + 1)
                    for (value in entries) {
                        if (value.extension == fileExtension) {
                            return value
                        }
                    }
                    log.warn("invalid extension on {}", filename)
                }
                return result
            }
        }
    }

    companion object {
        private val localeHandler = getDefault()

        private val log: Logger = LoggerFactory.getLogger(VolumeSaveDialog::class.java)
        const val SETTINGS_FORMAT: String = "volumeSaveDialog.format"
        const val SWTBOT_SAVE_SINGLE: String = "volumeSaveDialog.saveAll"
        const val SWTBOT_SAVE_FOLDER: String = "volumeSaveDialog.saveFolder"
        const val SWTBOT_SAVE_FOLDER_ALL: String = "volumeSaveDialog.saveFolderAll"
        private const val KEY_VOLUME_DATA = "volumeSaveDialog.volumeData"
        @JvmStatic
        fun getBRFPath(
            documentPath: Path,
            volumeType: VolumeType,
            volumeTypeIndex: Int,
            selectedFormat: Format?
        ): File {
            return getBRFPath(documentPath.parent, documentPath, volumeType, volumeTypeIndex, selectedFormat)
        }

        fun getBRFPath(
            parentFile: Path,
            documentPath: Path,
            volumeType: VolumeType,
            volumeTypeIndex: Int,
            selectedFormat: Format?
        ): File {
            //Strip off extension
            var name = documentPath.fileName.toString().substringBeforeLast('.')

            var format = ".brf"
            if (selectedFormat == Format.PEF) {
                format = ".pef"
            }

            name = (name
                    + "_"
                    + volumeType.volumeNameShort.lowercase(Locale.getDefault())
                    + volumeTypeIndex
                    + format)
            return parentFile.resolve(name).toFile()
        }

        @Throws(IOException::class)
        fun volumeToBRF(engine: UTDTranslationEngine, doc: Document, volume: Int, convertLineEndings: Boolean): String {
            log.trace("Saving volume {}", volume)
            val volumeEndBrls = FastXPath.descendantFindList(
                doc
            ) { results, curNode ->
                if (BBX.BLOCK.VOLUME_END.isA(curNode)) {
                    val blockBrls = FastXPath.descendantFindList<Element>(
                        curNode
                    ) { _, curEndBlockChild ->
                        UTDElements.BRL.isA(
                            curEndBlockChild
                        )
                    }
                    //manually add the results we want instead of this method doing it for us
                    results.add(blockBrls.last())
                }
                false
            }

            val brfBuilder = StringBuilder()
            var finalBrfMut: String? = null

            val stream =
                if (convertLineEndings) BRFWriter.lineEndingRewriter { c: Char -> brfBuilder.append(c) } else OutputCharStream { c: Char ->
                    brfBuilder.append(c)
                }

            engine.toBRF(doc, stream, 0, object : PageListener {
                var saveState: State = State.NOT_RUNNING

                init {
                    if (volume == 0) {
                        saveState = State.STARTED
                    }
                }

                override fun onBeforeBrl(grid: BRFWriter?, brl: Element) {
                    if (saveState == State.FINISHED) {
                        return
                    }

                    //Note: As this is called before brls are processed triggers are nessesary
                    if (volume == 0) {
                        //Save everything from the start of toBRF to the first volume end brl
                        if (finalBrfMut == null && brl === volumeEndBrls.first()) {
                            //Haven't processed last brl yet
                            saveState = State.END_TRIGGER
                        }
                    } else if (saveState == State.NOT_RUNNING && brl === volumeEndBrls[volume - 1]) {
                        saveState = State.START_TRIGGER
                    } else if (saveState == State.STARTED && volume != volumeEndBrls.size && brl === volumeEndBrls[volume]
                    ) {
                        saveState = State.END_TRIGGER
                    }

                    log.debug("State {} element {}", saveState, brl.toXML())
                }

                override fun onAfterFlush(brfWriter: BRFWriter?) {
                    if (saveState == State.FINISHED) {
                        return
                    }

                    if (saveState == State.START_TRIGGER) {
                        log.debug("Start triggered")
                        brfBuilder.setLength(0)
                        saveState = State.STARTED
                    } else if (saveState == State.END_TRIGGER) {
                        finalBrfMut = brfBuilder.toString()
                        saveState = State.FINISHED
                    }
                }
            }, true)

            //The last volume does not end with a END OF VOLUME tag
            if (volume == volumeEndBrls.size) {
                check(finalBrfMut == null) { "Wut $finalBrfMut" }
                finalBrfMut = brfBuilder.toString()
            }

            val finalBrf = finalBrfMut!!
            check(!finalBrf.isBlank()) { "No finalBrf?! $LINE_BREAK$finalBrf$LINE_BREAK=------=" }

            //Remove lingering empty page from resetting the page to restart the BRF output
            val linesPerPage = engine.brailleSettings.cellType.getLinesForHeight(
                BigDecimal.valueOf(engine.pageSettings.drawableHeight)
            )
            return if (finalBrf.startsWith(BRFWriter.NEWLINE.toString().repeat(linesPerPage) + BRFWriter.PAGE_SEPARATOR)) {
                finalBrf.substring(linesPerPage + 1)
            } else {
                finalBrf
            }
        }
    }
}
