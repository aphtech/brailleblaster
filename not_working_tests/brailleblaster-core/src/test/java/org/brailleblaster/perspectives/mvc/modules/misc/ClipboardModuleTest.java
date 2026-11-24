// Methods removed from brailleblaster-core/src/test/java/org/brailleblaster/perspectives/mvc/modules/misc/ClipboardModuleTest.java because they open the lost Jenkins test books
// (/media/brailleblaster/nimas-books/...). Not compiled. See junk/README.md.
// Restore them once new test documents exist.
class ClipboardModuleTest {

    @Test(groups = TestGroups.CLIPBOARD_TESTS, enabled = false)
    public void rt_5842_copy_paste_whitespace_across_sections() {
        /*
         * Go to print page iv of the Literature book. Use ctrl+enter. Now put
         * your cursor before the heading at the top of print page iv and press
         * enter to make a blank line. Now cut the cell-5 heading Contributing
         * Authors and paste it onto that blank line. You will then get the
         * fatal exception below.
         */

        BBTestRunner bbTest = new BBTestRunner(new File(TestFiles.litBook));
        GoToPageTest.goToPage(bbTest, PageType.PRINT, "iv", false);
        bbTest.textViewTools.pressShortcut(SWT.CTRL, SWT.CR);
        bbTest.textViewTools.pressKey(SWT.CR, 1);
        bbTest.textViewTools.navigateToText("Contributing Authors");
        bbTest.textViewTools.selectToEndOfLine();
        pressCut(bbTest);
        bbTest.textViewTools.pressKey(SWT.ARROW_UP, 2);
        pressPaste(bbTest);
    }
}
