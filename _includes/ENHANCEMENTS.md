# FCLI Documentation Enhancements

This directory contains Jekyll includes that provide enhanced functionality for FCLI documentation pages.

## Architecture

### Master Include: `fcli-enhancements.html`

The master include coordinates all enhancement features. It:
- Loads individual feature includes based on configuration
- Provides a single point of control for all enhancements
- Can be enabled/disabled per page or site-wide

**Usage in pages:**
```html
{% include asciidoc-head.html %}
{% include fcli-enhancements.html %}
```

## Available Features

### ✅ Smart Version Navigation (`/js/version-nav.js`)

**Status:** Implemented

Automatically navigates to the same page in different versions when clicking version dropdown. Falls back to index.html if page doesn't exist in target version.

**Features:**
- Event delegation on version dropdown links
- HEAD request to check page existence (500ms timeout)
- Graceful fallback to index.html
- Console logging for debugging
- Progressive enhancement (works without JavaScript)

### ✅ Version Warning Banner (`fcli-version-warning.html`)

**Status:** Implemented (Server-Side Jekyll)

Shows a warning banner when viewing older or development version documentation, with appropriate links to stable/latest versions.

**Features:**
- **Server-side rendering** - No JavaScript required, instant display, no flash
- **Smart version detection** - Distinguishes between dev, old major, and old minor versions
- **Contextual messaging:**
  - **Development versions** (`dev_*`) - Blue banner, links to stable (v3)
  - **Old major versions** (v2.x) - Yellow banner, links to both latest v2 and latest v3
  - **Old minor versions** (v3.13.0 vs v3.14.3) - Yellow banner, links to latest v3
- **Dismissible banner** - Click × to hide
- **Automatic layout adjustment** - Adjusts page padding and TOC position
- **Major version symlinks** - Uses `v2`, `v3` symlinks, not specific versions
- **Future-proof** - Works with any major version (v4, v5, etc.)

**Implementation:**
- Uses Jekyll Liquid templates to compare `page.fcli_version` with `site.data.versions.release[0]`
- Renders HTML/CSS directly in page source (no JavaScript dependency)
- Extracts major version numbers to determine appropriate links

**Configuration:**
```yaml
# In _config.yml (site-wide)
fcli_enable_version_warning: true

# In page front matter (per-page)
fcli_enable_version_warning: false
```

**Examples:**
- Viewing `v2.12.3` → Shows "Older Major Version" with links to v2 and v3
- Viewing `v3.13.0` (when latest is v3.14.3) → Shows "Older Version" with link to v3
- Viewing `dev_v3.x` → Shows "Development Version" with link to v3

## Configuration

### Site-Wide Configuration

Add to `_config.yml`:

```yaml
# FCLI Enhancement Features
fcli_enable_version_warning: true
```

### Per-Page Configuration

Add to page front matter:

```yaml
---
title: My Page
fcli_version: 3.14.3
fcli_enable_version_warning: false  # Override site setting
---
```

## Bulk Update Scripts

### `update-to-enhancements.sh`

Updates existing versioned Jekyll pages to use the new enhancement system.

**What it does:**
- Removes old individual script includes
- Adds `{% include fcli-enhancements.html %}`
- Safe to run multiple times (idempotent)
- Creates backups before modification

**Usage:**
```bash
cd /home/rsenden/workspace/fcli-gh-pages
./update-to-enhancements.sh
```

### `add-version-nav-script.sh` (Deprecated)

Old script for adding version-nav.js directly. Use `update-to-enhancements.sh` instead.

## Development Workflow

### Adding a New Feature

1. **Create include file** in `_includes/`:
   ```bash
   touch _includes/fcli-my-feature.html
   ```

2. **Implement feature** in the include file

3. **Add to master include** (`fcli-enhancements.html`):
   ```liquid
   {% assign enable_my_feature = page.fcli_enable_my_feature | default: site.fcli_enable_my_feature | default: false %}
   
   {% if enable_my_feature %}
     {% include fcli-my-feature.html %}
   {% endif %}
   ```

4. **Test with new builds** - automatically included in all new pages

5. **Update existing pages** - create/update bulk script if needed

### Testing Changes

1. **Rebuild documentation:**
   ```bash
   cd /home/rsenden/workspace/fcli
   ./gradlew :fcli-other:fcli-doc:asciiDoctorVersionedJekyll
   ```

2. **Copy to gh-pages:**
   ```bash
   cp -r fcli-other/fcli-doc/build/generated-docs/gh-pages/versioned/* ../fcli-gh-pages/v3.x.x/
   ```

3. **Test locally with Jekyll:**
   ```bash
   cd /home/rsenden/workspace/fcli-gh-pages
   jekyll serve
   # Open http://localhost:4000/fcli/
   ```

## File Structure

```
fcli-gh-pages/
├── _includes/
│   ├── fcli-enhancements.html      # Master coordinator
│   ├── fcli-version-warning.html   # Version warning banner
│   └── ENHANCEMENTS.md             # This file
├── js/
│   └── version-nav.js              # Smart version navigation
├── update-to-enhancements.sh       # Bulk update script
└── ...

fcli/
└── fcli-other/fcli-doc/
    └── src/docs/asciidoc/templates/html5/
        └── document.html.erb       # Template that includes fcli-enhancements.html
```

## Benefits of This Architecture

1. **Single Point of Control**: Update one include file instead of regenerating all version docs
2. **Progressive Enhancement**: New features automatically available in new builds
3. **Backward Compatible**: Old pages work without updates
4. **Configurable**: Enable/disable features per page or site-wide
5. **Modular**: Each feature is independent and can be developed separately
6. **Maintainable**: Clear separation between features
7. **Future-Proof**: Easy to add new features without touching existing code

## Troubleshooting

### Feature not appearing on existing pages

Run the bulk update script:
```bash
./update-to-enhancements.sh
```

### Feature not appearing on new builds

Check that the template includes fcli-enhancements.html:
```bash
grep -n "fcli-enhancements" fcli/fcli-other/fcli-doc/src/docs/asciidoc/templates/html5/document.html.erb
```

### Console errors in browser

1. Open browser DevTools (F12)
2. Check Console tab for errors
3. Look for `[FCLI]` or `[Version Nav]` prefixed messages

### Version warning not showing

1. Ensure you're viewing an older version (not latest or dev)
2. Check that `fcli_enable_version_warning` is not set to `false`
3. Check browser console for errors
