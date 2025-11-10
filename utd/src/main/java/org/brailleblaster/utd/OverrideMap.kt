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

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.actions.GenericAction
import org.brailleblaster.utd.actions.IAction
import org.brailleblaster.utd.config.StyleDefinitions
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.matchers.INodeMatcher
import org.brailleblaster.utils.xml.UTD_NS
import kotlin.jvm.Throws

/**
 * Handle the override* attributes on elements
 */
object OverrideMap {
    const val OVERRIDE_ATTRIB_STYLE: String = "overrideStyle"
    const val OVERRIDE_ATTRIB_ACTION: String = "overrideAction"

    /**
     * Auto-generated styleMap that looks for utd:overrideStyle=
     * "Some Style Name" attributes and maps them to the style object
     *
     * @param styleDefs
     * @return
     */
    fun generateOverrideStyleMap(styleDefs: StyleDefinitions): IStyleMap {
        return OverrideStyleMap.newMap(styleDefs)
    }

    /**
     * Auto-generated actionMap that looks for utd:overrideAction="SomeAction"
     * attributes and maps them to their action object
     *
     * @param existingActionMap Original actionMap xml
     * @return
     */
    fun generateOverrideActionMap(existingActionMap: ActionMap): IActionMap {
        return OverrideActionMap.newMap(existingActionMap)
    }

    /**
     * Get name to apply to overrideAction
     *
     * @param action
     * @return
     */
    fun getActionClassName(action: IAction): String {
        return action.javaClass.simpleName
    }

    private class OverrideStyleMap private constructor(stringToValueMap: Map<String?, IStyle>) :
        AbstractOverrideMap<IStyle>(
            OVERRIDE_ATTRIB_STYLE, stringToValueMap
        ), IStyleMap {
        override val defaultValue: IStyle
            get() = Style()
        companion object {
            fun newMap(styleDefs: StyleDefinitions): OverrideStyleMap {
                return OverrideStyleMap(styleDefs.styles.associateBy { it.name })
            }
        }
    }

    private class OverrideActionMap private constructor(stringToValueMap: Map<String?, IAction>) :
        AbstractOverrideMap<IAction>(
            OVERRIDE_ATTRIB_ACTION, stringToValueMap
        ), IActionMap {
        override val defaultValue: IAction
            get() = GenericAction()
        companion object {
            fun newMap(existingActionMap: ActionMap): OverrideActionMap {
                val builder: MutableMap<String?, IAction> = HashMap()
                for (curAction in existingActionMap.values) {
                    val actionName = getActionClassName(curAction)

                    val existingAction = builder[actionName]
                    if (existingAction != null) {
                        val existingName = existingAction.javaClass.toString()
                        val curName = curAction.javaClass.toString()
                        if (curName != existingName) {
                            throw RuntimeException("$curName has same class name as $existingName")
                        } else {
                            //skip multiple entries of the same action
                            continue
                        }
                    }

                    try {
                        //make a new instance to drop entry specific fields that won't make sense in OverrideMaps
                        builder[actionName] = curAction.javaClass.getDeclaredConstructor().newInstance()
                    } catch (ex: Exception) {
                        throw RuntimeException("Failed to load action $curAction", ex)
                    }
                }
                return OverrideActionMap(builder.toMap())
            }
        }
    }

    /**
     * Generic attribute value to object mapper
     *
     * @param <V>
    </V> */
    private abstract class AbstractOverrideMap<V>(
        private val matcherAttributeName: String,
        private val stringToValueMap: Map<String?, V>
    ) : AbstractNonMatcherMap<V>() {
        @Throws(NoSuchElementException::class)
        override fun findValue(node: Node): V {
            if (node !is Element) {
                throw NoSuchElementException("No value found")
            }

            val attribValue: String = node.getAttributeValue(matcherAttributeName, UTD_NS)
                ?: throw NoSuchElementException("No value found")
            val matchingValue = stringToValueMap[attribValue]
                ?: throw NodeException("Node has unknown $matcherAttributeName value $attribValue", node)
            return matchingValue
        }

        override var namespaces: NamespaceMap
            get() = NAMESPACE_MAP_EMPTY
            set(_) { throw UnsupportedOperationException() }

        override val values: MutableCollection<V>
            get() = stringToValueMap.toMutableMap().values
    }

    /**
     * Implements all the unused parts of INodeMatcherMap for non INodeMatcher to V based Maps
     *
     * @param <V>
    </V> */
    abstract class AbstractNonMatcherMap<V> : INodeMatcherMap<V> {

        override var namespaces: NamespaceMap
            get() = throw UnsupportedOperationException()
            set(_) { throw UnsupportedOperationException() }

        override val size: Int
            get() = throw UnsupportedOperationException("Not supported yet.")

        override fun isEmpty(): Boolean {
            throw UnsupportedOperationException("Not supported yet.")
        }

        override fun containsKey(key: INodeMatcher): Boolean {
            throw UnsupportedOperationException("Not supported yet.")
        }

        override fun containsValue(value: V): Boolean {
            throw UnsupportedOperationException("Not supported yet.")
        }

        override fun get(key: INodeMatcher): V? {
            throw UnsupportedOperationException("Not supported yet.")
        }

        override fun put(key: INodeMatcher, value: V): V? {
            throw UnsupportedOperationException("Not supported yet.")
        }

        override fun remove(key: INodeMatcher): V? {
            throw UnsupportedOperationException("Not supported yet.")
        }

        override fun putAll(from: Map<out INodeMatcher, V>) {
            throw UnsupportedOperationException("Not supported yet.")
        }

        override fun clear() {
            throw UnsupportedOperationException("Not supported yet.")
        }

        override val keys: MutableSet<INodeMatcher>
            get() = throw UnsupportedOperationException("Not supported yet.")

        override val values: MutableCollection<V>
            get() = throw UnsupportedOperationException("Not supported yet.")

        override val entries: MutableSet<MutableMap.MutableEntry<INodeMatcher, V>>
            get() = throw UnsupportedOperationException("Not supported yet.")

        companion object {
            val NAMESPACE_MAP_EMPTY: NamespaceMap = NamespaceMap()
        }
    }
}
