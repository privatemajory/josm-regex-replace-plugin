package org.example.josmregexreplace;

import java.util.List;

public record RegexReplacementPreview(
    List<RegexReplacementPreviewRow> rows,
    int scannedCount,
    int matchedCount,
    int changedCount,
    int skippedCount,
    boolean truncated
) {
}