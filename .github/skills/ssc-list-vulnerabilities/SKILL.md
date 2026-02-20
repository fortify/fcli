---
name: ssc-list-vulnerabilities
description: "List, filter, count, and review security vulnerabilities for a Fortify SSC application version. Use this skill when a developer wants to see scan findings in SSC, apply filter sets, filter by severity or category, export to SARIF or other formats, or evaluate a security policy build gate."
argument-hint: "<app-name>:<version-name> [--filterset <name>] [--severity Critical|High|Medium|Low]"
---

## Overview

After scan results are processed in SSC, this skill helps you explore findings: listing, filtering by filter sets, summarizing counts, and exporting vulnerabilities for a given application version.

## Prerequisites

- Active SSC session (`/ssc-authenticate` first)
- At least one processed scan artifact on the target application version

---

## Step 1 – List All Issues

```shell
fcli ssc issue list \
  --appversion "<app-name>:<version-name>"
```

---

## Step 2 – List Available Filter Sets

Filter sets define what issues are shown (similar to SSC's UI filter set selector). List the available ones:

```shell
fcli ssc issue list-filtersets \
  --appversion "<app-name>:<version-name>"
```

Apply a specific filter set:

```shell
fcli ssc issue list \
  --appversion "<app-name>:<version-name>" \
  --filterset "Security Auditor View"
```

---

## Step 3 – Filter with Queries

```shell
# Critical and High issues only
fcli ssc issue list \
  --appversion "<app-name>:<version-name>" \
  -q "friority=='Critical' || friority=='High'"

# Open (not suppressed, not removed) issues
fcli ssc issue list \
  --appversion "<app-name>:<version-name>" \
  -q "suppressed==false && removed==false"

# Newly introduced issues (not previously seen)
fcli ssc issue list \
  --appversion "<app-name>:<version-name>" \
  -q "foundDate!=null && #date(foundDate) > #now('-7d')"

# SQL Injection issues
fcli ssc issue list \
  --appversion "<app-name>:<version-name>" \
  -q "issueName=='SQL Injection'"

# Issues in a specific file
fcli ssc issue list \
  --appversion "<app-name>:<version-name>" \
  -q "primaryLocation matches '.*UserController.*'"
```

---

## Step 4 – Count Issues by Group

```shell
# Count by friority (severity)
fcli ssc issue count \
  --appversion "<app-name>:<version-name>" \
  --group-by-field friority

# Count by category
fcli ssc issue count \
  --appversion "<app-name>:<version-name>" \
  --group-by-field issueName
```

Review available grouping fields:

```shell
fcli ssc issue list-groups \
  --appversion "<app-name>:<version-name>"
```

---

## Step 5 – Get Details for a Specific Issue

```shell
# List issues as JSON to find the issue ID
fcli ssc issue list \
  --appversion "<app-name>:<version-name>" \
  -q "issueName=='SQL Injection'" \
  -o json
```

---

## Step 6 – Export to SARIF or Other Formats

```shell
# Export as SARIF (uses fcli ssc action)
fcli ssc action run sarif-export \
  --appversion "<app-name>:<version-name>" \
  --output findings.sarif.json

# GitHub Code Scanning SARIF
fcli ssc action run github-sast-report \
  --appversion "<app-name>:<version-name>" \
  --output github-code-scanning.sarif

# GitLab SAST report
fcli ssc action run gitlab-sast-report \
  --appversion "<app-name>:<version-name>" \
  --output gl-sast-report.json
```

List all available SSC actions (including export formats):

```shell
fcli ssc action list
```

---

## Step 7 – Application Version Summary

Get a quick Markdown-formatted summary of the application version:

```shell
fcli ssc action run appversion-summary \
  --appversion "<app-name>:<version-name>"
```

---

## Step 8 – Raw Count for Policy Evaluation

```shell
# Count Critical+High open issues for a build gate
CRITICAL_HIGH=$(fcli ssc issue list \
  --appversion "<app-name>:<version-name>" \
  -q "(friority=='Critical' || friority=='High') && suppressed==false && removed==false" \
  -o 'expr={id}\n' | wc -l)

echo "Critical/High issues: $CRITICAL_HIGH"
if [ "$CRITICAL_HIGH" -gt 0 ]; then
  echo "FAIL: Security policy violation – $CRITICAL_HIGH critical/high issues found"
  exit 1
fi
```

Or use the built-in policy action:

```shell
fcli ssc action run check-policy \
  --appversion "<app-name>:<version-name>"
```

---

## Output Formats

```shell
# Table (default)
fcli ssc issue list --appversion "<app>:<version>"

# JSON
fcli ssc issue list --appversion "<app>:<version>" -o json

# CSV
fcli ssc issue list --appversion "<app>:<version>" -o csv

# Custom expression: file + line + category
fcli ssc issue list --appversion "<app>:<version>" \
  -o 'expr=[{friority}] {issueName} at {primaryLocation}:{lineNumber}\n'
```

---

## Troubleshooting

| Problem | Solution |
|---------|---------|
| No issues returned | Verify artifact is processed: `fcli ssc artifact list --appversion "<app>:<version>"` |
| `appversion not found` | Check with `fcli ssc appversion list -q "application.name=='<app>'"` |
| Filter set not applied | Verify filter set name with `fcli ssc issue list-filtersets` |
| `friority` property not found | Property names are case-sensitive; use `fcli ssc issue list -o json-properties` to list available properties |
| Suppressed issues hidden | Add filter: `-q "suppressed==true"` to see suppressed-only, or omit suppressed filter for all |
