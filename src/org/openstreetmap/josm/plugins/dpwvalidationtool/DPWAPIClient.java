package org.openstreetmap.josm.plugins.dpwvalidationtool;

import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.spi.preferences.Config;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service class for DPW Manager API communication.
 * Handles all HTTP requests to the DPW backend including:
 * - Fetching authorized mappers list
 * - Submitting validation logs
 * - Uploading OSM files to cloud storage
 * - Retrieving user information
 * 
 * @author Spatial Collective Ltd
 * @version 3.0.6
 * @since 3.0.6
 */
public class DPWAPIClient {
    
    private final String baseUrl;
    
    /**
     * Response wrapper for API calls.
     */
    public static class APIResponse {
        public final boolean success;
        public final int statusCode;
        public final String body;
        public final String errorMessage;
        
        public APIResponse(boolean success, int statusCode, String body, String errorMessage) {
            this.success = success;
            this.statusCode = statusCode;
            this.body = body;
            this.errorMessage = errorMessage;
        }
        
        public static APIResponse success(int statusCode, String body) {
            return new APIResponse(true, statusCode, body, null);
        }
        
        public static APIResponse error(int statusCode, String body, String errorMessage) {
            return new APIResponse(false, statusCode, body, errorMessage);
        }
    }
    
    /**
     * User information from the API.
     */
    public static class UserInfo {
        public final String osmUsername;
        public final String settlement;
        public final int userId;
        
        public UserInfo(String osmUsername, String settlement, int userId) {
            this.osmUsername = osmUsername;
            this.settlement = settlement;
            this.userId = userId;
        }
    }
    
    /**
     * Validation submission result.
     */
    public static class ValidationSubmissionResult {
        public final boolean success;
        public final int logId;
        public final String mapperName;
        public final String validatorName;
        public final String errorMessage;
        
        private ValidationSubmissionResult(boolean success, int logId, String mapperName, 
                                           String validatorName, String errorMessage) {
            this.success = success;
            this.logId = logId;
            this.mapperName = mapperName;
            this.validatorName = validatorName;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationSubmissionResult success(int logId, String mapperName, String validatorName) {
            return new ValidationSubmissionResult(true, logId, mapperName, validatorName, null);
        }
        
        public static ValidationSubmissionResult error(String errorMessage) {
            return new ValidationSubmissionResult(false, -1, null, null, errorMessage);
        }
    }
    
    /**
     * Cloud upload result.
     */
    public static class CloudUploadResult {
        public final boolean success;
        public final String driveUrl;
        public final String errorMessage;
        
        private CloudUploadResult(boolean success, String driveUrl, String errorMessage) {
            this.success = success;
            this.driveUrl = driveUrl;
            this.errorMessage = errorMessage;
        }
        
        public static CloudUploadResult success(String driveUrl) {
            return new CloudUploadResult(true, driveUrl, null);
        }
        
        public static CloudUploadResult error(String errorMessage) {
            return new CloudUploadResult(false, null, errorMessage);
        }
    }
    
    /**
     * Create a new API client with the configured base URL.
     */
    public DPWAPIClient() {
        this.baseUrl = PluginSettings.getDPWApiBaseUrl();
    }
    
    /**
     * Create a new API client with a custom base URL.
     * 
     * @param baseUrl the base URL of the DPW API
     */
    public DPWAPIClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    /**
     * Fetch the list of authorized mappers from the API.
     * Only returns active users, excluding managers.
     * 
     * @return list of UserInfo objects
     * @throws IOException if network error occurs
     * @throws APIException if API returns error response
     */
    public List<UserInfo> fetchAuthorizedMappers() throws IOException, APIException {
        // Construct URL with query parameters (SECURITY: exclude_managers=true is REQUIRED)
        String fullUrl = baseUrl + "/users?exclude_managers=true&status=Active";
        
        Logging.debug("DPWValidationTool: Fetching authorized mappers from " + fullUrl);
        
        try {
            URL url = new URI(fullUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "DPW-JOSM-Plugin/3.2.0");
            conn.setConnectTimeout(ValidationConstants.CONNECTION_TIMEOUT_MS);
            conn.setReadTimeout(ValidationConstants.READ_TIMEOUT_MS);
            
            int responseCode = conn.getResponseCode();
            
            // Log rate limit headers (as recommended by DPW team)
            logRateLimitHeaders(conn);
            
            // Read response body
            String responseBody = readResponse(conn, responseCode);
            Logging.debug("DPWValidationTool: API response: " + responseBody);
            
            if (responseCode < 200 || responseCode >= 300) {
                String errorMsg = extractErrorMessage(responseBody);
                throw new APIException("Failed to fetch authorized mappers: HTTP " + responseCode + " - " + errorMsg);
            }
            
            // Parse JSON response
            return parseUserListJson(responseBody);
        } catch (java.net.URISyntaxException e) {
            throw new IOException("Invalid API URL: " + fullUrl, e);
        }
    }
    
    /**
     * Get user ID by OSM username.
     * 
     * @param osmUsername the OSM username to look up
     * @return user ID, or -1 if not found
     */
    public int getUserIdByOsmUsername(String osmUsername) {
        if (osmUsername == null || osmUsername.trim().isEmpty()) {
            return -1;
        }
        
        try {
            String encodedUsername = URLEncoder.encode(osmUsername, StandardCharsets.UTF_8.toString());
            String apiUrl = baseUrl + "/api/users?osm_username=" + encodedUsername + "&exclude_managers=true";
            
            Logging.debug("DPWValidationTool: Fetching user_id for: " + osmUsername);
            
            URL url = new URI(apiUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(ValidationConstants.CONNECTION_TIMEOUT_MS);
            conn.setReadTimeout(ValidationConstants.READ_TIMEOUT_MS);
            
            int responseCode = conn.getResponseCode();
            String responseBody = readResponse(conn, responseCode);
            
            if (responseCode == 200) {
                // Parse user_id from response
                Pattern userIdPattern = Pattern.compile("\"user_id\"\\s*:\\s*(\\d+)");
                Matcher matcher = userIdPattern.matcher(responseBody);
                
                if (matcher.find()) {
                    int userId = Integer.parseInt(matcher.group(1));
                    Logging.info("DPWValidationTool: Found user_id=" + userId + " for " + osmUsername);
                    return userId;
                } else {
                    Logging.warn("DPWValidationTool: No user_id found in response for " + osmUsername);
                    return -1;
                }
            } else {
                Logging.error("DPWValidationTool: Failed to fetch user_id: HTTP " + responseCode);
                return -1;
            }
            
        } catch (Exception ex) {
            Logging.error("DPWValidationTool: Error fetching user_id: " + ex.getMessage());
            Logging.error(ex);
            return -1;
        }
    }
    
    /**
     * Submit validation data to the DPW Manager API.
     * 
     * @param jsonData the JSON payload
     * @return ValidationSubmissionResult with log ID and user names
     * @throws IOException if network error occurs
     */
    public ValidationSubmissionResult submitValidation(String jsonData) throws IOException {
        String apiUrl = baseUrl + "/api/validation-log";
        
        try {
            Logging.info("DPWValidationTool: Submitting validation data to " + apiUrl);
            Logging.debug("DPWValidationTool: JSON payload: " + jsonData);
            
            URL url = new URI(apiUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "DPW-JOSM-Plugin/3.2.0");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            
            // Write JSON payload
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }
            
            int responseCode = conn.getResponseCode();
            Logging.info("DPWValidationTool: API responded with HTTP " + responseCode);
            
            String responseBody = readResponse(conn, responseCode);
            Logging.debug("DPWValidationTool: API response body: " + responseBody);
            
            // Handle different response codes
            if (responseCode == 201) {
                // Success: 201 Created
                return parseValidationSubmissionResponse(responseBody);
                
            } else if (responseCode == 400) {
                // Bad Request
                String errorMsg = extractErrorMessage(responseBody);
                Logging.error("DPWValidationTool: 400 Bad Request - " + errorMsg);
                return ValidationSubmissionResult.error("Invalid data: " + errorMsg);
                
            } else if (responseCode == 404) {
                // User not found
                String errorMsg = extractErrorMessage(responseBody);
                Logging.error("DPWValidationTool: 404 Not Found - " + errorMsg);
                return ValidationSubmissionResult.error("User not found: " + errorMsg);
                
            } else if (responseCode >= 500) {
                // Server error
                String errorMsg = extractErrorMessage(responseBody);
                Logging.error("DPWValidationTool: 500 Server Error - " + errorMsg);
                return ValidationSubmissionResult.error("Server error: " + errorMsg);
                
            } else {
                // Unexpected error
                String errorMsg = extractErrorMessage(responseBody);
                Logging.error("DPWValidationTool: Unexpected HTTP " + responseCode + " - " + errorMsg);
                return ValidationSubmissionResult.error("HTTP " + responseCode + ": " + errorMsg);
            }
            
        } catch (URISyntaxException e) {
            Logging.error("DPWValidationTool: Invalid API URL: " + e.getMessage());
            throw new IOException("Invalid API URL", e);
        } catch (Exception e) {
            Logging.error("DPWValidationTool: Submission exception: " + e.getMessage());
            Logging.error(e);
            return ValidationSubmissionResult.error("Network error: " + e.getMessage());
        }
    }
    
    /**
     * Upload OSM file to cloud storage via DPW API.
     * 
     * @param file the OSM file to upload
     * @param validationLogId the log_id from validation submission
     * @param mapperUserId database user_id of the mapper
     * @param validatorUserId database user_id of the validator
     * @param taskId optional task identifier
     * @param settlement optional settlement name
     * @return CloudUploadResult with Drive URL if successful
     */
    public CloudUploadResult uploadToCloud(File file, int validationLogId, int mapperUserId,
                                           int validatorUserId, String taskId, String settlement) {
        try {
            String apiUrl = baseUrl + "/api/upload-osm";
            
            Logging.info("DPWValidationTool: Uploading to cloud: " + file.getName());
            
            URL url = new URI(apiUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000); // 30 seconds for large files
            conn.setReadTimeout(30000);
            
            // Multipart form data
            String boundary = "----DPWBoundary" + System.currentTimeMillis();
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setRequestProperty("User-Agent", "DPW-JOSM-Plugin/3.2.0");
            
            try (OutputStream os = conn.getOutputStream();
                 OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                
                // Add validation_log_id
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"validation_log_id\"\r\n\r\n");
                writer.append(String.valueOf(validationLogId)).append("\r\n");
                
                // Add mapper_user_id
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"mapper_user_id\"\r\n\r\n");
                writer.append(String.valueOf(mapperUserId)).append("\r\n");
                
                // Add validator_user_id
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"validator_user_id\"\r\n\r\n");
                writer.append(String.valueOf(validatorUserId)).append("\r\n");
                
                // Add task_id (optional)
                if (taskId != null && !taskId.trim().isEmpty()) {
                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"task_id\"\r\n\r\n");
                    writer.append(taskId.trim()).append("\r\n");
                }
                
                // Add settlement (optional)
                if (settlement != null && !settlement.trim().isEmpty()) {
                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"settlement\"\r\n\r\n");
                    writer.append(settlement.trim()).append("\r\n");
                }
                
                // Add file
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                      .append(file.getName()).append("\"\r\n");
                writer.append("Content-Type: application/xml\r\n\r\n");
                writer.flush();
                
                // Write file content
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
                os.flush();
                
                // End boundary
                writer.append("\r\n--").append(boundary).append("--\r\n");
                writer.flush();
            }
            
            int responseCode = conn.getResponseCode();
            String responseBody = readResponse(conn, responseCode);
            Logging.debug("DPWValidationTool: Upload response: " + responseBody);
            
            if (responseCode == 200) {
                // Parse drive_file_url from response
                Pattern urlPattern = Pattern.compile("\"drive_file_url\"\\s*:\\s*\"([^\"]+)\"");
                Matcher matcher = urlPattern.matcher(responseBody);
                
                if (matcher.find()) {
                    String driveUrl = matcher.group(1);
                    Logging.info("DPWValidationTool: Upload successful, Drive URL: " + driveUrl);
                    return CloudUploadResult.success(driveUrl);
                } else {
                    Logging.warn("DPWValidationTool: Upload successful but no drive_file_url in response");
                    return CloudUploadResult.error("No Drive URL in response");
                }
            } else {
                String errorMsg = extractErrorMessage(responseBody);
                Logging.error("DPWValidationTool: Upload failed: HTTP " + responseCode + " - " + errorMsg);
                return CloudUploadResult.error("HTTP " + responseCode + ": " + errorMsg);
            }
            
        } catch (Exception ex) {
            Logging.error("DPWValidationTool: Upload exception: " + ex.getMessage());
            Logging.error(ex);
            return CloudUploadResult.error("Network error: " + ex.getMessage());
        }
    }
    
    // ========== Private Helper Methods ==========
    
    /**
     * Read HTTP response body.
     */
    private String readResponse(HttpURLConnection conn, int responseCode) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString().trim();
    }
    
    /**
     * Log rate limit headers from API response.
     */
    private void logRateLimitHeaders(HttpURLConnection conn) {
        String rateLimitRemaining = conn.getHeaderField("X-RateLimit-Remaining");
        String rateLimitLimit = conn.getHeaderField("X-RateLimit-Limit");
        String rateLimitReset = conn.getHeaderField("X-RateLimit-Reset");
        
        if (rateLimitRemaining != null && rateLimitLimit != null) {
            Logging.info("DPWValidationTool: API rate limit: " + rateLimitRemaining + "/" + rateLimitLimit +
                " (resets at " + rateLimitReset + ")");
            
            // Warn if approaching limit
            try {
                int remaining = Integer.parseInt(rateLimitRemaining);
                if (remaining < 10) {
                    Logging.warn("DPWValidationTool: API rate limit nearly reached: " + remaining + " requests remaining");
                }
            } catch (NumberFormatException ignore) {}
        }
    }
    
    /**
     * Extract error message from API response JSON.
     */
    private String extractErrorMessage(String responseBody) {
        try {
            // Try to extract "error" field from JSON
            Pattern errorPattern = Pattern.compile("\"error\"\\s*:\\s*\"([^\"]+)\"");
            Matcher errorMatcher = errorPattern.matcher(responseBody);
            if (errorMatcher.find()) {
                return errorMatcher.group(1);
            }
            
            // Try to extract "message" field
            Pattern messagePattern = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");
            Matcher messageMatcher = messagePattern.matcher(responseBody);
            if (messageMatcher.find()) {
                return messageMatcher.group(1);
            }
            
            // Return raw response if no structured error found
            return responseBody.isEmpty() ? "No error details" : responseBody;
            
        } catch (Exception e) {
            return responseBody;
        }
    }
    
    /**
     * Parse user list JSON response.
     * Expected format: { "success": true, "data": [...], "count": N }
     */
    private List<UserInfo> parseUserListJson(String json) throws APIException {
        List<UserInfo> users = new ArrayList<>();
        
        try {
            // Extract "data" array
            Pattern dataPattern = Pattern.compile("\"data\"\\s*:\\s*\\[([^\\]]+)\\]");
            Matcher dataMatcher = dataPattern.matcher(json);
            
            if (!dataMatcher.find()) {
                throw new APIException("No 'data' array in response");
            }
            
            String dataArray = dataMatcher.group(1);
            
            // Parse each user object
            Pattern userPattern = Pattern.compile("\\{([^\\}]+)\\}");
            Matcher userMatcher = userPattern.matcher(dataArray);
            
            while (userMatcher.find()) {
                String userObj = userMatcher.group(1);
                
                // Extract osm_username
                Pattern usernamePattern = Pattern.compile("\"osm_username\"\\s*:\\s*\"([^\"]+)\"");
                Matcher usernameMatcher = usernamePattern.matcher(userObj);
                
                // Extract settlement
                Pattern settlementPattern = Pattern.compile("\"settlement\"\\s*:\\s*\"([^\"]+)\"");
                Matcher settlementMatcher = settlementPattern.matcher(userObj);
                
                // Extract user_id
                Pattern userIdPattern = Pattern.compile("\"user_id\"\\s*:\\s*(\\d+)");
                Matcher userIdMatcher = userIdPattern.matcher(userObj);
                
                if (usernameMatcher.find()) {
                    String username = usernameMatcher.group(1);
                    String settlement = settlementMatcher.find() ? settlementMatcher.group(1) : "";
                    int userId = userIdMatcher.find() ? Integer.parseInt(userIdMatcher.group(1)) : -1;
                    
                    users.add(new UserInfo(username, settlement, userId));
                }
            }
            
        } catch (Exception e) {
            throw new APIException("Failed to parse user list: " + e.getMessage(), e);
        }
        
        return users;
    }
    
    /**
     * Parse validation submission response.
     */
    private ValidationSubmissionResult parseValidationSubmissionResponse(String responseBody) {
        try {
            Pattern logIdPattern = Pattern.compile("\"log_id\"\\s*:\\s*(\\d+)");
            Pattern mapperNamePattern = Pattern.compile("\"mapper_name\"\\s*:\\s*\"([^\"]+)\"");
            Pattern validatorNamePattern = Pattern.compile("\"validator_name\"\\s*:\\s*\"([^\"]+)\"");
            
            Matcher logIdMatcher = logIdPattern.matcher(responseBody);
            Matcher mapperNameMatcher = mapperNamePattern.matcher(responseBody);
            Matcher validatorNameMatcher = validatorNamePattern.matcher(responseBody);
            
            int logId = logIdMatcher.find() ? Integer.parseInt(logIdMatcher.group(1)) : -1;
            String mapperName = mapperNameMatcher.find() ? mapperNameMatcher.group(1) : "";
            String validatorName = validatorNameMatcher.find() ? validatorNameMatcher.group(1) : "";
            
            if (logId > 0) {
                return ValidationSubmissionResult.success(logId, mapperName, validatorName);
            } else {
                return ValidationSubmissionResult.error("No log_id in response");
            }
            
        } catch (Exception e) {
            Logging.warn("DPWValidationTool: Could not parse success response details: " + e.getMessage());
            return ValidationSubmissionResult.error("Failed to parse response: " + e.getMessage());
        }
    }
    
    /**
     * Custom exception for API errors.
     */
    public static class APIException extends Exception {
        public APIException(String message) {
            super(message);
        }
        
        public APIException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
