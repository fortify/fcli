# Documentation Improvement Analysis - README

This directory contains analysis and recommendations for improving fcli's documentation user experience.

## Files in This Analysis

### 1. `docs-improvement-analysis.md` (Technical Deep Dive)
**Purpose:** Comprehensive technical analysis for implementation teams

**Contents:**
- Current documentation build process breakdown
- Detailed explanation of each proposed improvement
- Implementation approaches with code examples
- Technical considerations (compatibility, performance, etc.)
- Testing strategy and success metrics
- Complete reference for developers

**Target Audience:** Developers, architects, technical leads

---

### 2. `ISSUE-documentation-improvements.md` (GitHub Issue Template)
**Purpose:** Concise summary for team discussion and tracking

**Contents:**
- Problem statement and proposed solutions
- Implementation phases with timelines
- Technical impact summary
- Discussion questions
- Ready to copy/paste into GitHub issue

**Target Audience:** Product managers, team leads, stakeholders

**Usage:** 
```bash
# Copy content to create a new GitHub issue at:
# https://github.com/fortify/fcli/issues/new
```

---

## Quick Summary

### Current State
- **425 separate man page HTML files** (one per command)
- No search functionality
- No breadcrumb navigation
- Large monolithic index file (834 lines)

### Proposed Improvements

| Priority | Improvement | Benefit | Effort |
|----------|------------|---------|--------|
| HIGH | Combined command reference | Single-page overview of all commands | 1-2 weeks |
| HIGH | Client-side search (Lunr.js) | Fast, offline-capable command search | 1-2 weeks |
| HIGH | Breadcrumb navigation | Clear location in command hierarchy | 1 week |
| MEDIUM | Documentation restructuring | Better organization and entry points | 1-2 weeks |
| LOW | Enhanced styling & examples | Improved readability and usability | 1-2 weeks |

**Total Phase 1 Effort:** 2-3 weeks  
**Total Phase 1-3 Effort:** 5-9 weeks

---

## Implementation Workflow

### Option 1: GitHub Issue → Implementation (Recommended)

1. **Create GitHub Issue**
   ```bash
   # Copy content from ISSUE-documentation-improvements.md
   # Create issue at: https://github.com/fortify/fcli/issues/new
   # Labels: enhancement, documentation, user-experience
   ```

2. **Team Discussion**
   - Review and refine proposals
   - Prioritize improvements
   - Assign resources
   - Set timeline

3. **Create Sub-Issues**
   - Break Phase 1 into separate issues
   - Assign to developers
   - Link to parent issue

4. **Implementation**
   - Reference `docs-improvement-analysis.md` for technical details
   - Create PRs for each improvement
   - Review and merge

5. **Testing & Rollout**
   - Beta test with community
   - Gather feedback
   - Iterate

### Option 2: Direct Implementation

1. **Review Analysis**
   - Read `docs-improvement-analysis.md` thoroughly
   - Understand current architecture
   - Review code examples

2. **Start with Phase 1**
   - Implement combined command reference
   - Add client-side search
   - Add breadcrumb navigation

3. **Create PRs**
   - One PR per improvement
   - Include tests
   - Update documentation

4. **Iterate**
   - Move to Phase 2 based on feedback
   - Continue improvements

---

## Key Technical Details

### Build System
- **Tool:** Gradle 8.14.3
- **Module:** `fcli-other/fcli-doc`
- **Main File:** `build.gradle.kts`

### Documentation Pipeline
1. Picocli ManPageGenerator → 425 `.adoc` files
2. Custom Gradle tasks → additional `.adoc` files
3. Asciidoctor → HTML conversion
4. Jekyll/Static HTML → deployment

### Deployment Targets
- **GitHub Pages:** Jekyll-based, version-specific directories
- **Release Assets:** Static HTML in `docs-html.zip`

### Technologies Used
- Picocli (command-line parsing + man page generation)
- AsciiDoc/Asciidoctor (document format)
- Gradle (build orchestration)
- Jekyll (gh-pages rendering)
- Java 17 (build-time code generation)

---

## Contact & Questions

### For Technical Questions
- Review `docs-improvement-analysis.md` Appendix B for reference links
- Check existing GitHub issues for similar discussions
- Consult Picocli and Asciidoctor documentation

### For Process Questions
- Use GitHub issue for team discussion
- Tag relevant stakeholders
- Schedule sync meeting if needed

---

## Success Criteria

### User Experience
✅ Users can find any command in <10 seconds  
✅ Clear overview of all available commands  
✅ Easy navigation within command hierarchy  
✅ Fast, responsive search  
✅ Works offline (static HTML)

### Technical
✅ Build time increase <60 seconds  
✅ 100% backward compatibility  
✅ All existing links continue working  
✅ Mobile-friendly  
✅ Accessible (WCAG 2.1 AA)

### Process
✅ Clear implementation plan  
✅ Testable improvements  
✅ Measurable metrics  
✅ Community feedback incorporated

---

## Version History

| Date | Version | Changes |
|------|---------|---------|
| 2026-02-04 | 1.0 | Initial analysis and recommendations |

---

## License

This analysis is part of the fcli project and follows the same license terms.

