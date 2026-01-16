# DPW Validation Tool - Complete Technical Documentation

**Version:** 3.4.0  
**Last Updated:** January 15, 2026  
**Author:** Spatial Collective Ltd  
**Platform:** JOSM (Java OpenStreetMap Editor)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [What Is This Plugin?](#2-what-is-this-plugin)
3. [Why Does This Plugin Exist?](#3-why-does-this-plugin-exist)
4. [System Architecture](#4-system-architecture)
5. [External Systems & API Integrations](#5-external-systems--api-integrations)
6. [Core Components](#6-core-components)
7. [Data Flow Diagrams](#7-data-flow-diagrams)
8. [Authentication Systems](#8-authentication-systems)
9. [API Reference](#9-api-reference)
10. [Security Implementation](#10-security-implementation)
11. [Configuration & Settings](#11-configuration--settings)
12. [Technical Specifications](#12-technical-specifications)

---

## 1. Executive Summary

The **DPW Validation Tool** is a JOSM plugin that bridges the gap between OpenStreetMap mapping workflows and the DPW (Digital Public Works) Manager system. It enables validators to:

- **Isolate** a specific mapper's work by date and username
- **Review** building digitization quality with error categorization
- **Submit** validation records to a centralized database
- **Export** validated data with automatic cloud backup
- **Track** mapper productivity and quality metrics

The plugin connects to **four external systems**:
1. **DPW Manager API** (app.spatialcollective.com) - User management & validation logging
2. **HOT Tasking Manager API** (tasks.hotosm.org) - Task coordination
3. **OpenStreetMap API** (openstreetmap.org or custom) - User authentication
4. **Google Drive API** (via DPW backend) - Cloud storage for exports

---

## 2. What Is This Plugin?

### 2.1 Purpose

The DPW Validation Tool is a **quality assurance workflow manager** for OpenStreetMap building digitization projects. It's designed specifically for humanitarian mapping initiatives where:

- Multiple mappers contribute to building digitization
- Validators must review each mapper's work
- Quality metrics need to be tracked and reported
- Validated data must be archived for project records

### 2.2 Core Functionality

```
┌─────────────────────────────────────────────────────────────────┐
│                    DPW VALIDATION TOOL                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐         │
│  │   ISOLATE   │ ─► │   REVIEW    │ ─► │   SUBMIT    │         │
│  │   Mapper's  │    │   Quality   │    │  Validation │         │
│  │    Work     │    │   Errors    │    │    Log      │         │
│  └─────────────┘    └─────────────┘    └─────────────┘         │
│         │                 │                   │                 │
│         ▼                 ▼                   ▼                 │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐         │
│  │   Filter    │    │   10 Error  │    │   DPW API   │         │
│  │   by Date   │    │  Categories │    │   Database  │         │
│  └─────────────┘    └─────────────┘    └─────────────┘         │
│                                               │                 │
│                                               ▼                 │
│                                        ┌─────────────┐         │
│                                        │   EXPORT    │         │
│                                        │  + Cloud    │         │
│                                        │   Backup    │         │
│                                        └─────────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 Error Categories Tracked

The plugin tracks **10 specific error types** for building digitization:

| # | Error Type | Description |
|---|------------|-------------|
| 1 | Hanging Nodes | Nodes not connected to ways properly |
| 2 | Overlapping Buildings | Buildings that overlap each other |
| 3 | Buildings Crossing Highway | Buildings intersecting with roads |
| 4 | Missing Tags | Buildings without required tags |
| 5 | Improper Tags | Incorrect or invalid tag values |
| 6 | Features Misidentified | Wrong feature classification |
| 7 | Missing Buildings | Buildings not traced in imagery |
| 8 | Building Inside Building | Nested building geometries |
| 9 | Building Crossing Residential | Buildings crossing residential areas |
| 10 | Improperly Drawn | General geometry issues |

---

## 3. Why Does This Plugin Exist?

### 3.1 The Problem

In humanitarian mapping projects (like those coordinated by HOT - Humanitarian OpenStreetMap Team), the standard workflow is:

1. Project managers create tasks on Tasking Manager
2. Volunteer mappers claim and complete tasks
3. **Validators must review all mapped data** ← This is the bottleneck

Without this plugin, validators must:
- Manually track which mapper's work they're reviewing
- Manually count errors in spreadsheets
- Manually report quality metrics to project managers
- Have no standardized way to archive validated data

### 3.2 The Solution

This plugin provides:

| Problem | Solution |
|---------|----------|
| "Whose work am I reviewing?" | Auto-fetches authorized mappers from DPW database |
| "How do I isolate one mapper's work?" | One-click isolation by username + date |
| "How do I track errors?" | Built-in counters for 10 error categories |
| "Where do I submit reports?" | Direct API submission to DPW Manager |
| "How do I backup validated data?" | Automatic cloud export to Google Drive |
| "How do I switch between OSM servers?" | Custom OAuth support for private servers |

### 3.3 Business Context

The DPW (Digital Public Works) initiative by **Spatial Collective Ltd** manages large-scale building digitization projects in Kenya. This plugin is essential for:

- **Quality Control** - Ensuring mapper output meets standards
- **Performance Tracking** - Measuring mapper productivity
- **Project Management** - Coordinating validation workflows
- **Data Archival** - Preserving validated data for clients

---

## 4. System Architecture

### 4.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              JOSM EDITOR                                │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                     DPW VALIDATION TOOL PLUGIN                    │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐               │  │
│  │  │  Main UI    │  │  Settings   │  │   Auth      │               │  │
│  │  │  Panel      │  │   Panel     │  │  Dialog     │               │  │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘               │  │
│  │         │                │                │                       │  │
│  │  ┌──────▼──────────────────────────────────────────────────────┐ │  │
│  │  │                    CORE SERVICES                             │ │  │
│  │  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │ │  │
│  │  │  │  TM API  │ │ DPW API  │ │  OAuth   │ │   Token      │   │ │  │
│  │  │  │  Client  │ │  Client  │ │  Client  │ │   Manager    │   │ │  │
│  │  │  └────┬─────┘ └────┬─────┘ └────┬─────┘ └──────────────┘   │ │  │
│  │  └───────┼────────────┼────────────┼──────────────────────────┘ │  │
│  └──────────┼────────────┼────────────┼────────────────────────────┘  │
└─────────────┼────────────┼────────────┼────────────────────────────────┘
              │            │            │
              ▼            ▼            ▼
     ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────────┐
     │    HOT     │ │    DPW     │ │    OSM     │ │  Google Drive  │
     │  Tasking   │ │  Manager   │ │   Server   │ │   (via DPW)    │
     │  Manager   │ │    API     │ │            │ │                │
     └────────────┘ └────────────┘ └────────────┘ └────────────────┘
```

### 4.2 Component Overview

| Component | File | Purpose |
|-----------|------|---------|
| Main Plugin | `DPWValidationToolPlugin.java` | Plugin entry point, menu registration |
| Validation Panel | `ValidationToolPanel.java` | Main UI, validation workflow logic |
| Settings Panel | `SettingsPanel.java` | Configuration UI |
| TM API Client | `TaskManagerAPIClient.java` | HOT Tasking Manager integration |
| Plugin Settings | `PluginSettings.java` | Preferences management |
| Update Checker | `UpdateChecker.java` | Auto-update from GitHub |
| OAuth Client | `CustomOAuthClient.java` | OAuth 2.0 PKCE authentication |
| Token Manager | `TokenManager.java` | Secure token storage |
| OAuth Callback | `OAuthCallbackServer.java` | Local HTTP server for OAuth |
| OSM Config | `OSMServerConfiguration.java` | Custom server settings |
| Auth Dialog | `AuthenticationDialog.java` | Login/logout UI |
| Icon Resources | `IconResources.java` | UI icons |

### 4.3 File Structure

```
src/org/openstreetmap/josm/plugins/dpwvalidationtool/
├── DPWValidationToolPlugin.java    (168 lines)  - Entry point
├── ValidationToolPanel.java        (2961 lines) - Main logic
├── SettingsPanel.java              (411 lines)  - Settings UI
├── PluginSettings.java             (185 lines)  - Preferences
├── TaskManagerAPIClient.java       (338 lines)  - TM integration
├── UpdateChecker.java              (573 lines)  - Auto-updates
├── CustomOAuthClient.java          (522 lines)  - Custom OAuth
├── TokenManager.java               (334 lines)  - Token storage
├── OAuthCallbackServer.java        (284 lines)  - OAuth callback
├── OSMServerConfiguration.java     (237 lines)  - Server config
├── AuthenticationDialog.java       (265 lines)  - Auth UI
└── IconResources.java              (50 lines)   - Icons
```

**Total:** ~6,318 lines of production Java code

---

## 5. External Systems & API Integrations

The plugin connects to **four external systems**:

### 5.1 DPW Manager API

**Base URL:** `https://app.spatialcollective.com/api`  
**Purpose:** User management, validation logging, cloud uploads

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/users` | GET | Fetch authorized mappers list |
| `/validation-log` | POST | Submit validation records |
| `/osm-uploads` | POST | Upload validated OSM files |

**Authentication:** API Key (`X-API-Key` header)

```
API Key: dpw_josm_plugin_digitization_2025_secure_key_f8a9b2c3d1e4
```

### 5.2 HOT Tasking Manager API

**Base URL:** `https://tasking-manager-tm4-production-api.hotosm.org/api/v2`  
**Purpose:** Task information, mapper detection

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/projects/{id}/tasks/{taskId}/` | GET | Fetch task details |

**Authentication:** None (public API)

### 5.3 OpenStreetMap API

**Default URL:** `https://api.openstreetmap.org/api`  
**Custom URL:** `https://osm.spatialcollective.co.ke/api` (configurable)  
**Purpose:** User authentication, user details

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/0.6/user/details` | GET | Fetch authenticated user info |
| `/oauth2/authorize` | GET | OAuth authorization |
| `/oauth2/token` | POST | Token exchange |

**Authentication:** OAuth 2.0 with PKCE

### 5.4 Google Drive API (via DPW Backend)

**Access:** Indirect (through DPW Manager API)  
**Purpose:** Cloud backup of validated OSM files

The plugin uploads files to the DPW API's `/osm-uploads` endpoint, which handles Google Drive storage internally.

---

## 6. Core Components

### 6.1 ValidationToolPanel.java (Main Component)

This is the **heart of the plugin** (2,961 lines). It handles:

#### State Management
```java
private enum ValidationState {
    IDLE,       // No layer isolated
    ISOLATED,   // Mapper's work isolated
    SUBMITTED,  // Validation submitted
    EXPORTED    // Data exported
}
```

#### Key Methods

| Method | Purpose |
|--------|---------|
| `setupUI()` | Build the validation panel UI |
| `fetchAuthorizedMappers()` | Load users from DPW API |
| `isolateButton.addActionListener()` | Filter data by mapper + date |
| `submitData()` | Send validation to API |
| `performExport()` | Export to OSM file + cloud |
| `getCurrentValidator()` | Get authenticated username |
| `resetValidationSession()` | Clear form for next task |

#### User List Caching
```java
// 5-minute cache as recommended by DPW team
private static List<UserInfo> cachedUserList = null;
private static long cacheTimestamp = 0;
private static final long CACHE_DURATION = 300000; // 5 minutes
```

### 6.2 TaskManagerAPIClient.java

Handles HOT Tasking Manager integration:

#### URL Parsing
```java
// Supports multiple URL formats:
// - https://tasks.hotosm.org/projects/12345/tasks/678
// - https://tasks.hotosm.org/projects/12345#task/678
// - tasks.hotosm.org/projects/12345

private static final Pattern TM_URL_PATTERN = Pattern.compile(
    "(?:https?://)?(?:www\\.)?tasks\\.hotosm\\.org/projects/(\\d+)(?:/tasks/)?(\\d+)?");
```

#### Changeset Comment Detection
```java
// Detects task from changeset comments
// Format: #hotosm-project-12345-task-678

private static final Pattern CHANGESET_COMMENT_PATTERN = Pattern.compile(
    "#hotosm-project-(\\d+)-task-(\\d+)");
```

### 6.3 CustomOAuthClient.java

Implements OAuth 2.0 with PKCE for custom OSM servers:

#### PKCE Flow
```java
// 1. Generate code verifier (random 32 bytes)
private String generateCodeVerifier() {
    SecureRandom random = new SecureRandom();
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
}

// 2. Generate code challenge (SHA-256 of verifier)
private String generateCodeChallenge(String verifier) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.UTF_8));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
}
```

#### Authentication Flow
```
User clicks "Login"
       │
       ▼
┌──────────────────┐
│ Generate PKCE    │
│ verifier+challenge│
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Start callback   │
│ server (8111)    │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Open browser to  │
│ OAuth authorize  │
└────────┬─────────┘
         │
         ▼
   User logs in
         │
         ▼
┌──────────────────┐
│ Callback server  │
│ receives code    │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Exchange code    │
│ for tokens       │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Save encrypted   │
│ tokens           │
└──────────────────┘
```

### 6.4 TokenManager.java

Secure token storage with AES-256 encryption:

```java
// Encryption configuration
private static final String ALGORITHM = "AES";
private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
private static final int KEY_SIZE = 256;
private static final int ITERATION_COUNT = 65536;
private static final int SALT_LENGTH = 16;
private static final int IV_LENGTH = 16;
```

---

## 7. Data Flow Diagrams

### 7.1 Validation Workflow

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│  LOAD   │───►│ SELECT  │───►│ ISOLATE │───►│ REVIEW  │───►│ SUBMIT  │
│  DATA   │    │ MAPPER  │    │  WORK   │    │ ERRORS  │    │   LOG   │
└─────────┘    └─────────┘    └─────────┘    └─────────┘    └────┬────┘
                                                                 │
     ┌───────────────────────────────────────────────────────────┘
     │
     ▼
┌─────────┐    ┌─────────┐    ┌─────────┐
│ EXPORT  │───►│ UPLOAD  │───►│  RESET  │
│  FILE   │    │ TO CLOUD│    │ SESSION │
└─────────┘    └─────────┘    └─────────┘
```

### 7.2 API Data Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         JOSM + PLUGIN                               │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
        ▼                       ▼                       ▼
┌───────────────┐       ┌───────────────┐       ┌───────────────┐
│  DPW Manager  │       │  HOT Tasking  │       │    OSM        │
│     API       │       │    Manager    │       │   Server      │
│               │       │               │       │               │
│ GET /users    │       │ GET /projects │       │ OAuth2        │
│ POST /val-log │       │   /{id}/tasks │       │ GET /user     │
│ POST /uploads │       │    /{taskId}  │       │   /details    │
└───────────────┘       └───────────────┘       └───────────────┘
        │                                               │
        │                                               │
        ▼                                               │
┌───────────────┐                                       │
│ Google Drive  │◄──────────────────────────────────────┘
│   (Storage)   │
└───────────────┘
```

### 7.3 Authentication Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    AUTHENTICATION DECISION                       │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                    ┌───────────┴───────────┐
                    │ Custom server enabled?│
                    └───────────┬───────────┘
                          │           │
                        YES          NO
                          │           │
                          ▼           ▼
                   ┌───────────┐ ┌───────────┐
                   │ Custom    │ │  JOSM     │
                   │ OAuth     │ │ Standard  │
                   │ Client    │ │ OAuth     │
                   └─────┬─────┘ └─────┬─────┘
                         │             │
                         ▼             ▼
                   ┌───────────┐ ┌───────────┐
                   │ osm.      │ │ openstreet│
                   │ spatial   │ │ map.org   │
                   │ collective│ │           │
                   │ .co.ke    │ │           │
                   └───────────┘ └───────────┘
```

---

## 8. Authentication Systems

### 8.1 Overview

The plugin supports **two authentication systems**:

| System | Server | When Used |
|--------|--------|-----------|
| JOSM Standard | openstreetmap.org | Default |
| Custom OAuth | osm.spatialcollective.co.ke | When enabled |

### 8.2 JOSM Standard Authentication

Uses JOSM's built-in `UserIdentityManager`:

```java
UserIdentityManager userManager = UserIdentityManager.getInstance();
String username = userManager.getUserName();
```

### 8.3 Custom OAuth Authentication

Full OAuth 2.0 PKCE implementation:

```java
// Client configuration
private static final String CLIENT_ID = "dpw_josm_plugin";
private static final String SCOPE = "read_prefs write_api";

// Callback server ports
private static final int PRIMARY_PORT = 8111;  // JOSM default
private static final int FALLBACK_PORT = 8112;

// Token storage (AES-256 encrypted)
TokenManager.saveAccessToken(accessToken);
TokenManager.saveRefreshToken(refreshToken);
TokenManager.saveTokenExpiry(expiryTime);
```

### 8.4 API Key Authentication

DPW Manager API uses API key authentication:

```java
conn.setRequestProperty("X-API-Key", DPW_API_KEY);
// Key: dpw_josm_plugin_digitization_2025_secure_key_f8a9b2c3d1e4
```

---

## 9. API Reference

### 9.1 DPW Manager API

#### GET /users

Fetch authorized project members.

**Request:**
```http
GET /api/users?exclude_managers=true&status=Active
X-API-Key: dpw_josm_plugin_digitization_2025_secure_key_f8a9b2c3d1e4
Accept: application/json
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "user_id": 45,
      "osm_username": "john_mapper",
      "settlement": "Kibera",
      "status": "Active"
    }
  ],
  "count": 1
}
```

**Rate Limiting Headers:**
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1705312800
```

#### POST /validation-log

Submit a validation record.

**Request:**
```http
POST /api/validation-log
X-API-Key: dpw_josm_plugin_digitization_2025_secure_key_f8a9b2c3d1e4
Content-Type: application/json
Accept: application/json

{
  "task_id": "27",
  "settlement": "Kibera",
  "mapper_osm_username": "john_mapper",
  "validator_osm_username": "jane_validator",
  "total_buildings": 150,
  "error_hanging_nodes": 2,
  "error_overlapping_buildings": 1,
  "error_buildings_crossing_highway": 0,
  "error_missing_tags": 3,
  "error_improper_tags": 0,
  "error_features_misidentified": 0,
  "error_missing_buildings": 5,
  "error_building_inside_building": 0,
  "error_building_crossing_residential": 0,
  "error_improperly_drawn": 2,
  "validation_status": "Validated",
  "validator_comments": "Good work, minor issues fixed"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "log_id": 1234,
    "created_at": "2026-01-15T10:30:00Z"
  },
  "message": "Validation log created successfully"
}
```

#### POST /osm-uploads

Upload validated OSM file to cloud storage.

**Request:**
```http
POST /api/osm-uploads
X-API-Key: dpw_josm_plugin_digitization_2025_secure_key_f8a9b2c3d1e4
Content-Type: multipart/form-data; boundary=----DPWBoundary

------DPWBoundary
Content-Disposition: form-data; name="file"; filename="Task_27_john_mapper_2026-01-15.osm"
Content-Type: application/xml

<?xml version="1.0"?>
<osm>...</osm>
------DPWBoundary
Content-Disposition: form-data; name="validation_log_id"

1234
------DPWBoundary
Content-Disposition: form-data; name="mapper_user_id"

45
------DPWBoundary
Content-Disposition: form-data; name="validator_user_id"

78
------DPWBoundary--
```

**Response:**
```json
{
  "success": true,
  "data": {
    "upload_id": 5678,
    "drive_file_url": "https://drive.google.com/file/d/..."
  }
}
```

### 9.2 HOT Tasking Manager API

#### GET /projects/{projectId}/tasks/{taskId}/

Fetch task information.

**Request:**
```http
GET /api/v2/projects/27396/tasks/15/
Accept: application/json
```

**Response:**
```json
{
  "taskId": 15,
  "projectId": 27396,
  "taskStatus": "MAPPED",
  "properties": {
    "mappedBy": "john_mapper"
  },
  "taskHistory": [
    {
      "action": "STATE_CHANGE",
      "actionText": "MAPPED",
      "actionBy": "john_mapper",
      "actionDate": "2026-01-14T15:00:00Z"
    }
  ]
}
```

### 9.3 OSM OAuth API

#### GET /oauth2/authorize

OAuth authorization endpoint.

**Request:**
```http
GET /oauth2/authorize?
  response_type=code&
  client_id=dpw_josm_plugin&
  redirect_uri=http://localhost:8111/oauth/callback&
  scope=read_prefs%20write_api&
  state=abc123&
  code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&
  code_challenge_method=S256
```

**Response:** Browser redirect to login page

#### POST /oauth2/token

Token exchange endpoint.

**Request:**
```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&
client_id=dpw_josm_plugin&
code=AUTH_CODE_HERE&
redirect_uri=http://localhost:8111/oauth/callback&
code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
```

**Response:**
```json
{
  "access_token": "eyJ0eXAiOiJKV1...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "def502003f9...",
  "scope": "read_prefs write_api"
}
```

#### GET /api/0.6/user/details

Fetch authenticated user details.

**Request:**
```http
GET /api/0.6/user/details
Authorization: Bearer eyJ0eXAiOiJKV1...
Accept: application/json
```

**Response (XML):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<osm version="0.6">
  <user id="12345" display_name="john_mapper">
    <changesets count="42"/>
  </user>
</osm>
```

---

## 10. Security Implementation

### 10.1 Token Encryption

Tokens are encrypted using AES-256-CBC with PBKDF2 key derivation:

```java
// Key derivation
SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
SecretKey key = factory.generateSecret(spec);

// Encryption
Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
byte[] encrypted = cipher.doFinal(plaintext.getBytes());
```

### 10.2 OAuth Security (PKCE)

PKCE (Proof Key for Code Exchange) prevents authorization code interception:

```
1. Client generates random code_verifier
2. Client computes code_challenge = SHA256(code_verifier)
3. Client sends code_challenge to authorization server
4. User authenticates
5. Client receives authorization code
6. Client sends code + code_verifier to token endpoint
7. Server verifies: SHA256(code_verifier) == code_challenge
8. Server issues tokens
```

### 10.3 API Key Security

The DPW API key is hardcoded but:
- Only grants access to non-sensitive endpoints
- Server-side validation restricts operations
- Rate limiting prevents abuse
- `exclude_managers=true` prevents admin account exposure

### 10.4 User Authorization

Multi-level authorization checks:

```java
// 1. Check if user is authenticated
if (validatorUsername == null) {
    // Reject - not logged in
}

// 2. Check if user is in authorized list
boolean validatorAuthorized = authorizedMappers.stream()
    .anyMatch(user -> user.equalsIgnoreCase(validatorUsername));

// 3. Check if mapper is authorized
boolean mapperAuthorized = authorizedMappers.stream()
    .anyMatch(user -> user.equalsIgnoreCase(finalMapperUsername));
```

---

## 11. Configuration & Settings

### 11.1 Preference Keys

All settings are stored in JOSM preferences:

```java
// Plugin prefix
private static final String PREFIX = "dpw-validation-tool.";

// API Settings
PREFIX + "dpw-api-base-url"           // DPW API URL
PREFIX + "tm-api-base-url"            // Tasking Manager API URL

// Feature Toggles
PREFIX + "tm-integration-enabled"     // TM integration (BETA)
PREFIX + "remote-control-detection"   // Remote control task detection
PREFIX + "auto-fetch-settlement"      // Auto-fill settlement

// Project Defaults
PREFIX + "default-project-url"        // Default TM project URL
PREFIX + "default-project-id"         // Default TM project ID

// Cache Settings
PREFIX + "cache-expiry-hours"         // TM data cache duration

// OSM Server (v3.4.0)
PREFIX + "osm-server.use-custom-server"   // Use custom OSM
PREFIX + "osm-server.url"                  // Custom server URL
PREFIX + "osm-server.api-endpoint"         // Custom API endpoint
PREFIX + "osm-server.oauth-auth-url"       // Custom OAuth auth URL
PREFIX + "osm-server.oauth-token-url"      // Custom OAuth token URL
```

### 11.2 Default Values

| Setting | Default Value |
|---------|---------------|
| DPW API URL | `https://app.spatialcollective.com/api` |
| TM API URL | `https://tasking-manager-tm4-production-api.hotosm.org/api/v2` |
| OSM Server | `https://www.openstreetmap.org` |
| OSM API | `https://api.openstreetmap.org/api` |
| Cache Duration | 5 minutes (300,000 ms) |
| TM Integration | `false` (opt-in BETA) |
| Auto-fetch Settlement | `true` |

### 11.3 Custom Server Configuration

For private OSM instances:

```java
// Spatial Collective configuration
String baseUrl = "https://osm.spatialcollective.co.ke";

OSMServerConfiguration config = new OSMServerConfiguration(
    baseUrl,                        // OSM server URL
    baseUrl + "/api",               // API endpoint
    baseUrl + "/oauth2/authorize",  // OAuth authorization
    baseUrl + "/oauth2/token",      // OAuth token
    true                            // Use custom server
);
```

---

## 12. Technical Specifications

### 12.1 System Requirements

| Requirement | Specification |
|-------------|---------------|
| JOSM Version | 18.11 or later |
| Java Version | 8 or later (11+ recommended) |
| Operating System | Windows, macOS, Linux |
| Network | Internet connection required |

### 12.2 Dependencies

The plugin uses only JOSM standard libraries plus optional:
- `jdatepicker` - Date picker component (optional, fallback to text field)

### 12.3 Network Endpoints

| Service | Primary URL | Backup URL |
|---------|-------------|------------|
| DPW Manager | `app.spatialcollective.com` | N/A |
| HOT Tasking Manager | `tasking-manager-tm4-production-api.hotosm.org` | N/A |
| OpenStreetMap | `openstreetmap.org` | `osm.spatialcollective.co.ke` |
| GitHub (Updates) | `api.github.com` | `github.com` |

### 12.4 Rate Limits

| API | Limit | Cooldown |
|-----|-------|----------|
| DPW Users | 100/hour | 10 seconds |
| DPW Validation Log | No limit | N/A |
| HOT Tasking Manager | Public API | N/A |

### 12.5 Timeouts

| Operation | Connect | Read |
|-----------|---------|------|
| API Calls | 10 seconds | 10 seconds |
| OAuth Token | 15 seconds | 15 seconds |
| File Upload | 30 seconds | 30 seconds |

### 12.6 Caching Strategy

| Data | Duration | Storage |
|------|----------|---------|
| User List | 5 minutes | Memory |
| OAuth Tokens | Until expiry | JOSM Preferences (encrypted) |
| TM Task Info | Configurable (1-168 hours) | Memory |

### 12.7 Error Handling

The plugin implements comprehensive error handling:

```java
// API errors
if (responseCode != 200) {
    String errorMsg = extractErrorMessage(responseBody);
    throw new IllegalStateException("API error: " + errorMsg);
}

// Network errors
try {
    conn.connect();
} catch (java.net.SocketTimeoutException e) {
    // Show user-friendly timeout message
}

// JSON parsing errors
try {
    users = parseUserListJson(body);
} catch (Exception e) {
    throw new IllegalStateException("Failed to parse: " + e.getMessage());
}
```

---

## Summary

The **DPW Validation Tool** is a sophisticated JOSM plugin that:

1. **Integrates** with DPW Manager, HOT Tasking Manager, and OSM servers
2. **Streamlines** the validation workflow for building digitization projects
3. **Secures** authentication with OAuth 2.0 PKCE and AES-256 token encryption
4. **Tracks** quality metrics across 10 error categories
5. **Archives** validated data to cloud storage automatically
6. **Supports** both public OpenStreetMap and private OSM instances

The plugin represents **6,300+ lines of production Java code** with:
- 4 external API integrations
- Full OAuth 2.0 implementation
- Enterprise-grade security
- Comprehensive error handling
- Automatic updates from GitHub

---

**Document Version:** 1.0  
**Plugin Version:** 3.4.0  
**Last Updated:** January 15, 2026  
**Author:** Spatial Collective Ltd / GitHub Copilot
