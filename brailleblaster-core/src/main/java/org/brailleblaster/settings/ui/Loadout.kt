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
package org.brailleblaster.settings.ui

import org.eclipse.swt.SWT

class Loadout(@JvmField var name: String, @JvmField var accelerator: Int) {
    companion object {
        val DEFAULT_LOADOUT_ACCELERATOR = SWT.MOD1 + SWT.MOD2 + 'L'.code
        val list: List<Loadout> by lazy { listOf(
            Loadout("Basic", SWT.MOD1 + SWT.MOD2 + 'B'.code),
            Loadout("Captions", SWT.MOD1 + SWT.MOD2 + 'C'.code),
            Loadout("Heading", SWT.MOD1 + SWT.MOD2 + 'H'.code),
            Loadout("List", DEFAULT_LOADOUT_ACCELERATOR),
            Loadout("Poetry", SWT.MOD1 + SWT.MOD2 + 'P'.code),
            Loadout("Plays", SWT.MOD1 + SWT.MOD2 + 'A'.code),
            Loadout("Glossary", SWT.MOD1 + SWT.MOD2 + 'G'.code),
            Loadout("Exercise", SWT.MOD1 + SWT.MOD2 + 'E'.code),
            Loadout("Index", SWT.MOD1 + SWT.MOD2 + 'I'.code),
            Loadout("Numeric", SWT.MOD1 + SWT.MOD2 + 'U'.code),
            Loadout("Notes", SWT.MOD1 + SWT.MOD2 + 'N'.code),
            Loadout("Miscellaneous", SWT.MOD1 + SWT.MOD2 + '0'.code)
        ) }

        fun getAcc(name: String?): Int = list.firstOrNull { it.name.equals(name, ignoreCase = true) }?.accelerator ?: -1

        fun getName(accelerator: Int): String? = list.firstOrNull { it.accelerator == accelerator }?.name
    }
}
