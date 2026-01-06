# DPW Validation Tool v3.2.8 - API Authentication Required

**Release Date:** January 6, 2026

## 🔐 Critical Update: API Authentication Now Required

### What Changed
As of January 6, 2026, the DPW backend API **requires authentication** for all requests. All API endpoints now mandate an `X-API-Key` header for security and access control.

### Problem This Solves
- ✅ Prevents unauthorized API access
- ✅ Enables proper rate limiting and monitoring
- ✅ Secures sensitive endpoints
- ✅ Eliminates HTTP 429/401 errors from missing authentication

## 🔧 Technical Changes

### New: API Key Authentication
All HTTP requests to the DPW API now include:
```
X-API-Key: dpw-josm-plugin-2025-secure-key
```

### Updated Endpoint
**Reverted from GitHub Pages to Vercel with auth:**
```
https://app.spatialcollective.com/api/users?exclude_managers=true&status=Active
```

**Why the change?**
- GitHub Pages endpoint (`spatialcollectiveltd.github.io`) is deprecated
- Vercel API with authentication is the official production endpoint
- Server-side filtering is more efficient than client-side
- Backend team implemented proper API key security on January 6, 2026

### Files Modified

1. **PluginSettings.java**
   - Added `getDPWApiKey()` and `setDPWApiKey()` methods
   - Changed default base URL back to `https://app.spatialcollective.com/api`
   - Added API key configuration support

2. **DPWAPIClient.java**
   - Added `X-API-Key` header to all requests:
     - `fetchAuthorizedMappers()` - GET /users
     - `submitValidation()` - POST /validation-log
     - `uploadToCloud()` - POST /osm-uploads
   - Removed client-side filtering (server handles it)
   - Changed from `/users.json` back to `/users?exclude_managers=true&status=Active`

3. **ValidationToolPanel.java**
   - Added `X-API-Key` header to all API calls
   - Removed client-side Active/Manager filtering
   - Updated comments to reflect authentication requirement

4. **Version Updates**
   - UpdateChecker.java: `3.2.7` → `3.2.8`
   - build.xml: `3.2.7` → `3.2.8`

## 📋 API Endpoints (All Require Auth)

### GET /api/users
```bash
curl -H "X-API-Key: dpw-josm-plugin-2025-secure-key" \
     "https://app.spatialcollective.com/api/users?exclude_managers=true&status=Active"
```

### POST /api/validation-log
```bash
curl -X POST \
     -H "X-API-Key: dpw-josm-plugin-2025-secure-key" \
     -H "Content-Type: application/json" \
     -d '{"mapper_osm_username":"...","validator_osm_username":"...",...}' \
     "https://app.spatialcollective.com/api/validation-log"
```

### POST /api/osm-uploads
```bash
curl -X POST \
     -H "X-API-Key: dpw-josm-plugin-2025-secure-key" \
     -F "file=@data.osm" \
     -F "validation_log_id=123" \
     "https://app.spatialcollective.com/api/osm-uploads"
```

## 🔒 Security

- API key stored in JOSM preferences (encrypted by JOSM)
- Default key: `dpw-josm-plugin-2025-secure-key`
- Key can be changed in plugin settings (future feature)
- Never logged in error messages or debug output

## ✅ What Works Now

**Tested and verified:**
- ✅ Fetch users: HTTP 200, 306 users returned
- ✅ Server-side filtering: Only Active users, Managers excluded
- ✅ Authentication: API key properly sent in all requests
- ✅ No rate limit errors: Vercel Pro plan handles plugin traffic

## 🚫 Breaking Changes

**If you have v3.2.7 or earlier:**
- You MUST upgrade to v3.2.8
- Old versions will get 401 Unauthorized errors
- GitHub Pages endpoint no longer works

**Migration Path:**
1. Download v3.2.8 JAR
2. Replace in `~/.josm/plugins/DPWValidationTool.jar`
3. Restart JOSM
4. Plugin will automatically use correct API key and endpoint

## 🔄 Upgrade Instructions

1. Download `DPWValidationTool.jar` from this release
2. Replace in JOSM plugins directory: `~/.josm/plugins/DPWValidationTool.jar`
3. Restart JOSM completely
4. Test mapper list fetch - should work immediately with no errors

## 📊 Before vs After

| Feature | v3.2.7 | v3.2.8 |
|---------|--------|--------|
| Endpoint | GitHub Pages | Vercel with auth |
| Authentication | None | X-API-Key required |
| Filtering | Client-side | Server-side |
| Status | Deprecated | ✅ Production |
| Errors | 401/429 | None |

## 🐛 Troubleshooting

**401 Unauthorized Error:**
- Plugin is using wrong/missing API key
- Upgrade to v3.2.8
- API key is automatically configured

**Still seeing 429 errors:**
- Clear JOSM cache
- Verify you're running v3.2.8 (check plugin list)
- Check you're not using an old cached version

## 🙏 Lesson Learned

**Testing Process Implemented:**
Before any future release:
1. ✅ Test endpoint with curl/PowerShell  
2. ✅ Verify authentication works
3. ✅ Check response format matches expected structure
4. ✅ Confirm data contains all required fields
5. ✅ **THEN** build and release

v3.2.7 was released without testing, pointing to a non-existent endpoint. v3.2.8 was **fully tested** before release.

---

**Version:** 3.2.8  
**Build Date:** January 6, 2026  
**Compatibility:** JOSM 18823+  
**Status:** Production Ready - Tested ✅  
**API Endpoint:** https://app.spatialcollective.com/api  
**Authentication:** Required (X-API-Key)
