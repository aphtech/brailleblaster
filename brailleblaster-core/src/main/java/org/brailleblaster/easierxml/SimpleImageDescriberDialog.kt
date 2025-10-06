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
package org.brailleblaster.easierxml

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.BBIni
import org.brailleblaster.bbx.BBX
import org.brailleblaster.easierxml.ImageUtils.findAllImages
import org.brailleblaster.easierxml.ImageUtils.getElementFromInputDescription
import org.brailleblaster.easierxml.ImageUtils.getImage
import org.brailleblaster.easierxml.ImageUtils.getImageNavigateBlock
import org.brailleblaster.easierxml.ImageUtils.imageNotFound
import org.brailleblaster.easierxml.ImageUtils.matchingImages
import org.brailleblaster.easierxml.ImageUtils.setImageDescription
import org.brailleblaster.easierxml.ImageUtils.willReplaceWarning
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.stylers.StyleHandler
import org.brailleblaster.perspectives.braille.ui.BBStyleableText
import org.brailleblaster.perspectives.braille.views.wp.SixKeyHandler
import org.brailleblaster.perspectives.mvc.XMLNodeCaret
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.util.FormUIUtils
import org.brailleblaster.util.Notify.notify
import org.brailleblaster.util.Notify.showMessage
import org.brailleblaster.utils.swt.SizeAndLocation
import org.brailleblaster.util.Utils
import org.brailleblaster.util.YesNoChoice.Companion.ask
import org.brailleblaster.util.ui.SixKeyUtils
import org.brailleblaster.wordprocessor.FontManager
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.events.ShellEvent
import org.eclipse.swt.events.ShellListener
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.function.Consumer

/**
 * Simplified version of the Image Describer, assumes user is given context of
 * the image in the source view or original text book
 */
class SimpleImageDescriberDialog(
    private val m: Manager, swtParent: Shell?, cursorImage: Element?,
    private val onFinishedImgGroup: Consumer<List<Element>>
) {
    private val array: MutableList<Node> = findAllImages(m)
    private var matchingImages: List<Element> = mutableListOf()
    private var index = 0
    private var descTextEditor: BBStyleableText? = null
    private lateinit var shell: Shell
    private var brailleWidget: StyledText? = null
    private var wantsCaptionStyle = false

    init {
        if (array.isNotEmpty()) {
            if (cursorImage != null) {
                log.debug("Cursor in image")
                index = array.indexOf(cursorImage)
                matchingImages = matchingImages(m.doc, getSrc(cursorImage))
            } else {
                index = 0
                matchingImages = matchingImages(m.doc, getSrc(array[index] as Element))
            }
            captionEnabled = matchingImages.any { node -> BBX.CONTAINER.IMAGE.isA(node) }
            open()
        } else {
            notify("There are no images in your file", "No Images")
        }
    }

    private fun open() {
        // Issue #4781: Highlight nearest block so user knows where they are
        val selectedImage = array[index] as Element

        // temporarily disable Table selections due to #5706
        if (Manager.getTableParent(selectedImage) == null) {
            m.simpleManager.dispatchEvent(
                XMLCaretEvent(
                    Sender.IMAGE_DESCRIBER,
                    XMLNodeCaret(getImageNavigateBlock(selectedImage)!!)
                )
            )
        }
        shell = Shell(m.wpManager.shell, SWT.APPLICATION_MODAL or SWT.DIALOG_TRIM or SWT.RESIZE)
        shell.text = "Image Describer"
        val grid = GridLayout(1, false)
        shell.layout = grid
        val menuBar = Menu(shell, SWT.BAR)
        addSizeMenu(shell, menuBar)
        addPreferencesMenu(shell, menuBar)
        val outerContainer = Composite(shell, SWT.NONE)
        outerContainer.layout = GridLayout(1, false)
        outerContainer.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        val sc = ScrolledComposite(outerContainer, SWT.V_SCROLL or SWT.H_SCROLL or SWT.BORDER)
        sc.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        sc.expandVertical = true
        sc.expandHorizontal = true
        val innerContainer = Composite(sc, SWT.NONE)
        innerContainer.layout = GridLayout(1, false)
        innerContainer.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        val dualPane = Group(innerContainer, SWT.NONE)
        dualPane.layout = GridLayout(2, true)
        dualPane.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        dualPane.text = (index + 1).toString() + " of " + array.size + " Images"
        EasySWT.addSwtBotKey(dualPane, SWTBOT_INFO_COMPOSITE)
        val imagePane = Group(dualPane, SWT.NONE)
        imagePane.layout = GridLayout(1, false)
        imagePane.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        val archiver = m.archiver
        val image = getImage(array[index] as Element, archiver, defaultSize)
        val imageLabel = Label(imagePane, SWT.NONE)
        imageLabel.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        if (image != null) {
            imageLabel.image = image
        } else {
            imageNotFound(imageLabel, archiver.path, getSrc(array[index] as Element))
        }
        val inputPane = Group(dualPane, SWT.NONE)
        inputPane.layout = GridLayout(1, false)
        inputPane.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        if (braille) {
            addBraillePane(inputPane)
        } else {
            addStyledTextPane(shell, inputPane, archiver.path, array[index] as Element)
        }
        val nav1 = Group(innerContainer, SWT.NONE)
        nav1.layout = GridLayout(4, false)
        nav1.layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
        val nav2 = Group(innerContainer, SWT.NONE)
        nav2.layout = GridLayout(5, false)
        nav2.layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
        val applySingleInlineButton = Button(nav1, SWT.NONE)
        applySingleInlineButton.text = INLINE_ONE
        applySingleInlineButton.layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
        FormUIUtils.addSelectionListener(applySingleInlineButton) {
            clickApplySingleInline(
                selectedImage
            )
        }
        val applySingleBlockButton = Button(nav1, SWT.NONE)
        applySingleBlockButton.text = BLOCK_ONE
        applySingleBlockButton.layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
        FormUIUtils.addSelectionListener(applySingleBlockButton) {
            clickApplySingleBlock(
                selectedImage
            )
        }
        val applyAllInlineButton = Button(nav1, SWT.PUSH)
        EasySWT.addSwtBotKey(applyAllInlineButton, SWTBOT_APPLY_ALL_INLINE)
        applyAllInlineButton.layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
        applyAllInlineButton.text = INLINE_ALL_P1 + matchingImages.size + INLINE_ALL_P2
        FormUIUtils.addSelectionListener(applyAllInlineButton) {
            clickApplyAllInline(
                matchingImages
            )
        }
        val applyAllBlockButton = Button(nav1, SWT.PUSH)
        EasySWT.addSwtBotKey(applyAllBlockButton, SWTBOT_APPLY_ALL_BLOCK)
        applyAllBlockButton.layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
        applyAllBlockButton.text = BLOCK_ALL_P1 + matchingImages.size + BLOCK_ALL_P2
        FormUIUtils.addSelectionListener(applyAllBlockButton) { clickApplyAllBlock(matchingImages) }
        val deleteSingleButton = Button(nav2, SWT.PUSH)
        deleteSingleButton.layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
        deleteSingleButton.text = "Delete  description"
        FormUIUtils.addSelectionListener(deleteSingleButton) { clickDeleteSingle(selectedImage) }
        val deleteAllButton = Button(nav2, SWT.PUSH)
        deleteAllButton.layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
        deleteAllButton.text = "Delete all " + matchingImages.size + " descriptions"
        FormUIUtils.addSelectionListener(deleteAllButton) { clickDeleteAll(matchingImages) }
        val previous = Button(nav2, SWT.PUSH)
        previous.text = "Previous Image"
        previous.layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
        FormUIUtils.addSelectionListener(previous) {
            clickPrevious(
                shell
            )
        }
        val next = Button(nav2, SWT.PUSH)
        next.text = "Next Image"
        next.layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
        FormUIUtils.addSelectionListener(next) {
            clickNext(
                shell
            )
        }
        val cancel = Button(nav2, SWT.PUSH)
        cancel.text = "Close"
        cancel.layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
        FormUIUtils.addSelectionListener(cancel) { close() }
        sc.content = innerContainer
        FormUIUtils.addEscapeCloseListener(shell)
        shell.addShellListener(object : ShellListener {
            override fun shellActivated(e: ShellEvent) {}
            override fun shellClosed(e: ShellEvent) {
                e.doit = false
                saveLocation()
                e.doit = true
            }

            override fun shellDeactivated(e: ShellEvent) {}
            override fun shellDeiconified(e: ShellEvent) {}
            override fun shellIconified(e: ShellEvent) {}
        })
        FormUIUtils.addKeyListener(shell, SWT.CTRL, 'n'.code) {
            clickNext(
                shell
            )
        }
        FormUIUtils.addKeyListener(shell, SWT.CTRL, 'p'.code) {
            clickPrevious(
                shell
            )
        }
        val pl = previousLocation
        if (pl == null) {
            EasySWT.setSizeAndLocationWideScreen(shell)
        } else {
            EasySWT.setSizeAndLocation(shell, pl)
        }
        shell.layout()
        shell.open()
        if (braille) {
            Utils.adjustFontToDialog(m, brailleWidget!!)
            EasySWT.addResizeListener(
                shell,
                brailleWidget!!
            ) { Utils.adjustFontToDialog(WPManager.getInstance().controller, it) }
        } else {
            Utils.adjustFontToDialog(m, descTextEditor!!.text)
            EasySWT.addResizeListener(shell, descTextEditor!!.text) {
                Utils.adjustFontToDialog(
                    WPManager.getInstance().controller,
                    it
                )
            }
        }
    }

    private fun saveLocation() {
        previousLocation = SizeAndLocation(
            shell.size.x, shell.size.y, shell.location.x,
            shell.location.y
        )
    }

    private fun addSizeMenu(dialog: Shell, menuBar: Menu) {
        val cascadeFileMenu = MenuItem(menuBar, SWT.CASCADE)
        cascadeFileMenu.text = "Image Size"
        val fileMenu = Menu(dialog, SWT.DROP_DOWN)
        cascadeFileMenu.menu = fileMenu
        val currentSelection = defaultSize
        val two = MenuItem(fileMenu, SWT.CHECK)
        two.text = "200"
        two.selection = currentSelection == two.text.toInt()
        dialog.menuBar = menuBar
        two.addListener(SWT.Selection) {
            defaultSize = two.text.toInt()
            previousLocation = null
            dialog.close()
            open()
        }
        val three = MenuItem(fileMenu, SWT.CHECK)
        three.text = "300"
        three.selection = currentSelection == three.text.toInt()
        dialog.menuBar = menuBar
        three.addListener(SWT.Selection) {
            defaultSize = three.text.toInt()
            previousLocation = null
            dialog.close()
            open()
        }
        val four = MenuItem(fileMenu, SWT.CHECK)
        four.text = "400"
        four.selection = currentSelection == four.text.toInt()
        dialog.menuBar = menuBar
        four.addListener(SWT.Selection) {
            defaultSize = four.text.toInt()
            previousLocation = null
            dialog.close()
            open()
        }
        val five = MenuItem(fileMenu, SWT.CHECK)
        five.text = "500"
        five.selection = currentSelection == five.text.toInt()
        dialog.menuBar = menuBar
        five.addListener(SWT.Selection) {
            defaultSize = five.text.toInt()
            previousLocation = null
            dialog.close()
            open()
        }
    }

    private fun addPreferencesMenu(dialog: Shell, menuBar: Menu) {
        val cascadeFileMenu = MenuItem(menuBar, SWT.CASCADE)
        cascadeFileMenu.text = "Input Preference"
        val fileMenu = Menu(dialog, SWT.DROP_DOWN)
        cascadeFileMenu.menu = fileMenu
        val sixKey = MenuItem(fileMenu, SWT.RADIO)
        sixKey.text = "Six Key"
        sixKey.selection = braille
        dialog.menuBar = menuBar
        val styledText = MenuItem(fileMenu, SWT.RADIO)
        styledText.text = "Styled Text"
        styledText.selection = !braille
        dialog.menuBar = menuBar
        sixKey.addListener(SWT.Selection) { event: Event ->
            if ( /* ignore re-selection */braille || (!BBIni.debugging && descTextEditor!!.text.text.isNotEmpty()
                        && !ask("Delete entered text and switch to Six Key?"))
            ) {
                // event.doit is ignored, so manually re-select the other option
                event.doit = false
                styledText.selection = false
                sixKey.selection = true
                return@addListener
            }
            braille = true
            saveLocation()
            dialog.close()
            open()
        }
        styledText.addListener(SWT.Selection) { event: Event ->
            if ( /* ignore re-selection */!braille || (!BBIni.debugging && brailleWidget!!.text.isNotEmpty()
                        && !ask("Delete entered text and switch to Styled Text?"))
            ) {
                // event.doit is ignored, so manually re-select the other option
                event.doit = false
                styledText.selection = true
                sixKey.selection = false
                return@addListener
            }
            braille = false
            saveLocation()
            dialog.close()
            open()
        }
    }

    private fun clickPrevious(shell: Shell) {
        if (index > 0) {
            index--
            matchingImages = matchingImages(m.doc, getSrc(array[index] as Element))
            captionEnabled = matchingImages.any { node: Element? -> BBX.CONTAINER.IMAGE.isA(node) }
            saveLocation()
            shell.close()
            open()
        } else {
            notify("You have reached the beginning of the images", "No more images")
        }
    }

    private fun clickNext(shell: Shell) {
        if (index < array.size - 1) {
            index++
            matchingImages = matchingImages(m.doc, getSrc(array[index] as Element))
            captionEnabled = matchingImages.any { node: Element? -> BBX.CONTAINER.IMAGE.isA(node) }
            saveLocation()
            shell.close()
            open()
        } else {
            notify("You have reached the end of the images", "No more images")
        }
    }

    private fun getSrc(element: Element): String {
        return BBX.SPAN.IMAGE.ATTRIB_SOURCE[element]
    }

    private fun removeBraille(image: Element): Element {
        val oldText = alreadyHasBrailleElement(image)
        oldText?.detach()
        return image
    }

    private fun addBraillePane(inputPane: Group) {
        brailleWidget = StyledText(inputPane, SWT.H_SCROLL or SWT.WRAP)
        brailleWidget!!.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        val brailleString = alreadyHasBrailleElement(array[index] as Element)
        if (brailleString != null) {
            brailleWidget!!.text = SixKeyUtils.formatPreviousImageDescription(brailleString)
        }
        val skh = SixKeyHandler(null, null, true)
        brailleWidget!!.addKeyListener(skh)
        brailleWidget!!.addVerifyKeyListener(skh)
        brailleWidget!!.font = FontManager.BRAILLE_FONTS[0].newFont()
    }

    private fun addStyledTextPane(shell: Shell, inputPane: Group, imgSrc: Path, imgElem: Element) {
        inputPane.layout = GridLayout(1, false)
        FormUIUtils.setGridData(inputPane)
        val actions = Group(inputPane, SWT.NONE)
        actions.layout = GridLayout(3, false)
        actions.layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
        descTextEditor = BBStyleableText(
            inputPane, actions,
            BBStyleableText.BOLD or BBStyleableText.ITALIC or BBStyleableText.CHANGETRANSLATION
                    or BBStyleableText.TRANSCRIBEREMPHASIS or BBStyleableText.SMALL_BUTTONS
                    or BBStyleableText.NONEWLINES,
            SWT.MULTI or SWT.BORDER or SWT.V_SCROLL
        )
        for (i in actions.children.indices) {
            actions.children[i].data = GridData(SWT.FILL, SWT.FILL, true, false)
        }
        FormUIUtils.setGridDataVertical(descTextEditor!!.text)
        val captionStyle = Button(inputPane, SWT.CHECK)
        captionStyle.text = "Wrap Text in Caption Style"
        // #4567: Do not check this by default as this tool isn't usually used
        // for this and disable if only inline images
        if (!captionEnabled) {
            captionStyle.isEnabled = false
        }
        FormUIUtils.addSelectionListener(captionStyle) { wantsCaptionStyle = !wantsCaptionStyle }
        val parentImgGroup =
            XMLHandler.ancestorVisitorElement(imgElem) { node: Element? -> BBX.CONTAINER.IMAGE.isA(node) }
        if (parentImgGroup != null) {
            willReplaceWarning(shell)
        }
        if (imgElem.childCount > 0) {
            descTextEditor!!.setXML(imgElem)
        }
    }

    private fun close() {
        saveLocation()
        shell.close()
    }

    private fun clickApplySingleInline(imgElem: Element) {
        if (braille && brailleWidget!!.text.isEmpty()) {
            return
        } else if (!braille && descTextEditor!!.text.text.isEmpty()) {
            return
        }
        val imgWrapper: Element = if (braille) {
            removeBraille(array[index] as Element)
            insertIntoImageTag(
                SixKeyUtils.saveBraille(true, m, brailleWidget!!.text, true), true,
                array[index] as Element
            )
        } else {
            if (descTextEditor!!.text.text.isBlank()) {
                showMessage("Description is empty")
                return
            }
            val newDescElement = getElementFromInputDescription(
                descTextEditor!!
            )
            setImageDescription(imgElem, newDescElement, wantsCaptionStyle)
        }
        postClick(listOf(imgWrapper))
        open()
    }

    private fun clickApplySingleBlock(imgElem: Element) {
        if (braille && brailleWidget!!.text.isEmpty()) {
            return
        } else if (!braille && descTextEditor!!.text.text.isEmpty()) {
            return
        }
        val imgWrapper: Element = if (braille) {
            removeBraille(array[index] as Element)
            insertIntoImageTag(
                SixKeyUtils.saveBraille(false, m, brailleWidget!!.text, true), false,
                array[index] as Element
            )
        } else {
            if (descTextEditor!!.text.text.isBlank()) {
                showMessage("Description is empty")
                return
            }
            val newDescElement = getElementFromInputDescription(
                descTextEditor!!
            )
            setImageDescription(imgElem, newDescElement, wantsCaptionStyle)
        }
        postClick(listOf(imgWrapper))
        open()
    }

    private fun clickApplyAllInline(imgElements: List<Element?>) {
        val changedImgGroups: MutableList<Element> = mutableListOf()
        if (braille && brailleWidget!!.text.isEmpty()) {
            return
        } else if (!braille && descTextEditor!!.text.text.isEmpty()) {
            return
        }
        if (braille) {
            for (e in matchingImages) {
                removeBraille(e)
                changedImgGroups.add(
                    insertIntoImageTag(SixKeyUtils.saveBraille(true, m, brailleWidget!!.text, true), true, e)
                )
            }
        } else {
            if (descTextEditor!!.text.text.isBlank()) {
                showMessage("Description is empty")
                return
            }
            val newDescElement = getElementFromInputDescription(
                descTextEditor!!
            )
            if (!BBIni.debugging && !ask("Replace description on " + imgElements.size + " images?")) {
                return
            }
            for (curElem in imgElements) {
                val newImgGroup = setImageDescription(curElem!!, newDescElement, wantsCaptionStyle)
                changedImgGroups.add(newImgGroup)
            }
        }
        postClick(changedImgGroups)
        open()
    }

    private fun clickApplyAllBlock(imgElements: List<Element?>) {
        val changedImgGroups: MutableList<Element> = ArrayList()
        if (braille && brailleWidget!!.text.isEmpty()) {
            return
        } else if (!braille && descTextEditor!!.text.text.isEmpty()) {
            return
        }
        if (braille) {
            for (e in matchingImages) {
                removeBraille(e)
                changedImgGroups.add(
                    insertIntoImageTag(
                        SixKeyUtils.saveBraille(false, m, brailleWidget!!.text, true), false, e
                    )
                )
            }
        } else {
            if (descTextEditor!!.text.text.isBlank()) {
                showMessage("Description is empty")
                return
            }
            val newDescElement = getElementFromInputDescription(
                descTextEditor!!
            )
            if (!BBIni.debugging && !ask("Replace description on " + imgElements.size + " images?")) {
                return
            }
            for (curElem in imgElements) {
                val newImgGroup = setImageDescription(curElem!!, newDescElement, wantsCaptionStyle)
                changedImgGroups.add(newImgGroup)
            }
        }
        postClick(changedImgGroups)
        open()
    }

    private fun insertIntoImageTag(ele: Element, inline: Boolean, image: Element): Element {
        if (inline && !BBX.CONTAINER.IMAGE.isA(image)) {
            Utils.insertChildCountSafe(image, ele, 0)
        } else {
            val bodyText = BBX.BLOCK.DEFAULT.create()
            Utils.insertChildCountSafe(image, bodyText, 0)
            bodyText.appendChild(ele)
            StyleHandler.addStyle(bodyText, "1-1", m)
        }
        return image
    }

    private fun clickDeleteAll(imgElements: List<Element>) {
        val changedImgGroups: MutableList<Element> = ArrayList()
        if (braille && brailleWidget!!.text.isEmpty()) {
            return
        } else if (!braille && descTextEditor!!.text.text.isEmpty()) {
            return
        }
        for (e in matchingImages) {
            changedImgGroups.add(removeBraille(e))
        }
        postClick(changedImgGroups)
        open()
    }

    private fun clickDeleteSingle(imgElem: Element) {
        if (braille && brailleWidget!!.text.isEmpty()) {
            return
        } else if (!braille && descTextEditor!!.text.text.isEmpty()) {
            return
        }
        val imgWrapper: Element = removeBraille(array[index] as Element)
        postClick(listOf(imgWrapper))
        open()
    }

    private fun postClick(modifiedElements: List<Element>) {
        onFinishedImgGroup.accept(modifiedElements)
        saveLocation()
        shell.close()
    }

    companion object {
        private val log = LoggerFactory.getLogger(SimpleImageDescriberDialog::class.java)
        const val SWTBOT_APPLY_ALL_INLINE = "simpleImageDescriber.applyAllInline"
        const val SWTBOT_APPLY_ALL_BLOCK = "simpleImageDescriber.applyAllBlock"
        const val SWTBOT_INFO_COMPOSITE = "simpleImageDescriber.applyAll"
        const val INLINE_ONE = "Apply to this Image Inline"
        const val BLOCK_ONE = "Apply to this Image Block"
        const val INLINE_ALL_P1 = "Apply to All "
        const val BLOCK_ALL_P1 = "Apply to All "
        const val INLINE_ALL_P2 = " Instances Inline"
        const val BLOCK_ALL_P2 = " Instances Block"
        const val CAPTION_STYLE_BOX = "Wrap Text in Caption Style"
        var defaultSize = 200
        private var captionEnabled = false
        private var braille = true
        private var previousLocation: SizeAndLocation? = null
        fun alreadyHasBrailleElement(image: Element): Node? {
            return XMLHandler.childrenRecursiveVisitor(
                image
            ) { e ->
                (BBX.INLINE.EMPHASIS.isA(e)
                        && BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[e].contains(EmphasisType.NO_TRANSLATE)) && XMLHandler.findFirstText(
                    e
                ) != null
            }
        }

        @JvmStatic
        fun makeAllString(block: Boolean, number: String): String {
            return if (block) {
                BLOCK_ALL_P1 + number + BLOCK_ALL_P2
            } else {
                INLINE_ALL_P1 + number + INLINE_ALL_P2
            }
        }
    }
}