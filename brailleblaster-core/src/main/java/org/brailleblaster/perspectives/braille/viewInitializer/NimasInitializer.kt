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
package org.brailleblaster.perspectives.braille.viewInitializer

import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.document.BrailleDocument
import org.brailleblaster.perspectives.braille.mapping.elements.BrailleMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.SectionElement
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.perspectives.braille.views.wp.BrailleView
import org.brailleblaster.perspectives.braille.views.wp.TextView
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.UTDElements

class NimasInitializer(doc: BrailleDocument, text: TextView, braille: BrailleView) :
    ViewInitializer(doc, text, braille) {
    init {
        sectionList = ArrayList()
    }

    override fun initializeViews(m: Manager) {
//		findSections(m, document.getRootElement());
        initializeMap(m)

        sectionList[0].setInView(true)

        reformatViews(m, 0)

        if (isEmpty(sectionList)) formatTemplateDocument(m)

        //called during init
        m.onPostBuffer(sectionList[0].list)
    }


    //adds or tracks a text node for a blank document when user starts
    fun formatTemplateDocument(m: Manager): MapList {
        val list = sectionList[0].list

        //search for blank document placeholder
        //if it does not exist create placeholder element
        //if it exists populate with text node and brl node
        var section = BBX.getRoot(m.doc)
        while (section.childCount > 0 && BBX.SECTION.isA(section.getChild(0))) {
            section = section.getChild(0) as Element
        }
        var block = XMLHandler.childrenRecursiveNodeVisitor(section) { n: Node? -> n is Element && isPlaceholder(n) }

        //Block page numbers have brl that do not have children
        if (block != null && !BBX.BLOCK.PAGE_NUM.isA(block)) {
            block = populateBlock(block as Element)
            val t = TextMapElement(0, 0, block.getChild(0))
            t.brailleList.add(BrailleMapElement(0, 0, block.getChild(1).getChild(0)))
            list.add(t)
        } else {
            val blankBlock = blankBlock()
            section.appendChild(blankBlock)
            val t = TextMapElement(0, 0, blankBlock.getChild(0))
            t.brailleList.add(BrailleMapElement(0, 0, blankBlock.getChild(1).getChild(0)))
            list.add(t)
        }

        //				m.getStyleView().generate(section);
        //TODO-STYLEPANE: was calling generate
        return list
    }

    //creates a blank document placeholder
    private fun blankBlock(): Element {
        val block = BBX.BLOCK.create(BBX.BLOCK.DEFAULT)
        BBX.BLOCK.ATTRIB_BLANKDOC_PLACEHOLDER[block] = true
        block.appendChild(Text(""))
        val brl = UTDElements.BRL.create()
        brl.appendChild(Text(""))
        block.appendChild(brl)
        return block
    }

    /**
     * populates an existing blank document placeholder
     *
     * @param e
     * @return element with empty text node and braille element
     */
    private fun populateBlock(e: Element): Element {
        if (e.childCount > 0 && BBX.INLINE.isA(e.getChild(0))) {
            return populateBlock(e.getChild(0) as Element)
        } else {
            //inline elements may have existing text node, but no braille
            if (e.childCount == 0 || e.getChild(0) !is Text) e.appendChild(Text(""))

            val brl = UTDElements.BRL.create()
            brl.appendChild(Text(""))
            e.appendChild(brl)
        }
        return e
    }

    private fun isEmpty(sectionList: ArrayList<SectionElement>): Boolean {
        return sectionList[0].list.isEmpty()
    }

    private fun isPlaceholder(n: Node): Boolean {
        return BBX.BLOCK.ATTRIB_BLANKDOC_PLACEHOLDER.has(n)
    }
}
