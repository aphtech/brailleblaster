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
package org.brailleblaster.math.numberLine

import nu.xom.Element
import nu.xom.Node
import org.apache.commons.lang3.math.Fraction
import org.brailleblaster.BBIni
import org.brailleblaster.bbx.BBX
import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.math.spatial.*
import org.brailleblaster.math.spatial.SpatialMathEnum.BlankOptions
import org.brailleblaster.math.spatial.SpatialMathEnum.Fill
import org.brailleblaster.math.spatial.SpatialMathEnum.IntervalType
import org.brailleblaster.math.spatial.SpatialMathEnum.NumberLineOptions
import org.brailleblaster.math.spatial.SpatialMathEnum.NumberLineSection
import org.brailleblaster.math.spatial.SpatialMathEnum.NumberLineType
import org.brailleblaster.math.spatial.SpatialMathEnum.NumberLineViews
import org.brailleblaster.math.spatial.SpatialMathEnum.SpatialMathContainers
import org.brailleblaster.math.spatial.SpatialMathUtils.translate
import org.brailleblaster.perspectives.braille.mapping.elements.LineBreakElement
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.WhiteSpaceElement
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.XMLNodeCaret
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.util.Notify
import org.brailleblaster.wordprocessor.WPManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.floor

class NumberLine : ISpatialMathContainer {
    private val formatter: NumberLineFormatter
    fun retranslateLabels() {
        for (i in settings.userDefinedArray.indices) {
            val interval = settings.userDefinedArray[i]
            interval.labelText = NumberLinePoint(
                format = false,
                mathText =
                MathText(
                    print = interval.labelText.mathText.print,
                    braille = translate(
                        settings.translationLabel,
                        interval.labelText.mathText.print
                    )
                )
            )
        }
    }

    fun retranslateUserText() {
        for (i in settings.userDefinedArray.indices) {
            val interval = settings.userDefinedArray[i]
            interval.userText = MathText(
                print = interval.userText.print,
                braille = translate(
                    settings.translationUserDefined, interval.userText.print
                )
            )
        }
    }

    @Throws(MathFormattingException::class)
    fun parse() {
        val oldPoints = numberLineText.points
        val oldSegment = numberLineText.segment
        val newText = settings.stringParser.parse()
        numberLineText = newText
        settings.intervalType = settings.stringParser.getNumberLineType(this)
        numberLineText.points = oldPoints
        numberLineText.segment.startInterval = oldSegment.startInterval
        numberLineText.segment.endInterval = oldSegment.endInterval
        numberLineText.segment.startSegmentCircle = oldSegment.startSegmentCircle
        numberLineText.segment.endSegmentCircle = oldSegment.endSegmentCircle
    }

    fun loadStringParser() {
        try {
            settings.stringParser.unparse(settings.intervalType, numberLineText)
        } catch (e: MathFormattingException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    fun segmentNotEmpty(): Boolean {
        return settings.stringParser.segmentStart.isNotEmpty()
    }

    fun shouldFormatSegment(): Boolean {
        val segmentSettings = settings.sectionType == NumberLineSection.SEGMENT
        val segmentFilled = segmentNotEmpty()
        return segmentSettings && segmentFilled
    }

    fun addBlankOption(): Boolean {
        var hasBlank = false
        for (i in settings.userDefinedArray.indices) {
            val interval = settings.userDefinedArray[i]
            if (interval.blankType != BlankOptions.NONE) {
                hasBlank = true
            }
        }
        if (hasBlank) {
            settings.options.add(NumberLineOptions.BLANKS)
        }
        return hasBlank
    }

    fun addLabelOption(): Boolean {
        var hasLabel = false
        for (i in settings.userDefinedArray.indices) {
            val interval = settings.userDefinedArray[i]
            if (interval.labelText.mathText.braille.isNotEmpty()) {
                hasLabel = true
            }
        }
        if (hasLabel) {
            settings.options.add(NumberLineOptions.LABELS)
        }
        return hasLabel
    }

    @Throws(MathFormattingException::class)
    fun setSegmentIntervals() {
        if (!segmentNotEmpty()) {
            return
        }
        val intervalFraction = numberLineText.interval.fraction
        val startSegmentFraction = numberLineText.segment.segmentStart.fraction
        val startLineFraction = numberLineText.lineStart.fraction
        val endSegmentFraction = numberLineText.segment.segmentEnd.fraction
        val startInterval = (startSegmentFraction
            .subtract(startLineFraction)
            .divideBy(intervalFraction)
            .toDouble().toInt() + 1)
        val segmentLength = endSegmentFraction
            .subtract(startSegmentFraction)
            .divideBy(intervalFraction)
            .toDouble().toInt()
        segment.startInterval = startInterval
        segment.endInterval = startInterval + segmentLength
    }

    override fun format() {
        try {
            formatter.format()
        } catch (_: MathFormattingException) {
            Notify.notify(NumberLineConstants.NUMERAL_WARNING, Notify.ALERT_SHELL_NAME)
        }
    }

    val segment: NumberLineSegment
        get() = numberLineText.segment
    val segmentPoints: MutableList<NumberLineSegmentPoint>
        get() = numberLineText.points

    fun rebuildPoints(points: Int) {
        if (numberLineText.points.size < points) {
            val difference = points - numberLineText.points.size
            for (j in 0 until difference) {
                numberLineText
                    .points
                    .add(NumberLineSegmentPoint())
            }
        } else if (numberLineText.points.size > points) {
            val difference = numberLineText.points.size - points
            for (j in 0 until difference) {
                numberLineText.points.removeAt(numberLineText.points.size - 1)
            }
        }
    }

    fun initializePoints() {
        if (numberLineText.points.isEmpty()) {
            for (j in 0 until DEFAULT_NUM_POINTS) {
                numberLineText
                    .points
                    .add(NumberLineSegmentPoint())
            }
        }
    }

    override var settings = NumberLineSettings()
    var numberLineText = NumberLineText()
    override var widget: NumberLineWidget
    override val lines = ArrayList<Line>()
    var line = ArrayList<NumberLineLine>()
    var points = ArrayList<NumberLinePoint>()
    override var widestLine = 0
    override var blank = false
    private var hush = false

    init {
        loadSettingsFromFile()
        formatter = NumberLineFormatter(this)
        widget = createWidgetForType(settings.view)
    }


    @Throws(MathFormattingException::class)
    fun makeLine(): Boolean {
        widget.extractText()
        try {
            parse()
        } catch (_: MathFormattingException) {
            Notify.notify(NumberLineConstants.NUMBER_LINE_ALLOWED_CHARS, Notify.ALERT_SHELL_NAME)
            return false
        }
        if (settings.type == NumberLineType.AUTOMATIC_MATH && !mathFormattingChecks(false)) {
            return false
        }
        if (!formatter.format()) {
            if (!hush) {
                Notify.notify(MathModule.LONG_LINE_WARNING, Notify.ALERT_SHELL_NAME)
            }
            return false
        }
        return true
    }

    fun mathFormattingChecks(hush: Boolean): Boolean {
        this.hush = hush
        return try {
            if (intervalIsZero()) {
                if (!hush) {
                    Notify.notify(NumberLineConstants.INTERVAL_IS_ZERO_WARNING, Notify.ALERT_SHELL_NAME)
                } else {
                    log.error(NumberLineConstants.INTERVAL_IS_ZERO_WARNING)
                }
                return false
            }
            if (intervalIsNegative()) {
                if (!hush) {
                    Notify.notify(NumberLineConstants.INTERVAL_IS_NEGATIVE_WARNING, Notify.ALERT_SHELL_NAME)
                } else {
                    log.error(NumberLineConstants.INTERVAL_IS_NEGATIVE_WARNING)
                }
                return false
            }
            if (denominatorIsZero()) {
                if (!hush) {
                    Notify.notify(NumberLineConstants.DENOMINATOR_IS_ZERO_WARNING, Notify.ALERT_SHELL_NAME)
                } else {
                    log.error(NumberLineConstants.DENOMINATOR_IS_ZERO_WARNING)
                }
                return false
            }
            if (!hasLineFilled()) {
                if (!hush) {
                    Notify.notify(NumberLineConstants.EMPTY_FIELD_WARNING, Notify.ALERT_SHELL_NAME)
                } else {
                    log.error(NumberLineConstants.EMPTY_FIELD_WARNING)
                }
                return false
            }
            if (!bothOrNoneSegments()) {
                if (!hush) {
                    Notify.notify(NumberLineConstants.FILL_BOTH_SEGMENTS_WARNING, Notify.ALERT_SHELL_NAME)
                } else {
                    log.error(NumberLineConstants.FILL_BOTH_SEGMENTS_WARNING)
                }
                return false
            }
            if (notInOrder()) {
                if (!hush) {
                    Notify.notify(NumberLineConstants.OUT_OF_ORDER, Notify.ALERT_SHELL_NAME)
                } else {
                    log.error(NumberLineConstants.OUT_OF_ORDER)
                }
                return false
            }
            try {
                if (pointsDontMatchIntervals()) {
                    if (!hush) {
                        Notify.notify(NumberLineConstants.INTERVAL_WARNING, Notify.ALERT_SHELL_NAME)
                    } else {
                        log.error(NumberLineConstants.INTERVAL_WARNING)
                    }
                    return false
                }
            } catch (_: NumberFormatException) {
                if (!hush) {
                    Notify.notify(NumberLineConstants.NUMERAL_WARNING, Notify.ALERT_SHELL_NAME)
                } else {
                    log.error(NumberLineConstants.NUMERAL_WARNING)
                }
                return false
            }
            true
        } catch (_: MathFormattingException) {
            false
        }
    }

    private fun intervalIsNegative(): Boolean {
        return numberLineText.interval.isMinus
    }

    @Throws(MathFormattingException::class)
    private fun notInOrder(): Boolean {
        if (numberLineText.lineStart > numberLineText.lineEnd) {
            return true
        }
        if (shouldFormatSegment()) {
            if (numberLineText.segment.segmentStart > numberLineText.segment.segmentEnd) {
                return true
            }
        }
        return false
    }

    fun hasLineFilled(): Boolean {
        return (settings.stringParser.lineEndString.isNotBlank()
                && settings.stringParser.lineStartString.isNotBlank())
    }

    private fun notEmptyAndZero(s: String): Boolean {
        return try {
            s.isNotBlank() && s.toInt() == 0
        } catch (_: NumberFormatException) {
            false
        }
    }

    fun denominatorIsZero(): Boolean {
        if (settings.intervalType == IntervalType.IMPROPER || settings.intervalType == IntervalType.MIXED) {
            if (notEmptyAndZero(numberLineText.interval.denominator)
                || notEmptyAndZero(numberLineText.lineStart.denominator)
                || notEmptyAndZero(numberLineText.lineEnd.denominator)
            ) {
                return true
            }
            if (notEmptyAndZero(numberLineText.segment.segmentStart.denominator)
                || notEmptyAndZero(numberLineText.segment.segmentEnd.denominator)
            ) {
                return true
            }
        }
        return false
    }

    @Throws(MathFormattingException::class)
    fun intervalIsZero(): Boolean {
        return numberLineText.interval.fraction.compareTo(Fraction.ZERO) == 0
    }

    /** @return true if both or none are filled, if only one filled return false
     */
    fun bothOrNoneSegments(): Boolean {
        return if (numberLineText.segment.segmentStart.isEmpty) numberLineText.segment.segmentEnd.isEmpty
        else !numberLineText.segment.segmentEnd.isEmpty
    }

    @Throws(MathFormattingException::class)
    fun pointsDontMatchIntervals(): Boolean {
        val interval = numberLineText.interval.fraction
        val lineStart = numberLineText.lineStart.fraction
        val lineEnd = numberLineText.lineEnd.fraction
        val adjustedLineEnd = lineEnd.subtract(lineStart)
        val b = (adjustedLineEnd.divideBy(interval).toDouble()
                - floor(adjustedLineEnd.divideBy(interval).toDouble())
                != 0.0)
        if (b) {
            return true
        }
        if (shouldFormatSegment()) {
            val segmentStart = numberLineText.segment.segmentStart.fraction
            val segmentEnd = numberLineText.segment.segmentEnd.fraction
            val adjustedSegmentStart = segmentStart.subtract(lineStart)
            val adjustedSegmentEnd = segmentEnd.subtract(lineStart)
            val c = (adjustedSegmentStart.divideBy(interval).toDouble()
                    - floor(adjustedSegmentStart.divideBy(interval).toDouble())
                    != 0.0)
            val d = (adjustedSegmentEnd.divideBy(interval).toDouble()
                    - floor(adjustedSegmentEnd.divideBy(interval).toDouble())
                    != 0.0)
            if (c || d) {
                return true
            }
        }
        return false
    }

    override fun saveSettings() {
        BBIni.propertyFileManager
            .save(USER_SETTINGS_INTERVAL, settings.intervalType.name)
        BBIni.propertyFileManager
            .save(USER_SETTINGS_START_SEGMENT, settings.startSegmentCircle.name)
        BBIni.propertyFileManager
            .save(USER_SETTINGS_END_SEGMENT, settings.endSegmentCircle.name)
        BBIni.propertyFileManager
            .saveAsBoolean(USER_SETTINGS_REDUCE, settings.isReduceFraction)
        BBIni.propertyFileManager
            .saveAsBoolean(USER_SETTINGS_BEVELED, settings.isBeveledFraction)
        BBIni.propertyFileManager.saveAsBoolean(USER_SETTINGS_ARROW, settings.isArrow)
        BBIni.propertyFileManager.saveAsBoolean(USER_SETTINGS_STRETCH, settings.isStretch)
        BBIni.propertyFileManager
            .saveAsBoolean(USER_SETTINGS_LEADING_ZEROS, settings.isRemoveLeadingZeros)
    }

    /*
     * Interval type, start segment type, end segment type, reduce, beveled, arrows,
     * stretch, leading zeros
     */
    override fun loadSettingsFromFile() {
        val intervalString =
            BBIni.propertyFileManager.getProperty(USER_SETTINGS_INTERVAL, settings.intervalType.name)
        val interval = IntervalType.valueOf(intervalString)
        settings.intervalType = interval
        val startString =
            BBIni.propertyFileManager.getProperty(USER_SETTINGS_START_SEGMENT, settings.startSegmentCircle.name)
        val start = Fill.valueOf(startString)
        settings.startSegmentCircle = start
        val endString =
            BBIni.propertyFileManager.getProperty(USER_SETTINGS_END_SEGMENT, settings.endSegmentCircle.name)
        val end = Fill.valueOf(endString)
        settings.endSegmentCircle = end
        val reduce =
            BBIni.propertyFileManager.getProperty(USER_SETTINGS_REDUCE, "true")
        settings.isReduceFraction = java.lang.Boolean.valueOf(reduce)
        val beveled =
            BBIni.propertyFileManager.getProperty(USER_SETTINGS_BEVELED, "false")
        settings.isBeveledFraction = java.lang.Boolean.valueOf(beveled)
        val arrow =
            BBIni.propertyFileManager.getProperty(USER_SETTINGS_ARROW, "true")
        settings.isArrow = java.lang.Boolean.valueOf(arrow)
        val stretch =
            BBIni.propertyFileManager.getProperty(USER_SETTINGS_STRETCH, "false")
        settings.isStretch = java.lang.Boolean.valueOf(stretch)
        val zero =
            BBIni.propertyFileManager.getProperty(USER_SETTINGS_LEADING_ZEROS, "false")
        settings.isRemoveLeadingZeros = java.lang.Boolean.valueOf(zero)
    }

    override val typeEnum: SpatialMathContainers = SpatialMathContainers.NUMBER_LINE

    internal fun createWidgetForType(type: NumberLineViews) = when (type) {
        NumberLineViews.AUTOMATIC_MATH -> {
            NumberLineWidgetAutomaticMath()
        }

        NumberLineViews.BLANKS -> {
            NumberLineWidgetAddBlanks()
        }

        NumberLineViews.LABELS -> {
            NumberLineWidgetAddLabels()
        }

        else -> {
            NumberLineWidgetUserDefined()
        }
    }

    override val json: NumberLineJson
        get() = NumberLineJson().containerToJson(this) as NumberLineJson

    override fun preFormatChecks(): Boolean {
        addBlankOption()
        addLabelOption()
        if (settings.type == NumberLineType.AUTOMATIC_MATH) {
            try {
                parse()
            } catch (_: MathFormattingException) {
                Notify.notify(NumberLineConstants.NUMBER_LINE_ALLOWED_CHARS, Notify.ALERT_SHELL_NAME)
                return false
            }
            if (!mathFormattingChecks(false)) {
                return false
            }
        }
        return true
    }

    val lastIntervalLength: Int
        get() {
            var lastInterval = points[points.size - 1].rightDec.length
            if (settings.options.contains(NumberLineOptions.LABELS)) {
                val interval = settings.userDefinedArray[settings.userDefinedArray.size - 1]
                if (interval.labelText.rightDec.length > lastInterval) {
                    lastInterval = interval.labelText.rightDec.length + 1
                }
            }
            return lastInterval
        }

    companion object {
        private const val USER_SETTINGS_INTERVAL = "nle.interval"
        private const val USER_SETTINGS_START_SEGMENT = "nle.startSegment"
        private const val USER_SETTINGS_END_SEGMENT = "nle.endSegment"
        private const val USER_SETTINGS_REDUCE = "nle.reduce"
        private const val USER_SETTINGS_BEVELED = "nle.beveled"
        private const val USER_SETTINGS_ARROW = "nle.arrow"
        private const val USER_SETTINGS_STRETCH = "nle.stretch"
        private const val USER_SETTINGS_LEADING_ZEROS = "nle.leadingZeros"
        val log: Logger = LoggerFactory.getLogger(NumberLine::class.java)

        @JvmStatic
        fun getContainerFromElement(node: Node): NumberLine {
            var numberLine = NumberLine()
            var ele = node as Element
            if (BBX.CONTAINER.NUMBER_LINE.isA(ele)) {
                if (!BBX.CONTAINER.NUMBER_LINE.ATTRIB_VERSION.has(ele)) {
                    ele = VersionConverter.convertNumberLine(ele)
                }
                val json = BBX.CONTAINER.NUMBER_LINE.JSON_NUMBER_LINE[ele] as NumberLineJson
                numberLine = json.jsonToContainer() as NumberLine
            }
            return numberLine
        }

        @JvmStatic
        fun middleNumberLine(currentElement: TextMapElement): Boolean {
            if (currentElement.node == null || currentElement is LineBreakElement) {
                return false
            }
            val numberLine = XMLHandler.ancestorVisitorElement(
                currentElement.node
            ) { node: Element? -> BBX.CONTAINER.NUMBER_LINE.isA(node) }
            return numberLine != null
        }

        @JvmStatic
        fun isNumberLine(node: Node?): Boolean {
            if (node == null || node.document == null) {
                return false
            }
            return XMLHandler.ancestorElementIs(node) { e: Element -> BBX.CONTAINER.NUMBER_LINE.isA(e) }
        }

        @JvmStatic
        fun getNumberLineParent(node: Node): Element? {
            return XMLHandler.ancestorVisitorElement(node) { e: Element -> BBX.CONTAINER.NUMBER_LINE.isA(e) }
        }

        @JvmStatic
        fun initialize(n: Node): Element {
            val e = n as Element
            val t = getContainerFromElement(e)
            t.format()
            val newElement = BBX.CONTAINER.NUMBER_LINE.create(t)
            try {
                SpatialMathBlock.format(newElement, t.lines)
            } catch (e1: MathFormattingException) {
                e1.printStackTrace()
            }
            e.parent.replaceChild(e, newElement)
            return newElement
        }

        @JvmStatic
        fun currentIsNumberLine(): Boolean {
            val current: Node? = XMLHandler.ancestorVisitorElement(
                WPManager.getInstance().controller
                    .simpleManager.currentCaret.node
            ) { node: Element? -> BBX.CONTAINER.NUMBER_LINE.isA(node) }
            val isWhitespace = WPManager.getInstance().controller.mapList.current is WhiteSpaceElement
            return !isWhitespace && current != null
        }

        fun deleteNumberLine() {
            val current = XMLHandler.ancestorVisitorElement(
                WPManager.getInstance().controller.simpleManager.currentCaret.node
            ) { node -> BBX.CONTAINER.NUMBER_LINE.isA(node) }!!
            // get previous block for caret event
            val previous = XMLHandler.previousSiblingNode(current)
            val parent = current.parent
            parent.removeChild(current)
            WPManager.getInstance()
                .controller
                .simpleManager
                .dispatchEvent(ModifyEvent(Sender.TEXT, true, parent))
            if (previous != null) {
                WPManager.getInstance()
                    .controller
                    .simpleManager
                    .dispatchEvent(XMLCaretEvent(Sender.TEXT, XMLNodeCaret(previous)))
            }
        }

        private const val DEFAULT_NUM_POINTS = 1
    }
}