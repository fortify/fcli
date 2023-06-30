/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.app.log;

import java.io.PrintWriter;

import org.slf4j.LoggerFactory;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand.GenericOptionsArgGroup;
import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand.LogLevel;
import com.fortify.cli.common.cli.util.FortifyCLIInitializerRunner.FortifyCLIInitializerCommand;
import com.fortify.cli.common.cli.util.IFortifyCLIInitializer;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.layout.TTLLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import jakarta.inject.Singleton;
import picocli.CommandLine;

/**
 * This class is responsible for setting up logging. It simply sets up a
 * small {@link CommandLine} instance with a single {@link SetupLoggingCommand}
 * that looks for logging parameters as defined in {@link LoggingMixin}
 * while ignoring everything else (including any sub-commands) on the command 
 * line; this essentially means that this command will always run. Upon execution,
 * the {@link SetupLoggingCommand} will simply invoke {@link LoggingMixin#configureLogging()}
 * to actually configure the logging. All output from this small {@link CommandLine}
 * implementation will be suppressed by sending the output to a dummy {@link PrintWriter}. 
 * 
 * @author Ruud Senden
 */
@Singleton
public class LoggingInitializer implements IFortifyCLIInitializer {
    @Override
    public void initializeFortifyCLI(FortifyCLIInitializerCommand cmd) {
        configureLogging(cmd.getGenericOptions());
    }
        
    public void configureLogging(GenericOptionsArgGroup genericOptions) {
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
}
