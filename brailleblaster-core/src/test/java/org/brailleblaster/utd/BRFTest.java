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
package org.brailleblaster.utd;

import java.io.IOException;
import org.brailleblaster.TestGroups;
import org.testng.annotations.Test;

/**
 * Broken: written for the pre-BBX DTBook pipeline. BRFTestRunner now converts to BBX first, and the output
 * differs in braille: changeTable spans are no longer computer braille (issue3615), utd:toc-* attributes no
 * longer make TOC entries, and head style maps aren't read. Needs a braille/product decision, see test_fix.md
 * Phase 6.
 */
@Test(groups = TestGroups.BROKEN_TESTS)
public class BRFTest {
	@Test
	public void genericBlockActionOnCustomElementsTest() throws IOException {
		//Issue #3512, #3517
		new BRFTestRunner().compareXMLtoBraille(
				"""
						<styleMap xmlns="http://brailleblaster.org/ns/utd">
							<namespaces>
								<namespace prefix="utd" uri="http://brailleblaster.org/ns/utd"/>
								<namespace prefix="dsy" uri="http://www.daisy.org/z3986/2005/dtbook/"/>
							</namespaces>
							<!-- SAVE -->
							<entry>
								<matcher selfAttribName="customStyle" selfAttribNamespace="utd" selfAttribValue="73e32ca4-74db-4f32-99f9-6309f4306155" type="org.brailleblaster.utd.matchers.NodeAttributeMatcher"/>
								<style>
									<align>CENTERED</align>
									<dontSplit>false</dontSplit>
									<format>NORMAL</format>
									<guideWords>false</guideWords>
									<id>heading/centeredHeading</id>
									<indent>3</indent>
									<keepWithNext>true</keepWithNext>
									<keepWithPrevious>false</keepWithPrevious>
									<lineLength>-3</lineLength>
									<lineNumber>false</lineNumber>
									<lineSpacing>1</lineSpacing>
									<linesAfter>2</linesAfter>
									<linesBefore>2</linesBefore>
									<name>Centered Heading</name>
									<newPagesAfter>0</newPagesAfter>
									<newPagesBefore>0</newPagesBefore>
									<orphanControl>1</orphanControl>
									<pageNum>false</pageNum>
									<pageSide>AUTO</pageSide>
									<skipNumberLines>NONE</skipNumberLines>
									<skipPages>0</skipPages>
									<translation>CONTRACTED</translation>
									<volumeEnd>false</volumeEnd>
								</style>
							</entry>
							<!-- SAVE-->
							<entry>
								<matcher selfAttribName="customStyle" selfAttribNamespace="utd" selfAttribValue="1e4f322d-9b64-4636-acf9-e9acb0f96c6e" type="org.brailleblaster.utd.matchers.NodeAttributeMatcher"/>
								<style>
									<align>CENTERED</align>
									<dontSplit>false</dontSplit>
									<format>NORMAL</format>
									<guideWords>false</guideWords>
									<id>heading/centeredHeading</id>
									<indent>3</indent>
									<keepWithNext>true</keepWithNext>
									<keepWithPrevious>false</keepWithPrevious>
									<lineLength>-3</lineLength>
									<lineNumber>false</lineNumber>
									<lineSpacing>1</lineSpacing>
									<linesAfter>2</linesAfter>
									<linesBefore>2</linesBefore>
									<name>Centered Heading</name>
									<newPagesAfter>0</newPagesAfter>
									<newPagesBefore>0</newPagesBefore>
									<orphanControl>1</orphanControl>
									<pageNum>false</pageNum>
									<pageSide>AUTO</pageSide>
									<skipNumberLines>NONE</skipNumberLines>
									<skipPages>0</skipPages>
									<translation>CONTRACTED</translation>
									<volumeEnd>false</volumeEnd>
								</style>
							</entry>
							<!-- SAVE-->
							<entry>
								<matcher selfAttribName="id" selfAttribValue="2300777131233294483_0" type="org.brailleblaster.utd.matchers.NodeAttributeMatcher"/>
								<style>
									<align>CENTERED</align>
									<dontSplit>false</dontSplit>
									<format>NORMAL</format>
									<guideWords>false</guideWords>
									<id>heading/centeredHeading</id>
									<indent>3</indent>
									<keepWithNext>true</keepWithNext>
									<keepWithPrevious>false</keepWithPrevious>
									<lineLength>-3</lineLength>
									<lineNumber>false</lineNumber>
									<lineSpacing>1</lineSpacing>
									<linesAfter>2</linesAfter>
									<linesBefore>2</linesBefore>
									<name>Centered Heading</name>
									<newPagesAfter>0</newPagesAfter>
									<newPagesBefore>0</newPagesBefore>
									<orphanControl>1</orphanControl>
									<pageNum>false</pageNum>
									<pageSide>AUTO</pageSide>
									<skipNumberLines>NONE</skipNumberLines>
									<skipPages>0</skipPages>
									<translation>CONTRACTED</translation>
									<volumeEnd>false</volumeEnd>
								</style>
							</entry>
						</styleMap>
						<actionMap xmlns="http://brailleblaster.org/ns/utd">
							<namespaces>
								<namespace prefix="utd" uri="http://brailleblaster.org/ns/utd"/>
								<namespace prefix="dsy" uri="http://www.daisy.org/z3986/2005/dtbook/"/>
							</namespaces>
							<!-- SAVE -->
							<entry>
								<matcher selfAttribName="customAction" selfAttribNamespace="utd" selfAttribValue="56b7d184-a8f5-44dd-b417-429902719910" type="org.brailleblaster.utd.matchers.NodeAttributeMatcher"/>
								<action type="org.brailleblaster.utd.actions.BoldAction"/>
							</entry>
						</actionMap>""".indent(2),
				//Book
				"""
						<level3 class='sections' id='NIMAS9780133268195-lvl3-0037'>
						<pagenum id='p119' page='normal'>119</pagenum>
						<h3 class='section' id='2300777131233294483_0' utd:customStyle='fc09305d-2672-40ac-b680-e270a304f27d'>Writing to Sources</h3>
						<list type='pl' utd-style='Centered Heading' utd:customStyle='1e4f322d-9b64-4636-acf9-e9acb0f96c6e'>
						<customUTD utd:customAction='56b7d184-a8f5-44dd-b417-429902719910'>The Finish of Patsy Barnes</customUTD>
						</list>
						<customUTD utd:customStyle='73e32ca4-74db-4f32-99f9-6309f4306155'>•\s
						<strong>The Drummer Boy of Shiloh</strong></customUTD>
						</level3>
						""",
				//braille
				"""
						       ,writ+ to ,s|rces        #aai

						  ~7,! ,f9i% ( ,patsy ,b>nes~'

						_4 ~7,! ,drumm} ,boy ( ,%iloh~'""".indent(4),
				BRFTestRunner.OPTS_STRIP_ENDING_PAGE);
	}

	@Test
	public void issue3615() throws IOException {
		new BRFTestRunner().compareXMLtoBraille("",
				//book
				"<p>this is some tests</p>"
				+ "<p><strong>this is some <changeTable table='COMPUTER_BRAILLE'>tests</changeTable></strong></p>"
				+ "<p><strong>this is <changeTable table='COMPUTER_BRAILLE'>some</changeTable> tests</strong></p>"
				+ "<p><strong><changeTable table='COMPUTER_BRAILLE'>this</changeTable> is some tests</strong></p>",
				//braille
				"""
						? is "s te/s
						~7? is some te/s~'
						~7this is some te/s~'
						~7this is "s tests~'""".indent(2),
				BRFTestRunner.OPTS_STRIP_ENDING_PAGE);
	}

	@Test
	public void tocPageElementPlacementTest() throws IOException {
		new BRFTestRunner().compareXMLtoBraille("",
				//book
				"<p><span utd:toc-depth='1' utd:toc-maxdepth='3'>test letter a<span utd:toc-type='page'>24</span></span></p>"
				+ "<p><span utd:toc-depth='1' utd:toc-maxdepth='3'>test letter b</span><span utd:toc-type='page'>25</span></p>"
				+ "<p><span utd:toc-depth='1' utd:toc-maxdepth='3'>test letter a<span utd:toc-type='page'>24</span></span></p>"
				+ "<p><span utd:toc-depth='1' utd:toc-maxdepth='3'>test letter c</span></p><p><span utd:toc-type='page'>26</span></p>"
				+ "<p><span utd:toc-depth='1' utd:toc-maxdepth='3'>test letter a<span utd:toc-type='page'>24</span></span></p>",
				//braille
				"""

						te/ lr a ""\"""\"""\"""\"""\"""\"""\"""\"""\" #bd
						te/ lr b ""\"""\"""\"""\"""\"""\"""\"""\"""\" #be
						te/ lr a ""\"""\"""\"""\"""\"""\"""\"""\"""\" #bd
						te/ lr ;c ""\"""\"""\"""\"""\"""\"""\"""\""" #bf
						te/ lr a ""\"""\"""\"""\"""\"""\"""\"""\"""\" #bd""",
				BRFTestRunner.OPTS_STRIP_ENDING_PAGE);
	}
}
