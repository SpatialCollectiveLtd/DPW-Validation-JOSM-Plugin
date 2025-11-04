# Testing Checklist - Cloud Upload Integration v3.0.1

## Pre-Testing Setup

### API Verification
- [ ] Verify API is accessible: `https://dpw-mauve.vercel.app`
- [ ] Test user lookup endpoint:
  ```bash
  curl "https://dpw-mauve.vercel.app/api/users?osm_username=<test_username>&exclude_managers=true"
  ```
- [ ] Verify your OSM username returns a user_id
- [ ] Test upload endpoint with sample file (see PLUGIN_INTEGRATION_INSTRUCTIONS.md)

### JOSM Setup
- [ ] JOSM is installed and running
- [ ] Logged in with OAuth 2.0 (check: JOSM > File > OAuth Authorization)
- [ ] Plugin installed in JOSM plugins folder
- [ ] Plugin appears in Windows > DPW Validation Tool menu

---

## Test Cases

### Test 1: Complete Success Flow
**Objective**: Verify end-to-end workflow with successful cloud upload

**Steps**:
1. [ ] Open JOSM with OAuth authentication
2. [ ] Open DPW Validation Tool panel
3. [ ] Select a validation date (REQUIRED)
4. [ ] Select mapper from dropdown
5. [ ] Click "Isolate" button
6. [ ] Wait for isolated layer to load
7. [ ] Verify validation preview panel appears
8. [ ] Add some error counts using +/- buttons
9. [ ] Enter validator comments
10. [ ] Click "Accept" button
11. [ ] Confirm in dialog
12. [ ] Wait for submission confirmation
13. [ ] Click "📤 Export Now" in export dialog
14. [ ] Choose save location in file chooser
15. [ ] Observe progress: "Exporting validated layer..."
16. [ ] Observe progress change: "Uploading to cloud storage..."
17. [ ] Verify success message shows:
    - Local file path
    - "✓ Backed up to cloud storage"
    - NO Google Drive URL (company property - kept internal)
18. [ ] Verify Drive URL is stored internally but not displayed
19. [ ] Click "🔄 Reset Session"
20. [ ] Verify all layers cleared

**Expected Results**:
- ✅ File exported locally
- ✅ File uploaded to Google Drive
- ✅ Backup confirmation shown (Drive URL NOT displayed - company property)
- ✅ Session reset successful

---

### Test 2: Network Failure Handling
**Objective**: Verify graceful degradation when upload fails

**Setup**: Temporarily disconnect network after export starts (or use invalid API URL)

**Steps**:
1. [ ] Complete steps 1-13 from Test 1
2. [ ] Choose save location
3. [ ] Disconnect network OR modify API URL in preferences
4. [ ] Wait for export to complete
5. [ ] Verify local file is saved
6. [ ] Verify success message shows:
    - Local file path
    - "⚠ Cloud backup failed"
    - "Local file saved successfully."
7. [ ] Verify no error dialogs appear
8. [ ] Verify file exists at local path

**Expected Results**:
- ✅ Local file saved successfully
- ✅ Warning shown about upload failure
- ✅ No error dialogs
- ✅ Workflow continues normally

---

### Test 3: Unknown User Handling
**Objective**: Verify handling when user_id cannot be fetched

**Setup**: Use a mapper username that doesn't exist in the API

**Steps**:
1. [ ] Complete isolation workflow
2. [ ] Select a mapper that likely doesn't exist in API
3. [ ] Complete validation and accept
4. [ ] Export file
5. [ ] Check JOSM console for warnings about user IDs
6. [ ] Verify local file saved
7. [ ] Verify upload skipped (check console logs)

**Expected Results**:
- ✅ Local file saved
- ✅ Upload skipped gracefully
- ✅ Warning logged in console
- ✅ No error to user

---

### Test 4: Multiple Validation Cycles
**Objective**: Verify multiple validations work correctly

**Steps**:
1. [ ] Complete full validation cycle (Test 1)
2. [ ] Click "🔄 Reset Session"
3. [ ] Start new validation with different mapper
4. [ ] Complete validation and export
5. [ ] Verify second file uploads successfully
6. [ ] Verify both files are backed up to cloud (check logs, not visible to validator)
7. [ ] Verify files have unique names

**Expected Results**:
- ✅ Each validation gets unique log_id
- ✅ Each file uploads successfully
- ✅ Files don't overwrite each other
- ✅ State resets properly between cycles

---

### Test 5: Export Cancel Flow
**Objective**: Verify handling when user cancels export

**Steps**:
1. [ ] Complete validation workflow
2. [ ] Accept validation
3. [ ] Click "📤 Export Now"
4. [ ] Click "Cancel" in file chooser
5. [ ] Choose "Yes" to retry
6. [ ] Choose location and complete export
7. [ ] Verify upload happens

**Expected Results**:
- ✅ Cancel prompts retry dialog
- ✅ Retry works correctly
- ✅ Upload happens after successful export

---

### Test 6: Validation Rejection Flow
**Objective**: Verify rejection doesn't trigger export/upload

**Steps**:
1. [ ] Complete isolation workflow
2. [ ] Add error counts
3. [ ] Click "Reject" button
4. [ ] Confirm rejection
5. [ ] Verify NO export dialog appears
6. [ ] Verify NO upload happens
7. [ ] Verify restart dialog appears

**Expected Results**:
- ✅ No export for rejected validations
- ✅ No upload for rejected validations
- ✅ Session reset still offered

---

### Test 7: Large File Upload
**Objective**: Verify timeout handling for large files

**Setup**: Validate a mapper with many buildings (if available)

**Steps**:
1. [ ] Isolate large dataset (>500 buildings)
2. [ ] Complete validation
3. [ ] Export and wait for upload
4. [ ] Verify 30-second timeout is sufficient
5. [ ] If timeout occurs, verify error handling

**Expected Results**:
- ✅ Large files upload within timeout
- ✅ Progress indicator stays active
- ✅ Timeout errors handled gracefully

---

### Test 8: Special Characters in Metadata
**Objective**: Verify proper encoding of special characters

**Steps**:
1. [ ] Use task ID with special chars: "Task-123/A"
2. [ ] Use settlement with special chars: "Nairobi (West)"
3. [ ] Complete validation with comments: "Good work! 👍"
4. [ ] Export and upload
5. [ ] Verify upload succeeds
6. [ ] Check logs for metadata (Drive URL not shown to validator)

**Expected Results**:
- ✅ Special characters handled correctly
- ✅ Upload succeeds
- ✅ Metadata preserved

---

### Test 9: API Error Handling
**Objective**: Verify handling of API errors

**Note**: May require API team assistance to test

**Scenarios to test**:
- [ ] 400 Bad Request (invalid data)
- [ ] 401 Unauthorized (invalid auth)
- [ ] 500 Internal Server Error
- [ ] Network timeout
- [ ] Invalid JSON response

**Expected Results**:
- ✅ Error logged with details
- ✅ Local file still saved
- ✅ User sees warning
- ✅ No crash

---

### Test 10: Console Log Verification
**Objective**: Verify comprehensive logging

**Steps**:
1. [ ] Complete full validation workflow
2. [ ] Open JOSM error log: Help > Show log
3. [ ] Search for "DPWValidationTool"
4. [ ] Verify log entries include:
   - "Fetching user_id for: <username>"
   - "Found user_id=X for <username>"
   - "Uploading to cloud: <filename>"
   - "Upload successful, Drive URL: <url>"
   - "Export workflow complete"

**Expected Results**:
- ✅ All operations logged
- ✅ User IDs logged
- ✅ Upload result logged
- ✅ No errors in log (unless expected)

---

## Performance Tests

### Upload Speed
- [ ] Measure time for small file (<100 KB): _____ seconds
- [ ] Measure time for medium file (100-500 KB): _____ seconds
- [ ] Measure time for large file (>500 KB): _____ seconds

### Network Impact
- [ ] Monitor network usage during upload
- [ ] Verify upload doesn't block JOSM UI
- [ ] Verify progress dialog responsive

---

## Regression Tests

### Existing Functionality
- [ ] Isolate still works
- [ ] Accept/Reject still works
- [ ] Manual export still works (if available)
- [ ] Session reset still works
- [ ] Validation preview still works
- [ ] Error counting still works
- [ ] Comments field still works
- [ ] All dialogs display correctly

---

## Edge Cases

### No Validation Log ID
**Scenario**: Submission succeeds but no log_id returned
- [ ] Upload skipped gracefully
- [ ] Warning logged
- [ ] Local file saved

### Duplicate Usernames
**Scenario**: API returns multiple users with same name
- [ ] First user_id used
- [ ] Upload proceeds normally

### Empty Filenames
**Scenario**: Task ID is empty
- [ ] Filename uses "unknown"
- [ ] Upload still works

### File Permissions
**Scenario**: No write permission to export location
- [ ] Export fails with clear error
- [ ] Upload doesn't attempt
- [ ] Error message helpful

---

## Sign-Off

### Tester Information
- **Tester Name**: _______________________
- **Date**: _______________________
- **JOSM Version**: _______________________
- **Plugin Version**: 3.0.1
- **OS**: _______________________

### Test Results
- **Tests Passed**: ____ / ____
- **Tests Failed**: ____ / ____
- **Critical Issues**: _______________________
- **Minor Issues**: _______________________

### Approval
- [ ] All critical tests passed
- [ ] Known issues documented
- [ ] Ready for deployment

**Notes**:
_______________________________________________________________
_______________________________________________________________
_______________________________________________________________
_______________________________________________________________
