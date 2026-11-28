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
package org.brailleblaster.bbx.legacy

import nu.xom.Document
import nu.xom.Element
import org.brailleblaster.bbx.BBX
import org.brailleblaster.utils.xml.BB_NS

/**
 * Read-only detection of documents written by older BrailleBlaster releases.
 *
 * Nothing here modifies the document, so calling it on a current (format [BBX.FORMAT_VERSION])
 * file has no effect on how that file is loaded.
 */
object LegacyFormat {
    private const val HEAD = "head"
    private const val VERSION = "version"

    /**
     * The BBX format version of a BrailleBlaster 1.x document, or `null` when the document is not
     * a 1.x document (it is current, it is a 2.0 document, or it is not BBX at all).
     *
     * 1.x documents have the same element structure as current documents (`SECTION`, `CONTAINER`,
     * `BLOCK`, ...) and an integer `bb:version` lower than [BBX.FORMAT_VERSION]. A 1.x document
     * with no version element is treated as version 1.
     */
    fun legacyBbxVersion(doc: Document): Int? {
        val root = doc.rootElement
        if (!isBbDoc(root) || !hasRootSection(root)) {
            return null
        }
        val versionText = versionText(root) ?: return 1
        val version = versionText.toIntOrNull() ?: return null
        return if (version < BBX.FORMAT_VERSION) version.coerceAtLeast(1) else null
    }

    /** Trimmed text of `head/version`, or `null` when there is no version element. */
    internal fun versionText(root: Element): String? =
        root.getFirstChildElement(HEAD, BB_NS)
            ?.getFirstChildElement(VERSION, BB_NS)
            ?.value
            ?.trim()

    internal fun isBbDoc(root: Element): Boolean =
        root.localName == BBX.DOCUMENT_ROOT_NAME && root.namespaceURI == BB_NS

    private fun hasRootSection(root: Element): Boolean =
        root.childElements.any { it.localName == BBX.SECTION.name && it.namespaceURI == BB_NS }
}
