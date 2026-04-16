---
name: fcli-command-dev
description: >-
  Implement new CLI commands for the fcli (Fortify CLI) Java project. Use this skill whenever the user asks to
  add, create, scaffold, or implement a new fcli command or subcommand — including proposing command/option names,
  generating Java source files, updating resource bundles, wiring container commands, and creating tests.
  Also use when the user wants to add new options to existing commands, refactor command structure, or needs
  guidance on fcli command conventions. Trigger on any mention of "fcli command", "new command", "add a command",
  "implement command", or references to fcli modules like SSC, FoD, SC-SAST, SC-DAST, tool, util, config, action.
---

# Fcli Command Development Skill

Guide for implementing new commands in the fcli (Fortify CLI) project. Fcli is a modular Java 17 CLI built with
Picocli and Gradle. Commands follow strict naming, structural, and testing conventions that vary by module.

## Before You Start

Read the project's existing Copilot instruction files — they contain detailed, authoritative guidance that
complements this skill:

- `.github/copilot-instructions.md` — Project overview, build workflow, code conventions
- `.github/instructions/java.instructions.md` — Architecture, command structure, exceptions, session management
- `.github/instructions/style.instructions.md` — Naming, formatting, imports, Java 17 usage
- `.github/instructions/utilities.instructions.md` — Common utility classes in `fcli-common`
- `.github/instructions/action-yaml.instructions.md` — Action YAML editing (if the command involves actions)

These files are the source of truth. This skill provides the *workflow* for combining that knowledge into a
working command implementation.

## Workflow Overview

When the user asks to implement a new fcli command:

1. **Clarify scope** — Determine which module (SSC, FoD, SC-SAST, SC-DAST, tool, util, config, action) and
   what entity/resource the command operates on
2. **Propose names** — Suggest command names and option names following conventions (see below)
3. **Identify base classes** — Pick the right abstract base class for the module and command type
4. **Implement the command** — Create the leaf command class, container command (if new entity group),
   and any helpers/mixins
5. **Update resource bundles** — Add entries to the module's `*Messages.properties`
6. **Wire into command tree** — Register in the container command's `@Command(subcommands=...)` list
7. **Propose tests** — Suggest unit test additions to `FortifyCLITest` awareness, and functional test
   specs if applicable
8. **Validate** — Run `get_errors` and `./gradlew build` to catch issues early

## Step 1: Determine the Module

Each module has its own patterns. Read `references/module-patterns.md` for the full breakdown.

| Module | Gradle path | Prefix | Has sessions? | Base command |
|--------|------------|--------|---------------|--------------|
| SSC | `fcli-core/fcli-ssc` | `SSC` | Yes | `AbstractSSCBaseRequestOutputCommand` or `AbstractSSCJsonNodeOutputCommand` |
| FoD | `fcli-core/fcli-fod` | `FoD` | Yes | `AbstractFoDBaseRequestOutputCommand` or `AbstractFoDJsonNodeOutputCommand` |
| SC-SAST | `fcli-core/fcli-sc-sast` | `SCSast` | Yes | `AbstractSCSast*OutputCommand` |
| SC-DAST | `fcli-core/fcli-sc-dast` | `SCDast` | Yes | `AbstractSCDast*OutputCommand` |
| tool | `fcli-core/fcli-tool` | `Tool` | No | `AbstractTool*Command` (Install, List, Get, etc.) |
| util | `fcli-core/fcli-util` | *(none)* | No | `AbstractOutputCommand` directly |
| config | `fcli-core/fcli-config` | *(none)* | No | `AbstractOutputCommand` directly |
| action | `fcli-core/fcli-action` | *(none)* | No | Varies |

## Step 2: Propose Command & Option Names

### Command Naming Rules (enforced by `FortifyCLITest`)

- **Kebab-case**, lowercase letters and digits only, separated by hyphens: `list`, `create`, `wait-for`,
  `purge-artifacts`, `download-state`
- Must not start or end with a hyphen
- At least 2 characters
- Maximum command depth: 4 levels (e.g., `fcli ssc appversion create`)
- Use standard verbs from `OutputHelperMixins`: `list`, `get`, `create`, `update`, `delete`, `upload`,
  `download`, `install`, `uninstall`, `enable`, `disable`, `start`, `cancel`, `wait-for`, etc.

### Option Naming Rules (enforced by `FortifyCLITest`)

- Long options: `--kebab-case` (at least 2 chars after `--`, only lowercase letters, digits, hyphens)
- Short options: `-x` (single lowercase letter or digit)
- At most 1 short name per option
- At least 1 long name per option
- Multi-value options: long name **must be plural** (e.g., `--versions`, `--attributes`) unless `@DisableTest(MULTI_OPT_PLURAL_NAME)` is used
- Multi-value options **must** have `split=","` (or another separator) defined
- `arity` may only be specified on boolean options (`0` or `1`) or interactive options (`0..1`)
- Every option and positional parameter must have a non-empty description (via resource bundle)

### Naming Conventions by Module

**Product modules (SSC, FoD, SC-SAST, SC-DAST):**
- Class: `<Prefix><Entity><Verb>Command` — e.g., `SSCAppVersionCreateCommand`, `FoDMastScanListCommand`
- Container: `<Prefix><Entity>Commands` — e.g., `SSCAppVersionCommands`
- Command name in CLI: the entity becomes a kebab-case subcommand under the product, e.g., `fcli ssc appversion create`

**Tool module:**
- Class: `Tool<ToolName><Verb>Command` — e.g., `ToolFcliInstallCommand`, `ToolSCClientListCommand`
- Each tool has a fixed set of standard commands (install, uninstall, list, get, list-platforms, run, register)
  provided by abstract base classes

**Util/Config modules:**
- Class: `<Entity><Verb>Command` — e.g., `VariableListCommand`, `CryptoEncryptCommand`
- No product prefix

## Step 3: Pick the Right Base Class

### Product modules (SSC example — same pattern for FoD, SC-SAST, SC-DAST)

Choose between two primary abstract classes:

- **`AbstractSSCBaseRequestOutputCommand`** — For commands that directly return an HTTP request (`getBaseRequest(UnirestInstance)`).
  Best for simple list/get commands that map to a single REST endpoint. The framework handles pagination,
  output formatting, and error handling.

- **`AbstractSSCJsonNodeOutputCommand`** — For commands that need to do complex processing and return a
  `JsonNode` result (`getJsonNode(UnirestInstance)`). Best for create/update/delete commands that need
  multiple API calls, bulk requests, or complex orchestration.

Both extend `AbstractSSCOutputCommand` which provides:
- `@Mixin SSCUnirestInstanceSupplierMixin` (automatic session management)
- `SSCProductHelper.INSTANCE` as the product helper

### Tool module

Use the provided abstract base classes — do NOT extend `AbstractOutputCommand` directly:
- `AbstractToolInstallCommand` — for install commands
- `AbstractToolUninstallCommand` — for uninstall commands
- `AbstractToolListCommand` — for list commands
- `AbstractToolGetCommand` — for get commands
- `AbstractToolListPlatformsCommand` — for list-platforms commands
- `AbstractToolRunCommand` / `AbstractToolRunShellOrJavaCommand` — for run commands
- `AbstractToolRegisterCommand` — for register commands

Tool commands override `getTool()` to return the appropriate `Tool` enum value.

### Util/Config modules

Extend `AbstractOutputCommand` directly. Implement `IJsonNodeSupplier` for commands that produce
JSON output. No session management is needed.

## Step 4: Implement the Command

### Leaf Command Template (Product module)

```java
package com.fortify.cli.ssc.<entity>.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SSC<Entity>ListCommand extends AbstractSSCBaseRequestOutputCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get("/api/v1/<endpoint>?limit=100");
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
```

### Container Command Template

```java
package com.fortify.cli.ssc.<entity>.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;

import picocli.CommandLine.Command;

@Command(
    name = "<entity-kebab>",
    subcommands = {
        SSC<Entity>CreateCommand.class,
        SSC<Entity>DeleteCommand.class,
        SSC<Entity>GetCommand.class,
        SSC<Entity>ListCommand.class,
    }
)
@DefaultVariablePropertyName("id")
public class SSC<Entity>Commands extends AbstractContainerCommand {
}
```

### Key Interfaces to Implement

- **`IRecordTransformer`** — Override `transformRecord(JsonNode)` to rename/reshape fields before output
- **`IActionCommandResultSupplier`** — Override `getActionCommandResult()` to return a status string
  (e.g., `"CREATED"`, `"DELETED"`) appended as `__action__` field
- **`IServerSideQueryParamGeneratorSupplier`** — For list commands with server-side query support (SSC)
- **`IBaseRequestSupplier`** — Implemented by `AbstractSSCBaseRequestOutputCommand` automatically

### OutputHelperMixin Selection

Use the appropriate mixin from `OutputHelperMixins` (or a product-specific variant):

| Action | Mixin | Output format | Query support |
|--------|-------|---------------|---------------|
| list | `OutputHelperMixins.List` | Table | Yes |
| get | `OutputHelperMixins.Get` | Details | No |
| create | `OutputHelperMixins.Create` | Table | No |
| delete | `OutputHelperMixins.Delete` | Table | No |
| update | `OutputHelperMixins.Update` | Table | No |
| download | `OutputHelperMixins.Download` | Table | No |
| upload | `OutputHelperMixins.Upload` | Table | No |
| install | `OutputHelperMixins.Install` | Table | No |
| enable/disable | `OutputHelperMixins.Enable` / `.Disable` | Table | No |
| start/cancel | `OutputHelperMixins.Start` / `.Cancel` | Table | No |

The mixin's `CMD_NAME` constant defines the command name; use it in `@Command(name = ...)`.

## Step 5: Update Resource Bundles

Each module has a single `*Messages.properties` file:

| Module | Resource bundle path |
|--------|---------------------|
| SSC | `fcli-core/fcli-ssc/src/main/resources/com/fortify/cli/ssc/i18n/SSCMessages.properties` |
| FoD | `fcli-core/fcli-fod/src/main/resources/com/fortify/cli/fod/i18n/FoDMessages.properties` |
| SC-SAST | `fcli-core/fcli-sc-sast/src/main/resources/com/fortify/cli/sc_sast/i18n/SCSastMessages.properties` |
| SC-DAST | `fcli-core/fcli-sc-dast/src/main/resources/com/fortify/cli/sc_dast/i18n/SCDastMessages.properties` |
| Tool | `fcli-core/fcli-tool/src/main/resources/com/fortify/cli/tool/i18n/ToolMessages.properties` |

### Required Entries

For every new command, add these entries (keys use the command's qualified name with dots):

```properties
# Usage header (REQUIRED — FortifyCLITest checks this)
fcli.<product>.<entity>.<verb>.usage.header = Short description of what the command does.

# Usage description (optional, for detailed help text)
fcli.<product>.<entity>.<verb>.usage.description = Longer description with details...

# Default table output columns (REQUIRED for commands with outputHelper mixin)
fcli.<product>.<entity>.output.table.args = id,name,status,createdDate

# Custom table column headers (optional — override auto-generated headers)
fcli.<product>.<entity>.output.table.header.createdDate = Created

# Option descriptions (REQUIRED for every option without descriptionKey)
# Default key: <qualified-command-name>.<option-name-without-dashes>
fcli.<product>.<entity>.<verb>.<option-name> = Description of the option.
```

**Key patterns:**
- `output.table.args` is usually defined at the entity level (shared across list/get/create/delete), not per-verb
- Verb-specific `output.table.args` override the entity-level default when needed
- The key format mirrors the command's qualified name: `fcli ssc appversion create` → `fcli.ssc.appversion.create`
- Option description keys default to Picocli's lookup: `fcli.<product>.<entity>.<verb>.<option-no-dashes>`
- Only use `descriptionKey` annotation when sharing descriptions across commands (e.g., shared tool options)

### Shared Option Descriptions

For options shared via mixins, use `descriptionKey` to point at a shared key:

```java
@Option(names={"--version"}, descriptionKey="fcli.tool.install.version")
```

### Cleanup

When removing or renaming commands:
- Remove corresponding `usage.header`, `usage.description`, `output.table.args`, option description entries
- Remove any orphaned `output.table.header.*` entries
- Search for cross-references in descriptions (e.g., `See 'fcli ssc artifact download'`)

## Step 6: Wire Into Command Tree

Register the new command in the container command's `@Command(subcommands=...)` array.
If this is a new entity group:

1. Create the container command class (see template above)
2. Add it to the product's root command (e.g., `SSCCommands.java`) in the `subcommands` list
3. If the container needs a `usage.header`, add it to `*Messages.properties`
4. Consider adding `@DefaultVariablePropertyName("id")` if the entity has an ID field

Product root commands (`SSCCommands`, `FoDCommands`, etc.) maintain a specific ordering convention:
- Session commands first
- Other commands alphabetically
- REST commands last

## Step 7: Propose Tests

### FortifyCLITest (automatic validation)

`FortifyCLITest` in `fcli-core/fcli-app/src/test/java/com/fortify/cli/FortifyCLITest.java` automatically
validates all commands. No manual test code is needed for basic structure validation. It checks:

- Command names are kebab-case
- Usage headers are defined
- Options follow naming rules
- Default table columns (`output.table.args`) are defined
- Multi-value options have `split` and plural names
- Arity rules are followed
- No `@Spec(MIXEE)` in mixins

If a command intentionally violates a rule, use `@DisableTest(TestType.XXX)` and document why.
Available test types: `CMD_STD_OPTS`, `CMD_NAME`, `CMD_USAGE_HEADER`, `CMD_DEFAULT_TABLE_OPTIONS_PRESENT`,
`CMD_DEPTH`, `OPT_NAME_FORMAT`, `OPT_SHORT_NAME_COUNT`, `OPT_SHORT_NAME`, `OPT_LONG_NAME_COUNT`,
`OPT_LONG_NAME`, `MULTI_OPT_PLURAL_NAME`, `MULTI_OPT_SPLIT`, `OPT_ARITY_VARIABLE`, `OPT_ARITY_BOOL`,
`OPT_ARITY_INTERACTIVE`, `OPT_ARITY_PRESENT`, `OPT_EMPTY_DESCRIPTION`, `PARAM_EMPTY_DESCRIPTION`,
`INJECT_MIXEE`.

### Functional Tests

Functional tests live in `fcli-other/fcli-functional-test/src/ftest/groovy/com/fortify/cli/ftest/`.
They use Spock (Groovy) and are organized by product:

- `ftest/ssc/` — SSC-specific tests
- `ftest/fod/` — FoD-specific tests
- `ftest/core/` — Cross-cutting tests

Each spec typically:
1. Uses `@FcliSession(SSC)` (or FoD, etc.) for session setup
2. Uses `@Prefix("ssc.<entity>")` for test naming
3. Runs commands via `Fcli.run("ssc <entity> <verb> ...", { it.expectSuccess() })`
4. Uses `@Stepwise` for ordered test execution
5. Includes cleanup in `cleanupSpec()`

Suggest a functional test spec when the command interacts with a live product API and can be safely
tested in a CI environment.

### Build Validation

After implementing, always:
1. Run `get_errors` to catch compile errors
2. Run `./gradlew build` from the project root to validate all tests pass
3. `FortifyCLITest` will automatically validate the new command structure

## Package Structure Convention

Commands follow this package structure:

```
com.fortify.cli.<module>.<entity_snake_case>.cli.cmd/     — Command classes
com.fortify.cli.<module>.<entity_snake_case>.cli.mixin/    — Mixin classes (shared options, resolvers)
com.fortify.cli.<module>.<entity_snake_case>.helper/       — Helper classes (descriptors, API wrappers)
com.fortify.cli.<module>._common.*/                        — Module-wide shared code
com.fortify.cli.<module>._main.cli.cmd/                    — Root module command
```

Entity names in packages use `snake_case` (e.g., `app_version` for the `appversion` CLI command, though
older packages sometimes use `appversion` without underscore — follow the existing pattern in each module).

## HTTP Header Best Practice

Always use `headerReplace(name, value)` instead of convenience methods like `accept()`, `contentType()`:

```java
// Wrong — may add duplicate headers
request.accept("application/octet-stream")

// Correct
request.headerReplace(HttpHeader.ACCEPT, "application/octet-stream")
```

Use constants from `com.fortify.cli.common.rest.unirest.HttpHeader`.

## Exception Handling

Use the fcli exception hierarchy:
- `FcliSimpleException` — User-facing errors (invalid input, missing resource)
- `FcliTechnicalException` — Unexpected technical failures (wrap cause)
- `FcliBugException` — Invariant violations, unreachable code

See `.github/instructions/java.instructions.md` for the full decision matrix and message style guide.
