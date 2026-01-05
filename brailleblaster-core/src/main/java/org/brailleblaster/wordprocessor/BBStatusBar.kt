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

import org.brailleblaster.util.ColorManager
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell

class BBStatusBar(documentWindow: Shell?) {
    private val statusBar: Label = Label(documentWindow, SWT.BORDER).apply {
        val location = FormData()
        location.left = FormAttachment(0)
        location.right = FormAttachment(75)
        location.bottom = FormAttachment(100)
        layoutData = location
    }

    fun setColor(color: ColorManager.Colors) {
        statusBar.foreground = ColorManager.getColor(color, statusBar)
    }

    fun setText(text: String?) {
        statusBar.text = text
    }

    fun resetLocation(left: Int, right: Int, bottom: Int) {
        val data = statusBar.layoutData as FormData
        data.left = FormAttachment(left)
        data.right = FormAttachment(right)
        data.bottom = FormAttachment(bottom)
        statusBar.layoutData = data
        statusBar.parent.layout()
    }
}