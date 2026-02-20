# Test: fod-list-vulnerabilities

## Purpose
Verify the skill correctly lists, filters, and summarizes FoD vulnerabilities.

## Test Cases

### Test 1: List All Open Vulnerabilities
**Prompt:** "Show me the security issues in my MyApp:main FoD release"

**Expected command:**
```shell
fcli fod issue list --release "MyApp:main"
```

---

### Test 2: Filter by Severity
**Prompt:** "Show me only Critical and High issues in MyApp:main"

**Expected command:**
```shell
fcli fod issue list \
  --release "MyApp:main" \
  -q "severityString=='Critical' || severityString=='High'"
```

---

### Test 3: Export as SARIF
**Prompt:** "Export the FoD findings for MyApp:main as a SARIF file"

**Expected command:**
```shell
fcli fod action run sarif-export \
  --release "MyApp:main" \
  --output findings.sarif.json
```

---

### Test 4: Count for Build Gate
**Prompt:** "How many Critical issues are there? Should I fail the build?"

**Expected behavior:**
- Agent produces count command or policy action
- Mentions `fcli fod action run check-policy` or similar

---

### Test 5: New Issues Only
**Prompt:** "Show me only new issues introduced in the latest scan"

**Expected command:**
```shell
fcli fod issue list --release "MyApp:main" -q "status=='New'"
```

---

## Validation Checklist

- [ ] SpEL expressions use double quotes for string values
- [ ] Severity filter uses `severityString` property name
- [ ] SARIF export uses `fcli fod action run`
- [ ] Count/policy check mentioned for build gate use case
