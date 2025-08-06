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
import org.brailleblaster.utd.UTDTranslationEngine
import org.brailleblaster.utd.utils.Page
import org.brailleblaster.utils.swt.AccessibilityUtils.appendName
import org.brailleblaster.util.FormUIUtils.addDoubleFilter
import org.brailleblaster.util.FormUIUtils.addIntegerFilter
import org.brailleblaster.util.FormUIUtils.addLabel
import org.brailleblaster.util.FormUIUtils.makeSelectedListener
import org.brailleblaster.util.FormUIUtils.setGridData
import org.brailleblaster.util.FormUIUtils.setGridDataGroup
import org.brailleblaster.util.FormUIUtils.updateObject
import org.brailleblaster.utils.LengthUtils
import org.brailleblaster.utils.UnitConverter
import org.eclipse.swt.SWT
import org.eclipse.swt.events.*
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.function.Consumer
import java.util.function.DoubleUnaryOperator
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.min

class PagePropertiesTab private constructor(parent: Composite, engine: UTDTranslationEngine, shell: Shell) :
    SettingsUITab {
    private val unitConverter = UnitConverter()
    private val standardPages: List<Page>
    private val widthBox: Text
    private val heightBox: Text
    private val linesBox: Text
    private val cellsBox: Text
    private val marginTopBox: Text
    private val marginLeftBox: Text
    private val marginRightBox: Text
    private val marginBottomBox: Text
    private val marginTopLabel: Label
    private val marginBottomLabel: Label
    private val marginLeftLabel: Label
    private val marginRightLabel: Label
    private val pageTypes: Combo
    private val interpointCombo: Combo
    private val regionalButton: Button
    private val cellsLinesButton: Button
    private val unitName: String
    private val units: LengthUtils.Units
    private var marginLocalUnit = true


    /**
     * Internal representation always in mm, only converted for display/input.
     */
    private var pageHeight = 0.0
    private var pageWidth = 0.0
    private var cellsWidth: Double
    private var linesHeight: Double
    private var marginTop = 0.0
    private var marginBottom = 0.0
    private var marginLeft = 0.0
    private var marginRight = 0.0

    // ---Init---
    private val brailleCell = engine.brailleSettings.cellType

    init {
        val metric = unitConverter.isMetric
        unitName = if (metric) "mm" else "in"
        val unitSuffix = " ($unitName)"
        units = if (metric) LengthUtils.Units.MILLIMETRES else LengthUtils.Units.INCHES
        standardPages = Page.STANDARD_PAGES
        // Group page size
        val pageGroup = Group(parent, 0)
        pageGroup.text = localeHandler["pageSize"]
        pageGroup.layout = GridLayout(2, true)
        setGridDataGroup(pageGroup)

        addLabel(pageGroup, localeHandler["pageSize"])
        pageTypes = Combo(pageGroup, SWT.READ_ONLY)
        setGridData(pageTypes)

        addLabel(pageGroup, localeHandler["width"] + unitSuffix)
        widthBox = Text(pageGroup, SWT.BORDER)
        addDoubleFilter(widthBox)
        setGridData(widthBox)

        addLabel(pageGroup, localeHandler["height"] + unitSuffix)
        heightBox = Text(pageGroup, SWT.BORDER)
        addDoubleFilter(heightBox)
        setGridData(heightBox)

        addLabel(pageGroup, localeHandler["linesPerPage"])
        linesBox = Text(pageGroup, SWT.BORDER)
        setGridData(linesBox)
        addIntegerFilter(linesBox)

        addLabel(pageGroup, localeHandler["cellsPerLine"])
        cellsBox = Text(pageGroup, SWT.BORDER)
        setGridData(cellsBox)
        addIntegerFilter(cellsBox)

        // Margin group
        val marginGroup = Group(parent, 0)
        marginGroup.layout = GridLayout(2, true)
        marginGroup.text = localeHandler["margins"]
        setGridDataGroup(marginGroup)

        // Units subgroup
        addLabel(marginGroup, localeHandler["measurementUnits"])
        val unitsGroup = Composite(marginGroup, 0)
        unitsGroup.layout = GridLayout(2, true)
        regionalButton = Button(unitsGroup, SWT.RADIO)
        regionalButton.text = unitName
        regionalButton.selection = true
        setGridData(regionalButton)
        cellsLinesButton = Button(unitsGroup, SWT.RADIO)
        cellsLinesButton.text = localeHandler["cellsLines"]
        setGridData(cellsLinesButton)

        // All other margins
        marginTopLabel = addLabel(marginGroup, localeHandler["topMargin"] + unitSuffix)
        marginTopBox = Text(marginGroup, SWT.BORDER)
        addDoubleFilter(marginTopBox)
        setGridData(marginTopBox)

        marginBottomLabel = addLabel(marginGroup, localeHandler["bottomMargin"] + unitSuffix)
        marginBottomBox = Text(marginGroup, SWT.BORDER or SWT.READ_ONLY)
        addDoubleFilter(marginBottomBox)
        setGridData(marginBottomBox)

        marginLeftLabel = addLabel(marginGroup, localeHandler["leftMargin"] + unitSuffix)
        marginLeftBox = Text(marginGroup, SWT.BORDER)
        addDoubleFilter(marginLeftBox)
        setGridData(marginLeftBox)

        marginRightLabel = addLabel(marginGroup, localeHandler["rightMargin"] + unitSuffix)
        marginRightBox = Text(marginGroup, SWT.BORDER or SWT.READ_ONLY)
        addDoubleFilter(marginRightBox)
        setGridData(marginRightBox)

        // Group for interpoint
        val interpointGroup = Group(parent, 0)
        interpointGroup.layout = GridLayout(2, true)
        interpointGroup.text = "Interpoint"
        setGridDataGroup(interpointGroup)

        val interpointLabel = addLabel(interpointGroup, "Interpoint")
        interpointLabel.toolTipText =
            "Sets embosser for interpoint embossing and puts t1, p1, and 1 on a right-hand page."
        interpointCombo = makeYesNoCombo(interpointGroup, engine.pageSettings.interpoint)
        appendName(interpointCombo, "Ensures t1, p1, and 1 appear on right-hand page.")

        // ----Add listeners----
        // When the user selects a page from the drop down, fill out the width,
        // height, cells, and lines boxes
        pageTypes.addSelectionListener(makeSelectedListener { onStandardPageSelected() })

        // Size fields
        // When a user types a digit, adjust cells, lines and page combo
        widthBox.addKeyListener(
            makeFieldListener(
                { widthBox.text },
                { v ->
                    pageWidth = unitConverter.localUnitsToMM(v)
                    v
                },
                { calculateCellsLinesAndUpdate() })
        )
        heightBox.addKeyListener(
            makeFieldListener(
                { heightBox.text },
                { v ->
                    pageHeight = unitConverter.localUnitsToMM(v)
                    v
                },
                { calculateCellsLinesAndUpdate() })
        )

        // Cell fields
        // BELOW LOGIC IS COPIED TO configureForBenchmarks METHOD. IF CHANGING
        // MAKE SURE THAT METHOD IS UPDATED!!!
        cellsBox.addKeyListener(
            makeFieldListener(
                { cellsBox.text },
                { v ->
                    cellsWidth = brailleCell.getWidthForCells(v.toInt()).toDouble()
                    v
                },
                { calculateTopAndLeftMarginsAndUpdate() })
        )
        linesBox.addKeyListener(
            makeFieldListener(
                { linesBox.text },
                { v ->
                    linesHeight = brailleCell.getHeightForLines(v.toInt()).toDouble()
                    v
                },
                { calculateTopAndLeftMarginsAndUpdate() })
        )

        // Update cells/line and lines/page when margins change
        val marginHeight = DoubleUnaryOperator { v: Double ->
            if (marginLocalUnit)
                unitConverter.localUnitsToMM(v)
            else
                brailleCell.getHeightForLines(v.toInt()).toDouble()
        }
        val marginWidth = DoubleUnaryOperator { v: Double ->
            if (marginLocalUnit)
                unitConverter.localUnitsToMM(v)
            else
                brailleCell.getWidthForCells(v.toInt()).toDouble()
        }

        // 2020/01/28: No longer need the update for changes to bottom and right margins as these are now readonly and not changed by the user.
        marginTopBox.addFocusListener(
            makeFieldFocusListener(
                { marginTopBox.text },
                marginHeight.andThen { v ->
                    marginTop = v
                    v
                },
                { calculateCellsLinesAndUpdate() })
        )

        marginLeftBox.addFocusListener(
            makeFieldFocusListener(
                { marginLeftBox.text },
                marginWidth.andThen { v ->
                    marginLeft = v
                    v
                },
                { calculateCellsLinesAndUpdate() })
        )

        // marginRightBox.addFocusListener(makeFieldFocusListener(() -> marginRightBox.getText(),
        // marginWidth.andThen((v) -> marginRight = v), (e) -> calculateCellsLinesAndUpdate()));

        // Margin unit suffixes
        val marginUnitChangedListener = makeSelectedListener { _: SelectionEvent? -> onMarginUnitSelected() }
        regionalButton.addSelectionListener(marginUnitChangedListener)
        cellsLinesButton.addSelectionListener(marginUnitChangedListener)

        // ---Set field default values---
        // Pages drop down box
        for (curPage in standardPages) pageTypes.add(curPage.toString(units, NUMBER_FORMATTER))

        // Page size
        val pageSettings = engine.pageSettings
        if (pageSettings.paperHeight != 0.0 && pageSettings.paperWidth != 0.0) {
            // User has set their own settings stored in their own units
            pageHeight = pageSettings.paperHeight
            pageWidth = pageSettings.paperWidth
            marginTop = pageSettings.topMargin
            marginBottom = pageSettings.bottomMargin
            marginLeft = pageSettings.leftMargin
            marginRight = pageSettings.rightMargin
        } else {
            // TODO: assume the first one?
            // Page handles unit conversions
            val defaultPage = standardPages[0]
            pageHeight = defaultPage.getHeight(LengthUtils.Units.MILLIMETRES)
            pageWidth = defaultPage.getWidth(LengthUtils.Units.MILLIMETRES)
            marginTop = defaultPage.getTopMargin(LengthUtils.Units.MILLIMETRES)
            marginBottom = defaultPage.getBottomMargin(LengthUtils.Units.MILLIMETRES)
            marginLeft = defaultPage.getLeftMargin(LengthUtils.Units.MILLIMETRES)
            marginRight = defaultPage.getRightMargin(LengthUtils.Units.MILLIMETRES)
        }

        cellsWidth = max(pageWidth - marginLeft - marginRight, 0.0)
        linesHeight = max(pageHeight - marginTop - marginBottom, 0.0)

        calculateCellsLinesAndUpdate()
    }

    /**
     * Update UI with internally stored values
     */
    private fun updateFields() {
        val microWidth = LengthUtils.Units.MILLIMETRES.fromUnits(pageWidth)
        val microHeight = LengthUtils.Units.MILLIMETRES.fromUnits(pageHeight)
        // Compare width and height in micrometres as int
        // It is not good to compare == for double values.
        val matchedPage = standardPages.stream()
            .filter { p: Page -> p.height == microHeight && p.width == microWidth }.findFirst()
        if (matchedPage.isPresent) pageTypes.select(
            pageTypes.indexOf(
                matchedPage.get().toString(units, NUMBER_FORMATTER)
            )
        )
        else if (pageTypes.itemCount == standardPages.size) {
            pageTypes.add(localeHandler["custom"])
            pageTypes.select(pageTypes.itemCount - 1)
        } else pageTypes.select(pageTypes.itemCount - 1)

        setTextIfDifferent(widthBox, unitConverter.mmToLocalUnits(pageWidth))
        setTextIfDifferent(heightBox, unitConverter.mmToLocalUnits(pageHeight))

        setTextIfDifferent(linesBox, brailleCell.getLinesForHeight(BigDecimal.valueOf(linesHeight)).toDouble())
        setTextIfDifferent(cellsBox, brailleCell.getCellsForWidth(BigDecimal.valueOf(cellsWidth)).toDouble())

        setTextIfDifferent(
            marginTopBox, if (marginLocalUnit)
                unitConverter.mmToLocalUnits(marginTop)
            else
                brailleCell.getLinesForHeight(BigDecimal.valueOf(marginTop)).toDouble()
        )
        setTextIfDifferent(
            marginBottomBox, if (marginLocalUnit)
                unitConverter.mmToLocalUnits(marginBottom)
            else
                brailleCell.getLinesForHeight(BigDecimal.valueOf(marginBottom)).toDouble()
        )
        setTextIfDifferent(
            marginLeftBox, if (marginLocalUnit)
                unitConverter.mmToLocalUnits(marginLeft)
            else
                brailleCell.getCellsForWidth(BigDecimal.valueOf(marginLeft)).toDouble()
        )
        setTextIfDifferent(
            marginRightBox, if (marginLocalUnit)
                unitConverter.mmToLocalUnits(marginRight)
            else
                brailleCell.getCellsForWidth(BigDecimal.valueOf(marginRight)).toDouble()
        )
    }

    /**
     * Update UI field if needed
     */
    private fun setTextIfDifferent(box: Text, number: Double) {
        val given = box.text
        val expected = NUMBER_FORMATTER.format(number)
        // Reset if number is different
        if (!(given.isNotEmpty() && parseDouble(given) == number)) {
            box.text = expected
        }
    }

    private fun getWholeCellsWidth(width: Double): Double {
        val cpl = brailleCell.getCellsForWidth(BigDecimal.valueOf(width))
        return brailleCell.getWidthForCells(cpl).toDouble()
    }

    private fun getWholeLinesHeight(height: Double): Double {
        val lpp = brailleCell.getLinesForHeight(BigDecimal.valueOf(height))
        return brailleCell.getHeightForLines(lpp).toDouble()
    }

    private fun calculateTopAndLeftMarginsAndUpdate() {
        // Protect against too many cells and lines.
        if (cellsWidth > pageWidth) {
            cellsWidth = getWholeCellsWidth(pageWidth)
        }
        if (linesHeight > pageHeight) {
            linesHeight = getWholeLinesHeight(pageHeight)
        }
        // Reduce the margins if required
        if (pageWidth - marginLeft - cellsWidth < 0) {
            marginLeft = pageWidth - cellsWidth
        }
        if (pageHeight - marginTop - linesHeight < 0) {
            marginTop = pageHeight - linesHeight
        }
        updatePageValues()
    }

    /**
     * Recalculate page cells and page lines Validates negative values ​​and zero
     */
    private fun calculateCellsLinesAndUpdate() {
        // 2020/01/28: Adjust right margin first as this is now readonly to the user.
        // 2020/01/28: Ensure left margin is not wider than the page.
        marginLeft = min(marginLeft, pageWidth)
        if ((pageWidth - marginLeft - cellsWidth) < 0) {
            cellsWidth = pageWidth - marginLeft
        }


        // 2020/01/28: Ensure top margin is not larger than page.
        marginTop = min(marginTop, pageHeight)
        if ((pageHeight - marginTop - linesHeight) < 0) {
            linesHeight = pageHeight - marginTop
        }
        updatePageValues()
    }

    private fun updatePageValues() {
        // 2020/01/28: cellsWidth should always relate to a width of a whole number of cells.
        cellsWidth = getWholeCellsWidth(cellsWidth)
        marginRight = pageWidth - marginLeft - cellsWidth
        // 2020/01/28: linesHeight should always relate to a whole number of lines.
        linesHeight = getWholeLinesHeight(linesHeight)
        marginBottom = pageHeight - marginTop - linesHeight
        updateFields()
    }

    // for recalculate cells after go to negative (when the user change margin)
    //	private void handleWidthError() {
    //		 MWhapples: To tamper with user input and pop up a dialog is confusing
    //		 to the user
    //		 if(marginLeftBox.isFocusControl()) marginLeft = 0;
    //		 if(marginRightBox.isFocusControl()) marginRight = 0;
    //		 cellsWidth = pageWidth - marginLeft - marginRight;
    //		 setTextIfDifferent(cellsBox, brailleCell.cellsFromWidth(cellsWidth));
    //		 MessageBox msg = new MessageBox(parentShell);
    //		 msg.setMessage("Incorrect margin. Please enter another number." );
    //		 msg.open();
    //	}
    // for recalculate lines after go to negative (when the user change margin)
    //	private void handleHeightError() {
    //		 MWhapples: To tamper with user input and pop up a dialog is confusing
    //		 to the user
    //		 if(marginBottomBox.isFocusControl()){ marginBottom = 0; }
    //		 if(marginTopBox.isFocusControl()) marginTop = 0;
    //		 linesHeight = pageHeight - marginTop - marginBottom;
    //		 setTextIfDifferent(linesBox,
    //		 brailleCell.linesFromHeight(linesHeight));
    //		 MessageBox msg = new MessageBox(parentShell);
    //		 msg.setMessage("Incorrect margin. Please enter another number." );
    //		 msg.open();
    //	}
    /**
     * When the user selects a standard page size, update width, height, cells,
     * lines fields
     */
    private fun onStandardPageSelected() {
        val p = standardPages[pageTypes.selectionIndex]
        pageHeight = p.getHeight(LengthUtils.Units.MILLIMETRES)
        pageWidth = p.getWidth(LengthUtils.Units.MILLIMETRES)
        marginLeft = p.getLeftMargin(LengthUtils.Units.MILLIMETRES)
        marginRight = p.getRightMargin(LengthUtils.Units.MILLIMETRES)
        marginTop = p.getTopMargin(LengthUtils.Units.MILLIMETRES)
        marginBottom = p.getBottomMargin(LengthUtils.Units.MILLIMETRES)
        cellsWidth = max(pageWidth - marginLeft - marginRight, 0.0)
        linesHeight = max(pageHeight - marginTop - marginBottom, 0.0)
        calculateCellsLinesAndUpdate()

        if (pageTypes.getItem(pageTypes.itemCount - 1) == localeHandler["custom"]) pageTypes.remove(pageTypes.itemCount - 1)
    }

    /**
     * When the user switches between displaying margins as inch/mm or cells, update
     * label suffix and convert fields to new unit
     */
    private fun onMarginUnitSelected() {
        if (isAnyFieldEmpty || marginLocalUnit == regionalButton.selection) return
        if (regionalButton.selection) {
            marginLocalUnit = true
            marginTopLabel.text = replaceOldUnitLabel(marginTopLabel, unitName)
            marginBottomLabel.text = replaceOldUnitLabel(marginBottomLabel, unitName)
            marginLeftLabel.text = replaceOldUnitLabel(marginLeftLabel, unitName)
            marginRightLabel.text = replaceOldUnitLabel(marginRightLabel, unitName)
            regionalButton.selection = true
            cellsLinesButton.selection = false
        } else {
            marginLocalUnit = false
            marginTopLabel.text = replaceOldUnitLabel(marginTopLabel, "lines")
            marginBottomLabel.text = replaceOldUnitLabel(marginBottomLabel, "lines")
            marginLeftLabel.text = replaceOldUnitLabel(marginLeftLabel, "cells")
            marginRightLabel.text = replaceOldUnitLabel(marginRightLabel, "cells")
            regionalButton.selection = false
            cellsLinesButton.selection = true
        }
        updateFields()
    }

    private fun replaceOldUnitLabel(label: Label, newUnit: String): String {
        // Replace the last word with the new unit
        var text = label.text
        text = text.substringBeforeLast(" ")
        return "$text ($newUnit)"
    }

    private val isAnyFieldEmpty: Boolean
        /**
         * If this is false the user cleared out a field
         */
        get() = widthBox.text.isEmpty() || heightBox.text.isEmpty() || linesBox.text.isEmpty()
                || cellsBox.text.isEmpty() || marginTopBox.text.isEmpty() || marginLeftBox.text
            .isEmpty()
                || marginRightBox.text.isEmpty() || marginBottomBox.text.isEmpty()

    override fun validate(): String? {
        if (marginRight + marginLeft + cellsWidth > pageWidth) return "incorrectMarginWidth"
        if (marginTop + marginBottom + linesHeight > pageHeight) return "incorrectMarginHeight"
        if (pageHeight <= 0 || pageWidth <= 0 || linesHeight <= 0 || cellsWidth <= 0 || marginTop < 0 || marginBottom < 0 || marginLeft < 0 || marginRight < 0) return "settingsBelowZero"
        return null
    }

    override fun updateEngine(engine: UTDTranslationEngine): Boolean {
        val pageSettings = engine.pageSettings
        val brailleSettings = engine.brailleSettings

        var updated =
            updateObject(brailleSettings::cellType::get, brailleSettings::cellType::set, brailleCell, false)
        updated =
            updateObject(pageSettings::paperHeight::get, pageSettings::paperHeight::set, pageHeight, updated)
        updated =
            updateObject(pageSettings::paperWidth::get, pageSettings::paperWidth::set, pageWidth, updated)
        updated =
            updateObject(pageSettings::topMargin::get, pageSettings::topMargin::set, marginTop, updated)
        updated =
            updateObject(pageSettings::bottomMargin::get, pageSettings::bottomMargin::set, marginBottom, updated)
        updated =
            updateObject(pageSettings::leftMargin::get, pageSettings::leftMargin::set, marginLeft, updated)
        updated =
            updateObject(pageSettings::rightMargin::get, pageSettings::rightMargin::set, marginRight, updated)
        updated =
            updateObject(pageSettings::interpoint::get, pageSettings::interpoint::set, interpointCombo.text == "Yes", updated)

        return updated
    }

    private fun makeFieldListener(
        getRawValue: Supplier<String>, setParsedValue: DoubleUnaryOperator,
        function: Consumer<KeyEvent>
    ): KeyListener {
        return object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                if (isAnyFieldEmpty || !e.doit) return
                val rawValue = getRawValue.get()
                if (rawValue == "." || rawValue == "-") return
                try {
                    val value = rawValue.toDouble()
                    setParsedValue.applyAsDouble(value)
                    function.accept(e)
                } catch (_: NumberFormatException) {
                    val msg = MessageBox(Display.getCurrent().activeShell)
                    msg.message = "Incorrect number format. Please enter another number."
                    msg.open()
                }
            }
        }
    }

    private fun makeFieldFocusListener(
        getRawValue: Supplier<String>, setParsedValue: DoubleUnaryOperator,
        function: Consumer<FocusEvent>
    ): FocusListener {
        return object : FocusListener {
            override fun focusLost(e: FocusEvent) {
                // TODO Auto-generated method stub
                if (isAnyFieldEmpty) function.accept(e)
                val rawValue = getRawValue.get()
                if (rawValue == "." || rawValue == "-") return
                try {
                    val value = rawValue.toDouble()
                    setParsedValue.applyAsDouble(value)
                    function.accept(e)
                } catch (_: NumberFormatException) {
                    val msg = MessageBox(Display.getCurrent().activeShell)
                    msg.message = "Incorrect number format. Please enter another number."
                    msg.open()
                }
            }

            override fun focusGained(e: FocusEvent) {
                // TODO Auto-generated method stub
            }
        }
    }

    companion object {
        private val localeHandler = getDefault()
        private val NUMBER_FORMATTER = DecimalFormat("###.##")
        fun create(folder: TabFolder, engine: UTDTranslationEngine, shell: Shell): PagePropertiesTab {
            // ---Add widgets to tab---
            val tab = TabItem(folder, 0)
            tab.text = localeHandler["pageProperties"]

            val parent = Composite(folder, 0)
            parent.layout = GridLayout(1, true)
            tab.control = parent

            return PagePropertiesTab(parent, engine, shell)
        }

        private fun parseDouble(number: String): Double {
            try {
                return NUMBER_FORMATTER.parse(number).toDouble()
            } catch (e: Exception) {
                throw RuntimeException("Failed to parse '$number'", e)
            }
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

            setGridData(combo)
            return combo
        }
    }
}
