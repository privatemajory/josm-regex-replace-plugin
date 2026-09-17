package org.example.josmregexreplace;

import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

public final class RegexReplacementCondition {

    private final String key;
    private final Pattern pattern;

    public RegexReplacementCondition(String key, String expression, int flags) {
        this.key = key;
        this.pattern = Pattern.compile(expression, flags);
    }

    public boolean matches(OsmPrimitive primitive) {
        String value = primitive.get(key);
        return value != null && pattern.matcher(value).find();
    }
}