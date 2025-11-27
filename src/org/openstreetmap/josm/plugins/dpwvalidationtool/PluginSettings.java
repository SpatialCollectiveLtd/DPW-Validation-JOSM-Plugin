package org.openstreetmap.josm.plugins.dpwvalidationtool;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Manages plugin settings and preferences
 * Stores configuration in JOSM's preference system
 * 
 * @version 3.1.0-BETA
 */
public class PluginSettings {
    
    // Preference keys
    private static final String PREFIX = "dpw-validation-tool.";
    private static final String TM_INTEGRATION_ENABLED = PREFIX + "tm-integration-enabled";
    private static final String DPW_API_BASE_URL = PREFIX + "dpw-api-base-url";
    private static final String TM_API_BASE_URL = PREFIX + "tm-api-base-url";
    private static final String DEFAULT_SETTLEMENT = PREFIX + "default-settlement";
    private static final String AUTO_FETCH_SETTLEMENT = PREFIX + "auto-fetch-settlement";
    private static final String PREF_AUTO_FETCH_SETTLEMENT = PREFIX + "auto-fetch-settlement"; // Alias
    private static final String REMOTE_CONTROL_DETECTION = PREFIX + "remote-control-detection";
    private static final String CACHE_TM_DATA = PREFIX + "cache-tm-data";
    private static final String PREF_CACHE_EXPIRY_HOURS = PREFIX + "cache-expiry-hours";
    private static final String DEFAULT_PROJECT_URL = PREFIX + "default-project-url";
    private static final String DEFAULT_PROJECT_ID = PREFIX + "default-project-id";
    
    // Default values
    private static final String DEFAULT_DPW_API_URL = "https://app.spatialcollective.com/api";
    private static final String DEFAULT_TM_API_URL = "https://tasking-manager-tm4-production-api.hotosm.org/api/v2";
    
    /**
     * Check if Task Manager integration is enabled
     * Default: false (BETA feature, opt-in)
     */
    public static boolean isTMIntegrationEnabled() {
        return Config.getPref().getBoolean(TM_INTEGRATION_ENABLED, false);
    }
    
    /**
     * Enable or disable Task Manager integration
     */
    public static void setTMIntegrationEnabled(boolean enabled) {
        Config.getPref().putBoolean(TM_INTEGRATION_ENABLED, enabled);
    }
    
    /**
     * Get DPW API base URL
     * Default: https://app.spatialcollective.com/api
     */
    public static String getDPWApiBaseUrl() {
        return Config.getPref().get(DPW_API_BASE_URL, DEFAULT_DPW_API_URL);
    }
    
    /**
     * Set DPW API base URL
     */
    public static void setDPWApiBaseUrl(String url) {
        Config.getPref().put(DPW_API_BASE_URL, url);
    }
    
    /**
     * Get Task Manager API base URL
     * Default: https://tasking-manager-tm4-production-api.hotosm.org/api/v2
     */
    public static String getTMApiBaseUrl() {
        return Config.getPref().get(TM_API_BASE_URL, DEFAULT_TM_API_URL);
    }
    
    /**
     * Set Task Manager API base URL
     */
    public static void setTMApiBaseUrl(String url) {
        Config.getPref().put(TM_API_BASE_URL, url);
    }
    
    /**
     * Get default settlement
     */
    public static String getDefaultSettlement() {
        return Config.getPref().get(DEFAULT_SETTLEMENT, "");
    }
    
    /**
     * Set default settlement
     */
    public static void setDefaultSettlement(String settlement) {
        Config.getPref().put(DEFAULT_SETTLEMENT, settlement);
    }
    
    /**
     * Check if settlement should be auto-fetched when mapper is selected
     * Default: true
     */
    public static boolean isAutoFetchSettlementEnabled() {
        return Config.getPref().getBoolean(AUTO_FETCH_SETTLEMENT, true);
    }
    
    /**
     * Enable or disable auto-fetch settlement
     */
    public static void setAutoFetchSettlementEnabled(boolean enabled) {
        Config.getPref().putBoolean(AUTO_FETCH_SETTLEMENT, enabled);
    }
    
    /**
     * Check if remote control detection is enabled
     * Default: true (when TM integration is enabled)
     */
    public static boolean isRemoteControlDetectionEnabled() {
        return isTMIntegrationEnabled() && 
               Config.getPref().getBoolean(REMOTE_CONTROL_DETECTION, true);
    }
    
    /**
     * Enable or disable remote control detection
     */
    public static void setRemoteControlDetectionEnabled(boolean enabled) {
        Config.getPref().putBoolean(REMOTE_CONTROL_DETECTION, enabled);
    }
    
    /**
     * Check if TM data caching is enabled
     * Default: true
     */
    public static boolean isCacheTMDataEnabled() {
        return Config.getPref().getBoolean(CACHE_TM_DATA, true);
    }
    
    /**
     * Enable or disable TM data caching
     */
    public static void setCacheTMDataEnabled(boolean enabled) {
        Config.getPref().putBoolean(CACHE_TM_DATA, enabled);
    }
    
    public static boolean isAutoFetchSettlement() {
        return Config.getPref().getBoolean(PREF_AUTO_FETCH_SETTLEMENT, true);
    }
    
    public static void setAutoFetchSettlement(boolean enabled) {
        Config.getPref().putBoolean(PREF_AUTO_FETCH_SETTLEMENT, enabled);
    }
    
    public static int getCacheExpiryHours() {
        return Config.getPref().getInt(PREF_CACHE_EXPIRY_HOURS, 24);
    }
    
    public static void setCacheExpiryHours(int hours) {
        Config.getPref().putInt(PREF_CACHE_EXPIRY_HOURS, hours);
    }
    
    /**
     * Get default project URL for Task Manager
     * This is used to pre-fill the TM URL field for validators
     */
    public static String getDefaultProjectUrl() {
        return Config.getPref().get(DEFAULT_PROJECT_URL, "");
    }
    
    /**
     * Set default project URL for Task Manager
     */
    public static void setDefaultProjectUrl(String url) {
        Config.getPref().put(DEFAULT_PROJECT_URL, url);
    }
    
    /**
     * Get default project ID for Task Manager
     * This is used when only project ID is known (not full URL)
     */
    public static String getDefaultProjectId() {
        return Config.getPref().get(DEFAULT_PROJECT_ID, "");
    }
    
    /**
     * Set default project ID for Task Manager
     */
    public static void setDefaultProjectId(String projectId) {
        Config.getPref().put(DEFAULT_PROJECT_ID, projectId);
    }
    
    /**
     * Reset all settings to default values
     */
    public static void resetToDefaults() {
        setTMIntegrationEnabled(false);
        setDPWApiBaseUrl(DEFAULT_DPW_API_URL);
        setTMApiBaseUrl(DEFAULT_TM_API_URL);
        setDefaultSettlement("");
        setAutoFetchSettlementEnabled(true);
        setRemoteControlDetectionEnabled(true);
        setCacheTMDataEnabled(true);
        setDefaultProjectUrl("");
        setDefaultProjectId("");
    }
}
