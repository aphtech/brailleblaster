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

import nu.xom.Builder
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.legacy.LegacyTestSupport.V1X
import org.brailleblaster.bbx.legacy.LegacyTestSupport.files
import org.brailleblaster.bbx.legacy.LegacyTestSupport.load
import org.brailleblaster.bbx.legacy.LegacyTestSupport.rawVersion
import org.testng.Assert.assertEquals
import org.testng.Assert.assertNull
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.StringReader
import java.nio.file.Path
import java.nio.file.Paths

class LegacyFormatDetectionTest {
    @DataProvider
    fun v1xFiles(): Array<Array<Any>> = files(V1X).map { arrayOf<Any>(it) }.toTypedArray()

    @Test(dataProvider = "v1xFiles")
    fun detectsOneXVersion(file: Path) {
        val doc = load(file)
        val raw = rawVersion(doc)?.toInt()
        val expected = when {
            raw == null -> 1
            raw < BBX.FORMAT_VERSION -> raw
            else -> null
        }
        assertEquals(LegacyFormat.legacyBbxVersion(doc), expected, "$file (raw version $raw)")
    }

    @Test
    fun missingVersionIsVersionOne() {
        assertEquals(LegacyFormat.legacyBbxVersion(load(V1X.resolve("edge/no_version.bbx"))), 1)
    }

    @DataProvider
    fun currentFiles(): Array<Array<Any>> = CURRENT_FIXTURES.map { arrayOf<Any>(it) }.toTypedArray()

    @Test(dataProvider = "currentFiles")
    fun currentFilesAreNotLegacy(file: Path) {
        val doc = load(file)
        val before = doc.toXML()
        assertNull(LegacyFormat.legacyBbxVersion(doc))
        assertEquals(doc.toXML(), before, "detection must not modify the document")
    }

    @Test
    fun twoPointXFilesAreNotOneX() {
        val v2 = """<bbdoc xmlns="http://brailleblaster.org/ns/bb"><head><version>2.3</version></head>""" +
            """<blocklist><block id="a" gid="a" type="DEFAULT"><tme type="text">Hi</tme></block></blocklist></bbdoc>"""
        assertNull(LegacyFormat.legacyBbxVersion(Builder().build(StringReader(v2))))
    }

    @Test
    fun nonBbxIsNotLegacy() {
        val dtbook = """<dtbook xmlns="http://www.daisy.org/z3986/2005/dtbook/"><head/><book/></dtbook>"""
        assertNull(LegacyFormat.legacyBbxVersion(Builder().build(StringReader(dtbook))))
    }

    companion object {
        /** BBX files already in the current format, from elsewhere in the test resources. */
        val CURRENT_FIXTURES: List<Path> = listOf(
            "src/test/resources/org/brailleblaster/printView/BoxLineSelectionTests.bbx",
            "src/test/resources/org/brailleblaster/printView/testMatrix.bbx",
            "src/test/resources/org/brailleblaster/math/templateOldBBX.bbx",
            "src/test/resources/org/brailleblaster/bbx/fixers/9781946636171NIMAS.bbx",
        ).map { Paths.get(it) }
    }
}
