# Developer Integration Guide: CI/CD Platform Integration

This guide is for developers building platform-specific integrations with fcli (e.g., GitHub Actions, Azure DevOps tasks, GitLab CI templates, shell scripts).

## Overview

Fcli provides two primary integration points for CI/CD platforms:
1. **`fcli tool setup` command** - Comprehensive tool installation and registration
2. **`fcli tool env` command** - Environment variable generation for installed tools

Platform integrations should handle **fcli bootstrap** (getting fcli itself) and then delegate tool installation to these commands.

## Fcli Bootstrap Strategy

### Why Platform Tools Must Bootstrap Fcli

Fcli cannot install itself (circular dependency). Platform integration tools must:
1. Resolve fcli version/path using platform-specific logic
2. Download/cache fcli if needed
3. Pass bootstrapped fcli to `fcli tool setup` command via `--self`

### Fcli Semantic Versioning (v3.x Support)

**Important:** Fcli GitHub releases include semantic version tags for major.minor patterns:
- Release `v3.6.1` includes tags: `v3.6.1`, `v3.6`, `v3`
- All three tags point to the same release assets
- Platform tools can download from `/v3/` or `/v3.6/` URL patterns reliably

**Resolution Strategy:**
```typescript
// Example: @fortify/setup bootstrap logic
const version = resolveVersion(); // e.g., "v3", "v3.6", "v3.6.1"
const downloadUrl = `https://github.com/fortify/fcli/releases/download/${version}/fcli-linux.tgz`;
// This works for all three patterns due to GitHub release tags
```

### The `--self` and `--self-type` Parameters

Pass bootstrapped fcli to `fcli tool setup` command using these parameters:

#### `--self <path>`
Path to bootstrapped fcli executable. This enables the action to use your pre-resolved fcli instead of attempting to install it.

#### `--self-type <stable|unstable>`
Classification of the bootstrapped fcli's stability:

**`stable` (default):**
- Fcli is from trusted/verified source: pre-installed, CI/CD tool cache, explicit path
- Action registers this fcli immediately and proceeds with version detection
- No version mismatch handling needed

**`unstable`:**
- Fcli was dynamically downloaded from URL (GitHub releases)
- Action treats as `--fcli-copy-from` source with version matching
- Subject to `--on-copy-version-mismatch` behavior (default: `warn`)
- Use case: Fresh downloads where version may not exactly match requested pattern

**Decision tree:**
```
Downloaded from GitHub releases? → unstable
From tool cache? → stable
Pre-installed in environment? → stable
Explicit user-provided path? → stable
```

## Integration Patterns

### Pattern 1: Shell Script Bootstrap

```bash
#!/bin/bash
# Example: fortify-setup.sh

# Detect or download fcli
if command -v fcli &> /dev/null; then
    FCLI_PATH=$(command -v fcli)
    FCLI_TYPE="stable"
else
    # Download from GitHub releases
    VERSION="${FCLI_VERSION:-v3}"
    FCLI_PATH="/tmp/fcli"
    curl -L "https://github.com/fortify/fcli/releases/download/${VERSION}/fcli-linux.tgz" | tar xz -C /tmp
    FCLI_TYPE="unstable"
fi

# Delegate to fcli tool setup command
"${FCLI_PATH}" tool setup \
    --self "${FCLI_PATH}" \
    --self-type "${FCLI_TYPE}" \
    --fod-version v3 \
    --sc-client-version v24.4
```

### Pattern 2: TypeScript/JavaScript Module (@fortify/setup)

```typescript
// Bootstrap fcli
const fcliPath = await bootstrapFcli({
    version: 'v3',
    useToolCache: true
});

// Determine stability based on resolution source
const fcliType = fcliPath.source === 'download' ? 'unstable' : 'stable';

// Delegate to fcli tool setup command
await runFortifySetup({
    self: fcliPath.path,
    selfType: fcliType,
    fodVersion: 'v3',
    scClientVersion: 'v24.4'
});
```

### Pattern 3: Platform Action/Task Wrapper

```yaml
# Example: GitHub Action
- name: Setup Fortify tools
  uses: fortify/fortify-setup@v1
  with:
    fcli-version: v3
    fod-version: v3
    sc-client-version: v24.4
```

Implementation:
1. Action downloads fcli (`fcli-version: v3`) from GitHub releases
2. Marks as `unstable` (fresh download)
3. Invokes: `fcli tool setup --tools fod:v3,sc-client:v24.4 --self /path/to/fcli`
4. Action outputs environment variables from `fcli tool env` command

## Why `@fortify/setup` Doesn't Use Fcli Tool Definitions

**Question:** Why doesn't `@fortify/setup` TypeScript module leverage fcli's tool definitions for version resolution?

**Answer:** Bootstrap chicken-and-egg problem:
1. Tool definitions are part of fcli
2. Fcli must be available to query tool definitions
3. But we're trying to bootstrap fcli itself

**Solution:** `@fortify/setup` uses simple version resolution:
- Exact versions: `v3.6.1` → download from `/v3.6.1/`
- Semantic patterns: `v3`, `v3.6` → download from `/v3/` or `/v3.6/` (relies on GitHub release tags)
- Latest: Queries GitHub API for latest release

Once fcli is bootstrapped, `fcli tool setup` command uses tool definitions for all other tools (FoD CLI, SC Client, etc).

## Environment Variable Generation

After tool installation, generate environment variables for CI/CD platform:

```bash
# GitHub Actions format
fcli tool env --format github --output-file "$GITHUB_ENV"

# Azure DevOps format  
fcli tool env --format azure

# GitLab CI format
fcli tool env --format gitlab --output-file build.env

# Shell format
eval "$(fcli tool env --format shell)"
```

## Version Resolution Best Practices

### For Platform Integrations

**Default to semantic versions:**
- Use `v3` (not `latest`) for fcli to get stable v3.x releases
- Use `v24` or `v24.4` for tools with yearly versioning (SC Client, FoD CLI)

**Semantic version benefits:**
- Predictable: Users get latest v3.x.y without surprises
- Stable: No breaking changes within major version
- Cacheable: `v3` key remains stable across minor/patch updates

**Latest version risks:**
- Unpredictable: v4.0.0 release could break workflows
- Cache invalidation: `latest` key changes frequently
- Harder to debug: "it worked yesterday" issues

### Handling Version Mismatch

When using `--self-type unstable`, the action may warn about version mismatches:

```
WARNING: fcli version 3.5.0 does not match requested pattern v3.6
```

**Options:**
1. `--on-copy-version-mismatch warn` (default) - Log warning, continue
2. `--on-copy-version-mismatch error` - Fail action
3. `--on-copy-version-mismatch ignore` - Silent

**Recommendation:** Keep default `warn` behavior. Users can override if needed.

## Tool Registration: `--require-latest` Flag

When using `--auto-detect` with semantic version patterns, consider `--require-latest`:

```bash
# Register pre-installed fcli, but require latest v3.x.y
fcli tool fcli register --auto-detect --version v3 --require-latest

# Without --require-latest:
# - Pre-installed v3.5.0 is accepted even if v3.6.1 is available in definitions
# With --require-latest:
# - Pre-installed v3.5.0 is rejected (exit code 5: VERSION_NOT_LATEST)
# - Action will then install v3.6.1
```

**Use cases:**
- Semantic version patterns (`v3`, `v24`, `v24.4`) where "latest matching" is expected
- Skip for exact versions (`v3.6.1`), `latest`, `auto`, or `preinstalled`

**The `fcli tool setup` command handles this automatically based on version pattern.**

## Air-Gapped Environments

Support offline environments using `--copy-from` parameters:

```bash
fcli tool setup \
    --fcli-copy-from /shared/binaries/fcli \
    --fod-copy-from /shared/binaries/FoDUploader.jar \
    --sc-client-copy-from /shared/binaries/ScanCentralClient.jar \
    --air-gapped true
```

**Requirements:**
- Pre-stage binaries in accessible location
- Version detection runs automatically on copy sources
- Use `--on-copy-version-mismatch` to control validation strictness

## Tool Cache Integration

Platform integrations can leverage CI/CD tool caches:

```typescript
// Example: @fortify/setup with GitHub Actions tool-cache
const cachedPath = tc.find('fcli', version);
if (cachedPath) {
    return { path: cachedPath, source: 'cache' };
}

const downloadPath = await downloadFcli(version);
const cachedPath = await tc.cacheDir(downloadPath, 'fcli', version);
return { path: cachedPath, source: 'cache' };
```

**Benefits:**
- Faster execution (no repeated downloads)
- Bandwidth savings
- Offline resilience

**Recommendation:** Mark cache-resolved fcli as `--self-type stable` (trusted source).

## Example Complete Integration: GitHub Action

```typescript
// src/index.ts
import { bootstrapFcli, runFortifySetup, runFortifyEnv } from '@fortify/setup';
import * as core from '@actions/core';

async function run() {
    try {
        // Bootstrap fcli
        const fcliPath = await bootstrapFcli({
            version: core.getInput('fcli-version') || 'v3',
            useToolCache: true
        });

        // Determine stability
        const fcliType = fcliPath.source === 'download' ? 'unstable' : 'stable';

        // Install tools
        await runFortifySetup({
            self: fcliPath.path,
            selfType: fcliType,
            fodVersion: core.getInput('fod-version'),
            scClientVersion: core.getInput('sc-client-version')
        });

        // Generate environment variables
        await runFortifyEnv({
            format: 'github',
            outputFile: process.env.GITHUB_ENV
        });

    } catch (error) {
        core.setFailed(error.message);
    }
}

run();
```

## Testing Platform Integrations

Verify your integration handles:

1. **Fresh download:** Fcli not installed, downloads from GitHub
2. **Pre-installed:** Fcli in PATH, uses existing version
3. **Tool cache:** Fcli in platform cache, reuses cached version
4. **Version mismatch:** Requested v3.6, found v3.5 pre-installed
5. **Air-gapped:** All binaries from `--copy-from` sources
6. **Multiple tools:** Fcli + FoD CLI + SC Client in single setup

## Troubleshooting

**Q: Action fails with "fcli not found"**  
A: Forgot to pass `--self`? Platform integration must bootstrap fcli first.

**Q: Version mismatch warnings**  
A: Using `--self-type unstable` with semantic version pattern. Expected behavior. Use `--on-copy-version-mismatch error` if strict validation required.

**Q: "Cannot install tool fcli"**  
A: Circular dependency. Fcli cannot install itself. Use `--self` parameter.

**Q: GitHub download fails from `/v3/` URL**  
A: Verify fcli release includes semantic version tags. Report if missing.

## Summary

Platform integration checklist:
- [ ] Bootstrap fcli using platform-specific logic
- [ ] Leverage fcli semantic version tags (`v3`, `v3.6`) for downloads
- [ ] Pass bootstrapped fcli via `--self` and `--self-type`
- [ ] Delegate tool installation to `fcli tool setup` command
- [ ] Generate environment variables via `fcli tool env` command
- [ ] Support tool cache integration where available
- [ ] Handle air-gapped environments via `--copy-from` parameters
- [ ] Use semantic versions by default (not `latest`)

For complete examples, see:
- `@fortify/setup` TypeScript module: `/fortify-setup-js/`
- Shell script examples: `fortify-setup.sh`, `fortify-setup.ps1`
- Command implementations: `fcli tool setup`, `fcli tool env`
