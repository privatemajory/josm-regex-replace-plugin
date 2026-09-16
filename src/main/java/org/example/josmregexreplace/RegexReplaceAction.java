package org.example.josmregexreplace;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Prompts for a tag key + regex find/replace, previews the changes,
 * then applies them as a single undoable SequenceCommand.
 *
 * Semantics match Java's Matcher.replaceAll(): the pattern does NOT need
 * to match the whole value (unlike JOSM's own "~" search operator), and
 * capture groups ($1, $2, ...) are supported in the replacement string.
 */
public class RegexReplaceAction extends JosmAction {

    public RegexReplaceAction() {
        super(
            "Regex Search/Replace...",
            "regexreplace",
            "Search and replace tag values using a regular expression",
            Shortcut.registerShortcut(
                "tools:regexreplace",
                "More tools: Regex Search/Replace",
                KeyEvent.VK_R,
                Shortcut.ALT_CTRL_SHIFT
            ),
            true
        );
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DataSet ds = getLayerManager().getEditDataSet();
        if (ds == null) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                "No active data layer.", "Regex Replace", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Set<String> tagKeys = new TreeSet<>();
        for (OsmPrimitive obj : ds.allPrimitives()) {
            tagKeys.addAll(obj.keySet());
        }
        tagKeys.add("name");
        JComboBox<String> keyField = new JComboBox<>(tagKeys.toArray(new String[0]));
        keyField.setEditable(true);
        keyField.setSelectedItem("name");
        JTextField findField = new JTextField("^District d(e |')", 20);
        JTextField replaceField = new JTextField("", 20);
        JCheckBox selectionOnly = new JCheckBox("Selected objects only", true);
        JCheckBox ignoreCase = new JCheckBox("Ignore case", false);

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(labeled("Tag key:", keyField));
        panel.add(labeled("Find (regex):", findField));
        panel.add(labeled("Replace with:", replaceField));
        panel.add(selectionOnly);
        panel.add(ignoreCase);

        int result = JOptionPane.showConfirmDialog(
            MainApplication.getMainFrame(), panel, "Regex Search/Replace tag value",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String key = keyField.getEditor().getItem().toString().trim();
        String findRegex = findField.getText();
        String replacement = replaceField.getText();

        if (key.isEmpty() || findRegex.isEmpty()) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                "Tag key and search pattern are required.", "Regex Replace", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Pattern pattern;
        try {
            int flags = ignoreCase.isSelected() ? Pattern.CASE_INSENSITIVE : 0;
            pattern = Pattern.compile(findRegex, flags);
        } catch (PatternSyntaxException ex) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                "Invalid regular expression:\n" + ex.getMessage(), "Regex Replace", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Collection<OsmPrimitive> candidates = selectionOnly.isSelected()
            ? ds.getSelected()
            : ds.allPrimitives();

        List<OsmPrimitive> matched = new ArrayList<>();
        List<String> oldValues = new ArrayList<>();
        List<String> newValues = new ArrayList<>();

        for (OsmPrimitive obj : candidates) {
            String value = obj.get(key);
            if (value == null) {
                continue;
            }
            Matcher m = pattern.matcher(value);
            if (m.find()) {
                String newValue = m.replaceAll(replacement);
                if (!newValue.equals(value)) {
                    matched.add(obj);
                    oldValues.add(value);
                    newValues.add(newValue);
                }
            }
        }

        if (matched.isEmpty()) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                "No matching tag values found.", "Regex Replace", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Old value", "New value"}, 0);
        for (int i = 0; i < matched.size(); i++) {
            model.addRow(new Object[]{oldValues.get(i), newValues.get(i)});
        }
        JTable table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(600, 300));

        int confirm = JOptionPane.showConfirmDialog(
            MainApplication.getMainFrame(), scroll,
            matched.size() + " object(s) will be changed. Apply?",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }

        List<Command> commands = new ArrayList<>();
        for (int i = 0; i < matched.size(); i++) {
            commands.add(new ChangePropertyCommand(
                Collections.singleton(matched.get(i)), key, newValues.get(i)));
        }

        UndoRedoHandler.getInstance().add(new SequenceCommand("Regex replace " + key, commands));
    }

    private JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.add(new JLabel(label), BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }
}
