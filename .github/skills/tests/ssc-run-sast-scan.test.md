# Test: ssc-run-sast-scan

## Purpose
Verify the skill correctly guides through SC-SAST scan submission and SSC artifact processing.

## Test Cases

### Test 1: Full Scan Workflow
**Prompt:** "Run a SAST scan on my Maven project and publish results to SSC version MyApp:main"

**Expected commands (in order):**
```shell
scancentral package --build-tool mvn -o package.zip

fcli sc-sast scan start \
  --controller-url https://scancentral.example.com/scancentral-ctrl \
  -f package.zip \
  --publish-to "MyApp:main" \
  --store scSastScan

fcli sc-sast scan wait-for ::scSastScan:: --timeout 90m
```

---

### Test 2: Check Available Sensor Pools
**Prompt:** "Which sensor pools are available for SC-SAST scanning?"

**Expected command:**
```shell
fcli sc-sast sensor-pool list
```

---

### Test 3: Wait for SSC Artifact Processing
**Prompt:** "The scan published to SSC but I need to wait for SSC to process the artifact"

**Expected commands:**
```shell
fcli ssc artifact list --appversion "MyApp:main" -q 'scanTypes matches "\bSCA\b"' --store sscArtifact
fcli ssc artifact wait-for ::sscArtifact:: --until PROCESS_COMPLETE
```

---

### Test 4: Approve Artifact (If Required)
**Prompt:** "The SSC artifact needs approval before results are visible"

**Expected command:**
```shell
fcli ssc artifact approve ::sscArtifact::id
```

---

## Validation Checklist

- [ ] `--publish-to` always used in `sc-sast scan start`
- [ ] `--store` used for variable chaining
- [ ] Both `sc-sast scan wait-for` AND `ssc artifact wait-for` patterns shown
- [ ] Controller URL mentioned (env var `FCLI_DEFAULT_SC_SAST_URL` suggested)
