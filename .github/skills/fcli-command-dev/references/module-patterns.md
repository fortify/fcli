# Module-Specific Command Patterns

This reference documents how each fcli module differs in command implementation patterns.
Read the section relevant to the module you're working in.

## Table of Contents

1. [Product Modules (SSC, FoD, SC-SAST, SC-DAST)](#product-modules)
2. [Tool Module](#tool-module)
3. [Util Module](#util-module)
4. [Config Module](#config-module)
5. [Action Module](#action-module)

---

## Product Modules

Product modules (SSC, FoD, SC-SAST, SC-DAST) share a common pattern but have product-specific
abstract base classes and session management.

### Common Structure

Each product module has:
- `_common/output/cli/cmd/` — Abstract base command classes
- `_common/rest/` — REST helpers, URL constants, bulk request builders
- `_common/session/` — Session login/logout commands and helpers
- `_main/cli/cmd/` — Root product command (e.g., `SSCCommands`)
- `<entity>/cli/cmd/` — Entity-specific commands
- `<entity>/cli/mixin/` — Entity-specific mixins (resolvers, shared options)
- `<entity>/helper/` — Entity descriptors and API helpers

### SSC Module

**Base classes:**
- `AbstractSSCBaseRequestOutputCommand` — Returns `HttpRequest<?>` from `getBaseRequest(UnirestInstance)`
- `AbstractSSCJsonNodeOutputCommand` — Returns `JsonNode` from `getJsonNode(UnirestInstance)`

**Session management:** Automatic via `SSCUnirestInstanceSupplierMixin` (injected in `AbstractSSCOutputCommand`)

**URL constants:** Use `SSCUrls` for standard API paths:
```java
SSCUrls.PROJECT_VERSIONS         // "/api/v1/projectVersions"
SSCUrls.PROJECT_VERSION(id)      // "/api/v1/projectVersions/" + id
```

**Bulk requests:** Use `SSCBulkRequestBuilder` for multi-step operations:
```java
new SSCBulkRequestBuilder()
    .request("step1", unirest.post(url).body(body))
    .request("step2", unirest.get(url2))
    .execute(unirest);
```

**Server-side query support:** SSC supports server-side filtering via the `q` parameter.
Implement `IServerSideQueryParamGeneratorSupplier` for list commands:
```java
@Getter private IServerSideQueryParamValueGenerator serverSideQueryParamGenerator =
    new SSCQParamGenerator()
        .add("id", SSCQParamValueGenerators::plain)
        .add("name", SSCQParamValueGenerators::wrapInQuotes);
```

**Resource bundle:** `com.fortify.cli.ssc.i18n.SSCMessages`
**Key prefix:** `fcli.ssc.<entity>.<verb>`

**Example list command:**
```java
@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SSCPluginListCommand extends AbstractSSCBaseRequestOutputCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get("/api/v1/plugins?limit=100");
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
```

**Example create command (complex):**
```java
@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class SSCAppVersionCreateCommand extends AbstractSSCJsonNodeOutputCommand
    implements IRecordTransformer, IActionCommandResultSupplier {

    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Mixin private SSCAppAndVersionNameResolverMixin.PositionalParameter sscAppAndVersionNameResolver;
    @Option(names={"--description","-d"}, required = false)
    private String description;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        // Complex multi-step creation logic
        // Return the created entity as JsonNode
    }

    @Override
    public JsonNode transformRecord(JsonNode input) {
        return SSCAppVersionHelper.renameFields(input);
    }

    @Override
    public String getActionCommandResult() {
        return "CREATED";
    }
}
```

### FoD Module

**Base classes:**
- `AbstractFoDBaseRequestOutputCommand` — Same pattern as SSC
- `AbstractFoDJsonNodeOutputCommand` — Same pattern as SSC

**Session management:** Via `FoDUnirestInstanceSupplierMixin`

**Key difference from SSC:** FoD uses OAuth tokens; session descriptors include client credentials or
user credentials. The `FoDProductHelper` handles FoD-specific pagination and response wrapping.

**Resource bundle:** `com.fortify.cli.fod.i18n.FoDMessages`
**Key prefix:** `fcli.fod.<entity>.<verb>`

### SC-SAST Module

**Base classes:** `AbstractSCSast*OutputCommand` variants
**Session management:** Via `SCSastUnirestInstanceSupplierMixin`
**Resource bundle:** `com.fortify.cli.sc_sast.i18n.SCSastMessages`
**Key prefix:** `fcli.sc-sast.<entity>.<verb>`

### SC-DAST Module

**Base classes:** `AbstractSCDast*OutputCommand` variants
**Session management:** Via `SCDastUnirestInstanceSupplierMixin`
**Resource bundle:** `com.fortify.cli.sc_dast.i18n.SCDastMessages`
**Key prefix:** `fcli.sc-dast.<entity>.<verb>`

---

## Tool Module

The tool module manages installation and execution of Fortify tools (fcli itself, ScanCentral Client,
FoD Uploader, Debricked CLI, etc.). It has a very different pattern from product modules.

### Key Differences from Product Modules

- **No session management** — Tool commands don't connect to a product API
- **Abstract base classes per verb** — Each standard operation (install, uninstall, list, get, run,
  register, list-platforms) has its own abstract base class
- **`Tool` enum** — Each tool is defined in the `Tool` enum; commands override `getTool()` to identify
  which tool they manage
- **Shared option descriptions** — Tool options use `descriptionKey` to share descriptions across tools
  (e.g., `fcli.tool.install.version` is shared by all install commands)

### Standard Tool Commands

Every tool typically provides these commands:
- `install` — extends `AbstractToolInstallCommand`
- `uninstall` — extends `AbstractToolUninstallCommand`
- `list` — extends `AbstractToolListCommand`
- `get` — extends `AbstractToolGetCommand`
- `list-platforms` — extends `AbstractToolListPlatformsCommand`
- `run` — extends `AbstractToolRunCommand` or `AbstractToolRunShellOrJavaCommand`
- `register` — extends `AbstractToolRegisterCommand`

### Example Tool Command

```java
@Command(name = OutputHelperMixins.Install.CMD_NAME)
public class ToolFcliInstallCommand extends AbstractToolInstallCommand {
    @Getter @Mixin private OutputHelperMixins.Install outputHelper;

    @Override
    protected final Tool getTool() {
        return Tool.FCLI;
    }

    @Override
    protected String getFallbackPlatform() {
        return "java";
    }

    @Override
    protected void postInstall(ToolInstaller installer, ToolInstallationResult result) {
        // Tool-specific post-install logic (bin scripts, completion scripts, etc.)
    }
}
```

### Tool Container Command

```java
@Command(
    name = "fcli",
    subcommands = {
        ToolFcliGetCommand.class,
        ToolFcliInstallCommand.class,
        ToolFcliListCommand.class,
        ToolFcliListPlatformsCommand.class,
        ToolFcliRegisterCommand.class,
        ToolFcliRunCommand.class,
        ToolFcliUninstallCommand.class,
    }
)
public class ToolFcliCommands extends AbstractContainerCommand {
}
```

### Resource Bundle

Tool messages use shared description keys extensively:
```properties
# Shared across all tool install commands
fcli.tool.install.version = Tool version to install; see output of list command...
fcli.tool.install.platform = By default, fcli will try to install tool binaries...

# Per-tool usage headers
fcli.tool.fcli.install.usage.header = Install fcli.
fcli.tool.fcli.list.usage.header = List available and installed fcli versions.

# Shared output table columns
fcli.tool.output.table.args = name,version,aliasesString,stable,installDir,isDefaultMarker
```

---

## Util Module

The util module provides general-purpose utilities that don't depend on any product.

### Key Differences

- **No session management** — Commands extend `AbstractOutputCommand` directly
- **No product prefix** — Classes are named `<Entity><Verb>Command` (e.g., `VariableListCommand`)
- **Simple structure** — No product-specific helpers or API clients needed

### Example

```java
@Command(name = OutputHelperMixins.List.CMD_NAME)
public class VariableListCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;

    @Override
    public JsonNode getJsonNode() {
        return FcliVariableHelper.listDescriptors();
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
```

### Resource Bundle

```properties
fcli.util.variable.usage.header = Manage fcli variables.
fcli.util.variable.list.usage.header = List fcli variables.
fcli.util.variable.output.table.args = name,value
```

---

## Config Module

Similar to the util module but for configuration management (proxy, public keys, trust store, language).

### Key Differences

- Same base pattern as util (extends `AbstractOutputCommand`)
- Root command: `ConfigCommands` in `fcli-core/fcli-config`
- Resource bundle: `com.fortify.cli.config.i18n.ConfigMessages`

---

## Action Module

The action module provides commands for managing and running YAML-based workflow actions.

### Key Differences

- Action commands extend `AbstractActionRunCommand`
- Actions are YAML files in `src/main/resources/.../actions/zip/`
- The command framework parses action YAML and runs steps via `ActionRunner`
- See `.github/instructions/action-yaml.instructions.md` for action YAML development

---

## Adding a New Tool to the Tool Module

If adding support for a completely new tool:

1. Add a new enum value to `Tool` (in `fcli-core/fcli-tool/src/main/java/.../helper/Tool.java`)
2. Create the package: `com.fortify.cli.tool.<tool_name>.cli.cmd`
3. Create standard commands extending the abstract base classes
4. Create the container command `Tool<ToolName>Commands`
5. Register in `ToolCommands.java` subcommands
6. Add tool definition in tool-definitions repository
7. Add entries to `ToolMessages.properties`

## Adding a New Entity to a Product Module

If adding support for a new SSC/FoD entity:

1. Create the entity package: `com.fortify.cli.<product>.<entity_name>/`
2. Create sub-packages: `cli/cmd/`, `cli/mixin/`, `helper/`
3. Create leaf command classes (list, get, create, etc.)
4. Create the container command `<Prefix><Entity>Commands`
5. Register in the product root command (e.g., `SSCCommands.java`)
6. Add all required entries to `*Messages.properties`:
   - `usage.header` for container and each leaf command
   - `output.table.args` for default table columns
   - Option descriptions for each option
7. If the entity has a resolver pattern (lookup by ID or name), create a resolver mixin in `cli/mixin/`
8. If the entity needs data transformation, create a helper in `helper/`
