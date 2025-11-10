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

import org.brailleblaster.BBIni
import org.brailleblaster.document.BBDocument
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.utd.UTDTranslationEngine
import org.brailleblaster.utd.config.UTDConfig
import org.brailleblaster.utd.properties.BrailleTableType
import org.brailleblaster.libembosser.spi.BrlCell
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FilenameFilter
import java.nio.file.Paths

class TranslationSettingsTab internal constructor(folder: TabFolder?, private val bbdoc: BBDocument) : SettingsUITab {
    private val config: Map<String, File>
    private val standardCombo: Combo
    private val brailleCellRadios: MutableMap<String, Button> = HashMap()
    private val mainTableText: Text
    private val compTableText: Text
    private val unconTableText: Text
    private val mathTextTableText: Text
    private val mathExprTableText: Text
    lateinit var selectedName: String
        private set

    init {
        val tab = TabItem(folder, SWT.NONE)
        tab.text = localeHandler["translationSettings"]
        val parent = Composite(folder, 0)
        parent.layout = GridLayout(1, true)
        tab.control = parent

        //---Add widgets---
        //Standards dropdown
        val standardsGroup = Group(parent, SWT.NONE)
        standardsGroup.layout = GridLayout(2, true)
        standardsGroup.text = "Standards"
        EasySWT.setGridData(standardsGroup)
        EasySWT.addLabel(standardsGroup, "Braille Standard")
        standardCombo = Combo(standardsGroup, SWT.READ_ONLY)
        EasySWT.setGridData(standardCombo)
        EasySWT.addSwtBotKey(standardCombo, SWTBOT_STANDARD_COMBO)

        //Specific settings
        val settingsGroup = Group(parent, SWT.NONE)
        settingsGroup.layout = GridLayout(2, false)
        settingsGroup.text = "Selected Standard Settings"
        EasySWT.setGridData(settingsGroup)
        EasySWT.addLabel(settingsGroup, "Cell type")
        val cellTypeContainer = Composite(settingsGroup, SWT.NONE)
        cellTypeContainer.layout = RowLayout()
        for (curType in BrlCell.entries) {
            //TODO: Make these settings actually do something
            val typeButton = Button(cellTypeContainer, SWT.RADIO)
            typeButton.isEnabled = false
            typeButton.text = curType.name
            //log.debug("Found " + curType.name)
            brailleCellRadios[curType.name] = typeButton
        }

        EasySWT.addLabel(settingsGroup, "Main Translation Table")
        mainTableText = Text(settingsGroup, SWT.BORDER)
        mainTableText.isEnabled = false
        EasySWT.setGridData(mainTableText)
        EasySWT.addLabel(settingsGroup, "Computer Braille Table")
        compTableText = Text(settingsGroup, SWT.BORDER)
        compTableText.isEnabled = false
        EasySWT.setGridData(compTableText)
        EasySWT.addLabel(settingsGroup, "Uncontracted Table")
        unconTableText = Text(settingsGroup, SWT.BORDER)
        unconTableText.isEnabled = false
        EasySWT.setGridData(unconTableText)
        EasySWT.addLabel(settingsGroup, "Math Text Table")
        mathTextTableText = Text(settingsGroup, SWT.BORDER)
        mathTextTableText.isEnabled = false
        EasySWT.setGridData(mathTextTableText)
        EasySWT.addLabel(settingsGroup, "Math Expression Table")
        mathExprTableText = Text(settingsGroup, SWT.BORDER)
        mathExprTableText.isEnabled = false
        EasySWT.setGridData(mathExprTableText)

        //--Add listeners--
        standardCombo.addSelectionListener(EasySWT.makeSelectedListener { it: SelectionEvent ->
            updateUI(
                standardCombo.text
            )
        })

        //---Set data---
        config = loadAllStandardFiles()
        for (curKey in config.keys) standardCombo.add(curKey)

        //Set default
        val stdBaseName = bbdoc.settingsManager.brailleStandard
        val currentStd = config.filterValues { it.name.removeSuffix(FILE_SUFFIX) == stdBaseName }.keys.first()
        standardCombo.select(standardCombo.indexOf(currentStd))
        updateUI(currentStd)
    }

    private fun updateUI(standardName: String) {
        try {
            val selectedConfig = config[standardName]!!
            val selectedSettings = UTDConfig.loadBrailleSettings(selectedConfig)
            selectedName = selectedConfig.name.removeSuffix(FILE_SUFFIX)

//			//SettingsManager adds the exceptions table when loaded
//			mainTableText.setText(selectedSettings.getMainTranslationTable()
//					+ "," + standardName + TableExceptions.EXCEPTIONS_TABLE_EXTENSION);
//			compTableText.setText(selectedSettings.getComputerBrailleTable());
//			unconTableText.setText(selectedSettings.getUncontractedTable());
//			mathTextTableText.setText(selectedSettings.getMathTextTable());
//			String mathTablesText = selectedSettings.getMathExpressionTable();
//			if (TableExceptions.MATH_EXCEPTION_TABLES){
//				mathTablesText +=  "," + MathTranslationSettings.getMathTable(selectedSettings) + TableExceptions.EXCEPTIONS_TABLE_EXTENSION;
//			}
//			mathExprTableText.setText(mathTablesText);
            mainTableText.text = BrailleTableType.LITERARY.getTableName(selectedSettings)
            compTableText.text = BrailleTableType.COMPUTER_BRAILLE.getTableName(selectedSettings)
            unconTableText.text = BrailleTableType.UNCONTRACTED.getTableName(selectedSettings)
            mathTextTableText.text = BrailleTableType.MATH_TEXT.getTableName(selectedSettings)
            mathExprTableText.text = BrailleTableType.MATH.getTableName(selectedSettings)
            brailleCellRadios[selectedSettings.cellType.name]!!.selection = true
        } catch (ex: Exception) {
            throw RuntimeException("Failed when loading standard $standardName", ex)
        }
    }

    override fun validate(): String? {
        //Nothing really to validate
        return null
    }

    override fun updateEngine(engine: UTDTranslationEngine): Boolean {
        return bbdoc.settingsManager.brailleStandard != selectedName
    }

    companion object {
        private val localeHandler = getDefault()
        private val log = LoggerFactory.getLogger(TranslationSettingsTab::class.java)
        private const val FILE_SUFFIX = ".brailleSettings.xml"
        const val SWTBOT_STANDARD_COMBO = "translationSettingsTab.standard"

        // settings constants for testing 
        const val UEB_PLUS_NEMETH = "UEB-PLUS-NEMETH"
        const val UEB = "UEB"
        private fun loadAllStandardFiles(): Map<String, File> {
            //Get all the defined config files
            val fileFilter = FilenameFilter { dir: File?, name: String -> name.endsWith(FILE_SUFFIX) }
            val defaultFiles = BBIni.programDataPath.resolve("utd").toFile().listFiles(fileFilter)?.map {
                (it.name.removeSuffix(FILE_SUFFIX)) to it
            } ?: emptyList()
            val userFiles = BBIni.userProgramDataPath.resolve("utd").toFile().listFiles(fileFilter)?.map {
                (it.name.removeSuffix(FILE_SUFFIX)) to it
            } ?: emptyList()
            val legacyFiles =
                BBIni.programDataPath.resolve(Paths.get("utd", "legacy")).toFile().listFiles(fileFilter)?.map {
                    ("${it.name.removeSuffix(FILE_SUFFIX)} (Legacy)") to it
                } ?: emptyList()
            val filesToInsert = defaultFiles + userFiles + legacyFiles

            //Add to clean map (user's configs named the same will overwrite the key of the built in config)
            val results = linkedMapOf(*filesToInsert.toTypedArray())
            return results
        }
    }
}