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
package org.brailleblaster.utils.xom

import nu.xom.Document
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class NodeSorterTest {
    @Test(dataProvider = "nodesProvider")
    fun testSortByDocumentOrderSameTree(inputNodes: List<Node>, expectedNodes: List<Node>) {
        val actualNodes: List<Node> = NodeSorter.sortByDocumentOrder(inputNodes)
        Assert.assertNotNull(actualNodes)
        Assert.assertEquals(actualNodes.size, expectedNodes.size)
        for (i in actualNodes.indices) {
            val expected = expectedNodes[i]
            val actual = actualNodes[i]
            Assert.assertSame(actual, expected)
        }
    }

    @DataProvider
    fun nodesProvider(): MutableIterator<Array<Any>> {
        val data: MutableList<Array<Any>> = ArrayList()
        var root = Element("root")
        Document(root)
        var input: List<Node> = listOf(root)
        var expected: List<Node> = listOf(root)
        data.add(arrayOf(input, expected))
        var t1 = Text("Hello world")
        root = Element("root")
        root.appendChild(t1)
        Document(root)
        input = listOf(root, t1)
        expected = listOf(root, t1)
        data.add(arrayOf(input, expected))
        input = listOf(t1, root)
        data.add(arrayOf(input, expected))
        t1 = Text("Hello")
        var t2 = Text("World")
        root = Element("p")
        root.appendChild(t1)
        root.appendChild(t2)
        Document(root)
        input = listOf(t1, t2)
        expected = listOf(t1, t2)
        data.add(arrayOf(input, expected))
        input = listOf(t2, t1)
        data.add(arrayOf(input, expected))
        input = listOf(root, t2, t1)
        expected = listOf(root, t1, t2)
        data.add(arrayOf(input, expected))
        t1 = Text("Hello")
        t2 = Text(", ")
        val t3 = Text("world")
        val e1 = Element("b")
        e1.appendChild(t2)
        root = Element("p")
        root.appendChild(t1)
        root.appendChild(e1)
        root.appendChild(t3)
        Document(root)
        input = listOf(e1, t3, t2, t1, root)
        expected = listOf(root, t1, e1, t2, t3)
        data.add(arrayOf(input, expected))
        return data.iterator()
    }
}