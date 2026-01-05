# DPW Validation Tool for JOSM

![Build](https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/actions/workflows/ci.yml/badge.svg)
![Release](https://img.shields.io/github/v/release/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin)
![License](https://img.shields.io/badge/license-Proprietary-red.svg)
![Java](https://img.shields.io/badge/Java-21-blue.svg)

**Current Version**: 3.0.6  
**Release Date**: January 5, 2026

A specialized JOSM plugin for streamlined quality assurance and validation workflow for the Digital Public Works Settlement Digitization project. Developed for validators to assess mapper work, log quality metrics, and produce clean data deliverables with integrated cloud backup.

---

## What's New in v3.0.6 🎉

### 🔒 Critical Security & Stability Improvements

- **Thread Safety Fixed**: Eliminated race conditions in mapper authorization cache
  - Made collections `final` with dedicated lock objects
  - Updated 7 synchronized blocks to use proper locking patterns
  - Prevents concurrent modification issues under load

- **Exception Handling Enhanced**: Added comprehensive logging to 14 empty catch blocks
  - All exceptions now logged at appropriate levels (debug/warn)
  - Improved debugging and troubleshooting capabilities
  - No more silent failures

- **Resource Leak Fixed**: HTTP connections now properly closed
  - Added try-finally blocks with guaranteed cleanup
  - Prevents connection pool exhaustion
  - Better memory management

- **Input Validation Hardened**: Protected all integer parsing operations
  - Wrapped 11 `parseInt()` calls with try-catch
  - Graceful handling of malformed server responses
  - No crashes on invalid input

### 🏗️ Code Quality Improvements

- **5 New Utility Classes** (2,190+ lines extracted):
  - `ValidationConstants`: Centralized configuration and constants
  - `DialogHelper`: Reusable UI dialog components
  - `InputValidator`: Robust input validation utilities
  - `DPWAPIClient`: Separated API communication layer
  - `ValidationModel`: Data model abstraction

- **Comprehensive Test Suite**: 48 unit tests with JUnit 5 + Mockito
- **Code Quality**: Improved from grade C+ to B+ (production-ready)
- **Refactored UI Setup**: Split 3,200-line method into 10 focused methods
- **100% Backward Compatible**: No breaking changes

---

## Features

It provides Final Validators with a secure, efficient, and integrated environment to assess the work of youth mappers, log quality metrics, and produce clean data deliverables — all directly inside JOSM with automatic cloud backup.

### Core Features

- **OAuth 2.0 Authentication** - Automatic validator detection using JOSM credentials
- **Automated Work Isolation** - Isolate specific mapper's work for validation
- **Quality Assurance Panel** - Track 10 error types with validation comments
- **Automated Export** - One-click export of validated data
- **Cloud Backup** - Automatic secure backup to project storage
- **Session Management** - Quick reset between validation tasks

---

## Table of Contents

- [What's New in v3.0.6](#whats-new-in-v306-)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [The Validation Workflow](#the-validation-workflow-validator)
- [Development & Build](#development--build)
- [Troubleshooting](#troubleshooting)
- [License](#license--ownership)

---

## Requirements

### 4. Validate- **Graceful failures**: Clear error messages with recovery options

- Count errors using +/- buttons- **Retry mechanisms**: Option to retry failed operations

- Add validation comments- **State consistency**: Plugin maintains consistent state even after errors

- Click **Accept** (if valid) or **Reject** (if invalid)

---

### 5. Export (for Accepted work)

- Choose where to save the file## What's New in v2.1

- File is automatically backed up to cloud

- Click **Reset Session** to start next validation### 🔐 OAuth 2.0 Authentication

- **Automatic user detection**: Plugin now uses JOSM's OAuth 2.0 identity to automatically detect the validator

---- **No more disconnection warnings**: Fixes JOSM disconnection errors by properly integrating with OAuth

- **Seamless authentication**: No manual username entry required

## Validation Workflow

### 🛡️ Enhanced Security

```- **Mandatory date selection**: Users must select a date before isolating work to ensure data integrity

Select Date & Mapper → Isolate Work → Review Quality → - **Authorization checks**: Only authorized project members can isolate and validate data

Submit Decision → Export Data → Reset Session → Repeat- **Automatic authorization**: Validates current user against project registry before allowing operations

```

### ⚡ Simplified Workflow

**No JOSM restarts needed** - the plugin handles session management automatically.- **Removed unnecessary buttons**: Eliminated "Scan Layers" and "Force Submit" buttons

- **Streamlined UI**: Clean, intuitive interface with Accept, Reject, Isolate, and Export buttons

---- **Session management**: Option to reset and start new validation session after submission



## Error Types Tracked### 🎯 Improved Data Validation

- **Robust JSON parsing**: Better handling of special characters and unicode in API responses

1. Hanging Nodes- **Input validation**: Comprehensive checks on field lengths and formats before submission

2. Overlapping Buildings- **Better error messages**: Clear, actionable error messages with step-by-step instructions

| Requirement | Minimum Version |
|------------|----------------|
| **JOSM** | Build 18823 (June 2024+) |
| **Java** | 21.0 or higher |
| **Internet** | Required for API access |
| **Operating System** | Windows 10+, Linux, macOS |

---

## Installation

### Step 1: Download Plugin

Download the latest `DPWValidationTool.jar` from the [Releases](https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/releases) page.

### Step 2: Install in JOSM

**Windows:**
```
Copy DPWValidationTool.jar to: %APPDATA%\JOSM\plugins\
```

**Linux:**
```
Copy DPWValidationTool.jar to: ~/.config/JOSM/plugins/
```

**macOS:**
```
Copy DPWValidationTool.jar to: ~/Library/JOSM/plugins/
```

### Step 3: Restart JOSM

Close and reopen JOSM for the plugin to load.

### Step 4: Open Plugin

Go to **Windows** → **DPW Validation Tool** to open the panel.

---

## Quick Start

### 1. Authenticate

- Make sure you're logged into JOSM (Edit → Preferences → Connection Settings)
- Your OSM account must be authorized in the DPW project

### 2. Select Task

- Choose validation date (required)
- Select mapper from dropdown
- Enter Task ID

### 3. Isolate Work

- Click **Isolate** to load mapper's work
- Review buildings in the isolated layer

### 4. Validate & Log

Track errors using the QA panel:
1. Buildings Touching Settlement
2. Buildings Crossing Administrative
3. Buildings Crossing Highway
4. Missing Tags
5. Improper Tags
6. Features Misidentified
7. Missing Buildings
8. Building Inside Building
9. Building Crossing Residential
10. Improperly Drawn

### 5. Submit & Export

- Click **Accept** or **Reject** to submit validation
- Export cleaned data when prompted
- Files automatically backed up to cloud storage

---

## The Validation Workflow (Validator)



**Note:** v3.0.6 introduces critical security and stability improvements with comprehensive test coverage.

### Prerequisites

1. **JOSM Authentication**: Be logged in to JOSM with your OSM account (OAuth 2.0)
2. **Project Authorization**: Your OSM username must be registered in the DPW Manager project registry
3. **API Access**: Valid connection to the DPW Manager API

### Workflow Steps

**1. Select Date & Mapper**
- Choose the date of the work you want to validate (REQUIRED)
- Select the mapper from the dropdown
- The settlement field auto-fills based on the mapper

**2. Isolate Work**
- Click **"Isolate"** to fetch data for the selected date and mapper
- Plugin creates a temporary validation layer with only that mapper's work
- Clean, isolated view of just the buildings to validate

**3. Review & Validate**
- Review each building visually in JOSM
- Use JOSM's validation tools to check quality
- Count and categorize errors using the +/- buttons
- Enter comments about the validation

**4. Submit Decision**
- Click **"Accept"** if work meets quality standards
- Click **"Reject"** if major issues exist
- Enhanced confirmation dialog shows complete summary

**5. Export Data (Accept Only)**
- File chooser opens with pre-filled filename: `Task_[ID]_[Mapper]_[Date].osm`
- Only the isolated validation layer is exported
- File is automatically backed up to cloud storage
- Success message confirms local and cloud backup

**6. Reset Session**
- Clear all JOSM layers automatically
- Reset the validation form
- Much faster than restarting JOSM

---

## Development & Build

### Prerequisites

- JDK 21 or higher
- Apache Ant 1.10+
- JOSM tested jar (for compilation)

### Build Commands

```bash
# Clean build outputs
ant clean

# Compile the plugin
ant compile

# Create distributable JAR
ant dist

# Run tests
ant test
```

### Project Structure

```
src/
├── org/openstreetmap/josm/plugins/dpwvalidationtool/
│   ├── DPWValidationToolPlugin.java    # Main plugin class
│   ├── ValidationToolPanel.java         # UI panel
│   ├── DPWAPIClient.java               # API communication
│   ├── DialogHelper.java               # Reusable dialogs
│   ├── InputValidator.java             # Input validation
│   ├── ValidationConstants.java        # Constants
│   ├── ValidationModel.java            # Data model
│   ├── IconResources.java              # Icon management
│   ├── PluginSettings.java             # Settings
│   ├── SettingsPanel.java              # Settings UI
│   ├── TaskManagerAPIClient.java       # TM integration
│   └── UpdateChecker.java              # Update checks

test/
└── org/openstreetmap/josm/plugins/dpwvalidationtool/
    ├── InputValidatorTest.java
    └── ValidationConstantsTest.java
```

---

## Troubleshooting

### Plugin Not Loading?

**Check JOSM Version:**
- Help → About
- Version must be 18823 or higher
- Update JOSM if needed: [josm.openstreetmap.de](https://josm.openstreetmap.de/)

**Check Plugin Location:**
- Windows: `%APPDATA%\JOSM\plugins\DPWValidationTool.jar`
- Linux: `~/.config/JOSM/plugins/DPWValidationTool.jar`
- macOS: `~/Library/JOSM/plugins/DPWValidationTool.jar`

**Check Logs:**
- Help → Show Log
- Search for "DPWValidationTool"
- Look for error messages

### Plugin Becomes Unresponsive?

Restart JOSM once. The plugin automatically maintains state during normal operation.

### Cannot Submit Validation?

- Ensure you're connected to the internet
- Check you're logged into JOSM with OAuth
- Verify your OSM account is authorized in the DPW project

---

## Support

For issues or questions:
- **GitHub Issues**: [Report a bug](https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/issues)
- **Documentation**: See this README

---

## Version History

### v3.0.6 (January 5, 2026)
- Critical security and stability improvements
- Fixed thread safety issues in mapper authorization cache
- Added comprehensive exception logging
- Fixed HTTP resource leaks
- Hardened input validation
- Extracted 5 utility classes (2,190+ lines)
- Added 48 unit tests with JUnit 5 + Mockito
- Code quality improved from C+ to B+
- 100% backward compatible

### v3.0.5 (Previous releases)
- Cloud storage integration
- Automated export workflow
- Session reset functionality
- Validation preview panel
- Enhanced confirmation dialogs

### v3.0.0 (October 24, 2025)
- OAuth 2.0 authentication
- Enhanced security features
- Improved user interface

---

## License & Ownership

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

**PROPRIETARY SOFTWARE - ALL RIGHTS RESERVED**

Copyright © 2025-2026 Spatial Collective Ltd. All Rights Reserved.

This software is proprietary and confidential. Unauthorized copying, distribution, modification, or use of this software, via any medium, is strictly prohibited without express written permission from Spatial Collective Ltd.

**This plugin is NOT open source.** While it interfaces with OpenStreetMap and JOSM (which are open source), the DPW Validation Tool plugin itself is private company property and is licensed only for internal use within the Digital Public Works project.

For licensing inquiries, contact: info@spatialcollective.com

See [LICENSE](LICENSE) file for complete terms and conditions.

---

**Developer**: Spatial Collective Ltd  
**Project**: Digital Public Works - Settlement Digitization  
**Repository**: https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin

---

**Ready to validate? Install the plugin and start improving OSM data quality!** 🗺️

