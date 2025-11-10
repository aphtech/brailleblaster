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
package org.brailleblaster.bbx.fixers

import nu.xom.Document
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.fixers2.ImportFixerCommon
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.utd.exceptions.UTDInterruption
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.utils.TableUtils
import org.brailleblaster.utd.utils.TableUtils.TableTypes
import org.brailleblaster.utils.xom.childNodes
import org.slf4j.LoggerFactory

class TableImportFixer : AbstractFixer() {
    override fun fix(matchedNode: Node) {
        BBX.CONTAINER.TABLE.assertIsA(matchedNode)
        val table = matchedNode as Element
        BBX._ATTRIB_FIXER_TODO.assertAndDetach(BBX.FixerTodo.TABLE_SIZE, table)
        // Cells only containing spaces may cause errors when translating
        // We must do it before trying to detect the table type because type detection uses translation.
        XMLHandler.childrenRecursiveNodeVisitor(table) {
            // Not sure if this stripping of spaces is wise
            // Certainly should not be done for MathML.
            if (it is Text && it.value.isBlank() && !MathModuleUtils.isMath(it)) {
                it.value = ""
            }
            false
        }
        if (detectTableTypeUntranslated(table) == TableTypes.NONTABLE) {
            log.trace("Table is too big, stripping table elements")
            try {
                stripTable(table)
            } catch (e: Exception) {
                throw RuntimeException("Cannot strip non-table table element", e)
            }
        } else {
            try {
                stripUnusedCellElements(table)
            } catch (e: Exception) {
                throw RuntimeException("Can't cleanup table cell contents", e)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TableImportFixer::class.java)

        /**
         * Detect table type when given table has no braille, without changing the
         * source document with table copies
         */
        @JvmStatic
        fun detectTableTypeUntranslated(table: Element): TableTypes {
            BBX.CONTAINER.TABLE.assertIsA(table)
            val utdMan = ImportFixerCommon.UTD_MANAGER
            var doc: Document
            /*
            Issue #4780: Opening other documents can cause our UTD formatter thread to get interrupted.
            So keep trying and the formatter will stop or the Notify dialog will block user input
            TODO: They shouldn't be connected in the first place
            */
            // Set maximum retry limit to prevent infinite looping
            var retries = 10
            while (true) {
                doc = BBX.newDocument()
                doc.rootElement.appendChild(BBX.SECTION.ROOT.create())
                val root = BBX.getRoot(doc)
                root.appendChild(table.copy() as Element)

                //Translate for table type detection
                doc = try {
                    utdMan.engine.translateAndFormatDocument(doc)
                } catch (e: UTDInterruption) {
                    retries--
                    if (retries <= 0) {
                        throw RuntimeException(
                            "Something is preventing translation and formatting, please try again.",
                            e
                        )
                    }
                    continue
                }
                break
            }
            val tableForDetection = BBX.getRoot(doc).childElements[0]
            BBX.CONTAINER.TABLE.assertIsA(tableForDetection)
            return TableUtils.detectType(
                tableForDetection,
                utdMan.engine.styleMap,
                utdMan.engine.brailleSettings,
                utdMan.engine.pageSettings
            )
        }

        /**
         *
         */
        fun stripUnusedCellElements(bbxTable: Element) {
            for (curRow in bbxTable.childElements) {
                if (BBX.CONTAINER.TABLE_ROW.isA(curRow)) {
                    for (curCell in curRow.childElements.filter {
                        BBX._ATTRIB_FIXER_TODO.has(it)
                                && BBX._ATTRIB_FIXER_TODO[it] == BBX.FixerTodo.TABLE_CELL_REAL
                    }) {
                        BBX.transform(curCell, BBX.BLOCK.TABLE_CELL)
                        BBX._ATTRIB_FIXER_TODO.assertAndDetach(BBX.FixerTodo.TABLE_CELL_REAL, curCell)
                        for (curCellChild in curCell.childElements) {
                            doUnwrap(curCellChild)
                        }
                    }
                }
            }
        }

        private fun doUnwrap(curElement: Element) {
            if (BBX.INLINE.isA(curElement)) {
                return
            }
            if (BBX._ATTRIB_FIXER_TODO.has(curElement)
                && BBX._ATTRIB_FIXER_TODO[curElement] == BBX.FixerTodo.LINE_BREAK
            ) {
                //Do not allow line breaks to duplicate table cells
                curElement.parent.replaceChild(curElement, Text(" "))
                return
            }
            // Could there be elements we need to retain which have no child nodes?
            if (curElement.childCount == 0) {
                curElement.detach()
                return
            }
            val nextNode = XMLHandler.nextSiblingNode(curElement)
            val needsSpace =
                (BBX.BLOCK.isA(curElement) || BBX.CONTAINER.isA(curElement)) && nextNode != null && (BBX.BLOCK.isA(
                    nextNode
                ) || BBX.CONTAINER.isA(nextNode))
            for (childElement in curElement.childElements) {
                doUnwrap(childElement)
            }
            val parent = curElement.parent
            val lastChild = curElement.childNodes.lastOrNull()
            XMLHandler.unwrapElement(curElement)
            if (needsSpace) {
                parent.insertChild(Text(" "), if (lastChild != null) parent.indexOf(lastChild) + 1 else 0)
            }
        }

        /**
         * Removes all table elements leaving the table cell children
         *
         * @param bbxTable
         * @return List of elements (blocks and maybe containers) that were under the table cell
         */
        @JvmStatic
        fun stripTable(bbxTable: Element): List<Element> {
            BBX.CONTAINER.TABLE.assertIsA(bbxTable)
            val cells: MutableList<Element> = ArrayList()
            for (curRow in bbxTable.childElements) {
                if (BBX.CONTAINER.TABLE_ROW.isA(curRow)) {
                    cells.addAll(curRow.childElements)
                    XMLHandler.unwrapElement(curRow)
                }
            }
            XMLHandler.unwrapElement(bbxTable)
            for (cell in cells) {
                if (BBX.BLOCK.isA(cell)) {
                    BBX.transform(cell, BBX.BLOCK.DEFAULT)
                }
            }
            return cells
        }
    }
}