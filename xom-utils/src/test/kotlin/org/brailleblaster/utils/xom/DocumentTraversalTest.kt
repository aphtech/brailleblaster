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

import nu.xom.*
import org.testng.Assert
import org.testng.annotations.Test

class DocumentTraversalTest {
    private class TestHandler : DocumentTraversal.Handler {
        private val sb = StringBuilder()

        val result: String
            get() = sb.toString()

        override fun onStartDocument(d: Document) {
            sb.append("Start of document\n")
        }

        override fun onEndDocument(d: Document) {
            sb.append("End of document\n")
        }

        override fun onStartElement(e: Element): Boolean {
            sb.append("Start of element: ")
            sb.append(e.localName)
            sb.append("\n")
            return "descend" == e.localName
        }

        override fun onEndElement(e: Element) {
            sb.append("End of element: ")
            sb.append(e.localName)
            sb.append("\n")
        }

        override fun onComment(c: Comment) {
            sb.append("On comment: ")
            sb.append(c.value)
            sb.append("\n")
        }

        override fun onProcessingInstruction(pi: ProcessingInstruction) {
            sb.append("On processing instruction: ")
            sb.append(pi.target)
            sb.append("\n")
        }

        override fun onText(t: Text) {
            sb.append("On text: ")
            sb.append(t.value)
            sb.append("\n")
        }

        override fun onUnknownNode(n: Node) {
            sb.append("Unknown node")
        }
    }

    @Test
    fun testTraverseDocument() {
        val root = Element("descend")
        var e = Element("E1")
        e.appendChild(Comment("C1"))
        e.appendChild(Text("T1"))
        e.appendChild(Element("E2"))
        root.appendChild(e)
        e = Element("descend")
        e.appendChild(Comment("C2"))
        e.appendChild(ProcessingInstruction("PI1", "data1"))
        e.appendChild(Text("T2"))
        e.appendChild(Element("E3"))
        root.appendChild(e)
        val doc = Document(root)
        val handler = TestHandler()
        DocumentTraversal.traverseDocument(doc, handler)
        val expected = """
                Start of document
                Start of element: descend
                Start of element: E1
                End of element: E1
                Start of element: descend
                On comment: C2
                On processing instruction: PI1
                On text: T2
                Start of element: E3
                End of element: E3
                End of element: descend
                End of element: descend
                End of document
                
                """.trimIndent()
        Assert.assertEquals(handler.result, expected)
    }
}