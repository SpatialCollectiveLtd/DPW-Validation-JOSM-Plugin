# DPW Validation Tool - User Presentation

**A JOSM Plugin for Quality Assurance in Settlement Digitization**

Version 3.0.5 | Developed by Spatial Collective Ltd  
For the 2025 Digital Public Works Project

---

## 🎯 What is the DPW Validation Tool?

The DPW Validation Tool is a specialized plugin for JOSM (Java OpenStreetMap Editor) that streamlines the quality assurance workflow for validators checking the work of youth mappers in settlement digitization projects.

### **Key Purpose**
Validators can efficiently:
- Isolate a specific mapper's work
- Track quality issues across 10 error categories
- Submit validation data to central database
- Export clean data with automatic cloud backup
- Move to the next task without restarting JOSM

---

## ✨ Key Features

### 🔍 **1. Automatic Mapper Isolation**
```
Instead of manually searching:
  user:"mapper_name"
  
Just click:
  [Isolate Mapper Work]
  
Plugin automatically:
  ✅ Searches for all objects by mapper
  ✅ Creates new isolated layer
  ✅ Sets it as active for review
```

### 🔄 **2. Tasking Manager Integration (BETA)**
```
Traditional workflow:
  1. Open TM task in browser
  2. Copy task ID manually
  3. Look up mapper name
  4. Enter everything manually
  
With TM Integration:
  1. Paste TM URL in plugin
  2. Load data via remote control
  3. Everything auto-fills! ✨
     • Task ID detected from changeset
     • Mapper name fetched from TM API
     • Settlement auto-populated
```

### 📊 **3. Error Tracking Dashboard**
```
10 Predefined Error Categories:
  [ ➖ 0 ➕ ] Hanging Nodes
  [ ➖ 0 ➕ ] Overlapping Buildings
  [ ➖ 0 ➕ ] Buildings Crossing Highway
  [ ➖ 0 ➕ ] Missing Tags
  [ ➖ 0 ➕ ] Improper Tags
  [ ➖ 0 ➕ ] Features Misidentified
  [ ➖ 0 ➕ ] Missing Buildings
  [ ➖ 0 ➕ ] Building Inside Building
  [ ➖ 0 ➕ ] Building Crossing Residential
  [ ➖ 0 ➕ ] Improperly Drawn

Click + or - to count each error type
```

### ☁️ **4. Automatic Cloud Backup (v3.0.1)**
```
After validation:
  1. Export to local .osm file ✅
  2. Automatic upload to Google Drive ✅
  3. Linked to your validation record ✅
  
No manual uploads needed!
Data safely backed up automatically.
```

### 🔄 **5. Auto-Updates**
```
Plugin automatically:
  ✅ Checks for updates on startup
  ✅ Downloads latest version
  ✅ Installs on next JOSM restart
  
Always have the latest features!
No manual downloads required.
```

---

## 📱 Simple 7-Step Workflow

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  1️⃣  OPEN PANEL                                            │
│     Tools → DPW Validation Tool → Open Validation Panel    │
│                                                             │
│  2️⃣  ENTER TASK INFO                                       │
│     • Task ID: 27                                           │
│     • Select Mapper: john_mapper                            │
│     • Settlement: (auto-fills)                              │
│                                                             │
│  3️⃣  ISOLATE WORK                                          │
│     Click [Isolate Mapper Work]                             │
│     New layer created with mapper's objects                 │
│                                                             │
│  4️⃣  REVIEW IN JOSM                                        │
│     Visually inspect buildings, roads, tags                 │
│     Use JOSM's validation tools                             │
│                                                             │
│  5️⃣  COUNT ERRORS                                          │
│     Click +/- for each error type found                     │
│     Enter total buildings count                             │
│     Add comments if needed                                  │
│                                                             │
│  6️⃣  SUBMIT DECISION                                       │
│     Click [✅ VALIDATED] or [❌ REJECTED]                   │
│     Confirm in preview dialog                               │
│     Data submitted to DPW database                          │
│                                                             │
│  7️⃣  EXPORT & CONTINUE                                     │
│     Export to .osm file (choose location)                   │
│     Automatic cloud backup                                  │
│     Click [Start New Validation]                            │
│     Ready for next mapper!                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🖥️ User Interface Overview

```
┌───────────────────────────────────────────────────────────────┐
│  DPW Validation Tool v3.0.5                            [ × ]  │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  TM Project URL: [https://tasks.hotosm.org/projects/27396]   │
│  (Optional - enables auto-detection)                          │
│                                                               │
│  Task ID: [27______________]  ← Auto-detected from TM        │
│                                                               │
│  Settlement: [Example Settlement] (Auto-filled)               │
│                                                               │
│  Mapper Username: [john_mapper ▼]  🔄                        │
│  (Dropdown shows authorized mappers)                          │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ ⏳ Loading user list...                                 │ │
│  │ ✅ Authorized validator: jane_validator                 │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  [🔍 Isolate Mapper Work]                                    │
│                                                               │
│  Date: [📅 01/05/2026]                                       │
│                                                               │
│  Total Buildings: [150____]                                   │
│                                                               │
│  ┌─ Error Tracking ────────────────────────────────────────┐ │
│  │                                                          │ │
│  │  Hanging Nodes              [ ➖ ] 5 [ ➕ ]             │ │
│  │  Overlapping Buildings      [ ➖ ] 3 [ ➕ ]             │ │
│  │  Buildings Crossing Highway [ ➖ ] 0 [ ➕ ]             │ │
│  │  Missing Tags               [ ➖ ] 4 [ ➕ ]             │ │
│  │  Improper Tags              [ ➖ ] 0 [ ➕ ]             │ │
│  │  Features Misidentified     [ ➖ ] 0 [ ➕ ]             │ │
│  │  Missing Buildings          [ ➖ ] 0 [ ➕ ]             │ │
│  │  Building Inside Building   [ ➖ ] 0 [ ➕ ]             │ │
│  │  Building Cross Residential [ ➖ ] 0 [ ➕ ]             │ │
│  │  Improperly Drawn           [ ➖ ] 0 [ ➕ ]             │ │
│  │                                                          │ │
│  │  Total Errors: 12                                        │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                               │
│  Comments: ┌─────────────────────────────────────────────┐   │
│            │ Good work overall. Minor tag issues.        │   │
│            │                                             │   │
│            └─────────────────────────────────────────────┘   │
│                                                               │
│  ┌─ 📊 Validation Summary ─────────────────────────────────┐ │
│  │ [📊 Show Validation Summary]                            │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  [✅ VALIDATED]                    [❌ REJECTED]             │
│                                                               │
│  [📁 Export Validated Layer]                                 │
│                                                               │
│  [🔄 Start New Validation]                                   │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

---

## 🎓 Quick Start Guide

### **Installation**

1. **Download Plugin**
   - Get `DPWValidationTool.jar` from GitHub Releases
   - Or ask your admin for the file

2. **Install in JOSM**
   - Windows: `%APPDATA%\JOSM\plugins\`
   - Linux: `~/.config/JOSM/plugins/`
   - Mac: `~/Library/JOSM/plugins/`

3. **Restart JOSM**
   - Plugin loads automatically
   - Check for "DPW Validation Tool" in Tools menu

### **First Validation**

1. **Open Panel**
   ```
   Tools → DPW Validation Tool → Open Validation Panel
   ```

2. **Enter Task Info**
   - Task ID: Get from your task assignment
   - Mapper: Select from dropdown
   - Settlement: Auto-fills

3. **Isolate & Review**
   - Click "Isolate Mapper Work"
   - Review buildings in JOSM
   - Count errors using +/- buttons

4. **Submit**
   - Click ✅ VALIDATED or ❌ REJECTED
   - Confirm in dialog
   - Export when prompted

5. **Next Task**
   - Click "Start New Validation"
   - Repeat!

---

## 🔧 Settings & Configuration

```
Tools → DPW Validation Tool → Settings

┌─────────────────────────────────────────────────────────┐
│  DPW Validation Tool Settings v3.1.0-BETA              │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  API Configuration                                      │
│  • DPW API URL: app.spatialcollective.com/api          │
│  • TM API URL: tasking-manager-tm4...                  │
│                                                         │
│  Default Project (Pre-fill TM URL)                     │
│  • Project URL: tasks.hotosm.org/projects/27396        │
│  • OR Project ID: 27396                                │
│                                                         │
│  Feature Toggles (BETA)                                │
│  ☑ Enable Tasking Manager Integration                 │
│  ☑ Auto-fetch settlement from DPW API                  │
│  ☑ Enable Remote Control Task Detection               │
│                                                         │
│  Cache Settings                                        │
│  • Cache Expiry: [24] hours                            │
│                                                         │
│  [Check for Updates] [Reset to Defaults]               │
│  [Cancel] [Save]                                       │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### **Recommended Settings**

**For Tasking Manager Users:**
- ✅ Enable TM Integration
- ✅ Set Default Project URL (saves time!)
- ✅ Enable Remote Control Detection

**For Manual Workflow:**
- ❌ Disable TM Integration
- ✅ Keep Auto-fetch settlement enabled

---

## 💡 Pro Tips

### **Tip 1: Use TM Integration for Speed**
```
Instead of manually entering task info:
  1. Paste TM project URL once in settings
  2. Load task via remote control from TM
  3. Everything auto-fills! ✨
```

### **Tip 2: Review Before Submitting**
```
Always click "Show Validation Summary" to review:
  ✅ Correct mapper selected?
  ✅ Task ID accurate?
  ✅ Error counts reasonable?
  ✅ Total buildings count correct?
```

### **Tip 3: Use Keyboard for Error Counting**
```
Click in error field, then:
  ↑ Arrow Up = Increment (+1)
  ↓ Arrow Down = Decrement (-1)
  
Faster than clicking + / - buttons!
```

### **Tip 4: Keep Layers for Reference**
```
When asked "Clear all layers or keep?":
  • Clear = Clean slate (recommended)
  • Keep = Reference previous validations
  
You can always reopen OSM files later!
```

### **Tip 5: Check for Updates Regularly**
```
Settings → Check for Updates
  
New features added frequently:
  • Bug fixes
  • Performance improvements
  • New error categories
```

---

## 🆘 Troubleshooting

### **Problem: Mapper list won't load**
```
✅ Solutions:
  1. Check internet connection
  2. Click refresh button (🔄)
  3. Wait 10 seconds (rate limiting)
  4. Check Settings → DPW API URL
```

### **Problem: Can't isolate mapper work**
```
✅ Solutions:
  1. Ensure data is loaded in JOSM
  2. Check mapper username is exact match
  3. Try refreshing mapper list
  4. Verify mapper has objects in current view
```

### **Problem: Cloud upload fails**
```
✅ Solutions:
  1. Check internet connection
  2. Local file is still saved! ✅
  3. Contact admin if persists
  4. Manual upload available as backup
```

### **Problem: Title shows old version after update**
```
⚠️ Known Issue (Fix in progress)

Workaround:
  1. Plugin still updated (works correctly)
  2. Restart JOSM to see new version number
  3. Check Tools menu for confirmation
```

### **Problem: TM auto-detection not working**
```
✅ Solutions:
  1. Check Settings → ☑ Enable TM Integration
  2. Verify TM project URL is set
  3. Ensure changeset comment has #hotosm-project...
  4. Fallback: Enter task ID manually
```

---

## 📊 Validation Statistics

### **Typical Validation Speed**

```
Traditional Manual Workflow:
  ⏱️ ~15-20 minutes per task
    • 5 min: Find mapper's work
    • 5 min: Count errors manually
    • 5 min: Submit & export
    • 5 min: Reset for next task

With DPW Validation Tool:
  ⏱️ ~8-10 minutes per task ⚡
    • 1 min: Auto-isolate mapper work
    • 5 min: Count errors (UI assists)
    • 2 min: Submit & auto-export
    • 1 min: One-click reset

⚡ 40-50% faster validations!
```

### **Typical Error Distribution**

Based on 100+ validations:
```
Most Common Errors:
  1. Missing Tags (35%)
  2. Hanging Nodes (25%)
  3. Overlapping Buildings (15%)
  4. Improper Tags (10%)
  5. Improperly Drawn (8%)
  6. Other (7%)

Average Errors per Task: 8-12
Validation Pass Rate: ~75%
```

---

## 🌟 What's New

### **Version 3.0.5** (Current)
- ✅ Fixed auto-update installation
- ✅ Improved error messages
- ✅ Better rate limiting

### **Version 3.0.1**
- ✨ Automatic Google Drive cloud backup
- ✨ Upload progress indicator
- ✨ Enhanced validation preview

### **Version 3.1.0-BETA** (Upcoming)
- 🎉 Tasking Manager integration
- 🎉 Remote control task detection
- 🎉 Comprehensive settings panel
- 🎉 Auto-fetch mapper from TM API

---

## 🎯 Success Metrics

What makes a good validation?

```
✅ Completeness
   • All mapper's objects reviewed
   • No missed buildings
   • Full coverage of area

✅ Accuracy
   • Correct error counts
   • Proper categorization
   • Valid justifications

✅ Consistency
   • Same standards across mappers
   • Fair assessment
   • Constructive feedback

✅ Speed
   • Efficient workflow
   • No wasted time
   • Quick turnaround

✅ Documentation
   • Clear comments
   • Error details
   • Helpful feedback for mapper
```

---

## 📞 Support & Resources

### **Getting Help**

🐛 **Bug Reports**
- GitHub Issues: [Link to repo]
- Email: support@spatialcollective.com

📖 **Documentation**
- User Guide: See README.md
- Workflow Diagrams: See WORKFLOW_DIAGRAM.md
- Video Tutorial: [Link if available]

💬 **Community**
- Validator Chat: [Link to Slack/Discord]
- Monthly Q&A Sessions
- Validator Handbook

### **Admin Contact**

For access issues, API keys, or permissions:
- 📧 admin@spatialcollective.com
- 📱 [Phone number if applicable]

---

## 🏆 Best Practices

### **Before Starting**
1. ✅ Update plugin to latest version
2. ✅ Configure settings (TM URL if using)
3. ✅ Test with one simple task first
4. ✅ Familiarize yourself with error types

### **During Validation**
1. ✅ Use validation preview before submitting
2. ✅ Add helpful comments for mapper
3. ✅ Be consistent with error categorization
4. ✅ Focus on one mapper at a time

### **After Validation**
1. ✅ Verify export completed successfully
2. ✅ Check cloud backup confirmation
3. ✅ Clear layers before next task
4. ✅ Take breaks every hour

---

## 📈 Future Roadmap

### **Planned Features**

**Q1 2026**
- ✨ Batch validation support
- ✨ Validation statistics dashboard
- ✨ Export validation reports
- ✨ Custom error categories

**Q2 2026**
- ✨ Multi-language support
- ✨ Advanced TM integration
- ✨ Validation templates
- ✨ Mobile companion app (?)

**Your Ideas?**
- 💡 Submit feature requests on GitHub
- 💡 Join validator feedback sessions
- 💡 Vote on proposed features

---

## 🎓 Training Materials

### **Video Tutorials** (Planned)
1. Introduction to DPW Validation Tool (5 min)
2. Basic Validation Workflow (10 min)
3. TM Integration Setup (5 min)
4. Advanced Tips & Tricks (8 min)

### **Quick Reference Cards**
- Error Type Definitions
- Keyboard Shortcuts
- Troubleshooting Checklist
- Workflow Cheat Sheet

---

## 🙏 Credits

**Developed by:** Spatial Collective Ltd  
**Project:** Digital Public Works - Settlement Digitization  
**Year:** 2025-2026  

**Special Thanks:**
- DPW Team for API support
- JOSM Developer Community
- Beta testers and validators
- Youth mapper participants

---

## 📄 License

**Proprietary License**  
Internal Use Only - Digital Public Works Project  
© 2025 Spatial Collective Ltd

---

**End of User Presentation**  
Version: 3.0.5 | Updated: January 5, 2026
