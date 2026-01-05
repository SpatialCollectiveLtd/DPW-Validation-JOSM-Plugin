package org.openstreetmap.josm.plugins.dpwvalidationtool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InputValidator class.
 * Tests validation and sanitization logic for all input types.
 * 
 * @author Spatial Collective Ltd
 * @version 3.0.6
 */
@DisplayName("InputValidator Tests")
class InputValidatorTest {
    
    // ========== Task ID Tests ==========
    
    @Test
    @DisplayName("Valid task ID should pass validation")
    void testValidTaskId() throws Exception {
        assertEquals("ABC-123", InputValidator.validateTaskId("ABC-123"));
        assertEquals("task123", InputValidator.validateTaskId("task123"));
        assertEquals("Project-2024-001", InputValidator.validateTaskId("Project-2024-001"));
    }
    
    @Test
    @DisplayName("Task ID should trim whitespace")
    void testTaskIdTrimsWhitespace() throws Exception {
        assertEquals("ABC-123", InputValidator.validateTaskId("  ABC-123  "));
    }
    
    @Test
    @DisplayName("Empty task ID should throw exception")
    void testEmptyTaskIdThrowsException() {
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateTaskId("");
        });
        
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateTaskId("   ");
        });
        
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateTaskId(null);
        });
    }
    
    @Test
    @DisplayName("Task ID with special characters should throw exception")
    void testTaskIdWithSpecialCharacters() {
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateTaskId("task@123");
        });
        
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateTaskId("task#123");
        });
    }
    
    @Test
    @DisplayName("Task ID exceeding max length should throw exception")
    void testTaskIdMaxLength() {
        String longTaskId = "A".repeat(ValidationConstants.TASK_ID_MAX_LENGTH + 1);
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateTaskId(longTaskId);
        });
    }
    
    // ========== Username Tests ==========
    
    @Test
    @DisplayName("Valid username should pass validation")
    void testValidUsername() throws Exception {
        assertEquals("john_mapper", InputValidator.validateUsername("john_mapper"));
        assertEquals("Jane Doe", InputValidator.validateUsername("Jane Doe"));
        assertEquals("user-123", InputValidator.validateUsername("user-123"));
    }
    
    @Test
    @DisplayName("Empty username should throw exception")
    void testEmptyUsernameThrowsException() {
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateUsername("");
        });
        
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateUsername(null);
        });
    }
    
    @Test
    @DisplayName("Username exceeding max length should throw exception")
    void testUsernameMaxLength() {
        String longUsername = "A".repeat(ValidationConstants.USERNAME_MAX_LENGTH + 1);
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateUsername(longUsername);
        });
    }
    
    // ========== Settlement Tests ==========
    
    @Test
    @DisplayName("Valid settlement should pass validation")
    void testValidSettlement() throws Exception {
        assertEquals("Nairobi", InputValidator.validateSettlement("Nairobi"));
        assertEquals("Cape Town", InputValidator.validateSettlement("Cape Town"));
    }
    
    @Test
    @DisplayName("Empty settlement should return empty string")
    void testEmptySettlement() throws Exception {
        assertEquals("", InputValidator.validateSettlement(""));
        assertEquals("", InputValidator.validateSettlement(null));
        assertEquals("", InputValidator.validateSettlement("   "));
    }
    
    @Test
    @DisplayName("Settlement exceeding max length should throw exception")
    void testSettlementMaxLength() {
        String longSettlement = "A".repeat(ValidationConstants.SETTLEMENT_MAX_LENGTH + 1);
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateSettlement(longSettlement);
        });
    }
    
    // ========== Comments Tests ==========
    
    @Test
    @DisplayName("Valid comments should pass validation")
    void testValidComments() throws Exception {
        assertEquals("Good work!", InputValidator.validateComments("Good work!"));
        assertEquals("Multiple errors found.", InputValidator.validateComments("Multiple errors found."));
    }
    
    @Test
    @DisplayName("Empty comments should return empty string")
    void testEmptyComments() throws Exception {
        assertEquals("", InputValidator.validateComments(""));
        assertEquals("", InputValidator.validateComments(null));
    }
    
    @Test
    @DisplayName("Comments should remove control characters")
    void testCommentsRemovesControlCharacters() throws Exception {
        String withControlChars = "Hello\u0000World\u0001Test";
        String cleaned = InputValidator.validateComments(withControlChars);
        assertFalse(cleaned.contains("\u0000"));
        assertFalse(cleaned.contains("\u0001"));
    }
    
    @Test
    @DisplayName("Comments exceeding max length should throw exception")
    void testCommentsMaxLength() {
        String longComments = "A".repeat(ValidationConstants.COMMENTS_MAX_LENGTH + 1);
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateComments(longComments);
        });
    }
    
    // ========== Total Buildings Tests ==========
    
    @Test
    @DisplayName("Valid total buildings should pass validation")
    void testValidTotalBuildings() throws Exception {
        assertEquals(100, InputValidator.validateTotalBuildings("100"));
        assertEquals(1, InputValidator.validateTotalBuildings("1"));
        assertEquals(9999, InputValidator.validateTotalBuildings("9999"));
    }
    
    @Test
    @DisplayName("Zero or negative total buildings should throw exception")
    void testInvalidTotalBuildings() {
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateTotalBuildings("0");
        });
        
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateTotalBuildings("-5");
        });
    }
    
    @Test
    @DisplayName("Non-numeric total buildings should throw exception")
    void testNonNumericTotalBuildings() {
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateTotalBuildings("abc");
        });
        
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateTotalBuildings("12.5");
        });
    }
    
    // ========== Error Count Tests ==========
    
    @Test
    @DisplayName("Valid error count should pass validation")
    void testValidErrorCount() throws Exception {
        assertEquals(0, InputValidator.validateErrorCount("0", "Test"));
        assertEquals(5, InputValidator.validateErrorCount("5", "Test"));
        assertEquals(100, InputValidator.validateErrorCount("100", "Test"));
    }
    
    @Test
    @DisplayName("Empty error count should default to 0")
    void testEmptyErrorCount() throws Exception {
        assertEquals(0, InputValidator.validateErrorCount("", "Test"));
        assertEquals(0, InputValidator.validateErrorCount(null, "Test"));
    }
    
    @Test
    @DisplayName("Negative error count should throw exception")
    void testNegativeErrorCount() {
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateErrorCount("-5", "Test");
        });
    }
    
    // ========== Date Tests ==========
    
    @Test
    @DisplayName("Valid date should pass validation")
    void testValidDate() throws Exception {
        assertEquals("2024-01-15", InputValidator.validateDate("2024-01-15"));
        assertEquals("2023-12-31", InputValidator.validateDate("2023-12-31"));
    }
    
    @Test
    @DisplayName("Invalid date format should throw exception")
    void testInvalidDateFormat() {
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateDate("15-01-2024");
        });
        
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateDate("2024/01/15");
        });
        
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateDate("abc");
        });
    }
    
    @Test
    @DisplayName("Invalid date ranges should throw exception")
    void testInvalidDateRanges() {
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateDate("2024-13-01"); // Invalid month
        });
        
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateDate("2024-01-32"); // Invalid day
        });
        
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateDate("1999-01-01"); // Too old
        });
    }
    
    // ========== URL Tests ==========
    
    @Test
    @DisplayName("Valid URL should pass validation")
    void testValidUrl() throws Exception {
        assertEquals("https://example.com", InputValidator.validateUrl("https://example.com"));
        assertEquals("http://example.com", InputValidator.validateUrl("http://example.com"));
    }
    
    @Test
    @DisplayName("Empty URL should return empty string")
    void testEmptyUrl() throws Exception {
        assertEquals("", InputValidator.validateUrl(""));
        assertEquals("", InputValidator.validateUrl(null));
    }
    
    @Test
    @DisplayName("Invalid URL should throw exception")
    void testInvalidUrl() {
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateUrl("not-a-url");
        });
        
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateUrl("ftp://example.com"); // Only HTTP/HTTPS allowed
        });
    }
    
    // ========== Status Tests ==========
    
    @Test
    @DisplayName("Valid status should pass validation")
    void testValidStatus() throws Exception {
        assertEquals(ValidationConstants.STATUS_VALIDATED, 
            InputValidator.validateStatus(ValidationConstants.STATUS_VALIDATED));
        assertEquals(ValidationConstants.STATUS_REJECTED,
            InputValidator.validateStatus(ValidationConstants.STATUS_REJECTED));
    }
    
    @Test
    @DisplayName("Invalid status should throw exception")
    void testInvalidStatus() {
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateStatus("Pending");
        });
        
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateStatus("Unknown");
        });
        
        assertThrows(InputValidator.ValidationException.class, () -> {
            InputValidator.validateStatus("");
        });
    }
}
