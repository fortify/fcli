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
package com.fortify.cli.app.runner.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.cli.util.StdioHelper;
import com.fortify.cli.common.progress.helper.ProgressAwareConsoleAppender;
import com.fortify.cli.common.util.DebugHelper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

class FortifyCLIDynamicInitializerTest {
    @TempDir
    Path tempDir;

    @Test
    void errorLevelKeepsWarningsOnStderrAndWritesOnlyErrorsToFile() throws Exception {
        String warning = "error-level-warning-stderr-only";
        String error = "error-level-error-stderr-and-file";
        String childDebug = "error-level-child-debug-excluded";
        String childWarning = "error-level-child-warning-stderr-only";
        var output = capture(new String[] { "--log-level=ERROR" }, tempDir.resolve("error.log"), root -> {
            root.warn(warning);
            root.error(error);
            withDebugChild(root, child -> {
                child.debug(childDebug);
                child.warn(childWarning);
            });
        });

        assertTrue(output.stderr().contains(warning), output.stderr());
        assertTrue(output.stderr().contains(error), output.stderr());
        assertTrue(output.stderr().contains(childWarning), output.stderr());
        assertFalse(output.stderr().contains(childDebug), output.stderr());
        assertFalse(output.file().contains(warning), output.file());
        assertFalse(output.file().contains(childDebug), output.file());
        assertFalse(output.file().contains(childWarning), output.file());
        assertTrue(output.file().contains(error), output.file());
    }

    @Test
    void infoLevelWritesInfoToFileButKeepsStderrAtWarnAndAbove() throws Exception {
        String debug = "info-level-debug-file-excluded";
        String info = "info-level-info-file-only";
        String warning = "info-level-warning-stderr-and-file";
        String error = "info-level-error-stderr-and-file";
        var output = capture(new String[] { "--log-level=INFO" }, tempDir.resolve("info.log"), root -> {
            root.debug(debug);
            root.info(info);
            root.warn(warning);
            root.error(error);
        });

        assertFalse(output.stderr().contains(debug), output.stderr());
        assertFalse(output.stderr().contains(info), output.stderr());
        assertTrue(output.stderr().contains(warning), output.stderr());
        assertTrue(output.stderr().contains(error), output.stderr());
        assertFalse(output.file().contains(debug), output.file());
        assertTrue(output.file().contains(info), output.file());
        assertTrue(output.file().contains(warning), output.file());
        assertTrue(output.file().contains(error), output.file());
    }

    @Test
    void debugLevelWritesDebugButNotTraceToFile() throws Exception {
        String trace = "explicit-debug-trace-excluded";
        String debug = "explicit-debug-debug-included";
        String warning = "explicit-debug-warning-stderr-and-file";
        var output = capture(new String[] { "--log-level=DEBUG" }, tempDir.resolve("explicit-debug.log"), root -> {
            root.trace(trace);
            root.debug(debug);
            root.warn(warning);
        });

        assertFalse(output.stderr().contains(trace), output.stderr());
        assertFalse(output.stderr().contains(debug), output.stderr());
        assertTrue(output.stderr().contains(warning), output.stderr());
        assertFalse(output.file().contains(trace), output.file());
        assertTrue(output.file().contains(debug), output.file());
        assertTrue(output.file().contains(warning), output.file());
    }

    @Test
    void logFileWithoutExplicitLevelDefaultsToInfo() throws Exception {
        String debug = "default-file-debug-excluded";
        String info = "default-file-info-included";
        String warning = "default-file-warning-stderr-and-file";
        var output = capture(new String[0], tempDir.resolve("default.log"), root -> {
            root.debug(debug);
            root.info(info);
            root.warn(warning);
        });

        assertFalse(output.stderr().contains(debug), output.stderr());
        assertFalse(output.stderr().contains(info), output.stderr());
        assertTrue(output.stderr().contains(warning), output.stderr());
        assertFalse(output.file().contains(debug), output.file());
        assertTrue(output.file().contains(info), output.file());
        assertTrue(output.file().contains(warning), output.file());
    }

    @Test
    void debugDefaultsFileLevelToTraceWithoutLoweringConsoleThreshold() throws Exception {
        String trace = "debug-file-trace-included";
        String debug = "debug-file-debug-included";
        String info = "debug-file-info-included";
        String warning = "debug-file-warning-stderr-and-file";
        var output = capture(new String[] { "--debug" }, tempDir.resolve("debug.log"), root -> {
            root.trace(trace);
            root.debug(debug);
            root.info(info);
            root.warn(warning);
        });

        assertFalse(output.stderr().contains(trace), output.stderr());
        assertFalse(output.stderr().contains(debug), output.stderr());
        assertFalse(output.stderr().contains(info), output.stderr());
        assertTrue(output.stderr().contains(warning), output.stderr());
        assertTrue(output.file().contains(trace), output.file());
        assertTrue(output.file().contains(debug), output.file());
        assertTrue(output.file().contains(info), output.file());
        assertTrue(output.file().contains(warning), output.file());
    }

    @Test
    void explicitErrorLevelOverridesDebugFileDetail() throws Exception {
        String debug = "debug-explicit-error-debug-excluded";
        String warning = "debug-explicit-error-warning-stderr-only";
        String error = "debug-explicit-error-error-stderr-and-file";
        var output = capture(new String[] { "--debug", "--log-level=ERROR" }, tempDir.resolve("debug-error.log"), root -> {
            assertEquals(Level.WARN, root.getLevel());
            root.debug(debug);
            root.warn(warning);
            root.error(error);
        });

        assertFalse(output.stderr().contains(debug), output.stderr());
        assertTrue(output.stderr().contains(warning), output.stderr());
        assertTrue(output.stderr().contains(error), output.stderr());
        assertFalse(output.file().contains(debug), output.file());
        assertFalse(output.file().contains(warning), output.file());
        assertTrue(output.file().contains(error), output.file());
    }

    @Test
    void noneDisablesDynamicFileLoggingAndKeepsWarningsOnStderr() throws Exception {
        String warning = "none-warning-stderr-only";
        var output = capture(new String[] { "--log-level=NONE" }, tempDir.resolve("none.log"), root -> root.warn(warning));

        assertTrue(output.stderr().contains(warning), output.stderr());
        assertFalse(output.fileCreated());
    }

    @Test
    void noLoggingOptionsKeepWarningsOnStderrWithoutCreatingFile() throws Exception {
        String warning = "default-warning-stderr-only";
        var output = capture(new String[0], tempDir.resolve("no-options.log"), false, root -> root.warn(warning));

        assertTrue(output.stderr().contains(warning), output.stderr());
        assertFalse(output.fileCreated());
    }

    @Test
    void explicitLevelWithoutFilePathUsesFcliLog() throws Exception {
        Path defaultLogFile = Path.of("fcli.log").toAbsolutePath();
        Path backup = tempDir.resolve("fcli.log.backup");
        boolean existedBeforeTest = Files.exists(defaultLogFile);
        if ( existedBeforeTest ) {
            Files.copy(defaultLogFile, backup);
        }
        try {
            String warning = "implicit-log-file-warning-stderr-only";
            String error = "implicit-log-file-error";
            var output = capture(new String[] { "--log-level=ERROR" }, defaultLogFile, false, root -> {
                root.warn(warning);
                root.error(error);
            });

            assertTrue(output.fileCreated());
            assertTrue(output.stderr().contains(warning), output.stderr());
            assertFalse(output.file().contains(warning), output.file());
            assertTrue(output.file().contains(error), output.file());
        } finally {
            if ( existedBeforeTest ) {
                Files.copy(backup, defaultLogFile, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.deleteIfExists(defaultLogFile);
            }
        }
    }

    private LogOutput capture(String[] options, Path logFile, Consumer<Logger> logEvents) throws Exception {
        return capture(options, logFile, true, logEvents);
    }

    private LogOutput capture(String[] options, Path logFile, boolean includeLogFileOption, Consumer<Logger> logEvents) throws Exception {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = context.getLogger(ROOT_LOGGER_NAME);
        var previousLevel = root.getLevel();
        boolean previousDebugEnabled = DebugHelper.isDebugEnabled();
        var previousAppenders = getAppenders(root);
        PrintStream previousErr = System.err;
        var stderrBytes = new ByteArrayOutputStream();
        var captureErr = new PrintStream(stderrBytes, true, StandardCharsets.UTF_8);
        var encoder = new PatternLayoutEncoder();
        var thresholdFilter = new ThresholdFilter();
        var consoleAppender = new ProgressAwareConsoleAppender();
        boolean pushedErr = false;
        try {
            previousAppenders.forEach(root::detachAppender);
            root.setLevel(Level.WARN);
            encoder.setContext(context);
            encoder.setPattern("%msg%n");
            encoder.start();
            thresholdFilter.setLevel("WARN");
            thresholdFilter.start();
            consoleAppender.setContext(context);
            consoleAppender.setEncoder(encoder);
            consoleAppender.addFilter(thresholdFilter);
            consoleAppender.start();
            root.addAppender(consoleAppender);
            System.setErr(captureErr);
            StdioHelper.pushErr(captureErr);
            pushedErr = true;

            var args = new String[options.length + (includeLogFileOption ? 2 : 0)];
            System.arraycopy(options, 0, args, 0, options.length);
            if ( includeLogFileOption ) {
                args[options.length] = "--log-file";
                args[options.length + 1] = logFile.toString();
            }
            FortifyCLIDynamicInitializer.getInstance().initialize(args);
            logEvents.accept(root);
            captureErr.flush();

            detachAndStopTestAppenders(root, previousAppenders);
            boolean fileCreated = Files.exists(logFile);
            String file = fileCreated ? Files.readString(logFile) : "";
            return new LogOutput(stderrBytes.toString(StandardCharsets.UTF_8), file, fileCreated);
        } finally {
            if ( pushedErr ) {
                StdioHelper.popErr();
            }
            detachAndStopTestAppenders(root, previousAppenders);
            root.setLevel(previousLevel);
            DebugHelper.setDebugEnabled(previousDebugEnabled);
            previousAppenders.forEach(root::addAppender);
            System.setErr(previousErr);
            consoleAppender.stop();
            thresholdFilter.stop();
            encoder.stop();
            captureErr.close();
        }
    }

    private static void withDebugChild(Logger root, Consumer<Logger> logEvents) {
        Logger child = root.getLoggerContext().getLogger(
                "com.fortify.cli.app.runner.util.FortifyCLIDynamicInitializerTest.explicitDebugChild");
        Level previousLevel = child.getLevel();
        boolean previousAdditive = child.isAdditive();
        try {
            child.setLevel(Level.DEBUG);
            child.setAdditive(true);
            logEvents.accept(child);
        } finally {
            child.setLevel(previousLevel);
            child.setAdditive(previousAdditive);
        }
    }

    private static List<Appender<ILoggingEvent>> getAppenders(Logger logger) {
        var appenders = new ArrayList<Appender<ILoggingEvent>>();
        var iterator = logger.iteratorForAppenders();
        while ( iterator.hasNext() ) {
            appenders.add(iterator.next());
        }
        return appenders;
    }

    private static void detachAndStopTestAppenders(Logger root, List<Appender<ILoggingEvent>> previousAppenders) {
        getAppenders(root).stream()
                .filter(appender -> !previousAppenders.contains(appender))
                .forEach(appender -> {
                    root.detachAppender(appender);
                    appender.stop();
                });
    }

    private record LogOutput(String stderr, String file, boolean fileCreated) {}
}
