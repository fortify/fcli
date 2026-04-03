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

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.workflow;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.ActionFunction;
import com.fortify.cli.common.action.model.ActionFunctionArg;
import com.fortify.cli.common.action.model.ActionStepRecordsForEach.IActionStepForEachProcessor;
import com.fortify.cli.common.action.runner.processor.ActionStepProcessorSteps;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;

import lombok.RequiredArgsConstructor;

/**
 * SpEL functions registered as {@code #fn}. Provides {@code #fn.call(name, args...)}
 * for invoking action functions from SpEL expressions.
 */
@Reflectable @RequiredArgsConstructor
@SpelFunctionPrefix("fn.")
public final class ActionFunctionSpelFunctions {
    private static final ObjectMapper objectMapper = JsonHelper.getObjectMapper();
    private final ActionRunnerContextLocal ctx;

    @SpelFunction(cat = workflow, desc = "Call an action function by name. Arguments can be passed positionally (matching args declaration order) or as a single named-args map.",
            returns = "The function's return value (JsonNode), or an IActionStepForEachProcessor for streaming functions")
    public final Object call(
            @SpelFunctionParam(name = "name", desc = "Function name as declared in the functions: section") String name,
            @SpelFunctionParam(name = "args", desc = "Positional arguments or a single Map/ObjectNode for named arguments") Object... args) {
        var function = resolveFunction(name);
        var argsNode = buildArgsNode(function, args);
        if (function.isStreaming()) {
            return createStreamingProcessor(function, argsNode);
        }
        return executeNonStreaming(function, argsNode);
    }

    private ActionFunction resolveFunction(String name) {
        var functions = ctx.getConfig().getAction().getFunctions();
        var function = functions.get(name);
        if (function == null) {
            throw new FcliActionStepException("Unknown function: " + name);
        }
        return function;
    }

    private ObjectNode buildArgsNode(ActionFunction function, Object[] callArgs) {
        var argsNode = objectMapper.createObjectNode();
        var declaredArgs = function.getArgsOrEmpty();
        if (callArgs == null || callArgs.length == 0) {
            applyDefaults(declaredArgs, argsNode);
            return argsNode;
        }
        if (isNamedInvocation(declaredArgs, callArgs)) {
            applyNamedArgs(declaredArgs, callArgs[0], argsNode);
        } else {
            applyPositionalArgs(declaredArgs, callArgs, argsNode);
        }
        applyDefaults(declaredArgs, argsNode);
        validateRequiredArgs(function, declaredArgs, argsNode);
        return argsNode;
    }

    /**
     * Detect named invocation: single Map/ObjectNode arg AND the function's first declared
     * arg is NOT of type 'object' (to avoid ambiguity).
     */
    private boolean isNamedInvocation(Map<String, ActionFunctionArg> declaredArgs, Object[] callArgs) {
        if (callArgs.length != 1) { return false; }
        var arg = callArgs[0];
        if (!(arg instanceof Map<?,?>) && !(arg instanceof ObjectNode)) { return false; }
        if (declaredArgs.isEmpty()) { return true; }
        var firstArgType = declaredArgs.values().iterator().next().getType();
        return !"object".equalsIgnoreCase(firstArgType);
    }

    @SuppressWarnings("unchecked")
    private void applyNamedArgs(Map<String, ActionFunctionArg> declaredArgs, Object source, ObjectNode argsNode) {
        if (source instanceof ObjectNode on) {
            on.fields().forEachRemaining(e -> argsNode.set(e.getKey(), e.getValue()));
        } else if (source instanceof Map<?,?>) {
            ((Map<String, Object>) source).forEach((k, v) -> argsNode.set(k, toJsonNode(v)));
        }
    }

    private void applyPositionalArgs(Map<String, ActionFunctionArg> declaredArgs, Object[] callArgs, ObjectNode argsNode) {
        var argNames = declaredArgs.keySet().toArray(String[]::new);
        for (int i = 0; i < callArgs.length; i++) {
            if (i >= argNames.length) {
                throw new FcliActionStepException(
                        String.format("Too many arguments: expected at most %d, got %d", argNames.length, callArgs.length));
            }
            argsNode.set(argNames[i], toJsonNode(callArgs[i]));
        }
    }

    private void applyDefaults(Map<String, ActionFunctionArg> declaredArgs, ObjectNode argsNode) {
        for (var entry : declaredArgs.entrySet()) {
            if (!argsNode.has(entry.getKey()) && entry.getValue().getDefaultValue() != null) {
                var defaultExpr = entry.getValue().getDefaultValue();
                var defaultValue = ctx.getVars().eval(defaultExpr, JsonNode.class);
                if (defaultValue != null) {
                    argsNode.set(entry.getKey(), defaultValue);
                }
            }
        }
    }

    private void validateRequiredArgs(ActionFunction function, Map<String, ActionFunctionArg> declaredArgs, ObjectNode argsNode) {
        for (var entry : declaredArgs.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue().getRequired()) && !argsNode.has(entry.getKey())) {
                throw new FcliActionStepException(
                        String.format("Missing required argument '%s' for function '%s'", entry.getKey(), function.getKey()));
            }
        }
    }

    private IActionStepForEachProcessor createStreamingProcessor(ActionFunction function, ObjectNode argsNode) {
        return consumer -> {
            var funcCtx = ctx.createChildForFunction(argsNode);
            funcCtx.setYieldConsumer(consumer);
            try {
                new ActionStepProcessorSteps(funcCtx, function.getSteps()).process();
            } catch (ActionStepBreakException e) {
                // Normal flow — consumer signaled early termination
            } finally {
                funcCtx.setYieldConsumer(null);
            }
        };
    }

    private JsonNode executeNonStreaming(ActionFunction function, ObjectNode argsNode) {
        var funcCtx = ctx.createChildForFunction(argsNode);
        new ActionStepProcessorSteps(funcCtx, function.getSteps()).process();
        if (function.get_return() != null) {
            return funcCtx.getVars().eval(function.get_return(), JsonNode.class);
        }
        var values = funcCtx.getVars().getValues();
        return values.has("_result") ? values.get("_result") : null;
    }

    private JsonNode toJsonNode(Object value) {
        if (value == null) { return objectMapper.nullNode(); }
        if (value instanceof JsonNode jn) { return jn; }
        return objectMapper.valueToTree(value);
    }
}
