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

class Tokenizer {
    private var text: String
    var startPos: Int
        private set
    var endPos: Int
        private set
    var splitPos = 0
        private set
    var isComplete: Boolean
        private set
    var capFlag = false

    constructor(text: String) {
        startPos = 0
        endPos = 0
        capFlag = false
        this.text = text.replace("\u2019".toRegex(), "'")
        isComplete = false
    }

    constructor(text: String, startPos: Int, endPos: Int) {
        this.startPos = startPos
        this.endPos = endPos
        this.text = text.replace("\u2019".toRegex(), "'")
        isComplete = false
    }

    private fun setStartPos() {
        if (endPos > 0) startPos = endPos + 1
        while (startPos < text.length && !Character.isLetter(text[startPos]) && !Character.isDigit(text[startPos])) {
            startPos++
        }
    }

    private fun setEndPos() {
        endPos = startPos
        splitPos = 0
        val punctuation = ".,;:?!" //List of punctuation marks that require a space after
        val sentenceEnd = ".?!" //List of punctuation that ends a sentence
        while (endPos < text.length && (Character.isLetter(text[endPos]) || Character.isDigit(text[endPos]) || text[endPos] == '\'')) {
            endPos++
            if (endPos + 2 < text.length - 1) {
                if (punctuation.contains(text[endPos].toString())) { //We are at a punctuation mark.
                    if (text[endPos + 1] != ' ') {
                        //If the next character isn't a space, something might be wrong
                        if (text[endPos + 2] != '.') {
                            //If it is initials, ignore it
                            if (text[endPos + 1] != '"' && text[endPos + 1] != '\'' && text[endPos + 1] != '"') {
                                //If it is the end of a quote, ignore it
                                endPos++ //Set endPos past the period - user likely forgot a space
                                splitPos = endPos - startPos //Denotes location of period for SpellCheckManager
                            }
                        }
                    } else { //If it is a space, the following character needs to be capitalized
                        if (sentenceEnd.contains(text[endPos].toString())) { //Make sure it isn't a comma
                            if (Character.isLowerCase(text[endPos + 2])) {
                                capFlag = true
                            }
                        }
                    }
                }
            }
        }
    }

    fun resetText(text: String) {
        this.text = text.replace("\u2019".toRegex(), "'")
        setEndPos()
    }

    val currentWord: String
        get() = text.substring(startPos, endPos)

    operator fun next(): Boolean {
        setStartPos()
        setEndPos()
        return if (startPos < text.length) true else {
            isComplete = true
            false
        }
    }
}
