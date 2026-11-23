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
package org.brailleblaster.utd.tables;
/*
import java.io.IOException;
import java.io.InputStream;

import org.brailleblaster.utd.Cursor;
import org.brailleblaster.utd.Style;
import org.brailleblaster.utd.StyleMap;
import org.brailleblaster.utd.UTDTranslationEngine;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;
import nu.xom.Text;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.ValidityException;
*/
public class LinearTableFormatterTest {
/*	
	@Test
	public void testLinearTables() throws ValidityException, ParsingException, IOException{
		Builder builder = new Builder();
		InputStream inStream = null;
		Nodes tableNodes;
		try {
			inStream = getClass().getResourceAsStream("/org/brailleblaster/utd/TablesTestXml.xml");
			Document doc = builder.build(inStream);
			tableNodes = doc.query("//table");
		} finally {
			if (inStream != null) {
				inStream.close();
			}
		}
		for(int table = 0; table<tableNodes.size(); table++){
			LinearTableFormatter tf = (LinearTableFormatter)TableFormat.LINEAR.getFormatter();
			Style tStyle = new Style();
			tStyle.setLeftMargin(0);
			tStyle.setRightMargin(0);
			Element testTable = (Element)tableNodes.get(table);
			tf.createTable(testTable, tStyle, new Cursor(), new UTDTranslationEngine(), new StyleMap());
			Nodes entries = testTable.query("descendant::tr");
			for(int i = 0; i < entries.size(); i++){
				Nodes moveTos = entries.get(i).query("descendant::moveTo");
				assertTrue(moveTos.size() > 0);
				if(moveTos.size()>0){
					Element moveTo = (Element)moveTos.get(moveTos.size()-1);
					assertNotNull(moveTo.getAttribute("hPos"));
					assertNotNull(moveTo.getAttribute("vPos"));
				}
			}
			
			//Test that line widths never exceed 40
			Nodes allChildren = testTable.query("descendant::td/brl");
			int rowWidth = 0;
			for(int i = 0; i < allChildren.size(); i++){
				if(allChildren.get(i) instanceof Element){
					Element row = (Element)allChildren.get(i);
					for(int q = 0; q < row.getChildCount(); q++){
						if(row.getChild(q) instanceof Text){
							rowWidth += row.getChild(q).getValue().length();
						}
						if(row.getChild(q) instanceof Element){
							Element potentialMoveTo = (Element)row.getChild(q);
							if(potentialMoveTo.getLocalName().equals("moveTo")){
								rowWidth = 0;
							}
						}
						assertTrue(rowWidth <= 40);
					}
				}
			}
		}
	}*/
}
