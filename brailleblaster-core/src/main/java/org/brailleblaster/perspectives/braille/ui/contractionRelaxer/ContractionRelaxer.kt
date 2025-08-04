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
package org.brailleblaster.perspectives.braille.ui.contractionRelaxer

import org.brailleblaster.BBIni
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.settings.TableExceptions
import org.brailleblaster.settings.TableExceptions.getCurrentExceptionFile
import org.brailleblaster.tools.MenuToolListener
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Dialog
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text
import org.mwhapples.jlouis.Louis
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import kotlin.experimental.and

//Should probably set this as a dialog in the translation settings menu - and only have it enabled when
// the translation is set to UEB Uncontracted. Doesn't make sense otherwise.
class ContractionRelaxer(parent: Shell) : Dialog(parent, SWT.NONE), MenuToolListener {
  override val topMenu: TopMenu = TopMenu.TOOLS
  override val title: String = "Contraction Relaxer"

  private var changeToContracted: Boolean = false
  private var unitToggle = 0

  class Unit(val unitName: String, val contractions: List<String>)

  //For ticket #12864
  //UI for toggling select UEB translation substitutions
  //Need to look into whether our translation libraries can support this the easy way or the hard way.
  //Should this be part of the translation settings dialog?
  //At least, it only makes sense to have enabled when UEB Uncontracted is selected.

  override fun onRun(bbData: BBSelectionData) {
    //Get the property for the unit toggle.
    BBIni.propertyFileManager.getProperty(UNIT_TOGGLE_PROPERTY)?.let {
      unitToggle = it.toInt()
    }

    val shell = Shell(parent.display, SWT.DIALOG_TRIM or SWT.RESIZE or SWT.CENTER)
    shell.text = "Contraction Relaxer"
    shell.layout = GridLayout(1, false)

    val shellText = EasySWT.makeLabel(shell)
    shellText.text("Select a unit to toggle specific translation rules.\nThis will allow you to specify words and contractions up to and including the selected unit.")

    //Drop-down list with all the units to toggle rules.
    val unitSelector = EasySWT.makeComboDropdown(shell)
    for (unit in UnitList.allUnits) {
      unitSelector.add(unit.unitName)
    }
    unitSelector.select(unitToggle)
    unitSelector.onSelect {
      unitToggle = unitSelector.get().selectionIndex
      //println("Unit selected: ${UnitList.allUnits[unitToggle].unitName}")
    }

    val buttonLayout = EasySWT.makeComposite(shell, 3)

    val saveBtn = EasySWT.makeButton(buttonLayout, SWT.PUSH)
    saveBtn.text("OK")
    saveBtn.onSelection {
      //Save settings to the translation file
      if (unitToggle >= 0) {
        clearAndSave(bbData)
        shell.close()
      }
    }

    //TODO
    //The preview box never worked properly -
    // just have a separate window to view the full list of selected contractions.
    val previewBtn = EasySWT.makeButton(buttonLayout, SWT.PUSH)
    previewBtn.text("View Contractions")
    previewBtn.onSelection {
      //println("View contractions button pressed.")
      viewContractions(shell)
    }

    val closeBtn = EasySWT.makeButton(buttonLayout, SWT.PUSH)
    closeBtn.text("Cancel")
    closeBtn.onSelection {
      shell.close()
    }

    EasySWT.addEscapeCloseListener(shell)
    shell.pack()
    shell.open()
  }


  fun clearAndSave(bbData: BBSelectionData) {
    //println("Clearing and saving contraction settings")
    val file = getCurrentExceptionFile(bbData.manager)
      ?: throw RuntimeException("Unable to save exceptions to translation correction file - no file found.")

    val fileContent = file.readText(StandardCharsets.UTF_8)
    val headerIndex = fileContent.indexOf(RELAXER_HEADER)
    val footerIndex = fileContent.lastIndexOf(RELAXER_FOOTER)
    //println("Header index: $headerIndex; Footer index: $footerIndex; Header count: $headerCount; Footer count: $footerCount. File content:")

    val contractionList = makeContractionList()

    if (headerIndex == -1 && footerIndex == -1) {
      //println("Header and footer not found. Appending new header and footer along with contraction list.")
      OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8).use { writer ->
        writer.append(RELAXER_HEADER)
        writer.append(System.lineSeparator())
        writer.append(contractionList)
        writer.append(RELAXER_FOOTER)
      }
    } else if (headerIndex < footerIndex) {
      //Doc is structured correctly, so clear everything between the header and footer.
      //println("Header and Footer found. Writing new contraction list.")
      val newContent =
        fileContent.substring(0, headerIndex + RELAXER_HEADER.length) +
            System.lineSeparator() +
            contractionList +
            fileContent.substring(footerIndex)

      OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8).use { writer ->
        writer.write(newContent)
      }
    } else {
      //Shouldn't happen, but if it does, just overwrite the file.
      //println("Header is after footer or missing header/footer. Fully overwriting file.")
      OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8).use { writer ->
        writer.write("")
        writer.append(RELAXER_HEADER)
        writer.append(System.lineSeparator())
        writer.append(contractionList)
        writer.append(RELAXER_FOOTER)
      }
    }

    if(changeToContracted){
      //Change the translation mode to UEB (grade 2)
      bbData.manager.document.settingsManager.updateBrailleStandard(bbData.manager.document.doc, "UEB")
      changeToContracted = false
    }
    else{
      //Ensure the translation mode is UEB-Uncontracted (grade 1)
      bbData.manager.document.settingsManager.updateBrailleStandard(bbData.manager.document.doc, "UEB-UNCONTRACTED")
    }

    //println("Attempting to save unitToggle: $unitToggle")
    //Save the unitToggle setting
    BBIni.propertyFileManager.saveAsInt(UNIT_TOGGLE_PROPERTY, unitToggle)
    //println("UnitToggle saved. Attempting to refresh document and translator.")
    bbData.manager.document.engine.brailleTranslator.close()
    bbData.manager.refresh()
  }

  fun viewContractions(shell: Shell) {
    //TODO: Make a legible list of contractions for each unit. The liblouis tables are not exactly human-readable.
    val contractionList = makePrettyContractionList()

    val vc =  Shell(shell, SWT.APPLICATION_MODAL or SWT.DIALOG_TRIM)
    vc.text = "Contraction List"
    vc.layout = GridLayout(1, false)

    val comp = EasySWT.makeComposite(vc, SWT.NONE)
    comp.layout = GridLayout(1, false)
    comp.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)

    val myGrid = GridData(SWT.FILL, SWT.FILL, true, true)
    val bounds = Display.getCurrent().primaryMonitor.clientArea
    myGrid.widthHint = bounds.width / 4
    myGrid.heightHint = bounds.height / 3

    val st = Text(comp, SWT.BORDER or SWT.V_SCROLL or SWT.H_SCROLL or SWT.MULTI or SWT.READ_ONLY)
    st.layoutData = myGrid
    st.text = contractionList
    st.editable = false

    vc.pack()
    vc.open()
  }

  fun makeContractionList(): String {
    //println("Making contraction list. UnitToggle: $unitToggle")
    var totalUnits = 0
    val f: StringBuilder = StringBuilder()

    if (unitToggle == 0 || unitToggle == UnitList.allUnits.lastIndex){
      //Return nothing - no need to add contractions if None or All is selected.
      if (unitToggle == UnitList.allUnits.lastIndex){
        changeToContracted = true
      }
      return ""
    }

    //Start at 1, since the 0th unit has no contraction list.
    for (i in 1..unitToggle) {
      val unit = UnitList.allUnits[i]
      //println("Adding: ${unit.unitName}")
      for (c in unit.contractions) {
        f.append(c)
        f.append(System.lineSeparator())
        totalUnits++
      }
    }
    //println("Total contractions added: $totalUnits")
    return f.toString()
  }

  fun makePrettyContractionList(): String {
    //This is used for the preview window.
    val f: StringBuilder = StringBuilder()
    for (i in 0..unitToggle) {
      val unit = UnitList.allPrettyUnits[i]
      for (c in unit) {
        f.append(c)
        f.append(System.lineSeparator())
      }
    }
    return f.toString()
  }


  //Convert an ascii braille string into a dot string
  //IE "for" -> "123456"
  //Braille is now hard-coded, but I used this method to generate it. Keep it - it's useful.
  fun brailleAsciiToDots(brailleAscii: String, bbData: BBSelectionData): String {
    val translation = bbData.manager.document.engine.brailleTranslator
    val inputBuffer = Louis.WideChar(brailleAscii)
    val outputBuffer = Louis.WideChar(brailleAscii.length)
    translation.charToDots(
      TableExceptions.getCurrentExceptionTable(bbData.manager),
      inputBuffer, outputBuffer, brailleAscii.length, 0
    )
    val brailleDots = outputBuffer.getText(outputBuffer.length())
    val brailleBytes = brailleDots.toByteArray(StandardCharsets.UTF_16LE)

    var mask: Byte = 0x01
    val result = StringBuilder()
    for (d in brailleBytes) {
      //Don't include negative bytes.
      if (d >= 0) {
        for (c in 1..6) {
          if ((d and mask) == mask) {
            result.append(c)
          }
          mask = (mask.toInt() shl 1).toByte()
        }
        result.append("-")
      }
      mask = 0x01
    }
    //Delete the last char, as it will always be a dash regardless of the final length of the string.
    //It's a little silly, but it keeps the loop simple.
    result.deleteCharAt(result.length - 1)

    //println("Braille dots for $brailleAscii: $result")
    return result.toString()
  }

  companion object {
    private const val UNIT_TOGGLE_PROPERTY = "ContractionRelaxer.unitToggle"
    private const val RELAXER_HEADER = "#Begin UEB Contraction Relaxer"
    private const val RELAXER_FOOTER = "#End UEB Contraction Relaxer"
  }

}
