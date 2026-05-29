/*
 * Copyright (C) 2026 American Printing House for the Blind
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
package org.brailleblaster.spi

import org.brailleblaster.perspectives.braille.Manager
import java.nio.file.Path

interface ExportProvider {
    val id: String
    val displayName: String
    val cliOption: String
    val cliDescription: String
        get() = "Export opened document to $displayName."
    val priority: Int
        get() = 0

    fun export(manager: Manager, outputPath: Path)
}