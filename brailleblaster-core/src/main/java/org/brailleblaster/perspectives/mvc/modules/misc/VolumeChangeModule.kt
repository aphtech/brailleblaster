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

import nu.xom.Element
import org.brailleblaster.BBIni
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBX.VolumeType
import org.brailleblaster.frontmatter.VolumeUtils.getVolumeElements
import org.brailleblaster.frontmatter.VolumeUtils.getVolumeName
import org.brailleblaster.frontmatter.VolumeUtils.getVolumeNames
import org.brailleblaster.frontmatter.VolumeUtils.updateEndOfVolume
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.menu.MenuManager
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.menu.SubMenuBuilder
import org.brailleblaster.perspectives.mvc.modules.views.DebugModule
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.exceptions.BBNotifyException
import org.brailleblaster.util.YesNoChoice.Companion.ask
import org.eclipse.swt.SWT

class VolumeChangeModule(private val manager: Manager) : SimpleListener {
    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent && DebugModule.enabled) {
            val volumeManagerMenu = SubMenuBuilder(
                VolumeInsertModule.MENU_VOLUME_MANAGER_TOP,
                VolumeInsertModule.MENU_VOLUME_MANAGER_NAME
            )
            volumeManagerMenu.addSubMenu(
                SubMenuBuilder(
                    volumeManagerMenu,
                    "Change Volume Type"
                )
                    .addItem(
                        VolumeType.VOLUME.volumeMenuName,
                        0
                    ) {
                        changeVolumeType(
                            VolumeType.VOLUME
                        )
                    }.addItem(
                        VolumeType.VOLUME_PRELIMINARY.volumeMenuName,
                        0
                    ) {
                        changeVolumeType(
                            VolumeType.VOLUME_PRELIMINARY
                        )
                    }.addItem(
                        VolumeType.VOLUME_SUPPLEMENTAL.volumeMenuName,
                        0
                    ) {
                        changeVolumeType(
                            VolumeType.VOLUME_SUPPLEMENTAL
                        )
                    }.build()
            )
            volumeManagerMenu.addItem(
                MENU_DELETE_CURRENT_VOLUME,
                SWT.NONE
            ) { deleteCurrentVolume() }
            MenuManager.add(volumeManagerMenu.build())
        }
    }

    private fun changeVolumeType(type: VolumeType) {
        val volumes = getVolumeElements(manager.doc)
        if (volumes.isEmpty()) {
            throw BBNotifyException("Document does not have any volumes")
        }
        val volumeData = getVolumeNames(volumes)
        val curVolume = manager.volumeAtCursor
        val curIndex = volumes.indexOf(curVolume)
        if (curIndex == -1) {
            throw NodeException("Volume not found?", curVolume)
        }
        val curData = volumeData[curIndex]
        if (curData.type == type) {
            throw BBNotifyException("Volume is already a " + saneVolumeName(type))
        }
        if (curIndex != 0) {
            val previousData = volumeData[curIndex - 1]
            if (type.ordinal < previousData.type.ordinal) {
                throw BBNotifyException(
                    "A " + saneVolumeName(type)
                            + " cannot follow a " + saneVolumeName(previousData.type)
                )
            }
        }
        if (curIndex != volumes.size - 1) {
            val nextData = volumeData[curIndex + 1]
            if (type.ordinal > nextData.type.ordinal) {
                throw BBNotifyException(
                    "A " + saneVolumeName(type)
                            + " cannot precede a " + saneVolumeName(nextData.type)
                )
            }
        }
        BBX.CONTAINER.VOLUME.ATTRIB_TYPE[curVolume] = type
        for (volume in volumes) {
            updateEndOfVolume(volume, volumes)
        }
        manager.simpleManager.dispatchEvent(ModifyEvent(Sender.VOLUME_CHANGE, volumes, true))
    }

    private fun deleteCurrentVolume() {
        val curVolume = manager.volumeAtCursor ?: throw BBNotifyException("No volumes found")
        val doc = curVolume.document
        var volumeElements = getVolumeElements(
            manager.doc
        )
        val curVolumeData = getVolumeName(volumeElements, curVolume)
        if (!BBIni.debugging
            && !ask("Delete " + saneVolumeName(curVolumeData.type) + " " + curVolumeData.volumeTypeIndex)
        ) {
            return
        }
        if (volumeElements.size == 2) {
            //Something that can be passed to the formatter
            val parent = volumeElements[0].parent as Element
            //Remove THE END
            for (volumeElement in volumeElements) {
                volumeElement.detach()
            }
            manager.simpleManager.dispatchEvent(ModifyEvent(Sender.VOLUME_CHANGE, listOf(parent), false))
        } else {
            curVolume.detach()
            volumeElements = updateEndOfVolume(doc)
            manager.simpleManager.dispatchEvent(ModifyEvent(Sender.VOLUME_CHANGE, volumeElements, true))
        }
    }

    companion object {
        const val MENU_DELETE_CURRENT_VOLUME = "Delete Current Volume"
        private fun saneVolumeName(volumeType: VolumeType): String {
            return if (volumeType == VolumeType.VOLUME) {
                "Regular Volume"
            } else {
                volumeType.volumeName
            }
        }
    }
}
