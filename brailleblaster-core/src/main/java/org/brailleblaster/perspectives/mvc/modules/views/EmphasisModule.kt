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
package org.brailleblaster.perspectives.mvc.modules.views

import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.isPageNumAncestor
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.BBSimpleManager
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.XMLSelection
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.menu.*
import org.brailleblaster.perspectives.mvc.menu.MenuManager.addToSharedSubMenus
import org.brailleblaster.perspectives.mvc.modules.misc.TableSelectionModule
import org.brailleblaster.perspectives.mvc.modules.views.EmphasisModule.addEmphasis
import org.brailleblaster.perspectives.mvc.modules.views.EmphasisModule.verifyNumberedListItem
import org.brailleblaster.tools.EmphasisMenuTool
import org.brailleblaster.tools.MenuToolModule
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.internal.xml.splitNode
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.utd.utils.getTextChild
import org.brailleblaster.utd.utils.stripUTDRecursive
import org.brailleblaster.util.Utils
import org.brailleblaster.utils.xom.childNodes
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.MessageBox
import org.eclipse.swt.widgets.Shell
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer

private const val NO_OFFSET = -1
private val log = LoggerFactory.getLogger(EmphasisModule::class.java)

object EmphasisModule : AbstractModule(), SimpleListener {
    init {
        sender = Sender.EMPHASIS
    }

    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent) {
            MenuManager.add(BoldTool)
            MenuManager.add(ItalicsTool)
            MenuManager.add(UnderlineTool)
            MenuManager.add(ScriptTool)
            MenuManager.add(TnSymbolsTool)
            val smb = SubMenuBuilder(TopMenu.EMPHASIS, "Transcriber-Defined Typeforms")
            smb.addItem(
                EmphasisItem.TD1.longName,
                if (Utils.isLinux) SWT.MOD3 + SWT.MOD2 + '!'.code else SWT.MOD3 + SWT.MOD2 + '1'.code
            ) { addEmphasis(event.manager, EmphasisType.TRANS_1) }
            smb.addItem(
                EmphasisItem.TD2.longName,
                if (Utils.isLinux) SWT.MOD3 + SWT.MOD2 + '@'.code else SWT.MOD3 + SWT.MOD2 + '2'.code
            ) { addEmphasis(event.manager, EmphasisType.TRANS_2) }
            smb.addItem(
                EmphasisItem.TD3.longName,
                if (Utils.isLinux) SWT.MOD3 + SWT.MOD2 + '#'.code else SWT.MOD3 + SWT.MOD2 + '3'.code
            ) { addEmphasis(event.manager, EmphasisType.TRANS_3) }
            smb.addItem(
                EmphasisItem.TD4.longName,
                if (Utils.isLinux) SWT.MOD3 + SWT.MOD2 + '$'.code else SWT.MOD3 + SWT.MOD2 + '4'.code
            ) { addEmphasis(event.manager, EmphasisType.TRANS_4) }
            smb.addItem(
                EmphasisItem.TD5.longName,
                if (Utils.isLinux) SWT.MOD3 + SWT.MOD2 + '%'.code else SWT.MOD3 + SWT.MOD2 + '5'.code
            ) { addEmphasis(event.manager, EmphasisType.TRANS_5) }
            addToSharedSubMenus(SharedItem.TYPEFORMS, smb)
            MenuManager.add(smb.build())
            MenuManager.add(RemoveAllEmphasisTool)
            MenuManager.add(RemoveAllHeadingEmphasisTool)
            MenuManager.add(RemoveAllListEmphasisTool)
            MenuManager.add(RemoveAllGuideWordEmphasisTool)
        }
    }

    fun interface EmphasizeCallback {
        fun emphasize(emphasis: EmphasisType, node: Text, start: Int, end: Int, remove: Boolean): Text
    }



        @JvmField
		val REMOVE_ALL_EMPHASIS_MODIFIER = SWT.MOD1 + SWT.MOD2 //Abstracted for use in tests
        const val REMOVE_ALL_EMPHASIS_ACCELERATOR = 'R'
        @JvmStatic
		fun addEmphasis(manager: BBSimpleManager, emphasisType: EmphasisType) {
            addEmphasis(manager, emphasisType, ::emphasize)
        }

        /**
         *
         * @param manager
         * @param emphasisType
         * @param callback Allow calling code to custom handle emphaize
         */
        fun addEmphasis(manager: BBSimpleManager, emphasisType: EmphasisType, callback: EmphasizeCallback) {
            if (manager.getModule(TableSelectionModule::class.java)!!.isTableSelected) {
                TableSelectionModule.displayInvalidTableMessage(WPManager.getInstance().shell)
                return
            }
            val currentSelection = manager.currentSelection
            val modifiedBlocks = currentSelection.selectedBlocks

//		for(Element e: modifiedBlocks){
//			if (XMLHandler.childrenRecursiveNodeVisitor(e, n -> MathModule.isMath(n)) != null
//					&& MathModule.cursorIsInMath(manager.getManager().getTextView().getCaretOffset(), manager.getManager())
//					){
//				new Notify("Cannot emphasize math", Notify.ALERT_SHELL_NAME);
//				return;
//			}
//		}
            modifiedBlocks.forEach(Consumer { rootRaw -> rootRaw.stripUTDRecursive() })
            //If all of the current selection is emphasized, remove that emphasis. Otherwise, emphasize the selection.
            val removeEmphasis = isAllEmphasized(currentSelection, emphasisType)
            if (currentSelection.isSingleNode) {
                if (currentSelection.start.node is Text) {
                    if ((currentSelection.end as XMLTextCaret).offset == (currentSelection.start as XMLTextCaret).offset) {
                        return  // nothing selected, return
                    } else {
                        log.debug("Single node is selected")
                        callback.emphasize(
                            emphasisType, currentSelection.start.node, currentSelection.start.offset,
                            currentSelection.end.offset, removeEmphasis
                        )
                    }
                }
                else {
                    //Element is selected. Apply emphasis to all descendant text nodes
                    FastXPath.descendant(currentSelection.start.node)
                        .filterIsInstance<Text>()
                        .filter { n -> !MathModuleUtils.isMath(n) }
                        .forEach { n ->
                            callback.emphasize(
                                emphasisType,
                                n,
                                NO_OFFSET,
                                NO_OFFSET,
                                removeEmphasis
                            )
                        }
                }
            }
            else {
                modifiedBlocks.forEach(Consumer { rootRaw -> rootRaw.stripUTDRecursive() })
                var startNode: Node? = currentSelection.start.node

                //If the last node of the selection is not a text node, find the last text descendant of that node
                val finalTextNode = getFinalTextNode(currentSelection.end.node)
                var finalTextOffset = NO_OFFSET
                if (currentSelection.end.node is Text) {
                    finalTextOffset = (currentSelection.end as XMLTextCaret).offset
                }
                if (startNode is Text && !MathModuleUtils.isMath(startNode)) { //Apply the emphasis to the first node
                    if ((currentSelection.start as XMLTextCaret).offset != startNode.value.length) {
                        startNode = callback.emphasize(
                            emphasisType,
                            startNode,
                            currentSelection.start.offset,
                            NO_OFFSET,
                            removeEmphasis
                        )
                    }
                    startNode = XMLHandler.followingNode(startNode)
                }

                //Iterate from the starting text node to the ending text node, applying emphasis
                //as each text node is found
                while (startNode !== finalTextNode) {
                    startNode =
                        XMLHandler.followingWithSelfVisitor(startNode) { n: Node -> n is Text && !MathModuleUtils.isMath(n) || n === finalTextNode }
                    if (startNode != null && startNode !== finalTextNode) {
                        startNode =
                            callback.emphasize(emphasisType, startNode as Text, NO_OFFSET, NO_OFFSET, removeEmphasis)
                        startNode = XMLHandler.followingNode(startNode)
                    }
                }
                if (finalTextNode is Text && !MathModuleUtils.isMath(finalTextNode)) callback.emphasize(
                    emphasisType,
                    finalTextNode,
                    NO_OFFSET,
                    finalTextOffset,
                    removeEmphasis
                )
            }

            //Clean up elements and retranslate/reformat.
            modifiedBlocks.forEach(Consumer { element: Element -> cleanInlineNoText(element) })
            cleanEmptyInlineAttributes(modifiedBlocks)
            mergeAdjacentInline(modifiedBlocks)
            mergeAdjacentTextNodes(modifiedBlocks)
            val modifiedNodes: List<Node> = ArrayList<Node>(modifiedBlocks)
            if (modifiedNodes.isEmpty()) {
                return  //this is causing the undo event frame to be null
            }
            manager.dispatchEvent(ModifyEvent(Sender.EMPHASIS, modifiedNodes, true))
        }

    /*
 * Check if this emphasis item is a numbering of some sort for a list.
 * Ex. A., a., 1, I, etc.
 * Logical conditions:
 * 		1. If it's the first child of the list item  -- check
 * 		2. If it ends with a period.
 * 		3. If the string value is numeric
 * 			or if it's any single letter of the alphabet
 * 			or if it's a Roman number.
 */
        fun verifyNumberedListItem(emphasisItem: Element, parent: Element): Boolean {
            val value = getTextChild(emphasisItem).value.lowercase(Locale.getDefault())
            if (parent.indexOf(emphasisItem) == 0
                && (value.matches("\\d+[.]".toRegex()) || value.matches("[a-z][.]".toRegex())
                        || value.matches("^m{0,4}(cm|cd|d?c{0,3})(xc|xl|l?x{0,3})(ix|iv|v?i{0,3})$[.]".toRegex()))
            ) {
                return true
            } else {
                removePossibleNumberInListItem(emphasisItem, parent)
            }
            return false
        }

        /*
	 * 	If there is text immediately emphasized after the number, try and find where the number ends.
	 */
        fun removePossibleNumberInListItem(emphasisItem: Element, parent: Element) {
            val textChild = getTextChild(emphasisItem)
            val value = textChild.value
            if (parent.indexOf(emphasisItem) == 0 && (value.matches(".*\\d+[.].+".toRegex())
                        || value.matches(".*[a-zA-Z][.].+".toRegex())
                        || value.matches("^m{0,4}(cm|cd|d?c{0,3})(xc|xl|l?x{0,3})(ix|iv|v?i{0,3})$[.].+".toRegex()))
            ) {
                val str = value.split(" ".toRegex(), limit = 2).toTypedArray()
                if (str.size == 2) {
                    val number = Text(str[0])
                    //Sanity check
                    if (str[0].contains(" ")) {
                        return
                    }
                    val text = Text(" " + str[1])
                    emphasisItem.parent.insertChild(number, 0)
                    emphasisItem.replaceChild(textChild, text)
                }
            }
        }


    /**
         * @param modifiedNodes
         * @return
         * clean-up method to remove any empty <INLINE bb:emphasis = "">
        </INLINE> */
        private fun cleanEmptyInlineAttributes(modifiedNodes: List<Element>) {
            modifiedNodes.forEach { e: Element ->
                e.childNodes.forEach { child: Node ->
                    if (BBX.INLINE.EMPHASIS.isA(child) && BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[child as Element].isEmpty()) {
                        val childText = Text(child.value)
                        val parent = child.parent
                        parent.replaceChild(child, childText)
                    }
                }
            }
    }

        private fun mergeAdjacentInline(modifiedNodes: List<Element>) {
            modifiedNodes.forEach(Consumer { n: Element? ->
                val descendantInlines = FastXPath.descendant(n)
                    .filter { node: Node? -> BBX.INLINE.EMPHASIS.isA(node) }.toMutableList()
                var i = 0
                while (i < descendantInlines.size) {
                    val curNode = descendantInlines[i] as Element
                    val parent = curNode.parent
                    val index = parent.indexOf(curNode)
                    if (index > 0 && mergeEmphasis(
                            parent.getChild(index - 1),
                            curNode,
                            descendantInlines
                        ) != null || index < parent.childCount - 1 && mergeEmphasis(
                            curNode,
                            parent.getChild(index + 1),
                            descendantInlines
                        ) != null
                    ) {
                        i--
                    }
                    i++
                }
            })
        }

        private fun mergeAdjacentTextNodes(modifiedBlocks: List<Element>) {
            modifiedBlocks.forEach(Consumer { n ->
                n.stripUTDRecursive()
                Utils.combineAdjacentTextNodes(n)
            })
        }

        private fun hasAllEmphasis(node: Node, emphasis: EnumSet<EmphasisType>): Boolean {
            return BBX.INLINE.EMPHASIS.isA(node) && BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[node as Element] == emphasis
        }

        private fun mergeEmphasis(node1: Node, node2: Node, modifiedEmphasis: MutableList<Node>): Element? {
            val parent = node1.parent
            require(!(parent !== node2.parent || parent.indexOf(node2) != parent.indexOf(node1) + 1)) { "node2 must be the following sibling of node1" }
            if (!BBX.INLINE.EMPHASIS.isA(node1) || !BBX.INLINE.EMPHASIS.isA(node2)) return null
            val emp = BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[node1 as Element]
            if (hasAllEmphasis(node2, emp)) {
                modifiedEmphasis.remove(node1)
                modifiedEmphasis.remove(node2)
                val newInline = BBX.INLINE.EMPHASIS.create(emp)
                newInline.appendChild(node1.value + node2.value)
                node2.detach()
                parent.replaceChild(node1, newInline)
                modifiedEmphasis.add(newInline)
                return newInline
            }
            return null
        }

        /**
         * @param element
         * clean-up method to remove any <INLINE type = BOLD>
        </INLINE> */
        private fun cleanInlineNoText(element: Element) {
            val children = element.childNodes
            children.removeAll { it is Element && BBX.INLINE.EMPHASIS.isA(it) && hasNoText(it) }
        }

        private fun hasNoText(element: Element): Boolean {
            return element.childNodes.none { it is Text }
        }

        private fun emphasize(emphasis: EmphasisType, node: Text, start: Int, end: Int, remove: Boolean): Text {
            return emphasize(EnumSet.of(emphasis), node, start, end, remove)
        }

}
object RemoveAllEmphasisTool : MenuToolModule {
    override val topMenu = TopMenu.EMPHASIS
    override val title = "Remove Emphasis From Selection"
    override val accelerator =
        EmphasisModule.REMOVE_ALL_EMPHASIS_MODIFIER or EmphasisModule.REMOVE_ALL_EMPHASIS_ACCELERATOR.code
    override val enableListener = EnableListener.SELECTION
    override fun onRun(bbData: BBSelectionData) {
        val process = processNotice("Removing all emphasis from the selection.")
        val selection = bbData.manager.simpleManager.currentSelection
        if (!selection.isTextNoSelection) {
            //too complicated and no need to re-invent the wheel
            addEmphasis(
                bbData.manager.simpleManager,
                EmphasisType.ITALICS
            ) { _, node, start, end, _ ->
                if (!validateEmphasis(node.value.length, start, end)) {
                    return@addEmphasis node
                }
                if (getEmphasisInline(node) == null) return@addEmphasis node
                val parent = getEmphasisInline(node)
                val curEmphasis = BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[parent]
                emphasize(curEmphasis, node, start, end, true)
            }
        }
        process.close()
    }
}
object RemoveAllHeadingEmphasisTool : MenuToolModule {
    override val topMenu = TopMenu.EMPHASIS
    override val title = "Remove All Emphasis from Headings"
    override val enableListener = EnableListener.SELECTION
    override fun onRun(bbData: BBSelectionData) {
        val process = processNotice("Removing all emphasis from headings.")
        val root = BBX.getRoot(bbData.manager.simpleManager.doc)
        //		List<Node> modifiedNodes = new ArrayList<>();
        /*
		 * 	Find all elements with emphasis
		 * 	Find all INLINE bb:type="EMPHASIS" descendants
		 */
        val emphasisNodes = root.query("descendant::node()[contains(@bb:type, 'EMPHASIS')]", BBX.XPATH_CONTEXT)
        for (i in 0 until emphasisNodes.size()) {
            //Headings are <BLOCK bb:type="STYLE" utd:style="...heading">
            val parent = emphasisNodes[i].parent as Element
            if (BBX.BLOCK.STYLE.isA(parent)) {
                //Check the attribute for heading
                if (parent.getAttribute("utd-style") != null) {
                    if (parent.getAttributeValue("utd-style").lowercase(Locale.getDefault()).contains("heading")) {
                        (emphasisNodes[i] as Element).stripUTDRecursive()
                        stripEmphasis(emphasisNodes[i] as Element)
                        //						modifiedNodes.add(parent);
                    }
                }
            }
        }
        //		manager.dispatchEvent(new ModifyEvent(Sender.EMPHASIS, modifiedNodes, true));
        bbData.manager.simpleManager.dispatchEvent(
            ModifyEvent(
                Sender.NO_SENDER,
                true,
                BBX.getRoot(bbData.manager.simpleManager.doc)
            )
        )
        process.close()
        val message = MessageBox(bbData.wpManager.shell)
        message.message = "Finished removing all emphasis from headings."
        message.open()
    }
}
object RemoveAllListEmphasisTool : MenuToolModule {
    override val topMenu = TopMenu.EMPHASIS
    override val title = "Remove Emphasis from List Prefixes"
    override val enableListener = EnableListener.SELECTION
    override fun onRun(bbData: BBSelectionData) {
        val process = processNotice("Removing all emphasis from list prefixes.")
        val root = BBX.getRoot(bbData.manager.simpleManager.doc)
        val emphasisNodes = root.query("descendant::node()[contains(@bb:type, 'EMPHASIS')]", BBX.XPATH_CONTEXT)
        for (i in 0 until emphasisNodes.size()) {
            val parent = emphasisNodes[i].parent as Element
            if (isListItem(emphasisNodes[i]) && verifyNumberedListItem(
                    emphasisNodes[i] as Element, parent
                )
            ) {
                (emphasisNodes[i] as Element).stripUTDRecursive()
                stripEmphasis(emphasisNodes[i] as Element)
            }
        }
        bbData.manager.simpleManager.dispatchEvent(
            ModifyEvent(
                Sender.NO_SENDER,
                true,
                BBX.getRoot(bbData.manager.simpleManager.doc)
            )
        )
        process.close()
        val message = MessageBox(bbData.wpManager.shell)
        message.message = "Finished removing all emphasis from list items."
        message.open()
    }
}
object RemoveAllGuideWordEmphasisTool : MenuToolModule {
    override val topMenu = TopMenu.EMPHASIS
    override val title = "Remove Emphasis from Alphabetic Reference Entry Words"
    override val enableListener = EnableListener.SELECTION
    override fun onRun(bbData: BBSelectionData) {
        val process = processNotice("Removing all emphasis from alphabetic reference entry words.")
        val root = BBX.getRoot(bbData.manager.simpleManager.doc)
        val emphasisNodes = root.query("descendant::node()[contains(@bb:type, 'EMPHASIS')]", BBX.XPATH_CONTEXT)
        for (i in 0 until emphasisNodes.size()) {
            if (isGuideWordItem(emphasisNodes[i])) {
                (emphasisNodes[i] as Element).stripUTDRecursive()
                stripEmphasis(emphasisNodes[i] as Element)
            }
        }
        bbData.manager.simpleManager.dispatchEvent(
            ModifyEvent(
                Sender.NO_SENDER,
                true,
                BBX.getRoot(bbData.manager.simpleManager.doc)
            )
        )
        process.close()
        val message = MessageBox(bbData.wpManager.shell)
        message.message = "Finished removing all emphasis from alphabatic reference entry words."
        message.open()
    }
}
private fun processNotice(label: String): Shell {
    val process = Shell(WPManager.getInstance().shell, SWT.TITLE)
    process.setSize(350, 60)
    process.text = "Working..."
    val notice = Label(process, SWT.CENTER)
    notice.text = label
    notice.pack()
    process.open()
    return process
}
private fun isGuideWordItem(node: Node): Boolean {
    val e = node.parent as Element
    //If parent is a block of a list item
    return if (BBX.SPAN.DEFINITION_TERM.isA(e)) {
        true
    } else e.getAttribute("utd-style") != null && e.getAttributeValue("utd-style") == "Guide Word"
    //If node has style list
}
private fun stripEmphasis(emphasis: Element): Text {
    require(BBX.INLINE.EMPHASIS.isA(emphasis)) { "Element must be emphasis" }
    val parent = emphasis.parent
    var text = Text(emphasis.value)
    parent.replaceChild(emphasis, text)
    text = normalizeTextNode(text)
    return text
}
private fun normalizeTextNode(node: Text): Text {
    val parent = node.parent
    var index = parent.indexOf(node)
    if (index != 0 && parent.getChild(index - 1) is Text) {
        val tempValue = parent.getChild(index - 1).value + node.value
        parent.removeChild(index - 1)
        parent.replaceChild(parent.getChild(index - 1), Text(tempValue))
        index--
    }
    if (index != parent.childCount - 1 && parent.getChild(index + 1) is Text) {
        val tempValue = parent.getChild(index).value + parent.getChild(index + 1).value
        parent.removeChild(index + 1)
        parent.replaceChild(parent.getChild(index), Text(tempValue))
    }
    return parent.getChild(index) as Text
}
private fun isListItem(node: Node): Boolean {
    val e = node.parent as Element
    //If parent is a block of a list item
    return if (BBX.BLOCK.LIST_ITEM.isA(e)) {
        true
    } else (e.getAttributeValue("utd-style")?.let { it.startsWith("L") && it[1].isDigit() } ?: false)
    //If node has style list
}
private fun validateEmphasis(nodeLength: Int, start: Int, end: Int): Boolean {
    if (start == NO_OFFSET && end == NO_OFFSET) return true
    if (start == NO_OFFSET) return end > 0
    return if (end == NO_OFFSET) start < nodeLength else start != end
}
private fun getEmphasisInline(node: Text): Element? {
    return XMLHandler.ancestorVisitor(node) { node: Node? -> BBX.INLINE.EMPHASIS.isA(node) } as Element?
}
private fun emphasize(
    emphasis: EnumSet<EmphasisType>,
    node: Text,
    start: Int,
    end: Int,
    remove: Boolean
): Text {
    if (node.isPageNumAncestor() || MathModuleUtils.isMath(node) || !validateEmphasis(
            node.value.length,
            start,
            end
        )
    ) {
        return node
    }
    return if (remove == hasEmphasis(node, emphasis)) {
        if (remove) toggleWithPreviousEmphasis(
            getEmphasisInline(node),
            emphasis,
            node,
            start,
            end
        ) else applyEmphasis(emphasis, node, start, end)
    } else {
        node
    }
}

private fun hasEmphasis(node: Text, emphasisTypes: EnumSet<EmphasisType>): Boolean {
    val parent = getEmphasisInline(node)
    return parent != null && BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[parent].containsAll(emphasisTypes)
}

private fun isAllEmphasized(selection: XMLSelection, emphasisType: EmphasisType): Boolean {
    var startNode = getFirstTextNode(selection.start.node)
    val endNode = getFinalTextNode(selection.end.node)
    val endNodeSelected = selection.end !is XMLTextCaret || selection.end.offset > 0
    if (startNode is Text && !MathModuleUtils.isMath(startNode) && !BBX.BLOCK.PAGE_NUM.isA(startNode.parent) && !BBX.SPAN.PAGE_NUM.isA(
            startNode.parent
        ) && !hasEmphasis(
            startNode, emphasisType
        )
        && !MathModuleUtils.isMath(startNode)
    ) {
        return false
    }
    //Iterate through all text nodes between startNode and endNode
    while (startNode !== endNode) {
        startNode = XMLHandler.followingVisitor(startNode) { n: Node -> n is Text || n === endNode }!!
        if (startNode !== endNode && !MathModuleUtils.isMath(startNode) && !BBX.BLOCK.PAGE_NUM.isA(startNode.parent) && !BBX.SPAN.PAGE_NUM.isA(
                startNode.parent
            ) && !hasEmphasis(startNode as Text, emphasisType)
        ) {
            return false
        }
    }
    return (!endNodeSelected || endNode !is Text
            || MathModuleUtils.isMath(startNode) || BBX.BLOCK.PAGE_NUM.isA(startNode.parent) || BBX.SPAN.PAGE_NUM.isA(
        startNode.parent
    ) || hasEmphasis(startNode as Text, emphasisType))
}

private fun getFirstTextNode(node: Node): Node {
    if (node is Text) {
        return node
    }
    return FastXPath.descendantOrSelf(node).filterIsInstance<Text>().firstOrNull() ?: node
}

private fun getFinalTextNode(node: Node): Node {
    if (node is Text) {
        return node
    }
    return FastXPath.descendantOrSelf(node).filterIsInstance<Text>().lastOrNull() ?: node
}

private fun hasEmphasis(node: Text, emphasisType: EmphasisType): Boolean {
    return hasEmphasis(node, EnumSet.of(emphasisType))
}
private fun toggleWithPreviousEmphasis(
    inlineElement: Element?,
    emphasisToSet: EnumSet<EmphasisType>,
    node: Text,
    start: Int,
    end: Int
): Text {
    /*
 * The text has emphasis, we need to find out if it is
 * toggled with our current action or previous actions.
 */
    val emphasisBits = BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[inlineElement]
    val emphasisBitsToggled = EnumSet.copyOf(emphasisBits)
    for (emphasis in emphasisToSet) {
        val toggleOn = !emphasisBits.contains(emphasis)
        if (toggleOn) {
            emphasisBitsToggled.add(emphasis)
        } else {
            emphasisBitsToggled.remove(emphasis)
        }
    }
    val nodeToWrap: Text
    val inlineElementParent = inlineElement!!.parent as Element
    var insertionIndex = inlineElementParent.indexOf(inlineElement)
    /*
 * This whole block is pretty ugly, but each node needs to be changed
 * in a certain order to respect the current indexing and not have jumbled
 * text.
 *
 * Call process emphasis with EMPHASISBITS on text nodes that are not
 * toggling on the CURRENT emphasis action (they may have other, previous
 * emphasis)
 *
 * Call process emphasis with EMPHASISBITSTOGGLED on text nodes
 * that are toggling the CURRENT emphasis action, in addition to any previous
 * emphasis actions.
 */
    val nodeLength = node.value.length
    if ((start == 0 || start == NO_OFFSET) && (end == nodeLength || end == NO_OFFSET)) {
        //Simple toggle the whole thing
        if (emphasisBitsToggled.isEmpty()) {
            // Remove unnessesary emphasis element
            XMLHandler.unwrapElement(inlineElement)
        } else {
            BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[inlineElement] = emphasisBitsToggled
        }
        return node
    } else if (start > 0 && end != NO_OFFSET && end != nodeLength) {
        val splitTextNode =
            node.splitNode(start, end)
        insertionIndex++
        processEmphasis(emphasisBits, splitTextNode[0], inlineElementParent, insertionIndex)
        insertionIndex++
        processEmphasis(emphasisBitsToggled, splitTextNode[1], inlineElementParent, insertionIndex)
        insertionIndex++
        processEmphasis(emphasisBits, splitTextNode[2], inlineElementParent, insertionIndex)
        nodeToWrap = splitTextNode[1]
    } else if (start > 0) {
        val splitTextNode =
            node.splitNode(start)
        nodeToWrap = splitTextNode[1]
        insertionIndex++
        processEmphasis(emphasisBits, splitTextNode[0], inlineElementParent, insertionIndex)
        insertionIndex++
        processEmphasis(emphasisBitsToggled, splitTextNode[1], inlineElementParent, insertionIndex)
    } else { // start == 0 || start == NO_OFFSET && (end != NO_OFFSET && end != nodeLength)
        val splitTextNode = node.splitNode(end)
        nodeToWrap = splitTextNode[0]
        insertionIndex++
        processEmphasis(emphasisBitsToggled, splitTextNode[0], inlineElementParent, insertionIndex)
        insertionIndex++
        processEmphasis(emphasisBits, splitTextNode[1], inlineElementParent, insertionIndex)
    }
    return nodeToWrap
}
/**
 * @param emphasisBits to be toggled on in the new emphasis element
 * @param text node to be wrapped in the new emphasis element
 * @param parent of emphasis element to be created
 * @param index of new emphasis element in parent's children
 * @return the parent, transformed with new emphasis child
 * Wraps text node in new emphasis element, inserts in correct index, returns
 * the modified parent.
 */
private fun processEmphasis(
    emphasisBits: EnumSet<EmphasisType>,
    text: Text,
    parent: Element,
    index: Int
): Element {
    text.detach()
    if (emphasisBits.isEmpty()) {
        // Don't add an unnessesary emphasis element
        parent.insertChild(text, index)
    } else {
        val newEmphasisWrapper = BBX.INLINE.EMPHASIS.create(emphasisBits)
        newEmphasisWrapper.appendChild(text)
        parent.insertChild(newEmphasisWrapper, index)
    }
    return parent
}
private fun applyEmphasis(emphasisToSet: EnumSet<EmphasisType>, node: Text, start: Int, end: Int): Text {
    require(start >= NO_OFFSET) { "Unexpected start $start" }
    require(end >= NO_OFFSET) { "Unexpected end $end" }
    val inlineElement = node.parent as Element
    log.debug("Emphasising text {}", node)
    return if (!BBX.INLINE.EMPHASIS.isA(inlineElement)) {
        toggleNoPreviousEmphasis(emphasisToSet, node, start, end)
    } else {
        toggleWithPreviousEmphasis(inlineElement, emphasisToSet, node, start, end)
    }
}
private fun toggleNoPreviousEmphasis(
    emphasisToSet: EnumSet<EmphasisType>,
    node: Text,
    start: Int,
    end: Int
): Text {
    /*
 * Currently no emphasis of any kind.
 * Toggle on mode, split up text if needed and wrap
 */
    val nodeToWrap: Text
    val nodeLength = node.value.length
    nodeToWrap = if (start > 0 && end != NO_OFFSET && end != nodeLength) {
        //get middle and wrap
        val splitTextNode =
            node.splitNode(start, end)
        splitTextNode[1]
    } else if (start > 0) {
        //get last and wrap
        val splitTextNode =
            node.splitNode(start)
        splitTextNode[1]
    } else if (end != NO_OFFSET && end != nodeLength) {
        //get beginning and wrap
        val splitTextNode = node.splitNode(end)
        splitTextNode[0]
    } else {
        //wrap all
        node
    }
    XMLHandler.wrapNodeWithElement(
        nodeToWrap,
        BBX.INLINE.EMPHASIS.create(emphasisToSet)
    )
    return nodeToWrap
}
object BoldTool : EmphasisMenuTool {
    override val emphasis = EmphasisItem.BOLD
    override val accelerator = SWT.MOD1 or 'B'.code
}
object ItalicsTool : EmphasisMenuTool {
    override val emphasis = EmphasisItem.ITALIC
    override val accelerator = SWT.MOD1 or 'I'.code
}
object UnderlineTool : EmphasisMenuTool {
    override val emphasis = EmphasisItem.UNDERLINE
    override val accelerator = SWT.MOD1 or 'U'.code
}
object ScriptTool : EmphasisMenuTool {
    override val emphasis = EmphasisItem.SCRIPT
    override val accelerator = SWT.MOD3 or SWT.MOD2 or 'S'.code
}
object TnSymbolsTool : EmphasisMenuTool {
    override val emphasis = EmphasisItem.TNSYMBOLS
}