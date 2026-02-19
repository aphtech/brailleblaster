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
import org.brailleblaster.utd.PageBuilder
import org.brailleblaster.utd.internal.PartialFormatNodeAncestor
import org.brailleblaster.utd.properties.PageNumberType
import org.brailleblaster.utd.properties.PageNumberType.Companion.equivalentPage
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.PageBuilderHelper.handlePageIndicator
import org.brailleblaster.utd.utils.PageBuilderHelper.isPageIndicator
import org.brailleblaster.utd.utils.PageBuilderHelper.setPageNumberType
import org.brailleblaster.utd.utils.PageBuilderHelper.verifyPageSide
import org.brailleblaster.utd.utils.getAssociatedBrlElement
import org.brailleblaster.utd.utils.getDescendantBrlFast
import java.util.*
import java.util.function.Consumer

open class LiteraryFormatter : Formatter() {
    override fun format(
        node: Node, style: IStyle, pageBuilders: Set<PageBuilder>,
        formatSelector: FormatSelector
    ): MutableSet<PageBuilder> {
        var mutPageBuilders = pageBuilders.toMutableSet()
        if (mutPageBuilders.isEmpty()) {
            return mutPageBuilders
        }
        var pageBuilder = mutPageBuilders.last()
        val prevLineSpacing = pageBuilder.lineSpacing
        pageBuilder.setLineSpacing(style.lineSpacing)
        val styleMap = formatSelector.styleMap
        startNewBlock = false
        mutPageBuilders.addAll(preFormat(node, pageBuilder, style, styleMap))
        pageBuilder = mutPageBuilders.last()

        if (style.isVolumeEnd) {
            pageBuilder.volumeEndLength = node.getDescendantBrlFast().firstOrNull()!!.value.length
            pageBuilder.setVolumeEnd(true)
            mutPageBuilders.addAll(pageBuilder.addVolumeBlankLines())
            if (mutPageBuilders.size > 1) {
                pageBuilder = mutPageBuilders.last()
            }

            if (!formatSelector.engine.pageSettings.isContinuePages) {
                pageBuilder.resetPageNumbers()
                //				pageBuilder.getBraillePageNumber().resetNextPageNumberCounters(pageBuilder.getPageNumberType());
            } else {
                pageBuilder.resetSpecialPageNumbers()
            }

            pageBuilder.titlePage = true
            pageBuilder.alignment = style.align
            pageBuilder.setOverridePageType(false)
        }
        if (startNewBlock) {
            pageBuilder.setStartOfBlock(true)
        }
        if (node is Text) {
            val brl = node.getAssociatedBrlElement()
            if (brl != null) {
                pageBuilder.setCurrBrl(brl)
                if (style.braillePageNumberFormat != null && pageBuilder.pendingPages == 0 && !pageBuilder.isOverridePageType()) {
                    setPageNumberType(pageBuilder, style.braillePageNumberFormat, formatSelector)
                }

                if (isPageIndicator(brl)) {
                    mutPageBuilders.addAll(handlePageIndicator(pageBuilder, brl, style, formatSelector))
                    pageBuilder = mutPageBuilders.last()
                } else {
                    // Catch for guide words
                    if (style.isGuideWords && pageBuilder.engine!!.pageSettings.isGuideWords) {
                        var go = true
                        // Check for first ancestor that says guideWords=true/false
                        val guideWords = node.query("ancestor::*[@guideWords][1]")
                        if (guideWords.size() > 0) {
                            val guideWordParent = guideWords.firstOrNull() as Element?
                            if (guideWordParent != null && guideWordParent.getAttributeValue("guideWords") == "false") {
                                go = false
                            }
                        }

                        // Check if it has an ancestor with spacing value
                        if (node.query("ancestor::*[@spacing][1]").size() > 0) {
                            val spacingAttr = brl.query("ancestor::node()[@spacing][1]")[0] as Element
                            val spacing = spacingAttr.getAttributeValue("spacing").toInt() - 1
                            brl.addAttribute(Attribute("spacing", spacing.toString()))
                        }

                        if (go) {
                            pageBuilder.setSkipNumberLineBottom(true)
                            // Don't forget to change the padding to 3
                            pageBuilder.padding = 3
                            pageBuilder.setGuideWordEnabled(true)

                            // If you don't have a starting guide word, use current
                            // text
                            val guideWord = node.parent as Element

                            if (pageBuilder.getStartGuideWord() == null) {
                                if (pageBuilder.pendingPages > 0) {
                                    pageBuilder = pageBuilder.processSpacing().last()
                                }
                                pageBuilder.setStartGuideWord(guideWord)
                            }

                            pageBuilder.altEndGuideWord = pageBuilder.getEndGuideWord()
                            pageBuilder.setEndGuideWord(guideWord)
                        }
                    }

                    // Check if it has an ancestor with type=pronunciation
                    if (node.query("ancestor::*[@type='pronunciation'][1]").size() > 0) {
                        val pronunciation = node.query("ancestor::*[@type='pronunciation'][1]")[0] as Element

                        brl.addAttribute(Attribute("type", "pronunciation"))
                        brl.addAttribute(Attribute("term", pronunciation.getAttributeValue("term")))
                    }

                    // Check if it has an ancestor with spaced=true
                    if (node.query("ancestor::*[@spaced='true'][1]").size() > 0) {
                        brl.addAttribute(Attribute("spaced", "true"))
                    }

                    mutPageBuilders.addAll(pageBuilder.addBrl(brl))
                    if (mutPageBuilders.size > 1) {
                        pageBuilder = mutPageBuilders.last()
                    }
                }
            }
        } else if (node is Element && !(UTDElements.BRL.isA(node))) {
            val keepWithNext = style.isKeepWithNext
            if (keepWithNext && !pageBuilder.getKeepWithNext()) pageBuilder.setKeepWithNext(true)
            if (style.isDontSplit && !pageBuilder.dontSplit) pageBuilder.setDontSplit(true)
            if (node.getAttribute("pageSide") != null) verifyPageSide(pageBuilder, node.getAttributeValue("pageSide"))
            if (node.getAttribute("pageType") != null) {
                pageBuilder.updatePageNumberType(equivalentPage(node.getAttributeValue("pageType")))
            } else if (node.localName == "BLOCK" && pageBuilder.isOverridePageType() && pageBuilder.pageNumberType == PageNumberType.T_PAGE) {
                pageBuilder.updatePageNumberType(pageBuilder.previousPageNumberType!!)
                pageBuilder.setOverridePageType(false)
            }

            var i = 0
            while (i < node.getChildCount()) {
                val child = node.getChild(i)
                if (child is ProcessingInstruction) {
                    val pageBuildersSize = mutPageBuilders.size
                    mutPageBuilders = processProcessingInstruction(
                        child, mutPageBuilders,
                        formatSelector
                    ).toMutableSet()
                    if (mutPageBuilders.size > pageBuildersSize) {
                        pageBuilder = mutPageBuilders.last()
                    }
                } else if (!UTDElements.BRL.isA(child)) {
                    pageBuilder = Objects.requireNonNull(formatSelector.formatNode(child, mutPageBuilders))
                    // pageBuilder may be different now
                    mutPageBuilders.add(pageBuilder)
                } else {
                    val brl = child as Element
                    if (isPageIndicator(brl) //You only have to do this if you're doing blank PPI, meaning the brl should be the only child of the node
                        && (brl.childCount == 0 || brl.parent.childCount == 1)
                    ) {
                        handlePageIndicator(pageBuilder, brl, style, formatSelector)
                        //						PageBuilderHelper.changePageNumberType(pageBuilder, brl, style, formatSelector);
                    } else if (brl.getAttributeValue("tabValue") != null) {
                        val curX = pageBuilder.x
                        val tabValue = brl.getAttributeValue("tabValue").toInt() - 1

                        if (curX < tabValue) {
                            mutPageBuilders.addAll(pageBuilder.processSpacing())
                            if (mutPageBuilders.size > 1) {
                                pageBuilder = mutPageBuilders.last()
                            }
                            pageBuilder.setTabbed(true)
                            pageBuilder.x = tabValue
                        } else {
                            pageBuilder.addAtLeastLinesBefore(1)
                            mutPageBuilders.addAll(pageBuilder.processSpacing())
                            if (mutPageBuilders.size > 1) {
                                pageBuilder = mutPageBuilders.last()
                            }
                            pageBuilder.x = tabValue
                        }
                    }
                }
                if (child.getDescendantBrlFast().isNotEmpty()) {
                    //Should keep start of block true if what you've found are pages to preserve correct indentations
                    if (child is Element) {
                        if (!((UTDElements.BRL.isA(child) && isPageIndicator(child))
                                    || (child.getAttribute("utd-action") != null && child.getAttributeValue("utd-action") == "PageAction"))
                        ) {
                            pageBuilder.setStartOfBlock(false)
                        }
                    }
                }
                i++
            }
            if (keepWithNext) mutPageBuilders.forEach(Consumer { pb: PageBuilder -> pb.setKeepWithNext(false) })
            if (style.isDontSplit) mutPageBuilders.forEach(Consumer { pb: PageBuilder -> pb.setDontSplit(false) })
        }
        mutPageBuilders.addAll(postFormat(node, pageBuilder, style, styleMap))

        pageBuilder = mutPageBuilders.last()
        if (style.linesAfter > 0) {
            pageBuilder.setStartOfBlock(true)
        }
        pageBuilder.setLineSpacing(prevLineSpacing)
        return mutPageBuilders
    }

    fun partialFormat(
        pathToStart: Deque<PartialFormatNodeAncestor>, startPoint: Node, style: IStyle,
        pageBuilders: MutableSet<PageBuilder>, formatSelector: FormatSelector
    ): Set<PageBuilder> {
        var pageBuilder = pageBuilders.last()
        val pathElement = pathToStart.pop()
        val node = pathElement.node
        val start = pathElement.isFirstBrailleAtStartPoint
        setAlignmentOptions(pageBuilder, style)
        if (start) {
            pageBuilder.preparePage()
            pageBuilders.addAll(addStartSeparator(node, pageBuilder, style))
        }
        pageBuilder = pageBuilders.last()
        if (!pathToStart.isEmpty() && node is ParentNode) {
            // As we are partially formatting, the first child we process also
            // should be partially formatted.
            // We MUST get the start child from the pathToStart before partial
            // formatting lower down due to FormatSelector.partialFormat
            // modifying the pathToStart collection
            val startChild = pathToStart.first.node
            val keepWithNext = style.isKeepWithNext
            if (keepWithNext && !pageBuilder.getKeepWithNext()) pageBuilder.setKeepWithNext(true)
            pageBuilders.add(formatSelector.partialFormat(pathToStart, startPoint, pageBuilders))
            val startIndex = node.indexOf(startChild) + 1
            for (i in startIndex until node.getChildCount()) {
                val nodeChild = node.getChild(i)
                if (!UTDElements.BRL.isA(nodeChild)) {
                    pageBuilders.add(formatSelector.formatNode(nodeChild, pageBuilders))
                }
            }
            if (keepWithNext) pageBuilders.forEach(Consumer { pb: PageBuilder? -> pb!!.setKeepWithNext(false) })
        } else {
            // Node has Braille content and needs formatting
            // NOTE: We need not handle print page indicators here as partial
            // formatting always starts at a Braille page boundary and thus a
            // print page indicator should not be inserted.
            val brl = startPoint.parent as Element
            if (isPageIndicator(brl) //You only have to do this if you're doing blank PPI
                && brl.getAttributeValue("printPage").isEmpty()
            ) {
                handlePageIndicator(pageBuilder, brl, style, formatSelector)
            }
            val startPointIndex = brl.indexOf(startPoint)
            if (pageBuilder.isStartOfBlock()) {
                if (pageBuilder.hasLeftPage(pageBuilder.braillePageNumber)) {
                    pageBuilder.x = pageBuilder.cornerPageLength
                } else {
                    pageBuilder.x = pageBuilder.firstLineIndent
                }
            } else {
                if (pageBuilder.hasLeftPage(pageBuilder.braillePageNumber)) {
                    pageBuilder.x = pageBuilder.cornerPageLength
                } else {
                    pageBuilder.x = pageBuilder.leftIndent
                }
            }

            pageBuilders.addAll(pageBuilder.addBrlFromChild(brl, startPointIndex))
            if (pageBuilders.size > 1) {
                pageBuilder = pageBuilders.last()
            }

            // Catch for guide words
            if (style.isGuideWords) {
                // Don't forget to change the padding to 3
                pageBuilder.padding = 3
                pageBuilder.setGuideWordEnabled(true)
                // If you don't have a starting guide word, use current text
                // Or, if you are on the first line of the page, use current
                // text
                if (pageBuilder.getStartGuideWord() == null
                    || (pageBuilder.y == pageBuilder.findFirstBlankLine() || pageBuilder.y == 0)
                ) {
                    if (pageBuilder.pendingPages > 0) {
                        pageBuilder = pageBuilder.processSpacing().last()
                    }
                    pageBuilder.setStartGuideWord(node.parent as Element)
                }
                pageBuilder.altEndGuideWord = pageBuilder.getEndGuideWord()
                pageBuilder.setEndGuideWord(node.parent as Element)
            }
        }
        pageBuilders.addAll(postFormat(node, pageBuilder, style, formatSelector.styleMap))
        return pageBuilders
    }
}