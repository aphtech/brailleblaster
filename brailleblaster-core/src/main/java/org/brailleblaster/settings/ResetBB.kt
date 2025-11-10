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
package org.brailleblaster.settings

import org.brailleblaster.utils.BBData
import org.brailleblaster.BBIni
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.tools.MenuToolModule
import org.brailleblaster.util.LINE_BREAK
import org.brailleblaster.util.Notify.showMessage
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.MessageBox
import org.eclipse.swt.widgets.Shell
import org.slf4j.LoggerFactory
import java.nio.file.Files
import kotlin.system.exitProcess

object ResetBB : MenuToolModule {
    private val log = LoggerFactory.getLogger(ResetBB::class.java)
    override val topMenu = TopMenu.HELP
    override val title = "Reset BB"
    override fun onRun(bbData: BBSelectionData) {
        if (!ask()) return

        //Bye bye
        //This file is checked by BBBoostrap for if the version is out of date
        val versionFile = BBIni.userProgramDataPath.parent.resolve(BBData.USERDATA_VERSION_FILE)
        log.debug("Deleting file " + versionFile.toAbsolutePath())
        try {
            //File.delete() doesn't throw an exception if it fails to delete
            Files.delete(versionFile)
        } catch (e: Exception) {
            throw RuntimeException("Cannot delete file " + versionFile.toAbsolutePath(), e)
        }
        showMessage("Please restart BrailleBlaster.")
        exitProcess(1)
    }

    private fun ask(): Boolean {
        val messageStr = ("WARNING: DO NOT CONTINUE UNLESS INSTRUCTED$LINE_BREAK${LINE_BREAK}This will DELETE ALL user profile data, including logs and UNSAVED work. ${LINE_BREAK}This should only be a LAST RESORT. Please report bugs first!${LINE_BREAK}${LINE_BREAK}Continue?")
        val d = WPManager.display
        val s = Shell(d)
        val msgB = MessageBox(s, SWT.APPLICATION_MODAL or SWT.YES or SWT.NO)
        msgB.message = messageStr
        val result = msgB.open()
        return result == SWT.YES
    }
}