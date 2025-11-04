# Connect Your JOSM Plugin to DPW Cloud Storage
## Simple Integration Instructions

**Date:** October 30, 2025  
**Purpose:** Replace local OSM file saving with cloud upload to Google Drive

---

## What You Need to Know

Your plugin currently saves validated OSM files to local disk. We need you to change it to upload files to our API instead. The files will be automatically stored in Google Drive and tracked in our database.

---

## API Endpoint for File Upload

**Upload Endpoint:**
```
POST https://dpw-mauve.vercel.app/api/osm-uploads
```

**Content-Type:** `multipart/form-data`

---

## Required Form Fields

Send these fields in your multipart form request:

| Field Name | Type | Required | Description |
|------------|------|----------|-------------|
| `file` | File | ✅ Yes | The OSM XML file to upload |
| `validation_log_id` | Integer | ✅ Yes | ID from validation log (see step 1 below) |
| `mapper_user_id` | Integer | ✅ Yes | Database user_id of the digitizer/mapper |
| `validator_user_id` | Integer | ✅ Yes | Database user_id of the validator |
| `uploaded_by_user_id` | Integer | ✅ Yes | Database user_id of uploader (usually same as validator) |
| `task_id` | String | ❌ No | Optional task identifier |
| `settlement` | String | ❌ No | Optional settlement name |

---

## How to Get Required IDs

### Step 1: Get Validation Log ID

You should already be submitting validation results to:
```
POST https://dpw-mauve.vercel.app/api/validation-log
```

The response includes a `log_id`:
```json
{
  "success": true,
  "data": {
    "log_id": 123
  }
}
```

**Use this `log_id` as `validation_log_id` for the file upload.**

### Step 2: Get User IDs

Query user by OSM username:
```
GET https://dpw-mauve.vercel.app/api/users?osm_username={USERNAME}&exclude_managers=true
```

Response:
```json
{
  "success": true,
  "data": [{
    "user_id": 45,
    "osm_username": "john_mapper",
    "role": "Digitizer"
  }]
}
```

**Use the `user_id` from this response.**

Do this for:
- The mapper (digitizer) → `mapper_user_id`
- The validator → `validator_user_id` and `uploaded_by_user_id`

---

## Expected Response

### Success (200 OK):
```json
{
  "success": true,
  "message": "OSM file uploaded successfully to Google Drive",
  "file_id": 156,
  "file_name": "validated_map.osm",
  "file_size": 45678,
  "drive_file_id": "1a2b3c4d5e6f7g8h9i0j",
  "drive_file_url": "https://drive.google.com/file/d/1a2b3c4d5e6f7g8h9i0j/view",
  "upload_status": "Uploaded",
  "upload_timestamp": "2025-10-30T10:15:30.000Z",
  "folder_path": "Digitizers/john_mapper"
}
```

**Show the `drive_file_url` to your users** - this is the Google Drive link they can click to view the file.

### Error (400/500):
```json
{
  "success": false,
  "error": "Error message describing what went wrong"
}
```

---

## Workflow

1. User completes validation in JOSM
2. Your plugin submits validation log → receives `log_id`
3. Your plugin queries user IDs for mapper and validator
4. Your plugin exports OSM data to a file
5. **Your plugin uploads file to our API** (this is the new part)
6. API uploads to Google Drive and returns Drive link
7. Your plugin shows the Drive link to the user
8. User can click to view file in Google Drive

---

## File Organization

Files are automatically organized in Google Drive:
```
Digitizers/
  └── {mapper_username}/
      └── {your_file.osm}
```

Each mapper gets their own folder named after their OSM username.

---

## Testing the API

Before integrating, test that the API is working:

**Test endpoint:**
```bash
curl https://dpw-mauve.vercel.app/api/test-drive
```

Should return: `{"success": true, ...}`

**Test upload:**
```bash
curl -X POST https://dpw-mauve.vercel.app/api/osm-uploads \
  -F "file=@test.osm" \
  -F "validation_log_id=1" \
  -F "mapper_user_id=2" \
  -F "validator_user_id=3" \
  -F "uploaded_by_user_id=3"
```

(Use real IDs from your database)

---

## Common Issues

**"Missing required field"**
- Make sure you're sending all required form fields
- Double-check field names are exactly as specified

**"Validation log not found"**
- The `validation_log_id` doesn't exist in database
- Make sure you submit the validation log FIRST and use the returned `log_id`

**"User not found"**
- The `user_id` doesn't exist in database
- Query `/api/users` to verify user exists and get correct ID

**Connection timeout**
- Check internet connection
- File might be too large (try smaller file first)

---

## What Changes in Your Plugin

**Before:**
- Plugin saves OSM file to local disk
- User gets local file path

**After:**
- Plugin uploads OSM file to our API
- User gets Google Drive link
- File is backed up to cloud
- File is tracked in database

---

## Important Notes

1. **User IDs are integers** - Don't send OSM usernames, send the database `user_id`
2. **Submit validation log first** - You need the `log_id` before uploading the file
3. **Files are permanent** - Once uploaded, files stay in Google Drive
4. **No authentication required** - Just send the form data, no API keys needed
5. **Multipart form-data** - Use multipart form encoding, not JSON

---

## Need Help?

1. **Test the API directly** first with cURL to make sure it's working
2. **Check the response** - Error messages will tell you what's wrong
3. **Verify your IDs** - Query the database to make sure validation log and user IDs exist
4. **Contact us** if API is not responding or returning unexpected errors

---

## Summary

**What to do:**
1. Find where your plugin saves files locally
2. Replace with HTTP POST to `https://dpw-mauve.vercel.app/api/osm-uploads`
3. Send the file + required IDs as multipart form data
4. Show the returned `drive_file_url` to your users

**API Endpoint:** `https://dpw-mauve.vercel.app/api/osm-uploads`  
**Method:** POST  
**Content-Type:** multipart/form-data  
**Required:** file, validation_log_id, mapper_user_id, validator_user_id, uploaded_by_user_id

That's it! You know your plugin's code best - integrate this however makes sense for your architecture.

---

**Last Updated:** October 30, 2025  
**Status:** API is live and ready for integration
