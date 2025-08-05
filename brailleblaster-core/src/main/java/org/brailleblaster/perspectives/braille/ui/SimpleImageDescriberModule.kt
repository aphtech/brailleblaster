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

import nu.xom.Element
import org.brailleblaster.bbx.BBX
import org.brailleblaster.easierxml.ImageUtils
import org.brailleblaster.easierxml.SimpleImageDescriberDialog
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.XMLSelection
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.tools.DebugMenuToolModule
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.internal.xml.XMLHandler2

object SimpleImageDescriberModule : DebugMenuToolModule {
    const val MENU_ITEM_NAME = "Image Describer"
    override val title = MENU_ITEM_NAME
    override fun onRun(bbData: BBSelectionData) {
        pressedOpen(bbData.manager)
    }

    private fun pressedOpen(m: Manager) {
        val imageCursor = getCaretImage(m.simpleManager.currentSelection)
        SimpleImageDescriberDialog(
            m,
            m.wpManager.shell,
            imageCursor
        ) { images: List<Element> -> handleUpdatedImages(m, imageCursor, images) }
    }

    private fun handleUpdatedImages(m: Manager, imageCursor: Element?, images: List<Element>) {
        m.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, images, true))
    }

    private fun getCaretImage(sel: XMLSelection): Element? {
        val imgParent =
            XMLHandler.ancestorVisitorElement(sel.start.node) { node: Element -> ImageUtils.isImage(node) }
        if (imgParent != null) {
            return imgParent
        }

        //Search current block
        var block = XMLHandler.ancestorVisitorElement(sel.start.node) { node: Element -> BBX.BLOCK.isA(node) }
        if (block == null) {
            //selection might be something strange
            block =
                XMLHandler2.nodeToElementOrParentOrDocRoot(sel.start.node)
        }
        return FastXPath.followingAndSelf(block).firstOrNull { node -> ImageUtils.isImage(node) } as Element?
    }
}