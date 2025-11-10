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
package org.brailleblaster.perspectives.mvc.modules.views

import com.google.common.base.Suppliers
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.XMLNodeCaret
import org.brailleblaster.perspectives.mvc.XMLNodeCaret.CursorPosition
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utils.xml.BB_NS
import org.brailleblaster.utils.xml.UTD_NS
import org.eclipse.swt.custom.StyledText
import org.slf4j.LoggerFactory
import java.util.function.Supplier

class TextViewModule(private val manager: Manager) : AbstractModule(), SimpleListener {
    init {
        sender = Sender.TEXT
    }

    override fun onEvent(event: SimpleEvent) {
        if (event is ModifyEvent) {
            manager.text.resetSelectionObject()
        } else if (event is XMLCaretEvent) {
            if (event.isSingleNode) {
                if (event.start is XMLTextCaret) {
                    val caret = event.start
                    val tme = getTMEOfTextNode(caret.node)

                    //old current section based
                    //			MapList mapList = manager.getMapList();
                    //			int tmeIndex = mapList.findNodeIndex(caret.node(), 0);
                    //			TextMapElement tme = mapList.get(tmeIndex);
                    manager.checkView(tme)
                    if (!tme.isFullyVisible) {
                        val atEnd = tme.getStart(manager.mapList) > 0
                        val section = manager.getSection(tme)
                        if (atEnd && manager.sectionList.size > section + 1) {
                            manager.buffer(section + 1)
                        } else if (section > 0) {
                            manager.buffer(section - 1)
                        }
                    }
                    manager.mapList.setCurrent(
                        manager.mapList.indexOf(tme)
                    )
                    if (event.sender != Sender.TEXT) {
                        manager.text.positionFromStart = 0
                        manager.text.cursorOffset = 0
                        log.debug("positionFromStart " + manager.text.positionFromStart + " cursorOfset" + manager.text.cursorOffset + " viewOffset " + manager.textView.caretOffset)
                        manager.text.updateCursor(caret.offset)
                        if (!currentElementOnScreen(manager.textView, manager.textView.caretOffset)) {
                            manager.textView.topIndex = manager.textView.getLineAtOffset(manager.textView.caretOffset)
                        }
                    }
                } else {
                    val node = event.start.node as Element
                    //Find usable text node
                    val tableParent = Supplier {
                        Suppliers.memoize { Manager.getTableParent(node) }
                            .get()
                    }
                    val descendants = Supplier {
                        Suppliers.memoize { FastXPath.descendantOrSelf(node).toList() }
                            .get()
                    }
                    var tmeStart = getTMEFirst(node, tableParent, descendants)
                    // RT 7214: Must buffer view before scrolling to it - moved to here for RT 7388
                    manager.checkView(tmeStart)
                    val tmeStartIndex = manager.mapList.indexOf(tmeStart)
                    var tmeEnd = getTMELast(node, tableParent, descendants)
                    val tmeEndIndex = manager.mapList.indexOf(tmeEnd)
                    tmeStart = manager.mapList[tmeStartIndex]
                    tmeEnd = manager.mapList[tmeEndIndex]

                    //TODO: Fake selection
                    manager.text.setListenerLock(true)
                    log.trace("tmeStart {} {} tmeEnd {} {}", tmeStartIndex, tmeStart, tmeEndIndex, tmeEnd)
                    log.trace(
                        "Set selection from tme node {} start {} end {} to tme node {} start {} end {}",
                        tmeStart.node,
                        tmeStart.getStart(manager.mapList),
                        tmeStart.getEnd(manager.mapList),
                        tmeEnd.node,
                        tmeEnd.getStart(manager.mapList),
                        tmeEnd.getEnd(manager.mapList)
                    )
                    var actualStart = tmeStart.getStart(manager.mapList)
                    var actualEnd = tmeEnd.getEnd(manager.mapList)
                    log.trace("Actually selecting {}-{}", actualStart, actualEnd)
                    if (event.sender == Sender.BREADCRUMBS || event.sender == Sender.IMAGE_DESCRIBER) {
                        if (actualStart == -1) {
                            /*
                            tme is most likely in previous loaded section
                            this is just a visual indicator, right? as long as the XMLCaret is correct it should be fine
                            */
                            actualStart = 0
                        }
                        if (actualEnd == -1) {
                            /*
                            tme is most likely in previous loaded section
                            this is just a visual indicator, right? as long as the XMLCaret is correct it should be fine
                            */
                            actualEnd = manager.textView.charCount - actualStart
                        }
                        manager.text.setCurrentSelection(actualStart, actualEnd)
                    } else {
                        manager.textView.caretOffset = actualStart
                        if (!currentElementOnScreen(manager.textView, manager.textView.caretOffset)) {
                            manager.textView.topIndex = manager.textView.getLineAtOffset(manager.textView.caretOffset)
                        }
                    }
                    manager.text.setListenerLock(false)
                    return
                }
            } else {
                //Find usable text node
                //Fix for #5861: Direct translate has a 
                //different breadcrumb if inside emphasized
                //text and can cause an exception
                val tv = event.manager.manager.text
                val actualStart = tv.currentStart
                val actualEnd = tv.currentEnd
                log.trace("Actually selecting {}-{}", actualStart, actualEnd)
                manager.textView.setSelection(actualStart, actualEnd)
                manager.text.setListenerLock(false)
            }
        }
    }

    private fun getTMEFirst(
        node: Node,
        tableParent: Supplier<Element>,
        descendants: Supplier<List<Node>>
    ): TextMapElement {
        val preFilter = getTMEPreFilter(node, tableParent)
        if (preFilter != null) {
            return preFilter
        } else {
            for (curDescendant in descendants.get()) {
                if (XMLHandler.ancestorElementIs(
                        curDescendant
                    ) { curAncestor: Element -> curAncestor.namespaceURI == UTD_NS }
                ) {
                    continue
                }
                var currentSection = manager
                    .getSection(manager.mapList.getClosest(manager.textView.caretOffset, true))
                if (currentSection < 0) {
                    currentSection = 0
                }
                for (i in manager.sectionList.indices) {
                    val list = if (manager.isEmptyDocument) manager.mapList else manager.sectionList[i].list
                    for (j in list.indices) {
                        val curTME = list[j]
                        if (curTME.node === curDescendant) {
                            if (currentSection != i) {
                                manager.resetSection(i)
                            }
                            return curTME
                        }
                    }
                }
            }
        }
        throw NodeException("No usable TME found", node)
    }

    private fun getTMELast(
        node: Node,
        tableParent: Supplier<Element>,
        descendants: Supplier<List<Node>>
    ): TextMapElement {
        val preFilter = getTMEPreFilter(node, tableParent)
        if (preFilter != null) {
            return preFilter
        } else {
            for (curDescendant in descendants.get().reversed()) {
                if (XMLHandler.ancestorElementIs(
                        curDescendant
                    ) { curAncestor: Element -> curAncestor.namespaceURI == UTD_NS }
                ) {
                    continue
                }
                var currentSection = manager
                    .getSection(manager.mapList.getClosest(manager.textView.caretOffset, true))
                if (currentSection < 0) {
                    currentSection = 0
                }
                for (i in manager.sectionList.indices) {
                    val list = if (manager.isEmptyDocument) manager.mapList else manager.sectionList[i].list
                    for (j in list.indices) {
                        val curTME = list[j]
                        if (curTME.node === curDescendant) {
                            if (currentSection != i) {
                                manager.resetSection(i)
                            }
                            return curTME
                        }
                    }
                }
            }
        }
        throw NodeException("No usable TME found", node)
    }

    private fun getTMEPreFilter(inputNode: Node, tableParent: Supplier<Element>): TextMapElement? {
        var node = inputNode
        if (node is Text) {
            return getTMEOfTextNode(node)
        } else if (BBX.CONTAINER.TABLE.isA(node)) {
            node = tableParent.get()
            for (curTME in manager.mapList) {
                if (curTME.nodeParent === node) {
                    return curTME
                }
            }
        } else if (node is Element && node.namespaceURI != BB_NS) {
            // Accept utd linebreak elements
            return getTMEOfElement(node)
        }
        return null
    }

    private fun getTMEOfTextNode(text: Text): TextMapElement {
        //TODO: Speed of always searching from start
        //check if in list, if not find and update views
        var tme = manager.mapList.findNode(text)
        if (tme == null) {
            //Multithreaded search of all other non-visible sections
            var result: Pair<Int, Int> =
                manager.viewInitializer.findSection(text) ?: throw NodeException("Node is not in maplist", text)
            //TODO: Used to throw NPE, this is at least more useful
            //If still formatting wait for formatting and find TME again
            //TODO: What if this section has already been formatted?
            if (!manager.viewInitializer.sectionList[result.first].isVisible && manager.needsMapListUpdate()) {
                manager.waitForFormatting(true)
                result = manager.viewInitializer.findSection(text)  ?: throw NodeException("Node is not in maplist", text)
            }
            tme = manager.viewInitializer.sectionList[result.first].list[result.second]
        }
        return tme
    }

    private fun getTMEOfElement(element: Element): TextMapElement? {
        return manager.mapList.findNode(element)
    }

    private fun currentElementOnScreen(view: StyledText, pos: Int): Boolean {
        val viewHeight = view.clientArea.height
        val lineHeight = view.lineHeight
        val totalLines = viewHeight / lineHeight
        val currentLine = view.getLineAtOffset(pos)
        val topIndex = view.topIndex
        return (currentLine >= topIndex
                && currentLine <= topIndex + totalLines - 1)
    }

    companion object {
        private val log = LoggerFactory.getLogger(TextViewModule::class.java)

        @JvmStatic
        fun getAllTextMapElementsInSelectedRange(m: Manager): Array<TextMapElement> {
            val allElements = m.getAllTextMapElementsInRange(
                m.textView.selection.x, m.textView.selection.y
            )
            return allElements.toTypedArray()
        }

        fun setCursorAfterInsert(m: Manager, numberCharsAdded: Int, sender: Sender) {
            val cursorPos = m.textView.caretOffset
            val mapElement = m.mapList.getClosest(cursorPos + numberCharsAdded, true)
            if (mapElement.node is Text) {
                m.simpleManager.dispatchEvent(
                    XMLCaretEvent(
                        sender,
                        XMLTextCaret(mapElement.node as Text, cursorPos + numberCharsAdded)
                    )
                )
            } else {
                m.simpleManager.dispatchEvent(
                    XMLCaretEvent(
                        sender,
                        XMLNodeCaret(mapElement.node, CursorPosition.AFTER)
                    )
                )
            }
            m.text.setCurrentElement(cursorPos + numberCharsAdded)
            //		m.getText().update(true);
            //		m.getTextView().setCaretOffset(cursorPos + numberCharsAdded);
            //		m.getMapList().setCurrent(m.getMapList().indexOf(m.getMapList().getClosest(cursorPos+ numberCharsAdded, true)));
        }
    }
}