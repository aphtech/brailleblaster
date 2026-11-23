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
package org.brailleblaster.testrunners

import org.brailleblaster.frontmatter.TOCBuilderBBX
import org.brailleblaster.frontmatter.TPagesDialog
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.ui.SimpleImageDescriberModule
import org.brailleblaster.perspectives.mvc.BBSimpleManager
import org.brailleblaster.perspectives.mvc.modules.misc.AlphabeticReferenceModule
import org.brailleblaster.perspectives.mvc.modules.misc.VolumeChangeModule
import org.brailleblaster.perspectives.mvc.modules.misc.VolumeInsertModule
import org.brailleblaster.spi.ModuleFactory

/**
 * Test-only copy of brailleblaster-debug-tools' DebugModulesFactory, for the modules whose code lives in core.
 *
 * The app only registers these through DebugModulesFactory, which core's tests can't load (debug-tools depends on
 * core), so without this there is no TOC Builder, T-Page Generator, volume tools or image describer in GUI tests.
 * The debug-tools debuggers (DebugTool.tools) are left out because their code is in debug-tools.
 * Keep in step with DebugModulesFactory. See test_fix.md, "Plugin modules".
 */
class TestDebugModulesFactory : ModuleFactory {
    override fun createModules(manager: Manager): Iterable<BBSimpleManager.SimpleListener> = listOf(
        TPagesDialog(),
        VolumeInsertModule(),
        TOCBuilderBBX(manager),
        AlphabeticReferenceModule(manager),
        SimpleImageDescriberModule,
        VolumeChangeModule(manager),
    )
}
