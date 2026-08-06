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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;

import org.brailleblaster.utd.actions.BoldAction;
import org.brailleblaster.utd.actions.GenericAction;
import org.brailleblaster.utd.actions.IAction;
import org.brailleblaster.utd.actions.ItalicsAction;
import org.brailleblaster.utd.actions.SkipAction;
import org.brailleblaster.utd.config.UTDConfig;
import org.brailleblaster.utd.matchers.INodeMatcher;
import org.brailleblaster.utd.matchers.NodeAttributeMatcher;
import org.brailleblaster.utd.matchers.NodeNameMatcher;
import org.brailleblaster.utd.matchers.XPathMatcher;
import org.brailleblaster.utd.testutils.UTDConfigUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import nu.xom.Element;
import nu.xom.XPathContext;

public class ActionMapTest extends NodeMatcherMapTest {
    private ActionMap actionMap;
    private INodeMatcher matcher1;
    private INodeMatcher matcher2;
    private IAction action1;
    private IAction action2;

    @BeforeClass
    public void classSetup() {
        matcher1 = new XPathMatcher("//a");
        matcher2 = new XPathMatcher("//b");
        action1 = new GenericAction();
        action2 = new SkipAction();
        actionMap = new ActionMap();
        actionMap.put(matcher1, action1);
        actionMap.put(matcher2, action2);
    }

    @Test
    public void putNewEntry() {
        // Last added should appear first
        assertSame(actionMap.get(0), matcher2);
        assertSame(actionMap.get(1), matcher1);
    }

    @Test
    public void findAction() {
        Element elementA = new Element("a");
        Element elementB = new Element("b");
        Element elementC = new Element("c");
        assertSame(actionMap.findValueOrDefault(elementA), action1);
        assertSame(actionMap.findValueOrDefault(elementB), action2);
        assertSame(actionMap.findValueOrDefault(elementC), actionMap.getDefaultValue());
        assertNull(actionMap.findValueWithDefault(elementC, null));
        assertSame(actionMap.findValueWithDefault(elementC, action1), action1);
    }

    @Test
    public void putAll() {
        INodeMatcher matcherTmp = new XPathMatcher("//tmp");
        IAction actionTmp = new SkipAction();
        ActionMap cfgTmp = new ActionMap();
        cfgTmp.put(matcherTmp, actionTmp);
        cfgTmp.putAll(actionMap);
        // New items should be first and maintain order for ordered maps
        assertSame(cfgTmp.get(0), matcher2);
        assertSame(cfgTmp.get(1), matcher1);
        assertSame(cfgTmp.get(2), matcherTmp);
    }

    @Test
    public void defaultConstructor() {
        ActionMap actions = new ActionMap();
        assertEquals(actions.size(), 0);
        assertNotNull(actions.getDefaultValue());
        assertTrue(actions.getDefaultValue() instanceof GenericAction);
        assertNotNull(actions.getNamespaces());
    }

    @Test
    public void getAndSetNamespaceMap() {
        super.getAndSetNamespaceMap(new ActionMap());
    }

    @DataProvider
    private Object[][] actionMapDataProvider() {
        ActionMap map = new ActionMap();
        map.getNamespaces().addNamespace("xhtml", "http://www.w3.org/555555/xhtml");

        map.put(new XPathMatcher("//p"), new GenericAction());
        map.put(new XPathMatcher("//b"), new BoldAction());
        map.put(new NodeNameMatcher("img"), new SkipAction());

        return new Object[][]{
                new Object[]{map},
        };
    }

    @Test(dataProvider = "actionMapDataProvider")
    public void saveTest(ActionMap map) throws Exception {
        File tempOutput = File.createTempFile("actionMap", "test");
        UTDConfig.INSTANCE.saveActions(tempOutput, map);
        UTDConfigUtils.compareOutputToSaved(tempOutput, UTDConfigUtils.TEST_ACTION_FILE);
    }

    @Test(dataProvider = "actionMapDataProvider")
    public void loadTest(ActionMap mapExpected) throws Exception {
        //TODO: Only handles ActionMap impl do to get(int)
        ActionMap mapLoaded = UTDConfig.INSTANCE.loadActions(UTDConfigUtils.TEST_ACTION_FILE);

        //Validate action map
        assertEquals(mapLoaded.size(), mapExpected.size(), "Map sizes are different");
        for (int i = 0; i < mapExpected.size(); i++) {
            INodeMatcher matcherExpected = mapExpected.get(i);
            INodeMatcher matcherLoaded = mapLoaded.get(i);
            assertEquals(matcherLoaded.getClass(), matcherExpected.getClass(), "Matcher classes are different at index " + i
                    + System.lineSeparator() + " expected keys " + mapExpected.keyList()
                    + System.lineSeparator() + " loaded keys " + mapLoaded.keyList());

            if (matcherExpected instanceof XPathMatcher) {
                assertEquals(((XPathMatcher) matcherLoaded).getExpression(), ((XPathMatcher) matcherExpected).getExpression(),
                        "XPath expressions are different at index " + i);
            } else if (matcherExpected instanceof NodeNameMatcher) {
                assertEquals(((NodeNameMatcher) matcherLoaded).getNamespace(), ((NodeNameMatcher) matcherExpected).getNamespace(),
                        "Namespace are different at index " + i);
                assertEquals(((NodeNameMatcher) matcherLoaded).getNodeName(), ((NodeNameMatcher) matcherExpected).getNodeName(),
                        "Node name are different at index " + i);
            } else {
                Assert.fail("Matcher " + matcherExpected.getClass() + " not handled");
            }
        }

        //Validate internal namespace map
        assertEquals(mapLoaded.getNamespaces().getPrefixes(), mapExpected.getNamespaces().getPrefixes(), "Namespace prefixes are different");

        //Forcibly rip out the internal map of XPathContext to compare since it doesn't expose any get methods
        //There's not really a better way to do this
        Field xpathInternalMap = XPathContext.class.getDeclaredField("namespaces");
        xpathInternalMap.setAccessible(true);
        Map<?, ?> xpathLoaded = (Map<?, ?>) xpathInternalMap.get(mapLoaded.getNamespaces().getXPathContext());
        Map<?, ?> xpathExpected = (Map<?, ?>) xpathInternalMap.get(mapExpected.getNamespaces().getXPathContext());
        assertEquals(xpathLoaded, xpathExpected, "XPathContext maps are different");
    }

    @Test
    public void testRemoveEntry() {
        ActionMap map = new ActionMap();
        map.put(new NodeNameMatcher("sidebar"), new BoldAction());
        map.put(new NodeAttributeMatcher("sidebar", "box", "true"), new ItalicsAction());
        assertEquals(map.size(), 2);
        assertSame(map.get(0).getClass(), NodeAttributeMatcher.class);
        assertSame(map.get(1).getClass(), NodeNameMatcher.class);
        map.remove(new NodeNameMatcher("sidebar"));
        assertEquals(map.size(), 1);
        assertSame(map.get(0).getClass(), NodeAttributeMatcher.class);
        assertNotNull(map.get(new NodeAttributeMatcher("sidebar", "box", "true")));
    }
}
