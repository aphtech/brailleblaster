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

import org.brailleblaster.utd.tables.AutoTableFormatter;
import org.brailleblaster.utd.tables.FacingTableFormatter;
import org.brailleblaster.utd.tables.LinearTableFormatter;
import org.brailleblaster.utd.tables.ListedTableFormatter;
import org.brailleblaster.utd.tables.SimpleTableFormatter;
import org.brailleblaster.utd.tables.StairstepTableFormatter;
import org.brailleblaster.utd.toc.TOCFormatter;

import nu.xom.Node;

import java.util.List;

import org.brailleblaster.utd.formatters.Formatter;
import org.brailleblaster.utd.formatters.LiteraryFormatter;
import org.brailleblaster.utd.formatters.MathFormatter;
import org.brailleblaster.utd.formatters.NumberedLineFormatter;
import org.brailleblaster.utd.formatters.NumberedProseFormatter;
import org.brailleblaster.utd.formatters.SkipFormatter;
import org.brailleblaster.utd.formatters.TPageFormatter;
import org.brailleblaster.utd.properties.Align;
import org.brailleblaster.utd.properties.NumberLinePosition;
import org.brailleblaster.utd.properties.PageNumberType;
import org.brailleblaster.utd.properties.PageSide;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Interface for style data.
 */
public interface IStyle {
    @NonNull
    String getId();

    List<ConditionalValue<Integer>> getLinesBeforeWhen();

    List<ConditionalValue<Integer>> getAllLinesBeforeWhen();

    int getLinesBefore();

    List<ConditionalValue<Integer>> getLinesAfterWhen();

    List<ConditionalValue<Integer>> getAllLinesAfterWhen();

    int getLinesAfter();

    /**
     * Get how far text should be indented in cells.
     *
     * @return The number of cells to indent the content.
     */
    @Nullable Integer getIndent();

    /**
     * Get the length for a line in cells.
     *
     * @return The length of the line. Positive values are the actual length, zero or negative values indicate how much shorter than the maximum possible line length the line should be.
     */
    @Nullable Integer getLineLength();

    @Nullable Integer getFirstLineIndent();

    NumberLinePosition getSkipNumberLines();

    int getSkipPages();

    Align getAlign();

    Format getFormat();

    int getNewPagesBefore();

    int getNewPagesAfter();

    PageSide getPageSide();

    PageNumberType getBraillePageNumberFormat();

    boolean isDontSplit();

    boolean isKeepWithNext();

    boolean isKeepWithPrevious();

    int getOrphanControl();

    int getLineSpacing();

    @NonNull
    String getName();

    boolean isTable();

    boolean isTableRow();

    boolean isTableCell();

    String getEndSeparator();

    boolean isGuideWords();

    String getStartSeparator();

    String getColor();

    boolean isPageNum();

    boolean isLineNumber();

    boolean isVolumeEnd();

    enum Format {
        NORMAL(new LiteraryFormatter()),
        TOC(new TOCFormatter()),
        TPAGE(new TPageFormatter()),
        LINENUMBER(new NumberedLineFormatter()),
        PROSE(new NumberedProseFormatter()),
        SIMPLE(new SimpleTableFormatter()),
        LISTED(new ListedTableFormatter()),
        FACING(new FacingTableFormatter()),
        STAIRSTEP(new StairstepTableFormatter()),
        LINEAR(new LinearTableFormatter()),
        AUTO(new AutoTableFormatter()),
        MATH(new MathFormatter()),
        SKIP(new SkipFormatter());
        private final Formatter formatter;

        Format(Formatter formatter) {
            this.formatter = formatter;
        }

        public Formatter getFormatter() {
            return formatter;
        }
    }

    @Nullable Integer getLinesAfter(Node node, NamespaceMap namespaces);

    @Nullable Integer getLinesBefore(Node node, NamespaceMap namespaces);

    /**
     * Gets the minimum number of spaces which should appear before this node.
     * <p>
     * The padding specified by this value is applied between the node the style is applied on and the text which comes before it. Should the text which comes before be on a separate line then the padding will not be applied.
     *
     * @return The minimum number of spaces which should appear to the left of this node.
     */
    int getLeftPadding();

    /**
     * Gets the minimum number of spaces which should appear after this node.
     * <p>
     * The padding specified by this value is applied between the node the style is applied on and the text which comes after it. Should the text which comes after be on a separate line then the padding will not be applied.
     *
     * @return The minimum number of spaces which should appear to the right of this node.
     */
    int getRightPadding();

    /**
     * Create a copy of the style.
     * <p>
     * This method creates an actual copy of the style rather than creating a style based upon another style. Any modifications to the copy will not be seen in the original style object.
     */
    @NonNull
    IStyle copy(@NonNull String id, @NonNull String name);
}

