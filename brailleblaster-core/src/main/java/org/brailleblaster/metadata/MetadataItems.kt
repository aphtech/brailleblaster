/*
 * Copyright (C) 2026 American Printing House for the Blind
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
package org.brailleblaster.metadata

import java.time.Clock
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface MetadataItem

class Format(val format: String = "1.0") : MetadataItem

class Identifier @OptIn(ExperimentalUuidApi::class) constructor(val identifier: String = Uuid.generateV4().toHexDashString()) : MetadataItem

class Date(val date: LocalDate = LocalDate.now(Clock.systemUTC())) : MetadataItem

class Title(val title: String) : MetadataItem

class Creator(val creator: String) : MetadataItem

class Language(val language: String) : MetadataItem
