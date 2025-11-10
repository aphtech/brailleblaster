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
package org.brailleblaster.perspectives.braille.ui

import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.findBlock
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.ui.WrapSelectionInTextSymbols.TextWrapCallBack
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.util.Utils

/**
 * Based on the selected text, create or find the block and surround with
 * the provided text symbols.  Text symbols are on their own line.
 */
object BlockSelectionInTextSymbols {
    /**
     * May return null;
     * @param m
     * @param startText
     * @param endText
     * @param callback for emphasis if needed
     * @return arraylist of blocks
     */
    fun block(m: Manager, startText: String?, endText: String?, callback: TextWrapCallBack?): List<Node>? {
        if (m.simpleManager.currentSelection.isTextNoSelection) {
            return null
        }
        val array: MutableList<Node> = ArrayList()
        val selection = m.textView.selection
        val startIndex = selection.x
        val endIndex = selection.y
        val start = m.mapList.getClosest(startIndex, true)
        val end = m.mapList.getClosest(endIndex, true)
        val startblock: Node = start.node.findBlock()
        var startparent = startblock.parent
        var startb = startparent.indexOf(startblock)
        val startnewBlock = BBX.BLOCK.DEFAULT.create()
        var startbegInd: Node = Text(startText)
        startnewBlock.appendChild(startbegInd)
        if (MathModuleUtils.isSpatialMath(startparent)) {
            val mathParent = MathModuleUtils.getSpatialMathParent(startparent)
            startparent = mathParent.parent
            startb = mathParent.parent.indexOf(mathParent)
        }
        Utils.insertChildCountSafe(startparent, startnewBlock, startb)
        if (callback != null) {
            startbegInd = callback.wrap(startbegInd)
        }
        array.add(startnewBlock)
        val endblock: Node = end.node.findBlock()
        var endparent = endblock.parent
        var endb = endparent.indexOf(endblock)
        val endnewBlock = BBX.BLOCK.DEFAULT.create()
        var endbegInd: Node = Text(endText)
        endnewBlock.appendChild(endbegInd)
        if (MathModuleUtils.isSpatialMath(endparent)) {
            val mathParent = MathModuleUtils.getSpatialMathParent(endparent)
            endparent = mathParent.parent
            endb = mathParent.parent.indexOf(mathParent)
        }
        Utils.insertChildCountSafe(endparent, endnewBlock, endb + 1)
        XMLHandler.wrapNodeWithElement(
            endbegInd,
            BBX.INLINE.EMPHASIS.create(EmphasisType.NO_TRANSLATE)
        )
        if (callback != null) {
            endbegInd = callback.wrap(endbegInd)
        }
        array.add(endnewBlock)
        return array
    }
}