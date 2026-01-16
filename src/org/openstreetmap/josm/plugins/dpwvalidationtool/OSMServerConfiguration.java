package org.openstreetmap.josm.plugins.dpwvalidationtool;

import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

/**
 * Configuration for custom OSM server instances.
 * Supports private OSM servers with custom OAuth endpoints.
 * 
 * This allows the plugin to work with:
 * - Custom OSM instances (e.g., osm.spatialcollective.co.ke)
 * - Alternative authentication servers
 * - Private infrastructure
 * 
 * @version 3.4.0
 * @since 3.4.0
 */
public class OSMServerConfiguration {
    
    // Preference keys
    private static final String PREFIX = "dpw-validation-tool.osm-server.";
    private static final String OSM_SERVER_URL = PREFIX + "url";
    private static final String API_ENDPOINT_URL = PREFIX + "api-endpoint";
    private static final String OAUTH_AUTHORIZATION_URL = PREFIX + "oauth-auth-url";
    private static final String OAUTH_TOKEN_URL = PREFIX + "oauth-token-url";
    private static final String USE_CUSTOM_SERVER = PREFIX + "use-custom-server";
    
    // Default values (OpenStreetMap.org)
    private static final String DEFAULT_OSM_SERVER = "https://www.openstreetmap.org";
    private static final String DEFAULT_API_ENDPOINT = "https://api.openstreetmap.org/api";
    private static final String DEFAULT_OAUTH_AUTH = "https://www.openstreetmap.org/oauth2/authorize";
    private static final String DEFAULT_OAUTH_TOKEN = "https://www.openstreetmap.org/oauth2/token";
    
    private final String osmServerUrl;
    private final String apiEndpointUrl;
    private final String authorizationUrl;
    private final String tokenAcquisitionUrl;
    private final boolean useCustomServer;
    
    /**
     * Create a new OSM server configuration
     */
    public OSMServerConfiguration(String osmServerUrl, String apiEndpointUrl, 
                                  String authorizationUrl, String tokenAcquisitionUrl,
                                  boolean useCustomServer) {
        this.osmServerUrl = osmServerUrl;
        this.apiEndpointUrl = apiEndpointUrl;
        this.authorizationUrl = authorizationUrl;
        this.tokenAcquisitionUrl = tokenAcquisitionUrl;
        this.useCustomServer = useCustomServer;
    }
    
    /**
     * Load configuration from JOSM preferences
     */
    public static OSMServerConfiguration loadFromPreferences() {
        boolean useCustom = Config.getPref().getBoolean(USE_CUSTOM_SERVER, false);
        
        if (useCustom) {
            String osmUrl = Config.getPref().get(OSM_SERVER_URL, DEFAULT_OSM_SERVER);
            String apiUrl = Config.getPref().get(API_ENDPOINT_URL, osmUrl + "/api");
            String authUrl = Config.getPref().get(OAUTH_AUTHORIZATION_URL, osmUrl + "/oauth2/authorize");
            String tokenUrl = Config.getPref().get(OAUTH_TOKEN_URL, osmUrl + "/oauth2/token");
            
            Logging.info("DPW: Using custom OSM server: " + osmUrl);
            return new OSMServerConfiguration(osmUrl, apiUrl, authUrl, tokenUrl, true);
        } else {
            Logging.info("DPW: Using default OpenStreetMap.org server");
            return getDefaultConfiguration();
        }
    }
    
    /**
     * Save configuration to JOSM preferences
     */
    public void saveToPreferences() {
        Config.getPref().putBoolean(USE_CUSTOM_SERVER, useCustomServer);
        Config.getPref().put(OSM_SERVER_URL, osmServerUrl);
        Config.getPref().put(API_ENDPOINT_URL, apiEndpointUrl);
        Config.getPref().put(OAUTH_AUTHORIZATION_URL, authorizationUrl);
        Config.getPref().put(OAUTH_TOKEN_URL, tokenAcquisitionUrl);
        
        Logging.info("DPW: Saved OSM server configuration");
    }
    
    /**
     * Get default configuration (OpenStreetMap.org)
     */
    public static OSMServerConfiguration getDefaultConfiguration() {
        return new OSMServerConfiguration(
            DEFAULT_OSM_SERVER,
            DEFAULT_API_ENDPOINT,
            DEFAULT_OAUTH_AUTH,
            DEFAULT_OAUTH_TOKEN,
            false
        );
    }
    
    /**
     * Create configuration for Spatial Collective private server
     */
    public static OSMServerConfiguration getSpatialCollectiveConfiguration() {
        String baseUrl = "https://osm.spatialcollective.co.ke";
        return new OSMServerConfiguration(
            baseUrl,
            baseUrl + "/api",
            baseUrl + "/oauth2/authorize",
            baseUrl + "/oauth2/token",
            true
        );
    }
    
    // Getters
    
    public String getOsmServerUrl() {
        return osmServerUrl;
    }
    
    public String getApiEndpointUrl() {
        return apiEndpointUrl;
    }
    
    public String getAuthorizationUrl() {
        return authorizationUrl;
    }
    
    public String getTokenAcquisitionUrl() {
        return tokenAcquisitionUrl;
    }
    
    public boolean isUsingCustomServer() {
        return useCustomServer;
    }
    
    /**
     * Check if custom server is enabled
     */
    public static boolean isCustomServerEnabled() {
        return Config.getPref().getBoolean(USE_CUSTOM_SERVER, false);
    }
    
    /**
     * Enable or disable custom server
     */
    public static void setCustomServerEnabled(boolean enabled) {
        Config.getPref().putBoolean(USE_CUSTOM_SERVER, enabled);
    }
    
    /**
     * Get configured OSM server URL
     */
    public static String getConfiguredOSMServerUrl() {
        if (isCustomServerEnabled()) {
            return Config.getPref().get(OSM_SERVER_URL, DEFAULT_OSM_SERVER);
        }
        return DEFAULT_OSM_SERVER;
    }
    
    /**
     * Set OSM server URL
     */
    public static void setOSMServerUrl(String url) {
        Config.getPref().put(OSM_SERVER_URL, url);
    }
    
    /**
     * Get configured API endpoint URL
     */
    public static String getConfiguredApiEndpointUrl() {
        if (isCustomServerEnabled()) {
            return Config.getPref().get(API_ENDPOINT_URL, DEFAULT_API_ENDPOINT);
        }
        return DEFAULT_API_ENDPOINT;
    }
    
    /**
     * Set API endpoint URL
     */
    public static void setApiEndpointUrl(String url) {
        Config.getPref().put(API_ENDPOINT_URL, url);
    }
    
    /**
     * Get configured OAuth authorization URL
     */
    public static String getConfiguredAuthorizationUrl() {
        if (isCustomServerEnabled()) {
            return Config.getPref().get(OAUTH_AUTHORIZATION_URL, DEFAULT_OAUTH_AUTH);
        }
        return DEFAULT_OAUTH_AUTH;
    }
    
    /**
     * Set OAuth authorization URL
     */
    public static void setAuthorizationUrl(String url) {
        Config.getPref().put(OAUTH_AUTHORIZATION_URL, url);
    }
    
    /**
     * Get configured OAuth token URL
     */
    public static String getConfiguredTokenUrl() {
        if (isCustomServerEnabled()) {
            return Config.getPref().get(OAUTH_TOKEN_URL, DEFAULT_OAUTH_TOKEN);
        }
        return DEFAULT_OAUTH_TOKEN;
    }
    
    /**
     * Set OAuth token URL
     */
    public static void setTokenUrl(String url) {
        Config.getPref().put(OAUTH_TOKEN_URL, url);
    }
    
    /**
     * Apply Spatial Collective server configuration
     */
    public static void applySpatialCollectiveConfig() {
        OSMServerConfiguration config = getSpatialCollectiveConfiguration();
        config.saveToPreferences();
        Logging.info("DPW: Applied Spatial Collective OSM server configuration");
    }
    
    @Override
    public String toString() {
        return "OSMServerConfiguration{" +
                "osmServerUrl='" + osmServerUrl + '\'' +
                ", apiEndpointUrl='" + apiEndpointUrl + '\'' +
                ", authorizationUrl='" + authorizationUrl + '\'' +
                ", tokenAcquisitionUrl='" + tokenAcquisitionUrl + '\'' +
                ", useCustomServer=" + useCustomServer +
                '}';
    }
}
