package org.openstreetmap.josm.plugins.dpwvalidationtool;

import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

public class DPWValidationToolPlugin extends Plugin {

    private ValidationToolPanel validationToolPanel;

    public DPWValidationToolPlugin(PluginInformation info) {
        super(info);
        // Defer constructing the UI panel until a MapFrame is available.
        // Creating a ToggleDialog too early can cause a NullPointerException
        // because internal titleBar fields are initialized later by JOSM.
        validationToolPanel = null;

        // Register a Tools menu action and toolbar button to show the dialog explicitly
        try {
            javax.swing.JMenuItem mi = new javax.swing.JMenuItem(new AbstractAction("DPW Validation Tool") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        if (validationToolPanel == null) {
                            validationToolPanel = new ValidationToolPanel();
                        }
                        MainApplication.getMap().addToggleDialog(validationToolPanel);
                    } catch (Throwable ex) {
                        Logging.error("DPWValidationTool: failed to show dialog from menu action: " + ex);
                        Logging.trace(ex);
                    }
                }
            });
            MainApplication.getMenu().toolsMenu.add(mi);
        } catch (Exception ex) {
            Logging.warn("DPWValidationTool: failed to register menu action: " + ex);
            Logging.trace(ex);
        }
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        try {
            Logging.info("DPWValidationTool: mapFrameInitialized called, newFrame=" + newFrame);
            if (newFrame != null && validationToolPanel != null) {
                newFrame.addToggleDialog(validationToolPanel);
                Logging.info("DPWValidationTool: added toggle dialog to newFrame");
            }
        } catch (Throwable t) {
            Logging.error("DPWValidationTool: mapFrameInitialized failed: " + t);
            Logging.trace(t);
        }
    }
}