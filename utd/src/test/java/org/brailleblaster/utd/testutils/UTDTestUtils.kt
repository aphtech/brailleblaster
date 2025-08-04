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
package org.brailleblaster.utd.testutils

import nu.xom.Document
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.*
import org.brailleblaster.utd.actions.GenericAction
import org.brailleblaster.utd.actions.GenericBlockAction
import org.brailleblaster.utd.actions.IAction
import org.brailleblaster.utd.actions.SkipAction
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.BrailleTableType
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.UTD_NS
import java.io.StringReader

object UTDTestUtils {
    const val BLOCK_NAME: String = "p"

    /**
     * Wraps the given xml string into a larger xml string that can be parsed into a
     * valid nimas file
     */
    fun generateBookString(headXmlContent: String, bookXmlContent: String): String {
        return ("<?xml version='1.0' encoding='UTF-8'?>"
                + "<!DOCTYPE dtbook"
                + "  PUBLIC '-//NISO//DTD dtbook 2005-3//EN'"
                + "  'http://www.daisy.org/z3986/2005/dtbook-2005-3.dtd'>"
                + "<dtbook version='2005-3' "
                + "xmlns='http://www.daisy.org/z3986/2005/dtbook/' "
                + "xmlns:utd='" + UTD_NS + "' "
                + "xmlns:m='" + "http://www.w3.org/1998/Math/MathML" + "' "
                + ">"
                + "<head>"
                + "<utd:isNormalised>true</utd:isNormalised>"
                + headXmlContent
                + "</head>"
                + "<book bbtestroot='true' testid='testroot'>"
                + bookXmlContent
                + "</book>"
                + "</dtbook>")
    }

    /**
     * Wraps the given xml string into a XOM Document
     */
	@JvmStatic
	fun generateBookDoc(headXML: String, bodyXML: String): Document {
        val xml = generateBookString(headXML, bodyXML)
        return XMLHandler().load(StringReader(xml))
    }

    /**
     * Initializes the engine with the default test configuration and then
     * uses it to translate and format the document. Note that the liblouis
     * table used for translation is the computer braille table, so the
     * resulting braille translation is exactly what is passed in
     * @return The engine that was initialized
     */
	@JvmStatic
	fun translateAndFormat(doc: Document?): UTDTranslationEngine {
        val engine = initTestEngine()
        engine.translateAndFormatDocument(doc!!)
        return engine
    }

    /**
     * Initializes the engine with the TestOptionStyleMap and the test ActionMap
     */
    @JvmOverloads
    fun initTestEngine(
        styleMap: IStyleMap? = initTestStyleMap(),
        actionMap: IActionMap? = initTestActionMap(),
        asciiBraille: Boolean = true
    ): UTDTranslationEngine {
        val engine = UTDTranslationEngine()
        engine.brailleSettings.isUseLibLouisAPH = false
        engine.styleMap = styleMap!!
        engine.actionMap = actionMap!!
        engine.brailleSettings.isUseAsciiBraille = asciiBraille
        return engine
    }

    fun initTestStyleMap(): IStyleMap {
        return TestOptionStyleMap()
    }

    /**
     * Initializes the test ActionMap that will apply
     * GenericBlockAction to tags with the `DEFAULT_BLOCK` name,
     * SkipAction to tags in the UTD namespace, and GenericAction to all
     * other tags.
     */
    fun initTestActionMap(): IActionMap {
        return object : ActionMap() {
            @Throws(NoSuchElementException::class)
            override fun findValue(node: Node): IAction {
                return if ((node is Element) && node.localName == BLOCK_NAME) {
                    TestGenericBlockAction()
                } else if (node is Element && UTD_NS == node.namespaceURI) {
                    SkipAction()
                } else {
                    GenericAction()
                }
            }
        }
    }

    @JvmStatic
	fun elementToString(element: UTDElements, text: String): String {
        return "<" + UTDElements.UTD_PREFIX + ":" + element.elementName +
                (if (text.isEmpty()) "/>" else ">" + text + "</" + UTDElements.UTD_PREFIX + ":" + element.elementName + ">")
    }

    class TestGenericBlockAction : GenericBlockAction() {
        override fun applyTo(node: Node, context: ITranslationEngine): List<TextSpan> {
            return translate(node, BrailleTableType.COMPUTER_BRAILLE, context)
        }
    }
}
