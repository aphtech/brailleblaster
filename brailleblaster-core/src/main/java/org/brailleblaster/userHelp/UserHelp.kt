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
package org.brailleblaster.userHelp

import org.brailleblaster.AppProperties
import org.brailleblaster.BBIni
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.tools.MenuToolModule
import org.brailleblaster.util.Notify.notify
import org.brailleblaster.utils.BBData.getBrailleblasterPath
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.program.Program
import org.eclipse.swt.widgets.*
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths

object GoToWebsiteTool : MenuToolModule {
    override val topMenu: TopMenu = TopMenu.HELP
    override val title: String = "${AppProperties.displayName} Website"
    override fun onRun(bbData: BBSelectionData) {
        showHelp(HelpOptions.GoToSite)
    }
}
object UserGuideTool : MenuToolModule {
    override val topMenu: TopMenu = TopMenu.HELP
    override val title: String = "${AppProperties.displayName} User Guide"
    override val accelerator: Int = SWT.F1
    override fun onRun(bbData: BBSelectionData) {
        showHelp(HelpOptions.UserGuide)
    }
}
object AboutTool : MenuToolModule {
    override val topMenu: TopMenu = TopMenu.HELP
    override val title: String = "About ${AppProperties.displayName}"
    override fun onRun(bbData: BBSelectionData) {
        showHelp(HelpOptions.AboutBB)
    }
}

private enum class HelpOptions {
    UserGuide, AboutBB, GoToSite
}

private val helpPath = BBIni.helpDocsPath.toString() + FileSystems.getDefault().separator
private fun showHelp(helpChoice: HelpOptions) {
    when (helpChoice) {
        HelpOptions.GoToSite -> Program.launch(AppProperties.websiteUrl)
        HelpOptions.UserGuide -> showHelp("manualV2_1.html")
        HelpOptions.AboutBB -> showAbout()
    }
}
private fun showAbout() {
    val shell = Shell(SWT.DIALOG_TRIM or SWT.SYSTEM_MODAL)
    shell.text = "About ${AppProperties.displayName}"
    shell.layout = GridLayout(2, false)
    run {
        val img: Image = try {
            Image(
                Display.getCurrent(),
                Files.newInputStream(BBIni.programDataPath.resolve(Paths.get("images", "APH_Logo.png")))
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to load APH logo", e)
        }
        val text = Label(shell, SWT.PUSH)
        text.text = "APH Logo"
        text.image = img
        EasySWT.setGridDataVertical(text)
    }
    val sidePanel = Composite(shell, SWT.NONE)
    EasySWT.setGridDataVertical(sidePanel)
    sidePanel.layout = GridLayout()
    EasySWT.newLabel(
        sidePanel,
        "For more information or to report bugs visit ${AppProperties.websiteUrl}"
    )
    run {
        val text = StyledText(sidePanel, SWT.BORDER or SWT.READ_ONLY)
        text.text = versionsSimple
        EasySWT.setGridDataVertical(text)
    }
    val bottomPanel = Composite(sidePanel, SWT.NONE)
    EasySWT.setGridDataVertical(bottomPanel)
    //		((GridData) bottomPanel.getLayoutData()).horizontalSpan = 2;
    val rowLayout = RowLayout(SWT.HORIZONTAL)
    rowLayout.center = true
    bottomPanel.layout = rowLayout
    val license = Button(bottomPanel, SWT.PUSH)
    license.text = "License"

    // TODO #6115
    val privacyPolicy = Button(bottomPanel, SWT.PUSH)
    privacyPolicy.text = "APH Privacy Policy"

    // ------ Listeners ------
    EasySWT.addSelectionListener(license) { it: SelectionEvent ->
        val licensePath = getBrailleblasterPath("LICENSE.txt")
        val text: String = try {
            licensePath.readText(Charsets.UTF_8)
        } catch (ex: IOException) {
            throw RuntimeException("Unable to open license at " + licensePath.absolutePath, ex)
        }
        notify(text, "License")
    }
    EasySWT.addSelectionListener(privacyPolicy) { it: SelectionEvent ->
        val licensePath = getBrailleblasterPath("APH_Privacy_Policy.txt")
        val text: String = try {
            licensePath.readText(Charsets.UTF_8)
        } catch (ex: IOException) {
            throw RuntimeException("Unable to open license at " + licensePath.absolutePath, ex)
        }
        notify(text, "APH Privacy Policy")
    }
    shell.pack()
    shell.setActive()
    shell.open()
}

/**
 * Display help documents in the local browser.
 */
private fun showHelp(fileName: String) {
    val us = "file:///" + helpPath.replace('\\', '/') + fileName
    val uriString = us.replace(" ", "%20")
    Program.launch(uriString)
}