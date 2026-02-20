# Test: ssc-list-vulnerabilities

## Purpose
Verify the skill correctly lists and filters SSC vulnerabilities with filter sets and SpEL queries.

## Test Cases

### Test 1: List All Issues
**Prompt:** "Show me the vulnerabilities in MyApp:main in SSC"

**Expected command:**
```shell
fcli ssc issue list --appversion "MyApp:main"
```

---

### Test 2: List Available Filter Sets
**Prompt:** "What filter sets are available for my SSC version?"

**Expected command:**
```shell
fcli ssc issue list-filtersets --appversion "MyApp:main"
```

---

### Test 3: Apply Filter Set
**Prompt:** "Show issues using the 'Security Auditor View' filter set"

**Expected command:**
```shell
fcli ssc issue list --appversion "MyApp:main" --filterset "Security Auditor View"
```

---

### Test 4: Count by Severity
**Prompt:** "Give me a count of vulnerabilities by severity in MyApp:main"

**Expected command:**
```shell
fcli ssc issue count --appversion "MyApp:main" --group-by-field friority
```

---

### Test 5: Export as SARIF
**Prompt:** "Export SSC findings for MyApp:main as SARIF"

**Expected command:**
```shell
fcli ssc action run sarif-export --appversion "MyApp:main" --output findings.sarif.json
```

---

### Test 6: Build Gate — Policy Check
**Prompt:** "Should my build pass based on the Fortify results in SSC?"

**Expected behavior:**
- Agent suggests `fcli ssc action run check-policy`
- Or produces a query to count Critical/High issues

---

## Validation Checklist

- [ ] Property name `friority` (not `severity`) used for SSC
- [ ] `issueName` (not `category`) used for issue category in SSC
- [ ] Filter sets explained and shown
- [ ] `fcli ssc action list` mentioned for available export actions
- [ ] Build gate / policy check pattern included
