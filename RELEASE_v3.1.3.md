# DPW Validation Tool v3.1.3 Release

**Release Date:** January 6, 2025  
**Status:** ⚠️ TESTING REQUIRED  

---

## 📦 Release Files

**Plugin:** `DPWValidationTool.jar`  
**Version:** 3.1.3  
**Size:** 157,731 bytes (154 KB)  
**SHA-256:** `978A4A3FD23BF762935FE3A36B45AC12308F57E4449268BC79ADB1B99C91A25A`

---

## ✨ What's New

### Critical Fix #1: HTTP 429 Bypass
- Added browser-like HTTP headers to all POST requests
- Mimics legitimate browser traffic to bypass Vercel DDoS protection
- Headers: User-Agent, Origin, Referer, Accept-Language, Cache-Control

### Critical Fix #2: Task ID Auto-Population
- Enter Task ID in format `projectId-taskId` (e.g., `16043-21`)
- Automatically fetches mapper from HOT Tasking Manager API
- No more manual lookup on TM website!

---

## ⚠️ IMPORTANT: Server-Side Configuration Required

You've added a bypass rule for `/api/users` ✅, but you also need to add:

### Required Vercel Firewall Rules:

**Rule 2: Allow validation-log submissions**
```
Name: DPW JOSM PLUGIN VALIDATION BYPASS
If: Request Header X-API-Key Equals dpw_josm_plugin_digitization_2025_secure_key_f8a9b2c3d1e4
And: Raw Path Starts with /api/validation-log
Then: Bypass
```

**Rule 3: Allow OSM file uploads**
```
Name: DPW JOSM PLUGIN UPLOAD BYPASS
If: Request Header X-API-Key Equals dpw_josm_plugin_digitization_2025_secure_key_f8a9b2c3d1e4
And: Raw Path Starts with /api/upload-osm
Then: Bypass
```

**OR** create a single rule covering all API endpoints:
```
Name: DPW JOSM PLUGIN API BYPASS
If: Request Header X-API-Key Equals dpw_josm_plugin_digitization_2025_secure_key_f8a9b2c3d1e4
And: Raw Path Starts with /api/
Then: Bypass
```

---

## 🧪 Testing Procedure

### 1. Install Plugin
```
1. Copy dist/DPWValidationTool.jar to your JOSM plugins folder
   Windows: %APPDATA%\JOSM\plugins\
   
2. Restart JOSM

3. Verify: Preferences > Plugins > Search "DPW"
   Should show version 3.1.3
```

### 2. Test Task ID Auto-Population
```
1. Open DPW Validation Tool panel
2. In Task ID field, enter: 16043-21 (or any valid HOT TM task)
3. ✅ Expected: Mapper dropdown auto-selects the mapper's username
4. ✅ Check log: "TM integration: Auto-populated mapper 'USERNAME' for task 21"
```

### 3. Test Validation Submission (CRITICAL!)
```
1. Load buildings in JOSM
2. Fill in all fields (mapper, task ID, counts, status)
3. Click "Validate" or "Invalidate"
4. ✅ Expected: Green success message "Validation submitted successfully"
5. ❌ If 429 error: Check if you added the /api/validation-log bypass rule!
```

### 4. Verify on Dashboard
```
1. Go to https://dpw-mauve.vercel.app
2. Login as validator
3. Check validation logs table
4. ✅ Expected: Your test validation appears with correct data
```

### 5. Test File Upload (Optional)
```
1. After successful validation, click "Upload Audited OSM Data to Cloud"
2. Select a .osm file
3. ✅ Expected: Upload completes without errors
4. ❌ If 429 error: Add the /api/upload-osm bypass rule
```

---

## 📋 Test Results Log

**Date:** __________  
**Tester:** __________  

- [ ] Plugin installs successfully (version shows 3.1.3)
- [ ] Task ID auto-populates mapper
- [ ] Validation submission works (no 429 error)
- [ ] Validation appears on dashboard
- [ ] File upload works (if tested)

**Notes:**
```
(Record any issues here)
```

---

## 🐛 Troubleshooting

### Still Getting 429 Errors?

**Step 1: Check JOSM Log**
```
Preferences > Advanced > Show Advanced Preferences
Search for: "Log level"
Set to: DEBUG
Try submission again
Check log for exact error
```

**Step 2: Verify Vercel Rules**
- Go to Vercel dashboard > Your project > Firewall
- Confirm bypass rules exist for:
  - `/api/users` ✅ (you already added this)
  - `/api/validation-log` ❓
  - `/api/upload-osm` ❓

**Step 3: Test from Browser**
- Open https://dpw-mauve.vercel.app
- Submit validation from web interface
- If web works but plugin doesn't → firewall issue
- If web also fails → API/server issue

### Task ID Not Auto-Populating?

**Check format:** Must be `PROJECT-TASK` (e.g., `16043-21`)

**Test TM API:**
```powershell
Invoke-WebRequest -Uri "https://tasking-manager-tm4-production-api.hotosm.org/api/v2/projects/16043/tasks/21/"
```
Should return JSON with task data.

---

## 📞 Support

If tests fail:
1. Copy full error from JOSM log
2. Screenshot of error message
3. Vercel firewall rules screenshot
4. Send to developer

---

## ✅ Ready to Release?

**DO NOT USE IN PRODUCTION** until:
- ✅ All firewall bypass rules are configured
- ✅ Test validation submission succeeds
- ✅ Task ID auto-population works
- ✅ Validation appears on dashboard

Once all tests pass, this version can replace v3.1.2.
