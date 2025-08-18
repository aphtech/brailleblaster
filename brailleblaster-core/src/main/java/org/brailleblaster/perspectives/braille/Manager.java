/*
 * Copyright (C) 2025 American Printing House for the Blind
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.brailleblaster.perspectives.braille;

import kotlin.Pair;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.ParentNode;
import org.apache.commons.lang3.time.StopWatch;
import org.brailleblaster.BBIni;
import org.brailleblaster.Main;
import org.brailleblaster.archiver2.Archiver2;
import org.brailleblaster.archiver2.ArchiverFactory;
import org.brailleblaster.archiver2.ArchiverRecoverThread;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.bbx.fixers2.LiveFixer;
import org.brailleblaster.embossers.EmbossingUtils;
import org.brailleblaster.exceptions.*;
import org.brailleblaster.frontmatter.VolumeUtils;
import org.brailleblaster.utils.localization.LocaleHandler;
import org.brailleblaster.math.mathml.MathModule;
import org.brailleblaster.perspectives.Controller;
import org.brailleblaster.perspectives.braille.document.BrailleDocument;
import org.brailleblaster.perspectives.braille.eventQueue.EventFrame;
import org.brailleblaster.perspectives.braille.mapping.elements.*;
import org.brailleblaster.perspectives.braille.mapping.maps.MapList;
import org.brailleblaster.perspectives.braille.messages.*;
import org.brailleblaster.perspectives.braille.searcher.Searcher;
import org.brailleblaster.perspectives.braille.stylers.*;
import org.brailleblaster.perspectives.braille.viewInitializer.NimasInitializer;
import org.brailleblaster.perspectives.braille.viewInitializer.ViewFactory;
import org.brailleblaster.perspectives.braille.viewInitializer.ViewInitializer;
import org.brailleblaster.perspectives.braille.views.style.StylePane;
import org.brailleblaster.perspectives.braille.views.tree.BookTreeDialog;
import org.brailleblaster.perspectives.braille.views.wp.BrailleView;
import org.brailleblaster.perspectives.braille.views.wp.TextView;
import org.brailleblaster.perspectives.mvc.BBSimpleManager;
import org.brailleblaster.perspectives.mvc.SimpleEvent;
import org.brailleblaster.perspectives.mvc.ViewManager;
import org.brailleblaster.perspectives.mvc.events.ModifyEvent;
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent;
import org.brailleblaster.perspectives.mvc.modules.misc.*;
import org.brailleblaster.printers.PrintPreview;
import org.brailleblaster.printers.PrintersManager;
import org.brailleblaster.search.SearchDialog;
import org.brailleblaster.settings.UTDManager;
import org.brailleblaster.settings.ui.BrailleSettingsDialog;
import org.brailleblaster.settings.ui.EmbosserSettingsTab;
import org.brailleblaster.utd.IStyle;
import org.brailleblaster.utd.UTDTranslationEngineCallback;
import org.brailleblaster.utd.actions.IAction;
import org.brailleblaster.utd.exceptions.NodeException;
import org.brailleblaster.utd.exceptions.UTDInterruption;
import org.brailleblaster.utd.internal.DocumentOrderComparator;
import org.brailleblaster.utd.internal.xml.FastXPath;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.brailleblaster.utd.internal.xml.XMLHandler2;
import org.brailleblaster.utd.properties.UTDElements;
import org.brailleblaster.utd.utils.TableUtils;
import org.brailleblaster.utd.utils.UTDHelper;
import org.brailleblaster.util.*;
import org.brailleblaster.wordprocessor.BBStatusBar;
import org.brailleblaster.wordprocessor.FontManager;
import org.brailleblaster.wordprocessor.RecentDocs;
import org.brailleblaster.wordprocessor.WPManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

//This class manages each document in an MDI environment. It controls the braille View and the daisy View.
public class Manager extends Controller {
    private static final LocaleHandler localeHandler = LocaleHandler.getDefault();
    private static final ModuleService moduleService = new ModuleService();
    public static final Path DEFAULT_FILE = BBIni.getProgramDataPath().resolve(Paths.get("xmlTemplates", "blankTemplate.bbz"));
    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger(0);
    public final int instanceId = INSTANCE_COUNTER.getAndIncrement();

    private final ViewManager viewManager;
    // TODO: After the great merge, move all calls to getters instead
    private final SashForm containerSash;
    private final TextView text;
    private final BrailleView braille;
    private Archiver2 archiver;

    private ViewInitializer viewInitializer;

    private final static Logger logger = LoggerFactory.getLogger(Manager.class);
    private BrailleDocument document;
    private final FontManager fontManager;
    private MapList list;
    private final Vector<String> ignoreList = new Vector<>();

    private final Reformatter reformatter;
    private CountDownLatch finishFormattingLatch, rebuiltSectionLatch, rebuiltSectionMapLatch;
    private Integer pages;
    private int extraPages;
    private boolean xmlEdited = false;
    private boolean restartFormatFlag = false;
    private boolean pressNo = true;
    private final ArchiverRecoverThread myArchiverRecoverThread = new ArchiverRecoverThread(this);
    private final BBSimpleManager simpleManager;
    private boolean isDocumentEdited = false;
    private Document lastCopiedDoc = null;
    private static RuntimeException formatterException = null;
    /**
     * DO NOT USE, ONLY getArchiver() IS ACCURATE!!! Internal use for
     * {@link #open() }
     */
    private final @NotNull Path _initFile;

    // Common init
    public Manager(WPManager wp, @Nullable Path file) {
        super(wp);
        final Path iFile;
        if (file == null) {
            iFile = DEFAULT_FILE;
            logger.info("No input file given, opening template {}", iFile);
        } else {
            iFile = file;
        }
        _initFile = iFile;
        simpleManager = new BBSimpleManager() {
            // TODO: temporary workarounds to keep current manager design
            @Override
            @NotNull
            public UTDManager getUtdManager() {
                return getDocument().getSettingsManager();
            }

            @Override
            @NotNull
            public Document getDoc() {
                return Manager.this.getDoc();
            }

            @Override
            @NotNull
            public Manager getManager() {
                return Manager.this;
            }
        };

        fontManager = new FontManager(this);

        viewManager = new ViewManager(wp.getFolder(), this);
        getFontManager().initViews();
        containerSash = viewManager.containerSash;
        text = viewManager.getTextView();
        braille = viewManager.getBrailleView();

        Thread.currentThread().setName("main-" + instanceId);

        reformatter = new Reformatter(this);
        Thread formatThread = new Thread(reformatter);
        formatThread.setDaemon(true);
        formatThread.setName("reformatter-" + instanceId);
        formatThread.start();

        // Core wiring module
        // This must be first as the event must be translated
        simpleManager.registerModule((SimpleEvent event) -> {
            if (event instanceof ModifyEvent) {
                ModifyEvent mEvent = (ModifyEvent) event;
                stopFormatting();

                mEvent.changedNodes.removeIf(n -> n.getDocument() == null);

                if (mEvent.translate) {
                    List<Element> changedNodes = new ArrayList<>();

                    // Used to comminicate information before a formatting stage
                    // Can't just apply to mEvent.changedNodes as these might be added to a parent
                    FastXPath.descendant(getDoc()).stream().filter(Searcher.Filters::isElement)
                            .map(Searcher.Mappers::toElement)
                            .filter(BBX.PreFormatterMarker.ATTRIB_PRE_FORMATTER_MARKER::has).forEach(BBX.PreFormatterMarker.ATTRIB_PRE_FORMATTER_MARKER::detach);

                    Set<Element> liveFixedSections = new HashSet<>();
                    mEvent.changedNodes.forEach(n -> {
                        if (n.getDocument() == null) {
                            return;
                        }
                        if (n instanceof Document || n.getDocument().getRootElement() == n) {
                            LiveFixer.fix(XMLHandler2.nodeToElementOrParentOrDocRoot(n));
                            refresh(false);
                            changedNodes.clear();
                            return;
                        } else {
                            // Issue #6022 #5947: cleanup document to prevent weird state or formatting
                            Element section = XMLHandler.ancestorVisitorElement(n, BBX.SECTION::isA);
                            if (section != null && !liveFixedSections.contains(section)) {
                                LiveFixer.fix(section);
                                liveFixedSections.add(section);

                                if (n.getDocument() == null) {
                                    // was removed by fixer
                                    return;
                                }
                            }
                        }
                        Element tableParent = XMLHandler.ancestorVisitorElement(n,
                                BBX.CONTAINER.TABLE::isA);
                        if (tableParent != null) {
                            // Table formatter re-creates table, meaning a
                            // referenced element
                            // inside of the table won't be in this document
                            // after reformat
                            changedNodes.add(tableParent);
                        } else if (BBX.SECTION.isA(n) || (BBX.CONTAINER.isA(n) && !BBX.CONTAINER.TABLE.isA(n))) {
                            for (Node child : FastXPath.descendantOrSelf(n)) {
                                if (BBX.CONTAINER.TABLE.isA(child)
                                        && ((Element) child).getAttribute(TableUtils.ATTRIB_TABLE_COPY) == null
                                        && !changedNodes.contains(child)) {
                                    changedNodes.add((Element) child.getParent());
                                } else if (!BBX.BLOCK.TABLE_CELL.isA(child) && BBX.BLOCK.isA(child) && !changedNodes.contains((Element) child)) {
                                    changedNodes.add((Element) child);
                                }
                            }
                        } else if (BBX.CONTAINER.TABLE.isA(n)) {
                            // Bug inside UTDTranslationEngine prevents you from
                            // being able to pass in a table container
                            changedNodes.add((Element) n.getParent());
                        } else if (!BBX.BLOCK.isA(n)) {
                            while (!BBX.BLOCK.isA(n)) {
                                n = n.getParent();
                            }
                            if (n instanceof Element && !changedNodes.contains((Element)n))
                                changedNodes.add((Element) n);
                        } else {
                            if (n instanceof Element && !changedNodes.contains((Element)n))
                                changedNodes.add((Element) n);
                        }
                    });

                    mEvent.changedNodes.clear();
                    if (!changedNodes.isEmpty()) {
                        for (Node changedNode : changedNodes) {
                            logger.debug("Changed node {}", changedNode.toXML());
                        }
                        getDocument().getSettingsManager().getEngine().expectedTranslate = true;
                        try {
                            List<Element> newChangedNodes = getDocument().getSettingsManager().getEngine()
                                    .translateAndReplace(changedNodes);
                            getDocument().getSettingsManager().getEngine().expectedTranslate = false;
                            for (Element newChangedNode : newChangedNodes) {
                                logger.debug("Retranslated block: {}", newChangedNode.toXML());
                            }
                            mEvent.changedNodes.addAll(newChangedNodes);
                        } catch (RuntimeException e) {
                            getDocument().getSettingsManager().getEngine().expectedTranslate = false;
                            throw new TranslationException("An exception occurred while translating", e);
                        }
                    } else {
                        logger.debug("No blocks found to be translated");
                    }
                }

                lastCopiedDoc = document.doc.copy();
                // Must call reformat as early as possible due to table's being
                // rebuilt,
                // meaning references to elements inside tables will not work
                /*
                 * TODO: why does calling this here instead of inside TextViewModule break all the tests?!
                 *  Without this, StyleEditorTest.imageDescriber_insideTableCopy_rt4566() fails
                 */

                mEvent.changedNodes.sort(new DocumentOrderComparator());
                if (!mEvent.changedNodes.isEmpty() && list.findNode(mEvent.changedNodes.get(0)) == null) {
                    reformat(mEvent.changedNodes.get(0));
                } else {
                    reformat();
                }
            }
        });
        //The following should only be visible in the menus in debugging mode:
        simpleManager.registerModules(moduleService.modules(this));
        // TODO:Use split merge module and remove duplicate code from
        //  file module when way exists to specify item location in menu
        //  simpleManager.registerModule(new SplitMergeModule());
    }

    /**
     * Executes file open
     * <p>
     * Separated from constructor due to higher likelihood of Exceptions, which if
     * in constructor means {@link #close() } cleanup cannot be ran.
     * <p>
     * Other Exceptions in constructor are most likely fatal for everything making
     * cleanup unnecessary
     */
    public void open() {
        Path file = _initFile;
        if (!Files.exists(file)) {
            throw new RuntimeException("File " + file.toUri() + " does not exist");
        }
        // Attempt to open file before all else. RT-7789
        openDocument(file);
        addTabItem(getWpManager().getFolder()).setControl(this.containerSash);
        initializeDocumentTab();

        // RT 7560 - Pandoc Imported File should be saved before closing
        if (getArchiver().isImported()) {
            this.setDocumentEdited(true);
        }
        Archiver2 archiver2 = getArchiver();
        String newName = Optional.ofNullable(archiver2.getNewPath()).orElse(file).getFileName().toString();
        setTabTitle(newName);
        lineUpViews();
    }

    private void initializeDocumentTab() {
        viewManager.setTabList();
        getWpManager().getShell().layout();
    }

    public void repeatLastSearch() {
        SearchDialog search = Objects.requireNonNull(simpleManager.getModule(SearchDialog.class));
        search.repeatLastSearch();
    }

    public boolean isTableSelected() {
        return Objects.requireNonNull(simpleManager.getModule(TableSelectionModule.class)).isTableSelected();
    }

    /*
     * Checks whether the current cursor position is a valid spot for a page break
     */
    public boolean isValidPageBreak() {
        return !(getMapList().getCurrent() instanceof ReadOnlyFormattingWhiteSpace);
    }

    public static Element getTableParent(Node n) {
        if (n == null) {
            throw new NullPointerException("n");
        } else if (n.getDocument() == null) {
            throw new NodeException("node not attached to document", n);
        }
        Element parent;
        if (n instanceof Element)
            parent = (Element) n;
        else
            parent = (Element) n.getParent();
        Element root = parent.getDocument().getRootElement();
        // logger.debug("Starting parent = " + XMLHandler.toXMLStartTag(root));
        // TODO: This should come from an enum or call a utility method
        List<String> tableFormats = Arrays.asList("simple", "listed", "stairstep", "linear");
        while (!tableFormats.contains(parent.getAttributeValue("format"))
                && parent.getParent() != parent.getDocument()) {
            if (parent.getParent() == root)
                return null;
            parent = (Element) parent.getParent();
        }
        if (parent.getAttribute("class") != null && parent.getAttributeValue("class").contains("utd:table")) {
            return (Element) parent.getParent().getChild(parent.getParent().indexOf(parent) - 1);
        }
        return parent;
    }

    public static Element getTableBrlCopy(Node node) {
        Element table = Manager.getTableParent(node);
        if (table == null)
            return null;
        if (TableUtils.isTableCopy(table))
            return table;
        ParentNode parent = table.getParent();
        int index = parent.indexOf(table);
        if (index != parent.getChildCount() - 1 && BBX.CONTAINER.TABLE.isA(parent.getChild(index + 1))
                && TableUtils.isTableCopy((Element) parent.getChild(index + 1))) {
            return (Element) parent.getChild(index + 1);
        }
        return null;
    }

    public void closeThreads() {
        // stop the thread for auto save
        myArchiverRecoverThread.autoSave(false);
        if (reformatter != null) {
            reformatter.close();
        }
        UndoRedoModule undoRedo = getSimpleManager().getModule(UndoRedoModule.class);
        if (undoRedo != null) {
            undoRedo.closeUndoThread();
        }
    }

    @Override
    public boolean close() {
        /*
         * May be from quick closing before finished initing Exception Report #137
         */
        if (archiver == null) {
            return true;
        }

        boolean cancel = false;
        // Issue #4321: Prompt user if they deleted the origional file
        if (!BBIni.getDebugging() && (isDocumentEdited() || !Files.exists(getArchiver().getPath()))) {
            YesNoChoice ync = new YesNoChoice(localeHandler.get("hasChanged"), true);

            if (ync.getResult() == SWT.YES) {
                cancel = !FileModule.Companion.fileSave(this);
                // myArchiverRecoverThread.removeFile(pathToRemove);
                pressNo = false;
            } else if (ync.getResult() == SWT.CANCEL) {
                cancel = true;
                pressNo = false;
            }

            if (pressNo) {
                text.hasChanged = false;
                getBraille().hasChanged = false;
                isDocumentEdited = false;
                myArchiverRecoverThread.removeFile();

            }
        }

        if (!cancel) {
            closeThreads();

            viewManager.saveScreenProperties();
            // remove listener before you build toolbar again
            if (getWpManager().getCurrentPerspective().getKeyListener() != null) {
                text.getView().removeVerifyKeyListener(getWpManager().getCurrentPerspective().getKeyListener());
                getWpManager().getCurrentPerspective().setKeyListener(null);
            }
            dispose();
            final CTabItem tab = getTab();
            if (tab != null) {
                tab.dispose();
            }
            // fontManager.disposeFonts();
            if (archiver == null & docCount > 0)
                docCount--;
            getWpManager().removeController(this);

            try {
                getArchiver().close();
            } catch (IOException e) {
                logger.error("Error closing Archiver", e);
            }

            if (getWpManager().getList().isEmpty())
                getWpManager().getStatusBar().setText("");
        }

        return !cancel;
    }

    public void openDocument(Path file) {
        try (WorkingDialog ignored = new WorkingDialog("Opening book " + file)) {
            archiver = ArchiverFactory.INSTANCE.load(file);
        } catch (Exception e) {
            logger.error("Problem loading file", e);
        }

        // Recent Files.
        if (file != DEFAULT_FILE) {
            RecentDocs.Companion.getDefaultRecentDocs().addRecentDoc(file);
        }
        try {
            document = new BrailleDocument(this, getArchiver().getBbxDocument());
        } catch (NullPointerException npe) {
            throw new BBNotifyException("Cannot open " + file + " - file is corrupt or invalid.");
        }

        initializeAllViews();

        if (!list.isEmpty()) {
            // There's a possibility that the first thing on the page is a running head. Can
            // cause the document to be null.
            text.setCurrentElement(0);
            // Edge case. Prevent currentElement from
            // being null when first opening
            // document.
        }
        // Start the auto-saver ONLY if the opened file is not in the autosaved directory
        if (!BBIni.getDebugging() && !file.toString().contains(BBIni.getAutoSavePath().toString())) {
            //System.out.println("Starting autosave thread for " + file.toString());
  			    myArchiverRecoverThread.autoSave(true);
        }
    }

    private void initializeAllViews() {
        MathModule.retranslateSpatial(document);
        try (WorkingDialog ignored = new WorkingDialog(
                archiver != null ? "Parsing book " + getArchiver().getPath() : "Starting BrailleBlaster")) {
            document.translateDocument();
        } catch (RuntimeException e) {
            FormatterException newException = new FormatterException("An error occurred while opening the book", e);
            newException.setCurFallback(FormatterException.Fallback.REFRESH);
            countDownAllLatches();
            throw newException;
        }

        try (WorkingDialog ignored = new WorkingDialog(
                archiver != null ? "Parsing book " + getArchiver().getPath() : "Starting BrailleBlaster")) {
            Objects.requireNonNull(getSimpleManager().getModule(UndoRedoModule.class)).copyDocument(document.doc);

            containerSash.setRedraw(false);
            getWpManager().getStatusBar().resetLocation(6, 75, 100);
            getWpManager().getStatusBar().setText("Loading...");

            viewInitializer = ViewFactory.createUpdater(getArchiver(), document, text, braille);
            updateMapList(0);
            initializeListeners();
            text.hasChanged = false;
            braille.hasChanged = false;
            getWpManager().getStatusBar().resetLocation(0, 75, 100);
            getText().createNodeCaret(list.getCurrent(), text.getView().getCaretOffset());

            // Can't click on views without calling this? Why?
            containerSash.setRedraw(true);
        } catch (Exception e) {
            throw new RuntimeException("Unforeseen exception", e);
        }
    }

    public void onPostBuffer(MapList mapList) {
        // Rebuild style pane on buffer
        getViewManager().stylePane.generate(mapList);

        // Make sure views line up
        lineUpViews();
    }

    public void lineUpViews() {
        // The line spacing of the text view isn't actually changed until it's
        // painted
        getTextView().addListener(SWT.Paint, new Listener() {
            @Override
            public void handleEvent(Event event) {
                getFontManager().lineUpStylePaneWithTextView();
                getFontManager().lineUpBrailleViewWithTextView();
                getTextView().removeListener(SWT.Paint, this);
            }
        });
    }

    public void dispatch(Message message) {
        switch (message.eventType) {
            case GET_CURRENT -> handleGetCurrent((GetCurrentMessage) message);
            case WHITESPACE_TRANSFORM -> handleWhitespaceTransform((WhitespaceMessage) message);
            case UPDATE -> handleUpdate((UpdateMessage) message);
            case SELECTION -> handleSelection((SelectionMessage) message);
            case INSERT_NODE -> handleInsertNode((InsertNodeMessage) message);
            case REMOVE_NODE -> handleRemoveNode((RemoveNodeMessage) message);
            case UPDATE_STATUSBAR -> handleUpdateStatusBar((UpdateStatusbarMessage) message);
            case ADJUST_LOCAL_STYLE -> handleAdjustLocalStyle((AdjustLocalStyleMessage) message);
            case ADJUST_RANGE -> handleAdjustRange((AdjustRangeMessage) message);
            case GET_TEXT_MAP_ELEMENTS -> findTextMapElements((GetTextMapElementsMessage) message);
            case UPDATE_SCROLLBAR -> handleUpdateScrollbar((UpdateScrollbarMessage) message);
            case UPDATE_STYLE -> handleUpdateStyle((UpdateStyleMessage) message);
            case TAB_INSERTION, TAB_ADJUSTMENT, TAB_DELETION -> handleTabInsertion((TabInsertionMessage) message);
            default -> {
            }
        }
    }

    public void checkView(TextMapElement t) {
        if (!list.contains(t)) {
            list = viewInitializer.bufferViews(this, getSection(t));
        }
    }

    public void decrementView() {
        if (viewInitializer.getSectionList().size() == 1)
            return;
        if (text.hasChanged)
            text.update(false);

        waitForFormatting(true);
        TextMapElement t = list.getFirst();
        if (t instanceof WhiteSpaceElement)
            t = list.findClosestNonWhitespace((WhiteSpaceElement) t);
        int section = getSection(t);
        if (section != 0) {
            list = viewInitializer.bufferViews(this, section - 1, false);
            list.setCurrent(list.indexOf(t) - 1);
            text.resetOffsets();
        }
    }

    public void incrementView() {
        if (viewInitializer.getSectionList().size() == 1)
            return;
        waitForFormatting(true);
        TextMapElement t = list.getLast();
        if (t instanceof WhiteSpaceElement)
            t = list.findClosestNonWhitespace((WhiteSpaceElement) t);
        int section = getSection(t);
        if (section != viewInitializer.getSectionList().size() - 1) {
            list = viewInitializer.bufferViews(this, section + 1, true);
            list.setCurrent(list.indexOf(t) + 1);
            text.resetOffsets();
        }
    }

    public void incrementCurrent() {
        waitForFormatting(true);
        int index = list.getCurrentIndex();
        // update if not last in list
        if (index < list.size() - 1) {
            index++;

            TextMapElement t = list.get(index);
            getSimpleManager().dispatchEvent(new XMLCaretEvent(Sender.SIMPLEMANAGER, text.createNodeCaret(t, 0)));
        }
    }

    /**
     * Returns after the thread that is formatting the entire document has finished.
     * To be used if an operation requires the entire document to be formatted.
     *
     * @param updateMapList
     * If false, waitForFormatting will not update the map list
     * of the document. This can be used if the map list is not
     * referenced beyond the currently loaded section. In most
     * cases, it is correct to set this to true.
     */
    public void waitForFormatting(boolean updateMapList) {
        logger.debug("Enter waitForFormatting. Busy? {}", reformatter.busy);
        try {
            if (reformatter.busy) {
                text.removeFocusListener();
                Shell working = new Shell(Display.getCurrent(), SWT.TITLE | SWT.BORDER | SWT.ON_TOP);
                working.setLayout(new GridLayout(1, false));
                working.setText("Formatting...");

                Text workingText = new Text(working, SWT.NONE);
                workingText.setText("Formatting, please wait...");

                FormUIUtils.setLargeDialogSize(working);

                working.open();
                finishFormattingLatch.await();
                finishFormattingLatch = new CountDownLatch(1);
                working.dispose();
                text.addFocusListener();
            }
        } catch (InterruptedException e) {
            // There are no possible interruptions here.
            // Move along.
        }
        if (updateMapList && needsMapListUpdate()) {
            logger.debug("MapList needs update");
            updateFormatting();
        }
        logger.debug("Exit WaitForFormatting");
    }

    public boolean needsMapListUpdate() {
        return xmlEdited;
    }

    public void handleEditingException(Exception e) {
        if (BBIni.getDebugging()) {
            throw new RuntimeException(e);
        }
        logger.error("Handling editing exception", e);
        if (getLastCopiedDoc() != null) {
            document.doc.replaceChild(document.doc.getRootElement(),
                    getLastCopiedDoc().getRootElement().copy());
        } else {
            logger.error("Last copied doc is null");
        }
        try {
            refresh(false);
            Notify.showException("An error occurred. BrailleBlaster has attempted to repair the document.", e);
        } catch (Exception e2) {
            Main.handleFatalException(e2);
        }
    }

    private void findTextMapElements(GetTextMapElementsMessage message) {
        ArrayList<SectionElement> secList = viewInitializer.getSectionList();

        ArrayList<TextMapElement> itemList = message.itemList;
        ArrayList<Node> textList = message.getNodeList();

        if (secList.size() > 1) {
            for (SectionElement sectionElement : secList) {
                int size = itemList.size();
                if (sectionElement.isVisible())
                    list.findTextMapElements(textList, itemList);
                else
                    sectionElement.list.findTextMapElements(textList, itemList);

                if (size > 0 && size == itemList.size())
                    break;
            }
        } else
            list.findTextMapElements(textList, itemList);
    }

    private void handleGetCurrent(GetCurrentMessage message) {
        list.getCurrentNodeData(message, message.offset, list.getCurrent(), message.sender);
    }

    private void handleWhitespaceTransform(WhitespaceMessage message) {
        stopFormatting();
        WhitespaceTransformer wst = new WhitespaceTransformer(this);
        try {
            wst.transformWhiteSpace(message);
        } catch (RuntimeException e) {
            throw new EditingException("Exception while transforming whitespace", e);
        }
    }

    private void handleUpdate(UpdateMessage message) {
        stopFormatting();
        TextUpdateHandler tuh = new TextUpdateHandler(this, viewInitializer, list);
        tuh.updateText(message);
    }

    private void handleSelection(SelectionMessage message) {
        stopFormatting();
        SelectionHandler sh = new SelectionHandler(this, viewInitializer, list);
        try {
            sh.removeSelection(message);
        } catch (RuntimeException e) {
            throw new EditingException("Exception while deleting selection", e);
        }
    }

    private void handleInsertNode(InsertNodeMessage m) {
        stopFormatting();
        InsertElementHandler inserter = new InsertElementHandler(this, viewInitializer, list);
        inserter.insertElement(m);
    }

    /*
     * Return true when split is successful
     */
    public boolean splitElement() {
        stopFormatting();
        return SplitElementModule.splitElement(getSimpleManager());
    }

    private void handleRemoveNode(RemoveNodeMessage message) {
        stopFormatting();
        RemoveElementHandler er = new RemoveElementHandler(this, viewInitializer, list);
        er.removeNode(message);
    }

    private void handleUpdateStatusBar(UpdateStatusbarMessage message) {
        getWpManager().getStatusBar().setColor(ColorManager.Colors.BLACK);
        getWpManager().getStatusBar().setText(message.getStatus());
    }

    public void setTemporaryStatusBarMessage(String message, boolean redHighlight) {
        if (redHighlight) {
            getWpManager().getStatusBar().setColor(ColorManager.Colors.RED);
        } else {
            getWpManager().getStatusBar().setColor(ColorManager.Colors.BLACK);
        }
        getWpManager().getStatusBar().setText(message);
    }

    private void handleAdjustLocalStyle(AdjustLocalStyleMessage message) {
        stopFormatting();
        // Element e = document.getParent(list.getCurrent().n, true);
        text.update(false);

        StyleHandler sh = new StyleHandler(this, viewInitializer, list);
        try {
            sh.createAndApplyStyle(list.getCurrent(), message);
        } catch (RuntimeException e) {
            throw new EditingException("An error occurred while adjusting a style", e);
        }
    }

    public void handleAdjustRange(AdjustRangeMessage message) {
        list.adjustOffsets(message.type, list.getCurrentIndex(), message.position);
    }

    private void handleUpdateScrollbar(UpdateScrollbarMessage message) {
        text.setListenerLock(true);
        if (message.sender.equals(Sender.BRAILLE))
            text.getView().setTopPixel(getBrailleView().getTopPixel());
        else
            braille.getView().setTopPixel(getTextView().getTopPixel());
        viewManager.stylePane.widget.setTopPixel(getTextView().getTopPixel());
        text.setListenerLock(false);
    }

    /***
     * Handle style for all cases
     */
    private void handleUpdateStyle(UpdateStyleMessage message) {
        stopFormatting();
        if (!text.getView().getText().isEmpty()) {
            containerSash.setRedraw(false);
            StyleHandler sh = new StyleHandler(this, viewInitializer, list);
            sh.updateStyle(message);
            // update user settings
            BBIni.getPropertyFileManager().save("currentStyleId", message.style.getId());
            containerSash.setRedraw(true);
        } else
            notify(localeHandler.get("nothingToApply"));
    }

    private void handleTabInsertion(TabInsertionMessage m) {
        TabInsertionHandler th = new TabInsertionHandler(this, viewInitializer, list);
        if (m.eventType.equals(BBEvent.TAB_INSERTION))
            th.insertTab(m);
        else if (m.eventType.equals(BBEvent.TAB_ADJUSTMENT))
            th.adjustTab(m);
        else
            th.removeTab(m);
    }

    public void home() {
        // update if has changed
        text.update(false);
        if (getSectionList().size() > 1) {
            useResetSectionMethod(0);
        }
        text.setListenerLock(true);
//		text.view.setFocus();
        text.getView().setTopIndex(0);
        setTextCaret(0);
        text.setListenerLock(false);
    }

    public void end() {
        // update if has changed
        text.update(false);
        waitForFormatting(true);
        useResetSectionMethod(getSectionList().size() - 1);
        text.setListenerLock(true);
//		text.view.setFocus();
        text.getView().setTopIndex(text.getView().getLineCount() - 1);
        SectionElement lastSection = getSectionList().get(getSectionList().size() - 1);
        if (getSectionList().size() > 1 || !lastSection.list.isEmpty()) {
            setTextCaret(lastSection.list.getLast().getEnd(getMapList()));
        }
        text.setListenerLock(false);
    }

    public void textPrint() {
        PrintersManager pn = new PrintersManager(getWpManager().getShell(), text.getView());
        pn.beginPrintJob();
    }

    public void fileEmbossNow() {
        EmbossingUtils.emboss(document, document.getEngine(), Display.getCurrent().getActiveShell(), s -> new BrailleSettingsDialog(s, this, EmbosserSettingsTab.class));
    }

    public void printPreview() {
        this.waitForFormatting(true);
        new PrintPreview(getWpManager().getShell(), getFontManager(), getDocument().getSettingsManager(), getDoc(), Manager.this);
    }


    private void setCurrentOnRefresh(Sender sender) {
        simpleManager
                .dispatchEvent(new XMLCaretEvent(sender, text.createNodeCaret(Objects.requireNonNull(text.getCurrentElement()), 0)));
    }

    /**
     * THIS SHOULD NOT BE CALLED. If you need the document to be reformatted,
     * dispatch a ModifyEvent.
     * <p>
     * Reformats document starting at the top of the Text View's currently loaded
     * chunk.
     *
     * @see #reformat(Node)
     */
    private void reformat() {
        Node firstNonWhitespace = getFirstReformattableNode();
        if (firstNonWhitespace != null && firstNonWhitespace.getDocument() != null) {
            if (viewInitializer.getSectionList().size() > 1 && getSection(list.get(0)) != 0) {
                reformat(firstNonWhitespace);
                return;
            } else {
                // If the user is at the first section, find the first brl
                Node firstBrl = XMLHandler.followingVisitor(firstNonWhitespace, UTDElements.BRL::isA);
                if (firstBrl != null) {
                    Node firstNode = XMLHandler.childrenRecursiveNodeVisitor(firstBrl,
                            n -> UTDElements.NEW_PAGE.isA(n) || n instanceof nu.xom.Text);
                    if (!(firstNode instanceof nu.xom.Text) && firstNode != null) {
                        reformat(firstNode);
                        return;
                    }
                }
            }
        }
        reformatDocument(getDoc());
    }

    private void reformat(@NotNull Node node) {
        if (reformatter.busy) {
            // Note: It may be tempting to substitute this line for
            // stopFormatting(), but in 90% of cases, if you've reached the
            // point where you've called
            // reformat() and you receive this exception, you should've called
            // stopFormatting() a long time ago.
            throw new IllegalStateException(
                    "Format Thread already running. Use manager.stopFormatting() before making changes to the xml.");
        }
        if (node instanceof Document) {
            logger.debug("Node is the document");
            reformatDocument((Document) node);
        } else {
            Element prevNewPage = null;
            if (UTDElements.NEW_PAGE.isA(node)) {
                prevNewPage = (Element) node;
            } else if (!(node.getParent() == null || node.getParent() instanceof Document)) {
                prevNewPage = (Element) searchForPreviousNode(node, (n) -> UTDElements.NEW_PAGE.isA(n)
                        && !"formatting".equals(((Element) n.getParent()).getAttributeValue("type")));
            }
            if (prevNewPage == null) {
                logger.debug("NewPage element not found after searching from {}", node);
                reformatDocument(node.getDocument());
            } else {
                Element prevPrintPageNum = (Element) searchForPreviousNode(node, UTDElements.PRINT_PAGE_NUM::isA);
                _reformat(prevNewPage, prevPrintPageNum);
            }
        }
    }

    private void _reformat(Element newPage, Element printPageNum) {
        long startTime = System.currentTimeMillis();
        if (getSectionList().size() == 1) {
            logger.debug("Reformatting whole document");
            reformatDocument(newPage.getDocument());
            return;
        }
        pages = 0;
        extraPages = 0;
        initializeReformattingCallback(newPage);
        text.setUiLock(true);
        if (finishFormattingLatch == null)
            finishFormattingLatch = new CountDownLatch(1);
        rebuiltSectionLatch = new CountDownLatch(1);
        reformatter.startFormat(newPage, printPageNum);
        try {
            rebuiltSectionLatch.await();
        } catch (InterruptedException e) {
            // Shh it's ok
        }
        if (formatterException != null) {
            RuntimeException prevException = formatterException;
            formatterException = null;
            countDownAllLatches();
            throw new FormatterException("Error occurred while reformatting", prevException);
        }
        try {
            updateFormatting();
        } catch (RuntimeException e) {
            countDownAllLatches();
            throw new FormatterException("Error occurred while rebuilding view after formatting", e);
        }
        xmlEdited = viewInitializer.getSectionList().size() != 1;
        text.setUiLock(false);

        if (rebuiltSectionMapLatch != null)
            rebuiltSectionMapLatch.countDown();
        logger.debug("Leaving _reformat in {}", Utils.runtimeToString(startTime));
    }

    /*
     * Get the earliest node before the current section that is not null and is not
     * inside a table
     */
    private Node getFirstReformattableNode() {
        int curIndex = viewInitializer.getStartIndex();
        TextMapElement curTME = getSectionList().get(curIndex).list.getClosest(0, true);
        while (curTME == null || !isNodeReformattable(curTME.getNode())) {
            curIndex--;
            if (curIndex < 0) {
                return null;
            }
            MapList curList = getSectionList().get(curIndex).list;
            curTME = curList.getPrevious(curList.size(), true);
            while (curTME != null && !isNodeReformattable(curTME.getNode())) {
                curTME = curList.getPrevious(curList.indexOf(curTME), true);
            }
        }
        return curTME.getNode();
    }

    private boolean isNodeReformattable(Node n) {
        return n != null && n.getDocument() != null && Manager.getTableParent(n) == null;
    }

    private void initializeReformattingCallback(Element startingNewPage) {
        int maxPages = calculateReformattingPages(startingNewPage) + 1;
        Consumer<Integer> onComplete = (i) -> {
            rebuiltSectionMapLatch = new CountDownLatch(1);
            rebuiltSectionLatch.countDown();
            try {
                rebuiltSectionMapLatch.await();
            } catch (InterruptedException ignored) {
            }
        };
        // If a page is added in an edit, the reformatter stops too early and
        // causes bad markup.
        // This is a temporary fix that only addresses if at most two pages are
        // added.
        final int REFORMATTER_BUG_BUFFER = 2;
        document.getEngine().setCallback(new UTDTranslationEngineCallback() {
            @Override
            public void onUpdateNode(@NotNull Node n) {
                if (restartFormatFlag)
                    throw new UTDInterruption();
                incrementPages();
                if (startingNewPage.getDocument() != null) {
                    // If the original newPage element is not detached from the
                    // document, UTD chose a different
                    // starting point than the reformatter.
                    extraPages += extraPages == 0 ? 2 : 1; // Because writeUTD()
                    // in PageBuilder is
                    // not instantly
                    // called,
                    // the first
                    // non-detached
                    // newPage can
                    // represent 2 pages
                }
                if (pages != null && pages == maxPages + extraPages + REFORMATTER_BUG_BUFFER) {
                    logger.debug("Max pages {} reached. Rebuilding mapList", maxPages);
                    onComplete.accept(pages);
                }
            }

            @Override
            public void onFormatComplete(@NotNull Node root) {
                logger.debug("Formatting completed");
                if (pages != null && pages < maxPages + extraPages + REFORMATTER_BUG_BUFFER) {
                    logger.debug("MaxPages never reached.");
                    onComplete.accept(pages);
                }
            }
        });
    }

    private void removeReformattingCallback() {
        document.getEngine().setCallback(new UTDTranslationEngineCallback() {
            @Override
            public void onUpdateNode(@NotNull Node n) {
            }

            @Override
            public void onFormatComplete(@NotNull Node root) {
            }
        });
    }

    private int calculateReformattingPages(Element startPoint) {
        int totalPages = getTotalPagesInView();
        // If the startPoint comes before the current section, add that section's pages
        // to
        // what needs to be reformatted
        for (int i = viewInitializer.getStartIndex() - 1; i >= 0; i--) {
            MapList curList = viewInitializer.getSectionList().get(i).list;
            TextMapElement firstNonWhitespace = curList.getClosest(0, true);
            if (firstNonWhitespace == null || firstNonWhitespace.getNode() == null
                    || firstNonWhitespace.getNode().getDocument() == null) {
                logger.error("sectionList index {} consists only of whitespace?", i);
                totalPages += viewInitializer.getSectionList().get(i).pages;
            } else {
                totalPages += viewInitializer.getSectionList().get(i).pages;
                int compare = new DocumentOrderComparator().compare(startPoint, curList.getClosest(0, true).getNode());
                if (compare > 0) {
                    break;
                }
            }
        }
        return totalPages;
    }

    public void reformatDocument(Document doc) {
        stopFormatting();
        removeReformattingCallback();
        try {
            document.getEngine().format(doc.getRootElement());
            updateFormatting();
        } catch (RuntimeException e) {
            FormatterException newException = new FormatterException("An error occurred while reformatting", e);
            newException.setCurFallback(FormatterException.Fallback.FORMAT_DOCUMENT);
            countDownAllLatches();
            throw newException;
        }
    }

    private Node searchForPreviousNode(Node startPoint, Predicate<Node> test) {
        if (startPoint.getParent() instanceof Document)
            throw new IllegalArgumentException("Expected child node, received root element");
        if (startPoint.getParent() == null)
            throw new IllegalArgumentException("Node " + startPoint.toXML() + " has no parent");
        Element curElement = (Element) startPoint.getParent();
        int startIndex = curElement.indexOf(startPoint) - 1;
        for (int i = startIndex; i >= 0; i--) {
            if (test.test(curElement.getChild(i)))
                return curElement.getChild(i);
            if (curElement.getChild(i) instanceof Element) {
                Node searchElement = searchDescendantsForNodeBackwards((Element) curElement.getChild(i), test);
                if (searchElement != null)
                    return searchElement;
            }
        }
        if (curElement.getParent() instanceof Document)
            return null;
        return searchForPreviousNode(curElement, test);
    }

    private Node searchForPreviousTextMapElement(PageIndicator pageIndicator, Predicate<Node> test) {
        int startIndex = list.getCurrentIndex();
        while (list.getPrevious(startIndex, true) != null) {
            TextMapElement prev = list.getPrevious(startIndex, true);
            if (prev.getEnd(getMapList()) < 0) {
                throw new OutdatedMapListException("TME " + prev + " has negative end");
            }
            if (pageIndicator.getLine() >= text.getView()
                    .getLineAtOffset(list.getPrevious(startIndex, true).getEnd(getMapList()))) {
                break;
            }
            if (test.test(list.getPrevious(startIndex, true).getNodeParent())) {
                return list.getPrevious(startIndex, true).getNodeParent();
            }
            startIndex--;

        }
        return null;
    }

    private Node searchDescendantsForNodeBackwards(Element parent, Predicate<Node> test) {
        for (int search = parent.getChildCount() - 1; search >= 0; search--) {
            if (test.test(parent.getChild(search)))
                return parent.getChild(search);
            if (parent.getChild(search) instanceof Element) {
                Node searchElement = searchDescendantsForNodeBackwards((Element) parent.getChild(search), test);
                if (searchElement != null)
                    return searchElement;
            }
        }
        return null;
    }

    private void incrementPages() {
        if (pages != null) {
            pages++;
        }
    }

    /**
     * Interrupts the thread currently formatting the document and returns when it
     * is done. Use before making any edit to the Document.
     */
    public void stopFormatting() {
        logger.debug("Requesting to stop formatting. Busy? {}", reformatter.busy);
        if (reformatter.busy) {
            reformatter.stopFormat();
            try {
                finishFormattingLatch.await();
            } catch (InterruptedException e) {
                // Think about what you've done
            }

        }
        logger.debug("Exit stopFormatting.");
    }

    /**
     * updateFormatting() Re-initializes the text and braille views without
     * re-translating. Used when the formatter sends a notification that the
     * document has been reformatted. Note: DO NOT use to reformat document. See
     * reformat()
     */
    public synchronized void updateFormatting() {
        logger.debug("Begin updateFormatting");
        StopWatch sw = new StopWatch();
        sw.start();
        text.setListenerLock(true);
        int sectionIndex = viewInitializer.getStartIndex();
        int pos;
        if (text.getView().getSelectionRanges()[1] > 0) {
            pos = text.getView().getSelectionRanges()[0];
            // If something is selected, when the views are cleared, SWT fires a
            // selection listener, which causes an exception
            // even with the listener lock. Clear the selection first
            text.getView().setSelection(pos);
        } else
            pos = text.getView().getCaretOffset();

        int topIndex = text.getView().getTopIndex();
        list.clearList();
        containerSash.setRedraw(false);
        text.removeAllPaintedElements(text);
        braille.removeAllPaintedElements(braille);
        braille.clearPageRange();
        viewInitializer = ViewFactory.createUpdater(getArchiver(), document, text, braille);
        text.replaceTextRange(0, text.getView().getCharCount(), "");
        braille.replaceTextRange(0, braille.getView().getCharCount(), "");
        updateMapList(sectionIndex);
        xmlEdited = false;
        text.setTopIndex(topIndex);
        braille.setTopIndex(topIndex);
        final int newPos = pos;
        list.setCurrent(list.findClosest(newPos, list.getCurrent(), 0, list.size() - 1));
        // This needs to run after the views are finished updating, because
        // moving the caret will trigger code inside Style View, which
        // would be working with an outdated, unformatted document
//		simpleManager.getModule(PostViewUpdateModule.class).onModifyEvent(() -> { //commented for RT 7363
        text.setListenerLock(true);
        setTextCaret(newPos);
        text.setListenerLock(false);
//		});
        text.setListenerLock(false); //Does this need to be done twice?
        containerSash.setRedraw(true);
        sw.stop();
        logger.debug("Completed refreshFormat in: {}", sw);
    }

    private void updateMapList(int sectionIndex) {
        viewInitializer.initializeMap(this);
        viewInitializer.getSectionList().get(sectionIndex).setInView(true);
        list = viewInitializer.reformatViews(this, sectionIndex);
        if (list.isEmpty() && viewInitializer instanceof NimasInitializer)
            list = ((NimasInitializer) viewInitializer).formatTemplateDocument(this);
        onPostBuffer(list);
    }

    public void refresh() {
        refresh(true);
    }

    /**
     * There is a point in the formatting code where we want to refresh but we have
     * already handled updating, so the update argument allows you to specify that
     * an update is unnecessary
     */
    public void refresh(boolean update) {
        stopFormatting();
        removeReformattingCallback();

        int currentOffset;
        containerSash.setRedraw(true);
        if (update) {
            text.update(false);
        }
        if (text.getView().isFocusControl()) {
            currentOffset = text.getView().getCaretOffset();
            resetViews();

            if (currentOffset < text.getView().getCharCount() && currentOffset > 0)
                setTextCaret(currentOffset);
            else
                setTextCaret(0);

            setCurrentOnRefresh(Sender.TEXT);
            text.getView().setFocus();
            text.getView().setTopIndex(text.getView().getLineAtOffset(currentOffset));
        } else if (braille.getView().isFocusControl()) {
            currentOffset = braille.getView().getCaretOffset();
            int textOffset = text.getView().getCaretOffset();
            resetViews();

            braille.getView().setCaretOffset(currentOffset);
            braille.setPositionFromStart();
            setCurrentOnRefresh(Sender.BRAILLE);
            braille.getView().setFocus();
            text.getView().setTopIndex(text.getView().getLineAtOffset(textOffset));
        } else {
            currentOffset = text.getView().getCaretOffset();

            resetViews();

            setTextCaret(currentOffset);
            text.getView().setTopIndex(text.getView().getLineAtOffset(currentOffset));
            setCurrentOnRefresh(Sender.TEXT);
            text.getView().setFocus();
        }
        containerSash.setRedraw(true);
    }

    private void resetViews() {
        boolean textChanged = text.hasChanged;
        boolean brailleChanged = braille.hasChanged;

        int index = list.isEmpty() ? 0 : getSection(list.getCurrent());
        list.clearList();
        text.removeListeners();
        text.resetView(containerSash);
        braille.removeListeners();
        braille.resetView(containerSash);
        initializeDocumentTab();

        initializeAllViews();

        text.hasChanged = textChanged;
        braille.hasChanged = brailleChanged;

        if (index != -1)
            viewInitializer.bufferViews(this, index);

        onPostBuffer(getMapList());

        Objects.requireNonNull(getSimpleManager().getModule(ToggleViewsModule.class)).checkViews();
        getFontManager().initViews();

        /*
         * Stupid SWT hack: The line height will not change until the view is scrolled.
         * Meaning the views will line up but when scrolled the text view's line height
         * suddenly changes.
         */
        SelectionAdapter lineUpScrollListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                lineUpViews();
                getTextView().getVerticalBar().removeSelectionListener(this);
                getBrailleView().getVerticalBar().removeSelectionListener(this);
            }
        };
        getTextView().getVerticalBar().addSelectionListener(lineUpScrollListener);
        getBrailleView().getVerticalBar().addSelectionListener(lineUpScrollListener);
    }

    public void hide() {
        stopFormatting();
        HideActionHandler h = new HideActionHandler(this, viewInitializer, list);
        h.hideText();
    }

    public void closeUntitledTab() {
        // document.deleteDOM();
        text.removeListeners();
        text.clearText();
        braille.removeListeners();
        braille.clearText();
        list.clearList();
    }

    public void buffer(int index) {
        viewInitializer.bufferViews(this, index);
    }

    public void bufferForward() {
        waitForFormatting(true);
        int sectionIndex = viewInitializer.getStartIndex();
        while (viewInitializer.getSectionList().get(sectionIndex).isVisible()) {
            sectionIndex++;
        }

        viewInitializer.bufferViews(this, sectionIndex, true);
    }

    public void checkForUpdatedViews() {
        if (text.hasChanged)
            text.update(false);
    }

    public int indexOf(int section, TextMapElement t) {
        if (viewInitializer.getSectionList().size() > 1 && !(t instanceof WhiteSpaceElement))
            return viewInitializer.getSectionList().get(section).list.indexOf(t);
        else
            return list.indexOf(t);
    }

    public int getSectionSize(int index) {
        return viewInitializer.getSectionList().get(index).list.size();
    }

    public int getSection(TextMapElement t) {
        if (t instanceof TableCellTextMapElement) {
            t = ((TableCellTextMapElement) t).getParentTableMapElement();
        }
        for (int i = 0; i < viewInitializer.getSectionList().size(); i++) {
            if (viewInitializer.getSectionList().get(i).list.contains(t))
                return i;
        }
        return -1;
    }

    public int getSection(Node n) {
        for (int i = 0; i < viewInitializer.getSectionList().size(); i++) {
            if (viewInitializer.getSectionList().get(i).list.containsNode(n))
                return i;
        }

        return -1;
    }

    public Range getRange(int section, int listIndex, Node n) {
        for (int i = section; i < viewInitializer.getSectionList().size(); i++) {
            int index = viewInitializer.getSectionList().get(i).list.findNodeIndex(n, listIndex);
            if (index != -1)
                return new Range(i, index);

            listIndex = 0;
        }

        return null;
    }

    public int findNodeIndex(Node n, int section, int startIndex) {
        return viewInitializer.getSectionList().get(section).list.findNodeIndex(n, startIndex);
    }

    public TextMapElement getTextMapElement(int section, int index) {
        return viewInitializer.getSectionList().get(section).list.get(index);
    }

    /**
     * You probably don't want to be using this method. Use MapList's findNode
     * method instead. The methods using this need to be refactored
     */
    public List<TextMapElement> findNodeText(Node nodeToFind) {
        int index = list.findNodeIndex(nodeToFind, 0);
        if (index == -1)
            return null;

        logger.debug("findNodeIndex {}", index);
        // return result.get(result.size() - 1).end;
        return list.findTextMapElements(index, (Element) nodeToFind.getParent());
    }

    public String getCurrentPrintPage(int line) {
        String returnValue = null;
        //This method doesn't work if print pages are at the bottom. Alternate method is required.
        PageIndicator pageIndicator = findPrintPage(line);

        if (pageIndicator != null) {
            Element prevPrintPageNum = (Element) searchForPreviousTextMapElement(pageIndicator, (n) -> n instanceof Element
                    && (UTDElements.PRINT_PAGE_NUM.isA(n) || BBX.BLOCK.PAGE_NUM.isA(n) || BBX.SPAN.PAGE_NUM.isA(n)));
            if (prevPrintPageNum != null) {
                returnValue = UTDHelper.getFirstTextDescendant(prevPrintPageNum).getValue();
            } else {
                returnValue = pageIndicator.getPrintPageNum();
            }
        }

        return returnValue;
    }

    public Node getCurrentPrintPageMapElement(int line) {
        PageIndicator printPageIndicator = findPrintPage(line);
        Node returnValue = null;

        if (printPageIndicator != null && printPageIndicator.getPrintPageElement() != null && !printPageIndicator.getBraillePageNum().isEmpty()) {
            // You need to take into consideration the print page indicators in the middle
            // of the page, not just the print page on top of the page
            if (findPreviousPrintPageIndicator() != null) {
                try {
                    returnValue = Integer.parseInt(((Element) printPageIndicator.getPrintPageElement())
                            .getAttributeValue("printPage")) < Integer
                            .parseInt(findPreviousPrintPageIndicator().getChild(0).getValue())
                            ? UTDHelper.getAssociatedBrlElement(
                            findPreviousPrintPageIndicator().getChild(0))
                            : null;
                } catch (Exception ignored) {

                }
            }

            returnValue = printPageIndicator.getPrintPageElement();
        } else if (findPreviousPrintPageIndicator() != null) {
            returnValue = UTDHelper.getAssociatedBrlElement(findPreviousPrintPageIndicator().getChild(0));
        }
        return returnValue;
    }

    public Element findPreviousPrintPageIndicator() {
        // Make sure node to be passed isn't null
        Node node = list.getCurrent().getNode() != null ? list.getCurrent().getNode()
                : (list.getNext(true) != null ? list.getNext(true).getNode() : list.getPrevious(true).getNode());

        // Check if your current node is a page indicator. If it is, then return this
        // node
        if (node instanceof Element && "PageAction".equals(((Element) node).getAttributeValue("utd-action"))) {
            return (Element) node;
        }

        return (Element) searchForPreviousNode(node, (n) -> n instanceof Element && "PageAction".equals(((Element) n).getAttributeValue("utd-action")));
    }

    public String getCurrentBraillePage(int line) {
        PageIndicator braillePageIndicator = findBraillePage(line);
        if (braillePageIndicator != null)
            return braillePageIndicator.getBraillePageNum();
        return null;
    }

    public Node getCurrentBraillePageMapElement(int line) {
        PageIndicator braillePageIndicator = findBraillePage(line);
        if (braillePageIndicator != null)
            return braillePageIndicator.getBraillePageElement();
        return null;
    }

    private @Nullable PageIndicator findPrintPage(int line) {
        // This will not work if you have your print pages painted in the bottom
        // (different setting)
        List<PageIndicator> pages = text.paintedElements.getPageIndicators();
        if (!pages.isEmpty()) {
            int index = -1;
            for (int i = 0; i < pages.size() && line > pages.get(i).getLine(); i++) {
                index = i;
            }

            if (index > -1 && pages.get(index).getPrintPageNum() != null)
                return pages.get(index);
        }

        return null;
    }

    private @Nullable PageIndicator findBraillePage(int line) {
        List<PageIndicator> pages = text.paintedElements.getPageIndicators();
        if (!pages.isEmpty()) {
            int index = -1;
            for (int i = 0; i < pages.size() && line > pages.get(i).getLine(); i++)
                index = i;

            index++;

            if (index < pages.size() && pages.get(index).getBraillePageNum() != null)
                return pages.get(index);
        }
        return null;
    }

    public boolean isEmptyDocument() {
        return list.size() == 1 && list.get(0).getText().isEmpty();
    }

    //Addendum to isEmptyDocument - a document that's only newlines is bound to cause problems.
    public boolean documentIsOnlyNewlines(){
        //Skip the first index because it "technically" is text, even if it's just a newline.
        for (int i = 1; i < list.size(); i++){
            TextMapElement tme = list.get(i);
            if (tme.getNode() != null) {
                //Non-null nodes are good indicators of actual content in the document
                return false;
            }
        }
        return true;
    }

    public FontManager getFontManager() {
        return fontManager;
    }

    @Nullable
    public TextMapElement getCurrentTextMapElement() {
        return text.getCurrentElement();
    }
    public StyledText getTextView() {
        return text.getView();
    }

    public StyledText getBrailleView() {
        return braille.getView();
    }

    public Display getDisplay() {
        return getWpManager().getShell().getDisplay();
    }

    public BrailleDocument getDocument() {
        return document;
    }

    public ViewInitializer getViewInitializer() {
        return viewInitializer;
    }

    public TextView getText() {
        return viewManager.getTextView();
    }

    public BrailleView getBraille() {
        return viewManager.getBrailleView();
    }

    public StylePane getStylePane() {
        return viewManager.stylePane;
    }

    public Vector<String> getIgnoreList() {
        return ignoreList;
    }

    public void newIgnore(String newWord) {
        ignoreList.add(newWord);
    }

    @Override
    public void dispose() {
        text.update(false);
        list.clearList();
        containerSash.dispose();
        reformatter.close();
    }

    @NotNull
    @Override
    public Document getDoc() {
        return document.doc;
    }

    @Override
    public void setStatusBarText(BBStatusBar statusBar) {
        statusBar.setText("");
    }

    /**
     * Check is this is the default
     */
    public boolean isDefaultFile() {
        return getArchiver().getPath().equals(DEFAULT_FILE);
    }

    @Override
    public boolean canReuseTab() {
        return !isDocumentEdited() && !isDefaultFile();
    }

    @Override
    public void reuseTab(String file) {
        closeUntitledTab();
        openDocument(Paths.get(file));
        if (docCount > 0)
            docCount--;
    }

    public boolean isDocumentEdited() {
        return text.hasChanged || braille.hasChanged || isDocumentEdited;
    }

    public void setDocumentEdited(boolean documentEdited) {
        isDocumentEdited = documentEdited;
    }

    /**
     * Convenience method for ChangeTranslation to get all TextMapElements in a
     * range with no regard to parents
     */
    public Set<TextMapElement> getAllTextMapElementsInRange(int start, int end) {
        Set<TextMapElement> elementSelectedSet = new LinkedHashSet<>();
        int j = start;
        while (j < end) {
            TextMapElement t = list.getElementInRange(j);
            if (t != null) {
                elementSelectedSet.add(t);
                j = t.getEnd(getMapList()) + 1;
            } else {
                j = j + 1;
            }
        }
        return elementSelectedSet;
    }

    /**
     * Section and position of given element
     *
     * @param n The node to find.
     * @return Section index,
     */
    public Pair<Integer, Integer> getNodeIndexAllSections(Node n) {
        int startIndex = 0;
        for (int i = 0; i < viewInitializer.getSectionList().size(); i++) {
            int nodeBySection = viewInitializer.getSectionList().get(i).list.findNodeIndex(n, startIndex);
            if (nodeBySection != -1) {
                return new Pair<>(i, nodeBySection);
            }
        }
        return null;
    }

    /**
     * Find print page Section and PageMapElement, returns null if not found
     *
     * @param printPageNumber The print page number to find.
     * @return Index of section this page number was found in, the TextMapElement
     */
    public @Nullable Pair<Integer, TextMapElement> getPrintPageElement(String printPageNumber) {
        for (SectionElement curSection : viewInitializer.getSectionList()) {
            for (TextMapElement curElement : curSection.list) {
                if (!(curElement instanceof PageIndicatorTextMapElement))
                    continue;

                // PageMapElement's brailleList should be the brlOnly tag
                Node brl = curElement.brailleList.getFirst().getNode();
                while (!UTDElements.BRL.isA(brl)) {
                    brl = brl.getParent();
                }
                String brlPageNum = ((Element) brl).getAttributeValue("printPage");
                if (pageNumberEquals(brlPageNum, printPageNumber)) {
                    return new Pair<>(viewInitializer.getSectionList().indexOf(curSection), curElement);
                }
            }
        }

        // Not found, try searching the PrintPageElements
        // TODO: This can put the cursor at a paragraph that continues onto the
        // next page,
        // any way to detect this?
        for (SectionElement curSection : viewInitializer.getSectionList()) {
            for (TextMapElement curElement : curSection.list) {
                for (BrailleMapElement curBrailleElement : curElement.brailleList) {
                    if (!(curBrailleElement instanceof PrintPageBrlMapElement))
                        continue;
                    // Check both the origional page and the translated braille
                    // equivelent
                    String origPage = ((Element) curBrailleElement.getNode()).getAttributeValue("printPage");
                    String braillePage = curBrailleElement.getNode().getValue();
                    if (pageNumberEquals(origPage, printPageNumber) || pageNumberEquals(braillePage, printPageNumber))
                        return new Pair<>(viewInitializer.getSectionList().indexOf(curSection), curElement);
                }
            }
        }

        // Not found
        return null;
    }

    private static boolean pageNumberEquals(String orig, String compareTo) {
        if (orig.equalsIgnoreCase(compareTo)) {
            return true;
        } else if (orig.contains("-")) {
            // Issue #3814: Temporary way to find combined pages
            String[] parts = orig.split("-", 2);
            return parts[0].equals(compareTo) || parts[1].equals(compareTo);
        } else {
            return false;
        }
    }

    // Corey 10/10/2016: Commenting this out because it's not used but not sure
    // if it will ever be needed

    @Nullable
    public Pair<Integer, TextMapElement> getBraillePageElementByUntranslatedPage(
            String untranslatedBraillePage, @Nullable nu.xom.Text startNode) {
        boolean afterStartNode = false;
        for (SectionElement curSection : viewInitializer.getSectionList()) {
            for (TextMapElement curElement : curSection.list) {
                if (startNode != null && !afterStartNode) {
                    if (curElement.getNode() == startNode) {
                        afterStartNode = true;
                    } else {
                        continue;
                    }
                }

                for (BrailleMapElement curBrailleElement : curElement.brailleList) {
                    if (!(curBrailleElement instanceof NewPageBrlMapElement))
                        continue;
                    String page = ((Element) curBrailleElement.getNode()).getAttributeValue("untranslated");
                    if (untranslatedBraillePage.equalsIgnoreCase(page))
                        return new Pair<>(viewInitializer.getSectionList().indexOf(curSection), curElement);
                }
            }
        }

        // Not found
        return null;
    }

    /**
     * Get the TextMapElement that contains a braillePage that matches the given
     * number
     *
     * @param rawPageIndex The raw page index to find.
     * @return Index of section this page number was found in, the TextMapElement
     */
    @Nullable
    public Pair<Integer, TextMapElement> getBraillePageElement(int rawPageIndex) {
        int counter = 0;
        for (SectionElement curSection : viewInitializer.getSectionList()) {
            for (TextMapElement curElement : curSection.list) {
                for (BrailleMapElement curBrailleElement : curElement.brailleList) {
                    if (!(curBrailleElement instanceof BraillePageBrlMapElement))
                        continue;
                    if (counter == rawPageIndex)
                        return new Pair<>(viewInitializer.getSectionList().indexOf(curSection), curElement);
                    counter++;
                }
            }
        }

        // Not found
        return null;
    }

    public Element getVolumeAtCursor() {
        Node currentNode = list.getCurrentNonWhitespace(getTextView().getCaretOffset()).getNode();
        return VolumeUtils.getVolumeAfterNode(getDoc(), currentNode);
    }

    /**
     * Helper method to prevent exceptions being thrown from the caret being placed
     * between characters of a line delimiter
     */
    public void setTextCaret(int offset) {
        // during undo/redo sometimes offset no longer exists
        if (offset > text.getView().getCharCount()) {
            offset = text.getView().getCharCount();
        }

        int currentLine = text.getView().getLineAtOffset(offset);
        int offsetAtLine = text.getView().getOffsetAtLine(currentLine);
        int positionInLine = offset - offsetAtLine;
        int lineLength = text.getView().getLine(currentLine).length();
        if (positionInLine > lineLength) {
            // getLine(line).length() is the end of the line before the line
            // delimiter.
            // If the offset is more than that, the offset is in between two
            // characters of the line delimiter
            offset = offsetAtLine + lineLength;
        }

        // text.view.setCaretOffset(offset);
        // TODO: if this resolves caret not being reset, remove commented line
        // above
        // this current element is reset which resets the caret,
        text.setCurrentElement(offset);
        if (this.getStylePane().getUpdateStylePane() && !this.getTextView().isDisposed()) {
            this.getStylePane().updateCursor(this.getTextView().getLineAtOffset(this.getTextView().getCaretOffset()));
        }
    }

    public Document getLastCopiedDoc() {
        return lastCopiedDoc;
    }

    public void openBookTree() {
        checkForUpdatedViews();
        waitForFormatting(true);
        new BookTreeDialog(this);
    }

    public void initializeListeners() {
        viewManager.initializeListeners();
    }

    public void removeListeners() {
        viewManager.removeListeners();
    }

    public int getFirstSection() {
        return viewInitializer.findFirst();
    }

    public int getLastSection() {
        return viewInitializer.findLast();
    }

    public MapList resetSection(int sectionNumber) {
        return viewInitializer.bufferViews(this, sectionNumber);
    }

    public MapList useResetSectionMethod(int sectionNumber) {
        return viewInitializer.resetViews(this, sectionNumber);
    }

    public ArrayList<SectionElement> getSectionList() {
        return viewInitializer.getSectionList();
    }

    public void addUndoEvent(EventFrame f) {
        Objects.requireNonNull(getSimpleManager().getModule(UndoRedoModule.class)).addUndoEvent(f);
    }

    public void addRedoEvent(EventFrame f) {
        Objects.requireNonNull(getSimpleManager().getModule(UndoRedoModule.class)).addRedoEvent(f);
    }

    @Nullable
    public EventFrame peekUndoEvent() {
        return Objects.requireNonNull(getSimpleManager().getModule(UndoRedoModule.class)).peekUndoEvent();
    }

    public EventFrame popUndo() {
        return Objects.requireNonNull(getSimpleManager().getModule(UndoRedoModule.class)).popUndoEvent();
    }

    public IAction getAction(Node n) {
        return document.getEngine().getActionMap().findValueOrDefault(n);
    }

    public @Nullable IStyle getStyle(@NotNull Node n) {
        return document.getEngine().getStyle(n);
    }

    public Node getBlock(Node n) {
        return document.getEngine().findTranslationBlock(n);
    }

    public MapList getMapList() {
        return list;
    }

    public ViewManager getViewManager() {
        return viewManager;
    }

    public int getTotalPagesInView() {
        int totalPages = 0;
        for (int i = 0; i < viewInitializer.getSectionList().size(); i++) {
            if (viewInitializer.getSectionList().get(i).isVisible())
                totalPages += viewInitializer.getSectionList().get(i).pages;
        }
        return totalPages;
    }

    public BBSimpleManager getSimpleManager() {
        return simpleManager;
    }

    private void countDownAllLatches() {
        if (rebuiltSectionLatch != null) {
            rebuiltSectionLatch.countDown();
        }
        if (rebuiltSectionMapLatch != null) {
            rebuiltSectionMapLatch.countDown();
        }
        if (finishFormattingLatch != null) {
            finishFormattingLatch.countDown();
        }
    }

    @NotNull
    @Override
    public Archiver2 getArchiver() {
        return Objects.requireNonNull(archiver, "Manager not open");
    }

    class Reformatter implements Runnable {
        Element newPage;
        Element printPageBrl;
        public final boolean finished;
        public boolean busy;
        volatile boolean close = false;
        final Manager manager;
        CountDownLatch workLatch = new CountDownLatch(1);

        public Reformatter(Manager manager) {
            logger.debug("Reformatter initialized");
            finished = false;
            this.manager = manager;
        }

        @Override
        public void run() {
            boolean running = true;
            while (running) {
                try {
                    while (newPage == null) {
                        workLatch.await(10, TimeUnit.SECONDS);
                        if (newPage == null && manager.getWpManager().getShell().isDisposed()) return;
                        if (close)
                            return;
                    }
                } catch (InterruptedException ignored) {
                }
                if (close)
                    return;
                try {
                    finishFormattingLatch = new CountDownLatch(1);
                    restartFormatFlag = false;
                    logger.debug("Sending newPage: {} and printPageBrl: {} to partialFormat", newPage.toXML(), printPageBrl);
                    document.getEngine().partialFormat(newPage, printPageBrl);
                } catch (UTDInterruption e) {
                    logger.debug("Reformatter was interrupted");
                } catch (RuntimeException e) {
                    formatterException = e;
                } finally {
                    busy = false;
                    workLatch = new CountDownLatch(1);
                    newPage = null;
                    printPageBrl = null;
                    finishFormattingLatch.countDown();
                    if (formatterException != null) {
                        /*
                         * added because when UTDTranslationEngine class failed to initialize, this
                         * could end up being called when there is no GUI
                         */
                        if (rebuiltSectionLatch != null)
                            rebuiltSectionLatch.countDown();
                    }
                    if (close)
                        running = false;
                }
            }
        }

        public void startFormat(Element newPage, Element printPageBrl) {
            logger.debug("StartFormat received newPage: {} PrintPageBrl: {}", newPage.toXML(), printPageBrl);
            this.newPage = newPage;
            this.printPageBrl = printPageBrl;
            if (busy) {
                throw new IllegalStateException("StartFormat called while busy");
            }
            busy = true;
            workLatch.countDown();
        }

        /**
         * Sets restartFormatFlag for _reformat() to throw a UTDInterruption
         */
        public void stopFormat() {
            logger.debug("StopFormat called. Reformatter busy? {}", busy);
            newPage = null;
            printPageBrl = null;
            restartFormatFlag = true;
            workLatch = new CountDownLatch(1);
        }

        /**
         * Closes thread. To be used when exiting BB
         */
        public void close() {
            logger.debug("Closing reformatter thread");
            close = true;
            if (busy)
                stopFormat();
            workLatch.countDown();
        }
    }

}
