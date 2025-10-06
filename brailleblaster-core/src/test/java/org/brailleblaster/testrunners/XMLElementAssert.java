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
package org.brailleblaster.testrunners;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThan;
import static org.testng.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.xml.bind.JAXBElement;

import org.brailleblaster.bbx.BBX;
import org.brailleblaster.settings.UTDManager;
import org.brailleblaster.utd.IStyle;
import org.brailleblaster.utd.IStyleMap;
import org.brailleblaster.utd.Style;
import org.brailleblaster.utd.config.UTDConfig;
import org.brailleblaster.utd.exceptions.NodeException;
import org.brailleblaster.utd.internal.xml.FastXPath;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.brailleblaster.utd.properties.EmphasisType;
import org.brailleblaster.utd.utils.UTDHelper;
import org.brailleblaster.utils.NamespacesKt;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParentNode;
import nu.xom.Text;

/**
 * Utility for asserting an exact XML structure
 */
public class XMLElementAssert {
    private static final Logger log = LoggerFactory.getLogger(XMLElementAssert.class);
    /**
     * Warning: {@link #stripUTDAndCopy() } may make this a copied element!
     */
    private final Element elem;
    private final int childrenCount;
    private final IStyleMap styleMap;
    private int nextChild = -1;

    public XMLElementAssert(Element elem, IStyleMap styleMap) {
        this.elem = elem;
        this.childrenCount = elem.getChildCount();
        this.styleMap = styleMap;

        if (elem.getDocument() == null) {
            throw new IllegalArgumentException("Node needs to be attached to document");
        }
    }

    public XMLElementAssert textChildCount(int wanted) {
        Nodes n = elem.query("descendant::text()");
        Assert.assertEquals(wanted, n.size());
        return this;
    }

    public XMLElementAssert validate() {
        BBX.CoreType type = BBX.getType(elem);
        BBX.NodeValidator subType = type.getSubType(elem);

        Assert.assertNull(subType.validate(elem));
        return this;
    }

    public XMLElementAssert hasName(String name) {
        assertEquals(elem.getLocalName(), name, "Name is different");
        return this;
    }

    public XMLElementAssert isSection(BBX.SectionSubType type) {
        assertEquals(BBX.getType(elem), BBX.SECTION, "invalid element type");
        assertEquals(BBX.SECTION.getSubType(elem), type, "invalid section type");
        return this;
    }

    public XMLElementAssert isContainer(BBX.ContainerSubType type) {
        return isContainer(type, false);
    }

    private XMLElementAssert isContainer(BBX.ContainerSubType type, boolean fromTypeCheck) {
        if (!fromTypeCheck && type == BBX.CONTAINER.LIST) {
            throw new IllegalStateException("Call specific validate method");
        }
        assertEquals(BBX.getType(elem), BBX.CONTAINER, "invalid element type");
        assertEquals(BBX.CONTAINER.getSubType(elem), type, "invalid section type");
        return this;
    }

    public XMLElementAssert isContainerListType(BBX.ListType type) {
        isContainer(BBX.CONTAINER.LIST, true);
        assertEquals(BBX.CONTAINER.LIST.ATTRIB_LIST_TYPE.get(elem), type, "invalid list type");
        Assert.assertNull(type.validate(elem), "not a valid list type");
        return this;
    }

    public XMLElementAssert isContainerTableRowType(BBX.TableRowType type) {
        isContainer(BBX.CONTAINER.TABLE_ROW, true);
        assertEquals(BBX.CONTAINER.TABLE_ROW.ATTRIB_ROW_TYPE.get(elem), type, "invalid table row type");
        return this;
    }

    public XMLElementAssert isMathML(String localName) {
        assertEquals(elem.getNamespaceURI(), "http://www.w3.org/1998/Math/MathML", "Not in MathML namespace");
        assertEquals(elem.getLocalName(), localName, "Different element name");
        return this;
    }

    public XMLElementAssert isBlock(BBX.BlockSubType blockType) {
        assertEquals(BBX.getType(elem), BBX.BLOCK, "invalid element type");
        assertEquals(BBX.BLOCK.getSubType(elem), blockType, "invalid block sub type");
        return this;
    }

    public XMLElementAssert isBlockDefaultStyle(String style) {
        isBlock(BBX.BLOCK.DEFAULT);
        hasStyle(style);
        return this;
    }

    /*
     * Does not care about the sub type of the block
     */
    public XMLElementAssert isBlockWithStyle(String style) {
        assertEquals(BBX.getType(elem), BBX.BLOCK, "invalid element type");
        hasStyle(style);
        return this;
    }

    public XMLElementAssert isBlockWithOverrideStyle(String style) {
        assertEquals(BBX.getType(elem), BBX.BLOCK, "invalid element type");
        hasOverrideStyle(style);
        return this;
    }

    public XMLElementAssert hasStyle(String styleName) {
        try {
            IStyle style = styleMap.findValueWithDefault(elem, null);
            Assert.assertNotNull(style, "Style not found");
            assertEquals(style.getName(), styleName, "Style name different");
        } catch (Throwable e) {
            if (e instanceof NodeException) {
                throw e;
            }
            throw new NodeException("failed", elem, e);
        }

        return this;
    }

    public XMLElementAssert hasOverrideStyle(String styleName) {
        hasStyle(styleName);
        assertEquals(
                BBX._ATTRIB_OVERRIDE_STYLE.get(elem),
                styleName,
                "Style different"
        );
        return this;
    }

    public XMLElementAssert hasBaseStyleWithOptions(
            String baseStyleName,
            Style.StyleOption styleOption1,
            Object value1) {
        return hasBaseStyleWithOptions(baseStyleName, styleOption1, value1, null, null, null, null, null, null, null, null);
    }

    public XMLElementAssert hasBaseStyleWithOptions(
            String baseStyleName,
            Style.StyleOption styleOption1,
            Object value1,
            Style.StyleOption styleOption2,
            Object value2) {
        return hasBaseStyleWithOptions(baseStyleName, styleOption1, value1, styleOption2, value2, null, null, null, null, null, null);
    }

    public XMLElementAssert hasBaseStyleWithOptions(
            String baseStyleName,
            Style.StyleOption styleOption1,
            Object value1,
            Style.StyleOption styleOption2,
            Object value2,
            Style.StyleOption styleOption3,
            Object value3) {
        return hasBaseStyleWithOptions(baseStyleName, styleOption1, value1, styleOption2, value2, styleOption3, value3, null, null, null, null);
    }

    public XMLElementAssert hasBaseStyleWithOptions(
            String baseStyleName,
            Style.StyleOption styleOption1,
            Object value1,
            Style.StyleOption styleOption2,
            Object value2,
            Style.StyleOption styleOption3,
            Object value3,
            Style.StyleOption styleOption4,
            Object value4) {
        return hasBaseStyleWithOptions(baseStyleName, styleOption1, value1, styleOption2, value2, styleOption3, value3, styleOption4, value4, null, null);
    }

    //Generics make easily specifying Map<StyleOption, Object> hard...
    public XMLElementAssert hasBaseStyleWithOptions(
            String baseStyleName,
            Style.StyleOption styleOption1,
            Object value1,
            Style.StyleOption styleOption2,
            Object value2,
            Style.StyleOption styleOption3,
            Object value3,
            Style.StyleOption styleOption4,
            Object value4,
            Style.StyleOption styleOption5,
            Object value5) {
        Style style = (Style) styleMap.findValueOrDefault(elem);

        {
            Style baseStyle = UTDManager.getBaseStyle(style);
            assertEquals(baseStyle.getName(), baseStyleName, "Base styleName different");
        }

        int counter = 0;
        for (Map.Entry<Style.StyleOption, Field> entry : UTDConfig.STYLE_OPTION_FIELDS.entrySet()) {
            Style.StyleOption fieldStyleOption = entry.getKey();
            Field curField = entry.getValue();

            Object expectedStyleOptionValue;
            if (fieldStyleOption == styleOption1) {
                expectedStyleOptionValue = value1;
            } else if (fieldStyleOption == styleOption2) {
                expectedStyleOptionValue = value2;
            } else if (fieldStyleOption == styleOption3) {
                expectedStyleOptionValue = value3;
            } else if (fieldStyleOption == styleOption4) {
                expectedStyleOptionValue = value4;
            } else if (fieldStyleOption == styleOption5) {
                expectedStyleOptionValue = value5;
            } else {
                continue;
            }

            try {
                curField.setAccessible(true);
                JAXBElement<?> jaxbField = ((JAXBElement<?>) curField.get(style));
                try {
                    assertNotNull(jaxbField, "Missing field " + curField.getName() + " style " + style);
                    assertEquals(
                            jaxbField.getValue(),
                            expectedStyleOptionValue,
                            "Style option " + fieldStyleOption + " different " + jaxbField
                    );
                } catch (AssertionError e) {
                    throw new NodeException("failed to get field", elem, e);
                }
                counter++;
            } catch (Exception e) {
                throw new RuntimeException("failed to get field", e);
            }
        }

        int expectedCounter = 0;
        if (styleOption1 != null) {
            expectedCounter++;
        }
        if (styleOption2 != null) {
            expectedCounter++;
        }
        if (styleOption3 != null) {
            expectedCounter++;
        }
        if (styleOption4 != null) {
            expectedCounter++;
        }
        if (styleOption5 != null) {
            expectedCounter++;
        }
        assertEquals(counter, expectedCounter, "Didn't check all style options?");
        return this;
    }

    public XMLElementAssert isInline(BBX.InlineSubType type) {
        if (type == BBX.INLINE.EMPHASIS) {
            throw new IllegalStateException("Use specific validate method for emphasis");
        }
        assertEquals(BBX.getType(elem), BBX.INLINE, "invalid element type");
        type.assertIsA(elem);
        return this;
    }

    public XMLElementAssert isInlineEmphasis(EmphasisType... types) {
        assertEquals(BBX.getType(elem), BBX.INLINE, "invalid element type");
        BBX.INLINE.EMPHASIS.assertIsA(elem);
        assertThat(
                BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS.get(elem),
                containsInAnyOrder(types)
        );
        return this;
    }

    public XMLElementAssert isSpan(BBX.SpanSubType type) {
        assertEquals(BBX.getType(elem), BBX.SPAN, "invalid element type");
        type.assertIsA(elem);
        return this;
    }

    public XMLElementAssert hasAttributeUTD(String attribName, String expectedValue) {
        assertEquals(
                elem.getAttributeValue(attribName, NamespacesKt.UTD_NS),
                expectedValue,
                "Attrib " + attribName + " value doesn't match expected"
        );
        return this;
    }

    public XMLElementAssert hasAttributeUTD(String attribName) {
        assertEquals(
                elem.getAttributeValue(attribName, NamespacesKt.UTD_NS) != null,
                true,
                "Attrib " + attribName + " value doesn't match expected"
        );
        return this;
    }

    public <T> XMLElementAssert hasAttributeBB(BBX.BaseAttribute<T> attrib, T expectedValue) {
        assertEquals(
                attrib.get(elem),
                expectedValue,
                "Attrib " + attrib.name + " value doesn't match expected"
        );
        return this;
    }

    public <T> XMLElementAssert hasNoAttributeBB(BBX.BaseAttribute<T> attrib) {
        assertEquals(
                attrib.has(elem),
                false,
                "Attrib " + attrib.name + " exists when it should not"
        );
        return this;
    }

    public XMLElementAssert hasAttribute(String attribName, String expectedValue) {
        assertEquals(
                elem.getAttributeValue(attribName),
                expectedValue,
                "Attrib " + attribName + " value doesn't match expected"
        );
        return this;
    }

    public XMLElementAssert hasNoAttribute(String attribName) {
        assertEquals(
                elem.getAttribute(attribName),
                null,
                "Attrib " + attribName + " exists"
        );
        return this;
    }

    public XMLElementAssert hasBBNamespaceAttribute(String attribName, String expectedValue) {
        assertEquals(
                elem.getAttributeValue(attribName, NamespacesKt.BB_NS),
                expectedValue,
                "Attrib " + attribName + " value doesn't match expected"
        );
        return this;
    }

    public XMLElementAssert hasText(String text) {
        // TODO: throw exception when contains anything else besides text nodes?
        if (elem.getChildCount() == 0) {
            throw new NodeException("No children", elem);
        } else if (elem.getChildCount() == 1) {
            if (!(elem.getChild(0) instanceof Text)) {
                throw new NodeException("Expected text node, got", elem.getChild(0));
            }
            Text textNode = (Text) elem.getChild(0);
            assertEquals(textNode.getValue(), text, "text diff");
        } else {
            //combine adjacent text nodes
            Node curNode = elem.getChild(0);
            StringBuilder value = new StringBuilder(curNode.getValue());
            while ((curNode = XMLHandler.nextSiblingNode(curNode)) instanceof Text) {
                value.append(curNode.getValue());
            }
            assertEquals(value.toString(), text, "text diff");
        }
        return this;
    }

    private void incrimentChildIterator() {
        assertEquals(elem.getChildCount(), childrenCount, "children modified");

        nextChild++;
        log.debug("nextChild {} count {}", nextChild, elem.getChildCount());
        try {
            assertThat(nextChild, lessThan(childrenCount));
        } catch (Throwable e) {
            throw new NodeException("Index " + nextChild + " is larger than childCount " + childrenCount, elem, e);
        }
    }

    public XMLElementAssert nextChild() {
        incrimentChildIterator();
        return child(nextChild);
    }

    public XMLElementAssert nextChildIs(Consumer<XMLElementAssert> onChild) {
        onChild.accept(nextChild());
        return this;
    }

    public XMLElementAssert nextChildIsText(String text) {
        incrimentChildIterator();
        Node child = elem.getChild(nextChild);
        if (child == null) {
            throw new NodeException("No child at index " + nextChild, elem);
        } else if (!(child instanceof Text)) {
            throw new NodeException("Expected text, got ", child);
        }

        //combine adjacent text nodes
        StringBuilder value = new StringBuilder(child.getValue());
        while (nextChild + 1 < childrenCount && elem.getChild(nextChild + 1) instanceof Text) {
            incrimentChildIterator();
            value.append(elem.getChild(nextChild).getValue());
        }
        assertEquals(value.toString(), text, "Text value different");

        return this;
    }

    public XMLElementAssert onlyChildIsText(String text) {
        if (elem.getChildCount() != 1) {
            throw new NodeException("More than one child", elem);
        }

        Node child = elem.getChild(0);
        if (!(child instanceof Text)) {
            throw new NodeException("Expected text, got ", child);
        }

        assertEquals(child.getValue(), text, "Text value different");

        return this;
    }

    public XMLElementAssert noNextChild() {
        assertEquals(nextChild, childrenCount - 1, "extra element");

        return this;
    }

    public XMLElementAssert child(int index) {
        Node child;
        try {
            child = elem.getChild(index);
        } catch (Exception e) {
            throw new NodeException("Element doesn't have child at index " + index, elem, e);
        }

        try {
            assertThat(child, instanceOf(Element.class));
        } catch (Throwable e) {
            throw new NodeException("Unexpected type", child, e);
        }
        return new XMLElementAssert((Element) child, styleMap);
    }

    public XMLElementAssert childIs(int index, Consumer<XMLElementAssert> onChild) {
        onChild.accept(child(index));
        return this;
    }

    public XMLElementAssert childCount(int expected) {
        assertEquals(elem.getChildCount(), expected, "different child count");
        return this;
    }

    public XMLElementAssert isLastNodeOfParent() {
        ParentNode parent = elem.getParent();
        assertEquals(parent.indexOf(elem), parent.getChildCount() - 1, "Not last child of parent");
        return this;
    }

    public XMLElementAssert onlyChildIs() {
        assertEquals(elem.getChildCount(), 1, "Not 1 child");

        Node child = elem.getChild(0);
        if (!(child instanceof Element)) {
            throw new NodeException("Expected child of type Element, found: ", child);
        }

        return new XMLElementAssert((Element) child, styleMap);
    }

    public XMLElementAssert elementEquals(Element expectedNode) {
        assertEquals(elem, expectedNode, "Element does not match");
        return this;
    }

    public XMLElementAssert childEquals(int index, Node expectedNode) {
        assertEquals(elem.getChild(index), expectedNode, "child index " + index + " does not match");
        return this;
    }

    /**
     * Test without breaking method chain
     *
     * @param testAsserter
     * @return
     */
    public XMLElementAssert inlineTest(Consumer<XMLElementAssert> testAsserter) {
        testAsserter.accept(this);
        return this;
    }

    public XMLElementAssert stripUTDAndCopy() {
        String attribName = "stripUTDElem";
        if (elem != elem.getDocument().getRootElement()) {
            Attribute attribute = new Attribute(attribName, "true");
            elem.addAttribute(attribute);

            Document copyDoc = elem.getDocument().copy();
            Element copyElement = FastXPath.INSTANCE.descendantFindOnly(
                    copyDoc,
                    node -> node instanceof Element && ((Element) node).getAttribute(attribName) != null
            );
            UTDHelper.stripUTDRecursive(copyElement);

            copyElement.getAttribute(attribName).detach();
            attribute.detach();
            return new XMLElementAssert(copyElement, styleMap);
        } else {
            Document copyDoc = elem.getDocument().copy();
            UTDHelper.stripUTDRecursive(copyDoc);
            return new XMLElementAssert(copyDoc.getRootElement(), styleMap);
        }
    }

    public Element element() {
        return elem;
    }

    private void assertEquals(Object actual, Object expected, String message) {
        try {
            Assert.assertEquals(actual, expected, message);
        } catch (Throwable e) {
            throw new NodeException("Assert failed for element", elem, e);
        }
    }

    private <T> void assertThat(T actual, Matcher<? super T> matcher) {
        try {
            MatcherAssert.assertThat(actual, matcher);
        } catch (Exception e) {
            throw new NodeException("Assert failed for element", elem, e);
        }
    }
}
