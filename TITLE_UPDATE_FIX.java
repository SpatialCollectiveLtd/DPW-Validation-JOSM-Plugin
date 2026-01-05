// Fix for Settings Title Not Updated During Update/Auto-Installation
// This file contains the recommended code changes to fix the issue

// ============================================================================
// FILE: ValidationToolPanel.java
// ============================================================================

// ADD THIS METHOD to ValidationToolPanel class:

/**
 * Refresh the dialog title with current version
 * Call this after plugin updates to display new version number
 * 
 * Note: This is a best-effort fix using reflection since JOSM's ToggleDialog
 * doesn't expose a public setTitle() method. The proper fix requires JOSM restart.
 */
public void refreshTitle() {
    String newTitle = I18n.tr("DPW Validation Tool v" + UpdateChecker.CURRENT_VERSION);
    Logging.info("DPWValidationTool: Refreshing title to: " + newTitle);
    
    try {
        // Attempt 1: Try to access and update the title field directly
        Class<?> parent = getClass().getSuperclass(); // ToggleDialog.class
        
        try {
            Field titleField = parent.getDeclaredField("title");
            titleField.setAccessible(true);
            titleField.set(this, newTitle);
            Logging.info("DPWValidationTool: Title field updated successfully");
        } catch (NoSuchFieldException e) {
            Logging.warn("DPWValidationTool: 'title' field not found in ToggleDialog");
        }
        
        // Attempt 2: Try to update the TitleBar component (if accessible)
        try {
            Field titleBarField = parent.getDeclaredField("titleBar");
            titleBarField.setAccessible(true);
            Object titleBar = titleBarField.get(this);
            
            if (titleBar != null) {
                // Try various methods that might exist
                Method[] methods = titleBar.getClass().getMethods();
                
                // Look for setTitle, setText, or similar methods
                for (Method method : methods) {
                    if (method.getName().equals("setTitle") || method.getName().equals("setText")) {
                        if (method.getParameterCount() == 1 && 
                            method.getParameterTypes()[0].equals(String.class)) {
                            method.invoke(titleBar, newTitle);
                            Logging.info("DPWValidationTool: TitleBar updated via " + method.getName());
                            break;
                        }
                    }
                }
                
                // Force repaint of title bar
                if (titleBar instanceof Component) {
                    ((Component) titleBar).revalidate();
                    ((Component) titleBar).repaint();
                }
            }
        } catch (NoSuchFieldException e) {
            Logging.warn("DPWValidationTool: 'titleBar' field not found in ToggleDialog");
        }
        
        // Force repaint of entire dialog
        revalidate();
        repaint();
        
        Logging.info("DPWValidationTool: Title refresh complete");
        
    } catch (Exception e) {
        Logging.warn("DPWValidationTool: Could not refresh dialog title: " + e.getMessage());
        Logging.trace(e);
    }
}

// ============================================================================
// FILE: DPWValidationToolPlugin.java
// ============================================================================

// ADD THIS: Make validationToolPanel accessible for title refresh
// Add getter method to DPWValidationToolPlugin class:

/**
 * Get the validation tool panel instance
 * Used for updating title after auto-updates
 */
public ValidationToolPanel getValidationToolPanel() {
    return validationToolPanel;
}

// MODIFY the constructor to store plugin instance:
// Add at the top of DPWValidationToolPlugin class:

private static DPWValidationToolPlugin instance;

public static DPWValidationToolPlugin getInstance() {
    return instance;
}

// Then in the constructor, add:
public DPWValidationToolPlugin(PluginInformation info) {
    super(info);
    instance = this; // Store instance for access from UpdateChecker
    
    // ... rest of constructor code
}

// ============================================================================
// FILE: UpdateChecker.java
// ============================================================================

// MODIFY applyPendingUpdate() method to refresh title after update:

public static void applyPendingUpdate() {
    try {
        String josmHome = System.getProperty("josm.home");
        if (josmHome == null) {
            josmHome = System.getProperty("user.home") + File.separator + ".josm";
        }
        Path pluginDir = Paths.get(josmHome, "plugins");
        Path updateFile = pluginDir.resolve("DPWValidationTool.jar.new");
        Path currentFile = pluginDir.resolve("DPWValidationTool.jar");
        Path backupFile = pluginDir.resolve("DPWValidationTool.jar.bak");
        
        if (Files.exists(updateFile)) {
            Logging.info("UpdateChecker: Found pending update, installing...");
            
            // Backup current version
            if (Files.exists(currentFile)) {
                Files.move(currentFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                Logging.info("UpdateChecker: Backed up current version");
            }
            
            // Install the new version
            Files.move(updateFile, currentFile, StandardCopyOption.REPLACE_EXISTING);
            Logging.info("UpdateChecker: Update installed successfully!");
            
            // Show success notification
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                    null,
                    "<html><b>DPW Validation Tool Updated!</b><br><br>"
                        + "The plugin has been successfully updated.<br>"
                        + "New version is now active.<br><br>"
                        + "<i>Note: JOSM restart recommended for full update.</i></html>",
                    "Update Complete",
                    JOptionPane.INFORMATION_MESSAGE
                );
                
                // NEW: Attempt to refresh title in existing panel
                try {
                    DPWValidationToolPlugin plugin = DPWValidationToolPlugin.getInstance();
                    if (plugin != null) {
                        ValidationToolPanel panel = plugin.getValidationToolPanel();
                        if (panel != null) {
                            Logging.info("UpdateChecker: Refreshing panel title after update");
                            panel.refreshTitle();
                        }
                    }
                } catch (Exception e) {
                    Logging.warn("UpdateChecker: Could not refresh panel title: " + e.getMessage());
                }
            });
            
            // Delete backup after success
            Files.deleteIfExists(backupFile);
        }
    } catch (Exception e) {
        Logging.error("UpdateChecker: Failed to apply pending update: " + e.getMessage());
        e.printStackTrace();
    }
}

// ============================================================================
// ALTERNATIVE FIX: Force JOSM to Recreate Dialog
// ============================================================================

// If reflection approach is unreliable, alternative is to:
// 1. Remove old dialog from MapFrame
// 2. Create new dialog with updated title
// 3. Add new dialog to MapFrame

// Add to DPWValidationToolPlugin:

public void recreateValidationPanel() {
    try {
        MapFrame map = MainApplication.getMap();
        if (map != null && validationToolPanel != null && panelAdded) {
            // Remove old dialog
            map.removeToggleDialog(validationToolPanel);
            Logging.info("DPWValidationTool: Removed old dialog");
            
            // Create new dialog with updated version
            validationToolPanel = new ValidationToolPanel();
            try {
                javax.swing.Icon ic = IconResources.getPirateIcon(24);
                validationToolPanel.setIcon(ic);
            } catch (Throwable ignore) {}
            
            // Add new dialog
            map.addToggleDialog(validationToolPanel);
            Logging.info("DPWValidationTool: Added new dialog with updated title");
            
            panelAdded = true;
        }
    } catch (Exception e) {
        Logging.error("DPWValidationTool: Failed to recreate panel: " + e.getMessage());
        Logging.trace(e);
    }
}

// Then call from UpdateChecker after update:
SwingUtilities.invokeLater(() -> {
    DPWValidationToolPlugin plugin = DPWValidationToolPlugin.getInstance();
    if (plugin != null) {
        plugin.recreateValidationPanel();
    }
});

// ============================================================================
// RECOMMENDED APPROACH
// ============================================================================

/**
 * BEST PRACTICE: Show update notification that recommends JOSM restart
 * 
 * The title is set in the ToggleDialog constructor which runs once.
 * The CURRENT_VERSION constant is loaded from the class file in memory.
 * When the JAR is replaced, the old class is still loaded in JVM.
 * 
 * The proper solution is to restart JOSM, but we can provide visual feedback:
 * 1. Use refreshTitle() method to update visible title (best effort)
 * 2. Show notification recommending JOSM restart
 * 3. Add visual indicator (e.g., "⚠ Restart JOSM" badge) to panel
 */

// Add to ValidationToolPanel:

private JLabel restartWarningLabel;

// In setupUI():
restartWarningLabel = new JLabel("<html>⚠ <b>Plugin updated!</b> Restart JOSM to apply fully.</html>");
restartWarningLabel.setForeground(new Color(200, 100, 0));
restartWarningLabel.setVisible(false);
// Add to top of panel

// Call when update is applied:
public void showRestartWarning() {
    SwingUtilities.invokeLater(() -> {
        if (restartWarningLabel != null) {
            restartWarningLabel.setVisible(true);
            revalidate();
            repaint();
        }
    });
}

// ============================================================================
// SUMMARY
// ============================================================================

/**
 * IMPLEMENTATION STEPS:
 * 
 * 1. Add refreshTitle() method to ValidationToolPanel
 * 2. Add getInstance() and getValidationToolPanel() to DPWValidationToolPlugin
 * 3. Modify UpdateChecker.applyPendingUpdate() to call refreshTitle()
 * 4. Add showRestartWarning() visual indicator
 * 5. Test with update workflow
 * 
 * EXPECTED BEHAVIOR:
 * - Title updates via reflection (may work on most JOSM versions)
 * - Warning banner shows "Restart JOSM to apply fully"
 * - After JOSM restart, title definitively shows new version
 * 
 * LIMITATIONS:
 * - Reflection may fail on future JOSM versions
 * - Full update requires JOSM restart
 * - This is a best-effort user experience improvement
 */
