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

import nu.xom.*
import org.brailleblaster.utd.FormatSelector
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.IStyleMap
import org.brailleblaster.utd.PageBuilder
import org.brailleblaster.utd.properties.Align
import org.brailleblaster.utd.utils.PageBuilderHelper
import org.brailleblaster.utd.utils.getAssociatedBrlElement

abstract class Formatter {
    @JvmField
	protected var startNewBlock = false
    abstract fun format(
        node: Node,
        style: IStyle,
        pageBuilders: Set<PageBuilder>,
        formatSelector: FormatSelector
    ): MutableSet<PageBuilder>

    fun processProcessingInstruction(
        pi: ProcessingInstruction,
        pageBuilders: Set<PageBuilder>,
        formatSelector: FormatSelector?
    ): Set<PageBuilder> {
        val pageBuilder = pageBuilders.last()
        when (pi.target) {
            "newBrlPage" -> pageBuilder.addExplicitPages(1)
            "newBrlLine" -> {}
        }
        return pageBuilders
    }

    fun preFormat(node: Node?, pageBuilder: PageBuilder, style: IStyle, styleMap: IStyleMap): Set<PageBuilder> {
        setAlignmentOptions(pageBuilder, style)
        pageBuilder.updatePendingSpaces(style.leftPadding)
        val linesBefore = style.linesBefore
        pageBuilder.addAtLeastLinesBefore(linesBefore)
        val pagesBefore = style.newPagesBefore
        pageBuilder.addAtLeastPages(pagesBefore)
        val namespaces = styleMap.namespaces
        val linesRestrictions = style.getLinesBefore(node, namespaces)
        if (linesRestrictions != null) {
            pageBuilder.setMaxLines(linesRestrictions)
            if (linesRestrictions > 0) {
                startNewBlock = true
            }
        }
        if (linesBefore > 0 || pagesBefore > 0) {
            startNewBlock = true
        }
        return addStartSeparator(node, pageBuilder, style)
    }

    protected fun setAlignmentOptions(pageBuilder: PageBuilder, style: IStyle) {
        pageBuilder.setLeftIndent(style.indent ?: 0).setRightIndent(style.lineLength ?: 0)
            .setFirstLineIndent(style.firstLineIndent).setSkipNumberLines(style.skipNumberLines)
            .alignment = style.align ?: Align.LEFT
    }

    protected fun addStartSeparator(node: Node?, pageBuilder: PageBuilder, style: IStyle): Set<PageBuilder> {
        val results: MutableSet<PageBuilder> = LinkedHashSet()
        if (node is Element && style.startSeparator != null) {
            val color = style.color
            results.addAll(
                pageBuilder.insertStartSeparatorLine(
                    style.startSeparator, color ?: "",
                    (node as ParentNode?)!!
                )
            )
        }
        return results
    }

    fun postFormat(node: Node, pb: PageBuilder, style: IStyle, styleMap: IStyleMap): Set<PageBuilder> {
        var pageBuilder = pb
        val results: MutableSet<PageBuilder> = LinkedHashSet()
        if (node is Element && style.endSeparator != null) {
            results.addAll(PageBuilderHelper.forceNewLineIfNotEmpty(pageBuilder))
            pageBuilder = results.last()
            val brl = pageBuilder.insertEndSeparatorLine(style.endSeparator)
            brl.addAttribute(Attribute("separator", "end"))
            val parent = node.parent
            if (parent != null) {
                var nodeIndex = parent.indexOf(node)
                if (getAssociatedBrlElement(node) != null) {
                    nodeIndex++
                }
                parent.insertChild(brl, nodeIndex + 1)
            }
        }
        pageBuilder.updatePendingSpaces(style.rightPadding)
        pageBuilder.addAtLeastLinesAfter(style.linesAfter)
        val namespaces = styleMap.namespaces
        val maxLines = style.getLinesAfter(node, namespaces)
        if (maxLines != null) {
            pageBuilder.setMaxLines(maxLines)
        }
        pageBuilder.addAtLeastPages(style.newPagesAfter)
        return results
    }
}
