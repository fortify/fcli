/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.util.all_commands.helper.mcp.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.util.all_commands.helper.mcp.arg.CommandToolSpecArgHelper;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import picocli.CommandLine.Model.CommandSpec;

abstract class AbstractCommandToolSpecExecutor implements ICommandToolSpecExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractCommandToolSpecExecutor.class);
    protected abstract CommandSpec getCommandSpec();
    protected abstract CommandToolSpecArgHelper getToolSpecArgHelper();
    
    private final String getFullCmd(CallToolRequest request) {
        var cmd = getCommandSpec().qualifiedName(" ");
        var args = request==null || request.arguments()==null ? "" : getToolSpecArgHelper().getFcliCmdArgs(request.arguments());
        return String.format("%s %s", cmd, args);
    }
    
    @Override
    public CallToolResult execute(McpSyncServerExchange exchange, CallToolRequest request) {
        var fullCmd = getFullCmd(request);
        try {
            return execute(exchange, request, fullCmd);
        } catch ( Exception e ) {
            LOG.error("Exception while running fcli command:\n\t"+fullCmd, e);
            return new CallToolResult(e.toString(), true);
        }
    }
    
    protected abstract CallToolResult execute(McpSyncServerExchange exchange, CallToolRequest request, String fullCmd);
}