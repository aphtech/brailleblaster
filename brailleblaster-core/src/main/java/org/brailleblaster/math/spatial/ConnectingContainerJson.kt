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
package org.brailleblaster.math.spatial

import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.spatial.SpatialMathEnum.HorizontalJustify
import org.brailleblaster.math.spatial.SpatialMathEnum.VerticalJustify

class ConnectingContainerJson : ISpatialMathContainerJson {
    private lateinit var printText: String
    var isMath = false
    private var verticalJustify: VerticalJustify? = null
    private var horizontalJustify: HorizontalJustify? = null
    override fun jsonToContainer(): ISpatialMathContainer {
        val cc = ConnectingContainer()
        val connectingContainerJson = this
        connectingContainerJson.horizontalJustify?.let {
            cc.settings.horizontal = it
        }
        connectingContainerJson.verticalJustify?.let {
            cc.settings.vertical = it
        }
        cc.settings.isTranslateAsMath = connectingContainerJson.isMath
        cc.text = MathText(
            print = connectingContainerJson.printText,
            braille =
            if (isMath) MathModuleUtils.translateMathPrint(connectingContainerJson.printText) else MathModuleUtils.translateMainPrint(
                connectingContainerJson.printText
            )
        )
        return cc
    }

    override fun containerToJson(container: ISpatialMathContainer): ISpatialMathContainerJson {
        val cc = container as ConnectingContainer
        isMath = cc.settings.isTranslateAsMath
        printText = cc.printText
        verticalJustify = cc.settings.vertical
        horizontalJustify = cc.settings.horizontal
        return this
    }
}