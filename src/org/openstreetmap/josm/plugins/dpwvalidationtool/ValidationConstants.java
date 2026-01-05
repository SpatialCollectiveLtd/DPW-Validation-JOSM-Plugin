package org.openstreetmap.josm.plugins.dpwvalidationtool;

/**
 * Constants for the DPW Validation Tool plugin.
 * Centralizes magic numbers, strings, and configuration values.
 * 
 * @author Spatial Collective Ltd
 * @version 3.0.6
 * @since 3.0.6
 */
public final class ValidationConstants {
    
    // Prevent instantiation
    private ValidationConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    // ========================================================================
    // UI DIMENSIONS
    // ========================================================================
    
    /** Default panel width in pixels */
    public static final int PANEL_WIDTH = 640;
    
    /** Default panel height in pixels */
    public static final int PANEL_HEIGHT = 480;
    
    /** Maximum panel width in pixels */
    public static final int PANEL_MAX_WIDTH = 1024;
    
    /** Maximum panel height in pixels */
    public static final int PANEL_MAX_HEIGHT = 800;
    
    /** Mapper combo box width in pixels */
    public static final int MAPPER_COMBO_WIDTH = 220;
    
    /** Standard control height in pixels */
    public static final int CONTROL_HEIGHT = 24;
    
    /** Settlement field width in pixels */
    public static final int SETTLEMENT_FIELD_WIDTH = 130;
    
    /** Toggle preview button width in pixels */
    public static final int TOGGLE_PREVIEW_BUTTON_WIDTH = 200;
    
    /** Toggle preview button height in pixels */
    public static final int TOGGLE_PREVIEW_BUTTON_HEIGHT = 28;
    
    /** Preview text area rows */
    public static final int PREVIEW_TEXT_AREA_ROWS = 8;
    
    /** Preview text area columns */
    public static final int PREVIEW_TEXT_AREA_COLS = 40;
    
    // ========================================================================
    // API CONFIGURATION
    // ========================================================================
    
    /** Default DPW API base URL */
    public static final String DEFAULT_DPW_API_URL = "https://app.spatialcollective.com/api";
    
    /** Default Tasking Manager API base URL */
    public static final String DEFAULT_TM_API_URL = "https://tasking-manager-tm4-production-api.hotosm.org/api/v2";
    
    /** API connection timeout in milliseconds */
    public static final int API_TIMEOUT_MS = 10000; // 10 seconds
    public static final int CONNECTION_TIMEOUT_MS = API_TIMEOUT_MS; // Alias for compatibility
    
    /** API read timeout in milliseconds */
    public static final int API_READ_TIMEOUT_MS = 10000; // 10 seconds
    public static final int READ_TIMEOUT_MS = API_READ_TIMEOUT_MS; // Alias for compatibility
    
    // ========================================================================
    // CACHE & RATE LIMITING
    // ========================================================================
    
    /** User list cache duration in milliseconds (5 minutes) */
    public static final long CACHE_DURATION_MS = 300_000;
    
    /** Mapper fetch cooldown period in milliseconds (10 seconds) */
    public static final long MAPPER_FETCH_COOLDOWN_MS = 10_000;
    
    /** Default cache expiry in hours */
    public static final int DEFAULT_CACHE_EXPIRY_HOURS = 24;
    
    // ========================================================================
    // FIELD LIMITS (from API specification)
    // ========================================================================
    
    /** Maximum length for task ID field */
    public static final int TASK_ID_MAX_LENGTH = 100;
    
    /** Maximum length for settlement name */
    public static final int SETTLEMENT_MAX_LENGTH = 255;
    
    /** Maximum length for comments field */
    public static final int COMMENTS_MAX_LENGTH = 1000;
    
    /** Maximum length for username */
    public static final int USERNAME_MAX_LENGTH = 255;
    
    // ========================================================================
    // ERROR TYPES
    // ========================================================================
    
    /** Array of all validation error type names */
    public static final String[] ERROR_TYPES = {
        "Hanging Nodes",
        "Overlapping Buildings",
        "Buildings Crossing Highway",
        "Missing Tags",
        "Improper Tags",
        "Features Misidentified",
        "Missing Buildings",
        "Building Inside Building",
        "Building Crossing Residential",
        "Improperly Drawn"
    };
    
    /** Number of error types tracked */
    public static final int ERROR_TYPE_COUNT = ERROR_TYPES.length;
    
    // ========================================================================
    // VALIDATION STATUS
    // ========================================================================
    
    /** Validation status: Validated */
    public static final String STATUS_VALIDATED = "Validated";
    
    /** Validation status: Rejected */
    public static final String STATUS_REJECTED = "Rejected";
    
    // ========================================================================
    // UI TEXT & LABELS
    // ========================================================================
    
    /** Isolate button text */
    public static final String BTN_ISOLATE_MAPPER_WORK = "🔍 Isolate Mapper Work";
    
    /** Validate button text */
    public static final String BTN_VALIDATED = "✅ VALIDATED";
    
    /** Reject button text */
    public static final String BTN_REJECTED = "❌ REJECTED";
    
    /** Export button text */
    public static final String BTN_EXPORT_LAYER = "📁 Export Validated Layer";
    
    /** Reset button text */
    public static final String BTN_START_NEW_VALIDATION = "🔄 Start New Validation";
    
    /** Refresh mappers button text */
    public static final String BTN_REFRESH_MAPPERS = "🔄";
    
    /** Show preview button text (collapsed) */
    public static final String BTN_SHOW_PREVIEW = "📊 Show Validation Summary";
    
    /** Hide preview button text (expanded) */
    public static final String BTN_HIDE_PREVIEW = "⚫ Hide Validation Summary";
    
    // ========================================================================
    // SETTINGS KEYS
    // ========================================================================
    
    /** Settings key prefix */
    public static final String SETTINGS_PREFIX = "dpw-validation-tool.";
    
    /** TM integration enabled setting key */
    public static final String SETTING_TM_INTEGRATION_ENABLED = SETTINGS_PREFIX + "tm-integration-enabled";
    
    /** DPW API base URL setting key */
    public static final String SETTING_DPW_API_BASE_URL = SETTINGS_PREFIX + "dpw-api-base-url";
    
    /** TM API base URL setting key */
    public static final String SETTING_TM_API_BASE_URL = SETTINGS_PREFIX + "tm-api-base-url";
    
    /** Default settlement setting key */
    public static final String SETTING_DEFAULT_SETTLEMENT = SETTINGS_PREFIX + "default-settlement";
    
    /** Auto-fetch settlement setting key */
    public static final String SETTING_AUTO_FETCH_SETTLEMENT = SETTINGS_PREFIX + "auto-fetch-settlement";
    
    /** Remote control detection setting key */
    public static final String SETTING_REMOTE_CONTROL_DETECTION = SETTINGS_PREFIX + "remote-control-detection";
    
    /** Cache TM data setting key */
    public static final String SETTING_CACHE_TM_DATA = SETTINGS_PREFIX + "cache-tm-data";
    
    /** Cache expiry hours setting key */
    public static final String SETTING_CACHE_EXPIRY_HOURS = SETTINGS_PREFIX + "cache-expiry-hours";
    
    /** Default project URL setting key */
    public static final String SETTING_DEFAULT_PROJECT_URL = SETTINGS_PREFIX + "default-project-url";
    
    /** Default project ID setting key */
    public static final String SETTING_DEFAULT_PROJECT_ID = SETTINGS_PREFIX + "default-project-id";
    
    // ========================================================================
    // COLORS
    // ========================================================================
    
    /** Fetch status label background color (amber) */
    public static final java.awt.Color COLOR_FETCH_STATUS_BG = new java.awt.Color(255, 243, 205);
    
    /** Fetch status label foreground color (dark brown) */
    public static final java.awt.Color COLOR_FETCH_STATUS_FG = new java.awt.Color(102, 60, 0);
    
    /** Fetch status label border color (orange) */
    public static final java.awt.Color COLOR_FETCH_STATUS_BORDER = new java.awt.Color(255, 200, 100);
    
    /** Auth status label background color (light gray) */
    public static final java.awt.Color COLOR_AUTH_STATUS_BG = new java.awt.Color(224, 224, 224);
    
    /** Auth status label foreground color (dark gray) */
    public static final java.awt.Color COLOR_AUTH_STATUS_FG = new java.awt.Color(60, 60, 60);
    
    /** Auth status label border color (gray) */
    public static final java.awt.Color COLOR_AUTH_STATUS_BORDER = new java.awt.Color(180, 180, 180);
    
    /** Error status background color (light red) */
    public static final java.awt.Color COLOR_ERROR_BG = new java.awt.Color(255, 204, 204);
    
    /** Success status background color (light green) */
    public static final java.awt.Color COLOR_SUCCESS_BG = new java.awt.Color(204, 255, 204);
    
    /** Read-only field background color */
    public static final java.awt.Color COLOR_READ_ONLY_BG = new java.awt.Color(240, 240, 240);
    
    /** Preview text area background color */
    public static final java.awt.Color COLOR_PREVIEW_BG = new java.awt.Color(250, 250, 255);
    
    /** Preview text area border color */
    public static final java.awt.Color COLOR_PREVIEW_BORDER = new java.awt.Color(200, 200, 210);
    
    /** Warning text color (orange) */
    public static final java.awt.Color COLOR_WARNING_TEXT = new java.awt.Color(200, 100, 0);
    
    /** Info text color (blue) */
    public static final java.awt.Color COLOR_INFO_TEXT = new java.awt.Color(0, 100, 200);
    
    // ========================================================================
    // FILE & EXPORT
    // ========================================================================
    
    /** Default export file extension */
    public static final String EXPORT_FILE_EXTENSION = ".osm";
    
    /** Export file name pattern: validated_{mapper}_{task}.osm */
    public static final String EXPORT_FILE_NAME_PATTERN = "validated_%s_%s.osm";
    
    /** Isolated layer name pattern: Isolated: {username} */
    public static final String ISOLATED_LAYER_NAME_PATTERN = "Isolated: %s";
    
    // ========================================================================
    // REGEX PATTERNS
    // ========================================================================
    
    /** Date format: YYYY-MM-DD */
    public static final String REGEX_DATE_FORMAT = "\\d{4}-\\d{2}-\\d{2}";
    
    /** Task ID validation pattern (alphanumeric and hyphens) */
    public static final String REGEX_TASK_ID = "^[a-zA-Z0-9-]+$";
    
    /** Username validation pattern (OSM usernames) */
    public static final String REGEX_USERNAME = "^[a-zA-Z0-9_ -]+$";
    
    // ========================================================================
    // FONTS
    // ========================================================================
    
    /** Header font size */
    public static final float FONT_SIZE_HEADER = 14f;
    
    /** Standard font size */
    public static final float FONT_SIZE_STANDARD = 12f;
    
    /** Preview font size */
    public static final float FONT_SIZE_PREVIEW = 11f;
    
    /** Monospaced font name */
    public static final String FONT_MONOSPACED = java.awt.Font.MONOSPACED;
    
    // ========================================================================
    // INSETS & SPACING
    // ========================================================================
    
    /** Standard inset value */
    public static final int INSET_STANDARD = 5;
    
    /** Small inset value */
    public static final int INSET_SMALL = 2;
    
    /** Border padding */
    public static final int BORDER_PADDING = 4;
    
    /** Border inner padding */
    public static final int BORDER_INNER_PADDING = 8;
    
    // ========================================================================
    // MISC
    // ========================================================================
    
    /** Pirate icon base size (for scaling) */
    public static final int ICON_BASE_SIZE = 18;
    
    /** Large icon size */
    public static final int ICON_LARGE_SIZE = 24;
    
    /** Default icon size */
    public static final int ICON_DEFAULT_SIZE = 18;
}
