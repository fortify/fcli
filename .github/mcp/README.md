# fcli MCP Server

fcli includes a built-in MCP (Model Context Protocol) server that exposes its commands as tools for LLM-based agents. This directory contains sample configurations for connecting various clients to the fcli MCP server.

> **Note:** `fcli util mcp-server` is a PREVIEW feature as of fcli v3.14.3.

## Quick Start

### 1. Authenticate First

Before starting the MCP server, log in to your Fortify product:

```bash
# FoD
fcli fod session login --url https://ams.fortify.com --tenant <tenant> --user <user> --password <PAT>

# SSC
fcli ssc session login --url https://ssc.example.com/ssc --token <token>
```

### 2. Configure Your MCP Client

#### VS Code (Workspace)

Copy `mcp-config.json` to `.vscode/mcp.json` in your workspace, or merge the `servers` block into your existing `.vscode/mcp.json`:

```bash
cp mcp/mcp-config.json .vscode/mcp.json
```

Then open VS Code's MCP panel and click **Start** next to `fcli`.

#### VS Code (User Settings)

Add to your VS Code `settings.json` (`Ctrl+Shift+P` → "Open User Settings JSON"):

```json
{
  "mcp": {
    "servers": {
      "fcli": {
        "type": "stdio",
        "command": "fcli",
        "args": ["util", "mcp-server", "start"]
      }
    }
  }
}
```

#### Claude Desktop

Add to your Claude Desktop config:

- **Mac:** `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "fcli": {
      "command": "fcli",
      "args": ["util", "mcp-server", "start"]
    }
  }
}
```

#### Windows (fcli.exe not on PATH)

If `fcli` is not on your system PATH, use the full path to the executable:

```json
{
  "servers": {
    "fcli": {
      "type": "stdio",
      "command": "C:\\Users\\<user>\\fortify\\tools\\bin\\fcli.exe",
      "args": ["util", "mcp-server", "start"]
    }
  }
}
```

### 3. Start the Server

The server starts automatically when VS Code or Claude Desktop launches (based on your config). To start manually for testing:

```bash
fcli util mcp-server start
```

### 4. Verify

- **VS Code:** Open the MCP panel (bottom bar → MCP or `Ctrl+Shift+P` → "MCP: Show Servers"). `fcli` should show as **Connected**.
- **MCP Inspector:** `npx @modelcontextprotocol/inspector fcli util mcp-server start`

## Passing Credentials via Environment Variables

For CI/CD or shared environments, pass credentials as environment variables rather than relying on stored sessions:

```json
{
  "servers": {
    "fcli": {
      "type": "stdio",
      "command": "fcli",
      "args": ["util", "mcp-server", "start"],
      "env": {
        "FCLI_DEFAULT_SSC_URL": "https://ssc.example.com/ssc",
        "FCLI_DEFAULT_SSC_TOKEN": "<your-token>"
      }
    }
  }
}
```

## Troubleshooting

| Problem | Solution |
|---------|---------|
| `fcli: command not found` | Ensure fcli is on `PATH`; use full path in config if needed |
| Server shows as Disconnected | Restart VS Code; check for fcli errors via the MCP output panel |
| No Fortify tools appearing | The session may not be established; run login command in terminal |
| Preview API changes | Check release notes: https://github.com/fortify/fcli/releases |

## Further Reading

- [fcli util mcp-server docs](https://fortify.github.io/fcli/latest/manpage/fcli-util-mcp-server-start.html)
- [VS Code MCP documentation](https://code.visualstudio.com/docs/copilot/customization/mcp-servers)
- [Model Context Protocol specification](https://modelcontextprotocol.io/)
