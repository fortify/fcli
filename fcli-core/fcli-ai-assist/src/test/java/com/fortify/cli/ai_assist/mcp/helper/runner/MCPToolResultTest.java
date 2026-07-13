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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.json.JsonHelper;

class MCPToolResultTest {
    @Test
    void fromCompletedPagedResultPreservesJobToken() {
        var record = JsonHelper.getObjectMapper().createObjectNode().put("id", 1);
        var plainResult = MCPToolResult.builder()
            .exitCode(0)
            .stderr("")
            .records(List.of(record))
            .build();

        var result = MCPToolResult.fromCompletedPagedResult(plainResult, 0, 20, "job-123");

        assertEquals("job-123", result.getPagination().getJobToken());
    }
}