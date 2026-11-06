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
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.ui.contractionRelaxer.UnitList
import org.brailleblaster.settings.TableExceptions.getCurrentExceptionFile
import org.brailleblaster.util.LINE_BREAK
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.utd.UTDTranslationEngine
import org.brailleblaster.utd.config.UTDConfig
import org.brailleblaster.utd.properties.BrailleTableType
import org.brailleblaster.libembosser.spi.BrlCell
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Paths


class TranslationSettingsTab internal constructor(folder: TabFolder?, private val manager: Manager) : SettingsUITab {
    private val bbdoc: BBDocument = manager.document
    private val config: Map<String, File>
    private val standardCombo: Combo
    private val brailleCellRadios: MutableMap<String, Button> = HashMap()
    private val mainTableText: Text
    private val compTableText: Text
    private val unconTableText: Text
    private val mathTextTableText: Text
    private val mathExprTableText: Text
    private val unitSelector: Combo
    private val viewContractionsButton: Button
    private var unitToggle: Int = 0
    private val initialUnitToggle: Int
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

        //Contraction Relaxer - only makes sense when the selected standard translates with the
        //uncontracted (grade 1) UEB table, so it's enabled/disabled alongside the standard combo.
        val relaxerGroup = Group(parent, SWT.NONE)
        relaxerGroup.layout = GridLayout(3, false)
        relaxerGroup.text = "Contraction Relaxer"
        EasySWT.setGridData(relaxerGroup)
        val relaxerLabel = EasySWT.addLabel(
            relaxerGroup,
            "Select a unit to allow specific contractions on top of uncontracted (grade 1) UEB."
        )
        relaxerLabel.layoutData = GridData(SWT.FILL, SWT.FILL, true, false, 3, 1)
        EasySWT.addLabel(relaxerGroup, "Relax Level")
        unitSelector = Combo(relaxerGroup, SWT.READ_ONLY)
        EasySWT.setGridData(unitSelector)
        for (unit in UnitList.allUnits) unitSelector.add(unit.unitName)
        viewContractionsButton = Button(relaxerGroup, SWT.PUSH)
        viewContractionsButton.text = "View Contractions"

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

        //Get the saved unit toggle. The relaxer only applies while the standard translates
        //using uncontracted (grade 1) UEB, so the saved level is trusted as-is here; it stays
        //disabled below whenever the selected standard isn't uncontracted-compatible.
        BBIni.propertyFileManager.getProperty(UNIT_TOGGLE_PROPERTY)?.let {
            unitToggle = it.toInt()
        }
        //Clamp in case an older saved value refers to an index that no longer exists in the list.
        unitToggle = unitToggle.coerceIn(0, UnitList.allUnits.lastIndex)
        initialUnitToggle = unitToggle
        unitSelector.select(unitToggle)

        //--Add listeners--
        standardCombo.addSelectionListener(EasySWT.makeSelectedListener { it: SelectionEvent ->
            updateUI(
                standardCombo.text
            )
        })
        unitSelector.addSelectionListener(EasySWT.makeSelectedListener { it: SelectionEvent ->
            unitToggle = unitSelector.selectionIndex
        })
        viewContractionsButton.addSelectionListener(EasySWT.makeSelectedListener { it: SelectionEvent ->
            viewContractions(parent.shell)
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

            //Contraction Relaxer only makes sense when the standard translates using the
            //uncontracted (grade 1) UEB table - disable it otherwise.
            val isUncontractedUeb = mainTableText.text == UNCONTRACTED_UEB_TABLE
            unitSelector.isEnabled = isUncontractedUeb
            viewContractionsButton.isEnabled = isUncontractedUeb
        } catch (ex: Exception) {
            throw RuntimeException("Failed when loading standard $standardName", ex)
        }
    }

    override fun validate(): String? {
        //Nothing really to validate
        return null
    }

    override fun updateEngine(engine: UTDTranslationEngine): Boolean {
        var changed = bbdoc.settingsManager.brailleStandard != selectedName
        if (unitSelector.isEnabled && unitToggle != initialUnitToggle) {
            writeContractionExceptions()
            BBIni.propertyFileManager.saveAsInt(UNIT_TOGGLE_PROPERTY, unitToggle)
            engine.brailleTranslator.close()
            changed = true
        }
        return changed
    }

    private fun writeContractionExceptions() {
        val file = getCurrentExceptionFile(manager)
            ?: throw RuntimeException("Unable to save exceptions to translation correction file - no file found.")

        val fileContent = file.readText(StandardCharsets.UTF_8)
        val headerIndex = fileContent.indexOf(RELAXER_HEADER)
        val footerIndex = fileContent.lastIndexOf(RELAXER_FOOTER)

        val contractionList = makeContractionList()

        if (headerIndex == -1 && footerIndex == -1) {
            //Header and footer not found - append new header and footer along with contraction list.
            OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8).use { writer ->
                writer.append(RELAXER_HEADER)
                writer.append(LINE_BREAK)
                writer.append(contractionList)
                writer.append(RELAXER_FOOTER)
            }
        } else if (headerIndex < footerIndex) {
            //Doc is structured correctly, so clear everything between the header and footer.
            val newContent =
                "${fileContent.take(headerIndex + RELAXER_HEADER.length)}$LINE_BREAK$contractionList${
                    fileContent.substring(
                        footerIndex
                    )
                }"

            OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8).use { writer ->
                writer.write(newContent)
            }
        } else {
            //Shouldn't happen, but if it does, just overwrite the file.
            OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8).use { writer ->
                writer.write("")
                writer.append(RELAXER_HEADER)
                writer.append(LINE_BREAK)
                writer.append(contractionList)
                writer.append(RELAXER_FOOTER)
            }
        }
    }

    private fun makeContractionList(): String {
        return when (unitToggle) {
            0 -> ""
            else -> {
                //Start at 1, since the 0th unit has no contraction list. Includes everything up
                //to and including the selected unit (there's no "fully contracted" sentinel here -
                //switching to full grade 2 is done via the standard combo itself).
                UnitList.allUnits.drop(1).take(unitToggle)
                    .flatMap { unit -> unit.contractions.map { "$it${System.lineSeparator()}" } }
                    .joinToString(separator = "")
            }
        }
    }

    private fun makePrettyContractionList(): String {
        //This is used for the preview window.
        val f = StringBuilder()
        for (i in 0..unitToggle) {
            val unit = UnitList.allPrettyUnits[i]
            for (c in unit) {
                f.append(c)
                f.append(System.lineSeparator())
            }
        }
        return f.toString()
    }

    private fun viewContractions(parentShell: Shell) {
        //Make a legible list of contractions for each unit. The liblouis tables are not exactly human-readable.
        val contractionList = makePrettyContractionList()

        val vc = Shell(parentShell, SWT.APPLICATION_MODAL or SWT.DIALOG_TRIM)
        vc.text = "Contraction List"
        vc.layout = GridLayout(1, false)

        val comp = Composite(vc, SWT.NONE)
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

    companion object {
        private val localeHandler = getDefault()
        private val log = LoggerFactory.getLogger(TranslationSettingsTab::class.java)
        private const val FILE_SUFFIX = ".brailleSettings.xml"
        const val SWTBOT_STANDARD_COMBO = "translationSettingsTab.standard"
        private const val UNCONTRACTED_UEB_TABLE = "en-ueb-g1.ctb"
        private const val UNIT_TOGGLE_PROPERTY = "ContractionRelaxer.unitToggle"
        private const val RELAXER_HEADER = "#Begin UEB Contraction Relaxer"
        private const val RELAXER_FOOTER = "#End UEB Contraction Relaxer"

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