package org.example.josmregexreplace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.spi.preferences.Config;

public final class RegexReplacePreferences {

    private static final String PREFIX = "regexreplace.";
    private static final String DEFAULT_KEY = "";
    private static final String DEFAULT_FIND = "";
    private static final String HISTORY_KEY = PREFIX + "history";
    private static final String PRESETS_KEY = PREFIX + "presets";
    private static final int MAX_HISTORY = 10;
    // NB: never use List.of() as default/value for getListOfMaps/putListOfMaps.
    // JOSM's MapListSetting.consistencyTest() calls value.contains(null), and
    // ImmutableCollections (List.of(), Stream.toList()) throw NPE on contains(null).
    // Collections.emptyList()/new ArrayList<>() return false instead. See JOSM #23727.
    private static final List<Map<String, String>> EMPTY_MAP_LIST = Collections.emptyList();

    private RegexReplacePreferences() {
    }

    public static State load() {
        String modeValue = Config.getPref().get(PREFIX + "mode", RegexReplacementMode.REGEX.name());
        RegexReplacementMode mode;
        try {
            mode = RegexReplacementMode.valueOf(modeValue);
        } catch (IllegalArgumentException ex) {
            mode = RegexReplacementMode.REGEX;
        }
        return new State(
            Config.getPref().get(PREFIX + "key", DEFAULT_KEY),
            Config.getPref().get(PREFIX + "find", DEFAULT_FIND),
            Config.getPref().get(PREFIX + "replacement", ""),
            mode,
            Config.getPref().getBoolean(PREFIX + "selectionOnly", true),
            Config.getPref().getBoolean(PREFIX + "ignoreCase", false),
            Config.getPref().getBoolean(PREFIX + "multiline", false),
            Config.getPref().getBoolean(PREFIX + "dotAll", false),
            Config.getPref().getBoolean(PREFIX + "unicode", false),
            Config.getPref().getBoolean(PREFIX + "nodes", true),
            Config.getPref().getBoolean(PREFIX + "ways", true),
            Config.getPref().getBoolean(PREFIX + "relations", true),
            Config.getPref().getBoolean(PREFIX + "keepEmptyValues", false),
            Config.getPref().get(PREFIX + "search", ""),
            parseKeyOperation(Config.getPref().get(PREFIX + "keyOperation", "REPLACE")),
            Config.getPref().getBoolean(PREFIX + "allTags", false),
            Config.getPref().getBoolean(PREFIX + "createMissing", false),
            Config.getPref().get(PREFIX + "destinationKey", ""),
            Config.getPref().get(PREFIX + "conditionKey", ""),
            Config.getPref().get(PREFIX + "conditionPattern", ""),
            Config.getPref().getInt(PREFIX + "minimumMatches", 0),
            Config.getPref().getInt(PREFIX + "maximumMatches", Integer.MAX_VALUE)
            ,Config.getPref().getBoolean(PREFIX + "trimWhitespace", false)
            ,Config.getPref().getBoolean(PREFIX + "normalizeUnicode", false)
            ,Config.getPref().getBoolean(PREFIX + "preventEmptyResult", false)
        );
    }

    public static void save(State state) {
        Config.getPref().put(PREFIX + "key", state.key());
        Config.getPref().put(PREFIX + "find", state.findRegex());
        Config.getPref().put(PREFIX + "replacement", state.replacement());
        Config.getPref().put(PREFIX + "mode", state.mode().name());
        Config.getPref().putBoolean(PREFIX + "selectionOnly", state.selectionOnly());
        Config.getPref().putBoolean(PREFIX + "ignoreCase", state.ignoreCase());
        Config.getPref().putBoolean(PREFIX + "multiline", state.multiline());
        Config.getPref().putBoolean(PREFIX + "dotAll", state.dotAll());
        Config.getPref().putBoolean(PREFIX + "unicode", state.unicode());
        Config.getPref().putBoolean(PREFIX + "nodes", state.nodes());
        Config.getPref().putBoolean(PREFIX + "ways", state.ways());
        Config.getPref().putBoolean(PREFIX + "relations", state.relations());
        Config.getPref().putBoolean(PREFIX + "keepEmptyValues", state.keepEmptyValues());
        Config.getPref().put(PREFIX + "search", state.searchExpression());
        Config.getPref().put(PREFIX + "keyOperation", state.keyOperation().name());
        Config.getPref().putBoolean(PREFIX + "allTags", state.allTags());
        Config.getPref().putBoolean(PREFIX + "createMissing", state.createMissing());
        Config.getPref().put(PREFIX + "destinationKey", state.destinationKey());
        Config.getPref().put(PREFIX + "conditionKey", state.conditionKey());
        Config.getPref().put(PREFIX + "conditionPattern", state.conditionPattern());
        Config.getPref().putInt(PREFIX + "minimumMatches", state.minimumMatches());
        Config.getPref().putInt(PREFIX + "maximumMatches", state.maximumMatches());
        Config.getPref().putBoolean(PREFIX + "trimWhitespace", state.trimWhitespace());
        Config.getPref().putBoolean(PREFIX + "normalizeUnicode", state.normalizeUnicode());
        Config.getPref().putBoolean(PREFIX + "preventEmptyResult", state.preventEmptyResult());
    }

    public static List<State> loadRecent() {
        List<State> states = new ArrayList<>();
        for (Map<String, String> values : Config.getPref().getListOfMaps(HISTORY_KEY, EMPTY_MAP_LIST)) {
            states.add(fromMap(values));
        }
        return states;
    }

    public static void saveRecent(State state) {
        List<State> states = loadRecent();
        states.remove(state);
        states.add(0, state);
        if (states.size() > MAX_HISTORY) {
            states = new ArrayList<>(states.subList(0, MAX_HISTORY));
        }
        // Wrap in ArrayList: Stream.toList() is immutable and triggers the same NPE in MapListSetting.
        List<Map<String, String>> values = new ArrayList<>(
            states.stream().map(RegexReplacePreferences::toMap).toList());
        Config.getPref().putListOfMaps(HISTORY_KEY, values);
    }

    public static void clearRecent() {
        // Mutable empty list: List.of() would trigger the MapListSetting NPE (see above).
        Config.getPref().putListOfMaps(HISTORY_KEY, new ArrayList<>());
    }

    public static List<String> loadPresetNames() {
        return Config.getPref().getListOfMaps(PRESETS_KEY, EMPTY_MAP_LIST).stream()
            .map(values -> values.getOrDefault("name", ""))
            .filter(name -> !name.isBlank())
            .toList();
    }

    public static State loadPreset(String name) {
        return Config.getPref().getListOfMaps(PRESETS_KEY, EMPTY_MAP_LIST).stream()
            .filter(values -> name.equals(values.get("name")))
            .findFirst()
            .map(RegexReplacePreferences::fromMap)
            .orElse(null);
    }

    public static void savePreset(String name, State state) {
        List<Map<String, String>> presets = new ArrayList<>(
            Config.getPref().getListOfMaps(PRESETS_KEY, EMPTY_MAP_LIST));
        presets.removeIf(values -> name.equals(values.get("name")));
        Map<String, String> values = toMap(state);
        values.put("name", name);
        presets.add(values);
        Config.getPref().putListOfMaps(PRESETS_KEY, presets);
    }

    public static void deletePreset(String name) {
        List<Map<String, String>> presets = new ArrayList<>(
            Config.getPref().getListOfMaps(PRESETS_KEY, EMPTY_MAP_LIST));
        presets.removeIf(values -> name.equals(values.get("name")));
        Config.getPref().putListOfMaps(PRESETS_KEY, presets);
    }

    private static Map<String, String> toMap(State state) {
        Map<String, String> values = new HashMap<>();
        values.put("key", state.key());
        values.put("find", state.findRegex());
        values.put("replacement", state.replacement());
        values.put("mode", state.mode().name());
        values.put("selectionOnly", Boolean.toString(state.selectionOnly()));
        values.put("ignoreCase", Boolean.toString(state.ignoreCase()));
        values.put("multiline", Boolean.toString(state.multiline()));
        values.put("dotAll", Boolean.toString(state.dotAll()));
        values.put("unicode", Boolean.toString(state.unicode()));
        values.put("nodes", Boolean.toString(state.nodes()));
        values.put("ways", Boolean.toString(state.ways()));
        values.put("relations", Boolean.toString(state.relations()));
        values.put("keepEmptyValues", Boolean.toString(state.keepEmptyValues()));
        values.put("search", state.searchExpression());
        values.put("keyOperation", state.keyOperation().name());
        values.put("allTags", Boolean.toString(state.allTags()));
        values.put("createMissing", Boolean.toString(state.createMissing()));
        values.put("destinationKey", state.destinationKey());
        values.put("conditionKey", state.conditionKey());
        values.put("conditionPattern", state.conditionPattern());
        values.put("minimumMatches", Integer.toString(state.minimumMatches()));
        values.put("maximumMatches", Integer.toString(state.maximumMatches()));
        values.put("trimWhitespace", Boolean.toString(state.trimWhitespace()));
        values.put("normalizeUnicode", Boolean.toString(state.normalizeUnicode()));
        values.put("preventEmptyResult", Boolean.toString(state.preventEmptyResult()));
        return values;
    }

    private static State fromMap(Map<String, String> values) {
        return new State(
            values.getOrDefault("key", DEFAULT_KEY),
            values.getOrDefault("find", DEFAULT_FIND),
            values.getOrDefault("replacement", ""),
            parseMode(values.getOrDefault("mode", RegexReplacementMode.REGEX.name())),
            Boolean.parseBoolean(values.getOrDefault("selectionOnly", "true")),
            Boolean.parseBoolean(values.getOrDefault("ignoreCase", "false")),
            Boolean.parseBoolean(values.getOrDefault("multiline", "false")),
            Boolean.parseBoolean(values.getOrDefault("dotAll", "false")),
            Boolean.parseBoolean(values.getOrDefault("unicode", "false")),
            Boolean.parseBoolean(values.getOrDefault("nodes", "true")),
            Boolean.parseBoolean(values.getOrDefault("ways", "true")),
            Boolean.parseBoolean(values.getOrDefault("relations", "true")),
            Boolean.parseBoolean(values.getOrDefault("keepEmptyValues", "false")),
            values.getOrDefault("search", ""),
            parseKeyOperation(values.getOrDefault("keyOperation", "REPLACE")),
            Boolean.parseBoolean(values.getOrDefault("allTags", "false")),
            Boolean.parseBoolean(values.getOrDefault("createMissing", "false")),
            values.getOrDefault("destinationKey", ""),
            values.getOrDefault("conditionKey", ""),
            values.getOrDefault("conditionPattern", ""),
            parseInt(values.getOrDefault("minimumMatches", "0"), 0),
            parseInt(values.getOrDefault("maximumMatches", Integer.toString(Integer.MAX_VALUE)), Integer.MAX_VALUE)
            ,Boolean.parseBoolean(values.getOrDefault("trimWhitespace", "false"))
            ,Boolean.parseBoolean(values.getOrDefault("normalizeUnicode", "false"))
            ,Boolean.parseBoolean(values.getOrDefault("preventEmptyResult", "false"))
        );
    }

    private static RegexReplacementMode parseMode(String value) {
        try {
            return RegexReplacementMode.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return RegexReplacementMode.REGEX;
        }
    }

    private static RegexReplacementKeyOperation parseKeyOperation(String value) {
        try {
            return RegexReplacementKeyOperation.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return RegexReplacementKeyOperation.REPLACE;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public record State(
        String key,
        String findRegex,
        String replacement,
        RegexReplacementMode mode,
        boolean selectionOnly,
        boolean ignoreCase,
        boolean multiline,
        boolean dotAll,
        boolean unicode,
        boolean nodes,
        boolean ways,
        boolean relations,
        boolean keepEmptyValues,
        String searchExpression,
        RegexReplacementKeyOperation keyOperation,
        boolean allTags,
        boolean createMissing,
        String destinationKey,
        String conditionKey,
        String conditionPattern,
        int minimumMatches,
        int maximumMatches,
        boolean trimWhitespace,
        boolean normalizeUnicode,
        boolean preventEmptyResult
    ) {
    }
}