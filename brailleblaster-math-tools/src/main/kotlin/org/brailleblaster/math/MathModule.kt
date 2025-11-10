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

import org.brailleblaster.perspectives.mvc.BBSimpleManager
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.menu.MenuManager
import org.brailleblaster.perspectives.mvc.modules.views.DebugModule

class MathModule : BBSimpleManager.SimpleListener {
    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent) {
            MenuManager.add(ToggleMathTool)
            MenuManager.add(NumericSeriesTool)
            MenuManager.add(AsciiMathEditorTool)
            MenuManager.add(SpatialComboTool)
            MenuManager.add(NemethBlockTool)
            MenuManager.add(NemethInlineTool)
            MenuManager.add(NumericBlockTool)
            MenuManager.add(NumericInlineTool)
            MenuManager.add(AboutMathTool)
            if (DebugModule.enabled) {
                /*
                * if you take one out of debug, enable it in the ToolBar Builder
                */

                MenuManager.add(MathTableTool)
            }
        }
    }
}