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
package org.brailleblaster.perspectives.mvc.modules.misc

import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.menu.*
import org.brailleblaster.perspectives.mvc.menu.MenuManager.addToSharedSubMenus
import org.brailleblaster.perspectives.mvc.modules.views.EmphasisModule
import org.brailleblaster.utd.properties.EmphasisType
import org.eclipse.swt.SWT

class ChangeTranslationModule : SimpleListener {
    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent) {
            val smb = SubMenuBuilder(
                TopMenu.TOOLS,
                "Change Translation"
            )
            smb.addItem(
                "Direct",
                DIRECT_HOTKEY
            ) { e: BBSelectionData -> EmphasisModule.addEmphasis(e.manager.simpleManager, EmphasisType.NO_TRANSLATE) }
            smb.addItem(
                "Uncontracted",
                UNCONTRACTED_HOTKEY
            ) { e: BBSelectionData -> EmphasisModule.addEmphasis(e.manager.simpleManager, EmphasisType.NO_CONTRACT) }
            addToSharedSubMenus(SharedItem.CHANGE_TRANSLATION, smb)
            MenuManager.add(smb.build())
        }
    }

    companion object {
        @JvmField
		var DIRECT_HOTKEY = SWT.MOD1 or 'D'.code
        @JvmField
		var UNCONTRACTED_HOTKEY = SWT.MOD1 or SWT.MOD2 or 'T'.code
    }
}