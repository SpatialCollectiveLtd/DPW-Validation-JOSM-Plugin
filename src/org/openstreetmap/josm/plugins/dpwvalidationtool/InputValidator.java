package org.openstreetmap.josm.plugins.dpwvalidationtool;

/**
 * Utility class for validating and sanitizing user inputs.
 * Prevents injection attacks, data corruption, and invalid submissions.
 * All validation follows the DPW API specification field requirements.
 * 
 * @author Spatial Collective Ltd
 * @version 3.0.6
 * @since 3.0.6
 */
public final class InputValidator {
    
    // Prevent instantiation
    private InputValidator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Validates and sanitizes a task ID.
     * 
     * Rules:
     * - Not null or empty after trimming
     * - Maximum length: 100 characters
     * - Only alphanumeric characters and hyphens allowed
     * 
     * @param input the raw task ID input
     * @return the sanitized task ID
     * @throws ValidationException if validation fails
     */
    public static String validateTaskId(String input) throws ValidationException {
        if (input == null || input.trim().isEmpty()) {
            throw new ValidationException("Task ID cannot be empty");
        }
        
        String sanitized = input.trim();
        
        if (sanitized.length() > ValidationConstants.TASK_ID_MAX_LENGTH) {
            throw new ValidationException(
                "Task ID is too long (max " + ValidationConstants.TASK_ID_MAX_LENGTH + " characters)"
            );
        }
        
        if (!sanitized.matches(ValidationConstants.REGEX_TASK_ID)) {
            throw new ValidationException(
                "Task ID contains invalid characters. Only alphanumeric and hyphens allowed."
            );
        }
        
        return sanitized;
    }
    
    /**
     * Validates and sanitizes a username (OSM username).
     * 
     * Rules:
     * - Not null or empty after trimming
     * - Maximum length: 255 characters
     * - Only alphanumeric, spaces, underscores, and hyphens allowed
     * 
     * @param input the raw username input
     * @return the sanitized username
     * @throws ValidationException if validation fails
     */
    public static String validateUsername(String input) throws ValidationException {
        if (input == null || input.trim().isEmpty()) {
            throw new ValidationException("Username cannot be empty");
        }
        
        String sanitized = input.trim();
        
        if (sanitized.length() > ValidationConstants.USERNAME_MAX_LENGTH) {
            throw new ValidationException(
                "Username is too long (max " + ValidationConstants.USERNAME_MAX_LENGTH + " characters)"
            );
        }
        
        if (!sanitized.matches(ValidationConstants.REGEX_USERNAME)) {
            throw new ValidationException(
                "Username contains invalid characters. Only alphanumeric, spaces, underscores, and hyphens allowed."
            );
        }
        
        return sanitized;
    }
    
    /**
     * Validates and sanitizes a settlement name.
     * 
     * Rules:
     * - Can be empty (optional field)
     * - Maximum length: 255 characters
     * - Trims whitespace
     * 
     * @param input the raw settlement input
     * @return the sanitized settlement name (empty string if null/empty)
     * @throws ValidationException if validation fails
     */
    public static String validateSettlement(String input) throws ValidationException {
        if (input == null || input.trim().isEmpty()) {
            return ""; // Settlement is optional
        }
        
        String sanitized = input.trim();
        
        if (sanitized.length() > ValidationConstants.SETTLEMENT_MAX_LENGTH) {
            throw new ValidationException(
                "Settlement name is too long (max " + ValidationConstants.SETTLEMENT_MAX_LENGTH + " characters)"
            );
        }
        
        return sanitized;
    }
    
    /**
     * Validates and sanitizes validation comments.
     * 
     * Rules:
     * - Can be empty (optional field)
     * - Maximum length: 1000 characters
     * - Trims whitespace
     * - Removes potentially dangerous characters
     * 
     * @param input the raw comments input
     * @return the sanitized comments (empty string if null/empty)
     * @throws ValidationException if validation fails
     */
    public static String validateComments(String input) throws ValidationException {
        if (input == null || input.trim().isEmpty()) {
            return ""; // Comments are optional
        }
        
        String sanitized = input.trim();
        
        if (sanitized.length() > ValidationConstants.COMMENTS_MAX_LENGTH) {
            throw new ValidationException(
                "Comments are too long (max " + ValidationConstants.COMMENTS_MAX_LENGTH + " characters)"
            );
        }
        
        // Remove null bytes and other control characters (except newlines and tabs)
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        return sanitized;
    }
    
    /**
     * Validates a total buildings count.
     * 
     * Rules:
     * - Must be a valid integer
     * - Must be greater than 0
     * 
     * @param input the raw total buildings input
     * @return the validated integer value
     * @throws ValidationException if validation fails
     */
    public static int validateTotalBuildings(String input) throws ValidationException {
        if (input == null || input.trim().isEmpty()) {
            throw new ValidationException("Total buildings count cannot be empty");
        }
        
        try {
            int count = Integer.parseInt(input.trim());
            
            if (count <= 0) {
                throw new ValidationException("Total buildings must be greater than 0");
            }
            
            return count;
        } catch (NumberFormatException e) {
            throw new ValidationException("Total buildings must be a valid number");
        }
    }
    
    /**
     * Validates an error count value.
     * 
     * Rules:
     * - Must be a valid integer
     * - Must be greater than or equal to 0
     * 
     * @param input the raw error count input
     * @param errorType the type of error being validated (for error messages)
     * @return the validated integer value
     * @throws ValidationException if validation fails
     */
    public static int validateErrorCount(String input, String errorType) throws ValidationException {
        if (input == null || input.trim().isEmpty()) {
            return 0; // Default to 0 if empty
        }
        
        try {
            int count = Integer.parseInt(input.trim());
            
            if (count < 0) {
                throw new ValidationException(errorType + " count cannot be negative");
            }
            
            return count;
        } catch (NumberFormatException e) {
            throw new ValidationException(errorType + " count must be a valid number");
        }
    }
    
    /**
     * Validates a date string in YYYY-MM-DD format.
     * 
     * Rules:
     * - Not null or empty
     * - Must match YYYY-MM-DD format
     * - Basic range validation (year 2000-2100, month 1-12, day 1-31)
     * 
     * @param input the raw date input
     * @return the validated date string
     * @throws ValidationException if validation fails
     */
    public static String validateDate(String input) throws ValidationException {
        if (input == null || input.trim().isEmpty()) {
            throw new ValidationException("Date cannot be empty");
        }
        
        String sanitized = input.trim();
        
        if (!sanitized.matches(ValidationConstants.REGEX_DATE_FORMAT)) {
            throw new ValidationException("Date must be in YYYY-MM-DD format");
        }
        
        // Parse and validate ranges
        String[] parts = sanitized.split("-");
        try {
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            
            if (year < 2000 || year > 2100) {
                throw new ValidationException("Year must be between 2000 and 2100");
            }
            
            if (month < 1 || month > 12) {
                throw new ValidationException("Month must be between 1 and 12");
            }
            
            if (day < 1 || day > 31) {
                throw new ValidationException("Day must be between 1 and 31");
            }
            
            // Basic month-day validation (not perfect, but catches obvious errors)
            if ((month == 4 || month == 6 || month == 9 || month == 11) && day > 30) {
                throw new ValidationException("Invalid day for month " + month);
            }
            
            if (month == 2 && day > 29) {
                throw new ValidationException("February cannot have more than 29 days");
            }
            
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid date format");
        }
        
        return sanitized;
    }
    
    /**
     * Validates a URL string.
     * 
     * Rules:
     * - Can be empty (optional field)
     * - If not empty, must be a valid HTTP or HTTPS URL
     * 
     * @param input the raw URL input
     * @return the sanitized URL (empty string if null/empty)
     * @throws ValidationException if validation fails
     */
    public static String validateUrl(String input) throws ValidationException {
        if (input == null || input.trim().isEmpty()) {
            return ""; // URL is optional
        }
        
        String sanitized = input.trim();
        
        // Basic URL validation
        if (!sanitized.matches("^https?://.*")) {
            throw new ValidationException("URL must start with http:// or https://");
        }
        
        // Try to parse as URL to ensure it's valid
        try {
            new java.net.URL(sanitized);
        } catch (java.net.MalformedURLException e) {
            throw new ValidationException("Invalid URL format: " + e.getMessage());
        }
        
        return sanitized;
    }
    
    /**
     * Validates a validation status value.
     * 
     * Rules:
     * - Must be either "Validated" or "Rejected"
     * 
     * @param input the raw status input
     * @return the validated status
     * @throws ValidationException if validation fails
     */
    public static String validateStatus(String input) throws ValidationException {
        if (input == null || input.trim().isEmpty()) {
            throw new ValidationException("Validation status cannot be empty");
        }
        
        String sanitized = input.trim();
        
        if (!sanitized.equals(ValidationConstants.STATUS_VALIDATED) && 
            !sanitized.equals(ValidationConstants.STATUS_REJECTED)) {
            throw new ValidationException(
                "Status must be '" + ValidationConstants.STATUS_VALIDATED + 
                "' or '" + ValidationConstants.STATUS_REJECTED + "'"
            );
        }
        
        return sanitized;
    }
    
    /**
     * Custom exception for validation errors.
     */
    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
        
        public ValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
