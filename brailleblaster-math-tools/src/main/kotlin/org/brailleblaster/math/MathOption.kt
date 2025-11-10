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

import org.brailleblaster.math.mathml.MathModuleUtils

enum class MathOption(val key: String, val prettyString: String, val enabled: Boolean) {
    MATHHELP(MathModuleUtils.MATH_HELP_KEY, MathModuleUtils.MATH_HELP, true),
    ASCIIEDITOR(MathModuleUtils.ASCII_EDITOR_KEY, MathModuleUtils.ASCII_EDITOR, true),
    MATHTOGGLE(MathModuleUtils.MATH_TOGGLE_KEY, MathModuleUtils.MATH_TOGGLE, true),
    NEMETHBLOCK(MathModuleUtils.NEMETH_BLOCK_KEY, MathModuleUtils.NEMETH_BLOCK, true),
    NEMETHINLINE(MathModuleUtils.NEMETH_INLINE_KEY, MathModuleUtils.NEMETH_INLINE, true),
    NUMERICBLOCK(MathModuleUtils.NUMERIC_PASSAGE_BLOCK_KEY, MathModuleUtils.NUMERIC_PASSAGE_BLOCK, true),
    NUMERICINLINE(MathModuleUtils.NUMERIC_PASSAGE_INLINE_KEY, MathModuleUtils.NUMERIC_PASSAGE_INLINE, true),
    NUMERICSERIES(MathModuleUtils.NUMERIC_SERIES_KEY, MathModuleUtils.NUMERIC_SERIES, true),
    MATHTABLE(MathModuleUtils.MATH_TABLE_KEY, MathModuleUtils.MATH_TABLE, false),
    IMAGEDESCRIBER(MathModuleUtils.IMAGE_DESCRIBER_KEY, MathModuleUtils.IMAGE_DESCRIBER, true),
    SPATIALCOMBO(MathModuleUtils.SPATIAL_COMBO_KEY, MathModuleUtils.SPATIAL_COMBO, true)
}