# Release v3.2.9 - API Endpoint & Authentication Fix

**Release Date:** January 2025  
**Status:** Stable  
**Base Version:** 3.0.3 (restored and fixed)

## Overview

This release restores the stable v3.0.3 codebase and applies critical fixes to address API endpoint changes and authentication requirements. This version resolves HTTP 405 errors on validation submission and HTTP 429 rate limiting errors.

## What Changed

### API Endpoint Updates
- **Updated base URL:** Changed from `https://dpw-mauve.vercel.app` to `https://app.spatialcollective.com/api`
- **Fixed URL construction:** Removed duplicate `/api` prefix in all endpoints
- **Affected endpoints:**
  - `/users` - Mapper lookup
  - `/validation-log` - Validation submission
  - `/osm-uploads` - File upload

### Authentication
- **Added API key authentication** to all HTTP requests
- **Header:** `X-API-Key: dpw-josm-plugin-2025-secure-key`
- **Applied to:**
  - User lookup requests (GET /users)
  - Validation submissions (POST /validation-log)
  - OSM file uploads (POST /osm-uploads)

## Issues Fixed

- ✅ **HTTP 405 (Method Not Allowed)** - Fixed by updating to correct API endpoint
- ✅ **HTTP 429 (Too Many Requests)** - Fixed by adding authentication headers
- ✅ **Validation submission failures** - Now works with new backend API
- ✅ **Mapper list fetch errors** - Resolved with proper endpoint and auth

## Technical Changes

### Modified Files

1. **ValidationToolPanel.java**
   - Line 1664: Updated user lookup endpoint and added auth
   - Line 1733: Updated OSM upload endpoint and added auth  
   - Line 2566: Updated validation log endpoint and added auth (fixes 405 error)

2. **UpdateChecker.java**
   - Updated version to 3.2.9

3. **build.xml**
   - Updated version to 3.2.9

## Why v3.0.3 Base?

v3.0.3 was the last known working version with stable UI and workflow logic. Rather than debugging the issues in v3.2.6-v3.2.8 (which had untested endpoints and other problems), we restored to the proven v3.0.3 codebase and applied only the necessary endpoint and authentication fixes.

## Testing Checklist

Before using this release, verify:

- [ ] Mapper list loads successfully in the dropdown
- [ ] Validation submission completes without errors
- [ ] OSM file upload works (if applicable)
- [ ] No HTTP 405, 429, or 401 errors in logs
- [ ] API endpoint is reachable at https://app.spatialcollective.com/api

## Installation

1. Download `DPWValidationTool.jar` from the release
2. Copy to your JOSM plugins directory:
   - **Windows:** `%APPDATA%\JOSM\plugins\`
   - **Linux:** `~/.local/share/JOSM/plugins/`
   - **macOS:** `~/Library/JOSM/plugins/`
3. Restart JOSM
4. Verify version 3.2.9 appears in the plugin list

## API Endpoint Configuration

Default endpoint: `https://app.spatialcollective.com/api`

To override (if needed):
1. Open JOSM Preferences
2. Navigate to DPW Validation Tool settings
3. Change "API Base URL" field
4. Restart JOSM

## Rollback Instructions

If you encounter issues, you can rollback to v3.0.3:

```bash
git checkout v3.0.3
ant dist
# Install the built JAR
```

**Note:** v3.0.3 will have the original endpoint issues (405 errors).

## Known Limitations

- API key is hardcoded in the plugin source code
- Future enhancement: Move API key to user settings for security

## Migration from Previous Versions

### From v3.2.8, v3.2.7, v3.2.6
- Simply install v3.2.9 - no configuration changes needed
- Old endpoint URLs will be ignored
- Authentication is now automatic

### From v3.0.3 or earlier
- Install v3.2.9
- No manual configuration required
- Endpoints and authentication will work automatically

## Verification

After installation, check JOSM logs for:

```
DPWValidationTool: Fetching user_id for: <username>
DPWValidationTool: API responded with HTTP 200
```

If you see HTTP 405, 429, or 401 - the installation may have failed.

## Support

For issues or questions:
- GitHub Issues: https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/issues
- Email: tech@spatialcollective.com

---

**Build Information**
- Java Compiler: JDK 11+
- JOSM API Version: 18823
- Build Tool: Apache Ant
- Build Command: `ant dist`
