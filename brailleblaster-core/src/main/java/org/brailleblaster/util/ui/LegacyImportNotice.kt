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
package org.brailleblaster.util.ui

import org.brailleblaster.BBIni
import org.brailleblaster.archiver2.Archiver2
import org.brailleblaster.archiver2.LegacyImports
import org.brailleblaster.bbx.legacy.LegacyImportReport
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.MessageBox
import org.eclipse.swt.widgets.Shell
import org.slf4j.LoggerFactory

/**
 * Tells the user, once, that the file they opened came from an older BrailleBlaster release and
 * was converted. Does nothing for any other file.
 */
object LegacyImportNotice {
    private val log = LoggerFactory.getLogger(LegacyImportNotice::class.java)

    @JvmStatic
    fun showIfConverted(shell: Shell?, archiver: Archiver2) {
        val report = LegacyImports.takeReport(archiver) ?: return
        val message = message(report)
        log.info("Opened converted document {}: {}", archiver.importedFrom, message)
        if (shell == null || shell.isDisposed || BBIni.debugging) {
            return
        }
        MessageBox(shell, SWT.ICON_INFORMATION or SWT.OK).apply {
            text = getDefault()["LegacyImport.title"]
            this.message = message
        }.open()
    }

    fun message(report: LegacyImportReport): String {
        val converted = String.format(getDefault()["LegacyImport.converted"], report.release)
        val fallback = if (report.fallbacks.isEmpty()) "" else
            "\n\n" + String.format(getDefault()["LegacyImport.fallbacks"], report.fallbacks.size)
        return converted + fallback + "\n\n" + getDefault()["LegacyImport.review"]
    }
}
