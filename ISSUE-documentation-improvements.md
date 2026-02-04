# Documentation User Experience Improvements

## Overview

This issue proposes comprehensive improvements to fcli's documentation build process and user experience, based on detailed analysis of the current implementation. The goal is to make fcli documentation more discoverable, navigable, and user-friendly while maintaining backward compatibility.

**See [`docs-improvement-analysis.md`](./docs-improvement-analysis.md) for complete technical analysis.**

## Current State

The fcli documentation system currently:
- Generates **425 separate man page HTML files** (one per command)
- Uses Picocli's ManPageGenerator for command documentation
- Deploys to GitHub Pages (Jekyll) and release assets (static HTML)
- Requires manual navigation between related commands
- Lacks search functionality
- Has no breadcrumb navigation

## Problems

1. **Poor Discoverability** - Users can't search across commands; must browse manually through 425 files
2. **Navigation Challenges** - No breadcrumbs, difficult to understand location in command hierarchy
3. **Large Monolithic Index** - Main index.adoc is 834 lines, combining too much content
4. **Difficult to Get Overview** - No single-page reference to see all available commands

## Proposed Solutions

### 1. Combined Command Reference with Hierarchical TOC

**Problem:** Users struggle to get an overview of all available commands and must navigate between 425 separate pages.

**Solution:** Generate a single, combined command reference document alongside existing individual pages.

**Benefits:**
- ✅ Complete command overview in one place
- ✅ Single-page browsing with browser search (Ctrl+F)
- ✅ Better for printing/saving offline
- ✅ Reduced HTTP requests
- ✅ Maintains backward compatibility (individual pages still available)

**Implementation:**
- New Gradle task `generateCombinedManPages`
- Java class `ManPageCombiner` to process all `.adoc` files
- Generates `command-reference.html` with hierarchical structure
- Converts cross-references to internal anchors

**Code Location:** `fcli-other/fcli-doc/build.gradle.kts` + new Java class

**Estimated Effort:** 1-2 weeks

---

### 2. Client-Side Search Functionality

**Problem:** No way to quickly find specific commands or topics without manual browsing.

**Solution:** Implement JavaScript-based search using Lunr.js library.

**Benefits:**
- ✅ Fast, real-time search across all documentation
- ✅ Works offline (static HTML)
- ✅ No server infrastructure required
- ✅ Works identically for Jekyll and static deployments
- ✅ Fuzzy matching (typo-tolerant)

**Implementation:**
- New Gradle task `generateSearchIndex` 
- Java class `SearchIndexGenerator` to create JSON index
- Add search UI to HTML template (`document.html.erb`)
- Include Lunr.js library (50KB, CDN)
- Add `search.js` for UI logic

**Search Features:**
- As-you-type search
- Weighted results (titles > descriptions > content)
- Top 10 results in dropdown
- Keyboard navigation
- Click to navigate

**Code Locations:**
- `fcli-other/fcli-doc/build.gradle.kts` (task)
- New Java class for index generation
- `fcli-other/fcli-doc/src/docs/asciidoc/templates/html5/document.html.erb` (UI)
- New `search.js` file

**Estimated Effort:** 1-2 weeks

---

### 3. Breadcrumb Navigation

**Problem:** Users navigating command hierarchy have no visual indication of their location or easy way to go back up the tree.

**Solution:** Add breadcrumb navigation showing full command path with clickable links.

**Benefits:**
- ✅ Clear indication of current location
- ✅ One-click navigation to parent commands
- ✅ Standard UX pattern
- ✅ Improves user orientation

**Implementation:**
- Modify Gradle task to inject breadcrumb metadata into `.adoc` files
- Update HTML template to render breadcrumbs
- Add CSS styling

**Example Breadcrumb:**
```
fcli > action > run
[link] [link] [current]
```

**Code Locations:**
- `fcli-other/fcli-doc/build.gradle.kts` (breadcrumb generation)
- `fcli-other/fcli-doc/src/docs/asciidoc/templates/html5/document.html.erb` (rendering)

**Estimated Effort:** 1 week

---

### 4. Documentation Restructuring

**Problem:** Main `index.adoc` is too large (834 lines) and combines multiple concerns. Documentation structure doesn't provide clear entry points for different user needs.

**Solution:** Restructure documentation into focused, discoverable sections.

**Proposed Structure:**
```
Documentation Home
├── Getting Started
│   ├── Installation
│   ├── Quick Start Guide
│   └── Basic Concepts
├── Command Reference
│   ├── All Commands (single page)
│   └── By Module (existing separate pages)
├── Guides
│   ├── CI/CD Integration
│   ├── Session Management
│   ├── Output Formats
│   └── Environment Variables
├── Actions
│   ├── Action Development
│   ├── Generic Actions
│   ├── SSC Actions
│   └── FoD Actions
└── Developer Documentation
```

**Implementation:**
- Split large `index.adoc` into focused documents
- Create new landing page with clear navigation
- Reorganize existing content
- Update build tasks to process new structure

**Code Locations:**
- `fcli-other/fcli-doc/src/docs/asciidoc/versioned/` (restructured content)
- `fcli-other/fcli-doc/build.gradle.kts` (updated paths)

**Estimated Effort:** 1-2 weeks

---

## Additional Enhancements (Lower Priority)

### 5. Enhanced Man Page Styling
- Syntax highlighting for examples
- Color-coded option types
- Copy-to-clipboard buttons
- Collapsible sections

### 6. Command Examples Library
- Common use cases per command
- Complex scenarios
- Real-world workflows

### 7. Mobile Optimizations
- Responsive TOC
- Touch-friendly navigation
- Readable code blocks on small screens

### 8. Version Comparison (gh-pages only)
- Highlight new commands between versions
- Show changed options
- Indicate deprecated functionality

## Implementation Phases

### Phase 1: Foundation (High Priority)
**Timeline:** 2-3 weeks
**Impact:** High

- [ ] Combined command reference document
- [ ] Client-side search with Lunr.js
- [ ] Basic breadcrumb navigation

### Phase 2: Structure (Medium Priority)
**Timeline:** 1-2 weeks
**Impact:** Medium

- [ ] Restructure documentation
- [ ] Split large index.adoc
- [ ] Create clear landing page
- [ ] Enhanced man page styling

### Phase 3: Polish (Lower Priority)
**Timeline:** 1-2 weeks
**Impact:** Low-Medium

- [ ] Command examples library
- [ ] Mobile optimizations
- [ ] Version comparison

## Technical Considerations

### Backward Compatibility
- ✅ Maintain existing man page generation
- ✅ Both individual and combined formats available
- ✅ Existing links continue working
- ✅ Version selector unchanged

### Build Time Impact
- Combined document generation: +10-20 seconds
- Search index generation: +5-10 seconds
- **Total additional build time: ~30 seconds** (acceptable)

### File Size Impact
- Combined command reference: ~500KB HTML
- Search index: ~200KB JSON
- Lunr.js library: ~50KB
- **Net benefit:** Significantly smaller for users browsing multiple commands (vs 17MB total for 425 separate files)

### Browser Support
- Target: Modern browsers (Chrome, Firefox, Safari, Edge - last 2 versions)
- Fallback: Basic navigation works without JavaScript
- Search requires JavaScript (progressive enhancement)

## Success Metrics

### Quantitative
- Time to find specific command: **Target <10 seconds** (currently ~30-60s)
- Page load time: **Target <2 seconds**
- Search result relevance: **Target >90% accuracy** for top 3 results

### Qualitative
- User feedback surveys
- Reduction in documentation-related support questions
- Positive community engagement

## References

- 📄 **Full Analysis:** [`docs-improvement-analysis.md`](./docs-improvement-analysis.md)
- 🔨 **Current Build:** `fcli-other/fcli-doc/build.gradle.kts`
- 📝 **Man Pages:** Generated from Picocli annotations
- 🎨 **Template:** `fcli-other/fcli-doc/src/docs/asciidoc/templates/html5/document.html.erb`
- 🔍 **Search Library:** [Lunr.js](https://lunrjs.com/)

## Next Steps

1. ✅ Review and approve this proposal
2. ⬜ Create sub-issues for Phase 1 tasks
3. ⬜ Assign resources and timeline
4. ⬜ Begin implementation with combined command reference
5. ⬜ Iterate based on feedback

## Questions for Discussion

1. **Priority:** Should we implement all Phase 1 items together, or tackle them separately?
2. **Scope:** Are there other documentation pain points not covered here?
3. **Timeline:** What's the target release for these improvements?
4. **Resources:** Who will own implementation and maintenance?
5. **Testing:** Should we do beta testing with community before full rollout?

---

**Labels:** `enhancement`, `documentation`, `user-experience`
**Milestone:** TBD (suggest: next minor release)
**Assignee:** TBD

