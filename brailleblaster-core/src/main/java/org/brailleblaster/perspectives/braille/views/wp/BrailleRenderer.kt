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
package org.brailleblaster.perspectives.braille.views.wp

import nu.xom.Element
import org.brailleblaster.exceptions.OutdatedMapListException
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.*
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.util.ColorManager
import org.eclipse.swt.custom.StyleRange
import org.eclipse.swt.custom.StyledText

class BrailleRenderer(manager: Manager, private val brailleView: BrailleView) : Renderer(manager, brailleView) {
    var state: RendererState = RendererState(manager.simpleManager.utdManager.engine)
    private var pastFirstNewPage: Boolean
    private var pastLastNewPage: Boolean
    fun add(t: TextMapElement) {
        when (t) {
            is TableTextMapElement -> {
                addTable(t)
            }

            is ImagePlaceholderTextMapElement, is WhiteSpaceElement -> {
                return
            }

            else -> {
                setBraille(t)
            }
        }
    }

    init {
        state.setLineIndentMode(false)
        pastFirstNewPage = false
        pastLastNewPage = false
    }

    fun setBraille(t: TextMapElement) {
        for (bme in t.brailleList) {
            if (bme is NewPageBrlMapElement) {
                if (bme === firstPage) pastFirstNewPage = true
                if (bme === lastPage) pastLastNewPage = true
                state.newPage(bme.node as Element)
                bme.setOffsets(state.charCount, state.charCount)
            } else {
                if (!pastFirstNewPage || pastLastNewPage) {
                    bme.setOffsets(state.charCount, state.charCount)
                    continue
                }
                checkForMoveTo(bme)
                bme.setStart(state.charCount)
                state.addToLine(bme.text)
                bme.setEnd(state.charCount)
            }
        }
    }

    fun addTable(t: TableTextMapElement) {
        state.setTableMode(true)
        for (tcme in t.tableElements) {
            setBraille(tcme)
        }
        state.setTableMode(false)
    }

    fun finishRendering(list: MapList?) {
        state.finishPage()
        val text = state.text
        brailleView.view.text = text
        renderLineIndents(state.lineIndents, brailleView)
        renderNewPages()
        addLastPageIndicator(list!!, brailleView)
        highlightUnknownCharacters(brailleView.view, text)
    }

    private fun checkForMoveTo(bme: BrailleMapElement) {
        val parent = bme.nodeParent
            ?: throw OutdatedMapListException("BME " + bme.node.toXML() + " has no grandparent")
        val brlIndex = parent.indexOf(bme.node)
        if (brlIndex > 0 && UTDElements.MOVE_TO.isA(parent.getChild(brlIndex - 1))) {
            val moveTo = parent.getChild(brlIndex - 1) as Element
            state.moveTo(getHPos(moveTo), getVPos(moveTo))
        }
    }

    private fun renderNewPages() {
        val newPages = state.newPages
        for (pair in newPages) {
            handleNewPageElement(pair.first, pair.second, brailleView)
        }
    }

    companion object {
        /**
         * Highlight '\xFFFF' -like unknown liblouis characters
         * @param text
         */
        private fun highlightUnknownCharacters(view: StyledText, text: String) {
            var start = 0

            // TODO: Hardcoded UEB. No easy way to communicate this from UTD
            while (text.indexOf("`.<", start).also { start = it } != -1) {
                val end = text.indexOf("`.>", start)
                if (end != -1) {
                    val sr = StyleRange()
                    sr.background = ColorManager.getColor(ColorManager.Colors.KHAKI, view)
                    sr.start = start
                    sr.length = end - start +  /*length of end*/3
                    try {
                        view.setStyleRange(sr)
                    } catch (e: Exception) {
                        throw RuntimeException(sr.toString(), e)
                    }
                } else {
                    //Prevent infinite loops when there is a beginning transcriber's note but no end
                    break
                }

                // don't find this again
                start = end
            }
        }
    }
}
