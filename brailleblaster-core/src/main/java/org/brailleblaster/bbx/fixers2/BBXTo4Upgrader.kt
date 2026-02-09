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
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBX.SubType
import org.brailleblaster.perspectives.braille.searcher.Searcher
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler

object BBXTo4Upgrader {
    @JvmStatic
    fun upgrade(doc: Document) {
        if (BBX.getFormatVersion(doc) != 4) {
            fixStyleExtend(doc)
            fixPageSpanInBlockIssue5975(doc)
        }
        BBX.setFormatVersion(doc, 4)
    }

    /**
     * Issue #6032
     */
    private fun fixStyleExtend(doc: Document?) {
        FastXPath.descendant(doc)
            .filterIsInstance<Element>()
            .filter { Searcher.Filters.noUTDAncestor(it) }
            .forEach {
                val origStyle = ImportFixerCommon.UTD_ENGINE.getStyle(it)
                    ?: ImportFixerCommon.UTD_ENGINE.styleDefinitions.defaultStyle!!
                if (BBX._ATTRIB_OVERRIDE_STYLE.has(it)) {
                    val overrideAttrib = BBX._ATTRIB_OVERRIDE_STYLE.detach(it)
                    val newStyle = ImportFixerCommon.UTD_ENGINE.getStyle(it)
                    if (newStyle == null || newStyle != origStyle) {
                        val coreType = BBX.getType(it)
                        val oldType = coreType.getSubType(it)
                        val newType = fixStyleExtendFindSubtypeWithStyle(oldType, origStyle, it)
                        if (newType == null) {
                            // skip, re-adding old attribute
                            //			log.debug("skip, re-adding old attribute");
                            BBX.transform(it, oldType)
                            it.addAttribute(overrideAttrib)
                        } else {
                            BBX.transform(it, newType)
                        }
                    }
                }
            }
    }

    private fun fixStyleExtendFindSubtypeWithStyle(oldType: SubType, origStyle: IStyle, elem: Element): SubType? {
        var newType: SubType? = null
        for (subType in oldType.coreType.subTypes) {
            BBX.transform(elem, subType)
            val newStyle: IStyle? = try {
                System.setProperty(NodeException.SAVE_TO_DISK_ENABLED_PROPERTY, "false")
                ImportFixerCommon.UTD_ENGINE.getStyle(elem)
            } catch (_: Exception) {
                // cleanup
                val attribute = elem.getAttribute(NodeException.ATTRIBUTE_NAME)
                attribute?.detach()
                // ignore, most likely from breaking the BBXStyleMap
                continue
            } finally {
                System.setProperty(NodeException.SAVE_TO_DISK_ENABLED_PROPERTY, "true")
            }
            if (newStyle != null && newStyle == origStyle) {
                if (newType != null) {
                    throw NodeException("Duplicate fix: $newType and $subType", elem)
                }
                newType = subType
                //				log.info("Fixed with transform from {} to {}: {}", oldType.name, subType.name, XMLHandler.toXMLStartTag(elem));
            }
        }
        return newType
    }

    private fun fixPageSpanInBlockIssue5975(doc: Document?) {
        FastXPath.descendant(doc)
            .filter { node -> BBX.SPAN.PAGE_NUM.isA(node) }
            .map { node -> Searcher.Mappers.toElement(node) }
            .filter { curPage -> curPage.parent.childCount == 1 }
            .forEach { curPage ->
                val ancestorBlock =
                    requireNotNull(XMLHandler.ancestorVisitorElement(curPage) { node: Element? ->
                        BBX.BLOCK.isA(node)
                    }) { "Could not find suitable ancestor." }
                BBX.transform(ancestorBlock, BBX.BLOCK.PAGE_NUM)
                XMLHandler.unwrapElement(curPage)
            }
    }
}