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

import java.util.Arrays;

import org.slf4j.LoggerFactory;

import com.fortify.cli.common.action.helper.ActionLoaderHelper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionSource;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.runner.ActionRunner;
import com.fortify.cli.common.action.runner.ActionRunnerConfig;
import com.fortify.cli.common.action.runner.processor.ActionCliOptionsProcessor.ActionOptionHelper;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.OptionsParseResult;
import com.fortify.cli.common.progress.helper.ProgressWriterI18n;
import com.fortify.cli.common.progress.helper.ProgressWriterType;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.layout.TTLLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;

// TODO Do we want to load actions from built-in action sources, or define a separate source for build-time actions? 
public class RunBuildTimeFcliAction {
    public static void main(String[] args) {
        if ( args.length<2 ) {
            throw new RuntimeException("Usage: RunBuildTimeFcliAction <log file> <action-path> [action args]");
        }
        try (var progressWriter = new ProgressWriterI18n(ProgressWriterType.simple, null) ) {
            var logFile = args[0];
            var actionPath = args[1];
            var actionArgs = args.length==2 ? new String[]{} : Arrays.copyOfRange(args, 2, args.length);
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            rootLogger.detachAndStopAllAppenders();
            rootLogger.setLevel(Level.TRACE);
            configureLogFile(rootLogger, logFile);
            var action = ActionLoaderHelper.load(
                ActionSource.externalActionSources(null),
                actionPath,
                ActionValidationHandler.IGNORE).getAction();
            var config = ActionRunnerConfig.builder()
                    .action(action)
                    .progressWriter(progressWriter)
                    .onValidationErrors(RunBuildTimeFcliAction::onValidationErrors)
                    .build();
            new ActionRunner(config).run(actionArgs);
        }
    }
    
    private static final void configureLogFile(Logger rootLogger, String logFile) {
        LoggerContext loggerContext = rootLogger.getLoggerContext();
        FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
        fileAppender.setFile(logFile);
        fileAppender.setAppend(false);
        LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<ILoggingEvent>();
        encoder.setContext(loggerContext);
        TTLLLayout layout = new TTLLLayout();
        layout.setContext(loggerContext);
        layout.start();
        encoder.setLayout(layout);
        fileAppender.setEncoder(encoder);
        fileAppender.setContext(loggerContext);
        fileAppender.start();
        rootLogger.addAppender(fileAppender);
    }
    
    private static final RuntimeException onValidationErrors(OptionsParseResult optionsParseResult) {
        var errorsString = String.join("\n ", optionsParseResult.getValidationErrors());
        var supportedOptionsString = ActionOptionHelper.getSupportedOptionsTable(optionsParseResult.getOptions());
        var msg = String.format("Option errors:\n %s\nSupported options:\n%s\n", errorsString, supportedOptionsString);
        return new RuntimeException(msg);
    }
}