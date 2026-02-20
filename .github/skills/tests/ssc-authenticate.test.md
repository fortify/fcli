# Test: ssc-authenticate

## Purpose
Verify the ssc-authenticate skill correctly guides a user through SSC login, handles token-based and password auth, and follows best practices.

## Test Cases

### Test 1: Token-Based Login
**Prompt:** "I need to log in to SSC with a CIToken"

**Expected commands:**
```shell
fcli ssc session login --url https://ssc.example.com/ssc --token <token-value>
fcli ssc session list
```

---

### Test 2: Username/Password Login
**Prompt:** "Log in to SSC at https://ssc.mycompany.com/ssc with my username and password"

**Expected commands:**
```shell
fcli ssc session login --url https://ssc.mycompany.com/ssc --user <username> --password <password>
```

---

### Test 3: With ScanCentral URL
**Prompt:** "Log in to SSC and also configure ScanCentral SAST"

**Expected commands:**
```shell
fcli ssc session login \
  --url https://ssc.example.com/ssc \
  --user <user> --password <pwd> \
  --sc-sast-url https://scancentral.example.com/scancentral-ctrl
```

---

### Test 4: Create a Token via fcli
**Prompt:** "How do I create a new SSC token using fcli?"

**Expected commands:**
```shell
fcli ssc access-control create-token \
  --token-type CIToken \
  --expire-in 7d \
  -o 'expr=Token: {token}\n'
```

---

### Test 5: Pipeline Environment Variables
**Prompt:** "How do I set up SSC credentials for a Jenkins pipeline?"

**Expected output includes:**
- `FCLI_DEFAULT_SSC_URL` and `FCLI_DEFAULT_SSC_TOKEN` environment variables
- `fcli ssc session login` without inline credentials
- `fcli ssc session logout` in cleanup

---

### Test 6: Logout
**Prompt:** "Log out of SSC"

**Expected:**
```shell
fcli ssc session logout
```

---

## Validation Checklist

- [ ] Commands use `fcli ssc session login` (not `fcli ssc login`)
- [ ] Token-based auth recommended for pipelines
- [ ] Logout step always included for pipeline examples
- [ ] SC-SAST URL mentioned when ScanCentral scans are planned
- [ ] Token limit exhaustion warning present for pipeline patterns
