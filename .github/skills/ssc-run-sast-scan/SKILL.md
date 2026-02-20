---
name: ssc-run-sast-scan
description: "Package source code and submit a SAST scan via ScanCentral SAST (SC-SAST), publishing results to an SSC application version. Use this skill when a developer wants to run a SAST scan in an on-premise Fortify environment. Covers ScanCentral Client install, source packaging, scan submission, wait-for, and SSC artifact processing."
argument-hint: "<app-name>:<version-name> [--sensor-pool <pool>]"
---

## Overview

Running a SAST scan with ScanCentral SAST involves:
1. **Package** – Use ScanCentral Client to create a scan package
2. **Submit** – Send the package to the SC-SAST Controller via `fcli sc-sast scan start`
3. **Wait** – Poll for completion
4. **Publish** – Results are automatically published to SSC when `--publish-to` is specified at start time

## Prerequisites

- Active SSC session with SC-SAST URL configured (`/ssc-authenticate` first)
- A target SSC application version (`/ssc-create-appversion` first if needed)
- Java 17+ on `PATH`
- ScanCentral SAST Controller URL (usually `https://scancentral.example.com/scancentral-ctrl`)

---

## Step 1 – Install ScanCentral Client (first time only)

```shell
fcli tool sc-client install -v latest
```

Run `fcli tool env init` to add it to your `PATH`:

```shell
fcli tool env shell >> ~/.bashrc && source ~/.bashrc
# Windows PowerShell:
fcli tool env powershell >> $PROFILE
```

Verify: `scancentral --version`

---

## Step 2 – Check Available Sensor Pools (optional)

```shell
fcli sc-sast sensor-pool list
```

Use `--sensor-pool <pool-name>` in Step 4 to target a specific pool.

---

## Step 3 – Package Source Code

Navigate to your project root, then:

```shell
# Auto-detected build tool
scancentral package -o package.zip

# Explicit build tool
scancentral package --build-tool mvn -o package.zip      # Maven
scancentral package --build-tool gradle -o package.zip   # Gradle
scancentral package --build-tool msbuild -o package.zip  # .NET
scancentral package --build-tool none -o package.zip     # Python / JS / source-only
```

---

## Step 4 – Submit the Scan

```shell
fcli sc-sast scan start \
  --controller-url https://scancentral.example.com/scancentral-ctrl \
  -f package.zip \
  --publish-to "<app-name>:<version-name>" \
  --store scSastScan
```

| Option | Purpose |
|--------|---------|
| `--controller-url` | SC-SAST Controller URL (can be set via `FCLI_DEFAULT_SC_SAST_URL`) |
| `--publish-to` | SSC app:version to publish results to when scan completes |
| `--sensor-pool` | Target a specific sensor pool (optional) |
| `--store scSastScan` | Saves the `jobToken` for `wait-for` |

---

## Step 5 – Wait for Completion

```shell
fcli sc-sast scan wait-for ::scSastScan:: \
  --interval 30s \
  --timeout 90m
```

---

## Step 6 – Verify Results in SSC

After the scan completes and is published, check the artifact in SSC:

```shell
# List artifacts for the appversion
fcli ssc artifact list \
  --appversion "<app-name>:<version-name>" \
  -q 'scanTypes matches "\bSCA\b"' \
  --store sscArtifact

# Wait for SSC to process the artifact (if needed)
fcli ssc artifact wait-for ::sscArtifact:: \
  --until PROCESS_COMPLETE

# Approve if required by your SSC workflow
fcli ssc artifact approve ::sscArtifact::id
```

---

## Step 7 – Refresh Metrics (optional)

```shell
fcli ssc appversion refresh-metrics --appversion "<app-name>:<version-name>"
```

---

## Complete Pipeline Script

```shell
#!/bin/bash
set -e
APP_NAME="MyApp"
BRANCH=$(git rev-parse --abbrev-ref HEAD | tr '/' '-')
SC_SAST_URL="https://scancentral.example.com/scancentral-ctrl"

# Create version if needed
fcli ssc appversion create "${APP_NAME}:${BRANCH}" \
  --issue-template "Prioritized High Risk Issue Template" \
  --auto-required-attrs \
  --copy-from "${APP_NAME}:main" \
  --skip-if-exists \
  --store sscVersion

# Package
scancentral package -o package.zip

# Submit and wait
fcli sc-sast scan start \
  --controller-url "${SC_SAST_URL}" \
  -f package.zip \
  --publish-to "${APP_NAME}:${BRANCH}" \
  --store scSastScan

fcli sc-sast scan wait-for ::scSastScan:: --timeout 90m

# Wait for SSC to process
fcli ssc artifact wait-for \
  $(fcli ssc artifact list --appversion "${APP_NAME}:${BRANCH}" \
    -q 'scanTypes matches "\bSCA\b"' -o 'expr={id}\n' | head -1) \
  --until PROCESS_COMPLETE
```

---

## Environment Variables

| Variable | Description |
|----------|-------------|
| `FCLI_DEFAULT_SC_SAST_URL` | Default SC-SAST Controller URL |
| `FCLI_DEFAULT_SSC_URL` | SSC base URL |

---

## Troubleshooting

| Problem | Solution |
|---------|---------|
| `No sensors available` | Check `fcli sc-sast sensor list`; verify sensor pool name |
| Scan not published to SSC | Ensure `--publish-to` app:version exists and is committed |
| Artifact stuck in `SCHED_PROCESSING` | Check SSC job queue: `fcli ssc system-state list-jobs` |
| SSL errors between client and controller | Configure trust store; check `FCLI_TRUSTSTORE` env variable |
| `401` on controller | Verify SSC session includes SC-SAST URL (re-login with `--sc-sast-url`) |
