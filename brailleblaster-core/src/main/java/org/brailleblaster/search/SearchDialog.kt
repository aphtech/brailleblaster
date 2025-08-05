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
package org.brailleblaster.search

import org.brailleblaster.utils.localization.LocaleHandler.Companion.getBanaStyles
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.EnableListener
import org.brailleblaster.perspectives.mvc.menu.MenuManager
import org.brailleblaster.perspectives.mvc.menu.SharedItem
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.perspectives.mvc.modules.misc.TableSelectionModule.Companion.displayInvalidTableMessage
import org.brailleblaster.search.SavedSearches.findSize
import org.brailleblaster.search.SavedSearches.lastMemory
import org.brailleblaster.search.SavedSearches.replaceSize
import org.brailleblaster.search.SearchCriteria.*
import org.brailleblaster.search.SearchUtils.inTable
import org.brailleblaster.search.SearchUtils.isContainerStyle
import org.brailleblaster.tools.MenuTool
import org.brailleblaster.tools.RepeatSearchTool
import org.brailleblaster.utd.Style
import org.brailleblaster.utd.properties.EmphasisType.Companion.getEmphasisType
import org.brailleblaster.util.FormUIUtils.makeDialogFloating
import org.brailleblaster.util.FormUIUtils.setLargeDialogSize
import org.brailleblaster.util.Notify
import org.brailleblaster.util.Notify.notify
import org.brailleblaster.utils.swt.EasySWT.addSelectionListener
import org.brailleblaster.utils.swt.EasySWT.makeCheckBox
import org.brailleblaster.utils.swt.EasySWT.makeComboDropdown
import org.brailleblaster.utils.swt.EasySWT.makeGroup
import org.brailleblaster.utils.swt.EasySWT.makePushButton
import org.brailleblaster.utils.swt.EasySWT.makeRadioButton
import org.brailleblaster.wordprocessor.WPManager.Companion.getInstance
import org.eclipse.jface.viewers.ArrayContentProvider
import org.eclipse.jface.viewers.ComboViewer
import org.eclipse.jface.viewers.LabelProvider
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Combo
import org.eclipse.swt.widgets.Dialog
import org.eclipse.swt.widgets.Group
import org.eclipse.swt.widgets.Shell
import java.util.*

/**
 * This class is responsible for the UI of the search.
 */
class SearchDialog(parent: Shell?, style: Int) : Dialog(parent, style),
    SimpleListener {
    private var click: Click? = null
    private var m: Manager? = null
    private var findCombo: Combo? = null
    private var replaceCombo: Combo? = null

    //These reflect the names of the items in the findFormatting and replaceFormatting arrays
    private var findList: org.eclipse.swt.widgets.List? = null
    private var replaceList: org.eclipse.swt.widgets.List? = null
    private var sc = SearchCriteria()
    private val location = Point(0, 0)

    private var findObj: Find? = null
    private var replaceObj: Replace? = null

    init {
        text = "SWT Dialog"
    }

    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent) {
            val searchTool: MenuTool = object : MenuTool {
                override val topMenu: TopMenu
                    get() = TopMenu.EDIT

                override val title: String
                    get() = SearchConstants.SEARCH

                override val accelerator: Int
                    get() = SWT.MOD1 + 'F'.code

                override val sharedItem: SharedItem
                    get() = SharedItem.SEARCH

                override val enableListener: EnableListener?
                    get() = null

                override fun onRun(bbData: BBSelectionData) {
                    m = bbData.manager
                    open()
                }
            }
            MenuManager.add(searchTool)

            MenuManager.add(RepeatSearchTool)
        }
    }

    private val rightManager: Manager
        get() = getInstance().controller

    fun open() {
        createContents()
        Companion.shell!!.open()
        Companion.shell!!.layout()
        setLargeDialogSize(Companion.shell!!)
    }

    private fun createContents() {
        sc = if (SavedSearches.lastSettings == null) SearchCriteria() else SavedSearches.lastSettings!!
        findObj = Find()
        replaceObj = Replace()
        createBetterUI()
    }

    private fun saveLocation() {
        if (Companion.shell != null && !Companion.shell!!.isDisposed) {
            location.x = (Companion.shell!!.location.x)
            location.y = (Companion.shell!!.location.y)
        }
    }

    private fun createBetterUI() {
        if (Companion.shell != null && !Companion.shell!!.isDisposed) {
            saveLocation()
            Companion.shell!!.close()
        }

        Companion.shell = makeDialogFloating(parent)
        Companion.shell!!.location = location
        Companion.shell!!.text = SearchConstants.FIND_REPLACE_SHELL
        Companion.shell!!.layout = GridLayout(1, false)
        Companion.shell!!.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)

        //Find textbox w/ history dropdown
        //Formatting button expands the group with selected styles & emphasis
        val findGroup = makeGroup(Companion.shell, 0, 2, false)
        findGroup.text = SearchConstants.FIND
        findCombo = Combo(findGroup, SWT.DROP_DOWN)
        findCombo!!.layoutData = GridData(SWT.FILL, SWT.FILL, true, true, 1, 1)
        val findFormattingBtn =
            makePushButton(
                findGroup, SearchConstants.FORMATTING, 1
            ) {}
        findFormattingBtn.layoutData = GridData(SWT.FILL, SWT.FILL, true, true, 1, 1)

        findList = makeList(findGroup)

        val findFormattingGroup = makeGroup(Companion.shell, 0, 1, false)
        findFormattingGroup.isVisible = false
        findFormattingGroup.text = SearchConstants.FIND + " " + SearchConstants.FORMATTING
        val findFormattingData = GridData(SWT.FILL, SWT.CENTER, true, true)
        findFormattingData.exclude = true
        findFormattingGroup.layoutData = findFormattingData

        addStylesDropdown(findFormattingGroup, true)
        addEmphasisDropdown(findFormattingGroup, true)
        addContainersDropdown(findFormattingGroup, true)
        findFormattingBtn.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                findFormattingGroup.isVisible = !findFormattingGroup.visible
                findFormattingData.exclude = !findFormattingData.exclude
                if (!findFormattingGroup.visible) {
                    findFormattingBtn.text = SearchConstants.FORMATTING
                } else {
                    findFormattingBtn.text = SearchConstants.CLOSE + " " + SearchConstants.FORMATTING
                }
                Companion.shell!!.layout()
                Companion.shell!!.pack()
            }
        })

        //Same thing as Find, but for Replace
        //Formatting button expands the group with selected styles & emphasis
        val replaceGroup = makeGroup(Companion.shell, 0, 2, false)
        replaceGroup.text = SearchConstants.REPLACE_WITH
        replaceCombo = Combo(replaceGroup, SWT.DROP_DOWN)
        replaceCombo!!.layoutData = GridData(SWT.FILL, SWT.FILL, true, true, 1, 1)
        val replaceFormattingBtn =
            makePushButton(
                replaceGroup, SearchConstants.FORMATTING, 1
            ) {}
        replaceFormattingBtn.layoutData = GridData(SWT.FILL, SWT.FILL, true, true, 1, 1)
        replaceList = makeList(replaceGroup)

        val replaceFormattingGroup = makeGroup(Companion.shell, 0, 1, false)
        replaceFormattingGroup.isVisible = false
        replaceFormattingGroup.text = SearchConstants.REPLACE + " " + SearchConstants.FORMATTING
        val replaceFormattingData =
            GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1)
        replaceFormattingData.exclude = true
        replaceFormattingGroup.layoutData = replaceFormattingData

        //Disable this in dev or release builds until List replacement is working
        addStylesDropdown(replaceFormattingGroup, false)
        addEmphasisDropdown(replaceFormattingGroup, false)
        // No good way to replace containers. They're pretty glitchy as-is...
        //addContainersDropdown(replaceFormattingGroup, false);
        replaceFormattingBtn.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                replaceFormattingGroup.isVisible = !replaceFormattingGroup.visible
                replaceFormattingData.exclude = !replaceFormattingData.exclude
                if (!replaceFormattingGroup.visible) {
                    replaceFormattingBtn.text = SearchConstants.FORMATTING
                } else {
                    replaceFormattingBtn.text = SearchConstants.CLOSE + " " + SearchConstants.FORMATTING
                }
                Companion.shell!!.layout()
                Companion.shell!!.pack()
            }
        })

        //Forward / Backward radio buttons.
        val directionGroup = makeGroup(Companion.shell, 0, 2, true)
        val directionsData = GridData(SWT.FILL, SWT.CENTER, true, true, 1, 2)
        directionsData.exclude = true
        directionGroup.layoutData = directionsData
        directionGroup.text = "Direction: "
        //TODO: Logic for direction does not yet exist in the v2.1 SearchController
        directionGroup.isVisible = false

        val fwdButton = makeRadioButton(
            directionGroup, SearchConstants.FORWARD, 1
        ) {}
        fwdButton.selection = sc.isSearchForward
        addSelectionListener(
            fwdButton
        ) {
            if (fwdButton.selection) {
                sc.isSearchForward = true
            }
        }
        val revButton = makeRadioButton(
            directionGroup, SearchConstants.BACKWARD, 1
        ) {}
        revButton.selection = !sc.isSearchForward
        addSelectionListener(
            revButton
        ) {
            if (revButton.selection) {
                sc.isSearchForward = false
            }
        }

        //Match Case / Match Whole Word checkboxes
        val matchGroup = makeGroup(Companion.shell, 0, 2, true)
        val matchCase = makeCheckBox(
            matchGroup, SearchConstants.MATCH_CASE
        ) {}
        matchCase.selection = sc.isFindCaseSensitive
        matchCase.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                sc.isFindCaseSensitive = !sc.isFindCaseSensitive
            }
        })

        val matchWord = makeCheckBox(
            matchGroup, SearchConstants.WHOLE_WORD
        ) {}
        matchWord.selection = sc.isWholeWord
        matchWord.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                sc.isWholeWord = !sc.isWholeWord
            }
        })

        //Find, Replace, Replace All buttons
        //Maybe add a Find All button that counts the number of found instances?
        val searchGroup = makeGroup(Companion.shell, 0, 3, false)
        val findButton = makePushButton(
            searchGroup, SearchConstants.FIND, 1
        ) {}
        Companion.shell!!.defaultButton = findButton
        findButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                search()
            }
        })

        val replaceButton = makePushButton(
            searchGroup, SearchConstants.REPLACE, 1
        ) {}
        replaceButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                replace()
            }
        })

        val replaceAllButton = makePushButton(
            searchGroup, SearchConstants.REPLACE_ALL, 1
        ) {}
        replaceAllButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                replaceAll()
            }
        })

        //Reset & Close buttons
        val closeGroup = makeGroup(Companion.shell, 0, 2, false)
        val resetButton = makePushButton(
            closeGroup, SearchConstants.RESET, 1
        ) {}
        resetButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                sc.reset()
                findObj!!.clickCount = 0
                replaceObj!!.replaceClickCount = 0
                findCombo!!.text = ""
                replaceCombo!!.text = ""
                findList!!.removeAll()
                findList!!.add("") //Ensures the menu size stays consistent.
                replaceList!!.removeAll()
                replaceList!!.add("")
                Companion.shell!!.layout()
                Companion.shell!!.pack()
            }
        })

        val closeButton = makePushButton(
            closeGroup, SearchConstants.CLOSE, 1
        ) {}
        closeButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                saveLocation()
                Companion.shell!!.close()
            }
        })


        //Finishing touches
        Companion.shell!!.layout()
        Companion.shell!!.pack()
        loadSearchesIntoCombos()
    }

    //Taken from V2. The boolean isFind is true when making this group for the Find section, and false for Replace.
    //Didn't want to introduce the "Advanced" enum system from v2 just for one method.
    private fun addStylesDropdown(parent: Group, isFind: Boolean) {
        val addStyleGroup = makeGroup(parent, 0, 3, false)
        addStyleGroup.text = SearchConstants.STYLES
        val notCheckbox = makeCheckBox(
            addStyleGroup, "Not"
        ) {}
        //Disable "NOT" checkbox if formatting replace -
        // it doesn't make sense to replace something with "Not Body Text" or "Not Bold"
        //Nor does it make sense to remove a style - you have to replace it with something else.
        if (!isFind) {
            notCheckbox.text = "" //Shrinks it a little?
            notCheckbox.selection = false
            notCheckbox.isEnabled = false
            notCheckbox.isVisible = false
        }

        val styleComboViewer = ComboViewer(addStyleGroup, SWT.READ_ONLY or SWT.DROP_DOWN)
        styleComboViewer.contentProvider = ArrayContentProvider.getInstance()
        //Uses the localized display name for labels instead
        styleComboViewer.labelProvider = object : LabelProvider() {
            override fun getText(elm: Any): String {
                return getBanaStyles()[(elm as Style).name]
            }
        }

        val styleDefs =
            getInstance().controller.document.engine.styleDefinitions.styles

        val styles = ArrayList<Style>()
        for (s in styleDefs) {
            if (!(s.id.contains("internal") || s.id.contains("options"))
                && s.name != "Page" && !isContainerStyle(s.name)
                && s.name != "Poetic Stanza"
            ) { //Not an ideal way to filter it out - eventually want to do it when styles are loaded
                styles.add(s)
            }
        }

        //Neat auto-completed lambda expression
        val byDisplay = Comparator.comparing { o: Style -> getBanaStyles()[o.name] }
        styles.sortWith(byDisplay)
        styleComboViewer.add(*styles.toTypedArray())

        makePushButton(
            addStyleGroup, SearchConstants.ADD, 1
        ) {
            //Get the name of the item the selected index
            if (styleComboViewer.combo.selectionIndex > -1) {
                val name = (styleComboViewer.getElementAt(
                    styleComboViewer.combo.selectionIndex
                ) as Style).name
                val display = getDefault()[requireNotNull(
                    (styleComboViewer.getElementAt(
                        styleComboViewer.combo.selectionIndex
                    ) as Style).baseStyleName
                )]

                val sf = StyleFormatting(notCheckbox.selection, name, display)
                addToStyleList(isFind, sf, notCheckbox.selection)

                Companion.shell!!.redraw()
                Companion.shell!!.layout()
                Companion.shell!!.pack()
            }
        }
    }

    //Make the Text Attributes dropdown (Bold, Italics, etc)
    private fun addEmphasisDropdown(parent: Group, isFind: Boolean) {
        val addEmphasisGroup = makeGroup(parent, 0, 3, false)
        addEmphasisGroup.text = SearchConstants.TEXT_ATTRIBUTES
        val notCheckbox = if (isFind) {
            makeCheckBox(
                addEmphasisGroup, "Not"
            ) {}
        } else {
            //Change to "Remove" for Replace section - it makes better sense
            makeCheckBox(
                addEmphasisGroup, "Remove"
            ) {}
        }

        val emphasisCombo = makeComboDropdown(addEmphasisGroup).get()

        val emphasis: List<SearchFormatting> = sc.availableEmphasis.filterIsInstance<EmphasisFormatting>()

        for (entry in emphasis) {
            emphasisCombo.add(entry.toString())
        }

        makePushButton(
            addEmphasisGroup, SearchConstants.ADD, 1
        ) {
            //Only add something if it's within bounds
            if (emphasisCombo.selectionIndex > -1) {
                val ef = emphasisCombo.getItem(emphasisCombo.selectionIndex)
                addToEmphasisList(isFind, ef, notCheckbox.selection)
                Companion.shell!!.redraw()
                Companion.shell!!.layout()
                Companion.shell!!.pack()
            }
        }
    }

    private fun addContainersDropdown(parent: Group, isFind: Boolean) {
        val addContainersGroup = makeGroup(parent, 0, 3, false)
        addContainersGroup.text = "Containers"
        //TODO: Add to searchConstants
        val notCheckbox = if (isFind) {
            makeCheckBox(
                addContainersGroup, "Not"
            ) {}
        } else {
            //Change to "Remove" for Replace section - it makes better sense
            makeCheckBox(
                addContainersGroup, "Remove"
            ) {}
        }

        val containersCombo = makeComboDropdown(addContainersGroup).get()

        val styleDefs =
            getInstance().controller.document.engine.styleDefinitions.styles

        for (s in styleDefs) {
            if (isContainerStyle(s.name)) {
                containersCombo.add(s.name)
            }
        }

        makePushButton(
            addContainersGroup, SearchConstants.ADD, 1
        ) {
            //Only add something if it's within bounds
            if (containersCombo.selectionIndex > -1) {
                val cf = containersCombo.getItem(containersCombo.selectionIndex)
                addToContainersList(isFind, cf, notCheckbox.selection)
                Companion.shell!!.redraw()
                Companion.shell!!.layout()
                Companion.shell!!.pack()
            }
        }
    }

    private fun addToContainersList(isFind: Boolean, cfs: String, isNegated: Boolean) {
        val cf = ContainerFormatting(isNegated, cfs)
        val ncf = ContainerFormatting(!isNegated, cfs)
        if (isFind) {
            //If the find formatting list does not contain the current format or its negation...
            if (!(sc.containerFormattingContains(cf, sc.findContainerFormatting) ||
                        sc.containerFormattingContains(ncf, sc.findContainerFormatting))
            ) {
                sc.findContainerFormatting.add(cf)
                try {
                    findList!!.remove("")
                } catch (ignored: Exception) {
                }
                findList!!.add((if (isNegated) "Not " else "") + cfs)
            }
        } else {
            //Same thing, but for the replace lists
            if (!(sc.containerFormattingContains(cf, sc.replaceContainerFormatting) ||
                        sc.containerFormattingContains(ncf, sc.replaceContainerFormatting))
            ) {
                sc.replaceContainerFormatting.add(cf)
                try {
                    replaceList!!.remove("")
                } catch (ignored: Exception) {
                }
                replaceList!!.add((if (isNegated) "Remove " else "") + cfs)
            }
        }
    }

    private fun addToStyleList(isFind: Boolean, sf: StyleFormatting, isNegated: Boolean) {
        val sfs = sf.style //Style formatting name (unlocalized)
        val sfd = sf.display //Style formatting displayName (really more of a category name)
        val lc = getBanaStyles()[sfs] //Localized name

        val formatting = sc.getStyleFormatting(isNegated, sfs, sfd)
        val negatedFormatting = sc.getStyleFormatting(!isNegated, sfs, sfd)
        if (isFind) {
            //If the lists don't already contain the SearchFormat, add them.
            if (!(sc.styleFormattingContains(formatting, sc.findStyleFormatting)
                        || sc.styleFormattingContains(negatedFormatting, sc.findStyleFormatting))
            ) {
                sc.findStyleFormatting.add(formatting)

                //Add the localized name to the menu list
                try {
                    findList!!.remove("")
                } catch (ignored: Exception) {
                }
                findList!!.add((if (isNegated) "Not " else "") + lc)
            }
        } else {
            if (sc.replaceStyleFormatting == null) {
                //Only add a new formatting if there is no current style
                // checking the menu list for styles gets too ugly
                sc.replaceStyleFormatting = formatting
                try {
                    replaceList!!.remove("")
                } catch (ignored: Exception) {
                }
                replaceList!!.add((if (isNegated) "Remove " else "") + lc)
            }
        }
    }

    private fun addToEmphasisList(isFind: Boolean, s: String, isNegated: Boolean) {
        var s = s
        if (isNegated) {
            s = getNonNegated(s).trim { it <= ' ' }
        }
        val emphasisType = requireNotNull(getEmphasisType(s))
        val formatting = sc.getEmphasisFormatting(isNegated, emphasisType)
        if (isFind) {
            if (!sc.emphasisFormattingContains(formatting, sc.findEmphasisFormatting)) {
                sc.findEmphasisFormatting.add(formatting)

                try {
                    findList!!.remove("")
                } catch (ignored: Exception) {
                    //If it doesn't have an empty string, oh well
                }
                findList!!.add(formatting.toString())
            }
        } else {
            if (!sc.emphasisFormattingContains(formatting, sc.replaceEmphasisFormatting)) {
                sc.replaceEmphasisFormatting.add(formatting)

                try {
                    replaceList!!.remove("")
                } catch (ignored: Exception) {
                }
                var fs = getNonNegated(formatting.toString())
                if (isNegated) {
                    fs = "Remove $fs"
                }
                replaceList!!.add(fs)
            }
        }
    }

    private fun getNegated(s: String): String {
        if (isNot(s)) {
            return s
        }
        return ("Not $s")
    }

    private fun isNot(s: String): Boolean {
        val index = s.indexOf("Not")
        return index >= 0
    }

    //Trim "NOT" from the string prefix
    private fun getNonNegated(s: String): String {
        if (!isNot(s)) {
            return s
        }
        return s.substring(("Not ").length)
    }

    private fun isValidClick(click: Click): Boolean {
        return click.settings.findHasAttributes() || click.settings.findHasText()
    }

    //For making the lists of formatting styles under the Find & Replace search fields.
    private fun makeList(parent: Group): org.eclipse.swt.widgets.List {
        val list = org.eclipse.swt.widgets.List(parent, SWT.READ_ONLY or SWT.SINGLE or SWT.BORDER)
        val gdF = GridData(SWT.FILL, SWT.LEFT, true, true, 1, 1)
        list.layoutData = gdF
        list.add("") //Shrinks it!
        return list
    }

    private fun replace() {
        if (findCombo!!.text.isEmpty() && replaceCombo!!.text.isNotEmpty()) {
            SearchNotices().invalidMessage(parent, "Cannot perform replace with empty find criteria!")
            return
        }

        m = rightManager
        m!!.waitForFormatting(true)

        //SearchController.logIt("done waiting for formatting");
        val index = m!!.text.view.caretOffset
        val tme = m!!.mapList.getClosest(index, true)
        val section = m!!.getSection(tme)
        val replaceName = removeBlackBox(replaceCombo!!.text).ifEmpty {
            ""
        }

        sc.replaceString = replaceName
        sc.findString = removeBlackBox(findCombo!!.text)

        val state = ViewState(index, section, tme)
        click = Click(sc, state, true)
        if (!isValidClick(click!!)) {
            return
        }

        val replaced = replaceObj!!.replace(m!!, click!!)

        if (!replaced && inTable(m!!)) {
            displayInvalidTableMessage(m!!.wpManager.shell)
        } else if (!replaced && (index == 0 && section == 0)) {
            SearchNotices().wordNotFoundMessage(parent, findCombo)
        } else if (!replaced) {
            SearchNotices().endOfDoc(parent, "beginning", m!!)
        } else { //Success
            Companion.shell!!.setFocus()
        }
    }

    private fun replaceAll() {
        m = rightManager
        m!!.waitForFormatting(true)

        if (findCombo!!.text.isEmpty() && replaceCombo!!.text.isNotEmpty()) {
            SearchNotices().invalidMessage(parent, "Cannot perform replace with empty find criteria!")
            return
        }
        val replaceName = if (removeBlackBox(replaceCombo!!.text).isNotEmpty()) {
            removeBlackBox(replaceCombo!!.text)
        } else if (!sc.replaceHasAttributes()) {
            ""
        } else {
            removeBlackBox(findCombo!!.text)
        }

        //Need a viewState here; null Clicks don't work
        val index = m!!.text.view.caretOffset
        val tme = m!!.mapList.getClosest(index, true)
        val section = m!!.getSection(tme)
        val before = ViewState(index, section, tme)
        sc.replaceString = replaceName
        sc.findString = removeBlackBox(findCombo!!.text)
        click = Click(sc, before, true)
        if (!isValidClick(click!!)) {
            return
        }

        val sr = SearchController(m!!, click!!)

        val numberReplaceAlls = sr.replaceAll()

        if (numberReplaceAlls == -1 && sr.tableDoubles < 1) {
            SearchNotices().wordNotFoundMessage(parent, findCombo)
        }
        if (numberReplaceAlls > 0 || sr.tableDoubles > 0) {
            SearchNotices().replaceAllMessage(parent, numberReplaceAlls, sr.tableDoubles, findCombo)
        }
    }

    /**
     * converts the stored string with the non breaking space into the string
     * with the black box that the user sees
     */
    private fun addBlackBox(string: String): String {
        return if (string.contains(NB_SPACE)) {
            string.replace(NB_SPACE.toRegex(), BLACK_BOX)
        } else {
            string
        }
    }

    /**
     * converts the string with the black box that the user sees into the stored
     * string with the non breaking space
     */
    private fun removeBlackBox(string: String): String {
        return if (string.contains(BLACK_BOX)) {
            string.replace(BLACK_BOX.toRegex(), NB_SPACE)
        } else {
            string
        }
    }

    val shell: Shell?
        get() = Companion.shell

    private fun search() {
        //SearchController.logIt("search clicked");
        m = rightManager
        m!!.waitForFormatting(true)

        //SearchController.logIt("done waiting for formatting");
        val index = m!!.text.view.caretOffset
        val tme = m!!.mapList.getClosest(index, true)
        val section = m!!.getSection(tme)
        val before = ViewState(index, section, tme)
        if (m!!.text.currentElement == null) {
            m!!.text.setCurrentElement(tme.getStart(m!!.mapList))
        }
        sc.findString = removeBlackBox(findCombo!!.text)
        sc.replaceString = null
        click = Click(sc, before, false)
        if (!isValidClick(click!!)) {
            return
        }

        val found = findObj!!.find(m!!, click!!)

        if (!found && (index == 0 && section == 0)) {
            SearchNotices().wordNotFoundMessage(parent, findCombo)
        } else if (!found) {
            SearchNotices().endOfDoc(parent, "beginning", m!!)
        } else {
            Companion.shell!!.setFocus()
        }
    }

    fun repeatLastSearch() {
        //Does anyone actually use this feature?
        if (m == null) {
            return
        }
        m!!.waitForFormatting(true)
        click = lastMemory
        if (click == null) {
            notify(SearchConstants.NO_SEARCHES_IN_MEMORY, Notify.ALERT_SHELL_NAME)
            return
        }
        click!!.initialView.cursorOffset = m!!.textView.caretOffset
        click!!.initialView.mapElement = m!!.mapList.getClosest(click!!.initialView.cursorOffset, true)
        click!!.initialView.section = m!!.getSection(click!!.initialView.mapElement)
        if (m!!.text.currentElement == null) {
            m!!.text.setCurrentElement(click!!.initialView.mapElement.getStart(m!!.mapList))
        }

        if (click!!.isReplace) {
            replace()
        } else {
            search()
        }
    }

    private fun loadSearchesIntoCombos() {
        //Doesn't seem to work...
        findCombo!!.removeAll()
        for (i in 0 until findSize()) {
            findCombo!!.add(addBlackBox(SavedSearches.findSavedSearches[i]), 0)
        }
        if (SavedSearches.findSavedSearches.isNotEmpty()) {
            findCombo!!.text = addBlackBox(
                SavedSearches.findSavedSearches[SavedSearches.findSavedSearches.size - 1]
            )
        }

        replaceCombo!!.removeAll()
        for (i in 0 until replaceSize()) {
            replaceCombo!!.add(addBlackBox(SavedSearches.replaceSavedSearches[i]), 0)
        }
        if (SavedSearches.replaceSavedSearches.isNotEmpty()) {
            replaceCombo!!.text = addBlackBox(
                SavedSearches.replaceSavedSearches[SavedSearches.replaceSavedSearches.size - 1]
            )
        }
    }

    companion object {
        private var shell: Shell? = null
        private const val NB_SPACE = "\u00a0" //Nonbreaking space
        private const val BLACK_BOX = "\u25a0" //Literally the black square unicode char
    }
}
