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