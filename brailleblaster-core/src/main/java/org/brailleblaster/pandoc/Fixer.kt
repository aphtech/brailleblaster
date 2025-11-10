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
package org.brailleblaster.pandoc

import nu.xom.Builder
import nu.xom.Document
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utils.xml.BB_NS
import java.io.*
import java.nio.charset.StandardCharsets

class Fixer(filename: String) {
    private val file: File = File(filename)
    var document: Document
    val rootElement: Element
    val topNode: Node
    private val fixers = ArrayList<FixerInf>()

    init {
        val fis = FileInputStream(filename)
        // read the file with UTF-8 encoding
        val isr = InputStreamReader(fis, StandardCharsets.UTF_8)
        val bldr = Builder()
        document = bldr.build(isr)
        rootElement = document.rootElement
        topNode = rootElement
        isr.close()
    }

    fun addFixer(f: FixerInf) {
        f.setFixer(this)
        fixers.add(f)
    }

    val bbUri: String = BB_NS

    @Throws(Exception::class)
    fun processFixers() {
        for (f in fixers) {
            f.process()
        }
        val fos = FileOutputStream(file)
        val osw = OutputStreamWriter(fos, StandardCharsets.UTF_8)
        osw.write(document.toXML())
        osw.close()
    }
}