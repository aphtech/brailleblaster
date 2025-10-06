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

import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlRootElement
import org.brailleblaster.libembosser.spi.BrlCell

@XmlRootElement(name = "brailleSettings")
data class BrailleSettings(
    var cellType: BrlCell = BrlCell.NLS,
    /** What Braille table to use for the literary text  */
    var mainTranslationTable: String = "en-ueb-g2.ctb",
    var computerBrailleTable: String = "en-us-comp8.ctb",
    var uncontractedTable: String = "en-ueb-g1.ctb",
    /**
     * What table to use for non-mathematical content in books containing maths. This option is
     * normally not needed in many codes and so should be the same as literaryTextTable.
     */
    var mathTextTable: String = "en-us-g2.ctb",
    /** What Braille table to use for mathematical content  */
    @Deprecated("Should be using MathCAT instead")
    var mathExpressionTable: String = "nemeth.ctb",
    /** What table should be used to edit together parts of documents (eg. to join maths and text)  */
    var editTable: String = "nemeth_edit.ctb",
    var mathBrailleCode: MathBraileCode = MathBraileCode.Nemeth,
    var isUseAsciiBraille: Boolean = false,
    var mathIndicators: MathIndicators = MathIndicators()
)

enum class MathBraileCode(
    val preferenceName: String,
    val lineWrapping: List<InsertionPatternEntry> = listOf(),
    val startLines: List<InsertionPatternEntry> = listOf(),
) {
    UEB(
        "UEB",
        lineWrapping = listOf(
            InsertionPatternEntry(
                matchPattern = " (?=(\"7(`:)?|`[<>]) )"
            ),
            InsertionPatternEntry(
                matchPattern = "(?=\"[68\\-/])"
            ),
            InsertionPatternEntry(
                matchPattern = "[\\._\"]?>(?<contDots>)(?=[\\._\"]?<)",
                insertionDots = "\""
            )
        )
    ),
    Nemeth(
        "Nemeth",
        lineWrapping = listOf(
            InsertionPatternEntry(
                matchPattern = " (?=(/?\\.k|\\.1|\"\\.) )"
            ),
            InsertionPatternEntry(
                matchPattern = "(?=\\+|-|\\./|`\\*)"
            )
        ),
        startLines = listOf(
            InsertionPatternEntry(
                matchPattern = "(?=[0-9])",
                insertionDots = "#"
            ),
            InsertionPatternEntry(
                matchPattern = "-(?=[0-9])",
                insertionDots = "#"
            )
        )
    )
}

data class InsertionPatternEntry @JvmOverloads constructor(
    @get:XmlAttribute var matchPattern: String = "",
    @get:XmlAttribute var insertionDots: String = ""
) {

    companion object {
        @JvmStatic
        fun listToMap(listInsertions: List<InsertionPatternEntry>?): Map<String, String> {
            return listInsertions?.associate { it.matchPattern to it.insertionDots } ?: emptyMap()
        }
    }
}

data class MathIndicators(
    @get:XmlAttribute var start: String = "",
    @get:XmlAttribute var end: String = ""
)