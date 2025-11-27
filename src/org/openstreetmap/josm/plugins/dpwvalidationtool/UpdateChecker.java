package org.openstreetmap.josm.plugins.dpwvalidationtool;

import org.openstreetmap.josm.tools.Logging;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Checks for plugin updates from GitHub releases and auto-installs updates
 * @version 3.1.0-BETA
 */
public class UpdateChecker {
    
    // Changed from /releases/latest to /releases to include pre-releases and beta versions
    private static final String GITHUB_API_URL = "https://api.github.com/repos/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/releases";
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
     * Now checks ALL releases (including pre-releases and beta versions)
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
        
        // Parse JSON array of releases - find the most recent one by published_at date
        String latestRelease = findLatestRelease(jsonResponse);
        if (latestRelease == null || latestRelease.isEmpty()) {
            throw new Exception("No releases found on GitHub");
        }
        
        // Parse the latest release object
        String latestVersion = extractJsonStringField(latestRelease, "tag_name");
        if (latestVersion == null) {
            throw new Exception("Could not parse version from GitHub API");
        }
        
        // Remove 'v' prefix if present
        if (latestVersion.startsWith("v")) {
            latestVersion = latestVersion.substring(1);
        }
        
        String releaseName = extractJsonStringField(latestRelease, "name");
        String releaseNotes = extractJsonStringField(latestRelease, "body");
        String downloadUrl = extractDownloadUrl(latestRelease);
        
        // Debug logging
        Logging.info("UpdateChecker: Latest version from GitHub: " + latestVersion);
        Logging.info("UpdateChecker: Download URL extracted: " + (downloadUrl != null ? downloadUrl : "NULL - NOT FOUND!"));
        
        if (downloadUrl == null) {
            Logging.error("UpdateChecker: Failed to extract download URL from release JSON");
            Logging.error("UpdateChecker: Release JSON length: " + latestRelease.length() + " chars");
            // Log first 500 chars for debugging
            Logging.error("UpdateChecker: Release JSON preview: " + 
                latestRelease.substring(0, Math.min(500, latestRelease.length())));
        }
        
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
     * Find the latest release from the releases array (sorted by published_at descending)
     * GitHub returns releases in descending order by default, so first one is latest
     */
    private static String findLatestRelease(String jsonArray) {
        // The response is an array of release objects
        // Find the first release object (most recent)
        int firstReleaseStart = jsonArray.indexOf('{');
        if (firstReleaseStart == -1) return null;
        
        // Find matching closing brace
        int braceCount = 0;
        int pos = firstReleaseStart;
        while (pos < jsonArray.length()) {
            char c = jsonArray.charAt(pos);
            if (c == '{') braceCount++;
            else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    return jsonArray.substring(firstReleaseStart, pos + 1);
                }
            }
            pos++;
        }
        return null;
    }
    
    /**
     * Compare version strings
     * Returns true if latest version is newer than current OR if versions are equal
     * (to account for JAR updates within same version)
     */
    private static boolean isNewerVersion(String latest, String current) {
        try {
            // Exact match check - for beta releases, always show update notification
            // to ensure users get the latest JAR even within same version
            if (latest.equalsIgnoreCase(current)) {
                // For BETA versions, we want to notify about JAR updates
                // For stable versions, identical version means up to date
                return current.toUpperCase().contains("BETA");
            }
            
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
     * Show update available dialog with auto-install option
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
        Object[] options = {"Install Update", "Download Manually", "Remind Me Later"};
        int result = JOptionPane.showOptionDialog(
            null,
            panel,
            "DPW Validation Tool - Update Available",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[0]
        );
        
        if (result == 0) { // Install Update
            installUpdate(info);
        } else if (result == 1) { // Download Manually
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
     * Download and install the update automatically
     */
    private static void installUpdate(UpdateInfo info) {
        Logging.info("UpdateChecker: installUpdate called");
        Logging.info("UpdateChecker: Download URL: " + info.downloadUrl);
        Logging.info("UpdateChecker: Latest version: " + info.latestVersion);
        
        if (info.downloadUrl == null || info.downloadUrl.isEmpty()) {
            Logging.error("UpdateChecker: Download URL is null or empty!");
            JOptionPane.showMessageDialog(
                null,
                "<html><b>Download URL not found</b><br><br>" +
                "The GitHub release may not have a JAR file attached.<br>" +
                "Please download manually from GitHub.</html>",
                "Update Failed",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        // Show progress dialog
        JDialog progressDialog = new JDialog((Frame) null, "Downloading Update...", true);
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Connecting...");
        
        JPanel progressPanel = new JPanel(new BorderLayout(10, 10));
        progressPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        progressPanel.add(new JLabel("<html><b>Downloading DPW Validation Tool update...</b></html>"), BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.CENTER);
        
        JButton cancelButton = new JButton("Cancel");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(cancelButton);
        progressPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        progressDialog.setContentPane(progressPanel);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(null);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        final boolean[] cancelled = {false};
        cancelButton.addActionListener(e -> {
            cancelled[0] = true;
            progressDialog.dispose();
        });
        
        // Download in background thread
        new Thread(() -> {
            try {
                // Get plugin directory
                String josmHome = System.getProperty("josm.home");
                if (josmHome == null) {
                    josmHome = System.getProperty("user.home") + File.separator + ".josm";
                }
                Path pluginDir = Paths.get(josmHome, "plugins");
                if (!Files.exists(pluginDir)) {
                    Files.createDirectories(pluginDir);
                }
                
                Path targetFile = pluginDir.resolve("DPWValidationTool.jar");
                Path tempFile = pluginDir.resolve("DPWValidationTool.jar.tmp");
                
                // Download the JAR
                URL url = new URL(info.downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                
                int responseCode = conn.getResponseCode();
                Logging.info("UpdateChecker: Download response code: " + responseCode);
                
                if (responseCode != 200) {
                    throw new Exception("Download failed with HTTP status: " + responseCode);
                }
                
                long fileSize = conn.getContentLengthLong();
                Logging.info("UpdateChecker: File size from server: " + fileSize + " bytes");
                
                if (fileSize <= 0) {
                    Logging.warn("UpdateChecker: Server did not provide content length, will download without progress");
                }
                
                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(tempFile.toFile())) {
                    
                    byte[] buffer = new byte[8192];
                    long downloaded = 0;
                    int bytesRead;
                    
                    while ((bytesRead = in.read(buffer)) != -1 && !cancelled[0]) {
                        out.write(buffer, 0, bytesRead);
                        downloaded += bytesRead;
                        
                        final long currentDownloaded = downloaded;
                        
                        if (fileSize > 0) {
                            final int progress = (int) ((downloaded * 100) / fileSize);
                            final long downloadedKB = downloaded / 1024;
                            final long totalKB = fileSize / 1024;
                            
                            SwingUtilities.invokeLater(() -> {
                                progressBar.setValue(progress);
                                progressBar.setString(String.format("Downloading... %d KB / %d KB (%d%%)", 
                                    downloadedKB, totalKB, progress));
                            });
                        } else {
                            // No content length, show bytes downloaded
                            final long downloadedKB = downloaded / 1024;
                            SwingUtilities.invokeLater(() -> {
                                progressBar.setIndeterminate(true);
                                progressBar.setString(String.format("Downloading... %d KB", downloadedKB));
                            });
                        }
                    }
                    
                    Logging.info("UpdateChecker: Download complete. Total bytes: " + downloaded);
                }
                
                if (cancelled[0]) {
                    Logging.info("UpdateChecker: Download cancelled by user");
                    Files.deleteIfExists(tempFile);
                    SwingUtilities.invokeLater(progressDialog::dispose);
                    return;
                }
                
                // Replace the old JAR with the new one
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(100);
                    progressBar.setString("Installing...");
                });
                
                // Backup old file
                Path backupFile = pluginDir.resolve("DPWValidationTool.jar.bak");
                if (Files.exists(targetFile)) {
                    Files.move(targetFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                }
                
                // Move temp file to target
                Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                
                // Delete backup on success
                Files.deleteIfExists(backupFile);
                
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    
                    // Show success message
                    int restart = JOptionPane.showConfirmDialog(
                        null,
                        "<html><b>Update installed successfully!</b><br><br>" +
                        "DPW Validation Tool has been updated to version " + info.latestVersion + ".<br><br>" +
                        "<b>You must restart JOSM for the changes to take effect.</b><br><br>" +
                        "Would you like to restart JOSM now?</html>",
                        "Update Complete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    
                    if (restart == JOptionPane.YES_OPTION) {
                        // Request JOSM restart
                        JOptionPane.showMessageDialog(
                            null,
                            "<html><b>Please restart JOSM now</b><br><br>" +
                            "Close JOSM and reopen it to use the updated plugin.<br><br>" +
                            "The update has been installed successfully.</html>",
                            "Restart Required",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        // Exit JOSM to force restart
                        System.exit(0);
                    }
                });
                
            } catch (Exception e) {
                Logging.error("Failed to install update: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(
                        null,
                        "<html><b>Update installation failed</b><br><br>" +
                        "Error: " + e.getMessage() + "<br><br>" +
                        "Please download the update manually from GitHub.</html>",
                        "Update Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                });
            }
        }).start();
        
        progressDialog.setVisible(true);
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
