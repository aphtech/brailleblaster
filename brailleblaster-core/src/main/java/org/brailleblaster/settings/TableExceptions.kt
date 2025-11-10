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
package org.brailleblaster.settings

import org.brailleblaster.BBIni
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.settings.UTDManager.BBUTDTranslationEngine
import org.brailleblaster.utd.BrailleSettings
import org.brailleblaster.exceptions.BBRuntimeException
import org.brailleblaster.util.LINE_BREAK
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.PrintWriter

/**
 * Manage where to write exceptions to the current translation code
 */
object TableExceptions {
    const val EXCEPTIONS_TABLE_EXTENSION = "-bb-exceptions.ctb"
    @JvmField
	  var UNIVERSAL_EXCEPTION_FILE_HEADING =
            ("# Translation exception table $LINE_BREAK# for users of brailleblaster ${LINE_BREAK}sign \\xf000 4-46-126 transcriber start indicator${LINE_BREAK}sign \\xf001 4-46-354 transcriber end indicator")

    private val log = LoggerFactory.getLogger(TableExceptions::class.java)
    private var mainTranslationStandardFile: File? = null
    private var MATH_TRANSLATION_STANDARD_FILE: File? = null
    private var TEST_EXCEPTION_FILE: File? = null
    @JvmField
	  var KEEP_TEST_DATA = false
    @JvmField
	  var MATH_EXCEPTION_TABLES = false
    @JvmStatic
	  fun generateExceptionsTables(
        engine: BBUTDTranslationEngine, forTesting: Boolean,
        brailleStandard: String
    ) {
        val exceptionsTableFile: File?
        if (forTesting) {
            try {
                //TODO:  refactor tests that use actual translation
                exceptionsTableFile = testExceptionFile
                if (!exceptionsTableFile!!.exists()) {
                    log.debug("Making exceptions table {}", exceptionsTableFile)
                    exceptionsTableFile.parentFile.mkdirs()
                }
                try {
                    PrintWriter(exceptionsTableFile).use { writer -> writer.println(UNIVERSAL_EXCEPTION_FILE_HEADING) }
                } catch (e: Exception) {
                    throw BBRuntimeException("Can't create file $exceptionsTableFile", e)
                }
                val translationTable = engine.brailleSettings.mainTranslationTable
                log.info("Table before $translationTable")
                val lastTableStart = translationTable.lastIndexOf(',')
                val oldTable = translationTable.substring(lastTableStart + 1)
                if (!(oldTable.contains("/") || oldTable.contains("\\"))) {
                    throw RuntimeException(
                        "Not removing exceptions table? $oldTable for $translationTable"
                    )
                }
                engine.brailleSettings.mainTranslationTable =
                    translationTable.take(lastTableStart) + "," + exceptionsTableFile.absolutePath
                log.info("Reset exceptions table to " + engine.brailleSettings.mainTranslationTable)
                TEST_EXCEPTION_FILE = exceptionsTableFile
                mainTranslationStandardFile = exceptionsTableFile
            } catch (e: Exception) {
                throw RuntimeException("Failed to rebuild exceptions table", e)
            }
        }
        else {
            // Braille settings was changed, need to re-add exceptions table
            // Generate the braille standard specific exceptions table
            val brailleSettings = engine.brailleSettings
            exceptionsTableFile = getTranslationExceptionFile(brailleSettings, brailleStandard)
            if (!exceptionsTableFile!!.exists()) {
                log.debug("Making exceptions table {}", exceptionsTableFile)
                exceptionsTableFile.parentFile.mkdirs()
                try {
                    PrintWriter(exceptionsTableFile).use { writer ->
                        writer.println(UNIVERSAL_EXCEPTION_FILE_HEADING)
                    }
                } catch (e: Exception) {
                    throw BBRuntimeException("Can't create file $exceptionsTableFile", e)
                }
            }
            log.debug("BB Exceptions Table {}", exceptionsTableFile.absolutePath)

            // Add to UTD's mainTranslationTable
            brailleSettings.mainTranslationTable = "${brailleSettings.mainTranslationTable},$exceptionsTableFile"
            mainTranslationStandardFile = exceptionsTableFile

            // Now add the math exceptions table
            /*
             * Adding these to the default settings so that transcriber notes will
             * use our private unicode characters when translating as math,
             * but not making them visible to the user right now
             * since they cannot modify the table with correct translation dialog
             */
            val mathExceptionsTableFile = getMathTranslationExceptionFile(brailleSettings, brailleStandard)
            if (!mathExceptionsTableFile!!.exists()) {
                log.debug("Making exceptions table {}", mathExceptionsTableFile)
                mathExceptionsTableFile.parentFile.mkdirs()
                try {
                    PrintWriter(mathExceptionsTableFile).use { writer -> writer.println(UNIVERSAL_EXCEPTION_FILE_HEADING) }
                } catch (e: Exception) {
                    throw BBRuntimeException("Can't create file $mathExceptionsTableFile", e)
                }
            }
            brailleSettings.mathExpressionTable = "${brailleSettings.mathExpressionTable},$mathExceptionsTableFile"
            MATH_TRANSLATION_STANDARD_FILE = mathExceptionsTableFile
        }
    }

    @JvmStatic
	  fun getTranslationExceptionFile(settings: BrailleSettings, brailleStandard: String): File? {
        if (TEST_EXCEPTION_FILE != null && KEEP_TEST_DATA) {
            return TEST_EXCEPTION_FILE
        }
        val exceptionsTableName = brailleStandard + EXCEPTIONS_TABLE_EXTENSION
        return BBIni.getUserProgramDataFile("liblouis", "tables", exceptionsTableName)
    }

    private fun getMathTranslationExceptionFile(settings: BrailleSettings, brailleStandard: String?): File? {
        if (TEST_EXCEPTION_FILE != null && KEEP_TEST_DATA) {
            return TEST_EXCEPTION_FILE
        }
        val math = MathTranslationSettings.getMathTable(settings)
        val exceptionsTableName = math + EXCEPTIONS_TABLE_EXTENSION
        return BBIni.getUserProgramDataFile("liblouis", "tables", exceptionsTableName)
    }

    // TODO Auto-generated catch block
    private val testExceptionFile: File?
        get() {
            var file: File? = null
            try {
                file = File.createTempFile("bbTest", "utd$EXCEPTIONS_TABLE_EXTENSION")
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
            return file
        }

    @JvmStatic
	  fun getCurrentStandardName(m: Manager): String {
        return if (MathModuleUtils.isMath(m.mapList.current.node) &&
            MATH_EXCEPTION_TABLES
        ) {
            MathTranslationSettings.getMathTable(m.document.settingsManager.engine.brailleSettings)
        } else {
            m.document.settingsManager.brailleStandard
        }
    }

    @JvmStatic
	  fun getCurrentExceptionFile(m: Manager): File? {
        return if (MathModuleUtils.isMath(m.mapList.current.node) &&
            MATH_EXCEPTION_TABLES
        ) {
            MATH_TRANSLATION_STANDARD_FILE
        } else {
            mainTranslationStandardFile
        }
    }

    @JvmStatic
	  fun getCurrentExceptionTable(m: Manager): String {
        return if (MathModuleUtils.isMath(m.mapList.current.node)
            && MATH_EXCEPTION_TABLES
        ) {
            m.document.settingsManager.engine.brailleSettings.mathExpressionTable
        } else {
            m.document.settingsManager.engine.brailleSettings.mainTranslationTable
        }
    }
}