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

/**
 * Marks a test class that opens SWT windows (BBTestRunner, SWTBot, Shell, ...).
 *
 * [GuiGroupTransformer] adds [TestGroups.GUI] to every `@Test` in the class, so the default
 * build skips it and `-Dgui` runs it. A class-level `@Test(groups = GUI)` is not used because
 * it would turn every public helper method into a test.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GuiTest
