// Methods removed from brailleblaster-core/src/test/java/org/brailleblaster/perspectives/braille/document/FindReplaceStylesTest.java because they open the lost Jenkins test books
// (/media/brailleblaster/nimas-books/...). Not compiled. See junk/README.md.
// Restore them once new test documents exist.
class FindReplaceStylesTest {

    @Test(enabled = false)
    public void rt6100_replace_split_text_node_and_make_style() {

        BBTestRunner bb = new BBTestRunner(new File(TestFiles.litBook));
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        FindReplaceBotHelper.clickFormatting(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(0).select("Centered Text");
        FindReplaceBotHelper.modifyReplace(bb);
        FindReplaceBotHelper.done(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText("States");
        bot.comboBox(1).setText("steak");
        FindReplaceBotHelper.clickFind(bb);
        FindReplaceBotHelper.clickReplace(bb);
        String afterXml = TestXMLUtils.toXMLnoNS(bb.getDoc().getRootElement());
        log.debug("Log Replaced Text " + afterXml);
        String textNode = bb.getDoc().getChild(0).getChild(1).getChild(0).getChild(3).getChild(3).getChild(3).getValue();
        TestUtils.getInnerSection(bb, true).child(0).child(3).child(3).hasOverrideStyle("Centered Text");
        Assert.assertTrue(textNode.contains("steak"));
    }
}
