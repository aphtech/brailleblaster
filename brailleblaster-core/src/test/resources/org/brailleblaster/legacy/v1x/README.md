# BrailleBlaster 1.x test files

Used by `LegacyFormatDetectionTest`, `LegacyBbxUpgraderTest` and `LegacyArchiverLoadTest`.

| Folder | What it is |
|---|---|
| `documents/` | Real files from BrailleBlaster 1.x (test and template files from the 1.x source history; `PROVENANCE.txt` lists the commit each came from). The name prefix is the BBX format version in the file (`v1_` to `v6_`). `math_V1*.bbx` are 1.x spatial math files (format 6). |
| `upgrade-pairs/` | 1.x's own upgrade test data: each `*.source.bbx` upgraded to the current format should match its `*.expected.bbx` upgraded to the current format. |
| `edge/` | Small hand-written files: no version element, a page number with no block around it (makes the 3 to 4 step fail), tabs in text, and an unknown block subtype. |
| `bbz/` | `.bbz` archives (`bbx_location` + `document.bbx`) wrapping two of the documents. |

BBX format history: v1 2016-05, v2 2016-07, v3 2017-02 (#5320), v4 2017-06 (#6032), v5 2017-07 (#6163), v6 2017-10 (#6629). BrailleBlaster 3.x still writes format 6.
