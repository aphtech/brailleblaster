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

import nu.xom.Document
import nu.xom.Node
import nu.xom.Nodes
import org.brailleblaster.utd.config.ShortcutDefinitions
import org.brailleblaster.utd.config.StyleDefinitions
import org.mwhapples.jlouis.Louis

/**
 * Interface for all translation context classes to implement. This will contain
 * things like the configuration, the semantic actions and so on.
 */
interface ITranslationEngine {
    /**
     * The actionMap for the translation context.
     *
     * The action map is the mapping of matchers to actions and determines which
     * actions should be applied to which nodes.
     *
     */
    var actionMap: IActionMap
    var styleMap: IStyleMap
    var isTestMode: Boolean
    var brailleSettings: BrailleSettings
    var pageSettings: PageSettings
    fun translate(doc: Node): Nodes
    fun format(nodes: Node): Document
    val brailleTranslator: Louis

    /**
     * Translate and format the complete document.
     *
     * This method is a helper method which will take the input document and
     * perform both Braille translation and format the resulting document. This
     * method is the equivalent to calling translateDocument and then passing
     * the result to format.
     *
     * @param doc
     * The document to be translated and formatted.
     * @return The translated and formatted document.
     */
    fun translateAndFormatDocument(doc: Document): Document

    /**
     * Translate the input document into Braille.
     *
     * This method will perform the translation step of converting the document
     * into Braille but will not perform formatting. In general translation is
     * considered where content is changed into Braille, where as formatting is
     * only inserting elements which affect the layout.
     *
     * @param doc
     * The document to translate.
     * @return The translated document without formatting.
     */
    fun translateDocument(doc: Document): Document

    /**
     * Find the nearest translation block node.
     *
     * A translation block is a node which can be translated into Braille
     * without any context from the document. The search should look for the
     * nearest ancester of the input node which could be considered a
     * translation block.
     *
     * @param inputNode
     * The node to start searching from.
     * @return The node which is the nearest translation block to the input
     * node. This might even be the input node if the input node is a
     * translation block.
     */
    fun findTranslationBlock(inputNode: Node): Node
	  var styleDefinitions: StyleDefinitions
    fun getStyle(node: Node): IStyle?
    val tableID: String
    /**
     * The UTDTranslationEngineCallback that is to recieve notifications. Always use this method as the object in the translation engine is a WeakReference.
     */
    var callback: UTDTranslationEngineCallback
	  var shortcutDefinitions: ShortcutDefinitions
    fun toBRF(
        utdDocument: Document,
        ocs: BRFWriter.OutputCharStream,
        opts: Int,
        outputPageListener: BRFWriter.PageListener,
        convertToBrfChars: Boolean = false
    )
}
