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

import org.brailleblaster.utd.properties.UTDElements;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import org.jspecify.annotations.Nullable;


public class BrailleMapElement extends AbstractMapElement{
	private final static String VPOS = "vPos";
	private final static String HPOS = "hPos";
	private boolean insideTable = false;
	private Double vPos, hPos;
	private final Node node;
	
	public BrailleMapElement(int start, int end, @Nullable Node n) {
		super(start, end);
		node = n;
		vPos = null;
		hPos = null;
	}
	
	public BrailleMapElement(@Nullable Node n){
		node = n;
	}

	@Override
	public Node getNode() {
		return node;
	}

	public void setPosition(Element moveTo){
		if(UTDElements.MOVE_TO.isA(moveTo)){
			setVPos(moveTo);
			setHPos(moveTo);
		}
	}
	
	public void setPosition(double vPos, double hPos){
		this.vPos = vPos;
		this.hPos = hPos;
	}
	
	private void setVPos(Element moveTo){
		Attribute vert = moveTo.getAttribute(VPOS);
		if(vert != null)
			vPos = Double.valueOf(vert.getValue());
	}
	
	private void setHPos(Element moveTo){
		Attribute hor = moveTo.getAttribute(HPOS);
		if(hor != null)
			hPos = Double.valueOf(hor.getValue());
	}
	
	public Double getVPos(){
		return vPos;
	}
	
	public Double getHPos(){
		return hPos;
	}
	
	public void setInsideTable(boolean value){
		insideTable = value;
	}
	public boolean isInsideTable(){
		return insideTable;
	}
}
