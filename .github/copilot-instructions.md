# GitHub Copilot Instructions

- Before running a Gradle build, 'cd' into project directory
- After making any changes, please check for any warnings or errors introduced by those changes and fix if needed; first check using get_errors tool, once that no longer reports any errors, run full gradle build
- Avoid duplication; if needed, refactor to allow for a more generic solution
- Prefer imports over fully qualified  class names
- Prefer short methods by splitting into multiple methods or utilizing modern Java features, optimize for human readibility
- Prefer short methods; split if necessary
- Utilize Java 17 features like Streams to minimize/optimize code
- Don't add comments that describe changes, like 'New ...', 'Updated ...'. Comments should only be used to explain code if necessary
  
## Exception Handling
Don't throw standard Java exceptions (like RuntimeException) for CLI-surface errors; prefer the hierarchy in `com.fortify.cli.common.exception` so output and exit codes are consistent.

Primary types:
    - FcliSimpleException: Expected/user-facing error (invalid input, missing resource, conflicting options). Shows concise summary; underlying cause stack trace suppressed unless it's another non-simple exception.
    - FcliTechnicalException: Unexpected technical failure (I/O, parsing, remote API protocol issue) that isn't a product bug but may need investigation. Full stack trace is printed.
    - FcliBugException: Indicates a product defect or impossible state (logic invariant broken, unreachable code reached). Full stack trace printed. Message should guide user to file a bug.

Additional specialized subclasses may extend these (e.g. `FcliAbortedByUserException` for deliberate aborts, session/logout variants). Choose the most specific subclass available; otherwise use one of the primary types above.

Decision matrix (choose first matching row):
    1. User-provided input invalid, missing, ambiguous, conflicting -> FcliSimpleException.
    2. External system/resource not found or access denied and this is a normal possibility -> FcliSimpleException (clear guidance, possible remediation).
    3. Operation aborted intentionally by user confirmation flow -> FcliAbortedByUserException (subclass of FcliSimpleException).
    4. Unexpected low-level failure (network hiccup, JSON parse error, file read error) -> FcliTechnicalException (wrap original cause).
    5. Invariant violation, impossible branch, internal misuse of API -> FcliBugException.

Message guidelines:
    - Be actionable: state what was wrong and how to correct (option name, expected format, required precondition).
    - Avoid starting with lowercase; sentence case preferred. No trailing periods unless multiple sentences.
    - Keep within one line when feasible; use "\n" for multi-line details (FcliSimpleException formatting maintains indentation).
    - For multi-value guidance prefer lists separated by "|" for enums (e.g. true|1|false|0) or "<value1>, <value2>" for sets.
    - Include contextual identifiers (names, ids) quoted only when necessary to disambiguate. Prefer single quotes.

Wrapping causes:
    - Always preserve root cause in constructor when useful: `throw new FcliTechnicalException("Error reading file "+file, e);`
    - For FcliSimpleException, passing a cause will print either a concise summary (if cause is ParameterException or another FcliSimpleException) or the full stack of the cause for other types.
    - Do NOT build manual stack trace strings; rely on `getStackTraceString()` implementation.

Exit codes:
    - Set a custom exit code only when needed (`exception.exitCode(<code>)` after construction). Default picocli execution exception exit code otherwise.
    - Keep mapping stable; introduce new codes deliberately (document in CHANGELOG).

When to rethrow vs convert:
    - Convert known checked/third-party exceptions at boundary layers (REST helpers, file readers) to FcliTechnicalException.
    - Let existing AbstractFcliException instances propagate unchanged (avoid wrapping to keep message formatting intact).
    - Only catch and wrap if you can add meaningful context; otherwise allow bubble-up.

Avoid:
    - Throwing generic RuntimeException, Exception.
    - Logging and then throwing for Simple exceptions (output already user-focused); log debug if extra diagnostics desired.
    - Swallowing cause details—always attach original cause for technical/bug exceptions.

Examples:
    - Validation: `if (StringUtils.isBlank(name)) throw new FcliSimpleException("--name must be specified");`
    - Remote parse: `try { parse(json); } catch (JsonProcessingException e) { throw new FcliTechnicalException("Error processing JSON data", e); }`
    - Impossible branch: `default -> throw new FcliBugException("Unexpected scan status: "+status);`

Subclasses: If adding a new subclass, extend the closest existing base (`FcliSimpleException` for user-level semantics) and keep constructors mirroring base for consistency. Provide a distinct semantic purpose; avoid proliferation for single-use cases.

Testing: For command tests expecting failures, assert on exception class and message prefix (not full text) to allow minor wording refinements.


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