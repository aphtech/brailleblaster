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
package org.brailleblaster.archiver2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.brailleblaster.BBIni;
import org.testng.annotations.Test;

public class ArchiverRecoveryThreadTest {
	
	@Test(enabled = false)
	public void garbageRecoveryFile_rt6844() throws IOException {
		FileUtils.write(BBIni.getRecentSaves().toFile(), "\0\0\0\0", StandardCharsets.UTF_8, true);
		
		// should not fail
		ArchiverRecoverThread.Companion.readRecentSaves();
	}
}
