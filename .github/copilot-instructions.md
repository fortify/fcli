# GitHub Copilot Instructions

## Project Overview

Fcli is a modular CLI tool for interacting with Fortify products (FoD, SSC, ScanCentral SAST/DAST).

**Key characteristics:**
- Built with Java 17, Gradle, and Picocli for command structure
- Multi-module Gradle project: `fcli-core/*` (product modules), `fcli-other/*` (supporting modules)
- Module references defined in `gradle.properties` via `*Ref` properties
- Unified output framework (JSON/CSV/XML/YAML/table), session management, and YAML-based action automation

## Development Workflow

- **Build:** `cd` to project root, run `./gradlew build` (don't run from subdirectories)
  - Creates shadow jar at `fcli-core/fcli-app/build/libs/fcli.jar` and copies to `build/libs/fcli.jar`
  - Module-specific tasks (`shadowJar`, `dist`, `distAll`) do NOT create root `build/libs/fcli.jar`
  - Always run `./gradlew build` before manual testing if local script depends on `build/libs/fcli.jar`
- **Validation:** Use `get_errors` tool first, then full Gradle build to catch warnings
- **Testing:**
  - Unit tests: `src/test`; command structure validated in `FortifyCLITest`
  - Functional tests: `fcli-core/fcli-functional-test` module; run with `./gradlew :fcli-core:fcli-functional-test:test`

## Code Conventions

- Target Java 17 features: records, text blocks, `var`, pattern matching for `instanceof`
- Prefer explicit imports; avoid wildcards
- **Always use imports, not fully qualified class names** (unless collision with same class name from multiple packages)
- Short methods (~20 lines max); extract helpers or use Streams for clarity
- No change-tracking comments (e.g., "New ...", "Updated ..."); only explanatory comments when code is complex

## Detailed Development Guides

**When working on specific areas, read the relevant detailed guide:**

- **Java code (commands, utilities, exceptions, etc.):** Read [copilot/java-guide.md](.github/copilot/java-guide.md)
- **Action YAML files:** Read [copilot/action-yaml-guide.md](.github/copilot/action-yaml-guide.md)
- **Utility classes:** Reference [copilot/utilities-guide.md](.github/copilot/utilities-guide.md)
- **Style conventions:** Follow [copilot/style-guide.md](.github/copilot/style-guide.md)

