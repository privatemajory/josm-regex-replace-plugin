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