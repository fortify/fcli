# Design Discussion: --require-latest Flag Behavior

## Current Implementation (Opt-In)

### Current Behavior
The `--require-latest` flag is an **opt-in** feature for `fcli tool <name> register --auto-detect`:

```bash
# Without --require-latest (current default)
fcli tool fcli register --auto-detect --version v3
# Result: Accepts any pre-installed version matching v3 pattern (v3.5.0, v3.6.1, etc.)

# With --require-latest (opt-in)
fcli tool fcli register --auto-detect --version v3 --require-latest
# Result: Only accepts pre-installed version if it's the latest v3.x.y in tool definitions
#         Exit code 5 (VERSION_NOT_LATEST) if v3.5.0 found but v3.6.1 is latest
```

### Current Usage Pattern
The `fortify-setup.yaml` action automatically adds `--require-latest` for semantic version patterns:

```yaml
# Line 220-231 of fortify-setup.yaml
- if: ${tool.version != 'skip' && tool.path == null}
  var.set:
    tool.registerCmd: tool ${tool.name} register --auto-detect
    tool.versionIsSemantic: ${tool.version != 'auto' && tool.version != 'latest' && tool.version != 'preinstalled' && tool.version.matches('^v?\\d+(\\.\\d+)?$')}

- if: ${tool.version != 'skip' && tool.path == null && tool.version != 'auto' && tool.version != 'latest' && tool.version != 'preinstalled'}
  var.set:
    tool.registerCmd: ${tool.registerCmd} --version ${tool.version}

- if: ${tool.version != 'skip' && tool.path == null && tool.versionIsSemantic}
  var.set:
    tool.registerCmd: ${tool.registerCmd} --require-latest
```

**Logic:**
- Semantic version patterns (`v3`, `v24`, `v24.4`): Add `--require-latest`
- Exact versions (`v3.6.1`): Do NOT add `--require-latest` (no ambiguity)
- Special keywords (`latest`, `auto`, `preinstalled`): Do NOT add `--require-latest`

### Rationale for Current Design
1. **User control:** Explicit opt-in gives users choice
2. **Backward compatibility:** Doesn't break existing workflows expecting "any matching version"
3. **Clear intent:** Flag name makes behavior obvious

---

## Alternative Design (Opt-Out)

### Proposed Behavior
Make `--require-latest` the **default** for semantic version patterns, with opt-out via new flag:

```bash
# New default behavior (for semantic patterns)
fcli tool fcli register --auto-detect --version v3
# Result: Only accepts pre-installed version if it's latest v3.x.y in definitions
#         Exit code 5 if outdated version found

# New opt-out flag
fcli tool fcli register --auto-detect --version v3 --allow-any-matching
# Result: Accepts any pre-installed version matching v3 pattern (current default)
```

### When Auto-Apply (New Default)
Only for semantic version patterns where "latest matching" is implied:
- `v3`, `v24` (major version)
- `v3.6`, `v24.4` (major.minor version)

**Do NOT auto-apply for:**
- Exact versions: `v3.6.1` (already unambiguous)
- Keywords: `latest` (already implies latest), `auto` (accepts any), `preinstalled` (explicit intent)

### Implementation Changes
**AbstractToolRegisterCommand.java:**
```java
// Add new option
@Option(names = "--allow-any-matching", 
        descriptionKey = "allow-any-matching.desc")
private boolean allowAnyMatching = false;

// Modify validation logic
private void validateVersion() {
    if (isSemanticVersion(version) && !allowAnyMatching && !isLatestMatchingVersion()) {
        throw new FcliSimpleException(
            "Pre-installed version " + detectedVersion + 
            " does not match latest available " + latestVersion +
            " (use --allow-any-matching to accept any matching version)",
            ExitCode.VERSION_NOT_LATEST
        );
    }
}

private boolean isSemanticVersion(String version) {
    return version != null 
        && !version.equals("latest") 
        && !version.equals("auto")
        && !version.equals("preinstalled")
        && version.matches("^v?\\d+(\\.\\d+)?$");
}
```

**fortify-setup.yaml:**
```yaml
# Simplified logic - no longer needs to add --require-latest
- if: ${tool.version != 'skip' && tool.path == null}
  var.set:
    tool.registerCmd: tool ${tool.name} register --auto-detect

- if: ${tool.version != 'skip' && tool.path == null && tool.version != 'auto' && tool.version != 'latest' && tool.version != 'preinstalled'}
  var.set:
    tool.registerCmd: ${tool.registerCmd} --version ${tool.version}

# Only add opt-out flag if user explicitly requested lenient behavior
# (Could add new action parameter: --allow-any-matching-versions: false)
```

---

## Comparison

| Aspect | Current (Opt-In) | Proposed (Opt-Out) |
|--------|------------------|-------------------|
| **Default for `v3` pattern** | Accept any v3.x.y | Require latest v3.x.y |
| **User intent clarity** | "Latest" must be explicit via flag | "Latest" is implicit in semantic pattern |
| **Surprise factor** | Low (accepts anything) | Medium (rejects outdated versions) |
| **Security posture** | Weaker (may use outdated tools) | Stronger (encourages latest versions) |
| **Backward compatibility** | N/A (current behavior) | Breaking change (requires migration) |
| **Action complexity** | Requires semantic version detection | Simpler (default behavior) |
| **Opt-out complexity** | Simple (omit flag) | Requires new flag `--allow-any-matching` |
| **CI/CD integration** | Action must add `--require-latest` | Action uses default, optionally adds opt-out |

---

## User Impact Analysis

### Scenario 1: Fresh CI/CD Environment
**No pre-installed tools, action installs everything**

- Current: No difference (nothing to register)
- Proposed: No difference (nothing to register)
- **Impact:** None

### Scenario 2: Pre-installed Outdated Tool
**CI/CD image has fcli v3.5.0, latest is v3.6.1**

- Current: User specifies `--fcli-version v3` → accepts v3.5.0 (may be outdated)
- Proposed: User specifies `--fcli-version v3` → rejects v3.5.0, installs v3.6.1
- **Impact:** Positive (ensures latest), but could surprise users expecting "any v3.x.y"

### Scenario 3: Intentionally Pinned Older Version
**CI/CD image has fcli v3.5.0 for compatibility testing**

- Current: User specifies `--fcli-version v3` → accepts v3.5.0
- Proposed: User specifies `--fcli-version v3 --allow-any-matching` → accepts v3.5.0
- **Impact:** Minor (requires flag), but user should use exact version `v3.5.0` instead

### Scenario 4: Exact Version
**User specifies exact version: `v3.6.1`**

- Current: Accepts only v3.6.1
- Proposed: Accepts only v3.6.1
- **Impact:** None (behavior unchanged)

---

## Recommendation

### Analysis

**DECISION MADE:** After review, the simplest approach is to **always apply --require-latest** (except for `auto` and `preinstalled`).

**Rationale:**
1. **Semantic alignment:** All version patterns (`v3`, `latest`, `v3.6.1`) should get the latest matching version
2. **Simplicity:** No need to detect "semantic" vs "exact" versions - they all behave consistently
3. **Security:** Ensures users get latest versions with bug fixes and security patches
4. **Implementation:** Much simpler - just always add the flag for version-filtered registrations

**Why This Works:**
- `latest` is semantically identical to `v3` (both are "patterns" that match multiple versions)
- Even "exact" versions like `v3.6.1` benefit from validation (reject v3.6.0 if found)
- Only `auto` (accept any) and `preinstalled` (use what's there) should skip the validation

**Previous Analysis (Opt-Out Design):**
The previous recommendation explored opt-out behavior with `--allow-any-matching` flag, but this adds unnecessary complexity. Always applying `--require-latest` is simpler and achieves the same goal.

### Implemented Solution

**Current behavior (as of this change):**
```yaml
# fortify-setup.yaml now always adds --require-latest
- if: ${tool.version != 'auto' && tool.version != 'preinstalled'}
  var.set:
    tool.registerCmd: ${tool.registerCmd} --version ${tool.version} --require-latest
```

**This means:**
- `--version v3`: Requires latest v3.x.y
- `--version latest`: Requires absolute latest version
- `--version v3.6.1`: Requires exactly v3.6.1 (and validates it's the latest matching that exact pattern)
- `--version auto`: Accepts any installed version (no `--require-latest`)
- `--version preinstalled`: Accepts any installed version (no `--require-latest`)

**Migration Impact:**
- **Low risk:** Most users want latest versions anyway
- **Escape hatch:** Users who need "any matching version" can use `--version auto`
- **Clearer semantics:** Version pattern always means "latest matching this pattern"

---

## Documentation Needs

Regardless of chosen approach, document:

1. **Current behavior clearly:**
   - `--require-latest` is opt-in for semantic version patterns
   - Without flag, any matching version accepted
   - fortify-setup action automatically adds flag for semantic patterns

2. **Semantic version expectations:**
   - `v3` means "latest v3.x.y" in most contexts, but "any v3.x.y" without `--require-latest`
   - Exact versions (`v3.6.1`) avoid ambiguity

3. **Security considerations:**
   - Recommend using `--require-latest` for production environments
   - Explain risks of accepting outdated versions

4. **Exit codes:**
   - Document exit code 5 (VERSION_NOT_LATEST) behavior

5. **Workarounds:**
   - If user wants "any matching" behavior, omit `--require-latest` (current)
   - If user wants "latest matching" behavior, add `--require-latest` (current)
   - Future: use `--allow-any-matching` to opt out of default strict behavior

---

## Questions for Decision

1. **Is the current opt-in behavior causing user confusion?**
   - Do users expect `v3` to mean "any v3.x.y" or "latest v3.x.y"?
   - Have there been issues with outdated tools in CI/CD?

2. **Is backward compatibility more important than semantic alignment?**
   - How many existing workflows would break with opt-out default?
   - Is the migration effort justified by improved semantics?

3. **Should we prioritize security (latest versions) over flexibility?**
   - Are there legitimate use cases for "any matching version"?
   - Should users be forced to opt into accepting outdated versions?

4. **What timeline is acceptable for a breaking change?**
   - Can we make this change in v4.0.0?
   - Should we provide gradual migration path via config option?

---

## Next Steps

1. **Gather user feedback:**
   - Survey CI/CD integration users about expectations
   - Analyze existing workflows for impact

2. **Improve documentation immediately (Option B):**
   - Update developer-integration-guide.md with current behavior
   - Document `--require-latest` flag prominently
   - Clarify semantic version expectations

3. **Consider config option (Option C):**
   - Implement `tool.require-latest-by-default` configuration
   - Monitor adoption and feedback

4. **Plan for v4.0.0 (Option A):**
   - If feedback supports change, make opt-out default
   - Provide clear migration guide
   - Update all documentation and examples

---

## References

- AbstractToolRegisterCommand.java: Lines 64, 132
- fortify-setup.yaml: Lines 220-231
- Developer Integration Guide: doc-resources/developer-integration-guide.md
- TODO Documentation: TODO-documentation-enhancements.md
