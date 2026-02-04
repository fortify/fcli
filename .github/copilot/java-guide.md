# Java Development Guide

## Architecture Overview

**Module structure:**
- Multi-module Gradle project: `fcli-core/*` (product modules), `fcli-other/*` (supporting modules)
- Module references defined in `gradle.properties` via `*Ref` properties (e.g., `fcliFoDRef=:fcli-core:fcli-fod`)

**Key components:**
- **Commands:** `AbstractContainerCommand` (command groups), `AbstractRunnableCommand` (leaf commands implementing `Callable<Integer>`)
- **Output:** Unified framework supporting JSON/CSV/XML/YAML/table via `IRecordWriter` implementations
- **Session management:** Product-specific descriptors (e.g., `FoDSessionDescriptor`), cached `UnirestInstance` per session
- **Actions:** YAML-based workflow automation in `src/main/resources/.../actions/zip/` directories
  - Schema version tracked in `gradle.properties` (`fcliActionSchemaVersion`)
  - Step types: `run.fcli`, `rest.call`, `var.set`, `for-each`, etc.
  - Action commands extend `AbstractActionRunCommand`; parsed options passed to `ActionRunner`

## Code Conventions

- Target Java 17 features: records, text blocks, `var`, pattern matching for `instanceof`
- Prefer explicit imports; avoid wildcards
- Always use imports, no fully qualified class names (unless this results in collision because same class name exists in multiple packages)
- Short methods (~20 lines max); extract helpers or use Streams for clarity
- No change-tracking comments (e.g., "New ...", "Updated ..."); only explanatory comments when code is complex

## Command Structure (Picocli-based)

**Container commands** (`AbstractContainerCommand`): Group subcommands; only define help option.  
**Leaf commands** (`AbstractRunnableCommand`): Implement `Callable<Integer>`; return 0 for success.

### Mixins Pattern

- Inject shared options/functionality via `@Mixin` (e.g., `OutputHelperMixins.List`, `UnirestInstanceSupplierMixin`)
- Mixins implementing `ICommandAware` receive `CommandSpec` injection for accessing command metadata
- `CommandHelperMixin`: Standard mixin providing access to `CommandSpec`, message resolver, root `CommandLine`

### Session Management

- Product modules extend `AbstractSessionLoginCommand`/`AbstractSessionLogoutCommand`
- Sessions stored in fcli state directory; access via `*SessionHelper.instance().get(sessionName)`
- UnirestInstance configured per session; managed by `*UnirestInstanceSupplierMixin` with automatic caching

### Output Handling

- Commands implement `IOutputConfigSupplier` to define default output format (table/json/csv/xml/yaml)
- `StandardOutputWriter` drives output; delegates to `RecordWriterFactory` enum for format-specific writers
- Data flow: `IObjectNodeProducer` → formatter/transformer → `IRecordWriter` → output stream

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

### Primary Types

- `FcliSimpleException`: User-facing errors (invalid input, missing resource). Concise summary; suppresses underlying stack trace unless cause is non-simple.
- `FcliTechnicalException`: Unexpected technical failures (I/O, JSON parsing, network). Prints full stack trace.
- `FcliBugException`: Product defects/impossible states. Full stack trace; message should guide bug report.

### Decision Matrix

1. Invalid/missing/ambiguous user input → `FcliSimpleException`
2. External resource not found (normal possibility) → `FcliSimpleException` with remediation guidance
3. User-initiated abort → `FcliAbortedByUserException` (extends `FcliSimpleException`)
4. Low-level failure (network, file I/O, JSON parse) → `FcliTechnicalException` (wrap cause)
5. Invariant violation, unreachable code → `FcliBugException`

### Message Style

- Actionable: specify option name, expected format, remediation steps
- Sentence case; no trailing periods unless multiple sentences
- Multi-value options: use "|" for enums (`true|1|false|0`), ", " for sets
- Contextual IDs in single quotes only when ambiguous

### Wrapping

- Preserve root cause: `throw new FcliTechnicalException("Error reading "+file, e);`
- Convert third-party exceptions at boundaries; don't re-wrap `AbstractFcliException`
- Only wrap when adding context; otherwise propagate

### Examples

```java
if (StringUtils.isBlank(name)) throw new FcliSimpleException("--name must be specified");
try { parse(json); } catch (JsonProcessingException e) { throw new FcliTechnicalException("Error processing JSON", e); }
default -> throw new FcliBugException("Unexpected status: "+status);
```

## Common Utility Classes

The `fcli-common` module provides utility classes in `com.fortify.cli.common.util` for common operations. Always prefer these over direct JDK/third-party equivalents.

**Note for AI assistants:** If you discover utility classes, methods, or patterns in `fcli-common/src/main/java/com/fortify/cli/common/util/` that are not documented below, or if documented utilities appear outdated/incorrect, please notify the user and suggest updates to this section.

See [utilities-guide.md](utilities-guide.md) for complete documentation of available utility classes.

## Style Guide

See [style-guide.md](style-guide.md) for detailed coding style guidelines.
