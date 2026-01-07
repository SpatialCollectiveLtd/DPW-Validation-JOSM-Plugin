# Release v3.3.0 - Stable Release

**Release Date:** January 7, 2026  
**Status:** ✅ **STABLE**  
**Base Version:** 3.2.9 (enhanced)

---

## 🎯 Overview

This is a **stable release** with significant UI improvements and critical bug fixes. v3.3.0 streamlines the validation workflow, fixes the session reset bug, and introduces intelligent auto-detection of HOT Tasking Manager project URLs.

---

## ✨ What's New

### 1. **Cleaner User Interface**
- ❌ Removed "Show Validation Summary" toggle button
- ❌ Removed TM Project URL field from main panel
- ✅ Streamlined, less cluttered interface
- ✅ Focus on core validation workflow

### 2. **Critical Bug Fix: Reset Session**
- 🐛 **Fixed:** Plugin becoming non-functional after session reset
- ✅ Panel now stays fully responsive after clearing layers
- ✅ Complete UI refresh ensures all controls work
- ✅ No more JOSM restarts needed!

### 3. **Smart Project URL Management**
- 🚀 **Auto-detects** project URL from HOT Tasking Manager remote control
- 💾 **Auto-saves** to settings for future sessions
- 🔄 **No manual entry** needed when switching projects
- 📍 Works with `#hotosm-project-XXXXX` changeset format

### 4. **Version Updates**
- ✅ All version numbers updated to 3.3.0
- ✅ Settings panel now shows correct version
- ✅ Consistent versioning across all components

---

## 🔧 Technical Changes

### File Modifications

**ValidationToolPanel.java:**
- Removed validation summary panel (lines ~430-475)
- Removed TM URL field from main panel (lines ~169-196)
- Enhanced `ensureDialogVisible()` with complete UI refresh
- Removed `handleTMUrlInput()` method (no longer needed)
- Enhanced `checkRemoteControlForTMTask()` with auto-save feature
- Removed `tmUrlField` declaration and references

**UpdateChecker.java:**
- Version updated: `3.2.9` → `3.3.0`

**SettingsPanel.java:**
- Version updated: `3.1.0-BETA` → `3.3.0`
- Header label updated to show v3.3.0

**build.xml:**
- Plugin version updated: `3.2.9` → `3.3.0`

---

## 🐛 Bugs Fixed

### Critical
- **Reset Session Bug** - Plugin panel no longer becomes non-functional after resetting session
  - Root cause: Insufficient UI refresh after layer removal
  - Solution: Complete invalidate/validate/repaint cycle with focus management

### Medium
- **Version Display** - Settings panel showed old version (3.1.0-BETA)
  - Fixed: Updated to 3.3.0 in both header and dialog title

---

## 🎨 UI/UX Improvements

### Removed
1. **Validation Summary Toggle Button**
   - Why: Cluttered interface, rarely used feature
   - Impact: Cleaner, more focused validation panel

2. **TM Project URL Field** (from main panel)
   - Why: Now auto-detected from remote control
   - Impact: One less field to manually enter
   - Note: Still available in settings for manual override

### Enhanced
1. **Session Reset Workflow**
   - More reliable layer clearing
   - Full UI refresh ensures responsiveness
   - Progress dialog for large layer counts
   - Better error handling

2. **Auto-Detection Notification**
   - Now shows "URL saved to settings"
   - Clearer feedback to users

---

## 🚀 New Features

### Auto-Save Project URL
When you load a task from HOT Tasking Manager via remote control:

**Before v3.3.0:**
```
User manually enters: https://tasks.hotosm.org/projects/38055
```

**With v3.3.0:**
```
1. User clicks "Edit in JOSM" in HOT TM
2. JOSM loads task via remote control
3. Plugin detects: #hotosm-project-38055-task-27
4. Plugin auto-saves: https://tasks.hotosm.org/projects/38055
5. Next session: URL already configured!
```

**Benefits:**
- ✅ No manual configuration needed
- ✅ Automatically switches when validating different projects
- ✅ Settings always reflect current project

---

## 📋 How to Use

### First Time Setup
1. Install v3.3.0
2. Go to **Tools → DPW Validation Tool Settings**
3. Enable "Tasking Manager Integration"
4. Enable "Auto-detect tasks from remote control"

### Daily Workflow
1. Open HOT Tasking Manager project
2. Click task → "Edit in JOSM"
3. Plugin **automatically detects** and saves project URL
4. Validate as normal
5. Submit validation
6. Reset session - **panel stays functional!** ✨

---

## 🧪 Testing Checklist

Before releasing to production, verify:

- [ ] Version shows 3.3.0 in all places
  - [ ] JOSM Plugins list
  - [ ] Settings panel header
  - [ ] Plugin panel title
  
- [ ] UI is clean
  - [ ] No "Show Summary" button
  - [ ] No TM URL field on main panel
  - [ ] TM URL field still in settings
  
- [ ] Reset session works
  - [ ] Submit a validation
  - [ ] Upload to cloud (if applicable)
  - [ ] Click "Reset Session"
  - [ ] Verify all layers cleared
  - [ ] **Verify panel is still responsive**
  - [ ] **Verify all controls work**
  - [ ] Enter new data and submit again
  
- [ ] Auto-detection works
  - [ ] Load task from HOT TM via remote control
  - [ ] Check logs for "Auto-saved project URL"
  - [ ] Open settings - verify URL saved
  - [ ] Load task from different project
  - [ ] Verify URL updated in settings

---

## 🔄 Migration from v3.2.9

**Automatic** - just install and restart JOSM.

**What Users Will Notice:**
1. Cleaner interface (less buttons/fields)
2. Reset session actually works now!
3. Project URL auto-saves from HOT TM

**Settings Migration:**
- All existing settings preserved
- New feature (auto-save URL) works alongside existing config

---

## 📝 Known Limitations

1. **Auto-detection only works with HOT Tasking Manager**
   - Format: `#hotosm-project-XXXXX-task-YYY`
   - Other TM instances may not be detected

2. **Manual URL override still available**
   - Users can manually set URL in settings if needed

3. **Java deprecation warnings** (non-critical)
   - URL(String) constructor deprecated in Java 21
   - Doesn't affect functionality
   - Will address in future release

---

## 🛠️ For Developers

### Building
```bash
ant clean
ant dist
```

### Installing
```bash
cp dist/DPWValidationTool.jar %APPDATA%\JOSM\plugins\
```

### Testing Reset Session
```java
// Key method: ensureDialogVisible()
// Now includes:
- invalidate() / validate()
- repaint()
- updatePanelData()
- updateSubmitButtonsEnabled()
- updateAuthStatus()
- requestFocus()
```

### Auto-Save Implementation
```java
// In checkRemoteControlForTMTask():
String detectedProjectUrl = "https://tasks.hotosm.org/projects/" + projectId;
PluginSettings.setDefaultProjectUrl(detectedProjectUrl);
```

---

## 📊 Version Comparison

| Feature | v3.0.3 | v3.2.9 | v3.3.0 |
|---------|---------|---------|---------|
| Endpoint | ❌ Old | ✅ New | ✅ New |
| Authentication | ❌ None | ✅ X-API-Key | ✅ X-API-Key |
| Reset Session Bug | 🐛 Broken | 🐛 Broken | ✅ Fixed |
| Show Summary Button | ✅ Yes | ✅ Yes | ❌ Removed |
| TM URL Field (main) | ❌ No | ✅ Yes | ❌ Removed |
| Auto-Save Project URL | ❌ No | ❌ No | ✅ Yes |
| Version Display | ✅ Correct | ⚠️ Mixed | ✅ Correct |

---

## 🎁 Bonus Features

### Settings Panel
Still includes all advanced configuration:
- ✅ DPW API Base URL
- ✅ Tasking Manager API URL  
- ✅ **Default Project URL** (now auto-updated!)
- ✅ TM Integration toggle
- ✅ Auto-fetch settlement toggle
- ✅ Remote control detection toggle
- ✅ Cache expiry settings

### Smart Defaults
- Default TM API: `tasking-manager-tm4-production-api.hotosm.org/api/v2`
- Default DPW API: `app.spatialcollective.com/api`
- Cache duration: Matches server recommendations (5 minutes)

---

## 🆘 Troubleshooting

### Panel Not Responsive After Reset
**This should be fixed in v3.3.0!** If you still experience this:
1. Check JOSM logs for errors during `ensureDialogVisible()`
2. Report the issue with full log context
3. Workaround: Restart JOSM

### Project URL Not Auto-Saving
1. Verify TM Integration enabled in settings
2. Verify Remote Control Detection enabled
3. Check logs for "Auto-saved project URL" message
4. Ensure changeset comment includes `#hotosm-project-XXXXX`

### Version Still Shows Old Number
1. Delete old JAR: `%APPDATA%\JOSM\plugins\DPWValidationTool.jar`
2. Install new v3.3.0 JAR
3. Restart JOSM completely (not just reload plugins)

---

## 📞 Support

**Issues:** https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/issues  
**Email:** tech@spatialcollective.com  
**Documentation:** See README.md and TESTING_v3.3.0.md

---

## 🙏 Acknowledgments

- HOT Tasking Manager team for the remote control integration
- DPW Manager backend team for API support
- Beta testers who reported the reset session bug

---

**Build Information:**
- Java Version: 21+
- JOSM API: 18823+
- Build Tool: Apache Ant
- Build Date: January 7, 2026
- Git Branch: `fix-from-v3.0.3` → `main`
- Git Tag: `v3.3.0`

---

**Upgrade Path:**
- From v3.0.x: Direct upgrade recommended
- From v3.2.x: Direct upgrade recommended
- From v3.1.x-BETA: Direct upgrade required

✨ **Enjoy the stable release!** ✨
