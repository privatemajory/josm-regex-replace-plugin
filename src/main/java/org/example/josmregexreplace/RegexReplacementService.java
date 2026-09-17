/*
 * Copyright (C) 2026 Dolly Andriatsiferana
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
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