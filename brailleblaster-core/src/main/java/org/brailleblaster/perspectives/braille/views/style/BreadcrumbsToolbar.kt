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
package org.brailleblaster.perspectives.braille.views.style

import jakarta.xml.bind.JAXBElement
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.toolbar.CustomToolBarBuilder.Companion.userDefined
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.XMLNodeCaret
import org.brailleblaster.perspectives.mvc.events.BuildToolBarEvent
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.settings.UTDManager
import org.brailleblaster.utd.Style
import org.brailleblaster.utd.config.UTDConfig
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.util.Utils.runtimeToString
import org.brailleblaster.utils.xml.BB_NS
import org.brailleblaster.utils.xml.UTD_NS
import org.brailleblaster.utils.swt.AccessibilityUtils.prependName
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getBanaStyles
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.accessibility.ACC
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class BreadcrumbsToolbar(private val manager: Manager) : SimpleListener {
    private var wrapper: Composite? = null

    override fun onEvent(event: SimpleEvent) {
        if (event is BuildToolBarEvent) {
            if (enabled) {
                WPManager.getInstance().currentPerspective
                    .addToToolBar(userDefined { parent: Composite? -> this.buildToolbar(parent) })
            }
        } else if (event is XMLCaretEvent && enabled) {
            updateOutput()
        }
    }

    private fun buildToolbar(parent: Composite?) {
        //99 is an arbitrary high nesting that no real book should reach
        (parent!!.layout as GridLayout).numColumns = 99
        wrapper = parent
        updateOutput()
    }

    private fun updateOutput() {
        val startTime = System.currentTimeMillis()
        //While opening the widget might not be initted yet
        if (wrapper == null || wrapper!!.isDisposed) {
            return
        } else if (manager.simpleManager.isSelectionNotSet) {
            return
            // RT 6434 opening BB, opening book, reduce window size
        }

        for (child in wrapper!!.children) {
            child.dispose()
        }

        val oldDescription = crumb_string
        val crumbsBuilder = StringBuilder()
        val caret = manager.simpleManager.currentCaret.node

        val ancestors = FastXPath.ancestor(caret).reversed().also {
            if ((caret is Element) && caret.namespaceURI == BB_NS) {
                it + caret
            }
        }
        var running = false
        var counter = 0
        val iterator: Iterator<Element> = ancestors.iterator()
        while (iterator.hasNext()) {
            val curAncestor = iterator.next()
            log.trace(
                "CurAncestor: {}",
                XMLHandler.toXMLStartTag(curAncestor)
            )
            if (!running) {
                if (BBX.SECTION.ROOT.isA(curAncestor)) {
                    running = true
                }
                continue
            }

            val type = BBX.getType(curAncestor)
            val subtype = type.getSubType(curAncestor)
            var elemType: String
            /*
			Custom displaying of individual elements goes here
			 */
            elemType = if (subtype === BBX.SECTION.OTHER) {
                //Almost everything is a SECTION-OTHER by default
                type.name + (if (BBX._ATTRIB_ORIGINAL_ELEMENT.has(curAncestor)
                ) " " + BBX._ATTRIB_ORIGINAL_ELEMENT[curAncestor]
                else "")
            } else if (subtype === BBX.CONTAINER.BOX) {
                type.name + " " + getStyleLong(curAncestor)
            } else if (type === BBX.BLOCK) {
                //All blocks are styled and the subtypes are just filler
                //Used to just filter BBX.BLOCK.DEFAULT
                type.name
            } else if (subtype === BBX.INLINE.EMPHASIS) {
                //Pretty prefix
                "Emphasis"
            } else if (subtype === BBX.SPAN.PAGE_NUM) {
                "SPAN Print Page"
            } else {
                type.name + "-" + subtype.name
            }

            //Add style name and options
            if (BBX.BLOCK.PAGE_NUM === subtype) {
                elemType += " Print Page"
            } else if (type === BBX.BLOCK) {
                elemType += " " + getStyleLong(curAncestor)
            }

            var emphasisBits: EnumSet<EmphasisType>? = null
            if (subtype === BBX.INLINE.EMPHASIS
                && BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[curAncestor].also { emphasisBits = it }.size == 1
            ) {
                elemType += " " + emphasisBits!!.iterator().next().longName
            } else if (BBX.CONTAINER.LIST.isA(curAncestor) && (curAncestor.getAttribute("utd-style") != null && curAncestor.getAttributeValue(
                    "utd-style"
                ) == "Poetic Stanza")
            ) {
                elemType = BBX.CONTAINER.name + " - Poetic Stanza"
            }

            val button = Button(wrapper, SWT.NONE)
            button.text = elemType
            if (elemType.uppercase(Locale.getDefault()).contains("MATTER")) {
                button.isEnabled = false
            }
            if (!(elemType.uppercase(Locale.getDefault()).contains("MATTER") ||
                        elemType.uppercase(Locale.getDefault()).contains("SECTION"))
            ) {
                crumbsBuilder.append(" ").append(elemType.replace("BLOCK".toRegex(), ""))
            }
            EasySWT.addSwtBotKey(button, SWTBOT_ANCESTOR_PREFIX + counter++)
            EasySWT.addSelectionListener(button) { it: SelectionEvent ->
                manager.waitForFormatting(true)
                manager.simpleManager.dispatchEvent(XMLCaretEvent(Sender.BREADCRUMBS, XMLNodeCaret(curAncestor)))
                manager.textView.forceFocus()
            }

            prependName(button, NAME)

            //Issue #5027: Allow selection of all text with similar emphasis
            if (subtype === BBX.INLINE.EMPHASIS && emphasisBits!!.size != 1) {
                for (curEmphasis in emphasisBits) {
                    val emphasisButton = Button(wrapper, SWT.NONE)
                    emphasisButton.text = curEmphasis.longName
                    EasySWT.addSwtBotKey(emphasisButton, SWTBOT_ANCESTOR_PREFIX + (counter - 1) + curEmphasis)
                    log.trace("Added {}", emphasisButton.getData(EasySWT.SWTBOT_WIDGET_KEY))
                    EasySWT.addSelectionListener(emphasisButton) { it: SelectionEvent ->
                        selectSurroundingEmphasis(curAncestor, curEmphasis)
                        manager.textView.forceFocus()
                    }
                }
            }

            if (BBX.CONTAINER.TABLE.isA(curAncestor) || BBX.INLINE.MATHML.isA(curAncestor)) {
                //These elements have their own editor
                //Showing any more exposes unnecessary implementation details
                log.debug("Stopping early for special elements")
                break
            } else if (iterator.hasNext()) {
                val label = Label(wrapper, SWT.CENTER)
                label.text = ">"
            }
        }
        crumb_string = crumbsBuilder.toString()
        if (oldDescription != crumb_string) {
            manager.textView.accessible.sendEvent(ACC.EVENT_DESCRIPTION_CHANGED, null)
            manager.brailleView.accessible.sendEvent(ACC.EVENT_DESCRIPTION_CHANGED, null)
        }
        wrapper!!.layout(true)
        log.info("Building Breadcrumbs took {}", runtimeToString(startTime))
    }

    private fun getStyleLong(block: Element): String {
        val style = manager.document.settingsManager.engine.getStyle(block) as Style?
            ?: //EG PageNum Blocks
            return ""
        //throw new NodeException("Block with no style/", ancestorBlock);
        var styleName = StringBuilder(getBanaStyles()[style.name])

        //Handle document specific styles with changed style options
        if (styleName.toString().startsWith(UTDManager.DOCUMENT_STYLE_NAME_PREFIX)) {
            var baseStyle: Style? = style
            while (baseStyle != null && baseStyle.name.startsWith(UTDManager.DOCUMENT_STYLE_NAME_PREFIX)) {
                baseStyle = baseStyle.baseStyle
            }
            styleName = StringBuilder(baseStyle?.name ?: "")

            /*
			TODO: Any way to do this without reflection?
			TODO: This heavily depends on the current Style impl of JAXB
			 */
            for (curField in UTDConfig.STYLE_OPTION_FIELDS.values) {
                curField.isAccessible = true
                var curValue: JAXBElement<*>?
                var baseValue: JAXBElement<*>?
                try {
                    curValue = curField[style] as JAXBElement<*>?
                    baseValue = if (baseStyle != null) curField[baseStyle] as JAXBElement<*>? else null
                } catch (e: Exception) {
                    throw RuntimeException("Failed to get field $curField", e)
                }

                //Style options are inherited so don't worry about null values in current style
                if (curValue != null && (baseValue == null || curValue.value !== baseValue.value)) {
                    styleName.append(" ").append(curField.name).append("=").append(curValue.value)
                }
            }
        }

        //Handle the pageSide attrib which doesn't change the style name
        val pageSide = block.getAttributeValue("pageSide")
        if (pageSide != null) {
            styleName.append(" pageSide=").append(pageSide)
        }

        if (style.isLineNumber) {
            styleName = StringBuilder(" Line Number = ")
            if (block.getAttribute("printLineNum") != null) {
                styleName.append(block.getAttributeValue("printLineNum"))
            } else if (block.getAttribute("linenum") != null) {
                styleName.append(block.getAttributeValue("linenum"))
            }
        }

        if (block.getAttribute("skipLines", UTD_NS) != null) {
            styleName.append(" Skip Lines = ").append(BBX.BLOCK.IMAGE_PLACEHOLDER.ATTRIB_SKIP_LINES[block])
        }

        return styleName.toString()
    }

    private fun selectSurroundingEmphasis(needleElement: Element, emphasisBit: EmphasisType) {
        val block = XMLHandler.ancestorVisitorFatal(needleElement) { node: Element? -> BBX.BLOCK.isA(node) }

        var startNode: Node? = needleElement
        log.trace("start {}", needleElement.toXML())
        for (curNode in FastXPath.preceding(needleElement)) {
            if (isUnusableNode(curNode)) {
                continue
            }
            if (isEmphasisInBlock(curNode, emphasisBit, block)) {
                //curNode is a valid text node inside an emphasis
                log.trace("moving start {}", curNode.toXML())
                startNode = curNode
            } else {
                break
            }
        }

        var endNode: Node? = needleElement
        log.trace("end {}", needleElement.toXML())
        for (curNode in FastXPath.following(needleElement)) {
            if (isUnusableNode(curNode)) {
                continue
            }
            log.trace("Testing {}", curNode)
            if (isEmphasisInBlock(curNode, emphasisBit, block)) {
                //curNode is a valid text node inside an emphasis
                log.trace("moving end {}", curNode.toXML())
                endNode = curNode
            } else {
                break
            }
        }

        manager.simpleManager.dispatchEvent(
            XMLCaretEvent(
                Sender.BREADCRUMBS,
                XMLNodeCaret(startNode!!),
                XMLNodeCaret(endNode!!)
            )
        )
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BreadcrumbsToolbar::class.java)
        const val SWTBOT_ANCESTOR_PREFIX: String = "breadcrumbs.ancestorButton"
        var enabled: Boolean = true
        var crumb_string: String = ""
        private const val NAME = "Breadcrumb" //Name attached to each button

        private fun isUnusableNode(curNode: Node): Boolean {
            return (curNode is Text
                    || XMLHandler.ancestorElementIs(curNode) { node: Element? -> UTDElements.BRL.isA(node) })
        }

        private fun isEmphasisInBlock(curNode: Node, emphasisBit: EmphasisType, block: Element): Boolean {
            return (XMLHandler.ancestorElementIs(
                curNode
            ) { node: Element? ->
                BBX.INLINE.EMPHASIS.isA(node)
                        && BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[node].contains(emphasisBit)
            }
                    && XMLHandler.ancestorElementIs(curNode) { curAncestor: Element -> curAncestor === block })
        }
    }
}
