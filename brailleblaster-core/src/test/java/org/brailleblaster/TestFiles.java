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
package org.brailleblaster;

import org.brailleblaster.testrunners.TestXMLUtils;

import nu.xom.Document;

public class TestFiles {
    public static final String mathGuts = "<p><m:math><m:mi>x</m:mi><m:mo>+</m:mo><m:mn>2</m:mn></m:math></p>";
    public static final String mathPrintView = "x + 2";
    public static final Document simpleMath = TestXMLUtils.generateBookDoc("", mathGuts);
    public static final String wizard1 = "We're off to see the wizard.";
    public static final String wizard2 = "The wonderful wizard of Oz.";
    public static final String wizard3 = "We hear he is a whiz of a wiz";
    public static final String textGuts = "<p>" + wizard1 + "</p>" + "<p>" + wizard2 + "</p>" + "<p>" + wizard3
            + "</p>";
    public static final Document text = TestXMLUtils.generateBookDoc("", textGuts);
    public static final String listGuts = "<list><li>" + wizard1 + "</li>" + "<li>" + wizard2 + "</li>" + "<li>"
            + wizard3 + "</li></list>";
    public static final Document lists = TestXMLUtils.generateBookDoc("", listGuts);
    public static final Document listMath = TestXMLUtils.generateBookDoc("", listGuts + mathGuts);
    public static final Document listText = TestXMLUtils.generateBookDoc("", listGuts + textGuts);
    public static final Document textMath = TestXMLUtils.generateBookDoc("", textGuts + mathGuts);
}
