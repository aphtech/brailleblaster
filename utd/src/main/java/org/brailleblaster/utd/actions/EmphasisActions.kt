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
package org.brailleblaster.utd.actions

import jakarta.xml.bind.annotation.XmlAttribute
import nu.xom.Node
import org.brailleblaster.libembosser.utils.BrailleMapper
import org.brailleblaster.utd.ITranslationEngine
import org.brailleblaster.utd.TextSpan
import org.brailleblaster.utd.properties.BrailleTableType
import org.brailleblaster.utd.properties.EmphasisType
import java.util.*

abstract class BaseEmphasisAction : IAction {
    private val action = GenericAction()
    protected abstract val emphasis: EnumSet<EmphasisType>
    override fun applyTo(node: Node, context: ITranslationEngine): List<TextSpan> {
        /*
     *  It will gather all the List<TextSpan> results of the actions of its
     *  child nodes (like GenericAction does, in fact this is why I think in
     *  this case subclassing might be valid now).
     */
        val processedInput = action.applyTo(node, context)

        /*
     *  It should then go through every TextSpan it has gathered up and add
     *  Emphasis.BOLD to each TextSpan's emphasis property.
     */for (input in processedInput) {
            processEmphasis(node, input)
        }
        return processedInput
    }

    protected open fun processEmphasis(origNode: Node?, processedInput: TextSpan) {
        // When using NO_TRANSLATE we should send it through LibLouis as unicode Braille
        // This prevents LibLouis applying correct opcode rules to it
        // This is because there are no correct opcode rules processing unicode Braille.
        if (emphasis.contains(EmphasisType.NO_TRANSLATE)) {
            processedInput.text = BrailleMapper.ASCII_TO_UNICODE_FAST.map(processedInput.text)
        }
        processedInput.emphasis.addAll(emphasis)
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 89 * hash + Objects.hashCode(emphasis)
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        val o = other as BaseEmphasisAction
        // Compare the emphasis objects which should be the same
        return emphasis == o.emphasis
    }
}

@Suppress("UNUSED")
class EmphasisAction : BaseEmphasisAction() {
    @XmlAttribute(name = "emphasis")
    override var emphasis: EnumSet<EmphasisType> = EnumSet.noneOf(EmphasisType::class.java)
}

class BoldAction : BaseEmphasisAction() {
    override val emphasis: EnumSet<EmphasisType> = EnumSet.of(EmphasisType.BOLD)
}

class CompBRLAction : BaseEmphasisAction() {
    override val emphasis: EnumSet<EmphasisType> = EnumSet.of(EmphasisType.NO_TRANSLATE)
}

/*
 * This class uses the same Typeform in LibLouis as CompBRLAction.
 * The reason for this redundancy is that in the UI for BrailleBlaster,
 * Direct action and Computer Braille action are two separate things, but
 * they map to the same action internally.
 */
class DirectAction : BaseEmphasisAction() {
    override val emphasis: EnumSet<EmphasisType> = EnumSet.of(EmphasisType.NO_TRANSLATE)
}

class ItalicsAction : BaseEmphasisAction() {
    override val emphasis: EnumSet<EmphasisType> = EnumSet.of(EmphasisType.ITALICS)
}

class NoContractionAction : BaseEmphasisAction() {
    override val emphasis: EnumSet<EmphasisType> = EnumSet.of(EmphasisType.NO_CONTRACT)
}

class ScriptAction : BaseEmphasisAction() {
    override val emphasis: EnumSet<EmphasisType> = EnumSet.of(EmphasisType.SCRIPT)
}

class Trans1Action : BaseEmphasisAction() {
    override val emphasis: EnumSet<EmphasisType> = EnumSet.of(EmphasisType.TRANS_1)
}

class Trans2Action : BaseEmphasisAction() {
    override val emphasis:EnumSet<EmphasisType> = EnumSet.of(EmphasisType.TRANS_2)
}

class Trans3Action : BaseEmphasisAction() {
    override val emphasis: EnumSet<EmphasisType> = EnumSet.of(EmphasisType.TRANS_3)
}

class Trans4Action : BaseEmphasisAction() {
    override val emphasis: EnumSet<EmphasisType> = EnumSet.of(EmphasisType.TRANS_4)
}

class Trans5Action : BaseEmphasisAction() {
    override val emphasis:EnumSet<EmphasisType> = EnumSet.of(EmphasisType.TRANS_5)
}

class UnderlineAction : BaseEmphasisAction() {
    override val emphasis: EnumSet<EmphasisType> = EnumSet.of(EmphasisType.UNDERLINE)
}

class TransNoteAction : BaseEmphasisAction(), IBlockAction {
    override val emphasis:EnumSet<EmphasisType> = EnumSet.of(EmphasisType.TRANS_NOTE)

    var gba = GenericBlockAction()
    override fun applyTo(node: Node, context: ITranslationEngine): List<TextSpan> {
        val spanList = super.applyTo(node, context)
        gba.translateString(spanList, BrailleTableType.LITERARY, context)
        return emptyList()
    }

    companion object {
        const val START = "\uf000"
        const val END = "\uf001"
        const val UEB_START = "`.<"
        const val UEB_END = "`.>"
        const val EBAE_START = ",'"
        const val EBAE_END = ",'"
        fun getStart(brailleStandard: String): String {
            return if (brailleStandard.lowercase(Locale.getDefault()).contains("EBAE".lowercase(Locale.getDefault()))) {
                EBAE_START
            } else {
                UEB_START
            }
        }

        fun getEnd(brailleStandard: String): String {
            return if (brailleStandard.lowercase(Locale.getDefault()).contains("EBAE".lowercase(Locale.getDefault()))) {
                EBAE_END
            } else {
                UEB_END
            }
        }
    }
}