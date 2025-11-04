# DPW JOSM Plugin - API Update v2.1

## Date: October 23, 2025

## Summary
Updated the DPW JOSM Plugin to comply with the new API specification from the DPW application development team.

---

## Changes Implemented

### 1. **Updated Authorization Endpoint** ✅

**Previous (v2.0):**
```
GET /api/users?role=Digitizer,Validator&status=Active
```

**New (v2.1):**
```
GET /api/users?exclude_managers=true&status=Active
```

**Reason:** 
- Security requirement: `exclude_managers=true` prevents exposing Manager/Admin accounts
- Simplified filtering: API now returns all authorized users (Digitizers and Validators) by default when managers are excluded
- Per integration guide: "SECURITY CRITICAL - ALWAYS include exclude_managers=true"

**Code Location:** `fetchAuthorizedMappers()` method in `ValidationToolPanel.java`

---

### 2. **Error Field Names** ✅

**Status:** Already compliant with API specification

The plugin already correctly maps error types to API field names:

| Plugin Error Type | API Field Name |
|-------------------|----------------|
| Hanging Nodes | `error_hanging_nodes` |
| Overlapping Buildings | `error_overlapping_buildings` |
| Buildings Crossing Highway | `error_buildings_crossing_highway` |
| Missing Tags | `error_missing_tags` |
| Improper Tags | `error_improper_tags` |
| Features Misidentified | `error_features_misidentified` |
| Missing Buildings | `error_missing_buildings` |
| Building Inside Building | `error_building_inside_building` |
| Building Crossing Residential | `error_building_crossing_residential` |
| Improperly Drawn | `error_improperly_drawn` |

**Code:** The plugin uses `errorTypes[i].toLowerCase().replace(' ', '_')` which automatically generates the correct field names.

---

### 3. **Validation Log Submission** ✅

**Status:** Already compliant with API specification

The plugin already sends the correct JSON structure:

```json
{
  "task_id": "string (optional)",
  "mapper_osm_username": "string (REQUIRED)",
  "validator_osm_username": "string (REQUIRED)",
  "settlement": "string (optional)",
  "total_buildings": 123 (REQUIRED),
  "error_hanging_nodes": 0,
  "error_overlapping_buildings": 0,
  // ... other error counts ...
  "validation_status": "Validated|Rejected",
  "validator_comments": "string (optional)"
}
```

**Code Location:** `submitData()` method in `ValidationToolPanel.java`

---

## API Endpoints Used

### Base URL
```
https://dpw-mauve.vercel.app
```

### Endpoints

1. **GET /api/users** - Fetch authorized users
   - Query Parameters:
     - `exclude_managers=true` (REQUIRED for security)
     - `status=Active` (recommended)
   - Response: Array of user objects with `osm_username` field

2. **POST /api/validation-log** - Submit validation results
   - Content-Type: `application/json`
   - Required fields: `mapper_osm_username`, `validator_osm_username`, `total_buildings`
   - Response: Success with `log_id` or error message

---

## Key Integration Points

### Authentication Flow
1. Plugin uses JOSM OAuth 2.0 to get validator's OSM username
2. Checks if validator is in authorized users list (from `/api/users`)
3. Proceeds only if authorized

### Terminology Mapping

| Plugin Term | API/Database Term | Field Name |
|-------------|-------------------|------------|
| Mapper | Digitizer | `mapper_osm_username` |
| Validator | Validator | `validator_osm_username` |

**Important:** The API expects OSM usernames, NOT database user IDs.

---

## Error Handling

### User Not Found (404)
```json
{
  "success": false,
  "error": "Mapper with OSM username 'xyz' not found in Users table"
}
```

### Missing Required Fields (400)
```json
{
  "success": false,
  "error": "Missing required fields: mapper_osm_username, validator_osm_username, total_buildings"
}
```

### Success (201)
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

---

## Testing Checklist

- [x] Build successful (ant clean dist)
- [ ] Test authorization endpoint in live JOSM
- [ ] Verify authorized users are loaded
- [ ] Test validation log submission
- [ ] Verify no Manager accounts are exposed
- [ ] Confirm error messages are clear

---

## Files Modified

1. `src/org/openstreetmap/josm/plugins/dpwvalidationtool/ValidationToolPanel.java`
   - Updated `fetchAuthorizedMappers()` method
   - Changed API endpoint from `role=Digitizer,Validator` to `exclude_managers=true`
   - Updated version references from v2.0 to v2.1

2. `build.xml`
   - Version updated to 2.1.0
   - Plugin description updated

3. `README.md`
   - Added v2.1 changelog
   - Updated documentation

---

## Next Steps

1. **Install plugin in JOSM:**
   ```powershell
   $josmPlugins = "$env:USERPROFILE\.josm\plugins"
   Copy-Item "dist\DPWValidationTool.jar" -Destination $josmPlugins
   ```

2. **Test in JOSM:**
   - Open JOSM
   - Authenticate with OSM (OAuth 2.0)
   - Open DPW Validation Tool from Data menu
   - Verify authorized users load
   - Test validation submission

3. **Monitor for issues:**
   - Check JOSM console for errors
   - Verify API responses in logs
   - Confirm no Manager accounts appear

---

## References

- Integration Guide: `JOSM_PLUGIN_INTEGRATION_GUIDE.md`
- API Base URL: `https://dpw-mauve.vercel.app`
- Plugin Version: 2.1.0
- Build Date: October 23, 2025

---

## Support

For API issues or questions, contact the DPW application development team with reference to this document and the integration guide.
