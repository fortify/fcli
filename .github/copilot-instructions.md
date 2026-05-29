# GitHub Copilot Instructions

## Project Overview

Fcli is a modular CLI tool for interacting with Fortify products (FoD, SSC, ScanCentral SAST/DAST).

**Key characteristics:**
- Built with Java 17, Gradle, and Picocli for command structure
- Multi-module Gradle project: `fcli-core/*` (core modules), `fcli-other/*` (supporting modules)
- Module references defined in `gradle.properties` via `*Ref` properties
- Unified output framework (JSON/CSV/XML/YAML/table), session management, and YAML-based action automation

## Module Structure

### Common modules (`fcli-core/fcli-common-*`)
- **fcli-common-thirdparty** — Patched third-party classes (picocli `CommandLine`/`AutoComplete`, Spring `JsonPropertyAccessor`). **Do not modify these files unless explicitly requested** — they are vendored patches of upstream libraries with specific behavioral fixes.
- **fcli-common-core** — Core CLI framework: output, JSON/REST, SpEL, sessions, HTTP, crypto, logging, progress, exceptions. Depends on fcli-common-thirdparty.
- **fcli-common-ci** — CI platform integration (GitHub Actions, GitLab CI, ADO, Bitbucket). Depends on fcli-common-core (via module-conventions auto-dependency).
- **fcli-common-action** — Action engine: YAML action model, runner, schema, CLI commands, concurrent utilities. Depends on fcli-common-ci.
- **fcli-common-tool** — Tool definitions API (6 files). Depends on fcli-common-core (via module-conventions auto-dependency).

### Product modules (`fcli-core/fcli-*`)
- **fcli-fod** — Fortify on Demand. Depends on fcli-common-action, fcli-aviator-common.
- **fcli-ssc** — Software Security Center. Depends on fcli-common-action.
- **fcli-sc-sast** / **fcli-sc-dast** — ScanCentral SAST/DAST. Depend on fcli-ssc.
- **fcli-tool** — Tool installation/management. Depends on fcli-common-tool.
- **fcli-aviator-common** — FPR parsing, auditing, protobuf/gRPC. Depends on fcli-common-core (hardcoded).
- **fcli-aviator** — Aviator CLI. Depends on fcli-aviator-common, fcli-ssc, fcli-fod.
- **fcli-ai-assist** — AI assistant / MCP server. Depends on fcli-common-action, fcli-common-tool, fcli-fod, fcli-ssc. Only module with MCP SDK dependency.
- **fcli-action** / **fcli-config** / **fcli-license** / **fcli-util** — Smaller product modules.

### Assembly & supporting
- **fcli-app** — Shadow jar assembly. Depends on all product modules. Only module with `picocli-codegen` annotation processor.
- **fcli-other/fcli-bom** — Dependency version management (BOM).
- **fcli-other/fcli-doc** — Documentation generation (AsciiDoc).
- **fcli-other/fcli-functional-test** — Functional test suite.

### Build conventions (in `build-logic/`)
- **FcliJavaConventionsPlugin** (`fcli.java-conventions`) — Applied to all modules. Configures Java 17, Lombok, library dependencies, GraalVM resource-config generation (conditional — only for modules with resources), action zip packaging (auto-discovered).
- **FcliModuleConventionsPlugin** (`fcli.module-conventions`) — Applied to product modules. Extends java-conventions; auto-adds `fcliCommonRef` and `fcliCommonThirdpartyRef` as dependencies (skipping self).

### Dependency rules
- `fcli.module-conventions` auto-adds fcli-common-core and fcli-common-thirdparty; other common module deps must be declared explicitly
- MCP SDK is scoped to fcli-ai-assist only (not in the convention plugin)
- `picocli-codegen` annotation processor is scoped to fcli-app only

## Development Workflow

- **Build:** `cd` to project root, run `./gradlew build` (don't run from subdirectories)
  - Creates shadow jar at `fcli-core/fcli-app/build/libs/fcli.jar` and copies to `build/libs/fcli.jar`
  - Module-specific tasks (`shadowJar`, `dist`, `distAll`) do NOT create root `build/libs/fcli.jar`
  - Always run `./gradlew build` before manual testing if local script depends on `build/libs/fcli.jar`
- **Validation:** Use `get_errors` tool first, then full Gradle build to catch warnings
- **Testing:**
  - Unit tests: `src/test`; command structure validated in `FortifyCLITest`
  - Functional tests: `fcli-other/fcli-functional-test` module; run with `./gradlew :fcli-other:fcli-functional-test:ftest`

## Code Conventions

- Target Java 17 features: records, text blocks, `var`, pattern matching for `instanceof`
- Prefer explicit imports; avoid wildcards
- **Always use imports, not fully qualified class names** (unless collision with same class name from multiple packages)
- Short methods (~20 lines max); extract helpers or use Streams for clarity
- No change-tracking comments (e.g., "New ...", "Updated ..."); only explanatory comments when code is complex

## Agent Behavior

**Think and work as a senior/expert developer** on every task:
- Apply SOLID principles, proper separation of concerns, and established design patterns
- Treat code quality and security as non-negotiable, not optional extras
- Never guess or make assumptions about how existing code works — analyze first:
  1. Use `semantic_search`, `grep_search`, and `read_file` to trace through related code
  2. Find all callers of anything you change (`vscode_listCodeUsages`)
  3. Understand the design intent before writing a single line
- Identify and use the most appropriate existing abstraction instead of creating a new one
- If a request is ambiguous, surface the design trade-offs and ask; do not silently pick one

## Self-Learning Instructions

**Automatically update these instruction files** when you learn or implement something new that would help in future sessions. Do not just notify the user — make the edit as part of completing the task.

Update the file that best scopes the knowledge:
- `copilot-instructions.md` — project-wide workflow facts and agent behavior rules
- `.github/instructions/java.instructions.md` — Java patterns, architecture, APIs
- `.github/instructions/style.instructions.md` — naming, formatting, AI assistant rules
- `.github/instructions/utilities.instructions.md` — utility class discoveries

Rules for self-updating:
- Only add content that is **reusable and non-obvious** (things a smart developer wouldn't know without reading the source)
- Keep additions **concise** — bullet points and short examples, not prose explanations; no introductory paragraphs
- A method signature + one-liner purpose is enough; omit obvious behaviour
- Correct or remove instructions that turn out to be wrong
- Never duplicate existing content; prefer extending an existing section
- Verify the change compiles/is consistent before updating instructions

**If you detect discrepancies** between these instructions and the actual implementation:
1. **Fix the instruction** to match reality (or fix the code if it's a bug)
2. **Verify against current code** before proceeding — never rely on stale instructions

This applies to all instruction files in `.github/instructions/`.

## Context-Specific Instructions

Additional detailed instructions are automatically applied based on the file you're working with:

- **Java files** (`fcli/**/*.java`): Java development guide with architecture, command structure, exceptions, and utilities
- **Action YAML files** (`fcli/**/actions/**/*.yaml`): Action YAML editing guide with SpEL expressions, schema validation, and patterns
- **All Java files** (`fcli/**/*.java`): Common utility classes documentation for use throughout the codebase
- **All fcli files** (`fcli/**/*`): Style guide with naming, formatting, and coding conventions

These instructions are defined in `.github/instructions/*.instructions.md` files with `applyTo` directives.

