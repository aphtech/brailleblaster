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
package org.brailleblaster.updater

import dev.hydraulic.conveyor.control.SoftwareUpdateController
import dev.hydraulic.conveyor.control.SoftwareUpdateController.Availability.AVAILABLE
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.mvc.BBSimpleManager
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.spi.ModuleFactory
import org.brailleblaster.tools.MenuToolModule
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.swt.program.Program
import java.net.URI

object UpdaterModule : MenuToolModule {
    private val updater = SoftwareUpdateController.getInstance()
    override val topMenu: TopMenu = TopMenu.HELP
    override val title: String = "Check For Updates"
    override val enabled: Boolean
        get() = updater != null && updater.currentVersion != null

    override fun onRun(bbData: BBSelectionData) {
        if (enabled) {
            val currentVersion = updater.currentVersion ?: return
            try {
                val latestVersion = updater.currentVersionFromRepository
                if (latestVersion == null) {
                    MessageDialog.openWarning(bbData.manager.wpManager.shell, "Unable to find latest available version", "It was not possible to find the latest version of the software which is available.")
                    return
                }
                if (latestVersion > currentVersion) {
                    when(updater.canTriggerUpdateCheckUI()) {
                        AVAILABLE -> {
                            if (MessageDialog.openQuestion(bbData.manager.wpManager.shell, "Update available", "You can update to ${latestVersion.version}, you are running ${currentVersion.version}. Would you like to restart and update the software now?") && bbData.manager.wpManager.close()) {
                                updater.triggerUpdateCheckUI()
                            }
                        }
                        else -> {
                            if (MessageDialog.openQuestion(bbData.manager.wpManager.shell, "Update available", "You can update to ${latestVersion.version}, you are currently running ${currentVersion.version}. It is not possible to automatically update your software, you will need to download it yourself. Would you like to visit the download page?")) {
                                val downloadPage = URI.create(System.getProperty("app.repositoryUrl")).resolve("download.html")
                                Program.launch(downloadPage.toString())
                            }
                        }
                    }
                } else {
                    MessageDialog.openInformation(bbData.manager.wpManager.shell, "All up-to-date", "Great news, you are running the latest version of the software.")
                }
            } catch (e: SoftwareUpdateController.UpdateCheckException) {
                MessageDialog.openWarning(bbData.manager.wpManager.shell, "Error when checking for update", "There was an error whilst checking for updates. The error message given was ${e.message}")
            }
        }
    }
}
class UpdaterModuleFactory : ModuleFactory {
    override fun createModules(manager: Manager): Iterable<BBSimpleManager.SimpleListener> = listOf(UpdaterModule)
}