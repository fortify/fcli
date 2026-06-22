---
name: fcli-utilities
description: >-
  Discover and use fcli-common utility classes when implementing new Java code.
  Use when adding new code that needs file I/O, environment access, date parsing,
  version comparison, string manipulation, type casting, or other common operations.
  NOT needed for code review, refactoring, or analysis tasks.
user-invocable: true
disable-model-invocation: false
---

# fcli-common Utility Classes

When implementing new Java code in fcli, always prefer project utility classes over
direct JDK/third-party equivalents. This skill helps you discover what's available.

## Step 1: Discover available utilities

Run these commands to find utility classes and their public methods:

```bash
# List all utility classes
find fcli-core/fcli-common/src/main/java/com/fortify/cli/common/util/ -name "*.java" -exec basename {} .java \;

# Show public methods for all utility classes
grep -rn 'public.*static\|public.*class\|public.*interface\|public.*enum' \
  fcli-core/fcli-common/src/main/java/com/fortify/cli/common/util/ \
  --include="*.java" | grep -v 'test'
```

## Step 2: Check for relevant utility before writing new code

Before implementing common operations, check if a utility exists:

| Operation | Check class | Key methods |
|-----------|-------------|-------------|
| Environment variables | `EnvHelper` | `env()`, `envOrDefault()`, `requiredEnv()` — use instead of `System.getenv()` |
| Fcli data dirs/files | `FcliDataHelper` | `getFcliHomePath()`, `saveFile()`, `saveSecuredFile()` |
| Build metadata | `FcliBuildProperties` | `INSTANCE.getFcliVersion()`, `getFcliActionSchemaVersion()` |
| Semantic versions | `SemVer` | `compareTo()`, `isCompatibleWith()` |
| Duration parsing | `DateTimePeriodHelper` | `parsePeriodToMillis()` — supports `2h30m`, `1d`, `1w` |
| Zip/tar extraction | `FileUtils` | `extractZip()`, `extractTarGZ()` — zip-slip protected |
| File glob matching | `FileUtils` | `processMatchingFileStream()` — Ant-style globs |
| Gzip + Base64 | `GzipHelper` | `gzipAndBase64()` — for GitHub SARIF upload |
| Safe type casting | `JavaHelper` | `as(obj, Class)` → `Optional<T>` |
| Loop control | `Break` | `Break.TRUE`/`Break.FALSE` enum for processors |
| Log masking | `LogMaskHelper` | `registerValue()` — use `@MaskValue` annotation on fields |

## Step 3: Read the source if you need details

If you need method signatures or behavior details for a specific utility class:

```bash
# Read specific utility class
cat fcli-core/fcli-common/src/main/java/com/fortify/cli/common/util/<ClassName>.java
```

Also check `com.fortify.cli.common.cli.util` for execution context classes:
- `FcliExecutionContext` / `FcliExecutionContextHolder` — per-invocation context
- `FcliIsolationScope` — auth/session boundary isolation
- `FcliActionState` — mutable `global.*` action variables
