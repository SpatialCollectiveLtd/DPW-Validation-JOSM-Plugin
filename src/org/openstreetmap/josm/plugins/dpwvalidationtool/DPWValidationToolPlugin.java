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
    private boolean panelAdded = false;

    public DPWValidationToolPlugin(PluginInformation info) {
        super(info);
        // Defer constructing the UI panel until a MapFrame is available.
        // Creating a ToggleDialog too early can cause a NullPointerException
        // because internal titleBar fields are initialized later by JOSM.
        validationToolPanel = null;

        // Register a Tools menu action and toolbar button to show the dialog explicitly
        try {
            javax.swing.AbstractAction action = new javax.swing.AbstractAction("DPW Validation Tool") {
                {
                    // try to set a small pirate icon programmatically
                    try {
                        putValue(NAME, "DPW Validation Tool");
                        putValue(SMALL_ICON, createPirateIcon());
                    } catch (Throwable t) {
                        // ignore icon creation problems
                    }
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        if (validationToolPanel == null) {
                            validationToolPanel = new ValidationToolPanel();
                            // set a larger title icon for the dialog if possible
                            try {
                                javax.swing.Icon ic = IconResources.getPirateIcon(24);
                                validationToolPanel.setIcon(ic);
                            } catch (Throwable ignore) {}
                        }
                        if (!panelAdded) {
                            MainApplication.getMap().addToggleDialog(validationToolPanel);
                            panelAdded = true;
                        }
                    } catch (Throwable ex) {
                        Logging.error("DPWValidationTool: failed to show dialog from menu action: " + ex);
                        Logging.trace(ex);
                    }
                }
            };
            javax.swing.JMenuItem mi = new javax.swing.JMenuItem(action);
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
            if (newFrame != null && validationToolPanel != null && !panelAdded) {
                newFrame.addToggleDialog(validationToolPanel);
                panelAdded = true;
                Logging.info("DPWValidationTool: added toggle dialog to newFrame");
            }
        } catch (Throwable t) {
            Logging.error("DPWValidationTool: mapFrameInitialized failed: " + t);
            Logging.trace(t);
        }
    }

    /**
     * Create a small programmatic pirate-style icon so we don't have to bundle a bitmap.
     */
    private javax.swing.Icon createPirateIcon() {
        int w = 18, h = 18;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            // background transparent
            g.setColor(new java.awt.Color(0,0,0,0));
            g.fillRect(0,0,w,h);
            // draw skull
            g.setColor(java.awt.Color.WHITE);
            g.fillOval(3,2,12,10);
            g.setColor(java.awt.Color.BLACK);
            g.fillOval(6,5,2,2); // left eye
            g.fillOval(10,5,2,2); // right eye
            // eye patch strap
            g.setStroke(new java.awt.BasicStroke(2f));
            g.drawLine(3,6,15,6);
            // mouth
            g.drawArc(7,8,4,3,0,-180);
            // crossbones
            g.setStroke(new java.awt.BasicStroke(1.5f));
            g.drawLine(2,14,8,9);
            g.drawLine(8,14,2,9);
            g.drawLine(10,14,16,9);
            g.drawLine(16,14,10,9);
        } finally {
            g.dispose();
        }
        return new javax.swing.ImageIcon(img);
    }
}