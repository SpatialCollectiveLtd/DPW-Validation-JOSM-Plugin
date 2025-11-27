# 🎉 DPW Validation Tool v3.1.0-BETA

**Release Type:** BETA (Pre-Release)  
**Release Date:** November 27, 2025  
**Previous Version:** v3.0.1

---

## ⚠️ BETA NOTICE

This is a **BETA release** with optional new features. The Tasking Manager integration is:
- ✅ **Disabled by default** - Must be manually enabled in settings
- ✅ **100% backward compatible** - All v3.0.1 features work unchanged
- ✅ **Optional upgrade** - Use only if you need TM integration
- ⚠️ **Experimental** - Test thoroughly before production use

---

## 🚀 What's New

### 1. HOT Tasking Manager Integration (BETA)

**Automatic Mapper Detection:**
- Paste a Tasking Manager URL to auto-populate Task ID and Mapper
- Supported formats: `tasks.hotosm.org/projects/27396/tasks/123`
- Fetches mapper username from TM API task history
- Auto-triggers settlement fetch from DPW API

**Remote Control Workflow (Game Changer!):**
- Click "Edit with JOSM" in HOT Tasking Manager
- Plugin automatically detects task from changeset comment
- Parses `#hotosm-project-XXXXX-task-YYY` format
- All fields auto-populate instantly
- **Perfect for validators**: Zero manual data entry!

### 2. Plugin Settings Panel

**Configuration Without Rebuild:**
- New menu: **Tools → DPW Validation Settings...**
- Configure API URLs (DPW, TM)
- Toggle TM integration on/off
- Enable/disable auto-fetch settlement
- Enable/disable remote control detection
- Set cache expiry (1-168 hours)
- Reset to defaults option

### 3. Auto-Update System

**Stay Up-to-Date Automatically:**
- Menu: **Tools → Check for DPW Plugin Updates...**
- Button in Settings panel
- Automatic check on JOSM startup (silent)
- Update notifications with release notes preview
- One-click GitHub download link
- Version comparison (BETA → Stable notifications)

---

## 📦 Installation

### New Users:

1. Download `DPWValidationTool.jar` from Assets below
2. Copy to JOSM plugins folder:
   - **Windows:** `%APPDATA%\JOSM\plugins\`
   - **Linux:** `~/.config/JOSM/plugins/`
   - **macOS:** `~/Library/JOSM/plugins/`
3. Restart JOSM
4. Open: **Tools → DPW Validation Tool**

### Upgrading from v3.0.1:

1. Download new JAR from Assets
2. Replace old JAR in plugins folder
3. Restart JOSM
4. **TM integration is disabled by default** - enable in settings if needed

---

## 🎯 Quick Start - TM Integration

### Enable TM Integration:

1. **Tools → DPW Validation Settings...**
2. Check ☑️ "Enable Tasking Manager Integration"
3. Check ☑️ "Enable Remote Control Task Detection" (recommended)
4. Click **Save**

### Usage - Remote Control (Recommended):

1. Open a mapped task in HOT Tasking Manager
2. Click **"Edit with JOSM"** button
3. **Plugin auto-detects task and populates all fields** ✨
4. Verify data and continue validation

### Usage - Manual URL:

1. Copy TM URL (e.g., `tasks.hotosm.org/projects/27396/tasks/123`)
2. Paste into "TM URL (BETA)" field in validation panel
3. Task ID and Mapper auto-populate
4. Continue validation

### Disable TM Integration:

1. **Tools → DPW Validation Settings...**
2. Uncheck ☐ "Enable Tasking Manager Integration"
3. Click **Save**
4. Plugin works exactly as v3.0.1

---

## 📋 Technical Details

### New Files:
- `PluginSettings.java` - Settings management via JOSM preferences
- `TaskManagerAPIClient.java` - TM API client with manual JSON parsing
- `SettingsPanel.java` - Swing-based settings UI
- `UpdateChecker.java` - GitHub releases API integration

### Modified Files:
- `ValidationToolPanel.java` - TM URL field, remote control detection
- `DPWValidationToolPlugin.java` - Menu items, startup update check
- `build.xml` - Version 3.1.0-BETA

### API Integration:
- **DPW API:** `https://app.spatialcollective.com/api` (updated from dpw-mauve.vercel.app)
- **TM API:** `https://tasking-manager-tm4-production-api.hotosm.org/api/v2`
- **GitHub API:** Auto-update from releases

---

## 🧪 Testing

See [TESTING_GUIDE_v3.1.0-BETA.md](https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/blob/main/TESTING_GUIDE_v3.1.0-BETA.md) for comprehensive testing procedures.

**Test Project:** https://tasks.hotosm.org/projects/27396

---

## 🐛 Known Issues

1. **Test Project Has No Mapped Tasks:** Project 27396 used for testing has no mapped tasks yet. Real-world testing with mapped tasks is pending.

2. **Remote Control Detection:** Layer name parsing may not work in all scenarios. Fallback to manual entry always available.

3. **Network Timeouts:** TM API calls have 10-second timeout. Slow connections may fail silently (check JOSM logs).

---

## ⚙️ Default Configuration

```
TM Integration: DISABLED
DPW API: https://app.spatialcollective.com/api
TM API: https://tasking-manager-tm4-production-api.hotosm.org/api/v2
Auto-fetch Settlement: ENABLED
Remote Control Detection: DISABLED
Cache Expiry: 24 hours
```

---

## 🔄 Upgrade Path

**From v3.0.1:**
- ✅ Non-breaking upgrade
- ✅ All existing features preserved
- ✅ TM features are opt-in only
- ✅ No configuration changes required
- ✅ Can disable TM integration anytime

---

## 📚 Documentation

- [RELEASE_NOTES_v3.1.0-BETA.md](https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/blob/main/RELEASE_NOTES_v3.1.0-BETA.md) - Complete release notes
- [TESTING_GUIDE_v3.1.0-BETA.md](https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/blob/main/TESTING_GUIDE_v3.1.0-BETA.md) - QA procedures
- [QUICKSTART_v3.1.0-BETA.md](https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/blob/main/QUICKSTART_v3.1.0-BETA.md) - Quick setup guide
- [IMPLEMENTATION_SUMMARY.md](https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/blob/main/IMPLEMENTATION_SUMMARY.md) - Technical architecture

---

## 📞 Support

**Issues:** https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/issues  
**Developer:** Spatial Collective Ltd  
**Repository:** https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin

---

## ✅ Requirements

- JOSM Build 18823 or higher
- Java 21.0 or higher
- Internet connection for TM API and auto-update
- Windows 10+, Linux, or macOS

---

## 🎯 Future Enhancements (Post-BETA)

Based on user feedback, future releases may include:
- OAuth 2.0 authentication for TM API
- Task grid visualization on map
- Bulk validation support for multiple tasks
- Full cache implementation for TM data
- Task locking/unlocking integration
- Project statistics integration

---

## 🙏 Feedback Welcome

This is a BETA release - your feedback is crucial! Please report:
- Bugs or unexpected behavior
- Feature requests
- Usability improvements
- Documentation gaps

**Report via:** [GitHub Issues](https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/issues)

---

**Ready to test?** Download the JAR from Assets below and give it a try! 🚀
