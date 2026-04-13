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

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.runner.ActionFunctionExecutor;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.util._common.helper.AsyncTaskActionFunction;
import com.fortify.cli.util.mcp_server.helper.mcp.MCPJobManager;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * {@link IMCPToolRunner} implementation for streaming action functions. Records are
 * collected in the background using {@link MCPToolAsyncJobManager}, and results are
 * returned as paged responses via {@link MCPToolFcliPagedHelper} — the same mechanism
 * used by {@link MCPToolFcliRunnerRecordsPaged} for streaming fcli commands.
 *
 * @author Ruud Senden
 */
public final class MCPToolFcliRunnerFunctionStreaming implements IMCPToolRunner {
    private final ActionFunctionExecutor executor;
    private final MCPJobManager jobManager;
    private final String toolName;
    private final MCPToolFcliPagedHelper pagedHelper;

    public MCPToolFcliRunnerFunctionStreaming(ActionFunctionExecutor executor, MCPJobManager jobManager, String toolName) {
        this.executor = executor;
        this.jobManager = jobManager;
        this.toolName = toolName;
        this.pagedHelper = new MCPToolFcliPagedHelper(jobManager);
    }

    @Override
    public CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request) {
        var argsNode = buildArgsNode(request);
        var jobId = toolName + ":" + argsNode;
        var pageParams = MCPToolFcliPagedHelper.PageParams.from(request);
        var producer = new AsyncTaskActionFunction(executor, argsNode);
        return pagedHelper.run(jobId, pageParams,
            (key, refresh) -> jobManager.getAsyncJobManager().getOrStartBackground(key, refresh, producer));
    }

    private ObjectNode buildArgsNode(CallToolRequest request) {
        var argsNode = JsonHelper.getObjectMapper().createObjectNode();
        if (request != null && request.arguments() != null) {
            for (Map.Entry<String, Object> entry : request.arguments().entrySet()) {
                var value = entry.getValue();
                if (value instanceof JsonNode jn) {
                    argsNode.set(entry.getKey(), jn);
                } else if (value != null) {
                    argsNode.set(entry.getKey(), JsonHelper.getObjectMapper().valueToTree(value));
                }
            }
        }
        return argsNode;
    }
}
