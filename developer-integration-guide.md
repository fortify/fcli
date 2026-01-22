# Developer Integration Guide: CI/CD Platform Integration

This guide is for developers building platform-specific integrations with fcli (e.g., GitHub Actions, Azure DevOps tasks, GitLab CI templates, shell scripts).

## Overview

Fcli provides two primary integration points for CI/CD platforms:
1. **`fcli tool env init` command** - Comprehensive tool installation and registration
2. **`fcli tool env <format>` commands** - Environment variable generation for installed tools

Platform integrations should handle **fcli bootstrap** (getting fcli itself) and then delegate tool installation to these commands.

## Fcli Bootstrap Strategy

### Why Platform Tools Must Bootstrap Fcli

Fcli cannot install itself (circular dependency). Platform integration tools must:
1. Resolve fcli version/path using platform-specific logic
2. Download/cache fcli if needed
3. Pass bootstrapped fcli to `fcli tool env init` command via `--self`

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

### The `--self` Parameter

Pass bootstrapped fcli to `fcli tool env init` command using the `--self` parameter:

#### `--self <path>`
Path to bootstrapped fcli executable. This enables fcli to potentially use `--copy-if-matching` logic when installing the fcli tool itself, copying from the bootstrapped version if the version matches the requested version, otherwise downloading.

**When to use:**
- Always pass `--self` when your platform integration has bootstrapped fcli
- Improves performance by avoiding redundant downloads when versions match
- Particularly useful in CI/CD environments with tool caches

#### Special Tool Specification: `fcli:self` and `fcli:bootstrapped`

When using `--self`, you can specify `fcli:self` or `fcli:bootstrapped` in the `--tools` list to register the bootstrapped fcli binary directly without searching PATH:

```bash
# Register the bootstrapped fcli from --self parameter (using fcli:self)
fcli tool env init \
  --self /path/to/bootstrapped/fcli \
  --tools fcli:self,sc-client:latest

# Or using fcli:bootstrapped (same behavior, different name)
fcli tool env init \
  --self /path/to/bootstrapped/fcli \
  --tools fcli:bootstrapped,sc-client:latest
```

**Naming:**
- Use `fcli:self` when calling `fcli tool env init` directly (matches the `--self` option)
- Use `fcli:bootstrapped` in platform integrations like `@fortify/setup` (matches the concept)
- Both are functionally identical - choose based on context and readability

This is particularly useful when:
- You've bootstrapped fcli but don't want it in PATH during registration
- You want explicit control over which fcli gets registered
- You're in containerized or isolated environments

**Requirements:**
- The `--self` path must point to an actual fcli executable or JAR file, not a wrapper script
- The fcli binary must be executable and respond to `--version` command
- Wrapper scripts that set environment variables (e.g., debug logging) may interfere with version detection

**Validation:** Specifying `fcli:self` or `fcli:bootstrapped` requires the `--self` option to be present, otherwise the command will fail with a clear error message.

## Integration Patterns

### Pattern 1: Shell Script Bootstrap

```bash
#!/bin/bash
# Example: fortify-setup.sh

# Detect or download fcli
if command -v fcli &> /dev/null; then
    FCLI_PATH=$(command -v fcli)
else
    # Download from GitHub releases
    VERSION="${FCLI_VERSION:-v3}"
    FCLI_PATH="/tmp/fcli"
    curl -L "https://github.com/fortify/fcli/releases/download/${VERSION}/fcli-linux.tgz" | tar xz -C /tmp
fi

# Delegate to fcli tool env init command
"${FCLI_PATH}" tool env init \
  --self "${FCLI_PATH}" \
  --tools fcli:self,fod-uploader:v3,sc-client:v24.4
```

### Pattern 2: TypeScript/JavaScript Module (@fortify/setup)

```typescript
// Bootstrap fcli
const fcliPath = await bootstrapFcli({
    version: 'v3',
    useToolCache: true
});

// Delegate to fcli tool env init command
await runFortifyEnv({
    args: ['init', '--self', fcliPath.path, '--tools', 'fcli:bootstrapped,fod-uploader:v3,sc-client:v24.4'],
    verbose: true
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
2. Invokes: `fcli tool env init --self /path/to/fcli --tools fcli:bootstrapped,fod-uploader:v3,sc-client:v24.4`
3. Action outputs environment variables from `fcli tool env <format>` commands

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

Once fcli is bootstrapped, `fcli tool env init` command uses tool definitions for all other tools (FoD CLI, SC Client, etc).

## Environment Variable Generation

After tool installation, generate environment variables for CI/CD platform:

```bash
# GitHub Actions format
fcli tool env github

# Azure DevOps format  
fcli tool env ado

# GitLab CI format
fcli tool env gitlab --file build.env

# Shell format
eval "$(fcli tool env shell)"
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

**The `fcli tool env init` command handles this automatically based on version pattern.**

## Air-Gapped Environments

Support offline environments using `--copy-if-matching` parameters:

```bash
fcli tool env init \
    --tools fcli:/shared/binaries/fcli,fod-uploader:/shared/binaries/FoDUploader.jar,sc-client:/shared/binaries/ScanCentralClient.jar \
    --preinstalled
```

**Requirements:**
- Pre-stage binaries in accessible location
- Version detection runs automatically on copy sources
- Copy only occurs if detected version matches requested version

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
import { runFortifyEnv } from '@fortify/setup';
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
        const tools = [];
        const fodVersion = core.getInput('fod-version');
        const scClientVersion = core.getInput('sc-client-version');
        if (fodVersion) tools.push(`fod-uploader:${fodVersion}`);
        if (scClientVersion) tools.push(`sc-client:${scClientVersion}`);
        
        await runFortifyEnv({
            args: ['init', '--self', fcliPath.path, '--self-type', fcliType, '--tools', tools.join(',')],
            verbose: true
        });

        // Generate environment variables
        await runFortifyEnv({
            args: ['github']
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
4. **Version mismatch:** Requested v3.6, found v3.5 pre-installed (should download)
5. **Air-gapped:** All binaries from `--copy-if-matching` sources
6. **Multiple tools:** Fcli + FoD CLI + SC Client in single setup

## Troubleshooting

**Q: Action fails with "fcli not found"**  
A: Forgot to pass `--self`? Platform integration must bootstrap fcli first.

**Q: "Cannot initialize tool fcli"**  
A: Circular dependency. Fcli cannot install itself. Use `--self` parameter.

**Q: GitHub download fails from `/v3/` URL**  
A: Verify fcli release includes semantic version tags. Report if missing.

## Summary

Platform integration checklist:
- [ ] Bootstrap fcli using platform-specific logic
- [ ] Leverage fcli semantic version tags (`v3`, `v3.6`) for downloads
- [ ] Pass bootstrapped fcli via `--self` and `--self-type`
- [ ] Delegate tool installation to `fcli tool env init` command
- [ ] Generate environment variables via `fcli tool env <format>` commands
- [ ] Support tool cache integration where available
- [ ] Handle air-gapped environments via `--copy-if-matching` parameters
- [ ] Use semantic versions by default (not `latest`)

For complete examples, see:
- `@fortify/setup` TypeScript module: `/fortify-setup-js/`
- Shell script examples: `fortify-setup.sh`, `fortify-setup.ps1`
- Command implementations: `fcli-core/fcli-tool/src/main/java/com/fortify/cli/tool/env/`
