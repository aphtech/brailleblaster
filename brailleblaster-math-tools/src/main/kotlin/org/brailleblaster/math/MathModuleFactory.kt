package org.brailleblaster.math

import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.mvc.BBSimpleManager
import org.brailleblaster.spi.ModuleFactory

class MathModuleFactory : ModuleFactory {
    override fun createModules(manager: Manager): Iterable<BBSimpleManager.SimpleListener> = listOf(MathModule())
}