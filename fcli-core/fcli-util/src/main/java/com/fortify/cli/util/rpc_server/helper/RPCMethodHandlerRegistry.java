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
import java.util.function.Supplier;

import com.fortify.cli.common.action.helper.ActionLoaderHelper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionSource;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.runner.ActionFunctionExecutor;
import com.fortify.cli.common.cli.util.FcliActionState;
import com.fortify.cli.common.cli.util.FcliExecutionContext;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.cli.util.FcliIsolationScope;
import com.fortify.cli.common.concurrent.job.AsyncJobManager;
import com.fortify.cli.common.concurrent.job.CachingJobEventListener;

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
    private final FcliIsolationScope isolationScope;
    private final RPCJobEventListenerFactory listenerFactory;

    private RPCMethodHandlerRegistry(Map<String, IRPCMethodHandler> handlers,
                                     AsyncJobManager asyncJobManager,
                                     FcliIsolationScope isolationScope,
                                     RPCJobEventListenerFactory listenerFactory) {
        this.handlers = handlers;
        this.asyncJobManager = asyncJobManager;
        this.isolationScope = isolationScope;
        this.listenerFactory = listenerFactory;
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
        return isolationScope.getOrCreateScopedState(CachingJobEventListener.class, CachingJobEventListener::new);
    }

    FcliIsolationScope getIsolationScope() {
        return isolationScope;
    }

    /**
     * Set the output writer. Called by {@link RPCServer} on start/stop.
     */
    void setOutputWriter(RPCServer.RPCOutputWriter writer) {
        listenerFactory.setOutputWriter(writer);
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
        private final FcliIsolationScope sharedIsolationScope = new FcliIsolationScope();
        private final FcliActionState sharedFunctionActionState = new FcliActionState();
        private final Supplier<FcliExecutionContextHolder.ContextFrame> sharedFunctionFrameSupplier =
                () -> FcliExecutionContextHolder.push(new FcliExecutionContext(sharedIsolationScope, sharedFunctionActionState));
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
                var executor = new ActionFunctionExecutor(action, function, sharedFunctionFrameSupplier);
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
            var listenerFactory = new RPCJobEventListenerFactory();

            register("rpc.listMethods", new RPCMethodHandlerListMethods(handlers));
            register("fcli.buildInfo", new RPCMethodHandlerFcliInfo());
            register("fcli.execute", new RPCMethodHandlerFcliExecute(asyncJobManager, listenerFactory));
            register("fcli.listCommands", new RPCMethodHandlerFcliListCommands());
            register("fcli.getCommandDetails", new RPCMethodHandlerFcliGetCommandDetails());
                register("job.getPage", new RPCMethodHandlerJobGetPage());
                register("job.getStatus", new RPCMethodHandlerJobGetStatus(asyncJobManager));
            register("job.cancel", new RPCMethodHandlerJobCancel(asyncJobManager));
                register("job.remove", new RPCMethodHandlerJobRemove(asyncJobManager));
            register("job.list", new RPCMethodHandlerJobList(asyncJobManager));
            register("fn.call", new RPCMethodHandlerFnCall(importedFunctions, asyncJobManager, listenerFactory));
            register("fn.list", new RPCMethodHandlerFnList(importedFunctions));

            return new RPCMethodHandlerRegistry(
                    Collections.unmodifiableMap(handlers), asyncJobManager, sharedIsolationScope, listenerFactory);
        }
    }
}
