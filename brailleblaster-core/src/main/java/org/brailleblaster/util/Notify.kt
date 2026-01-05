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
package org.brailleblaster.util

import org.apache.commons.lang3.exception.ExceptionUtils
import org.brailleblaster.BBIni
import org.brailleblaster.exceptions.BBNotifyException
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.perspectives.mvc.modules.misc.ExceptionReportingModule
import org.brailleblaster.perspectives.mvc.modules.misc.ExceptionReportingModule.ErrorReportResponse
import org.brailleblaster.perspectives.mvc.modules.misc.ExceptionReportingModule.ExceptionReportingLevel
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.TraverseEvent
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.MessageBox
import org.eclipse.swt.widgets.Shell
import org.slf4j.LoggerFactory
import java.util.function.Consumer
import kotlin.system.exitProcess

/** Display a message.  */ // Do not depend on LocaleHandler or any other class
// as this class can be used before BBIni is initted for Exceptions
object Notify {
    private val log = LoggerFactory.getLogger(Notify::class.java)

    /**
     * If under debug mode throw an exception, default true. Note if changing to false in a unit test
     * you MUST set it back to true in a try...finally block so future tests do not break
     */
    @JvmField
    var DEBUG_EXCEPTION = true

    @JvmField
    val EXCEPTION_SHELL_NAME = getDefault()["exception"]

    @JvmField
    val ALERT_SHELL_NAME = getDefault()["alert"]
    private val USER_FRIENDLY_ERROR_MESSAGE = getDefault()["userFriendlyErrorMessage"]
    private val WARNING = getDefault()["warning"]
    private val BRAILLEBLASTER_ERROR = getDefault()["brailleblasterError"]

    @JvmField
    val GENERIC_UNEDITABLE = getDefault()["genericUneditable"]

    /**
     * For when you want to use the notify dialog without the "Exception" name on the shell
     *
     * @param message
     * @param shellName
     * @param isFatal
     */
    @JvmOverloads
    fun notify(message: String?, shellName: String, isFatal: Boolean = false) {
        val preformattedMessage = formatMessage(message)
        displayMessage(preformattedMessage, shellName, null, isFatal)
    }

    /**
     * Actually display message on screen
     * TODO
     * @param message
     * isFatal is a flag that signifies whether the program should close after the user sends a report (or not)
     * This should only be flagged true in Main, since fatal exceptions trace back to there.
     */
    private fun displayMessage(message: String?, shellName: String, exception: Throwable?, isFatal: Boolean) {
        if (BBIni.debugging && DEBUG_EXCEPTION) {
            //System.out.println("Throwing DebugException...");
            throw DebugException("Notify dialog message: $message")
        } else if (WPManager.getInstanceOrNull() != null
            && WPManager.display.thread !== Thread.currentThread()
        ) {
            //System.out.println("Spooling off new thread for displayMessage...");
            // User Exception #57,71,109: Handle threaded tools
            // This line isn't working as expected. Not for the table/newline error, anyway.
            WPManager.display.syncExec { displayMessage(message, shellName, exception, isFatal) }
            return
        }
        //Slight change in menu design depending if error is fatal
        // - don't want the user to ignore fatal ones.
        val shellStyle: Int = if (isFatal) {
            SWT.TITLE or SWT.APPLICATION_MODAL
        } else {
            SWT.DIALOG_TRIM or SWT.SYSTEM_MODAL
        }
        val shell = Shell(Display.getCurrent(), shellStyle)
        shell.text = shellName
        shell.layout = GridLayout(1, false)

        //Handle the escape key:
        if (isFatal) {
            shell.addTraverseListener { e: TraverseEvent ->
                if (e.detail == SWT.TRAVERSE_ESCAPE) {
                    e.doit = false
                }
            }
        } else {
            EasySWT.addEscapeCloseListener(shell)
        }

        //Error text box
        val text =
            EasySWT.makeStyledText(shell, SWT.V_SCROLL or SWT.READ_ONLY or SWT.BORDER or SWT.MULTI).text(message ?: "")
                .gridDataHeightFromGC(shell, 20).gridDataHorizontalFill().get()
        text.alwaysShowScrollBars = false

        //Behavior for "Send Report" button
        //Set just to close for now, since we've not determined if the exception is null yet.
        var sendCallback = Consumer<SelectionEvent> { shell.close() }
        //Behavior for "Don't Send Report" button
        val dontSendCallback = Consumer<SelectionEvent> {
            shell.close()
            log.info("No Error Report Sent")
            if (isFatal) {
                //Exit the program on fatal errors
                exitProcess(1)
            }
        }
        var okButtonText = "Send Report"
        var notOkButtonText = "Don't Send Report"

        //Build the rest of the menu
        if (exception != null) {
            //No checkbox!
            //Button enableReport = EasySWT2.makeCheckBox(shell, NotifyUtils.REPORT_TEXT, e -> {});
            EasySWT.makeLabel(shell, NotifyUtils.REPORT_COMMENT_TEXT, 1)

            //Error reporting comment text box
            //Need to see if the tabbing error is present
            val errorDesc = EasySWT.makeStyledText(shell, SWT.BORDER or SWT.V_SCROLL)
                .gridDataHorizontalFill()
                .gridDataHeightFromGC(shell, 3)
                .get()

            //Prevent the text box from eating the tab key during navigation.
            errorDesc.addTraverseListener { e: TraverseEvent ->
                if (e.detail == SWT.TRAVERSE_TAB_NEXT) {
                    e.doit = true
                }
            }
            //See how this looks.
            errorDesc.toolTipText = NotifyUtils.REPORT_TEXT
            // Do not allow reporting exceptions during development
            /*
      if (!isReleaseBuild) {
        errorDesc.setEnabled(false);
      }
      */

            //null indicates the user has not been asked about auto-uploading
            //Disabled for now since having this feature is an open question.
            /*
      if (userReportingLevel == null) {
        EasySWT2.addSelectionListener(
            enableReport,
            e -> ExceptionReportingModule.setAutoUploadEnabled(enableReport.getSelection()));
      } else {
        enableReport.setSelection(userReportingLevel);
      }
      */sendCallback = Consumer {
                // in ui thread
                val display = Display.getCurrent()
                //If not an empty comment box...
                if (errorDesc.text.isNotEmpty()) {
                    // Disabled for now since this feature is an open question
                    /*
            if (!enableReport.isEnabled() || !enableReport.getSelection()) {
              shell.close();
              return;
            }
             */
                    //Logging from this section of code never gets triggered. What's going on?
                    ExceptionReportingModule.reportException(
                        exception, errorDesc.text
                    ) { response: ErrorReportResponse ->
                        // in reporting thread
                        if (!response.success) {
                            log.info("Exception report failure!")
                            display.asyncExec {

                                // back in ui thread
                                var activeShell = display.activeShell
                                var fakeActiveShell = false
                                if (activeShell == null) {
                                    // make a fake one? MessageBox can't take a null shell
                                    fakeActiveShell = true
                                    activeShell = Shell()
                                }
                                val messageBox = MessageBox(
                                    activeShell, (SWT.ICON_ERROR)
                                            or SWT.OK
                                )
                                messageBox.text = response.title
                                messageBox.message = response.text
                                messageBox.open()
                                if (fakeActiveShell) {
                                    activeShell.dispose()
                                }
                            }
                        } else {
                            log.info("Exception report received successfully")
                        }
                    }
                    shell.close()
                    if (isFatal) {
                        //Exit the program on fatal errors
                        exitProcess(1)
                    }
                } else {
                    //Don't bother trying to send empty reports.
                    log.info("Empty Report! Nothing sent.")
                    shell.close()
                    if (isFatal) {
                        //Exit the program on fatal errors
                        exitProcess(1)
                    }
                }
            }
        } else {
            //Needed for non-exception windows, without need for a comment submission field.
            okButtonText = "OK"
            notOkButtonText = "Cancel"
        }
        val send = EasySWT.makePushButton(shell, okButtonText, 2) { }
        val dontSend = EasySWT.makePushButton(shell, notOkButtonText, 2) { }

        //Disable send button if not a release build.
        //if (!isReleaseBuild) {
        //  send.setEnabled(false);
        //}
        EasySWT.addSelectionListener(send, sendCallback)
        EasySWT.addSelectionListener(dontSend, dontSendCallback)
        EasySWT.setLargeDialogSize(shell)
        shell.defaultButton = send
        shell.open()
    }

    private fun formatMessage(rawmessage: String?, vararg args: Any?): String {
        val message = Utils.formatMessage(rawmessage, *args)
        log.error(message)
        val realMessage: String = if (message[0] == '&') {
            getDefault()[message]
        } else {
            message
        }
        return realMessage
    }

    @JvmStatic
    fun showMessage(rawmessage: String?, vararg args: Any?) {
        val realMessage = formatMessage(rawmessage, *args)
        displayMessage(realMessage, WARNING, null, false)
    }

    fun handleFatalException(message: String?, shellName: String, t: Throwable?) {
        //log.error("Encountered Fatal Exception: {}", message, t);
        displayMessage(message, shellName, t, true)
    }

    fun showException(exception: Throwable?) {
        showException(null, exception)
    }

    @JvmStatic
    fun showException(message: String?, exception: Throwable?) {
        log.error("Encountered Exception: {}", message, exception)
        if (BBIni.debugging) {
            throw DebugException("Notify dialog exception", exception)
        }
        //		else if (exception instanceof DebugModule.DebugFatalException) {
        //			throw new RuntimeException("Simulated exception when making the SWT exception dialog");
        //		}

        // TODO: Allow nested exceptions, but should we only check the first exception? May break
        // something though
        //		Throwable rootException = exception.getCause() != null
        //			? ExceptionUtils.getRootCause(exception)
        //					: exception;
        val bbNotifyException = ExceptionUtils.getThrowableList(exception).stream()
            .reduce(null) { result: Throwable?, current: Throwable? ->
                if (result != null) {
                    return@reduce result
                } else if (current is BBNotifyException) {
                    return@reduce current
                } else {
                    return@reduce null
                }
            }
        if (bbNotifyException != null) {
            // Don't display the full stack trace
            showMessage(bbNotifyException.message, EXCEPTION_SHELL_NAME)
            return
        }
        // For now, all non-BBExceptions will be displayed in a dialog
        when (ExceptionReportingModule.exceptionReportingLevel) {
            ExceptionReportingLevel.DEBUG -> showExceptionDialog(message, exception)
            ExceptionReportingLevel.USER_FRIENDLY -> showUserFriendlyExceptionDialog(message, exception)
            ExceptionReportingLevel.STATUS_BAR -> showExceptionStatusBar(exception)
            else -> {}
        }
    }

    //Shows exception message AND stack trace via generateExceptionMessage
    private fun showExceptionDialog(message: String?, exception: Throwable?) {
        displayMessage(generateExceptionMessage(message, exception), BRAILLEBLASTER_ERROR, exception, false)
    }

    //Shows exception message, but not the full stack trace.
    private fun showUserFriendlyExceptionDialog(message: String?, exception: Throwable?) {
        displayMessage(message, BRAILLEBLASTER_ERROR, exception, false)
    }

    private fun showExceptionStatusBar(exception: Throwable?) {
        WPManager.getInstance()
            .currentManager
            ?.setTemporaryStatusBarMessage(USER_FRIENDLY_ERROR_MESSAGE, true)
    }

    private fun generateExceptionMessage(message: String?, exception: Throwable?): String {
        return NotifyUtils.generateExceptionMessage(message, exception)
    }

    class DebugException : RuntimeException {
        constructor(message: String?) : super(message)
        constructor(message: String?, cause: Throwable?) : super(message, cause)
    }
}