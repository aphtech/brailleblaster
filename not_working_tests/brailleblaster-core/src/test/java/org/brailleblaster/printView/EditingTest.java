// Methods removed from brailleblaster-core/src/test/java/org/brailleblaster/printView/EditingTest.java because they open the lost Jenkins test books
// (/media/brailleblaster/nimas-books/...). Not compiled. See junk/README.md.
// Restore them once new test documents exist.
class EditingTest {

    @Test(enabled = false)
    public void type_translate_collections_rt6550() {
        BBTestRunner bbTest = new BBTestRunner(new File(TestFiles.litBook));
        //go to braille page 1
        GoToPageTest.goToPage(bbTest, GoToPageDialog.PageType.BRAILLE, "1", false);
        //go to line after "Easily capture notes and complete assignments online"
        String passage = "assignments online";
        bbTest.textViewTools.navigateToText(passage, 0);
        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        //type test
        bbTest.textViewTools.typeText("test");
        //arrow down to retranslate
        bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 1);
        //no exception thrown
    }
}
