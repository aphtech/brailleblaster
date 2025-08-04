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

import com.google.common.base.CharMatcher;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import nu.xom.Element;
import org.apache.commons.lang3.StringUtils;
import org.brailleblaster.libembosser.spi.BrlCell;
import org.brailleblaster.utd.utils.UTDHelper;
import org.jetbrains.annotations.NotNull;
import org.mwhapples.jlouis.Louis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Write translated text to BRF (Braille Ready File)
 */
public class BRFWriter {
	/**
	 * Output will only receive the *nix newline because its a single char
	 */
	public static final char NEWLINE = '\n';
	public static final char PAGE_SEPARATOR = (char) 0x0C;
	/**
	 * Will override BrailleSettings.useAsciiBraille
	 * Enabled=ASCII braille
	 */
	public static final int OPTS_OUTPUT_ASCII = 1 << 1;
	/**
	 * Will override BrailleSettings.useAsciiBraille
	 * Enabled=Unicode braille dots
	 */
	public static final int OPTS_OUTPUT_UNICODE = 1 << 2;
	/**
	 * Enabled=Unset characters are "."
	 * Default=Spaces with line endings trimmed
	 */
	public static final int OPTS_DEBUG_OUTPUT = 1 << 3;
	public static final int OPTS_DEFAULT = 0;
	private static final Logger log = LoggerFactory.getLogger(BRFWriter.class);
	private final int opts;
	private char[][] grid;
	private final int maxCells;
	private final int maxLines;
	/**
	 * X
	 */
	private int curCell;
	/**
	 * Y
	 */
	private int curLine;
	private final OutputCharStream output;
	public int brlPage = -1;
	public final UTDTranslationEngine engine;
	private boolean afterBookStart = false;
	
	private boolean nonsequentialPages = false;
	private int startOfSequence = -1;
	private int endOfSequence = -1;
	private String lastBrlNum;
	private String lastTranslatedBrlNum;
	private HashMap<Integer, NonsequentialState> nonsequentialMap;

	private record NonsequentialState(char[][] grid, String translatedBrlNum, String brlNum, int curCell, int curLine) {
	}

	@NotNull
	private final InputPageListener inputPageListenerImpl = new InputPageListener();
	@NotNull
	private final PageListener outputPageListener;
	private StringBuilder pendingSpaces = new StringBuilder();
	@NotNull
	public static final PageListener EMPTY_PAGE_LISTENER = new PageListener() {};

	public BRFWriter(UTDTranslationEngine engine, OutputCharStream output, int opts, @NotNull PageListener outputPageListener) {
		this.opts = opts;
		this.outputPageListener = outputPageListener;
		this.engine = engine;
		this.output = output;

		PageSettings pageSettings = engine.getPageSettings();
		BrlCell cell = engine.getBrailleSettings().getCellType();
		log.debug("Drawable size {}x{}", pageSettings.getDrawableWidth(), pageSettings.getDrawableHeight());
		log.debug("Braille Cell type {} {}x{}", cell, cell.getWidth().doubleValue(), cell.getHeight().doubleValue());
		this.maxCells = cell.getCellsForWidth(BigDecimal.valueOf(pageSettings.getDrawableWidth()));
		this.maxLines = cell.getLinesForHeight(BigDecimal.valueOf(pageSettings.getDrawableHeight()));

		log.debug("Created BrfGrid {}x{}", maxCells, maxLines);
		this.grid = new char[maxLines][maxCells];
	}

	/**
	 * Append translated braille to current line
	 */
	public void append(String braille) {
		if (braille == null)
			throw new NullPointerException("braille");
		//Cannot do anything on empty nodes
		if (braille.isEmpty()) {
			log.info(debug("Skipping appending empty string on braille page {}", brlPage));
			return;
		}
		final CharMatcher brailleSpaceMatcher = CharMatcher.whitespace().or(CharMatcher.anyOf("\u2800"));
		final boolean onlyBrailleSpace = brailleSpaceMatcher.matchesAllOf(braille);
		if (brlPage == -1) {
			if (onlyBrailleSpace) {
				log.info("Ignoring whitespace prior to start of document");
				return;
			} else {
				throw new BRFOutputException("Expected <newPage> before text on first page");
			}
		}
		// Hold off inserting whitespace until we know whether it is trailing and followed by a moveTo
		if (onlyBrailleSpace) {
			pendingSpaces.append(braille);
			return;
		} else {
			// Add any pending spaces
			if (!pendingSpaces.isEmpty()) {
				log.debug("Performing implicit moveTo due to pending spaces");
				moveTo(curCell + pendingSpaces.length(), curLine);
			}
			int endSpaces = UTDHelper.endsWithWhitespace(braille);
			if (endSpaces > 0) {
				pendingSpaces.append(braille.substring(braille.length() - endSpaces));
				braille = braille.substring(0, braille.length() - endSpaces);
				log.debug("Srink to '{}'", braille);
			}
		}
		if (curCell + braille.length() > maxCells) {
			throw new BRFOutputException("String \"{}\" length {} is too long", braille, braille.length());
		}
		
		//Issue #4162: non breaking spaces are mangled in other brf readers
		braille = braille.replace('\u00a0', ' ');

		//Potentially convert to ascii or unicode
		String textToAppend = braille;
		try {
			boolean convertToAscii = (OPTS_OUTPUT_ASCII & opts) == OPTS_OUTPUT_ASCII && !engine.getBrailleSettings().isUseAsciiBraille();
			boolean convertToUnicode = (OPTS_OUTPUT_UNICODE & opts) == OPTS_OUTPUT_UNICODE && engine.getBrailleSettings().isUseAsciiBraille();
			if (convertToAscii || convertToUnicode) {
				//TODO:  switch to using LibLouisAPH conversion
				Louis trans = engine.getBrailleTranslator();
				Louis.WideChar input = new Louis.WideChar(braille);
				Louis.WideChar outputLouis = new Louis.WideChar(braille.length());
				if (convertToAscii) {
					trans.dotsToChar(engine.getBrailleSettings().getMainTranslationTable(), input, outputLouis, braille.length(), 0);
					String outputStr = outputLouis.getText(outputLouis.length());
					log.trace("Converted unicode '{}' to ascii '{}' brailleSettings {}", braille, outputStr, engine.getBrailleSettings().isUseAsciiBraille());
					textToAppend = outputStr;
				} else if (convertToUnicode) {
					trans.charToDots(engine.getBrailleSettings().getMainTranslationTable(), input, outputLouis, braille.length(), Louis.TranslationModes.UC_BRL);
					String outputStr = outputLouis.getText(outputLouis.length());
					log.trace("Converted ascii '{}' to unicode '{}'", braille, outputStr);
					textToAppend = outputStr;
				} else {
					throw new IllegalStateException("Wrong options? " + opts);
				}
			}
		} catch (Exception e) {
			throw new BRFOutputException("Failed to translate", e);
		}
		//Write to grid
		for (char curChar : textToAppend.toCharArray()) {
			if (grid[curLine][curCell] != '\0')
				throw new BRFOutputException("Cell already has value {}, line {}, braille {}",
						grid[curLine][curCell], debugLine(grid[curLine]), braille);

			grid[curLine][curCell] = curChar;
			curCell++;
		}
	}

	/**
	 * Move to arbitrary position on the page
	 */
	public void moveTo(int hPosCells, int vPosLines) {
		log.trace("Moving to hPos/cells {} vPos/lines {}", hPosCells, vPosLines);
		if (hPosCells < 0 || hPosCells >= maxCells)
			throw new BRFOutputException("Invalid hPos, given {}", hPosCells);
		if (vPosLines < 0 || vPosLines >= maxLines)
			throw new BRFOutputException("Invalid vPos, given {}", vPosLines);
		curCell = hPosCells;
		curLine = vPosLines;
		pendingSpaces = new StringBuilder();
	}

	/**
	 * Make a new page, flushing grid if necessary
	 */
	public void newPage(int pageNum) {
		log.trace("New page {}", pageNum);
		if (brlPage == -1)
			//Should be able to start the pages on any number the user chooses
//			if (pageNum != 1)
//				throw new BRFOutputException("Unexpected initial page number {}", pageNum);
//			else
				brlPage = pageNum;
		else if (brlPage + 1 != pageNum && pageNum != 1 && !nonsequentialPages && engine.isTestMode()){
			throw new BRFOutputException("Unexpected page jump, current {} given {}", brlPage, pageNum);
		}
		
		if(nonsequentialPages){
			if(startOfSequence == -1){
				startOfSequence = brlPage;
			}
			if(nonsequentialMap.containsKey(brlPage))
				throw new BRFOutputException("Duplicate page {}" + brlPage);
			endOfSequence = Math.max(endOfSequence, brlPage);
			nonsequentialMap.put(brlPage, new NonsequentialState(grid, lastTranslatedBrlNum, lastBrlNum, curCell, curLine));
			log.trace("Storing nonsequential page {}", brlPage);
		}

		if (!afterBookStart) {
			//UTD has a newPage element at the very beginning of the document, but there's nothing to do
			// Remember there is the first print page number as that is processed when entering the brl before child nodes are processed.
			//inputPageListener.pages.clear();
			afterBookStart = true;
			return;
		}
		
		if(!nonsequentialPages)
			flush(grid);
		brlPage = pageNum;
		grid = new char[maxLines][maxCells];
		curLine = 0;
		curCell = 0;
		pendingSpaces = new StringBuilder();
	}
	
	/**
	 * Flush remaining grid
	 */
	public void onEndOfFile() {
		if(nonsequentialPages)
			setNonsequential(false);
		flush(grid);
		log.debug("Reached End Of File");
	}

	private void flush(char[][] grid) {
		log.trace("Flushing grid");
		try {
			int flushLine = 0;
			for (char[] curGridLine : grid) {
				int flushCell = 0;
				boolean ignore = false;
				for (char curGridCell : curGridLine) {
					if (!ignore) {
						if (curGridCell == '\0') {
							//This character hasn't been set yet
							if ((OPTS_DEBUG_OUTPUT & opts) == OPTS_DEBUG_OUTPUT)
								//Helpful debug for the non-transcriber
								output.accept('.');
							else if (String.valueOf(curGridLine, flushCell, curGridLine.length - flushCell).trim().isEmpty())
								//Don't output whitespace at end of line
								ignore = true;
							else
								output.accept(' ');
						} else
							//This character was set
							output.accept(curGridCell);
					}

					//Notify page number listener
					Collection<PageEntry> pageEntries = inputPageListenerImpl.getEntryAtPos(flushCell, flushLine);
					for (PageEntry curPageEntry : pageEntries) {
						if (curPageEntry.isPrintPage) {
							outputPageListener.onPrintPageNum(curPageEntry.braille, curPageEntry.orig);
						} else {
							outputPageListener.onBrlPageNum(curPageEntry.braille, curPageEntry.orig);
						}
					}

					flushCell++;
				}
				output.accept(NEWLINE);
				flushLine++;
			}

			//Page seperator is literally ascii page seperator character, always appended
			output.accept(PAGE_SEPARATOR);

			inputPageListenerImpl.onAfterFlush(this);
			outputPageListener.onAfterFlush(this);
		} catch (Exception e) {
			throw new RuntimeException("Failed to flush BRF grid", e);
		}
	}

	@NotNull
	public PageListener getInputPageListener() {
		return inputPageListenerImpl;
	}
	
	/**
	 * Set whether nonsequential pages will be allowed. If being set to
	 * false from true, will flush out current list of nonsequential
	 * pages.
	 * @throws BRFOutputException A full sequence of pages were not found
	 * 							  while flushing list of nonsequential pages
	 */
	public void setNonsequential(boolean nonsequential){
		if(!nonsequentialPages && nonsequential){
			log.trace("Begin nonsequential pages");
			nonsequentialMap = new HashMap<>();
		} else if (nonsequentialPages && !nonsequential){
			log.trace("End nonsequential pages");
			for(int i = startOfSequence; i <= endOfSequence; i++){
				if(!nonsequentialMap.containsKey(i))
					throw new BRFOutputException("Page {} not found", i);
				NonsequentialState state = nonsequentialMap.get(i);
				flush(state.grid);
				int tempCurCell = curCell;
				int tempCurLine = curLine;
				curCell = state.curCell;
				curLine = state.curLine;
				inputPageListenerImpl.onBrlPageNum(state.translatedBrlNum, state.brlNum);
				curCell = tempCurCell;
				curLine = tempCurLine;
			}
			startOfSequence = -1;
			endOfSequence = -1;
		}
		nonsequentialPages = nonsequential;
	}
	
	public void setNonsequentialBrlNum(String translatedBrlNum, String brlNum){
		lastTranslatedBrlNum = translatedBrlNum;
		lastBrlNum = brlNum;
	}

	public interface PageListener {
		default void onBrlPageNum(String brlPageBraille, String brlPageOrig) {
		}

		default void onPrintPageNum(String printPageBraille, String printPageOrig) {
		}
		
		default void onBeforeBrl(BRFWriter brfWriter, Element brl) {
		}
		
		default void onAfterFlush(BRFWriter brfWriter) {
		}
	}

	@FunctionalInterface
	public interface OutputCharStream {
		void accept(char curChar) throws Exception;
	}
	
	/**
	 * Issue #6646: BRFs must use Windows line endings. However to keep the 
	 * simple char stream API, rewrite the characters when needed
	 */
	public static BRFWriter.OutputCharStream lineEndingRewriter(BRFWriter.OutputCharStream dest) {
		return (char givenChar) -> {
			if (givenChar == BRFWriter.NEWLINE) {
				dest.accept('\r');
				dest.accept('\n');
			} else {
				dest.accept(givenChar);
			}
		};
	}

	/**
	 * Store positions of braille and print pages while building page, will be stored by
	 */
	private class InputPageListener implements PageListener {
		private final Logger log = LoggerFactory.getLogger(InputPageListener.class);
		private final Multimap<Point, PageEntry> pages = ArrayListMultimap.create();

		@Override
		public void onBrlPageNum(String brlPageBraille, String brlPageOrig) {
			log.debug("At {}x{} braille page {} orig {}", curCell, curLine, brlPageBraille, brlPageOrig);
			if (StringUtils.isAnyBlank(brlPageBraille, brlPageOrig)) {
				throw new IllegalArgumentException("blank: brlPageBraille " + brlPageBraille + " and/or brlPageOrig " + brlPageOrig);
			}
			// Braille pages only start after the first newPage.
			// MWhapples: Might it be better to throw an exception as this would indicate invalid UTD when there is a call before the start of Braille?
			if (afterBookStart) {
				pages.put(newPointCur(), new PageEntry(brlPageBraille, brlPageOrig, false));
			}
		}

		@Override
		public void onPrintPageNum(String printPageBraille, String printPageOrig) {
			log.debug("At {}x{} print page {} orig {}", curCell, curLine, printPageBraille, printPageOrig);
			if (StringUtils.isAnyBlank(printPageBraille, printPageOrig))
				throw new IllegalArgumentException("blank: printPageBraille '" + printPageBraille + "' and/or printPageOrig '" + printPageOrig + "'");
			// We only want the last print page number from before the start of the Braille.
			// MWhapples: not really sure we need to clear pages, but as unsure why it was being done for newPage I keep it to ensure similar behaviour.
			if (!afterBookStart) {
				pages.clear();
			}
			pages.put(newPointCur(), new PageEntry(printPageBraille, printPageOrig, true));
		}
		
		private Point newPointCur() {
			//If these are at edges flush() will never call them because their out of the the array index
			//Can be at edges if cursor is at the end of a line and a pagenum is triggered
			int cell = curCell >= maxCells ? maxCells - 1 : curCell;
			int line = curLine >= maxLines ? maxLines - 1 : curLine;
			return new Point(cell, line);
		}

		public Collection<PageEntry> getEntryAtPos(int cell, int line) {
			Collection<PageEntry> entries = pages.get(new Point(cell, line));
			List<PageEntry> result = new ArrayList<>(entries);
			entries.clear();
			return result;
		}

		public void onAfterFlush(BRFWriter brfWriter) {
			if (!pages.isEmpty()) {
				throw new IllegalStateException("Pages isn't empty: " + pages);
			}
		}
	}

	public static class PageEntry {
		final String braille, orig;
		final boolean isPrintPage;

		public PageEntry(String braille, String orig, boolean isPrintPage) {
			this.braille = braille;
			this.orig = orig;
			this.isPrintPage = isPrintPage;
		}

		@Override
		public String toString() {
			return "PageEntry{" + "braille=" + braille + ", orig=" + orig + ", isPrintPage=" + isPrintPage + '}';
		}
	}

	public static class Point {
		final int x, y;

		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public String toString() {
			return "(" + x + "," + y + ')';
		}

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 19 * hash + this.x;
			hash = 19 * hash + this.y;
			return hash;
		}

		@Override
		public boolean equals(Object other) {
			if (other == null)
				return false;
			if (getClass() != other.getClass())
				return false;
			final Point obj = (Point) other;
      return this.x == obj.x && this.y == obj.y;
    }
	}

	private static String debugLine(char[] line) {
		StringBuilder builder = new StringBuilder();
		for (char curChar : line)
			if (curChar == '\0')
				builder.append(".");
			else
				builder.append(curChar);
		return builder.toString();
	}

	protected class BRFOutputException extends RuntimeException {

		public BRFOutputException(String messagePattern, Object... args) {
			this(messagePattern, null, args);
		}

		public BRFOutputException(String messagePattern, Throwable cause, Object... args) {
			super(debug(messagePattern, args), cause);
		}
	}

	/**
	 * SLF4J string formatter
	 */
	private static String formatString(String messagePattern, Object... args) {
		return MessageFormatter.arrayFormat(messagePattern, args).getMessage();
	}

	private String debug(String messagePattern, Object... args) {
		return formatString("BRF page {} position {}x{} max {}x{} - ", brlPage, curCell, curLine, maxCells, maxLines)
				+ formatString(messagePattern, args);
	}
}
