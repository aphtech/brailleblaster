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
package org.brailleblaster.math

import org.brailleblaster.math.ascii.ASCIIMathEditorDialog
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.mathml.NemethIndicators
import org.brailleblaster.math.mathml.NumericPassage
import org.brailleblaster.math.mathml.NumericSeries
import org.brailleblaster.math.spatial.GridEditor
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.SharedItem
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.tools.MenuTool
import org.brailleblaster.util.Notify
import org.eclipse.swt.SWT

object ToggleMathTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.MATH
    override val title: String = MathModuleUtils.MATH_TOGGLE
    override val accelerator: Int = SWT.MOD1 or 'M'.code
    override val sharedItem: SharedItem = SharedItem.MATH_TOGGLE
    override fun onRun(bbData: BBSelectionData) {
        MathModuleUtils.toggleMath(bbData.manager)
    }
}
object NumericSeriesTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.MATH
    override val title: String = MathModuleUtils.NUMERIC_SERIES
    override val sharedItem: SharedItem = SharedItem.NUMERIC_SERIES
    override fun onRun(bbData: BBSelectionData) {
        NumericSeries.toggleNumeric(bbData.manager)
    }
}
object AsciiMathEditorTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.MATH
    override val title: String = MathModuleUtils.ASCII_EDITOR
    override val sharedItem: SharedItem = SharedItem.ASCII_EDITOR
    override fun onRun(bbData: BBSelectionData) {
        ASCIIMathEditorDialog(bbData.manager)
    }
}
object SpatialComboTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.MATH
    override val title: String = MathModuleUtils.SPATIAL_COMBO
    override val sharedItem: SharedItem = SharedItem.SPATIAL_COMBO
    override fun onRun(bbData: BBSelectionData) {
        GridEditor()
    }
}
object NemethBlockTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.MATH
    override val title: String = MathModuleUtils.NEMETH_BLOCK
    override val sharedItem: SharedItem = SharedItem.NEMETH_BLOCK
    override fun onRun(bbData: BBSelectionData) {
        NemethIndicators.block(bbData.manager)
    }
}
object NemethInlineTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.MATH
    override val title: String = MathModuleUtils.NEMETH_INLINE
    override val sharedItem: SharedItem = SharedItem.NEMETH_INLINE
    override fun onRun(bbData: BBSelectionData) {
        NemethIndicators.inline(bbData.manager)
    }
}
object NumericBlockTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.MATH
    override val title: String = MathModuleUtils.NUMERIC_BLOCK
    override val sharedItem: SharedItem = SharedItem.NUMERIC_BLOCK
    override fun onRun(bbData: BBSelectionData) {
        NumericPassage.block(bbData.manager)
    }
}
object NumericInlineTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.MATH
    override val title: String = MathModuleUtils.NUMERIC_INLINE
    override val sharedItem: SharedItem = SharedItem.NUMERIC_INLINE
    override fun onRun(bbData: BBSelectionData) {
        NumericPassage.inline(bbData.manager)
    }
}
object AboutMathTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.MATH
    override val title: String = MathModuleUtils.MATH_HELP
    override val sharedItem: SharedItem = SharedItem.ABOUT_MATH
    override fun onRun(bbData: BBSelectionData) {
        AboutMathDialog.createDialog(bbData.manager.display.activeShell)
    }
}
object MathTableTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.MATH
    override val title: String = MathModuleUtils.MATH_TABLE
    override val sharedItem: SharedItem = SharedItem.MATH_TABLE
    override fun onRun(bbData: BBSelectionData) {
        Notify.notify(MathModuleUtils.NOT_YET_IMPLEMENTED, Notify.ALERT_SHELL_NAME)
    }
}