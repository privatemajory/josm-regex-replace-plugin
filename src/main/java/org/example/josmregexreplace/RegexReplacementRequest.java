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