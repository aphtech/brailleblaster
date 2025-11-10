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
package org.brailleblaster.utd.toc

import nu.xom.Attribute
import nu.xom.Element
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.xml.UTD_NS

enum class TOCAttributes(@JvmField val origName: String) {
    TYPE("toc-type");

    val tagName: String = UTDElements.UTD_PREFIX + ":" + origName

    fun inElement(elem: Element): Boolean {
        return getAttribute(elem) != null
    }

    fun add(elem: Element, value: String?) {
        if (inElement(elem)) throw RuntimeException("Element already has attribute " + tagName + " xml " + elem.toXML())
        elem.addAttribute(Attribute(tagName, UTD_NS, value))
    }

    fun remove(elem: Element) {
        val attrib = getAttribute(elem)
            ?: throw RuntimeException("Element doesn't have attribute " + tagName + " xml " + elem.toXML())
        elem.removeAttribute(attrib)
    }

    /**
     * Remove attribute from element and all its children recursively
     * @return the number of removed attributes
     */
    fun removeRecursive(elem: Element): Int {
        var count = 0
        if (inElement(elem)) {
            count++
            remove(elem)
        }
        for (curChild in elem.childElements) count += removeRecursive(curChild)
        return count
    }

    fun getValue(elem: Element): String {
        val attrib = getAttribute(elem)
            ?: throw RuntimeException("Element doesn't have attribute " + tagName + " xml " + elem.toXML())
        return attrib.value
    }

    fun getAttribute(elem: Element): Attribute? {
        return elem.getAttribute(origName, UTD_NS)
    }

    companion object {
        fun removeAllRecursive(elem: Element) {
            removeAll(elem)
            for (curChild in elem.childElements) removeAllRecursive(curChild)
        }

        @JvmStatic
		fun removeAll(elem: Element) {
            for (curAttrib in entries) {
                if (curAttrib.inElement(elem)) curAttrib.remove(elem)
            }
        }
    }
}
