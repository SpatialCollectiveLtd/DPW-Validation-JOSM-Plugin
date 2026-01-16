# Build and Test Guide - DPW JOSM Plugin v3.4.0

Complete guide for building, installing, and testing the updated plugin with OAuth authentication.

---

## 📦 Building the Plugin

### Prerequisites
- Apache Ant (1.9.x or later)
- Java JDK 8 or later
- JOSM installed

### Build Commands

```powershell
# Navigate to plugin directory
cd "c:\Users\TECH\Desktop\DPW JOSM Plugin"

# Clean previous builds
ant clean

# Build the plugin
ant dist

# Verify build
ls dist\DPWValidationTool.jar
```

**Expected Output:**
```
Buildfile: C:\Users\TECH\Desktop\DPW JOSM Plugin\build.xml

clean:
   [delete] Deleting directory C:\Users\TECH\Desktop\DPW JOSM Plugin\build

compile:
    [mkdir] Created dir: C:\Users\TECH\Desktop\DPW JOSM Plugin\build
    [javac] Compiling 13 source files to C:\Users\TECH\Desktop\DPW JOSM Plugin\build

dist:
      [jar] Building jar: C:\Users\TECH\Desktop\DPW JOSM Plugin\dist\DPWValidationTool.jar

BUILD SUCCESSFUL
Total time: 5 seconds
```

### Build Issues?

#### "Cannot find JOSM jar"
```powershell
# Check build.xml for JOSM path
# Update if needed:
# <property name="josm" value="C:/Program Files/JOSM/josm-custom.jar"/>
```

#### "Source incompatible with target 1.8"
```powershell
# Check Java version
java -version

# Should be 1.8+ (Java 8+)
```

#### "Package javax.crypto does not exist"
```powershell
# Ensure using JDK (not JRE)
# JDK includes javax.crypto
```

---

## 📥 Installing the Plugin

### Method 1: Manual Installation (Recommended for Testing)

```powershell
# Copy to JOSM plugins folder
copy dist\DPWValidationTool.jar $env:APPDATA\JOSM\plugins\

# Restart JOSM
```

### Method 2: JOSM Plugin Manager

1. Build the plugin (see above)
2. Open JOSM
3. Edit → Preferences → Plugins
4. Click "Load from file..."
5. Select `dist\DPWValidationTool.jar`
6. Click "OK"
7. Restart JOSM

### Verify Installation

After restarting JOSM:
1. Tools menu should show "DPW Validation Tool" submenu
2. Submenu should have:
   - Open Validation Panel
   - ────────────────
   - **Authenticate...** ← NEW
   - Settings...
   - Check for Updates...

---

## 🔧 Configuration

### Step 1: Configure Server Settings

1. Open JOSM
2. Tools → DPW Validation Tool → Settings
3. Scroll to **"OSM Server Configuration"** section
4. Check ☑ **"Use Custom OSM Server"**
5. Click **"📋 Apply Spatial Collective Configuration"**
6. Verify URLs are set:
   - Server URL: `https://osm.spatialcollective.co.ke`
   - API URL: `https://osm.spatialcollective.co.ke/api`
   - OAuth Authorize: `https://osm.spatialcollective.co.ke/oauth2/authorize`
   - OAuth Token: `https://osm.spatialcollective.co.ke/oauth2/token`
7. Click **"Save"** or **"OK"**

### Step 2: Verify Configuration

```powershell
# Check JOSM preferences file
cat $env:APPDATA\JOSM\preferences.xml | Select-String -Pattern "dpw.osm"

# Should show:
# <tag key='dpw.osm.use_custom_server' value='true'/>
# <tag key='dpw.osm.server_url' value='https://osm.spatialcollective.co.ke'/>
# ... etc
```

---

## 🧪 Testing Authentication

### Test 1: Initial Authentication

1. **Open Authentication Dialog**
   - Tools → DPW Validation Tool → Authenticate

2. **Verify Initial State**
   - Status: ❌ Not Authenticated
   - Username: (none)
   - Login button enabled
   - Logout button disabled

3. **Click "🔓 Login"**
   - Dialog shows: "🔄 Authenticating..."
   - Progress bar appears
   - Browser opens automatically

4. **Browser Authentication**
   - Should open: `https://osm.spatialcollective.co.ke/oauth2/authorize?...`
   - Login with your OSM credentials
   - Authorize application
   - Browser redirects to: `http://localhost:8111/oauth/callback?code=...`
   - Success page appears: "✅ Authentication Successful!"

5. **Return to JOSM**
   - Dialog should update automatically
   - Status: ✅ Authenticated
   - Username: [your username]
   - Login button disabled
   - Logout button enabled

**Expected Time:** 30-60 seconds

**If Browser Doesn't Open:**
- Check console for URL
- Copy URL and paste in browser manually
- Should still work

### Test 2: Token Persistence

1. **Close JOSM** (File → Exit)
2. **Reopen JOSM**
3. **Open Authentication Dialog**
   - Tools → DPW Validation Tool → Authenticate
4. **Verify**
   - Status: ✅ Authenticated (still!)
   - Username: [your username] (restored!)
   - No re-login needed

**Expected:** Tokens persist across restarts

### Test 3: Logout

1. **Open Authentication Dialog**
2. **Click "🔒 Logout"**
3. **Confirm** logout in dialog
4. **Verify**
   - Status: ❌ Not Authenticated
   - Username: (none)
   - Login button enabled
   - Logout button disabled

**Expected:** Tokens cleared, authentication reset

### Test 4: Validation Workflow

1. **Ensure Authenticated** (Test 1)
2. **Open Validation Panel**
   - Tools → DPW Validation Tool → Open Validation Panel
3. **Select a Mapper**
   - Choose from dropdown
4. **Select Task**
   - Enter task number or select
5. **Click "Record Validation"**
6. **Verify**
   - Should NOT prompt for OSM authentication
   - Should use your custom server username
   - Check console for submission details

**Expected:** Seamless validation using custom OAuth

### Test 5: Switch Between Servers

1. **Configure Custom Server** (enabled)
2. **Authenticate** with custom server
3. **Open Settings**
4. **Uncheck** "Use Custom OSM Server"
5. **Save**
6. **Perform Validation**
   - Should use JOSM's UserIdentityManager
   - Should authenticate with openstreetmap.org

7. **Re-enable Custom Server**
8. **Perform Validation**
   - Should use CustomOAuthClient
   - Should use custom server username

**Expected:** Seamless switching, correct auth method for each

---

## 🐛 Debugging

### Enable Logging

View JOSM console:
- View → Toggle Console (or F4)

All OAuth operations log to console:
```
[DPW] Starting OAuth authentication...
[DPW] Generated code verifier: dBjftJeZ...
[DPW] Generated code challenge: E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM
[DPW] Starting callback server on port 8111...
[DPW] Opening browser to: https://osm.spatialcollective.co.ke/oauth2/authorize?...
[DPW] Received callback: http://localhost:8111/oauth/callback?code=...
[DPW] State parameter validated
[DPW] Exchanging code for token...
[DPW] Token received, expires in 3600 seconds
[DPW] Saving tokens (encrypted)...
[DPW] Fetching user details...
[DPW] Username: john_doe
[DPW] Authentication successful
```

### Common Issues

#### 1. "Cannot open browser"
**Symptom:** Browser doesn't open automatically  
**Cause:** `Desktop.browse()` not supported on your system  
**Fix:** 
- Check console for authorization URL
- Copy and paste into browser manually
- Complete OAuth flow
- Return to JOSM

#### 2. "Port 8111 already in use"
**Symptom:** Callback server fails to start  
**Cause:** JOSM remote control already using port 8111  
**Fix:**
- Close JOSM remote control (Edit → Preferences → Remote Control → uncheck)
- OR: Plugin tries port 8112 automatically
- OR: Restart JOSM

#### 3. "State parameter mismatch"
**Symptom:** Authentication fails after redirect  
**Cause:** Security check failed (CSRF protection)  
**Fix:**
- This is a security feature
- Restart authentication flow
- Don't manipulate callback URL

#### 4. "Token request failed: HTTP 400"
**Symptom:** Cannot exchange authorization code for token  
**Causes:**
- Client ID not registered on server
- Redirect URI mismatch
- Authorization code expired
- PKCE verification failed

**Fix:**
- Check server OAuth configuration (see [SERVER_SETUP_GUIDE.md](SERVER_SETUP_GUIDE.md))
- Verify client ID: `dpw_josm_plugin`
- Verify redirect URIs registered
- Authorization code expires after 10 minutes

#### 5. "Failed to fetch user details"
**Symptom:** Authentication succeeds but username not retrieved  
**Causes:**
- API endpoint incorrect
- Access token doesn't have `read_prefs` scope
- Server API not responding

**Fix:**
- Check API URL in settings
- Verify scope includes `read_prefs`
- Test endpoint manually: `curl -H "Authorization: Bearer TOKEN" https://osm.spatialcollective.co.ke/api/0.6/user/details`

#### 6. "Encryption failed"
**Symptom:** Cannot save or load tokens  
**Cause:** AES encryption error  
**Fix:**
- Check JOSM console for stack trace
- Verify Java Cryptography Extension (JCE) installed
- Clear corrupted tokens: Delete preferences with key starting with `dpw.oauth`

#### 7. "Access token expired"
**Symptom:** Validation fails after some time  
**Cause:** Token expired (normal after 1 hour)  
**Fix:**
- Plugin should auto-refresh
- If not, logout and login again
- Check refresh token hasn't expired (30 days)

---

## 🧪 Test Scenarios Checklist

Use this checklist to ensure all functionality works:

### Authentication Flow
- [ ] Initial login opens browser
- [ ] Browser shows server login page
- [ ] Authorization consent screen appears
- [ ] Callback redirects to localhost
- [ ] Success page appears in browser
- [ ] JOSM dialog updates with username
- [ ] Status shows "Authenticated"

### Token Persistence
- [ ] Close and reopen JOSM
- [ ] Authentication persists
- [ ] Username remembered
- [ ] No re-login needed

### Logout
- [ ] Logout button works
- [ ] Tokens cleared
- [ ] Status shows "Not Authenticated"
- [ ] Can login again

### Validation Workflow
- [ ] Authenticated state detected
- [ ] Username used in validation
- [ ] Submission works
- [ ] No JOSM auth prompt

### Server Switching
- [ ] Can switch to custom server
- [ ] Can switch to public OSM
- [ ] Correct auth method used for each
- [ ] No errors when switching

### Error Handling
- [ ] Browser not opening (manual URL works)
- [ ] Port conflict (fallback port works)
- [ ] Network errors (user-friendly message)
- [ ] Invalid tokens (prompts re-login)

### Security
- [ ] Tokens encrypted in preferences
- [ ] State parameter validated
- [ ] PKCE code verifier/challenge used
- [ ] No secrets in code

---

## 📊 Performance Benchmarks

Expected performance metrics:

| Operation | Expected Time | Notes |
|-----------|---------------|-------|
| Initial authentication | 30-60 seconds | Depends on user login speed |
| Token load from storage | <100ms | Decryption + parsing |
| Token refresh | 1-3 seconds | Network call to server |
| Username fetch | 1-2 seconds | Network call to API |
| Logout | <50ms | Clear tokens from memory |
| Settings apply | <100ms | Save to preferences |

---

## 🎯 Test Environment Setup

### Minimum Requirements
- JOSM 18.11 or later
- Java 8 or later
- Internet connection
- Custom OSM server configured and running

### Recommended Setup
- JOSM latest tested version
- Java 11 or later
- Stable internet connection
- OAuth client registered on server

### Test Data
- Valid OSM account on custom server
- Active mappers in dropdown
- Real tasks for validation

---

## 📝 Test Report Template

Use this template to document your testing:

```markdown
## Test Report - DPW JOSM Plugin v3.4.0

**Date:** [Date]
**Tester:** [Name]
**Environment:**
- JOSM Version: [version]
- Java Version: [version]
- OS: [Windows/Linux/macOS]
- Server: osm.spatialcollective.co.ke

### Test Results

#### Authentication Flow
- [ ] Pass / [ ] Fail
- Notes: ___________

#### Token Persistence
- [ ] Pass / [ ] Fail
- Notes: ___________

#### Logout
- [ ] Pass / [ ] Fail
- Notes: ___________

#### Validation Workflow
- [ ] Pass / [ ] Fail
- Notes: ___________

#### Server Switching
- [ ] Pass / [ ] Fail
- Notes: ___________

### Issues Found
1. ___________
2. ___________

### Overall Assessment
- [ ] Ready for deployment
- [ ] Needs fixes
- [ ] Blocked (explain): ___________

**Recommendation:** ___________
```

---

## 🚀 Deployment Checklist

Before deploying to production:

### Code
- [ ] Built successfully with no errors
- [ ] All source files committed to Git
- [ ] Version number updated (3.4.0)
- [ ] Documentation complete

### Testing
- [ ] All test scenarios passed
- [ ] No critical bugs found
- [ ] Performance acceptable
- [ ] Security verified

### Server
- [ ] OAuth client registered
- [ ] Endpoints accessible
- [ ] Scopes configured
- [ ] Rate limiting configured

### Documentation
- [ ] User guide written
- [ ] Admin guide written
- [ ] Troubleshooting guide written
- [ ] Server setup guide complete

### Deployment
- [ ] JAR file ready
- [ ] Distribution method determined
- [ ] Rollback plan in place
- [ ] Support contact established

---

## 📞 Support

If you encounter issues during testing:

1. **Check JOSM console** (View → Toggle Console)
2. **Review this guide** (common issues section)
3. **Check server logs** (for OAuth errors)
4. **Verify configuration** (settings guide)
5. **Test manually** (curl commands in server guide)

---

**Last Updated:** January 12, 2026  
**Plugin Version:** 3.4.0  
**Status:** Ready for Testing ✅
