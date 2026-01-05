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
package org.brailleblaster.math.ascii

import nu.xom.Node
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.math.ascii.MathDialogSettings.CATEGORIES
import org.brailleblaster.math.mathml.ImageCreator
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.mathml.MathSubject
import org.brailleblaster.math.mathml.MathUtils
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.views.wp.MathEditHandler
import org.brailleblaster.utils.swt.AccessibilityUtils.setName
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.util.Notify
import org.brailleblaster.utils.swt.SizeAndLocation
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.browser.Browser
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.VerifyEvent
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import java.util.*

class ASCIIMathEditorDialog(m: Manager) {
  private val buttonImageWidth = 270
  private val buttonImageHeight = 90
  private val buttonTextWidth = 90
  private val buttonTextHeight = 45
  private val fontScaleMax = 30
  private val fontScaleMin = 9
  private var fontScale = fontScaleMin
  private var panelWidth = 0
  private var panelHeight = 0
  private var textBox: StyledText? = null
  private var browser: Browser? = null
  var settings = MathDialogSettings()
  private var searchTextBox: Text? = null

  init {
    settings.loadSettings()
    open()
    putAttributeInTextBox(m.simpleManager.currentCaret.node)
  }

  fun open() {
      val m = WPManager.getInstance().controller
      if (shell == null || shell!!.isDisposed) {
          shell = Shell(m.display.activeShell, SWT.DIALOG_TRIM)
      } else {
          sizeLocation = EasySWT.saveLocation(shell!!)
          val length = shell!!.children.size
          for (i in 0 until length) {
              shell!!.children[0].dispose()
          }
      }
      shell!!.text = MathModuleUtils.ASCII_EDITOR
      shell!!.layout = GridLayout(2, false)
      if (settings.isFullScreen) {
          EasySWT.setFullScreen(shell!!)
      } else if (sizeLocation != null) {
          EasySWT.setSizeAndLocation(shell!!, sizeLocation!!)
      } else {
          EasySWT.setSizeAndLocationHalfScreen(shell!!)
      }
      determineDialogProportions()
      val menuBar = Menu(shell, SWT.BAR)
      val settingsFileMenu = MenuItem(menuBar, SWT.CASCADE)
      settingsFileMenu.text = SETTINGS
      addSettingsDropDown(shell, settingsFileMenu, menuBar)
      val leftPanel = EasySWT.makeGroup(shell, 0, 1, true)
      val rightPanel = EasySWT.makeGroup(shell, 0, 1, true)
      makeCombo(leftPanel)
      makeButtons(leftPanel)
      makeBrowserView(rightPanel)
      makeTextBox(rightPanel)

      val options = Group(rightPanel, SWT.NONE)
      val optionsGrid = GridData(4, 4, true, true)
      options.data = optionsGrid
      options.layout = GridLayout(5, false)

      val apply = Button(options, SWT.PUSH)
      apply.text = INSERT_MATH
      val grid = GridData(4, 4, true, true)
      apply.data = grid
      EasySWT.addSelectionListener(apply) { it: SelectionEvent -> insert() }

      val replace = Button(options, SWT.PUSH)
      replace.text = REPLACE_CURRENT_MATH
      replace.data = grid
      EasySWT.addSelectionListener(replace) { it: SelectionEvent -> replace() }

      val previous = Button(options, SWT.PUSH)
      previous.text = PREVIOUS_MATH
      previous.data = grid
      EasySWT.addSelectionListener(previous) { it: SelectionEvent -> previous() }

      val next = Button(options, SWT.PUSH)
      next.text = NEXT_MATH
      next.data = grid
      EasySWT.addSelectionListener(next) { it: SelectionEvent -> next() }

      val clear = Button(options, SWT.PUSH)
      clear.text = CLEAR_MATH
      clear.data = grid
      EasySWT.addSelectionListener(clear) { it: SelectionEvent -> clear() }
      EasySWT.addEscapeCloseListener(shell!!)

      val fontControls = Group(rightPanel, SWT.NONE)
      val fontControlsGrid = GridData(3, 1, false, false)
      fontControls.data = fontControlsGrid
      fontControls.layout = GridLayout(3, false)

      val fontSizeLabel = Label(fontControls, SWT.NONE)
      fontSizeLabel.text = "Font Size: "

      val fontPlus = Button(fontControls, SWT.PUSH)
      fontPlus.text = " + "
      EasySWT.addSelectionListener(fontPlus) {
          if (fontScale < fontScaleMax) {
              fontScale++
          }
          shell!!.close()
          open()
      }

      val fontMinus = Button(fontControls, SWT.PUSH)
      fontMinus.text = " - "
      EasySWT.addSelectionListener(fontMinus) {
          if (fontScale > fontScaleMin) {
              fontScale--
          }
          shell!!.close()
          open()
      }

      EasySWT.addEscapeCloseListener(shell!!)

      shell!!.pack()
      shell!!.layout(true)
      shell!!.setSize(panelWidth * 3, panelHeight * 3)
      shell!!.open()
      textBox!!.setFocus()
      if (settings.currentName == CATEGORIES.SEARCH) {
          setSearchBox()
      }
  }

  private fun clear() {
    settings.currentAscii = ""
    textBox!!.text = ""
  }

  private fun setSearchBox() {
    if (searchTextBox != null && !searchTextBox!!.isDisposed) {
      searchTextBox!!.text = settings.lastSearch
    }
  }

  private fun makeSearchBox(leftPanel: Group) {
    val g = Group(leftPanel, SWT.NONE)
    val gl = GridLayout(3, true)
    g.layout = gl
    g.layoutData = GridData(SWT.FILL, SWT.NONE, false, false)
    EasySWT.makeLabel(g, SEARCH, 1)
    searchTextBox = Text(g, SWT.WRAP or SWT.BORDER)

    EasySWT.makePushButton(g, OK, 1) {
      val searchText = searchTextBox!!.text
      //Clear before setting new results
      settings.currentAscii = ""
      settings.lastSearch = ""
      settings.currentCategory!!.array.clear()
      search(searchText)
      searchTextBox!!.text = ""
    }

    val searchBox = " ".repeat(SEARCH_BOX_LENGTH * 2)
    searchTextBox!!.text = searchBox.trimIndent()

    searchTextBox!!.addKeyListener(object : KeyListener {
      override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == SWT.CR.code) {
          val searchText = searchTextBox!!.text
          //Clear before setting new results
          settings.currentAscii = ""
          settings.lastSearch = ""
          settings.currentCategory!!.array.clear()
          search(searchText)
          searchTextBox!!.text = ""
        }
      }

      override fun keyReleased(e: KeyEvent) {}
    })
  }

  private fun determineDialogProportions() {
    val width = shell!!.bounds.width
    val height = shell!!.bounds.height
    panelWidth = width / 4
    panelHeight = height / 4
  }

  private fun makeButtons(leftPanel: Group) {
    if (settings.currentName == CATEGORIES.SEARCH) {
      makeSearchBox(leftPanel)
    }
    val outerContainer = Composite(leftPanel, SWT.BORDER)
    outerContainer.layout = GridLayout(1, false)
    outerContainer.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
    val sc = ScrolledComposite(outerContainer, SWT.V_SCROLL or SWT.H_SCROLL or SWT.BORDER)
    sc.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
    sc.expandVertical = true
    sc.expandHorizontal = true
    sc.alwaysShowScrollBars = true
    val innerContainer = Composite(sc, SWT.BORDER)
    innerContainer.layout = GridLayout(6, false)
    innerContainer.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
    val entrySize = settings.currentCategory!!.array.size
    if (entrySize == 0) {
      val t = EasySWT.makeText(innerContainer, 1)
      t.text = NO_SEARCH_RESULTS
      t.editable = false
    } else {
      // make buttons with Unicode chars
      val imagesMap = settings.currentCategory!!.array.associateWith { MathUtils.getImageFromMMLDoc(it.name) }
      for ((entry, image) in imagesMap) {
        if (image == null) continue
        val im = Image(Display.getCurrent(), buttonImageWidth, buttonImageHeight)
        val g = GC(im)
        val xSpace = (buttonImageWidth - image.bounds.width) / 2
        val ySpace = (buttonImageHeight - image.bounds.height) / 2
        g.drawImage(
          image, 0, 0, image.bounds.width, image.bounds.height, xSpace, ySpace,
          image.bounds.width, image.bounds.height
        )
        g.dispose()
        val button = Button(innerContainer, SWT.NONE)
        val data = GridData()
        data.horizontalSpan = 2
        button.layoutData = data
          EasySWT.addSelectionListener(button) { it: SelectionEvent ->
              settings.currentAscii = entry.entry
              updateTextAndBrowser()
          }
          button.image = im
        addAltText(entry, button)
        addHoverText(entry, button)
      }
      for ((entry, image) in imagesMap) {
        if (image != null) continue
        val button = Button(innerContainer, SWT.NONE or SWT.CENTER)
        val data = GridData()
        data.horizontalAlignment = GridData.FILL
        button.layoutData = data
          EasySWT.addSelectionListener(button) { it: SelectionEvent ->
              settings.currentAscii = entry.entry
              updateTextAndBrowser()
          }
          val fontdata = button.font.fontData
        fontdata[0].setHeight(fontScale * 2)
        val font = Font(Display.getCurrent(), fontdata)
        button.font = font
        button.text = addButtonTextPadding(entry.see)
        button.setSize(buttonTextWidth, buttonTextHeight)
        addAltText(entry, button)
        addHoverText(entry, button)
      }
    }
    sc.content = innerContainer
    sc.setMinSize(innerContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT))
  }

  private fun addButtonTextPadding(see: String): String {
    val width = 8
    if (see.length > width) {
      return see
    }
    val padding = width - see.length
    val leftPadding = padding / 2
    val rightPadding = padding - leftPadding
    return " ".repeat(leftPadding) + see + " ".repeat(rightPadding)
  }

  private fun addHoverText(entry: ASCIIEntry, button: Button) {
    button.toolTipText = entry.name
  }

  private fun addAltText(entry: ASCIIEntry, button: Button) {
    setName(button, entry.name)
  }

  private fun insert() {
    MathEditHandler.insertNew(MathSubject(settings.currentAscii))
  }

  private fun replace() {
    MathEditHandler.replaceMathAtCursor(MathSubject(textBox!!.text))
  }

  private fun previous() {
    val m = WPManager.getInstance().controller
    val startNode = m.simpleManager.currentSelection.start.node
    val node = MathUtils.previous(startNode, m)
    if (node != null) {
      putAttributeInTextBox(node)
      updateBrowser()
      updateTextView(node, m)
    } else {
      Notify.notify(BEGINNING_OF_MATH_WARNING, Notify.ALERT_SHELL_NAME)
    }
  }

  private operator fun next() {
    val m = WPManager.getInstance().controller
    val startNode = m.simpleManager.currentSelection.end.node
    val node = MathUtils.next(startNode, m)
    if (node != null) {
      putAttributeInTextBox(node)
      updateBrowser()
      updateTextView(node, m)
    } else {
      Notify.notify(END_OF_MATH_WARNING, Notify.ALERT_SHELL_NAME)
    }
  }

  private fun updateTextView(startNode: Node, m: Manager?) {
    val startSection = 0
    for (i in startSection until m!!.sectionList.size) {
      val section = m.sectionList[i]
      val list = section.list
      val listSize = list.size
      for (j in 0 until listSize) {
        val mapElement = list[j]
        if (mapElement.node != null && mapElement.node == startNode) {
          if (!section.isVisible) {
            m.useResetSectionMethod(i)
            m.mapList.setCurrent(0)
            m.waitForFormatting(true)
          }
          m.textView.topIndex = m.textView.getLineAtOffset(mapElement.getStart(m.mapList))
          val index = m.mapList.indexOf(mapElement)
          if (index != -1) {
            //because it's the maplist, who knows
            m.mapList.setCurrent(index)
          }
          m.text.highlight(mapElement.getStart(m.mapList), mapElement.getEnd(m.mapList))
          return
        }
      }
    }
  }

  private fun updateTextAndBrowser() {
    updateBrowser()
    insertIntoTextBox(settings.currentAscii)
  }

  private fun updateBrowser() {
    ImageCreator.updateBrowser(browser, settings.currentAscii)
  }

  private fun makeBrowserView(rightPanel: Group) {
    browser = ImageCreator.makeBrowserView(rightPanel, settings.currentAscii, panelWidth, panelHeight)
  }

  private fun putAttributeInTextBox(startNode: Node?) {
    textBox!!.text = if (startNode != null && MathModuleUtils.isMath(startNode)) {
      MathModuleUtils.getMathText(startNode)
    } else{
      ""
    }
  }

  private fun makeTextBox(rightPanel: Group) {
    val outer = ScrolledComposite(rightPanel, SWT.V_SCROLL or SWT.H_SCROLL)
    outer.data = GridData(SWT.FILL, SWT.FILL, true, true)
    outer.layout = GridLayout(1, true)
    outer.setMinSize(panelWidth, panelHeight)
    outer.expandHorizontal = true
    outer.expandVertical = true
    textBox = StyledText(outer, SWT.BORDER or SWT.V_SCROLL or SWT.H_SCROLL).apply {
        data = GridData(4, 4, true, true)
        setSize(panelWidth, panelHeight)
    }
    outer.content = textBox
    val fontdata = textBox!!.font.fontData
    fontdata[0].setHeight(fontScale * 2)
    val font = Font(Display.getCurrent(), fontdata)
    textBox!!.font = font
    textBox!!.addVerifyKeyListener { event: VerifyEvent ->
      if (event.keyCode == SWT.CR.code || event.keyCode == SWT.TAB.code) {
        event.doit = false
      } else if (event.keyCode == SWT.DEL.code || event.keyCode == SWT.BS.code
        && textBox != null && !textBox!!.isDisposed && textBox!!.text.length == 1) {
        // last char was deleted, so it won't set off the modify
        // listener
        extractText()
        updateBrowser()
      }
    }
    textBox!!.addModifyListener {
      extractText()
      updateBrowser()
    }
    textBox!!.addKeyListener(object : KeyListener {
      //For Ctrl-A
      override fun keyPressed(e: KeyEvent) {
        if (e.stateMask == SWT.CTRL && e.keyCode == 'a'.code) {
          textBox!!.selectAll()
        }
      }

      override fun keyReleased(e: KeyEvent) {}
    })
    insertIntoTextBox(settings.currentAscii)
  }

  private fun addSettingsDropDown(shell: Shell?, settingsFileMenu: MenuItem, menuBar: Menu) {
    val fileMenu = Menu(shell, SWT.DROP_DOWN)
    settingsFileMenu.menu = fileMenu
    val full = MenuItem(fileMenu, SWT.CHECK)
    full.text = FULL_SCREEN
    shell!!.menuBar = menuBar
    full.selection = settings.isFullScreen
    full.addListener(SWT.Selection) {
      settings.isFullScreen = !settings.isFullScreen
      full.selection = settings.isFullScreen
      settings.saveSettings()
      shell.close()
      open()
    }
  }

  private fun makeCombo(leftPanel: Group) {
    val g = Group(leftPanel, SWT.NONE)
    g.layout = GridLayout(2, false)
    g.layoutData = GridData(0, 0, false, false)
    EasySWT.makeLabel(g, CATEGORY_LABEL, 1)
    val categories = settings.categories
    categories.sortWith(ASCIILoadJAXB.Comparators.CATEGORY)
    val combo = Combo(g, SWT.DROP_DOWN or SWT.READ_ONLY)
    combo.data = GridData(SWT.FILL, SWT.NONE, true, true)
    combo.setItems(*settings.categoriesEnumStringArray)
      EasySWT.addSelectionListener(combo) { it: SelectionEvent ->
          extractText()
          settings.currentName = settings.getCategoryPrettyName(combo.text)!!
          settings.saveSettings()
          open()
      }
      combo.select(combo.indexOf(settings.currentName.prettyName))
  }

  private fun getSearchResults(text: String): ArrayList<ASCIIEntry> {
    val results = ArrayList<ASCIIEntry>()
    val categories = settings.categories
    for (i in categories.indices) {
      val array = categories[i].array
      for (j in array.indices) {
        if (array[j].name.lowercase(Locale.getDefault()).contains(text.lowercase(Locale.getDefault()))
          || array[j].entry.lowercase(Locale.getDefault()).contains(text.lowercase(Locale.getDefault()))
        ) {
          results.add(array[j])
        }
      }
    }
    return results
  }

  private fun search(text: String) {
    extractText()
    settings.lastSearch = searchTextBox!!.text
    val results = getSearchResults(text)
    settings.currentName = CATEGORIES.SEARCH
    settings.currentCategory!!.array = results
    open()
  }

  private fun insertIntoTextBox(txt: String) {
    var text = txt
    text = text.replace("\n".toRegex(), "")
    val offset = textBox!!.caretOffset
    textBox!!.insert(text)
    textBox!!.caretOffset = offset + text.length
  }

  private fun extractText() {
    settings.currentAscii = textBox!!.text
  }

  companion object {
    private val localeHandler = getDefault()
    private val SETTINGS = localeHandler["settings"]
    private val BEGINNING_OF_MATH_WARNING = localeHandler["beginningOfMathWarning"]
    private val END_OF_MATH_WARNING = localeHandler["endOfMathWarning"]
    private val FULL_SCREEN = localeHandler["fullScreen"]
    private val NEXT_MATH = localeHandler["nextMath"]
    private val PREVIOUS_MATH = localeHandler["previousMath"]
    @JvmField
    val REPLACE_CURRENT_MATH = localeHandler["replaceCurrentMath"]
    @JvmField
    val INSERT_MATH = localeHandler["insertMath"]
    val PLACEHOLDER = localeHandler["mathJaxPlaceHolder"]
      private var shell: Shell? = null
    private var sizeLocation: SizeAndLocation? = null
    private val SEARCH = localeHandler["search"]
    private val OK = localeHandler["lblOk"]
    val CATEGORY_LABEL = localeHandler["categoryLabel"]
    private val CLEAR_MATH = localeHandler["clear"]
    private const val SEARCH_BOX_LENGTH = 20
    private val NO_SEARCH_RESULTS = localeHandler["noSearchResults"]
  }
}