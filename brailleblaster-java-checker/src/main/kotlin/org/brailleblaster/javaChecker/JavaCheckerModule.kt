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
package org.brailleblaster.javaChecker

import org.brailleblaster.AppProperties
import org.brailleblaster.BBIni
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.events.AppStartedEvent
import org.brailleblaster.spi.ModuleFactory
import org.eclipse.jface.dialogs.MessageDialogWithToggle
import org.eclipse.swt.widgets.Display

object JavaCheckerModule : SimpleListener {
    override fun onEvent(event: SimpleEvent) {
        if (event is AppStartedEvent) {
            val minVersion = Runtime.Version.parse("21")
            val jvmVersion = Runtime.version()
            val propManager = BBIni.propertyFileManager
            val warnVersion = maxOf(jvmVersion, Runtime.Version.parse(propManager.getProperty("javaChecker.warn.version", "11")))
            if (warnVersion < minVersion) {
                val result = MessageDialogWithToggle.openWarning(Display.getCurrent()?.activeShell, "Outdated Java", "Your Java runtime is no longer supported by ${AppProperties.displayName} and may in the future fail to run this software. Please update to at least Java ${minVersion}.", "Do not show this warning again", false, null, null)
                if (result.toggleState) {
                    propManager.save("javaChecker.warn.version", minVersion.toString())
                }
            }
        }
    }
}

class JavaCheckerModuleFactory() : ModuleFactory {
    override fun createModules(manager: Manager): Iterable<SimpleListener> = listOf(JavaCheckerModule)
}