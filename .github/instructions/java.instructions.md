---
name: 'Java Development Guide'
description: 'Java development patterns for fcli commands, exceptions, and utilities'
applyTo: 'fcli/**/*.java'
---

# Java Development Guide

## Architecture

- **Commands:** `AbstractContainerCommand` (groups), `AbstractRunnableCommand` (leaf, `Callable<Integer>`)
- **Output:** `IRecordWriter` implementations via `RecordWriterFactory`; commands implement `IOutputConfigSupplier`
- **Sessions:** Product-specific descriptors, cached `UnirestInstance` per session via `*UnirestInstanceSupplierMixin`
- **Actions:** YAML in `src/main/resources/.../actions/zip/`; extend `AbstractActionRunCommand`

## Command Patterns

- Leaf commands extend `Abstract<product>OutputCommand`; name/output via `OutputHelperMixins`
- Descriptions and table columns in `*Messages.properties`; rely on Picocli default key lookup (don't set `descriptionKey`)
- Every command needs `fcli.<path>.usage.header` in `*Messages.properties` — `FortifyCLITest` validates this
- Use `@Mixin` for shared options; `@Reflectable` for Jackson/reflection-accessed classes
- Only use `@DisableTest` when a genuine design conflict exists with `FortifyCLITest`
- Register new commands as subcommands in parent; externalize all user-facing strings

## Unirest HTTP Headers

Always use `headerReplace(name, value)` — never `accept()`, `contentType()`, or `header()` (they add duplicates). Use `HttpHeader.*` constants.

## Exception Handling

- New/updated code should throw only fcli-domain exceptions (`Fcli*Exception`), module-domain exceptions (for example `Aviator*Exception` in Aviator modules), or picocli exceptions (`ParameterException` and related) when integrating with command parsing.
- Avoid throwing standard Java runtime exceptions (`IllegalArgumentException`, `IllegalStateException`, `RuntimeException`, and similar) for user-facing or command-flow errors.

| Scenario | Exception |
|----------|-----------|
| Invalid/missing user input | `FcliSimpleException` |
| External resource not found | `FcliSimpleException` with remediation |
| User abort | `FcliAbortedByUserException` |
| I/O, network, JSON parse | `FcliTechnicalException` (wrap cause) |
| Invariant violation, unreachable | `FcliBugException` |

Messages: actionable, sentence case, no trailing periods. Preserve root cause in wrapping.

## Design Patterns

- **Template Method:** Override narrowest hook in abstract base classes
- **Strategy:** `IOutputConfigSupplier`, `IRecordWriter`, `UnirestInstanceSupplierMixin` — inject via `@Mixin`
- **Factory/Registry:** `RecordWriterFactory`, `OutputHelperMixins` — extend enum/factory, don't modify consumers
- **Composite:** Command tree — containers have zero business logic
- **Separation of concerns:** Commands parse+orchestrate; helpers hold logic; writers shape output
