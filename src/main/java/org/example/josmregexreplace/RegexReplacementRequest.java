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

public record RegexReplacementRequest(
	String findRegex,
	String replacement,
	int patternFlags,
	RegexReplacementMode mode,
	boolean trimWhitespace,
	boolean normalizeUnicode,
	boolean preventEmptyResult
) {

	public RegexReplacementRequest(String findRegex, String replacement, int patternFlags) {
		this(findRegex, replacement, patternFlags, RegexReplacementMode.REGEX, false, false, false);
	}

	public RegexReplacementRequest(
		String findRegex,
		String replacement,
		int patternFlags,
		RegexReplacementMode mode) {
		this(findRegex, replacement, patternFlags, mode, false, false, false);
	}
}