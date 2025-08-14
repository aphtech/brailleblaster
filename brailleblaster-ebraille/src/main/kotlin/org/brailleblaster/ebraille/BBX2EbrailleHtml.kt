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
package org.brailleblaster.ebraille

import nu.xom.Element
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.libembosser.utils.BrailleMapper
import org.brailleblaster.utils.xom.DocumentTraversal
import org.brailleblaster.utils.xom.childNodes

class BBX2EbrailleHtml : DocumentTraversal.Handler {
    var htmlDoc: org.jsoup.nodes.Document? = null
        private set
    private var currentBlock: org.jsoup.nodes.Element? = null
    override fun onStartDocument(d: nu.xom.Document) {
        htmlDoc = org.jsoup.nodes.Document.createShell("").also {
            it.insertChildren(0, org.jsoup.nodes.DocumentType("html", "", ""))
        }
    }

    override fun onStartElement(e: Element): Boolean {
        return if (e.localName == "brl") {
            currentBlock?.appendText(
                e.childNodes.filterIsInstance<Text>().joinToString { BrailleMapper.ASCII_TO_UNICODE_FAST.map(it.value) }
            )
            false
        } else if (BBX.BLOCK.isA(e)) {
            currentBlock = htmlDoc?.body()?.appendElement("pre")
            true
        } else {
            true
        }
    }
}