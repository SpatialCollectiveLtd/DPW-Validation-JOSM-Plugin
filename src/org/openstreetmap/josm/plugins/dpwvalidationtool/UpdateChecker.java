package org.openstreetmap.josm.plugins.dpwvalidationtool;

import org.openstreetmap.josm.tools.Logging;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Checks for plugin updates from GitHub releases
 * @version 3.1.0-BETA
 */
public class UpdateChecker {
    
    private static final String GITHUB_API_URL = "https://api.github.com/repos/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/releases/latest";
    private static final String GITHUB_RELEASES_URL = "https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/releases";
    private static final String CURRENT_VERSION = "3.1.0-BETA";
    
    /**
     * Check for updates in background and show notification if available
     */
    public static void checkForUpdatesAsync(boolean showNoUpdateMessage) {
        new Thread(() -> {
            try {
                UpdateInfo info = checkForUpdates();
                
                SwingUtilities.invokeLater(() -> {
                    if (info.updateAvailable) {
                        showUpdateAvailableDialog(info);
                    } else if (showNoUpdateMessage) {
                        JOptionPane.showMessageDialog(
                            null,
                            "<html><b>DPW Validation Tool is up to date!</b><br><br>" +
                            "Current version: " + CURRENT_VERSION + "<br>" +
                            "You are running the latest version.</html>",
                            "No Updates Available",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                });
            } catch (Exception e) {
                Logging.error("Failed to check for updates: " + e.getMessage());
                if (showNoUpdateMessage) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                            null,
                            "<html><b>Unable to check for updates</b><br><br>" +
                            "Error: " + e.getMessage() + "<br><br>" +
                            "Please check your internet connection and try again.</html>",
                            "Update Check Failed",
                            JOptionPane.WARNING_MESSAGE
                        );
                    });
                }
            }
        }).start();
    }
    
    /**
     * Check for updates synchronously
     */
    public static UpdateInfo checkForUpdates() throws Exception {
        URL url = new URL(GITHUB_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("GitHub API returned status: " + responseCode);
        }
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        String jsonResponse = response.toString();
        
        // Parse JSON manually
        String latestVersion = extractJsonStringField(jsonResponse, "tag_name");
        if (latestVersion == null) {
            throw new Exception("Could not parse version from GitHub API");
        }
        
        // Remove 'v' prefix if present
        if (latestVersion.startsWith("v")) {
            latestVersion = latestVersion.substring(1);
        }
        
        String releaseName = extractJsonStringField(jsonResponse, "name");
        String releaseNotes = extractJsonStringField(jsonResponse, "body");
        String downloadUrl = extractDownloadUrl(jsonResponse);
        
        boolean updateAvailable = isNewerVersion(latestVersion, CURRENT_VERSION);
        
        return new UpdateInfo(
            updateAvailable,
            latestVersion,
            CURRENT_VERSION,
            releaseName,
            releaseNotes,
            downloadUrl
        );
    }
    
    /**
     * Compare version strings
     */
    private static boolean isNewerVersion(String latest, String current) {
        try {
            // Remove BETA/ALPHA suffixes for comparison
            String latestClean = latest.replaceAll("-.*$", "");
            String currentClean = current.replaceAll("-.*$", "");
            
            String[] latestParts = latestClean.split("\\.");
            String[] currentParts = currentClean.split("\\.");
            
            int maxLength = Math.max(latestParts.length, currentParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int latestNum = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                int currentNum = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                
                if (latestNum > currentNum) {
                    return true;
                } else if (latestNum < currentNum) {
                    return false;
                }
            }
            
            // If base versions are equal, check if current is BETA and latest is stable
            if (current.contains("BETA") && !latest.contains("BETA")) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            Logging.warn("Error comparing versions: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Show update available dialog
     */
    private static void showUpdateAvailableDialog(UpdateInfo info) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Header
        JLabel headerLabel = new JLabel("<html><b style='font-size: 14px'>🎉 Update Available!</b></html>");
        panel.add(headerLabel, BorderLayout.NORTH);
        
        // Content
        JPanel contentPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        contentPanel.add(new JLabel("<html><b>Current version:</b> " + info.currentVersion + "</html>"));
        contentPanel.add(new JLabel("<html><b>Latest version:</b> " + info.latestVersion + "</html>"));
        
        if (info.releaseName != null && !info.releaseName.isEmpty()) {
            contentPanel.add(new JLabel("<html><b>Release:</b> " + info.releaseName + "</html>"));
        }
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        // Release notes preview
        if (info.releaseNotes != null && !info.releaseNotes.isEmpty()) {
            String preview = info.releaseNotes.length() > 200 
                ? info.releaseNotes.substring(0, 200) + "..." 
                : info.releaseNotes;
            JTextArea notesArea = new JTextArea(preview);
            notesArea.setEditable(false);
            notesArea.setLineWrap(true);
            notesArea.setWrapStyleWord(true);
            notesArea.setRows(5);
            notesArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            
            JPanel notesPanel = new JPanel(new BorderLayout(5, 5));
            notesPanel.add(new JLabel("<html><b>What's new:</b></html>"), BorderLayout.NORTH);
            notesPanel.add(new JScrollPane(notesArea), BorderLayout.CENTER);
            panel.add(notesPanel, BorderLayout.SOUTH);
        }
        
        // Buttons
        Object[] options = {"Download from GitHub", "Remind Me Later"};
        int result = JOptionPane.showOptionDialog(
            null,
            panel,
            "DPW Validation Tool - Update Available",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[0]
        );
        
        if (result == 0) { // Download
            try {
                Desktop.getDesktop().browse(new URI(GITHUB_RELEASES_URL));
            } catch (Exception e) {
                Logging.error("Failed to open browser: " + e.getMessage());
                JOptionPane.showMessageDialog(
                    null,
                    "<html><b>Please visit:</b><br>" + GITHUB_RELEASES_URL + "</html>",
                    "Download Update",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        }
    }
    
    /**
     * Extract download URL for JAR file from releases
     */
    private static String extractDownloadUrl(String json) {
        // Find assets array
        int assetsStart = json.indexOf("\"assets\"");
        if (assetsStart == -1) return null;
        
        int arrayStart = json.indexOf('[', assetsStart);
        if (arrayStart == -1) return null;
        
        // Look for .jar file
        int jarUrlStart = json.indexOf("\"browser_download_url\"", arrayStart);
        if (jarUrlStart == -1) return null;
        
        String url = extractJsonStringField(json.substring(jarUrlStart), "browser_download_url");
        return url != null && url.endsWith(".jar") ? url : null;
    }
    
    /**
     * Extract string field from JSON (manual parsing)
     */
    private static String extractJsonStringField(String json, String fieldName) {
        int fieldStart = json.indexOf("\"" + fieldName + "\"");
        if (fieldStart == -1) {
            return null;
        }
        
        int colonPos = json.indexOf(':', fieldStart);
        if (colonPos == -1) {
            return null;
        }
        
        int pos = colonPos + 1;
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }
        
        if (pos >= json.length()) {
            return null;
        }
        
        char firstChar = json.charAt(pos);
        if (firstChar == 'n' && json.startsWith("null", pos)) {
            return null;
        }
        
        if (firstChar != '"') {
            return null;
        }
        
        int stringStart = pos + 1;
        int stringEnd = stringStart;
        while (stringEnd < json.length()) {
            char c = json.charAt(stringEnd);
            if (c == '"' && (stringEnd == stringStart || json.charAt(stringEnd - 1) != '\\')) {
                break;
            }
            stringEnd++;
        }
        
        if (stringEnd >= json.length()) {
            return null;
        }
        
        return json.substring(stringStart, stringEnd);
    }
    
    /**
     * Update information container
     */
    public static class UpdateInfo {
        public final boolean updateAvailable;
        public final String latestVersion;
        public final String currentVersion;
        public final String releaseName;
        public final String releaseNotes;
        public final String downloadUrl;
        
        public UpdateInfo(boolean updateAvailable, String latestVersion, String currentVersion,
                         String releaseName, String releaseNotes, String downloadUrl) {
            this.updateAvailable = updateAvailable;
            this.latestVersion = latestVersion;
            this.currentVersion = currentVersion;
            this.releaseName = releaseName;
            this.releaseNotes = releaseNotes;
            this.downloadUrl = downloadUrl;
        }
    }
}
