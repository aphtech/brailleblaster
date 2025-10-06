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
package org.brailleblaster.utd

import org.brailleblaster.libembosser.spi.BrlCell
import org.brailleblaster.utd.config.UTDConfig.loadBrailleSettings
import org.brailleblaster.utd.config.UTDConfig.saveBrailleSettings
import org.brailleblaster.utd.testutils.UTDConfigUtils
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File
import java.io.IOException

class BrailleSettingsTest {
    private var braille: BrailleSettings? = null

    @BeforeMethod
    fun setBrailleSettings() {
        braille = BrailleSettings()
    }

    @Test
    fun defaultConstructor() {
        //Disabled to stop this test failing when the tables are changed (eg us to UEB)
//		assertEquals(braille.getMainTranslationTable(), "en-us-g2.ctb");
        Assert.assertFalse(braille!!.isUseAsciiBraille)
        Assert.assertEquals(braille!!.cellType, BrlCell.NLS)
    }

    @Test
    fun testSetters() {
        val setUseAsciiBraille = true
        braille!!.isUseAsciiBraille = setUseAsciiBraille
        val getUseAsciiBraille = braille!!.isUseAsciiBraille
        Assert.assertEquals(getUseAsciiBraille, setUseAsciiBraille)

        val setMainTranslationTable = "nemeth.ctb"
        braille!!.mainTranslationTable = setMainTranslationTable
        val getMainTranslationTable = braille!!.mainTranslationTable
        Assert.assertEquals(getMainTranslationTable, setMainTranslationTable)

        val setCellType = BrlCell.SMALL_ENGLISH
        braille!!.cellType = setCellType
        val getCellType = braille!!.cellType
        Assert.assertEquals(getCellType, setCellType)
    }

    /**
     * Disabled to stop this test failing when the tables are changed (eg us to UEB)
     *
     * @throws IOException
     */
    @Test(enabled = false)
    @Throws(IOException::class)
    fun saveEngine() {
        val brailleSettings = BrailleSettings()
        Assert.assertNotSame(brailleSettings.cellType, BrlCell.SMALL_ENGLISH, "Defaults changed, update test")
        brailleSettings.cellType = BrlCell.SMALL_ENGLISH

        val tempOutput = File.createTempFile("utdEngine", "test")
        saveBrailleSettings(tempOutput, brailleSettings)
        UTDConfigUtils.compareOutputToSaved(tempOutput, UTDConfigUtils.TEST_BRAILLE_SETTINGS_FILE)
    }

    @Test
    fun loadEngine() {
        val brailleSettings = loadBrailleSettings(UTDConfigUtils.TEST_BRAILLE_SETTINGS_FILE)
        Assert.assertEquals(brailleSettings.cellType, BrlCell.SMALL_ENGLISH)
    }
}
