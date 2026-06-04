---
name: fcli-command-patterns
description: "Detailed reference for fcli Java command implementation patterns. Use when creating new picocli commands, adding mixins, or need to understand the command class hierarchy, OutputHelperMixins catalog, FortifyCLITest validations, or Messages.properties conventions. NOT needed for simple edits to existing commands."
---

# Command Patterns Reference

Detailed reference for implementing fcli picocli commands. The always-on `java.instructions.md` has a concise summary; this skill provides the full details.

## Command Class Hierarchy

### Core base classes (`fcli-common-core`)
- `AbstractContainerCommand` — Command groups; zero business logic, only registers subcommands
- `AbstractRunnableCommand` — Leaf commands implementing `Callable<Integer>`; return 0 for success
- `AbstractOutputCommand` — Base for commands producing output; implements `IOutputConfigSupplier`
- `AbstractGenerateConfigCommand` — Config file generators
- `AbstractRestCallCommand` — Direct REST API call commands
- `AbstractWaitForCommand` — Polling/wait commands
- `AbstractSessionLoginCommand` / `AbstractSessionLogoutCommand` — Session lifecycle
- `AbstractSessionListCommand` — List active sessions

### Product-specific base classes
Each product module has its own output command hierarchy:
- `AbstractSSCOutputCommand` / `AbstractSSCJsonNodeOutputCommand` / `AbstractSSCBaseRequestOutputCommand`
- `AbstractFoDOutputCommand` / `AbstractFoDJsonNodeOutputCommand` / `AbstractFoDBaseRequestOutputCommand`
- `AbstractSCSastOutputCommand` / `AbstractSCSastJsonNodeOutputCommand` / `AbstractSCSastBaseRequestOutputCommand`

**Pattern:** Most leaf commands extend `Abstract<Product>OutputCommand` (or the JsonNode/BaseRequest variants for legacy code).

## OutputHelperMixins Catalog

These inner classes of `OutputHelperMixins` define the command name (via `@Command(name=...)`) and default output format. Use `@Mixin OutputHelperMixins.<Type>` in your command class.

### Base types
| Type | Output format | Query support |
|------|--------------|---------------|
| `TableWithQuery` | Table | Yes |
| `TableNoQuery` | Table | No |
| `DetailsNoQuery` | Detail view | No |
| `DetailsWithQuery` | Detail view | Yes |

### Named types (extend base types)
| Type | Base | Typical use |
|------|------|------------|
| `List` | TableWithQuery | List entities |
| `ListNoQuery` | TableNoQuery | List without query |
| `ListDefinitions` | TableWithQuery | List definitions |
| `ListTemplates` | TableWithQuery | List templates |
| `Get` | DetailsNoQuery | Get single entity |
| `GetDefinition` | DetailsNoQuery | Get definition |
| `GetTemplate` | DetailsNoQuery | Get template |
| `Create` | TableNoQuery | Create entity |
| `CreateWithDetailsOutput` | DetailsNoQuery | Create with detail output |
| `CreateConfig` | TableNoQuery | Create config |
| `CreateTemplate` | TableNoQuery | Create template |
| `Delete` | TableNoQuery | Delete entity |
| `DeleteTemplate` | TableNoQuery | Delete template |
| `Update` | TableNoQuery | Update entity |
| `UpdateTemplate` | TableNoQuery | Update template |
| `Set` | TableNoQuery | Set value |
| `Add` | TableNoQuery | Add entity |
| `Clear` | TableNoQuery | Clear entity |
| `Revoke` | TableNoQuery | Revoke access |
| `Enable` | TableNoQuery | Enable entity |
| `Disable` | TableNoQuery | Disable entity |
| `Start` | TableNoQuery | Start process |
| `Pause` | TableNoQuery | Pause process |
| `Resume` | TableNoQuery | Resume process |
| `Cancel` | TableNoQuery | Cancel process |
| `Status` | TableNoQuery | Show status |
| `Upload` | TableNoQuery | Upload artifact |
| `Download` | TableNoQuery | Download artifact |
| `DownloadTemplate` | TableNoQuery | Download template |
| `Install` | TableNoQuery | Install tool |
| `Uninstall` | TableNoQuery | Uninstall tool |
| `Register` | TableNoQuery | Register entity |
| `Import` | TableNoQuery | Import data |
| `Export` | TableNoQuery | Export data |
| `Setup` | TableNoQuery | Setup configuration |
| `WaitFor` | TableNoQuery | Wait for condition |
| `Login` | TableNoQuery | Session login |
| `Logout` | TableNoQuery | Session logout |
| `RestCall` | DetailsWithQuery | Direct REST call |

## Messages.properties Conventions

Every command needs entries in the module's `*Messages.properties` file:

### Required entries
- `fcli.<path>.usage.header` — Command description (e.g., `fcli.ssc.appversion.list.usage.header = List application versions`)
- `fcli.<path>.output.table.args` — Default table columns for output commands (e.g., `--columns id,applicationName,name,currentState`)

### Option descriptions
Picocli resolves descriptions via default key lookup: `fcli.<command-qualified-name>.<option-name-without-dashes>`

Example: option `--import` on command `fcli util mcp-server start` → key `fcli.util.mcp-server.start.import`

**Do not** set `descriptionKey` in Picocli annotations unless sharing a description across commands.

## FortifyCLITest Validations

`FortifyCLITest` walks the entire command tree and checks:

| Test | What it checks | Common fix |
|------|---------------|------------|
| `CMD_USAGE_HEADER` | Every command has a non-empty usage header | Add `fcli.<path>.usage.header` to Messages.properties |
| `CMD_DEFAULT_TABLE_OPTIONS_PRESENT` | Output commands have default table columns | Add `*.output.table.args` to Messages.properties |
| `CMD_STD_OPTS` | Standard options present (`-h`, `--help`, `--log-level`, `--log-file`, `--env-prefix`) | Inherited from base classes |
| `CMD_NAME` | Command name is kebab-case | Use `kebab-case` names |
| `CMD_DEPTH` | Command depth <= 4 | Don't nest too deeply |
| `OPT_NAME_FORMAT` | Options start with `-` (short) or `--` (long) | Fix option name format |
| `OPT_LONG_NAME` | Long options: lowercase + numbers + dashes, no trailing dash | Fix option name |
| `OPT_SHORT_NAME` | Short options: single lowercase letter or digit | Fix short name |
| `OPT_LONG_NAME_COUNT` | At least 1 long option name | Add `--long-name` |
| `OPT_SHORT_NAME_COUNT` | At most 1 short option name | Remove extra short names |
| `MULTI_OPT_PLURAL_NAME` | Multi-value option names are plural | Use `--items` not `--item` |
| `MULTI_OPT_SPLIT` | Multi-value options define `split` regex | Add `split=","` |
| `OPT_ARITY_VARIABLE` | No variable arity | Remove `arity` or fix range |
| `OPT_ARITY_BOOL` | Boolean option arity is 0 or 1 | Fix boolean arity |
| `OPT_ARITY_INTERACTIVE` | Interactive option arity is `0..1` | Set `arity="0..1"` |
| `OPT_ARITY_PRESENT` | Arity only on boolean/interactive options | Remove `arity` from other options |
| `OPT_EMPTY_DESCRIPTION` | All options have descriptions | Add to Messages.properties |
| `PARAM_EMPTY_DESCRIPTION` | Positional params have descriptions | Add to Messages.properties |
| `INJECT_MIXEE` | Mixins don't use `@Spec(MIXEE)` | Use `CommandHelperMixin` instead |

### `@DisableTest`
Use only when a design requirement genuinely conflicts with a test. Example: a multi-value option intentionally named `--import` (singular) may disable `MULTI_OPT_PLURAL_NAME`. Don't preemptively disable tests — fix the option definition instead.

## Key Mixins

| Mixin | Purpose |
|-------|---------|
| `OutputHelperMixins.*` | Command name + default output config |
| `*UnirestInstanceSupplierMixin` | Session-aware HTTP client (per product) |
| `CommandHelperMixin` | Access to `CommandSpec`, message resolver, root `CommandLine` |

### Mixin rules
- Use `@Mixin` to inject shared options/functionality
- Mixins implementing `ICommandAware` receive `CommandSpec` injection
- **Never** use `@Spec(MIXEE)` in mixins — use `CommandHelperMixin` instead (FortifyCLITest validates this)

## Command Implementation Checklist

1. Extend appropriate `Abstract<Product>OutputCommand`
2. Add `@Mixin OutputHelperMixins.<Type>` for command name + output
3. Add `usage.header` and `output.table.args` to Messages.properties
4. Add option descriptions to Messages.properties
5. Register as subcommand in parent container
6. Create missing parent containers if needed
7. Run `./gradlew :fcli-core:fcli-<module>:test` to verify FortifyCLITest passes
