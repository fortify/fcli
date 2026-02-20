---
name: fod-check-scan-status
description: "Check the status of one or more FoD SAST scans, wait for completion, and handle timeouts. Use this skill after a scan is submitted to FoD to monitor progress, gate a pipeline build, or confirm results are available."
argument-hint: "<app-name>:<release-name> [--scan-id <id>]"
---

## Overview

After submitting a SAST scan to FoD you often need to:

1. **Find** the scan ID(s) for a release.
2. **Inspect** detailed scan status and metadata.
3. **Wait** for a terminal state so downstream steps (vulnerability review, report generation) can proceed.

## Prerequisites

- Active FoD session (`/fod-authenticate` first)
- A scan must already be in progress or completed (`/fod-run-sast-scan` first)

---

## Step 1 – List Scans for a Release

```shell
fcli fod sast-scan list --release "<app-name>:<release-name>"
```

This returns the scan IDs and their current status. Look for the `scanId` column.

Store the release for later reuse:

```shell
fcli fod release get "<app-name>:<release-name>" --store fodRelease
fcli fod sast-scan list --release "::fodRelease::releaseId"
```

---

## Step 2 – Get Details for a Single Scan

```shell
fcli fod sast-scan get --scan-id <scan-id>
```

To get machine-readable output (useful for scripting):

```shell
fcli fod sast-scan get --scan-id <scan-id> --output json
```

Key fields in the JSON response:

| Field | Description |
|-------|-------------|
| `scanId` | Unique scan identifier |
| `status` | Current scan status (see table below) |
| `startedDateTime` | When the scan started |
| `completedDateTime` | When the scan finished (if terminal) |
| `issueCountCritical` | Critical findings count |
| `issueCountHigh` | High findings count |
| `issueCountMedium` | Medium findings count |
| `issueCountLow` | Low findings count |

---

## Scan Status Values

| Status | Meaning |
|--------|---------|
| `Queued` | Scan is waiting for a sensor |
| `In_Progress` | Scan is actively running |
| `Completed` | Scan finished successfully |
| `Failed` | Scan encountered an error |
| `Cancelled` | Scan was cancelled |
| `Suspended` | Scan is paused |

---

## Step 3 – Wait for Completion (Preferred)

Use `wait-for` to block until the scan reaches a terminal state:

```shell
fcli fod sast-scan wait-for --scan-id <scan-id> \
  --interval 30s \
  --timeout 90m
```

If you used `--store fodScan` during scan submission, reference it directly:

```shell
fcli fod sast-scan wait-for ::fodScan:: \
  --interval 30s \
  --timeout 90m
```

`wait-for` exits with a **non-zero exit code** if the scan does not complete successfully — ideal for breaking a CI/CD pipeline on failure.

Check all available wait conditions:

```shell
fcli fod sast-scan wait-for -h
```

---

## Step 4 – Polling Fallback (when `wait-for` is unavailable)

### Bash / shell

```bash
#!/bin/bash
set -e
SCAN_ID=456
TIMEOUT=1800   # seconds
ELAPSED=0

while [ $ELAPSED -lt $TIMEOUT ]; do
  STATUS=$(fcli fod sast-scan get --scan-id "$SCAN_ID" --output json | jq -r '.status')
  echo "$(date -u) status=$STATUS"
  case "$STATUS" in
    Completed) echo "Scan completed successfully."; exit 0 ;;
    Failed|Cancelled) echo "Scan ended with status: $STATUS"; exit 1 ;;
  esac
  sleep 30
  ELAPSED=$((ELAPSED + 30))
done

echo "Timeout waiting for scan $SCAN_ID" >&2
exit 1
```

### PowerShell

```powershell
$scanId  = 456
$timeout = 1800   # seconds
$start   = Get-Date

while (((Get-Date) - $start).TotalSeconds -lt $timeout) {
    $scan   = fcli fod sast-scan get --scan-id $scanId --output json | ConvertFrom-Json
    $status = $scan.status
    Write-Host "$(Get-Date -Format u)  status=$status"

    switch ($status) {
        'Completed' { Write-Host "Scan $scanId completed.";              exit 0 }
        'Failed'    { Write-Error "Scan $scanId failed.";                exit 1 }
        'Cancelled' { Write-Error "Scan $scanId was cancelled.";         exit 1 }
    }
    Start-Sleep -Seconds 30
}

Write-Error "Timeout waiting for scan $scanId after $timeout seconds."
exit 1
```

---

## Complete Pipeline Snippet

```shell
#!/bin/bash
set -e
APP_NAME="MyApp"
BRANCH=$(git rev-parse --abbrev-ref HEAD | tr '/' '-')

# Submit scan (stores scan reference as fodScan)
fcli fod sast-scan start \
  --release "${APP_NAME}:${BRANCH}" \
  --file package.zip \
  --notes "CI scan on ${BRANCH}" \
  --store fodScan

# Wait for results (up to 90 minutes)
fcli fod sast-scan wait-for ::fodScan:: --interval 30s --timeout 90m

# Show summary
fcli fod sast-scan get ::fodScan::scanId
```

---

## Environment Variables

| Variable | Description |
|----------|-------------|
| `FCLI_DEFAULT_FOD_RELEASE` | Default release (`app:release`) – avoids repeating `--release` |

---

## Troubleshooting

| Problem | Solution |
|---------|---------|
| Scan stuck in `Queued` | Check FoD portal for sensor availability or subscription issues |
| Scan stuck in `In_Progress` | Check FoD portal; increase `--timeout`; confirm no sensor errors |
| Authentication error | Re-run `fcli fod session login` or pass `--session <name>` |
| `scanId` unknown | Run `fcli fod sast-scan list --release "<app>:<release>"` to retrieve it |
