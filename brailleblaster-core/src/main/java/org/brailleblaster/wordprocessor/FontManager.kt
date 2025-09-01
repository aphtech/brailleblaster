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
package org.brailleblaster.wordprocessor

import org.brailleblaster.BBIni
import org.brailleblaster.abstractClasses.BBEditorView
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.views.style.StylePane
import org.brailleblaster.perspectives.braille.views.wp.BrailleView
import org.brailleblaster.perspectives.braille.views.wp.TextView
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.FontData
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.widgets.Display
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.math.abs

/**
 * Handle fonts in views
 */
class FontManager(val m: Manager) {
    fun initViews() {
        initTextView()
        initBrailleView()
    }

    private fun initTextView() {
        if (BBIni.debugging) {
            m.text.setCharWidth()
            return
        }
        val swtFont = newTextFont()
        log.info(
            "Set text view font to {} height {}",
            @Suppress("UsePropertyAccessSyntax") swtFont.fontData[0].getName(),
            swtFont.fontData[0].getHeight()
        )
        textView.view.font = swtFont
        stylePane.widget.font = swtFont
        lineUpStylePaneWithTextView()
        m.text.setCharWidth()
    }

    private fun initBrailleView() {
        if (BBIni.debugging) {
            m.braille.setCharWidth()
            m.braille.view.editable = false
            return
        }

        val swtFont = newBrailleFont()
        log.info(
            "Set braille view font to {} height {}",
            @Suppress("UsePropertyAccessSyntax") swtFont.fontData[0].getName(),
            swtFont.fontData[0].getHeight()
        )
        brailleView.view.font = swtFont
        lineUpBrailleViewWithTextView()
        m.braille.setCharWidth()
        m.braille.view.editable = false
    }

    fun lineUpStylePaneWithTextView() {
        log.trace("style")
        val indexBefore = textView.view.topPixel
        textView.view.topPixel = 0
        stylePane.view.topPixel = 0
        stylePane.view.lineSpacing = 0
        if (!lineUpSpacingWithTextViewByFontSize(
                stylePane.view,
                SELECTED_TEXT_FONT
            )
        ) {
            lineUpSpacingWithTextViewByLineHeight(stylePane.view)
        }
        textView.view.topPixel = indexBefore
        stylePane.view.topPixel = indexBefore
    }

    fun lineUpBrailleViewWithTextView() {
        log.trace("braille")
        val indexBefore = textView.view.topPixel
        textView.view.topPixel = 0
        brailleView.view.topPixel = 0
        brailleView.view.lineSpacing = 0
        if (!lineUpSpacingWithTextViewByFontSize(
                brailleView.view,
                SELECTED_BRAILLE_FONT
            )
        ) {
            lineUpSpacingWithTextViewByLineHeight(brailleView.view)
        }
        textView.view.topPixel = indexBefore
        brailleView.view.topPixel = indexBefore
    }

    /**
     * Obnoxious Unicode characters can change the line spacing of the entire view
     *
     * @param widget
     * @return false if adjusting did not work
     */
    private fun lineUpSpacingWithTextViewByLineHeight(widget: StyledText): Boolean {
        if (BBIni.debugging) return true
        var previousDelta = getLineDelta(widget)
        if (previousDelta > 0) {
            //something changed, reset instead of leaving view in a broken state
            log.warn("View is bigger than text view")
            widget.lineSpacing = 0
            previousDelta = getLineDelta(widget)
        }
        if (previousDelta == 0) {
            log.info("Views already line up")
            return true
        }

        while (true) {
            val newLineSpacing = widget.lineSpacing + 1
            log.debug("Setting line spacing to {}, ", newLineSpacing)
            widget.lineSpacing = newLineSpacing
            val newDelta = getLineDelta(widget)
            log.debug("previous delta {} new {}", previousDelta, newDelta)

            if (newDelta == 0) {
                log.debug("Done")
                break
            } else if (newDelta == previousDelta) {
                widget.lineSpacing = 0
                log.error("Failed to change line height")
                return false
            } else if (abs(newDelta.toDouble()) >= abs(previousDelta.toDouble())) {
                //too far, previous one matched
                widget.lineSpacing = newLineSpacing - 1
                break
            }

            previousDelta = newDelta
        }
        return true
    }

    /**
     * Eclipse Bug #506572: setLineSpacing does not do anything on Linux. Workaround: Make the font bigger
     *
     * @see .lineUpSpacingWithTextViewByLineHeight
     */
    private fun lineUpSpacingWithTextViewByFontSize(widget: StyledText, font: LoadedFont): Boolean {
        if (BBIni.debugging) return true
        var previousDelta = getLineDelta(widget)
        if (previousDelta > 0) {
            //something changed, reset instead of leaving view in a broken state
            log.warn("View is bigger than text view")
            widget.font = font.newFont()
            previousDelta = getLineDelta(widget)
        }
        if (previousDelta == 0) {
            log.info("Views already line up")
            return true
        }

        var curHeight = widget.font.fontData[0].getHeight()
        while (true) {
            log.debug("Setting font from {} to {}", curHeight, curHeight + FONT_SIZE_DELTA)
            var newFont: Font = font.newFont(FONT_SIZE_DELTA.let { curHeight += it; curHeight })
            widget.font = newFont
            val newDelta = getLineDelta(widget)
            log.debug("previous delta {} new {}", previousDelta, newDelta)

            if (newDelta == 0) {
                return true
            } else if (newDelta == previousDelta) {
                log.error("Failed to change line height by font size?")
                return false
            } else if (newDelta > 0) {
                //too far, previous one matched
                log.trace("too far")
                newFont = font.newFont(FONT_SIZE_DELTA.let { curHeight -= it; curHeight })
                widget.font = newFont
                return previousDelta > 0
            }

            previousDelta = newDelta
        }
    }

    private fun getLineDelta(widget: StyledText): Int {
        val stylePos = widget.getLinePixel(15)
        val textPos = textView.view.getLinePixel(15)
        return stylePos - textPos
    }

    fun toggleBrailleFont(showSimBraille: Boolean) {
        // save setting to user settings property file
        BBIni.propertyFileManager.save(SETTING_SHOW_BRAILLE, showSimBraille.toString())
        initBrailleView()
    }

    fun increaseFont() {
        increaseFontSetting()
        initViews()
    }

    fun decreaseFont() {
        decreaseFontSetting()
        initViews()
    }

    private val brailleView: BrailleView
        get() = m.viewManager.brailleView

    private val textView: TextView
        get() = m.viewManager.textView

    private val stylePane: StylePane
        get() = m.viewManager.stylePane

    open class LoadedFont(protected val filename: String, val defaultHeight: Int) {
        var name: String = filename.substringBeforeLast('.')
        private var loaded = false

        /**
         * @return true on success
         */
        open fun load(): Boolean {
            if (loaded) {
                return true
            }

            val fonts = Display.getCurrent().getFontList(null, true)
            if (fonts.any {
                @Suppress("UsePropertyAccessSyntax")
                it.getName()!!.contentEquals(name)
            }) {
                loaded = true
                return true
            }

            val fontPath = File(FONT_DIR, filename).absolutePath
            log.info("Loading font: {}", fontPath)
            loaded = true
            val result = Display.getCurrent().loadFont(fontPath)
            if (!result) {
                log.error("SWT failed to load font {}", fontPath)
            }
            return result
        }

        @JvmOverloads
        fun newFont(height: Int = defaultHeight): Font {
            check(load()) { "Font $name not loaded" }

            return Font(Display.getCurrent(), name, height.coerceAtLeast(1), SWT.NONE)
        }
    }

    private class SystemFont(name: String, defaultHeight: Int) : LoadedFont(name, defaultHeight) {
        override fun load(): Boolean {
            return fontExists(name)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(FontManager::class.java)
        private val FONT_DIR = BBIni.programDataPath.resolve("fonts").toFile()
        private const val SETTING_SHOW_BRAILLE = "simBraille"
        private const val SETTING_SIZE_DELTA = "fontManager.sizeDelta"
        private fun isFontLoaded(it: LoadedFont) = if (!it.load()) {
            log.warn("Unable to load font {}", it.name)
            false
        } else true
        @Suppress("UsePropertyAccessSyntax")
        private val FALLBACK_FONT_NAME = Display.getCurrent().getFontList(null, true)[0].getName()
        private val TEXT_FONTS: List<LoadedFont> = listOf( //Monospaced Microsoft Font
            SystemFont("Courier New", 12),  //Monospaced Ubuntu Font (Ubuntu does not ship with Microsoft fonts)
            SystemFont("Liberation Mono", 12)
        ).filter { isFontLoaded(it) }.ifEmpty {
            log.error("No text fonts found, using fallback {}", FALLBACK_FONT_NAME)
            listOf(SystemFont(FALLBACK_FONT_NAME, 12))
        }

        val BRAILLE_FONTS: List<LoadedFont> = listOf( //APH Braille Font with shadow dots
            //Different font sizes to try to line up with text view
            LoadedFont("APH_Braille_Font-6s.otf", 12),  //Show braille as ascii
            LoadedFont("unibraille29.ttf", 12)
        ).filter { isFontLoaded(it) }.ifEmpty {
            log.error("No braille fonts found, using fallback {}", FALLBACK_FONT_NAME)
            listOf(SystemFont(FALLBACK_FONT_NAME, 12))
        }
        private val SELECTED_TEXT_FONT: LoadedFont = TEXT_FONTS[0]
        private lateinit var SELECTED_BRAILLE_FONT: LoadedFont
        private const val FONT_SIZE_DELTA = 1
        private const val MINIMUM_FONT_SIZE_DELTA = -7
        private const val MAXIMUM_FONT_SIZE_DELTA = 20

        init {
            refreshBrailleFont()
        }

        private fun refreshBrailleFont() {
            SELECTED_BRAILLE_FONT = if (isShowBraille) {
                BRAILLE_FONTS[0]
            } else {
                TEXT_FONTS[0]
            }
        }

        @JvmStatic
        fun newTextFont(): Font {
            return SELECTED_TEXT_FONT.newFont(SELECTED_TEXT_FONT.defaultHeight + sizeDelta)
        }

        @JvmStatic
        fun newBrailleFont(): Font {
            refreshBrailleFont()
            return SELECTED_BRAILLE_FONT.newFont(SELECTED_BRAILLE_FONT.defaultHeight + sizeDelta)
        }

        @JvmStatic
        fun increaseFontSetting() {
            adjustSizeDelta(FONT_SIZE_DELTA)
        }

        @JvmStatic
        fun decreaseFontSetting() {
            adjustSizeDelta(FONT_SIZE_DELTA * -1)
        }

        @JvmStatic
        fun copyViewFont(m: Manager, view: BBEditorView, gc: GC) {
            if (view === m.text) {
                gc.font = m.textView.font
            } else {
                gc.font = m.brailleView.font
            }
        }

        @JvmStatic
        val isShowBraille: Boolean
            get() = (BBIni.propertyFileManager.getProperty(SETTING_SHOW_BRAILLE, "true") == "true")

        private val sizeDelta: Int
            get() = BBIni.propertyFileManager.getPropertyAsInt(SETTING_SIZE_DELTA, 0)

        private fun adjustSizeDelta(additional: Int) {
            val newSize = BBIni.propertyFileManager.getPropertyAsInt(SETTING_SIZE_DELTA, 0) + additional
            if (newSize !in MINIMUM_FONT_SIZE_DELTA..MAXIMUM_FONT_SIZE_DELTA) {
                return
            }
            BBIni.propertyFileManager.saveAsInt(
                SETTING_SIZE_DELTA,
                newSize
            )
        }

        private fun fontExists(font: String?): Boolean {
            return sWTFonts
                .any { fontData: FontData ->
                    @Suppress("UsePropertyAccessSyntax")
                    fontData.getName() == font
                }
        }

        private val sWTFonts: List<FontData>
            get() = listOf(*Display.getCurrent().getFontList(null, true))
    }
}
