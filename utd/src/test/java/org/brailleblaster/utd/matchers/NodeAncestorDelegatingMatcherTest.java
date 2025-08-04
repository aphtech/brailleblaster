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
package org.brailleblaster.utd.matchers;

import com.google.common.collect.Iterators;
import org.brailleblaster.utd.ComparableStyle;
import org.brailleblaster.utd.NamespaceMap;
import org.brailleblaster.utd.matchers.NodeAncestorDelegatingMatcher.Position;
import org.brailleblaster.utd.properties.UTDElements;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;

public class NodeAncestorDelegatingMatcherTest {
    @DataProvider(name = "matchingStartProvider")
    public Iterator<Object[]> matchingStartProvider() {
        List<Object[]> dataList = new ArrayList<>();
        Element body = new Element("body");
        Element box = new Element("sidebar");
        box.addAttribute(new Attribute(UTDElements.UTD_STYLE_ATTRIB, "Box"));
        body.appendChild(box);
        Element heading = new Element("h1");
        heading.addAttribute(new Attribute(UTDElements.UTD_STYLE_ATTRIB, "Centered Heading"));
        Node textNode = new Text("Some text");
        heading.appendChild(textNode);
        box.appendChild(heading);
        Element list = new Element("list");
        box.appendChild(list);
        dataList.add(new Object[]{textNode, "Box"});
        body = new Element("body");
        box = new Element("sidebar");
        box.addAttribute(new Attribute(UTDElements.UTD_STYLE_ATTRIB, "Box"));
        body.appendChild(box);
        Element brl = UTDElements.BRL.create();
        brl.addAttribute(new Attribute("type", "formatting"));
        box.appendChild(brl);
        heading = new Element("h1");
        heading.addAttribute(new Attribute(UTDElements.UTD_STYLE_ATTRIB, "Centered Heading"));
        textNode = new Text("Some text");
        heading.appendChild(textNode);
        box.appendChild(heading);
        list = new Element("list");
        box.appendChild(list);
        dataList.add(new Object[]{textNode, "Box"});
        return dataList.iterator();
    }

    @Test(dataProvider = "matchingStartProvider")
    public void matchStartPosition(Node node, String ancestorStyle) {
        NodeAncestorDelegatingMatcher ancestorMatcher = new NodeAncestorDelegatingMatcher();
        ActionAndStyleMatcher styleMatcher = new ActionAndStyleMatcher();
        styleMatcher.getStyles().add(new ComparableStyle(ancestorStyle));
        ancestorMatcher.setMatcher(styleMatcher);
        ancestorMatcher.setPosition(Position.START);
        NamespaceMap namespaces = new NamespaceMap();
        assertTrue(ancestorMatcher.isMatch(node, namespaces));
    }

    @DataProvider(name = "matchingAnyProvider")
    public Iterator<Object[]> matchingAnyProvider() {
        List<Object[]> dataList = new ArrayList<>();
        Iterators.addAll(dataList, matchingStartProvider());
        Iterators.addAll(dataList, matchingEndProvider());
        return dataList.iterator();
    }

    @Test(dataProvider = "matchingAnyProvider")
    public void matchAnyPosition(Node node, String ancestorStyle) {
        NodeAncestorDelegatingMatcher ancestorMatcher = new NodeAncestorDelegatingMatcher();
        ActionAndStyleMatcher styleMatcher = new ActionAndStyleMatcher();
        styleMatcher.getStyles().add(new ComparableStyle(ancestorStyle));
        ancestorMatcher.setMatcher(styleMatcher);
        NamespaceMap namespaces = new NamespaceMap();
        assertTrue(ancestorMatcher.isMatch(node, namespaces));
    }

    @DataProvider(name = "matchingEndProvider")
    public Iterator<Object[]> matchingEndProvider() {
        List<Object[]> dataList = new ArrayList<>();
        Element body = new Element("body");
        Element box = new Element("sidebar");
        box.addAttribute(new Attribute("utd-style", "Box"));
        body.appendChild(box);
        Element heading = new Element("h1");
        Node textNode = new Text("Some text");
        box.appendChild(heading);
        Element list = new Element("list");
        list.appendChild(textNode);
        box.appendChild(list);
        dataList.add(new Object[]{textNode, "Box"});
        return dataList.iterator();
    }

    @Test(dataProvider = "matchingEndProvider")
    public void matchEndPosition(Node node, String ancestorStyle) {
        NodeAncestorDelegatingMatcher ancestorMatcher = new NodeAncestorDelegatingMatcher();
        ActionAndStyleMatcher styleMatcher = new ActionAndStyleMatcher();
        styleMatcher.getStyles().add(new ComparableStyle(ancestorStyle));
        ancestorMatcher.setMatcher(styleMatcher);
        ancestorMatcher.setPosition(Position.END);
        NamespaceMap namespaces = new NamespaceMap();
        assertTrue(ancestorMatcher.isMatch(node, namespaces));
    }
}
