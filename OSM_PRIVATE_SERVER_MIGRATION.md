# OSM Private Server Migration Guide

**Date:** January 12, 2026  
**Plugin Version:** 3.4.0  
**Target Server:** osm.spatialcollective.co.ke

---

## Overview

This document explains how to configure the DPW Validation Tool to work with your private OSM server at **osm.spatialcollective.co.ke** instead of the public openstreetmap.org.

## Changes Made (v3.4.0)

### 1. New File: `OSMServerConfiguration.java`

This class manages custom OSM server settings including:
- OSM Server URL
- API Endpoint URL
- OAuth Authorization URL
- OAuth Token URL

**Features:**
- ✅ Stores configuration in JOSM preferences
- ✅ Supports switching between public OSM and private server
- ✅ One-click Spatial Collective configuration
- ✅ Validates and persists settings

### 2. Enhanced `SettingsPanel.java`

Added new configuration section:
- **OSM Server Configuration (Advanced)** section
- Checkbox to enable/disable custom server
- Input fields for all server URLs
- "Apply Spatial Collective Configuration" button for quick setup
- Warning messages about advanced features

### 3. Configuration Settings

The following settings are now configurable:

| Setting | Default | Your Private Server |
|---------|---------|---------------------|
| OSM Server URL | https://www.openstreetmap.org | https://osm.spatialcollective.co.ke |
| API Endpoint | https://api.openstreetmap.org/api | https://osm.spatialcollective.co.ke/api |
| OAuth Authorization | https://www.openstreetmap.org/oauth2/authorize | https://osm.spatialcollective.co.ke/oauth2/authorize |
| OAuth Token | https://www.openstreetmap.org/oauth2/token | https://osm.spatialcollective.co.ke/oauth2/token |

---

## Installation & Configuration

### Step 1: Update the Plugin

1. Copy the new files to your plugin directory:
   - `OSMServerConfiguration.java`
   - Updated `SettingsPanel.java`

2. Rebuild the plugin:
   ```bash
   ant clean
   ant dist
   ```

3. Install in JOSM:
   - Copy `dist/DPWValidationTool.jar` to JOSM plugins folder
   - Restart JOSM

### Step 2: Configure Custom Server

1. **Open Settings:**
   - In JOSM, go to: **Tools → DPW Validation Tool → Settings**

2. **Scroll to "OSM Server Configuration (Advanced)"**

3. **Quick Setup (Recommended):**
   - Check ☑ "Use Custom OSM Server"
   - Click **"📋 Apply Spatial Collective Configuration"**
   - This auto-fills all URLs for osm.spatialcollective.co.ke
   - Click **"Save"**

4. **Manual Setup (Alternative):**
   - Check ☑ "Use Custom OSM Server"
   - Fill in each field:
     * OSM Server URL: `https://osm.spatialcollective.co.ke`
     * API Endpoint URL: `https://osm.spatialcollective.co.ke/api`
     * OAuth Authorization URL: `https://osm.spatialcollective.co.ke/oauth2/authorize`
     * OAuth Token URL: `https://osm.spatialcollective.co.ke/oauth2/token`
   - Click **"Save"**

### Step 3: Verify Configuration

Open JOSM console and check for:
```
DPW: Using custom OSM server: https://osm.spatialcollective.co.ke
```

---

## Important Notes

### ⚠️ Authentication Limitations

**CURRENT LIMITATION:**  
The plugin currently uses JOSM's built-in `UserIdentityManager` which is hardcoded to openstreetmap.org. Simply changing these URLs in settings **will NOT** automatically enable authentication with your private server.

### 🔨 Additional Work Required

To fully enable authentication with your private OSM server, you need to implement **custom OAuth 2.0 authentication**. This requires:

1. **Create `CustomOAuthClient.java`** (not yet implemented)
   - Implement OAuth 2.0 PKCE flow
   - Handle token storage and refresh
   - Manage authentication state

2. **Update `ValidationToolPanel.java`**
   - Replace `UserIdentityManager.getInstance()` calls
   - Use `CustomOAuthClient` instead
   - Handle custom server authentication

3. **Create OAuth Callback Handler**
   - Local HTTP server to receive OAuth callback
   - Exchange authorization code for tokens
   - Secure token storage

### Recommended Approach

**Option A: Plugin-Level Authentication (RECOMMENDED)**
- Implement custom OAuth client within the plugin
- Users authenticate separately in the plugin
- More flexible and independent from JOSM

**Option B: JOSM-Level Configuration**
- Configure JOSM itself to connect to your server
- Edit → Preferences → Connection Settings
- Change "OSM Server URL" to your server
- ⚠️ Affects ALL JOSM functionality, not just your plugin

**Option C: Hybrid Approach (BEST)**
- Keep JOSM connected to openstreetmap.org
- Use custom authentication only for plugin features
- Validators can still contribute to main OSM
- Your validation data uses private server

---

## What Works Now (v3.4.0)

✅ **Configuration Storage**
- Custom server URLs can be saved and loaded
- Settings persist between JOSM sessions
- Easy switching between servers

✅ **UI Integration**
- Settings panel with all fields
- Quick setup button
- Visual feedback

❌ **What Doesn't Work Yet**

- Authentication with private server
- Data upload/download from private server
- OAuth token management

---

## Next Steps for Full Implementation

### 1. Implement Custom OAuth Client

Create `src/org/openstreetmap/josm/plugins/dpwvalidationtool/CustomOAuthClient.java`:

```java
public class CustomOAuthClient {
    private final OSMServerConfiguration config;
    private String accessToken;
    private String refreshToken;
    
    public void authenticate() {
        // 1. Generate PKCE code verifier and challenge
        // 2. Open browser to config.getAuthorizationUrl()
        // 3. Start local server on localhost:8080 for callback
        // 4. Receive authorization code
        // 5. Exchange code for tokens at config.getTokenAcquisitionUrl()
        // 6. Store tokens securely in JOSM preferences
    }
    
    public String getUsername() {
        // Call /api/0.6/user/details on custom server
        // Parse and return username
    }
    
    public boolean isAuthenticated() {
        return accessToken != null && !isTokenExpired();
    }
}
```

### 2. Update ValidationToolPanel.java

Replace:
```java
// OLD:
UserIdentityManager userManager = UserIdentityManager.getInstance();
String username = userManager.getUserName();

// NEW:
if (OSMServerConfiguration.isCustomServerEnabled()) {
    CustomOAuthClient oauth = new CustomOAuthClient(
        OSMServerConfiguration.loadFromPreferences()
    );
    if (!oauth.isAuthenticated()) {
        oauth.authenticate();
    }
    username = oauth.getUsername();
} else {
    // Use JOSM's standard authentication
    UserIdentityManager userManager = UserIdentityManager.getInstance();
    username = userManager.getUserName();
}
```

### 3. Add Authentication Dialog

Create UI for OAuth login:
- Show when custom server is configured
- Button to initiate OAuth flow
- Display authentication status
- Handle errors gracefully

### 4. Secure Token Storage

```java
public class SecureTokenStorage {
    public static void saveToken(String token) {
        // Encrypt token before storing in preferences
        String encrypted = encrypt(token);
        Config.getPref().put("dpw.oauth.token.encrypted", encrypted);
    }
    
    public static String loadToken() {
        String encrypted = Config.getPref().get("dpw.oauth.token.encrypted", "");
        return decrypt(encrypted);
    }
}
```

---

## Testing Checklist

Once custom OAuth is implemented:

- [ ] Can save custom server URLs in settings
- [ ] Can load saved settings after JOSM restart
- [ ] Quick setup button populates all fields correctly
- [ ] Custom authentication initiates OAuth flow
- [ ] Browser opens to correct authorization URL
- [ ] Callback receives authorization code
- [ ] Tokens are exchanged successfully
- [ ] Username is fetched from custom server
- [ ] Tokens are stored encrypted
- [ ] Token refresh works when expired
- [ ] Can switch between custom and public OSM
- [ ] Error handling for network failures
- [ ] Error handling for invalid credentials

---

## Troubleshooting

### "Authentication Failed"
- Verify OAuth URLs are correct
- Check if your OSM server is running
- Verify firewall allows connections
- Check JOSM console for error details

### "Server Not Responding"
- Test server URL in browser
- Verify network connectivity
- Check if server requires VPN
- Verify SSL certificates are valid

### "Invalid OAuth Configuration"
- Verify all URLs use HTTPS
- Check OAuth client is registered on server
- Verify redirect URI matches callback server
- Check server logs for OAuth errors

---

## Security Considerations

### Token Storage
- Tokens are encrypted before storage
- Never log tokens in plain text
- Clear tokens on logout
- Implement token expiry checks

### HTTPS Required
- All URLs must use HTTPS
- Verify SSL certificates
- No plain HTTP allowed

### OAuth Best Practices
- Use PKCE for security
- Implement state parameter for CSRF protection
- Validate redirect URIs
- Refresh tokens securely

---

## Migration Timeline

### Phase 1: Configuration (DONE ✅)
- ✅ Created `OSMServerConfiguration.java`
- ✅ Updated `SettingsPanel.java`
- ✅ Added UI for server configuration
- ✅ Settings persistence

### Phase 2: Authentication (TODO)
- [ ] Implement `CustomOAuthClient.java`
- [ ] Create OAuth callback handler
- [ ] Implement token management
- [ ] Add authentication UI
- Estimated: 2-3 days

### Phase 3: Integration (TODO)
- [ ] Update `ValidationToolPanel.java`
- [ ] Replace `UserIdentityManager` calls
- [ ] Test with private server
- [ ] Error handling
- Estimated: 1-2 days

### Phase 4: Testing & Deployment (TODO)
- [ ] Unit tests
- [ ] Integration tests
- [ ] User acceptance testing
- [ ] Documentation
- Estimated: 1-2 days

**Total Estimated Time:** 4-7 days

---

## Support

For issues or questions:
1. Check JOSM console for error messages
2. Review this migration guide
3. Contact plugin maintainer
4. Check server logs for OAuth errors

---

## Summary

**What's Been Done (v3.4.0):**
- ✅ Infrastructure for custom server configuration
- ✅ UI for easy setup
- ✅ Quick configuration button
- ✅ Settings persistence

**What's Needed Next:**
- Custom OAuth implementation
- Token management
- Integration with validation workflow
- Testing and deployment

**Recommended Action:**
Implement Phase 2 (Authentication) to enable full functionality with your private OSM server.

---

**Version:** 3.4.0  
**Last Updated:** January 12, 2026  
**Author:** GitHub Copilot
