# Test: fod-run-sast-scan

## Purpose
Verify the skill correctly guides through packaging source code and submitting a SAST scan to FoD.

## Test Cases

### Test 1: Full Scan Workflow
**Prompt:** "Run a SAST scan on my Java project and publish to FoD release MyApp:main"

**Expected commands (in order):**
```shell
# Install ScanCentral Client if not present
fcli tool sc-client install -v latest

# Package
scancentral package --build-tool mvn -o package.zip

# Submit
fcli fod sast-scan start \
  --release "MyApp:main" \
  --file package.zip \
  --store fodScan

# Wait (optional)
fcli fod sast-scan wait-for ::fodScan:: --timeout 90m
```

---

### Test 2: Check Scan Configuration First
**Prompt:** "Before running the scan, check if SAST scanning is configured on MyApp:main"

**Expected command:**
```shell
fcli fod sast-scan get-config --release "MyApp:main"
```

---

### Test 3: Set Up SAST Configuration
**Prompt:** "Configure SAST scanning for a Python project on FoD release MyApp:feature-branch"

**Expected commands:**
```shell
fcli fod release list-assessment-types --release "MyApp:feature-branch"
fcli fod sast-scan setup \
  --release "MyApp:feature-branch" \
  --assessment-type "Static+" \
  --frequency "Subscription" \
  --technology-type "Python"
```

---

### Test 4: Async Pipeline (no wait)
**Prompt:** "Submit a scan to FoD and don't wait for results – I'll check later"

**Expected behavior:**
- Agent omits `wait-for` command
- Shows how to check status later with `fcli fod sast-scan list`

---

## Validation Checklist

- [ ] `scancentral package` step always before `fcli fod sast-scan start`
- [ ] `--store` used so `wait-for` can reference scan by variable
- [ ] Technology type mapping table mentioned
- [ ] Scan setup (Step 2) mentioned for first-time run
- [ ] Pipeline script pattern shown
