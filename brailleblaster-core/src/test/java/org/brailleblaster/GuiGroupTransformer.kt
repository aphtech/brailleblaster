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

import org.testng.IAnnotationTransformer
import org.testng.annotations.ITestAnnotation
import java.lang.reflect.Constructor
import java.lang.reflect.Method

/**
 * Adds the [TestGroups.GUI] group to every `@Test` declared in a [GuiTest] class.
 * Registered as a TestNG listener in brailleblaster-core/pom.xml.
 */
class GuiGroupTransformer : IAnnotationTransformer {
    override fun transform(
        annotation: ITestAnnotation,
        testClass: Class<*>?,
        testConstructor: Constructor<*>?,
        testMethod: Method?
    ) {
        val owner = testClass ?: testMethod?.declaringClass ?: testConstructor?.declaringClass
        if (owner?.isAnnotationPresent(GuiTest::class.java) == true && TestGroups.GUI !in annotation.groups) {
            annotation.groups = annotation.groups + TestGroups.GUI
        }
    }
}
