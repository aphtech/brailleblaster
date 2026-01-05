package org.brailleblaster.spellcheck

import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.mvc.BBSimpleManager
import org.brailleblaster.spi.ModuleFactory

class SpellCheckModuleFactory : ModuleFactory {
    override fun createModules(manager: Manager): Iterable<BBSimpleManager.SimpleListener> = listOf(SpellCheckTool)
}