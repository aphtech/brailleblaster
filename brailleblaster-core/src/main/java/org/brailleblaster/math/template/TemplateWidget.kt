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
package org.brailleblaster.math.template

import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.math.spatial.*
import org.brailleblaster.math.spatial.SpatialMathEnum.OPERATOR
import org.brailleblaster.math.spatial.SpatialMathEnum.OPERATOR.Companion.stringArray
import org.brailleblaster.math.template.Template.Companion.translateIdentifier
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.util.FormUIUtils
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.*

class TemplateWidget : ISpatialMathWidget {
    var template: Template? = null
    override fun fillDebug(t: ISpatialMathContainer) {
        template = t as Template
        val array =
            (0 until (template!!.settings.operands * template!!.settings.type.numberComponents)).map {
                    (it + 1).toString() + (((it + 1) * 2).toString() +
                        if (it % 2 == 0) ((it + 1) * 2).toString() else "") }
        template!!.printOperands = array
        val array2 = ArrayList<String>()
        for (i in 0 until (template!!.settings.solutions * template!!.settings.type.numberComponents)) {
            array2.add((i + 1).toString() + (((i + 1) * 2).toString() + if (i % 2 == 0) ((i + 1) * 2).toString() else ""))
        }
        template!!.printSolutions = array2
        template!!.identifier = MathText(print="1", braille=translateIdentifier("1", template!!))
    }

    private fun extractFromTextBoxes() {
        template?.let { t ->
            t.printOperands = t.ui.operandBoxes.map { it.text }
            t.printSolutions = t.ui.solutionBoxes.map { it.text }
            t.identifier = MathText(print=t.ui.identifierBox?.text ?: "",
                braille=translateIdentifier(t.ui.identifierBox?.text ?: "", t))
        }
    }

    private fun makeOperandGroup(parent: Composite, cols: Int): Group {
        val g = EasySWT.makeGroup(parent, SWT.NONE, cols, false)
        g.text = TemplateConstants.OPERAND_GROUP
        return g
    }

    private fun makeSolutionGroup(parent: Composite, cols: Int): Group {
        val g = EasySWT.makeGroup(parent, SWT.NONE, cols, false)
        g.text = TemplateConstants.SOLUTION_GROUP
        return g
    }

    private fun makeOperandFraction(parent: Composite) {
        template!!.ui.operandBoxes.clear()
        val components = template!!.settings.type.numberComponents
        for (i in 0 until template!!.settings.operands) {
            for (j in 0 until components) {
                EasySWT.makeLabel(parent, template!!.settings.type.operandComponents[j] + " " + (i + 1), 1)
                template!!.ui.operandBoxes.add(EasySWT.makeText(parent, 1))
            }
        }
    }

    private fun makeIdentifierGroup(parent: Composite, cols: Int): Group {
        val templateTypes = EasySWT.makeGroup(parent, SWT.NONE, cols, false)
        templateTypes.text = TemplateConstants.IDENTIFIER_GROUP
        return templateTypes
    }

    private fun makeIdentifier(g: Group) {
        template?.let {
            EasySWT.makeLabel(g, TemplateConstants.IDENTIFIER_LABEL, 1)
            it.ui.identifierBox = EasySWT.makeText(g, 1).apply {
                text = it.identifier.braille
            }
        }
    }

    private fun makeSolutionRadical(g3: Group) {
        template!!.ui.solutionBoxes.clear()
        EasySWT.makeLabel(g3, template!!.settings.type.solutionComponents[0], 1)
        template!!.ui.solutionBoxes.add(EasySWT.makeText(g3, 1))
        EasySWT.makeLabel(g3, TemplateConstants.REMAINDER, 1)
        template!!.ui.solutionBoxes.add(EasySWT.makeText(g3, 1))
    }

    private fun makeOperandRadical(parent: Group) {
        template!!.ui.operandBoxes.clear()
        EasySWT.makeLabel(parent, template!!.settings.type.operandComponents[0], 1)
        template!!.ui.operandBoxes.add(EasySWT.makeText(parent, 1))
        EasySWT.makeLabel(parent, template!!.settings.type.operandComponents[1], 1)
        template!!.ui.operandBoxes.add(EasySWT.makeText(parent, 1))
    }

    private fun makeSolutionFraction(g3: Group) {
        template!!.ui.solutionBoxes.clear()
        val components = template!!.settings.type.numberComponents
        if (template!!.settings.solutions > 0) {
            for (j in 0 until components) {
                EasySWT.makeLabel(g3, template!!.settings.type.solutionComponents[j], 1)
                template!!.ui.solutionBoxes.add(EasySWT.makeText(g3, 1))
            }
        }
    }

    override fun getWidget(parent: Composite, container: ISpatialMathContainer): Composite {
        template = container as Template
        if (template!!.settings.type == SpatialMathEnum.TemplateType.FRACTION_ENUM) {
            val g5 = makeIdentifierGroup(parent, 2)
            makeIdentifier(g5)
            val g4 = makeOperatorOperandGroup(parent, 2)
            val g1 = makeOperatorGroup(g4, 1)
            val g2 = makeOperandGroup(g4, 6)
            makeOperator(g1)
            makeOperandFraction(g2)
            if (template!!.settings.solutions > 0) {
                val g3 = makeSolutionGroup(parent, 6)
                makeSolutionFraction(g3)
            } else {
                template!!.ui.solutionBoxes.clear()
            }
        } else if (template!!.settings.type == SpatialMathEnum.TemplateType.RADICAL_ENUM) {
            val g5 = makeIdentifierGroup(parent, 2)
            makeIdentifier(g5)
            val g2 = makeOperandGroup(parent, 2)
            makeOperandRadical(g2)
            if (template!!.settings.solutions > 0) {
                val g3 = makeSolutionGroup(parent, 2)
                makeSolutionRadical(g3)
            } else {
                template!!.ui.solutionBoxes.clear()
            }
        } else {
            val g5 = makeIdentifierGroup(parent, 2)
            makeIdentifier(g5)
            val g4 = makeOperatorOperandGroup(parent, 2)
            val g1 = makeOperatorGroup(g4, 1)
            makeOperandGroup(g4, 2)
            makeOperator(g1)
            //Simplify the Operands
            template!!.ui.operandBoxes.clear()
            val components = template!!.settings.type.numberComponents
            for (i in 0 until template!!.settings.operands) {
                for (j in 0 until components) {
                    EasySWT.makeLabel(parent, template!!.settings.type.operandComponents[j] + " " + (i + 1), 1)
                    template!!.ui.operandBoxes.add(EasySWT.makeText(parent, 1))
                }
            }
            if (template!!.settings.solutions > 0) {
                val g3 = makeSolutionGroup(parent, 2)
                //Simplify the Solutions
                template!!.ui.solutionBoxes.clear()
                if (template!!.settings.solutions > 0) {
                    EasySWT.makeLabel(g3, template!!.settings.type.solutionComponents[0], 1)
                    template!!.ui.solutionBoxes.add(EasySWT.makeText(g3, 1))
                }
            } else {
                template!!.ui.solutionBoxes.clear()
            }
        }
        return parent
    }

    private fun makeOperator(parent: Group) {
        val operatorCombo = Combo(parent, SWT.DROP_DOWN or SWT.READ_ONLY)
        operatorCombo.data = GridData(SWT.FILL, SWT.FILL, true, true)
        operatorCombo.setItems(*stringArray())
        FormUIUtils.addSelectionListener(operatorCombo) {
            template!!.settings.operator = TemplateSettings.enumifyOperator(operatorCombo.text)
            extractFromTextBoxes()
            SpatialMathDispatcher.dispatch()
        }
        operatorCombo.select(operatorCombo.indexOf(template!!.settings.operator.symbol))
    }

    private fun makeOperatorGroup(parent: Group, cols: Int): Group {
        val g = EasySWT.makeGroup(parent, SWT.NONE, cols, false)
        g.text = TemplateConstants.OPERATOR_LABEL
        return g
    }

    private fun makeOperatorOperandGroup(parent: Composite, cols: Int): Group {
        return EasySWT.makeGroup(parent, SWT.NONE, cols, false)
    }

    override fun onOpen() {
        template?.let {
            for (i in it.ui.operandBoxes.indices) {
                if (it.printOperands.size > i) {
                    it.ui.operandBoxes[i].text = it.printOperands[i]
                } else {
                    it.ui.operandBoxes[i].text = ""
                }
            }
            for (i in it.ui.solutionBoxes.indices) {
                if (it.printSolutions.size > i) {
                    it.ui.solutionBoxes[i].text = it.printSolutions[i]
                } else {
                    it.ui.solutionBoxes[i].text = ""
                }
            }
            it.ui.identifierBox?.text = it.identifier.print
        }
    }

    override fun extractText() {
        extractFromTextBoxes()
    }

    override fun addMenuItems(shell: Shell, menu: Menu, settingsMenu: Menu): Menu {
        addTemplateTypeMenu(shell, menu)
        addOperatorMenu(shell, menu)
        addOptionsMenu(shell, settingsMenu)
        addOperandsMenu(shell, menu)
        addSolutionsMenu(shell, menu)
        return menu
    }

    private fun addSolutionsMenu(shell: Shell, menu: Menu) {
        val cascadeMenu = MenuItem(menu, SWT.CASCADE)
        cascadeMenu.text = TemplateConstants.SOLUTION_GROUP
        val dropDownMenu = Menu(shell, SWT.DROP_DOWN)
        cascadeMenu.menu = dropDownMenu
        for (i in 0..1) {
            val blankBlock = MenuItem(dropDownMenu, SWT.RADIO)
            blankBlock.text = if (i == 0) TemplateConstants.FALSE else TemplateConstants.TRUE
            blankBlock.selection = template!!.settings.solutions == i
            blankBlock.addListener(SWT.Selection) {
                if (!blankBlock.selection) {
                    return@addListener
                }
                extractFromTextBoxes()
                template!!.settings.solutions = i
                SpatialMathDispatcher.dispatch()
                template!!.ui.operandBoxes[0].setFocus()
            }
        }
    }

    private fun addOperandsMenu(shell: Shell, menu: Menu) {
        val cascadeMenu = MenuItem(menu, SWT.CASCADE)
        cascadeMenu.text = TemplateConstants.OPERAND_GROUP
        val dropDownMenu = Menu(shell, SWT.DROP_DOWN)
        cascadeMenu.menu = dropDownMenu
        for (i in 2..OPERAND_LIMIT) {
            val s = i.toString()
            val blankBlock = MenuItem(dropDownMenu, SWT.RADIO)
            blankBlock.text = s
            blankBlock.selection = template!!.settings.operands == i
            blankBlock.addListener(SWT.Selection) {
                if (!blankBlock.selection) {
                    return@addListener
                }
                extractFromTextBoxes()
                template!!.settings.operands = i
                SpatialMathDispatcher.dispatch()
                template!!.ui.operandBoxes[0].setFocus()
            }
        }
    }

    private fun addOptionsMenu(shell: Shell, settingsMenu: Menu) {
        if (MathModule.isNemeth) {
            val b = MenuItem(settingsMenu, SWT.CHECK)
            b.text = TemplateConstants.STRAIGHT_VERTICAL
            b.selection = template!!.settings.isStraightRadicalSymbol
            b.addListener(SWT.Selection) {
                template!!.settings.isStraightRadicalSymbol = !template!!.settings.isStraightRadicalSymbol
                b.selection = template!!.settings.isStraightRadicalSymbol
            }
            val button = MenuItem(settingsMenu, SWT.CHECK)
            button.text = TemplateConstants.LINEAR
            button.selection = template!!.settings.isLinear
            button.addListener(SWT.Selection) {
                template!!.settings.isLinear = !template!!.settings.isLinear
                button.selection = template!!.settings.isLinear
            }
        }
    }

    private fun addOperatorMenu(shell: Shell, menu: Menu) {
        val cascadeMenu = MenuItem(menu, SWT.CASCADE)
        cascadeMenu.text = TemplateConstants.OPERATOR_LABEL
        val dropDownMenu = Menu(shell, SWT.DROP_DOWN)
        cascadeMenu.menu = dropDownMenu
        for (bt in OPERATOR.entries) {
            val blankBlock = MenuItem(dropDownMenu, SWT.RADIO)
            blankBlock.text = bt.prettyName
            blankBlock.selection = template!!.settings.operator == bt
            blankBlock.addListener(SWT.Selection) {
                template!!.settings.operator = bt
                extractFromTextBoxes()
                SpatialMathDispatcher.dispatch()
            }
        }
    }

    private fun addTemplateTypeMenu(shell: Shell, menu: Menu) {
        val cascadeMenu = MenuItem(menu, SWT.CASCADE)
        cascadeMenu.text = TemplateConstants.TEMPLATE_TYPE_LABEL
        val dropDownMenu = Menu(shell, SWT.DROP_DOWN)
        cascadeMenu.menu = dropDownMenu
        for (bt in SpatialMathEnum.TemplateType.entries) {
            val blankBlock = MenuItem(dropDownMenu, SWT.RADIO)
            blankBlock.text = bt.prettyName
            blankBlock.selection = template!!.settings.type == bt
            blankBlock.addListener(SWT.Selection) {
                extractFromTextBoxes()
                template!!.settings.type = bt
                SpatialMathDispatcher.dispatch()
            }
        }
    }

    companion object {
        const val OPERAND_LIMIT = 20
    }
}
