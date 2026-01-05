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
package org.brailleblaster.settings.ui

import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.utd.PageSettings
import org.brailleblaster.utd.UTDTranslationEngine
import org.brailleblaster.utd.properties.PageNumberPosition
import org.brailleblaster.utils.swt.AccessibilityUtils.appendName
import org.brailleblaster.util.FormUIUtils
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*

class PageNumbersTab internal constructor(folder: TabFolder, pageSettingsDefault: PageSettings) : SettingsUITab {
    private val evenBrailleNumCombo: Combo
    private val evenPrintNumCombo: Combo
    private val continueSymbolsCombo: Combo
    private val continuePagesCombo: Combo
    private val guideWordsCombo: Combo
    private val oddBrailleNumCombo: Combo
    private val oddPrintNumCombo: Combo
    private val continuationIndicatorCombo: Combo
    var shell: Shell = folder.shell

    init {
        val item = TabItem(folder, 0)
        item.text = localeHandler["pageNumbers"]

        //----Setup UI----
        val parent = Composite(folder, 0)
        parent.layout = GridLayout(1, true)
        item.control = parent

        //Braille group
        val brailleGroup = Group(parent, 0)
        brailleGroup.layout = GridLayout(2, true)
        brailleGroup.text = localeHandler["braille"]
        EasySWT.setGridDataGroup(brailleGroup)
        val evenBraille = EasySWT.addLabel(brailleGroup, "Even Braille Page Number")
        evenBraille.toolTipText = "Sets location of even braille page number."
        evenBrailleNumCombo = makeNumberPositionCombo(brailleGroup, pageSettingsDefault.evenBraillePageNumberAt)
        appendName(evenBrailleNumCombo, "Sets location of even braille page number.")
        val oddBraille = EasySWT.addLabel(brailleGroup, "Odd Braille Page Number")
        oddBraille.toolTipText = "Sets location of odd braille page number."
        oddBrailleNumCombo = makeNumberPositionCombo(brailleGroup, pageSettingsDefault.oddBraillePageNumberAt)
        appendName(oddBrailleNumCombo, "Sets location of odd braille page number.")

        //Print group
        val printGroup = Group(parent, 0)
        printGroup.layout = GridLayout(2, true)
        printGroup.text = localeHandler["print"]
        EasySWT.setGridDataGroup(printGroup)
        val evenPrint = EasySWT.addLabel(printGroup, "Even Print Page Number")
        evenPrint.toolTipText = "Sets location of print page number when the braille page number is even."
        evenPrintNumCombo = makeNumberPositionCombo(printGroup, pageSettingsDefault.evenPrintPageNumberAt)
        appendName(evenPrintNumCombo, "Sets location of print page number when the braille page number is even.")
        val oddPrint = EasySWT.addLabel(printGroup, "Odd Print Page Number")
        oddPrint.toolTipText = "Sets location of print page number when the braille page number is odd."
        oddPrintNumCombo = makeNumberPositionCombo(printGroup, pageSettingsDefault.oddPrintPageNumberAt)
        appendName(oddPrintNumCombo, "Sets location of print page number when the braille page number is odd.")

        //Continuation Symbols For Print Pages
        val contSymbols = EasySWT.addLabel(printGroup, "Lettered Continuation Pages")
        contSymbols.toolTipText = "Sets whether a letter appears to the left of a print page number on runovers."
        continueSymbolsCombo = makeYesNoCombo(printGroup, pageSettingsDefault.isPrintPageNumberRange)
        appendName(
            continueSymbolsCombo,
            "Sets whether a letter appears to the left of a print page number on runovers."
        )
        val contIndicator = EasySWT.addLabel(printGroup, "Continuation Indicator For Print Pages")
        contIndicator.toolTipText =
            "Uses Grade 1 symbol indicator between continuation letter and alphabetic page number."
        continuationIndicatorCombo = makeYesNoCombo(printGroup, pageSettingsDefault.isPrintPageLetterIndicator)
        appendName(
            continuationIndicatorCombo,
            "Use Grade 1 symbol indicator between continuation letter and alphabetic page number."
        )

        //Continue pages group
        val cpGroup = Group(parent, 0)
        cpGroup.layout = GridLayout(2, true)
        cpGroup.text = localeHandler["continue"]
        EasySWT.setGridDataGroup(cpGroup)

        //Continue Pages
        val contPages = EasySWT.addLabel(cpGroup, "Continue Braille Pages Across Volumes")
        contPages.toolTipText = "Sets whether braille page numbering resets at start of new volume."
        continuePagesCombo = makeYesNoCombo(cpGroup, pageSettingsDefault.isContinuePages)
        EasySWT.setGridData(continuePagesCombo)
        appendName(continuePagesCombo, "Sets whether braille page numbering resets at start of new volume.")

        //Guide words group
        val guideWordsGrp = Group(parent, 0)
        guideWordsGrp.layout = GridLayout(2, true)
        guideWordsGrp.text = "Guide Words"
        EasySWT.setGridDataGroup(guideWordsGrp)
        val guideWords = EasySWT.addLabel(guideWordsGrp, "Guide Words")
        guideWords.toolTipText = "Turns automatic Guide Words on/off for entire document."
        guideWordsCombo = makeYesNoCombo(guideWordsGrp, pageSettingsDefault.isGuideWords)
        EasySWT.setGridData(guideWordsCombo)
        appendName(guideWordsCombo, "Turns automatic Guide Words on/off for entire document.")
    }

    override fun validate(): String? {
        //No validation needed as there is only Combos with a fixed set of values
        return null
    }

    @Throws(IllegalArgumentException::class)
    override fun updateEngine(engine: UTDTranslationEngine): Boolean {
        val pageSettings = engine.pageSettings
        val pageNumberLocations: MutableList<PageNumberPosition> = ArrayList()
        pageNumberLocations.add(PageNumberPosition.valueOf(evenPrintNumCombo.text))
        pageNumberLocations.add(PageNumberPosition.valueOf(oddPrintNumCombo.text))
        pageNumberLocations.add(PageNumberPosition.valueOf(evenBrailleNumCombo.text))
        pageNumberLocations.add(PageNumberPosition.valueOf(oddBrailleNumCombo.text))
        return FormUIUtils.updateObject(
            pageSettings::pageNumberLocations,
            pageNumberLocations
        ) || FormUIUtils.updateObject(
            pageSettings::isPrintPageNumberRange,
            continueSymbolsCombo.text == "Yes"
        ) || FormUIUtils.updateObject(
            pageSettings::isContinuePages,
            continuePagesCombo.text == "Yes"
        ) || FormUIUtils.updateObject(
            pageSettings::isPrintPageLetterIndicator,
            continuationIndicatorCombo.text == "Yes"
        ) || FormUIUtils.updateObject(
            pageSettings::isGuideWords,
            guideWordsCombo.text == "Yes"
        )
    }

    companion object {
        private val localeHandler = getDefault()
        private fun makeNumberPositionCombo(parent: Composite, defaultValue: PageNumberPosition): Combo {
            val combo = Combo(parent, SWT.READ_ONLY)
            for (curLoc in PageNumberPosition.entries) {
                combo.add(curLoc.name)
            }
            combo.text = defaultValue.name
            EasySWT.setGridData(combo)
            return combo
        }

        private fun makeYesNoCombo(parent: Composite, defaultValue: Boolean): Combo {
            val combo = Combo(parent, SWT.READ_ONLY)
            combo.add("Yes")
            combo.add("No")
            if (defaultValue) {
                combo.text = "Yes"
            } else {
                combo.text = "No"
            }
            EasySWT.setGridData(combo)
            return combo
        }
    }
}
