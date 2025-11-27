# DPW Validation Tool v3.1.0-BETA Release Notes

**Release Date:** TBD  
**Type:** BETA Release  
**Previous Version:** v3.0.1

---

## 🚀 Major New Features

### HOT Tasking Manager Integration (BETA)

This BETA release introduces optional integration with the HOT Tasking Manager API, enabling a seamless validation workflow for validators working with TM tasks.

#### Key Capabilities:

1. **Automatic Mapper Detection**
   - Paste a Tasking Manager URL to automatically detect the mapper
   - Supported URL formats:
     - `https://tasks.hotosm.org/projects/27396/tasks/123`
     - `tasks.hotosm.org/projects/27396#task/123`
   - Automatically fetches mapper username from task history
   - Auto-populates settlement data when available

2. **Remote Control Workflow Detection** ⭐
   - Automatically detects TM tasks opened via JOSM remote control
   - Parses changeset comments for `#hotosm-project-XXXXX-task-YYY` format
   - Auto-fills Task ID and Mapper fields when layer changes
   - **Perfect for validators**: Start validation from TM → JOSM auto-populates all fields

3. **Plugin Settings Panel**
   - New "DPW Validation Settings..." menu in Tools menu
   - Configure all plugin settings without rebuilding
   - Settings include:
     - Enable/disable TM integration (default: OFF)
     - DPW API base URL configuration
     - TM API base URL configuration
     - Auto-fetch settlement toggle
     - Remote control detection toggle
     - Cache expiry settings
     - Reset to defaults option

---

## 🔧 Technical Improvements

- **Configuration Management**: All settings now stored in JOSM preferences
- **Non-blocking API Calls**: TM API requests run in background threads
- **Robust Error Handling**: Graceful degradation when TM API unavailable
- **Manual JSON Parsing**: No external library dependencies added
- **Changeset Comment Parsing**: Regex-based detection of TM task references

---

## 📋 Updated Components

### New Files:
- `PluginSettings.java` - Centralized settings management via JOSM preferences
- `TaskManagerAPIClient.java` - TM API client with manual JSON parsing
- `SettingsPanel.java` - Swing-based settings UI dialog

### Modified Files:
- `ValidationToolPanel.java` - Added TM URL field, remote control detection, auto-population logic
- `DPWValidationToolPlugin.java` - Added settings menu action
- `build.xml` - Updated version to 3.1.0-BETA

---

## ⚠️ BETA Notice

**This is a BETA release. The Tasking Manager integration feature is:**
- **Disabled by default** - Must be manually enabled in Settings
- **Experimental** - Test thoroughly before production use
- **Optional** - All existing functionality works without TM integration

To enable TM integration:
1. Open JOSM
2. Go to **Tools → DPW Validation Settings...**
3. Check "Enable Tasking Manager Integration"
4. Optionally enable "Remote Control Task Detection"
5. Click **Save**

---

## 🧪 Testing Notes

**Test Project Used During Development:**
- Project: https://tasks.hotosm.org/projects/27396
- Test API: TM API v2 (`tasking-manager-tm4-production-api.hotosm.org/api/v2`)
- DPW API: `app.spatialcollective.com/api` (updated from `dpw-mauve.vercel.app`)

**Testing Workflow:**
1. Enable TM integration in settings
2. Open a mapped task from HOT Tasking Manager via remote control
3. Verify Task ID and Mapper fields auto-populate
4. Verify settlement auto-fetches from DPW API
5. Complete validation as normal

**Alternative Workflow:**
1. Enable TM integration in settings
2. Manually paste TM URL into "TM URL (BETA)" field
3. Verify Task ID and Mapper auto-populate
4. Continue validation workflow

---

## 🐛 Known Issues

1. **Mapper Not in Authorized List**
   - If TM mapper is not in DPW authorized mapper list, you'll receive a warning
   - Workaround: Refresh mapper list or manually select mapper

2. **Unmapped Tasks**
   - TM API will return error for tasks that haven't been mapped yet
   - Expected behavior: Error message "No mapper found for this task"

3. **Network Timeouts**
   - TM API calls have 10-second timeout
   - If TM API is slow, auto-detection may fail silently
   - Check JOSM logs for detailed error messages

---

## 📝 Configuration Reference

### Default Settings:
```
TM Integration: DISABLED
DPW API: app.spatialcollective.com/api
TM API: tasking-manager-tm4-production-api.hotosm.org/api/v2
Auto-fetch Settlement: ENABLED
Remote Control Detection: DISABLED
Cache Expiry: 24 hours
```

### Recommended Settings for Validators:
```
TM Integration: ENABLED
Auto-fetch Settlement: ENABLED
Remote Control Detection: ENABLED
```

---

## 🔜 Future Enhancements (Not in this release)

- OAuth 2.0 authentication for TM API
- Task grid visualization on JOSM map
- Bulk validation support for multiple tasks
- TM project statistics integration
- Automatic task locking/unlocking

---

## 📞 Support & Feedback

This is a BETA release. Please report issues to:
- **GitHub**: [SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin](https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin)
- **Email**: Spatial Collective Ltd support

**Important:** Always test in a non-production environment first.

---

## 📦 Installation

1. Download `DPWValidationTool.jar` from GitHub Releases
2. Copy to JOSM plugins directory
3. Restart JOSM
4. Open **Tools → DPW Validation Settings...** to configure
5. Enable TM integration if desired

---

## ✅ Upgrade Path from v3.0.1

This is a **non-breaking upgrade**:
- All v3.0.1 functionality preserved
- TM features are opt-in only
- No configuration changes required for existing users
- Existing workflows unaffected

---

**Release Prepared By:** GitHub Copilot & Spatial Collective Development Team  
**Testing Status:** Awaiting real-world validation workflow testing  
**Production Ready:** NO - BETA only
