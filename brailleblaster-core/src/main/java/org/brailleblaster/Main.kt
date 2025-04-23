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
import org.brailleblaster.firstrun.runFirstRunWizard
import org.brailleblaster.logging.initLogback
import org.brailleblaster.logging.preLog
import org.brailleblaster.usage.*
import org.brailleblaster.userHelp.Project
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.util.Notify
import org.brailleblaster.util.NotifyUtils
import org.brailleblaster.util.SoundManager
import org.brailleblaster.util.WorkingDialog
import org.brailleblaster.utils.braille.singleThreadedMathCAT
import org.brailleblaster.wordprocessor.WPManager
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

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
    var isInitted = false
        private set

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            start(args)
        } catch (e: Throwable) {
            handleFatalException(e)
        } finally {
            ZipHandles.closeAll()
        }
        exitProcess(0)
    }

    @Throws(Exception::class)
    fun start(args: Array<String>) {
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
            } catch (e: Exception) {
                LoggerFactory.getLogger(Main::class.java).error("Failed to open $firstArg", e)
                RECOVERABLE_BOOT_EXCEPTIONS.add(e)
            }
            argsToParse.removeAt(0)
        }
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
                WPManager.createInstance(fileToOpen, usageManager).start()
                usageManager.logger.logDurationSeconds(tool = BB_TOOL, duration = Duration.between(bbStartTime, Instant.now()))
                usageManager.logger.logEnd(tool = BB_TOOL, message = runId.toString())
                usageManager.reportDataAsync().get()
            }
        }
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