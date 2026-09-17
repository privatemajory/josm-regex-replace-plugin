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
