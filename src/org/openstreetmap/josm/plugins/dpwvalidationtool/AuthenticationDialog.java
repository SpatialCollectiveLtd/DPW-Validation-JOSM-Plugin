package org.openstreetmap.josm.plugins.dpwvalidationtool;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Logging;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Dialog for authenticating with custom OSM server.
 * Shows authentication status and allows login/logout.
 * 
 * @version 3.4.0
 * @since 3.4.0
 */
public class AuthenticationDialog extends JDialog {
    
    private JLabel statusLabel;
    private JLabel usernameLabel;
    private JButton loginButton;
    private JButton logoutButton;
    private JTextArea tokenInfoArea;
    private JProgressBar progressBar;
    
    private CustomOAuthClient oauthClient;
    private boolean authInProgress = false;
    
    public AuthenticationDialog() {
        super(MainApplication.getMainFrame(), "OSM Server Authentication", true);
        oauthClient = CustomOAuthClient.getInstance();
        initComponents();
        updateUI();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        int row = 0;
        
        // Header
        JLabel headerLabel = new JLabel("<html><b>Custom OSM Server Authentication</b></html>");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 16f));
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        mainPanel.add(headerLabel, gbc);
        
        // Server info
        OSMServerConfiguration config = OSMServerConfiguration.loadFromPreferences();
        JLabel serverLabel = new JLabel("Server: " + config.getOsmServerUrl());
        serverLabel.setFont(serverLabel.getFont().deriveFont(Font.PLAIN, 12f));
        serverLabel.setForeground(Color.GRAY);
        gbc.gridy = row++;
        mainPanel.add(serverLabel, gbc);
        
        // Separator
        gbc.gridy = row++;
        mainPanel.add(new JSeparator(), gbc);
        
        // Status
        gbc.gridwidth = 1;
        gbc.gridy = row;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Status:"), gbc);
        
        statusLabel = new JLabel("Checking...");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 1;
        mainPanel.add(statusLabel, gbc);
        row++;
        
        // Username
        gbc.gridx = 0;
        gbc.gridy = row;
        mainPanel.add(new JLabel("Username:"), gbc);
        
        usernameLabel = new JLabel("-");
        gbc.gridx = 1;
        mainPanel.add(usernameLabel, gbc);
        row++;
        
        // Progress bar
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        mainPanel.add(progressBar, gbc);
        
        // Separator
        gbc.gridy = row++;
        mainPanel.add(new JSeparator(), gbc);
        
        // Info section
        JLabel infoLabel = new JLabel("<html><i>Token Information (Debug)</i></html>");
        infoLabel.setForeground(Color.GRAY);
        gbc.gridy = row++;
        mainPanel.add(infoLabel, gbc);
        
        tokenInfoArea = new JTextArea(8, 40);
        tokenInfoArea.setEditable(false);
        tokenInfoArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        tokenInfoArea.setBackground(new Color(245, 245, 245));
        JScrollPane scrollPane = new JScrollPane(tokenInfoArea);
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        mainPanel.add(scrollPane, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        
        loginButton = new JButton("🔓 Login");
        loginButton.addActionListener(e -> login());
        buttonPanel.add(loginButton);
        
        logoutButton = new JButton("🔒 Logout");
        logoutButton.addActionListener(e -> logout());
        buttonPanel.add(logoutButton);
        
        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> updateUI());
        buttonPanel.add(refreshButton);
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Dialog settings
        setSize(650, 500);
        setLocationRelativeTo(MainApplication.getMainFrame());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        // Update UI when window is shown
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                updateUI();
            }
        });
    }
    
    /**
     * Update UI to reflect current authentication status
     */
    private void updateUI() {
        SwingUtilities.invokeLater(() -> {
            boolean authenticated = oauthClient.isAuthenticated();
            String username = oauthClient.getUsername();
            
            if (authenticated && username != null) {
                statusLabel.setText("✅ Authenticated");
                statusLabel.setForeground(new Color(0, 128, 0));
                usernameLabel.setText(username);
                loginButton.setEnabled(false);
                logoutButton.setEnabled(true);
            } else {
                statusLabel.setText("❌ Not Authenticated");
                statusLabel.setForeground(new Color(200, 0, 0));
                usernameLabel.setText("-");
                loginButton.setEnabled(!authInProgress);
                logoutButton.setEnabled(false);
            }
            
            // Update token info
            try {
                String tokenInfo = oauthClient.getTokenInfo();
                tokenInfoArea.setText(tokenInfo);
            } catch (Exception e) {
                tokenInfoArea.setText("Error getting token info: " + e.getMessage());
            }
            
            progressBar.setVisible(authInProgress);
        });
    }
    
    /**
     * Initiate login flow
     */
    private void login() {
        if (authInProgress) {
            return;
        }
        
        authInProgress = true;
        loginButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("⏳ Authenticating...");
        statusLabel.setForeground(Color.BLUE);
        
        // Start authentication in background
        oauthClient.authenticate().thenAccept(success -> {
            SwingUtilities.invokeLater(() -> {
                authInProgress = false;
                progressBar.setVisible(false);
                progressBar.setIndeterminate(false);
                
                if (success) {
                    JOptionPane.showMessageDialog(this,
                        "<html><b>Authentication Successful!</b><br><br>" +
                        "You are now authenticated with the OSM server.<br>" +
                        "Username: " + oauthClient.getUsername() + "</html>",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "<html><b>Authentication Failed</b><br><br>" +
                        "Could not authenticate with the OSM server.<br>" +
                        "Please check your server configuration and try again.</html>",
                        "Authentication Failed",
                        JOptionPane.ERROR_MESSAGE);
                }
                
                updateUI();
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                authInProgress = false;
                progressBar.setVisible(false);
                progressBar.setIndeterminate(false);
                
                Logging.error("DPW OAuth: Authentication error: " + ex.getMessage());
                Logging.trace(ex);
                
                JOptionPane.showMessageDialog(this,
                    "<html><b>Authentication Error</b><br><br>" +
                    "An error occurred during authentication:<br>" +
                    ex.getMessage() + "<br><br>" +
                    "Please check the JOSM console for details.</html>",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                
                updateUI();
            });
            return null;
        });
    }
    
    /**
     * Logout and clear tokens
     */
    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "<html>Are you sure you want to logout?<br><br>" +
            "This will clear all stored authentication tokens.</html>",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            oauthClient.logout();
            updateUI();
            
            JOptionPane.showMessageDialog(this,
                "You have been logged out successfully.",
                "Logged Out",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Show authentication dialog
     */
    public static void showDialog() {
        SwingUtilities.invokeLater(() -> {
            AuthenticationDialog dialog = new AuthenticationDialog();
            dialog.setVisible(true);
        });
    }
    
    /**
     * Quick authentication check - shows dialog if not authenticated
     * 
     * @return true if authenticated, false otherwise
     */
    public static boolean ensureAuthenticated() {
        CustomOAuthClient client = CustomOAuthClient.getInstance();
        
        if (client.isAuthenticated()) {
            return true;
        }
        
        // Show dialog
        int choice = JOptionPane.showConfirmDialog(
            MainApplication.getMainFrame(),
            "<html><b>Authentication Required</b><br><br>" +
            "You need to authenticate with the OSM server to continue.<br><br>" +
            "Would you like to login now?</html>",
            "Authentication Required",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (choice == JOptionPane.YES_OPTION) {
            showDialog();
            // Check again after dialog
            return client.isAuthenticated();
        }
        
        return false;
    }
}
