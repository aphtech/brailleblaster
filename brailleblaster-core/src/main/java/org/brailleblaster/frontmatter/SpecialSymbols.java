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
package org.brailleblaster.frontmatter;

import nu.xom.*;
import org.brailleblaster.BBIni;
import org.brailleblaster.settings.UTDManager;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.brailleblaster.utd.properties.UTDElements;
import org.brailleblaster.util.Notify;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpecialSymbols {
    private static final String DEFAULT_SYMBOL_PATH = BBIni.getProgramDataPath().resolve(Paths.get("settings", "symbols.xml")).toString();
    private static final String CUSTOM_SYMBOL_PATH = BBIni.getUserProgramDataPath().resolve(Paths.get("settings", "symbols.xml")).toString();
    private static final Logger log = LoggerFactory.getLogger(SpecialSymbols.class);
    private static final String RULE_NAME_WHOLE_WORD = "Whole Word";
    private static final String RULE_NAME_BEGINNING_OF_WORD = "Beginning of Word";
    private static final String RULE_NAME_END_OF_WORD = "End of Word";
    private static final String RULE_NAME_DIRECT_TRANSLATED = "Direct Translated";
    private static final String RULE_NAME_FOLLOWED_BY = "Followed By";
    private static final String RULE_NAME_PRECEDED_BY = "Preceded By";
    private static final String RULE_NAME_NONE = "None";
    private static final String RULE_MODIFIER_NOT = "Not ";
    private static final String RULE_MODIFIER_ALWAYS = " [Always]";
    //For menus
    public static final String[] RULES = new String[]{
            RULE_NAME_NONE,
            RULE_NAME_WHOLE_WORD, RULE_MODIFIER_NOT + RULE_NAME_WHOLE_WORD,
            RULE_NAME_BEGINNING_OF_WORD, RULE_MODIFIER_NOT + RULE_NAME_BEGINNING_OF_WORD,
            RULE_NAME_END_OF_WORD, RULE_MODIFIER_NOT + RULE_NAME_END_OF_WORD,
            RULE_NAME_DIRECT_TRANSLATED, RULE_MODIFIER_NOT + RULE_NAME_DIRECT_TRANSLATED,
            RULE_NAME_FOLLOWED_BY, RULE_MODIFIER_NOT + RULE_NAME_FOLLOWED_BY,
            RULE_NAME_PRECEDED_BY, RULE_MODIFIER_NOT + RULE_NAME_PRECEDED_BY
    };

    public static class Symbol {
        private String symbol;
        private String desc;
        private List<DetectionRule> rules;

        public Symbol(String name, String desc, List<DetectionRule> rules) {
            this.symbol = name;
            this.desc = desc;
            this.rules = rules;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public List<DetectionRule> getRules() {
            return rules;
        }

        public void setRules(List<DetectionRule> rules) {
            this.rules = rules;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Symbol) {
                return symbol.equals(((Symbol) o).getSymbol());
            }
            if (o instanceof String) {
                return symbol.equals(o);
            }
            return false;
        }
    }

    public interface DetectionRule {
        boolean test(Element symbolContainer, int childIndex, String symbol, int iteration, UTDManager m);

        String getRuleName();

        boolean getAlways();

        default boolean compare(DetectionRule rule2) {
            return getRuleName().equals(rule2.getRuleName()) && getAlways() == rule2.getAlways();
        }
    }

    public interface AdvancedDetectionRule extends DetectionRule {
        void setOption(String option);

        String getOption();

        default boolean compare(DetectionRule rule2) {
            if (!(rule2 instanceof AdvancedDetectionRule))
                return false;
            return getRuleName().equals(rule2.getRuleName()) && getAlways() == rule2.getAlways() && getOption().equals(((AdvancedDetectionRule) rule2).getOption());
        }
    }

    public static class DirectTranslatedRule implements DetectionRule {
        final boolean not;
        final boolean always;

        public DirectTranslatedRule(boolean not, boolean always) {
            this.not = not;
            this.always = always;
        }

        @Override
        public boolean test(Element symbolContainer, int childIndex, String symbol, int iteration, UTDManager m) {
            return not != directTranslatedTest(symbolContainer, symbol);
        }

        @Override
        public String getRuleName() {
            return (not ? RULE_MODIFIER_NOT : "") + RULE_NAME_DIRECT_TRANSLATED;
        }

        @Override
        public boolean getAlways() {
            return always;
        }
    }

    public static class WholeWordRule implements DetectionRule {
        final boolean not;
        final boolean always;

        public WholeWordRule(boolean not, boolean always) {
            this.not = not;
            this.always = always;
        }

        @Override
        public boolean test(Element symbolContainer, int childIndex, String symbol, int iteration, UTDManager m) {
            return not != wholeWordTest(symbolContainer, childIndex, symbol, iteration, m);
        }

        @Override
        public String getRuleName() {
            return (not ? RULE_MODIFIER_NOT : "") + RULE_NAME_WHOLE_WORD;
        }

        @Override
        public boolean getAlways() {
            return always;
        }
    }

    public static class BeginningOfWordRule implements DetectionRule {
        final boolean not;
        final boolean always;

        public BeginningOfWordRule(boolean not, boolean always) {
            this.not = not;
            this.always = always;
        }

        @Override
        public boolean test(Element symbolContainer, int childIndex, String symbol, int iteration, UTDManager m) {
            return not ? !beginningOfWordTest(symbolContainer, childIndex, symbol, iteration, m) || wholeWordTest(symbolContainer, childIndex, symbol, iteration, m)
                    : beginningOfWordTest(symbolContainer, childIndex, symbol, iteration, m) && !wholeWordTest(symbolContainer, childIndex, symbol, iteration, m);
        }

        @Override
        public String getRuleName() {
            return (not ? RULE_MODIFIER_NOT : "") + RULE_NAME_BEGINNING_OF_WORD;
        }

        @Override
        public boolean getAlways() {
            return always;
        }
    }

    public static class EndOfWordRule implements DetectionRule {
        final boolean not;
        final boolean always;

        public EndOfWordRule(boolean not, boolean always) {
            this.not = not;
            this.always = always;
        }

        @Override
        public boolean test(Element symbolContainer, int childIndex, String symbol, int iteration, UTDManager m) {
            return not ? !endOfWordTest(symbolContainer, childIndex, symbol, iteration, m) || wholeWordTest(symbolContainer, childIndex, symbol, iteration, m)
                    : endOfWordTest(symbolContainer, childIndex, symbol, iteration, m) && !wholeWordTest(symbolContainer, childIndex, symbol, iteration, m);
        }

        @Override
        public String getRuleName() {
            return (not ? RULE_MODIFIER_NOT : "") + RULE_NAME_END_OF_WORD;
        }

        @Override
        public boolean getAlways() {
            return always;
        }
    }

    public static class PrecededByRule implements AdvancedDetectionRule {
        final boolean not;
        final boolean always;
        String option;

        public PrecededByRule(boolean not, boolean always) {
            this.not = not;
            this.always = always;
        }

        @Override
        public boolean test(Element symbolContainer, int childIndex, String symbol, int iteration, UTDManager m) {
            return not != precededByTest(symbolContainer, childIndex, symbol, option, iteration, m);
        }

        @Override
        public String getRuleName() {
            return (not ? RULE_MODIFIER_NOT : "") + RULE_NAME_PRECEDED_BY;
        }

        @Override
        public void setOption(String option) {
            this.option = option;
        }

        @Override
        public String getOption() {
            return option;
        }

        @Override
        public boolean getAlways() {
            return always;
        }
    }

    public static class FollowedByRule implements AdvancedDetectionRule {
        final boolean not;
        final boolean always;
        String option;

        public FollowedByRule(boolean not, boolean always) {
            this.not = not;
            this.always = always;
        }

        @Override
        public boolean test(Element symbolContainer, int childIndex, String symbol, int iteration, UTDManager m) {
            return not != followedByTest(symbolContainer, childIndex, symbol, option, iteration, m);
        }

        @Override
        public String getRuleName() {
            return (not ? RULE_MODIFIER_NOT : "") + RULE_NAME_FOLLOWED_BY;
        }

        @Override
        public void setOption(String option) {
            this.option = option;
        }

        @Override
        public String getOption() {
            return option;
        }

        @Override
        public boolean getAlways() {
            return always;
        }
    }

    public static DetectionRule getEquivalentRule(String string, boolean always) {
        return switch (string) {
            case RULE_NAME_WHOLE_WORD, "WHOLE_WORD" -> //symbols.xml uses old enum names
                    new WholeWordRule(false, always);
            case RULE_MODIFIER_NOT + RULE_NAME_WHOLE_WORD, "NOT_WHOLE_WORD" -> new WholeWordRule(true, always);
            case RULE_NAME_DIRECT_TRANSLATED, "DIRECT_TRANSLATED" -> new DirectTranslatedRule(false, always);
            case RULE_MODIFIER_NOT + RULE_NAME_DIRECT_TRANSLATED, "NOT_DIRECT_TRANSLATED" ->
                    new DirectTranslatedRule(true, always);
            case RULE_NAME_BEGINNING_OF_WORD, "BEGINNING_OF_WORD" -> new BeginningOfWordRule(false, always);
            case RULE_MODIFIER_NOT + RULE_NAME_BEGINNING_OF_WORD, "NOT_BEGINNING_OF_WORD" ->
                    new BeginningOfWordRule(true, always);
            case RULE_NAME_END_OF_WORD, "END_OF_WORD" -> new EndOfWordRule(false, always);
            case RULE_MODIFIER_NOT + RULE_NAME_END_OF_WORD, "NOT_END_OF_WORD" -> new EndOfWordRule(true, always);
            case RULE_NAME_PRECEDED_BY, "PRECEDED_BY" -> new PrecededByRule(false, always);
            case RULE_MODIFIER_NOT + RULE_NAME_PRECEDED_BY, "NOT_PRECEDED_BY" -> new PrecededByRule(true, always);
            case RULE_NAME_FOLLOWED_BY, "FOLLOWED_BY" -> new FollowedByRule(false, always);
            case RULE_MODIFIER_NOT + RULE_NAME_FOLLOWED_BY, "NOT_FOLLOWED_BY" -> new FollowedByRule(true, always);
            default -> throw new IllegalArgumentException("Rule not found: " + string);
        };
    }

    private static boolean wholeWordTest(Element e, int childIndex, String s, int iteration, UTDManager m) {
        return beginningOfWordTest(e, childIndex, s, iteration, m) && endOfWordTest(e, childIndex, s, iteration, m);
    }

    private static boolean directTranslatedTest(Element e, String s) {
        return "DIRECT".equals(((Element) e.getParent()).getAttributeValue("table"));
    }

    private static boolean beginningOfWordTest(Element e, int childIndex, String s, int iteration, UTDManager m) {
        Element block = (Element) m.getEngine().findTranslationBlock(e.getParent());
        boolean isBlock = block == e.getParent();
        Node textNode = e.getChild(childIndex);
        //Case 1: text node begins with the symbol and is preceded by a moveTo
        if (iteration == 0 && textNode.getValue().startsWith(s)) {
            if (childIndex > 0 && UTDElements.MOVE_TO.isA(e.getChild(childIndex - 1))) {
                return true;
            }
            //Case 2: brl element is not a block and the previous brl element ended with a space
            if (!isBlock) {
                List<Element> allBrl = getDescendantBrl(block);
                int thisBrlIndex = allBrl.indexOf(e);
                if (thisBrlIndex == 0) {
                    return false;
                }
                Element prevBrl = allBrl.get(thisBrlIndex - 1);
                return prevBrl.getValue().endsWith(" ");
            }
            return false;
        }
        int symbolLocation = findSymbolIndex(textNode, s, iteration);
        //Case 3: symbol is preceded by a space
        String textNodeValue = textNode.getValue();
        int charIndex = textNodeValue.indexOf(s, symbolLocation);
        return charIndex > 0 && textNodeValue.charAt(charIndex - 1) == ' ';
    }

    private static boolean endOfWordTest(Element e, int childIndex, String s, int iteration, UTDManager m) {
        Element block = (Element) m.getEngine().findTranslationBlock(e.getParent());
        boolean isBlock = block == e.getParent();
        Node textNode = e.getChild(childIndex);
        boolean finalIteration = findMaxIterations(textNode, s) == iteration + 1;
        //Case 1: text node ends with the symbol and is followed by a moveTo or brl node is a block
        if (finalIteration && textNode.getValue().endsWith(s)) {
            if ((isBlock && childIndex == e.getChildCount() - 1) || (childIndex + 1 < e.getChildCount() && UTDElements.MOVE_TO.isA(e.getChild(childIndex + 1)))) {
                return true;
            }
            //Case 2: brl element is not a block and the next brl element begins with a space or move to
            if (!isBlock) {
                List<Element> allBrl = getDescendantBrl(block);
                int thisBrlIndex = allBrl.indexOf(e);
                if (thisBrlIndex == allBrl.size() - 1) {
                    return false;
                }
                Element nextBrl = allBrl.get(thisBrlIndex + 1);
                if (nextBrl.getValue().startsWith(" ")) {
                    return true;
                }
                return UTDElements.MOVE_TO.isA(nextBrl.getChild(0)) || UTDElements.NEW_PAGE.isA(nextBrl.getChild(0));
            }
            return false;
        }
        //Case 3: symbol is followed by a space
        String textNodeValue = textNode.getValue();
        int symbolLocation = findSymbolIndex(textNode, s, iteration);
        int charIndex = textNodeValue.indexOf(s, symbolLocation);
        return charIndex + s.length() + 1 < textNodeValue.length() && textNodeValue.substring(charIndex + s.length(), charIndex + s.length() + 1).equals(" ");
    }

    private static boolean precededByTest(Element e, int childIndex, String s, String option, int iteration, UTDManager m) {
        Element block = (Element) m.getEngine().findTranslationBlock(e.getParent());
        boolean isBlock = block == e.getParent();
        Node textNode = e.getChild(childIndex);

        //Case 1: Symbol is beginning of text node, brl is not block element, look at previous brl
        if (iteration == 0 && textNode.getValue().startsWith(s)) {
            if (childIndex > 0 && (UTDElements.MOVE_TO.isA(e.getChild(childIndex - 1)) || UTDElements.NEW_PAGE.isA(e.getChild(childIndex - 1)))) {
                return false;
            }
            if (!isBlock) {
                List<Element> allBrl = getDescendantBrl(block);
                int thisBrl = allBrl.indexOf(e);
                if (thisBrl == 0) {
                    return false;
                }
                Element prevBrl = allBrl.get(thisBrl - 1);
                if (prevBrl.getValue().endsWith(option)) {
                    return true;
                }
            }
        }
        String textNodeValue = textNode.getValue();
        int symbolLocation = findSymbolIndex(textNode, s, iteration);
        //Case 2: Symbol is immediately preceded by the option
        return textNodeValue.substring(0, textNodeValue.indexOf(s, symbolLocation)).endsWith(option);
    }

    private static boolean followedByTest(Element e, int childIndex, String s, String option, int iteration, UTDManager m) {
        int symbolLocation = 0;
        Element block = (Element) m.getEngine().findTranslationBlock(e.getParent());
        boolean isBlock = block == e.getParent();
        Node textNode = e.getChild(childIndex);
        boolean finalIteration = findMaxIterations(textNode, s) == iteration + 1;

        //Case 1: Symbol is end of text node, brl is not block element, look at next brl
        if (finalIteration && textNode.getValue().endsWith(s)) {
            if (childIndex + 1 < e.getChildCount() && (UTDElements.MOVE_TO.isA(e.getChild(childIndex + 1)) || UTDElements.NEW_PAGE.isA(e.getChild(childIndex + 1)))) {
                return false;
            }
            if (!isBlock) {
                List<Element> allBrl = getDescendantBrl(block);
                int thisBrl = allBrl.indexOf(e);
                if (thisBrl == allBrl.size() - 1) {
                    return false;
                }
                Element nextBrl = allBrl.get(thisBrl + 1);
                if (nextBrl.getValue().startsWith(option)) {
                    return true;
                }
            }
        }
        String textNodeValue = textNode.getValue();
        //Case 2: Symbol is immediately followed by the option
        int i = textNodeValue.indexOf(s, symbolLocation) + s.length();
        return i < textNodeValue.length() && textNodeValue.substring(i).startsWith(option);
    }


    //Helper method that can find copies of the same symbol in a single text node multiple times
    private static int findSymbolIndex(Node textNode, String symbol, int iteration) {
        int symbolLocation = 0;
        int startIter = 0;
        String text = textNode.getValue();
        while (symbolLocation < text.length() && text.indexOf(symbol, symbolLocation) != -1) {
            if (startIter == iteration)
                return symbolLocation;
            startIter++;
            symbolLocation = text.indexOf(symbol, symbolLocation) + 1;
        }
        throw new IllegalArgumentException("The " + iteration + " copy of symbol " + symbol + " was not found in text node " + textNode.getValue());
    }

    private static int findMaxIterations(Node textNode, String symbol) {
        int symbolLocation = 0;
        int totalIterations = 0;
        String text = textNode.getValue();
        while (symbolLocation < text.length() && text.indexOf(symbol, symbolLocation) != -1) {
            totalIterations++;
            symbolLocation = text.indexOf(symbol, symbolLocation) + 1;
        }
        return totalIterations;
    }

    private static List<Element> getDescendantBrl(Element parent) {
        List<Element> returnList = new ArrayList<>();
        Nodes query = parent.query("descendant::*[local-name()='brl']");
        for (int i = 0; i < query.size(); i++) {
            returnList.add((Element) query.get(i));
        }
        return returnList;
    }


    /**
     * Retrieve the default map and any changes inside user's programData symbols
     * file and combine them into one hashmap
     */
    public static List<Symbol> getSymbols() {
        List<Symbol> defaultList = readSymbolsFromFile(DEFAULT_SYMBOL_PATH);
        List<Symbol> customList = readSymbolsFromFile(CUSTOM_SYMBOL_PATH);
        log.debug("Found {} items in custom list and {} items in the default list", customList.size(), defaultList.size());

        //Fill our map with the custom map first
        List<Symbol> returnList = new ArrayList<>(customList);

        //Now fill it with the default map, but don't overwrite anything in the custom map
        for (Symbol symbol : defaultList) {
            if (!customList.contains(symbol))
                returnList.add(symbol);
        }
        return returnList;
    }

    /**
     * Load symbols from a file and store it in a map
     */
    private static List<Symbol> readSymbolsFromFile(String path) {
        log.debug("Loading symbol map from path {} ", path);
        List<Symbol> returnList = new ArrayList<>();
        if (new File(path).exists()) {
            Document newDoc = readDocFromPath(path);
            if (newDoc == null)
                return returnList;
            List<Element> entries = XMLHandler.queryElements(newDoc, "descendant::entry");
            for (Element entry : entries) {
                if (entry.getChildElements().size() > 0) {
                    String name = entry.getChildElements().get(0).getValue();
                    String desc = null;
                    if (entry.getChildElements().size() > 1) {
                        desc = entry.getChildElements().get(1).getValue();
                    }
                    List<DetectionRule> rules = new ArrayList<>();
                    if (entry.getChildElements().size() > 2) {
                        for (int child = 2; child < entry.getChildElements().size(); child++) {
                            try {
                                String option = entry.getChildElements().get(child).getAttributeValue("option");
                                boolean always = "true".equals(entry.getChildElements().get(child).getAttributeValue("always"));
                                DetectionRule rule = getEquivalentRule(entry.getChildElements().get(child).getValue(), always);
                                if (option != null) {
                                    ((AdvancedDetectionRule) rule).setOption(option);
                                }
                                rules.add(rule);
                            } catch (IllegalArgumentException e) {
                                Notify.showMessage("An error occurred while parsing rules for symbol: " + name + ": " + desc + ". The rule was not loaded");
                                log.debug(e.toString());
                            }
                        }
                    }
                    returnList.add(new Symbol(name, desc, rules));
                }
            }
        } else {
            log.debug("Symbol map not found at {}", path);
        }

        return returnList;
    }

    /**
     * Compare given map to default symbols map and save changes to user's
     * programData folder
     */
    public static void saveChanges(List<Symbol> list) {
        log.debug("Saving list: {}", list.size());
        List<Symbol> changes = new ArrayList<>();
        List<Symbol> defaultList = readSymbolsFromFile(DEFAULT_SYMBOL_PATH);

        for (Symbol symbol : defaultList) {
            if (!list.contains(symbol)) {
                symbol.setDesc(null);
                symbol.setRules(null);
            }
        }

        //Look for symbols added to custom map or changed from default
        for (Symbol symbol : list) {
            boolean changed;
            if (defaultList.contains(symbol)) {
                Symbol defaultSymbol = defaultList.get(defaultList.indexOf(symbol));
                Symbol customSymbol = list.get(list.indexOf(symbol));
                changed = !compareSymbol(defaultSymbol, customSymbol);
            } else {
                log.debug("Default list does not contain {}", symbol.getSymbol());
                //Key is not in default map
                changed = true;
            }

            if (changed)
                changes.add(symbol);
        }

        saveListToXML(changes, CUSTOM_SYMBOL_PATH);
    }

    public static String[] getPrefixDefault() {
        if (new File(CUSTOM_SYMBOL_PATH).exists()) {
            String[] prefix = loadPrefix(CUSTOM_SYMBOL_PATH);
            if (prefix != null) {
                log.debug("Found custom symbol prefix: {}: {}", prefix[0], prefix[1]);
                return prefix;
            }
        }
        if (new File(DEFAULT_SYMBOL_PATH).exists()) {
            String[] prefix = loadPrefix(DEFAULT_SYMBOL_PATH);
            if (prefix != null) {
                log.debug("Found default symbol prefix: {}: {}", prefix[0], prefix[1]);
                return prefix;
            }
        }
        return null;
    }

    private static String[] loadPrefix(String path) {
        Document doc = readDocFromPath(path);
        if (doc != null) {
            Nodes query = doc.query("descendant::prefix");
            if (query.size() > 0) {
                Element prefix = (Element) query.get(0);
                if (prefix.getChildElements().size() > 0) {
                    String symbol = prefix.getChildElements().get(0).getValue();
                    String desc = "";
                    if (prefix.getChildElements().size() > 1) {
                        desc = prefix.getChildElements().get(1).getValue();
                    }
                    return new String[]{symbol, desc};
                }
            }
        }
        return null;
    }

    public static void setPrefixDefault(String symbol, String desc) {
        if (!new File(CUSTOM_SYMBOL_PATH).exists()) {
            saveListToXML(new ArrayList<>(), CUSTOM_SYMBOL_PATH);
        }
        Document custom = readDocFromPath(CUSTOM_SYMBOL_PATH);
        if (custom == null) {
            return;
        }
        Nodes existingPrefix = custom.query("descendant::prefix");
        if (existingPrefix.size() > 0)
            existingPrefix.get(0).detach();
        String[] defaultPrefix = loadPrefix(DEFAULT_SYMBOL_PATH);
        if (defaultPrefix != null && !(symbol.equals(defaultPrefix[0]) && desc.equals(defaultPrefix[1]))) {
            Element root = custom.getRootElement();
            Element newPrefix = new Element("prefix");
            Element newSymbol = new Element("symbol");
            newSymbol.appendChild(symbol);
            Element newDesc = new Element("desc");
            newDesc.appendChild(desc);
            newPrefix.appendChild(newSymbol);
            newPrefix.appendChild(newDesc);
            root.appendChild(newPrefix);
        }
        new XMLHandler().save(custom, new File(CUSTOM_SYMBOL_PATH));
    }

    static final String[][] sharedSymbols = new String[][]{{"~", "^"}, {"`", "@"}, {"{", "["}, {"}", "]"}, {"|", "\\"}};

    /**
     * Helper method that returns every permutation of a symbol that has ascii character(s) that share the same braille representation
     * as other ascii characters (ex. <code>~</code> and <code>^</code> both represent a braille cell with dots 4 and 5)
     */
    public static List<String> getSymbolPermutations(String symbol) {
        return symbolPermutationHelper(symbol, 0);
    }

    private static List<String> symbolPermutationHelper(String symbol, int startIndex) {
        List<String> returnList = new ArrayList<>();
        for (int curCharNum = startIndex; curCharNum < symbol.length(); curCharNum++) {
            String curChar = String.valueOf(symbol.charAt(curCharNum));
            for (String[] sharedSymbol : sharedSymbols) {
                if (curChar.equals(sharedSymbol[0])) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(symbol, 0, curCharNum);
                    sb.append(sharedSymbol[1]);
                    if (curCharNum + 1 < symbol.length())
                        sb.append(symbol.substring(curCharNum + 1));
                    returnList.add(sb.toString());
                    returnList.addAll(symbolPermutationHelper(sb.toString(), curCharNum + 1));
                } else if (curChar.equals(sharedSymbol[1])) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(symbol, 0, curCharNum);
                    sb.append(sharedSymbol[0]);
                    if (curCharNum + 1 < symbol.length())
                        sb.append(symbol.substring(curCharNum + 1));
                    returnList.add(sb.toString());
                    returnList.addAll(symbolPermutationHelper(sb.toString(), curCharNum + 1));
                }
            }
        }
        return returnList;
    }

    private static void saveListToXML(List<Symbol> symbols, String path) {
        log.debug("Saving map to path {}", path);
        Element root = new Element("symbols");
        Document doc = new Document(root);
        for (Symbol symbol : symbols) {
            String symbolName = symbol.getSymbol();
            String desc = symbol.getDesc();
            List<DetectionRule> rules = symbol.getRules();
            Element newEntry = new Element("entry");
            Element newSymbol = new Element("symbol");
            newEntry.appendChild(newSymbol);
            newSymbol.appendChild(symbolName);
            if (desc != null && !desc.isEmpty()) {
                Element newDesc = new Element("desc");
                newDesc.appendChild(desc);
                newEntry.appendChild(newDesc);
            }
            if (rules != null && !rules.isEmpty()) {
                for (DetectionRule rule : rules) {
                    Element newRule = new Element("rule");
                    newRule.appendChild(rule.getRuleName());
                    if (rule.getAlways()) {
                        newRule.addAttribute(new Attribute("always", "true"));
                    }
                    if (rule instanceof AdvancedDetectionRule) {
                        newRule.addAttribute(new Attribute("option", ((AdvancedDetectionRule) rule).getOption()));
                    }
                    newEntry.appendChild(newRule);
                }
            }
            root.appendChild(newEntry);
        }
        new XMLHandler().save(doc, new File(path));
    }

    private static @Nullable Document readDocFromPath(String path) {
        Builder builder = new Builder();
        File file = new File(path);
        try {
            return builder.build(file);
        } catch (IOException | ParsingException e) {
            log.warn("Problem parsing XML", e);
            return null;
        }
    }

    /**
     * Delete symbols file inside user's programData
     */
    public static void restoreDefault() {
        new File(CUSTOM_SYMBOL_PATH).delete();
    }

    public static String singleRuleToString(DetectionRule rule) {
        if (rule instanceof AdvancedDetectionRule)
            return rule.getRuleName() + " (" + ((AdvancedDetectionRule) rule).getOption() + ")" + (rule.getAlways() ? RULE_MODIFIER_ALWAYS : "");
        else
            return rule.getRuleName() + (rule.getAlways() ? RULE_MODIFIER_ALWAYS : "");
    }

    public static String rulesToString(List<DetectionRule> rules) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rules.size(); i++) {
            sb.append(singleRuleToString(rules.get(i)));
            if (i != rules.size() - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    public static List<DetectionRule> stringToRules(String input) {
        if (input.isEmpty())
            return new ArrayList<>();
        String[] split = splitSafely(input);
        List<DetectionRule> rules = new ArrayList<>();
        for (String rule : split) {
            rule = rule.trim();
            boolean always = rule.contains("[Always]");
            String actualRule = rule.replace(" [Always]", "");
            //Find options (e.g. "Followed By (_)")
            Pattern pattern = Pattern.compile(" \\(.+\\)");
            Matcher matcher = pattern.matcher(actualRule);
            String option = matcher.find() ? actualRule.substring(matcher.start(), matcher.end()) : null;
            if (option != null) {
                actualRule = actualRule.replace(option, "");
            }
            DetectionRule newRule = getEquivalentRule(actualRule, always);
            if (option != null)
                ((AdvancedDetectionRule) newRule).setOption(option.replace(" (", "").replace(")", ""));
            rules.add(newRule);
        }
        return rules;
    }

    private static String[] splitSafely(String string) {
        Pattern p = Pattern.compile("\\(.?,.?\\)");
        Matcher m = p.matcher(string);
        ArrayList<String> array = new ArrayList<>();
        if (m.find()) {
            // RT 4620 advanced rule with a comma
            char[] charArray = string.toCharArray();
            StringBuilder s = new StringBuilder();
            boolean inParenthesis = false;
            for (char c : charArray) {
                if (c == ',' && !inParenthesis) {
                    array.add(s.toString());
                    s = new StringBuilder();
                } else {
                    if (c == '(') {
                        inParenthesis = true;
                    } else if (c == ')') {
                        inParenthesis = false;
                    }
                    s.append(c);
                }
            }
            if (!s.isEmpty()) {
                array.add(s.toString());
            }
            String[] sArray = new String[array.size()];
            return array.toArray(sArray);
        } else {
            return string.split(",");
        }
    }

    /**
     * Organizes symbols by UEB cell complexity
     */
    public static class SymbolComparator implements Comparator<Symbol> {
        final String complexityOrder = "abcdefghijklmnopqrstuvxyz&=(!)*<%?:$]\\[w1234567890/+#>'-@^_\".;,";

        @Override
        public int compare(Symbol sym1, Symbol sym2) {
            String o1 = sym1.getSymbol();
            String o2 = sym2.getSymbol();
            char prevChar = o1.toLowerCase().charAt(0);
            char thisChar = o2.toLowerCase().charAt(0);
            if (o1.length() > 1 && o2.length() > 1) {
                int iterator = 0;
                while (prevChar == thisChar) {
                    iterator++;
                    if (iterator >= o1.length()) {
                        prevChar = '|';
                        break;
                    } else if (iterator >= o2.length()) {
                        thisChar = '|';
                        break;
                    }
                    prevChar = o1.toLowerCase().charAt(iterator);
                    thisChar = o2.toLowerCase().charAt(iterator);
                }
            }
            return Integer.compare(complexityOrder.indexOf(prevChar), complexityOrder.indexOf(thisChar));
        }

    }

    private static boolean compareSymbol(Symbol symbol1, Symbol symbol2) {
        if (symbol1.getSymbol() != null && symbol2.getSymbol() != null) {
            if (!symbol1.getSymbol().equals(symbol2.getSymbol())) {
                return false;
            }
        } else if ((symbol1.getSymbol() == null && symbol2.getSymbol() != null)
                || (symbol1.getSymbol() != null && symbol2.getSymbol() == null)) {
            return false;
        }
        if (symbol1.getDesc() != null && symbol2.getDesc() != null) {
            if (!symbol1.getDesc().equals(symbol2.getDesc())) {
                return false;
            }
        } else if ((symbol1.getDesc() == null && symbol2.getDesc() != null)
                || (symbol1.getDesc() != null && symbol2.getDesc() == null)) {
            return false;
        }

        List<DetectionRule> list1 = symbol1.getRules();
        List<DetectionRule> list2 = symbol2.getRules();
        if (list1 == null || list2 == null) {
            return list1 == list2;
        } else if (list1.size() != list2.size()) {
            return false;
        }
        for (int i = 0; i < list1.size(); i++) {
            DetectionRule rule1 = list1.get(i);
            DetectionRule rule2 = list2.get(i);
            if (!rule1.compare(rule2))
                return false;
        }
        return true;
    }

}
