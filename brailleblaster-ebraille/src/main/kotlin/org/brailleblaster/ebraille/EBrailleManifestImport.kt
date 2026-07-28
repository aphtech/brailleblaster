/*
 * Copyright (C) 2026 American Printing House for the Blind
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
package org.brailleblaster.ebraille

import nu.xom.Document
import org.brailleblaster.archiver2.ImportedSourceMetadata

/**
 * Overlays bibliographic metadata captured from the book's original NIMAS/EPUB source (see
 * [ImportedSourceMetadata]) onto a manifest, filling in only the fields the source can justify
 * and only where a higher-precedence value (an authored one) isn't already present.
 */
internal fun EBrailleManifest.withImportedSourceMetadata(doc: Document): EBrailleManifest {
    val imported = ImportedSourceMetadata.load(doc)
    if (imported.isEmpty) {
        return this
    }

    val importedTitle = imported.title?.let { ManifestValue(it, ManifestValueSource.IMPORTED) }
    val importedCreators = imported.creators.map { ManifestValue(it, ManifestValueSource.IMPORTED) }
    val importedIdentifier = imported.identifier?.let { ManifestValue(it, ManifestValueSource.IMPORTED) }
    val importedDate = imported.date?.let { ManifestValue(it, ManifestValueSource.IMPORTED) }

    return copy(
        title = ManifestValuePrecedence.choose(title, importedTitle) ?: title,
        creators = ManifestValuePrecedence.chooseList(creators, importedCreators),
        identifier = ManifestValuePrecedence.choose(identifier, importedIdentifier) ?: identifier,
        date = ManifestValuePrecedence.choose(date, importedDate) ?: date
    )
}
