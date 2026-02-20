---
name: fod-list-vulnerabilities
description: "List, filter, and review security vulnerabilities for a Fortify on Demand (FoD) release. Use this skill when a developer wants to see scan findings, filter by severity or status, export to SARIF or other formats, count issues for a build gate, or identify new versus existing issues."
argument-hint: "<app-name>:<release-name> [--severity Critical|High|Medium|Low] [--status Open|Closed]"
---

## Overview

After a SAST (or other) scan completes on FoD, this skill helps you explore the findings: listing, filtering, summarizing, and optionally exporting vulnerabilities for a given release.

## Prerequisites

- Active FoD session (`/fod-authenticate` first)
- At least one completed scan on the target release

---

## Step 1 – List All Open Vulnerabilities

```shell
fcli fod issue list \
  --release "<app-name>:<release-name>"
```

Default output is a table. Use `-o json` for programmatic processing.

---

## Step 2 – Filter by Severity

```shell
# Critical and High only
fcli fod issue list \
  --release "<app-name>:<release-name>" \
  -q "severityString=='Critical' || severityString=='High'"
```

| Severity | `severityString` value |
|----------|----------------------|
| Critical | `Critical` |
| High | `High` |
| Medium | `Medium` |
| Low | `Low` |

---

## Step 3 – Filter by Status

```shell
# Open issues only (not yet remediated)
fcli fod issue list \
  --release "<app-name>:<release-name>" \
  -q "status=='Open'"

# New (introduced in latest scan)
fcli fod issue list \
  --release "<app-name>:<release-name>" \
  -q "status=='New'"

# Accepted risks / false positives
fcli fod issue list \
  --release "<app-name>:<release-name>" \
  -q "status=='Risk Accepted' || status=='False Positive'"
```

---

## Step 4 – Filter by Category

```shell
# SQL Injection issues only
fcli fod issue list \
  --release "<app-name>:<release-name>" \
  -q "category=='SQL Injection'"

# List available categories
fcli fod issue list \
  --release "<app-name>:<release-name>" \
  -o json | jq '[.[].category] | unique | sort'
```

---

## Step 5 – Count / Summarize

```shell
# Count by severity
fcli fod issue list \
  --release "<app-name>:<release-name>" \
  -o json | jq 'group_by(.severityString) | map({severity: .[0].severityString, count: length})'
```

Or use the FoD REST API lookup for a quick rollup:

```shell
fcli fod release get "<app-name>:<release-name>" \
  -o 'expr=Critical: {currentAnalysisStatusTypeText}\nRating: {rating}\n'
```

---

## Step 6 – Export to SARIF (for IDE or PR integration)

```shell
# Export all issues as SARIF (requires fcli fod action)
fcli fod action run sarif-export \
  --release "<app-name>:<release-name>" \
  --output findings.sarif.json

# Export as GitHub-compatible SARIF for upload to Code Scanning
fcli fod action run github-sast-report \
  --release "<app-name>:<release-name>" \
  --output github-code-scanning.sarif
```

List all available export actions:

```shell
fcli fod action list
```

---

## Step 7 – Get Details on a Specific Issue

```shell
# The issue ID is shown in the list output
fcli fod issue list \
  --release "<app-name>:<release-name>" \
  -q "vulnId==<issue-id>" \
  -o json
```

---

## Step 8 – Bulk Update Issue Status (optional)

```shell
# Accept risk on all Low issues
fcli fod issue update \
  --release "<app-name>:<release-name>" \
  -q "severityString=='Low'" \
  --status "Risk Accepted" \
  --comment "Low severity accepted per security policy"
```

---

## Output Formats

```shell
# Table (default)
fcli fod issue list --release "<app>:<release>"

# JSON (machine-readable)
fcli fod issue list --release "<app>:<release>" -o json

# CSV for spreadsheet import
fcli fod issue list --release "<app>:<release>" -o csv

# Custom expression
fcli fod issue list --release "<app>:<release>" \
  -o 'expr=[{severityString}] {category} - {primaryLocation}\n'
```

---

## Examples: Common Queries

```shell
# Show only Critical/High open issues, save to file
fcli fod issue list \
  --release "MyApp:main" \
  -q "(severityString=='Critical' || severityString=='High') && status=='Open'" \
  --to-file critical-high-issues.json \
  -o json

# Count of new issues introduced in latest scan
fcli fod issue list \
  --release "MyApp:main" \
  -q "status=='New'" \
  -o 'expr={vulnId}\n' | wc -l
```

---

## Troubleshooting

| Problem | Solution |
|---------|---------|
| Empty results | Verify at least one scan has completed on the release |
| `release not found` | Check spelling: `fcli fod release list -q "releaseName=='<name>'"` |
| SpEL expression errors | Wrap in single quotes (shell) and use double quotes inside for strings |
| Export action not found | `fcli fod action list` to see available actions; may need `--from-zip` for custom actions |
