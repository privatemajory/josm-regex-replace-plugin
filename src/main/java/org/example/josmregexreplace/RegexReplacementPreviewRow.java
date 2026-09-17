package org.example.josmregexreplace;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

public record RegexReplacementPreviewRow(
    OsmPrimitive primitive,
    String key,
    String sourceKey,
    String oldValue,
    String newValue,
    int matchCount,
    boolean removeSource
) {

    public RegexReplacementPreviewRow(
        OsmPrimitive primitive,
        String key,
        String oldValue,
        String newValue,
        int matchCount) {
        this(primitive, key, key, oldValue, newValue, matchCount, false);
    }
}