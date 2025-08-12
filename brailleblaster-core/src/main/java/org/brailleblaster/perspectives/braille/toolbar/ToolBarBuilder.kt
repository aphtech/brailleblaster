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
package org.brailleblaster.perspectives.braille.toolbar

import org.apache.commons.lang3.StringUtils
import org.brailleblaster.utils.localization.LocaleHandler
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.perspectives.braille.toolbar.ToolBarSettings.saveUserSettings
import org.brailleblaster.perspectives.braille.toolbar.ToolBarSettings.scale
import org.brailleblaster.perspectives.braille.toolbar.ToolBarSettings.userSettings
import org.brailleblaster.perspectives.mvc.ViewManager.Companion.colorizeIconToolbars
import org.brailleblaster.perspectives.mvc.ViewManager.Companion.colorizeToolbarHolder
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.EmphasisItem
import org.brailleblaster.perspectives.mvc.menu.MenuManager
import org.brailleblaster.perspectives.mvc.menu.MenuManager.createSharedSubMenu
import org.brailleblaster.perspectives.mvc.menu.MenuManager.getEmphasisSelection
import org.brailleblaster.perspectives.mvc.menu.MenuManager.getFromStyleMenu
import org.brailleblaster.perspectives.mvc.menu.MenuManager.styleMenuCategories
import org.brailleblaster.perspectives.mvc.menu.SharedItem
import org.brailleblaster.perspectives.mvc.modules.misc.ToggleViewsModule.Companion.loadSettings
import org.brailleblaster.perspectives.mvc.modules.misc.ToggleViewsModule.Views
import org.brailleblaster.perspectives.mvc.modules.views.DebugModule
import org.brailleblaster.util.ImageHelper
import org.brailleblaster.wordprocessor.WPManager.Companion.getInstance
import org.eclipse.swt.SWT
import org.eclipse.swt.events.MouseAdapter
import org.eclipse.swt.events.MouseEvent
import org.eclipse.swt.events.MouseMoveListener
import org.eclipse.swt.graphics.Cursor
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.Rectangle
import org.eclipse.swt.graphics.Resource
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors
import kotlin.math.abs

class ToolBarBuilder(
    private val parent: Shell,
    private val onRebuild: Consumer<ToolBar>,
    private val onExpand: Consumer<ToolBar>,
    private val onCondense: Consumer<ToolBar>
) {
    private val disposeables: MutableList<Resource> //List of SWT widgets that need to be disposed but won't be disposed alongside the toolbar
    private var sections: MutableList<IToolBarElement>
    private val customToolbars: MutableList<ToolBarCustomSection>
    private val widgets: MutableList<ToolBar>
    private var expanded = false
    private val arrowCursor: Cursor
    private val moveCursor: Cursor
    private var state: MouseState = MouseState()
    private val toolBarHolder: Composite
    private val userScale: ToolBarSettings.Scale

    init {
        sections = ArrayList()
        customToolbars = ArrayList()
        widgets = ArrayList()
        arrowCursor = Cursor(Display.getCurrent(), SWT.CURSOR_ARROW)
        moveCursor = Cursor(Display.getCurrent(), SWT.CURSOR_SIZEALL)
        toolBarHolder = Composite(parent, SWT.NONE)
        toolBarHolder.layout = GridLayout(1, false)
        colorizeToolbarHolder(toolBarHolder)
        disposeables = ArrayList()
        disposeables.add(arrowCursor)
        disposeables.add(moveCursor)
        userScale = scale
    }

    fun createSection(setting: ToolBarSettings.Settings) {
        val section = when (setting) {
            ToolBarSettings.Settings.DOCUMENT -> createDocumentSection()
            ToolBarSettings.Settings.FILE -> createFileSection()
            ToolBarSettings.Settings.STYLE -> createStyleSection()
            ToolBarSettings.Settings.TOOLS -> createToolsSection()
            ToolBarSettings.Settings.VIEW -> createViewSection()
            ToolBarSettings.Settings.EMPHASIS -> createEmphasisSection()
            ToolBarSettings.Settings.NEWLINE -> createNewLine()
            ToolBarSettings.Settings.MATH -> createMath()
        }
        sections.add(section)
        if (section is ToolBarSection) {
            calculateLineWrap()
        }
    }

    private fun createMath(): ToolBarSection {
        val section = ToolBarSection(ToolBarSettings.Settings.MATH)
        section.addItem(getImage(MathModule.MATH_TOGGLE_KEY), MathModule.MATH_TOGGLE, SharedItem.MATH_TOGGLE)
        section.addItem(getImage(MathModule.NUMERIC_SERIES_KEY), MathModule.NUMERIC_SERIES, SharedItem.NUMERIC_SERIES)
        section.addItem(getImage(MathModule.ASCII_EDITOR_KEY), MathModule.ASCII_EDITOR, SharedItem.ASCII_EDITOR)
        section.addItem(getImage(MathModule.SPATIAL_MATH_KEY), MathModule.SPATIAL_COMBO, SharedItem.SPATIAL_COMBO)
        section.addItem(getImage(MathModule.NEMETH_BLOCK_KEY), MathModule.NEMETH_BLOCK, SharedItem.NEMETH_BLOCK)
        section.addItem(getImage(MathModule.NEMETH_INLINE_KEY), MathModule.NEMETH_INLINE, SharedItem.NEMETH_INLINE)
        section.addItem(
            getImage(MathModule.NUMERIC_PASSAGE_BLOCK_KEY),
            MathModule.NUMERIC_BLOCK,
            SharedItem.NUMERIC_BLOCK
        )
        section.addItem(
            getImage(MathModule.NUMERIC_PASSAGE_INLINE_KEY),
            MathModule.NUMERIC_INLINE,
            SharedItem.NUMERIC_INLINE
        )
        //always make the help section last
        section.addItem(getImage(MathModule.MATH_HELP_KEY), MathModule.MATH_HELP, SharedItem.ABOUT_MATH)
        if (DebugModule.enabled) {
            /*
             * if you take one out of debug, enable it in the Math Module
             */
            section.addItem(getImage(MathModule.MATH_TABLE_KEY), MathModule.MATH_TABLE, SharedItem.MATH_TABLE)
        }
        return section
    }

    fun createCustomToolBar(custTB: CustomToolBarBuilder) {
        val section = ToolBarCustomSection(custTB, toolBarHolder)
        sections.add(section)
        customToolbars.add(section)
    }

    private fun createFileSection(): ToolBarSection {
        val section = ToolBarSection(ToolBarSettings.Settings.FILE)
        section.addItem(getImage("open"), "Open", SharedItem.OPEN)
        section.addItem(getImage("save"), "Save", SharedItem.SAVE)
        section.addItem(getImage("print"), "Print", SharedItem.PRINT)
        section.addItem(getImage("emboss"), "Emboss", SharedItem.EMBOSS)
        return section
    }

    private fun createDocumentSection(): ToolBarSection {
        val section = ToolBarSection(ToolBarSettings.Settings.DOCUMENT)
        //Removing for now since it isn't necessary anymore
//		section.addItem(getImage("translate"), "Refresh Translation", SharedItem.REFRESH);
        section.addItem(getImage("undo"), "Undo", SharedItem.UNDO)
        section.addItem(getImage("redo"), "Redo", SharedItem.REDO)
        section.addItem(getImage("braillePreview"), "Braille Preview", SharedItem.BRAILLE_PREVIEW)
        return section
    }

    private fun createStyleSection(): ToolBarSection {
        val section = ToolBarSection(ToolBarSettings.Settings.STYLE)
        val categories = styleMenuCategories
        for (category in categories) {
            section.addItem(
                getImage(category), StringUtils.capitalize(category.lowercase(Locale.getDefault()))
            ) { e: BBSelectionData -> openStyleMenu(category, e) }
        }
        section.addItem(getImage("repeatStyle"), "Repeat Last Style", SharedItem.REPEAT_LAST_STYLE)
        return section
    }

    private fun openStyleMenu(category: String, event: BBSelectionData) {
        val testMenu = getFromStyleMenu(category, toolBarHolder)
        setMenuLocation(testMenu!!, event)
    }

    private fun createViewSection(): ToolBarSection {
        val section = ToolBarSection(ToolBarSettings.Settings.VIEW)
        val activeViews: List<Views> = loadSettings()
        section.addCheckItem(getImage("view_S"), "Style View", activeViews.contains(Views.STYLE), SharedItem.STYLE_VIEW)
        section.addCheckItem(getImage("view_P"), "Print View", activeViews.contains(Views.PRINT), SharedItem.PRINT_VIEW)
        section.addCheckItem(
            getImage("view_B"),
            "Braille View",
            activeViews.contains(Views.BRAILLE),
            SharedItem.BRAILLE_VIEW
        )
        return section
    }

    private fun createToolsSection(): ToolBarSection {
        val section = ToolBarSection(ToolBarSettings.Settings.TOOLS)
        section.addItem(getImage("editTable"), "Edit Table", SharedItem.EDIT_TABLE)
        val currentManager = getInstance().controller
        section.addCheckItem(
            getImage("sixKey"),
            LOCALE_HANDLER["SixKeyMode.toolbarItem"], currentManager.text.sixKeyMode, SharedItem.SIX_KEY
        )
        section.addItem(getImage("findReplace"), "Find/Replace", SharedItem.SEARCH)
        section.addItem(getImage("changeTranslation"), "Change Translation") { e: BBSelectionData ->
            val newMenu = createSharedSubMenu(
                SharedItem.CHANGE_TRANSLATION,
                parent, true
            )
            newMenu.isVisible = true
            setMenuLocation(newMenu, e)
        }
        return section
    }

    private fun createEmphasisSection(): ToolBarSection {
        val section = ToolBarSection(ToolBarSettings.Settings.EMPHASIS)
        section.addItem(getImage("bold"), EmphasisItem.BOLD.longName, getEmphasisSelection(EmphasisItem.BOLD))
        section.addItem(getImage("italic"), EmphasisItem.ITALIC.longName, getEmphasisSelection(EmphasisItem.ITALIC))
        section.addItem(
            getImage("underline"),
            EmphasisItem.UNDERLINE.longName,
            getEmphasisSelection(EmphasisItem.UNDERLINE)
        )
        section.addItem(getImage("script"), EmphasisItem.SCRIPT.longName, getEmphasisSelection(EmphasisItem.SCRIPT))
        section.addItem(
            getImage("tnNote"),
            EmphasisItem.TNSYMBOLS.longName,
            getEmphasisSelection(EmphasisItem.TNSYMBOLS)
        )
        section.addItem(
            getImage("tdTypeform"), "Transcriber-Defined Typeforms"
        ) { e: BBSelectionData ->
            val newMenu = createSharedSubMenu(
                SharedItem.TYPEFORMS,
                parent, true
            )
            newMenu.isVisible = true
            setMenuLocation(newMenu, e)
        }
        return section
    }

    private fun createNewLine(): ToolBarNewLine {
        return ToolBarNewLine(true)
    }

    private fun setMenuLocation(menu: Menu, event: BBSelectionData) {
        var cursorLoc: Rectangle? = null
        val cursorControl = Display.getCurrent().cursorControl
        // User Exception #321, #328: This can be null sometimes on at least OS X
        if (cursorControl != null) {
            cursorLoc = cursorControl.bounds //Should be the toolbar
        }

        if (cursorLoc == null) cursorLoc = Rectangle(0, 0, 0, 0)
        val toolItem = (event.widget as ToolItem).bounds
        menu.setLocation(toolBarHolder.toDisplay(cursorLoc.x + toolItem.x, cursorLoc.y + toolItem.height))
        menu.isVisible = true
    }

    fun build(): ToolBar {
        var newToolBar = ToolBar(toolBarHolder, SWT.HORIZONTAL)
        colorizeIconToolbars(newToolBar)
        widgets.add(newToolBar)
        newToolBar.layoutData = GridData(SWT.LEFT, SWT.LEFT, false, true, 1, 1)
        for (section in sections) {
            val tempToolBar = section.createSection(newToolBar, MenuManager.sharedToolBars)
            colorizeIconToolbars(tempToolBar)
            if (tempToolBar !== newToolBar) {
                widgets.add(tempToolBar)
                newToolBar = tempToolBar
            }
        }
        addListeners()
        return newToolBar
    }

    private fun calculateLineWrap() {
        var lineWidth = 0
        val shellWidth = parent.clientArea.width
        for (i in sections.indices) {
            val section = sections[i]
            if (section is ToolBarNewLine) {
                lineWidth = 0
            } else {
                val beginningOfLine = lineWidth == 0
                val tbSection = section as ToolBarSection
                val horizPadding = (toolBarHolder.layout as GridLayout).horizontalSpacing
                lineWidth += tbSection.getImageWidth(horizPadding) + ToolBarSection.DEFAULT_RELOCATOR_WIDTH + horizPadding
                if (lineWidth > shellWidth && !beginningOfLine) {
                    sections.add(i, ToolBarNewLine(false))
                    lineWidth = 0
                }
            }
        }
    }

    private fun addListeners() {
        val relocBounds: MutableList<RelocatorRectangle> = ArrayList() //"Hitboxes" for the relocators
        for (toolBar in widgets) {
            val sectionsInToolBar = sections.stream()
                .filter { tb: IToolBarElement? -> tb is ToolBarSection && tb.parent === toolBar }.toList()
            for (iToolBarElement in sectionsInToolBar) {
                if (iToolBarElement is ToolBarSection) {
                    relocBounds.add(RelocatorRectangle(iToolBarElement.relocator.bounds, toolBar))
                }
            }
            if (sectionsInToolBar.isNotEmpty()) {
                val lastSection = sectionsInToolBar[sectionsInToolBar.size - 1] as ToolBarSection
                val endOfToolbar = lastSection.x + lastSection.width
                relocBounds.add(
                    RelocatorRectangle(
                        Rectangle(
                            endOfToolbar,
                            lastSection.y,
                            lastSection.dockPoint.width,
                            lastSection.height
                        ), toolBar
                    )
                )
            }
        }

        val mml = handleDragging(relocBounds)
        for (toolBar in widgets) {
            toolBar.addMouseMoveListener(mml)
            toolBar.addMouseListener(object : MouseAdapter() {
                override fun mouseDown(e: MouseEvent) {
                    handleStartDrag(e, toolBar, relocBounds)
                }

                override fun mouseUp(e: MouseEvent) {
                    handleEndDrag(e, toolBar, relocBounds)
                }
            })
        }
    }

    private fun handleStartDrag(e: MouseEvent, toolBar: ToolBar, relocBounds: List<RelocatorRectangle>) {
        //Find the current ToolBarSection and begin dragging behavior
        val reloc = overRelocator(relocBounds, e.x, e.y, toolBar)
        if (reloc >= 0) {
            var section: ToolBarSection? = null //Current ToolBarSection
            for (iToolBarElement in sections) {
                if (iToolBarElement is ToolBarSection && iToolBarElement.parent === toolBar && iToolBarElement.relocator.bounds == relocBounds[reloc].rectangle) {
                    section = iToolBarElement
                    break
                }
            }
            if (section != null) {
                state.draggingSection = section //Mark current ToolBarSection as the section being dragged
            }
            val draggingImage = ImageHelper.createScaledImage("draggingcursor.png", 1f)
            toolBar.cursor = Cursor(
                Display.getCurrent(),
                draggingImage.imageData,
                draggingImage.bounds.width / 2,
                draggingImage.bounds.height / 2
            )
        }
    }

    private fun handleDragging(relocBounds: List<RelocatorRectangle>): MouseMoveListener {
        return MouseMoveListener { e: MouseEvent ->
            val toolBar = e.source as ToolBar
            if (e.data == null) {
                for (widget in widgets) {
                    //SWT captures drag movements onto the first control that was clicked. The fix
                    //is for all toolbars to have their listeners fire on a MouseMoveListener.
                    if (widget !== e.source) {
                        //Calculate the correct Y for each toolbar.
                        val toolBarHeight =
                            widget.bounds.height + (if (widget.parent.layout is GridLayout) (widget.parent.layout as GridLayout).verticalSpacing else 0)
                        val relativeToolBarPos =
                            toolBarHeight * (widgets.indexOf(widget) - widgets.indexOf(toolBar)) * -1
                        val event = Event()
                        event.type = SWT.MouseMove
                        event.data = -1 //Ensure other toolbars do not get stuck in a loop of listeners
                        event.x = e.x
                        event.y = e.y + relativeToolBarPos
                        widget.notifyListeners(SWT.MouseMove, event)
                    }
                }
            }
            if (state.draggingSection != null) { //The user is currently dragging another section.
                for (i in sections.indices) {
                    if (sections[i] is ToolBarSection && sections[i] !== state.draggingSection && (sections[i] as ToolBarSection).parent === toolBar) {
                        val sect = sections[i] as ToolBarSection
                        var changedWidth = false
                        if (rectangleCollision(
                                e.x,
                                e.y,
                                sect.dockPoint,
                                DRAG_PADDING,
                                0
                            )
                        ) { //User may be dragging the section here
                            if (sect.relocator.width != EXTENDED_RELOCATOR) {
                                state.collidingSection = sect
                                changedWidth = true
                                sect.relocator.width = EXTENDED_RELOCATOR
                            }
                        } else {
                            if (sect.relocator.width != ToolBarSection.DEFAULT_RELOCATOR_WIDTH) {
                                state.collidingSection = null
                                changedWidth = true
                                sect.relocator.width = ToolBarSection.DEFAULT_RELOCATOR_WIDTH
                            }
                        }
                        if (changedWidth) {
                            for (j in i until sections.size) { //All sections will be shifted if the relocator width was changed
                                if (sections[j] is ToolBarSection && (sections[j] as ToolBarSection).parent === toolBar) {
                                    (sections[j] as ToolBarSection).draw(toolBar)
                                    (sections[j] as ToolBarSection).drawRelocator(toolBar)
                                }
                            }
                            toolBar.setSize(
                                toolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).x + 20,
                                toolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).y
                            )
                            toolBar.parent.size = toolBar.parent.computeSize(SWT.DEFAULT, SWT.DEFAULT)
                        }
                    }
                }
                if (rectangleCollision(e.x, e.y, findBottomDockPoint(toolBar))) {
                    if (!expanded) {
                        onExpand.accept(toolBar)
                        expanded = true
                    }
                } else {
                    if (expanded) {
                        onCondense.accept(toolBar)
                        expanded = false
                    }
                }
            } else {
                toolBar.cursor = if (overRelocator(relocBounds, e.x, e.y, toolBar) >= 0) moveCursor else arrowCursor
            }
        }
    }

    private fun handleEndDrag(e: MouseEvent, toolBar: ToolBar, relocBounds: List<RelocatorRectangle>) {
        //See if a ToolBarSection has been dragged to another place
        if (state.draggingSection != null) {
            if (state.collidingSection != null) {
                val settings = userSettings
                removeLineWrapNewLines()
                settings.removeAt(sections.indexOf(state.draggingSection!!))
                sections.remove(state.draggingSection!!)
                settings.add(sections.indexOf(state.collidingSection!!), state.draggingSection!!.name)
                saveUserSettings(settings)
                onRebuild.accept(toolBar)
            } else {
                val collidingToolBar = findToolBar(toolBar, e.y) //Find the index of the toolbar the mouse ended on
                if (collidingToolBar >= 0) {
                    val endOfLineBounds = getEndOfLineBounds(relocBounds)
                    val endOfToolBar = endOfLineBounds[collidingToolBar]
                    if (rectangleCollision(e.x, 0, endOfToolBar.rectangle, DRAG_PADDING, 0)) { //Dragging to end
                        val finalToolBar = widgets[collidingToolBar]
                        val sectionsInToolbar = getSectionsInToolBar(finalToolBar)
                        val lastElement = sectionsInToolbar[sectionsInToolbar.size - 1] as ToolBarSection
                        if (state.draggingSection != lastElement) {
                            val settings = userSettings
                            removeLineWrapNewLines()
                            settings.removeAt(sections.indexOf(state.draggingSection!!))
                            sections.remove(state.draggingSection!!)
                            settings.add(sections.indexOf(lastElement) + 1, state.draggingSection!!.name)
                            saveUserSettings(settings)
                            onRebuild.accept(toolBar)
                        }
                    }
                } else {
                    val bottomDockPoint = findBottomDockPoint(toolBar)
                    if (rectangleCollision(e.x, e.y, bottomDockPoint)) {
                        val settings = userSettings
                        removeLineWrapNewLines()
                        settings.removeAt(sections.indexOf(state.draggingSection!!))
                        settings.add(ToolBarSettings.Settings.NEWLINE)
                        settings.add(state.draggingSection!!.name)
                        saveUserSettings(settings)
                        onRebuild.accept(toolBar)
                    }
                }
            }
            if (!toolBar.isDisposed) toolBar.cursor = arrowCursor
            state.draggingSection = null
        }
    }

    private fun findToolBar(startingToolBar: ToolBar, y: Int): Int {
        val toolBarHeight = startingToolBar.bounds.height
        val toolBarIndex = widgets.indexOf(startingToolBar)
        if (y in 0..toolBarHeight) {
            return toolBarIndex
        } else {
            val dist = if (y > 0) y / toolBarHeight else (abs((y / toolBarHeight).toDouble()) + 1).toInt()
            if (y < 0 && toolBarIndex - dist >= 0) {
                return toolBarIndex - dist
            } else if (y > 0 && toolBarIndex + dist < widgets.size) {
                return toolBarIndex + dist
            }
        }
        return -1
    }

    private fun getEndOfLineBounds(relocBounds: List<RelocatorRectangle>): List<RelocatorRectangle> {
        return relocBounds.stream()
            .filter { r: RelocatorRectangle ->
                val index = relocBounds.indexOf(r)
                if (index == relocBounds.size - 1) return@filter true
                if (index > 0) {
                    return@filter relocBounds[index - 1].rectangle.x < r.rectangle.x && relocBounds[index + 1].rectangle.x < r.rectangle.x
                }
                false
            }.collect(Collectors.toList())
    }

    private fun getSectionsInToolBar(toolBar: ToolBar): List<IToolBarElement> {
        return sections.stream().filter { tbe: IToolBarElement? -> tbe is ToolBarSection && tbe.parent === toolBar }
            .collect(Collectors.toList())
    }

    private fun findBottomDockPoint(toolBar: ToolBar): Rectangle {
        val lastToolBar = widgets[widgets.size - 1]
        val bottomDockPoint: Rectangle
        if (lastToolBar === toolBar) {
            bottomDockPoint =
                Rectangle(0, toolBar.bounds.height, toolBar.shell.clientArea.width, toolBar.bounds.height / 2)
        } else {
            var bottomStart = (widgets.size - widgets.indexOf(toolBar)) * toolBar.bounds.height
            if (toolBar.parent.layout is GridLayout) bottomStart += (toolBar.parent.layout as GridLayout).verticalSpacing * (widgets.size - widgets.indexOf(
                toolBar
            ))
            bottomDockPoint = Rectangle(0, bottomStart, toolBar.shell.clientArea.width, toolBar.bounds.height / 2)
        }
        return bottomDockPoint
    }

    private fun overRelocator(relocBounds: List<RelocatorRectangle>, x: Int, y: Int, source: ToolBar): Int {
        for (rect in relocBounds) {
            if (rect.source === source && rectangleCollision(x, y, rect.rectangle)) {
                return relocBounds.indexOf(rect)
            }
        }
        return -1
    }

    private fun removeLineWrapNewLines() {
        sections = sections.stream().filter { s: IToolBarElement? -> !(s is ToolBarNewLine && !s.saveInSettings) }
            .collect(Collectors.toList())
    }

    private fun rectangleCollision(
        x: Int,
        y: Int,
        rect: Rectangle,
        horizPadding: Int = 0,
        vertPadding: Int = 0
    ): Boolean {
        return x >= rect.x - horizPadding && x <= rect.x + rect.width + horizPadding && y >= rect.y - vertPadding && y <= rect.y + rect.height + vertPadding
    }

    private fun getImage(imgName: String): Image {
        val fileName = TOOLBAR_FOLDER + userScale.scale + "/" + imgName + ".png"
        val scaledImage = ImageHelper.createScaledImage(fileName, 1f)
        disposeables.add(scaledImage)
        return scaledImage
    }

    fun dispose() {
        for (widget in widgets) {
            widget.dispose()
        }
        customToolbars.forEach(Consumer { obj: ToolBarCustomSection -> obj.dispose() })
        disposeables.forEach(Consumer { obj: Resource -> obj.dispose() })
    }

    val height: Int
        get() {
            var height = 0
            for (widget in widgets) {
                if (widget.itemCount > 0) {
                    height += widget.getItem(0).bounds.height
                }
                if (widget.parent.layout is GridLayout) {
                    height += (widget.parent.layout as GridLayout).verticalSpacing
                }
            }
            for (section in customToolbars) {
                //height += section.getHeight();
                height += 35 //Magic number - section.getHeight returns 0 periodically, and is not 100% reliable.
            }
            return height
        }

    private class MouseState {
        var draggingSection: ToolBarSection? = null
        var collidingSection: ToolBarSection? = null
    }

    @JvmRecord
    private data class RelocatorRectangle(val rectangle: Rectangle, val source: ToolBar)

    companion object {
        val LOCALE_HANDLER: LocaleHandler = getDefault()
        const val TOOLBAR_FOLDER: String = "toolbar/"
        private const val DRAG_PADDING = 30 //Amount of leeway given when dragging toolbar section
        private const val EXTENDED_RELOCATOR = ToolBarSection.DEFAULT_RELOCATOR_WIDTH + 10
    }
}
