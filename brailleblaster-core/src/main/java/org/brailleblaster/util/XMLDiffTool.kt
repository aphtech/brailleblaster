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
package org.brailleblaster.util

import nu.xom.*
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utils.xom.attributes
import org.brailleblaster.utils.xom.childNodes

/**
 * Compare XML documents without pretty printing them for line-by-line diff tools
 */
object XMLDiffTool {

    private fun assertEqualRecursive(actualRoot: Element, expectedRoot: Element) {
        try {
            assertEqualRecursiveElement(actualRoot, expectedRoot)
            if (actualRoot.childCount != expectedRoot.childCount) {
                throw AssertionError(
                    "Expected " + actualRoot.childCount + " children "
                            + actualRoot.childNodes
                        .joinToString(separator = ", ") { node ->
                            XMLHandler.toXMLSimple(
                                node
                            )
                        } + " but found " + expectedRoot.childCount + " children "
                            + expectedRoot.childNodes
                        .joinToString(separator = ", ") { node ->
                            XMLHandler.toXMLSimple(
                                node
                            )
                        }
                )
            }
            assertEquals(actualRoot.childCount, expectedRoot.childCount)
        } catch (e: Throwable) {
            throw assertEqualException(actualRoot, expectedRoot, e)
        }
        for (i in 0 until actualRoot.childCount) {
            val actualChild = actualRoot.getChild(i)
            val expectedChild = expectedRoot.getChild(i)
            try {
                assertEquals(actualChild.javaClass, expectedChild.javaClass)
                when (actualChild) {
                    is Element -> {
                        assertEqualRecursiveElement(actualChild, expectedChild as Element)
                        assertEqualRecursive(actualChild, expectedChild)
                    }

                    is Text -> {
                        assertEquals(actualChild.value, expectedChild.value)
                    }

                    is Comment -> {
                        assertEquals(actualChild.value, expectedChild.value)
                    }

                    else -> {
                        throw NodeException("Unexpected node type", actualChild)
                    }
                }
            } catch (e: NodeException) {
                // do not re-wrap
                throw e
            } catch (e: Throwable) {
                throw assertEqualException(actualChild, expectedChild, e)
            }
        }
    }

    private fun assertEqualRecursiveElement(actualElement: Element, expectedElement: Element) {
        assertEquals(actualElement.localName, expectedElement.localName)
        assertEquals(actualElement.namespaceURI, expectedElement.namespaceURI)
        assertEquals(actualElement.namespacePrefix, expectedElement.namespacePrefix)
        for (actualAttribute in actualElement.attributes) {
            val expectedAttribute = expectedElement.getAttribute(
                actualAttribute.localName,
                actualAttribute.namespaceURI
            )
            if (expectedAttribute == null) {
                if (actualAttribute.localName == "fuckedBy") {
                    continue
                }
                throw AssertionError("Expected missing attribute from actual: " + actualAttribute.localName)
            }
            assertEquals(actualAttribute.value, expectedAttribute.value)
        }
        if (actualElement.attributeCount != expectedElement.attributeCount) {
            val expectedAttribMap = expectedElement.attributes.associate { it.localName to it.value }.toMutableMap()
            actualElement.attributes
                .forEach { attr: Attribute -> expectedAttribMap.remove(attr.localName) }
            throw NodeException(
                "Expected has extra attributes " + java.lang.String.join(", ", expectedAttribMap.keys),
                actualElement
            )
        }
    }

    private fun assertEqualException(actualNode: Node, expectedNode: Node, cause: Throwable?): NodeException {
        val actualDoc = actualNode.document
        val expectedRootCopy = expectedNode.document.rootElement.copy()
        expectedRootCopy.addAttribute(Attribute("expectedRoot", "true"))
        actualDoc.rootElement.appendChild(expectedRootCopy)
        throw NodeException(
            "Expected "
                    + XMLHandler.toXMLSimple(expectedNode)
                    + " does not match actual "
                    + XMLHandler.toXMLSimple(actualNode)
                    + " | ",
            actualNode,
            cause
        )
    }

    private fun assertEquals(actual: Any, expected: Any) {
        if (actual != expected) {
            throw AssertionError("Exected '$expected' but found '$actual'")
        }
    }
}
