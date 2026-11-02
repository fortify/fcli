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
package com.fortify.cli.common.progress.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.cli.util.StdioHelper;
import com.fortify.cli.common.log.LogMaskHelper;
import com.fortify.cli.common.log.LogMaskLevel;
import com.fortify.cli.common.log.LogSensitivityLevel;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class ProgressWarningRoutingTest {
    @Test
    void workerWarningUsesCapturedStderrAndMaskContext() throws Exception {
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        var physical = new ByteArrayOutputStream();
        var capturedErr = new ByteArrayOutputStream();
        var physicalStream = new PrintStream(physical, true, StandardCharsets.UTF_8);
        var captureStream = new PrintStream(capturedErr, true, StandardCharsets.UTF_8);
        Field maskLevelField = LogMaskHelper.class.getDeclaredField("logMaskLevel");
        maskLevelField.setAccessible(true);
        var previousMaskLevel = (LogMaskLevel) maskLevelField.get(LogMaskHelper.INSTANCE);
        System.setOut(physicalStream);
        System.setErr(physicalStream);
        StdioHelper.install();

        Logger logger = (Logger) LoggerFactory.getLogger("com.fortify.cli.common.progress.helper.ProgressWriterType$AbstractProgressWriter");
        Level previousLevel = logger.getLevel();
        boolean previousAdditive = logger.isAdditive();
        var encoder = new PatternLayoutEncoder();
        encoder.setContext(logger.getLoggerContext());
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.setPattern("%msg%n%ex{0}");
        encoder.start();
        var console = new ProgressAwareConsoleAppender();
        console.setContext(logger.getLoggerContext());
        console.setEncoder(encoder);
        console.start();
        var events = new ListAppender<ILoggingEvent>();
        events.setContext(logger.getLoggerContext());
        events.start();
        logger.setLevel(Level.WARN);
        logger.setAdditive(false);
        logger.addAppender(console);
        logger.addAppender(events);

        try (var context = FcliExecutionContextHolder.pushNew()) {
            LogMaskHelper.INSTANCE.setLogMaskLevel(LogMaskLevel.high);
            LogMaskHelper.INSTANCE.registerValue(LogSensitivityLevel.high, "secret-worker-72814", "<MASKED>");
            StdioHelper.pushErr(captureStream);
            try (var writer = ProgressWriterType.single_line.create()) {
                var pool = Executors.newSingleThreadExecutor();
                try {
                    pool.submit(() -> writer.writeWarningWithException("worker secret-worker-72814",
                            new IllegalArgumentException("cause secret-worker-72814"))).get(5, TimeUnit.SECONDS);
                } finally {
                    pool.shutdownNow();
                }
            } finally {
                StdioHelper.popErr();
            }

            String routed = capturedErr.toString(StandardCharsets.UTF_8);
            assertTrue(routed.contains("worker <MASKED>"), routed);
            assertTrue(routed.contains("cause <MASKED>"), routed);
            assertFalse(routed.contains("secret-worker-72814"), routed);
            assertFalse(physical.toString(StandardCharsets.UTF_8).contains("worker secret-worker-72814"));
            assertEquals(1, events.list.size());
            assertEquals(Level.WARN, events.list.get(0).getLevel());
            assertTrue(events.list.get(0).getThrowableProxy() != null);
        } finally {
            logger.detachAppender(console);
            logger.detachAppender(events);
            logger.setAdditive(previousAdditive);
            logger.setLevel(previousLevel);
            console.stop();
            events.stop();
            encoder.stop();
            LogMaskHelper.INSTANCE.setLogMaskLevel(previousMaskLevel);
            StdioHelper.uninstall();
            System.setOut(oldOut);
            System.setErr(oldErr);
            captureStream.close();
            physicalStream.close();
        }
    }
}
