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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.util.mcp_server.helper.mcp.MCPJobManager;
import com.fortify.cli.util.mcp_server.helper.mcp.arg.IMCPToolArgHandler;

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
            var fullCmd = buildFullCmd(request);
            var toolName = buildToolName();
            var work = createActionWork(fullCmd);
            return jobManager.execute(exchange, toolName, work, MCPJobManager.ticking(new AtomicInteger()), true);
        } catch (Exception e) {
            return MCPToolResult.fromError(e).asCallToolResult();
        }
    }
    
    private Callable<CallToolResult> createActionWork(String fullCmd) {
        return () -> {
            var result = MCPToolFcliRunnerHelper.collectStdout(fullCmd, getActionCommandSpec());
            return MCPToolResult.fromPlainText(result).asCallToolResult();
        };
    }
    
    private String buildToolName() {
        return String.format("fcli_%s_action_%s", 
            module.replace('-', '_'), 
            action.getMetadata().getName().replace('-', '_'));
    }
    
    private String buildFullCmd(CallToolRequest request) {
        var base = String.format("fcli %s action run %s", module, action.getMetadata().getName());
        var argStr = argHandlers.stream()
                .map(h->h.getFcliCmdArgs(request==null?null:request.arguments()))
                .filter(s->s!=null && !s.isBlank())
                .collect(Collectors.joining(" "));
        return argStr.isBlank()?base:String.format("%s %s", base, argStr);
    }

    private CommandSpec getActionCommandSpec() {
        return FcliCommandSpecHelper.getCommandSpec(String.format("fcli %s action run", module));
    }
}
