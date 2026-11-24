# not_working_tests

Tests that can never run, kept for reference. The folders mirror module paths so `git log --follow` still finds each file's history. `not_working_tests/` is not a Maven module, so nothing here compiles or runs.

See test_fix.md, Phase 2.

| File | Why it's here |
|---|---|
| `brailleblaster-core/.../BenchmarkBooksTest.java` | Benchmark that needs external user books and takes over an hour. Not a regression test. |
| `brailleblaster-core/.../frontmatter/TOCBuilderBookTest.java` | Whole class commented out. Targets the pre-BBX TOC builder; TOC now comes from `TOCBuilderBBX`. |
| `utd/.../utd/TableOfContentsTest.java` | Class under test `TableOfContents.kt` has no production users. TOC now comes from `TOCBuilderBBX`. |
| `utd/.../tables/AutoTableFormatterTest.java` | Body commented out since before open-sourcing. Covered by `HeadingsOnlyTableTest`, `SimpleTableColumnWidthTest` and `TableSpacingTest`. |
| `utd/.../tables/LinearTableFormatterTest.java` | Same as above. |
| `utd/.../tables/ListedTableFormatterTest.java` | Same as above. |
| `utd/.../tables/SimpleTableFormatterTest.java` | Same as above. |
| `utd/.../tables/StairstepTableFormatterTest.java` | Same as above. |
| `utd/.../actions/InsertUncontractedTest.java` | Unfinished stub (asserts `""`) for `InsertUncontractedAction`, which has no production users. |

## Needs new test documents

The Jenkins test books (`/media/brailleblaster/nimas-books/...`) were lost in 2026. These tests opened them, so they can't run anywhere. Bring them back once new documents exist (small, redistributable, under `src/test/resources`). See test_fix.md, Phase 2b.

| File | What's in it |
|---|---|
| `brailleblaster-core/.../document/FindReplaceAcrossSectionsTest.java` | Whole class (litBook, collections). |
| `brailleblaster-core/.../document/FindReplaceReplaceAllTest.java` | `replaceAllAttributionsCollections`, `replaceAllLargeDocumentLitBook`, `replaceAllLargeDocumentCollections`, `replaceAllBoldUnbold`. |
| `brailleblaster-core/.../document/FindReplaceStylesTest.java` | `rt6100_replace_split_text_node_and_make_style` (litBook). |
| `brailleblaster-core/.../misc/ClipboardModuleTest.java` | `rt_5842_copy_paste_whitespace_across_sections` (litBook). |
| `brailleblaster-core/.../printView/EditingTest.java` | `type_translate_collections_rt6550` (litBook). |
| `brailleblaster-core/.../printView/TransNoteEmphasisTest.java` | `trans_note_collections` (collections). |
| `brailleblaster-core/.../search/GoToPageTest.java` | `goToAll_simple`, `goToAll_volumes` and their helper `visitPages` (litBook). |
| `brailleblaster-core/.../frontmatter/VolumeTest.java` | `insertManyVolumes`, `insertManyVolumes_byPage` (litBook). |

The partial files hold only the removed methods, wrapped in a class of the same name. To restore one, paste the methods back into the live class. `TestFiles.litBook` / `collections` and `TestGroups.SLOW_TESTS` were removed with them; the paths are in this folder's copies.

## Deleted outright instead of moved (git history keeps them):

- `TOCBuilderTest`: the commented-out block of 5 `BROKEN_TESTS` methods that called `TOCBuilderBookTest`.
- `VolumeTest.slowInsertVolumeEverywhereTest` and its DataProvider: commented-out benchmark, over an hour per run.
- `BBTest.splitHeadingTest`: its own comment said "No idea what this is testing".
- `StyleStackTest.findDontSplit`: commented out; its comment says the behaviour is intentionally obsolete.
- `DocumentUTDConfigTest`: the commented `NimasUTDConfig::loadActions` row; that class no longer exists.
- `TestGroups.BENCHMARK`: unused constant.
