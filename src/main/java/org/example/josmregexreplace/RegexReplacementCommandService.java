package org.example.josmregexreplace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;

public final class RegexReplacementCommandService {

    private RegexReplacementCommandService() {
    }

    public static SequenceCommand createSequence(
        String key,
        List<RegexReplacementPreviewRow> rows,
        RegexReplacementEmptyValuePolicy emptyValuePolicy) {
        List<Command> commands = new ArrayList<>();
        for (RegexReplacementPreviewRow row : rows) {
            String newValue = valueForCommand(row.newValue(), emptyValuePolicy);
            Map<String, String> changes = new LinkedHashMap<>();
            if (row.removeSource() && !row.sourceKey().equals(row.key())) {
                changes.put(row.sourceKey(), null);
            }
            changes.put(row.key(), newValue);
            commands.add(new ChangePropertyCommand(
                Collections.singleton(row.primitive()), changes));
        }
        return new SequenceCommand("Regex replace " + key, commands);
    }

    static String valueForCommand(String newValue, RegexReplacementEmptyValuePolicy emptyValuePolicy) {
        if (emptyValuePolicy == RegexReplacementEmptyValuePolicy.DELETE_TAG && newValue.isEmpty()) {
            return null;
        }
        return newValue;
    }
}