# DPW Validation Tool - Implementation Plan

## Executive Summary
This document outlines the implementation plan for improving the DPW Validation JOSM Plugin. The critical issue is **OAuth 2.0 authentication** - JOSM uses OAuth 2.0 but our plugin currently doesn't use authentication, which has caused disconnection reports.

---

## CRITICAL ISSUE: OAuth 2.0 Authentication

### Problem
- **User Report**: Connection kept breaking, JOSM reporting disconnection errors when using basic auth
- **Root Cause**: Our plugin currently makes HTTP requests WITHOUT any authentication headers
- **JOSM Behavior**: JOSM uses OAuth 2.0 for OSM API authentication. Our external API calls may trigger JOSM's authentication monitoring

### Solution: Get Current User from JOSM OAuth
Instead of adding authentication to our DPW Manager API (which is separate from OSM), we need to:
1. Get the current JOSM user's OSM username from their OAuth identity
2. Use that username as the validator in submissions
3. This eliminates the need for manual validator input

### JOSM OAuth API Usage
```java
import org.openstreetmap.josm.data.UserIdentityManager;

// Get current JOSM user
UserIdentityManager userManager = UserIdentityManager.getInstance();
String validatorUsername = userManager.getUserName();

// Check if user is authenticated
if (userManager.isAnonymous()) {
    // Show error: "Please authenticate with OSM first"
} else if (userManager.isPartiallyIdentified()) {
    // Have username only
} else if (userManager.isFullyIdentified()) {
    // Have full user info including ID
}
```

**Benefits:**
- No need to manually enter validator name
- Automatic authentication with JOSM's OAuth
- No more disconnection warnings
- Better security - users are automatically validated

---

## Priority 1: Critical Fixes (Est: 2-3 days)

### 1.1 OAuth 2.0 Integration ⚠️ CRITICAL
**File:** `ValidationToolPanel.java`

**Changes:**
1. Remove validator username field (auto-detected from JOSM)
2. Add authentication check before allowing any operations
3. Get current user from `UserIdentityManager`

**Code Location:**
- Line ~150-250: UI construction - remove validator field
- Line ~820-850: `fetchAuthorizedMappers()` - no changes needed
- Line ~950-1010: `submitData()` - auto-populate validator from JOSM
- Add new method: `getCurrentValidator()`

### 1.2 Date Validation Before Isolation ⚠️ HIGH PRIORITY
**File:** `ValidationToolPanel.java`

**Problem:** Users can click "Isolate" without selecting a date, causing errors

**Changes:**
- Line ~400-500: `isolateButton` ActionListener
- Add date validation before allowing isolation
- Show error dialog if date not set

```java
// Pseudo-code
if (dateField.getText().trim().isEmpty() || dateField.getText().equals("YYYY-MM-DD")) {
    JOptionPane.showMessageDialog(null, 
        "Please select a date before isolating work", 
        "Date Required", 
        JOptionPane.ERROR_MESSAGE);
    return;
}
```

### 1.3 Mapper Authorization Check ⚠️ HIGH PRIORITY
**File:** `ValidationToolPanel.java`

**Problem:** Users can isolate data even if they're not authorized for the project

**Changes:**
- Line ~400-500: `isolateButton` ActionListener
- Check if current JOSM user is in `authorizedMappers` list
- Show error if not authorized

```java
// Pseudo-code
String currentUser = UserIdentityManager.getInstance().getUserName();
if (!authorizedMappers.contains(currentUser)) {
    JOptionPane.showMessageDialog(null, 
        "You are not authorized for this project.\nCurrent user: " + currentUser, 
        "Authorization Required", 
        JOptionPane.ERROR_MESSAGE);
    return;
}
```

---

## Priority 2: Workflow Simplification (Est: 1-2 days)

### 2.1 Remove Unnecessary Buttons
**File:** `ValidationToolPanel.java`

**Buttons to Remove:**
1. **"Scan Layers" button** - Automate this when isolation happens
2. **"Force Submit" button** - Remove entirely, make authorization mandatory

**Simplified Workflow:**
1. User selects date (REQUIRED)
2. User clicks "Isolate Work" (auto-checks date + authorization + auto-scans)
3. User reviews isolated data
4. User exports if needed
5. User submits (auto-uses JOSM username as validator)

**UI Layout:**
```
[Task ID Field] [Fetch Button]
[Date Picker] (REQUIRED)
[Mapper Name Field] (read-only, shows current JOSM user)
[Authorization Status Label]

[Isolate Work Button] (checks date + auth)
[Export OSM File Button]
[Submit Validation Button]
```

### 2.2 Automate Layer Scanning
**File:** `ValidationToolPanel.java`

Move scanning logic from button click into `isolateButton` ActionListener:
```java
// When Isolate button clicked:
1. Validate date is set
2. Check mapper authorization
3. Auto-scan layers (current scan button logic)
4. Perform isolation
5. Show success message
```

---

## Priority 3: Data Integrity (Est: 1 day)

### 3.1 Date + Mapper Filter Verification
**File:** `ValidationToolPanel.java`

**Location:** Line ~400-600 (isolation logic with primitive filtering)

**Test Cases:**
1. Date range: Start date only
2. Date range: End date only
3. Date range: Both dates
4. Date range with specific mapper
5. Edge case: Midnight timestamps

**Verification:**
- Check that `createdAt` and `timestamp` filtering works correctly
- Ensure mapper name matching is case-sensitive (OSM standard)
- Test with real data containing various date formats

---

## Priority 4: Production Polish (Est: 2-3 days)

### 4.1 JSON Library Integration
**Files:** `ValidationToolPanel.java`, `build.xml`

Replace regex-based JSON parsing with `org.json`:
```xml
<!-- build.xml -->
<dependency>
    <groupId>org.json</groupId>
    <artifactId>json</artifactId>
    <version>20231013</version>
</dependency>
```

```java
// ValidationToolPanel.java - fetchAuthorizedMappers()
JSONArray usersArray = new JSONObject(response).getJSONArray("users");
for (int i = 0; i < usersArray.length(); i++) {
    JSONObject user = usersArray.getJSONObject(i);
    String username = user.getString("osm_username");
    authorizedMappers.add(username);
}
```

### 4.2 Session Reset
**File:** `ValidationToolPanel.java`

After successful submission (line ~1050-1090):
```java
SwingUtilities.invokeLater(() -> {
    int choice = JOptionPane.showConfirmDialog(null, 
        "Validation submitted successfully!\n\nStart a new validation session?",
        "Success", 
        JOptionPane.YES_NO_OPTION);
    if (choice == JOptionPane.YES_OPTION) {
        submittedThisSession = false;
        taskIdField.setText("");
        notesField.setText("");
        // Clear error counts
        // Reset date picker
    }
});
```

### 4.3 Input Validation
**File:** `ValidationToolPanel.java`

Add new method before line ~950 (submitData):
```java
private boolean validateInputs() {
    String taskId = taskIdField.getText().trim();
    if (taskId.isEmpty() || taskId.length() > 100) {
        showError("Task ID must be 1-100 characters");
        return false;
    }
    
    String notes = notesField.getText().trim();
    if (notes.length() > 1000) {
        showError("Notes cannot exceed 1000 characters");
        return false;
    }
    
    // Validate error counts are non-negative integers
    try {
        int total = Integer.parseInt(totalBuildingsField.getText().trim());
        if (total < 0) throw new NumberFormatException();
    } catch (NumberFormatException e) {
        showError("Total buildings must be a positive number");
        return false;
    }
    
    return true;
}
```

---

## Priority 5: Testing & Documentation (Est: 1-2 days)

### 5.1 Live Testing Scenarios
1. **OAuth Integration:**
   - Test with authenticated JOSM user
   - Test with anonymous user (should show error)
   - Verify no disconnection warnings

2. **Date Validation:**
   - Try to isolate without date
   - Try to isolate with date
   - Test date filtering accuracy

3. **Authorization Check:**
   - Test with authorized mapper
   - Test with unauthorized mapper
   - Verify error messages

4. **Simplified Workflow:**
   - Full workflow: date → isolate (auto-scan) → submit
   - Verify all buttons work correctly
   - Test rapid sequential operations

### 5.2 Documentation Updates
**File:** `README.md`

Update sections:
1. **Authentication:** Explain OAuth 2.0 auto-detection
2. **Workflow:** Document new simplified process
3. **Requirements:** Date selection mandatory before isolation
4. **Authorization:** Explain project access control
5. **Error Messages:** Document all new validation errors

---

## Implementation Order

### Day 1-2: OAuth 2.0 (CRITICAL)
- [ ] Remove validator field from UI
- [ ] Add `getCurrentValidator()` method
- [ ] Integrate `UserIdentityManager`
- [ ] Test authentication detection
- [ ] Update `submitData()` to auto-populate validator
- [ ] Test with live JOSM instance

### Day 3: Date & Authorization Validation
- [ ] Add date validation to isolate button
- [ ] Add authorization check to isolate button
- [ ] Create helper method `checkMapperAuthorization()`
- [ ] Test error dialogs
- [ ] Verify authorization list loading

### Day 4: Workflow Simplification
- [ ] Remove scan button
- [ ] Remove force submit button
- [ ] Move scan logic into isolate button
- [ ] Update UI layout
- [ ] Test automated workflow

### Day 5: Data Integrity & Polish
- [ ] Verify date filtering logic
- [ ] Add JSON library dependency
- [ ] Replace regex parsing
- [ ] Add session reset dialog
- [ ] Add input validation
- [ ] Test all edge cases

### Day 6: Testing & Documentation
- [ ] Full integration testing
- [ ] Monitor for disconnection warnings
- [ ] Update README.md
- [ ] Create user guide updates
- [ ] Version bump to 2.1.0

---

## Technical Notes

### JOSM OAuth Dependencies
Already in JOSM core:
```java
org.openstreetmap.josm.data.UserIdentityManager
org.openstreetmap.josm.data.UserInfo
org.openstreetmap.josm.io.auth.CredentialsManager
org.openstreetmap.josm.data.oauth.*
```

### Current Plugin Dependencies
```xml
<classpath>
    <pathelement location="${josm}"/>
    <pathelement location="${lib}/jdatepicker-1.3.4.jar"/>
</classpath>
```

### New Dependency Needed
```xml
<dependency>
    <groupId>org.json</groupId>
    <artifactId>json</artifactId>
    <version>20231013</version>
</dependency>
```

---

## Risk Assessment

### High Risk ⚠️
- **OAuth Integration:** If JOSM user not authenticated, plugin won't work
  - *Mitigation:* Show clear error message with instructions to authenticate

### Medium Risk 🟡
- **Authorization Check:** May block legitimate users if API list not updated
  - *Mitigation:* Show clear error with contact information
  
- **Workflow Changes:** Users familiar with old buttons may be confused
  - *Mitigation:* Update documentation, provide migration guide

### Low Risk 🟢
- **Date Validation:** Simple validation, easy to test
- **JSON Library:** Well-tested library, minimal risk

---

## Success Criteria

1. ✅ No more disconnection warnings in JOSM
2. ✅ Users cannot isolate without date
3. ✅ Users cannot isolate if not authorized
4. ✅ Validator name auto-populated from JOSM
5. ✅ Simplified 3-button workflow
6. ✅ All authorization checks work correctly
7. ✅ Date filtering works accurately
8. ✅ Session can be reset after submission
9. ✅ Input validation prevents bad data
10. ✅ Comprehensive testing completed

---

## Version History

- **v2.0.0** (Current): Migrated from Google Sheets to DPW Manager API
- **v2.1.0** (Planned): OAuth 2.0 integration + workflow improvements

---

## Questions for User

1. Should we make the plugin require authentication (block anonymous users)?
2. What should happen if a user is not in the authorized mappers list?
3. Should we add a "Request Access" button that opens the DPW Manager website?
4. Do you want to keep the task ID field or auto-generate task IDs?
5. Should we add any logging to help debug authorization issues?

---

## Next Steps

**Immediate Action:** Start with OAuth 2.0 integration (Priority 1.1)

**Command to Begin:**
```bash
# No external dependencies needed - JOSM OAuth is built-in
# Start editing ValidationToolPanel.java
```

