// Methods removed from brailleblaster-core/src/test/java/org/brailleblaster/printView/TransNoteEmphasisTest.java because they open the lost Jenkins test books
// (/media/brailleblaster/nimas-books/...). Not compiled. See junk/README.md.
// Restore them once new test documents exist.
class TransNoteEmphasisTest {

    /*
     * Just open the collections book and add a transcriber note rt 6539
     */
    @Test(enabled = false)
    public void trans_note_collections() {
        BBTestRunner bbTest = new BBTestRunner(new File(TestFiles.collections));
        String expectedBraille = ",h>c`.<|rt ,p`.>ubli%+ ,company";

        bbTest.navigateTextView(bbTest.textViewWidget.getOffsetAtLine(6) + 4);
        bbTest.textViewTools.selectRight(6);
        bbTest.openMenuItem(TopMenu.EMPHASIS, EmphasisItem.TNSYMBOLS.longName);

        assertEquals(bbTest.brailleViewBot.getTextOnLine(6), expectedBraille);
    }
}
