package org.openstreetmap.josm.plugins.dpwvalidationtool;

import org.openstreetmap.josm.data.Preferences;
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

    private JTextField taskIdField;
    private JComboBox<String> mapperUsernameComboBox;
    private JTextField totalBuildingsField;
    private JTextArea validatorCommentsArea;
    private List<String> authorizedMappers = new ArrayList<>();
    private JLabel authStatusLabel;
    private JLabel fetchStatusLabel;
    private JButton validateButton;
    private JButton invalidateButton;
    private JButton refreshButton;
    private JButton refreshMapperListButton;
    private JButton forceSubmitButton;
    private javax.swing.JComponent datePickerComponent;
    private JButton isolateButton;
    private final JButton exportLayerButton = new JButton("Export Validated Layer");
    private volatile boolean allowForceSubmit = false;
    private volatile boolean isSending = false;
    private volatile boolean isFetchingMappers = false;
    private JDialog sendingDialog;
    private boolean submittedThisSession = false;

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
                    SwingUtilities.invokeLater(() -> {
                        fetchStatusLabel.setText("Error: Failed to fetch mapper list - " + e.getMessage());
                        fetchStatusLabel.setBackground(new Color(0xffcccc));
                        updateSubmitButtonsEnabled();
                    });
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

    // place combo and a small refresh button together but keep date/isolate compact on the right
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

    // Right-side compact controls: date and isolate button
    mpGbc.gridx = 2;
    mpGbc.weightx = 0;
    mpGbc.fill = GridBagConstraints.NONE;
    JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
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
    // keep date picker compact
    if (datePickerComponent instanceof JComponent) {
        ((JComponent) datePickerComponent).setPreferredSize(new Dimension(110, 24));
    }
    rightControls.add(new JLabel("Date:"));
    rightControls.add(datePickerComponent);
    // isolate button compact
    isolateButton = new JButton("Isolate");
    isolateButton.setToolTipText("Isolate work for selected mapper and date");
    isolateButton.setPreferredSize(new Dimension(120, 24));
    rightControls.add(isolateButton);
    mapperPanel.add(rightControls, mpGbc);

    panel.add(mapperPanel, gbc);

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

        // Action Buttons - place each button into the GridBag so they reflow with width
    validateButton = new JButton("Accept");
    validateButton.setToolTipText("Mark this task as validated (accept)");
    invalidateButton = new JButton("Reject");
    invalidateButton.setToolTipText("Mark this task as rejected (invalidate)");
    refreshButton = new JButton("Scan");
    refreshButton.setToolTipText("Rescan the active layer for buildings and mappers");
    forceSubmitButton = new JButton("Force");
    forceSubmitButton.setToolTipText("Force submit even if mapper not authorised (use with caution)");

    // Use a compact FlowLayout so buttons don't expand when the panel is narrowed
    JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    actionPanel.add(validateButton);
    actionPanel.add(invalidateButton);
    actionPanel.add(refreshButton);
    actionPanel.add(forceSubmitButton);

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
                JOptionPane.showMessageDialog(null, "Error refreshing layer: " + ex.getMessage(), "Refresh Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        forceSubmitButton.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(null, "Force submit will bypass the authorized mapper list. Continue?", "Confirm Force Submit", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                allowForceSubmit = true;
                forceSubmitButton.setEnabled(false);
                forceSubmitButton.setText("Force Submit (ON)");
            }
        });

        // Export button default disabled
        exportLayerButton.setEnabled(false);
        exportLayerButton.setToolTipText("Export the isolated validated layer to an .osm file");

    // Place export button on its own row below the other action buttons for clarity
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    exportPanel.add(exportLayerButton);
    panel.add(exportPanel, gbc);

        // Isolate action listener
        isolateButton.addActionListener(e -> {
            isolateButton.setEnabled(false);
            new Thread(() -> {
                try {
                    String mapper = (String) mapperUsernameComboBox.getSelectedItem();
                    if (mapper == null) mapper = "";
                    String dateString = getDateStringFromPicker();
                    if (dateString == null || dateString.isEmpty()) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Please select a date.", "No Date", JOptionPane.WARNING_MESSAGE));
                        return;
                    }
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
                    SwingUtilities.invokeLater(() -> {
                        exportLayerButton.setEnabled(true);
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

        // Export action listener
        exportLayerButton.addActionListener(e -> {
            new Thread(() -> {
                try {
                    org.openstreetmap.josm.gui.layer.Layer active = MainApplication.getLayerManager().getActiveLayer();
                    if (!(active instanceof OsmDataLayer)) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Active layer is not an OSM data layer.", "Cannot Export", JOptionPane.ERROR_MESSAGE));
                        return;
                    }
                    String taskId = taskIdField.getText().trim();
                    String mapper = (String) mapperUsernameComboBox.getSelectedItem();
                    if (mapper == null) mapper = "";
                    String dateString = getDateStringFromPicker();
                    String filename = String.format("Task_%s_%s_%s.osm", taskId.isEmpty() ? "unknown" : taskId, mapper, dateString == null ? "unknown" : dateString);
                    OsmDataLayer odl = (OsmDataLayer) active;
                    // Show a file chooser with a suggested filename
                    SwingUtilities.invokeLater(() -> {
                        JFileChooser chooser = new JFileChooser();
                        chooser.setDialogTitle("Export Validated Layer");
                        chooser.setSelectedFile(new java.io.File(filename));
                        int res = chooser.showSaveDialog(MainApplication.getMainFrame());
                        if (res != JFileChooser.APPROVE_OPTION) return;
                        java.io.File file = chooser.getSelectedFile();
                        try {
                            // Write DataSet to file using JOSM OsmWriter
                            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {
                                OsmWriter w = OsmWriterFactory.createOsmWriter(pw, true, org.openstreetmap.josm.io.OsmWriter.DEFAULT_API_VERSION);
                                w.write(odl.getDataSet());
                                pw.flush();
                                exportLayerButton.setEnabled(false);
                            }
                        } catch (Exception ex) {
                            Logging.error(ex);
                            JOptionPane.showMessageDialog(null, "Failed to export layer: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                } catch (Exception ex) {
                    Logging.error(ex);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Failed to export layer: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
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

        mapperUsernameComboBox.addItemListener(e -> updateAuthStatus());
    // when mapper selection changes, update the building count shown (mapper-specific)
    mapperUsernameComboBox.addItemListener(e -> updateMapperBuildingCount());
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
            refreshButton.setEnabled(!sending);
            refreshMapperListButton.setEnabled(!sending && !isFetchingMappers);
            forceSubmitButton.setEnabled(!sending && !allowForceSubmit);
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
     * Fetch the authorized mapper usernames from the configured mapper list URL.
     * Expects the endpoint to return a JSON array of strings, e.g. ["user1","user2"]
     */
    private void fetchAuthorizedMappers() throws Exception {
        String defaultUrl = "https://script.google.com/macros/s/AKfycbytfxwDkZG1qDpgXV4QcplBgq4u9PVSW7yQzfe47UG4dmM_nh5-D6mb7LZ4vxib9KQp/exec";
        String urlStr = Preferences.main().get("dpw.mapper_list_url", defaultUrl);
        if (urlStr == null || urlStr.trim().isEmpty()) {
            throw new IllegalStateException("Mapper list URL is not configured (dpw.mapper_list_url)");
        }
        // indicate fetching to the user
        SwingUtilities.invokeLater(() -> {
            fetchStatusLabel.setText("Fetching authorized users...");
            fetchStatusLabel.setBackground(Color.YELLOW);
            updateSubmitButtonsEnabled();
        });

        URL url = new URI(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int rc = conn.getResponseCode();
        if (rc < 200 || rc >= 300) {
            StringBuilder err = new StringBuilder();
            try (BufferedReader ebr = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String l;
                while ((l = ebr.readLine()) != null) err.append(l);
            } catch (Exception ignore) {}
            throw new IllegalStateException("Mapper list endpoint returned HTTP " + rc + ": " + err.toString());
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

        // Update UI to reflect success
        SwingUtilities.invokeLater(() -> {
            fetchStatusLabel.setText("Success: User list updated. Ready for validation.");
            fetchStatusLabel.setBackground(new Color(0x88ff88));
            updateAuthStatus();
            updateSubmitButtonsEnabled();
        });
    }


    private void submitData(String validationStatus) {
    // Validator pre-flight checks - get current OSM username from JOSM preferences
    String validatorUsername = Preferences.main().get("osm-server.username", "").trim();
        if (validatorUsername == null || validatorUsername.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Submission Failed: Cannot identify the current user. Please set your OSM username in JOSM's Connection Settings (Preferences > Connection Settings).",
                    "Submission Failed", JOptionPane.ERROR_MESSAGE));
            return;
        }

        synchronized (authorizedMappers) {
            if (authorizedMappers.isEmpty()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                        "Submission Failed: The list of authorized project members has not been loaded. Please click 'Refresh Mapper List' and try again.",
                        "Submission Failed", JOptionPane.ERROR_MESSAGE));
                return;
            }
            if (!authorizedMappers.contains(validatorUsername)) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                        "Submission Failed: Your username ('" + validatorUsername + "') is not registered as a validator for this project. Please contact the project manager.",
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
        // Client-side authorization check: ensure mapper is in authorized list
        synchronized (authorizedMappers) {
        if (!authorizedMappers.isEmpty() && !authorizedMappers.contains(mapperUsername)) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
            "Error: The selected mapper is not a registered participant in this project. Please select a valid user or refresh the mapper list.",
            "Unauthorized Mapper", JOptionPane.ERROR_MESSAGE));
        return;
        }
        }
        String totalBuildings = totalBuildingsField.getText();
    String validatorComments = validatorCommentsArea.getText();
    // Validator username previously retrieved above via User.getName()
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
            setSending(true);
            String baseUrl = "https://script.google.com/macros/s/AKfycbytfxwDkZG1qDpgXV4QcplBgq4u9PVSW7yQzfe47UG4dmM_nh5-D6mb7LZ4vxib9KQp/exec";
            Exception lastEx = null;
            int lastRc = -1;
            String lastResp = "";
            try {
                // First attempt: POST JSON with proper headers
                try {
                    URL url = new URI(baseUrl).toURL();
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json, text/plain, */*");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);

                    // Write JSON using try-with-resources to ensure stream closed/flushed
                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                        os.flush();
                    }

                    lastRc = conn.getResponseCode();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(
                            lastRc >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        lastResp = response.toString();
                    }

                    if (lastRc == HttpURLConnection.HTTP_OK) {
                        // success
                        submittedThisSession = true;
                        final String resp = lastResp == null ? "" : lastResp;
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Data submitted successfully!\nResponse: " + resp, "Success", JOptionPane.INFORMATION_MESSAGE));
                        return;
                    } else {
                        // Non-200 response: show server error body if any
                        final String resp = lastResp == null ? "" : lastResp;
                        final int rcSnapshot = lastRc;
                        final String respSnapshot = lastResp == null ? "" : lastResp;
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Submission failed. Server returned HTTP " + rcSnapshot + "\nResponse: " + respSnapshot, "Submission Failed", JOptionPane.ERROR_MESSAGE));
                        return;
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

                    // write form payload with try-with-resources
                    try (OutputStream os = conn2.getOutputStream()) {
                        byte[] input = form.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                        os.flush();
                    }

                    lastRc = conn2.getResponseCode();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(
                            lastRc >= 400 ? conn2.getErrorStream() : conn2.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response2 = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response2.append(responseLine.trim());
                        }
                        lastResp = response2.toString();
                    }

                    if (lastRc == HttpURLConnection.HTTP_OK) {
                        submittedThisSession = true;
                        final String resp = lastResp == null ? "" : lastResp;
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Data submitted successfully!\nResponse: " + resp, "Success", JOptionPane.INFORMATION_MESSAGE));
                        return;
                    } else {
                        final String resp = lastResp == null ? "" : lastResp;
                        final int rcSnapshot2 = lastRc;
                        final String respSnapshot2 = lastResp == null ? "" : lastResp;
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Submission failed. Server returned HTTP " + rcSnapshot2 + "\nResponse: " + respSnapshot2, "Submission Failed", JOptionPane.ERROR_MESSAGE));
                        return;
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
                        submittedThisSession = true;
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Data submitted successfully!\nResponse: " + resp, "Success", JOptionPane.INFORMATION_MESSAGE));
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
                        JOptionPane.showMessageDialog(null, "An error occurred during submission: " + exFinal.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(null, "Data submission failed. Code: " + rcFinal + "\nResponse: " + respFinal, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } finally {
                setSending(false);
            }

        }).start();
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
}