# CI Documentation Updates - TODO

## Completed ✅

### Phase 1: Foundation & Fragments (January 23, 2026)
- [x] Renamed `ci-envvars.yaml` to `ci-doc.yaml` for broader scope
- [x] Updated Gradle build task from `buildTimeAction_ci_envvars` to `buildTimeAction_ci_doc`
- [x] Generated fine-grained documentation fragments instead of monolithic variants:
  - `bootstrap.txt/adoc` (3.8KB/1.8KB) - Bootstrap environment variables
  - `session-fod.txt/adoc` (1.9KB/749B) - FoD session configuration
  - `session-ssc.txt/adoc` (2.1KB/810B) - SSC session configuration
  - `ci-core-fod.txt/adoc` (14KB/7.1KB) - FoD CI integration (4 sections)
  - `ci-core-ssc.txt/adoc` (21KB/11KB) - SSC CI integration (4 sections)
- [x] Added bootstrap section with correct fortify-setup-js environment variables:
  - `FCLI_PATH` - Path to pre-installed fcli 3.14.0+
  - `FCLI_URL` - Custom fcli download URL
  - `FCLI_RSA_SHA256_URL` - Custom signature file URL
  - `FCLI_VERIFY_SIGNATURE` - Enable/disable signature verification
- [x] Implemented multi-section AsciiDoc output for ci-core fragments:
  - FoD: Release Configuration, Packaging, Scan, Post-Scan
  - SSC: Application Version Configuration, Packaging, Scan, Post-Scan
  - Proper heading hierarchy (level 4 under level 3 CI Integration)
- [x] Updated all consumer ci.yaml files (FoD, SSC, generic) to use new fragments

## In Progress 🚧

### Phase 2: CI-Specific Versioned Guides (Target: Q1 2026)
- [x] **Add versioning to bootstrap and CI system definitions**
  - Added multi-version support: `bootstrap-1.0`, `bootstrap-1.1`, etc.
  - Added CI system versioning: `github-3.0`, `github-3.1`, etc.
  - Each CI version references specific bootstrap version via `bootstrap-version` field
  - Version numbers use `.x` suffix (e.g., `3.0.x`) to indicate patch version range
  - Clean output filenames: `fod-github-v3.0.x.adoc` (not `fod-github-3.0-v3.0.adoc`)
  - Tracks: GitHub Action version, @fortify/setup version, bootstrap version
  - Enables generation of multiple versioned documentation files simultaneously
  
- [x] **Add fcli-specific environment variables**
  - Added `fcliEnvVars` section for fcli-specific vars (e.g., TOOL_DEFINITIONS)
  - Included in all bootstrap documentation (applies to all @fortify/setup versions)
  - Separated from @fortify/setup-specific vars for clarity
  
- [ ] **Test versioned documentation generation**
  - Verify `bootstrap-v1.0.x.adoc` and `bootstrap-v1.1.x.adoc` generate correctly
  - Test `fod-github-v3.0.x.adoc` uses bootstrap-1.0 env vars
  - Test `fod-github-v3.1.x.adoc` uses bootstrap-1.1 env vars (with FCLI_CACHE_DIR)
  - Validate version information tables show correct component versions
  
- [ ] **Generate CI-specific versioned documentation**
  - ✅ **IMPLEMENTED**: Outputs `fod-github-v3.0.x.adoc`, `ssc-github-v3.1.x.adoc`, etc.
  - ✅ **IMPLEMENTED**: Version information table at document start
  - ✅ **IMPLEMENTED**: Version compatibility notes from definition metadata
  - ✅ **IMPLEMENTED**: Bootstrap section with @fortify/setup + fcli env vars
  - ⚠️ **PENDING**: Test generation with actual build
  
- [ ] **Support multiple CI system versions simultaneously**
  - ✅ **IMPLEMENTED**: Can define `github-3.0`, `github-3.1`, `gitlab-2.0` simultaneously
  - ✅ **IMPLEMENTED**: Each CI version references its bootstrap version
  - ✅ **IMPLEMENTED**: Bootstrap env vars pulled from correct bootstrap definition
  - ⚠️ **PENDING**: Add GitLab, Azure DevOps examples when needed
  
- [ ] **Implement CI-specific template substitution**
  - Replace generic placeholders with CI-specific references in generated documentation
  - Examples of generic text to parameterize:
    - `matching the current CI system like actionRef:github-sast-report or actionRef:gitlab-sast-report`
  - Should become CI-specific:
    - GitHub docs: `using the fcli-provided actionRef:github-sast-report action`
    - GitLab docs: `using the fcli-provided actionRef:gitlab-sast-report action`
  - Implementation approach:
    - Add `ciSpecificActionRefs` map to ciSystemDefinitions (e.g., `{sastExport: 'github-sast-report', debrickedExport: 'gitlab-debricked-report', prComment: 'github-pr-comment'}`)
    - Update env var descriptions to use placeholders like `{{ciSastExport}}` instead of hardcoded action lists
    - Replace placeholders during document generation using ciSystem's action reference map
  - Affected env vars: DO_SAST_EXPORT, DO_DEBRICKED_EXPORT, DO_PR_COMMENT (check for others)
  - Benefits: Cleaner docs, easier maintenance, no confusing multi-CI references

### Phase 3: Multi-Version Support & Risk Mitigation (Target: Q1 2026)
- [x] **Add support for multiple CI system versions**
  - ✅ Multiple version entries per CI system (`github-3.0`, `github-3.1`)
  - ✅ Generate separate documentation for each version
  - ✅ Track which bootstrap version each CI version uses
  - ✅ Bootstrap fragments generated per version (`bootstrap-v1.0.x.adoc`, `bootstrap-v1.1.x.adoc`)
  - ✅ Clean filenames with `.x` suffix to indicate patch version range

- [ ] **Version compatibility validation**
  - Add compatibility matrix generation formatter
  - Validate @fortify/setup version compatibility with fcli version
  - Document deprecation timelines for old versions
  - Add lifecycle policy (e.g., "Support last 3 minor versions")

- [ ] **Runtime version checking recommendations**
  - Document best practices for GitHub Action to log versions
  - Add guidance on detecting and warning about version mismatches
  - Provide documentation permalink strategy for detected versions
### Phase 4: Build Integration & Publishing (Target: Q1 2026)
- [x] **Move CI documentation generation to fcli-app module**
  - ✅ Moved ci-doc.yaml from fcli-action to fcli-app module
  - ✅ fcli-app now generates both fcli.jar (shadow jar) and ci-docs.zip
  - ✅ Created packageCiDocs task to package CI-specific documentation
  - ✅ Fragments (bootstrap-*.txt/adoc, session-*.txt/adoc, ci-core-*.txt/adoc) packaged in fcli.jar
  - ✅ Full guides (*-github-v*.adoc, *-gitlab-v*.adoc) packaged in ci-docs.zip for publishing
  - ✅ Single source of truth: ci-doc.yaml generates all documentation variants

- [ ] **Integrate with fcli-doc module**
  - Extract ci-docs.zip in fcli-doc build process
  - Configure Asciidoctor Gradle plugin for HTML conversion
  - Generate Jekyll-compatible HTML for GitHub Pages
  - Ensure proper styling and navigation
  - Implement versioned doc hosting (e.g., `/docs/ci-integration/github/v3.0/`)
  - Add index page linking to all available CI system versions

- [ ] **Test end-to-end documentation pipeline**
  - Verify fcli-app builds both fcli.jar and ci-docs.zip
  - Confirm fragments available in fcli.jar (test with `fcli action run ci`)
  - Verify fcli-doc can extract and process ci-docs.zip
  - Test GitHub Pages deployment with generated documentation
  - Validate all links work correctly in published docs

### Phase 5: CI System Support Expansion (Target: Q2 2026)
- [ ] **Enhance detect-env.yaml** with additional CI metadata
  - Detect CI capabilities (SAST export formats, PR/MR comment support)
  - Provide CI-specific action recommendations
  - Handle edge cases (Jenkins, TeamCity, custom CI)

- [ ] **Create CI-specific action variants** (optional)
  - Simplified actions that auto-select CI-specific exports
  - Example: `ci-github.yaml`, `ci-gitlab.yaml`
  - Reduce need for manual environment variable configuration

## Future Enhancements 🔮

### Documentation Improvements
- [ ] Add usage examples to each section (code snippets)
- [ ] Create quick-start guides for common scenarios
- [ ] Add troubleshooting section with common issues
- [ ] Generate PDF documentation alongside HTML

### Technical Improvements
- [ ] Support custom section titles via CLI options
- [ ] Add validation for environment variable references
- [ ] Generate OpenAPI/JSON Schema for programmatic access
- [ ] Create interactive documentation with collapsible sections

### CI Integration Examples
- [ ] Add complete workflow examples for each CI system:
  - GitHub Actions workflow files
  - GitLab CI pipeline configurations
  - Azure DevOps pipeline templates
  - Jenkins pipeline scripts

## Notes & Considerations

### Architecture Decisions
- **Fragment-first approach**: Generate fine-grained fragments that can be composed into larger documents
- **Two-phase build**: Generate AsciiDoc fragments, then convert to HTML via Gradle
- **Incremental enhancement**: Start simple (fragments) before adding complex features (full guides)
- **Version tracking**: Each CI system and bootstrap definition tracks major-minor version for generating versioned docs
- **Semantic versioning alignment**: fcli v3 → @fortify/setup v1 → GitHub Action v3; breaking changes cascade across all components

### Version Management Strategy
- **Bootstrap versioning**: `bootstrapDefinitions.bootstrap-X.Y` with `major-minor: "X.Y.x"` tracks @fortify/setup API version
- **CI system versioning**: Each CI system (e.g., `github-3.0`) tracks its action version + bootstrap version dependency
- **Generated docs**: Include version metadata table and compatibility notes
- **Multiple versions**: Support generating docs for multiple CI system versions (e.g., v3.0.x, v3.1.x) from same fcli build
- **Version file naming**: Pattern `{module}-{ciSystem}-v{major.minor.x}.adoc` (e.g., `fod-github-v3.0.x.adoc`)
- **Patch version range**: `.x` suffix indicates documentation applies to all patch releases (3.0.0, 3.0.1, 3.0.2, etc.)

### Risk Mitigation Strategies

#### 1. **Version Coupling & Documentation Drift**
- **Pin dependencies**: GitHub Action `package.json` locks exact @fortify/setup version
- **Version metadata**: All generated docs include fcli + @fortify/setup + CI Action versions
- **Versioned hosting**: Docs published to `/docs/github-action/v3.0/`, never overwrite released versions
- **Compatibility matrix**: Generate version compatibility table in documentation
- **Runtime validation**: CI actions log version information and detect mismatches

#### 2. **Breaking Changes in @fortify/setup**
- **Semantic versioning**: Major version bump for breaking changes (@fortify/setup 1.x → 2.x)
- **Deprecation policy**: Mark deprecated env vars for 2 minor versions before removal
- **Integration testing**: Test fcli with @fortify/setup N-1, N, N+1 versions
- **Minimum version checks**: @fortify/setup validates fcli >= 3.14.0 at runtime
- **Documentation updates**: Regenerate docs when @fortify/setup version changes in CI action

#### 3. **Platform-Specific Features**
- **Manual override mechanism**: Allow CI actions to provide custom doc fragments
- **Hybrid generation**: Base docs from fcli + platform-specific overrides
- **Clear marking**: Distinguish "fcli-Generated" vs "Platform-Specific" sections
- **Capability detection**: Use `detect-env.yaml` to identify platform-specific features

#### 4. **Documentation Staleness**
- **Version-specific permalinks**: Link to exact version, never "latest"
- **Archive old versions**: Keep but mark as "Archived - Upgrade to v3.x"
- **Version warnings**: Display notice when viewing docs for old versions
- **Update triggers**: Regenerate docs on fcli release OR @fortify/setup version bump in CI action

#### 5. **Multi-CI Support**
- **Separate definitions**: Each CI system gets its own entry in `ciSystemDefinitions`
- **Version independence**: GitLab v2.0 can use different @fortify/setup than GitHub v3.0
- **Platform capabilities**: Document what features work on which platforms
- **Example workflows**: Include platform-specific workflow snippets

### Challenges Addressed
- SpEL expression limitations in formatters (no nested curly braces in inline YAML maps)
- YAML parsing constraints (quoted SpEL expressions required in flow mappings)
- Array initialization syntax (`${{'a', 'b', 'c'}}` not `['a', 'b', 'c']`)
- Section heading hierarchy (level 4 for subsections under level 3 CI Integration)

### Testing Strategy
- Incremental builds after each change
- Verify fragment content with `cat` and `grep`
- Full integration test with `fcli ssc action asciidoc`
- Check final output on fortify.github.io after development release

## References

- **Original requirements**: See conversation context for detailed feature requests
- **fortify-setup-js**: `/home/rsenden/workspace/fortify-setup-js/src/config.ts` for bootstrap variables
- **fcli actions**: See `fcli-core/*/actions/zip/ci.yaml` for consumer patterns
- **Build system**: `fcli-core/fcli-action/build.gradle.kts` for task definitions

---
**Status**: Phase 1 complete (fragments with sections) ✅  
**Phase 2**: In progress (versioning infrastructure added) 🚧  
**Next**: Complete CI-specific doc generation and test with build  
**Last Updated**: January 25, 2026
