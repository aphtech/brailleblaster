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
package org.brailleblaster.perspectives.mvc.modules.misc

import com.google.common.base.CaseFormat
import org.brailleblaster.bbx.BBX
import org.brailleblaster.utils.localization.LocaleHandler
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.views.wp.NumberInputDialog
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.BBStyleOptionSelection
import org.brailleblaster.perspectives.mvc.menu.BBStyleSelection
import org.brailleblaster.perspectives.mvc.menu.SubMenuBuilder
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.Style.StyleOption
import org.brailleblaster.utd.config.StyleDefinitions
import org.brailleblaster.utd.properties.NumberLinePosition
import org.brailleblaster.util.FormUIUtils
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.widgets.*
import java.nio.file.FileSystems
import java.util.*
import java.util.function.Consumer
import java.util.regex.Pattern
import kotlin.math.max

/**
 * Generates the Styles menu
 */
open class StylesBuilder(val shell: Shell, private val manager: Manager) {

  @JvmField
  val styleDefs: StyleDefinitions = manager.document.settingsManager.engine.styleDefinitions

  fun getStyleOptions(
    smb: SubMenuBuilder,
    onOptionSelect: Consumer<BBStyleOptionSelection>,
    onStyleSelect: Consumer<BBStyleSelection>
  ) {
      smb.addItem("Don't Split", 0) { e: BBSelectionData ->
          val styleName = getStyleName(DONT_SPLIT_STYLE_ID)
          val style: IStyle? = styleDefs.getStyleByName(styleName)
          if (style != null) {
              onStyleSelect.accept(
                  BBStyleSelection(
                      style,
                      e.widget
                  )
              )
          }
      }
      smb.addSubMenu(
          SubMenuBuilder(smb, "Keep With Next")
              .addItem("Yes", 0) { e: BBSelectionData ->
                  handleOption(
                      StyleOption.KEEP_WITH_NEXT,
                      true,
                      onOptionSelect,
                      e.widget
                  )
              }
              .addItem("No", 0) { e: BBSelectionData ->
                  handleOption(
                      StyleOption.KEEP_WITH_NEXT,
                      false,
                      onOptionSelect,
                      e.widget
                  )
              }.build()
      )
      smb.addItem("Lines Before", 0) { e: BBSelectionData ->
          handleOption(
              StyleOption.LINES_BEFORE,
              0,
              onOptionSelect,
              e.widget
          )
      }
      smb.addItem("Lines After", 0) { e: BBSelectionData ->
          handleOption(
              StyleOption.LINES_AFTER,
              0,
              onOptionSelect,
              e.widget
          )
      }

      //Not sure what this is for -- comment in case we need it still
//        smb.addSubMenu(new SubMenuBuilder(smb, "Line Number")
//        		.addItem("Yes", 0, e -> handleOption(Style.StyleOption.LINE_NUMBER, true, onOptionSelect, e.widget))
//        		.addItem("No", 0, e -> handleOption(Style.StyleOption.LINE_NUMBER, false, onOptionSelect, e.widget)));
      smb.addSubMenu(
          SubMenuBuilder(smb, "Page Side")
              .addItem("Left", 0) { e: BBSelectionData ->
                  handleOption(
                      StyleOption.PAGE_SIDE,
                      "left",
                      onOptionSelect,
                      e.widget
                  )
              }
              .addItem("Right", 0) { e: BBSelectionData ->
                  handleOption(
                      StyleOption.PAGE_SIDE,
                      "right",
                      onOptionSelect,
                      e.widget
                  )
              }.build()
      )
      smb.addSubMenu(
          SubMenuBuilder(smb, "Skip Number Lines")
              .addItem("Top", 0) { e: BBSelectionData ->
                  handleOption(
                      StyleOption.SKIP_NUMBER_LINES,
                      NumberLinePosition.TOP,
                      onOptionSelect,
                      e.widget
                  )
              }
              .addItem("Bottom", 0) { e: BBSelectionData ->
                  handleOption(
                      StyleOption.SKIP_NUMBER_LINES,
                      NumberLinePosition.BOTTOM,
                      onOptionSelect,
                      e.widget
                  )
              }
              .addItem("Both", 0) { e: BBSelectionData ->
                  handleOption(
                      StyleOption.SKIP_NUMBER_LINES,
                      NumberLinePosition.BOTH,
                      onOptionSelect,
                      e.widget
                  )
              }
              .addItem("None", 0) { e: BBSelectionData ->
                  handleOption(
                      StyleOption.SKIP_NUMBER_LINES,
                      NumberLinePosition.NONE,
                      onOptionSelect,
                      e.widget
                  )
              }.build()
      )
      smb.addItem("New Pages Before", 0) { e: BBSelectionData ->
          handleOption(
              StyleOption.NEW_PAGES_BEFORE,
              0,
              onOptionSelect,
              e.widget
          )
      }
      smb.addItem("New Pages After", 0) { e: BBSelectionData ->
          handleOption(
              StyleOption.NEW_PAGES_AFTER,
              0,
              onOptionSelect,
              e.widget
          )
      }
      smb.addSubMenu(
          SubMenuBuilder(smb, "Guide Words")
              .addItem("Yes", 0) { e: BBSelectionData ->
                  handleOption(
                      StyleOption.GUIDE_WORDS,
                      true,
                      onOptionSelect,
                      e.widget
                  )
              }
              .addItem("No", 0) { e: BBSelectionData ->
                  handleOption(
                      StyleOption.GUIDE_WORDS,
                      false,
                      onOptionSelect,
                      e.widget
                  )
              }.build()
      )
      smb.addItem("Double Spaced", 0) { wrapSelectedElements() }
  }

  private fun handleOption(
    optionKey: StyleOption, optionValueRaw: Any,
    onOptionSelect: Consumer<BBStyleOptionSelection>, widget: Widget
  ) {
    if (optionKey == StyleOption.LINES_BEFORE || optionKey == StyleOption.LINES_AFTER
      || optionKey == StyleOption.NEW_PAGES_BEFORE || optionKey == StyleOption.NEW_PAGES_AFTER
    ) {
      // Retain compatibility with original locale key which used
      // different case
      val lhKey = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, optionKey.name)
      NumberInputDialog(Display.getCurrent().activeShell).open(
        localeHandler.getDefault()[lhKey]
      ) { givenValue: Int ->
        if (givenValue <= 0) return@open
        onOptionSelect.accept(
          BBStyleOptionSelection(
            optionKey,
            givenValue,
            widget
          )
        )
      }
    } else {
      onOptionSelect.accept(
        BBStyleOptionSelection(
          optionKey,
          optionValueRaw,
          widget
        )
      )
    }
  }

  fun getMenuItem(parentMenu: Menu?, label: String?, menuType: Int, miId: Int, keyComb: String?): MenuItem {
    val mi = MenuItem(parentMenu, menuType)
    val tlabel = StringBuilder(localeHandler.getBanaStyles()[label!!])
    if (keyComb != null && keyComb != "") {
      // add spacing to manually added shortcuts
      tlabel.append(" ".repeat(max(0, 35 - tlabel.length)))
      tlabel.append(keyComb)
    }
    mi.text = tlabel.toString()
    mi.id = miId
    return mi
  }

  fun getStyleName(styleId: String): String {
    var styleName = ""
    for (curStyle in styleDefs.styles) {
      if (styleId == curStyle.id) {
        styleName = curStyle.name
        break
      }
    }
    return styleName
  }

  private fun getCategoryIndex(categoryName: String): Int {
    for (i in STYLES_LIST.indices) {
      if (categoryName == STYLES_LIST[i]) return i + 10
    }
    return 0
  }

  fun addIndexes(idList: MutableList<String>) {
    var styleId: String
    var categoryName: String
    val fileSeparator = "/"
    for (i in idList.indices) {
      styleId = idList[i]
      categoryName = styleId.substringBefore(fileSeparator)
      idList[i] = getCategoryIndex(categoryName).toString() + "-" + styleId
    }
  }

  private fun wrapSelectedElements() {
    val currentSelection = manager.simpleManager.currentSelection
    val startNode = currentSelection.start.node
    val endNode = currentSelection.end.node
    val container = BBX.CONTAINER.DOUBLE_SPACE.create()
    StylesMenuModule.wrapSelectedElements(manager, container, startNode, endNode)
  }

  companion object {
    private val localeHandler = LocaleHandler
    const val DEFAULT_STYLE_LEVELS = 5
    private val STYLES_LIST: List<String> = ArrayList(
      listOf(
        "basic",
        "lists",
        "heading",
        "captions",
        "exerciseMaterial",
        "glossary",
        "index",
        "miscellaneous",
        "notes",
        "numeric",
        "plays",
        "poetry"
      )
    )
    const val INTERNAL_CATEGORY_NAME = "internal"
    const val LEVELS_INDICATOR = "LEVELS"
    const val OPTIONS_CATEGORY_NAME = "options"
    var sep: String = FileSystems.getDefault().separator
    fun newMenuItem(parent: Menu?, text: String?, onSelect: Consumer<SelectionEvent>): MenuItem {
      val item = MenuItem(parent, SWT.PUSH)
      item.text = text
      FormUIUtils.addSelectionListener(item, onSelect)
      return item
    }

    private const val DONT_SPLIT_STYLE_ID: String = "internal/dontSplit"
  }
}

/**
 * Comparator to sort strings based on category, subcategory and id
 */
internal class StyleIdComparator : Comparator<String> {
  /**
   * Supports strings sorting based on indent and level.
   *
   * @param s1 the first string to compare
   * @param s2 the second string to compare
   * @return result of the comparison as int
   */
  override fun compare(s1: String, s2: String): Int {
    val matcher1 = fullPattern.matcher(s1)
    val matcher2 = fullPattern.matcher(s2)
    return if (matcher1.matches() && matcher2.matches()) {
      val s1FirstPart = matcher1.group(1)
      val s2FirstPart = matcher2.group(1)
      var retValue = s1FirstPart.compareTo(s2FirstPart)

      // If both strings are the same until the last slash, then compare after the slash.
      if (retValue == 0) {
        val s1Letter = matcher1.group(2)
        val s1Indent = matcher1.group(3).toInt()
        val s1Level = matcher1.group(4).toInt()
        val s2Letter = matcher2.group(2)
        val s2Indent = matcher2.group(3).toInt()
        val s2Level = matcher2.group(4).toInt()
        retValue = s1Letter.compareTo(s2Letter)
        if (retValue == 0) {
          retValue = s1Indent.compareTo(s2Indent)
          if (retValue == 0) {
            s1Level.compareTo(s2Level)
          } else retValue
        } else retValue
      } else retValue
    } else {
      val semiMatcher1 = semiPattern.matcher(s1)
      val semiMatcher2 = semiPattern.matcher(s2)
      if (semiMatcher1.matches() && semiMatcher2.matches()) {
        val s1FirstPart = semiMatcher1.group(1)
        val s2FirstPart = semiMatcher2.group(1)
        var retValue = s1FirstPart.compareTo(s2FirstPart)

        // If they match on the first part, match the second part.
        if (retValue == 0) {
          val s1SubPart = semiMatcher1.group(2)
          val s2SubPart = semiMatcher2.group(2)
          retValue = s1SubPart.compareTo(s2SubPart)
          if (retValue == 0) {
            val s1PreLevel = semiMatcher1.group(3).toInt()
            val s2PreLevel = semiMatcher2.group(3).toInt()
            retValue = s1PreLevel.compareTo(s2PreLevel)
            if (retValue == 0) {
              val s1Indent = semiMatcher1.group(4).toInt()
              val s1Level = semiMatcher1.group(5).toInt()
              val s2Indent = semiMatcher2.group(4).toInt()
              val s2Level = semiMatcher2.group(5).toInt()
              retValue = s1Indent.compareTo(s2Indent)
              if (retValue == 0) {
                retValue = s1Level.compareTo(s2Level)
              }
            }
          }
        }
        retValue
      } else {
        s1.compareTo(s2)
      }
    }
  }

  companion object {
    private val fullPattern = Pattern.compile("^([^/]*)/([^\\d]+)([0-9]+)-([0-9]+)([^\\d]*)$")
    private val semiPattern =
      Pattern.compile("^([^/]*)/([^\\d]*)([0-9]+)[^\\d/]*/[^\\d]*([0-9]+)-([0-9]+)([^\\d]*)$")
  }
}