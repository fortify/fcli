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

import java.util.ArrayDeque;
import java.util.ArrayList;

import com.fortify.cli.ai_assist.mcp.helper.MCPJobManager;
import com.fortify.cli.ai_assist.mcp.helper.arg.MCPToolArgHandlers;

import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Abstract {@link IMCPToolRunner} implementation that provides common functionality for
 * fcli-based tool runners, including command building and access to job management.
 *
 * @author Ruud Senden
 */
abstract class AbstractMCPToolFcliRunner implements IMCPToolRunner {
    protected abstract CommandSpec getCommandSpec();
    protected abstract MCPToolArgHandlers getToolSpecArgHelper();
    protected final MCPJobManager jobManager;
    
    protected AbstractMCPToolFcliRunner(MCPJobManager jobManager) {
        this.jobManager = jobManager;
    }
    
    /**
     * Build fcli arguments from the command identity and the given MCP tool arguments.
     */
    protected final String[] getFullArgs(CallToolRequest request) {
        var commandPath = new ArrayDeque<String>();
        for ( var spec = getCommandSpec(); spec.parent()!=null; spec = spec.parent() ) {
            commandPath.addFirst(spec.name());
        }
        var args = new ArrayList<>(commandPath);
        args.addAll(getToolSpecArgHelper().getFcliArgs(request==null ? null : request.arguments()));
        return args.toArray(String[]::new);
    }
}