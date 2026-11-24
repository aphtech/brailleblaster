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
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;

import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.Text;
import nu.xom.ValidityException;
import nu.xom.ParsingException;

import org.brailleblaster.utd.Cursor;
import org.brailleblaster.utd.Style;
import org.brailleblaster.utd.StyleMap;
import org.brailleblaster.utd.UTDTranslationEngine;
import org.brailleblaster.utd.tables.StairstepTableFormatter;
*/
public class StairstepTableFormatterTest {
/*	@Test
	public void testStairstepTable() throws ValidityException, ParsingException, IOException{
		Builder builder = new Builder();
		InputStream inStream = null;
		Nodes tableNodes = null;
		try{
			inStream = getClass().getResourceAsStream("/org/brailleblaster/utd/TablesTestXml.xml");
			Document doc = builder.build(inStream); 
			tableNodes = doc.query("//table");
		} finally{
			if (inStream != null){
				inStream.close();
			}
		}
		for(int table = 0; table < tableNodes.size(); table++){
			StairstepTableFormatter tf = new StairstepTableFormatter();
			Element testTable = (Element) tableNodes.get(table);
			Style style = new Style();
			style.setLeftMargin(0);
			style.setRightMargin(0);
			tf.createTable(testTable, style, new Cursor(), new UTDTranslationEngine(), new StyleMap());
			Nodes tr = testTable.query("descendant::tr");
			for(int row = 0; row < tr.size(); row++){
				Nodes moveTos = tr.get(row).query("descendant::moveTo");
				assertTrue(moveTos.size()>0);
				for(int mt = 0; mt < moveTos.size(); mt++){
					Element moveTo = (Element)moveTos.get(mt);
					assertNotNull(moveTo.getAttribute("hPos"));
					assertNotNull(moveTo.getAttribute("vPos"));
				}
			}
			Nodes brlChildren = testTable.query("descendant::brl");
			int rowWidth = 0;
			for(int brl = 0; brl < brlChildren.size(); brl++){
				Element row = (Element)brlChildren.get(brl);
				for(int rowChildren = 0; rowChildren < row.getChildCount(); rowChildren++){
					if(row.getChild(rowChildren) instanceof Text){
						rowWidth += row.getChild(rowChildren).getValue().length();
					}
					if(row.getChild(rowChildren) instanceof Element){
						Element potentialMoveTo = (Element) row.getChild(rowChildren);
						if(potentialMoveTo.getLocalName().equals("moveTo")){
							rowWidth = 0;
						}
					}
					assertTrue(rowWidth <= 40);
				}
			}
		}
	}*/
}
