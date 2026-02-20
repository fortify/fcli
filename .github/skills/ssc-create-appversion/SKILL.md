---
name: ssc-create-appversion
description: "Create a new SSC application version for the current Git branch. Use this skill when a developer needs a corresponding SSC application version to track scan results. Handles checking for an existing version, creating it with required attributes, copying issue state from a template version, and committing it."
argument-hint: "<app-name>:<version-name> [--issue-template <template>]"
---

## Overview

Every set of scan results in SSC lives inside an **Application Version** (app:version). This skill creates a version for the current branch if one does not already exist, copies issue audit state from a reference version, and sets required attributes so the version is commit-ready.

Typical naming convention: `<app-name>:<git-branch>`.

## Prerequisites

- Active SSC session (`/ssc-authenticate` first)
- SSC `Application Creator` or `Security Lead` role (or `Administrator`)
- The **Application** already exists, or will be created in Step 1

---

## Step 1 – Check Whether the Version Already Exists

```shell
fcli ssc appversion list \
  -q "application.name=='<app-name>' && name=='<version-name>'" \
  -o 'expr={id}: {application.name}:{name}\n'
```

If found, note the `id` and skip to Step 4. If empty, continue to Step 2.

---

## Step 2 – Create the Application Version

### Basic Creation (copy state from `main`)

```shell
fcli ssc appversion create "<app-name>:<version-name>" \
  --issue-template "Prioritized High Risk Issue Template" \
  --auto-required-attrs \
  --copy-from "<app-name>:main" \
  --skip-if-exists \
  --store sscVersion
```

| Option | Purpose |
|--------|---------|
| `--auto-required-attrs` | Automatically set required attribute values (avoids "uncommitted" errors) |
| `--copy-from` | Copy issue template, rules, filters, and audit state from a reference version |
| `--skip-if-exists` | Idempotent – safe for CI/CD pipelines |
| `--store sscVersion` | Save version data for use in subsequent fcli commands |

### Without Copying State

```shell
fcli ssc appversion create "<app-name>:<version-name>" \
  --issue-template "Prioritized High Risk Issue Template" \
  --auto-required-attrs \
  --skip-if-exists \
  --store sscVersion
```

---

## Step 3 – List Available Issue Templates (if unsure)

```shell
fcli ssc issue-template list
```

---

## Step 4 – Verify the Version Was Created and Committed

```shell
fcli ssc appversion get "<app-name>:<version-name>"
```

A successfully created version will show `committed: true`. If `committed: false`, run:

```shell
fcli ssc appversion update "<app-name>:<version-name>" --committed true
```

---

## Step 5 – Set Custom Attributes (optional)

If your SSC instance requires custom attributes beyond the defaults:

```shell
# List required attributes
fcli ssc attribute list-definitions -q "required==true"

# Set a specific attribute value
fcli ssc attribute update "<app-name>:<version-name>" \
  --attrs "<attr-guid>=<value>"
```

---

## Automating with Git Branch Name

```shell
#!/bin/bash
APP_NAME="MyApp"
BRANCH=$(git rev-parse --abbrev-ref HEAD | tr '/' '-')

fcli ssc appversion create "${APP_NAME}:${BRANCH}" \
  --issue-template "Prioritized High Risk Issue Template" \
  --auto-required-attrs \
  --copy-from "${APP_NAME}:main" \
  --skip-if-exists \
  --store sscVersion

echo "AppVersion ID: $(fcli util variable contents sscVersion -o 'expr={id}\n')"
```

---

## Environment Variables

| Variable | Description |
|----------|-------------|
| `FCLI_DEFAULT_SSC_APPVERSION_CREATE_ISSUE_TEMPLATE` | Default issue template name |

---

## Troubleshooting

| Problem | Solution |
|---------|---------|
| `Application doesn't exist` | Create it first: `fcli ssc app create` is not a direct command – create with the first version using `fcli ssc appversion create "NewApp:1.0" ...` |
| `Version committed: false` | Run `fcli ssc appversion update --committed true` |
| `Required attribute missing` | Use `--auto-required-attrs` or set attributes manually via `fcli ssc attribute update` |
| `copy-from version not found` | Verify source version exists: `fcli ssc appversion list -q "application.name=='<app>'"` |
| Permission denied | Ensure user has `Application Creator` or `Administrator` role |
