package org.brailleblaster.perspectives.mvc.modules.misc

import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.mvc.BBSimpleManager
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.MenuManager
import org.brailleblaster.perspectives.mvc.menu.SharedItem
import org.brailleblaster.perspectives.mvc.menu.SubMenuBuilder
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.perspectives.mvc.modules.views.DebugModule
import org.brailleblaster.util.UncontractedGlossary

class AlphabeticReferenceModule(var manager: Manager) : BBSimpleManager.SimpleListener {
    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent && DebugModule.enabled) {
            val smb = SubMenuBuilder(
                TopMenu.DEBUG,
                "Alphabetic Reference"
            )
            smb.addItem(UNCONTRACTED_GLOSSARY, 0) { e: BBSelectionData -> UncontractedGlossary.addUncontractedWord(e) }
            smb.addItem(IDENTIFY_GUIDE_WORDS, 0) {
                FindGuideWordModule.findGuideWords(
                    null,
                    manager
                )
            }
            smb.addItem(TERM_SPACING, 0) {
                AdjustGlossarySpacing.open(
                    manager, manager.wpManager.shell
                )
            }
            smb.addItem(EDIT_GUIDE_WORD, 0, 0, SharedItem.EDIT_GUIDE_WORD) {
                GuideWordEditor.open(
                    manager
                )
            }
            MenuManager.add(smb.build())
        }
    }

    companion object {
        var UNCONTRACTED_GLOSSARY = "Add/Remove Uncontracted Glossary Items"
        var IDENTIFY_GUIDE_WORDS = "Identify Guide Words"
        var TERM_SPACING = "Adjust Term/Definition Spacing"
        @JvmField
		var EDIT_GUIDE_WORD = "Edit Guide Word"
    }
}