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

import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.fixers2.ImportFixerCommon
import org.brailleblaster.bbx.legacy.LegacyTestSupport
import org.brailleblaster.bbx.legacy.LegacyTestSupport.V2X
import org.brailleblaster.bbx.legacy.LegacyTestSupport.files
import org.brailleblaster.bbx.legacy.LegacyTestSupport.sha256
import org.brailleblaster.util.ui.LegacyImportNotice
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertNotNull
import org.testng.Assert.assertNull
import org.testng.Assert.assertTrue
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.nio.file.Files
import java.nio.file.Path

/** Opening 2.0 files through the same entry point the app uses. */
class V2ArchiverLoadTest {
    @DataProvider
    fun v2Files(): Array<Array<Any>> = (files(V2X) + files(V2X.resolve("bbz"), "bbz"))
        .map { arrayOf<Any>(it) }.toTypedArray()

    @Test(dataProvider = "v2Files")
    fun opensAsConvertedDocument(file: Path) {
        val originalHash = sha256(file)
        ArchiverFactory.load(file).use { archiver ->
            assertTrue(archiver is BBZArchiver, archiver.javaClass.name)
            assertEquals(BBX.getFormatVersion(archiver.bbxDocument), BBX.FORMAT_VERSION)
            BBX.assertIsA(archiver.bbxDocument)
            assertTrue(archiver.isImported, "converted files must be saved with Save As")
            assertEquals(archiver.importedFrom, file)
            assertTrue(archiver.newPath.toString().endsWith(" (converted).bbz"), archiver.newPath.toString())

            val report = LegacyImports.takeReport(archiver)
            assertNotNull(report)
            assertEquals(report!!.release, "2.0")
            assertTrue(LegacyImportNotice.message(report).contains("2.0"))

            LegacyTestSupport.toBrf(archiver.bbxDocument)
        }
        assertEquals(sha256(file), originalHash, "opening must not modify the original file")
    }

    @Test(dataProvider = "v2Files")
    fun savedCopyReopensAsCurrentFile(file: Path) {
        val originalHash = sha256(file)
        val saved = Files.createTempFile("v2-saved-", ".bbz").also { Files.delete(it) }
        try {
            val convertedXml = ArchiverFactory.load(file).use { archiver ->
                archiver.saveAs(saved, archiver.bbxDocument, ImportFixerCommon.UTD_ENGINE, emptySet())
                LegacyImports.takeReport(archiver)
                archiver.bbxDocument.toXML()
            }
            ZipHandles.close(saved)
            ArchiverFactory.load(saved).use { reopened ->
                assertFalse(reopened.isImported, "a saved converted file opens as a normal file")
                assertNull(reopened.newPath)
                assertNull(LegacyImports.takeReport(reopened), "no conversion notice for the saved file")
                assertEquals(BBX.getFormatVersion(reopened.bbxDocument), BBX.FORMAT_VERSION)
                assertEquals(reopened.bbxDocument.rootElement.childElements[1].value,
                    nu.xom.Builder().build(java.io.StringReader(convertedXml)).rootElement.childElements[1].value,
                    "the saved document has the converted content")
            }
        } finally {
            ZipHandles.close(saved)
            Files.deleteIfExists(saved)
        }
        assertEquals(sha256(file), originalHash, "saving the converted file must not modify the original")
    }

    @Test
    fun bbzWithoutLocationFileIsFound() {
        val file = V2X.resolve("bbz/v2x_no_location_file.bbz")
        ArchiverFactory.load(file).use { archiver ->
            assertTrue(LegacyFallbackText.contains(archiver.bbxDocument, "Hello World!"))
            assertNotNull(LegacyImports.takeReport(archiver))
        }
    }

    private object LegacyFallbackText {
        fun contains(doc: nu.xom.Document, text: String) =
            org.brailleblaster.bbx.legacy.LegacyFallback.visibleText(doc.rootElement).contains(text)
    }
}
