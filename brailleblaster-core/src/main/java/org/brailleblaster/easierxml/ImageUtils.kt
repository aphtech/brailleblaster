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
package org.brailleblaster.easierxml

import nu.xom.Document
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.archiver2.Archiver2
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.ui.BBStyleableText
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.utils.TableUtils
import org.brailleblaster.utils.UTD_NS
import org.brailleblaster.utils.xom.childNodes
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object ImageUtils {
    /**
     * @param imgElem
     * @param archiver
     * @param imageSize
     * - the drawable area, square
     * @return
     */
	@JvmStatic
	fun getImage(imgElem: Element?, archiver: Archiver2, imageSize: Int): Image? {
        val imgSrc = BBX.SPAN.IMAGE.ATTRIB_SOURCE[imgElem]
            ?: throw NodeException("Image doesn't have src attribute ", imgElem)
        val imgFile = archiver.resolveSibling(Paths.get(imgSrc))
        if (!Files.exists(imgFile)) {
            return null
        }
        var img: Image = try {
            Image(Display.getCurrent(), Files.newInputStream(imgFile))
        } catch (e: IOException) {
            throw RuntimeException("Failed to render image at $imgFile", e)
        }
        var imgData = img.imageData
        val originalWidth = imgData.width
        val originalHeight = imgData.height
        val scaledHeight: Int
        val scaledWidth: Int
        if (originalWidth > originalHeight) {
            val extraPixels = imageSize - originalWidth
            if (extraPixels > 0) {
                //expand image
                scaledWidth = originalWidth + extraPixels
                val scaleFactor = scaledWidth.toDouble() / originalWidth.toDouble()
                scaledHeight = (originalHeight * scaleFactor).toInt()
            } else {
                //contract image
                scaledWidth = imageSize
                val scaleFactor = scaledWidth.toDouble() / originalWidth.toDouble()
                scaledHeight = (originalHeight * scaleFactor).toInt()
            }
        } else if (originalHeight > originalWidth) {
            val extraPixels = imageSize - originalHeight
            if (extraPixels > 0) {
                //expand image
                scaledHeight = originalHeight + extraPixels
                val scaleFactor = scaledHeight.toDouble() / originalHeight.toDouble()
                scaledWidth = (originalWidth * scaleFactor).toInt()
            } else {
                //contract image
                scaledHeight = imageSize
                val scaleFactor = scaledHeight.toDouble() / originalHeight.toDouble()
                scaledWidth = (originalWidth * scaleFactor).toInt()
            }
        } else {
            scaledHeight = imageSize
            scaledWidth = imageSize
        }
        imgData = imgData.scaledTo(scaledWidth, scaledHeight)
        img = Image(Display.getCurrent(), imgData)
        return img
    }

    @JvmStatic
	fun findAllImages(m: Manager): ArrayList<Node> {
        val doc: Document = m.doc
        val array = ArrayList<Node>()
        gatherImages(doc.rootElement, array)
        return array
    }

    private fun gatherImages(node: Node, array: ArrayList<Node>) {
        if (BBX.SPAN.IMAGE.isA(node) || BBX.CONTAINER.IMAGE.isA(node)) {
            array.add(node)
        } else {
            for (i in 0 until node.childCount) {
                gatherImages(node.getChild(i), array)
            }
        }
    }

    /**
     * Load archiver from active document, assuming its a manager
     *
     * @return
     */
    val archiver: Archiver2
        get() = WPManager.getInstance().controller.archiver

    fun isImage(node: Node): Boolean {
        return ((BBX.SPAN.IMAGE.isA(node) || BBX.CONTAINER.IMAGE.isA(node))
                && XMLHandler.ancestorElementNot(
            node
        ) { curAncestor ->
            curAncestor.getAttribute(
                TableUtils.ATTRIB_TABLE_COPY,
                UTD_NS
            ) != null
        })
    }

    // getXML will return null on cancel
	@JvmStatic
	fun getElementFromInputDescription(text: BBStyleableText): Element {
        return text.getXML(Element("output"))
    }

    @JvmStatic
	fun setImageDescription(
        imgElement: Element, descriptionElementRoot: Element,
        captionEnabled: Boolean
    ): Element {
        if (!isImage(imgElement)) {
            throw NodeException("Expected img element, got", imgElement)
        }
        imgElement.removeChildren()
        val newDescRoot: Element
        if (BBX.SPAN.IMAGE.isA(imgElement)) {
            newDescRoot = imgElement
        } else {
            // img is a container, text needs to be inside a block
            newDescRoot = if (captionEnabled) {
                BBX.BLOCK.STYLE.create("Caption")
            } else {
                BBX.BLOCK.DEFAULT.create()
            }
            imgElement.appendChild(newDescRoot)
        }

        // will work with both img span and container
        for (curDescChild in descriptionElementRoot.childNodes) {
            newDescRoot.appendChild(curDescChild.copy())
        }
        return imgElement
    }

    @JvmStatic
	fun matchingImages(doc: Document?, imgSrc: String): List<Element> {
        return FastXPath.descendant(doc).filterIsInstance<Element>().filter { node: Element -> isImage(node) && BBX.SPAN.IMAGE.ATTRIB_SOURCE[node] == imgSrc }.filter { node: Element ->
            XMLHandler.ancestorElementNot(node) { curAncestor: Element ->
                curAncestor
                    .getAttribute(TableUtils.ATTRIB_TABLE_COPY, UTD_NS) != null
            }
        }
    }

    /**
     * Block that can be used to navigate to the image
     *
     * @param imageNode
     * @return
     */
	@JvmStatic
	fun getImageNavigateBlock(imageNode: Node): Element? {
        return if (BBX.SPAN.IMAGE.isA(imageNode)) {
            var block = XMLHandler.ancestorVisitor(imageNode) { node: Node? -> BBX.BLOCK.isA(node) } as Element?
            // When not in a block treat the imageNode as the BLOCK, like a container.
            if (block == null) block = imageNode as Element?
            getPreviousNavigateBlock(block)
        } else if (BBX.CONTAINER.IMAGE.isA(imageNode)) {
            getPreviousNavigateBlock(imageNode as Element?)
        } else {
            null
        }
    }

    private fun getPreviousNavigateBlock(caret: Element?): Element? {
        var block = caret
        while (block != null && FastXPath.descendant(block).none { node: Node ->
                (node is Text
                        && (node.parent as Element).namespaceURI != UTD_NS)
            }) {
            block = FastXPath.preceding(block)
                .filterIsInstance<Element>().firstOrNull { node -> BBX.BLOCK.isA(node) }
        }
        return block
    }

    @JvmStatic
	fun imageNotFound(l: Label, imgDir: Path, imgPath: String) {
        l.text = "File " + imgPath + " not found in:" + System.lineSeparator() + imgDir
    }

    @JvmStatic
	fun willReplaceWarning(shell: Shell?) {
        val descWarning = Label(shell, SWT.NONE)
        descWarning.text = "Warning: This will replace the existing image description"
        descWarning.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_RED)
    }
}