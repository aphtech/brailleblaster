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
package org.brailleblaster.util

import org.brailleblaster.BBIni
import org.eclipse.swt.SWTException
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.widgets.Display
import java.nio.file.Path

object ImageHelper {
    val imagesPath: Path = BBIni.programDataPath.resolve("images")
    const val MISSING_IMAGE_FILENAME = "missing.png"

    fun createScaledImage(fileName: String, scale: Float = 1f, fallback: String? = MISSING_IMAGE_FILENAME): Image = if (fallback == null) {
        val img = Image(Display.getCurrent(), imagesPath.resolve(fileName).toString())
        val width = img.imageData.width
        val height = img.imageData.height
        val returnImage =
            Image(Display.getCurrent(), img.imageData.scaledTo((width * scale).toInt(), (height * scale).toInt()))
        img.dispose()
        returnImage
    } else {
        try {
            createScaledImage(fileName, scale, null)
        } catch (_: SWTException) {
            //Intended for FileNotFoundExceptions, which SWT catches and throws as SWTExceptions
            createScaledImage(fallback, scale, null)
        }
    }
}
