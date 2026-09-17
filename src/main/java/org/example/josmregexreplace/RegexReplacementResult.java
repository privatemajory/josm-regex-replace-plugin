package org.example.josmregexreplace;

public record RegexReplacementResult(String oldValue, String newValue, int matchCount) {

    public boolean changed() {
        return !oldValue.equals(newValue);
    }

    public boolean matched() {
        return matchCount > 0;
    }
}