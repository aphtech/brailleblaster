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

import nu.xom.Element;
import org.brailleblaster.utd.UTDTranslationEngine;

import static org.testng.Assert.assertEquals;

public class InsertUncontractedTest {
	/*
	 *  !!!THIS ACTION IS NOT BEING USED AT THIS MOMENT. POSSIBLE DELETION.!!!
	 */
//	@Test
	public void processNodeTest() {
		Element dl = new Element("dl");
		Element dt = new Element("dt");
		dt.appendChild("test");
		Element dd = new Element("dd");
		dd.appendChild("definition");
		
		dl.appendChild(dt);
		dl.appendChild(dd);
		
		InsertUncontractedAction action = new InsertUncontractedAction();
		action.applyTo(dt, new UTDTranslationEngine());
		assertEquals(dd.toXML(), "");
		assertEquals(dd.getChildCount(), 2);
	}
}
