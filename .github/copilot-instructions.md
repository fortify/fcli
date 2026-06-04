# Fcli — Copilot Instructions

Fcli is a modular Java 17 CLI (Picocli + Gradle) for Fortify products (FoD, SSC, ScanCentral SAST/DAST). Multi-module project: `fcli-core/*` (product modules), `fcli-other/*` (supporting modules).

## Quick Reference

- **Build:** `./gradlew build` from project root (creates `build/libs/fcli.jar`)
- **Unit tests:** `./gradlew test`
- **Functional tests:** `./gradlew :fcli-other:fcli-functional-test:ftest`
- **Validate:** Use `get_errors` tool first, then Gradle build

## Module Layout

- `fcli-common-thirdparty` — Patched vendor code (**do not modify** unless explicitly requested)
- `fcli-common-core` — Core framework (output, REST, SpEL, sessions, exceptions)
- `fcli-common-ci` / `fcli-common-action` / `fcli-common-tool` — CI, action engine, tool definitions
- `fcli-fod`, `fcli-ssc`, `fcli-sc-sast`, `fcli-sc-dast` — Product modules
- `fcli-aviator-common`, `fcli-aviator` — FPR parsing, Aviator CLI
- `fcli-ai-assist` — MCP server (only module with MCP SDK dep)
- `fcli-app` — Shadow jar assembly (only module with `picocli-codegen`)
- `build-logic/` — `fcli.java-conventions` (all modules), `fcli.module-conventions` (product modules; auto-adds common-core + common-thirdparty)

## Key Rules

- Focus on the repository containing the currently open file; other workspace folders are unrelated projects
- When in doubt about intent, ask the user rather than guessing
- Analyze existing code before implementing; trace callers of anything you change
- Prefer existing abstractions over creating new ones
- After edits: run `get_errors`, build with Gradle if appropriate

