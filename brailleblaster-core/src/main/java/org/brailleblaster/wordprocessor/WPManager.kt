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
package org.brailleblaster.wordprocessor

import nu.xom.XPathException
import org.apache.commons.lang3.exception.ExceptionUtils
import org.brailleblaster.AppProperties
import org.brailleblaster.BBIni
import org.brailleblaster.Main.deleteExceptionFiles
import org.brailleblaster.Main.handleFatalException
import org.brailleblaster.RECOVERABLE_BOOT_EXCEPTIONS
import org.brailleblaster.archiver2.ArchiverRecoverThread.Companion.recentSaves
import org.brailleblaster.archiver2.BRFArchiverLoader.Companion.isBRF
import org.brailleblaster.exceptions.*
import org.brailleblaster.perspectives.braille.BraillePerspective
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.toolbar.BrailleToolBar
import org.brailleblaster.perspectives.mvc.ViewManager.Companion.colorizeCompositeRecursive
import org.brailleblaster.perspectives.mvc.events.AppStartedEvent
import org.brailleblaster.perspectives.mvc.events.BuildToolBarEvent
import org.brailleblaster.perspectives.mvc.modules.misc.ExceptionReportingModule
import org.brailleblaster.perspectives.mvc.modules.misc.ExceptionReportingModule.exceptionRecoveryLevel
import org.brailleblaster.printers.PrintPreview
import org.brailleblaster.usage.SimpleUsageManager
import org.brailleblaster.usage.UsageManager
import org.brailleblaster.util.Notify.showException
import org.brailleblaster.util.Notify.showMessage
import org.brailleblaster.util.Utils.runtimeToString
import org.brailleblaster.util.WorkingDialog
import org.brailleblaster.util.ui.AutoSaveDialog
import org.brailleblaster.utils.OS
import org.brailleblaster.utils.os
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.CTabFolder
import org.eclipse.swt.custom.CTabFolder2Adapter
import org.eclipse.swt.custom.CTabFolderEvent
import org.eclipse.swt.custom.CTabItem
import org.eclipse.swt.events.*
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.MessageBox
import org.eclipse.swt.widgets.Shell
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.SplashScreen
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class WPManager private constructor(val usageManager: UsageManager) {
    lateinit var shell: Shell
        private set
    lateinit var folder: CTabFolder
        private set
    private lateinit var bbToolbar: BBToolBar
    lateinit var statusBar: BBStatusBar
        private set
    lateinit var currentPerspective: BraillePerspective
        private set
    lateinit var controller: Manager
    fun getControllerOrNull(): Manager? = if (this::controller.isInitialized) controller else null
    lateinit var list: LinkedList<Manager>
        private set
    private lateinit var folderListener: SelectionAdapter
    private var resizeListener: ControlListener? = null

    /**
     * @see org.brailleblaster.perspectives.mvc.BBSimpleManager.initMenu
     */
    var isMenuInitialized: Boolean = false
        get() {
            return if (!field) {
                field = true
                false
            } else {
                true
            }
        }
        private set
    var isManagerChanged: Boolean = false
        private set
    private var isClosed: Exception? = null
    private var userClosed = false
    private var openingDoc = false //RT 6370

    // This constructor is the entry point to the word processor. It gets things
    // set up, handles multiple documents, etc.
    fun initialize(fileName: Path?) {
        val startTime = System.currentTimeMillis()
        // Must be after display is created due to working dialog
        // Should WPManager take Display instead?
        deleteExceptionFiles()

        list = LinkedList()
        shell = Shell(display, SWT.SHELL_TRIM)
        shell.text = AppProperties.displayName
        shell.images = newShellIcons()

        //		ViewManager.colorizeShell(shell);
        val layout = FormLayout()
        shell.layout = layout

        folder = CTabFolder(this.shell, SWT.NONE)
        statusBar = BBStatusBar(shell)

        val location = FormData()
        location.left = FormAttachment(0)
        location.right = FormAttachment(100)
        location.top = FormAttachment(0, 0)
        location.bottom = FormAttachment(100, -15)
        folder.layoutData = location

        folder.addCTabFolder2Listener(object : CTabFolder2Adapter() {
            override fun close(event: CTabFolderEvent) {
                event.doit = false
                folder.removeSelectionListener(folderListener)

                val curSelection = folder.selectionIndex
                val closeIndex = folder.indexOf(event.item as CTabItem)
                if (closeIndex != curSelection) {
                    folder.setSelection(closeIndex)
                }

                closeCurrentManager()
            }
        })


        shell.addListener(SWT.Close) { logger.info("Main Shell handling Close event, about to dispose the main Display") }

        setShellScreenLocation(display, shell)
        shell.maximized = true
        shell.setActive()

        userClosed = false
        //Used for when user closes the shell
        shell.addListener(SWT.Close) { event: Event ->
            folder.setSelection(folder.itemCount - 1)
            if (!openingDoc) {
                if (!close()) {
                    event.doit = false
                } else {
                    userClosed = true
                }
            } else {
                event.doit = false
            }
        }
        if (os == OS.Mac) {
            for (mi in display.systemMenu.items) {
                if (mi.id == SWT.ID_QUIT) {
                    mi.addListener(SWT.Selection) { it.doit = close() }
                }
            }
        }

        //Used for when tests are disposing the view
        shell.addListener(SWT.Dispose) {
            if (isClosed == null && !userClosed) {
                close()
            }
        }

        val fileToOpen = when {
            fileName != null -> fileName
            BBIni.debugging && BBIni.debugFilePath != null -> BBIni.debugFilePath
            else -> null
        }
        currentPerspective = BraillePerspective(this)
        addDocumentManager(fileToOpen)
        val firstManager = controller

        //move to here for #6370: Open BB, Close it before it can open, get a fatal exception
        /*
		Issue #6458: open() *MUST* be followed by closing the spash screen.
		Maybe SWT does some AWT/Swing handling?
		*/
        shell.open()
        // User sees a window, can close splash screen now
        val splash = SplashScreen.getSplashScreen()
        splash?.close()

        if (firstManager.text != null && firstManager.text.view.isVisible) {
            firstManager.text.view.setFocus()
        }

        //Add after shell is opened to prevent from firing on start
        folder.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                val index = folder.selectionIndex
                if (!list.isEmpty()) {
                    changeManager(list[index])
                }
            }
        }.also { folderListener = it })

        colorizeCompositeRecursive(shell)

        logger.info("Started in {}", runtimeToString(startTime))

        //auto save dialog
        val recentSaves = recentSaves
        if (!BBIni.debugging && recentSaves.isNotEmpty() && Path(BBIni.autoSaveCrashPath).exists()) {
            showAutoSaveDialog(recentSaves)
            try {
                Files.deleteIfExists(Paths.get(BBIni.autoSaveCrashPath))
            } catch (_: IOException) {
            }
        }

        for (e in RECOVERABLE_BOOT_EXCEPTIONS) {
            showException("Error when starting", e)
        }
    }

    private fun showAutoSaveDialog(recentSaves: List<Path>): Int {
        val dialog = AutoSaveDialog(this.shell)
        return dialog.openAutoSaveDialog("Auto Save", recentSaves)
    }

    fun start() {
        currentManager?.simpleManager?.dispatchEvent(AppStartedEvent(Sender.SIMPLEMANAGER))
        while (!shell.isDisposed) {
            try {
                if (!display.readAndDispatch()) {
                    display.sleep()
                }
            } catch (e: Throwable) {
                //TODO: This is needed as mvn exec:java will override the default exeception handler in Main
                //      http://stackoverflow.com/a/33344714
                try {
                    if (e is BBException
                        && exceptionRecoveryLevel == ExceptionReportingModule.ExceptionRecoveryLevel.RECOVER
                    ) {
                        handleBBException(e)
                    }
                    showException("", e)
                } catch (e2: Throwable) {
                    e.addSuppressed(e2)
                    if (!BBIni.debugging) {
                        handleFatalException(e)
                    } else {
                        throw RuntimeException(
                            "Fatal Exception caught by WPManager.start"
                                    + System.lineSeparator()
                                    + "----------------- First exception ------------------"
                                    + ExceptionUtils.getStackTrace(e)
                                    + System.lineSeparator()
                                    + "----------------- Nested exception ------------------",
                            e2
                        )
                    }
                }
            }
        }
        display.dispose()
    }

    fun closeCurrentManager() {
        folder.removeSelectionListener(folderListener)

        val curSelection = folder.selectionIndex
        val index = folder.selectionIndex
        list[index].close()

        if (list.isEmpty()) {
            WorkingDialog("Opening a new document...").use {    //RT 7175
                addDocumentManager(null)
            }
        }

        folder.addSelectionListener(folderListener)
        folder.setSelection(curSelection.coerceAtMost(folder.itemCount - 1))
        folder.notifyListeners(SWT.Selection, Event())
    }

    fun close(): Boolean {
        if (isClosed != null) {
            logger.error("Last close call", isClosed)
            val ex = RuntimeException("close called twice")
            ex.addSuppressed(isClosed)
            handleFatalException(ex)
        }

        isClosed = RuntimeException("first close call")

        try {
            folder.removeSelectionListener(folderListener)
            for (i in list.indices.reversed()) {
                val temp = list[i]
                if (!temp.close()) {
                    isClosed = null
                    folder.addSelectionListener(folderListener)
                    return false
                }
            }
            if (!list.isEmpty()) {
                logger.error(
                    "Unable to close {}",
                    list.joinToString(separator = ", ") { it.toString() }
                )
            }


            INSTANCE = null
            shell.dispose()
        } catch (e: Exception) {
            if (BBIni.debugging) {
                throw e
            }
            handleFatalException(e)
        }
        return true
    }

    private fun setShellScreenLocation(display: Display, shell: Shell) {
        val primary = display.primaryMonitor
        val bounds = primary.bounds
        val rect = shell.bounds
        val x = bounds.x + ((bounds.width - rect.width) / 2)
        val y = bounds.y + ((bounds.height - rect.height) / 2)
        shell.setLocation(x, y)
    }

    private fun newManager(fileName: Path?, oldManager: Manager?): Manager {
        try {
            return Manager(this, fileName)
        } catch (e: Exception) {
            if (oldManager != null) {
                changeManager(oldManager)
            }

            throw RuntimeException("Failed to init Manager", e)
        }
    }

    fun addDocumentManager(fileName: Path?) {
        if (fileName == null || (fileName.exists() && !fileName.isDirectory())) {
            val numberOfTab = 10
            if (list.size < numberOfTab) {
                openingDoc = true
                if (isBRF(fileName)) {
                    if (list.isEmpty()) {
                        addDocumentManager(null)
                    }
                    openBRFFile(fileName)
                    openingDoc = false
                    return
                }

                val oldManager = currentManager
                setupDocumentManager(fileName, oldManager)
                openingDoc = false
            } else {
                showMessage("The number of tabs allowed open is 10.")
            }
        } else {
            if (fileName in RecentDocs.defaultRecentDocs.recentDocs) {
                val removeFromRecentDocs = MessageBox(shell, SWT.ICON_ERROR or SWT.YES or SWT.NO).apply {
                    text = "File unavailable"
                    message = "The file $fileName is unavailable. Would you like to remove it from recent documents?"
                }.open()
                if (removeFromRecentDocs == SWT.YES) {
                    RecentDocs.defaultRecentDocs.removeRecentDoc(fileName)
                    currentManager?.simpleManager?.initMenu(shell)
                }
            }  else {
                showMessage(fileName.fileName.toString() + " cannot be opened and may have been relocated.")
            }
        }
    }

    private fun setupDocumentManager(fileName: Path?, oldManager: Manager?) {
        val m = newManager(fileName, oldManager)
        try {
            // try opening file first before everything else / RT-7789
            m.open()
            list.add(m)
            controller = m
            setSelection()
            /*
          Issue #6449: Restore text view focus
          Presumably when focusing the tab it then focus the first control in that tab,
          which is the style view
          */
            m.textView.setFocus()
            m.setStatusBarText(statusBar)
        } catch (e: Throwable) {
            logger.error("Detected an error whilst attempting to open a file, attempting to recover.", e)
            // cleanup
            openingDoc = false //RT 7896
            m.closeThreads()
            folder.removeSelectionListener(folderListener)
            val curSelection = folder.selectionIndex
            if (list.isEmpty()) {
                WorkingDialog("Opening a new document...").use {    //RT 7175
                    addDocumentManager(null)
                }
            }

            folder.addSelectionListener(folderListener)
            folder.setSelection(curSelection.coerceAtMost(folder.itemCount - 1))
            folder.notifyListeners(SWT.Selection, Event())

            val filenameString = fileName!!.fileName.toString()
            val fileExt = filenameString.substring(filenameString.lastIndexOf('.'))

            if (e is BBNotifyException) {
                //Generally a problem with the Archiver being null - an invalid file type
                //Message is already provided
                showMessage(e.message)
                //throw e;
            } else if (e is XPathException) {
                //Happens with an invalid ZIP file or outdated / corrupt bbz document
                if (fileExt == ".bbz") {
                    showMessage(
                        """$filenameString may be a file from an earlier release of BrailleBlaster and is not supported in this version.
 Please use an earlier version of BrailleBlaster to open this file."""
                    )
                } else {
                    showMessage(
                        filenameString +
                                " is not an archive that can be opened by BrailleBlaster."
                    )
                }
            } else {
                //Some other file open exception
                showMessage("$filenameString cannot be opened by BrailleBlaster")
                //throw new RuntimeException("Failed to open " + fileName, e);
            }
        } finally {
            val curManager = currentManager
            initMenuAndToolbar(curManager)
            isManagerChanged = true
            buildToolBar()
            isManagerChanged = false
        }

        //Issue #4638: Close empty template document when opening another file
        if (list.size == 2) {
            val newDocManager = list[0]
            if (newDocManager.isDefaultFile
                && !newDocManager.isDocumentEdited //Issue #5273: Allow multiple blank documents
                && !m.isDefaultFile
            ) {
                newDocManager.close()
            }
        }
    }

    private fun openBRFFile(fileName: Path?) {
        PrintPreview(shell, fileName, this)
        RecentDocs.defaultRecentDocs.addRecentDoc(fileName!!)
    }

    fun setSelection() {
        val index = list.size - 1
        folder.setSelection(index)
    }


    private fun initMenuAndToolbar(manager: Manager?) {
        //Creating the menu triggers a resize event, so remove the listener if it exists
        if (resizeListener != null) {
            shell.removeControlListener(resizeListener)
        }
        manager!!.simpleManager.initMenu(shell)
        buildToolBar()
        shell.addControlListener(object : ControlAdapter() {
            override fun controlResized(e: ControlEvent) {
                buildToolBar()
            }
        }.also { resizeListener = it })
    }

    private fun changeManager(manager: Manager) {
        controller = manager
        initMenuAndToolbar(manager)
        manager.setStatusBarText(statusBar)
    }

    fun buildToolBar() {
        currentPerspective.rebuildToolBar(this)
        bbToolbar = currentPerspective.toolBar
        controller.simpleManager.dispatchEvent(BuildToolBarEvent(Sender.NO_SENDER))
        (bbToolbar as BrailleToolBar).build()
        (folder.layoutData as FormData).top = FormAttachment(0, bbToolbar.height + PADDING_AFTER_TOOLBAR)
        shell.layout(true)
    }

    fun onToolBarExpand() {
        (folder.layoutData as FormData).top = FormAttachment(0, bbToolbar.height + (PADDING_AFTER_TOOLBAR * 2))
        shell.layout(true)
    }

    fun onToolBarCondense() {
        (folder.layoutData as FormData).top = FormAttachment(0, bbToolbar.height + PADDING_AFTER_TOOLBAR)
        shell.layout(true)
    }

    fun removeController(m: Manager) {
        isManagerChanged = true
        if (isClosed == null) {
            buildToolBar()
        }
        isManagerChanged = false
        list.remove(m)
    }

    val currentManager: Manager?
        get() {
            val index = folder.selectionIndex
            if (index == -1) {
                return null
            }
            return list[index]
        }

    fun isClosed(): Boolean {
        return isClosed != null
    }

    private fun handleBBException(e: BBException) {
        when (e) {
            is OutdatedMapListException -> {
                //Force a refresh to make the maplist update
                controller.refresh()
            }

            is EditingException -> {
                handleEditingException()
            }

            is CursorMovementException -> {
                //TODO: Do nothing?
            }

            is FormatterException -> {
                handleFormattingException(e, null)
            }

            is TranslationException -> {
                //Last resort is to refresh
                controller.refresh()
            }
        }
    }

    private fun handleEditingException() {
        //TODO: In the future we can use this case to restore the previous undo frame,
        //but for now refresh to make sure the MapList can be corrected
        controller.refresh()
    }

    private fun handleFormattingException(e: FormatterException, originalException: FormatterException?) {
        val origException = originalException ?: e
        when (e.curFallback) {
            null -> {
                logger.error("Reformatting document")
                //Try reformatting the document from the beginning
                try {
                    controller.reformatDocument(controller.doc)
                } catch (e2: FormatterException) {
                    handleFormattingException(e2, origException)
                }
            }
            FormatterException.Fallback.FORMAT_DOCUMENT -> {
                logger.error("Refreshing")
                //Try refreshing the entire document
                try {
                    controller.refresh()
                } catch (e2: FormatterException) {
                    handleFormattingException(e2, origException)
                }
            }
            FormatterException.Fallback.REFRESH -> {
                try {
                    logger.error("Escalating to an editing exception")
                    //Try undoing the edit
                    handleEditingException()
                } catch (_: RuntimeException) {
                    logger.error("Dying")
                    //Try dying
                    handleFatalException(origException)
                }
            }
        }
    }

    companion object {
        /**
         * This is the controller for the whole word processing operation. It is the
         * entry point for the word processor, and therefore the only public class.
         */
        private val logger: Logger = LoggerFactory.getLogger(WPManager::class.java)
        private var INSTANCE: WPManager? = null

        @JvmStatic
        fun getInstance(): WPManager {
            if (INSTANCE != null) {
                return INSTANCE!!
            } else throw IllegalStateException("WPManager has not been initialised")
        }

        fun getInstanceOrNull(): WPManager? {
            return INSTANCE
        }

        @JvmField
        val display: Display

        init {
            Display.setAppName("BrailleBlaster")
            display = Display()
        }

        private const val PADDING_AFTER_TOOLBAR = 20

        @JvmStatic
        fun createInstance(fileName: Path?): WPManager {
            return createInstance(fileName, SimpleUsageManager())
        }

        @Synchronized
        fun createInstance(fileName: Path?, usageManager: UsageManager): WPManager {
            if (INSTANCE != null && !BBIni.debugging) throw RuntimeException("Cannot create multiple WPManager instances")
            val result = WPManager(usageManager)
            INSTANCE = result
            result.initialize(fileName)
            return result
        }

        /**
         * Array of logo images from largest to smallest, meant to be passed to Shell.setImages
         *
         * @return
         */
        fun newShellIcons(): Array<Image> {
            return arrayOf(
                Image(Display.getCurrent(), BBIni.programDataPath.resolve(Paths.get("images", "toolbar", "large", "logo.png")).toString()),
                Image(Display.getCurrent(), BBIni.programDataPath.resolve(Paths.get("images", "toolbar", "medium", "logo.png")).toString()),
                Image(Display.getCurrent(), BBIni.programDataPath.resolve(Paths.get("images", "toolbar", "small", "logo.png")).toString()),
            )
        }
    }
}
