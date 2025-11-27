# Bug Fix & Feature Updates - v3.1.0-BETA

## Updates Applied

**Date:** November 27, 2025  
**Commits:** 87835cd, 84589d1, 526c66e

---

## Feature #1: Auto-Install Updates (Commit: 526c66e)

**Feature Added**
- **One-click automatic plugin updates** - No more manual JAR downloads!
- Users can now install updates directly from within JOSM with a single click

**How It Works**
1. User clicks "Check for Updates" in settings
2. If update available, dialog shows with three options:
   - **Install Update** (NEW!) - Downloads and installs automatically
   - **Download Manually** - Opens GitHub releases in browser
   - **Remind Me Later** - Dismisses the dialog
3. If user clicks "Install Update":
   - Downloads JAR from GitHub releases
   - Shows progress bar with MB/percentage
   - Backs up current plugin (DPWValidationTool.jar.bak)
   - Replaces old JAR with new one
   - Prompts to restart JOSM
   - Deletes backup on success

**Technical Implementation**
- **Download:** Uses HttpURLConnection to download from GitHub release assets
- **Progress:** Real-time progress bar showing bytes downloaded and percentage
- **Safety:** Creates backup before replacing, deletes on success
- **Location:** Installs to `%JOSM_HOME%/plugins/DPWValidationTool.jar`
- **Error Handling:** Falls back to manual download on any error
- **Thread Safety:** Downloads in background thread, UI updates via SwingUtilities

**Benefits**
- No need to manually download JAR files
- No need to navigate to JOSM plugins folder
- Automatic backup ensures safe updates
- Progress tracking keeps users informed
- Seamless experience - just one click!

**Files Modified**
- `src/org/openstreetmap/josm/plugins/dpwvalidationtool/UpdateChecker.java`
  - Added imports: `FileOutputStream`, `Files`, `Path`, `Paths`, `StandardCopyOption`
  - New method: `installUpdate()` - Downloads and installs JAR
  - Updated: `showUpdateAvailableDialog()` - Added "Install Update" button
  - Progress dialog with download tracking and cancellation

---

## Fix #2: Update Checker Not Detecting Beta Releases (Commit: 84589d1)

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

**Files Modified**
- `src/org/openstreetmap/josm/plugins/dpwvalidationtool/UpdateChecker.java`
  - Line 18: Changed API endpoint to include all releases
  - Lines 64-102: New `findLatestRelease()` method
  - Lines 130-164: Improved `isNewerVersion()` with BETA handling

---

## Fix #3: HTTP 429 API Rate Limiting (Commit: 87835cd)

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

**Files Modified**
- `src/org/openstreetmap/josm/plugins/dpwvalidationtool/ValidationToolPanel.java`
  - Lines 85-86: Rate limiting variables
  - Lines 944-970: Updated `fetchAuthorizedMappers()` method

---

## Testing Instructions

### Test Auto-Install Updates:
1. Install v3.1.0-beta plugin
2. Click "Check for Updates" in settings
3. Click "Install Update" button
4. Verify progress bar shows download progress
5. Wait for download to complete
6. Verify success message and restart prompt
7. Restart JOSM
8. Verify plugin updated successfully

### Test Rate Limiting:
1. Enable TM Integration in settings
2. Try refreshing mapper list multiple times quickly
3. Should see rate limit message after first fetch
4. Wait 10 seconds, then refresh works again

### Test Update Checker:
1. Click "Check for Updates" in settings
2. Should now detect v3.1.0-beta as available (if on older version)
3. For users already on v3.1.0-beta, will show update to ensure latest JAR

---

## For DPW App Developers

**API Rate Limiting:**
The 429 errors indicate the `/api/users` endpoint has rate limiting enabled. Consider:
- Documenting rate limits in API documentation
- Adding `X-RateLimit-*` headers in responses
- Allowing higher limits for authenticated requests if needed

**Update Mechanism:**
The auto-install feature downloads directly from GitHub releases. Ensure:
- Release assets are always available
- JAR files are properly attached to releases
- Asset URLs remain stable
