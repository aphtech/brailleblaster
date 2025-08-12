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
package org.brailleblaster.util

import nu.xom.Document
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.archiver2.BBXArchiver
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.utd.UTDTranslationEngine
import org.brailleblaster.utd.internal.xml.XMLHandler
import java.io.File
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

class BookSplitter(private var volList: ArrayList<Element>, var manager: Manager, private val destPath: Path) {
    var doc: Document = manager.doc
    private var manifest: Document = Document(Element("Files"))
    private var originalFileName: String = manager.archiver.path.nameWithoutExtension
    var copy: Boolean

    init {
        copy = true
    }

    fun split() {
        removeBraille(doc.rootElement)
        for (i in volList.indices) {
            setFile(i)
            var root: Element
            when (i) {
                0 -> {
                    root = copyFirst(volList[1])
                }
                volList.size - 1 -> {
                    root = copyRest(volList[i], null)
                    root.insertChild(copyHead(), 0)
                }
                else -> {
                    root = copyRest(volList[i], volList[i + 1])
                    root.insertChild(copyHead(), 0)
                }
            }
            val splitDoc = Document(root)
            saveDoc(splitDoc, destPath.resolve(getName(i)))
        }
        setFileName()
        outputManifest()
        manager.refresh()
    }

    private fun copyFirst(end: Element): Element {
        val copy = copyRootElement()
        return copyDocumentFromStart(copy, doc.rootElement, end)
    }

    private fun copyRest(start: Element, end: Element?): Element {
        val copy = copyRootElement()
        return copyDocumentFromPosition(copy, doc.rootElement, start, end)
    }

    private fun copyDocumentFromStart(copyTo: Element, e: Element, stopElement: Element): Element {
        if (e == stopElement) copy = false

        //if(canCopy(e) && e.getChildCount() == 0){
        ///	Element copy = copyElement(e);
        //	copyTo.appendChild(copy);
        //}
        for (i in 0 until e.childCount) {
            if (canCopy(e.getChild(i)) && e.getChild(i) is Element) {
                val child = e.getChild(i) as Element
                if (child == stopElement) {
                    copy = false
                }
                if (canCopy(child)) {
                    val copy = copyElement(child)
                    copyTo.appendChild(copy)
                    copyDocumentFromStart(copy, child, stopElement)
                }
            } else if (canCopy(e.getChild(i)) && e.getChild(i) is Text) {
                val child = e.getChild(i) as Text
                val copy = child.copy()
                copyTo.appendChild(copy)
            }
        }
        return copyTo
    }

    private fun copyDocumentFromPosition(
        copyTo: Element,
        e: Element,
        startElement: Element,
        stopElement: Element?
    ): Element {
        if (e == stopElement) copy = false else if (e == startElement) copy = true

        //if(canCopy(e) && e.getChildCount() == 0){
        //	Element copy = copyElement(e);
        //	copyTo.appendChild(copy);
        //}
        for (i in 0 until e.childCount) {
            if (isStartElement(e.getChild(i), startElement)) copy = true
            if (canCopy(e.getChild(i)) && e.getChild(i) is Element) {
                val child = e.getChild(i) as Element
                if (child == stopElement) {
                    copy = false
                }
                if (canCopy(child)) {
                    val copy = copyElement(child)
                    copyTo.appendChild(copy)
                    copyDocumentFromPosition(copy, child, startElement, stopElement)
                }
            } else if (canCopy(e.getChild(i)) && e.getChild(i) is Text) {
                val child = e.getChild(i) as Text
                val copy = child.copy()
                copyTo.appendChild(copy)
            }
        }
        return copyTo
    }

    private fun canCopy(n: Node): Boolean {
        if (copy) return true
        return n is Element && n.localName == "SECTION"
    }

    private fun isStartElement(n: Node, start: Element): Boolean {
        if (n is Element) {
            return n == start
        }
        return false
    }

    private fun copyElement(original: Element): Element {
        val copy = BBX.newElement(original.localName)
        val count = original.attributeCount
        for (i in 0 until count) {
            val atr = original.getAttribute(i)
            copy.addAttribute(atr.copy())
        }
        return copy
    }

    private fun copyRootElement(): Element {
        val root = doc.rootElement
        val e = BBX.newElement(doc.rootElement.localName)
        val count = root.namespaceDeclarationCount
        for (i in 0 until count) e.addNamespaceDeclaration(
            root.getNamespacePrefix(i),
            root.getNamespaceURI(root.getNamespacePrefix(i))
        )
        return e
    }

    private fun copyHead(): Element {
        val head = BBX.getHead(doc)
        //int count = root.getNamespaceDeclarationCount();
        //for(int i = 0; i < count; i++)
        //	e.addNamespaceDeclaration(root.getNamespacePrefix(i), root.getNamespaceURI(root.getNamespacePrefix(i)));
        return head.copy()
    }

    private fun saveDoc(d: Document, newDocPath: Path) {
        val arch = BBXArchiver(newDocPath, d, null)
        arch.saveAs(newDocPath, d, UTDTranslationEngine(), setOf())
    }

    private fun outputManifest() {
        XMLHandler().save(
            manifest,
            File(destPath.resolve(originalFileName + "_manifest.mnf").toAbsolutePath().toString())
        )
    }

    private fun setFileName() {
        val fileName = Element("fileName")
        fileName.appendChild(Text(originalFileName))
        manifest.rootElement.appendChild(fileName)
    }

    private fun setFile(index: Int) {
        val file = Element("file")
        val t = Text(getName(index))
        file.appendChild(t)
        manifest.rootElement.appendChild(file)
    }

    private fun getName(index: Int): String {
        return originalFileName + "_part_" + (index + 1).toString() + ".bbx"
    }

    /** Recursively removes braille from an element and its children
     * @param e : Element to remove braille
     */
    private fun removeBraille(e: Element) {
        val els = e.childElements
        for (i in 0 until els.size()) {
            if (els[i].localName == "brl") e.removeChild(els[i]) else removeBraille(els[i])
        }
    }
}