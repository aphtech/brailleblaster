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
package org.brailleblaster.perspectives.braille.spellcheck

import org.apache.commons.lang3.exception.ExceptionUtils
import org.brailleblaster.BBIni
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.tools.MenuToolModule
import org.brailleblaster.util.FileUtils
import org.brailleblaster.util.Notify
import org.eclipse.swt.SWT
import org.slf4j.LoggerFactory
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

private val localeHandler = getDefault()

object SpellCheckTool : MenuToolModule {
  override val topMenu: TopMenu = TopMenu.TOOLS
  override val title: String = localeHandler["&SpellCheck"]
  override val accelerator: Int = SWT.F7
  override val enabled: Boolean = false
  override fun onRun(bbData: BBSelectionData) {
    SpellCheckManager(bbData.manager).open()
  }
}

class SpellCheckManager(private var m: Manager) {
  private var sc: SpellChecker? = null
  private var view: SpellCheckView? = null
  private var tokenizer: Tokenizer? = null
  private val ignoreList = Vector<String>()
  private lateinit var dictPath: String
  private lateinit var affPath: String
  private var dictLang: String? = null
  private var correctSpelling = false

  fun open() {
    if (!m.ignoreList.isEmpty()) {
      for (dc in m.ignoreList) {
        ignoreList.add(dc)
      }
    }
    try {
      dictLang = localeHandler["dictionary"]
      val dictPath = FileUtils.findInProgramData("dictionaries${FileSystems.getDefault().separator}$dictLang.dic")
      val affPath = FileUtils.findInProgramData("dictionaries${FileSystems.getDefault().separator}$dictLang.aff")
      if (dictPath != null && affPath != null) {
        this.dictPath = dictPath
        this.affPath = affPath
        sc = SpellChecker(dictPath, affPath)
        if (sc!!.isActive) {
          view = SpellCheckView(m.wpManager.shell, this)
          tokenizer = Tokenizer(m.text.view.text.replace("\n", " "))
          checkWord()
        } else {
          Notify.notify(localeHandler["spellCheckError"], Notify.ALERT_SHELL_NAME)
        }
      } else Notify.notify(localeHandler["spellCheckError"], Notify.ALERT_SHELL_NAME)
    } catch (e: Throwable) {
      log.error("Failed to load hunspell", e)
      Notify.notify(ExceptionUtils.getRootCauseMessage(e), Notify.EXCEPTION_SHELL_NAME)
    }
  }

  private fun setWord(word: String?, suggestions: Array<String>) {
    view!!.setWord(word, suggestions)
    m.text.highlight(tokenizer!!.startPos, tokenizer!!.endPos)
  }

  fun checkWord() {
    correctSpelling = true
    while (correctSpelling && tokenizer!!.next()) {
      if (!ignoreList.contains(tokenizer!!.currentWord)) {
        correctSpelling = sc!!.checkSpelling(tokenizer!!.currentWord)
        if (!correctSpelling) {
          val suggestions = sc!!.getSuggestions(tokenizer!!.currentWord)
          if (tokenizer!!.splitPos != 0
            && tokenizer!!.splitPos != tokenizer!!.currentWord.length
          ) {
            // Caught a word that probably needs a space
            var word2: String
            val punc = ".?!"
            val word1: String = tokenizer!!.currentWord.substring(0, tokenizer!!.splitPos)
            word2 = if (punc.contains(
                tokenizer!!.currentWord[tokenizer!!.splitPos - 1].toString()
              )
            ) {
              // If we're splitting a word at a . ! or ? the
              // second word should be capitalized.
              tokenizer!!.currentWord[tokenizer!!.splitPos].uppercaseChar()
                .toString() + tokenizer!!.currentWord.substring(
                tokenizer!!.splitPos + 1
              )
            } else {
              tokenizer!!.currentWord.substring(tokenizer!!.splitPos)
            }
            // Make a new suggestions array that includes existing
            // words with space
            val newSuggestions = arrayOf("$word1 $word2", *suggestions)
            setWord(tokenizer!!.currentWord, newSuggestions)
          } else {
            setWord(tokenizer!!.currentWord, suggestions)
          }
        } else { // correctSpelling == true
          if (tokenizer!!.capFlag) { // Something needs capitalization
            tokenizer!!.next()
            correctSpelling = false
            val capsSuggestion = arrayOf((tokenizer!!.currentWord.substring(0, 1).uppercase(Locale.getDefault())
                + tokenizer!!.currentWord.substring(1)))
            setWord(tokenizer!!.currentWord, capsSuggestion)
            tokenizer!!.capFlag = false
          }
        }
      }
    }
    if (tokenizer!!.isComplete) {
      view!!.close()
      Notify.notify(localeHandler["checkComplete"], Notify.ALERT_SHELL_NAME)
    }
  }

  fun addWord(word: String) {
    val userDictPath =
      BBIni.userProgramDataPath.resolve(Paths.get("dictionaries", "$dictLang.dic"))
    if (Files.exists(userDictPath)) {
      sc!!.addToDictionary(word)
      FileUtils.appendToFile(dictPath, word)
    } else {
      val oldPath = dictPath
      dictPath =
        BBIni.userProgramDataPath.resolve(Paths.get("dictionaries", "$dictLang.dic")).toString()
      val oldAffPath = affPath
      affPath =
        BBIni.userProgramDataPath.resolve(Paths.get("dictionaries", "$dictLang.aff")).toString()
      FileUtils.create(dictPath)
      FileUtils.copyFile(oldPath, dictPath)
      FileUtils.create(affPath)
      FileUtils.copyFile(oldAffPath, affPath)
      FileUtils.appendToFile(dictPath, word)
      sc!!.addToDictionary(word)
    }
    m.newIgnore(word)
  }

  fun ignoreWord(word: String) {
    m.newIgnore(word)
    ignoreList.add(word)
  }

  fun replace(text: String) {
    m.text.copyAndPaste(text, tokenizer!!.startPos, tokenizer!!.endPos)
    tokenizer!!.resetText(m.text.view.text.replace("\n", " "))
  }

  fun replaceAll(oldWord: String, newWord: String) {
    sc!!.addToDictionary(newWord)
    if (oldWord != newWord) {
      val tk = Tokenizer(
        m.text.view.text.replace("\n", " "), tokenizer!!.startPos, tokenizer!!.endPos
      )
      do {
        if (tk.currentWord == oldWord) {
          m.text.copyAndPaste(newWord, tk.startPos, tk.endPos)
          tk.resetText(m.text.view.text.replace("\n", " "))
        }
        tk.next()
      } while (!tk.isComplete)
      tokenizer!!.resetText(m.text.view.text.replace("\n", " "))
    }
  }

  fun closeSpellChecker() {
    if (sc!!.isActive) sc!!.close()
    val pos = m.text.view.caretOffset
    m.text.view.setSelection(pos, pos)
    //	m.dispatch(new SetCurrentMessage(Sender.TEXT, pos, false));
  }

  companion object {
    private val log = LoggerFactory.getLogger(SpellCheckManager::class.java)
    private val localeHandler = getDefault()
  }
}