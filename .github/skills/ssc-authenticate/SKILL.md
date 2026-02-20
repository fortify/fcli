---
name: ssc-authenticate
description: "Log in to or log out of Fortify Software Security Center (SSC) using fcli. Use this skill when the user needs to authenticate to SSC before running scans, managing application versions, or reviewing vulnerabilities. Covers token-based auth (CIToken, AutomationToken), username/password, environment variable patterns, named sessions, and SC-SAST/SC-DAST URL configuration."
argument-hint: "[--session <name>] [--url <ssc-url>]"
---

## Overview

Most `fcli ssc`, `fcli sc-sast`, and `fcli sc-dast` commands require an active SSC session. A single `fcli ssc session login` creates the session used by all three product modules.

## Prerequisites

- `fcli` v3.x installed and on your `PATH` (verify: `fcli --version`)
- SSC URL and either:
  - A **token** (CIToken or AutomationToken) – recommended for automation
  - **Username + password** – for interactive/developer use
  - A **UnifiedLoginToken** – if your SSC is configured for unified auth

---

## Step 1 – Gather Credentials

| Scenario | Auth Type |
|----------|-----------|
| Developer / IDE | Username + password OR CIToken |
| CI/CD pipeline | AutomationToken or CIToken |
| Admin tasks | Username + password (full permissions) |

To create a token in SSC: **Administration → Token Management → New Token**. Choose `CIToken` for limited access or `AutomationToken` for broader access (review security implications with your SSC admin).

You can also create a token via fcli:

```shell
fcli ssc access-control create-token \
  --token-type CIToken \
  --expire-in 7d \
  -o 'expr=Token: {token}\n'
```

---

## Step 2 – Log In

### Option A: Username + Password

```shell
fcli ssc session login \
  --url https://ssc.example.com/ssc \
  --user <username> \
  --password <password>
```

### Option B: Token (CIToken / AutomationToken)

```shell
fcli ssc session login \
  --url https://ssc.example.com/ssc \
  --token <token-value>
```

### Option C: Environment Variables (recommended for pipelines)

```shell
export FCLI_DEFAULT_SSC_URL=https://ssc.example.com/ssc
export FCLI_DEFAULT_SSC_USER=<username>
export FCLI_DEFAULT_SSC_PASSWORD=<password>
# OR for token auth:
export FCLI_DEFAULT_SSC_TOKEN=<token-value>

fcli ssc session login
```

### Named Sessions

```shell
fcli ssc session login --ssc-session dev --url https://dev-ssc.example.com/ssc --user admin --password <pwd>
fcli ssc session login --ssc-session prod --url https://ssc.example.com/ssc --token <token>

# Use named session in any ssc/sc-sast/sc-dast command:
fcli ssc appversion list --ssc-session prod
fcli sc-sast scan list --ssc-session dev
```

---

## Step 3 – Verify

```shell
fcli ssc session list
```

Expected output: one row with status `ACTIVE` and the correct URL.

---

## Step 4 – Log Out

```shell
fcli ssc session logout
# With username/password login, providing credentials on logout explicitly revokes the generated token:
fcli ssc session logout --user <username> --password <password>
# Named session:
fcli ssc session logout --ssc-session <name>
```

> **Pipeline best practice:** Always run `logout` in a `finally`/`cleanup` step to revoke tokens and avoid exhausting SSC's active token limit.

---

## ScanCentral Context

If you plan to submit SC-SAST or SC-DAST scans, you may also need to provide the ScanCentral Controller URL. This can be included at login time:

```shell
fcli ssc session login \
  --url https://ssc.example.com/ssc \
  --user <user> --password <pwd> \
  --sc-sast-url https://scancentral.example.com/scancentral-ctrl
```

Or via environment variable: `FCLI_DEFAULT_SC_SAST_URL`.

---

## Environment Variables Reference

| Variable | Description |
|----------|-------------|
| `FCLI_DEFAULT_SSC_URL` | SSC base URL |
| `FCLI_DEFAULT_SSC_USER` | SSC username |
| `FCLI_DEFAULT_SSC_PASSWORD` | SSC password |
| `FCLI_DEFAULT_SSC_TOKEN` | Pre-existing token value |
| `FCLI_DEFAULT_SC_SAST_URL` | ScanCentral SAST Controller URL |

---

## Troubleshooting

| Problem | Solution |
|---------|---------|
| `401 Unauthorized` | Check URL trailing slash (omit `/`); verify credentials; check token expiry |
| Token limit exceeded | Revoke stale tokens: `fcli ssc access-control list-tokens` then `fcli ssc access-control revoke-token` |
| SSL errors | `fcli config truststore set --truststore <path>` |
| `CIToken` doesn't have permission | Switch to `AutomationToken` or use username/password |
