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
package com.fortify.cli.aviator.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.progress.helper.ProgressWriterType;

class AviatorLoggerImplTest {
    @ParameterizedTest
    @MethodSource("projectNames")
    void singleLineProgressHandlesLineBreaksInArguments(String projectName, String displayedName) {
        var writer = new RecordingProgressWriter(false);

        new AviatorLoggerImpl(writer).progress("Starting audit for project: %s", projectName);

        assertEquals("Starting audit for project: " + displayedName, writer.message);
    }

    static Stream<Arguments> projectNames() {
        return Stream.of(
                Arguments.of("app1\n", "app1 "),
                Arguments.of("project\r\nversion", "project version"),
                Arguments.of("project\rversion", "project version"),
                Arguments.of("project\nversion", "project version"),
                Arguments.of("project\u2028version", "project version"),
                Arguments.of("100% ready\t now", "100% ready\t now"));
    }

    @Test
    void singleLineProgressHandlesLineBreaksInFormat() {
        var writer = new RecordingProgressWriter(false);

        new AviatorLoggerImpl(writer).progress("First%nSecond %d", 42);

        assertEquals("First Second 42", writer.message);
    }

    @Test
    void multilineProgressKeepsOriginalFormatting() {
        var writer = new RecordingProgressWriter(true);
        String message = "First\r\nSecond\nThird\rFourth";

        new AviatorLoggerImpl(writer).progress("%s", message);

        assertEquals(message, writer.message);
    }

    @Test
    void projectNameWithTrailingNewlineDoesNotAbortRealSingleLineWriter() {
        try (var writer = ProgressWriterType.single_line.create()) {
            var logger = new AviatorLoggerImpl(writer);

            assertDoesNotThrow(() -> logger.progress("Starting audit for project: %s", "app1\n"));
        }
    }

    private static final class RecordingProgressWriter implements IProgressWriter {
        private final boolean multiLineSupported;
        private String message;

        private RecordingProgressWriter(boolean multiLineSupported) {
            this.multiLineSupported = multiLineSupported;
        }

        @Override public boolean isMultiLineSupported() { return multiLineSupported; }
        @Override public void writeProgress(String message, Object... args) { this.message = message; }
        @Override public void writeInfo(String message, Object... args) {}
        @Override public void writeInfoWithException(String message, Throwable cause, Object... args) {}
        @Override public void writeWarning(String message, Object... args) {}
        @Override public void writeWarningWithException(String message, Throwable cause, Object... args) {}
        @Override public void clearProgress() {}
        @Override public void close() {}
        @Override public String type() { return "recording"; }
    }
}
