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
package org.brailleblaster.bbx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.brailleblaster.utils.xml.NamespacesKt;
import org.jetbrains.annotations.Nullable;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang3.StringUtils;
import org.brailleblaster.bbx.parsers.ImportParserUtils;
import org.brailleblaster.math.mathml.MathModuleUtils;
import org.brailleblaster.math.numberLine.NumberLine;
import org.brailleblaster.math.numberLine.NumberLineConstants;
import org.brailleblaster.math.numberLine.NumberLineJson;
import org.brailleblaster.math.spatial.ConnectingContainer;
import org.brailleblaster.math.spatial.ConnectingContainerJson;
import org.brailleblaster.math.spatial.Grid;
import org.brailleblaster.math.spatial.GridJson;
import org.brailleblaster.math.spatial.GsonInterfaceAdapter;
import org.brailleblaster.math.spatial.ISpatialMathContainerJson;
import org.brailleblaster.math.spatial.Matrix;
import org.brailleblaster.math.spatial.MatrixConstants;
import org.brailleblaster.math.spatial.MatrixConstants.BracketType;
import org.brailleblaster.math.spatial.MatrixConstants.Wide;
import org.brailleblaster.math.spatial.MatrixJson;
import org.brailleblaster.math.spatial.SpatialMathEnum;
import org.brailleblaster.math.spatial.SpatialMathEnum.Passage;
import org.brailleblaster.math.spatial.SpatialMathEnum.Translation;
import org.brailleblaster.math.template.Template;
import org.brailleblaster.math.template.TemplateJson;
import org.brailleblaster.utd.OverrideMap;
import org.brailleblaster.utd.actions.TabAction;
import org.brailleblaster.utd.config.StyleDefinitions;
import org.brailleblaster.utd.exceptions.NodeException;
import org.brailleblaster.utd.internal.xml.FastXPath;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.brailleblaster.utd.properties.EmphasisType;
import org.brailleblaster.utd.properties.UTDElements;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.ParsingException;
import nu.xom.XPathContext;

/**
 * BBX (BrailleBlaster Xml) definitions, validation, and jaxb-serialization
 *
 * <h1>API Design</h1> Overview:
 *
 * <pre>
 * //The API is self-navigatable with autocomplete/intellisense.
 * //All types are stored in public static fields for easy access
 * BBX.[coretype].[subtype].create()
 * BBX.[coretype].[subtype].[attrib].get(Element)
 * BBX.[attrib].get(Element) //Common attribute that may be on any type
 * BBX.[coretype].[subtype].isA(elementToCheck)
 * BBX.[coretype].[subtype].assertIsA(notThisTypeElement) //throw an exception if element is not the subtype
 *
 * //Example:
 * BBX._ATTRIB_OVERRIDE_STYLE.get(someStyledBlockElement)
 * BBX.BLOCK.STYLE.create("Body Text")
 * BBX.CONTAINER.LIST.isA(someListElement)
 * BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS.get(someEmphasisElement)
 * </pre>
 *
 * <h1>Parts</h1>
 * <ul>
 * <li>Type/CoreType - Maps to one of the 5 core BBX elements, can also think of
 * it as categories of elements. Eg a span can be a page number, image, poem
 * line number, etc</li>
 * <li>SubType - A specific element use, like a styled block or a sidebar
 * container</li>
 * <li>*Attribute - Unlike XOM's attribute, these are generic classes to work
 * with any element's attribute, as well as marshalling/converting into a more
 * friendly format, eg number values are returned as ints instead of
 * strings</li>
 * <li>validate()/subValidate() - Validate basic element values during initially
 * building the BBX document, less restrictive as state might not be finished
 * yet</li>
 * <li>validateComplete() - Used during full validation of the entire completed
 * BBX document</li>
 * </ul>
 */
public class BBX {
    private static final Logger log = LoggerFactory.getLogger(BBX.class);
    /**
     * Element and attribute prefix, eg bb:type="OTHER"
     */
    public static final String BB_PREFIX = "bb";
    /**
     * XML Root
     */
    public static final String DOCUMENT_ROOT_NAME = "bbdoc";
    /**
     * The version of the BBX Format, increment and write converter if
     * drastically changing
     */
    public static final int FORMAT_VERSION = 6;
    /**
     * Marker used by parsers to trigger fixers, should not exist in final
     * document
     */
    public static final EnumAttribute<FixerTodo> _ATTRIB_FIXER_TODO = new EnumAttribute<>("fixerTodo", FixerTodo.class);
    /**
     * Internal data of the original imported element name
     */
    public static final StringAttribute _ATTRIB_ORIGINAL_ELEMENT = new StringAttribute("origElement");
    /**
     * UTD action
     */
    public static final StringAttribute _ATTRIB_OVERRIDE_ACTION = new StringAttribute(
            OverrideMap.OVERRIDE_ATTRIB_ACTION, UTDElements.UTD_PREFIX, NamespacesKt.UTD_NS);
    /**
     * UTD style
     */
    public static final StringAttribute _ATTRIB_OVERRIDE_STYLE = new StringAttribute(OverrideMap.OVERRIDE_ATTRIB_STYLE,
            UTDElements.UTD_PREFIX, NamespacesKt.UTD_NS);
    /**
     * BB subtype of whatever coretype the element is
     */
    public static final StringAttribute _ATTRIB_TYPE = new StringAttribute("type");
    /**
     * Usable as entire document should only have elements in these namespaces
     */
    public static final XPathContext XPATH_CONTEXT;

    static {
        XPATH_CONTEXT = new XPathContext();
        XPATH_CONTEXT.addNamespace("bb", NamespacesKt.BB_NS);
        XPATH_CONTEXT.addNamespace("utd", NamespacesKt.UTD_NS);
        XPATH_CONTEXT.addNamespace("m", NamespacesKt.MATHML_NS);
    }

    // Don't make instances
    private BBX() {
    }

    public static Element newElement(String name) {
        return new Element(name, NamespacesKt.BB_NS);
    }

    public static Document newDocument() {
        Element root = newElement(DOCUMENT_ROOT_NAME);
        root.addNamespaceDeclaration("bb", NamespacesKt.BB_NS);
        root.addNamespaceDeclaration("utd", NamespacesKt.UTD_NS);
        root.addNamespaceDeclaration("m", NamespacesKt.MATHML_NS);

        Element headElem = newElement("head");
        root.appendChild(headElem);

        Document doc = new Document(root);

        setFormatVersion(doc, FORMAT_VERSION);

        return doc;
    }

    public static Element getHead(Document doc) {
        Element head = doc.getRootElement().getFirstChildElement("head", NamespacesKt.BB_NS);
        if (head == null) {
            head = new Element("head", NamespacesKt.BB_NS);
            doc.getRootElement().insertChild(head, 0);
        }
        return head;
    }

    public static int getFormatVersion(Document doc) {
        Element head = getHead(doc);
        Element versionElem = head.getFirstChildElement("version", NamespacesKt.BB_NS);
        if (versionElem == null) {
            throw new NodeException("No bb version attribute found", head);
        }
        return Integer.parseInt(versionElem.getValue());
    }

    public static void setFormatVersion(Document doc, int version) {
        Element head = getHead(doc);
        Element versionElem = head.getFirstChildElement("version", NamespacesKt.BB_NS);
        if (versionElem == null) {
            versionElem = new Element("version", NamespacesKt.BB_NS);
            head.appendChild(versionElem);
        }
        versionElem.removeChildren();
        versionElem.appendChild(String.valueOf(version));
    }

    public static void assertIsA(Document doc) {
        // TODO: will throw an exception if not found
        getFormatVersion(doc);
        getRoot(doc);
    }

    public static void assertAttachedToDocument(Node node) {
        BBX.CONTAINER.VOLUME.assertIsA(node);
        if (node.getDocument() == null) {
            throw new NodeException("Node not attached to document", node);
        }
    }

    public static @NotNull Element getRoot(@NotNull Document doc) {
        if (doc.getRootElement().getChildElements().size() != 2) {
            throw new NodeException("Root should have both <head> and <section bb:type='ROOT'>", doc.getRootElement());
        }
        Element root = doc.getRootElement().getChildElements().get(1);
        BBX.SECTION.ROOT.assertIsA(root);
        return root;
    }

    /**
     * Transform an element into another type. <b>You shouldn't have to use this
     * in normal tools</b>
     */
    public static void transform(@NotNull Element elem, @NotNull SubType destType) {
        elem.setLocalName(destType.coreType.name);
        _ATTRIB_TYPE.set(elem, destType.name);
    }

    // ----------------- Definitions -------------------------
    public static final SectionElement SECTION = new SectionElement();

    public static class SectionElement extends CoreType {
        public final SectionSubType ROOT = new SectionSubType(this, "ROOT");
        public final SectionSubType OTHER = new SectionSubType(this, "OTHER");

        private SectionElement() {
            super("SECTION", false);
            subTypes = List.of(ROOT, OTHER);
        }

        @Override
        public boolean isValidChild(CoreType child) {
            return child == SECTION || child == CONTAINER || child == BLOCK;
        }
    }

    @XmlJavaTypeAdapter(SectionSubType.TypeAdapter.class)
    public static class SectionSubType extends SubType {
        private SectionSubType(SectionElement coreType, String name) {
            super(coreType, name);
        }

        private static class TypeAdapter extends JAXBSubTypeAdapter<SectionSubType> {
            public TypeAdapter() {
                super(SECTION);
            }
        }
    }

    public static final ContainerElement CONTAINER = new ContainerElement();

    public static class ContainerElement extends CoreType {
        public final ListSubType LIST = new ListSubType(this, "LIST");

        public static class ListSubType extends ContainerSubType {
            public final IntAttribute ATTRIB_LIST_LEVEL = new IntAttribute("listLevel");
            public final EnumAttribute<ListType> ATTRIB_LIST_TYPE = new EnumAttribute<>("listType", ListType.class);

            private ListSubType(ContainerElement coreType, String name) {
                super(coreType, name);
            }

            @Deprecated
            @Override
            public Element create() {
                throw new RuntimeException("Call other create method instead");
            }

            public Element create(ListType type) {
                Element elem = super.create();
                ATTRIB_LIST_TYPE.set(elem, type);
                return elem;
            }

            @Override
            public void assertComplete(Element node, StyleDefinitions styleDefs) {
                super.assertComplete(node, styleDefs);

                ATTRIB_LIST_TYPE.has(node);
                validateChildrenOnlyListItems(node, styleDefs);
            }

            private static void validateChildrenOnlyListItems(Element elem, StyleDefinitions styleDefs) {
                if (!BookToBBXConverter.STRICT_MODE) {
                    return;
                }
                for (Element curChildElement : elem.getChildElements()) {
                    CoreType type = BBX.getType(curChildElement);
                    if (type == BLOCK) {
                        if (!BBX.BLOCK.LIST_ITEM
                                .isA(curChildElement)/*
                         * && !BBX.BLOCK.
                         * SPAN_WRAPPER.isA(
                         * curChildElement)
                         */) {
                            if (!_ATTRIB_OVERRIDE_STYLE.has(curChildElement)) {
                                throw new NodeException("unhandled: not a list item, no style", curChildElement);
                            }
                            // TODO: Disabled as lists can container <hd>
                            // <author> and other stuff
                            // Style style =
                            // styleDefs.getStyleByName(_ATTRIB_OVERRIDE_STYLE.get(curChildElement));
                            // String styleBaseName = style.getBaseStyleName();
                            // if (!styleBaseName.equals("Heading") &&
                            // style.getName().equals("")) {
                            // throw new NodeException("unhandled: style with
                            // base " + styleBaseName, curChildElement);
                            // }
                        }
                    } else if (type == CONTAINER) {
                        if (BookToBBXConverter.STRICT_MODE && !BBX.CONTAINER.IMAGE.isA(curChildElement)
                                // TODO: Do we want this nonsense in lists?
                                && !BBX.CONTAINER.TABLE.isA(curChildElement) && !BBX.CONTAINER.BOX.isA(curChildElement)
                                && !ListType.POEM_LINE_GROUP.isA(curChildElement)
                                && !BBX.CONTAINER.FALLBACK.isA(curChildElement)
                                && !BBX.SPAN.FALLBACK.isA(curChildElement)) {
                            log.debug("Test {}", ListType.POEM_LINE_GROUP.validate(curChildElement));
                            throw new NodeException("unhandled list child", curChildElement);
                        }
                    }
                }
            }

        }

        public final ContainerSubType DONT_SPLIT = new ContainerSubType(this, "DONT_SPLIT");

        public final ContainerSubType TABLE = new ContainerSubType(this, "TABLE") {
            // TODO: Removed as this is way to strict
        };
        public final TableRowSubType TABLE_ROW = new TableRowSubType(this, "TABLE_ROW");

        public static class TableRowSubType extends ContainerSubType {
            public final EnumAttribute<TableRowType> ATTRIB_ROW_TYPE = new EnumAttribute<>("rowType",
                    TableRowType.class);

            private TableRowSubType(ContainerElement coreType, String name) {
                super(coreType, name);
            }

            @Deprecated
            @Override
            public Element create() {
                throw new UnsupportedOperationException("Use other create method");
            }

            public Element create(TableRowType rowType) {
                Element elem = super.create();
                ATTRIB_ROW_TYPE.set(elem, rowType);
                return elem;
            }

            @Override
            public void assertComplete(Element node, StyleDefinitions styleDefs) {
                BBX.CONTAINER.TABLE.assertIsA(node.getParent());
            }
        }

        public final ContainerSubType TABLETN = new ContainerSubType(this, "TABLETN");
        public final ContainerSubType DOUBLE_SPACE = new ContainerSubType(this, "DOUBLE_SPACE");
        public final ContainerSubType BOX = new ContainerSubType(this, "BOX");
        public final ImageSubType IMAGE = new ImageSubType(this, "IMAGE");

        public static class ImageSubType extends ContainerSubType {
            public final IntAttribute ATTRIB_GROUP_ID = new IntAttribute("groupId");

            private ImageSubType(ContainerElement coreType, String name) {
                super(coreType, name);
            }
        }

        public final ContainerSubType PRODNOTE = new ContainerSubType(this, "PRODNOTE");
        public final ContainerSubType NOTE = new ContainerSubType(this, "NOTE");
        public final ContainerSubType PROSE = new ContainerSubType(this, "PROSE");
        public final StyleSubType STYLE = new StyleSubType(this, "STYLE");

        public static class StyleSubType extends ContainerSubType {
            private StyleSubType(ContainerElement coreType, String name) {
                super(coreType, name);
            }

            @Deprecated
            @Override
            public Element create() {
                throw new UnsupportedOperationException("Use other create method");
            }

            public Element create(String styleName) {
                Element elem = super.create();
                _ATTRIB_OVERRIDE_STYLE.set(elem, styleName);
                return elem;
            }

            @Override
            protected String subValidate(Element elem) {
                if (StringUtils.isBlank(_ATTRIB_OVERRIDE_STYLE.get(elem))) {
                    return "Missing overrideStyle attrib";
                } else {
                    return null;
                }
            }
        }

        public final ContainerSubType TPAGE = new ContainerSubType(this, "TPAGE");
        public final TPageSectionSubType TPAGE_SECTION = new TPageSectionSubType(this, "TPAGE_SECTION");

        public static class TPageSectionSubType extends ContainerSubType {
            public final EnumAttribute<TPageSection> ATTRIB_TYPE = new EnumAttribute<>("tpageType", TPageSection.class);

            private TPageSectionSubType(ContainerElement coreType, String name) {
                super(coreType, name);
            }

            @Deprecated
            @Override
            public Element create() {
                throw new RuntimeException("Use other create method");
            }

            public Element create(TPageSection sectionType) {
                Element elem = super.create();
                ATTRIB_TYPE.set(elem, sectionType);
                return elem;
            }
        }

        public final TPageCategorySubType TPAGE_CATEGORY = new TPageCategorySubType(this, "TPAGE_CATEGORY");

        public static class TPageCategorySubType extends ContainerSubType {
            public final EnumAttribute<TPageCategory> ATTRIB_TYPE = new EnumAttribute<>("category",
                    TPageCategory.class);

            private TPageCategorySubType(ContainerElement coreType, String name) {
                super(coreType, name);
            }

            @Deprecated
            @Override
            public Element create() {
                throw new RuntimeException("Use other create method");
            }

            public Element create(TPageCategory category) {
                Element elem = super.create();
                ATTRIB_TYPE.set(elem, category);
                return elem;
            }
        }

        public final ContainerSubType VOLUME_TOC = new ContainerSubType(this, "VOLUME_TOC");
        public final VolumeSubType VOLUME = new VolumeSubType(this, "VOLUME");

        public static class VolumeSubType extends ContainerSubType {
            public final EnumAttribute<VolumeType> ATTRIB_TYPE = new EnumAttribute<>("volumeType", VolumeType.class);
            public final BooleanAttribute ATTRIB_FIRST_VOLUME = new BooleanAttribute("firstVolume");

            private VolumeSubType(ContainerElement coreType, String name) {
                super(coreType, name);
            }

            @Deprecated
            @Override
            public Element create() {
                throw new RuntimeException("Use other create method");
            }

            public Element create(VolumeType volumeType) {
                Element elem = super.create();
                ATTRIB_TYPE.set(elem, volumeType);
                return elem;
            }
        }

        public final ContainerSubType FALLBACK = new ContainerSubType(this, "FALLBACK");
        public final ContainerSubType OTHER = new ContainerSubType(this, "OTHER");
        public final ContainerSubType CAPTION = new ContainerSubType(this, "CAPTION");
        public final ContainerSubType BLOCKQUOTE = new ContainerSubType(this, "BLOCKQUOTE");
        public final ContainerSubType DEFAULT = new ContainerSubType(this, "DEFAULT");
        public final NumberLineContainerSubType NUMBER_LINE = new NumberLineContainerSubType(this, "NUMBER_LINE");

        public static class NumberLineContainerSubType extends ContainerSubType {
            public final JsonAttribute<NumberLineJson> JSON_NUMBER_LINE =
                    new JsonAttribute<>("JSON_NUMBER_LINE", NumberLineJson.class);
            public final EnumAttribute<SpatialMathEnum.Fill> ATTRIB_SEGMENT_FILL_START =
                    new EnumAttribute<>(NumberLineConstants.ATTRIB_SEGMENT_FILL_START, SpatialMathEnum.Fill.class);
            public final EnumAttribute<SpatialMathEnum.Fill> ATTRIB_SEGMENT_FILL_END =
                    new EnumAttribute<>(NumberLineConstants.ATTRIB_SEGMENT_FILL_START, SpatialMathEnum.Fill.class);
            public final EnumAttribute<SpatialMathEnum.Fill> ATTRIB_LINE_FILL_START =
                    new EnumAttribute<>(NumberLineConstants.ATTRIB_SEGMENT_FILL_START, SpatialMathEnum.Fill.class);
            public final EnumAttribute<SpatialMathEnum.Fill> ATTRIB_LINE_FILL_END =
                    new EnumAttribute<>(NumberLineConstants.ATTRIB_SEGMENT_FILL_START, SpatialMathEnum.Fill.class);
            public final EnumAttribute<SpatialMathEnum.IntervalType> ATTRIB_INTERVAL_TYPE =
                    new EnumAttribute<>(NumberLineConstants.ATTRIB_INTERVAL_TYPE, SpatialMathEnum.IntervalType.class);
            public final BooleanAttribute ATTRIB_HAS_SEGMENT =
                    new BooleanAttribute(NumberLineConstants.ATTRIB_HAS_SEGMENT);
            public final BooleanAttribute ATTRIB_ARROW =
                    new BooleanAttribute(NumberLineConstants.ATTRIB_ARROW);
            public final BooleanAttribute ATTRIB_STRETCH =
                    new BooleanAttribute(NumberLineConstants.ATTRIB_STRETCH);
            public final BooleanAttribute ATTRIB_REDUCE_FRACTION =
                    new BooleanAttribute(NumberLineConstants.ATTRIB_REDUCE_FRACTION);
            public final BooleanAttribute ATTRIB_LEADING_ZEROS =
                    new BooleanAttribute(NumberLineConstants.ATTRIB_LEADING_ZEROS);
            public final StringAttribute ATTRIB_SEGMENT_START_WHOLE =
                    new StringAttribute(NumberLineConstants.ATTRIB_SEGMENT_START_WHOLE);
            public final StringAttribute ATTRIB_SEGMENT_END_WHOLE =
                    new StringAttribute(NumberLineConstants.ATTRIB_SEGMENT_END_WHOLE);
            public final StringAttribute ATTRIB_LINE_START_WHOLE =
                    new StringAttribute(NumberLineConstants.ATTRIB_LINE_START_WHOLE);
            public final StringAttribute ATTRIB_LINE_END_WHOLE =
                    new StringAttribute(NumberLineConstants.ATTRIB_LINE_END_WHOLE);
            public final StringAttribute ATTRIB_INTERVAL_WHOLE =
                    new StringAttribute(NumberLineConstants.ATTRIB_INTERVAL_WHOLE);
            public final StringAttribute ATTRIB_INTERVAL_NUMERATOR =
                    new StringAttribute(NumberLineConstants.ATTRIB_INTERVAL_NUMERATOR);
            public final StringAttribute ATTRIB_INTERVAL_DENOMINATOR =
                    new StringAttribute(NumberLineConstants.ATTRIB_INTERVAL_DENOMINATOR);
            public final StringAttribute ATTRIB_SEGMENT_START_NUMERATOR =
                    new StringAttribute(NumberLineConstants.ATTRIB_SEGMENT_START_NUMERATOR);
            public final StringAttribute ATTRIB_SEGMENT_START_DENOMINATOR =
                    new StringAttribute(NumberLineConstants.ATTRIB_SEGMENT_START_DENOMINATOR);
            public final StringAttribute ATTRIB_SEGMENT_END_NUMERATOR =
                    new StringAttribute(NumberLineConstants.ATTRIB_SEGMENT_END_NUMERATOR);
            public final StringAttribute ATTRIB_SEGMENT_END_DENOMINATOR =
                    new StringAttribute(NumberLineConstants.ATTRIB_SEGMENT_END_DENOMINATOR);
            public final StringAttribute ATTRIB_LINE_START_NUMERATOR =
                    new StringAttribute(NumberLineConstants.ATTRIB_LINE_START_NUMERATOR);
            public final StringAttribute ATTRIB_LINE_START_DENOMINATOR =
                    new StringAttribute(NumberLineConstants.ATTRIB_LINE_START_DENOMINATOR);
            public final StringAttribute ATTRIB_LINE_END_NUMERATOR =
                    new StringAttribute(NumberLineConstants.ATTRIB_LINE_END_NUMERATOR);
            public final StringAttribute ATTRIB_LINE_END_DENOMINATOR =
                    new StringAttribute(NumberLineConstants.ATTRIB_LINE_END_DENOMINATOR);
            public final StringAttribute ATTRIB_SEGMENT_START_DECIMAL =
                    new StringAttribute(NumberLineConstants.ATTRIB_SEGMENT_START_DECIMAL);
            public final StringAttribute ATTRIB_SEGMENT_END_DECIMAL =
                    new StringAttribute(NumberLineConstants.ATTRIB_SEGMENT_END_DECIMAL);
            public final StringAttribute ATTRIB_LINE_START_DECIMAL =
                    new StringAttribute(NumberLineConstants.ATTRIB_LINE_START_DECIMAL);
            public final StringAttribute ATTRIB_LINE_END_DECIMAL =
                    new StringAttribute(NumberLineConstants.ATTRIB_LINE_END_DECIMAL);
            public final StringAttribute ATTRIB_INTERVAL_DECIMAL =
                    new StringAttribute(NumberLineConstants.ATTRIB_INTERVAL_DECIMAL);
            public final EnumAttribute<Passage> NUMERIC_PASSAGE =
                    new EnumAttribute<>("numericPassage", Passage.class);
            public final EnumAttribute<org.brailleblaster.math.spatial.SpatialMathEnum.NumberLineType> NUMBER_LINE_TYPE =
                    new EnumAttribute<>("numberLineType",
                            org.brailleblaster.math.spatial.SpatialMathEnum.NumberLineType.class);
            public final EnumAttribute<Translation> USER_DEFINED_TRANSLATION =
                    new EnumAttribute<>("userDefinedTranslation", Translation.class);
            public final StringArrayAttribute USER_DEFINED_SEGMENTS =
                    new StringArrayAttribute("userDefinedSegments");
            public final StringAttribute ATTRIB_VERSION =
                    new StringAttribute("version");

            private NumberLineContainerSubType(ContainerElement coreType, String name) {
                super(coreType, name);
            }

            @Deprecated
            @Override
            public Element create() {
                throw new UnsupportedOperationException("Use other create method");
            }

            public Element create(NumberLine numberLine) {
                Element elem = super.create();
                String version = MathModuleUtils.SPATIAL_MATH_BBX_VERSION;
                JSON_NUMBER_LINE.set(elem, numberLine.getJson());
                ATTRIB_VERSION.set(elem, version);
                return elem;
            }
        }

        public final MatrixContainerSubType MATRIX = new MatrixContainerSubType(this, "MATRIX");

        public static class MatrixContainerSubType extends ContainerSubType {
            public final JsonAttribute<MatrixJson> JSON_MATRIX = new JsonAttribute<>("JSON_MATRIX", MatrixJson.class);
            public final StringAttribute ATTRIB_VERSION = new StringAttribute("version");
            public final IntAttribute ROWS = new IntAttribute(MatrixConstants.ROW_KEY);
            public final IntAttribute COLS = new IntAttribute(MatrixConstants.COL_KEY);
            public final EnumAttribute<Wide> WIDE_TYPE = new EnumAttribute<>("wideType", Wide.class);
            public final EnumAttribute<BracketType> BRACKET = new EnumAttribute<>("bracketType", BracketType.class);
            public final StringArrayAttribute ASCII_MATH = new StringArrayAttribute("asciiMath");
            public final StringArrayAttribute ELLIPSES_ARRAY = new StringArrayAttribute("ellipsesArray");
            public final EnumAttribute<Passage> NUMERIC_PASSAGE = new EnumAttribute<>("numericPassage", Passage.class);
            public final EnumAttribute<Translation> MATRIX_TRANSLATION = new EnumAttribute<>("matrixTranslation", Translation.class);

            private MatrixContainerSubType(ContainerElement coreType, String name) {
                super(coreType, name);
            }

            @Deprecated
            @Override
            public Element create() {
                return super.create();
            }

            public Element create(Matrix matrix) {
                Element elem = super.create();
                JSON_MATRIX.set(elem, matrix.getJson());
                String version = MathModuleUtils.SPATIAL_MATH_BBX_VERSION;
                ATTRIB_VERSION.set(elem, version);
                return elem;
            }
        }

        public final TemplateContainerSubType TEMPLATE = new TemplateContainerSubType(this, "TEMPLATE");

        public static class TemplateContainerSubType extends ContainerSubType {
            public final JsonAttribute<TemplateJson> JSON_TEMPLATE = new JsonAttribute<>("JSON_TEMPLATE", TemplateJson.class);
            public final BooleanAttribute STRAIGHT_RADICAL = new BooleanAttribute("straightRadical");
            public final EnumAttribute<org.brailleblaster.math.spatial.SpatialMathEnum.OPERATOR> OPERATOR = new EnumAttribute<>(
                    "operator", org.brailleblaster.math.spatial.SpatialMathEnum.OPERATOR.class);
            public final EnumAttribute<org.brailleblaster.math.spatial.SpatialMathEnum.TemplateType> TYPE = new EnumAttribute<>(
                    "templateType", org.brailleblaster.math.spatial.SpatialMathEnum.TemplateType.class);
            public final StringArrayAttribute OPERANDS = new StringArrayAttribute("operands");
            public final StringArrayAttribute SOLUTIONS = new StringArrayAttribute("solutions");
            public final EnumAttribute<Passage> PASSAGE_MODE = new EnumAttribute<>("passageMode", Passage.class);
            public final StringAttribute IDENTIFIER = new StringAttribute("identifier");
            public final BooleanAttribute IDENTIFER_AS_MATH = new BooleanAttribute("mathIdentifier");
            public final BooleanAttribute LINEAR = new BooleanAttribute("linear");
            public final BooleanAttribute NEMETH = new BooleanAttribute("nemeth");
            public final StringAttribute ATTRIB_VERSION = new StringAttribute("version");

            private TemplateContainerSubType(ContainerElement coreType, String name) {
                super(coreType, name);
            }

            @Deprecated
            @Override
            public Element create() {
                return super.create();
            }

            public Element create(Template template) {
                Element elem = super.create();
                String version = MathModuleUtils.SPATIAL_MATH_BBX_VERSION;
                JSON_TEMPLATE.set(elem, template.getJson());
                ATTRIB_VERSION.set(elem, version);
                return elem;
            }
        }

        public final SpatialGridContainerSubType SPATIAL_GRID = new SpatialGridContainerSubType(this, "SPATIAL_GRID");

        public static class SpatialGridContainerSubType extends ContainerSubType {
            final Gson gson = new GsonBuilder().registerTypeAdapter(ISpatialMathContainerJson.class,
                            new GsonInterfaceAdapter<ISpatialMathContainerJson>())
                    .create();
            public final JsonAttribute<GridJson> JSON_GRID = new JsonAttribute<>("JSON_GRID", GridJson.class, gson);
            public final StringAttribute ATTRIB_VERSION = new StringAttribute("version");
            public final IntAttribute ROWS = new IntAttribute("rows");
            public final IntAttribute COLS = new IntAttribute("cols");
            public final XMLAttribute GRID = new XMLAttribute("grid");

            private SpatialGridContainerSubType(ContainerElement coreType, String name) {
                super(coreType, name);
            }

            @Deprecated
            @Override
            public Element create() {
                return super.create();
            }

            public Element create(Grid grid) {
                Element elem = super.create();
                JSON_GRID.set(elem, grid.getJson());
                String version = MathModuleUtils.SPATIAL_MATH_BBX_VERSION;
                ATTRIB_VERSION.set(elem, version);
                return elem;
            }
        }

        public final ConnectingContainerSubType CONNECTING_CONTAINER = new ConnectingContainerSubType(this,
                "CONNECTING_CONTAINER");

        public static class ConnectingContainerSubType extends ContainerSubType {
            public final JsonAttribute<ConnectingContainerJson> JSON_CONNECTING_CONTAINER = new JsonAttribute<>(
                    "JSON_CONNECTING_CONTAINER", ConnectingContainerJson.class);
            public final StringAttribute ATTRIB_VERSION = new StringAttribute("version");
            public final StringAttribute TEXT = new StringAttribute("text");
            public final BooleanAttribute IS_MATH = new BooleanAttribute("isMath");
            public final EnumAttribute<SpatialMathEnum.VerticalJustify> VERTICAL = new EnumAttribute<>(
                    "vertical", SpatialMathEnum.VerticalJustify.class);
            public final EnumAttribute<SpatialMathEnum.HorizontalJustify> HORIZONTAL = new EnumAttribute<>(
                    "horizontal", SpatialMathEnum.HorizontalJustify.class);

            private ConnectingContainerSubType(ContainerElement coreType, String name) {
                super(coreType, name);
            }

            @Deprecated
            @Override
            public Element create() {
                return super.create();
            }

            public Element create(ConnectingContainer container) {
                Element elem = super.create();
                JSON_CONNECTING_CONTAINER.set(elem, container.getJson());
                String version = MathModuleUtils.SPATIAL_MATH_BBX_VERSION;
                ATTRIB_VERSION.set(elem, version);
                return elem;
            }
        }

        private ContainerElement() {
            super("CONTAINER", false);
            subTypes = List.of(LIST, DONT_SPLIT, TABLE, TABLE_ROW, TABLETN, DOUBLE_SPACE, BOX, IMAGE, PRODNOTE,
                    NOTE, PROSE, STYLE, TPAGE, TPAGE_SECTION, TPAGE_CATEGORY, VOLUME_TOC, VOLUME, FALLBACK, OTHER,
                    CAPTION, BLOCKQUOTE, DEFAULT, NUMBER_LINE, MATRIX, TEMPLATE, SPATIAL_GRID, CONNECTING_CONTAINER);
        }

        @Override
        public boolean isValidChild(CoreType child) {
            return child == CONTAINER || child == BLOCK;
        }
    }

    @XmlJavaTypeAdapter(ContainerSubType.TypeAdapter.class)
    public static class ContainerSubType extends SubType {
        private ContainerSubType(ContainerElement coreType, String name) {
            super(coreType, name);
        }

        private static class TypeAdapter extends JAXBSubTypeAdapter<ContainerSubType> {
            public TypeAdapter() {
                super(CONTAINER);
            }
        }
    }

    /**
     * The types of list container subtypes...
     */
    public enum ListType implements NodeValidator {
        NORMAL("L"), DEFINITION("G"), POEM("P"), POEM_LINE_GROUP("P");

        public final String styleNamePrefix;

        ListType(String stylePrefix) {
            this.styleNamePrefix = stylePrefix;
        }

        @Override
        public String validate(Node node) {
            String isList = BBX.CONTAINER.LIST.validate(node);
            if (isList != null) {
                return isList;
            }
            ListType listType = BBX.CONTAINER.LIST.ATTRIB_LIST_TYPE.get((Element) node);
            return listType == this ? null : "listType is " + listType + " not " + this;
        }
    }

    public enum MarginType {
        TOC("T"), EXERCISE("E"), INDEX("I"), NUMERIC("");

        public final String styleNamePrefix;

        MarginType(String styleNamePrefix) {
            this.styleNamePrefix = styleNamePrefix;
        }
    }

    /**
     * The types of table row container subtypes...
     */
    public enum TableRowType {
        NORMAL, HEAD, FOOT
    }

    public enum TPageSection {
        TITLE_PAGE, SECOND_TITLE_PAGE, SPECIAL_SYMBOLS, TRANSCRIBER_NOTES
    }

    public enum TPageCategory {
        TITLE, AUTHOR, PUBLISHER, TRANSCRIPTION, VOLUMES
    }

    public enum VolumeType {
      VOLUME_PRELIMINARY("Preliminary Volume", "Preliminary", "Preliminary"),
      VOLUME("Volume", "Volume", "Normal"),
      VOLUME_SUPPLEMENTAL("Supplemental Volume", "Supplemental", "Supplemental");

        public final String volumeName, volumeNameShort, volumeMenuName;

        VolumeType(String volumeName, String volumeNameShort, String volumeMenuName) {
            this.volumeName = volumeName;
            this.volumeNameShort = volumeNameShort;
            this.volumeMenuName = volumeMenuName;
        }
    }

    public static boolean isBlockOrChild(Node node) {
        return BBX.BLOCK.isA(node) || BBX.SPAN.isA(node) || BBX.INLINE.isA(node);
    }

    public static final BlockElement BLOCK = new BlockElement();

    public static class BlockElement extends CoreType {
        public final ListItemSubType LIST_ITEM = new ListItemSubType(this);
        public final BooleanAttribute ATTRIB_BLANKDOC_PLACEHOLDER = new BooleanAttribute("blankDocPlaceholder");
        public final StringAttribute LINKID = new StringAttribute("linkID");

        public static class ListItemSubType extends BlockSubType {
            public final IntAttribute ATTRIB_ITEM_LEVEL = new IntAttribute("itemLevel");

            private ListItemSubType(BlockElement type) {
                super(type, "LIST_ITEM");
            }

            @Override
            public Element create() {
                Element elem = super.create();
                ATTRIB_ITEM_LEVEL.set(elem, 0);
                return elem;
            }

            @Override
            protected String subValidate(Element elem) {
                if (!ATTRIB_ITEM_LEVEL.has(elem)) {
                    return "Missing itemLevel attrib";
                } else {
                    return null;
                }
            }

            @Override
            public void assertComplete(Element node, StyleDefinitions styleDefs) {
                super.assertComplete(node, styleDefs);

                Element parentList = XMLHandler.Companion.ancestorVisitorElement(node,
                        BBX.CONTAINER.LIST::isA);
                if (parentList == null) {
                    throw new NodeException("List item not under list", node);
                }
                if (BBX.ListType.POEM_LINE_GROUP.isA(parentList)) {
                    Element poemWrapper = XMLHandler.Companion.ancestorVisitorElement(parentList.getParent(),
                            ListType.POEM::isA);
                    if (poemWrapper != null) {
                        parentList = poemWrapper;
                    }
                }
                int listLevel = BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.get(parentList);
                int itemLevel = ATTRIB_ITEM_LEVEL.get(node);
                if (listLevel < itemLevel) {
                    throw new NodeException(
                            "Size of list item " + itemLevel + " is greater than parent list " + listLevel, node);
                }
            }
        }

        public final BlockSubType TABLE_CELL = new BlockSubType(this, "TABLE_CELL") {
            @Override
            protected String subValidate(Element elem) {
                if (_ATTRIB_OVERRIDE_STYLE.has(elem)) {
                    return "Unexpected override style attrib, should not exist as table formatter ignores them";
                } else {
                    return null;
                }
            }

            @Override
            public void assertComplete(Element node, StyleDefinitions styleDefs) {
                if (!BBX.CONTAINER.TABLE_ROW.isA(node.getParent())) {
                    throw new NodeException("Unexpected parent of table cell", node);
                }
            }
        };

        public final MarginSubType MARGIN = new MarginSubType(this);

        public static class MarginSubType extends BlockSubType {
            public final IntAttribute ATTRIB_INDENT = new IntAttribute("marginIndent");
            public final IntAttribute ATTRIB_RUNOVER = new IntAttribute("marginRunover");
            public final EnumAttribute<MarginType> ATTRIB_MARGIN_TYPE = new EnumAttribute<>("marginType",
                    MarginType.class);

            private MarginSubType(BlockElement type) {
                super(type, "MARGIN");
            }

            @Override
            protected String subValidate(Element elem) {
                if (!ATTRIB_INDENT.has(elem)) {
                    return "No marginIndent attribute";
                } else if (!ATTRIB_RUNOVER.has(elem)) {
                    return "No marginRunover attribute";
                } else {
                    return null;
                }
            }
        }

        public final BlockSubType VOLUME_END = new BlockSubType(this, "VOLUME_END");
        public final BlockSubType TOC_VOLUME_SPLIT = new BlockSubType(this, "TOC_VOLUME_SPLIT");
        public final PageNumBlockSubType PAGE_NUM = new PageNumBlockSubType(this, "PAGE_NUM");

        public static class PageNumBlockSubType extends BlockSubType {
            private PageNumBlockSubType(BlockElement coreType, String name) {
                super(coreType, name);
            }

            @Override
            public boolean isA(Node node) {
                return super.isA(node);
            }

            /**
             * <b>NOTE: You probably should also check SPAN.PAGE_NUM
             */
            @Override
            public void assertComplete(Element node, StyleDefinitions styleDefs) {
                super.assertComplete(node, styleDefs);
            }

            @Override
            public void assertIsA(Node node) throws IllegalBBXException {
                super.assertIsA(node);
            }
        }

        public final ImagePlaceholderType IMAGE_PLACEHOLDER = new ImagePlaceholderType(this, "IMAGE_PLACEHOLDER");

        public static class ImagePlaceholderType extends BlockSubType {
            public final IntAttribute ATTRIB_SKIP_LINES = new IntAttribute("skipLines", UTDElements.UTD_PREFIX,
                    NamespacesKt.UTD_NS);
            public final StringAttribute ATTRIB_IMG_PATH = new StringAttribute("src", UTDElements.UTD_PREFIX,
                    NamespacesKt.UTD_NS);
            public final StringAttribute ATTRIB_ALT_TEXT = new StringAttribute("alt", UTDElements.UTD_PREFIX,
                    NamespacesKt.UTD_NS);

            private ImagePlaceholderType(BlockElement coreType, String name) {
                super(coreType, name);
            }
        }

        public final BlockSubType DEFAULT = new BlockSubType(this, "DEFAULT");
        public final StyleSubType STYLE = new StyleSubType(this, "STYLE");

        public static class StyleSubType extends BlockSubType {
            private StyleSubType(BlockElement coreType, String name) {
                super(coreType, name);
            }

            @Deprecated
            @Override
            public Element create() {
                throw new UnsupportedOperationException("Use other create method");
            }

            public Element create(String styleName) {
                Element elem = super.create();
                _ATTRIB_OVERRIDE_STYLE.set(elem, styleName);
                return elem;
            }

            @Override
            protected String subValidate(Element elem) {
                if (_ATTRIB_OVERRIDE_STYLE.has(elem) && StringUtils.isNotBlank(_ATTRIB_OVERRIDE_STYLE.get(elem))) {
                    return null;
                }
                return "Missing overrideStyle attrib";
            }
        }

        public final SpatialMathSubType SPATIAL_MATH = new SpatialMathSubType(this, "SPATIAL_MATH");

        public static class SpatialMathSubType extends BlockSubType {
            private SpatialMathSubType(BlockElement coreType, String name) {
                super(coreType, name);
            }

            @Override
            public Element create() {
                Element elem = super.create();
                _ATTRIB_OVERRIDE_STYLE.set(elem, "Spatial Math");
                return elem;
            }
        }

        private BlockElement() {
            super("BLOCK", true);
            subTypes = List.of(LIST_ITEM, TABLE_CELL, MARGIN, VOLUME_END, TOC_VOLUME_SPLIT, PAGE_NUM,
                    IMAGE_PLACEHOLDER, DEFAULT, STYLE, SPATIAL_MATH);
        }

        @Override
        public boolean isValidChild(CoreType child) {
            return child == INLINE || child == SPAN;
        }
    }

    @XmlJavaTypeAdapter(BlockSubType.TypeAdapter.class)
    public static class BlockSubType extends SubType {
        public final StringAttribute linkID;

        private BlockSubType(BlockElement coreType, String name) {
          super(coreType, name);
          this.linkID = new StringAttribute("linkID"); // Optional attribute for internally linking blocks
          //May expand to other subtypes depending on how much work it is...or how well it works.
          //Idea is to set some property in the manager to the highest linkID in the doc to avoid collisions.
          //Then when adding or removing internal links, update as needed. The actual number is irrelevant beyond uniqueness.
          //Broken links need to be handled gracefully as well - if the linked block is deleted, the link manager should
          // notify the user and remove the linkID attribute from the link, or prompt to reassign it.
        }

        private static class TypeAdapter extends JAXBSubTypeAdapter<BlockSubType> {
            public TypeAdapter() {
                super(BLOCK);
            }
        }
    }

    public static final InlineElement INLINE = new InlineElement();

    public static class InlineElement extends CoreType {
        public final EmphasisSubType EMPHASIS = new EmphasisSubType(this);
        public final MathSubType MATHML = new MathSubType(this);
        public final LinkSubType LINK = new LinkSubType(this);

        public static class EmphasisSubType extends InlineSubType {
            public final EnumSetAttribute<EmphasisType> ATTRIB_EMPHASIS =
                new EnumSetAttribute<>("emphasis", EmphasisType.class);

            private EmphasisSubType(InlineElement coreType) {
                super(coreType, "EMPHASIS");
            }

            @Override
            protected String subValidate(Element elem) {
                // TODO: Will throw an exception if it fails
                ATTRIB_EMPHASIS.get(elem);
                return null;
            }

            @Override
            public void assertComplete(Element node, StyleDefinitions styleDefs) {
                if (ATTRIB_EMPHASIS.get(node).isEmpty()) {
                    throw new NodeException("Missing emphasis bits", node);
                }
            }

            @Override
            @Deprecated
            public Element create() {
                throw new RuntimeException("don't use me");
            }

            public Element create(EnumSet<EmphasisType> emphasisBits) {
                Element create = super.create();
                ATTRIB_EMPHASIS.set(create, emphasisBits);
                return create;
            }

            public Element create(EmphasisType... emphasisBits) {
                return create(EnumSet.copyOf(Arrays.asList(emphasisBits)));
            }
        }

        public static class MathSubType extends InlineSubType {

            private MathSubType(InlineElement coreType) {
                super(coreType, "MATHML");
            }

            @Override
            public void assertComplete(Element node, StyleDefinitions styleDefs) {
                if (node.getChildCount() != 1) {
                    throw new NodeException("MATHML container can only contain <m:math> tag", node);
                }
                Element mathTag = node.getChildElements().get(0);
                if (!mathTag.getLocalName().equals("math")) {
                    throw new NodeException("Expected only container child to be math, got ", mathTag);
                }

                List<Element> nonMathMl = StreamSupport.stream(((Iterable<Node>)FastXPath.descendant(node)::iterator).spliterator(), false).filter(n -> n instanceof Element)
                        .map(n -> (Element) n).filter(e -> !e.getNamespaceURI().equals(NamespacesKt.MATHML_NS)).toList();
                if (!nonMathMl.isEmpty()) {
                    throw new NodeException("Unexpected non-mathml elements " + StringUtils.join(nonMathMl, ", "),
                            node);
                }
            }
        }

        public static class LinkSubType extends InlineSubType{

          public final StringAttribute ATTRIB_HREF = new StringAttribute("href");
          public final BooleanAttribute IS_EXTERNAL = new BooleanAttribute("external");

          private LinkSubType(InlineElement coreType) {
            super(coreType, "LINK");
          }

          @Override
          protected String subValidate(Element elem) {
            if (ATTRIB_HREF.has(elem) && IS_EXTERNAL.has(elem)) {
              return null;
            }
            return "Link must have href and external attributes";
          }
        }

        public final InlineSubType LINE_BREAK = new InlineSubType(this, "LINE_BREAK");

        private InlineElement() {
            super("INLINE", true);
            subTypes = List.of(EMPHASIS, MATHML, LINE_BREAK, LINK);
        }

        @Override
        public boolean isValidChild(CoreType child) {
            return child == INLINE;
        }
    }

    @XmlJavaTypeAdapter(InlineSubType.TypeAdapter.class)
    public static class InlineSubType extends SubType {
        private InlineSubType(InlineElement coreType, String name) {
            super(coreType, name);
        }

        private static class TypeAdapter extends JAXBSubTypeAdapter<InlineSubType> {
            public TypeAdapter() {
                super(INLINE);
            }
        }
    }

    public static final SpanElement SPAN = new SpanElement();

    public static class SpanElement extends CoreType {
        public final ImageSubType IMAGE = new ImageSubType(this, "IMAGE");

        public static class ImageSubType extends SpanSubType {
            public final StringAttribute ATTRIB_SOURCE = new StringAttribute("source");

            private ImageSubType(SpanElement coreType, String name) {
                super(coreType, name);
            }
        }

        public final PageNumSpanSubType PAGE_NUM = new PageNumSpanSubType(this, "PAGE_NUM");

        public static class PageNumSpanSubType extends SpanSubType {
            private PageNumSpanSubType(SpanElement coreType, String name) {
                super(coreType, name);
            }

            @Override
            public boolean isA(Node node) {
                return super.isA(node);
            }

            /**
             * <b>NOTE: You probably should also check BLOCK.PAGE_NUM
             */
            @Override
            public void assertComplete(Element node, StyleDefinitions styleDefs) {
                super.assertComplete(node, styleDefs);
            }

            @Override
            public void assertIsA(Node node) throws IllegalBBXException {
                super.assertIsA(node);
            }
        }

        public final SpanSubType SUPERSCRIPT = new SpanSubType(this, "SUPERSCRIPT");
        public final SpanSubType POEM_LINE_NUMBER = new SpanSubType(this, "POEM_LINE_NUMBER");
        public final SpanSubType PROSE_LINE_NUMBER = new SpanSubType(this, "PROSE_LINE_NUMBER");
        public final SpanSubType DEFINITION_TERM = new SpanSubType(this, "DEFINITION_TERM") {
            // Does not properly handle mandled d
        };
        /**
         * @see TabAction
         */
        public final TabSpanSubType TAB = new TabSpanSubType(this, "TAB");

        public static class TabSpanSubType extends SpanSubType {
            public final IntAttribute ATTRIB_VALUE = new IntAttribute("tabValue", null, null);

            private TabSpanSubType(SpanElement coreType, String name) {
                super(coreType, name);
            }

            @Override
            protected String subValidate(Element elem) {
                if (elem.getChildCount() != 0) {
                    return "tab is supposed to be empty";
                }
                return null;
            }
        }

        public final SpanSubType FALLBACK = new SpanSubType(this, "FALLBACK");
        public final SpanSubType NOTEREF = new SpanSubType(this, "NOTEREF");
        public final SpanSubType GUIDEWORD = new SpanSubType(this, "GUIDEWORD");
        public final SpanSubType OTHER = new SpanSubType(this, "OTHER");

        private SpanElement() {
            super("SPAN", true);
            subTypes = List.of(IMAGE, PAGE_NUM, SUPERSCRIPT, POEM_LINE_NUMBER, PROSE_LINE_NUMBER,
                    DEFINITION_TERM, TAB, FALLBACK, NOTEREF, GUIDEWORD, OTHER);
        }

        @Override
        public boolean isValidChild(CoreType child) {
            return child == SPAN || child == INLINE;
        }
    }

    @XmlJavaTypeAdapter(SpanSubType.TypeAdapter.class)
    public static class SpanSubType extends SubType {
        private SpanSubType(SpanElement coreType, String name) {
            super(coreType, name);
        }

        private static class TypeAdapter extends JAXBSubTypeAdapter<SpanSubType> {
            public TypeAdapter() {
                super(SPAN);
            }
        }
    }

    public enum FixerTodo {
        LINE_BREAK, TABLE_GROUP_UNWRAP, TABLE_SIZE, TABLE_CELL_REAL, CONTAINER_STYLE_TO_BLOCK, CONVERT_IMAGE_GROUP,
    }

    public enum FixerMarker {
        DEFINITION_LINE_BREAK_SPLIT;

        public static final EnumAttribute<FixerMarker> ATTRIB_FIXER_MARKER = new EnumAttribute<>("fixerMarker",
                FixerMarker.class);

        public boolean has(Node node) {
            if (!ATTRIB_FIXER_MARKER.has(node)) {
                return false;
            }

            return ATTRIB_FIXER_MARKER.get((Element) node) == this;
        }
    }

    public enum PreFormatterMarker {
        LIST_SPLIT;

        public static final EnumAttribute<PreFormatterMarker> ATTRIB_PRE_FORMATTER_MARKER = new EnumAttribute<>(
                "preFormatterMarker", PreFormatterMarker.class);

        public boolean has(Node node) {
            if (!ATTRIB_PRE_FORMATTER_MARKER.has(node)) {
                return false;
            }

            return ATTRIB_PRE_FORMATTER_MARKER.get((Element) node) == this;
        }
    }

    public static final List<BBX.@NotNull CoreType> CORE_TYPES =
        List.of(SECTION, CONTAINER, BLOCK, INLINE, SPAN);

    public static CoreType getType(Element elem) {
        CoreType result = getTypeOrNull(elem);
        if (result == null) {
            throw new NodeException("No coreType found for element", elem);
        }
        return result;
    }

    public static CoreType getTypeOrNull(Element elem) {
        for (CoreType coreType : CORE_TYPES) {
            if (coreType.name.equals(elem.getLocalName())) {
                return coreType;
            }
        }
        return null;
    }

    public static boolean isA(Node node) {
        if (!(node instanceof Element)) {
            return false;
        }
        Element elem = (Element) node;

        return getTypeOrNull(elem) != null;
    }

    // ---------------------- "Java Enterprise Verbose Edition" classes
    // -----------------------
    public interface NodeValidator {
        @Nullable
        String validate(Node node);

        default void assertIsA(Node node) throws IllegalBBXException {
            String validateResult = validate(node);
            if (validateResult != null) {
                throw new IllegalBBXException(node, validateResult);
            }
        }

        default boolean isA(Node node) {
            return validate(node) == null;
        }

        default void assertComplete(Element node, StyleDefinitions styleDefs) {
            assertIsA(node);
        }
    }

    @XmlJavaTypeAdapter(CoreType.TypeAdapter.class)
    public static abstract class CoreType implements NodeValidator {
        public final String name;
        public final boolean textChildrenValid;
        // TODO: It would be nice if subtypes could be passed as a varargs
        // argument instead of an abstract method
        // However all subtypes are stored in fields and fields can't be
        // accessed while calling super(), thanks java
        // If the fields are static they work, but IDE autocomplete only shows
        // instance fields/methods
        // So this workaround was used so getSubType works, is performant, can
        // still use final, just a bit more verbose
        protected List<@NotNull SubType> subTypes;

        public CoreType(String name, boolean textChildrenValid) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalArgumentException("name is blank");
            }
            this.name = name;
            this.textChildrenValid = textChildrenValid;
        }

        public abstract boolean isValidChild(CoreType child);

        public SubType getSubType(Node sectionNode) {
            assertIsA(sectionNode);

            String subtypeName = _ATTRIB_TYPE.get((Element) sectionNode);
            for (SubType subType : subTypes) {
                if (subType.name.equals(subtypeName)) {
                    return subType;
                }
            }
            throw new NodeException("Missing subtype " + subtypeName + " for", sectionNode);
        }

        public List<@NotNull SubType> getSubTypes() {
            return subTypes;
        }

        @Override
        public final String validate(Node node) {
            if (node == null) {
                throw new NullPointerException("node");
            }

            if (!(node instanceof Element)) {
                return "Expected element";
            }
            Element element = (Element) node;

            if (!NamespacesKt.BB_NS.equals(element.getNamespaceURI())) {
                return "Not in BB namespace";
            }

            if (!name.equals(element.getLocalName())) {
                return "Expected element name " + name;
            }

            return null;
        }

        public Element create(SubType subType) {
            if (subType.coreType != this) {
                throw new IllegalArgumentException("Unexpected subType " + subType);
            }
            Element elem = newElement(name);
            subType.set(elem);
            return elem;
        }

        @Override
        public String toString() {
            return getClass().getName() + "{" + "name=" + name
                    // + ", textChildrenValid=" + textChildrenValid
                    // + ", subTypes=" + subTypes
                    + '}';
        }

        public static class TypeAdapter extends XmlAdapter<String, CoreType> {
            @Override
            public String marshal(CoreType v) {
                return v.name;
            }

            @Override
            public CoreType unmarshal(String v) {
                for (CoreType curCoreType : CORE_TYPES) {
                    if (curCoreType.name.equals(v)) {
                        return curCoreType;
                    }
                }
                throw new RuntimeException("Cannot find type for " + v);
            }
        }
    }

    public static abstract class SubType implements NodeValidator {
        public final CoreType coreType;
        public final String name;

        public SubType(CoreType coreType, String name) {
            if (coreType == null) {
                throw new NullPointerException("coreType");
            }
            if (StringUtils.isBlank(name)) {
                throw new IllegalArgumentException("name is blank");
            }
            this.coreType = coreType;
            this.name = name;
        }

        public Element create() {
            return coreType.create(this);
        }

        public void set(Element container) {
            coreType.assertIsA(container);

            _ATTRIB_TYPE.set(container, name);
        }

        @Override
        public boolean isA(Node node) {
            return coreType.isA(node) && this.name.equals(_ATTRIB_TYPE.get((Element) node));
        }

        @Override
        public final String validate(Node node) {
            String blockValid = coreType.validate(node);
            if (blockValid != null) {
                return blockValid;
            }

            String nodeType = _ATTRIB_TYPE.get((Element) node);
            if (nodeType == null) {
                return "missing attribute bb:type";
            } else if (!nodeType.equals(name)) {
                return "unexpected bb:type of " + nodeType + " expected " + name;
            }

            return subValidate((Element) node);
        }

        protected String subValidate(Element elem) {
            return null;
        }

        @Override
        public String toString() {
            return getClass().getName() + "{" + "coreType=" + coreType + ", name=" + name + '}';
        }
    }

    /**
     * Useful wrapper around attributes
     *
     * @param <T> Type of actual value
     */
    public static abstract class BaseAttribute<T> {
        public final String name;
        public final String nsPrefix;
        public final String nsUrl;

        public BaseAttribute(String name) {
            this.name = name;
            this.nsPrefix = "bb";
            this.nsUrl = NamespacesKt.BB_NS;
        }

        public BaseAttribute(String name, String nsPrefix, String nsUrl) {
            this.name = name;
            this.nsPrefix = nsPrefix;
            this.nsUrl = nsUrl;
        }

        public Attribute getAttribute(Element element) {
            if (element == null) {
                throw new NullPointerException("element");
            } else if (isNotNamespaced()) {
                return element.getAttribute(name);
            }
            return element.getAttribute(name, nsUrl);
        }

        public T get(Element elem) {
            // TODO: Assert?
            return getOptional(elem).orElseThrow(() -> new NodeException("Cannot find attribute " + name, elem));
        }

        public Optional<T> getOptional(Element elem) {
            Attribute attribute = getAttribute(elem);
            if (attribute == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(unmarshall(attribute.getValue()));
        }

        public Attribute newAttribute(T value) {
            if (isNotNamespaced()) {
                return new Attribute(name, marshall(value));
            }
            return new Attribute(nsPrefix + ":" + name, nsUrl, marshall(value));
        }

        public void set(Element elem, T value) {
            elem.addAttribute(newAttribute(value));
        }

        public boolean has(Node node) {
            Element elem = ImportParserUtils.failIfNotElement(node);
            return getAttribute(elem) != null;
        }

        public void set(Element elem, T onMissingValue, Function<T, T> onUpdate) {
            if (has(elem)) {
                set(elem, onUpdate.apply(get(elem)));
            } else {
                set(elem, onMissingValue);
            }
        }

        /**
         * similar to
         * {@link #set(nu.xom.Element, java.lang.Object, java.util.function.Function) }
         * but throws an exception if an existing value doesn't exist
         */
        public void set(Element elem, Function<T, T> onUpdate) {
            if (!has(elem)) {
                throw new NodeException("Missing attribute " + elem, elem);
            }
            set(elem, onUpdate.apply(get(elem)));
        }

        public Attribute detach(Element elem) {
            Attribute attribute = getAttribute(elem);
            if (attribute == null) {
                throw new NodeException("Cannot find attribute " + name, elem);
            }
            attribute.detach();
            return attribute;
        }

        private boolean isNotNamespaced() {
            return nsPrefix == null && nsUrl == null;
        }

        protected abstract String marshall(T input);

        protected abstract T unmarshall(String input);
    }

    public static class StringAttribute extends BaseAttribute<String> {
        public StringAttribute(String name) {
            super(name);
        }

        public StringAttribute(String name, String nsPrefix, String nsUrl) {
            super(name, nsPrefix, nsUrl);
        }

        @Override
        protected String marshall(String input) {
            return input;
        }

        @Override
        protected String unmarshall(String input) {
            return input;
        }
    }

    public static class IntAttribute extends BaseAttribute<Integer> {
        public IntAttribute(String name) {
            super(name);
        }

        public IntAttribute(String name, String nsPrefix, String nsUrl) {
            super(name, nsPrefix, nsUrl);
        }

        @Override
        protected String marshall(Integer input) {
            return input.toString();
        }

        @Override
        protected Integer unmarshall(String input) {
            return Integer.parseInt(input);
        }
    }

    public static class EnumAttribute<E extends Enum<E>> extends BaseAttribute<E> {
        private final Class<E> enumClazz;

        public EnumAttribute(String name, Class<E> enumClazz) {
            super(name);
            this.enumClazz = enumClazz;
        }

        @Override
        protected String marshall(E input) {
            return input.name();
        }

        @Override
        protected E unmarshall(String input) {
            return Enum.valueOf(enumClazz, input);
        }

        public void assertAndDetach(E expected, Element elem) {
            E actual = get(elem);
            if (actual != expected) {
                throw new RuntimeException("Expected " + expected + " got " + actual);
            }
            detach(elem);
        }
    }

    public static class EnumSetAttribute<E extends Enum<E>> extends BaseAttribute<EnumSet<E>> {
        private final Class<E> enumClazz;

        public EnumSetAttribute(String name, Class<E> enumClazz) {
            super(name);
            this.enumClazz = enumClazz;
        }

        @Override
        protected String marshall(EnumSet<E> input) {
            return input.stream().map(Enum::name).collect(Collectors.joining(" "));
        }

        @Override
        protected EnumSet<E> unmarshall(String input) {
            EnumSet<E> set = EnumSet.noneOf(enumClazz);
            for (String curEntry : StringUtils.split(input, ' ')) {
                set.add(Enum.valueOf(enumClazz, curEntry));
            }
            return set;
        }
    }

    public static class JsonAttribute<T> extends BaseAttribute<Object> {

        private final Object jsonClass;
        private Gson gson;

        public JsonAttribute(String name, Object jsonClass) {
            super(name);
            this.jsonClass = jsonClass;
        }

        public JsonAttribute(String name, Object jsonClass, Gson gson) {
            super(name);
            this.jsonClass = jsonClass;
            this.gson = gson;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected Object unmarshall(String input) {
            if (gson == null) {
                gson = new Gson();
            }
            return gson.fromJson(input, (Class<T>) jsonClass);
        }

        @Override
        protected String marshall(Object input) {
            if (gson == null) {
                gson = new Gson();
            }
            return gson.toJson(input);
        }
    }

    public static class XMLAttribute extends BaseAttribute<Node> {
        public XMLAttribute(String name) {
            super(name);
        }

        @Override
        protected String marshall(Node node) {
            return node.toXML();
        }

        @Override
        protected Node unmarshall(String input) {
            Builder parser = new Builder();
            try {
                Document doc = parser.build(input, null);
                return doc.getRootElement();
            } catch (ParsingException | IOException e) {
                log.warn("Problem when parsing XML", e);
                throw new RuntimeException("Error parsing XML Attribute " + input);
            }
        }
    }

    public static class StringArrayAttribute extends BaseAttribute<ArrayList<String>> {

        public StringArrayAttribute(String name) {
            super(name);
        }

        @Override
        protected ArrayList<String> unmarshall(String input) {
            try {
                ArrayList<String> array = new ArrayList<>();
                while (!input.isEmpty()) {
                    int metadataLength = 0;
                    char digitsCodePoint = input.charAt(metadataLength);
                    String digitsString = String.valueOf(digitsCodePoint);
                    int digitsInt = Integer.parseInt(digitsString);
                    metadataLength += digitsString.length();
                    int numChars = Integer.parseInt(input.substring(metadataLength, metadataLength + digitsInt));
                    metadataLength += String.valueOf(numChars).length();
                    array.add(input.substring(metadataLength, metadataLength + numChars));
                    input = input.substring(metadataLength + numChars);
                }
                return array;
            } catch (IndexOutOfBoundsException e) {
                log.error("String array attribute formatting issue, iob exception");
                return new ArrayList<>();
            } catch (NumberFormatException e) {
                log.error("String array attribute formatting issue, number format exception");
                return new ArrayList<>();
            }

        }

        @Override
        protected String marshall(ArrayList<String> input) {
            /*
             * for(string in array){ int numChars = string length; int digits =
             * number of digits it takes to represent numChars; make string
             * digits(must be 1-9)+numChars+string; add to string builder } will
             * not work for strings > 999,999,999 chars (don't do that)
             */
            StringBuilder s = new StringBuilder();
            for (String string : input) {
                int numChars = string.length();
                String sizeString = String.valueOf(numChars);
                int digitLength = sizeString.length();
                Matrix.matrixLog("StringBuilder digits {} length {} and string {}" + digitLength + sizeString + string);
                s.append(digitLength).append(sizeString).append(string);
            }
            return s.toString();
        }
    }

    public static class BooleanAttribute extends BaseAttribute<Boolean> {
        public BooleanAttribute(String name) {
            super(name);
        }

        @Override
        protected String marshall(Boolean input) {
            return input ? "true" : "false";
        }

        @Override
        protected Boolean unmarshall(String input) {
            return input.equals("true");
        }
    }

    private static abstract class JAXBSubTypeAdapter<S extends SubType> extends XmlAdapter<String, S> {
        private final CoreType coreType;

        public JAXBSubTypeAdapter(CoreType coreType) {
            if (coreType == null) {
                throw new NullPointerException("coreType");
            }
            this.coreType = coreType;
        }

        @Override
        public String marshal(S v) {
            return v.name;
        }

        @Override
        @SuppressWarnings("unchecked")
        public S unmarshal(String v) {
            for (SubType curSubType : coreType.subTypes) {
                if (v.equals(curSubType.name)) {
                    return (S) curSubType;
                }
            }
            throw new RuntimeException("Cannot find subtype " + v + " in " + coreType);
        }
    }
}
