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

import kotlin.NotImplementedError;
import nu.xom.Node;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.brailleblaster.utd.properties.Align;
import org.brailleblaster.utd.properties.NumberLinePosition;
import org.brailleblaster.utd.properties.PageNumberType;
import org.brailleblaster.utd.properties.PageSide;
import org.jspecify.annotations.NonNull;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;

public class StyleStack extends ArrayDeque<IStyle> implements IStyle {
    private final Style defaultStyle;
    private String name;

    public StyleStack(String name) {
        this();
        this.name = name;
    }

    public StyleStack() {
        super();
        defaultStyle = new Style();
        defaultStyle.setIndent(0);
        defaultStyle.setLineLength(0);
        defaultStyle.setFirstLineIndent(0);
        defaultStyle.setAlign(Align.LEFT);
    }

    @Override
    public @NonNull String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private IStyle getInnerMostStyle() {
        IStyle style = this.peek();
        if (style == null) {
            style = defaultStyle;
        }
        return style;
    }

    @Override
    public @NonNull String getId() {
        return getInnerMostStyle().getId();
    }

    @Override
    public int getLinesBefore() {
        return getInnerMostStyle().getLinesBefore();
    }

    @Override
    public int getLinesAfter() {
        return getInnerMostStyle().getLinesAfter();
    }

    @Override
    public Integer getIndent() {
        Integer value = null;
        Iterator<IStyle> iter = this.iterator();
        while (iter.hasNext() && value == null) {
            value = iter.next().getIndent();
        }
        if (value == null) {
            value = defaultStyle.getIndent();
        }
        return value;
    }

    @Override
    public Integer getLineLength() {
        Integer value = null;
        Iterator<IStyle> iter = this.iterator();
        while (iter.hasNext() && value == null) {
            value = iter.next().getLineLength();
        }
        if (value == null) {
            value = defaultStyle.getLineLength();
        }
        return value;
    }

    @Override
    public Integer getFirstLineIndent() {
        Align align = getAlign();
        if (align != Align.LEFT && align != null) {
            // First line indent does not apply when not left aligned.
            return getIndent();
        }
        Integer value = null;
        Iterator<IStyle> iter = this.iterator();
        while (iter.hasNext() && value == null) {
            value = iter.next().getFirstLineIndent();
        }
        if (value == null) {
            value = getIndent();
        }
        return value;
    }

    @Override
    public int getNewPagesBefore() {
        return getInnerMostStyle().getNewPagesBefore();
    }

    @Override
    public int getNewPagesAfter() {
        return getInnerMostStyle().getNewPagesAfter();
    }

    @Override
    public boolean isDontSplit() {
        return getInnerMostStyle().isDontSplit();
    }

    @Override
    public boolean isKeepWithNext() {
        return getInnerMostStyle().isKeepWithNext();
    }

    @Override
    public boolean isKeepWithPrevious() {
        return getInnerMostStyle().isKeepWithPrevious();
    }

    @Override
    public int getOrphanControl() {
        return getInnerMostStyle().getOrphanControl();
    }

    @Override
    public NumberLinePosition getSkipNumberLines() {
        NumberLinePosition pos = NumberLinePosition.NONE;
        Iterator<IStyle> iter = this.iterator();
        while (iter.hasNext() && pos == NumberLinePosition.NONE) {
            pos = iter.next().getSkipNumberLines();
        }
        if (pos == null) {
            pos = defaultStyle.getSkipNumberLines();
        }
        return pos;
    }

    @Override
    public Style.Format getFormat() {
        return getInnerMostStyle().getFormat();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = result + ((defaultStyle == null) ? 0 : defaultStyle.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StyleStack input)) {
            return false;
        }
        Iterator<IStyle> iter = input.iterator();

        if (this.size() != input.size()) {
            return false;
        }
        if (this == input) {
            return true;
        } else {
            for (IStyle style : this) {
                if (!style.equals(iter.next())) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int getSkipPages() {
        return getInnerMostStyle().getSkipPages();
    }

    @Override
    public Align getAlign() {
        Align value = Align.LEFT;
        Iterator<IStyle> iter = this.iterator();
        while (iter.hasNext() && value == Align.LEFT) {
            value = iter.next().getAlign();
        }
        return value;
    }

    @Override
    public PageSide getPageSide() {
        return getInnerMostStyle().getPageSide();
    }

    @Override
    public PageNumberType getBraillePageNumberFormat() {
        PageNumberType type = PageNumberType.NORMAL;
        Iterator<IStyle> iter = this.iterator();
        while (iter.hasNext() && type == PageNumberType.NORMAL) {
            type = iter.next().getBraillePageNumberFormat();
        }
        if (type == null) {
            type = defaultStyle.getBraillePageNumberFormat();
        }
        return type;
    }

    @Override
    public int getLineSpacing() {
        // lineSpacing with value of 0 means check parent element's style
        // So we work from inner most towards the root until we have a non-zero definition
        int result = 0;
        Iterator<IStyle> it = this.iterator();
        while (result == 0 && it.hasNext()) {
            IStyle style = it.next();
            result = style.getLineSpacing();
        }
        return result == 0 ? 1 : result;
    }

    @Override
    public boolean isTable() {
        return getInnerMostStyle().isTable();
    }

    @Override
    public boolean isTableRow() {
        return getInnerMostStyle().isTableRow();
    }

    @Override
    public boolean isTableCell() {
        return getInnerMostStyle().isTable();
    }

    @Override
    public String getEndSeparator() {
        return getInnerMostStyle().getEndSeparator();
    }

    @Override
    public String getStartSeparator() {
        return getInnerMostStyle().getStartSeparator();
    }

    @Override
    public String getColor() {
        return getInnerMostStyle().getColor();
    }

    @Override
    public boolean isGuideWords() {
        for (IStyle style : this) {
            if (style.isGuideWords()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPageNum() {
        return getInnerMostStyle().isPageNum();
    }

    @Override
    public boolean isLineNumber() {
        return getInnerMostStyle().isLineNumber();
    }

    @Override
    public boolean isVolumeEnd() {
        return getInnerMostStyle().isVolumeEnd();
    }

    @Override
    public List<ConditionalValue<Integer>> getLinesBeforeWhen() {
        if (!this.isEmpty()) {
            return this.getFirst().getLinesBeforeWhen();
        }
        return defaultStyle.getLinesBeforeWhen();
    }

    @Override
    public List<ConditionalValue<Integer>> getLinesAfterWhen() {
        if (!this.isEmpty()) {
            return this.getFirst().getLinesAfterWhen();
        }
        return defaultStyle.getLinesAfterWhen();
    }

    @Override
    public Integer getLinesAfter(Node node, NamespaceMap namespaces) {
        if (!this.isEmpty()) {
            return this.getFirst().getLinesAfter(node, namespaces);
        }
        return defaultStyle.getLinesAfter(node, namespaces);
    }

    @Override
    public Integer getLinesBefore(Node node, NamespaceMap namespaces) {
        if (!this.isEmpty()) {
            return this.getFirst().getLinesBefore(node, namespaces);
        }
        return defaultStyle.getLinesBefore(node, namespaces);
    }

    @Override
    public @NonNull IStyle copy(@NonNull String id, @NonNull String name) {
        throw new NotImplementedError("StyleStacks currently cannot be copied.");
    }

    @Override
    public List<ConditionalValue<Integer>> getAllLinesBeforeWhen() {
        if (!this.isEmpty()) {
            return this.getFirst().getAllLinesBeforeWhen();
        }
        return defaultStyle.getAllLinesBeforeWhen();
    }

    @Override
    public List<ConditionalValue<Integer>> getAllLinesAfterWhen() {
        if (!this.isEmpty()) {
            return this.getFirst().getAllLinesAfterWhen();
        }
        return defaultStyle.getAllLinesAfterWhen();
    }

    @Override
    public int getLeftPadding() {
        if (!this.isEmpty()) {
            return this.getFirst().getLeftPadding();
        }
        return defaultStyle.getLeftPadding();
    }

    @Override
    public int getRightPadding() {
        if (!this.isEmpty()) {
            return this.getFirst().getRightPadding();
        }
        return defaultStyle.getRightPadding();
    }
}
