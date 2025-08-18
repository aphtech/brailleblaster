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
import nu.xom.ParentNode
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBX.VolumeType
import org.brailleblaster.bbx.findBlock
import org.brailleblaster.bbx.isPageNum
import org.brailleblaster.exceptions.BBNotifyException
import org.brailleblaster.frontmatter.VolumeUtils.getVolumeElementsFatal
import org.brailleblaster.frontmatter.VolumeUtils.getVolumeNames
import org.brailleblaster.frontmatter.VolumeUtils.newVolumeElement
import org.brailleblaster.frontmatter.VolumeUtils.updateEndOfVolume
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.WhiteSpaceElement
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.MenuManager
import org.brailleblaster.perspectives.mvc.menu.SubMenuBuilder
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.perspectives.mvc.modules.views.DebugModule
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.internal.xml.XMLHandler2
import org.brailleblaster.util.Notify.showMessage
import org.brailleblaster.util.WorkingDialog
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault

class VolumeInsertModule : SimpleListener {
    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent && DebugModule.enabled) {
            val volumeManagerMenu = SubMenuBuilder(
                MENU_VOLUME_MANAGER_TOP,
                MENU_VOLUME_MANAGER_NAME
            )
            volumeManagerMenu.addSubMenu(
                SubMenuBuilder(volumeManagerMenu, MENU_INSERT_VOLUME)
                    .addItem(
                        VolumeType.VOLUME.volumeMenuName,
                        0
                    ) { e: BBSelectionData ->
                        insertVolume(
                            VolumeType.VOLUME, e.manager
                        )
                    }.addItem(
                        VolumeType.VOLUME_PRELIMINARY.volumeMenuName,
                        0
                    ) { e: BBSelectionData ->
                        insertVolume(
                            VolumeType.VOLUME_PRELIMINARY, e.manager
                        )
                    }.addItem(
                        VolumeType.VOLUME_SUPPLEMENTAL.volumeMenuName,
                        0
                    ) { e: BBSelectionData ->
                        insertVolume(
                            VolumeType.VOLUME_SUPPLEMENTAL, e.manager
                        )
                    }
                    .build()
            )
            MenuManager.add(volumeManagerMenu.build())
        }
    }

    companion object {
        private val localeHandler = getDefault()
        @JvmField
		val MENU_VOLUME_MANAGER_TOP: TopMenu = TopMenu.DEBUG
        const val MENU_VOLUME_MANAGER_NAME: String = "Volume Manager"
        const val MENU_INSERT_VOLUME: String = "Insert"

        val pageNumbers: ArrayList<Element?> = ArrayList()

        private fun insertVolume(volumeType: VolumeType, m: Manager) {
            if (!m.isEmptyDocument) {
                val firstTme = m.sectionList[0].list.firstUsable
                val lastTme = m.sectionList[m.lastSection].list.last()
                val firstTmeParent: ParentNode = firstTme.node.findBlock()
                var currentTme = m.mapList.current

                if (lastTme.getEnd(m.mapList) > currentTme.getStart(m.mapList)) {
                    if (currentTme is WhiteSpaceElement) currentTme = m.mapList.getNext(true)
                    if (firstTmeParent != currentTme.node.findBlock()) {
                        WorkingDialog("Inserting new volume").use { dialog ->
                            m.stopFormatting()
                            val insertedVolume = insertVolumeBeforeElement(volumeType, currentTme.node)
                            val volumes: MutableList<Element> = ArrayList(getVolumeElementsFatal(m.doc))
                            if (volumes.size == 1) {
                                val lastVolume = insertLastVolume(volumeType, m.doc)
                                volumes.add(lastVolume)
                            }
                            updateEndOfVolume(m.doc)
                            if (volumes.size != 2 && insertedVolume == volumes[volumes.size - 2]) {
                                val volumeData = getVolumeNames(volumes)
                                val lastData = volumeData.last()
                                val prevData = volumeData[volumeData.size - 2]
                                // #4764 Make the last volume a sane value
                                if ((lastData.type == VolumeType.VOLUME_PRELIMINARY
                                            && (prevData.type == VolumeType.VOLUME || prevData.type == VolumeType.VOLUME_SUPPLEMENTAL))
                                    || (lastData.type == VolumeType.VOLUME && prevData.type == VolumeType.VOLUME_SUPPLEMENTAL)
                                ) {
                                    BBX.CONTAINER.VOLUME.ATTRIB_TYPE[lastData.element] = prevData.type
                                }
                            }
                        }
                        val toUpdate = ArrayList(getVolumeElementsFatal(m.doc))
                        // Update the page indicator that you may have skipped while in insertVolumeBeforeElement()
                        toUpdate.addAll(pageNumbers)
                        m.simpleManager.dispatchEvent(
                            ModifyEvent(
                                Sender.NO_SENDER,
                                toUpdate,
                                true
                            )
                        )
                    } else {
                        showMessage("Volumes must be added at the end of the volume, not the beginning.")
                    }
                } else {
                    showMessage("Cannot add volume at end of file.")
                }
            } else {
                m.notify(localeHandler["emptyDocMenuWarning"])
            }
        }

        fun insertVolumeBeforeElement(
            type: VolumeType?,
            caret: Node?
        ): Element {
            var stopTag =
                XMLHandler2.nodeToElementOrParentOrDocRoot(caret)
            if (XMLHandler.ancestorVisitorElement(
                    stopTag
                ) { node: Element? -> BBX.CONTAINER.VOLUME.isA(node) } != null
            ) {
                throw BBNotifyException("Cannot insert volume inside another volume division")
            }
            stopTag = XMLHandler.ancestorVisitorElement(
                stopTag
            ) { curAncestor: Element? -> BBX.BLOCK.isA(curAncestor) || BBX.CONTAINER.isA(curAncestor) }
            val previous = FastXPath.preceding(stopTag)
                .filterIsInstance<Element>().firstOrNull { node: Node? -> BBX.BLOCK.isA(node) }
            if (previous.isPageNum()) {
                pageNumbers.add(previous)
                stopTag = previous
            }
            if (previous != null && XMLHandler.ancestorElementIs(previous) { node: Element? ->
                    BBX.CONTAINER.TABLETN.isA(
                        node
                    )
                }) {
                stopTag =
                    XMLHandler.ancestorVisitorElement(previous) { node: Element? -> BBX.CONTAINER.TABLETN.isA(node) }
            }

            val newVolumeElement = newVolumeElement(type)
            val stopTagParent = stopTag!!.parent!!
            stopTagParent.insertChild(newVolumeElement, stopTagParent.indexOf(stopTag))
            return newVolumeElement
        }

        fun insertLastVolume(type: VolumeType?, doc: Document?): Element {
            var root = BBX.getRoot(doc!!)
            run {
                var curElem: Node
                while (BBX.SECTION.isA(root.getChild(root.childCount - 1).also { curElem = it })) {
                    root = curElem as Element
                }
            }

            val newVolumeElement = newVolumeElement(type)
            root.appendChild(newVolumeElement)
            return newVolumeElement
        }
    }
}
