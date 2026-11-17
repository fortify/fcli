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
package com.fortify.cli.ai_assist.mcp.helper.runner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import com.fortify.cli.ai_assist.mcp.helper.MCPJobManager;
import com.fortify.cli.ai_assist.mcp.helper.arg.IMCPToolArgHandler;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import picocli.CommandLine.Model.CommandSpec;

/**
 * IMCPToolRunner implementation for running fcli actions through 'fcli <module> action run <action>'
 * Collects stdout and returns as plain text MCP tool result.
 */
public final class MCPToolFcliRunnerAction implements IMCPToolRunner {
    private final String module;
    private final Action action;
    private final List<IMCPToolArgHandler> argHandlers;
    private final MCPJobManager jobManager;
    
    public MCPToolFcliRunnerAction(String module, Action action, List<IMCPToolArgHandler> argHandlers, MCPJobManager jobManager) {
        this.module = module;
        this.action = action;
        this.argHandlers = argHandlers;
        this.jobManager = jobManager;
    }

    @Override
    public CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request) {
        try {
            var args = buildArgs(request);
            var toolName = buildToolName();
            var work = createActionWork(args);
            return jobManager.execute(exchange, toolName, work, MCPJobManager.ticking(new AtomicInteger()), true);
        } catch (Exception e) {
            return MCPToolResult.fromError(e).asCallToolResult();
        }
    }
    
    private Callable<CallToolResult> createActionWork(String[] args) {
        return () -> {
            var result = MCPToolFcliRunnerHelper.collectStdout(args, getActionCommandSpec());
            return MCPToolResult.fromPlainText(result).asCallToolResult();
        };
    }
    
    private String buildToolName() {
        return String.format("fcli_%s_action_%s", 
            module.replace('-', '_'), 
            action.getMetadata().getName().replace('-', '_'));
    }
    
    private String[] buildArgs(CallToolRequest request) {
        var args = new ArrayList<>(List.of(module, "action", "run", action.getMetadata().getName()));
        var toolArgs = request==null || request.arguments()==null ? Map.<String, Object>of() : request.arguments();
        argHandlers.forEach(h->args.addAll(h.getFcliArgs(toolArgs)));
        return args.toArray(String[]::new);
    }

    private CommandSpec getActionCommandSpec() {
        return FcliCommandSpecHelper.getCommandSpec(String.format("fcli %s action run", module));
    }
}
