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

import nu.xom.Document
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.BBIni
import org.brailleblaster.Main.handleFatalException
import org.brailleblaster.bbx.BBX
import org.brailleblaster.document.DocumentSnapshot
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.eventQueue.EventFrame
import org.brailleblaster.perspectives.braille.eventQueue.QueueManager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.events.ModifyEvent.Companion.canUndo
import org.brailleblaster.perspectives.mvc.events.ModifyEvent.Companion.resetUndoable
import org.brailleblaster.perspectives.mvc.events.ModularEvent
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.MenuManager
import org.brailleblaster.perspectives.mvc.menu.SharedItem
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.tools.MenuTool
import org.brailleblaster.utd.ActionMap
import org.brailleblaster.utd.StyleMap
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.util.Notify.showException
import org.brailleblaster.util.Utils
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Display
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private val localeHandler = getDefault()

class UndoTool(private val module: UndoRedoModule) : MenuTool {
    override val topMenu = TopMenu.EDIT
    override val title = localeHandler["&Undo"]
    override val accelerator = SWT.MOD1 or 'Z'.code
    override val sharedItem = SharedItem.UNDO
    override fun onRun(bbData: BBSelectionData) {
        module.undo(bbData)
    }
}
class RedoTool(private val module: UndoRedoModule) : MenuTool {
    override val topMenu = TopMenu.EDIT
    override val title = localeHandler["&Redo"]
    override val accelerator = SWT.MOD1 or 'Y'.code
    override val sharedItem = SharedItem.REDO
    override fun onRun(bbData: BBSelectionData) {
        module.redo(bbData)
    }
}
class UndoRedoModule(m: Manager) : SimpleListener {
    private val undoRun: AddToUndoQueueRunnable
    private var takeSnapshotLock: CountDownLatch? = null
    private val queueManager: QueueManager = QueueManager()
    private var lastDoc: DocumentSnapshot? = null

    init {
        undoRun = AddToUndoQueueRunnable()
        val newThread = Thread(undoRun)
        newThread.setName("undo-" + m.instanceId)
        newThread.setDaemon(true)
        newThread.start()
    }

    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent) {
            MenuManager.add(UndoTool(this))
            MenuManager.add(RedoTool(this))
        } else if (event is ModifyEvent) {
            if (canUndo()) {
                //On every ModifyEvent, create a copy of the document and add it to the undo queue
                val copyDoc: Document?
                try {
                    copyDoc = event.manager.manager.lastCopiedDoc
                    if (copyDoc == null) {
                        return
                    }
                } catch (e: NullPointerException) {
                    log.error("Potential reformatter thread race, see Issue #5259", e)
                    return
                }
                undoRun.addToQueue(event, copyDoc)
            } else {
                resetUndoable()
            }
        }
    }

    fun redo(bbData: BBSelectionData) {
        bbData.manager.stopFormatting()
        waitForUndoThread()
        queueManager.redo(bbData.manager)
    }

    fun undo(bbData: BBSelectionData) {
        bbData.manager.stopFormatting()
        waitForUndoThread()
        queueManager.undo(bbData.manager)
    }

    fun addUndoEvent(frame: EventFrame?) {
        queueManager.addUndoEvent(frame!!)
    }

    fun addRedoEvent(frame: EventFrame?) {
        queueManager.addRedoEvent(frame!!)
    }

    fun peekUndoEvent(): EventFrame? {
        return queueManager.peekUndoEvent()
    }

    fun peekRedoEvent(): EventFrame? {
        return queueManager.peekRedoEvent()
    }

    fun popUndoEvent(): EventFrame {
        return queueManager.popUndoEvent()
    }

    private fun copyDocumentNoBraille(doc: Document, m: Manager) {
        m.document.removeAllBraille(doc.rootElement)
        lastDoc = DocumentSnapshot(doc)
    }

    fun copyDocument(doc: Document?) {
        lastDoc = DocumentSnapshot(doc!!)
    }

    fun waitForUndoThread() {
        if (takeSnapshotLock != null) {
            try {
                takeSnapshotLock!!.await()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun getParent(documentSnapshot: Document, indexes: List<Int>): Element? {
        var e = if (indexes.isNotEmpty()) documentSnapshot.getChild(indexes[0]) as Element else null
        for (i in 1 until indexes.size) {
            if (indexes[i] >= e!!.childCount) {
                //XOM does not always throw an exception in this case and will sometimes just return null
                throw IndexOutOfBoundsException("Element has no child at index " + indexes[i])
            }
            e = e.getChild(indexes[i]) as Element
        }
        return e
    }

    fun closeUndoThread() {
        undoRun.close()
    }

    private inner class AddToUndoQueueRunnable : Runnable {
        var waitForWork: CountDownLatch

        @Volatile
        var close: Boolean
        var queue: LinkedList<UndoState>

        init {
            waitForWork = CountDownLatch(1)
            close = false
            queue = LinkedList()
        }

        /**
         * Add a ModifyEvent to the undo queue
         */
        fun addToQueue(ev: ModifyEvent, doc: Document) {
            //Current section of the sectionList
            val sectionIndex =
                if (ev.isQueueEvent) ev.sectionIndex else ev.manager.manager.viewInitializer.startIndex
            //Current cursor position
            val textOffset = if (ev.isQueueEvent) ev.textOffset else ev.manager.manager.text.view.caretOffset
            queue.add(UndoState(ev, doc, sectionIndex, textOffset))
            waitForWork.countDown()
        }

        override fun run() {
            while (true) {
                try {
                    var doneWaiting = false
                    while (!doneWaiting) {
                        doneWaiting = waitForWork.await(10, TimeUnit.SECONDS)
                        if (close) return
                    }
                } catch (_: InterruptedException) {
                    return
                }
                waitForWork = CountDownLatch(1)
                takeSnapshotLock = CountDownLatch(1)
                try {
                    if (close) return
                    while (!queue.isEmpty()) {
                        val curState = queue.pop()
                        addToUndoQueue(curState.ev, curState.sectionIndex, curState.textOffset, curState.doc)
                    }
                } catch (e: Exception) {
                    if (BBIni.debugging) {
                        handleFatalException(e)
                    } else {
                        //This is async because if the main thread is waiting on the
                        //takeSnapshotLock, BB will be deadlocked
                        Display.getDefault().asyncExec { showException(e) }
                    }
                } finally {
                    takeSnapshotLock!!.countDown()
                }
            }
        }

        fun close() {
            close = true
        }

        private fun addToUndoQueue(event: ModifyEvent, sectionIndex: Int, textOffset: Int, doc: Document) {
            val newEventFrame = createEventFrame(event, sectionIndex, textOffset)
            val isUndoEvent = event.sender == Sender.UNDO_QUEUE
            if (newEventFrame != null) {
                //If this modify event originated from pressing undo, put this frame into the redo queue
                if (isUndoEvent) {
                    addRedoEvent(newEventFrame)
                } else {
                    addUndoEvent(newEventFrame)
                }

                //Create a new copy of the document to use in createEventFrame
                if (event.manager.manager.isEmptyDocument) {
                    copyDocumentNoBraille(doc, event.manager.manager)
                } else {
                    copyDocument(doc)
                }
            }
        }

        /**
         * Find the list of affected sections and add them to an EventFrame along with
         * the action/style maps
         */
        private fun createEventFrame(ev: ModifyEvent, sectionIndex: Int, textOffset: Int): EventFrame? {
            if (ev.changedNodes.isEmpty()) {
                log.error("Could not add event to undo")
                return null
            }
            val startTime = System.currentTimeMillis()
            val event = EventFrame()
            val sections = ArrayList<Element?>()
            //A path of indexes to go from the root element to the section
            val indexes = ArrayList<List<Int>>()
            changedNodesLoop@ for (n in ev.changedNodes) {
                if (n.document == null) {
                    continue
                }
                val parent = getSection(n)
                //Ensure that we haven't already counted this section or an ancestor
                if (!sections.contains(parent)) {
                    for (i in sections.indices) {
                        if (isDescendedFrom(parent, sections[i])) {
                            continue@changedNodesLoop
                        }
                        if (isDescendedFrom(sections[i], parent)) {
                            sections.removeAt(i)
                        }
                    }
                    sections.add(parent)
                    indexes.add(makeIndexes(parent))
                }
            }

            //Create another copy of the document
            val documentSnapshot = lastDoc!!.snapshot
            val actionMap = DocumentUTDConfig.NIMAS.getConfigElement(documentSnapshot, ActionMap::class.java)
            val styleMap = DocumentUTDConfig.NIMAS.getConfigElement(documentSnapshot, StyleMap::class.java)

            //Add each section to the EventFrame
            for (i in sections.indices) {
                event.addEvent(
                    ModularEvent(
                        getParent(documentSnapshot, indexes[i])!!,
                        actionMap,
                        styleMap,
                        indexes[i],
                        sectionIndex,
                        textOffset
                    )
                )
            }
            log.trace("Took document snapshot in " + Utils.runtimeToString(startTime))
            return event
        }

        private fun isDescendedFrom(child: Node?, parent: Node?): Boolean {
            var child = child
            while (child != null) {
                if (child === parent) return true
                child = child.parent
            }
            return false
        }

        private fun makeIndexes(parent: Node?): ArrayList<Int> {
            var parent = parent
            val list = ArrayList<Int>()
            var p = parent!!.parent
            do {
                list.add(0, p!!.indexOf(parent))
                parent = p
                p = p.parent
            } while (p != null)
            return list
        }

        private fun getSection(n: Node): Element? {
            if (n.document == null) {
                throw RuntimeException("node not attached to document " + n.toXML())
            }
            if (n is Document || n.document.rootElement === n) return n.document.rootElement
            if (n is Element && BBX.SECTION.isA(n)) return n
            var section = XMLHandler.ancestorVisitor(n) { parent: Node ->
                //Get the parent section that only has section siblings (see RT#6154)
                if (BBX.SECTION.isA(parent)) {
                    val sectionParent = parent.parent
                    for (i in 0 until sectionParent.childCount) {
                        if (BBX.CONTAINER.isA(sectionParent.getChild(i)) || BBX.BLOCK.isA(sectionParent.getChild(i))) {
                            return@ancestorVisitor false
                        }
                    }
                    return@ancestorVisitor true
                }
                false
            }
            if (section == null) {
                section = n.document.rootElement
            }
            return section as Element?
        }

        private inner class UndoState(
            var ev: ModifyEvent,
            var doc: Document,
            var sectionIndex: Int,
            var textOffset: Int
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(UndoRedoModule::class.java)
    }
}
