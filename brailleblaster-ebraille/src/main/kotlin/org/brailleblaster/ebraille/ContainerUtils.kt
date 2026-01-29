/*
 * Copyright (C) 2026 American Printing House for the Blind
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

import nu.xom.Attribute
import nu.xom.Document
import nu.xom.Element
import org.brailleblaster.utils.xml.OPEN_DOCUMENT_CONTAINER_NS

fun createContainerXml(opfPath: String): Document = Document(Element("container", OPEN_DOCUMENT_CONTAINER_NS).apply {
    addAttribute(Attribute("version", "1.0"))
    appendChild(Element("rootfiles", OPEN_DOCUMENT_CONTAINER_NS).apply {
        appendChild(Element("rootfile", OPEN_DOCUMENT_CONTAINER_NS).apply {
            addAttribute(Attribute("full-path", opfPath))
            addAttribute(Attribute("media-type", "application/oebps-package+xml"))
        })
    }
    )
})