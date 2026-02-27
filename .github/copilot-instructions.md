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

## Maintaining Instructions

**If you detect discrepancies between these instructions and the actual implementation**, or discover patterns/features not documented here:

1. **Notify the user** about the discrepancy or missing documentation
2. **Suggest specific updates** to the relevant instruction file(s)
3. **Verify against current code** before making changes based on outdated instructions

This applies to:
- Main instructions (this file)
- Specific instruction files in `.github/instructions/`
- Examples that no longer match current patterns
- Missing documentation for new features or utilities

## Context-Specific Instructions

Additional detailed instructions are automatically applied based on the file you're working with:

- **Java files** (`fcli/**/*.java`): Java development guide with architecture, command structure, exceptions, and utilities
- **Action YAML files** (`fcli/**/actions/**/*.yaml`): Action YAML editing guide with SpEL expressions, schema validation, and patterns
- **All Java files** (`fcli/**/*.java`): Common utility classes documentation for use throughout the codebase
- **All fcli files** (`fcli/**/*`): Style guide with naming, formatting, and coding conventions

These instructions are defined in `.github/instructions/*.instructions.md` files with `applyTo` directives.

