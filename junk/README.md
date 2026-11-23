# junk

Tests that can never run, kept for reference. The folders mirror module paths so `git log --follow` still finds each file's history. `junk/` is not a Maven module, so nothing here compiles or runs.

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

Deleted outright instead of moved (git history keeps them):

- `TOCBuilderTest`: the commented-out block of 5 `BROKEN_TESTS` methods that called `TOCBuilderBookTest`.
- `VolumeTest.slowInsertVolumeEverywhereTest` and its DataProvider: commented-out benchmark, over an hour per run.
- `BBTest.splitHeadingTest`: its own comment said "No idea what this is testing".
- `StyleStackTest.findDontSplit`: commented out; its comment says the behaviour is intentionally obsolete.
- `DocumentUTDConfigTest`: the commented `NimasUTDConfig::loadActions` row; that class no longer exists.
- `TestGroups.BENCHMARK`: unused constant.
