---
name: fcli-mcp-setup
description: "Configure and start the fcli built-in MCP server so AI agents can use fcli commands as MCP tools. Use this skill when setting up fcli for MCP-based interaction in VS Code, Claude Desktop, or any MCP-compatible client."
argument-hint: "[--port <port>] [--modules fod,ssc,sc-sast]"
---

## Overview

fcli includes a built-in MCP server (`fcli util mcp-server start`) that exposes fcli commands as MCP tools. Once running, any MCP-compatible agent can call these tools directly, enabling AI-driven Fortify automation without shell command construction.

> **Note:** The fcli MCP server is a PREVIEW feature as of fcli v3.14.3. The API may change in future releases.

## Prerequisites

- `fcli` v3.x installed and on your `PATH`
- An active FoD or SSC session (the MCP server uses the existing fcli session)
- VS Code with GitHub Copilot (for VS Code MCP integration) or another MCP client

---

## Step 1 – Start the MCP Server

```shell
fcli util mcp-server start
```

By default, the server starts on `stdio` transport, which is how VS Code and most MCP clients expect to connect. The process stays running until terminated.

To run it as a background service (Linux/Mac):

```shell
fcli util mcp-server start &
```

---

## Step 2 – Configure VS Code

Add the fcli MCP server to VS Code's MCP configuration. There are two ways:

### Option A: Workspace-level (`mcp.json`)

Create or edit `.vscode/mcp.json` in your workspace:

```json
{
  "inputs": [],
  "servers": {
    "fcli": {
      "type": "stdio",
      "command": "fcli",
      "args": ["util", "mcp-server", "start"],
      "env": {
        "FCLI_DEFAULT_SSC_URL": "${env:FCLI_DEFAULT_SSC_URL}",
        "FCLI_DEFAULT_FOD_URL": "${env:FCLI_DEFAULT_FOD_URL}"
      }
    }
  }
}
```

### Option B: User-level (`settings.json`)

Add to VS Code `settings.json`:

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

After saving, VS Code will prompt you to start the server. Click **Start** or run the `MCP: Start Server` command.

---

## Step 3 – Configure Claude Desktop

Add to `~/Library/Application Support/Claude/claude_desktop_config.json` (Mac) or `%APPDATA%\Claude\claude_desktop_config.json` (Windows):

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

---

## Step 4 – Verify the Server

In VS Code: open the **MCP** panel (in the Activity Bar) and verify `fcli` shows as **Connected**.

Or test via terminal with the MCP inspector:

```shell
npx @modelcontextprotocol/inspector fcli util mcp-server start
```

---

## Step 5 – Log In Before Using MCP Tools

The MCP server relies on existing fcli sessions. Before using any Fortify MCP tools, authenticate:

```shell
# FoD
fcli fod session login --url https://ams.fortify.com --tenant <tenant> --user <user> --password <PAT>

# SSC
fcli ssc session login --url https://ssc.example.com/ssc --token <token>
```

---

## Available MCP Tools

Once running, the fcli MCP server exposes tools corresponding to fcli commands. The exact list depends on the fcli version; view available tools in your MCP client's tool panel.

Example tools (varies by version):
- `fod_release_list` – List FoD releases
- `fod_issue_list` – List FoD vulnerabilities
- `ssc_appversion_list` – List SSC application versions
- `ssc_issue_list` – List SSC vulnerabilities
- `sc_sast_scan_start` – Submit a SC-SAST scan
- `ssc_artifact_list` – List SSC artifacts

---

## Restricting Modules (optional)

If you want to expose only a subset of fcli modules as MCP tools, check the fcli help for available options:

```shell
fcli util mcp-server start --help
```

---

## Troubleshooting

| Problem | Solution |
|---------|---------|
| Server not found in VS Code | Check `fcli` is on the `PATH` used by VS Code; try restarting VS Code |
| `stdio` connection errors | Ensure no other process is using the same stdio channel |
| Tools not appearing | The server must be RUNNING (not just configured); check the MCP panel |
| Session errors when calling tools | Log in via fcli CLI first: `fcli fod session login ...` |
| Preview feature behavior changes | Check fcli release notes: https://github.com/fortify/fcli/releases |
