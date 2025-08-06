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

import org.brailleblaster.utd.actions.*
import org.eclipse.swt.events.DisposeListener
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display

object ColorManager {
    private val bbColors: MutableList<BBColor> = ArrayList()
    fun getColor(hexCode: String, control: Control): Color {
        require(hexCode.length == 6) { "Not a valid hex code $hexCode" }

        val r = hexCode.take(2).toInt(16)
        val g = hexCode.substring(2, 4).toInt(16)
        val b = hexCode.substring(4, 6).toInt(16)

        return getColor(r, g, b, control)
    }

    /**
     *
     * Get the Color object for a specified value in the ColorManager.Colors enum.
     *
     *
     * ColorManager maintains a list of colors currently in use. The control passed
     * into this method will be added to that Color object's list of controls
     * if the Color is already in use, and will create a new Color object if that color
     * is not in use. When all of a Color's assigned controls are disposed, the Color will
     * be disposed.
     *
     *
     * Note: The Color object returned by this method should not be disposed. The
     * ColorManager handles all necessary disposing.
     *
     * @param color The desired color
     * @param control The control intending to use this color
     * @return A color object that is useable by your control
     */
    fun getColor(color: Colors, control: Control): Color {
        return getColor(color.r, color.g, color.b, control)
    }

    /**
     *
     * Get the Color object for a specified RGB value.
     *
     *
     * ColorManager maintains a list of colors currently in use. The control passed
     * into this method will be added to that Color object's list of controls
     * if the Color is already in use, and will create a new Color object if that color
     * is not in use. When all of a Color's assigned controls are disposed, the Color will
     * be disposed.
     *
     *
     * Note: The Color object returned by this method should not be disposed. The
     * ColorManager handles all necessary disposing.
     *
     * @param r Red value of the color
     * @param g Green value of the color
     * @param b Blue value of the color
     * @param control The control intending to use this color
     * @return A color object that is useable by your control
     */
    fun getColor(r: Int, g: Int, b: Int, control: Control): Color {
        var newColor = findColor(r, g, b)
        if (newColor != null) {
            if (!newColor.controls.contains(control)) {
                newColor.addControl(control)
            }
        } else {
            newColor = BBColor(r, g, b)
            newColor.addControl(control)
            newColor.initColor()
            bbColors.add(newColor)
        }
        return newColor.color
    }

    /**
     *
     * Get the Color object for a particular action.
     *
     *
     * ColorManager maintains a list of colors currently in use. The control passed
     * into this method will be added to that Color object's list of controls
     * if the Color is already in use, and will create a new Color object if that color
     * is not in use. When all of a Color's assigned controls are disposed, the Color will
     * be disposed.
     *
     *
     * Note: The Color object returned by this method should not be disposed. The
     * ColorManager handles all necessary disposing.
     *
     * @param action The UTD action to find a color for
     * @param control The control intending to use this color
     * @return A color object that is useable by your control, or null if no color
     * matches the action
     */
    fun getColorFromAction(action: IAction?, control: Control): Color? {
        return when (action) {
            is Trans1Action -> {
                getColor(Colors.YELLOW, control)
            }

            is Trans2Action -> {
                getColor(Colors.ORANGE, control)
            }

            is Trans3Action -> {
                getColor(Colors.GREEN, control)
            }

            is Trans4Action -> {
                getColor(Colors.BLUE, control)
            }

            is Trans5Action -> {
                getColor(Colors.LIGHT_PURPLE, control)
            }

            is CompBRLAction -> {
                getColor(Colors.BROWN, control)
            }

            is NoContractionAction -> {
                getColor(Colors.SEAGLASS, control)
            }

            is ScriptAction -> {
                getColor(Colors.KHAKI, control)
            }

            is DirectAction -> {
                getColor(Colors.LAVENDER, control)
            }

            else -> null
        }
    }

    fun equals(color1: Color, color2: Colors): Boolean {
        return color1.red == color2.r && color1.green == color2.g && color1.blue == color2.b
    }

    private fun findColor(r: Int, g: Int, b: Int): BBColor? {
        for (color in bbColors) {
            if (color.r == r && color.g == g && color.b == b) return color
        }
        return null
    }

    enum class Colors(val r: Int, val g: Int, val b: Int) {
        YELLOW(255, 255, 0),
        ORANGE(254, 204, 152),
        GREEN(0, 254, 0),
        BLUE(0, 254, 254),
        LIGHT_PURPLE(254, 152, 204),
        BROWN(192, 80, 76),
        PURPLE(254, 0, 254),
        KHAKI(189, 183, 107),
        RED(255, 0, 0),
        WHITE(255, 255, 255),
        BLACK(0, 0, 0),
        LAVENDER(225, 197, 228),
        SEAGLASS(198, 240, 220),
        BLUSH(241, 190, 191)
    }

    private class BBColor(val r: Int, val g: Int, val b: Int) {
        val controls: MutableList<Control> = ArrayList()
        lateinit var color: Color

        fun initColor() {
            color = Color(Display.getCurrent(), r, g, b)
        }

        fun addControl(control: Control) {
            controls.add(control)
            control.addDisposeListener(onDispose(control, this))
        }

        private fun onDispose(control: Control, self: BBColor): DisposeListener {
            return DisposeListener {
                controls.remove(control)
                if (controls.isEmpty()) {
                    bbColors.remove(self)
                    color.dispose()
                }
            }
        }
    }
}
