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
package org.brailleblaster.perspectives.braille.views.wp

import nu.xom.Element
import nu.xom.Node
import nu.xom.ParentNode
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBXUtils
import org.brailleblaster.math.ascii.ASCII2MathML
import org.brailleblaster.math.mathml.*
import org.brailleblaster.perspectives.braille.mapping.elements.LineBreakElement
import org.brailleblaster.perspectives.braille.mapping.elements.TabTextMapElement
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.XMLNodeCaret.CursorPosition
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.modules.misc.TableSelectionModule
import org.brailleblaster.perspectives.mvc.modules.views.TextViewModule
import org.brailleblaster.utd.internal.xml.XMLHandler2
import org.brailleblaster.utd.utils.UTDHelper
import org.brailleblaster.util.Notify
import org.brailleblaster.util.Utils
import org.brailleblaster.wordprocessor.WPManager
import org.slf4j.LoggerFactory

object MathEditHandler {
  private val log = LoggerFactory.getLogger(MathEditHandler::class.java)

  /**
   * Make selected text from the print view into math.
   *
   */
	fun makeMathFromTextViewSelection() {
    val m = WPManager.getInstance().controller
    log.debug("Make math from text view selection")
    val mapElements = m.mapList.getElementsOneByOne(
      m.textView.selection.x,
      m.textView.selection.y
    )
    // check to make sure we aren't inserting new math into a toc or
    // table
    if (mapElements.any { it.isReadOnly }) {
      TableSelectionModule.displayInvalidTableMessage(m.wpManager.shell)
      return
    } else if (mapElements.any { BBXUtils.isTOCText(it.node) }) {
      Notify.notify("Cannot insert math into TOC", Notify.ALERT_SHELL_NAME)
      return
    }
    val array = mapElements.toList()
    val arrayWithStrings = array.map { it to m.textView.text.substring(it.getStart(m.mapList), it.getEnd(m.mapList)) }
    val selectionStart = m.textView.selection.x
    val selectionEnd = m.textView.selection.y
    val blocks = MathUtils.getBlocksFromTextMapElements(array)
    for (block in blocks) {
      var index = 0
      val actions = mutableListOf<MathAction>()
      val toDetach = mutableListOf<Node>()
      val textNodes = block.query("descendant::text()")
      for ((k, s) in arrayWithStrings) {
        if (k is TabTextMapElement) {
          // remove tabs
          toDetach.add(k.node)
        } else if (k is MathMLElement) {
          actions.add(MathAction(MathSubject(k.node), MathVerb.Verb.MakeMath))
          toDetach.add(k.node)
        }
        for (j in textNodes) {
          if (k.node === j && k !is MathMLElement) {
            index = BBXUtils.getIndexInBlock(k.node)
            val start = k.getStart(m.mapList)
            val startOffset = if (selectionStart - start > 0) selectionStart - start else 0
            val endOffset = if (selectionEnd - start < s.length) selectionEnd - start else s.length
            actions.addAll(MathUtils.wrapInMath(startOffset, endOffset, s))
            toDetach.add(j)
          }
        }
      }
      MathAction.execute(actions, block, index)
      for (l in toDetach.indices) {
        toDetach[l].detach()
      }
      m.simpleManager.dispatchEvent(ModifyEvent(Sender.MATH, true, block))
    }
  }

  /**
   * Insert new math
   *
   */
	fun insertNew(o: MathSubject) {
    val m = WPManager.getInstance().controller
    val mapElement = m.mapList.current
    if (!MathModule.isMath(mapElement.node)) {
      // check to make sure we aren't inserting new math into a toc or table
      if (mapElement.isReadOnly) {
        TableSelectionModule.displayInvalidTableMessage(m.wpManager.shell)
        return
      }
      // insert new
      val block: Node
      val blockParent: ParentNode?
      var index: Int
      val offset: Int
      val newBlockIndex: Int
      val before: String
      val after: String
      if (mapElement.node == null || mapElement is LineBreakElement) {
        // trying to insert in whitespace, create a new block and insert
        // it right after this block in the block's child order
        if (m.simpleManager.currentCaret.cursorPosition == CursorPosition.BEFORE) {
          val previousTme = m.mapList.findPreviousNonWhitespace(m.mapList.indexOf(mapElement))
          if (previousTme != null) {
            val previousBlock: Node = BBXUtils.findBlock(previousTme.node)
            blockParent = previousBlock.parent
            newBlockIndex = blockParent.indexOf(previousBlock)
            block = BBX.BLOCK.DEFAULT.create()
            Utils.insertChildCountSafe(blockParent, block, newBlockIndex)
          } else {
            blockParent = BBXUtils.findBlock(m.simpleManager.currentCaret.node).parent
            block = BBX.BLOCK.DEFAULT.create()
            Utils.insertChildCountSafe(blockParent, block, 0)
          }
        } else {
          val nextTme = m.mapList.findNextNonWhitespace(m.mapList.indexOf(mapElement))
          if (nextTme != null) {
            val nextBlock: Node = BBXUtils.findBlock(nextTme.node)
            blockParent = nextBlock.parent
            newBlockIndex = blockParent.indexOf(nextBlock)
            block = BBX.BLOCK.DEFAULT.create()
            Utils.insertChildCountSafe(blockParent, block, newBlockIndex)
          } else {
            blockParent = BBXUtils.findBlock(m.simpleManager.currentCaret.node)
            val indexOfBlock = blockParent.parent.indexOf(blockParent)
            block = BBX.BLOCK.DEFAULT.create()
            //Yes, add 2 and not 1
            Utils.insertChildCountSafe(blockParent, block, indexOfBlock + 2)
          }
        }
        index = 0
        after = ""
        before = after
      } else if (BBXUtils.isTOCText(mapElement.node)) {
        Notify.notify("Cannot insert math into TOC", Notify.ALERT_SHELL_NAME)
        return
      } else {
        block = BBXUtils.findBlock(mapElement.node)
        index = BBXUtils.getIndexInBlock(mapElement.node)
        if (mapElement is MathMLElement) {
          // cursor is not in math, but the closest mapElement is math
          after = ""
          before = after
          // is our cursor before or after the node?
          if (m.textView.caretOffset >= mapElement.getEnd(m.mapList)) {
            index++
          }
        } else {
          // cursor is not in math, and the closest mapElement is a
          // text node we may have to split
          val length = mapElement.text.length
          offset =
            if (m.textView.caretOffset - mapElement.getStart(m.mapList) < length) m.textView.caretOffset - mapElement.getStart(
              m.mapList
            ) else length
          val s = m.textView.text.substring(
            mapElement.getStart(m.mapList),
            mapElement.getEnd(m.mapList)
          )
          before = s.take(offset).replace("\n".toRegex(), "").replace("\r".toRegex(), "")
          after = s.substring(offset).replace("\n".toRegex(), "").replace("\r".toRegex(), "")
          mapElement.node.detach()
        }
      }
      val array = MathUtils.wrapInMath(before, o, after)
      MathAction.execute(array, block, index)
      m.simpleManager.dispatchEvent(ModifyEvent(Sender.MATH, true, block))
      var translatedText = o.string
      if ((o.node as Element).getAttributeValue("alttext") != null) {
        translatedText = MathModule.getMathText(o.node)
      }
      TextViewModule.setCursorAfterInsert(m, translatedText.length, Sender.MATH)
    } else {
      // edit existing
      val text = o.string
      // account for the cursor being in whitespace before or after the element
      val offset = m.textView.caretOffset
      val mapStart = mapElement.getStart(m.mapList)
      val mapElementText = mapElement.text
      var index = offset - mapStart
      if (index < 0) {
        index = 0
      } else if (index > mapElementText.length) {
        index = mapElementText.length
      }
      val firstHalf = mapElementText.take(index)
      val lastHalf = mapElementText.substring(index)
      addMathTextToExistingMathText(firstHalf + text + lastHalf)
    }
  }

  /**
   *
   */
  private fun addMathTextToExistingMathText(text: String): MutableList<Node> {
    val m = WPManager.getInstance().controller
    if (text.isEmpty()) {
      return ArrayList()
    }
    val mapElement = m.mapList.current // the math
    // node
    val block: ParentNode
    if (mapElement.node is ParentNode) {
      UTDHelper.stripUTDRecursive(mapElement.node as ParentNode)
    }
    val mathNode = ASCII2MathML.translate(text)
    val parent: Node = mapElement.nodeParent
    (parent as ParentNode).replaceChild(mapElement.node, mathNode)
    block = BBXUtils.findBlock(mathNode)
    m.simpleManager.dispatchEvent(ModifyEvent(Sender.MATH, true, parent))
    val array = ArrayList<Node>()
    array.add(block)
    return array
  }

	fun translateAndReplaceAtCursor(o: MathSubject) {
    translateAndReplace(o, WPManager.getInstance().controller.mapList.current.node)
  }

	fun translateAndReplace(o: MathSubject, mathNode: Node) {
    val m = WPManager.getInstance().controller
    val block: ParentNode = BBXUtils.findBlock(mathNode)
    val index = BBXUtils.getIndexInBlock(mathNode)
    val math = o.node
    val parent = block.getChild(index)
    block.replaceChild(parent, math)
        XMLHandler2.wrapNodeWithElement(
            math,
            BBX.INLINE.MATHML.create()
        )
    m.simpleManager.dispatchEvent(ModifyEvent(Sender.MATH, true, block))
  }

  fun replaceMathAtCursor(o: MathSubject) {
    val m = WPManager.getInstance().controller
    if (MathModule.isMath(m.mapList.current.node)) {
      translateAndReplaceAtCursor(o)
    } else {
      insertNew(o)
    }
  }
}