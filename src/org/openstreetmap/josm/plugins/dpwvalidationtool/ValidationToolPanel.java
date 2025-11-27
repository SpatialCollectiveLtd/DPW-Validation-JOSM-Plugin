package org.openstreetmap.josm.plugins.dpwvalidationtool;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.io.OsmWriterFactory;
import org.openstreetmap.josm.io.OsmWriter;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
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

    /**
     * Validation workflow states
     */
    private enum ValidationState {
        IDLE,           // Initial state, no layer isolated
        ISOLATED,       // Layer isolated, ready for validation
        SUBMITTED,      // Validation submitted to API
        EXPORTED        // Data exported, ready for restart
    }

    /**
     * Simple data class to store user information from DPW API
     */
    private static class UserInfo {
        String osmUsername;
        String settlement;
        
        UserInfo(String osmUsername, String settlement) {
            this.osmUsername = osmUsername;
            this.settlement = settlement != null ? settlement : "";
        }
    }

    private JTextField taskIdField;
    private JTextField tmUrlField; // v3.1.0-BETA: TM URL input
    private JTextField settlementField;
    private JComboBox<String> mapperUsernameComboBox;
    private JTextField totalBuildingsField;
    private JTextArea validatorCommentsArea;
    private List<String> authorizedMappers = new ArrayList<>();
    private Map<String, String> mapperSettlements = new HashMap<>(); // username -> settlement mapping
    private JLabel authStatusLabel;
    private JLabel fetchStatusLabel;
    private JButton validateButton;
    private JButton invalidateButton;
    private JButton refreshMapperListButton;
    private javax.swing.JComponent datePickerComponent;
    private JButton isolateButton;
    private volatile boolean isSending = false;
    private volatile boolean isFetchingMappers = false;
    private volatile long lastMapperFetchTime = 0;
    private static final long MAPPER_FETCH_COOLDOWN = 10000; // 10 seconds between fetches to prevent 429 errors
    private JDialog sendingDialog;
    private boolean submittedThisSession = false;
    
    // v3.0 - Workflow state management
    private ValidationState currentState = ValidationState.IDLE;
    private OsmDataLayer isolatedLayer = null;
    private String lastValidationStatus = null; // "Validated" or "Rejected"
    
    // v3.0.1 - Cloud upload integration
    private int lastValidationLogId = -1;
    private int mapperUserId = -1;
    private int validatorUserId = -1;
    private String googleDriveFileUrl = null;
    
    // v3.0 - Validation Preview Panel
    private JPanel validationPreviewPanel;
    private JTextArea previewTextArea;
    private boolean previewExpanded = false;

    private final String[] errorTypes = {
        "Hanging Nodes", "Overlapping Buildings", "Buildings Crossing Highway",
        "Missing Tags", "Improper Tags", "Features Misidentified",
        "Missing Buildings", "Building Inside Building", "Building Crossing Residential",
        "Improperly Drawn"
    };
    private int[] errorCounts = new int[errorTypes.length];
    private JLabel[] errorCountLabels = new JLabel[errorTypes.length];
    
    public ValidationToolPanel() {
        super(I18n.tr("DPW Validation Tool v3.1.0-BETA"), "validator", I18n.tr("Open DPW Validation Tool"), null, 150);
        try {
            Logging.info("DPWValidationTool: constructing ValidationToolPanel v3.1.0-BETA");
            setupUI();
            updatePanelData();
            
            // v3.1.0-BETA: Setup remote control detection listener
            if (PluginSettings.isTMIntegrationEnabled() && 
                PluginSettings.isRemoteControlDetectionEnabled()) {
                MainApplication.getLayerManager().addActiveLayerChangeListener(e -> {
                    checkRemoteControlForTMTask();
                });
            }
            
            // Kick off an initial authorized-mapper fetch in background with rate limiting
            new Thread(() -> {
                try {
                    setFetchingMappers(true);
                    fetchAuthorizedMappers();
                    SwingUtilities.invokeLater(() -> updateAuthStatus());
                } catch (Exception e) {
                    Logging.info("DPWValidationTool: initial mapper fetch failed: " + e.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        fetchStatusLabel.setText("Error: Failed to fetch mapper list - " + e.getMessage());
                        fetchStatusLabel.setBackground(new Color(0xffcccc));
                        updateSubmitButtonsEnabled();
                    });
                } finally {
                    setFetchingMappers(false);
                }
            }).start();
            Logging.info("DPWValidationTool: ValidationToolPanel v3.1.0-BETA constructed");
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

        // v3.1.0-BETA: TM URL field (optional, only shown if TM integration enabled)
        if (PluginSettings.isTMIntegrationEnabled()) {
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            JLabel tmLabel = new JLabel("<html><b>TM URL (BETA):</b></html>");
            tmLabel.setToolTipText("Optional: Tasking Manager task URL for auto-detection");
            panel.add(tmLabel, gbc);

            gbc.gridx = 1;
            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            tmUrlField = new JTextField();
            tmUrlField.setToolTipText("<html>Example: https://tasks.hotosm.org/projects/27396/tasks/123<br>Leave blank to use remote control auto-detection</html>");
            panel.add(tmUrlField, gbc);
            
            gbc.gridy++;
        }

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

        // Settlement (optional field)
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Settlement:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        settlementField = new JTextField();
        settlementField.setToolTipText("Auto-filled from DPW system based on selected mapper");
        settlementField.setEditable(false); // Make read-only, auto-filled from API
        settlementField.setBackground(new Color(240, 240, 240)); // Gray background to indicate read-only
        panel.add(settlementField, gbc);

    // NOTE: the mapper-list refresh button will be placed beside the mapper combo (small icon)

        // Mapper Username
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Mapper Username:"), gbc);

    // place mapper combo and refresh button on top row
    gbc.gridx = 1;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    JPanel mapperPanel = new JPanel(new GridBagLayout());
    GridBagConstraints mpGbc = new GridBagConstraints();
    mpGbc.insets = new Insets(0, 0, 0, 4);
    mpGbc.gridy = 0;

    // Combo expands
    mpGbc.gridx = 0;
    mpGbc.weightx = 1.0;
    mpGbc.fill = GridBagConstraints.HORIZONTAL;
    mapperUsernameComboBox = new JComboBox<>();
    mapperUsernameComboBox.setPreferredSize(new Dimension(220, 24));
    mapperPanel.add(mapperUsernameComboBox, mpGbc);

    // small refresh button stays compact
    mpGbc.gridx = 1;
    mpGbc.weightx = 0;
    mpGbc.fill = GridBagConstraints.NONE;
    refreshMapperListButton = new JButton("\u21bb");
    refreshMapperListButton.setMargin(new Insets(2,2,2,2));
    refreshMapperListButton.setPreferredSize(new Dimension(26, 22));
    refreshMapperListButton.setToolTipText("Refresh authorized mapper list");
    mapperPanel.add(refreshMapperListButton, mpGbc);
    
    panel.add(mapperPanel, gbc);
    
    // Add Date and Isolate controls on a new row under the mapper combo
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0;
    panel.add(new JLabel("Filter Date:"), gbc);
    
    // Date controls in a separate panel
    JPanel dateIsolatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    
    // instantiate a JDatePicker if the library is available in lib/, otherwise fall back to JTextField
    try {
        org.jdatepicker.impl.SqlDateModel model = new org.jdatepicker.impl.SqlDateModel();
        java.util.Properties p = new java.util.Properties();
        org.jdatepicker.impl.JDatePanelImpl datePanel = new org.jdatepicker.impl.JDatePanelImpl(model, p);
        org.jdatepicker.impl.JDatePickerImpl picker = new org.jdatepicker.impl.JDatePickerImpl(datePanel, new org.jdatepicker.impl.DateComponentFormatter());
        datePickerComponent = picker;
    } catch (Throwable t) {
        datePickerComponent = new JTextField(10);
    }
    
    // keep date picker compact but slightly wider for better usability
    if (datePickerComponent instanceof JComponent) {
        ((JComponent) datePickerComponent).setPreferredSize(new Dimension(130, 24));
    }
    dateIsolatePanel.add(datePickerComponent);
    
    // isolate button right next to date picker
    isolateButton = new JButton("Isolate");
    isolateButton.setToolTipText("Isolate work for selected mapper and date");
    isolateButton.setPreferredSize(new Dimension(100, 24));
    dateIsolatePanel.add(isolateButton);
    
    gbc.gridx = 1;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    panel.add(dateIsolatePanel, gbc);

        // wire refresh mapper list button
        refreshMapperListButton.addActionListener(e -> {
            setFetchingMappers(true);
            new Thread(() -> {
                try {
                    fetchAuthorizedMappers();
                    SwingUtilities.invokeLater(() -> {
                        updateAuthStatus();
                        fetchStatusLabel.setText("Success: User list updated. Ready for validation.");
                        fetchStatusLabel.setBackground(new Color(0x88ff88));
                        updateSubmitButtonsEnabled();
                        JOptionPane.showMessageDialog(null, "Authorized mapper list refreshed (" + authorizedMappers.size() + ")", "Mapper List", JOptionPane.INFORMATION_MESSAGE);
                    });
                } catch (Exception ex) {
                    Logging.error(ex);
                    SwingUtilities.invokeLater(() -> {
                        fetchStatusLabel.setText("Error: Failed to fetch mapper list - " + ex.getMessage());
                        fetchStatusLabel.setBackground(new Color(0xffcccc));
                        updateSubmitButtonsEnabled();
                        JOptionPane.showMessageDialog(null, "Failed to fetch mapper list: " + ex.getMessage(), "Mapper List Error", JOptionPane.ERROR_MESSAGE);
                    });
                } finally {
                    setFetchingMappers(false);
                }
            }).start();
        });

        // Total Buildings - note: after Date and Isolate row
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

    // Fetch status label + Authorization status label + Force Submit
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    fetchStatusLabel = new JLabel("User list: unknown");
    fetchStatusLabel.setOpaque(true);
    fetchStatusLabel.setBackground(Color.YELLOW);
    panel.add(fetchStatusLabel, gbc);

    gbc.gridy++;
    authStatusLabel = new JLabel("Mapper authorization: unknown");
    authStatusLabel.setOpaque(true);
    authStatusLabel.setBackground(Color.LIGHT_GRAY);
    panel.add(authStatusLabel, gbc);

        // v3.0 - Validation Preview Panel (collapsible)
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        validationPreviewPanel = new JPanel(new BorderLayout());
        validationPreviewPanel.setBorder(BorderFactory.createTitledBorder("📊 Validation Summary"));
        validationPreviewPanel.setVisible(false); // Hidden by default
        
        previewTextArea = new JTextArea(8, 40);
        previewTextArea.setEditable(false);
        previewTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        previewTextArea.setBackground(new Color(245, 245, 245));
        JScrollPane previewScroll = new JScrollPane(previewTextArea);
        
        JButton togglePreviewButton = new JButton("▼ Show Summary");
        togglePreviewButton.addActionListener(e -> {
            previewExpanded = !previewExpanded;
            if (previewExpanded) {
                updateValidationPreview();
                previewScroll.setVisible(true);
                togglePreviewButton.setText("▲ Hide Summary");
            } else {
                previewScroll.setVisible(false);
                togglePreviewButton.setText("▼ Show Summary");
            }
            validationPreviewPanel.revalidate();
            validationPreviewPanel.repaint();
        });
        
        validationPreviewPanel.add(togglePreviewButton, BorderLayout.NORTH);
        validationPreviewPanel.add(previewScroll, BorderLayout.CENTER);
        previewScroll.setVisible(false);
        
        panel.add(validationPreviewPanel, gbc);

        // Action Buttons - simplified workflow
    validateButton = new JButton("Accept");
    validateButton.setToolTipText("Mark this task as validated (accept)");
    invalidateButton = new JButton("Reject");
    invalidateButton.setToolTipText("Mark this task as rejected (invalidate)");

    // Use a compact FlowLayout so buttons don't expand when the panel is narrowed
    JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    actionPanel.add(validateButton);
    actionPanel.add(invalidateButton);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(actionPanel, gbc);

        createLayout(panel, false, null);

        // Action Listeners
        validateButton.addActionListener(e -> {
            // v3.0 - Show enhanced confirmation dialog before accepting
            if (showConfirmationDialog("Validated")) {
                submitData("Validated");
            }
        });
        invalidateButton.addActionListener(e -> {
            // v3.0 - Show enhanced confirmation dialog before rejecting
            if (showConfirmationDialog("Rejected")) {
                submitData("Rejected");
            }
        });

        // Isolate action listener
        isolateButton.addActionListener(e -> {
            isolateButton.setEnabled(false);
            new Thread(() -> {
                try {
                    // CRITICAL: Date validation - must be set before isolation
                    String dateString = getDateStringFromPicker();
                    if (dateString == null || dateString.isEmpty() || dateString.equals("YYYY-MM-DD")) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, 
                            "Please select a date before isolating work.\n\n" +
                            "Date selection is mandatory to ensure proper data filtering.",
                            "Date Required", 
                            JOptionPane.ERROR_MESSAGE));
                        return;
                    }
                    
                    // CRITICAL: Authorization check - current user must be authorized
                    String currentValidator = getCurrentValidator();
                    if (currentValidator == null || currentValidator.isEmpty()) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                            "Cannot isolate work: You are not authenticated with OpenStreetMap.\n\n" +
                            "Please authenticate with OSM in JOSM:\n" +
                            "1. Go to Edit → Preferences → Connection Settings\n" +
                            "2. Click 'Authorize now' to authenticate with OSM\n" +
                            "3. Complete the OAuth authorization process",
                            "Authentication Required",
                            JOptionPane.ERROR_MESSAGE));
                        return;
                    }
                    
                    // Check if current user is authorized for this project (case-insensitive)
                    synchronized (authorizedMappers) {
                        if (!authorizedMappers.isEmpty()) {
                            final String validatorToCheck = currentValidator;
                            boolean isAuthorized = authorizedMappers.stream()
                                .anyMatch(user -> user.equalsIgnoreCase(validatorToCheck));
                            
                            if (!isAuthorized) {
                                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                                    "Access Denied: You are not authorized for this project.\n\n" +
                                    "Current user: " + validatorToCheck + "\n" +
                                    "Authorized mappers: " + authorizedMappers.size() + " users\n\n" +
                                    "Please contact your project manager to request access.\n\n" +
                                    "Note: Only authorized project members can isolate and validate data.",
                                    "Authorization Required",
                                    JOptionPane.ERROR_MESSAGE));
                                return;
                            }
                        }
                    }
                    
                    String mapper = (String) mapperUsernameComboBox.getSelectedItem();
                    if (mapper == null) mapper = "";
                    String search = "user:'" + mapper + "' timestamp:" + dateString;
                    // create and apply search to current active layer
                    DataSet editDataSet = MainApplication.getLayerManager().getEditDataSet();
                    if (editDataSet == null) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "No active editing layer found.", "No Layer", JOptionPane.ERROR_MESSAGE));
                        return;
                    }
                    SearchCompiler.Match match;
                    try {
                        match = SearchCompiler.compile(search);
                    } catch (SearchParseError spe) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Invalid search expression: " + spe.getMessage(), "Search Error", JOptionPane.ERROR_MESSAGE));
                        return;
                    }
                    // collect building primitives authored by the selected mapper (and matching date where possible)
                    Set<OsmPrimitive> selected = new HashSet<>();
                    for (OsmPrimitive p : editDataSet.getPrimitives(pr -> pr.hasKey("building"))) {
                        try {
                            User u = p.getUser();
                            if (u == null) continue;
                            if (!u.getName().equals(mapper)) continue;
                            
                            // Direct timestamp comparison using Java time API
                            if (dateString != null && !dateString.isEmpty()) {
                                // Skip if primitive has no timestamp
                                if (p.isTimestampEmpty()) {
                                    Logging.debug("DPWValidationTool: primitive " + p.getId() + " has no timestamp, skipping");
                                    continue;
                                }
                                
                                try {
                                    // Parse the selected date (YYYY-MM-DD)
                                    java.time.LocalDate filterDate = java.time.LocalDate.parse(dateString);
                                    
                                    // Get primitive's timestamp as Instant
                                    java.time.Instant timestamp = p.getInstant();
                                    if (timestamp == null) {
                                        Logging.debug("DPWValidationTool: primitive " + p.getId() + " has null instant timestamp");
                                        continue;
                                    }
                                    
                                    // Convert to LocalDate for date-only comparison (ignore time)
                                    java.time.LocalDate primitiveDate = timestamp.atZone(java.time.ZoneOffset.UTC).toLocalDate();
                                    
                                    // Compare dates (ignoring time portion)
                                    if (!primitiveDate.equals(filterDate)) {
                                        Logging.debug("DPWValidationTool: primitive " + p.getId() + " date " + primitiveDate + 
                                                    " doesn't match filter " + filterDate);
                                        continue; // Skip if dates don't match
                                    }
                                    
                                    Logging.debug("DPWValidationTool: primitive " + p.getId() + " passed date filter: " + primitiveDate);
                                } catch (Exception ex) {
                                    Logging.warn("DPWValidationTool: error checking timestamp for primitive " + p.getId() + 
                                               ": " + ex.getMessage());
                                    
                                    // Fall back to SearchCompiler as a backup method
                                    try {
                                        if (!p.evaluateCondition(match)) {
                                            continue; // SearchCompiler says no match
                                        }
                                    } catch (Exception searchEx) {
                                        // Both timestamp methods failed - continue with next primitive
                                        Logging.warn("DPWValidationTool: search filter also failed for primitive " + p.getId());
                                        continue;
                                    }
                                }
                            }
                            
                            // If we get here, the primitive passed both mapper and date filters
                            selected.add(p);
                        } catch (Exception ignore) {
                        }
                    }
                    if (selected.isEmpty()) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "No building objects found for selected mapper.", "No Matches", JOptionPane.INFORMATION_MESSAGE));
                        return;
                    }
                    // create new dataset and clone primitives into it to avoid sharing primitives
                    Logging.info("DPWValidationTool: preparing to create isolated layer for mapper='" + mapper + "', date='" + dateString + "', selectedCount=" + selected.size());
                    DataSet newDs = new DataSet();
                    // split selected primitives into nodes/ways/relations for clonePrimitives
                    java.util.List<org.openstreetmap.josm.data.osm.Node> nodes = new java.util.ArrayList<>();
                    java.util.List<org.openstreetmap.josm.data.osm.Way> ways = new java.util.ArrayList<>();
                    java.util.List<org.openstreetmap.josm.data.osm.Relation> relations = new java.util.ArrayList<>();
                    for (OsmPrimitive p : selected) {
                        if (p instanceof org.openstreetmap.josm.data.osm.Node) nodes.add((org.openstreetmap.josm.data.osm.Node) p);
                        else if (p instanceof org.openstreetmap.josm.data.osm.Way) ways.add((org.openstreetmap.josm.data.osm.Way) p);
                        else if (p instanceof org.openstreetmap.josm.data.osm.Relation) relations.add((org.openstreetmap.josm.data.osm.Relation) p);
                    }
                    // Ensure we include all nodes referenced by selected ways (they might not be in selected set)
                    java.util.Set<Long> existingNodeIds = new java.util.HashSet<>();
                    for (org.openstreetmap.josm.data.osm.Node n : nodes) {
                        existingNodeIds.add(n.getUniqueId());
                    }
                    for (org.openstreetmap.josm.data.osm.Way w : ways) {
                        try {
                            for (org.openstreetmap.josm.data.osm.Node n : w.getNodes()) {
                                if (n == null) continue;
                                long uid = n.getUniqueId();
                                if (!existingNodeIds.contains(uid)) {
                                    nodes.add(n);
                                    existingNodeIds.add(uid);
                                }
                            }
                        } catch (Exception ignore) {
                        }
                    }
                    // Log IDs and dataset presence for diagnostics before cloning
                    StringBuilder ids = new StringBuilder();
                    for (OsmPrimitive p : selected) {
                        try {
                            long id = p.getId();
                            boolean hasDs = p.getDataSet() != null;
                            ids.append(String.format("[type=%s id=%d dataset=%s] ", p.getClass().getSimpleName(), id, hasDs ? "yes" : "no"));
                        } catch (Exception ignore) {
                        }
                    }
                    Logging.info("DPWValidationTool: primitives before clone: " + ids.toString());
                    try {
                        java.util.Map<org.openstreetmap.josm.data.osm.OsmPrimitive, org.openstreetmap.josm.data.osm.OsmPrimitive> mapping = newDs.clonePrimitives(nodes, ways, relations);
                        Logging.info("DPWValidationTool: clonePrimitives mapping size=" + (mapping == null ? 0 : mapping.size()) + ", newDs size=" + newDs.getPrimitives(pr -> true).size());
                    } catch (org.openstreetmap.josm.data.osm.DataIntegrityProblemException dip) {
                        // Specific dataset integrity problem: log and show detailed message to help diagnosis
                        Logging.error("DPWValidationTool: DataIntegrityProblemException while cloning primitives: " + dip.getMessage());
                        Logging.error(dip);
                        SwingUtilities.invokeLater(() -> {
                            JTextArea ta = new JTextArea();
                            ta.setEditable(false);
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            dip.printStackTrace(pw);
                            ta.setText("Failed to clone primitives into new dataset: " + dip.getMessage() + "\n\nStacktrace:\n" + sw.toString());
                            ta.setRows(20);
                            ta.setColumns(80);
                            JOptionPane.showMessageDialog(null, new JScrollPane(ta), "Data Integrity Error", JOptionPane.ERROR_MESSAGE);
                        });
                        return;
                    } catch (Exception ex) {
                        // Log and abort - do not add original primitives to new dataset (this causes duplication errors)
                        Logging.error("DPWValidationTool: Exception while cloning primitives: " + ex.getMessage());
                        Logging.error(ex);
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Failed to clone primitives into new dataset: " + ex.getMessage(), "Clone Error", JOptionPane.ERROR_MESSAGE));
                        return;
                    }
                    String layerName = "[Validation] " + mapper + " - " + dateString;
                    OsmDataLayer newLayer = new OsmDataLayer(newDs, layerName, null);
                    MainApplication.getLayerManager().addLayer(newLayer);
                    MainApplication.getLayerManager().setActiveLayer(newLayer);
                    
                    // v3.0 - Track the isolated layer and update state
                    isolatedLayer = newLayer;
                    currentState = ValidationState.ISOLATED;
                    
                    SwingUtilities.invokeLater(() -> {
                        updateWorkflowState();
                        JOptionPane.showMessageDialog(null, "Isolated layer created: " + layerName, "Isolated", JOptionPane.INFORMATION_MESSAGE);
                    });
                } catch (Exception ex) {
                    Logging.error(ex);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Failed to isolate work: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                } finally {
                    SwingUtilities.invokeLater(() -> isolateButton.setEnabled(true));
                }
            }).start();
        });

        // Disable submit buttons until Task ID is provided
        validateButton.setEnabled(false);
        invalidateButton.setEnabled(false);
        taskIdField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                updateSubmitButtonsEnabled();
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        // v3.1.0-BETA: TM URL auto-detection listener
        if (PluginSettings.isTMIntegrationEnabled() && tmUrlField != null) {
            tmUrlField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                private void update() {
                    handleTMUrlInput();
                }
                public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
            });
        }

        mapperUsernameComboBox.addItemListener(e -> updateAuthStatus());
    // when mapper selection changes, update the building count shown (mapper-specific)
    mapperUsernameComboBox.addItemListener(e -> updateMapperBuildingCount());
    // when mapper selection changes, auto-fill settlement from DPW data
    mapperUsernameComboBox.addItemListener(e -> updateMapperSettlement());
    }

    private void setFetchingMappers(boolean fetching) {
        isFetchingMappers = fetching;
        SwingUtilities.invokeLater(() -> {
            refreshMapperListButton.setEnabled(!fetching);
            if (fetching) {
                refreshMapperListButton.setToolTipText("Refreshing authorized mapper list...");
            } else {
                refreshMapperListButton.setToolTipText("Refresh authorized mapper list");
            }
            updateSubmitButtonsEnabled();
        });
    }

    private void setSending(boolean sending) {
        isSending = sending;
        SwingUtilities.invokeLater(() -> {
            validateButton.setEnabled(!sending && !taskIdField.getText().trim().isEmpty());
            invalidateButton.setEnabled(!sending && !taskIdField.getText().trim().isEmpty());
            refreshMapperListButton.setEnabled(!sending && !isFetchingMappers);
            if (sending) {
                showSendingDialog();
            } else {
                hideSendingDialog();
            }
        });
    }

    private void showSendingDialog() {
        if (sendingDialog != null && sendingDialog.isShowing()) return;
        Frame owner = MainApplication.getMainFrame();
        sendingDialog = new JDialog(owner, "Submitting...", false);
        JPanel p = new JPanel(new BorderLayout(8,8));
        p.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        JLabel l = new JLabel("Submitting data — please wait...");
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        p.add(l, BorderLayout.NORTH);
        p.add(bar, BorderLayout.CENTER);
        sendingDialog.getContentPane().add(p);
        sendingDialog.pack();
        sendingDialog.setLocationRelativeTo(owner);
        sendingDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        sendingDialog.setModal(false);
        sendingDialog.setVisible(true);
    }

    private void hideSendingDialog() {
        if (sendingDialog != null) {
            sendingDialog.setVisible(false);
            sendingDialog.dispose();
            sendingDialog = null;
        }
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
        // Show overall building count by default, but if a mapper is selected, show mapper-specific buildings
        String selectedMapper = (String) mapperUsernameComboBox.getSelectedItem();
        if (selectedMapper != null && !selectedMapper.isEmpty()) {
            int mapperBuildings = countBuildingsForMapper(selectedMapper, dataSet);
            totalBuildingsField.setText(String.valueOf(mapperBuildings));
        } else {
            totalBuildingsField.setText(String.valueOf(buildingCount));
        }

        // Populate mapper dropdown
        mapperUsernameComboBox.removeAllItems();
        List<String> sortedUsers = new ArrayList<>(userNames);
        Collections.sort(sortedUsers);
        for (String userName : sortedUsers) {
            mapperUsernameComboBox.addItem(userName);
        }
    }

    /**
     * Count building primitives in the provided DataSet authored by the given mapper username.
     */
    private int countBuildingsForMapper(String mapper, DataSet dataSet) {
        if (dataSet == null || mapper == null) return 0;
        int count = 0;
        for (OsmPrimitive p : dataSet.getPrimitives(pr -> pr.hasKey("building"))) {
            try {
                User u = p.getUser();
                if (u != null && mapper.equals(u.getName())) {
                    count++;
                }
            } catch (Exception ignore) {
            }
        }
        return count;
    }

    /**
     * Update the totalBuildingsField to reflect the currently selected mapper's building count.
     */
    private void updateMapperBuildingCount() {
        SwingUtilities.invokeLater(() -> {
            DataSet ds = MainApplication.getLayerManager().getEditDataSet();
            if (ds == null) {
                totalBuildingsField.setText("0");
                return;
            }
            String sel = (String) mapperUsernameComboBox.getSelectedItem();
            if (sel == null || sel.isEmpty()) {
                // show overall building count
                int total = 0;
                for (OsmPrimitive p : ds.getPrimitives(pr -> pr.hasKey("building"))) total++;
                totalBuildingsField.setText(String.valueOf(total));
            } else {
                totalBuildingsField.setText(String.valueOf(countBuildingsForMapper(sel, ds)));
            }
        });
    }

    /**
     * Update the settlement field based on selected mapper's data from DPW system.
     * Auto-fills the settlement from the mapper's profile in the DPW database.
     */
    private void updateMapperSettlement() {
        SwingUtilities.invokeLater(() -> {
            String selectedMapper = (String) mapperUsernameComboBox.getSelectedItem();
            if (selectedMapper == null || selectedMapper.trim().isEmpty()) {
                settlementField.setText("");
                return;
            }
            
            // Look up settlement for this mapper (case-insensitive)
            String settlement = null;
            synchronized (mapperSettlements) {
                for (Map.Entry<String, String> entry : mapperSettlements.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(selectedMapper)) {
                        settlement = entry.getValue();
                        break;
                    }
                }
            }
            
            settlementField.setText(settlement != null ? settlement : "");
            Logging.debug("DPWValidationTool: Auto-filled settlement '" + settlement + "' for mapper '" + selectedMapper + "'");
        });
    }

    /**
     * Fetch the authorized mapper/validator usernames from the DPW Manager API.
     * Connects to /api/users endpoint with exclude_managers=true for security.
     * Per v2.1 API spec: ALWAYS include exclude_managers=true to prevent exposing admin accounts.
     */
    private void fetchAuthorizedMappers() throws Exception {
        // Rate limiting: prevent 429 Too Many Requests errors
        long now = System.currentTimeMillis();
        if (now - lastMapperFetchTime < MAPPER_FETCH_COOLDOWN) {
            long waitTime = (MAPPER_FETCH_COOLDOWN - (now - lastMapperFetchTime)) / 1000;
            Logging.info("DPWValidationTool: Skipping mapper fetch - rate limit active. Wait " + waitTime + " seconds.");
            throw new Exception("Rate limit: Please wait " + waitTime + " seconds before refreshing");
        }
        lastMapperFetchTime = now;
        
        // Use configurable DPW API base URL (v3.1.0-BETA: from PluginSettings)
        String apiBaseUrl = PluginSettings.getDPWApiBaseUrl();
        
        // Construct full URL with query parameters (SECURITY: exclude_managers=true is REQUIRED)
        String fullUrl = apiBaseUrl + "/users?exclude_managers=true&status=Active";
        
        // indicate fetching to the user
        SwingUtilities.invokeLater(() -> {
            fetchStatusLabel.setText("Fetching authorized users from DPW Manager...");
            fetchStatusLabel.setBackground(Color.YELLOW);
            updateSubmitButtonsEnabled();
        });

        URL url = new URI(fullUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "DPW-JOSM-Plugin/3.1.0-BETA (JOSM Validation Tool)");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int rc = conn.getResponseCode();
        
        // Read response body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                rc >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        String body = sb.toString().trim();
        
        if (rc < 200 || rc >= 300) {
            // Try to parse error message from JSON
            String errorMsg = "HTTP " + rc;
            try {
                // Simple error extraction: look for "error" field
                Pattern errorPattern = Pattern.compile("\"error\"\\s*:\\s*\"([^\"]+)\"");
                Matcher errorMatcher = errorPattern.matcher(body);
                if (errorMatcher.find()) {
                    errorMsg = errorMatcher.group(1);
                }
            } catch (Exception ignore) {}
            throw new IllegalStateException("User list API returned error: " + errorMsg);
        }

        // Parse JSON response: { "success": true, "data": [...], "count": N }
        // Extract usernames and settlements
        List<UserInfo> userInfoList = new ArrayList<>();
        
        try {
            userInfoList = parseUserListJson(body);
        } catch (Exception e) {
            Logging.error("DPWValidationTool: Failed to parse user list response: " + e.getMessage());
            throw new IllegalStateException("Failed to parse user list from API: " + e.getMessage());
        }

        if (userInfoList.isEmpty()) {
            Logging.warn("DPWValidationTool: No users found in API response");
        }

        // persist the used base URL so users don't have to set it manually
        try {
            Preferences.main().put("dpw.api_base_url", apiBaseUrl);
        } catch (Exception ignore) {
        }

        // Extract usernames and build settlement mapping
        List<String> usernames = new ArrayList<>();
        Map<String, String> settlements = new HashMap<>();
        for (UserInfo user : userInfoList) {
            usernames.add(user.osmUsername);
            settlements.put(user.osmUsername, user.settlement);
        }

        // replace authorizedMappers and mapperSettlements atomically
        synchronized (authorizedMappers) {
            authorizedMappers.clear();
            authorizedMappers.addAll(usernames);
        }
        
        synchronized (mapperSettlements) {
            mapperSettlements.clear();
            mapperSettlements.putAll(settlements);
        }

        // Update UI to reflect success
        final int userCount = usernames.size();
        SwingUtilities.invokeLater(() -> {
            fetchStatusLabel.setText("Success: " + userCount + " authorized users loaded from DPW Manager.");
            fetchStatusLabel.setBackground(new Color(0x88ff88));
            updateAuthStatus();
            updateSubmitButtonsEnabled();
        });
    }

    /**
     * Parse the user list JSON response from the API.
     * Extracts osm_username and settlement for each user.
     * Expected format: { "success": true, "data": [{"osm_username": "user1", "settlement": "Area1"}, ...], "count": N }
     * 
     * @param json The JSON response body
     * @return List of UserInfo objects containing username and settlement
     * @throws Exception if parsing fails
     */
    private List<UserInfo> parseUserListJson(String json) throws Exception {
        List<UserInfo> users = new ArrayList<>();
        
        // Find the "data" array
        int dataStart = json.indexOf("\"data\"");
        if (dataStart == -1) {
            throw new IllegalStateException("Response does not contain 'data' field");
        }
        
        // Find the opening bracket of the data array
        int arrayStart = json.indexOf('[', dataStart);
        if (arrayStart == -1) {
            throw new IllegalStateException("'data' field is not an array");
        }
        
        // Find the matching closing bracket
        int arrayEnd = findMatchingBracket(json, arrayStart);
        if (arrayEnd == -1) {
            throw new IllegalStateException("Malformed 'data' array");
        }
        
        String dataArray = json.substring(arrayStart + 1, arrayEnd);
        
        // Parse each user object in the array - extract both username and settlement
        int pos = 0;
        while (pos < dataArray.length()) {
            // Find the start of next user object
            int objStart = dataArray.indexOf('{', pos);
            if (objStart == -1) {
                break; // No more objects
            }
            
            // Find the end of this user object
            int objEnd = findMatchingBrace(dataArray, objStart);
            if (objEnd == -1) {
                pos = objStart + 1;
                continue;
            }
            
            String userObj = dataArray.substring(objStart, objEnd + 1);
            
            // Extract osm_username
            String username = extractJsonField(userObj, "osm_username");
            
            // Extract settlement (may be null or empty)
            String settlement = extractJsonField(userObj, "settlement");
            
            if (username != null && !username.trim().isEmpty()) {
                users.add(new UserInfo(username, settlement));
            }
            
            pos = objEnd + 1;
        }
        
        return users;
    }
    
    /**
     * Extract a string field value from a JSON object string.
     * Returns null if field not found.
     */
    private String extractJsonField(String jsonObj, String fieldName) {
        int fieldStart = jsonObj.indexOf("\"" + fieldName + "\"");
        if (fieldStart == -1) {
            return null;
        }
        
        // Find the colon after the field name
        int colonPos = jsonObj.indexOf(':', fieldStart);
        if (colonPos == -1) {
            return null;
        }
        
        // Skip whitespace after colon
        int pos = colonPos + 1;
        while (pos < jsonObj.length() && Character.isWhitespace(jsonObj.charAt(pos))) {
            pos++;
        }
        
        if (pos >= jsonObj.length()) {
            return null;
        }
        
        // Check if value is null
        if (jsonObj.startsWith("null", pos)) {
            return null;
        }
        
        // Find the opening quote of the value
        if (jsonObj.charAt(pos) != '"') {
            return null; // Not a string value
        }
        
        // Find the closing quote, handling escaped quotes
        int valueEnd = findClosingQuote(jsonObj, pos + 1);
        if (valueEnd == -1) {
            return null;
        }
        
        // Extract and unescape the value
        return unescapeJsonString(jsonObj.substring(pos + 1, valueEnd));
    }
    
    /**
     * Find the matching closing brace for an opening brace.
     */
    private int findMatchingBrace(String str, int openPos) {
        if (openPos >= str.length() || str.charAt(openPos) != '{') {
            return -1;
        }
        
        int depth = 1;
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = openPos + 1; i < str.length(); i++) {
            char c = str.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (inString) {
                continue;
            }
            
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        
        return -1;
    }
    
    /**
     * Find the matching closing bracket for an opening bracket.
     */
    private int findMatchingBracket(String str, int openPos) {
        if (openPos >= str.length() || str.charAt(openPos) != '[') {
            return -1;
        }
        
        int depth = 1;
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = openPos + 1; i < str.length(); i++) {
            char c = str.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (inString) {
                continue;
            }
            
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        
        return -1;
    }
    
    /**
     * Find the closing quote of a JSON string value, handling escaped quotes.
     */
    private int findClosingQuote(String str, int startPos) {
        boolean escaped = false;
        
        for (int i = startPos; i < str.length(); i++) {
            char c = str.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * Unescape a JSON string value (handles escape sequences like quotes, backslash, newline, tab, unicode).
     */
    private String unescapeJsonString(String str) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            
            if (escaped) {
                switch (c) {
                    case '"':
                    case '\\':
                    case '/':
                        result.append(c);
                        break;
                    case 'n':
                        result.append('\n');
                        break;
                    case 'r':
                        result.append('\r');
                        break;
                    case 't':
                        result.append('\t');
                        break;
                    case 'b':
                        result.append('\b');
                        break;
                    case 'f':
                        result.append('\f');
                        break;
                    case 'u':
                        // Unicode escape sequence (4 hex digits)
                        if (i + 4 < str.length()) {
                            try {
                                String hex = str.substring(i + 1, i + 5);
                                int codePoint = Integer.parseInt(hex, 16);
                                result.append((char) codePoint);
                                i += 4;
                            } catch (NumberFormatException e) {
                                // Invalid unicode escape, keep as-is
                                result.append('\\').append(c);
                            }
                        } else {
                            result.append('\\').append(c);
                        }
                        break;
                    default:
                        // Unknown escape, keep as-is
                        result.append('\\').append(c);
                        break;
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }

    /**
     * Reset the validation session to allow a new submission.
     * Clears all form fields and resets the submission flag.
     */
    private void resetValidationSession() {
        submittedThisSession = false;
        
        // v3.0 - Reset workflow state
        currentState = ValidationState.IDLE;
        isolatedLayer = null;
        lastValidationStatus = null;
        
        // Clear form fields
        taskIdField.setText("");
        settlementField.setText("");
        validatorCommentsArea.setText("");
        
        // Reset error counts
        for (int i = 0; i < errorCounts.length; i++) {
            errorCounts[i] = 0;
            errorCountLabels[i].setText("0");
        }
        
        // Reset date picker
        try {
            if (datePickerComponent instanceof org.jdatepicker.impl.JDatePickerImpl) {
                org.jdatepicker.impl.JDatePickerImpl picker = (org.jdatepicker.impl.JDatePickerImpl) datePickerComponent;
                picker.getModel().setValue(null);
            } else if (datePickerComponent instanceof JTextField) {
                ((JTextField) datePickerComponent).setText("YYYY-MM-DD");
            }
        } catch (Exception e) {
            Logging.warn("DPWValidationTool: Could not reset date picker: " + e.getMessage());
        }
        
        // Keep mapper selection and total buildings as-is (user may want to validate more from same mapper)
        
        updateSubmitButtonsEnabled();
        updateWorkflowState();
        
        Logging.info("DPWValidationTool: Validation session reset");
    }
    
    /**
     * Update UI based on current workflow state.
     * v3.0 - New method to manage state-based UI updates.
     */
    private void updateWorkflowState() {
        SwingUtilities.invokeLater(() -> {
            // Enable/disable buttons based on state
            switch (currentState) {
                case IDLE:
                    isolateButton.setEnabled(true);
                    validateButton.setEnabled(false);
                    invalidateButton.setEnabled(false);
                    validationPreviewPanel.setVisible(false);
                    break;
                case ISOLATED:
                    isolateButton.setEnabled(true);
                    validateButton.setEnabled(!taskIdField.getText().trim().isEmpty());
                    invalidateButton.setEnabled(!taskIdField.getText().trim().isEmpty());
                    validationPreviewPanel.setVisible(true);
                    break;
                case SUBMITTED:
                    isolateButton.setEnabled(false);
                    validateButton.setEnabled(false);
                    invalidateButton.setEnabled(false);
                    break;
                case EXPORTED:
                    isolateButton.setEnabled(false);
                    validateButton.setEnabled(false);
                    invalidateButton.setEnabled(false);
                    break;
            }
        });
    }
    
    /**
     * Update the validation preview panel with current form data.
     * v3.0 - Shows summary of validation before submission.
     */
    private void updateValidationPreview() {
        StringBuilder preview = new StringBuilder();
        preview.append("═══════════════════════════════════════════════════════\n");
        preview.append("              VALIDATION SUMMARY\n");
        preview.append("═══════════════════════════════════════════════════════\n\n");
        
        // Task Information
        String taskId = taskIdField.getText().trim();
        String mapper = (String) mapperUsernameComboBox.getSelectedItem();
        String settlement = settlementField.getText().trim();
        String dateString = getDateStringFromPicker();
        String totalBuildings = totalBuildingsField.getText().trim();
        
        preview.append("Task ID:         ").append(taskId.isEmpty() ? "(not specified)" : taskId).append("\n");
        preview.append("Settlement:      ").append(settlement.isEmpty() ? "(not specified)" : settlement).append("\n");
        preview.append("Mapper:          ").append(mapper != null ? mapper : "(not selected)").append("\n");
        preview.append("Date:            ").append(dateString != null ? dateString : "(not specified)").append("\n");
        preview.append("Total Buildings: ").append(totalBuildings).append("\n\n");
        
        // Error Summary
        preview.append("───────────────────────────────────────────────────────\n");
        preview.append("                 ERROR BREAKDOWN\n");
        preview.append("───────────────────────────────────────────────────────\n\n");
        
        int totalErrors = 0;
        for (int i = 0; i < errorTypes.length; i++) {
            if (errorCounts[i] > 0) {
                preview.append(String.format("  • %-35s %3d\n", errorTypes[i] + ":", errorCounts[i]));
                totalErrors += errorCounts[i];
            }
        }
        
        if (totalErrors == 0) {
            preview.append("  ✓ No errors found - Clean validation!\n");
        } else {
            preview.append(String.format("\n  TOTAL ERRORS: %d\n", totalErrors));
        }
        
        // Comments
        String comments = validatorCommentsArea.getText().trim();
        if (!comments.isEmpty()) {
            preview.append("\n───────────────────────────────────────────────────────\n");
            preview.append("                VALIDATOR COMMENTS\n");
            preview.append("───────────────────────────────────────────────────────\n\n");
            preview.append(comments).append("\n");
        }
        
        preview.append("\n═══════════════════════════════════════════════════════\n");
        
        // Validation Decision (will be set when button is clicked)
        if (lastValidationStatus != null) {
            preview.append("\nDecision: ").append(lastValidationStatus).append("\n");
        }
        
        previewTextArea.setText(preview.toString());
        previewTextArea.setCaretPosition(0); // Scroll to top
    }
    
    /**
     * Show enhanced confirmation dialog before submitting validation.
     * v3.0 - Rich dialog with context and summary.
     * 
     * @param validationStatus "Validated" or "Rejected"
     * @return true if user confirmed, false if cancelled
     */
    private boolean showConfirmationDialog(String validationStatus) {
        // Build confirmation message with rich context
        StringBuilder message = new StringBuilder();
        
        String mapper = (String) mapperUsernameComboBox.getSelectedItem();
        String dateString = getDateStringFromPicker();
        String totalBuildings = totalBuildingsField.getText().trim();
        
        // Count total errors
        int totalErrors = 0;
        for (int count : errorCounts) {
            totalErrors += count;
        }
        
        // Header based on action
        if ("Validated".equals(validationStatus)) {
            message.append("✓ Confirm Validation Acceptance\n");
            message.append("═══════════════════════════════════════════\n\n");
            message.append("You are about to ACCEPT this validation:\n\n");
        } else {
            message.append("✗ Confirm Validation Rejection\n");
            message.append("═══════════════════════════════════════════\n\n");
            message.append("You are about to REJECT this validation:\n\n");
        }
        
        // Summary
        message.append("  • Mapper:           ").append(mapper != null ? mapper : "(not selected)").append("\n");
        message.append("  • Date:             ").append(dateString != null ? dateString : "(not specified)").append("\n");
        message.append("  • Total Buildings:  ").append(totalBuildings).append("\n");
        message.append("  • Total Errors:     ").append(totalErrors).append("\n\n");
        
        // Error breakdown if there are errors
        if (totalErrors > 0) {
            message.append("Error Breakdown:\n");
            for (int i = 0; i < errorTypes.length; i++) {
                if (errorCounts[i] > 0) {
                    message.append("  • ").append(errorTypes[i]).append(": ").append(errorCounts[i]).append("\n");
                }
            }
            message.append("\n");
        }
        
        // What will happen next
        message.append("───────────────────────────────────────────\n");
        message.append("This will:\n");
        message.append("  1. Submit validation to DPW Manager\n");
        
        if ("Validated".equals(validationStatus)) {
            message.append("  2. Prompt you to export the validated data\n");
            message.append("  3. Prepare JOSM for the next task\n");
        } else {
            message.append("  2. Log the rejection for review\n");
            message.append("  3. Reset the form for next validation\n");
        }
        
        message.append("\n Do you want to continue?");
        
        // Show custom dialog with OK/Cancel
        int result = JOptionPane.showConfirmDialog(
            null,
            message.toString(),
            "Confirm " + validationStatus,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        return result == JOptionPane.OK_OPTION;
    }
    
    /**
     * Fetch user_id from DPW API by OSM username.
     * v3.0.1 - Needed for cloud upload integration.
     * 
     * @param osmUsername The OSM username to look up
     * @return user_id or -1 if not found
     */
    private int getUserIdByOsmUsername(String osmUsername) {
        if (osmUsername == null || osmUsername.trim().isEmpty()) {
            return -1;
        }
        
        try {
            String baseUrl = Preferences.main().get("dpw.api_base_url", "https://dpw-mauve.vercel.app");
            String encodedUsername = URLEncoder.encode(osmUsername, StandardCharsets.UTF_8.toString());
            String apiUrl = baseUrl + "/api/users?osm_username=" + encodedUsername + "&exclude_managers=true";
            
            Logging.debug("DPWValidationTool: Fetching user_id for: " + osmUsername);
            
            URL url = new URI(apiUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            int responseCode = conn.getResponseCode();
            
            // Read response
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            
            String responseBody = response.toString();
            
            if (responseCode == 200) {
                // Parse user_id from response
                // Expected: {"success":true,"data":[{"user_id":45,"osm_username":"john_mapper",...}]}
                Pattern userIdPattern = Pattern.compile("\"user_id\"\\s*:\\s*(\\d+)");
                Matcher matcher = userIdPattern.matcher(responseBody);
                
                if (matcher.find()) {
                    int userId = Integer.parseInt(matcher.group(1));
                    Logging.info("DPWValidationTool: Found user_id=" + userId + " for " + osmUsername);
                    return userId;
                } else {
                    Logging.warn("DPWValidationTool: No user_id found in response for " + osmUsername);
                    return -1;
                }
            } else {
                Logging.error("DPWValidationTool: Failed to fetch user_id: HTTP " + responseCode);
                return -1;
            }
            
        } catch (Exception ex) {
            Logging.error("DPWValidationTool: Error fetching user_id: " + ex.getMessage());
            Logging.error(ex);
            return -1;
        }
    }
    
    /**
     * Upload OSM file to cloud storage via DPW API.
     * v3.0.1 - Cloud integration for validated data backup.
     * 
     * @param file The OSM file to upload
     * @param validationLogId The log_id from validation submission
     * @param mapperUserId Database user_id of the mapper
     * @param validatorUserId Database user_id of the validator
     * @param taskId Optional task identifier
     * @param settlement Optional settlement name
     * @return Google Drive URL if successful, null otherwise
     */
    private String uploadToCloud(java.io.File file, int validationLogId, int mapperUserId, 
                                   int validatorUserId, String taskId, String settlement) {
        try {
            String baseUrl = Preferences.main().get("dpw.api_base_url", "https://dpw-mauve.vercel.app");
            String apiUrl = baseUrl + "/api/osm-uploads";
            
            Logging.info("DPWValidationTool: Uploading to cloud: " + file.getName());
            
            URL url = new URI(apiUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000); // 30 seconds for upload
            conn.setReadTimeout(30000);
            
            // Create multipart boundary
            String boundary = "----DPWValidationToolBoundary" + System.currentTimeMillis();
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            
            try (OutputStream out = conn.getOutputStream();
                 java.io.PrintWriter writer = new java.io.PrintWriter(
                     new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8), true)) {
                
                // Add file field
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                      .append(file.getName()).append("\"\r\n");
                writer.append("Content-Type: application/xml\r\n\r\n");
                writer.flush();
                
                // Write file content
                java.nio.file.Files.copy(file.toPath(), out);
                out.flush();
                writer.append("\r\n");
                
                // Add validation_log_id
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"validation_log_id\"\r\n\r\n");
                writer.append(String.valueOf(validationLogId)).append("\r\n");
                
                // Add mapper_user_id
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"mapper_user_id\"\r\n\r\n");
                writer.append(String.valueOf(mapperUserId)).append("\r\n");
                
                // Add validator_user_id
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"validator_user_id\"\r\n\r\n");
                writer.append(String.valueOf(validatorUserId)).append("\r\n");
                
                // Add uploaded_by_user_id (same as validator)
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"uploaded_by_user_id\"\r\n\r\n");
                writer.append(String.valueOf(validatorUserId)).append("\r\n");
                
                // Add optional task_id
                if (taskId != null && !taskId.trim().isEmpty()) {
                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"task_id\"\r\n\r\n");
                    writer.append(taskId).append("\r\n");
                }
                
                // Add optional settlement
                if (settlement != null && !settlement.trim().isEmpty()) {
                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"settlement\"\r\n\r\n");
                    writer.append(settlement).append("\r\n");
                }
                
                // End multipart
                writer.append("--").append(boundary).append("--\r\n");
                writer.flush();
            }
            
            int responseCode = conn.getResponseCode();
            
            // Read response
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            
            String responseBody = response.toString();
            Logging.debug("DPWValidationTool: Upload response: " + responseBody);
            
            if (responseCode == 200) {
                // Parse drive_file_url from response
                Pattern urlPattern = Pattern.compile("\"drive_file_url\"\\s*:\\s*\"([^\"]+)\"");
                Matcher matcher = urlPattern.matcher(responseBody);
                
                if (matcher.find()) {
                    String driveUrl = matcher.group(1);
                    Logging.info("DPWValidationTool: Upload successful, Drive URL: " + driveUrl);
                    return driveUrl;
                } else {
                    Logging.warn("DPWValidationTool: Upload successful but no drive_file_url in response");
                    return null;
                }
            } else {
                String errorMsg = extractErrorMessage(responseBody);
                Logging.error("DPWValidationTool: Upload failed: HTTP " + responseCode + " - " + errorMsg);
                return null;
            }
            
        } catch (Exception ex) {
            Logging.error("DPWValidationTool: Upload exception: " + ex.getMessage());
            Logging.error(ex);
            return null;
        }
    }
    
    /**
     * Show export dialog after successful validation acceptance.
     * v3.0 - Auto-prompt to export validated layer.
     */
    private void showExportDialog() {
        StringBuilder message = new StringBuilder();
        message.append("📦 Export Validated Layer\n");
        message.append("═══════════════════════════════════════════\n\n");
        message.append("Validation accepted successfully!\n\n");
        message.append("Would you like to export the validated layer now?\n\n");
        message.append("The export will save ONLY the isolated validated\n");
        message.append("data for this mapper and date.\n\n");
        message.append("Export now or skip this step?");
        
        String[] options = {"📤 Export Now", "Skip"};
        int result = JOptionPane.showOptionDialog(
            null,
            message.toString(),
            "Export Validated Layer",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        
        if (result == 0) { // Export Now
            performExport();
        } else {
            // User skipped export, ask if they want to reset
            int choice = JOptionPane.showConfirmDialog(null,
                "Would you like to start a new validation session?",
                "Start New Session?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            
            if (choice == JOptionPane.YES_OPTION) {
                resetValidationSession();
            }
        }
    }
    
    /**
     * Perform the export of the isolated validated layer.
     * v3.0 - Exports with progress indication and handles errors.
     */
    private void performExport() {
        new Thread(() -> {
            try {
                // Validate isolated layer still exists
                if (isolatedLayer == null) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(null,
                            "Error: Isolated layer not found.\n\n" +
                            "The validation layer may have been closed.\n" +
                            "Please isolate the work again before exporting.",
                            "Cannot Export",
                            JOptionPane.ERROR_MESSAGE));
                    return;
                }
                
                // Check if layer is still in layer manager
                boolean layerExists = false;
                for (org.openstreetmap.josm.gui.layer.Layer layer : MainApplication.getLayerManager().getLayers()) {
                    if (layer == isolatedLayer) {
                        layerExists = true;
                        break;
                    }
                }
                
                if (!layerExists) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(null,
                            "Error: Isolated layer has been closed.\n\n" +
                            "Please isolate the work again before exporting.",
                            "Layer Closed",
                            JOptionPane.ERROR_MESSAGE));
                    isolatedLayer = null;
                    return;
                }
                
                // Generate filename
                String taskId = taskIdField.getText().trim();
                String mapper = (String) mapperUsernameComboBox.getSelectedItem();
                if (mapper == null) mapper = "";
                String dateString = getDateStringFromPicker();
                String filename = String.format("Task_%s_%s_%s.osm", 
                    taskId.isEmpty() ? "unknown" : taskId, 
                    mapper, 
                    dateString != null ? dateString : "unknown");
                
                // Show file chooser on EDT
                SwingUtilities.invokeLater(() -> {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Export Validated Layer");
                    chooser.setSelectedFile(new java.io.File(filename));
                    
                    int res = chooser.showSaveDialog(MainApplication.getMainFrame());
                    if (res != JFileChooser.APPROVE_OPTION) {
                        // User cancelled, ask if they want to retry or skip
                        int retry = JOptionPane.showConfirmDialog(null,
                            "Export cancelled. Would you like to try again?",
                            "Export Cancelled",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                        
                        if (retry == JOptionPane.YES_OPTION) {
                            performExport(); // Retry
                        }
                        return;
                    }
                    
                    java.io.File file = chooser.getSelectedFile();
                    
                    // Perform export in background thread
                    new Thread(() -> {
                        try {
                            // Show progress dialog
                            JDialog progressDialog = new JDialog(MainApplication.getMainFrame(), "Exporting...", false);
                            JPanel progressPanel = new JPanel(new BorderLayout(8,8));
                            progressPanel.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
                            JLabel progressLabel = new JLabel("Exporting validated layer...");
                            JProgressBar progressBar = new JProgressBar();
                            progressBar.setIndeterminate(true);
                            progressPanel.add(progressLabel, BorderLayout.NORTH);
                            progressPanel.add(progressBar, BorderLayout.CENTER);
                            progressDialog.getContentPane().add(progressPanel);
                            progressDialog.pack();
                            progressDialog.setLocationRelativeTo(MainApplication.getMainFrame());
                            progressDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                            
                            SwingUtilities.invokeLater(() -> progressDialog.setVisible(true));
                            
                            // Write DataSet to file
                            try (java.io.PrintWriter pw = new java.io.PrintWriter(
                                    new java.io.OutputStreamWriter(
                                        new java.io.FileOutputStream(file), 
                                        java.nio.charset.StandardCharsets.UTF_8))) {
                                OsmWriter w = OsmWriterFactory.createOsmWriter(pw, true, 
                                    org.openstreetmap.josm.io.OsmWriter.DEFAULT_API_VERSION);
                                w.write(isolatedLayer.getDataSet());
                                pw.flush();
                            }
                            
                            Logging.info("DPWValidationTool: Export successful: " + file.getAbsolutePath());
                            
                            // v3.0.1 - Upload to cloud if we have validation log data
                            String driveUrl = null;
                            if (lastValidationLogId > 0) {
                                SwingUtilities.invokeLater(() -> {
                                    progressLabel.setText("Uploading to cloud storage...");
                                });
                                
                                // Fetch user IDs from API
                                String mapperOsmUsername = (String) mapperUsernameComboBox.getSelectedItem();
                                
                                UserIdentityManager userManager = UserIdentityManager.getInstance();
                                String validatorOsmUsername = userManager.getUserName();
                                
                                int mapperId = -1;
                                int validatorId = -1;
                                
                                if (mapperOsmUsername != null && !mapperOsmUsername.trim().isEmpty()) {
                                    mapperId = getUserIdByOsmUsername(mapperOsmUsername.trim());
                                }
                                
                                if (validatorOsmUsername != null && !validatorOsmUsername.trim().isEmpty()) {
                                    validatorId = getUserIdByOsmUsername(validatorOsmUsername.trim());
                                }
                                
                                // Upload to cloud
                                if (mapperId > 0 && validatorId > 0) {
                                    String settlement = settlementField.getText().trim();
                                    driveUrl = uploadToCloud(file, lastValidationLogId, mapperId, 
                                                             validatorId, taskId, settlement);
                                    
                                    if (driveUrl != null) {
                                        googleDriveFileUrl = driveUrl;
                                        Logging.info("DPWValidationTool: Cloud upload successful");
                                    } else {
                                        Logging.warn("DPWValidationTool: Cloud upload failed, continuing...");
                                    }
                                } else {
                                    Logging.warn("DPWValidationTool: Could not fetch user IDs for upload. " +
                                               "Mapper ID: " + mapperId + ", Validator ID: " + validatorId);
                                }
                            } else {
                                Logging.info("DPWValidationTool: No validation log ID, skipping cloud upload");
                            }
                            
                            final String finalDriveUrl = driveUrl;
                            SwingUtilities.invokeLater(() -> progressDialog.dispose());
                            
                            // Update state
                            currentState = ValidationState.EXPORTED;
                            
                            // Show success message (Drive URL hidden - company property)
                            SwingUtilities.invokeLater(() -> {
                                String message = "✓ Export Successful!\n\n" +
                                                "File saved to:\n" + file.getAbsolutePath();
                                
                                // v3.0.1 - Cloud upload happens silently, Drive URL not shown to validators
                                if (finalDriveUrl != null) {
                                    message += "\n\n✓ Backed up to cloud storage";
                                } else if (lastValidationLogId > 0) {
                                    message += "\n\n⚠ Cloud backup failed\n" +
                                              "Local file saved successfully.";
                                }
                                
                                JOptionPane.showMessageDialog(null,
                                    message,
                                    "Export Complete",
                                    JOptionPane.INFORMATION_MESSAGE);
                                
                                showRestartDialog(file.getName());
                            });
                            
                            Logging.info("DPWValidationTool: Export workflow complete");
                            
                        } catch (Exception ex) {
                            Logging.error("DPWValidationTool: Export failed: " + ex.getMessage());
                            Logging.error(ex);
                            
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(null,
                                    "Export Failed\n\n" +
                                    "Error: " + ex.getMessage() + "\n\n" +
                                    "Please try again or contact support.",
                                    "Export Error",
                                    JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    }).start();
                });
                
            } catch (Exception ex) {
                Logging.error("DPWValidationTool: Export preparation failed: " + ex.getMessage());
                Logging.error(ex);
                
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null,
                        "Failed to prepare export: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }
    
    /**
     * Show dialog prompting user to reset session after export.
     * v3.0 - Helps prevent data duplication by clearing all layers.
     * 
     * @param exportedFileName The name of the file that was exported
     */
    private void showRestartDialog(String exportedFileName) {
        StringBuilder message = new StringBuilder();
        message.append("🔄 Ready for Next Task\n");
        message.append("═══════════════════════════════════════════\n\n");
        message.append("✓ Validation submitted successfully\n");
        message.append("✓ Data exported to: ").append(exportedFileName).append("\n\n");
        message.append("To prevent data duplication and prepare for your\n");
        message.append("next validation task, we recommend resetting the\n");
        message.append("session now. This will:\n\n");
        message.append("  • Clear all JOSM layers\n");
        message.append("  • Reset the validation form\n");
        message.append("  • Prepare for the next validation\n\n");
        message.append("What would you like to do?\n\n");
        message.append("⚠️  Continuing without reset may cause layer\n");
        message.append("   confusion in future validations.");
        
        String[] options = {"🔄 Reset Session", "📝 Continue Working"};
        int result = JOptionPane.showOptionDialog(
            null,
            message.toString(),
            "Reset Session",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        
        if (result == 0) { // Reset Session
            resetSession();
        } else {
            // Continue working - just reset the form
            int choice = JOptionPane.showConfirmDialog(null,
                "Would you like to reset the form for a new validation?",
                "Reset Form?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            
            if (choice == JOptionPane.YES_OPTION) {
                resetValidationSession();
            }
        }
    }
    
    /**
     * Reset the entire session by clearing all layers and resetting the form.
     * v3.0 - Provides clean slate for next validation task without restarting JOSM.
     */
    private void resetSession() {
        try {
            Logging.info("DPWValidationTool: User requested session reset");
            
            // Store dialog visibility state before clearing layers
            boolean wasVisible = isDialogShowing();
            
            // Get layer manager
            org.openstreetmap.josm.gui.layer.LayerManager layerManager = MainApplication.getLayerManager();
            
            // Get all layers and remove them
            java.util.List<org.openstreetmap.josm.gui.layer.Layer> allLayers = new java.util.ArrayList<>(layerManager.getLayers());
            
            if (allLayers.isEmpty()) {
                Logging.info("DPWValidationTool: No layers to remove");
            } else {
                Logging.info("DPWValidationTool: Removing " + allLayers.size() + " layers");
                
                // Show progress for large number of layers
                if (allLayers.size() > 5) {
                    SwingUtilities.invokeLater(() -> {
                        JDialog progressDialog = new JDialog(MainApplication.getMainFrame(), "Resetting Session...", false);
                        JPanel progressPanel = new JPanel(new BorderLayout(8,8));
                        progressPanel.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
                        JLabel progressLabel = new JLabel("Clearing all layers...");
                        JProgressBar progressBar = new JProgressBar();
                        progressBar.setIndeterminate(true);
                        progressPanel.add(progressLabel, BorderLayout.NORTH);
                        progressPanel.add(progressBar, BorderLayout.CENTER);
                        progressDialog.getContentPane().add(progressPanel);
                        progressDialog.pack();
                        progressDialog.setLocationRelativeTo(MainApplication.getMainFrame());
                        progressDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                        progressDialog.setVisible(true);
                        
                        // Remove layers in background
                        new Thread(() -> {
                            try {
                                for (org.openstreetmap.josm.gui.layer.Layer layer : allLayers) {
                                    try {
                                        layerManager.removeLayer(layer);
                                        Thread.sleep(50); // Small delay to avoid overwhelming UI
                                    } catch (Exception e) {
                                        Logging.warn("DPWValidationTool: Could not remove layer: " + e.getMessage());
                                    }
                                }
                                
                                SwingUtilities.invokeLater(() -> {
                                    progressDialog.dispose();
                                    
                                    // Reset the form
                                    resetValidationSession();
                                    
                                    // Ensure dialog stays visible and functional after layer removal
                                    ensureDialogVisible(wasVisible);
                                    
                                    // Show success message
                                    JOptionPane.showMessageDialog(null,
                                        "✓ Session Reset Complete!\n\n" +
                                        "All layers have been cleared.\n" +
                                        "The form has been reset.\n\n" +
                                        "You're ready for the next validation task.",
                                        "Session Reset",
                                        JOptionPane.INFORMATION_MESSAGE);
                                    
                                    Logging.info("DPWValidationTool: Session reset completed successfully");
                                });
                            } catch (Exception ex) {
                                Logging.error("DPWValidationTool: Error during session reset: " + ex.getMessage());
                                SwingUtilities.invokeLater(() -> {
                                    progressDialog.dispose();
                                    JOptionPane.showMessageDialog(null,
                                        "Session reset completed with some errors.\n\n" +
                                        "Some layers may not have been removed.\n" +
                                        "Please check the layer list.",
                                        "Reset Warning",
                                        JOptionPane.WARNING_MESSAGE);
                                });
                            }
                        }).start();
                    });
                } else {
                    // Quick reset for few layers
                    for (org.openstreetmap.josm.gui.layer.Layer layer : allLayers) {
                        try {
                            layerManager.removeLayer(layer);
                        } catch (Exception e) {
                            Logging.warn("DPWValidationTool: Could not remove layer: " + e.getMessage());
                        }
                    }
                    
                    // Reset the form
                    resetValidationSession();
                    
                    // Ensure dialog stays visible and functional after layer removal
                    ensureDialogVisible(wasVisible);
                    
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null,
                            "✓ Session Reset Complete!\n\n" +
                            "All layers have been cleared.\n" +
                            "The form has been reset.\n\n" +
                            "You're ready for the next validation task.",
                            "Session Reset",
                            JOptionPane.INFORMATION_MESSAGE);
                    });
                    
                    Logging.info("DPWValidationTool: Session reset completed successfully");
                }
            }
            
        } catch (Exception ex) {
            Logging.error("DPWValidationTool: Failed to reset session: " + ex.getMessage());
            Logging.error(ex);
            
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                    "Failed to reset session automatically.\n\n" +
                    "Error: " + ex.getMessage() + "\n\n" +
                    "Please manually:\n" +
                    "1. Remove all layers from the Layers panel\n" +
                    "2. Click 'Reset Form' below to clear the form\n\n" +
                    "Or restart JOSM for a clean state.",
                    "Reset Failed",
                    JOptionPane.ERROR_MESSAGE);
                
                // Still try to reset the form
                int choice = JOptionPane.showConfirmDialog(null,
                    "Would you like to reset the form anyway?",
                    "Reset Form?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
                
                if (choice == JOptionPane.YES_OPTION) {
                    resetValidationSession();
                }
            });
        }
    }
    
    /**
     * Ensure the dialog stays visible and functional after session reset.
     * v3.0.1 - Fix for plugin becoming unresponsive after clearing all layers.
     * 
     * @param wasVisible whether the dialog was visible before reset
     */
    private void ensureDialogVisible(boolean wasVisible) {
        try {
            // Force dialog to stay registered and visible
            if (wasVisible) {
                // Make sure the dialog is shown
                if (!isDialogShowing()) {
                    showDialog();
                }
                
                // Repaint to ensure UI is responsive
                revalidate();
                repaint();
                
                Logging.info("DPWValidationTool: Dialog visibility restored");
            }
        } catch (Exception e) {
            Logging.warn("DPWValidationTool: Could not restore dialog visibility: " + e.getMessage());
        }
    }
    
    /**
     * Validate all input fields before submission.
     * Checks field lengths, formats, and required values.
     * 
     * @return true if all inputs are valid, false otherwise (shows error dialog)
     */
    private boolean validateInputs() {
        // Task ID validation (optional but if provided, must be reasonable)
        String taskId = taskIdField.getText().trim();
        if (!taskId.isEmpty() && taskId.length() > 100) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                "Task ID is too long. Maximum length is 100 characters.\n\nCurrent length: " + taskId.length(),
                "Invalid Input",
                JOptionPane.ERROR_MESSAGE));
            return false;
        }
        
        // Settlement validation (optional but if provided, must be reasonable)
        String settlement = settlementField.getText().trim();
        if (!settlement.isEmpty() && settlement.length() > 255) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                "Settlement name is too long. Maximum length is 255 characters.\n\nCurrent length: " + settlement.length(),
                "Invalid Input",
                JOptionPane.ERROR_MESSAGE));
            return false;
        }
        
        // Mapper username validation (required)
        String mapper = (String) mapperUsernameComboBox.getSelectedItem();
        if (mapper == null || mapper.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                "Please select a mapper username from the dropdown.",
                "Invalid Input",
                JOptionPane.ERROR_MESSAGE));
            return false;
        }
        if (mapper.length() > 255) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                "Mapper username is too long. Maximum length is 255 characters.",
                "Invalid Input",
                JOptionPane.ERROR_MESSAGE));
            return false;
        }
        
        // Comments validation (optional but if provided, must not exceed limit)
        String comments = validatorCommentsArea.getText().trim();
        if (!comments.isEmpty() && comments.length() > 1000) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                "Validator comments are too long. Maximum length is 1000 characters.\n\nCurrent length: " + comments.length(),
                "Invalid Input",
                JOptionPane.ERROR_MESSAGE));
            return false;
        }
        
        // Total buildings validation (required, must be non-negative integer)
        String totalBuildingsStr = totalBuildingsField.getText().trim();
        try {
            int totalBuildings = Integer.parseInt(totalBuildingsStr);
            if (totalBuildings < 0) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Total buildings must be a positive number or zero.\n\nCurrent value: " + totalBuildings,
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE));
                return false;
            }
        } catch (NumberFormatException e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                "Total buildings must be a valid number.\n\nCurrent value: \"" + totalBuildingsStr + "\"",
                "Invalid Input",
                JOptionPane.ERROR_MESSAGE));
            return false;
        }
        
        // Error counts validation (all must be non-negative integers)
        for (int i = 0; i < errorCounts.length; i++) {
            if (errorCounts[i] < 0) {
                final int index = i;
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Error count for \"" + errorTypes[index] + "\" cannot be negative.\n\nCurrent value: " + errorCounts[index],
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE));
                return false;
            }
        }
        
        return true;
    }

    /**
     * Get the current JOSM user's OSM username from OAuth identity.
     * This replaces the old basic auth method and prevents disconnection warnings.
     * 
     * @return The current user's OSM username, or null if not authenticated
     */
    private String getCurrentValidator() {
        try {
            UserIdentityManager userManager = UserIdentityManager.getInstance();
            
            // Check authentication status
            if (userManager.isAnonymous()) {
                Logging.info("DPWValidationTool: User is anonymous (not authenticated)");
                return null;
            }
            
            String username = userManager.getUserName();
            if (username == null || username.trim().isEmpty()) {
                Logging.warn("DPWValidationTool: UserIdentityManager returned null/empty username");
                return null;
            }
            
            Logging.info("DPWValidationTool: Current validator: " + username);
            return username.trim();
            
        } catch (Exception e) {
            Logging.error("DPWValidationTool: Error getting current validator: " + e.getMessage());
            Logging.error(e);
            return null;
        }
    }

    private void submitData(String validationStatus) {
        // Validate all inputs before proceeding
        if (!validateInputs()) {
            return; // validateInputs() shows error dialog
        }
        
        // v3.0 - Store validation status for later use
        lastValidationStatus = validationStatus;
        
        // Get current OSM username from JOSM OAuth identity (not basic auth)
        String validatorUsername = getCurrentValidator();
        if (validatorUsername == null || validatorUsername.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Submission Failed: Cannot identify the current user.\n\n" +
                    "Please authenticate with OpenStreetMap in JOSM:\n" +
                    "1. Go to Edit → Preferences → Connection Settings\n" +
                    "2. Click 'Authorize now' to authenticate with OSM\n" +
                    "3. Complete the OAuth authorization process\n\n" +
                    "This plugin uses JOSM's OAuth 2.0 authentication.",
                    "Authentication Required", JOptionPane.ERROR_MESSAGE));
            return;
        }

        synchronized (authorizedMappers) {
            if (authorizedMappers.isEmpty()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                        "Submission Failed: The list of authorized project members has not been loaded. Please click 'Refresh Mapper List' and try again.",
                        "Submission Failed", JOptionPane.ERROR_MESSAGE));
                return;
            }
            // Case-insensitive username check (OSM usernames may have different casing)
            boolean validatorAuthorized = authorizedMappers.stream()
                .anyMatch(user -> user.equalsIgnoreCase(validatorUsername));
            
            if (!validatorAuthorized) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                        "Submission Failed: Your username ('" + validatorUsername + "') is not registered as a validator for this project.\n\n" +
                        "Please ensure:\n" +
                        "1. You are registered in the DPW Manager system\n" +
                        "2. Your account status is 'Active'\n" +
                        "3. Your OSM username matches exactly (contact project manager if needed)\n\n" +
                        "Click 'Refresh Mapper List' to reload the authorized users.",
                        "Submission Failed", JOptionPane.ERROR_MESSAGE));
                return;
            }
        }

        if (submittedThisSession) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Data has already been submitted in this session.", "Already Submitted", JOptionPane.INFORMATION_MESSAGE));
            return;
        }

        String taskId = taskIdField.getText();
        String mapperUsername = (String) mapperUsernameComboBox.getSelectedItem();
        if (mapperUsername == null) {
            mapperUsername = "";
        }
        final String finalMapperUsername = mapperUsername; // Make effectively final for lambda
        
        // Client-side authorization check: ensure mapper is in authorized list (case-insensitive)
        synchronized (authorizedMappers) {
            if (!authorizedMappers.isEmpty()) {
                boolean mapperAuthorized = authorizedMappers.stream()
                    .anyMatch(user -> user.equalsIgnoreCase(finalMapperUsername));
                
                if (!mapperAuthorized) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                        "Error: The selected mapper is not a registered participant in this project. Please select a valid user or refresh the mapper list.",
                        "Unauthorized Mapper", JOptionPane.ERROR_MESSAGE));
                    return;
                }
            }
        }
        
        String totalBuildings = totalBuildingsField.getText();
        String validatorComments = validatorCommentsArea.getText();
        String settlement = settlementField.getText().trim();

        int totalBuildingsInt = 0;
        try {
            totalBuildingsInt = Integer.parseInt(totalBuildings.trim());
        } catch (NumberFormatException e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                "Error: Total buildings must be a valid number.",
                "Invalid Input", JOptionPane.ERROR_MESSAGE));
            return;
        }

        // Build JSON payload according to v2.0 API spec
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");
        
        // Optional fields first
        if (taskId != null && !taskId.trim().isEmpty()) {
            jsonBuilder.append("\"task_id\": \"").append(jsonEscape(taskId)).append("\",");
        }
        if (settlement != null && !settlement.trim().isEmpty()) {
            jsonBuilder.append("\"settlement\": \"").append(jsonEscape(settlement)).append("\",");
        }
        
        // Required fields (per API spec)
        jsonBuilder.append("\"mapper_osm_username\": \"").append(jsonEscape(finalMapperUsername)).append("\",")
            .append("\"validator_osm_username\": \"").append(jsonEscape(validatorUsername)).append("\",")
            .append("\"total_buildings\": ").append(totalBuildingsInt).append(",");

        // Error count fields (optional, will default to 0 on server if not provided)
        for (int i = 0; i < errorTypes.length; i++) {
            String errorKey = "error_" + errorTypes[i].toLowerCase().replace(' ', '_');
            jsonBuilder.append("\"").append(errorKey).append("\": ").append(errorCounts[i]).append(",");
        }

        // Validation status and comments
        jsonBuilder.append("\"validation_status\": \"").append(jsonEscape(validationStatus)).append("\"");
        
        if (validatorComments != null && !validatorComments.trim().isEmpty()) {
            jsonBuilder.append(",\"validator_comments\": \"").append(jsonEscape(validatorComments)).append("\"");
        }
        
        jsonBuilder.append("}");

        sendPostRequest(jsonBuilder.toString());
    }

    /**
     * Send validation data to the DPW Manager API (/api/validation-log endpoint).
     * Implements v2.1 API specification with proper JSON response parsing and error handling.
     */
    private void sendPostRequest(String jsonData) {
        new Thread(() -> {
            setSending(true);
            
            // v2.1: Use Vercel-hosted DPW Manager API
            String baseUrl = Preferences.main().get("dpw.api_base_url", "https://dpw-mauve.vercel.app");
            String apiUrl = baseUrl + "/api/validation-log";
            
            try {
                Logging.info("DPWValidationTool: Submitting validation data to " + apiUrl);
                Logging.debug("DPWValidationTool: JSON payload: " + jsonData);
                
                URL url = new URI(apiUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                // Write JSON payload
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                Logging.info("DPWValidationTool: API responded with HTTP " + responseCode);
                
                // Read response body
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), 
                        StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }
                
                String responseBody = response.toString();
                Logging.debug("DPWValidationTool: API response body: " + responseBody);

                // Handle different response codes according to API spec
                if (responseCode == 201) {
                    // Success: 201 Created
                    try {
                        // Parse success response to extract log_id and names
                        Pattern logIdPattern = Pattern.compile("\"log_id\"\\s*:\\s*(\\d+)");
                        Pattern mapperNamePattern = Pattern.compile("\"mapper_name\"\\s*:\\s*\"([^\"]+)\"");
                        Pattern validatorNamePattern = Pattern.compile("\"validator_name\"\\s*:\\s*\"([^\"]+)\"");
                        
                        Matcher logIdMatcher = logIdPattern.matcher(responseBody);
                        Matcher mapperNameMatcher = mapperNamePattern.matcher(responseBody);
                        Matcher validatorNameMatcher = validatorNamePattern.matcher(responseBody);
                        
                        String logId = logIdMatcher.find() ? logIdMatcher.group(1) : "unknown";
                        String mapperName = mapperNameMatcher.find() ? mapperNameMatcher.group(1) : "";
                        String validatorName = validatorNameMatcher.find() ? validatorNameMatcher.group(1) : "";
                        
                        submittedThisSession = true;
                        currentState = ValidationState.SUBMITTED; // v3.0 - Update state
                        
                        // v3.0.1 - Store log_id for cloud upload
                        try {
                            lastValidationLogId = Integer.parseInt(logId);
                        } catch (NumberFormatException nfe) {
                            Logging.warn("DPWValidationTool: Could not parse log_id: " + logId);
                            lastValidationLogId = -1;
                        }
                        
                        SwingUtilities.invokeLater(() -> {
                            String successMsg = "✓ Validation log created successfully!\n\n" +
                                "Log ID: " + logId + "\n" +
                                "Mapper: " + mapperName + "\n" +
                                "Validator: " + validatorName + "\n\n" +
                                "Data has been recorded in the DPW Manager system.";
                            JOptionPane.showMessageDialog(null, successMsg, "Submission Successful", 
                                JOptionPane.INFORMATION_MESSAGE);
                            
                            // v3.0 - Show export dialog for Validated submissions only
                            if ("Validated".equals(lastValidationStatus)) {
                                showExportDialog();
                            } else {
                                // For rejected submissions, just ask to reset
                                int choice = JOptionPane.showConfirmDialog(null,
                                    "Would you like to start a new validation session?\n\n" +
                                    "This will clear the form and allow you to validate another task.",
                                    "Start New Session?",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.QUESTION_MESSAGE);
                                
                                if (choice == JOptionPane.YES_OPTION) {
                                    resetValidationSession();
                                }
                            }
                        });
                        
                        Logging.info("DPWValidationTool: Submission successful. Log ID: " + logId);
                        
                    } catch (Exception e) {
                        Logging.warn("DPWValidationTool: Could not parse success response details: " + e.getMessage());
                        submittedThisSession = true;
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, 
                            "Data submitted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE));
                    }
                    
                } else if (responseCode == 400) {
                    // Bad Request: Missing or invalid fields
                    String errorMsg = extractErrorMessage(responseBody);
                    Logging.error("DPWValidationTool: 400 Bad Request - " + errorMsg);
                    
                    SwingUtilities.invokeLater(() -> {
                        String userMsg = "Submission Failed: Invalid Data\n\n" + errorMsg + 
                            "\n\nPlease check that all required fields are filled correctly.";
                        JOptionPane.showMessageDialog(null, userMsg, "Validation Error", 
                            JOptionPane.ERROR_MESSAGE);
                    });
                    
                } else if (responseCode == 404) {
                    // Not Found: Mapper or Validator username not found in database
                    String errorMsg = extractErrorMessage(responseBody);
                    Logging.error("DPWValidationTool: 404 Not Found - " + errorMsg);
                    
                    SwingUtilities.invokeLater(() -> {
                        String userMsg = "Submission Failed: User Not Found\n\n" + errorMsg + 
                            "\n\nThe mapper or validator username is not registered in the DPW Manager system.\n" +
                            "Please contact your project manager to add this user.";
                        JOptionPane.showMessageDialog(null, userMsg, "User Not Found", 
                            JOptionPane.ERROR_MESSAGE);
                    });
                    
                } else if (responseCode >= 500) {
                    // Server Error
                    String errorMsg = extractErrorMessage(responseBody);
                    Logging.error("DPWValidationTool: 500 Server Error - " + errorMsg);
                    
                    SwingUtilities.invokeLater(() -> {
                        String userMsg = "Submission Failed: Server Error\n\n" +
                            "The DPW Manager server encountered an error while processing your request.\n" +
                            "Please try again later or contact support if the problem persists.\n\n" +
                            "Error: " + errorMsg;
                        JOptionPane.showMessageDialog(null, userMsg, "Server Error", 
                            JOptionPane.ERROR_MESSAGE);
                    });
                    
                } else {
                    // Unexpected response code
                    String errorMsg = extractErrorMessage(responseBody);
                    Logging.error("DPWValidationTool: Unexpected HTTP " + responseCode + " - " + errorMsg);
                    
                    SwingUtilities.invokeLater(() -> {
                        String userMsg = "Submission Failed: Unexpected Response\n\n" +
                            "HTTP " + responseCode + ": " + errorMsg;
                        JOptionPane.showMessageDialog(null, userMsg, "Submission Failed", 
                            JOptionPane.ERROR_MESSAGE);
                    });
                }
                
            } catch (Exception e) {
                Logging.error("DPWValidationTool: Exception during submission: " + e.getMessage());
                Logging.error(e);
                
                SwingUtilities.invokeLater(() -> {
                    String errorMsg = "An error occurred while submitting data:\n\n" + 
                        e.getClass().getSimpleName() + ": " + e.getMessage() + 
                        "\n\nPlease check your internet connection and try again.";
                    JOptionPane.showMessageDialog(null, errorMsg, "Submission Error", 
                        JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                setSending(false);
            }
        }).start();
    }
    
    /**
     * Extract error message from JSON error response.
     * Expected format: {"success": false, "error": "Error message here"}
     */
    private String extractErrorMessage(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            return "No error message provided";
        }
        
        try {
            // Simple regex to extract "error" field value
            Pattern errorPattern = Pattern.compile("\"error\"\\s*:\\s*\"([^\"]+)\"");
            Matcher errorMatcher = errorPattern.matcher(jsonResponse);
            
            if (errorMatcher.find()) {
                return errorMatcher.group(1);
            }
        } catch (Exception e) {
            Logging.trace("DPWValidationTool: Could not parse error message: " + e.getMessage());
        }
        
        // Fallback: return raw response (truncated if too long)
        if (jsonResponse.length() > 200) {
            return jsonResponse.substring(0, 200) + "...";
        }
        return jsonResponse;
    }

    private void updateSubmitButtonsEnabled() {
        SwingUtilities.invokeLater(() -> {
            boolean hasTaskId = taskIdField != null && !taskIdField.getText().trim().isEmpty();
            boolean mapperListLoaded;
            synchronized (authorizedMappers) {
                mapperListLoaded = !authorizedMappers.isEmpty();
            }
            boolean enable = hasTaskId && mapperListLoaded && !isSending;
            validateButton.setEnabled(enable);
            invalidateButton.setEnabled(enable);
        });
    }

    @Override
    public void destroy() {
        try {
            super.destroy();
        } catch (IllegalArgumentException iae) {
            // ToggleDialog.destroy may attempt to remove a preference listener that
            // was never registered or already removed. Swallow this to avoid crash on shutdown.
            Logging.warn("DPWValidationTool: destroy() - ignored IllegalArgumentException: " + iae.getMessage());
        } catch (Throwable t) {
            Logging.error("DPWValidationTool: unexpected error in destroy(): " + t);
            Logging.trace(t);
        }
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

    /**
     * Allow the plugin to set a title/icon for the dialog in a safe, reflective way
     * so we work across JOSM versions that may or may not expose a direct API.
     */
    public void setIcon(javax.swing.Icon icon) {
        if (icon == null) return;
        try {
            // Try to call a possible ToggleDialog.setTitleIcon(Icon) if present
            Class<?> sup = this.getClass().getSuperclass();
            try {
                java.lang.reflect.Method m = sup.getMethod("setTitleIcon", javax.swing.Icon.class);
                m.invoke(this, icon);
                return;
            } catch (NoSuchMethodException ignored) {}

            // Fallback: try setIcon
            try {
                java.lang.reflect.Method m2 = sup.getMethod("setIcon", javax.swing.Icon.class);
                m2.invoke(this, icon);
                return;
            } catch (NoSuchMethodException ignored) {}

            // As last resort try to set via a titlebar field if exists (best-effort)
            try {
                java.lang.reflect.Field tb = sup.getDeclaredField("titleBar");
                tb.setAccessible(true);
                Object titleBar = tb.get(this);
                if (titleBar != null) {
                    try {
                        java.lang.reflect.Method setIcon = titleBar.getClass().getMethod("setIcon", javax.swing.Icon.class);
                        setIcon.invoke(titleBar, icon);
                    } catch (NoSuchMethodException ignored) {}
                }
            } catch (NoSuchFieldException ignored) {}
        } catch (Exception e) {
            Logging.trace(e);
        }
    }

    private String getDateStringFromPicker() {
        if (datePickerComponent == null) return null;
        try {
            if (datePickerComponent instanceof JTextField) {
                String txt = ((JTextField) datePickerComponent).getText().trim();
                if (txt.isEmpty()) return null;
                // If already in YYYY-MM-DD, return as-is; otherwise attempt to normalize
                if (txt.matches("\\d{4}-\\d{2}-\\d{2}")) return txt;
                // try simple common formats: dd/MM/yyyy or dd-MM-yyyy
                Pattern p = Pattern.compile("(\\d{1,2})[\\/\\-](\\d{1,2})[\\/\\-](\\d{4})");
                Matcher m = p.matcher(txt);
                if (m.find()) {
                    String day = String.format("%02d", Integer.parseInt(m.group(1)));
                    String month = String.format("%02d", Integer.parseInt(m.group(2)));
                    String year = m.group(3);
                    return year + "-" + month + "-" + day;
                }
                // else return raw text
                return txt;
            } else {
                // attempt to extract value from JDatePickerImpl via reflection
                Object picker = datePickerComponent;
                try {
                    java.lang.reflect.Method getModel = picker.getClass().getMethod("getModel");
                    Object model = getModel.invoke(picker);
                    if (model != null) {
                        // try getValue()
                        try {
                            java.lang.reflect.Method getValue = model.getClass().getMethod("getValue");
                            Object val = getValue.invoke(model);
                            if (val != null) return val.toString();
                        } catch (NoSuchMethodException ignored) {}
                        // try year/month/day getters
                        try {
                            java.lang.reflect.Method getYear = model.getClass().getMethod("getYear");
                            java.lang.reflect.Method getMonth = model.getClass().getMethod("getMonth");
                            java.lang.reflect.Method getDay = model.getClass().getMethod("getDay");
                            Object y = getYear.invoke(model);
                            Object mo = getMonth.invoke(model);
                            Object d = getDay.invoke(model);
                            if (y != null && mo != null && d != null) {
                                int year = Integer.parseInt(y.toString());
                                int month = Integer.parseInt(mo.toString()) + 1; // JDatePicker months may be 0-based
                                int day = Integer.parseInt(d.toString());
                                return String.format("%04d-%02d-%02d", year, month, day);
                            }
                        } catch (NoSuchMethodException ignored) {}
                    }
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception e) {
            Logging.trace(e);
        }
        return null;
    }

    // ========================================================================================
    // v3.1.0-BETA: Tasking Manager Integration Methods
    // ========================================================================================

    /**
     * Handle TM URL input and auto-populate mapper/task information
     */
    private void handleTMUrlInput() {
        if (!PluginSettings.isTMIntegrationEnabled() || tmUrlField == null) {
            return;
        }

        String tmUrl = tmUrlField.getText().trim();
        if (tmUrl.isEmpty()) {
            return;
        }

        // Parse TM URL in background to avoid blocking UI
        new Thread(() -> {
            try {
                TaskManagerAPIClient.TaskInfo info = TaskManagerAPIClient.fetchTaskInfoFromURL(tmUrl);
                
                SwingUtilities.invokeLater(() -> {
                    if (info.success) {
                        // Auto-fill task ID
                        taskIdField.setText(String.valueOf(info.taskId));
                        
                        // Auto-select mapper if found
                        if (info.mapperUsername != null && !info.mapperUsername.isEmpty()) {
                            selectMapperInComboBox(info.mapperUsername);
                            
                            // Auto-fetch settlement if enabled
                            if (PluginSettings.isAutoFetchSettlement()) {
                                updateMapperSettlement();
                            }
                        }
                        
                        Logging.info("TM integration: Auto-populated from " + tmUrl);
                    } else {
                        Logging.warn("TM integration: " + info.errorMessage);
                    }
                });
            } catch (Exception e) {
                Logging.error("TM integration error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Check remote control for TM task information
     * Parses changeset comments for #hotosm-project-XXXXX-task-YYY format
     */
    private void checkRemoteControlForTMTask() {
        if (!PluginSettings.isTMIntegrationEnabled() || 
            !PluginSettings.isRemoteControlDetectionEnabled()) {
            return;
        }

        try {
            // Get active data layer
            OsmDataLayer layer = MainApplication.getLayerManager().getActiveDataLayer();
            if (layer == null) {
                return;
            }

            // Get changeset comment from layer dataset
            DataSet ds = layer.getDataSet();
            if (ds == null) {
                return;
            }

            // Try to get changeset comment from layer name or associated changeset
            // When JOSM loads via remote control, the changeset comment is often in the layer name
            String changesetComment = layer.getName();
            if (changesetComment == null || changesetComment.trim().isEmpty()) {
                return;
            }

            // Parse changeset comment for TM task info
            int[] taskInfo = TaskManagerAPIClient.parseChangesetComment(changesetComment);
            if (taskInfo == null) {
                return;
            }

            int projectId = taskInfo[0];
            int taskId = taskInfo[1];

            Logging.info("TM integration: Detected task from remote control - project " + projectId + " task " + taskId);

            // Fetch mapper info in background
            new Thread(() -> {
                TaskManagerAPIClient.TaskInfo info = TaskManagerAPIClient.fetchTaskInfo(projectId, taskId);
                
                SwingUtilities.invokeLater(() -> {
                    if (info.success) {
                        // Auto-fill fields
                        taskIdField.setText(String.valueOf(taskId));
                        
                        if (info.mapperUsername != null && !info.mapperUsername.isEmpty()) {
                            selectMapperInComboBox(info.mapperUsername);
                            
                            if (PluginSettings.isAutoFetchSettlement()) {
                                updateMapperSettlement();
                            }
                        }
                        
                        // Show notification
                        JOptionPane.showMessageDialog(
                            ValidationToolPanel.this,
                            "<html><b>Task Manager Task Detected!</b><br>" +
                            "Project: " + projectId + "<br>" +
                            "Task: " + taskId + "<br>" +
                            "Mapper: " + info.mapperUsername + "</html>",
                            "TM Auto-Detection",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        
                        Logging.info("TM integration: Auto-populated from remote control");
                    } else {
                        Logging.warn("TM integration: " + info.errorMessage);
                    }
                });
            }).start();

        } catch (Exception e) {
            Logging.error("TM remote control detection error: " + e.getMessage());
        }
    }

    /**
     * Select a mapper in the combo box by username
     */
    private void selectMapperInComboBox(String username) {
        if (username == null || username.trim().isEmpty()) {
            return;
        }

        for (int i = 0; i < mapperUsernameComboBox.getItemCount(); i++) {
            String item = mapperUsernameComboBox.getItemAt(i);
            if (item != null && item.equals(username)) {
                mapperUsernameComboBox.setSelectedIndex(i);
                return;
            }
        }

        // Mapper not found in authorized list - add warning
        Logging.warn("TM mapper '" + username + "' not found in authorized mapper list");
        JOptionPane.showMessageDialog(
            this,
            "<html><b>Warning:</b> Mapper '" + username + "' is not in the authorized mapper list.<br>" +
            "Please verify the mapper or refresh the mapper list.</html>",
            "Mapper Not Found",
            JOptionPane.WARNING_MESSAGE
        );
    }
}
