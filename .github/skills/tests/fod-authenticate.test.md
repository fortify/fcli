# Test: fod-authenticate

## Purpose
Verify the fod-authenticate skill correctly guides a user through FoD login, handles various credential types, and follows best practices.

## Test Cases

### Test 1: Basic PAT Login
**Prompt:** "I need to log in to Fortify on Demand"

**Expected agent behavior:**
- Asks for FoD URL, tenant, and credential type (PAT recommended for interactive use)
- Produces a login command using `fcli fod session login --url ... --tenant ... --user ... --password ...`
- Suggests verifying with `fcli fod session list`

**Expected commands produced:**
```shell
fcli fod session login --url https://ams.fortify.com --tenant <tenant> --user <email> --password <PAT>
fcli fod session list
```

---

### Test 2: Pipeline-style (Environment Variables)
**Prompt:** "How do I set up FoD authentication for my GitHub Actions pipeline?"

**Expected agent behavior:**
- Recommends client credentials (Client ID + Secret) for pipelines
- Shows environment variable pattern with `FCLI_DEFAULT_FOD_*` variables
- Demonstrates login without inline credentials
- Reminds to logout in cleanup step

**Expected output includes:**
```shell
export FCLI_DEFAULT_FOD_URL=https://ams.fortify.com
export FCLI_DEFAULT_FOD_CLIENT_ID=${{ secrets.FOD_CLIENT_ID }}
export FCLI_DEFAULT_FOD_CLIENT_SECRET=${{ secrets.FOD_CLIENT_SECRET }}
fcli fod session login
# ... work ...
fcli fod session logout
```

---

### Test 3: Logout
**Prompt:** "Log out of FoD"

**Expected command:**
```shell
fcli fod session logout
```

---

### Test 4: Multiple Sessions
**Prompt:** "I need to work with two FoD tenants at the same time"

**Expected agent behavior:**
- Explains named sessions
- Shows `--fod-session <name>` on login and subsequent commands

---

### Test 5: Troubleshooting 401 Error
**Prompt:** "I'm getting 401 Unauthorized when trying to log in to FoD"

**Expected agent behavior:**
- Suggests checking URL, tenant name, and credentials
- Mentions token expiry
- Suggests proxy configuration if relevant

---

## Negative Tests

### Test N1: Missing Credentials
**Prompt:** "Log in to FoD" (with no URL/credentials provided)

**Expected:** Agent asks for URL, tenant, and credential type before producing commands.

---

## Validation Checklist

- [ ] Commands use `fcli fod session login` (not `fcli fod login`)
- [ ] No credentials hard-coded in pipeline examples
- [ ] Logout is recommended
- [ ] Session list verification step included
- [ ] Named sessions explained when asked about multiple tenants
