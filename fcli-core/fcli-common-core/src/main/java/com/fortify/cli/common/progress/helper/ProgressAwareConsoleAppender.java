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

import java.nio.charset.StandardCharsets;

import com.fortify.cli.common.log.LogMaskHelper;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;

/** Emits each log event after clearing visible progress on the same output lock. */
public final class ProgressAwareConsoleAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private Encoder<ILoggingEvent> encoder;

    public void setEncoder(Encoder<ILoggingEvent> encoder) { this.encoder = encoder; }

    @Override
    public void start() {
        if (encoder == null) {
            addError("No encoder configured for progress-aware console appender");
            return;
        }
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        // Resolve System.err when the event is emitted so command-scoped
        // redirection and masking also work for worker-thread warnings.
        var encoded = new String(encoder.encode(event), StandardCharsets.UTF_8);
        byte[] bytes = LogMaskHelper.INSTANCE.maskStdio(encoded).getBytes(StandardCharsets.UTF_8);
        ProgressRenderCoordinator.emit(() -> System.err.write(bytes, 0, bytes.length));
    }
}
