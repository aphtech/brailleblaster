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
package org.brailleblaster.search

import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBXUtils
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.stylers.StyleHandler
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.eclipse.swt.graphics.Point
import java.util.*

class Replace {

    var replaceClickCount = 0
    var doExtraFind = false

    fun replace(m: Manager, click: Click): Boolean {
        //println("Replace.replace; clickCount:" + replaceClickCount)
        replaceClickCount++
        val domCon = DOMControl(click)
        val viewCon = ViewControl(m, click)

        //println("Selected text size: " + m.text.selectedText.size)

        val node: Node? = viewCon.checkToReplaceCurrent()

        if (node != null) {
            //println("Node is not null")
            if (!SearchUtils.checkUneditable(node)) {
                //println("Node is editable")
                replaceControl(m, click, domCon, viewCon)

                //Tell the manager the document has changed - but haven't we done this already?
                m.waitForFormatting(true)

                //Update the view state and click object
                val end = viewCon.currentViewState
                click.replaced = end
                //I'm not sure that SavedSearches actually works as expected
                SavedSearches.addToMemory(click)

                //Using find() here advances the text selection. It's stupid but it works.
                val fm = Find()
                if (doExtraFind) {
                    //Equally stupid is this solution: performing an extra find() when replacing the same case-sensitive word
                    fm.find(m, click, domCon, viewCon)
                    doExtraFind = false
                }

                return fm.find(m, click, domCon, viewCon)
            } else {
                return false
            }
        } else {
            //println("Node is null, returning false")
            return false
        }
    }


    //The "Brains" of the replace function. Deals with calculating text positions and all that horrible stuff.
    private fun replaceControl(m: Manager, click: Click, domCon: DOMControl, viewCon: ViewControl) {
        val selection: Point = m.textView.selection
        val array: Array<TextMapElement> = m.mapList.getElementsOneByOne(selection.x, selection.y).toTypedArray()
        val acrossNodes = array.size > 1
        val nodeList: MutableList<Node> = ArrayList()
        //println("ReplaceControl initialized")

        val replaceString = click.settings.replaceString
        if (!replaceString.isNullOrEmpty()) {
            val findString = click.settings.findString
            //println("replaceHasText")

            //Quirk of leaving this out is that it means searches are *always* case-sensitive.
            //Want a way to make that not so...
            //dealWithReplaceCase(click, selectionText)

            //println("fnr for ${click.settings.findString}, replace with ${click.settings.replaceString}")

            if (!click.settings.isFindCaseSensitive) {
                //println("Search is NOT case-sensitive")
                if (replaceString.lowercase() == findString?.lowercase()) {
                    //println("criteria are the same - Will do an extra find")
                    doExtraFind = true
                }
            } else {
                if (replaceString == findString) {
                    //println("criteria are the same - Will do an extra find")
                    doExtraFind = true
                }
            }


            var n: Node
            var nodeStart: Int
            var nodeEnd: Int
            var selectionStart: Int
            var selectionEnd: Int

            if (acrossNodes) {
                n = viewCon.acrossNodeReplace(array, selection.x, selection.y)
                nodeStart = selection.x
                selectionStart = nodeStart
                nodeEnd = selection.y
                selectionEnd = nodeEnd
            } else {
                n = array[0].node
                nodeStart = array[0].getStart(m.mapList)
                nodeEnd = array[0].getEnd(m.mapList)
                selectionStart = selection.x
                selectionEnd = selection.y
            }

            val viewText: String = m.textView.getText(nodeStart, nodeEnd - 1)
            var before = viewText.substring(0, selectionStart - nodeStart)
            val textWithControlChars = before.length
            //Strip out windows newlines
            before = before
                .replace("\n".toRegex(), "")
                .replace("\r".toRegex(), "")

            val textNoControlChars = before.length
            val controlCharsBeforeText = textWithControlChars - textNoControlChars
            //Strip out windows newlines
            val after = viewText.substring(selectionEnd - nodeStart)
                .replace("\n".toRegex(), "")
                .replace("\r".toRegex(), "")

            (n as Text).value = before + click.settings.replaceString + after

            val oldLength = selectionEnd - selectionStart
            val newLength: Int = replaceString.length
            selectionEnd = selectionEnd - controlCharsBeforeText + (newLength - oldLength)
            selectionStart = nodeStart + before.length

            if (click.settings.replaceHasAttributes()) {
                n = domCon.split(n, nodeStart, nodeEnd, selectionStart, selectionEnd)
            }

            if (n is Text && n.value.isEmpty()) {
                n = BBXUtils.findBlock(n)
            }

            nodeList.add(n)
        } else {
            //println("replace has no text")
            for (textMapElement in array) {
                nodeList.add(textMapElement.node)
            }
        }

        if (click.settings.replaceHasEmphasis()) {
            //println("replaceHasEmphasis")
            for (node in nodeList) {
                SearchUtils.modifyEmphasis(node, click.settings.replaceEmphasisFormatting)
            }
        }

        click.settings.replaceStyleFormatting?.let {
            //println("replaceHasStyle")
            for (value in nodeList) {
                var node: Node? = value.parent
                while (node != null && !BBX.BLOCK.isA(node)) {
                    node = node.parent
                }
                //Might consider modifying this method to be "changeStyle" instead, as that's all you can really do:
                // change one style from another
                if (node != null) {
                    StyleHandler.addStyle(node as Element, it.style, m)
                }
            }
        }

        if (click.settings.replaceHasContainer()) {
            //Containers are currently disabled in the replace menu, as some can't be changed from one to another.
            //println("replaceHasContainer")
            SearchUtils.changeContainer(nodeList, click.settings.replaceContainerFormatting, m)
        }

        m.simpleManager.dispatchEvent(ModifyEvent(Sender.SEARCH, nodeList, true))
    }

    //Function for matching case of the replace term with the case of the word.
    //IE, replacing "THE" with "That" yields "THAT", which is a weird behavior.
    private fun dealWithReplaceCase(click: Click, nodeString: String) {
        //println("Replace Case Start: findString: ${click.settings.findString}, replaceString: ${click.settings.replaceString}")
        val findString = click.settings.findString!!
        val start = nodeString.lowercase().indexOf(findString.lowercase())
        val foundString = nodeString.substring(start, start + findString.length)

        val replaceString = click.settings.replaceString!!
        val newReplaceString = StringBuilder()
        var allcaps = true
        val stop: Int = replaceString.length.coerceAtMost(foundString.length)

        for (element in foundString) {
            if (!Character.isUpperCase(element) && Character.isAlphabetic(element.code)) {
                allcaps = false
                break
            }
        }

        var i = 0
        var j = 0
        while (i < stop) {
            val findChar = foundString[i]
            val replaceChar = replaceString[j]
            if (allcaps || (i == 0 && Character.isUpperCase(findChar))) {
                val converted = replaceChar.uppercaseChar()
                newReplaceString.append(converted)
            } else {
                val converted = replaceChar.lowercaseChar()
                newReplaceString.append(converted)
            }
            i++
            j++
        }

        val leftoverChars = replaceString.length - foundString.length
        if (leftoverChars > 0) {
            var k = 0
            var m = foundString.length
            while (k < leftoverChars) {
                val leftoverChar = replaceString[m]
                // if every character so far has been capitalized, keep up the trend
                val converted: Char = if (allcaps) {
                    leftoverChar.uppercaseChar()
                } else {
                    leftoverChar.lowercaseChar()
                }

                newReplaceString.append(converted)
                k++
                m++
            }
        }

        click.settings.replaceString = newReplaceString.toString()
        //println("Replace Case end: replaceString: findString: ${click.settings.findString}, replaceString: ${click.settings.replaceString}")
    }

}