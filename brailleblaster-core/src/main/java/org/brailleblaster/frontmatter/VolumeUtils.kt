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
package org.brailleblaster.frontmatter

import nu.xom.Document
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBX.VolumeType
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.utils.swt.EasySWT
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Predicate

/**
 * Tools for working with volumes. If a new volume may be inserted you must call
 * [.updateEndOfVolume]
 */
object VolumeUtils {
    private val log = LoggerFactory.getLogger(VolumeUtils::class.java)
    @JvmStatic
	fun getVolumeElements(doc: Document?): List<Element> {
        return FastXPath.descendant(doc).toList()
            .filterIsInstance<Element>()
            .filter { node -> BBX.CONTAINER.VOLUME.isA(node) }
    }

    @JvmStatic
	fun getVolumeElementsFatal(doc: Document?): List<Element> {
        val volumes = getVolumeElements(doc)
        if (volumes.isEmpty()) throw RuntimeException("Must create volumes first")
        return volumes
    }

    @JvmStatic
	fun newVolumeElement(type: VolumeType?): Element {
        log.debug("Adding new volume")
        val volumeElement = BBX.CONTAINER.VOLUME.create(type)
        getOrCreateVolumeEnd(volumeElement)
        return volumeElement
    }

    fun getOrCreateVolumeEnd(volumeElement: Element): Element? {
        var volumeEnd: Element? = null
        for (curChild in volumeElement.childElements) {
            if (BBX.BLOCK.VOLUME_END.isA(curChild)) {
                volumeEnd = curChild
            }
        }
        if (volumeEnd == null) {
            volumeEnd = BBX.BLOCK.VOLUME_END.create()
            volumeEnd.appendChild("New Volume Placeholder")
            //Must be at start of volume tag, the volumes are split after this
            volumeElement.insertChild(volumeEnd, 0)
        }

        //TODO: Need to update volumes
        return volumeEnd
    }

    @JvmStatic
	fun getOrCreateTPage(volumeElement: Element): Element {
        return volumeElement.childElements.lastOrNull { BBX.CONTAINER.TPAGE.isA(it) }
            ?: createTPage(volumeElement)
    }

    private fun createTPage(volumeElement: Element): Element {
        val result = BBX.CONTAINER.TPAGE.create()
        val volumeEnd = getOrCreateVolumeEnd(volumeElement)
        //TPages always follow volumeEnd
        volumeEnd!!.parent.insertChild(
            result,
            volumeEnd.parent.indexOf(volumeEnd) + 1
        )
        return result
    }

    @JvmStatic
	fun getOrCreateTOC(volumeElement: Element): Element {
        var tocContainer: Element? = null
        for (curChild in volumeElement.childElements) {
            if (BBX.CONTAINER.VOLUME_TOC.isA(curChild)) {
                tocContainer = curChild
            }
        }
        return tocContainer
            ?: BBX.CONTAINER.VOLUME_TOC.create().also {
                //Must be last? At least after volume end
                volumeElement.appendChild(it)
            }
    }

    /**
     * Update volumes in dom per Formats 2011 § 1.5.3
     *
     * @param doc
     * @return All volume elements to be translated
     */
	@JvmStatic
	fun updateEndOfVolume(doc: Document?): List<Element> {
        val volumes = getVolumeElementsFatal(doc)
        for (curVolume in volumes) {
            updateEndOfVolume(curVolume, volumes)
        }
        return volumes
    }

    @JvmOverloads
    fun updateEndOfVolume(
        volumeElement: Element,
        volumes: List<Element> = getVolumeElementsFatal(volumeElement.document)
    ) {
        BBX.CONTAINER.VOLUME.assertIsA(volumeElement)
        val volumeNum = volumes.indexOf(volumeElement)
        if (volumeNum == -1) {
            throw NodeException("Volume doesn't exist in volumes list?", volumeElement)
        }
        val volumeEnd = getOrCreateVolumeEnd(volumeElement)

        //TODO: Hardcoded English Formats 2011 § 1.5.3
        volumeEnd!!.removeChildren()
        val span = BBX.INLINE.EMPHASIS.create(EnumSet.of(EmphasisType.TRANS_NOTE))
        if (volumes.last() === volumeElement) {
            //Assuming the same is the case for the end
            span.appendChild("THE END")
        } else {
            //This needs to be wrapped inside an inline TRANS_NOTE
            span.appendChild(
                "End of "
                        + BBX.CONTAINER.VOLUME.ATTRIB_TYPE[volumeElement].volumeName
                        + " "
                        + getVolumeNumber(volumes, volumeElement)
            )
        }
        volumeEnd.appendChild(span)
    }

    fun getVolumeNumber(volumes: List<Element>, needleVolume: Element): Int {
        BBX.CONTAINER.VOLUME.assertIsA(needleVolume)
        val needleVolumeType = BBX.CONTAINER.VOLUME.ATTRIB_TYPE[needleVolume]
        var counter = 0
        for (volume in volumes) {
            val curVolumeType = BBX.CONTAINER.VOLUME.ATTRIB_TYPE[volume]
            if (volume === needleVolume) {
                //Have counted every volume with this type
                break
            } else if (curVolumeType == needleVolumeType) {
                counter++
            }
        }

        //For 1-based users
        counter++
        return counter
    }

    fun getVolumeNames(volumes: List<Element>): List<VolumeData> {
        val result: MutableList<VolumeData> = ArrayList()
        for (curVolume in volumes) {
            val type = BBX.CONTAINER.VOLUME.ATTRIB_TYPE[curVolume]
            val volumeTypeIndex = getVolumeNumber(volumes, curVolume)
            result.add(
                VolumeData(
                    curVolume,
                    type,
                    type.volumeNameShort + " " + volumeTypeIndex,
                    volumeTypeIndex
                )
            )
        }
        return result
    }

    fun getVolumeName(volumes: List<Element>, needleVolume: Element): VolumeData {
        for (curVolume in volumes) {
            if (curVolume !== needleVolume) {
                continue
            }
            val type = BBX.CONTAINER.VOLUME.ATTRIB_TYPE[curVolume]
            val volumeTypeIndex = getVolumeNumber(volumes, curVolume)
            return VolumeData(
                curVolume,
                type,
                type.volumeNameShort + " " + volumeTypeIndex,
                volumeTypeIndex
            )
        }
        throw NodeException("Volume not found", needleVolume)
    }

    /**
     * Scroll text view to specified volume
     *
     * @param volumeNum Volume number to scroll to
     * @return offset
     */
    fun scrollToVolume(manager: Manager, volumeNum: Int): Int {
        val volumeElement = getVolumeElementsFatal(manager.doc)[volumeNum]
        log.debug("Scrolling to volume $volumeNum")
        manager.text.setListenerLock(true)
        val textView = manager.text.view

        //Find the first text of the volume
        val text = XMLHandler.findFirstText(volumeElement)
        log.debug("scrolling to text " + text.value)

        //Make sure text is in a buffered section
        val nodeSection = manager.getNodeIndexAllSections(text).first
        val currentSection = manager.firstSection
        log.debug("Current section {} found section {}", currentSection, nodeSection)
        if (nodeSection != currentSection) manager.resetSection(nodeSection)

        //Find the offset
        val findNodeText = manager.findNodeText(text)
        val offset =
            findNodeText[0].getStart(manager.mapList) //start ?  : findNodeText.get(findNodeText.size() - 1).end;
        log.debug("Scrolling to offset {}", offset)
        textView.caretOffset = offset

        //Scroll if nessesary
        EasySWT.scrollViewToCursor(textView)
        manager.text.setListenerLock(false)
        textView.setFocus()
        return offset
    }

    @JvmStatic
	fun getVolumeAfterNode(doc: Document, caretNode: Node): Element {
        //Descend into tree and find volume after this node
        //Benchmarked to be faster than a following:: query for volumes
        return FastXPath.descendantFindFirst(doc, object : Predicate<Node> {
            var foundNode = false
            override fun test(t: Node): Boolean {
                if (!foundNode) {
                    if (t === caretNode) {
                        foundNode = true
                    }
                    return true
                } else if (BBX.CONTAINER.VOLUME.isA(t)) {
                    return false
                }
                return true
            }
        }) as Element
    }

    class VolumeData(@JvmField val element: Element, @JvmField val type: VolumeType, @JvmField val nameLong: String, @JvmField val volumeTypeIndex: Int)
}
