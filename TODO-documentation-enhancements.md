# TODO: Fcli Documentation Enhancements for CI/CD Integration

This TODO tracks comprehensive documentation improvements needed in the `fcli-doc` project to support CI/CD platform integrations.

## High-Level Goals

1. **Improve discoverability** of CI/CD integration guidance for platform developers
2. **Document complete workflow** from fcli bootstrap to tool installation to environment setup
3. **Clarify design decisions** that affect platform integrations (--require-latest, --self, semantic versioning)
4. **Provide practical examples** for shell scripts, TypeScript modules, and platform-specific wrappers

---

## 1. CI/CD Integration Documentation

### 1.1 Developer Integration Guide (COMPLETED)
✅ Created `doc-resources/developer-integration-guide.md` covering:
- Fcli bootstrap strategies
- `--self` and `--self-type` parameter usage
- Semantic version support (v3, v3.6, v3.6.1)
- Integration patterns (shell, TypeScript, platform wrappers)
- Tool cache integration
- Air-gapped environment support

**Remaining:**
- [ ] Port to fcli-doc project with proper navigation structure
- [ ] Add to main documentation ToC under "CI/CD Integration"
- [ ] Include mermaid diagrams for bootstrap flow
- [ ] Add troubleshooting section with common issues

### 1.2 Platform-Specific Examples
Create dedicated guides for each major CI/CD platform:

**GitHub Actions:**
- [ ] Document @fortify/setup module architecture
- [ ] Explain tool-cache integration (@actions/tool-cache)
- [ ] Show action.yml definition with all inputs
- [ ] Include examples: basic setup, air-gapped, multi-version matrix
- [ ] Document GITHUB_ENV integration for environment variables

**Azure DevOps:**
- [ ] Document task.json structure for fcli bootstrap task
- [ ] Explain AGENT_TOOLSDIRECTORY integration
- [ ] Show VSTS task variable output (##vso[task.setvariable])
- [ ] Include examples: pipeline YAML with Fortify tools setup

**GitLab CI:**
- [ ] Document component template structure (template.yml)
- [ ] Explain dotenv artifact integration for environment variables
- [ ] Show job definition with Fortify tools setup
- [ ] Include examples: .gitlab-ci.yml with fcli tool setup component

**Shell Scripts:**
- [ ] Document standalone fcli tool setup.sh structure
- [ ] Show platform detection logic (Linux/Mac/Windows)
- [ ] Explain PATH vs explicit fcli bootstrapping
- [ ] Include examples: Bash and PowerShell variants

### 1.3 Action Reference Documentation
Comprehensive documentation for built-in actions:

**fcli tool setup:**
- [ ] Complete parameter reference with examples for each option
- [ ] Document all version resolution patterns (latest, v3, v3.6.1, auto, preinstalled, skip)
- [ ] Explain --self and --self-type in detail (link to developer guide)
- [ ] Document --copy-from variants for each tool with version matching behavior
- [ ] Explain --on-copy-version-mismatch options (warn, error, ignore)
- [ ] Document --use-tool-cache and platform detection
- [ ] Provide decision tree for choosing version patterns
- [ ] Include complete examples: basic, air-gapped, multi-tool, tool-cache

**fcli tool env:**
- [ ] Complete parameter reference for all output formats
- [ ] Document PATH, cmd-var, home-var modes with examples
- [ ] Explain auto/include/exclude behavior for each tool
- [ ] Show integration with CI/CD platforms (GITHUB_ENV, VSTS, dotenv)
- [ ] Include examples for each output format

### 1.4 Tool Command Documentation
Document tool-related commands for manual usage:

**fcli tool <name> install:**
- [ ] Parameter reference with examples
- [ ] Document version resolution and --copy-from behavior
- [ ] Explain tool cache detection and integration
- [ ] Show air-gapped installation workflow

**fcli tool <name> register:**
- [ ] Complete parameter reference
- [ ] Document --auto-detect vs --path modes
- [ ] Explain --require-latest behavior and exit codes
- [ ] Show version filtering with --version parameter
- [ ] Include examples for semantic versions

**fcli tool <name> env:**
- [ ] Parameter reference for all output formats
- [ ] Show shell integration examples (eval, export)
- [ ] Document PATH manipulation modes

---

## 2. Semantic Versioning Documentation

### 2.1 Fcli Semantic Version Strategy
- [ ] Document that GitHub releases include v3, v3.6, v3.6.1 tags
- [ ] Explain how all three tags point to same release assets
- [ ] Show examples of downloading from /v3/ or /v3.6/ URLs
- [ ] Document benefits: cache stability, predictable updates, no surprises

### 2.2 Tool Semantic Versioning
For tools with yearly versioning (FoD CLI, SC Client):
- [ ] Document v24, v24.4 pattern support
- [ ] Explain relationship to fcli tool definitions
- [ ] Show how --require-latest ensures latest matching version

### 2.3 Version Resolution Flow
- [ ] Create flowchart showing version resolution decision tree
- [ ] Document precedence: explicit version → semantic pattern → latest
- [ ] Explain when --require-latest applies vs doesn't apply

---

## 3. Design Decision Documentation

### 3.1 Why Fcli Cannot Install Itself
- [ ] Explain circular dependency problem
- [ ] Document requirement for platform integrations to bootstrap fcli
- [ ] Show how --self parameter solves this

### 3.2 Why @fortify/setup Doesn't Use Tool Definitions
- [ ] Explain bootstrap chicken-and-egg problem
- [ ] Document that tool definitions require fcli to be available
- [ ] Show how bootstrap uses simple version resolution instead

### 3.3 --require-latest Flag Design
**Current behavior (opt-in):**
- Semantic versions (v3, v24) optionally add --require-latest
- Default: accept any pre-installed version matching pattern

**Proposed discussion:**
- [ ] Document rationale for current opt-in design
- [ ] Consider documenting alternative: default to require-latest with --allow-any-matching opt-out
- [ ] Explain trade-offs: user surprise vs strict validation
- [ ] Include user feedback if design changes in future

### 3.4 Tool Cache Integration
- [ ] Document why tool cache uses stable classification (--self-type stable)
- [ ] Explain relationship to CI/CD platform caching
- [ ] Show benefits: speed, bandwidth, offline resilience

---

## 4. Air-Gapped Environment Documentation

### 4.1 Complete Air-Gapped Workflow
- [ ] Document pre-staging requirements
- [ ] Show how to download all artifacts for offline use
- [ ] Explain --copy-from parameters for each tool
- [ ] Document version detection from copy sources
- [ ] Show complete example: staging → transfer → install

### 4.2 Version Mismatch Handling
- [ ] Document --on-copy-version-mismatch behavior in detail
- [ ] Show examples of warn vs error vs ignore
- [ ] Explain when each option is appropriate

---

## 5. Troubleshooting Documentation

### 5.1 Common Issues
- [ ] "fcli not found" → forgot --self or fcli not bootstrapped
- [ ] "Cannot install tool fcli" → circular dependency, use --self
- [ ] Version mismatch warnings → explain --self-type unstable behavior
- [ ] GitHub download failures from /v3/ → verify semantic version tags
- [ ] Tool cache not detected → verify platform environment variables

### 5.2 Exit Code Reference
Document exit codes for tool register command:
- [ ] 0: SUCCESS - Tool registered successfully
- [ ] 1: TOOL_NOT_FOUND - Tool not found in auto-detect mode
- [ ] 2: INVALID_PATH - Provided path invalid or doesn't exist
- [ ] 3: NOT_EXECUTABLE - Binary not executable
- [ ] 4: VERSION_MISMATCH - Version doesn't match --version filter
- [ ] 5: VERSION_NOT_LATEST - With --require-latest, version is not latest matching

### 5.3 Debug Mode
- [ ] Document how to enable debug output for troubleshooting
- [ ] Show how to inspect tool cache contents
- [ ] Explain version detection logic and fallbacks

---

## 6. Example Collection

### 6.1 Complete Working Examples
Provide downloadable, runnable examples:
- [ ] **GitHub Action:** Complete repository with fcli tool setup command
- [ ] **Azure DevOps:** Complete pipeline with Fortify tools task
- [ ] **GitLab CI:** Complete .gitlab-ci.yml with fcli tool setup component
- [ ] **Shell Script:** Standalone Bash script for Linux/Mac
- [ ] **PowerShell Script:** Standalone script for Windows
- [ ] **TypeScript Module:** Example using @fortify/setup programmatically

### 6.2 Use Case Examples
- [ ] **Basic setup:** Install fcli + FoD CLI with latest versions
- [ ] **Semantic versions:** Use v3 for fcli, v24 for SC Client
- [ ] **Exact versions:** Pin to specific versions for reproducibility
- [ ] **Air-gapped:** Complete offline workflow
- [ ] **Multi-tool matrix:** Test multiple tool versions in matrix build
- [ ] **Tool cache:** Leverage CI/CD platform caching

---

## 7. Migration Guides

### 7.1 From Manual Installation
- [ ] Document moving from manual tool installation to fcli tool setup command
- [ ] Show before/after pipeline YAML comparison
- [ ] Highlight benefits: consistency, versioning, caching

### 7.2 From Legacy Actions
If older community actions exist:
- [ ] Document migration path to official fcli tool setup
- [ ] Show parameter mapping
- [ ] Highlight new features and improvements

---

## 8. API Documentation

### 8.1 Action Schema Documentation
- [ ] Document fcli tool setup command YAML schema structure
- [ ] Explain SpEL expression usage in actions
- [ ] Show how to extend actions for custom tools

### 8.2 @fortify/setup API
- [ ] Generate API documentation from TypeScript source
- [ ] Document all exported functions and types
- [ ] Show programmatic usage examples

---

## 9. Video/Visual Documentation

### 9.1 Diagrams
Create Mermaid diagrams:
- [ ] Bootstrap flow (fcli resolution → download → cache)
- [ ] Tool installation flow (version resolution → download/copy → register)
- [ ] Environment setup flow (tool detection → variable generation → platform output)
- [ ] Decision tree for version patterns
- [ ] Decision tree for --self-type selection

### 9.2 Screenshots
If applicable (for IDE/UI integrations):
- [ ] GitHub Actions UI showing fcli tool setup inputs
- [ ] Azure DevOps task configuration
- [ ] GitLab CI component usage

---

## 10. Documentation Quality

### 10.1 Consistency
- [ ] Ensure consistent terminology across all documentation
- [ ] Use same examples where possible for different platforms
- [ ] Maintain consistent parameter naming (--self, --self-type, etc.)

### 10.2 Completeness
- [ ] Every parameter documented with example
- [ ] Every exit code explained
- [ ] Every error message has troubleshooting guidance

### 10.3 Accessibility
- [ ] Clear navigation from main docs to CI/CD integration
- [ ] Search-friendly headings and keywords
- [ ] Cross-references between related topics

---

## Priority Levels

**P0 (Critical - Complete First):**
- 1.1 Developer Integration Guide (port to fcli-doc)
- 1.3 fcli tool setup command reference
- 2.1 Fcli semantic version strategy
- 5.2 Exit code reference

**P1 (High - Complete Second):**
- 1.2 Platform-specific examples (at least GitHub, Azure, GitLab)
- 1.3 fcli tool env command reference
- 3.1-3.4 Design decision documentation
- 5.1 Common issues troubleshooting

**P2 (Medium - Complete Third):**
- 1.4 Tool command documentation
- 4.1-4.2 Air-gapped environment documentation
- 6.1-6.2 Example collection
- 9.1 Diagrams

**P3 (Nice to Have):**
- 7.1-7.2 Migration guides
- 8.1-8.2 API documentation
- 9.2 Screenshots
- 10.1-10.3 Documentation quality improvements

---

## Ownership

- **fcli-doc maintainers:** Port and integrate documentation
- **@fortify/setup maintainers:** API documentation and TypeScript examples
- **Platform integration authors:** Platform-specific examples and guides

---

## Success Criteria

Documentation is complete when:
1. ✅ Platform developer can bootstrap fcli without reading code
3. ✅ All fcli tool setup parameters documented with examples
3. ✅ Semantic versioning strategy clearly explained
4. ✅ Every exit code and error message has troubleshooting guidance
5. ✅ At least 3 complete working examples available (GitHub, Azure, GitLab)
6. ✅ All design decisions documented with rationale

---

## Related Issues/PRs

- [ ] Link to fcli-doc GitHub issues tracking each section
- [ ] Link to @fortify/setup documentation PRs
- [ ] Link to example repository creation

---

**Next Steps:**
1. Create tracking issues in fcli-doc project for each P0/P1 section
2. Port developer-integration-guide.md to fcli-doc with navigation
3. Begin comprehensive action reference documentation for fcli tool setup
4. Document semantic versioning strategy in fcli-doc
