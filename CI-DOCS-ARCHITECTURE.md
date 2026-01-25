# CI Documentation Architecture

## Overview
CI documentation is generated from a single source (`ci-doc.yaml`) and produces two types of outputs:
1. **Fragments** - Packaged in `fcli.jar` for runtime inclusion by ci.yaml actions
2. **Full Guides** - Packaged in `ci-docs.zip` for GitHub Pages publishing

## File Locations

### Source
- **ci-doc.yaml**: `fcli-core/fcli-app/src/main/resources/com/fortify/cli/app/actions/build-time/ci-doc.yaml`
  - Single source of truth for all CI documentation
  - Contains environment variable definitions for FoD, SSC, and common config
  - Defines @fortify/setup versions and CI system versions
  - Generates both fragments and full guides

### Generated Outputs

#### 1. Fragments (Packaged in fcli.jar)
Location: `fcli-core/fcli-app/build/generated-action-output-resources/`

**Runtime inclusions** (used by ci.yaml actions via `${#include('/...')}`):
- `bootstrap-v{version}.txt` / `.adoc` - @fortify/setup and fcli bootstrap env vars
- `session-{product}.txt` / `.adoc` - FoD/SSC authentication env vars
- `ci-core-{product}.txt` / `.adoc` - CI integration env vars by workflow phase

**Purpose**: Dynamically included in ci.yaml action help text and AsciiDoc documentation

#### 2. Full Guides (Packaged in ci-docs.zip)
Location: `build/dist/release-assets/ci-docs.zip`

**CI-specific complete documentation**:
- `{product}-{ciSystem}-v{version}.adoc` (e.g., `fod-github-v3.0.x.adoc`)
- Complete integration guides with version table, all env var sections
- One file per product × CI system × version combination

**Purpose**: Published to GitHub Pages at `/docs/ci-integration/{ciSystem}/v{version}/`

## Build Process

### 1. fcli-app Module
**Task: `buildTimeAction_ci_doc`**
- Runs ci-doc.yaml as a build-time fcli action
- Generates all fragments and full guides to `build/generated-action-output-resources/`
- Classpath: Runtime dependencies + annotation processor
- Output: Both fragments and full guides in same directory

**Task: `packageCiDocs`**
- Depends on: `buildTimeAction_ci_doc`
- Packages only full guides (*-github-v*.adoc, *-gitlab-v*.adoc, etc.)
- Excludes fragments (bootstrap-*, session-*, ci-core-*)
- Output: `build/dist/release-assets/ci-docs.zip`

**Task: `shadowJar`**
- Depends on: `buildTimeAction_ci_doc` (among others)
- Includes fragments from `build/generated-action-output-resources/` in jar resources
- Excludes: Nothing (fragments automatically included via source set output)
- Output: `fcli.jar` with embedded fragments

### 2. fcli-doc Module (TODO)
**Task: `extractCiDocs`** (to be implemented)
- Depends on: `:fcli-core:fcli-app:packageCiDocs`
- Extracts ci-docs.zip contents to `build/ci-docs-extracted/`

**Task: `asciiDoctorCiDocsJekyll`** (to be implemented)
- Depends on: `extractCiDocs`
- Converts extracted .adoc files to Jekyll-compatible HTML
- Output: `build/generated-docs/gh-pages/static/ci-integration/`
- Applies standard fcli documentation styling and navigation

## Version Management

### Adding a New @fortify/setup Version
1. Add definition in `fortifySetupDefinitions` (e.g., `fortify-setup-1.1.x`)
2. Define version-specific environment variables
3. Add key to hardcoded array in STEP 1 (~line 36)
4. Run: `./gradlew :fcli-core:fcli-app:buildTimeAction_ci_doc`
5. Verify: `bootstrap-v1.1.x.txt` and `bootstrap-v1.1.x.adoc` generated

### Adding a New CI System Version
1. Add definition in `ciSystemDefinitions` (e.g., `github-3.1.x`)
2. Set `bootstrap-version` to reference existing @fortify/setup version
3. Define version-specific notes
4. Add key to hardcoded array in STEP 4 (~line 231)
5. Run: `./gradlew :fcli-core:fcli-app:packageCiDocs`
6. Verify: `fod-github-v3.1.x.adoc` and `ssc-github-v3.1.x.adoc` in ci-docs.zip

## CI System Parameterization (TODO)

**Problem**: Generic descriptions reference multiple CI systems:
```
"using an fcli-provided action matching the current CI system like 
actionRef:github-sast-report or actionRef:gitlab-sast-report"
```

**Solution**: Add CI-specific action reference maps to ciSystemDefinitions:
```yaml
ciSystemDefinitions:
  github-3.0.x:
    bootstrap-version: "1.0.x"
    actionRefs:
      sastExport: github-sast-report
      debrickedExport: github-debricked-report  # if supported
      prComment: github-pr-comment
  gitlab-2.0.x:
    bootstrap-version: "1.0.x"
    actionRefs:
      sastExport: gitlab-sast-report
      debrickedExport: gitlab-debricked-report
      prComment: gitlab-mr-comment
```

Update descriptions to use placeholders:
```yaml
desc: >-
  ... using the fcli-provided actionRef:{{ciSastExport}} action ...
```

Replace during generation:
```yaml
|${envVar.desc.replaceAll('\\{\\{ciSastExport\\}\\}', ciSystemDef.actionRefs.sastExport)}
```

**Benefits**:
- Cleaner, CI-specific documentation
- No confusing multi-CI references
- Easier maintenance when adding new CI systems

## Testing

### Test Fragment Generation
```bash
./gradlew :fcli-core:fcli-app:buildTimeAction_ci_doc
ls -l fcli-core/fcli-app/build/generated-action-output-resources/
```

### Test ci-docs.zip Packaging
```bash
./gradlew :fcli-core:fcli-app:packageCiDocs
unzip -l build/dist/release-assets/ci-docs.zip
```

### Test Fragments in fcli.jar
```bash
./gradlew :fcli-core:fcli-app:shadowJar
./fcli fod action help ci
# Should display environment variables dynamically included from fragments
```

### Test Complete Build
```bash
./gradlew :fcli-core:fcli-app:dist
ls -lh build/dist/release-assets/
# Should contain: fcli.jar and ci-docs.zip
```

## Dependencies

**fcli-app** → **fcli-action** (for action framework)
**fcli-app** → **fcli-common** (for action runner)
**fcli-doc** → **fcli-app** (for ci-docs.zip artifact)

## Future Enhancements

1. **GitLab Support**: Add gitlab-* versions to ciSystemDefinitions
2. **Azure DevOps Support**: Add azure-* versions
3. **Jenkins/Generic**: Add jenkins-* or generic-* versions for self-hosted CI
4. **Automated Testing**: Validate generated docs in CI pipeline
5. **Versioned URLs**: Implement semantic version-based URL routing on GitHub Pages
