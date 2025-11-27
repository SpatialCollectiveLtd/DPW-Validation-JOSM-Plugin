# Testing Guide - DPW Validation Tool v3.1.0-BETA

## Overview
This guide provides comprehensive testing procedures for the Tasking Manager integration features introduced in v3.1.0-BETA.

---

## Prerequisites

### Required:
- JOSM installed and running
- DPW Validation Tool v3.1.0-BETA plugin installed
- Active internet connection
- Access to HOT Tasking Manager (tasks.hotosm.org)
- DPW API credentials configured

### Recommended:
- Test on non-production data first
- Use test project 27396 or similar test environment
- JOSM remote control enabled (Edit → Preferences → Remote Control)

---

## Test Suite 1: Settings Panel

### Test 1.1: Open Settings Panel
**Steps:**
1. Open JOSM
2. Go to **Tools → DPW Validation Settings...**

**Expected Result:**
- Settings dialog opens
- All fields populated with default values
- TM Integration checkbox is **unchecked** by default

**Pass/Fail:** ___

---

### Test 1.2: Enable TM Integration
**Steps:**
1. Open Settings
2. Check "Enable Tasking Manager Integration"
3. Click Save

**Expected Result:**
- Settings saved message appears
- Dialog closes
- TM URL field appears in main validation panel

**Pass/Fail:** ___

---

### Test 1.3: Configure API URLs
**Steps:**
1. Open Settings
2. Change DPW API URL to: `app.spatialcollective.com/api`
3. Verify TM API URL: `tasking-manager-tm4-production-api.hotosm.org/api/v2`
4. Click Save

**Expected Result:**
- Custom API URLs saved
- Plugin uses new URLs for subsequent requests

**Pass/Fail:** ___

---

### Test 1.4: Reset to Defaults
**Steps:**
1. Open Settings
2. Change several values
3. Click "Reset to Defaults"
4. Confirm reset

**Expected Result:**
- All values reset to original defaults
- Confirmation message appears

**Pass/Fail:** ___

---

## Test Suite 2: Manual TM URL Input

### Test 2.1: Paste Valid TM URL
**Steps:**
1. Enable TM integration in settings
2. Restart JOSM and open validation panel
3. Paste URL: `https://tasks.hotosm.org/projects/27396/tasks/123`

**Expected Result:**
- Task ID field auto-fills with `123`
- If task is mapped, mapper username auto-selects
- If auto-fetch enabled, settlement auto-populates

**Pass/Fail:** ___

---

### Test 2.2: Alternative URL Format
**Steps:**
1. Clear all fields
2. Paste URL: `tasks.hotosm.org/projects/27396#task/456`

**Expected Result:**
- Task ID auto-fills with `456`
- Mapper detection works same as Test 2.1

**Pass/Fail:** ___

---

### Test 2.3: Invalid TM URL
**Steps:**
1. Paste invalid URL: `https://example.com/invalid`
2. Check JOSM log (F12)

**Expected Result:**
- No auto-population occurs
- Warning message in JOSM log
- No errors or crashes

**Pass/Fail:** ___

---

### Test 2.4: Unmapped Task
**Steps:**
1. Find an unmapped task (READY status)
2. Paste its TM URL

**Expected Result:**
- Task ID populates
- Warning: "No mapper found for this task"
- Log shows appropriate error message

**Pass/Fail:** ___

---

## Test Suite 3: Remote Control Detection

### Test 3.1: Enable Remote Control Detection
**Steps:**
1. Open Settings
2. Check "Enable Remote Control Task Detection"
3. Save settings

**Expected Result:**
- Setting saved successfully
- Layer change listener activated

**Pass/Fail:** ___

---

### Test 3.2: Open Task via TM Remote Control
**Steps:**
1. Enable remote control detection in settings
2. Go to tasks.hotosm.org
3. Select a MAPPED task
4. Click "Edit with JOSM" button
5. Wait for JOSM to load data

**Expected Result:**
- JOSM loads task data via remote control
- Changeset comment contains: `#hotosm-project-XXXXX-task-YYY`
- Validation panel auto-populates Task ID
- Mapper username auto-selects
- Popup notification appears: "Task Manager Task Detected!"

**Pass/Fail:** ___

---

### Test 3.3: Manual Layer Creation (No TM Info)
**Steps:**
1. With remote control detection enabled
2. Manually create a new data layer (File → New Layer)
3. Open validation panel

**Expected Result:**
- No auto-detection occurs
- Fields remain empty (expected behavior)
- No errors

**Pass/Fail:** ___

---

### Test 3.4: Switch Between Layers
**Steps:**
1. Open multiple TM tasks via remote control
2. Switch between layers in Layers panel

**Expected Result:**
- Auto-detection triggers on each layer switch
- Task ID updates to match active layer
- Mapper updates to match active layer

**Pass/Fail:** ___

---

## Test Suite 4: Auto-Fetch Settlement

### Test 4.1: Enable Auto-Fetch
**Steps:**
1. Open Settings
2. Check "Auto-fetch settlement from DPW API"
3. Save settings

**Expected Result:**
- Setting saved
- Future mapper selections trigger settlement fetch

**Pass/Fail:** ___

---

### Test 4.2: Test Settlement Auto-Fetch
**Steps:**
1. Enable auto-fetch settlement
2. Load a TM task with valid mapper
3. Verify mapper is in DPW authorized list

**Expected Result:**
- Settlement field auto-populates after mapper selection
- Settlement matches DPW database for that mapper
- Read-only settlement field shows gray background

**Pass/Fail:** ___

---

### Test 4.3: Mapper Not in DPW System
**Steps:**
1. Load a TM task
2. Mapper auto-detected but NOT in DPW authorized list

**Expected Result:**
- Warning dialog: "Mapper not found in authorized mapper list"
- Settlement remains empty
- User can manually select different mapper

**Pass/Fail:** ___

---

## Test Suite 5: Integration with Existing Workflow

### Test 5.1: Complete Validation Workflow
**Steps:**
1. Open TM task via remote control
2. Verify auto-population (Task ID, Mapper, Settlement)
3. Click "Isolate Mapper Work for Date"
4. Perform validation
5. Click "Submit Validation" or "Submit Rejection"
6. Verify submission to DPW API

**Expected Result:**
- Entire workflow completes successfully
- All API calls successful
- Data uploaded to Google Drive
- Validation logged in DPW system

**Pass/Fail:** ___

---

### Test 5.2: Disable TM Integration
**Steps:**
1. Disable TM integration in settings
2. Restart validation panel
3. Test traditional manual workflow

**Expected Result:**
- TM URL field hidden
- No auto-detection occurs
- All v3.0.1 functionality works normally
- No errors or regressions

**Pass/Fail:** ___

---

## Test Suite 6: Error Handling & Edge Cases

### Test 6.1: Network Timeout
**Steps:**
1. Disable internet connection
2. Paste TM URL or open task via remote control

**Expected Result:**
- Operation fails gracefully
- Error message in JOSM log
- No crashes or hangs
- Timeout after 10 seconds

**Pass/Fail:** ___

---

### Test 6.2: Invalid TM API URL
**Steps:**
1. Open Settings
2. Change TM API URL to invalid endpoint
3. Save and test TM integration

**Expected Result:**
- API call fails
- Error logged: "TM API returned status: XXX"
- No crashes

**Pass/Fail:** ___

---

### Test 6.3: Malformed Changeset Comment
**Steps:**
1. Manually edit layer changeset comment
2. Add malformed comment: `#hotosm-project-ABC-task-XYZ` (invalid format)

**Expected Result:**
- Remote control detection fails gracefully
- No auto-detection occurs
- No errors

**Pass/Fail:** ___

---

### Test 6.4: Concurrent API Requests
**Steps:**
1. Quickly paste multiple TM URLs in succession
2. Rapidly switch between layers

**Expected Result:**
- All requests handled in background threads
- UI remains responsive
- No race conditions or crashes

**Pass/Fail:** ___

---

## Test Suite 7: Cross-Platform Testing

### Test 7.1: Windows
**Platform:** Windows 10/11  
**Test:** Run all test suites above  
**Pass/Fail:** ___

---

### Test 7.2: macOS
**Platform:** macOS 12+  
**Test:** Run all test suites above  
**Pass/Fail:** ___

---

### Test 7.3: Linux
**Platform:** Ubuntu/Debian/Fedora  
**Test:** Run all test suites above  
**Pass/Fail:** ___

---

## Test Suite 8: Performance Testing

### Test 8.1: Large Project
**Steps:**
1. Test with large TM project (1000+ tasks)
2. Paste TM URL

**Expected Result:**
- Response time < 5 seconds
- No memory issues

**Pass/Fail:** ___

---

### Test 8.2: Cache Validation
**Steps:**
1. Set cache expiry to 1 hour
2. Fetch same task twice within 1 hour
3. Check if cached data used

**Expected Result:**
- Second fetch uses cached data (faster)
- Cache expires after 1 hour

**Pass/Fail:** ___

---

## JOSM Log Analysis

### How to Check JOSM Logs:
1. Press **F12** to open log window
2. Filter for: `DPWValidationTool` or `TM integration`

### Expected Log Entries:

**Successful TM URL parsing:**
```
Fetching TM task info: https://tasking-manager-tm4-production-api.hotosm.org/api/v2/projects/27396/tasks/123/
Found mapper: username123 for task 123
TM integration: Auto-populated from https://tasks.hotosm.org/projects/27396/tasks/123
```

**Successful Remote Control Detection:**
```
TM integration: Detected task from remote control - project 27396 task 123
TM integration: Auto-populated from remote control
```

**Expected Warnings:**
```
TM integration: No mapper found for this task. Task may not be mapped yet.
TM mapper 'username' not found in authorized mapper list
```

---

## Regression Testing Checklist

Verify all v3.0.1 features still work:

- [ ] Manual Task ID entry
- [ ] Manual mapper selection
- [ ] Manual settlement entry
- [ ] Date picker functionality
- [ ] Isolate mapper work
- [ ] Error counting
- [ ] Validation submission
- [ ] Rejection submission
- [ ] Google Drive upload
- [ ] Session reset
- [ ] Mapper list refresh

---

## Test Results Summary

**Tester Name:** _______________  
**Date:** _______________  
**JOSM Version:** _______________  
**Plugin Version:** v3.1.0-BETA  
**OS:** _______________

| Test Suite | Pass | Fail | Notes |
|------------|------|------|-------|
| Suite 1: Settings Panel | ☐ | ☐ | |
| Suite 2: Manual TM URL | ☐ | ☐ | |
| Suite 3: Remote Control | ☐ | ☐ | |
| Suite 4: Auto-Fetch Settlement | ☐ | ☐ | |
| Suite 5: Integration | ☐ | ☐ | |
| Suite 6: Error Handling | ☐ | ☐ | |
| Suite 7: Cross-Platform | ☐ | ☐ | |
| Suite 8: Performance | ☐ | ☐ | |
| Regression Tests | ☐ | ☐ | |

**Overall Status:** PASS / FAIL / NEEDS REVISION

---

## Bug Report Template

If you find issues, report using this template:

```
**Bug Title:** Brief description

**Environment:**
- JOSM Version: 
- Plugin Version: v3.1.0-BETA
- OS: 
- Java Version: 

**Steps to Reproduce:**
1. 
2. 
3. 

**Expected Behavior:**


**Actual Behavior:**


**JOSM Log Output:**
```
[Paste relevant log entries]
```

**Screenshots:**
[Attach if applicable]

**Severity:** Critical / High / Medium / Low

**Workaround:** [If known]
```

---

## Approval Signoff

**Developer:** _______________  Date: ___  
**QA Tester:** _______________  Date: ___  
**Product Owner:** _______________  Date: ___  

**Ready for Production:** YES / NO / WITH RESERVATIONS

---

**Document Version:** 1.0  
**Last Updated:** [Date of testing]
