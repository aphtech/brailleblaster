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
package org.brailleblaster.utd.asciimath;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.testng.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xmlunit.matchers.CompareMatcher;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.ValidityException;
import nu.xom.XPathContext;
import nu.xom.canonical.Canonicalizer;

public class AsciiMathConverterTest {
	private final Logger log = LoggerFactory.getLogger(AsciiMathConverterTest.class);
	private final AsciiMathConverter converter = AsciiMathConverter.INSTANCE;
	@DataProvider(name="conversionsProvider")
	public Iterator<Object[]> conversionsProvider() {
		List<Object[]> data = new ArrayList<>();
		data.add(new Object[] {"`a^2 + b^2`", "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><msup><mi>a</mi><mn>2</mn></msup><mo>+</mo><msup><mi>b</mi><mn>2</mn></msup></math>"});
		data.add(new Object[] {"   `a^2 + b^2`    ", "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><msup><mi>a</mi><mn>2</mn></msup><mo>+</mo><msup><mi>b</mi><mn>2</mn></msup></math>"});
		data.add(new Object[] {"`sqrt(x^2 + y^2)`", "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><msqrt><mrow><msup><mi>x</mi><mn>2</mn></msup><mo>+</mo><msup><mi>y</mi><mn>2</mn></msup></mrow></msqrt></math>"});
		data.add(new Object[] {"`sqrt(x^2 + y^2)`", """
<math xmlns="http://www.w3.org/1998/Math/MathML">
  <msqrt><mrow>
    <msup>
      <mi>x</mi>
      <mn>2</mn>
    </msup>
    <mo>+</mo>
    <msup>
      <mi>y</mi>
      <mn>2</mn>
    </msup>
  </mrow></msqrt>
</math>"""});
		data.add(new Object[] {"`fr a`", "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><mstyle mathvariant=\"fraktur\"><mi>a</mi></mstyle></math>"});
		return data.iterator();
	}
	@Test(dataProvider="conversionsProvider")
	public void basicConvertToAsciiMath(String asciiMath, String mathml) {
		String expected = asciiMath.trim();
		Builder builder = new Builder();
		Document doc;
		try {
			doc = builder.build(new StringReader(mathml));
		} catch (ParsingException | IOException e) {
			throw new RuntimeException("Problem creating test data", e);
		}
        Element root = doc.getRootElement();
		String result = converter.toAsciiMath(new Nodes(root), false, true, MathTextFinder.ALTTEXT_ATTRIBUTE);
		assertEquals(result, expected);
	}
	@Test(dataProvider="conversionsProvider")
	public void basicConvertToMathML(String asciiMath, String mathml) {
		// Make the expected MathML canonical
		String expected;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Builder builder = new Builder();
			Document doc = builder.build(new StringReader(mathml));
			Canonicalizer c = new Canonicalizer(out);
			c.write(doc.getRootElement());
			expected = out.toString(StandardCharsets.UTF_8);
		} catch (ValidityException e1) {
			throw new RuntimeException("The test data MathML is invalid", e1);
		} catch (ParsingException e1) {
			throw new RuntimeException("Unable to parse test data MathML", e1);
		} catch (IOException e1) {
			throw new RuntimeException("Problem in creating the canonical form of the test data MathML", e1);
		}
		Nodes resultNodes = converter.toMathML(asciiMath, false, false, true);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < resultNodes.size(); i++) {
			sb.append(resultNodes.get(i).toXML());
		}
		String actual = sb.toString();
		if (actual.isEmpty()) {
			assertEquals(actual, expected);
		} else {
			assertThat(actual, CompareMatcher.isSimilarTo(mathml).ignoreWhitespace());
		}
	}
	@DataProvider(name="asciiMathParserTestsProvider")
	public Iterator<Object[]> asciiMathParserTestsProvider() throws IOException, ParsingException {
		List<Object[]> data = new ArrayList<>();
		XPathContext mmlContext = new XPathContext("m", "http://www.w3.org/1998/Math/MathML");
		Document testDataXml;
		try (InputStream in = getClass().getResourceAsStream("/org/brailleblaster/utd/asciimath/asciimathConverterTests.xml")) {
			Builder builder = new Builder();
			testDataXml = builder.build(in);
		}
		Nodes testNodes = testDataXml.query("/tests/test[not(@toXml='false')]");
		for (int i = 0; i < testNodes.size(); i++) {
			Nodes inputNodes = testNodes.get(i).query("input[1]");
			Nodes expectedNodes = testNodes.get(i).query("expected[1]");
			if (inputNodes.size() > 0 && expectedNodes.size() > 0) {
				Node inputNode = inputNodes.get(0);
				Node expectedNode = expectedNodes.get(0);
				Nodes mmlNodes = inputNode.query("m:math", mmlContext);
				String[] amStrs = expectedNode.getValue().trim().split("\\r?\\n|\\r");
				if (mmlNodes.size() == amStrs.length) {
					for (int j = 0; j < amStrs.length; j++) {
						Node mmlNode = mmlNodes.get(j);
						mmlNode.detach();
						data.add(new Object[] {amStrs[j], mmlNode.toXML()});
					}
				} else {
					log.warn("Unable to use test case {} due to differing lengths of inputs and outputs ", testNodes.get(i).toXML());
				}
			} else {
				log.warn("Test case {} has no data", testNodes.get(i).toXML());
			}
		}
		return data.iterator();
	}
	@Test(dataProvider="asciiMathParserTestsProvider")
	public void asciiMathParserTest(String asciiMath, String mml) {
		Nodes actualNodes = converter.toMathML(asciiMath, false, false, true);
		assertEquals(actualNodes.size(), 1);
		String actualStr = actualNodes.get(0).toXML();
		assertThat(actualStr, CompareMatcher.isSimilarTo(mml).ignoreWhitespace());
	}
	@DataProvider(name="storedASCIIMathProvider")
	public Iterator<Object[]> storedASCIIMathProvider() throws IOException, ParsingException {
		List<Object[]> data = new ArrayList<>();
		MathTextFinder[] alttextFinder = new MathTextFinder[] {MathTextFinder.ALTTEXT_ATTRIBUTE};
		Builder builder = new Builder();
		String mathml = "<math xmlns=\"http://www.w3.org/1998/Math/MathML\" alttext=\"y=x+2\"><mi>y</mi><mo>=</mo><mi>x</mi><mo>+</mo><mn>2</mn></math>";
		Document doc = builder.build(mathml, "");
		Node root = doc.getRootElement();
		data.add(new Object[] {"y=x+2", root, true, alttextFinder});
		data.add(new Object[] {"y=x+2", root, false, alttextFinder});
		data.add(new Object[] {"`y = x + 2`", root, true, new MathTextFinder[] {MathTextFinder.NONE}});
		data.add(new Object[] {"y = x + 2", root, false, new MathTextFinder[] {MathTextFinder.NONE}});
		mathml = "<math xmlns=\"http://www.w3.org/1998/Math/MathML\" alttext=\"`y=x+2`\"><mi>y</mi><mo>=</mo><mi>x</mi><mo>+</mo><mn>3</mn></math>";
		doc = builder.build(mathml, "");
		root = doc.getRootElement();
		data.add(new Object[] {"`y = x + 3`", root, true, alttextFinder});
		data.add(new Object[] {"y = x + 3", root, false, alttextFinder});
		data.add(new Object[] {"`y = x + 3`", root, true, new MathTextFinder[] {MathTextFinder.NONE}});
		data.add(new Object[] {"y = x + 3", root, false, new MathTextFinder[] {MathTextFinder.NONE}});
		mathml = "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"/>";
		doc = builder.build(mathml, "");
		root = doc.getRootElement();
		data.add(new Object[] {"``", root, true, alttextFinder});
		data.add(new Object[] {"", root, false, alttextFinder});
		mathml = "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><mi>y</mi></math>";
		doc = builder.build(mathml, "");
		root = doc.getRootElement();
		data.add(new Object[] {"`y`", root, true, alttextFinder});
		mathml = "<m:math xmlns:m=\"http://www.w3.org/1998/Math/MathML\" alttext=\"`x`\"><m:mi>x</m:mi></m:math>";
		doc = builder.build(mathml, "");
		root = doc.getRootElement();
		data.add(new Object[] {"`x`", root, true, alttextFinder});
		mathml = "<m:math xmlns:m=\"http://www.w3.org/1998/Math/MathML\" mathcolor=\"blue\" alttext=\"`x`\"/>";
		doc = builder.build(mathml, "");
		root = doc.getRootElement();
		data.add(new Object[] {"``", root, true, alttextFinder});
		mathml = "<math xmlns=\"http://www.w3.org/1998/Math/MathML\" mathcolor=\"blue\" alttext=\"`y`\"/>";
		doc = builder.build(mathml, "");
		root = doc.getRootElement();
		data.add(new Object[] {"``", root, true, alttextFinder});
		return data.iterator();
	}
	@Test(dataProvider="storedASCIIMathProvider")
	public void testUseStored(String asciiMath, Node mathml, boolean includeMathMarkers, MathTextFinder[] finders) {
		AsciiMathConverter convert = AsciiMathConverter.INSTANCE;
		String result = convert.toAsciiMath(new Nodes(mathml), false, includeMathMarkers, finders);
		assertEquals(result, asciiMath, String.format("Test of ASCIIMath %s against MathML %s", asciiMath, mathml.toXML()));
	}
}
