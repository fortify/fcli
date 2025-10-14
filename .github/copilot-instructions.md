# GitHub Copilot Instructions

- Before running a Gradle build, 'cd' into project directory
- After making any changes, please check for any warnings or errors introduced by those changes and fix if needed; first check using get_errors tool, once that no longer reports any errors, run full gradle build
- Avoid duplication; if needed, refactor to allow for a more generic solution
- Prefer imports over fully qualified  class names
- Prefer short methods by splitting into multiple methods or utilizing modern Java features, optimize for human readibility
- Prefer short methods; split if necessary
- Utilize Java 17 features like Streams to minimize/optimize code
- Don't add comments that describe changes, like 'New ...', 'Updated ...'. Comments should only be used to explain code if necessary

## Detailed Style Guide (AI & Manual Edits)

### General
- Follow `.editorconfig` for indentation (4 spaces Java, 2 spaces YAML/JSON/Markdown), LF endings, trimmed trailing whitespace, final newline.
- Keep line length <= 140 characters; wrap earlier if readability improves.
- Prefer small, cohesive methods; extract private helpers instead of long procedural blocks.
- Avoid duplication; factor out common logic (prefer composition over inheritance when practical).

### Java Language Usage
- Target Java 17 features: records (where immutable data carriers), `var` for obvious local types, text blocks for multi-line strings, pattern matching for `instanceof` when it clarifies code.
- Use Streams for clear transformations/filtering, but favor simple for-loops if they are more readable or avoid unnecessary allocations.
- Use `Optional` sparingly (avoid in hot inner loops or for simple nullable fields inside DTOs).

### Imports & Formatting
- Always use explicit imports; avoid wildcard imports.
- Order: static imports (grouped), then normal imports alphabetically; keep separation between 3rd-party and internal logical groups only if automated tooling maintains it.
- No unused imports; remove immediately.

### Naming
- Classes: PascalCase. Interfaces describing capabilities may use verbs/adjectives (e.g., `ObjectNodeProducerSupplier`, `RecordWriterFactory`).
- Methods: camelCase verbs. Accessors for booleans use `is`/`has` prefixes.
- Constants: `UPPER_SNAKE_CASE`.
- Avoid abbreviations unless industry-standard (e.g., `ID`, `URL`, `JSON`).

### Comments & Javadoc
- Provide Javadoc for public types & methods: purpose, key parameters, return, error conditions.
- Avoid redundant comments restating code; focus on rationale, invariants, edge cases.
- Use `TODO(username):` for actionable future work when necessary.

### Error Handling
- Throw specific exceptions (custom domain exceptions or `IllegalArgumentException`, `IllegalStateException`) rather than generic ones.
- Fail fast on invalid arguments; validate early.
- When wrapping exceptions, retain root cause.

### Null & Collections
- Prefer non-null return values; return empty collections instead of `null`.
- Use `Objects.requireNonNull()` for mandatory constructor parameters.
- Use defensive copies for mutable internal collections exposed via getters.

### Performance & Memory
- Avoid premature optimization; measure first. However, do not create intermediate Stream collections when direct streaming suffices.
- Reuse immutable objects; favor unmodifiable views for returned collections.

### Concurrency
- Mark shared mutable state clearly; use final where possible.
- Prefer immutable data carriers or confined mutation.

### Testing Considerations (when adding tests)
- Happy-path test plus at least one edge case.
- Use descriptive test names; avoid over-mocking—favor real collaborators where cheap.

### Output / CLI Specific
- Keep formatting responsibilities in writer/formatter classes; avoid embedding formatting in commands.
- Record producers should remain side-effect free except for streaming output records.
- Avoid static mutable state; prefer instance-level control (see recent refactor removing static collectors).

### AI Assistant Expectations
- Before large edits: scan related files (search by symbol) to avoid breaking contracts.
- After edits: run compile, address warnings if feasible.
- Never introduce commented-out code blocks; remove instead.
- Provide incremental, minimal diffs; do not reformat unrelated code.
- If refactoring signature changes, update all usages in same change.

### Pull Request Hygiene
- Keep PRs scoped: one logical change set (feature, refactor, bugfix) per PR when possible.
- Include concise summary of intent and any trade-offs.

---
If a rule conflicts with clarity, choose clarity and (optionally) document the exception.