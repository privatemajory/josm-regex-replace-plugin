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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.Match;
import org.openstreetmap.josm.data.osm.search.SearchParseError;

public final class RegexReplacementPreviewService {

    private static final int DEFAULT_PREVIEW_LIMIT = 5_000;

    private RegexReplacementPreviewService() {
    }

    public static RegexReplacementPreview preview(
        DataSet dataSet,
        String key,
        RegexReplacementRequest request,
        RegexReplacementScope scope) throws SearchParseError {
        return preview(
            dataSet,
            key,
            request,
            scope,
            new RegexReplacementFilter(java.util.Set.of(
                RegexReplacementPrimitiveType.NODE,
                RegexReplacementPrimitiveType.WAY,
                RegexReplacementPrimitiveType.RELATION)),
            "");
    }

    public static RegexReplacementPreview preview(
        DataSet dataSet,
        String key,
        RegexReplacementRequest request,
        RegexReplacementScope scope,
        RegexReplacementFilter filter,
        String searchExpression) throws SearchParseError {
        return preview(
            dataSet, key, request, scope, filter, searchExpression,
            null, 0, Integer.MAX_VALUE, DEFAULT_PREVIEW_LIMIT);
    }

    public static RegexReplacementPreview preview(
        DataSet dataSet,
        String key,
        RegexReplacementRequest request,
        RegexReplacementScope scope,
        RegexReplacementFilter filter,
        String searchExpression,
        RegexReplacementCondition condition,
        int minimumMatches,
        int maximumMatches,
        int previewLimit) throws SearchParseError {
        return preview(
            dataSet, key, key, false, false, request, scope, filter, searchExpression,
            condition, minimumMatches, maximumMatches, previewLimit);
    }

    public static RegexReplacementPreview preview(
        DataSet dataSet,
        String sourceKey,
        String targetKey,
        boolean removeSource,
        boolean createMissing,
        RegexReplacementRequest request,
        RegexReplacementScope scope,
        RegexReplacementFilter filter,
        String searchExpression,
        RegexReplacementCondition condition,
        int minimumMatches,
        int maximumMatches,
        int previewLimit) throws SearchParseError {
        Collection<OsmPrimitive> candidates = scope == RegexReplacementScope.SELECTED
            ? dataSet.getSelected()
            : dataSet.allPrimitives();
        Match searchMatch = searchExpression == null || searchExpression.isBlank()
            ? null
            : SearchCompiler.compile(searchExpression);
        List<RegexReplacementPreviewRow> rows = new ArrayList<>();
        int scannedCount = 0;
        int matchedCount = 0;
        int changedCount = 0;
        boolean truncated = false;
        for (OsmPrimitive primitive : candidates) {
            if (!filter.accepts(primitive) || (searchMatch != null && !searchMatch.match(primitive))) {
                continue;
            }
            if (condition != null && !condition.matches(primitive)) {
                continue;
            }
            String value = primitive.get(sourceKey);
            if (value == null && !createMissing) {
                continue;
            }
            if (value == null) {
                value = "";
            }
            scannedCount++;
            RegexReplacementResult replacementResult = RegexReplacementService.replace(value, request);
            if (replacementResult.matchCount() < minimumMatches
                || replacementResult.matchCount() > maximumMatches) {
                continue;
            }
            if (replacementResult.matched()) {
                matchedCount++;
            }
            if (replacementResult.changed()) {
                changedCount++;
                if (rows.size() < previewLimit) {
                    rows.add(new RegexReplacementPreviewRow(
                        primitive, targetKey, sourceKey, value,
                        replacementResult.newValue(), replacementResult.matchCount(), removeSource));
                } else {
                    truncated = true;
                }
            }
        }
        int candidateCount = candidates.size();
        return new RegexReplacementPreview(
            rows, scannedCount, matchedCount, changedCount,
            Math.max(0, candidateCount - scannedCount), truncated);
    }
}