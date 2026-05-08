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
package com.fortify.cli.agent.mcp.helper.runner;

import java.util.List;
import java.util.Optional;

import com.fortify.cli.agent.mcp.helper.MCPJobManager;
import com.fortify.cli.agent.mcp.helper.arg.MCPToolArgHandlerPaging;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;

import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Shared paged-result logic for both command-based and function-based MCP tool runners.
 * Callers supply a {@link BackgroundStarter} that encapsulates how to start or resume
 * background record collection; this class owns the cache-check, wait, and result assembly.
 * <p>
 * In HTTP mode, paged/background execution must be isolated per request auth context.
 * This helper enforces that boundary by scoping semantic job IDs with the hashed auth
 * scope stored in the current {@link FcliExecutionContextHolder} context before any cache
 * or background-job lookup occurs.
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
final class MCPToolFcliPagedHelper {

    /**
     * Starts or resumes a background async job for the given job ID.
     * Returns {@code null} if the result is already completed (the caller should fall
     * back to a direct {@link MCPToolAsyncJobManager#getCached} lookup), or an
     * {@link MCPToolAsyncJobManager.InProgressEntry} when the job is still running.
     */
    @FunctionalInterface
    interface BackgroundStarter {
        MCPToolAsyncJobManager.InProgressEntry start(String jobId, boolean refresh);
    }

    /**
     * Immutable paging parameters extracted from a tool call request.
     */
    record PageParams(boolean refresh, int offset, int limit) {
        static PageParams from(CallToolRequest request) {
            var refresh = toolArgAsBoolean(request, MCPToolArgHandlerPaging.ARG_REFRESH, false);
            var offset  = toolArgAsInt(request, MCPToolArgHandlerPaging.ARG_OFFSET, 0);
            var limit   = 20; // Fixed page size
            return new PageParams(refresh, offset, limit);
        }
    }

    private final MCPJobManager jobManager;

    /**
     * Execute the paged-result flow. Checks the cache first, then falls back to starting or
     * resuming background collection via the supplied {@link BackgroundStarter}.
     */
    CallToolResult run(String jobId, PageParams pageParams, BackgroundStarter starter) {
        var scopedJobId = scopeJobId(jobId);
        try {
            return tryGetCachedResult(scopedJobId, pageParams)
                .or(() -> {
                    try {
                        return tryGetInProgressResult(scopedJobId, pageParams, starter);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting for records", e);
                    }
                })
                .orElseThrow(() -> new IllegalStateException("No result path succeeded for: " + scopedJobId));
        } catch (Exception e) {
            log.warn("Paged helper failed jobId='{}' offset={} limit={} error={}",
                scopedJobId, pageParams.offset, pageParams.limit, e.toString());
            return MCPToolResult.fromError(e).asCallToolResult();
        }
    }

    /**
     * Scope a semantic job ID by the current request auth context when present.
     *
     * In stateless HTTP MCP mode, two clients may invoke the same tool with identical
     * arguments but different credentials. Using an auth-scoped key prevents those
     * requests from sharing cached pages or background job state. Callers that interact
     * with paged/background async state must use the scoped key consistently.
     */
    static String scopeJobId(String jobId) {
        var authScopeKey = FcliExecutionContextHolder.getMcpRequestAuthScopeKey();
        return authScopeKey == null ? jobId : authScopeKey + "|" + jobId;
    }

    private Optional<CallToolResult> tryGetCachedResult(String jobId, PageParams params) {
        if (params.refresh) {
            return Optional.empty();
        }
        return Optional.ofNullable(jobManager.getAsyncJobManager().getCached(jobId))
            .map(cached -> {
                log.debug("Completed job hit jobId='{}' offset={} limit={} total={}",
                    jobId, params.offset, params.limit, cached.getRecords().size());
                return MCPToolResult.fromCompletedPagedResult(
                    cached,
                    params.offset,
                    params.limit,
                    jobManager.getAsyncJobManager().getJobToken(jobId)
                ).asCallToolResult();
            });
    }

    private Optional<CallToolResult> tryGetInProgressResult(
            String jobId, PageParams params, BackgroundStarter starter) throws InterruptedException {
        var inProgress = starter.start(jobId, params.refresh);
        if (inProgress == null) {
            return handleSyncJobCompleted(jobId, params);
        }
        waitForSufficientRecords(inProgress, params);
        return checkForCompletedWithError(inProgress, jobId)
            .or(() -> checkForCompletedSuccessfully(inProgress, jobId, params))
            .or(() -> buildPartialPageResult(inProgress, jobId, params));
    }

    private Optional<CallToolResult> handleSyncJobCompleted(String jobId, PageParams params) {
        return Optional.ofNullable(jobManager.getAsyncJobManager().getCached(jobId))
            .map(cached -> MCPToolResult.fromCompletedPagedResult(
                cached,
                params.offset,
                params.limit,
                jobManager.getAsyncJobManager().getJobToken(jobId)
            ).asCallToolResult())
            .or(() -> Optional.of(MCPToolResult.fromError("Async job completed but no completed result found").asCallToolResult()));
    }

    private static final long WAIT_TIMEOUT_MS = 300_000;

    private void waitForSufficientRecords(
            MCPToolAsyncJobManager.InProgressEntry inProgress, PageParams params) throws InterruptedException {
        var requiredCount = params.offset + params.limit + 1;
        var deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;
        while (inProgress.getLoadedCount() < requiredCount && !inProgress.isCompleted()
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
    }

    private Optional<CallToolResult> checkForCompletedWithError(
            MCPToolAsyncJobManager.InProgressEntry inProgress, String jobId) {
        if (inProgress.isCompleted() && inProgress.getExitCode() != 0) {
            log.warn("Background async job failed jobId='{}' exitCode={} stderr='{}'",
                jobId, inProgress.getExitCode(), inProgress.getStderr());
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
            MCPToolAsyncJobManager.InProgressEntry inProgress, String jobId, PageParams params) {
        if (!inProgress.isCompleted()) {
            return Optional.empty();
        }
        return Optional.ofNullable(jobManager.getAsyncJobManager().getCached(jobId))
            .map(cached -> {
                log.debug("Returning COMPLETE paged result jobId='{}' offset={} limit={} loaded={} total={}",
                    jobId, params.offset, params.limit, inProgress.getRecords().size(), cached.getRecords().size());
                return MCPToolResult.fromCompletedPagedResult(
                    cached,
                    params.offset,
                    params.limit,
                    jobManager.getAsyncJobManager().getJobToken(jobId)
                ).asCallToolResult();
            })
            .or(() -> {
                log.warn("Background async job completed without completed entry jobId='{}'", jobId);
                return Optional.empty();
            });
    }

    private Optional<CallToolResult> buildPartialPageResult(
            MCPToolAsyncJobManager.InProgressEntry inProgress, String jobId, PageParams params) {
        var requiredCount = params.offset + params.limit + 1;
        log.debug("Returning PARTIAL paged result jobId='{}' offset={} limit={} loaded={} need>={} jobToken={}",
            jobId, params.offset, params.limit, inProgress.getRecords().size(), requiredCount, inProgress.getJobToken());
        var result = MCPToolResult.fromPartialPagedResult(
            inProgress.getRecords(), params.offset, params.limit, false, inProgress.getJobToken());
        return Optional.of(result.asCallToolResult());
    }

    // --- Utility helpers ---

    static int toolArgAsInt(CallToolRequest request, String argName, int defaultValue) {
        var o = toolArg(request, argName);
        return o == null ? defaultValue : Integer.parseInt(o.toString());
    }

    static boolean toolArgAsBoolean(CallToolRequest request, String argName, boolean defaultValue) {
        var o = toolArg(request, argName);
        return o == null ? defaultValue : Boolean.parseBoolean(o.toString());
    }

    static Object toolArg(CallToolRequest request, String argName) {
        return request == null || request.arguments() == null ? null : request.arguments().get(argName);
    }
}
