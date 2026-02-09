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

import org.brailleblaster.perspectives.braille.mapping.maps.MapList;

import nu.xom.Element;
import nu.xom.Node;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.jspecify.annotations.NonNull;

public abstract class AbstractMapElement {
	public final static int NOT_SET = -1;
	protected int start, end;
	
	public AbstractMapElement(int start, int end){
		this.start = start;
		this.end = end;
	}
	
	public AbstractMapElement(){
		this(NOT_SET, NOT_SET);
	}
	
	public Element getNodeParent(){
		Node n = getNode();
		return n != null ? (Element)n.getParent() : null;
	}

	@NonNull
	public String getText(){
		Node n = getNode();
		return n!= null ? n.getValue() : "";
	}
	
	public void setOffsets(int start, int end){
		this.start = start;
		this.end = end;
	}
	
	public void resetOffsets(){
		start = NOT_SET;
		end = NOT_SET;
	}
	
	public void setStart(int start){
		this.start = start;
	}
	
	public void setEnd(int end){
		this.end = end;
	}
	
	public int getStart(MapList list){
		return start;
	}
	
	public int getEnd(MapList list){
		return end;
	}
	
	public abstract Node getNode();
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" + "start=" + start + ", end=" + end + ", n=" + XMLHandler.toXMLSimple(getNode()) + '}';
	}
	
	
}
