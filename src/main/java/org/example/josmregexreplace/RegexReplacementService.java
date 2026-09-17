package org.example.josmregexreplace;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.Normalizer;

public final class RegexReplacementService {

    private RegexReplacementService() {
    }

    public static void validate(RegexReplacementRequest request) {
        Pattern pattern = compile(request);
        applyReplacement(pattern, "", request.replacement());
    }

    public static RegexReplacementResult replace(String value, RegexReplacementRequest request) {
        Pattern pattern = compile(request);
        String input = value;
        if (request.trimWhitespace()) {
            input = input.trim();
        }
        if (request.normalizeUnicode()) {
            input = Normalizer.normalize(input, Normalizer.Form.NFC);
        }
        Matcher matcher = pattern.matcher(input);
        int matchCount = countMatches(matcher);
        String newValue = matchCount == 0 ? value : applyReplacement(pattern, input, request.replacement());
        if (request.preventEmptyResult() && newValue.isEmpty()) {
            newValue = value;
        }
        return new RegexReplacementResult(value, newValue, matchCount);
    }

    private static Pattern compile(RegexReplacementRequest request) {
        String expression = switch (request.mode()) {
            case REGEX -> request.findRegex();
            case LITERAL -> Pattern.quote(request.findRegex());
            case WHOLE_VALUE -> "^(?:" + request.findRegex() + ")$";
        };
        return Pattern.compile(expression, request.patternFlags());
    }

    private static String applyReplacement(Pattern pattern, String value, String replacement) {
        try {
            return pattern.matcher(value).replaceAll(replacement);
        } catch (IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("Invalid replacement group: " + replacement, ex);
        }
    }

    private static int countMatches(Matcher matcher) {
        int matchCount = 0;
        while (matcher.find()) {
            matchCount++;
        }
        return matchCount;
    }
}