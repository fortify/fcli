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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.progress.helper.IProgressWriter;

public class AviatorLoggerImpl implements IAviatorLogger {
    private final IProgressWriter progressWriter;
    private final Logger logger = LoggerFactory.getLogger("com.fortify.cli.aviator");

    public AviatorLoggerImpl(IProgressWriter progressWriter) {
        this.progressWriter = progressWriter;
    }

    @Override
    public void progress(String format, Object... args) {
        String message = String.format(format, args);
        progressWriter.writeProgress(message); // Console
        logger.info(message);
    }

    @Override
    public void info(String format, Object... args) {
        logger.info(format, args);
    }

    /**
     * Formats the message and routes it through {@code progressWriter.writeWarning}, not the
     * {@code com.fortify.cli.aviator} SLF4J logger. {@code warning()} prefixes the formatted message
     * with {@code WARN: } when it does not already start with {@code WARN:}, so the prefix appears
     * once. {@code writeWarning} records the line on the progress writer's logger, which shows the
     * warning on the console and in the log file.
     */
    @Override
    public void warn(String format, Object... args) {
        progressWriter.writeWarning(warning(format, args));
    }

    @Override
    public void warnWithException(String format, Throwable cause, Object... args) {
        progressWriter.writeWarningWithException(warning(format, args), cause);
    }

    private static String warning(String format, Object... args) {
        String message = String.format(format, args);
        return message.startsWith("WARN:") ? message : "WARN: " + message;
    }

    @Override
    public void error(String format, Object... args) {
        progressWriter.writeError(format, args);
    }

    @Override
    public void errorWithException(String format, Throwable cause, Object... args) {
        progressWriter.writeErrorWithException(format, cause, args);
    }
}
