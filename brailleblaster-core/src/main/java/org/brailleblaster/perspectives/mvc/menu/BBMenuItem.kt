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

import org.eclipse.swt.SWT

/**
 * An abstraction of SWT's MenuItem to be used internally in MenuManager
 */
open class BBMenuItem internal constructor(
    override val topMenu: TopMenu?,
    override val title: String,
    override val accelerator: Int,
    override val onActivated: (BBSelectionData) -> Unit,
    override val enabled: Boolean = true,
    override val swtOpts: Int = SWT.PUSH,
    override val sharedItem: SharedItem? = null,
    override val enableListener: EnableListener? = null
) : IBBMenuItem
