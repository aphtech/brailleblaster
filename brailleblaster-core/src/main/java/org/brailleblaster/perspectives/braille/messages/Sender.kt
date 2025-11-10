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
package org.brailleblaster.perspectives.braille.messages

enum class Sender {
  TEXT,
  BRAILLE,
  TREE,
  HANDLER,
  STYLE,
  DEBUG,
  EMPHASIS,
  SEARCH,
  SIMPLEMANAGER,
  UNDO_QUEUE,
  REDO_QUEUE,
  BREADCRUMBS,
  GO_TO_PAGE,
  TOC,
  MATH,
  IMAGE_DESCRIBER,
  VOLUME_CHANGE,
  TAB,
  SIX_KEY,
  NO_SENDER /* Used if dispatching module should process its own event */
}