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
package org.brailleblaster.debug

import nu.xom.Element
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.BraillePageBrlMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.PageIndicatorTextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.PrintPageBrlMapElement
import org.brailleblaster.util.LINE_BREAK
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.TabFolder
import org.eclipse.swt.widgets.TabItem

class PageNumberDebugger(manager: Manager) {
    init {
        val shell = EasySWT.makeDialogFloating(manager.wpManager.shell)
        shell.layout = GridLayout(1, true)
        val tabContainer = TabFolder(shell, SWT.NONE)
        EasySWT.setGridDataVertical(tabContainer)
        val printPageTab = TabItem(tabContainer, SWT.NONE)
        printPageTab.text = "Print Pages"
        val printPageLabel = StyledText(tabContainer, SWT.READ_ONLY or SWT.V_SCROLL)
        printPageTab.control = printPageLabel
        val braillePageTab = TabItem(tabContainer, SWT.NONE)
        braillePageTab.text = "Braille Pages"
        val braillePageLabel = StyledText(tabContainer, SWT.READ_ONLY or SWT.V_SCROLL)
        braillePageTab.control = braillePageLabel

        //----------------- Data ------------------------
        val printPageBuilder = StringBuilder()
        val braillePageBuilder = StringBuilder()
        for (curSection in manager.sectionList) {
            for (curMapping in curSection.list) {
                if (curMapping is PageIndicatorTextMapElement) {
                    val usableNode =
                        (curMapping.node as? Element ?: curMapping.nodeParent)
                    val brlPageNum = usableNode.getAttributeValue("printPage")
                    if (brlPageNum != null) printPageBuilder.append("PageMapElement: ").append(brlPageNum)
                        .append(LINE_BREAK)
                } else {
                    for (curBrailleElement in curMapping.brailleList) {
                        if (curBrailleElement is PrintPageBrlMapElement) {
                            //Check both the origional page and the translated braille equivelent
                            val origPage = (curBrailleElement.node as Element).getAttributeValue("printPage")
                            val braillePage = curBrailleElement.node.value
                            printPageBuilder.append("PrintPageMapElement: ").append(origPage).append(" - ")
                                .append(braillePage)
                                .append(LINE_BREAK)
                        } else if (curBrailleElement is BraillePageBrlMapElement) {
                            val page = curBrailleElement.node.value
                            braillePageBuilder.append("BraillePageMapElement: ").append(page)
                                .append(LINE_BREAK)
                        }
                    }
                }
            }
        }
        printPageLabel.text = printPageBuilder.toString()
        braillePageLabel.text = braillePageBuilder.toString()
        shell.open()
    }
}
