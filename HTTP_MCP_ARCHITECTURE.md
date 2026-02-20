# HTTP MCP Server for SSC - Architecture

## Overview

FCLI's existing STDIO MCP server (`fcli util mcp-server start -m=ssc`) communicates over standard input/output and relies on persisted fcli sessions for authentication. The new **Streamable HTTP** MCP server (`fcli util mcp-server http-start`) exposes the same SSC tools over HTTP, where:

- The SSC URL is fixed at server startup via `--ssc-url`
- Each request authenticates via a Bearer token in the HTTP `Authorization` header
- No fcli session login/logout is needed; a synthetic in-memory session is created per-request
- All existing SSC tool specs, runners, and argument handlers are reused unchanged

## Request Flow

```
MCP Client  --->  HTTP POST /mcp
                  [Authorization: Bearer <ssc-token>]
                     |
              BearerAuthFilter (Jetty servlet filter)
              Extracts token, sets HttpMcpAuthContext ThreadLocal
                     |
              HttpServletStreamableServerTransportProvider
              (MCP SDK 0.17.0 - handles JSON-RPC over HTTP)
                     |
              McpServer.sync(transport)
              Routes to registered tool handler
                     |
              Tool handler -> MCPJobManager.execute()
              Captures ThreadLocal, wraps Callable to propagate
                     |
              Worker thread: ThreadLocal restored
              fcli command execution begins
                     |
              SSCAndScanCentralUnirestInstanceSupplierMixin
              -> getSessionDescriptor()
              -> SSCAndScanCentralSessionHelper.getOrSynthetic()
                     |
              Detects HttpMcpAuthContext is set
              Returns synthetic SSCAndScanCentralSessionDescriptor
              (in-memory, from URL + token, no disk I/O)
                     |
              SSC REST call with FortifyToken header
```

## Key Design Decisions

### ThreadLocal-based auth propagation

The MCP SDK's `HttpServletStreamableServerTransportProvider` runs tool handlers on the servlet thread. However, `MCPJobManager` dispatches actual work to a thread pool. To bridge this gap:

1. The Jetty `BearerAuthFilter` sets `HttpMcpAuthContext` (a `ThreadLocal`) on the servlet thread
2. `MCPJobManager.startJobExecution()` captures the auth context before submitting work
3. The worker thread wrapper restores it before calling the tool, and clears it in a `finally` block

When `HttpMcpAuthContext.get()` returns null (STDIO mode), no wrapping occurs - zero overhead for the existing path.

### Synthetic session descriptors

Rather than modifying the session login/logout flow, `SSCAndScanCentralSessionHelper.getOrSynthetic()` checks for a non-null `HttpMcpAuthContext`:

- **HTTP mode**: Builds an `SSCAndScanCentralSessionDescriptor` in memory with the URL and token from the request. SC-SAST and SC-DAST are disabled with a reason string.
- **STDIO mode**: Falls through to the normal `instance().get()` which reads from the persisted session store.

The synthetic descriptor sets `terminalDate=null` on the token data, which the existing `hasActiveCachedTokenResponse()` logic at `SSCAndScanCentralSessionDescriptor:93-94` treats as "active" (the null-means-active convention comes from older SSC instances that don't report expiry).

### Reuse via MCPToolSpecFactory

The tool spec creation logic (converting fcli command specs and actions into MCP tool definitions) was extracted from `MCPServerStartCommand` inner classes into `MCPToolSpecFactory`. Both `MCPServerStartCommand` (STDIO) and `HttpMCPServerStartCommand` (HTTP) call `MCPToolSpecFactory.createToolSpecs(module, jobManager)`, ensuring identical tool registration.

## Files Changed

### New Files

| File | Purpose |
|------|---------|
| `fcli-core/fcli-common/.../rest/unirest/HttpMcpAuthContext.java` | ThreadLocal holder with `AuthInfo(sscUrl, token)` record. Placed in fcli-common so fcli-ssc can read it (fcli-ssc depends on fcli-common, not fcli-util). |
| `fcli-core/fcli-util/.../helper/mcp/MCPToolSpecFactory.java` | Extracted tool spec creation from `MCPServerStartCommand`. Public entry point: `createToolSpecs(McpModule, MCPJobManager)`. |
| `fcli-core/fcli-util/.../cli/cmd/HttpMCPServerStartCommand.java` | The `http-start` command. Sets up embedded Jetty with auth filter + MCP servlet, creates MCPJobManager and tool specs for the SSC module. |

### Modified Files

| File | Change |
|------|--------|
| `fcli-other/fcli-bom/build.gradle.kts` | Added version constraints for `jetty-ee10-servlet:12.0.18` and `jetty-server:12.0.18` |
| `fcli-core/fcli-util/build.gradle.kts` | Added `implementation` dependencies on the two Jetty artifacts |
| `fcli-core/fcli-util/.../helper/mcp/MCPJobManager.java` | In `startJobExecution()`: captures `HttpMcpAuthContext` and wraps the work `Callable` to propagate it to the worker thread |
| `fcli-core/fcli-ssc/.../_common/session/helper/SSCAndScanCentralSessionHelper.java` | Added `getOrSynthetic(sessionName, failIfUnavailable)` static method |
| `fcli-core/fcli-ssc/.../_common/rest/cli/mixin/SSCAndScanCentralUnirestInstanceSupplierMixin.java` | Changed `getSessionDescriptor()` to call `getOrSynthetic()` instead of `instance().get()` |
| `fcli-core/fcli-util/.../cli/cmd/MCPServerStartCommand.java` | Replaced inner helper classes with delegation to `MCPToolSpecFactory`; made `PERIOD_HELPER` and `getServerCapabilities()` package-accessible for reuse by `HttpMCPServerStartCommand` |
| `fcli-core/fcli-util/.../cli/cmd/MCPServerCommands.java` | Added `HttpMCPServerStartCommand.class` to `@Command(subcommands=...)` |

## Usage

```bash
# Start the HTTP MCP server
java -jar fcli.jar util mcp-server http-start \
  --ssc-url https://ssc.example.com \
  --port 8080

# Test with curl (initialize handshake)
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ssc-token>" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2025-03-26",
      "capabilities": {},
      "clientInfo": {"name": "test", "version": "1.0"}
    }
  }'
```

### Command Options

| Option | Default | Description |
|--------|---------|-------------|
| `--ssc-url` | *(required)* | SSC base URL |
| `--port` | 8080 | HTTP listen port |
| `--work-threads` | 10 | Worker threads for tool execution |
| `--progress-threads` | 4 | Threads for progress tracking |
| `--job-safe-return` | 25s | Time to wait before returning an in-progress job token |
| `--progress-interval` | 5s | Interval between progress updates |

## Backward Compatibility

- The existing STDIO server (`fcli util mcp-server start -m=ssc`) is unchanged in behavior
- The `MCPJobManager` auth propagation is a no-op when `HttpMcpAuthContext` is null (STDIO mode)
- `SSCAndScanCentralSessionHelper.getOrSynthetic()` falls through to normal session lookup when no auth context is set
- No changes to native image configuration (HTTP mode is JVM-only; Jetty is not included in native image builds)
