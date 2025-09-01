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

import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.model.ActionConfig.ActionConfigOutput;
import com.fortify.cli.common.action.model.ActionInputMask;
import com.fortify.cli.common.action.model.ActionStepCheckEntry;
import com.fortify.cli.common.action.model.ActionStepCheckEntry.CheckStatus;
import com.fortify.cli.common.action.runner.processor.ActionCliOptionsProcessor;
import com.fortify.cli.common.action.runner.processor.ActionStepProcessorSteps;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.log.LogMaskHelper;
import com.fortify.cli.common.log.LogMaskSource;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;
import com.fortify.cli.common.output.writer.record.RecordWriterStyle;
import com.fortify.cli.common.output.writer.record.RecordWriterStyle.RecordWriterStyleElement;
import com.fortify.cli.common.output.writer.record.util.NonClosingWriterWrapper;
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
        maskEnvVars();
        return _run(args);
    }
    
    // TODO Review try/close/finally blocks and handling of output in delayed console writers
    //      to see whether anything can be simplified, and whether there are any bugs.
    public final Integer _run(String[] args) {
        List<Runnable> delayedConsoleWriterRunnables = Collections.emptyList();
        Map<ActionStepCheckEntry, CheckStatus> checkStatuses = Collections.emptyMap();
        CheckStatus overallCheckstatus = CheckStatus.SKIP;
        int exitCode = 0;
        try ( var progressWriter = createProgressWriter() ) {
            var parameterValues = getParameterValues(args);
            try ( var ctx = createContext(progressWriter, parameterValues) ) {
                initializeCheckStatuses(ctx);
                ActionRunnerVars vars = new ActionRunnerVars(ctx.getSpelEvaluator(), ctx.getParameterValues());
                try {
                    new ActionStepProcessorSteps(ctx, vars, config.getAction().getSteps()).process();
                } finally {
                    // Collect outputs from context; we can't write any of these outputs
                    // until after the progress writer has been closed.
                    delayedConsoleWriterRunnables = ctx.getDelayedConsoleWriterRunnables();
                    checkStatuses = ctx.getCheckStatuses();
                    exitCode = ctx.getExitCode();
                }
            }
        } finally {
            // Write delayed console output and check statuses, now that progress writer has been closed
            delayedConsoleWriterRunnables.forEach(Runnable::run);
            overallCheckstatus = processAndPrintCheckStatuses(checkStatuses);
        }
        // Determine final exit code
        return exitCode==0 && overallCheckstatus==CheckStatus.FAIL ? 100 : exitCode;
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
    
    private final CheckStatus processAndPrintCheckStatuses(Map<ActionStepCheckEntry, CheckStatus> checkStatuses) {
        if ( checkStatuses.isEmpty() ) { return CheckStatus.SKIP; }
        try ( var recordWriter = createCheckStatusWriter(); ) {
            checkStatuses.entrySet().stream()
                .filter(e->e.getValue()!=CheckStatus.HIDE)
                .map(this::checkStatusAsObjectNode)
                .forEach(recordWriter::append);
            var overallStatus = CheckStatus.combine(checkStatuses.values());
            recordWriter.append(checkStatusAsObjectNode("Overall Status", overallStatus));
            return overallStatus;
        }
    }
    
    private final ObjectNode checkStatusAsObjectNode(Map.Entry<ActionStepCheckEntry, CheckStatus> e) {
        return checkStatusAsObjectNode(e.getKey().getDisplayName(), e.getValue());
    }
    
    private final ObjectNode checkStatusAsObjectNode(String displayName, CheckStatus status) {
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("Check", displayName)
                .put("Status", status.toString());
    }

    private IRecordWriter createCheckStatusWriter() {
        var recordWriterConfig = RecordWriterConfig.builder()
                .style(RecordWriterStyle.apply(RecordWriterStyleElement.md_border))
                .writerSupplier(()->new NonClosingWriterWrapper(new OutputStreamWriter(System.out)))
                .build();
        var recordWriter = RecordWriterFactory.table.createWriter(recordWriterConfig);
        return recordWriter;
    }
    
    private final void maskEnvVars() {
        var envVarMasks = config.getAction().getConfig().getEnvVarMasks();
        if ( envVarMasks!=null ) {
            envVarMasks.forEach(this::maskEnvVar);
        }
    }

    private void maskEnvVar(String envVar, ActionInputMask maskConfig) {
        var value = System.getenv(envVar);
        if ( StringUtils.isNotBlank(value) ) {
            var description = maskConfig.getDescription();
            if ( StringUtils.isBlank(description)) {
                description = envVar;
            }
            LogMaskHelper.INSTANCE.registerValue(maskConfig.getSensitivityLevel(), LogMaskSource.ENV_VAR, description, value, maskConfig.getPattern());
        }
    }
}
