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
package org.brailleblaster.bbx.legacy

/**
 * Result of bringing an older BrailleBlaster document up to the current format.
 *
 * @param release the BrailleBlaster release line the file came from, e.g. "1.x"
 * @param sourceVersion the format version found in the file
 * @param fallbacks one human readable entry per item that could not be converted exactly and was
 * put into the document as a Body Text block instead
 */
data class LegacyImportReport(
    val release: String,
    val sourceVersion: String,
    val fallbacks: List<String>
)
