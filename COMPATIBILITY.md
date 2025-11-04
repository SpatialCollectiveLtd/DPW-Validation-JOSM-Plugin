# JOSM Plugin Compatibility Guide

## Overview

This document explains the DPW Validation Tool plugin's compatibility requirements and how to ensure it works with different JOSM versions.

---

## Current Version

**Plugin Version**: 3.0.1  
**Minimum JOSM Version**: 18823  
**Tested JOSM Version**: 19439  
**Release Date**: November 4, 2025

---

## JOSM Version Requirements

### Minimum Required Version

The plugin requires **JOSM build 18823 or higher**.

This corresponds to:
- JOSM released around **June 2024** or later
- Any JOSM version from mid-2024 onwards should work

### Recommended Version

For best compatibility and latest features:
- **JOSM 19000+** (September 2024 or later)
- Latest stable release from [josm.openstreetmap.de](https://josm.openstreetmap.de/)

---

## Checking Your JOSM Version

### Method 1: Help Menu
1. Open JOSM
2. Click **Help** → **About**
3. Check the version number (e.g., "19439")

### Method 2: Log File
1. Open JOSM
2. Click **Help** → **Show Log**
3. Look for line: `Revision: XXXXX`

### Method 3: Preferences
1. Open JOSM
2. Press **F12** (or Edit → Preferences)
3. Look at the window title showing version

---

## Compatibility Issues & Solutions

### Issue 1: "Missing plugin main version"

**Symptoms**:
```
WARNING: Missing plugin main version in plugin DPWValidationTool
```

**Cause**: Plugin manifest missing or incorrect `Plugin-Main-Version` field

**Fixed In**: v3.0.1 (November 4, 2025)

**Solution**:
1. Download latest plugin version (v3.0.1+)
2. Replace old JAR file in JOSM plugins directory
3. Restart JOSM

---

### Issue 2: Plugin Not Loading

**Symptoms**:
- Plugin not listed in Plugin Manager
- Menu item missing under Windows menu
- No errors shown

**Possible Causes & Solutions**:

#### Cause A: JOSM Too Old
- **Check**: Your JOSM version < 18823
- **Solution**: Update JOSM to build 18823 or higher
- **Download**: [josm.openstreetmap.de](https://josm.openstreetmap.de/)

#### Cause B: Wrong Plugin Location
- **Check**: Plugin JAR not in correct directory
- **Solution**: 
  - **Windows**: `%APPDATA%\JOSM\plugins\`
  - **Linux**: `~/.config/JOSM/plugins/`
  - **Mac**: `~/Library/JOSM/plugins/`

#### Cause C: Corrupted JAR
- **Check**: JAR file size is too small or damaged
- **Solution**: Re-download or rebuild plugin

---

### Issue 3: Plugin Loads But Crashes

**Symptoms**:
- Plugin appears in menu
- Crashes when clicked
- Error in log file

**Possible Causes & Solutions**:

#### Cause A: Missing Dependencies
- **Check**: JDatePicker JAR missing from lib/
- **Solution**: Ensure `lib/` folder contents are included in build

#### Cause B: Java Version Mismatch
- **Check**: JOSM running on Java < 21
- **Solution**: Update to Java 21 or higher

---

## Version History

### v3.0.1 (November 4, 2025)
- ✅ Fixed: Plugin-Main-Version now correctly set to JOSM build number (18823)
- ✅ Updated: Plugin version metadata to 3.0.1
- ✅ Updated: Plugin description with all features
- ✅ Fixed: Plugin visibility issue after session reset
- ✅ Added: Cloud storage integration

### v3.0.0 (October 24, 2025)
- Major workflow overhaul
- Automated export and session reset
- Validation preview panel
- Enhanced confirmations

### v2.1.0 (October 23, 2025)
- OAuth 2.0 integration
- API v2.1 compatibility
- Enhanced security

---

## Plugin Manifest Details

The plugin JAR includes these metadata fields:

```
Plugin-Class: org.openstreetmap.josm.plugins.dpwvalidationtool.DPWValidationToolPlugin
Plugin-Version: 3.0.1
Plugin-Name: DPW Validation Tool
Plugin-Main-Version: 18823
Plugin-Description: DPW Validation Tool v3.0.1 - Quality assurance and validation with OAuth 2.0, cloud storage integration, and session management
Plugin-Author: Spatial Collective Ltd
Plugin-Link: https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin
```

### Field Explanations

| Field | Value | Purpose |
|-------|-------|---------|
| `Plugin-Class` | Main plugin class | Entry point for JOSM to load plugin |
| `Plugin-Version` | `3.0.1` | Plugin's own version number |
| `Plugin-Name` | Display name | Name shown in JOSM menus |
| **`Plugin-Main-Version`** | **`18823`** | **Minimum JOSM build required** |
| `Plugin-Description` | Feature summary | Shown in Plugin Manager |
| `Plugin-Author` | Organization | Credits |
| `Plugin-Link` | GitHub URL | Source code location |

**Important**: `Plugin-Main-Version` must be a JOSM build number, NOT the plugin version!

---

## Testing Compatibility

### Quick Test
1. Install plugin JAR
2. Restart JOSM
3. Check for warning: "Missing plugin main version"
   - ✅ No warning = Compatible
   - ❌ Warning shown = Incompatible

### Full Test
1. Open JOSM
2. Go to **Edit** → **Preferences** → **Plugins**
3. Find "DPW Validation Tool" in list
4. Check status:
   - ✅ Green checkmark = Loaded successfully
   - ⚠️ Yellow warning = Compatibility issue
   - ❌ Red X = Failed to load

### Log Test
1. Open **Help** → **Show Log**
2. Search for "DPWValidationTool"
3. Look for messages:
   - ✅ `INFO: DPWValidationTool: constructing ValidationToolPanel v3.0.1`
   - ✅ `INFO: DPWValidationTool: ValidationToolPanel v3.0.1 constructed`
   - ❌ `ERROR:` or `WARNING:` messages indicate problems

---

## Building for Different JOSM Versions

If you need to support older JOSM versions:

### Step 1: Check Required APIs
Review which JOSM APIs your plugin uses:
- OAuth 2.0: Requires JOSM 18200+
- Layer Manager: Stable since JOSM 16000+
- Toggle Dialog: Stable since JOSM 15000+

### Step 2: Update Minimum Version
Edit `build.xml`:
```xml
<property name="plugin.main.version" value="18823"/>
```

Change to lower version if tested and compatible.

### Step 3: Test Thoroughly
- Test on minimum version
- Test on latest version
- Test on intermediate versions

### Step 4: Document Compatibility
Update this file with tested version range.

---

## Troubleshooting Commands

### Check Plugin Installation
**Windows**:
```powershell
dir %APPDATA%\JOSM\plugins\DPWValidationTool.jar
```

**Linux/Mac**:
```bash
ls ~/.config/JOSM/plugins/DPWValidationTool.jar
```

### Check Manifest in JAR
```bash
unzip -p DPWValidationTool.jar META-INF/MANIFEST.MF
```

### Validate JAR Contents
```bash
jar tf DPWValidationTool.jar | grep -E "(class|jar)$"
```

---

## Support

### If Plugin Won't Load

1. **Check JOSM Version**
   - Must be 18823 or higher
   - Update JOSM if needed

2. **Check Java Version**
   - JOSM Help → About → Java version
   - Must be Java 21 or higher

3. **Check Plugin Location**
   - Windows: `%APPDATA%\JOSM\plugins\`
   - Copy JAR to correct location

4. **Check Log File**
   - Help → Show Log
   - Look for errors mentioning DPWValidationTool

5. **Clean Reinstall**
   ```powershell
   # Remove old plugin
   del %APPDATA%\JOSM\plugins\DPWValidationTool.jar
   
   # Copy new plugin
   copy DPWValidationTool.jar %APPDATA%\JOSM\plugins\
   
   # Restart JOSM
   ```

---

## Developer Notes

### Why Plugin-Main-Version Matters

JOSM uses this field to:
1. Check if plugin is compatible with current JOSM version
2. Warn users if JOSM is too old
3. Prevent loading incompatible plugins
4. Show version info in Plugin Manager

### Common Mistakes

❌ **Wrong**: `Plugin-Main-Version: 3.0.1` (plugin version)  
✅ **Right**: `Plugin-Main-Version: 18823` (JOSM build number)

❌ **Wrong**: `Plugin-Main-Version: 1.5` (JOSM release number)  
✅ **Right**: `Plugin-Main-Version: 19439` (JOSM build number)

### Finding Minimum JOSM Version

1. Check what JOSM APIs you use
2. Look up when those APIs were introduced
3. Test on oldest JOSM version with those APIs
4. Set Plugin-Main-Version to that build number

---

## Version Matrix

| Plugin Version | Min JOSM | Tested JOSM | Java | Release Date |
|----------------|----------|-------------|------|--------------|
| 3.0.1 | 18823 | 19439 | 21+ | Nov 2025 |
| 3.0.0 | 18823 | 19200 | 21+ | Oct 2025 |
| 2.1.0 | 18200 | 18900 | 17+ | Oct 2025 |
| 2.0.0 | 18000 | 18500 | 17+ | Sep 2025 |

---

## FAQ

### Q: What does "Plugin-Main-Version: 18823" mean?
**A**: Your JOSM must be build 18823 or newer to use this plugin.

### Q: How do I find JOSM build numbers?
**A**: Go to Help → About in JOSM, or check [JOSM changelog](https://josm.openstreetmap.de/wiki/Changelog).

### Q: Can I use this plugin with JOSM 15000?
**A**: No, minimum is 18823. Update your JOSM.

### Q: Will this plugin work with future JOSM versions?
**A**: Yes, unless JOSM makes breaking API changes (rare).

### Q: How often should I update the plugin?
**A**: Check for updates monthly, or when you update JOSM.

---

## Summary

✅ **Plugin Version**: 3.0.1  
✅ **Minimum JOSM**: 18823  
✅ **Compatibility**: Fixed (November 4, 2025)  
✅ **Status**: Production-ready  

**The plugin is now fully compatible with JOSM and will no longer show "Missing plugin main version" warnings.**
