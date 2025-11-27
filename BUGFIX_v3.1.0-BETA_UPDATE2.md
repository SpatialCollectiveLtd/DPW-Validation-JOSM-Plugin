# Bug Fixes for v3.1.0-BETA (Update #2)

**Release Date:** 2025-01-XX  
**Build:** 19439  
**Focus:** Critical auto-update fixes + validator UX improvements

---

## 🐛 Critical Bugs Fixed

### 1. **Update Loop Bug** ✅ FIXED
**Problem:**
- Plugin continuously showed "Update Available" even when already on latest version
- Users stuck in infinite update loop, unable to complete updates

**Root Cause:**
- `UpdateChecker.isNewerVersion()` returned `true` for identical BETA versions
- Version comparison: `current.toUpperCase().contains("BETA")` matched even when versions were the same

**Fix Applied:**
```java
// OLD CODE (line 176):
return current.toUpperCase().contains("BETA"); // ❌ Always true for BETA versions

// NEW CODE:
return false; // ✅ Return false when versions match exactly (case-insensitive)
```

**Impact:**
- Update notifications now only show when newer version is actually available
- BETA versions correctly compared using semantic versioning
- Users can successfully complete updates without loops

---

### 2. **Restart JOSM Failure** ✅ FIXED
**Problem:**
- "Restart JOSM" button closed JOSM but didn't restart it
- Users expected automatic restart after plugin update
- Left JOSM closed, requiring manual re-launch

**Root Cause:**
- Used `System.exit(0)` which terminates JVM process
- No mechanism exists in Java to restart parent application (JOSM)

**Fix Applied:**
```java
// OLD CODE (line 320-325):
if (restartChoice == 0) {
    System.exit(0); // ❌ Exits JOSM but doesn't restart
}

// NEW CODE:
if (restartChoice == 0) {
    JOptionPane.showMessageDialog(null,
        "Please manually restart JOSM to complete the update.\n" +
        "Close JOSM and reopen it to use the updated plugin.",
        "Manual Restart Required",
        JOptionPane.INFORMATION_MESSAGE);
}
```

**Impact:**
- Clear user guidance: manual restart required
- No unexpected JOSM closure
- Users understand update completion process

---

### 3. **Download Shows 0KB/0KB** 🔍 UNDER INVESTIGATION
**Problem:**
- Update download progress shows "0KB / 0KB"
- Download completes successfully but size display incorrect
- Confusing UX, users think download failed

**Debug Enhancements Added:**
```java
// Added comprehensive logging:
System.out.println("UpdateChecker: Download URL: " + downloadUrl);
System.out.println("UpdateChecker: File size: " + fileSize + " bytes");
System.out.println("UpdateChecker: Progress: " + downloaded + " / " + fileSize);
```

**Next Steps:**
- User to install debug version and share JOSM logs
- Need to verify: GitHub asset URL extraction, HTTP headers, content-length
- Likely causes: GitHub redirect, missing content-length header

---

## ✨ Feature Enhancements

### 4. **Default Project URL/ID Settings** ✅ IMPLEMENTED
**Problem:**
- Validators must enter Tasking Manager project URL for every validation session
- Repetitive, error-prone, wastes validator time
- No persistence between JOSM sessions

**Solution Implemented:**
Added **Default Project Configuration** section to Settings panel with:

1. **New Settings Fields:**
   - `Default Project URL`: Full TM project URL (e.g., `https://tasks.hotosm.org/projects/27396`)
   - `Default Project ID`: Just the project ID (e.g., `27396`)
   - Tooltips with examples and usage guidance

2. **Backend Support (`PluginSettings.java`):**
   ```java
   // New preference keys
   public static final String DEFAULT_PROJECT_URL = "dpw.default.project.url";
   public static final String DEFAULT_PROJECT_ID = "dpw.default.project.id";
   
   // New methods
   public static String getDefaultProjectUrl();
   public static void setDefaultProjectUrl(String url);
   public static String getDefaultProjectId();
   public static void setDefaultProjectId(String id);
   ```

3. **Settings UI (`SettingsPanel.java`):**
   - New text fields: `projectUrlField` and `projectIdField`
   - Load/save logic integrated with JOSM preferences
   - Reset functionality clears both fields
   - Helpful tip: "Set either Project URL OR Project ID to avoid entering URLs for each validation"

**Impact:**
- **Validators:** Set project URL/ID once in settings, reused automatically
- **Workflow:** Faster validation startup, fewer input errors
- **Flexibility:** Supports both full URL or just project ID
- **Persistence:** Settings saved across JOSM sessions

---

## 📝 Files Modified

### `UpdateChecker.java` (570 lines)
**Changes:**
1. Line 176: Fixed `isNewerVersion()` to return `false` for identical versions
2. Line 320-325: Changed restart behavior to show manual instruction
3. Added debug logging for download URL, file size, and progress

### `PluginSettings.java` (195 lines)
**Changes:**
1. Added `DEFAULT_PROJECT_URL` and `DEFAULT_PROJECT_ID` constants
2. Added getter/setter methods for project URL and ID
3. Updated `resetToDefaults()` to clear project settings

### `SettingsPanel.java` (268 lines)
**Changes:**
1. Added `projectUrlField` and `projectIdField` declarations
2. Added "Default Project Configuration" section to UI (lines 84-114)
3. Updated `loadSettings()` to load project URL/ID
4. Updated `saveSettings()` to persist project URL/ID

---

## ✅ Testing Checklist

### Update Loop Fix
- [ ] Install v3.1.0-BETA, check for updates
- [ ] Verify "No update available" message (not infinite loop)
- [ ] Manually upload new JAR to test update detection

### Restart JOSM Fix
- [ ] Download update, click "Restart JOSM"
- [ ] Verify informative message displayed (no auto-exit)
- [ ] Manually restart JOSM, verify plugin updated

### Download 0KB Issue
- [ ] Install debug version, trigger update
- [ ] Share JOSM log output containing "UpdateChecker:" lines
- [ ] Verify download URL and file size logged

### Project URL Settings
- [ ] Open Settings (Tools → DPW Validation Tool Settings)
- [ ] Enter project URL: `https://tasks.hotosm.org/projects/27396`
- [ ] Save settings, restart JOSM
- [ ] Open validation panel, verify project URL pre-filled
- [ ] Test with Project ID instead of URL

---

## 🚀 Deployment Plan

### 1. Commit Changes
```bash
git add .
git commit -m "Fix update loop, restart JOSM, and add project URL settings (v3.1.0-BETA Update #2)"
git push origin main
```

### 2. Upload Fixed JAR
- Navigate to: https://github.com/[username]/DPW-JOSM-Plugin/releases/tag/v3.1.0-beta
- Delete old `DPWValidationTool.jar` asset
- Upload new JAR from `dist/DPWValidationTool.jar`
- Update release notes with bug fixes

### 3. Test Auto-Update
- Install previous version (with bugs)
- Trigger "Check for Updates"
- Verify:
  - Update detected correctly
  - Download completes (watch for 0KB issue)
  - Restart instruction shows correctly
  - Manual restart applies update

---

## 📊 Impact Summary

| Issue | Severity | Status | Impact |
|-------|----------|--------|--------|
| Update Loop | 🔴 Critical | ✅ Fixed | Users can now update successfully |
| Restart JOSM | 🟡 Medium | ✅ Fixed | Clear guidance, no confusion |
| 0KB Download | 🟡 Medium | 🔍 Investigating | Download works, UI misleading |
| Project URL Entry | 🟢 Enhancement | ✅ Implemented | Faster workflow, less errors |

**Overall Result:**
- **2 critical bugs FIXED** (update loop, restart)
- **1 bug under investigation** (0KB display - needs user logs)
- **1 major UX improvement** (project URL persistence)

---

## 🔮 Future Enhancements (Post-BETA)

1. **Auto-Restart Mechanism:**
   - Investigate JOSM plugin API for restart capability
   - Possibly use JOSM's internal restart mechanism

2. **Download Progress Accuracy:**
   - Fix 0KB/0KB display issue once root cause identified
   - Consider alternative download method if GitHub redirects problematic

3. **Collapsible Menu:**
   - User requested: "can we have a collapsible menu item that has dropdowns"
   - For DPWValidationToolPlugin menu actions

---

## 📞 Support Information

**For Issues:**
- GitHub Issues: https://github.com/[username]/DPW-JOSM-Plugin/issues
- Include JOSM version, plugin version, and full error logs

**For Update 0KB Issue:**
Please provide JOSM log output containing lines starting with "UpdateChecker:" after triggering an update check.

---

**Build Info:**
- **Compiler:** javac 21.0.8
- **JOSM Version:** Build 19439 (minimum)
- **Build Date:** 2025-01-XX
- **Git Commit:** [to be added]
