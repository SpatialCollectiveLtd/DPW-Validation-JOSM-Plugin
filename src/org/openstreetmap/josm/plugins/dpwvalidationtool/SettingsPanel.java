package org.openstreetmap.josm.plugins.dpwvalidationtool;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.GBC;

/**
 * Settings Panel for DPW Validation Tool Plugin
 * Allows configuration of API endpoints, TM integration, and feature toggles
 * 
 * @version 3.0.6
 */
public class SettingsPanel extends JDialog {
    
    private JCheckBox tmIntegrationCheckbox;
    private JTextField dpwApiUrlField;
    private JTextField tmApiUrlField;
    private JTextField projectUrlField;
    private JTextField projectIdField;
    private JCheckBox autoFetchSettlementCheckbox;
    private JCheckBox remoteControlDetectionCheckbox;
    private JSpinner cacheExpirySpinner;
    
    public SettingsPanel() {
        super(MainApplication.getMainFrame(), "DPW Validation Tool - Settings", true);
        initComponents();
        loadSettings();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Main panel with settings
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        int row = 0;
        
        // Header
        JLabel headerLabel = new JLabel("<html><b>DPW Validation Tool Settings v" + UpdateChecker.CURRENT_VERSION + "</b></html>");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        mainPanel.add(headerLabel, GBC.eol().fill(GBC.HORIZONTAL).insets(0, 0, 0, 15));
        row++;
        
        // API Configuration Section
        mainPanel.add(new JLabel("<html><b>API Configuration</b></html>"), 
            GBC.eol().fill(GBC.HORIZONTAL).insets(0, 10, 0, 5));
        row++;
        
        // DPW API URL
        mainPanel.add(new JLabel("DPW API Base URL:"), GBC.std().insets(5, 5, 5, 5));
        dpwApiUrlField = new JTextField(40);
        dpwApiUrlField.setToolTipText("Base URL for DPW API (e.g., app.spatialcollective.com/api)");
        mainPanel.add(dpwApiUrlField, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 5, 5));
        row++;
        
        JLabel dpwExample = new JLabel("<html><i>Example: app.spatialcollective.com/api</i></html>");
        dpwExample.setForeground(Color.GRAY);
        mainPanel.add(new JLabel(""), GBC.std());
        mainPanel.add(dpwExample, GBC.eol().insets(5, 0, 5, 10));
        row++;
        
        // TM API URL
        mainPanel.add(new JLabel("Tasking Manager API URL:"), GBC.std().insets(5, 5, 5, 5));
        tmApiUrlField = new JTextField(40);
        tmApiUrlField.setToolTipText("Base URL for HOT Tasking Manager API");
        mainPanel.add(tmApiUrlField, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 5, 5));
        row++;
        
        JLabel tmExample = new JLabel("<html><i>Default: tasking-manager-tm4-production-api.hotosm.org/api/v2</i></html>");
        tmExample.setForeground(Color.GRAY);
        mainPanel.add(new JLabel(""), GBC.std());
        mainPanel.add(tmExample, GBC.eol().insets(5, 0, 5, 10));
        row++;
        
        // Default Project Configuration Section
        mainPanel.add(new JLabel("<html><b>Default Project Configuration</b></html>"), 
            GBC.eol().fill(GBC.HORIZONTAL).insets(0, 15, 0, 5));
        row++;
        
        // Project URL
        mainPanel.add(new JLabel("Default Project URL:"), GBC.std().insets(5, 5, 5, 5));
        projectUrlField = new JTextField(40);
        projectUrlField.setToolTipText("<html>Default TM project URL to pre-fill for validators<br>" +
            "Example: https://tasks.hotosm.org/projects/27396<br>" +
            "This saves validators from entering URLs repeatedly</html>");
        mainPanel.add(projectUrlField, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 5, 5));
        row++;
        
        JLabel projectUrlExample = new JLabel("<html><i>Example: https://tasks.hotosm.org/projects/27396</i></html>");
        projectUrlExample.setForeground(Color.GRAY);
        mainPanel.add(new JLabel(""), GBC.std());
        mainPanel.add(projectUrlExample, GBC.eol().insets(5, 0, 5, 5));
        row++;
        
        // OR Project ID
        mainPanel.add(new JLabel("OR Default Project ID:"), GBC.std().insets(5, 5, 5, 5));
        projectIdField = new JTextField(40);
        projectIdField.setToolTipText("<html>Default TM project ID (e.g., 27396)<br>" +
            "Used if full URL not provided<br>" +
            "One of Project URL or Project ID should be set</html>");
        mainPanel.add(projectIdField, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 5, 5));
        row++;
        
        JLabel projectIdNote = new JLabel("<html><i>💡 Tip: Set either Project URL OR Project ID to avoid entering URLs for each validation</i></html>");
        projectIdNote.setForeground(new Color(0, 100, 200));
        mainPanel.add(new JLabel(""), GBC.std());
        mainPanel.add(projectIdNote, GBC.eol().insets(5, 0, 5, 10));
        row++;
        
        // Feature Toggles Section
        mainPanel.add(new JLabel("<html><b>Feature Toggles</b></html>"), 
            GBC.eol().fill(GBC.HORIZONTAL).insets(0, 15, 0, 5));
        row++;
        
        // TM Integration Toggle
        tmIntegrationCheckbox = new JCheckBox("Enable Tasking Manager Integration");
        tmIntegrationCheckbox.setToolTipText("<html>Enable integration with HOT Tasking Manager<br>" +
            "Allows automatic mapper detection from TM tasks<br>" +
            "<b>BETA feature - test before production use</b></html>");
        mainPanel.add(tmIntegrationCheckbox, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 5, 5));
        row++;
        
        JLabel tmWarning = new JLabel("<html><i>⚠ BETA: Automatically detects mapper from TM URLs or remote control</i></html>");
        tmWarning.setForeground(new Color(200, 100, 0));
        mainPanel.add(new JLabel(""), GBC.std());
        mainPanel.add(tmWarning, GBC.eol().insets(5, 0, 5, 10));
        row++;
        
        // Auto-fetch settlement toggle
        autoFetchSettlementCheckbox = new JCheckBox("Auto-fetch settlement from DPW API");
        autoFetchSettlementCheckbox.setToolTipText("<html>Automatically fetch settlement when mapper is selected<br>" +
            "Requires valid DPW API credentials</html>");
        mainPanel.add(autoFetchSettlementCheckbox, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 5, 5));
        row++;
        
        // Remote control detection toggle
        remoteControlDetectionCheckbox = new JCheckBox("Enable Remote Control Task Detection");
        remoteControlDetectionCheckbox.setToolTipText("<html>Automatically detect TM task from JOSM remote control<br>" +
            "Parses changeset comments: #hotosm-project-XXXXX-task-YYY<br>" +
            "<b>Recommended for validators using TM workflow</b></html>");
        mainPanel.add(remoteControlDetectionCheckbox, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 5, 5));
        row++;
        
        // Cache Settings Section
        mainPanel.add(new JLabel("<html><b>Cache Settings</b></html>"), 
            GBC.eol().fill(GBC.HORIZONTAL).insets(0, 15, 0, 5));
        row++;
        
        mainPanel.add(new JLabel("Cache Expiry (hours):"), GBC.std().insets(5, 5, 5, 5));
        cacheExpirySpinner = new JSpinner(new SpinnerNumberModel(24, 1, 168, 1));
        cacheExpirySpinner.setToolTipText("How long to cache TM task information (1-168 hours)");
        mainPanel.add(cacheExpirySpinner, GBC.eol().insets(5, 5, 5, 10));
        row++;
        
        // Add flexible space
        mainPanel.add(new JLabel(""), GBC.eol().fill(GBC.BOTH).weight(1.0, 1.0));
        
        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        
        JButton checkUpdatesButton = new JButton("Check for Updates");
        checkUpdatesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateChecker.checkForUpdatesAsync(true);
            }
        });
        buttonsPanel.add(checkUpdatesButton);
        
        JButton resetButton = new JButton("Reset to Defaults");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetToDefaults();
            }
        });
        buttonsPanel.add(resetButton);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        buttonsPanel.add(cancelButton);
        
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSettings();
                JOptionPane.showMessageDialog(SettingsPanel.this,
                    "Settings saved successfully!",
                    "Settings Saved",
                    JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
        });
        buttonsPanel.add(saveButton);
        
        // Add panels to dialog
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);
        
        // Dialog settings
        setSize(700, 600);
        setLocationRelativeTo(MainApplication.getMainFrame());
    }
    
    private void loadSettings() {
        tmIntegrationCheckbox.setSelected(PluginSettings.isTMIntegrationEnabled());
        dpwApiUrlField.setText(PluginSettings.getDPWApiBaseUrl());
        tmApiUrlField.setText(PluginSettings.getTMApiBaseUrl());
        projectUrlField.setText(PluginSettings.getDefaultProjectUrl());
        projectIdField.setText(PluginSettings.getDefaultProjectId());
        autoFetchSettlementCheckbox.setSelected(PluginSettings.isAutoFetchSettlement());
        remoteControlDetectionCheckbox.setSelected(PluginSettings.isRemoteControlDetectionEnabled());
        cacheExpirySpinner.setValue(PluginSettings.getCacheExpiryHours());
    }
    
    private void saveSettings() {
        PluginSettings.setTMIntegrationEnabled(tmIntegrationCheckbox.isSelected());
        PluginSettings.setDPWApiBaseUrl(dpwApiUrlField.getText().trim());
        PluginSettings.setTMApiBaseUrl(tmApiUrlField.getText().trim());
        PluginSettings.setDefaultProjectUrl(projectUrlField.getText().trim());
        PluginSettings.setDefaultProjectId(projectIdField.getText().trim());
        PluginSettings.setAutoFetchSettlement(autoFetchSettlementCheckbox.isSelected());
        PluginSettings.setRemoteControlDetectionEnabled(remoteControlDetectionCheckbox.isSelected());
        PluginSettings.setCacheExpiryHours((Integer) cacheExpirySpinner.getValue());
    }
    
    private void resetToDefaults() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Reset all settings to default values?",
            "Confirm Reset",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            PluginSettings.resetToDefaults();
            loadSettings();
            JOptionPane.showMessageDialog(this,
                "Settings reset to defaults.",
                "Reset Complete",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Show the settings dialog
     */
    public static void showSettingsDialog() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SettingsPanel dialog = new SettingsPanel();
                dialog.setVisible(true);
            }
        });
    }
}
