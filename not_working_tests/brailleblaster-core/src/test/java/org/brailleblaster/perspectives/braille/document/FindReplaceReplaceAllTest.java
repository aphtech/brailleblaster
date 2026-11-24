// Methods removed from brailleblaster-core/src/test/java/org/brailleblaster/perspectives/braille/document/FindReplaceReplaceAllTest.java because they open the lost Jenkins test books
// (/media/brailleblaster/nimas-books/...). Not compiled. See junk/README.md.
// Restore them once new test documents exist.
class FindReplaceReplaceAllTest {

    @Test(enabled = false)
    public void replaceAllAttributionsCollections() {
        BBTestRunner bb = new BBTestRunner(new File(collections));
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);
        FindReplaceBotHelper.addStylesAndContainers(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(0).select("Attribution");
        FindReplaceBotHelper.modifyFind(bb);
        FindReplaceBotHelper.addStylesAndContainers(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(0).select("Centered Heading");
        FindReplaceBotHelper.modifyReplace(bb);
        FindReplaceBotHelper.done(bb);
        long start = FindAndReplaceTest.startTimer();
        FindReplaceBotHelper.clickReplaceAll(bb);
        SearchController.logIt(bot.getDisplay().getActiveShell().getText() + " is the active shell.");
        String replaced = TestUtils.refreshReturnActiveBot(bb).label(1).getText();
        SearchController.logIt(replaced + " the label");
        String[] replacedMessage = replaced.split(" ");
        // This is the message "229 instances replaced"
        String stringNumber = replacedMessage[0];
        int numReplaced = Integer.parseInt(stringNumber);
        long minutes = FindAndReplaceTest.getTotalTime(start);
        SearchController.logIt("Total time: " + minutes + " minutes. ");
        assertEquals(numReplaced, 6);
    }

    @Test(enabled = false)
    public void replaceAllLargeDocumentLitBook() {
        // These paths exist on Jenkins and the testFast scripts
        String litBook = "/media/brailleblaster/nimas-books/9780133268195NIMAS_revised.xml";
        BBTestRunner bb = new BBTestRunner(new File(litBook));
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        bot.comboBox(0).setText("literature");
        bot.comboBox(1).setText("success");
        long start = FindAndReplaceTest.startTimer();
        FindReplaceBotHelper.clickReplaceAll(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        String replaced = bot.label(1).getText();
        String[] replacedMessage = replaced.split(" ");
        // This is the message "229 instances replaced"
        String stringNumber = replacedMessage[0];
        int numReplaced = Integer.parseInt(stringNumber);
        long minutes = FindAndReplaceTest.getTotalTime(start);
        SearchController.logIt("Total time: " + minutes + " minutes. ");
        assertEquals(numReplaced, 229);
    }

    @Test(enabled = false)
    public void replaceAllLargeDocumentCollections() {
        /*
         * 40 instances of 'teacher', 3 in tables
         */
        BBTestRunner bb = new BBTestRunner(new File(collections));

        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        bot.comboBox(0).setText("teacher");
        bot.comboBox(1).setText("success");
        long start = FindAndReplaceTest.startTimer();
        FindReplaceBotHelper.clickReplaceAll(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        String replaced = bot.label(1).getText();
        String[] replacedMessage = replaced.split(" ");
        // This is the message "229 instances replaced"
        String stringNumber = replacedMessage[0];
        int numReplaced = Integer.parseInt(stringNumber);
        long minutes = FindAndReplaceTest.getTotalTime(start);
        SearchController.logIt("Total time: " + minutes + " minutes. ");
        assertEquals(numReplaced, 37);
    }

    @Test(enabled = false)
    public void replaceAllBoldUnbold() {
        /*
         * The word opportunity in Collections book -- 1 bold, 10 unbold, 3 in
         * tables
         */
        BBTestRunner bb = new BBTestRunner(new File(collections));
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = bb.bot.activeShell().bot();
        bot = FindAndReplaceTest.cleanSettings(bot, bb);
        FindReplaceBotHelper.clickFormatting(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.list(2).select("BOLD");
        FindReplaceBotHelper.modifyFind(bb);
        FindReplaceBotHelper.done(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText("opportunity");
        bot.comboBox(1).setText("success");
        long start = FindAndReplaceTest.startTimer();
        FindReplaceBotHelper.clickReplaceAll(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        String replaced = bot.label(1).getText();

        String[] replacedMessage = replaced.split(" ");
        // This is the message "229 instances replaced"
        String stringNumber = replacedMessage[0];
        int numReplaced = Integer.parseInt(stringNumber);

        long minutes = FindAndReplaceTest.getTotalTime(start);

        SearchController.logIt("Total time: " + minutes + " minutes. ");
        assertEquals(numReplaced, 1);
        FindReplaceBotHelper.clickOk(bb);

        // __________Replace all unbold______________

        FindReplaceBotHelper.clickReset(bb);
        bot = TestUtils.refreshReturnActiveBot(bb);
        bot.comboBox(0).setText("opportunity");
        bot.comboBox(1).setText("success");
        start = FindAndReplaceTest.startTimer();
        FindReplaceBotHelper.clickReplaceAll(bb);

        bot = TestUtils.refreshReturnActiveBot(bb);
        replaced = bot.label(1).getText();

        replacedMessage = replaced.split(" ");
        // This is the message "229 instances replaced"
        stringNumber = replacedMessage[0];
        numReplaced = Integer.parseInt(stringNumber);

        minutes = FindAndReplaceTest.getTotalTime(start);

        SearchController.logIt("Total time: " + minutes + " minutes. ");
        assertEquals(numReplaced, 10);
    }
}
