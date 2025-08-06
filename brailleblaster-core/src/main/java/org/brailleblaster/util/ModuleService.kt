/*
 * Copyright (C) 2025 Michael Whapples
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
package org.brailleblaster.util

import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.spi.ModuleFactory
import java.util.*

class ModuleService {
    private val serviceLoader: ServiceLoader<ModuleFactory> = ServiceLoader.load(ModuleFactory::class.java)
    val moduleFactories: Sequence<ModuleFactory>
        get() = serviceLoader.asSequence()
    fun modules(manager: Manager): Sequence<SimpleListener> = moduleFactories.sortedByDescending { it.priority }.flatMap { it.createModules(manager) }
}