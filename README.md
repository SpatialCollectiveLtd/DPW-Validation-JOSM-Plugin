![Build](https://github.com/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin/actions/workflows/ci.yml/badge.svg)
![License](https://img.shields.io/badge/license-Proprietary-lightgrey.svg)
![Release](https://img.shields.io/github/v/release/SpatialCollectiveLtd/DPW-Validation-JOSM-Plugin)

# DPW Validation Tool for JOSM

The DPW (Digital Public Works) Validation Tool is a specialized plugin for the Java OpenStreetMap Editor (JOSM). Developed for the 2025 Digital Public Works project, this tool streamlines the quality assurance and validation workflow for the Settlement Digitization module.

It provides Final Validators with a secure, efficient, and integrated environment to assess the work of youth mappers, log quality metrics, and produce clean data deliverables — all directly inside JOSM.

---

## Table of Contents

- [Key Features](#key-features)
- [The Validation Workflow (Validator)](#the-validation-workflow-validator)
- [Installation (Interactive)](#installation-interactive)
- [Development & Build](#development--build)
- [Design Notes & UX](#design-notes--ux)
- [Contributing](#contributing)
- [Changelog (Selected)](#changelog-selected)
- [License & Ownership](#license--ownership)
- [Interactive Tips](#interactive-tips)

---

## Key Features

- **Secure User Authentication**: Fetches the project's central user registry so only authorized validators and mappers are used for tasks.
- **Automated Work Isolation**: Copy a single mapper's contributions into a clean JOSM layer for unbiased review.
- **Integrated QA Panel**: Side panel to select mapper, log counts for 10 error classes with +/- controls, add validator comments, and mark Accept/Reject.
- **Direct Submission**: Send validation reports (error counts, metadata) directly to the project's Google Sheet endpoint.
- **One-Click Deliverable Export**: Export cleaned data with a project-compliant filename: `Task_<taskId>_<mapper>_<YYYY-MM-DD>.osm`.

---

## The Validation Workflow (Validator)

1. Lock the task in HOT Tasking Manager and download it to JOSM.
2. In JOSM, open the DPW Validation Tool from the menu (Data -> DPW Validation Tool).
3. Click **Refresh Mapper List** to fetch authorized mappers.
4. Select the mapper whose work will be reviewed.
5. Optionally select a date and click **Isolate** to create a new layer containing only that mapper's contributions.
6. Review the isolated layer and use the side panel to log errors (+ / -) and comments.
7. Click **Validate (Accept)** or **Invalidate (Reject)** to submit the report.
8. Click **Export Validated Layer** to save the final, clean `.osm` file as a deliverable.

---

## Installation (Interactive)

Follow these steps to install the plugin locally in JOSM.

1. Download the latest release jar (example: `dist/DPWValidationTool.jar`).
2. Open JOSM and go to `Edit -> Preferences` (or press `F12`).
3. Open the `Plugins` tab.
4. Click **"Install from file..."**, select the downloaded JAR and confirm.
5. Restart JOSM.

Tip: to quickly install from the command line (Windows PowerShell), copy the jar into your JOSM plugins folder (example path shown below):

```powershell
$josmPlugins = "$env:USERPROFILE\\.josm\\plugins"
New-Item -ItemType Directory -Path $josmPlugins -Force | Out-Null
Copy-Item -Path .\\dist\\DPWValidationTool.jar -Destination $josmPlugins
```

After restart, open the plugin from the `Data` menu.

---

## Development & Build

Requirements:
- JDK 21
- Apache Ant

Quick build steps (PowerShell):

```powershell
# Set ANT_HOME if needed, then build
$env:ANT_HOME = 'C:\\Apache Ant\\apache-ant-1.10.15'
$env:PATH = $env:ANT_HOME + '\\bin;' + $env:PATH
ant -f build.xml clean dist
```

Result: `dist/DPWValidationTool.jar`

Note: The repository currently bundles the date picker dependency under `lib/` for convenience. For production-ready distribution you may prefer to use a build process that shades or downloads dependencies.

---

## Design Notes & UX

- The side panel uses a compact responsive layout: mapper combo expands, while date and isolate controls remain compact.
- The isolate functionality clones primitives to a new in-memory `DataSet` to avoid sharing primitives across layers and to create a clean review layer.
- The plugin attempts to use the JOSM search compiler for timestamp filtering; if timestamps are missing or the search cannot be parsed, the plugin falls back to mapper-only filtering.

---

## Contributing

Contributions and bug reports are welcome. Suggested workflow:

- Fork the repository
- Create a feature branch for your change
- Run the build and tests (if present)
- Open a pull request describing your change

Please avoid committing large binary dependencies to git in future patches; consider adding them to the release bundle only.

---

## Changelog (Selected)

- 2025-09-18: UI: responsive mapper/date/isolate row; mapper-specific building counts; layout polish
- 2025-09-01: Initial plugin scaffold and basic validation workflow

---

## License & Ownership

This plugin is maintained and owned by Spatial Collective Ltd.
For licensing and commercial questions, contact the maintainers.

---

## Interactive Tips

- To quickly test the isolate flow, open a layer with edits, select a mapper, pick a date and click **Isolate**.
- Use the **Refresh Mapper List** button if the mapper doesn't appear or the registry changed.
- Exported filenames follow the `Task_<taskId>_<mapper>_<YYYY-MM-DD>.osm` convention.

---
