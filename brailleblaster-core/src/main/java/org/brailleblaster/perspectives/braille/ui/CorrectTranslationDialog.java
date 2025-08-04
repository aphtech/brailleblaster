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
package org.brailleblaster.perspectives.braille.ui;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.brailleblaster.BBIni;
import org.brailleblaster.utils.localization.LocaleHandler;
import org.brailleblaster.perspectives.braille.Manager;
import org.brailleblaster.perspectives.braille.document.BrailleDocument;
import org.brailleblaster.perspectives.braille.views.wp.TextView;
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.settings.TableExceptions;
import org.brailleblaster.tools.MenuToolListener;
import org.brailleblaster.utd.UTDTranslationEngine;
import org.brailleblaster.util.FileUtils;
import org.brailleblaster.util.Notify;
import org.brailleblaster.wordprocessor.WPManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jetbrains.annotations.NotNull;
import org.mwhapples.jlouis.Louis;
import org.mwhapples.jlouis.TranslationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * This class creates the dialog for transcribers to correct a word or phrase
 * that is translated wrong. They can save the changes locally or upload to be
 * reviewed by BrailleBlaster.
 */
public class CorrectTranslationDialog extends Dialog implements MenuToolListener {
    private static final LocaleHandler localeHandler = LocaleHandler.getDefault();

    private static final Logger log = LoggerFactory.getLogger(CorrectTranslationDialog.class);
    private static final String CORRECT_TRANSLATION_SHELL = localeHandler.get("correctTranslationShell");
    private static final String INSTRUCTION_P1 = localeHandler.get("instructionP1a");
    //Don't use P1, as there is no functional Global Save option.
    private static final String INSTRUCTION_P2 = localeHandler.get("instructionP2");
    private static final String INSTRUCTION_P3 = localeHandler.get("instructionP3");
    private static final String CHOOSE_OPERATION = localeHandler.get("chooseOperation");
    private static final String DEFINE_NEW_CHARACTER = localeHandler.get("defineNewCharacter");
    private static final String CORRECT_CHARACTER = localeHandler.get("correctCharacter");
    private static final String CORRECT_WORD = localeHandler.get("correctWord");
    private static final String PRINT = localeHandler.get("ctd.print");
    private static final String CHOOSE_ENTRY_METHOD_FOR_BRAILLE = localeHandler.get("chooseEntryForBraille");
    private static final String SIX_KEY = localeHandler.get("sixKey");
    private static final String ASCII = localeHandler.get("ascii");
    private static final String BRAILLE = localeHandler.get("braille");
    private static final String SAVE_TRANSLATION_LOCALLY = localeHandler.get("saveTranslationLocally");
    protected static final String BOTH_BOXES_NEED_INPUT = localeHandler.get("bothBoxesNeedInput");
    protected static final String ONE_WORD_CHARACTER_AT_ONCE = localeHandler.get("oneWordCharacterAtOnce");
    private static final String CANCEL = localeHandler.get("ctd.cancel");
    private static final String VIEW_CORRECTIONS = localeHandler.get("viewCorrections");
    private static final String CORRECT_UNDEFINED_CHARACTER_WARNING = localeHandler.get("correctUndefinedCharacterWarning");
    private static final String SUCCESS = localeHandler.get("success");
    private static final String TABLE_UPDATED_DOC_RETRANSLATED = localeHandler.get("tableUpdatedDocRetranslated");
    private static final String OK = localeHandler.get("ctd.ok");
    private static final String CORRECTED_TRANSLATIONS_SHELL = localeHandler.get("correctedTranslationsShell");
    private static final String EDIT_TRANSLATION = localeHandler.get("editTranslation");
    protected static final String SELECT_LIST_ITEM = localeHandler.get("selectListItem");
    private static final String DELETE_ENTRY = localeHandler.get("deleteEntry");
    private Manager man;
    private UTDTranslationEngine engine;
    private Properties dfnProps, corrProps;
    private BrailleDocument brailleDoc;

    private Shell ctDialog, vcDialog;
    private Text brailleText, printText;
    private Button correctWord;
    private Button correctChar;
    private Button ascii;
    private Button sixKey;
    private List list;

    @SuppressWarnings("unused")
    private final static String OP_NO_BACK = "noback";// I'm leaving this unused opcode
    private final static String FILE_SEP = FileSystems.getDefault().getSeparator();
    private final static String PRINT_BLANK = " ";
    private String dfnPropertiesPath;
    private String corrPropertiesPath;
    private final static String FONT_PATH = BBIni.getProgramDataPath() + FILE_SEP + "fonts" + FILE_SEP + "APH_Braille_Font-6s.otf";
    private String dots = "", asciiBraille = "", unicode = "";
    private final static String OP_ALWAYS = "always";
    private final static String OP_WORD = "word";
    private final static String OP_SIGN = "sign";

    public CorrectTranslationDialog(Shell parent, int style) {
        super(parent, style);
    }

    @Override
    public @NotNull TopMenu getTopMenu() {
        return TopMenu.TOOLS;
    }

    @Override
    public @NotNull String getTitle() {
        return localeHandler.get("&correctTranslation");
    }

    @Override
    public int getAccelerator() {
        return SWT.MOD1 | 'T';
    }

    @Override
    public void onRun(BBSelectionData bbData) {
        man = bbData.getManager();
        open();
    }

    public void open() {
        engine = man.getDocument().getEngine();
        brailleDoc = man.getDocument();
        dfnPropertiesPath = BBIni.getUserProgramDataPath().resolve(Paths.get("settings", getTableType() + "-define.properties")).toString();

        corrPropertiesPath = BBIni.getUserProgramDataPath().resolve(Paths.get("settings", getTableType() + "-correct.properties")).toString();
        Shell parent = getParent();
        ctDialog = new Shell(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM | SWT.RESIZE);
        ctDialog.setText(CORRECT_TRANSLATION_SHELL);
        GridLayout grid = new GridLayout(1, false);
        ctDialog.setLayout(grid);

        Composite outerContainer = new Composite(ctDialog, SWT.NONE);
        outerContainer.setLayout(new GridLayout(1, false));
        outerContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        ScrolledComposite sc = new ScrolledComposite(outerContainer, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        sc.setExpandVertical(true);
        sc.setExpandHorizontal(true);

        Composite innerContainer = new Composite(sc, SWT.NONE);
        innerContainer.setLayout(new GridLayout(1, false));
        innerContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label explain = new Label(innerContainer, SWT.NONE);
        String blankline = "\n\n";
        explain.setText(addNewLines(INSTRUCTION_P1) + blankline + addNewLines(INSTRUCTION_P2) + blankline + addNewLines(INSTRUCTION_P3));

        Group radioPrint = new Group(innerContainer, SWT.NONE);
        radioPrint.setLayout(new GridLayout(1, false));
        Label radioLabelPrint = new Label(radioPrint, SWT.NULL);
        radioLabelPrint.setText(CHOOSE_OPERATION);
        Button define = new Button(radioPrint, SWT.RADIO);
        define.setText(DEFINE_NEW_CHARACTER);

        correctChar = new Button(radioPrint, SWT.RADIO);
        correctChar.setText(CORRECT_CHARACTER);

        correctWord = new Button(radioPrint, SWT.RADIO);
        correctWord.setText(CORRECT_WORD);
        correctWord.setSelection(true);

        Label printLabel = new Label(innerContainer, SWT.NULL);
        printText = new Text(innerContainer, SWT.BORDER);
        printText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
        printText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.stateMask == SWT.CTRL && e.keyCode == 'a') {
                    printText.selectAll();
                }
            }
        });


        printLabel.setText(PRINT);
        TextView tv = man.getText();
        boolean textSelection = tv.getView().getSelectionCount() > 0;
        if (textSelection) {
            printText.setText(tv.getView().getSelectionText());
        }
        Group radio = new Group(innerContainer, SWT.NONE);
        radio.setLayout(new GridLayout(1, false));
        Label radioLabel = new Label(radio, SWT.NULL);
        radioLabel.setText(CHOOSE_ENTRY_METHOD_FOR_BRAILLE);
        sixKey = new Button(radio, SWT.RADIO);
        sixKey.setText(SIX_KEY);
        sixKey.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (sixKey.getSelection()) {
                    brailleText.setText("");
                    dots = "";
                    simBraille();
                }

            }
        });
        ascii = new Button(radio, SWT.RADIO);
        ascii.setText(ASCII);
        ascii.setSelection(true);
        ascii.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (ascii.getSelection()) {
                    brailleText.setText("");
                    dots = "";
                    noSimBraille();
                }
            }
        });

        Label brailleLabel = new Label(innerContainer, SWT.NULL);
        brailleLabel.setText(BRAILLE);
        brailleText = new Text(innerContainer, SWT.BORDER);
        brailleText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
        brailleText.addVerifyListener(e -> {
            log.debug(e.toString());
            // If we are using six-key input we don't want the normal key
            // presses to generate text in the braille text widget
            if ((e.character > 32 && e.character < 127) && (sixKey.getSelection()))
                e.doit = false;
        });
        brailleText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.stateMask == SWT.CTRL && e.keyCode == 'a') {
                    brailleText.selectAll();
                }
            }
        });

        // TODO: Use SixKeyWidget
        brailleText.addKeyListener(new KeyAdapter() {
            private byte downKeys = 0;
            private byte dotsByte = 0;
            private static final byte DOT_1 = 0x01;
            private static final byte DOT_2 = 0x02;
            private static final byte DOT_3 = 0x04;
            private static final byte DOT_4 = 0x08;
            private static final byte DOT_5 = 0x10;
            private static final byte DOT_6 = 0x20;

            @Override
            public void keyPressed(KeyEvent e) {
                if (!sixKey.getSelection()) {
                    return;
                }

                char input = Character.toLowerCase(e.character);
                if (input == 'f') {
                    setDots(DOT_1);
                } else if (input == 'd') {
                    setDots(DOT_2);
                } else if (input == 's') {
                    setDots(DOT_3);
                } else if (input == 'j') {
                    setDots(DOT_4);
                } else if (input == 'k') {
                    setDots(DOT_5);
                } else if (input == 'l') {
                    setDots(DOT_6);
                } else if (e.keyCode == SWT.BS || e.keyCode == SWT.DEL) {
                    dots = "";
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (!sixKey.getSelection()) {
                    return;
                }

                char input = Character.toLowerCase(e.character);
                if (input == 'f') {
                    showKey();
                } else if (input == 'd') {
                    showKey();
                } else if (input == 's') {
                    showKey();
                } else if (input == 'j') {
                    showKey();
                } else if (input == 'k') {
                    showKey();
                } else if (input == 'l') {
                    showKey();
                }
            }

            private void showKey() {
                if (downKeys == 0) {
                    String s = byteToDotsString(dotsByte);
                    addToDotString(s);
                    byteToAsciiString(dotsByte);
                    dotsByte = 0;
                }
            }

            private void setDots(byte dot) {
                downKeys &= dot;
                dotsByte |= dot;
            }
        });

        Group saveOptions = new Group(innerContainer, SWT.NONE);
        GridLayout gridSaveOptions = new GridLayout(3, false);
        saveOptions.setLayout(gridSaveOptions);

        Button saveLocal = new Button(saveOptions, SWT.PUSH);
        saveLocal.setText(SAVE_TRANSLATION_LOCALLY);
        saveLocal.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (StringUtils.isBlank(brailleText.getText()) || StringUtils.isBlank(printText.getText()))
                    Notify.INSTANCE.notify(BOTH_BOXES_NEED_INPUT, Notify.ALERT_SHELL_NAME);
                else if (printText.getText().contains(" ")) {
                    Notify.INSTANCE.notify(ONE_WORD_CHARACTER_AT_ONCE,
                            Notify.ALERT_SHELL_NAME);
                } else {
                    putInPropertiesFile();
                    if (saveLocally()) {
                        ctDialog.close();
                    }
                }
            }
        });


        Button cancel = new Button(saveOptions, SWT.PUSH);
        cancel.setText(CANCEL);
        cancel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ctDialog.close();
            }
        });

        Button seeCorrections = new Button(saveOptions, SWT.PUSH);
        seeCorrections.setText(VIEW_CORRECTIONS);
        seeCorrections.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                makeViewDialog();
            }
        });

        FileUtils.INSTANCE.create(dfnPropertiesPath);
        dfnProps = new Properties();
        try {
            dfnProps.load(new FileInputStream(dfnPropertiesPath));
        } catch (IOException e) {
            throw new RuntimeException("Unable to read file " + dfnPropertiesPath, e);
        }

        FileUtils.INSTANCE.create(corrPropertiesPath);
        corrProps = new Properties();
        try {
            corrProps.load(new FileInputStream(corrPropertiesPath));
        } catch (IOException e) {
            throw new RuntimeException("Unable to read file " + corrPropertiesPath, e);
        }
        sc.setContent(innerContainer);
        sc.setMinSize(innerContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        ctDialog.pack();
        ctDialog.layout(true);
        ctDialog.setSize(
                ctDialog.computeSize(SWT.DEFAULT, Display.getCurrent().getPrimaryMonitor().getClientArea().height / 2));
        ctDialog.open();
    }

    private String addNewLines(String s1) {
        StringBuilder s2 = new StringBuilder();
        int cutPoint = 80;
        int curChars = 0;
        String[] array = s1.split(" ");
        for (String s : array) {
            if (curChars > cutPoint) {
                s2.append("\n");
                curChars = 0;
            }
            s2.append(s).append(" ");
            curChars += s.length();
        }
        return s2.toString();
    }

    /**
     * Save the corrected phrase to the user's local BB exception table
     */
    public boolean saveLocally() {
        File file = TableExceptions.getCurrentExceptionFile(man);

        if (file == null || !file.exists())
            // This should have been created in SettingsManager
            throw new RuntimeException("User exceptions table not found: " + (file != null ? file.getAbsolutePath() : null));

        try (OutputStreamWriter f = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            f.append(TableExceptions.UNIVERSAL_EXCEPTION_FILE_HEADING);
            f.append(System.lineSeparator());
            for (Entry<Object, Object> e : dfnProps.entrySet()) {
                String[] value = e.getValue().toString().split(",");
                f.append(PRINT_BLANK).append(value[0]).append(PRINT_BLANK);
                f.append(e.getKey().toString());
                f.append(PRINT_BLANK);
                f.append(value[1]);
                f.append(System.lineSeparator());
            }
            f.append(System.lineSeparator());
            for (Entry<Object, Object> e : corrProps.entrySet()) {
                String[] value = e.getValue().toString().split(",");
                f.append(PRINT_BLANK).append(value[0]).append(PRINT_BLANK);
                f.append(e.getKey().toString());
                f.append(PRINT_BLANK);
                f.append(value[1]);
                f.append(System.lineSeparator());
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to write exceptions to file " + file.getAbsolutePath(), e);
        }
        try {
            sanityCheck();
        } catch (RuntimeException | TranslationException e) {
            Notify.INSTANCE.notify(CORRECT_UNDEFINED_CHARACTER_WARNING, Notify.ALERT_SHELL_NAME);
            removeUndefinedCharacter(printText.getText().toLowerCase());
            return false;
        }
        refreshTranslation();
        savedCorrectly();
        return true;
    }

    /**
     * Dialog telling the transcriber the information was saved correctly
     */

    public void savedCorrectly() {
        Shell saved = new Shell(getParent(), SWT.NONE | SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        saved.setText(SUCCESS);
        saved.setLayout(new GridLayout(1, true));
        Label label = new Label(saved, SWT.NONE);
        label.setText(TABLE_UPDATED_DOC_RETRANSLATED);
        Button button = new Button(saved, SWT.PUSH);
        button.setData(new GridData(1, 1, true, true));
        button.setText(OK);
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                saved.close();
            }
        });
        saved.open();
        saved.pack();
    }

    /**
     * This method checks to make sure the character is defined before it starts
     * messing with the refreshing methods. Redundant but good for now.
     */
    public void sanityCheck() throws TranslationException {
        engine.getBrailleTranslator().close();
        engine.getBrailleTranslator().translateString(TableExceptions.getCurrentExceptionTable(man),
                printText.getText().toLowerCase(), 0);
    }

    /**
     * This method removes a character that threw an exception from the sanity
     * check method.
     */
    public void removeUndefinedCharacter(String stringToRemove) {
        dfnProps.remove(stringToRemove);
        corrProps.remove(stringToRemove);
        brailleText.setText("");
        dots = "";
        asciiBraille = "";
        unicode = "";
        try {
            dfnProps.store(new FileOutputStream(dfnPropertiesPath), null);
            corrProps.store(new FileOutputStream(corrPropertiesPath), null);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save file " + dfnPropertiesPath, e);
        }
        File file = brailleDoc.getSettingsManager().getExceptionsTableFile();
        if (file == null || !file.exists())
            // This should have been created in SettingsManager
            throw new RuntimeException("User exceptions table not found: " + (file != null ? file.getAbsolutePath() : null));

        try (OutputStreamWriter f = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            f.append(TableExceptions.UNIVERSAL_EXCEPTION_FILE_HEADING);
            f.append(System.lineSeparator());
            for (Entry<Object, Object> e : dfnProps.entrySet()) {
                String[] s = e.getValue().toString().split(",");
                f.append(PRINT_BLANK);
                f.append(s[0]);
                f.append(PRINT_BLANK);
                f.append(e.getKey().toString());
                f.append(PRINT_BLANK);
                f.append(s[1]);
                f.append(System.lineSeparator());
            }
            for (Entry<Object, Object> e : corrProps.entrySet()) {
                String[] s = e.getValue().toString().split(",");
                f.append(PRINT_BLANK);
                f.append(s[0]);
                f.append(PRINT_BLANK);
                f.append(e.getKey().toString());
                f.append(PRINT_BLANK);
                f.append(s[1]);
                f.append(System.lineSeparator());
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to write exceptions to file " + file.getAbsolutePath(), e);
        }
    }

    /**
     * This method will refresh the translation in the view by recompiling
     * liblouis and then refreshing the view.
     */
    public void refreshTranslation() {
        engine.getBrailleTranslator().close();
        man.refresh();
    }

    /**
     * This class will create a properties file from and to which the exceptions
     * will be written. This allows the user to rewrite previously written
     * exceptions. Upon saving locally, this properties file will be written to
     * the exceptions table.
     */
    public void putInPropertiesFile() {
        unicode = dots = asciiBraille = "";
        parseWordsFromBrailleInputBox();

            if (correctWord.getSelection()) {
                corrProps.setProperty(printText.getText().toLowerCase(), OP_WORD + "," + dots + ","
                        + asciiBraille + "," + unicode);
            } else if (correctChar.getSelection()) {
                corrProps.setProperty(printText.getText().toLowerCase(), OP_ALWAYS + "," + dots + ","
                        + asciiBraille + "," + unicode);
            }
            else {
                // because it may be used in the
                // future.
                dfnProps.setProperty(printText.getText().toLowerCase(), OP_SIGN + "," + dots + ","
                        + asciiBraille + "," + unicode);
                corrProps.setProperty(printText.getText().toLowerCase(), OP_ALWAYS + "," + dots + ","
                        + asciiBraille + "," + unicode);
            }

        try {
            if (corrProps != null) {
                corrProps.store(new FileOutputStream(corrPropertiesPath), null);
            }
            if (dfnProps != null) {
                dfnProps.store(new FileOutputStream(dfnPropertiesPath), null);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to save properties file.", e);
        }

    }

    /**
     * This method converts the Ascii characters from the braille text widget
     * into a byte array to be stored in the table
     */
    public void processWord(String brailleString) {

        if (brailleString.isEmpty()) {
            return;
        }

        Louis translation = engine.getBrailleTranslator();
        Louis.WideChar input = new Louis.WideChar(brailleString);
        Louis.WideChar outputLouis = new Louis.WideChar(brailleString.length());
        translation.charToDots(TableExceptions.getCurrentExceptionTable(man), input, outputLouis,
                brailleString.length(), 0);
        String outputStr = outputLouis.getText(outputLouis.length());
        byte[] os;
        os = outputStr.getBytes(StandardCharsets.UTF_16LE);
        for (byte o : os) {
            addToDotString(byteToDotsString(o));
            addByteToUnicodeString(o);
        }
    }

    private void parseWordsFromBrailleInputBox() {
        asciiBraille = brailleText.getText();// locked
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < asciiBraille.length(); i++) {
            if (asciiBraille.charAt(i) == ' ') {
                processWord(word.toString());
                addBlank();
                word = new StringBuilder();
            } else {
                word.append(asciiBraille.charAt(i));
            }
        }
        processWord(word.toString());
    }

    private void addBlank() {
        addToDotString("0");
        unicode += "\\u2800";
    }

    /**
     * This method takes the dots calculated from the six key and converts them
     * to a string of integers (1-6) to be written to the table.
     */
    public String byteToDotsString(byte dots) {
        byte mask = 0x01;
        StringBuilder result = new StringBuilder();
        for (int c = 1; c < 7; c++) {
            if ((dots & mask) == mask)
                result.append(c);
            mask = (byte) (mask << 1);
        }
        return result.toString();
    }

    /**
     * This method determines what to put in the braille string,building a
     * string of braille dots separated by dashes.
     */
    public void addToDotString(String braille) {
        if (braille.isEmpty())
            return;
        if (dots.isEmpty()) {
            dots += braille;
        } else {
            dots = dots + "-" + braille;
        }
    }

    /**
     * This method takes the byte from the six key entry, converts it into a
     * UTF-16 encoded string for liblouis, sends into through the dots to char
     * method, and then converts the resulting string to UTF-16 so that it will
     * concatenate properly with the existing string and appear in the widget.
     */
    public void byteToAsciiString(byte b) {
        Louis translation = engine.getBrailleTranslator();
        byte[] bb = new byte[2];
        bb[0] = b;
        bb[1] = 0x28;
        String s = new String(bb, StandardCharsets.UTF_16LE);
        Louis.WideChar input = new Louis.WideChar(s);
        Louis.WideChar outputLouis = new Louis.WideChar(2);
        translation.dotsToChar(TableExceptions.getCurrentExceptionTable(man), input, outputLouis, s.length(), 1);
        String outputStr = outputLouis.getText(outputLouis.length());
        putInBrailleTextWidget(outputStr.substring(0, 1));

    }

    public void addByteToUnicodeString(byte bite) {
        if (bite >= 0) {
            byte[] bb = new byte[2];
            bb[0] = bite;
            bb[1] = 0x28;
            String s2 = new String(bb, StandardCharsets.UTF_16LE);

            unicode += "\\u" + Integer.toHexString(s2.charAt(0) | 0x10000).substring(1);
        }
    }

    /**
     * This method puts the six key char, seen in simBraille, into the widget.
     */
    public void putInBrailleTextWidget(String s) {
        if (s.isEmpty())
            return;
        brailleText.insert(s);
    }

    /**
     * The method sets the braille text to simBraille font.
     */
    public void simBraille() {
        ctDialog.getDisplay().loadFont(FONT_PATH);
        Font simBrailleFont = new Font(WPManager.display, "SimBraille", 12, SWT.NORMAL);
        brailleText.setFont(simBrailleFont);
    }

    public void noSimBraille() {
        Font courier = new Font(WPManager.display, "Courier New", 12, SWT.NORMAL);
        brailleText.setFont(courier);
    }

    /**
     * Method searches the users settings file for the braille translation
     * schema.
     *
     * @return String either UEB or EBAE
     */
    public String getTableType() {
        return TableExceptions.getCurrentStandardName(man);
    }

    // --------View corrections dialog---------

    /**
     * Make the view of the previous corrections.
     */
    public void makeViewDialog() {

        Shell parent = getParent();
        vcDialog = new Shell(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
        vcDialog.setText(CORRECTED_TRANSLATIONS_SHELL);
        vcDialog.setLayout(new GridLayout(1, false));

        Composite container = new Composite(vcDialog, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        GridData myGrid = new GridData(SWT.FILL, SWT.FILL, true, true);
        Rectangle bounds = Display.getCurrent().getPrimaryMonitor().getClientArea();
        myGrid.widthHint = bounds.width / 3;
        myGrid.heightHint = bounds.height / 2;
        list = new List(vcDialog, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
        list.setLayoutData(myGrid);

        java.util.List<String> keys = new ArrayList<>(corrProps.stringPropertyNames());
        Collections.sort(keys);
        String[] keyValues;

        for (int i = 0; i < corrProps.size(); i++) {
            String b = corrProps.getProperty(keys.get(i));
            keyValues = b.split(",");

            String mode = ascii.getSelection() ? keyValues[2] : StringEscapeUtils.unescapeJava(keyValues[3]);
            list.add(keys.get(i) + " → " + mode);
        }

        Group saveOptions = new Group(vcDialog, SWT.NONE);
        GridLayout gridSaveOptions = new GridLayout(3, false);
        saveOptions.setLayout(gridSaveOptions);

        Button cancel = new Button(saveOptions, SWT.PUSH);
        cancel.setText(CANCEL);
        cancel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                vcDialog.close();
                ctDialog.setActive();// if we don't do this BB loses focus
                // control
            }
        });

        Button edit = new Button(saveOptions, SWT.PUSH);
        edit.setText(EDIT_TRANSLATION);
        edit.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent se) {
                if (list.getSelectionIndex() == -1) {
                    Notify.INSTANCE.notify(SELECT_LIST_ITEM, Notify.ALERT_SHELL_NAME);
                    return;
                }
                String[] key = list.getItem(list.getSelectionIndex()).split("→");
                String s = corrProps.getProperty(key[0].trim());
                String[] st = s.split(",");
                printText.setText(key[0].trim());
                brailleText.setText(st[2]);
                vcDialog.close();
                printText.setFocus();
            }

        });

        Button delete = new Button(saveOptions, SWT.PUSH);
        delete.setText(DELETE_ENTRY);
        delete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (list.getSelectionIndex() == -1) {
                    Notify.INSTANCE.notify(SELECT_LIST_ITEM, Notify.ALERT_SHELL_NAME);
                    return;
                }
                String[] key = list.getItem(list.getSelectionIndex()).split("→");
                removeUndefinedCharacter(key[0].trim());
                refreshTranslation();
                vcDialog.close();
                printText.setFocus();
            }
        });
        vcDialog.pack();
        vcDialog.layout(true);
        vcDialog.open();
    }
}
