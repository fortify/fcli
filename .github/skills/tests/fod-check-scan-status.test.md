# Test: fod-check-scan-status

## Purpose
Verify the skill correctly guides through checking and waiting on the status of an FoD SAST scan.

## Test Cases

---

### Test 1: List Scans for a Release
**Prompt:** "Show me all scans for the FoD release MyApp:main"

**Expected command:**
```shell
fcli fod sast-scan list --release "MyApp:main"
```

**Expected behavior:**
- Agent outputs the list with `scanId`, `status`, `startedDateTime`, and issue counts visible
- Agent identifies the most recent `scanId` if asked to proceed

---

### Test 2: Get Status of a Specific Scan
**Prompt:** "Check the status of FoD SAST scan 456"

**Expected commands:**
```shell
fcli fod sast-scan get --scan-id 456
```

**Expected behavior:**
- Agent reports `status` field (e.g., `In_Progress`, `Completed`, `Failed`)
- Agent explains meaning of the status

---

### Test 3: Wait for Scan Completion (stored scan)
**Prompt:** "Wait for FoD scan 456 to finish before continuing – timeout after 30 minutes"

**Expected command:**
```shell
fcli fod sast-scan wait-for --scan-id 456 --interval 30s --timeout 30m
```

**Expected behavior:**
- Agent uses `wait-for` (not a manual loop) since it's the preferred approach
- Agent notes non-zero exit code indicates failure

---

### Test 4: Wait Using Stored Variable Reference
**Prompt:** "The scan was submitted with --store fodScan. Wait for it to finish."

**Expected command:**
```shell
fcli fod sast-scan wait-for ::fodScan:: --interval 30s --timeout 90m
```

**Expected behavior:**
- Agent correctly uses `::fodScan::` variable syntax — no hardcoded scan ID

---

### Test 5: Full Pipeline Snippet
**Prompt:** "Give me a complete bash pipeline that submits a scan to FoD release MyApp:feature-x and waits for it to finish"

**Expected commands (in order):**
```shell
fcli fod sast-scan start \
  --release "MyApp:feature-x" \
  --file package.zip \
  --notes "CI scan on feature-x" \
  --store fodScan

fcli fod sast-scan wait-for ::fodScan:: --interval 30s --timeout 90m

fcli fod sast-scan get ::fodScan::scanId
```

**Expected behavior:**
- `--store fodScan` used for variable reference in wait-for
- Pipeline fails if `wait-for` exits non-zero

---

### Test 6: Async Pipeline (no wait)
**Prompt:** "Submit a scan but don't wait — I'll check it manually later"

**Expected behavior:**
- Agent omits `wait-for`
- Agent shows how to retrieve the scan ID later:

```shell
fcli fod sast-scan list --release "<app>:<release>"
```

---

## Validation Checklist

- [ ] `::fodScan::` variable syntax demonstrated correctly
- [ ] Non-zero exit on failure/cancelled/timeout
- [ ] `--interval` and `--timeout` flags present in `wait-for` examples
- [ ] Status value table (Queued / In_Progress / Completed / Failed / Cancelled / Suspended) mentioned or shown
- [ ] Troubleshooting guidance included for stuck/queued scans
