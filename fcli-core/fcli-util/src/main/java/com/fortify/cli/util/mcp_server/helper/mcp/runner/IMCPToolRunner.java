/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.util.mcp_server.helper.mcp.runner;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * Functional interface providing a single {@link #run(McpSyncServerExchange, CallToolRequest)}
 * method that can be registered as an MCP tool call handler through 
 * SyncToolSpecification.Builder::callHandler(runner::run).
 * 
 * @author Ruud Senden
 */
@FunctionalInterface
public interface IMCPToolRunner {
    public CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request);
}