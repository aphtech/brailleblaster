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
package org.brailleblaster.tools

import nu.xom.Element
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.views.wp.TextView
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.utils.localization.LocaleHandler
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.SharedItem
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.internal.xml.splitNode
import org.brailleblaster.utils.xml.BB_NS
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Dialog
import org.eclipse.swt.widgets.Group
import org.eclipse.swt.widgets.List
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.TabFolder
import org.eclipse.swt.widgets.TabItem
import org.slf4j.LoggerFactory


private val localeHandler = LocaleHandler.getDefault()

class InsertLinkTool(parent: Manager) : Dialog(parent.wpManager.shell, SWT.NONE), MenuToolModule {
  override val topMenu: TopMenu = TopMenu.INSERT
  override val title: String = localeHandler["InsertLink"]
  override val accelerator: Int = SWT.MOD1 or 'K'.code
  override val sharedItem: SharedItem = SharedItem.INSERT_LINK

  private val logger = LoggerFactory.getLogger(TextView::class.java)

  override fun onRun(bbData: BBSelectionData) {
    //Get input from a simple dialog box
    val current = bbData.manager.mapList.current
    //If a link already exists in the selection, fill the dialog box with that link
    //Only applies to external links - internal links are handled in separate menu tab
    var linkText = ""
    val el = current.nodeParent
    var isLink = false
    var startInternal = false
    if (el != null && BBX.INLINE.LINK.isA(el)) {
      if (el.getAttributeValue("external", BB_NS).toBoolean()) {
        //If it's a Link BBX, get the href attribute and set linkText to that
        //If it's not, leave linkText as an empty string
        linkText = el.getAttributeValue("href", BB_NS)
      }
      else{
        startInternal = true
      }
      isLink = true
    }
    //In lieu of a tool disable mechanism, just don't open the dialog if there's no selection and no existing link
    if (bbData.manager.simpleManager.currentSelection.isTextNoSelection && !isLink) {
      return
    }

    makeGUI(bbData, linkText, startInternal)
  }

  private fun makeGUI(bbData: BBSelectionData, linkText: String, startInternal: Boolean = false) {
    val shell = Shell(parent.shell)
    shell.text = localeHandler["InsertLink"]
    shell.layout = FillLayout()

    val tabs = TabFolder(shell, SWT.NONE)
    val tabExternal = TabItem(tabs, SWT.NONE)
    tabExternal.text = "External Links"
    val tabInternal = TabItem(tabs, SWT.NONE)
    tabInternal.text = "Internal Links"

    val externalTabGroup = Group(tabs, SWT.NONE)
    externalTabGroup.layout = GridLayout(2, false)
    val internalTabGroup = Group(tabs, SWT.NONE)
    internalTabGroup.layout = GridLayout(2, false)
    val internalListGroup = Group(internalTabGroup, SWT.NONE)
    internalListGroup.layout = GridLayout(1, false)
    val internalButtonsGroup = Group(internalTabGroup, SWT.NONE)
    internalButtonsGroup.layout = GridLayout(1, false)

    val entryText = EasySWT.makeText(externalTabGroup, 180, 2)
    entryText.text = linkText
    entryText.message = "Enter URL, email, or file link here"
    EasySWT.addEnterListener(entryText){
      //Insert the link at the current selection (same behavior as pressing the button)
      if (entryText.text.isNotEmpty()) {
        insertLink(entryText.text, true, bbData)
      }
      shell.close()
    }
    EasySWT.makePushButton(externalTabGroup, localeHandler["InsertLink"], 1) {
      //Insert the link at the current selection (same behavior as pressing Enter in the text box)
      insertLink(entryText.text, true, bbData)
      shell.close()
    }
    EasySWT.makePushButton(externalTabGroup, localeHandler["removeLink"], 1) {
      removeLink(bbData)
      shell.close()
    }
    EasySWT.makePushButton(externalTabGroup, localeHandler["buttonCancel"], 1) {
      shell.close()
    }
    tabExternal.control = externalTabGroup

    val gd1 = GridData()
    gd1.widthHint = 180
    gd1.heightHint = 90
    gd1.grabExcessVerticalSpace = true
    gd1.grabExcessHorizontalSpace = true

    val bookmarkList = List(internalListGroup, SWT.BORDER or SWT.V_SCROLL or SWT.SINGLE)
    //Set list with names of bookmarks in document; subsequent buttons will use the selected bookmark
    getBookmarksList(bbData).forEach {
      bookmarkList.add(it)
    }
    bookmarkList.layoutData = gd1

    EasySWT.makePushButton(internalButtonsGroup, "Set Internal Link", 1) {
      //Set a link pointer at the current block based on the selected bookmark
      if (bookmarkList.selectionIndex != -1){ // Ensure something is selected
        val bookmarkID = bookmarkList.selection[0] // List is single selection, so it should always be index 0
        insertLink(bookmarkID, false, bbData)
      }
      shell.close()
    }
    EasySWT.addEnterListener(bookmarkList){
      //Set a link pointer at the current block based on the selected bookmark
      if (bookmarkList.selectionIndex != -1){ // Ensure something is selected
        val bookmarkID = bookmarkList.selection[0] // List is single selection, so it should always be index 0
        insertLink(bookmarkID, false, bbData)
      }
      shell.close()
    }

    EasySWT.makePushButton(internalButtonsGroup, "Remove Internal Link", 1) {
      //Works the same for internal and external links
      removeLink(bbData)
      shell.close()
    }
    EasySWT.makePushButton(internalButtonsGroup, localeHandler["buttonCancel"], 1) {
      shell.close()
    }
    tabInternal.control = internalTabGroup

    EasySWT.addEscapeCloseListener(shell)

    if (startInternal) {
      tabs.setSelection(tabInternal)
    }

    shell.pack()
    shell.open()
  }

  private fun insertLink(link: String, isExternal: Boolean, bbData: BBSelectionData) {
    logger.info("Inserting link text: $link; isExternal: $isExternal")
    val mapList = bbData.manager.mapList
    val current = mapList.current

    if (current.nodeParent != null && BBX.INLINE.LINK.isA(current.nodeParent)) {
      //Currently on an existing link - modify it
      //Process is same for internal and external links - the external attribute has already been set
      //println("Modifying existing link")
      if (link.isNotEmpty()) { //Might want to validate the link format here
        current.nodeParent.addAttribute(BBX.INLINE.LINK.ATTRIB_HREF.newAttribute(link))
        bbData.manager.simpleManager.dispatchEvent(ModifyEvent(Sender.TEXT, false, current.nodeParent))
      }
      return
    }
    else {
      //Not on a link - create one if there's a valid selection
      if (current.isReadOnly || bbData.manager.simpleManager.currentSelection.isTextNoSelection) {
        //Do not allow link creation in read-only areas or if there's no selection
        logger.debug("No selection or read-only area - not creating link")
        return
      }
      if (!bbData.manager.simpleManager.currentSelection.isSingleNode) {
        logger.debug("Multiple nodes selected - cannot create link")
        return
      }

      val newLink = BBX.INLINE.LINK.create()
      newLink.addAttribute(BBX.INLINE.LINK.IS_EXTERNAL.newAttribute(isExternal))
      newLink.addAttribute(BBX.INLINE.LINK.ATTRIB_HREF.newAttribute(link))

      val currentNode = bbData.manager.simpleManager.currentCaret.node

      if (currentNode !is Text) {
        logger.debug("Current node is not text - cannot create link")
        return
      }

      val nodeLength = currentNode.value.length
      val start = (bbData.manager.simpleManager.currentSelection.start as XMLTextCaret).offset
      val end = (bbData.manager.simpleManager.currentSelection.end as XMLTextCaret).offset
      //Mark had problems in a Nimas file too. Maybe too many blocks to handle?
      logger.info("Adding link to Node ${currentNode.value}, length: $nodeLength, start: $start, end: $end")

      //We want either the whole node, or a portion of it.
      //That portion might be from the start to somewhere in the middle, two middle points, or middle to end.
      //So 4 cases, and the return size of splitTextNode varies accordingly.
      val nodeToWrap =
        if (start > 0 && end != -1 && end != nodeLength) {
          //get middle and wrap (two middle points)
          val splitTextNode = currentNode.splitNode(start, end)
          splitTextNode[1]
        } else if (start > 0) {
          //get last and wrap (middle to very end of node)
          val splitTextNode = currentNode.splitNode(start)
          splitTextNode[1]
        } else if (end != -1 && end != nodeLength) {
          //get beginning and wrap (start to middle)
          val splitTextNode = currentNode.splitNode(end)
          splitTextNode[0]
        } else {
          //wrap all (start to very end)
          currentNode
        }
      XMLHandler.wrapNodeWithElement(nodeToWrap, newLink)
      bbData.manager.simpleManager.dispatchEvent(ModifyEvent(Sender.TEXT, true, nodeToWrap.parent))
    }
    return
  }

  //Generate the strings for the SWT List
  private fun getBookmarksList(bbData: BBSelectionData): kotlin.collections.List<String> {
    var elementStrings = listOf<String>()
    try {
      val xpath = """//*[@*[local-name() = 'linkID']]"""
      val results = bbData.manager.simpleManager.doc.query(xpath).toList()
      elementStrings = results.map {
        val el = it as Element
        el.getAttributeValue("linkID", BB_NS).toString()
      }
    }
    catch (e: Exception) {
      e.printStackTrace()
    }

    return elementStrings
  }

  private fun removeLink(bbData: BBSelectionData) {
    val mapList = bbData.manager.mapList
    val current = mapList.current
    if (current.nodeParent != null && BBX.INLINE.LINK.isA(current.nodeParent)) {
      //Wow, this part was way easier than I thought it would be
      XMLHandler.unwrapElement(current.nodeParent)
      bbData.manager.simpleManager.dispatchEvent(ModifyEvent(Sender.TEXT, false, current.nodeParent))
    }
  }

}
