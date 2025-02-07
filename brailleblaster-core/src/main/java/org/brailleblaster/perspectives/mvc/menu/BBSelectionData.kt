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
package org.brailleblaster.perspectives.mvc.menu

import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.widgets.MenuItem
import org.eclipse.swt.widgets.ToolItem
import org.eclipse.swt.widgets.Widget

/**
 * Selection event that contains information about the current manager
 * and the widget that sent the event
 */
open class BBSelectionData(val widget: Widget, val wpManager: WPManager) {
    val manager: Manager = wpManager.controller
    var menuItem: MenuItem? = null
    var toolBarItem: ToolItem? = null

    override fun toString(): String {
        return "BBSelectionData {widget: $widget, menuItem: $menuItem, toolBarItem: $toolBarItem}"
    }
}
