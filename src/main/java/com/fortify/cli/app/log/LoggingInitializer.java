/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.app.log;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.LoggerFactory;

import com.fortify.cli.app.FortifyCLIDefaultValueProvider;
import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.util.IFortifyCLIInitializer;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import jakarta.inject.Singleton;
import picocli.CommandLine;
import picocli.CommandLine.Command;

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
    private static final PrintWriter DUMMY_WRITER = new PrintWriter(new StringWriter());
    
    /**
     * Configure logging based on the provided command line arguments.
     * @param args Arguments passed on the command line
     */
    public static final void initializeLogging(String[] args) {
        CommandLine commandLine = new CommandLine(SetupLoggingCommand.class)
                .setOut(DUMMY_WRITER)
                .setErr(DUMMY_WRITER)
                .setUnmatchedArgumentsAllowed(true)
                .setUnmatchedOptionsArePositionalParams(true)
                .setExpandAtFiles(true);
        commandLine.execute(args);
    }

    @Override
    public void initializeFortifyCLI(String[] args) {
        initializeLogging(args);
    }

    /**
     * {@link Command} implementation for setting up logging, based on the
     * options and functionality provided by {@link LoggingMixin}.
     * 
     * @author Ruud Senden
     */
    @Command(defaultValueProvider = FortifyCLIDefaultValueProvider.class)
    public static final class SetupLoggingCommand extends AbstractFortifyCLICommand implements Runnable {
        /**
         * Configure logging by calling the {@link LoggingMixin#configureLogging()}
         * method.
         */
        @Override
        public void run() {
            configureLogging();
        }
        
        public void configureLogging() {
            String logFile = getGenericOptions().getLogFile();
            LogLevel logLevel = getGenericOptions().getLogLevel();
            if ( logFile!=null || logLevel!=null ) {
                LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
                configureLogFile(rootLogger, logFile==null ? "fcli.log" : logFile);
                configureLogLevel(rootLogger, logLevel==null ? LogLevel.INFO : logLevel);
            }
        }

        private void configureLogFile(Logger rootLogger, String logFile) {
            FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
            fileAppender.setFile(logFile);
            fileAppender.setAppend(false);
            fileAppender.setEncoder(((ConsoleAppender<ILoggingEvent>)rootLogger.getAppender("default")).getEncoder());
            fileAppender.setContext(rootLogger.getLoggerContext());
            fileAppender.start();
            rootLogger.addAppender(fileAppender);
        }

        private void configureLogLevel(Logger rootLogger, LogLevel level) {
            rootLogger.setLevel(level.getLogbackLevel());
        }

    }
}
