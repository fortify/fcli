---
name: 'Action YAML Guide'
description: 'Editing guide for fcli action YAML files with SpEL expressions'
applyTo: 'fcli/**/src/main/resources/**/actions/**/*.yaml'
---

# Action YAML Editing Guide

## Schema & Reference Docs

Schema declaration (required at top of every action YAML):
```yaml
# yaml-language-server: $schema=https://fortify.github.io/fcli/schemas/action/fcli-action-schema-dev-2.x.json
```

Generate comprehensive reference: `./gradlew generateAsciiDocActionDevelopment` → `fcli-other/fcli-doc/build/asciidoc/action-development.adoc`

Programmatic access: `#actionSchema()`, `#actionSpelFunctions()` (available inside actions)

## SpEL Expression Scope

- **`cli.options::default`** — Only `ActionSpelFunctions` + `#env()` + `#<ci>.env` (e.g. `#ado.env`, `#github.env`); NO action context, NO product-specific functions
- **`steps` section** — Full access: all SpEL functions, action variables, product-specific (`fod.*`, `ssc.*`), CI-specific (`github.*`, `gitlab.*`, `ado.*`)

## YAML/SpEL Pitfalls

- `#` means comment in YAML — quote expressions: `"${#opt('x', val)}"`
- `:` in ternary — remove spaces or quote: `"${a ? b : c}"`
- Prefer `#ifBlank(value, default)` over ternary operators

## Step Nesting

- `do:` is the standard (replaces deprecated `steps:`); use for conditional/grouped steps
- Single action after `if:` can be inlined (no `do:` needed)
- `records.for-each` always uses `do:`

## Error Handling (`on.fail` / `on.success`)

All step elements support `on.fail` and `on.success` handlers. Without `on.fail`, failures terminate the action.

In `on.fail` blocks: `lastException.type`, `lastException.message`, `lastException.httpStatus` (REST only). Named elements get `${name}_exception`.

`throw` accepts: simple string, `{msg: "...", cause: ${exception}}`, or `{cause: ${exception}}` (rethrow).
`log.*` accepts: simple string or `{msg: "...", cause: ${exception}}`.

## Validation

1. `get_errors` tool for IDE validation
2. `./fcli <product> action help <path-to-yaml>` — fast syntax check (no rebuild)
3. `./gradlew build` — full validation
4. `./gradlew :fcli-core:fcli-<product>:test` — module tests

## Finding Examples

Search existing actions for patterns:
- SSC: `fcli-core/fcli-ssc/src/main/resources/com/fortify/cli/ssc/actions/zip/`
- FoD: `fcli-core/fcli-fod/src/main/resources/com/fortify/cli/fod/actions/zip/`
- Generic: `fcli-core/fcli-action/src/main/resources/com/fortify/cli/generic_action/actions/zip/`
- CI: look at `ci.yaml` in each product's actions directory
