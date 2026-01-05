package org.brailleblaster.tools

import nu.xom.Element
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.XMLNodeCaret
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.SharedItem
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.utils.localization.LocaleHandler
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.utils.xml.BB_NS
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Dialog
import org.eclipse.swt.widgets.List
import org.eclipse.swt.widgets.Shell

//This tool adds bookmark XML elements at the current block.
//The link manager then handles linking internal links to those bookmarks, much like MS Word
private val localeHandler = LocaleHandler.getDefault()

class InsertBookmarkTool(parent: Manager) : Dialog(parent.wpManager.shell, SWT.NONE), MenuToolModule {
  override val topMenu: TopMenu = TopMenu.NAVIGATE
  override val title: String = localeHandler["BookmarksMenu"]
  override val accelerator: Int = SWT.MOD1 or SWT.MOD2 or 'K'.code
  override val sharedItem: SharedItem = SharedItem.INSERT_BOOKMARK

  private var bookmarksNodeList = mutableListOf<Element>()

  override fun onRun(bbData: BBSelectionData) {
    val shell = Shell(parent.display)
    shell.text = "Bookmarks"
    shell.layout = GridLayout(2, false)
    
    val textBoxGroup = EasySWT.makeGroup(shell, SWT.NONE, 1, true)
    val buttonsGroup = EasySWT.makeGroup(shell, SWT.NONE, 1, true)

    val gd = GridData()
    gd.widthHint = 180
    gd.heightHint = 90
    gd.grabExcessVerticalSpace = true
    gd.grabExcessHorizontalSpace = true

    val entryBox = EasySWT.makeText(textBoxGroup, 180, 1)
    entryBox.message = "Enter a unique bookmark name"
    val bookmarksList = List(textBoxGroup, SWT.BORDER or SWT.V_SCROLL or SWT.SINGLE)
    getBookmarksXPath(bbData).forEach {
      bookmarksList.add(it)
    }
    //Add a double-click listener to the list to go to the selected bookmark
    bookmarksList.addListener(SWT.MouseDoubleClick) {
      if (bookmarksList.selectionIndex != -1) {
        moveToNode(bbData, bookmarksList.selectionIndex)
      }
    }
    bookmarksList.layoutData = gd
    bookmarksList.setFocus()

    EasySWT.addEnterListener(bookmarksList){
      if (bookmarksList.selectionIndex != -1) {
        //Navigate to existing bookmark if already present
        moveToNode(bbData, bookmarksList.selectionIndex)
        bookmarksList.deselectAll()
      }
    }

    EasySWT.addEnterListener(entryBox) {
      if (!entryBox.text.isEmpty() && !bookmarksList.items.contains(entryBox.text)) {
        addBookmark(bbData, entryBox.text)
        //Refresh manager to avoid errors when navigating to newly added bookmarks
        //Not sure why it works, but it does fix the problem.
        bbData.manager.refresh()
        bookmarksList.deselectAll()
        //Regenerate bookmarks list
        bookmarksList.removeAll()
        getBookmarksXPath(bbData).forEach {
          bookmarksList.add(it)
        }
        shell.forceFocus()
      }
    }

    val bookmarkAtCursor = Button(buttonsGroup, SWT.PUSH)
    bookmarkAtCursor.text = "Add Bookmark at Cursor"
    bookmarkAtCursor.addListener(SWT.Selection) {
      if (!entryBox.text.isEmpty() && !bookmarksList.items.contains(entryBox.text)) {
        addBookmark(bbData, entryBox.text)
        //Refresh manager to avoid errors when navigating to newly added bookmarks
        bbData.manager.refresh()
        bookmarksList.deselectAll()
        bookmarksList.removeAll()
        getBookmarksXPath(bbData).forEach {
          bookmarksList.add(it)
        }
        shell.forceFocus()
      }
    }

    EasySWT.makePushButton(buttonsGroup, "Remove Selected Bookmark", 1) {
      //remove currently selected bookmark
      removeBookmark(bbData, bookmarksList.selectionIndex)
      //Regenerate bookmarks list
      bookmarksList.deselectAll()
      bookmarksList.removeAll()
      getBookmarksXPath(bbData).forEach {
        bookmarksList.add(it)
      }
    }

    EasySWT.makePushButton(buttonsGroup, "Go To Selected Bookmark", 1) {
      if (bookmarksList.selectionIndex != -1) {
        moveToNode(bbData, bookmarksList.selectionIndex)
        bookmarksList.deselectAll()
      }
    }

    EasySWT.makePushButton(buttonsGroup, "Clear Selection", 1) {
      bookmarksList.deselectAll()
    }

    EasySWT.addEscapeCloseListener(shell)

    shell.pack()
    shell.open()
  }

  private fun removeBookmark(bbData: BBSelectionData, bookmarkIndex: Int) {
    //Remove the link pointer in the selected block with the given linkID.
    //Note that this does not remove any links that point to this bookmark...Does MS Word even do that?
    if (bookmarkIndex in bookmarksNodeList.indices) {
      val el = bookmarksNodeList[bookmarkIndex]
      el.removeAttribute(el.getAttribute("linkID", BB_NS))
      bbData.manager.simpleManager.dispatchEvent(ModifyEvent(Sender.TEXT, false, el.parent))
    }
    else {
      return
    }
  }

  private fun addBookmark(bbData: BBSelectionData, newLinkID: String) {
    //Create a linkID in a selected block with a unique name (provided by user)
    //println("Adding bookmark with linkID: $newLinkID")
    val block = bbData.manager.mapList.current.block

    if (block != null) {
      try {
        block.addAttribute(BBX.BLOCK.LINKID.newAttribute(newLinkID))
        bbData.manager.simpleManager.dispatchEvent(ModifyEvent(Sender.TEXT, false, block.parent))
      }
      catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  private fun moveToNode(bbData: BBSelectionData, bookmarkIndex: Int) {
    //Move the caret to the block that contains the given node
    if (bookmarkIndex in bookmarksNodeList.indices) {
      //println("Moving to bookmark index $bookmarkIndex; bookmark count: ${bookmarksNodeList.size}")
      val node = bookmarksNodeList[bookmarkIndex]
      //println("Bookmark node: ${node.toXML()}")
      bbData.manager.simpleManager.dispatchEvent(XMLCaretEvent(Sender.GO_TO_PAGE, XMLNodeCaret(node)))
    }
  }

  private fun getBookmarksXPath(bbData: BBSelectionData): kotlin.collections.List<String> {
    var elementStrings = listOf<String>()
    try {
      //By Jove, it works!
      val xpath = """//*[@*[local-name() = 'linkID']]"""
      val results = bbData.manager.simpleManager.doc.query(xpath).toList()
      //println("Found ${results.size} bookmarks via XPath")
      bookmarksNodeList.clear()
      bookmarksNodeList = results.map{
        it as Element
      } as MutableList<Element>
      //println("${bookmarksNodeList.size} nodes added to bookmarksNodeList")

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
}