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

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

/**
 * This class defines command line options for configuring logging, and provides the associated functionality 
 * for configuring Logback based on these command line options. Note that this component should not be used 
 * in regular command implementations; logging should be configured long before any regular commands are being
 * invoked.
 * 
 * @author Ruud Senden
 */
@ReflectiveAccess
public class LoggingMixin {
	@Option(names = "--log-level", scope = ScopeType.INHERIT)
	private LogLevel logLevel;

	@Option(names = "--log-file", scope = ScopeType.INHERIT)
	private String logFile;
	
	private static enum LogLevel {
	    TRACE(Level.TRACE),
	    DEBUG(Level.DEBUG),
	    INFO(Level.INFO),
	    WARN(Level.WARN),
	    ERROR(Level.ERROR);

		private final Level logbackLevel;
		LogLevel(Level logbackLevel) {
			this.logbackLevel = logbackLevel;
		}
	}

	public void configureLogging() {
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
		rootLogger.setLevel(level.logbackLevel);
	}

}
