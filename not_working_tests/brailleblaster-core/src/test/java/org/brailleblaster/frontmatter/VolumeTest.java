// Methods removed from brailleblaster-core/src/test/java/org/brailleblaster/frontmatter/VolumeTest.java because they open the lost Jenkins test books
// (/media/brailleblaster/nimas-books/...). Not compiled. See junk/README.md.
// Restore them once new test documents exist.
class VolumeTest {

	@Test(enabled = false)
	public void insertManyVolumes() {
		// While the literature book is in the repo, might as well use it as its much bigger
//		BBTestRunner bbTest = new BBTestRunner(new File("src/tests/resources/nimasbaseline/NIMASXMLGtDepJan2009_valid3.xml"));
//		int NUM_VOLUMES = 10;
		BBTestRunner bbTest = new BBTestRunner(new File("/media/brailleblaster/nimas-books/9780133268195NIMAS_revised.xml"));
		int NUM_VOLUMES = 20;

		int textNodeCount = (int) StreamSupport.stream(((Iterable<Node>)FastXPath.descendant(BBX.getRoot(bbTest.getDoc()))::iterator).spliterator(), false)
				.filter(curNode -> curNode instanceof Text)
				.count();
		int textNodesPerVolume = textNodeCount / NUM_VOLUMES;
		log.trace("{} textNodes / {} Volumes = {} nodes per volume",
				NUM_VOLUMES,
				textNodeCount,
				textNodesPerVolume
		);

		List<Node> volumizedNodes = new LinkedList<>(); //Keeps track of insertions in case test fails
		Element startNode = BBX.getRoot(bbTest.getDoc());
		startNode.addAttribute(new Attribute("insertManyVolumes", "mee"));
		Outer:
		for (int i = 0; i < NUM_VOLUMES - 1; i++) {
			ViewTestRunner.doPendingSWTWork();
			bbTest.manager.waitForFormatting(true);
			ViewTestRunner.doPendingSWTWork();
			//find usable text node to scroll to
			log.debug("startNode doc " + startNode.getDocument());
			Element startNodeProcessing = StreamSupport.stream(FastXPath.descendantAndFollowing(bbTest.getDoc()).spliterator(), false)
					.filter(curNode -> curNode instanceof Element && ((Element) curNode).getAttribute("insertManyVolumes") != null)
					.findFirst()
					.map(curNode -> (Element) curNode)
					.orElseThrow(() -> new NodeException("failed to find insertManyVolumes", bbTest.getDoc()));
			startNodeProcessing.removeAttribute(startNodeProcessing.getAttribute("insertManyVolumes"));
			int endOffset = -1;
			Text nodeToVolumize = null;
			do {
				endOffset++;
				nodeToVolumize = (Text) StreamSupport.stream(FastXPath.descendantAndFollowing(startNodeProcessing).spliterator(), false)
                        .filter(curNode -> curNode instanceof Text)
                        .skip(textNodesPerVolume + endOffset)
                        .filter(curNode -> !BBXUtils.INSTANCE.isPageNumAncestor(curNode))
                        .findFirst().orElse(null);
				if (nodeToVolumize == null) {
					//end of document
					break Outer;
				}
			} while (XMLHandler.Companion.ancestorVisitor(
					nodeToVolumize,
					curNode -> BBX.CONTAINER.TABLE.isA(curNode)
					|| BBX.CONTAINER.VOLUME.isA(curNode)
					|| UTDElements.BRL.isA(curNode)
					//workaround for lambda final variable nonsense
					|| (curNode instanceof Text && curNode.getValue().isEmpty())
			) != null);
			Element parent = (Element) nodeToVolumize.getParent();
			parent.addAttribute(new Attribute("insertManyVolumes", "mee"));
			startNode = parent;

			log.debug("Search #{} for {} in {}",
					i,
					nodeToVolumize,
					nodeToVolumize.getParent().toXML()
			);

			ViewTestRunner.doPendingSWTWork();
			volumizedNodes.add(nodeToVolumize);

			try{
				/*
				TODO #4713: Ideally this should not be needed. But without it there's 
				a significant number of thread racing and deadlocks, more than there
				was before when this was commented out
				*/
				bbTest.manager.waitForFormatting(true);
				ViewTestRunner.doPendingSWTWork();
				bbTest.manager.getSimpleManager().dispatchEvent(new XMLCaretEvent(Sender.SIMPLEMANAGER, new XMLTextCaret(nodeToVolumize, 0)));
				ViewTestRunner.doPendingSWTWork();
				bbTest.manager.waitForFormatting(true);
				ViewTestRunner.doPendingSWTWork();
				
				//sanity check
				String firstWord = nodeToVolumize.getValue();
				if (firstWord.contains(" ")) {
					firstWord = firstWord.substring(0, firstWord.indexOf(' '));
				}
				ViewTestRunner.doPendingSWTWork();
				assertThat(bbTest.textViewBot.getTextOnCurrentLine(), containsString(firstWord));
				if (true) {
	//				bbTest.manager.waitForFormatting(true);
				}
				openInsertVolume(bbTest, BBX.VolumeType.VOLUME);
				ViewTestRunner.doPendingSWTWork();
			} catch (Exception e){
				StringBuilder sb = new StringBuilder();
				for(int nodeNum = 0; nodeNum < volumizedNodes.size(); nodeNum++){
					sb.append(nodeNum).append(": ").append(volumizedNodes.get(nodeNum).toXML()).append(nodeNum == volumizedNodes.size() - 1 ? "" : System.lineSeparator());
				}
				throw new RuntimeException("Exception after inserting following volumes:" + System.lineSeparator() + sb, e);
			}
			//bbTestRunner reset?
			bbTest.textViewBot.setFocus();
			ViewTestRunner.doPendingSWTWork();

			bbTest.textViewBot.navigateTo(0, 0);
			ViewTestRunner.doPendingSWTWork();
		}
	}

	@Test(enabled = false )
	public void insertManyVolumes_byPage() {
		BBTestRunner test = new BBTestRunner(new File("/media/brailleblaster/nimas-books/9780133268195NIMAS_revised.xml"));
		int PRINT_PAGE_COUNTER = 100;
		
		for (int i = 1; i <= 9; i++) {
			int curPrintPage = i * PRINT_PAGE_COUNTER;
			
			test.openMenuItem(TopMenu.NAVIGATE, "Go To Page");
			
			SWTBot goToBot = test.bot.activeShell().bot();
			goToBot.text(0).setText("" + curPrintPage);
			ViewTestRunner.doPendingSWTWork();
			goToBot.radio(GoToPageDialog.PageType.PRINT.ordinal()).click();
			ViewTestRunner.doPendingSWTWork();
			goToBot.button(0).click();
			
			test.openMenuItem(TopMenu.TOOLS, "Volume Manager", "Insert", "Normal");
		}
	}
}
