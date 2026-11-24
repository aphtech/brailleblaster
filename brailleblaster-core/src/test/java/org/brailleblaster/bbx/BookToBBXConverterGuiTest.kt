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
package org.brailleblaster.bbx

import org.brailleblaster.GuiTest
import org.brailleblaster.testrunners.BBTestRunner
import org.testng.annotations.Test

/** The BookToBBXConverterTest cases that need a full BBTestRunner window. */
@GuiTest
class BookToBBXConverterGuiTest {
    @Test(enabled = false)
    fun tableNonWithEmptyRow_issue6163() {
        /*
        make sure when LiveFixer cleans up empty cells and containers it gets
        stripped of being a table
        */
        val test = BBTestRunner(
            "",
            "<table>"
                + "<tr>"
                + "<td>Historical</td>"
                + "<td>Mythical</td>"
                + "</tr>"
                + "<tr>"
                + "<td></td>"
                + "<td></td>"
                + "</tr>"
                + "</table>"
        )

        test.assertRootSection_NoBrlCopy()
            .nextChildIs { block ->
                block
                    .onlyChildIsText("Historical")
                    .isBlock(BBX.BLOCK.DEFAULT)
            }.nextChildIs { block ->
                block
                    .onlyChildIsText("Mythical")
                    .isBlock(BBX.BLOCK.DEFAULT)
            }.noNextChild()
    }
}
