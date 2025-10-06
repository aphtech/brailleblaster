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
package org.brailleblaster.bbx

import nu.xom.*
import org.brailleblaster.BBIni
import org.brailleblaster.bbx.BBX.CoreType
import org.brailleblaster.utd.config.StyleDefinitions
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utils.xom.childNodes
import org.brailleblaster.util.Utils
import org.brailleblaster.utils.BB_NS
import org.brailleblaster.utils.MATHML_NS
import org.brailleblaster.utils.UTD_NS
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Validate BBX format
 */
object BBXValidator {
    private val log = LoggerFactory.getLogger(BBXValidator::class.java)

    @Throws(ValidateException::class)
    fun validElementType(type: CoreType?, node: Node?) {
        if (type == null) {
            throw NullPointerException("type")
        }
        if (node == null) {
            throw NullPointerException("node")
        }
        val validateResult = type.validate(node)
        if (validateResult != null) {
            throw ValidateException(
                node,
                "Input node is not expected type {}: {}",
                type,
                validateResult
            )
        }
    }

    fun validateDocument(doc: Document, styleDefs: StyleDefinitions) {
        if (!BBIni.debugging) {
            return
        }
        val startTime = System.currentTimeMillis()
        assertEquals(BBX.getFormatVersion(doc), BBX.FORMAT_VERSION, doc)
        assertEquals(BBX.getHead(doc), doc.rootElement.childElements[0], doc)
        val bookRoot = BBX.getRoot(doc)
        assertEquals(bookRoot, doc.rootElement.childElements[1], doc)
        //TODO: skip comments in root
//		assertEquals(doc.getRootElement().getChildCount(), 2, doc);
        val itrStack: Deque<Element> = ArrayDeque()
        itrStack.push(bookRoot)
        while (!itrStack.isEmpty()) {
            val curElem = itrStack.pop()
            when (curElem.namespaceURI) {
                UTD_NS, MATHML_NS ->                    //do not try to validate
                    continue

                BB_NS -> {}
                else -> throw ValidateException(curElem, "Unexpected namspace " + curElem.namespaceURI)
            }
            val curType = BBX.getType(curElem)
            val curSubType = curType.getSubType(curElem)
            val elemDebug = Utils.formatMessage(
                "{} subtype {} for element {}",
                curType,
                curSubType,
                XMLHandler.toXMLSimple(curElem)
            )
            log.trace("Found type $elemDebug")
            try {
                curSubType.assertComplete(curElem, styleDefs)

                //Can't validate root element this way as there's no parent
                if (curElem !== bookRoot) {
                    val parentType = BBX.getType(curElem.parent as Element)
                    if (!parentType.isValidChild(curType)) {
                        throw ValidateException(
                            curElem,
                            "Child type {} cannot descend from {} for node",
                            curType,
                            parentType
                        )
                    }
                }
                if (BBX._ATTRIB_OVERRIDE_STYLE.has(curElem)) {
                    assertNotEquals(
                        styleDefs.getStyleByName(BBX._ATTRIB_OVERRIDE_STYLE[curElem]),
                        null,
                        curElem
                    )
                }
                if (curSubType === BBX.INLINE.MATHML) {
                    continue
                }
                for (curChildNode in curElem.childNodes) {
                    if (curChildNode is Comment) {
                        //do nothing, these are valid everywhere
                    } else if (curChildNode is Text) {
                        if (!curType.textChildrenValid) {
                            throw ValidateException(curChildNode, "Unexpected text node under {}", curType)
                        }
                    } else if (curChildNode is Element) {
                        //TODO: This design validates the document in reverse...
                        itrStack.push(curChildNode)
                    } else {
                        throw NodeException("Unhandled node", curChildNode)
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException("Failed when processing $elemDebug", e)
            }
        }
        log.info("Validated BBX document in " + Utils.runtimeToString(startTime))
    }

    fun assertEquals(obj1: Any?, obj2: Any?, context: Node?) {
        if (obj1 != obj2) {
            throw ValidateException(context, "Objects not equal: {} != {}", obj1, obj2)
        }
    }

    private fun assertNotEquals(obj1: Any?, obj2: Any?, context: Node?) {
        if (obj1 == obj2) {
            throw ValidateException(context, "Objects equal: {} == {}", obj1, obj2)
        }
    }

    class ValidateException(node: Node?, message: String?) : NodeException(message!!, node) {
        constructor(node: Node?, message: String?, vararg args: Any?) : this(node, Utils.formatMessage(message, *args))
    }
}
