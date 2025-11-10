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

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import com.fortify.cli.util.mcp_server.helper.mcp.MCPJobManager;
import com.fortify.cli.util.mcp_server.helper.mcp.arg.MCPToolArgHandlers;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import lombok.Getter;
import picocli.CommandLine.Model.CommandSpec;
/**
 * IMCPToolRunner implementation for running regular fcli commands. Returns the command result
 * being executed in a structured JSON object with stdout output.
 * The command is expected to return stdout output which is collected and returned as-is
 * in the 'stdout' field of the result JSON.
 *
 * @author Ruud Senden
 */
public final class MCPToolFcliRunnerPlainText extends AbstractMCPToolFcliRunner {
    @Getter private final MCPToolArgHandlers toolSpecArgHelper;
    @Getter private final CommandSpec commandSpec;
    public MCPToolFcliRunnerPlainText(MCPToolArgHandlers toolSpecArgHelper, CommandSpec commandSpec, MCPJobManager jobManager) {
        super(jobManager);
        this.toolSpecArgHelper = toolSpecArgHelper;
        this.commandSpec = commandSpec;
    }

    @Override
    public CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request) {
        try {
            var fullCmd = getFullCmd(request);
            var work = createCommandWork(fullCmd);
            var toolName = getCommandSpec().qualifiedName("_").replace('-', '_');
            return jobManager.execute(exchange, toolName, work, MCPJobManager.ticking(new AtomicInteger()), true);
        } catch (Exception e) {
            return MCPToolResult.fromError(e).asCallToolResult();
        }
    }
    
    private Callable<CallToolResult> createCommandWork(String fullCmd) {
        return () -> {
            var result = MCPToolFcliRunnerHelper.collectStdout(fullCmd, commandSpec);
            return MCPToolResult.fromPlainText(result).asCallToolResult();
        };
    }
}