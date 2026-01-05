package org.openstreetmap.josm.plugins.dpwvalidationtool;

import org.openstreetmap.josm.tools.Logging;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class for displaying standard dialogs in the DPW Validation Tool.
 * Centralizes all JOptionPane calls to reduce code duplication and ensure consistency.
 * All methods automatically handle Swing threading via SwingUtilities.invokeLater().
 * 
 * @author Spatial Collective Ltd
 * @version 3.0.6
 * @since 3.0.6
 */
public final class DialogHelper {
    
    // Prevent instantiation
    private DialogHelper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Display an error message dialog.
     * Automatically runs on the Event Dispatch Thread.
     * 
     * @param title the dialog title
     * @param message the error message to display
     */
    public static void showError(String title, String message) {
        SwingUtilities.invokeLater(() -> 
            JOptionPane.showMessageDialog(
                null, 
                message, 
                title, 
                JOptionPane.ERROR_MESSAGE
            )
        );
    }
    
    /**
     * Display an error message dialog with a parent component.
     * Automatically runs on the Event Dispatch Thread.
     * 
     * @param parent the parent component
     * @param title the dialog title
     * @param message the error message to display
     */
    public static void showError(Component parent, String title, String message) {
        SwingUtilities.invokeLater(() -> 
            JOptionPane.showMessageDialog(
                parent, 
                message, 
                title, 
                JOptionPane.ERROR_MESSAGE
            )
        );
    }
    
    /**
     * Display a success/information message dialog.
     * Automatically runs on the Event Dispatch Thread.
     * 
     * @param title the dialog title
     * @param message the success message to display
     */
    public static void showSuccess(String title, String message) {
        SwingUtilities.invokeLater(() -> 
            JOptionPane.showMessageDialog(
                null, 
                message, 
                title, 
                JOptionPane.INFORMATION_MESSAGE
            )
        );
    }
    
    /**
     * Display a success/information message dialog with a parent component.
     * Automatically runs on the Event Dispatch Thread.
     * 
     * @param parent the parent component
     * @param title the dialog title
     * @param message the success message to display
     */
    public static void showSuccess(Component parent, String title, String message) {
        SwingUtilities.invokeLater(() -> 
            JOptionPane.showMessageDialog(
                parent, 
                message, 
                title, 
                JOptionPane.INFORMATION_MESSAGE
            )
        );
    }
    
    /**
     * Display a warning message dialog.
     * Automatically runs on the Event Dispatch Thread.
     * 
     * @param title the dialog title
     * @param message the warning message to display
     */
    public static void showWarning(String title, String message) {
        SwingUtilities.invokeLater(() -> 
            JOptionPane.showMessageDialog(
                null, 
                message, 
                title, 
                JOptionPane.WARNING_MESSAGE
            )
        );
    }
    
    /**
     * Display a warning message dialog with a parent component.
     * Automatically runs on the Event Dispatch Thread.
     * 
     * @param parent the parent component
     * @param title the dialog title
     * @param message the warning message to display
     */
    public static void showWarning(Component parent, String title, String message) {
        SwingUtilities.invokeLater(() -> 
            JOptionPane.showMessageDialog(
                parent, 
                message, 
                title, 
                JOptionPane.WARNING_MESSAGE
            )
        );
    }
    
    /**
     * Display a confirmation dialog (Yes/No).
     * This method blocks until the user responds.
     * 
     * @param title the dialog title
     * @param message the confirmation message
     * @return true if user clicked Yes, false otherwise
     */
    public static boolean showConfirmation(String title, String message) {
        AtomicBoolean result = new AtomicBoolean(false);
        
        if (SwingUtilities.isEventDispatchThread()) {
            // Already on EDT, show directly
            int choice = JOptionPane.showConfirmDialog(
                null, 
                message, 
                title,
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE
            );
            return choice == JOptionPane.YES_OPTION;
        } else {
            // Not on EDT, use invokeAndWait to block
            try {
                SwingUtilities.invokeAndWait(() -> {
                    int choice = JOptionPane.showConfirmDialog(
                        null, 
                        message, 
                        title,
                        JOptionPane.YES_NO_OPTION, 
                        JOptionPane.QUESTION_MESSAGE
                    );
                    result.set(choice == JOptionPane.YES_OPTION);
                });
            } catch (Exception e) {
                Logging.error("DialogHelper: Error showing confirmation dialog: " + e.getMessage());
                Logging.trace(e);
                return false;
            }
            return result.get();
        }
    }
    
    /**
     * Display a confirmation dialog (Yes/No) with a parent component.
     * This method blocks until the user responds.
     * 
     * @param parent the parent component
     * @param title the dialog title
     * @param message the confirmation message
     * @return true if user clicked Yes, false otherwise
     */
    public static boolean showConfirmation(Component parent, String title, String message) {
        AtomicBoolean result = new AtomicBoolean(false);
        
        if (SwingUtilities.isEventDispatchThread()) {
            // Already on EDT, show directly
            int choice = JOptionPane.showConfirmDialog(
                parent, 
                message, 
                title,
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE
            );
            return choice == JOptionPane.YES_OPTION;
        } else {
            // Not on EDT, use invokeAndWait to block
            try {
                SwingUtilities.invokeAndWait(() -> {
                    int choice = JOptionPane.showConfirmDialog(
                        parent, 
                        message, 
                        title,
                        JOptionPane.YES_NO_OPTION, 
                        JOptionPane.QUESTION_MESSAGE
                    );
                    result.set(choice == JOptionPane.YES_OPTION);
                });
            } catch (Exception e) {
                Logging.error("DialogHelper: Error showing confirmation dialog: " + e.getMessage());
                Logging.trace(e);
                return false;
            }
            return result.get();
        }
    }
    
    /**
     * Display a progress dialog with indeterminate progress bar.
     * The dialog is non-modal and can be dismissed by calling dispose() on the returned JDialog.
     * Automatically runs on the Event Dispatch Thread.
     * 
     * @param title the dialog title
     * @param message the progress message to display
     * @return the JDialog instance (call dispose() to close it)
     */
    public static JDialog showProgress(String title, String message) {
        JDialog[] dialogHolder = new JDialog[1];
        
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog();
            dialog.setTitle(title);
            dialog.setModal(false);
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            JLabel label = new JLabel(message);
            panel.add(label, BorderLayout.CENTER);
            
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            panel.add(progressBar, BorderLayout.SOUTH);
            
            dialog.add(panel);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            
            dialogHolder[0] = dialog;
        });
        
        // Wait for dialog to be created
        while (dialogHolder[0] == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return dialogHolder[0];
    }
    
    /**
     * Display an input dialog to get text from the user.
     * This method blocks until the user responds.
     * 
     * @param title the dialog title
     * @param message the prompt message
     * @param initialValue the initial value in the text field (can be null)
     * @return the user's input, or null if cancelled
     */
    public static String showInput(String title, String message, String initialValue) {
        String[] resultHolder = new String[1];
        
        if (SwingUtilities.isEventDispatchThread()) {
            // Already on EDT, show directly
            return (String) JOptionPane.showInputDialog(
                null,
                message,
                title,
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                initialValue
            );
        } else {
            // Not on EDT, use invokeAndWait to block
            try {
                SwingUtilities.invokeAndWait(() -> {
                    resultHolder[0] = (String) JOptionPane.showInputDialog(
                        null,
                        message,
                        title,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        null,
                        initialValue
                    );
                });
            } catch (Exception e) {
                Logging.error("DialogHelper: Error showing input dialog: " + e.getMessage());
                Logging.trace(e);
                return null;
            }
            return resultHolder[0];
        }
    }
    
    /**
     * Display a choice dialog with multiple options.
     * This method blocks until the user responds.
     * 
     * @param title the dialog title
     * @param message the prompt message
     * @param options array of option strings
     * @param defaultOption the default selected option (can be null)
     * @return the selected option, or null if cancelled
     */
    public static String showChoice(String title, String message, String[] options, String defaultOption) {
        String[] resultHolder = new String[1];
        
        if (SwingUtilities.isEventDispatchThread()) {
            // Already on EDT, show directly
            return (String) JOptionPane.showInputDialog(
                null,
                message,
                title,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                defaultOption
            );
        } else {
            // Not on EDT, use invokeAndWait to block
            try {
                SwingUtilities.invokeAndWait(() -> {
                    resultHolder[0] = (String) JOptionPane.showInputDialog(
                        null,
                        message,
                        title,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        defaultOption
                    );
                });
            } catch (Exception e) {
                Logging.error("DialogHelper: Error showing choice dialog: " + e.getMessage());
                Logging.trace(e);
                return null;
            }
            return resultHolder[0];
        }
    }
    
    /**
     * Close a dialog safely on the Event Dispatch Thread.
     * 
     * @param dialog the dialog to close (can be null)
     */
    public static void closeDialog(JDialog dialog) {
        if (dialog != null) {
            SwingUtilities.invokeLater(dialog::dispose);
        }
    }
}
