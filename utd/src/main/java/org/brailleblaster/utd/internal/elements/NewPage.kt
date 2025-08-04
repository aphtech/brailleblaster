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
package org.brailleblaster.utd.internal.elements

import nu.xom.Attribute
import nu.xom.Element
import org.brailleblaster.utd.properties.UTDElements
import kotlin.jvm.JvmOverloads
import org.brailleblaster.utd.properties.PageNumberType
import org.brailleblaster.utils.UTD_NS

class NewPage : Element {
    @JvmOverloads
    constructor(pageNumber: Int = 1, untranslated: String? = "1", pageBrl: String? = "#a", numberType: PageNumberType = PageNumberType.NORMAL) : super(PROTOTYPE) {
        this.pageNumber = pageNumber
        this.untranslated = untranslated
        brlPageNumber = pageBrl
        pageNumberType = numberType
    }

    constructor(e: Element?) : super(e) {
        val localName = localName
        val namespaceUri = namespaceURI
        require(namespaceUri == UTD_NS && localName == UTDElements.NEW_PAGE.elementName) { "Element is not a newPage element" }
    }

    var pageNumber: Int
        get() = getAttributeValue("brlnum").toInt()
        set(pageNumber) {
            addAttribute(Attribute("brlnum", pageNumber.toString()))
        }
    var brlPageNumber: String?
        get() = getAttributeValue("transbrlnum")
        set(pageBrl) {
            addAttribute(Attribute(BRL_PAGE_NUM_ATTIRB_NAME, pageBrl))
        }
    var pageNumberType: PageNumberType
        get() = PageNumberType.equivalentPage(getAttributeValue("brlnumtype"))
        set(pageNumberType) {
            addAttribute(Attribute("brlnumtype", pageNumberType.name))
        }

    var untranslated: String?
    get() = getAttributeValue(UNTRANSLATED_ATTRIB_NAME)
    set(untranslated) {
        addAttribute(Attribute(UNTRANSLATED_ATTRIB_NAME, untranslated))
    }

    override fun addAttribute(attribute: Attribute) {
        val attrName = attribute.localName
        if (attrName == "brlnum") {
            attribute.value.toInt()
        } else if (attrName == "brlnumtype") {
            PageNumberType.valueOf(attribute.value)
        }
        super.addAttribute(attribute)
    }

    override fun removeAttribute(attribute: Attribute): Attribute {
        val attrName = attribute.localName
        require(attrName != "brlnum") { "Cannot remove brlnum from this element" }
        require(attrName != "transbrlnum") { "Cannot remove tranbrlnum from this element" }
        return super.removeAttribute(attribute)
    }

    companion object {
        private val PROTOTYPE = Element(UTDElements.NEW_PAGE.qName, UTD_NS)
        const val PAGE_BREAK_ATTR_NAME = "pageBreak"
        const val PAGE_BREAK_ATTR_VALUE = "true"
        const val UNTRANSLATED_ATTRIB_NAME = "untranslated"
        const val BRL_PAGE_NUM_ATTIRB_NAME = "transbrlnum"
    }
}