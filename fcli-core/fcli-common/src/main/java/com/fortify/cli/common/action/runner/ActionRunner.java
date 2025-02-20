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

import java.util.function.Supplier;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.model.ActionConfig.ActionConfigOutput;
import com.fortify.cli.common.action.model.ActionStepCheckEntry;
import com.fortify.cli.common.action.model.ActionStepCheckEntry.CheckStatus;
import com.fortify.cli.common.action.runner.processor.ActionCliOptionsProcessor;
import com.fortify.cli.common.action.runner.processor.ActionStepProcessorSteps;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.progress.helper.ProgressWriterType;

import lombok.RequiredArgsConstructor;

// TODO Move processing of each descriptor element into a separate class,
//      either for all elements or just for step elements.
//      For example, each of these classes could have a (static?) 
//      process(context, vars, action element) method
@RequiredArgsConstructor
public class ActionRunner {
    private final ActionRunnerConfig config;
    
    public final Integer run(String[] args) {
        return _run(args).get();
    }
    
    public final Supplier<Integer> _run(String[] args) {
        try ( var progressWriter = createProgressWriter() ) {
            var parameterValues = getParameterValues(args);
            try ( var ctx = createContext(progressWriter, parameterValues) ) {
                initializeCheckStatuses(ctx);
                ActionRunnerVars vars = new ActionRunnerVars(ctx.getSpelEvaluator(), ctx.getParameterValues());
                new ActionStepProcessorSteps(ctx, vars, config.getAction().getSteps()).process();;
             
                return ()->{
                    ctx.getDelayedConsoleWriterRunnables().forEach(Runnable::run);
                    if ( !ctx.getCheckStatuses().isEmpty() ) {
                        ctx.getCheckStatuses().entrySet().forEach(
                            e-> printCheckResult(ctx, e.getValue(), e.getKey()));
                        var overallStatus = CheckStatus.combine(ctx.getCheckStatuses().values());
                        ctx.getStdout().println("Status: "+overallStatus);
                        if ( ctx.getExitCode()==0 && overallStatus==CheckStatus.FAIL ) {
                            ctx.setExitCode(100);
                        }
                    }
                    return ctx.getExitCode();
                };
            }
        }
    }

    private IProgressWriterI18n createProgressWriter() {
        var factory = config.getProgressWriterFactory();
        var type = factory.getType();
        if ( config.getAction().getConfig().getOutput()==ActionConfigOutput.immediate && type!=ProgressWriterType.none) {
            type = ProgressWriterType.simple;
        }
        return factory.create(type);
    }

    private ActionRunnerContext createContext(IProgressWriterI18n progressWriter, ObjectNode parameterValues) {
        return ActionRunnerContext.builder()
                .config(config)
                .progressWriter(progressWriter)
                .parameterValues(parameterValues)
                .build().initialize();
    }

    private ObjectNode getParameterValues(String[] args) {
        var parameterValues = ActionCliOptionsProcessor.builder()
                .config(config)
                .spelEvaluator(config.getSpelEvaluator())
                .build()
                .parseOptionValues(args);
        return parameterValues;
    }
    
    private static final void initializeCheckStatuses(ActionRunnerContext ctx) {
        for ( var elt : ctx.getConfig().getAction().getAllActionElements() ) {
            if ( elt instanceof ActionStepCheckEntry ) {
                var checkStep = (ActionStepCheckEntry)elt;
                var value = CheckStatus.combine(ctx.getCheckStatuses().get(checkStep), checkStep.getIfSkipped());
                ctx.getCheckStatuses().put(checkStep, value);
            }
        }
    }

    private static final void printCheckResult(ActionRunnerContext ctx, CheckStatus status, ActionStepCheckEntry checkStep) {
        if ( status!=CheckStatus.HIDE ) {
            // Even when flushing, output may appear in incorrect order if some 
            // check statuses are written to stdout and others to stderr.
            //var out = status==CheckStatus.PASS?stdout:stderr;
            var out = ctx.getStdout();
            out.println(String.format("%s: %s", status, checkStep.getDisplayName()));
            //out.flush();
        }
    }
}
