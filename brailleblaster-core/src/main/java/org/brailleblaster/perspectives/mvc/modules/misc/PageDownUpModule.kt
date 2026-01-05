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
package org.brailleblaster.perspectives.mvc.modules.misc

import org.brailleblaster.abstractClasses.BBEditorView
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.views.wp.WPView
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.events.BBViewListener
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.VerifyKeyListener
import org.eclipse.swt.events.VerifyEvent

class PageDownUpModule(private val manager: Manager) : SimpleListener, VerifyKeyListener, BBViewListener {
    override fun onEvent(event: SimpleEvent) {
//		if (event instanceof BuildMenuEvent) {
//			MenuManager.addMenuItem(TopMenu.NAVIGATE, "Next Page",  SWT.PAGE_DOWN, e -> nextPage(e.manager), null);
//			MenuManager.addMenuItem(TopMenu.NAVIGATE, "Previous Page",  SWT.PAGE_UP, e -> previousPage(e.manager), null);
//		}
    }

    private fun nextPage(m: Manager) {
        val line = WPView.currentLine
        val pageLine = line % m.document.linesPerPage + 1
        val newLine = line + m.document.linesPerPage
        if (newLine < m.textView.content.lineCount) {
            m.text.setCurrentElement(m.textView.getOffsetAtLine(newLine))
        } else {
            if (m.viewInitializer.sectionList.size > 1) {
                m.text.setListenerLock(true)
                m.text.setCurrentElement(m.mapList.last().getEnd(m.mapList))
                //m.getText().setCurrentElement(m.getText().getCurrentEnd());
                val newPageLine = WPView.currentLine % m.document.linesPerPage + 1
                if (newPageLine != pageLine) {
                    adjustLine(pageLine, newPageLine, m)
                }
                m.text.setListenerLock(false)
            } else {
                m.textView.caretOffset = m.textView.getOffsetAtLine(m.textView.content.lineCount - 1)
            }
        }
        EasySWT.scrollViewToCursor(m.textView)
    }

    private fun previousPage(m: Manager) {
        val line = WPView.currentLine
        val pageLine = line % m.document.linesPerPage + 1
        val newLine = line - m.document.linesPerPage
        if (newLine > 0) {
            m.text.setCurrentElement(m.textView.getOffsetAtLine(newLine))
        } else {
            if (m.viewInitializer.sectionList.size > 1) {
                m.text.setListenerLock(true)
                m.text.setCurrentElement(m.mapList.firstUsable.getStart(m.mapList))
                //m.getText().setCurrentElement(m.getText().getCurrentStart());
                val newPageLine = WPView.currentLine % m.document.linesPerPage + 1
                if (newPageLine != pageLine) {
                    adjustLine(pageLine, newPageLine, m)
                }
                m.text.setListenerLock(false)
            } else {
                m.textView.caretOffset = m.textView.getOffsetAtLine(0)
            }
        }
        EasySWT.scrollViewToCursor(m.textView)
    }

    private fun adjustLine(pageLine: Int, newPageLine: Int, m: Manager) {
        val dif: Int
        if (newPageLine > pageLine) {
            dif = newPageLine - pageLine
            m.textView.caretOffset = m.textView.getOffsetAtLine(WPView.currentLine - dif)
        } else {
            dif = pageLine - newPageLine
            m.textView.caretOffset = m.textView.getOffsetAtLine(WPView.currentLine + dif)
        }
    }

    override fun verifyKey(e: VerifyEvent) {
        if (e.keyCode == SWT.PAGE_DOWN) {
            nextPage(manager)
        }
        if (e.keyCode == SWT.PAGE_UP) {
            previousPage(manager)
        }
    }

    override fun initializeListener(view: BBEditorView) {
        view.view.addVerifyKeyListener(this)
    }

    override fun removeListener(view: BBEditorView) {
        view.view.removeVerifyKeyListener(this)
    }
}