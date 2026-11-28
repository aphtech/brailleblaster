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
package org.brailleblaster.bbx.legacy

import nu.xom.Document
import nu.xom.Element
import org.brailleblaster.bbx.BBX
import org.brailleblaster.settings.UTDManager
import org.brailleblaster.utd.BRFWriter
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utils.xml.BB_NS
import org.brailleblaster.utils.xml.UTD_NS
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.io.path.extension
import kotlin.io.path.name

/** Shared helpers for the legacy import tests. */
object LegacyTestSupport {
    val LEGACY_ROOT: Path = Paths.get("src/test/resources/org/brailleblaster/legacy")
    val V1X: Path = LEGACY_ROOT.resolve("v1x")

    fun files(dir: Path, extension: String = "bbx"): List<Path> =
        Files.walk(dir).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.extension == extension }.sorted().toList()
        }

    fun load(path: Path): Document = XMLHandler().load(path)

    /** Format version written in the file, read directly from the XML. */
    fun rawVersion(doc: Document): String? = LegacyFormat.versionText(doc.rootElement)

    /**
     * All words of the visible text (UTD markup skipped), for checking that no text is lost.
     * Words are split per text node, so trimming whitespace at block edges does not merge words.
     */
    fun words(node: nu.xom.Node): List<String> = FastXPath.descendantOrSelf(node)
        .filterIsInstance<nu.xom.Text>()
        .filter { text ->
            generateSequence(text.parent as? Element) { it.parent as? Element }.none { it.namespaceURI == UTD_NS }
        }
        .flatMap { it.value.split(Regex("\\s+")) }
        .filter { it.isNotEmpty() }
        .toList()

    /** Words in [expected] that are missing (counting repeats) from [actual]. */
    fun missingWords(expected: List<String>, actual: List<String>): List<String> {
        val remaining = actual.groupingBy { it }.eachCount().toMutableMap()
        return expected.filter { word ->
            val count = remaining[word] ?: 0
            if (count > 0) { remaining[word] = count - 1; false } else true
        }
    }

    /**
     * Structure of a BBX document: one line per bb element with its depth, name, bb:type and
     * utd:overrideStyle, plus its own trimmed text. UTD markup and the head are ignored.
     */
    fun structure(doc: Document): List<String> = FastXPath.descendant(BBX.getRoot(doc))
        .filterIsInstance<Element>()
        .filter { it.namespaceURI == BB_NS }
        .map { elem ->
            val depth = generateSequence(elem.parent as? Element) { it.parent as? Element }.count()
            val ownText = (0 until elem.childCount).map { elem.getChild(it) }
                .filterIsInstance<nu.xom.Text>().joinToString("") { it.value }
                .replace(Regex("\\s+"), " ").trim()
            "$depth ${elem.localName}[${elem.getAttributeValue("type", BB_NS)}]" +
                (elem.getAttributeValue("overrideStyle", UTD_NS)?.let { " style=$it" } ?: "") +
                (if (ownText.isNotEmpty()) " \"$ownText\"" else "")
        }
        .toList()

    /**
     * Blocks formatted as Body Text: the fallback's `BLOCK STYLE` with Body Text, or a plain
     * `BLOCK DEFAULT` (whose style is Body Text), which later upgrade steps may turn it into.
     */
    fun bodyTextBlocks(doc: Document): List<Element> = FastXPath.descendant(doc.rootElement)
        .filterIsInstance<Element>()
        .filter {
            (BBX.BLOCK.STYLE.isA(it) && BBX._ATTRIB_OVERRIDE_STYLE.get(it) == LegacyFallback.BODY_TEXT) ||
                (BBX.BLOCK.DEFAULT.isA(it) && !BBX._ATTRIB_OVERRIDE_STYLE.has(it))
        }
        .toList()

    /** Translate and format [doc] and return the BRF text, as the app does when it opens a file. */
    fun toBrf(doc: Document): String {
        val utdManager = UTDManager()
        utdManager.loadEngineFromDoc(doc, "bbx")
        val translated = utdManager.engine.translateAndFormatDocument(doc.copy())
        return StringWriter().also {
            utdManager.engine.toBRF(translated, it, 0, BRFWriter.EMPTY_PAGE_LISTENER)
        }.toString()
    }

    fun sha256(path: Path): String =
        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)).joinToString("") { "%02x".format(it) }

    fun displayName(path: Path): String = path.parent.name + "/" + path.name
}
