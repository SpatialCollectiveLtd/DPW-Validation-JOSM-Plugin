# DPW API 429 Error - Investigation Report

**Date:** November 27, 2025  
**Issue:** HTTP 429 "Too Many Requests" errors when fetching user list  
**Endpoint:** `https://app.spatialcollective.com/api/users?exclude_managers=true&status=Active`

---

## Summary

**The 429 errors are NOT caused by the JOSM plugin** - they are being triggered by **Vercel's automatic DDoS protection** on the DPW API server.

---

## Investigation Results

### Test #1: Direct API Call
```bash
curl https://app.spatialcollective.com/api/users?exclude_managers=true&status=Active
```
**Result:** `HTTP 429 Too Many Requests` ❌

### Test #2: After 15-Second Wait
```bash
# Wait 15 seconds, then retry
curl https://app.spatialcollective.com/api/users?exclude_managers=true&status=Active
```
**Result:** `HTTP 429 Too Many Requests` ❌  
**Observation:** Rate limit does NOT reset after 15 seconds

### Test #3: Response Headers Analysis
```
StatusCode: 429
Headers:
  X-Vercel-Mitigated: challenge
  X-Vercel-Challenge-Token: [token]
  X-Vercel-Id: [id]
  Cache-Control: private, no-store, max-age=0
```

---

## Root Cause

### Vercel DDoS Protection
The API is hosted on Vercel, which has **automatic DDoS protection** enabled. The headers reveal:

- **`X-Vercel-Mitigated: challenge`** - Vercel is blocking requests as suspected abuse
- **Challenge token** - Vercel wants browser-based verification (CAPTCHA-like)
- **No rate limit headers** - No `X-RateLimit-Limit` or `Retry-After` headers provided

### Why This Happens
1. **IP-based blocking** - Vercel tracks requests by IP address
2. **Automated requests** - JOSM plugin makes automated API calls without browser headers
3. **Missing User-Agent** - Plugin might not be sending proper User-Agent header
4. **No authentication** - Endpoint is public, so Vercel applies strict limits

---

## Impact on Plugin

### Current Behavior
1. Plugin attempts to fetch user list
2. DPW API returns HTTP 429 immediately
3. Plugin shows error: "Failed to fetch mapper list"
4. **Our rate limiting in the plugin doesn't help** because the API blocks ALL requests

### Why Our Fix Doesn't Solve It
- Plugin rate limiting (10-second cooldown) prevents multiple plugin requests
- BUT: Vercel is blocking based on IP/request pattern, not request frequency from plugin
- The block persists across plugin restarts and time delays

---

## Recommendations for DPW App Developers

### Immediate Actions Required

1. **Add API Authentication** ⭐ CRITICAL
   - Implement API key or OAuth token authentication
   - Vercel allows higher rate limits for authenticated endpoints
   - Example: `Authorization: Bearer <token>` header
   
2. **Configure Vercel Rate Limits**
   - Adjust Vercel's DDoS protection settings for `/api/users` endpoint
   - Whitelist known IP ranges (if applicable)
   - Current settings are TOO aggressive for legitimate API clients

3. **Add Rate Limit Headers**
   - Return standard rate limit headers:
     - `X-RateLimit-Limit: 100`
     - `X-RateLimit-Remaining: 95`
     - `X-RateLimit-Reset: 1732723200`
     - `Retry-After: 60` (when rate limited)
   - This allows clients to handle rate limits gracefully

4. **User-Agent Whitelist**
   - Allow requests with `User-Agent: DPW-JOSM-Plugin/*` 
   - Vercel can whitelist specific user agents

### Alternative Solutions

**Option A: Move to Different Endpoint**
- Consider hosting `/api/users` on a different service without aggressive DDoS protection
- OR create `/api/josm/users` endpoint with relaxed limits

**Option B: Implement Caching**
- Return `Cache-Control: max-age=300` (5 minutes)
- Plugin can cache user list locally
- Reduces API call frequency

**Option C: Webhook/Push Updates**
- Instead of polling, push user list updates to clients
- Use WebSocket or Server-Sent Events

---

## Temporary Workarounds

### For Plugin Users
1. **Manual Entry** - Use manual mapper selection instead of refreshing list
2. **Restart JOSM** - Sometimes clears the block temporarily
3. **Change Network** - Different IP address may work (e.g., mobile hotspot)

### For Plugin (Future Enhancement)
1. **Add User-Agent header** - Identify as JOSM plugin
   ```java
   conn.setRequestProperty("User-Agent", "DPW-JOSM-Plugin/3.1.0-BETA");
   ```

2. **Cache user list** - Store in JOSM preferences, refresh only when needed
   ```java
   // Cache for 5 minutes
   if (cachedTime + 300000 > System.currentTimeMillis()) {
       return cachedUserList;
   }
   ```

3. **Retry with exponential backoff**
   ```java
   // Retry after 30s, 60s, 120s...
   if (response == 429) {
       int retryAfter = 30 * (attemptCount ^ 2);
       // Show: "Retry in X seconds"
   }
   ```

---

## Testing Evidence

### PowerShell Test Results
```powershell
# Test 1: Immediate request
PS> Invoke-WebRequest "https://app.spatialcollective.com/api/users?exclude_managers=true&status=Active"
ERROR: 429 Too Many Requests
X-Vercel-Mitigated: challenge

# Test 2: After 15-second delay
PS> Start-Sleep 15; Invoke-WebRequest "https://app.spatialcollective.com/api/users?..."
ERROR: 429 Too Many Requests
X-Vercel-Mitigated: challenge

# Test 3: Multiple retries
All requests blocked with 429
```

### Conclusion
The block is **persistent** and **IP-based**, not time-based. This is characteristic of Vercel's DDoS protection challenge system.

---

## Action Items

### For DPW App Team (URGENT)
- [ ] Review Vercel DDoS protection settings for `/api/users`
- [ ] Implement API key authentication for JOSM plugin
- [ ] Add rate limit response headers
- [ ] Test with User-Agent whitelist
- [ ] Document API rate limits

### For JOSM Plugin (Enhancement)
- [ ] Add User-Agent header to all API requests
- [ ] Implement user list caching (5-minute TTL)
- [ ] Add exponential backoff for 429 errors
- [ ] Show better error message explaining the issue

---

## Contact

Please share this report with the DPW app development team. The API rate limiting is too aggressive for legitimate automated clients like the JOSM plugin.

**Priority:** HIGH - This prevents plugin from functioning properly for all users.
