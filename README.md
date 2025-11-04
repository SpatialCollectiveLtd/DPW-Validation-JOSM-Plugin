![Build](https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/actions/workflows/ci.yml/badge.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Release](https://img.shields.io/github/v/release/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin)

# DPW Validation Tool for JOSM

The DPW (Digital Public Works) Validation Tool is a specialized plugin for the Java OpenStreetMap Editor (JOSM). Developed for the 2025 Digital Public Works project, this tool streamlines the quality assurance and validation workflow for the Settlement Digitization module.

**Version 3.0.1** introduces cloud storage integration with automated Google Drive upload, along with the revolutionary streamlined workflow with automated export, validation preview panel, and enhanced confirmation dialogs.

It provides Final Validators with a secure, efficient, and integrated environment to assess the work of youth mappers, log quality metrics, and produce clean data deliverables — all directly inside JOSM with automatic cloud backup.

---

## Table of Contents

- [What's New in v3.0.1](#whats-new-in-v301)
- [What's New in v3.0](#whats-new-in-v30)
- [What's New in v2.1](#whats-new-in-v21)
- [Key Features](#key-features)
- [The Validation Workflow (Validator)](#the-validation-workflow-validator)
- [Authentication & Authorization](#authentication--authorization)
- [Migration from v2.1 to v3.0](#migration-from-v21-to-v30)
- [Installation (Interactive)](#installation-interactive)
- [Development & Build](#development--build)
- [Design Notes & UX](#design-notes--ux)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [Changelog (Selected)](#changelog-selected)
- [License & Ownership](#license--ownership)

---

## What's New in v3.0.1

### ☁️ Cloud Storage Integration
- **Automatic Google Drive upload**: Validated OSM files are automatically uploaded to project cloud storage
- **Seamless backup**: After local export, files are uploaded in the background with progress indication
- **Secure access**: Drive URLs are kept internal (company property), not displayed to validators
- **User tracking**: Mapper and validator IDs are automatically fetched and linked to uploads
- **Graceful fallback**: If cloud upload fails, local file is still saved successfully
- **API integration**: Fully integrated with DPW Manager API for centralized data management

### 🎯 Enhanced Upload Progress
- **Real-time status**: Progress dialog updates from "Exporting..." to "Uploading to cloud storage..."
- **Clear feedback**: Success messages confirm cloud backup without exposing internal Drive URLs
- **Error handling**: Informative messages if upload fails with local backup confirmation

---

## What's New in v3.0

### 🚀 Streamlined Automated Workflow
- **Automatic export prompts**: After accepting validation, you're automatically prompted to export
- **No manual export button needed**: Export is seamlessly integrated into the workflow
- **Smart session reset**: Option to clear all layers after export to prevent data duplication
- **State-based workflow**: Plugin tracks your progress and guides you through each step
- **Lightweight reset**: No need to restart JOSM - just clear layers and continue

### 📊 Validation Preview Panel
- **Collapsible summary panel**: See complete validation summary before submitting
- **Error breakdown display**: Visual breakdown of all error types and counts
- **Clear decision preview**: Review mapper, date, buildings, and comments at a glance
- **Professional formatting**: Easy-to-read summary with clear sections

### ✅ Enhanced Confirmation Dialogs
- **Rich context dialogs**: See exactly what you're submitting before confirming
- **Error summaries**: View total errors and breakdown in confirmation
- **Next-step preview**: Know what happens after you click Accept or Reject
- **Professional UX**: Clean, informative dialogs that reduce mistakes

### 🎯 Workflow Status Indicator
- **Visual progress tracking**: Status bar shows current step in the workflow
- **Color-coded states**: Different colors for each workflow stage
- **Clear next steps**: Always know what to do next
- **Real-time updates**: Status changes as you progress through validation

### 🛡️ Comprehensive Error Handling
- **Layer validation**: Checks if isolated layer exists before export
- **Graceful failures**: Clear error messages with recovery options
- **Retry mechanisms**: Option to retry failed operations
- **State consistency**: Plugin maintains consistent state even after errors

---

## What's New in v2.1

### 🔐 OAuth 2.0 Authentication
- **Automatic user detection**: Plugin now uses JOSM's OAuth 2.0 identity to automatically detect the validator
- **No more disconnection warnings**: Fixes JOSM disconnection errors by properly integrating with OAuth
- **Seamless authentication**: No manual username entry required

### 🛡️ Enhanced Security
- **Mandatory date selection**: Users must select a date before isolating work to ensure data integrity
- **Authorization checks**: Only authorized project members can isolate and validate data
- **Automatic authorization**: Validates current user against project registry before allowing operations

### ⚡ Simplified Workflow
- **Removed unnecessary buttons**: Eliminated "Scan Layers" and "Force Submit" buttons
- **Streamlined UI**: Clean, intuitive interface with Accept, Reject, Isolate, and Export buttons
- **Session management**: Option to reset and start new validation session after submission

### 🎯 Improved Data Validation
- **Robust JSON parsing**: Better handling of special characters and unicode in API responses
- **Input validation**: Comprehensive checks on field lengths and formats before submission
- **Better error messages**: Clear, actionable error messages with step-by-step instructions

---

## Key Features

- **Secure User Authentication**: Fetches the project's central user registry so only authorized validators and mappers are used for tasks.
- **Automated Work Isolation**: Copy a single mapper's contributions into a clean JOSM layer for unbiased review.
- **Integrated QA Panel**: Side panel to select mapper, log counts for 10 error classes with +/- controls, add validator comments, and mark Accept/Reject.
- **Direct Submission**: Send validation reports (error counts, metadata) directly to the project's Google Sheet endpoint.
- **One-Click Deliverable Export**: Export cleaned data with a project-compliant filename: `Task_<taskId>_<mapper>_<YYYY-MM-DD>.osm`.
- **Cloud Storage Integration**: Automatic backup to secure company cloud storage (internal use only) for centralized data management.

---

## The Validation Workflow (Validator)

**Note:** v3.0 introduces an automated, guided workflow with visual progress tracking.

### Prerequisites
1. **JOSM Authentication**: Be logged in to JOSM with your OSM account (OAuth 2.0)
2. **Project Authorization**: Your OSM username must be registered in the DPW Manager project registry
3. **API Access**: Valid connection to the DPW Manager API

### Workflow Steps

**1. Select Date & Mapper**
- **Status**: "▶ Current Step: Select Date & Mapper"
- Choose the date of the work you want to validate (REQUIRED)
- Select the mapper from the dropdown
- The settlement field auto-fills based on the mapper
- **Purpose**: Date selection ensures you only see work from that specific day

**2. Isolate Work**
- **Status**: Still "▶ Current Step: Select Date & Mapper"
- Click **"Isolate"** to fetch data for the selected date and mapper
- Plugin creates a temporary validation layer with only that mapper's work
- **Result**: Clean, isolated view of just the buildings to validate
- **Status changes to**: "▶ Current Step: Validate & Submit"
- **Validation Summary Panel appears**: Collapsible panel showing preview of validation

**3. Review & Validate**
- **Status**: "▶ Current Step: Validate & Submit"
- Review each building visually in JOSM
- Use JOSM's validation tools to check quality
- Count and categorize errors using the +/- buttons
- Enter comments about the validation
- **Validation Preview**: Expand summary panel to see complete breakdown

**4. Submit Decision**
- **Status**: Still "▶ Current Step: Validate & Submit"
- Click **"Accept"** if work meets quality standards
  - Enhanced confirmation dialog shows summary
  - Includes: mapper, date, buildings, error breakdown
  - Shows what will happen next
- Click **"Reject"** if major issues exist
  - Similar confirmation dialog with different context
  - Explains next steps for rejection
- **Status changes to**: "✓ Submitted - Exporting..."

**5. Export Data (Accept Only)**
- **Automatic prompt appears**: "Export validated layer now?"
- Options: **"📤 Export Now"** or **"Skip"**
- If you choose "Export Now":
  - File chooser opens with pre-filled filename: `Task_[ID]_[Mapper]_[Date].osm`
  - Progress dialog shows "Exporting validated layer..."
  - Only the isolated validation layer is exported
  - **Progress updates to**: "Uploading to cloud storage..."
  - File is automatically backed up to cloud (internal use only)
  - Success message shows:
    - Local file path
    - "✓ Backed up to cloud storage" (if successful)
  - If upload fails, local file is still saved
- **Status changes to**: "✓ Complete - Ready to Restart"

**6. Reset Session (After Export)**
- **Automatic prompt appears**: "Ready for Next Task"
- Shows confirmation: ✓ Validation submitted, ✓ Data exported, ☁️ Backed up to cloud
- Options: **"🔄 Reset Session"** or **"📝 Continue Working"**
- **Why reset?**: 
  - Clears all JOSM layers automatically
  - Resets the validation form
  - Prepares for next validation
  - Much faster than restarting JOSM
  - No need to wait for JOSM to restart
- If you choose "Reset Session":
  - All layers are removed automatically
  - Form is cleared and reset
  - Ready for next task immediately
- If you skip reset, option to reset form only

### For Rejected Validations

- No export prompt (rejected data is not exported)
- Simple prompt to start new validation session
- Form resets for next task

### Input Requirements

The plugin validates all inputs before submission:

- **Task ID**: Optional, max 100 characters
- **Settlement**: Optional, max 255 characters  
- **Mapper Username**: Required, max 255 characters (auto-filled via OAuth)
- **Comments**: Optional, max 1000 characters
- **Total Buildings**: Required, must be non-negative integer
- **Error Counts**: All must be non-negative integers
  - Duplicate Features
  - Missing Building Tags
  - Incomplete Attributes
  - Geometry Errors
  - Topological Issues
  - Tagging Inconsistencies
  - Unverified Sources
  - Outdated Data
  - Overlapping Features
  - Other Errors

---

## Authentication & Authorization

### OAuth 2.0 Integration

The plugin automatically detects your identity from JOSM's OAuth 2.0 authentication:

- **No manual login required**: Your OSM username is automatically retrieved
- **Secure**: Uses JOSM's built-in OAuth system
- **No disconnection warnings**: Properly integrates with JOSM's authentication layer

**Important**: You must be logged in to JOSM with your OSM account before using the plugin.

### Authorization Requirements

Before you can isolate and validate work:

1. Your OSM username must be registered in the DPW Manager project registry
2. The plugin automatically checks your authorization status
3. If not authorized, you'll see an error message with instructions

Contact your project administrator to be added as an authorized validator.

---

## Installation (Interactive)

Follow these steps to install the plugin locally in JOSM.

1. Download the latest release jar (example: `dist/DPWValidationTool.jar`).
2. Open JOSM and go to `Edit -> Preferences` (or press `F12`).
3. Open the `Plugins` tab.
4. Click **"Install from file..."**, select the downloaded JAR and confirm.
5. Restart JOSM.

Tip: to quickly install from the command line (Windows PowerShell), copy the jar into your JOSM plugins folder (example path shown below):

```powershell
$josmPlugins = "$env:USERPROFILE\\.josm\\plugins"
New-Item -ItemType Directory -Path $josmPlugins -Force | Out-Null
Copy-Item -Path .\\dist\\DPWValidationTool.jar -Destination $josmPlugins
```

After restart, open the plugin from the `Data` menu.

---

## Development & Build

Requirements:
- JDK 21
- Apache Ant

Quick build steps (PowerShell):

```powershell
# Set ANT_HOME if needed, then build
$env:ANT_HOME = 'C:\\Apache Ant\\apache-ant-1.10.15'
$env:PATH = $env:ANT_HOME + '\\bin;' + $env:PATH
ant -f build.xml clean dist
```

Result: `dist/DPWValidationTool.jar`

Note: The repository currently bundles the date picker dependency under `lib/` for convenience. For production-ready distribution you may prefer to use a build process that shades or downloads dependencies.

---

## Design Notes & UX

- The side panel uses a compact responsive layout: mapper combo expands, while date and isolate controls remain compact.
- The isolate functionality clones primitives to a new in-memory `DataSet` to avoid sharing primitives across layers and to create a clean review layer.
- The plugin attempts to use the JOSM search compiler for timestamp filtering; if timestamps are missing or the search cannot be parsed, the plugin falls back to mapper-only filtering.

---

## Troubleshooting

### "Not authenticated" or "OAuth error"
**Problem:** Plugin can't detect your OSM username.  
**Solution:** 
1. Open JOSM Preferences (F12)
2. Go to Connection Settings → OSM Server
3. Click "Authenticate" and log in with your OSM account
4. Restart JOSM and try again

### "User not authorized"
**Problem:** Your OSM username isn't registered in the project.  
**Solution:** 
1. Contact your project administrator
2. Provide your OSM username
3. Wait for authorization confirmation
4. Restart the plugin

### "Please select a date first"
**Problem:** Trying to isolate without selecting a date.  
**Solution:** 
1. Click the date picker in the plugin panel
2. Select the date of the work you want to validate
3. Then click "Isolate Mapper's Work"

### Validation input errors
**Problem:** Getting "field too long" or "invalid value" errors.  
**Solution:** Check these limits:
- Task ID: max 100 characters
- Settlement: max 255 characters
- Comments: max 1000 characters
- Total buildings: must be a non-negative number
- All error counts: must be non-negative numbers

### Plugin not appearing in Data menu
**Problem:** Plugin installed but not visible.  
**Solution:**
1. Check JOSM version (requires JOSM 18xxx or newer)
2. Verify plugin jar is in `%USERPROFILE%\.josm\plugins\`
3. Restart JOSM completely
4. Check for error messages in JOSM console

### Export button not working
**Problem:** Can't export validated data.  
**Solution:**
1. Only works after accepting work (not rejecting)
2. Ensure isolation layer is still loaded
3. Check you have write permissions to the target directory

---

## Contributing

Contributions and bug reports are welcome. Suggested workflow:

- Fork the repository
- Create a feature branch for your change
- Run the build and tests (if present)
- Open a pull request describing your change

Please avoid committing large binary dependencies to git in future patches; consider adding them to the release bundle only.

---

## Migration from v2.1 to v3.0

### Breaking Changes
- **"Export Validated Layer" button removed**: Export is now automatic after accepting validation
- **Workflow is now guided**: You can't skip steps or export manually
- **Session reset recommended**: After each validation cycle to prevent data duplication (no JOSM restart needed)

### New Behavior
- **Accept button**: Now triggers confirmation dialog → submission → export prompt → restart prompt
- **Reject button**: Now triggers confirmation dialog → submission → reset prompt
- **Export**: Automatically prompted after successful Accept submission
- **No manual export**: The standalone export button has been removed

### Benefits
- **Faster workflow**: Fewer clicks to complete validation
- **Fewer errors**: Guided workflow prevents skipping steps
- **Better data quality**: Session reset prevents layer confusion and duplication
- **Clearer process**: Visual indicators show exactly where you are
- **Lightweight**: No need to restart JOSM - just reset the session

### What Stays the Same
- OAuth 2.0 authentication
- Authorization checks
- Date-first isolation requirement
- Error counting and categorization
- All API integrations

---

## Changelog (Selected)

### Version 3.0.1 (2025-10-24)

**Cloud Storage Integration:**

- **Google Drive Upload**:
  - Automatic upload to cloud storage after local export
  - Uploads validated OSM files via DPW Manager API
  - Drive URLs kept internal (company property only)
  - Progress indication: "Exporting..." → "Uploading to cloud storage..."
  - Graceful fallback: local file saved even if upload fails

- **User ID Integration**:
  - Automatically fetches mapper and validator user IDs from API
  - Links uploaded files to specific users in database
  - Tracks uploader information for audit trail
  - Seamless integration with DPW Manager user registry

- **Enhanced Upload Workflow**:
  - Multipart form-data upload with proper encoding
  - Includes validation_log_id, task_id, settlement metadata
  - HTTP error handling with detailed logging
  - Success message confirms backup without exposing Drive URLs

- **API Integration**:
  - GET /api/users endpoint for user ID lookup by OSM username
  - POST /api/osm-uploads endpoint for file upload
  - Captures validation_log_id from submission response
  - Maintains backward compatibility with existing workflow

- **Bug Fix - Plugin Visibility**:
  - Fixed issue where plugin became unresponsive after session reset
  - Plugin now stays functional after clearing all layers
  - No need to restart JOSM between validations
  - Dialog visibility explicitly restored after layer removal
  - See PLUGIN_VISIBILITY_FIX.md for technical details

### Version 3.0.0 (2025-10-24)

**Major Workflow Overhaul:**

- **Automated Export Workflow**: 
  - Removed manual "Export Validated Layer" button
  - Export automatically prompted after successful Accept submission
  - Progress dialog shows export status
  - Only isolated validation layer is exported
  
- **JOSM Restart Integration**:
  - Changed to lightweight session reset (v3.0.1)
  - Automatically clears all JOSM layers after export
  - Resets validation form for next task
  - No need to restart JOSM (much faster)
  - Progress indicator for large number of layers
  - Graceful error handling with manual fallback

- **Validation Preview Panel**:
  - Collapsible summary panel added
  - Shows complete validation breakdown before submission
  - Includes mapper, date, buildings, error counts, comments
  - Professional formatting with clear sections
  - Expandable/collapsible to save screen space

- **Enhanced Confirmation Dialogs**:
  - Rich context before Accept/Reject actions
  - Shows summary of what will be submitted
  - Explains next steps after submission
  - Error breakdown in confirmation
  - Professional, informative UX

- **Workflow Status Indicator**:
  - Visual status bar at top of panel
  - Color-coded states (yellow → blue → green)
  - Shows current workflow step
  - Real-time updates as you progress

- **Validation State Machine**:
  - States: IDLE → ISOLATED → SUBMITTED → EXPORTED
  - Enforces proper workflow sequence
  - Prevents invalid operations
  - Maintains consistent plugin state

- **Comprehensive Error Handling**:
  - Layer existence validation before export
  - Graceful handling of closed layers
  - Retry mechanisms for failed operations
  - Clear error messages with recovery options
  - Restart failure fallback instructions

- **UI/UX Improvements**:
  - Cleaner button layout
  - Better visual hierarchy
  - More informative dialogs
  - Progress indicators for long operations
  - Consistent color scheme for states

### Version 2.1.0 (2025-10-23)

**Major Improvements:**

- **OAuth 2.0 Integration**: Automatic validator detection using JOSM's OAuth identity
  - Eliminates manual username entry
  - Fixes JOSM disconnection warnings
  - Seamless integration with JOSM authentication

- **API Integration Update**: Updated to match DPW Manager API v2.1 specification
  - Changed authorization endpoint to use `exclude_managers=true` for enhanced security
  - Prevents exposure of Manager/Admin accounts to plugin
  - Complies with new API security requirements

- **Enhanced Security & Validation**:
  - Mandatory date selection before isolation
  - Authorization checks before data access
  - Comprehensive input validation (field lengths, formats, required fields)
  - Robust JSON parsing with proper escape handling

- **Simplified User Interface**:
  - Removed "Scan Layers" button (direct isolation workflow)
  - Removed "Force Submit" button (proper validation enforced)
  - Cleaner button layout with Accept, Reject, Isolate, Export

- **Improved User Experience**:
  - Session reset functionality after successful submission
  - Better error messages with actionable instructions
  - Date-first workflow for better data integrity

- **Technical Improvements**:
  - Custom JSON parser replacing fragile regex patterns
  - Better handling of unicode and special characters
  - Validation for all 10 error count types
  - Fixed date filter + mapper filter combination

### Version 2.0.0 (2025-09-18)
- UI: responsive mapper/date/isolate row
- Mapper-specific building counts
- Layout polish

### Version 1.0.0 (2025-09-01)
- Initial plugin scaffold and basic validation workflow

---

## License & Ownership

This plugin is maintained and owned by Spatial Collective Ltd.
For licensing and commercial questions, contact the maintainers.

---

## Interactive Tips

- To quickly test the isolate flow, open a layer with edits, select a mapper, pick a date and click **Isolate**.
- Use the **Refresh Mapper List** button if the mapper doesn't appear or the registry changed.
- Exported filenames follow the `Task_<taskId>_<mapper>_<YYYY-MM-DD>.osm` convention.

---
