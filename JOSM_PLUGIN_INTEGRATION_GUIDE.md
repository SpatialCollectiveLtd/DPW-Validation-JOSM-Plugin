# DPW JOSM Plugin Integration Guide
## Complete Developer Documentation for Plugin Integration

**Last Updated:** October 23, 2025  
**API Version:** 1.0  
**Base URL:** `https://your-domain.com/api`

---

## Table of Contents
1. [Overview](#overview)
2. [Critical Terminology](#critical-terminology)
3. [Authentication & Authorization](#authentication--authorization)
4. [Validation Log Submission](#validation-log-submission)
5. [Error Handling](#error-handling)
6. [Complete Code Examples](#complete-code-examples)
7. [Testing & Debugging](#testing--debugging)

---

## Overview

The DPW (Digital Public Works) application manages digitizers (mappers), validators, and their work in OpenStreetMap. The JOSM plugin must:

1. **Authenticate users** before allowing them to work
2. **Submit validation logs** after validators review digitizer work
3. **Handle errors** gracefully and inform users of issues

### System Architecture
```
JOSM Plugin → DPW API → MySQL Database
                ↓
          User Validation
          Log Recording
          Payment Calculation
```

---

## Critical Terminology

⚠️ **IMPORTANT: The plugin uses different terminology than our database. You MUST map these correctly:**

| JOSM Plugin Term | DPW Database Term | Notes |
|-----------------|-------------------|-------|
| **Mapper** | **Digitizer** | Person who maps buildings in OSM |
| **mapper_osm_username** | `osm_username` where `role = 'Digitizer'` | Must match exactly |
| **Validator** | **Validator** | Person who reviews digitizer's work |
| **validator_osm_username** | `osm_username` where `role = 'Validator'` | Must match exactly |

### User Roles in Database
Our database has 4 roles:
- **Digitizer** - Maps buildings (plugin calls them "mappers")
- **Validator** - Reviews work (plugin calls them "validators")  
- **Trainee** - In training, not yet active
- **Manager** - Admin users (⚠️ NEVER expose to plugin!)

---

## Authentication & Authorization

### Step 1: Check if User Exists and is Authorized

**Endpoint:** `GET /api/users`

**Critical Security Rule:** ⚠️ **ALWAYS include `exclude_managers=true`** to prevent exposing admin accounts!

#### Request Format
```http
GET /api/users?osm_username={OSM_USERNAME}&exclude_managers=true&status=Active
```

#### Parameters
| Parameter | Required | Values | Description |
|-----------|----------|--------|-------------|
| `osm_username` | ✅ Yes | String | The OSM username to check |
| `exclude_managers` | ✅ Yes | `true` | **SECURITY**: Prevents exposing Manager accounts |
| `status` | ⚠️ Recommended | `Active` | Only check active users |

#### Response - User Exists and Authorized
```json
{
  "success": true,
  "data": [
    {
      "user_id": 5,
      "youth_id": "DPW-DIGI-003",
      "full_name": "John Doe",
      "osm_username": "john_mapper",
      "settlement": "Mji wa Huruma",
      "role": "Digitizer",
      "status": "Active",
      "start_date": "2024-01-15",
      "created_at": "2024-01-15T10:00:00.000Z"
    }
  ],
  "count": 1
}
```

#### Response - User NOT Authorized
```json
{
  "success": true,
  "data": [],
  "count": 0
}
```

**Reasons for count: 0:**
- OSM username doesn't exist in database
- User is a Manager (blocked by `exclude_managers=true`)
- User status is "Terminated"
- User doesn't exist

#### Authorization Logic
```java
public boolean isUserAuthorized(String osmUsername, String expectedRole) {
    try {
        String apiUrl = String.format(
            "%s/api/users?osm_username=%s&exclude_managers=true&status=Active",
            BASE_URL,
            URLEncoder.encode(osmUsername, "UTF-8")
        );
        
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            return false;
        }
        
        String response = readResponse(conn);
        JSONObject json = new JSONObject(response);
        
        // Check if user was found
        if (!json.getBoolean("success") || json.getInt("count") == 0) {
            return false;
        }
        
        // Verify role matches expected role
        JSONArray users = json.getJSONArray("data");
        if (users.length() == 0) {
            return false;
        }
        
        JSONObject user = users.getJSONObject(0);
        String userRole = user.getString("role");
        
        // Match the role
        return userRole.equals(expectedRole);
        
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

// Usage examples:
boolean isMapperValid = isUserAuthorized("john_mapper", "Digitizer");
boolean isValidatorValid = isUserAuthorized("jane_validator", "Validator");
```

---

## Validation Log Submission

### Step 2: Submit Validation Results

After a validator reviews a digitizer's work, submit the validation log.

**Endpoint:** `POST /api/validation-log`

#### Request Format
```http
POST /api/validation-log
Content-Type: application/json
```

#### Request Body - Complete Structure
```json
{
  "task_id": "TM-12345-Building-Digitization",
  "mapper_osm_username": "john_mapper",
  "validator_osm_username": "jane_validator",
  "settlement": "Mji wa Huruma",
  "total_buildings": 150,
  "error_hanging_nodes": 5,
  "error_overlapping_buildings": 2,
  "error_buildings_crossing_highway": 0,
  "error_missing_tags": 3,
  "error_improper_tags": 1,
  "error_features_misidentified": 0,
  "error_missing_buildings": 4,
  "error_building_inside_building": 1,
  "error_building_crossing_residential": 0,
  "error_improperly_drawn": 2,
  "validation_status": "Validated",
  "validator_comments": "Good work overall. Minor tag corrections needed."
}
```

#### Required Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `mapper_osm_username` | String | ✅ Yes | OSM username of digitizer (NOT our user_id) |
| `validator_osm_username` | String | ✅ Yes | OSM username of validator (NOT our user_id) |
| `total_buildings` | Integer | ✅ Yes | Total buildings mapped/validated |

#### Optional Fields
| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `task_id` | String | `null` | Task/project identifier |
| `settlement` | String | `null` | Settlement/location name |
| `error_hanging_nodes` | Integer | `0` | Count of hanging node errors |
| `error_overlapping_buildings` | Integer | `0` | Count of overlapping buildings |
| `error_buildings_crossing_highway` | Integer | `0` | Buildings crossing highways |
| `error_missing_tags` | Integer | `0` | Missing required tags |
| `error_improper_tags` | Integer | `0` | Incorrect tag usage |
| `error_features_misidentified` | Integer | `0` | Wrong feature type |
| `error_missing_buildings` | Integer | `0` | Buildings not mapped |
| `error_building_inside_building` | Integer | `0` | Nested buildings |
| `error_building_crossing_residential` | Integer | `0` | Buildings crossing landuse=residential |
| `error_improperly_drawn` | Integer | `0` | Geometry issues |
| `validation_status` | String | `"Validated"` | Either `"Validated"` or `"Rejected"` |
| `validator_comments` | String | `null` | Free-text comments from validator |

#### Success Response
```json
{
  "success": true,
  "message": "Validation log created successfully",
  "data": {
    "log_id": 42,
    "mapper_user_id": 5,
    "validator_user_id": 3,
    "mapper_name": "John Doe",
    "validator_name": "Jane Smith"
  }
}
```

#### Error Response - Mapper Not Found
```json
{
  "success": false,
  "error": "Mapper with OSM username 'unknown_mapper' not found in Users table"
}
```

#### Error Response - Validator Not Found
```json
{
  "success": false,
  "error": "Validator with OSM username 'unknown_validator' not found in Users table"
}
```

#### Error Response - Missing Required Fields
```json
{
  "success": false,
  "error": "Missing required fields: mapper_osm_username, validator_osm_username, total_buildings"
}
```

---

## How It Works Behind the Scenes

### What Happens When You Submit a Validation Log

1. **OSM Username Lookup**
   - Plugin sends `mapper_osm_username: "john_mapper"`
   - API queries: `SELECT user_id FROM Users WHERE osm_username = 'john_mapper'`
   - Gets `user_id = 5`

2. **Validation**
   - Checks if mapper exists → if not, returns 404 error
   - Checks if validator exists → if not, returns 404 error

3. **Database Insert**
   - Inserts into `Validation_Logs` table with `mapper_user_id = 5` and `validator_user_id = 3`
   - Auto-generates `validation_timestamp` (current time)
   - Auto-increments `log_id`

4. **Response**
   - Returns the created log with both user_ids and full names

### Database Schema
```sql
CREATE TABLE Validation_Logs (
  log_id INT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(100),
  validation_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  mapper_user_id INT NOT NULL,      -- Looked up from osm_username
  validator_user_id INT NOT NULL,   -- Looked up from osm_username
  settlement VARCHAR(255),
  total_buildings INT DEFAULT 0,
  -- ... error counts ...
  validation_status ENUM('Validated', 'Rejected') DEFAULT 'Validated',
  validator_comments TEXT,
  FOREIGN KEY (mapper_user_id) REFERENCES Users(user_id),
  FOREIGN KEY (validator_user_id) REFERENCES Users(user_id)
);
```

---

## Error Handling

### HTTP Status Codes

| Code | Meaning | Action |
|------|---------|--------|
| `200` | Success (GET) | Process response data |
| `201` | Created (POST) | Validation log created |
| `400` | Bad Request | Check required fields |
| `404` | Not Found | User doesn't exist in database |
| `500` | Server Error | Retry with exponential backoff |

### Handling Errors in Plugin

```java
public class ValidationLogSubmitter {
    
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;
    
    public boolean submitValidationLog(ValidationLogData data) {
        int attempts = 0;
        
        while (attempts < MAX_RETRIES) {
            try {
                HttpURLConnection conn = createConnection();
                sendData(conn, data);
                
                int responseCode = conn.getResponseCode();
                String response = readResponse(conn);
                
                if (responseCode == 201) {
                    // Success!
                    showSuccess("Validation log submitted successfully");
                    return true;
                    
                } else if (responseCode == 404) {
                    // User not found - don't retry
                    JSONObject json = new JSONObject(response);
                    showError(json.getString("error"));
                    return false;
                    
                } else if (responseCode == 400) {
                    // Bad request - don't retry
                    JSONObject json = new JSONObject(response);
                    showError("Invalid data: " + json.getString("error"));
                    return false;
                    
                } else if (responseCode >= 500) {
                    // Server error - retry
                    attempts++;
                    if (attempts < MAX_RETRIES) {
                        Thread.sleep(RETRY_DELAY_MS * attempts);
                        continue;
                    } else {
                        showError("Server error. Please try again later.");
                        return false;
                    }
                }
                
            } catch (Exception e) {
                attempts++;
                e.printStackTrace();
                
                if (attempts >= MAX_RETRIES) {
                    showError("Network error: " + e.getMessage());
                    return false;
                }
                
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        
        return false;
    }
}
```

---

## Complete Code Examples

### Example 1: Complete Authorization Check

```java
import java.net.*;
import java.io.*;
import org.json.*;

public class DPWAuthenticator {
    
    private static final String BASE_URL = "https://your-domain.com";
    
    /**
     * Check if a user is authorized to work on DPW projects
     * 
     * @param osmUsername The OSM username to check
     * @param expectedRole "Digitizer" or "Validator"
     * @return true if authorized, false otherwise
     */
    public static boolean isAuthorized(String osmUsername, String expectedRole) {
        try {
            // Build URL with security parameter
            String urlString = String.format(
                "%s/api/users?osm_username=%s&exclude_managers=true&status=Active",
                BASE_URL,
                URLEncoder.encode(osmUsername, "UTF-8")
            );
            
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Authorization check failed: HTTP " + responseCode);
                return false;
            }
            
            // Read response
            BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            // Parse JSON
            JSONObject json = new JSONObject(response.toString());
            
            if (!json.getBoolean("success")) {
                System.err.println("API returned success: false");
                return false;
            }
            
            int count = json.getInt("count");
            if (count == 0) {
                System.out.println("User not found or not authorized: " + osmUsername);
                return false;
            }
            
            // Get user data
            JSONArray users = json.getJSONArray("data");
            JSONObject user = users.getJSONObject(0);
            
            String role = user.getString("role");
            String status = user.getString("status");
            
            // Verify role matches
            if (!role.equals(expectedRole)) {
                System.out.println(String.format(
                    "Role mismatch: expected %s, got %s",
                    expectedRole, role
                ));
                return false;
            }
            
            // Verify status is Active
            if (!status.equals("Active")) {
                System.out.println("User status is not Active: " + status);
                return false;
            }
            
            System.out.println(String.format(
                "User authorized: %s (%s - %s)",
                user.getString("full_name"),
                user.getString("youth_id"),
                role
            ));
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Authorization error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
```

### Example 2: Complete Validation Log Submission

```java
import java.net.*;
import java.io.*;
import org.json.*;

public class ValidationLogSubmitter {
    
    private static final String BASE_URL = "https://your-domain.com";
    
    public static class ValidationData {
        public String taskId;
        public String mapperOsmUsername;
        public String validatorOsmUsername;
        public String settlement;
        public int totalBuildings;
        public int errorHangingNodes = 0;
        public int errorOverlappingBuildings = 0;
        public int errorBuildingsCrossingHighway = 0;
        public int errorMissingTags = 0;
        public int errorImproperTags = 0;
        public int errorFeaturesHisidentified = 0;
        public int errorMissingBuildings = 0;
        public int errorBuildingInsideBuilding = 0;
        public int errorBuildingCrossingResidential = 0;
        public int errorImproperlyDrawn = 0;
        public String validationStatus = "Validated"; // or "Rejected"
        public String validatorComments;
    }
    
    /**
     * Submit validation log to DPW API
     * 
     * @param data The validation data to submit
     * @return true if successful, false otherwise
     */
    public static boolean submitValidationLog(ValidationData data) {
        try {
            // Create JSON payload
            JSONObject payload = new JSONObject();
            payload.put("task_id", data.taskId);
            payload.put("mapper_osm_username", data.mapperOsmUsername);
            payload.put("validator_osm_username", data.validatorOsmUsername);
            payload.put("settlement", data.settlement);
            payload.put("total_buildings", data.totalBuildings);
            payload.put("error_hanging_nodes", data.errorHangingNodes);
            payload.put("error_overlapping_buildings", data.errorOverlappingBuildings);
            payload.put("error_buildings_crossing_highway", data.errorBuildingsCrossingHighway);
            payload.put("error_missing_tags", data.errorMissingTags);
            payload.put("error_improper_tags", data.errorImproperTags);
            payload.put("error_features_misidentified", data.errorFeaturesHisidentified);
            payload.put("error_missing_buildings", data.errorMissingBuildings);
            payload.put("error_building_inside_building", data.errorBuildingInsideBuilding);
            payload.put("error_building_crossing_residential", data.errorBuildingCrossingResidential);
            payload.put("error_improperly_drawn", data.errorImproperlyDrawn);
            payload.put("validation_status", data.validationStatus);
            payload.put("validator_comments", data.validatorComments);
            
            // Create connection
            URL url = new URL(BASE_URL + "/api/validation-log");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            
            // Send request
            OutputStream os = conn.getOutputStream();
            os.write(payload.toString().getBytes("UTF-8"));
            os.close();
            
            // Get response
            int responseCode = conn.getResponseCode();
            
            // Read response body
            InputStream is;
            if (responseCode >= 200 && responseCode < 300) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            // Parse response
            JSONObject json = new JSONObject(response.toString());
            
            if (responseCode == 201) {
                // Success
                System.out.println("Validation log submitted successfully");
                System.out.println("Log ID: " + json.getJSONObject("data").getInt("log_id"));
                return true;
                
            } else if (responseCode == 404) {
                // User not found
                String error = json.getString("error");
                System.err.println("Error: " + error);
                showUserError("User not found in database. Please contact admin.");
                return false;
                
            } else if (responseCode == 400) {
                // Bad request
                String error = json.getString("error");
                System.err.println("Error: " + error);
                showUserError("Invalid data: " + error);
                return false;
                
            } else {
                // Other error
                String error = json.optString("error", "Unknown error");
                System.err.println("Server error: " + error);
                showUserError("Server error. Please try again later.");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Submission error: " + e.getMessage());
            e.printStackTrace();
            showUserError("Network error: " + e.getMessage());
            return false;
        }
    }
    
    private static void showUserError(String message) {
        // Show error dialog to user
        javax.swing.JOptionPane.showMessageDialog(
            null,
            message,
            "DPW Validation Error",
            javax.swing.JOptionPane.ERROR_MESSAGE
        );
    }
}
```

### Example 3: Complete Workflow

```java
public class DPWPluginWorkflow {
    
    public static void main(String[] args) {
        // Step 1: Authenticate mapper (digitizer)
        String mapperUsername = "john_mapper";
        if (!DPWAuthenticator.isAuthorized(mapperUsername, "Digitizer")) {
            System.err.println("Mapper not authorized!");
            return;
        }
        
        // Step 2: Authenticate validator
        String validatorUsername = "jane_validator";
        if (!DPWAuthenticator.isAuthorized(validatorUsername, "Validator")) {
            System.err.println("Validator not authorized!");
            return;
        }
        
        // Step 3: Perform validation in JOSM
        // ... your validation logic here ...
        
        // Step 4: Prepare validation data
        ValidationLogSubmitter.ValidationData data = new ValidationLogSubmitter.ValidationData();
        data.taskId = "TM-12345-Mji-wa-Huruma";
        data.mapperOsmUsername = mapperUsername;
        data.validatorOsmUsername = validatorUsername;
        data.settlement = "Mji wa Huruma";
        data.totalBuildings = 150;
        data.errorHangingNodes = 5;
        data.errorMissingTags = 3;
        data.validationStatus = "Validated";
        data.validatorComments = "Good work overall. Minor corrections made.";
        
        // Step 5: Submit validation log
        boolean success = ValidationLogSubmitter.submitValidationLog(data);
        
        if (success) {
            System.out.println("✓ Validation workflow completed successfully");
        } else {
            System.err.println("✗ Validation workflow failed");
        }
    }
}
```

---

## Testing & Debugging

### Test Environment
- **API Base URL:** `https://your-domain.com`
- **Test Credentials:** Contact admin for test accounts

### Testing Checklist

#### 1. Test Authorization - Digitizer
```bash
curl -X GET "https://your-domain.com/api/users?osm_username=test_digitizer&exclude_managers=true&status=Active"
```
**Expected:** `count: 1`, `role: "Digitizer"`

#### 2. Test Authorization - Validator
```bash
curl -X GET "https://your-domain.com/api/users?osm_username=test_validator&exclude_managers=true&status=Active"
```
**Expected:** `count: 1`, `role: "Validator"`

#### 3. Test Security - Manager Blocked
```bash
curl -X GET "https://your-domain.com/api/users?osm_username=admin_user&exclude_managers=true"
```
**Expected:** `count: 0` (blocked!)

#### 4. Test Validation Log Submission
```bash
curl -X POST "https://your-domain.com/api/validation-log" \
  -H "Content-Type: application/json" \
  -d '{
    "mapper_osm_username": "test_digitizer",
    "validator_osm_username": "test_validator",
    "settlement": "Test Settlement",
    "total_buildings": 100,
    "error_hanging_nodes": 5,
    "validation_status": "Validated",
    "validator_comments": "Test submission"
  }'
```
**Expected:** `201 Created`, returns `log_id`

### Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| `count: 0` when user exists | Missing `exclude_managers=true` and user is Manager | Always include `exclude_managers=true` |
| `404 User not found` | OSM username doesn't match database | Check exact spelling, case-sensitive |
| `400 Missing required fields` | Missing mapper/validator username or total_buildings | Include all required fields |
| Network timeout | Server slow or unreachable | Increase timeout, add retry logic |
| `mapper_user_id` vs `mapper_osm_username` | Sending user_id instead of username | Always send OSM username, not database ID |

### Debug Logging

Enable detailed logging in your plugin:

```java
public class DPWLogger {
    private static final boolean DEBUG = true;
    
    public static void logRequest(String method, String url, String body) {
        if (DEBUG) {
            System.out.println("=== DPW API REQUEST ===");
            System.out.println("Method: " + method);
            System.out.println("URL: " + url);
            if (body != null) {
                System.out.println("Body: " + body);
            }
            System.out.println("=======================");
        }
    }
    
    public static void logResponse(int code, String body) {
        if (DEBUG) {
            System.out.println("=== DPW API RESPONSE ===");
            System.out.println("Status: " + code);
            System.out.println("Body: " + body);
            System.out.println("========================");
        }
    }
}
```

---

## Quick Reference

### Essential Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/users?osm_username={username}&exclude_managers=true` | GET | Check authorization |
| `/api/validation-log` | POST | Submit validation results |

### Critical Parameters

| Parameter | Required | Example | Notes |
|-----------|----------|---------|-------|
| `exclude_managers` | ✅ Always | `true` | **SECURITY CRITICAL** |
| `mapper_osm_username` | ✅ POST | `"john_mapper"` | OSM username, NOT user_id |
| `validator_osm_username` | ✅ POST | `"jane_validator"` | OSM username, NOT user_id |
| `total_buildings` | ✅ POST | `150` | Required integer |

### Role Mapping

| Plugin Term | Database Value |
|------------|----------------|
| Mapper | `"Digitizer"` |
| Validator | `"Validator"` |

---

## Support & Contact

For implementation questions or API issues:
- **Technical Lead:** [Your contact info]
- **API Documentation:** This document
- **Test Environment:** Contact admin for credentials

---

**Version History:**
- v1.0 (2025-10-23): Initial comprehensive guide
- Added security requirements for `exclude_managers`
- Added complete code examples
- Added troubleshooting guide
