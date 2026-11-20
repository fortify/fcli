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
import java.io.File;
import java.io.PrintStream;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
import ch.qos.logback.core.FileAppender;

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
        Path output = tempDir.resolve("child-output.txt");
        var process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", subprocessClasspath(), DefaultLogFileProbe.class.getName())
                .directory(tempDir.toFile())
                .redirectErrorStream(true)
                .redirectOutput(output.toFile())
                .start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Default log file probe timed out");
            assertEquals(0, process.exitValue(), Files.readString(output));
            String file = Files.readString(tempDir.resolve("fcli.log"));
            assertFalse(file.contains("implicit-log-file-warning-stderr-only"), file);
            assertTrue(file.contains("implicit-log-file-error"), file);
            assertTrue(Files.readString(output).contains("implicit-log-file-warning-stderr-only"));
        } finally {
            process.destroyForcibly();
        }
    }

    /** Runs in a fresh JVM whose working directory belongs exclusively to this test. */
    public static class DefaultLogFileProbe {
        public static void main(String[] args) {
            FortifyCLIDynamicInitializer.getInstance().initialize(new String[] { "--log-level=ERROR" });
            Logger root = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);
            root.warn("implicit-log-file-warning-stderr-only");
            root.error("implicit-log-file-error");
            ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
        }
    }

    private static String subprocessClasspath() throws Exception {
        var entries = new LinkedHashSet<String>();
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            entries.add(Path.of(entry).toAbsolutePath().toString());
        }
        // Gradle loads test dependencies through a separate class loader.
        for (ClassLoader loader = FortifyCLIDynamicInitializerTest.class.getClassLoader();
                loader != null; loader = loader.getParent()) {
            if (loader instanceof URLClassLoader urls) {
                for (var url : urls.getURLs()) {
                    entries.add(Path.of(url.toURI()).toAbsolutePath().toString());
                }
            }
        }
        return String.join(File.pathSeparator, entries);
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
            // The production singleton caches its parser; every test needs fresh option state.
            var constructor = FortifyCLIDynamicInitializer.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance().initialize(args);
            if (!includeLogFileOption && options.length == 0) {
                assertFalse(getAppenders(root).stream().anyMatch(FileAppender.class::isInstance),
                        "No logging options must not install any file appender");
            }
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
