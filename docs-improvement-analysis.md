# FCLI Documentation Build Process - Analysis & Improvement Suggestions

## Executive Summary

This document provides a comprehensive analysis of the current fcli documentation build process and proposes improvements to enhance user experience. The analysis covers both the Jekyll-based GitHub Pages deployment and the static HTML documentation distributed via release assets.

**Current State:**
- 425 separate man page HTML files generated from Picocli
- Single large index.html (834 lines AsciiDoc, 2288 lines HTML) with embedded manual pages
- No search functionality
- No breadcrumb navigation
- Separate processing for versioned vs static documentation
- Manual navigation between command pages

**Key Improvements Proposed:**
1. Combined single-document command reference with hierarchical TOC
2. Client-side search functionality (works for both Jekyll and static HTML)
3. Breadcrumb navigation for better UX
4. Improved page structure and discoverability

---

## Background: Current Implementation

### Documentation Types

The fcli build process generates two documentation sets:

1. **Jekyll-based HTML** (`docs-gh-pages-*.zip`)
   - Deployed to GitHub Pages (`gh-pages` branch)
   - Version-specific directories (e.g., `v3.0.0/`, `dev_v3.x/`)
   - Includes version selector widget
   - Static content (index, dev-info, migration guide)
   - Versioned content (command reference, actions, CI docs)

2. **Static HTML** (`docs-html.zip`)
   - Distributed as release asset
   - Current version only
   - Same content as Jekyll but without version selector
   - Standalone browseable without web server

### Build Process Overview

**Gradle Tasks** (`fcli-other/fcli-doc/build.gradle.kts`):

1. **Man Page Generation** (`generateAsciiDocManPage`)
   - Uses Picocli's `ManPageGenerator` 
   - Generates **425 separate `.adoc` files** in `build/generated-docs/asciidoc/manpage/`
   - Each command gets its own file (e.g., `fcli-action-run.adoc`)
   - Cross-references between pages using `xref:` links

2. **Additional Doc Generation**
   - `generateAsciiDocGenericActions` - Generic action documentation
   - `generateAsciiDocSSCActions` - SSC-specific actions
   - `generateAsciiDocFoDActions` - FoD-specific actions
   - `generateAsciiDocActionDevelopment` - Action development guide
   - `extractCiDocs` - CI integration documentation from fcli-app

3. **AsciiDoc → HTML Conversion** (via AsciidoctorTask)
   - `asciiDoctorVersionedHtml` - Static HTML output
   - `asciiDoctorVersionedJekyll` - Jekyll-compatible HTML
   - `asciiDoctorStaticJekyll` - Static Jekyll pages
   - Custom ERB template (`templates/html5/document.html.erb`) for branding

4. **Distribution Packaging**
   - `distDocsVersionedHtml` → `docs-html.zip` (release asset)
   - `distDocsVersionedJekyll` → `docs-gh-pages-versioned.zip` (gh-pages)
   - `distDocsStaticJekyll` → `docs-gh-pages-static.zip` (gh-pages)

**GitHub Workflow** (`.github/workflows/ci.yml`):

The `publishPages` job:
1. Checks out `gh-pages` branch
2. Extracts `docs-gh-pages-static.zip` to root (overwrites)
3. Extracts `docs-gh-pages-versioned.zip` to version-specific directory
4. Updates symlinks (`latest`, `v3`, `v3.0`, `latest_dev`)
5. Updates `_data/versions/{release,dev}.yml` for version selector
6. Commits and pushes to `gh-pages`

### Current File Structure

**Source:**
```
fcli-other/fcli-doc/src/docs/asciidoc/
├── static/                    # Version-independent content
│   ├── index.adoc            # Landing page for gh-pages
│   ├── dev-info.adoc         # Developer documentation
│   └── migration-v2.x-v3.x.adoc
├── versioned/                # Version-specific content
│   └── index.adoc            # Main user documentation (834 lines!)
└── templates/
    └── html5/
        └── document.html.erb # Custom HTML template with branding
```

**Generated (per version):**
```
build/generated-docs/html/    # Static HTML output
├── index.html                # Large single-page doc (2288 lines)
├── action-development.html
├── {fod,ssc,generic}-actions.html
└── manpage/
    ├── fcli.html            # Root command
    ├── fcli-action.html     # Module pages
    ├── fcli-action-run.html # Individual command pages
    └── ... (425 total files)
```

### User Experience Issues

1. **Navigation Challenges:**
   - Users must navigate through 425+ separate HTML files
   - No breadcrumbs to show location in command hierarchy
   - Back button required to return to parent command
   - No quick way to jump between related commands

2. **Discoverability Problems:**
   - No search functionality to find commands quickly
   - Must know command structure to navigate effectively
   - Difficult to discover related functionality
   - Can't search across all commands simultaneously

3. **Large Index Page:**
   - Main `index.adoc` is 834 lines (2288 lines as HTML)
   - Includes ALL manual page content embedded inline
   - Difficult to maintain and update
   - Slow page load for users with limited bandwidth

---

## Improvement Suggestions

### 1. Combined Command Reference with Hierarchical TOC

**Problem:** 425 separate man page files make navigation cumbersome and don't provide a good overview of all available commands.

**Solution:** Generate a single, combined command reference document with a comprehensive Table of Contents.

**Benefits:**
- Single-page browsing with browser search (Ctrl+F)
- Complete overview of all commands in one place
- Easier to print or save for offline use
- Better for users who want to understand full capability
- Reduced number of HTTP requests
- Better for SEO (single authoritative page)

**Implementation Options:**

#### Option A: Gradle Task Combination (Recommended)
Create a new Gradle task that:
1. Reads all generated `.adoc` man page files
2. Combines them into a single AsciiDoc document
3. Generates hierarchical TOC based on command structure
4. Maintains cross-references as internal anchors

```kotlin
val generateCombinedManPages = tasks.register<JavaExec>("generateCombinedManPages") {
    group = "documentation"
    description = "Combine all man pages into single document with hierarchical TOC"
    dependsOn(generateAsciiDocManPage)
    
    inputs.dir(asciiDocManPageOutDir)
    outputs.file(asciiDocOutDir.map { it.file("command-reference.adoc") })
    
    classpath(configurations.runtimeClasspath, configurations.annotationProcessor)
    mainClass.set("com.fortify.cli.app.doc.ManPageCombiner")
    args(
        asciiDocManPageOutDir.get().asFile.absolutePath,
        asciiDocOutDir.get().asFile.resolve("command-reference.adoc").absolutePath
    )
}
```

**Java Implementation (ManPageCombiner):**
```java
public class ManPageCombiner {
    public static void main(String[] args) {
        Path manPageDir = Paths.get(args[0]);
        Path outputFile = Paths.get(args[1]);
        
        // 1. Parse all .adoc files
        // 2. Build command tree structure
        // 3. Generate combined document with:
        //    - Document header
        //    - Hierarchical TOC (auto-generated by AsciiDoc)
        //    - All commands organized by hierarchy
        //    - Convert xref: links to internal anchors (#)
    }
}
```

#### Option B: Custom Picocli ManPageGenerator
Extend Picocli's ManPageGenerator to output a single combined document instead of multiple files.

**Pros:** More control, cleaner integration
**Cons:** More complex, harder to maintain with Picocli updates

**Recommendation:** Start with Option A (Gradle task) as it's simpler and doesn't require forking/extending Picocli internals.

**Structure of Combined Document:**

```asciidoc
= FCLI Command Reference
:toc: left
:toclevels: 4
:sectanchors: true

== Introduction
Brief overview of fcli command structure.

== Commands

=== fcli
Root command description and options.

=== fcli action
Action module description.

==== fcli action run
Run action command - full details.

==== fcli action list
List actions command - full details.

=== fcli ssc
SSC module description.

==== fcli ssc session
SSC session management.

===== fcli ssc session login
Login to SSC - full details.

... (continue for all 425 commands)
```

**Backward Compatibility:**
- Keep generating individual man pages for those who prefer them
- Include both in `docs-html.zip` and gh-pages
- Add prominent link to combined reference on index page

---

### 2. Client-Side Search Functionality

**Problem:** No way to search across commands and documentation. Users must manually browse or use browser's find function (which only works on current page).

**Solution:** Implement client-side JavaScript search that works for both static HTML and Jekyll deployments.

**Benefits:**
- Fast, instant search results
- Works offline (static HTML)
- No server-side infrastructure needed
- Improves discoverability significantly
- Better user experience

**Implementation:**

#### Recommended Library: Lunr.js
- Pure JavaScript, no server required
- Works offline
- Small footprint (~50KB minified)
- Excellent for technical documentation
- Used by many documentation sites (Gatsby, Hugo, Jekyll)

**Implementation Steps:**

1. **Generate Search Index** (Gradle task)
```kotlin
val generateSearchIndex = tasks.register<JavaExec>("generateSearchIndex") {
    group = "documentation"
    description = "Generate search index for documentation"
    dependsOn(generateAsciiDocAll)
    
    outputs.file(docsOutDir.map { it.file("search-index.json") })
    
    classpath(sourceSets.main.get().runtimeClasspath)
    mainClass.set("com.fortify.cli.app.doc.SearchIndexGenerator")
    args(
        asciiDocOutDir.get().asFile.absolutePath,
        docsOutDir.get().asFile.resolve("search-index.json").absolutePath
    )
}
```

2. **SearchIndexGenerator.java:**
```java
public class SearchIndexGenerator {
    // Parse all .adoc files
    // Extract: title, command name, description, synopsis, options
    // Generate JSON in Lunr.js format:
    // [
    //   {
    //     "id": "fcli-action-run",
    //     "title": "fcli action run",
    //     "description": "Run an action",
    //     "body": "full text content...",
    //     "url": "manpage/fcli-action-run.html"
    //   },
    //   ...
    // ]
}
```

3. **Add Search UI to Template** (`document.html.erb`):
```html
<div id="search-container">
  <input type="text" id="search-input" placeholder="Search commands...">
  <div id="search-results"></div>
</div>

<script src="https://cdn.jsdelivr.net/npm/lunr@2.3.9/lunr.min.js"></script>
<script src="search.js"></script>
```

4. **search.js:**
```javascript
// Load search index
fetch('search-index.json')
  .then(response => response.json())
  .then(documents => {
    // Build Lunr index
    const idx = lunr(function() {
      this.ref('id');
      this.field('title', { boost: 10 });
      this.field('description', { boost: 5 });
      this.field('body');
      documents.forEach(doc => this.add(doc));
    });
    
    // Handle search input
    document.getElementById('search-input').addEventListener('input', (e) => {
      const results = idx.search(e.target.value);
      displayResults(results, documents);
    });
  });
```

**Search Features:**
- Real-time as-you-type search
- Fuzzy matching (typo-tolerant)
- Weighted results (titles > descriptions > body)
- Keyboard navigation (arrow keys, Enter)
- Highlight search terms in results

**Styling:**
- Dropdown below search box
- Show top 10 results
- Display: command name, description, path
- Click to navigate to command page

**Jekyll Integration:**
- Works identically for Jekyll and static HTML
- Add search box to top navbar
- Ensure search-index.json is included in Jekyll builds

---

### 3. Breadcrumb Navigation

**Problem:** Users navigating through command hierarchy have no visual indication of their location or easy way to navigate back up the hierarchy.

**Solution:** Add breadcrumb navigation to show command hierarchy and enable quick navigation.

**Benefits:**
- Clear indication of current location in hierarchy
- One-click navigation to parent commands
- Improves user orientation
- Standard UX pattern, familiar to users

**Implementation:**

#### Add to HTML Template (`document.html.erb`):
```html
<% if attr?(:breadcrumbs) %>
<nav aria-label="breadcrumb" id="breadcrumbs">
  <ol>
    <%
    breadcrumbs = attr(:breadcrumbs).split(' > ')
    breadcrumbs.each_with_index do |crumb, index|
      parts = crumb.split('|')
      text = parts[0]
      link = parts[1] if parts.size > 1
      
      if index == breadcrumbs.size - 1 %>
        <li class="active"><%= text %></li>
    <% else %>
        <li><a href="<%= link %>"><%= text %></a></li>
    <% end
    end %>
  </ol>
</nav>
<% end %>
```

#### Generate Breadcrumb Metadata:

Modify `ManPageGenerator` output or post-process `.adoc` files to add:

```asciidoc
:breadcrumbs: fcli|fcli.html > action|fcli-action.html > run
```

For `fcli-action-run.adoc`:
- Home: fcli
- Module: action  
- Command: run (current, no link)

**Gradle Task to Inject Breadcrumbs:**
```kotlin
val addBreadcrumbs = tasks.register("addBreadcrumbs") {
    group = "documentation"
    dependsOn(generateAsciiDocManPage)
    
    doLast {
        val manPageDir = asciiDocManPageOutDir.get().asFile
        manPageDir.listFiles()?.forEach { file ->
            if (file.extension == "adoc") {
                val breadcrumb = generateBreadcrumb(file.nameWithoutExtension)
                file.writeText(":breadcrumbs: $breadcrumb\n" + file.readText())
            }
        }
    }
}

fun generateBreadcrumb(fileName: String): String {
    // Parse "fcli-action-run" -> ["fcli", "action", "run"]
    val parts = fileName.split('-')
    
    // Build breadcrumb: "fcli|fcli.html > action|fcli-action.html > run"
    return parts.mapIndexed { index, part ->
        if (index == parts.size - 1) {
            part // Current page, no link
        } else {
            val link = parts.subList(0, index + 1).joinToString("-") + ".html"
            "$part|$link"
        }
    }.joinToString(" > ")
}
```

**Styling:**
```css
#breadcrumbs {
  padding: 10px 0;
  margin-bottom: 20px;
  border-bottom: 1px solid #ddd;
}

#breadcrumbs ol {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-wrap: wrap;
}

#breadcrumbs li {
  display: inline;
}

#breadcrumbs li:not(:last-child)::after {
  content: " › ";
  padding: 0 8px;
  color: #999;
}

#breadcrumbs li.active {
  font-weight: bold;
}
```

---

### 4. Restructure Documentation for Better Navigation

**Problem:** Current structure has a massive `index.adoc` file and doesn't provide clear entry points for different user needs.

**Solution:** Restructure documentation into focused, discoverable sections.

**Proposed Structure:**

```
Documentation Home
├── Getting Started
│   ├── Installation
│   ├── Quick Start Guide
│   └── Basic Concepts
├── Command Reference (NEW: combined or separate)
│   ├── All Commands (single page with TOC)
│   └── By Module (if keeping separate pages)
│       ├── fcli action
│       ├── fcli ssc
│       ├── fcli fod
│       └── ...
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
    ├── Contributing
    ├── Architecture
    └── Build Instructions
```

**Implementation:**

1. **Split Large index.adoc:**
   - Extract installation → `getting-started/installation.adoc`
   - Extract CI integration → `guides/ci-integration.adoc`
   - Extract session management → `guides/sessions.adoc`
   - Keep only overview and links in main index

2. **Create Clear Landing Page:**
```asciidoc
= FCLI Documentation

Welcome to the Fortify CLI documentation.

== Quick Links

* link:getting-started/installation.html[Install FCLI]
* link:command-reference/index.html[Command Reference] (all commands)
* link:guides/ci-integration.html[CI/CD Integration Guide]
* link:actions/index.html[Actions Documentation]

== What is FCLI?

[Brief description]

== Popular Tasks

* link:getting-started/quick-start.html[Get started in 5 minutes]
* link:guides/sessions.html[Manage SSC/FoD sessions]
* link:command-reference/fcli-ssc.html[Browse SSC commands]
* link:guides/output-formats.html[Work with JSON output]
```

3. **Add "Jump To" Navigation:**
- Sticky sidebar with major sections
- Quick access to frequently used commands
- Search integration

---

## Additional Improvements

### 5. Enhanced Man Page Styling

**Current:** Basic man page styling with minimal formatting.

**Improvements:**
- Syntax highlighting for command examples
- Color-coded option types (required/optional)
- Collapsible sections for long option lists
- Copy-to-clipboard buttons for code examples
- Visual indicators for deprecated/preview commands

### 6. Command Examples Library

**Add:** Dedicated examples section for each command showing:
- Common use cases
- Complex scenarios
- Integration with other commands
- Real-world workflows

### 7. Interactive Command Builder (Future)

**Concept:** Web-based UI where users can:
- Select command from dropdown
- Fill in required options via form
- See generated command line
- Copy to clipboard
- Useful for beginners learning fcli

### 8. Version Comparison

**For gh-pages:** Add ability to compare command reference between versions
- Highlight new commands
- Show changed options
- Indicate deprecated functionality

### 9. Mobile-Friendly Improvements

- Responsive TOC (hamburger menu on mobile)
- Touch-friendly navigation
- Readable code blocks on small screens
- Optimized search for mobile

---

## Implementation Priorities

### Phase 1: Foundation (High Priority)
1. ✅ Combined command reference document (Option A: Gradle task)
2. ✅ Client-side search with Lunr.js
3. ✅ Basic breadcrumb navigation

**Timeline:** 2-3 weeks
**Impact:** High - significantly improves core UX

### Phase 2: Structure (Medium Priority)
1. Restructure documentation into focused sections
2. Split large index.adoc
3. Create clear landing page
4. Enhanced man page styling

**Timeline:** 1-2 weeks
**Impact:** Medium - improves discoverability

### Phase 3: Polish (Low Priority)
1. Command examples library
2. Enhanced styling and formatting
3. Mobile optimizations
4. Version comparison (gh-pages only)

**Timeline:** 1-2 weeks
**Impact:** Low-Medium - nice-to-have enhancements

### Phase 4: Advanced (Future)
1. Interactive command builder
2. API documentation integration
3. Video tutorials
4. Community contributions section

**Timeline:** TBD
**Impact:** Variable - depends on user feedback

---

## Technical Considerations

### Backward Compatibility
- Must maintain existing man page generation for compatibility
- Both individual and combined formats should be available
- Existing links should not break
- Version selector must continue working

### Build Time Impact
- Combined document generation: +10-20 seconds
- Search index generation: +5-10 seconds
- Total additional build time: ~30 seconds (acceptable)

### File Size Impact
- Combined command reference: ~500KB HTML (vs 425 × ~40KB = 17MB total)
- Search index: ~200KB JSON
- Search library (Lunr.js): ~50KB
- Net savings: Significant for users browsing multiple commands

### Browser Compatibility
- Target: Modern browsers (Chrome, Firefox, Safari, Edge)
- Fallback: Basic navigation works without JavaScript
- Search requires JavaScript (progressive enhancement)

### Accessibility
- ARIA labels for navigation elements
- Keyboard navigation support
- Screen reader compatibility
- High contrast mode support
- Semantic HTML structure

---

## Testing Strategy

### Functional Testing
1. Build documentation locally
2. Verify all links work (combined + individual)
3. Test search functionality
4. Validate breadcrumb navigation
5. Check mobile responsiveness

### Content Testing
1. Verify all 425 commands present in combined doc
2. Check TOC completeness and accuracy
3. Validate cross-references
4. Ensure examples render correctly

### Performance Testing
1. Measure page load times
2. Test search response time
3. Validate on slow connections
4. Check memory usage with large documents

### User Acceptance Testing
1. Internal team review
2. Beta testing with community
3. Gather feedback on usability
4. Iterate based on results

---

## Success Metrics

### Quantitative
- Time to find specific command: Target <10 seconds (vs. current ~30-60s)
- Page load time: Target <2 seconds for main pages
- Search result relevance: Target >90% accuracy for top 3 results
- Documentation coverage: 100% of commands documented

### Qualitative
- User feedback surveys (post-implementation)
- Reduction in documentation-related support questions
- Community engagement (PRs, issues about docs)
- Positive sentiment in user comments

---

## Conclusion

The proposed improvements focus on three core areas:

1. **Navigation** - Combined reference + breadcrumbs for easier browsing
2. **Search** - Client-side search for quick discovery
3. **Structure** - Better organization for improved UX

These changes will significantly improve the fcli documentation user experience while maintaining backward compatibility and requiring minimal additional build time.

**Recommended Next Steps:**
1. Review and approve this proposal
2. Create implementation issues for Phase 1
3. Assign resources and timeline
4. Begin implementation with combined command reference
5. Iterate based on feedback

---

## Appendix A: Alternative Approaches Considered

### Static Site Generators
**Considered:** Moving to Sphinx, MkDocs, Docusaurus
**Decision:** Rejected - too much rework, current AsciiDoc pipeline works well

### Server-Side Search
**Considered:** Algolia, self-hosted search backend
**Decision:** Rejected - unnecessary complexity, client-side sufficient

### Separate Documentation Site
**Considered:** docs.fortify.com subdomain with full-featured doc platform
**Decision:** Future consideration - current gh-pages + release assets work well

---

## Appendix B: References

- [Picocli ManPageGenerator Documentation](https://picocli.info/man/picocli-codegen-manpage.html)
- [Lunr.js Documentation](https://lunrjs.com/)
- [AsciiDoc User Guide](https://docs.asciidoctor.org/asciidoc/latest/)
- [GitHub Pages Documentation](https://docs.github.com/en/pages)
- [Jekyll Documentation](https://jekyllrb.com/docs/)

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-02-04 | Analysis Agent | Initial analysis and recommendations |

