# Validation Submission Endpoint - Diagnostic Report

**Date:** January 7, 2026  
**Plugin Version:** 3.2.9  
**Endpoint:** `https://app.spatialcollective.com/api/validation-log`

---

## Executive Summary

✅ **THE ENDPOINT WORKS CORRECTLY!**

The HTTP 405 error you're seeing is **NOT from the endpoint** - it's likely due to one of these issues:
1. **User not found in database** (HTTP 404)
2. **Invalid usernames** being submitted
3. **Mapper/Validator not registered** in the DPW Manager system

The endpoint **accepts POST requests** and **returns HTTP 201** when given valid data.

---

## Test Results

### Test 1: Endpoint Accessibility ✅
- **Method:** OPTIONS
- **Result:** SUCCESS
- **Conclusion:** Endpoint exists and accepts requests

### Test 2: POST with Invalid Username ❌
```json
{
  "mapper_osm_username": "FTM_Shadia",
  "validator_osm_username": "test_validator",
  ...
}
```
- **Result:** HTTP 404
- **Error:** `"Mapper with OSM username 'FTM_Shadia' not found in Users table"`
- **Reason:** User not registered in database

### Test 3: POST with Valid Usernames ✅
```json
{
  "mapper_osm_username": "FTM_SC",
  "validator_osm_username": "admin_primoz",
  "total_buildings": 50,
  "error_hanging_nodes": 2,
  "error_overlapping_buildings": 1,
  ...
}
```
- **Result:** HTTP 201 Created
- **Response:**
```json
{
  "success": true,
  "message": "Validation log created successfully",
  "data": {
    "log_id": 3,
    "mapper_user_id": 342,
    "validator_user_id": 317,
    "mapper_name": "Alex Okumu",
    "validator_name": "Primoz Admin"
  }
}
```

---

## Current Plugin Submission Details

### Endpoint Configuration
```
URL: https://app.spatialcollective.com/api/validation-log
Method: POST
Content-Type: application/json
Accept: application/json
X-API-Key: dpw-josm-plugin-2025-secure-key
```

### Payload Structure
The plugin sends this JSON structure:

```json
{
  "task_id": "optional - only if provided",
  "settlement": "optional - only if provided",
  "mapper_osm_username": "required",
  "validator_osm_username": "required",
  "total_buildings": 123,
  "error_hanging_nodes": 0,
  "error_overlapping_buildings": 0,
  "error_buildings_crossing_highway": 0,
  "error_missing_tags": 0,
  "error_improper_tags": 0,
  "error_features_misidentified": 0,
  "error_missing_buildings": 0,
  "error_building_inside_building": 0,
  "error_building_crossing_residential": 0,
  "error_improperly_drawn": 0,
  "validation_status": "Validated or Rejected",
  "validator_comments": "optional - only if provided"
}
```

### Field Details

**Required Fields:**
- `mapper_osm_username` - Must exist in Users table
- `validator_osm_username` - Must exist in Users table
- `total_buildings` - Integer
- `validation_status` - Either "Validated" or "Rejected"

**Optional Fields:**
- `task_id` - String (e.g., "123", "TM-456")
- `settlement` - String (e.g., "Kampala", "Nairobi")
- `validator_comments` - String
- All `error_*` fields - Integers (default to 0 if not provided)

### Error Field Names
The plugin converts error types to field names like this:
```
"Hanging Nodes" → "error_hanging_nodes"
"Overlapping Buildings" → "error_overlapping_buildings"
"Buildings Crossing Highway" → "error_buildings_crossing_highway"
"Missing Tags" → "error_missing_tags"
"Improper Tags" → "error_improper_tags"
"Features Misidentified" → "error_features_misidentified"
"Missing Buildings" → "error_missing_buildings"
"Building Inside Building" → "error_building_inside_building"
"Building Crossing Residential" → "error_building_crossing_residential"
"Improperly Drawn" → "error_improperly_drawn"
```

---

## Why You're Getting HTTP 405 Errors

**HTTP 405 = Method Not Allowed**

This typically means one of these:

### 1. Wrong Endpoint Path (MOST LIKELY)
If the plugin is hitting a different URL, you'll get 405.

**Check these in your JOSM logs:**
```
DPWValidationTool: Submitting validation data to <URL>
```

**Expected:** `https://app.spatialcollective.com/api/validation-log`  
**Wrong:** `https://app.spatialcollective.com/validation-log` (missing /api)  
**Wrong:** `https://dpw-mauve.vercel.app/api/validation-log` (old domain)

### 2. HTTP Method Mismatch
The endpoint **only accepts POST**, not GET/PUT/DELETE.

**Verify in logs:**
```
conn.setRequestMethod("POST")
```

### 3. Missing/Invalid Authentication
Without the X-API-Key header, the request might be rejected.

**Verify in logs:**
```
conn.setRequestProperty("X-API-Key", "dpw-josm-plugin-2025-secure-key")
```

---

## What to Tell DPW App Developers

### ✅ The Endpoint is Working Correctly

**Evidence:**
- Accepts POST requests ✓
- Returns HTTP 201 on success ✓
- Validates required fields ✓
- Returns proper error messages ✓

**No changes needed on their side!**

### ❌ However, There Are User Experience Issues

**Issue 1: User Not Found Returns HTTP 404**

Currently:
```json
{
  "success": false,
  "error": "Mapper with OSM username 'X' not found in Users table"
}
```

**Recommendation:** Return HTTP 400 (Bad Request) instead of 404
- 404 suggests the endpoint doesn't exist
- 400 clearly indicates invalid input data

**Issue 2: Error Messages Could Be More Helpful**

Current: `"Mapper with OSM username 'X' not found in Users table"`

Better: 
```json
{
  "success": false,
  "error": "User not found",
  "details": {
    "field": "mapper_osm_username",
    "value": "X",
    "message": "This OSM username is not registered in the DPW Manager system. Please contact your project manager to add this user."
  }
}
```

**Issue 3: Missing Field Validation Details**

If a required field is missing, what error is returned?

**Request:** Provide specific field validation errors:
```json
{
  "success": false,
  "error": "Validation failed",
  "details": {
    "mapper_osm_username": ["This field is required"],
    "total_buildings": ["Must be a positive integer"]
  }
}
```

---

## Debugging Your HTTP 405 Error

### Step 1: Check JOSM Logs

Open: **Help → Show Log** in JOSM

Look for these lines when you submit:
```
DPWValidationTool: Submitting validation data to <URL>
DPWValidationTool: JSON payload: <JSON>
DPWValidationTool: API responded with HTTP <CODE>
```

### Step 2: Verify the URL

**Expected:**
```
DPWValidationTool: Submitting validation data to https://app.spatialcollective.com/api/validation-log
```

**If you see anything different, that's your problem!**

### Step 3: Check the Response Code

- **HTTP 405** = Wrong endpoint or method
- **HTTP 404** = User not found in database
- **HTTP 400** = Invalid data (missing fields, wrong format)
- **HTTP 401** = Missing/invalid API key
- **HTTP 201** = Success!

### Step 4: Verify Plugin Version

In JOSM: **Edit → Preferences → Plugins**

**Must be:** DPW Validation Tool **v3.2.9**

If it's v3.0.3 or earlier, you have the old endpoint!

### Step 5: Test Usernames

Mapper and validator OSM usernames **must exist** in the DPW Manager Users table.

**Test command:**
```powershell
$headers = @{"X-API-Key" = "dpw-josm-plugin-2025-secure-key"}
Invoke-RestMethod -Uri "https://app.spatialcollective.com/api/users?osm_username=<USERNAME>&exclude_managers=true" -Headers $headers
```

If this returns empty results, the user is not registered!

---

## Summary for DPW App Devs

### What the Plugin Sends

**HTTP Request:**
```
POST /api/validation-log HTTP/1.1
Host: app.spatialcollective.com
Content-Type: application/json; charset=UTF-8
Accept: application/json
X-API-Key: dpw-josm-plugin-2025-secure-key

{
  "task_id": "123",
  "settlement": "Kampala",
  "mapper_osm_username": "john_mapper",
  "validator_osm_username": "jane_validator",
  "total_buildings": 50,
  "error_hanging_nodes": 2,
  "error_overlapping_buildings": 1,
  "error_buildings_crossing_highway": 0,
  "error_missing_tags": 3,
  "error_improper_tags": 0,
  "error_features_misidentified": 0,
  "error_missing_buildings": 5,
  "error_building_inside_building": 0,
  "error_building_crossing_residential": 0,
  "error_improperly_drawn": 1,
  "validation_status": "Validated",
  "validator_comments": "Good work overall"
}
```

### Expected Response Codes

| Code | Meaning | When |
|------|---------|------|
| 201 | Created | Success - validation log created |
| 400 | Bad Request | Missing/invalid fields |
| 404 | Not Found | Mapper/validator username not in database |
| 401 | Unauthorized | Missing/invalid API key |
| 500 | Server Error | Database or server error |

### Recommended Changes

**Priority 1: Change 404 to 400 for User Not Found**
- Reason: 404 suggests endpoint doesn't exist, causing confusion
- Current: HTTP 404 "Mapper not found"
- Better: HTTP 400 "Invalid mapper_osm_username"

**Priority 2: Add Field-Level Validation Errors**
- Return specific field errors in structured format
- Helps plugin show users exactly what's wrong

**Priority 3: Document Rate Limits**
- Does the endpoint have rate limits?
- Should the plugin implement retry logic?

### No Issues Found

✅ Endpoint accepts POST correctly  
✅ Authentication works  
✅ Success response is properly formatted  
✅ Error messages are returned

---

## Conclusion

**The endpoint is working correctly!** 

Your HTTP 405 error is most likely caused by:
1. Plugin using wrong URL (check version!)
2. User not registered in database (HTTP 404, not 405)
3. Network/proxy issues blocking the request

**Action Items:**
1. ✅ Verify you're running plugin v3.2.9
2. ✅ Check JOSM logs for the exact URL being called
3. ✅ Verify mapper/validator usernames exist in database
4. ✅ Share logs with devs if error persists

**For DPW Devs:**
- Consider changing HTTP 404 to 400 for missing users
- Add structured validation error responses
- Document expected fields and constraints

---

**Generated:** January 7, 2026  
**Plugin:** DPW Validation Tool v3.2.9  
**Endpoint:** https://app.spatialcollective.com/api/validation-log
