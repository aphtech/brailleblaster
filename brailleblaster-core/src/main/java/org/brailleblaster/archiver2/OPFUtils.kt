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
package org.brailleblaster.archiver2

import nu.xom.Document
import nu.xom.Element
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.FastXPath
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

/**
 * Utils for parsing OPF ( http://www.idpf.org/epub/20/spec/OPF_2.0.1_draft.htm ) files
 * which are in both nimas and epub zip books
 */
object OPFUtils {
    private val log: Logger = LoggerFactory.getLogger(OPFUtils::class.java)

    @JvmStatic
	fun findOPFFilesInFolder(folder: Path): List<Path> {
        try {
            val opfs = Files.walk(folder)
                .filter { obj: Path -> pathNotHiddenOrInHiddenDirectory(obj) && obj.toString().endsWith(".opf") }
                .peek { curPAth: Path -> log.trace("Found OPF at {}", curPAth.toUri()) }
                .collect(Collectors.toList())
            if (opfs.isEmpty()) {
                log.warn("No opfs found in {}", folder)
            } else if (opfs.size > 1) {
                log.warn("Found {} OPF files?", opfs.size)
            }
            return opfs
        } catch (_: Exception) {
            throw RuntimeException("Failed to find OPFs in $folder")
        }
    }

    @JvmStatic
	fun getDCElementValueCaseInsensitive(opfDocument: Document?, opfElemName: String?): String? {
        val results = FastXPath.descendant(opfDocument)
            .filterIsInstance<Element>()
            .filter { curElem: Element -> curElem.namespacePrefix == "dc" && curElem.localName.equals(opfElemName, ignoreCase = true) }.toList()
        if (results.isEmpty()) {
            return null
        } else if (results.size > 1) {
            throw NodeException("Found more than 1 node", results[1])
        }
        return results[0].value
    }

    @JvmStatic
	fun getManifestItems(opfDocument: Document): List<ManifestEntry> {
        val namespace = opfDocument.rootElement.namespaceURI
        val manifestRoot = opfDocument.rootElement.getFirstChildElement(
            "manifest",
            namespace
        )
        val entries: MutableList<ManifestEntry> = ArrayList()
        for (curElement in manifestRoot.childElements) {
            entries.add(
                ManifestEntry(
                    curElement.getAttributeValue("id"),
                    curElement.getAttributeValue("href"),
                    curElement.getAttributeValue("media-type"),
                    curElement
                )
            )
        }
        return entries
    }

    /**
     * Attempt to exclude Mac OSX data files under __MACOSX or ._actualfile.xml.
     *
     * @param path
     * @return
     */
	@JvmStatic
	fun pathNotHiddenOrInHiddenDirectory(path: Path): Boolean {
        if (path.fileName == null) {
            return false
        }
        for (value in path) {
            val curPathPart = value.toString()
            if (curPathPart.startsWith(".") || curPathPart.startsWith("_")) {
                return false
            }
        }
        return true
    }

    class ManifestEntry(val id: String, @JvmField val href: String, val mimeType: String, @JvmField val elem: Element)
}
