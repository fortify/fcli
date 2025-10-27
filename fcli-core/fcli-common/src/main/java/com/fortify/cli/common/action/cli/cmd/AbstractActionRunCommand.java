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
package com.fortify.cli.common.action.cli.cmd;

import java.util.List;

import com.fortify.cli.common.action.cli.mixin.ActionResolverMixin;
import com.fortify.cli.common.action.cli.mixin.ActionValidationMixin;
import com.fortify.cli.common.action.runner.ActionRunner;
import com.fortify.cli.common.action.runner.ActionRunnerConfig;
import com.fortify.cli.common.action.runner.ActionRunnerConfig.ActionRunnerConfigBuilder;
import com.fortify.cli.common.action.runner.processor.ActionCliOptionsProcessor.ActionOptionHelper;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.OptionsParseResult;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.rest.cli.mixin.UnirestContextMixin;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;

import lombok.SneakyThrows;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Unmatched;

public abstract class AbstractActionRunCommand extends AbstractRunnableCommand {
    @Mixin private ActionResolverMixin.RequiredParameter actionResolver;
    @DisableTest({TestType.MULTI_OPT_SPLIT, TestType.MULTI_OPT_PLURAL_NAME, TestType.OPT_LONG_NAME, TestType.OPT_LONG_NAME_COUNT, TestType.OPT_NAME_FORMAT, TestType.OPT_ARITY_PRESENT})
    @Option(names="action-parameters", arity="0", descriptionKey="fcli.action.run.action-parameter") 
    private List<String> dummyForSynopsis;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;
    @Mixin private UnirestContextMixin unirestContextMixin;
    @Mixin private ActionValidationMixin actionValidationMixin;
    @Unmatched private String[] actionArgs;
    
    @Override @SneakyThrows
    public final Integer call() {
        try (var progressWriter = progressWriterFactory.create()) {
            progressWriter.writeProgress("Loading action %s", actionResolver.getAction());
            var action = actionResolver.loadAction(getType(), actionValidationMixin.getActionValidationHandler());
            var configBuilder = ActionRunnerConfig.builder()
                .onValidationErrors(this::onValidationErrors)
                .action(action)
                .progressWriter(progressWriter)
                .unirestContext(unirestContextMixin.getUnirestContext());
            configure(configBuilder);
            var config = configBuilder.build();
            progressWriter.writeProgress("Executing action %s", config.getAction().getMetadata().getName());
            return run(config, new ActionRunner(config));
        }
    }
    
    private final Integer run(ActionRunnerConfig config, ActionRunner actionRunner) {
        try {
            preRun(config);
            return actionRunner.run(actionArgs);
        } finally {
            postRun(config);
        }
    }

    protected void preRun(ActionRunnerConfig config) {}
    protected void postRun(ActionRunnerConfig config) {}
    
    private ParameterException onValidationErrors(OptionsParseResult optionsParseResult) {
        var errorsString = String.join("\n ", optionsParseResult.getValidationErrors());
        var supportedOptionsString = ActionOptionHelper.getSupportedOptionsTable(optionsParseResult.getOptions());
        var msg = String.format("Option errors:\n %s\nSupported options:\n%s\n", errorsString, supportedOptionsString);
        return new ParameterException(getCommandHelper().getCommandSpec().commandLine(), msg);
    }

    protected abstract String getType();
    protected abstract void configure(ActionRunnerConfigBuilder actionRunnerConfigBuilder);
}