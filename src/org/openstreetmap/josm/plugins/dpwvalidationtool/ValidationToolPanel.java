package org.openstreetmap.josm.plugins.dpwvalidationtool;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ValidationToolPanel extends ToggleDialog {

    private JTextField taskIdField;
    private JTextField mapperUsernameField;
    private JTextField totalBuildingsField;
    private JTextArea validatorCommentsArea;

    private int errorHangingNodes = 0;
    private int errorOverlappingBuildings = 0;
    private int errorBuildingsCrossingHighway = 0;
    private int errorMissingTags = 0;
    private int errorImproperTags = 0;
    private int errorFeaturesMisidentified = 0;
    private int errorMissingBuildings = 0;
    private int errorBuildingInsideBuilding = 0;
    private int errorBuildingCrossingResidential = 0;
    private int errorImproperlyDrawn = 0;

    private JLabel hangingNodesCountLabel;
    private JLabel overlappingBuildingsCountLabel;
    private JLabel buildingsCrossingHighwayCountLabel;
    private JLabel missingTagsCountLabel;
    private JLabel improperTagsCountLabel;
    private JLabel featuresMisidentifiedCountLabel;
    private JLabel missingBuildingsCountLabel;
    private JLabel buildingInsideBuildingCountLabel;
    private JLabel buildingCrossingResidentialCountLabel;
    private JLabel improperlyDrawnCountLabel;

    public ValidationToolPanel() {
        // Use a known JOSM dialog icon to avoid missing resource errors.
        // 'validator' maps to images/dialogs/validator.svg in JOSM resources.
        super(I18n.tr("DPW Validation Tool"), "validator", I18n.tr("Open DPW Validation Tool"), null, 150);
        try {
            Logging.info("DPWValidationTool: constructing ValidationToolPanel");
            setupUI();
            updatePanelData();
            Logging.info("DPWValidationTool: ValidationToolPanel constructed");
        } catch (Throwable t) {
            Logging.error(t);
        }
    }

    private void setupUI() {
    JPanel panel = new JPanel();
    // Constrain preferred size so the dialog doesn't expand too wide
    panel.setPreferredSize(new Dimension(420, 600));
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

    JLabel taskIdLabel = new JLabel("Task ID:");
    taskIdField = new JTextField();
        JLabel mapperUsernameLabel = new JLabel("Mapper Username:");
        mapperUsernameField = new JTextField();
        mapperUsernameField.setEditable(false);
        JLabel totalBuildingsLabel = new JLabel("Total Buildings:");
        totalBuildingsField = new JTextField();
        totalBuildingsField.setEditable(false);

    // Tasking Manager helpers: base URL and fetch button
    JLabel tmBaseLabel = new JLabel("TM Base URL:");
    JTextField tmBaseField = new JTextField(20);
    tmBaseField.setText(Preferences.main().get("dpw.tm.baseurl", "https://tasks.hotosm.org"));
    JButton fetchTmButton = new JButton("Fetch mapper from TM");

        JSeparator separator = new JSeparator();

        JLabel validatorCommentsLabel = new JLabel("Validator Comments:");
        validatorCommentsArea = new JTextArea(5, 20);
        JScrollPane commentsScrollPane = new JScrollPane(validatorCommentsArea);

    final JButton validateButton = new JButton("Validate (Accept)");
    final JButton invalidateButton = new JButton("Invalidate (Reject)");
    final JButton refreshButton = new JButton("Refresh");

        // Error rows
        hangingNodesCountLabel = new JLabel("0");
        overlappingBuildingsCountLabel = new JLabel("0");
        buildingsCrossingHighwayCountLabel = new JLabel("0");
        missingTagsCountLabel = new JLabel("0");
        improperTagsCountLabel = new JLabel("0");
        featuresMisidentifiedCountLabel = new JLabel("0");
        missingBuildingsCountLabel = new JLabel("0");
        buildingInsideBuildingCountLabel = new JLabel("0");
        buildingCrossingResidentialCountLabel = new JLabel("0");
        improperlyDrawnCountLabel = new JLabel("0");

        JPanel errorPanel = new JPanel(new GridLayout(10, 3, 5, 5));
        errorPanel.add(new JLabel("Hanging Nodes:"));
        errorPanel.add(createPlusButton(e -> {
            errorHangingNodes++;
            hangingNodesCountLabel.setText(String.valueOf(errorHangingNodes));
        }));
        errorPanel.add(hangingNodesCountLabel);

        errorPanel.add(new JLabel("Overlapping Buildings:"));
        errorPanel.add(createPlusButton(e -> {
            errorOverlappingBuildings++;
            overlappingBuildingsCountLabel.setText(String.valueOf(errorOverlappingBuildings));
        }));
        errorPanel.add(overlappingBuildingsCountLabel);

        errorPanel.add(new JLabel("Buildings Crossing Highway:"));
        errorPanel.add(createPlusButton(e -> {
            errorBuildingsCrossingHighway++;
            buildingsCrossingHighwayCountLabel.setText(String.valueOf(errorBuildingsCrossingHighway));
        }));
        errorPanel.add(buildingsCrossingHighwayCountLabel);

        errorPanel.add(new JLabel("Missing Tags:"));
        errorPanel.add(createPlusButton(e -> {
            errorMissingTags++;
            missingTagsCountLabel.setText(String.valueOf(errorMissingTags));
        }));
        errorPanel.add(missingTagsCountLabel);

        errorPanel.add(new JLabel("Improper Tags:"));
        errorPanel.add(createPlusButton(e -> {
            errorImproperTags++;
            improperTagsCountLabel.setText(String.valueOf(errorImproperTags));
        }));
        errorPanel.add(improperTagsCountLabel);

        errorPanel.add(new JLabel("Features Misidentified:"));
        errorPanel.add(createPlusButton(e -> {
            errorFeaturesMisidentified++;
            featuresMisidentifiedCountLabel.setText(String.valueOf(errorFeaturesMisidentified));
        }));
        errorPanel.add(featuresMisidentifiedCountLabel);

        errorPanel.add(new JLabel("Missing Buildings:"));
        errorPanel.add(createPlusButton(e -> {
            errorMissingBuildings++;
            missingBuildingsCountLabel.setText(String.valueOf(errorMissingBuildings));
        }));
        errorPanel.add(missingBuildingsCountLabel);

        errorPanel.add(new JLabel("Building Inside Building:"));
        errorPanel.add(createPlusButton(e -> {
            errorBuildingInsideBuilding++;
            buildingInsideBuildingCountLabel.setText(String.valueOf(errorBuildingInsideBuilding));
        }));
        errorPanel.add(buildingInsideBuildingCountLabel);

        errorPanel.add(new JLabel("Building Crossing Residential:"));
        errorPanel.add(createPlusButton(e -> {
            errorBuildingCrossingResidential++;
            buildingCrossingResidentialCountLabel.setText(String.valueOf(errorBuildingCrossingResidential));
        }));
        errorPanel.add(buildingCrossingResidentialCountLabel);

        errorPanel.add(new JLabel("Improperly Drawn:"));
        errorPanel.add(createPlusButton(e -> {
            errorImproperlyDrawn++;
            improperlyDrawnCountLabel.setText(String.valueOf(errorImproperlyDrawn));
        }));
        errorPanel.add(improperlyDrawnCountLabel);


    GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();
    hGroup.addGroup(layout.createParallelGroup()
        .addComponent(taskIdLabel)
        .addComponent(mapperUsernameLabel)
        .addComponent(totalBuildingsLabel)
        .addComponent(separator)
        .addComponent(errorPanel)
        .addComponent(validatorCommentsLabel)
        .addComponent(commentsScrollPane)
        .addComponent(tmBaseLabel)
        .addGroup(layout.createSequentialGroup()
            .addComponent(validateButton)
            .addComponent(invalidateButton)
            .addComponent(refreshButton))
    );
    hGroup.addGroup(layout.createParallelGroup()
        .addComponent(taskIdField)
        .addComponent(mapperUsernameField)
        .addComponent(totalBuildingsField)
        .addGroup(layout.createSequentialGroup()
            .addComponent(tmBaseField)
            .addComponent(fetchTmButton))
    );
    layout.setHorizontalGroup(hGroup);

    GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();
    vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        .addComponent(taskIdLabel)
        .addComponent(taskIdField));
    vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        .addComponent(mapperUsernameLabel)
        .addComponent(mapperUsernameField));
    vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        .addComponent(totalBuildingsLabel)
        .addComponent(totalBuildingsField));
    vGroup.addComponent(separator);
    vGroup.addComponent(errorPanel);
    vGroup.addComponent(validatorCommentsLabel);
    vGroup.addComponent(commentsScrollPane);
    vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        .addComponent(tmBaseLabel)
        .addComponent(tmBaseField)
        .addComponent(fetchTmButton));
    vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        .addComponent(validateButton)
        .addComponent(invalidateButton)
        .addComponent(refreshButton));
    layout.setVerticalGroup(vGroup);

        createLayout(panel, false, null);

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

        fetchTmButton.addActionListener(e -> {
            String tmBase = tmBaseField.getText().trim();
            String taskInput = taskIdField.getText().trim();
            if (taskInput.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a Task ID or Tasking Manager task URL first.", "Missing Task ID", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // do network fetch off EDT
            new Thread(() -> {
                try {
                    String username = fetchMapperFromTaskingManager(tmBase, taskInput);
                    if (username != null && !username.isEmpty()) {
                        SwingUtilities.invokeLater(() -> mapperUsernameField.setText(username));
                        Preferences.main().put("dpw.tm.baseurl", tmBase);
                    } else {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Mapper not found in TM response.", "TM Lookup", JOptionPane.WARNING_MESSAGE));
                    }
                } catch (Exception ex) {
                    Logging.error(ex);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error fetching from TM: " + ex.getMessage(), "TM Lookup Error", JOptionPane.ERROR_MESSAGE));
                }
            }).start();
        });

        // Disable submit buttons until Task ID is provided
        validateButton.setEnabled(false);
        invalidateButton.setEnabled(false);
        taskIdField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                String t = taskIdField.getText();
                boolean ok = t != null && !t.trim().isEmpty();
                validateButton.setEnabled(ok);
                invalidateButton.setEnabled(ok);
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });
    }

    private JButton createPlusButton(ActionListener listener) {
        JButton button = new JButton("+");
        button.addActionListener(listener);
        return button;
    }

    public void updatePanelData() {
        DataSet dataSet = MainApplication.getLayerManager().getEditDataSet();
        if (dataSet == null) {
            totalBuildingsField.setText("0");
            mapperUsernameField.setText("");
            return;
        }

        int buildingCount = 0;
        Map<String, Integer> userCounts = new HashMap<>();
        for (OsmPrimitive primitive : dataSet.getPrimitives(osmPrimitive -> true)) {
            if (primitive.hasKey("building")) {
                buildingCount++;
            }
            User user = primitive.getUser();
            if (user != null) {
                userCounts.put(user.getName(), userCounts.getOrDefault(user.getName(), 0) + 1);
            }
        }
        totalBuildingsField.setText(String.valueOf(buildingCount));

        String mostFrequentUser = "";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : userCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostFrequentUser = entry.getKey();
            }
        }
        mapperUsernameField.setText(mostFrequentUser);
    }

    private void submitData(String validationStatus) {
        String taskId = taskIdField.getText();
        String mapperUsername = mapperUsernameField.getText();
        String totalBuildings = totalBuildingsField.getText();
        String validatorComments = validatorCommentsArea.getText();
        String validatorUsername = Preferences.main().get("osm-server.username", "unknown");
        String settlement = Preferences.main().get("dpw.settlement", "Mji wa Huruma");

        int totalBuildingsInt = 0;
        try {
            totalBuildingsInt = Integer.parseInt(totalBuildings.trim());
        } catch (Exception e) {
            totalBuildingsInt = 0;
        }

        String json = "{" +
                "\"task_id\": \"" + jsonEscape(taskId) + "\"," +
                "\"settlement\": \"" + jsonEscape(settlement) + "\"," +
                "\"mapper_username\": \"" + jsonEscape(mapperUsername) + "\"," +
                "\"validator_username\": \"" + jsonEscape(validatorUsername) + "\"," +
                "\"total_buildings\": " + totalBuildingsInt + "," +
                "\"error_hanging_nodes\": " + errorHangingNodes + "," +
                "\"error_overlapping_buildings\": " + errorOverlappingBuildings + "," +
                "\"error_buildings_crossing_highway\": " + errorBuildingsCrossingHighway + "," +
                "\"error_missing_tags\": " + errorMissingTags + "," +
                "\"error_improper_tags\": " + errorImproperTags + "," +
                "\"error_features_misidentified\": " + errorFeaturesMisidentified + "," +
                "\"error_missing_buildings\": " + errorMissingBuildings + "," +
                "\"error_building_inside_building\": " + errorBuildingInsideBuilding + "," +
                "\"error_building_crossing_residential\": " + errorBuildingCrossingResidential + "," +
                "\"error_improperly_drawn\": " + errorImproperlyDrawn + "," +
                "\"validation_status\": \"" + jsonEscape(validationStatus) + "\"," +
                "\"validator_comments\": \"" + jsonEscape(validatorComments) + "\"" +
                "}";

        sendPostRequest(json);
    }

    private void sendPostRequest(String jsonData) {
        new Thread(() -> {
            int attempts = 0;
            int maxAttempts = 3;
            Exception lastEx = null;
            while (attempts < maxAttempts) {
                attempts++;
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
                        if (br != null) {
                            String responseLine;
                            while ((responseLine = br.readLine()) != null) {
                                response.append(responseLine.trim());
                            }
                        }
                    }

                    final int rc = responseCode;
                    final String respBody = response.toString();
                    SwingUtilities.invokeLater(() -> {
                        if (rc == HttpURLConnection.HTTP_OK) {
                            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "Data submitted successfully!\nResponse: " + respBody, "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "Data submission failed. Response code: " + rc + "\nResponse: " + respBody, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                    return; // success or server returned a response, exit retry loop

                } catch (Exception e) {
                    lastEx = e;
                    try {
                        Thread.sleep(1000 * attempts);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            final Exception finalEx = lastEx;
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "An error occurred during submission after retries: " + (finalEx != null ? finalEx.getMessage() : "unknown"), "Error", JOptionPane.ERROR_MESSAGE);
            });
        }).start();
    }

    /**
     * Attempt to fetch the mapper username from a Tasking Manager instance.
     * Accepts either a numeric task id or a full TM task URL. Tries common
     * endpoint patterns used by HOT/Tasking Manager instances.
     * Returns the username or null if not found.
     */
    private String fetchMapperFromTaskingManager(String tmBaseUrl, String taskIdOrUrl) throws Exception {
        // Normalize base URL
        if (tmBaseUrl == null || tmBaseUrl.trim().isEmpty()) {
            tmBaseUrl = "https://tasks.hotosm.org";
        }
        tmBaseUrl = tmBaseUrl.trim();
        if (tmBaseUrl.endsWith("/")) tmBaseUrl = tmBaseUrl.substring(0, tmBaseUrl.length() - 1);

        // Extract numeric task id if user pasted a full URL
        String taskId = taskIdOrUrl.trim();
        Pattern idPattern = Pattern.compile("(\\d+)");
        Matcher m = idPattern.matcher(taskId);
        if (taskId.contains("/")) {
            // try to pull last number-looking token
            List<String> parts = Arrays.asList(taskId.split("/"));
            for (int i = parts.size() - 1; i >= 0; i--) {
                Matcher mm = idPattern.matcher(parts.get(i));
                if (mm.find()) { taskId = mm.group(1); break; }
            }
        } else if (m.find()) {
            taskId = m.group(1);
        }

        // Common candidate endpoints to try
        String[] endpoints = new String[] {
                tmBaseUrl + "/api/v1/tasks/" + taskId,
                tmBaseUrl + "/api/v2/tasks/" + taskId,
                tmBaseUrl + "/tasks/api/v1/tasks/" + taskId,
                tmBaseUrl + "/projects/" + taskId
        };

        Exception lastEx = null;
        for (String ep : endpoints) {
            try {
                Logging.info("DPWValidationTool: trying TM endpoint " + ep);
                URL url = new URL(ep);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int rc = conn.getResponseCode();
                if (rc >= 200 && rc < 300) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                    }
                    String body = sb.toString();
                    Logging.info("DPWValidationTool: TM response: " + (body.length() > 200 ? body.substring(0,200) + "..." : body));
                    // Heuristically look for a username in common JSON shapes
                    // Try fields like owner, created_by, last_modified_by, tasks, task, etc.
                    // This is a heuristic simple parser (no JSON library to avoid deps).
                    String username = tryExtractUsernameFromJson(body);
                    if (username != null && !username.isEmpty()) return username;
                }
            } catch (Exception e) {
                lastEx = e;
            }
        }
        if (lastEx != null) throw lastEx;
        return null;
    }

    private String tryExtractUsernameFromJson(String json) {
        if (json == null) return null;
        // simple patterns: "username":"..." or "name":"..." near owner/created_by
    Pattern[] patterns = new Pattern[] {
        Pattern.compile("\\\"username\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\\"user_name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\\"created_by\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\\"owner\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE)
    };
        for (Pattern p : patterns) {
            Matcher m = p.matcher(json);
            if (m.find()) return m.group(1);
        }
        return null;
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
