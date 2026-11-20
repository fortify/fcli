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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.cli.util.StdioHelper;
import com.fortify.cli.common.util.OutputHelper;
import com.fortify.cli.common.util.OutputHelper.OutputType;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

class ProgressCaptureIsolationTest {
    private final ByteArrayOutputStream terminal = new ByteArrayOutputStream();
    private final PrintStream terminalStream = new PrintStream(terminal, true, StandardCharsets.UTF_8);
    private final PrintStream terminalErrStream = new PrintStream(terminal, true, StandardCharsets.UTF_8);
    private PrintStream oldOut;
    private PrintStream oldErr;
    private Logger logger;
    private Level previousLevel;
    private boolean previousAdditive;
    private PatternLayoutEncoder encoder;
    private ProgressAwareConsoleAppender appender;

    @BeforeEach
    void install() {
        oldOut = System.out;
        oldErr = System.err;
        System.setOut(terminalStream);
        System.setErr(terminalErrStream);
        StdioHelper.install();
        logger = (Logger) LoggerFactory.getLogger(ProgressCaptureIsolationTest.class);
        previousLevel = logger.getLevel();
        previousAdditive = logger.isAdditive();
        encoder = new PatternLayoutEncoder();
        encoder.setContext(logger.getLoggerContext());
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.setPattern("%msg%n");
        encoder.start();
        appender = new ProgressAwareConsoleAppender();
        appender.setContext(logger.getLoggerContext());
        appender.setEncoder(encoder);
        appender.start();
        logger.setLevel(Level.WARN);
        logger.setAdditive(false);
        logger.addAppender(appender);
    }

    @AfterEach
    void restore() {
        logger.detachAppender(appender);
        logger.setLevel(previousLevel);
        logger.setAdditive(previousAdditive);
        appender.stop();
        encoder.stop();
        StdioHelper.uninstall();
        System.setOut(oldOut);
        System.setErr(oldErr);
        terminalStream.close();
        terminalErrStream.close();
    }

    @Test
    void collectedAndSuppressedOutputDoesNotClearTerminalProgress() throws Exception {
        try (var outer = ProgressWriterType.single_line.create()) {
            outer.writeProgress("visible progress");
            var before = terminal.toString(StandardCharsets.UTF_8);
            var result = OutputHelper.builder().stdoutType(OutputType.collect).stderrType(OutputType.collect).build()
                    .call(() -> {
                        try (var inner = ProgressWriterType.none.create(); var nested = ProgressWriterType.none.create()) {
                            System.out.println("captured data");
                            System.err.println("captured stderr");
                            logger.warn("captured warning");
                        }
                        return 0;
                    });
            assertEquals("captured data" + System.lineSeparator(), result.getOut());
            assertEquals("captured stderr" + System.lineSeparator() + "captured warning" + System.lineSeparator(),
                    result.getErr());
            assertEquals(before, terminal.toString(StandardCharsets.UTF_8));

            OutputHelper.builder().stdoutType(OutputType.suppress).stderrType(OutputType.suppress).build()
                    .call(() -> {
                        System.out.println("suppressed data");
                        logger.warn("suppressed warning");
                        return 0;
                    });
            assertEquals(before, terminal.toString(StandardCharsets.UTF_8));
        }
    }

    @Test
    void physicalStderrClearsProgressOnPhysicalStdout() {
        try (var writer = ProgressWriterType.single_line.create()) {
            writer.writeProgress("visible progress");
            logger.warn("physical warning");
            assertTrue(terminal.toString(StandardCharsets.UTF_8).contains("\rphysical warning"));
        }
    }

    @Test
    void workerCaptureDoesNotClearAnotherThreadsProgress() throws Exception {
        var pool = Executors.newSingleThreadExecutor();
        try (var writer = ProgressWriterType.single_line.create()) {
            writer.writeProgress("visible progress");
            var before = terminal.toString(StandardCharsets.UTF_8);
            var result = pool.submit(() ->
                    OutputHelper.builder().stdoutType(OutputType.collect).stderrType(OutputType.collect).build()
                            .call(() -> {
                                try (var captured = ProgressWriterType.none.create()) {
                                    System.out.println("worker data");
                                    logger.warn("worker warning");
                                }
                                return 0;
                            })).get(5, TimeUnit.SECONDS);
            assertEquals("worker data" + System.lineSeparator(), result.getOut());
            assertEquals("worker warning" + System.lineSeparator(), result.getErr());
            assertEquals(before, terminal.toString(StandardCharsets.UTF_8));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void blockedCaptureDoesNotBlockTerminalProgress() throws Exception {
        var enteredWrite = new CountDownLatch(1);
        var releaseWrite = new CountDownLatch(1);
        var capture = new PrintStream(new ByteArrayOutputStream()) {
            @Override
            public void print(String message) {
                enteredWrite.countDown();
                try {
                    if (!releaseWrite.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("capture write was not released");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
                super.print(message);
            }
        };
        var pool = Executors.newFixedThreadPool(2);
        try (var writer = ProgressWriterType.single_line.create()) {
            var output = pool.submit(() -> new ProgressWriterPrintStreamWrapper(capture).print("captured"));
            assertTrue(enteredWrite.await(5, TimeUnit.SECONDS));
            var progress = pool.submit(() -> writer.writeProgress("physical progress"));
            progress.get(3, TimeUnit.SECONDS);
            releaseWrite.countDown();
            output.get(5, TimeUnit.SECONDS);
            assertTrue(terminal.toString(StandardCharsets.UTF_8).contains("physical progress"));
        } finally {
            releaseWrite.countDown();
            pool.shutdownNow();
            capture.close();
        }
    }
}
