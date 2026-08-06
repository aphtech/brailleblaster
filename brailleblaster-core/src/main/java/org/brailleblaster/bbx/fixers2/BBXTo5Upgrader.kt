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
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.fixers.TableImportFixer
import org.brailleblaster.bbx.fixers.TableImportFixer.Companion.detectTableTypeUntranslated
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.utils.TableUtils

object BBXTo5Upgrader {
    fun upgrade(doc: Document) {
        tableCleanup(doc)
        BBX.setFormatVersion(doc, 5)
    }

    /**
     * Issue #6163: Tables were mangled by LiveFixer
     * @param doc
     */
    private fun tableCleanup(doc: Document) {
        if (BBX.getFormatVersion(doc) == 5) {
            // this is Expensive, only run once
            return
        }

        // Remove old ugly way
        val oldCleanupElem = DocumentUTDConfig.NIMAS.getConfigElement(doc, "tableCleanup6163")
        oldCleanupElem?.detach()

        // Let the LiveFixer potentially mangle the document
        LiveFixer.fix(BBX.getRoot(doc))
        FastXPath.descendant(doc)
            .filter { node: Node? -> BBX.CONTAINER.TABLE.isA(node) }
            .map { node: Node? -> node as Element }
            .filter { table ->
                detectTableTypeUntranslated(
                    table
                ) == TableUtils.TableTypes.NONTABLE
            }.forEach { TableImportFixer.stripTable(it) }
    }
}