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

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.BBIni
import org.brailleblaster.bbx.BBX
import org.brailleblaster.frontmatter.VolumeUtils
import org.brailleblaster.frontmatter.VolumeUtils.VolumeData
import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.XMLNodeCaret
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.internal.xml.XMLHandler2
import org.brailleblaster.utd.properties.UTDElements.Companion.getByName
import org.brailleblaster.utd.utils.UTDHelper
import org.brailleblaster.exceptions.BBNotifyException
import org.brailleblaster.util.FormUIUtils
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Combo
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text
import org.slf4j.LoggerFactory

class GoToPageDialog(private val m: Manager) {
  @JvmField
  val dialog: Shell = FormUIUtils.makeDialog(m)
  private val pageText: Text
  val rawPageButton: Button

  @JvmField
  val braillePageButton: Button

  @JvmField
  val printPageButton: Button
  private val volumeSelect: Combo
  private var volumeData: List<VolumeData>? = null

  init {
    //------------------- Contents -----------------------
    dialog.text = "Go To"
    val dialogLayout = RowLayout()
    dialogLayout.center = true
    dialog.layout = dialogLayout
    volumeSelect = Combo(dialog, SWT.READ_ONLY or SWT.DROP_DOWN)
    pageText = Text(dialog, SWT.BORDER)
    printPageButton = Button(dialog, SWT.RADIO)
    printPageButton.text = "Print Page"
    braillePageButton = Button(dialog, SWT.RADIO)
    braillePageButton.text = "Braille Page"
    rawPageButton = Button(dialog, SWT.RADIO)
    rawPageButton.text = "Ordinal Page"
    val submitButton = Button(dialog, SWT.NONE)
    submitButton.text = "Go to"
    //		submitButton.setEnabled(false);

    //--------------------- Listeners -----------------------
    //Save user state
    FormUIUtils.addSelectionListener(printPageButton) { savePageType(PageType.PRINT) }
    FormUIUtils.addSelectionListener(braillePageButton) { savePageType(PageType.BRAILLE) }
    FormUIUtils.addSelectionListener(rawPageButton) { savePageType(PageType.ORDINAL) }
    FormUIUtils.addSelectionListener(volumeSelect) {
      val isVolumeSelected = volumeSelect.selectionIndex != 0
      printPageButton.isEnabled = !isVolumeSelected
      rawPageButton.isEnabled = !isVolumeSelected
      //Issue #4341: Default select braille page
      if (isVolumeSelected) {
        printPageButton.selection = false
        braillePageButton.selection = true
        rawPageButton.selection = false
      }
    }

    //Submit when user presses enter inside page field
    pageText.addSelectionListener(object : SelectionAdapter() {
      override fun widgetDefaultSelected(e: SelectionEvent) {
        onSubmit()
      }
    })
    FormUIUtils.addSelectionListener(submitButton) { onSubmit() }

    // ------------------------ data ----------------------
    val volumeElements = VolumeUtils.getVolumeElements(m.doc)
    if (volumeElements.isEmpty()) {
      log.trace("Disabling volume nav, no volumes found")
      volumeSelect.isEnabled = false
      volumeData = null
    } else {
      log.trace("Enabling volume nav, found " + volumeElements.size)
      volumeSelect.add("")
      volumeData = VolumeUtils.getVolumeNames(volumeElements).onEach { vol -> volumeSelect.add(vol.nameLong) }
    }
    val lastPageType = BBIni.propertyFileManager.getProperty(SETTING_PAGE_TYPE)
    if (lastPageType != null) {
      when (PageType.valueOf(lastPageType)) {
        PageType.PRINT -> printPageButton.selection = true
        PageType.BRAILLE -> braillePageButton.selection = true
        PageType.ORDINAL -> rawPageButton.selection = true
      }
    } else {
      printPageButton.selection = true
    }
    FormUIUtils.setLargeDialogSize(dialog)
    dialog.open()
    //Issue #4341: Default select text box as that's what user will be doing most of the time
    pageText.setFocus()
  }

  fun onSubmit() {
    m.waitForFormatting(true)
    if (volumeSelect.selectionIndex > 0) {
      scrollToVolume(volumeSelect.selectionIndex -  /*skip blank entry*/1, pageText.text)
      return
    } else if (pageText.text.isBlank()) {
      throw BBNotifyException("No text entered yet, ignoring")
    }
    val pageTypeText =
      if (printPageButton.selection) "print" else if (braillePageButton.selection) "braille" else "ordinal"
    val userPage = pageText.text
    log.debug("Executing Go To Page {}", userPage)
    val (_, value) = findPage(userPage)
      ?: throw BBNotifyException((pageTypeText.replaceFirstChar { it.titlecase() }) + " Page '" + userPage + "' not found")

    //Modules: Scroll to closest node
    scrollToTextAfterNode(value.node)
    m.textView.setFocus()
    dialog.close()
  }

  fun findPage(page: String): Pair<Int, TextMapElement>? {
    //Find the right text map element
    val pme: Pair<Int, TextMapElement>? = if (printPageButton.selection) {
      m.getPrintPageElement(page)
    } else if (braillePageButton.selection) {
      m.getBraillePageElementByUntranslatedPage(page, null)
    } else {
      try {
        //Normal people are 1-based
        val index = page.toInt() - 1
        m.getBraillePageElement(index)
      } catch (_: NumberFormatException) {
        log.warn("Page '{}' is not a number", page)
        return null
      }
    }
    return pme
  }

  fun scrollToVolume(index: Int, brlPage: String?) {
    log.debug("scrolling to volume $index")
    val nodeToSearchForText: Node = if (index < 0) {
      throw IllegalArgumentException("must be positive, $index")
    } else if (index == 0) {
      BBX.getRoot(m.doc)
    } else {
      val volumeInfo = volumeData!![index - 1]
      volumeInfo.element
    }
    scrollToTextAfterNode(nodeToSearchForText, brlPage)
    dialog.close()
    m.textView.setFocus()
  }

  fun scrollToTextAfterNode(nodeToSearchForText: Node) {
    scrollToTextAfterNode(nodeToSearchForText, null)
  }

  private fun scrollToTextAfterNode(nodeToSearchForText: Node, brlPage: String?) {
    if (brlPage.isNullOrBlank()) {
      log.trace("Scrolling to node: " + nodeToSearchForText.toXML())
      m.simpleManager.dispatchEvent(
        XMLCaretEvent(
          Sender.GO_TO_PAGE,
          if (nodeToSearchForText is nu.xom.Text) XMLTextCaret(nodeToSearchForText, 0) else XMLNodeCaret(
            nodeToSearchForText
          )
        )
      )
    } else {
      val textAfterVolume = FastXPath.descendantAndFollowing(nodeToSearchForText)
          .filterIsInstance<nu.xom.Text>()
        .filter { curNode: Node ->
            (curNode.value.isNotEmpty()
                    && XMLHandler.ancestorElementNot(
                curNode
            ) { curAncestor: Element ->
                if (BBX.BLOCK.VOLUME_END.isA(curAncestor) //Don't get text nodes inside utd elements
                    || getByName(curAncestor.localName) != null
                ) {
                    return@ancestorElementNot true
                }
                val brl = UTDHelper.getAssociatedBrlElement(curNode)
                if (brl != null) {
                    return@ancestorElementNot brl.childCount == 0
                }
                false
            }
                    &&
                    !MathModule.isMath(curNode))
        }.first()
      val (_, value) = m.getBraillePageElementByUntranslatedPage(brlPage, textAfterVolume)
        ?: throw BBNotifyException(
          "Braille page "
              + brlPage
              + " in " + volumeSelect.getItem(volumeSelect.selectionIndex)
              + " not found"
        )
      val node = value.node
      if (node is nu.xom.Text) {
        log.debug("scrolling to text node for braille page {} {}", brlPage, node)
        m.simpleManager.dispatchEvent(XMLCaretEvent(Sender.GO_TO_PAGE, XMLTextCaret(node, 0)))
      } else {
        log.debug(
            "scrolling to node for braille page $brlPage " + XMLHandler2.toXMLSimple(
            node
        )
        )
        m.simpleManager.dispatchEvent(XMLCaretEvent(Sender.GO_TO_PAGE, XMLNodeCaret(node)))
      }
    }
  }

  enum class PageType {
    PRINT, BRAILLE, ORDINAL
  }

  companion object {
    private val log = LoggerFactory.getLogger(GoToPageDialog::class.java)
    private const val SETTING_PAGE_TYPE = "goToPage.pageType"
    private fun savePageType(type: PageType) {
      BBIni.propertyFileManager.save(SETTING_PAGE_TYPE, type.name)
    }
  }
}