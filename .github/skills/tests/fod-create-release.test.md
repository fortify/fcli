# Test: fod-create-release

## Purpose
Verify the skill creates FoD releases correctly, handles the skip-if-exists pattern, and supports branch-based naming.

## Test Cases

### Test 1: Create Release for Current Branch
**Prompt:** "Create a FoD release for my current branch in the MyApp application"

**Expected behavior:**
- Agent detects (or asks for) current branch name
- Produces a `fcli fod release create` command with `--skip-if-exists`
- Uses `--copy-state-from` from main/master

**Expected commands:**
```shell
BRANCH=$(git rev-parse --abbrev-ref HEAD | tr '/' '-')
fcli fod release create "MyApp:${BRANCH}" \
  --copy-state-from "MyApp:main" \
  --sdlc-status Development \
  --skip-if-exists \
  --store fodRelease
```

---

### Test 2: Check If Release Exists First
**Prompt:** "Check if an FoD release called 'feature-login' already exists in MyApp"

**Expected command:**
```shell
fcli fod release list --app MyApp -q "releaseName=='feature-login'" -o 'expr={releaseId}: {releaseName}\n'
```

---

### Test 3: Microservice Release
**Prompt:** "Create a release for the 'auth-service' microservice of MyApp"

**Expected command includes:**
```shell
fcli fod release create "MyApp:auth-service:<release-name>" --sdlc-status Development --skip-if-exists
```

---

### Test 4: Create Application + First Release
**Prompt:** "Create a new FoD application called NewProject with an initial release"

**Expected commands:**
```shell
fcli fod app create "NewProject" --type Web --business-criticality High --release-name main --sdlc-status Development --skip-if-exists
```

---

## Validation Checklist

- [ ] `--skip-if-exists` always present
- [ ] `--store` used for subsequent command chaining
- [ ] `--copy-state-from` mentioned for branch releases
- [ ] Branch name sanitization shown (`tr '/' '-'`)
