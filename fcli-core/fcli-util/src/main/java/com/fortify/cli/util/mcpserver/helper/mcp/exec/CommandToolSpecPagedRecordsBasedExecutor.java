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
package com.fortify.cli.util.mcpserver.helper.mcp.exec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.util.mcpserver.helper.mcp.arg.CommandToolSpecArgHelper;
import com.fortify.cli.util.mcpserver.helper.mcp.arg.PagingToolSpecArgHelper;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Model.CommandSpec;

@RequiredArgsConstructor
public final class CommandToolSpecPagedRecordsBasedExecutor extends AbstractCommandToolSpecRecordsBasedExecutor {
    @Getter private final CommandToolSpecArgHelper toolSpecArgHelper;
    @Getter private final CommandSpec commandSpec;
    
    @Override
    protected CallToolResult execute(McpSyncServerExchange exchange, CallToolRequest request, String fullCmd) {
        var offset = asInt(request.arguments().get(PagingToolSpecArgHelper.ARG_OFFSET), 0);
        var limit = asInt(request.arguments().get(PagingToolSpecArgHelper.ARG_LIMIT), 20);
        var records = new ArrayList<JsonNode>();
        var fcliResult = collectRecords(fullCmd, records::add);
        int totalRecords = records.size();
        var totalPages = (int)Math.ceil((double)totalRecords / (double)limit);
        var lastPageOffset = (totalPages - 1) * limit;
        var nextPageOffset = offset+limit;
        var endIndex = Math.min(nextPageOffset, totalRecords);
        var hasMore = totalRecords>nextPageOffset;
        var pageInfo = PageInfo.builder()
            .currentLimit(limit)
            .currentOffset(offset)
            .lastPageOffset(lastPageOffset)
            .nextPageOffset(hasMore ? nextPageOffset : null)
            .hasMore(hasMore)
            .totalRecords(totalRecords)
            .totalPages(totalPages)
            .build();
        var result = PagedResult.builder()
            .pagination(pageInfo)
            .records(offset>endIndex ? Collections.emptyList() : records.subList(offset, endIndex))
            .build();
        return new CallToolResult(JsonHelper.getObjectMapper().valueToTree(result).toPrettyString(), fcliResult.getExitCode()!=0);
    }
    
    private static final int asInt(Object o, int defaultValue) {
        return o==null ? defaultValue : Integer.parseInt(o.toString());
    }

    @Data @Reflectable @Builder
    private static final class PagedResult {
        private final List<JsonNode> records;
        private final PageInfo pagination;
    }
    
    @Data @Reflectable @Builder
    private static final class PageInfo {
        private final int totalRecords;
        private final int totalPages;
        private final int currentOffset;
        private final int currentLimit;
        private final Integer nextPageOffset;
        private final int lastPageOffset;
        private final boolean hasMore;
    }
}