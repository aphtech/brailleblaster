/*
 * Copyright (C) 2025 Michael Whapples
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
package org.brailleblaster.util

import org.brailleblaster.BBIni
import org.brailleblaster.utils.OS
import org.brailleblaster.utils.os
import kotlin.io.path.absolutePathString

val PANDOC_CMD: String by lazy {
    BBIni.nativeBinPath.resolve(
        when (os) {
            OS.Windows -> "pandoc.exe"
            else -> "pandoc"
        }
    ).absolutePathString()
}

val PANDOC_VERSION: String by lazy {
    var version = "Unknown"
    val p = Runtime.getRuntime().exec(arrayOf(PANDOC_CMD, "--version"))
    if (p.waitFor() == 0)  {
        val firstLine = p.inputReader(Charsets.UTF_8).readLines().firstOrNull()
        if (firstLine != null) {
            version = firstLine.split(' ').last()
        }
    }
    version
}