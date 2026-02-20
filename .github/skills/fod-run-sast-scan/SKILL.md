---
name: fod-run-sast-scan
description: "Package source code and submit a SAST scan to Fortify on Demand (FoD). Use this skill when a developer wants to run a security scan against an FoD release. Covers ScanCentral Client install, source packaging, scan submission, waiting for results, and first-time scan configuration."
argument-hint: "<app-name>:<release-name> [--sensor-version <version>]"
---

## Overview

Running a SAST scan on FoD involves three phases:
1. **Package** – Use ScanCentral Client to create a `.zip` of the source code
2. **Submit** – Upload the package to FoD via `fcli fod sast-scan start`
3. **Wait** – Optionally poll for completion with `fcli fod sast-scan wait-for`

## Prerequisites

- Active FoD session (`/fod-authenticate` first)
- A FoD release for the current branch (`/fod-create-release` first if needed)
- SAST scanning must be configured on the release (run `fcli fod sast-scan setup` if needed – see Step 1)
- Java 17+ on the `PATH` (required by ScanCentral Client)
- Internet access to download ScanCentral Client (or provide local install path)

---

## Step 1 – Install ScanCentral Client (first time only)

```shell
fcli tool sc-client install -v latest
```

This installs the ScanCentral SAST Client and adds it to the Fortify tools `bin` directory (`~/fortify/tools/bin`). Run `fcli tool env init` to add it to your current shell `PATH`:

```shell
fcli tool env shell >> ~/.bashrc && source ~/.bashrc
# Windows PowerShell:
fcli tool env powershell >> $PROFILE
```

Verify: `scancentral --version`

---

## Step 2 – Configure SAST Scanning on the Release (first time only)

Check whether SAST scanning is already configured:

```shell
fcli fod sast-scan get-config --release "<app-name>:<release-name>"
```

If not configured, set it up:

```shell
# List available assessment types:
fcli fod release list-assessment-types --release "<app-name>:<release-name>"

fcli fod sast-scan setup \
  --release "<app-name>:<release-name>" \
  --assessment-type "Static" \
  --frequency "Subscription" \
  --technology-type "Java/J2EE" \
  --oss true
```

Common `--technology-type` values: `Java/J2EE`, `.NET/C#`, `Python`, `JavaScript/TypeScript`, `Go`, `C/C++`, `Ruby`.

---

## Step 3 – Package Source Code

Navigate to your project root directory, then:

### Auto-detected build tool (Maven, Gradle, MSBuild, etc.)

```shell
scancentral package -o package.zip
```

### With explicit build tool

```shell
# Maven
scancentral package --build-tool mvn -o package.zip

# Gradle
scancentral package --build-tool gradle -o package.zip

# MSBuild / .NET
scancentral package --build-tool msbuild -o package.zip

# NPM / JavaScript / TypeScript
scancentral package --build-tool none -o package.zip
```

### Packaging tip

Add `--bt none` for interpreted languages (Python, Ruby, JavaScript) — ScanCentral will include source files without building.

---

## Step 4 – Submit the Scan

```shell
fcli fod sast-scan start \
  --release "<app-name>:<release-name>" \
  --file package.zip \
  --notes "Branch: $(git rev-parse --abbrev-ref HEAD), Commit: $(git rev-parse --short HEAD)" \
  --store fodScan
```

The `--store fodScan` saves the scan ID for the next step.

---

## Step 5 – Wait for Results (optional)

```shell
fcli fod sast-scan wait-for ::fodScan:: \
  --interval 30s \
  --timeout 90m
```

`wait-for` exits with a non-zero code if the scan does not complete successfully, making it useful for breaking a CI/CD build.

List possible terminal statuses:

```shell
fcli fod sast-scan wait-for -h
```

---

## Step 6 – Check Scan Status

```shell
fcli fod sast-scan get ::fodScan::scanId
# Or list recent scans:
fcli fod sast-scan list --release "<app-name>:<release-name>"
```

---

## Complete Pipeline Script

```shell
#!/bin/bash
set -e
APP_NAME="MyApp"
BRANCH=$(git rev-parse --abbrev-ref HEAD | tr '/' '-')

# Create release if needed
fcli fod release create "${APP_NAME}:${BRANCH}" \
  --copy-state-from "${APP_NAME}:main" \
  --sdlc-status Development \
  --skip-if-exists \
  --store fodRelease

# Package
scancentral package -o package.zip

# Submit scan
fcli fod sast-scan start \
  --release "::fodRelease::releaseId" \
  --file package.zip \
  --notes "CI build on branch ${BRANCH}" \
  --store fodScan

# Wait (optional – remove for async pipelines)
fcli fod sast-scan wait-for ::fodScan:: --timeout 90m
```

---

## Environment Variables

| Variable | Description |
|----------|-------------|
| `FCLI_DEFAULT_FOD_RELEASE` | Default release (format `app:release`) to avoid repeating `--release` |

---

## Troubleshooting

| Problem | Solution |
|---------|---------|
| `scancentral: command not found` | Run `fcli tool env shell >> ~/.bashrc && source ~/.bashrc` |
| `No SAST subscription` | Configure scan setup in Step 2; verify entitlements in FoD portal |
| Packaging fails | Try `scancentral package --bt none` for source-only scan |
| Scan stuck in `In_Progress` | Check FoD portal for queued/error status; increase `--timeout` |
| Large package (>2 GB) | Exclude test resources, build artifacts: `scancentral package --exclude target/ -o package.zip` |
