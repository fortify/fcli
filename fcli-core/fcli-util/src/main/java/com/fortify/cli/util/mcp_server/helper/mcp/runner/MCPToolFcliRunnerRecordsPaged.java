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

import java.util.List;
import java.util.Optional;

import com.fortify.cli.util.mcp_server.helper.mcp.MCPJobManager;
import com.fortify.cli.util.mcp_server.helper.mcp.arg.MCPToolArgHandlerPaging;
import com.fortify.cli.util.mcp_server.helper.mcp.arg.MCPToolArgHandlers;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Model.CommandSpec;

/**
 * {@link IMCPToolRunner} implementation that, given offset and limit, returns the requested set of 
 * records together with pagination data in a structured JSON object.
 * This is commonly used to run fcli commands that may return a large number of records.
 *
 * @author Ruud Senden
 */
@Slf4j
public final class MCPToolFcliRunnerRecordsPaged extends AbstractMCPToolFcliRunner {
    @Getter private final MCPToolArgHandlers toolSpecArgHelper;
    @Getter private final CommandSpec commandSpec;
    public MCPToolFcliRunnerRecordsPaged(MCPToolArgHandlers toolSpecArgHelper, CommandSpec commandSpec, MCPJobManager jobManager) {
        super(jobManager);
        this.toolSpecArgHelper = toolSpecArgHelper;
        this.commandSpec = commandSpec;
    }

    @Override
    public CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request) {
        final var fullCmd = getFullCmd(request);
        final var pageParams = PageParams.from(request);
        
        try {
            return tryGetCachedResult(fullCmd, pageParams)
                .or(() -> {
                    try {
                        return tryGetInProgressResult(fullCmd, pageParams);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting for records", e);
                    }
                })
                .orElseThrow(() -> new IllegalStateException("No result path succeeded for: " + fullCmd));
        } catch (Exception e) {
            log.warn("Paged runner failed cmd='{}' offset={} limit={} error={}", 
                fullCmd, pageParams.offset, pageParams.limit, e.toString());
            return MCPToolResult.fromError(e).asCallToolResult();
        }
    }
    
    private Optional<CallToolResult> tryGetCachedResult(String fullCmd, PageParams params) {
        if (params.refresh) {
            return Optional.empty();
        }
        return Optional.ofNullable(jobManager.getRecordsCache().getCached(fullCmd))
            .map(cached -> {
                log.debug("Cache hit cmd='{}' offset={} limit={} total={}", 
                    fullCmd, params.offset, params.limit, cached.getRecords().size());
                return MCPToolResult.fromCompletedPagedResult(cached, params.offset, params.limit).asCallToolResult();
            });
    }
    
    private Optional<CallToolResult> tryGetInProgressResult(String fullCmd, PageParams params) 
            throws InterruptedException {
        var inProgress = jobManager.getRecordsCache().getOrStartBackground(fullCmd, params.refresh, getCommandSpec());
        
        if (inProgress == null) {
            return handleSyncCollectionCompleted(fullCmd, params);
        }
        
        waitForSufficientRecords(inProgress, params);
        
        return checkForCompletedWithError(inProgress, fullCmd)
            .or(() -> checkForCompletedSuccessfully(inProgress, fullCmd, params))
            .or(() -> buildPartialPageResult(inProgress, fullCmd, params));
    }
    
    private Optional<CallToolResult> handleSyncCollectionCompleted(String fullCmd, PageParams params) {
        return Optional.ofNullable(jobManager.getRecordsCache().getCached(fullCmd))
            .map(cached -> MCPToolResult.fromCompletedPagedResult(cached, params.offset, params.limit).asCallToolResult())
            .or(() -> Optional.of(MCPToolResult.fromError("Collection completed but no cached result found").asCallToolResult()));
    }
    
    private void waitForSufficientRecords(MCPToolFcliRecordsCache.InProgressEntry inProgress, PageParams params) 
            throws InterruptedException {
        var records = inProgress.getRecords();
        var requiredCount = params.offset + params.limit + 1;
        
        while (records.size() < requiredCount && !inProgress.isCompleted()) {
            Thread.sleep(50);
        }
    }
    
    private Optional<CallToolResult> checkForCompletedWithError(
            MCPToolFcliRecordsCache.InProgressEntry inProgress, String fullCmd) {
        if (inProgress.isCompleted() && inProgress.getExitCode() != 0) {
            log.warn("Background collection failed cmd='{}' exitCode={} stderr='{}'", 
                fullCmd, inProgress.getExitCode(), inProgress.getStderr());
            var errorResult = MCPToolResult.builder()
                .exitCode(inProgress.getExitCode())
                .stderr(inProgress.getStderr())
                .records(List.of())
                .build();
            return Optional.of(errorResult.asCallToolResult());
        }
        return Optional.empty();
    }
    
    private Optional<CallToolResult> checkForCompletedSuccessfully(
            MCPToolFcliRecordsCache.InProgressEntry inProgress, String fullCmd, PageParams params) {
        if (!inProgress.isCompleted()) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(jobManager.getRecordsCache().getCached(fullCmd))
            .map(cached -> {
                log.debug("Returning COMPLETE paged result cmd='{}' offset={} limit={} loaded={} total={}",
                    fullCmd, params.offset, params.limit, inProgress.getRecords().size(), cached.getRecords().size());
                return MCPToolResult.fromCompletedPagedResult(cached, params.offset, params.limit).asCallToolResult();
            })
            .or(() -> {
                log.warn("Background collection completed without cache entry cmd='{}'", fullCmd);
                return Optional.empty();
            });
    }
    
    private Optional<CallToolResult> buildPartialPageResult(
            MCPToolFcliRecordsCache.InProgressEntry inProgress, String fullCmd, PageParams params) {
        var requiredCount = params.offset + params.limit + 1;
        log.debug("Returning PARTIAL paged result cmd='{}' offset={} limit={} loaded={} need>={} jobToken={}",
            fullCmd, params.offset, params.limit, inProgress.getRecords().size(), requiredCount, inProgress.getJobToken());
        
        var result = MCPToolResult.fromPartialPagedResult(
            inProgress.getRecords(), 
            params.offset, 
            params.limit, 
            false, 
            inProgress.getJobToken()
        );
        return Optional.of(result.asCallToolResult());
    }
    
    private static final int toolArgAsInt(CallToolRequest request, String argName, int defaultValue) {
        var o = toolArg(request, argName);
        return o==null ? defaultValue : Integer.parseInt(o.toString());
    }
    
    private static final boolean toolArgAsBoolean(CallToolRequest request, String argName, boolean defaultValue) {
        var o = toolArg(request, argName);
        return o==null ? defaultValue : Boolean.parseBoolean(o.toString());
    }

    private static Object toolArg(CallToolRequest request, String argName) {
        return request==null || request.arguments()==null ? null : request.arguments().get(argName);
    }
    
    /**
     * Immutable parameter object encapsulating pagination request parameters.
     * Using a record provides:
     * - Immutability by default
     * - Clear grouping of related parameters
     * - Automatic equals/hashCode/toString
     * - Reduced parameter passing (1 object vs 3 primitives)
     */
    private record PageParams(boolean refresh, int offset, int limit) {
        static PageParams from(CallToolRequest request) {
            var refresh = toolArgAsBoolean(request, MCPToolArgHandlerPaging.ARG_REFRESH, false);
            var offset = toolArgAsInt(request, MCPToolArgHandlerPaging.ARG_OFFSET, 0);
            var limit = 20; // Fixed for now
            return new PageParams(refresh, offset, limit);
        }
    }
}