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
package org.brailleblaster.embossers

import kotlinx.serialization.json.Json
import org.brailleblaster.BBIni
import org.brailleblaster.document.BBDocument
import org.brailleblaster.libembosser.embossing.attribute.*
import org.brailleblaster.libembosser.embossing.attribute.PaperSize
import org.brailleblaster.libembosser.spi.*
import org.brailleblaster.libembosser.spi.Notification.NotificationType
import org.brailleblaster.utd.ITranslationEngine
import org.brailleblaster.utd.UTDTranslationEngine
import org.brailleblaster.utd.utils.ALL_VOLUMES
import org.brailleblaster.utd.utils.convertBBX2PEF
import org.brailleblaster.util.Notify
import org.brailleblaster.utils.LengthUtils.toLengthBigDecimal
import org.brailleblaster.utils.PageFilterInputStream
import org.brailleblaster.utils.localization.LocaleHandler
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.jface.window.Window
import org.eclipse.swt.SWT
import org.eclipse.swt.printing.PrinterData
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.FileDialog
import org.eclipse.swt.widgets.Shell
import org.slf4j.LoggerFactory
import java.io.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.print.PrintService
import javax.print.StreamPrintServiceFactory

object EmbossingUtils {
    private const val EMBOSSER_OUTPUT_ENTRY = "embosser_output.dat"
    private const val EMBOSSER_CONFIG_ENTRY = "embosser_config.json"
    private val logger = LoggerFactory.getLogger(EmbossingUtils::class.java)

    val embossersFile: File = BBIni.getUserProgramDataFile("settings/embossers.json")

    @JvmStatic
    @JvmOverloads
    fun emboss(
        document: BBDocument,
        engine: UTDTranslationEngine,
        parent: Shell = Display.getCurrent().activeShell,
        openSettings: Consumer<Shell>? = null
    ): Int {

        val embosser = EmbossDialog(Display.getCurrent().activeShell, openSettings)
        if (!embosser.ensureEmbossersAvailable(parent)) {
            return -1
        }
        val pageSettings = engine.pageSettings
        var data: EmbosserConfig?
        var continueEmboss: Boolean
        do {
            continueEmboss = true
            val result = embosser.open()
            if (result != Window.OK) {
                return -1
            }
            data = embosser.embosser
            val driver = Optional.ofNullable(data).map(EmbosserConfig::embosserDriver)
                .orElseThrow {
                    NoSuchElementException("Config selected has no embosser driver, this should not be possible")
                }!!
            if (!embosserPrerequisitesMet(driver)) {
                return -1
            }
            val docWidth = BigDecimal.valueOf(pageSettings.paperWidth).setScale(2, RoundingMode.HALF_UP)
            val docHeight = BigDecimal.valueOf(pageSettings.paperHeight).setScale(2, RoundingMode.HALF_UP)
            val paperSize = driver.maximumPaper
            if (paperSize.width < docWidth || paperSize.height < docHeight) {
                continueEmboss = MessageDialog.openQuestion(
                    parent,
                    LocaleHandler.getDefault()["EmbossersManager.msgboxTitleDocumentTooLarge"],
                    LocaleHandler.getDefault()["EmbossersManager.msgboxMessageDocumentTooLarge"]
                )
            }
        } while (!continueEmboss)
        val config = data
        val start = embosser.startPage
        val end = embosser.endPage
        val scope = embosser.scope
        val copies = embosser.copies
        return try {
            val attributes = EmbossingAttributeSet()
            val leftMargin = toLengthBigDecimal(pageSettings.leftMargin)
            val rightMargin = toLengthBigDecimal(pageSettings.rightMargin)
            val topMargin = toLengthBigDecimal(pageSettings.topMargin)
            val bottomMargin = toLengthBigDecimal(pageSettings.bottomMargin)
            attributes.add(
                PaperMargins(Margins(leftMargin, rightMargin, topMargin, bottomMargin))
            )
            val pageWidth = toLengthBigDecimal(pageSettings.paperWidth)
            val pageHeight = toLengthBigDecimal(pageSettings.paperHeight)
            attributes.add(PaperSize(Rectangle(pageWidth, pageHeight)))
            // As BBX documents only say if to interpoint, at the moment we just use INTERPOINT or P1ONLY.
            // Other more advanced modes would require BBX to support more of these.
            val sides = if (pageSettings.interpoint) Layout.INTERPOINT else Layout.P1ONLY
            attributes.add(PaperLayout(sides))
            attributes.add(Copies(copies))
            attributes.add(BrailleCellType(BrlCell.NLS))
            val pages = if (scope == PrinterData.PAGE_RANGE) PageRanges(start, end) else PageRanges()
            attributes.add(pages)
            val pef = convertBBX2PEF(
                document.doc, "EmbossJob", engine, ALL_VOLUMES
            )
            if (embosser.isCreateDebugFile) {
                embossToFile(parent, buildMap {
                    put(
                        EMBOSSER_CONFIG_ENTRY,
                        EmbosserConfigToStreamFunction(config)
                    )
                    config!!.embosserDriver?.streamPrintServiceFactory?.ifPresent { f: StreamPrintServiceFactory ->
                        put(
                            EMBOSSER_OUTPUT_ENTRY,
                            EmbossToStreamFunction(
                                f
                            ) { ps: PrintService? -> config.embossPef(pef, attributes, ps!!) })
                    }
                })
            } else {
                config!!.embossPef(pef, attributes)
            }
            pef.getElementsByTagNameNS("http://www.daisy.org/ns/2008/pef", "page").length
        } catch (e: Exception) {
            val args: MutableMap<String?, Any?> = HashMap()
            args["embosser"] = data!!.name
            args["error"] = e.message
            Notify.notify(LocaleHandler.getDefault().format("cannotEmboss", args), Notify.EXCEPTION_SHELL_NAME, false)
            logger.error("Print Exception", e)
            -1
        }
    }

    private fun embosserPrerequisitesMet(driver: Embosser): Boolean {
        val prerequisiteWarnings = driver
            .checkPrerequisites()
            .filter { n: Notification -> NotificationType.WARNING >= n.notificationType }
            .toList()
        val noWarnings = prerequisiteWarnings.isEmpty()
        if (!noWarnings) {
            val warningMsg = prerequisiteWarnings.joinToString(
                "\n", prefix = String.format(
                    "%s%n%n", LocaleHandler.getDefault()["EmbossersManager.prerequisitesNotMet"]
                )
            ) { n: Notification -> n.getMessage(LocaleHandler.getDefault().locale) }
            Notify.showMessage(warningMsg)
        }
        return noWarnings
    }

    fun embossBrf(
        brfMinHeight: BigDecimal,
        brfMinWidth: BigDecimal,
        brfPath: Path,
        engine: ITranslationEngine,
        parent: Shell = Display.getCurrent().activeShell,
        openSettings: Consumer<Shell>? = null
    ): Int {
        val embosser = EmbossDialog(Display.getCurrent().activeShell, openSettings)
        if (!embosser.ensureEmbossersAvailable(parent)) {
            return -1
        }
        val result = embosser.open()
        val start = embosser.startPage
        val end = embosser.endPage
        val scope = embosser.scope
        val copies = embosser.copies
        if (result != Window.OK) {
            return -1
        }
        val data = embosser.embosser
        val driver = data?.embosserDriver
            ?: throw NoSuchElementException("Config does not contain embosser driver, this should not happen")
        if (!embosserPrerequisitesMet(driver)) {
            return -1
        }

        //Ensures a page size check is done before embossing.
        if (driver.maximumPaper.width < brfMinWidth || driver.maximumPaper.height < brfMinHeight) {
            if (!MessageDialog.openQuestion(
                    parent,
                    LocaleHandler.getDefault()["EmbossersManager.msgboxTitleDocumentTooLarge"],
                    LocaleHandler.getDefault()["EmbossersManager.msgboxMessageDocumentTooLarge"]
                )
            ) {
                //Return -1 if the user cancels the dialog.
                return -1
            }
        }

        return try {
            PageFilterInputStream(Files.newInputStream(brfPath)).use { inputStream ->
                val pageSettings = engine.pageSettings
                val attributes = EmbossingAttributeSet()
                val leftMargin = toLengthBigDecimal(pageSettings.leftMargin)
                val topMargin = toLengthBigDecimal(pageSettings.topMargin)
                val rightMargin = toLengthBigDecimal(pageSettings.rightMargin)
                val bottomMargin = toLengthBigDecimal(pageSettings.bottomMargin)
                attributes.add(
                    PaperMargins(Margins(leftMargin, rightMargin, topMargin, bottomMargin))
                )
                val paperWidth = toLengthBigDecimal(pageSettings.paperWidth)
                val paperHeight = toLengthBigDecimal(pageSettings.paperHeight)
                attributes.add(PaperSize(Rectangle(paperWidth, paperHeight)))
                attributes.add(Copies(copies))
                attributes.add(BrailleCellType(BrlCell.NLS))
                val pageLayout = if (pageSettings.interpoint) Layout.INTERPOINT else Layout.P1ONLY
                attributes.add(PaperLayout(pageLayout))
                val pages = if (scope == PrinterData.PAGE_RANGE) PageRanges(start, end) else PageRanges()
                attributes.add(pages)
                if (embosser.isCreateDebugFile) {
                    embossToFile(parent, buildMap {
                        put(EMBOSSER_CONFIG_ENTRY, EmbosserConfigToStreamFunction(data))
                        Optional.of(driver)
                            .flatMap { obj: Embosser -> obj.streamPrintServiceFactory }
                            .ifPresent { f: StreamPrintServiceFactory ->
                                put(
                                    EMBOSSER_OUTPUT_ENTRY,
                                    EmbossToStreamFunction(f) { ps: PrintService? ->
                                        data.embossBrf(inputStream, attributes, ps!!)
                                    }
                                )
                            }
                    })
                } else {
                    data.embossBrf(inputStream, attributes)
                }
                inputStream.pageNumber
            }
        } catch (e: Exception) {
            val args = mapOf("embosser" to data.name, "error" to e.message)
            Notify.notify(LocaleHandler.getDefault().format("cannotEmboss", args), Notify.EXCEPTION_SHELL_NAME, false)
            logger.error("Print Exception", e)
            -1
        }
    }

    private fun embossToFile(parent: Shell, zipEntryFunctions: Map<String, OutputToStreamFunction>): Boolean {
        require(
            !parent.isDisposed
        ) { "Shell has been disposed, this manager is no longer valid for use." }
        val saveDialog = FileDialog(parent, SWT.SAVE)
        saveDialog.setFilterNames(LocaleHandler.getDefault()["EmbossersManager.zipFiles"])
        saveDialog.setFilterExtensions("*.zip")
        saveDialog.overwrite = true
        val fileName = saveDialog.open()
        if (fileName == null) {
            MessageDialog.openInformation(
                parent,
                LocaleHandler.getDefault()["EmbossersManager.embossingCancelled"],
                LocaleHandler.getDefault()["EmbossersManager.embossingCancelledMsg"]
            )
            return true
        }
        val outputFile = File(fileName)
        var result = false
        try {
            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                for ((key, value) in zipEntryFunctions) {
                    val ze = ZipEntry(key)
                    zos.putNextEntry(ze)
                    value.consume(zos)
                    zos.closeEntry()
                }
                result = true
            }
        } catch (e: FileNotFoundException) {
            logger.error("Problem embossing to file", e)
            MessageDialog.openError(
                parent,
                LocaleHandler.getDefault()["EmbossersManager.fileNotFound"],
                LocaleHandler.getDefault()["EmbossersManager.fileNotFoundMsg"]
            )
        } catch (e: IOException) {
            logger.error("Problem writing to file when embossing", e)
            MessageDialog.openError(
                parent,
                LocaleHandler.getDefault()["EmbossersManager.embossToFileError"],
                LocaleHandler.getDefault()["Embossersmanager.embossToFileErrorMsg"]
            )
        }
        return result
    }

    private fun interface OutputToStreamFunction {
        @Throws(IOException::class)
        fun consume(os: OutputStream)
    }

    private class EmbossToStreamFunction(
        private val serviceFactory: StreamPrintServiceFactory, private val embossFunction: Function<PrintService?, Boolean>
    ) : OutputToStreamFunction {
        override fun consume(os: OutputStream) {
            val service = serviceFactory.getPrintService(os)
            if (!embossFunction.apply(service)) {
                throw RuntimeException("Unknown error whilst embossing to stream")
            }
        }

    }

    private class EmbosserConfigToStreamFunction(config: EmbosserConfig?) : OutputToStreamFunction {
        private val config: EmbosserConfig = requireNotNull(config)

        @Throws(IOException::class)
        override fun consume(os: OutputStream) {
            val configStr = Json.encodeToString(config)
            os.write(configStr.toByteArray(Charsets.UTF_8))
        }

    }
}