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
package org.brailleblaster.perspectives.braille.document

import nu.xom.*
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBX.SubType
import org.brailleblaster.bbx.BBXUtils
import org.brailleblaster.bbx.findBlockOrNull
import org.brailleblaster.document.BBDocument
import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.math.mathml.MathSubject
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.BoxLineTextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.PageIndicatorTextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.WhiteSpaceElement
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.perspectives.braille.messages.RemoveNodeMessage
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.views.wp.MathEditHandler.translateAndReplaceAtCursor
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.Style
import org.brailleblaster.utd.UTDTranslationEngineCallback
import org.brailleblaster.utd.actions.GenericAction
import org.brailleblaster.utd.actions.IAction
import org.brailleblaster.utd.actions.RemoveLineAttributeAction
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.util.WhitespaceUtils.removeLineBreakElements
import org.slf4j.LoggerFactory

class BrailleDocument(dm: Manager, doc: Document) : BBDocument(dm, doc) {
    init {
        engine.callback = object : UTDTranslationEngineCallback() {
            override fun onUpdateNode(n: Node) {
                // //Reduce unnessesary logging
                // if (n instanceof Document)
                // log.debug("onUpdateNode: Whole XML document");
                // else if (n.equals(n.getDocument().getRootElement()))
                // log.debug("onUpdateNode: Root Element of XML document");
                // else
                // log.debug("onUpdateNode: "+ n.toXML());
                // //Note: TOC and some volume code for right now depends on
                // this being a complete refresh
                // dm.refreshFormat();
            }

            override fun onFormatComplete(root: Node) {}
        }
    }

    /**
     * Updates a node when it has been edited
     *
     */
    fun updateNode(manager: Manager, list: MapList, text: String) {
        var text = text
        manager.stopFormatting()
        text = text.replace("\n", "").replace("\r", "")
        val currentNode = list.current.node
            ?: throw NullPointerException("CurrentNode is null. TME: " + list.current)
        val block = engine.findTranslationBlock(currentNode)
        findAndRemoveBrailleElement(block as Element)
        if (currentNode is Element && MathModule.isMath(currentNode)) {
            log.debug("Insert new into math from Braille Document")
            translateAndReplaceAtCursor(MathSubject(text))
            return
        }
        if (!list.current.isFullyVisible) {
            text = if (list.current.getStart(list) == 0) {
                list.current.invisibleText + text
            } else {
                text + list.current.invisibleText
            }
        }
        var parentNode = if (currentNode is Text) currentNode.parent else currentNode
        changeTextNode(currentNode, text)
        val parent = BBXUtils.cleanupBlock(parentNode)
        // If last text node of document, remove line break elements between
        // this and the previous
        // element
        if (parentNode.document == null && list.getPrevious(true) != null) {
            var lastTextTME = true
            for (index in list.currentIndex + 1 until list.size) {
                if (list[index] !is WhiteSpaceElement) {
                    lastTextTME = false
                    break
                }
            }
            if (lastTextTME) {
                removeLineBreakElements(list, list.indexOf(list.getPrevious(true)), list.size - 1)
            }
        }
        if (parent != null) {
            parentNode = parent
        }
        manager.simpleManager.dispatchEvent(ModifyEvent(Sender.SIMPLEMANAGER, true, parentNode))
    }

    /**
     * Inserts an Element into the DOM. This is used for inserting transcribers
     * notes
     */
    fun insertElement(current: TextMapElement, offset: Int, subType: SubType, atStart: Boolean): Element {
        var offset = offset
        val p = makeElement(subType)
        p.appendChild(Text(""))
        var parent =
            if (current is BoxLineTextMapElement) current.nodeParent else (engine.findTranslationBlock(current.node) as Element)
        if (isEmptyPlaceholder(parent)) {
            parent.parent.replaceChild(parent, p)
        } else {
            var nodeIndex: Int
            if (current is BoxLineTextMapElement) {
                if (current.nodeParent == current.nodeParent) {
                    if (atStart) {
                        parent = parent.parent as Element
                        nodeIndex = parent.indexOf(current.nodeParent)
                        offset = 0
                    } else nodeIndex = 0
                } else {
                    parent = current.nodeParent
                    nodeIndex = parent.indexOf(current.node)
                }
            } else if (current is PageIndicatorTextMapElement) {
                parent = parent.parent as Element
                nodeIndex = parent.indexOf(current.nodeParent)
            } else if (engine.actionMap.findValueOrDefault(parent) is GenericAction
                || engine.actionMap.findValueOrDefault(parent) is RemoveLineAttributeAction
            ) {
                nodeIndex = parent.parent.indexOf(parent)
                parent = parent.parent as Element
            } else {
                while (engine.actionMap.findValueOrDefault(parent) !is GenericAction) {
                    nodeIndex = parent.parent.indexOf(parent)
                    parent = parent.parent as Element
                }
                nodeIndex = parent.parent.indexOf(parent)
                parent = parent.parent as Element
            }
            parent.insertChild(p, nodeIndex + offset)
        }
        return p
    }

    fun insertElement(parent: Element, insertIndex: Int, text: String?): Element {
        val child = BBX.BLOCK.STYLE.create("Body Text")
        child.appendChild(Text(text))
        parent.insertChild(child, insertIndex)
        return child
    }
    /*
	 * private Element appendBRLNode(Element e){ Element brl =
	 * UTDElements.BRL.create(); brl.appendChild(new Text(""));
	 * e.appendChild(brl); addNamespace(brl);
	 * 
	 * return brl; }
	 */
    /**
     * Updates the text of a given text node prior to translation
     *
     * @param n
     * : Text node to update
     * @param text
     * : String containing new text for the node
     */
    private fun changeTextNode(n: Node, text: String) {
        if (n is Text) {
            if (text.isEmpty()) {
                n.detach()
                return
            }
            // !!!If it's am empty text node and there's a page beside it, make
            // sure to
            // assign this new node after the page
            if (n.value.isEmpty()) {
                val parent = n.parent
                val index = parent.indexOf(n)
                if (parent.childCount - index > 1 && BBX.SPAN.PAGE_NUM.isA(parent.getChild(index + 1))) {
                    val copy = parent.getChild(index + 1).copy()
                    parent.insertChild(copy, index)
                    parent.getChild(index + 2).detach()
                }
            }
            n.value = text
        }
    }

    fun removeNode(list: MapList, message: RemoveNodeMessage) {
        if (message.contains("element")) removeElement(message) else removeNode(list[message.index])
    }

    private fun removeElement(m: RemoveNodeMessage) {
        val e = m.getValue("element") as Element
        e.parent.removeChild(e)
    }

    /**
     * Removes a node from the DOM, checks whether other children exists, if not
     * the entire element is removed
     */
    private fun removeNode(t: TextMapElement) {
        val e = t.brailleList.firstOrNull()?.nodeParent
        if (e != null && e !== t.node) t.nodeParent.removeChild(e)
        try {
            t.nodeParent.removeChild(t.node)
        }
        catch (_: NoSuchChildException){
            //This fixes bug #43207
        }
    }

    /**
     * Translates text contents of an element and children
     *
     * @param e
     * : Element to translate
     * @return translated element
     */
    fun translateElement(e: Element): Element {
        removeBraille(e)
        val nodes = engine.translate(e)
        return nodes[0] as Element
    }

    /**
     * Recursively removes braille from an element and its children
     *
     * @param e
     * : Element to remove braille
     */
    private fun removeBraille(e: Element) {
        val els = e.childElements
        for (i in 0 until els.size()) {
            if (UTDElements.BRL.isA(els[i])) {
                e.removeChild(els[i])
            } else removeBraille(els[i])
        }
    }

    /**
     * Updates an entry in a document's semantic action file if an entry exists
     */
    fun changeStyle(style: IStyle?, e: Element?) {
        utdManager.applyStyle(style as Style?, e!!)
    }

    fun changeAction(action: IAction?, e: Element?) {
        utdManager.applyAction(action, e!!)
    }

    fun removeAction(e: Element?) {
        utdManager.removeOverrideAction(e!!)
    }

    /**
     * Translates only the specified element and it's children Calling classes
     * should call reformat if required
     *
     * @param e
     * Element to translate
     * @return translated Element
     */
    fun retranslateElement(e: Element): Element {
        removeBraille(e)
        val parent = e.parent as Element
        val translatedElement = engine.translate(e)[0] as Element
        parent.replaceChild(e, translatedElement)
        // TODO: make sure calling classes call reformat after performing any
        // other necessary operations
        // utdManager.reformat(translatedElement);
        return translatedElement
    }

    fun getParent(n: Node): Element {
        return n.parent.findBlockOrNull() ?: return engine.findTranslationBlock(n) as Element
    }

    /**
     * Helper methods for methods that update text in the DOM during text
     * editing
     *
     * @param element
     * :Element to search
     * @return BRL element if found, null if it does not exist
     */
    fun findAndRemoveBrailleElement(element: Element) {
        val els = element.childElements
        for (i in 0 until els.size()) {
            if (UTDElements.BRL.isA(els[i])) removeElement(els[i]) else findAndRemoveBrailleElement(els[i])
        }
    }

    private fun removeElement(e: Element) {
        e.parent.removeChild(e)
    }

    fun createDontSplitSpan(parents: ArrayList<Element>): Element {
        val span = BBX.CONTAINER.DONT_SPLIT.create()
        span.addAttribute(Attribute("class", "dontsplit"))
        val grandparent = parents[0].parent as Element
        val grandParentIndex = grandparent.indexOf(parents[0])
        val elementsToInsert: MutableList<Element> = ArrayList()
        for (parent in parents) {
            if (parent.document != null) { // element is attached
                // to document
                parent.detach() // detach it and its children from
                // document
                elementsToInsert.add(parent)
            }
        }
        for (element in elementsToInsert) {
            span.appendChild(element)
        }
        grandparent.insertChild(span, grandParentIndex)
        return span
    }

    fun mergeElements(originalParent: Element, child: Element): Element {
        val parent = originalParent.copy()
        removeBraille(parent)
        removeBraille(child)
        while (child.childCount > 0) {
            if (parent.getChild(parent.childCount - 1) is Text && child.getChild(0) is Text) {
                (parent.getChild(parent.childCount - 1) as Text).value =
                    parent.getChild(parent.childCount - 1).value + child.getChild(0).value
                child.removeChild(0)
            } else if (child.getChild(0) is Element && BBX.SPAN.DEFINITION_TERM.isA(child.getChild(0))) {
                val gw = child.getChild(0).getChild(0).copy()
                child.getChild(0).detach()
                parent.appendChild(gw)
            } else {
                val e1 = parent.getChild(parent.childCount - 1)
                val e2 = child.getChild(0)
                if (e1 is Element
                    && e2 is Element
                ) {

                    var merged = false
                    if (engine.actionMap.findValueOrDefault(e1) !is GenericAction
                        && engine.actionMap.findValueOrDefault(e2) !is GenericAction
                    ) {
                        // Below if condition was unclear if it should match class/type or test instances for equality.
                        // MWhapples: Opting for instance equality as previously would never evaluate as true and equality is more restrictive than type equality.
                        // Therefore this should not give false positives.
                        // Should others know otherwise, change it to class/type equality.
                        if (engine.actionMap.findValueOrDefault(e1)
                            == engine.actionMap.findValueOrDefault(e2)
                        ) {
                            if (e1.childCount > 0 && e1.getChild(0) is Text && e2.childCount > 0 && e2.getChild(0) is Text) {
                                (e1.getChild(0) as Text).value = e1.getChild(0).value + e2.getChild(0).value
                                val els = e2.childElements
                                for (i in 0 until els.size()) e1.appendChild(e2.removeChild(els[0]))
                                e2.parent.removeChild(e2)
                                merged = true
                            }
                        }
                    }
                    if (!merged) {
                        parent.appendChild(child.removeChild(0))
                    }
                } else {
                    parent.appendChild(child.removeChild(0))
                }
            }
        }

        //If there are skip lines before the child, remove those too
        //Otherwise, you'll have skip lines after the new element
        val childParent = child.parent as Element
        val position = childParent.indexOf(child)
        while (position > 0 && UTDElements.NEW_LINE.isA(childParent.getChild(position - 1))) {
            childParent.getChild(position - 1).detach()
        }
        child.detach()
        originalParent.parent.replaceChild(originalParent, parent)
        return parent
    }

    companion object {
        private val log = LoggerFactory.getLogger(BrailleDocument::class.java)

        @JvmStatic
        fun isEmptyPlaceholder(e: Element): Boolean {
            return if (BBX.BLOCK.ATTRIB_BLANKDOC_PLACEHOLDER.has(e)) {
                e.value.isEmpty()
            } else false
        }
    }
}
