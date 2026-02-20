---
name: fod-create-release
description: "Create a new FoD application release for the current Git branch. Use this skill when a developer working on a new branch needs a corresponding FoD release to track scan results. Handles checking for an existing release, creating the release, copying state from another release, and setting up SAST scan configuration."
argument-hint: "<app-name>:<release-name> [--sdlc-status Development|QA|Production|Retired]"
---

## Overview

Each branch or feature that undergoes security scanning in FoD needs a **Release** (under an **Application**). This skill walks through automatically discovering whether the app and release already exist, creating them if not, and optionally configuring SAST scanning.

## Prerequisites

- Active FoD session (`/fod-authenticate` first)
- For microservice apps: the **Microservice** already exists or will be created

---

## Step 1 – Auto-Discover App Name and Release Name

Before asking the user for an app name or release name, attempt to determine them automatically in this priority order:

### Check `FCLI_DEFAULT_FOD_RELEASE` environment variable

fcli uses `FCLI_DEFAULT_FOD_RELEASE` as the standard default for the `--release` / `--rel` option across all FoD commands. If this variable is already set, use it directly and skip to Step 2.

```shell
# Bash / Linux / macOS
echo "${FCLI_DEFAULT_FOD_RELEASE}"

# PowerShell
$Env:FCLI_DEFAULT_FOD_RELEASE
```

The value may be either a **release ID** (numeric) or an **`app:release`** name. If it is a numeric ID, use it directly in subsequent commands. If it is `app:release`, split on `:` to obtain `APP_NAME` and `RELEASE_NAME`.

### Derive names from Git (fallback)

If `FCLI_DEFAULT_FOD_RELEASE` is not set, derive the names from the current Git repository:

```shell
# Bash / Linux / macOS
APP_NAME=$(basename -s .git $(git remote get-url origin 2>/dev/null) 2>/dev/null \
           || basename "$(git rev-parse --show-toplevel)")
RELEASE_NAME=$(git rev-parse --abbrev-ref HEAD | tr '/' '-')

# PowerShell
$repoUrl = git remote get-url origin 2>$null
$APP_NAME = if ($repoUrl) {
    ($repoUrl -replace '\.git$','') -replace '.*/',''
} else {
    Split-Path -Leaf (git rev-parse --show-toplevel)
}
$RELEASE_NAME = (git rev-parse --abbrev-ref HEAD) -replace '/','-'
```

> **Convention:** app name = repository name, release name = branch name (forward slashes replaced with hyphens for FoD compatibility).
>
> Confirm these derived names with the user before proceeding if running interactively.

---

## Step 2 – Check Whether the Release Already Exists

**If you have a release ID** (numeric value from `FCLI_DEFAULT_FOD_RELEASE`):

```shell
fcli fod release get <releaseId>
```

If the command succeeds, the release exists — skip to Step 6. If it fails with a not-found error, proceed to Step 3.

**If you have `app:release` names** (from env var or git-derived):

First confirm the **application** exists:

```shell
fcli fod app get "<app-name>"
```

If the application is not found, proceed to Step 3 (create application + release together).

If the application exists, check whether the **release** exists:

```shell
fcli fod release list \
  --app "<app-name>" \
  -q "releaseName=='<release-name>'" \
  -o 'expr={releaseId}: {releaseName}\n'
```

If output is non-empty, the release exists — note the `releaseId` and skip to Step 6. If output is empty, proceed to Step 4.

---

## Step 3 – Create the Application (if it does not exist)

Skip this step if your application already exists (confirmed in Step 2). Creating an application also creates the first release, so you can skip Step 6 after the application and release are created and the release ID is saved.

```shell
fcli fod app create "<app-name>" \
  --type Web \
  --business-criticality High \
  --release-name main \
  --sdlc-status Development \
  --auto-required-attrs \
  --skip-if-exists
```

Supported `--type` values: `Web`, `Mobile`, `Thick Client`, `Microservice`.

After creating the app+release, save the release ID to `FCLI_DEFAULT_FOD_RELEASE` so all subsequent fcli FoD commands in this session automatically target the correct release without needing `--release` on every command.

Only set the variable if it is not already defined:

```shell
# Bash / Linux / macOS
if [ -z "${FCLI_DEFAULT_FOD_RELEASE}" ]; then
  export FCLI_DEFAULT_FOD_RELEASE=$(fcli util variable contents fodRelease -o 'expr={releaseId}\n')
  echo "FCLI_DEFAULT_FOD_RELEASE set to ${FCLI_DEFAULT_FOD_RELEASE}"
fi

# PowerShell
if (-not $Env:FCLI_DEFAULT_FOD_RELEASE) {
  $Env:FCLI_DEFAULT_FOD_RELEASE = fcli util variable contents fodRelease -o 'expr={releaseId}\n'
  Write-Host "FCLI_DEFAULT_FOD_RELEASE set to $Env:FCLI_DEFAULT_FOD_RELEASE"
}
```

---

## Step 4 – Detect Copy-State Source

Copying audit state from a parent release is the default recommended behavior. Before creating the release, automatically determine which FoD release to copy state from.

### Find the default or parent branch in Git

```shell
# Bash / Linux / macOS
# Try the remote's HEAD branch first; fall back to common default branch names
COPY_FROM_BRANCH=$(git remote show origin 2>/dev/null | grep 'HEAD branch' | awk '{print $NF}')
if [ -z "$COPY_FROM_BRANCH" ]; then
  for b in main master develop; do
    if git show-ref --verify --quiet "refs/heads/$b" || git show-ref --verify --quiet "refs/remotes/origin/$b"; then
      COPY_FROM_BRANCH="$b"; break
    fi
  done
fi
COPY_FROM_BRANCH=$(echo "$COPY_FROM_BRANCH" | tr '/' '-')  # FoD-safe

# PowerShell
$remoteHead = git remote show origin 2>$null | Select-String 'HEAD branch'
$COPY_FROM_BRANCH = if ($remoteHead) {
    ($remoteHead.ToString().Trim() -replace '.*HEAD branch:\s*','')
} else { $null }
if (-not $COPY_FROM_BRANCH) {
    foreach ($b in @('main','master','develop')) {
        $ref = git show-ref --verify "refs/heads/$b" 2>$null
        if (-not $ref) { $ref = git show-ref --verify "refs/remotes/origin/$b" 2>$null }
        if ($ref) { $COPY_FROM_BRANCH = $b; break }
    }
}
$COPY_FROM_BRANCH = $COPY_FROM_BRANCH -replace '/','-'
```

If `COPY_FROM_BRANCH` equals `RELEASE_NAME` (you are already on the default branch), **skip copy-state** — there is no parent release to copy from.

### Verify the source release exists in FoD

```shell
fcli fod release list \
  --app "<app-name>" \
  -q "releaseName=='<copy-from-branch>'" \
  -o 'expr={releaseId}: {releaseName}\n'
```

- **Release found** → set `COPY_FROM_RELEASE="<app-name>:<copy-from-branch>"` and use `--copy-state-from` in Step 5.
- **Release not found** → inform the user. Ask whether to: (a) specify a different release to copy from, or (b) proceed without copying state. Do not silently skip — missing state copy may lose audit history.

---

## Step 5 – Create the Release

### Standard Application

**With copy-state** (recommended — `COPY_FROM_RELEASE` was found in Step 4):

```shell
fcli fod release create "<app-name>:<release-name>" \
  --copy-state-from "<copy-from-release>" \
  --sdlc-status Development \
  --skip-if-exists \
  --store fodRelease
```

**Without copy-state** (only when on the default branch, or source release does not exist in FoD):

```shell
fcli fod release create "<app-name>:<release-name>" \
  --sdlc-status Development \
  --skip-if-exists \
  --store fodRelease
```

- `--copy-state-from` copies issue audit state from an existing release so findings and audit decisions carry over from the parent branch.
- `--skip-if-exists` makes the command idempotent – safe to run in CI/CD.
- `--store fodRelease` saves the release details for use in subsequent commands.

### Microservice Application

**With copy-state** (recommended):

```shell
fcli fod release create "<app-name>:<microservice-name>:<release-name>" \
  --copy-state-from "<app-name>:<microservice-name>:<copy-from-branch>" \
  --sdlc-status Development \
  --skip-if-exists \
  --store fodRelease
```

**Without copy-state:**

```shell
fcli fod release create "<app-name>:<microservice-name>:<release-name>" \
  --sdlc-status Development \
  --skip-if-exists \
  --store fodRelease
```

### Persist the Release ID (if `FCLI_DEFAULT_FOD_RELEASE` is not already set)

After creating the release, save its ID to `FCLI_DEFAULT_FOD_RELEASE` so all subsequent fcli FoD commands in this session automatically target the correct release without needing `--release` on every command.

Only set the variable if it is not already defined:

```shell
# Bash / Linux / macOS
if [ -z "${FCLI_DEFAULT_FOD_RELEASE}" ]; then
  export FCLI_DEFAULT_FOD_RELEASE=$(fcli util variable contents fodRelease -o 'expr={releaseId}\n')
  echo "FCLI_DEFAULT_FOD_RELEASE set to ${FCLI_DEFAULT_FOD_RELEASE}"
fi

# PowerShell
if (-not $Env:FCLI_DEFAULT_FOD_RELEASE) {
  $Env:FCLI_DEFAULT_FOD_RELEASE = fcli util variable contents fodRelease -o 'expr={releaseId}\n'
  Write-Host "FCLI_DEFAULT_FOD_RELEASE set to $Env:FCLI_DEFAULT_FOD_RELEASE"
}
```

> **Note:** This sets the variable for the current shell session only. To persist it across sessions (e.g., in a CI/CD pipeline), write it to the pipeline's environment file or export mechanism (e.g., `$GITHUB_ENV`, `.env` file, or system environment).

---

## Step 6 – Verify

```shell
fcli fod release get "<app-name>:<release-name>"
```

Or, using the stored variable:

```shell
fcli fod release get ::fodRelease::releaseId
```

---

## Automating with Git Branch Name

```shell
#!/bin/bash
APP_NAME=$(basename -s .git $(git remote get-url origin 2>/dev/null) 2>/dev/null || basename "$(git rev-parse --show-toplevel)")
BRANCH=$(git rev-parse --abbrev-ref HEAD | tr '/' '-')

# Detect copy-state source: use remote HEAD branch, fall back to main/master/develop
COPY_FROM_BRANCH=$(git remote show origin 2>/dev/null | grep 'HEAD branch' | awk '{print $NF}' | tr '/' '-')
if [ -z "$COPY_FROM_BRANCH" ]; then
  for b in main master develop; do
    if git show-ref --verify --quiet "refs/remotes/origin/$b"; then
      COPY_FROM_BRANCH="$b"; break
    fi
  done
fi

COPY_FROM_FLAG=""
if [ -n "$COPY_FROM_BRANCH" ] && [ "$COPY_FROM_BRANCH" != "$BRANCH" ]; then
  # Verify the source release exists in FoD before using it
  COPY_FROM_ID=$(fcli fod release list --app "$APP_NAME" \
    -q "releaseName=='${COPY_FROM_BRANCH}'" \
    -o 'expr={releaseId}\n' 2>/dev/null)
  if [ -n "$COPY_FROM_ID" ]; then
    COPY_FROM_FLAG="--copy-state-from ${APP_NAME}:${COPY_FROM_BRANCH}"
  fi
fi

fcli fod release create "${APP_NAME}:${BRANCH}" \
  $COPY_FROM_FLAG \
  --sdlc-status Development \
  --skip-if-exists \
  --store fodRelease

if [ -z "${FCLI_DEFAULT_FOD_RELEASE}" ]; then
  export FCLI_DEFAULT_FOD_RELEASE=$(fcli util variable contents fodRelease -o 'expr={releaseId}\n')
fi
echo "Release ID: ${FCLI_DEFAULT_FOD_RELEASE}"
```

---

## Environment Variables

| Variable | Description |
|----------|-------------|
| `FCLI_DEFAULT_FOD_RELEASE` | Default release for all FoD `--release`/`--rel` options. Accepts a release ID (numeric) or `app:release` / `app:microservice:release` name. When set, Step 1 uses this value directly and skips git-based discovery. |
| `FCLI_DEFAULT_FOD_RELEASE_SDLC_STATUS` | Default SDLC status for new releases. |
| `FCLI_DEFAULT_FOD_APP` | Default application name or ID for FoD `--app` options. Can be used instead of deriving the app name from the git repository. |

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `FCLI_DEFAULT_FOD_RELEASE` is set but points to wrong release | Unset the variable or override it explicitly for this workflow |
| Git remote URL cannot be parsed | Set `APP_NAME` manually or via `FCLI_DEFAULT_FOD_APP`; confirm with the user |
| Detached HEAD — branch name is `HEAD` | Ask the user for the release name explicitly; do not use `HEAD` as a release name |
| Default branch cannot be determined from `git remote show origin` | Check git remote configuration; manually specify `COPY_FROM_BRANCH` or ask the user |
| Copy-state source release not found in FoD | Ask the user to specify a release to copy from, or confirm proceeding without copy-state |
| `Application not found` | Check application name spelling with `fcli fod app list -q "applicationName=='<name>'"` |
| `Release already exists` | Expected with `--skip-if-exists`; the existing release is returned |
| `Microservice not found` | Create microservice first: `fcli fod microservice create "<app>:<ms-name>"` |
| Permission errors | Ensure FoD user has **Create Release** permission on the application |
