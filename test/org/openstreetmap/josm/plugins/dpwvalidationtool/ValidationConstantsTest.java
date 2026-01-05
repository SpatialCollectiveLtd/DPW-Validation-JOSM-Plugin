package org.openstreetmap.josm.plugins.dpwvalidationtool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationConstants class.
 * Ensures all constants are properly defined and have reasonable values.
 * 
 * @author Spatial Collective Ltd
 * @version 3.0.6
 */
@DisplayName("ValidationConstants Tests")
class ValidationConstantsTest {
    
    @Test
    @DisplayName("UI dimensions should be positive")
    void testUIDimensionsArePositive() {
        assertTrue(ValidationConstants.PANEL_WIDTH > 0, "Panel width must be positive");
        assertTrue(ValidationConstants.PANEL_HEIGHT > 0, "Panel height must be positive");
        assertTrue(ValidationConstants.BUTTON_WIDTH > 0, "Button width must be positive");
        assertTrue(ValidationConstants.BUTTON_HEIGHT > 0, "Button height must be positive");
        assertTrue(ValidationConstants.TEXT_FIELD_WIDTH > 0, "Text field width must be positive");
    }
    
    @Test
    @DisplayName("Timeouts should be reasonable")
    void testTimeoutsAreReasonable() {
        assertTrue(ValidationConstants.CONNECTION_TIMEOUT_MS >= 5000, 
            "Connection timeout should be at least 5 seconds");
        assertTrue(ValidationConstants.CONNECTION_TIMEOUT_MS <= 60000,
            "Connection timeout should not exceed 60 seconds");
        
        assertTrue(ValidationConstants.READ_TIMEOUT_MS >= 5000,
            "Read timeout should be at least 5 seconds");
        assertTrue(ValidationConstants.READ_TIMEOUT_MS <= 60000,
            "Read timeout should not exceed 60 seconds");
    }
    
    @Test
    @DisplayName("Cache duration should be at least 1 minute")
    void testCacheDuration() {
        assertTrue(ValidationConstants.CACHE_DURATION_MS >= 60000,
            "Cache duration should be at least 1 minute");
    }
    
    @Test
    @DisplayName("Field limits should be reasonable")
    void testFieldLimits() {
        assertTrue(ValidationConstants.TASK_ID_MAX_LENGTH > 0, "Task ID max length must be positive");
        assertTrue(ValidationConstants.USERNAME_MAX_LENGTH > 0, "Username max length must be positive");
        assertTrue(ValidationConstants.SETTLEMENT_MAX_LENGTH > 0, "Settlement max length must be positive");
        assertTrue(ValidationConstants.COMMENTS_MAX_LENGTH > 0, "Comments max length must be positive");
        
        // Reasonable limits
        assertTrue(ValidationConstants.TASK_ID_MAX_LENGTH <= 255, "Task ID should not exceed 255 chars");
        assertTrue(ValidationConstants.COMMENTS_MAX_LENGTH >= 500, "Comments should allow at least 500 chars");
    }
    
    @Test
    @DisplayName("Error types array should not be empty")
    void testErrorTypesNotEmpty() {
        assertNotNull(ValidationConstants.ERROR_TYPES, "Error types array should not be null");
        assertTrue(ValidationConstants.ERROR_TYPES.length > 0, "Error types should contain at least one type");
    }
    
    @Test
    @DisplayName("All error types should be non-empty strings")
    void testErrorTypesAreValid() {
        for (String errorType : ValidationConstants.ERROR_TYPES) {
            assertNotNull(errorType, "Error type should not be null");
            assertFalse(errorType.trim().isEmpty(), "Error type should not be empty");
        }
    }
    
    @Test
    @DisplayName("Regex patterns should be valid")
    void testRegexPatternsAreValid() {
        assertDoesNotThrow(() -> {
            java.util.regex.Pattern.compile(ValidationConstants.REGEX_TASK_ID);
            java.util.regex.Pattern.compile(ValidationConstants.REGEX_USERNAME);
            java.util.regex.Pattern.compile(ValidationConstants.REGEX_DATE_FORMAT);
        }, "Regex patterns should compile without errors");
    }
    
    @Test
    @DisplayName("Status constants should be defined")
    void testStatusConstantsDefined() {
        assertNotNull(ValidationConstants.STATUS_VALIDATED, "Validated status should be defined");
        assertNotNull(ValidationConstants.STATUS_REJECTED, "Rejected status should be defined");
        assertFalse(ValidationConstants.STATUS_VALIDATED.isEmpty(), "Validated status should not be empty");
        assertFalse(ValidationConstants.STATUS_REJECTED.isEmpty(), "Rejected status should not be empty");
    }
    
    @Test
    @DisplayName("Default API URL should be valid")
    void testDefaultAPIUrl() {
        assertNotNull(ValidationConstants.DEFAULT_DPW_API_URL, "Default API URL should be defined");
        assertTrue(ValidationConstants.DEFAULT_DPW_API_URL.startsWith("http"),
            "Default API URL should start with http");
    }
}
