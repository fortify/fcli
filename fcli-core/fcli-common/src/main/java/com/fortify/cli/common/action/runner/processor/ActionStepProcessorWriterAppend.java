/*
 * Copyright 2021-2025 Open Text.
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

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.FcliActionValidationException;
import com.fortify.cli.common.action.model.TemplateExpressionWithFormatter;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerHelper;
import com.fortify.cli.common.action.runner.ActionRunnerVars;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public class ActionStepProcessorWriterAppend extends AbstractActionStepProcessorMapEntries<String, TemplateExpressionWithFormatter>{
    private final ActionRunnerContext ctx;
    private final ActionRunnerVars vars;
    private final LinkedHashMap<String,TemplateExpressionWithFormatter> map;
    
    @Override
    protected final void process(String writerId, TemplateExpressionWithFormatter templateExpressionWithFormatter) {
        var writer = ctx.getWriters().get(writerId);
        if ( writer==null ) {
            throw new FcliActionValidationException("No writer available with id "+writerId);
        }
        var value = ActionRunnerHelper.formatValueAsJsonNode(ctx, vars, templateExpressionWithFormatter);
        if ( !(value instanceof ObjectNode) ) {
            throw new FcliActionValidationException("Data to append to writer must be an ObjectNode; actual type: "+value.getClass().getSimpleName());
        }
        writer.append((ObjectNode)value);
        var countVarName = String.format("%s.count", writerId);  
        vars.set(countVarName, new IntNode(vars.eval(countVarName, Integer.class)+1));
    }
}
