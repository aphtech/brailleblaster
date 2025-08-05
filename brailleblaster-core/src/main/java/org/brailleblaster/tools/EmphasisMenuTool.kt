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
package org.brailleblaster.tools

import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.EmphasisItem
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.perspectives.mvc.modules.views.EmphasisModule

interface EmphasisMenuTool : MenuToolModule {
    override val topMenu: TopMenu
        get() = TopMenu.EMPHASIS
    val emphasis: EmphasisItem
    override val title: String
        get() = emphasis.longName

    override fun onRun(bbData: BBSelectionData) {
        EmphasisModule.addEmphasis(bbData.manager.simpleManager, emphasis.emphasisType)
    }
}