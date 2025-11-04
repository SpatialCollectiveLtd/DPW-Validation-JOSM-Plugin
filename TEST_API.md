# API Testing Guide for DPW JOSM Plugin v2.1

## Quick Test Commands

### Test 1: Verify Authorization Endpoint

**Command (PowerShell):**
```powershell
curl "https://dpw-mauve.vercel.app/api/users?exclude_managers=true&status=Active"
```

**Expected Response:**
```json
{
  "success": true,
  "data": [
    {
      "user_id": 5,
      "youth_id": "DPW-DIGI-003",
      "full_name": "John Doe",
      "osm_username": "john_mapper",
      "settlement": "Mji wa Huruma",
      "role": "Digitizer",
      "status": "Active",
      "start_date": "2024-01-15",
      "created_at": "2024-01-15T10:00:00.000Z"
    }
  ],
  "count": 1
}
```

**What to check:**
- ✅ Response has `success: true`
- ✅ `count` > 0 (shows active users exist)
- ✅ Each user has `osm_username` field
- ✅ No Manager roles appear (security check)

---

### Test 2: Verify Validation Log Endpoint

**Command (PowerShell):**
```powershell
$body = @{
    mapper_osm_username = "test_digitizer"
    validator_osm_username = "test_validator"
    settlement = "Test Settlement"
    total_buildings = 100
    error_hanging_nodes = 5
    validation_status = "Validated"
    validator_comments = "Test submission"
} | ConvertTo-Json

Invoke-RestMethod -Uri "https://dpw-mauve.vercel.app/api/validation-log" `
    -Method Post `
    -Body $body `
    -ContentType "application/json"
```

**Expected Success Response:**
```json
{
  "success": true,
  "message": "Validation log created successfully",
  "data": {
    "log_id": 42,
    "mapper_user_id": 5,
    "validator_user_id": 3,
    "mapper_name": "John Doe",
    "validator_name": "Jane Smith"
  }
}
```

**Expected Error (User Not Found):**
```json
{
  "success": false,
  "error": "Mapper with OSM username 'test_digitizer' not found in Users table"
}
```

---

## Testing in JOSM Plugin

### Setup

1. **Install the plugin:**
   ```powershell
   $josmPlugins = "$env:USERPROFILE\.josm\plugins"
   New-Item -ItemType Directory -Path $josmPlugins -Force
   Copy-Item "c:\Users\TECH\Desktop\DPW JOSM Plugin\dist\DPWValidationTool.jar" -Destination $josmPlugins
   ```

2. **Start JOSM**
   - Restart JOSM completely
   - Open Data → DPW Validation Tool

3. **Authenticate**
   - Edit → Preferences → Connection Settings
   - Click "Authorize now"
   - Complete OAuth 2.0 authentication

---

### Test Scenarios

#### Scenario 1: Load Authorized Users

**Steps:**
1. Open DPW Validation Tool panel
2. Plugin should auto-load authorized users on startup
3. Check status label at bottom of panel

**Expected Result:**
- Status: "Success: X authorized users loaded from DPW Manager."
- Background color: Green
- Mapper dropdown populated with OSM usernames
- No Manager accounts visible

**If it fails:**
- Check JOSM console for error messages
- Verify base URL: https://dpw-mauve.vercel.app
- Check internet connection
- Verify API is accessible

---

#### Scenario 2: Validate Authorization Check

**Steps:**
1. Note your OSM username (from JOSM → Edit → Preferences → Connection Settings)
2. Check if your username appears in the authorized list
3. Try to isolate mapper data

**Expected Result:**
- If authorized: Isolation proceeds
- If NOT authorized: Error dialog appears with clear message

**Error Message Should Say:**
```
Submission Failed: Your username ('xxx') is not registered as a validator 
for this project. Please contact the project manager.
```

---

#### Scenario 3: Submit Validation Log

**Steps:**
1. Select a date
2. Click "Isolate Mapper's Work"
3. Review data in JOSM
4. Fill in error counts and comments
5. Click "Accept Validator Work" or "Reject Validator Work"

**Expected Result:**
- Sending dialog appears
- API call completes successfully
- Success message: "Validation data submitted successfully!"
- Prompt to reset session

**Check JOSM Console For:**
```
DPWValidationTool: Submitting validation data to https://dpw-mauve.vercel.app/api/validation-log
DPWValidationTool: API responded with HTTP 201
```

---

## Debugging

### Enable Debug Logging

Check JOSM console (`View → Toggle Console`) for messages:

```
DPWValidationTool: constructing ValidationToolPanel v2.1
DPWValidationTool: Fetching authorized users from DPW Manager...
DPWValidationTool: Submitting validation data to [URL]
DPWValidationTool: JSON payload: {...}
DPWValidationTool: API responded with HTTP [code]
```

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| "No users loaded" | API connection failed | Check internet, verify API URL |
| "User not authorized" | Your OSM username not in database | Contact DPW admin to add you |
| "404 User not found" | Selected mapper doesn't exist | Refresh mapper list, select different user |
| "400 Missing fields" | Required field empty | Check all required fields are filled |
| Connection timeout | Slow network or API down | Increase timeout, retry |

### Test with curl

```powershell
# Test authorization endpoint
curl -v "https://dpw-mauve.vercel.app/api/users?exclude_managers=true&status=Active"

# Should return HTTP 200 and JSON with users array
```

---

## Validation Checklist

Before deploying to production:

- [ ] Authorization endpoint returns users successfully
- [ ] No Manager accounts visible in plugin
- [ ] Mapper dropdown populates correctly
- [ ] OAuth authentication works
- [ ] Date selection enforced before isolation
- [ ] Authorization check blocks unauthorized users
- [ ] Validation submission succeeds with HTTP 201
- [ ] Error messages are clear and actionable
- [ ] Session reset works after submission
- [ ] Input validation catches invalid data
- [ ] All 10 error types submit correctly
- [ ] Both "Accept" and "Reject" statuses work

---

## API Response Examples

### Success - Users List
```json
{
  "success": true,
  "data": [
    {
      "osm_username": "mapper1",
      "full_name": "John Mapper",
      "role": "Digitizer",
      "status": "Active"
    },
    {
      "osm_username": "validator1",
      "full_name": "Jane Validator",
      "role": "Validator",
      "status": "Active"
    }
  ],
  "count": 2
}
```

### Success - Validation Log Created
```json
{
  "success": true,
  "message": "Validation log created successfully",
  "data": {
    "log_id": 123,
    "mapper_user_id": 5,
    "validator_user_id": 8,
    "mapper_name": "John Mapper",
    "validator_name": "Jane Validator"
  }
}
```

### Error - User Not Found
```json
{
  "success": false,
  "error": "Validator with OSM username 'unknown_user' not found in Users table"
}
```

### Error - Missing Fields
```json
{
  "success": false,
  "error": "Missing required fields: mapper_osm_username, validator_osm_username, total_buildings"
}
```

---

## Support

If tests fail or you encounter issues:

1. Check this test guide first
2. Review JOSM console logs
3. Verify API endpoints with curl
4. Check integration guide: `JOSM_PLUGIN_INTEGRATION_GUIDE.md`
5. Contact DPW development team with:
   - Error messages from JOSM console
   - curl test results
   - Your OSM username
   - Steps to reproduce the issue

---

**Last Updated:** October 23, 2025  
**Plugin Version:** 2.1.0  
**API Base URL:** https://dpw-mauve.vercel.app
