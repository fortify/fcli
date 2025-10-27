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
package com.fortify.cli.util.mcp_server.helper.mcp.arg;

import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;

/**
 * Interface for defining MCP tool arguments
 *
 * @author Ruud Senden
 */
public interface IMCPToolArgHandler {
    /**
     * This method allows implementations to add MCP tool arguments and related data to the given {@link JsonSchema}.
     */
    public void updateSchema(JsonSchema schema);
    
    /**
     * This method allows implementations to generate fcli command arguments based on the given MCP tool arguments.
     * If no arguments are to be added, this should return an empty string.  
     */
    public String getFcliCmdArgs(Map<String, Object> toolArgs);
}