package org.openstreetmap.josm.plugins.dpwvalidationtool;

import org.openstreetmap.josm.tools.Logging;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom OAuth 2.0 client for authenticating with private OSM servers.
 * Implements OAuth 2.0 Authorization Code flow with PKCE (RFC 7636).
 * 
 * @version 3.4.0
 * @since 3.4.0
 */
public class CustomOAuthClient {
    
    private static CustomOAuthClient instance;
    private final OSMServerConfiguration config;
    
    // OAuth client credentials (public client - no secret)
    private static final String CLIENT_ID = "dpw_josm_plugin";
    private static final String SCOPE = "read_prefs write_api";
    
    // PKCE parameters (stored during auth flow)
    private String codeVerifier;
    private String state;
    
    /**
     * Private constructor - use getInstance()
     */
    private CustomOAuthClient(OSMServerConfiguration config) {
        this.config = config;
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized CustomOAuthClient getInstance() {
        if (instance == null) {
            OSMServerConfiguration config = OSMServerConfiguration.loadFromPreferences();
            instance = new CustomOAuthClient(config);
        }
        return instance;
    }
    
    /**
     * Reload configuration (call after settings change)
     */
    public static synchronized void reloadConfiguration() {
        OSMServerConfiguration config = OSMServerConfiguration.loadFromPreferences();
        instance = new CustomOAuthClient(config);
        Logging.info("DPW OAuth: Configuration reloaded");
    }
    
    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        if (!TokenManager.hasValidTokens()) {
            return false;
        }
        
        // Check if token is expired
        if (TokenManager.isAccessTokenExpired()) {
            // Try to refresh
            try {
                refreshAccessToken();
                return TokenManager.hasValidTokens();
            } catch (Exception e) {
                Logging.warn("DPW OAuth: Token refresh failed: " + e.getMessage());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get authenticated username
     */
    public String getUsername() {
        String cached = TokenManager.loadUsername();
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        
        // Fetch from server
        try {
            String username = fetchUserDetails();
            if (username != null) {
                TokenManager.saveUsername(username);
            }
            return username;
        } catch (Exception e) {
            Logging.error("DPW OAuth: Failed to fetch username: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Start OAuth authentication flow
     * Opens browser and waits for callback
     */
    public CompletableFuture<Boolean> authenticate() {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        
        new Thread(() -> {
            try {
                // Generate PKCE parameters
                codeVerifier = generateCodeVerifier();
                String codeChallenge = generateCodeChallenge(codeVerifier);
                state = generateState();
                
                // Start callback server
                OAuthCallbackServer callbackServer = new OAuthCallbackServer();
                CompletableFuture<OAuthCallbackServer.OAuthCallback> callbackFuture = 
                    callbackServer.startAndWaitForCallback(state);
                
                String redirectUri = callbackServer.getCallbackUrl();
                
                // Build authorization URL
                String authUrl = buildAuthorizationUrl(redirectUri, codeChallenge, state);
                
                Logging.info("DPW OAuth: Opening browser for authentication: " + authUrl);
                
                // Open browser
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(authUrl));
                } else {
                    Logging.error("DPW OAuth: Desktop not supported, cannot open browser");
                    result.completeExceptionally(new UnsupportedOperationException("Cannot open browser"));
                    callbackServer.stop();
                    return;
                }
                
                // Wait for callback
                OAuthCallbackServer.OAuthCallback callback = callbackFuture.get();
                
                if (callback.isSuccess()) {
                    // Exchange code for token
                    exchangeCodeForToken(callback.code, redirectUri, codeVerifier);
                    
                    // Fetch username
                    String username = fetchUserDetails();
                    TokenManager.saveUsername(username);
                    
                    Logging.info("DPW OAuth: Authentication successful for user: " + username);
                    result.complete(true);
                } else if (callback.isError()) {
                    Logging.error("DPW OAuth: Authentication error: " + callback.error + " - " + callback.errorDescription);
                    result.complete(false);
                } else {
                    Logging.error("DPW OAuth: Authentication failed - no code received");
                    result.complete(false);
                }
                
            } catch (Exception e) {
                Logging.error("DPW OAuth: Authentication failed: " + e.getMessage());
                Logging.trace(e);
                result.completeExceptionally(e);
            }
        }, "DPW-OAuth-Auth-Thread").start();
        
        return result;
    }
    
    /**
     * Logout (clear all tokens)
     */
    public void logout() {
        TokenManager.clearAllTokens();
        Logging.info("DPW OAuth: User logged out");
    }
    
    /**
     * Build OAuth authorization URL
     */
    private String buildAuthorizationUrl(String redirectUri, String codeChallenge, String state) {
        try {
            StringBuilder url = new StringBuilder(config.getAuthorizationUrl());
            url.append("?response_type=code");
            url.append("&client_id=").append(URLEncoder.encode(CLIENT_ID, "UTF-8"));
            url.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, "UTF-8"));
            url.append("&scope=").append(URLEncoder.encode(SCOPE, "UTF-8"));
            url.append("&state=").append(URLEncoder.encode(state, "UTF-8"));
            url.append("&code_challenge=").append(URLEncoder.encode(codeChallenge, "UTF-8"));
            url.append("&code_challenge_method=S256");
            
            return url.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build authorization URL", e);
        }
    }
    
    /**
     * Exchange authorization code for access token
     */
    private void exchangeCodeForToken(String code, String redirectUri, String codeVerifier) throws Exception {
        String tokenUrl = config.getTokenAcquisitionUrl();
        
        Logging.info("DPW OAuth: Exchanging code for token at: " + tokenUrl);
        
        // Build POST data
        StringBuilder postData = new StringBuilder();
        postData.append("grant_type=authorization_code");
        postData.append("&code=").append(URLEncoder.encode(code, "UTF-8"));
        postData.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, "UTF-8"));
        postData.append("&client_id=").append(URLEncoder.encode(CLIENT_ID, "UTF-8"));
        postData.append("&code_verifier=").append(URLEncoder.encode(codeVerifier, "UTF-8"));
        
        // Make request
        URL url = new URL(tokenUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        
        // Send POST data
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = postData.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = conn.getResponseCode();
        Logging.debug("DPW OAuth: Token endpoint responded with: " + responseCode);
        
        // Read response
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), 
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
        }
        
        String responseBody = response.toString();
        Logging.debug("DPW OAuth: Token response: " + responseBody);
        
        if (responseCode != 200) {
            throw new IOException("Token request failed: HTTP " + responseCode + " - " + responseBody);
        }
        
        // Parse JSON response (simple regex-based parsing)
        String accessToken = extractJsonField(responseBody, "access_token");
        String refreshToken = extractJsonField(responseBody, "refresh_token");
        String expiresInStr = extractJsonField(responseBody, "expires_in");
        
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IOException("No access token in response");
        }
        
        // Calculate expiry time
        long expiresIn = 3600; // Default 1 hour
        if (expiresInStr != null) {
            try {
                expiresIn = Long.parseLong(expiresInStr);
            } catch (NumberFormatException e) {
                Logging.warn("DPW OAuth: Invalid expires_in value: " + expiresInStr);
            }
        }
        
        long expiryTime = System.currentTimeMillis() + (expiresIn * 1000);
        
        // Save tokens
        TokenManager.saveAccessToken(accessToken);
        if (refreshToken != null) {
            TokenManager.saveRefreshToken(refreshToken);
        }
        TokenManager.saveTokenExpiry(expiryTime);
        
        Logging.info("DPW OAuth: Tokens saved successfully (expires in " + expiresIn + " seconds)");
    }
    
    /**
     * Refresh access token using refresh token
     */
    private void refreshAccessToken() throws Exception {
        String refreshToken = TokenManager.loadRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IOException("No refresh token available");
        }
        
        String tokenUrl = config.getTokenAcquisitionUrl();
        
        Logging.info("DPW OAuth: Refreshing access token");
        
        // Build POST data
        StringBuilder postData = new StringBuilder();
        postData.append("grant_type=refresh_token");
        postData.append("&refresh_token=").append(URLEncoder.encode(refreshToken, "UTF-8"));
        postData.append("&client_id=").append(URLEncoder.encode(CLIENT_ID, "UTF-8"));
        
        // Make request
        URL url = new URL(tokenUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = postData.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = conn.getResponseCode();
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), 
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
        }
        
        String responseBody = response.toString();
        
        if (responseCode != 200) {
            throw new IOException("Token refresh failed: HTTP " + responseCode + " - " + responseBody);
        }
        
        // Parse and save new tokens
        String accessToken = extractJsonField(responseBody, "access_token");
        String newRefreshToken = extractJsonField(responseBody, "refresh_token");
        String expiresInStr = extractJsonField(responseBody, "expires_in");
        
        if (accessToken != null && !accessToken.isEmpty()) {
            TokenManager.saveAccessToken(accessToken);
            
            if (newRefreshToken != null && !newRefreshToken.isEmpty()) {
                TokenManager.saveRefreshToken(newRefreshToken);
            }
            
            long expiresIn = 3600;
            if (expiresInStr != null) {
                try {
                    expiresIn = Long.parseLong(expiresInStr);
                } catch (NumberFormatException e) {
                    // Use default
                }
            }
            
            long expiryTime = System.currentTimeMillis() + (expiresIn * 1000);
            TokenManager.saveTokenExpiry(expiryTime);
            
            Logging.info("DPW OAuth: Access token refreshed successfully");
        } else {
            throw new IOException("No access token in refresh response");
        }
    }
    
    /**
     * Fetch user details from OSM API
     */
    private String fetchUserDetails() throws Exception {
        String apiUrl = config.getApiEndpointUrl() + "/0.6/user/details";
        String accessToken = TokenManager.loadAccessToken();
        
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IOException("No access token available");
        }
        
        Logging.info("DPW OAuth: Fetching user details from: " + apiUrl);
        
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/json, application/xml");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        
        int responseCode = conn.getResponseCode();
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), 
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        
        String responseBody = response.toString();
        Logging.debug("DPW OAuth: User details response: " + responseBody);
        
        if (responseCode != 200) {
            throw new IOException("Failed to fetch user details: HTTP " + responseCode);
        }
        
        // Parse username from response (could be JSON or XML)
        String username = extractUsername(responseBody);
        
        if (username == null || username.isEmpty()) {
            throw new IOException("Could not extract username from response");
        }
        
        Logging.info("DPW OAuth: User details fetched: " + username);
        return username;
    }
    
    /**
     * Extract username from user details response
     * Handles both JSON and XML formats
     */
    private String extractUsername(String response) {
        // Try JSON first
        String username = extractJsonField(response, "display_name");
        if (username != null) {
            return username;
        }
        
        // Try XML
        Pattern xmlPattern = Pattern.compile("<user[^>]*display_name=\"([^\"]+)\"");
        Matcher matcher = xmlPattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Try another XML format
        xmlPattern = Pattern.compile("<display_name>([^<]+)</display_name>");
        matcher = xmlPattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Extract field from JSON response (simple regex-based)
     */
    private String extractJsonField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Try without quotes (for numbers)
        pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*([^,}\\s]+)");
        matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Generate PKCE code verifier
     * Random string of 43-128 characters
     */
    private String generateCodeVerifier() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    /**
     * Generate PKCE code challenge from verifier
     * SHA-256 hash of verifier, base64url encoded
     */
    private String generateCodeChallenge(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate code challenge", e);
        }
    }
    
    /**
     * Generate random state parameter for CSRF protection
     */
    private String generateState() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    /**
     * Get current access token (for debugging)
     */
    public String getAccessToken() {
        return TokenManager.loadAccessToken();
    }
    
    /**
     * Get token info for debugging
     */
    public String getTokenInfo() {
        return TokenManager.getTokenInfo();
    }
}
