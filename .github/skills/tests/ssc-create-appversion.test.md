# Test: ssc-create-appversion

## Purpose
Verify the skill creates SSC application versions correctly with proper attribute handling.

## Test Cases

### Test 1: Create Version for Current Branch
**Prompt:** "Create an SSC app version for my current branch in the MyApp application"

**Expected commands:**
```shell
BRANCH=$(git rev-parse --abbrev-ref HEAD | tr '/' '-')
fcli ssc appversion create "MyApp:${BRANCH}" \
  --issue-template "Prioritized High Risk Issue Template" \
  --auto-required-attrs \
  --copy-from "MyApp:main" \
  --skip-if-exists \
  --store sscVersion
```

---

### Test 2: Check Existing Versions
**Prompt:** "Check if an SSC version called 'feature-x' exists in MyApp"

**Expected command:**
```shell
fcli ssc appversion list \
  -q "application.name=='MyApp' && name=='feature-x'" \
  -o 'expr={id}: {application.name}:{name}\n'
```

---

### Test 3: List Issue Templates
**Prompt:** "What issue templates are available in SSC?"

**Expected command:**
```shell
fcli ssc issue-template list
```

---

### Test 4: Fix Uncommitted Version
**Prompt:** "My SSC version shows committed: false, how do I fix it?"

**Expected command:**
```shell
fcli ssc appversion update "MyApp:feature-x" --committed true
```

---

## Validation Checklist

- [ ] `--auto-required-attrs` always included
- [ ] `--skip-if-exists` always included
- [ ] `--copy-from` mentioned for branch-based versions
- [ ] `--store` used for variable chaining
- [ ] Fix for `committed: false` documented
