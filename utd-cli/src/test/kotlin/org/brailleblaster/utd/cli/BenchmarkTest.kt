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
package org.brailleblaster.utd.cli

import org.brailleblaster.libembosser.spi.BrlCell
import org.brailleblaster.utd.PageSettings
import org.brailleblaster.utd.cli.Benchmark.setNewPageHeight
import org.brailleblaster.utd.cli.Benchmark.setNewPageWidth
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class BenchmarkTest {
    @DataProvider
    fun pageSizeDataProvider(): Array<Array<Any?>?> {
        val result: MutableList<Array<Any?>?> = ArrayList()
        for (i in 1..49) {
            result.add(arrayOf(i))
        }
        return result.toTypedArray<Array<Any?>?>()
    }

    @Test(dataProvider = "pageSizeDataProvider")
    fun setNewPageHeightTest(size: Int) {
        val pageSettings = PageSettings()
        setNewPageHeight(pageSettings, BrlCell.NLS, size)
    }

    @Test(dataProvider = "pageSizeDataProvider")
    fun setNewPageWidthTest(size: Int) {
        val pageSettings = PageSettings()
        setNewPageWidth(pageSettings, BrlCell.NLS, size)
    }
}
