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
package org.brailleblaster.utd.utils

import org.brailleblaster.utd.ITranslationEngine
import org.brailleblaster.utd.actions.TransNoteAction
import org.brailleblaster.utd.exceptions.UTDTranslateException
import org.brailleblaster.utd.properties.BrailleTableType
import org.mwhapples.jlouis.Louis
import org.mwhapples.jlouis.Louis.TranslationModes
import org.mwhapples.jlouis.Louis.TypeForms
import org.mwhapples.jlouis.TranslationException
import org.mwhapples.jlouis.TranslationResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

object TextTranslator {
    private val log: Logger = LoggerFactory.getLogger(TextTranslator::class.java)


    @JvmStatic
    @JvmOverloads
    fun translateText(
        translateText: String,
        context: ITranslationEngine,
        typeForm: Short = TypeForms.PLAIN_TEXT,
        tableType: BrailleTableType = BrailleTableType.LITERARY
    ): String {
        return translateIndexedText(translateText, context, typeForm, tableType).second
    }

    @JvmOverloads
    fun translateIndexedText(
        translateText: String,
        context: ITranslationEngine,
        typeForm: Short = TypeForms.PLAIN_TEXT,
        tableType: BrailleTableType = BrailleTableType.LITERARY
    ): Pair<IntArray, String> {
        val translator = context.brailleTranslator

        val emphasisArr = ShortArray(translateText.length)
        Arrays.fill(emphasisArr, typeForm)

        var mode = TranslationModes.DOTS_IO or TranslationModes.UC_BRL
        if (context.brailleSettings.isUseAsciiBraille) {
            mode = 0
        }

        try {
            val transTable = tableType.getTableName(context.brailleSettings)

            val transResult = translator.translate(transTable, translateText, emphasisArr, 0, mode)
            return replaceUnknownCharacters(transResult, translator, transTable, mode)
        } catch (e: TranslationException) {
            throw UTDTranslateException("Problem with Braille translation, see log for details", e)
        }
    }

    private fun startEmp(
        translateEmpText: StringBuilder,
        j: Int,
        emphasisArr: ShortArray,
        i: Int,
        prvEmp: Short,
        emp: Short,
        tag: String
    ): Int {
        if ((prvEmp.toInt() and emp.toInt()) == 0x00 && (emphasisArr[i].toInt() and emp.toInt()) == emp.toInt()) {
            translateEmpText.insert(j, "\uf002" + tag + "\uf002")
            return tag.length + 2
        }
        return 0
    }

    private fun stopEmp(
        translateEmpText: StringBuilder,
        j: Int,
        emphasisArr: ShortArray,
        i: Int,
        prvEmp: Short,
        emp: Short,
        tag: String
    ): Int {
        if ((prvEmp.toInt() and emp.toInt()) == emp.toInt() && (emphasisArr[i].toInt() and emp.toInt()) == 0x00) {
            translateEmpText.insert(j, "\uf003" + tag + "\uf003")
            return tag.length + 2
        }
        return 0
    }

    private fun lastEmp(translateEmpText: StringBuilder, prvEmp: Short, emp: Short, tag: String): Int {
        if ((prvEmp.toInt() and emp.toInt()) == emp.toInt()) {
            translateEmpText.append("\uf003").append(tag).append("\uf003")
            return tag.length + 2
        }
        return 0
    }

    private const val ITALIC: Short = 1
    private const val UNDERLINE: Short = 2
    private const val BOLD: Short = 4
    private const val SCRIPT: Short = 8
    private const val TRANS_NOTE: Short = 16
    private const val TRANS_1: Short = 32
    private const val TRANS_2: Short = 64
    private const val TRANS_3: Short = 128
    private const val TRANS_4: Short = 256
    private const val TRANS_5: Short = 512
    private const val NO_CONTRACT: Short = 4096

    @Throws(UTDTranslateException::class)
    fun translateTextWithEmpArray(
        translateText: String?,
        context: ITranslationEngine,
        emphasisArr: ShortArray?,
        tableType: BrailleTableType
    ): TranslationEmpResult {
        var translated = ""
        var indexInts: IntArray? = null
        try {
            val translator = context.brailleTranslator

            var mode = TranslationModes.DOTS_IO or TranslationModes.UC_BRL
            if (context.brailleSettings.isUseAsciiBraille) mode = 0

            val transTable = tableType.getTableName(context.brailleSettings)
            log.debug("About to use tables \"{}\" to translate text \"{}\"", transTable, translateText)
            try {
                val transResult = translator.translate(transTable, translateText, emphasisArr, 0, mode)
                val indexAndTrans = replaceUnknownCharacters(transResult, translator, transTable, mode)
                translated = indexAndTrans.second
                log.debug("Translated text is: \"{}\"", translated)
                if (translated.isNotEmpty()) indexInts = indexAndTrans.first
            } catch (e: IllegalArgumentException) {
                log.error("FTSFP", e)
            }
        } catch (e: TranslationException) {
            throw UTDTranslateException("Problem with Braille translation, see log for details", e)
        }

        return TranslationEmpResult(translated, indexInts)
    }

    /**
     * Replace unknown liblouis characters (returned as '\xFFFF') with Unicode description
     *
     *
     * Issue #5810
     *
     * @param result
     * @param translator
     * @param transTable
     * @param mode
     * @return
     */
    fun replaceUnknownCharacters(
        result: TranslationResult,
        translator: Louis,
        transTable: String?,
        mode: Int
    ): Pair<IntArray, String> {
        var offset = 0
        val inputPos: MutableList<Int> by lazy { result.inputPos.toMutableList() }
        var translation = result.translation

        while ((findUnknownCharacters(translation, offset).also { offset = it }) != -1) {
            val liblouisCharacter = translation.substring(offset, offset + 8)
            if (liblouisCharacter.length != 8) {
                throw AssertionError("Not enouch chars |$liblouisCharacter|")
            } else if (liblouisCharacter[liblouisCharacter.length - 1] != '\'') {
                throw AssertionError("Wrong end |$liblouisCharacter|")
            } else if (liblouisCharacter[0] != '\'') {
                throw AssertionError("Wrong start |$liblouisCharacter|")
            }
            val hex = liblouisCharacter.substring(3, 7)
            if (hex.length != 4) {
                throw AssertionError("invalid |$hex|")
            }
            val actualChar = hex.toInt(16)

            val unicodeName = Character.getName(actualChar)
            val alternateString = TransNoteAction.START + unicodeName + TransNoteAction.END
            var alternateTrans: String
            try {
                val alternateResult = translator.translate(transTable, alternateString, ShortArray(0), 0, mode)
                alternateTrans = alternateResult.translation
            } catch (e: Exception) {
                throw UTDTranslateException("Problem with Braille translation, see log for details", e)
            }



            var ref = 0
            for (i in offset until offset + liblouisCharacter.length) {
                val next: Int = inputPos.removeAt(offset)
                if (i != offset && next != ref) {
                    throw AssertionError(
                        "expected $ref but found $next for $i | " + inputPos.joinToString(separator = ",")
                    )
                }
                ref = next
            }
            for (i in alternateTrans.indices) {
                inputPos.add(offset + i, ref)
            }

            translation = translation.replaceFirst( liblouisCharacter, alternateTrans)

            offset += alternateTrans.length
        }

        return inputPos.toIntArray() to translation
    }

    /**
     * Highlight '\xFFFF' -like unknown liblouis characters
     *
     * @param text
     * @return offset of unknown character or -1
     */
    private fun findUnknownCharacters(text: String, fromIndex: Int): Int {
        var start = 0
        var wordLength = -1

        for (i in fromIndex until text.length) {
            val cur = text[i]

            if (cur == '\n' || cur == '\r') {
                continue
            }
            when (wordLength) {
                -1 -> if (cur == '\'') {
                    start = i
                    wordLength = 0
                }

                0 -> wordLength = if ((cur == '\\')) 1 else -1
                1 -> wordLength = if ((cur == 'x')) wordLength + 1 else -1
                2, 3, 4, 5 -> wordLength = if ((isHex(cur))) wordLength + 1 else -1
                6 -> {
                    if (cur == '\'') {
                        return start
                    }
                    wordLength = -1
                }

                else -> {}
            }
        }
        return -1
    }

    private fun isHex(hex: Char): Boolean {
        return (hex in '0'..'9') || (hex in 'a'..'f')
    }

    class TranslationEmpResult(@JvmField var translation: String, @JvmField var dotsToCharsMap: IntArray?)
}
