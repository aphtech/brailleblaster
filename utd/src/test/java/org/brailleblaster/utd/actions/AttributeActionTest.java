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
package org.brailleblaster.utd.actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.brailleblaster.utd.testutils.asserts.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.brailleblaster.utd.ITranslationEngine;
import org.brailleblaster.utd.TextSpan;
import org.brailleblaster.utd.UTDTranslationEngine;
import org.brailleblaster.utd.properties.UTDElements;
import org.brailleblaster.utils.NamespacesKt;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import nu.xom.Attribute;
import nu.xom.Element;

public class AttributeActionTest {
	@DataProvider(name="validNodesProvider")
	public Iterator<Object[]> validNodesProvider() {
		List<Object[]> dataList = new ArrayList<>();
		Element p = new Element("p");
		Element img = new Element("img");
		img.addAttribute(new Attribute("alt", "My image description"));
		p.appendChild(img);
		String brlXML = String.format("<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\">,my image descrip;n</utd:brl>", NamespacesKt.UTD_NS);
		dataList.add(new Object[] {img, "alt", brlXML});
		p = new Element("p");
		img = new Element("image");
		img.addAttribute(new Attribute("text", "An image"));
		p.appendChild(img);
		brlXML = String.format("<utd:brl xmlns:utd=\"%s\" xml:space=\"preserve\">,an image</utd:brl>", NamespacesKt.UTD_NS);
		dataList.add(new Object[] {img, "text", brlXML});
		return dataList.iterator();
	}
	@Test(dataProvider="validNodesProvider")
	public void applyToNoStringTemplate(Element element, String attrName, String expectedBrlXML) {
		IAction action = new AttributeAction(attrName);
		ITranslationEngine engine = new UTDTranslationEngine();
		engine.getBrailleSettings().setUseAsciiBraille(true);
		List<TextSpan> result = action.applyTo(element, engine);
		assertThat(result).isNotNull().hasSize(1).doesNotContainNull();
		TextSpan ts = result.get(0);
		String expectedText = element.getAttributeValue(attrName);
		assertThat(ts).isTranslated().hasText(expectedText).hasNode(element).hasBrlElementXML(expectedBrlXML);
		
	}
}
