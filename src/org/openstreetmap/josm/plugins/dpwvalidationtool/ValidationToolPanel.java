package org.openstreetmap.josm.plugins.dpwvalidationtool;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ValidationToolPanel extends ToggleDialog {

    private JTextField taskIdField;
    private JComboBox<String> mapperUsernameComboBox;
    private JTextField totalBuildingsField;
    private JTextArea validatorCommentsArea;

    private final String[] errorTypes = {
        "Hanging Nodes", "Overlapping Buildings", "Buildings Crossing Highway",
        "Missing Tags", "Improper Tags", "Features Misidentified",
        "Missing Buildings", "Building Inside Building", "Building Crossing Residential",
        "Improperly Drawn"
    };
    private int[] errorCounts = new int[errorTypes.length];
    private JLabel[] errorCountLabels = new JLabel[errorTypes.length];

    public ValidationToolPanel() {
        super(I18n.tr("DPW Validation Tool v2.0"), "validator", I18n.tr("Open DPW Validation Tool"), null, 150);
        try {
            Logging.info("DPWValidationTool: constructing ValidationToolPanel v2.0");
            setupUI();
            updatePanelData();
            Logging.info("DPWValidationTool: ValidationToolPanel v2.0 constructed");
        } catch (Throwable t) {
            Logging.error(t);
        }
    }

    private void setupUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Task ID
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Task ID:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        taskIdField = new JTextField();
        panel.add(taskIdField, gbc);

        // Mapper Username
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Mapper Username:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        mapperUsernameComboBox = new JComboBox<>();
        panel.add(mapperUsernameComboBox, gbc);

        // Total Buildings
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Total Buildings:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        totalBuildingsField = new JTextField();
        totalBuildingsField.setEditable(false);
        panel.add(totalBuildingsField, gbc);
        
        // Separator
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JSeparator(), gbc);

        // Error Rows
        for (int i = 0; i < errorTypes.length; i++) {
            gbc.gridy++;
            addErrorRow(panel, gbc, errorTypes[i], i);
        }

        // Separator
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JSeparator(), gbc);

        // Comments
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Validator Comments:"), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0; // Allow comments to grow vertically
        validatorCommentsArea = new JTextArea(5, 20);
        panel.add(new JScrollPane(validatorCommentsArea), gbc);
        gbc.weighty = 0; // Reset for subsequent components

        // Action Buttons
        final JButton validateButton = new JButton("Validate (Accept)");
        final JButton invalidateButton = new JButton("Invalidate (Reject)");
        final JButton refreshButton = new JButton("Refresh");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(validateButton);
        buttonPanel.add(invalidateButton);
        buttonPanel.add(refreshButton);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(buttonPanel, gbc);

        createLayout(panel, false, null);

        // Action Listeners
        validateButton.addActionListener(e -> submitData("Validated"));
        invalidateButton.addActionListener(e -> submitData("Rejected"));
        refreshButton.addActionListener(e -> {
            try {
                updatePanelData();
            } catch (Exception ex) {
                Logging.error(ex);
                JOptionPane.showMessageDialog(this, "Error refreshing layer: " + ex.getMessage(), "Refresh Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Disable submit buttons until Task ID is provided
        validateButton.setEnabled(false);
        invalidateButton.setEnabled(false);
        taskIdField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                boolean enabled = !taskIdField.getText().trim().isEmpty();
                validateButton.setEnabled(enabled);
                invalidateButton.setEnabled(enabled);
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });
    }

    private void addErrorRow(JPanel panel, GridBagConstraints gbc, String labelText, final int index) {
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0.8; // Give label space
        panel.add(new JLabel(labelText), gbc);

        // Panel for buttons and count (minus, plus, count)
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton minusButton = new JButton("-");
        minusButton.addActionListener(e -> {
            if (errorCounts[index] > 0) {
                errorCounts[index]--;
                errorCountLabels[index].setText(String.valueOf(errorCounts[index]));
            }
        });
        JButton plusButton = new JButton("+");
        plusButton.addActionListener(e -> {
            errorCounts[index]++;
            errorCountLabels[index].setText(String.valueOf(errorCounts[index]));
        });

        errorCountLabels[index] = new JLabel(String.valueOf(errorCounts[index]));
        errorCountLabels[index].setHorizontalAlignment(SwingConstants.CENTER);
        errorCountLabels[index].setPreferredSize(new Dimension(30, 20));

        controlPanel.add(minusButton);
        controlPanel.add(plusButton);
        controlPanel.add(errorCountLabels[index]);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0.2;
        panel.add(controlPanel, gbc);
    }

    public void updatePanelData() {
        DataSet dataSet = MainApplication.getLayerManager().getEditDataSet();
        if (dataSet == null) {
            totalBuildingsField.setText("0");
            mapperUsernameComboBox.removeAllItems();
            return;
        }

        int buildingCount = 0;
        Set<String> userNames = new HashSet<>();
        for (OsmPrimitive primitive : dataSet.getPrimitives(p -> true)) {
            if (primitive.hasKey("building")) {
                buildingCount++;
            }
            User user = primitive.getUser();
            if (user != null && user.getName() != null && !user.getName().isEmpty()) {
                userNames.add(user.getName());
            }
        }
        totalBuildingsField.setText(String.valueOf(buildingCount));

        // Populate mapper dropdown
        mapperUsernameComboBox.removeAllItems();
        List<String> sortedUsers = new ArrayList<>(userNames);
        Collections.sort(sortedUsers);
        for (String userName : sortedUsers) {
            mapperUsernameComboBox.addItem(userName);
        }
    }

    private void submitData(String validationStatus) {
        String taskId = taskIdField.getText();
        String mapperUsername = (String) mapperUsernameComboBox.getSelectedItem();
        if (mapperUsername == null) {
            mapperUsername = "";
        }
        String totalBuildings = totalBuildingsField.getText();
        String validatorComments = validatorCommentsArea.getText();
        String validatorUsername = Preferences.main().get("osm-server.username", "unknown");
        String settlement = Preferences.main().get("dpw.settlement", "Mji wa Huruma");

        int totalBuildingsInt = 0;
        try {
            totalBuildingsInt = Integer.parseInt(totalBuildings.trim());
        } catch (NumberFormatException e) {
            // Keep as 0 if parsing fails
        }

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{")
            .append("\"task_id\": \"").append(jsonEscape(taskId)).append("\",")
            .append("\"settlement\": \"").append(jsonEscape(settlement)).append("\",")
            .append("\"mapper_username\": \"").append(jsonEscape(mapperUsername)).append("\",")
            .append("\"validator_username\": \"").append(jsonEscape(validatorUsername)).append("\",")
            .append("\"total_buildings\": ").append(totalBuildingsInt).append(",");

        for (int i = 0; i < errorTypes.length; i++) {
            String errorKey = "error_" + errorTypes[i].toLowerCase().replace(' ', '_');
            jsonBuilder.append("\"").append(errorKey).append("\": ").append(errorCounts[i]).append(",");
        }

        jsonBuilder.append("\"validation_status\": \"").append(jsonEscape(validationStatus)).append("\",")
            .append("\"validator_comments\": \"").append(jsonEscape(validatorComments)).append("\"")
            .append("}");

        sendPostRequest(jsonBuilder.toString());
    }

    private void sendPostRequest(String jsonData) {
        new Thread(() -> {
            try {
                URL url = new URI("https://script.google.com/macros/s/AKfycbytfxwDkZG1qDpgXV4QcplBgq4u9PVSW7yQzfe47UG4dmM_nh5-D6mb7LZ4vxib9KQp/exec").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }

                final int rc = responseCode;
                final String respBody = response.toString();
                SwingUtilities.invokeLater(() -> {
                    if (rc == HttpURLConnection.HTTP_OK) {
                        JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "Data submitted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "Data submission failed. Code: " + rc + "\nResponse: " + respBody, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });

            } catch (Exception e) {
                Logging.error(e);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "An error occurred during submission: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20 || c > 0x7e) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}