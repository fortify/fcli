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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.json.JsonHelper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

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

    @Test
    void resultLogsEscapeUnicodeLineSeparatorsWithoutChangingOutput() throws Exception {
        var logger = (Logger)LoggerFactory.getLogger(MCPToolResult.class);
        var previousLevel = logger.getLevel();
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        try {
            var value = "text\u0085forged\u2028next\u2029end";
            var result = MCPToolResult.builder().exitCode(0).stdout(value).build().asCallToolResult();
            var returned = JsonHelper.getObjectMapper().readTree(((TextContent)result.content().get(0)).text());
            assertEquals(value, returned.path("stdout").asText());
            var message = appender.list.get(0).getFormattedMessage();
            assertFalse(message.contains("\u0085"));
            assertFalse(message.contains("\u2028"));
            assertFalse(message.contains("\u2029"));
            assertTrue(message.contains("\\u0085") && message.contains("\\u2028") && message.contains("\\u2029"));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
    }
}