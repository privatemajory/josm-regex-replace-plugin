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

import java.util.Set;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

public record RegexReplacementFilter(Set<RegexReplacementPrimitiveType> primitiveTypes) {

    public RegexReplacementFilter {
        primitiveTypes = Set.copyOf(primitiveTypes);
    }

    public boolean accepts(OsmPrimitive primitive) {
        if (primitive instanceof Node) {
            return primitiveTypes.contains(RegexReplacementPrimitiveType.NODE);
        }
        if (primitive instanceof Way) {
            return primitiveTypes.contains(RegexReplacementPrimitiveType.WAY);
        }
        if (primitive instanceof Relation) {
            return primitiveTypes.contains(RegexReplacementPrimitiveType.RELATION);
        }
        return false;
    }
}