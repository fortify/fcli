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
package com.fortify.cli.aviator._common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.exception.FcliExecutionExceptionHandler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Model.CommandSpec;

class AviatorConnectivityExceptionTest {
    private static final String SECRET = "sentinel-secret-value";

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
    void handlerPrintsTheMessageWithoutCause() {
        var err = new StringWriter();
        var exception = new AviatorConnectivityException("Proxy localhost:33444 refused the connection; check the proxy host and port");

        int exitCode = FcliExecutionExceptionHandler.INSTANCE.handleException(exception, createCommandLine(err));

        assertEquals(1, exitCode);
        assertNull(exception.getCause());
        assertFalse(err.toString().contains("Caused by"));
    }

    @Test
    void malformedUrlDoesNotExposeCredentialsInOutputOrLogs() {
        var exception = assertThrows(AviatorSimpleException.class,
            () -> AviatorGrpcClientHelper.parseTarget("https://user:" + SECRET + "@aviator.invalid/%zz"));
        var err = new StringWriter();

        assertEquals(1, FcliExecutionExceptionHandler.INSTANCE.handleException(exception, createCommandLine(err)));

        assertNull(exception.getCause());
        assertFalse(err.toString().contains(SECRET));
        assertEquals(1, appender.list.size());
        var event = appender.list.get(0);
        assertFalse(event.getFormattedMessage().contains(SECRET));
        assertFalse(ThrowableProxyUtil.asString(event.getThrowableProxy()).contains(SECRET));
    }

    private static CommandLine createCommandLine(StringWriter err) {
        var commandLine = new CommandLine(CommandSpec.create());
        commandLine.setErr(new PrintWriter(err));
        commandLine.setColorScheme(CommandLine.Help.defaultColorScheme(Ansi.OFF));
        return commandLine;
    }
}