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
package org.brailleblaster.spellcheck

import org.brailleblaster.BBIni
import org.brailleblaster.utils.Architecture
import org.brailleblaster.utils.OS
import org.brailleblaster.utils.arch
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.utils.os
import org.slf4j.LoggerFactory

class SpellChecker(private val dictPath: String, private val affPath: String) {
    var isActive: Boolean

    init {

        //TODO: Remove when better hunspell library is chosen
        if (OS.Windows != os) {
            throw RuntimeException("Hunspell Spell Check is currently only supported on Windows.")
        }
        try {
            val path = BBIni.nativeLibraryPath.resolve("bbhunspell_"
                    + (if (Architecture.X86_64 == arch) "x86-64" else "x86")
                    + BBIni.nativeLibrarySuffix
            ).toString()
            log.debug("Loading hunspell library at $path")
            System.load(path)
        } catch (e: Exception) {
            throw RuntimeException("Could not load library", e)
        }
        val result = open(dictPath, affPath)
        isActive = result == 1
    }

    private fun open(dictPath: String, affPath: String): Int {
        return try {
            openDict(dictPath, affPath)
        } catch (e: UnsatisfiedLinkError) {
            log.error("openDict Unsatisfied Link Error", e)
            -1
        }
    }

    fun open() {
        open(dictPath, affPath)
    }

    fun close() {
        closeDict()
    }

    fun checkSpelling(word: String): Boolean {
        val result = checkWord(word)
        return result > 0
    }

    //hunspell's addWord function only adds to the runtime dictionary
    fun addToDictionary(word: String) {
        addWord(word)
    }

    fun getSuggestions(word: String): Array<String> {
        val s = checkSug(word)
        return s.makeArray()
    }

    companion object {
        private val log = LoggerFactory.getLogger(SpellChecker::class.java)
        private external fun openDict(dictPath: String, affPath: String): Int
        private external fun checkWord(wd: String): Int
        private external fun addWord(wd: String): Int
        private external fun checkSug(wd: String): Suggestions
        private external fun closeDict()
    }
}

internal class Suggestions {
    var suggestionList: String? = null
    fun makeArray(): Array<String> {
        val sl = suggestionList
        return if (sl.isNullOrEmpty()) {
            arrayOf(localeHandler["noSuggestion"])
        } else sl.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }

    companion object {
        private val localeHandler = getDefault()
    }
}
