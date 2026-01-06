# DPW Validation Tool v3.1.3 - TESTING CHECKLIST

## Version: 3.1.3
**Release Date:** January 2025  
**Critical Fixes:** Vercel DDoS bypass + Task ID mapper auto-population

---

## What Was Fixed

### Issue 1: HTTP 429 Errors on Validation Submission
**Root Cause:** Vercel's DDoS protection was blocking POST requests from the plugin  
**Solution:** Added browser-like HTTP headers to all POST requests:
- `User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) DPW-JOSM-Plugin/3.1.3`
- `Accept-Language: en-US,en;q=0.9`
- `Accept-Encoding: gzip, deflate`
- `Origin`, `Referer`, `Cache-Control`, `Pragma` headers

**Files Modified:**
- `ValidationToolPanel.java` (line 2673)
- `DPWAPIClient.java` (lines 258, 351)

### Issue 2: Task ID Doesn't Auto-Populate Mapper
**Root Cause:** Task ID field listener only enabled/disabled submit buttons, didn't fetch mapper info  
**Solution:** Added `fetchMapperFromTaskId()` method that:
- Parses Task ID in format `projectId-taskId` (e.g., `12345-67`)
- Calls HOT Tasking Manager API
- Auto-selects mapper in dropdown
- Auto-fetches settlement if enabled

**Files Modified:**
- `ValidationToolPanel.java` (line 560, added method at line 3299)

---

## MANDATORY TESTING BEFORE RELEASE

### ✅ Test 1: Verify Build
```bash
cd "c:\Users\TECH\Desktop\DPW JOSM Plugin"
ant clean dist
```
**Expected:** BUILD SUCCESSFUL  
**Location:** `dist/DPWValidationTool.jar`

---

### ✅ Test 2: Task ID Auto-Population

1. **Install Plugin:**
   - Copy `dist/DPWValidationTool.jar` to JOSM plugins folder
   - Restart JOSM
   - Verify version shows "3.1.3" in Plugin Manager

2. **Test Task ID Parsing:**
   - Open DPW Validation Tool panel
   - Enter a valid HOT TM task ID in format: `PROJECT-TASK` (e.g., `16043-21`)
   - **Expected Behavior:**
     - Mapper dropdown should auto-populate with the mapper's OSM username
     - If "Auto-fetch settlement" is enabled, settlement should also populate
     - Check JOSM log for: `TM integration: Auto-populated mapper 'USERNAME' for task XX`

3. **Test Invalid Format:**
   - Enter just a number (e.g., `12345`)
   - **Expected:** No error, just no auto-population

4. **Test Empty Task ID:**
   - Clear the Task ID field
   - **Expected:** No errors, submit buttons disabled

---

### ✅ Test 3: Validation Submission (CRITICAL)

**THIS TEST MUST PASS BEFORE RELEASE!**

1. **Prepare Test Data:**
   - Load some buildings in JOSM
   - Open DPW Validation Tool
   - Select a mapper from dropdown
   - Enter valid Task ID (e.g., `16043-21`)
   - Enter building counts
   - Select validation status

2. **Submit Validation:**
   - Click "Validate" or "Invalidate" button
   - **Expected Response:**
     - **SUCCESS:** Green notification "Validation submitted successfully"
     - **FAILURE:** If you still get 429 error, check JOSM log for exact error

3. **Check DPW Manager Dashboard:**
   - Go to https://dpw-mauve.vercel.app
   - Login as validator
   - Check if validation log appears
   - Verify all fields are correct

4. **Check JOSM Log:**
   ```
   Preferences > Advanced > Expert mode > Log
   Filter for: "DPWValidationTool"
   ```
   - Look for: `API responded with HTTP 201` (success)
   - If 429: Copy full log and send to developer

---

### ✅ Test 4: Remote Control TM Integration

1. **Use HOT Tasking Manager:**
   - Go to https://tasks.hotosm.org
   - Find a task you're mapped
   - Click "Edit in JOSM" button
   - **Expected:**
     - JOSM loads data
     - DPW panel auto-detects project/task
     - Shows popup: "Task Manager Task Detected!"
     - Auto-fills Task ID and Mapper

---

### ✅ Test 5: OSM File Upload

1. **After Successful Validation:**
   - Click "Upload Audited OSM Data to Cloud"
   - Select a .osm file
   - **Expected:**
     - Upload completes without 429 error
     - Success notification

---

## If Tests FAIL

### If Still Getting 429 Errors:

**Do NOT release!** Instead:

1. **Check JOSM Log:**
   - Copy the full error from JOSM log
   - Look for the exact response from server

2. **Test from Browser:**
   - Open https://dpw-mauve.vercel.app
   - Open browser DevTools > Network tab
   - Submit a validation from the web interface
   - Check if it works (should work)
   - Compare request headers

3. **Contact Server Admin:**
   - The issue is likely Vercel firewall/DDoS protection
   - May need to whitelist the plugin's User-Agent
   - OR disable Vercel Shield for `/api/validation-log`

### If Task ID Not Auto-Populating:

1. **Check TM API Access:**
   - Test: https://tasking-manager-tm4-production-api.hotosm.org/api/v2/projects/16043/tasks/21/
   - Should return JSON with `taskHistory` array

2. **Check JOSM Log:**
   - Look for: "TM integration: Auto-populated mapper..."
   - OR: "Error fetching mapper from Task ID..."

---

## Release Notes for v3.1.3

```
**v3.1.3 - Critical Fixes**

- FIX: Added browser-like HTTP headers to bypass Vercel DDoS protection (fixes persistent 429 errors)
- FIX: Task ID field now auto-populates mapper from HOT Tasking Manager API
- Format: Enter Task ID as "projectId-taskId" (e.g., "16043-21")
- Improved: Better logging for API requests and TM integration

**Testing Status:**
- [ ] Validation submission works without 429 errors
- [ ] Task ID auto-populates mapper
- [ ] Remote control TM integration works
- [ ] OSM file upload works

**DO NOT RELEASE UNTIL ALL TESTS PASS!**
```

---

## Developer Notes

**Why We Can't Test POST from PowerShell:**
- Vercel's edge network blocks PowerShell/curl/wget User-Agents
- Must test from actual JOSM plugin
- Browser-like headers should bypass protection

**Task ID Format:**
- Old: User had to manually check TM website for mapper
- New: Enter `PROJECT-TASK` and mapper auto-fills
- Example: `16043-21` → queries TM API → selects mapper

**Next Steps if 429 Persists:**
- Vercel Shield may need configuration change
- Alternative: Move API to different hosting (not Vercel)
- Last resort: Implement OAuth2 authentication
