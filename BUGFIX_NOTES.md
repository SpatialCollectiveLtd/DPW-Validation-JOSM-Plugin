# Bug Fix - v3.1.0-BETA (Updated)

## Critical Fixes Applied

**Date:** November 27, 2025  
**Commits:** 87835cd, 84589d1

### Fix #1: HTTP 429 API Rate Limiting (Commit: 87835cd)

**Issue Fixed**
- **HTTP 429 "Too Many Requests" errors** when fetching user list from DPW API
- Plugin was making too many rapid API calls without rate limiting

**Changes**
1. **Rate Limiting Implemented**
   - Added 10-second cooldown between user list fetches
   - Prevents rapid-fire API calls that trigger 429 errors
   - Shows user-friendly message: "Rate limit: Please wait X seconds before refreshing"

2. **API URL Updated**
   - Now uses `PluginSettings.getDPWApiBaseUrl()` instead of hardcoded old URL
   - Points to production API: `app.spatialcollective.com/api`
   - Fully configurable via settings panel

3. **Debug Logging Added**
   - Logs rate limit events for troubleshooting
   - Better visibility into API call patterns

**Technical Details**
- **Rate limit:** 10 seconds (`MAPPER_FETCH_COOLDOWN = 10000ms`)
- **Implementation:** `lastMapperFetchTime` timestamp check before each fetch
- **Fallback:** Shows informative error instead of crashing

### Fix #2: Update Checker Not Detecting Beta Releases (Commit: 84589d1)

**Issue Fixed**
- Users clicking "Check for Updates" were told "already up to date"
- Update checker was using GitHub `/releases/latest` endpoint which excludes pre-releases
- v3.1.0-beta is marked as pre-release, so it was invisible to the update checker

**Changes**
1. **Updated GitHub API Endpoint**
   - Changed from `/releases/latest` to `/releases`
   - Now fetches ALL releases including pre-releases and beta versions

2. **Improved Version Detection**
   - Parses release array to find most recent version
   - Includes beta, alpha, and pre-release versions

3. **Smart BETA Update Logic**
   - For BETA versions with same version number, always shows update notification
   - Ensures users get latest JAR updates even within same version
   - Example: v3.1.0-BETA with updated JAR will notify users on v3.1.0-BETA

**Technical Details**
- **API Change:** `GITHUB_API_URL` now points to `/releases` instead of `/releases/latest`
- **Parser:** `findLatestRelease()` extracts first release from array (most recent)
- **Version Comparison:** Special handling for BETA versions to detect JAR updates

### Testing
To verify the fixes:

**Rate Limiting:**
1. Enable TM Integration in settings
2. Try refreshing mapper list multiple times quickly
3. Should see rate limit message after first fetch
4. Wait 10 seconds, then refresh works again

**Update Checker:**
1. Click "Check for Updates" in settings
2. Should now detect v3.1.0-beta as available (if on older version)
3. For users already on v3.1.0-beta, will show update to ensure latest JAR

### Files Modified
- `src/org/openstreetmap/josm/plugins/dpwvalidationtool/ValidationToolPanel.java`
  - Lines 85-86: Rate limiting variables
  - Lines 944-970: Updated `fetchAuthorizedMappers()` method

- `src/org/openstreetmap/josm/plugins/dpwvalidationtool/UpdateChecker.java`
  - Line 18: Changed API endpoint to include all releases
  - Lines 64-102: New `findLatestRelease()` method
  - Lines 130-164: Improved `isNewerVersion()` with BETA handling

### For DPW App Developers
The 429 errors indicate the `/api/users` endpoint has rate limiting enabled. Consider:
- Documenting rate limits in API documentation
- Adding `X-RateLimit-*` headers in responses
- Allowing higher limits for authenticated requests if needed
