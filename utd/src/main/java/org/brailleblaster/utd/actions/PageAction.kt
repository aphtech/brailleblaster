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
package org.brailleblaster.utd.actions

import nu.xom.Attribute
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.utd.ITranslationEngine
import org.brailleblaster.utd.MetadataHelper
import org.brailleblaster.utd.TextSpan
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.TextTranslator
import org.brailleblaster.utd.utils.getAssociatedBrlElement
import org.brailleblaster.utd.utils.getDocumentHead
import org.brailleblaster.utd.utils.getTextChild
import org.mwhapples.jlouis.Louis.TypeForms

/**
 * This action will append a <brl> element after the <pagenum>
 * or any other valid page element tags, such that the
 * new brl element will contain an attribute printPage
 * with the page number information.
</pagenum></brl> */
class PageAction : IAction {
    var printPage: String = ""
    private var translatedPage: String = ""
    override fun applyTo(node: Node, context: ITranslationEngine): List<TextSpan> {
        printPage = ""
        translatedPage = ""
        return if (node is Element) {
            processPageNode(node, context)
        } else emptyList()
    }

    private fun processPageNode(node: Element, engine: ITranslationEngine): List<TextSpan> {
        //If the node already has a brl for a child, then return
        val textChild = getTextChild(node)
        if (getAssociatedBrlElement(textChild) != null) {
            return emptyList()
        }

        //This can change depending on the configuration used
        val brl = UTDElements.BRL.create()
        var ppOverride = ""
        var translatedOverride: String? = ""
        if (node.getAttribute("utd-pnOverride") != null) {
            ppOverride = node.getAttributeValue("utd-pnOverride")
        }
        translateTextChildren(node, engine, false)
        normalize(node)
        if (ppOverride.isNotEmpty()) {
            translatedOverride = TextTranslator.translateText(ppOverride, engine)
        }
        brl.addAttribute(Attribute("printPage", printPage))
        brl.addAttribute(Attribute("printPageBrl", translatedPage))
        if (node.getAttribute("utd-pnOverride") != null) {
            brl.addAttribute(Attribute("printPageOverride", node.getAttributeValue("utd-pnOverride")))
            brl.addAttribute(Attribute("printPageOverrideBrl", translatedOverride))
        }
        var pageType = getAttributeValue(node)

        //Check the pageType from changes shown in metadata
        val head = getDocumentHead(node.document)
        if (head != null) {
//			Element meta = MetadataHelper.findPrintPageChange(head.getDocument(), printPage);
            val meta = MetadataHelper.findPrintPageChange(head.document, translatedPage)
            if (meta?.getAttribute("pageType") != null) {
                pageType = meta.getAttributeValue("pageType")
            }
        }
        brl.addAttribute(Attribute("pageType", pageType))
        node.appendChild(brl)
        return emptyList()
    }

    /*
	 * This needs to collect all potential text nodes within the element, including the ones that are inside emphasis tags.
	 * Translate here as well.
	 */
    private fun translateTextChildren(node: Element, engine: ITranslationEngine, unwrap: Boolean) {
        if (unwrap) {
            unwrap(node)
        }
        val sb1 = StringBuilder(printPage)
        val sb2 = StringBuilder(translatedPage)
        for (i in 0 until node.childCount) {
            if (node.getChild(i) is Text) {
                //Translate and append to the final string
                sb1.append(node.getChild(i).value)
                var typeForm = TypeForms.PLAIN_TEXT
                if (node.getAttribute("emphasis", node.namespaceURI) != null) {
                    typeForm = getTypeForm(node.getAttributeValue("emphasis", node.namespaceURI))
                }
                sb2.append(TextTranslator.translateText(node.getChild(i).value, engine, typeForm))
            } else if (node.getChild(i) is Element) {
                //There could be something else inside pages that isn't an element (e.g. Comment) so take that into account
                translateTextChildren(node.getChild(i) as Element, engine, true)
            }
        }
        printPage = sb1.toString()
        translatedPage = sb2.toString()
    }

    //Get all the children and insert it to the parent
    private fun unwrap(node: Element) {
        val parent = node.parent as Element
        var index = parent.indexOf(node)
        for (i in 0 until node.childCount) {
            parent.insertChild(node.getChild(i).copy(), index)
            index++
        }
        node.detach()
    }

    private fun normalize(node: Element) {
        var text = StringBuilder()
        var i = 0
        while (i < node.childCount) {
            if (node.getChild(i) is Text) {
                text.append(node.getChild(i).value)
                node.getChild(i).detach()
                i--
            } else {
                node.insertChild(text.toString(), i)
                text = StringBuilder()
                i++
            }
            i++
        }
        node.appendChild(text.toString())
    }

    private fun getTypeForm(emphasis: String): Short {
        if (emphasis == "NO_TRANSLATE") {
            return TypeForms.NO_TRANSLATE
        }
        return if (emphasis == "NO_CONTRACT") {
            TypeForms.NO_CONTRACT
        } else TypeForms.PLAIN_TEXT
    }

    private fun getAttributeValue(node: Element): String {
        val pageType: String = if (node.getAttributeValue("page") != null) {
            when (node.getAttributeValue("page")) {
                "front" -> "P_PAGE"
                "bbAdded" -> {
                    //Find the previous page node and get its attribute value
                    val pagesBefore = node.query("preceding::node()[@utd-action='PageAction']")
                    if (pagesBefore.size() > 0 && (pagesBefore[pagesBefore.size() - 1] as Element).getAttribute("page") != null) {
                        return getAttributeValue(pagesBefore[pagesBefore.size() - 1] as Element)
                    }
                    "NORMAL"
                }

                else -> "NORMAL"
            }
        } else {
            "NORMAL"
        }
        return pageType
    }
}