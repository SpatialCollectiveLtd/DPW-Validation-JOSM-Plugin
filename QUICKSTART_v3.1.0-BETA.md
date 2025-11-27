# Quick Start Guide - v3.1.0-BETA TM Integration

## For Developers: Building the Plugin

### Prerequisites
- JDK 21+ installed
- Apache Ant installed
- JOSM test jar (`josm-tested.jar`) in project root

### Build Steps

```powershell
# Clean previous builds
ant clean

# Build the plugin
ant dist

# Result: dist/DPWValidationTool.jar
```

### Install in JOSM

```powershell
# Windows - Copy to JOSM plugins folder
$josmPlugins = "$env:APPDATA\JOSM\plugins"
Copy-Item dist\DPWValidationTool.jar $josmPlugins\
```

Then restart JOSM.

---

## For Users: First-Time Setup

### Step 1: Install Plugin
1. Download `DPWValidationTool.jar` from GitHub Releases
2. Copy to JOSM plugins folder:
   - Windows: `%APPDATA%\JOSM\plugins\`
   - Linux: `~/.config/JOSM/plugins/`
   - macOS: `~/Library/JOSM/plugins/`
3. Restart JOSM

### Step 2: Open Plugin
- Go to **Tools → DPW Validation Tool**
- Panel opens on right side of JOSM

### Step 3: (Optional) Enable TM Integration
1. Go to **Tools → DPW Validation Settings...**
2. Check "Enable Tasking Manager Integration"
3. Optionally check "Enable Remote Control Task Detection"
4. Click **Save**
5. Restart validation panel (close and reopen)

**Note:** TM integration is **disabled by default** in BETA

---

## Usage: TM Integration Workflows

### Workflow 1: Paste TM URL (Simple)

1. Open validation panel
2. Find TM URL field at top (if integration enabled)
3. Paste URL: `https://tasks.hotosm.org/projects/27396/tasks/123`
4. Wait 2-3 seconds
5. **Task ID and Mapper auto-populate!**
6. Continue validation as normal

### Workflow 2: Remote Control (Advanced)

1. **Enable remote control detection in settings first**
2. Go to tasks.hotosm.org
3. Select a mapped task
4. Click "Edit with JOSM"
5. JOSM loads data via remote control
6. **Popup appears**: "Task Manager Task Detected!"
7. **All fields auto-populated from changeset comment**
8. Continue validation as normal

### Workflow 3: Traditional (No TM)

1. Leave TM integration disabled
2. Manually enter Task ID
3. Manually select mapper
4. Continue as in v3.0.1

---

## Testing the BETA Feature

### Test Project
Use HOT TM project 27396 for testing:
- URL: https://tasks.hotosm.org/projects/27396
- Name: "test api"
- Status: Most tasks are READY (unmapped)

### To Test Mapper Detection:
1. You'll need to map a task in project 27396 first
2. Or use a different project with mapped tasks
3. Paste the URL into the plugin
4. Verify mapper auto-detection

### What to Test:
- [ ] Settings panel opens and saves
- [ ] TM URL parsing works
- [ ] Task ID auto-fills
- [ ] Mapper auto-selects (if in authorized list)
- [ ] Settlement auto-fetches
- [ ] Remote control detection (if enabled)
- [ ] Error handling for invalid URLs
- [ ] Error handling for unmapped tasks
- [ ] No regressions in existing features

---

## Troubleshooting

### TM URL field not showing?
- Check settings: Tools → DPW Validation Settings
- Ensure "Enable Tasking Manager Integration" is checked
- Close and reopen validation panel

### Task ID not auto-filling?
- Check JOSM logs (F12) for errors
- Verify URL format is correct
- Ensure internet connection is active
- TM API may be slow (10s timeout)

### Mapper not auto-selecting?
- Mapper may not be in DPW authorized list
- Check warning message in dialog
- Refresh mapper list with button
- Manually select correct mapper

### Remote control not detecting?
- Enable in settings first
- Ensure changeset comment has correct format
- Check JOSM logs for detection messages
- Verify JOSM remote control is enabled

### Settings not persisting?
- JOSM preferences may be read-only
- Check JOSM preferences file permissions
- Try running JOSM as administrator (Windows)

---

## Configuration Reference

### Settings Panel Options

| Setting | Default | Description |
|---------|---------|-------------|
| TM Integration | OFF | Enable HOT TM features |
| DPW API URL | app.spatialcollective.com/api | DPW backend |
| TM API URL | tasking-manager-tm4-production-api.hotosm.org/api/v2 | TM backend |
| Auto-fetch Settlement | ON | Auto-get settlement from DPW |
| Remote Control Detection | OFF | Auto-detect from changeset |
| Cache Expiry | 24 hours | TM API cache duration |

### Recommended Settings for Validators:
```
TM Integration: ON
Auto-fetch Settlement: ON
Remote Control Detection: ON
```

---

## Known Issues (BETA)

1. **Limited Testing**
   - No mapped tasks in test project 27396
   - Full workflow not validated yet

2. **Pre-existing Code Warnings**
   - ValidationToolPanel has legacy warnings
   - Not introduced by v3.1.0
   - Does not affect functionality

3. **Network Dependency**
   - Requires internet for TM API
   - 10-second timeout may be too short
   - Fails silently on network errors

4. **No OAuth Support**
   - Read-only TM API access
   - Cannot lock/unlock tasks
   - Future enhancement

---

## Reporting Issues

If you encounter bugs:

1. Check JOSM logs (F12) for error messages
2. Note exact steps to reproduce
3. Report on GitHub Issues with:
   - JOSM version
   - Plugin version (3.1.0-BETA)
   - OS and Java version
   - Log output
   - Screenshots if applicable

---

## Next Steps After BETA

### If successful:
1. Gather user feedback
2. Fix reported issues
3. Performance optimization
4. Promote to stable release (v3.1.0)

### If issues found:
1. Document all bugs
2. Disable TM integration
3. Continue using v3.0.1 workflow
4. Fix issues in patch release

---

## Documentation Files

- `RELEASE_NOTES_v3.1.0-BETA.md` - Full release notes
- `TESTING_GUIDE_v3.1.0-BETA.md` - Comprehensive test plan
- `IMPLEMENTATION_SUMMARY.md` - Technical implementation details
- `README.md` - General plugin documentation

---

## Quick Command Reference

### Build Plugin:
```bash
ant clean dist
```

### Install Plugin (Windows):
```powershell
Copy-Item dist\DPWValidationTool.jar $env:APPDATA\JOSM\plugins\
```

### View JOSM Logs:
Press `F12` in JOSM

### Open Settings:
Tools → DPW Validation Settings...

### Open Validation Panel:
Tools → DPW Validation Tool

---

**Happy Validating!** 🗺️

For questions or support, see the main README or open a GitHub issue.

---

**Version:** 1.0  
**Last Updated:** [Current Date]  
**Plugin Version:** 3.1.0-BETA
