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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.exception.FcliExceptionHelper;
import com.fortify.cli.common.exception.FcliExecutionExceptionHandler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import io.grpc.Status;
import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Model.CommandSpec;

class AviatorConnectivityExceptionTest {
    private static final String SECRET = "sentinel-secret-value";
    private static final String MESSAGE = "Connection to proxy localhost:33444 was refused\n"
            + "Check that the proxy is running and that its host and port are correct";

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
    void stackTraceStringIsOnlyTheMessage() {
        var exception = createException();

        assertEquals(MESSAGE, exception.getStackTraceString());
        assertEquals(MESSAGE, FcliExceptionHelper.formatException(exception));
    }

    @Test
    void safeDiagnosticStringHasMetadataButNoSecrets() {
        var diagnostic = createException().getSafeDiagnosticString();

        assertTrue(diagnostic.contains("kind=PROXY_REFUSED"));
        assertTrue(diagnostic.contains("proxy localhost:33444"));
        assertTrue(diagnostic.contains("java.net.ConnectException"));
        assertFalse(diagnostic.contains(SECRET));
    }

    @Test
    void handlerPrintsOnlyMessageAndLogsOnlySafeDiagnostic() {
        var err = new StringWriter();
        var commandLine = new CommandLine(CommandSpec.create());
        commandLine.setErr(new PrintWriter(err));
        commandLine.setColorScheme(CommandLine.Help.defaultColorScheme(Ansi.OFF));

        int exitCode = FcliExecutionExceptionHandler.INSTANCE.handleException(createException(), commandLine);

        assertEquals(1, exitCode);
        assertEquals(MESSAGE, err.toString().trim());
        assertFalse(err.toString().contains("Exception"));
        assertEquals(1, appender.list.size());
        var event = appender.list.get(0);
        assertNull(event.getThrowableProxy());
        assertTrue(event.getFormattedMessage().contains("kind=PROXY_REFUSED"));
        assertFalse(event.getFormattedMessage().contains(SECRET));
    }

    @Test
    void malformedUrlDoesNotExposeCredentialsInOutputOrLogs() {
        var exception = assertThrows(AviatorSimpleException.class,
            () -> AviatorGrpcClientHelper.parseTarget("https://user:" + SECRET + "@aviator.invalid/%zz"));
        var err = new StringWriter();
        var commandLine = new CommandLine(CommandSpec.create());
        commandLine.setErr(new PrintWriter(err));

        assertEquals(1, FcliExecutionExceptionHandler.INSTANCE.handleException(exception, commandLine));

        assertNull(exception.getCause());
        assertFalse(err.toString().contains(SECRET));
        assertEquals(1, appender.list.size());
        var event = appender.list.get(0);
        assertFalse(event.getFormattedMessage().contains(SECRET));
        assertFalse(ThrowableProxyUtil.asString(event.getThrowableProxy()).contains(SECRET));
    }

    private static AviatorConnectivityException createException() {
        var transportCause = new ConnectException("Connection refused: proxy-user:" + SECRET + "@localhost");
        transportCause.addSuppressed(new IllegalStateException("suppressed " + SECRET));
        var statusException = Status.UNAVAILABLE
            .withDescription("PKIX path building failed: " + SECRET)
            .withCause(transportCause)
            .asRuntimeException();
        return new AviatorConnectivityException(MESSAGE, "PROXY_REFUSED", statusException);
    }
}
