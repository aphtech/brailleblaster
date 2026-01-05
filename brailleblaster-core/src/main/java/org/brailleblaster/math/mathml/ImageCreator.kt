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
package org.brailleblaster.math.mathml

import net.sourceforge.jeuclid.MutableLayoutContext
import net.sourceforge.jeuclid.context.LayoutContextImpl
import net.sourceforge.jeuclid.context.Parameter
import nu.xom.Element
import org.brailleblaster.BBIni
import org.brailleblaster.math.ascii.ASCII2MathML.translateUseHTML
import org.brailleblaster.math.ascii.ASCIIMathEditorDialog
import org.eclipse.swt.SWT
import org.eclipse.swt.browser.Browser
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Group
import org.eclipse.swt.widgets.Shell
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Node
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.nio.file.FileSystems
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object ImageCreator {
    private val logger: Logger = LoggerFactory.getLogger(ImageCreator::class.java)
    private val MATHJAX_PATH_BEG =
        BBIni.bbDistPath.resolve(Paths.get("lib", "META-INF", "resources", "webjars", "MathJax")).toString()
    private const val MATHJAX_PATH_END = "MathJax.js?config=TeX-MML-AM_CHTML"
    private const val HTML_HEAD_BEG = "<!doctype html><html><head><script type='text/javascript' async src='"
    private const val HTML_HEAD_END = "'></script></head><body>"
    private const val HTML_TAIL = "</body></html>"
    private const val HTML_HEAD = "<!doctype html><html><head></head><body>"
    private val HTML_PLACEHOLDER = "<p>" + ASCIIMathEditorDialog.PLACEHOLDER + "</p>"

    // private static boolean internet_connection = false;
    fun createImage(d: Display?, e: Element, fontHeight: Int): Image? {
        val mr = MathRenderer
        val dbFactory = DocumentBuilderFactory.newInstance()
        try {
            val dBuilder = dbFactory.newDocumentBuilder()
            val n: Node = dBuilder.parse(InputSource(StringReader(e.toXML())))
            val params: MutableLayoutContext = LayoutContextImpl(LayoutContextImpl.getDefaultLayoutContext())
            params.setParameter(Parameter.MATHSIZE, fontHeight)
            val imageData = mr.render(n, params)
            return Image(d, imageData)
        } catch (e1: ParserConfigurationException) {
            logger.error("Parser Config Error", e1)
        } catch (e1: SAXException) {
            logger.error("Sax Error", e1)
        } catch (e1: IOException) {
            logger.error("IOException Error", e1)
        }

        return null
    }

    private val hTMLHead: String
        get() {
            val mj = File(MATHJAX_PATH_BEG)
            if (mj.exists()) {
                val files = mj.listFiles()
                if (files != null && files.isNotEmpty()) {
                    val s = files[0].name
                    val withVersion = File(mj, s)
                    if (withVersion.exists()) {
                        return (HTML_HEAD_BEG + MATHJAX_PATH_BEG + FileSystems.getDefault().separator + s + FileSystems.getDefault().separator
                                + MATHJAX_PATH_END + HTML_HEAD_END)
                    }
                }
            }
            return HTML_HEAD
        }

    private fun makeImage(shell: Shell, translate: nu.xom.Node, imageWidth: Int, imageHeight: Int): Image {
        var image = createImage(shell.display, translate as Element, 50)!!
        if (image.bounds.width > imageWidth) {
            image = Image(Display.getCurrent(), image.imageData.scaledTo(imageWidth, imageHeight))
        }
        val im = Image(Display.getCurrent(), imageWidth, imageHeight)
        val g = GC(im)
        g.drawImage(
            image, 0, 0, image.bounds.width, image.bounds.height, 0, 0, image.bounds.width,
            image.bounds.height
        )
        g.dispose()
        image = im
        return image
    }

    fun updateBrowser(browser: Browser?, mmlString: String) {
        if (mmlString.isEmpty()) {
            browser!!.text = HTML_HEAD + HTML_PLACEHOLDER + HTML_TAIL
        } else {
            val math = translateUseHTML(mmlString)
            val str = getHTMLString(math)
            if (browser != null && !browser.isDisposed) {
                browser.text = hTMLHead + str + HTML_TAIL
            }
        }
    }

    private fun getHTMLString(math: org.w3c.dom.Element?): String {
        val transFactory = TransformerFactory.newInstance()
        try {
            val transformer = transFactory.newTransformer()
            val buffer = StringWriter()
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            transformer.transform(DOMSource(math), StreamResult(buffer))
            return buffer.toString()
        } catch (e1: TransformerConfigurationException) {
            logger.error("Exception whilst creating transformer", e1)
            throw RuntimeException("Problem creating transformer when getting HTML string.", e1)
        } catch (e: TransformerException) {
            logger.error("Exception whilst transforming", e)
            throw RuntimeException("Problem transforming to HTML string.", e)
        }
    }

    private fun makeImageSpacing(imageWidth: Int, imageHeight: Int): Image {
        val im = Image(Display.getCurrent(), imageWidth, imageHeight)
        val g = GC(im)
        g.drawText(ASCIIMathEditorDialog.PLACEHOLDER, 0, 0)
        return im
    }

    fun makeBrowserView(
        rightPanel: Group?, mmlString: String, imageWidth: Int,
        imageHeight: Int
    ): Browser {
        // checkInternet();
        return Browser(rightPanel, SWT.NONE).apply {
            layoutData = GridData(SWT.FILL, SWT.FILL, true, true).apply {
                widthHint = imageWidth
                heightHint = imageHeight
            }
            javascriptEnabled = true
            text = HTML_HEAD + if (mmlString.isEmpty()) {
                HTML_PLACEHOLDER
            } else {
                val math = translateUseHTML(mmlString)
                getHTMLString(math)
            } + HTML_TAIL
        }
    }

}
