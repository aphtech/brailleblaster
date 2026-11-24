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
package org.brailleblaster

import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.readBytes
import kotlin.io.path.relativeTo

/**
 * Fails when a test class opens SWT windows but is not marked [GuiTest], because it would then
 * run (and take over the desktop) in the default headless build.
 *
 * Scans the compiled test classes, so commented-out code is ignored and Kotlin is covered too.
 */
class GuiGroupTaggingTest {
    @Test
    fun everyGuiTestClassIsMarkedGuiTest() {
        val untagged = untaggedGuiTestClasses(testClassesDir())
        assertTrue(
            untagged.isEmpty(),
            "Test classes that open SWT windows must be marked @GuiTest:\n  " + untagged.joinToString("\n  ")
        )
    }

    @Test
    fun detectsUntaggedGuiClass() {
        // Sanity check that the scan finds a GUI reference at all, so the test above can't pass vacuously.
        val guiClasses = guiReferencingClasses(testClassesDir())
        assertTrue("org.brailleblaster.printView.EditAfterPageBreakTest" in guiClasses, "scan found: $guiClasses")
        assertEquals(isGuiTestClass("org.brailleblaster.printView.EditAfterPageBreakTest"), true)
        assertEquals(isGuiTestClass(GuiGroupTaggingTest::class.java.name), false)
    }

    companion object {
        /** Constant-pool strings that mean a class drives a real SWT window. */
        private val GUI_MARKERS = listOf(
            "org/brailleblaster/testrunners/BBTestRunner",
            "org/brailleblaster/testrunners/BBViewTestRunner",
            "org/eclipse/swtbot/",
            "org/eclipse/swt/widgets/Shell",
        )

        private fun testClassesDir(): Path =
            Path.of(GuiGroupTaggingTest::class.java.protectionDomain.codeSource.location.toURI())

        /** Top-level class names (inner and lambda classes folded into their owner) that reference a GUI marker. */
        fun guiReferencingClasses(root: Path): Set<String> =
            Files.walk(root).use { paths ->
                paths
                    .filter { it.extension == "class" }
                    .filter { path -> String(path.readBytes(), Charsets.ISO_8859_1).let { bytes -> GUI_MARKERS.any { it in bytes } } }
                    .map { it.relativeTo(root).invariantSeparatorsPathString.removeSuffix(".class").substringBefore('$').replace('/', '.') }
                    .toList()
                    .toSortedSet()
                    // this class holds the marker strings itself
                    .minus(GuiGroupTaggingTest::class.java.name)
                    .toSortedSet()
            }

        private fun loadClass(name: String): Class<*> =
            Class.forName(name, false, GuiGroupTaggingTest::class.java.classLoader)

        private fun hasTests(name: String): Boolean = loadClass(name).let { cls ->
            cls.isAnnotationPresent(Test::class.java) ||
                cls.declaredMethods.any { it.isAnnotationPresent(Test::class.java) }
        }

        fun isGuiTestClass(name: String): Boolean = loadClass(name).isAnnotationPresent(GuiTest::class.java)

        fun untaggedGuiTestClasses(root: Path): List<String> =
            guiReferencingClasses(root)
                .filter(::hasTests)
                .filterNot(::isGuiTestClass)
    }
}
