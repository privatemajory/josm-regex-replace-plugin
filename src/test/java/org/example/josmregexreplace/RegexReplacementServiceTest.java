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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class RegexReplacementServiceTest {

    @Test
    void replacesAllMatchesAndCountsThem() {
        RegexReplacementResult result = RegexReplacementService.replace(
            "District de Paris",
            new RegexReplacementRequest("^District d(e )", "", 0)
        );

        assertEquals("Paris", result.newValue());
        assertEquals(1, result.matchCount());
        assertTrue(result.matched());
        assertTrue(result.changed());
    }

    @Test
    void supportsCaptureGroups() {
        RegexReplacementResult result = RegexReplacementService.replace(
            "Rue de Paris",
            new RegexReplacementRequest("Rue (de) (.+)", "$2, $1 Rue", 0)
        );

        assertEquals("Paris, de Rue", result.newValue());
        assertEquals(1, result.matchCount());
    }

    @Test
    void leavesUnmatchedValuesUntouched() {
        RegexReplacementResult result = RegexReplacementService.replace(
            "Paris",
            new RegexReplacementRequest("London", "", 0)
        );

        assertEquals("Paris", result.newValue());
        assertEquals(0, result.matchCount());
        assertFalse(result.matched());
        assertFalse(result.changed());
    }

    @Test
    void honorsPatternFlags() {
        RegexReplacementResult result = RegexReplacementService.replace(
            "PARIS",
            new RegexReplacementRequest("paris", "Lyon", Pattern.CASE_INSENSITIVE)
        );

        assertEquals("Lyon", result.newValue());
        assertEquals(1, result.matchCount());
    }

    @Test
    void countsZeroLengthMatches() {
        RegexReplacementResult result = RegexReplacementService.replace(
            "aba",
            new RegexReplacementRequest("(?=a)", "X", 0)
        );

        assertEquals("XabXa", result.newValue());
        assertEquals(2, result.matchCount());
    }

    @Test
    void supportsLiteralMode() {
        RegexReplacementResult result = RegexReplacementService.replace(
            "a.b aXb",
            new RegexReplacementRequest("a.b", "value", 0, RegexReplacementMode.LITERAL)
        );

        assertEquals("value aXb", result.newValue());
        assertEquals(1, result.matchCount());
    }

    @Test
    void supportsWholeValueMode() {
        RegexReplacementResult result = RegexReplacementService.replace(
            "District 12",
            new RegexReplacementRequest("District \\d+", "District", 0, RegexReplacementMode.WHOLE_VALUE)
        );

        assertEquals("District", result.newValue());
        assertEquals(1, result.matchCount());
    }

    @Test
    void reportsInvalidPatterns() {
        assertThrows(
            java.util.regex.PatternSyntaxException.class,
            () -> RegexReplacementService.replace(
                "value",
                new RegexReplacementRequest("[", "", 0)
            )
        );
    }

    @Test
    void reportsInvalidReplacementGroups() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RegexReplacementService.replace(
                "value",
                new RegexReplacementRequest("value", "$2", 0)
            )
        );
    }

    @Test
    void emptyResultsDeleteTagsByDefault() {
        assertEquals(
            null,
            RegexReplacementCommandService.valueForCommand(
                "", RegexReplacementEmptyValuePolicy.DELETE_TAG)
        );
    }

    @Test
    void emptyResultsCanBePreserved() {
        assertEquals(
            "",
            RegexReplacementCommandService.valueForCommand(
                "", RegexReplacementEmptyValuePolicy.KEEP_EMPTY)
        );
    }

    @Test
    void trimsInputBeforeReplacementWhenRequested() {
        RegexReplacementResult result = RegexReplacementService.replace(
            "  Paris  ",
            new RegexReplacementRequest("Paris", "Lyon", 0, RegexReplacementMode.REGEX, true, false, false)
        );

        assertEquals("Lyon", result.newValue());
    }

    @Test
    void preventsEmptyResultsWhenRequested() {
        RegexReplacementResult result = RegexReplacementService.replace(
            "Paris",
            new RegexReplacementRequest("Paris", "", 0, RegexReplacementMode.REGEX, false, false, true)
        );

        assertEquals("Paris", result.newValue());
        assertFalse(result.changed());
    }

    @Test
    void normalizesUnicodeWhenRequested() {
        RegexReplacementResult result = RegexReplacementService.replace(
            "\u0043\u0061\u0066\u0065\u0301",
            new RegexReplacementRequest("\u0043\u0061\u0066\u00e9", "Coffee", 0,
                RegexReplacementMode.REGEX, false, true, false)
        );

        assertEquals("Coffee", result.newValue());
    }

}