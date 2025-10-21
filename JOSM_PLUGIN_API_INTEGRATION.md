# JOSM Plugin API Integration Guide
## DPW Manager Backend - Validation Data Submission

**Version:** 1.0  
**Last Updated:** January 2025  
**Production URL:** `https://dpw-mauve.vercel.app`

---

## 📋 Overview

This guide provides the technical specifications for integrating the existing JOSM plugin with the DPW Manager backend API. The plugin must submit validation data to the `/api/validation-log` endpoint, which serves as the **primary and exclusive data source** for all dashboards, reports, and payment calculations in the web application.

---

## 🌐 API Endpoint Details

### Full URL Path
```
https://dpw-mauve.vercel.app/api/validation-log
```

**Local Development URL (if testing locally):**
```
http://localhost:3000/api/validation-log
```

### HTTP Method
```
POST
```

### Base URL
```
Production: https://dpw-mauve.vercel.app
Local: http://localhost:3000
```

---

## 🔐 Authentication

### Method
**No authentication required for validation log submissions.**

The endpoint is currently **open** to allow the JOSM plugin to submit validation data without requiring API keys or bearer tokens. User authorization is handled internally by verifying that the `mapper_osm_username` and `validator_osm_username` exist in the Users database.

### Future Authentication (If Implemented)
If authentication is added in future updates, this guide will be updated with:
- Authentication method (e.g., API Key, Bearer Token)
- Header name and format
- Instructions for obtaining credentials

---

## 📤 Request Headers

The following HTTP headers are **required** for all POST requests:

```http
Content-Type: application/json
```

**Complete Header Example:**
```http
POST /api/validation-log HTTP/1.1
Host: dpw-mauve.vercel.app
Content-Type: application/json
```

---

## 📦 Request Body (JSON Payload)

### JSON Structure

The plugin must send a JSON payload with the following structure:

```json
{
  "task_id": "string (optional)",
  "mapper_osm_username": "string (required)",
  "validator_osm_username": "string (required)",
  "settlement": "string (optional)",
  "total_buildings": integer (required),
  "error_hanging_nodes": integer (optional, defaults to 0),
  "error_overlapping_buildings": integer (optional, defaults to 0),
  "error_buildings_crossing_highway": integer (optional, defaults to 0),
  "error_missing_tags": integer (optional, defaults to 0),
  "error_improper_tags": integer (optional, defaults to 0),
  "error_features_misidentified": integer (optional, defaults to 0),
  "error_missing_buildings": integer (optional, defaults to 0),
  "error_building_inside_building": integer (optional, defaults to 0),
  "error_building_crossing_residential": integer (optional, defaults to 0),
  "error_improperly_drawn": integer (optional, defaults to 0),
  "validation_status": "string (optional, defaults to 'Validated')",
  "validator_comments": "string (optional)"
}
```

### Field Specifications

| Field Name | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `task_id` | String | No | `null` | Unique identifier for the validation task (e.g., "TASK_001") |
| `mapper_osm_username` | String | **Yes** | - | OSM username of the mapper who digitized the buildings. Must exist in Users table. |
| `validator_osm_username` | String | **Yes** | - | OSM username of the validator who checked the work. Must exist in Users table. |
| `settlement` | String | No | `null` | Name of the settlement/area being mapped (e.g., "Kibera", "Mathare") |
| `total_buildings` | Integer | **Yes** | - | Total number of buildings validated in this submission |
| `error_hanging_nodes` | Integer | No | `0` | Count of hanging node errors found |
| `error_overlapping_buildings` | Integer | No | `0` | Count of overlapping buildings errors |
| `error_buildings_crossing_highway` | Integer | No | `0` | Count of buildings crossing highway errors |
| `error_missing_tags` | Integer | No | `0` | Count of missing tag errors |
| `error_improper_tags` | Integer | No | `0` | Count of improper tag errors |
| `error_features_misidentified` | Integer | No | `0` | Count of misidentified feature errors |
| `error_missing_buildings` | Integer | No | `0` | Count of missing building errors |
| `error_building_inside_building` | Integer | No | `0` | Count of building-inside-building errors |
| `error_building_crossing_residential` | Integer | No | `0` | Count of buildings crossing residential area errors |
| `error_improperly_drawn` | Integer | No | `0` | Count of improperly drawn building errors |
| `validation_status` | String | No | `"Validated"` | Status of validation. Valid values: `"Validated"` or `"Rejected"` |
| `validator_comments` | String | No | `null` | Optional comments from the validator (e.g., "Good work overall") |

### Important Notes

1. **Required Fields:** Only three fields are strictly required:
   - `mapper_osm_username`
   - `validator_osm_username`
   - `total_buildings`

2. **Error Counts:** All error fields are optional. If not provided, they default to `0`.

3. **Usernames Must Exist:** Both `mapper_osm_username` and `validator_osm_username` must exist in the Users database, or the request will fail with a 404 error.

4. **Validation Status:** Valid values are `"Validated"` or `"Rejected"`. If omitted, defaults to `"Validated"`.

5. **All Keys Use Snake_Case:** Follow the snake_case naming convention (e.g., `total_buildings`, not `totalBuildings`).

### Example Payload (Complete)

```json
{
  "task_id": "TASK_2025_001",
  "mapper_osm_username": "john_mapper",
  "validator_osm_username": "jane_validator",
  "settlement": "Kibera",
  "total_buildings": 250,
  "error_hanging_nodes": 5,
  "error_overlapping_buildings": 2,
  "error_buildings_crossing_highway": 1,
  "error_missing_tags": 3,
  "error_improper_tags": 4,
  "error_features_misidentified": 0,
  "error_missing_buildings": 2,
  "error_building_inside_building": 1,
  "error_building_crossing_residential": 0,
  "error_improperly_drawn": 3,
  "validation_status": "Validated",
  "validator_comments": "Excellent work! High quality digitization with minimal errors."
}
```

### Example Payload (Minimal - Required Fields Only)

```json
{
  "mapper_osm_username": "john_mapper",
  "validator_osm_username": "jane_validator",
  "total_buildings": 150
}
```

---

## ✅ Success Response

### HTTP Status Code
```
201 Created
```

### Response Body Structure

```json
{
  "success": true,
  "message": "Validation log created successfully",
  "data": {
    "log_id": integer,
    "mapper_user_id": integer,
    "validator_user_id": integer,
    "mapper_name": "string",
    "validator_name": "string"
  }
}
```

### Success Response Example

```json
{
  "success": true,
  "message": "Validation log created successfully",
  "data": {
    "log_id": 123,
    "mapper_user_id": 1,
    "validator_user_id": 2,
    "mapper_name": "John Mapper",
    "validator_name": "Jane Validator"
  }
}
```

### Success Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `success` | Boolean | Always `true` for successful submissions |
| `message` | String | Human-readable success message |
| `data.log_id` | Integer | Database ID of the newly created validation log entry |
| `data.mapper_user_id` | Integer | Internal database ID of the mapper |
| `data.validator_user_id` | Integer | Internal database ID of the validator |
| `data.mapper_name` | String | Full name of the mapper from the database |
| `data.validator_name` | String | Full name of the validator from the database |

---

## ❌ Error Responses

### Error Response Structure

All error responses follow this JSON structure:

```json
{
  "success": false,
  "error": "string - error message"
}
```

### Error Codes and Responses

#### 1. **400 Bad Request - Missing Required Fields**

**HTTP Status Code:** `400`

**Scenario:** The request is missing one or more required fields (`mapper_osm_username`, `validator_osm_username`, or `total_buildings`).

**Response Body:**
```json
{
  "success": false,
  "error": "Missing required fields: mapper_osm_username, validator_osm_username, total_buildings"
}
```

**Plugin Action:** Display an error to the user indicating which fields are missing.

---

#### 2. **404 Not Found - Mapper Not Found**

**HTTP Status Code:** `404`

**Scenario:** The `mapper_osm_username` provided does not exist in the Users database.

**Response Body:**
```json
{
  "success": false,
  "error": "Mapper with OSM username 'john_mapper' not found in Users table"
}
```

**Plugin Action:** Display an error message to the user: "Mapper username not found. Please verify the username or contact your manager to add this user to the system."

---

#### 3. **404 Not Found - Validator Not Found**

**HTTP Status Code:** `404`

**Scenario:** The `validator_osm_username` provided does not exist in the Users database.

**Response Body:**
```json
{
  "success": false,
  "error": "Validator with OSM username 'jane_validator' not found in Users table"
}
```

**Plugin Action:** Display an error message to the user: "Validator username not found. Please verify the username or contact your manager to add this user to the system."

---

#### 4. **500 Internal Server Error**

**HTTP Status Code:** `500`

**Scenario:** An unexpected error occurred on the server (e.g., database connection failure, unhandled exception).

**Response Body:**
```json
{
  "success": false,
  "error": "Internal server error",
  "details": "Connection to database failed"
}
```

**Plugin Action:** Display a generic error message to the user: "Server error occurred. Please try again later or contact support." Log the error details for debugging.

---

## 🧪 Testing the Integration

### Testing Checklist

Before deploying the plugin to production, test the following scenarios:

#### ✅ Success Cases
1. **Minimal Payload:** Send only required fields
2. **Complete Payload:** Send all fields with valid data
3. **With Comments:** Include `validator_comments`
4. **Rejected Status:** Send `validation_status: "Rejected"`

#### ❌ Error Cases
1. **Missing Required Field:** Omit `total_buildings`
2. **Invalid Mapper Username:** Use a non-existent OSM username
3. **Invalid Validator Username:** Use a non-existent OSM username
4. **Invalid JSON:** Send malformed JSON

### Test Script Example (Node.js)

```javascript
// test-validation-log.js
const API_URL = 'https://dpw-mauve.vercel.app/api/validation-log';

async function testValidationSubmission() {
  const testData = {
    task_id: 'TEST_001',
    mapper_osm_username: 'john_mapper',
    validator_osm_username: 'jane_validator',
    settlement: 'Kibera',
    total_buildings: 150,
    error_hanging_nodes: 5,
    error_overlapping_buildings: 2,
    validation_status: 'Validated',
    validator_comments: 'Test submission from plugin'
  };

  try {
    const response = await fetch(API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(testData)
    });

    const result = await response.json();
    console.log('Status:', response.status);
    console.log('Response:', JSON.stringify(result, null, 2));

    if (response.ok) {
      console.log('✅ SUCCESS: Validation log created with ID:', result.data.log_id);
    } else {
      console.log('❌ ERROR:', result.error);
    }
  } catch (error) {
    console.error('❌ REQUEST FAILED:', error.message);
  }
}

testValidationSubmission();
```

### Test with cURL

```bash
# Success Case
curl -X POST https://dpw-mauve.vercel.app/api/validation-log \
  -H "Content-Type: application/json" \
  -d '{
    "mapper_osm_username": "john_mapper",
    "validator_osm_username": "jane_validator",
    "total_buildings": 150,
    "error_hanging_nodes": 5,
    "validator_comments": "Test submission"
  }'

# Expected Response (201 Created):
# {
#   "success": true,
#   "message": "Validation log created successfully",
#   "data": {
#     "log_id": 123,
#     "mapper_user_id": 1,
#     "validator_user_id": 2,
#     "mapper_name": "John Mapper",
#     "validator_name": "Jane Validator"
#   }
# }
```

---

## 📊 Data Flow Overview

```
┌─────────────────┐
│  JOSM Plugin    │
│                 │
│ 1. User maps    │
│ 2. Validator    │
│    checks work  │
│ 3. Plugin       │
│    captures     │
│    metrics      │
└────────┬────────┘
         │
         │ POST /api/validation-log
         │ (JSON payload)
         ▼
┌─────────────────┐
│   Backend API   │
│                 │
│ 1. Validate     │
│    usernames    │
│ 2. Insert into  │
│    database     │
│ 3. Return       │
│    log_id       │
└────────┬────────┘
         │
         │ Stored in database
         ▼
┌─────────────────┐
│   Database      │
│                 │
│ Validation_Logs │
│ table           │
└────────┬────────┘
         │
         │ Processed by
         ▼
┌─────────────────┐
│  Web App        │
│                 │
│ • Dashboard     │
│ • Reports       │
│ • Payments      │
└─────────────────┘
```

---

## 👥 User Management

### Fetching Authorized Users (Optional)

If the plugin needs to fetch a list of authorized users before submission (e.g., for dropdown selections), use:

**Endpoint:**
```
GET /api/users?role=Digitizer,Validator&status=Active
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "user_id": 1,
      "full_name": "John Mapper",
      "osm_username": "john_mapper",
      "role": "Digitizer",
      "status": "Active"
    },
    {
      "user_id": 2,
      "full_name": "Jane Validator",
      "osm_username": "jane_validator",
      "role": "Validator",
      "status": "Active"
    }
  ],
  "count": 2
}
```

---

## 🚨 Important Considerations

### 1. **Usernames Must Be Pre-Registered**
- Both mapper and validator OSM usernames must exist in the Users database before the plugin can submit data
- If a username is not found, the API returns a 404 error
- Managers can add new users through the web application's Users page

### 2. **No Authentication Currently Required**
- The endpoint is currently open for plugin submissions
- Future updates may add API key authentication
- Plugin developers will be notified if authentication is implemented

### 3. **Validation Happens Server-Side**
- The backend validates that usernames exist
- Error counts default to 0 if not provided
- `validation_timestamp` is automatically set to current time on the server

### 4. **Data Integrity**
- All error counts must be non-negative integers
- `total_buildings` must be a positive integer
- `validation_status` accepts only "Validated" or "Rejected"

### 5. **Rate Limiting**
- No rate limiting currently implemented
- Plugin can submit multiple validations in succession
- Future updates may add rate limiting if needed

---

## 📞 Support & Contact

### Technical Questions
- Review the full implementation guide: `DPW_IMPLEMENTATION_GUIDE.md`
- Check database schema: `dpw-app/prisma/schema.prisma`
- Review API code: `dpw-app/app/api/validation-log/route.ts`

### Production URL
```
https://dpw-mauve.vercel.app
```

### Database Users (for testing)
The production database currently has these test users:

| OSM Username | Full Name | Role | Status |
|--------------|-----------|------|--------|
| `john_mapper` | John Mapper | Digitizer | Active |
| `jane_validator` | Jane Validator | Validator | Active |
| `alice_manager` | Alice Manager | Manager | Active |
| `bob_trainee` | Bob Trainee | Trainee | In Training |

---

## 📝 Quick Reference

### Endpoint Summary
```
URL:    https://dpw-mauve.vercel.app/api/validation-log
Method: POST
Auth:   None (currently)
Header: Content-Type: application/json
```

### Required Fields
```json
{
  "mapper_osm_username": "string",
  "validator_osm_username": "string",
  "total_buildings": integer
}
```

### Success Response
```json
{
  "success": true,
  "message": "Validation log created successfully",
  "data": { "log_id": 123, ... }
}
```

### Error Response
```json
{
  "success": false,
  "error": "Error message here"
}
```

---

**Document Version:** 1.0  
**Last Updated:** January 2025  
**Status:** Production Ready ✅
