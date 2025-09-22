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
package com.fortify.cli.util.mcp_server.helper.mcp.arg;

import java.util.Map;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.util.mcp_server.helper.mcp.runner.MCPToolFcliRunnerRecordsPaged;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import lombok.SneakyThrows;

/**
 * {@link IMCPToolArgHandler} implementation for handling paging. The {@link #updateSchema(JsonSchema)}
 * method adds {@value MCPToolArgHandlerPaging#ARG_OFFSET}, {@value MCPToolArgHandlerPaging#ARG_LIMIT},
 * and {@value MCPToolArgHandlerPaging#REFRESH} arguments to the MCP tool schema; these arguments will
 * be used by {@link MCPToolFcliRunnerRecordsPaged} to return paged results and refresh any cached results
 * if requested.
 *
 * @author Ruud Senden
 */
public final class MCPToolArgHandlerPaging implements IMCPToolArgHandler {
    public static final String ARG_OFFSET = "pagination-offset";
    public static final String ARG_LIMIT = "pagination-limit";
    public static final String ARG_REFRESH = "refresh-cache";
    
    @Override @SneakyThrows
    public void updateSchema(JsonSchema schema) { 
        schema.properties().put(ARG_OFFSET, JsonHelper.getObjectMapper().readTree("""
            {
              "type": "integer",
              "default": 0,
              "title": "Paging offset",
              "description": "Return results starting from the given offset. Responses on previous requests indicate the total number of available records, as well as first, last, and next page offsets."
            }    
            """));
        /*
        schema.properties().put(ARG_LIMIT, JsonHelper.getObjectMapper().readTree("""
            {
              "type": "integer",
              "default": 20,
              "title": "Paging limit",
              "description": "Return at most the given number of records."
            }    
            """));
        */
        schema.properties().put(ARG_REFRESH, JsonHelper.getObjectMapper().readTree("""
            {
              "type": "boolean",
              "default": false,
              "title": "Refresh cache",
              "description": "This MCP tool caches results to allow for optimized retrieval of additional pages. If set to true, any cached data will be refreshed. It's recommended to pass 'true' after running any update operations (like 'create', 'update', 'delete', ...) on the same entity."
            }    
            """));
    }
    
    /** We always have fcli return full set of results, so there are no arguments to pass.
     *  Paging the results is handled by {@link MCPToolFcliRunnerRecordsPaged} */
    @Override
    public String getFcliCmdArgs(Map<String, Object> toolArgs) {
        return "";
    }
}