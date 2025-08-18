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
package org.brailleblaster.bbx.fixers2

import nu.xom.Document
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.utd.internal.xml.FastXPath

object BBXTo6Upgrader {
    @JvmStatic
	fun upgrade(doc: Document) {
        tabCleanup(doc)
        BBX.setFormatVersion(doc, 6)
    }

    /**
     * Issue #6629: Tabs may mangle physical embosser output
     *
     * @param doc
     */
    private fun tabCleanup(doc: Document) {
        FastXPath.descendant(doc)
            .filterIsInstance<Text>()
            .filter { text: Text -> text.value.contains("\t") }
            .forEach { text: Text -> text.value = text.value.replace('\t',' ') }
    }
}