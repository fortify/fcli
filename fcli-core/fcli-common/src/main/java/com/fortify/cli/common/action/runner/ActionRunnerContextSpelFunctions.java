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

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.fcli;
import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.workflow;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;

import lombok.RequiredArgsConstructor;

@Reflectable @RequiredArgsConstructor
@SpelFunctionPrefix("action.")
public final class ActionRunnerContextSpelFunctions {
    private final ActionRunnerContext ctx;
    private final static String RUN_ID = UUID.randomUUID().toString();
    
    @SpelFunction(cat = workflow, desc = "This function returns the current fcli run id, which uniquely represents the current fcli invocation. Different invocations of the fcli executable are guaranteed to have a different, unique run id. Within a single fcli executable invocation, the run id remains the same, even across run.fcli instructions and any other internal fcli command invocations.", 
            returns = "Current fcli run id in UUID format")
    public final String runID() {
        return RUN_ID;
    }
    
    @SpelFunction(cat=workflow, returns="String listing non-blank command-line options copied from the given `cli.options` group")
    public final String copyParametersFromGroup(
        @SpelFunctionParam(name="group", desc="the `cli.options` group name from which to copy CLI options") String group) 
    {
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

    @SpelFunction(cat=fcli, desc = "Formats the input value using the specified formatter as declared through the `formatters` YAML instruction",
                  returns="The formatted value") 
    public final JsonNode fmt(
        @SpelFunctionParam(name="formatterName", desc="the name of the formatter to apply") String formatterName,
        @SpelFunctionParam(name="input", desc="the input to be formatted") JsonNode input)
    {
        return ActionRunnerHelper.fmt(ctx, formatterName, input);
    }

}