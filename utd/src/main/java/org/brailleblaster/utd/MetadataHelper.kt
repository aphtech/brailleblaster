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
package org.brailleblaster.utd

import nu.xom.Attribute
import nu.xom.Document
import nu.xom.Element
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.getDocumentHead

/*
 * 	All metadata must be appended to the <head>. 
 * 
 * 	*Note: Some documents may not have <head> required, 
 * 		   need to add support for those.
 */
object MetadataHelper {
    private const val PRINT = "printPage"
    private const val BRL = "braillePage"

    @JvmStatic
    fun changePrintPageNumber(doc: Document, originalNum: String, newNum: String?, volume: String?, skip: Boolean) {
        addNewChangeMeta(doc, PRINT, originalNum, newNum, null, null,
            combined = false,
            blank = false,
            volume = volume,
            skip = skip,
            runHead = true
        )
    }

    fun changePrintPageNumber(
        doc: Document, originalNum: String,
        newNum: String?, newContLetter: String?, volume: String?, skip: Boolean
    ) {
        addNewChangeMeta(doc, PRINT, originalNum, newNum, newContLetter, null,
            combined = false,
            blank = false,
            volume = volume,
            skip = skip,
            runHead = true
        )
    }

    @JvmStatic
    fun changePrintPageNumber(
        doc: Document, originalNum: String, newNum: String?,
        newContLetter: String?, pageType: String?, volume: String?, skip: Boolean
    ) {
        addNewChangeMeta(doc, PRINT, originalNum, newNum, newContLetter, pageType,
            combined = false,
            blank = false,
            volume = volume,
            skip = skip,
            runHead = true
        )
    }

    @JvmStatic
    fun markBlankPrintPageNumber(doc: Document, originalNum: String, volume: String?, skip: Boolean) {
        addNewChangeMeta(doc, PRINT, originalNum, null, null, null,
            combined = false,
            blank = true,
            volume = volume,
            skip = skip,
            runHead = true
        )
    }

    @JvmStatic
    fun changeBraillePageNumber(
        doc: Document,
        originalNum: String,
        newNum: String?,
        volume: String?,
        skip: Boolean,
        runHead: Boolean
    ) {
        addNewChangeMeta(doc, BRL, originalNum, newNum, null, null,
            combined = false,
            blank = false,
            volume = volume,
            skip = skip,
            runHead = runHead
        )
    }

    fun combinePrintPageNumbers(doc: Document, originalNum: String, newNum: String?, skip: Boolean, runHead: Boolean) {
        addNewChangeMeta(doc, PRINT, originalNum, newNum, null, null,
            combined = true,
            blank = false,
            volume = null,
            skip = skip,
            runHead = runHead
        )
    }

    fun addMeta(
        type: String,
        originalStr: String,
        newStr: String?,
        resetCL: String?,
        pageType: String?,
        combined: Boolean,
        blank: Boolean,
        volume: String?,
        skip: Boolean,
        runHead: Boolean
    ): Element {
        val meta = UTDElements.META.create()
        meta.addAttribute(Attribute("type", type))
        meta.addAttribute(Attribute("original", originalStr))
        if (newStr != null) {
            meta.addAttribute(Attribute("new", newStr))
        }
        if (resetCL != null) {
            meta.addAttribute(Attribute("cl", resetCL))
        }
        if (pageType != null) {
            meta.addAttribute(Attribute("pageType", pageType))
        }
        if (combined) {
            meta.addAttribute(Attribute("combined", "true"))
        }
        if (blank) {
            meta.addAttribute(Attribute("blank", "true"))
        }
        if (!volume.isNullOrEmpty()) {
            meta.addAttribute(Attribute("pageVolume", volume))
        }
        //Meta should not be considered in formatting
        if (skip) {
            meta.addAttribute(Attribute("skip", "true"))
        }
        //Either you have a running head disabled tag or no attribute at all
        if (!runHead) {
            meta.addAttribute(Attribute("runHead", "false"))
        }
        return meta
    }

    private fun addNewChangeMeta(
        doc: Document,
        type: String,
        originalStr: String,
        newStr: String?,
        resetCL: String?,
        pageType: String?,
        combined: Boolean,
        blank: Boolean,
        volume: String?,
        skip: Boolean,
        runHead: Boolean
    ) {
        val head = doc.document.getDocumentHead()
        val meta = addMeta(type, originalStr, newStr, resetCL, pageType, combined, blank, volume, skip, runHead)
        head?.appendChild(meta)
    }

    @JvmStatic
    fun findPrintPageChange(doc: Document, originalNum: String): Element? {
        return findPageChange(doc, originalNum, PRINT)
    }

    @JvmStatic
    fun findBraillePageChange(doc: Document, originalNum: String): Element? {
        return findPageChange(doc, originalNum, BRL)
    }

    fun findPrintPageCombiner(doc: Document?, originalNum: String): Element? {
        val metas = getUTDMeta(doc)
        for (meta in metas) {
            if (PRINT == meta.getAttributeValue("type") && originalNum == meta.getAttributeValue("original") && "true" == meta.getAttributeValue(
                    "combined"
                )
            ) return meta
        }
        return null
    }

    private fun findPageChange(doc: Document, originalNum: String, type: String): Element? {
        var curOriginalNum = originalNum
        val metas = getUTDMeta(doc)
        var resultMeta: Element? = null
        for (meta in metas) {
            if (type == meta.getAttributeValue("type")) {
                if (curOriginalNum == meta.getAttributeValue("original")) {
                    resultMeta = combineElementAttributes(resultMeta, meta)
                }
                if (resultMeta != null) {
                    resultMeta = adaptNewPageNumber(resultMeta, meta)
                    //You have to take into account the possibility that your "original number" may have changed after you adapted to another meta
                    curOriginalNum = resultMeta.getAttributeValue("original")
                }
            }
        }
        return resultMeta
    }

    fun markUsed(doc: Document?, originalNum: String, type: String) {
        val metas = getUTDMeta(doc)
        if (metas.isNotEmpty()) {
            for (meta in metas) {
                if (type == meta.getAttributeValue("type")) {
                    //You need to mark each specific meta as used as you combine/adapt each one of them
                    if (originalNum == meta.getAttributeValue("original")) {
                        meta.addAttribute(Attribute("used", "true"))
                    }
                }
            }
        }
    }

    /*
	 * If old and new has the same attributes, the value of the attributes of the new meta must hold true
	 * Run through all the attributes of the old meta and if the new one doesn't have it, add.
	 */
    private fun combineElementAttributes(oldMeta: Element?, newMeta: Element): Element {
        if (oldMeta == null) {
            return newMeta
        }
        for (i in 0 until oldMeta.attributeCount) {
            //Exception: Running heads. Do not combine. Taken out on purpose.
            if (newMeta.getAttribute(oldMeta.getAttribute(i).localName) == null && oldMeta.getAttribute(i).localName != "runHead") {
                val temp = Attribute(oldMeta.getAttribute(i).localName, oldMeta.getAttribute(i).value)
                newMeta.addAttribute(temp)
            }
        }
        return newMeta
    }

    /*
	 * If you change a page number to another, and change that page number again, the meta should adapt to the changes
	 * such that "new" gets searched against any "original".
	 * e.g. Metadata 1: Original = 4AB   New = a4AB
	 *      Metadata 2: Original = a4AB  New = b4AB
	 *      
	 * If you search for 4AB, it should now return b4AB.
	 * Always make sure that they are from the same volume as well. 
	 */
    private fun adaptNewPageNumber(baseMeta: Element, newMeta: Element): Element {
        val adaptedMeta = baseMeta.copy()
        if (baseMeta.getAttribute("new") != null && baseMeta.getAttributeValue("new") == newMeta.getAttributeValue("original")) {
            //Only update the attributes. Do not return the newMeta. There might be other attributes from the baseMeta that you still need.
//			adaptedMeta.addAttribute(new Attribute("original", newMeta.getAttributeValue("original")));

            //Originals come paired with new, cl, blank, pageType.
            if (newMeta.getAttribute("new") != null) {
                adaptedMeta.addAttribute(Attribute("new", newMeta.getAttributeValue("new")))
            }
            if (newMeta.getAttribute("cl") != null) {
                adaptedMeta.addAttribute(Attribute("cl", newMeta.getAttributeValue("cl")))
            }
            if (newMeta.getAttribute("pageType") != null) {
                adaptedMeta.addAttribute(Attribute("pageType", newMeta.getAttributeValue("pageType")))
            } else {
                adaptedMeta.addAttribute(
                    Attribute(
                        newMeta.getAttribute(newMeta.attributeCount - 1).localName,
                        newMeta.getAttribute(newMeta.attributeCount - 1).value
                    )
                )
            }

            //If baseMeta contains runHead and new doesn't, remove that attribute.
            if (baseMeta.getAttribute("runHead") != null && newMeta.getAttribute("runHead") == null) {
                adaptedMeta.removeAttribute(adaptedMeta.getAttribute("runHead"))
            }

            //If you used a metadata for adapting, you shouldn't re-check this as you go through pageBuilder anymore
            if (newMeta.getAttribute("blank") == null) {
                newMeta.addAttribute(Attribute("used", "true"))
            }
        }
        return adaptedMeta
    }

    @JvmStatic
    fun getUTDMeta(doc: Document?): List<Element> {
        val returnList: MutableList<Element> = ArrayList()
        val head = doc.getDocumentHead()
        if (head != null) {
            for (i in 0 until head.childElements.size()) {
                if (UTDElements.META.isA(head.childElements[i])) {
                    returnList.add(head.childElements[i])
                }
            }
        }
        return returnList
    }
}
