/*
 * Copyright (C) 2025 American Printing House for the Blind
 * Copyright (C) 2026 Michael Whapples
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
package org.brailleblaster.perspectives.braille.views.wp.tableEditor

import org.brailleblaster.perspectives.braille.ui.BBStyleableText
import org.brailleblaster.perspectives.braille.views.wp.SixKeyHandler
import org.brailleblaster.utils.swt.AccessibilityUtils.setName
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.widgets.Composite

/**
 * Wrapper around BBStyleableText to store column and row information
 */
internal class CellText(parent: Composite?, @JvmField var row: Int, @JvmField var col: Int, sixKeyMode: Boolean) {
    @JvmField
    val text: BBStyleableText =
        BBStyleableText(parent!!, null, BBStyleableText.NONEWLINES, SWT.BORDER or SWT.V_SCROLL or SWT.WRAP)
    val sixKeyHandler = SixKeyHandler(null, null, sixKeyMode)
    var isCaption: Boolean = false
    var tn: Boolean = false

    init {
        text.text.addVerifyKeyListener(sixKeyHandler)
        text.text.addKeyListener(sixKeyHandler)
        setName(
            text.text,
            TableEditor.ACCESSIBLE_ROW_NAME + " " + (row + 1) + " " + TableEditor.ACCESSIBLE_COLUMN_NAME + " " + (col + 1)
        )
    }

    var isTN: Boolean
        get() = tn
        set(tn) {
            this.tn = tn
            if (tn) {
                if (col == -1) {
                    setName(text.text, TableEditor.ACCESSIBLE_TN_HEADING_NAME)
                } else {
                    setName(
                        text.text,
                        TableEditor.ACCESSIBLE_TN_HEADING_NAME + " " + TableEditor.ACCESSIBLE_COLUMN_NAME + " " + (col + 1)
                    )
                }
            }
        }

    fun dispose() {
        text.dispose()
    }

    val widget: StyledText
        get() = text.text
}
