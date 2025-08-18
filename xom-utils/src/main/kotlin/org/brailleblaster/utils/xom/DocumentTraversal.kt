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
package org.brailleblaster.utils.xom

import com.google.common.base.Preconditions
import nu.xom.Comment
import nu.xom.DocType
import nu.xom.Document
import nu.xom.Element
import nu.xom.NoSuchChildException
import nu.xom.Node
import nu.xom.ProcessingInstruction
import nu.xom.Text
import java.util.Deque
import java.util.LinkedList

/**
 * Traverse a XML document
 *
 *
 * This will traverse an XML document whilst calling callback methods upon certain events. There
 * may be similarities to SAX, however the event callbacks still have access to the XOM document
 * model which may reduce the need to hold internal state in the parser.
 *
 * Default implementations of the callbacks are provided, however these are minimal
 * implementations and simply ignore the event.
 */
object DocumentTraversal {
    interface Handler {
        /**
         * Notification of start of an element.
         *
         *
         * When walking the document, when the start of an element is encountered this method will be
         * called. The handler can indicate whether to descend into the element through the return value.
         *
         * @param e The element which is starting.
         * @return True if to descend into the element, false if not.
         */
        fun onStartElement(e: Element): Boolean {
            return true
        }

        /**
         * Notification of the end of an element.
         *
         *
         * This method is called when the end of a element is encountered.
         *
         * @param e The element which is ending.
         */
        fun onEndElement(e: Element) {}

        /**
         * Notification of the start of a document.
         *
         * @param d The document being started.
         */
        fun onStartDocument(d: Document) {}

        /**
         * Notification of the end of a document.
         *
         * @param d The document being ended.
         */
        fun onEndDocument(d: Document) {}

        /**
         * Notification of a text node.
         *
         * @param t The text node which is being encountered.
         */
        fun onText(t: Text) {}

        /**
         * Notifyication of DOCTYPE
         *
         * @param dt The doctype node.
         */
        fun onDocType(dt: DocType) {}

        /**
         * Notification of a processing instruction being encountered.
         *
         * @param pi The process instruction being encountered.
         */
        fun onProcessingInstruction(pi: ProcessingInstruction) {}

        /**
         * Notification of a comment being encountered.
         *
         * @param c The comment being encountered.
         */
        fun onComment(c: Comment) {}

        /**
         * Notification of unknown node type.
         *
         *
         * This method is only called should the node not be of a known type handled by one of the
         * other more specific handler methods.
         *
         * @param n The node.
         */
        fun onUnknownNode(n: Node) {}
    }
    private class PathElement(val element: Element) {
        var curIndex: Int = 0
            set(value) {
                field = Preconditions.checkPositionIndex(value, element.childCount)
            }

        val curChild: Node
            get() {
                if (!hasChildren()) {
                    throw NoSuchChildException("Element is empty")
                }
                return element.getChild(curIndex)
            }

        fun hasChildren(): Boolean {
            return element.childCount > 0
        }
    }

    fun traverseDocument(doc: Document, handler: Handler) {
        requireNotNull(doc.rootElement)
        handler.onStartDocument(doc)
        doc.docType?.let { handler.onDocType(it) }
        val stack: Deque<PathElement> = LinkedList()
        var curPath = PathElement(doc.rootElement)
        while (true) {
            val curElement = curPath.element
            var descend = true
            // Have we just entered the element?
            if (curPath.curIndex == 0) {
                descend = handler.onStartElement(curElement)
                stack.push(curPath)
            }
            // Are we at the end of an element?
            // Alternatively did the handler request not to descend.
            if (curPath.curIndex >= curElement.childCount || !descend) {
                handler.onEndElement(curElement)
                stack.pop()
                curPath = stack.peek() ?: break
            } else {
                when (val curNode = curPath.curChild) {
                    is Element -> {
                        curPath = PathElement(curNode)
                        continue
                    }

                    is Text -> {
                        handler.onText(curNode)
                    }

                    is ProcessingInstruction -> {
                        handler.onProcessingInstruction(curNode)
                    }

                    is Comment -> {
                        handler.onComment(curNode)
                    }

                    else -> {
                        // Unknown node type
                        handler.onUnknownNode(curNode)
                    }
                }
            }
            curPath.curIndex += 1
        }
        handler.onEndDocument(doc)
    }
}