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
package org.brailleblaster.bbx

import nu.xom.Attribute
import nu.xom.Document
import nu.xom.Element
import nu.xom.Text
import org.brailleblaster.settings.DefaultNimasMaps
import org.brailleblaster.testrunners.TestXMLUtils
import org.brailleblaster.testrunners.XMLElementAssert
import org.brailleblaster.utd.IStyleMap
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.properties.EmphasisType
import org.testng.ITestContext
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.util.EnumSet

class BookToBBXConverterTest {
    private lateinit var converter: BookToBBXConverter
    private lateinit var maps: DefaultNimasMaps

    @BeforeClass
    fun init(context: ITestContext) {
        converter = BookToBBXConverter.fromConfig()
        maps = DefaultNimasMaps()
        BookToBBXConverter.devSetup(arrayOf())
        //Don't stop on errors when doing bulk testing
//		if (context.getAllTestMethods().length == 1) {
//			BookToBBXConverter.DEBUG_LEVEL.fancySWTWait = true;
//			BookToBBXConverter.DEBUG = BookToBBXConverter.DEBUG_LEVEL.UNTIL_TRIGGER;
//		} else {
//			BookToBBXConverter.DEBUG = BookToBBXConverter.DEBUG_LEVEL.NONE;
//		}
    }

    // -------------------- Simple -------------------
    @Test
    fun levelSimpleAndCopyAttrute() {
        convertAndAssertFirst("<level1 utd:customId='test'><p moreCustom='attribs'>test</p></level1>")
            .isSection(BBX.SECTION.OTHER)
            .hasAttributeUTD("customId", "test")
            .nextChildIs { childAssert ->
                childAssert
                    .hasAttribute("moreCustom", "attribs")
                    .hasText("test")
            }
    }

    @Test
    fun paragraphSimple() {
        convertAndAssertFirst("<p>test</p>")
            .isBlockDefaultStyle("Body Text")
            .hasText("test")
    }

    @Test
    fun listSimple() {
        convertAndAssertFirst("<list><li>item 1</li><li>item 2</li></list>")
            .isContainerListType(BBX.ListType.NORMAL)
            .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 0)
            .nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L1-3")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("item 1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L1-3")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("item 2")
            }.noNextChild()
    }

    @Test
    fun captionAndParagraph() {
        convertAndAssertFirst("<caption><p>test</p><p>test2</p></caption>")
            .isContainer(BBX.CONTAINER.CAPTION)
            .nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Caption")
                    .hasText("test")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Caption")
                    .hasText("test2")
            }.noNextChild()
    }

    @Test
    fun sidebar_removed_issue5265() {
        convertAndAssert("<sidebar><p>test</p><p>test2</p></sidebar>")
            .nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Body Text")
                    .hasText("test")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Body Text")
                    .hasText("test2")
            }.noNextChild()
    }

    // --------------------- Complicated lists -----------------------
    @Test
    fun listItemNestedParagraphs() {
        convertAndAssertFirst(
            "<list>"
                + "<li>item 1</li>"
                //text nodes after...
                + "<li><p>item 2.1</p><p>item 2.2</p>item outside1</li>"
                //and before paragraph tags are blocks themselves
                + "<li>item outside2<p>item 3.1</p><p>item 3.2</p></li>"
                //Regular nested paragraphs
                + "<li><p>item 4.1</p><p>item 4.2</p></li>"
                + "</list>"
        )
            .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 0)
            .isContainerListType(BBX.ListType.NORMAL)
            .nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L1-3")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("item 1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L1-3")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("item 2.1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L1-3")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("item 2.2")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L1-3")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("item outside1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L1-3")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("item outside2")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L1-3")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("item 3.1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L1-3")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("item 3.2")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L1-3")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("item 4.1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L1-3")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("item 4.2")
            }.noNextChild()
    }

    @Test
    fun listItemNestedList() {
        convertAndAssertFirst(
            "<list>"
                + "<li>item 1</li>"
                //text nodes after...
                + "<li><list><li>item 2.1</li><li>item 2.2</li></list>item outside1</li>"
                //and before paragraph tags are blocks themselves
                + "<li>item outside2<list><li>item 3.1</li><li>item 3.2</li></list></li>"
                //Regular nested lists
                + "<li><list><li>item 4.1</li><li>item 4.2</li></list></li>"
                + "</list>"
        )
            .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 1)
            .isContainerListType(BBX.ListType.NORMAL)
            .nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L1-5")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("item 1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L3-5")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                    .hasText("item 2.1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L3-5")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                    .hasText("item 2.2")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L1-5")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("item outside1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L1-5")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("item outside2")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L3-5")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                    .hasText("item 3.1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L3-5")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                    .hasText("item 3.2")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L3-5")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                    .hasText("item 4.1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L3-5")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                    .hasText("item 4.2")
            }.noNextChild()
    }

    @Test
    fun listsWithListItemWrapperParagraph() {
        //Nested paragraphs do not incriment level, containers in list are taken out
        convertAndAssert(
            "<list>"
                + "<li><p>term1</p><p>term2</p></li>"
                + "<div><list>"
                + "<li><p>term3.1</p><p>term3.2</p><p>term3.3</p></li>"
                + "<li>term4</li>"
                + "</list></div>"
                + "<li>term5</li>"
                + "</list>"
        )
            .nextChildIs { childAssert ->
                childAssert
                    .isContainerListType(BBX.ListType.NORMAL)
                    .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 0)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasStyle("L1-3")
                            .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                            .hasText("term1")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasStyle("L1-3")
                            .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                            .hasText("term2")
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainer(BBX.CONTAINER.OTHER)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isContainerListType(BBX.ListType.NORMAL)
                            .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 0)
                            .nextChildIs { childAssert3 ->
                                childAssert3
                                    .isBlock(BBX.BLOCK.LIST_ITEM)
                                    .hasStyle("L1-3")
                                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                                    .hasText("term3.1")
                            }.nextChildIs { childAssert3 ->
                                childAssert3
                                    .isBlock(BBX.BLOCK.LIST_ITEM)
                                    .hasStyle("L1-3")
                                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                                    .hasText("term3.2")
                            }.nextChildIs { childAssert3 ->
                                childAssert3
                                    .isBlock(BBX.BLOCK.LIST_ITEM)
                                    .hasStyle("L1-3")
                                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                                    .hasText("term3.3")
                            }.nextChildIs { childAssert3 ->
                                childAssert3
                                    .isBlock(BBX.BLOCK.LIST_ITEM)
                                    .hasStyle("L1-3")
                                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                                    .hasText("term4")
                            }.noNextChild()
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainerListType(BBX.ListType.NORMAL)
                    .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 0)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasStyle("L1-3")
                            .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                            .hasText("term5")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test
    fun listsWithListItem_NestedParagraph() {
        //Nested paragraphs do not incriment level, containers in list are taken out
        convertAndAssertFirst(
            "<list>"
                + "<li><p>term1</p><p>term2</p></li>"
                + "<list>"
                + "<li><p>term3.1</p><p>term3.2</p><p>term3.3</p></li>"
                + "<li>term4</li>"
                + "</list>"
                + "<li>term5</li>"
                + "</list>"
        )
            .isContainerListType(BBX.ListType.NORMAL)
            .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 1)
            .nextChildIs { childAssert2 ->
                childAssert2
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L1-5")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("term1")
            }.nextChildIs { childAssert2 ->
                childAssert2
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L1-5")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("term2")
            }.nextChildIs { childAssert3 ->
                childAssert3
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L3-5")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                    .hasText("term3.1")
            }.nextChildIs { childAssert3 ->
                childAssert3
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L3-5")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                    .hasText("term3.2")
            }.nextChildIs { childAssert3 ->
                childAssert3
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L3-5")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                    .hasText("term3.3")
            }.nextChildIs { childAssert3 ->
                childAssert3
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L3-5")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                    .hasText("term4")
            }.nextChildIs { childAssert3 ->
                childAssert3
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("L1-5")
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("term5")
            }.noNextChild()
    }

    @Test
    fun listsWithListItemWrapper() {
        //containers in list are taken out
        convertAndAssert(
            "<list>"
                + "<li><list><li>term1</li><li>term2</li></list></li>"
                + "<div><list>"
                + "<li><list><li>term3.1</li><li>term3.2</li><li>term3.3</li></list></li>"
                + "<li>term4</li>"
                + "</list></div>"
                + "<li>term5</li>"
                + "</list>"
        )
            .nextChildIs { childAssert ->
                childAssert
                    .isContainerListType(BBX.ListType.NORMAL)
                    .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 1)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasStyle("L3-5")
                            .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                            .hasText("term1")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasStyle("L3-5")
                            .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                            .hasText("term2")
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainer(BBX.CONTAINER.OTHER)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isContainerListType(BBX.ListType.NORMAL)
                            .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 1)
                            .nextChildIs { childAssert3 ->
                                childAssert3
                                    .isBlock(BBX.BLOCK.LIST_ITEM)
                                    .hasStyle("L3-5")
                                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                                    .hasText("term3.1")
                            }.nextChildIs { childAssert3 ->
                                childAssert3
                                    .isBlock(BBX.BLOCK.LIST_ITEM)
                                    .hasStyle("L3-5")
                                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                                    .hasText("term3.2")
                            }.nextChildIs { childAssert3 ->
                                childAssert3
                                    .isBlock(BBX.BLOCK.LIST_ITEM)
                                    .hasStyle("L3-5")
                                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                                    .hasText("term3.3")
                            }.nextChildIs { childAssert3 ->
                                childAssert3
                                    .isBlock(BBX.BLOCK.LIST_ITEM)
                                    .hasStyle("L1-5")
                                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                                    .hasText("term4")
                            }.noNextChild()
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainerListType(BBX.ListType.NORMAL)
                    .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 0)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasStyle("L1-3")
                            .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                            .hasText("term5")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test
    fun listTripleNested() {
        convertAndAssertFirst(
            "<list><li>level 1</li>"
                + "<li>level 1 w/ nested"
                + "<list><li>level 2</li>"
                + "<li>level 2 w/ nested"
                + "<list><li>level 3</li>"
                + "</list></li><li>level 2.1</li></list></li></list>"
        )
            .isContainerListType(BBX.ListType.NORMAL)
            .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 2)
            .nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("level 1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                    .hasText("level 1 w/ nested")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                    .hasText("level 2")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                    .hasText("level 2 w/ nested")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 2)
                    .hasText("level 3")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                    .hasText("level 2.1")
            }.noNextChild()
    }

    /**
     * Actual literature book edge case
     */
    @Test
    fun listInsideListInsideProdnoteInsideImageGroupInsideList() {
        convertAndAssert(
            "<list><li>level 1</li>"
                + "<li>level 1 w/ nested"
                + "<imggroup>"
                + "blocked text"
                + "<prodnote>"
                //2
                + "<list>"
                + "<li>level 2</li>"
                + "<li>level 2 w/ nested"
                //3
                + "<list>"
                + "<li>level 3</li>"
                + "</list>"
                //end3
                + "</li>"
                + "</list>"
                //end2
                + "</prodnote></imggroup>"
                + "</li><li>level 1.2</li></list>"
        )
            .nextChildIs { childAssert ->
                childAssert
                    .isContainerListType(BBX.ListType.NORMAL)
                    .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 0)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                            .hasText("level 1")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                            .hasText("level 1 w/ nested")
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainer(BBX.CONTAINER.IMAGE)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlockDefaultStyle("Caption")
                            .hasText("blocked text")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isContainer(BBX.CONTAINER.PRODNOTE)
                            .nextChildIs { childAssert3 ->
                                childAssert3
                                    .isContainerListType(BBX.ListType.NORMAL)
                                    .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 1)
                                    .nextChildIs { childAssert4 ->
                                        childAssert4
                                            .isBlock(BBX.BLOCK.LIST_ITEM)
                                            .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                                            .hasText("level 2")
                                    }.nextChildIs { childAssert4 ->
                                        childAssert4
                                            .isBlock(BBX.BLOCK.LIST_ITEM)
                                            .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                                            .hasText("level 2 w/ nested")
                                    }.nextChildIs { childAssert4 ->
                                        childAssert4
                                            .isBlock(BBX.BLOCK.LIST_ITEM)
                                            .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                                            .hasText("level 3")
                                    }.noNextChild()
                            }.noNextChild()
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainerListType(BBX.ListType.NORMAL)
                    //TODO: This should be 2 and 1, copied from the origional list
                    //.hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 2)
                    .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 0)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            //.hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 1)
                            .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                            .hasText("level 1.2")
                    }.noNextChild()
            }.noNextChild()
    }

    /**
     * Actual literature book edge case
     */
    @Test
    fun listItemWithImgggroup() {
        convertAndAssert(
            "<list>"
                + "<li>before</li>"
                + "<li><list>"
                + "<li>nested before</li>"
                + "<li>Text <imggroup><img src='puppies.jpg'/><prodnote>hahah</prodnote><prodnote>number2</prodnote></imggroup> Text After</li>"
                + "<li>nested after</li>"
                + "</list></li>"
                + "<li>after</li>"
                + "</list>"
        )
            .nextChildIs { childAssert ->
                childAssert
                    .isContainerListType(BBX.ListType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasStyle("L1-5")
                            .hasText("before")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasStyle("L3-5")
                            .hasText("nested before")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasStyle("L3-5")
                            .hasText("Text")
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainer(BBX.CONTAINER.IMAGE)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isContainer(BBX.CONTAINER.PRODNOTE)
                            .nextChildIs { childAssert3 ->
                                childAssert3
                                    .hasText("hahah")
                            }.noNextChild()
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isContainer(BBX.CONTAINER.PRODNOTE)
                            .nextChildIs { childAssert3 ->
                                childAssert3
                                    .hasText("number2")
                            }.noNextChild()
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainerListType(BBX.ListType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasStyle("L3-5")
                            .hasText("Text After")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasStyle("L3-5")
                            .hasText("nested after")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasStyle("L1-5")
                            .hasText("after")
                    }.noNextChild()
            }.noNextChild()
    }

    //Definition lists
    @Test
    fun definitionListTest() {
        convertAndAssertFirst(
            "<dl>"
                + "<dt>term1</dt><dd>def1</dd>"
                + "<dt>term2</dt><dd>def2</dd>"
                + "</dl>"
        )
            .isContainerListType(BBX.ListType.DEFINITION)
            .nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("G1-3")
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isSpan(BBX.SPAN.DEFINITION_TERM)
                            .hasText("term1")
                    }.nextChildIsText("def1")
                    .noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("G1-3")
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isSpan(BBX.SPAN.DEFINITION_TERM)
                            .hasText("term2")
                    }.nextChildIsText("def2")
                    .noNextChild()
            }.noNextChild()
    }

    @Test
    fun definitionSpaceBetweenTest() {
        val doc = TestXMLUtils.generateBookDoc(
            "", "<dl>"
                + "<dt>term1</dt> <dd>def1</dd>"
                + "<dt>term2</dt> <dd>def2</dd>"
                + "</dl>"
        )
        convertAndAssert(doc)
            .childCount(1)
            .child(0)
            .isContainerListType(BBX.ListType.DEFINITION)
            .nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("G1-3")
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isSpan(BBX.SPAN.DEFINITION_TERM)
                            .hasText("term1")
                    }.nextChildIsText("def1")
                    .noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("G1-3")
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isSpan(BBX.SPAN.DEFINITION_TERM)
                            .hasText("term2")
                    }.nextChildIsText("def2")
                    .noNextChild()
            }.noNextChild()
    }

    @Test
    fun definitionListParagraphTest() {
        convertAndAssertFirst(
            "<dl>"
                + "<dt>term1</dt><dd><p>def1</p></dd>"
                + "<dt>term2</dt><dd><p>def2</p></dd>"
                + "</dl>"
        )
            .isContainerListType(BBX.ListType.DEFINITION)
            .nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("G1-3")
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isSpan(BBX.SPAN.DEFINITION_TERM)
                            .hasText("term1")
                    }.nextChildIsText("def1")
                    .noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("G1-3")
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isSpan(BBX.SPAN.DEFINITION_TERM)
                            .hasText("term2")
                    }.nextChildIsText("def2")
                    .noNextChild()
            }.noNextChild()
    }

    @Test
    fun definitionListMultiParagraphTest() {
        convertAndAssertFirst(
            "<dl>"
                + "<dt>term1</dt><dd><p>def1</p><p>def1.2</p></dd>"
                + "<dt>term2</dt><dd><p>def2</p><p>def2.2</p></dd>"
                + "</dl>"
        )
            .isContainerListType(BBX.ListType.DEFINITION)
            .nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("G1-3")
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isSpan(BBX.SPAN.DEFINITION_TERM)
                            .hasText("term1")
                    }.nextChildIsText("def1")
                    .noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("G1-3")
                    .hasText("def1.2")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("G1-3")
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isSpan(BBX.SPAN.DEFINITION_TERM)
                            .hasText("term2")
                    }.nextChildIsText("def2")
                    .noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("G1-3")
                    .hasText("def2.2")
            }.noNextChild()
    }

    @Test
    fun definitionListLineBreakTest_issue5390() {
        convertAndAssertFirst(
            "<dl>"
                + "<dt>term1</dt><dd>def1<br/>def1.2<br/>def1.3</dd>"
                + "<dt>term2</dt><dd><br/>def2</dd>"
                + "<dt>term3</dt><dd>def3<br/></dd>"
                + "</dl>"
        )
            .isContainerListType(BBX.ListType.DEFINITION)
            .nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("G1-5")
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isSpan(BBX.SPAN.DEFINITION_TERM)
                            .hasText("term1")
                    }.nextChildIsText("def1")
                    .noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("G3-5")
                    .hasText("def1.2")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("G3-5")
                    .hasText("def1.3")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("G1-5")
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isSpan(BBX.SPAN.DEFINITION_TERM)
                            .hasText("term2")
                    }.nextChildIsText("def2")
                    .noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasStyle("G1-5")
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isSpan(BBX.SPAN.DEFINITION_TERM)
                            .hasText("term3")
                    }.nextChildIsText("def3")
                    .noNextChild()
            }.noNextChild()
    }

    //---------------------- Span ----------------------------
    @Test
    fun anchorTest() {
        convertAndAssertFirst("<p><a href='#testing'>test</a></p>")
            .isBlockDefaultStyle("Body Text")
            .nextChildIs { childAssert ->
                childAssert
                    .isSpan(BBX.SPAN.OTHER)
                    .hasAttribute("href", "#testing")
                    .hasText("test")
            }.noNextChild()
    }

    //---------------------- Emphasis ---------------------------
    @Test
    fun emphasisTest() {
        convertAndAssertFirst("<p><strong>test</strong></p>")
            .isBlockDefaultStyle("Body Text")
            .nextChildIs { childAssert ->
                childAssert
                    .isInlineEmphasis(EmphasisType.BOLD)
                    .hasText("test")
            }.noNextChild()
    }

    @Test
    fun emphasisNestedSpanTest() {
        convertAndAssertFirst("<p><strong>before <a href='#test'>nested</a> after</strong></p>")
            .isBlockDefaultStyle("Body Text")
            .nextChildIs { childAssert ->
                childAssert
                    .isInlineEmphasis(EmphasisType.BOLD)
                    .hasText("before ")
            }.nextChildIs { anchorAssert ->
                anchorAssert
                    .isSpan(BBX.SPAN.OTHER)
                    .hasAttribute("href", "#test")
                    .nextChildIs { childAssert ->
                        childAssert
                            .isInlineEmphasis(EmphasisType.BOLD)
                            .hasText("nested")
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isInlineEmphasis(EmphasisType.BOLD)
                    .hasText(" after")
            }.noNextChild()
    }

    @Test
    fun emphasisNestedEmphasisTest() {
        convertAndAssertFirst("<p><strong>before <em>nested</em> after</strong></p>")
            .isBlockDefaultStyle("Body Text")
            .nextChildIs { childAssert ->
                childAssert
                    .isInlineEmphasis(EmphasisType.BOLD)
                    .hasText("before ")
            }.nextChildIs { childAssert ->
                childAssert
                    .isInlineEmphasis(EmphasisType.ITALICS, EmphasisType.BOLD)
                    .hasText("nested")
            }.nextChildIs { childAssert ->
                childAssert
                    .isInlineEmphasis(EmphasisType.BOLD)
                    .hasText(" after")
            }.noNextChild()
    }

    //---------------------- PageNum ------------------------
    @Test
    fun pagenumStandaloneTest() {
        convertAndAssert("<p>test1</p><pagenum some='attrib'>15</pagenum><p>test2</p>")
            .nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Body Text")
                    .hasText("test1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.PAGE_NUM)
                    .hasAttribute("some", "attrib")
                    .hasText("15")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Body Text")
                    .hasText("test2")
            }.noNextChild()
    }

    @Test
    fun pagenumNestedParagraphTest() {
        convertAndAssertFirst("<p>test1 <pagenum some='attrib'>15</pagenum> test2</p>")
            .isBlockDefaultStyle("Body Text")
            .nextChildIsText("test1 ")
            .nextChildIs { childAssert ->
                childAssert
                    .isSpan(BBX.SPAN.PAGE_NUM)
                    .hasAttribute("some", "attrib")
                    .hasText("15")
            }.nextChildIsText(" test2")
            .noNextChild()
    }

    @Test
    fun pagenumNoChildren() {
        convertAndAssert("<p>test1</p><pagenum some='attrib'/><p>test2</p>")
            .nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Body Text")
                    .hasText("test1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.PAGE_NUM)
                    .hasAttribute("some", "attrib")
                    .childCount(0)
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Body Text")
                    .hasText("test2")
            }.noNextChild()
    }

    @Test
    fun pagenumNestedSpanTest() {
        convertAndAssertFirst("<p>test1 <span>span<pagenum some='attrib'>15</pagenum>city</span> test2</p>")
            .isBlockDefaultStyle("Body Text")
            .nextChildIsText("test1 ")
            .nextChildIs { childAssert ->
                childAssert
                    .nextChildIsText("span")
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isSpan(BBX.SPAN.PAGE_NUM)
                            .hasAttribute("some", "attrib")
                            .hasText("15")
                    }.nextChildIsText("city")
            }.nextChildIsText(" test2")
            .noNextChild()
    }

    //---------------------- br/lineBreak --------------------
    @Test
    fun lineBreakParagraph() {
        convertAndAssert("<p>test1<br/>test2</p>")
            .nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Body Text")
                    .hasText("test1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Body Text")
                    .hasText("test2")
            }.noNextChild()
    }

    @Test
    fun lineBreakParagraphUseless() {
        convertAndAssert("<p><br/>test2</p>")
            .nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Body Text")
                    .hasText("test2")
            }.noNextChild()
    }

    @Test
    fun lineBreakParagraphEmphasis() {
        convertAndAssert("<p>this is <strong>super<br/>strong</strong></p>")
            .nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Body Text")
                    .nextChildIsText("this is ")
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isInlineEmphasis(EmphasisType.BOLD)
                            .hasText("super")
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Body Text")
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isInlineEmphasis(EmphasisType.BOLD)
                            .hasText("strong")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test
    fun lineBreakParagraphNestedEmphasis() {
        convertAndAssert("<p>this is <strong>super <em>italic <br/>strong</em></strong></p>")
            .nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Body Text")
                    .nextChildIsText("this is ")
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isInlineEmphasis(EmphasisType.BOLD)
                            .hasAttributeBB(BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS, EnumSet.of(EmphasisType.BOLD))
                            .hasText("super ")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isInlineEmphasis(EmphasisType.ITALICS, EmphasisType.BOLD)
                            .hasText("italic")
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Body Text")
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isInlineEmphasis(EmphasisType.ITALICS, EmphasisType.BOLD)
                            .hasText("strong")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test
    fun multipleLineBreaks() {
        convertAndAssert(
            "<p>"
                + "<span class='affiliation'>"
                + "before"
                + "<br/>"
                + "middle"
                + "<br/>"
                + "after"
                + "</span>"
                + "</p>"
        )
            .nextChildIs { p ->
                p
                    .isBlockDefaultStyle("Body Text")
                    .nextChildIs { span ->
                        span
                            .isSpan(BBX.SPAN.OTHER)
                            .hasText("before")
                    }.noNextChild()
            }.nextChildIs { p ->
                p
                    .isBlockDefaultStyle("Body Text")
                    .nextChildIs { span ->
                        span
                            .isSpan(BBX.SPAN.OTHER)
                            .hasText("middle")
                    }.noNextChild()
            }.nextChildIs { p ->
                p
                    .isBlockDefaultStyle("Body Text")
                    .nextChildIs { span ->
                        span
                            .isSpan(BBX.SPAN.OTHER)
                            .hasText("after")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test(enabled = false)
    fun lineBreakTableCell() {
        convertAndAssertFirst(
            "<table>"
                + "<tr><td>Line 1<br/>Line 2</td></tr>"
                //need 2 table columns otherwise table formatter considers it a non-table
                + "<tr><td>Line 3</td><td>more</td></tr>"
                + "</table>"
        )
            .isContainer(BBX.CONTAINER.TABLE)
            .nextChildIs { childAssert ->
                childAssert
                    .isContainerTableRowType(BBX.TableRowType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .nextChildIsText("Line 1 Line 2")
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainerTableRowType(BBX.TableRowType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .nextChildIsText("Line 3")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .nextChildIsText("more")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test
    fun lineBreakListItem() {
        convertAndAssertFirst(
            "<list>"
                + "<li>term1<br/>term2</li>"
                + "<li>test</li>"
                + "</list>"
        )
            .isContainerListType(BBX.ListType.NORMAL)
            .nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasText("term1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasText("term2")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasText("test")
            }.noNextChild()
    }

    @Test
    fun lineBreakListItemWithParagraph() {
        convertAndAssertFirst(
            "<list>"
                + "<li><p>term1<br/>term2</p></li>"
                + "<li>test</li>"
                + "</list>"
        )
            .isContainerListType(BBX.ListType.NORMAL)
            .nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasText("term1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasText("term2")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasText("test")
            }.noNextChild()
    }

    @Test
    fun lineBreakListItemWithTwoParagraphsOneAfter() {
        convertAndAssertFirst(
            "<list>"
                + "<li><p>term1<br/>term2</p><p>after</p></li>"
                + "<li>test</li>"
                + "</list>"
        )
            .isContainerListType(BBX.ListType.NORMAL)
            .nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasText("term1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasText("term2")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasText("after")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasText("test")
            }.noNextChild()
    }

    @Test
    fun lineBreakListItemWithTwoParagraphsOneBefore() {
        convertAndAssertFirst(
            "<list>"
                + "<li><p>before</p><p>term1<br/>term2</p></li>"
                + "<li>test</li>"
                + "</list>"
        )
            .isContainerListType(BBX.ListType.NORMAL)
            .nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasText("before")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasText("term1")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasText("term2")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.LIST_ITEM)
                    .hasText("test")
            }.noNextChild()
    }

    //---------------------- Images ------------------
    @Test
    fun imageParagraph() {
        convertAndAssertFirst("<p>test <img src='evil.jpg'/> after</p>")
            .isBlock(BBX.BLOCK.DEFAULT)
            .hasStyle("Body Text")
            .nextChildIsText("test ")
            .nextChildIs { childAssert ->
                childAssert
                    .isSpan(BBX.SPAN.IMAGE)
                    .hasAttributeBB(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "evil.jpg")
                    .childCount(0)
            }.nextChildIsText(" after")
    }

    @Test
    fun imageParagraphWithAlt() {
        // Standalone image-only paragraph with alt text: the paragraph is promoted
        // to a CONTAINER.IMAGE, and a transcriber note block is created after it.
        convertAndAssert("<p><img src='x.jpg' alt='A photograph.'/></p>")
            .nextChildIs { childAssert ->
                childAssert
                    .isContainer(BBX.CONTAINER.IMAGE)
                    .hasAttributeBB(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "x.jpg")
                    .childCount(0)
            }.nextChildIs { tnAssert ->
                tnAssert
                    .isBlock(BBX.BLOCK.STYLE)
                    .hasOverrideStyle("7-5")
                    .nextChildIs { inlineAssert ->
                        inlineAssert
                            .isInlineEmphasis(EmphasisType.TRANS_NOTE)
                            .hasText("A photograph.")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test
    fun imageParagraphInlineWithAlt() {
        // Image inline inside a paragraph with surrounding text and alt attribute:
        // a transcriber note block should be inserted after the parent paragraph.
        convertAndAssert("<p>before <img src='x.jpg' alt='A photograph.'/> after</p>")
            .nextChildIs { childAssert ->
                childAssert
                    .isBlock(BBX.BLOCK.DEFAULT)
                    .hasStyle("Body Text")
                    .nextChildIsText("before ")
                    .nextChildIs { imgAssert ->
                        imgAssert
                            .isSpan(BBX.SPAN.IMAGE)
                            .hasAttributeBB(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "x.jpg")
                            .childCount(0)
                    }.nextChildIsText(" after")
                    .noNextChild()
            }.nextChildIs { tnAssert ->
                tnAssert
                    .isBlock(BBX.BLOCK.STYLE)
                    .hasOverrideStyle("7-5")
                    .nextChildIs { inlineAssert ->
                        inlineAssert
                            .isInlineEmphasis(EmphasisType.TRANS_NOTE)
                            .hasText("A photograph.")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test
    fun imageParagraphWithEmptyAlt() {
        // Empty alt attribute: no transcriber note should be created.
        convertAndAssertFirst("<p>text <img src='x.jpg' alt=''/> after</p>")
            .isBlock(BBX.BLOCK.DEFAULT)
            .hasStyle("Body Text")
            .nextChildIsText("text ")
            .nextChildIs { imgAssert ->
                imgAssert
                    .isSpan(BBX.SPAN.IMAGE)
                    .hasAttributeBB(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "x.jpg")
                    .childCount(0)
            }.nextChildIsText(" after")
            .noNextChild()
    }

    @Test
    fun imageGroupProdnoteWithNoParagraph() {
        convertAndAssert(
            "<imggroup>"
                + "<img src='evil.jpg'/><prodnote><strong>bold</strong> desc</prodnote>"
                + "<img src='evil2.jpg'/><prodnote>Another <strong>bolder</strong> long desc</prodnote>"
                + "</imggroup>"
        )
            .nextChildIs { childAssert ->
                childAssert
                    .isContainer(BBX.CONTAINER.IMAGE)
                    .hasAttributeBB(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "evil.jpg")
                    .nextChildIs { childAssert1 ->
                        childAssert1
                            .isContainer(BBX.CONTAINER.PRODNOTE)
                            .nextChildIs { childAssert2 ->
                                childAssert2
                                    .isBlock(BBX.BLOCK.DEFAULT)
                                    .hasStyle("Caption")
                                    .nextChildIs { childAssert3 ->
                                        childAssert3
                                            .isInlineEmphasis(EmphasisType.BOLD)
                                            .hasText("bold")
                                    }.nextChildIsText(" desc")
                                    .noNextChild()
                            }.noNextChild()
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainer(BBX.CONTAINER.IMAGE)
                    .hasAttributeBB(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "evil2.jpg")
                    .nextChildIs { childAssert1 ->
                        childAssert1
                            .isContainer(BBX.CONTAINER.PRODNOTE)
                            .nextChildIs { childAssert2 ->
                                childAssert2
                                    .isBlock(BBX.BLOCK.DEFAULT)
                                    .hasStyle("Caption")
                                    .nextChildIsText("Another ")
                                    .nextChildIs { childAssert3 ->
                                        childAssert3
                                            .isInlineEmphasis(EmphasisType.BOLD)
                                            .hasText("bolder")
                                    }.nextChildIsText(" long desc")
                                    .noNextChild()
                            }.noNextChild()
                    }.noNextChild()
            }.noNextChild()
    }

    @Test
    fun imageGroupProdnoteWithParagraph() {
        convertAndAssertFirst(
            "<imggroup>"
                + "<img src='evil.jpg'/><prodnote><p>I'm a <strong>bold</strong> desc</p><p>number 2</p></prodnote>"
                + "</imggroup>"
        )
            .isContainer(BBX.CONTAINER.IMAGE)
            .hasAttributeBB(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "evil.jpg")
            .nextChildIs { childAssert ->
                childAssert
                    .isContainer(BBX.CONTAINER.PRODNOTE)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.DEFAULT)
                            .hasStyle("Caption")
                            .nextChildIsText("I'm a ")
                            .nextChildIs { childAssert3 ->
                                childAssert3
                                    .isInlineEmphasis(EmphasisType.BOLD)
                                    .hasText("bold")
                            }.nextChildIsText(" desc")
                            .noNextChild()
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.DEFAULT)
                            .hasStyle("Caption")
                            .hasText("number 2")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test
    fun imageGroupMultipleImagesWithMultipleProdnoteAndCaption() {
        convertAndAssert(
            "<imggroup>"
                + "<img src='evil1.jpg'/>"
                + "<prodnote>prod1</prodnote>"
                + "<caption>prod2</caption>"
                + "<img src='evil2.jpg'/>"
                + "<prodnote><p>prod3</p><p>prod3.1</p></prodnote>"
                + "<caption>prod4</caption>"
                + "</imggroup>"
        )
            .nextChildIs { childAssert ->
                childAssert
                    .isContainer(BBX.CONTAINER.IMAGE)
                    .hasAttributeBB(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "evil1.jpg")
                    .nextChildIs { childAssert1 ->
                        childAssert1
                            .isContainer(BBX.CONTAINER.PRODNOTE)
                            .nextChildIs { childAssert2 ->
                                childAssert2
                                    .isBlock(BBX.BLOCK.DEFAULT)
                                    .hasStyle("Caption")
                                    .hasText("prod1")
                            }.noNextChild()
                    }.nextChildIs { childAssert1 ->
                        childAssert1
                            .nextChildIs { childAssert2 ->
                                childAssert2
                                    .isBlock(BBX.BLOCK.DEFAULT)
                                    .hasStyle("Caption")
                                    .hasText("prod2")
                            }.noNextChild()
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainer(BBX.CONTAINER.IMAGE)
                    .hasAttributeBB(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "evil2.jpg")
                    .nextChildIs { childAssert1 ->
                        childAssert1
                            .isContainer(BBX.CONTAINER.PRODNOTE)
                            .nextChildIs { childAssert2 ->
                                childAssert2
                                    .isBlock(BBX.BLOCK.DEFAULT)
                                    .hasStyle("Caption")
                                    .hasText("prod3")
                            }.nextChildIs { childAssert2 ->
                                childAssert2
                                    .isBlock(BBX.BLOCK.DEFAULT)
                                    .hasStyle("Caption")
                                    .hasText("prod3.1")
                            }.noNextChild()
                    }.nextChildIs { childAssert1 ->
                        childAssert1
                            .nextChildIs { childAssert2 ->
                                childAssert2
                                    .isBlock(BBX.BLOCK.DEFAULT)
                                    .hasStyle("Caption")
                                    .hasText("prod4")
                            }.noNextChild()
                    }.noNextChild()
            }.noNextChild()
    }

    @Test
    fun imageGroupProdnoteWithParagraph_insidePoem() {
        convertAndAssert(
            "<poem><linegroup><line>"
                + "This is test"
                + "<imggroup><img src='evil.jpg'/><prodnote><p>I'm a <strong>bold</strong> desc</p></prodnote></imggroup>"
                + "</line></linegroup></poem>"
        )
            .nextChildIs { poem ->
                poem
                    .isContainerListType(BBX.ListType.POEM)
                    .nextChildIs { lineGroup ->
                        lineGroup
                            .isContainerListType(BBX.ListType.POEM_LINE_GROUP)
                            .nextChildIs { item ->
                                item
                                    .isBlock(BBX.BLOCK.LIST_ITEM)
                                    .nextChildIsText("This is test")
                            }.noNextChild()
                    }.noNextChild()
            }.nextChildIs { imageContainer ->
                imageContainer
                    .isContainer(BBX.CONTAINER.IMAGE)
                    .nextChildIs { prodnote ->
                        prodnote
                            .isContainer(BBX.CONTAINER.PRODNOTE)
                            .nextChildIs { block ->
                                block
                                    .isBlockDefaultStyle("Caption")
                                    .nextChildIsText("I'm a ")
                                    .nextChildIs { childAssert3 ->
                                        childAssert3
                                            .isInlineEmphasis(EmphasisType.BOLD)
                                            .hasText("bold")
                                    }.nextChildIsText(" desc")
                                    .noNextChild()
                            }.noNextChild()
                    }.noNextChild()
            }.noNextChild()
    }

    @Test
    fun imageGroupProdnote_insidePoem() {
        convertAndAssert(
            "<poem><linegroup><line>"
                + "This is test"
                + "<imggroup><img src='evil.jpg'/><prodnote>I'm a <strong>bold</strong> desc</prodnote></imggroup>"
                + "</line></linegroup></poem>"
        )
            .nextChildIs { poem ->
                poem
                    .isContainerListType(BBX.ListType.POEM)
                    .nextChildIs { lineGroup ->
                        lineGroup
                            .isContainerListType(BBX.ListType.POEM_LINE_GROUP)
                            .nextChildIs { item ->
                                item
                                    .isBlock(BBX.BLOCK.LIST_ITEM)
                                    .nextChildIsText("This is test")
                            }.noNextChild()
                    }.noNextChild()
            }.nextChildIs { imageContainer ->
                imageContainer
                    .isContainer(BBX.CONTAINER.IMAGE)
                    .nextChildIs { prodnote ->
                        prodnote
                            .isContainer(BBX.CONTAINER.PRODNOTE)
                            .nextChildIs { block ->
                                block
                                    .isBlockDefaultStyle("Caption")
                                    .nextChildIsText("I'm a ")
                                    .nextChildIs { childAssert3 ->
                                        childAssert3
                                            .isInlineEmphasis(EmphasisType.BOLD)
                                            .hasText("bold")
                                    }.nextChildIsText(" desc")
                                    .noNextChild()
                            }.noNextChild()
                    }.noNextChild()
            }.noNextChild()
    }

    @Test
    fun imageGroupParagraph_insidePoem() {
        convertAndAssert(
            "<poem><linegroup><line>"
                + "This is test"
                + "<imggroup><img src='evil.jpg'/><p>I'm a <strong>bold</strong> desc</p></imggroup>"
                + "</line></linegroup></poem>"
        )
            .nextChildIs { poem ->
                poem
                    .isContainerListType(BBX.ListType.POEM)
                    .nextChildIs { lineGroup ->
                        lineGroup
                            .isContainerListType(BBX.ListType.POEM_LINE_GROUP)
                            .nextChildIs { item ->
                                item
                                    .isBlock(BBX.BLOCK.LIST_ITEM)
                                    .nextChildIsText("This is test")
                            }.noNextChild()
                    }.noNextChild()
            }.nextChildIs { imageContainer ->
                imageContainer
                    .isContainer(BBX.CONTAINER.IMAGE)
                    .nextChildIs { block ->
                        block
                            .isBlockDefaultStyle("Caption")
                            .nextChildIsText("I'm a ")
                            .nextChildIs { childAssert3 ->
                                childAssert3
                                    .isInlineEmphasis(EmphasisType.BOLD)
                                    .hasText("bold")
                            }.nextChildIsText(" desc")
                            .noNextChild()
                    }.noNextChild()
            }.noNextChild()
    }

    @Test
    fun imageGroupParagraph_insideList_2ps() {
        _imageGroupParagrah_insideList(
            "<list>"
                + "<li>before</li>"
                + "<li>test"
                + "<p><imggroup><img src='evil.jpg'/><p>desc1</p><p>desc2</p></imggroup></p>"
                + "</li>"
                + "<li>after</li>"
                + "</list>"
        )
    }

    @Test
    fun imageGroupParagraph_insideList_1ps() {
        _imageGroupParagrah_insideList(
            "<list>"
                + "<li>before</li>"
                + "<li>test"
                + "<imggroup><img src='evil.jpg'/><p>desc1</p><p>desc2</p></imggroup>"
                + "</li>"
                + "<li>after</li>"
                + "</list>"
        )
    }

    fun _imageGroupParagrah_insideList(xml: String) {
        convertAndAssert(xml)
            .nextChildIs { list ->
                list
                    .isContainerListType(BBX.ListType.NORMAL)
                    .nextChildIs { item ->
                        item
                            .hasText("before")
                            .isBlockWithStyle("L1-3")
                    }.nextChildIs { item ->
                        item
                            .hasText("test")
                            .isBlockWithStyle("L1-3")
                    }.noNextChild()
            }.nextChildIs { img ->
                img
                    .isContainer(BBX.CONTAINER.IMAGE)
                    .nextChildIs { p ->
                        p
                            .hasText("desc1")
                            .isBlockDefaultStyle("Caption")
                    }.nextChildIs { p ->
                        p
                            .hasText("desc2")
                            .isBlockDefaultStyle("Caption")
                    }
            }.nextChildIs { list ->
                list
                    .isContainerListType(BBX.ListType.NORMAL)
                    .nextChildIs { item ->
                        item
                            .hasText("after")
                            .isBlockWithStyle("L1-3")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test(enabled = false)
    fun imageBlock() {
        convertAndAssert(
            "<p><img src='evil1.jpg'/></p>"
                + "<p><imggroup><img src='evil.jpg'/><p>I'm text</p></imggroup></p>"
        )
            .nextChildIs { childAssert ->
                childAssert
                    .isContainer(BBX.CONTAINER.IMAGE)
                    .hasAttributeBB(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "evil1.jpg")
                    .childCount(0)
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainer(BBX.CONTAINER.IMAGE)
                    .hasAttributeBB(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "evil.jpg")
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.DEFAULT)
                            .hasText("I'm text")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test
    fun imggroup_emphasisNoParagraph_inList() {
        convertAndAssert(
            "<list><li>libefore"
                + "<imggroup>"
                + "<img src='sigh.jpg'/>"
                + "<caption>"
                + "<strong>nestedStrong</strong>"
                + "<p>paragraph</p>"
                + "</caption>"
                + "</imggroup>"
                + "liafter</li></list>"
        )
            .nextChildIs { list ->
                list
                    .isContainerListType(BBX.ListType.NORMAL)
                    .nextChildIs { li ->
                        li
                            .hasText("libefore")
                            .isBlockWithStyle("L1-3")
                    }.noNextChild()
            }.nextChildIs { imggroup ->
                imggroup
                    .isContainer(BBX.CONTAINER.IMAGE)
                    .nextChildIs { caption ->
                        caption
                            .isContainer(BBX.CONTAINER.CAPTION)
                            .nextChildIs { p ->
                                p
                                    .isBlockDefaultStyle("Caption")
                                    .nextChildIs { strong ->
                                        strong
                                            .isInlineEmphasis(EmphasisType.BOLD)
                                            .hasText("nestedStrong")
                                    }.noNextChild()
                            }.nextChildIs { p ->
                                p
                                    .isBlockDefaultStyle("Caption")
                                    .hasText("paragraph")
                            }.noNextChild()
                    }.noNextChild()
            }.nextChildIs { list ->
                list
                    .isContainerListType(BBX.ListType.NORMAL)
                    .nextChildIs { li ->
                        li
                            .hasText("liafter")
                            .isBlockWithStyle("L1-3")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test
    fun imggroup_onlyInList() {
        convertAndAssertFirst(
            "<list><li>"
                + "<imggroup>"
                + "<img src='sigh.jpg'/>"
                + "<caption>"
                + "<strong>nestedStrong</strong>"
                + "<p>paragraph</p>"
                + "</caption>"
                + "</imggroup>"
                + "</li></list>"
        )
            .isContainer(BBX.CONTAINER.IMAGE)
            .nextChildIs { caption ->
                caption
                    .isContainer(BBX.CONTAINER.CAPTION)
                    .nextChildIs { p ->
                        p
                            .isBlockDefaultStyle("Caption")
                            .nextChildIs { strong ->
                                strong
                                    .isInlineEmphasis(EmphasisType.BOLD)
                                    .hasText("nestedStrong")
                            }.noNextChild()
                    }.nextChildIs { p ->
                        p
                            .isBlockDefaultStyle("Caption")
                            .hasText("paragraph")
                    }.noNextChild()
            }.noNextChild()
    }

    //----------------------- Tables -------------------------
    @Test(enabled = false)
    fun tableGroupTest() {
        convertAndAssertFirst(
            "<table>"
                + "<thead>"
                + "<tr><td>Heading 1</td><td>Heading 2</td></tr>"
                + "<tr><th>SubHeading 1</th><th>SubHeading 2</th></tr>"
                + "</thead>"
                + "<tbody>"
                + "<tr><td>Value 1</td><td>Value 2</td></tr>"
                + "</tbody>"
                + "<tfoot>"
                + "<tr><td>Footer 1</td><td>Footer 2</td></tr>"
                + "</tfoot>"
                + "</table>"
        )
            .isContainer(BBX.CONTAINER.TABLE)
            .nextChildIs { childAssert ->
                childAssert
                    .isContainerTableRowType(BBX.TableRowType.HEAD)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("Heading 1")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("Heading 2")
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainerTableRowType(BBX.TableRowType.HEAD)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("SubHeading 1")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("SubHeading 2")
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainerTableRowType(BBX.TableRowType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("Value 1")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("Value 2")
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainerTableRowType(BBX.TableRowType.FOOT)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("Footer 1")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("Footer 2")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test(enabled = false)
    fun tableTest() {
        convertAndAssertFirst(
            "<table>"
                //must have 2 columns to not be considered a non-table
                + "<tr><td><p>paragraph 1</p><p>paragraph 2</p></td><td>first</td></tr>"
                + "<tr><td><p>paragraph 3</p><p>paragraph 4</p></td><td>second</td></tr>"
                + "</table>"
        )
            .isContainer(BBX.CONTAINER.TABLE)
            .nextChildIs { childAssert ->
                childAssert
                    .isContainerTableRowType(BBX.TableRowType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("paragraph 1 paragraph 2")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("first")
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainerTableRowType(BBX.TableRowType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("paragraph 3 paragraph 4")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("second")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test(enabled = false)
    fun tableTextWrapTest() {
        convertAndAssertFirst(
            "<table>"
                //must have 2 columns to not be considered a non-table
                + "<tr><td>first</td><td>second</td></tr>"
                + "<tr><td>third</td><td>fourth</td></tr>"
                + "</table>"
        )
            .isContainer(BBX.CONTAINER.TABLE)
            .nextChildIs { childAssert ->
                childAssert
                    .isContainerTableRowType(BBX.TableRowType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("first")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("second")
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainerTableRowType(BBX.TableRowType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("third")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("fourth")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test(enabled = false)
    fun tableInsideList() {
        convertAndAssert(
            "<list><li>first</li><li>some text<table>"
                + "<tr><td>Line 1<br/>Line 2</td></tr>"
                //must have 2 columns to not be considered a non-table
                + "<tr><td>Line 3</td><td>second</td></tr>"
                + "</table> more text</li><li>number 2</li></list>"
        )
            .nextChildIs { childAssert ->
                childAssert
                    .isContainerListType(BBX.ListType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasText("first")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasText("some text")
                    }.noNextChild()
            }.nextChildIs { childAssert2 ->
                childAssert2
                    .isContainer(BBX.CONTAINER.TABLE)
                    .nextChildIs { childAssert3 ->
                        childAssert3
                            .isContainerTableRowType(BBX.TableRowType.NORMAL)
                            .nextChildIs { childAssert4 ->
                                childAssert4
                                    .isBlock(BBX.BLOCK.TABLE_CELL)
                                    .hasText("Line 1 Line 2")
                            }.noNextChild()
                    }.nextChildIs { childAssert3 ->
                        childAssert3
                            .isContainerTableRowType(BBX.TableRowType.NORMAL)
                            .nextChildIs { childAssert4 ->
                                childAssert4
                                    .isBlock(BBX.BLOCK.TABLE_CELL)
                                    .hasText("Line 3")
                            }.nextChildIs { childAssert4 ->
                                childAssert4
                                    .isBlock(BBX.BLOCK.TABLE_CELL)
                                    .hasText("second")
                            }.noNextChild()
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainerListType(BBX.ListType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasText("more text")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasText("number 2")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test(enabled = false)
    fun tableWithSidebarAndList() {
        convertAndAssertFirst(
            "<table><tr>"
                + "<td><em>test </em>paragraph 1<div><p>tesrt</p><list><li><strong>te</strong>st</li></list></div></td>"
                + "</tr><tr>"
                + "<td><p>paragraph 3</p><p>paragraph 4</p></td><td>second</td>"
                + "</tr></table>"
        )
            .isContainer(BBX.CONTAINER.TABLE)
            .nextChildIs { childAssert ->
                childAssert
                    .isContainerTableRowType(BBX.TableRowType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .nextChildIs { childAssert3 ->
                                childAssert3
                                    .isInlineEmphasis(EmphasisType.ITALICS)
                                    .hasText("test ")
                            }.nextChildIsText("paragraph 1 tesrt ")
                            .nextChildIs { childAssert3 ->
                                childAssert3
                                    .isInlineEmphasis(EmphasisType.BOLD)
                                    .hasText("te")
                            }
                            .nextChildIsText("st")
                            .noNextChild()
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainerTableRowType(BBX.TableRowType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("paragraph 3 paragraph 4")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("second")
                    }.noNextChild()
            }
    }

    @Test(enabled = false)
    fun tableWithSidebarAndListAndText() {
        convertAndAssertFirst(
            "<table><tr>"
                + "<td><em>test </em>paragraph 1<div><p>tesrt</p><list><li><strong>te</strong>st</li></list></div>after</td>"
                + "</tr><tr>"
                + "<td><p>paragraph 3</p><p>paragraph 4</p></td><td>second</td>"
                + "</tr></table>"
        )
            .isContainer(BBX.CONTAINER.TABLE)
            .nextChildIs { childAssert ->
                childAssert
                    .isContainerTableRowType(BBX.TableRowType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .nextChildIs { childAssert3 ->
                                childAssert3
                                    .isInlineEmphasis(EmphasisType.ITALICS)
                                    .hasText("test ")
                            }.nextChildIsText("paragraph 1 tesrt ")
                            .nextChildIs { childAssert3 ->
                                childAssert3
                                    .isInlineEmphasis(EmphasisType.BOLD)
                                    .hasText("te")
                            }
                            .nextChildIsText("st after")
                            .noNextChild()
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainerTableRowType(BBX.TableRowType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("paragraph 3 paragraph 4")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("second")
                    }.noNextChild()
            }
    }

    @Test(enabled = false)
    fun tableWithImage() {
        convertAndAssertFirst(
            "<table>"
                + "<tr><td><p><img src='test.jpg'/>paragraph 1</p><p>paragraph 2</p></td></tr>"
                + "<tr><td><p>paragraph 3</p></td><td>second</td></tr>"
                + "</table>"
        )
            .isContainer(BBX.CONTAINER.TABLE)
            .nextChildIs { childAssert ->
                childAssert
                    .isContainerTableRowType(BBX.TableRowType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .nextChildIsText("paragraph 1 paragraph 2")
                            .noNextChild()
                    }.noNextChild()
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainerTableRowType(BBX.TableRowType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("paragraph 3")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("second")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test(enabled = false)
    fun tableWithImage_OnlyImageInCell_issue5817() {
        convertAndAssertFirst(
            "<table>"
                + "<tr><td>paragraph 1.1</td><td>paragraph 1.2</td></tr>"
                + "<tr><td><img src='test.jpg'/></td><td>paragraph 2.2</td></tr>"
                + "</table>"
        )
            .isContainer(BBX.CONTAINER.TABLE)
            .nextChildIs { childAssert ->
                childAssert
                    .isContainerTableRowType(BBX.TableRowType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("paragraph 1.1")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .hasText("paragraph 1.2")
                    }.noNextChild()
            }.nextChildIs { rowAssert ->
                rowAssert
                    .isContainerTableRowType(BBX.TableRowType.NORMAL)
                    .nextChildIs { cellAssert ->
                        cellAssert
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .noNextChild()
                    }.nextChildIs { cellAssert ->
                        cellAssert
                            .isBlock(BBX.BLOCK.TABLE_CELL)
                            .nextChildIsText("paragraph 2.2")
                            .noNextChild()
                    }.noNextChild()
            }.noNextChild()
    }

    @Test(enabled = false)
    fun tableNonWithListAndText() {
        convertAndAssert(
            "<table><tr>"
                + "<td>test <list><li>words</li></list></td>"
                + "</tr></table>"
        )
            .nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Body Text")
                    .hasText("test")
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainerListType(BBX.ListType.NORMAL)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasStyle("L1-3")
                            .hasText("words")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test(enabled = false)
    fun tableNonWithEmptyTableCell() {
        convertAndAssert(
            "<table><tbody><tr>"
                + "<td>before</td>"
                + "<td/>"
                + "<td>after</td>"
                + "</tr></tbody></table>"
        )
            .nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Body Text")
                    .hasText("before")
            }.nextChildIs { childAssert ->
                childAssert
                    .isBlockDefaultStyle("Body Text")
                    .hasText("after")
            }.noNextChild()
    }

    // tableNonWithEmptyRow_issue6163 opens a BBTestRunner, see BookToBBXConverterGuiTest

    //---------------------- Poem ----------------------
    @Test
    fun poemStandaloneLineGroupTest() {
        convertAndAssertFirst(
            "<linegroup>"
                + "<line>test1</line>"
                + "<line>te<linenum>4</linenum>st2</line>"
                + "</linegroup>"
        )
            .isContainerListType(BBX.ListType.POEM_LINE_GROUP)
            .inlineTest(::poemIsChildrenValid)
            .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 0)
            .noNextChild()
    }

    @Test
    fun poemTest() {
        convertAndAssertFirst(
            "<poem>"
                + "<linegroup>"
                + "<line>test1</line>"
                + "<line>te<linenum>4</linenum>st2</line>"
                + "</linegroup>"
                + "<linegroup>"
                + "<line>group2</line>"
                + "</linegroup>"
                + "</poem>"
        )
            .isContainerListType(BBX.ListType.POEM)
            .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 0)
            .nextChildIs { childAssert ->
                childAssert
                    .isContainerListType(BBX.ListType.POEM_LINE_GROUP)
                    .inlineTest(::poemIsChildrenValid)
            }.nextChildIs { childAssert ->
                childAssert
                    .isContainerListType(BBX.ListType.POEM_LINE_GROUP)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                            .hasStyle("P1-3")
                            .hasText("group2")
                    }.noNextChild()
            }.noNextChild()
    }

    private fun poemIsChildrenValid(asserter: XMLElementAssert) {
        asserter.nextChildIs { childAssert ->
            childAssert
                .isBlock(BBX.BLOCK.LIST_ITEM)
                .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                .hasStyle("P1-3")
                .hasText("test1")
        }.nextChildIs { childAssert ->
            childAssert
                .isBlock(BBX.BLOCK.LIST_ITEM)
                .hasAttributeBB(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL, 0)
                .hasStyle("P1-3")
                .nextChildIsText("te")
                .nextChildIs { childAssert2 ->
                    childAssert2
                        .isSpan(BBX.SPAN.POEM_LINE_NUMBER)
                        .hasText("4")
                }.nextChildIsText("st2")
                .noNextChild()
        }.noNextChild()
    }

    @Test
    fun poemList() {
        convertAndAssertFirst(
            "<poem>"
                + "<line>test1</line>"
                + "<line>te<linenum>4</linenum>st2</line>"
                + "</poem>"
        )
            .isContainerListType(BBX.ListType.POEM)
            .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 0)
            .inlineTest(::poemIsChildrenValid)
    }

    @Test
    fun poemPageNumInsideLineGroup() {
        convertAndAssertFirst(
            "<poem>"
                + "<linegroup>"
                + "<line>before</line>"
                + "<pagenum>5</pagenum>"
                + "<line>after</line>"
                + "</linegroup>"
                + "</poem>"
        )
            .isContainerListType(BBX.ListType.POEM)
            .hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 0)
            .nextChildIs { childAssert ->
                childAssert
                    .isContainerListType(BBX.ListType.POEM_LINE_GROUP)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasStyle("P1-3")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.PAGE_NUM)
                            .hasText("5")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasStyle("P1-3")
                    }.noNextChild()
            }.noNextChild()
    }

    @Test
    fun poemNestedLineGroup_issue5507() {
        convertAndAssertFirst(
            "<poem>"
                + "<linegroup>"
                + "<line>before</line>"
                + "<linegroup>"
                + "<line>inside</line>"
                + "</linegroup>"
                + "<line>after</line>"
                + "</linegroup>"
                + "</poem>"
        )
            .isContainerListType(BBX.ListType.POEM)
//				.hasAttributeBB(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL, 0)
            .nextChildIs { childAssert ->
                childAssert
                    .isContainerListType(BBX.ListType.POEM_LINE_GROUP)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasStyle("P1-5")
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isContainerListType(BBX.ListType.POEM_LINE_GROUP)
                            .nextChildIs { childAssert3 ->
                                childAssert3
                                    .isBlock(BBX.BLOCK.LIST_ITEM)
                                    .hasStyle("P3-5")
                            }.noNextChild()
                    }.nextChildIs { childAssert2 ->
                        childAssert2
                            .isBlock(BBX.BLOCK.LIST_ITEM)
                            .hasStyle("P1-5")
                    }.noNextChild()
            }.noNextChild()
    }

    //-------------------------- mathml ----------------------------------
    @Test
    fun mathmlTest() {
        convertAndAssertFirst(
            "<p>test<m:math>"
                + "<m:mrow>"
                + "<m:mn>3</m:mn>"
                + "</m:mrow>"
                + "</m:math></p>"
        )
            .isBlockDefaultStyle("Body Text")
            .nextChildIsText("test")
            .nextChildIs { childAssert ->
                childAssert
                    .isInline(BBX.INLINE.MATHML)
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isMathML("math")
                            .nextChildIs { childAssert3 ->
                                childAssert3
                                    .isMathML("mrow")
                                    .nextChildIs { childAssert4 ->
                                        childAssert4
                                            .isMathML("mn")
                                            .hasText("3")
                                    }.noNextChild()
                            }.noNextChild()
                    }
            }
            .noNextChild()
    }

    @Test
    fun mathmlEmphasisTest() {
        convertAndAssertFirst(
            "<p><strong>test<m:math>"
                + "<m:mn>3</m:mn>"
                + "</m:math></strong></p>"
        )
            .isBlockDefaultStyle("Body Text")
            .nextChildIs { childAssert ->
                childAssert
                    .isInlineEmphasis(EmphasisType.BOLD)
                    .hasText("test")
            }.nextChildIs { childAssert ->
                childAssert
                    .isInline(BBX.INLINE.MATHML)
                    .hasAttributeBB(BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS, EnumSet.of(EmphasisType.BOLD))
                    .nextChildIs { childAssert2 ->
                        childAssert2
                            .isMathML("math")
                            .nextChildIs { childAssert4 ->
                                childAssert4
                                    .isMathML("mn")
                                    .hasText("3")
                            }.noNextChild()
                    }.noNextChild()
            }.noNextChild()
    }

    @Test(enabled = false)
    fun tabTest_issue6629() {
        convertAndAssertFirst("<p>test\ttest</p>")
            .isBlockDefaultStyle("Body Text")
            .hasText("test test")
    }

    //---------------------- Utils ---------------------------
    fun convertAndAssert(body: String): XMLElementAssert {
        return convertAndAssert(converter, maps.styleMultiMap(), body)
    }

    fun convertAndAssert(doc: Document): XMLElementAssert {
        return convertAndAssert(converter, maps.styleMultiMap(), doc)
    }

    private fun convertAndAssertFirst(body: String): XMLElementAssert {
        return convertAndAssert(body)
            .childCount(1)
            .nextChild()
    }

    companion object {
        @JvmStatic
        fun convertAndAssert(converter: BookToBBXConverter, map: IStyleMap, body: String): XMLElementAssert {
            val doc = TestXMLUtils.generateBookDoc("", body)
            for (node in FastXPath.descendant(doc)) {
                if (node is Text && node.value.isEmpty()) {
                    throw NodeException("blank text node", node)
                }
            }
            return convertAndAssert(converter, map, doc)
        }

        @JvmStatic
        fun convertAndAssert(converter: BookToBBXConverter, map: IStyleMap, doc: Document): XMLElementAssert {
            val origRoot = doc.rootElement.copy()

            val bbxDoc = converter.convert(doc)
            BookToBBXConverter.upgradeFormat(bbxDoc)

            if (FastXPath.descendant(bbxDoc).filterIsInstance<Element>().any { BBX._ATTRIB_FIXER_TODO.has(it) }) {
                throw NodeException("Document still contains fixerTodo attributes", bbxDoc)
            }

            bbxDoc.rootElement.appendChild(origRoot)
            origRoot.addAttribute(Attribute("sourceDoc", "true"))

            return XMLElementAssert(bbxDoc.rootElement, map)
                .child(1)
                .isSection(BBX.SECTION.ROOT)
                .hasAttribute("bbtestroot", "true")
                .validate()
        }
    }
}
