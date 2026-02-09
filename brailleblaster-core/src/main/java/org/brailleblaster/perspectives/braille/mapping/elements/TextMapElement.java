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
package org.brailleblaster.perspectives.braille.mapping.elements;

import nu.xom.Element;
import nu.xom.Node;
import nu.xom.ParentNode;
import org.brailleblaster.math.mathml.MathModuleUtils;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.stream.Stream;


public class TextMapElement extends AbstractMapElement {
	public LinkedList<BrailleMapElement>brailleList;
	private boolean readOnly = false;
	private boolean fullyVisible;
	private String invisibleText; //Text of this TME that is not rendered due to fullyVisible = false
	private Node node;

	public TextMapElement(int start, int end, Node n) {
		super(start, end);
		node = n;
		brailleList = new LinkedList<>();
		fullyVisible = true;
		invisibleText = "";
	}
	
	public TextMapElement(Node n){
		node = n;
		brailleList = new LinkedList<>();
		fullyVisible = true;
		invisibleText = "";
	}

	@Override
	public Node getNode() {
		return node;
	}

	public void setNode(Node node){
		this.node = node;
	}
	
	public int textLength(){
		return getNode().getValue().length();
	}
	
	public void sort(){
		for(int i = 0; i < brailleList.size() - 1; i++){
			if(brailleList.get(i).getVPos() != null && brailleList.get(i + 1).getVPos() != null){
				if(brailleList.get(i).getVPos().equals(brailleList.get(i + 1).getVPos())){
					if(brailleList.get(i).getHPos() != null && brailleList.get(i + 1).getHPos() != null){
						if(brailleList.get(i).getHPos() > brailleList.get(i + 1).getHPos()){
							Collections.swap(brailleList, i, i + 1);
						}
					}
				}
			}
		}
	}

	public boolean isSpatialMath() {
		Node node = getNode();
		return getNode() != null && node.getDocument() != null && MathModuleUtils.isSpatialMath(node);
	}

	public boolean isMathML(){
		Node node = getNode();
		return node instanceof Element && (((Element) node).getLocalName().equals("math"));
	}
	
	public boolean ancestorIsMath(Node text){
		return XMLHandler.Companion.ancestorVisitorElement(text, n -> n.getLocalName().equals("math")) != null;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}
	
	/*
	 * Is TME in the map list but not actually loaded into the current section?
	 */
	public boolean isFullyVisible(){
		return fullyVisible;
	}
	
	public void setFullyVisible(boolean fullyVisible){
		this.fullyVisible = fullyVisible;
	}
	
	public String getInvisibleText(){
		return invisibleText;
	}
	
	public void appendInvisibleText(String invisibleText){
		this.invisibleText += invisibleText;
	}
	public @Nullable Element getBlock() {
		ParentNode p = getNodeParent();
		return (Element)Stream.iterate(p, e -> e instanceof Element, Node::getParent).filter(e -> "BLOCK".equals(((Element)e).getLocalName())).findFirst().orElse(null);
	}
}
