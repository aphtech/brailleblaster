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
package org.brailleblaster.firstrun

import org.brailleblaster.BBIni
import org.brailleblaster.utils.BBData.getBrailleblasterPath
import org.brailleblaster.utils.PropertyFileManager
import org.brailleblaster.utils.localization.LocaleHandler
import org.eclipse.jface.layout.GridDataFactory
import org.eclipse.jface.widgets.ButtonFactory
import org.eclipse.jface.widgets.CompositeFactory
import org.eclipse.jface.widgets.TextFactory
import org.eclipse.jface.wizard.WizardPage
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import java.io.IOException

const val LICENSE_ACCEPTED_SETTING = "licenseAccepted"
const val CURRENT_LICENSE_ID = "GPL-3.0-only"
private const val PAGE_TITLE = "LicensePage.title"
private const val PAGE_DESCRIPTION = "LicensePage.description"
private const val CHECKBOX_TEXT= "LicensePage.checkbox"

class LicencePage(private val userSettings: PropertyFileManager = BBIni.propertyFileManager) : WizardPage(LocaleHandler.getDefault()[PAGE_TITLE]), ActionOnFinishWizardPage {
  override fun createControl(parent: Composite?) {
    //CompositeFactory is neat, but holy crap is the code hard to read if not broken down into smaller chunks.
    // Sighted person problems...
    control = CompositeFactory.newComposite(SWT.NONE)
      .layout(GridLayout(1, true))
      .layoutData(GridDataFactory.fillDefaults().create())
      .create(parent).also {
        //This code taken from UserHelp
        val licensePath = getBrailleblasterPath("LICENSE.txt")
        val licenseText: String = try {
          licensePath.readText(Charsets.UTF_8)
        } catch (ex: IOException) {
          throw RuntimeException("Unable to open license at " + licensePath.absolutePath, ex)
        }

        //Sizes are hard-coded intentionally - Text box will block the view of the checkbox
        TextFactory.newText(SWT.BORDER or SWT.V_SCROLL or SWT.WRAP or SWT.READ_ONLY)
          .text(licenseText)
          .layoutData(GridDataFactory.fillDefaults().hint(500,350).create())
          .create(it)

        ButtonFactory.newButton(SWT.CHECK)
          .text(LocaleHandler.getDefault()[CHECKBOX_TEXT])
          .layoutData(GridDataFactory.fillDefaults().hint(100, 100).create())
          .onSelect { isPageComplete = !isPageComplete }
          .create(it)
      }
    //Does this even help? Not so sure...
    control.pack()
  }

  override fun performFinish(): Boolean {
    userSettings.save(LICENSE_ACCEPTED_SETTING, CURRENT_LICENSE_ID)
    return true
  }

  init{
    isPageComplete = false
    title = LocaleHandler.getDefault()[PAGE_TITLE]
    description = LocaleHandler.getDefault()[PAGE_DESCRIPTION]
  }
}