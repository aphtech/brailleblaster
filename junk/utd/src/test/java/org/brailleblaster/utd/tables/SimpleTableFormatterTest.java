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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.brailleblaster.utd.ActionMap;
import org.brailleblaster.utd.Cursor;
import org.brailleblaster.utd.Style;
import org.brailleblaster.utd.StyleMap;
import org.brailleblaster.utd.UTDTranslationEngine;
import org.brailleblaster.utd.actions.GenericBlockAction;
import org.brailleblaster.utd.config.StyleDefinitions;
import org.brailleblaster.utd.config.UTDConfig;
import org.brailleblaster.utd.matchers.NodeNameMatcher;
import org.brailleblaster.utd.matchers.XPathMatcher;
import org.brailleblaster.utd.testutils.UTDConfigUtils;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.ValidityException;
*/
public class SimpleTableFormatterTest {
	
/*	@Test
	public void testSimpleTables() throws ValidityException, ParsingException, IOException{
		Builder builder = new Builder();
		InputStream inStream = null;
		Nodes tableNodes;
		UTDTranslationEngine engine = new UTDTranslationEngine();
		ActionMap actionMap = new ActionMap();
		actionMap.put(new XPathMatcher("self::*"), new GenericBlockAction());
		engine.setActionMap(actionMap);
		try{
			inStream = getClass().getResourceAsStream("/org/brailleblaster/utd/TablesTestXml.xml");
			Document doc = builder.build(inStream);
			tableNodes = doc.query("//table");
		} finally {
			if (inStream != null){
				inStream.close();
			}
		}
		for(int table = 0; table < tableNodes.size(); table++){
			SimpleTableFormatter tf = (SimpleTableFormatter)TableFormat.SIMPLE.getFormatter();
			Style tStyle = new Style();
			tStyle.setLeftMargin(0);
			tStyle.setRightMargin(0);
			Element testTable = (Element)tableNodes.get(table);
			/*tf.createTable(testTable, tStyle, new Cursor(), new UTDTranslationEngine());
			System.out.println(testTable.toXML());
			Nodes entries = testTable.query("descendant::td");
			for(int i = 0; i < entries.size(); i++){
				Nodes moveTo = entries.get(i).query("descendant::moveTo");
				assertTrue(moveTo.size() > 0);
				if(moveTo.size() > 0){
					Attribute vPos = ((Element)moveTo.get(0)).getAttribute("vPos");
					assertNotNull(vPos);
					Attribute hPos = ((Element)moveTo.get(0)).getAttribute("hPos");
					assertNotNull(hPos);
				}
			}
			
			//Test for overlapping moveTo elements
			Nodes moveTos = testTable.query("descendant::moveTo");
			double lastHPos = 0;
			double lastVPos = 0;
			for(int i = 1; i < moveTos.size(); i++){
				Element mtElement = (Element) moveTos.get(i);
				Attribute hPos = mtElement.getAttribute("hPos");
				Attribute vPos = mtElement.getAttribute("vPos");
				double hPosValue = Double.parseDouble(hPos.getValue());
				double vPosValue = Double.parseDouble(vPos.getValue());
				assertTrue(hPosValue > lastHPos || vPosValue > lastVPos);
				lastHPos = hPosValue;
				lastVPos = vPosValue;
			}
			
		}
	}*/
}
