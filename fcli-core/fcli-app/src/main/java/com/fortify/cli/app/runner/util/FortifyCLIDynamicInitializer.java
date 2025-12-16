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
package com.fortify.cli.app.runner.util;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand.GenericOptionsArgGroup;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand.LogLevel;
import com.fortify.cli.common.log.LogMaskHelper;
import com.fortify.cli.common.log.LogMessageType;
import com.fortify.cli.common.log.LogMessageTypeConverter;
import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskingLogMessageConverter;
import com.fortify.cli.common.util.DebugHelper;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.FileAppender;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * This class is responsible for performing static initialization of fcli, i.e.,
 * initialization that is not dependent on command-line options.
 * 
 * @author Ruud Senden
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FortifyCLIDynamicInitializer {
    private static final PrintWriter DUMMY_WRITER = new PrintWriter(new StringWriter());
    @Getter(lazy = true)
    private static final FortifyCLIDynamicInitializer instance = new FortifyCLIDynamicInitializer(); 
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    private final CommandLine genericOptionsCommandLine = createGenericOptionsCommandLine();
    
    public void initialize(String[] args) {
        // Remove help options, as we want initialization always to occur
        String[] argsWithoutHelp = Stream.of(args).filter(a->!a.matches("-h|--help")).toArray(String[]::new);
        getGenericOptionsCommandLine().execute(argsWithoutHelp);
    }
    
    private void initialize(GenericOptionsArgGroup genericOptions) {
        initializeEnvPrefix(genericOptions);
        initializeLogging(genericOptions);
    }
    
    private void initializeEnvPrefix(GenericOptionsArgGroup genericOptions) {
        String envPrefix = genericOptions.getEnvPrefix();
        System.setProperty("fcli.env.default.prefix", envPrefix);
        FortifyCLIDefaultValueProvider.getInstance().setEnvPrefix(envPrefix);
    }
        
    public void initializeLogging(GenericOptionsArgGroup genericOptions) {
        LogMaskHelper.INSTANCE.setLogMaskLevel(genericOptions.getLogMaskLevel());
        registerDefaultLogMaskPatterns();
        boolean isDebugEnabled = genericOptions.isDebug();
        File logFile = genericOptions.getLogFile();
        LogLevel logLevel = genericOptions.getLogLevel();
        if ( logLevel==null && isDebugEnabled ) {
            // If no log level is specified and --debug is specified, set log level to TRACE
            logLevel = LogLevel.TRACE;
        }
        DebugHelper.setDebugEnabled(isDebugEnabled);
        if ( logLevel!=LogLevel.NONE && (logFile!=null || logLevel!=null) ) {
            // Configure logging if logLevel is not set to NONE, and logFile and/or logLevel 
            // have been specified
            LoggerContext loggerContext = initializeLoggerContext();
            Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            configureLogFile(rootLogger, logFile==null ? "fcli.log" : logFile.getAbsolutePath());
            configureLogLevel(rootLogger, logLevel==null ? LogLevel.INFO : logLevel);
        }
    }

    private void registerDefaultLogMaskPatterns() {
        LogMaskHelper.INSTANCE
            .registerPattern(LogSensitivityLevel.high, "Authorization: (?:[a-zA-Z]+ )?(.*?)(?:\\Q[\\r]\\E|\\Q[\\n]\\E)*\\\"?$", "<REDACTED>", LogMessageType.HTTP_OUT)
            .registerPattern(LogSensitivityLevel.high, "(?:\\\"token\\\"|\\\"access_token\\\"):\\s*\\\"(.*?)\\\"", "<REDACTED TOKEN (RESPONSE)>", LogMessageType.HTTP_IN);
    }

    @SuppressWarnings("unchecked")
    private LoggerContext initializeLoggerContext() {
        var lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        var ruleRegistry = (Map<String, String>)lc.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
        if (ruleRegistry == null) {
            ruleRegistry = new HashMap<String, String>();
        }
        lc.putObject(CoreConstants.PATTERN_RULE_REGISTRY, ruleRegistry);
        ruleRegistry.put(MaskingLogMessageConverter.conversionWord, MaskingLogMessageConverter.class.getName());
        ruleRegistry.put(LogMessageTypeConverter.conversionWord, LogMessageTypeConverter.class.getName());
        return lc;
    }

    private void configureLogFile(Logger rootLogger, String logFile) {
        LoggerContext loggerContext = rootLogger.getLoggerContext();
        FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
        fileAppender.setFile(logFile);
        fileAppender.setAppend(false);
        //LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<ILoggingEvent>();
        //encoder.setContext(loggerContext);
        var ple = new PatternLayoutEncoder();
        ple.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %"+LogMessageTypeConverter.conversionWord+" %logger{36} -%kvp- %"+MaskingLogMessageConverter.conversionWord+"%n");
        ple.setContext(loggerContext);
        ple.start();
        //encoder.setLayout(layout);
        fileAppender.setEncoder(ple);
        fileAppender.setContext(loggerContext);
        fileAppender.start();
        rootLogger.addAppender(fileAppender);
    }

    private void configureLogLevel(Logger rootLogger, LogLevel level) {
        rootLogger.setLevel(level.getLogbackLevel());
    }
    
    private CommandLine createGenericOptionsCommandLine() {
        return new CommandLine(new FortifyCLIInitializerCommand(this::initialize))
                .setOut(DUMMY_WRITER)
                .setErr(DUMMY_WRITER)
                .setUnmatchedArgumentsAllowed(true)
                .setUnmatchedOptionsArePositionalParams(true)
                .setExpandAtFiles(true)
                .setDefaultValueProvider(FortifyCLIDefaultValueProvider.getInstance());
    }
    
    @Command(name = "fcli")
    @RequiredArgsConstructor
    public static final class FortifyCLIInitializerCommand extends AbstractRunnableCommand {
        private final Consumer<GenericOptionsArgGroup> consumer;
        
        @Override
        public Integer call() {
            consumer.accept(getGenericOptions());
            return 0;
        }
    }
}
