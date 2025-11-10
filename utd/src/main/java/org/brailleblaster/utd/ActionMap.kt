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
package org.brailleblaster.utd

import jakarta.xml.bind.annotation.XmlRootElement
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.utd.actions.GenericAction
import org.brailleblaster.utd.actions.IAction
import org.brailleblaster.utd.actions.SkipAction
import org.brailleblaster.utd.internal.ActionMapAdapter
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.xml.UTD_NS
import kotlin.jvm.Throws

@XmlJavaTypeAdapter(ActionMapAdapter::class)
@XmlRootElement(name = "actionMap")
open class ActionMap : NodeMatcherMap<IAction>, IActionMap {
    constructor() : super(GenericAction())
    constructor(map: NodeMatcherMap<IAction>) : super(map)

    @Throws(NoSuchElementException::class)
    override fun findValue(node: Node): IAction {
        if (node is Text) {
            return textAction
        } else if ((node is Element) && UTD_NS == node.namespaceURI) {
            return skipAction
        } else if (UTDElements.BRL.isA(node)) {
            return skipAction
        }
        return super.findValue(node)
    }

    companion object {
        private val textAction: IAction = GenericAction()
        private val skipAction: IAction = SkipAction()
    }
}
