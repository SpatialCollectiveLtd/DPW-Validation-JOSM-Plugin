# DPW Validation Tool v3.2.6 - Critical Bug Fix Release

**Release Date:** January 6, 2026

## 🐛 Critical Bug Fixes

### Fixed HTTP 429 Errors in User Fetching
**Issue:** Plugin was receiving HTTP 429 errors when attempting to fetch mapper lists, despite backend APIs being fully functional and unrestricted.

**Root Cause:** Incorrect URL construction causing duplicate `/api` prefixes in API endpoint URLs:
- ❌ Before: `https://app.spatialcollective.com/api/api/users` (double /api)
- ✅ After: `https://app.spatialcollective.com/api/users` (correct)

**Files Fixed:**
1. **DPWAPIClient.java**
   - Fixed `getUserIdByOsmUsername()` - removed duplicate `/api` prefix
   - Fixed `submitValidation()` - removed duplicate `/api` prefix  
   - Fixed `uploadToCloud()` - removed duplicate `/api` prefix and updated endpoint to `/osm-uploads`

2. **ValidationToolPanel.java**
   - Fixed `getUserIdByOsmUsername()` - now uses `PluginSettings.getDPWApiBaseUrl()` instead of deprecated preference key
   - Fixed `uploadToCloud()` - corrected URL construction
   - Fixed `sendPostRequest()` - corrected URL construction
   - Removed all instances of deprecated `Preferences.main().get("dpw.api_base_url")`

**Impact:**
- ✅ Mapper list fetching now works correctly
- ✅ User authorization checks function properly
- ✅ Validation submission endpoints work as expected
- ✅ Cloud upload endpoints aligned with backend API
- ✅ All API calls now use consistent, correct URL patterns

## 🔧 Technical Details

### API Endpoints (Now Correct)
All endpoints now correctly resolve to:
- `GET /api/users?exclude_managers=true&status=Active` - Fetch active users
- `GET /api/users?osm_username={username}` - Lookup user by OSM username
- `POST /api/validation-log` - Submit validation data
- `POST /api/osm-uploads` - Upload OSM files to cloud storage

### Code Quality Improvements
- Centralized API base URL configuration through `PluginSettings.getDPWApiBaseUrl()`
- Removed deprecated direct preference access patterns
- Ensured consistency across all API client methods

## 📋 Verification
Backend verification confirmed all endpoints working:
- ✅ 306 users fetched successfully (167KB response)
- ✅ OSM username lookup working correctly
- ✅ Validation log creation functional
- ✅ File uploads to Google Drive operational
- ✅ No authentication barriers

## 🔄 Upgrade Instructions
1. Download `DPWValidationTool.jar` from this release
2. Place in your JOSM plugins directory (usually `~/.josm/plugins/`)
3. Restart JOSM
4. The plugin will automatically use the corrected API endpoints

## 🙏 Acknowledgments
Special thanks to the DPW backend development team for testing and confirming API endpoint availability, which helped identify this client-side URL construction issue.

---

**Version:** 3.2.6  
**Build Date:** January 6, 2026  
**Compatibility:** JOSM 18823+
