/*
 * Copyright 2021-2026 Open Text.
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

import com.fortify.cli.util._common.helper.AsyncTaskFcliCommand;
import com.fortify.cli.util.mcp_server.helper.mcp.MCPJobManager;
import com.fortify.cli.util.mcp_server.helper.mcp.arg.MCPToolArgHandlers;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Model.CommandSpec;

/**
 * {@link IMCPToolRunner} implementation that, given offset and limit, returns the requested set of
 * records together with pagination data in a structured JSON object.
 * This is commonly used to run fcli commands that may return a large number of records.
 * Paging logic is shared via {@link MCPToolFcliPagedHelper}.
 *
 * @author Ruud Senden
 */
@Slf4j
public final class MCPToolFcliRunnerRecordsPaged extends AbstractMCPToolFcliRunner {
    @Getter private final MCPToolArgHandlers toolSpecArgHelper;
    @Getter private final CommandSpec commandSpec;
    private final MCPToolFcliPagedHelper pagedHelper;

    public MCPToolFcliRunnerRecordsPaged(MCPToolArgHandlers toolSpecArgHelper, CommandSpec commandSpec, MCPJobManager jobManager) {
        super(jobManager);
        this.toolSpecArgHelper = toolSpecArgHelper;
        this.commandSpec = commandSpec;
        this.pagedHelper = new MCPToolFcliPagedHelper(jobManager);
    }

    @Override
    public CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request) {
        var fullCmd = getFullCmd(request);
        var pageParams = MCPToolFcliPagedHelper.PageParams.from(request);
        var defaultOptions = MCPToolFcliRunnerHelper.collectMcpDefaultOptions(commandSpec);
        var producer = new AsyncTaskFcliCommand(fullCmd, defaultOptions);
        return pagedHelper.run(fullCmd, pageParams,
            (jobId, refresh) -> jobManager.getAsyncJobManager().getOrStartBackground(jobId, refresh, producer));
    }
}
