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
package com.fortify.cli.util._common.helper;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.exception.FcliExceptionHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.OutputHelper.Result;

import lombok.Builder;
import lombok.Data;

/**
 * Unified result class for fcli command execution. Supports multiple output formats:
 * plain text (stdout), structured records, paginated records, and errors.
 * Null fields are excluded from JSON serialization.
 * 
 * This class is shared between MCP server and RPC server implementations.
 * 
 * @author Ruud Senden
 */
@Data @Builder
@Reflectable
@JsonInclude(Include.NON_NULL)
public class FcliToolResult {
    private static final Logger LOG = LoggerFactory.getLogger(FcliToolResult.class);
    
    // Common fields for all result types
    private final Integer exitCode;
    private final String stderr;
    
    // Error fields (populated when exitCode != 0)
    private final String error;
    private final String errorStackTrace;
    private final String errorGuidance;
    
    // Plain text output
    private final String stdout;
    
    // Structured records output
    private final List<JsonNode> records;
    
    // Pagination metadata (for paged results)
    private final PageInfo pagination;
    
    // Factory methods
    
    /**
     * Create result from fcli execution with plain text stdout.
     */
    public static FcliToolResult fromPlainText(Result result) {
        return builder()
            .exitCode(result.getExitCode())
            .stderr(result.getErr())
            .stdout(result.getOut())
            .build();
    }
    
    /**
     * Create result from fcli execution with structured records.
     */
    public static FcliToolResult fromRecords(Result result, List<JsonNode> records) {
        return builder()
            .exitCode(result.getExitCode())
            .stderr(result.getErr())
            .records(records)
            .build();
    }
    
    /**
     * Create complete paged result once all records have been collected.
     */
    public static FcliToolResult fromCompletedPagedResult(FcliToolResult plainResult, int offset, int limit) {
        var allRecords = plainResult.getRecords();
        var pageInfo = PageInfo.complete(allRecords.size(), offset, limit);
        var endIndexExclusive = Math.min(offset+limit, allRecords.size());
        List<JsonNode> pageRecords = offset>=endIndexExclusive ? List.<JsonNode>of() : allRecords.subList(offset, endIndexExclusive);
        return builder()
            .exitCode(plainResult.getExitCode())
            .stderr(plainResult.getStderr())
            .error(plainResult.getError())
            .errorStackTrace(plainResult.getErrorStackTrace())
            .errorGuidance(plainResult.getErrorGuidance())
            .records(pageRecords)
            .pagination(pageInfo)
            .build();
    }
    
    /**
     * Create partial paged result while background collection is still running.
     */
    public static FcliToolResult fromPartialPagedResult(List<JsonNode> loadedRecords, int offset, int limit, boolean complete, String cacheKey) {
        if ( complete ) {
            return fromCompletedPagedResult(
                    builder().exitCode(0).stderr("").records(loadedRecords).build(),
                    offset, limit);
        }
        var endIndexExclusive = Math.min(offset+limit, loadedRecords.size());
        List<JsonNode> pageRecords = offset>=endIndexExclusive ? List.<JsonNode>of() : loadedRecords.subList(offset, endIndexExclusive);
        var hasMore = loadedRecords.size() > offset+limit;
        var pageInfo = PageInfo.partial(offset, limit, hasMore).toBuilder().cacheKey(cacheKey).build();
        return builder()
            .exitCode(0)
            .stderr("")
            .records(pageRecords)
            .pagination(pageInfo)
            .build();
    }
    
    /**
     * Create error result from exit code and stderr message.
     */
    public static FcliToolResult fromError(int exitCode, String stderr) {
        return builder()
            .exitCode(exitCode)
            .stderr(stderr != null ? stderr : "Unknown error")
            .records(List.<JsonNode>of())
            .build();
    }
    
    /**
     * Create error result from exception with structured error information.
     */
    public static FcliToolResult fromError(Exception e) {
        return builder()
            .exitCode(1)
            .stderr(getErrorMessage(e))
            .error(getErrorMessage(e))
            .errorStackTrace(formatException(e))
            .errorGuidance(getErrorGuidance())
            .records(List.<JsonNode>of())
            .build();
    }
    
    /**
     * Create error result with simple message.
     */
    public static FcliToolResult fromError(String message) {
        return fromError(1, message);
    }
    
    // Conversion to JSON
    
    public final String asJsonString() {
        return JsonHelper.getObjectMapper().valueToTree(this).toPrettyString();
    }
    
    public final JsonNode asJsonNode() {
        return JsonHelper.getObjectMapper().valueToTree(this);
    }
    
    // Pagination metadata inner class
    
    @Data @Builder(toBuilder = true)
    @Reflectable
    public static final class PageInfo {
        private final Integer totalRecords;
        private final Integer totalPages;
        private final int currentOffset;
        private final int currentLimit;
        private final Integer nextPageOffset;
        private final Integer lastPageOffset;
        private final boolean hasMore;
        private final boolean complete;
        private final String cacheKey;  // For RPC: reference to cached result
        private final String jobToken;  // For MCP: reference to job tracking
        private final String guidance;
        
        public static PageInfo complete(int totalRecords, int offset, int limit) {
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
                .complete(true)
                .guidance("All records loaded; totals available.")
                .build();
        }
        
        public static PageInfo partial(int offset, int limit, boolean hasMore) {
            return PageInfo.builder()
                .currentLimit(limit)
                .currentOffset(offset)
                .nextPageOffset(hasMore ? offset+limit : null)
                .hasMore(hasMore)
                .complete(false)
                .guidance("Partial page; totals unavailable. Use cacheKey/jobToken to wait for completion.")
                .build();
        }
        
        @JsonIgnore
        public boolean isComplete() {
            return complete;
        }
    }
    
    // Exception formatting helpers
    
    private static String formatException(Exception e) {
        return FcliExceptionHelper.formatException(e);
    }
    
    private static String getErrorMessage(Exception e) {
        return FcliExceptionHelper.getErrorMessage(e);
    }
    
    private static String getErrorGuidance() {
        return """
            The fcli command failed with an exception. You may use the error message and stack trace to:
            1. Diagnose the root cause and suggest corrective actions to resolve the issue
            2. Provide the error details to the user if manual troubleshooting is required
            3. Adjust command parameters or suggest alternative approaches to accomplish the task
            """;
    }
}
