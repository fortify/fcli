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
     * Writes one warning for the user.
     * The progress writer prints the line. It is recorded at debug level rather than warning
     * level, because the JAR also prints warning-level log output to standard error and the
     * same line would appear twice.
     */
    @Override
    public void warn(String format, Object... args) {
        String message = String.format(format, args);
        progressWriter.writeWarning(message);
        logger.debug(message);
    }

    @Override
    public void error(String format, Object... args) {
        String message = String.format(format, args);
        if (logger.isErrorEnabled()) {
            logger.error(message);
        } else {
            progressWriter.writeWarning(message);
            logger.error(message);
        }
    }
}
