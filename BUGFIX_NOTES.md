# Bug Fix - v3.1.0-BETA (Updated)

## Critical Fix Applied

**Date:** November 27, 2025  
**Commit:** 87835cd

### Issue Fixed
- **HTTP 429 "Too Many Requests" errors** when fetching user list from DPW API
- Plugin was making too many rapid API calls without rate limiting

### Changes
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

### Technical Details
- **Rate limit:** 10 seconds (`MAPPER_FETCH_COOLDOWN = 10000ms`)
- **Implementation:** `lastMapperFetchTime` timestamp check before each fetch
- **Fallback:** Shows informative error instead of crashing

### Testing
To verify the fix:
1. Enable TM Integration in settings
2. Try refreshing mapper list multiple times quickly
3. Should see rate limit message after first fetch
4. Wait 10 seconds, then refresh works again

### Files Modified
- `src/org/openstreetmap/josm/plugins/dpwvalidationtool/ValidationToolPanel.java`
  - Lines 85-86: Rate limiting variables
  - Lines 944-970: Updated `fetchAuthorizedMappers()` method

### For DPW App Developers
The 429 errors indicate the `/api/users` endpoint has rate limiting enabled. Consider:
- Documenting rate limits in API documentation
- Adding `X-RateLimit-*` headers in responses
- Allowing higher limits for authenticated requests if needed
