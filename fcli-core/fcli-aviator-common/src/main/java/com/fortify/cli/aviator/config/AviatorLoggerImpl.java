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

    @Override
    public void warn(String format, Object... args) {
        logger.warn(format, args);
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