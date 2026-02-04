# Common Utility Classes

The `fcli-common` module provides utility classes in `com.fortify.cli.common.util` for common operations. Always prefer these over direct JDK/third-party equivalents.

**Note for AI assistants:** If you discover utility classes, methods, or patterns in `fcli-common/src/main/java/com/fortify/cli/common/util/` that are not documented here, or if documented utilities appear outdated/incorrect, please notify the user and suggest updates to this section.

## Environment & Configuration

### `EnvHelper`
Environment variable access with fcli-specific features.

- Use `EnvHelper.env(name)` instead of `System.getenv()` — supports override via system properties (`fcli.env.VAR_NAME`) for testing
- Methods: `env()`, `envOrDefault()`, `requiredEnv()`, `envName()` (builds `FCLI_*` variable names), `asBoolean()`, `asInteger()`, `asCharArray()`
- Validation helpers: `checkSecondaryWithoutPrimary()`, `checkBothOrNone()`, `checkExclusive()`

### `FcliBuildProperties`
Access build metadata.

- Singleton: `FcliBuildProperties.INSTANCE`
- Methods: `getFcliVersion()`, `getFcliBuildDate()`, `getFcliActionSchemaVersion()`, `getFcliDocBaseUrl()`, etc.

### `FcliDataHelper`
Manage fcli data directories and files.

- Paths: `getFcliHomePath()`, `getFcliConfigPath()`, `getFcliStatePath()`
- File I/O: `saveFile()`, `readFile()`, `saveSecuredFile()` (encrypted), `readSecuredFile()`, `deleteFile()`, `deleteDir()`
- All paths are relative to fcli home directory; use `resolveFcliHomePath()` for absolute paths

## Version Handling

### `SemVer`
Semantic version parsing and comparison.

- Constructor takes any string; `isProperSemver()` indicates validity
- Accessors: `getMajor()`, `getMinor()`, `getPatch()`, `getLabel()` (all return `Optional`)
- Comparison: `compareTo()`, `isCompatibleWith()` (checks major.minor compatibility)
- Example: `new SemVer("1.2.3").isCompatibleWith("1.3.0")` → `true` (same major, minor >= required)

## Date/Time & Periods

### `DateTimePeriodHelper`
Parse period strings to durations.

- Supports: `ms`, `s`, `m`, `h`, `d`, `w`, `M` (months), `y` (years)
- Example: `"2h30m"` → 9000000 milliseconds
- Methods: `parsePeriodToMillis()`, `getCurrentDatePlusPeriod()`, `getCurrentOffsetDateTimePlusPeriod()`
- Create instances: `all()`, `byRange(Period.SECONDS, Period.DAYS)` for validation

## File Operations

### `FileUtils`
Advanced file/archive operations.

- Archive extraction: `extractZip()`, `extractTarGZ()` with path resolver for security (zip-slip protection)
- Pattern matching: `processMatchingFileStream()`, `processMatchingDirStream()` — process files matching Ant-style globs (`**/*.jar`)
- Permissions: `setAllFilePermissions()`, `setSinglePathPermissions()`
- Utilities: `deleteRecursive()`, `isDirPathInUse()`, `moveFiles()`, `pathToString(path, separatorChar)`

### `ZipHelper`
Stream-based zip entry processing.

- Use `processZipEntries(inputStream, processor)` to iterate zip entries without full extraction
- Processor returns `Break.TRUE` to stop iteration early

### `GzipHelper`
Gzip compression utilities.

- `gzipAndBase64(content)` — compress string with gzip and encode as Base64 (required by GitHub Code Scanning SARIF upload)
- `gzip(bytes)` — compress byte array using gzip, returns compressed byte array

## String & Formatting

### `StringHelper`
String manipulation utilities.

- `indent(str, indentStr)` — indent multi-line strings

## Reflection & Type Handling

### `JavaHelper`
Type-safe casting and null-handling.

- `as(obj, Class<T>)` → `Optional<T>` — safe cast, returns `Optional.empty()` if incompatible
- `is(obj, Class<T>)` → `boolean` — check if object is assignable to type
- `getOrCreate(obj, supplier)` — return obj if non-null, else create via supplier

### `ReflectionHelper`
Reflective operations.

- `getAllTypes(field)` — returns field type + generic type arguments as array
- `getGenericTypes(field)` — extract generic type arguments from parameterized fields
- `hasAnnotation()`, `getAnnotationValue()` — annotation introspection

## Control Flow & Counters

### `Break`
Enum for loop control (`Break.TRUE` / `Break.FALSE`).

- Use instead of `boolean` for clarity in processors/iterators
- Methods: `doBreak()`, `doContinue()`

### `Counter`
Simple mutable counter.

- Methods: `increase()`, `increase(long)`, `increase(Counter)`, `getCount()`

## Console & Debug

### `ConsoleHelper`
Terminal detection and width.

- `hasTerminal()` — check if running in interactive terminal
- `getTerminalWidth()` — get terminal width for formatting
- `installAnsiConsole()`, `uninstallAnsiConsole()` — manage JAnsi integration

### `DebugHelper`
Global debug flag.

- `isDebugEnabled()`, `setDebugEnabled()`

## Usage Principles

1. **Prefer fcli utilities**: Use `EnvHelper.env()` over `System.getenv()`, `FcliDataHelper` over raw `Files` API for fcli data
2. **Semantic versions**: Always use `SemVer` for version comparisons instead of string manipulation
3. **Period parsing**: Use `DateTimePeriodHelper` for user-input duration strings (e.g., CLI options)
4. **File patterns**: Use `FileUtils.processMatching*Stream()` for glob-based file filtering instead of manual pattern matching
5. **Safe casting**: Use `JavaHelper.as()` for type-safe optional casting instead of `instanceof` + cast
6. **Loop control**: Use `Break` enum in processors for clarity over `boolean` return values
