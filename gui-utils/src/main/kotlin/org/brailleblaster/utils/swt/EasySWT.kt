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
package org.brailleblaster.utils.swt

import org.apache.commons.lang3.time.DurationFormatUtils
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.CCombo
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.events.*
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer

/**
 * A collection of helper methods to reduce the verbosity of creating SWT shells. See TPagesDialog
 * for examples on how to use these methods. Note that, for simplicity's sake, only GridLayouts are
 * supported.
 */
object EasySWT {
    private val localeHandler = getDefault()
    @JvmField
    val OK_LABEL: String = localeHandler["lblOk"]
    const val TEXT_PADDING: Int = 20
    const val SWTBOT_WIDGET_KEY: String = "org.eclipse.swtbot.widget.key"

    @JvmStatic
    fun buildGridData(): GridDataBuilder {
        return GridDataBuilder()
    }

    fun makeDialog(parent: Shell?): Shell {
        return Shell(parent, SWT.APPLICATION_MODAL or SWT.DIALOG_TRIM or SWT.RESIZE)
    }

    fun makeDialogFloating(parent: Shell?): Shell {
        return Shell(parent, SWT.DIALOG_TRIM or SWT.RESIZE)
    }

    @JvmStatic
    fun makeComposite(parent: Composite?, columns: Int): Composite {
        return CompositeBuilder1(parent).apply {
            this.columns = columns
        }.build()
    }

    @JvmStatic
    fun buildComposite(parent: Composite?): CompositeBuilder1 {
        return CompositeBuilder1(parent)
    }

    @JvmStatic
    fun buildPushButton(parent: Composite?): ButtonBuilder1 {
        return ButtonBuilder1(parent)
    }

    @JvmStatic
    fun makePushButton(
        parent: Composite?, text: String?, columns: Int, onClick: Consumer<SelectionEvent>?
    ): Button {
        return ButtonBuilder1(text, columns, onClick).apply {
            this.parent = parent
        }.build()
    }

    @JvmStatic
    fun makePushButton(
        parent: Composite?, text: String?, width: Int, columns: Int, onClick: Consumer<SelectionEvent>?
    ): Button {
        return ButtonBuilder1(text, columns, onClick).apply {
            this.width = width
            this.parent = parent
        }.build()
    }

    @JvmStatic
    fun makeRadioButton(
        parent: Composite?, text: String?, columns: Int, onClick: Consumer<SelectionEvent>?
    ): Button {
        return ButtonBuilder1(text, columns, onClick).apply {
            this.parent = parent
            swtOptions = SWT.RADIO
        }.build()
    }

    @JvmStatic
    fun makeCheckBox(
        parent: Composite?, text: String?, onClick: Consumer<SelectionEvent>?
    ): Button {
        return ButtonBuilder1(parent).apply {
            this.text = text
            swtOptions = SWT.CHECK
            this.onClick = onClick
        }.build()
    }

    @JvmStatic
    fun makeLabel(parent: Composite?, text: String, columns: Int): Label {
        return LabelBuilder1(text, columns).apply {
            this.parent = parent
        }.build()
    }

    @JvmStatic
    fun makeLabel(parent: Composite?, text: String, width: Int, columns: Int): Label {
        return LabelBuilder1(text, columns).apply {
            this.parent = parent
            this.width = width
        }.build()
    }

    @JvmStatic
    fun makeText(parent: Composite?, columns: Int): Text {
        return TextBuilder1(columns).apply{
            this.parent = parent
        }.build()
    }

    @JvmStatic
    fun makeText(parent: Composite?, width: Int, columns: Int): Text {
        return TextBuilder1(columns).apply {
            this.parent = parent
            this.width = width
        }.build()
    }

    fun makeText(
        parent: Composite?, width: Int?, columns: Int, onEnter: Consumer<KeyEvent>?
    ): Text {
        return TextBuilder1(columns).apply {
            this.parent = parent
            this.width = width
            this.onEnter = onEnter
        }.build()
    }

    @JvmStatic
    fun getWidthOfText(text: String?): Int {
        val gc = GC(Display.getCurrent())
        val width = gc.textExtent(text).x
        gc.dispose()
        return width
    }

    const val NOPARENTSET: Int = 1
    const val NOTEXTSET: Int = 2
    const val NOMENU: Int = 3

    @JvmStatic
    fun error(code: Int) {
        when (code) {
            NOPARENTSET -> throw IllegalArgumentException("No parent was set")
            NOTEXTSET -> throw IllegalArgumentException("No text set")
            NOMENU -> throw IllegalArgumentException("addToMenu must be called first")
            else -> throw IllegalArgumentException("An unknown error occurred")
        }
    }

    fun setSizeAndLocationWideScreen(shell: Shell) {
        val displayWidth = Display.getCurrent().primaryMonitor.clientArea.width
        val displayHeight = Display.getCurrent().primaryMonitor.clientArea.height
        val width = (displayWidth / 1.25).toInt()
        val height = displayHeight / 2
        shell.setSize(width, height)
        val locationx = displayWidth / 2 - ((width) / 2)
        val locationy = displayHeight / 2 - ((height) / 2)
        shell.setLocation(locationx, locationy)
    }

    /*
     * Sets size and location in the middle of the screen
     * without letting you go off the screen.  If you pass in 0
     * the default is half the display width and height.
     */
    @JvmStatic
    fun setSizeAndLocationMiddleScreen(shell: Shell, width: Int, height: Int) {
        var width = width
        var height = height
        val displayWidth = Display.getCurrent().primaryMonitor.clientArea.width
        val displayHeight = Display.getCurrent().primaryMonitor.clientArea.height
        if (width == 0) {
            width = displayWidth / 2
        }
        if (height == 0) {
            height = displayHeight / 2
        }
        if (width > displayWidth) {
            width = displayWidth
        }
        if (height > displayHeight) {
            height = displayHeight
        }
        shell.setSize(width, height)
        val locationx = displayWidth / 2 - ((width) / 2)
        val locationy = displayHeight / 2 - ((height) / 2)
        shell.setLocation(locationx, locationy)
    }

    fun setSizeAndLocationHalfScreen(shell: Shell) {
        val displayWidth = Display.getCurrent().primaryMonitor.clientArea.width
        val displayHeight = Display.getCurrent().primaryMonitor.clientArea.height
        val width = displayWidth / 3
        val height = (displayHeight / 4) * 3
        shell.setSize(width, height)
        val locationx = displayWidth / 2 - ((width) / 2)
        val locationy = displayHeight / 2 - ((height) / 2)
        shell.setLocation(locationx, locationy)
    }

    fun setFullScreen(shell: Shell) {
        val displayWidth = Display.getCurrent().primaryMonitor.clientArea.width
        val displayHeight = Display.getCurrent().primaryMonitor.clientArea.height
        shell.setSize(displayWidth, displayHeight)
        shell.setLocation(0, 0)
    }

    fun setSizeAndLocation(shell: Shell, previous: SizeAndLocation) {
        val displayWidth = Display.getCurrent().primaryMonitor.clientArea.width
        val displayHeight = Display.getCurrent().primaryMonitor.clientArea.height
        if (previous.sizex > displayWidth) {
            previous.sizex = displayWidth
        }
        if (previous.sizey > displayHeight) {
            previous.sizey = displayHeight
        }
        shell.setSize(previous.sizex, previous.sizey)
        shell.setLocation(previous.locx, previous.locy)
    }

    fun saveLocation(shell: Shell): SizeAndLocation {
        return SizeAndLocation(
            shell.size.x, shell.size.y, shell.location.x, shell.location.y
        )
    }

    @JvmStatic
    fun makeGroup(
        comp: Composite?, swtOptions: Int, gridColumns: Int, columnsEqual: Boolean
    ): Group {
        val g = Group(comp, swtOptions)
        val gl = GridLayout(gridColumns, columnsEqual)
        g.layout = gl
        g.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        return g
    }

    fun makeToggleButton(
        parent: Composite?, text: String?, columns: Int, onClick: Consumer<SelectionEvent>?
    ): Button {
        return ButtonBuilder1(text, columns, onClick).apply {
            this.parent = parent
            swtOptions = SWT.TOGGLE
        }.build()
    }

    private val log: Logger = LoggerFactory.getLogger(EasySWT::class.java)

    fun setGridData(c: Control) {
        c.layoutData = GridData().apply {
            horizontalAlignment = GridData.FILL
            verticalAlignment = GridData.FILL
            grabExcessHorizontalSpace = true
        }
    }

    fun setGridDataVertical(c: Control) {
        setGridData(c)
        (c.layoutData as GridData).grabExcessVerticalSpace = true
    }

    fun setGridDataGroup(group: Group) {
        setGridData(group)
        (group.layoutData as GridData).grabExcessVerticalSpace = true
    }

    fun makeButton(parent: Composite, style: Int): ButtonBuilder {
        return ButtonBuilder(parent, style)
    }

    fun makeCheckbox(parent: Composite): ButtonBuilder {
        return ButtonBuilder(parent, SWT.CHECK)
    }

    @JvmStatic
    fun makeComboDropdown(parent: Composite): ComboBuilder {
        return ComboBuilder(parent, SWT.DROP_DOWN or SWT.READ_ONLY)
    }

    fun makeLabel(parent: Composite): LabelBuilder {
        return LabelBuilder(parent, SWT.NONE)
    }

    @JvmOverloads
    fun makeTextBuilder(parent: Composite, style: Int = SWT.BORDER or SWT.SINGLE): TextBuilder {
        return TextBuilder(parent, style)
    }

    fun makeStyledText(parent: Composite, style: Int): StyledTextBuilder {
        return StyledTextBuilder(parent, style)
    }

    fun addEscapeCloseListener(shell: Shell) {
        shell.addTraverseListener { e: TraverseEvent ->
            if (e.detail == SWT.TRAVERSE_ESCAPE) {
                shell.close()
                e.detail = SWT.TRAVERSE_NONE
                e.doit = false
            }
        }
    }

    fun addEnterListener(control: Control, onEnter: Consumer<KeyEvent>) {
        control.addKeyListener(
            object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (e.keyCode == SWT.CR.code || e.keyCode == SWT.KEYPAD_CR) {
                        onEnter.accept(e)
                    }
                }
            })
    }


    /**
     * Calculate sane size for large windows that may run off the screen
     */
    fun setLargeDialogSize(shell: Shell) {
        // Give the window a sane default size and location
        val start = System.currentTimeMillis()
        val size = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT)
        val clientArea = shell.monitor.clientArea
        log.debug(
            "Resizing dialog to {}x{} | Client area x {} y {} width {} height {} | took {}",
            size.x,
            size.y,
            clientArea.x,
            clientArea.y,
            clientArea.width,
            clientArea.height,
            DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start)
        )
        val bounds = shell.bounds
        if (size.y > clientArea.height) {
            size.y = clientArea.height
            // Also put window at the top of the screen
            bounds.y = 0
        }
        if (size.x > clientArea.width) {
            size.x = clientArea.width
            // Put the window at the left of the screen
            bounds.x = 0
        }
        if (bounds.x < clientArea.x) {
            bounds.x = clientArea.x
        }
        if (bounds.y < clientArea.y) {
            bounds.y = clientArea.y
        }
        shell.bounds = bounds
        shell.size = size
    }

    /**
     * Generate a standard label
     */
    fun addLabel(parent: Composite?, text: String?): Label {
        val label = newLabel(parent, text)
        setGridData(label)
        return label
    }

    fun newLabel(parent: Composite?, text: String?): Label {
        val label = Label(parent, 0)
        label.text = text
        return label
    }

    fun makeSelectedListener(function: Consumer<SelectionEvent>): SelectionListener {
        return object : SelectionAdapter() {
            override fun widgetSelected(se: SelectionEvent) {
                function.accept(se)
            }
        }
    }

    fun makeKeyListener(swtmod: Int, swtchar: Int, function: Consumer<KeyEvent>): KeyAdapter {
        return object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.stateMask == swtmod && e.character.code == swtchar) {
                    function.accept(e)
                }
            }

            override fun keyReleased(e: KeyEvent) {
                // TODO Auto-generated method stub
            }
        }
    }

    fun addSelectionListener(
        item: MenuItem, function: Consumer<SelectionEvent>
    ): SelectionListener {
        return makeSelectedListener(function).also { item.addSelectionListener(it) }
    }

    fun addSelectionListener(
        item: ToolItem, function: Consumer<SelectionEvent>
    ): SelectionListener {
        return makeSelectedListener(function).also { item.addSelectionListener(it) }
    }

    @JvmStatic
    fun addSelectionListener(
        button: Button, function: Consumer<SelectionEvent>
    ): SelectionListener {
        return makeSelectedListener(function).also { button.addSelectionListener(it) }
    }

    fun addSelectionListener(
        text: StyledText, function: Consumer<SelectionEvent>
    ): SelectionListener {
        return makeSelectedListener(function).also { text.addSelectionListener(it) }
    }

    fun addSelectionListener(
        combo: Combo, function: Consumer<SelectionEvent>
    ): SelectionListener {
        return makeSelectedListener(function).also { combo.addSelectionListener(it) }
    }

    fun addSelectionListener(
        combo: CCombo, function: Consumer<SelectionEvent>
    ): SelectionListener {
        return makeSelectedListener(function).also { combo.addSelectionListener(it) }
    }

    fun addSelectionListener(
        combo: ScrollBar, function: Consumer<SelectionEvent>
    ): SelectionListener {
        return makeSelectedListener(function).also { combo.addSelectionListener(it) }
    }

    fun addSelectionListener(
        spinner: Spinner, function: Consumer<SelectionEvent>
    ): SelectionListener {
        return makeSelectedListener(function).also { spinner.addSelectionListener(it) }
    }

    fun addKeyListener(
        shell: Shell, swtmod: Int, swtchar: Int, function: Consumer<KeyEvent>
    ): KeyAdapter {
        return makeKeyListener(swtmod, swtchar, function).also { shell.addKeyListener(it) }
    }

    fun addDoubleFilter(t: Text) {
        addNumberFilter(t, false)
    }

    fun addIntegerFilter(t: Text) {
        addNumberFilter(t, true)
    }

    /**
     * Disallow non-number and non-navigation keys in the text field
     */
    private fun addNumberFilter(t: Text, noDecimal: Boolean) {
        t.addKeyListener(NumberFilterKeyListener(noDecimal))
    }

    fun calcAverageCharWidth(parent: Composite?): Int {
        val gc = GC(parent)
        val averageCharWidth = gc.fontMetrics.averageCharacterWidth.toInt()
        gc.dispose()
        return averageCharWidth
    }

    @JvmStatic
    fun calcAverageCharHeight(parent: Composite?): Int {
        val gc = GC(parent)
        val averageCharHeight = gc.fontMetrics.height
        gc.dispose()
        return averageCharHeight
    }

    fun addModifyListener(textBox: Text, function: Consumer<ModifyEvent>): ModifyListener {
        val listener = makeModifyListener(function)
        textBox.addModifyListener(listener)
        return listener
    }

    fun makeModifyListener(function: Consumer<ModifyEvent>): ModifyListener {
        return ModifyListener { function.accept(it) }
    }

    /**
     * Allows finding widgets by id's using eg bot.getStyledTextWithId(name)
     */
    fun addSwtBotKey(widget: Control, name: String?) {
        widget.setData(SWTBOT_WIDGET_KEY, name)
    }

    /**
     * Makes a MessageBox SWT class to display the given message.
     *
     * @return True if Yes was clicked
     */
    @JvmStatic
    fun makeEasyYesNoDialog(windowTitle: String?, message: String?, parent: Shell?): Boolean {
        val msg = MessageBox(parent, SWT.YES or SWT.NO)
        msg.text = windowTitle
        msg.message = message
        return msg.open() == SWT.YES
    }

    /**
     * Makes a MessageBox SWT class to display the given message
     */
    @JvmStatic
    fun makeEasyOkDialog(windowTitle: String?, message: String, parent: Shell?) {
        val dialog = Shell(parent, SWT.APPLICATION_MODAL or SWT.DIALOG_TRIM)
        dialog.text = windowTitle
        dialog.layout = GridLayout(1, false)
        val comp = makeComposite(dialog, 1)
        makeLabel(comp, message, 1)
        val button = makePushButton(comp, "Ok", 1) { dialog.close() }
        button.layoutData = buildGridData()
            .setAlign(SWT.CENTER, SWT.CENTER)
            .setColumns(1)
            .setHint(getWidthOfText("Ok") * 3, SWT.DEFAULT)
            .build()

        dialog.pack()
        dialog.open()
    }

    /**
     * Sets the size of a label so that it fits the entirety of its text, regardless of OS settings
     */
    @JvmStatic
    fun setLabelSizeToFit(label: Label, maxWidth: Int) {
        val tempData = buildGridData().applyTo(label)
        if (tempData.widthHint > maxWidth) {
            val sb = StringBuilder()
            val text = label.text
            for (element in text) {
                sb.append(element)
                if (getWidthOfText(sb.toString()) > maxWidth) {
                    for (curChar in sb.length - 1 downTo 0) {
                        if (sb[curChar] == ' ') {
                            sb.insert(curChar, System.lineSeparator())
                            break
                        }
                    }
                }
            }
            label.text = sb.toString()
            buildGridData().applyTo(label)
        }
    }

    fun addResizeListener(control: Control, text: StyledText, callback: Consumer<StyledText>) {
        control.addControlListener(object : ControlAdapter() {
            override fun controlResized(e: ControlEvent) {
                callback.accept(text)
            }
        })
    }
}
