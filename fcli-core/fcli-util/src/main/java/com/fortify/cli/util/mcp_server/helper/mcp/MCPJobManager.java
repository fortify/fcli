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
package com.fortify.cli.util.mcp_server.helper.mcp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.util.mcp_server.helper.mcp.runner.MCPToolFcliRecordsCache;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.ProgressNotification;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import lombok.extern.slf4j.Slf4j;

/**
 * Minimal asynchronous job manager for MCP tools. This implementation:
 * - Wraps synchronous tool execution in a worker thread
 * - Returns an in_progress result if execution exceeds the safe-return threshold
 * - Exposes a unified job tool (fcli_<module>_mcp_job) with status|wait|cancel operations
 * - Tracks simple progress either through a record counter or ticking strategy
 * - Best-effort cancellation via thread interrupt
 *
 * NOTE: Progress notifications are not currently emitted; clients poll the job tool.
 */
@Slf4j
public class MCPJobManager {
    private final String moduleName;
    private final ExecutorService workExecutor;
    private final ScheduledExecutorService progressExecutor;
    private final long safeReturnMillis;
    private final long progressIntervalMillis;
    private final Map<String, JobExecution> jobs = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final MCPToolFcliRecordsCache recordsCache;

    public MCPJobManager(String moduleName, int workThreads, int progressThreads, long safeReturnMillis, long progressIntervalMillis) {
        this.moduleName = moduleName;
        this.workExecutor = Executors.newFixedThreadPool(workThreads);
        this.progressExecutor = Executors.newScheduledThreadPool(progressThreads);
        this.safeReturnMillis = safeReturnMillis;
        this.progressIntervalMillis = progressIntervalMillis;
        this.recordsCache = new MCPToolFcliRecordsCache(this);
        log.info("Initialized MCPJobManager for module={} workThreads={} progressThreads={} safeReturnMillis={} progressIntervalMillis={}",
                moduleName, workThreads, progressThreads, safeReturnMillis, progressIntervalMillis);
    }

    public MCPToolFcliRecordsCache getRecordsCache() {
        return recordsCache;
    }

    // Public API for runners
    public CallToolResult execute(McpSyncServerExchange exchange, String toolName, Callable<CallToolResult> work, ProgressStrategy progressStrategy, boolean sendNotifications) {
        JobExecution exec = createAndQueueJob(toolName, progressStrategy);
        if ( sendNotifications ) {
            sendProgressNotification(exchange, exec, false);
        }
        
        CompletableFuture<CallToolResult> future = startJobExecution(exchange, exec, work, sendNotifications);
        scheduleProgressUpdates(exchange, exec, progressStrategy, sendNotifications);
        
        return waitForCompletionOrReturnInProgress(exec, future);
    }
    
    private JobExecution createAndQueueJob(String toolName, ProgressStrategy progressStrategy) {
        String token = UUID.randomUUID().toString();
        JobExecution exec = new JobExecution(token, toolName, progressStrategy);
        jobs.put(token, exec);
        log.info("Queued job {} for tool {}", token, toolName);
        return exec;
    }
    
    private CompletableFuture<CallToolResult> startJobExecution(McpSyncServerExchange exchange, JobExecution exec, Callable<CallToolResult> work, boolean sendNotifications) {
        CompletableFuture<CallToolResult> future = CompletableFuture.supplyAsync(() -> executeWork(exchange, exec, work, sendNotifications), workExecutor)
            .whenComplete((res, t) -> handleJobCompletion(exchange, exec, res, t, sendNotifications));
        exec.future = future;
        return future;
    }
    
    private CallToolResult executeWork(McpSyncServerExchange exchange, JobExecution exec, Callable<CallToolResult> work, boolean sendNotifications) {
        exec.status = JobStatus.RUNNING;
        exec.startTime = Instant.now();
        if ( sendNotifications ) {
            sendProgressNotification(exchange, exec, false);
        }
        log.info("Started job {} for tool {}", exec.token, exec.toolName);
        
        try {
            return work.call();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            exec.status = JobStatus.CANCELLED;
            return buildMessageResult("cancelled", exec.token, exec.toolName, "Interrupted");
        } catch (Exception e) {
            exec.status = JobStatus.FAILED;
            return buildErrorResult(exec.token, exec.toolName, e.getMessage());
        }
    }
    
    private void handleJobCompletion(McpSyncServerExchange exchange, JobExecution exec, CallToolResult res, Throwable t, boolean sendNotifications) {
        if ( t != null ) {
            exec.status = JobStatus.FAILED;
            exec.result = buildErrorResult(exec.token, exec.toolName, t.getMessage());
        } else {
            exec.status = exec.status==JobStatus.CANCELLED?exec.status:JobStatus.COMPLETED;
            exec.result = res;
        }
        exec.endTime = Instant.now();
        if ( sendNotifications ) {
            sendProgressNotification(exchange, exec, true);
        }
        log.info("Finished job {} for tool {} with status {}", exec.token, exec.toolName, exec.status);
    }
    
    private void scheduleProgressUpdates(McpSyncServerExchange exchange, JobExecution exec, ProgressStrategy progressStrategy, boolean sendNotifications) {
        if ( !sendNotifications ) {
            return;
        }
        
        exec.progressTask = progressExecutor.scheduleAtFixedRate(() -> {
            updateProgress(exchange, exec, progressStrategy);
        }, progressIntervalMillis, progressIntervalMillis, TimeUnit.MILLISECONDS);
    }
    
    private void updateProgress(McpSyncServerExchange exchange, JobExecution exec, ProgressStrategy progressStrategy) {
        try {
            exec.progress.set(progressStrategy.updateAndGetProgress());
        } catch (Exception e) {
            log.trace("Progress strategy error: {}", e.toString());
        }
        
        if ( exec.status==JobStatus.RUNNING ) {
            log.debug("Periodic progress tick for job {} progress={}", exec.token, exec.progress.get());
            sendProgressNotification(exchange, exec, false);
        }
    }
    
    private CallToolResult waitForCompletionOrReturnInProgress(JobExecution exec, CompletableFuture<CallToolResult> future) {
        long start = System.currentTimeMillis();
        while ( System.currentTimeMillis()-start < safeReturnMillis ) {
            if ( future.isDone() ) {
                exec.cleanup();
                jobs.remove(exec.token);
                return exec.result!=null?exec.result:buildErrorResult(exec.token, exec.toolName, "No result");
            }
            sleep(100);
        }
        
        return buildInProgressResult(exec);
    }
    
    private CallToolResult buildInProgressResult(JobExecution exec) {
        exec.status = JobStatus.RUNNING;
        ObjectNode n = mapper.createObjectNode();
        n.put("status", "in_progress");
        n.put("job_token", exec.token);
        n.put("tool", exec.toolName);
        n.put("progress", exec.progress.get());
        n.put("message", "Operation still running; call fcli_"+moduleName+"_mcp_job for status|wait|cancel");
        return new CallToolResult(n.toPrettyString(), false);
    }

    /**
     * Track an existing future (background work started elsewhere) as a job so the
     * fcli_<module>_mcp_job tool can report status|wait|cancel. The provided progressStrategy
     * is sampled periodically for progress updates. Completion of the future sets a final
     * tool_result summary; cancellation interrupts progress sampling and marks the job cancelled.
     *
     * Returns job token immediately; does not wait for safeReturnMillis.
     */
    public String trackFuture(String toolName, CompletableFuture<?> future, ProgressStrategy progressStrategy) {
        JobExecution exec = createRunningJob(toolName, progressStrategy);
        scheduleProgressSampling(exec, progressStrategy);
        attachFutureCompletionHandler(exec, future);
        return exec.token;
    }
    
    private JobExecution createRunningJob(String toolName, ProgressStrategy progressStrategy) {
        String token = UUID.randomUUID().toString();
        JobExecution exec = new JobExecution(token, toolName, progressStrategy);
        jobs.put(token, exec);
        exec.status = JobStatus.RUNNING;
        exec.startTime = Instant.now();
        log.info("Tracking external future as job {} tool {}", token, toolName);
        return exec;
    }
    
    private void scheduleProgressSampling(JobExecution exec, ProgressStrategy progressStrategy) {
        exec.progressTask = progressExecutor.scheduleAtFixedRate(() -> {
            try {
                exec.progress.set(progressStrategy.updateAndGetProgress());
            } catch ( Exception e ) {
                log.trace("Progress strategy error: {}", e.toString());
            }
        }, progressIntervalMillis, progressIntervalMillis, TimeUnit.MILLISECONDS);
    }
    
    private void attachFutureCompletionHandler(JobExecution exec, CompletableFuture<?> future) {
        future.whenComplete((res, t) -> {
            finalizeFutureJob(exec, t);
            exec.endTime = Instant.now();
            exec.cleanup();
            log.info("Tracked future finished job {} status {}", exec.token, exec.status);
        });
    }
    
    private void finalizeFutureJob(JobExecution exec, Throwable t) {
        if ( t != null ) {
            exec.status = exec.cancelled.get()?JobStatus.CANCELLED:JobStatus.FAILED;
            exec.result = buildErrorResult(exec.token, exec.toolName, t.getMessage());
        } else {
            exec.status = exec.cancelled.get()?JobStatus.CANCELLED:JobStatus.COMPLETED;
            exec.result = buildFutureCompletionResult(exec);
        }
    }
    
    private CallToolResult buildFutureCompletionResult(JobExecution exec) {
        ObjectNode n = mapper.createObjectNode();
        n.put("status", exec.status.name().toLowerCase());
        n.put("job_token", exec.token);
        n.put("tool", exec.toolName);
        n.put("message", exec.status==JobStatus.COMPLETED?"background collection completed":"background collection ended");
        return new CallToolResult(n.toPrettyString(), false);
    }

    public SyncToolSpecification getJobToolSpecification() {
        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(Tool.builder()
                .name("fcli_"+moduleName+"_mcp_job")
                .description("Manage long-running fcli MCP jobs (status|wait|cancel)")
                .inputSchema(createJobToolSchema())
                .build())
            .callHandler((exchange, request) -> {
                var args = request==null?null:request.arguments();
                String op = stringArg(args, "operation");
                String token = stringArg(args, "job_token");
                return handleJobOperation(token, op);
            })
            .build();
    }

    private JsonSchema createJobToolSchema() {
        // Build a minimal JSON schema describing accepted arguments. Both fields are required.
        // operation: enum status|wait|cancel
        // job_token: string identifying the job
        var properties = new LinkedHashMap<String,Object>();
        var required = new ArrayList<String>();
        var opSchema = new LinkedHashMap<String,Object>();
        opSchema.put("type", "string");
        opSchema.put("enum", List.of("status","wait","cancel"));
        properties.put("operation", opSchema);
        required.add("operation");
        var tokenSchema = new LinkedHashMap<String,Object>();
        tokenSchema.put("type", "string");
        properties.put("job_token", tokenSchema);
        required.add("job_token");
        return new JsonSchema("object", properties, required, false, new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    private CallToolResult handleJobOperation(String token, String op) {
        if ( token==null || op==null ) {
            return new CallToolResult("Missing job_token or operation", true);
        }
        JobExecution exec = jobs.get(token);
        try {
            return JobOperation.valueOf(op.toUpperCase()).apply(this, exec, token);
        } catch ( IllegalArgumentException e ) {
            return new CallToolResult("Invalid operation: "+op, true);
        }
    }

    private CallToolResult status(JobExecution exec, String token) {
        if ( exec==null ) {
            return new CallToolResult(json(mapper.createObjectNode().put("job_token", token).put("status", "not_found")), false);
        }
        var n = mapper.createObjectNode();
        n.put("job_token", token);
        n.put("tool", exec.toolName);
        n.put("status", exec.status.name().toLowerCase());
        n.put("progress", exec.progress.get());
        n.put("queue_seconds", exec.getQueueSeconds());
        n.put("execution_seconds", exec.getExecutionSeconds());
        if ( exec.status==JobStatus.COMPLETED || exec.status==JobStatus.FAILED || exec.status==JobStatus.CANCELLED ) {
            n.put("final", true);
            if ( exec.result!=null ) {
                addFinalResult(n, exec.result);
            }
        }
        return new CallToolResult(n.toPrettyString(), false);
    }

    private CallToolResult wait(JobExecution exec, String token) {
        if ( exec==null ) {
            return status(null, token);
        }
        long start = System.currentTimeMillis();
        while ( System.currentTimeMillis()-start < safeReturnMillis ) {
            if ( exec.future!=null && exec.future.isDone() ) {
                break;
            }
            sleep(150);
        }
        if ( exec.future!=null && exec.future.isDone() ) {
            exec.cleanup();
            jobs.remove(token);
        }
        return status(exec, token);
    }

    private CallToolResult cancel(JobExecution exec, String token) {
        if ( exec==null ) {
            return status(null, token);
        }
        exec.cancelled.set(true);
        if ( exec.future!=null ) {
            exec.future.cancel(true);
        }
        exec.status = JobStatus.CANCELLED;
        exec.endTime = Instant.now();
        sendProgressNotification(null, exec, true);
        return status(exec, token);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static String json(ObjectNode n) {
        return n.toPrettyString();
    }
    
    private static String stringArg(Map<String,Object> args, String name) {
        if ( args==null ) {
            return null;
        }
        var o=args.get(name);
        return o==null?null:o.toString();
    }

    // Progress strategy: either record counter or ticking progress
    public interface ProgressStrategy {
        int updateAndGetProgress();
    }
    
    public static ProgressStrategy recordCounter(AtomicInteger counter) {
        return () -> counter.get();
    }
    
    public static ProgressStrategy ticking(AtomicInteger counter) {
        return () -> counter.incrementAndGet();
    }

    private enum JobStatus { QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED }

    private static final class JobExecution {
        final String token;
        final String toolName;
    @SuppressWarnings("unused") final ProgressStrategy progressStrategy; // retained for potential future enhancements
        final AtomicInteger progress = new AtomicInteger(0);
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        volatile JobStatus status = JobStatus.QUEUED;
        volatile Instant creationTime = Instant.now();
        volatile Instant startTime;
        volatile Instant endTime;
        volatile CompletableFuture<CallToolResult> future;
        volatile CallToolResult result;
    volatile ScheduledFuture<?> progressTask;
        volatile Integer total; // Optional total count for progress
        
        JobExecution(String token, String toolName, ProgressStrategy progressStrategy) {
            this.token = token;
            this.toolName = toolName;
            this.progressStrategy = progressStrategy;
        }
        
        void cleanup() {
            if ( progressTask!=null ) {
                progressTask.cancel(false);
            }
        }
        
        long getQueueSeconds() {
            if ( startTime==null ) {
                return Math.max(0, (Instant.now().getEpochSecond()-creationTime.getEpochSecond()));
            }
            return Math.max(0, (startTime.getEpochSecond()-creationTime.getEpochSecond()));
        }
        
        long getExecutionSeconds() {
            if ( startTime==null ) {
                return 0;
            }
            Instant effectiveEnd = endTime!=null?endTime:Instant.now();
            return Math.max(0, (effectiveEnd.getEpochSecond()-startTime.getEpochSecond()));
        }
    }

    private CallToolResult buildErrorResult(String token, String tool, String message) {
        ObjectNode n = mapper.createObjectNode();
        n.put("status", "failed");
        n.put("job_token", token);
        n.put("tool", tool);
        n.put("error", message==null?"Unknown":message);
        return new CallToolResult(n.toPrettyString(), true);
    }
    
    private CallToolResult buildMessageResult(String status, String token, String tool, String msg) {
        ObjectNode n = mapper.createObjectNode();
        n.put("status", status);
        n.put("job_token", token);
        n.put("tool", tool);
        n.put("message", msg);
        return new CallToolResult(n.toPrettyString(), false);
    }

    private void addFinalResult(ObjectNode target, CallToolResult result) {
        String content = null;
        try {
            content = (String)result.getClass().getMethod("content").invoke(result);
        } catch ( Exception e ) {
            content = result.toString();
        }
        if ( content==null ) {
            return;
        }
        // Try to parse as JSON, fall back to raw string
        try {
            var jsonNode = mapper.readTree(content);
            target.set("tool_result", jsonNode);
        } catch ( Exception e ) {
            target.put("tool_result_raw", content);
        }
        // Provide simple summary fields to help LLMs
        target.put("tool_result_error", isError(result));
    }

    private boolean isError(CallToolResult result) {
        try {
            return (Boolean)result.getClass().getMethod("isError").invoke(result);
        } catch ( Exception e ) {
            return false;
        }
    }

    private void sendProgressNotification(McpSyncServerExchange exchange, JobExecution exec, boolean finalNotification) {
        if ( exchange==null ) {
            return;
        }
        try {
            if ( finalNotification && exec.status==JobStatus.COMPLETED && exec.total==null ) {
                exec.total = exec.progress.get();
            }
            double progress = exec.progress.get();
            Double total = exec.total==null?null:exec.total.doubleValue();
            String message = buildStatusMessage(exec, finalNotification, progress, total);
            var pn = new ProgressNotification(exec.token, progress, total, message);
            log.debug("Progress notification job={} status={} progress={} total={}", exec.token, exec.status, progress, total);
            exchange.progressNotification(pn);
        } catch ( Exception e ) {
            log.trace("Couldn't emit progress notification: {}", e.toString());
        }
    }

    private String buildStatusMessage(JobExecution exec, boolean finalNotification, double progress, Double total) {
        var status = exec.status.name().toLowerCase();
        long queueSecs = exec.getQueueSeconds();
        long execSecs = exec.getExecutionSeconds();
        var root = mapper.createObjectNode();
        root.put("status", status);
        root.put("tool", exec.toolName);
        root.put("progress", progress);
        if ( total!=null ) {
            root.put("total", total);
        }
        root.put("queue_seconds", queueSecs);
        root.put("execution_seconds", execSecs);
        root.put("final", finalNotification);
        switch ( exec.status ) {
            case COMPLETED -> root.put("message", String.format("completed in %ds", execSecs));
            case FAILED -> root.put("message", String.format("failed after %ds", execSecs));
            case CANCELLED -> root.put("message", String.format("cancelled after %ds", execSecs));
            case RUNNING -> root.put("message", String.format("running (%ds)", execSecs));
            case QUEUED -> root.put("message", String.format("queued (%ds)", queueSecs));
            default -> root.put("message", status);
        }
        return root.toPrettyString();
    }

    private static enum JobOperation {
        STATUS {
            @Override
            CallToolResult apply(MCPJobManager mgr, JobExecution exec, String token) {
                return mgr.status(exec, token);
            }
        },
        WAIT {
            @Override
            CallToolResult apply(MCPJobManager mgr, JobExecution exec, String token) {
                return mgr.wait(exec, token);
            }
        },
        CANCEL {
            @Override
            CallToolResult apply(MCPJobManager mgr, JobExecution exec, String token) {
                return mgr.cancel(exec, token);
            }
        };
        
        abstract CallToolResult apply(MCPJobManager mgr, JobExecution exec, String token);
    }
}