# Code Style Guide

This project enforces a consistent style for both manual and AI-assisted edits. The essentials are summarized here; AI guidance lives in `.github/copilot-instructions.md`.

## Formatting & Files
- Configuration: see `.editorconfig` (authoritative for whitespace, line endings, indentation).
- Max line length: 140 (wrap earlier if clarity improves).
- Always end files with a newline; no trailing whitespace.

## Java
- Target Java 17 features when they improve clarity (records, pattern matching, text blocks, `var` for obvious locals).
- Prefer composition over inheritance; extract small, cohesive methods.
- Avoid duplication—refactor common logic.
- Streams: use for clear transformation pipelines; avoid when a simple loop is clearer or more efficient.
- Avoid unnecessary intermediate collections; stream directly to the consumer when possible.

## Imports
- No wildcard imports; remove unused imports immediately.
- Static imports first (grouped), then normal imports alphabetically.

## Naming
- Classes & Records: PascalCase.
- Interfaces that describe a capability may use adjectives/verbs (e.g., `ObjectNodeProducerSupplier`).
- Methods: camelCase verbs (`calculateTotal`).
- Booleans: `is`, `has`, `can`, `should` prefixes where appropriate.
- Constants: UPPER_SNAKE_CASE.

## Null Handling & Collections
- Return empty collections instead of `null`.
- Use `Objects.requireNonNull()` for mandatory constructor parameters.
- Defensive copies or unmodifiable views for exposed mutable collections.

## Error Handling
- Fail fast on invalid input; throw specific exceptions (`IllegalArgumentException`, `IllegalStateException`, domain exceptions).
- Preserve root causes when wrapping.

## Comments & Documentation
- Public types/methods: Javadoc with purpose + key params + return + error conditions.
- Comments explain *why*, not *what* (avoid restating code).
- Use `TODO(username):` for actionable follow-ups.
- No commented-out code blocks; delete instead (git history preserves old code).

## Performance
- Avoid premature optimization; measure before tuning.
- Reuse immutable objects; prefer unmodifiable snapshots for shared state.
- Avoid creating temporary objects inside tight loops unless necessary.

## Concurrency
- Prefer immutability; mark shared fields `final` where possible.
- Explicitly document any shared mutable state.

## Testing
- Each change should include (or preserve) happy-path coverage and at least one edge case.
- Tests should be deterministic—no reliance on external network/services unless mocked or explicitly integration-level.

## CLI / Output Specifics
- Formatting belongs in writer/formatter components, not commands.
- Record producers should be side-effect free aside from streaming output.
- Avoid new static mutable state—prefer instance-scoped or dependency-injected collaborators.

## Pull Requests
- Scope narrowly: one feature/refactor/bugfix per PR.
- Include concise summary of intent, trade-offs, and any follow-up tasks.

## Deviations
If a rule conflicts with clarity, choose clarity and optionally note rationale in a brief comment or PR description.

---
For AI-specific operational rules (tool usage, build validation), see `.github/copilot-instructions.md`.
