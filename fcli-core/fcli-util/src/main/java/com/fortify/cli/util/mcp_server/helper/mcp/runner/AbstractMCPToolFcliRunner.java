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

import com.fortify.cli.util.mcp_server.helper.mcp.MCPJobManager;
import com.fortify.cli.util.mcp_server.helper.mcp.arg.MCPToolArgHandlers;

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
     * Build full fcli command to execute, based on MCP tool arguments from the given request
     */
    protected final String getFullCmd(CallToolRequest request) {
        var cmd = getCommandSpec().qualifiedName(" ");
        var providedArgs = request==null ? null : request.arguments();
        var args = providedArgs==null ? "" : getToolSpecArgHelper().getFcliCmdArgs(providedArgs);
        return String.format("%s %s", cmd, args).trim();
    }
}