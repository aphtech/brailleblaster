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

import org.brailleblaster.bbx.legacy.LegacyFormatDetectionTest
import org.brailleblaster.bbx.legacy.LegacyTestSupport.load
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertNull
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.nameWithoutExtension

/**
 * Files in the current BBX format must open exactly as they did before legacy import existed:
 * same document, same archiver state, no conversion notice.
 */
class CurrentFormatUnchangedTest {
    @DataProvider
    fun currentFiles(): Array<Array<Any>> =
        LegacyFormatDetectionTest.CURRENT_FIXTURES.map { arrayOf<Any>(it) }.toTypedArray()

    @Test(dataProvider = "currentFiles")
    fun currentBbxOpensUnchanged(file: Path) {
        val expectedXml = load(file).toXML()
        ArchiverFactory.load(file).use { archiver ->
            assertEquals(archiver.bbxDocument.toXML(), expectedXml)
            // existing behaviour for .bbx: imported into a new BBZ named after the file
            assertEquals(archiver.importedFrom, file)
            assertEquals(archiver.newPath.toString(), file.nameWithoutExtension + ".bbz")
            assertNull(LegacyImports.takeReport(archiver))
        }
    }

    @Test(dataProvider = "currentFiles")
    fun currentBbzOpensUnchanged(file: Path) {
        val expectedXml = load(file).toXML()
        val bbz = Files.createTempFile("current-", ".bbz")
        try {
            ZipOutputStream(Files.newOutputStream(bbz)).use { zip ->
                zip.putNextEntry(ZipEntry("bbx_location"))
                zip.write("/document.bbx\n".toByteArray())
                zip.putNextEntry(ZipEntry("document.bbx"))
                zip.write(Files.readAllBytes(file))
            }
            ArchiverFactory.load(bbz).use { archiver ->
                assertEquals(archiver.bbxDocument.toXML(), expectedXml)
                assertFalse(archiver.isImported)
                assertNull(archiver.newPath)
                assertNull(LegacyImports.takeReport(archiver))
            }
        } finally {
            ZipHandles.close(bbz)
            Files.deleteIfExists(bbz)
        }
    }
}
