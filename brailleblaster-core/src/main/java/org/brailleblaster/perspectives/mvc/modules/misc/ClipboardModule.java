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
package org.brailleblaster.perspectives.mvc.modules.misc;

import nu.xom.*;
import org.brailleblaster.BBIni;
import org.brailleblaster.archiver2.TextArchiveLoader;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.bbx.BBXUtils;
import org.brailleblaster.bbx.fixers.NodeTreeSplitter;
import org.brailleblaster.bbx.fixers2.LiveFixer;
import org.brailleblaster.math.mathml.MathModule;
import org.brailleblaster.math.mathml.MathSubject;
import org.brailleblaster.perspectives.braille.Manager;
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement;
import org.brailleblaster.perspectives.braille.mapping.elements.WhiteSpaceElement;
import org.brailleblaster.perspectives.braille.mapping.interfaces.Uneditable;
import org.brailleblaster.perspectives.braille.messages.Sender;
import org.brailleblaster.perspectives.braille.stylers.WhitespaceTransformer;
import org.brailleblaster.perspectives.braille.views.wp.MathEditHandler;
import org.brailleblaster.perspectives.braille.views.wp.TextVerifyKeyListener;
import org.brailleblaster.perspectives.mvc.*;
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener;
import org.brailleblaster.perspectives.mvc.XMLNodeCaret.CursorPosition;
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent;
import org.brailleblaster.perspectives.mvc.events.ModifyEvent;
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent;
import org.brailleblaster.perspectives.mvc.menu.MenuManager;
import org.brailleblaster.tools.CopyTool;
import org.brailleblaster.tools.CopyUnicodeBrailleTool;
import org.brailleblaster.tools.CutTool;
import org.brailleblaster.tools.PasteTool;
import org.brailleblaster.tools.PasteAsMathTool;
import org.brailleblaster.utd.internal.xml.FastXPath;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.brailleblaster.utd.properties.EmphasisType;
import org.brailleblaster.utd.properties.UTDElements;
import org.brailleblaster.utils.braille.BrailleUnicodeConverter;
import org.brailleblaster.utd.utils.TableUtils;
import org.brailleblaster.utd.utils.UTDHelper;
import org.brailleblaster.util.Utils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class ClipboardModule implements SimpleListener {
    private static final Logger log = LoggerFactory.getLogger(ClipboardModule.class);
    private final List<Clip> clips;
    private String lastCopiedString;
    private final BBSimpleManager manager;
    public final Paste paste;

    public ClipboardModule(BBSimpleManager manager) {
        clips = new ArrayList<>();
        paste = new Paste();
        this.manager = manager;
    }

    @Override
    public void onEvent(@NotNull SimpleEvent event) {
        if (event instanceof BuildMenuEvent) {
            MenuManager.INSTANCE.add(new CutTool(this));
            MenuManager.INSTANCE.add(new CopyTool(this));
            MenuManager.INSTANCE.add(new CopyUnicodeBrailleTool(this));
            MenuManager.INSTANCE.add(new PasteTool(this));
            MenuManager.INSTANCE.add(new PasteAsMathTool(this));
        }
    }

    public void copy(@NotNull Manager manager) {
        copy(manager, false);
    }

    public void copy(@NotNull Manager manager, boolean returnUnicodeBraille) {
        XMLSelection sel = manager.getSimpleManager().getCurrentSelection();
        boolean isBrailleView = manager.getBrailleView().isTextSelected();
        boolean isTextView = manager.getTextView().isTextSelected();
        String textViewSelection = manager.getTextView().getSelectionText();
        String brailleViewSelection = manager.getBraille().getView().getSelectionText();

        //Sanity checks
        if (textViewSelection.isEmpty() && isTextView)
            return;
        else if (brailleViewSelection.isEmpty() && isBrailleView)
            return;

        clips.clear();

        //System.out.println("Selected text: " + sel.toString());
        //Stupid, simple solution is just to copy the ascii braille to the clipboard
        //Things like tables might throw it off, but I could be wrong about that - MNS
        if (isBrailleView) {
            if (returnUnicodeBraille){
                brailleViewSelection = BrailleUnicodeConverter.INSTANCE.asciiToUnicodeLouis(brailleViewSelection);
            }
            Clipboard cb = new Clipboard(Display.getCurrent());
            TextTransfer tt = TextTransfer.getInstance();
            cb.setContents(new Object[]{brailleViewSelection}, new Transfer[]{tt});
            return;
        }

        XMLNodeCaret start = sel.start;
        XMLNodeCaret end = sel.end;
        if (sel.isSingleNode()) {
            // Convert anything that isn't a container or section into a block
            if (!(BBX.CONTAINER.isA(start.getNode()) || BBX.SECTION.isA(start.getNode()))) {
                Element block = BBXUtils.findBlock(start.getNode());
                if (BBX.BLOCK.LIST_ITEM.isA(block)) {
                    addSimpleListItemToClipboard(block, start.getNode(), end.getNode(), sel);
                } else if (BBX.BLOCK.SPATIAL_MATH.isA(block)) {
                    Node node = XMLHandler.ancestorVisitorElement(block, BBX.CONTAINER::isA);
                    if (node == null) {
                        log.error("Spatial Math block is not in a Spatial math container");
                        return;
                    }
                    clips.add(new Clip(node));
                } else {
                    Node node;
                    if (start instanceof XMLTextCaret) {
                        // Trim the text down to only be what is selected
                        String textValue = start.getNode().getValue().substring(((XMLTextCaret) start).getOffset(),
                                ((XMLTextCaret) end).getOffset());
                        Node inlineParent = XMLHandler.ancestorVisitor(start.getNode(), BBX.INLINE::isA);
                        // If text is descended from an emphasis, take the emphasis with it
                        if (inlineParent != null) {
                            node = inlineParent.copy();
                            ((Element) node).removeChildren();
                            ((Element) node).appendChild(textValue);
                        } else {
                            node = new Text(textValue);
                        }
                    } else if (MathModule.isMath(start.getNode())) {
                        // will get the inline.mathml element
                        node = MathModule.makeMathFromSelection(manager);
                    } else {
                        node = start.getNode().copy();
                    }
                    if (!BBX.BLOCK.isA(node)) {
                        block = block.copy();
                        block.removeChildren();
                        block.appendChild(node);
                        node = block;
                    }
                    clips.add(new Clip(Objects.requireNonNull(node)));
                }
            } else {
                // If it's a container or a section, we don't need to do
                // anything but just copy it
                clips.add(new Clip(start.getNode().copy()));
            }
        } else {
            Node startNode = start.getNode();
            Node endNode = end.getNode();

            Node startElement = startNode;
            Node endElement = endNode;
            if (XMLHandler.ancestorElementIs(startElement, BBX.BLOCK.SPATIAL_MATH::isA)) {
                startElement = XMLHandler.ancestorVisitorElement(startElement, BBX.CONTAINER::isA);
                if (startElement == null) {
                    log.error("Spatial Math block does not start in a Spatial math container");
                    return;
                }
            }
            if (XMLHandler.ancestorElementIs(endElement, BBX.BLOCK.SPATIAL_MATH::isA)) {
                endElement = XMLHandler.ancestorVisitorElement(endElement, BBX.CONTAINER::isA);
                if (endElement == null) {
                    log.error("Spatial Math block does not end in a Spatial math container");
                    return;
                }
            }
            // If either element is text, inline, or span, convert it to its parent block
            if (!BBX.CONTAINER.isA(startElement) && !BBX.SECTION.isA(startElement) && !BBX.BLOCK.isA(startElement)) {
                startElement = BBXUtils.findBlock(startNode);
            }
            if (!BBX.CONTAINER.isA(endElement) && !BBX.SECTION.isA(endElement) && !BBX.BLOCK.isA(endElement)) {
                endElement = BBXUtils.findBlock(endNode);
            }

            if (startElement == endElement) { // Text is selected. Parent must be blocks
                addNodeToClipboard(startElement, startNode, endNode, sel);
            } else {
                // Keep track of the sibling of startElement (or startElement's parent)
                Node curNode = XMLHandler.followingNode(startElement);

                // Add the initially selected block
                addNodeToClipboard(startElement, startNode, endNode, sel);

                // Loop through following nodes until endElement is reached
                while (curNode != null) {
                    if (curNode == endNode || curNode == endElement)
                        break;
                    // If the node is an ancestor of endElement, start looping through children
                    if (FastXPath.descendant(curNode).list().contains(endElement)) {
                        curNode = curNode.getChild(0);
                        continue;
                    }
                    addNodeToClipboard(curNode, startNode, endNode, sel);
                    curNode = XMLHandler.followingNode(curNode);
                }

                // Add the final selected block
                addNodeToClipboard(endElement, startNode, endNode, sel);
            }
        }
        // Tests will fail when attempting to use the system clipboard
        if (!BBIni.getDebugging()) {
            lastCopiedString = convertBBXClipsToSystemClipboard(clips);
        }
    }

    public static String convertBBXClipsToSystemClipboard(List<Clip> clips) {
        StringBuilder systemCB = new StringBuilder();
        clips.forEach(c -> {
            Node copy = c.getNode().copy();
            if (copy instanceof ParentNode)
                UTDHelper.stripUTDRecursive((ParentNode) copy);
            if (copy instanceof Element && (BBX.SECTION.isA(copy) || BBX.CONTAINER.isA(copy))) {
//				systemCB.append(getBlocksTextInSection((Element) copy).trim()); ticket 7627
                systemCB.append(getBlocksTextInSection((Element) copy));
            } else {
                systemCB.append(copy.getValue());
            }
            if (clips.indexOf(c) != clips.size() - 1)
                systemCB.append(System.lineSeparator());
        });
        TextTransfer textTransfer = TextTransfer.getInstance();
        Clipboard clipboard = new Clipboard(Display.getCurrent());
        clipboard.clearContents();
        if (!systemCB.isEmpty()) {
            clipboard.setContents(new String[]{systemCB.toString()}, new Transfer[]{textTransfer});
        } else {
            // SWT cannot setContents to be an empty string, because you're
            // expected to use clearContents.
            // However, clearContents can only clear the contents of the
            // clipboard if it was the one who
            // put it there. If the user copied text from another application,
            // clearContents does nothing.
            // So to override the clipboard, first we override the system
            // clipboard, then clearContents
            // works, since we were the last ones to touch the clipboard.
            clipboard.setContents(new String[]{" "}, new Transfer[]{textTransfer});
            clipboard.clearContents();
        }
        clipboard.dispose();
        return systemCB.toString();
    }

    public void convertSystemClipboardToBBXClips() {
        Clipboard cb = new Clipboard(Display.getCurrent());
        TextTransfer tt = TextTransfer.getInstance();
        String cbString = (String) cb.getContents(tt);
        if (cbString != null) {
            // If it was copied from outside of BrailleBlaster, or BB's
            // clipboard is empty,
            // paste that instead of what's inside BrailleBlaster's clipboard
            if (lastCopiedString == null || !lastCopiedString.equals(cbString)) {
                // Convert it to BBX
                clips.clear();
                String[] blocks = cbString.split(System.lineSeparator());
                for (String block : blocks) {
                    Element newBlock = BBX.BLOCK.DEFAULT.create();

                    block = block.replaceAll("\t", " ");
                    Text text = TextArchiveLoader.getUsableText(block);
                    newBlock.appendChild(text);

                    clips.add(new Clip(newBlock));
                }
            }
        }
    }

    public void cut(@NotNull Manager manager) {
        copy(manager);
        if (manager.getTextView().getSelectionText().isEmpty())
            return;
        if (MathModule.selectionContainsMath(manager)) {
            log.error("Cutting math");
        }

        // TODO: Should be false? See UndoEditTest.fullCut
        deleteTextViewSelection(manager, false);
    }

    private void logPasteVariables(
        boolean isStartText, boolean isStartMath, boolean inBlock,
        boolean inSection, boolean inList, boolean multipleClips
    ) {
        log.debug("""
                        
                         isStartText {}
                         isStartMath {}
                         inBlock {}
                         inSection {}
                         inList {}
                         multipleClips {}
                         first clip {}\
                        """,
            isStartText, isStartMath, inBlock,
            inSection, inList, multipleClips, (!clips.isEmpty()) ? clips.get(0).toString() : "");
    }

    public void pasteAsMath(@NotNull Manager manager){
        paste(manager, true);
    }

    public void paste(@NotNull Manager manager) {
        paste(manager, false);
    }

    public void paste(@NotNull Manager manager, boolean isMath) {
        manager.getText().update(false);
        manager.stopFormatting();
        XMLSelection sel = manager.getSimpleManager().getCurrentSelection();
        // Make sure we're not in a read only element
        TextMapElement tme = manager.getMapList().findNode(sel.start.getNode());
        if (tme instanceof Uneditable && sel.start.getCursorPosition() == CursorPosition.ALL) {
            ((Uneditable) tme).blockEdit(manager);
            return;
        }
        //also check for cursor being in an uneditable TME that has no corresponding node
        if (manager.getMapList().getCurrent() instanceof Uneditable) {
            ((Uneditable) manager.getMapList().getCurrent()).blockEdit(manager);
            return;
        }
        // Wrap to prevent 2 undo events
        ModifyEvent.cannotUndoNextEvent();
        // Overwrite
        deleteTextViewSelection(manager, true);
        ModifyEvent.resetUndoable();
        // references changed
        sel = manager.getSimpleManager().getCurrentSelection();
        TextMapElement currentElement = manager.getMapList().getCurrent();
        // Is there something in the system clipboard?
        if (!BBIni.getDebugging()) { // Tests will fail when attempting to access
            // the clipboard
            convertSystemClipboardToBBXClips();
        }
        if (clips.isEmpty()) {
            return;
        }

        if (isMath){
            //I still don't believe it's this simple...there has to be a catch
            MathEditHandler.INSTANCE.insertNew(new MathSubject(clips.get(0).getNode()));
            return;
        }

        Node startNode = sel.start.getNode();
        List<Node> changedNodes = new ArrayList<>();

        // Is the cursor in the middle of a block?
        boolean isStartText = sel.start instanceof XMLTextCaret;
        boolean isStartMath = MathModule.isMath(sel.start.getNode());
        boolean cursorInText = isStartText || isStartMath;
        boolean inBlock = sel.start.getCursorPosition() == CursorPosition.ALL;
        boolean inSection = BBX.SECTION.isA(sel.start.getNode());
        boolean inList = BBX.CONTAINER.LIST.isA(sel.start.getNode()) || BBX.BLOCK.LIST_ITEM.isA(sel.start.getNode());
        boolean multipleClips = clips.size() > 1;
        logPasteVariables(isStartText, isStartMath, inBlock, inSection, inList, multipleClips);

        if (inSection) {
            // TODO
            return;
        }
        if (BBX.BLOCK.SPATIAL_MATH.isA(clips.get(0).getNode())) {
            return;
            //Spatial math blocks should always be attached to a container
        }
        if (cursorInText && inBlock && clips.size() == 1 && BBX.CONTAINER.LIST.isA(clips.get(0).getNode())
                && BBX.BLOCK.LIST_ITEM.isA(clips.get(0).getNode().getChild(0))
                && !BBX.BLOCK.LIST_ITEM.isA(BBXUtils.findBlock(sel.start.getNode()))) {
            changedNodes.addAll(insertListInMiddleBlockText(isStartText, startNode, isStartMath, changedNodes, sel));
        }
        else if (isStartText && inBlock && !multipleClips) {
            Node newEndNode;
            Text text = (Text) startNode;
            String startText = text.getValue().substring(0, ((XMLTextCaret) sel.start).getOffset());
            int pasteOffset = ((XMLTextCaret) sel.start).getOffset();
            String endText = text.getValue().substring(((XMLTextCaret) sel.start).getOffset());
            text.setValue(startText);
            newEndNode = new Text(endText);
            ParentNode parent = startNode.getParent();
            int index = parent.indexOf(startNode) + 1;
            if (BBX.BLOCK.isA(clips.get(0).getNode())) {
                for (int i = 0; i < clips.get(0).getNode().getChildCount(); i++) {
                    Node child = clips.get(0).getNode().getChild(i).copy();
                    pasteOffset += child.getValue().length();
                    index = addNodeToParent(parent, child, index);
                    paste.recordPaste(parent.getChild(index - 1), pasteOffset);
                }
            } else {
                Node newChild = clips.get(0).getNode().copy();
                if (BBX.CONTAINER.LIST.isA(clips.get(0).getNode())) {
                    if (newChild.getChildCount() == 1) {
                        //Do this to preserve emphasis
                        newChild = BBXUtils.INSTANCE.findBlockChild((Element) newChild).getChild(0).copy();
                        //newChild = UTDHelper.getFirstTextDescendant((Element) newChild);
                        index = addNodeToParent(parent, newChild, index);
                        paste.recordPaste(parent.getChild(index - 1), 0);
                    } else if (newChild.getChildCount() > 1) {
                        newChild = BBXUtils.INSTANCE.findBlockChild((Element) newChild).getChild(0).copy();
                        index = addNodeToParent(parent, newChild, index);
                        paste.recordPaste(parent.getChild(index - 1), 0);
                        ParentNode blockParent = parent.getParent();
                        index = blockParent.indexOf(parent) + 1;
                        parent = blockParent;
                        for (int i = 1; i < clips.get(0).getNode().getChildCount(); i++) {
                            Node child = clips.get(0).getNode().getChild(i).copy();
                            index = addNodeToParent(parent, child, index);
                            paste.recordPaste(parent.getChild(index - 1), 0);
                        }
                    }
                }
            }
            if (!newEndNode.getValue().trim().isEmpty())
                addNodeToParent(parent, newEndNode, index);
            changedNodes.add(parent);
        } else if (isStartMath && inBlock && !multipleClips) {
            MathEditHandler.INSTANCE.insertNew(new MathSubject(clips.get(0).getNode()));
        }
        // Are there multiple blocks in the clipboard?
        else if (cursorInText && inBlock && multipleClips) {
            // Split the existing block into two
            Element block = BBXUtils.findBlock(startNode);
            Element blockCopy = copyBlock(startNode, null, block, sel, true);
            // Put the text of the first block into the existing first block
            if (!clips.get(0).getNode().getValue().trim().isEmpty()) {
                if (BBX.BLOCK.isA(clips.get(0).getNode())) {
                    for (int i = 0; i < clips.get(0).getNode().getChildCount(); i++) {
                        addNodeToParent(block, clips.get(0).getNode().getChild(i).copy(), block.getChildCount());
                        paste.recordPaste(getFinalTextChild(block, true), 0);
                    }
                } else {
                    block.appendChild(clips.get(0).getNode());
                }
            }
            // Put the remaining blocks in between the two blocks
            ParentNode blockParent = block.getParent();
            int blockIndex = blockParent.indexOf(block) + 1;
            for (int i = 1; i < clips.size(); i++) {
                Node newChild = clips.get(i).getNode().copy();
                if (newChild.getValue().trim().isEmpty()) {
                    continue;
                }
                blockParent.insertChild(newChild, blockIndex);
                blockIndex++;
                paste.recordPaste(newChild, 0);
            }
            blockParent.insertChild(blockCopy, blockIndex);
            changedNodes.add(blockParent);
        }
        // Is the cursor outside a block?
        else if (!inBlock) {
            if (BBX.INLINE.isA(startNode) || BBX.SPAN.isA(startNode) || cursorInText) {
                startNode = BBXUtils.findBlock(startNode);
            }
            // Loop through clipboard
            ParentNode startNodeParent = startNode.getParent();
            int index = startNodeParent.indexOf(startNode)
                    + (sel.start.getCursorPosition() == CursorPosition.BEFORE ? 0 : 1);
            if (index < startNodeParent.getChildCount() && BBX.CONTAINER.TABLE.isA(startNodeParent.getChild(index))
                    && TableUtils.isTableCopy((Element) startNodeParent.getChild(index))) {
                index++;
            }
            if (BBX.BLOCK.LIST_ITEM.isA(startNode))
                inList = true;
            if (BBX.CONTAINER.LIST.isA(startNodeParent) && !inList) {
                startNode = startNodeParent;
                startNodeParent = startNode.getParent();
                index = startNodeParent.indexOf(startNode)
                        + (sel.start.getCursorPosition() == CursorPosition.BEFORE ? 0 : 1);
            }
            for (int i = 0; i < clips.size(); i++) {
                Node listParent = XMLHandler.ancestorVisitor(startNode, BBX.CONTAINER.LIST::isA);
                Node clipboardNode = clips.get(i).getNode();
                if (clipboardNode.getValue().trim().isEmpty()) {
                    continue;
                }
                // Is the cursor inside a list container? Is the current clip a
                // list container?
                if (inList && BBX.CONTAINER.LIST.isA(clipboardNode)) {
                    normalizeList(listParent, clipboardNode, currentElement, index, startNodeParent, i);
                }
                // Otherwise, just insert the item
                else if (currentElement instanceof WhiteSpaceElement && i == 0 && !BBX.CONTAINER.LIST.isA(startNode)) {
                    WhitespaceTransformer wt = new WhitespaceTransformer(manager);
                    Node copyNode = clipboardNode.copy();
                    wt.transformWhiteSpace((WhiteSpaceElement) currentElement, copyNode);
                    index = startNodeParent.indexOf(copyNode);
                    if (index == -1) {
                        /*There is no guarantee that transform whitespace will insert the node as a child of the current element's parent*/
                        startNodeParent = copyNode.getParent();
                        index = startNodeParent.indexOf(copyNode);
                    }
                    paste.recordPaste(copyNode, 0);
                    index++;
                } else {
                    Node copyNode = clipboardNode.copy();
                    startNodeParent.insertChild(copyNode, index);
                    paste.recordPaste(copyNode, 0);
                    index++;
                }

            }
            changedNodes.add(startNodeParent);
        } // end cursor is outside of block

        if (changedNodes.isEmpty())
            return;

        // Refer to ticket 6177
        // We have to stop formatting again here.
        // I don't understand why.
        // UTD throws a nonsensical exception if we don't, even though the
        // formatting thread has been stopped.
        // This code is a nightmare.
        manager.stopFormatting();

        // Normalize nested emphasis
        changedNodes.forEach(n -> {
            if (n instanceof ParentNode)
                UTDHelper.stripUTDRecursive((ParentNode) n);
            normalizeEmphasis(n);
        });

        Node[] changedNodesArray = new Node[changedNodes.size()];
        changedNodesArray = changedNodes.toArray(changedNodesArray);
        manager.stopFormatting();
        manager.getSimpleManager().dispatchEvent(new ModifyEvent(Sender.NO_SENDER, true, changedNodesArray));

        // Move cursor to last pasted text
        Node pasteNode = paste.getNode();
        if (pasteNode != null && pasteNode.getDocument() != null) {
            if (pasteNode instanceof Text) {
                manager.getSimpleManager().dispatchEvent(
                        new XMLCaretEvent(Sender.BRAILLE, new XMLTextCaret((Text) pasteNode, paste.getOffset())));
            } else {
                manager.getSimpleManager().dispatchEvent(new XMLCaretEvent(Sender.BRAILLE, new XMLNodeCaret(pasteNode)));
                //RT 7767
                List<TextMapElement> findNodeText = manager.findNodeText(pasteNode);
                int offset = findNodeText.get(0).getEnd(manager.getMapList());
                manager.setTextCaret(offset);
            }
        }
    }

    private List<Node> insertListInMiddleBlockText(
        boolean isStartText, Node startNode, boolean isStartMath, List<Node> changedNodes, XMLSelection sel
    ) {
        Element listItem = (Element) clips.get(0).getNode();
        clips.remove(0);
        clips.add(new Clip(listItem));
        String endText = "";

        if (isStartText) {
            Text text = (Text) startNode;
            endText = text.getValue().substring(((XMLTextCaret) sel.start).getOffset());
        } else if (isStartMath) {
            endText = MathModule.getMathText(startNode)
                    .substring(Math.min(manager.getManager().getTextView().getCaretOffset() - manager.getManager().getMapList()
                            .getCurrent().getStart(manager.getManager().getMapList()), MathModule
                            .getMathText(startNode).length()));
        }

        if (!endText.isEmpty() && !endText.contentEquals(startNode.getValue())) {
            manager.getManager().splitElement();
        }

        ParentNode parent = BBXUtils.findBlock(startNode).getParent();
        int index = !endText.isEmpty() && endText.contentEquals(startNode.getValue()) ? parent.indexOf(BBXUtils.findBlock(startNode)) : parent.indexOf(BBXUtils.findBlock(startNode)) + 1;

        if (BBX.CONTAINER.LIST.isA(clips.get(0).getNode())) {
            for (int i = 0; i < clips.get(0).getNode().getChildCount(); i++) {
                Node newChild = clips.get(0).getNode().getChild(i).copy();
                index = addNodeToParent(parent, newChild, index);
                paste.recordPaste(parent.getChild(index - 1), 0);
            }
        } else {
            Node newChild = clips.get(0).getNode().copy();
            index = addNodeToParent(parent, newChild, index);
            paste.recordPaste(parent.getChild(index - 1), 0);
        }

        changedNodes.add(parent);
        //clips.remove(0);
        return changedNodes;
    }

    public void normalizeList(
        Node listParent, Node clipboardNode, TextMapElement currentElement,
        int index, ParentNode startNodeParent, int i
    ) {
        // If the parent list has a smaller list level, change its
        // list level to equal
        // the clipboard list's level
        int parentLevel = BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.get((Element) listParent);
        int clipboardLevel = BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.get((Element) clipboardNode);
        if (parentLevel < clipboardLevel) {
            BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.set((Element) listParent, clipboardLevel);
        }

        // Throw out the list container in the clip and attach
        // children
        for (int child = 0; child < clipboardNode.getChildCount(); child++) {
            if (clipboardNode.getChild(child).getValue().trim().isEmpty()) {
                continue;
            }
            if (currentElement instanceof WhiteSpaceElement && i == 0 && child == 0) {
                // TODO: Copy/pasted from below
                WhitespaceTransformer wt = new WhitespaceTransformer(manager.getManager());
                Node copyNode = clipboardNode.getChild(child).copy();
                wt.transformWhiteSpace((WhiteSpaceElement) currentElement, copyNode);
                index = startNodeParent.indexOf(copyNode);
                if (index == -1) {
                    // I'm not sure how this could come up
                    System.out.println("An error occurred when pasting");
                    continue;
                }
                paste.recordPaste(copyNode, 0);
            } else {
                Node newChild = clipboardNode.getChild(child).copy();
                startNodeParent.insertChild(newChild, index);
                paste.recordPaste(newChild, 0);
            }
            index++;
        }
    }

    /**
     * If inserting a text node, normalizes text node with surrounding text
     * nodes and handle emphasis
     *
     * @return index + 1 if no normalization happened, index if new node was
     * normalized
     */
    private int addNodeToParent(ParentNode parent, Node node, int index) {
        if (node instanceof Text && index > 0 && parent.getChild(index - 1) instanceof Text sibling) {
            sibling.setValue(sibling.getValue() + node.getValue());
        } else {
            parent.insertChild(node, index);
            index++;
        }
        return index;
    }

    private void addNodeToClipboard(Node nodeToCopy, Node startNode, Node endNode, XMLSelection selection) {
        //Children of lists should be added to the list in the clipboard
        if (BBX.CONTAINER.LIST.isA(nodeToCopy.getParent())) {
            addListItemToClipboard(nodeToCopy, startNode, endNode, selection);
        } else if (BBX.BLOCK.isA(nodeToCopy)) {
            Element copy = copyBlock(startNode, endNode, (Element) nodeToCopy, selection, false);
            UTDHelper.stripUTDRecursive(copy);
            if (!blockCopy(copy, selection,
                    FastXPath.descendant(nodeToCopy).list().contains(startNode),
                    FastXPath.descendant(nodeToCopy).list().contains(endNode)))
                clips.add(new Clip(copy));
        } else {
            if (nodeToCopy instanceof ParentNode) {
                nodeToCopy = nodeToCopy.copy();
                UTDHelper.stripUTDRecursive((ParentNode) nodeToCopy);
                clips.add(new Clip(nodeToCopy));
            } else {
                clips.add(new Clip(nodeToCopy.copy()));
            }
        }
    }

    private Element copyBlock(Node startNode, Node endNode, Element block, XMLSelection selection, boolean cut) {
        List<Node> children = FastXPath.descendant(block).list();
        if (!children.contains(startNode) && !children.contains(endNode)) {
            return block.copy();
        }
        Element blockCopy = block.copy();
        List<Node> nodesToDetach = new ArrayList<>();
        if (children.contains(startNode)) {
            // If the startNode is inside this block, remove every node that
            // comes before startNode
            List<Integer> indexes = findNodeIndexes(block, startNode);
            Node findStartNodeCopy = blockCopy;
            for (int i = indexes.size() - 1; i >= 0; i--) {
                findStartNodeCopy = findStartNodeCopy.getChild(indexes.get(i));
            }
            final Node startNodeCopy = findStartNodeCopy; // lambdas!

            List<Node> following = FastXPath.followingAndSelf(blockCopy).list();
            for (Node n : following) {
                if (n != blockCopy && n != startNodeCopy && !FastXPath.descendant(n).list().contains(startNodeCopy)) {
                    nodesToDetach.add(n);
                }
                if (n == startNodeCopy)
                    break;
            }
            if (selection.start instanceof XMLTextCaret) {
                // If only part of the text node inside the block was selected,
                // split the text node
                String newValue = startNodeCopy.getValue().substring(((XMLTextCaret) selection.start).getOffset());
                if (newValue.isEmpty())
                    nodesToDetach.add(startNodeCopy);
                else
                    ((Text) startNodeCopy).setValue(newValue);
                if (cut && startNode instanceof Text) {
                    ((Text) startNode)
                            .setValue(startNode.getValue().substring(0, ((XMLTextCaret) selection.start).getOffset()));
                }
            } else if (MathModule.isMath(selection.start.getNode())) {
                Node newMath = MathModule.makeMathFromSelection(manager.getManager());
                ParentNode parentNode = startNodeCopy.getParent();
                parentNode.replaceChild(startNodeCopy, newMath);
            }
            if (cut && (startNode instanceof Text || MathModule.isMath(startNode))) {
                for (int i = children.indexOf(startNode) + 1; i < children.size(); i++) {
                    nodesToDetach.add(children.get(i));
                }
            }
        }
        if (children.contains(endNode)) {
            // If the endNode is inside this block, remove every node that comes
            // after endNode
            List<Integer> indexes = findNodeIndexes(block, endNode);
            Node findEndNodeCopy = blockCopy;
            for (int i = indexes.size() - 1; i >= 0; i--) {
                findEndNodeCopy = findEndNodeCopy.getChild(indexes.get(i));
            }
            final Node endNodeCopy = findEndNodeCopy;
            //If they're page numbers, don't detach anything from the node
            if (!(BBX.SPAN.PAGE_NUM.isA(endNodeCopy) || BBX.BLOCK.PAGE_NUM.isA(endNodeCopy))) {
                FastXPath.followingAndSelf(endNodeCopy).stream().forEach(n -> {
                    if (n != endNodeCopy) {
                        nodesToDetach.add(n);
                    }
                });
            }

            if (selection.end instanceof XMLTextCaret) {
                // If only part of the text node inside the block was selected,
                // split the text node
                String newValue = endNodeCopy.getValue().substring(0, ((XMLTextCaret) selection.end).getOffset());
                if (newValue.isEmpty())
                    nodesToDetach.add(endNodeCopy);
                else
                    ((Text) endNodeCopy).setValue(newValue);
                if (cut && endNode instanceof Text)
//					((Text) endNode).setValue(endNode.getValue().substring(caretSel.offset));
                    ((Text) endNode).setValue(endNode.getValue().substring(((XMLTextCaret) selection.end).getOffset()));
            } else if (MathModule.isMath(selection.end.getNode())) {
                Node newMath = MathModule.makeMathFromSelection(manager.getManager());
                ParentNode parentNode = endNodeCopy.getParent();
                parentNode.replaceChild(endNodeCopy, newMath);
            }
            if (cut && (endNode instanceof Text || MathModule.isMath(endNode))) {
                for (int i = 0; i < children.indexOf(endNode); i++) {
                    if (children.get(i).getChildCount() == 0
                            || !FastXPath.descendant(children.get(i)).list().contains(endNode))
                        nodesToDetach.add(children.get(i));
                }
            }
        }

        nodesToDetach.forEach(Node::detach);
        UTDHelper.stripUTDRecursive(blockCopy);
        return blockCopy;
    }

    private void addListItemToClipboard(Node listItem, Node startNode, Node endNode, XMLSelection selection) {
        Element listTag = (Element) listItem.getParent();
        if (!BBX.CONTAINER.LIST.isA(listTag)) {
            throw new IllegalArgumentException("Invalid BBX: List Item outside of List Tag");
        }
        int listLevel = BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.get(listTag);
        Element listItemCopy = copyBlock(startNode, endNode, (Element) listItem, selection, false);
        if (blockCopy(listItemCopy, selection, FastXPath.descendant(listItem).list().contains(startNode),
                FastXPath.descendant(listItem).list().contains(endNode))) {
            return;
        }
        if (clips.isEmpty() // If there's nothing else in the clipboard
                //or the previous clipboard item wasn't a list
                || !BBX.CONTAINER.LIST.isA(clips.get(clips.size() - 1).getNode())
                // the previous list was a different list level Just add the list
                // tag to the clipboard
                || listLevel != BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.get((Element) clips.get(clips.size() - 1).getNode())) {
            Element copy = listTag.copy();
            copy.removeChildren();
            copy.appendChild(listItemCopy);
            clips.add(new Clip(copy));
        } else { // Otherwise, add the list item to the previous list tag
            ((Element) clips.get(clips.size() - 1).getNode()).appendChild(listItemCopy);
        }
    }

    private void addSimpleListItemToClipboard(Node listItem, Node startNode, Node endNode, XMLSelection selection) {
        Element listTag = (Element) listItem.getParent();
        if (!BBX.CONTAINER.LIST.isA(listTag)) {
            throw new IllegalArgumentException("Invalid BBX: List Item outside of List Tag");
        }
        int listLevel = BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.get(listTag);
//		Element listItemCopy = copyBlock(startNode, endNode, (Element) listItem, selection, false);
        Element block = BBXUtils.findBlock(startNode);
        Node node;
        if (selection.start instanceof XMLTextCaret) {
            // Trim the text down to only be what is selected
            String textValue = selection.start.getNode().getValue().substring(((XMLTextCaret) selection.start).getOffset(),
                    ((XMLTextCaret) selection.end).getOffset());
            Node inlineParent = XMLHandler.ancestorVisitor(selection.start.getNode(), BBX.INLINE::isA);
            // If text is descended from an emphasis, take the
            // emphasis with it
            if (inlineParent != null) {
                node = inlineParent.copy();
                ((Element) node).removeChildren();
                ((Element) node).appendChild(textValue);
            } else {
                node = new Text(textValue);
            }
        } else if (MathModule.isMath(selection.start.getNode())) {
            // will get the inline.mathml element
            node = MathModule.makeMathFromSelection(manager.getManager());
        } else {
            node = selection.start.getNode().copy();
        }
        if (!BBX.BLOCK.isA(node)) {
            block = block.copy();
            block.removeChildren();
            block.appendChild(node);
            node = block;
        }
        if (blockCopy(block, selection, FastXPath.descendant(listItem).list().contains(startNode),
                FastXPath.descendant(listItem).list().contains(endNode))) {
            return;
        }
        if (!BBX.BLOCK.isA(node)) {
            block = block.copy();
            block.removeChildren();
            block.appendChild(node);
            node = block;
        }
        if (clips.isEmpty() // If there's nothing else in the clipboard
                //or the previous clipboard item wasn't a list
                || !BBX.CONTAINER.LIST.isA(clips.get(clips.size() - 1).getNode())
                // the previous list was a different list level Just add the list
                // tag to the clipboard
                || listLevel != BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.get((Element) clips.get(clips.size() - 1).getNode())) {
            Element copy = listTag.copy();
            copy.removeChildren();
            copy.appendChild(node);
            clips.add(new Clip(copy));
        } else { // Otherwise, add the list item to the previous list tag
            ((Element) clips.get(clips.size() - 1).getNode()).appendChild(node);
        }
    }

    private List<Integer> findNodeIndexes(Element block, Node nodeToFind) {
        List<Integer> indexes = new ArrayList<>();
        Node curNode = nodeToFind;
        while (curNode != block) {
            indexes.add(curNode.getParent().indexOf(curNode));
            curNode = curNode.getParent();
        }
        return indexes;
    }

    private Text getFinalTextChild(Node node, boolean lookForMath) {
        if (node == null)
            throw new NullPointerException("node");
        if (node instanceof Text)
            return (Text) node;
        Text returnText = null;
        List<Node> children = FastXPath.descendant(node).list();
        for (Node child : children) {
            if (child instanceof Text && ((!lookForMath || MathModule.isMath(child)))
                    && !XMLHandler.ancestorElementIs(child, UTDElements.BRL::isA))
                returnText = (Text) child;
        }
        return returnText;
    }

    private void normalizeEmphasis(Node parent) {
        if (parent.getChildCount() == 0)
            return;
        List<Node> children = FastXPath.descendant(parent).stream().filter(BBX.INLINE.EMPHASIS::isA).toList();
        for (Node child : children) {
            Element emphasis = (Element) child;
            ParentNode empParent = emphasis.getParent();
            int index = empParent.indexOf(emphasis);
            EnumSet<EmphasisType> empType = BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS.get(emphasis);
            // Is there an identical emphasis before this emphasis?
            if (index > 0 && BBX.INLINE.EMPHASIS.isA(empParent.getChild(index - 1))) {
                Element prev = (Element) empParent.getChild(index - 1);
                if (BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS.get(prev).equals(empType)) {
                    for (int i = prev.getChildCount() - 1; i >= 0; i--) {
                        Node prevChild = prev.getChild(i);
                        prevChild.detach();
                        emphasis.insertChild(prevChild, 0);
                    }
                    prev.detach();
                    normalizeEmphasis(parent);
                    return;
                }
            }
            // Is there an identical emphasis after this emphasis?
            if (index < empParent.getChildCount() - 1 && BBX.INLINE.EMPHASIS.isA(empParent.getChild(index + 1))) {
                Element next = (Element) empParent.getChild(index + 1);
                if (BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS.get(next).equals(empType)) {
                    while (next.getChildCount() > 0) {
                        Node nextChild = next.getChild(0);
                        nextChild.detach();
                        emphasis.appendChild(nextChild);
                    }
                    next.detach();
                    normalizeEmphasis(parent);
                    return;
                }
            }

            // Is there an emphasis descended from this emphasis?
            for (int i = 0; i < emphasis.getChildCount(); i++) {
                if (BBX.INLINE.EMPHASIS.isA(emphasis.getChild(i))) {
                    Element nestedEmphasis = (Element) emphasis.getChild(i);
                    ParentNode nestedParent = nestedEmphasis.getParent();
                    int nestedEmphasisIndex = nestedParent.indexOf(nestedEmphasis);
                    // Is it identical?
                    if (BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS.get(nestedEmphasis).equals(empType)) {
                        // Just throw out the emphasis node and replace it with
                        // its children
                        for (int j = 0; j < nestedEmphasis.getChildCount(); j++) {
                            Node nestedChild = nestedEmphasis.getChild(j);
                            nestedChild.detach();
                            nestedParent.insertChild(nestedChild, nestedEmphasisIndex);
                        }
                        nestedEmphasis.detach();
                    } else {
                        // Split the parent emphasis into two and put the nested
                        // emphasis in between
                        NodeTreeSplitter.split((Element) nestedParent, nestedEmphasis);
                    }
                    normalizeEmphasis(parent);
                    return;
                }
            }
        }
        Utils.combineAdjacentTextNodes((ParentNode) parent);
    }

    /**
     * Returns true if copiedBlock should not be added to the clipboard
     */
    private boolean blockCopy(Element copiedBlock, XMLSelection selection, boolean atStart, boolean atEnd) {
        if (copiedBlock.getChildCount() == 0) {
            // RT #4707: If the selection is being done through the text view,
            // and it has nothing actually selected (thus copiedBlock has no
            // children)
            // do not copy the block.
            return (atStart && selection.start instanceof XMLTextCaret)
                    || (atEnd && selection.end instanceof XMLTextCaret);
        }
        return false;
    }

    private static String getBlocksTextInSection(Element section) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < section.getChildCount(); i++) {
            if (section.getChild(i) instanceof Element) {
                if (BBX.BLOCK.isA(section.getChild(i)))
                    sb.append(section.getChild(i).getValue()).append(System.lineSeparator());
                else {
                    sb.append(getBlocksTextInSection((Element) section.getChild(i)));
                }
            }
        }
        return sb.toString();
    }

    private void deleteTextViewSelection(@NotNull Manager manager, boolean keepBlock) {
        if (manager.getTextView().getSelectionText().isEmpty()) {
            return;// use the view here as
            // manager.getSelection.isTextNoSelection will ignore math
        }

        // Delete selected nodes
        XMLSelection selection = manager.getSimpleManager().getCurrentSelection();
        Node section = selection.start.getNode();
        while (!BBX.SECTION.isA(section)) {
            section = section.getParent();
        }

        if (!(MathModule.isMath(selection.start.getNode()) && MathModule.isMath(selection.end.getNode())) && keepBlock
                && TextVerifyKeyListener.isBeginningOfBlock(selection.start.getNode())
                && TextVerifyKeyListener.isEndOfBlock(selection.end.getNode())) {
            /*
             * When the block is fully selected, pressing delete removes the
             * whole block, which causes the paste-over feature to always apply
             * to the next block
             *
             * Pilcrow hack to the rescue! Text is overwritten with the pilcrow
             * character, paste can now add to block like normal, then LiveFixer
             * removes it
             */
            Event event = new Event();
            event.character = LiveFixer.PILCROW.charAt(0);
            if (manager.getText().getView().isFocusControl()) {
                manager.getText().getView().notifyListeners(SWT.KeyDown, event);
                manager.getText().update(false);

                Element parentBlock = BBXUtils.findBlock(manager.getSimpleManager().getCurrentCaret().getNode());
                if (parentBlock != null) {
                    parentBlock.addAttribute(new Attribute(LiveFixer.NEWPAGE_PLACEHOLDER_ATTRIB, "true"));
                }

                log.error("updated");
            }
        } else {
            // I'm so sorry for what's about to happen
            Event event = new Event();
            event.keyCode = SWT.DEL;
            if (manager.getText().getView().isFocusControl()) {
                // Send a delete key event to the text view
                manager.getText().getView().notifyListeners(SWT.KeyDown, event);
                // trigger MapList rebuild
                manager.getText().update(false);
            }
        }
    }

}
