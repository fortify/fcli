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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fortify.cli.common.action.helper.ActionLoaderHelper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionSource;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.runner.ActionFunctionExecutor;
import com.fortify.cli.common.cli.util.FcliExecutionContext;
import com.fortify.cli.util._common.helper.AsyncJobManager;
import com.fortify.cli.util._common.helper.CachingJobEventListener;
import com.fortify.cli.util._common.helper.CompositeJobEventListener;
import com.fortify.cli.util._common.helper.IJobEventListener;

import lombok.extern.slf4j.Slf4j;

/**
 * Registry of JSON-RPC method handlers. Use {@link #builder()} to construct an instance.
 * <p>
 * The following method groups are always registered:
 * <ul>
 *   <li>{@code rpc.listMethods} — protocol-level method listing</li>
 *   <li>{@code fcli.buildInfo} — fcli build/version metadata</li>
 *   <li>{@code fcli.execute}, {@code fcli.listCommands}, {@code fcli.getCommandDetails} — fcli command invocation (always async)</li>
 *   <li>{@code job.*} — paged retrieval, listing, and cancellation of async jobs</li>
 *   <li>{@code fn.call}, {@code fn.list} — dispatch and discovery of imported action functions (always async)</li>
 * </ul>
 * Exported functions from action YAML files are made available via {@code fn.call}
 * and discoverable via {@code fn.list} by calling {@link Builder#importAction(String)}.
 *
 * @author Ruud Senden
 */
public final class RPCMethodHandlerRegistry {
    private final Map<String, IRPCMethodHandler> handlers;
    private final AsyncJobManager asyncJobManager;
    private final CachingJobEventListener cachingListener;
    private volatile RPCServer.RPCOutputWriter outputWriter;

    private RPCMethodHandlerRegistry(Map<String, IRPCMethodHandler> handlers,
                                     AsyncJobManager asyncJobManager,
                                     CachingJobEventListener cachingListener) {
        this.handlers = handlers;
        this.asyncJobManager = asyncJobManager;
        this.cachingListener = cachingListener;
    }

    public IRPCMethodHandler get(String methodName) {
        return handlers.get(methodName);
    }

    public Map<String, IRPCMethodHandler> getAll() {
        return handlers;
    }

    public AsyncJobManager getAsyncJobManager() {
        return asyncJobManager;
    }

    public CachingJobEventListener getCachingListener() {
        return cachingListener;
    }

    /**
     * Return the current output writer, or {@code null} if the server is not running.
     * Method handlers can use this to send notifications (server-to-client messages
     * without a request id) at any time from any thread.
     */
    public RPCServer.RPCOutputWriter getOutputWriter() {
        return outputWriter;
    }

    /**
     * Set the output writer and create the push listener. Called by {@link RPCServer} on start/stop.
     */
    void setOutputWriter(RPCServer.RPCOutputWriter writer) {
        this.outputWriter = writer;
    }

    public static Builder builder() {
        return new Builder(new AsyncJobManager());
    }

    public static Builder builder(AsyncJobManager asyncJobManager) {
        return new Builder(asyncJobManager);
    }

    @Slf4j
    public static final class Builder {
        private final AsyncJobManager asyncJobManager;
        private final CachingJobEventListener cachingListener = new CachingJobEventListener();
        private final FcliExecutionContext sharedFunctionContext = new FcliExecutionContext();
        private final Map<String, IRPCMethodHandler> handlers = new LinkedHashMap<>();
        private final Map<String, ActionFunctionExecutor> importedFunctions = new LinkedHashMap<>();

        private Builder(AsyncJobManager asyncJobManager) {
            this.asyncJobManager = asyncJobManager;
            // The job event listener is wired later in build() once we know the
            // outputWriter won't be available yet — we use a deferred composite
            // that always includes the caching listener and adds push when available.
        }

        /**
         * Load the action YAML at {@code importFile} and make each exported function
         * available via {@code fn.call} and discoverable via {@code fn.list}.
         */
        public Builder importAction(String importFile) {
            var action = ActionLoaderHelper.load(
                    ActionSource.externalActionSources(importFile),
                    importFile,
                    ActionValidationHandler.WARN)
                .getAction();
            for (var entry : action.getFunctions().entrySet()) {
                var function = entry.getValue();
                if (!function.isExported()) { continue; }
                var executor = new ActionFunctionExecutor(action, function, sharedFunctionContext);
                importedFunctions.put(function.getKey(), executor);
                log.debug("Imported exported function for fn.call: {}", function.getKey());
            }
            return this;
        }

        /** Register a custom method handler. */
        public Builder register(String methodName, IRPCMethodHandler handler) {
            handlers.put(methodName, handler);
            log.debug("Registered RPC method: {}", methodName);
            return this;
        }

        public RPCMethodHandlerRegistry build() {
            // Create a deferred listener that wraps caching + push. The push listener
            // is created lazily when the output writer becomes available.
            var deferredListener = new DeferredPushJobEventListener(cachingListener);

            register("rpc.listMethods", new RPCMethodHandlerListMethods(handlers));
            register("fcli.buildInfo", new RPCMethodHandlerFcliInfo());
            register("fcli.execute", new RPCMethodHandlerFcliExecute(asyncJobManager, deferredListener));
            register("fcli.listCommands", new RPCMethodHandlerFcliListCommands());
            register("fcli.getCommandDetails", new RPCMethodHandlerFcliGetCommandDetails());
            register("job.getPage", new RPCMethodHandlerJobGetPage(cachingListener));
            register("job.cancel", new RPCMethodHandlerJobCancel(asyncJobManager));
            register("job.list", new RPCMethodHandlerJobList(asyncJobManager));
            register("fn.call", new RPCMethodHandlerFnCall(importedFunctions, asyncJobManager, deferredListener));
            register("fn.list", new RPCMethodHandlerFnList(importedFunctions));

            var registry = new RPCMethodHandlerRegistry(
                    Collections.unmodifiableMap(handlers), asyncJobManager, cachingListener);
            deferredListener.setRegistry(registry);
            return registry;
        }
    }

    /**
     * Deferred composite listener that always delegates to the caching listener,
     * and also to a push listener once the output writer is available.
     */
    static final class DeferredPushJobEventListener implements IJobEventListener {
        private final CachingJobEventListener cachingListener;
        private volatile RPCMethodHandlerRegistry registry;
        private volatile IJobEventListener resolved;

        DeferredPushJobEventListener(CachingJobEventListener cachingListener) {
            this.cachingListener = cachingListener;
        }

        void setRegistry(RPCMethodHandlerRegistry registry) {
            this.registry = registry;
        }

        private IJobEventListener resolveListener() {
            var r = resolved;
            if (r != null) { return r; }
            var writer = registry != null ? registry.getOutputWriter() : null;
            if (writer != null) {
                r = new CompositeJobEventListener(
                        cachingListener,
                        new RPCPushJobEventListener(writer));
                resolved = r;
                return r;
            }
            return cachingListener;
        }

        @Override
        public void onJobStarted(String jobId, String description) {
            resolveListener().onJobStarted(jobId, description);
        }

        @Override
        public void onRecord(String jobId, com.fasterxml.jackson.databind.JsonNode record) {
            resolveListener().onRecord(jobId, record);
        }

        @Override
        public void onProgress(String jobId, String message) {
            resolveListener().onProgress(jobId, message);
        }

        @Override
        public void onJobComplete(String jobId, int exitCode, String stderr, String stdout) {
            resolveListener().onJobComplete(jobId, exitCode, stderr, stdout);
        }
    }
}
