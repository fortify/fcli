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
package com.fortify.cli.common.action.runner;

import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.model.ActionFunction;
import com.fortify.cli.common.cli.util.FcliExecutionContext;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.ProgressWriterI18n;
import com.fortify.cli.common.progress.helper.ProgressWriterType;

/**
 * Thread-safe executor for a single action function. Creates a fresh
 * {@link ActionRunnerContextLocal} per invocation, builds the args ObjectNode,
 * and delegates to {@link ActionFunctionSpelFunctions#call(String, Object...)}.
 * <p>
 * A {@link Supplier} of {@link FcliExecutionContext} is evaluated on each invocation
 * to determine which context to push. For RPC/stdio servers this supplier typically
 * returns the same shared instance (so {@code globalActionValues} persist across
 * invocations). For HTTP servers the supplier can return a per-auth-scope context
 * so that different auth identities have isolated {@code globalActionValues}.
 * <p>
 * Used by MCP/RPC server implementations to invoke exported functions.
 */
public final class ActionFunctionExecutor {
    private final Action action;
    private final ActionFunction function;
    private final Supplier<FcliExecutionContext> contextSupplier;

    /**
     * Create an executor that resolves its execution context via a supplier on
     * each invocation.
     */
    public ActionFunctionExecutor(Action action, ActionFunction function, Supplier<FcliExecutionContext> contextSupplier) {
        this.action = action;
        this.function = function;
        this.contextSupplier = contextSupplier;
    }

    /**
     * Convenience constructor that wraps a fixed, shared {@link FcliExecutionContext}
     * so that all invocations use the same context (original behaviour for RPC/stdio).
     */
    public ActionFunctionExecutor(Action action, ActionFunction function, FcliExecutionContext sharedContext) {
        this(action, function, () -> sharedContext);
    }

    public Action getAction() {
        return action;
    }

    public ActionFunction getFunction() {
        return function;
    }

    /**
     * Execute the function with the given arguments. Creates a fresh action
     * runner context per invocation, ensuring thread safety.
     *
     * @param argsNode Function arguments as ObjectNode
     * @return For non-streaming functions: the return value as JsonNode.
     *         For streaming functions: an IActionStepForEachProcessor.
     */
    public Object execute(ObjectNode argsNode) {
        FcliExecutionContextHolder.push(contextSupplier.get());
        try {
            var config = ActionRunnerConfig.builder()
                    .action(action)
                    .progressWriter(new ProgressWriterI18n(ProgressWriterType.none, null))
                    .onValidationErrors(r -> new RuntimeException(String.join("; ", r.getValidationErrors())))
                    .build();
            try (var ctx = ActionRunnerContextLocal.create(config, config.getProgressWriter(), JsonHelper.getObjectMapper().createObjectNode())) {
                ctx.initialize();
                var fnSpel = new ActionFunctionSpelFunctions(ctx);
                return fnSpel.call(function.getKey(), argsNode);
            }
        } finally {
            FcliExecutionContextHolder.pop();
        }
    }

    /**
     * Execute the function with named arguments from a Map-like structure.
     */
    public Object execute(java.util.Map<String, Object> args) {
        var argsNode = JsonHelper.getObjectMapper().createObjectNode();
        if (args != null) {
            args.forEach((k, v) -> {
                if (v instanceof JsonNode jn) {
                    argsNode.set(k, jn);
                } else if (v != null) {
                    argsNode.set(k, JsonHelper.getObjectMapper().valueToTree(v));
                }
            });
        }
        return execute(argsNode);
    }
}
