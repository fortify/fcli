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
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.runner.ActionFunctionExecutor;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.util.mcp_server.helper.mcp.MCPJobManager;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * IMCPToolRunner implementation for running action functions directly.
 * Converts MCP tool arguments to function args, executes the function
 * via {@link ActionFunctionExecutor}, and returns the result as JSON text.
 */
public final class MCPToolFcliRunnerFunction implements IMCPToolRunner {
    private final ActionFunctionExecutor executor;
    private final MCPJobManager jobManager;
    private final String toolName;

    public MCPToolFcliRunnerFunction(ActionFunctionExecutor executor, MCPJobManager jobManager, String toolName) {
        this.executor = executor;
        this.jobManager = jobManager;
        this.toolName = toolName;
    }

    @Override
    public CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request) {
        try {
            var argsNode = buildArgsNode(request);
            Callable<CallToolResult> work = () -> {
                var result = executor.execute(argsNode);
                return toCallToolResult(result);
            };
            return jobManager.execute(exchange, toolName, work, MCPJobManager.ticking(new AtomicInteger()), true);
        } catch (Exception e) {
            return MCPToolResult.fromError(e).asCallToolResult();
        }
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

    private CallToolResult toCallToolResult(Object result) {
        if (result instanceof JsonNode jn) {
            var text = jn.toPrettyString();
            return MCPToolResult.fromPlainText(
                    new com.fortify.cli.common.util.OutputHelper.Result(0, text, ""))
                    .asCallToolResult();
        }
        // For streaming functions, collect all records
        if (result instanceof com.fortify.cli.common.action.model.ActionStepRecordsForEach.IActionStepForEachProcessor processor) {
            var records = new java.util.ArrayList<JsonNode>();
            processor.process(node -> {
                records.add(node);
                return true;
            });
            var arrayNode = JsonHelper.getObjectMapper().createArrayNode();
            records.forEach(arrayNode::add);
            return MCPToolResult.fromPlainText(
                    new com.fortify.cli.common.util.OutputHelper.Result(0, arrayNode.toPrettyString(), ""))
                    .asCallToolResult();
        }
        return MCPToolResult.fromPlainText(
                new com.fortify.cli.common.util.OutputHelper.Result(0, String.valueOf(result), ""))
                .asCallToolResult();
    }
}
