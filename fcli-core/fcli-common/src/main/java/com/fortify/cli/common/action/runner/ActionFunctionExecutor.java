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

import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.model.ActionFunction;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.ProgressWriterI18n;
import com.fortify.cli.common.progress.helper.ProgressWriterType;

/**
 * Thread-safe executor for a single action function. Creates a fresh
 * {@link ActionRunnerContextLocal} per invocation and delegates to
 * {@link ActionFunctionSpelFunctions#call(String, Object...)}.
 *
 * <p>The caller supplies a {@code Supplier<ContextFrame>} that is responsible for
 * pushing the correct {@link com.fortify.cli.common.cli.util.FcliExecutionContext}
 * onto the thread-local stack and returning the associated
 * {@link FcliExecutionContextHolder.ContextFrame}. Typical patterns:</p>
 * <ul>
 *   <li><b>MCP stdio / RPC server</b> — the supplier captures a shared
 *       {@link com.fortify.cli.common.cli.util.FcliActionState} and pushes a new
 *       {@code FcliExecutionContext} (fresh {@code UnirestContext}) each call, so
 *       connections are always clean while {@code global.*} variables persist across
 *       calls within the same server instance.</li>
 *   <li><b>MCP HTTP server</b> — the supplier resolves the per-auth-scope action state
 *       and isolation scope at call time, providing full isolation between different
 *       authenticated identities.</li>
 * </ul>
 * Used by MCP/RPC server implementations to invoke exported functions.
 */
public final class ActionFunctionExecutor {
    private final Action action;
    private final ActionFunction function;
    private final Supplier<FcliExecutionContextHolder.ContextFrame> frameSupplier;

    public ActionFunctionExecutor(Action action, ActionFunction function, Supplier<FcliExecutionContextHolder.ContextFrame> frameSupplier) {
        this.action = action;
        this.function = function;
        this.frameSupplier = frameSupplier;
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
        try (var frame = frameSupplier.get()) {
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
        }
    }

    /**
     * Execute the function with named arguments from a Map-like structure.
     */
    public Object execute(Map<String, Object> args) {
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
