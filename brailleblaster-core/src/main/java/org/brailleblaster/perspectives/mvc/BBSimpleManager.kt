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
package org.brailleblaster.perspectives.mvc

import nu.xom.Document
import org.brailleblaster.exceptions.OutdatedMapListException
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.perspectives.mvc.menu.MenuManager.buildMenu
import org.brailleblaster.perspectives.mvc.menu.MenuManager.disposeMenu
import org.brailleblaster.perspectives.mvc.modules.views.AbstractModule
import org.brailleblaster.settings.UTDManager
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.Shell
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class BBSimpleManager {
    private var privCurrentSelection: XMLSelection? = null
    private val listenersByClass: MutableMap<Class<out SimpleListener>, MutableList<SimpleListener>> = mutableMapOf()

    val currentCaret: XMLNodeCaret
        get() = checkSelection().start

    val currentSelection: XMLSelection
        get() = checkSelection()

    private fun checkSelection(): XMLSelection {
        try {
            val cs = requireNotNull(privCurrentSelection)
            requireNotNull(
                cs.start.node.document
            ) { "Start node " + privCurrentSelection!!.start.node.toXML() + " is not attached to document" }
            requireNotNull(
                cs.end.node.document
            ) { "End node ${privCurrentSelection!!.end.node.toXML()} is not attached to document" }
            return cs
        } catch (e: NullPointerException) {
            throw OutdatedMapListException("Selection has invalid node", e)
        }
    }
    abstract val utdManager: UTDManager

    abstract val doc: Document

    abstract val manager: Manager

    val listeners: List<SimpleListener>
        get() = listenersByClass.values.flatten()

    fun dispatchEvent(event: SimpleEvent) {
        log.debug("Event: {}", event, RuntimeException("event"))
        event.manager = this

        //TODO: validation
        if (event is XMLCaretEvent) {

            //			if (currentSelection != null && (cEvent.start != cEvent.end)) {
//				currentSelection = new XMLSelection(currentSelection.start, cEvent.end);
//			}
//			else {
            privCurrentSelection = XMLSelection(event.start, event.end)
            //			}
            checkSelection()
        }


        listeners.filter { it !is AbstractModule || !(it.self(event.sender)) || event is ModifyEvent }.forEach {
            it.onEvent(event)
        }
    }

    fun registerModules(eventHandlers: Sequence<SimpleListener>) {
        eventHandlers.forEach { registerModule(it) }
    }

    fun registerModule(eventHandler: SimpleListener) {
        log.trace("Registered listener: ${eventHandler.javaClass.simpleName}")
        listenersByClass.getOrPut(eventHandler.javaClass, ::mutableListOf).add(eventHandler)
    }

    fun <N : SimpleListener> getModule(clazz: Class<N>): N? {
        @Suppress("UNCHECKED_CAST")
        return listenersByClass[clazz]?.lastOrNull() as N?
    }

    fun initMenu(shell: Shell) {
        if (WPManager.getInstance().isMenuInitialized) {
            disposeMenu(shell.menuBar)
        }
        val shellMenu = Menu(shell, SWT.BAR)
        dispatchEvent(BuildMenuEvent(Sender.SIMPLEMANAGER))
        buildMenu(shellMenu)
        shell.menuBar = shellMenu
    }

    fun interface SimpleListener {
        fun onEvent(event: SimpleEvent)
    }

    val isSelectionNotSet: Boolean
        get() = privCurrentSelection == null

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BBSimpleManager::class.java)
    }
}
