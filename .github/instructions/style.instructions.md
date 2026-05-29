---
name: 'Style Guide'
description: 'Coding style and conventions for fcli'
applyTo: 'fcli/**/*'
---

# Detailed Style Guide (AI & Manual Edits)

## Maintaining These Instructions

**If you detect discrepancies** between these instructions and the actual codebase patterns, or discover conventions not documented here:

1. **Automatically update this file** to reflect the correct pattern
2. **Consider whether the discrepancy represents** an intentional exception (clarity over rules) or an outdated instruction
3. **Verify against current code** before documenting — never guess at intent

## General

- Follow `.editorconfig` for indentation (4 spaces Java, 2 spaces YAML/JSON/Markdown), LF endings, trimmed trailing whitespace, final newline.
- Keep line length <= 140 characters; wrap earlier if readability improves.
- Prefer small, cohesive methods; extract private helpers instead of long procedural blocks.
- Avoid duplication; factor out common logic (prefer composition over inheritance when practical).

## Java Language Usage

- Target Java 17 features: records (where immutable data carriers), `var` for obvious local types, text blocks for multi-line strings, pattern matching for `instanceof` when it clarifies code.
- Use Streams for clear transformations/filtering, but favor simple for-loops if they are more readable or avoid unnecessary allocations.
- Use `Optional` sparingly (avoid in hot inner loops or for simple nullable fields inside DTOs).

## Lombok Usage

Always prefer Lombok annotations over hand-written boilerplate. Key rules beyond the obvious:

- **`@Getter(lazy = true)`** — for expensive fields computed once on first access (thread-safe).
- **`@Getter(value = AccessLevel.PRIVATE)`** — restrict getter visibility; combine with `@Accessors(fluent = true)` for fluent data-holder APIs.
- **`@Builder` + `@RequiredArgsConstructor(access = AccessLevel.PRIVATE)`** — enforces construction via builder only; use `@Builder.Default` for non-null field defaults.
- **Jackson-deserialized descriptors (`@Reflectable`)** — do NOT add `@Builder`; use `@NoArgsConstructor` + `@Data` / `@Getter` (Jackson requires a no-arg constructor).
- **`@Data` with inheritance** — always add `@EqualsAndHashCode(callSuper = true)` explicitly.
- **`@Slf4j`** — preferred for new code; older code uses `LoggerFactory.getLogger(getClass())`.
- **Avoid** `@Value` (use Java `record`), `@With`, `@SuperBuilder` — not currently in use.

## Imports & Formatting

- Always use explicit imports; avoid wildcard imports.
- Always use imports, no fully qualified class names (unless this results in collision because same class name exists in multiple packages)
- Order: static imports (grouped), then normal imports alphabetically; keep separation between 3rd-party and internal logical groups only if automated tooling maintains it.
- No unused imports; remove immediately.

## Naming

- Classes: PascalCase. Interfaces describing capabilities may use verbs/adjectives (e.g., `ObjectNodeProducerSupplier`, `RecordWriterFactory`).
- Interfaces start with capital 'I' (e.g., `IRecordWriter`, `IOutputConfigSupplier`).
- Prefer clarity over brevity in names.
- For related/specialized classes, append specifics to end for alphabetical grouping (e.g., `RecordWriterCsv`, `RecordWriterYaml` rather than `CsvRecordWriter`, `YamlRecordWriter`).
- Methods: camelCase verbs. Accessors for booleans use `is`/`has` prefixes.
- Constants: `UPPER_SNAKE_CASE`.
- Avoid abbreviations unless industry-standard (e.g., `ID`, `URL`, `JSON`).

## Comments & Javadoc

- Provide Javadoc for public types & methods: purpose, key parameters, return, error conditions.
- Avoid redundant comments restating code; focus on rationale, invariants, edge cases.
- Use `TODO:` for actionable future work when necessary.

## Null & Collections

- Prefer non-null return values; return empty collections instead of `null`.
- Use `Objects.requireNonNull()` for mandatory constructor parameters.
- Use defensive copies for mutable internal collections exposed via getters.

## Performance & Memory

- Avoid premature optimization; measure first. However, do not create intermediate Stream collections when direct streaming suffices.
- Reuse immutable objects; favor unmodifiable views for returned collections.

## Concurrency

- Mark shared mutable state clearly; use final where possible.
- Prefer immutable data carriers or confined mutation.

## Testing Considerations (when adding tests)

- Happy-path test plus at least one edge case.
- Use descriptive test names; avoid over-mocking—favor real collaborators where cheap.

## Output / CLI Specific

- Keep formatting responsibilities in writer/formatter classes; avoid embedding formatting in commands.
- Record producers should remain side-effect free except for streaming output records.
- Avoid static mutable state; prefer instance-level control (see recent refactor removing static collectors).

## Security Mindset

Apply OWASP principles defensively — even in a CLI tool that processes data from trusted sources:

- **Injection:** Although injecting user input into shell commands, file paths, SpEL/template expression is fairly common in fcli, always verify the potential security impact and apply proper safeguards if appropriate.
- **Isolation:** Avoid static mutable state that could be manipulated across command executions; prefer instance-level state or thread confinement. For multi-user server scenarios like MCP HTTP server, ensure proper data isolation.
- **Sensitive data exposure:** Don't log or print credentials, tokens, or secrets; use `saveSecuredFile()` / `readSecuredFile()` for persisted session credentials, and make sure that log masking is applied (see `LogMaskHelper`)
- **Path traversal:** Always resolve user-supplied paths against an expected root; use `FileUtils`'s zip-slip-protected extraction methods rather than raw `ZipEntry.getName()`
- **Deserialization:** Prefer Jackson with explicit type constraints; avoid `ObjectMapper.enableDefaultTyping()` or polymorphic `@JsonTypeInfo` with user-controlled type names
- **Dependency hygiene:** Don't add new dependencies without reviewing their transitive impact; prefer well-maintained libraries already on the classpath

## AI Assistant Expectations

- **Analyze before implementing:** Trace through existing abstractions with `semantic_search`, `grep_search`, and `vscode_listCodeUsages` before writing a single line. Understand the design intent, not just the surface syntax.
- **Reuse, don't reinvent:** Identify the most appropriate existing base class, mixin, or utility before creating new abstractions. Fcli has rich shared infrastructure — use it.
- **Apply SOLID principles:** Single Responsibility (each class does one thing), Open/Closed (extend via interfaces/abstractions, not modification), Liskov Substitution (subtypes must honor contracts), Interface Segregation (small focused interfaces), Dependency Inversion (depend on abstractions).
- **Separation of concerns:** Commands parse options and orchestrate; helpers/services contain business logic; writers handle output. Don't embed formatting in commands or business logic in writers.
- **Before large edits:** Scan related files (search by symbol) to avoid breaking contracts.
- **After edits:** Run `get_errors`, address any issues; build with Gradle if appropriate.
- **Never introduce commented-out code blocks;** remove instead.
- **Provide incremental, minimal diffs;** do not reformat unrelated code.
- **If refactoring changes a signature,** update all usages in the same change.
- **Self-update these instructions** when you discover a pattern, pitfall, or convention not yet documented here — make the edit immediately as part of the task.

## Pull Request Hygiene

- Keep PRs scoped: one logical change set (feature, refactor, bugfix) per PR when possible.
- Include concise summary of intent and any trade-offs.

---
If a rule conflicts with clarity, choose clarity and (optionally) document the exception.
