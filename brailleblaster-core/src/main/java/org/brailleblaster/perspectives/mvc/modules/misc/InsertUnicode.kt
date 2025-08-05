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

import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.tools.MenuToolModule
import org.brailleblaster.utils.swt.EasyListeners
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.dnd.Clipboard
import org.eclipse.swt.dnd.TextTransfer
import org.eclipse.swt.dnd.Transfer
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import java.util.HexFormat


class InsertUnicode(parent: Shell) : Dialog(parent, SWT.NONE), MenuToolModule {
  override val topMenu: TopMenu = TopMenu.INSERT
  override val title: String = SHELL_NAME

  //Create a pop-up SWT window for a user to manually enter a unicode value
  //Then insert a unicode character at the location of the cursor.
  override fun onRun(bbData: BBSelectionData) {
    val shell = Shell(
      parent.display,
      SWT.DIALOG_TRIM or SWT.RESIZE or SWT.CENTER //or SWT.APPLICATION_MODAL
    )
    shell.text = SHELL_NAME
    shell.layout = GridLayout(1, false)

    //Label, text, flat button (for displaying unicode symbol), push button (to insert)
    val inputComp = EasySWT.makeGroup(shell, SWT.CENTER, 4, false)
    val unicodeLabel = Label(inputComp, SWT.RIGHT)
    unicodeLabel.text = UNICODE_LABEL

    val entryText = EasySWT.makeText(inputComp, 40, 1)
    entryText.toolTipText = TOOLTIP_TEXT
    entryText.textLimit = 4

    //Show the name of the potential character to the user before inserting it.
    val displayText = Text(inputComp, SWT.READ_ONLY or SWT.BORDER or SWT.CENTER)

    entryText.addModifyListener {
      //Want to update the displayText whenever text is added/removed
      val newText = convertUnicodeChar(entryText.text)
      displayText.text = newText.toString()
      displayText.toolTipText = Character.getName(newText.code)
    }

    EasySWT.addEnterListener(entryText) {
      addUnicodeChar(convertUnicodeChar(entryText.text))
      entryText.text = ""
      entryText.forceFocus()
    }

    EasyListeners.verifyHexadecimal(entryText)

    EasySWT.makePushButton(inputComp, BUTTON_NAME, 1)
    {
      addUnicodeChar(convertUnicodeChar(entryText.text))
      entryText.text = ""
      entryText.forceFocus()
    }

    val buttonGrid = EasySWT.makeGroup(shell, SWT.CENTER, GRID_SIZE, true)
    buttonGrid.layout = GridLayout(GRID_SIZE, true)
    buttonGrid.text = COMMON_SYMBOLS

    //Make a button for each symbol declared in the list. Much easier to expand or modify this way.
    for (s in commonSymbols) {
      val pb = EasySWT.makePushButton(buttonGrid, " " + s.toChar().toString() + " ", 1) {
        val hexVal = "0x" + HexFormat.of().toHexDigits(s)
        entryText.text = hexVal.substring(6)
        entryText.forceFocus()
      }
      pb.toolTipText = Character.getName(s)
    }

    EasySWT.addEscapeCloseListener(shell)
    shell.pack()
    shell.open()
  }

  private fun addUnicodeChar(char: Char) {
    val tt = TextTransfer.getInstance()
    val clipboard = Clipboard(Display.getCurrent())
    clipboard.clearContents()
    if (char != ' '){
      //copy char to clipboard
      clipboard.setContents(arrayOf(char.toString()), arrayOf<Transfer>(tt))
    }
    //println("Char $char copied to clipboard")
    return
  }

  /** Checks if inputted string is a valid / desired Unicode char
   * @param text string to convert
   * @param base Default is base 16 (hex), but can be overwritten
   *
   * @returns a Unicode char. Will return a regular space ' ' if things go wrong.
   */
  //TODO: Replace this with native Kotlin/Java language calls. Character class has the digit() and forDigit() functions.
  private fun convertUnicodeChar(text: String, base: Int = 16): Char {
    val convertedChar = try {
      text.toInt(base).toChar()
    } catch (nfe: NumberFormatException) {
      //What shall we do with the badly formatted string?
      ' ' //Add a regular space instead!
    }
    return convertedChar
  }

  companion object {
    const val MENU_NAME = "Unicode Character"
    const val SHELL_NAME = "Insert Unicode"
    const val BUTTON_NAME = "Copy to Clipboard"
    const val GRID_SIZE = 5 //How many columns of symbols should appear. Can easily add more this way.
    const val TOOLTIP_TEXT = "Enter a 4-digit Unicode key in Hexadecimal (0-F)"
    const val UNICODE_LABEL = "U+"
    const val COMMON_SYMBOLS = "Common Symbols:"

    //val MENU_NAME = LocaleHandler.getDefault()["&Insert Page Number"]
    //Unicodes for various common symbols
    val commonSymbols = intArrayOf(
      0x0024, //dollar
      0x00A2, //cent
      0x00A3, //pound
      0x20AC, //euro
      0x00A5, //yen

      0x2018, //lQuote
      0x2019, //rQuote
      0x00BF, //inverted question
      0x00A1, //inverted exclamation
      0x2013, //en Dash

      0x00AE, //registered
      0x00A9, //copyright
      0x2122, //trademark
      0x00B0, //degree
      0x2014 //em Dash
    )
    //The intArray can be expanded or shrunk as needed. It'd be nifty to have it
    // user-configurable from a file.
  }
}