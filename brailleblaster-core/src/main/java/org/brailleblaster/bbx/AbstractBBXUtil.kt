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
package org.brailleblaster.bbx

import jakarta.xml.bind.annotation.XmlAttribute
import org.apache.commons.lang3.builder.ToStringStyle
import org.brailleblaster.bbx.BBX.*
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.utd.utils.autoToString
import java.util.*

open class AbstractBBXUtil {
    @JvmField
	@XmlAttribute
    protected val sectionType: SectionSubType?

    @JvmField
	@XmlAttribute
    protected val containerType: ContainerSubType?

    @JvmField
	@XmlAttribute
    protected val blockType: BlockSubType?

    @JvmField
	@XmlAttribute
    protected val inlineType: InlineSubType?

    @JvmField
	@XmlAttribute
    protected val spanType: SpanSubType?

    @JvmField
	@XmlAttribute
    protected val listType: ListType?

    @XmlAttribute
    protected val marginType: MarginType?

    @JvmField
	@XmlAttribute
    protected val tableRowType: TableRowType?

    @JvmField
	@XmlAttribute
    protected val emphasisType: EmphasisType?

    @JvmField
	@XmlAttribute
    protected val fixerTodo: FixerTodo?

    protected constructor() {
        sectionType = null
        containerType = null
        blockType = null
        inlineType = null
        spanType = null
        listType = null
        marginType = null
        tableRowType = null
        emphasisType = null
        fixerTodo = null
    }

    constructor(
        coreType: CoreType?,
        sectionType: SectionSubType?,
        containerType: ContainerSubType?,
        blockType: BlockSubType?,
        inlineType: InlineSubType?,
        spanType: SpanSubType?,
        listType: ListType?,
        marginType: MarginType?,
        tableRowType: TableRowType?,
        emphasisType: EmphasisType?,
        fixerTodo: FixerTodo?
    ) {
        this.sectionType = sectionType
        this.containerType = containerType
        this.blockType = blockType
        this.inlineType = inlineType
        this.spanType = spanType
        this.listType = listType
        this.marginType = marginType
        this.tableRowType = tableRowType
        this.emphasisType = emphasisType
        this.fixerTodo = fixerTodo
    }

    protected fun validateOnlyOneBBXFieldSet(vararg aditionalFields: Any?): Set<Any?> {
        return validateNumBBXFieldSet(1, *aditionalFields)
    }

    private fun validateNumBBXFieldSet(count: Int, vararg aditionalFields: Any?): Set<Any?> {
        val configuredFields: MutableSet<Any?> = HashSet()
        configuredFields.add(sectionType)
        configuredFields.add(containerType)
        configuredFields.add(blockType)
        configuredFields.add(inlineType)
        configuredFields.add(spanType)
        configuredFields.add(listType)
        configuredFields.add(marginType)
        configuredFields.add(tableRowType)
        configuredFields.add(emphasisType)
        configuredFields.addAll(listOf(*aditionalFields))
        //nulls will be condensed into one entry + set entry = 2
        if (configuredFields.size != 1 + count) {
            throw RuntimeException("Expected " + count + " set fields, found:" + toString() + " size " + configuredFields.size)
        }
        return configuredFields
    }

    override fun toString(): String {
        return autoToString(this, ToStringStyle.MULTI_LINE_STYLE)
    }
}