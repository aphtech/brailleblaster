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
package org.brailleblaster.utd.formatters

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.FormatSelector
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.PageBuilder
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.PageBuilderHelper
import org.brailleblaster.utils.UTD_NS
import kotlin.math.max

/**
 * A formatter for reserving a number of lines for a graphic.
 */
class SkipLinesFormatter : Formatter() {
    override fun format(
        node: Node, style: IStyle, pageBuilders: Set<PageBuilder>,
        formatSelector: FormatSelector
    ): MutableSet<PageBuilder> {
        val mutPageBuilders = pageBuilders.toMutableSet()
        if (mutPageBuilders.isEmpty()) {
            return mutPageBuilders
        }
        if (node !is Element) {
            return mutPageBuilders
        }
        val skipLinesAttr = node.getAttributeValue(UTDElements.UTD_SKIP_LINES_ATTRIB, UTD_NS)
            ?: return mutPageBuilders
        var reserveLines: Int = try {
            skipLinesAttr.toInt()
        } catch (e: NumberFormatException) {
            return mutPageBuilders
        }
        if (reserveLines < 1) {
            return mutPageBuilders
        }
        var pbs = mutPageBuilders
        var pageBuilder = pbs.last()
        if (node.getAttribute("pageSide") != null) PageBuilderHelper.verifyPageSide(
            pageBuilder,
            node.getAttributeValue("pageSide")
        )
        val linesPerPage = pageBuilder.linesPerPage
        var y = pageBuilder.y
        var firstBlank = max(pageBuilder.findFirstBlankLineAfter(y), y)
        pageBuilder.y = firstBlank
        if (pageBuilder.newLinesOverride > 0) {
            pbs.addAll(pageBuilder.processSpacing())
            pageBuilder = pbs.last()
            pageBuilder.setIgnoreSpacing(false)
            y = pageBuilder.y
            firstBlank = pageBuilder.findFirstBlankLineAfter(y).coerceAtLeast(y)
        }
        var keepWithLines = 0
        if (pageBuilder.hasKeepWithNextQueued()) keepWithLines = 1
        if (pageBuilder.hasRunningHead()) {
            reserveLines++
        }
        if (reserveLines > linesPerPage - firstBlank - keepWithLines || pageBuilder.pendingPages > 0) {
            // Start the new page
            pageBuilder.addAtLeastPages(1)
            pbs = pageBuilder.processSpacing()
            pageBuilder = pbs.last()
            pageBuilder.addSkipLines(reserveLines)
        } else {
            pageBuilder.addSkipLines(reserveLines)
        }
        return pbs
    }
}