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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.progress.helper.ProgressWriterType;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;

class AviatorLoggerImplTest {
    private static final String WRITER_LOGGER_NAME =
            "com.fortify.cli.common.progress.helper.ProgressWriterType$AbstractProgressWriter";

    @Test
    void warningsAndErrorsUseOneEventWithCorrectSeverityAndCause() {
        Logger logbackLogger = (Logger) LoggerFactory.getLogger(WRITER_LOGGER_NAME);
        Level previousLevel = logbackLogger.getLevel();
        boolean previousAdditivity = logbackLogger.isAdditive();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logbackLogger.addAppender(appender);
        logbackLogger.setAdditive(false);
        logbackLogger.setLevel(Level.WARN);
        try (var writer = ProgressWriterType.none.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(writer);
            IllegalStateException warningCause = new IllegalStateException("quota unavailable");
            IllegalStateException errorCause = new IllegalStateException("stream failed");

            logger.warn("Quota unavailable for %s", "app");
            logger.warn("WARN: Quota unavailable for %s", "other app");
            logger.warnWithException("Cannot retrieve quota", warningCause);
            logger.errorWithException("Audit failed for %s", errorCause, "app");

            assertEquals(4, appender.list.size());
            assertEvent(appender.list.get(0), Level.WARN, "WARN: Quota unavailable for app", null);
            assertEvent(appender.list.get(1), Level.WARN, "WARN: Quota unavailable for other app", null);
            assertEvent(appender.list.get(2), Level.WARN, "WARN: Cannot retrieve quota", warningCause);
            assertEvent(appender.list.get(3), Level.ERROR, "Audit failed for app", errorCause);
        } finally {
            logbackLogger.setLevel(previousLevel);
            logbackLogger.setAdditive(previousAdditivity);
            logbackLogger.detachAppender(appender);
            appender.stop();
        }
    }

    private static void assertEvent(ILoggingEvent event, Level level, String message, Throwable cause) {
        assertEquals(level, event.getLevel());
        assertEquals(message, event.getFormattedMessage());
        if (cause == null) {
            assertNull(event.getThrowableProxy());
        } else {
            assertSame(cause, ((ThrowableProxy) event.getThrowableProxy()).getThrowable());
        }
    }
}
