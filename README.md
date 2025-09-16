DPW Validation Tool - JOSM Plugin

This repository contains the DPW Validation Tool JOSM plugin used by validators to inspect and submit validation results.

Build

Requirements:
- JDK 21 (or compatible Java 21 runtime for compilation)
- Apache Ant 1.10+

Build steps:

```powershell
$env:ANT_HOME='C:\Apache Ant\apache-ant-1.10.15'
$env:PATH = $env:ANT_HOME + '\bin;' + $env:PATH
ant -f build.xml clean dist
```

The plugin JAR will be produced in `dist/DPWValidationTool.jar`.

Usage

- Copy the JAR to your JOSM `plugins` folder or install it from the plugin manager.
- Open the DPW Validation Tool from the `Tools` menu.

Development

- Source is under `src/org/openstreetmap/josm/plugins/dpwvalidationtool`.
- Preferences used by the plugin are stored via JOSM `Preferences` keys: `dpw.settlement`, `dpw.tm.baseurl`, etc.

License

(Place license information here.)
