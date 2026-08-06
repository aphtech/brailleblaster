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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import static org.testng.Assert.*;

public class InsertionPatternEntryTest {
    @Test
    public void testEquals() {
        InsertionPatternEntry i1 = new InsertionPatternEntry("aa", "bb");
        InsertionPatternEntry i2 = new InsertionPatternEntry("aa", "bb");
        InsertionPatternEntry i3 = new InsertionPatternEntry("aa", "bb");
        InsertionPatternEntry i4 = new InsertionPatternEntry("-", "\"");
        assertEquals(i2, i1);
        assertEquals(i3, i1);
        assertEquals(i3, i2);
        assertEquals(i1, i3);
        assertEquals(i1, i2);
        assertNotEquals(i4, i1);
        assertNotEquals(i1, i4);
        assertNotEquals(i4, i2);
        assertNotEquals(i4, i3);
    }

    @Test
    public void listToMap() {
        List<InsertionPatternEntry> listInsertions = Lists.newArrayList(new InsertionPatternEntry("-", "#"), new InsertionPatternEntry("0", ";"), new InsertionPatternEntry("j", ";"));
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("-", "#");
        expected.put("0", ";");
        expected.put("j", ";");
        Map<String, String> actual = InsertionPatternEntry.Companion.listToMap(listInsertions);
        assertEquals(actual.size(), expected.size());
        Iterator<Map.Entry<String, String>> expectedSet = expected.entrySet().iterator();
        Iterator<Map.Entry<String, String>> actualSet = actual.entrySet().iterator();
        while (expectedSet.hasNext() && actualSet.hasNext()) {
            Map.Entry<String, String> actualEntry = actualSet.next();
            Map.Entry<String, String> expectedEntry = expectedSet.next();
            assertEquals(actualEntry.getKey(), expectedEntry.getKey());
            assertEquals(actualEntry.getValue(), expectedEntry.getValue());
        }
    }

    @Test
    public void listToMapWithNull() {
        Map<String, String> actual = InsertionPatternEntry.Companion.listToMap(null);
        assertTrue(actual.isEmpty());
    }
}
