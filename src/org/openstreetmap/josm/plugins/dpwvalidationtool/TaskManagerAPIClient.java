package org.openstreetmap.josm.plugins.dpwvalidationtool;

import org.openstreetmap.josm.tools.Logging;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client for interacting with HOT Tasking Manager API
 * Handles task information retrieval and mapper detection
 * 
 * @version 3.1.0-BETA
 */
public class TaskManagerAPIClient {
    
    // URL patterns
    private static final Pattern TM_URL_PATTERN = Pattern.compile(
        "(?:https?://)?(?:www\\.)?tasks\\.hotosm\\.org/projects/(\\d+)(?:/tasks/)?(\\d+)?");
    private static final Pattern CHANGESET_COMMENT_PATTERN = Pattern.compile(
        "#hotosm-project-(\\d+)-task-(\\d+)");
    
    /**
     * Result class for TM task information
     */
    public static class TaskInfo {
        public final int projectId;
        public final int taskId;
        public final String mapperUsername;
        public final String taskStatus;
        public final boolean success;
        public final String errorMessage;
        
        public TaskInfo(int projectId, int taskId, String mapperUsername, 
                       String taskStatus, boolean success, String errorMessage) {
            this.projectId = projectId;
            this.taskId = taskId;
            this.mapperUsername = mapperUsername;
            this.taskStatus = taskStatus;
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public static TaskInfo error(String message) {
            return new TaskInfo(-1, -1, null, null, false, message);
        }
        
        public static TaskInfo success(int projectId, int taskId, String mapper, String status) {
            return new TaskInfo(projectId, taskId, mapper, status, true, null);
        }
    }
    
    /**
     * Parse Task Manager URL to extract project and task IDs
     * Supports formats:
     * - https://tasks.hotosm.org/projects/12345/tasks/678
     * - https://tasks.hotosm.org/projects/12345#task/678
     * - tasks.hotosm.org/projects/12345 (project only)
     */
    public static int[] parseTaskManagerURL(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        // Handle #task/ format by replacing with /tasks/
        url = url.replace("#task/", "/tasks/");
        
        Matcher matcher = TM_URL_PATTERN.matcher(url);
        if (matcher.find()) {
            int projectId = Integer.parseInt(matcher.group(1));
            int taskId = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : -1;
            return new int[]{projectId, taskId};
        }
        
        return null;
    }
    
    /**
     * Parse changeset comment to extract project and task IDs
     * Detects format: #hotosm-project-12345-task-678
     */
    public static int[] parseChangesetComment(String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            return null;
        }
        
        Matcher matcher = CHANGESET_COMMENT_PATTERN.matcher(comment);
        if (matcher.find()) {
            int projectId = Integer.parseInt(matcher.group(1));
            int taskId = Integer.parseInt(matcher.group(2));
            return new int[]{projectId, taskId};
        }
        
        return null;
    }
    
    /**
     * Fetch task information from Tasking Manager API
     * Returns mapper username and task status
     */
    public static TaskInfo fetchTaskInfo(int projectId, int taskId) {
        try {
            String apiUrl = PluginSettings.getTMApiBaseUrl() + "/projects/" 
                + projectId + "/tasks/" + taskId + "/";
            
            Logging.info("Fetching TM task info: " + apiUrl);
            
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000); // 10 second timeout
            conn.setReadTimeout(10000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return TaskInfo.error("TM API returned status: " + responseCode);
            }
            
            // Read response
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            String jsonResponse = response.toString();
            
            // Parse JSON response manually
            String taskStatus = extractJsonStringField(jsonResponse, "taskStatus");
            if (taskStatus == null) {
                taskStatus = "UNKNOWN";
            }
            
            // Find mapper from task history
            String mapperUsername = null;
            
            // First try to extract from taskHistory array
            int historyStart = jsonResponse.indexOf("\"taskHistory\"");
            if (historyStart != -1) {
                int arrayStart = jsonResponse.indexOf('[', historyStart);
                if (arrayStart != -1) {
                    int arrayEnd = findMatchingBracket(jsonResponse, arrayStart);
                    if (arrayEnd != -1) {
                        String historyArray = jsonResponse.substring(arrayStart + 1, arrayEnd);
                        
                        // Parse history entries in reverse order (most recent first)
                        String[] entries = splitJsonArray(historyArray);
                        for (int i = entries.length - 1; i >= 0; i--) {
                            String entry = entries[i];
                            String action = extractJsonStringField(entry, "action");
                            
                            if ("STATE_CHANGE".equals(action)) {
                                String actionText = extractJsonStringField(entry, "actionText");
                                if (actionText != null && (actionText.contains("MAPPED") || actionText.contains("BADIMAGERY"))) {
                                    mapperUsername = extractJsonStringField(entry, "actionBy");
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            
            // Also check mappedBy field in properties
            if (mapperUsername == null) {
                int propsStart = jsonResponse.indexOf("\"properties\"");
                if (propsStart != -1) {
                    int objStart = jsonResponse.indexOf('{', propsStart);
                    if (objStart != -1) {
                        int objEnd = findMatchingBrace(jsonResponse, objStart);
                        if (objEnd != -1) {
                            String properties = jsonResponse.substring(objStart, objEnd + 1);
                            mapperUsername = extractJsonStringField(properties, "mappedBy");
                        }
                    }
                }
            }
            
            if (mapperUsername == null || mapperUsername.trim().isEmpty()) {
                return TaskInfo.error("No mapper found for this task. Task may not be mapped yet.");
            }
            
            Logging.info("Found mapper: " + mapperUsername + " for task " + taskId);
            return TaskInfo.success(projectId, taskId, mapperUsername, taskStatus);
            
        } catch (Exception e) {
            Logging.error("Error fetching TM task info: " + e.getMessage());
            return TaskInfo.error("Failed to fetch task info: " + e.getMessage());
        }
    }
    
    /**
     * Fetch task info from TM URL
     */
    public static TaskInfo fetchTaskInfoFromURL(String tmUrl) {
        int[] ids = parseTaskManagerURL(tmUrl);
        if (ids == null || ids.length < 2 || ids[1] == -1) {
            return TaskInfo.error("Invalid Task Manager URL. Please include task ID.");
        }
        
        return fetchTaskInfo(ids[0], ids[1]);
    }
    
    /**
     * Check if a changeset comment contains TM task information
     */
    public static boolean hasTaskManagerInfo(String changesetComment) {
        return parseChangesetComment(changesetComment) != null;
    }
    
    // ========================================================================================
    // JSON Parsing Helper Methods (manual parsing, no library dependencies)
    // ========================================================================================
    
    /**
     * Extract a string field value from a JSON string
     */
    private static String extractJsonStringField(String json, String fieldName) {
        int fieldStart = json.indexOf("\"" + fieldName + "\"");
        if (fieldStart == -1) {
            return null;
        }
        
        int colonPos = json.indexOf(':', fieldStart);
        if (colonPos == -1) {
            return null;
        }
        
        // Skip whitespace after colon
        int pos = colonPos + 1;
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }
        
        if (pos >= json.length()) {
            return null;
        }
        
        // Check if value is a string (starts with quote) or null
        char firstChar = json.charAt(pos);
        if (firstChar == 'n') {
            // Might be null
            if (json.startsWith("null", pos)) {
                return null;
            }
        }
        
        if (firstChar != '"') {
            // Not a string value
            return null;
        }
        
        // Extract string value until closing quote
        int stringStart = pos + 1;
        int stringEnd = stringStart;
        while (stringEnd < json.length()) {
            char c = json.charAt(stringEnd);
            if (c == '"' && (stringEnd == stringStart || json.charAt(stringEnd - 1) != '\\')) {
                break;
            }
            stringEnd++;
        }
        
        if (stringEnd >= json.length()) {
            return null;
        }
        
        return json.substring(stringStart, stringEnd);
    }
    
    /**
     * Find the matching closing bracket for an opening bracket
     */
    private static int findMatchingBracket(String json, int openPos) {
        int depth = 0;
        for (int i = openPos; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
    
    /**
     * Find the matching closing brace for an opening brace
     */
    private static int findMatchingBrace(String json, int openPos) {
        int depth = 0;
        for (int i = openPos; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
    
    /**
     * Split a JSON array string into individual object strings
     */
    private static String[] splitJsonArray(String arrayContent) {
        java.util.List<String> objects = new java.util.ArrayList<>();
        int depth = 0;
        int objStart = 0;
        
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    objStart = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    objects.add(arrayContent.substring(objStart, i + 1));
                }
            }
        }
        
        return objects.toArray(new String[0]);
    }
}

