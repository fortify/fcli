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

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.util.mcp_server.helper.mcp.MCPJobManager;
import com.fortify.cli.util.mcp_server.helper.mcp.arg.MCPToolArgHandlers;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import lombok.Getter;
import picocli.CommandLine.Model.CommandSpec;

/**
 * {@link IMCPToolRunner} implementation that returns zero or more records as produced
 * by the fcli command being executed in a structured JSON object.
 * This is commonly used to run fcli commands that return either a single or small number of records.
 * See {@link MCPToolFcliRunnerRecordsPaged} for running fcli commands that may return a large number
 * of records.
 *
 * @author Ruud Senden
 */
public final class MCPToolFcliRunnerRecords extends AbstractMCPToolFcliRunner {
    @Getter private final MCPToolArgHandlers toolSpecArgHelper;
    @Getter private final CommandSpec commandSpec;
    public MCPToolFcliRunnerRecords(MCPToolArgHandlers toolSpecArgHelper, CommandSpec commandSpec, MCPJobManager jobManager) {
        super(jobManager);
        this.toolSpecArgHelper = toolSpecArgHelper;
        this.commandSpec = commandSpec;
    }

    @Override
    public CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request) {
        var fullCmd = getFullCmd(request);
        var toolName = getCommandSpec().qualifiedName("_").replace('-', '_');
        try {
            var records = new ArrayList<JsonNode>();
            var counter = new AtomicInteger();
            Callable<CallToolResult> callable = () -> {
                var result = MCPToolFcliRunnerHelper.collectRecords(fullCmd, r->{ counter.incrementAndGet(); records.add(r); }, getCommandSpec());
                return MCPToolResult.fromRecords(result, records).asCallToolResult();
            };
            var progressStrategy = MCPJobManager.recordCounter(counter);
            return jobManager.execute(exchange, toolName, callable, progressStrategy, true);
        } catch ( Exception e ) {
            return MCPToolResult.fromError(e).asCallToolResult();
        }
    }
}