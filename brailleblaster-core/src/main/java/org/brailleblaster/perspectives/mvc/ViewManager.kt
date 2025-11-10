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
package org.brailleblaster.perspectives.mvc

import org.apache.commons.text.WordUtils
import org.brailleblaster.BBIni
import org.brailleblaster.abstractClasses.BBEditorView
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.views.style.StylePane
import org.brailleblaster.perspectives.braille.views.wp.BrailleView
import org.brailleblaster.perspectives.braille.views.wp.TextView
import org.brailleblaster.perspectives.mvc.events.BBViewListener
import org.brailleblaster.perspectives.mvc.modules.misc.ToggleViewsModule.Companion.loadSettings
import org.brailleblaster.perspectives.mvc.modules.misc.ToggleViewsModule.Views
import org.brailleblaster.util.ColorManager
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.CTabFolder
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import org.slf4j.LoggerFactory
import java.util.*

class ViewManager(folder: CTabFolder?, private val m: Manager) {
    private val views: MutableList<BBEditorView> = ArrayList()

    // ----------- Layout ------------------
    @JvmField
	val containerSash: SashForm = SashForm(folder, SWT.HORIZONTAL).apply {
        layout = FormLayout()
    }
    val textView: TextView = TextView(m, containerSash)
    val brailleView: BrailleView = BrailleView(m, containerSash)
    @JvmField
	val stylePane: StylePane = StylePane(containerSash, m)
    private var windowedViewCurrent: Views? = null

    init {
        reparent()

        // ------ Listeners -----------
        views.add(textView)
        views.add(brailleView)
        views.add(stylePane)

        // --------- Data -------------
//		ToggleViewsModule.setViewOrder(stylePane.widget, text.view, braille.view, this);
    }

    /**
     * Handle windowed views
     */
    fun reparent() {
        val windowedViewNew = windowedView
        var redrawMainContainer = false
        val visibleViews: List<Views> = loadSettings()
        val viewVisible = visibleViews.contains(windowedViewNew)
        log.info(
            "reparent current {} new {} visible {} {}",
            windowedViewCurrent,
            windowedViewNew,
            viewVisible,
            visibleViews
        )
        if (windowedViewCurrent != null) {
            val windowedEditor = getView(windowedViewCurrent!!)
            windowedEditor.view.setParent(containerSash)
            log.info("resetting container sash of $windowedEditor")
            redrawMainContainer = true
        }
        if (windowedShell != null && (windowedViewNew == null || !viewVisible)) {
            windowedShell!!.dispose()
            windowedShell = null
        }
        if (!viewVisible) {
            return
        }
        if (windowedViewNew != null) {
            if (windowedShell == null) {
                windowedShell = EasySWT.makeDialogFloating(m.wpManager.shell).apply {
                    layout = GridLayout(1, true)
                    open()
                }
            }
            val windowedEditor = getView(windowedViewNew)
            windowedEditor.view.setParent(windowedShell)
            EasySWT.setGridDataVertical(windowedEditor.view)
            windowedShell!!.layout(true)
            windowedShell!!.setRedraw(true)
            windowedShell!!.text =
                "${WordUtils.capitalizeFully(windowedViewNew.name)} View"
            redrawMainContainer = true
        }
        windowedViewCurrent = windowedViewNew
        if (redrawMainContainer) {
            containerSash.layout(true)
            containerSash.setRedraw(true)
        }
    }

    private fun getView(view: Views): BBEditorView {
        return if (view === Views.PRINT) {
            textView
        } else if (view === Views.BRAILLE) {
            brailleView
        } else if (view === Views.STYLE) {
            stylePane
        } else {
            throw UnsupportedOperationException("Unhandled $view")
        }
    }

    fun setViewSizes() {
        val viewOrder: MutableList<Views> = loadSettings()
        removeWindowedView(viewOrder)
        // Windowed view is in a different shell
        val pfm = BBIni.propertyFileManager
        val mainShellViewCount = mainShellViewCount()
        log.debug("mainShellViewCount {} actual {}", mainShellViewCount, containerSash.children)
        val weights = IntArray(mainShellViewCount)
        var weightCounter = 0
        val totalSize = DEFAULT_SIZE
        var styleSize = DEFAULT_STYLE_PANE_SIZE
        var printSize = DEFAULT_PRINT_SIZE
        var brailleSize = DEFAULT_BRAILLE_SIZE
        val style = viewOrder.contains(Views.STYLE)
        val braille = viewOrder.contains(Views.BRAILLE)
        val text = viewOrder.contains(Views.PRINT)
        if (style) {
            val weightStyle = pfm.getPropertyAsInt(SETTING_WEIGHT_STYLE, 0)
            if (weightStyle != 0) {
                styleSize = weightStyle
            }
            if (braille) {
                if (text) {
                    // style, braille, and text
                    val weightBraille = pfm.getPropertyAsInt(SETTING_WEIGHT_BRAILLE, 0)
                    if (weightBraille != 0) {
                        brailleSize = weightBraille
                    }
                    val weightPrint = pfm.getPropertyAsInt(SETTING_WEIGHT_PRINT, 0)
                    if (weightPrint != 0) {
                        printSize = weightPrint
                    }
                    if (printSize + brailleSize + styleSize > totalSize) {
                        // case where the user has selected sizes that exceed
                        // the bounds.
                        // we have decided to respect the style panel size and
                        // split the
                        // rest of the space evenly between the other two views.
                        // see RT 5096
                        printSize = (totalSize - styleSize) / 2
                        brailleSize = printSize
                    }
                } else {
                    // style, braille
                    if (styleSize in 1..<totalSize) {
                        brailleSize = totalSize - styleSize
                    } else {
                        // reset default
                        brailleSize = 800
                        styleSize = DEFAULT_STYLE_PANE_SIZE
                    }
                    printSize = 0
                }
            } else {
                if (text) {
                    // style, text
                    if (styleSize in 1..<totalSize) {
                        printSize = totalSize - styleSize
                    } else {
                        // reset default
                        printSize = 800
                        styleSize = DEFAULT_STYLE_PANE_SIZE
                    }
                } else {
                    // style
                    styleSize = DEFAULT_SIZE
                    printSize = 0
                }
                brailleSize = 0
            }
        } else if (braille) {
            if (text) {
                // braille, text
                val weightBraille = pfm.getPropertyAsInt(SETTING_WEIGHT_BRAILLE, 0)
                if (weightBraille != 0) {
                    brailleSize = weightBraille
                }
                val weightPrint = pfm.getPropertyAsInt(SETTING_WEIGHT_PRINT, 0)
                if (weightPrint != 0) {
                    printSize = weightPrint
                }
                if (printSize + brailleSize > totalSize) {
                    printSize = totalSize / 2
                    brailleSize = printSize
                }
            } else {
                // braille
                brailleSize = DEFAULT_SIZE
                printSize = 0
            }
            styleSize = 0
        } else {
            // text
            printSize = DEFAULT_SIZE
            styleSize = 0
            brailleSize = 0
        }
        for (curChild in containerSash.children) {
            if (curChild === stylePane.view) {
                weights[weightCounter++] = styleSize
            } else if (curChild === textView.view) {
                weights[weightCounter++] = printSize
            } else if (curChild === brailleView.view) {
                weights[weightCounter++] = brailleSize
            }
        }
        log.trace("Weightsarr: {} | Sizer {}", weights, weights.size)
        containerSash.setWeights(*weights)
    }

    /**
     * Configure tab key order. Need to call after changing instances
     */
    fun setTabList() {
        val viewOrder: MutableList<Views> = loadSettings()
        removeWindowedView(viewOrder)
        val tabList = arrayOfNulls<Control>(viewOrder.size)
        if (viewOrder.contains(Views.STYLE)) {
            tabList[viewOrder.indexOf(Views.STYLE)] = stylePane.view
        }
        if (viewOrder.contains(Views.PRINT)) {
            tabList[viewOrder.indexOf(Views.PRINT)] = textView.view
        }
        if (viewOrder.contains(Views.BRAILLE)) {
            tabList[viewOrder.indexOf(Views.BRAILLE)] = brailleView.view
        }
        containerSash.tabList = tabList
    }

    fun saveScreenProperties() {
        val pfm = BBIni.propertyFileManager
        val weights = containerSash.weights
        var weightCounter = 0

        // if (containerSash.getMaximizedControl() == null){
        // Only save if not 0. The weight will be set to
        // 0 if the view is not in the active views. This
        // will keep the user's previous preference in this value.
        for (curChild in containerSash.children) {
            if (weights.size > weightCounter) {
                if (weights[weightCounter] != 0
                    && views.any { curView: BBEditorView -> curView.view === curChild }
                ) {
                    weightCounter++
                    continue
                } else if (curChild === stylePane.view) {
                    val weight = weights[weightCounter++]
                    if (weight != 0) {
                        pfm.saveAsInt(SETTING_WEIGHT_STYLE, weight)
                    }
                } else if (curChild === textView.view) {
                    val weight = weights[weightCounter++]
                    if (weight != 0) {
                        pfm.saveAsInt(SETTING_WEIGHT_PRINT, weight)
                    }
                } else if (curChild === brailleView.view) {
                    val weight = weights[weightCounter++]
                    if (weight != 0) {
                        pfm.saveAsInt(SETTING_WEIGHT_BRAILLE, weight)
                    }
                }
            } else {
                break
            }
        }
        // }

        // if (containerSash.getMaximizedControl() != null) {
        // if (containerSash.getMaximizedControl().equals(text.view))
        // pfm.save("editorView", text.getClass().getCanonicalName());
        // else if (containerSash.getMaximizedControl().equals(braille.view))
        // pfm.save("editorView", braille.getClass().getCanonicalName());
        // } else
        // pfm.save("editorView", "");
    }

    fun initializeListeners() {
        for (view in views) {
            view.initializeListeners()
        }
    }

    fun removeListeners() {
        for (view in views) {
            view.removeListeners()
        }
    }

    fun initializeModuleListeners(view: BBEditorView?) {
        for (curListener in m.simpleManager.listeners) {
            if (curListener is BBViewListener) {
                (curListener as BBViewListener).initializeListener(view!!)
            }
        }
    }

    fun removeModuleListeners(view: BBEditorView) {
        for (curListener in m.simpleManager.listeners) {
            if (curListener is BBViewListener) {
                (curListener as BBViewListener).removeListener(view)
            }
        }
    }

    /**
     * Number of views that are a child of the main shell. Windowed views are
     * not, except when hidden
     * @return
     */
    fun mainShellViewCount(): Int {
        var count = 0
        for (child in containerSash.children) {
            if (child is StyledText) {
                count++
            }
        }
        return count
    }

    companion object {
        private val log = LoggerFactory.getLogger(ViewManager::class.java)
        private const val DEFAULT_SIZE = 1000
        private const val DEFAULT_STYLE_PANE_SIZE = 100
        private const val DEFAULT_BRAILLE_SIZE = 450
        private const val DEFAULT_PRINT_SIZE = 450
        private const val SETTING_WEIGHT_STYLE = "viewManager.weightStyle"
        private const val SETTING_WEIGHT_PRINT = "viewManager.weightPrint"
        private const val SETTING_WEIGHT_BRAILLE = "viewManager.weightBraille"
        const val SETTING_DARK_THEME = "viewManager.darkTheme"
        private const val SETTING_WINDOWED_VIEW = "viewManager.windowedView"
        private var windowedShell: Shell? = null
        @JvmStatic
		    fun colorizeCompositeRecursive(composite: Composite) {
            if (!isDarkMode) {
                return
            }
            log.info("colorize $composite")
            composite.background = ColorManager.getColor("1e1f1c", composite)
            for (control in composite.children) {
                if (control is Composite) {
                    colorizeCompositeRecursive(control)
                } else {
                    colorizeControl(control)
                }
            }
        }

        fun colorizeControl(control: Control) {
            if (!isDarkMode) {
                return
            }
            log.info("colorize control $control")
            if (control is StyledText) {
                // a monitor doesn't have to be a white sheet of paper
                control.setBackground(ColorManager.getColor("272822", control))
                control.setForeground(ColorManager.getColor("f8f8f2", control))
            }
        }

        @JvmStatic
		    fun colorizeToolbarHolder(composite: Composite) {
            if (true) {
                return
            }
            if (BBIni.propertyFileManager.getProperty(SETTING_DARK_THEME, "") == "true") {
                composite.background = ColorManager.getColor("1e1f1c", composite)
            }
        }

        @JvmStatic
		    fun colorizeIconToolbars(bar: ToolBar) {
            if (true) {
                return
            }
            if (BBIni.propertyFileManager.getProperty(SETTING_DARK_THEME, "") == "true") {
                bar.background = ColorManager.getColor("1e1f1c", bar)
            }
        }

        @JvmStatic
		    fun colorizeCustomToolbars(composite: Composite) {
            if (true) {
                return
            }
            if (BBIni.propertyFileManager.getProperty(SETTING_DARK_THEME, "") == "true") {
                composite.background = ColorManager.getColor("1e1f1c", composite)
            }
        }

        val isDarkMode: Boolean
            get() = BBIni.propertyFileManager.getPropertyAsBoolean(SETTING_DARK_THEME, false)
        @JvmStatic
		    var windowedView: Views?
            get() = BBIni.propertyFileManager.getPropertyAsEnumOrNull<Views>(SETTING_WINDOWED_VIEW)
            set(view) {
                if (view == null) {
                    BBIni.propertyFileManager.removeProperty(SETTING_WINDOWED_VIEW)
                } else {
                    BBIni.propertyFileManager.saveAsEnum(SETTING_WINDOWED_VIEW, view)
                }
            }

        fun removeWindowedView(views: MutableList<Views>) {
            val windowedView = windowedView
            if (windowedView != null) {
                views.remove(windowedView)
            }
        }
    }
}
