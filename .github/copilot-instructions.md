# GitHub Copilot Instructions

## Project Overview
Fcli is a modular CLI tool for interacting with Fortify products (FoD, SSC, ScanCentral SAST/DAST). Built with Java 17, Gradle, and Picocli for command structure.

**Architecture:**
- Multi-module Gradle project: `fcli-core/*` (product modules), `fcli-other/*` (supporting modules)
- Module references defined in `gradle.properties` via `*Ref` properties (e.g., `fcliFoDRef=:fcli-core:fcli-fod`)
- Commands: `AbstractContainerCommand` (groups), `AbstractRunnableCommand` (leaf commands implementing `Callable<Integer>`)
- Output: Unified framework supporting JSON/CSV/XML/YAML/table via `IRecordWriter` implementations
- Session management: Product-specific descriptors (e.g., `FoDSessionDescriptor`), cached `UnirestInstance` per session
- Actions: YAML-based workflow automation with built-in library and custom import capability

## Development Workflow
- **Build:** `cd` to project root, run `./gradlew build` (don't run from subdirectories)
- **Validation:** After edits, use `get_errors` tool first, then full Gradle build to catch warnings
- **Testing:** Located in `src/test`; command structure validated in `FortifyCLITest`

## Code Conventions
- Target Java 17 features: records, text blocks, `var`, pattern matching for `instanceof`
- Prefer explicit imports; avoid wildcards
- Short methods (~20 lines max); extract helpers or use Streams for clarity
- No change-tracking comments (e.g., "New ...", "Updated ..."); only explanatory comments when code is complex
  
## Command Structure (Picocli-based)
**Container commands** (`AbstractContainerCommand`): Group subcommands; only define help option.  
**Leaf commands** (`AbstractRunnableCommand`): Implement `Callable<Integer>`; return 0 for success.

**Mixins pattern:**  
- Inject shared options/functionality via `@Mixin` (e.g., `OutputHelperMixins.List`, `UnirestInstanceSupplierMixin`)
- Mixins implementing `ICommandAware` receive `CommandSpec` injection for accessing command metadata
- `CommandHelperMixin`: Standard mixin providing access to `CommandSpec`, message resolver, root `CommandLine`

**Session management:**  
- Product modules extend `AbstractSessionLoginCommand`/`AbstractSessionLogoutCommand`
- Sessions stored in fcli state directory; access via `*SessionHelper.instance().get(sessionName)`
- UnirestInstance configured per session; managed by `*UnirestInstanceSupplierMixin` with automatic caching

**Output handling:**  
- Commands implement `IOutputConfigSupplier` to define default output format (table/json/csv/xml/yaml)
- `StandardOutputWriter` drives output; delegates to `RecordWriterFactory` enum for format-specific writers
- Data flow: `IObjectNodeProducer` → formatter/transformer → `IRecordWriter` → output stream

**Actions framework:**  
- YAML-defined workflows in `src/main/resources/.../actions/zip/` directories
- Schema version tracked in `gradle.properties` (`fcliActionSchemaVersion`)
- Steps like: `run.fcli` (execute fcli commands), `rest.call` (HTTP), `var.set` (variables), `for-each` (iteration)
- Action commands extend `AbstractActionRunCommand`; parsed options passed to `ActionRunner`

## Picocli Command Implementation Details
- Most leaf commands should extend from `Abstract<product>OutputCommand` or (legacy) `Abstract<product>[JsonNode|Request]OutputCommand`. Command name and output helper are usually defined through `OutputHelperMixins` (or a product-specific variant).
- Usage headers, command and option descriptions, and default table output options must be defined in the appropriate `*Messages.properties` resource bundle for the product/module.
- Unless a shared description is appropriate for shared options in mixins/arggroups, rely on Picocli's default message key lookup mechanism. Do not specify `descriptionKey` or similar attributes in Picocli annotations; let Picocli resolve keys based on command/field/option names.
- Use `@Mixin` to inject shared option groups or helpers. For product-specific shared options, create a dedicated mixin class.
- For commands that produce output, implement `IOutputConfigSupplier` to define the default output format and columns.
- For commands that require session or API context, use the appropriate `*UnirestInstanceSupplierMixin` and session helper pattern.
- All classes that may need to be accessed reflectively (e.g., for Jackson serialization/deserialization, action YAML mapping, or runtime plugin discovery) must be annotated with `@Reflectable`.
- When adding new commands, ensure they are registered as subcommands in the appropriate parent command class, and that all user-facing strings are externalized to the correct resource bundle.

## Exception Handling
Use hierarchy in `com.fortify.cli.common.exception` for consistent CLI error handling:

**Primary types:**
- `FcliSimpleException`: User-facing errors (invalid input, missing resource). Concise summary; suppresses underlying stack trace unless cause is non-simple.
- `FcliTechnicalException`: Unexpected technical failures (I/O, JSON parsing, network). Prints full stack trace.
- `FcliBugException`: Product defects/impossible states. Full stack trace; message should guide bug report.

**Decision matrix:**
1. Invalid/missing/ambiguous user input → `FcliSimpleException`
2. External resource not found (normal possibility) → `FcliSimpleException` with remediation guidance
3. User-initiated abort → `FcliAbortedByUserException` (extends `FcliSimpleException`)
4. Low-level failure (network, file I/O, JSON parse) → `FcliTechnicalException` (wrap cause)
5. Invariant violation, unreachable code → `FcliBugException`

**Message style:**
- Actionable: specify option name, expected format, remediation steps
- Sentence case; no trailing periods unless multiple sentences
- Multi-value options: use "|" for enums (`true|1|false|0`), ", " for sets
- Contextual IDs in single quotes only when ambiguous

**Wrapping:**
- Preserve root cause: `throw new FcliTechnicalException("Error reading "+file, e);`
- Convert third-party exceptions at boundaries; don't re-wrap `AbstractFcliException`
- Only wrap when adding context; otherwise propagate

**Examples:**
```java
if (StringUtils.isBlank(name)) throw new FcliSimpleException("--name must be specified");
try { parse(json); } catch (JsonProcessingException e) { throw new FcliTechnicalException("Error processing JSON", e); }
default -> throw new FcliBugException("Unexpected status: "+status);
```


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
- Interfaces start with capitcal 'I' (e.g., `IRecordWriter`, `IOutputConfigSupplier`).
- Prefer clarity over brevity in names.
- For related/specialized classes, append specifics to end for alphabetical grouping (e.g., `RecordWriterCsv`, `RecordWriterYaml` rather than `CsvRecordWriter`, `YamlRecordWriter`).
- Methods: camelCase verbs. Accessors for booleans use `is`/`has` prefixes.
- Constants: `UPPER_SNAKE_CASE`.
- Avoid abbreviations unless industry-standard (e.g., `ID`, `URL`, `JSON`).

### Comments & Javadoc
- Provide Javadoc for public types & methods: purpose, key parameters, return, error conditions.
- Avoid redundant comments restating code; focus on rationale, invariants, edge cases.
- Use `TODO:` for actionable future work when necessary.

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