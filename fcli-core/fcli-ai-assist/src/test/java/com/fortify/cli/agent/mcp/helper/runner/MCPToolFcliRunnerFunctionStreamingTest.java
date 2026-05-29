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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

class MCPToolFcliRunnerFunctionStreamingTest {
    @Test
    void buildExecutionArgsNodeExcludesPagingArguments() {
        var request = new CallToolRequest(
            "fcli_fn_sscAppListStream",
            Map.of(
                "pagination-offset", 20,
                "refresh-cache", true,
                "name", "demo"
            )
        );

        ObjectNode argsNode = MCPToolFcliRunnerFunctionStreaming.buildExecutionArgsNode(request);

        assertEquals("demo", argsNode.path("name").asText());
        assertFalse(argsNode.has("pagination-offset"));
        assertFalse(argsNode.has("refresh-cache"));
    }
}