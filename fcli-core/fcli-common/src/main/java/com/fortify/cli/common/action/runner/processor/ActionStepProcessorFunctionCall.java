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
package com.fortify.cli.common.action.runner.processor;

import java.util.LinkedHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.ActionStepFunctionCallEntry;
import com.fortify.cli.common.action.runner.ActionFunctionSpelFunctions;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.json.JsonHelper;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

/**
 * Processor for the {@code function.call} step. Resolves the named function,
 * builds the args ObjectNode from the entry's argument template expressions,
 * delegates execution to {@link ActionFunctionSpelFunctions}, and stores the
 * return value in the caller's vars.
 */
@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public class ActionStepProcessorFunctionCall extends AbstractActionStepProcessorMapEntries<String, ActionStepFunctionCallEntry> {
    private final ActionRunnerContext ctx;
    private final LinkedHashMap<String, ActionStepFunctionCallEntry> map;

    @Override
    protected void process(String key, ActionStepFunctionCallEntry entry) {
        var argsNode = buildArgsNode(entry);
        var result = new ActionFunctionSpelFunctions(ctx).call(key, argsNode);
        if (result instanceof JsonNode jn) {
            getVars().set(entry.getVarName(), jn);
        } else {
            // For streaming functions, wrap IActionStepForEachProcessor in POJONode
            getVars().set(entry.getVarName(), new POJONode(result));
        }
    }

    private ObjectNode buildArgsNode(ActionStepFunctionCallEntry entry) {
        var argsNode = JsonHelper.getObjectMapper().createObjectNode();
        for (var argEntry : entry.getArgs().entrySet()) {
            var value = getVars().eval(argEntry.getValue(), JsonNode.class);
            if (value != null) {
                argsNode.set(argEntry.getKey(), value);
            }
        }
        return argsNode;
    }
}
