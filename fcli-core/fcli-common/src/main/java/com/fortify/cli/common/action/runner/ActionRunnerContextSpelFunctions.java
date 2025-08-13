/**
 * Copyright 2023 Open Text.
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

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.fn.descriptor.annotation.SpelFunctionDescription;
import com.fortify.cli.common.spring.expression.fn.descriptor.annotation.SpelFunctionParamDescription;
import com.fortify.cli.common.spring.expression.fn.descriptor.annotation.SpelFunctionPrefix;
import com.fortify.cli.common.spring.expression.fn.descriptor.annotation.SpelFunctionReturnDescription;

import lombok.RequiredArgsConstructor;

@Reflectable @RequiredArgsConstructor
@SpelFunctionPrefix("action.")
public final class ActionRunnerContextSpelFunctions {
    private final ActionRunnerContext ctx;
    
    @SpelFunctionDescription("Copies parameter key-value pairs from the context's CLI options filtered by the specified group, formatting them as command-line arguments.")
    public final @SpelFunctionReturnDescription("a string containing the copied parameters formatted as CLI options") String copyParametersFromGroup(
        @SpelFunctionParamDescription("the group name used to filter parameters; if null, all groups are included") String group) {
        StringBuilder result = new StringBuilder();
        for (var e : ctx.getConfig().getAction().getCliOptions().entrySet()) {
            var name = e.getKey();
            var p = e.getValue();
            if (group == null || group.equals(p.getGroup())) {
                var val = ctx.getParameterValues().get(name);
                if (val != null && StringUtils.isNotBlank(val.asText())) {
                    result
                        .append("\"--")
                        .append(name)
                        .append("=")
                        .append(val.asText())
                        .append("\" ");
                }
            }
        }
        return result.toString();
    }

    @SpelFunctionDescription("Formats the input JsonNode using the specified formatter name via ActionRunnerHelper.")
    public final @SpelFunctionReturnDescription("the formatted JsonNode result") JsonNode fmt(
        @SpelFunctionParamDescription("the name of the formatter to apply") String formatterName,
        @SpelFunctionParamDescription("the JsonNode input to be formatted") JsonNode input) {
        return ActionRunnerHelper.fmt(ctx, formatterName, input);
    }

}