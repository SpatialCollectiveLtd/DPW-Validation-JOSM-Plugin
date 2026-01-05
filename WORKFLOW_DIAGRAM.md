# DPW Validation Tool - Complete Workflow Diagrams

## User Workflow - Visual Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         INITIAL SETUP                                │
│  (One-time or after updates)                                         │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │  1. Install Plugin             │
        │     - Download .jar            │
        │     - Place in plugins folder  │
        │     - Restart JOSM             │
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  2. Plugin Initialization      │
        │     - Check for updates        │
        │     - Apply pending updates    │
        │     - Fetch mapper list (API)  │
        │     - Detect validator (OAuth) │
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  3. Open Validation Panel      │
        │     Tools → DPW Validation     │
        │     Tool → Open Panel          │
        └────────────┬───────────────────┘
                     │
                     └──────────┐
                                │
                                ▼
┌───────────────────────────────────────────────────────────────────────┐
│                    VALIDATION WORKFLOW LOOP                           │
│                  (Repeat for each task)                               │
└───────────────────────┬───────────────────────────────────────────────┘
                        │
        ┌───────────────┴────────────────┐
        │                                │
        ▼                                ▼
┌──────────────────┐          ┌──────────────────────┐
│  MANUAL ENTRY    │          │  TM INTEGRATION      │
│                  │          │  (BETA - Optional)   │
├──────────────────┤          ├──────────────────────┤
│ 1. Task ID       │          │ 1. Enter TM URL      │
│ 2. Settlement    │          │ 2. Click remote ctrl │
│ 3. Pick Mapper   │          │ 3. Load OSM data     │
│ 4. Date          │          │ 4. Auto-detect:      │
│                  │          │    - Task ID         │
│                  │          │    - Mapper          │
│                  │          │    - Settlement      │
└────────┬─────────┘          └──────────┬───────────┘
         │                               │
         └───────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  4. Click "Isolate Mapper      │
        │     Work"                      │
        │                                │
        │  Background Process:           │
        │  • Search user:"mapper"        │
        │  • Create new layer            │
        │  • Copy objects                │
        │  • Set as active               │
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  5. Review Isolated Data       │
        │                                │
        │  In JOSM Editor:               │
        │  • Visual inspection           │
        │  • Check buildings             │
        │  • Identify errors             │
        │  • Take notes                  │
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  6. Track Errors               │
        │                                │
        │  Click +/- for each type:      │
        │  ☐ Hanging Nodes               │
        │  ☐ Overlapping Buildings       │
        │  ☐ Buildings Cross Highway     │
        │  ☐ Missing Tags                │
        │  ☐ Improper Tags               │
        │  ☐ Features Misidentified      │
        │  ☐ Missing Buildings           │
        │  ☐ Building Inside Building    │
        │  ☐ Building Cross Residential  │
        │  ☐ Improperly Drawn            │
        │                                │
        │  Enter:                        │
        │  • Total buildings count       │
        │  • Validation comments         │
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  7. Toggle Validation Preview  │
        │                                │
        │  Review Summary:               │
        │  Mapper: john_mapper           │
        │  Task: 27                      │
        │  Total Buildings: 150          │
        │  Total Errors: 12              │
        │  Error Breakdown:              │
        │    Hanging Nodes: 5            │
        │    Overlapping: 3              │
        │    Missing Tags: 4             │
        │  Comments: Good work overall   │
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  8. Make Decision              │
        └────────┬───────────┬───────────┘
                 │           │
         ┌───────┴───┐   ┌───┴───────┐
         │           │   │           │
         ▼           │   │           ▼
    ┌────────┐      │   │      ┌─────────┐
    │   ✅   │      │   │      │    ❌   │
    │ VALID  │      │   │      │ REJECT  │
    └───┬────┘      │   │      └────┬────┘
        │           │   │           │
        └───────────┴───┴───────────┘
                    │
                    ▼
        ┌────────────────────────────────┐
        │  9. Confirm Submission         │
        │                                │
        │  Dialog shows:                 │
        │  • All details                 │
        │  • Error summary               │
        │  • Validation status           │
        │                                │
        │  [Cancel] [Confirm]            │
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  10. Submit to API             │
        │                                │
        │  POST /api/validation-logs/    │
        │  • Send JSON data              │
        │  • Show progress dialog        │
        │  • Wait for response           │
        │  • Get validation_log_id       │
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  11. Export Data               │
        │                                │
        │  File Chooser:                 │
        │  • Suggest filename            │
        │  • Select location             │
        │  • Save .osm file              │
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  12. Cloud Backup (v3.0.1)     │
        │                                │
        │  Background Upload:            │
        │  • POST file to API            │
        │  • Upload to Google Drive      │
        │  • Show progress               │
        │  • Success confirmation        │
        │                                │
        │  Fallback: Local copy saved    │
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  13. Start New Validation      │
        │                                │
        │  Options:                      │
        │  • Clear all layers            │
        │  • Keep layers for reference   │
        │                                │
        │  Reset form fields             │
        └────────────┬───────────────────┘
                     │
                     └──► BACK TO TOP (Repeat)
```

---

## Data Flow Diagram

```
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│   JOSM UI    │◄───────►│    Plugin    │◄───────►│   DPW API    │
│   (User)     │         │              │         │   (Server)   │
└──────────────┘         └──────┬───────┘         └──────────────┘
                                │
                                │
                    ┌───────────┼───────────┐
                    │           │           │
                    ▼           ▼           ▼
            ┌──────────┐  ┌──────────┐  ┌──────────┐
            │   TM     │  │  Google  │  │  Local   │
            │   API    │  │  Drive   │  │  File    │
            │ (HOT)    │  │  (Cloud) │  │  System  │
            └──────────┘  └──────────┘  └──────────┘
```

### Detailed Data Flow

```
User Action: "Isolate Mapper Work"
│
├──► Plugin searches JOSM DataSet
│    └──► user:"mapper_username"
│
├──► Creates new OsmDataLayer
│    └──► "Isolated: mapper_username"
│
├──► Copies OsmPrimitives to new layer
│    ├──► Nodes
│    ├──► Ways
│    └──► Relations
│
└──► Updates UI state
     └──► IDLE → ISOLATED

─────────────────────────────────────────

User Action: "✅ VALIDATED"
│
├──► Validates form inputs
│    ├──► Task ID: not empty
│    ├──► Mapper: selected
│    ├──► Total buildings: > 0
│    └──► Error counts: valid
│
├──► Shows confirmation dialog
│    └──► User confirms
│
├──► Builds JSON payload
│    {
│      "task_id": "27",
│      "mapper_osm_username": "john",
│      "validator_osm_username": "jane",
│      "total_buildings": 150,
│      "validation_status": "Validated",
│      "validation_date": "2026-01-05",
│      "hanging_nodes": 5,
│      ... (all error types)
│      "comments": "Good work"
│    }
│
├──► HTTP POST to DPW API
│    └──► /api/validation-logs/
│         ├──► Headers:
│         │    └──► Authorization: Bearer <API_KEY>
│         └──► Body: JSON
│
├──► API Response
│    {
│      "success": true,
│      "validation_log_id": 123,
│      "mapper_user_id": 45,
│      "validator_user_id": 67
│    }
│
├──► Export to OSM file
│    ├──► Show file chooser
│    ├──► User selects location
│    ├──► Write OSM XML
│    └──► validated_john_27.osm
│
├──► Cloud Upload (v3.0.1)
│    ├──► POST /api/validation-logs/123/upload-file/
│    ├──► multipart/form-data
│    ├──► API uploads to Google Drive
│    └──► Returns drive_url (internal only)
│
└──► Update UI state
     └──► ISOLATED → SUBMITTED → EXPORTED
```

---

## State Machine Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    VALIDATION STATE MACHINE                  │
└─────────────────────────────────────────────────────────────┘

        ┌──────────────────┐
        │                  │
        │   INITIAL IDLE   │◄────────────────────┐
        │                  │                     │
        └────────┬─────────┘                     │
                 │                               │
                 │ User clicks                   │
                 │ "Isolate Mapper Work"         │
                 │                               │
                 ▼                               │
        ┌──────────────────┐                     │
        │                  │                     │
        │    ISOLATED      │                     │
        │  (Layer Created) │                     │
        │                  │                     │
        └────────┬─────────┘                     │
                 │                               │
                 │ User clicks                   │
                 │ ✅ VALIDATED or ❌ REJECTED   │
                 │                               │
                 ▼                               │
        ┌──────────────────┐                     │
        │                  │                     │
        │    SUBMITTED     │                     │
        │ (API Success)    │                     │
        │                  │                     │
        └────────┬─────────┘                     │
                 │                               │
                 │ User exports                  │
                 │ validated data                │
                 │                               │
                 ▼                               │
        ┌──────────────────┐                     │
        │                  │                     │
        │    EXPORTED      │                     │
        │ (File Saved +    │                     │
        │  Cloud Backup)   │                     │
        │                  │                     │
        └────────┬─────────┘                     │
                 │                               │
                 │ User clicks                   │
                 │ "Start New Validation"        │
                 │                               │
                 └───────────────────────────────┘


Valid Transitions:
  IDLE → ISOLATED       ✅ (Isolate mapper work)
  ISOLATED → SUBMITTED  ✅ (Validate/Reject)
  SUBMITTED → EXPORTED  ✅ (Export data)
  EXPORTED → IDLE       ✅ (Start new validation)

Invalid Transitions (Blocked):
  IDLE → SUBMITTED      ❌ (Must isolate first)
  IDLE → EXPORTED       ❌ (Must complete workflow)
  ISOLATED → EXPORTED   ❌ (Must submit first)
  SUBMITTED → IDLE      ❌ (Must export first)
```

---

## Settings Configuration Flow

```
┌─────────────────────────────────────────────────────────────┐
│              SETTINGS PANEL WORKFLOW                         │
└─────────────────────────────────────────────────────────────┘

Tools → DPW Validation Tool → Settings
│
├──► Open Settings Dialog
│    └──► Load current values from JOSM preferences
│
├──► User modifies settings
│    │
│    ├──► API Configuration
│    │    ├── DPW API URL
│    │    └── TM API URL
│    │
│    ├──► Default Project Config
│    │    ├── Project URL (e.g., tasks.hotosm.org/projects/27396)
│    │    └── OR Project ID (e.g., 27396)
│    │
│    ├──► Feature Toggles
│    │    ├── ☑ Enable TM Integration
│    │    ├── ☑ Auto-fetch settlement
│    │    └── ☑ Remote control detection
│    │
│    └──► Cache Settings
│         └── Cache expiry (hours)
│
├──► User clicks action button
│    │
│    ├──► [Save]
│    │    ├── Validate inputs
│    │    ├── Write to JOSM preferences:
│    │    │    • Config.getPref().put(key, value)
│    │    │    • Config.getPref().putBoolean(key, value)
│    │    │    • Config.getPref().putInt(key, value)
│    │    ├── Show success message
│    │    └── Close dialog
│    │
│    ├──► [Reset to Defaults]
│    │    ├── Confirmation dialog
│    │    ├── PluginSettings.resetToDefaults()
│    │    ├── Reload UI with default values
│    │    └── Show reset complete message
│    │
│    ├──► [Check for Updates]
│    │    ├── UpdateChecker.checkForUpdatesAsync(true)
│    │    ├── Show progress
│    │    └── Display update dialog if available
│    │
│    └──► [Cancel]
│         └── Discard changes, close dialog
│
└──► Settings Applied
     └──► Plugin uses new configuration values
```

---

## Auto-Update Workflow

```
┌─────────────────────────────────────────────────────────────┐
│                 AUTO-UPDATE WORKFLOW                         │
└─────────────────────────────────────────────────────────────┘

SCENARIO 1: Startup with Pending Update
│
├──► JOSM starts
│
├──► DPWValidationToolPlugin constructor
│    └──► UpdateChecker.applyPendingUpdate()
│         │
│         ├──► Check for: DPWValidationTool.jar.new
│         │
│         ├──► If exists:
│         │    ├── Backup: .jar → .jar.bak
│         │    ├── Install: .jar.new → .jar
│         │    ├── Delete: .jar.new
│         │    ├── Show notification:
│         │    │    "✅ Update installed successfully!"
│         │    └── Delete: .jar.bak (cleanup)
│         │
│         └──► Continue plugin initialization
│
└──► Plugin loads with NEW version

─────────────────────────────────────────────────────────────────

SCENARIO 2: Background Update Check (Silent)
│
├──► Plugin initialization complete
│
├──► UpdateChecker.checkForUpdatesAsync(silent=true)
│    └──► Background thread
│         │
│         ├──► HTTP GET to GitHub API
│         │    └──► /repos/.../releases
│         │
│         ├──► Parse JSON response
│         │    └──► Extract latest version
│         │
│         ├──► Compare versions
│         │    ├── Current: 3.0.5
│         │    └── Latest: 3.1.0
│         │
│         ├──► If newer version available:
│         │    └──► Show notification toast:
│         │         "🎉 Update available: v3.1.0"
│         │         [Install Now] [Later]
│         │
│         └──► If silent=true and no update:
│              └──► No notification (silent)
│
└──► User can continue working

─────────────────────────────────────────────────────────────────

SCENARIO 3: Manual Update Check
│
├──► Tools → DPW Validation Tool → Check for Updates
│    OR Settings → [Check for Updates] button
│
├──► UpdateChecker.checkForUpdatesAsync(silent=false)
│    │
│    ├──► Show progress dialog:
│    │    "Checking for updates..."
│    │
│    ├──► HTTP GET to GitHub API
│    │
│    ├──► If update available:
│    │    │
│    │    └──► Show update dialog:
│    │         ┌─────────────────────────────────┐
│    │         │ 🎉 Update Available!            │
│    │         ├─────────────────────────────────┤
│    │         │ Current: 3.0.5                  │
│    │         │ Latest: 3.1.0                   │
│    │         │                                 │
│    │         │ Release Notes:                  │
│    │         │ • New TM integration            │
│    │         │ • Bug fixes                     │
│    │         │ • Performance improvements      │
│    │         │                                 │
│    │         │ [Install Update] [View GitHub]  │
│    │         │              [Later]            │
│    │         └─────────────────────────────────┘
│    │
│    └──► If no update:
│         └──► Show dialog:
│              "✅ You're up to date! (v3.0.5)"
│
├──► User clicks [Install Update]
│    │
│    ├──► Download .jar from GitHub
│    │    ├── Show progress bar
│    │    ├── URL: release.assets[0].browser_download_url
│    │    └── Save as: DPWValidationTool.jar.new
│    │
│    ├──► Download complete
│    │    └──► Success dialog:
│    │         "✅ Update downloaded successfully!
│    │          Restart JOSM to apply the update."
│    │
│    └──► User restarts JOSM
│         └──► SCENARIO 1 triggers (apply pending update)
│
└──► Updated plugin now active!
```

---

## TM Integration Workflow (BETA)

```
┌─────────────────────────────────────────────────────────────┐
│           TASKING MANAGER INTEGRATION (v3.1.0-BETA)         │
└─────────────────────────────────────────────────────────────┘

PREREQUISITE: Settings → ☑ Enable TM Integration

METHOD 1: Manual TM URL Entry
│
├──► User enters TM Project URL
│    └──► "https://tasks.hotosm.org/projects/27396"
│
├──► Plugin parses URL
│    └──► TaskManagerAPIClient.parseTaskManagerURL()
│         └──► Extract: projectId = 27396
│
├──► User loads OSM data via remote control
│
├──► Plugin detects remote control activity
│    └──► Active layer change listener triggers
│
├──► Parse changeset comment
│    └──► "#hotosm-project-27396-task-27"
│         └──► Extract: taskId = 27
│
├──► Fetch task info from TM API
│    └──► GET /api/v2/projects/27396/tasks/27/
│         │
│         ├──► Parse JSON response
│         └──► Extract:
│              ├── mapperUsername: "john_mapper"
│              └── taskStatus: "MAPPED"
│
├──► Auto-populate form
│    ├── Task ID: "27" ✅
│    ├── Mapper: "john_mapper" ✅
│    └── Settlement: (auto-fetch from DPW API)
│
└──► User proceeds with validation workflow

─────────────────────────────────────────────────────────────────

METHOD 2: Remote Control Detection (Auto)
│
├──► User working in HOT Tasking Manager
│
├──► User clicks "Edit in JOSM" in TM
│
├──► TM sends remote control command
│    └──► http://localhost:8111/import?
│         new_layer=true&
│         layer_name=Task%2027396%2327&
│         changeset_comment=#hotosm-project-27396-task-27
│         &changeset_source=Bing
│
├──► JOSM loads data (new layer created)
│
├──► Plugin detects layer change
│    └──► LayerChangeListener.activeLayerChange()
│         │
│         ├──► Check layer name: "Task 27396#27" ✅
│         │
│         ├──► Get changeset comment from layer
│         │    └──► "#hotosm-project-27396-task-27"
│         │
│         ├──► Parse comment
│         │    └──► Extract: project=27396, task=27
│         │
│         ├──► Fetch from TM API
│         │    └──► GET /projects/27396/tasks/27/
│         │         └──► Get mapper username
│         │
│         └──► Auto-populate form fields
│              ├── Task ID: "27" ✅
│              ├── Mapper: "john_mapper" ✅
│              └── Trigger settlement auto-fetch
│
└──► User can immediately start validation
     (No manual data entry needed!)

─────────────────────────────────────────────────────────────────

ERROR HANDLING
│
├──► Invalid TM URL
│    └──► Show error: "Invalid TM URL format"
│
├──► TM API unreachable
│    └──► Show error: "Cannot connect to TM API"
│         Fallback: Manual entry still available
│
├──► Task not found
│    └──► Show error: "Task not found on TM"
│
├──► No mapper assigned
│    └──► Show warning: "Task not mapped yet"
│         User can select mapper manually
│
└──► Rate limiting (429)
     └──► Cache response for 10 minutes
          Retry with exponential backoff
```

---

## API Integration Flow

```
┌─────────────────────────────────────────────────────────────┐
│                DPW MANAGER API INTEGRATION                   │
└─────────────────────────────────────────────────────────────┘

ENDPOINT 1: Fetch Authorized Mappers
│
├──► Trigger: Plugin startup, Refresh button
│
├──► HTTP GET /api/users/
│    Headers:
│      Authorization: Bearer <API_KEY>
│      Accept: application/json
│
├──► Response (200 OK):
│    {
│      "users": [
│        {
│          "osm_username": "john_mapper",
│          "settlement": "Settlement A"
│        },
│        {
│          "osm_username": "jane_mapper",
│          "settlement": "Settlement B"
│        }
│      ]
│    }
│
├──► Cache for 5 minutes
│    └──► Config: CACHE_DURATION = 300000ms
│
└──► Populate mapper dropdown
     └──► Show settlement in tooltip

─────────────────────────────────────────────────────────────────

ENDPOINT 2: Submit Validation Data
│
├──► Trigger: User clicks ✅ VALIDATED or ❌ REJECTED
│
├──► HTTP POST /api/validation-logs/
│    Headers:
│      Authorization: Bearer <API_KEY>
│      Content-Type: application/json
│    Body:
│    {
│      "task_id": "27",
│      "settlement": "Settlement A",
│      "mapper_osm_username": "john_mapper",
│      "validator_osm_username": "jane_validator",
│      "total_buildings": 150,
│      "validation_status": "Validated",
│      "validation_date": "2026-01-05",
│      "hanging_nodes": 5,
│      "overlapping_buildings": 3,
│      "buildings_crossing_highway": 0,
│      "missing_tags": 4,
│      "improper_tags": 0,
│      "features_misidentified": 0,
│      "missing_buildings": 0,
│      "building_inside_building": 0,
│      "building_crossing_residential": 0,
│      "improperly_drawn": 0,
│      "comments": "Good work overall"
│    }
│
├──► Response (201 Created):
│    {
│      "success": true,
│      "message": "Validation log created successfully",
│      "validation_log_id": 123,
│      "mapper_user_id": 45,
│      "validator_user_id": 67
│    }
│
└──► Store for cloud upload
     ├── validation_log_id: 123
     ├── mapper_user_id: 45
     └── validator_user_id: 67

─────────────────────────────────────────────────────────────────

ENDPOINT 3: Cloud Upload (v3.0.1)
│
├──► Trigger: After local OSM file export
│
├──► HTTP POST /api/validation-logs/{id}/upload-file/
│    Headers:
│      Authorization: Bearer <API_KEY>
│    Content-Type: multipart/form-data
│    Body:
│      file: <validated_john_27.osm>
│
├──► Server Process:
│    ├── Receive file
│    ├── Validate file format
│    ├── Upload to Google Drive
│    │    └──► drive_url: "https://drive.google.com/..."
│    └── Link to validation_log_id
│
├──► Response (200 OK):
│    {
│      "success": true,
│      "message": "File uploaded successfully",
│      "drive_url": "https://drive.google.com/..." (internal)
│    }
│
└──► Show success notification
     └──► "✅ Data saved and backed up to cloud"

─────────────────────────────────────────────────────────────────

ERROR HANDLING
│
├──► 401 Unauthorized
│    └──► Show error: "Invalid API key"
│         Action: Contact admin
│
├──► 403 Forbidden
│    └──► Show error: "You're not authorized as validator"
│         Action: Request validator access
│
├──► 429 Too Many Requests
│    └──► Show warning: "Rate limit exceeded"
│         Action: Wait 10 seconds, retry
│
├──► 500 Internal Server Error
│    └──► Show error: "Server error, please try again"
│         Action: Retry after delay
│
└──► Network Error
     └──► Show error: "Cannot connect to server"
          Action: Check internet connection
```

---

**End of Workflow Diagrams**  
For implementation details, see COMPREHENSIVE_ANALYSIS_REPORT.md
