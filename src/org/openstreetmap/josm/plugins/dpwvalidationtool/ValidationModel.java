package org.openstreetmap.josm.plugins.dpwvalidationtool;

import java.util.*;

/**
 * Data model for validation workflow.
 * Stores all validation-related data separate from UI logic.
 * 
 * This model follows the Single Responsibility Principle by handling
 * only data storage and validation rules, while UI components handle
 * presentation and user interaction.
 * 
 * @author Spatial Collective Ltd
 * @version 3.0.6
 * @since 3.0.6
 */
public class ValidationModel {
    
    /**
     * Validation workflow states.
     */
    public enum ValidationState {
        /** Initial state - no work started */
        IDLE,
        
        /** User list being fetched from API */
        FETCHING_USERS,
        
        /** User list loaded successfully */
        USERS_LOADED,
        
        /** Data isolation in progress */
        ISOLATING,
        
        /** Data successfully isolated */
        ISOLATED,
        
        /** Validation submitted to API */
        SUBMITTED,
        
        /** Data exported to file */
        EXPORTED
    }
    
    // ========== Form Data Fields ==========
    
    private String taskId;
    private String settlement;
    private String mapperUsername;
    private String filterDate;
    private int totalBuildings;
    private String validatorComments;
    private String validationStatus; // "Validated" or "Rejected"
    
    // ========== Error Tracking Fields ==========
    
    private final Map<String, Integer> errorCounts;
    
    // ========== Workflow State ==========
    
    private ValidationState currentState;
    private boolean submittedThisSession;
    private int lastValidationLogId;
    private String googleDriveFileUrl;
    
    // ========== Layer References ==========
    
    private Object isolatedLayer; // OsmDataLayer reference (using Object to avoid JOSM dependency in model)
    
    // ========== Constructor ==========
    
    /**
     * Create a new validation model with default values.
     */
    public ValidationModel() {
        this.taskId = "";
        this.settlement = "";
        this.mapperUsername = "";
        this.filterDate = "";
        this.totalBuildings = 0;
        this.validatorComments = "";
        this.validationStatus = ValidationConstants.STATUS_VALIDATED;
        
        this.errorCounts = new HashMap<>();
        initializeErrorCounts();
        
        this.currentState = ValidationState.IDLE;
        this.submittedThisSession = false;
        this.lastValidationLogId = -1;
        this.googleDriveFileUrl = null;
        this.isolatedLayer = null;
    }
    
    /**
     * Initialize all error counts to zero.
     */
    private void initializeErrorCounts() {
        for (String errorType : ValidationConstants.ERROR_TYPES) {
            errorCounts.put(errorType, 0);
        }
    }
    
    // ========== Form Data Getters/Setters ==========
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId != null ? taskId.trim() : "";
    }
    
    public String getSettlement() {
        return settlement;
    }
    
    public void setSettlement(String settlement) {
        this.settlement = settlement != null ? settlement.trim() : "";
    }
    
    public String getMapperUsername() {
        return mapperUsername;
    }
    
    public void setMapperUsername(String mapperUsername) {
        this.mapperUsername = mapperUsername != null ? mapperUsername.trim() : "";
    }
    
    public String getFilterDate() {
        return filterDate;
    }
    
    public void setFilterDate(String filterDate) {
        this.filterDate = filterDate != null ? filterDate.trim() : "";
    }
    
    public int getTotalBuildings() {
        return totalBuildings;
    }
    
    public void setTotalBuildings(int totalBuildings) {
        this.totalBuildings = Math.max(0, totalBuildings);
    }
    
    public String getValidatorComments() {
        return validatorComments;
    }
    
    public void setValidatorComments(String validatorComments) {
        this.validatorComments = validatorComments != null ? validatorComments.trim() : "";
    }
    
    public String getValidationStatus() {
        return validationStatus;
    }
    
    public void setValidationStatus(String validationStatus) {
        if (ValidationConstants.STATUS_VALIDATED.equals(validationStatus) ||
            ValidationConstants.STATUS_REJECTED.equals(validationStatus)) {
            this.validationStatus = validationStatus;
        } else {
            throw new IllegalArgumentException("Invalid validation status: " + validationStatus);
        }
    }
    
    // ========== Error Tracking Methods ==========
    
    /**
     * Get error count for a specific error type.
     * 
     * @param errorType the error type (must be in ValidationConstants.ERROR_TYPES)
     * @return error count, or 0 if not found
     */
    public int getErrorCount(String errorType) {
        return errorCounts.getOrDefault(errorType, 0);
    }
    
    /**
     * Set error count for a specific error type.
     * 
     * @param errorType the error type
     * @param count the count (negative values treated as 0)
     */
    public void setErrorCount(String errorType, int count) {
        errorCounts.put(errorType, Math.max(0, count));
    }
    
    /**
     * Get all error counts as unmodifiable map.
     * 
     * @return map of error type to count
     */
    public Map<String, Integer> getAllErrorCounts() {
        return Collections.unmodifiableMap(errorCounts);
    }
    
    /**
     * Calculate total number of errors across all types.
     * 
     * @return sum of all error counts
     */
    public int getTotalErrors() {
        return errorCounts.values().stream()
            .mapToInt(Integer::intValue)
            .sum();
    }
    
    /**
     * Check if there are any errors recorded.
     * 
     * @return true if total errors > 0
     */
    public boolean hasErrors() {
        return getTotalErrors() > 0;
    }
    
    /**
     * Reset all error counts to zero.
     */
    public void clearAllErrors() {
        errorCounts.replaceAll((k, v) -> 0);
    }
    
    // ========== Workflow State Methods ==========
    
    public ValidationState getCurrentState() {
        return currentState;
    }
    
    public void setCurrentState(ValidationState state) {
        this.currentState = state != null ? state : ValidationState.IDLE;
    }
    
    public boolean isSubmittedThisSession() {
        return submittedThisSession;
    }
    
    public void setSubmittedThisSession(boolean submitted) {
        this.submittedThisSession = submitted;
    }
    
    public int getLastValidationLogId() {
        return lastValidationLogId;
    }
    
    public void setLastValidationLogId(int logId) {
        this.lastValidationLogId = logId;
    }
    
    public String getGoogleDriveFileUrl() {
        return googleDriveFileUrl;
    }
    
    public void setGoogleDriveFileUrl(String url) {
        this.googleDriveFileUrl = url;
    }
    
    public Object getIsolatedLayer() {
        return isolatedLayer;
    }
    
    public void setIsolatedLayer(Object layer) {
        this.isolatedLayer = layer;
    }
    
    // ========== Validation Methods ==========
    
    /**
     * Validate that all required fields are filled.
     * 
     * @return validation result with errors
     */
    public ValidationResult validate() {
        List<String> errors = new ArrayList<>();
        
        // Task ID validation (optional but recommended)
        if (taskId.isEmpty()) {
            errors.add("Task ID is recommended but not required");
        }
        
        // Mapper username is required
        if (mapperUsername.isEmpty()) {
            errors.add("Mapper username is required");
        }
        
        // Filter date is required
        if (filterDate.isEmpty()) {
            errors.add("Filter date is required");
        }
        
        // Total buildings must be > 0
        if (totalBuildings <= 0) {
            errors.add("Total buildings must be greater than 0");
        }
        
        // Comments recommended if there are errors
        if (hasErrors() && validatorComments.isEmpty()) {
            errors.add("Validator comments recommended when errors are present");
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Check if validation can be submitted.
     * 
     * @return true if all required fields are valid
     */
    public boolean canSubmit() {
        return !mapperUsername.isEmpty() && 
               !filterDate.isEmpty() && 
               totalBuildings > 0;
    }
    
    /**
     * Check if data has been isolated.
     * 
     * @return true if in ISOLATED or later state
     */
    public boolean isIsolated() {
        return currentState == ValidationState.ISOLATED ||
               currentState == ValidationState.SUBMITTED ||
               currentState == ValidationState.EXPORTED;
    }
    
    /**
     * Reset the model to initial state.
     * Clears all form data and error counts.
     */
    public void reset() {
        this.taskId = "";
        this.settlement = "";
        this.mapperUsername = "";
        this.filterDate = "";
        this.totalBuildings = 0;
        this.validatorComments = "";
        this.validationStatus = ValidationConstants.STATUS_VALIDATED;
        
        clearAllErrors();
        
        this.currentState = ValidationState.IDLE;
        this.submittedThisSession = false;
        this.lastValidationLogId = -1;
        this.googleDriveFileUrl = null;
        this.isolatedLayer = null;
    }
    
    /**
     * Create a JSON representation for API submission.
     * 
     * @param validatorUsername the OSM username of the validator
     * @return JSON string
     */
    public String toJSON(String validatorUsername) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"task_id\": \"").append(escapeJSON(taskId)).append("\",\n");
        json.append("  \"mapper_osm_username\": \"").append(escapeJSON(mapperUsername)).append("\",\n");
        json.append("  \"validator_osm_username\": \"").append(escapeJSON(validatorUsername)).append("\",\n");
        json.append("  \"validation_date\": \"").append(escapeJSON(filterDate)).append("\",\n");
        json.append("  \"settlement\": \"").append(escapeJSON(settlement)).append("\",\n");
        json.append("  \"total_buildings\": ").append(totalBuildings).append(",\n");
        json.append("  \"validation_status\": \"").append(escapeJSON(validationStatus)).append("\",\n");
        json.append("  \"validator_comments\": \"").append(escapeJSON(validatorComments)).append("\",\n");
        
        // Add error counts
        int idx = 0;
        for (String errorType : ValidationConstants.ERROR_TYPES) {
            int count = getErrorCount(errorType);
            json.append("  \"").append(errorType.toLowerCase().replace(" ", "_")).append("\": ").append(count);
            if (idx < ValidationConstants.ERROR_TYPES.length - 1) {
                json.append(",");
            }
            json.append("\n");
            idx++;
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Escape special characters for JSON.
     */
    private String escapeJSON(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    // ========== Inner Classes ==========
    
    /**
     * Validation result wrapper.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = Collections.unmodifiableList(errors);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            if (valid) return "";
            return String.join("\n", errors);
        }
    }
}
