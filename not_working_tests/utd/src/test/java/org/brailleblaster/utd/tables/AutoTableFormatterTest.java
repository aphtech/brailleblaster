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

/*import org.brailleblaster.utd.Cursor;
import org.brailleblaster.utd.StyleMap;
import org.brailleblaster.utd.UTDTranslationEngine;
import org.brailleblaster.utd.Style;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import nu.xom.Element;

import org.brailleblaster.utd.tables.SimpleTableFormatter;
import org.brailleblaster.utd.tables.StairstepTableFormatter;
import org.brailleblaster.utd.tables.ListedTableFormatter;
import org.brailleblaster.utd.tables.LinearTableFormatter;
import org.brailleblaster.utd.tables.AutoTableFormatter;*/

public class AutoTableFormatterTest {
/*	private AutoTableFormatter newTable = (AutoTableFormatter)TableFormat.AUTO.getFormatter();
	private UTDTranslationEngine engine = new UTDTranslationEngine();
	private Style style = new Style();
	@BeforeMethod
	public void setupMethod(){
		style.setLeftMargin(0);
		style.setRightMargin(0);
	}
	@DataProvider(name="simpleTable")
	public Object[][] createThreeByThreeTable(){
		return new Object[][]{ {createTable(3, 3, "Test", ",te/")} };
	}
	@DataProvider(name="linearTable")
	public Object[][] createLinearTable(){
		return new Object[][] { {createTable(5, 5, "Testing with very long cells", "test test test test test test test test test test test")} };
	}
	@DataProvider(name="listedTable")
	public Object[][] createListedTable(){
		//Table with column headings and numbers
		Element tableElement = new Element("table");
		Element firstRow = new Element("tr");
		for(int i = 0; i < 4; i++){
			Element colHeadings = new Element("td");
			colHeadings.appendChild("Heading");
			Element brlHeading = new Element("brl");
			brlHeading.appendChild(",heading");
			colHeadings.appendChild(brlHeading);
			firstRow.appendChild(colHeadings);
		}
		tableElement.appendChild(firstRow);
		for(int i = 0; i <2; i++){
			Element newRow = new Element("tr");
			for(int q = 0; q < 4; q++){
				Element entry = new Element("td");
				entry.appendChild("" + q);
				newRow.appendChild(entry);
			}
			tableElement.appendChild(newRow);
		}
		return new Object[][] { {createTable(3, 3, "1", "#a")}, {tableElement} };
	}
	@DataProvider(name="stairstepTable")
	private Object[][] createStairstepTable(){
		return new Object[][] { {createTable(3, 3, "test", "This; is a test in which a table has very long columns which thus eliminates the possibility of the table being a simple table")} };
	}
	
	private Element createTable(int rows, int columns, String text, String brl){
		Element tableElement = new Element("table");
		for(int i = 0; i < rows; i++){
			Element rowElement = new Element("tr");
			for(int q = 0; q < columns; q++){
				Element entry = new Element("td");
				entry.appendChild(text);
				Element brlElement = new Element("brl");
				brlElement.appendChild(brl);
				entry.appendChild(brlElement);
				rowElement.appendChild(entry);
			}
			tableElement.appendChild(rowElement);
		}
		return tableElement;
	}
	
	@Test(dataProvider="simpleTable")
	public void testSimpleTable(Element tableElement){
		System.out.println("Starting simple");
		newTable.createTable(tableElement, style, new Cursor(), engine, new StyleMap());
		System.out.println(tableElement.toXML());
		assertTrue(newTable.getActiveFormatter() instanceof SimpleTableFormatter);
	}
	
	@Test(dataProvider="linearTable")
	public void testLinearTable(Element tableElement){
		newTable.createTable(tableElement, style, new Cursor(), engine, new StyleMap());
		System.out.println(tableElement.toXML());
		assertTrue(newTable.getActiveFormatter() instanceof LinearTableFormatter);
	}
	
	@Test(dataProvider="listedTable")
	public void testListedTable(Element tableElement){
		newTable.createTable(tableElement, style, new Cursor(), engine, new StyleMap());
		System.out.println(tableElement.toXML());
		assertTrue(newTable.getActiveFormatter() instanceof ListedTableFormatter);
	}
	
	@Test(dataProvider="stairstepTable")
	public void testStairstepTable(Element tableElement){
		System.out.println("starting stairstep");
		newTable.createTable(tableElement, style, new Cursor(), engine, new StyleMap());
		System.out.println(tableElement.toXML());
		assertTrue(newTable.getActiveFormatter() instanceof StairstepTableFormatter);
	}*/
}
