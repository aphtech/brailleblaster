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
package org.brailleblaster.math.ascii

import org.brailleblaster.BBIni
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault

class MathDialogSettings {
  var currentName = CATEGORIES.EXAMPLES
  var isFullScreen = true
  var currentAscii = ""
  val categories = ArrayList<ASCIICategory>()
  var lastSearch = ""

  enum class CATEGORIES(val idName: String) {
    OPERATION_SYMBOLS("operation"),
    MISCELLANEOUS("miscellaneous"),
    RELATION("relation"),
    GREEK("greek"),
    LOGICAL("logical"),
    GROUPING("grouping"),
    ARROWS("arrows"),
    ACCENTS("accents"),
    FUNCTIONS("functions"),
    SPECIAL("special"),
    EXAMPLES("examples"),
    SEARCH("search");

    val prettyName: String = localeHandler[idName]

  }

    fun saveSettings() {
    BBIni.propertyFileManager.save(CURRENT_TAB, currentName.prettyName)
    BBIni.propertyFileManager.save(FULLSCREEN, isFullScreen.toString())
  }

  fun loadSettings() {
    val info = ASCIILoadJAXB()
    for (s in info.categories) {
      val array = info.getEntries(s)
      categories.add(ASCIICategory(array, getCategoryIdName(s)!!)) //shouldn't this be PrettyName?
    }
    categories.add(ASCIICategory(ArrayList(), getCategoryIdName("search")!!))
    if (BBIni.propertyFileManager.getProperty(CURRENT_TAB) != null) {
      val currentNameString = BBIni.propertyFileManager.getProperty(CURRENT_TAB)
      for (i in CATEGORIES.entries.toTypedArray().indices) {
        val c = CATEGORIES.entries[i]
        if (c.prettyName == currentNameString) {
          currentName = c
          break
        }
      }
    }
    if (BBIni.propertyFileManager.getProperty(FULLSCREEN) != null) {
      isFullScreen = (BBIni.propertyFileManager.getPropertyAsBoolean(FULLSCREEN, false))
    }
  }

  val categoriesEnum: ArrayList<CATEGORIES>
    get() = ArrayList(CATEGORIES.entries)

  fun getCategoryIdName(idName: String): CATEGORIES? {
    for (i in CATEGORIES.entries.toTypedArray().indices) {
      if (CATEGORIES.entries[i].idName == idName) {
        return CATEGORIES.entries[i]
      }
    }
    return null
  }

  fun getCategoryPrettyName(idName: String): CATEGORIES? {
    for (i in CATEGORIES.entries.toTypedArray().indices) {
      if (CATEGORIES.entries[i].prettyName == idName) {
        return CATEGORIES.entries[i]
      }
    }
    return null
  }

  val categoriesEnumStringArray: Array<String>
    get() {
        return CATEGORIES.entries.map { it.prettyName }.toTypedArray()
    }

  val currentCategory: ASCIICategory?
    get() {
      for (i in categories.indices) {
        if (categories[i].category == currentName) {
          return categories[i]
        }
      }
      return null
    }

  companion object {
    private val localeHandler = getDefault()
    private const val FULLSCREEN = "mathsettings.fullscreen"
    private const val CURRENT_TAB = "mathsettings.tab"
  }
}