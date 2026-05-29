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
package org.brailleblaster

import org.brailleblaster.utils.BBData.brailleblasterPath
import org.brailleblaster.utils.BBData.userDataPath
import org.brailleblaster.archiver2.ZipHandles
import org.brailleblaster.archiver2.ArchiverFactory
import org.brailleblaster.firstrun.runFirstRunWizard
import org.brailleblaster.logging.initLogback
import org.brailleblaster.logging.preLog
import org.brailleblaster.spi.ExportProvider
import org.brailleblaster.usage.*
import org.brailleblaster.userHelp.Project
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.util.ExportService
import org.brailleblaster.util.Notify
import org.brailleblaster.util.NotifyUtils
import org.brailleblaster.util.SoundManager
import org.brailleblaster.util.WorkingDialog
import org.brailleblaster.utils.braille.singleThreadedMathCAT
import org.brailleblaster.wordprocessor.WPManager
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import kotlin.io.path.createDirectories

/**
 * This class contains the main method. If there are no arguments it passes
 * control directly to the word processor. If there are arguments it passes them
 * first to BBIni and then to Subcommands. It will do more processing as the
 * project develops.
 *
 * Note on Mac to initialize SWT properly you must pass -XstartOnFirstThread or
 * you will get "SWTException: Invalid Thread Access"
 */
object Main {
    private data class CliExportTarget(val provider: ExportProvider, val outputPath: Path)

    private val exportService = ExportService()

    var isInitted = false
        private set

    @JvmStatic
    fun main(args: Array<String>) {
        var exitCode = 0
        try {
            start(args)
        } catch (e: IllegalArgumentException) {
            System.err.println(e.message ?: "Invalid command-line arguments.")
            exitCode = 1
        } catch (e: Throwable) {
            handleFatalException(e)
            exitCode = 1
        } finally {
            ZipHandles.closeAll()
        }
        exitProcess(exitCode)
    }

    @Throws(Exception::class)
    fun start(args: Array<String>) {
        if (args.any { it == "--help" || it == "-h" }) {
            printCliHelp()
            return
        }

        val argsToParse = args.toMutableList()
        initBB(argsToParse)
        if (System.getProperty("dumpClassPath", "false") == "true") {
            dumpClassLoader(ClassLoader.getSystemClassLoader())
            //Handle maven-wrapper and presumably other IDE loaders
            if (ClassLoader.getSystemClassLoader() !== Thread.currentThread().contextClassLoader) {
                dumpClassLoader(Thread.currentThread().contextClassLoader)
            }
        }
        var fileToOpen: Path? = null
        if (argsToParse.isNotEmpty()) {
            val firstArg = argsToParse[0]
            try {
                fileToOpen = Paths.get(firstArg)
                if (!Files.exists(fileToOpen)) {
                    throw IllegalArgumentException("Input file does not exist: ${fileToOpen.toAbsolutePath()}")
                }
            } catch (e: Exception) {
                if (e is IllegalArgumentException) {
                    throw e
                }
                LoggerFactory.getLogger(Main::class.java).error("Failed to open $firstArg", e)
                throw IllegalArgumentException("Invalid input path: $firstArg", e)
            }
            argsToParse.removeAt(0)
        }

        val exportTargets = parseCliExportTargets(argsToParse)

        if (argsToParse.isNotEmpty()) {
            LoggerFactory.getLogger(Main::class.java)
                .error("Unknown extra arguments beyond file: " + args.joinToString(" "))
        }
        createDefaultBBUsageManager().use { usageManager ->
            // Need to get the display to ensure initialised before the FirstRunWizard
            WPManager.display
            if (runFirstRunWizard(usageManager = usageManager, userSettings = BBIni.propertyFileManager)) {
                val runId = UUID.randomUUID()
                usageManager.logger.log(UsageRecord(tool = BB_TOOL, event = "product", message = Project.BB.displayName))
                usageManager.logger.log(
                    UsageRecord(
                        tool = BB_TOOL,
                        event = "version",
                        message = Project.BB.version
                    ))
                usageManager.logger.log(UsageRecord(tool = BB_TOOL, event = "os", message = System.getProperty("os.name")))
                usageManager.logger.log(UsageRecord(tool = BB_TOOL, event = "os-version", message = System.getProperty("os.version")))
                usageManager.startPeriodicDataReporting(0, 5, units = TimeUnit.MINUTES)
                val bbStartTime = Instant.now()
                usageManager.logger.logStart(tool = BB_TOOL, message = runId.toString())
                if (exportTargets.isEmpty()) {
                    WPManager.createInstance(fileToOpen, usageManager).start()
                } else {
                    runCommandLineExport(fileToOpen, usageManager, exportTargets)
                }
                usageManager.logger.logDurationSeconds(tool = BB_TOOL, duration = Duration.between(bbStartTime, Instant.now()))
                usageManager.logger.logEnd(tool = BB_TOOL, message = runId.toString())
                usageManager.reportDataAsync().get()
            }
        }
    }

    private fun parseCliExportTargets(argsToParse: MutableList<String>): List<CliExportTarget> {
        val targets = mutableListOf<CliExportTarget>()
        var i = 0
        while (i < argsToParse.size) {
            val provider = exportService.providerByCliOption(argsToParse[i]) ?: run {
                i++
                continue
            }

            if (i + 1 >= argsToParse.size) {
                throw IllegalArgumentException("Missing output path for option ${argsToParse[i]}")
            }

            val outputPath = Paths.get(argsToParse[i + 1])
            targets.add(CliExportTarget(provider, outputPath))

            argsToParse.removeAt(i + 1)
            argsToParse.removeAt(i)
        }

        val duplicates = targets.groupBy { it.provider.id }.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            throw IllegalArgumentException(
                "Duplicate export options are not allowed: ${duplicates.joinToString(", ") { "--$it" }}"
            )
        }
        return targets
    }

    private fun runCommandLineExport(fileToOpen: Path?, usageManager: UsageManager, targets: List<CliExportTarget>) {
        require(fileToOpen != null) { "Command-line export requires an input file." }

        try {
            ArchiverFactory.load(fileToOpen).close()
        } catch (e: Throwable) {
            LoggerFactory.getLogger(Main::class.java)
                .error("Command-line export cannot open input '{}'", fileToOpen.toAbsolutePath(), e)
            System.err.println("Command-line export failed: unable to open input file '${fileToOpen.toAbsolutePath()}'")
            return
        }

        var wpManager: WPManager? = null
        try {
            wpManager = WPManager.createInstance(fileToOpen, usageManager)
            val manager = requireNotNull(wpManager.currentManager) { "Unable to open input document for export." }
            if (manager.isDefaultFile) {
                System.err.println("Unable to export '${fileToOpen.toAbsolutePath()}'. Input could not be opened as an editable document.")
                return
            }

            manager.checkForUpdatedViews()
            manager.waitForFormatting(true)

            val doc = manager.doc
            val engine = manager.document.settingsManager.engine

            for (target in targets) {
                val outputPath = target.outputPath.toAbsolutePath()
                outputPath.parent?.createDirectories()
                target.provider.export(manager, outputPath)
            }
        } catch (e: Throwable) {
            LoggerFactory.getLogger(Main::class.java)
                .error("Command-line export failed for '{}'.", fileToOpen.toAbsolutePath(), e)
            System.err.println("Command-line export failed: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            wpManager?.let {
                try {
                    it.close()
                } catch (closeError: Throwable) {
                    LoggerFactory.getLogger(Main::class.java).warn("Error while closing command-line export session", closeError)
                }
            }
        }
    }

        private fun printCliHelp() {
            val exportProviders = exportService.providers.toList()
            val optionLines = buildString {
                appendLine("    -h, --help         Show this help and exit.")
                for (provider in exportProviders) {
                    appendLine("    --${provider.cliOption} <file>      ${provider.cliDescription}")
                }
            }.trimEnd()
            val exampleLines = exportProviders.joinToString("\n") { provider ->
                "    brailleblaster book.bbz --${provider.cliOption} output.${provider.cliOption}"
            }.ifEmpty { "    brailleblaster paragraph_list.docx" }
            println(
                """
BrailleBlaster command-line usage:

    brailleblaster [input-file] [options]

Options:
    ${optionLines}

Examples:
    ${exampleLines}

Notes:
    - Input file must appear before export options.
    - Export uses the same default table and page settings used by normal startup.
""".trimIndent()
                )
        }

    /**
     *
     * @param argsToParse Arguments that will be removed from list when parsed
     */
    @JvmOverloads
    fun initBB(argsToParse: List<String>, initBBIni: Boolean = true) {
        if (isInitted && !BBIni.debugging) throw RuntimeException("Do not call init twice")
        val bbPath = brailleblasterPath
        println("BrailleBlaster path: $bbPath")
        val userPath = userDataPath
        var bbLogConfig = File(userPath, "programData/settings/logback.xml")
        if (!bbLogConfig.exists()) {
            bbLogConfig = File(bbPath, "programData/settings/logback.xml")
        }
        initLogback(bbLogConfig)
        singleThreadedMathCAT {
            setRulesDir(File(bbPath, "programData/MathCAT/Rules").absolutePath)
        }
        //Store node exceptions in user folder, as on Windows when installed, Program Files (our working directory) isn't writable
        if (System.getProperty(NodeException.SAVE_TO_DISK_FOLDER_PROPERTY) == null) {
            System.setProperty(NodeException.SAVE_TO_DISK_FOLDER_PROPERTY, userPath.toString())
        }

        //Catch any lingering exceptions 
        //TODO: If the -debug option is passed but BBIni throws an Exception, the dialog will still be shown
        //      But on a EU machine they need to see the dialog
        //      Unit tests that use BBIni.setDebuggingEnabled() won't be affected
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        if (!BBIni.debugging) {
            Thread.setDefaultUncaughtExceptionHandler { _: Thread?, e: Throwable -> handleFatalException(e) }
        }
        if (initBBIni) BBIni.initialize(argsToParse.toMutableList(), bbPath, userPath)
        if (BBIni.debugging) {
            //Hit above mentioned edge case, but BBIni ran succesfully
            Thread.setDefaultUncaughtExceptionHandler(oldHandler)
        }
        isInitted = true
    }

    /*
   * New version that uses Notify.java for a fully-generic version.
   */
    /**
     * Display something to the user before BB crashes with an un-recoverable Exception.
     * More friendly than arbitrary closing with no warning
     *
     * @param t Exception
     */
    @JvmStatic
    fun handleFatalException(t: Throwable) {

        //Same as before: system prints and suchlike
        println("---Fatal Exception caught by Main---")
        t.printStackTrace()
        val message = NotifyUtils.generateExceptionMessage(
            "FATAL ERROR: BrailleBlaster must now close. You may choose to send an error report to APH below.",
            RuntimeException("Fatal exception caught by Main, see logs", t)
        )

        //create a file to know if the system has a fatal error
        //BBini may not be loaded but errors that early don't have books open anyway
        if (BBIni.autoSaveCrashPath != null) {
            //System.out.println("Saving crash file in Main");
            BBIni.createAutoSaveCrashFile()
        }

        //Convenient way to test different platforms' system beeps - cause a fatal error!
        //SoundManager.playBeepSWT()
        SoundManager.playBeep()
        Notify.handleFatalException(message, "Fatal Error", t)
    }

    @JvmStatic
    fun deleteExceptionFiles() {
        /*
		Cleanup old Exception files from previous run
		User probably doesn't care at this point
		Over time folder size can grow substantially
		*/
        val filesToDelete: MutableList<File> = ArrayList()
        (userDataPath.listFiles() ?: arrayOf()).forEach { curFile ->
            if (curFile.name.startsWith(NodeException.FILENAME_PREFIX)) {
                filesToDelete.add(curFile)
                curFile.delete()
            }
        }
        if (filesToDelete.isNotEmpty()) {
            WorkingDialog("Cleanup Exception Files...").use {
                for (curFile in filesToDelete) {
                    preLog(Main::class.java, "Deleting old exception file " + curFile.absolutePath)
                    curFile.delete()
                }
            }
        }
    }

    private fun dumpClassLoader(curClassLoader: ClassLoader) {
        if (curClassLoader is URLClassLoader) {
            for (curString in curClassLoader.urLs) {
                println("ClassPath: $curString")
            }
        } else {
            println("Unknown ClassLoader " + curClassLoader.javaClass.toString())
        }
    }
}