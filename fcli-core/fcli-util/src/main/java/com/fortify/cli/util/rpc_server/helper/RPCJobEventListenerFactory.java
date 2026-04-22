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
package com.fortify.cli.util.rpc_server.helper;

import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.util._common.helper.CachingJobEventListener;
import com.fortify.cli.util._common.helper.CompositeJobEventListener;
import com.fortify.cli.util._common.helper.IJobEventListener;

import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating per-job {@link IJobEventListener} instances in the RPC server.
 * Handles parsing of the {@code cache} and {@code push} parameters and composing
 * push and caching listeners.
 *
 * <p>Cache behavior:
 * <ul>
 *   <li>No {@code cache} param: no caching (records only available via push notifications)</li>
 *   <li>{@code cache: {ttl: "5m"}}: cache with specified TTL (e.g., "30s", "5m", "1h", "1d")</li>
 * </ul>
 *
 * <p>Push behavior:
 * <ul>
 *   <li>No {@code push} param or {@code push: true} (default): push notifications enabled</li>
 *   <li>{@code push: false}: push notifications disabled</li>
 * </ul>
 *
 * @author Ruud Senden
 */
@Slf4j
final class RPCJobEventListenerFactory {
    private static final DateTimePeriodHelper PERIOD_HELPER =
            DateTimePeriodHelper.byRange(Period.SECONDS, Period.DAYS);

    private final CachingJobEventListener cachingListener;
    private volatile RPCServer.RPCOutputWriter outputWriter;
    private volatile RPCPushJobEventListener pushListener;

    RPCJobEventListenerFactory(CachingJobEventListener cachingListener) {
        this.cachingListener = cachingListener;
    }

    void setOutputWriter(RPCServer.RPCOutputWriter writer) {
        this.outputWriter = writer;
        // Reset push listener so it's recreated with the new writer
        this.pushListener = null;
    }

    /**
     * Cache configuration parsed from the {@code cache} request parameter.
     * TTL must always be explicitly specified.
     */
    record CacheConfig(Duration ttl) {}

    /**
     * Parse the {@code cache} parameter from RPC request params.
     *
     * @return {@code null} if no caching requested, or a {@link CacheConfig} with the
     *         specified TTL for {@code cache: {ttl: "10m"}}
     * @throws RPCMethodException if cache is specified without a ttl
     */
    static CacheConfig parseCacheParam(JsonNode params) throws RPCMethodException {
        if (params == null || !params.has("cache")) {
            return null;
        }
        var cacheNode = params.get("cache");
        if (cacheNode instanceof ObjectNode obj && obj.has("ttl")) {
            var millis = PERIOD_HELPER.parsePeriodToEpochMillis(obj.get("ttl").asText());
            return new CacheConfig(Duration.ofMillis(millis));
        }
        throw RPCMethodException.invalidParams("cache parameter requires a ttl, e.g. cache: {ttl: \"5m\"}");
    }

    /**
     * Parse the {@code push} parameter from RPC request params.
     * Defaults to {@code true} if not specified.
     */
    static boolean parsePushParam(JsonNode params) {
        if (params == null || !params.has("push")) {
            return true;
        }
        return params.get("push").asBoolean(true);
    }

    /**
     * Wait configuration parsed from the {@code wait} request parameter.
     * When present, the handler blocks until the job completes (or the timeout elapses)
     * and returns results inline instead of returning a job handle.
     *
     * @param timeout if {@code null}, wait indefinitely; otherwise the maximum duration to wait
     */
    record WaitConfig(Duration timeout) {
        boolean hasTimeout() { return timeout != null; }
    }

    /**
     * Parse the {@code wait} parameter from RPC request params.
     *
     * <p>Accepted forms:
     * <ul>
     *   <li>Absent or {@code false} → {@code null} (async, default)</li>
     *   <li>{@code true} or {@code {}} → {@link WaitConfig} with no timeout</li>
     *   <li>{@code {timeout: "30s"}} → {@link WaitConfig} with the given timeout</li>
     * </ul>
     */
    static WaitConfig parseWaitParam(JsonNode params) throws RPCMethodException {
        if (params == null || !params.has("wait")) {
            return null;
        }
        var waitNode = params.get("wait");
        if (waitNode.isBoolean()) {
            return waitNode.asBoolean() ? new WaitConfig(null) : null;
        }
        if (waitNode instanceof ObjectNode obj) {
            if (obj.has("timeout")) {
                var millis = PERIOD_HELPER.parsePeriodToEpochMillis(obj.get("timeout").asText());
                return new WaitConfig(Duration.ofMillis(millis));
            }
            return new WaitConfig(null);
        }
        throw RPCMethodException.invalidParams(
                "wait parameter must be a boolean or object, e.g. wait: true or wait: {timeout: \"30s\"}");
    }

    /**
     * Create a job event listener based on the cache and push configuration.
     */
    IJobEventListener createListener(CacheConfig cacheConfig, boolean push) {
        var pushListener = push ? resolvePushListener() : null;
        IJobEventListener caching = null;
        if (cacheConfig != null) {
            caching = withTtlEviction(cachingListener, cacheConfig.ttl());
        }
        if (caching != null && pushListener != null) {
            return new CompositeJobEventListener(caching, pushListener);
        }
        if (caching != null) {
            return caching;
        }
        if (pushListener != null) {
            return pushListener;
        }
        return IJobEventListener.NOOP;
    }

    private RPCPushJobEventListener resolvePushListener() {
        var p = pushListener;
        if (p != null) { return p; }
        var writer = outputWriter;
        if (writer != null) {
            p = new RPCPushJobEventListener(writer);
            pushListener = p;
        }
        return p;
    }

    /**
     * Wrap a delegate listener to schedule cache eviction on job completion.
     */
    private IJobEventListener withTtlEviction(IJobEventListener delegate, Duration ttl) {
        return new IJobEventListener() {
            @Override
            public void onJobStarted(String jobId, String description) {
                delegate.onJobStarted(jobId, description);
            }

            @Override
            public void onRecord(String jobId, JsonNode record) {
                delegate.onRecord(jobId, record);
            }

            @Override
            public void onProgress(String jobId, String message) {
                delegate.onProgress(jobId, message);
            }

            @Override
            public void onJobComplete(String jobId, int exitCode, String stderr, String stdout) {
                delegate.onJobComplete(jobId, exitCode, stderr, stdout);
                cachingListener.scheduleEviction(jobId, ttl);
            }
        };
    }
}
