# DPW Validation Tool v3.0.1

**Release Date**: November 4, 2025

## 🎉 What's New

### Critical Fixes
- **Fixed Plugin Compatibility**: Plugin-Main-Version now correctly set to JOSM build 18823
- **Fixed Plugin Visibility**: Plugin stays functional after session reset - no need to restart JOSM between validations

### New Features
- **Cloud Storage Integration**: Automatic secure backup to project storage after export
- **User Tracking**: Automatic mapper and validator identification
- **Enhanced Stability**: Improved session management and error handling

## 📦 Installation

### Requirements
- **JOSM**: Build 18823 or higher (June 2024+)
- **Java**: Version 21.0 or higher
- **Internet**: Required for API access

### Steps

1. **Download** `DPWValidationTool.jar` from this release
2. **Copy to JOSM plugins folder**:
   - Windows: `%APPDATA%\JOSM\plugins\`
   - Linux: `~/.config/JOSM/plugins/`
   - macOS: `~/Library/JOSM/plugins/`
3. **Restart JOSM**
4. **Open plugin**: Windows → DPW Validation Tool

## 🔧 What's Fixed

| Issue | Status | Description |
|-------|--------|-------------|
| Plugin compatibility warnings | ✅ Fixed | No more "Missing plugin main version" warnings |
| Plugin unresponsive after reset | ✅ Fixed | Dialog stays functional indefinitely |
| Cloud upload | ✅ Added | Automatic backup to secure storage |
| Version metadata | ✅ Updated | All version references now show 3.0.1 |

## 📝 Changelog

### Added
- Cloud storage integration with automatic upload
- User ID resolution for audit trails
- Validation log linkage

### Fixed
- Plugin-Main-Version now uses JOSM build number (18823)
- Plugin dialog visibility after clearing all layers
- Session reset reliability

### Changed
- Updated plugin version to 3.0.1
- Simplified README for end users
- Cleaned up repository structure

## 🚀 Upgrade from Previous Versions

If you're using v2.1.0 or v3.0.0:

1. Remove old plugin: Delete `DPWValidationTool.jar` from JOSM plugins folder
2. Install new version: Copy new JAR file to plugins folder
3. Restart JOSM once
4. All settings and workflows remain the same

## 📊 Tested On

- ✅ JOSM 19439 (September 2025)
- ✅ Java 21.0.8
- ✅ Windows 10 & 11
- ✅ Complete validation workflow
- ✅ Session reset functionality
- ✅ Cloud upload integration

## 🐛 Known Issues

None at this time.

## 📖 Documentation

- **User Guide**: See [README](https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin#readme)
- **Report Issues**: [GitHub Issues](https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/issues)

## 🙏 Acknowledgments

Thanks to all validators using this plugin and providing feedback!

---

**Full Changelog**: [v3.0.0...v3.0.1](https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/compare/v3.0.0...v3.0.1)
