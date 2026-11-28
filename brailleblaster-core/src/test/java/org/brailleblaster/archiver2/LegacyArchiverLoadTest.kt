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
import org.brailleblaster.bbx.legacy.LegacyFormat
import org.brailleblaster.bbx.legacy.LegacyTestSupport
import org.brailleblaster.bbx.legacy.LegacyTestSupport.V1X
import org.brailleblaster.bbx.legacy.LegacyTestSupport.files
import org.brailleblaster.bbx.legacy.LegacyTestSupport.load
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

/** Opening 1.x files through the same entry point the app uses. */
class LegacyArchiverLoadTest {
    @DataProvider
    fun legacyFiles(): Array<Array<Any>> = (files(V1X) + files(V1X.resolve("bbz"), "bbz"))
        .filter { !it.toString().contains("upgrade-pairs") }
        .filter { it.toString().endsWith(".bbz") || LegacyFormat.legacyBbxVersion(load(it)) != null }
        .map { arrayOf<Any>(it) }.toTypedArray()

    @Test(dataProvider = "legacyFiles")
    fun opensAsConvertedDocument(file: Path) {
        val originalHash = sha256(file)
        ArchiverFactory.load(file).use { archiver ->
            assertTrue(archiver is BBZArchiver, archiver.javaClass.name)
            assertEquals(BBX.getFormatVersion(archiver.bbxDocument), BBX.FORMAT_VERSION)
            assertTrue(archiver.isImported, "converted files must be saved with Save As")
            assertEquals(archiver.importedFrom, file)
            assertEquals(archiver.newPath.toString(), LegacyImports.convertedName(file).toString())
            assertTrue(archiver.newPath.toString().endsWith(" (converted).bbz"))

            val report = LegacyImports.takeReport(archiver)
            assertNotNull(report)
            assertEquals(report!!.release, "1.x")
            assertNull(LegacyImports.takeReport(archiver), "the notice is only shown once")
            assertTrue(LegacyImportNotice.message(report).contains("1.x"))

            LegacyTestSupport.toBrf(archiver.bbxDocument)
        }
        assertEquals(sha256(file), originalHash, "opening must not modify the original file")
    }

    @Test(dataProvider = "legacyFiles")
    fun savedCopyReopensAsCurrentFile(file: Path) {
        val originalHash = sha256(file)
        val saved = Files.createTempFile("legacy-saved-", ".bbz").also { Files.delete(it) }
        try {
            ArchiverFactory.load(file).use { archiver ->
                archiver.saveAs(saved, archiver.bbxDocument, ImportFixerCommon.UTD_ENGINE, emptySet())
                LegacyImports.takeReport(archiver)
            }
            ZipHandles.close(saved)
            ArchiverFactory.load(saved).use { reopened ->
                assertFalse(reopened.isImported, "a saved converted file opens as a normal file")
                assertNull(reopened.newPath)
                assertNull(LegacyImports.takeReport(reopened), "no conversion notice for the saved file")
                assertEquals(BBX.getFormatVersion(reopened.bbxDocument), BBX.FORMAT_VERSION)
            }
        } finally {
            ZipHandles.close(saved)
            Files.deleteIfExists(saved)
        }
        assertEquals(sha256(file), originalHash, "saving the converted file must not modify the original")
    }
}
