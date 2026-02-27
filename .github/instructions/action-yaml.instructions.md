---
name: 'Action YAML Guide'
description: 'Editing guide for fcli action YAML files with SpEL expressions'
applyTo: 'fcli/**/src/main/resources/**/actions/**/*.yaml'
---

# Action YAML Editing Guide

This guide provides detailed instructions for editing fcli action YAML files.

## Maintaining These Instructions

**If you detect discrepancies** between these instructions and the actual implementation (e.g., in action model classes, generated documentation, or existing YAML files), or discover patterns/features not documented here:

1. **Notify the user** about the discrepancy or missing documentation
2. **Suggest specific updates** to this instruction file
3. **Verify against current code** (action model classes in `com.fortify.cli.common.action.model`) and generated docs (`fcli-other/fcli-doc/build/generated-docs/asciidoc/action-development.adoc`) before proceeding
4. **Check existing action YAML files** for actual usage patterns

## Schema Discovery and Validation

**Schema declaration** (required at top of every action YAML file):
```yaml
# yaml-language-server: $schema=https://fortify.github.io/fcli/schemas/action/fcli-action-schema-dev-2.x.json
```

**Comprehensive reference documentation:**

Fcli generates complete action development documentation that includes ALL step instructions, SpEL functions, property types, and examples:

```bash
# Generate comprehensive action reference (if not already built)
./gradlew generateAsciiDocActionDevelopment

# View generated documentation
cat fcli-other/fcli-doc/build/asciidoc/action-development.adoc
```

This generated documentation is **always up-to-date** with the code and includes:
- All primary YAML instructions (author, usage, config, cli.options, defaults, steps, formatters)
- All step instructions (var.set, rest.call, run.fcli, records.for-each, with, etc.)
- All YAML types with property tables
- Complete SpEL function reference with signatures, parameters, descriptions, and return types
- Organized by category (fcli, workflow, util, txt, date, fortify)

**Programmatic access to schema information:**

Fcli provides SpEL functions and Java APIs for runtime access to schema and function metadata:

- `#actionSchema()` — Returns complete action schema as JSON (available in action YAML)
- `#actionSpelFunctions()` — Returns all available SpEL functions with metadata (available in action YAML)
- `ActionSchemaDescriptorFactory.getActionSchemaDescriptor()` — Java API for schema structure
- `SpelFunctionDescriptorsFactory.getActionSpelFunctionsDescriptors()` — Java API for SpEL function descriptors
- Action model classes in `com.fortify.cli.common.action.model` package define YAML structure with JavaDoc
- VS Code YAML extension automatically validates against declared schema URL

**Finding examples:**
- Use `semantic_search` to find similar action examples: "github report action", "rest call pagination", "issue processing loop"
- Use `list_code_usages` on action model classes to understand property structures

## Where SpEL Expressions Can Be Used

### In `cli.options::<option>::default`
Default values for CLI options (evaluated before action steps run).

**Available functions:**
- Standard SpEL functions + `ActionSpelFunctions` only
- Evaluated using `ActionRunnerConfig.getSpelEvaluator()` before action context is created
- `ActionRunnerContextSpelFunctions` (prefixed with `action.`) is NOT available here
- Product-specific functions (e.g., `fod.*`, `ssc.*`) are NOT available here
- Can access environment variables via `#env()`, but not action variables or execution context

### In `steps` Section
All step instructions and their properties (evaluated during action execution).

**Available functions:**
- Standard SpEL functions + `ActionSpelFunctions` + `ActionRunnerContextSpelFunctions` + product-specific + CI-specific
- Evaluated using `ActionRunnerContext.getSpelEvaluator()` during action execution
- Full access to action variables (`${cli.option}`, `${varName}`), execution context (`${action.*}`), and CI metadata
- Product-specific functions: `fod.*` (FoD module only), `ssc.*` (SSC module only)
- CI-specific functions (automatically detected based on environment):
  - `_ci.*` — Auto-detects current CI system and provides access to CI-specific functions
  - `github.*` — GitHub Actions functions (repository info, PR details, etc.)
  - `gitlab.*` — GitLab CI functions
  - `ado.*` — Azure DevOps functions
  - `bitbucket.*` — Bitbucket Pipelines functions

**Note:** For a complete, up-to-date list of all available SpEL functions with signatures and descriptions, see the generated documentation at `fcli-other/fcli-doc/build/asciidoc/action-development.adoc` or invoke `#actionSpelFunctions()` from within an action.

### Properties Accepting `TemplateExpression`

**Step-level:**
- `if`, `var.set`, `var.rm`, `log.*`, `out.write`
- `records.for-each::from`, `records.for-each::breakIf`

**REST calls:**
- `rest.target::baseUrl`, `rest.target::headers`
- `rest.call::uri`, `rest.call::query`, `rest.call::body`
- Pagination expressions

**Writers:**
- `out.write::to`, `out.write::type`, `out.write::type-args`, `out.write::style`

**Other step types:**
- `with.session::login`, `with.session::logout`
- `run.fcli::cmd`, `run.fcli::skip.if-reason`

See action model classes in `com.fortify.cli.common.action.model` for complete list.

## SpEL Expression Syntax in YAML

### Common Pitfalls

SpEL expressions may contain characters with special YAML meaning, causing parse errors:
- `#` starts SpEL function calls (e.g., `#opt()`) but means comment in YAML
- `:` in ternary operator (e.g., `a ? b : c`) but means key-value separator in YAML

### Workaround Strategies

1. **Remove spaces around special characters:** `a?b:#c()` often works, `a ? b : #c()` won't
2. **Quote the entire expression:** `"${a ? b : #c()}"` ensures YAML treats it as a string
3. **Use SpEL function alternatives:** `#ifBlank(value, default)` instead of ternary operator

### Best Practices

- Prefer quoted expressions when in doubt
- Use `#opt()` and `#ifBlank()` SpEL functions to conditionally include values without ternary operators

### Conditional Logic: SpEL vs `if:` Steps

**Use conditional SpEL expressions in `var.set` when:**
- Setting a single variable value conditionally
- The condition is simple and readable inline
- You want to keep the YAML compact

```yaml
# Good: Inline conditional for simple value assignment
- var.set:
    reportFile: "${cli.file!=null ? cli.file : cli.publish==true ? null : 'gh-fortify-sast.sarif'}"
    run.fod_ci: ${#isNotBlank(#env('FOD_URL'))}
```

**Use `if:` step-level conditions when:**
- Conditionally executing multiple steps or complex logic
- The condition guards an entire operation (e.g., publishing, writing output)
- Readability is improved by separating condition from action

```yaml
# Good: Separate if statement for guarding multiple operations
- if: ${reportFile!=null}
  do:
    - out.write:
        ${reportFile}: ${reportContents}
    - if: ${!{'stdout','stderr'}.contains(reportFile)}
      log.info: Output written to ${reportFile}

# Good: Guarding a significant operation
- if: ${cli.publish==true}
  do:
    - var.set:
        githubUpload: ${#github.uploadSarif(reportContents)}
    - log.info: Report published to GitHub
```

### Critical: Step Nesting with `do:` (formerly `steps:`)

**The `do:` property is the current standard for nesting multiple steps**. The `steps:` property name is deprecated but still supported for backward compatibility.

**Pattern 1: Nesting steps conditionally or for grouping** — Use `do:` (or `steps:` for backward compatibility):
```yaml
# Current standard: do:
- if: ${!#isEmpty(docListEntries)}
  do:
    - var.set:
        output: ${#join("\n", docListEntries)}
    - log.info: Generated ${docListEntries.size()} entries

# Legacy but still supported: steps:
- if: ${condition}
  steps:
    - var.set: ...
```

**Pattern 2: `if` + single action instruction** — Nest the action instruction directly (no `do:` needed):
```yaml
- if: ${!#isBlank(product)}
  var.set:
    productName: ${productNames[product]}
```

**Pattern 3: `records.for-each`** — Always uses `do:` property:
```yaml
- records.for-each:
    from: ${items}
    record.var-name: item
    do:
      - var.set:
          processed..: ${item.name}
```

**Best practice:** Use `do:` for new action files and when updating existing ones. The `steps:` property will continue to work but is considered deprecated.

### Error Handling with `on.fail` and `on.success`

**All action step elements** (including individual steps, `rest.call` entries, `run.fcli` entries, and `out.write` entries) support `on.fail` and `on.success` handlers for error handling and post-success processing.

**Default behavior (without `on.fail`):**
- Failed operations throw exceptions and terminate the entire action (fail-fast)
- This is usually desired for critical operations

**With `on.fail` handler:**
- Exceptions are caught and `on.fail` steps are executed
- Action continues unless `on.fail` steps throw an exception
- Useful for graceful degradation, fallback logic, or custom error messages

**Available exception variables in `on.fail` blocks:**
- `lastException` — ObjectNode with exception details:
  - `lastException.type` — Exception class simple name (e.g., `GhasUnavailableException`)
  - `lastException.message` — Exception message text
  - `lastException.httpStatus` — HTTP status code (only present for `UnexpectedHttpResponseException`)
  - `lastException.pojo` — Original exception as POJONode (for calling methods like `.getMessage()`)
- `${name}_exception` — For named elements (e.g., `rest.call` or `run.fcli` entries), same structure as `lastException`

**Common patterns:**

**Pattern 1: Graceful fallback**
```yaml
- var.set:
    sarifUploadResponse: ${#github.repo().uploadSarif(reportContents)}
    sarifUploaded: true
  on.fail:
    - log.warn:
        msg: 'SARIF upload failed, will try Check Run fallback'
        cause: ${lastException}
    - var.set:
        sarifUploaded: false
```

**Pattern 2: Conditional error handling based on exception type**
```yaml
- var.set:
    result: ${#someOperation()}
  on.fail:
    - if: ${lastException.type=='GhasUnavailableException'}
      log.info: GitHub Advanced Security not available
    - if: ${lastException.type!='GhasUnavailableException'}
      throw:
        msg: 'Unexpected error'
        cause: ${lastException}
```

**Pattern 3: Named element exception handling**
```yaml
- rest.call:
    staticScanSummary:
      uri: /api/v3/scans/${scanId}/summary
      on.fail:
        - log.warn:
            msg: 'Unable to load scan summary'
            cause: ${staticScanSummary_exception}
```

**Pattern 4: Success handler for validation or logging**
```yaml
- rest.call:
    issues:
      uri: /api/v3/releases/${releaseId}/vulnerabilities
      on.success:
        - if: ${issues_raw.totalCount>5000}
          throw: Too many vulnerabilities to process (${issues_raw.totalCount})
```

**Pattern 5: Rethrowing with cause (exception chaining)**
```yaml
- var.set:
    result: ${#someOperation()}
  on.fail:
    # Simple throw with message
    - if: ${lastException.type=='NetworkException'}
      throw: 'Network operation failed: ${lastException.message}'
    
    # Structured throw with message and cause
    - if: ${lastException.type!='NetworkException'}
      throw:
        msg: 'Unexpected error during operation'
        cause: ${lastException}
    
    # Rethrow exception (preserves original exception type if FcliException)
    - throw:
        cause: ${lastException}
```

**Pattern 6: Logging with exception cause**
```yaml
- var.set:
    result: ${#someOperation()}
  on.fail:
    # Simple log message
    - log.warn: 'Operation failed: ${lastException.message}'
    
    # Log with exception cause (includes stack trace in log files)
    - log.warn:
        msg: 'Operation failed with unexpected error'
        cause: ${lastException}
```

**`throw` instruction capabilities:**
- **Simple string:** `throw: "Error message"` — Throws `FcliActionStepException` with message
- **Structured with msg and cause:** `throw: { msg: "...", cause: ${exception} }` — Attaches original exception as cause
- **Rethrow only:** `throw: { cause: ${exception} }` — Rethrows FcliException as-is, or wraps other exceptions
- **Cause extraction:** Automatically unwraps Throwables from `POJONode` or `ObjectNode.pojo` properties

**`log.*` instruction capabilities:**
- **Simple string:** `log.info: "Message"` — Logs simple message
- **Structured with msg and cause:** `log.info: { msg: "...", cause: ${exception} }` — Logs message with exception details
- **Exception support:** `log.info`, `log.warn`, `log.debug` support both simple and structured formats with exception cause
- **Progress logging:** `log.progress` only supports simple string format (no exception cause support)
- **Exception cause handling:** When cause is provided, exception details (including stack trace) are included in log output

**When to use `on.fail`:**
- Implementing fallback strategies (try SARIF, fall back to Check Run)
- Converting exceptions into warnings for non-critical operations
- Adding context-specific error messages
- Recovering from expected failure scenarios

**When NOT to use `on.fail`:**
- For operations that MUST succeed — let exceptions propagate to fail the action
- When you just want to log the error but still fail — use fail-fast instead and check logs

**Quick validation (no rebuild required if no related Java changes):**
```bash
# Validates and displays action help
./fcli <product> action help <path-to-yaml>

# Validates and displays raw YAML
./fcli <product> action get <path-to-yaml>

# Example
./fcli ssc action help fcli-core/fcli-ssc/src/main/resources/com/fortify/cli/ssc/actions/zip/package.yaml
```

These commands parse the full YAML structure and SpEL expressions without needing a rebuild.

- Use `get_errors` tool after editing for IDE-level validation
- Run module tests after editing: `./gradlew :fcli-core:fcli-<product>:test` (e.g., `fcli-ssc`, `fcli-fod`)

## Available SpEL Functions

**For a complete, up-to-date list of all SpEL functions, see:**
- **Generated documentation:** `fcli-other/fcli-doc/build/asciidoc/action-development.adoc` (regenerate with `./gradlew generateAsciiDocActionDevelopment`)
- **Programmatically within actions:** Use `#actionSpelFunctions()` to get JSON array of all available functions
- **Source code:** Functions are defined in classes annotated with `@SpelFunction`:
  - `ActionSpelFunctions` — Common functions for all actions
  - `ActionRunnerContextSpelFunctions` — Action execution context functions (prefixed with `action.*`)
  - `FoDActionSpelFunctions` — FoD-specific functions (prefixed with `fod.*`)
  - `SSCActionSpelFunctions` — SSC-specific functions (prefixed with `ssc.*`)
  - CI-specific: `ActionGitHubSpelFunctions`, `ActionGitLabSpelFunctions`, `ActionAdoSpelFunctions`, `ActionBitbucketSpelFunctions`

**Commonly used functions (examples only, not exhaustive):**

- `#opt(name, value)` — returns `"name=value"` if value is not blank, empty string otherwise
- `#ifBlank(value, default)` — returns default if value is blank
- `#extraOpts(prefix)` — retrieves `<prefix>_EXTRA_OPTS` environment variable
- `#env(name)` — retrieves environment variable
- `#actionSchema()` — returns complete action schema as JSON
- `#actionSpelFunctions()` — returns array of all available SpEL functions with metadata
- `#action.runID()` — returns unique identifier for current fcli invocation
- `#action.copyParametersFromGroup(group)` — copies CLI options from specified group

## Common Action Patterns

### Adding CLI Options

```yaml
cli.options:
  myOption:
    names: --my-option, -m
    description: "Description of the option"
    required: false
    type: string  # boolean, integer, string
    default: "${#env('MY_DEFAULT_VAR')}"  # Optional, using SpEL
```

### REST API Calls

**Simple call:**
```yaml
steps:
  - rest.call:
      myData:
        uri: /api/v3/resource/${rel.id}
        query:
          filter: status:active
```

**With pagination and record processing:**
```yaml
steps:
  - rest.call:
      issues:
        uri: /api/v3/releases/${rel.releaseId}/vulnerabilities
        query:
          limit: 50
          filters: scantype:Static
        log.progress:
          page.post-process: Processed ${totalIssueCount?:0} of ${issues_raw.totalCount} issues
        records.for-each:
          record.var-name: issue
          embed:
            details:
              uri: /api/v3/releases/${rel.releaseId}/vulnerabilities/${issue.vulnId}/details
          do:
            - var.set:
                results..: {fmt: results}
```

### Conditional Logic

```yaml
steps:
  - if: ${cli.publish==true}
    do:
      - log.info: Publishing report...
      - rest.call:
          # ... publish steps
```

### Variable Setting

**Scalar variables:**
```yaml
- var.set:
    myVar: ${someExpression}
    anotherVar: "literal value"
```

**Append to arrays:**
```yaml
- var.set:
    results..: {fmt: results}  # Appends to results array
```

### Output Writing

```yaml
- out.write:
    ${cli.file!=null ? cli.file : 'output.json'}: ${reportContents}
```

## Common Mistakes to Avoid

1. **Don't quote property names** in YAML (e.g., use `on.success:` not `"on.success:"`)
2. **Don't forget to quote SpEL expressions** with special YAML characters (`:`, `#`)
3. **Don't use `action.*` context functions** in `cli.options::default` values
4. **Don't forget schema declaration** at top of file

## Example Action Files

Reference these for patterns:

**Report generation:**
- GitHub SARIF report: `fcli-core/fcli-fod/src/main/resources/com/fortify/cli/fod/actions/zip/github-sast-report.yaml`
- Policy check: `fcli-core/fcli-fod/src/main/resources/com/fortify/cli/fod/actions/zip/check-policy.yaml`

**CI/CD integration:**
- Generic CI action: `fcli-core/fcli-action/src/main/resources/com/fortify/cli/generic_action/actions/zip/ci.yaml`
- FoD CI action: `fcli-core/fcli-fod/src/main/resources/com/fortify/cli/fod/actions/zip/ci.yaml`
- SSC CI action: `fcli-core/fcli-ssc/src/main/resources/com/fortify/cli/ssc/actions/zip/ci.yaml`

**Documentation generation:**
- CI documentation action: `fcli-core/fcli-app/src/main/resources/com/fortify/cli/app/actions/build-time/ci-doc.yaml`
- Action development doc: `fcli-other/fcli-doc/src/actions/generate-action-dev-doc.yaml`

**All built-in actions:**
- SSC actions: `fcli-core/fcli-ssc/src/main/resources/com/fortify/cli/ssc/actions/zip/`
- FoD actions: `fcli-core/fcli-fod/src/main/resources/com/fortify/cli/fod/actions/zip/`
- Generic actions: `fcli-core/fcli-action/src/main/resources/com/fortify/cli/generic_action/actions/zip/`

## Validation Workflow

After editing any action YAML:

1. **Immediate validation:** Use `get_errors` tool
2. **Fast syntax check:** `./fcli <product> action help <path-to-yaml>` (if no Java changes)
3. **Full validation:** `./gradlew build` (if Java model classes changed)
4. **Module tests:** `./gradlew :fcli-core:fcli-<product>:test`
