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

import nu.xom.Node
import nu.xom.ParentNode
import nu.xom.Text
import org.apache.commons.lang3.time.DurationFormatUtils
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.settings.UTDManager.Companion.getCellsPerLine
import org.brailleblaster.utd.internal.xml.FastXPath
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.graphics.Font
import org.slf4j.helpers.MessageFormatter
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Randomly useful methods that aren't implemented
 */
object Utils {

    /**
     * Wrapper around slf4j's message formatter
     *
     * @return
     */
	@JvmStatic
	fun formatMessage(inputPattern: String?, vararg args: Any?): String {
        if (args.isNotEmpty() && args[args.size - 1] is Throwable) {
            throw RuntimeException(
                "Simple formatter cannot handle exceptions, see attached",
                args[args.size - 1] as Throwable?
            )
        }
        return MessageFormatter.arrayFormat(inputPattern, args).message
    }

    @JvmStatic
	fun runtimeToString(startTimeMills: Long): String {
        return DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - startTimeMills)
    }

    val isWindows: Boolean
        get() = SWT.getPlatform() == "win32"

    val isLinux: Boolean
        get() = SWT.getPlatform() == "gtk"

    fun adjustFontToDialog(m: Manager?, text: StyledText) {
        val middle = getCellsPerLine(m!!).toDouble()
        val width = text.bounds.width.toDouble()
        val charWidth = width / middle
        val fd = text.font.fontData[0]
        fd.setHeight(charWidth.toInt())
        val f = Font(text.display, fd)
        text.font = f
    }

    @JvmStatic
	fun removeRegionString(start: Int, end: Int, str: String): String {
        return (str.take(start)
                + str.drop(end))
    }

    @JvmStatic
	fun removeRegionRangeString(start: Int, range: Int, str: String): String {
        return (str.take(start)
                + str.drop(start + range))
    }

    @JvmStatic
	fun combineAdjacentTextNodes(parent: ParentNode?) {
        val textNodes = FastXPath.descendant(parent).filterIsInstance<Text>()
        for (node in textNodes) {
            val textParent = node.parent
            val index = textParent.indexOf(node)
            if (index + 1 < textParent.childCount && textParent.getChild(index + 1) is Text) {
                val sibling = textParent.getChild(index + 1) as Text
                sibling.detach()
                node.value += sibling.value
                combineAdjacentTextNodes(parent)
                return
            }
        }
    }

    /**
     * insert a child without worrying about a child count exception
     *
     * @param p
     * parent node
     * @param n
     * unattached node
     * @param index
     */
	@JvmStatic
	fun insertChildCountSafe(p: ParentNode, n: Node?, index: Int) {
        if (index < 0) {
            if (p.childCount > 0) {
                p.insertChild(n, 0)
            } else {
                p.appendChild(n)
            }
        } else {
            if (p.childCount > index) {
                p.insertChild(n, index)
            } else {
                p.appendChild(n)
            }
        }
    }

    fun vPosToLines(manager: Manager, vPos: Double): Int {
        return manager.simpleManager.utdManager.engine.brailleSettings.cellType.getLinesForHeight(vPos.toBigDecimal())
    }


    /**
     * Warning: URL must not redirect, POST parameters will be lost
     */
    @Throws(IOException::class, InterruptedException::class)
    fun httpPost(requestURL: String, postDataParams: Map<String, String>): String {
        val client = HttpClient.newHttpClient()
        val body = httpPostMakePostParams(postDataParams)
        val request =
            HttpRequest.newBuilder().uri(URI.create(requestURL)).POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded").timeout(
                Duration.ofSeconds(15)
            ).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() == 200) return response.body()
        else throw RuntimeException("Error " + response.statusCode() + " when posting to " + requestURL)
    }

    private fun httpPostMakePostParams(params: Map<String, String>): String {
        return params.entries.joinToString(separator = "&") { e ->
            String.format(
                "%s=%s",
                URLEncoder.encode(e.key, StandardCharsets.UTF_8),
                URLEncoder.encode(e.value, StandardCharsets.UTF_8)
            )
        }
    }
}
