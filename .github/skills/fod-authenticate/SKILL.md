---
name: fod-authenticate
description: "Log in to or log out of Fortify on Demand (FoD) using fcli. Use this skill when the user needs to authenticate to FoD before running scans, managing releases, or reviewing vulnerabilities. Covers PAT, client credentials, environment variable patterns, and named sessions."
argument-hint: "[--session <name>] [--tenant <tenant>]"
---

## Overview

Most `fcli fod` commands require an active FoD session. This skill creates or tears down that session using `fcli fod session login` / `fcli fod session logout`.

## Prerequisites

- `fcli` v3.x installed and on your `PATH` (verify: `fcli --version`)
- FoD tenant name, and either:
  - A **Personal Access Token (PAT)** – recommended for interactive/developer use
  - **Client credentials** (Client ID + Client Secret) – recommended for pipelines

---

## Step 1 – Gather Credentials

Determine which credential type to use:

| Scenario | Credential Type |
|----------|----------------|
| Developer / IDE | Personal Access Token (PAT) |
| CI/CD pipeline | Client credentials (Client ID + Secret) |

To create a PAT in FoD: **User Menu → Personal Access Tokens**.
To create client credentials: **Administration → Settings → API**. Note the client ID and secret values. Requires Security Lead (admin) permissions.

---

## Step 2 – Log In

### Option A: Personal Access Token (PAT)

```shell
fcli fod session login \
  --url https://ams.fortify.com \
  --tenant <your-tenant> \
  --user <your-username> \
  --password <your-PAT>
```

### Option B: Client Credentials

```shell
fcli fod session login \
  --url https://ams.fortify.com \
  --client-id <client-id> \
  --client-secret <client-secret>
```

### Option C: Environment Variables (recommended for pipelines)

Set these variables in your environment or CI/CD secrets, then run `fcli fod session login` with no credential flags:

```shell
# Required
export FCLI_DEFAULT_FOD_URL=https://ams.fortify.com
export FCLI_DEFAULT_FOD_TENANT=<your-tenant>

# PAT login
export FCLI_DEFAULT_FOD_USER=<your-email>
export FCLI_DEFAULT_FOD_PASSWORD=<your-PAT>

# OR client credentials login
export FCLI_DEFAULT_FOD_CLIENT_ID=<client-id>
export FCLI_DEFAULT_FOD_CLIENT_SECRET=<client-secret>

fcli fod session login
```

### Named Sessions (advanced)

Use named sessions when working with multiple tenants or multiple credential sets simultaneously:

```shell
fcli fod session login --fod-session dev --url https://ams.fortify.com \
  --tenant dev-tenant --user <user> --password <PAT>

fcli fod session login --fod-session prod --url https://ams.fortify.com \
  --tenant prod-tenant --client-id <id> --client-secret <secret>

# Use a specific session in any fod command:
fcli fod release list --fod-session prod
```

---

## Step 3 – Verify

```shell
fcli fod session list
```

Expected output: one row with status `ACTIVE` and the correct tenant/URL.

---

## Step 4 – Log Out

Always log out when finished, especially in automation, to clean up tokens server-side:

```shell
fcli fod session logout
# Or for a named session:
fcli fod session logout --fod-session <name>
```

---

## Environment Variables Reference

| Variable | Description |
|----------|-------------|
| `FCLI_DEFAULT_FOD_URL` | FoD base URL (e.g., `https://ams.fortify.com`) |
| `FCLI_DEFAULT_FOD_TENANT` | FoD tenant name |
| `FCLI_DEFAULT_FOD_USER` | Username / email for PAT login |
| `FCLI_DEFAULT_FOD_PASSWORD` | PAT or password |
| `FCLI_DEFAULT_FOD_CLIENT_ID` | OAuth Client ID |
| `FCLI_DEFAULT_FOD_CLIENT_SECRET` | OAuth Client Secret |

---

## Troubleshooting

| Problem | Solution |
|---------|---------|
| `401 Unauthorized` | Verify tenant name, credentials, and that the token/PAT has not expired |
| `Connection refused` / timeout | Check `FCLI_DEFAULT_FOD_URL`; configure proxy if behind a corporate firewall: `fcli config proxy add <host:port>` |
| Multiple active sessions | Run `fcli fod session list` and log out stale sessions |
| `SSL certificate` errors | Add your CA bundle: `fcli config truststore set --truststore <path>.jks` |
