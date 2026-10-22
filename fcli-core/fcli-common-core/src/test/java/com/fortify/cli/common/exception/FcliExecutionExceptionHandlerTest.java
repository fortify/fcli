/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Model.CommandSpec;

/**
 * AviatorConnectivityExceptionTest covers the safe logging path. This module's test
 * classpath lacks the annotation library needed to extend @Reflectable exceptions.
 */
class FcliExecutionExceptionHandlerTest {
    private final Logger logger = (Logger)LoggerFactory.getLogger(FcliExecutionExceptionHandler.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level previousLevel;

    @BeforeEach
    void setUp() {
        previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(previousLevel);
    }

    @Test
    void logsThrowableForFcliExceptionsWithoutSafeDiagnosticString() {
        var err = new StringWriter();

        int exitCode = FcliExecutionExceptionHandler.INSTANCE.handleException(new FcliSimpleException("Plain failure"), createCommandLine(err));

        assertEquals(1, exitCode);
        assertTrue(err.toString().contains("Plain failure"));
        var event = singleEvent();
        assertNotNull(event.getThrowableProxy());
        assertEquals("fcli terminating with exception", event.getFormattedMessage());
    }

    @Test
    void logsThrowableForNonFcliExceptions() {
        FcliExecutionExceptionHandler.INSTANCE.handleException(new IllegalStateException("Unexpected"), createCommandLine(new StringWriter()));

        assertNotNull(singleEvent().getThrowableProxy());
    }

    private ILoggingEvent singleEvent() {
        assertEquals(1, appender.list.size());
        return appender.list.get(0);
    }

    private static CommandLine createCommandLine(StringWriter err) {
        var commandLine = new CommandLine(CommandSpec.create());
        commandLine.setErr(new PrintWriter(err));
        commandLine.setColorScheme(CommandLine.Help.defaultColorScheme(Ansi.OFF));
        return commandLine;
    }
}
