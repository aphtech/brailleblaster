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
package org.brailleblaster.math.mathml

import nu.xom.*
import org.brailleblaster.BBIni
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBXUtils
import org.brailleblaster.math.ascii.ASCII2MathML.translate
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.WhiteSpaceElement
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.modules.views.TextViewModule.Companion.getAllTextMapElementsInSelectedRange
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.internal.xml.XMLHandler2
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.UTDHelper
import org.brailleblaster.util.Utils
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.widgets.Display
import java.io.IOException
import java.nio.file.Paths

object MathUtils {
    fun nextTextIsMathButNotSameMathRoot(node: Node, mathParent: Element): Boolean {
        val nextText = node.query("following::text()")
        for (i in 0 until nextText.size()) {
            val text = nextText[i]
            if (XMLHandler.ancestorElementIs(text) { n -> UTDElements.BRL.isA(n) }) {
                continue
            }
            val thisMathParent = MathModule.getMathParent(text)
            if (thisMathParent == null) {
                return false
            } else if (thisMathParent != mathParent) {
                return true
            }
        }
        // if you have reached this, you are at the end of the document and the
        // only text you have found is math text
        // that belongs to the same math root of your math parent.
        return false
    }

    fun wrapInMath(math: String?, block: ParentNode, index: Int) {
        val mathNode = translate(math!!)
        Utils.insertChildCountSafe(block, mathNode, index)
        XMLHandler2.wrapNodeWithElement(
            mathNode,
            BBX.INLINE.MATHML.create()
        )
    }

    fun wrapInMath(i: Int, j: Int, s: String): ArrayList<MathAction> {
        val before = s.take(i).replace("\n".toRegex(), "").replace(
            "\r".toRegex(),
            ""
        )
        val math = s.substring(i, j).replace("\n".toRegex(), "").replace(
            "\r".toRegex(),
            ""
        )
        val after = s.substring(j).replace("\n".toRegex(), "")
            .replace("\r".toRegex(), "")
        return wrapInMath(before, MathSubject(math), after)
    }

    fun wrapInMath(before: String, math: MathSubject?, after: String): ArrayList<MathAction> {
        val array = ArrayList<MathAction>()
        if (before.isNotEmpty()) {
            array.add(MathAction(MathSubject(before), MathVerb.Verb.WrapInText))
        }
        array.add(MathAction(math!!, MathVerb.Verb.MakeMath))
        if (after.isNotEmpty()) {
            array.add(MathAction(MathSubject(after), MathVerb.Verb.WrapInText))
        }
        return array
    }

    fun wrapInMath(before: String, getNode: GetMathNode, o: Any?, after: String, block: ParentNode, index: Int): Node {
        var curIndex = index
        if (before.isNotEmpty()) {
            Utils.insertChildCountSafe(block, Text(before), curIndex++)
        }
        val mathNode = getNode.getNode(o!!)
        Utils.insertChildCountSafe(block, mathNode, curIndex++)
        XMLHandler2.wrapNodeWithElement(
            mathNode,
            BBX.INLINE.MATHML.create()
        )
        if (after.isNotEmpty()) {
            Utils.insertChildCountSafe(block, Text(after), curIndex)
        }
        return mathNode
    }

    fun getBlocksFromTextMapElements(mapElements: List<TextMapElement>): ArrayList<Node> {
        val array = ArrayList<Node>()
        for (mapElement in mapElements) {
            val block: Node = BBXUtils.findBlock(mapElement.node)
            if (!array.contains(block)) {
                array.add(block)
            }
        }
        return array
    }

    @JvmStatic
	fun removeMathInSelectedRange(m: Manager) {
        val timmies = getAllTextMapElementsInSelectedRange(m)
        val blocks = ArrayList<Node>()
        for (mapElement in timmies) {
            if (mapElement !is MathMLElement) {
                continue
            }
            if (mapElement.node.document == null) {
                continue
            }
            val block: ParentNode = BBXUtils.findBlock(mapElement.node)
            if (!blocks.contains(block)) {
                blocks.add(block)
            }
            val index = BBXUtils.getIndexInBlock(mapElement.node)
            val parent = block.getChild(index)
            val mathText = mapElement.text
            val textNode = Text(mathText)
            block.replaceChild(parent, textNode)
        }
        if (blocks.isNotEmpty()) {
            m.simpleManager.dispatchEvent(ModifyEvent(Sender.MATH, blocks, true))
        }
    }

    fun deleteMathInSelectedRange(m: Manager) {
        val timmies = getAllTextMapElementsInSelectedRange(m)
        val blocks: MutableList<Node> = ArrayList()
        for (mapElement in timmies) {
            if (mapElement !is MathMLElement) {
                continue
            }
            if (mapElement.node.document == null) {
                continue
            }
            val block: Node = BBXUtils.findBlock(mapElement.node)
            if (!blocks.contains(block)) {
                blocks.add(block)
            }
            mapElement.getNodeParent().detach()
        }
        if (blocks.isNotEmpty()) {
            m.simpleManager.dispatchEvent(ModifyEvent(Sender.MATH, blocks, true))
        }
    }

    @JvmStatic
	fun deleteMathFromSelectionHandlerEvent(m: Manager, mapElement: MathMLElement, start: Int, end: Int) {
        var end = end
        val block: ParentNode = BBXUtils.findBlock(mapElement.node)
        var index = BBXUtils.getIndexInBlock(mapElement.node)
        val s = mapElement.text
        end -= mapElement.getStart(m.mapList)
        val offset = if (start > mapElement.getStart(m.mapList)) start - mapElement.getStart(m.mapList) else 0
        val mathEnd = offset + mapElement.text.length
        val one = StringBuilder()
        val two = StringBuilder()
        for (i in s.indices) {
            if (i < offset) {
                one.append(s[i])
            } else if (i in (end + 1) until mathEnd) {
                two.append(s[i])
            }
        }
        block.getChild(index).detach()
        m.mapList.remove(mapElement)
        if (one.isNotEmpty()) {
            wrapInMath(one.toString(), block, index)
            index++
        }
        if (two.isNotEmpty()) {
            wrapInMath(two.toString(), block, index)
        }
    }

    fun previous(startNode: Node?, m: Manager): Node? {
        var startNode = startNode
        if (startNode == null || startNode.document == null) {
            val tme = m.mapList.current
            startNode = if (tme is WhiteSpaceElement) {
                m.mapList.getCurrentNonWhitespace(m.mapList.currentIndex).node
            } else {
                tme.node
            }
        }
        val nodes = startNode!!.query("preceding::*[local-name()='math']")
        return if (nodes.size() > 0) nodes[nodes.size() - 1] else null
    }

    fun next(startNode: Node?, m: Manager): Node? {
        var startNode = startNode
        if (startNode == null || startNode.document == null) {
            val tme = m.mapList.current
            startNode = if (tme is WhiteSpaceElement) {
                m.mapList.getCurrentNonWhitespace(m.mapList.currentIndex).node
            } else {
                tme.node
            }
        }
        val nodes = startNode!!.query("following::*[local-name()='math' ]")
        return if (nodes.size() > 0) nodes[0] else null
    }

    fun getImageFromMMLDoc(exampleName: String): Image? {
        val file = BBIni.programDataPath.resolve(Paths.get("settings", "MathMLExamples.xml")).toFile()
        try {
            val doc = Builder().build(file)
            val nodes = doc.query("//*[local-name()='math' and @example='$exampleName']")
            if (nodes.size() > 0) {
                return ImageCreator.createImage(Display.getCurrent(), nodes[0] as Element, 30)
            }
        } catch (e: ParsingException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * @param bbNode
     * @return MathML Node, detached from any document
     */
    fun removeAllBBMarkupFromMathML(bbNode: Node): Node? {
        var mathNode: Node? = null
        if (MathModule.isMath(bbNode)) {
            UTDHelper.stripUTDRecursive(bbNode as ParentNode)
            mathNode = bbNode.copy()
            removeAltText(mathNode)
        }
        return mathNode
    }

    fun queryForMathNodes(root: Node?): Nodes {
        return if (root == null) {
            Nodes()
        } else root.query("//*[local-name()='math']")
    }

    fun removeAltText(mathNode: Node?) {
        if (mathNode == null) {
            return
        }
        if (mathNode !is Element) {
            return
        }
        if (mathNode.getAttributeValue("alttext") != null) {
            mathNode.removeAttribute(mathNode.getAttribute("alttext"))
        }
    }

    class MixedMathNonMathString {
        constructor()
        constructor(math: Boolean, text: String) {
            this.math = math
            this.text = text
        }

        var math = false
        var text = ""
    }
}