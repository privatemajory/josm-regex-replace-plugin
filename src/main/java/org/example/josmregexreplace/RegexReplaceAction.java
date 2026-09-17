package org.example.josmregexreplace;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
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
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getEditDataSet() != null);
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
        String layerName = getLayerManager().getLayersOfType(OsmDataLayer.class).stream()
            .filter(layer -> layer.getDataSet() == ds)
            .map(OsmDataLayer::getName)
            .findFirst()
            .orElse("Active edit layer");
        RegexReplaceDialog dialog = new RegexReplaceDialog(
            MainApplication.getMainFrame(), ds, tagKeys, layerName);
        RegexReplaceDialog.Result dialogResult = dialog.showDialog();
        if (dialogResult == null) {
            return;
        }

        UndoRedoHandler.getInstance().add(RegexReplacementCommandService.createSequence(
            dialogResult.key(), dialogResult.rows(), dialogResult.emptyValuePolicy()));
    }
}
