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

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data class representing the output of the {@link MCPToolFcliRunnerRecordsPaged},
 * storing records, pagination info, stderr, and exit code.
 * 
 * @author Ruud Senden
 */
@Data @EqualsAndHashCode(callSuper = false) @Builder
@Reflectable
public class MCPToolResultRecordsPaged extends AbstractMCPToolResult {
    private final List<JsonNode> records;
    private final PageInfo pagination;
    private final String stderr;
    private final int exitCode;
    
    public static final MCPToolResultRecordsPaged from(MCPToolResultRecords plainResult, int offset, int limit) {
        var allRecords = plainResult.getRecords();
        var pageInfo = PageInfo.from(allRecords.size(), offset, limit);
        var endIndex = Math.min(pageInfo.getNextPageOffsetOrMaxInt(), pageInfo.getTotalRecords());
        return builder()
            .exitCode(plainResult.getExitCode())
            .stderr(plainResult.getStderr())
            .records(offset>endIndex ? Collections.emptyList() : allRecords.subList(offset, endIndex))
            .pagination(pageInfo)
            .build();
    }
    
    @Data @Builder
    @Reflectable
    private static final class PageInfo {
        private final int totalRecords;
        private final int totalPages;
        private final int currentOffset;
        private final int currentLimit;
        private final Integer nextPageOffset;
        private final int lastPageOffset;
        private final boolean hasMore;
        
        private static final PageInfo from(int totalRecords, int offset, int limit) {
            var totalPages = (int)Math.ceil((double)totalRecords / (double)limit);
            var lastPageOffset = (totalPages - 1) * limit;
            var nextPageOffset = offset+limit;
            var hasMore = totalRecords>nextPageOffset;
            return PageInfo.builder()
                .currentLimit(limit)
                .currentOffset(offset)
                .lastPageOffset(lastPageOffset)
                .nextPageOffset(hasMore ? nextPageOffset : null)
                .hasMore(hasMore)
                .totalRecords(totalRecords)
                .totalPages(totalPages)
                .build();
        }
        
        @JsonIgnore
        public final int getNextPageOffsetOrMaxInt() {
            return nextPageOffset==null ? Integer.MAX_VALUE : nextPageOffset;
        }
    }
}
