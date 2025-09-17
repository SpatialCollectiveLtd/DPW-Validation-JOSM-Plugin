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
import java.net.URLEncoder;
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
    private List<String> authorizedMappers = new ArrayList<>();
    private JLabel authStatusLabel;
    private JButton validateButton;
    private JButton invalidateButton;
    private JButton refreshButton;
    private JButton refreshMapperListButton;
    private JButton forceSubmitButton;
    private volatile boolean allowForceSubmit = false;
    private volatile boolean isSending = false;
    private volatile boolean isFetchingMappers = false;

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
            // Kick off an initial authorized-mapper fetch in background
            new Thread(() -> {
                try {
                    setFetchingMappers(true);
                    fetchAuthorizedMappers();
                    SwingUtilities.invokeLater(() -> updateAuthStatus());
                } catch (Exception e) {
                    Logging.info("DPWValidationTool: initial mapper fetch failed: " + e.getMessage());
                } finally {
                    setFetchingMappers(false);
                }
            }).start();
            Logging.info("DPWValidationTool: ValidationToolPanel v2.0 constructed");
        } catch (Throwable t) {
            Logging.error(t);
        }
    }

    private void setupUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        // Constrain preferred width so the toggle dialog doesn't expand too wide
        panel.setPreferredSize(new Dimension(640, 480));
        panel.setMaximumSize(new Dimension(1024, 800));
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

    // NOTE: the mapper-list refresh button will be placed beside the mapper combo (small icon)

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

    // small refresh button next to the mapper combo to avoid widening the panel
    gbc.gridx = 3;
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0;
    refreshMapperListButton = new JButton("\u21bb"); // unicode clockwise refresh
    refreshMapperListButton.setPreferredSize(new Dimension(28, 22));
    refreshMapperListButton.setToolTipText("Refresh authorized mapper list");
    panel.add(refreshMapperListButton, gbc);

        // wire refresh mapper list button
        refreshMapperListButton.addActionListener(e -> {
            setFetchingMappers(true);
            new Thread(() -> {
                try {
                    fetchAuthorizedMappers();
                    SwingUtilities.invokeLater(() -> {
                        updateAuthStatus();
                        JOptionPane.showMessageDialog(this, "Authorized mapper list refreshed (" + authorizedMappers.size() + ")", "Mapper List", JOptionPane.INFORMATION_MESSAGE);
                    });
                } catch (Exception ex) {
                    Logging.error(ex);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed to fetch mapper list: " + ex.getMessage(), "Mapper List Error", JOptionPane.ERROR_MESSAGE));
                } finally {
                    setFetchingMappers(false);
                }
            }).start();
        });

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

    // Authorization status label + Force Submit
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    authStatusLabel = new JLabel("Mapper authorization: unknown");
    authStatusLabel.setOpaque(true);
    authStatusLabel.setBackground(Color.LIGHT_GRAY);
    panel.add(authStatusLabel, gbc);

        // Action Buttons - place each button into the GridBag so they reflow with width
    validateButton = new JButton("Accept");
    validateButton.setToolTipText("Mark this task as validated (accept)");
    invalidateButton = new JButton("Reject");
    invalidateButton.setToolTipText("Mark this task as rejected (invalidate)");
    refreshButton = new JButton("Scan");
    refreshButton.setToolTipText("Rescan the active layer for buildings and mappers");
    forceSubmitButton = new JButton("Force");
    forceSubmitButton.setToolTipText("Force submit even if mapper not authorised (use with caution)");

        JPanel actionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints aGbc = new GridBagConstraints();
        aGbc.insets = new Insets(2,4,2,4);
        aGbc.gridy = 0;
        aGbc.fill = GridBagConstraints.HORIZONTAL;
        aGbc.weightx = 1.0;

        aGbc.gridx = 0;
        actionPanel.add(validateButton, aGbc);
        aGbc.gridx = 1;
        actionPanel.add(invalidateButton, aGbc);
        aGbc.gridx = 2;
        actionPanel.add(refreshButton, aGbc);
        aGbc.gridx = 3;
        actionPanel.add(forceSubmitButton, aGbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(actionPanel, gbc);

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

        forceSubmitButton.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this, "Force submit will bypass the authorized mapper list. Continue?", "Confirm Force Submit", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                allowForceSubmit = true;
                forceSubmitButton.setEnabled(false);
                forceSubmitButton.setText("Force Submit (ON)");
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

        mapperUsernameComboBox.addItemListener(e -> updateAuthStatus());
    }

    private void setFetchingMappers(boolean fetching) {
        isFetchingMappers = fetching;
        SwingUtilities.invokeLater(() -> {
            refreshMapperListButton.setEnabled(!fetching);
            if (fetching) refreshMapperListButton.setText("Refreshing..."); else refreshMapperListButton.setText("Refresh Mapper List");
        });
    }

    private void setSending(boolean sending) {
        isSending = sending;
        SwingUtilities.invokeLater(() -> {
            validateButton.setEnabled(!sending && !taskIdField.getText().trim().isEmpty());
            invalidateButton.setEnabled(!sending && !taskIdField.getText().trim().isEmpty());
            refreshButton.setEnabled(!sending);
            refreshMapperListButton.setEnabled(!sending && !isFetchingMappers);
            forceSubmitButton.setEnabled(!sending && !allowForceSubmit);
            if (sending) {
                validateButton.setText("Sending...");
                invalidateButton.setText("Sending...");
            } else {
                validateButton.setText("Validate (Accept)");
                invalidateButton.setText("Invalidate (Reject)");
            }
        });
    }

    private void updateAuthStatus() {
        String sel = (String) mapperUsernameComboBox.getSelectedItem();
        if (sel == null || sel.isEmpty()) {
            authStatusLabel.setText("Mapper authorization: unknown");
            authStatusLabel.setBackground(Color.LIGHT_GRAY);
            return;
        }
        boolean authorized;
        synchronized (authorizedMappers) {
            authorized = authorizedMappers.contains(sel);
        }
        if (authorized) {
            authStatusLabel.setText("Mapper authorization: AUTHORIZED");
            authStatusLabel.setBackground(new Color(0x88ff88));
        } else {
            authStatusLabel.setText("Mapper authorization: UNAUTHORIZED");
            authStatusLabel.setBackground(new Color(0xff8888));
        }
    }

    private void addErrorRow(JPanel panel, GridBagConstraints gbc, String labelText, final int index) {
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0.8; // Give label space
        panel.add(new JLabel(labelText), gbc);

        // Panel for buttons and count (minus, count, plus) using GridBag so it resizes better
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints cGbc = new GridBagConstraints();
        cGbc.insets = new Insets(0, 2, 0, 2);
        cGbc.gridy = 0;

        JButton minusButton = new JButton("-");
        minusButton.addActionListener(e -> {
            if (errorCounts[index] > 0) {
                errorCounts[index]--;
                errorCountLabels[index].setText(String.valueOf(errorCounts[index]));
            }
        });

        errorCountLabels[index] = new JLabel(String.valueOf(errorCounts[index]));
        errorCountLabels[index].setHorizontalAlignment(SwingConstants.CENTER);
        errorCountLabels[index].setPreferredSize(new Dimension(40, 22));

        JButton plusButton = new JButton("+");
        plusButton.addActionListener(e -> {
            errorCounts[index]++;
            errorCountLabels[index].setText(String.valueOf(errorCounts[index]));
        });

        cGbc.gridx = 0;
        cGbc.fill = GridBagConstraints.NONE;
        controlPanel.add(minusButton, cGbc);
        cGbc.gridx = 1;
        cGbc.weightx = 1.0;
        cGbc.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(errorCountLabels[index], cGbc);
        cGbc.gridx = 2;
        cGbc.weightx = 0;
        cGbc.fill = GridBagConstraints.NONE;
        controlPanel.add(plusButton, cGbc);

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

    /**
     * Fetch the authorized mapper usernames from the configured mapper list URL.
     * Expects the endpoint to return a JSON array of strings, e.g. ["user1","user2"]
     */
    private void fetchAuthorizedMappers() throws Exception {
        String defaultUrl = "https://script.google.com/macros/s/AKfycbytfxwDkZG1qDpgXV4QcplBgq4u9PVSW7yQzfe47UG4dmM_nh5-D6mb7LZ4vxib9KQp/exec";
        String urlStr = Preferences.main().get("dpw.mapper_list_url", defaultUrl);
        if (urlStr == null || urlStr.trim().isEmpty()) {
            throw new IllegalStateException("Mapper list URL is not configured (dpw.mapper_list_url)");
        }
        URL url = new URI(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int rc = conn.getResponseCode();
        if (rc < 200 || rc >= 300) {
            throw new IllegalStateException("Mapper list endpoint returned HTTP " + rc);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        String body = sb.toString().trim();
        // Very small JSON parser for an array of strings or a simple object containing Youth_Registry
        List<String> result = new ArrayList<>();
        if (body.startsWith("[") && body.endsWith("]")) {
            String inner = body.substring(1, body.length() - 1).trim();
            if (!inner.isEmpty()) {
                String[] parts = inner.split(",");
                for (String p : parts) {
                    String t = p.trim();
                    if (t.startsWith("\"") && t.endsWith("\"")) {
                        t = t.substring(1, t.length() - 1);
                    }
                    if (!t.isEmpty()) result.add(t);
                }
            }
        } else if (body.startsWith("{") && body.endsWith("}")) {
            // try to find a Youth_Registry array inside
            Pattern p = Pattern.compile("\"Youth_Registry\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
            Matcher m = p.matcher(body);
            if (m.find()) {
                String inner = m.group(1);
                String[] parts = inner.split(",");
                for (String part : parts) {
                    String t = part.trim();
                    if (t.startsWith("\"") && t.endsWith("\"")) t = t.substring(1, t.length() - 1);
                    if (!t.isEmpty()) result.add(t);
                }
            } else {
                // fallback: collect quoted strings (best-effort)
                Matcher m2 = Pattern.compile("\"([A-Za-z0-9_\\-\\.]+)\"").matcher(body);
                while (m2.find()) {
                    String t = m2.group(1);
                    if (!t.isEmpty()) result.add(t);
                }
            }
        } else {
            throw new IllegalStateException("Unexpected response from mapper list endpoint");
        }

        // persist the used URL so users don't have to set it manually
        try {
            Preferences.main().put("dpw.mapper_list_url", urlStr);
        } catch (Exception ignore) {
        }

        // replace authorizedMappers atomically
        synchronized (authorizedMappers) {
            authorizedMappers.clear();
            authorizedMappers.addAll(result);
        }
    }


    private void submitData(String validationStatus) {
        String taskId = taskIdField.getText();
        String mapperUsername = (String) mapperUsernameComboBox.getSelectedItem();
        if (mapperUsername == null) {
            mapperUsername = "";
        }
        // Client-side authorization check: ensure mapper is in authorized list
        synchronized (authorizedMappers) {
            if (!authorizedMappers.isEmpty() && !authorizedMappers.contains(mapperUsername)) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Error: The selected mapper is not a registered participant in this project. Please select a valid user or refresh the mapper list.",
                        "Unauthorized Mapper", JOptionPane.ERROR_MESSAGE));
                return;
            }
        }
        String totalBuildings = totalBuildingsField.getText();
        String validatorComments = validatorCommentsArea.getText();
        // Validator username taken from JOSM preferences (OSM server username)
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
            String baseUrl = "https://script.google.com/macros/s/AKfycbytfxwDkZG1qDpgXV4QcplBgq4u9PVSW7yQzfe47UG4dmM_nh5-D6mb7LZ4vxib9KQp/exec";
            Exception lastEx = null;
            int lastRc = -1;
            String lastResp = "";
            try {
                // First attempt: POST JSON with proper headers
                URL url = new URI(baseUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json, text/plain, */*");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                lastRc = conn.getResponseCode();
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        lastRc >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }
                lastResp = response.toString();

                if (lastRc == HttpURLConnection.HTTP_OK) {
                    final String resp = lastResp;
                    // Interpret response: prefer JSON or explicit success markers
                    String trimmed = resp == null ? "" : resp.trim();
                    boolean looksLikeJson = trimmed.startsWith("{") || trimmed.startsWith("[");
                    boolean containsOk = trimmed.toLowerCase().contains("ok") || trimmed.toLowerCase().contains("success");
                    boolean containsError = trimmed.toLowerCase().contains("error") || trimmed.toLowerCase().contains("exception") || trimmed.toLowerCase().contains("stacktrace");
                    boolean looksLikeHtml = trimmed.startsWith("<");
                    if (looksLikeHtml || containsError) {
                        // Treat as failure
                        lastResp = resp;
                        lastRc = HttpURLConnection.HTTP_INTERNAL_ERROR;
                    } else if (looksLikeJson || containsOk) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "Data submitted successfully!\nResponse: " + resp, "Success", JOptionPane.INFORMATION_MESSAGE));
                        return;
                    } else {
                        // Unknown plain-text response; show as success but warn
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "Submission sent — server returned non-JSON response:\n" + resp, "Submitted (uncertain)", JOptionPane.WARNING_MESSAGE));
                        return;
                    }
                }
            } catch (Exception e) {
                lastEx = e;
            }

            // Second attempt: Apps Script sometimes expects form-encoded 'payload' param
            try {
                String form = "payload=" + URLEncoder.encode(jsonData, StandardCharsets.UTF_8.name());
                URL url2 = new URI(baseUrl).toURL();
                HttpURLConnection conn2 = (HttpURLConnection) url2.openConnection();
                conn2.setRequestMethod("POST");
                conn2.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                conn2.setRequestProperty("Accept", "application/json, text/plain, */*");
                conn2.setDoOutput(true);
                conn2.setConnectTimeout(10000);
                conn2.setReadTimeout(15000);

                try (OutputStream os = conn2.getOutputStream()) {
                    byte[] input = form.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                lastRc = conn2.getResponseCode();
                StringBuilder response2 = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        lastRc >= 400 ? conn2.getErrorStream() : conn2.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response2.append(responseLine.trim());
                    }
                }
                lastResp = response2.toString();
                if (lastRc == HttpURLConnection.HTTP_OK) {
                    final String resp = lastResp;
                    String trimmed = resp == null ? "" : resp.trim();
                    boolean looksLikeJson = trimmed.startsWith("{") || trimmed.startsWith("[");
                    boolean containsOk = trimmed.toLowerCase().contains("ok") || trimmed.toLowerCase().contains("success");
                    boolean containsError = trimmed.toLowerCase().contains("error") || trimmed.toLowerCase().contains("exception") || trimmed.toLowerCase().contains("stacktrace");
                    boolean looksLikeHtml = trimmed.startsWith("<");
                    if (looksLikeHtml || containsError) {
                        lastResp = resp;
                        lastRc = HttpURLConnection.HTTP_INTERNAL_ERROR;
                    } else if (looksLikeJson || containsOk) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "Data submitted successfully!\nResponse: " + resp, "Success", JOptionPane.INFORMATION_MESSAGE));
                        return;
                    } else {
                        // If the response is of the form payload=..., attempt to decode and inspect
                        String lower = resp == null ? "" : resp;
                        int idx = lower.indexOf("payload=");
                        if (idx >= 0) {
                            String suffix = resp.substring(idx + "payload=".length());
                            try {
                                String decoded = java.net.URLDecoder.decode(suffix, StandardCharsets.UTF_8.name());
                                String dtrim = decoded.trim();
                                if (dtrim.startsWith("{") || dtrim.startsWith("[") || dtrim.toLowerCase().contains("ok")) {
                                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "Data submitted successfully!\nResponse: " + decoded, "Success", JOptionPane.INFORMATION_MESSAGE));
                                    return;
                                } else {
                                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "Submission sent — server returned non-JSON payload:\n" + decoded, "Submitted (uncertain)", JOptionPane.WARNING_MESSAGE));
                                    return;
                                }
                            } catch (Exception e) {
                                // fall through to failure
                                lastResp = resp;
                                lastRc = HttpURLConnection.HTTP_INTERNAL_ERROR;
                            }
                        } else {
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "Submission sent — server returned non-JSON response:\n" + resp, "Submitted (uncertain)", JOptionPane.WARNING_MESSAGE));
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                lastEx = e;
            }

            // Third attempt: GET fallback with payload param
            try {
                String qs = "?payload=" + URLEncoder.encode(jsonData, StandardCharsets.UTF_8.name());
                URL url3 = new URI(baseUrl + qs).toURL();
                HttpURLConnection conn3 = (HttpURLConnection) url3.openConnection();
                conn3.setRequestMethod("GET");
                conn3.setRequestProperty("Accept", "application/json, text/plain, */*");
                conn3.setConnectTimeout(10000);
                conn3.setReadTimeout(15000);

                lastRc = conn3.getResponseCode();
                StringBuilder response3 = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        lastRc >= 400 ? conn3.getErrorStream() : conn3.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response3.append(responseLine.trim());
                    }
                }
                lastResp = response3.toString();
                if (lastRc == HttpURLConnection.HTTP_OK) {
                    final String resp = lastResp;
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "Data submitted successfully!\nResponse: " + resp, "Success", JOptionPane.INFORMATION_MESSAGE));
                    return;
                }
            } catch (Exception e) {
                lastEx = e;
            }

            final int rcFinal = lastRc;
            final String respFinal = lastResp;
            final Exception exFinal = lastEx;
            SwingUtilities.invokeLater(() -> {
                if (exFinal != null) {
                    Logging.error(exFinal);
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "An error occurred during submission: " + exFinal.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "Data submission failed. Code: " + rcFinal + "\nResponse: " + respFinal, "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

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