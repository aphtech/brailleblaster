// Methods removed from brailleblaster-core/src/test/java/org/brailleblaster/search/GoToPageTest.java because they open the lost Jenkins test books
// (/media/brailleblaster/nimas-books/...). Not compiled. See junk/README.md.
// Restore them once new test documents exist.
class GoToPageTest {

    @Test(groups = TestGroups.SLOW_TESTS, enabled = false)
    public void goToAll_simple() {
//		BBTestRunner bbTest = new BBTestRunner(new File("/home/leon/projects/aph/linuxdev/Combined Pages 14-15.bbx"));
        BBTestRunner bbTest = new BBTestRunner(new File(TestFiles.litBook));
        visitPages(bbTest, false);
    }

    @Test(groups = TestGroups.SLOW_TESTS, enabled = false)
    public void goToAll_volumes() {
//		BBTestRunner bbTest = new BBTestRunner(new File("/home/leon/projects/aph/linuxdev/Combined Pages 14-15.bbx"));
        BBTestRunner bbTest = new BBTestRunner(new File(TestFiles.litBook));

        for (int i = 1; i < 15; i++) {
            goToPage(bbTest, GoToPageDialog.PageType.ORDINAL, "" + (i * 100), false);
            bbTest.updateTextView();
            bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 2);

            BBX.VolumeType volumeType;
            if (i < 6) {
                volumeType = BBX.VolumeType.VOLUME_PRELIMINARY;
            } else if (i < 10) {
                volumeType = BBX.VolumeType.VOLUME;
            } else {
                volumeType = BBX.VolumeType.VOLUME_SUPPLEMENTAL;
            }
            VolumeTest.openInsertVolume(bbTest, volumeType);
        }

        visitPages(bbTest, true);
    }

// Helper used only by the methods above
    public SWTBot visitPages(BBTestRunner bbTest, boolean selectVolume) {
        for (SectionElement curSection : bbTest.manager.getSectionList()) {
            for (TextMapElement curMapping : curSection.list) {
                if (curMapping instanceof PageIndicatorTextMapElement) {
                    Element usableNode = (Element) (curMapping.getNode() instanceof Element
                            ? curMapping.getNode()
                            : curMapping.getNodeParent());
                    String brlPageNum = usableNode.getAttributeValue("printPage");
                    if (brlPageNum != null) {
                        System.out.println("PageMapElement: " + brlPageNum);
                        return goToPage(bbTest, GoToPageDialog.PageType.PRINT, brlPageNum, selectVolume);
                    } else {
                        throw new RuntimeException("uhh...");
                    }
                } else {
                    for (BrailleMapElement curBrailleElement : curMapping.brailleList) {
                        try {
                            if (curBrailleElement instanceof PrintPageBrlMapElement) {
                                //Check both the origional page and the translated braille equivelent
                                String origPage = ((Element) curBrailleElement.getNode()).getAttributeValue("printPage");
                                String braillePage = curBrailleElement.getNode().getValue();

                                System.out.println("PrintPageMapElement: " + origPage + " - " + braillePage);
                                return goToPage(bbTest, GoToPageDialog.PageType.PRINT, origPage, /*can only select braille*/false);
                            } else if (curBrailleElement instanceof BraillePageBrlMapElement) {
                                String page = ((Element) curBrailleElement.getNode()).getAttributeValue("untranslated");

                                System.out.println("BraillePageMapElement: " + page + " " + (curBrailleElement.getNode()).toXML());
                                return goToPage(bbTest, GoToPageDialog.PageType.BRAILLE, page, selectVolume);
                            }
                        } catch (Exception e) {
                            log.error("this is test", e);
                            ViewTestRunner.DEBUG_MODE();
                            throw new NodeException("fail", curBrailleElement.getNode(), e);
                        }
                    }
                }
            }
        }
        throw new RuntimeException("not found");
    }
}
