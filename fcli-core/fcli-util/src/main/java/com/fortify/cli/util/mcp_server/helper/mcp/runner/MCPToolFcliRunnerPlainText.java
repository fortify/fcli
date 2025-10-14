/*
 * Copyright 2021-2025 Open Text.
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

import com.fortify.cli.util.mcp_server.helper.mcp.arg.MCPToolArgHandlers;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Model.CommandSpec;
/**
 * {@link IMCPToolRunner} implementation that returns plain text output of the fcli command
 * being executed in a structured JSON object as described by {@link MCPToolResultPlainText}.
 * This is commonly used to run fcli commands that don't output structured records, like 
 * 'fcli * action run' commands.
 *
 * @author Ruud Senden
 */
@RequiredArgsConstructor
public final class MCPToolFcliRunnerPlainText extends AbstractMCPToolFcliRunner {
    @Getter private final MCPToolArgHandlers toolSpecArgHelper;
    @Getter private final CommandSpec commandSpec;
    
    @Override
    protected CallToolResult execute(McpSyncServerExchange exchange, CallToolRequest request, String fullCmd) {
        var result = MCPToolFcliRunnerHelper.collectStdout(fullCmd);
        return MCPToolResultPlainText.from(result).asCallToolResult();
    }
}