---
name: 'Style Guide'
description: 'Coding style and conventions for fcli'
applyTo: 'fcli/**/*'
---

# Style Guide

## General

- Follow `.editorconfig` for indentation (4 spaces Java, 2 spaces YAML/JSON/Markdown), LF endings, trimmed trailing whitespace, final newline
- Line length <= 140 characters
- Small cohesive methods; extract private helpers instead of long procedural blocks
- Avoid duplication; prefer composition over inheritance when practical

## Java

- Java 17 features: records, `var` for obvious types, text blocks, pattern matching for `instanceof`
- Streams for clear transformations; simple for-loops if more readable
- `Optional` sparingly (not in hot loops or simple nullable DTO fields)

## Lombok

- Prefer Lombok over hand-written boilerplate
- `@Getter(lazy = true)` for expensive computed-once fields
- `@Builder` + `@RequiredArgsConstructor(access = AccessLevel.PRIVATE)` for builder-only construction
- Jackson-deserialized classes (`@Reflectable`): use `@NoArgsConstructor` + `@Data`/`@Getter`, NOT `@Builder`
- `@Data` with inheritance: always add `@EqualsAndHashCode(callSuper = true)`
- `@Slf4j` for new code
- Avoid `@Value` (use records), `@With`, `@SuperBuilder`

## Imports & Naming

- Explicit imports only; no wildcards; no unused imports
- Always use imports, not FQCNs (unless same-name collision)
- Classes: PascalCase; interfaces: `I` prefix (e.g., `IRecordWriter`)
- Append specifics for alphabetical grouping: `RecordWriterCsv` not `CsvRecordWriter`
- Methods: camelCase verbs; boolean accessors: `is`/`has` prefix
- Constants: `UPPER_SNAKE_CASE`

## Security

- Don't log/print credentials; use `@MaskValue` and `LogMaskHelper`
- Use `FileUtils` zip-slip-protected extraction; resolve paths against expected root
- Prefer Jackson with explicit type constraints; no `enableDefaultTyping()`
- Don't add dependencies without reviewing transitive impact
