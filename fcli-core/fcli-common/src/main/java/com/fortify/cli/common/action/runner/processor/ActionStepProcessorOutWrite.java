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
package com.fortify.cli.common.action.runner.processor;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.TemplateExpressionWithFormatter;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerHelper;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.action.runner.FcliActionStepException;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public class ActionStepProcessorOutWrite extends AbstractActionStepProcessorMapEntries<TemplateExpression, TemplateExpressionWithFormatter> {
    private final ActionRunnerContext ctx;
    private final ActionRunnerVars vars;
    private final LinkedHashMap<TemplateExpression,TemplateExpressionWithFormatter> map;
    
    @Override
    protected final void process(TemplateExpression target, TemplateExpressionWithFormatter templateExpressionWithFormatter) {
        write(target, ActionRunnerHelper.formatValueAsObject(ctx, vars, templateExpressionWithFormatter));
    }
    
    private final void write(TemplateExpression destinationExpression, Object valueObject) {
        var destination = vars.eval(destinationExpression, String.class);
        var value = asString(valueObject);
        try {
            switch (destination.toLowerCase()) {
            case "stdout": System.out.print(value); break;
            case "stderr": System.err.print(value); break;
            default: write(new File(destination), value);
            }
        } catch (IOException e) {
            throw new FcliActionStepException("Error writing action output to "+destination);
        }
    }
    
    private final void write(File file, String output) throws IOException {
        try ( var out = new PrintStream(file, StandardCharsets.UTF_8) ) {
            out.println(output);
        }
    }
}
