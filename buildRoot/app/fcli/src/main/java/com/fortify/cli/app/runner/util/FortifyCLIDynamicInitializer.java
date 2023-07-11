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
package com.fortify.cli.app.runner.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand.GenericOptionsArgGroup;
import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand.LogLevel;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.layout.TTLLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
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
        String logFile = genericOptions.getLogFile();
        LogLevel logLevel = genericOptions.getLogLevel();
        if ( logFile!=null || logLevel!=null ) {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            configureLogFile(rootLogger, logFile==null ? "fcli.log" : logFile);
            configureLogLevel(rootLogger, logLevel==null ? LogLevel.INFO : logLevel);
        }
    }

    private void configureLogFile(Logger rootLogger, String logFile) {
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
    public static final class FortifyCLIInitializerCommand extends AbstractFortifyCLICommand implements Runnable {
        private final Consumer<GenericOptionsArgGroup> consumer;
        
        @Override
        public void run() {
            consumer.accept(getGenericOptions());
        }
    }
}
