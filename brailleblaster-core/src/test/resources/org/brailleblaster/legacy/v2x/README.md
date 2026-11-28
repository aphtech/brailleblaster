# BrailleBlaster 2.0 test files

Used by `V2ToBBXConverterTest` and `V2ArchiverLoadTest`.

BrailleBlaster 2.0 (2019 to 2.0.42 in 2022) wrote its own flat format: `bbdoc/head` + `blocklist/block/tme`, `<version>2.3</version>` (2.0's `bbx.xsd`). Every 2.0 release wrote version 2.3.

| Folder | What it is |
|---|---|
| `real/` | Real files from the 2.0 source history (`PROVENANCE.txt` lists the commit each came from). Paragraphs, headings, lists, listed tables (including a sparse one and one whose last cell comes after other blocks), images, print page numbers, linear math, translated braille stored as `utd:brl`, and a version with whitespace around it. |
| `synthetic/` | Hand-written from 2.0.42's `bbx.xsd` and its `TPageBlock`, `TocBlock`, `VolumeBlock` and TME classes, covering what the real files do not: every emphasis value, headings and style options, lists with levels, poetry, simple and stairstep tables with a TN, title page, table of contents, volumes, page type indicator, image placeholder and description, inline MathML, spatial math, boxlines, separation line, page break, tab, line number, guide word, pronunciation, and page settings in the head. |
| `fallback/` | Unknown tme type, unknown block type, unknown style, broken table grid, invalid MathML, invalid list level, empty blocklist, version 2.1, version with whitespace, and attributes 2.0 never documented. Each must open with its text kept. |
| `bbz/` | `.bbz` archives wrapping some of the files above, plus one with no `bbx_location` (2.0 then looked for any `*.bbx` in the archive). |

Things real 2.0 files do that the converter handles: style names with underscores (`Body_Text`), emphasis in any case and `italic` as well as `italics`, extra attributes (`utd-style`, `row-col`), table cells grouped by `gid` with empty cells left out, several blocks at one table position, and `utd:brl` children holding translated braille.
