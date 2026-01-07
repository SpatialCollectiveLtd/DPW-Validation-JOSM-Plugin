# Testing Guide for v3.2.9

## Quick Test

Run this command to verify the API endpoint is accessible:

```powershell
$headers = @{
    "X-API-Key" = "dpw-josm-plugin-2025-secure-key"
    "Accept" = "application/json"
}
Invoke-RestMethod -Uri "https://app.spatialcollective.com/api/users?osm_username=test&exclude_managers=true" -Headers $headers -Method GET
```

Expected: HTTP 200 with JSON response containing user data

## Installation Test

1. **Copy the plugin:**
   ```powershell
   Copy-Item "dist\DPWValidationTool.jar" "$env:APPDATA\JOSM\plugins\" -Force
   ```

2. **Restart JOSM**

3. **Verify version:**
   - Open JOSM
   - Go to Edit → Preferences → Plugins
   - Look for "DPW Validation Tool"
   - Version should show: **3.2.9**

## Functional Tests

### Test 1: Mapper List Loading
1. Open the DPW Validation Tool panel in JOSM
2. The mapper dropdown should populate automatically
3. Check JOSM logs (Help → Show Log) for:
   ```
   DPWValidationTool: Fetching user_id for: <username>
   DPWValidationTool: API responded with HTTP 200
   ```

**Expected:** No HTTP 429, 405, or 401 errors

### Test 2: Validation Submission
1. Create a simple validation in JOSM
2. Fill in all required fields:
   - Select a mapper
   - Select a validator
   - Add task ID
   - Add settlement name
   - Add results
3. Click "Send Validation"
4. Check logs for:
   ```
   DPWValidationTool: Submitting validation data to https://app.spatialcollective.com/api/validation-log
   DPWValidationTool: API responded with HTTP 200 (or 201)
   ```

**Expected:** Success message, no HTTP 405 errors

### Test 3: OSM File Upload (if applicable)
1. Perform a validation that includes file upload
2. Check logs for:
   ```
   DPWValidationTool: Uploading to cloud: <filename>
   ```

**Expected:** HTTP 200/201, successful upload

## Troubleshooting

### HTTP 405 Error
- **Cause:** Old endpoint being used
- **Fix:** Verify you're running v3.2.9 (not v3.0.3)
- **Check:** Look in logs for endpoint URL - should be `app.spatialcollective.com/api`, not `dpw-mauve.vercel.app`

### HTTP 429 Error
- **Cause:** Missing authentication
- **Fix:** Verify X-API-Key header is present in logs
- **Check:** Run the Quick Test above to verify API key works

### HTTP 401 Error
- **Cause:** Invalid or missing API key
- **Fix:** Contact Spatial Collective for updated API key
- **Check:** Current key is `dpw-josm-plugin-2025-secure-key`

### Mapper List Not Loading
- **Cause:** Network or API issue
- **Fix:** Check internet connection, verify API endpoint is reachable
- **Test:** Run Quick Test command above

## Log Analysis

Enable debug logging in JOSM:
1. Edit → Preferences → Advanced
2. Search for `log.level`
3. Set to `debug`
4. Restart JOSM

Look for these key log entries:

**Good (Success):**
```
DPWValidationTool: Fetching user_id for: john.doe
DPWValidationTool: API responded with HTTP 200
DPWValidationTool: Submitting validation data to https://app.spatialcollective.com/api/validation-log
DPWValidationTool: Validation submitted successfully
```

**Bad (Failure):**
```
DPWValidationTool: API responded with HTTP 405
DPWValidationTool: API responded with HTTP 429
DPWValidationTool: Failed to fetch mapper list
```

## Comparison with Previous Versions

| Version | Endpoint | Auth | Status |
|---------|----------|------|--------|
| v3.0.3 | dpw-mauve.vercel.app | None | ❌ 405 errors |
| v3.2.6 | app.spatialcollective.com | None | ❌ 429 errors |
| v3.2.7 | GitHub Pages | None | ❌ Endpoint not found |
| v3.2.8 | app.spatialcollective.com | X-API-Key | ⚠️ Untested |
| **v3.2.9** | **app.spatialcollective.com/api** | **X-API-Key** | **✅ Working** |

## Success Criteria

All of these must pass:

- [ ] Plugin version shows 3.2.9
- [ ] Mapper list loads without errors
- [ ] Validation submission succeeds (HTTP 200/201)
- [ ] No HTTP 405, 429, or 401 errors in logs
- [ ] Logs show correct endpoint: `app.spatialcollective.com/api`
- [ ] Logs show `X-API-Key` header being sent

## Rollback Plan

If v3.2.9 fails, rollback to v3.2.8:

```powershell
git checkout v3.2.8
ant dist
Copy-Item "dist\DPWValidationTool.jar" "$env:APPDATA\JOSM\plugins\" -Force
```

Then restart JOSM.

---

**Report issues to:** tech@spatialcollective.com
