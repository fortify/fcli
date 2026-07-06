---
name: fcli-action-yaml-reference
description: "Detailed reference for fcli action YAML development. Use when creating or editing action YAML files and need SpEL scope rules, TemplateExpression properties, error handling patterns, or SpEL function details. NOT needed for quick edits where the always-on action-yaml.instructions.md suffices."
---

# Action YAML Reference

Detailed reference material for developing fcli action YAML files. The always-on `action-yaml.instructions.md` has a concise summary; this skill provides the full details.

## SpEL Scope Rules

### `cli.options::<option>::default`

Default values for CLI options — evaluated **before** action steps run.

- Only `ActionSpelFunctions` + `#env()` + `#<ci>.env` (e.g. `#ado.env`, `#github.env`) available
- Evaluated via `ActionRunnerConfig.getSpelEvaluator()`
- **NOT available:** `ActionRunnerContextSpelFunctions` (`action.*`), product-specific functions (`fod.*`, `ssc.*`), action variables, execution context

### `steps` Section

All step instructions and their properties — evaluated **during** action execution.

- Full access: `ActionSpelFunctions` + `ActionRunnerContextSpelFunctions` + product-specific + CI-specific
- Evaluated via `ActionRunnerContext.getSpelEvaluator()`
- Action variables: `${cli.option}`, `${varName}`
- Product-specific: `fod.*` (FoD module only), `ssc.*` (SSC module only)
- CI-specific (auto-detected): `_ci.*`, `github.*`, `gitlab.*`, `ado.*`, `bitbucket.*`

## Properties Accepting TemplateExpression

### Step-level
`if`, `var.set`, `var.rm`, `log.*`, `out.write`

### Records iteration
`records.for-each::from`, `records.for-each::breakIf`

### REST calls
`rest.target::baseUrl`, `rest.target::headers`, `rest.call::uri`, `rest.call::query`, `rest.call::body`, pagination expressions

### Writers
`out.write::to`, `out.write::type`, `out.write::type-args`, `out.write::style`

### Other
`with.session::login`, `with.session::logout`, `run.fcli::cmd`, `run.fcli::skip.if-reason`

See action model classes in `com.fortify.cli.common.action.model` for the complete list.

## Error Handling: `on.fail` and `on.success`

All action step elements support `on.fail` and `on.success` handlers.

### Default behavior (no `on.fail`)
Failed operations throw exceptions and terminate the action (fail-fast).

### Exception variables in `on.fail`
- `lastException.type` — Exception class simple name
- `lastException.message` — Exception message text
- `lastException.httpStatus` — HTTP status (only for `UnexpectedHttpResponseException`)
- `lastException.pojo` — Original exception as POJONode
- `${name}_exception` — For named elements (e.g., `rest.call` entries)

### Pattern: Graceful fallback
```yaml
- var.set:
    result: ${#someOperation()}
  on.fail:
    - log.warn:
        msg: 'Operation failed, using fallback'
        cause: ${lastException}
    - var.set:
        result: ${fallbackValue}
```

### Pattern: Conditional error handling
```yaml
- var.set:
    result: ${#someOperation()}
  on.fail:
    - if: ${lastException.type=='ExpectedException'}
      log.info: Expected error occurred
    - if: ${lastException.type!='ExpectedException'}
      throw:
        msg: 'Unexpected error'
        cause: ${lastException}
```

### Pattern: Named element exception
```yaml
- rest.call:
    myData:
      uri: /api/v3/resource/${id}
      on.fail:
        - log.warn:
            msg: 'Unable to load resource'
            cause: ${myData_exception}
```

### `throw` instruction
- Simple: `throw: "Error message"` — throws `FcliActionStepException`
- With cause: `throw: { msg: "...", cause: ${exception} }` — chains original exception
- Rethrow: `throw: { cause: ${exception} }` — preserves original type for FcliExceptions

### `log.*` with exceptions
- Simple: `log.warn: "Message"`
- With cause: `log.warn: { msg: "...", cause: ${exception} }` — includes stack trace in log
- Levels: `log.info`, `log.warn`, `log.debug` support structured format; `log.progress` does not

## SpEL Functions Discovery

For a complete, up-to-date function list:
- **Generated docs:** `./gradlew generateAsciiDocActionDevelopment` → `fcli-other/fcli-doc/build/asciidoc/action-development.adoc`
- **Runtime:** `#actionSpelFunctions()` returns all functions with metadata
- **Runtime:** `#actionSchema()` returns complete action schema as JSON

### Function source classes
- `ActionSpelFunctions` — Common (all actions)
- `ActionRunnerContextSpelFunctions` — Execution context (`action.*` prefix)
- `FoDActionSpelFunctions` — FoD-specific (`fod.*`)
- `SSCActionSpelFunctions` — SSC-specific (`ssc.*`)
- CI: `ActionGitHubSpelFunctions`, `ActionGitLabSpelFunctions`, `ActionAdoSpelFunctions`, `ActionBitbucketSpelFunctions`

### Commonly used functions
- `#opt(name, value)` — `"name=value"` if value is not blank, else empty string
- `#ifBlank(value, default)` — returns default if value is blank
- `#extraOpts(prefix)` — retrieves `<prefix>_EXTRA_OPTS` env var
- `#env(name)` — environment variable
- `#action.runID()` — unique identifier for current fcli invocation
- `#action.copyParametersFromGroup(group)` — copies CLI options from group

## Common Action Patterns

### CLI options
```yaml
cli.options:
  myOption:
    names: --my-option, -m
    description: "Description"
    required: false
    type: string
    default: "${#env('MY_DEFAULT_VAR')}"
```

### REST with pagination
```yaml
- rest.call:
    issues:
      uri: /api/v3/releases/${rel.releaseId}/vulnerabilities
      query:
        limit: 50
      log.progress:
        page.post-process: Processed ${totalIssueCount?:0} of ${issues_raw.totalCount}
      records.for-each:
        record.var-name: issue
        do:
          - var.set:
              results..: {fmt: results}
```

### Variable accumulation
```yaml
# Append to list with `..` suffix
- var.set:
    myList..: ${newItem}

# Scalar variable
- var.set:
    myVar: ${someExpression}
```

## Finding Examples

- SSC actions: `fcli-core/fcli-ssc/src/main/resources/com/fortify/cli/ssc/actions/zip/`
- FoD actions: `fcli-core/fcli-fod/src/main/resources/com/fortify/cli/fod/actions/zip/`
- Generic: `fcli-core/fcli-action/src/main/resources/com/fortify/cli/generic_action/actions/zip/`
