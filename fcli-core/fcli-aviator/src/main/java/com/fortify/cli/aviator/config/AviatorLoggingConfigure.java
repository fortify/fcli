package com.fortify.cli.aviator.config;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;

public class AviatorLoggingConfigure {

    public static void configureLogger() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        ConsoleAppender consoleAppender = new ConsoleAppender();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setName("fcli-aviator-console");

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%msg%n");
        encoder.start();
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();

        Logger aviatorLogger = loggerContext.getLogger("com.fortify.cli.aviator");
        aviatorLogger.setLevel(Level.INFO);
        aviatorLogger.setAdditive(false);
        aviatorLogger.addAppender(consoleAppender);
    }
}