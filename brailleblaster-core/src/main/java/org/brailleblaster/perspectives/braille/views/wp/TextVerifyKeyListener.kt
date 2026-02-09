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

import nu.xom.*
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBXUtils
import org.brailleblaster.bbx.findBlock
import org.brailleblaster.bbx.fixers2.LiveFixer
import org.brailleblaster.exceptions.EditingException
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.numberLine.NumberLine.Companion.middleNumberLine
import org.brailleblaster.math.numberLine.NumberLineConstants
import org.brailleblaster.math.spatial.Matrix.Companion.middleMatrix
import org.brailleblaster.math.spatial.MatrixConstants
import org.brailleblaster.math.spatial.SpatialMathUtils
import org.brailleblaster.math.spatial.SpatialMathUtils.middleSpatialMathPage
import org.brailleblaster.math.template.Template.Companion.middleTemplate
import org.brailleblaster.math.template.TemplateConstants
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.*
import org.brailleblaster.perspectives.braille.mapping.interfaces.Deletable
import org.brailleblaster.perspectives.braille.mapping.interfaces.Uneditable
import org.brailleblaster.perspectives.braille.messages.AdjustLocalStyleMessage.AdjustLinesMessage
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.messages.TabInsertionMessage
import org.brailleblaster.perspectives.braille.stylers.MergeElementHandler.Companion.merge
import org.brailleblaster.perspectives.braille.views.wp.TextRenderer.Companion.setNonBreakingSpaceStyleRange
import org.brailleblaster.perspectives.braille.views.wp.formatters.EditRecorder
import org.brailleblaster.perspectives.mvc.XMLNodeCaret
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.perspectives.mvc.modules.misc.TableSelectionModule.Companion.displayInvalidTableMessage
import org.brailleblaster.tools.LineBreakTool.insertInlineLineBreak
import org.brailleblaster.utd.Style
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.UTDHelper.stripUTDRecursive
import org.brailleblaster.util.LINE_BREAK
import org.brailleblaster.util.Notify
import org.brailleblaster.util.Notify.notify
import org.brailleblaster.util.PILCROW
import org.brailleblaster.util.SoundManager.playSelectionBell
import org.brailleblaster.util.Utils.combineAdjacentTextNodes
import org.brailleblaster.util.WhitespaceUtils.appendLineBreakElement
import org.brailleblaster.util.WhitespaceUtils.convertWhiteSpaceToLineBreaks
import org.brailleblaster.util.WhitespaceUtils.prependLineBreakElement
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ST
import org.eclipse.swt.custom.StyleRange
import org.eclipse.swt.custom.VerifyKeyListener
import org.eclipse.swt.events.VerifyEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.math.min

class TextVerifyKeyListener(
    private val manager: Manager,
    private val textView: TextView,
    private val stateObj: ViewStateObject,
    private val selection: Selection,
    private val validator: Validator,
    private val editRecorder: EditRecorder
) : VerifyKeyListener {
    private val overrides: List<OverrideKeyStroke>

    init {
        overrides = listOf( // Override default SWT behavior
            OverrideKeyStroke(CTRL, 'a', NO_ACTION),  // Traverse event handles movement between views
            OverrideKeyStroke(CTRL + SHIFT, SWT.TAB, NO_ACTION),  // Cut inside of read only elements
            OverrideKeyStroke(
                CTRL,
                'x',
                { e: VerifyEvent -> readOnly(e) && !validCut() },
                NO_ACTION
            ),  // Paste inside of read only elements
            OverrideKeyStroke(
                CTRL,
                'v',
                { e: VerifyEvent -> readOnly(e) && !validPaste() },
                NO_ACTION
            ),  // Shift+Enter inserts soft line break
            OverrideKeyStroke(SHIFT, SWT.CR) {
                checkInlineEnter(
                    textView.view.caretOffset
                )
            },  // Shift+Enter inserts soft line break
            OverrideKeyStroke(SHIFT, SWT.KEYPAD_CR) {
                checkInlineEnter(
                    textView.view.caretOffset
                )
            },  // Override SWT enter code with our own
            OverrideKeyStroke(SWT.NONE, SWT.CR) { e: VerifyEvent -> checkEnter(e, textView.view.caretOffset) },
            OverrideKeyStroke(SWT.NONE, SWT.KEYPAD_CR) { e: VerifyEvent -> checkEnter(e, textView.view.caretOffset) },
            OverrideKeyStroke(SWT.NONE, SWT.TAB) {
                // Issue #5431: Disable SWT.TAB
                // Issue #6629: Meanwhile, replace as tabs may mangle physical embosser output
                val activeSelection = textView.view.selection
                val start = min(activeSelection.x.toDouble(), activeSelection.y.toDouble()).toInt()
                val tabSpaces = "    "
                textView.view.insert(tabSpaces)
                // move cursor like the user typed as it is not updated
                textView.view.caretOffset = start + tabSpaces.length
            },  // Insert non-breaking space
            OverrideKeyStroke(CTRL, SWT.SPACE) {
                // Issue #6519: Handle selected text then nbsp
                val activeSelection = textView.view.selection
                val start = min(activeSelection.x.toDouble(), activeSelection.y.toDouble()).toInt()
                textView.view.insert(TextRenderer.NON_BREAKING_SPACE.toString())
                val nbspRange = StyleRange()
                nbspRange.start = start
                nbspRange.length = 1
                setNonBreakingSpaceStyleRange(nbspRange)
                textView.view.setStyleRange(nbspRange)
                // move cursor like the user typed as it is not updated
                textView.view.caretOffset = start + 1
            },  // Override SWT
            OverrideKeyStroke(CTRL + SHIFT, SWT.HOME) { manager.home() },  // Override SWT
            OverrideKeyStroke(CTRL + SHIFT, SWT.END) { manager.end() },  // Override SWT
            OverrideKeyStroke(SWT.NONE, SWT.INSERT, NO_ACTION),
            OverrideKeyStroke(CTRL, SWT.BS){
                //This really is stupid enough to work.
                textView.view.insert("")
                textView.update(true)
            },
            OverrideKeyStroke(CTRL, SWT.DEL){
                textView.view.insert("")
                textView.update(true)
            }
        )
    }

    override fun verifyKey(e: VerifyEvent) {
        stateObj.oldCursorPosition = textView.view.caretOffset
        stateObj.currentChar = e.keyCode
        stateObj.currentStateMask = e.stateMask
        val currentElement = textView.currentElement

        recordLine()

        if (textView.uiLock) {
            e.doit = false
            return
        }
        // check to clear selection, since selection
        // then arrow press sometimes does not fire caret listener
        // when caret does not move, only selection is removed in view
        if (isArrowKey(e.keyCode) && e.stateMask != SWT.SHIFT) {
            selection.setSelectionLength(0)
        }

        if (openSubmenu(e)) {
            textView.updateContextMenu()
            return
        }

        if (isEditingKey(e) && currentElement is Uneditable) {
            currentElement.blockEdit(manager)
            e.doit = false
            return
        }

        // TODO: When selection is reworked this check for tables should hopefully be unnecessary
        if ((isEditingKey(e) || isDeletionKey(e)) && manager.isTableSelected) {
            displayInvalidTableMessage(manager.wpManager.shell)
            e.doit = false
            return
        }

        if (currentElement != null) {//TODO Ditto
            if ((isEditingKey(e) || isDeletionKey(e) || e.character == SWT.CR) && middleSpatialMathPage(currentElement)
            ) {
                log.error("Editing inside spatial math page on the border of two blocks")
                notify(SpatialMathUtils.USE_EDITOR_WARNING, Notify.ALERT_SHELL_NAME)
                e.doit = false
                return
            }
            if ((isEditingKey(e) || isDeletionKey(e) || e.character == SWT.CR) &&
                middleNumberLine(currentElement)
            ) {
                log.error("Editing inside number line on the border of two blocks")
                notify(NumberLineConstants.USE_EDITOR_WARNING, Notify.ALERT_SHELL_NAME)
                e.doit = false
                return
            }
            if ((isEditingKey(e) || isDeletionKey(e) || e.character == SWT.CR) &&
                middleMatrix(currentElement)
            ) {
                log.error("Editing inside matrix on the border of two blocks")
                notify(MatrixConstants.USE_EDITOR_WARNING, Notify.ALERT_SHELL_NAME)
                e.doit = false
                return
            }
            if ((isEditingKey(e) || isDeletionKey(e) || e.character == SWT.CR) &&
                middleTemplate(currentElement)
            ) {
                log.error("Editing inside template on the border of two blocks")
                notify(TemplateConstants.USE_EDITOR_WARNING, Notify.ALERT_SHELL_NAME)
                e.doit = false
                return
            }

            if (atDocumentBoundary(e.keyCode, currentElement)) {
                playSelectionBell()
                return
            }

            if (e.keyCode == SWT.BS.code) {
                if (e.stateMask == CTRL) {
                    textView.view.invokeAction(ST.SELECT_WORD_PREVIOUS)
                }

                //Allow longer selection lengths if the user is trying to delete a print page indicator
                if ((selection.getSelectionLength() > 0 && currentElement !is PageIndicatorTextMapElement)
                    && !validator.validBackspace(
                        currentElement, stateObj,
                        selection.selectionStart, selection.getSelectionLength()
                    )
                ) {
                    e.doit = false
                } else {
                    try {
                        checkBackspace(e, stateObj.oldCursorPosition)
                    } catch (ex: RuntimeException) {
                        throw EditingException("An error occurred while processing backspace", ex)
                    }
                }
            } else if (e.keyCode == SWT.DEL.code) {
                if (e.stateMask == CTRL) {
                    textView.view.invokeAction(ST.SELECT_WORD_NEXT)
                }

                //Allow longer selection lengths if the user is trying to delete a print page indicator
                if ((selection.getSelectionLength() > 0 && currentElement !is PageIndicatorTextMapElement)
                    && !validator.validDelete(
                        currentElement, stateObj,
                        selection.selectionStart, selection.getSelectionLength()
                    )
                ) {
                    e.doit = false
                } else {
                    try {
                        checkDelete(e)
                    } catch (ex: RuntimeException) {
                        throw EditingException("An error occurred while processing delete", ex)
                    }
                }
            }
        }

        if (!e.doit) {
            return
        }

        overrides.firstOrNull {
            it.stateMask == e.stateMask && it.keyCode == e.keyCode && it.criteria.test(
                e
            )
        }?.let {
            it.onOverride.accept(e)
            e.doit = false
        }
    }

    private fun recordLine() {
        if (selection.getSelectionLength() > 0) {
            editRecorder.recordLine(selection.selectionStart, selection.selectionEnd)
        } else {
            editRecorder.recordLine(
                textView.view.getLine(textView.view.getLineAtOffset(textView.view.caretOffset)),
                textView.view.getLineAtOffset(textView.view.caretOffset)
            )
        }
    }

    private fun atStart(currentElement: TextMapElement, caretOffset: Int): Boolean {
        return currentElement is WhiteSpaceElement || (caretOffset == currentElement.getStart(manager.mapList)
                && (isBeginningOfBlock(currentElement.node) || currentElement is BoxLineTextMapElement
                || currentElement is PageIndicatorTextMapElement))
    }

    private fun atEnd(currentElement: TextMapElement, caretOffset: Int): Boolean {
        return currentElement is WhiteSpaceElement ||
                (caretOffset == currentElement.getEnd(manager.mapList)
                        && (isEndOfBlock(currentElement.node) || currentElement is BoxLineTextMapElement
                        || currentElement is PageIndicatorTextMapElement))
    }

    private fun checkInlineEnter(caretOffset: Int) {
        val currentElement = manager.mapList.getClosest(caretOffset, true)
        if (atStart(currentElement, caretOffset)) {
            checkLineBreak(caretOffset)
        } else if (atEnd(currentElement, caretOffset)) {
            checkLineBreak(caretOffset)
        } else {
            insertInlineLineBreak(manager)
        }
    }

    private fun checkEnter(e: VerifyEvent, caretOffset: Int) {
        try {
            checkLineBreak(textView.view.caretOffset)
        } catch (ex: RuntimeException) {
            throw EditingException("An error occurred while processing a line break", ex)
        }
        e.doit = false
    }

    private fun checkLineBreak(caretOffset: Int) {
        // If user has typed text and then pressed enter, update the views and
        // then move the cursor to the correct location if any line breaks were
        // added
        val calculatedCaretOffset = if (textView.textChanged) {
            val text = textView.view
                .getTextRange(
                    textView.currentElement!!.getStart(manager.mapList),
                    textView.view.caretOffset - textView.currentElement!!.getStart(manager.mapList)
                )
                .replace(LINE_BREAK, "")
            val realOffset = text.length
            textView.update(false)
            textView.setCurrent(textView.view.caretOffset)
            if (textView.currentElement!!.node is Text) {
                manager.simpleManager.dispatchEvent(
                    XMLCaretEvent(
                        Sender.NO_SENDER,
                        XMLTextCaret((textView.currentElement!!.node as Text), realOffset)
                    )
                )
            }
            textView.view.caretOffset
        } else {
            caretOffset
        }

        val currentElement = manager.mapList.current
        val atEnd = atEnd(currentElement, calculatedCaretOffset)
        val atStart = atStart(currentElement, calculatedCaretOffset)
        if (currentElement is WhiteSpaceElement) {
            if (currentElement is Uneditable) {
                (currentElement as Uneditable).blockEdit(manager)
            } else {
                val t = findWhitespace(currentElement)
                val newOffset = if (t!!.getStart(manager.mapList) < currentElement.getStart(manager.mapList)) t.getEnd(
                    manager.mapList
                ) else t.getStart(manager.mapList)
                check(newOffset != calculatedCaretOffset) {  // Break out of infinite loops
                    "Unable to add line break"
                }
                textView.setCurrent(newOffset)
                checkLineBreak(newOffset)
            }
        } else if (atEnd) {
            if (manager.mapList.getNext(false) is ReadOnlyFormattingWhiteSpace) {
                (manager.mapList.getNext(false) as ReadOnlyFormattingWhiteSpace).blockEdit(manager)
                return
            }
            addLineBreak(
                currentElement,
                manager.mapList.findNextNonWhitespace(manager.mapList.indexOf(currentElement) + 1)
            )
            dispatchModifyEvent(currentElement.nodeParent, false)
            if (manager.mapList.getNext(false) != null) {
                val nextTME = manager.mapList.getNext(false)
                try {
                    textView.setCursor(nextTME.getStart(manager.mapList))
                } catch (e: Exception) {
                    throw RuntimeException("Failed on TME $nextTME", e)
                }
                textView.setCurrent(textView.view.caretOffset)
            } else {
                // Potential workaround for issue #4871
                var lastTME = manager.mapList.findPreviousNonWhitespace(manager.mapList.size - 1)
                var lastBlock: Element? = null
                while (lastTME != null) {
                    lastBlock = lastTME.node.findBlock()
                    if (lastBlock != null) {
                        break
                    } else {
                        lastTME = manager.mapList.findPreviousNonWhitespace(manager.mapList.indexOf(lastTME))
                    }
                }
                if (lastBlock == null) {
                    throw RuntimeException("Could not find last block")
                }

                manager.document.settingsManager.applyStyleWithOption(
                    (manager.getStyle(lastBlock) as Style),
                    Style.StyleOption.NEW_PAGES_AFTER,
                    1,
                    lastBlock
                )

                //Break out of containers. See RT 6603
                var lastBlockParent: Node? = lastBlock.parent
                while (BBX.CONTAINER.isA(lastBlockParent)) {
                    lastBlock = lastBlockParent as Element?
                    lastBlockParent = lastBlock!!.parent
                }

                val lastStyle = lastBlock.getAttributeValue("utd-style")
                val newBlock = if (lastStyle != null) {
                    val style = manager.simpleManager.utdManager.getBaseStyle(lastStyle, lastBlock)
                    BBX.BLOCK.STYLE.create(style)
                } else {
                    BBX.BLOCK.DEFAULT.create()
                }
                newBlock.addAttribute(Attribute(LiveFixer.NEWPAGE_PLACEHOLDER_ATTRIB, "true"))
                newBlock.appendChild(PILCROW)
                lastBlock.parent.appendChild(newBlock)

                dispatchModifyEvent(true, lastBlock, newBlock)

                // now find our TME and map
                val pilcrowTME =
                    manager.mapList.getPrevious(manager.mapList.size - 1, true)
                try {
                    textView.setCursor(pilcrowTME.getStart(manager.mapList) + pilcrowTME.textLength())
                } catch (e: Exception) {
                    throw RuntimeException("Failed on TME $pilcrowTME", e)
                }
                textView.setCurrent(textView.view.caretOffset)
            }
        } else if (atStart) {
            if (manager.mapList.getPrevious(false) is ReadOnlyFormattingWhiteSpace) {
                (manager.mapList.getPrevious(false) as ReadOnlyFormattingWhiteSpace).blockEdit(
                    manager
                )
            } else {
                addLineBreak(
                    manager.mapList.findPreviousNonWhitespace(manager.mapList.indexOf(currentElement) - 1),
                    currentElement
                )
                dispatchModifyEvent(currentElement.nodeParent, false)
                textView.setCursor(textView.view.caretOffset + LINE_BREAK.length)
                textView.setCurrent(textView.view.caretOffset)
            }
        } else {
            if (currentElement is Uneditable) {
                (currentElement as Uneditable).blockEdit(manager)
            } else {
                manager.splitElement()
            }
        }
    }

    private fun addLineBreak(t1: TextMapElement?, t2: TextMapElement?) {
        var prepend = true
        // Don't insert a newLine between the boxline and the element it's
        // applied to
        if (t2 is BoxLineTextMapElement) {
            prepend = false
        } else if (t2 == null) {
            // At the end of the document. Do nothing.
            return
        }
        if (t1 != null) {
            convertWhiteSpaceToLineBreaks(t1, t2, manager.mapList)
        }
        if (prepend) {
            prependLineBreakElement(t2.node)
        } else if (t1 != null) {
            appendLineBreakElement(t1.node)
        }
    }

    private fun findWhitespace(current: TextMapElement): TextMapElement? {
        var tme = manager.mapList.getNext(true)
        if (tme == null) {
            tme = manager.mapList.getPrevious(true)
        }
        return tme
    }

    //TODO: Simplify this mess
    private fun checkBackspace(e: VerifyEvent, caretOffset: Int) {
        val currentElement = textView.currentElement

        if (caretOffset != stateObj.currentStart) {
            if (checkElementDeletion(e)) {
                return
            }
        }
        if (selection.getSelectionLength() > 0) {
            return
        }

        var cursorUpdated = false //Tracks whether any cursor movement occurred for an edge case
        if (caretOffset == stateObj.currentStart) {
            // if current element is whitespace remove whitespace
            if (currentElement is WhiteSpaceElement) {
                val currentOffset = textView.view.caretOffset
                removeWhitespace(currentElement, false)

                if (currentOffset >= LINE_BREAK.length) {
                    textView.setCursor(currentOffset - LINE_BREAK.length)
                    textView.setCurrent(textView.view.caretOffset)
                }
                e.doit = false
                return
            }
            // if space between non-whitespace elements
            if (caretOffset != stateObj.previousEnd) {
                val parent: ParentNode = currentElement!!.nodeParent
                // if inside placeholder, ignore
                if (deletePrecedingInlineLineBreak(currentElement.node)) {
                    dispatchModifyEvent(parent.parent, true)
                    cursorUpdated = true
                }
                else if (stateObj.currentStart != stateObj.currentEnd && followsWhiteSpace(currentElement)) {
                    removeWhitespace(manager.mapList.getPrevious(false) as WhiteSpaceElement, false)
                    if (currentElement.node is Text) {
                        manager.simpleManager.dispatchEvent(
                            XMLCaretEvent(Sender.NO_SENDER, XMLTextCaret((currentElement.node as Text), 0))
                        )
                    } else if (e.keyCode == SWT.BS.code) {
                        textView.setCursor(textView.view.caretOffset - System.lineSeparator().length)
                        textView.setCurrent(textView.view.caretOffset)
                    }
                    cursorUpdated = true
                }
                else if (manager.mapList.getPrevious(false) is Deletable) {
                    textView.update(false)
                    val current = manager.mapList.current.node
                    val modifiedNode: Node?
                        = (manager.mapList.getPrevious(false) as? Deletable)?.deleteNode(manager)
                    if (modifiedNode != null) {
                        //deleteNode returns null if it already dispatches a ModifyEvent and returns the parent of
                        //the edited node otherwise
                        manager.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, false, modifiedNode))
                    }
                    if (current != null && manager.mapList.findNode(current) != null) {
                        if (current is Text) {
                            manager.simpleManager.dispatchEvent(
                                XMLCaretEvent(Sender.NO_SENDER, XMLTextCaret(current, 0))
                            )

                            cursorUpdated = true
                        } else if (current is Element) {
                            manager.simpleManager.dispatchEvent(XMLCaretEvent(Sender.NO_SENDER, XMLNodeCaret(current)))
                            cursorUpdated = true
                        }
                    }
                }
                else {
                    // Exception can occur here if maplist.getPrevious returns null
                    // (which it sometimes will until we convert it to Kotlin)
                    // Worth a check to see if we're at the start of the document / maplist is null.
                    // In that case there's nothing to merge, and we should just return.
                    if (manager.mapList.getPrevious(false) == null){
                        playSelectionBell()
                        e.doit = false
                        return
                    }
                    // If we've reached this point, the only thing left for us to try is to merge the two elements.
                    // Before we do that, let's make sure we can actually merge these two elements
                    if (validMerge(currentElement, manager.mapList.getPrevious(false))) {
                        textView.update(false)
                        // If, after updating, our currentElement reference is off, do nothing
                        // (example case: user deleted entire element and then pressed backspace to go to previous line
                        if (currentElement.node === textView.currentElement!!.node) {
                            merge(
                                manager.mapList.getPrevious(false),
                                textView.currentElement!!, manager
                            )
                        } else {
                            if (textView.view.caretOffset >= System.lineSeparator().length) {
                                textView.setCurrent(textView.view.caretOffset - System.lineSeparator().length)
                                textView.setCursor(textView.view.caretOffset - System.lineSeparator().length)
                            }
                        }
                        cursorUpdated = true
                    }
                }
                e.doit = false
            }
            else {
                // We are at the edge between two TME's
                if (manager.mapList.getPrevious(false) is TabTextMapElement) {
                    removeTab(manager.mapList.getPrevious(false) as TabTextMapElement)
                    e.doit = false
                    cursorUpdated = true
                } else if (manager.mapList.getPrevious(false) is Uneditable) {
                    e.doit = false
                    cursorUpdated = true
                }
            }
        }
        else if (currentElement is WhiteSpaceElement && e.start == currentElement.getStart(manager.mapList)) {
            removeWhitespace(currentElement, false)
            e.doit = false
            cursorUpdated = true
        }

        if (!cursorUpdated && caretOffset == textView.view.getOffsetAtLine(textView.view.getLineAtOffset(caretOffset))) {
            // If nothing was changed and the cursor is at the beginning of the line,
            // update any changes and then move the cursor to the end of the previous line.
            val line = textView.view.getLineAtOffset(caretOffset)
            if (line > 0) {
                val newOffset = textView.view.getOffsetAtLine(line - 1) + textView.view.getLine(line - 1).length
                textView.update(false)
                if (newOffset < textView.view.charCount) {
                    textView.setCurrent(newOffset)
                    textView.setCursor(newOffset)
                }
            }
            e.doit = false
        }
    }

    //TODO: Simplify this mess
    private fun checkDelete(e: VerifyEvent) {
        var caretOffset = stateObj.oldCursorPosition
        if (caretOffset != stateObj.currentEnd) {
            if (checkElementDeletion(e)) {
                return
            }
        }
        if (selection.getSelectionLength() > 0) {
            return
        }
        // Do nothing if we're at the end of the document
        if (caretOffset + 1 >= textView.view.text.length) {
            e.doit = false
            return
        }
        if (caretOffset == stateObj.nextStart && manager.mapList.getNext(false) !is WhiteSpaceElement
            && manager.mapList.getNext(false) !is Deletable
            && manager.mapList.getNext(false) !is Uneditable
        ) {
            //If an update is needed, process update before sending delete event
            textView.update(false)
            caretOffset = stateObj.oldCursorPosition
        }
        if (caretOffset == stateObj.currentEnd) {
            e.doit = true
            val currentElement = textView.currentElement
            // if current element is whitespace remove whitespace
            if (currentElement is WhiteSpaceElement) {
                val currentOffset = textView.view.caretOffset
                removeWhitespace(currentElement, true)
                if (currentOffset >= LINE_BREAK.length) {
                    textView.setCursor(currentOffset - LINE_BREAK.length)
                    textView.setCurrent(textView.view.caretOffset)
                }
                e.doit = false
                return
            }

            //Handle deleting pilcrows in a newPagePlaceholder
            val nextTME = manager.mapList.getNext(false)
            if (nextTME.node != null) {
                val currNode = nextTME.node
                if (currNode.value == PILCROW && (currNode.parent as Element).getAttribute("newPagePlaceholder") != null) {
                    val parent = currNode.parent.parent as Element
                    nextTME.node.parent.detach()
                    manager.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, false, parent))
                    e.doit = false
                    return
                }
            }

            if (nextTME is Deletable) {
                textView.update(false)

                if (manager.mapList.contains(nextTME)) {
                    val modifiedNode = (nextTME as Deletable).deleteNode(manager)
                    if (modifiedNode == null) {
                        manager.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, false))
                    } else {
                        manager.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, false, modifiedNode))
                    }
                }
                e.doit = false
                return
            }
            if (textView.textChanged) {
                //If entire block was deleted, stop processing
                if (stateObj.currentStart == stateObj.currentEnd) {
                    e.doit = false
                }
                textView.update(false)
                if (!e.doit) {
                    return
                }
            }
            var newOffset = nextTME.getStart(manager.mapList)
            if (newOffset == caretOffset) {
                newOffset++
            }
            // Make the textView stop redrawing to hide the fact we're moving the cursor
            textView.view.setRedraw(false)
            textView.setCurrentElement(newOffset)
            checkBackspace(e, newOffset)
            // Move the cursor back to where it was
            textView.setCurrentElement(caretOffset)
            textView.view.setRedraw(true)
        }
    }

    private fun checkElementDeletion(e: VerifyEvent): Boolean {
        val currentElement = textView.currentElement
        //Should only delete elements here if they're singularly selected
        if (textView.isMultiSelected //&& !(currentElement instanceof PageIndicatorTextMapElement)
        ) {
            return false
        }
        if (currentElement is Deletable) {
            val elementStart = currentElement.getStart(manager.mapList)
            val modifiedNode: Node? = (currentElement as Deletable).deleteNode(manager)
            if (modifiedNode != null) {
                manager.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, modifiedNode))
            }
            textView.setCursor(elementStart)
            textView.setCurrent(textView.view.caretOffset)
            e.doit = false
            return true
        } else if (currentElement is Uneditable) {
            (currentElement as Uneditable).blockEdit(manager)
            e.doit = false
            return true
        }
        return false
    }

    /**
     * Finds nearest non-whitespace elements and adjusts line breaks
     * appropriately
     *
     * @param wse
     */
    private fun removeWhitespace(wse: WhiteSpaceElement, delete: Boolean) {
        if (wse is PageBreakWhiteSpaceElement) {
            wse.deleteNode(manager)
            return
        }

        //At bottom of document, so ignore
        if (manager.mapList.getNext(manager.mapList.indexOf(wse), true) == null) {
            return
        }

        if (removeWhitespaceInline(wse, delete)) {
            return
        }

        val wseIndex = manager.mapList.indexOf(wse)
        val prev = manager.mapList.findPreviousNonWhitespace(wseIndex)
        val next = manager.mapList.findNextNonWhitespace(wseIndex)
        if (isInline(prev, wse)) {
            removeInlineWhitespace(prev!!.nodeParent)
            return
        } else if (isInline(next, wse)) {
            removeInlineWhitespace(next!!.nodeParent)
            return
        }

        //converted starts true when doc is empty or at beginning/end of doc, false otherwise
        var converted = prev == null || next == null

        //the following catches an edge case where there is a maplist problem that has combined
        //LineBreakElements and FormattingWhiteSpaceElements between two TMEs. We don't want to run
        //convertWhiteSpaceToLineBreaks more than once, because otherwise pressing backspace
        //will add blank lines instead of delete them (which shouldn't happen even though it's
        //pretty funny)
        if (!converted) {
            for (i in manager.mapList.indexOf(prev)..manager.mapList.indexOf(next)) {
                if (manager.mapList[i] is LineBreakElement) {
                    converted = true
                    break
                }
            }
        }

        if (!converted) {
            convertWhiteSpaceToLineBreaks(prev!!, next!!, manager.mapList)
        }

        var lbe: LineBreakElement? = null
        for (i in manager.mapList.indexOf(prev) + 1 until manager.mapList.indexOf(next)) {
            if (manager.mapList[i] is LineBreakElement
                && !(manager.mapList[i] as LineBreakElement).isEndOfLine
            ) {
                lbe = manager.mapList[i] as LineBreakElement
                break
            }
        }
        if (lbe != null) {
            var modifyNode = if (prev == null) (if (next == null) lbe.nodeParent else next.node) else prev.node
            lbe.node.detach()
            if (UTDElements.BRL.isA(modifyNode)) // Primarily catches boxlines
            {
                modifyNode = modifyNode.parent
            }
            dispatchModifyEvent(modifyNode, false)
        }
    }

    //to remove white space made with shift + enter
    private fun removeWhitespaceInline(currentElement: TextMapElement, delete: Boolean): Boolean {
        var previousNode: Node? = null
        var nextNode: Node? = null
        var workNode: Node? = null
        if (manager.mapList.getPrevious(false) != null) {
            previousNode = manager.mapList.getPrevious(false).node
        }
        if (manager.mapList.getNext(false) != null) {
            nextNode = manager.mapList.getNext(false).node
        }

        val currentNode = manager.simpleManager.currentSelection.start.node

        if (currentElement is FormattingWhiteSpaceElement) {
            if (manager.mapList.getPrevious(true) != null) {
                previousNode = manager.mapList.getPrevious(true).node
            }
            if (manager.mapList.getNext(true) != null) {
                nextNode = manager.mapList.getNext(true).node
            }
        } else if (manager.mapList.getNext(false) is FormattingWhiteSpaceElement && delete) {
            previousNode = currentNode
            if (manager.mapList.getNext(true) != null) {
                nextNode = manager.mapList.getNext(true).node
            }
        } else if (manager.mapList.getPrevious(false) is FormattingWhiteSpaceElement && !delete) {
            nextNode = currentNode
            if (manager.mapList.getPrevious(true) != null) {
                previousNode = manager.mapList.getPrevious(true).node
            }
        }

        if (previousNode == null || previousNode == currentNode) {
            workNode = FastXPath.followingAndSelf(currentNode).firstOrNull { n: Node ->
                (BBX.INLINE.LINE_BREAK.isA(n)
                        && !UTDElements.BRL.isA(n.parent)
                        && !UTDElements.BRL_PAGE_NUM.isA(n.parent)
                        && !UTDElements.BRLONLY.isA(n.parent))
            }
        } else if (nextNode == null || nextNode == currentNode) {
            workNode = FastXPath.precedingAndSelf(currentNode).firstOrNull { n: Node ->
                (BBX.INLINE.LINE_BREAK.isA(n)
                        && !UTDElements.BRL.isA(n.parent)
                        && !UTDElements.BRL_PAGE_NUM.isA(n.parent)
                        && !UTDElements.BRLONLY.isA(n.parent))
            }
        }

        if (workNode != null && BBX.INLINE.LINE_BREAK.isA(workNode)) {
            val style = manager.getStyle(workNode)
            if (style != null && style.linesBefore > 1) {
                try {
                    manager.dispatch(
                        AdjustLinesMessage(
                            (workNode as Element?)!!,
                            true,
                            style.linesBefore - 1
                        )
                    )
                    return true
                } catch (e1: RuntimeException) {
                    throw EditingException("An error occurred while processing Inline delete.", e1)
                }
            }
        }

        return false
    }

    private fun removeTab(tab: TabTextMapElement) {
        textView.update(false)
        val newOffset = tab.getStart(manager.mapList)
        val parent = tab.nodeParent
        tab.node.detach()
        stripUTDRecursive(parent)
        combineAdjacentTextNodes(parent)
        dispatchModifyEvent(parent, true)
        textView.setCursor(newOffset)
        textView.setCurrent(textView.view.caretOffset)
    }

    private fun removeInlineWhitespace(e: Element) {
        manager.dispatch(TabInsertionMessage(-1, e))
    }

    private fun dispatchModifyEvent(modifiedNode: Node, translate: Boolean) {
        dispatchModifyEvent(translate, modifiedNode)
    }

    private fun dispatchModifyEvent(translate: Boolean, vararg modifiedNodes: Node) {
        manager.stopFormatting()
        manager.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, translate, *modifiedNodes))
    }

    private fun isEditingKey(e: VerifyEvent): Boolean {
        return e.keyCode in 32..126 || e.keyCode == SWT.TAB.code && e.stateMask != SWT.MOD1 && e.stateMask != SWT.MOD2
    }

    private fun isDeletionKey(e: VerifyEvent): Boolean {
        return e.keyCode == SWT.DEL.code || e.keyCode == SWT.BS.code
    }

    private fun readOnly(e: VerifyEvent): Boolean {
        val t = textView.currentElement
        return (t is BoxLineTextMapElement
                || (t is PageIndicatorTextMapElement && !(e.keyCode == SWT.BS.code || e.keyCode == SWT.DEL.code))
                || t is ImagePlaceholderTextMapElement)
    }

    private fun isInline(t: TextMapElement?, wse: WhiteSpaceElement): Boolean {
        if (t == null) {
            return false
        }
        val line1 = textView.view.getLineAtOffset(t.getStart(manager.mapList))
        val line2 = textView.view.getLineAtOffset(wse.getStart(manager.mapList))

        return line1 == line2
    }

    /*
     * Return true if tme follows a formatting white space or a line break that
     * isn't at the end of a line
     */
    private fun followsWhiteSpace(tme: TextMapElement): Boolean {
        val index = manager.mapList.indexOf(tme)
        if (index == 0) {
            return false
        }
        val prev = manager.mapList.getPrevious(index, false)
        return index > 0 && (prev is LineBreakElement || ((prev is FormattingWhiteSpaceElement)
                && prev !is PaintedWhiteSpaceElement && prev !is PageBreakWhiteSpaceElement))
    }

    private fun deletePrecedingInlineLineBreak(n: Node): Boolean {
        for (node in FastXPath.preceding(n)) {
            //If at the document level, abort
            if (node.parent == null) {
                return false
            }
            //Brl elements aren't in the view, so we don't care about them
            if (UTDElements.BRL.isA(node)
                || UTDElements.BRL.isA(node.parent)
                || UTDElements.BRLONLY.isA(node.parent)
            ) {
                continue
            }

            //Text node lies between the line break and the start node, so do nothing
            if (node is Text) {
                return false
            }

            //Line break occurred before text node, so delete it
            if (BBX.INLINE.LINE_BREAK.isA(node) //					|| UTDElements.NEW_LINE.isA(node)
            ) {
                val parent = node.parent as Element
                node.detach()
                stripUTDRecursive(parent)
                combineAdjacentTextNodes(parent)
                return true
            }
        }
        return false
    }

    private fun openSubmenu(e: VerifyEvent): Boolean {
        return e.stateMask == SWT.MOD2 && e.keyCode == SWT.F10
    }

    private fun validCut(): Boolean {
        return validator.validCut(
            textView.currentElement!!, stateObj, selection.selectionStart,
            selection.getSelectionLength()
        )
    }

    private fun validPaste(): Boolean {
        return validator.validPaste(
            textView.currentElement, stateObj, selection.selectionStart,
            selection.getSelectionLength()
        )
    }

    private fun validMerge(t1: TextMapElement, t2: TextMapElement): Boolean {
        return isMergable(t1) && isMergable(t2) && t1.node != null && t2.node != null && t1.node.findBlock() != null && t1.node.findBlock() !== t2.node.findBlock()
    }

    private fun isMergable(t: TextMapElement?): Boolean {
        return t != null && !(t is BoxLineTextMapElement || t is PageIndicatorTextMapElement
                || t is WhiteSpaceElement)
    }

    private fun atDocumentBoundary(keyCode: Int, currentElement: TextMapElement): Boolean {
        return when (keyCode) {
            SWT.ARROW_UP if isBeginningOfDocument(currentElement) -> {
                true
            }
            SWT.ARROW_LEFT if textView.view.caretOffset == 0 && isBeginningOfDocument(currentElement) -> {
                true
            }
            SWT.ARROW_DOWN if isEndOfDocument(currentElement) -> {
                true
            }
            else -> {
                keyCode == SWT.ARROW_RIGHT && textView.view.caretOffset == textView.view.charCount && isEndOfDocument(
                    currentElement
                )
            }
        }
    }

    private fun isBeginningOfDocument(t: TextMapElement): Boolean {
        if (textView.view.getLineAtOffset(textView.view.caretOffset) == 0) {
            return manager.indexOf(0, t) == 0
        }

        return false
    }

    private fun isEndOfDocument(t: TextMapElement): Boolean {
        if (textView.view.getLineAtOffset(textView.view.caretOffset) == textView.view.lineCount - 1) {
            // check if last section index is in view
            if (manager.viewInitializer.findLast() == manager.viewInitializer.sectionList.size - 1) {
                // check if current element is last in maplist in view
                return manager.indexOf(manager.viewInitializer.findLast(), t) == manager.mapList.size - 1
            }
        }

        return false
    }

    private fun isArrowKey(keyCode: Int): Boolean {
        return keyCode == SWT.ARROW_DOWN || keyCode == SWT.ARROW_LEFT || keyCode == SWT.ARROW_RIGHT || keyCode == SWT.ARROW_UP
    }

    private class OverrideKeyStroke(
        val stateMask: Int, val keyCode: Int, val criteria: Predicate<VerifyEvent>,
        val onOverride: Consumer<VerifyEvent>
    ) {
        constructor(stateMask: Int, keyCode: Int, override: Consumer<VerifyEvent>) : this(
            stateMask,
            keyCode,
            Predicate<VerifyEvent> { true },
            override
        )

        constructor(stateMask: Int, key: Char, override: Consumer<VerifyEvent>) : this(
            stateMask,
            key.code,
            Predicate<VerifyEvent> { true },
            override
        )

        constructor(
            stateMask: Int, keyCode: Char, test: Predicate<VerifyEvent>,
            override: Consumer<VerifyEvent>
        ) : this(stateMask, keyCode.code, test, override)
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(TextVerifyKeyListener::class.java)

        private val CTRL = SWT.MOD1
        private val SHIFT = SWT.MOD2

        // private final int ALT = SWT.MOD3;
        private val NO_ACTION = Consumer { _: VerifyEvent -> }

        @JvmStatic
        fun isBeginningOfBlock(n: Node): Boolean {
            var parent = n
            if (BBX.CONTAINER.TABLE.isA(n)) {
                return true
            }
            while (!BBX.BLOCK.isA(parent)) {
                val newParent = parent.parent
                var index = newParent.indexOf(parent)
                if (MathModuleUtils.isMath(n)) {
                    index = BBXUtils.getIndexInBlock(n)
                }
                parent = newParent
                for (sibling in index - 1 downTo 0) {
                    val siblingNode = newParent.getChild(sibling)
                    if (siblingNode is Text) {
                        return false
                    } else if (siblingNode is Element && !UTDElements.BRL.isA(siblingNode)) {
                        if (XMLHandler.childrenRecursiveNodeVisitor(siblingNode) { c: Node? -> c is Text } != null) {
                            return false
                        }
                    }
                }
            }
            return true
        }

        @JvmStatic
        fun isEndOfBlock(n: Node): Boolean {
            var parent = n
            if (BBX.CONTAINER.TABLE.isA(n)) {
                return true
            }
            while (!BBX.BLOCK.isA(parent)) {
                val newParent = parent.parent
                    ?: //Null check to prevent exception - bug #34189
                    return true

                var index = newParent.indexOf(parent)
                if (MathModuleUtils.isMath(n)) {
                    index = BBXUtils.getIndexInBlock(n)
                }
                parent = newParent
                for (sibling in index + 1 until newParent.childCount) {
                    val siblingNode = newParent.getChild(sibling)
                    if (siblingNode is Text) {
                        return false
                    } else if (siblingNode is Element && !UTDElements.BRL.isA(siblingNode)) {
                        if (XMLHandler.childrenRecursiveNodeVisitor(siblingNode) { c: Node? -> c is Text }
                            != null) {
                            return false
                        }
                    }
                }
            }
            return true
        }
    }
}
