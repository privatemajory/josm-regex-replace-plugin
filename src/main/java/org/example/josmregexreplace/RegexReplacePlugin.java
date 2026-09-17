package org.example.josmregexreplace;

import javax.swing.JMenu;
import javax.swing.JButton;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * Entry point JOSM loads when the plugin starts.
 * Adds a "Regex Search/Replace..." item to the "More tools" menu.
 */
public class RegexReplacePlugin extends Plugin {

    public RegexReplacePlugin(PluginInformation info) {
        super(info);
        RegexReplaceAction action = new RegexReplaceAction();
        JMenu moreTools = MainApplication.getMenu().moreToolsMenu;
        moreTools.add(action);
        MainApplication.getToolbar().register(action);
        MainApplication.getToolbar().control.add(new JButton(action));
    }
}
