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

import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.utils.localization.LocaleHandler
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utils.xml.BB_NS
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Dialog
import org.eclipse.swt.widgets.Shell


private val localeHandler = LocaleHandler.getDefault()

class InsertLinkTool(parent: Shell) : Dialog(parent, SWT.NONE), MenuToolModule {
  override val topMenu: TopMenu = TopMenu.INSERT
  override val title: String = localeHandler["InsertLink"]
  override val accelerator: Int = SWT.MOD1 or 'K'.code

  override fun onRun(bbData: BBSelectionData) {
    //Get input from a simple dialog box
    val current = bbData.manager.mapList.current
    //Need to determine if the selection is valid for inserting a link
    //If a link already exists in the selection, fill the dialog box with that link
    var linkText = ""
    val el = current.nodeParent
    if (el != null && BBX.INLINE.LINK.isA(el)){
      linkText = el.getAttributeValue("href", BB_NS)
    }
    //If it's a Link BBX, get the href attribute and set linkText to that
    //If it's not, leave linkText as an empty string

    makeGUI(bbData, linkText)
  }

  private fun makeGUI(bbData: BBSelectionData, linkText: String) {
    val shell = Shell(
      parent.display,
      SWT.DIALOG_TRIM or SWT.RESIZE or SWT.CENTER
    )
    shell.text = localeHandler["InsertLink"]
    shell.layout = GridLayout(1, false)

    val inputGroup = EasySWT.makeGroup(shell, SWT.CENTER, 2, false)
    val entryText = EasySWT.makeText(inputGroup, 180, 2)
    entryText.text = linkText
    EasySWT.addEnterListener(entryText) {
      //Insert the link at the current selection
      val linkText = entryText.text
      insertExternalLink(linkText, bbData)
      shell.close()
    }

    val buttonsGroup = EasySWT.makeGroup(shell, SWT.CENTER, 3, true)
    EasySWT.makePushButton(buttonsGroup, localeHandler["InsertLink"], 1) {
      //Insert the link at the current selection (same behavior as pressing Enter in the text box)
      val linkText = entryText.text
      insertExternalLink(linkText, bbData)
      shell.close()
    }
    EasySWT.makePushButton(buttonsGroup, localeHandler["buttonCancel"], 1) {
      shell.close()
    }
    EasySWT.makePushButton(buttonsGroup, localeHandler["removeLink"], 1) {
      removeExternalLink(bbData)
      shell.close()
    }
    EasySWT.addEscapeCloseListener(shell)
    shell.pack()
    shell.open()
  }

  private fun insertExternalLink(link: String, bbData: BBSelectionData) {
    println("Inserting link text: $link")
    val mapList = bbData.manager.mapList
    val current = mapList.current

    if (current.nodeParent != null && BBX.INLINE.LINK.isA(current.nodeParent)) {
      //Currently on an existing link - modify it
      if (current.nodeParent.getAttributeValue("external", BB_NS).toBoolean()) {
        //Modify existing link
        println("Modifying existing link")
        if (link.isNotEmpty()) { //Might want to validate the link format here
          current.nodeParent.addAttribute(BBX.INLINE.LINK.ATTRIB_HREF.newAttribute(link))
          bbData.manager.simpleManager.dispatchEvent(ModifyEvent(Sender.TEXT, false, current.nodeParent))
        }
      } else {
        //Don't modify internal links...maybe perform this check earlier?
      }
      return
    } else {
      //Not on a link - create one if there's a valid selection
      if (current.isReadOnly || bbData.manager.simpleManager.currentSelection.isTextNoSelection) {
        //Do not allow link creation in read-only areas or if there's no selection
        //println("No selection or read-only area - not creating link")
        return
      }
      if (!bbData.manager.simpleManager.currentSelection.isSingleNode){
        //println("Multiple nodes selected - cannot create link")
        return
      }

      //println("Single node selected - creating link")
      val newLink = BBX.INLINE.LINK.create()
      newLink.addAttribute(BBX.INLINE.LINK.IS_EXTERNAL.newAttribute(true))
      newLink.addAttribute(BBX.INLINE.LINK.ATTRIB_HREF.newAttribute(link))

      val currentNode = bbData.manager.simpleManager.currentCaret.node

      if (currentNode !is Text) {
        println("Current node is not text - cannot create link")
        return
      }

      val nodeLength = currentNode.value.length
      val start = (bbData.manager.simpleManager.currentSelection.start as XMLTextCaret).offset
      val end = (bbData.manager.simpleManager.currentSelection.end as XMLTextCaret).offset
      //println("Adding link to Node ${currentNode.value}, length: $nodeLength, start: $start, end: $end")

      //We want either the whole node, or a portion of it.
      //That portion might be from the start to somewhere in the middle, two middle points, or middle to end.
      //So 4 cases, and the return size of splitTextNode varies accordingly.
      val nodeToWrap =
        if (start > 0 && end != -1 && end != nodeLength) {
          //get middle and wrap (two middle points)
          val splitTextNode = XMLHandler.splitTextNode(currentNode, start, end)
          splitTextNode[1]
        } else if (start > 0) {
          //get last and wrap (middle to very end of node)
          val splitTextNode = XMLHandler.splitTextNode(currentNode, start)
          splitTextNode[1]
        } else if (end != -1 && end != nodeLength) {
          //get beginning and wrap (start to middle)
          val splitTextNode = XMLHandler.splitTextNode(currentNode, end)
          splitTextNode[0]
        } else {
          //wrap all (start to very end)
          currentNode
        }
      XMLHandler.wrapNodeWithElement(nodeToWrap, newLink)
      //That got it!
      bbData.manager.simpleManager.dispatchEvent(ModifyEvent(Sender.TEXT, true, nodeToWrap.parent))
    }
    return
  }
}

private fun removeExternalLink(bbData: BBSelectionData) {
  val mapList = bbData.manager.mapList
  val current = mapList.current
  if (current.nodeParent != null && BBX.INLINE.LINK.isA(current.nodeParent)) {
    if (!current.nodeParent.getAttributeValue("external", BB_NS).toBoolean()) {
      //Don't remove internal links from this interface
      return
    }
    //Wow, this was way easier than I thought it would be
    XMLHandler.unwrapElement(current.nodeParent)
    bbData.manager.simpleManager.dispatchEvent(ModifyEvent(Sender.TEXT, false, current.nodeParent))
  }
}
