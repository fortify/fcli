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

import lombok.extern.slf4j.Slf4j;

/**
 * Registry of JSON-RPC method handlers. Use {@link #builder()} to construct an instance.
 * <p>
 * The following method groups are always registered:
 * <ul>
 *   <li>{@code rpc.listMethods} — protocol-level method listing</li>
 *   <li>{@code fcli.buildInfo} — fcli build/version metadata</li>
 *   <li>{@code fcli.execute}, {@code fcli.listCommands}, {@code fcli.getCommandDetails} — fcli command invocation</li>
 *   <li>{@code async.*} — paged retrieval and lifecycle operations for async jobs</li>
 * </ul>
 * Exported functions from action YAML files are registered as {@code fn.<functionKey>}
 * methods via {@link Builder#importAction(String)}.
 *
 * @author Ruud Senden
 */
public final class RPCMethodHandlerRegistry {
    private final Map<String, IRPCMethodHandler> handlers;
    private final AsyncJobManager asyncJobManager;

    private RPCMethodHandlerRegistry(Map<String, IRPCMethodHandler> handlers, AsyncJobManager asyncJobManager) {
        this.handlers = handlers;
        this.asyncJobManager = asyncJobManager;
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

    public static Builder builder() {
        return new Builder(new AsyncJobManager());
    }

    public static Builder builder(AsyncJobManager asyncJobManager) {
        return new Builder(asyncJobManager);
    }

    @Slf4j
    public static final class Builder {
        private final AsyncJobManager asyncJobManager;
        private final FcliExecutionContext sharedFunctionContext = new FcliExecutionContext();
        private final Map<String, IRPCMethodHandler> handlers = new LinkedHashMap<>();

        private Builder(AsyncJobManager asyncJobManager) {
            this.asyncJobManager = asyncJobManager;
            // Always registered — rpc.listMethods references the live handlers map so it
            // reflects all later additions made in importAction().
            register("rpc.listMethods", new RPCMethodHandlerListMethods(handlers));
            register("fcli.buildInfo", new RPCMethodHandlerFcliInfo());
            register("fcli.execute", new RPCMethodHandlerFcliExecute(asyncJobManager));
            register("fcli.listCommands", new RPCMethodHandlerFcliListCommands());
            register("fcli.getCommandDetails", new RPCMethodHandlerFcliGetCommandDetails());
            register("async.getPage", new RPCMethodHandlerAsyncGetPage(asyncJobManager));
            register("async.getResult", new RPCMethodHandlerAsyncGetResult(asyncJobManager));
            register("async.cancel", new RPCMethodHandlerAsyncCancel(asyncJobManager));
            register("async.clear", new RPCMethodHandlerAsyncClear(asyncJobManager));
        }

        /**
         * Load the action YAML at {@code importFile} and register each exported
         * function as an {@code fn.<functionKey>} RPC method.
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
                var methodName = "fn." + function.getKey();
                var executor = new ActionFunctionExecutor(action, function, sharedFunctionContext);
                register(methodName, new RPCMethodHandlerActionFunction(executor, asyncJobManager));
                log.debug("Registered imported function as RPC method: {}", methodName);
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
            return new RPCMethodHandlerRegistry(Collections.unmodifiableMap(handlers), asyncJobManager);
        }
    }
}
