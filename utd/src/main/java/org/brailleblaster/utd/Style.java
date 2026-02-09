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
package org.brailleblaster.utd;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;
import nu.xom.Node;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.brailleblaster.utd.config.StyleDefinitions;
import org.brailleblaster.utd.internal.StyleOptionsFactory;
import org.brailleblaster.utd.properties.Align;
import org.brailleblaster.utd.properties.NumberLinePosition;
import org.brailleblaster.utd.properties.PageNumberType;
import org.brailleblaster.utd.properties.PageSide;
import org.brailleblaster.utd.utils.UTDHelper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class to hold settings of a style.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
@XmlSeeAlso(StyleOptionsFactory.class)
public class Style implements IStyle, Serializable {
    public enum StyleOption {
        ALIGN(StyleOptionsFactory.ALIGN, Align.LEFT),
        BRAILLE_PAGE_NUMBER_FORMAT(StyleOptionsFactory.BRAILLE_PAGE_NUMBER_FORMAT, null),
        DONT_SPLIT(StyleOptionsFactory.DONT_SPLIT, false),
        END_SEPARATOR(StyleOptionsFactory.END_SEPARATOR, null),
        FIRST_LINE_INDENT(StyleOptionsFactory.FIRST_LINE_INDENT, null),
        FORMAT(StyleOptionsFactory.FORMAT, Format.NORMAL),
        GUIDE_WORDS(StyleOptionsFactory.GUIDE_WORDS, false),
        INDENT(StyleOptionsFactory.INDENT, null),
        LEFT_PADDING(StyleOptionsFactory.LEFT_PADDING, 0),
        LINES_AFTER(StyleOptionsFactory.LINES_AFTER, 0),
        LINES_BEFORE(StyleOptionsFactory.LINES_BEFORE, 0),
        LINE_LENGTH(StyleOptionsFactory.LINE_LENGTH, null),
        LINE_SPACING(StyleOptionsFactory.LINE_SPACING, 0),
        NEW_PAGES_AFTER(StyleOptionsFactory.NEW_PAGES_AFTER, 0),
        NEW_PAGES_BEFORE(StyleOptionsFactory.NEW_PAGES_BEFORE, 0),
        ORPHAN_CONTROL(StyleOptionsFactory.ORPHAN_CONTROL, 1),
        PAGE_SIDE(StyleOptionsFactory.PAGE_SIDE, PageSide.AUTO),
        RIGHT_PADDING(StyleOptionsFactory.RIGHT_PADDING, 0),
        SKIP_NUMBER_LINES(StyleOptionsFactory.SKIP_NUMBER_LINES, NumberLinePosition.NONE),
        SKIP_PAGES(StyleOptionsFactory.SKIP_PAGES, 0),
        START_SEPARATOR(StyleOptionsFactory.START_SEPARATOR, null),
        COLOR(StyleOptionsFactory.COLOR, null),
        KEEP_WITH_NEXT(StyleOptionsFactory.KEEP_WITH_NEXT, false),
        KEEP_WITH_PREVIOUS(StyleOptionsFactory.KEEP_WITH_PREVIOUS, false),
        LINE_NUMBER(StyleOptionsFactory.LINE_NUMBER, false),
        PAGE_NUM(StyleOptionsFactory.PAGE_NUM, false),
        VOLUME_END(StyleOptionsFactory.VOLUME_END, false),
        IS_TABLE(StyleOptionsFactory.IS_TABLE, false),
        IS_TABLE_CELL(StyleOptionsFactory.IS_TABLE_CELL, false),
        IS_TABLE_ROW(StyleOptionsFactory.IS_TABLE_ROW, false);


        private final Object defaultValue;
        private final String optionName;

        StyleOption(String optionName, Object defaultValue) {
            this.optionName = optionName;
            this.defaultValue = defaultValue;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public String getOptionName() {
            return optionName;
        }
    }

    @XmlTransient
    private @Nullable Style baseStyle;
    @XmlAttribute(name = "baseStyle")
    private @Nullable String baseStyleName;
    private static final Logger log = LoggerFactory.getLogger(Style.class);
    @XmlTransient
    private static final StyleOptionsFactory factory = new StyleOptionsFactory();
    @XmlElement
    private String id;
    @XmlID
    @XmlElement(required = true)
    private String name;
    @XmlElementRef(name = StyleOptionsFactory.LINES_BEFORE)
    private JAXBElement<Integer> linesBefore = null;
    @XmlElementRef(name = StyleOptionsFactory.LINES_AFTER)
    private JAXBElement<Integer> linesAfter = null;
    @XmlElementRef(name = StyleOptionsFactory.INDENT)
    private JAXBElement<Integer> indent = null;
    @XmlElementRef(name = StyleOptionsFactory.LINE_LENGTH)
    private JAXBElement<Integer> lineLength = null;
    @XmlElementRef(name = StyleOptionsFactory.FIRST_LINE_INDENT)
    private JAXBElement<Integer> firstLineIndent = null;
    @XmlElementRef(name = StyleOptionsFactory.SKIP_NUMBER_LINES)
    private JAXBElement<NumberLinePosition> skipNumberLines = null;
    @XmlElementRef(name = StyleOptionsFactory.SKIP_PAGES)
    private JAXBElement<Integer> skipPages = null;
    @XmlElementRef(name = StyleOptionsFactory.ALIGN)
    private JAXBElement<Align> align = null;
    @XmlElementRef(name = StyleOptionsFactory.FORMAT)
    private JAXBElement<Format> format = null;
    @XmlElementRef(name = StyleOptionsFactory.NEW_PAGES_BEFORE)
    private JAXBElement<Integer> newPagesBefore = null;
    @XmlElementRef(name = StyleOptionsFactory.NEW_PAGES_AFTER)
    private JAXBElement<Integer> newPagesAfter = null;
    @XmlElementRef(name = StyleOptionsFactory.PAGE_SIDE)
    private JAXBElement<PageSide> pageSide = null;
    @XmlElementRef(name = StyleOptionsFactory.BRAILLE_PAGE_NUMBER_FORMAT)
    private JAXBElement<PageNumberType> braillePageNumberFormat;
    @XmlElementRef(name = StyleOptionsFactory.DONT_SPLIT)
    private JAXBElement<Boolean> dontSplit = null;
    @XmlElementRef(name = StyleOptionsFactory.KEEP_WITH_NEXT)
    private JAXBElement<Boolean> keepWithNext = null;
    @XmlElementRef(name = StyleOptionsFactory.KEEP_WITH_PREVIOUS)
    private JAXBElement<Boolean> keepWithPrevious = null;
    @XmlElementRef(name = StyleOptionsFactory.ORPHAN_CONTROL)
    private JAXBElement<Integer> orphanControl = null;
    @XmlElementRef(name = StyleOptionsFactory.IS_TABLE)
    private JAXBElement<Boolean> isTable = null;
    @XmlElementRef(name = StyleOptionsFactory.IS_TABLE_ROW)
    private JAXBElement<Boolean> isTableRow = null;
    @XmlElementRef(name = StyleOptionsFactory.IS_TABLE_CELL)
    private JAXBElement<Boolean> isTableCell = null;
    @XmlElementRef(name = StyleOptionsFactory.LINE_SPACING)
    private JAXBElement<Integer> lineSpacing = null;
    @XmlElementRef(name = StyleOptionsFactory.GUIDE_WORDS)
    private JAXBElement<Boolean> guideWords = null;
    @XmlElementRef(name = StyleOptionsFactory.START_SEPARATOR)
    private JAXBElement<String> startSeparator = null;
    @XmlElementRef(name = StyleOptionsFactory.COLOR)
    private JAXBElement<String> color = null;
    @XmlElementRef(name = StyleOptionsFactory.END_SEPARATOR)
    private JAXBElement<String> endSeparator = null;
    @XmlElementRef(name = StyleOptionsFactory.PAGE_NUM)
    private JAXBElement<Boolean> pageNum = null;
    @XmlElementRef(name = StyleOptionsFactory.LINE_NUMBER)
    private JAXBElement<Boolean> lineNumber = null;
    @XmlElementRef(name = StyleOptionsFactory.VOLUME_END)
    private JAXBElement<Boolean> volumeEnd = null;

    /**
     * Copy given style settings with a new name
     */
    public Style(Style otherStyle, String newId, String newName) {
        this(otherStyle);
        this.id = newId;
        this.name = newName;
    }

    /**
     * Create style with the given defaults
     *
     * @see StyleDefinitions#addStyle(org.brailleblaster.utd.Style)
     */
    private Style(IStyle defaultStyle) {
        if (defaultStyle instanceof Style) {
            setBaseStyle((Style) defaultStyle);
        }
        this.id = defaultStyle.getId();
        this.name = defaultStyle.getName();
        this.linesBeforeWhen = defaultStyle.getLinesBeforeWhen();
        this.linesAfterWhen = defaultStyle.getLinesAfterWhen();
    }

    /**
     * Create style with the Java specified defaults
     *
     * @see StyleDefinitions#addStyle(org.brailleblaster.utd.Style)
     */
    public Style() {
        id = "DEFAULT";
        name = "DEFAULT";
        linesBeforeWhen = new ArrayList<>();
        linesAfterWhen = new ArrayList<>();
    }

    @Override
    public @NonNull Style copy(@NonNull String id, @NonNull String name) {
        Style copy = new Style();

        if (this.baseStyle != null)
            copy.setBaseStyle(this.getBaseStyle());

        if (this.align != null)
            copy.setAlign(this.getAlign());

        if (this.braillePageNumberFormat != null)
            copy.setBraillePageNumberFormat(this.getBraillePageNumberFormat());

        if (this.dontSplit != null)
            copy.setDontSplit(this.isDontSplit());

        if (this.endSeparator != null)
            copy.setEndSeparator(this.getEndSeparator());

        if (this.firstLineIndent != null)
            copy.setFirstLineIndent(this.getFirstLineIndent());

        if (this.format != null)
            copy.setFormat(this.getFormat());

        if (this.guideWords != null)
            copy.setGuideWords(this.isGuideWords());

        copy.setId(id);

        if (this.indent != null)
            copy.setIndent(this.getIndent());

        if (this.keepWithNext != null)
            copy.setKeepWithNext(this.isKeepWithNext());

        if (this.keepWithPrevious != null)
            copy.setKeepWithPrevious(this.isKeepWithPrevious());

        if (this.lineLength != null)
            copy.setLineLength(this.getLineLength());

        if (this.lineNumber != null)
            copy.setLineNumber(this.isLineNumber());

        if (this.linesAfter != null)
            copy.setLinesAfter(this.getLinesAfter());

        if (this.linesBefore != null)
            copy.setLinesBefore(this.getLinesBefore());

        if (this.lineSpacing != null)
            copy.setLineSpacing(this.getLineSpacing());

        if (this.linesBeforeWhen != null)
            copy.setLinesBeforeWhen(this.getAllLinesBeforeWhen());

        if (this.linesAfterWhen != null)
            copy.setLinesAfterWhen(this.getAllLinesAfterWhen());

        copy.setName(name);

        if (this.newPagesAfter != null)
            copy.setNewPagesAfter(this.getNewPagesAfter());

        if (this.newPagesBefore != null)
            copy.setNewPagesBefore(this.getNewPagesBefore());

        if (this.orphanControl != null)
            copy.setOrphanControl(this.getOrphanControl());

        if (this.pageNum != null)
            copy.setPageNum(this.isPageNum());

        if (this.pageSide != null)
            copy.setPageSide(this.getPageSide());

        if (this.skipNumberLines != null)
            copy.setSkipNumberLines(this.getSkipNumberLines());

        if (this.skipPages != null)
            copy.setSkipPages(this.getSkipPages());

        if (this.startSeparator != null)
            copy.setStartSeparator(this.getStartSeparator());

        if (this.color != null)
            copy.setColor(this.getColor());

        if (this.isTable != null)
            copy.setIsTable(this.isTable());

        if (this.isTableCell != null)
            copy.setIsTableCell(this.isTableCell());

        if (this.isTableRow != null)
            copy.setIsTableRow(this.isTableRow());

        if (this.volumeEnd != null)
            copy.setVolumeEnd(this.isVolumeEnd());

        return copy;
    }

    @Override
    public @NonNull String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        if (StringUtils.isBlank(id))
            throw new IllegalArgumentException("Style ID cannot be blank");
        this.id = id;
    }

    @Override
    public @NonNull String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        if (StringUtils.isBlank(name))
            throw new IllegalArgumentException("Name cannot be blank");
        this.name = name;
    }

    @Override
    public int getLinesBefore() {
        if (linesBefore != null) {
            return linesBefore.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getLinesBefore();
        }
        return (Integer) StyleOption.LINES_BEFORE.getDefaultValue();
    }

    public void setLinesBefore(int linesBefore) {
        validateLinesBefore(linesBefore);
        if (this.baseStyle == null && (int) StyleOption.LINES_BEFORE.getDefaultValue() == linesBefore) {
            this.linesBefore = null;
        } else if (this.linesBefore == null) {
            this.linesBefore = factory.createLinesBefore(linesBefore);
        } else {
            this.linesBefore.setValue(linesBefore);
        }
    }

    private void validateLinesBefore(int linesBefore) {
        if (linesBefore < 0)
            throw new IllegalArgumentException("linesBefore must be positive, given " + linesBefore);
    }

    @Override
    public int getLinesAfter() {
        if (linesAfter != null) {
            return linesAfter.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getLinesAfter();
        }
        return (Integer) StyleOption.LINES_AFTER.getDefaultValue();
    }

    public void setLinesAfter(int linesAfter) {
        validateLinesAfter(linesAfter);
        if (this.baseStyle == null && (int) StyleOption.LINES_AFTER.getDefaultValue() == linesAfter) {
            this.linesAfter = null;
        } else if (this.linesAfter == null) {
            this.linesAfter = factory.createLinesAfter(linesAfter);
        } else {
            this.linesAfter.setValue(linesAfter);
        }
    }

    private void validateLinesAfter(int linesAfter) {
        if (linesAfter < 0)
            throw new IllegalArgumentException("linesAfter must be positive, given " + linesAfter);
    }

    public void validateSkipPages(int skipPages) {
        if (skipPages < 0)
            throw new IllegalArgumentException("skipPages must be positive, given " + skipPages);
    }

    @Override
    public Integer getIndent() {
        if (indent != null) {
            return indent.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getIndent();
        }
        return (Integer) StyleOption.INDENT.getDefaultValue();
    }

    public void setIndent(Integer indent) {
        validateIndent(indent);
        if (this.baseStyle == null && Objects.equals(StyleOption.INDENT.getDefaultValue(), indent)) {
            this.indent = null;
        } else if (this.indent == null) {
            this.indent = factory.createIndent(indent);
        } else {
            this.indent.setValue(indent);
        }
    }

    private void validateIndent(Integer indent) {
        if (indent != null && indent < 0)
            throw new IllegalArgumentException("indent must be positive, given " + indent);
    }

    @Override
    public Integer getLineLength() {
        if (lineLength != null) {
            return lineLength.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getLineLength();
        }
        return (Integer) StyleOption.LINE_LENGTH.getDefaultValue();
    }

    public void setLineLength(Integer lineLength) {
        if (this.baseStyle == null && Objects.equals(StyleOption.LINE_LENGTH.getDefaultValue(), lineLength)) {
            this.lineLength = null;
        } else if (this.lineLength == null) {
            this.lineLength = factory.createLineLength(lineLength);
        } else {
            this.lineLength.setValue(lineLength);
        }
    }

    @Override
    public Integer getFirstLineIndent() {
        if (firstLineIndent != null) {
            return firstLineIndent.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getFirstLineIndent();
        }
        return (Integer) StyleOption.FIRST_LINE_INDENT.getDefaultValue();
    }

    public void setFirstLineIndent(Integer firstLineIndent) {
        validateFirstLineIndent(firstLineIndent);
        if (this.baseStyle == null && Objects.equals(StyleOption.FIRST_LINE_INDENT.getDefaultValue(), firstLineIndent)) {
            this.firstLineIndent = null;
        } else if (this.firstLineIndent == null) {
            this.firstLineIndent = factory.createFirstLineIndent(firstLineIndent);
        } else {
            this.firstLineIndent.setValue(firstLineIndent);
        }
    }

    private void validateFirstLineIndent(Integer firstLineIndent) {
        if (firstLineIndent != null && firstLineIndent < 0)
            throw new IllegalArgumentException("firstLineIndent must be positive, given " + firstLineIndent);
    }

    @Override
    public NumberLinePosition getSkipNumberLines() {
        if (skipNumberLines != null) {
            return skipNumberLines.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getSkipNumberLines();
        }
        return (NumberLinePosition) StyleOption.SKIP_NUMBER_LINES.getDefaultValue();
    }

    public void setSkipNumberLines(NumberLinePosition skipNumberLines) {
        if (this.baseStyle == null && Objects.equals(StyleOption.SKIP_NUMBER_LINES.getDefaultValue(), skipNumberLines)) {
            this.skipNumberLines = null;
        } else if (this.skipNumberLines == null) {
            this.skipNumberLines = factory.createSkipPageNumberLines(skipNumberLines);
        } else {
            this.skipNumberLines.setValue(skipNumberLines);
        }
    }

    @Override
    public int getSkipPages() {
        if (skipPages != null) {
            return skipPages.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getSkipPages();
        }
        return (Integer) StyleOption.SKIP_PAGES.getDefaultValue();
    }

    public void setSkipPages(int skipPages) {
        validateSkipPages(skipPages);
        if (this.baseStyle == null && (int) StyleOption.SKIP_PAGES.getDefaultValue() == skipPages) {
            this.skipPages = null;
        } else if (this.skipPages == null) {
            this.skipPages = factory.createSkipPages(skipPages);
        } else {
            this.skipPages.setValue(skipPages);
        }
    }

    @Override
    public Align getAlign() {
        if (align != null) {
            return align.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getAlign();
        }
        return (Align) StyleOption.ALIGN.getDefaultValue();
    }

    public void setAlign(Align align) {
        Align validAlign = validateAlign(align);
        if (this.baseStyle == null && Objects.equals(StyleOption.ALIGN.getDefaultValue(), validAlign)) {
            this.align = null;
        } else if (this.align == null) {
            this.align = factory.createAlign(validAlign);
        } else {
            this.align.setValue(validAlign);
        }
    }

    private @NonNull Align validateAlign(Align align) {
        return Objects.requireNonNull(align, "align");
    }

    @Override
    public Format getFormat() {
        if (format != null) {
            return format.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getFormat();
        }
        return (Format) StyleOption.FORMAT.getDefaultValue();
    }

    public void setFormat(Format format) {
        validateFormat(format);
        if (this.baseStyle == null && Objects.equals(StyleOption.FORMAT.getDefaultValue(), format)) {
            this.format = null;
        } else if (this.format == null) {
            this.format = factory.createFormat(format);
        } else {
            this.format.setValue(format);
        }
    }

    private void validateFormat(Format format) {
        if (format == null)
            throw new NullPointerException("format");
    }

    public int getNewPagesBefore() {
        if (newPagesBefore != null) {
            return newPagesBefore.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getNewPagesBefore();
        }
        return (Integer) StyleOption.NEW_PAGES_BEFORE.getDefaultValue();
    }

    public void setNewPagesBefore(int newPageBefore) {
        if (this.baseStyle == null && (int) StyleOption.NEW_PAGES_BEFORE.getDefaultValue() == newPageBefore) {
            this.newPagesBefore = null;
        } else if (this.newPagesBefore == null) {
            this.newPagesBefore = factory.createNewPagesBefore(newPageBefore);
        } else {
            this.newPagesBefore.setValue(newPageBefore);
        }
    }

    @Override
    public int getNewPagesAfter() {
        if (newPagesAfter != null) {
            return newPagesAfter.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getNewPagesAfter();
        }
        return (Integer) StyleOption.NEW_PAGES_AFTER.getDefaultValue();
    }

    public void setNewPagesAfter(int newPageAfter) {
        if (this.baseStyle == null && (int) StyleOption.NEW_PAGES_AFTER.getDefaultValue() == newPageAfter) {
            this.newPagesAfter = null;
        } else if (this.newPagesAfter == null) {
            this.newPagesAfter = factory.createNewPagesAfter(newPageAfter);
        } else {
            this.newPagesAfter.setValue(newPageAfter);
        }
    }

    @Override
    public PageSide getPageSide() {
        if (pageSide != null) {
            return pageSide.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getPageSide();
        }
        return (PageSide) StyleOption.PAGE_SIDE.getDefaultValue();
    }

    public void setPageSide(PageSide pageSide) {
        PageSide validPageSide = validatePageSide(pageSide);
        if (baseStyle == null && Objects.equals(StyleOption.PAGE_SIDE.getDefaultValue(), validPageSide)) {
            this.pageSide = null;
        } else if (this.pageSide == null) {
            this.pageSide = factory.createPageSide(validPageSide);
        } else {
            this.pageSide.setValue(validPageSide);
        }
    }

    private @NonNull PageSide validatePageSide(PageSide pageSide) {
        return Objects.requireNonNull(pageSide);
    }

    @Override
    public PageNumberType getBraillePageNumberFormat() {
        if (braillePageNumberFormat != null) {
            return braillePageNumberFormat.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getBraillePageNumberFormat();
        }
        return (PageNumberType) StyleOption.BRAILLE_PAGE_NUMBER_FORMAT.getDefaultValue();
    }

    public void setBraillePageNumberFormat(PageNumberType braillePageNumberFormat) {
        if (this.baseStyle == null && Objects.equals(StyleOption.BRAILLE_PAGE_NUMBER_FORMAT.getDefaultValue(), braillePageNumberFormat)) {
            this.braillePageNumberFormat = null;
        } else if (this.braillePageNumberFormat != null) {
            this.braillePageNumberFormat.setValue(braillePageNumberFormat);
        } else {
            this.braillePageNumberFormat = factory.createBraillePageNumberFormat(braillePageNumberFormat);
        }
    }

    @Override
    public boolean isDontSplit() {
        if (dontSplit != null) {
            return dontSplit.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.isDontSplit();
        }
        return (Boolean) StyleOption.DONT_SPLIT.getDefaultValue();
    }

    public void setDontSplit(boolean dontSplit) {
        if (this.baseStyle == null && (boolean) StyleOption.DONT_SPLIT.getDefaultValue() == dontSplit) {
            this.dontSplit = null;
        } else if (this.dontSplit == null) {
            this.dontSplit = factory.createDontSplit(dontSplit);
        } else {
            this.dontSplit.setValue(dontSplit);
        }
    }

    @Override
    public boolean isKeepWithNext() {
        if (keepWithNext != null) {
            return keepWithNext.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.isKeepWithNext();
        }
        return (Boolean) StyleOption.KEEP_WITH_NEXT.getDefaultValue();
    }

    public void setKeepWithNext(boolean keepWithNext) {
        if (this.baseStyle == null && (boolean) StyleOption.KEEP_WITH_NEXT.getDefaultValue() == keepWithNext) {
            this.keepWithNext = null;
        } else if (this.keepWithNext == null) {
            this.keepWithNext = factory.createKeepWithNext(keepWithNext);
        } else {
            this.keepWithNext.setValue(keepWithNext);
        }
    }

    @Override
    public boolean isKeepWithPrevious() {
        if (keepWithPrevious != null) {
            return keepWithPrevious.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.isKeepWithPrevious();
        }
        return (Boolean) StyleOption.KEEP_WITH_PREVIOUS.getDefaultValue();
    }

    public void setKeepWithPrevious(boolean keepWithPrevious) {
        if (this.baseStyle == null && (boolean) StyleOption.KEEP_WITH_PREVIOUS.getDefaultValue() == keepWithPrevious) {
            this.keepWithPrevious = null;
        } else if (this.keepWithPrevious == null) {
            this.keepWithPrevious = factory.createKeepWithPrevious(keepWithPrevious);
        } else {
            this.keepWithPrevious.setValue(keepWithPrevious);
        }
    }

    @Override
    public int getOrphanControl() {
        if (orphanControl != null) {
            return orphanControl.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getOrphanControl();
        }
        return (Integer) StyleOption.ORPHAN_CONTROL.getDefaultValue();
    }

    public void setOrphanControl(int orphanControl) {
        validateOrphanControl(orphanControl);
        if (this.baseStyle == null && (int) StyleOption.ORPHAN_CONTROL.getDefaultValue() == orphanControl) {
            this.orphanControl = null;
        } else if (this.orphanControl == null) {
            this.orphanControl = factory.createOrphanControl(orphanControl);
        } else {
            this.orphanControl.setValue(orphanControl);
        }
    }

    private void validateOrphanControl(int orphanControl) {
        if (orphanControl < 1)
            throw new IllegalArgumentException(
                    "orphanControl must be positive, > 2 to take effect, given " + orphanControl);
    }

    @Override
    public int getLineSpacing() {
        if (lineSpacing != null) {
            return lineSpacing.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getLineSpacing();
        }
        return (Integer) StyleOption.LINE_SPACING.getDefaultValue();
    }

    public void setLineSpacing(int lineSpacing) {
        validateLineSpacing(lineSpacing);
        if (this.baseStyle == null && (int) StyleOption.LINE_SPACING.getDefaultValue() == lineSpacing) {
            this.lineSpacing = null;
        } else if (this.lineSpacing == null) {
            this.lineSpacing = factory.createLineSpacing(lineSpacing);
        } else {
            this.lineSpacing.setValue(lineSpacing);
        }
    }

    private void validateLineSpacing(int lineSpacing) {
        if (lineSpacing < 0)
            throw new IllegalArgumentException("lineSpacing must be positive, given " + lineSpacing);
    }

    @Override
    public String getStartSeparator() {
        if (startSeparator != null) {
            return startSeparator.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getStartSeparator();
        }
        return (String) StyleOption.START_SEPARATOR.getDefaultValue();
    }

    public void setStartSeparator(String startSeparator) {
        if (this.baseStyle == null && Objects.equals(StyleOption.START_SEPARATOR.getDefaultValue(), startSeparator)) {
            this.startSeparator = null;
        } else if (this.startSeparator == null) {
            this.startSeparator = factory.createStartSeparator(startSeparator);
        } else {
            this.startSeparator.setValue(startSeparator);
        }
    }

    @Override
    public String getColor() {
        if (color != null) {
            return color.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getColor();
        }
        return (String) StyleOption.COLOR.getDefaultValue();
    }

    public void setColor(String color) {
        if (this.baseStyle == null && Objects.equals(StyleOption.COLOR.getDefaultValue(), color)) {
            this.color = null;
        } else if (this.color == null) {
            this.color = factory.createColor(color);
        } else {
            this.color.setValue(color);
        }
    }

    @Override
    public String getEndSeparator() {
        if (endSeparator != null) {
            return endSeparator.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getEndSeparator();
        }
        return (String) StyleOption.END_SEPARATOR.getDefaultValue();
    }

    public void setEndSeparator(String endSeparator) {
        if (this.baseStyle == null && Objects.equals(StyleOption.END_SEPARATOR.getDefaultValue(), endSeparator)) {
            this.endSeparator = null;
        } else if (this.endSeparator == null) {
            this.endSeparator = factory.createEndSeparator(endSeparator);
        } else {
            this.endSeparator.setValue(endSeparator);
        }
    }

    @Override
    public int hashCode() {
        // Reflection hashCode does not work due to the potential for different
        // sources of property values, so explicitly provide the values to be
        // used in calculation.
        return new HashCodeBuilder(19, 37).append(getName()).append(getId()).append(getAlign())
                .append(getBraillePageNumberFormat()).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (other.getClass() != getClass()) {
            return false;
        }
        Style rhs = (Style) other;
        return new EqualsBuilder().append(getId(), rhs.getId()).append(getName(), rhs.getName())
                .append(getAlign(), rhs.getAlign())
                .append(getBraillePageNumberFormat(), rhs.getBraillePageNumberFormat())
                .append(getFormat(), rhs.getFormat()).append(getIndent(), rhs.getIndent())
                .append(getFirstLineIndent(), rhs.getFirstLineIndent()).append(getLineLength(), rhs.getLineLength())
                .append(getOrphanControl(), rhs.getOrphanControl()).append(isDontSplit(), rhs.isDontSplit())
                .append(isKeepWithNext(), rhs.isKeepWithNext()).append(getLinesAfter(), rhs.getLinesAfter())
                .append(getLinesBefore(), rhs.getLinesBefore()).append(getLineSpacing(), rhs.getLineSpacing())
                .append(getNewPagesAfter(), rhs.getNewPagesAfter()).append(getNewPagesBefore(), rhs.getNewPagesBefore())
                .append(getSkipPages(), rhs.getSkipPages()).append(getEndSeparator(), rhs.getEndSeparator())
                .append(getStartSeparator(), rhs.getStartSeparator()).append(getPageSide(), rhs.getPageSide())
                .append(getSkipNumberLines(), rhs.getSkipNumberLines())
                .append(isGuideWords(), rhs.isGuideWords()).append(isKeepWithPrevious(), rhs.isKeepWithPrevious())
                .append(isLineNumber(), rhs.isLineNumber()).append(isPageNum(), rhs.isPageNum())
                .append(isTable(), rhs.isTable()).append(isTableCell(), rhs.isTableCell())
                .append(isTableRow(), rhs.isTableRow()).append(isVolumeEnd(), rhs.isVolumeEnd()).isEquals();
    }

    @Override
    public String toString() {
        return UTDHelper.autoToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public void setGuideWords(boolean guideWords) {
        validateGuideWords(guideWords);
        if (this.baseStyle == null && (boolean) StyleOption.GUIDE_WORDS.getDefaultValue() == guideWords) {
            this.guideWords = null;
        } else if (this.guideWords == null) {
            this.guideWords = factory.createGuideWords(guideWords);
        } else {
            this.guideWords.setValue(guideWords);
        }
    }

    private void validateGuideWords(boolean guideWords) {
        if (guideWords) {
            this.setSkipNumberLines(NumberLinePosition.BOTTOM);
        }
    }

    @Override
    public boolean isGuideWords() {
        if (guideWords != null) {
            return guideWords.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.isGuideWords();
        }
        return (Boolean) StyleOption.GUIDE_WORDS.getDefaultValue();
    }

    @Override
    public boolean isTable() {
        if (isTable != null) {
            return isTable.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.isTable();
        }
        return (Boolean) StyleOption.IS_TABLE.getDefaultValue();
    }

    public void setIsTable(boolean isTable) {
        if (this.baseStyle == null && (boolean) StyleOption.IS_TABLE.getDefaultValue() == isTable) {
            this.isTable = null;
        } else if (this.isTable == null) {
            this.isTable = factory.createIsTable(isTable);
        } else {
            this.isTable.setValue(isTable);
        }
    }

    @Override
    public boolean isTableRow() {
        if (isTableRow != null) {
            return isTableRow.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.isTableRow();
        }
        return (Boolean) StyleOption.IS_TABLE_ROW.getDefaultValue();
    }

    public void setIsTableRow(boolean isTableRow) {
        if (this.baseStyle == null && (boolean) StyleOption.IS_TABLE_ROW.getDefaultValue() == isTableRow) {
            this.isTableRow = null;
        } else if (this.isTableRow == null) {
            this.isTableRow = factory.createIsTableRow(isTableRow);
        } else {
            this.isTableRow.setValue(isTableRow);
        }
    }

    @Override
    public boolean isTableCell() {
        if (isTableCell != null) {
            return isTableCell.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.isTableCell();
        }
        return (Boolean) StyleOption.IS_TABLE_CELL.getDefaultValue();
    }

    public void setIsTableCell(boolean isTableCell) {
        if (this.baseStyle == null && (boolean) StyleOption.IS_TABLE_CELL.getDefaultValue() == isTableCell) {
            this.isTableCell = null;
        } else if (this.isTableCell == null) {
            this.isTableCell = factory.createIsTableCell(isTableCell);
        } else {
            this.isTableCell.setValue(isTableCell);
        }
    }

    @Override
    public boolean isPageNum() {
        if (pageNum != null) {
            return pageNum.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.isPageNum();
        }
        return (Boolean) StyleOption.PAGE_NUM.getDefaultValue();
    }

    public void setPageNum(boolean pageNum) {
        if (this.baseStyle == null && (boolean) StyleOption.PAGE_NUM.getDefaultValue() == pageNum) {
            this.pageNum = null;
        } else if (this.pageNum == null) {
            this.pageNum = factory.createPageNum(pageNum);
        } else {
            this.pageNum.setValue(pageNum);
        }
    }

    @Override
    public boolean isLineNumber() {
        if (lineNumber != null) {
            return lineNumber.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.isLineNumber();
        }
        return (Boolean) StyleOption.LINE_NUMBER.getDefaultValue();
    }

    public void setLineNumber(boolean lineNumber) {
        if (this.baseStyle == null && (boolean) StyleOption.LINE_NUMBER.getDefaultValue() == lineNumber) {
            this.lineNumber = null;
        } else if (this.lineNumber == null) {
            this.lineNumber = factory.createLineNumber(lineNumber);
        } else {
            this.lineNumber.setValue(lineNumber);
        }
    }

    @Override
    public boolean isVolumeEnd() {
        if (volumeEnd != null) {
            return volumeEnd.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.isVolumeEnd();
        }
        return (Boolean) StyleOption.VOLUME_END.getDefaultValue();
    }

    public void setVolumeEnd(boolean volumeEnd) {
        if (this.baseStyle == null && (boolean) StyleOption.VOLUME_END.getDefaultValue() == volumeEnd) {
            this.volumeEnd = null;
        } else if (this.volumeEnd == null) {
            this.volumeEnd = factory.createVolumeEned(volumeEnd);
        } else {
            this.volumeEnd.setValue(volumeEnd);
        }
    }

    private Integer getConditionalValueFromList(List<ConditionalValue<Integer>> conditionalValues, IConditionalValueGetter<IStyle, Integer> getterFunction, Node node,
                                                NamespaceMap namespaces, Integer defaultValue) {
        log.debug("Checking {} exceptions in style {}", conditionalValues.size(), this.getName());
        for (ConditionalValue<Integer> conditional : conditionalValues) {
            if (conditional.checkCondition(node, namespaces)) {
                return conditional.getValue();
            }
        }
        if (baseStyle != null) {
            return getterFunction.get(baseStyle, node, namespaces);
        }
        return defaultValue;
    }

    private List<ConditionalValue<Integer>> linesBeforeWhen;

    @Override
    public List<ConditionalValue<Integer>> getLinesBeforeWhen() {
        return linesBeforeWhen;
    }

    public void setLinesBeforeWhen(List<ConditionalValue<Integer>> restrictions) {
        this.linesBeforeWhen = restrictions;
    }

    @Override
    public Integer getLinesBefore(Node node, NamespaceMap namespaces) {
        return getConditionalValueFromList(linesBeforeWhen, IStyle::getLinesBefore, node, namespaces, null);
    }

    private List<ConditionalValue<Integer>> linesAfterWhen;

    @Override
    public List<ConditionalValue<Integer>> getLinesAfterWhen() {
        return linesAfterWhen;
    }

    public void setLinesAfterWhen(List<ConditionalValue<Integer>> restrictions) {
        this.linesAfterWhen = restrictions;
    }

    @Override
    public Integer getLinesAfter(Node node, NamespaceMap namespaces) {
        return getConditionalValueFromList(linesAfterWhen, IStyle::getLinesAfter, node, namespaces, null);
    }

    public @Nullable Style getBaseStyle() {
        return baseStyle;
    }

    public void setBaseStyle(@Nullable Style baseStyle) {
        this.baseStyle = baseStyle;
        this.baseStyleName = baseStyle != null ? baseStyle.getName() : null;
    }

    public @Nullable String getBaseStyleName() {
        return baseStyleName;
    }

    /**
     * WARNING: You most likely want {@link #setBaseStyle(org.brailleblaster.utd.Style) },
     * this is only public for the JAXB unmarshaller/marshaller
     */
    public void setBaseStyleName(@Nullable String baseStyleName) {
        this.baseStyleName = baseStyleName;
    }

    // Used by JAXB
    @SuppressWarnings("unused")
    void afterUnmarshal(Unmarshaller ignoredU, Object parent) {
        validateAlign(getAlign());
        getBraillePageNumberFormat();
        validateFirstLineIndent(getFirstLineIndent());
        validateFormat(getFormat());
        validateGuideWords(isGuideWords());
        validateIndent(getIndent());
        validateLinesBefore(getLinesBefore());
        validateLinesAfter(getLinesAfter());
        validateLineSpacing(getLineSpacing());
        validateOrphanControl(getOrphanControl());
        validatePageSide(getPageSide());
        validateSkipPages(getSkipPages());
    }

    @Override
    public List<ConditionalValue<Integer>> getAllLinesBeforeWhen() {
        List<ConditionalValue<Integer>> result = new ArrayList<>(this.getLinesBeforeWhen());
        if (baseStyle != null) {
            result.addAll(baseStyle.getAllLinesBeforeWhen());
        }
        return result;
    }

    @Override
    public List<ConditionalValue<Integer>> getAllLinesAfterWhen() {
        List<ConditionalValue<Integer>> result = new ArrayList<>(this.getLinesAfterWhen());
        if (baseStyle != null) {
            result.addAll(baseStyle.getAllLinesAfterWhen());
        }
        return result;
    }

    @XmlElementRef(name = StyleOptionsFactory.LEFT_PADDING)
    private JAXBElement<Integer> leftPadding = null;

    @Override
    public int getLeftPadding() {
        if (leftPadding != null) {
            return leftPadding.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getLeftPadding();
        }
        return (Integer) StyleOption.LEFT_PADDING.getDefaultValue();
    }

    public void setLeftPadding(int leftPadding) {
        if (baseStyle == null && Objects.equals(StyleOption.LEFT_PADDING.getDefaultValue(), leftPadding)) {
            this.leftPadding = null;
        } else if (this.leftPadding == null) {
            this.leftPadding = factory.createLeftPadding(leftPadding);
        } else {
            this.leftPadding.setValue(leftPadding);
        }
    }

    @XmlElementRef(name = StyleOptionsFactory.RIGHT_PADDING)
    private JAXBElement<Integer> rightPadding;

    @Override
    public int getRightPadding() {
        if (rightPadding != null) {
            return rightPadding.getValue();
        }
        if (baseStyle != null) {
            return baseStyle.getRightPadding();
        }
        return (Integer) StyleOption.RIGHT_PADDING.getDefaultValue();
    }

    public void setRightPadding(int rightPadding) {
        if (baseStyle == null && Objects.equals(StyleOption.RIGHT_PADDING.getDefaultValue(), rightPadding)) {
            this.rightPadding = null;
        } else if (this.rightPadding == null) {
            this.rightPadding = factory.createRightPadding(rightPadding);
        } else {
            this.rightPadding.setValue(rightPadding);
        }
    }
}
