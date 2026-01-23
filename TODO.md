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

### Phase 2: CI-Specific Guides (Target: Q1 2026)
- [ ] **Add CI-guide generation mode** to `ci-doc.yaml`
  - Generate comprehensive CI-specific documentation files
  - Target outputs: `fod-ci-github.adoc`, `ssc-ci-gitlab.adoc`, etc.
  - Include bootstrap, session, and CI integration sections
  - Add CI-specific introductions and setup instructions
  
- [ ] **Implement CI-specific template substitution**
  - Replace generic placeholders with CI-specific references
  - Examples:
    - `{{ciSpecificSastExport}}` → `github-sast-report` or `gitlab-sast-report`
    - `{{ciSpecificDebrickedExport}}` → `gitlab-debricked-report`
    - `{{ciSpecificPrComment}}` → `github-pr-comment`
  - Apply to action descriptions in environment variable tables

### Phase 3: Build Integration (Target: Q1 2026)
- [ ] **Add second Gradle task** for CI-guide generation
  - `buildTimeAction_ci_doc_guides` task
  - Generate full guides in addition to fragments
  - Separate from fragment generation for modularity

- [ ] **Integrate with fcli-doc module**
  - Configure Asciidoctor Gradle plugin for HTML conversion
  - Generate Jekyll-compatible HTML for GitHub Pages
  - Ensure proper styling and navigation

### Phase 4: CI System Support (Target: Q1 2026)
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
**Next**: Phase 2 (CI-specific guides and template substitution) 🚧  
**Last Updated**: January 23, 2026
